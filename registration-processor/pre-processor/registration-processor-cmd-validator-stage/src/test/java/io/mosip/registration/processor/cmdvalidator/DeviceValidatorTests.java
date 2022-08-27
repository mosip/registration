package io.mosip.registration.processor.cmdvalidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import org.assertj.core.util.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.HotlistRequestResponseDTO;
import io.mosip.registration.processor.core.packet.dto.JWTSignatureVerifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.stages.cmdvalidator.DeviceValidator;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class DeviceValidatorTests {
	@InjectMocks
	private DeviceValidator deviceValidator;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Mock
	private Environment env;

	@Mock
	private OSIUtils osiUtils;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private AdditionalInfoRequestService additionalInfoRequestService;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;
	RegOsiDto regOsi;String process; String registrationId; String regClientVersion;

	@Before
	public void setUp() throws Exception {
		process="NEW";
		registrationId="1234567890";
		regClientVersion = "1.2.0";
		regOsi=new RegOsiDto();
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");
		regOsi.setRegClientVersion(regClientVersion);
		ReflectionTestUtils.setField(deviceValidator, "allowedDigitalIdTimestampVariation", 30);
		ReflectionTestUtils.setField(deviceValidator, "disableTrustValidation", true);
		ReflectionTestUtils.setField(deviceValidator, "digitalIdTimestampFormat", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ReflectionTestUtils.setField(deviceValidator, "regClientVersionsBeforeCbeffOthersAttritube",
			new ArrayList<String>());

		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir=new BIR();
		HashMap<String, String> entry=new HashMap<String, String>();
		entry.put("PAYLOAD", "{\"deviceServiceVersion\":\"0.9.5\",\"bioValue\":\"<bioValue>\",\"qualityScore\":\"80\",\"bioType\":\"Iris\",\"requestedScore\":\"60\",\"transactionId\":\"4ca4fc4c-4279-47b4-9603-2954a038ccf9\",\"timestamp\":\"2021-06-04T07:31:59.831Z\",\"env\":\"null\",\"purpose\":\"Registration\",\"bioSubType\":\"Left\",\"digitalId\":\"eyJ4NWMiOlsiTUlJRGtEQ0NBbmlnQXdJQkFnSUVYM1hNZFRBTkJna3Foa2lHOXcwQkFRc0ZBRENCaVRFTE1Ba0dBMVVFQmhNQ1NVNHhFakFRQmdOVkJBZ01DV3RoY201aGRHRnJZVEVZTUJZR0ExVUVCd3dQYlc5emFYQnBjbWx6Wkc5MVlteGxNUmd3RmdZRFZRUUtEQTl0YjNOcGNHbHlhWE5rYjNWaWJHVXhHREFXQmdOVkJBc01EMjF2YzJsd2FYSnBjMlJ2ZFdKc1pURVlNQllHQTFVRUF3d1BiVzl6YVhCcGNtbHpaRzkxWW14bE1CNFhEVEl3TVRBd01URXlNekkxTTFvWERUSXhNVEF3TVRFeU16STFNMW93Z1lreEN6QUpCZ05WQkFZVEFrbE9NUkl3RUFZRFZRUUlEQWxyWVhKdVlYUmhhMkV4R0RBV0JnTlZCQWNNRDIxdmMybHdhWEpwYzJSdmRXSnNaVEVZTUJZR0ExVUVDZ3dQYlc5emFYQnBjbWx6Wkc5MVlteGxNUmd3RmdZRFZRUUxEQTl0YjNOcGNHbHlhWE5rYjNWaWJHVXhHREFXQmdOVkJBTU1EMjF2YzJsd2FYSnBjMlJ2ZFdKc1pUQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQU43YnZWOUJUMGZBWU1zWjk1Uzh3OGQ4ckFHMlRvT0dURHQzNW4wbEJXbExhR3hBVTFQbE02alMzZFBNeDZGaGRHcjBKOFV4N1kxMHhUck5DUzZTVnAvRHFKZHIrakJxclEvN3JSQitqc3k0WGJseXd6eFhnTHdEYXE1akVRMnRxV3FXZGdnMjl0Y0Q1NzFHQ0NXZnkvY0xrbkFwY3JtbGl1Z2ZvVU1GTGIvNVo1dVF5eTZrbnI4a21JQytGU3BaMStKd1BBUmxYQWhWZ1BsZFpoVCtvdTg1S0hBTUQ5TFJ6cmpZazFyYjNJUVZsejE0VTNORlArTzVhVzNjVWRYSytMN2VpVk1yc3ZhaG5PV3hiRGNWUDVxYWJyYTZQN3ppV3ljeVRCT3NLV2JqMmNPWk9Ta2VhRGtVUzFpMFdVVFhDNXBhOTFXcXlkTytYL2hRc3E0YlliTUNBd0VBQVRBTkJna3Foa2lHOXcwQkFRc0ZBQU9DQVFFQW05V1NzZEdrdmJ2YlJVT21nTjV2ZDlKZDZSNDZJYk1URlFHa3h4WVRmS0ZFM1ZwQ1cxc1hscXJyQ2NuTkVhM3l0TUJsZnBMblBDV2ZmT2VCL2NEVDRIZ3R5VERRSGg3aWtFYzNBdGNiejFjM3BiMjF5a0pzUStoVHNTSDR0ZUJ6VDlSL2VyQmxQWktyR09BNmJUZUpjV3hyVmNaOFh6R2tEWkZiYXp4ck1JV3E3dUc3QXFaSzhxL2NWTUtSb1RIWjIzNmk3MWIvb1dOVCtEbXNxQjNLUmNNUFFHTWtyanpWRk13RUszbGl2RDFlT2xzenJJdnBEbDJkR09wNHJ3eThaenBTKytQWkwyV0hXQVBXMkkzK0k0cmh4K1RaUTlYakVwSEJkRlMzS1dMcy9aVyttOWVLb3BGT1NINFpsWHIvMnlaQjh5RWlFSkE3U3NESjl1T0R3QT09Il0sImFsZyI6IlJTMjU2In0.eyJkZXZpY2VTdWJUeXBlIjoiRG91YmxlIiwic2VyaWFsTm8iOiIzNDU2Nzg5MDEyIiwibWFrZSI6Ik1PU0lQIiwibW9kZWwiOiJJUklTMDEiLCJkZXZpY2VQcm92aWRlcklkIjoiTU9TSVAuUFJPWFkuU0JJIiwiZGV2aWNlUHJvdmlkZXIiOiJNT1NJUCIsImRhdGVUaW1lIjoiMjAyMS0wNi0wNFQwNzozMTo1OS41NjFaIiwidHlwZSI6IklyaXMifQ.auo5Ej_opCIGdCxpqT17GqoEzZIBoTvjCGZvqMAYZoxdu9SMXcX-F7OtCTLomLJv3ArjGFP0gXxURjYS3TlXx5KG51nCfWiM3CIIT1R9_WLOObM4KSw2xGwktmSz04yE6_1n0Vs8v54T2gCugbSzOFYQag8MQ6erSLfuKju1G8Ypki54lXLWyeBSmuIeSxZZsZu6id9O23wpaBLNvshVpE4gb88Ig_rSSE0ENcQ7p9jjNxcbVG9ixb0i-s2zYIqqZvKSCIkdz9z7zKPK1VP7RmW_4COdB2taPvla5d6qNZHO0SfaBU53UyqYUKNsYkc4buOX_NgoRT7Yg_gR5lR4kQ\",\"deviceCode\":\"b692b595-3523-iris-99fc-bd76e35f190f\"}");
		bir.setOthers(entry);
		biometricRecord.setSegments(Arrays.asList(bir));

		Mockito.when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);
		Mockito.when(packetManagerService.getField(any(),
				any(), any(),any())).thenReturn("individualBiometrics_bio_CBEFF");
		HotlistRequestResponseDTO hotlistRequestResponseDTO=new HotlistRequestResponseDTO();
		hotlistRequestResponseDTO.setExpiryTimestamp(null);
		hotlistRequestResponseDTO.setId("b692b595-3523-iris-99fc-bd76e35f190f");
		hotlistRequestResponseDTO.setIdType("DEVICE");
		hotlistRequestResponseDTO.setStatus("UNBLOCKED");
		ResponseWrapper<HotlistRequestResponseDTO> ResponseWrapper=new ResponseWrapper<HotlistRequestResponseDTO>();
		ResponseWrapper.setResponse(hotlistRequestResponseDTO);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper);
		JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto=new JWTSignatureVerifyResponseDto();
		jwtSignatureVerifyResponseDto.setMessage("signature valid");
		jwtSignatureVerifyResponseDto.setSignatureValid(true);
		jwtSignatureVerifyResponseDto.setTrustValid("TRUST_CERT_PATH_VALID");
		ResponseWrapper<JWTSignatureVerifyResponseDto> ResponseWrapper1=new ResponseWrapper<JWTSignatureVerifyResponseDto>();
		ResponseWrapper1.setResponse(jwtSignatureVerifyResponseDto);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper1);
		Mockito.when(env.getProperty(anyString())).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoByRid(any())).thenReturn(null);
	}
	@Test
	public void deviceValidationTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationOthersNullTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir=new BIR();
		biometricRecord.setSegments(Arrays.asList(bir));

		Mockito.when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationdigitalIdFailedTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto=new JWTSignatureVerifyResponseDto();
		jwtSignatureVerifyResponseDto.setMessage("signature invalid");
		jwtSignatureVerifyResponseDto.setSignatureValid(false);
		jwtSignatureVerifyResponseDto.setTrustValid("TRUST_CERT_PATH_VALID");
		ResponseWrapper<JWTSignatureVerifyResponseDto> ResponseWrapper1=new ResponseWrapper<JWTSignatureVerifyResponseDto>();
		ResponseWrapper1.setResponse(jwtSignatureVerifyResponseDto);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper1);

		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationdigitalIdAPIResourceExceptionTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		ResponseWrapper<?> ResponseWrapper1=new ResponseWrapper();
		ErrorDTO errorDTO=new ErrorDTO();
		errorDTO.setErrorCode("");
		errorDTO.setMessage("");
		ResponseWrapper1.setErrors(Arrays.asList(errorDTO));
		ResponseWrapper1.setResponse(null);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper1);

		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationdigitalIdTimestampAfterCreationTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		regOsi.setPacketCreationDate("2021-06-05T07:31:59.831Z");
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationdigitalIdTimestampBeforeCreationTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		regOsi.setPacketCreationDate("2021-06-03T07:31:59.831Z");
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationDeviceHotlistTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		ResponseWrapper<HotlistRequestResponseDTO> ResponseWrapper=new ResponseWrapper<HotlistRequestResponseDTO>();
		ErrorDTO errorDTO=new ErrorDTO();
		errorDTO.setErrorCode("");
		errorDTO.setMessage("");
		ResponseWrapper.setErrors(Arrays.asList(errorDTO));
		ResponseWrapper.setResponse(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper);
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected=BaseCheckedException.class)
	public void deviceValidationDeviceHotlistBLOCKEDTest() throws JsonProcessingException, ApisResourceAccessException, IOException, BaseCheckedException, JSONException {
		HotlistRequestResponseDTO hotlistRequestResponseDTO=new HotlistRequestResponseDTO();
		hotlistRequestResponseDTO.setExpiryTimestamp(null);
		hotlistRequestResponseDTO.setId("b692b595-3523-iris-99fc-bd76e35f190f");
		hotlistRequestResponseDTO.setIdType("DEVICE");
		hotlistRequestResponseDTO.setStatus("BLOCKED");
		ResponseWrapper<HotlistRequestResponseDTO> ResponseWrapper=new ResponseWrapper<HotlistRequestResponseDTO>();
		ResponseWrapper.setResponse(hotlistRequestResponseDTO);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(ResponseWrapper);

		deviceValidator.validate(regOsi, process, registrationId);
	}

	// For backward compatibility of pre LTS reg client cbeff files
	@Test
	public void deviceValidationOthersNullButRegClientVersionBeforeCbeffOthersTest()
			throws JsonProcessingException, ApisResourceAccessException, IOException,
				BaseCheckedException, JSONException {
		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir=new BIR();
		biometricRecord.setSegments(Arrays.asList(bir));

		Mockito.when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);

		ReflectionTestUtils.setField(deviceValidator, "regClientVersionsBeforeCbeffOthersAttritube",
			Arrays.asList(regClientVersion));
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test
	public void deviceValidationAllSegmentAsException()
			throws JsonProcessingException, ApisResourceAccessException, IOException,
				BaseCheckedException, JSONException {
		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir1 = new BIR();
		HashMap<String, String> entry = new HashMap<>();
		entry.put("EXCEPTION", "true");
		bir1.setOthers(entry);
		BIR bir2 = new BIR();
		bir2.setOthers(entry);
		biometricRecord.setSegments(Arrays.asList(bir1,bir2));

		Mockito.when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);

		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test
	public void deviceValidationNoBiometricsInPacket()
			throws JsonProcessingException, ApisResourceAccessException, IOException,
				BaseCheckedException, JSONException {
		Mockito.when(packetManagerService.getField(any(),
				any(), any(),any())).thenReturn(null);
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test
	public void deviceValidationNoOfficerBiometricsInPacket() 
			throws JsonProcessingException, ApisResourceAccessException, IOException, 
				BaseCheckedException, JSONException {
		Map<String, String> metamap = new HashMap<>();
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject1 = new JSONObject();
		jsonObject1.put("officerBiometricFileName", JSONObject.NULL);
		metamap.put(JsonConstant.OPERATIONSDATA, jsonArray.toString());
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metamap);
		deviceValidator.validate(regOsi, process, registrationId);
	}

	@Test(expected = BaseCheckedException.class)
	public void testCorrectionPacketDatetimeValidation() throws JSONException, IOException, BaseCheckedException {
		AdditionalInfoRequestDto infoRequestDto = new AdditionalInfoRequestDto("", "",
				"", "BIOMETRIC_CORRECTION", 1, DateUtils.getUTCCurrentDateTime());

		regOsi.setPacketCreationDate("2021-03-04T07:31:59.831Z");

		Map<String, String> metamap = new HashMap<>();
		JSONObject jsonObject1 = new JSONObject();
		jsonObject1.put("creationDate", "2021-10-26T12:32:20.972Z");
		metamap.put("creationDate", "2021-10-26T12:32:20.972Z");
		RegOsiDto regOsiDto = new RegOsiDto();
		regOsiDto.setPacketCreationDate("2021-10-26T12:32:20.972Z");

		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metamap);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyMap())).thenReturn(regOsiDto);
		Mockito.when(additionalInfoRequestService.getAdditionalInfoByRid(any())).thenReturn(Lists.newArrayList(infoRequestDto));

		deviceValidator.validate(regOsi, process, registrationId);

	}
	
	@Test(expected = BaseCheckedException.class)
	public void deviceValidationOfficerBiometricsInPacket() throws JSONException, IOException, BaseCheckedException {
		Map<String, String> metamap = new HashMap<>();
		metamap.put(JsonConstant.OPERATIONSDATA, "[ {\n  \"label\" : \"officerId\",\n  \"value\" : \"5151\"\n},"
				+ " {\n  \"label\" : \"officerBiometricFileName\",\n  \"value\" : \"individualBiometrics\"\n}, "
				+ "{\n  \"label\" : \"supervisorId\",\n  \"value\" : \"5050\"\n}, "
				+ "{\n  \"label\" : \"supervisorBiometricFileName\",\n  \"value\" : null\n}, "
				+ "{\n  \"label\" : \"supervisorPassword\",\n  \"value\" : \"true\"\n}, "
				+ "{\n  \"label\" : \"officerPassword\",\n  \"value\" : \"true\"\n}, "
				+ "{\n  \"label\" : \"supervisorPIN\",\n  \"value\" : null\n}, "
				+ "{\n  \"label\" : \"officerPIN\",\n  \"value\" : null\n}, "
				+ "{\n  \"label\" : \"supervisorOTPAuthentication\",\n  \"value\" : \"false\"\n}, "
				+ "{\n  \"label\" : \"officerOTPAuthentication\",\n  \"value\" : \"false\"\n} ]");
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metamap);
		deviceValidator.validate(regOsi, process, registrationId);

	}
}
