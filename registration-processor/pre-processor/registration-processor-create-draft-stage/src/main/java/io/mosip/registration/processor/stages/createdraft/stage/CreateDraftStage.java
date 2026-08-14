package io.mosip.registration.processor.stages.createdraft.stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
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
@RefreshScope
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
    private static final String ID_REPO_API_VERSION = "v1";

    private static Logger regProcLogger = RegProcessorLogger.getLogger(CreateDraftStage.class);

    /** The cluster manager url. */
    @Value("${vertx.cluster.configuration}")
    private String clusterManagerUrl;

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

    @Value("${mosip.commons.packet.manager.schema.validator.convertIdSchemaToDouble:true}")
    private boolean convertIdschemaToDouble;

    @Value("${mosip.regproc.uin.generator.trim-whitespaces.simpleType-value:false}")
    private boolean trimWhitespaces;

    @Value("#{${registration.processor.additional-process.category-mapping:{:}}}")
    private Map<String, String> additionalProcessCategoryMapping;

    @Value("${uingenerator.lost.packet.allowed.update.fields:null}")
    private String lostPacketUpdateFields;

    @Autowired
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    private TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

    /** Mosip event bus. */
    MosipEventBus mosipEventBus = null;

    /**
     * Deploy verticle – wires up the event bus consumer/producer.
     */
    public void deployVerticle() {
        mosipEventBus = this.getEventBus(this, clusterManagerUrl, getWorkerPoolSize());
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
            io.mosip.registration.processor.packet.storage.utils.StaleCheckResult staleCheck =
                    utility.isLatestPacket(resolvedUin, registrationStatusDto.getPacketCreateDateTime(), registrationId);
            if (staleCheck == io.mosip.registration.processor.packet.storage.utils.StaleCheckResult.STALE) {
                regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "CreateDraftStage :: Stale reprocess detected. reg_type=" + regType
                                + " pkt_cr_dtimes=" + registrationStatusDto.getPacketCreateDateTime());
                markAsObsoleted(registrationStatusDto, object, description);
                isTransactionSuccessful = false;
                return object;
            } else if (staleCheck == io.mosip.registration.processor.packet.storage.utils.StaleCheckResult.UNAVAILABLE) {
                regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        "CreateDraftStage :: Stale check unavailable — scheduling reprocess.");
                registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
                registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                        StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage()));
                registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
                registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                        .getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
                object.setInternalError(Boolean.TRUE);
                description.setMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage());
                description.setCode(PlatformErrorMessages.RPR_UGS_API_RESOURCE_EXCEPTION.getCode());
                return object;
            }

            // Compute effectiveProcess once so custom types (opencrvs_new, crvs_new, crvs_death, etc.)
            // mapped via registration.processor.additional-process.category-mapping are handled correctly.
            String effectiveProcess = utilities.getInternalProcess(additionalProcessCategoryMapping, regType);

            if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(regType)
                    || RegistrationType.ACTIVATED.toString().equalsIgnoreCase(effectiveProcess)) {
                JSONObject demographicIdentity = buildDemographicIdentity(registrationId,
                        registrationStatusDto.getRegistrationType());
                IdResponseDTO idResponseDTO = new IdResponseDTO();
                isTransactionSuccessful = reActivateUin(idResponseDTO, registrationId, resolvedUin, object,
                        demographicIdentity, description);
                applyDescriptionToStatus(description, registrationStatusDto);

            } else if (RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(regType)
                    || RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(effectiveProcess)) {
                JSONObject demographicIdentity = buildDemographicIdentity(registrationId,
                        registrationStatusDto.getRegistrationType());
                deactivateUin(registrationId, resolvedUin, object, demographicIdentity, description);
                isTransactionSuccessful = object.getIsValid();
                applyDescriptionToStatus(description, registrationStatusDto);

            } else {
                // Generic draft create + populate for NEW, UPDATE, RES_UPDATE, LOST,
                // and any custom type mapped to NEW/UPDATE via additionalProcessCategoryMapping.
                boolean isUpdateLike = isUpdateType(regType, effectiveProcess);
                boolean isNewLike = RegistrationType.NEW.toString().equalsIgnoreCase(regType)
                        || RegistrationType.NEW.toString().equalsIgnoreCase(effectiveProcess);
                boolean isLost = RegistrationType.LOST.toString().equalsIgnoreCase(regType);

                if (isNewLike || isUpdateLike || isLost) {
                    // UPDATE-like packets need an existing UIN; NEW and LOST do not.
                    String uin = isUpdateLike ? resolvedUin : null;
                    boolean generateUin = isNewLike;

                    if (isUpdateLike && (StringUtils.isEmpty(uin) || "null".equalsIgnoreCase(uin))) {
                        regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                "UIN not found for UPDATE-type packet (" + regType + ") — permanent failure.");
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

                    // For reprocessed NEW packets: check if the previously committed UIN was
                    // deactivated. FinalizationStage stores the UIN in referenceRegistrationId
                    // on the first successful publish; reading it here avoids an unreliable
                    // RID-based ID Repo lookup that fails in the draft-based flow.
                    if (isNewLike) {
                        String previousUin = registrationStatusDto.getReferenceRegistrationId();
                        if (previousUin != null && !previousUin.isEmpty()) {
                            try {
                                String idrepoStatus = utilities.retrieveIdrepoJsonStatus(previousUin);
                                if (RegistrationType.DEACTIVATED.name().equalsIgnoreCase(idrepoStatus)) {
                                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                            "Reprocessed NEW packet: previously committed UIN is deactivated — rejecting.");
                                    registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
                                    registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_FAILED.getMessage());
                                    registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_FAILED.getCode());
                                    registrationStatusDto.setLatestTransactionStatusCode(
                                            RegistrationTransactionStatusCode.FAILED.toString());
                                    description.setCode(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode());
                                    description.setMessage(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getMessage());
                                    object.setIsValid(Boolean.FALSE);
                                    object.setInternalError(Boolean.FALSE);
                                    return object;
                                }
                            } catch (Exception e) {
                                regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                        "Could not check deactivation status for reprocessed NEW packet, proceeding: " + e.getMessage());
                            }
                        }
                    }

                    if (idrepoDraftService.idrepoHasDraft(registrationId)) {
                        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                "Draft already exists for " + regType + ". Discarding before re-creation.");
                        idrepoDraftService.idrepoDiscardDraft(registrationId);
                    }

                    // Fix 1: createDraftV2 runs in parallel with packet data fetching
                    final boolean injectPacketCreatedOn = RegistrationType.NEW.toString().equalsIgnoreCase(regType)
                            || RegistrationType.UPDATE.toString().equalsIgnoreCase(regType);
                    final String regTypeForPayload = registrationStatusDto.getRegistrationType();
                    final String uinForDraft = uin;
                    final boolean generateUinFinal = generateUin;
                    ExecutorService draftExecutor = Executors.newVirtualThreadPerTaskExecutor();
                    CompletableFuture<Boolean> createFuture = CompletableFuture.supplyAsync(() -> {
                        try { return idrepoDraftService.idrepoCreateDraftV2(registrationId, uinForDraft, generateUinFinal); }
                        catch (Exception e) { throw new CompletionException(e); }
                    }, draftExecutor);
                    CompletableFuture<IdRequestDto> payloadFuture = CompletableFuture.supplyAsync(() -> {
                        // Always fetch all default fields for initial draft creation.
                        // lostPacketUpdateFields restriction is only for UIN Generator's update logic.
                        try { return buildDraftPayload(registrationId, regTypeForPayload, uinForDraft, false, injectPacketCreatedOn); }
                        catch (Exception e) { throw new CompletionException(e); }
                    }, draftExecutor);
                    boolean created;
                    IdRequestDto draftPayload;
                    try {
                        created = createFuture.join();
                        draftPayload = payloadFuture.join();
                    } catch (CompletionException ex) {
                        Throwable cause = ex.getCause();
                        while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
                        sneakyThrow(cause);
                        throw new RuntimeException(); // unreachable
                    } finally {
                        draftExecutor.close();
                    }
                    if (!created) {
                        throw new IdrepoDraftException(
                                PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode(),
                                PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getMessage());
                    }
                    IdResponseDTO draftResponse = idrepoDraftService.idrepoUpdateDraft(registrationId, uin, draftPayload);
                    if (draftResponse != null && draftResponse.getResponse() != null
                            && !"DRAFTED".equalsIgnoreCase(draftResponse.getResponse().getStatus())) {
                        regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                                "Draft update returned unexpected status: " + draftResponse.getResponse().getStatus());
                    }

                    isTransactionSuccessful = Boolean.TRUE;
                    object.setIsValid(Boolean.TRUE);
                    registrationStatusDto.setLatestTransactionStatusCode(
                            RegistrationTransactionStatusCode.SUCCESS.toString());
                    registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                    registrationStatusDto.setStatusComment(StatusUtil.CREATE_DRAFT_SUCCESS.getMessage());
                    registrationStatusDto.setSubStatusCode(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
                    description.setCode(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode());
                    description.setMessage(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getMessage());
                    regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                            "Draft created successfully for regType=" + regType);

                } else {
                    // Pass-through for unhandled packet types
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
                    description.setCode(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode());
                    description.setMessage(PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getMessage());
                }
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

        } catch (JsonProcessingException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.FAILED.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
            isTransactionSuccessful = false;
            description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
            object.setRid(registrationStatusDto.getRegistrationId());

        } catch (IOException e) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            object.setInternalError(Boolean.TRUE);
            description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());

        } catch (PacketManagerNonRecoverableException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.FAILED.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION));
            description.setMessage(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
            object.setRid(registrationStatusDto.getRegistrationId());

        } catch (PacketManagerException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    RegistrationStatusCode.PROCESSING.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                    StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
            description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
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
                        ? PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode()
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
            String effectiveProcess = utilities.getInternalProcess(additionalProcessCategoryMapping, regType);
            boolean needsUin = RegistrationType.UPDATE.toString().equalsIgnoreCase(regType)
                    || RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(regType)
                    || RegistrationType.ACTIVATED.toString().equalsIgnoreCase(regType)
                    || RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(regType)
                    || RegistrationType.UPDATE.toString().equalsIgnoreCase(effectiveProcess)
                    || RegistrationType.ACTIVATED.toString().equalsIgnoreCase(effectiveProcess)
                    || RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(effectiveProcess);
            if (needsUin) {
                return utility.getUIn(registrationId, registrationStatusDto.getRegistrationType(),
                        ProviderStageName.CREATE_DRAFT);
            }
        } catch (Exception e) {
            regProcLogger.warn(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    "CreateDraftStage :: Could not resolve UIN: " + e.getMessage());
        }
        return null;
    }

    private boolean isUpdateType(String regType, String effectiveProcess) {
        return RegistrationType.UPDATE.toString().equalsIgnoreCase(regType)
                || RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(regType)
                || RegistrationType.UPDATE.toString().equalsIgnoreCase(effectiveProcess);
    }

    private void markAsObsoleted(InternalRegistrationStatusDto dto, MessageDTO object, LogDescription description) {
        dto.setStatusCode(io.mosip.registration.processor.status.code.RegistrationStatusCode.FAILED.toString());
        dto.setStatusComment(StatusUtil.CREATE_DRAFT_STALE_PACKET.getMessage());
        dto.setSubStatusCode(StatusUtil.CREATE_DRAFT_STALE_PACKET.getCode());
        dto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                .getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
        description.setCode(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode());
        description.setMessage(StatusUtil.CREATE_DRAFT_STALE_PACKET.getMessage());
        description.setStatusCode(io.mosip.registration.processor.status.code.RegistrationStatusCode.FAILED.toString());
        description.setStatusComment(StatusUtil.CREATE_DRAFT_STALE_PACKET.getMessage());
        description.setSubStatusCode(StatusUtil.CREATE_DRAFT_STALE_PACKET.getCode());
        description.setTransactionStatusCode(registrationStatusMapperUtil
                .getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
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
     * Fetches all packet data (demographics + documents) needed for the draft payload.
     * Runs {@code getFields} in parallel with {@code retrieveCreatedDateFromPacket} (Fix 1 inner),
     * and fetches all documents in parallel (Fix 2). Returns the assembled {@link IdRequestDto}
     * ready for {@code idrepoUpdateDraft}.
     */
    private IdRequestDto buildDraftPayload(String registrationId, String process, String uin,
            boolean isLost, boolean injectPacketCreatedOn) throws Exception {
        String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId,
                MappingJsonConstants.IDSCHEMA_VERSION, process, ProviderStageName.CREATE_DRAFT);
        List<String> defaultFields = idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion));

        final List<String> fieldsToFetch;
        if (isLost && StringUtils.isNotEmpty(lostPacketUpdateFields)
                && !"null".equalsIgnoreCase(lostPacketUpdateFields)) {
            JSONObject identityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            List<String> allowedFields = new ArrayList<>();
            for (String fieldName : lostPacketUpdateFields.split(",")) {
                String actualFieldName = JsonUtil.getJSONValue(
                        JsonUtil.getJSONObject(identityJson, fieldName.trim()), MappingJsonConstants.VALUE);
                if (StringUtils.isNotEmpty(actualFieldName)) {
                    allowedFields.add(actualFieldName);
                }
            }
            fieldsToFetch = allowedFields.isEmpty() ? defaultFields : allowedFields;
        } else {
            fieldsToFetch = defaultFields;
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            // Parallelize getFields with retrieveCreatedDateFromPacket
            CompletableFuture<Map<String, String>> fieldsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return packetManagerService.getFields(registrationId, fieldsToFetch, process,
                            ProviderStageName.CREATE_DRAFT);
                } catch (Exception e) { throw new CompletionException(e); }
            }, executor);

            CompletableFuture<String> createdOnFuture = null;
            if (injectPacketCreatedOn && defaultFields.contains(MappingJsonConstants.PACKET_CREATED_ON)) {
                createdOnFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return utility.retrieveCreatedDateFromPacket(registrationId, process,
                                ProviderStageName.CREATE_DRAFT);
                    } catch (Exception e) { throw new CompletionException(e); }
                }, executor);
            }

            Map<String, String> fieldMap;
            String packetCreatedOn = null;
            try {
                fieldMap = fieldsFuture.join();
                if (createdOnFuture != null) {
                    packetCreatedOn = createdOnFuture.join();
                }
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
                sneakyThrow(cause);
                throw new RuntimeException(); // unreachable
            }

            JSONObject demographicIdentity = new JSONObject();
            demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION,
                    convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);
            loadDemographicIdentity(fieldMap, demographicIdentity);

            if (packetCreatedOn != null) {
                updatePacketCreatedOnInDemographicIdentity(registrationId, demographicIdentity, packetCreatedOn);
            }

            // Fix 2: fetch all documents in parallel using the same executor
            List<Documents> documentInfo = fetchDocumentsInParallel(registrationId, process,
                    demographicIdentity, executor);

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
            idRequestDTO.setVersion(ID_REPO_API_VERSION);

            return idRequestDTO;
        } finally {
            executor.close();
        }
    }

    private void updatePacketCreatedOnInDemographicIdentity(String registrationId,
            Map<String, Object> demographicIdentity, String packetCreatedOn) throws IOException {
        if (packetCreatedOn == null) {
            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    "packetCreatedOn not found in packet — skipping.");
            return;
        }
        String packetCreatedOnKey = utility.getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        if (packetCreatedOnKey == null) {
            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                    "packetCreatedOn not mapped in identity-mapping.json — skipping.");
            return;
        }
        demographicIdentity.put(packetCreatedOnKey, packetCreatedOn);
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
     * Fetches all supporting documents and biometrics in parallel (Fix 2).
     * Submits each fetch as an independent task on the supplied executor so
     * POI, POA, POR, and biometrics are retrieved concurrently.
     */
    private List<Documents> fetchDocumentsInParallel(String regId, String process,
            JSONObject demographicIdentity, ExecutorService executor) throws Exception {
        JSONObject docJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
        JSONObject identityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

        String applicantBiometricLabel = JsonUtil.getJSONValue(
                JsonUtil.getJSONObject(identityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
                MappingJsonConstants.VALUE);
        HashMap<String, String> applicantBiometric =
                (HashMap<String, String>) demographicIdentity.get(applicantBiometricLabel);

        List<CompletableFuture<Documents>> futures = new ArrayList<>();

        for (Object doc : docJson.values()) {
            Map docMap = (LinkedHashMap) doc;
            String docValue = docMap.values().iterator().next().toString();
            HashMap<String, String> docInIdentityJson =
                    (HashMap<String, String>) demographicIdentity.get(docValue);
            if (docInIdentityJson != null) {
                final String docKey = docValue;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try { return getIdDocument(regId, docKey, process); }
                    catch (Exception e) { throw new CompletionException(e); }
                }, executor));
            }
        }

        if (applicantBiometric != null) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try { return getBiometricsDocument(regId, applicantBiometricLabel, process); }
                catch (Exception e) { throw new CompletionException(e); }
            }, executor));
        }

        List<Documents> result = new ArrayList<>();
        try {
            for (CompletableFuture<Documents> future : futures) {
                Documents d = future.join();
                if (d != null) result.add(d);
            }
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
            sneakyThrow(cause);
            throw new RuntimeException(); // unreachable
        }
        return result;
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
                idRequestDTO.setVersion(ID_REPO_API_VERSION);

                result = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

                if (isIdResponseNotNull(result)) {
                    if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(result.getResponse().getStatus())) {
                        isTransactionSuccessful = true;
                        description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
                        description.setStatusComment(StatusUtil.UIN_ACTIVATED_SUCCESS.getMessage());
                        description.setSubStatusCode(StatusUtil.UIN_ACTIVATED_SUCCESS.getCode());
                        description.setMessage(PlatformSuccessMessages.RPR_UIN_ACTIVATED_SUCCESS.getMessage());
                        description.setCode(PlatformSuccessMessages.RPR_UIN_ACTIVATED_SUCCESS.getCode());
                        description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
                        object.setIsValid(Boolean.TRUE);
                    } else {
                        description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                        description.setStatusComment(StatusUtil.UIN_ACTIVATED_FAILED.getMessage());
                        description.setSubStatusCode(StatusUtil.UIN_ACTIVATED_FAILED.getCode());
                        description.setMessage(PlatformErrorMessages.UIN_ACTIVATED_FAILED.getMessage());
                        description.setCode(PlatformErrorMessages.UIN_ACTIVATED_FAILED.getCode());
                        description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                        object.setIsValid(Boolean.FALSE);
                    }
                } else {
                    String statusComment = result != null && result.getErrors() != null
                            ? result.getErrors().get(0).getMessage() : "Null response from Id Repo";
                    description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                    description.setStatusComment(trimExceptionMessage
                            .trimExceptionMessage(StatusUtil.UIN_REACTIVATION_FAILED.getMessage() + statusComment));
                    description.setSubStatusCode(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
                    description.setMessage(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getMessage());
                    description.setCode(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                    object.setIsValid(Boolean.FALSE);
                }
            }
        } else {
            String statusComment = result != null && result.getErrors() != null
                    ? result.getErrors().get(0).getMessage() : "Null response from Id Repo";
            description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
            description.setStatusComment(trimExceptionMessage
                    .trimExceptionMessage(StatusUtil.UIN_REACTIVATION_FAILED.getMessage() + statusComment));
            description.setSubStatusCode(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
            description.setMessage(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.UIN_REACTIVATION_FAILED.getCode());
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
            description.setStatusComment(StatusUtil.UIN_ALREADY_DEACTIVATED.getMessage());
            description.setSubStatusCode(StatusUtil.UIN_ALREADY_DEACTIVATED.getCode());
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
            idRequestDTO.setVersion(ID_REPO_API_VERSION);

            idResponseDto = idrepoDraftService.idrepoUpdateDraft(id, uin, idRequestDTO);

            if (isIdResponseNotNull(idResponseDto)) {
                if (idResponseDto.getResponse().getStatus()
                        .equalsIgnoreCase(RegistrationType.DEACTIVATED.toString())) {
                    description.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
                    description.setStatusComment(StatusUtil.UIN_DEACTIVATION_SUCCESS.getMessage());
                    description.setSubStatusCode(StatusUtil.UIN_DEACTIVATION_SUCCESS.getCode());
                    description.setMessage(PlatformSuccessMessages.RPR_UIN_DEACTIVATION_SUCCESS.getMessage());
                    description.setCode(PlatformSuccessMessages.RPR_UIN_DEACTIVATION_SUCCESS.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
                    object.setIsValid(Boolean.TRUE);
                    statusComment = idResponseDto.getResponse().getStatus();
                } else {
                    statusComment = idResponseDto.getResponse().getStatus();
                    description.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
                    description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
                            StatusUtil.UIN_DEACTIVATION_FAILED.getMessage() + statusComment));
                    description.setSubStatusCode(StatusUtil.UIN_DEACTIVATION_FAILED.getCode());
                    description.setMessage(PlatformErrorMessages.UIN_DEACTIVATION_FAILED.getMessage());
                    description.setCode(PlatformErrorMessages.UIN_DEACTIVATION_FAILED.getCode());
                    description.setTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
                    object.setIsValid(Boolean.FALSE);
                }
            } else {
                statusComment = idResponseDto != null && idResponseDto.getErrors() != null
                        ? idResponseDto.getErrors().get(0).getMessage() : "Null response from Id Repo";
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
            }
            throw e;
        }
        return response;
    }

    private boolean isIdResponseNotNull(IdResponseDTO result) {
        return result != null && result.getResponse() != null;
    }
}
