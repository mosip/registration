package io.mosip.registration.processor.stages.uingenerator.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.EventId;
import io.mosip.registration.processor.core.constant.EventName;
import io.mosip.registration.processor.core.constant.EventType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.RequestDto;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.entity.RegLostUinDetEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.Utility;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.uingenerator.constants.UINConstants;
import io.mosip.registration.processor.stages.uingenerator.exception.VidCreationException;
import io.mosip.registration.processor.stages.uingenerator.util.UinStatusMessage;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class UinGeneratorStage.
 * 
 * @author Ranjitha Siddegowda
 * @author Rishabh Keshari
 */
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.uingenerator.config",
		"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.stages.config",
		"io.mosip.kernel.packetmanager.config",
		"io.mosip.registration.processor.packet.manager.config",
		"io.mosip.registration.processor.core.kernel.beans"})
public class UinGeneratorStage extends MosipVerticleAPIManager {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UinGeneratorStage.class);
	private static final String RECORD_ALREADY_EXISTS_ERROR = "IDR-IDC-012";
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.uin.generator.";
	private static final String UIN = "UIN";
	private static final String IDREPO_STATUS = "DRAFTED";

	@Autowired
	private Environment env;

	@Autowired
	private IdRepoService idRepoService;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	@Value("${registration.processor.id.repo.vidType}")
	private String vidType;

	@Value("${mosip.commons.packet.manager.schema.validator.convertIdSchemaToDouble:true}")
	private boolean convertIdschemaToDouble;

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The id repo create. */
	@Value("${registration.processor.id.repo.create}")
	private String idRepoCreate;

	/** The id repo update. */
	@Value("${registration.processor.id.repo.update}")
	private String idRepoUpdate;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.uin.generator.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;
	
	@Value("${uingenerator.lost.packet.allowed.update.fields:null}")
	private String updateInfo;

	@Value("${mosip.regproc.uin.generator.trim-whitespaces.simpleType-value:false}")
	private boolean trimWhitespaces;

	@Value("#{${registration.processor.additional-process.category-mapping:{:}}}")
	private Map<String,String> additionalProcessCategoryMapping;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** Mosip router for APIs */
	@Autowired
	private MosipRouter router;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private IdrepoDraftService idrepoDraftService;

	/** The registration processor rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

	/** The demographic dedupe repository. */
	@Autowired
	private BasePacketRepository<RegLostUinDetEntity, String> regLostUinDetEntity;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The utilities. */
	@Autowired
	private Utilities utilities;

    /** The utility. */
    @Autowired
    private Utility utility;

	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Autowired
	private ABISHandlerUtil aBISHandlerUtil;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private IdSchemaUtil idSchemaUtil;

	@Autowired
	private ObjectMapper objectMapper;

	private TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MessageDTO process(MessageDTO object) {
		boolean isTransactionSuccessful = Boolean.FALSE;
		object.setMessageBusAddress(MessageBusAddress.UIN_GENERATION_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.TRUE);
		LogDescription description = new LogDescription();
		String registrationId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "UinGeneratorStage::process()::entry");
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
		try {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.UIN_GENERATOR.toString());
			registrationStatusDto.setRegistrationStageName(getStageName());

			if ((RegistrationType.LOST.toString()).equalsIgnoreCase(object.getReg_type())) {
				String lostPacketRegId = object.getRid();
				String matchedRegId = regLostUinDetEntity.getLostUinMatchedRegIdByWorkflowId(object.getWorkflowInstanceId());
				
				if (matchedRegId != null) {
					regProcLogger.info("Match for lostPacketRegId"+lostPacketRegId +"is "+matchedRegId);
					lostAndUpdateUin(lostPacketRegId, matchedRegId, registrationStatusDto.getRegistrationType(), object, description);
				}
			} else {
				if (RegistrationType.NEW.toString().equalsIgnoreCase(object.getReg_type())) {
					// Draft was created and populated in create_draft stage; no draft operations here for NEW packets
					registrationStatusDto.setStatusComment(StatusUtil.UIN_GENERATED_SUCCESS.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.UIN_GENERATED_SUCCESS.getCode());
					isTransactionSuccessful = true;
					registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					description.setMessage(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getMessage());
					description.setCode(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode());
					description.setTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				} else if (RegistrationType.UPDATE.toString().equalsIgnoreCase(object.getReg_type())
						|| RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(object.getReg_type())
						|| RegistrationType.UPDATE.toString().equalsIgnoreCase(utilities.getInternalProcess(additionalProcessCategoryMapping, object.getReg_type()))) {
					// Draft was created and populated in create_draft stage; no draft operations here for UPDATE packets
					isTransactionSuccessful = true;
					description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					description.setStatusComment(StatusUtil.UIN_DATA_UPDATION_SUCCESS.getMessage());
					description.setSubStatusCode(StatusUtil.UIN_DATA_UPDATION_SUCCESS.getCode());
					description.setMessage(StatusUtil.UIN_DATA_UPDATION_SUCCESS.getMessage() + " for registration Id: " + registrationId);
					description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
					object.setIsValid(Boolean.TRUE);
				} else {
					// ACTIVATED / DEACTIVATED — fetch UIN and identity fields for ID Repo draft update
					ExecutorService uinExecutor = Executors.newVirtualThreadPerTaskExecutor();
					CompletableFuture<String> uinFuture = CompletableFuture.supplyAsync(() -> {
						try {
							return utility.getUIn(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);
						} catch (Exception e) { throw new CompletionException(e); }
					}, uinExecutor);
					String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId, MappingJsonConstants.IDSCHEMA_VERSION, registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);
					List<String> defaultFields = idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion));
					Map<String, String> fieldMap = packetManagerService.getFields(registrationId,
							defaultFields, registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);
					String uinField;
					try {
						uinField = uinFuture.join();
					} catch (CompletionException e) {
						Throwable cause = e.getCause();
						while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
						sneakyThrow(cause);
						throw new RuntimeException(); // unreachable
					} finally {
						uinExecutor.close();
					}
					JSONObject demographicIdentity = new JSONObject();
					demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION, convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
					loadDemographicIdentity(fieldMap, demographicIdentity);
					IdResponseDTO idResponseDTO = new IdResponseDTO();
					if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(object.getReg_type())) {
						isTransactionSuccessful = reActivateUin(idResponseDTO, registrationId, uinField, object,
								demographicIdentity, description);
					} else if (RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(object.getReg_type())) {
						deactivateUin(registrationId, uinField, object, demographicIdentity, description);
					}
				}
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());
			registrationStatusDto.setUpdatedBy(UINConstants.USER);

		} catch (io.mosip.kernel.core.util.exception.JsonProcessingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
			isTransactionSuccessful = false;
			description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		}catch (PacketManagerNonRecoverableException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION));
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());

		}  catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.PROCESSING.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (ApisResourceAccessException ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.PROCESSING.toString() + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			description.setMessage(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + ex.getMessage()));
			description.setCode(PlatformErrorMessages.RPR_UGS_API_RESOURCE_EXCEPTION.getCode());

		} catch (IOException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
		} catch (IdrepoDraftException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.PROCESSING.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.IDREPO_DRAFT_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_EXCEPTION));
			description.setMessage(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (IdrepoDraftReprocessableException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.PROCESSING.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(
							StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION));
			description.setMessage(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (Exception ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.PROCESSING.toString() + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getCode());
		} finally {
			if (description.getStatusComment() != null)
				registrationStatusDto.setStatusComment(description.getStatusComment());
			if (description.getStatusCode() != null)
				registrationStatusDto.setStatusCode(description.getStatusCode());
			if (description.getSubStatusCode() != null)
				registrationStatusDto.setSubStatusCode(description.getSubStatusCode());
			if (description.getTransactionStatusCode() != null)
				registrationStatusDto.setLatestTransactionStatusCode(description.getTransactionStatusCode());

			if (object.getInternalError()) {
				updateErrorFlags(registrationStatusDto, object);
			}
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.UIN_GENERATOR.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);

		}

		return object;
	}

    private void loadDemographicIdentity(Map<String, String> fieldMap, JSONObject demographicIdentity) throws IOException, JSONException {
        for (Map.Entry e : fieldMap.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            String value = e.getValue().toString();
            if (value == null) {
                demographicIdentity.putIfAbsent(e.getKey(), value);
                continue;
            }

            Object json = new JSONTokener(value).nextValue();
            if (json instanceof org.json.JSONObject) {
                HashMap<String, Object> hashMap = objectMapper.readValue(value, HashMap.class);
                demographicIdentity.putIfAbsent(e.getKey(), hashMap);
                continue;
            }

            if (json instanceof JSONArray) {
                List jsonList = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(value);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object obj = jsonArray.get(i);
                    if (obj instanceof String) {
                        jsonList.add(obj);
                    } else {
                        HashMap<String, Object> hashMap = objectMapper.readValue(obj.toString(), HashMap.class);

                        if (trimWhitespaces && hashMap.containsKey("value") && hashMap.get("value") instanceof String) {
                            hashMap.put("value", ((String) hashMap.get("value")).trim());
                        }
                        jsonList.add(hashMap);
                    }
                }
                demographicIdentity.putIfAbsent(e.getKey(), jsonList);
            }
            else {
                demographicIdentity.putIfAbsent(e.getKey(), value);
            }
        }
    }

	/**
	 * Re activate uin.
	 *
	 * @param id          the reg id
	 * @param uin         the uin
	 * @param object      the object
	 * @param description
	 * @return the id response DTO
	 * @throws ApisResourceAccessException       the apis resource access exception
	 * @throws IOException
	 * @throws IdrepoDraftReprocessableException
	 */
	private boolean reActivateUin(IdResponseDTO idResponseDTO, String id, String uin, MessageDTO object,
			JSONObject demographicIdentity, LogDescription description)
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
		IdResponseDTO result = getIdRepoDataByUIN(uin, id, description);
		List<String> pathsegments = new ArrayList<>();
		RequestDto requestDto = new RequestDto();
		boolean isTransactionSuccessful = Boolean.FALSE;

		if (isIdResponseNotNull(result)) {

			if ((RegistrationType.ACTIVATED.toString()).equalsIgnoreCase(result.getResponse().getStatus())) {

				description.setStatusCode(RegistrationStatusCode.FAILED.toString());
				description.setStatusComment(StatusUtil.UIN_ALREADY_ACTIVATED.getMessage());
				description.setSubStatusCode(StatusUtil.UIN_ALREADY_ACTIVATED.getCode());
				description.setMessage(PlatformErrorMessages.UIN_ALREADY_ACTIVATED.getMessage());
				description.setCode(PlatformErrorMessages.UIN_ALREADY_ACTIVATED.getCode());
				description.setTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				object.setIsValid(Boolean.FALSE);
				return isTransactionSuccessful;

			} else {

				requestDto.setRegistrationId(id);
				requestDto.setStatus(RegistrationType.ACTIVATED.toString());
				requestDto.setBiometricReferenceId(uin);
				requestDto.setIdentity(demographicIdentity);

				IdRequestDto idRequestDTO = new IdRequestDto();
				idRequestDTO.setId(idRepoUpdate);
				idRequestDTO.setRequest(requestDto);
				idRequestDTO.setMetadata(null);
				idRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
				idRequestDTO.setVersion(UINConstants.idRepoApiVersion);

				result = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

				if (isIdResponseNotNull(result)) {

					if ((RegistrationType.ACTIVATED.toString()).equalsIgnoreCase(result.getResponse().getStatus())) {
						isTransactionSuccessful = true;
						description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
						description.setStatusComment(StatusUtil.UIN_ACTIVATED_SUCCESS.getMessage());
						description.setSubStatusCode(StatusUtil.UIN_ACTIVATED_SUCCESS.getCode());
						description.setMessage(StatusUtil.UIN_ACTIVATED_SUCCESS.getMessage() + id);
						description.setMessage(PlatformSuccessMessages.RPR_UIN_ACTIVATED_SUCCESS.getMessage());
						description.setCode(PlatformSuccessMessages.RPR_UIN_ACTIVATED_SUCCESS.getCode());
						description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
						object.setIsValid(Boolean.TRUE);
					} else {
						description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
						description.setStatusComment(StatusUtil.UIN_ACTIVATED_FAILED.getMessage());
						description.setSubStatusCode(StatusUtil.UIN_ACTIVATED_FAILED.getCode());
						description.setMessage(StatusUtil.UIN_ACTIVATED_FAILED.getMessage() + id);
						description.setMessage(PlatformErrorMessages.UIN_ACTIVATED_FAILED.getMessage());
						description.setCode(PlatformErrorMessages.UIN_ACTIVATED_FAILED.getCode());
						description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
						object.setIsValid(Boolean.FALSE);
					}
				} else {
					String statusComment = result != null && result.getErrors() != null
							? result.getErrors().get(0).getMessage()
							: UINConstants.NULL_IDREPO_RESPONSE;
					description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					description.setStatusComment(trimExceptionMessage
							.trimExceptionMessage(StatusUtil.UIN_REACTIVATION_FAILED.getMessage() + statusComment));
					description.setSubStatusCode(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
					description.setMessage(
							UINConstants.UIN_FAILURE + id + "::" + (result != null && result.getErrors() != null
									? result.getErrors().get(0).getMessage()
									: UINConstants.NULL_IDREPO_RESPONSE));
					description.setMessage(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getMessage());
					description.setCode(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getCode());
					description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
					object.setIsValid(Boolean.FALSE);
				}

			}

		}else {
			String statusComment = result != null && result.getErrors() != null
					? result.getErrors().get(0).getMessage()
					: UINConstants.NULL_IDREPO_RESPONSE;
			description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UIN_REACTIVATION_FAILED.getMessage() + statusComment));
			description.setSubStatusCode(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
			description.setMessage(
					UINConstants.UIN_FAILURE + id + "::" + (result != null && result.getErrors() != null
							? result.getErrors().get(0).getMessage()
							: UINConstants.NULL_IDREPO_RESPONSE));
			description.setMessage(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getCode());
			description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			object.setIsValid(Boolean.FALSE);
		}

		return isTransactionSuccessful;
	}

	private boolean isIdResponseNotNull(IdResponseDTO result) {
		return result != null && result.getResponse() != null;
	}

	/**
	 * Deactivate uin.
	 *
	 * @param id          the reg id
	 * @param uin         the uin
	 * @param object      the object
	 * @param description
	 * @return the id response DTO
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws IdrepoDraftReprocessableException
	 */
	private IdResponseDTO deactivateUin(String id, String uin, MessageDTO object, JSONObject demographicIdentity,
			LogDescription description)
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
		IdResponseDTO idResponseDto;
		RequestDto requestDto = new RequestDto();
		String statusComment = "";

		idResponseDto = getIdRepoDataByUIN(uin, id, description);

		if (idResponseDto.getResponse() != null
				&& idResponseDto.getResponse().getStatus().equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
			description.setStatusCode(RegistrationStatusCode.FAILED.toString());
			description.setStatusComment(StatusUtil.UIN_ALREADY_DEACTIVATED.getMessage());
			description.setSubStatusCode(StatusUtil.UIN_ALREADY_DEACTIVATED.getCode());
			description.setMessage(StatusUtil.UIN_ALREADY_DEACTIVATED.getMessage() + id);
			description.setMessage(PlatformErrorMessages.UIN_ALREADY_DEACTIVATED.getMessage());
			description.setCode(PlatformErrorMessages.UIN_ALREADY_DEACTIVATED.getCode());
			description.setTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
			object.setIsValid(Boolean.FALSE);
			return idResponseDto;

		} else {
			requestDto.setRegistrationId(id);
			requestDto.setStatus(RegistrationType.DEACTIVATED.toString());
			requestDto.setIdentity(demographicIdentity);
			requestDto.setBiometricReferenceId(uin);

			IdRequestDto idRequestDTO = new IdRequestDto();
			idRequestDTO.setId(idRepoUpdate);
			idRequestDTO.setMetadata(null);
			idRequestDTO.setRequest(requestDto);
			idRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
			idRequestDTO.setVersion(UINConstants.idRepoApiVersion);

			idResponseDto = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

			if (isIdResponseNotNull(idResponseDto)) {
				if (idResponseDto.getResponse().getStatus().equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
					description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
					description.setStatusComment(StatusUtil.UIN_DEACTIVATION_SUCCESS.getMessage());
					description.setSubStatusCode(StatusUtil.UIN_DEACTIVATION_SUCCESS.getCode());
					description.setMessage(StatusUtil.UIN_DEACTIVATION_SUCCESS.getMessage() + id);
					description.setMessage(PlatformSuccessMessages.RPR_UIN_DEACTIVATION_SUCCESS.getMessage());
					description.setCode(PlatformSuccessMessages.RPR_UIN_DEACTIVATION_SUCCESS.getCode());
					description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
					object.setIsValid(Boolean.TRUE);
					statusComment = idResponseDto.getResponse().getStatus().toString();

				}
			} else {

				statusComment = idResponseDto != null && idResponseDto.getErrors() != null
						? idResponseDto.getErrors().get(0).getMessage()
						: UINConstants.NULL_IDREPO_RESPONSE;
				description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				description.setStatusComment(trimExceptionMessage
						.trimExceptionMessage(StatusUtil.UIN_DEACTIVATION_FAILED.getMessage() + statusComment));
				description.setSubStatusCode(StatusUtil.UIN_DEACTIVATION_FAILED.getCode());
				description.setMessage(PlatformErrorMessages.UIN_DEACTIVATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.UIN_DEACTIVATION_FAILED.getCode());
				description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				object.setIsValid(Boolean.FALSE);
			}

		}
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString() + id, "Updated Response from IdRepo API",
				"is : " + statusComment);

		return idResponseDto;
	}

	/**
	 * Gets the id repo data by UIN.
	 *
	 * @param uin
	 *            the uin
	 * @param description
	 * @return the id repo data by UIN
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	private IdResponseDTO getIdRepoDataByUIN(String uin, String regId, LogDescription description)
			throws ApisResourceAccessException {
		IdResponseDTO response;

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(uin);
		try {

			response = (IdResponseDTO) registrationProcessorRestClientService.getApi(ApiName.IDREPOGETIDBYUIN,
					pathsegments, "", "", IdResponseDTO.class);

		} catch (ApisResourceAccessException e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				description.setMessage(PlatformErrorMessages.UIN_GENERATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.UIN_GENERATION_FAILED.getCode());
				throw new ApisResourceAccessException(httpClientException.getResponseBodyAsString(),
						httpClientException);
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				description.setMessage(PlatformErrorMessages.UIN_GENERATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.UIN_GENERATION_FAILED.getCode());

				throw new ApisResourceAccessException(httpServerException.getResponseBodyAsString(),
						httpServerException);
			} else {
				description.setMessage(PlatformErrorMessages.UIN_GENERATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.UIN_GENERATION_FAILED.getCode());
				throw e;
			}
		}
		return response;
	}

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {

		mosipEventBus = this.getEventBus(this, clusterManagerUrl, getWorkerPoolSize());
		this.consumeAndSend(mosipEventBus, MessageBusAddress.UIN_GENERATION_BUS_IN,
				MessageBusAddress.UIN_GENERATION_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.UIN_GENERATION_BUS_IN,
				MessageBusAddress.UIN_GENERATION_BUS_OUT));
		this.createServer(router.getRouter(), getPort());

	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	/**
	 * Link reg id wrt uin.
	 *
	 * @param lostPacketRegId the lost packet reg id
	 * @param matchedRegId    the matched reg id
	 * @param object          the object
	 * @param description
	 * @return the id response DTO
	 * @throws ApisResourceAccessException       the apis resource access exception
	 * @throws IOException                       Signals that an I/O exception has
	 *                                           occurred.
	 * @throws IdrepoDraftReprocessableException
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	private IdResponseDTO lostAndUpdateUin(String lostPacketRegId, String matchedRegId, String process, MessageDTO object,
			LogDescription description) throws ApisResourceAccessException, IOException,
			io.mosip.kernel.core.util.exception.JsonProcessingException, PacketManagerException, IdrepoDraftException,
			IdrepoDraftReprocessableException, JSONException {

		IdResponseDTO idResponse = null;
		String uin = idRepoService.getUinByRid(matchedRegId, utilities.getGetRegProcessorDemographicIdentity());


		RequestDto requestDto = new RequestDto();
		String statusComment = "";

		if (uin != null) {

			JSONObject  regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
			String idschemaversion = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.IDSCHEMA_VERSION), MappingJsonConstants.VALUE);

			JSONObject identityObject = new JSONObject();
			identityObject.put(UINConstants.UIN, uin);
			String schemaVersion = packetManagerService.getFieldByMappingJsonKey(lostPacketRegId, MappingJsonConstants.IDSCHEMA_VERSION, process, ProviderStageName.UIN_GENERATOR);
			identityObject.put(idschemaversion, convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
			regProcLogger.info("Fields to be updated "+updateInfo);
			Map<String, String> fieldMap = new HashMap<String, String>();
			if (StringUtils.isNotEmpty(updateInfo)) {
				String[] updateFields = updateInfo.split(",");
				List<String> actualFieldNames = new ArrayList<>();
				for (String fieldName : updateFields) {
					String actualFieldName = JsonUtil.getJSONValue(
							JsonUtil.getJSONObject(regProcessorIdentityJson, fieldName),
							MappingJsonConstants.VALUE);
					if (StringUtils.isNotEmpty(actualFieldName)) {
						actualFieldNames.add(actualFieldName);
					}
				}
				if (!actualFieldNames.isEmpty()) {
					Map<String, String> fetchedFields = packetManagerService.getFields(lostPacketRegId, actualFieldNames, process,
							ProviderStageName.UIN_GENERATOR);
					if (fetchedFields != null) {
						fetchedFields.forEach((k, v) -> { if (v != null) fieldMap.put(k, v); });
					}
				}
			}
			loadDemographicIdentity(fieldMap, identityObject);
			requestDto.setRegistrationId(lostPacketRegId);
			requestDto.setIdentity(identityObject);

			IdRequestDto idRequestDTO = new IdRequestDto();
			idRequestDTO.setId(idRepoUpdate);
			idRequestDTO.setRequest(requestDto);
			idRequestDTO.setMetadata(null);
			idRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
			idRequestDTO.setVersion(UINConstants.idRepoApiVersion);

			idResponse = idrepoDraftService.idrepoUpdateDraft(lostPacketRegId, uin, idRequestDTO);

			if (isIdResponseNotNull(idResponse)) {
				description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
				description.setStatusComment(StatusUtil.LINK_RID_FOR_LOST_PACKET_SUCCESS.getMessage());
				description.setSubStatusCode(StatusUtil.LINK_RID_FOR_LOST_PACKET_SUCCESS.getCode());
				description.setMessage(UinStatusMessage.PACKET_LOST_UIN_UPDATION_SUCCESS_MSG + lostPacketRegId);
				description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
				object.setIsValid(Boolean.TRUE);

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString() + lostPacketRegId,
						" UIN LINKED WITH " + matchedRegId, "is : " + description);
			} else {

				statusComment = idResponse != null && idResponse.getErrors() != null
						&& idResponse.getErrors().get(0) != null ? idResponse.getErrors().get(0).getMessage()
								: UinStatusMessage.PACKET_LOST_UIN_UPDATION_FAILURE_MSG + "  "
										+ UINConstants.NULL_IDREPO_RESPONSE + "for lostPacketRegId " + lostPacketRegId;
				description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				description.setStatusComment(StatusUtil.LINK_RID_FOR_LOST_PACKET_SUCCESS.getMessage() + statusComment);
				description.setSubStatusCode(StatusUtil.LINK_RID_FOR_LOST_PACKET_SUCCESS.getCode());
				description.setTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_ID_REPO_ERROR));
				if (idResponse != null
						&& idResponse.getErrors() != null)
					description.setMessage(idResponse.getErrors().get(0).getMessage());
				else
					description.setMessage(UINConstants.NULL_IDREPO_RESPONSE);
				object.setIsValid(Boolean.FALSE);
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString() + lostPacketRegId,
						" UIN NOT LINKED WITH " + matchedRegId, "is : " + statusComment);
			}

		} else {
			statusComment = UinStatusMessage.PACKET_LOST_UIN_UPDATION_FAILURE_MSG + "  "
					+ UINConstants.NULL_IDREPO_RESPONSE + " UIN not available for matchedRegId " + matchedRegId;
			description.setStatusComment(StatusUtil.LINK_RID_FOR_LOST_PACKET_FAILED.getMessage());
			description.setSubStatusCode(StatusUtil.LINK_RID_FOR_LOST_PACKET_FAILED.getCode());
			description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			description.setTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
			description.setMessage(UinStatusMessage.PACKET_LOST_UIN_UPDATION_FAILURE_MSG + "  "
					+ UINConstants.NULL_IDREPO_RESPONSE + " UIN not available for matchedRegId " + matchedRegId);

			object.setIsValid(Boolean.FALSE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString() + lostPacketRegId,
					" UIN NOT LINKED WITH " + matchedRegId, "is : " + statusComment);
		}

		return idResponse;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E { throw (E) e; }

	private void updateErrorFlags(InternalRegistrationStatusDto registrationStatusDto, MessageDTO object) {
		object.setInternalError(true);
		if (registrationStatusDto.getLatestTransactionStatusCode()
				.equalsIgnoreCase(RegistrationTransactionStatusCode.REPROCESS.toString())) {
			object.setIsValid(true);
		} else {
			object.setIsValid(false);
		}
	}

}
