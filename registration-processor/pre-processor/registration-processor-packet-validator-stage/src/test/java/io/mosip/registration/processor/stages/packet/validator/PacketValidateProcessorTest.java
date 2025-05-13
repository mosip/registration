package io.mosip.registration.processor.stages.packet.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.*;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ExceptionJSONInfoDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.NotificationUtility;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyBoolean;
import static org.junit.Assert.*;


/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtils.class, JsonUtil.class, IOUtils.class, HMACUtils2.class, Utilities.class,
		MessageDigest.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })
@TestPropertySource(locations = "classpath:application.properties")
public class PacketValidateProcessorTest {

	@InjectMocks
	private PacketValidateProcessor packetValidateProcessor;

	@Mock
	private PacketValidatorImpl packetValidator;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private ObjectMapper objectMapper;

	
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private AuditUtility auditUtility;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
	
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private Utilities utility;
	
	@Mock
	private Decryptor decryptor;
	
	@Mock
	private NotificationUtility notificationUtility;

	@Mock
	DateTimeFormatter dateTimeFormatter;
	
	private MessageDTO messageDTO;
	private String stageName;
	private InternalRegistrationStatusDto registrationStatusDto ;
	private SyncRegistrationEntity regEntity;
	
	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(packetValidateProcessor, "notificationTypes", "SMS|EMAIL");
		ReflectionTestUtils.setField(packetValidateProcessor, "dateformat", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		messageDTO=new MessageDTO();
		messageDTO.setRid("123456789");
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(true);
		messageDTO.setReg_type(RegistrationType.NEW.name());
		stageName="PacketValidatorStage";
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("123456789");
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any())).thenReturn(registrationStatusDto);
		Mockito.when(utility.getDefaultSource(any(), any())).thenReturn("reg-client");
		
		regEntity=new SyncRegistrationEntity();
		regEntity.setSupervisorStatus("APPROVED");
		regEntity.setOptionalValues("optionalvalues".getBytes());
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(any())).thenReturn(regEntity);
		
		InputStream inputStream = IOUtils.toInputStream("optionalvalues", "UTF-8");
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(inputStream);
		
		RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO = new RegistrationAdditionalInfoDTO();
		registrationAdditionalInfoDTO.setName("abc");
		registrationAdditionalInfoDTO.setPhone("9898989898");
		registrationAdditionalInfoDTO.setEmail("abc@gmail.com");
		
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any())).thenReturn(registrationAdditionalInfoDTO);
		
		Mockito.when(syncRegistrationService.deleteAdditionalInfo(any())).thenReturn(true);

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("resupdate");
		
		FieldValue preRegistrationId = new FieldValue();
		preRegistrationId.setLabel("preRegistrationId");
		preRegistrationId.setValue("123456789");

		FieldValue applicantType = new FieldValue();
		applicantType.setLabel("applicantType");
		applicantType.setValue("Child");

		FieldValue isVerified = new FieldValue();
		isVerified.setLabel("isVerified");
		isVerified.setValue("Verified");

		Document documentPob = new Document();
		documentPob.setDocumentCategory("pob");
		documentPob.setDocumentName("ProofOfBirth");
		Document document = new Document();
		document.setDocumentCategory("ProofOfRelation");
		document.setDocumentName("ProofOfRelation");
		List<Document> documents = new ArrayList<Document>();
		documents.add(documentPob);
		documents.add(document);

		List<FieldValueArray> fieldValueArrayList = new ArrayList<FieldValueArray>();

		FieldValueArray applicantBiometric = new FieldValueArray();
		applicantBiometric.setLabel(PacketFiles.APPLICANTBIOMETRICSEQUENCE.name());
		List<String> applicantBiometricValues = new ArrayList<String>();
		applicantBiometricValues.add(PacketFiles.BOTHTHUMBS.name());
		applicantBiometric.setValue(applicantBiometricValues);
		fieldValueArrayList.add(applicantBiometric);
		FieldValueArray introducerBiometric = new FieldValueArray();
		introducerBiometric.setLabel(PacketFiles.INTRODUCERBIOMETRICSEQUENCE.name());
		List<String> introducerBiometricValues = new ArrayList<String>();
		introducerBiometricValues.add("introducerLeftThumb");
		introducerBiometric.setValue(introducerBiometricValues);
		fieldValueArrayList.add(introducerBiometric);
		FieldValueArray applicantDemographic = new FieldValueArray();
		applicantDemographic.setLabel(PacketFiles.APPLICANTDEMOGRAPHICSEQUENCE.name());
		List<String> applicantDemographicValues = new ArrayList<String>();
		applicantDemographicValues.add(PacketFiles.APPLICANTPHOTO.name());
		applicantDemographicValues.add("ProofOfBirth");
		applicantDemographicValues.add("ProofOfAddress");
		applicantDemographic.setValue(applicantDemographicValues);
		fieldValueArrayList.add(applicantDemographic);
		List<String> sequence2 = new ArrayList<>();
		sequence2.add("audit");
		List<FieldValueArray> fieldValueArrayListSequence = new ArrayList<FieldValueArray>();
		FieldValueArray hashsequence2 = new FieldValueArray();
		hashsequence2.setLabel(PacketFiles.OTHERFILES.name());
		hashsequence2.setValue(sequence2);
		fieldValueArrayListSequence.add(hashsequence2);
		PowerMockito.mockStatic(JsonUtil.class);
		Mockito.when(packetValidator.validate(any(), any(),any())).thenReturn(true);
		Mockito.doNothing().when(auditUtility).saveAuditDetails(anyString(), anyString());
		
		MainResponseDTO<ReverseDatasyncReponseDTO> mainResponseDTO = new MainResponseDTO<>();
		ReverseDatasyncReponseDTO reverseDatasyncReponseDTO = new ReverseDatasyncReponseDTO();
		reverseDatasyncReponseDTO.setAlreadyStoredPreRegIds("2");
		reverseDatasyncReponseDTO.setCountOfStoredPreRegIds("2");
		reverseDatasyncReponseDTO.setTransactionId("07e3cea5-251d-11e9-a794-af3f5a85c414");
		mainResponseDTO.setErrors(null);
		mainResponseDTO.setResponsetime("2019-01-31T05:57:02.816Z");
		mainResponseDTO.setResponse(reverseDatasyncReponseDTO);
		List<String> preRegIds = new ArrayList<>();
		preRegIds.add("12345678");
		preRegIds.add("123456789");
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenReturn(mainResponseDTO);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(Matchers.any(), Matchers.any(), Matchers.any());
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(null);

		Map<String, String> metamap = new HashMap<>();
		org.json.JSONArray jsonArray = new org.json.JSONArray();
		org.json.JSONObject jsonObject1 = new org.json.JSONObject();
		metamap.put(JsonConstant.CREATIONDATE,"2023-10-17T03:01:09.893");
		metamap.put("creationDate","2023-10-17T03:01:09.893Z");
		jsonObject1.put("preRegistrationId", "12345");
		jsonArray.put(0, jsonObject1);
		metamap.put(JsonConstant.METADATA, jsonArray.toString());
		Mockito.when(packetManagerService.getMetaInfo(any(), any(), any())).thenReturn(metamap);
		Mockito.when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new FieldValue("preRegistrationId", "12345"));
	}

	@Test
	public void PacketValidationSuccessTest() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
		Map<String, String> metainfo1 = new HashMap<>();
		metainfo1.put(JsonConstant.CREATIONDATE,"2023-10-17T03:01:09.893");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);
		Mockito.verify(registrationStatusService,Mockito.atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		Assert.assertEquals(LocalDateTime.parse(metainfo1.get(JsonConstant.CREATIONDATE)), argument.getAllValues().get(0).getPacketCreateDateTime());
		Assert.assertTrue(object.getIsValid());
		Assert.assertFalse(object.getInternalError());
	}

	@Test
	public void PacketValidationSuccessTestwithPacketCreatedDateTimeNull() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
		Map<String, String> metainfo = new HashMap<>();
		metainfo.put(JsonConstant.CREATIONDATE,null);
		Mockito.when(packetManagerService.getMetaInfo(any(), any(), any())).thenReturn(metainfo);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);
		Mockito.verify(registrationStatusService,Mockito.atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(metainfo.get(JsonConstant.CREATIONDATE), argument.getAllValues().get(0).getPacketCreateDateTime());
		assertTrue(object.getIsValid());
		assertFalse(object.getInternalError());
	}

	@Test
	public void PacketValidationSuccessTestwithPacketCreatedDateTimeInvalidFormat() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
		Map<String, String> metainfo = new HashMap<>();
		metainfo.put(JsonConstant.CREATIONDATE,"2023-10-1703:01:09.893");
		Mockito.when(packetManagerService.getMetaInfo(any(), any(), any())).thenReturn(metainfo);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);
		Mockito.verify(registrationStatusService,Mockito.atLeastOnce()).updateRegistrationStatus(argument.capture(),any(),any());
		assertEquals(null, argument.getAllValues().get(0).getPacketCreateDateTime());
	}

	@Test
	public void PacketValidationFailureTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		registrationStatusDto.setRetryCount(1);
		Mockito.when(packetValidator.validate(any(), any(),any())).thenReturn(false);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertFalse(object.getInternalError());
	}
	@Test
	public void PacketValidationPacketManagerFailedTest()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		Mockito.when(packetManagerService.getMetaInfo(anyString(), any(), any()))
				.thenThrow(PacketManagerException.class);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}

	@Test
	public void invalidSupervisorStatusTest() throws PacketValidatorException {
		registrationStatusDto.setRetryCount(1);
		regEntity=new SyncRegistrationEntity();
		regEntity.setSupervisorStatus("REJECTED");
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(any())).thenReturn(regEntity);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertFalse(object.getInternalError());
	}
	@Test
	public void PacketValidationParsingFailedTest()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		Mockito.when(packetManagerService.getMetaInfo(anyString(), any(), any()))
				.thenThrow(ParsingException.class);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PARSE_EXCEPTION)).thenReturn("ERROR");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void PacketValidationAPIResourceExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		ApisResourceAccessException exc=new ApisResourceAccessException("Ex");
		Mockito.when(packetValidator.validate(any(),any(), any())).thenThrow(exc);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void PacketValidationIOExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		IOException exc=new IOException("Ex");
		Mockito.when(packetValidator.validate(any(),any(), any())).thenThrow(exc);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION)).thenReturn("ERROR");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void PacketValidationBaseCheckedExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		RegistrationProcessorCheckedException exc=new RegistrationProcessorCheckedException("", "", new RegistrationProcessorCheckedException("", ""));
		Mockito.when(packetValidator.validate(any(), any(),any())).thenThrow(exc);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.BASE_CHECKED_EXCEPTION)).thenReturn("ERROR");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void notificationSendFailedTest() throws PacketValidatorException, ApisResourceAccessException,
			PacketManagerException, JsonProcessingException, IOException, JSONException {
		Mockito.doThrow(IOException.class).when(notificationUtility).sendNotification(any(), any(), any(), any(),
				anyBoolean(),anyBoolean());
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertFalse(object.getInternalError());
	}

	@Test
	public void ReverseDataSyncFailureTest() throws ApisResourceAccessException {
		MainResponseDTO<ReverseDatasyncReponseDTO> mainResponseDTO = new MainResponseDTO<>();
		ExceptionJSONInfoDTO dto=new ExceptionJSONInfoDTO();
		dto.setErrorCode("");
		dto.setMessage("");
		mainResponseDTO.setErrors(Arrays.asList(dto));
		mainResponseDTO.setResponsetime("2019-01-31T05:57:02.816Z");
		mainResponseDTO.setResponse(null);
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenReturn(mainResponseDTO);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertFalse(object.getInternalError());
	}
	
	@Test
	public void ReverseDataSyncNullResponseTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenReturn(null);
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertFalse(object.getInternalError());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceExceptionClientTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(any(), any(), any(), any(),
				any())).thenThrow(new ApisResourceAccessException("",new HttpClientErrorException(HttpStatus.GATEWAY_TIMEOUT, "")));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceExceptionServerTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenThrow(new ApisResourceAccessException("",new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "")));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceExceptionTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenThrow(new ApisResourceAccessException(""));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}

	@Test
	public void TableNotAccessibleExceptionest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any()))
				.thenThrow( TablenotAccessibleException.class);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}

	@Test
	public void DataNotAccessibleExceptionest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(),any(),any(),any()))
				.thenThrow( new DataAccessException("DataAccessException") {});
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION))
				.thenReturn("REPROCESS");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertTrue(object.getIsValid());
		assertTrue(object.getInternalError());
	}

	@Test
	public void BaseUnCheckedExceptionTest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenThrow(BaseUncheckedException.class);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.BASE_UNCHECKED_EXCEPTION)).thenReturn("ERROR");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse(object.getIsValid());
		assertTrue(object.getInternalError());
	}

	@Test
	public void packetManagerNonRecoverableExceptionTest()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		Mockito.when(packetManagerService.getMetaInfo(anyString(), any(), any()))
				.thenThrow(PacketManagerNonRecoverableException.class);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION)).thenReturn("FAILED");
		MessageDTO object = packetValidateProcessor.process(messageDTO, stageName);
		assertFalse((object.getIsValid()));
		assertTrue(object.getInternalError());
	}
	
}
