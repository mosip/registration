package io.mosip.registration.processor.manual.verification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

import org.assertj.core.util.Arrays;
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
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
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
import io.mosip.registration.processor.manual.verification.exception.InvalidUpdateException;
import io.mosip.registration.processor.manual.verification.exception.MatchTypeNotFoundException;
import io.mosip.registration.processor.manual.verification.exception.NoRecordAssignedException;
import io.mosip.registration.processor.manual.verification.exception.UserIDNotPresentException;
import io.mosip.registration.processor.manual.verification.response.dto.AnalyticsDTO;
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
		ReflectionTestUtils.setField(manualAdjudicationService, "restClientService", restClientService);
		manualVerificationEntity = new ManualVerificationEntity();
		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();
		entities = new ArrayList<ManualVerificationEntity>();
		entitiesTemp = new ArrayList<ManualVerificationEntity>();
		PKId = new ManualVerificationPKEntity();
		PKId.setMatchedRefId("RefID");
		PKId.setMatchedRefType("Type");
		PKId.setWorkflowInstanceId("WorkflowInstanceId");
		PKId.setRegId("RegID");
		dto.setUserId("mvusr22");
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
		entities.add(manualVerificationEntity);
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

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any())).thenReturn(entities);

		Mockito.when(registrationStatusService.getRegStatusForMainProcess(anyString())).thenReturn(registrationStatusDto);
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
		AnalyticsDTO analyticsDTO=new AnalyticsDTO();
		analyticsDTO.setPrimaryOperatorID("110006");
		analyticsDTO.setPrimaryOperatorComments("abcd");
		analyticsDTO.setSecondaryOperatorComments("asbd");
		analyticsDTO.setSecondaryOperatorID("110005");
		analyticsDTO.setAnalytics(analytics);
		candidate.setAnalytics(analyticsDTO);
		candidates.add(candidate);
		CandidateList candidateList=new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String,String> analytics1=new HashMap<>();
		analytics.put("primaryOperatorID", "110006");//logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		Mockito.when(basePacketRepository.getAllAssignedRecord( anyString(), anyString()))
				.thenReturn(entitiesTemp);
		Mockito.when(registrationStatusService.getRegStatusForMainProcess(anyString())).thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	
	@Test
	public void updatePacketStatusApprovalMethodCheck() {
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegStatusForMainProcess(anyString())).thenReturn(registrationStatusDto);
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
		Map<String,String> analytics=new HashMap<>();
		AnalyticsDTO analyticsDTO=new AnalyticsDTO();
		analyticsDTO.setPrimaryOperatorID("110006");
		analyticsDTO.setPrimaryOperatorComments("abcd");
		analyticsDTO.setSecondaryOperatorComments("asbd");
		analyticsDTO.setSecondaryOperatorID("110005");
		analyticsDTO.setAnalytics(analytics);
		candidate.setAnalytics(analyticsDTO);
		candidates.add(candidate);
		CandidateList candidateList=new CandidateList();
		candidateList.setCandidates(candidates);
		candidateList.setCount(1);// logic needs to be implemented.
		Map<String,String> analytics1=new HashMap<>();
		analytics.put("primaryOperatorID", "110006");//logic needs to be implemented
		analytics.put("primaryOperatorComments", "abcd");
		candidateList.setAnalytics(analytics1);
		manualAdjudicationResponseDTO.setCandidateList(candidateList);
		manualVerificationDecisionDto.setStatusCode(ManualVerificationStatus.REJECTED.name());
		;
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegStatusForMainProcess(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any())).thenReturn(manualVerificationEntity);

		Mockito.doNothing().when(manualVerificationStage).sendMessage(any());
		manualAdjudicationService.updatePacketStatus(manualAdjudicationResponseDTO, stageName,queue);

	}

	
}
