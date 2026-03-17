package io.mosip.registration.processor.stages.uingenerator.stage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.registration.processor.packet.storage.utils.*;
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

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
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
import io.mosip.registration.processor.packet.storage.utils.Utility;
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
		boolean isTransactionSuccessful = false;
		object.setMessageBusAddress(MessageBusAddress.UIN_GENERATION_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.TRUE);
		LogDescription description = new LogDescription();
		String registrationId = object.getRid();

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
				"UinGeneratorStage::process()::entry");

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());

		try {
			registrationStatusDto.setLatestTransactionTypeCode(
					RegistrationTransactionTypeCode.UIN_GENERATOR.toString());
			registrationStatusDto.setRegistrationStageName(getStageName());

			if (RegistrationType.LOST.toString().equalsIgnoreCase(object.getReg_type())) {
				String lostPacketRegId = object.getRid();
				String matchedRegId = regLostUinDetEntity
						.getLostUinMatchedRegIdByWorkflowId(object.getWorkflowInstanceId());
				if (matchedRegId != null) {
					regProcLogger.info("Match for lostPacketRegId" + lostPacketRegId + " is " + matchedRegId);
					lostAndUpdateUin(lostPacketRegId, matchedRegId,
							registrationStatusDto.getRegistrationType(), object, description);
				}
			} else {
				IdResponseDTO idResponseDTO = new IdResponseDTO();

				// ── Parallel fan-out: getUIn + schemaVersion + fields ────────────
				ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
				try {
					String regType = registrationStatusDto.getRegistrationType();

					CompletableFuture<String> uinFuture = CompletableFuture.supplyAsync(() -> {
						try {
							return utility.getUIn(registrationId, regType, ProviderStageName.UIN_GENERATOR);
						} catch (Exception e) { throw new CompletionException(e); }
					}, exec);

					CompletableFuture<String> schemaFuture = CompletableFuture.supplyAsync(() -> {
						try {
							return packetManagerService.getFieldByMappingJsonKey(registrationId,
									MappingJsonConstants.IDSCHEMA_VERSION, regType,
									ProviderStageName.UIN_GENERATOR);
						} catch (Exception e) { throw new CompletionException(e); }
					}, exec);

					// schemaVersion needed to build the fields list, so chain fieldsFuture on schemaFuture
					CompletableFuture<Map<String, String>> fieldsFuture = schemaFuture.thenApplyAsync(sv -> {
						try {
							return packetManagerService.getFields(registrationId,
									idSchemaUtil.getDefaultFields(Double.parseDouble(sv)),
									regType, ProviderStageName.UIN_GENERATOR);
						} catch (Exception e) { throw new CompletionException(e); }
					}, exec);

					// Await both independent paths together
					CompletableFuture.allOf(uinFuture, fieldsFuture).join();

					String uinField   = unwrap(uinFuture);
					String schemaVersion = unwrap(schemaFuture);
					Map<String, String> fieldMap = unwrap(fieldsFuture);
					// ─────────────────────────────────────────────────────────────

					JSONObject demographicIdentity = new JSONObject();
					demographicIdentity.put(MappingJsonConstants.IDSCHEMA_VERSION,
							convertIdschemaToDouble ? Double.valueOf(schemaVersion) : schemaVersion);

					loadDemographicIdentity(fieldMap, demographicIdentity);
					updatePacketCreatedOnInDemographicIdentity(
							registrationId, registrationStatusDto, demographicIdentity, object);

					if (StringUtils.isEmpty(uinField) || "null".equalsIgnoreCase(uinField)) {
						idResponseDTO = sendIdRepoWithUin(registrationId, regType,
								demographicIdentity, uinField);
						boolean isUinAlreadyPresent = isUinAlreadyPresent(idResponseDTO, registrationId);

						if (isIdResponseNotNull(idResponseDTO) || isUinAlreadyPresent) {
							registrationStatusDto.setStatusComment(
									StatusUtil.UIN_GENERATED_SUCCESS.getMessage());
							registrationStatusDto.setSubStatusCode(
									StatusUtil.UIN_GENERATED_SUCCESS.getCode());
							isTransactionSuccessful = true;
							registrationStatusDto.setStatusCode(
									RegistrationStatusCode.PROCESSING.toString());
							description.setMessage(
									PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getMessage());
							description.setCode(
									PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode());
							description.setTransactionStatusCode(
									RegistrationTransactionStatusCode.SUCCESS.toString());
						} else {
							List<ErrorDTO> errors = idResponseDTO != null
									? idResponseDTO.getErrors() : Collections.emptyList();

							// ── Single-pass over errors (was two passes) ─────────
							String statusComment = errors.isEmpty()
									? UINConstants.NULL_IDREPO_RESPONSE : errors.get(0).getMessage();
							boolean hasRecoverableError = errors.stream().anyMatch(dto ->
									dto.getErrorCode().equalsIgnoreCase("IDR-IDC-004") ||
											dto.getErrorCode().equalsIgnoreCase("IDR-IDC-001"));
							// ─────────────────────────────────────────────────────

							if (hasRecoverableError) {
								registrationStatusDto.setStatusCode(
										RegistrationStatusCode.PROCESSING.toString());
								registrationStatusDto.setLatestTransactionStatusCode(
										registrationStatusMapperUtil.getStatusCode(
												RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
								description.setTransactionStatusCode(
										registrationStatusMapperUtil.getStatusCode(
												RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS));
							} else {
								registrationStatusDto.setStatusCode(
										RegistrationStatusCode.FAILED.toString());
								registrationStatusDto.setLatestTransactionStatusCode(
										registrationStatusMapperUtil.getStatusCode(
												RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
								description.setTransactionStatusCode(
										registrationStatusMapperUtil.getStatusCode(
												RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_FAILED));
							}
							registrationStatusDto.setStatusComment(trimExceptionMessage
									.trimExceptionMessage(
											StatusUtil.UIN_GENERATION_FAILED.getMessage() + statusComment));
							object.setInternalError(Boolean.TRUE);
							isTransactionSuccessful = false;
							description.setMessage(
									PlatformErrorMessages.RPR_UGS_UIN_UPDATE_FAILURE.getMessage());
							description.setCode(
									PlatformErrorMessages.RPR_UGS_UIN_UPDATE_FAILURE.getCode());
							description.setSubStatusCode(StatusUtil.UIN_GENERATION_FAILED.getCode());
							regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
									LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
									statusComment + "  :  " + (idResponseDTO != null
											? idResponseDTO : UINConstants.NULL_IDREPO_RESPONSE));
							object.setIsValid(Boolean.FALSE);
						}
					} else {
						String rt = object.getReg_type();
						if (RegistrationType.ACTIVATED.toString().equalsIgnoreCase(rt)) {
							isTransactionSuccessful = reActivateUin(idResponseDTO, registrationId,
									uinField, object, demographicIdentity, description);
						} else if (RegistrationType.DEACTIVATED.toString().equalsIgnoreCase(rt)) {
							idResponseDTO = deactivateUin(registrationId, uinField, object,
									demographicIdentity, description);
						} else if (RegistrationType.UPDATE.toString().equalsIgnoreCase(rt)
								|| RegistrationType.RES_UPDATE.toString().equalsIgnoreCase(rt)
								|| RegistrationType.UPDATE.toString().equalsIgnoreCase(
								utilities.getInternalProcess(additionalProcessCategoryMapping, rt))) {
							isTransactionSuccessful = uinUpdate(registrationId, regType, uinField,
									object, demographicIdentity, description);
						}
					}
				} finally {
					exec.close();
				}
			}

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), registrationId, description.getMessage());
			registrationStatusDto.setUpdatedBy(UINConstants.USER);

		} catch (io.mosip.kernel.core.util.exception.JsonProcessingException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.FAILED, StatusUtil.JSON_PARSING_EXCEPTION,
					RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION,
					PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION, e);
			registrationStatusDto.setRegistrationId(registrationStatusDto.getRegistrationId());
		} catch (PacketManagerNonRecoverableException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.FAILED, StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION,
					RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION,
					PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION, e);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (PacketManagerException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.PACKET_MANAGER_EXCEPTION,
					RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION,
					PlatformErrorMessages.PACKET_MANAGER_EXCEPTION, e);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (ApisResourceAccessException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.API_RESOUCE_ACCESS_FAILED,
					RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION,
					PlatformErrorMessages.RPR_UGS_API_RESOURCE_EXCEPTION, e);
		} catch (IOException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.IO_EXCEPTION,
					RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS,
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION, e);
		} catch (IdrepoDraftException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.IDREPO_DRAFT_EXCEPTION,
					RegistrationExceptionTypeCode.IDREPO_DRAFT_EXCEPTION,
					PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION, e);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (IdrepoDraftReprocessableException e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION,
					RegistrationExceptionTypeCode.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION,
					PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION, e);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (Exception e) {
			handleException(registrationId, registrationStatusDto, object, description,
					RegistrationStatusCode.PROCESSING, StatusUtil.UNKNOWN_EXCEPTION_OCCURED,
					RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS,
					PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION, e);
		} finally {
			applyDescriptionToStatus(description, registrationStatusDto);
			if (object.getInternalError()) updateErrorFlags(registrationStatusDto, object);

			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_UIN_GENERATOR_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.UIN_GENERATOR.toString();
			String eventId = isTransactionSuccessful
					? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			boolean isSuccess = isTransactionSuccessful;

			// ── Parallel finally I/O ──────────────────────────────────────────
			try (ExecutorService finallyExec = Executors.newVirtualThreadPerTaskExecutor()) {
				CompletableFuture<Void> updateFuture = CompletableFuture.runAsync(() ->
						registrationStatusService.updateRegistrationStatus(
								registrationStatusDto, moduleId, moduleName), finallyExec);

				CompletableFuture<Void> auditFuture = CompletableFuture.runAsync(() -> {
					String eventName = isSuccess ? EventName.UPDATE.toString()
							: EventName.EXCEPTION.toString();
					String eventType = isSuccess ? EventType.BUSINESS.toString()
							: EventType.SYSTEM.toString();
					auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(),
							eventId, eventName, eventType, moduleId, moduleName, registrationId);
				}, finallyExec);

				CompletableFuture.allOf(updateFuture, auditFuture).join();
			}
			// ─────────────────────────────────────────────────────────────────
		}

		return object;
	}

