package io.mosip.registration.processor.abis.handler.stage.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.abis.handler.dto.DataShare;
import io.mosip.registration.processor.abis.handler.dto.DataShareResponseDto;
import io.mosip.registration.processor.abis.handler.stage.AbisHandlerStage;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisApplicationDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegDemoDedupeListDto;
import io.mosip.registration.processor.core.packet.dto.datashare.Filter;
import io.mosip.registration.processor.core.packet.dto.datashare.ShareableAttributes;
import io.mosip.registration.processor.core.packet.dto.datashare.Source;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtils.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class AbisHandlerStageTest {

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	private InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	@Mock
	private Utilities utility;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;
	
	@Mock
	private PacketManagerService packetService;

	@Mock
	private LogDescription description;

	List<AbisApplicationDto> abisApplicationDtos = new ArrayList<>();

	List<RegBioRefDto> bioRefDtos = new ArrayList<>();

	List<RegDemoDedupeListDto> regDemoDedupeListDtoList = new ArrayList<>();

	List<AbisRequestDto> abisRequestDtoList = new ArrayList<>();
	
	Map<String, String> tags = new HashMap<String, String>();

	@Mock
	private Environment env;

	@Mock
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	@Mock
	private CbeffUtil cbeffutil;

	@InjectMocks
	private AbisHandlerStage abisHandlerStage = new AbisHandlerStage() {
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
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		}

		@Override
		public Integer getPort() {
			return 8080;
		};
	};

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(abisHandlerStage, "maxResults", "30");
		ReflectionTestUtils.setField(abisHandlerStage, "targetFPIR", "30");
		ReflectionTestUtils.setField(abisHandlerStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(abisHandlerStage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(abisHandlerStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(abisHandlerStage, "httpProtocol", "http");
		ReflectionTestUtils.setField(abisHandlerStage, "internalDomainName", "localhost");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Map<String, Map<String, List<String>>> biometricModalitySegmentsMapforAgeGroup = new HashMap<String, Map<String, List<String>>>();
		Map<String, List<String>> biometricModalitySegmentsMap = new HashMap();
		Map<String, List<String>> biometricModalitySegmentsMapForInfant = new HashMap();
		biometricModalitySegmentsMapForInfant.put("Face", getFaceList());
		biometricModalitySegmentsMap.put("Finger", getFingerList());
		biometricModalitySegmentsMap.put("Iris", getIrisList());
		biometricModalitySegmentsMap.put("Face", getFaceList());
		biometricModalitySegmentsMapforAgeGroup.put("INFANT", biometricModalitySegmentsMapForInfant);
		biometricModalitySegmentsMapforAgeGroup.put("MINOR", biometricModalitySegmentsMap);
		biometricModalitySegmentsMapforAgeGroup.put("ADULT", biometricModalitySegmentsMap);
		biometricModalitySegmentsMapforAgeGroup.put("DEFAULT", biometricModalitySegmentsMap);

		ReflectionTestUtils.setField(abisHandlerStage, "biometricModalitySegmentsMapforAgeGroup", biometricModalitySegmentsMapforAgeGroup);
		ReflectionTestUtils.setField(abisHandlerStage, "exceptionSegmentsMap", getExceptionModalityMap());
		ReflectionTestUtils.setField(abisHandlerStage, "regClientVersionsBeforeCbeffOthersAttritube",
				Arrays.asList("1.1.3"));
		Mockito.when(env.getProperty("DATASHARECREATEURL")).thenReturn("/v1/datashare/create");
		AbisApplicationDto dto = new AbisApplicationDto();
		dto.setCode("ABIS1");
		abisApplicationDtos.add(dto);

		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(description.getMessage()).thenReturn("description");

		
		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(getBiometricRecord(Arrays.asList("Left Thumb" ,"Right Thumb" , "Left MiddleFinger" ,
				"Left RingFinger" ,"Left LittleFinger" ,"Left IndexFinger" ,"Right MiddleFinger" , 
				"Right RingFinger" ,"Right LittleFinger" ,"Right IndexFinger" ,
				"Left" ,"Right","Face"),false));
		mockDataSharePolicy(Lists.newArrayList(BiometricType.FINGER,BiometricType.IRIS,BiometricType.FACE));
		
		List<String> list = new LinkedList<>();
		setMetaInfoMap(list);

		//Mockito.doNothing().when(registrationStatusDto).setLatestTransactionStatusCode(any());
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());

		Mockito.when(packetInfoManager.getAbisRequestsByBioRefId(any())).thenReturn(abisRequestDtoList);

		AbisQueueDetails abisQueueDetails = new AbisQueueDetails();
		abisQueueDetails.setName("ABIS1");
		List<AbisQueueDetails> abisQueueDetailsList = new ArrayList<>();
		abisQueueDetailsList.add(abisQueueDetails);
		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueDetailsList);

		AuditResponseDto auditResponseDto = new AuditResponseDto();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(auditResponseDto);
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(responseWrapper);

		DataShareResponseDto dataShareResponseDto = new DataShareResponseDto();
		DataShare dataShare = new DataShare();
		dataShare.setUrl("http://localhost");
		dataShareResponseDto.setDataShare(dataShare);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.FACE, BiometricType.FINGER));

		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(MediaType.class), any(), any(),
				any(), any(), any())).thenReturn(dataShareResponseDto);
		
		Mockito.when(cbeffutil.createXML(any())).thenReturn(new byte[2048]);
	}

	private Map<String, String> getExceptionModalityMap() {
		Map<String, String> exceptionMap = new HashMap<String, String>();
		exceptionMap.put("Left Thumb" , "leftThumb");
		exceptionMap.put("Right Thumb" , "rightThumb");
		exceptionMap.put("Left MiddleFinger" , "leftMiddle");
		exceptionMap.put("Left RingFinger" , "leftRing");
		exceptionMap.put("Left LittleFinger" , "leftLittle");
		exceptionMap.put("Left IndexFinger" , "leftIndex");
		exceptionMap.put("Right MiddleFinger" , "rightMiddle");
		exceptionMap.put("Right RingFinger" , "rightRing");
		exceptionMap.put("Right LittleFinger" , "rightLittle");
		exceptionMap.put("Right IndexFinger" , "rightIndex");
		exceptionMap.put("Left" , "leftEye");
		exceptionMap.put("Right" , "rightEye");
		exceptionMap.put("Face" , "face");
		
		return exceptionMap;
	}

	private List<String> getFaceList() {
		return Arrays.asList("Face");
	}

	private List<String> getIrisList() {
		return Arrays.asList("Left", "Right");

	}

	private List<String> getFingerList() {
		return Arrays.asList("Left Thumb", "Left LittleFinger", "Left IndexFinger", "Left MiddleFinger",
				"Left RingFinger", "Right Thumb", "Right LittleFinger",
				"Right IndexFinger", "Right MiddleFinger",
				"Right RingFinger");
	}

	private void mockDataSharePolicy(List<BiometricType> sherableBiometricList) throws ApisResourceAccessException {
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
				.thenReturn(getMockDataSharePolicy(sherableBiometricList));
	}

	private ResponseWrapper<LinkedHashMap<String, Object>> getMockDataSharePolicy(
			List<BiometricType> sherableBiometricList) {

		ObjectMapper mapper = new ObjectMapper();

		List<ShareableAttributes> attr = new LinkedList<>();
		if (sherableBiometricList != null && !sherableBiometricList.isEmpty()) {

			ShareableAttributes shareableAttributes = new ShareableAttributes();
			List<Source> sourceList = new ArrayList<>();

			for (BiometricType bioType : sherableBiometricList) {
				Filter filter = new Filter();
				filter.setType(bioType.value());
				Source src = new Source();
				src.setFilter(Lists.newArrayList(filter));
				sourceList.add(src);
			}

			shareableAttributes.setSource(sourceList);
			attr = Lists.newArrayList(shareableAttributes);
		}

		ResponseWrapper<LinkedHashMap<String, Object>> policy = new ResponseWrapper<>();
		LinkedHashMap<String, Object> policies = new LinkedHashMap<>();
		LinkedHashMap<String, Object> sharableAttributes = new LinkedHashMap<>();
		sharableAttributes.put(PolicyConstant.SHAREABLE_ATTRIBUTES, attr);
		policies.put(PolicyConstant.POLICIES, sharableAttributes);
		policy.setResponse(policies);

		return policy;
	}

	@Test
	public void testDeployVerticle() {
		abisHandlerStage.deployVerticle();
	}

	@Test
	public void testDemoToAbisHandlerTOMiddlewareSuccess() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
		regDemoDedupeListDto.setMatchedRegId("10003100030001520190422074511");
		regDemoDedupeListDtoList.add(regDemoDedupeListDto);
		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("abis-middle-ware-bus-in"));
	}

	@Test
	public void testBioToAbisHandlerToMiddlewareSuccess() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		RegBioRefDto regBioRefDto = new RegBioRefDto();
		regBioRefDto.setBioRefId("1234567890");
		bioRefDtos.add(regBioRefDto);
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
		regDemoDedupeListDto.setMatchedRegId("10003100030001520190422074511");
		regDemoDedupeListDtoList.add(regDemoDedupeListDto);
		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("abis-middle-ware-bus-in"));
	}

	@Test
	public void testMiddlewareToAbisHandlerToDemoSuccess() {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.TRUE);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("demo-dedupe-bus-in"));
	}

	@Test
	public void testMiddlewareToAbisHandlerToBioSuccess() {
		registrationStatusDto.setLatestTransactionTypeCode("BIOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.TRUE);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("bio-dedupe-bus-in"));
	}

	@Test
	public void testDemoDedupeDataNotFound() {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testReprocessInsert() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("BIOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		List<String> appCodeList = new ArrayList<>();
		appCodeList.add("ABIS1");
		Mockito.when(packetInfoManager.getAbisProcessedRequestsAppCodeByBioRefId(any(), any(), any()))
				.thenReturn(appCodeList);

		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		RegBioRefDto bioRefDto = new RegBioRefDto();
		bioRefDtos.add(bioRefDto);
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		AbisRequestDto abisRequestDto = new AbisRequestDto();
		abisRequestDto.setAbisAppCode("ABIS1");
		abisRequestDto.setStatusCode("IN-PROGRESS");
		abisRequestDtoList.add(abisRequestDto);
		Mockito.when(packetInfoManager.getAbisRequestsByBioRefId(any())).thenReturn(abisRequestDtoList);
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testAbisDetailsNotFound() throws RegistrationProcessorCheckedException {
		registrationStatusDto.setLatestTransactionTypeCode("BIOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		List<AbisQueueDetails> abisQueueDetails = new ArrayList<>();
		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueDetails);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testPotentialMatchNotFound() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(Arrays.asList());

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testIdentifyRequestJsonProcessingException() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
		regDemoDedupeListDto.setMatchedRegId("10003100030001520190422074511");
		regDemoDedupeListDtoList.add(regDemoDedupeListDto);
		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any())).thenReturn("value").thenThrow(JsonProcessingException.class);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testInsertRequestJsonProcessingException() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "MINOR");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any())).thenThrow(JsonProcessingException.class);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testDataShareResponseNullException() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "MINOR");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		
		DataShareResponseDto dataShareResponseDto = new DataShareResponseDto();
		ErrorDTO error = new ErrorDTO("ERR-001", "exception occured");
		dataShareResponseDto.setDataShare(null);
		dataShareResponseDto.setErrors(Arrays.asList(error));
		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(MediaType.class), any(), any(),
				any(), any(), any())).thenReturn(dataShareResponseDto);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testCreateTypeSubtypeMappingResponseNullException()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		ErrorDTO error = new ErrorDTO("ERR-001", "exception occured");
		ResponseWrapper<LinkedHashMap<String, Object>> policyResponse = new ResponseWrapper<>();
		
		policyResponse.setErrors(Arrays.asList(error));
		
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(),
				anyString(), any())).thenReturn(policyResponse);
		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testvalidateBiometricRecordModalitiesEmptyException()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		
		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "MINOR");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		ResponseWrapper<LinkedHashMap<String, Object>> policyResponse = new ResponseWrapper<>();
		LinkedHashMap<String, Object> response = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
		ShareableAttributes shareableAttributes = new ShareableAttributes();
		shareableAttributes.setSource(Arrays.asList());
		attributes.put(PolicyConstant.SHAREABLE_ATTRIBUTES, Arrays.asList(shareableAttributes));
		response.put(PolicyConstant.POLICIES, attributes);
		policyResponse.setResponse(response);
		
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
				.thenReturn(policyResponse);
		
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testvalidateBiometricRecordSegmentEmptyException()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		
		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);
		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(null);
		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(biometricRecord);
		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testvalidateBiometricRecordOthersMapNullException()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		
		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));
		tags.put("AGE_GROUP", "ADULT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);
		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir = new BIR.BIRBuilder().build();
		BDBInfo bdbInfo = new BDBInfo.BDBInfoBuilder().build();
		bdbInfo.setSubtype(Arrays.asList("Left"));
		bdbInfo.setType(Arrays.asList(BiometricType.IRIS));
		bir.setOthers(null);
		bir.setBdbInfo(bdbInfo);
		biometricRecord.setSegments(Arrays.asList(bir));
		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(biometricRecord);
		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}

	@Test
	public void testCreateRequestException() throws JsonProcessingException {
		registrationStatusDto.setLatestTransactionTypeCode("BIOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		RegBioRefDto bioRefDto = new RegBioRefDto();
		bioRefDtos.add(bioRefDto);
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		AbisRequestDto abisRequestDto = new AbisRequestDto();
		abisRequestDto.setAbisAppCode("ABIS1");
		abisRequestDto.setStatusCode("IN-PROGRESS");
		abisRequestDtoList.add(abisRequestDto);
		Mockito.when(packetInfoManager.getAbisRequestsByBioRefId(any())).thenReturn(abisRequestDtoList);

		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any(AbisInsertRequestDto.class)))
				.thenThrow(JsonProcessingException.class);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testIdentifyRequestException() throws JsonProcessingException {
		registrationStatusDto.setLatestTransactionTypeCode("BIOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		RegBioRefDto bioRefDto = new RegBioRefDto();
		bioRefDtos.add(bioRefDto);
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());

		AbisRequestDto abisRequestDto = new AbisRequestDto();
		abisRequestDto.setAbisAppCode("ABIS1");
		abisRequestDto.setStatusCode("IN-PROGRESS");
		abisRequestDtoList.add(abisRequestDto);
		Mockito.when(packetInfoManager.getAbisRequestsByBioRefId(any())).thenReturn(abisRequestDtoList);

		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any(AbisInsertRequestDto.class)))
				.thenReturn("AbisInsertRequestDto");
		PowerMockito.when(JsonUtils.javaObjectToJsonString(ArgumentMatchers.any(AbisIdentifyRequestDto.class)))
				.thenThrow(JsonProcessingException.class);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}


	@Test
	public void bioRecordDataNotFound()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		defaultMockToProcess();

		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(null);
		mockDataSharePolicy(Lists.newArrayList(BiometricType.IRIS, BiometricType.FINGER, BiometricType.FACE));

		setMetaInfoMap(new LinkedList<>(getExceptionModalityMap().values()));
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void biometricsNotFoundWithSegmentConfig()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		defaultMockToProcess();

		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(getBiometricRecord(Arrays.asList("Left Thumb" ,"Right Thumb","Face"),false));
		

		setMetaInfoMap(Arrays.asList("leftEye"));
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}
	
	@Test
	public void biometricsTypeNotFoundConfig()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		defaultMockToProcess();

		Map<String, Map<String, List<String>>> biometricModalitySegmentsMapforAgeGroup = new HashMap<String, Map<String, List<String>>>();
		Map<String, List<String>> biometricModalitySegmentsMap = new HashMap();

		biometricModalitySegmentsMap.put("Finger", getFingerList());
		biometricModalitySegmentsMap.put("Iris", getIrisList());
		biometricModalitySegmentsMapforAgeGroup.put("DEFAULT", biometricModalitySegmentsMap);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testBiometricSegmentNotConfiguredInfant() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);
		tags.put("AGE_GROUP", "INFANT");
		Mockito.when(packetService.getAllTags(any())).thenReturn(tags);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
		regDemoDedupeListDto.setMatchedRegId("10003100030001520190422074511");
		regDemoDedupeListDtoList.add(regDemoDedupeListDto);
		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}



	@Test
	public void emptyBdbFound()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		defaultMockToProcess();

		boolean isBdbEmpty = true;
		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(getBiometricRecord(Arrays.asList("Left RingFinger"), isBdbEmpty ));
		mockDataSharePolicy(Lists.newArrayList(BiometricType.FINGER));
		
		List<String> list = new ArrayList<String>(getExceptionModalityMap().values());
		list.remove("leftRing");
		setMetaInfoMap(list);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}
	
	@Test
	public void noBdbInAnyBiometric()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

		defaultMockToProcess();

		Mockito.when(packetManagerService.getBiometrics(any(), any(), any(), any(), any())).thenReturn(getBiometricRecord(Arrays.asList("Left Thumb" ,"Right Thumb" , "Left MiddleFinger" ,
				"Left RingFinger" ,"Left LittleFinger" ,"Left IndexFinger" ,"Right MiddleFinger" , 
				"Right RingFinger" ,"Right LittleFinger" ,"Right IndexFinger" ,
				"Left" ,"Right","Face"),true));

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	private void defaultMockToProcess() {
		registrationStatusDto.setLatestTransactionTypeCode("DEMOGRAPHIC_VERIFICATION");
		registrationStatusDto.setLatestRegistrationTransactionId("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		RegBioRefDto regBioRefDto = new RegBioRefDto();
		regBioRefDto.setBioRefId("1234567890");
		bioRefDtos.add(regBioRefDto);
		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

		Mockito.doNothing().when(packetInfoManager).saveBioRef(any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveAbisRequest(any(), any(), any());

		RegDemoDedupeListDto regDemoDedupeListDto = new RegDemoDedupeListDto();
		regDemoDedupeListDto.setMatchedRegId("10003100030001520190422074511");
		regDemoDedupeListDtoList.add(regDemoDedupeListDto);
		Mockito.when(packetInfoManager.getDemoListByTransactionId(any())).thenReturn(regDemoDedupeListDtoList);

	}

	private void setMetaInfoMap(List<String> exceptionAttributes) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException{
		
		Map<String, String> metaInfoMap = new HashMap<>();
		
		Map<String, Map<String, Object>> exceptionBiometrcisMap = new HashMap<>();
		
		Map<String, Object> applicantExceptionBiometrcisMap = new HashMap<String, Object>();
		
		if(exceptionAttributes!=null) {
			for(String exceptionAttribute : exceptionAttributes) {
			
			Map<String, String> detailMap = new HashMap<String, String>();
			detailMap.put("missingBiometric", exceptionAttribute);
			detailMap.put("reason", "Temporary");
			detailMap.put("individualType", "applicant");
			
			applicantExceptionBiometrcisMap.put(exceptionAttribute, detailMap);
			
			}
		}
		

		exceptionBiometrcisMap.put("applicant", applicantExceptionBiometrcisMap);
        String gsonString =mapper.writeValueAsString(exceptionBiometrcisMap);

		metaInfoMap.put("exceptionBiometrics", gsonString);
		metaInfoMap.put(JsonConstant.METADATA,
				"[{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0\"\n}]");
		Mockito.when(packetManagerService.getMetaInfo(any(), any(), any())).thenReturn(metaInfoMap);
	}
	
	private BiometricRecord getBiometricRecord(List<String> bioAttributes, boolean isBdbEmpty) {
		BiometricRecord biometricRecord = new BiometricRecord();
		
		byte[] bdb = isBdbEmpty ? null : new byte[2048];
		for(String bioAttribute : bioAttributes) {
			BIR birType1 = new BIR.BIRBuilder().build();
			BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
			io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new RegistryIDType();
			registryIDType.setOrganization("Mosip");
			registryIDType.setType("257");
			io.mosip.kernel.biometrics.constant.QualityType quality = new QualityType();
			quality.setAlgorithm(registryIDType);
			quality.setScore(90l);
			bdbInfoType1.setQuality(quality);
			
			BiometricType singleType1 = bioAttribute.equalsIgnoreCase("face") ? BiometricType.FACE :
				bioAttribute.equalsIgnoreCase("left") || bioAttribute.equalsIgnoreCase("right") ? BiometricType.IRIS : BiometricType.FINGER  ;
			List<BiometricType> singleTypeList1 = new ArrayList<>();
			singleTypeList1.add(singleType1);
			bdbInfoType1.setType(singleTypeList1);
		
			
				
			String[] bioAttributeArray = bioAttribute.split(" ");

			List<String> subtype = new ArrayList<>();
			for(String attribute : bioAttributeArray) {
				subtype.add(attribute);
			}
			bdbInfoType1.setSubtype(subtype);

			birType1.setBdbInfo(bdbInfoType1);
			birType1.setBdb(bdb);
			
			if(bdb==null) {
				Map<String, Object> others = new HashMap<>();
				others.put("EXCEPTION", true);
				HashMap<String, String> entry = new HashMap<>();
				entry.put("EXCEPTION", "true");
				birType1.setOthers(entry);
				
			}
			
			biometricRecord.getSegments().add(birType1); 
		}
		
		return biometricRecord;
	}
	
}