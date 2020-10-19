package io.mosip.registration.processor.stages.packet.validator;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ExceptionJSONInfoDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;


/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtils.class, JsonUtil.class, IOUtils.class, HMACUtils.class, Utilities.class, MasterDataValidation.class,
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
	AuditLogRequestBuilder auditLogRequestBuilder;

	
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private AuditUtility auditUtility;

	@Mock
	private PacketManagerService packetManagerService;

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
	
	private MessageDTO messageDTO;
	private String stageName;
	private InternalRegistrationStatusDto registrationStatusDto ;
	private SyncRegistrationEntity regEntity;
	
	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(packetValidateProcessor, "notificationTypes", "SMS|EMAIL");
		messageDTO=new MessageDTO();
		messageDTO.setRid("123456789");
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(true);
		messageDTO.setReg_type(RegistrationType.NEW);
		stageName="PacketValidatorStage";
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("123456789");
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(utility.getDefaultSource()).thenReturn("reg-client");
		
		regEntity=new SyncRegistrationEntity();
		regEntity.setSupervisorStatus("APPROVED");
		regEntity.setOptionalValues("optionalvalues".getBytes());
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		
		InputStream inputStream = IOUtils.toInputStream("optionalvalues", "UTF-8");
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any())).thenReturn(inputStream);
		
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
		registrationType.setLabel("preRegistrationId");
		registrationType.setValue("123456789");

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
		Mockito.when(packetValidator.validate(any(), any(), any(),any())).thenReturn(true);
		Mockito.doNothing().when(auditUtility).saveAuditDetails(anyString(), anyString(), anyString());
		
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
		jsonObject1.put("preRegistrationId", "12345");
		jsonArray.put(0, jsonObject1);
		metamap.put(JsonConstant.METADATA, jsonArray.toString());
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metamap);


	}
	
	@Test
	public void PacketValidationSuccessTest() {
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void PacketValidationFailureTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION)).thenReturn("ERROR");
		Mockito.when(packetValidator.validate(any(), any(), any(),any())).thenReturn(false);
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void invalidSupervisorStatusTest() throws PacketValidatorException {
		regEntity=new SyncRegistrationEntity();
		regEntity.setSupervisorStatus("REJECTED");
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	@Ignore
	public void PacketValidationAPIResourceExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		PacketValidatorException exc=new PacketValidatorException("", "", new ApisResourceAccessException(""));
		Mockito.when(packetValidator.validate(any(),any(), any(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	@Ignore
	public void PacketValidationIOExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		PacketValidatorException exc=new PacketValidatorException("", "", new IOException(""));
		Mockito.when(packetValidator.validate(any(),any(), any(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	@Ignore
	public void PacketValidationBaseCheckedExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		PacketValidatorException exc=new PacketValidatorException("", "", new RegistrationProcessorCheckedException("", ""));
		Mockito.when(packetValidator.validate(any(), any(), any(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	@Ignore
	public void PacketValidationBaseUncheckedExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		PacketValidatorException exc=new PacketValidatorException("", "", new BaseUncheckedException());
		Mockito.when(packetValidator.validate(any(), any(), any(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	@Ignore
	public void PacketValidationExceptionTest() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		PacketValidatorException exc=new PacketValidatorException("", "", new Exception());
		Mockito.when(packetValidator.validate(any(), any(), any(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
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
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void ReverseDataSyncNullResponseTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenReturn(null);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceEsxceptionClientTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(any(), any(), any(), any(),
				any())).thenThrow(new ApisResourceAccessException("",new HttpClientErrorException(HttpStatus.GATEWAY_TIMEOUT, "")));
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceEsxceptionServerTest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenThrow(new ApisResourceAccessException("",new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "")));
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void ReverseDataSyncAPIResourceExceptionest() throws ApisResourceAccessException {
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenThrow(new ApisResourceAccessException(""));
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void TableNotAccessibleExceptionest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow( TablenotAccessibleException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void DataNotAccessibleExceptionest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow( BaseUncheckedException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void BaseCheckedExceptionTest() throws Exception  {
		Mockito.when(registrationStatusService.getRegistrationStatus(any()))
				.thenThrow(BaseUncheckedException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
}
