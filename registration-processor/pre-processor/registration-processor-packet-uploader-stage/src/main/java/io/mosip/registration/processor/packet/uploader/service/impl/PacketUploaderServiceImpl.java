package io.mosip.registration.processor.packet.uploader.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.virusscanner.exception.VirusScannerException;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.JschConnectionException;
import io.mosip.registration.processor.core.exception.SftpFileOperationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.SftpJschConnectionDto;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
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

/**
 * The Class PacketUploaderServiceImpl.
 * 
 * @author Rishabh Keshari
 *
 */
@RefreshScope
@Component
public class PacketUploaderServiceImpl implements PacketUploaderService<MessageDTO> {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketUploaderServiceImpl.class);

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/** The hdfs adapter. */
	@Autowired
	private PacketManager fileSystemManager;

	/** The ppk file location. */
	// @Value("${registration.processor.server.ppk.filelocation}")
	private String ppkFileLocation;

	/** The ppk file name. */
	// @Value("${registration.processor.server.ppk.filename}")
	private String ppkFileName;

	/** The host. */
	@Value("${registration.processor.dmz.server.host}")
	private String host;

	/** The dmz port. */
	@Value("${registration.processor.dmz.server.port}")
	private String dmzPort;

	/** The dmz server user. */
	@Value("${registration.processor.dmz.server.user}")
	private String dmzServerUser;

	/** The dmz server protocal. */
	@Value("${registration.processor.dmz.server.protocal}")
	private String dmzServerProtocal;

	/** The file manager. */
	@Autowired
	private FileManager<DirectoryPathDto, InputStream> fileManager;

	/** The sync registration service. */
	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status mapper util. */
	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Value("${registration.processor.packet.ext}")
	private String extention;

	/** The file size. */
	@Value("${registration.processor.max.file.size}")
	private String fileSize;

	/** The virus scanner service. */
	@Autowired
	private VirusScanner<Boolean, InputStream> virusScannerService;

	/** The max retry count. */
	@Value("${registration.processor.max.retry}")
	private int maxRetryCount;

	/** The is transaction successful. */
	boolean isTransactionSuccessful = false;

	/** The packet archiver. */
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
		SftpJschConnectionDto jschConnectionDto = new SftpJschConnectionDto();
		jschConnectionDto.setHost(host);
		jschConnectionDto.setPort(Integer.parseInt(dmzPort));
		jschConnectionDto.setPpkFileLocation(ppkFileLocation + File.separator + ppkFileName);
		jschConnectionDto.setUser(dmzServerUser);
		jschConnectionDto.setProtocal(dmzServerProtocal);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "PacketUploaderServiceImpl::validateAndUploadPacket()::entry");
		messageDTO.setRid(registrationId);

		try {

			SyncRegistrationEntity regEntity = syncRegistrationService.findByRegistrationId(registrationId);
			messageDTO.setReg_type(RegistrationType.valueOf(regEntity.getRegistrationType()));
			dto = registrationStatusService.getRegistrationStatus(registrationId);

			dto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.UPLOAD_PACKET.toString());
			dto.setRegistrationStageName(stageName);

			byte[] encryptedByteArray = fileManager.getFile(DirectoryPathDto.LANDING_ZONE, registrationId,
					jschConnectionDto);

			if (encryptedByteArray != null) {

				if (validateHashCode(new ByteArrayInputStream(encryptedByteArray), regEntity, registrationId, dto,
						description)) {

					if (scanFile(new ByteArrayInputStream(encryptedByteArray), registrationId, dto, description)) {
						int retrycount = (dto.getRetryCount() == null) ? 0 : dto.getRetryCount() + 1;
						dto.setRetryCount(retrycount);
						if (retrycount < getMaxRetryCount()) {

							messageDTO = uploadPacket(dto, new ByteArrayInputStream(encryptedByteArray), messageDTO,
									jschConnectionDto, registrationId, description);
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
					.trimExceptionMessage(StatusUtil.PACKET_NOT_FOUND_PACKET_STORE.getMessage() + ex.getMessage()));
			dto.setSubStatusCode(StatusUtil.PACKET_NOT_FOUND_PACKET_STORE.getCode());
			messageDTO.setInternalError(true);
			messageDTO.setIsValid(false);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.name() + ExceptionUtils.getStackTrace(ex));
			description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_NOT_FOUND_EXCEPTION.getCode());
		} catch (FSAdapterException e) {
			dto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.FSADAPTER_EXCEPTION));
			dto.setStatusComment(
					trimExpMessage.trimExceptionMessage(StatusUtil.FS_ADAPTER_EXCEPTION.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.FS_ADAPTER_EXCEPTION.getCode());
			messageDTO.setInternalError(true);
			messageDTO.setIsValid(false);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_PUM_PACKET_STORE_NOT_ACCESSIBLE.name() + ExceptionUtils.getStackTrace(e));

			description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_STORE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_STORE_NOT_ACCESSIBLE.getCode());
		} catch (JschConnectionException e) {
			dto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.JSCH_CONNECTION));
			dto.setStatusComment(trimExpMessage
					.trimExceptionMessage(StatusUtil.JSCH_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.JSCH_EXCEPTION_OCCURED.getCode());
			messageDTO.setInternalError(true);
			messageDTO.setIsValid(false);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_PUM_JSCH_NOT_CONNECTED.name() + ExceptionUtils.getStackTrace(e));

			description.setMessage(PlatformErrorMessages.RPR_PUM_JSCH_NOT_CONNECTED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PUM_JSCH_NOT_CONNECTED.getCode());
		} catch (SftpFileOperationException e) {
			dto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.SFTP_OPERATION_EXCEPTION));
			dto.setStatusComment(trimExpMessage
					.trimExceptionMessage(StatusUtil.SFTP_FILE_OPERATION_EXCEPTION.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			messageDTO.setInternalError(true);
			messageDTO.setIsValid(false);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_PUM_SFTP_FILE_OPERATION_FAILED.name() + ExceptionUtils.getStackTrace(e));

			description.setMessage(PlatformErrorMessages.RPR_PUM_SFTP_FILE_OPERATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PUM_SFTP_FILE_OPERATION_FAILED.getCode());
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
	 * @param inputStream
	 *            the input stream
	 * @param registrationId
	 * @param description
	 * @return true, if successful
	 */
	private boolean scanFile(InputStream inputStream, String registrationId, InternalRegistrationStatusDto dto,
			LogDescription description) {
		boolean isInputFileClean = false;
		try {
			isInputFileClean = virusScannerService.scanFile(inputStream);
			if (!isInputFileClean) {
				description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCAN_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCAN_FAILED.getCode());
				dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
				dto.setStatusComment(StatusUtil.VIRUS_SCANNER_FAILED_UPLOADER.getMessage());
				dto.setSubStatusCode(StatusUtil.VIRUS_SCANNER_FAILED_UPLOADER.getCode());
				dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCAN_FAILED_EXCEPTION));
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
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
					registrationId, PlatformErrorMessages.RPR_PUM_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getMessage()
							+ ExceptionUtils.getStackTrace(e));

		}
		return isInputFileClean;
	}

	/**
	 * Validate hash code.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param inputStream
	 *            the input stream
	 * @param registrationId
	 * @param description
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
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
	 * @param dto
	 *            the dto
	 * @param decryptedData
	 *            the decrypted data
	 * @param object
	 *            the object
	 * @param registrationId
	 * @param description
	 * @return the message DTO
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JschConnectionException
	 * @throws SftpFileOperationException
	 */
	private MessageDTO uploadPacket(InternalRegistrationStatusDto dto, InputStream decryptedData, MessageDTO object,
			SftpJschConnectionDto jschConnectionDto, String registrationId, LogDescription description)
			throws IOException, JschConnectionException, SftpFileOperationException {

		object.setIsValid(false);
		registrationId = dto.getRegistrationId();
		fileSystemManager.storePacket(registrationId, decryptedData);
		if (fileSystemManager.isPacketPresent(registrationId)) {

			if (packetArchiver.archivePacket(dto.getRegistrationId(), jschConnectionDto)) {

				if (fileManager.cleanUp(dto.getRegistrationId(), DirectoryPathDto.LANDING_ZONE,
						DirectoryPathDto.ARCHIVE_LOCATION, jschConnectionDto)) {

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
				} else {
					dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
					dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED));
					dto.setStatusComment(StatusUtil.PACKET_CLEANUP_FAILED.getMessage());
					dto.setSubStatusCode(StatusUtil.PACKET_CLEANUP_FAILED.getCode());
					dto.setUpdatedBy(USER);
					object.setInternalError(true);
					object.setIsValid(false);
					object.setRid(registrationId);
					description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_DELETION_FAILED.getMessage());
					description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_DELETION_FAILED.getCode());

				}

			} else {

				dto.setStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED.toString());
				dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOADER_FAILED));
				dto.setStatusComment(StatusUtil.PACKET_ARCHIVAL_FAILED.getMessage());
				dto.setSubStatusCode(StatusUtil.PACKET_ARCHIVAL_FAILED.getCode());
				dto.setUpdatedBy(USER);
				object.setInternalError(true);
				object.setIsValid(false);
				object.setRid(registrationId);
				description.setMessage(PlatformErrorMessages.RPR_PUM_PACKET_ARCHIVAL_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PUM_PACKET_ARCHIVAL_FAILED.getCode());

			}

		}

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

}