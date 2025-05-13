package io.mosip.registration.processor.verification.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.util.ByteSequence;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTO;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTOWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.VerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.VerificationPKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.verification.dto.MatchDetail;
import io.mosip.registration.processor.verification.dto.UserDto;
import io.mosip.registration.processor.verification.dto.VerificationDecisionDto;
import io.mosip.registration.processor.verification.exception.InvalidRidException;
import io.mosip.registration.processor.verification.response.dto.VerificationResponseDTO;
import io.mosip.registration.processor.verification.service.impl.VerificationServiceImpl;
import io.mosip.registration.processor.verification.stage.VerificationStage;
import io.mosip.registration.processor.verification.util.SaveVerificationRecordUtility;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class VerificationServiceTest {

	private static final String STAGE_NAME = "VerificationStage";
	private List<VerificationEntity> entities;
	private List<VerificationEntity> entitiesTemp;
	@InjectMocks
	private VerificationService verificationService = new VerificationServiceImpl();
	
	@Autowired
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
    UserDto dto;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private VerificationStage manualAdjudicationStage;

	@Mock
	VerificationService mockManualAdjudicationService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private Utilities utility;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private BasePacketRepository<VerificationEntity, String> basePacketRepository;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private JsonUtil jsonUtil;

	@Mock
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	@Mock
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	@Mock
	SaveVerificationRecordUtility saveVerificationRecordUtility;


	private InternalRegistrationStatusDto registrationStatusDto;
	private VerificationPKEntity PKId;
	private ManualVerificationDTO manualVerificationDTO;
	private MatchDetail matchDetail=new MatchDetail();
	private VerificationEntity manualVerificationEntity;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger regprocLogger;
	ClassLoader classLoader;

	private String stageName = "ManualVerificationStage";

	private ResponseWrapper<UserResponseDTOWrapper> responseWrapper = new ResponseWrapper<>();
	private UserResponseDTOWrapper userResponseDTOWrapper = new UserResponseDTOWrapper();
	private List<UserResponseDTO> userResponseDto = new ArrayList<>();
	private UserResponseDTO userResponseDTO = new UserResponseDTO();
	private VerificationDecisionDto verificationDecisionDto =new  VerificationDecisionDto();
	private VerificationResponseDTO verificationResponseDTO=new  VerificationResponseDTO();
	private MosipQueue queue;
	LinkedHashMap dataShareResponse;

	@Mock
	LogDescription description;

	@Mock
	private Environment env;

	@Mock
	private CbeffUtil cbeffutil;

	MessageDTO object;

	@Mock
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	VerificationResponseDTO resp;

	@Before
	public void setup() throws Exception {

		resp = new VerificationResponseDTO();
		resp.setId("verification");
		resp.setRequestId("e2e59a9b-ce7c-41ae-a953-effb854d1205");
		resp.setResponsetime(DateUtils.getCurrentDateTimeString());
		resp.setReturnValue(1);

		object = new MessageDTO();
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");
		object.setIsValid(true);
		object.setInternalError(false);

		queue=new MosipQueue() {

			@Override
			public String getQueueName() {
				return null;
			}

			@Override
			public void createConnection(String username, String password, String brokerUrl,
					List<String> trustedPackage) {

			}
		};

		ReflectionTestUtils.setField(verificationService, "messageFormat", "text");
		regprocLogger = (Logger) LoggerFactory.getLogger(VerificationServiceImpl.class);
		listAppender = new ListAppender<>();
		classLoader = getClass().getClassLoader();

		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();

		PKId = new VerificationPKEntity();
		PKId.setWorkflowInstanceId("WorkflowInstanceId");
		dto.setUserId("mvusr22");

		entities = new ArrayList<VerificationEntity>();
		entitiesTemp = new ArrayList<VerificationEntity>();
		manualVerificationEntity = new VerificationEntity();
		manualVerificationEntity.setRegId("10002100741000320210107125533");
		manualVerificationEntity.setCrBy("regprc");
		Date date = new Date();
		manualVerificationEntity.setDelDtimes(new Timestamp(date.getTime()));
		manualVerificationEntity.setStatusComment("test");
		manualVerificationEntity.setStatusCode(ManualVerificationStatus.PENDING.name());
		manualVerificationEntity.setReasonCode("test");
		manualVerificationEntity.setId(PKId);
		entities.add(manualVerificationEntity);

		matchDetail.setMatchedRefType("Type");
		matchDetail.setMatchedRegId("RefID");
		matchDetail.setReasonCode(null);
		matchDetail.setUrl(null);
		manualVerificationDTO.setRegId("RegID");

		manualVerificationDTO.setMvUsrId("test");
		registrationStatusDto.setStatusCode(ManualVerificationStatus.PENDING.name());
		registrationStatusDto.setStatusComment("test");
		registrationStatusDto.setRegistrationType("LOST");
		registrationStatusDto.setRegistrationId("10002100741000320210107125533");
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

		List<MatchDetail> list=new ArrayList<>();
		list.add(matchDetail);
		manualVerificationDTO.setGallery(list);
		manualVerificationDTO.setStatusCode("PENDING");

		Mockito.when(basePacketRepository.getFirstApplicantDetails(ManualVerificationStatus.PENDING.name(), "DEMO"))
				.thenReturn(entities);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(any(), any())).thenReturn(entities);
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(registrationExceptionMapperUtil.getStatusCode(any())).thenReturn("ERROR");
		userResponseDTO.setStatusCode("ACT");
		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
		responseWrapper.setResponse(userResponseDTOWrapper);
		verificationDecisionDto.setMatchedRefType("Type");
		verificationDecisionDto.setMvUsrId("mvusr22");
		verificationDecisionDto.setReasonCode("test");
		verificationDecisionDto.setRegId("RegID");
		verificationDecisionDto.setStatusCode("APPROVED");
		verificationResponseDTO.setReturnValue(1);
		verificationResponseDTO.setResponsetime(DateUtils.getCurrentDateTimeString());
		verificationResponseDTO.setId("mosip.manual.adjudication.adjudicate");
		verificationResponseDTO.setRequestId("4d4f27d3-ec73-41c4-a384-bf87fce4969e");

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

		JSONObject docObject = new JSONObject();
		HashMap docmap = new HashMap<String, String>();
		docmap.put("documentType", "DOC005");
		docmap.put("documentCategory", "POI");
		docmap.put("documentName", "POI_DOC005");
		docObject.put("POI", docmap);

		JSONObject regProcessorIdentityJson = new JSONObject();
		LinkedHashMap bioIdentity = new LinkedHashMap<String, String>();
		bioIdentity.put("value", "biometrics");
		regProcessorIdentityJson.put("individualBiometrics", bioIdentity);

		Map<String, String> identity = new HashMap<String, String>();
		identity.put("fullName", "Satish");

		Map<String, String> metaInfo = new HashMap<String, String>();
		metaInfo.put("registrationId", "92379526572940");

		dataShareResponse = new LinkedHashMap<String, String>();
		LinkedHashMap datashareUrl = new LinkedHashMap<String, String>();
		datashareUrl.put("url", "Http://.....");
		dataShareResponse.put("dataShare", datashareUrl);

		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(env.getProperty(ApiName.DATASHARECREATEURL.name())).thenReturn("/v1/datashare/create");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(registrationStatusDto);

		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenReturn(identity);
		Mockito.when(packetManagerService.getBiometrics(anyString(), anyString(), any(), anyString(), any()))
				.thenReturn(biometricRecord);
		Mockito.when(cbeffutil.createXML(any())).thenReturn(new byte[120]);
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metaInfo);
		Mockito.when(utility.getRegistrationProcessorMappingJson(any())).thenReturn(docObject)
				.thenReturn(regProcessorIdentityJson);
		Mockito.when(mosipQueueManager.send(any(), anyString(), anyString(), anyInt())).thenReturn(true);

		ResponseWrapper policiesResponse = new ResponseWrapper();
		LinkedHashMap<String, Object> policiesMap = new LinkedHashMap<String, Object>();
		List<LinkedHashMap> attributeList = new ArrayList<LinkedHashMap>();
		LinkedHashMap<String, Object> shareableAttributes = new LinkedHashMap<String, Object>();
		LinkedHashMap attribute1 = new LinkedHashMap();
		attribute1.put("encrypted", "true");
		attribute1.put("attributeName", "fullName");
		LinkedHashMap source = new LinkedHashMap();
		source.put("attribute", "fullName");
		attribute1.put("source", Arrays.asList(source));
		LinkedHashMap attribute2 = new LinkedHashMap();
		attribute2.put("encrypted", "true");
		attribute2.put("attributeName", "meta_info");
		LinkedHashMap source2 = new LinkedHashMap();
		source2.put("attribute", "meta_info");
		attribute2.put("source", Arrays.asList(source2));
		LinkedHashMap attribute3 = new LinkedHashMap();
		attribute3.put("encrypted", "true");
		attribute3.put("attributeName", "biometrics");
		LinkedHashMap source31 = new LinkedHashMap();
		LinkedHashMap source32 = new LinkedHashMap();
		LinkedHashMap source33 = new LinkedHashMap();
		List<LinkedHashMap> filter1 = new ArrayList<LinkedHashMap>();
		List<LinkedHashMap> filter2 = new ArrayList<LinkedHashMap>();
		List<LinkedHashMap> filter3 = new ArrayList<LinkedHashMap>();
		LinkedHashMap type1 = new LinkedHashMap();
		type1.put("type", "Iris");
		filter1.add(type1);
		LinkedHashMap type2 = new LinkedHashMap();
		type2.put("type", "Finger");
		filter2.add(type2);
		LinkedHashMap type3 = new LinkedHashMap();
		type3.put("type", "Face");
		filter3.add(type3);
		source31.put("attribute", "biometrics");
		source31.put("filter", filter1);
		source32.put("attribute", "biometrics");
		source32.put("filter", filter2);
		source33.put("attribute", "biometrics");
		source33.put("filter", filter3);
		attribute3.put("source", Arrays.asList(source31, source32, source33));
		attributeList.add(attribute1);
		attributeList.add(attribute2);
		attributeList.add(attribute3);
		shareableAttributes.put("shareableAttributes", attributeList);
		policiesMap.put("policies", shareableAttributes);
		policiesResponse.setResponse(policiesMap);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(policiesResponse);

		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(resp.getRequestId())).thenReturn(entities);
		
	}

	@Test
	public void testVerificationSuccess() throws ApisResourceAccessException {

		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(), any(), any(), any(), any(),
				eq(LinkedHashMap.class))).thenReturn(dataShareResponse);

		MessageDTO response = verificationService.process(object, queue, stageName);

		assertTrue(response.getIsValid());
	}

	@Test
	public void testVerificationFailed() throws ApisResourceAccessException {

		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(), any(), any(), any(), any(),
				eq(LinkedHashMap.class))).thenThrow(new ApisResourceAccessException("exception"));

		MessageDTO response = verificationService.process(object, queue, stageName);

		assertFalse(response.getIsValid());
		assertTrue(response.getInternalError());
	}

	@Test
	public void testVerificationDatashareException() throws ApisResourceAccessException {
		LinkedHashMap dataShareResponse = new LinkedHashMap<String, String>();
		LinkedHashMap datashareUrl = new LinkedHashMap<String, String>();
		datashareUrl.put("errors", "Http://.....");
		dataShareResponse.put("errors", datashareUrl);

		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(), any(), any(), any(), any(),
				eq(LinkedHashMap.class))).thenReturn(dataShareResponse);

		MessageDTO response = verificationService.process(object, queue, stageName);

		assertFalse(response.getIsValid());
		assertTrue(response.getInternalError());
	}

	@Test
	public void testVerificationInvalidRId() {
		object.setRid("");

		MessageDTO response = verificationService.process(object, queue, stageName);

		assertFalse(response.getIsValid());
		assertTrue(response.getInternalError());
	}

	@Test(expected = InvalidRidException.class)
	public void testInvalidRidException() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {


		String response = objectMapper.writeValueAsString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		resp.setRequestId("2344");

		boolean result = verificationService.updatePacketStatus(resp, stageName, queue);
	}

	@Test
	public void testNoRecordAssignedException() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {


		String response = objectMapper.writeValueAsString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		boolean result = verificationService.updatePacketStatus(resp, stageName, queue);

		assertFalse(result);
	}
	
	@Test
	@Ignore
	public void testUpdateStatusSuccess() throws com.fasterxml.jackson.core.JsonProcessingException {

		Mockito.when(basePacketRepository.getAssignedVerificationRecord(anyString(), anyString())).thenReturn(entities);

		String response = objectMapper.writeValueAsString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		boolean result = verificationService.updatePacketStatus(resp, stageName, queue);

		assertTrue(result);
	}

	@Test
	public void testUpdateStatusResend() throws com.fasterxml.jackson.core.JsonProcessingException {

		Mockito.when(basePacketRepository.getAssignedVerificationRecord(anyString(), anyString())).thenReturn(entities);

		String response = objectMapper.writeValueAsString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		// for resend
		resp.setReturnValue(2);

		boolean result = verificationService.updatePacketStatus(resp, stageName, queue);

		assertFalse(result);
	}

	
	@Test
	@Ignore
	public void testUpdateStatusRejected() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {

		Mockito.when(basePacketRepository.getAssignedVerificationRecord(anyString(), anyString())).thenReturn(entities);

		String response = objectMapper.writeValueAsString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		// for rejected
		resp.setReturnValue(3);

		boolean result = verificationService.updatePacketStatus(resp, stageName, queue);

		assertTrue(result);
	}


	@Test
	public void PacketManagerNonRecoverableExceptionTest() throws ApisResourceAccessException, PacketManagerException, IOException, JsonProcessingException {
		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenThrow(new PacketManagerNonRecoverableException("exceptionCode","messahe"));
		MessageDTO response = verificationService.process(object, queue, stageName);
		assertFalse(response.getIsValid());
		assertTrue(response.getInternalError());
	}

}

