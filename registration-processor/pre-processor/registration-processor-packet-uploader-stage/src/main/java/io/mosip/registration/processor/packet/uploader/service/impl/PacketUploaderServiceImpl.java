package io.mosip.registration.processor.packet.uploader.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.virusscanner.exception.VirusScannerException;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.ObjectStoreNotAccessibleException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.SftpFileOperationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.utils.ZipUtils;
import io.mosip.registration.processor.packet.uploader.archiver.util.PacketArchiver;
import io.mosip.registration.processor.packet.uploader.exception.PacketNotFoundException;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class PacketUploaderServiceImpl.
 *
 * @author Rishabh Keshari
 */
@RefreshScope
@Component
public class PacketUploaderServiceImpl implements PacketUploaderService<MessageDTO> {

    /**
     * The reg proc logger.
     */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketUploaderServiceImpl.class);

    /**
     * The fillesystem manager.
     */
    private static final String USER = "MOSIP_SYSTEM";
    private static final String ZIP = ".zip";
    private static final String JSON = ".json";

    @Value("${packet.manager.account.name}")
    private String packetManagerAccount;

    /**
     * the packet extension(Ex - .zip)
     */
    @Value("${registration.processor.packet.ext}")
    private String extention;

    /**
     * The max retry count.
     */
    @Value("${registration.processor.max.retry}")
    private int maxRetryCount;

    @Autowired
    private ObjectStoreAdapter objectStoreAdapter;

    /**
     * The sync registration service.
     */
    @Autowired
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

    /**
     * The registration status service.
     */
    @Autowired
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /**
     * The core audit request builder.
     */
    @Autowired
    private AuditLogRequestBuilder auditLogRequestBuilder;

    /**
     * The virus scanner service.
     */
    @Autowired
    private VirusScanner<Boolean, InputStream> virusScannerService;

    @Autowired
    private RegistrationProcessorRestClientService restClient;

    /**
     * The registration status mapper util.
     */
    @Autowired
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    @Autowired
    private Decryptor decryptor;

    @Autowired
    private ObjectMapper mapper;


    /**
     * The is transaction successful.
     */
    boolean isTransactionSuccessful = false;

    /**
     * The packet archiver.
     */
    @Autowired
    private PacketArchiver packetArchiver;

    /*
     * java class to trim exception message
     */
    private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

    /*
     * (non-Javadoc)
     *
     * @see io.mosip.id.issuance.packet.handler.service.PacketUploadService#
     * validatePacket( java.lang.Object)
     */

    @Override
    public MessageDTO validateAndUploadPacket(String registrationId, String stageName) {

        LogDescription description = new LogDescription();
        InternalRegistrationStatusDto dto = new InternalRegistrationStatusDto();
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setInternalError(false);
        messageDTO.setIsValid(false);
        isTransactionSuccessful = false;
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "PacketUploaderServiceImpl::validateAndUploadPacket()::entry");
        messageDTO.setRid(registrationId);

        try {

            SyncRegistrationEntity regEntity = syncRegistrationService.findByRegistrationId(registrationId);
            messageDTO.setReg_type(RegistrationType.valueOf(regEntity.getRegistrationType()));
            dto = registrationStatusService.getRegistrationStatus(registrationId);

            dto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.UPLOAD_PACKET.toString());
            dto.setRegistrationStageName(stageName);

            final byte[] encryptedByteArray = getPakcetFromDMZ(registrationId);

            if (encryptedByteArray != null) {

                if (validateHashCode(new ByteArrayInputStream(encryptedByteArray), regEntity, registrationId, dto,
                        description)) {
                    InputStream decryptedPacket = decryptor.decrypt(new ByteArrayInputStream(encryptedByteArray), registrationId);
                    final byte[] decryptedPacketBytes = IOUtils.toByteArray(decryptedPacket);
                    if (scanFile(encryptedByteArray, registrationId, ZipUtils.unzipAndGetFiles(new ByteArrayInputStream(decryptedPacketBytes)), dto, description)) {
                        int retrycount = (dto.getRetryCount() == null) ? 0 : dto.getRetryCount() + 1;
                        dto.setRetryCount(retrycount);
                        if (retrycount < getMaxRetryCount()) {

                            messageDTO = uploadPacket(dto, ZipUtils.unzipAndGetFiles(new ByteArrayInputStream(decryptedPacketBytes)), messageDTO, description);
                            if (messageDTO.getIsValid()) {
                                dto.setLatestTransactionStatusCode(
                                        RegistrationTransactionStatusCode.SUCCESS.toString());
                                isTransactionSuccessful = true;
                                description.setMessage(PlatformSuccessMessages.RPR_PUM_PACKET_UPLOADER.getMessage());
                                regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                        description.getMessage());

                            }
                        } else {

                            messageDTO.setInternalError(Boolean.TRUE);
                            description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_UPLOAD_FAILURE.getMessage());
                            description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_UPLOAD_FAILURE.getCode());
                            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED));
                            dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
                            dto.setStatusComment(StatusUtil.PACKET_UPLOAD_FAILED.getMessage());
                            dto.setSubStatusCode(StatusUtil.PACKET_UPLOAD_FAILED.getCode());
                            dto.setUpdatedBy(USER);
                            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                    description.getMessage());
                        }
                    }
                }
            } else {
                messageDTO.setInternalError(Boolean.TRUE);

                dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                        .getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED));
                dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
                dto.setStatusComment(StatusUtil.PACKET_NOT_FOUND_LANDING_ZIONE.getMessage());
                dto.setSubStatusCode(StatusUtil.PACKET_NOT_FOUND_LANDING_ZIONE.getCode());
                dto.setUpdatedBy(USER);
                description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_UPLOAD_FAILURE.getMessage());
                description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_UPLOAD_FAILURE.getCode());

            }

        } catch (TablenotAccessibleException e) {
            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
            dto.setStatusComment(
                    trimExpMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
            dto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
            messageDTO.setInternalError(true);
            messageDTO.setIsValid(false);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.name()
                            + ExceptionUtils.getStackTrace(e));

            description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
            description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());

        } catch (PacketNotFoundException ex) {
            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_NOT_FOUND_EXCEPTION));
            dto.setStatusComment(trimExpMessage
                    .trimExceptionMessage(ex.getMessage()));
            dto.setSubStatusCode(StatusUtil.PACKET_NOT_FOUND_PACKET_STORE.getCode());
            messageDTO.setInternalError(true);
            messageDTO.setIsValid(false);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId,
                    PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.name() + ExceptionUtils.getStackTrace(ex));
            description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.getCode());
        } catch (ApisResourceAccessException e) {
            dto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.NGINX_ACCESS_EXCEPTION));
            dto.setStatusComment(trimExpMessage
                    .trimExceptionMessage(StatusUtil.NGINX_ACCESS_EXCEPTION.getMessage() + e.getMessage()));
            dto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
            messageDTO.setInternalError(true);
            messageDTO.setIsValid(false);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId,
                    PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.name() + ExceptionUtils.getStackTrace(e));

            description.setMessage(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getCode());
        } catch (IOException e) {
            dto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
            dto.setStatusComment(
                    trimExpMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
            dto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
            messageDTO.setIsValid(false);
            messageDTO.setInternalError(true);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId,
                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.name() + ExceptionUtils.getStackTrace(e));
            description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());

        } catch (PacketDecryptionFailureException e) {
            messageDTO.setInternalError(Boolean.TRUE);
            dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
            dto.setStatusComment(StatusUtil.PACKET_DECRYPTION_FAILED.getMessage());
            dto.setSubStatusCode(StatusUtil.PACKET_DECRYPTION_FAILED.getCode());
            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_DECRYPTION_FAILURE_EXCEPTION));
            description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_DECRYPTION_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_DECRYPTION_FAILED.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, ExceptionUtils.getStackTrace(e));

        } catch (ObjectStoreNotAccessibleException e) {
            messageDTO.setInternalError(Boolean.TRUE);
            dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
            dto.setStatusComment(StatusUtil.OBJECT_STORE_EXCEPTION.getMessage());
            dto.setSubStatusCode(StatusUtil.OBJECT_STORE_EXCEPTION.getCode());
            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.OBJECT_STORE_EXCEPTION));
            description.setMessage(PlatformErrorMessages.OBJECT_STORE_NOT_ACCESSIBLE.getMessage());
            description.setCode(PlatformErrorMessages.OBJECT_STORE_NOT_ACCESSIBLE.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, PlatformErrorMessages.OBJECT_STORE_NOT_ACCESSIBLE.name()
                            + ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            dto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
            dto.setStatusComment(trimExpMessage
                    .trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
            dto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
            messageDTO.setInternalError(true);
            messageDTO.setIsValid(false);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId,
                    PlatformErrorMessages.PACKET_UPLOAD_FAILED.name() + ExceptionUtils.getStackTrace(e));
            messageDTO.setInternalError(Boolean.TRUE);
            description.setMessage(PlatformErrorMessages.PACKET_UPLOAD_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.PACKET_UPLOAD_FAILED.getCode());

        } finally {
            /** Module-Id can be Both Success/Error code */
            String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PUM_PACKET_UPLOADER.getCode()
                    : description.getCode();
            String moduleName = ModuleName.PACKET_UPLOAD.toString();
            registrationStatusService.updateRegistrationStatus(dto, moduleId, moduleName);
            String eventId = "";
            String eventName = "";
            String eventType = "";
            eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
            eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
                    : EventName.EXCEPTION.toString();
            eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
                    : EventType.SYSTEM.toString();

            auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
                    moduleId, moduleName, registrationId);

        }

        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "PacketUploaderServiceImpl::validateAndUploadPacket()::exit");
        return messageDTO;
    }

    /**
     * Scan file.
     *
     * @param input    the input stream
     * @param id
     * @param description
     * @return true, if successful
     * @throws IOException
     * @throws ApisResourceAccessException
     */
    private boolean scanFile(final byte[] input, String id, final Map<String, InputStream> sourcePackets, InternalRegistrationStatusDto dto,
                             LogDescription description) throws ApisResourceAccessException, PacketDecryptionFailureException, IOException {
        boolean isInputFileClean = false;
        try {
            InputStream packet = new ByteArrayInputStream(input);
            // scanning the top level packet
            isInputFileClean = virusScannerService.scanFile(packet);

            if (isInputFileClean) {
                // scanning the source packets (Like - id, evidence, optional packets).
                for (final Map.Entry<String, InputStream> source : sourcePackets.entrySet()) {
                    if (source.getKey().endsWith(ZIP)) {
                        InputStream decryptedData = decryptor
                                .decrypt(source.getValue(), id);
                        isInputFileClean = virusScannerService.scanFile(decryptedData);
                    } else
                        isInputFileClean = virusScannerService.scanFile(source.getValue());
                    if (!isInputFileClean)
                        break;
                }
            }
            if (!isInputFileClean) {
                description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCAN_FAILED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCAN_FAILED.getCode());
                dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
                dto.setStatusComment(StatusUtil.VIRUS_SCANNER_FAILED_UPLOADER.getMessage());
                dto.setSubStatusCode(StatusUtil.VIRUS_SCANNER_FAILED_UPLOADER.getCode());
                dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                        .getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCAN_FAILED_EXCEPTION));
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), id,
                        PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCAN_FAILED.getMessage());
            }
        } catch (VirusScannerException e) {

            description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getCode());
            dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
            dto.setStatusComment(trimExpMessage.trimExceptionMessage(
                    StatusUtil.VIRUS_SCANNER_SERVICE_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
            dto.setSubStatusCode(StatusUtil.VIRUS_SCANNER_SERVICE_NOT_ACCESSIBLE.getCode());
            dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCANNER_SERVICE_FAILED));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getMessage()
                            + ExceptionUtils.getStackTrace(e));

        }
        return isInputFileClean;
    }

    /**
     * Validate hash code.
     *
     * @param registrationId the registration id
     * @param inputStream    the input stream
     * @param registrationId
     * @param description
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private boolean validateHashCode(InputStream inputStream, SyncRegistrationEntity regEntity, String registrationId,
                                     InternalRegistrationStatusDto dto, LogDescription description) throws IOException {
        boolean isValidHash = false;
        byte[] isbytearray = IOUtils.toByteArray(inputStream);
        byte[] dataByte = HMACUtils.generateHash(isbytearray);
        String hashSequence = HMACUtils.digestAsPlainText(dataByte);
        String packetHashSequence = regEntity.getPacketHashValue();
        if (!(MessageDigest.isEqual(packetHashSequence.getBytes(), hashSequence.getBytes()))) {
            description.setMessage(PlatformErrorMessages.RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH.getCode());
            dto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED));
            dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
            dto.setStatusComment(StatusUtil.PACKET_HASHCODE_VALIDATION_FAILED.getMessage());
            dto.setSubStatusCode(StatusUtil.PACKET_HASHCODE_VALIDATION_FAILED.getCode());
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, PlatformErrorMessages.RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH.getMessage());

            return isValidHash;
        } else {
            isValidHash = true;
            return isValidHash;
        }
    }

    /**
     * Uploadpacket.
     *
     * @param dto            the dto
     * @param sourcePackets  source packets
     * @param object         the object
     * @param description
     * @return the message DTO
     * @throws IOException                Signals that an I/O exception has occurred.
     * @throws SftpFileOperationException
     */
    private MessageDTO uploadPacket(InternalRegistrationStatusDto dto, final Map<String, InputStream> sourcePackets,
                                    MessageDTO object, LogDescription description) throws ObjectStoreNotAccessibleException {

        object.setIsValid(false);
        String registrationId = dto.getRegistrationId();
        // upload packets
        try {
            for (Map.Entry<String, InputStream> entry : sourcePackets.entrySet()) {
                if (entry.getKey().endsWith(ZIP)) {
                    boolean result = objectStoreAdapter.putObject(packetManagerAccount, registrationId,
                            null, null, entry.getKey().replace(ZIP, ""), entry.getValue());
                    if (!result)
                        throw new ObjectStoreNotAccessibleException("Failed to store packet : " + entry.getKey());
                }
            }

            // upload metadata
            for (Map.Entry<String, InputStream> entry : sourcePackets.entrySet()) {
                if (entry.getKey().endsWith(JSON)) {
                    byte[] bytearray = IOUtils.toByteArray(entry.getValue());
                    String jsonString = new String(bytearray);
                    LinkedHashMap<String, Object> currentIdMap = (LinkedHashMap<String, Object>) mapper.readValue(jsonString, LinkedHashMap.class);
                    objectStoreAdapter.addObjectMetaData(packetManagerAccount, registrationId,
                            null, null, entry.getKey().replace(JSON, ""), currentIdMap);
                }
            }
        } catch (Exception e) {
            object.setIsValid(false);
            object.setInternalError(true);
            throw new ObjectStoreNotAccessibleException(e.getMessage(), e);
        }


        dto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
        dto.setStatusComment(StatusUtil.PACKET_UPLOADED.getMessage());
        dto.setSubStatusCode(StatusUtil.PACKET_UPLOADED.getCode());
        dto.setUpdatedBy(USER);
        object.setInternalError(false);
        object.setIsValid(true);
        object.setRid(registrationId);

        isTransactionSuccessful = true;
        description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_DELETION_INFO.getMessage());
        description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_DELETION_INFO.getCode());

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                PlatformErrorMessages.RPR_PUM_PACKET_DELETION_INFO.getMessage());

        return object;
    }

    /**
     * Get max retry count.
     *
     * @return maxRetryCount
     */
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    private byte[] getPakcetFromDMZ(String rid) throws ApisResourceAccessException {
        List<String> pathSegment = new ArrayList<>();
        pathSegment.add(rid + extention);
        byte[] packet = null;

        try {
            packet = (byte[]) restClient.getApi(ApiName.NGINXDMZURL, pathSegment, "", null, byte[].class);
        } catch (ApisResourceAccessException e) {
            if (e.getCause() instanceof HttpClientErrorException) {
                HttpClientErrorException ex = (HttpClientErrorException) e.getCause();
                if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND))
                    throw new PacketNotFoundException(PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.getMessage(), ex);
            } else
                throw e;
        }
        return packet;
    }

}