package io.mosip.registration.bio.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinezoo.sourceafis.FingerprintTemplate;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.device.fp.FingerprintProvider;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.dto.json.metadata.DigitalId;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.id.UserBiometricId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.mdm.dto.CaptureResponsBioDataDto;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.service.bio.impl.BioServiceImpl;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.test.util.datastub.DataProvider;
import javafx.scene.image.Image;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ImageIO.class, IOUtils.class, FingerprintTemplate.class, SessionContext.class })
public class BioServiceTest {

	@InjectMocks
	private BioServiceImpl bioService;

	@Mock
	MosipBioDeviceManager mosipBioDeviceManager;

	@Mock
	private AuthenticationService authService;

	@Mock
	private FingerprintProvider fingerprintProvider;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	CaptureResponseDto fingerPritnCaptureResponse;
	
	CaptureResponseDto irisCaptureResponse;
	
	private RequestDetail requestDetail;

	FingerprintDetailsDTO fingerPrints;
	
	@Before
	public void beforeClass() throws RegBaseCheckedException, IOException {
		requestDetail= PowerMockito.spy(new RequestDetail("type", "timeout", 1, "60", null));
		RegistrationDTO registrationDTO = DataProvider.getPacketDTO();
		BiometricInfoDTO biometricInfoDTO = new BiometricInfoDTO();
		List<BiometricExceptionDTO> biometricExceptionDTOs = new ArrayList<>();
		biometricInfoDTO.setBiometricExceptionDTO(biometricExceptionDTOs);
		BiometricDTO biometricDTO = new BiometricDTO();
		biometricDTO.setApplicantBiometricDTO(biometricInfoDTO);
		biometricDTO.setIntroducerBiometricDTO(biometricInfoDTO);
		biometricDTO.setSupervisorBiometricDTO(biometricInfoDTO);
		biometricDTO.setOperatorBiometricDTO(biometricInfoDTO);

		registrationDTO.setBiometricDTO(biometricDTO);

		BiometricDTO useronboardbiometricDTO = new BiometricDTO();
		useronboardbiometricDTO.setOperatorBiometricDTO(createBiometricInfoDTO());
		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
		map.put(RegistrationConstants.USER_ONBOARD_DATA, useronboardbiometricDTO);
		map.put(RegistrationConstants.ONBOARD_USER, false);
		map.put(RegistrationConstants.IS_Child, false);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.when(SessionContext.map()).thenReturn(map);

		Map<String, Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.FINGER_PRINT_SCORE, 100);
		appMap.put("mosip.mdm.enabled", "Y");
		appMap.put(RegistrationConstants.FINGERPRINT_AUTHENTICATION_THRESHHOLD, "40");
		ApplicationContext.getInstance().setApplicationMap(appMap);
		HashMap<String, Map<Integer, Image>> BIO_STREAM_IMAGES = new HashMap<String, Map<Integer, Image>>();
		BIO_STREAM_IMAGES.put("key", new HashMap<>());
		//ReflectionTestUtils.setField(bioService.getClass(), "BIO_STREAM_IMAGES", BIO_STREAM_IMAGES);
		ReflectionTestUtils.setField(bioService, "BIO_STREAM_IMAGES", BIO_STREAM_IMAGES);
		CaptureResponseDto captureResponseDto = getFingerPritnCaptureResponse();
		Mockito.when(mosipBioDeviceManager.regScan(Mockito.anyObject())).thenReturn(captureResponseDto);
		Mockito.when(mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto))
				.thenReturn("value".getBytes());
		requestDetail.setType("thumbs");
		fingerPrints = bioService.getFingerPrintImageAsDTO(requestDetail,2);
		biometricDTO.getOperatorBiometricDTO().setFingerprintDetailsDTO(Arrays.asList(fingerPrints));
		
	}

	
	private static CaptureResponseDto getFingerPritnCaptureResponse() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		StringBuffer sBuffer = new StringBuffer();
		Resource resource = new ClassPathResource("fingersData.txt");
		File file = resource.getFile();
		BufferedReader  bR = new BufferedReader(new FileReader(file));
		String s;
		while((s=bR.readLine())!=null) {
			sBuffer.append(s);
		}
		bR.close();
		CaptureResponseDto captureResponse = mapper.readValue(sBuffer.toString().getBytes(StandardCharsets.UTF_8),
				CaptureResponseDto.class);
		decode(captureResponse);
		
		return captureResponse;
	}
	
	private static CaptureResponseDto getIrisCaptureResponse() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		StringBuffer sBuffer = new StringBuffer();
		Resource resource = new ClassPathResource("irisData.txt");
		File file = resource.getFile();
		BufferedReader  bR = new BufferedReader(new FileReader(file));
		String s;
		while((s=bR.readLine())!=null) {
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
	
	private BiometricInfoDTO createBiometricInfoDTO() {
		BiometricInfoDTO biometricInfoDTO = new BiometricInfoDTO();
		biometricInfoDTO.setBiometricExceptionDTO(new ArrayList<>());
		biometricInfoDTO.setFingerprintDetailsDTO(new ArrayList<>());
		biometricInfoDTO.setIrisDetailsDTO(new ArrayList<>());
		biometricInfoDTO.setFace(new FaceDetailsDTO());
		biometricInfoDTO.setExceptionFace(new FaceDetailsDTO());
		return biometricInfoDTO;
	}

	@Test
	public void testSegmentFingerPrintImage() throws IOException, RegBaseCheckedException {
		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");

		PowerMockito.mockStatic(IOUtils.class);
		Mockito.when(IOUtils.resourceToByteArray(Mockito.anyString())).thenReturn("image".getBytes());
		String[] LEFTHAND_SEGMNTD_FILE_PATHS = new String[] { "/fingerprints/lefthand/leftIndex/",
				"/fingerprints/lefthand/leftLittle/" };

		List<BiometricExceptionDTO> biometricExceptionDTOs = new ArrayList<>();
		BiometricExceptionDTO biometricExceptionDTO1 = new BiometricExceptionDTO();
		biometricExceptionDTO1.setMissingBiometric("leftMiddle");
		BiometricExceptionDTO biometricExceptionDTO2 = new BiometricExceptionDTO();
		biometricExceptionDTO2.setMissingBiometric("rightMiddle");
		biometricExceptionDTOs.add(biometricExceptionDTO1);
		biometricExceptionDTOs.add(biometricExceptionDTO2);

		((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA)).getBiometricDTO()
				.getApplicantBiometricDTO().setBiometricExceptionDTO(biometricExceptionDTOs);

		bioService.segmentFingerPrintImage(fingerprintDTO, LEFTHAND_SEGMNTD_FILE_PATHS, "leftSlap");

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(0).getFingerPrint()));
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerprintImageName());
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(0).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(0).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(1).getFingerPrint()));
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerprintImageName());
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(1).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(1).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());
	}

	@Test
	public void testSegmentFingerPrintImage2() throws IOException, RegBaseCheckedException {
		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");

		PowerMockito.mockStatic(IOUtils.class);
		Mockito.when(IOUtils.resourceToByteArray(Mockito.anyString())).thenReturn("image".getBytes());
		String[] LEFTHAND_SEGMNTD_FILE_PATHS = new String[] { "/fingerprints/lefthand/leftIndex/",
				"/fingerprints/lefthand/leftLittle/" };

		List<BiometricExceptionDTO> biometricExceptionDTOs = new ArrayList<>();
		BiometricExceptionDTO biometricExceptionDTO1 = new BiometricExceptionDTO();
		biometricExceptionDTO1.setMissingBiometric("leftMiddle");
		BiometricExceptionDTO biometricExceptionDTO2 = new BiometricExceptionDTO();
		biometricExceptionDTO2.setMissingBiometric("rightMiddle");
		biometricExceptionDTOs.add(biometricExceptionDTO1);
		biometricExceptionDTOs.add(biometricExceptionDTO2);

		((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA)).getBiometricDTO()
				.getApplicantBiometricDTO().setBiometricExceptionDTO(biometricExceptionDTOs);

		bioService.segmentFingerPrintImage(fingerprintDTO, LEFTHAND_SEGMNTD_FILE_PATHS, "leftSlap");

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(0).getFingerPrint()));
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerprintImageName());
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(0).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(0).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(1).getFingerPrint()));
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerprintImageName());
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(1).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(1).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());
	}

	@Test
	public void testSegmentedFingerPrintUserOnBoard() throws IOException, RegBaseCheckedException {

		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();

		PowerMockito.mockStatic(IOUtils.class);
		Mockito.when(IOUtils.resourceToByteArray(Mockito.anyString())).thenReturn("image".getBytes());
		String[] LEFTHAND_SEGMNTD_FILE_PATHS = new String[] { "/fingerprints/lefthand/leftIndex/",
				"/fingerprints/lefthand/leftLittle/" };

		List<BiometricExceptionDTO> biometricExceptionDTOs = new ArrayList<>();
		BiometricExceptionDTO biometricExceptionDTO1 = new BiometricExceptionDTO();
		biometricExceptionDTO1.setMissingBiometric("leftMiddle");
		BiometricExceptionDTO biometricExceptionDTO2 = new BiometricExceptionDTO();
		biometricExceptionDTO2.setMissingBiometric("rightMiddle");
		biometricExceptionDTOs.add(biometricExceptionDTO1);
		biometricExceptionDTOs.add(biometricExceptionDTO2);
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER, true);
		((BiometricDTO) SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA)).getOperatorBiometricDTO()
				.setBiometricExceptionDTO(biometricExceptionDTOs);

		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");

		bioService.segmentFingerPrintImage(fingerprintDTO, LEFTHAND_SEGMNTD_FILE_PATHS, "leftSlap");

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(0).getFingerPrint()));
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerprintImageName());
		assertEquals("Left Index", fingerprintDTO.getSegmentedFingerprints().get(0).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(0).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(0).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());

		assertEquals("image", new String(fingerprintDTO.getSegmentedFingerprints().get(1).getFingerPrint()));
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerprintImageName());
		assertEquals("Left Little", fingerprintDTO.getSegmentedFingerprints().get(1).getFingerType());
		assertEquals(0, fingerprintDTO.getSegmentedFingerprints().get(1).getNumRetry());
		assertEquals(90.0, fingerprintDTO.getSegmentedFingerprints().get(1).getQualityScore(), 0.1);
		assertEquals(false, fingerprintDTO.isForceCaptured());

	}

	@Test
	public void testExceptionSegmentFingerPrintImage() throws IOException, RegBaseCheckedException {
		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();

		PowerMockito.mockStatic(IOUtils.class);
		Mockito.when(IOUtils.resourceToByteArray(Mockito.anyString())).thenReturn("image".getBytes());
		String[] LEFTHAND_SEGMNTD_FILE_PATHS = new String[] { "/fingerprints/lefthand/leftIndex/",
				"/fingerprints/lefthand/leftLittle/", "/fingerprints/lefthand/leftMiddle/",
				"/fingerprints/lefthand/leftRing/" };

		List<BiometricExceptionDTO> biometricExceptionDTOs = new ArrayList<>();
		BiometricExceptionDTO biometricExceptionDTO1 = new BiometricExceptionDTO();
		biometricExceptionDTO1.setMissingBiometric("leftMiddle");
		BiometricExceptionDTO biometricExceptionDTO2 = new BiometricExceptionDTO();
		biometricExceptionDTO2.setMissingBiometric("leftRing");
		biometricExceptionDTOs.add(biometricExceptionDTO1);
		biometricExceptionDTOs.add(biometricExceptionDTO2);

		((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA)).getBiometricDTO()
				.getApplicantBiometricDTO().setBiometricExceptionDTO(biometricExceptionDTOs);

		// bioService.segmentFingerPrintImage(fingerprintDTO,
		// LEFTHAND_SEGMNTD_FILE_PATHS, null);
		CaptureResponseDto captureResponseDto = new CaptureResponseDto();
		CaptureResponseBioDto captureResponseBioDto = new CaptureResponseBioDto();
		captureResponseBioDto.setCaptureResponseData(new CaptureResponsBioDataDto());

		captureResponseDto.setMosipBioDeviceDataResponses(Arrays.asList(captureResponseBioDto));
		requestDetail.setType("leftslap");
		Mockito.when(mosipBioDeviceManager.regScan(requestDetail)).thenReturn(captureResponseDto);
		// Mockito.when(mosipBioDeviceManager.scan("leftslap")).thenReturn(value)
		bioService.segmentFingerPrintImage(fingerprintDTO, LEFTHAND_SEGMNTD_FILE_PATHS, "leftslap");

	}

	@Test(expected=RegBaseCheckedException.class)
	public void testGetIrisImageAsDTONoMdm() throws RegBaseCheckedException, IOException {
		PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(Mockito.any(InputStream.class))).thenReturn(Mockito.mock(BufferedImage.class));
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		IrisDetailsDTO detailsDTO = new IrisDetailsDTO();
		requestDetail.setType("LeftEye");
		bioService.getIrisImageAsDTO(requestDetail,2,2);
		IrisDetailsDTO irisDetail = detailsDTO.getIrises().get(0);
		assertNotNull(irisDetail.getIris());
		assertEquals("LeftEye.png", irisDetail.getIrisImageName());
		assertEquals("LeftEye", irisDetail.getIrisType());
		assertEquals(0, irisDetail.getNumOfIrisRetry());
		assertEquals(91.0, irisDetail.getQualityScore(),0.1);
		assertEquals(false, irisDetail.isForceCaptured());
	}

	@Ignore
	@Test
	public void testGetIrisImageAsDTOWithMdm() throws RegBaseCheckedException, IOException {
		CaptureResponseDto captureResponse = getIrisCaptureResponse();
		PowerMockito.mockStatic(ImageIO.class);
		Mockito.when(mosipBioDeviceManager.regScan(Mockito.anyObject())).thenReturn(captureResponse);
		requestDetail.setType("LEFT_EYE");
		bioService.getIrisImageAsDTO(requestDetail,2,2);
	}

	
	@Test(expected = RegBaseUncheckedException.class)
	public void testGetIrisImageAsDTOCheckedException() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(Mockito.any(InputStream.class))).thenThrow(new RuntimeException("Invalid"));
		requestDetail.setType("LeftEye");
		bioService.getIrisImageAsDTO(requestDetail,2,2);
	}


	@Test
	public void testvalidateFP() throws Exception {
		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();
		byte[] fpData = { 70, 77, 82, 0, 32, 50, 48, 0, 0, 0, 1, 26, 0, 0, 1, 60, 1, 98, 0, -59, 0, -59, 1, 0, 0, 0, 40,
				42, -128, -118, 0, -57, 35, 80, -128, 119, 0, -48, 46, 100, 64, 125, 0, -23, 34, 100, -128, -118, 0,
				-127, 14, 100, -128, -51, 0, -117, -29, 100, -128, 108, 1, 18, 21, 100, 64, -116, 1, 36, -4, 100, 64,
				70, 1, 15, 35, 33, 64, 59, 1, 16, -108, 33, 64, 65, 0, 105, 28, 93, 64, -27, 0, 87, -18, 100, -128, -81,
				1, 92, 119, 20, 64, -66, 0, 42, -10, 100, -128, 95, 0, 32, 19, 93, -128, -104, 0, -82, -83, 80, -128,
				-102, 0, -100, -1, 87, 64, -65, 0, -8, 87, 100, -128, -82, 0, 126, -117, 100, 64, 125, 1, 23, 12, 100,
				64, -54, 0, 119, -20, 100, -128, -21, 0, 124, 108, 100, -127, 14, 0, -39, 89, 100, -128, -127, 0, 82,
				-100, 100, -128, -49, 1, 65, -6, 100, 64, -40, 1, 60, 123, 100, 64, 56, 1, 58, 8, 87, 64, 49, 1, 72,
				-33, 67, 64, -13, 0, 41, -18, 100, -128, -125, 0, -82, 33, 93, -128, -73, 0, -94, -34, 93, -128, 100, 0,
				-27, 47, 100, 64, -41, 0, -16, 84, 100, -128, -74, 1, 25, 117, 100, 64, 94, 1, 14, 29, 100, 64, -125, 1,
				53, -13, 100, 64, 77, 1, 29, 18, 93, 64, 91, 1, 54, -4, 93, 65, 34, 0, -51, -41, 67, 64, -11, 1, 55,
				105, 100, -128, -29, 0, 58, 116, 100, -128, -92, 0, 27, 6, 100, -128, -59, 0, 22, -125, 100, 0, 0 };
		String minutiae = "{\"width\":316,\"height\":354,\"minutiae\":[{\"x\":129,\"y\":82,\"direction\":2.454369260617026,\"type\":\"bifurcation\"},{\"x\":91,\"y\":310,\"direction\":0.0981747704246807,\"type\":\"ending\"},{\"x\":131,\"y\":174,\"direction\":5.473243451175968,\"type\":\"bifurcation\"},{\"x\":140,\"y\":292,\"direction\":0.0981747704246807,\"type\":\"ending\"},{\"x\":216,\"y\":316,\"direction\":3.2643111166206444,\"type\":\"ending\"},{\"x\":77,\"y\":285,\"direction\":5.841398840268521,\"type\":\"ending\"},{\"x\":243,\"y\":41,\"direction\":0.44178646691106493,\"type\":\"ending\"},{\"x\":207,\"y\":321,\"direction\":0.14726215563702194,\"type\":\"bifurcation\"},{\"x\":49,\"y\":328,\"direction\":0.8099418560036185,\"type\":\"ending\"},{\"x\":56,\"y\":314,\"direction\":6.086835766330224,\"type\":\"ending\"},{\"x\":94,\"y\":270,\"direction\":5.5714182216006485,\"type\":\"ending\"},{\"x\":108,\"y\":274,\"direction\":5.767767762450011,\"type\":\"bifurcation\"},{\"x\":174,\"y\":126,\"direction\":2.8716120349219203,\"type\":\"bifurcation\"},{\"x\":175,\"y\":348,\"direction\":3.3624858870453256,\"type\":\"bifurcation\"},{\"x\":235,\"y\":124,\"direction\":3.6324665057131984,\"type\":\"bifurcation\"},{\"x\":100,\"y\":229,\"direction\":5.1296317546895835,\"type\":\"bifurcation\"},{\"x\":138,\"y\":129,\"direction\":5.939573610693203,\"type\":\"bifurcation\"},{\"x\":205,\"y\":139,\"direction\":0.7117670855789378,\"type\":\"bifurcation\"},{\"x\":182,\"y\":281,\"direction\":3.411573272257666,\"type\":\"bifurcation\"},{\"x\":270,\"y\":217,\"direction\":4.098796665230433,\"type\":\"bifurcation\"},{\"x\":154,\"y\":156,\"direction\":0.024543692606170175,\"type\":\"bifurcation\"},{\"x\":197,\"y\":22,\"direction\":3.067961575771282,\"type\":\"bifurcation\"},{\"x\":183,\"y\":162,\"direction\":0.8344855486097886,\"type\":\"bifurcation\"},{\"x\":227,\"y\":58,\"direction\":3.436116964863836,\"type\":\"bifurcation\"},{\"x\":65,\"y\":105,\"direction\":5.595961914206819,\"type\":\"ending\"},{\"x\":70,\"y\":271,\"direction\":5.424156065963627,\"type\":\"ending\"},{\"x\":164,\"y\":27,\"direction\":6.135923151542564,\"type\":\"bifurcation\"},{\"x\":125,\"y\":233,\"direction\":5.448699758569798,\"type\":\"ending\"},{\"x\":138,\"y\":199,\"direction\":5.424156065963627,\"type\":\"bifurcation\"},{\"x\":131,\"y\":309,\"direction\":0.31906800388021317,\"type\":\"ending\"},{\"x\":202,\"y\":119,\"direction\":0.4908738521234053,\"type\":\"ending\"},{\"x\":95,\"y\":32,\"direction\":5.816855147662351,\"type\":\"bifurcation\"},{\"x\":59,\"y\":272,\"direction\":2.650718801466388,\"type\":\"ending\"},{\"x\":119,\"y\":208,\"direction\":5.154175447295755,\"type\":\"bifurcation\"},{\"x\":152,\"y\":174,\"direction\":2.0371264863121317,\"type\":\"bifurcation\"},{\"x\":190,\"y\":42,\"direction\":0.24543692606170264,\"type\":\"ending\"},{\"x\":191,\"y\":248,\"direction\":4.147884050442774,\"type\":\"ending\"},{\"x\":215,\"y\":240,\"direction\":4.221515128261284,\"type\":\"ending\"},{\"x\":125,\"y\":279,\"direction\":5.988660995905543,\"type\":\"ending\"},{\"x\":229,\"y\":87,\"direction\":0.44178646691106493,\"type\":\"ending\"},{\"x\":245,\"y\":311,\"direction\":3.706097583531709,\"type\":\"ending\"},{\"x\":290,\"y\":205,\"direction\":1.0062913968529807,\"type\":\"ending\"}]}";
		fingerprintDTO.setFingerPrint(fpData);
		List<UserBiometric> userBiometrics = new ArrayList<>();
		UserBiometric userBiometric1 = new UserBiometric();
		UserBiometricId userBiometricId = new UserBiometricId();
		userBiometricId.setBioAttributeCode("leftIndex");
		userBiometricId.setUsrId("mosip");
		userBiometric1.setBioMinutia(minutiae);
		userBiometric1.setUserBiometricId(userBiometricId);
		UserBiometric userBiometric2 = new UserBiometric();
		userBiometric2.setBioMinutia(minutiae);
		userBiometric2.setUserBiometricId(userBiometricId);
		userBiometrics.add(userBiometric1);
		userBiometrics.add(userBiometric2);

		FingerprintTemplate fingerprintTemplate = Mockito.mock(FingerprintTemplate.class);
		PowerMockito.mockStatic(FingerprintTemplate.class);
		PowerMockito.whenNew(FingerprintTemplate.class).withNoArguments().thenReturn(fingerprintTemplate);
		Mockito.when(fingerprintTemplate.convert(fingerprintDTO.getFingerPrint())).thenReturn(fingerprintTemplate);
		Mockito.when(fingerprintTemplate.serialize()).thenReturn(minutiae);
		Mockito.when(fingerprintProvider.scoreCalculator(Mockito.anyString(), Mockito.anyString())).thenReturn(70.0);

		Boolean res = bioService.validateFP(fingerprintDTO, userBiometrics);
		assertTrue(!res);
	}

	@Test
	public void testvalidateFPfailure() throws Exception {
		FingerprintDetailsDTO fingerprintDTO = new FingerprintDetailsDTO();
		byte[] fpData = { 70, 77, 82, 0, 32, 50, 48, 0, 0, 0, 1, 26, 0, 0, 1, 60, 1, 98, 0, -59, 0, -59, 1, 0, 0, 0, 40,
				42, -128, -118, 0, -57, 35, 80, -128, 119, 0, -48, 46, 100, 64, 125, 0, -23, 34, 100, -128, -118, 0,
				-127, 14, 100, -128, -51, 0, -117, -29, 100, -128, 108, 1, 18, 21, 100, 64, -116, 1, 36, -4, 100, 64,
				70, 1, 15, 35, 33, 64, 59, 1, 16, -108, 33, 64, 65, 0, 105, 28, 93, 64, -27, 0, 87, -18, 100, -128, -81,
				1, 92, 119, 20, 64, -66, 0, 42, -10, 100, -128, 95, 0, 32, 19, 93, -128, -104, 0, -82, -83, 80, -128,
				-102, 0, -100, -1, 87, 64, -65, 0, -8, 87, 100, -128, -82, 0, 126, -117, 100, 64, 125, 1, 23, 12, 100,
				64, -54, 0, 119, -20, 100, -128, -21, 0, 124, 108, 100, -127, 14, 0, -39, 89, 100, -128, -127, 0, 82,
				-100, 100, -128, -49, 1, 65, -6, 100, 64, -40, 1, 60, 123, 100, 64, 56, 1, 58, 8, 87, 64, 49, 1, 72,
				-33, 67, 64, -13, 0, 41, -18, 100, -128, -125, 0, -82, 33, 93, -128, -73, 0, -94, -34, 93, -128, 100, 0,
				-27, 47, 100, 64, -41, 0, -16, 84, 100, -128, -74, 1, 25, 117, 100, 64, 94, 1, 14, 29, 100, 64, -125, 1,
				53, -13, 100, 64, 77, 1, 29, 18, 93, 64, 91, 1, 54, -4, 93, 65, 34, 0, -51, -41, 67, 64, -11, 1, 55,
				105, 100, -128, -29, 0, 58, 116, 100, -128, -92, 0, 27, 6, 100, -128, -59, 0, 22, -125, 100, 0, 0 };
		String minutiae = "{\"width\":316,\"height\":354,\"minutiae\":[{\"x\":129,\"y\":82,\"direction\":2.454369260617026,\"type\":\"bifurcation\"},{\"x\":91,\"y\":310,\"direction\":0.0981747704246807,\"type\":\"ending\"},{\"x\":131,\"y\":174,\"direction\":5.473243451175968,\"type\":\"bifurcation\"},{\"x\":140,\"y\":292,\"direction\":0.0981747704246807,\"type\":\"ending\"},{\"x\":216,\"y\":316,\"direction\":3.2643111166206444,\"type\":\"ending\"},{\"x\":77,\"y\":285,\"direction\":5.841398840268521,\"type\":\"ending\"},{\"x\":243,\"y\":41,\"direction\":0.44178646691106493,\"type\":\"ending\"},{\"x\":207,\"y\":321,\"direction\":0.14726215563702194,\"type\":\"bifurcation\"},{\"x\":49,\"y\":328,\"direction\":0.8099418560036185,\"type\":\"ending\"},{\"x\":56,\"y\":314,\"direction\":6.086835766330224,\"type\":\"ending\"},{\"x\":94,\"y\":270,\"direction\":5.5714182216006485,\"type\":\"ending\"},{\"x\":108,\"y\":274,\"direction\":5.767767762450011,\"type\":\"bifurcation\"},{\"x\":174,\"y\":126,\"direction\":2.8716120349219203,\"type\":\"bifurcation\"},{\"x\":175,\"y\":348,\"direction\":3.3624858870453256,\"type\":\"bifurcation\"},{\"x\":235,\"y\":124,\"direction\":3.6324665057131984,\"type\":\"bifurcation\"},{\"x\":100,\"y\":229,\"direction\":5.1296317546895835,\"type\":\"bifurcation\"},{\"x\":138,\"y\":129,\"direction\":5.939573610693203,\"type\":\"bifurcation\"},{\"x\":205,\"y\":139,\"direction\":0.7117670855789378,\"type\":\"bifurcation\"},{\"x\":182,\"y\":281,\"direction\":3.411573272257666,\"type\":\"bifurcation\"},{\"x\":270,\"y\":217,\"direction\":4.098796665230433,\"type\":\"bifurcation\"},{\"x\":154,\"y\":156,\"direction\":0.024543692606170175,\"type\":\"bifurcation\"},{\"x\":197,\"y\":22,\"direction\":3.067961575771282,\"type\":\"bifurcation\"},{\"x\":183,\"y\":162,\"direction\":0.8344855486097886,\"type\":\"bifurcation\"},{\"x\":227,\"y\":58,\"direction\":3.436116964863836,\"type\":\"bifurcation\"},{\"x\":65,\"y\":105,\"direction\":5.595961914206819,\"type\":\"ending\"},{\"x\":70,\"y\":271,\"direction\":5.424156065963627,\"type\":\"ending\"},{\"x\":164,\"y\":27,\"direction\":6.135923151542564,\"type\":\"bifurcation\"},{\"x\":125,\"y\":233,\"direction\":5.448699758569798,\"type\":\"ending\"},{\"x\":138,\"y\":199,\"direction\":5.424156065963627,\"type\":\"bifurcation\"},{\"x\":131,\"y\":309,\"direction\":0.31906800388021317,\"type\":\"ending\"},{\"x\":202,\"y\":119,\"direction\":0.4908738521234053,\"type\":\"ending\"},{\"x\":95,\"y\":32,\"direction\":5.816855147662351,\"type\":\"bifurcation\"},{\"x\":59,\"y\":272,\"direction\":2.650718801466388,\"type\":\"ending\"},{\"x\":119,\"y\":208,\"direction\":5.154175447295755,\"type\":\"bifurcation\"},{\"x\":152,\"y\":174,\"direction\":2.0371264863121317,\"type\":\"bifurcation\"},{\"x\":190,\"y\":42,\"direction\":0.24543692606170264,\"type\":\"ending\"},{\"x\":191,\"y\":248,\"direction\":4.147884050442774,\"type\":\"ending\"},{\"x\":215,\"y\":240,\"direction\":4.221515128261284,\"type\":\"ending\"},{\"x\":125,\"y\":279,\"direction\":5.988660995905543,\"type\":\"ending\"},{\"x\":229,\"y\":87,\"direction\":0.44178646691106493,\"type\":\"ending\"},{\"x\":245,\"y\":311,\"direction\":3.706097583531709,\"type\":\"ending\"},{\"x\":290,\"y\":205,\"direction\":1.0062913968529807,\"type\":\"ending\"}]}";
		fingerprintDTO.setFingerPrint(fpData);
		List<UserBiometric> userBiometrics = new ArrayList<>();
		UserBiometric userBiometric1 = new UserBiometric();
		UserBiometricId userBiometricId = new UserBiometricId();
		userBiometricId.setBioAttributeCode("leftIndex");
		userBiometricId.setUsrId("mosip");
		userBiometric1.setBioMinutia(minutiae);
		userBiometric1.setUserBiometricId(userBiometricId);
		UserBiometric userBiometric2 = new UserBiometric();
		userBiometric2.setBioMinutia(minutiae);
		userBiometric2.setUserBiometricId(userBiometricId);
		userBiometrics.add(userBiometric1);
		userBiometrics.add(userBiometric2);

		FingerprintTemplate fingerprintTemplate = Mockito.mock(FingerprintTemplate.class);
		PowerMockito.mockStatic(FingerprintTemplate.class);
		PowerMockito.whenNew(FingerprintTemplate.class).withNoArguments().thenReturn(fingerprintTemplate);
		Mockito.when(fingerprintTemplate.convert(fingerprintDTO.getFingerPrint())).thenReturn(fingerprintTemplate);
		Mockito.when(fingerprintTemplate.serialize()).thenReturn(minutiae);
		Mockito.when(fingerprintProvider.scoreCalculator(Mockito.anyString(), Mockito.anyString())).thenReturn(700.0);

		Boolean res = bioService.validateFP(fingerprintDTO, userBiometrics);
		assertTrue(res);
	}

	@Test
	public void validateFaceTest1() throws RegBaseCheckedException, IOException {
		requestDetail.setType(RegistrationConstants.FACE_FULLFACE);
		Mockito.when(mosipBioDeviceManager.regScan(Mockito.any())).thenReturn(getFingerPritnCaptureResponse());
		bioService.validateFace(bioService.getFaceAuthenticationDto("userId",requestDetail));
	}

	@Test
	public void validateFaceTest2() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		Mockito.when(mosipBioDeviceManager.regScan(Mockito.any())).thenReturn(getFingerPritnCaptureResponse());
		requestDetail.setType(RegistrationConstants.FACE_FULLFACE);
		bioService.validateFace(bioService.getFaceAuthenticationDto("userId",requestDetail));
	}

	@Test
	public void nonMdmTest() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		PowerMockito.mockStatic(IOUtils.class);
		Mockito.when(IOUtils.resourceToByteArray(Mockito.any())).thenReturn("image".getBytes());
		requestDetail.setType("LeftEye");
		bioService.getIrisImageAsDTO(requestDetail,2,2);
	}

	
	@Test
	public void getFingerPrintImageAsDTONonMDMTest() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		requestDetail.setType(RegistrationConstants.FINGERPRINT_SLAB_LEFT);
		bioService.getFingerPrintImageAsDTO(requestDetail,2);
		requestDetail.setType(RegistrationConstants.FINGERPRINT_SLAB_RIGHT);
		bioService.getFingerPrintImageAsDTO(requestDetail,2);
		requestDetail.setType(RegistrationConstants.FINGERPRINT_SLAB_THUMBS);
		bioService.getFingerPrintImageAsDTO(requestDetail,2);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getFingerPrintImageAsDTONonMDMTestWithException() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		Mockito.when(SessionContext.map().get(RegistrationConstants.ONBOARD_USER)).thenThrow(Exception.class);
		requestDetail.setType(RegistrationConstants.FINGERPRINT_SLAB_LEFT);
		bioService.getFingerPrintImageAsDTO(requestDetail,2);
	}

	@Test
	public void validateFingerPrintTest() throws RegBaseCheckedException, IOException {

		CaptureResponseDto captureResponseDto = getFingerPritnCaptureResponse();
		Mockito.when(mosipBioDeviceManager.authScan(Mockito.anyObject())).thenReturn(captureResponseDto);
		Mockito.when(mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto))
				.thenReturn("value".getBytes());
		bioService.validateFingerPrint(bioService.getFingerPrintAuthenticationDto("userId",requestDetail));
	}

	
	@Test
	public void validateIrisTest() throws RegBaseCheckedException, IOException {

		CaptureResponseDto captureResponseDto = getFingerPritnCaptureResponse();
		Mockito.when(mosipBioDeviceManager.authScan(Mockito.anyObject())).thenReturn(captureResponseDto);
		Mockito.when(mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto))
				.thenReturn("value".getBytes());
		bioService.validateIris(bioService.getFingerPrintAuthenticationDto("userId",requestDetail));
	}

	
	@Test
	public void validateFingerPrintTest2() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		bioService.validateFingerPrint(bioService.getFingerPrintAuthenticationDto("userId",requestDetail));
	}

	@Test
	public void validateIrisTest2() throws RegBaseCheckedException, IOException {
		ApplicationContext.getInstance().getApplicationMap().put("mosip.mdm.enabled", "N");
		bioService.validateIris(bioService.getIrisAuthenticationDto("userId", requestDetail));
	}

	@Test
	public void validateIrisTest1() throws RegBaseCheckedException, IOException {
		CaptureResponseDto captureResponseDto = getIrisCaptureResponse();
		Mockito.when(mosipBioDeviceManager.authScan(Mockito.anyObject())).thenReturn(captureResponseDto);
		Mockito.when(mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto))
				.thenReturn("value".getBytes());
		bioService.validateIris(bioService.getIrisAuthenticationDto("userId", requestDetail));
	}
	
	@Test
	public void simpleCallForCoverage() throws Exception {
		bioService.getBioQualityScores("",1);
		bioService.getHighQualityScoreByBioType("");
		PowerMockito.when(mosipBioDeviceManager,"regScan", Mockito.anyObject()).thenReturn(null);
		assertEquals(null, bioService.captureFace(Mockito.anyObject()));
		PowerMockito.when(mosipBioDeviceManager,"getSingleBiometricIsoTemplate", Mockito.anyObject()).thenReturn(null);
		assertEquals(null, bioService.getSingleBiometricIsoTemplate(Mockito.anyObject()));
		PowerMockito.when(mosipBioDeviceManager,"getSingleBioValue", Mockito.anyObject()).thenReturn(null);
		assertEquals(null, bioService.getSingleBioValue(Mockito.anyObject()));
		bioService.getBioStreamImage("key", 1);
		FingerprintDetailsDTO leftFingerPrint = PowerMockito.spy(fingerPrints);
		leftFingerPrint.setFingerType(RegistrationConstants.FINGERPRINT_SLAB_LEFT);
		//bioService.isValidFingerPrints(leftFingerPrint);
		//bioService.isAllNonExceptionFingerprintsCaptured();
		bioService.clearAllCaptures();
		bioService.clearAllStreamImages();
		bioService.clearBIOScoreByBioType(Arrays.asList("captured"));
		bioService.clearCaptures(Arrays.asList("captured"));

	}

}
