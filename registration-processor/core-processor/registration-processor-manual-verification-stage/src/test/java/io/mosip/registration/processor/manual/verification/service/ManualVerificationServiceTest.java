package io.mosip.registration.processor.manual.verification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.mosip.registration.processor.packet.storage.utils.Utilities;

import org.assertj.core.util.Arrays;
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
	@Mock
	LogDescription description;

	@Mock
	ObjectMapper mapper;

	@Mock
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;


	@Before
	public void setup()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		manualVerificationEntity = new ManualVerificationEntity();
		manualVerificationDTO = new ManualVerificationDTO();
		registrationStatusDto = new InternalRegistrationStatusDto();
		dto = new UserDto();
		entities = new ArrayList<ManualVerificationEntity>();
		entitiesTemp = new ArrayList<ManualVerificationEntity>();
		PKId = new ManualVerificationPKEntity();
		PKId.setMatchedRefId("RefID");
		PKId.setMatchedRefType("Type");
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

	@Test(expected=UserIDNotPresentException.class)
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

	@Test(expected = UserIDNotPresentException.class)
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
	
	@Test(expected = UserIDNotPresentException.class)
	public void ApisResourceAccessExceptionTest() throws ApisResourceAccessException {
		dto.setUserId("dummyID");
		dto.setMatchType("DEMO");
		Mockito.doThrow(ApisResourceAccessException.class).when(restClientService).getApi(any(), any(), anyString(), any(), any());
		
		manualAdjudicationService.assignApplicant(dto);
	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		manualVerificationDecisionDto.setStatusCode("REJECTED");
		Mockito.when(basePacketRepository.getAllAssignedRecord(any(), any(), any())).thenReturn(entities);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenThrow(new TablenotAccessibleException(""));
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);

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
		Mockito.when(utilities.getRegistrationProcessorMappingJson()).thenReturn(jsonObject);
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

	@Test(expected = InvalidUpdateException.class)
	public void updatePacketStatusExceptionCheck() {
		manualVerificationDecisionDto.setStatusCode("");
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);

	}

	@Test(expected = NoRecordAssignedException.class)
	public void updatePacketStatusNoRecordAssignedExceptionCheck() {
		manualVerificationDecisionDto.setStatusCode("REJECTED");
		Mockito.when(basePacketRepository.getAllAssignedRecord( anyString(), anyString(), anyString()))
				.thenReturn(entitiesTemp);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);

	}

	
	@Test
	public void updatePacketStatusApprovalMethodCheck() {
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString(), anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any(ManualVerificationEntity.class)))
				.thenReturn(manualVerificationEntity);
		manualVerificationDTO.setStatusCode(ManualVerificationStatus.APPROVED.name());

		Mockito.doNothing().when(manualVerificationStage).sendMessage(any(MessageDTO.class));
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);
	}

	@Test
	public void updatePacketStatusRejectionMethodCheck() {
		manualVerificationDecisionDto.setStatusCode(ManualVerificationStatus.REJECTED.name());
		;
		Mockito.when(basePacketRepository.getAllAssignedRecord(anyString(),  anyString(), anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		Mockito.when(basePacketRepository.update(any())).thenReturn(manualVerificationEntity);

		Mockito.doNothing().when(manualVerificationStage).sendMessage(any());
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);
	}

	@Test(expected = InvalidUpdateException.class)
	public void invalidStatusUpdateCheck() {
		manualVerificationDecisionDto.setStatusCode("ASSIGNED");
		manualVerificationDecisionDto.setMvUsrId("abcde");
		manualVerificationDecisionDto.setRegId("abcde");
		Mockito.when(basePacketRepository.getAllAssignedRecord( anyString(), anyString(), anyString()))
				.thenReturn(entities);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(basePacketRepository.getAssignedApplicantDetails(anyString(), anyString())).thenReturn(null);
		manualAdjudicationService.updatePacketStatus(manualVerificationDecisionDto, stageName);
	}

}
