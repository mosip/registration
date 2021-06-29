package io.mosip.registration.processor.manual.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTO;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTOWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.manual.verification.dto.MatchDetail;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.manual.verification.response.dto.Candidate;
import io.mosip.registration.processor.manual.verification.response.dto.CandidateList;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.manual.verification.service.impl.ManualVerificationServiceImpl;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationPKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, JsonUtil.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class ManualVerificationServiceTest {

	private List<ManualVerificationEntity> entities;
	private List<ManualVerificationEntity> entitiesTemp;
	@InjectMocks
	private ManualVerificationService manualAdjudicationService = new ManualVerificationServiceImpl();
	@Mock
	UserDto dto;
	@Mock
	ManualVerificationStage manualVerificationStage;
	@Mock
	ManualVerificationService mockManualAdjudicationService;
	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private Utilities utilities;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private BasePacketRepository<ManualVerificationEntity, String> basePacketRepository;
	@Mock
	private JsonUtil jsonUtil;
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

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
			public void createConnection(String username, String password, String brokerUrl) {
				// TODO Auto-generated method stub
				
			}
		};

		regprocLogger = (Logger) LoggerFactory.getLogger(ManualVerificationServiceImpl.class);
		listAppender = new ListAppender<>();
		classLoader = getClass().getClassLoader();

		ReflectionTestUtils.setField(manualAdjudicationService, "restClientService", restClientService);

		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();

		PKId = new ManualVerificationPKEntity();
		PKId.setMatchedRefId("10002100880000920210628085700");
		PKId.setMatchedRefType("Type");
		PKId.setRegId("RegID");
		dto.setUserId("mvusr22");

		entities = new ArrayList<ManualVerificationEntity>();
		entitiesTemp = new ArrayList<ManualVerificationEntity>();
		manualVerificationEntity = new ManualVerificationEntity();
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
		Mockito.when(basePacketRepository.getRegistrationIdbyRequestId(anyString())).thenReturn(Lists.newArrayList(registrationStatusDto.getRegistrationId()));
		
	}

//	@Test
//	public void assignStatusMethodCheck() throws JsonParseException, JsonMappingException, java.io.IOException {
//		Mockito.when(basePacketRepository.getAssignedApplicantDetails(any(), any())).thenReturn(entities);
//		dto.setMatchType("DEMO");
//		dto.setUserId("110003");
//
//		userResponseDTO.setStatusCode("ACT");
//		userResponseDto.add(userResponseDTO);
//		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
//		Mockito.when(mapper.readValue(anyString(),any(Class.class))).thenReturn(userResponseDTOWrapper);
//		responseWrapper.setResponse(userResponseDTOWrapper);
//		try {
//			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), any(), any(), any());
//		} catch (ApisResourceAccessException e) {
//			e.printStackTrace();
//		}
//		ManualVerificationDTO manualVerificationDTO1 = manualAdjudicationService.assignApplicant(dto);
//		assertEquals(manualVerificationDTO, manualVerificationDTO1);
//
//	}

	public void assignStatusMethodNullIdCheck() throws JsonParseException, JsonMappingException, java.io.IOException {
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString()))
				.thenReturn(entitiesTemp);
		Mockito.when(basePacketRepository.update(manualVerificationEntity)).thenReturn(manualVerificationEntity);
		dto.setMatchType("DEMO");
		dto.setUserId(null);

		userResponseDTO.setStatusCode("ACT");
		userResponseDto.add(userResponseDTO);
		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
		Mockito.when(mapper.readValue(anyString(),any(Class.class))).thenReturn(userResponseDTOWrapper);
		responseWrapper.setResponse(userResponseDTOWrapper);
		try {
			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), anyString(), any(), any());
		} catch (ApisResourceAccessException e) {
			e.printStackTrace();
		}

		manualAdjudicationService.assignApplicant(dto);
	}
	
	
	
