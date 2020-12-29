package io.mosip.registration.processor.abis.handler.stage.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.abis.handler.dto.DataShare;
import io.mosip.registration.processor.abis.handler.dto.DataShareResponseDto;
import io.mosip.registration.processor.abis.handler.stage.AbisHandlerStage;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.ApiName;
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
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
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
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class AbisHandlerStageTest {

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private Utilities utility;

	@Mock
	private PacketManagerService packetManagerService;

	@Mock
	private LogDescription description;

	List<AbisApplicationDto> abisApplicationDtos = new ArrayList<>();

	List<RegBioRefDto> bioRefDtos = new ArrayList<>();

	List<RegDemoDedupeListDto> regDemoDedupeListDtoList = new ArrayList<>();

	List<AbisRequestDto> abisRequestDtoList = new ArrayList<>();

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
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(abisHandlerStage, "maxResults", "30");
		ReflectionTestUtils.setField(abisHandlerStage, "targetFPIR", "30");
		ReflectionTestUtils.setField(abisHandlerStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(abisHandlerStage, "clusterManagerUrl", "/dummyPath");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		AbisApplicationDto dto = new AbisApplicationDto();
		dto.setCode("ABIS1");
		abisApplicationDtos.add(dto);

		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(description.getMessage()).thenReturn("description");

		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
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
		when(utility.getDefaultSource(any(), any())).thenReturn("reg-client");
		when(cbeffutil.createXML(any())).thenReturn("abishandlerstage".getBytes());

		Mockito.when(packetManagerService.getBiometrics(any(),any(),any(),any())).thenReturn(biometricRecord);

		Mockito.doNothing().when(registrationStatusDto).setLatestTransactionStatusCode(any());
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

		Mockito.when(registrationProcessorRestClientService.postApi(any(ApiName.class), any(MediaType.class), any(),any(),any(), any(), any())).thenReturn(dataShareResponseDto);
	}

	@Test
	public void testDeployVerticle() {
		abisHandlerStage.deployVerticle();
	}

	@Test
	public void testDemoToAbisHandlerTOMiddlewareSuccess() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("DEMOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		Mockito.when(packetInfoManager.getAllAbisDetails()).thenReturn(abisApplicationDtos);

		Mockito.when(packetInfoManager.getBioRefIdByRegId(any())).thenReturn(bioRefDtos);

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
	public void testBioToAbisHandlerToMiddlewareSuccess() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("DEMOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
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

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("abis-middle-ware-bus-in"));
	}

	@Test
	public void testMiddlewareToAbisHandlerToDemoSuccess() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("DEMOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.TRUE);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("demo-dedupe-bus-in"));
	}

	@Test
	public void testMiddlewareToAbisHandlerToBioSuccess() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("BIOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.TRUE);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("bio-dedupe-bus-in"));
	}

	@Test
	public void testDemoDedupeDataNotFound() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("DEMOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
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

	}

	@Test
	public void testReprocessInsert() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("BIOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);
		List<String> appCodeList = new ArrayList<>();
		appCodeList.add("ABIS1");
		Mockito.when(packetInfoManager.getAbisProcessedRequestsAppCodeByBioRefId(any(), any(), any())).thenReturn(appCodeList);

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

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getMessageBusAddress().getAddress().equalsIgnoreCase("abis-middle-ware-bus-in"));
	}

	@Test
	public void testAbisDetailsNotFound() throws RegistrationProcessorCheckedException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("BIOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
		Mockito.when(packetInfoManager.getIdentifyByTransactionId(any(), any())).thenReturn(Boolean.FALSE);

		List<AbisQueueDetails> abisQueueDetails = new ArrayList<>();
		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueDetails);

		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testCreateRequestException() throws JsonProcessingException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("BIOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
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
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any(AbisInsertRequestDto.class))).thenThrow(JsonProcessingException.class);
		
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testIdentifyRequestException() throws JsonProcessingException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode()).thenReturn("BIOGRAPHIC_VERIFICATION");
		Mockito.when(registrationStatusDto.getLatestRegistrationTransactionId())
				.thenReturn("dd7b7d20-910a-4b84-be21-c9f211318563");
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
		PowerMockito.when(JsonUtils.javaObjectToJsonString(any(AbisInsertRequestDto.class))).thenReturn("AbisInsertRequestDto");
		PowerMockito.when(JsonUtils.javaObjectToJsonString(ArgumentMatchers.any(AbisIdentifyRequestDto.class))).thenThrow(JsonProcessingException.class);
		
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		MessageDTO result = abisHandlerStage.process(dto);

		assertTrue(result.getInternalError());
	}


}
