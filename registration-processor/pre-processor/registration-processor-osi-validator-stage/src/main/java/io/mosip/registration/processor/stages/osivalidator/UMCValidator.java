package io.mosip.registration.processor.stages.osivalidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.DigitalId;
import io.mosip.registration.processor.core.packet.dto.NewDigitalId;
import io.mosip.registration.processor.core.packet.dto.NewRegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.RegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryRequest;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryResponse;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistartionCenterTimestampResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDeviceHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDeviceHistoryResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.stages.osivalidator.utils.OSIUtils;
import io.mosip.registration.processor.stages.osivalidator.utils.StatusMessage;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Class UMCValidator.
 *
 * @author Jyothi
 * @author Ranjitha Siddegowda
 */
@Service
public class UMCValidator {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UMCValidator.class);

	/** The response from masterdata validate api. */
	private static final String VALID = "Valid";

	public static final String GLOBAL_CONFIG_TRUE_VALUE = "Y";

	/** The umc client. */

	@Value("${mosip.workinghour.validation.required}")
	private Boolean isWorkingHourValidationRequired;

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private OSIUtils osiUtils;

	/** The primary languagecode. */
	@Value("${mosip.primary-language}")
	private String primaryLanguagecode;

	@Value("${mosip.kernel.device.validate.history.id}")
	private String deviceValidateHistoryId;

	@Value("${mosip.registration.gps_device_enable_flag}")
	private String gpsEnable;

	/** The identity iterator util. */
	IdentityIteratorUtil identityIteratorUtil = new IdentityIteratorUtil();

	ObjectMapper mapper = new ObjectMapper();

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Autowired
	private Environment env;

	/**
	 * Validate registration center.
	 *
	 * @param registrationCenterId
	 *            the registration center id
	 * @param langCode
	 *            the lang code
	 * @param effectiveDate
	 *            the effective date
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */
	@SuppressWarnings("unchecked")
	private boolean isValidRegistrationCenter(String registrationCenterId, String langCode, String effectiveDate,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean activeRegCenter = false;
		if (registrationCenterId == null || effectiveDate == null) {
			registrationStatusDto.setStatusComment(StatusUtil.CENTER_ID_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.CENTER_ID_NOT_FOUND.getCode());
			return false;
		}
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(registrationCenterId);
		pathsegments.add(langCode);
		pathsegments.add(effectiveDate);
		RegistrationCenterResponseDto rcpdto = null;
		ResponseWrapper<?> responseWrapper;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.CENTERHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		rcpdto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistrationCenterResponseDto.class);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"UMCValidator::isValidRegistrationCenter()::CenterHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(rcpdto));

		if (responseWrapper.getErrors() == null) {
			activeRegCenter = rcpdto.getRegistrationCentersHistory().get(0).getIsActive();
			if (!activeRegCenter) {
				registrationStatusDto
						.setStatusComment(StatusUtil.CENTER_ID_INACTIVE.getMessage() + registrationCenterId);
				registrationStatusDto.setSubStatusCode(StatusUtil.CENTER_ID_INACTIVE.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			registrationStatusDto.setStatusComment(error.get(0).getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.FAILED_TO_GET_CENTER_DETAIL.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					"UMCValidator::isValidRegistrationCenter()::CenterHistory service ended with response data : "
							+ error.get(0).getMessage());

		}

		return activeRegCenter;

	}

	/**
	 * Validate machine.
	 *
	 * @param machineId
	 *            the machine id
	 * @param langCode
	 *            the lang code
	 * @param effdatetimes
	 *            the effdatetimes
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */
	@SuppressWarnings("unchecked")
	private boolean isValidMachine(String machineId, String langCode, String effdatetimes,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {

		boolean isActiveMachine = false;
		if (machineId == null) {
			registrationStatusDto.setStatusComment(StatusUtil.MACHINE_ID_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.MACHINE_ID_NOT_FOUND.getCode());
			return false;
		}

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(machineId);
		pathsegments.add(langCode);
		pathsegments.add(effdatetimes);
		MachineHistoryResponseDto mhrdto;
		ResponseWrapper<?> responseWrapper;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		mhrdto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				MachineHistoryResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"UMCValidator::isValidMachine()::MachineHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(mhrdto));
		if (responseWrapper.getErrors() == null) {
			MachineHistoryDto dto = mhrdto.getMachineHistoryDetails().get(0);

			if (dto.getId() != null && dto.getId().matches(machineId)) {
				isActiveMachine = dto.getIsActive();
				if (!isActiveMachine) {
					registrationStatusDto.setStatusComment(StatusUtil.MACHINE_ID_NOT_ACTIVE.getMessage() + machineId);
					registrationStatusDto.setSubStatusCode(StatusUtil.MACHINE_ID_NOT_ACTIVE.getCode());
				}

			} else {
				registrationStatusDto.setStatusComment(StatusMessage.MACHINE_ID_NOT_FOUND);
				registrationStatusDto.setSubStatusCode(StatusUtil.MACHINE_ID_NOT_FOUND_MASTER_DB.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					"UMCValidator::isValidMachine()::MachineHistory service ended with response data : "
							+ error.get(0).getMessage());
			registrationStatusDto.setStatusComment(error.get(0).getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.FAILED_TO_GET_MACHINE_DETAIL.getCode());
		}

		return isActiveMachine;

	}

	/**
	 * Validate UM cmapping.
	 *
	 * @param effectiveTimestamp
	 *            the effective timestamp
	 * @param registrationCenterId
	 *            the registration center id
	 * @param machineId
	 *            the machine id
	 * @param superviserId
	 *            the superviser id
	 * @param officerId
	 *            the officer id
	 * @param registrationStatusDto
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	private boolean isValidUMCmapping(String effectiveTimestamp, String registrationCenterId, String machineId,
			String superviserId, String officerId, InternalRegistrationStatusDto registrationStatusDto)
			throws ApisResourceAccessException, IOException {

		boolean supervisorActive = false;
		boolean officerActive = false;
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(effectiveTimestamp);
		pathsegments.add(registrationCenterId);
		pathsegments.add(machineId);
		pathsegments.add(superviserId);
		RegistrationCenterUserMachineMappingHistoryResponseDto supervisordto;
		if (superviserId != null)
			supervisorActive = validateMapping(pathsegments, registrationStatusDto);

		if (!supervisorActive) {
			List<String> officerpathsegments = new ArrayList<>();
			officerpathsegments.add(effectiveTimestamp);
			officerpathsegments.add(registrationCenterId);
			officerpathsegments.add(machineId);
			officerpathsegments.add(officerId);
			if (officerId != null)
				officerActive = validateMapping(officerpathsegments, registrationStatusDto);

		}
		if (!supervisorActive && !officerActive) {
			registrationStatusDto.setStatusComment(StatusUtil.SUPERVISOR_OFFICER_NOT_ACTIVE.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.SUPERVISOR_OFFICER_NOT_ACTIVE.getCode());
		}
		return supervisorActive || officerActive;
	}

	private boolean validateMapping(List<String> pathsegments, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException {
		boolean isValidUser = false;
		ResponseWrapper<?> responseWrapper;
		RegistrationCenterUserMachineMappingHistoryResponseDto userDto = null;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY,
				pathsegments, "", "", ResponseWrapper.class);
		userDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistrationCenterUserMachineMappingHistoryResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"UMCValidator::validateMapping()::CenterUserMachineHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(userDto));
		if (userDto != null) {
			if (responseWrapper.getErrors() == null) {
				isValidUser = userDto.getRegistrationCenters().get(0).getIsActive();
			} else {
				List<ErrorDTO> error = responseWrapper.getErrors();
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						"UMCValidator::validateMapping()::CenterUserMachineHistory service ended with response data : "
								+ error.get(0).getMessage());
				registrationStatusDto.setStatusComment(error.get(0).getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getCode());
			}
		}

		return isValidUser;
	}

	/**
	 * Check not null.
	 *
	 * @param validatorDtos
	 *            the validator dtos
	 * @return true, if successful
	 */
	boolean checkNotNull(List<RegistrationCenterUserMachineMappingHistoryDto> validatorDtos) {
		return (validatorDtos != null && !validatorDtos.isEmpty());
	}

	/**
	 * Check null.
	 *
	 * @param validatorDtos
	 *            the validator dtos
	 * @return true, if successful
	 */
	boolean checkNull(List<RegistrationCenterUserMachineMappingHistoryDto> validatorDtos) {
		return (validatorDtos == null || validatorDtos.isEmpty());
	}

	/**
	 * Checks if is valid UMC.
	 *
	 * @param registrationId         the registration id
	 * @param registrationStatusDto
	 * @return true, if is valid UMC
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws IOException
	 * @throws                                  io.mosip.kernel.core.exception.IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws PacketDecryptionFailureException
	 */
	public boolean isValidUMC(String registrationId, InternalRegistrationStatusDto registrationStatusDto, Map<String, String> metaInfo)
			throws ApisResourceAccessException, IOException, PacketManagerException, JSONException, io.mosip.kernel.core.util.exception.JsonProcessingException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "UMCValidator::isValidUMC()::entry");

		RegOsiDto regOsi = osiUtils.getOSIDetailsFromMetaInfo(metaInfo);
		boolean umc = false;

		if ((gpsEnable.equalsIgnoreCase(GLOBAL_CONFIG_TRUE_VALUE))
				&& (regOsi.getLatitude() == null
				|| regOsi.getLongitude() == null
				|| regOsi.getLatitude().trim().isEmpty()
				|| regOsi.getLongitude().trim().isEmpty())) {
			registrationStatusDto.setStatusComment(StatusUtil.GPS_DETAILS_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.GPS_DETAILS_NOT_FOUND.getCode());
		}

		else if (isValidCenterDetails(registrationStatusDto, regOsi))
			umc = true;
		else if (isValidMachinDetails(registrationStatusDto, regOsi))
			umc = true;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "UMCValidator::isValidUMC()::exit");
		return umc;
	}

	private boolean isValidMachinDetails(InternalRegistrationStatusDto registrationStatusDto, RegOsiDto regOsi)
			throws ApisResourceAccessException, IOException {
		return isValidRegistrationCenter(regOsi.getRegcntrId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
				registrationStatusDto)
				&& isValidMachine(regOsi.getMachineId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
						registrationStatusDto)
				&& isValidUMCmapping(regOsi.getPacketCreationDate(), regOsi.getRegcntrId(), regOsi.getMachineId(),
						regOsi.getSupervisorId(), regOsi.getOfficerId(), registrationStatusDto)
				&& isValidDevice(regOsi, registrationStatusDto);
	}

	private boolean isValidCenterDetails(InternalRegistrationStatusDto registrationStatusDto, RegOsiDto regOsi)
			throws ApisResourceAccessException, IOException {
		return isWorkingHourValidationRequired
				&& isValidRegistrationCenter(regOsi.getRegcntrId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
						registrationStatusDto)
				&& isValidMachine(regOsi.getMachineId(), primaryLanguagecode, regOsi.getPacketCreationDate(),
						registrationStatusDto)
				&& isValidUMCmapping(regOsi.getPacketCreationDate(), regOsi.getRegcntrId(), regOsi.getMachineId(),
						regOsi.getSupervisorId(), regOsi.getOfficerId(), registrationStatusDto)
				&& validateCenterIdAndTimestamp(regOsi, registrationStatusDto)
				&& isValidDevice(regOsi, registrationStatusDto);
	}

	/**
	 * Checks if is valid device.
	 *
	 * @param regOsi
	 *            the rcm dto
	 * @param registrationStatusDto
	 * @return true, if is valid device
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	private boolean isValidDevice(RegOsiDto regOsi,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean isValidDevice = false;
		if (isDeviceActive(regOsi, registrationStatusDto) && isDeviceMappedWithCenter(regOsi, registrationStatusDto)) {
			isValidDevice = true;
		}
		return isValidDevice;
	}

	/**
	 * Checks if is device mapped with center.
	 *
	 * @param rcmDto
	 *            the rcm dto
	 * @param registrationStatusDto
	 * @return true, if is device mapped with center
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */

	private boolean isDeviceMappedWithCenter(RegOsiDto rcmDto,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean isDeviceMappedWithCenter = false;
		List<NewRegisteredDevice> registreredDevices = rcmDto.getCapturedRegisteredDevices();
		if (registreredDevices != null && !registreredDevices.isEmpty()) {
			for (NewRegisteredDevice deviceDetails : registreredDevices) {
				String deviceCode = null;
				deviceCode = deviceDetails.getDeviceCode();
				RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto;

				List<String> pathsegments = new ArrayList<>();
				pathsegments.add(rcmDto.getRegcntrId());
				pathsegments.add(deviceCode);
				pathsegments.add(rcmDto.getPacketCreationDate());

				ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
						.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments, "", "", ResponseWrapper.class);
				registrationCenterDeviceHistoryResponseDto = mapper.readValue(
						mapper.writeValueAsString(responseWrapper.getResponse()),
						RegistrationCenterDeviceHistoryResponseDto.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						"UMCValidator::isDeviceMappedWithCenter()::CenterUserMachineHistory service ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(registrationCenterDeviceHistoryResponseDto));
				if (responseWrapper.getErrors() == null) {
					isDeviceMappedWithCenter = validateDeviceMappedWithCenterResponse(
							registrationCenterDeviceHistoryResponseDto, deviceCode, rcmDto.getRegcntrId(),
							rcmDto.getRegId(), registrationStatusDto);
					if (!isDeviceMappedWithCenter) {
						registrationStatusDto.setStatusComment(StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getMessage()
								+ rcmDto.getRegcntrId() + deviceCode);
						registrationStatusDto.setSubStatusCode(StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getCode());
						break;
					}
				} else {
					isDeviceMappedWithCenter = false;
					List<ErrorDTO> error = responseWrapper.getErrors();
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
							"UMCValidator::isDeviceMappedWithCenter()::CenterUserMachineHistory service ended with response data : "
									+ error.get(0).getMessage());
					registrationStatusDto.setStatusComment(
							error.get(0).getMessage() + " " + "for" + " " + deviceDetails.getDigitalId().getType());
					registrationStatusDto.setSubStatusCode(StatusUtil.CENTER_DEVICE_MAPPING_NOT_FOUND.getCode());
					break;
				}

			}
		} else {
			isDeviceMappedWithCenter = true;
		}
		return isDeviceMappedWithCenter;
	}

	/**
	 * Validate device mapped with center response.
	 *
	 * @param registrationCenterDeviceHistoryResponseDto
	 *            the registration center device history response dto
	 * @param registrationStatusDto
	 * @return true, if successful
	 */
	private boolean validateDeviceMappedWithCenterResponse(
			RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto, String deviceId,
			String centerId, String regId, InternalRegistrationStatusDto registrationStatusDto) {
		boolean isDeviceMappedWithCenter = false;
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDto = registrationCenterDeviceHistoryResponseDto
				.getRegistrationCenterDeviceHistoryDetails();

		if (registrationCenterDeviceHistoryDto.getIsActive()) {
			isDeviceMappedWithCenter = true;
		} else {
			registrationStatusDto.setStatusComment(
					StatusUtil.CENTER_DEVICE_MAPPING_INACTIVE.getMessage() + centerId + " -" + deviceId);
			registrationStatusDto.setStatusComment(StatusUtil.CENTER_DEVICE_MAPPING_INACTIVE.getCode());
		}

		return isDeviceMappedWithCenter;

	}

	/**
	 * Checks if is device active.
	 *
	 * @param rcmDto
	 *            the rcm dto
	 * @param registrationStatusDto
	 * @return true, if is device active
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */

	private boolean isDeviceActive(RegOsiDto rcmDto,
			InternalRegistrationStatusDto registrationStatusDto)
			throws JsonProcessingException, IOException, ApisResourceAccessException {
		boolean isDeviceValid = false;

		List<NewRegisteredDevice> registreredDevices = rcmDto.getCapturedRegisteredDevices();
		if (registreredDevices != null && !registreredDevices.isEmpty()) {
			for (NewRegisteredDevice deviceDetails : registreredDevices) {
				RegisteredDevice registeredDevice = convert(deviceDetails);
				DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
				deviceValidateHistoryRequest.setDeviceCode(registeredDevice.getDeviceCode());
				deviceValidateHistoryRequest.setDeviceServiceVersion(registeredDevice.getDeviceServiceVersion());
				deviceValidateHistoryRequest.setDigitalId(registeredDevice.getDigitalId());
				deviceValidateHistoryRequest.setTimeStamp(rcmDto.getPacketCreationDate());
				RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

				request.setRequest(deviceValidateHistoryRequest);
				request.setId(deviceValidateHistoryId);
				request.setMetadata(null);
				request.setVersion("1.0");
				DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
				request.setRequesttime(localdatetime);

				DeviceValidateHistoryResponse deviceValidateResponse;

				ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
						.postApi(ApiName.DEVICEVALIDATEHISTORY, "", "", request, ResponseWrapper.class);
				deviceValidateResponse = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
						DeviceValidateHistoryResponse.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
						"UMCValidator::isDeviceActive()::DeviceValidate service ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(deviceValidateResponse));
				if (responseWrapper.getErrors() == null || responseWrapper.getErrors().isEmpty()) {
					if (deviceValidateResponse.getStatus().equalsIgnoreCase(VALID)) {
						isDeviceValid = true;
					} else {
						isDeviceValid = false;
						registrationStatusDto.setSubStatusCode(StatusUtil.DEVICE_VALIDATION_FAILED.getMessage());

					}

				} else {
					isDeviceValid = false;
					List<ErrorDTO> error = responseWrapper.getErrors();
					registrationStatusDto.setStatusComment(error.get(0).getMessage()+ " " +"for" +" " +deviceDetails.getDigitalId().getType());
					registrationStatusDto.setSubStatusCode(StatusUtil.DEVICE_VALIDATION_FAILED.getCode());
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationStatusDto.getRegistrationId(),
							"UMCValidator::isDeviceActive()::DEVICEVALIDATE service ended with error data : "
									+ error.get(0).getMessage());

				}
				if (!isDeviceValid) {
					break;
				}

			}

		} else {
			isDeviceValid = true;
		}
		return isDeviceValid;
	}

	/**
	 * Converts new registered device to masterdata registered device
	 *
	 * @param deviceDetails
	 * @return
	 */
	private RegisteredDevice convert(NewRegisteredDevice deviceDetails) {
		DigitalId digitalId = new DigitalId();
		NewDigitalId newDigitalId = deviceDetails.getDigitalId();
		if (newDigitalId != null) {
			digitalId.setDateTime(newDigitalId.getDateTime());
			digitalId.setDeviceSubType(newDigitalId.getDeviceSubType());
			digitalId.setDp(newDigitalId.getDeviceProvider());
			digitalId.setDpId(newDigitalId.getDeviceProviderId());
			digitalId.setMake(newDigitalId.getMake());
			digitalId.setModel(newDigitalId.getModel());
			digitalId.setSerialNo(newDigitalId.getSerialNo());
			digitalId.setType(newDigitalId.getType());
		}
		RegisteredDevice registeredDevice = new RegisteredDevice(deviceDetails.getDeviceCode(), deviceDetails.getDeviceServiceVersion(), digitalId);
		return registeredDevice;
	}

	/**
	 * Checks if is valid center id timestamp.
	 * 
	 * @param registrationStatusDto
	 *
	 * @param rcmDto
	 *            the RegistrationCenterMachineDto
	 * @param registrationStatusDto
	 *            the InternalRegistrationStatusDto
	 * @return true, if is valid center id timestamp
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 *
	 */

	private boolean validateCenterIdAndTimestamp(RegOsiDto rcmDto,
			InternalRegistrationStatusDto registrationStatusDto) throws ApisResourceAccessException, IOException {
		boolean isValid = false;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				rcmDto.getRegId(), "UMCValidator::validateCenterIdAndTimestamp()::entry");
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(rcmDto.getRegcntrId());
		pathsegments.add(primaryLanguagecode);
		pathsegments.add(rcmDto.getPacketCreationDate());
		ResponseWrapper<?> responseWrapper;
		RegistartionCenterTimestampResponseDto result;

		responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments, "", "", ResponseWrapper.class);

		result = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				RegistartionCenterTimestampResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationStatusDto.getRegistrationId(),
				"UMCValidator::isDeviceActive()::CenterUserMachineHistory service ended with response data : "
						+ JsonUtil.objectMapperObjectToJson(result));
		if (responseWrapper.getErrors() == null) {
			if (result.getStatus().equals(VALID)) {
				isValid = true;
			} else {
				registrationStatusDto.setStatusComment(
						StatusUtil.PACKET_CREATION_WORKING_HOURS.getMessage() + rcmDto.getPacketCreationDate());
				registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_CREATION_WORKING_HOURS.getCode());
			}
		} else {
			List<ErrorDTO> error = responseWrapper.getErrors();
			registrationStatusDto.setStatusComment(error.get(0).getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.REGISTRATION_CENTER_TIMESTAMP_FAILURE.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					"UMCValidator::isDeviceActive()::CenterUserMachineHistory service ended with response data : "
							+ error.get(0).getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				rcmDto.getRegId(), "UMCValidator::validateCenterIdAndTimestamp()::exit");
		return isValid;
	}
}