package io.mosip.registration.processor.stages.packet.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.logger.spi.Logger;
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
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainRequestDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDataSyncRequestDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.dto.PacketValidationDto;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.CheckSumValidation;
import io.mosip.registration.processor.stages.utils.DocumentUtility;
import io.mosip.registration.processor.stages.utils.FilesValidation;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@Service
@Transactional
public class PacketValidateProcessor {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = "\\";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidateProcessor.class);

	@Autowired
	private PacketManager fileSystemManager;

	@Autowired
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/** The Constant APPLICANT_TYPE. */
	public static final String APPLICANT_TYPE = "applicantType";

	private static final String VALIDATIONFALSE = "false";

	/** The Constant APPROVED. */
	public static final String APPROVED = "APPROVED";

	/** The Constant REJECTED. */
	public static final String REJECTED = "REJECTED";

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private Environment env;

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	DocumentUtility documentUtility;

	@Autowired
	IdObjectsSchemaValidationOperationMapper idObjectsSchemaValidationOperationMapper;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired(required = false)
	@Qualifier("referenceValidator")
	@Lazy
	IdObjectValidator idObjectValidator;

	@Autowired
	private Utilities utility;

	@Autowired
	ApplicantTypeDocument applicantTypeDocument;

	@Autowired
	private IdRepoService idRepoService;

	@Autowired
	private AuditUtility auditUtility;

	/** The sync registration service. */
	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	private static final String INDIVIDUALBIOMETRICS = "individualBiometrics";

	private static final String VALUE = "value";

	private static final String VALIDATESCHEMA = "registration.processor.validateSchema";

	private static final String VALIDATEFILE = "registration.processor.validateFile";

	private static final String VALIDATECHECKSUM = "registration.processor.validateChecksum";

	private static final String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";

	private static final String VALIDATEMASTERDATA = "registration.processor.validateMasterData";

	private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";

	private static final String PRE_REG_ID = "mosip.pre-registration.datasync.store";
	private static final String VERSION = "1.0";
	private static final String CREATED_BY = "MOSIP_SYSTEM";

	String registrationId = null;

	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	public MessageDTO process(MessageDTO object, String stageName) {
		TrimExceptionMessage trimMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		PacketValidationDto packetValidationDto = new PacketValidationDto();
		String preRegId = null;

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		try {

			object.setMessageBusAddress(MessageBusAddress.PACKET_VALIDATOR_BUS_IN);
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.FALSE);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "PacketValidateProcessor::process()::entry");
			registrationId = object.getRid();
			packetValidationDto.setTransactionSuccessful(false);

			registrationStatusDto = registrationStatusService.getRegistrationStatus(registrationId);
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.VALIDATE_PACKET.toString());
			registrationStatusDto.setRegistrationStageName(stageName);
			boolean isValidSupervisorStatus = isValidSupervisorStatus();
			if (isValidSupervisorStatus) {
			InputStream packetMetaInfoStream = fileSystemManager.getFile(registrationId,
					PacketFiles.PACKET_META_INFO.name());
			PacketMetaInfo packetMetaInfo = (PacketMetaInfo) JsonUtil.inputStreamtoJavaObject(packetMetaInfoStream,
					PacketMetaInfo.class);
			IdentityIteratorUtil identityIteratorUtil = new IdentityIteratorUtil();
			Boolean isValid = validate(registrationStatusDto, packetMetaInfo, object, identityIteratorUtil,
					packetValidationDto);
			if (isValid) {
			
				// save audit details
				Runnable r = () -> {
					try {
						auditUtility.saveAuditDetails(registrationId);
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
				registrationStatusDto.setStatusComment(StatusUtil.PACKET_STRUCTURAL_VALIDATION_SUCCESS.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_STRUCTURAL_VALIDATION_SUCCESS.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				// ReverseDataSync

				preRegId = identityIteratorUtil.getFieldValue(packetMetaInfo.getIdentity().getMetaData(),
						JsonConstant.PREREGISTRATIONID);
				reverseDataSync(preRegId, registrationId, description, packetValidationDto);

				object.setRid(registrationStatusDto.getRegistrationId());
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
				object.setIsValid(Boolean.FALSE);
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				description.setMessage("File validation(" + packetValidationDto.isFilesValidated()
						+ ")/Checksum validation(" + packetValidationDto.isCheckSumValidated() + ")"
						+ "/Applicant Document Validation(" + packetValidationDto.isApplicantDocumentValidation() + ")"
						+ "/Schema Validation(" + packetValidationDto.isSchemaValidated() + ")"
						+ "/Master Data Validation(" + packetValidationDto.isMasterDataValidation() + ")"
						+ "/MandatoryField Validation(" + packetValidationDto.isMandatoryValidation() + ")"
						+ "/isRidAndType Sync Validation(" + packetValidationDto.isRIdAndTypeSynched() + ")"
						+ " failed for registrationId " + registrationId);
				packetValidationDto.setTransactionSuccessful(false);
				registrationStatusDto.setRetryCount(retryCount);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto.setStatusComment(packetValidationDto.getPacketValidaionFailure());
				registrationStatusDto.setSubStatusCode(packetValidationDto.getPacketValidatonStatusCode());

				description.setMessage(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.STRUCTURAL_VALIDATION_FAILED.getCode());

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), description.getCode() + " -- " + registrationId,
						description.getMessage());

				}
			} else {
				registrationStatusDto.setLatestTransactionStatusCode(
						registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_REJECTED));
				object.setIsValid(Boolean.FALSE);
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
			registrationStatusDto.setUpdatedBy(USER);

		} catch (FSAdapterException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.FS_ADAPTER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.FS_ADAPTER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.FSADAPTER_EXCEPTION));
			packetValidationDto.setTransactionSuccessful(false);
			description.setMessage(PlatformErrorMessages.RPR_PVM_PACKET_STORE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PVM_PACKET_STORE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_PVM_PACKET_STORE_NOT_ACCESSIBLE.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(trimMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			description.setCode(PlatformErrorMessages.RPR_PVM_API_RESOUCE_ACCESS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PVM_API_RESOUCE_ACCESS_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());

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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());

		} catch (TablenotAccessibleException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(),
					ExceptionUtils.getStackTrace(e));

		} catch (BaseCheckedException e) {
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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());

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
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());

		} finally {

			if (object.getInternalError()) {
				registrationStatusDto.setUpdatedBy(USER);
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setRetryCount(retryCount);
			}
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

	private boolean isValidSupervisorStatus() {
		SyncRegistrationEntity regEntity = syncRegistrationService.findByRegistrationId(registrationId);
		if (regEntity.getSupervisorStatus().equalsIgnoreCase(APPROVED)) {
			return true;

		} else if (regEntity.getSupervisorStatus().equalsIgnoreCase(REJECTED)) {
			return false;
		}
		return false;
	}

	private boolean validate(InternalRegistrationStatusDto registrationStatusDto, PacketMetaInfo packetMetaInfo,
			MessageDTO object, IdentityIteratorUtil identityIteratorUtil, PacketValidationDto packetValidationDto)
			throws IOException, ApisResourceAccessException, JSONException, org.json.simple.parser.ParseException,
			RegistrationProcessorCheckedException, IdObjectValidationFailedException, IdObjectIOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {
		Long uin = null;
		JSONObject demographicIdentity = null;
		String registrationId = registrationStatusDto.getRegistrationId();

		if (!fileValidation(packetMetaInfo, registrationStatusDto, packetValidationDto)) {
			return false;
		}

		Identity identity = packetMetaInfo.getIdentity();
		InputStream idJsonStream = fileSystemManager.getFile(registrationId,
				PacketFiles.DEMOGRAPHIC.name() + FILE_SEPARATOR + PacketFiles.ID.name());

		byte[] bytearray = IOUtils.toByteArray(idJsonStream);
		String jsonString = new String(bytearray);
		ObjectMapper mapper = new ObjectMapper();
		JSONObject idObject = mapper.readValue(bytearray, JSONObject.class);

		if (!schemaValidation(idObject, registrationStatusDto, packetValidationDto)) {
			return false;
		}

		if (!checkSumValidation(identity, registrationStatusDto, packetValidationDto))
			return false;

		demographicIdentity = utility.getDemographicIdentityJSONObject(registrationId);
		if (!individualBiometricsValidation(registrationStatusDto, demographicIdentity, packetValidationDto))
			return false;

		List<FieldValue> metadataList = identity.getMetaData();
		if (object.getReg_type().toString().equalsIgnoreCase(RegistrationType.UPDATE.toString())
				|| object.getReg_type().toString().equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())) {
			uin = utility.getUIn(registrationId);
			if (uin == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PVM_INVALID_UIN.getMessage());
			JSONObject jsonObject = utility.retrieveIdrepoJson(uin);
			if (jsonObject == null)
				throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
			String status = utility.retrieveIdrepoJsonStatus(uin);
			if (object.getReg_type().toString().equalsIgnoreCase(RegistrationType.UPDATE.toString())
					&& status.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
				throw new RegistrationProcessorCheckedException(
						PlatformErrorMessages.RPR_PVM_UPDATE_DEACTIVATED.getCode(), "UIN is Deactivated");

			}
		}
		if (!applicantDocumentValidation(jsonString, registrationId, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(" applicant document validation failed ");
			return false;
		}
		if (!masterDataValidation(jsonString, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(" master data validation failed ");
			return false;
		}
		// check if uin is in idrepisitory
		if (RegistrationType.UPDATE.name().equalsIgnoreCase(object.getReg_type().name())
				|| RegistrationType.RES_UPDATE.name().equalsIgnoreCase(object.getReg_type().name())) {

			if (!uinPresentInIdRepo(String.valueOf(uin))) {
				packetValidationDto.setPacketValidaionFailure(StatusUtil.UIN_NOT_FOUND_IDREPO.getMessage());
				return false;
			}
		}

		if (RegistrationType.NEW.name().equalsIgnoreCase(registrationStatusDto.getRegistrationType())
				&& !mandatoryValidation(registrationStatusDto, packetValidationDto)) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			return false;
		}
		// Check RegId & regType are same or not From PacketMetaInfo by comparing with
		// Sync list table
		return validateRegIdAndTypeFromSyncTable(object.getRid(), metadataList, identityIteratorUtil,
				packetValidationDto);
	}

	private boolean uinPresentInIdRepo(String uin) throws ApisResourceAccessException, IOException {
		return idRepoService.findUinFromIdrepo(uin, utility.getGetRegProcessorDemographicIdentity()) != null;

	}

	private boolean validateRegIdAndTypeFromSyncTable(String regId, List<FieldValue> metadataList,
			IdentityIteratorUtil identityIteratorUtil, PacketValidationDto packetValidationDto) {
		String regType = identityIteratorUtil.getFieldValue(metadataList, JsonConstant.REGISTRATIONTYPE);
		List<SyncRegistrationEntity> syncRecordList = registrationRepositary.getSyncRecordsByRegIdAndRegType(regId,
				regType.toUpperCase());

		if (syncRecordList != null && !syncRecordList.isEmpty()) {
			packetValidationDto.setRIdAndTypeSynched(true);
			return packetValidationDto.isRIdAndTypeSynched();
		}
		packetValidationDto.setPacketValidaionFailure(StatusUtil.RID_AND_TYPE_SYNC_FAILED.getMessage());
		packetValidationDto.setPacketValidatonStatusCode(StatusUtil.RID_AND_TYPE_SYNC_FAILED.getCode());
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), regId,
				PlatformErrorMessages.RPR_PVM_RECORD_NOT_MATCHED_FROM_SYNC_TABLE.getCode(),
				PlatformErrorMessages.RPR_PVM_RECORD_NOT_MATCHED_FROM_SYNC_TABLE.getMessage());
		return packetValidationDto.isRIdAndTypeSynched();
	}

	private boolean mandatoryValidation(InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto) throws IOException, JSONException,
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException {
		if (env.getProperty(VALIDATEMANDATORY).trim().equalsIgnoreCase(VALIDATIONFALSE))
			return true;
		MandatoryValidation mandatoryValidation = new MandatoryValidation(fileSystemManager, registrationStatusDto,
				utility);
		packetValidationDto.setMandatoryValidation(
				mandatoryValidation.mandatoryFieldValidation(registrationStatusDto.getRegistrationId()));
		if (!packetValidationDto.isMandatoryValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MANDATORY_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MANDATORY_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isMandatoryValidation();
	}

	private boolean schemaValidation(JSONObject idObject, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, IOException, IdObjectValidationFailedException, IdObjectIOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {

		if (env.getProperty(VALIDATESCHEMA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setSchemaValidated(true);
			return packetValidationDto.isSchemaValidated();
		}
		IdObjectValidatorSupportedOperations operation = idObjectsSchemaValidationOperationMapper
				.getOperation(registrationStatusDto.getRegistrationId());
		packetValidationDto.setSchemaValidated(idObjectValidator.validateIdObject(idObject, operation));

		if (!packetValidationDto.isSchemaValidated()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.SCHEMA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.SCHEMA_VALIDATION_FAILED.getCode());

		}

		return packetValidationDto.isSchemaValidated();

	}

	private boolean fileValidation(PacketMetaInfo packetMetaInfo, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto)
			throws PacketDecryptionFailureException, ApisResourceAccessException, IOException {
		if (env.getProperty(VALIDATEFILE).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setFilesValidated(true);
			return packetValidationDto.isFilesValidated();
		}
		FilesValidation filesValidation = new FilesValidation(fileSystemManager, registrationStatusDto);
		packetValidationDto.setFilesValidated(
				filesValidation.filesValidation(registrationStatusDto.getRegistrationId(), packetMetaInfo));
		if (!packetValidationDto.isFilesValidated()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.FILE_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.FILE_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isFilesValidated();

	}

	private boolean checkSumValidation(Identity identity, InternalRegistrationStatusDto registrationStatusDto,
			PacketValidationDto packetValidationDto) throws IOException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException {
		if (env.getProperty(VALIDATECHECKSUM).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setCheckSumValidated(true);
			return packetValidationDto.isCheckSumValidated();
		}
		CheckSumValidation checkSumValidation = new CheckSumValidation(fileSystemManager, registrationStatusDto);
		packetValidationDto.setCheckSumValidated(
				checkSumValidation.checksumvalidation(registrationStatusDto.getRegistrationId(), identity));
		if (!packetValidationDto.isCheckSumValidated()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.CHECKSUM_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.CHECKSUM_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isCheckSumValidated();

	}

	private boolean individualBiometricsValidation(InternalRegistrationStatusDto registrationStatusDto,
			JSONObject demographicIdentity, PacketValidationDto packetValidationDto)
			throws RegistrationProcessorCheckedException {
		try {
			String registrationId = registrationStatusDto.getRegistrationId();

			JSONObject identityJsonObject = JsonUtil.getJSONObject(demographicIdentity, INDIVIDUALBIOMETRICS);
			if (identityJsonObject != null) {
				String cbefFile = (String) identityJsonObject.get(VALUE);
				if (cbefFile == null) {
					packetValidationDto
							.setPacketValidaionFailure(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getMessage());
					packetValidationDto
							.setPacketValidatonStatusCode(StatusUtil.INDIVIDUAL_BIOMETRIC_VALIDATION_FAILED.getCode());
					return false;
				}
				InputStream idJsonStream = fileSystemManager.getFile(registrationId,
						PacketFiles.BIOMETRIC.name() + FILE_SEPARATOR + cbefFile);
				if (idJsonStream != null)
					return true;

			}
		} catch (IOException | PacketDecryptionFailureException | ApisResourceAccessException
				| io.mosip.kernel.core.exception.IOException e) {
			throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		}

		return true;
	}

	private boolean applicantDocumentValidation(String jsonString, String registrationId,
			PacketValidationDto packetValidationDto)
			throws IOException, ApisResourceAccessException, org.json.simple.parser.ParseException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {
		if (env.getProperty(VALIDATEAPPLICANTDOCUMENT).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setApplicantDocumentValidation(true);
			return packetValidationDto.isApplicantDocumentValidation();
		}
		ApplicantDocumentValidation applicantDocumentValidation = new ApplicantDocumentValidation(utility, env,
				applicantTypeDocument);
		packetValidationDto.setApplicantDocumentValidation(
				applicantDocumentValidation.validateDocument(registrationId, jsonString));
		if (!packetValidationDto.isApplicantDocumentValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.APPLICANT_DOCUMENT_VALIDATION_FAILED.getCode());
		}
		return packetValidationDto.isApplicantDocumentValidation();

	}

	private boolean masterDataValidation(String jsonString, PacketValidationDto packetValidationDto)
			throws ApisResourceAccessException, IOException {
		if (env.getProperty(VALIDATEMASTERDATA).trim().equalsIgnoreCase(VALIDATIONFALSE)) {
			packetValidationDto.setMasterDataValidation(true);
			return packetValidationDto.isMasterDataValidation();
		}
		MasterDataValidation masterDataValidation = new MasterDataValidation(env, registrationProcessorRestService,
				utility);
		packetValidationDto.setMasterDataValidation(masterDataValidation.validateMasterData(jsonString));
		if (!packetValidationDto.isMasterDataValidation()) {
			packetValidationDto.setPacketValidaionFailure(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getMessage());
			packetValidationDto.setPacketValidatonStatusCode(StatusUtil.MASTER_DATA_VALIDATION_FAILED.getCode());
		}

		return packetValidationDto.isMasterDataValidation();

	}

	private void reverseDataSync(String preRegId, String registrationId, LogDescription description,
			PacketValidationDto packetValidationDto) throws IOException {
		try {
			if (registrationId != null) {
				packetValidationDto.setTransactionSuccessful(false);
				MainResponseDTO<ReverseDatasyncReponseDTO> mainResponseDto = null;
				if (preRegId != null && !preRegId.trim().isEmpty()) {
					MainRequestDTO<ReverseDataSyncRequestDTO> mainRequestDto = new MainRequestDTO<>();
					mainRequestDto.setId(PRE_REG_ID);
					mainRequestDto.setVersion(VERSION);
					mainRequestDto.setRequesttime(new Date());
					ReverseDataSyncRequestDTO reverseDataSyncRequestDto = new ReverseDataSyncRequestDTO();
					reverseDataSyncRequestDto.setCreatedBy(CREATED_BY);
					reverseDataSyncRequestDto.setLangCode("eng");
					reverseDataSyncRequestDto.setPreRegistrationIds(Arrays.asList(preRegId));
					reverseDataSyncRequestDto.setCreatedDateTime(new Date());
					reverseDataSyncRequestDto.setUpdateDateTime(new Date());
					reverseDataSyncRequestDto.setUpdateBy(CREATED_BY);
					mainRequestDto.setRequest(reverseDataSyncRequestDto);
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							"PacketValidateProcessor::reverseDataSync()::ReverseDataSync Api call started with request data :"
									+ JsonUtil.objectMapperObjectToJson(mainRequestDto));
					mainResponseDto = (MainResponseDTO) restClientService.postApi(ApiName.REVERSEDATASYNC, "", "",
							mainRequestDto, MainResponseDTO.class);

					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							"\"PacketValidateProcessor::reverseDataSync()::ReverseDataSync Api call ended with response data : "
									+ JsonUtil.objectMapperObjectToJson(mainResponseDto));
					packetValidationDto.setTransactionSuccessful(true);

				}
				if (mainResponseDto != null && mainResponseDto.getErrors() != null
						&& mainResponseDto.getErrors().size() > 0) {
					regProcLogger.error(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
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
					regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							PlatformErrorMessages.REVERSE_DATA_SYNC_SUCCESS.getMessage(), "");
				}

			}

		} catch (ApisResourceAccessException e) {

			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(),
						httpClientException.getResponseBodyAsString() + ExceptionUtils.getStackTrace(e));
				description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getCode());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(),
						httpServerException.getResponseBodyAsString() + ExceptionUtils.getStackTrace(e));
				description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getCode());
			} else {
				regProcLogger.info(LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage(), e.getMessage());
				description.setMessage(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.REVERSE_DATA_SYNC_FAILED.getCode());
			}

		} finally {
			if (packetValidationDto.isTransactionSuccessful())
				description.setMessage("Reverse data sync of Pre-RegistrationIds sucessful");
			String eventId = packetValidationDto.isTransactionSuccessful() ? EventId.RPR_402.toString()
					: EventId.RPR_405.toString();
			String eventName = packetValidationDto.isTransactionSuccessful() ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = packetValidationDto.isTransactionSuccessful() ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			/** Module-Id can be Both Success/Error code */
			String moduleId = packetValidationDto.isTransactionSuccessful()
					? PlatformSuccessMessages.RPR_PKR_PACKET_VALIDATE.getCode()
					: description.getCode();
			String moduleName = ModuleName.PACKET_VALIDATOR.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}

	}

}
