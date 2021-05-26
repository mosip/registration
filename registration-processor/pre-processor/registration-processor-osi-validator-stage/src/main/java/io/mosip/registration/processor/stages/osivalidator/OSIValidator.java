package io.mosip.registration.processor.stages.osivalidator;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.IndividualIdDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.ParentOnHoldException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.ServerError;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserResponseDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.osivalidator.utils.OSIUtils;
import io.mosip.registration.processor.stages.osivalidator.utils.StatusMessage;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class OSIValidator.
 */
@Service
public class OSIValidator {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(OSIValidatorStage.class);

	/** The packet info manager. */
	@Autowired
	PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = "\\";

	/** The Constant BIOMETRIC_INTRODUCER. */
	public static final String BIOMETRIC = PacketFiles.BIOMETRIC.name() + FILE_SEPARATOR;

	@Autowired
	private IdRepoService idRepoService;

	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The rest client service. */
	@Autowired
	RegistrationProcessorRestClientService<Object> restClientService;

	/** The osi utils. */
	@Autowired
	private OSIUtils osiUtils;

	@Autowired
	ABISHandlerUtil abisHandlerUtil;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The Constant TRUE. */
	private static final String ISTRUE = "true";

	@Value("${mosip.kernel.applicant.type.age.limit}")
	private String ageLimit;

	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

	@Value("${registration.processor.validate.introducer}")
	private boolean introducerValidation;

	@Value("${registration.processor.sub-process}")
	private String subProcesses;
	
	@Value("${registration.processor.validateIndividualBiometricsforSubProcess.enabled}")
	private boolean validateIndividualBiometricsforSubProcess;
	
	@Value("${registration-processor.validateOfficerOrSupervisorBiometricsforSubProcess.enabled}")
	private boolean validateOfficerOrSupervisorBiometricsforSubProcess;
	
	
	
	@Autowired
	private Utilities utility;

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Autowired
	private AuthUtil authUtil;

	/** The Constant APPLICATION_ID. */
	public static final String IDA_APP_ID = "IDA";

	/** The Constant RSA. */
	public static final String RSA = "RSA";

	/** The Constant RSA. */
	public static final String PARTNER_ID = "PARTNER";

	public static final String INDIVIDUAL_TYPE_UIN = "UIN";

	private static final String INDIVIDUAL_TYPE_USERID = "USERID";
	
	private static final String APPID = "regproc";

	@Autowired
	ObjectMapper mapper;
	/**
	 * Checks if is valid OSI.
	 *
	 * @param registrationId the registration id
	 * @return true, if is valid OSI
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws NumberFormatException
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws ParentOnHoldException
	 * @throws AuthSystemException
	 * @throws RegistrationProcessorCheckedException
	 */
	public boolean isValidOSI(String registrationId, InternalRegistrationStatusDto registrationStatusDto, Map<String, String> metaInfo)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			NumberFormatException, BiometricException, BioTypeException,
			AuthSystemException, PacketManagerException, JSONException, JsonProcessingException, ParentOnHoldException, CertificateException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidOSI()::entry");
		boolean isValidOsi = false;
		
		
		if(subProcesses.contains(registrationStatusDto.getRegistrationType())) {
			return validateOSIforSubProcess( registrationId,  registrationStatusDto, metaInfo);
			
		}

