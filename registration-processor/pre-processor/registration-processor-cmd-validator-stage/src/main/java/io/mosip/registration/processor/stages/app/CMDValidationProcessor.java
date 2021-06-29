package io.mosip.registration.processor.stages.app;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.cmdvalidator.CenterValidator;
import io.mosip.registration.processor.stages.cmdvalidator.DeviceValidator;
import io.mosip.registration.processor.stages.cmdvalidator.MachineValidator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Service
@Transactional
public class CMDValidationProcessor {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(CMDValidationProcessor.class);

	private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

	private static final String USER = "MOSIP_SYSTEM";

	public static final String GLOBAL_CONFIG_TRUE_VALUE = "Y";

	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private CenterValidator centerValidator;

	@Autowired
	private MachineValidator machineValidator;

	@Autowired
	private DeviceValidator deviceValidator;

	@Autowired
	private OSIUtils osiUtils;

	@Value("${mosip.registration.gps_device_enable_flag}")
	private String gpsEnable;

	@Value("${mosip.primary-language}")
	private String primaryLanguagecode;

	@Value("${mosip.registration.processor.validate-center}")
	private boolean validateCenter;

	@Value("${mosip.registration.processor.validate-machine}")
	private boolean validateMachine;

	@Value("${mosip.registration.processor.validate-device}")
	private boolean validateDevice;

	public MessageDTO process(MessageDTO object, String stageName) {

		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		String registrationId = "";
		object.setMessageBusAddress(MessageBusAddress.CMD_VALIDATOR_BUS_IN);
		object.setIsValid(Boolean.FALSE);
		object.setInternalError(Boolean.TRUE);

		regProcLogger.debug("process called for registration id {}", registrationId);
		registrationId = object.getRid();

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(registrationId, object.getReg_type(), object.getIteration());

		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.CMD_VALIDATION.toString());
		registrationStatusDto.setRegistrationStageName(stageName);
		try {

			Map<String, String> metaInfo = packetManagerService.getMetaInfo(registrationId,
					registrationStatusDto.getRegistrationType(), ProviderStageName.CMD_VALIDATOR);

			RegOsiDto regOsi = osiUtils.getOSIDetailsFromMetaInfo(metaInfo);

			if ((gpsEnable.equalsIgnoreCase(GLOBAL_CONFIG_TRUE_VALUE))
					&& (regOsi.getLatitude() == null || regOsi.getLongitude() == null
							|| regOsi.getLatitude().trim().isEmpty() || regOsi.getLongitude().trim().isEmpty())) {
				registrationStatusDto.setStatusComment(StatusUtil.GPS_DETAILS_NOT_FOUND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.GPS_DETAILS_NOT_FOUND.getCode());
				object.setIsValid(Boolean.FALSE);
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.PACKET_CMD_VALIDATION_FAILED));
				registrationStatusDto.setRetryCount(retryCount);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto.setUpdatedBy(USER);

