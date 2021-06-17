package io.mosip.registration.processor.stages.cmdvalidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.DigitalId;
import io.mosip.registration.processor.core.packet.dto.HotlistRequestResponseDTO;
import io.mosip.registration.processor.core.packet.dto.NewDigitalId;
import io.mosip.registration.processor.core.packet.dto.NewRegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.RegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryRequest;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryResponse;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;

@Component
public class DeviceValidator {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(DeviceValidator.class);

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String VALID = "Valid";

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;
	
	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Value("${mosip.kernel.device.validate.history.id}")
	private String deviceValidateHistoryId;
	@Value("${mosip.regproc.validate.trust:false}")
	private Boolean isTrustValidationRequired;
	
	@Value("${regproc.device.timestamp.validate:+5}")
	private String deviceTimeStamp;

	/**
	 * Checks if is device active.
	 *
	 * @param regOsi                the regOsi dto
	 * @param registrationStatusDto
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws BaseCheckedException,                               ApisResourceAccessException
	 * @throws JSONException 
	 * @throws com.fasterxml.jackson.databind.JsonMappingException
	 * @throws com.fasterxml.jackson.core.JsonParseException
	 */

	public void validate(RegOsiDto regOsi,String process, String registrationId)
			throws JsonProcessingException, IOException, BaseCheckedException, ApisResourceAccessException, JSONException {

		BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
				MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process,
				ProviderStageName.CMD_VALIDATOR);
		List<BIR> birs=biometricRecord.getSegments();
		List<JSONObject> payloads=new ArrayList<>();
		for(BIR bir: birs) {
			if(bir.getOthers()!=null) {
			for(Entry entry: bir.getOthers()) {
				if(entry.getKey().equals("PAYLOAD")) {
					payloads.add(new JSONObject(entry.getValue()));				
				}
			}
			}
		}
		if(!payloads.isEmpty()) {
		for(JSONObject payload :payloads) {
			if(!validateSignature(payload) ||
			!validateTimeStamp(payload,regOsi.getPacketCreationDate()) ||
			!isDeviceHotlisted(payload.getString("deviceCode"),payload.getString("timestamp"))
			) {
			throw new BaseCheckedException(
					StatusUtil.DEVICE_VALIDATION_FAILED.getCode(),StatusUtil.DEVICE_VALIDATION_FAILED.getMessage());
		}
		
		}
		}else {
			throw new BaseCheckedException(
					StatusUtil.DEVICE_VALIDATION_FAILED.getCode(),StatusUtil.DEVICE_VALIDATION_FAILED.getMessage()+"-->Others info is not prsent in packet");
		}
	}

	private boolean isDeviceHotlisted(String deviceCode, String payloadTimestamp) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException, BaseCheckedException {
		List<String> pathSegments=new ArrayList<>();
		pathSegments.add("DEVICE");
		pathSegments.add(deviceCode);
		ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.getApi(ApiName.HOTLIST, pathSegments,"", "", ResponseWrapper.class);
		if(responseWrapper.getResponse() !=null) {
			HotlistRequestResponseDTO hotListResponse=mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
					HotlistRequestResponseDTO.class);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		
		LocalDateTime payloadTime = LocalDateTime.parse(payloadTimestamp, format);
		if(hotListResponse.getExpiryTimestamp()!=null) {
		
		if(hotListResponse.getStatus().equalsIgnoreCase("BLOCKED") &&
				payloadTime.isBefore(hotListResponse.getExpiryTimestamp())) {
			throw new BaseCheckedException(
					StatusUtil.DEVICE_HOTLISTED.getCode(),
					StatusUtil.DEVICE_HOTLISTED.getMessage());
		}
		}
		else {
			if(hotListResponse.getStatus().equalsIgnoreCase("BLOCKED")) {
				throw new BaseCheckedException(
						StatusUtil.DEVICE_HOTLISTED.getCode(),
						StatusUtil.DEVICE_HOTLISTED.getMessage());
			}
		}
		 return true;
		}
		else {
			throw new BaseCheckedException(
					responseWrapper.getErrors().get(0).getErrorCode(),
					responseWrapper.getErrors().get(0).getMessage());
		}
	}

	private boolean validateTimeStamp(JSONObject payload, String packetCreationDate) throws JSONException, BaseCheckedException {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime packetCreationDateTime = LocalDateTime
				.parse(packetCreationDate, format);
		LocalDateTime payloadTimestamp = LocalDateTime
				.parse(payload.getString("timestamp"), format);
		
		String prefix = deviceTimeStamp.substring(0, 1);
		String timeString = deviceTimeStamp.replaceAll("\\" + prefix, "");
		boolean isBetween = payloadTimestamp
				.isAfter(packetCreationDateTime.minus(Long.valueOf("2"), ChronoUnit.MINUTES))
				&& payloadTimestamp.isBefore(
						packetCreationDateTime.plus(Long.valueOf(timeString), ChronoUnit.MINUTES));
		if (prefix.equals("+")) {
			if (!isBetween) {
				throw new BaseCheckedException(
						StatusUtil.TIMESTAMP_AFTER_PACKETTIME.getCode(),
						String.format(StatusUtil.TIMESTAMP_AFTER_PACKETTIME.getMessage(),
								timeString));
			}
		} else if (prefix.equals("-")) {
			if (packetCreationDateTime
					.isBefore(payloadTimestamp.plus(Long.valueOf(timeString), ChronoUnit.MINUTES))) {
				throw new BaseCheckedException(
						StatusUtil.TIMESTAMP_BEFORE_PACKETTIME.getCode(),
						String.format(StatusUtil.TIMESTAMP_BEFORE_PACKETTIME.getMessage(),
								timeString));
			}
		}
		return true;
	}

	private boolean validateSignature(JSONObject payload) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException, BaseCheckedException {
		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setApplicationId("REGISTRATION");
		jwtSignatureVerifyRequestDto.setReferenceId("SIGN");
		jwtSignatureVerifyRequestDto.setJwtSignatureData(payload.getString("digitalId"));
		jwtSignatureVerifyRequestDto.setValidateTrust(isTrustValidationRequired);
		jwtSignatureVerifyRequestDto.setDomain("Device");
		RequestWrapper<JWTSignatureVerifyRequestDto> request = new RequestWrapper<>();

		request.setRequest(jwtSignatureVerifyRequestDto);
		request.setVersion("1.0");
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		request.setRequesttime(localdatetime);
		ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.postApi(ApiName.JWTVERIFY, "", "", request, ResponseWrapper.class);
		if(responseWrapper.getResponse() !=null) {
		JWTSignatureVerifyResponseDto jwtResponse = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				JWTSignatureVerifyResponseDto.class);
				
		return isTrustValidationRequired
				? jwtResponse.isSignatureValid()
						&& jwtResponse.getTrustValid().contentEquals(SignatureConstant.TRUST_VALID)
				: jwtResponse.isSignatureValid();
		}
		else {
			throw new BaseCheckedException(
					responseWrapper.getErrors().get(0).getErrorCode(),
					responseWrapper.getErrors().get(0).getMessage());
		}
		
	}

	

}
