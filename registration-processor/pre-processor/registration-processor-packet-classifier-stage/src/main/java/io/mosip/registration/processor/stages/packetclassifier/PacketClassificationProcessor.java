package io.mosip.registration.processor.stages.packetclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
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
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class PacketClassificationProcessor contain the logic of how to invoke various 
 * configured tag generators, combine all generated tags and trigger adding them to the packet
 *
 * @author Vishwanath V
 */
@Service
@Transactional
public class PacketClassificationProcessor {

	/**
	 * The reg proc logger.
	 */
	private static Logger regProcLogger = 
		RegProcessorLogger.getLogger(PacketClassificationProcessor.class);

	/**
	 * The Constant USER.
	 */
	private static final String USER = "MOSIP_SYSTEM";

    private static final String VALUE = "value";

	/*
     * java class to trim exception message
     */
	private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

	/** The tag value that will be used by default when the packet does not have value for the tag field */
	@Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
	private String notAvailableTagValue;
	
	/**
	 * The packet manager service that will invoked for all the packet related activities
	 */
	@Autowired
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	@Autowired
	private PacketManagerService packetManagerService;

	/**
	 * The registration status service.
	 */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, 
		RegistrationStatusDto> registrationStatusService;

	/**
	 * The core audit request builder.
	 */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	/**
	 * The mapper util that holds the mapping to decide whether to fail or reprocess the packet
	 */
	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/**
	 * Frequently used util methods are available in this bean
	 */
	@Autowired
    private Utilities utility;

	/**
	 * Holds all the util methods to deal with Id schema
	 */
	@Autowired
	private IdSchemaUtil idSchemaUtil;

	/** 
	 * This List will contain all the tag generators that is applicable as per the configuration 
	 */
	@Autowired
	private List<TagGenerator> tagGenerators;

	/** 
	 * Id object fields required by all the configured tag generators will be maintained here
	 */
	private List<String> requiredIdObjectFieldNames;

	private String idSchemaVersionLabel;

	/**
	 * Once this bean is initialized and all properties are set, each configured tag
	 * generator is invoked to collect all the required Id object field names
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	private void collectRequiredIdObjectFieldNames() throws BaseCheckedException, IOException {
		regProcLogger.info("collectRequiredIdObjectFieldNames called in PostConstruct");
		try {
			requiredIdObjectFieldNames = new ArrayList<String>();
			org.json.simple.JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(
					MappingJsonConstants.IDENTITY);
			idSchemaVersionLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.IDSCHEMA_VERSION), VALUE);
			requiredIdObjectFieldNames.add(idSchemaVersionLabel);
			for(TagGenerator tagGenerator : tagGenerators) {
				regProcLogger.info("TagGenerator enabled : {}", tagGenerator.getClass().toString());
				List<String> idFieldNames;
				idFieldNames = tagGenerator.getRequiredIdObjectFieldNames();
				if(idFieldNames != null && !idFieldNames.isEmpty())
					requiredIdObjectFieldNames.addAll(idFieldNames);
			}
		} catch (BaseCheckedException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), 
				LoggerFileConstant.REGISTRATIONID.toString(),
				PlatformErrorMessages.RPR_PCM_COLLECT_IDOBJECT_FIELD_FAILED.getCode(),
				PlatformErrorMessages.RPR_PCM_COLLECT_IDOBJECT_FIELD_FAILED.getMessage() 
					+ e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), 
				LoggerFileConstant.REGISTRATIONID.toString(),
				PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(),
				PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage() 
					+ e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}

	/**
	 * Process methods will be called will the stage to perform packet classification for each packet
	 * @param object This is the actual event object will contains all the meta information about the packet
	 * @param stageName The stageName that needs to be used in audit and status updates
	 * @return The same event object with proper internal error and valid status set
	 */
	public MessageDTO process(MessageDTO object, String stageName) {

		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		String registrationId = "";

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setLatestTransactionTypeCode(
			RegistrationTransactionTypeCode.PACKET_CLASSIFICATION.toString());
		registrationStatusDto.setRegistrationStageName(stageName);

		try {
			object.setMessageBusAddress(MessageBusAddress.PACKET_CLASSIFIER_BUS_IN);
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), 
				LoggerFileConstant.REGISTRATIONID.toString(), "", 
				"PacketClassificationProcessor::process()::entry");
			registrationId = object.getRid();

