package io.mosip.registration.processor.stages.cmdvalidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.HotlistRequestResponseDTO;
import io.mosip.registration.processor.core.packet.dto.JWTSignatureVerifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.JWTSignatureVerifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.NewDigitalId;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;

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

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Value("${mosip.regproc.cmd-validator.device.disable-trust-validation:false}")
	private Boolean disableTrustValidation;

	@Value("${mosip.regproc.cmd-validator.device.allowed-digital-id-timestamp-variation:5}")
	private int allowedDigitalIdTimestampVariation;

	@Value("${mosip.regproc.cmd-validator.device.digital-id-timestamp-format:yyyy-MM-dd'T'HH:mm:ss'Z'}")
	private String digitalIdTimestampFormat;

	@Value("#{T(java.util.Arrays).asList('${mosip.regproc.common.before-cbeff-others-attibute.reg-client-versions:}')}")
	private List<String> regClientVersionsBeforeCbeffOthersAttritube;

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
			throws JsonProcessingException, IOException, BaseCheckedException,
				ApisResourceAccessException, JSONException {
		List<String> fields = Arrays.asList(MappingJsonConstants.INDIVIDUAL_BIOMETRICS,
				MappingJsonConstants.AUTHENTICATION_BIOMETRICS,
				MappingJsonConstants.INTRODUCER_BIO,
				MappingJsonConstants.OFFICERBIOMETRICFILENAME,
				MappingJsonConstants.SUPERVISORBIOMETRICFILENAME);
		for (String field : fields) {
			String value = packetManagerService.getField(registrationId, field, process,
				ProviderStageName.PACKET_VALIDATOR);
			if (value != null && !value.isEmpty()) {
				BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
						registrationId, field, process,ProviderStageName.CMD_VALIDATOR);
				if(biometricRecord == null)
					throw new BaseCheckedException(
						StatusUtil.DEVICE_VALIDATION_FAILED.getCode(),
						StatusUtil.DEVICE_VALIDATION_FAILED.getMessage() +
							" --> Biometrics not found for field " + field);
				validateDevicesInBiometricRecord(biometricRecord, regOsi);
			}
		}
	}

	private void validateDevicesInBiometricRecord(BiometricRecord biometricRecord, RegOsiDto regOsi)
			throws JsonProcessingException, IOException, BaseCheckedException,
				ApisResourceAccessException, JSONException {
		List<BIR> birs = biometricRecord.getSegments();
		List<JSONObject> payloads = new ArrayList<>();
		for(BIR bir : birs) {
			if(bir.getOthers() != null) {
				boolean exception = false;
				String payload = "";
				for(Entry entry: bir.getOthers()) {
					if(entry.getKey().equals("EXCEPTION") && entry.getValue().equals("true") ) {
						exception = true;
						break;
					}
					if(entry.getKey().equals("PAYLOAD")) {
						payload = entry.getValue();
					}
				}
				if(!exception)
					payloads.add(new JSONObject(payload));
			} else if(!regClientVersionsBeforeCbeffOthersAttritube.contains(regOsi.getRegClientVersion())) {
				throw new BaseCheckedException(
					StatusUtil.DEVICE_VALIDATION_FAILED.getCode(),
					StatusUtil.DEVICE_VALIDATION_FAILED.getMessage() +
						"-->Others info is not prsent in packet");
			}
		}
		Set<String> signatures = new HashSet<>();
		Set<String> deviceCodeTimestamps = new HashSet<>();
		for(JSONObject payload : payloads) {
			String digitalIdString = new String(CryptoUtil.decodeBase64(
						payload.getString("digitalId").split("\\.")[1]));
			NewDigitalId newDigitalId = mapper.readValue(digitalIdString, NewDigitalId.class);
			if(!signatures.contains(digitalIdString)) {
				validateDigitalId(payload);
				signatures.add(digitalIdString);
			}
			signatures.add(digitalIdString);
			validateTimestamp(payload, regOsi.getPacketCreationDate(), newDigitalId.getDateTime());
			validateTimestamp(payload, regOsi.getPacketCreationDate(), payload.getString("timestamp"));
			if(!deviceCodeTimestamps.contains(payload.getString("deviceCode") + newDigitalId.getDateTime())) {
				validateDeviceForHotlist(payload.getString("deviceCode"), newDigitalId.getDateTime());
				deviceCodeTimestamps.add(payload.getString("deviceCode") + newDigitalId.getDateTime());
			}

		}
	}

	private void validateDeviceForHotlist(String deviceCode, String digitalIdTimestamp) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException, BaseCheckedException {
		List<String> pathSegments=new ArrayList<>();
		pathSegments.add("DEVICE");
		pathSegments.add(deviceCode);
		ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.getApi(ApiName.DEVICEHOTLIST, pathSegments,"", "", ResponseWrapper.class);
		if(responseWrapper.getResponse() !=null) {
			HotlistRequestResponseDTO hotListResponse=mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
					HotlistRequestResponseDTO.class);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(digitalIdTimestampFormat);

		LocalDateTime payloadTime = LocalDateTime.parse(digitalIdTimestamp, format);
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

		}
		else {
			throw new BaseCheckedException(
					responseWrapper.getErrors().get(0).getErrorCode(),
					responseWrapper.getErrors().get(0).getMessage());
		}
	}

	private void validateTimestamp(JSONObject payload, String packetCreationDate,String dateTime)
			throws JSONException, BaseCheckedException, JsonParseException, JsonMappingException, IOException {
		DateTimeFormatter packetCreationTimestampFormatter = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		DateTimeFormatter digitalIdTimestampFormatter = DateTimeFormatter.ofPattern(digitalIdTimestampFormat);
		LocalDateTime packetCreationDateTime = LocalDateTime
				.parse(packetCreationDate, packetCreationTimestampFormatter);
		LocalDateTime timestamp = LocalDateTime
				.parse(dateTime, digitalIdTimestampFormatter);

			if (timestamp.isAfter(packetCreationDateTime)|| timestamp.isBefore(
							packetCreationDateTime.minus(allowedDigitalIdTimestampVariation, ChronoUnit.MINUTES))) {
				throw new BaseCheckedException(
						StatusUtil.TIMESTAMP_NOT_VALID.getCode(),
						StatusUtil.TIMESTAMP_NOT_VALID.getMessage());
			}


	}

	private void validateDigitalId(JSONObject payload) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException, BaseCheckedException {
		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setApplicationId("REGISTRATION");
		jwtSignatureVerifyRequestDto.setReferenceId("SIGN");
		jwtSignatureVerifyRequestDto.setJwtSignatureData(payload.getString("digitalId"));
		jwtSignatureVerifyRequestDto.setActualData(payload.getString("digitalId").split("\\.")[1]);
		jwtSignatureVerifyRequestDto.setValidateTrust(!disableTrustValidation);
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

		if( !jwtResponse.isSignatureValid()) {
			throw new BaseCheckedException(
					StatusUtil.DEVICE_SIGNATURE_VALIDATION_FAILED.getCode(),StatusUtil.DEVICE_SIGNATURE_VALIDATION_FAILED.getMessage());
		}
		else {
		if(!disableTrustValidation && !jwtResponse.getTrustValid().contentEquals(SignatureConstant.TRUST_VALID)) {
			throw new BaseCheckedException(
					StatusUtil.DEVICE_SIGNATURE_VALIDATION_FAILED.getCode(),StatusUtil.DEVICE_SIGNATURE_VALIDATION_FAILED.getMessage()+"-->"+jwtResponse.getTrustValid());
		}
		}
		}
		else {
			throw new BaseCheckedException(
					responseWrapper.getErrors().get(0).getErrorCode(),
					responseWrapper.getErrors().get(0).getMessage());
		}

	}



}
