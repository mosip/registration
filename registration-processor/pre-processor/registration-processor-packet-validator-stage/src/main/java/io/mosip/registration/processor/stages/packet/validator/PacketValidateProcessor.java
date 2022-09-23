package io.mosip.registration.processor.stages.packet.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainRequestDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDataSyncRequestDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.NotificationUtility;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@Service
@Transactional
public class PacketValidateProcessor {

	/**
	 * The Constant FILE_SEPARATOR.
	 */
	public static final String FILE_SEPARATOR = "\\";

	/**
	 * The reg proc logger.
	 */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidateProcessor.class);

	@Autowired
	private PacketValidator compositePacketValidator;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private Utilities utility;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * The Constant USER.
	 */
	private static final String USER = "MOSIP_SYSTEM";

	/**
	 * The Constant APPLICANT_TYPE.
	 */
	public static final String APPLICANT_TYPE = "applicantType";

	/**
	 * The Constant APPROVED.
	 */
	public static final String APPROVED = "APPROVED";

	/**
	 * The Constant REJECTED.
	 */
	public static final String REJECTED = "REJECTED";

	/**
	 * The registration status service.
	 */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/**
	 * The core audit request builder.
	 */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private AuditUtility auditUtility;

	/**
	 * The sync registration service.
	 */
	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	private static final String PRE_REG_ID = "mosip.pre-registration.datasync.store";
	private static final String VERSION = "1.0";

	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Value("${mosip.notificationtype}")
	private String notificationTypes;

	@Autowired
	private Decryptor decryptor;

	@Autowired
	private NotificationUtility notificationUtility;

	public MessageDTO process(MessageDTO object, String stageName) {
		TrimExceptionMessage trimMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		PacketValidationDto packetValidationDto = new PacketValidationDto();
		String registrationId = null;

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		try {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
			registrationStatusDto.setRegistrationStageName(stageName);
			object.setMessageBusAddress(MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "PacketValidateProcessor::process()::entry");
			registrationId = object.getRid();
			packetValidationDto.setTransactionSuccessful(false);

			registrationStatusDto = registrationStatusService.getRegistrationStatus(
					registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());

			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.VALIDATE_PACKET.toString());
			registrationStatusDto.setRegistrationStageName(stageName);
			boolean isValidSupervisorStatus = isValidSupervisorStatus(object);
			if (isValidSupervisorStatus) {
				Boolean isValid = compositePacketValidator.validate(object.getRid(),
						registrationStatusDto.getRegistrationType(), packetValidationDto);
				if (isValid) {
					// save audit details
					InternalRegistrationStatusDto finalRegistrationStatusDto = registrationStatusDto;
					String finalRegistrationId = registrationId;
					Runnable r = () -> {
						try {
							auditUtility.saveAuditDetails(finalRegistrationId,
									finalRegistrationStatusDto.getRegistrationType());
						} catch (Exception e) {
							regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
									LoggerFileConstant.REGISTRATIONID.toString(),
									description.getCode() + " Inside Runnable ", "");

						}
					};
					ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
					es.submit(r);
					es.shutdown();
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
					object.setIsValid(Boolean.TRUE);
					registrationStatusDto
							.setStatusComment(StatusUtil.PACKET_STRUCTURAL_VALIDATION_SUCCESS.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_STRUCTURAL_VALIDATION_SUCCESS.getCode());
					registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					// ReverseDataSync
					reverseDataSync(registrationId, registrationStatusDto.getRegistrationType(), description,
							packetValidationDto);

					packetValidationDto.setTransactionSuccessful(true);
					description.setMessage(
							PlatformSuccessMessages.RPR_PKR_PACKET_VALIDATE.getMessage() + " -- " + registrationId);
					description.setCode(PlatformSuccessMessages.RPR_PKR_PACKET_VALIDATE.getCode());

					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							description.getCode() + description.getMessage());

				} else {
					registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PACKET_STRUCTURAL_VALIDATION_FAILED));
					int retryCount = registrationStatusDto.getRetryCount() != null
							? registrationStatusDto.getRetryCount() + 1
							: 1;
					description.setMessage(packetValidationDto.getPacketValidatonStatusCode() + " : "
							+ packetValidationDto.getPacketValidaionFailureMessage());
					packetValidationDto.setTransactionSuccessful(false);
					registrationStatusDto.setRetryCount(retryCount);
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					registrationStatusDto.setStatusComment(packetValidationDto.getPacketValidaionFailureMessage());
					registrationStatusDto.setSubStatusCode(packetValidationDto.getPacketValidatonStatusCode());

					description.setMessage(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage());
					description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());

					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(),
							description.getCode() + " -- " + registrationId, description.getMessage());

				}
			} else {
				registrationStatusDto.setLatestTransactionStatusCode(
						registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_REJECTED));
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				packetValidationDto.setTransactionSuccessful(false);
				registrationStatusDto.setRetryCount(retryCount);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
				registrationStatusDto.setStatusComment(StatusUtil.PACKET_REJECTED.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_REJECTED.getCode());

				description.setMessage(PlatformErrorMessages.RPR_PVM_PACKET_REJECTED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PVM_PACKET_REJECTED.getCode());

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), description.getCode() + " -- " + registrationId,
						description.getMessage());
			}
			object.setInternalError(Boolean.FALSE);
			registrationStatusDto.setUpdatedBy(USER);
			SyncRegistrationEntity regEntity = syncRegistrationService.findByWorkflowInstanceId(object.getWorkflowInstanceId());
			sendNotification(regEntity, registrationStatusDto, packetValidationDto.isTransactionSuccessful());
		} catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
		} catch (DataAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		} catch (IdentityNotFoundException | IOException exc) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + exc.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage() + exc.getMessage()
							+ ExceptionUtils.getStackTrace(exc));
		} catch (ParsingException exc) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + exc.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PARSE_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage() + exc.getMessage()
							+ ExceptionUtils.getStackTrace(exc));
		} catch (TablenotAccessibleException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
			description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(),
					ExceptionUtils.getStackTrace(e));

		} catch (ApisResourceAccessException e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(),
						httpClientException.getResponseBodyAsString() + ExceptionUtils.getStackTrace(e));
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(),
						httpServerException.getResponseBodyAsString() + ExceptionUtils.getStackTrace(e));
			} else {
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(), e.getMessage());
			}
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REPROCESS.toString());
			registrationStatusDto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			registrationStatusDto.setStatusComment(trimMessage
                    .trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			packetValidationDto.setTransactionSuccessful(false);

			description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getCode());
        } catch (RegistrationProcessorCheckedException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.BASE_CHECKED_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BASE_CHECKED_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BASE_CHECKED_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_PVM_BASE_CHECKED_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PVM_BASE_CHECKED_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_PVM_BASE_CHECKED_EXCEPTION.getMessage() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		} catch (BaseUncheckedException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.BASE_UNCHECKED_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BASE_UNCHECKED_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_PVM_BASE_UNCHECKED_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PVM_BASE_UNCHECKED_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_PVM_BASE_UNCHECKED_EXCEPTION.getMessage() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		} catch (Exception ex) {
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage() + ex.getMessage()
							+ ExceptionUtils.getStackTrace(ex));
		} finally {

			if (object.getInternalError()) {
				registrationStatusDto.setUpdatedBy(USER);
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setRetryCount(retryCount);
				updateErrorFlags(registrationStatusDto, object);
			}
			object.setRid(registrationStatusDto.getRegistrationId());
			/** Module-Id can be Both Success/Error code */
			String moduleId = packetValidationDto.isTransactionSuccessful()
					? PlatformSuccessMessages.RPR_PKR_PACKET_VALIDATE.getCode()
					: description.getCode();
			String moduleName = ModuleName.PACKET_VALIDATOR.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			if (packetValidationDto.isTransactionSuccessful())
				description.setMessage(PlatformSuccessMessages.RPR_PKR_PACKET_VALIDATE.getMessage());
			String eventId = packetValidationDto.isTransactionSuccessful() ? EventId.RPR_402.toString()
					: EventId.RPR_405.toString();
			String eventName = packetValidationDto.isTransactionSuccessful() ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = packetValidationDto.isTransactionSuccessful() ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}

		return object;

	}

		private boolean isValidSupervisorStatus(MessageDTO messageDTO) {
			SyncRegistrationEntity regEntity = syncRegistrationService.findByWorkflowInstanceId(messageDTO.getWorkflowInstanceId());
			if (regEntity.getSupervisorStatus().equalsIgnoreCase(APPROVED)) {
				return true;

			} else if (regEntity.getSupervisorStatus().equalsIgnoreCase(REJECTED)) {
				return false;
			}
			return false;
		}

	@SuppressWarnings("unchecked")
	private void reverseDataSync(String id, String process, LogDescription description,
			PacketValidationDto packetValidationDto) throws IOException, ApisResourceAccessException,
			PacketManagerException, JsonProcessingException, JSONException {

		Map<String, String> metaInfoMap = packetManagerService.getMetaInfo(id, process,
				ProviderStageName.PACKET_VALIDATOR);
		String metadata = metaInfoMap.get(JsonConstant.METADATA);
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			String preRegId = null;
			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					JSONObject jsonObject = (JSONObject) jsonArray.get(i);
					FieldValue fieldValue = objectMapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(JsonConstant.PREREGISTRATIONID)) {
						preRegId = fieldValue.getValue();
						break;
					}

				}
			}
			if (preRegId == null || preRegId.trim().isEmpty()) {
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), id,
						"Pre-registration id not present.",
						"Reverse datasync is not applicable for the registration id");
				return;
			}
			if (id != null) {
				packetValidationDto.setTransactionSuccessful(false);
				MainResponseDTO<ReverseDatasyncReponseDTO> mainResponseDto = null;
				if (preRegId != null && !preRegId.trim().isEmpty()) {
					MainRequestDTO<ReverseDataSyncRequestDTO> mainRequestDto = new MainRequestDTO<>();
					mainRequestDto.setId(PRE_REG_ID);
					mainRequestDto.setVersion(VERSION);
					mainRequestDto.setRequesttime(new Date());
					ReverseDataSyncRequestDTO reverseDataSyncRequestDto = new ReverseDataSyncRequestDTO();
					reverseDataSyncRequestDto.setPreRegistrationIds(Arrays.asList(preRegId));
					mainRequestDto.setRequest(reverseDataSyncRequestDto);
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"PacketValidateProcessor::reverseDataSync()::ReverseDataSync Api call started with request data :"
									+ JsonUtil.objectMapperObjectToJson(mainRequestDto));
					mainResponseDto = (MainResponseDTO) restClientService.postApi(ApiName.REVERSEDATASYNC, "", "",
							mainRequestDto, MainResponseDTO.class);

					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), id,
							"\"PacketValidateProcessor::reverseDataSync()::ReverseDataSync Api call ended with response data : "
									+ JsonUtil.objectMapperObjectToJson(mainResponseDto));
					packetValidationDto.setTransactionSuccessful(true);

				}
				if (mainResponseDto != null && mainResponseDto.getErrors() != null
						&& mainResponseDto.getErrors().size() > 0) {
					regProcLogger.error(LoggerFileConstant.REGISTRATIONID.toString(), id,
							PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(),
							mainResponseDto.getErrors().toString());
					packetValidationDto.setTransactionSuccessful(false);
					description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage());

				} else if (mainResponseDto == null) {
					packetValidationDto.setTransactionSuccessful(false);
					description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage()
							+ " null response from rest client ");
				} else {
					packetValidationDto.setTransactionSuccessful(true);
					regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), id,
							PlatformSuccessMessages.REVERSE_DATA_SYNC_SUCCESS.getMessage(), "");
				}

			}
		}

	}

	private void updateErrorFlags(InternalRegistrationStatusDto registrationStatusDto, MessageDTO object) {
		object.setInternalError(true);
		if (registrationStatusDto.getLatestTransactionStatusCode()
				.equalsIgnoreCase(RegistrationTransactionStatusCode.REPROCESS.toString())) {
			object.setIsValid(true);
		} else {
			object.setIsValid(false);
		}
	}

	private void sendNotification(SyncRegistrationEntity regEntity,
								  InternalRegistrationStatusDto registrationStatusDto, boolean isTransactionSuccessful) {
		try {
			String registrationId = registrationStatusDto.getRegistrationId();
			if (regEntity.getOptionalValues() != null) {
				String[] allNotificationTypes = notificationTypes.split("\\|");
				boolean isProcessingSuccess;
			    InputStream inputStream = new ByteArrayInputStream(regEntity.getOptionalValues());
				InputStream decryptedInputStream = decryptor.decrypt(
						registrationId,
						utility.getRefId(registrationId, regEntity.getReferenceId()),
						inputStream);
				String decryptedData = IOUtils.toString(decryptedInputStream, StandardCharsets.UTF_8);
				RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO = (RegistrationAdditionalInfoDTO) JsonUtils
						.jsonStringToJavaObject(RegistrationAdditionalInfoDTO.class, decryptedData);
				if (isTransactionSuccessful) {
					isProcessingSuccess = true;
					notificationUtility.sendNotification(registrationAdditionalInfoDTO, registrationStatusDto,
							regEntity, allNotificationTypes, isProcessingSuccess);
				} else {
					isProcessingSuccess = false;
					notificationUtility.sendNotification(registrationAdditionalInfoDTO, registrationStatusDto,
							regEntity, allNotificationTypes, isProcessingSuccess);
				}
				boolean isDeleted = syncRegistrationService.deleteAdditionalInfo(regEntity);
				if (isDeleted) {
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							PlatformSuccessMessages.RPR_PKR_ADDITIONAL_INFO_DELETED.getCode() +
									PlatformSuccessMessages.RPR_PKR_ADDITIONAL_INFO_DELETED.getMessage());
				}
			}
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(),
					"Send notification failed for rid - " + registrationStatusDto.getRegistrationId(), ExceptionUtils.getStackTrace(e));
		}
	}

}
