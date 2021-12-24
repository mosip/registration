package io.mosip.registration.processor.verification.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
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
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.VerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.VerificationPKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.verification.dto.MatchDetail;
import io.mosip.registration.processor.verification.dto.UserDto;
import io.mosip.registration.processor.verification.dto.VerificationDecisionDto;
import io.mosip.registration.processor.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.verification.exception.InvalidRidException;
import io.mosip.registration.processor.verification.response.dto.VerificationResponseDTO;
import io.mosip.registration.processor.verification.service.impl.VerificationServiceImpl;
import io.mosip.registration.processor.verification.stage.VerificationStage;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, JsonUtil.class, JsonUtils.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class VerificationServiceTest {

	private List<VerificationEntity> entities;
	@InjectMocks
	private VerificationService verificationService = new VerificationServiceImpl();
	@Mock
    UserDto dto;
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

	private InternalRegistrationStatusDto registrationStatusDto;
	private VerificationPKEntity PKId;
	private ManualVerificationDTO manualVerificationDTO;
	private MatchDetail matchDetail=new MatchDetail();
	private VerificationEntity verificationEntity;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger regprocLogger;
	ClassLoader classLoader;

	private String stageName = "VerificationStage";

	private ResponseWrapper<UserResponseDTOWrapper> responseWrapper = new ResponseWrapper<>();
	private UserResponseDTOWrapper userResponseDTOWrapper = new UserResponseDTOWrapper();
	private List<UserResponseDTO> userResponseDto = new ArrayList<>();
	private UserResponseDTO userResponseDTO = new UserResponseDTO();
	private VerificationDecisionDto verificationDecisionDto =new  VerificationDecisionDto();
	private VerificationResponseDTO verificationResponseDTO=new  VerificationResponseDTO();
	private MosipQueue queue;
	@Mock
	LogDescription description;

	@Mock
	ObjectMapper mapper;

	@Mock
	private Environment env;

	@Mock
	private CbeffUtil cbeffutil;

	@Mock
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;
	
	ResponseWrapper policiesResponse = new ResponseWrapper();
	BiometricRecord biometricRecord = new BiometricRecord();
	Map<String, String> identity = new HashMap<String, String>();
	Map<String, String> metaInfo = new HashMap<String, String>();
	LinkedHashMap dataShareResponse = new LinkedHashMap<String, String>();
	MessageDTO object = new MessageDTO();
	JSONObject regProcessorIdentityJson = new JSONObject();
	JSONObject docObject = new JSONObject();
	Document document = new Document();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Before
	public void setup() throws SecurityException, IllegalArgumentException {

		queue=new MosipQueue() {

			@Override
			public String getQueueName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void createConnection(String username, String password, String brokerUrl) {
				// TODO Auto-generated method stub

			}
		};

		ReflectionTestUtils.setField(verificationService, "messageFormat", "text");
		regprocLogger = (Logger) LoggerFactory.getLogger(VerificationServiceImpl.class);
		listAppender = new ListAppender<>();
		classLoader = getClass().getClassLoader();
		
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");
		object.setIsValid(true);
		object.setInternalError(false);

		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();

		PKId = new VerificationPKEntity();
		PKId.setWorkflowInstanceId("WorkflowInstanceId");
		dto.setUserId("mvusr22");

		entities = new ArrayList<VerificationEntity>();
		verificationEntity = new VerificationEntity();
		verificationEntity.setRegId("10002100741000320210107125533");
		verificationEntity.setCrBy("regprc");
		Date date = new Date();
		verificationEntity.setDelDtimes(new Timestamp(date.getTime()));
		verificationEntity.setStatusComment("test");
		verificationEntity.setStatusCode(ManualVerificationStatus.PENDING.name());
		verificationEntity.setReasonCode("test");
		verificationEntity.setId(PKId);
		verificationEntity.setMatchType("Type");
		entities.add(verificationEntity);

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

		List<MatchDetail> list=new ArrayList<>();
		list.add(matchDetail);
		manualVerificationDTO.setGallery(list);
		manualVerificationDTO.setStatusCode("PENDING");

		Mockito.when(basePacketRepository.getFirstApplicantDetails(ManualVerificationStatus.PENDING.name(), "DEMO"))
				.thenReturn(entities);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(any(), any())).thenReturn(entities);
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(registrationExceptionMapperUtil.getStatusCode(any())).thenReturn("ERROR");
		Mockito.when(env.getProperty(anyString())).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		.thenReturn("/v1/datashare/create");
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

		biometricRecord.setSegments(birTypeList);

		HashMap docmap = new HashMap<String, String>();
		docmap.put("documentType", "DOC005");
		docmap.put("documentCategory", "POI");
		docmap.put("documentName", "POI_DOC005");
		docObject.put("POI", docmap);

		LinkedHashMap bioIdentity = new LinkedHashMap<String, String>();
		bioIdentity.put("value", "biometrics");
		regProcessorIdentityJson.put("individualBiometrics", bioIdentity);

		identity.put("fullName", "Satish");

		metaInfo.put("registrationId", "92379526572940");

		LinkedHashMap datashareUrl = new LinkedHashMap<String, String>();
		datashareUrl.put("url", "Http://.....");
		dataShareResponse.put("dataShare", datashareUrl);
		
		document.setDocument("document".getBytes());

	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testVerificationSuccess() throws Exception {

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(policiesResponse);
		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenReturn(identity);
		Mockito.when(packetManagerService.getBiometrics(anyString(), anyString(), any(), anyString(), any()))
				.thenReturn(biometricRecord);
		Mockito.when(cbeffutil.createXML(any())).thenReturn(new byte[120]);
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metaInfo);
		Mockito.when(utility.getRegistrationProcessorMappingJson(any())).thenReturn(docObject)
				.thenReturn(regProcessorIdentityJson);
		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(), any(), any(), any(), any(),
				eq(LinkedHashMap.class))).thenReturn(dataShareResponse);
		Mockito.when(packetManagerService.getDocument(anyString(), anyString(), any(), any())).thenReturn(document);
		Mockito.when(mosipQueueManager.send(any(), anyString(), anyString(), anyInt())).thenReturn(true);

		MessageDTO response = verificationService.process(object, queue, stageName);
		assertTrue(response.getIsValid());
	}
	
	@SuppressWarnings({ "unchecked" })
	@Test
	public void testVerificationRecordUpdateSuccess() throws Exception {

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(policiesResponse);
		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenReturn(identity);
		Mockito.when(packetManagerService.getBiometrics(anyString(), anyString(), any(), anyString(), any()))
				.thenReturn(biometricRecord);
		Mockito.when(cbeffutil.createXML(any())).thenReturn(new byte[120]);
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metaInfo);
		Mockito.when(utility.getRegistrationProcessorMappingJson(any())).thenReturn(docObject)
				.thenReturn(regProcessorIdentityJson);
		Mockito.when(registrationProcessorRestClientService.postApi(anyString(), any(), any(), any(), any(), any(),
				eq(LinkedHashMap.class))).thenReturn(dataShareResponse);
		Mockito.when(packetManagerService.getDocument(anyString(), anyString(), any(), any())).thenReturn(document);
		Mockito.when(mosipQueueManager.send(any(), anyString(), anyString(), anyInt())).thenReturn(true);
		Mockito.when(basePacketRepository.getVerificationRecordByWorkflowInstanceId(any())).thenReturn(entities);

		MessageDTO response = verificationService.process(object, queue, stageName);
		assertTrue(response.getIsValid());
	}
	
	@Test
	public void testVerificationRIDEmpty() throws Exception {

		object.setRid("");
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		MessageDTO response = verificationService.process(object, queue, stageName);
		assertFalse(response.getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testVerificationPoliciesNull() throws Exception {
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(null);
		MessageDTO response = verificationService.process(object, queue, stageName);
		assertFalse(response.getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testVerificationPoliciesApisResourceAccessException() throws Exception {
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenThrow(new ApisResourceAccessException("exception occured"));
		MessageDTO response = verificationService.process(object, queue, stageName);
		assertFalse(response.getIsValid());
	}
	
	@Test
	public void testPacketStatusApproved() throws Exception {
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		boolean response =verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
		assertTrue(response);
	}
	
	@Test(expected = InvalidRidException.class)
	public void testPacketStatusApprovedVerificationEntitiesNull() throws Exception {
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(null);
		verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
	}
	
	@Test
	public void testPacketStatusApprovedTablenotAccessibleException() throws Exception {
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(registrationExceptionMapperUtil.getStatusCode(any())).thenReturn("REPROCESS");
		Mockito.when(basePacketRepository.update(any()))
				.thenThrow(new TablenotAccessibleException("exception occured"));
		boolean response = verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
		assertFalse(response);
	}
	
	@Test
	public void testPacketStatusApprovedJsonException() throws Exception {
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		PowerMockito.mockStatic(JsonUtils.class);

		PowerMockito.when(JsonUtils.javaObjectToJsonString(any())).thenThrow(new JsonProcessingException("exception occured"));
		boolean response = verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
		assertFalse(response);
	}
	
	@Test(expected = InvalidFileNameException.class)
	public void testPacketStatusApprovedInvalidFileNameException() throws Exception {
		entities = new ArrayList<VerificationEntity>();
		VerificationEntity entity = new VerificationEntity();
		entity.setRegId(null);
		entities.add(entity);
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
	}
	
	@Test
	public void testPacketStatusRejected() throws Exception {
		verificationResponseDTO.setReturnValue(0);
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		boolean response =verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
		assertTrue(response);
	}
	
	@Test
	public void testPacketStatusResend() throws Exception {
		verificationResponseDTO.setReturnValue(2);
		Mockito.when(basePacketRepository.getVerificationRecordByRequestId(anyString())).thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
		.thenReturn(registrationStatusDto);
		boolean response =verificationService.updatePacketStatus(verificationResponseDTO, stageName, queue);
		assertFalse(response);
	}
	
}
