package io.mosip.registration.processor.quality.qualifier.stage.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.quality.classifier.stage.QualityClassifierStage;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*",
		"org.xml.*", })
public class QualityClassifierStageTest {

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private Utilities utility;

	@Mock
	private PriorityBasedPacketManagerService basedPacketManagerService;

	/** The cbeff util. */
	@Mock
	private CbeffUtil cbeffUtil;

	@Mock
	private BioAPIFactory bioApiFactory;
	@Mock
	private iBioProviderApi faceIBioProviderApi;
	@Mock
	private iBioProviderApi fingerIBioProviderApi;
	@Mock
	private iBioProviderApi irisIBioProviderApi;

	@Mock
	private PacketManagerService packetManagerService;

	private String qualityPrefixTag = "Biometric_Quality-";


	private String level_1 = "level-1";
	private String level_2 = "level-2";
	private String level_3 = "level-3";
	private String level_4 = "level-4";
	private String level_5 = "level-5";
	private String level_6 = "level-6";
	private String level_7 = "level-7";
	private String level_8 = "level-8";
	private String level_9 = "level-9";
	private String level_10 = "level-10";


	JSONObject mappingJSONObject;

	@InjectMocks
	private QualityClassifierStage qualityClassifierStage = new QualityClassifierStage() {
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

				@Override
				public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}