//	@Test
//	public void assignStatusMethodNullEntityCheck() throws JsonParseException, JsonMappingException, java.io.IOException {
//		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString()))
//				.thenReturn(entitiesTemp);
//		Mockito.when(basePacketRepository.update(manualVerificationEntity)).thenReturn(manualVerificationEntity);
//		dto.setMatchType("DEMO");
//		dto.setUserId("110003");
//
//		userResponseDTO.setStatusCode("ACT");
//		userResponseDto.add(userResponseDTO);
//		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
//		responseWrapper.setResponse(userResponseDTOWrapper);
//		Mockito.when(mapper.readValue(any(String.class),any(Class.class))).thenReturn(userResponseDTOWrapper);
//		try {
//			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), any(), any(), any());
//		} catch (ApisResourceAccessException e) {
//			e.printStackTrace();
//		}
//
//		manualAdjudicationService.assignApplicant(dto);
//	}

//	@Test(expected = NoRecordAssignedException.class)
//	public void noRecordAssignedExceptionAssignStatus() throws Exception {
//		Mockito.when(basePacketRepository.getAssignedApplicantDetails(any(), any()))
//				.thenReturn(entitiesTemp);
//		Mockito.when(basePacketRepository.getFirstApplicantDetails(ManualVerificationStatus.PENDING.name(), "DEMO"))
//				.thenReturn(entitiesTemp);
//		dto.setMatchType("DEMO");
//		dto.setUserId("110003");
//
//		userResponseDTO.setStatusCode("ACT");
//		userResponseDto.add(userResponseDTO);
//		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
//		Mockito.when(mapper.readValue(any(String.class),any(Class.class))).thenReturn(userResponseDTOWrapper);
//
//		JSONObject jsonObject = Mockito.mock(JSONObject.class);
//		PowerMockito.mockStatic(JsonUtil.class);
//		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any()).thenReturn(jsonObject);
//		PowerMockito.when(JsonUtil.class, "objectMapperReadValue", any(), any()).thenReturn(jsonObject);
//
//		responseWrapper.setResponse(userResponseDTOWrapper);
//		try {
//			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), any(), any(), any());
//		} catch (ApisResourceAccessException e) {
//			e.printStackTrace();
//		}
//		manualAdjudicationService.assignApplicant(dto);
//	}

