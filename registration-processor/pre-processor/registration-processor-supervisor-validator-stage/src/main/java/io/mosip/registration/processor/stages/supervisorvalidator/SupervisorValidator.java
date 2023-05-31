package io.mosip.registration.processor.stages.supervisorvalidator;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import io.mosip.registration.processor.packet.storage.utils.Utilities;
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
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
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
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.utils.BioSdkUtil;
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
	private ObjectMapper mapper;

	@Autowired
	private Utilities utility;

	@Autowired
	private BioSdkUtil bioUtil;

	/**
	 * Checks if is valid Supervisor.
	 *
	 * @param registrationId the registration id
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws Exception
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	public void validate(String registrationId, InternalRegistrationStatusDto registrationStatusDto, RegOsiDto regOsi)
			throws Exception {
		regProcLogger.debug("validate called for registrationId {}", registrationId);

		validateSupervisor(registrationId, regOsi, registrationStatusDto);
		authenticateSupervisor(regOsi, registrationId, registrationStatusDto);
		validateUMCmapping(regOsi.getPacketCreationDate(), regOsi.getRegcntrId(), regOsi.getMachineId(),
				regOsi.getSupervisorId(), registrationStatusDto);
		regProcLogger.debug("validate call ended for registrationId {}", registrationId);
	}

	private void validateSupervisor(String registrationId, RegOsiDto regOsi,
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
			throw new BaseCheckedException(StatusUtil.SUPERVISOR_PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getMessage(),
					StatusUtil.SUPERVISOR_PACKET_CREATION_DATE_NOT_FOUND_IN_PACKET.getCode());

		}
	}

	private boolean isActiveUser(String creationDate, String supervisorId,
			InternalRegistrationStatusDto registrationStatusDto) throws IOException, BaseCheckedException {
		boolean wasSupervisorActiveDuringPCT = false;

		if (supervisorId != null && !supervisorId.isEmpty()) {
			UserResponseDto supervisorResponse = getUserDetails(supervisorId, creationDate, registrationStatusDto);
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

	private UserResponseDto getUserDetails(String operatorId, String creationDate,
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
	 * To authenticate supervisor.
	 *
	 * @param regOsi                the reg osi
	 * @param registrationId        the registration id
	 * @param registrationStatusDto
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws Exception
	 */
	private void authenticateSupervisor(RegOsiDto regOsi, String registrationId,
			InternalRegistrationStatusDto registrationStatusDto) throws Exception {
		String supervisorId = regOsi.getSupervisorId();

		// officer password and otp check
		String supervisiorPassword = regOsi.getSupervisorHashedPwd();
		String supervisorOTP = regOsi.getSupervisorOTPAuthentication();

		String supervisorBiometricFileName = regOsi.getSupervisorBiometricFileName();

		if (StringUtils.isEmpty(supervisorBiometricFileName) || supervisorBiometricFileName == null) {
			if (!validateOtpAndPwd(supervisiorPassword, supervisorOTP)) {
				registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
						.getStatusCode(RegistrationExceptionTypeCode.SUPERVISOR_PASSWORD_OTP_FAILURE));
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				regProcLogger.debug("validateSupervisor call ended for registrationId {} {}", registrationId,
						StatusUtil.SUPERVISOR_PASSWORD_OTP_FAILURE.getMessage() + supervisorId);
				throw new ValidationFailedException(StatusUtil.SUPERVISOR_PASSWORD_OTP_FAILURE.getMessage() + supervisorId,
						StatusUtil.SUPERVISOR_PASSWORD_OTP_FAILURE.getCode());
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
	 * Validate opValidated and otpValidated.
	 *
	 * @param pwd the opValidated
	 * @param otp the otpValidated
	 * @return true, if successful
	 */
	private boolean validateOtpAndPwd(String opValidated, String otpValidated) {
		return (opValidated != null && opValidated.equals(ISTRUE) || otpValidated != null && otpValidated.equals(ISTRUE));
	}

	/**
	 * Validate user.
	 *
	 * @param userId                the userid
	 * @param registrationId        the registration id
	 * @param list                  biometric data as BIR object
	 * @param individualType        user type
	 * @param registrationStatusDto
	 * @throws Exception
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws BiometricException
	 */

	private void validateUserBiometric(String registrationId, String userId, List<BIR> list, String individualType,
			InternalRegistrationStatusDto registrationStatusDto)
			throws Exception {

		if (INDIVIDUAL_TYPE_USERID.equalsIgnoreCase(individualType)) {
			userId = getIndividualIdByUserId(userId);
			individualType = null;
		}

		bioUtil.authenticateBiometrics(userId, individualType, list, registrationStatusDto,
				StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getMessage(),
				StatusUtil.SUPERVISOR_AUTHENTICATION_FAILED.getCode());
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
					PlatformErrorMessages.LINK_FOR_USERID_INDIVIDUALID_FAILED_SVM_EXCEPTION.toString());
		} else {
			IndividualIdDto readValue = mapper.readValue(mapper.writeValueAsString(response.getResponse()),
					IndividualIdDto.class);
			individualId = readValue.getIndividualId();
		}
		regProcLogger.debug("getIndividualIdByUserId call ended for userid {}", userid);
		return individualId;
	}
	
	/**
	 * Validate UMC cmapping.
	 *
	 * @param effectiveTimestamp    the effective timestamp
	 * @param registrationCenterId  the registration center id
	 * @param machineId             the machine id
	 * @param superviserId          the superviser id
	 * @param registrationStatusDto
	 * @throws IOException
	 * @throws BaseCheckedException
	 */
	private void validateUMCmapping(String effectiveTimestamp, String registrationCenterId, String machineId,
			String supervisorId, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, BaseCheckedException {

		List<String> supervisorpathsegments = new ArrayList<>();
		supervisorpathsegments.add(effectiveTimestamp);
		supervisorpathsegments.add(registrationCenterId);
		supervisorpathsegments.add(machineId);
		supervisorpathsegments.add(supervisorId);

		if (!validateMapping(supervisorpathsegments, registrationStatusDto)) {
			throw new ValidationFailedException(StatusUtil.SUPERVISOR_NOT_ACTIVE.getMessage(),
					StatusUtil.SUPERVISOR_NOT_ACTIVE.getCode());
		}
	}

	private boolean validateMapping(List<String> pathsegments, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, BaseCheckedException {
		boolean isValidUser = false;
		ResponseWrapper<?> responseWrapper;
		RegistrationCenterUserMachineMappingHistoryResponseDto userDto = null;

		responseWrapper = (ResponseWrapper<?>) restClientService.getApi(ApiName.CENTERUSERMACHINEHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		userDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistrationCenterUserMachineMappingHistoryResponseDto.class);
		regProcLogger.debug("validateMapping call ended for registrationId {} with response data {}",
				registrationStatusDto.getRegistrationId(), JsonUtil.objectMapperObjectToJson(userDto));
		
		if (responseWrapper.getErrors() != null) {
			List<ErrorDTO> error = responseWrapper.getErrors();
			regProcLogger.debug("validateMapping call ended for registrationId {} with error data {}",
					registrationStatusDto.getRegistrationId(), error.get(0).getMessage());
			throw new BaseCheckedException(error.get(0).getMessage(),
					StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getCode());
		} else if (userDto != null) {
			userDto.setRegistrationCenters(userDto.getRegistrationCenters().stream().filter(u ->u!=null && u.getIsActive()).collect(Collectors.toList()));
			isValidUser = userDto.getRegistrationCenters()!=null && !userDto.getRegistrationCenters().isEmpty();
		} else {
			regProcLogger.debug(
					"validateMapping call ended with no erros and userDTO as null so considering as mapping not valid");
			isValidUser = false;
		}

		return isValidUser;
	}

}
