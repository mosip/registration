package io.mosip.registartion.processor.abis.middleware.stage;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.core.util.PropertiesUtil;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.util.ByteSequence;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorUnCheckedException;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidateListDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidatesDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.impl.exception.ConnectionUnavailableException;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.AbisRequestEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponseDetEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponseEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class AbisMiddleWareStageTest {

	@Mock
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private BasePacketRepository<AbisRequestEntity, String> abisRequestRepositary;

	@Mock
	private BasePacketRepository<AbisResponseEntity, String> abisResponseRepositary;

	@Mock
	private BasePacketRepository<AbisResponseDetEntity, String> abisResponseDetailRepositary;

	@Mock
	private Utilities utility;

	@Mock
	private RegistrationStatusDao registrationStatusDao;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private PacketInfoDao packetInfoDao;

	private RegistrationStatusEntity regStatusEntity;
	private List<String> abisRefList;
	private List<AbisRequestDto> abisInsertIdentifyList;
	private List<MosipQueue> mosipQueueList;
	private int messageTTL = 0;

	@InjectMocks
	AbisMiddleWareStage stage = new AbisMiddleWareStage() {

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
				MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		}

		@Override
		public void consume(MosipEventBus mosipEventBus, MessageBusAddress fromAddress, 
			long messageExpiryTimeLimit) {

		}
		
		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;
		}

		@Override
		public Integer getPort() {
			return 8080;
		};

	};

	@Mock
	private PropertiesUtil propertiesUtil;

	@Before
	public void setUp() throws RegistrationProcessorCheckedException {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(stage, "messageFormat", "byte");
		ReflectionTestUtils.setField(stage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(stage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(stage, "clusterManagerUrl", "/dummyPath");
		InternalRegistrationStatusDto internalRegStatusDto = new InternalRegistrationStatusDto();
		internalRegStatusDto.setRegistrationId("");
		internalRegStatusDto.setLatestTransactionStatusCode("Demodedupe");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(internalRegStatusDto);

		regStatusEntity = new RegistrationStatusEntity();
		regStatusEntity.setLatestRegistrationTransactionId("1234");
		Mockito.when(registrationStatusDao.find(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString())).thenReturn(regStatusEntity);

		abisRefList = new ArrayList<>();
		abisRefList.add("88");
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(abisRefList);

		abisInsertIdentifyList = new ArrayList<>();
		AbisRequestDto insertAbisReq = new AbisRequestDto();
		insertAbisReq.setRefRegtrnId("10001100010027120190430071052");
		insertAbisReq.setAbisAppCode("Abis1");
		insertAbisReq.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		insertAbisReq.setRequestType("INSERT");
		insertAbisReq.setId("f4b1f6fd-466c-462f-aa8b-c218596542ec");
		insertAbisReq.setStatusCode("IN_PROGRESS");
		insertAbisReq.setReqText("mosip".getBytes());

		AbisRequestDto insertAlreadyProcessed = new AbisRequestDto();
		insertAlreadyProcessed.setRefRegtrnId("de7c4893-bf6f-46b4-a4d5-5cd458d5c7e2");
		insertAlreadyProcessed.setAbisAppCode("Abis2");
		insertAlreadyProcessed.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		insertAlreadyProcessed.setRequestType("INSERT");
		insertAlreadyProcessed.setId("f4b1f6fd-466c-462f-aa8b-c218596542ed");
		insertAlreadyProcessed.setStatusCode("ALREADY_PROCESSED");
		insertAlreadyProcessed.setReqText("mosip".getBytes());

		AbisRequestDto identifyAbisReq = new AbisRequestDto();
		identifyAbisReq.setRefRegtrnId("de7c4893-bf6f-46b4-a4d5-5cd458d5c7e2");
		identifyAbisReq.setAbisAppCode("Abis1");
		identifyAbisReq.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		identifyAbisReq.setRequestType("IDENTIFY");
		insertAbisReq.setId("f4b1f6fd-466c-462f-aa8b-c218596542ee");
		insertAbisReq.setReqText("mosip".getBytes());

		AbisRequestDto identifyAbisReq1 = new AbisRequestDto();
		identifyAbisReq1.setRefRegtrnId("de7c4893-bf6f-46b4-a4d5-5cd458d5c7e2");
		identifyAbisReq1.setAbisAppCode("Abis2");
		identifyAbisReq1.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		identifyAbisReq1.setRequestType("IDENTIFY");
		identifyAbisReq1.setId("f4b1f6fd-466c-462f-aa8b-c218596542ef");
		identifyAbisReq1.setReqText("mosip".getBytes());

		abisInsertIdentifyList.add(insertAbisReq);
		abisInsertIdentifyList.add(identifyAbisReq);
		abisInsertIdentifyList.add(identifyAbisReq1);
		abisInsertIdentifyList.add(insertAlreadyProcessed);

		mosipQueueList = new ArrayList<>();
		MosipQueue queue1 = new MosipQueue() {
			@Override
			public String getQueueName() {
				// TODO Auto-generated method stub
				return "Abis1";
			}

			@Override
			public void createConnection(String username, String password, String brokerUrl) {
				// TODO Auto-generated method stub

			}
		};

		MosipQueue queue2 = new MosipQueue() {
			@Override
			public String getQueueName() {
				// TODO Auto-generated method stub
				return "Abis2";
			}

			@Override
			public void createConnection(String username, String password, String brokerUrl) {
				// TODO Auto-generated method stub

			}
		};
		mosipQueueList.add(queue1);
		mosipQueueList.add(queue2);

		List<String> abisInboundAddresses = new ArrayList<>();
		abisInboundAddresses.add("abis1-inbound-address");

		List<String> abisOutboundAddresses = new ArrayList<>();
		abisOutboundAddresses.add("abis1-outboundaddress");
		List<List<String>> abisInboundOutBounAddressList = new ArrayList<>();
		abisInboundOutBounAddressList.add(abisInboundAddresses);
		abisInboundOutBounAddressList.add(abisOutboundAddresses);

		AbisQueueDetails abisQueue = new AbisQueueDetails();
		abisQueue.setMosipQueue(queue1);
		abisQueue.setInboundQueueName("abis1-inbound-Queue");
		abisQueue.setOutboundQueueName("abis1-outbound-Queue");
		abisQueue.setName("Abis1");

		AbisQueueDetails abisQueue1 = new AbisQueueDetails();
		abisQueue1.setMosipQueue(queue2);
		abisQueue1.setInboundQueueName("abis2-inbound-Queue");
		abisQueue1.setOutboundQueueName("abis2-outbound-Queue");
		abisQueue1.setName("Abis2");
		List<AbisQueueDetails> abisQueueList = new ArrayList<>();
		abisQueueList.add(abisQueue);
		abisQueueList.add(abisQueue1);

		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueList);
		Mockito.when(packetInfoManager.getBatchIdByRequestId(ArgumentMatchers.any()))
				.thenReturn("69098823-eba8-4aa9-bb64-9e0d36bd64a9");
		List<String> bioRefId = new ArrayList<>();
		bioRefId.add("d1070375-0960-4e90-b12c-72ab6186444d");
		Mockito.when(packetInfoManager.getReferenceIdByBatchId(Mockito.anyString())).thenReturn(bioRefId);
		List<RegBioRefDto> regBioRefist = new ArrayList<RegBioRefDto>();
		RegBioRefDto bioRefDto = new RegBioRefDto();
		bioRefDto.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		bioRefDto.setRegId("10001100010027120190430071052");
		regBioRefist.add(bioRefDto);
		Mockito.when(packetInfoManager.getRegBioRefDataByBioRefIds(Mockito.any())).thenReturn(regBioRefist);
		List<String> transIdList = new ArrayList<>();
		transIdList.add("1234");
		Mockito.when(packetInfoManager.getAbisTransactionIdByRequestId(Mockito.anyString())).thenReturn(transIdList);
		//
		// packetInfoManager.getIdentifyReqListByTransactionId(
		// transactionIdList.get(0), AbisStatusCode.IDENTIFY.toString())
		List<AbisRequestDto> abisIdentifyRequestDtoList = new ArrayList<>();
		AbisRequestDto abisIdentifyRequestDto = new AbisRequestDto();
		abisIdentifyRequestDto.setAbisAppCode("Abis1");
		abisIdentifyRequestDto.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		abisIdentifyRequestDto.setId("98509c18-ff22-46b7-a8c7-b4dec1d00c85");
		abisIdentifyRequestDto.setReqText("mosip".getBytes());
		abisIdentifyRequestDto.setReqBatchId("d87e6e28-4234-4433-b45d-0313c2aeca01");
		abisIdentifyRequestDtoList.add(abisIdentifyRequestDto);
		Mockito.when(packetInfoManager.getIdentifyReqListByTransactionId(Mockito.any(), Mockito.any()))
				.thenReturn(abisIdentifyRequestDtoList);

		// Mockito.when(utility.getInboundOutBoundAddressList()).thenReturn(abisInboundOutBounAddressList);
		messageTTL = 30 * 60;

		Mockito.when(propertiesUtil.getProperty(any(), any(Class.class), anyBoolean())).thenReturn(true);
	}

	@Test
	public void processTest() throws RegistrationProcessorCheckedException {

		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);

		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(true);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		dto.setWorkflowInstanceId("workflowInstanceId");
		dto.setReg_type("NEW");

		stage.deployVerticle();
		stage.process(dto);
		assertTrue(dto.getIsValid());

		// test for insert request list is empty
		List<AbisRequestDto> abisInsertIdentifyList = new ArrayList<>();
		AbisRequestDto identifyAbisReq = new AbisRequestDto();
		identifyAbisReq.setRefRegtrnId("d87e6e28-4234-4433-b45d-0313c2aeca01");
		identifyAbisReq.setAbisAppCode("Abis1");
		identifyAbisReq.setBioRefId("d1070375-0960-4e90-b12c-72ab6186444d");
		identifyAbisReq.setRequestType("IDENTIFY");
		identifyAbisReq.setReqText("mosip".getBytes());
		abisInsertIdentifyList.add(identifyAbisReq);
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);
		stage.process(dto);
		assertTrue(dto.getIsValid());
	}

	@Test
	public void testProcessForMessageFormatText() throws RegistrationProcessorCheckedException {

		ReflectionTestUtils.setField(stage, "messageFormat", "text");
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);

		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
				.thenThrow(ConnectionUnavailableException.class);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		dto.setWorkflowInstanceId("workflowInstanceId");
		dto.setReg_type("NEW");

		stage.deployVerticle();
		stage.process(dto);
		assertTrue(dto.getIsValid());
		assertTrue(dto.getInternalError());
	}
	
	@Test
	public void testInsertIdentitySuccessText() throws RegistrationProcessorCheckedException {
		// test for insert identity success
		ReflectionTestUtils.setField(stage, "messageFormat", "text");
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);

		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
		.thenReturn(true);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		dto.setWorkflowInstanceId("workflowInstanceId");
		dto.setReg_type("NEW");
		
		stage.deployVerticle();
		stage.process(dto);
		assertTrue(dto.getIsValid());
	}

	@Test
	public void testVariousScenarious() throws RegistrationProcessorCheckedException {
		// Mockito.when(utility.getMosipQueuesForAbis()).thenReturn(mosipQueueList);
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);
		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(true);
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(null);
		stage.deployVerticle();
		stage.process(dto);
		assertTrue(dto.getIsValid());
		assertTrue(dto.getInternalError());

		// test for null transactionId
		Mockito.when(registrationStatusDao.find(Mockito.anyString(),Mockito.anyString(),Mockito.anyInt(), Mockito.anyString())).thenReturn(null);
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(abisRefList);
		stage.process(dto);
		assertTrue(dto.getIsValid());
		assertTrue(dto.getInternalError());

		// test for empty insertidentify request List
		Mockito.when(registrationStatusDao.find(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString())).thenReturn(regStatusEntity);
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(new ArrayList<AbisRequestDto>());
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(abisRefList);
		stage.process(dto);
		assertTrue(dto.getIsValid());
		assertTrue(dto.getInternalError());

		// test for send to queue failed
		Mockito.when(registrationStatusDao.find(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString())).thenReturn(regStatusEntity);
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(abisRefList);
		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(false);
		stage.process(dto);

		// test for exception while sending to queue
		Mockito.when(registrationStatusDao.find(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString())).thenReturn(regStatusEntity);
		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(abisInsertIdentifyList);
		Mockito.when(packetInfoManager.getReferenceIdByWorkflowInstanceId(Mockito.anyString())).thenReturn(abisRefList);
		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.anyString(), Mockito.any()))
				.thenThrow(new NullPointerException());
		stage.process(dto);
		assertTrue(dto.getIsValid());

	}

	// test for unknown exception occured
	@Test
	public void testException() {
//		Mockito.when(packetInfoManager.getInsertOrIdentifyRequest(Mockito.anyString(), Mockito.anyString()))
//				.thenReturn(abisInsertIdentifyList);
//		Mockito.when(packetInfoManager.getReferenceIdByRid(Mockito.anyString())).thenReturn(abisRefList);
//		Mockito.when(mosipQueueManager.send(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
//		Mockito.when(registrationStatusDao.findById(Mockito.anyString())).thenThrow(new TablenotAccessibleException());
		MessageDTO dto = new MessageDTO();
		dto.setRid("10003100030001520190422074511");
		stage.process(dto);
		assertTrue(dto.getIsValid());
		assertTrue(dto.getInternalError());
	}
		
	

	@Test
	public void testConsumerListener() throws RegistrationProcessorCheckedException {

		String failedInsertResponse = "{\"id\":\"mosip.abis.insert\",\"requestId\":\"5b64e806-8d5f-4ba1-b641-0b55cf40c0e1\",\"responsetime\":"
				+ null + ",\"returnValue\":2,\"failureReason\":7}\r\n"
				+ "";
		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(failedInsertResponse.getBytes());
		amq.setContent(byteSeq);
		Vertx vertx = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus evenBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto = new AbisRequestDto();
		abisCommonRequestDto.setRequestType("INSERT");
		abisCommonRequestDto.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);

		String sucessfulResponse = "{\"id\":\"mosip.abis.insert\",\"requestId\":\"5b64e806-8d5f-4ba1-b641-0b55cf40c0e1\",\"responsetime\":"
				+ null + ",\"returnValue\":1,\"failureReason\":null}\r\n"
				+ "";
		byteSeq.setData(sucessfulResponse.getBytes());
		amq.setContent(byteSeq);
		AbisRequestDto abisCommonRequestDto1 = new AbisRequestDto();
		abisCommonRequestDto1.setRequestType("INSERT");
		abisCommonRequestDto1.setAbisAppCode("Abis1");
		abisCommonRequestDto1.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto1);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
		// test for multiple response for same request id
		abisCommonRequestDto1.setStatusCode("PROCESSED");
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);	
	}
	

	@Test
	public void testConsumerListenerForIdentifyReq() throws RegistrationProcessorCheckedException, IOException {

		String failedInsertResponse = "{\"id\":\"mosip.id.identify\",\"requestId\":\"01234567-89AB-CDEF-0123-456789ABCDEF\",\"responsetime\":\"2020-03-29T07:01:24.692Z\",\"returnValue\":\"2\",\"failureReason\":\"7\"}";
		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(failedInsertResponse.getBytes());
		amq.setContent(byteSeq);
		Vertx vertx = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus evenBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto = new AbisRequestDto();
		abisCommonRequestDto.setRequestType("IDENTIFY");
		abisCommonRequestDto.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
		
		// Exception
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(
				JsonUtil.readValueWithUnknownProperties(Mockito.anyString(), Mockito.eq(AbisIdentifyResponseDto.class)))
				.thenThrow(NullPointerException.class);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
	}
	
	@Test(expected = RegistrationProcessorCheckedException.class)
	public void testConsumerListenerForIdentifyReqIOException() throws RegistrationProcessorCheckedException, IOException {

		// test for IO Exception
		String failedInsertResponse = "{\"id\":\"mosip.id.identify\",\"requestId\":\"01234567-89AB-CDEF-0123-456789ABCDEF\",\"responsetime\":\"2020-03-29T07:01:24.692Z\",\"returnValue\":\"2\",\"failureReason\":\"7\"}";
		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(failedInsertResponse.getBytes());
		amq.setContent(byteSeq);
		MosipEventBus evenBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue = Mockito.mock(MosipQueue.class);
		
		AbisRequestDto abisCommonRequestDto = new AbisRequestDto();
		abisCommonRequestDto.setRequestType("IDENTIFY");
		abisCommonRequestDto.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(
				JsonUtil.readValueWithUnknownProperties(Mockito.anyString(), Mockito.eq(AbisIdentifyResponseDto.class)))
				.thenThrow(IOException.class);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
	}
	
	
	@Test
	public void batchIdNull() throws RegistrationProcessorCheckedException {
		String sucessfulResponse = "{\"id\":\"mosip.abis.insert\",\"requestId\":\"5b64e806-8d5f-4ba1-b641-0b55cf40c0e1\",\"responsetime\":"
				+ null + ",\"returnValue\":1,\"failureReason\":null}\r\n"
				+ "";
		ActiveMQBytesMessage amq1 = new ActiveMQBytesMessage();
		ByteSequence byteSeq1 = new ByteSequence();
		byteSeq1.setData(sucessfulResponse.getBytes());
		amq1.setContent(byteSeq1);
		Vertx vertx1 = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus eventBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue1 = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto1 = new AbisRequestDto();
		abisCommonRequestDto1.setRequestType("INSERT");
		abisCommonRequestDto1.setAbisAppCode("Abis1");
		//Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto1);
		Mockito.when(packetInfoManager.getBatchIdByRequestId(Mockito.anyString())).thenReturn(null);
		stage.consumerListener(amq1, "abis1_inboundAddress", queue1, eventBus, messageTTL);

	}

	//
	@Test
	public void testIdentifyConsumerListener() throws RegistrationProcessorCheckedException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		Mockito.when(packetInfoManager.getBatchStatusbyBatchId(Mockito.anyString())).thenReturn(null);

		// test for identify succes response - no duplicates
		String identifySucessfulResponse = "{\"id\":\"mosip.abis.identify\",\"requestId\":\"8a3effd4-5fba-44e0-8cbb-3083ba098209\",\"responsetime\":"
				+ null + ",\"returnValue\":1,\"failureReason\":null,\"candidateList\":null}";
		ActiveMQBytesMessage amq1 = new ActiveMQBytesMessage();
		ByteSequence byteSeq1 = new ByteSequence();
		byteSeq1.setData(identifySucessfulResponse.getBytes());
		amq1.setContent(byteSeq1);
		Vertx vertx1 = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus evenBus1 = Mockito.mock(MosipEventBus.class);
		MosipQueue queue1 = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto1 = new AbisRequestDto();
		abisCommonRequestDto1.setRequestType("IDENTIFY");
		abisCommonRequestDto1.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto1);
		stage.consumerListener(amq1, "abis1_inboundAddress", queue1, evenBus1, messageTTL);
		// test for multiple response for same request id
		abisCommonRequestDto1.setStatusCode("PROCESSED");
		stage.consumerListener(amq1, "abis1_inboundAddress", queue1, evenBus1, messageTTL);
		// test for identify failed response
		String identifyFailedResponse = "{\"id\":\"mosip.abis.identify\",\"requestId\":\"8a3effd4-5fba-44e0-8cbb-3083ba098209\",\"responsetime\":"
				+ null + ",\"returnValue\":2,\"failureReason\":3,\"candidateList\":null}";
		byteSeq1.setData(identifyFailedResponse.getBytes());
		amq1.setContent(byteSeq1);
		abisCommonRequestDto1.setStatusCode("SENT");
		stage.consumerListener(amq1, "abis1_inboundAddress", queue1, evenBus1, messageTTL);
		
		// test for identify response - with duplicates
		List<String> bioRefId = new ArrayList<>();
		bioRefId.add("d1070375-0960-4e90-b12c-72ab6186764c");
		Mockito.when(packetInfoManager.getReferenceIdByBatchId(Mockito.anyString())).thenReturn(bioRefId);
		Mockito.when(packetInfoDao.getRegIdByBioRefId(ArgumentMatchers.any())).thenReturn("Test123");
		String duplicateIdentifySuccessResponse = "{\"id\":\"mosip.abis.identify\",\"requestId\":\"f4b1f6fd-466c-462f-aa8b-c218596542ec\",\"responsetime\":"
				+ null
				+ ",\"returnValue\":1,\"failureReason\":null,\"candidateList\":{\"count\":\"1\",\"candidates\":[{\"referenceId\":\"d1070375-0960-4e90-b12c-72ab6186444d\",\"analytics\":null,\"modalities\":null}]}}";
		byteSeq1.setData(duplicateIdentifySuccessResponse.getBytes());
		amq1.setContent(byteSeq1);
		abisCommonRequestDto1.setStatusCode("SENT");
		stage.consumerListener(amq1, "abis1_inboundAddress", queue1, evenBus1, messageTTL);

	}
	
	@Test(expected = RegistrationProcessorUnCheckedException.class)
	public void testDeployVerticle() throws RegistrationProcessorCheckedException {
		Mockito.when(utility.getAbisQueueDetails()).thenThrow(RegistrationProcessorCheckedException.class);
		stage.deployVerticle();
	}
	
	@Test
	public void testConsumerListenerForIdentifyRequest() throws RegistrationProcessorCheckedException, IOException {
		String failedInsertResponse = "{\"id\":\"mosip.id.identify\",\"requestId\":\"01234567-89AB-CDEF-0123-456789ABCDEF\",\"responsetime\":\"2020-03-29T07:01:24.692Z\",\"returnValue\":\"2\",\"failureReason\":\"7\"}";
		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(failedInsertResponse.getBytes());
		amq.setContent(byteSeq);
		Vertx vertx = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus evenBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto = new AbisRequestDto();
		abisCommonRequestDto.setRequestType("IDENTIFY");
		abisCommonRequestDto.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto);
		
		AbisIdentifyResponseDto abisIdentifyResponseDto = new AbisIdentifyResponseDto();
		abisIdentifyResponseDto.setReturnValue("1");
		abisIdentifyResponseDto.setFailureReason("Test");
		abisIdentifyResponseDto.setRequestId("Test1");
		CandidateListDto candidateList = new CandidateListDto();
		CandidatesDto candidate = new CandidatesDto();
		candidate.setReferenceId("e1070375-0960-4e90-b12c-72ab6186444d");
		CandidatesDto[] candidates = new CandidatesDto[1];
		candidates[0] = candidate;
		candidateList.setCandidates(candidates);
		abisIdentifyResponseDto.setCandidateList(candidateList);
		String response = new String(((ActiveMQBytesMessage) amq).getContent().data);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.objectMapperReadValue(response, AbisIdentifyResponseDto.class)).thenReturn(abisIdentifyResponseDto);
		PowerMockito.when(JsonUtil.readValueWithUnknownProperties(failedInsertResponse,
				AbisIdentifyResponseDto.class)).thenReturn(abisIdentifyResponseDto);
		Mockito.when(packetInfoDao.getRegIdByBioRefId(ArgumentMatchers.any())).thenReturn("Test123");

		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
	}
	
	@Test(expected = RegistrationProcessorCheckedException.class)
	public void testConsumerListenerForIdentifyReqException() throws RegistrationProcessorCheckedException, IOException {
		String failedInsertResponse = "{\"id\":\"mosip.id.identify\",\"requestId\":\"01234567-89AB-CDEF-0123-456789ABCDEF\",\"responsetime\":\"2020-03-29T07:01:24.692Z\",\"returnValue\":\"2\",\"failureReason\":\"7\"}";
		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(failedInsertResponse.getBytes());
		amq.setContent(byteSeq);
		Vertx vertx = Mockito.mock(Vertx.class);
		//MosipEventBus evenBus = new MosipEventBusFactory().getEventBus(vertx, "vertx", "mosip.regproc.abis.middleware.");
		MosipEventBus evenBus = Mockito.mock(MosipEventBus.class);
		MosipQueue queue = Mockito.mock(MosipQueue.class);
		AbisRequestDto abisCommonRequestDto = new AbisRequestDto();
		abisCommonRequestDto.setRequestType("IDENTIFY");
		abisCommonRequestDto.setStatusCode("SENT");
		Mockito.when(packetInfoManager.getAbisRequestByRequestId(Mockito.any())).thenReturn(abisCommonRequestDto);
		
		AbisIdentifyResponseDto abisIdentifyResponseDto = new AbisIdentifyResponseDto();
		abisIdentifyResponseDto.setReturnValue("1");
		abisIdentifyResponseDto.setFailureReason("Test");
		abisIdentifyResponseDto.setRequestId("Test1");
		CandidateListDto candidateList = new CandidateListDto();
		CandidatesDto candidate = new CandidatesDto();
		candidate.setReferenceId("e1070375-0960-4e90-b12c-72ab6186444d");
		CandidatesDto[] candidates = new CandidatesDto[1];
		candidates[0] = candidate;
		candidateList.setCandidates(candidates);
		abisIdentifyResponseDto.setCandidateList(candidateList);
		String response = new String(((ActiveMQBytesMessage) amq).getContent().data);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.readValueWithUnknownProperties(response, AbisIdentifyResponseDto.class)).thenThrow(IOException.class);
		stage.consumerListener(amq, "abis1_inboundAddress", queue, evenBus, messageTTL);
	}
}