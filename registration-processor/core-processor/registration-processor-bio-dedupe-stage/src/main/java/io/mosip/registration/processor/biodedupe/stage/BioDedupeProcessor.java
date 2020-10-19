package io.mosip.registration.processor.biodedupe.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.biodedupe.constants.BioDedupeConstants;
import io.mosip.registration.processor.biodedupe.stage.exception.CbeffNotFoundException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
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
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class BioDedupeProcessor.
 *
 * @author Nagalakshmi
 * @author Sowmya
 * @author Horteppa
 */

@Service
@Transactional
public class BioDedupeProcessor {

	/** The utilities. */
	@Autowired
	Utilities utilities;

	@Autowired
	private IdRepoService idRepoService;

	@Autowired
	private PacketManagerService packetManagerService;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The registration exception mapper util. */
	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	/** The abis handler util. */
	@Autowired
	private ABISHandlerUtil abisHandlerUtil;

	@Autowired
	private BioDedupeService biodedupeServiceImpl;

	@Autowired
	private Environment env;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;

	/** The age limit. */
	@Value("${mosip.kernel.applicant.type.age.limit}")
	private String ageLimit;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BioDedupeProcessor.class);



	private static final String VALIDATIONFALSE = "false";

	@Value("${registration.processor.infant.dedupe}")
	private String infantDedupe;

	public static final String GLOBAL_CONFIG_TRUE_VALUE = "Y";




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
				registrationId, "BioDedupeProcessor::process::entry");
		LogDescription description = new LogDescription();
		object.setMessageBusAddress(MessageBusAddress.BIO_DEDUPE_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);

		boolean isTransactionSuccessful = false;

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		String source = utilities.getDefaultSource();
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(registrationId);
			String registrationType = registrationStatusDto.getRegistrationType();
			if (registrationType.equalsIgnoreCase(SyncTypeDto.NEW.toString())) {
				String packetStatus = abisHandlerUtil.getPacketStatus(registrationStatusDto);
				if (packetStatus.equalsIgnoreCase(AbisConstant.PRE_ABIS_IDENTIFICATION)) {
					newPacketPreAbisIdentification(registrationStatusDto, source, object);
				} else if (packetStatus.equalsIgnoreCase(AbisConstant.POST_ABIS_IDENTIFICATION)) {
					postAbisIdentification(registrationStatusDto, source, object, registrationType);

				}

			} else if (registrationType.equalsIgnoreCase(SyncTypeDto.UPDATE.toString())
					|| registrationType.equalsIgnoreCase(SyncTypeDto.RES_UPDATE.toString())) {
				String packetStatus = abisHandlerUtil.getPacketStatus(registrationStatusDto);
				if (packetStatus.equalsIgnoreCase(AbisConstant.PRE_ABIS_IDENTIFICATION)) {
					updatePacketPreAbisIdentification(registrationStatusDto, source, object);
				} else if (packetStatus.equalsIgnoreCase(AbisConstant.POST_ABIS_IDENTIFICATION)) {
					postAbisIdentification(registrationStatusDto, source, object, registrationType);
				}

			} else if (registrationType.equalsIgnoreCase(SyncTypeDto.LOST.toString())
					&& isValidCbeff(registrationId, source, registrationType)) {
				String packetStatus = abisHandlerUtil.getPacketStatus(registrationStatusDto);

				if (packetStatus.equalsIgnoreCase(AbisConstant.PRE_ABIS_IDENTIFICATION)) {
					lostPacketPreAbisIdentification(registrationStatusDto, object);
				} else if (packetStatus.equalsIgnoreCase(AbisConstant.POST_ABIS_IDENTIFICATION)) {
					List<String> matchedRegIds = abisHandlerUtil
							.getUniqueRegIds(registrationStatusDto.getRegistrationId(), source, registrationType);
					lostPacketPostAbisIdentification(registrationStatusDto, source, object, matchedRegIds);
				}

			}

			registrationStatusDto.setRegistrationStageName(stageName);
			isTransactionSuccessful = true;

		} catch (DataAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			description.setCode(PlatformErrorMessages.RPR_BIO_API_RESOUCE_ACCESS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_BIO_API_RESOUCE_ACCESS_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description + "\n" + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (CbeffNotFoundException ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(StatusUtil.CBEF_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.CBEF_NOT_FOUND.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.CBEFF_NOT_PRESENT_EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description.getMessage() + "\n" + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (IdentityNotFoundException | IOException ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description.getMessage() + "\n" + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} catch (Exception ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			description.setCode(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.PACKET_BIO_DEDUPE_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
					description.getCode() + " -- " + LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					description.getMessage() + "\n" + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} finally {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.BIOGRAPHIC_VERIFICATION.toString());
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_BIO_DEDUPE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.BIO_DEDUPE.name();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "BioDedupeProcessor::" + registrationStatusDto.getLatestTransactionStatusCode());

			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}
		return object;
	}

	/**
	 * New packet pre abis identification.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param object                the object
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	private void newPacketPreAbisIdentification(InternalRegistrationStatusDto registrationStatusDto, String source, MessageDTO object)
			throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
		if (isValidCbeff(registrationStatusDto.getRegistrationId(), source, registrationStatusDto.getRegistrationType())) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_INPROGRESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_INPROGRESS.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			object.setMessageBusAddress(MessageBusAddress.ABIS_HANDLER_BUS_IN);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.CBEFF_PRESENT_IN_PACKET);
		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_SUCCESS.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			object.setIsValid(Boolean.TRUE);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.CBEFF_ABSENT_IN_PACKET);
		}
	}

	/**
	 * Update packet pre abis identification.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param object                the object
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws ApisResourceAccessException
	 * @throws PacketDecryptionFailureException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	private void updatePacketPreAbisIdentification(InternalRegistrationStatusDto registrationStatusDto, String source,
			MessageDTO object) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

		JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson();
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		String bioField = packetManagerService.getField(registrationStatusDto.getRegistrationId(), individualBiometricsLabel, source, registrationStatusDto.getRegistrationType());


		if (StringUtils.isNotEmpty(bioField)) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_INPROGRESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_INPROGRESS.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			object.setMessageBusAddress(MessageBusAddress.ABIS_HANDLER_BUS_IN);

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.UPDATE_PACKET_BIOMETRIC_NOT_NULL);
		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_SUCCESS.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			object.setIsValid(Boolean.TRUE);

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.UPDATE_PACKET_BIOMETRIC_NULL);
		}
	}

	/**
	 * Post abis identification.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param object                the object
	 * @param registrationType      the registration type
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	private void postAbisIdentification(InternalRegistrationStatusDto registrationStatusDto, String source, MessageDTO object,
			String registrationType) throws ApisResourceAccessException, IOException,
			io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {
		String moduleId = "";
		String moduleName = ModuleName.BIO_DEDUPE.toString();
		List<String> matchedRegIds = abisHandlerUtil.getUniqueRegIds(registrationStatusDto.getRegistrationId(), source,
				registrationType);
		// TODO : temporary fix. Need to analyze more.
		if (matchedRegIds != null && !matchedRegIds.isEmpty()
				&& matchedRegIds.contains(registrationStatusDto.getRegistrationId())) {
			matchedRegIds.remove(registrationStatusDto.getRegistrationId());
		}
		if (matchedRegIds == null || matchedRegIds.isEmpty()) {
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			object.setIsValid(Boolean.TRUE);
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_SUCCESS.getCode());
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.ABIS_RESPONSE_NULL);

		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_POTENTIAL_MATCH.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_POTENTIAL_MATCH.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
			moduleId = PlatformSuccessMessages.RPR_BIO_METRIC_POTENTIAL_MATCH.getCode();
			packetInfoManager.saveManualAdjudicationData(matchedRegIds, registrationStatusDto.getRegistrationId(),
					DedupeSourceName.BIO, moduleId, moduleName);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), BioDedupeConstants.ABIS_RESPONSE_NOT_NULL);

		}
	}

	/**
	 * Checks if is valid cbeff.
	 *
	 * @param registrationId the registration id
	 * @return the boolean
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	private Boolean isValidCbeff(String registrationId, String source, String registrationType) throws ApisResourceAccessException,
			IOException, JsonProcessingException, PacketManagerException {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "BioDedupeProcessor::isValidCbeff()::get BIODEDUPE service call started");
		byte[] bytefile = null;
		boolean isInfant = infantCheck(registrationId, source, registrationType);
		if (isInfant) {
			if (infantDedupe.equalsIgnoreCase(GLOBAL_CONFIG_TRUE_VALUE)) {
				bytefile = biodedupeServiceImpl.getFileByRegId(registrationId, source, registrationType);
			} else
				return false;
		} else {
			 bytefile = biodedupeServiceImpl.getFileByRegId(registrationId, source, registrationType);

		}
		if (bytefile != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					"BioDedupeProcessor::isValidCbeff()::get BIODEDUPE service call ended and Fetched ByteFile");
			return true;
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "BioDedupeProcessor::isValidCbeff()::get BIODEDUPE service call ended successfully"
							+ BioDedupeConstants.CBEFF_NOT_FOUND);
			throw new CbeffNotFoundException(PlatformErrorMessages.PACKET_BIO_DEDUPE_CBEFF_NOT_PRESENT.getMessage());
		}

	}

	private boolean infantCheck(String registrationId, String source, String registrationType) throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
		boolean isInfant = false;
		if (RegistrationType.NEW.name().equalsIgnoreCase(registrationType)) {
			int age = utilities.getApplicantAge(registrationId, source, registrationType);
			int ageThreshold = Integer.parseInt(ageLimit);
			isInfant = age < ageThreshold;
		} else {
			isInfant = false;
		}
		return isInfant;
	}

	private void lostPacketPreAbisIdentification(InternalRegistrationStatusDto registrationStatusDto,
			MessageDTO object) {

		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
		registrationStatusDto.setStatusComment(StatusUtil.BIO_DEDUPE_INPROGRESS.getMessage());
		registrationStatusDto.setSubStatusCode(StatusUtil.BIO_DEDUPE_INPROGRESS.getCode());
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
		object.setMessageBusAddress(MessageBusAddress.ABIS_HANDLER_BUS_IN);

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(), BioDedupeConstants.LOST_PRE_ABIS_IDENTITIFICATION);

	}

	private void lostPacketPostAbisIdentification(InternalRegistrationStatusDto registrationStatusDto, String source,
			MessageDTO object, List<String> matchedRegIds) throws IOException, ApisResourceAccessException, JsonProcessingException, PacketManagerException {
		String moduleId = "";
		String moduleName = ModuleName.BIO_DEDUPE.toString();
		String registrationId = registrationStatusDto.getRegistrationId();

		if (matchedRegIds.isEmpty()) {
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
			object.setIsValid(Boolean.FALSE);
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(StatusUtil.LOST_PACKET_BIOMETRICS_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.LOST_PACKET_BIOMETRICS_NOT_FOUND.getCode());
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					BioDedupeConstants.NO_MATCH_FOUND_FOR_LOST + registrationId);

		} else if (matchedRegIds.size() == 1) {

			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
			object.setIsValid(Boolean.TRUE);
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(StatusUtil.LOST_PACKET_UNIQUE_MATCH_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.LOST_PACKET_UNIQUE_MATCH_FOUND.getCode());
			moduleId = PlatformSuccessMessages.RPR_BIO_LOST_PACKET_UNIQUE_MATCH_FOUND.getCode();
			packetInfoManager.saveRegLostUinDet(registrationId, matchedRegIds.get(0), moduleId, moduleName);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					BioDedupeConstants.FOUND_UIN_IN_BIO_CHECK + registrationId);

		} else {

			List<String> demoMatchedIds = new ArrayList<>();
			int matchCount = 0;

			for (String matchedRegId : matchedRegIds) {
				JSONObject matchedDemographicIdentity = idRepoService.getIdJsonFromIDRepo(matchedRegId,
						utilities.getGetRegProcessorDemographicIdentity());
				matchCount = addMactchedRefId(registrationStatusDto.getRegistrationId(), source,
						registrationStatusDto.getRegistrationType(), matchedDemographicIdentity, matchCount, demoMatchedIds, matchedRegId);
				if (matchCount > 1)
					break;
			}

			if (matchCount == 1) {

				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				object.setIsValid(Boolean.TRUE);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
				registrationStatusDto.setStatusComment(StatusUtil.LOST_PACKET_UNIQUE_MATCH_FOUND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.LOST_PACKET_UNIQUE_MATCH_FOUND.getCode());
				moduleId = PlatformSuccessMessages.RPR_BIO_LOST_PACKET_UNIQUE_MATCH_FOUND.getCode();
				packetInfoManager.saveRegLostUinDet(registrationId, demoMatchedIds.get(0), moduleId, moduleName);
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						BioDedupeConstants.FOUND_UIN_IN_DEMO_CHECK + registrationId);
			} else {

				registrationStatusDto.setStatusComment(StatusUtil.LOST_PACKET_MULTIPLE_MATCH_FOUND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.LOST_PACKET_MULTIPLE_MATCH_FOUND.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						BioDedupeConstants.MULTIPLE_RID_FOUND);
				moduleId = PlatformErrorMessages.RPR_BIO_LOST_PACKET_MULTIPLE_MATCH_FOUND.getCode();
				packetInfoManager.saveManualAdjudicationData(matchedRegIds,
						registrationStatusDto.getRegistrationId(), DedupeSourceName.BIO, moduleId, moduleName);
			}
		}
	}

	private int addMactchedRefId(String id, String source, String process, JSONObject matchedDemographicIdentity, int matchCount, List<String> demoMatchedIds,
			String matchedRegId) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		if (matchedDemographicIdentity != null) {
			Map<String, String> matchedAttribute = getIdJson(matchedDemographicIdentity);
			if (!matchedAttribute.isEmpty()) {
				if (compareDemoDedupe(id, source, process, matchedAttribute)) {
					matchCount++;
					demoMatchedIds.add(matchedRegId);
				}

			}
		}
		return matchCount;
	}

	private boolean compareDemoDedupe(String id, String source, String process, Map<String, String> matchedAttribute) throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		boolean isMatch = false;

		for (String key : matchedAttribute.keySet()) {
			String value = packetManagerService.getField(id, key, source, process);
			if (value != null && value.equalsIgnoreCase(matchedAttribute.get(key))) {
				isMatch = true;
			} else {
				isMatch = false;
				return isMatch;
			}

		}
		return isMatch;
	}

	private Map<String, String> getIdJson(JSONObject demographicJsonIdentity) throws IOException {
		Map<String, String> attribute = new LinkedHashMap<>();


		JSONObject mapperIdentity = utilities.getRegistrationProcessorMappingJson();
		List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());

		for (String key : mapperJsonKeys) {
			JSONObject jsonValue = JsonUtil.getJSONObject(mapperIdentity, key);
			Object jsonObject = JsonUtil.getJSONValue(demographicJsonIdentity,
					(String) jsonValue.get(BioDedupeConstants.VALUE));
			if (jsonObject instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(demographicJsonIdentity,
						(String) jsonValue.get(BioDedupeConstants.VALUE));
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				if (jsonValues != null)
					for (int count = 0; count < jsonValues.length; count++) {
						String lang = jsonValues[count].getLanguage();
						attribute.put(key + "_" + lang, jsonValues[count].getValue());
					}

			} else if (jsonObject instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(demographicJsonIdentity,
						(String) jsonValue.get(BioDedupeConstants.VALUE));
				if (json != null)
					attribute.put(key, json.get(BioDedupeConstants.VALUE).toString());
			} else {
				if (jsonObject != null)
					attribute.put(key, jsonObject.toString());
			}
		}

		return attribute;
	}
}
