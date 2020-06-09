package io.mosip.registration.processor.stages.osivalidator;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.ParentOnHoldException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
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

	@Autowired
	private PacketReaderService packetReaderService;

	/** The rest client service. */
	@Autowired
	RegistrationProcessorRestClientService<Object> restClientService;

	/** The osi utils. */
	@Autowired
	private OSIUtils osiUtils;

	@Autowired
	ABISHandlerUtil abisHandlerUtil;

	/** The Constant TRUE. */
	private static final String ISTRUE = "true";

	@Value("${mosip.kernel.applicant.type.age.limit}")
	private String ageLimit;

	@Value("${registration.processor.applicant.dob.format}")
	private String dobFormat;

	@Value("${registration.processor.validate.introducer}")
	private boolean introducerValidation;

	@Value("${packet.default.source}")
	private String officerBiometricFileSource;

	@Value("${packet.default.source}")
	private String supervisorBiometricFileSource;

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

	@Autowired
	private IdSchemaUtils idSchemaUtils;

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
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	public boolean isValidOSI(String registrationId, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			ParserConfigurationException, SAXException, NumberFormatException, BiometricException, BioTypeException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ParentOnHoldException,
			AuthSystemException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidOSI()::entry");
		boolean isValidOsi = false;

		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson();
		Identity identity = osiUtils.getIdentity(registrationId);
		/** Getting data from packet MetadataInfo */
		RegOsiDto regOsi = osiUtils.getOSIDetailsFromMetaInfo(registrationId, identity);
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
			boolean isActive = isActiveUserId(registrationId, regOsi, identity, registrationStatusDto);
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
					&& (isValidIntroducer(registrationId, regProcessorIdentityJson,
							registrationStatusDto)))
				isValidOsi = true;
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "OSIValidator::isValidOSI()::exit");
		}
		return isValidOsi;
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
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 * @throws Exception
	 * 
	 */
	private boolean isValidOperator(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, AuthSystemException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
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

				InputStream biometricStream = packetReaderService.getFile(registrationId,
						officerBiometricFileName.toUpperCase(), officerBiometricFileSource);
				byte[] officerbiometric = IOUtils.toByteArray(biometricStream);
				isValid = validateUserBiometric(registrationId, officerId, officerbiometric, INDIVIDUAL_TYPE_USERID,
						registrationStatusDto);
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
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 * @throws Exception
	 */
	private boolean isValidSupervisor(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, AuthSystemException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
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
				InputStream biometricStream = packetReaderService.getFile(registrationId,
						supervisorBiometricFileName.toUpperCase(), supervisorBiometricFileSource);
				byte[] supervisorbiometric = IOUtils.toByteArray(biometricStream);
				isValid = validateUserBiometric(registrationId, supervisorId, supervisorbiometric,
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
	 * @param regOsi                the reg osi
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
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	private boolean isValidIntroducer(String registrationId,
			JSONObject regProcessorIdentityJson, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			ParserConfigurationException, SAXException, BiometricException, BioTypeException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ParentOnHoldException,
			AuthSystemException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "OSIValidator::isValidIntroducer()::entry");

		if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.NEW.name())
				|| (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.UPDATE.name()))) {
			int age = utility.getApplicantAge(registrationId);
			int ageThreshold = Integer.parseInt(ageLimit);
			if (age < ageThreshold) {
				if (!introducerValidation)
					return true;
				String introducerUinLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PARENT_OR_GUARDIAN_UIN), MappingJsonConstants.VALUE);
				String introducerRidLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PARENT_OR_GUARDIAN_RID), MappingJsonConstants.VALUE);
				String introducerBiometricsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PARENT_OR_GUARDIAN_BIO), MappingJsonConstants.VALUE);

				String introducerUIN = JsonUtil.getJSONValue(
						utility.getDemographicIdentityJSONObject(registrationId, introducerUinLabel),
						introducerUinLabel);
				String introducerRID = JsonUtil.getJSONValue(
						utility.getDemographicIdentityJSONObject(registrationId, introducerRidLabel),
						introducerRidLabel);
				String introducerBiometricsFileName = JsonUtil.getJSONValue(JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(registrationId, introducerBiometricsLabel), introducerBiometricsLabel), MappingJsonConstants.VALUE);

				if (isValidIntroducer(introducerUIN, introducerRID)) {
					registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
							.getStatusCode(RegistrationExceptionTypeCode.PARENT_UIN_AND_RID_NOT_IN_PACKET));
					registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
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
					return validateIntroducerBiometric(registrationId, registrationStatusDto,
							introducerBiometricsFileName, introducerUIN, introducerBiometricsLabel);
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
			InternalRegistrationStatusDto registrationStatusDto, String introducerBiometricsFileName,
			String introducerUIN,String introducerBiometricsLabel) throws IOException, PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			ParserConfigurationException, SAXException, BiometricException, BioTypeException, AuthSystemException,
			ApiNotAccessibleException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		if (introducerBiometricsFileName != null && (!introducerBiometricsFileName.trim().isEmpty())) {
			String source = idSchemaUtils.getSource(introducerBiometricsLabel, packetReaderService.getIdSchemaVersionFromPacket(registrationId));
			InputStream packetMetaInfoStream = packetReaderService.getFile(registrationId,
					introducerBiometricsFileName.toUpperCase(), source);

			byte[] introducerbiometric = IOUtils.toByteArray(packetMetaInfoStream);
			return validateUserBiometric(registrationId, introducerUIN, introducerbiometric,
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
		return introducerUIN == null && introducerRID == null;
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
	 * @param userbiometric
	 *            biometric data in byte array
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

	private boolean validateUserBiometric(String registrationId, String userId, byte[] userbiometric,
			String individualType, InternalRegistrationStatusDto registrationStatusDto)
			throws ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, IOException,
			ParserConfigurationException, SAXException, BiometricException, BioTypeException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, AuthSystemException {

		AuthResponseDTO authResponseDTO = authUtil.authByIdAuthentication(userId, individualType, userbiometric);
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
				.getRegistrationStatus(introducerRid);
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

	private boolean isActiveUserId(String registrationId, RegOsiDto regOsi, Identity identity,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean isValid = false;
		String creationDate = osiUtils.getMetaDataValue(JsonConstant.CREATIONDATE, identity);
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