//	@Test(expected = MatchTypeNotFoundException.class)
//	public void noMatchTypeNotFoundException() throws JsonParseException, JsonMappingException, java.io.IOException {
//		dto.setMatchType("test");
//		dto.setUserId("110003");
//		userResponseDTO.setStatusCode("ACT");
//		userResponseDto.add(userResponseDTO);
//		userResponseDTOWrapper.setUserResponseDto(userResponseDto);
//		Mockito.when(mapper.readValue(anyString(),any(Class.class))).thenReturn(userResponseDTOWrapper);
//		responseWrapper.setResponse(userResponseDTOWrapper);
//		try {
//			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), any(), any(), any());
//		} catch (ApisResourceAccessException e) {
//			e.printStackTrace();
//		}
//		manualAdjudicationService.assignApplicant(dto);
//	}

	public void noUserIDNotPresentException() {
		dto.setUserId("dummyID");
		dto.setMatchType("DEMO");

		responseWrapper.setResponse(null);
		try {
			Mockito.doReturn(responseWrapper).when(restClientService).getApi(any(), any(), anyString(), any(), any());
		} catch (ApisResourceAccessException e) {
			e.printStackTrace();
		}
		manualAdjudicationService.assignApplicant(dto);
	}
	
	public void ApisResourceAccessExceptionTest() throws ApisResourceAccessException {
		dto.setUserId("dummyID");
		dto.setMatchType("DEMO");
		Mockito.doThrow(ApisResourceAccessException.class).when(restClientService).getApi(any(), any(), anyString(), any(), any());

		ManualVerificationDTO resp = manualAdjudicationService.assignApplicant(dto);
		assertNull(resp.getRegId());
	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenThrow(new TablenotAccessibleException(""));
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test
	@Ignore
	public void getApplicantFileMethodCheck() throws Exception {
		String regId = "Id";
		String source = "id";
		JSONObject jsonObject = Mockito.mock(JSONObject.class);

		byte[] file = "Str".getBytes();
		InputStream fileInStream = new ByteArrayInputStream(file);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any()).thenReturn(jsonObject);
		PowerMockito.when(JsonUtil.class, "objectMapperReadValue", any(), any()).thenReturn(jsonObject);

		//Mockito.when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn(source);
		Mockito.when(utilities.getRegistrationProcessorMappingJson(anyString())).thenReturn(jsonObject);
		//Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(fileInStream);

		String fileName = PacketFiles.BIOMETRIC.name();
		byte[] biometricFile = manualAdjudicationService.getApplicantFile(regId, fileName, source);
		fileName = PacketFiles.DEMOGRAPHIC.name();
		byte[] demographicFile = manualAdjudicationService.getApplicantFile(regId, fileName, source);
		fileName = PacketFiles.PACKET_META_INFO.name();
		byte[] metainfoFile = manualAdjudicationService.getApplicantFile(regId, fileName, source);

		assertNotNull(biometricFile);
		assertNotNull(demographicFile);
		assertNotNull(metainfoFile);

	}

	@Test(expected = InvalidFileNameException.class)
	public void testExceptionIngetApplicantFile() throws Exception {
		String regId = "Id";
		String fileName = "";
		String source = "id";
		manualAdjudicationService.getApplicantFile(regId, fileName, source);
	}

	@Test(expected = InvalidFileNameException.class)
	public void testExceptionIngetApplicantData() throws Exception {
		String regId = "Id";
		String fileName = "";
		String source = "id";
		manualAdjudicationService.getApplicantFile(regId, fileName, source);
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
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	
	@Test
	public void updatePacketStatusApprovalMethodCheck() {
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenReturn(manualVerificationEntity);
		manualVerificationDTO.setStatusCode(ManualVerificationStatus.APPROVED.name());

		Mockito.doNothing().when(manualVerificationStage).sendMessage(any(MessageDTO.class));
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
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any())).thenReturn(manualVerificationEntity);

		Mockito.doNothing().when(manualVerificationStage).sendMessage(any());
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	@Test
	public void testManualVerificationResponse_CountMismatch() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("countMismatch.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
														responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false for response count mismatch", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.ERROR,
				"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - Validation error - Candidate count does not match reference ids count."));
	}

	@Test
	public void testManualVerificationResponse_RefIdMismatch() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("refIdMismatch.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false for response count mismatch", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.ERROR,
						"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - Validation error - " +
								"Received ReferenceIds does not match reference ids in manual verification table."));
	}


	@Test
	public void testManualVerification_ResendFlow() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("resendFlow.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertFalse("Should be false", isValidResponse);
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage).contains(
				Tuple.tuple(Level.INFO,
						"SESSIONID - REGISTRATIONID - 10002100741000320210107125533 - " +
						"Received resend request from manual verification application. This will be marked for reprocessing."));
	}

	@Test
	public void testManualVerification_SuccessFlow() throws IOException {

		listAppender.start();
		regprocLogger.addAppender(listAppender);

		File childFile = new File(classLoader.getResource("successFlow.json").getFile());
		InputStream is = new FileInputStream(childFile);
		String responseString = IOUtils.toString(is, "UTF-8");
		ManualAdjudicationResponseDTO responseDTO = JsonUtil.readValueWithUnknownProperties(
				responseString, ManualAdjudicationResponseDTO.class);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		boolean isValidResponse = manualAdjudicationService.updatePacketStatus(responseDTO, stageName,queue);

		assertTrue("Should be Success", isValidResponse);
	}

	
}
