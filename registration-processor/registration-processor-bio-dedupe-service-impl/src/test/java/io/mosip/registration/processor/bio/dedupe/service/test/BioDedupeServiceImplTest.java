package io.mosip.registration.processor.bio.dedupe.service.test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.registration.processor.bio.dedupe.exception.ABISAbortException;
import io.mosip.registration.processor.bio.dedupe.exception.ABISInternalError;
import io.mosip.registration.processor.bio.dedupe.exception.UnableToServeRequestABISException;
import io.mosip.registration.processor.bio.dedupe.exception.UnexceptedError;
import io.mosip.registration.processor.bio.dedupe.service.impl.BioDedupeServiceImpl;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidateListDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidatesDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ IOUtils.class, JsonUtil.class })
public class BioDedupeServiceImplTest {

	@Mock
	InputStream inputStream;

	@Mock
	RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	AbisInsertResponseDto abisInsertResponseDto = new AbisInsertResponseDto();

	@InjectMocks
	BioDedupeServiceImpl bioDedupeService = new BioDedupeServiceImpl();

	private AbisIdentifyResponseDto identifyResponse = new AbisIdentifyResponseDto();

	String registrationId = "1000";

	Identity identity = new Identity();

	private ListAppender<ILoggingEvent> listAppender;

	private Logger fooLogger;

	@Mock
	private Environment env;

	@Mock
	private RegistrationStatusService registrationStatusService;

	@Mock
	private Utilities utility;

	@Mock
	LogDescription description;
	private ClassLoader classLoader;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private CbeffUtil cbeffutil;

	private static final String process = "process";
	private static final byte[] fileBytes = "1234567890".getBytes();

	/*
	 * 
	 * /** Setup.
	 * 
	 * @throws Exception
	 */

