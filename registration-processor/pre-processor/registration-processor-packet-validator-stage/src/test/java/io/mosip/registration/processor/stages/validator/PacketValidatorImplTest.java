package io.mosip.registration.processor.stages.validator;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
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
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class, HMACUtils.class, Utilities.class, MasterDataValidation.class,
		MessageDigest.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })
@TestPropertySource(locations = "classpath:application.properties")
public class PacketValidatorImplTest {
	
	@InjectMocks
	private io.mosip.registration.processor.core.spi.packet.validator.PacketValidator PacketValidator=new PacketValidatorImpl();

	@Mock
	IdObjectValidator idObjectValidator;
	
	@Mock
	private Utilities utility;

	@Mock
	private MandatoryValidation mandatoryValidation;

	@Mock
	private MasterDataValidation masterDataValidation;
	
	@Mock
	private Environment env;
	
	@Mock
	ApplicantTypeDocument applicantTypeDocument;
	
	@Mock
	private IdRepoService idRepoService;

	@Mock
    private PacketManagerService packetManagerService;

	@Mock
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;
	
	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Value("${packet.default.source}")
	private String source;

	

	private PacketMetaInfo packetMetaInfo=new PacketMetaInfo();

	private FileInputStream inputStream;

	private PacketValidationDto packetValidationDto;

	public static final String APPROVED = "APPROVED";
	public static final String REJECTED = "REJECTED";
	private static final String VALIDATESCHEMA = "registration.processor.validateSchema";
	private static final String VALIDATEFILE = "registration.processor.validateFile";
	private static final String VALIDATECHECKSUM = "registration.processor.validateChecksum";
	private static final String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";
	private static final String VALIDATEMASTERDATA = "registration.processor.validateMasterData";
	private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";

	private static final String PRIMARY_LANGUAGE = "mosip.primary-language";

	private static final String SECONDARY_LANGUAGE = "mosip.secondary-language";

	private static final String ATTRIBUTES = "registration.processor.masterdata.validation.attributes";
	
