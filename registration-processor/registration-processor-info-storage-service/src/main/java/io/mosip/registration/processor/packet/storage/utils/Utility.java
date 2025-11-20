package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.processor.core.constant.*;
import io.mosip.registration.processor.core.exception.*;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataRequest;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataResponse;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import lombok.Data;
import static io.mosip.kernel.core.util.DateUtils.*;
import static io.mosip.kernel.core.util.DateUtils.parseUTCToLocalDateTime;

/**
 * The Class Utility.
 *
 * @author Sowmya Banakar
 */

@Component
@Data
public class Utility {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(Utility.class);

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	SyncRegistrationRepository<SyncRegistrationEntity, String> syncRegistrationRepository;

	@Autowired
	private IdRepoService idRepoService;

	@Autowired
	private Utilities utilities;

    @Value("${registration.processor.vid-support-for-update:false}")
    private Boolean isVidSupportedForUpdate;

    /** The vid validator. */
    @Autowired
    private VidValidator<String> vidValidator;

	/** The dob format. */
	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

	/** The get reg processor demographic identity. */
	@Value("${registration.processor.demographic.identity}")
	private String getRegProcessorDemographicIdentity;

	/**
	 * Configuration property that defines the age limit used to determine
	 * whether an applicant is considered an infant or non-infant.
	 */
	@Value("${mosip.kernel.applicant.type.age.limit:5}")
	private String ageLimit;

	/**
	 * Buffer (in months) applied to the applicant's age limit to handle edge cases
	 * where applicants are close to the threshold. Default is {@code 0}.
	 *
	 * <p>Increasing this value provides a safety margin for age calculations near
	 * the eligibility boundary.</p>
	 */
	@Value("${registration.processor.applicant.type.age.limit.buffer:0}")
	private Integer ageLimitBuffer;

	/**
	 * Estimated time (in hours) the system takes to process a packet.
	 * <p>
	 * This value helps calculate the approximate packet creation time.
	 * It is used when no other data source is available to determine the
	 * packet creation time, apart from the identity update time.
	 * The system subtracts this duration from the identity update time
	 * to estimate when the packet was likely created, since the identity
	 * update time includes the packet processing duration as well.
	 * The default value is {@code 0}.
	 * </p>
	 */
	@Value("${registration.processor.expected-packet-processing-duration:0}")
	private Integer expectedPacketProcessingDurationHours;


	private static final String VALUE = "value";
	public static final String EXCEPTION = "EXCEPTION";
	public static final String TRUE = "TRUE";

