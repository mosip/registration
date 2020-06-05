package io.mosip.registration.test.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.RegisteredDeviceDAO;
import io.mosip.registration.dto.json.metadata.DigitalId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.BioDevice;
import io.mosip.registration.mdm.dto.CaptureResponsBioDataDto;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, Base64.class })
@PowerMockIgnore("javax.net.ssl.*")
public class MosipBioDeviceManagerTest {
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private MosipBioDeviceManager mosipBioDeviceManager;
	@Mock
	private IMosipBioDeviceIntegrator mosipBioDeviceIntegrator;
	@Mock
	private AuditManagerService auditManagerService;
	@Mock
	private RegisteredDeviceDAO registeredDeviceDAO;

	private BioDevice device = null;

	@Before
	public void beforeClass() {
		Map<String, Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.FINGER_PRINT_SCORE, 100);
		appMap.put("mosip.mdm.enabled", "Y");
		appMap.put("current_mdm_spec", "0.9.2");
		appMap.put("mosip.mdm.enabled", "Y");
		appMap.put(RegistrationConstants.MDM_HOST, "127.0.0.1");
		appMap.put(RegistrationConstants.MDM_HOST_PROTOCOL, "http");
		appMap.put(RegistrationConstants.MDM_START_PORT_RANGE, "8080");
		appMap.put(RegistrationConstants.MDM_END_PORT_RANGE, "8090");
		