// ── Helpers ───────────────────────────────────────────────────────────────

	/** Unwraps a CompletableFuture, re-throwing the root cause without wrapping. */
	@SuppressWarnings("unchecked")
	private static <T> T unwrap(CompletableFuture<T> future) throws Exception {
		try {
			return future.join();
		} catch (CompletionException e) {
			Throwable cause = e.getCause();
			while (cause instanceof CompletionException && cause.getCause() != null)
				cause = cause.getCause();
			if (cause instanceof Exception ex) throw ex;
			throw e;
		}
	}

	/** Consolidates the repetitive exception-handler boilerplate into one call. */
	private void handleException(String registrationId,
								 InternalRegistrationStatusDto statusDto, MessageDTO object,
								 LogDescription description, RegistrationStatusCode statusCode,
								 StatusUtil statusUtil, RegistrationExceptionTypeCode exTypeCode,
								 PlatformErrorMessages platformMsg, Exception e) {

		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
				statusCode + " " + e.getMessage() + ExceptionUtils.getStackTrace(e));
		statusDto.setStatusCode(statusCode.name());
		statusDto.setStatusComment(trimExceptionMessage
				.trimExceptionMessage(statusUtil.getMessage() + e.getMessage()));
		statusDto.setSubStatusCode(statusUtil.getCode());
		statusDto.setLatestTransactionStatusCode(
				registrationStatusMapperUtil.getStatusCode(exTypeCode));
		description.setMessage(platformMsg.getMessage());
		description.setCode(platformMsg.getCode());
		object.setInternalError(Boolean.TRUE);
	}

	/** Copies non-null description fields back onto the status DTO (finally block). */
	private void applyDescriptionToStatus(LogDescription description,
										  InternalRegistrationStatusDto statusDto) {
		if (description.getStatusComment()      != null) statusDto.setStatusComment(description.getStatusComment());
		if (description.getStatusCode()         != null) statusDto.setStatusCode(description.getStatusCode());
		if (description.getSubStatusCode()      != null) statusDto.setSubStatusCode(description.getSubStatusCode());
		if (description.getTransactionStatusCode() != null)
			statusDto.setLatestTransactionStatusCode(description.getTransactionStatusCode());
	}
	private void loadDemographicIdentity(Map<String, String> fieldMap, JSONObject demographicIdentity)
			throws IOException, JSONException {

		// Pre-size to avoid rehashing; parallel threshold: only worthwhile above ~10 entries
		// since ConcurrentHashMap + virtual-thread overhead > gain for tiny maps
		final int size = fieldMap.size();
		if (size == 0) return;

		if (size < 10) {
			// ── Fast path: small map, stay single-threaded ─────────────────
			for (Map.Entry<String, String> e : fieldMap.entrySet()) {
				String key   = e.getKey();
				String value = e.getValue();
				if (value == null) continue;                       // skip nulls (toString() redundancy removed)
				demographicIdentity.putIfAbsent(key, parseValue(value));
			}
		} else {
			// ── Hot path: large map, parse entries in parallel ──────────────
			// Collect results into a plain ConcurrentHashMap first to avoid
			// JSONObject lock contention, then bulk-merge once.
			ConcurrentHashMap<String, Object> staging = new ConcurrentHashMap<>(size * 2);

			try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
				List<CompletableFuture<Void>> futures = new ArrayList<>(size);

				for (Map.Entry<String, String> e : fieldMap.entrySet()) {
					String key   = e.getKey();
					String value = e.getValue();
					if (value == null) continue;

					futures.add(CompletableFuture.runAsync(() -> {
						try {
							staging.put(key, parseValue(value));
						} catch (IOException | JSONException ex) {
							throw new CompletionException(ex);
						}
					}, exec));
				}

				try {
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
				} catch (CompletionException ex) {
					Throwable cause = ex.getCause();
					if (cause instanceof IOException ioe)      throw ioe;
					if (cause instanceof JSONException je)     throw je;
					throw ex;
				}
			}

			// Single-threaded merge: putIfAbsent semantics preserved
			staging.forEach((k, v) -> demographicIdentity.putIfAbsent(k, v));
		}
	}

	/**
	 * Parses a single field value into the appropriate Java type.
	 * Extracted so both the single-threaded and parallel paths share one code path,
	 * and so objectMapper is called only when the value is actually an object/array.
	 *
	 * Fast-path heuristic: check the first non-whitespace character before allocating
	 * a JSONTokener — avoids the most expensive case (object construction + tokenizing)
	 * for plain scalar values, which are by far the most common field type.
	 */
	private Object parseValue(String value) throws IOException, JSONException {
		if (value == null) return null;

		// ── Scalar fast-path: skip JSON parsing entirely ────────────────────────
		// If the value doesn't start with '{' or '[' it can't be a JSON object/array.
		// Trim only to find the first real char; don't allocate a trimmed copy.
		int start = 0;
		while (start < value.length() && value.charAt(start) <= ' ') start++;
		if (start == value.length()) return value;             // blank string

		char first = value.charAt(start);

		if (first == '{') {
			// JSON object → deserialize directly via objectMapper (skips JSONTokener)
			return objectMapper.readValue(value, HashMap.class);
		}

		if (first == '[') {
			// JSON array → parse once, reuse the JSONArray (no re-parse per element)
			JSONArray jsonArray = new JSONArray(value);        // single parse
			int len = jsonArray.length();
			List<Object> jsonList = new ArrayList<>(len);      // pre-sized

			for (int i = 0; i < len; i++) {
				Object obj = jsonArray.get(i);
				if (obj instanceof String s) {
					jsonList.add(s);
				} else {
					String raw = obj.toString();
					HashMap<String, Object> hashMap = objectMapper.readValue(raw, HashMap.class);
					if (trimWhitespaces) {
						Object val = hashMap.get("value");
						if (val instanceof String s) hashMap.put("value", s.strip()); // strip() > trim() (handles Unicode spaces)
					}
					jsonList.add(hashMap);
				}
			}
			return jsonList;
		}

		// Plain scalar (number, boolean, quoted string, etc.)
		return value;
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
	private IdResponseDTO sendIdRepoWithUin(String id, String process,
											JSONObject demographicIdentity, String uin) throws Exception {

		// ── Parallel: fetch documents + timestamp concurrently ──────────────────
		// These two are completely independent — no reason to serialize them.
		// getAllDocumentsByRegId is the heavy I/O call (packet manager fetch);
		// getUTCCurrentDateTimeString may involve a clock/format call — cheap but
		// free to overlap. Both run on virtual threads so no carrier thread is blocked.
		List<Documents> documentInfo;
		String requestTime;

		try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
			CompletableFuture<List<Documents>> docsFuture = CompletableFuture.supplyAsync(
					() -> {
						try {
							return getAllDocumentsByRegId(id, process, demographicIdentity);
						} catch (Exception e) { throw new CompletionException(e); }
					}, exec);

			CompletableFuture<String> timeFuture = CompletableFuture.supplyAsync(
					DateUtils2::getUTCCurrentDateTimeString, exec);

			try {
				CompletableFuture.allOf(docsFuture, timeFuture).join();
			} catch (CompletionException e) {
				Throwable cause = unwrapCompletionException(e);
				if (cause instanceof Exception ex) throw ex;
				throw e;
			}

			documentInfo = docsFuture.join();
			requestTime  = timeFuture.join();
		}
		// ────────────────────────────────────────────────────────────────────────

		// ── Build request objects (pure CPU, no I/O) ─────────────────────────────
		// RequestDto and IdRequestDto are independent; inline both to avoid
		// intermediate variable churn. No parallelism needed here — it's just
		// field assignments, negligible cost.
		RequestDto requestDto = new RequestDto();
		requestDto.setIdentity(demographicIdentity);
		requestDto.setDocuments(documentInfo);
		requestDto.setRegistrationId(id);
		requestDto.setStatus(RegistrationType.ACTIVATED.toString());
		requestDto.setBiometricReferenceId(uin);

		IdRequestDto idRequestDTO = new IdRequestDto();
		idRequestDTO.setId(idRepoUpdate);
		idRequestDTO.setRequest(requestDto);
		idRequestDTO.setRequesttime(requestTime);
		idRequestDTO.setVersion(UINConstants.idRepoApiVersion);
		idRequestDTO.setMetadata(null);
		// ────────────────────────────────────────────────────────────────────────

		// ── Call idRepo, unwrap HTTP errors cleanly ───────────────────────────────
		try {
			return idrepoDraftService.idrepoUpdateDraft(id, null, idRequestDTO);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error("Exception occurred updating draft for id {}", id, e);
			throw unwrapHttpException(e);   // never returns normally
		}
		// ────────────────────────────────────────────────────────────────────────
	}