	/**
	 * get applicant age by registration id. Checks the id json if dob or age
	 * present, if yes returns age if both dob or age are not present then retrieves
	 * age from id repo
	 *
	 * @param id the registration id
	 * @return the applicant age
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException           the packet decryption failure
	 *                                               exception
	 * @throws RegistrationProcessorCheckedException
	 */
	public int getApplicantAge(String id, String process, ProviderStageName stageName)
			throws IOException, ApisResourceAccessException, JsonProcessingException, PacketManagerException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"Utility::getApplicantAge()::entry");

		String applicantDob = packetManagerService.getFieldByMappingJsonKey(id, MappingJsonConstants.DOB, process,
				stageName);
		String applicantAge = packetManagerService.getFieldByMappingJsonKey(id, MappingJsonConstants.AGE, process,
				stageName);
		if (applicantDob != null) {
			return calculateAge(applicantDob);
		} else if (applicantAge != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
					"Utility::getApplicantAge()::exit when applicantAge is not null");
			return Integer.valueOf(applicantAge);
		} else {
			String uin = getUIn(id, process, stageName);
			JSONObject identityJSONOject = utilities.retrieveIdrepoJson(uin);
			JSONObject regProcessorIdentityJson = utilities
					.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
			String ageKey = JsonUtil
					.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.AGE), VALUE);
			String dobKey = JsonUtil
					.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.DOB), VALUE);
			String idRepoApplicantDob = JsonUtil.getJSONValue(identityJSONOject, dobKey);
			if (idRepoApplicantDob != null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
						"Utility::getApplicantAge()::exit when ID REPO applicantDob is not null");
				return calculateAge(idRepoApplicantDob);
			}
			Integer idRepoApplicantAge = JsonUtil.getJSONValue(identityJSONOject, ageKey);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
					"Utility::getApplicantAge()::exit when ID REPO applicantAge is not null");
			return idRepoApplicantAge != null ? idRepoApplicantAge : -1;

		}

	}

	/**
	 * Calculate age.
	 *
	 * @param applicantDob the applicant dob
	 * @return the int
	 */
	private int calculateAge(String applicantDob) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utility::calculateAge():: entry");

		DateFormat sdf = new SimpleDateFormat(dobFormat);
		Date birthDate = null;
		try {
			birthDate = sdf.parse(applicantDob);

		} catch (ParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "Utility::calculateAge():: error with error message "
							+ PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getMessage());
			throw new ParsingException(PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getCode(), e);
		}
		LocalDate ld = new java.sql.Date(birthDate.getTime()).toLocalDate();
		Period p = Period.between(ld, LocalDate.now());
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Utility::calculateAge():: exit");

		return p.getYears();

	}

	/**
	 * Get UIN from identity json (used only for update/res update/activate/de
	 * activate packets).
	 *
	 * @param id the registration id
	 * @return the u in
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws RegistrationProcessorCheckedException
	 */
	public String getUIn(String id, String process, ProviderStageName stageName)
			throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"Utility::getUIn()::entry");
		String UIN = packetManagerService.getFieldByMappingJsonKey(id, MappingJsonConstants.UIN, process, stageName);
		if(isVidSupportedForUpdate && StringUtils.isNotEmpty(UIN) && validateVid(UIN)) {
			regProcLogger.debug("VID structure validated successfully");
			JSONObject responseJson = utilities.retrieveIdrepoJson(UIN);
			if (responseJson != null) {
				UIN = JsonUtil.getJSONValue(responseJson, AbisConstant.UIN);
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"Utility::getUIn()::exit");

		return UIN;

	}

    public boolean validateVid(String vid) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                "Utilities::validateVid()::entry");
        try {
            return vidValidator.validateId(vid);
        } catch (InvalidIDException e) {
            return false;
        }
    }


	/**
	 * Determines whether the applicant was an infant at the time their last packet was processed in the system
	 *
	 * @param registrationId
	 * @param registrationType
	 * @param stageName
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws PacketManagerException
	 */
	public boolean wasInfantWhenLastPacketProcessed(String registrationId, String registrationType, ProviderStageName stageName) throws ApisResourceAccessException, IOException,
			JsonProcessingException, PacketManagerException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "utility::wasInfantWhenLastPacketProcessed()::entry");

		String packetUin = getUIn(registrationId, registrationType, stageName);
		if (packetUin == null || packetUin.trim().isEmpty()) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					"UIN not found in the packet");
			return false;
		}
		// Get created date and time from idrepo using above UIN
		JSONObject idvidResponse = idRepoService.getIdJsonFromIDRepo(packetUin, getGetRegProcessorDemographicIdentity());

		LocalDate lastPacketProcessedDate = resolveLastPacketProcessedDate(registrationId, packetUin, idvidResponse);

		if (lastPacketProcessedDate == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Unable to compute the creation date of the last processed packet");
			throw new PacketDateComputationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getMessage());
		}

		LocalDate dobOfApplicant = getDateOfBirthFromIdrepo(registrationId, idvidResponse);
		if (dobOfApplicant == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Unable to obtain the date of birth");
			throw new PacketDateComputationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_COMPUTE_CREATION_DATE.getMessage());
		}
		int age = calculateAgeAtLastPacketProcessing(dobOfApplicant, lastPacketProcessedDate, registrationId);

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "utility::wasInfantWhenLastPacketProcessed()::exit. age: {}", age);

		return age < getEffectiveAgeLimit();
	}


	/**
	 * Attempts to resolve the last packet processed date using multiple strategies in order
	 *
	 * @param registrationId
	 * @param packetUin
	 * @param idvidResponse
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	private LocalDate resolveLastPacketProcessedDate(String registrationId, String packetUin, JSONObject idvidResponse) throws ApisResourceAccessException, IOException {
		// 1. Try direct lookup
		LocalDate date = getLastProcessedPacketCreatedDate(registrationId, idvidResponse);
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from idvidResponse. date : {}", date);
			return date;
		}
		// Fetching last processed rid from idVidMetadata
		IdVidMetadataResponse idVidMetadataResponse = getIdVidMetadata(packetUin, null);
		if (idVidMetadataResponse == null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: idVidMetadataResponse is null");
			return null;
		}

		// 2. Try from packetId (yyyyMMddHHmmss)
		date = getPacketCreatedDateFromSyncRegistration(idVidMetadataResponse.getRid());
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from packetId. date : {}", date);
			return date;
		}

		// 3. Try from RID directly (yyyyMMddHHmmss)
		date = getPacketCreatedDateFromRid(idVidMetadataResponse.getRid());
		if (date != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Successfully resolved packet creation date from RID. date : {}", date);
			return date;
		}

		// 4. Fallback to IdRepo update date
		LocalDate approxCreatedDateTime = computePacketCreatedFromIdentityUpdate(idVidMetadataResponse, registrationId);
		if (approxCreatedDateTime != null) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"resolveLastPacketProcessedDate() :: Resolved packet creation date from identity update. approxCreatedDateTime : {}", approxCreatedDateTime);
			return approxCreatedDateTime;
		}

		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
				"resolveLastPacketProcessedDate() :: Unable to resolve packet creation date");
		return null;
	}

	/**
	 * Retrieves the effective age limit for the applicant by adding the configured buffer (in months)
	 * to the base age limit. This helps prevent exclusion of applicants who are close to the age threshold
	 * due to edge cases in age calculation
	 */
	public int getEffectiveAgeLimit() {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"utility::getEffectiveAgeLimit():: ageLimit: {}, ageLimitBuffer: {} ", ageLimit, ageLimitBuffer);
		return Integer.parseInt(ageLimit) + ageLimitBuffer;
	}

	/**
	 * Retrieves the created date of the last packet that was processed for the applicant
	 *
	 * @param rid
	 * @param idvidResponse
	 * @return
	 * @throws IOException
	 */
	private LocalDate getLastProcessedPacketCreatedDate(String rid, JSONObject idvidResponse) throws IOException {

		String packetCreatedDateTimeIsoFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getLastProcessedPacketCreatedDate()::entry");

		String packetCreatedOnFieldName = getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);

		// Check if the response object itself is null
		if (idvidResponse == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: idvidResponse is null");
			return null;
		}

		// Check if the key exists in the response
		if (!idvidResponse.containsKey(packetCreatedOnFieldName)) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: fieldName does not exist in idvidResponse : {} ", packetCreatedOnFieldName);
			return null;
		}

		// Safely get the value
		String packetCreatedOn = JsonUtil.getJSONValue(idvidResponse, packetCreatedOnFieldName);

		// Check if the value itself is null
		if (packetCreatedOn == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"getLastProcessedPacketCreatedDate() :: fieldValue does not exist in idvidResponse : {} ", packetCreatedOnFieldName);
			return null;
		}
		LocalDate packetCreatedDate = parseToLocalDate(packetCreatedOn, packetCreatedDateTimeIsoFormat);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getLastProcessedPacketCreatedDate()::exit");
		return packetCreatedDate;
	}

	public String getMappedFieldName(String key) throws IOException {
		JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		return JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, key),
				MappingJsonConstants.VALUE);

	}

	/**
	 * Extract Date of Birth from IDRepo using RID
	 *
	 * @param rid
	 * @param idvidResponse
	 * @return
	 * @throws IOException
	 */
	public LocalDate getDateOfBirthFromIdrepo(String rid, JSONObject idvidResponse) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getDateOfBirthFromIdrepo()::entry");

		// Step 2: Fetch DOB dynamically via mapping
		String dateOfBirthFieldName = getMappedFieldName(MappingJsonConstants.DOB);

		// Check if the key exists in the response
		if (!idvidResponse.containsKey(dateOfBirthFieldName)) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getDateOfBirthFromIdrepo():: fieldName does not exist in idvidResponse : {} ", dateOfBirthFieldName);
			return null;
		}

		// Safely get the value
		String dobValue = JsonUtil.getJSONValue(idvidResponse, dateOfBirthFieldName);

		// Check if the value itself is null
		if (dobValue == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getDateOfBirthFromIdrepo():: fieldValue does not exist in idvidResponse : {} ", dateOfBirthFieldName);
			return null;
		}

		LocalDate dob = parseToLocalDate(dobValue, dobFormat);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getDateOfBirthFromIdrepo()::exit with dob");
		return dob;
	}

	/**
	 * Calculates the age of an applicant at the time of the last packet processing
	 *
	 * @param dateOfBirth
	 * @param lastPacketProcessingDate
	 * @param rid
	 * @return
	 */
	public int calculateAgeAtLastPacketProcessing(LocalDate dateOfBirth, LocalDate lastPacketProcessingDate, String rid) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::calculateAgeAtLastPacketProcessing():: entry");

		// Calculate the period between the two dates
		Period period = Period.between(dateOfBirth, lastPacketProcessingDate);

		// Extract years from the period
		int ageInYears = period.getYears();

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::calculateAgeAtLastPacketProcessing():: exit");

		// Return age in years (as per the original method signature)
		return ageInYears;
	}

	/**
	 * Obtain the IdVidMetadata for applicant
	 *
	 * @param individualId
	 * @param idType
	 * @return
	 * @throws IOException
	 * @throws ApisResourceAccessException
	 */
	public IdVidMetadataResponse getIdVidMetadata(String individualId, String idType) throws IOException, ApisResourceAccessException {
		IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
		idVidMetadataRequest.setIndividualId(individualId);
		idVidMetadataRequest.setIdType(idType);
		IdVidMetadataResponse idVidMetadataResponse = idRepoService.searchIdVidMetadata(idVidMetadataRequest);
		return idVidMetadataResponse;
	}

	/**
	 * Retrieves the packet creation date for the given RID from the sync registration table
	 *
	 * @param rid
	 * @return
	 */
	public LocalDate getPacketCreatedDateFromSyncRegistration(String rid) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromSyncRegistration():: entry");

		List<SyncRegistrationEntity> registrations = syncRegistrationRepository.findByRegistrationId(rid);
		if (registrations == null || registrations.isEmpty()) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getPacketCreatedDateFromSyncRegistration():: no SyncRegistration records found");
			return null;
		}

		// Fetch latest packetId based on createOn
		String packetId = registrations.stream()
				.max(Comparator.comparing(SyncRegistrationEntity::getCreateDateTime)) // latest record
				.map(SyncRegistrationEntity::getPacketId) // extract packetId
				.orElse(null); // if no records, return null

		LocalDate packetCreatedDate = extractPacketCreatedDate(packetId);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromSyncRegistration():: exit with packetCreatedDate: " + packetCreatedDate);

		return packetCreatedDate;
	}

	private LocalDate extractPacketCreatedDate(String id) {
		String packetCreatedDateFormat = "yyyyMMddHHmmss";

		if (id == null || id.length() < 14) {
			regProcLogger.debug("Provided id is null or shorter than 14 characters. Cannot extract packet creation date. id : {} " , id);
			return null;
		}

		String dateStr = id.substring(id.length() - 14);
		return parseToLocalDate(dateStr, packetCreatedDateFormat);
	}


	/**
	 * Wrapper that returns LocalDate only (ignores time)
	 *
	 * @param dateString
	 * @param dateFormat
	 * @return
	 */
	public LocalDate parseToLocalDate(String dateString, String dateFormat) {

		// Log the incoming parameters for debugging
		regProcLogger.debug("Attempting to parse date: {} with format: {}", dateString, dateFormat);

		LocalDateTime ldt = null;

		try {
			// Parse the date string into LocalDateTime
			ldt = parseUTCToLocalDateTime(dateString, dateFormat);

			// Perform validation if parsing was successful
			if (ldt != null) {
				LocalDateTime now = LocalDateTime.now();

				// Check if date is in the future
				if (after(ldt, now)) {
					regProcLogger.error("Parsed LocalDateTime occurs in the future: {}", ldt);
					return null;
				}

				// Check if date is older than 100 years
				LocalDateTime hundredYearsAgo = now.minusYears(200);
				if (before(ldt, hundredYearsAgo)) {
					regProcLogger.error("Date is older than 200 years : {}", ldt);
					return null;
				}
			}
		} catch (io.mosip.kernel.core.exception.ParseException e) {
			regProcLogger.debug("Failed to parse date: {} with format: {}", dateString, dateFormat, e);
			return null;
		}

		return (ldt != null) ? ldt.toLocalDate() : null;
	}

	/**
	 * Extracts the packet creation date from the given Registration ID (RID) by interpreting its last 14 digits as a timestamp in the format {@code yyyyMMddHHmmss}
	 *
	 * @param rid
	 * @return
	 */
	public LocalDate getPacketCreatedDateFromRid(String rid) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromRid():: entry");

		LocalDate packetCreatedDate = extractPacketCreatedDate(rid);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getPacketCreatedDateFromRid():: exit with packetCreatedDate: " + packetCreatedDate);

		return packetCreatedDate;
	}

	/**
	 * Computes the approximate packet creation date (for the packet that updated the identity)
	 *
	 * @param idVidMetadataDTO
	 * @param processingRid
	 * @return
	 */
	public LocalDate computePacketCreatedFromIdentityUpdate(IdVidMetadataResponse idVidMetadataDTO, String processingRid) {

		String packetCreatedDateTimeIsoFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		String updatedOn = idVidMetadataDTO.getUpdatedOn();
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
				"computePacketCreatedFromIdentityUpdate() :: expectedPacketProcessingDurationHours: {}", expectedPacketProcessingDurationHours);

		if (updatedOn == null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: updatedOn is null for RID hence falling back to createdOn.");
			updatedOn = idVidMetadataDTO.getCreatedOn();
		}

		try {
			LocalDateTime referenceDateTime = parseUTCToLocalDateTime(updatedOn, packetCreatedDateTimeIsoFormat);
			LocalDateTime adjustedDateTime = referenceDateTime.minusHours(expectedPacketProcessingDurationHours);

			return adjustedDateTime.toLocalDate();

		} catch (io.mosip.kernel.core.exception.ParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: Failed to parse date {} using format {}. Exception: {}",
					updatedOn, packetCreatedDateTimeIsoFormat, e.getMessage());

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), processingRid,
					"computePacketCreatedFromIdentityUpdate() :: Unexpected error while computing packet creation date. Exception: {}",
					e.getMessage());
		}
		return null;
	}

	/**
	 * Get the BiometricRecord from the IdRepo for a given UIN
	 *
	 * @param uin
	 * @param rid
	 * @return
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	public BiometricRecord getBiometricRecordfromIdrepo(String uin, String rid) throws ApisResourceAccessException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::getBiometricRecordfromIdrepo():: entry");

		// Step 1: Retrieve all documents from IDRepo
		List<Documents> documents = utilities.retrieveIdrepoDocument(uin);

		// Step 2: Load mapping JSON and extract the label for individual biometrics
		JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		// Step 3: Find the biometric document
		String biometricDoc = null;
		for (Documents doc : documents) {
			if (doc.getCategory() != null && doc.getCategory().equalsIgnoreCase(individualBiometricsLabel)) {
				biometricDoc = doc.getValue();
				break;
			}
		}

		if (biometricDoc == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"No biometric document found in IDRepo");
			throw new BiometricClassificationException(PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}

		try {
			// Step 4: Decode and convert to BiometricRecord
			byte[] bi = CryptoUtil.decodeURLSafeBase64(biometricDoc);

			BIR birs = CbeffValidator.getBIRFromXML(bi);

			BiometricRecord biometricRecord = new BiometricRecord();
			// Copy "others" metadata if present
			if(birs.getOthers() != null) {
				HashMap<String, String> others = new HashMap<>();
				birs.getOthers().entrySet().forEach(e -> {
					others.put(e.getKey(), e.getValue());
				});
				biometricRecord.setOthers(others);
			}

			biometricRecord.setSegments(birs.getBirs());

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::getBiometricRecordfromIdrepo():: exit");
			return biometricRecord;

		} catch (Exception e) {
			// Any other error during XML parsing or processing
			String errorMsg = "Unexpected error while getting biometric document";
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					rid, errorMsg, e);
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}
	}

	/**
	 * Checks whether all biometric segments are marked as exceptions excluding FACE and EXCEPTION_PHOTO types, which cannot be marked as exceptions
	 *
	 * @param birs
	 * @param rid
	 * @return
	 * @throws BiometricClassificationException
	 */
	public boolean allBiometricHaveException(List<BIR> birs, String rid) throws BiometricClassificationException {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: entry");

		if (birs == null || birs.isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Biometric list is null or empty");
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}

		boolean hasOthers = hasBiometricWithOthers(birs);
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: hasOthers: {} ",hasOthers);

		for (BIR bir : birs) {
			BiometricType type = bir.getBdbInfo().getType().get(0);
			boolean isFaceOrExceptionPhoto = type == BiometricType.FACE || type == BiometricType.EXCEPTION_PHOTO;

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Checking biometric type: {} ",type);

			if (hasOthers) {
				if (!isFaceOrExceptionPhoto) {
					String exceptionValue = bir.getOthers().get(EXCEPTION);
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
							"utility::allBiometricHaveException()::  Biometric type: {},  exceptionValue: {}", type, exceptionValue);

					if (exceptionValue == null || !exceptionValue.equalsIgnoreCase(TRUE)) {
						regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
								"utility::allBiometricHaveException():: Biometric type: {} does not have exception", type);
						return false;
					}
				}
			} else {
				if (!isFaceOrExceptionPhoto) {
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
							"utility::allBiometricHaveException():: Biometric type: {} does not have exception", type);
					return false;
				}
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"utility::allBiometricHaveException():: exit");
		return true;
	}

	/**
	 * Checks whether any biometric record in the given list contains non-empty "others" data,
	 * 	which can be used to determine if the biometric was generated using the new or old version
	 *
	 * @param bir
	 * @return
	 */
	public boolean hasBiometricWithOthers(List<BIR> bir){
		if (bir == null || bir.isEmpty()) {
			return false;
		}
		return bir.stream()
				.anyMatch(bi -> bi.getOthers() != null && !bi.getOthers().isEmpty());
	}

	/**
	 * Checks whether all biometric segments are marked as exceptions for a registration Id
	 *
	 * @param rid
	 * @param registrationType
	 * @param stageName
	 * @return
	 * @throws BiometricClassificationException
	 */
	public boolean allBiometricHaveException(String rid, String registrationType, ProviderStageName stageName) throws BiometricClassificationException {
		try {
			String uin = getUIn(rid, registrationType, stageName);
			BiometricRecord bm = getBiometricRecordfromIdrepo(uin, rid);
			return allBiometricHaveException(bm.getSegments(), rid);
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
					"utility::allBiometricHaveException():: Error while classifying biometric exceptions", e);
			throw new BiometricClassificationException(
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getCode(),
					PlatformErrorMessages.RPR_BDD_UNABLE_TO_FETCH_BIOMETRIC_INFO.getMessage());
		}
	}

	/**
	 * Retrieves the packet creation date from the packet for a given registrationId
	 *
	 * @param rid
	 * @param process
	 * @param stageName
	 * @return
	 * @throws PacketManagerException
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	public String retrieveCreatedDateFromPacket(String rid, String process, ProviderStageName stageName)
			throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

		Map<String, String> metaInfo = packetManagerService.getMetaInfo(rid, process, stageName);
		String packetCreatedDateTime = metaInfo.get(JsonConstant.CREATIONDATE);

		if (packetCreatedDateTime != null && !packetCreatedDateTime.isEmpty()) {
			return packetCreatedDateTime;
		}

		regProcLogger.error(
				LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(),
				" -- " + rid,
				PlatformErrorMessages.RPR_PVM_PACKET_CREATED_DATE_TIME_EMPTY_OR_NULL.getMessage()
		);

		return null;
	}
}
