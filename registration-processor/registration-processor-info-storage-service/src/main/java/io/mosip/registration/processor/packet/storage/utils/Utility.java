package io.mosip.registration.processor.packet.storage.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
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
    public static final String EXCEPTION = "EXCEPTION";
    public static final String TRUE = "TRUE";
    public static final String DATEOFBIRTH="dateOfBirth";

    /** The Constant UIN. */
    private static final String UIN = "UIN";

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private Utilities utilities;

    @Autowired
    private IdRepoService idRepoService;

    @Autowired
    private BasePacketRepository basePacketRepository;

    @Autowired
    private ObjectMapper objectMapper;



	/** The dob format. */
	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

    @Value("${mosip.bio-deduped.max_age_limit:100}")
    private int MaxAgeLimit;

    @Value("${mosip.bio-deduped.min_age_limit:0}")
    private int MinAgeLimit;

    @Value("${registration.processor.identityjson}")
    private String getRegProcessorIdentityJson;


    /** The get reg processor demographic identity. */
    @Value("${registration.processor.demographic.identity}")
    private String getRegProcessorDemographicIdentity;

    @Value("${mosip.kernel.applicant.type.age.limit}")
    private String ageLimit;

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

    // Infant Age limit taken from config.
    public boolean isApplicantWasInfant(InternalRegistrationStatusDto registrationStatusDto) throws Exception {
//        Fetching the packet created date and time
        Date packetrCeatedDate=parseDate(getPacketcreatedDateAndtimesFromIdrepo(registrationStatusDto));
        if (packetrCeatedDate==null){
            packetrCeatedDate=parseDate(getPacketCreationDateTimeFromRegList(registrationStatusDto.getRegistrationId()));
            if (packetrCeatedDate==null) {
                packetrCeatedDate=parseDate(getPacketCreatedDateTimeFromRid(registrationStatusDto.getRegistrationId()));
            }
        }
        Date dobOfApplicant=parseDate(getDateOfBirthFromIdrepo(registrationStatusDto));
        int age=calculateAgeAtTheTimeOfRegistration(dobOfApplicant,packetrCeatedDate);
        int ageThreshold = Integer.parseInt(ageLimit);
        return age < ageThreshold;
    }


    /**    get packet created date and time from idrepo */
    public String getPacketcreatedDateAndtimesFromIdrepo(InternalRegistrationStatusDto registrationStatusDao) throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
                "Utilities::getPacketcreatedDateAndtimesFromIdrepo()::entry");
        /**        getting Uin from packetmanager from update packet */
        String uin=packetManagerService.getField(registrationStatusDao.getRegistrationId(),UIN,registrationStatusDao.getRegistrationType(),ProviderStageName.BIO_DEDUPE);
