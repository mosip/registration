package io.mosip.registration.processor.stages.biometric.extraction.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
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
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.pms.ExtractorDto;
import io.mosip.registration.processor.core.pms.ExtractorsDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.biometric.extraction.config",
		"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.stages.config",
		"io.mosip.kernel.packetmanager.config",
		"io.mosip.registration.processor.packet.manager.config",
		"io.mosip.registration.processor.core.kernel.beans"})
public class BiometricExtractionStage extends MosipVerticleAPIManager{
	/** regproc logger */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BiometricExtractionStage.class);
	/** stage properties prefix */
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.biometric.extraction.";
	private static final String USER = "MOSIP_SYSTEM";
	
	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;
	
	private TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
	
	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;
	
	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.biometric.extraction.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;
	
	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;
	
	/**partner policy ids */
	@Value("${biometric.extraction.default.partner.policy.ids}")
	private String partnerPolicyIdsJson;
	
	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** Mosip router for APIs */
	@Autowired
	private MosipRouter router;
	
	/** registration status mapper util */
	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	/** The registration processor rest client service. */
	@Autowired
	RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;
	
	/** The registration processor rest api client . */
	@Autowired 
	private IdrepoDraftService idrepoDraftService;
	
	@Autowired
	private ObjectMapper mapper;
	
	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;
	
	@Override
	protected String getPropertyPrefix() {
		// TODO Auto-generated method stub
		return STAGE_PROPERTY_PREFIX;
	}
	
	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.BIOMETRIC_EXTRACTION_BUS_IN,
				MessageBusAddress.BIOMETRIC_EXTRACTION_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.BIOMETRIC_EXTRACTION_BUS_IN,
				MessageBusAddress.BIOMETRIC_EXTRACTION_BUS_OUT));
		this.createServer(router.getRouter(), getPort());

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */
	@Override
	public MessageDTO process(MessageDTO object) {
		boolean isTransactionSuccessful = Boolean.FALSE;
		object.setMessageBusAddress(MessageBusAddress.BIOMETRIC_EXTRACTION_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);
		LogDescription description = new LogDescription();
		String registrationId = object.getRid();
		InternalRegistrationStatusDto registrationStatusDto=null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "BiometricExtractionStage::process()::entry");
		try {
		 registrationStatusDto = registrationStatusService.getRegistrationStatus(
				registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
		registrationStatusDto
			.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.BIOMETRIC_EXTRACTION.toString());
		registrationStatusDto.setRegistrationStageName(getStageName());
		
		
			if(!idrepoDraftService.idrepoHasDraft(registrationStatusDto.getRegistrationId())) {
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.DRAFT_REQUEST_UNAVAILABLE));
				description.setTransactionStatusCode(registrationStatusMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.DRAFT_REQUEST_UNAVAILABLE));
				registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getCode());
				registrationStatusDto.setStatusComment(trimExceptionMessage
						.trimExceptionMessage(StatusUtil.BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getMessage()));
				object.setInternalError(Boolean.TRUE);
				isTransactionSuccessful = false;
				description.setMessage(PlatformErrorMessages.RPR_BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getMessage());
				description.setCode(PlatformErrorMessages.RPR_BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getCode());
				description.setSubStatusCode(StatusUtil.BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getCode());

				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						StatusUtil.BIOMETRIC_EXTRACTION_DRAFT_REQUEST_UNAVAILABLE.getMessage());
			}
			else {
				ExtractorsDto extractorsDto=getExtractors(registrationStatusDto.getRegistrationId());
				if(extractorsDto.getExtractors()!=null && !extractorsDto.getExtractors().isEmpty()) {
				for(ExtractorDto dto:extractorsDto.getExtractors()) {
					addBiometricExtractiontoIdRepository(dto,registrationStatusDto.getRegistrationId());	
				}
				}
				else {
					throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_PMS_BIOMETRIC_EXTRACTION_NULL_RESPONSE.getCode(),
							PlatformErrorMessages.RPR_PMS_BIOMETRIC_EXTRACTION_NULL_RESPONSE.getMessage());
				}
				registrationStatusDto.setStatusComment(StatusUtil.BIOMETRIC_EXTRACTION_SUCCESS.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_EXTRACTION_SUCCESS.getCode());
				isTransactionSuccessful = true;
				object.setIsValid(Boolean.TRUE);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				description.setMessage(PlatformSuccessMessages.RPR_BIOMETRIC_EXTRACTION_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_BIOMETRIC_EXTRACTION_SUCCESS.getCode());
				description.setTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSING.toString());
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());
			registrationStatusDto.setUpdatedBy(USER);
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
			description.setCode(PlatformErrorMessages.RPR_BIOMETRIC_EXTRACTION_API_RESOURCE_EXCEPTION.getCode());
		} catch (JSONException | JsonProcessingException e) {
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
			e.printStackTrace();
		}catch (RegistrationProcessorCheckedException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.BASE_CHECKED_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BASE_CHECKED_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BASE_CHECKED_EXCEPTION));
			description.setMessage(e.getMessage());
			description.setCode(e.getErrorCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			isTransactionSuccessful = false;
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + registrationId,
					e.getErrorCode() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		}catch (IdrepoDraftException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.IDREPO_DRAFT_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_EXCEPTION));
			description.setMessage(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		}catch (Exception ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXTRACTION_FAILED));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getCode());
		}
		finally {
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
					? PlatformSuccessMessages.RPR_BIOMETRIC_EXTRACTION_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.BIOMETRIC_EXTRACTION.toString();
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

	/**
	 * add biometric extractions to id repo
	 * @param dto
	 * @param registrationId
	 * @throws ApisResourceAccessException
	 * @throws RegistrationProcessorCheckedException 
	 */
	private IdResponseDTO addBiometricExtractiontoIdRepository(ExtractorDto dto,
			String registrationId) throws ApisResourceAccessException, RegistrationProcessorCheckedException {
		String extractionFormat = "";
		if(dto.getBiometric().equals("iris")) {
			extractionFormat="irisExtractionFormat";
		}if(dto.getBiometric().equals("face")) {
			extractionFormat="faceExtractionFormat";
		}if(dto.getBiometric().equals("finger")) {
			extractionFormat="fingerExtractionFormat";
		}
		List<String> segments=List.of(registrationId);
		IdResponseDTO response= (IdResponseDTO) registrationProcessorRestClientService.putApi(ApiName.IDREPOEXTRACTBIOMETRICS, segments, extractionFormat, dto.getAttributeName(), null, IdResponseDTO.class, null);
		if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            regProcLogger.error("Error occured while updating draft for id : " + registrationId, response.getErrors().iterator().next().toString());
            throw new RegistrationProcessorCheckedException(response.getErrors().iterator().next().getErrorCode(),
            		response.getErrors().iterator().next().getMessage());
        }
		return response;
	}
	/**
	 * get the list of biometric extractors
	 * @param id
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws ApisResourceAccessException
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 * @throws RegistrationProcessorCheckedException 
	 */
	private ExtractorsDto getExtractors(String id) throws JSONException, ApisResourceAccessException, JsonParseException, JsonMappingException, JsonProcessingException, IOException, RegistrationProcessorCheckedException {
		JSONArray jArray=new JSONArray(partnerPolicyIdsJson);
		ExtractorsDto extractorsDto=new ExtractorsDto();
		 List<ErrorDTO> errors = new ArrayList<>();
		for(int i=0;i<jArray.length();i++) {
			List<String> pathsegments=new ArrayList<>();
			pathsegments.add(jArray.getJSONObject(i).getString("partnerId"));
			pathsegments.add("bioextractors");
			pathsegments.add(jArray.getJSONObject(i).getString("policyId"));
			
			ResponseWrapper<?> responseWrapper=(ResponseWrapper<?>) registrationProcessorRestClientService.
					getApi(ApiName.PARTNERGETBIOEXTRACTOR, pathsegments, "", "", ResponseWrapper.class);
			if(responseWrapper.getResponse() !=null) {
				extractorsDto=mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
					ExtractorsDto.class);
				if(extractorsDto.getExtractors()!=null &&!extractorsDto.getExtractors().isEmpty()) {
					return extractorsDto;
				}
				break;
			}
			else if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
	            regProcLogger.error("Error occured while getting  biometric extractors.", responseWrapper.getErrors().iterator().next().toString());
	            errors.addAll(responseWrapper.getErrors());
	        }
			
		}
		if(errors!=null && !errors.isEmpty()) {
			throw new RegistrationProcessorCheckedException(errors.iterator().next().getErrorCode(),
					errors.iterator().next().getMessage());
		}
		return extractorsDto;
	}
	/**
	 * update Error Flags
	 * @param registrationStatusDto
	 * @param object
	 */
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
