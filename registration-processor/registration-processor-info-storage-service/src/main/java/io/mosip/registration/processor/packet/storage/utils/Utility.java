package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import lombok.Data;

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
	private Utilities utilities;

	/** The dob format. */
	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

	private static final String VALUE = "value";

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
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"Utility::getUIn()::exit");

		return UIN;

	}
}