	@Before
	public void setup() throws Exception {
		classLoader = getClass().getClassLoader();
		Mockito.doNothing().when(packetInfoManager).saveAbisRef(any(), any(), any());

		abisInsertResponseDto.setReturnValue("2");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ReflectionTestUtils.setField(bioDedupeService, "maxResults", "30");
		ReflectionTestUtils.setField(bioDedupeService, "targetFPIR", "30");


		String refId = "01234567-89AB-CDEF-0123-456789ABCDEF";
		List<String> refIdList = new ArrayList<>();
		refIdList.add(refId);
		Mockito.when(packetInfoManager.getReferenceIdByRid(anyString())).thenReturn(refIdList);

		CandidatesDto candidate1 = new CandidatesDto();
		candidate1.setReferenceId("01234567-89AB-CDEF-0123-456789ABCDEG");

		CandidatesDto candidate2 = new CandidatesDto();
		candidate2.setReferenceId("01234567-89AB-CDEF-0123-456789ABCDEH");


		CandidatesDto[] candidateArray = new CandidatesDto[2];
		candidateArray[0] = candidate1;
		candidateArray[1] = candidate2;

		CandidateListDto candidateList = new CandidateListDto();
		candidateList.setCandidates(candidateArray);

		identifyResponse.setCandidateList(candidateList);

		List<DemographicInfoDto> demoList = new ArrayList<>();
		DemographicInfoDto demo1 = new DemographicInfoDto();
		demoList.add(demo1);
		Mockito.when(packetInfoManager.findDemoById(anyString())).thenReturn(demoList);
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(anyString())).thenReturn(true);

		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream);
		JSONObject mappingJSONObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson(any()))
				.thenReturn(JsonUtil.getJSONObject(mappingJSONObject, MappingJsonConstants.IDENTITY));
		fooLogger = (Logger) LoggerFactory.getLogger(BioDedupeServiceImpl.class);
		listAppender = new ListAppender<>();
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
		io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		io.mosip.kernel.biometrics.constant.QualityType quality = new QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.FINGER;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);

		Mockito.when(packetManagerService.getBiometrics(any(), any(),any(), any())).thenReturn(biometricRecord);
		Mockito.when(packetManagerService.getBiometricsByMappingJsonKey(any(), any(),any(), any())).thenReturn(biometricRecord);
		Mockito.when(cbeffutil.createXML(any())).thenReturn(fileBytes);


	}

	/**
	 * Insert biometrics success test.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test
	public void insertBiometricsSuccessTest() throws ApisResourceAccessException, IOException {

		abisInsertResponseDto.setReturnValue("1");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(abisInsertResponseDto);

		String authResponse = bioDedupeService.insertBiometrics(registrationId);
		assertTrue(authResponse.equals("success"));

	}

	/**
	 * Insert biometrics ABIS internal error failure test.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = ABISInternalError.class)
	public void insertBiometricsABISInternalErrorFailureTest() throws ApisResourceAccessException, IOException {

		abisInsertResponseDto.setFailureReason("1");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(abisInsertResponseDto);

		String authResponse = bioDedupeService.insertBiometrics(registrationId);
		assertTrue(authResponse.equals("2"));

	}

	/**
	 * Insert biometrics ABIS abort exception failure test.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */

	@Test(expected = ABISAbortException.class)
	public void insertBiometricsABISAbortExceptionFailureTest() throws ApisResourceAccessException, IOException {

		abisInsertResponseDto = new AbisInsertResponseDto();
		abisInsertResponseDto.setFailureReason("2");
		abisInsertResponseDto.setReturnValue("2");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(abisInsertResponseDto);

		String authResponse = bioDedupeService.insertBiometrics(registrationId);
		assertTrue(authResponse.equals("2"));

	}

	/**
	 * Insert biometrics unexcepted error failure test.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = UnexceptedError.class)
	public void insertBiometricsUnexceptedErrorFailureTest() throws ApisResourceAccessException, IOException {

		abisInsertResponseDto.setFailureReason("3");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(abisInsertResponseDto);

		String authResponse = bioDedupeService.insertBiometrics(registrationId);
		assertTrue(authResponse.equals("2"));

	}

	/**
	 * Insert biometrics unable to serve request ABIS exception failure test.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */

	@Test(expected = UnableToServeRequestABISException.class)
	public void insertBiometricsUnableToServeRequestABISExceptionFailureTest()
			throws ApisResourceAccessException, IOException {

		abisInsertResponseDto.setFailureReason("4");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(abisInsertResponseDto);

		String authResponse = bioDedupeService.insertBiometrics(registrationId);
		assertTrue(authResponse.equals("2"));

	}

	/**
	 * Test perform dedupe success.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test
	public void testPerformDedupeSuccess() throws ApisResourceAccessException, IOException {

		identifyResponse.setReturnValue("1");
		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(identifyResponse);
		String rid = "27847657360002520181208094056";

		List<String> list = new ArrayList<>();
		list.add(rid);
		Mockito.when(packetInfoManager.getRidByReferenceId(any())).thenReturn(list);

		List<String> ridList = new ArrayList<>();
		ridList.add(rid);
		ridList.add(rid);

		List<DemographicInfoDto> demoList = new ArrayList<>();
		DemographicInfoDto demo1 = new DemographicInfoDto();
		demoList.add(demo1);
		Mockito.when(packetInfoManager.findDemoById(any())).thenReturn(demoList);
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		
		List<String> duplicates = bioDedupeService.performDedupe(rid);

		assertEquals(ridList, duplicates);
	}

	/**
	 * Test perform dedupe failure.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = ABISInternalError.class)
	public void testPerformDedupeFailure() throws ApisResourceAccessException, IOException {

		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(identifyResponse);
		String rid = "27847657360002520181208094056";
		identifyResponse.setReturnValue("2");
		identifyResponse.setFailureReason("1");

		bioDedupeService.performDedupe(rid);
	}

	/**
	 * Test dedupe abis abort exception.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = ABISAbortException.class)
	public void testDedupeAbisAbortException() throws ApisResourceAccessException, IOException {

		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(identifyResponse);
		String rid = "27847657360002520181208094056";
		identifyResponse.setReturnValue("2");
		identifyResponse.setFailureReason("2");

		bioDedupeService.performDedupe(rid);
	}

	/**
	 * Test dedupe unexpected error.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = UnexceptedError.class)
	public void testDedupeUnexpectedError() throws ApisResourceAccessException, IOException {

		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(identifyResponse);
		String rid = "27847657360002520181208094056";
		identifyResponse.setReturnValue("2");
		identifyResponse.setFailureReason("3");

		bioDedupeService.performDedupe(rid);
	}

	/**
	 * Test dedupe unable to serve request ABIS exception.
	 *
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 */
	@Test(expected = UnableToServeRequestABISException.class)
	public void testDedupeUnableToServeRequestABISException() throws ApisResourceAccessException, IOException {

		Mockito.when(restClientService.postApi(any(), any(),any(), any(), any()))
				.thenReturn(identifyResponse);
		String rid = "27847657360002520181208094056";
		identifyResponse.setReturnValue("2");
		identifyResponse.setFailureReason("4");

		bioDedupeService.performDedupe(rid);
	}

	/**
	 * Test get file.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGetFile() throws Exception {

		byte[] fileData = bioDedupeService.getFileByRegId(registrationId, process);
		assertArrayEquals(fileData, fileBytes);
	}

	@Test
	public void getFileByAbisRefId() throws Exception {

		byte[] fileData = bioDedupeService.getFileByAbisRefId(registrationId, process);
		assertArrayEquals("verfing if byte array returned is null for the given invalid regId ", fileData, null);

		// case2 : if regId is valid
		List<String> regIds = new ArrayList<>();
		regIds.add("10006100360000320190702102135");
		Mockito.when(packetInfoManager.getRidByReferenceId(anyString())).thenReturn(regIds);
		byte[] result = bioDedupeService.getFileByAbisRefId(registrationId, process);
		assertArrayEquals("verfing if byte array returned is same as expected ", result, fileBytes);
	}

	@Test
	public void IOExceptionTest() throws Exception {

		listAppender.start();
		fooLogger.addAppender(listAppender);
		byte[] data = "1234567890".getBytes();
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenThrow(new IOException());

		byte[] fileData = bioDedupeService.getFileByRegId(registrationId, process);
		Assertions.assertThatExceptionOfType(IOException.class);
	}
}
