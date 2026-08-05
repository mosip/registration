package io.mosip.registration.processor.stages.createdraft.stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.RequestDto;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.utils.StaleReprocessChecker;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.utils.Utility;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Create Draft Stage – introduces a new stage placed before the Quality
 * Classifier stage. Responsible for creating (or re-creating) an ID Repository
 * Draft for NEW and UPDATE packets.
 *
 * <p>For NEW packets the UIN is allocated and assigned internally by the ID
 * Repository during draft creation (no UIN Generator call here).
 * For UPDATE packets the existing UIN is read from the packet so the ID
 * Repository can clone the existing identity into the draft.</p>
 *
 * <p>Workflow position: … → Packet Classifier → Create Draft → Quality Classifier → …</p>
 */
@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
        "io.mosip.registration.processor.core.config",
        "io.mosip.registration.processor.stages.createdraft.config",
        "io.mosip.registration.processor.stages.config",
        "io.mosip.registration.processor.status.config",
        "io.mosip.registration.processor.rest.client.config",
        "io.mosip.registration.processor.packet.storage.config",
        "io.mosip.registration.processor.packet.manager.config",
        "io.mosip.registration.processor.core.kernel.beans" })
public class CreateDraftStage extends MosipVerticleAPIManager {

    private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.create.draft.";

    private static Logger regProcLogger = RegProcessorLogger.getLogger(CreateDraftStage.class);

    /** The cluster manager url. */
    @Value("${vertx.cluster.configuration}")
    private String clusterManagerUrl;

    /** Worker pool size. */
    @Value("${worker.pool.size}")
    private Integer workerPoolSize;

    /** Message expiry time limit (seconds). */
    @Value("${mosip.regproc.create.draft.message.expiry-time-limit}")
    private Long messageExpiryTimeLimit;

    /** Mosip router for APIs. */
    @Autowired
    private MosipRouter router;

    /** Registration status service. */
    @Autowired
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /** Draft service for ID Repository draft operations. */
    @Autowired
    private IdrepoDraftService idrepoDraftService;

    /** Utility for retrieving UIN from the packet. */
    @Autowired
    private Utility utility;

    /** Audit log request builder. */
    @Autowired
    private AuditLogRequestBuilder auditLogRequestBuilder;

    /** Status mapper utility. */
    @Autowired
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    /** Packet Manager service for reading demographic/document/biometric data from the packet. */
    @Autowired
    private PriorityBasedPacketManagerService packetManagerService;

    /** ID schema utility for resolving default fields per schema version. */
    @Autowired
    private IdSchemaUtil idSchemaUtil;

    /** Utilities for identity / document mapping lookups. */
    @Autowired
    private Utilities utilities;

    /** Jackson object mapper for JSON conversions. */
    @Autowired
    private ObjectMapper objectMapper;

    /** CBEFF utility for building biometric XML. */
    @Autowired
    private CbeffUtil cbeffutil;

    @Value("${registration.processor.id.repo.update}")
    private String idRepoUpdate;

    @Value("${mosip.registration.processor.id.repo.api-version:v1}")
    private String idRepoApiVersion;

    @Value("${mosip.regproc.uin-generator.convert-id-schema-to-double:true}")
    private boolean convertIdschemaToDouble;

    @Value("${mosip.regproc.uin.generator.trim-whitespaces.simpleType-value:false}")
    private boolean trimWhitespaces;

    @Autowired
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Autowired
    private StaleReprocessChecker staleReprocessChecker;

    private TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

    /** Mosip event bus. */
    MosipEventBus mosipEventBus = null;

    /**
     * Deploy verticle – wires up the event bus consumer/producer.
     */
    public void deployVerticle() {
        mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
        this.consumeAndSend(mosipEventBus, MessageBusAddress.CREATE_DRAFT_BUS_IN,
                MessageBusAddress.CREATE_DRAFT_BUS_OUT, messageExpiryTimeLimit);
    }

    @Override
    public void start() {
        router.setRoute(this.postUrl(getVertx(), MessageBusAddress.CREATE_DRAFT_BUS_IN,
                MessageBusAddress.CREATE_DRAFT_BUS_OUT));
        this.createServer(router.getRouter(), getPort());
    }