				@Override
				public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}
			};
		}

		@Override
		public Integer getPort() {
			return 8080;
		};

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		}
	};

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(qualityClassifierStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(qualityClassifierStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(qualityClassifierStage, "messageExpiryTimeLimit", Long.valueOf(0));
//		ReflectionTestUtils.setField(qualityClassifierStage, "irisThreshold", 70);
//		ReflectionTestUtils.setField(qualityClassifierStage, "leftFingerThreshold", 80);
//		ReflectionTestUtils.setField(qualityClassifierStage, "rightFingerThreshold", 80);
//		ReflectionTestUtils.setField(qualityClassifierStage, "thumbFingerThreshold", 80);
//		ReflectionTestUtils.setField(qualityClassifierStage, "faceThreshold", 25);
		ReflectionTestUtils.setField(qualityClassifierStage, "qualityTagPrefix", qualityPrefixTag);

		Map<String, String> qualityClassificationRangeMap = new HashMap<String, String>();
		qualityClassificationRangeMap.put(level_1, "0-10");
		qualityClassificationRangeMap.put(level_2, "10-20");
		qualityClassificationRangeMap.put(level_3, "20-30");
		qualityClassificationRangeMap.put(level_4, "30-40");
		qualityClassificationRangeMap.put(level_5, "40-50");
		qualityClassificationRangeMap.put(level_6, "50-60");
		qualityClassificationRangeMap.put(level_7, "60-70");
		qualityClassificationRangeMap.put(level_8, "70-80");
		qualityClassificationRangeMap.put(level_9, "80-90");
		qualityClassificationRangeMap.put(level_10, "90-101");


		ReflectionTestUtils.setField(qualityClassifierStage, "qualityClassificationRangeMap",
				qualityClassificationRangeMap);

		Map<String, int[]> parsedMap = new HashMap<String, int[]>();
		parsedMap.put(level_1, new int[] { 0, 10 });
		parsedMap.put(level_2, new int[] { 10, 20 });
		parsedMap.put(level_3, new int[] { 20, 30 });
		parsedMap.put(level_4, new int[] { 30, 40 });
		parsedMap.put(level_5, new int[] { 40, 50 });
		parsedMap.put(level_6, new int[] { 50, 60 });
		parsedMap.put(level_7, new int[] { 60, 70 });
		parsedMap.put(level_8, new int[] { 70, 80 });
		parsedMap.put(level_9, new int[] { 80, 90 });
		parsedMap.put(level_10, new int[] { 90, 101 });


		ReflectionTestUtils.setField(qualityClassifierStage, "parsedQualityRangeMap", parsedMap);
		ReflectionTestUtils.setField(qualityClassifierStage, "modalities", Arrays.asList("Iris", "Finger", "Face"));

		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("123456789");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
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
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType2 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType3 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType4 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenReturn(biometricRecord);
		when(basedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any()))
				.thenReturn("individualBiometrics");
//		Mockito.doNothing().when(packetManagerService).addOrUpdateTags(any(), any());

		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream);
		mappingJSONObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString()))
				.thenReturn(JsonUtil.getJSONObject(mappingJSONObject, "identity"));
		Mockito.when(bioApiFactory.getBioProvider(any(), any())).thenReturn(faceIBioProviderApi);
		Mockito.when(bioApiFactory.getBioProvider(BiometricType.FACE, BiometricFunction.QUALITY_CHECK))
				.thenReturn(faceIBioProviderApi);
		Mockito.when(bioApiFactory.getBioProvider(BiometricType.FINGER, BiometricFunction.QUALITY_CHECK))
				.thenReturn(fingerIBioProviderApi);
		Mockito.when(bioApiFactory.getBioProvider(BiometricType.IRIS, BiometricFunction.QUALITY_CHECK))
				.thenReturn(irisIBioProviderApi);
		float[] scores = new float[1];
		scores[0] = 100;
		Mockito.when(faceIBioProviderApi.getSegmentQuality(any(), any())).thenReturn(scores);

	}

	@Test
	public void testDeployVerticle() {
		qualityClassifierStage.deployVerticle();
	}

	@Test
	public void testQualityClassifierMixTags() throws Exception {

		Whitebox.invokeMethod(qualityClassifierStage, "generateParsedQualityRangeMap");
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenReturn(getMockBiometricRecord());

		mockScores(40, 20, 98);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertTrue(result.getIsValid());

		ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor.forClass(Map.class);

		verify(packetManagerService, atLeastOnce()).addOrUpdateTags(any(), argument.capture());

		assertQualityTags(argument.getAllValues().get(0), level_5, level_3, level_10);

	}

	@Test
	public void testQualityClassifierAllGoodTags() throws ApisResourceAccessException, IOException,
			PacketManagerException, JsonProcessingException, BiometricException {

		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenReturn(getMockBiometricRecord());

		mockScores(90, 94, 98);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertTrue(result.getIsValid());

		ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor.forClass(Map.class);

		verify(packetManagerService, atLeastOnce()).addOrUpdateTags(any(), argument.capture());

		assertQualityTags(argument.getAllValues().get(0), level_10, level_10, level_10);

	}

	private void assertQualityTags(Map<String, String> qualityTags, String irisClassification,
			String fingerClassification, String faceClassification) {

		assertTrue(qualityTags.get(qualityPrefixTag + BiometricType.FACE.value()).equalsIgnoreCase(faceClassification));
		assertTrue(qualityTags.get(qualityPrefixTag + BiometricType.FINGER.value())
				.equalsIgnoreCase(fingerClassification));
		assertTrue(qualityTags.get(qualityPrefixTag + BiometricType.IRIS.value()).equalsIgnoreCase(irisClassification));
	}

	private void mockScores(float irisScore, float fingerScore, float faceScore) {
		float[] faceScores = new float[1];
		faceScores[0] = faceScore;
		Mockito.when(faceIBioProviderApi.getSegmentQuality(any(), any())).thenReturn(faceScores);

		float[] irisScores = new float[1];
		irisScores[0] = irisScore;
		Mockito.when(irisIBioProviderApi.getSegmentQuality(any(), any())).thenReturn(irisScores);

		float[] fingerScores = new float[1];
		fingerScores[0] = fingerScore;
		Mockito.when(fingerIBioProviderApi.getSegmentQuality(any(), any())).thenReturn(fingerScores);
	}

	private BiometricRecord getMockBiometricRecord() {
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType2 = new io.mosip.kernel.biometrics.entities.BDBInfo.BDBInfoBuilder()
				.build();
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
		return biometricRecord;
	}

	@Test
	public void testException() throws Exception {
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenThrow(new PacketManagerException("code", "message"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityClassifierStage.process(dto);
		assertTrue(messageDTO.getIsValid());
		assertTrue(messageDTO.getInternalError());
	}

	@Test
	public void testCbeffNotFound()
			throws IOException, PacketManagerException, JsonProcessingException, ApisResourceAccessException {
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenThrow(new IOException("message"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION))
		.thenReturn("ERROR");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityClassifierStage.process(dto);
		assertFalse(messageDTO.getIsValid());
		assertTrue(messageDTO.getInternalError());
	}

	@Test
	public void testApiNotAccessibleTest()
			throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenThrow(new ApisResourceAccessException("message"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO messageDTO = qualityClassifierStage.process(dto);
		assertTrue(messageDTO.getIsValid());
		assertTrue(messageDTO.getInternalError());
	}


	@Test
	public void testFileNameMissing()
			throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any())).thenReturn(null)
				.thenReturn(null);
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testBiometricException() throws BiometricException {
		Mockito.when(bioApiFactory.getBioProvider(any(), any()))
				.thenThrow(new BiometricException("", "error from provider"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testQualityCheckfailureException() throws BiometricException {
		Mockito.when(bioApiFactory.getBioProvider(any(), any())).thenReturn(null);
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testFileMissing()
			throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(null);
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testBioetricFileNotPresentInIdObject()
			throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

		when(basedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any())).thenReturn(null);

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);

		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testNoBiometricInPacket()
			throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any())).thenReturn(null);
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testJsonProcessingException()
			throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(basedPacketManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any()))
				.thenThrow(new JsonProcessingException("Json exception"));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void PacketManagerNonRecoverableExceptionTest() throws PacketManagerException, IOException, ApisResourceAccessException, JsonProcessingException {
		when(basedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any()))
				.thenThrow(new PacketManagerNonRecoverableException("code","message"));
		when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION))
				.thenReturn("Failed");
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890");
		MessageDTO result = qualityClassifierStage.process(dto);
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

}
