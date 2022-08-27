package io.mosip.registration.processor.stages.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.exception.CbeffException;
import io.mosip.kernel.core.exception.BiometricSignatureValidationException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import io.mosip.registration.processor.stages.utils.BiometricsXSDValidator;
import io.mosip.registration.processor.stages.validator.impl.BiometricsSignatureValidator;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;

@RunWith(MockitoJUnitRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class PacketValidatorImplTest {

	@InjectMocks
	private io.mosip.registration.processor.core.spi.packet.validator.PacketValidator PacketValidator = new PacketValidatorImpl();

	@Mock
	IdObjectValidator idObjectValidator;

	@Mock
	private Utilities utility;

	@Mock
	private BiometricsXSDValidator biometricsXSDValidator;
	
	@Mock
	private BiometricsSignatureValidator biometricsSignatureValidator;

	@Mock
	private Environment env;

	@Mock
	ApplicantTypeDocument applicantTypeDocument;

	@Mock
	private IdRepoService idRepoService;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private SyncRegistrationRepository<SyncRegistrationEntity, String> registrationRepositary;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private ApplicantDocumentValidation applicantDocumentValidation;

	@Mock
	ObjectMapper mapper;

	@Value("${packetmanager.name.source.default}")
	private String source;

	private PacketMetaInfo packetMetaInfo = new PacketMetaInfo();

	private FileInputStream inputStream;

	private PacketValidationDto packetValidationDto;

	public static final String APPROVED = "APPROVED";
	public static final String REJECTED = "REJECTED";
	private static final String VALIDATEAPPLICANTDOCUMENT = "mosip.regproc.packet.validator.validate-applicant-document";
	private static final String VALIDATEAPPLICANTDOCUMENTPROCESS = "mosip.regproc.packet.validator.validate-applicant-document.processes";

	private static final String PRIMARY_LANGUAGE = "mosip.primary-language";

	private static final String SECONDARY_LANGUAGE = "mosip.secondary-language";

	JSONObject jsonObject = Mockito.mock(JSONObject.class);
	Map<String, String> metamap = new HashMap<>();

	@Before
	public void setup() throws Exception {
		packetValidationDto = new PacketValidationDto();

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

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified, preRegistrationId));

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

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("ID.json").getFile());
		inputStream = new FileInputStream(file);
		// Mockito.when(packetReaderService.getFile(anyString(),
		// anyString(),anyString())).thenReturn(inputStream);
		// when(packetReaderService.getFile(anyString(),anyString(),anyString())).thenReturn(inputStream);


		byte[] data = "{}".getBytes();
		when(env.getProperty(anyString())).thenReturn("true").thenReturn("NEW");

		Mockito.when(utility.uinPresentInIdRepo(any())).thenReturn(true);
		ValidatePacketResponse validatePacketResponse = new ValidatePacketResponse();
		validatePacketResponse.setValid(true);
		when(packetManagerService.validate(anyString(), anyString(), any())).thenReturn(validatePacketResponse);
		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir = new BIR.BIRBuilder().build();
		biometricRecord.setSegments(Lists.newArrayList(bir, bir));
		when(packetManagerService.getField(any(), any(), any(), any())).thenReturn("biometricsField");
		doNothing().when(biometricsXSDValidator).validateXSD(any());
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(biometricRecord);
		when(applicantDocumentValidation.validateDocument(any(), any())).thenReturn(true);

		JSONArray jsonArray = new JSONArray();
		org.json.JSONObject jsonObject = new org.json.JSONObject();
		jsonObject.put(MappingJsonConstants.OFFICERBIOMETRICFILENAME, "officerBiometricFilename");
		jsonArray.put(0, jsonObject);
		metamap.put(JsonConstant.OPERATIONSDATA, jsonArray.toString());
		Mockito.when(packetManagerService.getMetaInfo(anyString(), any(), any())).thenReturn(metamap);
		Mockito.when(mapper.readValue(anyString(), any(Class.class)))
				.thenReturn(new FieldValue(MappingJsonConstants.OFFICERBIOMETRICFILENAME, "officerBiometricFilename"));
	}

	@Test
	public void testValidationSuccess() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.doNothing().when(biometricsSignatureValidator).validateSignature(anyString(), anyString(), any(),
				any());
		assertTrue(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}
	
	@Test(expected = PacketManagerException.class)
	public void testPacketManagerException() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		when(packetManagerService.getField(any(), any(), any(), any())).thenThrow(new PacketManagerException("",""));
		assertTrue(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}
	
	@Test
	public void testDocumentValidationFailure() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		
		when(applicantDocumentValidation.validateDocument(any(), any())).thenReturn(false);
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test
	public void testdocumentValidationFailed() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		when(applicantDocumentValidation.validateDocument(any(), any())).thenReturn(false);
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test
	public void testUpdateValidationSuccess() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.when(utility.getUIn(anyString(), anyString(), any())).thenReturn("12345678l");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("ACTIVE");
		Mockito.doNothing().when(biometricsSignatureValidator).validateSignature(anyString(), anyString(), any(),
				any());
		assertTrue(PacketValidator.validate("123456789", "UPDATE", packetValidationDto));
	}

	@Test
	public void testUINNotPresentinIDrepo() throws PacketValidatorException, ApisResourceAccessException, IOException,
			RegistrationProcessorCheckedException, JsonProcessingException, PacketManagerException {
		Mockito.when(utility.uinPresentInIdRepo(any())).thenReturn(false);
		Mockito.when(utility.getUIn(anyString(), anyString(), any())).thenReturn("12345678l");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("ACTIVE");
		
		assertFalse(PacketValidator.validate("123456789", "UPDATE", packetValidationDto));
	}

	@Test(expected = IdRepoAppException.class)
	public void testValidationUINNull() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.when(utility.getUIn(anyString(), anyString(), any())).thenReturn(null);
		
		PacketValidator.validate("123456789", "UPDATE", packetValidationDto);
	}

	@Test(expected = IdRepoAppException.class)
	public void testValidationJsonNull() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.when(utility.getUIn(anyString(), anyString(), any())).thenReturn("12345678l");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(null);
		
		PacketValidator.validate("123456789", "UPDATE", packetValidationDto);
	}

	@Test(expected = RegistrationProcessorCheckedException.class)
	public void testValidationStatusDeactived() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.when(utility.getUIn(anyString(), anyString(), any())).thenReturn("12345678l");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("deactivated");
		
		PacketValidator.validate("123456789", "UPDATE", packetValidationDto);
	}

	@Test
	public void testValidationConfigSuccess() throws PacketValidatorException, ApisResourceAccessException,
			JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException,
			BiometricSignatureValidationException, JSONException {
		Mockito.doNothing().when(biometricsSignatureValidator).validateSignature(anyString(), anyString(), any(),
				any());
		when(env.getProperty(VALIDATEAPPLICANTDOCUMENT)).thenReturn("false");
		assertTrue(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test(expected = RegistrationProcessorCheckedException.class)
	public void testBiometricsXSDValidatonException() throws Exception {
		doThrow(new Exception("IO Exception occurred")).when(biometricsXSDValidator).validateXSD(any());
		PacketValidator.validate("123456789", "NEW", packetValidationDto);
	}

	@Test
	public void testBiometricsXSDValidatonFailure() throws Exception {
		doThrow(new CbeffException("CbeffException occurred")).when(biometricsXSDValidator).validateXSD(any());
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}
	
	@Test
	public void testBiometricsSignatureValidatonFailure() throws Exception {
		doThrow(new BiometricSignatureValidationException("JWT signature Validation Failed"))
				.when(biometricsSignatureValidator).validateSignature(anyString(), anyString(), any(), any());
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test
	public void testPacketManagerValidationFailure()
			throws IOException, IdentityNotFoundException, ApisResourceAccessException, JsonProcessingException,
			RegistrationProcessorCheckedException, PacketManagerException {
		ValidatePacketResponse validatePacketResponse = new ValidatePacketResponse();
		validatePacketResponse.setValid(false);
		when(packetManagerService.validate(anyString(), anyString(), any())).thenReturn(validatePacketResponse);
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test(expected = PacketManagerException.class)
	public void testPacketManagerValidationException() throws PacketManagerException, ApisResourceAccessException,
			JsonProcessingException, IOException, RegistrationProcessorCheckedException {
		when(packetManagerService.validate(anyString(), anyString(), any())).thenThrow(PacketManagerException.class);
		assertFalse(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

	@Test
	public void testJsonException() throws PacketManagerException, ApisResourceAccessException, JsonProcessingException,
			IOException, RegistrationProcessorCheckedException, JSONException {
		org.json.JSONObject jsonObject = new org.json.JSONObject();
		jsonObject.put(MappingJsonConstants.OFFICERBIOMETRICFILENAME, "officerBiometricFilename");
		metamap.put(JsonConstant.OPERATIONSDATA, jsonObject.toString());
		Mockito.when(packetManagerService.getMetaInfo(anyString(), any(), any())).thenReturn(metamap);
		assertTrue(PacketValidator.validate("123456789", "NEW", packetValidationDto));
	}

}