	@Before
	public void setup() throws Exception {
		packetValidationDto=new PacketValidationDto();
		
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
		
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("ID.json").getFile());
		inputStream = new FileInputStream(file);
		//Mockito.when(packetReaderService.getFile(anyString(), anyString(),anyString())).thenReturn(inputStream);
		//when(packetReaderService.getFile(anyString(),anyString(),anyString())).thenReturn(inputStream);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
		.thenReturn(packetMetaInfo);
		when(masterDataValidation.validateMasterData(anyString(),anyString(),anyString())).thenReturn(true);
		when(mandatoryValidation.mandatoryFieldValidation(anyString(),anyString(),anyString(),any())).thenReturn(true);
		byte[] data = "{}".getBytes();
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);
		
		ReflectionTestUtils.setField(PacketValidator, "source", "id");
		when(env.getProperty(anyString())).thenReturn("gender");
		when(env.getProperty(PRIMARY_LANGUAGE)).thenReturn("eng");
		when(env.getProperty(SECONDARY_LANGUAGE)).thenReturn("ara");
		when(env.getProperty(ATTRIBUTES)).thenReturn("gender,region,province,city");
		when(env.getProperty(VALIDATESCHEMA)).thenReturn("true");
		when(env.getProperty(VALIDATEFILE)).thenReturn("true");
		when(env.getProperty(VALIDATECHECKSUM)).thenReturn("true");
		when(env.getProperty(VALIDATEAPPLICANTDOCUMENT)).thenReturn("true");
		when(env.getProperty(VALIDATEMASTERDATA)).thenReturn("true");
		when(env.getProperty(VALIDATEMANDATORY)).thenReturn("true");
		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		Mockito.when(idObjectValidator.validateIdObject(any(),any(), any())).thenReturn(true);

		PowerMockito.when(JsonUtil.getJSONObject(jsonObject, "individualBiometrics")).thenReturn(jsonObject);
		Mockito.when(jsonObject.get("value")).thenReturn("applicantCBEF");
		
		Mockito.when(utility.getUIn(anyString(),anyString(),anyString())).thenReturn("12345678l");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("ACTIVE");
		when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");
		when(utility.getMappingJsonValue(anyString())).thenReturn("value");
		Mockito.when(idRepoService.findUinFromIdrepo(anyString(), any())).thenReturn("123456781");
        ValidatePacketResponse validatePacketResponse = new ValidatePacketResponse();
        validatePacketResponse.setValid(true);
		when(packetManagerService.validate(anyString(),anyString(),anyString())).thenReturn(validatePacketResponse);
        BiometricRecord biometricRecord = new BiometricRecord();
        BIR bir = new BIR.BIRBuilder().build();
        biometricRecord.setSegments(Lists.newArrayList(bir,bir));
        when(packetManagerService.getBiometrics(anyString(),anyString(),any(),anyString(),anyString())).thenReturn(biometricRecord);
	}
	
	@Test
	public void testValidationSuccess() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		assertTrue(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
	@Test
	public void testUpdateValidationSuccess() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		assertTrue(PacketValidator.validate("123456789", "reg_client","UPDATE", packetValidationDto));
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=PacketManagerException.class)
	public void testException() throws PacketValidatorException, io.mosip.kernel.core.exception.IOException, IOException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, PacketManagerException {
		//when(packetReaderService.getFile(anyString(),anyString(),anyString())).thenThrow(PacketDecryptionFailureException.class);
        when(mandatoryValidation.mandatoryFieldValidation(anyString(),anyString(),anyString(),any())).thenThrow(new PacketManagerException("code","message"));
		PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto);
	}
	
	@Test
	public void testUINNotPresentinIDrepo() throws PacketValidatorException, ApisResourceAccessException, IOException, RegistrationProcessorCheckedException, JsonProcessingException, PacketManagerException {
		Mockito.when(idRepoService.findUinFromIdrepo(anyString(), any())).thenReturn(null);
		assertFalse(PacketValidator.validate("123456789", "reg_client","UPDATE", packetValidationDto));
	}
	
	@Test
	public void testValidationConfigSuccess() throws PacketValidatorException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, IOException, PacketManagerException {
		when(env.getProperty(VALIDATESCHEMA)).thenReturn("false");
		when(env.getProperty(VALIDATEFILE)).thenReturn("false");
		when(env.getProperty(VALIDATECHECKSUM)).thenReturn("false");
		when(env.getProperty(VALIDATEAPPLICANTDOCUMENT)).thenReturn("false");
		when(env.getProperty(VALIDATEMASTERDATA)).thenReturn("false");
		when(env.getProperty(VALIDATEMANDATORY)).thenReturn("false");
		assertTrue(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
	@Test
	public void testindividualBiometricsValidationFailure() throws PacketValidatorException, io.mosip.kernel.core.exception.IOException, IOException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, PacketManagerException {
        when(packetManagerService.getBiometrics(anyString(),anyString(),any(),anyString(),anyString())).thenReturn(null);
		assertFalse(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
	@Test
	public void testMasterdataValidationFailure() throws PacketValidatorException, ApisResourceAccessException, IOException, RegistrationProcessorCheckedException, JsonProcessingException, PacketManagerException {
		when(masterDataValidation.validateMasterData(anyString(),anyString(),anyString())).thenReturn(false);
		assertFalse(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
	@Test
	public void testMandatoryValidationFailure() throws PacketValidatorException, io.mosip.kernel.core.exception.IOException, IOException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, PacketManagerException {
		when(mandatoryValidation.mandatoryFieldValidation(anyString(),anyString(), anyString(),any())).thenReturn(false);
		assertFalse(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
	@Test
	public void testPacketManagerValidationFailure() throws PacketValidatorException, IOException, IdentityNotFoundException, ApisResourceAccessException, JsonProcessingException, RegistrationProcessorCheckedException, PacketManagerException {
        ValidatePacketResponse validatePacketResponse = new ValidatePacketResponse();
        validatePacketResponse.setValid(false);
        when(packetManagerService.validate(anyString(),anyString(),anyString())).thenReturn(validatePacketResponse);
		assertFalse(PacketValidator.validate("123456789", "reg_client","NEW", packetValidationDto));
	}
	
}
