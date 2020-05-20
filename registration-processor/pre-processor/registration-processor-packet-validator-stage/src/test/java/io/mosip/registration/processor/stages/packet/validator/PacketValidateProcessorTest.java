package io.mosip.registration.processor.stages.packet.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ExceptionJSONInfoDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

/**
 * The Class PacketValidatorStageTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class, HMACUtils.class, Utilities.class, MasterDataValidation.class,
		MessageDigest.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
@TestPropertySource(locations = "classpath:application.properties")
public class PacketValidateProcessorTest {
	@InjectMocks
	private PacketValidateProcessor packetValidateProcessor;
	@Mock
	private PacketReaderService packetReaderService;

	@Mock
	private PacketValidator packetValidator;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private AuditUtility auditUtility;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
	
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	private MessageDTO messageDTO;
	private String stageName;
	private InternalRegistrationStatusDto registrationStatusDto ;
	private SyncRegistrationEntity regEntity;
	@Mock
	private InputStream packetMetaInfoStream;
	private PacketMetaInfo packetMetaInfo;
	
	@Before
	public void setup() throws Exception {
		messageDTO=new MessageDTO();
		messageDTO.setRid("123456789");
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(true);
		messageDTO.setReg_type(RegistrationType.NEW);
		stageName="PacketValidatorStage";
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("123456789");
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		
		regEntity=new SyncRegistrationEntity();
		regEntity.setSupervisorStatus("APPROVED");
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		packetMetaInfo = new PacketMetaInfo();
		Identity identity = new Identity();

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

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified,preRegistrationId));

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
		identity.setHashSequence(fieldValueArrayList);
		List<String> sequence2 = new ArrayList<>();
		sequence2.add("audit");
		List<FieldValueArray> fieldValueArrayListSequence = new ArrayList<FieldValueArray>();
		FieldValueArray hashsequence2 = new FieldValueArray();
		hashsequence2.setLabel(PacketFiles.OTHERFILES.name());
		hashsequence2.setValue(sequence2);
		fieldValueArrayListSequence.add(hashsequence2);
		identity.setHashSequence2(fieldValueArrayListSequence);
		packetMetaInfo.setIdentity(identity);
		Mockito.when(packetReaderService.getFile(anyString(),anyString(),anyString())).thenReturn(packetMetaInfoStream);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", packetMetaInfoStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenReturn(true);
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
		
	}
	
	@Test
	public void PacketValidationSuccessTest() {
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@Test
	public void PacketValidationFailureTest() throws PacketValidatorException {
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION)).thenReturn("ERROR");
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenReturn(false);
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
	public void PacketValidationAPIResourceExceptionTest() throws PacketValidatorException {
		PacketValidatorException exc=new PacketValidatorException(new ApiNotAccessibleException(""));
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	public void PacketValidationIOExceptionTest() throws PacketValidatorException {
		PacketValidatorException exc=new PacketValidatorException(new IOException(""));
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	public void PacketValidationBaseCheckedExceptionTest() throws PacketValidatorException {
		PacketValidatorException exc=new PacketValidatorException(new BaseCheckedException());
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	public void PacketValidationBaseUncheckedExceptionTest() throws PacketValidatorException {
		PacketValidatorException exc=new PacketValidatorException(new BaseUncheckedException());
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenThrow(exc);
		assertTrue(packetValidateProcessor.process(messageDTO, stageName).getInternalError());
	}
	
	@Test
	public void PacketValidationExceptionTest() throws PacketValidatorException {
		PacketValidatorException exc=new PacketValidatorException(new Exception());
		Mockito.when(packetValidator.validate(anyString(), anyString(),any())).thenThrow(exc);
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
		
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenThrow(new ApisResourceAccessException("",new HttpClientErrorException(HttpStatus.GATEWAY_TIMEOUT, "")));
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
	public void IOExceptionest() throws Exception  {
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", packetMetaInfoStream, PacketMetaInfo.class)
				.thenThrow( UnsupportedEncodingException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void Exceptionest() throws Exception  {
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", packetMetaInfoStream, PacketMetaInfo.class)
				.thenThrow( NullPointerException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
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
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow( BaseCheckedException.class);
		
		assertFalse(packetValidateProcessor.process(messageDTO, stageName).getIsValid());
	}
	
}