package io.mosip.registration.processor.stages.demodedupe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.AbisStatusCode;
import io.mosip.registration.processor.core.code.DedupeSourceName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.BiometricRecordValidationException;
import io.mosip.registration.processor.core.exception.DataShareException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDetDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegDemoDedupeListDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.IndividualDemographicDedupe;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.app.constants.DemoDedupeConstants;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class DemodedupeProcessor.
 */
@Service
@Transactional
public class DemodedupeProcessor {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DemodedupeProcessor.class);

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The demo dedupe. */
	@Autowired
	private DemoDedupe demoDedupe;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;



	/** The registration exception mapper util. */
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil = new RegistrationExceptionMapperUtil();

	/** The utility. */
	@Autowired
	Utilities utility;

	/** The registration status dao. */
	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	/** The abis handler util. */
	@Autowired
	private ABISHandlerUtil abisHandlerUtil;

	@Autowired
	private Environment env;

	@Autowired
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	/** The is match found. */
	private volatile boolean isMatchFound = false;

	private static final String DEMODEDUPEENABLE = "mosip.registration.processor.demographic.deduplication.enable";

	private static final String TRUE = "true";

	private static final String GLOBAL_CONFIG_TRUE_VALUE = "Y";

	/** The age limit. */
	@Value("${mosip.kernel.applicant.type.age.limit}")
	private String ageLimit;

	@Value("${registration.processor.infant.dedupe}")
	private String infantDedupe;

	@Value("${mosip.regproc.demo.dedupe.invalid-biometrics-action}")
	private String demodedupeInvalidBiometricAction;

	@Value("${mosip.regproc.demo.dedupe.infant.invalid-biometrics-action}")
	private String demodedupeInfantInvalidBiometricAction;

	/**
	 * Process.
	 *
	 * @param object
	 *            the object
	 * @param stageName
	 *            the stage name
	 * @return the message DTO
	 */
	public MessageDTO process(MessageDTO object, String stageName) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

		String registrationId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "DemoDedupeStage::DemoDedupeProcessor::entry");
		LogDescription description = new LogDescription();
		object.setMessageBusAddress(MessageBusAddress.DEMO_DEDUPE_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);
		isMatchFound = false;

		/** The duplicate dtos. */
		List<DemographicInfoDto> duplicateDtos = new ArrayList<>();

		boolean isTransactionSuccessful = false;
		boolean isDemoDedupeSkip = true;
		String moduleName = ModuleName.DEMO_DEDUPE.toString();
		String moduleId = PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode();
		boolean isDuplicateRequestForSameTransactionId = false;
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(registrationId);

		try {
			// Persist Demographic packet Data if packet Registration type is NEW
			if (registrationStatusDto.getRegistrationType().equals(RegistrationType.NEW.name())) {

				String packetStatus = abisHandlerUtil.getPacketStatus(registrationStatusDto);

				if (packetStatus.equalsIgnoreCase(AbisConstant.PRE_ABIS_IDENTIFICATION)) {

					packetInfoManager.saveDemographicInfoJson(registrationId,
							registrationStatusDto.getRegistrationType(), moduleId, moduleName);
					int age = utility.getApplicantAge(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.DEMO_DEDUPE);
					int ageThreshold = Integer.parseInt(ageLimit);
					if (age < ageThreshold) {
						if (infantDedupe.equalsIgnoreCase(GLOBAL_CONFIG_TRUE_VALUE)) {
							isDemoDedupeSkip = false;
							duplicateDtos = performDemoDedupe(registrationStatusDto, object, description,
									trimExceptionMessage, true);
							if (duplicateDtos.isEmpty())
								isTransactionSuccessful = true;
						}
					}
					else {
						if (env.getProperty(DEMODEDUPEENABLE).trim().equalsIgnoreCase(TRUE)) {
							isDemoDedupeSkip = false;
							duplicateDtos = performDemoDedupe(registrationStatusDto, object, description,
									trimExceptionMessage, false);
						if (duplicateDtos.isEmpty())
							isTransactionSuccessful = true;
						}
					}

					if (isDemoDedupeSkip) {
						object.setIsValid(Boolean.TRUE);
						registrationStatusDto
								.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
						registrationStatusDto.setStatusComment(StatusUtil.DEMO_DEDUPE_SKIPPED.getMessage());
						registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
						registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_SKIPPED.getCode());
						description.setCode(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP_SKIP.getCode());
						description.setMessage(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP_SKIP.getMessage() + " -- "
								+ registrationId);
						regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), description.getCode(),
								registrationId, description.getMessage());
						registrationStatusDto.setUpdatedBy(DemoDedupeConstants.USER);
						regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
								DemoDedupeConstants.DEMO_SKIP);
					}
				} else if (packetStatus.equalsIgnoreCase(AbisConstant.POST_ABIS_IDENTIFICATION)) {
					isTransactionSuccessful = processDemoDedupeRequesthandler(registrationStatusDto, object,
							description);
				}

			} else if (registrationStatusDto.getRegistrationType().equals(RegistrationType.UPDATE.name())
					|| registrationStatusDto.getRegistrationType().equals(RegistrationType.RES_UPDATE.name())) {
				IndividualDemographicDedupe demoDedupeData = new IndividualDemographicDedupe();


				IndividualDemographicDedupe demographicData = packetInfoManager
						.getIdentityKeysAndFetchValuesFromJSON(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.DEMO_DEDUPE);
				JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
				String uinFieldCheck = utility.getUIn(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.DEMO_DEDUPE);
				JSONObject jsonObject = utility.retrieveIdrepoJson(uinFieldCheck);
				if (jsonObject == null) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
					throw new IdRepoAppException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
				}
				List<JsonValue[]> jsonValueList = new ArrayList<>();
				if (demographicData.getName() == null || demographicData.getName().isEmpty()) {
					String names = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.NAME), MappingJsonConstants.VALUE);
					Arrays.stream(names.split(","))
							.forEach(name -> {
								JsonValue[] nameArray = JsonUtil.getJsonValues(jsonObject, name);
								if (nameArray != null)
									jsonValueList.add(nameArray);
							});
				}
				if(demographicData.getName() == null || demographicData.getName().isEmpty())
					demoDedupeData.setName(jsonValueList.isEmpty() ? null : jsonValueList);
				else
					demoDedupeData.setName(demographicData.getName());
				demoDedupeData.setDateOfBirth(demographicData.getDateOfBirth() == null
						? JsonUtil.getJSONValue(jsonObject, JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.DOB), MappingJsonConstants.VALUE))
						: demographicData.getDateOfBirth());
				demoDedupeData.setGender(demographicData.getGender() == null
						? JsonUtil.getJsonValues(jsonObject,
								JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.GENDER), MappingJsonConstants.VALUE))
						: demographicData.getGender());
				demoDedupeData.setPhone(demographicData.getPhone() == null
						? JsonUtil
								.getJSONValue(jsonObject,
										JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson,
												MappingJsonConstants.PHONE), MappingJsonConstants.VALUE))
						: demographicData.getPhone());
				demoDedupeData.setEmail(demographicData.getEmail() == null
						? JsonUtil
								.getJSONValue(jsonObject,
										JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson,
												MappingJsonConstants.EMAIL), MappingJsonConstants.VALUE))
						: demographicData.getEmail());

				packetInfoManager.saveIndividualDemographicDedupeUpdatePacket(demoDedupeData, registrationId, moduleId,
						moduleName);
				object.setIsValid(Boolean.TRUE);
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				registrationStatusDto.setStatusComment(StatusUtil.DEMO_DEDUPE_SKIPPED.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_SKIPPED.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

			}
			if (abisHandlerUtil.getPacketStatus(registrationStatusDto).equalsIgnoreCase(AbisConstant.DUPLICATE_FOR_SAME_TRANSACTION_ID))
				isDuplicateRequestForSameTransactionId = true;
			
			registrationStatusDto.setRegistrationStageName(stageName);

		}catch (PacketManagerException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage
							.trimExceptionMessage(
									StatusUtil.DEMO_DEDUPE_PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description + "\n" + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} 
	     catch (FSAdapterException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.OBJECT_STORE_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.OBJECT_STORE_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.OBJECT_STORE_EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_DEMO_PACKET_STORE_NOT_ACCESSIBLE.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_DEMO_PACKET_STORE_NOT_ACCESSIBLE.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (IllegalArgumentException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.IIEGAL_ARGUMENT_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IIEGAL_ARGUMENT_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.ILLEGAL_ARGUMENT_EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_DEMO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_DEMO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			description.setCode(PlatformErrorMessages.RPR_DEMO_API_RESOUCE_ACCESS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_DEMO_API_RESOUCE_ACCESS_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description + "\n" + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (Exception ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_DEMO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_DEMO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} finally {
		    if (!isDuplicateRequestForSameTransactionId) {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.DEMOGRAPHIC_VERIFICATION.toString());
			moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode()
					: description.getCode();
			registrationStatusDto.setUpdatedBy(DemoDedupeConstants.USER);
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			try {
				if (isMatchFound) {
					saveDuplicateDtoList(duplicateDtos, registrationStatusDto, object);
				}
			} catch (Exception e) {
				registrationStatusDto.setRegistrationStageName(stageName);
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				registrationStatusDto.setStatusComment(trimExceptionMessage
						.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
				registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
				description.setMessage(DemoDedupeConstants.NO_DATA_IN_DEMO);
				registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

				regProcLogger.error(DemoDedupeConstants.NO_DATA_IN_DEMO, "", "", ExceptionUtils.getStackTrace(e));
				object.setIsValid(Boolean.FALSE);
				object.setMessageBusAddress(MessageBusAddress.DEMO_DEDUPE_BUS_IN);
				object.setInternalError(Boolean.TRUE);
			}
			if (object.getIsValid())
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "DemoDedupeProcessor::success");
			else
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "DemoDedupeProcessor::failure");

			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
			}
			else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Duplicate request received for same latest transaction id. This will be ignored.");
				object.setIsValid(false);
				object.setInternalError(true);
			}
		}

		return object;
	}


	/**
	 * Perform demo dedupe.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param object                the object
	 * @param description
	 * @return true, if successful
	 * @throws IOException
	 * @throws DataShareException
	 * @throws JsonProcessingException
	 * @throws PacketManagerException
	 * @throws ApisResourceAccessException
	 */
	private List<DemographicInfoDto> performDemoDedupe(InternalRegistrationStatusDto registrationStatusDto,
			MessageDTO object, LogDescription description, TrimExceptionMessage trimExceptionMessage, boolean isInfant)
			throws ApisResourceAccessException, PacketManagerException,
			JsonProcessingException, DataShareException, IOException {
		String registrationId = registrationStatusDto.getRegistrationId();
		// Potential Duplicate Ids after performing demo dedupe
		List<DemographicInfoDto> duplicateDtos = demoDedupe.performDedupe(registrationStatusDto.getRegistrationId());
		List<String> matchedRidsWithoutRejected = new ArrayList<>();
		List<String> matchedRegIds;
		if (!duplicateDtos.isEmpty()) {
			duplicateDtos.removeIf(duplicateDto -> duplicateDto.getRegId().equals(registrationId));
			matchedRegIds = duplicateDtos.stream().map(DemographicInfoDto::getRegId)
					.collect(Collectors.toList());
			matchedRidsWithoutRejected = abisHandlerUtil.removeRejectedIds(matchedRegIds);
			for (DemographicInfoDto demographicInfoDto : new ArrayList<DemographicInfoDto>(duplicateDtos)) {
				if (!matchedRidsWithoutRejected.contains(demographicInfoDto.getRegId())) {
					duplicateDtos.remove(demographicInfoDto);
				}
			}
		}
		if (!duplicateDtos.isEmpty()) {
			try {
				biometricValidation(registrationStatusDto);
				isMatchFound = true;
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.POTENTIAL_MATCH_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.POTENTIAL_MATCH_FOUND.getCode());
			object.setMessageBusAddress(MessageBusAddress.ABIS_HANDLER_BUS_IN);
			description.setCode(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode());
			description.setMessage(DemoDedupeConstants.RECORD_INSERTED_FROM_ABIS_HANDLER + " -- " + registrationId);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), DemoDedupeConstants.RECORD_INSERTED_FROM_ABIS_HANDLER);
		} catch (BiometricRecordValidationException e) {
			isMatchFound = true;
			handleInvalidBiometricAction(registrationStatusDto, object, description, trimExceptionMessage,
					registrationId, matchedRidsWithoutRejected, e, isInfant);

		}

		} else {
			object.setIsValid(Boolean.TRUE);
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			registrationStatusDto.setStatusComment(StatusUtil.DEMO_DEDUPE_SUCCESS.getMessage());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_SUCCESS.getCode());
			description.setCode(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode());
			description.setMessage(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getMessage() + " -- " + registrationId);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description.getMessage());
			registrationStatusDto.setUpdatedBy(DemoDedupeConstants.USER);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), DemoDedupeConstants.DEMO_SUCCESS);
		}
		return duplicateDtos;
	}


	private void handleInvalidBiometricAction(InternalRegistrationStatusDto registrationStatusDto, MessageDTO object,
			LogDescription description, TrimExceptionMessage trimExceptionMessage, String registrationId,
			List<String> matchedRidsWithoutRejected, BiometricRecordValidationException e, boolean isInfant)
			throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
		if ((!isInfant
				&& demodedupeInvalidBiometricAction.equalsIgnoreCase(DemoDedupeConstants.MARK_AS_DEMODEDUPE_SUCCESS))
				|| (isInfant && demodedupeInfantInvalidBiometricAction
						.equalsIgnoreCase(DemoDedupeConstants.MARK_AS_DEMODEDUPE_SUCCESS))) {
			object.setIsValid(Boolean.TRUE);
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			registrationStatusDto.setStatusComment(
					StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED_AND_PACKET_SUCCESS.getMessage());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setSubStatusCode(
					StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED_AND_PACKET_SUCCESS.getCode());
			description
					.setCode(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP_POTENTIAL_DUPLICATION_SUCCESS.getCode());
			description
					.setMessage(
							PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP_POTENTIAL_DUPLICATION_SUCCESS.getMessage()
									+ " -- " + registrationId);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
					DemoDedupeConstants.DEMO_SUCCESS);
		} else if ((!isInfant && demodedupeInvalidBiometricAction
				.equalsIgnoreCase(DemoDedupeConstants.REDIRECT_TO_MANUAL_VERIFICATION))
				|| (isInfant && demodedupeInfantInvalidBiometricAction
						.equalsIgnoreCase(DemoDedupeConstants.REDIRECT_TO_MANUAL_VERIFICATION))) {
			handleRedirectToManualVerification(registrationStatusDto, object, description, trimExceptionMessage,
					matchedRidsWithoutRejected, e);
		} else if ((!isInfant && demodedupeInvalidBiometricAction
				.equalsIgnoreCase(DemoDedupeConstants.MARK_AS_DEMODEDUPE_REJECTED))
				|| (isInfant && demodedupeInfantInvalidBiometricAction
						.equalsIgnoreCase(DemoDedupeConstants.MARK_AS_DEMODEDUPE_REJECTED))) {
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
			registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED_AND_PACKET_REJECTED.getMessage()
							+ " -> " + e.getErrorText()));
			registrationStatusDto
					.setSubStatusCode(StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED_AND_PACKET_REJECTED
							.getCode());
			description.setCode(PlatformErrorMessages.RPR_DEMO_POTENTIAL_PACKET_REJECTED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_DEMO_POTENTIAL_PACKET_REJECTED.getMessage());
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
					DemoDedupeConstants.BIOMETRIC_VALIDATION_FAILED_PACKET_REJECTED);
		} else {
			handleRedirectToManualVerification(registrationStatusDto, object, description, trimExceptionMessage,
					matchedRidsWithoutRejected, e);
		}
	}


	private void handleRedirectToManualVerification(InternalRegistrationStatusDto registrationStatusDto,
			MessageDTO object, LogDescription description, TrimExceptionMessage trimExceptionMessage,
			List<String> matchedRidsWithoutRejected, BiometricRecordValidationException e)
			throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
		registrationStatusDto
				.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
		registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
				StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED.getMessage() + " -> "
						+ e.getErrorText()));
		registrationStatusDto
				.setSubStatusCode(StatusUtil.DEMO_DEDUPE_BIOMTERIC_RECORD_VALIDAITON_FAILED.getCode());
		description.setCode(PlatformErrorMessages.RPR_DEMO_POTENTIAL_SENDING_FOR_MANUAL.getCode());
		description.setMessage(PlatformErrorMessages.RPR_DEMO_POTENTIAL_SENDING_FOR_MANUAL.getMessage());
		object.setMessageBusAddress(MessageBusAddress.MANUAL_VERIFICATION_BUS_IN);
		saveManualAdjudicationData(registrationStatusDto, matchedRidsWithoutRejected);
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
				DemoDedupeConstants.BIOMETRIC_VALIDATION_FAILED_SENDING_FOR_MANUAL);
	}

	/**
	 * Gets the latest transaction id.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the latest transaction id
	 */
	private String getLatestTransactionId(String registrationId) {
		RegistrationStatusEntity entity = registrationStatusDao.findById(registrationId);
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;
	}

	/**
	 * Process demo dedupe requesthandler.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param object                the object
	 * @param description
	 * @return true, if successful
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	private boolean processDemoDedupeRequesthandler(InternalRegistrationStatusDto registrationStatusDto,
			MessageDTO object, LogDescription description) throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException, JsonProcessingException, PacketManagerException {
		boolean isTransactionSuccessful = false;
		List<String> responsIds = new ArrayList<>();

		String latestTransactionId = getLatestTransactionId(registrationStatusDto.getRegistrationId());

		List<AbisResponseDto> abisResponseDto = packetInfoManager.getAbisResponseRecords(latestTransactionId,
				DemoDedupeConstants.IDENTIFY);

		for (AbisResponseDto responseDto : abisResponseDto) {
			if (responseDto.getStatusCode().equalsIgnoreCase(AbisStatusCode.SUCCESS.toString())) {
				responsIds.add(responseDto.getId());
			} else {
				isTransactionSuccessful = true;
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				description.setMessage(
						DemoDedupeConstants.SENDING_TO_REPROCESS + " -- " + registrationStatusDto.getRegistrationId());
				registrationStatusDto.setRetryCount(retryCount);

				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.DEMO_DEDUPE_ABIS_RESPONSE_ERROR));
				registrationStatusDto.setStatusComment(StatusUtil.DEMO_DEDUPE_FAILED_IN_ABIS.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_FAILED_IN_ABIS.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						DemoDedupeConstants.SENDING_TO_REPROCESS);
			}
		}

		if (!responsIds.isEmpty()) {
			List<AbisResponseDetDto> abisResponseDetDto = packetInfoManager.getAbisResponseDetRecordsList(responsIds);
			List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationStatusDto.getRegistrationId(),
					registrationStatusDto.getRegistrationType(), ProviderStageName.DEMO_DEDUPE);
			if (abisResponseDetDto.isEmpty() || uniqueRids.isEmpty()) {
				object.setIsValid(Boolean.TRUE);
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				registrationStatusDto.setStatusComment(StatusUtil.DEMO_DEDUPE_SUCCESS.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.DEMO_DEDUPE_SUCCESS.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						DemoDedupeConstants.NO_DUPLICATES_FOUND);
				isTransactionSuccessful = true;
				description.setCode(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode());
				description.setMessage(PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getMessage());
			} else {
				object.setIsValid(Boolean.FALSE);
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto.setStatusComment(StatusUtil.POTENTIAL_MATCH_FOUND_IN_ABIS.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.POTENTIAL_MATCH_FOUND_IN_ABIS.getCode());
				description.setCode(PlatformErrorMessages.RPR_DEMO_SENDING_FOR_MANUAL.getCode());
				description.setMessage(PlatformErrorMessages.RPR_DEMO_SENDING_FOR_MANUAL.getMessage());
				object.setMessageBusAddress(MessageBusAddress.MANUAL_VERIFICATION_BUS_IN);
				saveManualAdjudicationData(registrationStatusDto, uniqueRids);
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						DemoDedupeConstants.SENDING_FOR_MANUAL);

			}
		}

		return isTransactionSuccessful;
	}

	/**
	 * Save duplicate dto list.
	 *
	 * @param duplicateDtos
	 *            the duplicate dtos
	 * @param registrationStatusDto
	 *            the registration status dto
	 * @return true, if successful
	 */
	private boolean saveDuplicateDtoList(List<DemographicInfoDto> duplicateDtos,
			InternalRegistrationStatusDto registrationStatusDto, MessageDTO object) {
		boolean isDataSaved = false;
		int numberOfProcessedPackets = 0;

		String moduleId = PlatformSuccessMessages.RPR_PKR_DEMO_DE_DUP.getCode();

		String moduleName = ModuleName.DEMO_DEDUPE.toString();
		for (DemographicInfoDto demographicInfoDto : duplicateDtos) {
			InternalRegistrationStatusDto potentialMatchRegistrationDto = registrationStatusService
					.getRegistrationStatus(demographicInfoDto.getRegId());
			if (potentialMatchRegistrationDto.getLatestTransactionStatusCode()
					.equalsIgnoreCase(RegistrationTransactionStatusCode.REPROCESS.toString())
					|| potentialMatchRegistrationDto.getLatestTransactionStatusCode()
							.equalsIgnoreCase(AbisConstant.RE_REGISTER)) {
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						DemoDedupeConstants.REJECTED_OR_REREGISTER);
			} else if (potentialMatchRegistrationDto.getLatestTransactionStatusCode()
					.equalsIgnoreCase(RegistrationTransactionStatusCode.IN_PROGRESS.toString())
					|| potentialMatchRegistrationDto.getLatestTransactionStatusCode()
							.equalsIgnoreCase(RegistrationTransactionStatusCode.PROCESSED.toString())) {
				String latestTransactionId = getLatestTransactionId(registrationStatusDto.getRegistrationId());
				RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
				regDemoDedupeListDto.setRegId(registrationStatusDto.getRegistrationId());
				regDemoDedupeListDto.setMatchedRegId(demographicInfoDto.getRegId());
				regDemoDedupeListDto.setRegtrnId(latestTransactionId);
				regDemoDedupeListDto.setIsDeleted(Boolean.FALSE);
				regDemoDedupeListDto.setCrBy(DemoDedupeConstants.CREATED_BY);
				packetInfoManager.saveDemoDedupePotentialData(regDemoDedupeListDto, moduleId, moduleName);
				isDataSaved = true;
				numberOfProcessedPackets++;
			} else {
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						"The packet status is something different");
			}
			if (numberOfProcessedPackets == 0) {
				object.setIsValid(Boolean.TRUE);
			}
		}
		return isDataSaved;
	}

	/**
	 * Save manual adjudication data.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	private void saveManualAdjudicationData(InternalRegistrationStatusDto registrationStatusDto,
			List<String> matchedRegIds)
			throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
			String moduleId = PlatformErrorMessages.RPR_DEMO_SENDING_FOR_MANUAL.getCode();
			String moduleName = ModuleName.DEMO_DEDUPE.toString();
			packetInfoManager.saveManualAdjudicationData(matchedRegIds, registrationStatusDto.getRegistrationId(),
					DedupeSourceName.DEMO, moduleId, moduleName,null,null);
	}

	private void biometricValidation(InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException,
			BiometricRecordValidationException, DataShareException {
		Map<String, List<String>> typeAndSubtypMap = abisHandlerUtil.createBiometricTypeSubtypeMappingFromAbispolicy();
		List<String> segments = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : typeAndSubtypMap.entrySet()) {
			if (entry.getValue() == null) {
				segments.add(entry.getKey());
			} else {
				segments.addAll(entry.getValue());
			}
		}
		JSONObject regProcessorIdentityJson = utility
				.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);
		BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(
				registrationStatusDto.getRegistrationId(), individualBiometricsLabel, segments,
				registrationStatusDto.getRegistrationType(), ProviderStageName.DEMO_DEDUPE);
		abisHandlerUtil.validateBiometricRecord(biometricRecord, segments);

	}
}