				description.setCode(PlatformSuccessMessages.RPR_PKR_CMD_VALIDATE.getCode());
				description.setMessage(PlatformSuccessMessages.RPR_PKR_CMD_VALIDATE.getMessage() + registrationId + "::"
						+ " GPS is not valid");
				String moduleId = description.getCode();
				String moduleName = ModuleName.CMD_VALIDATOR.toString();
				registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
				updateAudit(description, isTransactionSuccessful, moduleId, moduleName, registrationId);
				return object;
			}

			if (validateCenter) {
				centerValidator.validate(primaryLanguagecode, regOsi, registrationStatusDto.getRegistrationId());
			}

			if (validateMachine) {
				machineValidator.validate(regOsi.getMachineId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
						registrationStatusDto.getRegistrationId());
			}

			if (validateDevice) {
				deviceValidator.validate(regOsi, registrationStatusDto.getRegistrationId());
			}

			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			registrationStatusDto.setStatusComment(StatusUtil.CMD_VALIDATION_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.CMD_VALIDATION_SUCCESS.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

			description.setMessage(PlatformSuccessMessages.RPR_PKR_CMD_VALIDATE.getMessage() + " -- " + registrationId);
			description.setCode(PlatformSuccessMessages.RPR_PKR_CMD_VALIDATE.getCode());

			regProcLogger.debug("process call ended for registration id {} {} {}", registrationId,
					description.getCode() + description.getMessage());

			object.setIsValid(Boolean.TRUE);
			object.setInternalError(Boolean.FALSE);
			isTransactionSuccessful = true;
		} catch (PacketManagerException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING,
					StatusUtil.PACKET_MANAGER_EXCEPTION, RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION,
					description, PlatformErrorMessages.PACKET_MANAGER_EXCEPTION, e);
		} catch (DataAccessException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING,
					StatusUtil.DB_NOT_ACCESSIBLE, RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION, description,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);
		} catch (IOException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, StatusUtil.IO_EXCEPTION,
					RegistrationExceptionTypeCode.IOEXCEPTION, description, PlatformErrorMessages.RPR_SYS_IO_EXCEPTION,
					e);
		} catch (ParsingException | JsonProcessingException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.JSON_PARSING_EXCEPTION, RegistrationExceptionTypeCode.PARSE_EXCEPTION, description,
					PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION, e);
		} catch (TablenotAccessibleException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING,
					StatusUtil.DB_NOT_ACCESSIBLE, RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION,
					description, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);
		} catch (ValidationFailedException e) {
			object.setInternalError(false);
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.VALIDATION_FAILED_EXCEPTION, RegistrationExceptionTypeCode.VALIDATION_FAILED_EXCEPTION,
					description, PlatformErrorMessages.CMD_VALIDATION_FAILED, e);
		} catch (BaseUncheckedException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.BASE_UNCHECKED_EXCEPTION, RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION,
					description, PlatformErrorMessages.CMD_BASE_UNCHECKED_EXCEPTION, e);
		} catch (BaseCheckedException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.BASE_CHECKED_EXCEPTION, RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION,
					description, PlatformErrorMessages.CMD_BASE_CHECKED_EXCEPTION, e);
		} catch (Exception e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.UNKNOWN_EXCEPTION_OCCURED, RegistrationExceptionTypeCode.EXCEPTION, description,
					PlatformErrorMessages.CMD_VALIDATION_FAILED, e);
		} finally {
			if (!isTransactionSuccessful) {
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setRetryCount(retryCount);
			}
			registrationStatusDto.setUpdatedBy(USER);
			/** Module-Id can be Both Success/Error code */
			String moduleId = description.getCode();
			String moduleName = ModuleName.CMD_VALIDATOR.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			updateAudit(description, isTransactionSuccessful, moduleId, moduleName, registrationId);
		}

		return object;
	}

	private void updateDTOsAndLogError(InternalRegistrationStatusDto registrationStatusDto,
			RegistrationStatusCode registrationStatusCode, StatusUtil statusUtil,
			RegistrationExceptionTypeCode registrationExceptionTypeCode, LogDescription description,
			PlatformErrorMessages platformErrorMessages, Exception e) {
		registrationStatusDto.setStatusCode(registrationStatusCode.toString());
		registrationStatusDto
				.setStatusComment(trimExpMessage.trimExceptionMessage(statusUtil.getMessage() + e.getMessage()));
		registrationStatusDto.setSubStatusCode(statusUtil.getCode());
		registrationStatusDto.setLatestTransactionStatusCode(
				registrationStatusMapperUtil.getStatusCode(registrationExceptionTypeCode));
		description.setMessage(platformErrorMessages.getMessage());
		description.setCode(platformErrorMessages.getCode());
		regProcLogger.error("Error in  process  for registration id  {} {} {} {} {}",
				registrationStatusDto.getRegistrationId(), description.getCode(), platformErrorMessages.getMessage(),
				e.getMessage(), ExceptionUtils.getStackTrace(e));
	}

	private void updateAudit(LogDescription description, boolean isTransactionSuccessful, String moduleId,
			String moduleName, String registrationId) {
		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
				moduleId, moduleName, registrationId);
	}

}
