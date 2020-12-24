package io.mosip.registration.processor.quality.checker.stage.test;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.bioapi.model.QualityScore;
import io.mosip.kernel.core.bioapi.model.Response;
import io.mosip.kernel.core.bioapi.spi.IBioApi;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.quality.checker.stage.QualityCheckerStage;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", })
public class QualityCheckerStageTest {

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private Utilities utility;

	@Mock
	private PacketManagerService packetManagerService;

	/** The cbeff util. */
	@Mock
	private CbeffUtil cbeffUtil;

	@Mock
	private IBioApi fingerApi;

	@InjectMocks
	private QualityCheckerStage qualityCheckerStage = new QualityCheckerStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus() {

				@Override
				public Vertx getEventbus() {
					return vertx;
				}

				@Override
				public void consume(MessageBusAddress fromAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void send(MessageBusAddress toAddress, MessageDTO message) {

				}
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(qualityCheckerStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(qualityCheckerStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(qualityCheckerStage, "irisThreshold", 70);
		ReflectionTestUtils.setField(qualityCheckerStage, "leftFingerThreshold", 80);
		ReflectionTestUtils.setField(qualityCheckerStage, "rightFingerThreshold", 80);
		ReflectionTestUtils.setField(qualityCheckerStage, "thumbFingerThreshold", 80);
		ReflectionTestUtils.setField(qualityCheckerStage, "faceThreshold", 25);
		ReflectionTestUtils.setField(qualityCheckerStage, "fingerApi", fingerApi);
		ReflectionTestUtils.setField(qualityCheckerStage, "faceApi", fingerApi);
		ReflectionTestUtils.setField(qualityCheckerStage, "irisApi", fingerApi);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		String idJsonString = "{\n" + "  \"identity\" : {\n" + "    \"fullName\" : [ {\n"
				+ "      \"language\" : \"eng\",\n" + "      \"value\" : \"Ragavendran V\"\n" + "    }, {\n"
				+ "      \"language\" : \"ara\",\n" + "      \"value\" : \"قشلشرثىيقشى ر\"\n" + "    } ],\n"
				+ "    \"individualBiometrics\" : {\n" + "      \"format\" : \"cbeff\",\n"
				+ "      \"version\" : 1.0,\n" + "      \"value\" : \"applicant_bio_CBEFF\"\n" + "    }\n" + "  }\n"
				+ "}";
		InputStream idJsonStream = IOUtils.toInputStream(idJsonString, "UTF-8");

		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");

		ClassLoader classLoader = getClass().getClassLoader();
		File cbeff1 = new File(classLoader.getResource("CBEFF1.xml").getFile());
		InputStream cbeff1Stream = new FileInputStream(cbeff1);

		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new io.mosip.kernel.biometrics.entities.RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		io.mosip.kernel.biometrics.constant.QualityType quality = new io.mosip.kernel.biometrics.constant.QualityType();
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

		BIR birType2 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType2 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		bdbInfoType2.setQuality(quality);
		BiometricType singleType2 = BiometricType.FINGER;
		List<BiometricType> singleTypeList2 = new ArrayList<>();
		singleTypeList2.add(singleType2);
		List<String> subtype2 = new ArrayList<>(Arrays.asList("Right", "RingFinger"));
		bdbInfoType2.setSubtype(subtype2);
		bdbInfoType2.setType(singleTypeList2);
		birType2.setBdbInfo(bdbInfoType2);
		birTypeList.add(birType2);

		BIR birType3 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType3 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		bdbInfoType3.setQuality(quality);
		BiometricType singleType3 = BiometricType.IRIS;
		List<BiometricType> singleTypeList3 = new ArrayList<>();
		singleTypeList3.add(singleType3);
		List<String> subtype3 = new ArrayList<>(Arrays.asList("Right"));
		bdbInfoType3.setSubtype(subtype3);
		bdbInfoType3.setType(singleTypeList3);
		birType3.setBdbInfo(bdbInfoType3);
		birTypeList.add(birType3);

		BIR birType4 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType4 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		bdbInfoType4.setQuality(quality);
		BiometricType singleType4 = BiometricType.FACE;
		List<BiometricType> singleTypeList4 = new ArrayList<>();
		singleTypeList4.add(singleType4);
		List<String> subtype4 = new ArrayList<>();
		bdbInfoType4.setSubtype(subtype4);
		bdbInfoType4.setType(singleTypeList4);
		birType4.setBdbInfo(bdbInfoType4);
		birTypeList.add(birType4);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenReturn(biometricRecord);
		when(packetManagerService.getFieldByKey(any(), any(), any())).thenReturn("individualBiometrics");

		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream);
		JSONObject mappingJSONObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(JsonUtil.getJSONObject(mappingJSONObject, "identity"));
	}

	@Test
	public void testDeployVerticle() {
		qualityCheckerStage.deployVerticle();
	}

	@Test
	public void testQualityCheckerSuccess() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		RegistryIDType registryIDType = new RegistryIDType();
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

		BIR birType2 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType2 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		bdbInfoType2.setQuality(quality);
		BiometricType singleType2 = BiometricType.FINGER;
		List<BiometricType> singleTypeList2 = new ArrayList<>();
		singleTypeList2.add(singleType2);
		List<String> subtype2 = new ArrayList<>(Arrays.asList("Right", "RingFinger"));
		bdbInfoType2.setSubtype(subtype2);
		bdbInfoType2.setType(singleTypeList2);
		birType2.setBdbInfo(bdbInfoType2);
		birTypeList.add(birType2);

		BIR birType3 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType3 = new BDBInfo.BDBInfoBuilder().build();
		bdbInfoType3.setQuality(quality);
		BiometricType singleType3 = BiometricType.IRIS;
		List<BiometricType> singleTypeList3 = new ArrayList<>();
		singleTypeList3.add(singleType3);
		List<String> subtype3 = new ArrayList<>(Arrays.asList("Right"));
		bdbInfoType3.setSubtype(subtype3);
		bdbInfoType3.setType(singleTypeList3);
		birType3.setBdbInfo(bdbInfoType3);
		birTypeList.add(birType3);

		BIR birType4 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType4 = new BDBInfo.BDBInfoBuilder().build();
		bdbInfoType4.setQuality(quality);
		BiometricType singleType4 = BiometricType.FACE;
		List<BiometricType> singleTypeList4 = new ArrayList<>();
		singleTypeList4.add(singleType4);
		List<String> subtype4 = new ArrayList<>();
		bdbInfoType4.setSubtype(subtype4);
		bdbInfoType4.setType(singleTypeList4);
		birType4.setBdbInfo(bdbInfoType4);
		birTypeList.add(birType4);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenReturn(null).thenReturn(biometricRecord);



		QualityScore qualityScore = new QualityScore();
		qualityScore.setScore(90);
		Response<QualityScore> response=new Response<>();
		response.setResponse(qualityScore);
		response.setStatusCode(200);
		Mockito.when(fingerApi.checkQuality(any(), any())).thenReturn(response);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testException() throws Exception {
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenThrow(new PacketManagerException("code","message"));
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityCheckerStage.process(dto);
		assertFalse(messageDTO.getIsValid());
	}
	
	@Test
	public void testCbeffNotFound() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenThrow(new IOException("message"));
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityCheckerStage.process(dto);
		assertFalse(messageDTO.getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testApiNotAccessibleTest() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenThrow(new ApisResourceAccessException("message"));
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityCheckerStage.process(dto);
		assertFalse(messageDTO.getIsValid());

	}

	@Test
	public void testQualityCheckFailure() {
		QualityScore qualityScore = new QualityScore();
		qualityScore.setScore(50);
		Response<QualityScore> response=new Response<>();
		response.setResponse(qualityScore);
		response.setStatusCode(200);
		Mockito.when(fingerApi.checkQuality(any(), any())).thenReturn(response);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertFalse(result.getIsValid());
	}

	@Test
	public void testFileNameMissing() throws IOException {

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testFsAdapterException() {
		FSAdapterException exception = new FSAdapterException("", "");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testBiometricException() throws BiometricException {
		QualityScore qualityScore = new QualityScore();
		qualityScore.setScore(50);
		Response<QualityScore> response=new Response<>();
		response.setResponse(qualityScore);
		response.setStatusCode(404);
		response.setStatusMessage("score is missing from database");
		Mockito.when(fingerApi.checkQuality(any(), any())).thenReturn(response);
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testQualityCheckfailureException() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testFileMissing() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		when(packetManagerService.getBiometrics(anyString(),anyString(),any(),anyString())).thenReturn(null);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testBioetricFileNotPresentInIdObject() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

		when(packetManagerService.getFieldByKey(any(), any(), any())).thenReturn(null);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getIsValid());
	}

	@Test
	public void testNoBiometricInPacket() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenReturn(null);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertFalse(result.getIsValid());
	}

	@Test
	public void testBioTypeException() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder().build();
		io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new io.mosip.kernel.biometrics.entities.RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		io.mosip.kernel.biometrics.constant.QualityType quality = new io.mosip.kernel.biometrics.constant.QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.DNA;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);
		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenReturn(biometricRecord);



		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testJsonProcessingException() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenThrow(new JsonProcessingException("Json exception"));
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityCheckerStage.process(dto);

		assertTrue(result.getInternalError());
	}
}
