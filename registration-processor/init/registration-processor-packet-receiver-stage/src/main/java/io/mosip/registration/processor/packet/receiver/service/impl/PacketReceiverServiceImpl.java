package io.mosip.registration.processor.packet.receiver.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.AdditionalInfoIdNotFoundException;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import org.apache.commons.io.IOUtils;
import org.h2.store.fs.FileUtils;
import org.h2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
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
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.receiver.constants.PacketReceiverConstant;
import io.mosip.registration.processor.packet.receiver.exception.DuplicateUploadRequestException;
import io.mosip.registration.processor.packet.receiver.exception.FileSizeExceedException;
import io.mosip.registration.processor.packet.receiver.exception.PacketNotSyncException;
import io.mosip.registration.processor.packet.receiver.exception.PacketNotValidException;
import io.mosip.registration.processor.packet.receiver.exception.PacketReceiverAppException;
import io.mosip.registration.processor.packet.receiver.exception.PacketSizeNotInSyncException;
import io.mosip.registration.processor.packet.receiver.exception.UnequalHashSequenceException;
import io.mosip.registration.processor.packet.receiver.service.PacketReceiverService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

/**
 * The Class PacketReceiverServiceImpl.
 *
 */
@RefreshScope
@Component
public class PacketReceiverServiceImpl implements PacketReceiverService<File, MessageDTO> {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketReceiverServiceImpl.class);

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/** The Constant LOG_FORMATTER. */
	public static final String LOG_FORMATTER = "{} - {}";

	/** The Constant RESEND. */
	private static final String RESEND = "RESEND";

	private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

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

	@Autowired
	private AdditionalInfoRequestService additionalInfoRequestService;

	/** The packet receiver stage. */

	@Value("${registration.processor.packet.ext}")
	private String extention;

	/** The file size. */
	@Value("${registration.processor.max.file.size}")
	private String fileSize;

	@Value("#{'${registration.processor.main-processes}'.split(',')}")
    private List<String> mainProcesses;

	/** The virus scanner service. */
	@Autowired
	private VirusScanner<Boolean, InputStream> virusScannerService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.id.issuance.packet.handler.service.PacketUploadService#
	 * validatePacket( java.lang.Object)
	 */
	@Override
	public MessageDTO validatePacket(File file, String stageName) {

		LogDescription description = new LogDescription();
		InternalRegistrationStatusDto dto = new InternalRegistrationStatusDto();
		MessageDTO messageDTO = new MessageDTO();
		Boolean storageFlag = false;
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		SyncRegistrationEntity regEntity;
		boolean isTransactionSuccessful = false;
		if (file.getName() != null && file.exists()) {
			String fileOriginalName = file.getName();
			String packetId = fileOriginalName.split("\\.")[0];
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					packetId, "PacketReceiverServiceImpl::validatePacket()::entry");
			regEntity = syncRegistrationService.findByPacketId(packetId);
			
			String registrationId = null;
			validatePacketWithSync(regEntity, registrationId, description);
			registrationId = regEntity.getRegistrationId();

			messageDTO.setRid(registrationId);
			messageDTO.setWorkflowInstanceId(regEntity.getWorkflowInstanceId());
			try (InputStream encryptedInputStream = FileUtils.newInputStream(file.getAbsolutePath())) {
				byte[] encryptedByteArray = IOUtils.toByteArray(encryptedInputStream);
				messageDTO.setReg_type(regEntity.getRegistrationType());
				validateHashCode(encryptedByteArray, regEntity, registrationId, description);
				validatePacketFormat(fileOriginalName, registrationId, description);
				validatePacketSize(file.length(), regEntity, registrationId, description);
				if (isDuplicatePacket(registrationId, regEntity) && !isExternalStatusResend(registrationId)) {
					description.setMessage(PlatformErrorMessages.RPR_PKR_DUPLICATE_PACKET_RECIEVED.getMessage());
					description.setCode(PlatformErrorMessages.RPR_PKR_DUPLICATE_PACKET_RECIEVED.getCode());
					throw new DuplicateUploadRequestException(
							PlatformErrorMessages.RPR_PKR_DUPLICATE_PACKET_RECIEVED.getMessage());
				}
				description.setMessage(PlatformSuccessMessages.PACKET_RECEIVER_VALIDATION_SUCCESS.getMessage());

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformSuccessMessages.PACKET_RECEIVER_VALIDATION_SUCCESS.getMessage());
				storageFlag = storePacket(stageName, regEntity, dto, description);
				isTransactionSuccessful = true;
			} catch (IOException | NoSuchAlgorithmException e) {

				description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
				description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
				throw new PacketReceiverAppException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			} catch (DataAccessException e) {

				description.setMessage(PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getCode());
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PacketReceiverConstant.ERROR_IN_PACKET_RECIVER
								+ PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getMessage()
								+ ExceptionUtils.getStackTrace(e));
				throw new PacketReceiverAppException(PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getMessage());

			} finally {

				String eventId = "";
				String eventName = "";
				String eventType = "";
				eventId = storageFlag ? EventId.RPR_407.toString() : EventId.RPR_405.toString();
				eventName = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventName.ADD.toString()
						: EventName.EXCEPTION.toString();
				eventType = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventType.BUSINESS.toString()
						: EventType.SYSTEM.toString();

				/** Module-Id can be Both Success/Error code */
				String moduleId = isTransactionSuccessful
						? PlatformSuccessMessages.PACKET_RECEIVER_VALIDATION_SUCCESS.getCode()
						: description.getCode();
				String moduleName = ModuleName.PACKET_RECEIVER.toString();
				auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName,
						eventType, moduleId, moduleName, registrationId);

			}

			if (storageFlag) {
				messageDTO.setIsValid(true);
			}

		}

		return messageDTO;
	}

	/**
	 * validate packet with reg entity.
	 * 
	 * @param description
	 */
	private void validatePacketWithSync(SyncRegistrationEntity regEntity, String registrationId,
			LogDescription description) {

		if (regEntity == null) {
			description.setMessage(PlatformErrorMessages.RPR_PKR_PACKET_NOT_YET_SYNC.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PKR_PACKET_NOT_YET_SYNC.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PKR_PACKET_NOT_YET_SYNC.getMessage());
			throw new PacketNotSyncException(PlatformErrorMessages.RPR_PKR_PACKET_NOT_YET_SYNC.getMessage());
		}
	}

	/**
	 * Store packet.
	 *
	 * @param dto
	 *            the InternalRegistrationStatusDto
	 * @param regEntity
	 *            the SyncRegistrationEntity
	 * @param stageName
	 *            the stage name
	 * @param description
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	// TODO remove InternalRegistrationStatusDto from method param and return new InternalRegistrationStatusDto,ignore for sonar
	private boolean storePacket(String stageName, SyncRegistrationEntity regEntity, InternalRegistrationStatusDto dto,
			LogDescription description) {
		Boolean storageFlag = false;
		int iteration = getIterationForSyncRecord(regEntity);
		dto = registrationStatusService.getRegistrationStatus(regEntity.getRegistrationId(),
				regEntity.getRegistrationType(), iteration, regEntity.getWorkflowInstanceId());
		if (dto == null) {
			dto = new InternalRegistrationStatusDto();
			dto.setRetryCount(0);
		} else {
			int retryCount = dto.getRetryCount() != null ? dto.getRetryCount() + 1 : 1;
			dto.setRetryCount(retryCount);

		}
		dto.setRegistrationId(regEntity.getRegistrationId());
		dto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PACKET_RECEIVER.toString());
		dto.setLatestTransactionTimes(DateUtils.getUTCCurrentDateTime());
		dto.setRegistrationStageName(stageName);
		dto.setRegistrationType(regEntity.getRegistrationType());
		dto.setReferenceRegistrationId(null);
		dto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
		dto.setLangCode("eng");
		dto.setStatusComment(StatusUtil.PACKET_RECEIVED.getMessage());
		dto.setSubStatusCode(StatusUtil.PACKET_RECEIVED.getCode());
		dto.setReProcessRetryCount(0);
		dto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
		dto.setIsActive(true);
		dto.setCreatedBy(USER);
		dto.setIsDeleted(false);
		dto.setSource(regEntity.getSource());
        dto.setIteration(iteration);
        dto.setWorkflowInstanceId(regEntity.getWorkflowInstanceId());

		/** Module-Id can be Both Success/Error code */
		String moduleId = PlatformSuccessMessages.PACKET_RECEIVER_VALIDATION_SUCCESS.getCode();
		String moduleName = ModuleName.PACKET_RECEIVER.toString();
		registrationStatusService.addRegistrationStatus(dto, moduleId, moduleName);
		storageFlag = true;
		description
				.setMessage(PacketReceiverConstant.PACKET_SUCCESS_UPLOADED_IN_PACKET_RECIVER + dto.getRegistrationId());
		return storageFlag;
	}

	/**
	 * Validate packet format.
	 *
	 * @param fileOriginalName
	 *            the file original name
	 * @param description
	 * @param registrationId
	 *            the reg id
	 */
	private void validatePacketFormat(String fileOriginalName, String registrationId, LogDescription description) {
		if (!(fileOriginalName.endsWith(getExtention()))) {
			description.setMessage(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_FORMAT.getMessage() + registrationId);
			description.setCode(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_FORMAT.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PKR_INVALID_PACKET_FORMAT.getMessage());
			throw new PacketNotValidException(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_FORMAT.getMessage());
		}

	}

	/**
	 * Scan file.
	 *
	 * @param input
	 *            the input packet byte
	 * @param description
	 */
	private boolean scanFile(final byte[] input, RegistrationExceptionMapperUtil registrationExceptionMapperUtil,
			String registrationId, InternalRegistrationStatusDto dto, LogDescription description) throws IOException {
		try {
			InputStream inputStream = new ByteArrayInputStream(input);
			boolean isInputFileClean = virusScannerService.scanFile(inputStream);

			if (!isInputFileClean) {
				description.setMessage(PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCAN_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCAN_FAILED.getCode());
				dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				dto.setStatusComment(StatusUtil.VIRUS_SCANNER_FAILED.getMessage());
				dto.setSubStatusCode(StatusUtil.VIRUS_SCANNER_FAILED.getCode());
				dto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCAN_FAILED_EXCEPTION));
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCAN_FAILED.getMessage());
			}
			return isInputFileClean;
		} catch (VirusScannerException e) {
			description.setMessage(PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getCode());
			dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			dto.setStatusComment(trimExpMessage.trimExceptionMessage(
					StatusUtil.VIRUS_SCANNER_SERVICE_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.VIRUS_SCANNER_SERVICE_NOT_ACCESSIBLE.getCode());
			dto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCANNER_SERVICE_FAILED));

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.PRP_PKR_PACKET_VIRUS_SCANNER_SERVICE_FAILED.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			return false;
		}

	}

	/**
	 * Gets the max file size.
	 *
	 * @return the max file size
	 */
	public long getMaxFileSize() {
		int maxFileSize = Integer.parseInt(fileSize);
		return maxFileSize * 1024L * 1024;
	}

	/**
	 * Gets the extention.
	 *
	 * @return the extention
	 */
	public String getExtention() {
		return extention;
	}

	/**
	 * Checks if registration id is already present in registration status table.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the boolean
	 */
	private Boolean isDuplicatePacket(String registrationId, SyncRegistrationEntity syncRegistrationEntity) {
		return StringUtils.isNullOrEmpty(syncRegistrationEntity.getAdditionalInfoReqId())
				&& mainProcesses.contains(syncRegistrationEntity.getRegistrationType())
				&& registrationStatusService.getRegistrationStatus(registrationId,
				syncRegistrationEntity.getRegistrationType(),
				getIterationForSyncRecord(syncRegistrationEntity), syncRegistrationEntity.getWorkflowInstanceId()) != null;
	}

	/**
	 * Checks if is external status resend.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the boolean
	 */
	public Boolean isExternalStatusResend(String registrationId) {
		List<RegistrationStatusSubRequestDto> regIds = new ArrayList<>();
		RegistrationStatusSubRequestDto registrationStatusSubRequestDto = new RegistrationStatusSubRequestDto();
		registrationStatusSubRequestDto.setRegistrationId(registrationId);
		regIds.add(registrationStatusSubRequestDto);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "PacketReceiverServiceImpl::isExternalStatusResend()::entry");

		List<RegistrationStatusDto> registrationExternalStatusCode = registrationStatusService.getByIds(regIds);

		String mappedValue = registrationExternalStatusCode.get(0).getStatusCode();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "PacketReceiverServiceImpl::isExternalStatusResend()::exit");
		return (mappedValue.equals(RESEND));
	}

	/**
	 * Validate hash code.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param inputStream
	 *            the input stream
	 * @param description
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void validateHashCode(byte[] isbytearray, SyncRegistrationEntity regEntity, String registrationId,
			LogDescription description) throws IOException, NoSuchAlgorithmException {
		// TO-DO testing
		String hashSequence = HMACUtils2.digestAsPlainText(isbytearray);
		String packetHashSequence = regEntity.getPacketHashValue();
		if (!(MessageDigest.isEqual(packetHashSequence.getBytes(), hashSequence.getBytes()))) {
			description.setMessage(PlatformErrorMessages.UNEQUAL_PACKET_HASH_PR.getMessage());
			description.setCode(PlatformErrorMessages.UNEQUAL_PACKET_HASH_PR.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH.getMessage());
			throw new UnequalHashSequenceException(
					PlatformErrorMessages.RPR_PKR_PACKET_HASH_NOT_EQUALS_SYNCED_HASH.getMessage());
		}
	}

	/**
	 * Validate packet size.
	 *
	 * @param length
	 *            the length
	 * @param registrationId
	 *            the regid
	 */
	private void validatePacketSize(long length, SyncRegistrationEntity regEntity, String registrationId,
			LogDescription description) {

		long packetSize = regEntity.getPacketSize().longValue();
		if (length != packetSize) {
			description.setMessage(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE_SYNCED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE_SYNCED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE_SYNCED.getMessage());
			throw new PacketSizeNotInSyncException(
					PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE_SYNCED.getMessage());
		}

		if (length > getMaxFileSize()) {
			description.setMessage(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE.getMessage());
			throw new FileSizeExceedException(PlatformErrorMessages.RPR_PKR_INVALID_PACKET_SIZE.getMessage());
		}

	}

	@Override
	public MessageDTO processPacket(File file) {
		LogDescription description = new LogDescription();
		MessageDTO messageDTO = new MessageDTO();
		RegistrationExceptionMapperUtil registrationExceptionMapperUtil = new RegistrationExceptionMapperUtil();
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		boolean scanningFlag;
		boolean isTransactionSuccessful = false;
		String fileOriginalName = file.getName();
		String packetId = fileOriginalName.split("\\.")[0];

		SyncRegistrationEntity regEntity = syncRegistrationService.findByPacketId(packetId);
		String registrationId = regEntity.getRegistrationId();

		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus(registrationId,
				regEntity.getRegistrationType(), getIterationForSyncRecord(regEntity), regEntity.getWorkflowInstanceId());
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "PacketReceiverServiceImpl::processPacket()::entry");
		messageDTO.setRid(registrationId);

		messageDTO.setReg_type(regEntity.getRegistrationType());
		if (dto != null) {
			messageDTO.setIteration(dto.getIteration());
		}
		messageDTO.setSource(regEntity.getSource());
		messageDTO.setWorkflowInstanceId(regEntity.getWorkflowInstanceId());
		try (InputStream encryptedInputStream = FileUtils.newInputStream(file.getAbsolutePath())) {
			final byte[] encryptedByteArray = IOUtils.toByteArray(encryptedInputStream);
			scanningFlag = scanFile(encryptedByteArray, registrationExceptionMapperUtil,
					registrationId, dto, description);
			if (scanningFlag) {
				fileManager.put(packetId, new ByteArrayInputStream(encryptedByteArray),
						DirectoryPathDto.LANDING_ZONE);
				dto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				dto.setStatusComment(StatusUtil.PACKET_UPLOADED_TO_LANDING_ZONE.getMessage());
				dto.setSubStatusCode(StatusUtil.PACKET_UPLOADED_TO_LANDING_ZONE.getCode());
				dto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				messageDTO.setIsValid(Boolean.TRUE);
				isTransactionSuccessful = true;
				dto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PACKET_RECEIVER.toString());
				description.setMessage(
						PlatformSuccessMessages.RPR_PKR_PACKET_RECEIVER.getMessage() + "-------" + registrationId);
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"PacketReceiverServiceImpl::success");

			}

		} catch (IOException e) {
			messageDTO.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			dto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			dto.setStatusComment(trimExpMessage.trimExceptionMessage(
					StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			dto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (DataAccessException e) {
			messageDTO.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getCode());
			dto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			dto.setStatusComment(trimExpMessage.trimExceptionMessage(
					StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			dto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			dto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PacketReceiverConstant.ERROR_IN_PACKET_RECIVER
							+ PlatformErrorMessages.RPR_PKR_DATA_ACCESS_EXCEPTION.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		}  finally {
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PKR_PACKET_RECEIVER.getCode()
					: description.getCode();
			String moduleName = ModuleName.PACKET_RECEIVER.toString();
			registrationStatusService.updateRegistrationStatus(dto, moduleId, moduleName);
			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_407.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventName.ADD.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);

		}

		return messageDTO;
	}

	private int getIterationForSyncRecord(SyncRegistrationEntity regEntity) {
		if (mainProcesses.contains(regEntity.getRegistrationType()))
			return 1;

		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByReqId(regEntity.getAdditionalInfoReqId());
		if (additionalInfoRequestDto == null)
			throw new AdditionalInfoIdNotFoundException();

		return additionalInfoRequestDto.getAdditionalInfoIteration();
    }

}