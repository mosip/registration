package io.mosip.registration.processor.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.adjudication.request.dto.Filter;
import io.mosip.registration.processor.adjudication.request.dto.ShareableAttributes;
import io.mosip.registration.processor.adjudication.request.dto.Source;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.commons.io.IOUtils;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
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
import io.mosip.registration.processor.adjudication.dto.ManualVerificationDTO;
import io.mosip.registration.processor.adjudication.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.adjudication.dto.ManualVerificationStatus;
import io.mosip.registration.processor.adjudication.dto.MatchDetail;
import io.mosip.registration.processor.adjudication.dto.UserDto;
import io.mosip.registration.processor.adjudication.exception.InvalidFileNameException;
import io.mosip.registration.processor.adjudication.exception.InvalidRidException;
import io.mosip.registration.processor.adjudication.response.dto.Candidate;
import io.mosip.registration.processor.adjudication.response.dto.CandidateList;
import io.mosip.registration.processor.adjudication.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.adjudication.service.ManualAdjudicationService;
import io.mosip.registration.processor.adjudication.service.impl.ManualAdjudicationServiceImpl;
import io.mosip.registration.processor.adjudication.stage.ManualAdjudicationStage;
import io.mosip.registration.processor.adjudication.util.ManualVerificationUpdateUtility;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
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
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationPKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, JsonUtil.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class ManualAdjudicationServiceTest {

	private List<ManualVerificationEntity> entities;
	private List<ManualVerificationEntity> entitiesTemp;

	@InjectMocks
	private ManualAdjudicationService manualAdjudicationService = new ManualAdjudicationServiceImpl();

	@Mock
    UserDto dto;

	@Mock
	private ManualVerificationUpdateUtility manualVerificationUpdateUtility;

	@Mock
    ManualAdjudicationStage manualAdjudicationStage;
	
	@Mock 
	private IdRepoService idRepoService;

	@Mock
	ManualAdjudicationService mockManualAdjudicationService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;


	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private Utilities utility;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private BasePacketRepository<ManualVerificationEntity, String> basePacketRepository;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private JsonUtil jsonUtil;

	@Mock
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	@Mock
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	private InternalRegistrationStatusDto registrationStatusDto;
	private ManualVerificationPKEntity PKId;
	private ManualVerificationDTO manualVerificationDTO;
	private MatchDetail matchDetail=new MatchDetail();
	private ManualVerificationEntity manualVerificationEntity;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger regprocLogger;
	ClassLoader classLoader;

	private String stageName = "ManualVerificationStage";

	private ResponseWrapper<UserResponseDTOWrapper> responseWrapper = new ResponseWrapper<>();
	private UserResponseDTOWrapper userResponseDTOWrapper = new UserResponseDTOWrapper();
	private List<UserResponseDTO> userResponseDto = new ArrayList<>();
	private UserResponseDTO userResponseDTO = new UserResponseDTO();
	private ManualVerificationDecisionDto manualVerificationDecisionDto=new  ManualVerificationDecisionDto();
	private ManualAdjudicationResponseDTO manualAdjudicationResponseDTO=new  ManualAdjudicationResponseDTO();
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


	@Before
	public void setup() throws SecurityException, IllegalArgumentException {

		queue=new MosipQueue() {
			
			@Override
			public String getQueueName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void createConnection(String username, String password, String brokerUrl,
					List<String> trustedPackage) {
				// TODO Auto-generated method stub
				
			}
		};

		ReflectionTestUtils.setField(manualAdjudicationService, "messageFormat", "text");
		regprocLogger = (Logger) LoggerFactory.getLogger(ManualAdjudicationServiceImpl.class);
		listAppender = new ListAppender<>();
		classLoader = getClass().getClassLoader();

		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();

		PKId = new ManualVerificationPKEntity();
		PKId.setMatchedRefId("10002100880000920210628085700");
		PKId.setMatchedRefType("Type");
		PKId.setWorkflowInstanceId("WorkflowInstanceId");
		dto.setUserId("mvusr22");

		entities = new ArrayList<ManualVerificationEntity>();
		entitiesTemp = new ArrayList<ManualVerificationEntity>();
		manualVerificationEntity = new ManualVerificationEntity();
		manualVerificationEntity.setRegId("10002100741000320210107125533");
		manualVerificationEntity.setCrBy("regprc");
		manualVerificationEntity.setMvUsrId("test");
		manualVerificationEntity.setIsActive(true);
		Date date = new Date();
		manualVerificationEntity.setDelDtimes(new Timestamp(date.getTime()));
		manualVerificationEntity.setIsDeleted(true);
		manualVerificationEntity.setStatusComment("test");
		manualVerificationEntity.setStatusCode(ManualVerificationStatus.PENDING.name());
		manualVerificationEntity.setReasonCode("test");
		manualVerificationEntity.setIsActive(true);
		manualVerificationEntity.setId(PKId);
		manualVerificationEntity.setLangCode("eng");
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
		manualVerificationDecisionDto.setMatchedRefType("Type");
		manualVerificationDecisionDto.setMvUsrId("mvusr22");
		manualVerificationDecisionDto.setReasonCode("test");
		manualVerificationDecisionDto.setRegId("RegID");
		manualVerificationDecisionDto.setStatusCode("APPROVED");
		manualAdjudicationResponseDTO.setReturnValue(1);
		manualAdjudicationResponseDTO.setResponsetime(DateUtils.getCurrentDateTimeString());
		manualAdjudicationResponseDTO.setId("mosip.manual.adjudication.adjudicate");
		manualAdjudicationResponseDTO.setRequestId("4d4f27d3-ec73-41c4-a384-bf87fce4969e");
		CandidateList candidateList=new CandidateList();
		candidateList.setCount(0);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);

		Mockito.doNothing().when(manualVerificationUpdateUtility).updateManualVerificationEntityRID(any(), any());

		Mockito.doNothing().when(manualVerificationUpdateUtility).updateManualVerificationEntityRID(any(), any());
		
	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenThrow(new TablenotAccessibleException(""));
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test
	public void updatePacketStatusNoRecordAssignedExceptionCheck() {
		Candidate candidate=new Candidate();
		List<Candidate> candidates=new ArrayList<>();
		
		candidate.setReferenceId("1234567890987654321");
		Map<String,String> analytics=new HashMap<>();
		candidates.add(candidate);
		CandidateList candidateList=new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String,Object> analytics1=new HashMap<>();
		analytics.put("primaryOperatorID", "110006");//logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		Mockito.when(basePacketRepository.getAllAssignedRecord( anyString(), anyString()))
				.thenReturn(entitiesTemp);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test(expected = InvalidRidException.class)
	public void updatePacketStatusInvalidRIDExceptionCheck() {
		Candidate candidate = new Candidate();
		List<Candidate> candidates = new ArrayList<>();

		candidate.setReferenceId("1234567890987654321");
		Map<String, String> analytics = new HashMap<>();
		candidates.add(candidate);
		CandidateList candidateList = new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String, Object> analytics1 = new HashMap<>();
		analytics.put("primaryOperatorID", "110006");// logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(), anyString())).thenReturn(entitiesTemp);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList());
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName, queue);

	}

	@Test(expected = InvalidFileNameException.class)
	public void updatePacketStatusEmptyRIDExceptionCheck() {
		Candidate candidate = new Candidate();
		List<Candidate> candidates = new ArrayList<>();

		candidate.setReferenceId("1234567890987654321");
		Map<String, String> analytics = new HashMap<>();
		candidates.add(candidate);
		CandidateList candidateList = new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String, Object> analytics1 = new HashMap<>();
		analytics.put("primaryOperatorID", "110006");// logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(), anyString())).thenReturn(entitiesTemp);
		manualVerificationEntity.setRegId("");
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName, queue);

	}


	@Test
	public void updatePacketStatusApprovalMethodCheck() {
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenReturn(manualVerificationEntity);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		manualVerificationDTO.setStatusCode(ManualVerificationStatus.APPROVED.name());

		Mockito.doNothing().when(manualAdjudicationStage).sendMessage(any(MessageDTO.class));
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test
	public void updatePacketStatusRejectionMethodCheck() {
		Candidate candidate=new Candidate();
		List<Candidate> candidates=new ArrayList<>();
		
		candidate.setReferenceId("1234567890987654321");
		JSONObject analytics=new JSONObject();
		candidate.setAnalytics(analytics);
		candidates.add(candidate);
		CandidateList candidateList=new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String,Object> analytics1=new HashMap<>();
		analytics.put("primaryOperatorID", "110006");//logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		manualVerificationDecisionDto.setStatusCode(ManualVerificationStatus.REJECTED.name());
		;
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString()))
				.thenReturn(entities);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any())).thenReturn(manualVerificationEntity);

		Mockito.doNothing().when(manualAdjudicationStage).sendMessage(any());
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test
	@Ignore
	public void testManualVerificationResponse_CountMismatch() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("countMismatch.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
														responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false for response count mismatch", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.ERROR,
				"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - Validation error - Candidate count does not match reference ids count."));
	}

	@Test
	@Ignore
	public void testManualVerificationResponse_RefIdMismatch() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("refIdMismatch.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false for response count mismatch", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.ERROR,
						"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - Validation error - " +
								"Received ReferenceIds does not match reference ids in manual verification table."));
	}


	@Test
	@Ignore
	public void testManualVerification_ResendFlow() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("resendFlow.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.INFO,
						"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - " +
						"Received resend request from manual verification application. This will be marked for reprocessing."));
	}

	@Test
	@Ignore
	public void testManualVerification_SuccessFlow() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("successFlow.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(manualVerificationEntity));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertTrue("Should be Success", isValidResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testManualAdjudication_Process() throws Exception {

		MessageDTO object = new MessageDTO();
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");

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

		LinkedHashMap dataShareResponse = new LinkedHashMap<String, String>();
		LinkedHashMap datashareUrl = new LinkedHashMap<String, String>();
		datashareUrl.put("url", "Http://.....");
		dataShareResponse.put("dataShare", datashareUrl);

		Mockito.when(basePacketRepository.getMatchedIds(any(), anyString())).thenReturn(entities);
		Mockito.when(env.getProperty(anyString())).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				.thenReturn("/v1/datashare/create");
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
		Mockito.when(mosipQueueManager.send(any(), anyString(), anyString(), anyInt())).thenReturn(true);
		manualAdjudicationService.process(object, queue);
	}

	public void setDataShareDetails() throws Exception {
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
		docmap.put("value", "biometrics");
		docObject.put("POI", docmap);

		JSONObject regProcessorIdentityJson = new JSONObject();
		LinkedHashMap bioIdentity = new LinkedHashMap<String, String>();
		bioIdentity.put("value", "biometrics");
		regProcessorIdentityJson.put("individualBiometrics", bioIdentity);

		Map<String, String> identity = new HashMap<String, String>();
		identity.put("fullName", "Satish");

		Map<String, String> metaInfo = new HashMap<String, String>();
		metaInfo.put("registrationId", "92379526572940");

		LinkedHashMap dataShareResponse = new LinkedHashMap<String, String>();
		LinkedHashMap datashareUrl = new LinkedHashMap<String, String>();
		datashareUrl.put("url", "Http://.....");
		dataShareResponse.put("dataShare", datashareUrl);

		Mockito.when(basePacketRepository.getMatchedIds(any(), anyString())).thenReturn(entities);
		Mockito.when(env.getProperty(anyString())).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				.thenReturn("/v1/datashare/create");
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
		Mockito.when(mosipQueueManager.send(any(), anyString(), anyString(), anyInt())).thenReturn(true);

		Document document = new Document();
		document.setDocument("abcd".getBytes(StandardCharsets.UTF_8));
		document.setValue("biometrics");
		document.setType("biometrics");
		Mockito.when(packetManagerService.getDocument(any(), any(), any(), any())).thenReturn(new Document());
	}
	@Test
	public void testManualAdjudicationProcess() throws Exception {

		MessageDTO object = new MessageDTO();
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");

		setDataShareDetails();

		manualAdjudicationService.process(object, queue);
	}
	@Test
	public void testManualAdjudicationProcessLatest() throws Exception {
		ReflectionTestUtils.setField(manualAdjudicationService, "uselatestManualAdjudicationRequestFormat", true);
		MessageDTO object = new MessageDTO();
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");
		setDataShareDetails();

		manualAdjudicationService.process(object, queue);
	}

	@Test
	public void PacketManagerNonRecoverableExceptionTest() throws Exception {
		ShareableAttributes shareableAttributes1 = new ShareableAttributes();
		shareableAttributes1.setAttributeName("fullName");
		shareableAttributes1.setEncrypted(true);

		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");
		List<Filter> filterlist = new ArrayList<>();
		Filter filter11 = new Filter();
		filter11.setLanguage("lang");
		filter11.setType("type");
		filterlist.add(filter11);
		source1.setFilter(filterlist);
		sourceList.add(source1);

		shareableAttributes1.setSource(sourceList);
		shareableAttributes1.setFormat("");
		shareableAttributes1.setGroup("");

		MessageDTO object = new MessageDTO();
		object.setReg_type("NEW");
		object.setRid("92379526572940");
		object.setIteration(1);
		object.setWorkflowInstanceId("26fa3eff-f3b9-48f7-b365-d7f7c2e56e00");
		setDataShareDetails();
		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenThrow(new PacketManagerNonRecoverableException("errorCode","message"));
		Mockito.when(mapper.writeValueAsString(any())).thenReturn("");
		Mockito.when(mapper.readValue(anyString(),any(Class.class))).thenReturn(shareableAttributes1).thenReturn(shareableAttributes1).thenReturn(shareableAttributes1);
		MessageDTO messageDTO=manualAdjudicationService.process(object,queue);
		assertFalse(messageDTO.getIsValid());
		assertTrue(messageDTO.getInternalError());
	}
}
