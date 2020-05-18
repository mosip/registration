package io.mosip.registration.processor.stages.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import org.apache.commons.io.IOUtils;
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
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class, HMACUtils.class, Utilities.class, MasterDataValidation.class,
		MessageDigest.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
@TestPropertySource(locations = "classpath:application.properties")
public class PacketValidatorImplTest {
	
	@InjectMocks
	private io.mosip.registration.processor.core.spi.packet.validator.PacketValidator PacketValidator=new PacketValidatorImpl();
	
	@Mock
	private PacketReaderService packetReaderService;
	
	@Mock
	IdObjectsSchemaValidationOperationMapper idObjectsSchemaValidationOperationMapper;
	
	@Mock
	IdObjectValidator idObjectValidator;
	
	@Mock
	private Utilities utility;
	
	@Mock
	private Environment env;
	
	@Mock
	ApplicantTypeDocument applicantTypeDocument;
	
	@Mock
	private IdRepoService idRepoService;
	
	@Mock
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;
	
	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	InternalRegistrationStatusDto registrationStatusDto=new InternalRegistrationStatusDto();
	PacketMetaInfo packetMetaInfo=new PacketMetaInfo();
	MessageDTO messageDTO=new MessageDTO();
	PacketValidationDto packetValidationDto=new PacketValidationDto();

	private InputStream inputStream;
	
	private static final String PRIMARY_LANGUAGE = "mosip.primary-language";

	private static final String SECONDARY_LANGUAGE = "mosip.secondary-languag";

	private static final String ATTRIBUTES = "registration.processor.masterdata.validation.attributes";

	private String VALIDATESCHEMA = "registration.processor.validateSchema";

	private String VALIDATEFILE = "registration.processor.validateFile";

	private String VALIDATECHECKSUM = "registration.processor.validateChecksum";

	private String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";

	private String VALIDATEMASTERDATA = "registration.processor.validateMasterData";

	private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";
	
	@Before
	public void setup() throws Exception {
		messageDTO.setRid("123456789");
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(true);
		messageDTO.setReg_type(RegistrationType.UPDATE);
		registrationStatusDto.setRegistrationId("123456789");
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
		ReflectionTestUtils.setField(PacketValidator, "source", "id");
		when(env.getProperty(anyString())).thenReturn("gender");
		when(env.getProperty(PRIMARY_LANGUAGE)).thenReturn("eng");
		when(env.getProperty(SECONDARY_LANGUAGE)).thenReturn("ara");
		when(env.getProperty(ATTRIBUTES)).thenReturn("gender,region,province,city");
		when(env.getProperty(VALIDATESCHEMA)).thenReturn("true");
		when(env.getProperty(VALIDATEFILE)).thenReturn("true");
		when(env.getProperty(VALIDATECHECKSUM)).thenReturn("true");
		when(env.getProperty(VALIDATEAPPLICANTDOCUMENT)).thenReturn("false");
		when(env.getProperty(VALIDATEMASTERDATA)).thenReturn("true");
		when(env.getProperty(VALIDATEMANDATORY)).thenReturn("false");
		Mockito.when(idObjectValidator.validateIdObject(any(), any())).thenReturn(true);
		Mockito.when(packetReaderService.checkFileExistence(anyString(), anyString(),anyString())).thenReturn(true);
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("ID.json").getFile());
		inputStream = new FileInputStream(file);
		Mockito.when(packetReaderService.getFile(anyString(), anyString(),anyString())).thenReturn(inputStream);
		Mockito.when(idObjectsSchemaValidationOperationMapper.getOperation(anyString())).thenReturn(IdObjectValidatorSupportedOperations.NEW_REGISTRATION);
		Mockito.when(idObjectValidator.validateIdObject(any(), any())).thenReturn(true);
		
		
		String test = "{}";
		byte[] data = "{}".getBytes();
		
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);
		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jsonObject);
		PowerMockito.when(JsonUtil.getJSONObject(jsonObject, "individualBiometrics")).thenReturn(jsonObject);
		Mockito.when(jsonObject.get("value")).thenReturn("applicantCBEF");
		Mockito.when(utility.getUIn(any())).thenReturn(12345678l);
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("ACTIVE");
		
		List<SyncRegistrationEntity> synchRecordList = new ArrayList<>();
		synchRecordList.add(new SyncRegistrationEntity());
		Mockito.when(idRepoService.findUinFromIdrepo(any(), any())).thenReturn(1);

		Mockito.when(registrationRepositary.getSyncRecordsByRegIdAndRegType(any(), any())).thenReturn(synchRecordList);
	}
	
	@Test
	@Ignore
	public void testValidationSuccess() throws PacketValidatorException {
		assertTrue(PacketValidator.validate("", "", packetValidationDto));
	}
	
}