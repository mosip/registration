package io.mosip.registration.processor.stages.uingenerator.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
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
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
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
import io.mosip.registration.processor.core.idrepo.dto.Documents;
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
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.RegLostUinDetEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.uingenerator.constants.UINConstants;
import io.mosip.registration.processor.stages.uingenerator.dto.UinGenResponseDto;
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

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

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

	/** The utility. */
	@Autowired
	private Utilities utility;

	@Autowired
	private CbeffUtil cbeffutil;

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
		UinGenResponseDto uinResponseDto = null;

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
				IdResponseDTO idResponseDTO = new IdResponseDTO();
				String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId, MappingJsonConstants.IDSCHEMA_VERSION, registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);

				Map<String, String> fieldMap = packetManagerService.getFields(registrationId,
						idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion)), registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);
				String uinField = utility.getUIn(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.UIN_GENERATOR);
				JSONObject demographicIdentity = new JSONObject();
				demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION, convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);

				loadDemographicIdentity(fieldMap, demographicIdentity);

				if (StringUtils.isEmpty(uinField) || uinField.equalsIgnoreCase("null") ) {

					idResponseDTO = sendIdRepoWithUin(registrationId, registrationStatusDto.getRegistrationType(), demographicIdentity,
							uinField);

					boolean isUinAlreadyPresent = isUinAlreadyPresent(idResponseDTO, registrationId);

					if (isIdResponseNotNull(idResponseDTO) || isUinAlreadyPresent) {
						registrationStatusDto.setStatusComment(StatusUtil.UIN_GENERATED_SUCCESS.getMessage());
						registrationStatusDto.setSubStatusCode(StatusUtil.UIN_GENERATED_SUCCESS.getCode());
						isTransactionSuccessful = true;
						registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
						description.setMessage(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getMessage());
						description.setCode(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode());
						description.setTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
						
					} else {
						List<ErrorDTO> errors = idResponseDTO != null ? idResponseDTO.getErrors() : null;
						String statusComment = errors != null ? errors.get(0).getMessage()
								: UINConstants.NULL_IDREPO_RESPONSE;
						int unknownErrorCount=0;
						for(ErrorDTO dto:errors) {
							if(dto.getErrorCode().equalsIgnoreCase("IDR-IDC-004")||dto.getErrorCode().equalsIgnoreCase("IDR-IDC-001")) {
								unknownErrorCount++;
							}
						}
						if(unknownErrorCount>0) {
							registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
							registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
									.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
							description.setTransactionStatusCode(registrationStatusMapperUtil
									.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
						}
						else {
							registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
							registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
									.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
							description.setTransactionStatusCode(registrationStatusMapperUtil
									.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
						}
						registrationStatusDto.setStatusComment(trimExceptionMessage
								.trimExceptionMessage(StatusUtil.UIN_GENERATION_FAILED.getMessage() + statusComment));
						object.setInternalError(Boolean.TRUE);
						isTransactionSuccessful = false;
						description.setMessage(PlatformErrorMessages.RPR_UGS_UIN_UPDATE_FAILURE.getMessage());
						description.setCode(PlatformErrorMessages.RPR_UGS_UIN_UPDATE_FAILURE.getCode());
						description.setSubStatusCode(StatusUtil.UIN_GENERATION_FAILED.getCode());
						String idres = idResponseDTO != null ? idResponseDTO.toString()
								: UINConstants.NULL_IDREPO_RESPONSE;

						regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
								statusComment + "  :  " + idres);
						object.setIsValid(Boolean.FALSE);
					}

				} else {
					if ((RegistrationType.ACTIVATED.toString()).equalsIgnoreCase(object.getReg_type())) {
						isTransactionSuccessful = reActivateUin(idResponseDTO, registrationId, uinField, object,
								demographicIdentity, description);
					} else if ((RegistrationType.DEACTIVATED.toString())
							.equalsIgnoreCase(object.getReg_type())) {
						idResponseDTO = deactivateUin(registrationId, uinField, object, demographicIdentity,
								description);
					} else if (RegistrationType.UPDATE.toString().equalsIgnoreCase(object.getReg_type())
							|| (RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(object.getReg_type()))
							|| (RegistrationType.UPDATE.toString().equalsIgnoreCase(utility.getInternalProcess(additionalProcessCategoryMapping, object.getReg_type())))) {
						isTransactionSuccessful = uinUpdate(registrationId, registrationStatusDto.getRegistrationType(), uinField, object, demographicIdentity,
								description);
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
	 * Send id repo with uin.
	 *
	 * @param id
	 *            the reg id
	 * @param uin
	 *            the uin
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws VidCreationException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws Exception
	 */
	private IdResponseDTO sendIdRepoWithUin(String id, String process, JSONObject demographicIdentity, String uin)
			throws Exception {

		List<Documents> documentInfo = getAllDocumentsByRegId(id, process, demographicIdentity);
		RequestDto requestDto = new RequestDto();
		requestDto.setIdentity(demographicIdentity);
		requestDto.setDocuments(documentInfo);
		requestDto.setRegistrationId(id);
		requestDto.setStatus(RegistrationType.ACTIVATED.toString());
		requestDto.setBiometricReferenceId(uin);

		IdResponseDTO result = null;
		IdRequestDto idRequestDTO = new IdRequestDto();
		idRequestDTO.setId(idRepoUpdate);
		idRequestDTO.setRequest(requestDto);
		idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
		idRequestDTO.setVersion(UINConstants.idRepoApiVersion);
		idRequestDTO.setMetadata(null);

		try {

			result = idrepoDraftService.idrepoUpdateDraft(id, null, idRequestDTO);

		} catch (ApisResourceAccessException e) {
			regProcLogger.error("Execption occured updating draft for id " + id, e);
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApisResourceAccessException(httpClientException.getResponseBodyAsString(),
						httpClientException);
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApisResourceAccessException(httpServerException.getResponseBodyAsString(),
						httpServerException);
			} else {
				throw e;
			}

		}
		return result;

	}

	/**
	 * Gets the all documents by reg id.
	 *
	 * @param regId
	 *            the reg id
	 * @return the all documents by reg id
	 * @throws IOException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws ApisResourceAccessException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	private List<Documents> getAllDocumentsByRegId(String regId, String process, JSONObject demographicIdentity) throws Exception {
		List<Documents> applicantDocuments = new ArrayList<>();

		JSONObject idJSON = demographicIdentity;
		JSONObject  docJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
		JSONObject  identityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

		String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);

		HashMap<String, String> applicantBiometric = (HashMap<String, String>) idJSON.get(applicantBiometricLabel);


		for (Object doc : docJson.values()) {
			Map docMap = (LinkedHashMap) doc;
			String docValue = docMap.values().iterator().next().toString();
			HashMap<String, String> docInIdentityJson = (HashMap<String, String>) idJSON.get(docValue);
			if (docInIdentityJson != null)
				applicantDocuments
						.add(getIdDocumnet(regId, docValue, process));
		}

		if (applicantBiometric != null) {
			applicantDocuments.add(getBiometrics(regId, applicantBiometricLabel, process, applicantBiometricLabel));
		}
		return applicantDocuments;
	}

	private Documents getIdDocumnet(String registrationId, String dockey, String process)
			throws IOException, ApisResourceAccessException, PacketManagerException, io.mosip.kernel.core.util.exception.JsonProcessingException {
		Documents documentsInfoDto = new Documents();

		Document document =
				packetManagerService.getDocument(registrationId, dockey, process, ProviderStageName.UIN_GENERATOR);
		if (document != null) {
			documentsInfoDto.setValue(CryptoUtil.encodeToURLSafeBase64(document.getDocument()));
			documentsInfoDto.setCategory(document.getValue());
			return documentsInfoDto;
		}
		return null;
	}

	private Documents getBiometrics(String registrationId, String person, String process, String idDocLabel) throws Exception {
		BiometricRecord biometricRecord = packetManagerService.getBiometrics(registrationId, person, process, ProviderStageName.UIN_GENERATOR);
		byte[] xml = cbeffutil.createXML(biometricRecord.getSegments());
		Documents documentsInfoDto = new Documents();
		documentsInfoDto.setValue(CryptoUtil.encodeToURLSafeBase64(xml));
		documentsInfoDto.setCategory(utility.getMappingJsonValue(idDocLabel, MappingJsonConstants.IDENTITY));
		return documentsInfoDto;

	}

	/**
	 * Update id repo wit uin.
	 *
	 * @param regId       the reg id
	 * @param uin         the uin
	 * @param object      the object
	 * @param description
	 * @return the id response DTO
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws RegistrationProcessorCheckedException
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 */
	private boolean uinUpdate(String regId, String process, String uin, MessageDTO object, JSONObject demographicIdentity, LogDescription description)
			throws Exception {
		IdResponseDTO result;
		boolean isTransactionSuccessful = Boolean.FALSE;
		List<Documents> documentInfo = getAllDocumentsByRegId(regId, process, demographicIdentity);
		result = idRepoRequestBuilder(regId, uin, RegistrationType.ACTIVATED.toString().toUpperCase(), documentInfo,
				demographicIdentity);
		if (null!=result && isIdResponseNotNull(result)) {

			if (IDREPO_STATUS.equalsIgnoreCase(result.getResponse().getStatus())) {
				isTransactionSuccessful = true;
				description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				description.setStatusComment(StatusUtil.UIN_DATA_UPDATION_SUCCESS.getMessage());
				description.setSubStatusCode(StatusUtil.UIN_DATA_UPDATION_SUCCESS.getCode());
				description.setMessage(
						StatusUtil.UIN_DATA_UPDATION_SUCCESS.getMessage() + " for registration Id: " + regId);
				description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
				object.setIsValid(Boolean.TRUE);
			}
		} else {
			String statusComment = result != null && result.getErrors() != null ? result.getErrors().get(0).getMessage()
					: UINConstants.NULL_IDREPO_RESPONSE;
			String message = result != null && result.getErrors() != null
					? result.getErrors().get(0).getMessage()
					: UINConstants.NULL_IDREPO_RESPONSE;
			description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UIN_DATA_UPDATION_FAILED.getMessage() + statusComment));
			description.setSubStatusCode(StatusUtil.UIN_DATA_UPDATION_FAILED.getCode());
			description
					.setMessage(UINConstants.UIN_FAILURE + regId + "::" + message );
			description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSING.toString());
			object.setIsValid(Boolean.FALSE);
		}
		return isTransactionSuccessful;
	}

	/**
	 * Id repo request builder.
	 *
	 * @param status              the status
	 * @param id                  the reg id
	 * @param demographicIdentity the JSONObject
	 * @param documentInfo        the document info
	 * @throws ApisResourceAccessException       the apis resource access exception
	 * @throws IOException
	 * @throws IdrepoDraftReprocessableException
	 */
	private IdResponseDTO idRepoRequestBuilder(String id, String uin, String status, List<Documents> documentInfo,
			JSONObject demographicIdentity)
			throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
		IdResponseDTO idResponseDto;
		RequestDto requestDto = new RequestDto();

		if (documentInfo != null)
			requestDto.setDocuments(documentInfo);

		requestDto.setRegistrationId(id);
		requestDto.setStatus(status);
		requestDto.setIdentity(demographicIdentity);

		IdRequestDto idRequestDTO = new IdRequestDto();
		idRequestDTO.setId(idRepoUpdate);
		idRequestDTO.setMetadata(null);
		idRequestDTO.setRequest(requestDto);
		idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
		idRequestDTO.setVersion(UINConstants.idRepoApiVersion);

		try {
		idResponseDto = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error("Execption occured updating draft for id " + id, e);
			if (e.getCause() instanceof HttpClientErrorException) {
				regProcLogger.error("Exception occured updating draft for id " + id, e);
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApisResourceAccessException(httpClientException.getResponseBodyAsString(),
						httpClientException);
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApisResourceAccessException(httpServerException.getResponseBodyAsString(),
						httpServerException);
			} else {
				throw e;
			}

		}
		return idResponseDto;
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
				idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
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

	private boolean isUinAlreadyPresent(IdResponseDTO result, String rid) {
		if  (result != null && result.getErrors() != null && result.getErrors().size() > 0
				&& result.getErrors().get(0).getErrorCode().equalsIgnoreCase(RECORD_ALREADY_EXISTS_ERROR)) {
			ErrorDTO errorDTO = result.getErrors().get(0);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString() + rid,
					"Record is already present in IDREPO. Error message received : " + errorDTO.getMessage(),
					"The stage will ignore this error and try to generate vid for the existing UIN now. " +
							"This is to make sure if the packet processing fails while generating VID then re-processor can process the packet again");
			return true;
		}
		return false;
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
			idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
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

		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
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
	 */
	@SuppressWarnings("unchecked")
	private IdResponseDTO lostAndUpdateUin(String lostPacketRegId, String matchedRegId, String process, MessageDTO object,
			LogDescription description) throws ApisResourceAccessException, IOException,
			io.mosip.kernel.core.util.exception.JsonProcessingException, PacketManagerException, IdrepoDraftException,
			IdrepoDraftReprocessableException {

		IdResponseDTO idResponse = null;
		String uin = idRepoService.getUinByRid(matchedRegId, utility.getGetRegProcessorDemographicIdentity());


		RequestDto requestDto = new RequestDto();
		String statusComment = "";

		if (uin != null) {

			JSONObject  regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
			String idschemaversion = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.IDSCHEMA_VERSION), MappingJsonConstants.VALUE);

			JSONObject identityObject = new JSONObject();
			identityObject.put(UINConstants.UIN, uin);
			String schemaVersion = packetManagerService.getFieldByMappingJsonKey(lostPacketRegId, MappingJsonConstants.IDSCHEMA_VERSION, process, ProviderStageName.UIN_GENERATOR);
			identityObject.put(idschemaversion, convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
			regProcLogger.info("Fields to be updated "+updateInfo);
			if (null != updateInfo && !updateInfo.isEmpty()) {
				String[] upd = updateInfo.split(",");
				for (String infoField : upd) {
					String fldValue = packetManagerService.getField(lostPacketRegId, infoField, process,
							ProviderStageName.UIN_GENERATOR);
					if (null != fldValue)
						identityObject.put(infoField, fldValue);
				}
			}
			requestDto.setRegistrationId(lostPacketRegId);
			requestDto.setIdentity(identityObject);

			IdRequestDto idRequestDTO = new IdRequestDto();
			idRequestDTO.setId(idRepoUpdate);
			idRequestDTO.setRequest(requestDto);
			idRequestDTO.setMetadata(null);
			idRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
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
