package io.mosip.registration.processor.stages.cmdvalidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
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
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;

@Service
public class DeviceValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(DeviceValidator.class);

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String VALID = "Valid";

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	@Value("${mosip.kernel.device.validate.history.id}")
	private String deviceValidateHistoryId;

	/**
	 * Checks if is valid device.
	 *
	 * @param regOsi                the rcm dto
	 * @param registrationStatusDto
	 * @return true, if is valid device
	 * @throws IOException
	 * @throws BaseCheckedException, ApisResourceAccessException
	 */
	public boolean isValidDevice(RegOsiDto regOsi, String registrationId)
			throws IOException, BaseCheckedException, ApisResourceAccessException {
		boolean isValidDevice = false;
		if (isDeviceActive(regOsi, registrationId)) {
			isValidDevice = true;
		}
		return isValidDevice;
	}

	/**
	 * Checks if is device active.
	 *
	 * @param rcmDto                the rcm dto
	 * @param registrationStatusDto
	 * @return true, if is device active
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws BaseCheckedException,                               ApisResourceAccessException
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */

	private boolean isDeviceActive(RegOsiDto rcmDto, String registrationId)
			throws JsonProcessingException, IOException, BaseCheckedException, ApisResourceAccessException {
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
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"DeviceValidator::isDeviceActive()::DeviceValidate service ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(deviceValidateResponse));
				if (responseWrapper.getErrors() == null || responseWrapper.getErrors().isEmpty()) {
					if (deviceValidateResponse.getStatus().equalsIgnoreCase(VALID)) {
						isDeviceValid = true;
					} else {
						isDeviceValid = false;
						throw new BaseCheckedException(StatusUtil.DEVICE_VALIDATION_FAILED.getMessage(),
								StatusUtil.DEVICE_VALIDATION_FAILED.getCode());
					}

				} else {
					isDeviceValid = false;
					List<ErrorDTO> error = responseWrapper.getErrors();
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
							"DeviceValidator::isDeviceActive()::DEVICEVALIDATE service ended with error data : "
									+ error.get(0).getMessage());
					throw new BaseCheckedException(
							error.get(0).getMessage() + " " + "for" + " " + deviceDetails.getDigitalId().getType(),
							StatusUtil.DEVICE_VALIDATION_FAILED.getCode());

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
		RegisteredDevice registeredDevice = new RegisteredDevice(deviceDetails.getDeviceCode(),
				deviceDetails.getDeviceServiceVersion(), digitalId);
		return registeredDevice;
	}

}