    @Override
    protected String getPropertyPrefix() {
        return STAGE_PROPERTY_PREFIX;
    }

    /**
     * Main processing method.
     *
     * <ul>
     *   <li>NEW packets  – allocates a UIN via the UIN Generator service, then creates
     *       (or discards-and-re-creates) a Draft in ID Repository.</li>
     *   <li>UPDATE packets – retrieves the existing UIN from the packet and
     *       creates (or discards-and-re-creates) a Draft.</li>
     *   <li>All other packet types – passed through without any draft operation.</li>
     * </ul>
     */
    @Override
    public MessageDTO process(MessageDTO object) {
        object.setMessageBusAddress(MessageBusAddress.CREATE_DRAFT_BUS_IN);
        object.setInternalError(Boolean.FALSE);
        object.setIsValid(Boolean.TRUE);

        String registrationId = object.getRid();
        String regType = object.getReg_type();
        boolean isTransactionSuccessful = Boolean.FALSE;
        LogDescription description = new LogDescription();

        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "CreateDraftStage::process()::entry");

        InternalRegistrationStatusDto registrationStatusDto = null;

        try {
            registrationStatusDto = registrationStatusService.getRegistrationStatus(
                    registrationId, regType, object.getIteration(), object.getWorkflowInstanceId());
            if (registrationStatusDto == null) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "Registration status not found for RID: " + registrationId);
                object.setIsValid(Boolean.FALSE);
                object.setInternalError(Boolean.TRUE);
                return object;
            }
            registrationStatusDto.setLatestTransactionTypeCode(
                    RegistrationTransactionTypeCode.CREATE_DRAFT.toString());
            registrationStatusDto.setRegistrationStageName(getStageName());

            String resolvedUin = resolveUin(registrationId, regType, registrationStatusDto);
            if (staleReprocessChecker.isStaleReprocess(resolvedUin, registrationStatusDto.getPacketCreateDateTime(), registrationId)) {
                regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "CreateDraftStage :: Stale reprocess detected. reg_type=" + regType
                                + " pkt_cr_dtimes=" + registrationStatusDto.getPacketCreateDateTime());
                markAsObsoleted(registrationStatusDto, object, description);
                isTransactionSuccessful = false;
                return object;
            }

            if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(regType)) {
                JSONObject demographicIdentity = buildDemographicIdentity(registrationId,
                        registrationStatusDto.getRegistrationType());
                IdResponseDTO idResponseDTO = new IdResponseDTO();
                isTransactionSuccessful = reActivateUin(idResponseDTO, registrationId, resolvedUin, object,
                        demographicIdentity, description);
                applyDescriptionToStatus(description, registrationStatusDto);

            } else if (RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(regType)) {
                JSONObject demographicIdentity = buildDemographicIdentity(registrationId,
                        registrationStatusDto.getRegistrationType());
                deactivateUin(registrationId, resolvedUin, object, demographicIdentity, description);
                isTransactionSuccessful = object.getIsValid();
                applyDescriptionToStatus(description, registrationStatusDto);

            } else if (RegistrationType.LOST.toString().equalsIgnoreCase(regType)) {
                // For LOST packets, create a draft WITHOUT UIN (UIN unknown until ABIS match).
                // BioDedupeProcessor will call idrepoUpdateDraftUin after the ABIS match.
                if (idrepoDraftService.idrepoHasDraft(registrationId)) {
                    regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                            "LOST packet draft already exists. Discarding before re-creation.");
                    idrepoDraftService.idrepoDiscardDraft(registrationId);
                }
                idrepoDraftService.idrepoCreateDraftV2(registrationId);
                populateDraftWithIdentity(registrationId, registrationStatusDto.getRegistrationType(), null);
                isTransactionSuccessful = Boolean.TRUE;
                object.setIsValid(Boolean.TRUE);
                registrationStatusDto.setLatestTransactionStatusCode(
                        RegistrationTransactionStatusCode.SUCCESS.toString());
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_SUCCESS.getMessage());
                registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
                description.setCode(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getCode());
                description.setMessage(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getMessage());

            } else if (RegistrationType.NEW.toString().equalsIgnoreCase(regType)
                    || RegistrationType.UPDATE.toString().equalsIgnoreCase(regType)
                    || RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(regType)) {

                // If a draft already exists (e.g. reprocessed packet), discard it first
                if (idrepoDraftService.idrepoHasDraft(registrationId)) {
                    regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                            "Draft already exists. Discarding before re-creation.");
                    idrepoDraftService.idrepoDiscardDraft(registrationId);
                }

                // For UPDATE/RES_UPDATE packets, pass the existing UIN so ID Repo can clone
                // the existing identity into the draft. For NEW packets, pass null — ID Repo
                // will allocate and assign the UIN internally during draft creation.
                String uin = null;
                if (RegistrationType.UPDATE.toString().equalsIgnoreCase(regType)
                        || RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(regType)) {
                    uin = resolvedUin;
                    if (StringUtils.isEmpty(uin) || "null".equalsIgnoreCase(uin)) {
                        regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                "UIN not found for UPDATE/RES_UPDATE packet — permanent failure.");
                        registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
                        registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_FAILED.getMessage());
                        registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_FAILED.getCode());
                        registrationStatusDto.setLatestTransactionStatusCode(
                                RegistrationTransactionStatusCode.FAILED.toString());
                        description.setCode(PlatformErrorMessages.RPR_CDS_UIN_NOT_FOUND_FOR_UPDATE.getCode());
                        description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_NOT_FOUND_FOR_UPDATE.getMessage());
                        object.setIsValid(Boolean.FALSE);
                        object.setInternalError(Boolean.FALSE);
                        return object;
                    }
                }

                boolean created = idrepoDraftService.idrepoCreateDraft(registrationId, uin);
                if (!created) {
                    throw new IdrepoDraftException(
                            PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode(),
                            PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getMessage());
                }

                populateDraftWithIdentity(registrationId, registrationStatusDto.getRegistrationType(), uin);

                isTransactionSuccessful = Boolean.TRUE;
                object.setIsValid(Boolean.TRUE);
                registrationStatusDto.setLatestTransactionStatusCode(
                        RegistrationTransactionStatusCode.SUCCESS.toString());
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_SUCCESS.getMessage());
                registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
                description.setCode(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getCode());
                description.setMessage(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getMessage());

                regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "Draft created successfully for regType: " + regType);

            } else {
                // Pass-through for any unrecognised packet type
                regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "Skipping create draft for reg type: " + regType);
                object.setIsValid(Boolean.TRUE);
                isTransactionSuccessful = Boolean.TRUE;
                registrationStatusDto.setLatestTransactionStatusCode(
                        RegistrationTransactionStatusCode.SUCCESS.toString());
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_SKIPPED.getMessage());
                registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_SKIPPED.getCode());
                description.setCode(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getCode());
                description.setMessage(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getMessage());
            }

        } catch (ApisResourceAccessException e) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.PROCESSING.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            object.setInternalError(Boolean.TRUE);
            description.setMessage(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
            description.setCode(PlatformErrorMessages.RPR_UGS_API_RESOURCE_EXCEPTION.getCode());

        } catch (IdrepoDraftReprocessableException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.PROCESSING.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION));
            description.setMessage(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
            object.setRid(registrationStatusDto.getRegistrationId());

        } catch (IdrepoDraftException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.PROCESSING.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.IDREPO_DRAFT_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_EXCEPTION));
            description.setMessage(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
            object.setRid(registrationStatusDto.getRegistrationId());

        } catch (Exception e) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.PROCESSING.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            object.setInternalError(Boolean.TRUE);
            description.setMessage(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getCode());

        } finally {
            if (registrationStatusDto != null) {
                if (object.getInternalError()) {
                    updateErrorFlags(registrationStatusDto, object);
                }
                object.setRid(registrationStatusDto.getRegistrationId());
                registrationStatusDto.setRegistrationStageName(getStageName());
                String moduleId = isTransactionSuccessful
                        ? PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getCode()
                        : description.getCode();
                String moduleName = ModuleName.CREATE_DRAFT.toString();
                registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
                String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
                String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
                String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();
                auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName,
                        eventType, moduleId, moduleName, registrationId);
            }
        }

        return object;
    }

    private String resolveUin(String registrationId, String regType,
            InternalRegistrationStatusDto registrationStatusDto) {
        try {
            if (RegistrationType.UPDATE.toString().equalsIgnoreCase(regType)
                    || RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(regType)
                    || RegistrationType.ACTIVATED.toString().equalsIgnoreCase(regType)
                    || RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(regType)) {
                return utility.getUIn(registrationId, registrationStatusDto.getRegistrationType(),
                        ProviderStageName.CREATE_DRAFT);
            }
        } catch (Exception e) {
            regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    "CreateDraftStage :: Could not resolve UIN for stale check: " + e.getMessage());
        }
        return null;
    }

    private void markAsObsoleted(InternalRegistrationStatusDto dto, MessageDTO object, LogDescription description) {
        dto.setStatusCode(io.mosip.registration.processor.status.code.RegistrationStatusCode.FAILED.toString());
        dto.setStatusComment(StatusUtil.PACKET_REPROCESS_OBSOLETED.getMessage());
        dto.setSubStatusCode(StatusUtil.PACKET_REPROCESS_OBSOLETED.getCode());
        dto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
        description.setCode(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode());
        description.setMessage(StatusUtil.PACKET_REPROCESS_OBSOLETED.getMessage());
        object.setIsValid(false);
        object.setInternalError(false);
    }

    private void applyDescriptionToStatus(LogDescription description,
            InternalRegistrationStatusDto registrationStatusDto) {
        if (description.getStatusCode() != null) {
            registrationStatusDto.setStatusCode(description.getStatusCode());
        }
        if (description.getStatusComment() != null) {
            registrationStatusDto.setStatusComment(description.getStatusComment());
        }
        if (description.getTransactionStatusCode() != null) {
            registrationStatusDto.setLatestTransactionStatusCode(description.getTransactionStatusCode());
        }
        // sub_status_code has a NOT NULL constraint; use fallback if the helper didn't set it
        registrationStatusDto.setSubStatusCode(description.getSubStatusCode() != null
                ? description.getSubStatusCode() : StatusUtil.CREATE_DRAFT_FAILED.getCode());
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

    /**
     * Builds the demographic identity + biometric documents from the packet
     * and pushes them into the ID Repository draft via {@code idrepoUpdateDraft}.
     */
    private void populateDraftWithIdentity(String registrationId, String process, String uin) throws Exception {
        String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId,
                MappingJsonConstants.IDSCHEMA_VERSION, process, ProviderStageName.CREATE_DRAFT);

        Map<String, String> fieldMap = packetManagerService.getFields(registrationId,
                idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion)), process,
                ProviderStageName.CREATE_DRAFT);

        JSONObject demographicIdentity = new JSONObject();
        demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION,
                convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
        loadDemographicIdentity(fieldMap, demographicIdentity);

        List<Documents> documentInfo = getAllDocumentsByRegId(registrationId, process, demographicIdentity);

        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(demographicIdentity);
        requestDto.setDocuments(documentInfo);
        requestDto.setRegistrationId(registrationId);
        requestDto.setStatus(RegistrationType.ACTIVATED.toString());
        requestDto.setBiometricReferenceId(uin);

        IdRequestDto idRequestDTO = new IdRequestDto();
        idRequestDTO.setId(idRepoUpdate);
        idRequestDTO.setRequest(requestDto);
        idRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
        idRequestDTO.setVersion(idRepoApiVersion);

        IdResponseDTO response = idrepoDraftService.idrepoUpdateDraft(registrationId, uin, idRequestDTO);
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                "Draft populated with identity. id-repo response id: "
                        + (response != null ? response.getId() : "null"));
    }

    /**
     * Copies field values from packet manager into the {@code demographicIdentity} JSON.
     * Mirrors {@code UinGeneratorStage.loadDemographicIdentity} so downstream stages see
     * the same shape they would have seen from packet manager.
     */
    private JSONObject buildDemographicIdentity(String registrationId, String registrationType) throws Exception {
        String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId,
                MappingJsonConstants.IDSCHEMA_VERSION, registrationType, ProviderStageName.CREATE_DRAFT);
        List<String> defaultFields = idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion));
        Map<String, String> fieldMap = packetManagerService.getFields(registrationId, defaultFields,
                registrationType, ProviderStageName.CREATE_DRAFT);
        JSONObject demographicIdentity = new JSONObject();
        demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION,
                convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
        loadDemographicIdentity(fieldMap, demographicIdentity);
        return demographicIdentity;
    }

    private void loadDemographicIdentity(Map<String, String> fieldMap, JSONObject demographicIdentity)
            throws IOException, JSONException {
        for (Map.Entry e : fieldMap.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String value = e.getValue().toString();
            Object json = new JSONTokener(value).nextValue();
            if (json instanceof org.json.JSONObject) {
                HashMap<String, Object> hashMap = objectMapper.readValue(value, HashMap.class);
                demographicIdentity.putIfAbsent(e.getKey(), hashMap);
                continue;
            }
            if (json instanceof JSONArray) {
                List<Object> jsonList = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(value);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object obj = jsonArray.get(i);
                    if (obj instanceof String) {
                        jsonList.add(obj);
                    } else {
                        HashMap<String, Object> hashMap = objectMapper.readValue(obj.toString(), HashMap.class);
                        if (trimWhitespaces && hashMap.containsKey("value")
                                && hashMap.get("value") instanceof String) {
                            hashMap.put("value", ((String) hashMap.get("value")).trim());
                        }
                        jsonList.add(hashMap);
                    }
                }
                demographicIdentity.putIfAbsent(e.getKey(), jsonList);
            } else {
                demographicIdentity.putIfAbsent(e.getKey(), value);
            }
        }
    }

    /**
     * Builds the list of documents (incl. biometrics) for the draft payload.
     */
    private List<Documents> getAllDocumentsByRegId(String regId, String process, JSONObject demographicIdentity)
            throws Exception {
        List<Documents> applicantDocuments = new ArrayList<>();
        JSONObject idJSON = demographicIdentity;
        JSONObject docJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
        JSONObject identityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

        String applicantBiometricLabel = JsonUtil.getJSONValue(
                JsonUtil.getJSONObject(identityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
                MappingJsonConstants.VALUE);

        HashMap<String, String> applicantBiometric = (HashMap<String, String>) idJSON.get(applicantBiometricLabel);

        for (Object doc : docJson.values()) {
            Map docMap = (LinkedHashMap) doc;
            String docValue = docMap.values().iterator().next().toString();
            HashMap<String, String> docInIdentityJson = (HashMap<String, String>) idJSON.get(docValue);
            if (docInIdentityJson != null) {
                Documents d = getIdDocument(regId, docValue, process);
                if (d != null) {
                    applicantDocuments.add(d);
                }
            }
        }

        if (applicantBiometric != null) {
            applicantDocuments.add(getBiometricsDocument(regId, applicantBiometricLabel, process));
        }
        return applicantDocuments;
    }

    private Documents getIdDocument(String registrationId, String dockey, String process)
            throws IOException, ApisResourceAccessException, PacketManagerException,
            io.mosip.kernel.core.util.exception.JsonProcessingException {
        Documents documentsInfoDto = new Documents();
        Document document = packetManagerService.getDocument(registrationId, dockey, process,
                ProviderStageName.CREATE_DRAFT);
        if (document != null) {
            documentsInfoDto.setValue(CryptoUtil.encodeToURLSafeBase64(document.getDocument()));
            documentsInfoDto.setCategory(document.getValue());
            return documentsInfoDto;
        }
        return null;
    }

    private Documents getBiometricsDocument(String registrationId, String person, String process) throws Exception {
        BiometricRecord biometricRecord = packetManagerService.getBiometrics(registrationId, person, process,
                ProviderStageName.CREATE_DRAFT);
        byte[] xml = cbeffutil.createXML(biometricRecord.getSegments());
        Documents documentsInfoDto = new Documents();
        documentsInfoDto.setValue(CryptoUtil.encodeToURLSafeBase64(xml));
        documentsInfoDto.setCategory(utilities.getMappingJsonValue(person, MappingJsonConstants.IDENTITY));
        return documentsInfoDto;
    }

    // -------------------------------------------------------------------------
    // ACTIVATED / DEACTIVATED helpers (ported from UinGeneratorStage)
    // -------------------------------------------------------------------------

    private boolean reActivateUin(IdResponseDTO idResponseDTO, String id, String uin, MessageDTO object,
            JSONObject demographicIdentity, LogDescription description)
            throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
        IdResponseDTO result = getIdRepoDataByUIN(uin, id, description);
        RequestDto requestDto = new RequestDto();
        boolean isTransactionSuccessful = Boolean.FALSE;

        if (isIdResponseNotNull(result)) {
            if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(result.getResponse().getStatus())) {
                description.setStatusCode(RegistrationStatusCode.FAILED.toString());
                description.setStatusComment(StatusUtil.CDS_UIN_ALREADY_ACTIVATED.getMessage());
                description.setSubStatusCode(StatusUtil.CDS_UIN_ALREADY_ACTIVATED.getCode());
                description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_ALREADY_ACTIVATED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_CDS_UIN_ALREADY_ACTIVATED.getCode());
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
                idRequestDTO.setVersion(idRepoApiVersion);

                result = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

                if (isIdResponseNotNull(result)) {
                    if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(result.getResponse().getStatus())) {
                        isTransactionSuccessful = true;
                        description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
                        description.setStatusComment(StatusUtil.CDS_UIN_ACTIVATED_SUCCESS.getMessage());
                        description.setSubStatusCode(StatusUtil.CDS_UIN_ACTIVATED_SUCCESS.getCode());
                        description.setMessage(PlatformSuccessMessages.RPR_CDS_UIN_ACTIVATED_SUCCESS.getMessage());
                        description.setCode(PlatformSuccessMessages.RPR_CDS_UIN_ACTIVATED_SUCCESS.getCode());
                        description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
                        object.setIsValid(Boolean.TRUE);
                    } else {
                        description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                        description.setStatusComment(StatusUtil.CDS_UIN_ACTIVATED_FAILED.getMessage());
                        description.setSubStatusCode(StatusUtil.CDS_UIN_ACTIVATED_FAILED.getCode());
                        description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_ACTIVATION_FAILED.getMessage());
                        description.setCode(PlatformErrorMessages.RPR_CDS_UIN_ACTIVATION_FAILED.getCode());
                        description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                        object.setIsValid(Boolean.FALSE);
                    }
                } else {
                    String statusComment = result != null && result.getErrors() != null
                            ? result.getErrors().get(0).getMessage() : "Null response from Id Repo";
                    description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                    description.setStatusComment(trimExceptionMessage
                            .trimExceptionMessage(StatusUtil.CDS_UIN_REACTIVATION_FAILED.getMessage() + statusComment));
                    description.setSubStatusCode(StatusUtil.CDS_UIN_REACTIVATION_FAILED.getCode());
                    description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_REACTIVATION_FAILED.getMessage());
                    description.setCode(PlatformErrorMessages.RPR_CDS_UIN_REACTIVATION_FAILED.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                    object.setIsValid(Boolean.FALSE);
                }
            }
        } else {
            String statusComment = result != null && result.getErrors() != null
                    ? result.getErrors().get(0).getMessage() : "Null response from Id Repo";
            description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
            description.setStatusComment(trimExceptionMessage
                    .trimExceptionMessage(StatusUtil.CDS_UIN_REACTIVATION_FAILED.getMessage() + statusComment));
            description.setSubStatusCode(StatusUtil.CDS_UIN_REACTIVATION_FAILED.getCode());
            description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_REACTIVATION_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_CDS_UIN_REACTIVATION_FAILED.getCode());
            description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
            object.setIsValid(Boolean.FALSE);
        }
        return isTransactionSuccessful;
    }

    private IdResponseDTO deactivateUin(String id, String uin, MessageDTO object, JSONObject demographicIdentity,
            LogDescription description)
            throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
        IdResponseDTO idResponseDto;
        RequestDto requestDto = new RequestDto();
        String statusComment = "";

        idResponseDto = getIdRepoDataByUIN(uin, id, description);

        if (isIdResponseNotNull(idResponseDto) && idResponseDto.getResponse().getStatus()
                .equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
            description.setStatusCode(RegistrationStatusCode.FAILED.toString());
            description.setStatusComment(StatusUtil.CDS_UIN_ALREADY_DEACTIVATED.getMessage());
            description.setSubStatusCode(StatusUtil.CDS_UIN_ALREADY_DEACTIVATED.getCode());
            description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_ALREADY_DEACTIVATED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_CDS_UIN_ALREADY_DEACTIVATED.getCode());
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
            idRequestDTO.setVersion(idRepoApiVersion);

            idResponseDto = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

            if (isIdResponseNotNull(idResponseDto)) {
                if (idResponseDto.getResponse().getStatus()
                        .equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
                    description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
                    description.setStatusComment(StatusUtil.CDS_UIN_DEACTIVATION_SUCCESS.getMessage());
                    description.setSubStatusCode(StatusUtil.CDS_UIN_DEACTIVATION_SUCCESS.getCode());
                    description.setMessage(PlatformSuccessMessages.RPR_CDS_UIN_DEACTIVATION_SUCCESS.getMessage());
                    description.setCode(PlatformSuccessMessages.RPR_CDS_UIN_DEACTIVATION_SUCCESS.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
                    object.setIsValid(Boolean.TRUE);
                    statusComment = idResponseDto.getResponse().getStatus();
                } else {
                    statusComment = idResponseDto.getResponse().getStatus();
                    description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                    description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                            StatusUtil.CDS_UIN_DEACTIVATION_FAILED.getMessage() + statusComment));
                    description.setSubStatusCode(StatusUtil.CDS_UIN_DEACTIVATION_FAILED.getCode());
                    description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_DEACTIVATION_FAILED.getMessage());
                    description.setCode(PlatformErrorMessages.RPR_CDS_UIN_DEACTIVATION_FAILED.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                    object.setIsValid(Boolean.FALSE);
                }
            } else {
                statusComment = idResponseDto != null && idResponseDto.getErrors() != null
                        ? idResponseDto.getErrors().get(0).getMessage() : "Null response from Id Repo";
                description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                description.setStatusComment(trimExceptionMessage
                        .trimExceptionMessage(StatusUtil.CDS_UIN_DEACTIVATION_FAILED.getMessage() + statusComment));
                description.setSubStatusCode(StatusUtil.CDS_UIN_DEACTIVATION_FAILED.getCode());
                description.setMessage(PlatformErrorMessages.RPR_CDS_UIN_DEACTIVATION_FAILED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_CDS_UIN_DEACTIVATION_FAILED.getCode());
                description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                object.setIsValid(Boolean.FALSE);
            }
        }
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                LoggerFileConstant.REGISTRATIONID.toString(), id,
                "Updated Response from IdRepo API: " + statusComment);
        return idResponseDto;
    }

    // -------------------------------------------------------------------------
    // ID Repo REST helper
    // -------------------------------------------------------------------------

    private IdResponseDTO getIdRepoDataByUIN(String uin, String regId, LogDescription description)
            throws ApisResourceAccessException {
        IdResponseDTO response;
        List<String> pathsegments = new ArrayList<>();
        pathsegments.add(uin);
        try {
            response = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                    ApiName.IDREPOGETIDBYUIN, pathsegments, "", "", IdResponseDTO.class);
        } catch (ApisResourceAccessException e) {
            if (e.getCause() instanceof HttpClientErrorException) {
                HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
                description.setMessage(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode());
                throw new ApisResourceAccessException(httpClientException.getResponseBodyAsString(),
                        httpClientException);
            } else if (e.getCause() instanceof HttpServerErrorException) {
                HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
                description.setMessage(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode());
                throw new ApisResourceAccessException(httpServerException.getResponseBodyAsString(),
                        httpServerException);
            }
            throw e;
        }
        return response;
    }

    private boolean isIdResponseNotNull(IdResponseDTO result) {
        return result != null && result.getResponse() != null;
    }
}