		/** Getting data from packet MetadataInfo */
		RegOsiDto regOsi = osiUtils.getOSIDetailsFromMetaInfo(metaInfo);
		String officerId = regOsi.getOfficerId();
		String supervisorId = regOsi.getSupervisorId();
		if ((officerId == null || officerId.isEmpty()) && (supervisorId == null || supervisorId.isEmpty())) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.SUPERVISORID_AND_OFFICERID_NOT_PRESENT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(StatusUtil.SUPERVISOR_OFFICER_NOT_FOUND_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_OFFICER_NOT_FOUND_PACKET.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Both Officer and Supervisor ID are not present in Packet");
			return false;
		} else {
			boolean isActive = isActiveUserId(registrationId, regOsi, metaInfo, registrationStatusDto);
			if (!isActive) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.SUPERVISOR_OR_OFFICER_WAS_INACTIVE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				String userId;
				if (StringUtils.isNotEmpty(regOsi.getOfficerId())) {
					userId = regOsi.getOfficerId();
				} else {
					userId = regOsi.getSupervisorId();
				}
				registrationStatusDto
						.setStatusComment(StatusUtil.SUPERVISOR_OR_OFFICER_WAS_INACTIVE.getMessage() + userId);
				registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_OR_OFFICER_WAS_INACTIVE.getCode());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						StatusMessage.SUPERVISOR_OR_OFFICER_WAS_INACTIVE);
				return false;
			}
			if (((isValidOperator(regOsi, registrationId, registrationStatusDto))
					&& (isValidSupervisor(regOsi, registrationId, registrationStatusDto)))
					&& (isValidIntroducer(registrationId, registrationStatusDto)))
				isValidOsi = true;
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "OSIValidator::isValidOSI()::exit");
		}
		return isValidOsi;
	}

	private boolean  validateOSIforSubProcess(String registrationId, InternalRegistrationStatusDto registrationStatusDto,
			Map<String, String> metaInfo) throws ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException, ParentOnHoldException, AuthSystemException, JsonProcessingException, PacketManagerException, CertificateException, IOException, JSONException, BiometricException {
		boolean isValidIntroducer=false;
		boolean isValidOfficerOrSupervisor=false;
		if(validateIndividualBiometricsforSubProcess) {
			isValidIntroducer=isValidIntroducer(registrationId, registrationStatusDto);
		}
		else {
			isValidIntroducer=true;
		}
		if(validateOfficerOrSupervisorBiometricsforSubProcess) {
			
			RegOsiDto regOsi = osiUtils.getOSIDetailsFromMetaInfo(metaInfo);
			String officerId = regOsi.getOfficerId();
			String supervisorId = regOsi.getSupervisorId();
			if ((officerId == null || officerId.isEmpty()) && (supervisorId == null || supervisorId.isEmpty())) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.SUPERVISORID_AND_OFFICERID_NOT_PRESENT_IN_PACKET));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto.setStatusComment(StatusUtil.SUPERVISOR_OFFICER_NOT_FOUND_PACKET.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_OFFICER_NOT_FOUND_PACKET.getCode());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Both Officer and Supervisor ID are not present in Packet");
				isValidOfficerOrSupervisor= false;
			} else {
				boolean isActive = isActiveUserId(registrationId, regOsi, metaInfo, registrationStatusDto);
				if (!isActive) {
					registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.SUPERVISOR_OR_OFFICER_WAS_INACTIVE));
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					String userId;
					if (StringUtils.isNotEmpty(regOsi.getOfficerId())) {
						userId = regOsi.getOfficerId();
					} else {
						userId = regOsi.getSupervisorId();
					}
					registrationStatusDto
							.setStatusComment(StatusUtil.SUPERVISOR_OR_OFFICER_WAS_INACTIVE.getMessage() + userId);
					registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_OR_OFFICER_WAS_INACTIVE.getCode());
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							StatusMessage.SUPERVISOR_OR_OFFICER_WAS_INACTIVE);
					isValidOfficerOrSupervisor= false;
				}
				if (((isValidOperator(regOsi, registrationId, registrationStatusDto))
						&& (isValidSupervisor(regOsi, registrationId, registrationStatusDto)))) {
					isValidOfficerOrSupervisor = true;
				}
			}
		}
		else {
			isValidOfficerOrSupervisor=true;
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::validateOSIforSubProcess()::exit");
		return isValidIntroducer && isValidOfficerOrSupervisor;
		
	}

	private boolean isActiveUser(String officerId, String creationDate, String supervisorId,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean wasOfficerActiveDuringPCT = false;
		boolean wasSupervisorActiveDuringPCT = false;
		if (officerId != null && !officerId.isEmpty()) {
			UserResponseDto officerResponse = isUserActive(officerId, creationDate, registrationStatusDto);
			if (officerResponse.getErrors() == null) {
				wasOfficerActiveDuringPCT = officerResponse.getResponse().getUserResponseDto().get(0).getIsActive();
				if (!wasOfficerActiveDuringPCT) {
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "", StatusMessage.OFFICER_NOT_ACTIVE);
				}
			} else {
				List<ServerError> errors = officerResponse.getErrors();
				registrationStatusDto.setStatusComment(
						StatusUtil.OFFICER_AUTHENTICATION_FAILED.getMessage() + errors.get(0).getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.OFFICER_AUTHENTICATION_FAILED.getCode());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "", errors.get(0).getMessage());
			}

		}

		if (supervisorId != null && !supervisorId.isEmpty()) {
			UserResponseDto supervisorResponse = isUserActive(supervisorId, creationDate, registrationStatusDto);
			if (supervisorResponse.getErrors() == null) {
				wasSupervisorActiveDuringPCT = supervisorResponse.getResponse().getUserResponseDto().get(0)
						.getIsActive();
				if (!wasSupervisorActiveDuringPCT) {
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "", StatusMessage.SUPERVISOR_NOT_ACTIVE);
				}
			} else {
				List<ServerError> errors = supervisorResponse.getErrors();
				registrationStatusDto.setStatusComment(
						StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getMessage() + errors.get(0).getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getCode());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "", errors.get(0).getMessage());
			}
		}
		return wasSupervisorActiveDuringPCT || wasOfficerActiveDuringPCT;
	}

	private UserResponseDto isUserActive(String operatorId, String creationDate,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		UserResponseDto userResponse;
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(operatorId);
		pathSegments.add(creationDate);

		userResponse = (UserResponseDto) restClientService.getApi(ApiName.USERDETAILS, pathSegments, "", "",
				UserResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"OSIValidator::isUserActive()::User Details Api ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(userResponse));

		return userResponse;
	}

	/**
	 * Checks if is valid operator.
	 *
	 * @param regOsi         the reg osi
	 * @param registrationId the registration id
	 * @return true, if is valid operator
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws ApisResourceAccessException
	 * @throws                                  io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws AuthSystemException
	 * @throws Exception
	 * 
	 */
	private boolean isValidOperator(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, BioTypeException,
			AuthSystemException, JsonProcessingException, PacketManagerException, CertificateException, NoSuchAlgorithmException {
		boolean isValid = false;
		String officerId = regOsi.getOfficerId();
		if (officerId != null) {
			// officer password and otp check
			String officerPassword = regOsi.getOfficerHashedPwd();
			String officerOTPAuthentication = regOsi.getOfficerOTPAuthentication();

			String officerBiometricFileName = regOsi.getOfficerBiometricFileName();

			if (StringUtils.isEmpty(officerBiometricFileName) || officerBiometricFileName == null) {
				isValid = validateOtpAndPwd(officerPassword, officerOTPAuthentication);
				if (!isValid) {
					registrationStatusDto.setStatusComment(StatusUtil.PASSWORD_OTP_FAILURE.getMessage() + officerId);
					registrationStatusDto.setSubStatusCode(StatusUtil.PASSWORD_OTP_FAILURE.getCode());
					registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PASSWORD_OTP_FAILURE));
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							StatusMessage.PASSWORD_OTP_FAILURE);
				}
			} else {
				BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
						MappingJsonConstants.OFFICERBIOMETRICFILENAME, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);

				if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().isEmpty()) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
							registrationId, "ERROR =======>" + StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getCode());
					registrationStatusDto.setStatusComment(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage() + " for officer : " + officerId);
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					isValid = false;
				} else {
					isValid = validateUserBiometric(registrationId, officerId, biometricRecord.getSegments(), INDIVIDUAL_TYPE_USERID,
							registrationStatusDto);
				}
			}
		} else {
			isValid = true; // either officer or supervisor information is mandatory. Officer id can be null
		}
		return isValid;
	}

	/**
	 * Check biometric null.
	 *
	 * @param fingerPrint
	 *            the finger print
	 * @param iris
	 *            the iris
	 * @param face
	 *            the face
	 * @param pin
	 *            the pin
	 * @return true, if successful
	 */
	boolean checkBiometricNull(String fingerPrint, String iris, String face, String pin) {
		return (fingerPrint == null) && (iris == null) && (face == null) && (pin == null);
	}

	/**
	 * Checks if is valid supervisor.
	 *
	 * @param regOsi                the reg osi
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @return true, if is valid supervisor
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws ApisResourceAccessException
	 * @throws                                  io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws AuthSystemException
	 * @throws Exception
	 */
	private boolean isValidSupervisor(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, AuthSystemException, JsonProcessingException, PacketManagerException, CertificateException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidSupervisor()::entry");
		String supervisorId = regOsi.getSupervisorId();
		boolean isValid = false;
		if (supervisorId != null) {

			// officer password and otp check
			String supervisiorPassword = regOsi.getSupervisorHashedPwd();
			String supervisorOTP = regOsi.getSupervisorOTPAuthentication();

			String supervisorBiometricFileName = regOsi.getSupervisorBiometricFileName();

			if (StringUtils.isEmpty(supervisorBiometricFileName) || supervisorBiometricFileName == null) {
				isValid = validateOtpAndPwd(supervisiorPassword, supervisorOTP);
				if (!isValid) {
					registrationStatusDto
							.setStatusComment(StatusUtil.PASSWORD_OTP_FAILURE_SUPERVISOR.getMessage() + supervisorId);
					registrationStatusDto.setSubStatusCode(StatusUtil.PASSWORD_OTP_FAILURE_SUPERVISOR.getCode());
					registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PASSWORD_OTP_FAILURE));
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							StatusMessage.PASSWORD_OTP_FAILURE);
				}
			} else {
				BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
						MappingJsonConstants.SUPERVISORBIOMETRICFILENAME, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);

				if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().isEmpty()) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
							registrationId, "ERROR =======>" + StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getCode());
					registrationStatusDto.setStatusComment(StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage() + " for officer : " + supervisorId);
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
					isValid = false;
				}
				isValid = validateUserBiometric(registrationId, supervisorId, biometricRecord.getSegments(),
						INDIVIDUAL_TYPE_USERID, registrationStatusDto);
			}

		} else {
			isValid = true; // either officer or supervisor information is mandatory. Supervisor id can be
							// null
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidSupervisor()::exit");
		return isValid;
	}

	/**
	 * Checks if is valid introducer.
	 *
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @return true, if is valid introducer
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws ParentOnHoldException
	 * @throws AuthSystemException
	 * @throws RegistrationProcessorCheckedException
	 */
	private boolean isValidIntroducer(String registrationId, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			BioTypeException, ParentOnHoldException,
			AuthSystemException, JsonProcessingException, PacketManagerException, CertificateException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidIntroducer()::entry");

		if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.NEW.name())
				|| (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.UPDATE.name()))) {
			int age = utility.getApplicantAge(registrationId, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);
			int ageThreshold = Integer.parseInt(ageLimit);
			if (age < ageThreshold) {
				if (!introducerValidation)
					return true;

				String introducerUIN = packetManagerService.getFieldByMappingJsonKey(registrationId, MappingJsonConstants
						.PARENT_OR_GUARDIAN_UIN, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);
				String introducerRID = packetManagerService.getFieldByMappingJsonKey(registrationId, MappingJsonConstants
						.PARENT_OR_GUARDIAN_RID, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);

				if (isValidIntroducer(introducerUIN, introducerRID)) {
					registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PARENT_UIN_AND_RID_NOT_IN_PACKET));
					registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
					registrationStatusDto.setStatusComment(StatusUtil.UIN_RID_NOT_FOUND.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.UIN_RID_NOT_FOUND.getCode());
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							StatusMessage.PARENT_UIN_AND_RID_NOT_IN_PACKET);
					return false;
				}

				if ((introducerUIN == null || introducerUIN.isEmpty())
						&& validateIntroducerRid(introducerRID, registrationId, registrationStatusDto)) {

					introducerUIN = idRepoService.getUinByRid(introducerRID,
							utility.getGetRegProcessorDemographicIdentity());

					if (introducerUIN == null) {
						registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
								.getStatusCode(RegistrationExceptionTypeCode.PARENT_UIN_NOT_AVAIALBLE));
						registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
						registrationStatusDto.setStatusComment(StatusUtil.PARENT_UIN_NOT_FOUND.getMessage());
						registrationStatusDto.setSubStatusCode(StatusUtil.PARENT_UIN_NOT_FOUND.getCode());
						regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
								StatusMessage.PARENT_UIN_NOT_AVAIALBLE);
						return false;
					}

				}
				if (introducerUIN != null && !introducerUIN.isEmpty()) {
					return validateIntroducerBiometric(registrationId, registrationStatusDto, introducerUIN);
				} else {
					return false;
				}
			}

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidIntroducer()::exit");

		return true;
	}

	private boolean validateIntroducerBiometric(String registrationId,
			InternalRegistrationStatusDto registrationStatusDto,
			String introducerUIN) throws IOException, ApisResourceAccessException,
			BioTypeException, AuthSystemException, JsonProcessingException, PacketManagerException, CertificateException, NoSuchAlgorithmException {
		BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
				MappingJsonConstants.PARENT_OR_GUARDIAN_BIO, registrationStatusDto.getRegistrationType(), ProviderStageName.OSI_VALIDATOR);
		if (biometricRecord != null && biometricRecord.getSegments() != null) {
			return validateUserBiometric(registrationId, introducerUIN, biometricRecord.getSegments(),
					INDIVIDUAL_TYPE_UIN, registrationStatusDto);
		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.PARENT_BIOMETRIC_NOT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto
					.setStatusComment(StatusUtil.PARENT_BIOMETRIC_FILE_NAME_NOT_FOUND.getMessage());
			registrationStatusDto
					.setSubStatusCode(StatusUtil.PARENT_BIOMETRIC_FILE_NAME_NOT_FOUND.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					StatusMessage.PARENT_BIOMETRIC_NOT_IN_PACKET);
			return false;
		}
	}

	private boolean isValidIntroducer(String introducerUIN, String introducerRID) {
		return ((introducerUIN == null && introducerRID == null) || ((introducerUIN != null && introducerUIN.isEmpty())
				&& (introducerRID != null && introducerRID.isEmpty())));
	}



	/**
	 * Validate otp and pwd.
	 *
	 * @param pwd
	 *            the password
	 * @param otp
	 *            the otp
	 * @return true, if successful
	 */
	boolean validateOtpAndPwd(String pwd, String otp) {
		return (pwd != null && pwd.equals(ISTRUE) || otp != null && otp.equals(ISTRUE));
	}

	/**
	 * Validate introducer.
	 *
	 * @param regOsi
	 *            the reg osi
	 * @param registrationId
	 *            the registration id
	 * @param introducerUin
	 *            the introducer uin
	 * @return true, if successful
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 */

	/**
	 * Validate user.
	 *
	 * @param userId
	 *            the userid
	 * @param registrationId
	 *            the registration id
	 * @param list
	 *            biometric data as BIR object
	 * @param individualType
	 *            user type
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws AuthSystemException 
	 */

	private boolean validateUserBiometric(String registrationId, String userId, List<BIR> list,
			String individualType, InternalRegistrationStatusDto registrationStatusDto)
			throws ApisResourceAccessException, IOException, BioTypeException, AuthSystemException, CertificateException, NoSuchAlgorithmException {

		if(INDIVIDUAL_TYPE_USERID.equalsIgnoreCase(individualType)) {
			userId = getIndividualIdByUserId(userId);
			individualType = null;
		 }
		
		AuthResponseDTO authResponseDTO = authUtil.authByIdAuthentication(userId, individualType, list);
		if (authResponseDTO.getErrors() == null || authResponseDTO.getErrors().isEmpty()) {
			if (authResponseDTO.getResponse().isAuthStatus()) {
				return true;
			} else {
				registrationStatusDto.setLatestTransactionStatusCode(
						registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.AUTH_FAILED));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto
						.setStatusComment(StatusUtil.OFFICER_SUPERVISOR_AUTHENTICATION_FAILED.getMessage() + userId);
				registrationStatusDto.setSubStatusCode(StatusUtil.OFFICER_SUPERVISOR_AUTHENTICATION_FAILED.getCode());
				return false;
			}

		} else {
			List<io.mosip.registration.processor.core.auth.dto.ErrorDTO> errors = authResponseDTO.getErrors();
			if (errors.stream().anyMatch(error -> error.getErrorCode().equalsIgnoreCase("IDA-MLC-007"))) {
				throw new AuthSystemException(PlatformErrorMessages.RPR_AUTH_SYSTEM_EXCEPTION.getMessage());
			} else {
				registrationStatusDto.setLatestTransactionStatusCode(
						registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.AUTH_ERROR));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				String result = errors.stream().map(s -> s.getErrorMessage() + " ").collect(Collectors.joining());
				registrationStatusDto.setStatusComment(result);
				registrationStatusDto.setSubStatusCode(StatusUtil.OFFICER_SUPERVISOR_AUTHENTICATION_FAILED.getCode());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, result);
				return false;
			}
			
		}
		
	}
	
	/**
	 *  get the individualId by userid
	 * @param userid
	 * @return individualId 
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	private String getIndividualIdByUserId(String userid) throws ApisResourceAccessException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"OSIValidator::getIndividualIdByUserId():: entry");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(APPID);
		pathSegments.add(userid);
		String individualId = null;
		ResponseWrapper<?> response = null;
		response =  (ResponseWrapper<?>) restClientService.getApi(ApiName.GETINDIVIDUALIDFROMUSERID, pathSegments, "", "", ResponseWrapper.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"OSIValidator::getIndividualIdByUserId():: GETINDIVIDUALIDFROMUSERID GET service call ended successfully");
		if (response.getErrors() != null) {
			throw new ApisResourceAccessException(PlatformErrorMessages.LINK_FOR_USERID_INDIVIDUALID_FAILED_OSI_EXCEPTION.toString());
		}else {
			IndividualIdDto readValue = mapper.readValue(mapper.writeValueAsString(response.getResponse()),IndividualIdDto.class);
			individualId = readValue.getIndividualId();
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"OSIValidator::getIndividualIdByUserId():: exit");
		return individualId;
	}

	/**
	 * Validate introducer rid.
	 *
	 * @param introducerRid         the introducer rid
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws ParentOnHoldException
	 */
	private boolean validateIntroducerRid(String introducerRid, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto) throws ParentOnHoldException {
		InternalRegistrationStatusDto introducerRegistrationStatusDto = registrationStatusService
				.getRegStatusForMainProcess(introducerRid);
		if (introducerRegistrationStatusDto != null) {
			if (introducerRegistrationStatusDto.getStatusCode().equals(RegistrationStatusCode.PROCESSING.toString())) {

				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.OSI_FAILED_ON_HOLD_PARENT_PACKET));

				registrationStatusDto.setStatusComment(StatusUtil.PACKET_ON_HOLD.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_ON_HOLD.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, StatusMessage.PACKET_IS_ON_HOLD);
				throw new ParentOnHoldException(StatusUtil.PACKET_ON_HOLD.getCode(),
						StatusUtil.PACKET_ON_HOLD.getMessage());

			} else if (introducerRegistrationStatusDto.getStatusCode()
					.equals(RegistrationStatusCode.REJECTED.toString())
					|| introducerRegistrationStatusDto.getStatusCode()
							.equals(RegistrationStatusCode.FAILED.toString())) {

				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.OSI_FAILED_REJECTED_PARENT));

				registrationStatusDto.setStatusComment(StatusUtil.CHILD_PACKET_REJECTED.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.CHILD_PACKET_REJECTED.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						StatusMessage.OSI_FAILED_REJECTED_PARENT);

				return false;
			} else {
				return true;
			}

		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.OSI_FAILED_ON_HOLD_PARENT_PACKET));

			registrationStatusDto.setStatusComment(StatusUtil.PACKET_IS_ON_HOLD.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_IS_ON_HOLD.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, StatusMessage.PACKET_IS_ON_HOLD);
			throw new ParentOnHoldException(StatusUtil.PACKET_ON_HOLD.getCode(),
					StatusUtil.PACKET_ON_HOLD.getMessage());
		}

	}

	private boolean isActiveUserId(String registrationId, RegOsiDto regOsi, Map<String, String> metaInfo,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean isValid = false;
		String creationDate = metaInfo.get(JsonConstant.CREATIONDATE);
		if (creationDate != null && !(StringUtils.isEmpty(creationDate))) {

			isValid = isActiveUser(regOsi.getOfficerId(), creationDate, regOsi.getSupervisorId(),
					registrationStatusDto);
			if (!isValid) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.SUPERVISOR_OR_OFFICER_WAS_INACTIVE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			}

		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.PACKET_CREATION_DATE_NOT_PRESENT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(StatusUtil.PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "packet creationDate is null");

		}
		return isValid;
	}


}