		ApplicationContext.getInstance().setApplicationMap(appMap);
		mosipBioDeviceManager = PowerMockito.spy(mosipBioDeviceManager);
		device = PowerMockito.spy(new BioDevice());
		device.setSpecVersion(new String[] { "0.9.2" });
	}

	@Test
	public void init() throws RegBaseCheckedException, IOException {
	
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/info"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "FINGERPRINT");
		map.put("subType", "SLAP");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);

		Resource resource = new ClassPathResource("deviceInfo.txt");
		File file = resource.getFile();

		String response = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/info", Object[].class))
				.thenReturn(response);
		mosipBioDeviceManager.init();

	}

	@Test
	public void single() throws RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "FINGERPRINT");
		map.put("subType", "SINGLE");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void touchLess() throws RegBaseCheckedException {
	
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "FINGERPRINT");
		map.put("subType", "TOUCHLESS");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void face() throws RegBaseCheckedException {
	
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "FACE");
		map.put("subType", "TOUCHLESS");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void irisDouble() throws RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "IRIS");
		map.put("subType", "DOUBLE");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void iris() throws RegBaseCheckedException {
	
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "IRIS");
		map.put("subType", "SINGLE");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void vein() throws RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8080/deviceInfo"))
				.thenReturn(true);
		List<LinkedHashMap<String, Object>> deviceInfoResponseDtos = new ArrayList<>();

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("type", "VEIN");
		map.put("subType", "SINGLE");
		map.put("status", "RUNNING");
		map.put("deviceInfo", null);
		map.put("deviceInfoSignature", null);
		map.put("serviceVersion", null);
		map.put("callbackId", null);
		map.put("deviceSubId", null);

		deviceInfoResponseDtos.add(map);
		Mockito.when(mosipBioDeviceIntegrator.getDeviceInfo("http://127.0.0.1:8080/deviceInfo", Object[].class))
				.thenReturn(deviceInfoResponseDtos);
		mosipBioDeviceManager.init();

	}

	@Test
	public void getDeviceDiscovery() throws RegBaseCheckedException {
	
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito
				.when(RegistrationAppHealthCheckUtil.checkServiceAvailability("http://127.0.0.1:8082/deviceDiscovery"))
				.thenReturn(true);
		mosipBioDeviceManager.getDeviceDiscovery("deviceType");

	}

	@Test(expected = RegBaseCheckedException.class)
	public void scan() throws RegBaseCheckedException, IOException {
		Map<String, BioDevice> deviceRegistry = new HashMap<>();
		deviceRegistry.put("deviceType", device);
		ReflectionTestUtils.setField(MosipBioDeviceManager.class, "deviceRegistry", deviceRegistry);
		mosipBioDeviceManager.regScan(new RequestDetail("deviceType", "", 1, "", null));
	}

	@Test
	public void authScan() throws Exception {

		RequestDetail request = new RequestDetail("deviceType", "5000", 1, "60", null);
		Map<String, BioDevice> deviceRegistry = new HashMap<>();
		deviceRegistry.put("deviceType", device);
		ReflectionTestUtils.setField(MosipBioDeviceManager.class, "deviceRegistry", deviceRegistry);
		PowerMockito.doReturn(getFingerPritnCaptureResponse()).when(device, "regCapture", request);
		PowerMockito.doReturn(null).when(mosipBioDeviceManager, "stream", Mockito.anyString());
		mosipBioDeviceManager.authScan(request);
	}

	@Test
	public void getSingleBioExtract() {
		CaptureResponseDto captureResponseDto = new CaptureResponseDto();
		CaptureResponseBioDto captureResponseBioDto = new CaptureResponseBioDto();
		captureResponseBioDto.setCaptureResponseData(new CaptureResponsBioDataDto());
		captureResponseDto.setMosipBioDeviceDataResponses(Arrays.asList(captureResponseBioDto));
		captureResponseBioDto.getCaptureResponseData()
				.setBioValue(Base64.getUrlEncoder().encodeToString("sfdfs".getBytes()));
		mosipBioDeviceManager.getSingleBioValue(captureResponseDto);
	}

	private static CaptureResponseDto getFingerPritnCaptureResponse() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		StringBuffer sBuffer = new StringBuffer();
		Resource resource = new ClassPathResource("fingersData.txt");
		File file = resource.getFile();
		BufferedReader bR = new BufferedReader(new FileReader(file));
		String s;
		while ((s = bR.readLine()) != null) {
			sBuffer.append(s);
		}
		bR.close();
		CaptureResponseDto captureResponse = mapper.readValue(sBuffer.toString().getBytes(StandardCharsets.UTF_8),
				CaptureResponseDto.class);
		decode(captureResponse);

		return captureResponse;
	}

	private static void decode(CaptureResponseDto mosipBioCaptureResponseDto)
			throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		if (null != mosipBioCaptureResponseDto && null != mosipBioCaptureResponseDto.getMosipBioDeviceDataResponses()) {
			for (CaptureResponseBioDto captureResponseBioDto : mosipBioCaptureResponseDto
					.getMosipBioDeviceDataResponses()) {
				//
				if (null != captureResponseBioDto) {
					String bioJson = captureResponseBioDto.getCaptureBioData();
					if (null != bioJson) {
						CaptureResponsBioDataDto captureResponsBioDataDto = getCaptureResponsBioDataDecoded(bioJson,
								mapper);
						captureResponsBioDataDto.setDigitalIdDecoded(mapper.readValue(
								new String(Base64.getDecoder().decode(captureResponsBioDataDto.getDigitalId()))
										.getBytes(),
								DigitalId.class));
						captureResponseBioDto.setCaptureResponseData(captureResponsBioDataDto);
					}
				}
			}
		}
	}

	private static CaptureResponsBioDataDto getCaptureResponsBioDataDecoded(String capturedData, ObjectMapper mapper) {
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		Pattern pattern = Pattern.compile("(?<=\\.)(.*)(?=\\.)");
		Matcher matcher = pattern.matcher(capturedData);
		String afterMatch = null;
		if (matcher.find()) {
			afterMatch = matcher.group(1);
		}

		try {
			String result = new String(Base64.getDecoder().decode(afterMatch));
			return (CaptureResponsBioDataDto) (mapper.readValue(result.getBytes(), CaptureResponsBioDataDto.class));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	@Test
	public void extractSingleBiometricIsoTemplate() throws IOException {
		CaptureResponseDto captureResponseDto = getFingerPritnCaptureResponse();
		mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto);
	}

	@Test
	public void doRegister() {
		Map<String, BioDevice> deviceRegistry = new HashMap<>();
		deviceRegistry.put("deviceType", new BioDevice());
		mosipBioDeviceManager.deRegister("deviceType");
	}

	@Test
	public void getBioDeviceTest() {
		mosipBioDeviceManager.getBioDevice("type", "modality");
	}

	@Test
	public void registerTest() {
		mosipBioDeviceManager.register();
	}

	@Test
	public void refreshBioDeviceByDeviceTypeTest() throws RegBaseCheckedException {
		Map<String, BioDevice> deviceRegistry = new HashMap<>();
		deviceRegistry.put("deviceType", device);
		ReflectionTestUtils.setField(MosipBioDeviceManager.class, "deviceRegistry", deviceRegistry);
		mosipBioDeviceManager.refreshBioDeviceByDeviceType("deviceType");
	}

	@Test
	public void streamTest() throws Exception {
		mosipBioDeviceManager.stream(Mockito.anyString());
	}

}