// ── Helpers ──────────────────────────────────────────────────────────────────

	/**
	 * Unwraps nested CompletionExceptions down to the original root cause.
	 * Virtual-thread CompletableFutures double-wrap on re-throw; this cuts through all layers.
	 */
	private static Throwable unwrapCompletionException(CompletionException e) {
		Throwable cause = e.getCause();
		while (cause instanceof CompletionException && cause.getCause() != null)
			cause = cause.getCause();
		return cause;
	}

	/**
	 * Extracts the response body from an HttpClientErrorException or
	 * HttpServerErrorException and re-wraps it as ApisResourceAccessException.
	 * Falls through to re-throw the original if neither type matches.
	 *
	 * Replaces the duplicated instanceof+cast+throw blocks in the original.
	 */
	private static ApisResourceAccessException unwrapHttpException(ApisResourceAccessException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException hce)
			return new ApisResourceAccessException(hce.getResponseBodyAsString(), hce);
		if (cause instanceof HttpServerErrorException hse)
			return new ApisResourceAccessException(hse.getResponseBodyAsString(), hse);
		return e;   // neither — rethrow as-is
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
		JSONObject idJSON = demographicIdentity;

		// Mapping JSONs are cached after first call — fetch sequentially (no ForkJoinPool)
		JSONObject docJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
		JSONObject identityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

		String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
		HashMap<String, String> applicantBiometric = (HashMap<String, String>) idJSON.get(applicantBiometricLabel);

		// Fetch all documents in parallel using virtual threads
		List<CompletableFuture<Documents>> futures = new ArrayList<>();
		ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			for (Object doc : docJson.values()) {
				Map docMap = (LinkedHashMap) doc;
				String docValue = docMap.values().iterator().next().toString();
				HashMap<String, String> docInIdentityJson = (HashMap<String, String>) idJSON.get(docValue);
				if (docInIdentityJson != null) {
					futures.add(CompletableFuture.supplyAsync(() -> {
						try { return getIdDocumnet(regId, docValue, process); }
						catch (Exception e) { throw new CompletionException(e); }
					}, executor));
				}
			}
			if (applicantBiometric != null) {
				String biometricLabel = applicantBiometricLabel;
				futures.add(CompletableFuture.supplyAsync(() -> {
					try { return getBiometrics(regId, biometricLabel, process, biometricLabel); }
					catch (Exception e) { throw new CompletionException(e); }
				}, executor));
			}
			try {
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			} catch (CompletionException e) {
				executor.shutdownNow();
				Throwable cause = e.getCause();
				while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
				sneakyThrow(cause);
				throw new RuntimeException(); // unreachable
			}
		} finally {
			executor.close();
		}

		List<Documents> applicantDocuments = new ArrayList<>();
		for (CompletableFuture<Documents> f : futures) {
			Documents document = f.getNow(null);
			if (document != null) applicantDocuments.add(document);
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
		documentsInfoDto.setCategory(utilities.getMappingJsonValue(idDocLabel, MappingJsonConstants.IDENTITY));
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
		idRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
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
				for (String fieldName : updateFields) {
					String actualFieldName = JsonUtil.getJSONValue(
							JsonUtil.getJSONObject(regProcessorIdentityJson, fieldName),
							MappingJsonConstants.VALUE);
					if (StringUtils.isNotEmpty(actualFieldName)) {
						String fldValue = packetManagerService.getField(lostPacketRegId, actualFieldName, process,
								ProviderStageName.UIN_GENERATOR);
						if (null != fldValue)
							fieldMap.put(actualFieldName, fldValue);
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

	private void updatePacketCreatedOnInDemographicIdentity(String registrationId,
															InternalRegistrationStatusDto registrationStatusDto,
															Map<String, Object> demographicIdentity, MessageDTO object) throws IOException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {
		// update packetCreatedOn only for NEW and UPDATE registrations
		if (!RegistrationType.NEW.toString().equalsIgnoreCase(object.getReg_type()) &&
				!RegistrationType.UPDATE.toString().equalsIgnoreCase(object.getReg_type())) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Skipping update of packetCreatedOn. registrationType: {}", object.getReg_type());
			return; // skip for other registration types
		}

		// Try to fetch the key using getMappedFieldName
		String packetCreatedOnKey = utility.getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);

		if (packetCreatedOnKey == null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Mapping is not configured in identity-mapping.json. key: {}", MappingJsonConstants.PACKET_CREATED_ON);
			return; // Cannot insert if key is null
		}

		// Fallback to metaInfo if not present in packet
		String packetCreatedOn = utility.retrieveCreatedDateFromPacket(
				registrationId,
				registrationStatusDto.getRegistrationType(),
				ProviderStageName.UIN_GENERATOR
		);

		if (packetCreatedOn == null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"unable to find the packetCreatedOn from packet");
			return; // Cannot insert if value is null
		}

		// Insert into demographicIdentity only if both key and value are present
		demographicIdentity.put(packetCreatedOnKey, packetCreatedOn);
	}

}