			registrationStatusDto = registrationStatusService.getRegistrationStatus(
					registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
			registrationStatusDto.setLatestTransactionTypeCode(
						RegistrationTransactionTypeCode.PACKET_CLASSIFICATION.toString());
			registrationStatusDto.setRegistrationStageName(stageName);

			generateAndAddTags(registrationStatusDto.getWorkflowInstanceId(), registrationId, 
				registrationStatusDto.getRegistrationType(), object.getIteration());
			object.setTags(null);

			registrationStatusDto.setLatestTransactionStatusCode(
				RegistrationTransactionStatusCode.SUCCESS.toString());
			registrationStatusDto.setStatusComment(
				StatusUtil.PACKET_CLASSIFICATION_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_CLASSIFICATION_SUCCESS.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

			description.setMessage(
				PlatformSuccessMessages.RPR_PKR_PACKET_CLASSIFIER.getMessage() + " -- " + registrationId);
			description.setCode(PlatformSuccessMessages.RPR_PKR_PACKET_CLASSIFIER.getCode());

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
				description.getCode() + description.getMessage());

			object.setIsValid(Boolean.TRUE);
			object.setInternalError(Boolean.FALSE);
			isTransactionSuccessful = true;
		}catch(PacketManagerNonRecoverableException e){
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED,
					StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION, RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION,
					description, PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION, e);
		}catch (PacketManagerException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING, 
				StatusUtil.PACKET_MANAGER_EXCEPTION, RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION, 
				description, PlatformErrorMessages.PACKET_MANAGER_EXCEPTION, e);
		} catch (DataAccessException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING, 
				StatusUtil.DB_NOT_ACCESSIBLE, RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION, 
				description, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);
		} catch (IOException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, 
				StatusUtil.IO_EXCEPTION, RegistrationExceptionTypeCode.IOEXCEPTION, 
				description, PlatformErrorMessages.RPR_SYS_IO_EXCEPTION, e);
		} catch (ParsingException | JsonProcessingException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, 
				StatusUtil.JSON_PARSING_EXCEPTION, RegistrationExceptionTypeCode.PARSE_EXCEPTION, 
				description, PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION, e);
		} catch (TablenotAccessibleException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.PROCESSING, 
				StatusUtil.DB_NOT_ACCESSIBLE, RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION, 
				description, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);
		} catch (BaseUncheckedException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, 
				StatusUtil.BASE_UNCHECKED_EXCEPTION, RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION, 
				description, PlatformErrorMessages.RPR_PCM_BASE_UNCHECKED_EXCEPTION, e);
		} catch (BaseCheckedException e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, 
				StatusUtil.BASE_CHECKED_EXCEPTION, RegistrationExceptionTypeCode.BASE_CHECKED_EXCEPTION, 
				description, PlatformErrorMessages.RPR_PCM_BASE_CHECKED_EXCEPTION, e);
		} catch (Exception e) {
			updateDTOsAndLogError(registrationStatusDto, RegistrationStatusCode.FAILED, 
				StatusUtil.UNKNOWN_EXCEPTION_OCCURED, RegistrationExceptionTypeCode.EXCEPTION, 
				description, PlatformErrorMessages.PACKET_CLASSIFICATION_FAILED, e);
		} finally {
			if (object.getInternalError()) {
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setRetryCount(retryCount);
				updateErrorFlags(registrationStatusDto, object);
			}
			registrationStatusDto.setUpdatedBy(USER);
			/** Module-Id can be Both Success/Error code */
			String moduleId = description.getCode();
			String moduleName = ModuleName.PACKET_CLASSIFIER.toString();
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
		registrationStatusDto.setStatusComment(trimExpMessage
				.trimExceptionMessage(statusUtil.getMessage() + e.getMessage()));
		registrationStatusDto.setSubStatusCode(statusUtil.getCode());
		registrationStatusDto.setLatestTransactionStatusCode(
				registrationStatusMapperUtil.getStatusCode(registrationExceptionTypeCode));
		description.setMessage(platformErrorMessages.getMessage());
		description.setCode(platformErrorMessages.getCode());
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), 
			LoggerFileConstant.REGISTRATIONID.toString(),
			description.getCode() + " -- " + registrationStatusDto.getRegistrationId(),
			platformErrorMessages.getMessage() + e.getMessage() + ExceptionUtils.getStackTrace(e));
	}

	private void updateAudit(LogDescription description, boolean isTransactionSuccessful, String moduleId,
			String moduleName, String registrationId) {
		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString()
				: EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString()
				: EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString()
				: EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, 
			eventType, moduleId, moduleName, registrationId);
	}

	private void generateAndAddTags(String workflowInstanceId, String registrationId, String process, int iteration)
			throws IOException, BaseCheckedException, NumberFormatException, JSONException {
		regProcLogger.debug("generateAndAddTags called for registration id {} {}", registrationId, 
			requiredIdObjectFieldNames);
		Map<String, String> identityFieldValueMap = priorityBasedPacketManagerService.getFields(registrationId,
			requiredIdObjectFieldNames, process, ProviderStageName.CLASSIFICATION);
		Map<String, String> fieldTypeMap = getFieldTypeMap(identityFieldValueMap.get(idSchemaVersionLabel));
		Map<String, FieldDTO> idObjectFieldDTOMap = 
			getIdObjectFieldDTOMap(identityFieldValueMap, fieldTypeMap);
		Map<String, String> metaInfoMap = priorityBasedPacketManagerService.getMetaInfo(registrationId, process, ProviderStageName.CLASSIFICATION);
		Map<String, String> allTags = new HashMap<String, String>();
		for(TagGenerator tagGenerator : tagGenerators) {
			Map<String, String> tags = tagGenerator.generateTags(workflowInstanceId, registrationId, process, 
				idObjectFieldDTOMap, metaInfoMap, iteration);
			if(tags != null && !tags.isEmpty())
				allTags.putAll(tags);
		}
		handleNullValueTags(allTags);
		regProcLogger.debug("generated tags {}", new JSONObject(allTags).toString());
		if(!allTags.isEmpty())
			packetManagerService.addOrUpdateTags(registrationId, allTags);
	}

	private Map<String, FieldDTO> getIdObjectFieldDTOMap(Map<String, String> identityFieldValueMap,
			Map<String, String> fieldTypeMap ) {
		Map<String, FieldDTO> idObjectFieldMap = new HashMap<>();
		identityFieldValueMap.forEach((key,value) -> {
			idObjectFieldMap.put(key, new FieldDTO(fieldTypeMap.get(key), value));
		});
		return idObjectFieldMap;
	}

	private Map<String,String> getFieldTypeMap(String idSchemaVersion)
			throws NumberFormatException, ApisResourceAccessException, JSONException, IOException {
		
		Map<String,String> fieldTypeMap = idSchemaUtil.getIdSchemaFieldTypes(
				Double.parseDouble(idSchemaVersion));
		regProcLogger.debug("getDefaultFields item {}", new JSONObject(fieldTypeMap).toString());
		return fieldTypeMap;
	}

	private void handleNullValueTags(Map<String, String> tags) {
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			if(entry.getValue() == null)
				entry.setValue(notAvailableTagValue);
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

}