/**        get created date and time from idrepo using above UIN */
        regProcLogger.debug("Uin = ",uin);
        String HCstring="1919-02-17T07:20:46.407Z";
        String[] str=HCstring.split("T");
        return str[0].replace("-","/");


    }


    public String getDateOfBirthFromIdrepo(InternalRegistrationStatusDto internalRegistrationStatusDto) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
                "Utilities::getDateOfDirthFromIdrepo()::entry");
        String uin=packetManagerService.getField(internalRegistrationStatusDto.getRegistrationId(),MappingJsonConstants.UIN,internalRegistrationStatusDto.getRegistrationType(),ProviderStageName.BIO_DEDUPE);
        JSONObject responseDTO= idRepoService.getIdJsonFromIDRepo(uin,getGetRegProcessorDemographicIdentity());
        if (responseDTO != null) {
            return JsonUtil.getJSONValue(responseDTO,DATEOFBIRTH );
        }
        return "";
    }

    public Date parseDate(String dateStr) {

        try {
            if (dateStr!=null){
                DateFormat sdf = new SimpleDateFormat(dobFormat);
                if(!dateStr.contains("/")) {
                    dateStr=getDateFromatedString(dateStr);
                }

                sdf.setLenient(false);
                Date birthDate = sdf.parse(dateStr);
                return birthDate;
            }
        }catch (Exception e){
            regProcLogger.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    public String getDateFromatedString(String dt) throws ParseException {
            DateFormat sdf = new SimpleDateFormat(dobFormat);
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Date date = inputFormat.parse(dt);
            return sdf.format(date);
    }


    //Minimum and Maximum age needs to be fetched from Properties
    public int calculateAgeAtTheTimeOfRegistration(Date dob, Date registeredDate) throws Exception {

        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                "Utilities::calculateAgeAtTheTimeOfRegistration():: entry");
        int age=0;
        Calendar dobCalendar = Calendar.getInstance();
        dobCalendar.setTime(dob);

        Calendar registeredCalendar = Calendar.getInstance();
        registeredCalendar.setTime(registeredDate);
        age = registeredCalendar.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

        if (age < MinAgeLimit && age > MaxAgeLimit ) {
            throw new IOException(PlatformErrorMessages.RPR_PDS_AGE_INVALID_EXCEPTION.getMessage());
        }

        return age;
    }

    public String getPacketCreationDateTimeFromRegList(String rid)
    {
        String packetId=basePacketRepository.getPacketIdfromRegprcList(rid);
        if (packetId!=null){
            packetId=packetId.substring(Math.max(0, packetId.length() - 14));
        }
        return packetId;
    }
    public String getPacketCreatedDateTimeFromRid(String rid) {
        if (rid != null) {
            return rid.substring(Math.max(0, rid.length() - 14));
        }
        return null;
    }

    public BiometricRecord getBiometricRecordfromIdrepo(String uin) throws Exception {
        ResponseDTO responseFromIDRepo =idRepoService.getIdResponseFromIDRepo(uin);
        String doc = responseFromIDRepo.getDocuments().get(0).getValue();
        byte[] bi=Base64.getUrlDecoder().decode(doc);
        if (bi == null)
            return null;
        BIR birs = CbeffValidator.getBIRFromXML(bi);
        BiometricRecord biometricRecord = new BiometricRecord();
        BDBInfo bdbInfo=new BDBInfo();
            biometricRecord.setSegments(birs.getBirs());
        return biometricRecord;
    }


    public boolean isALLBiometricHaveExceptoin(List<BIR> birs) throws PacketManagerException, IOException, ApisResourceAccessException, JsonProcessingException , BiometricException {
        boolean exceptionValue = true;
        // setting biometricNotAvailableTagValue for each modality in case biometrics are not available (need to confirm the exception)
        if (birs == null) {
            throw new BiometricException(PlatformErrorMessages.UNABLE_TO_FETCH_BIO_INFO.getCode(), PlatformErrorMessages.UNABLE_TO_FETCH_BIO_INFO.getMessage());
        }
        if (isBiometricHavingOthers(birs)) {
            // get individual biometrics file name from id.json
            for (BIR bir : birs) {

                if (!(bir.getBdbInfo().getType().get(0) == BiometricType.FACE || bir.getBdbInfo().getType().get(0) == BiometricType.EXCEPTION_PHOTO)) {
                    if (bir.getOthers() != null && bir.getOthers().get(EXCEPTION).equals(false)) {
                            return true;
                    }
                }
            }
        }else {
            for (BIR bir:birs)
            {
                return !(bir.getBdbInfo().getType().get(0) == BiometricType.FACE || bir.getBdbInfo().getType().get(0) == BiometricType.EXCEPTION_PHOTO);
            }
        }
        return exceptionValue;
    }

//    checking Biometric genrated using new or old version
    public boolean isBiometricHavingOthers(List<BIR> bir){
        return bir.stream()
                .anyMatch(bi -> bi.getOthers() != null && !bi.getOthers().isEmpty());
    }

    //    checking is ALL biometric is with exception
    public boolean isBioWithException(InternalRegistrationStatusDto registrationStatusDto) throws Exception {
    String uin=packetManagerService.getField(registrationStatusDto.getRegistrationId(),MappingJsonConstants.UIN,registrationStatusDto.getRegistrationType(),ProviderStageName.BIO_DEDUPE);
    BiometricRecord bm=getBiometricRecordfromIdrepo(uin);
    return isALLBiometricHaveExceptoin(bm.getSegments());
    }
}
