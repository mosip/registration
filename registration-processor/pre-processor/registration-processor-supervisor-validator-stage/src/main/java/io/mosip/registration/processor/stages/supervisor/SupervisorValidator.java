package io.mosip.registration.processor.stages.supervisor;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.IndividualIdDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.ServerError;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

@Service
public class SupervisorValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(SupervisorValidator.class);

	private static final String ISTRUE = "true";

	private static final String INDIVIDUAL_TYPE_USERID = "USERID";

	private static final String APPID = "regproc";

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Autowired
	RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	private AuthUtil authUtil;

	@Autowired
	ObjectMapper mapper;

	/**
	 * Checks if is valid Supervisor.
	 *
	 * @param registrationId the registration id
	 * @throws IOException                                Signals that an I/O
	 *                                                    exception has occurred.
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NumberFormatException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws BaseCheckedException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	public void validate(String registrationId, InternalRegistrationStatusDto registrationStatusDto, RegOsiDto regOsi)
			throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NumberFormatException, JSONException,
			CertificateException, BaseCheckedException {
		regProcLogger.debug("validate called for registrationId {}", registrationId);

		ActiveUserId(registrationId, regOsi, registrationStatusDto);
		validateSupervisor(regOsi, registrationId, registrationStatusDto);
		regProcLogger.debug("validate call ended for registrationId {}", registrationId);
	}

	private void ActiveUserId(String registrationId, RegOsiDto regOsi,
			InternalRegistrationStatusDto registrationStatusDto) throws IOException, BaseCheckedException {
		String creationDate = regOsi.getPacketCreationDate();
		if (creationDate != null && !(StringUtils.isEmpty(creationDate))) {
			if (!isActiveUser(creationDate, regOsi.getSupervisorId(), registrationStatusDto)) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.SUPERVISOR_WAS_INACTIVE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("ActiveUserId call ended for registrationId {} {}", registrationId,
						StatusUtil.SUPERVISOR_WAS_INACTIVE.getMessage() + regOsi.getSupervisorId());
				throw new BaseCheckedException(
						StatusUtil.SUPERVISOR_WAS_INACTIVE.getMessage() + regOsi.getSupervisorId(),
						StatusUtil.SUPERVISOR_WAS_INACTIVE.getCode());
			}

		} else {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.PACKET_CREATION_DATE_NOT_PRESENT_IN_PACKET));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			regProcLogger.debug("ActiveUserId call ended for registrationId {}. packet creationDate is null",
					registrationId);
			throw new BaseCheckedException(StatusUtil.PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getMessage(),
					StatusUtil.PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getCode());

		}
	}

	private boolean isActiveUser(String creationDate, String supervisorId,
			InternalRegistrationStatusDto registrationStatusDto) throws IOException, BaseCheckedException {
		boolean wasSupervisorActiveDuringPCT = false;

		if (supervisorId != null && !supervisorId.isEmpty()) {
			UserResponseDto supervisorResponse = isUserActive(supervisorId, creationDate, registrationStatusDto);
			if (supervisorResponse.getErrors() == null) {
				wasSupervisorActiveDuringPCT = supervisorResponse.getResponse().getUserResponseDto().get(0)
						.getIsActive();
				if (!wasSupervisorActiveDuringPCT) {
					regProcLogger.debug("isActiveUser call ended for registrationId {} {}",
							registrationStatusDto.getRegistrationId(), StatusUtil.SUPERVISOR_WAS_INACTIVE.getMessage());
				}
			} else {
				List<ServerError> errors = supervisorResponse.getErrors();
				regProcLogger.debug("isActiveUser call ended with error {}", errors.get(0).getMessage());
				throw new BaseCheckedException(
						StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getMessage() + errors.get(0).getMessage(),
						StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getCode());
			}
		}
		return wasSupervisorActiveDuringPCT;
	}

	private UserResponseDto isUserActive(String operatorId, String creationDate,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		UserResponseDto userResponse;
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(operatorId);
		pathSegments.add(creationDate);

		userResponse = (UserResponseDto) restClientService.getApi(ApiName.USERDETAILS, pathSegments, "", "",
				UserResponseDto.class);
		regProcLogger.debug("isUserActive call ended with response data {}",
				JsonUtil.objectMapperObjectToJson(userResponse));

		return userResponse;
	}

	/**
	 * Checks if is valid supervisor.
	 *
	 * @param regOsi                the reg osi
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws BaseCheckedException
	 * @throws PacketDecryptionFailureException
	 * @throws Exception
	 */
	private void validateSupervisor(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto) throws IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, CertificateException, BaseCheckedException {
		String supervisorId = regOsi.getSupervisorId();

		// officer password and otp check
		String supervisiorPassword = regOsi.getSupervisorHashedPwd();
		String supervisorOTP = regOsi.getSupervisorOTPAuthentication();

		String supervisorBiometricFileName = regOsi.getSupervisorBiometricFileName();

		if (StringUtils.isEmpty(supervisorBiometricFileName) || supervisorBiometricFileName == null) {
			if (!validateOtpAndPwd(supervisiorPassword, supervisorOTP)) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.PASSWORD_OTP_FAILURE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("validateSupervisor call ended for registrationId {} {}", registrationId,
						StatusUtil.PASSWORD_OTP_FAILURE_SUPERVISOR.getMessage() + supervisorId);
				throw new ValidationFailedException(StatusUtil.PASSWORD_OTP_FAILURE_SUPERVISOR.getMessage() + supervisorId,
						StatusUtil.PASSWORD_OTP_FAILURE_SUPERVISOR.getCode());
			}
		} else {
			BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
					MappingJsonConstants.SUPERVISORBIOMETRICFILENAME, registrationStatusDto.getRegistrationType(),
					ProviderStageName.SUPERVISOR_VALIDATOR);

			if (biometricRecord == null || biometricRecord.getSegments() == null
					|| biometricRecord.getSegments().isEmpty()) {
				regProcLogger.error("validateSupervisor call ended for registrationId {} {}", registrationId,
						"ERROR =======>" + StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				throw new ValidationFailedException(
						StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getMessage() + " for Supervisor : " + supervisorId,
						StatusUtil.BIOMETRICS_VALIDATION_FAILURE.getCode());
			}
			validateUserBiometric(registrationId, supervisorId, biometricRecord.getSegments(), INDIVIDUAL_TYPE_USERID,
					registrationStatusDto);
		}

	}

	/**
	 * Validate otp and pwd.
	 *
	 * @param pwd the password
	 * @param otp the otp
	 * @return true, if successful
	 */
	boolean validateOtpAndPwd(String pwd, String otp) {
		return (pwd != null && pwd.equals(ISTRUE) || otp != null && otp.equals(ISTRUE));
	}

	/**
	 * Validate user.
	 *
	 * @param userId                the userid
	 * @param registrationId        the registration id
	 * @param list                  biometric data as BIR object
	 * @param individualType        user type
	 * @param registrationStatusDto
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws IOException                  Signals that an I/O exception has
	 *                                      occurred.
	 * @throws BaseCheckedException
	 * @throws BiometricException
	 */

	private void validateUserBiometric(String registrationId, String userId, List<BIR> list, String individualType,
			InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, CertificateException, NoSuchAlgorithmException, BaseCheckedException {

		if (INDIVIDUAL_TYPE_USERID.equalsIgnoreCase(individualType)) {
			userId = getIndividualIdByUserId(userId);
			individualType = null;
		}

		AuthResponseDTO authResponseDTO = authUtil.authByIdAuthentication(userId, individualType, list);
		if (authResponseDTO.getErrors() == null || authResponseDTO.getErrors().isEmpty()) {
			if (!authResponseDTO.getResponse().isAuthStatus()) {
				registrationStatusDto.setLatestTransactionStatusCode(
						registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.AUTH_FAILED));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				throw new ValidationFailedException(StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getMessage() + userId,
						StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getCode());
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
				regProcLogger.debug("validateUserBiometric call ended for registrationId {} {}", registrationId,
						result);
				throw new ValidationFailedException(result, StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getCode());
			}

		}

	}

	/**
	 * get the individualId by userid
	 * 
	 * @param userid
	 * @return individualId
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	private String getIndividualIdByUserId(String userid) throws ApisResourceAccessException, IOException {

		regProcLogger.debug("getIndividualIdByUserId called for userid {}", userid);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(APPID);
		pathSegments.add(userid);
		String individualId = null;
		ResponseWrapper<?> response = null;
		response = (ResponseWrapper<?>) restClientService.getApi(ApiName.GETINDIVIDUALIDFROMUSERID, pathSegments, "",
				"", ResponseWrapper.class);
		regProcLogger.debug(
				"getIndividualIdByUserId called for with GETINDIVIDUALIDFROMUSERID GET service call ended successfully");
		if (response.getErrors() != null) {
			throw new ApisResourceAccessException(
					PlatformErrorMessages.LINK_FOR_USERID_INDIVIDUALID_FAILED_OSI_EXCEPTION.toString());
		} else {
			IndividualIdDto readValue = mapper.readValue(mapper.writeValueAsString(response.getResponse()),
					IndividualIdDto.class);
			individualId = readValue.getIndividualId();
		}
		regProcLogger.debug("getIndividualIdByUserId call ended for userid {}", userid);
		return individualId;
	}

}
