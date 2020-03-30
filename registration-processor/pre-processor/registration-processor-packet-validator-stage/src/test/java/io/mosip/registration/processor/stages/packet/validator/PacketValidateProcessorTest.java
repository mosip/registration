package io.mosip.registration.processor.stages.packet.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.packet.dto.masterdata.StatusResponseDto;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ExceptionJSONInfoDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.MainResponseDTO;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.ReverseDatasyncReponseDTO;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.stages.utils.CheckSumValidation;
import io.mosip.registration.processor.stages.utils.DocumentUtility;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
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

	/** The input stream. */
	private InputStream inputStream;

	/** The filesystem ceph adapter impl. */
	@Mock
	private PacketManager filesystemCephAdapterImpl;

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private LogDescription description;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/** The packet info manager. */
	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	CheckSumValidation checkSumValidation = new CheckSumValidation(filesystemCephAdapterImpl, registrationStatusDto);

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The dto. */
	MessageDTO dto = new MessageDTO();

	/** The packet validator stage. */
	@InjectMocks
	private PacketValidateProcessor packetValidateProcessor;

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder = new AuditLogRequestBuilder();

	@Mock
	private Environment env;

	/** The packet meta info. */
	private PacketMetaInfo packetMetaInfo;
	/** The identity. */
	Identity identity = new Identity();

	/** The dto. */
	InternalRegistrationStatusDto statusDto;
	/** The list. */
	List<InternalRegistrationStatusDto> list;

	/** The list appender. */
	private ListAppender<ILoggingEvent> listAppender;

	/** The document utility. */
	@Mock
	DocumentUtility documentUtility;

	@Mock
	ABISHandlerUtil handlerUtil;

	@Mock
	private IdRepoService idRepoService;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Mock
	IdObjectValidator idObjectValidator;

	@Mock
	IdObjectsSchemaValidationOperationMapper idObjectsSchemaValidationOperationMapper;

	@Mock
	private Utilities utility;

	@Mock
	ObjectMapper mapIdentityJsonStringToObject;

	@Mock
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;

	@Mock
	private MessageDigest messageDigestMock;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	StatusResponseDto statusResponseDto;
	private static final String PRIMARY_LANGUAGE = "mosip.primary-language";

	private static final String SECONDARY_LANGUAGE = "mosip.secondary-languag";

	private static final String ATTRIBUTES = "registration.processor.masterdata.validation.attributes";

	private String VALIDATESCHEMA = "registration.processor.validateSchema";

	private String VALIDATEFILE = "registration.processor.validateFile";

	private String VALIDATECHECKSUM = "registration.processor.validateChecksum";

	private String VALIDATEAPPLICANTDOCUMENT = "registration.processor.validateApplicantDocument";

	private String VALIDATEMASTERDATA = "registration.processor.validateMasterData";

	private static final String VALIDATEMANDATORY = "registration-processor.validatemandotary";
	private String stageName = "PacketValidatorStage";

	private SyncRegistrationEntity regEntity;

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		regEntity = new SyncRegistrationEntity();
		regEntity.setCreateDateTime(LocalDateTime.now());
		regEntity.setSupervisorStatus("APPROVED");
		regEntity.setId("001");

		list = new ArrayList<InternalRegistrationStatusDto>();

		listAppender = new ListAppender<>();

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("ID.json").getFile());
		inputStream = new FileInputStream(file);

		dto.setRid("2018701130000410092018110735");
		dto.setReg_type(RegistrationType.valueOf("UPDATE"));

		MockitoAnnotations.initMocks(this);
		packetMetaInfo = new PacketMetaInfo();

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("NEW");

		FieldValue applicantType = new FieldValue();
		applicantType.setLabel("applicantType");
		applicantType.setValue("Child");

		FieldValue isVerified = new FieldValue();
		isVerified.setLabel("isVerified");
		isVerified.setValue("Verified");

		FieldValue preRegistrationId = new FieldValue();
		preRegistrationId.setLabel("preRegistrationId");
		preRegistrationId.setValue("2018701130000410092018110736");

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified, preRegistrationId));

		Document documentPob = new Document();
		documentPob.setDocumentCategory("PROOFOFDATEOFBIRTH");
		documentPob.setDocumentName("ProofOfBirth");
		Document document = new Document();
		document.setDocumentCategory("PROOFOFRELATIONSHIP");
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
		applicantDemographicValues.add("ProofOfRelation");
		applicantDemographicValues.add("ProofOfAddress");
		applicantDemographicValues.add("ProofOfIdentity");
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

		AuditResponseDto auditResponseDto = new AuditResponseDto();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_405.toString(), EventName.UPDATE.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		String test = "{}";
		byte[] data = "{}".getBytes();
		// Mockito.when(filesystemCephAdapterImpl.getFile(anyString(),
		// anyString())).thenReturn(inputStream);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setStatusCode("PACKET_UPLOADED_TO_FILESYSTEM");
		listAppender.start();
		list.add(registrationStatusDto);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("Something");
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(description.getMessage()).thenReturn("hello");
		Mockito.when(registrationStatusService.getByStatus(anyString())).thenReturn(list);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);
		Mockito.when(idRepoService.findUinFromIdrepo(any(), any())).thenReturn(65324321);

		Mockito.when(filesystemCephAdapterImpl.getFile(any(), any())).thenReturn(inputStream);
		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);

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

		JSONObject demographicIdentity = new JSONObject();
		PowerMockito.when(JsonUtil.getJSONObject(any(), any())).thenReturn(demographicIdentity);

		statusResponseDto = new StatusResponseDto();
		statusResponseDto.setStatus("VALID");
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(statusResponseDto);

		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		Mockito.when(utility.getUIn(any())).thenReturn(12345678l);
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("ACTIVE");

		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jsonObject);
		PowerMockito.when(JsonUtil.getJSONObject(jsonObject, "individualBiometrics")).thenReturn(jsonObject);
		Mockito.when(jsonObject.get("value")).thenReturn("applicantCBEF");

		List<SyncRegistrationEntity> synchRecordList = new ArrayList<>();
		synchRecordList.add(new SyncRegistrationEntity());
		Mockito.when(idRepoService.findUinFromIdrepo(any(), any())).thenReturn(1);

		Mockito.when(registrationRepositary.getSyncRecordsByRegIdAndRegType(any(), any())).thenReturn(synchRecordList);

		// String test = "{}";
		// byte[] data = "{}".getBytes();

		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);

		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(test.getBytes());

		// MessageDigest.isEqual(generatedHash, );

		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);

	}

	/**
	 * Test structural validation success.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testStructuralValidationSuccess() throws Exception {

		// PowerMockito.mockStatic(MessageDigest.class);
		// PowerMockito.when(MessageDigest.isEqual(any(),
		// any())).thenReturn(Boolean.TRUE);
		// PowerMockito.when(MessageDigest.class, "isEqual", any(),
		// any()).thenReturn(true);
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue("Test for successful Structural Validation", messageDto.getIsValid());
	}

	@Test
	public void testStructuralValidationForConfigValues() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		when(env.getProperty(VALIDATESCHEMA)).thenReturn("false");
		when(env.getProperty(VALIDATEFILE)).thenReturn("false");
		when(env.getProperty(VALIDATECHECKSUM)).thenReturn("false");
		when(env.getProperty(VALIDATEAPPLICANTDOCUMENT)).thenReturn("false");
		when(env.getProperty(VALIDATEMASTERDATA)).thenReturn("false");
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue("Test for successful Structural Validation", messageDto.getIsValid());

	}

	@Test
	public void testSchemaValidationFailure() throws IdObjectValidationFailedException, IdObjectIOException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(idObjectValidator.validateIdObject(any(), any())).thenReturn(false);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());
	}

	/**
	 * Test structural document validation failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testStructuralDocumentValidationFailure() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		packetMetaInfo = new PacketMetaInfo();
		Identity identity = new Identity();

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("resupdate");

		FieldValue applicantType = new FieldValue();
		applicantType.setLabel("applicantType");
		applicantType.setValue("Child");

		FieldValue isVerified = new FieldValue();
		isVerified.setLabel("isVerified");
		isVerified.setValue("Verified");

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified));

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

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);
		dto.setReg_type(RegistrationType.ACTIVATED);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());

	}

	/**
	 * Test structural validation success for adult.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testStructuralValidationSuccessForAdult() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		listAppender.start();

		list.add(statusDto);

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("resupdate");

		FieldValue applicantType = new FieldValue();
		applicantType.setLabel("applicantType");
		applicantType.setValue("Adult");

		FieldValue isVerified = new FieldValue();
		isVerified.setLabel("isVerified");
		isVerified.setValue("Verified");

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified));

		Document documentPob = new Document();
		documentPob.setDocumentCategory("PROOFOFDATEOFBIRTH");
		documentPob.setDocumentName("ProofOfBirth");

		Document document = new Document();
		document.setDocumentCategory("PROOFOFADDRESS");
		document.setDocumentName("ProofOfAddress");

		Document document2 = new Document();
		document2.setDocumentCategory("PROOFOFIDENTITY");
		document2.setDocumentName("ProofOfIdentity");

		List<Document> documents = new ArrayList<Document>();
		documents.add(documentPob);
		documents.add(document);
		documents.add(document2);
		Mockito.when(documentUtility.getDocumentList(any())).thenReturn(documents);
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
		// applicantDemographicValues.add(PacketFiles.DEMOGRAPHICINFO.name());
		applicantDemographicValues.add(PacketFiles.APPLICANTPHOTO.name());
		applicantDemographicValues.add("ProofOfBirth");
		applicantDemographicValues.add("ProofOfRelation");
		applicantDemographicValues.add("ProofOfAddress");
		applicantDemographicValues.add("ProofOfIdentity");
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
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());

	}

	/**
	 * Test check sum validation failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCheckSumValidationFailure() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		String test = "123456789";
		byte[] data = "{}".getBytes();

		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);

		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());

	}

	/**
	 * Test files validation failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testFilesValidationFailure() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);
		PowerMockito.mockStatic(JsonUtil.class);

		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.FALSE);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());
	}

	/**
	 * Test exceptions.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testExceptions() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		packetMetaInfo.getIdentity().setHashSequence(null);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "'");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);

		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void testBaseCheckedExceptions() throws IdObjectValidationFailedException, IdObjectIOException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(idObjectValidator.validateIdObject(any(), any()))
				.thenThrow(new IdObjectValidationFailedException("", ""));

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);

		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void testRegprocesCheckExceptions() throws IdObjectValidationFailedException, IdObjectIOException,
			IdRepoAppException, ApisResourceAccessException, IOException {
		// Mockito.when(idObjectValidator.validateIdObject(any(),any())).thenThrow(new
		// IdObjectValidationFailedException("", ""));
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(utility.retrieveIdrepoJsonStatus(any())).thenReturn("DEACTIVATED");

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);

		assertEquals(true, messageDto.getInternalError());

	}

	/**
	 * Test IO exceptions.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testIOExceptions() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenThrow(new IOException());

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);

		assertEquals(true, messageDto.getInternalError());

	}

	/**
	 * Test check sum validation failure with retry count.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCheckSumValidationFailureWithRetryCount() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		String test = "123456789";
		byte[] data = "{}".getBytes();
		Mockito.when(filesystemCephAdapterImpl.getFile(anyString(), anyString())).thenReturn(inputStream);

		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRetryCount(1);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(registrationStatusDto, "", "");
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());

	}

	/**
	 * Data access exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void dataAccessExceptionTest() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow(new DataAccessException("") {
				});

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void testPreRegIdsAreNull() {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());

	}

	@Test
	public void reverseDataSyncHttpClientErrorException() throws ApisResourceAccessException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Invalid request");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());

	}

	@Test
	public void reverseDataSyncServerErrorExceptionTest() throws ApisResourceAccessException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"MainResponseDTO(err=ExceptionJSONInfoDTO(errorCode=PRG_CORE_REQ_001, message=INVALID_REQUEST_ID), status=false, resTime=2019-01-30T09:42:24.441Z, response=null)");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());

	}

	@Test
	public void reverseDataSyncErrorTest() throws ApisResourceAccessException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		MainResponseDTO<ReverseDatasyncReponseDTO> mainResponseDTO = new MainResponseDTO<>();
		ExceptionJSONInfoDTO exceptionJsonInfoDto = new ExceptionJSONInfoDTO();
		exceptionJsonInfoDto.setErrorCode("PRG_CORE_REQ_001");
		exceptionJsonInfoDto.setMessage("INVALID_REQUEST_ID");
		mainResponseDTO.setErrors(Arrays.asList(exceptionJsonInfoDto));
		mainResponseDTO.setResponsetime("2019-01-31T05:57:02.816Z");
		mainResponseDTO.setResponse(null);
		List<String> preRegIds = new ArrayList<>();
		preRegIds.add("12345678");
		preRegIds.add("123456789");
		Mockito.when(restClientService.postApi(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
				Matchers.any())).thenReturn(mainResponseDTO);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void apiResourceExceptionTest() throws ApisResourceAccessException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException(
				"Packet Decryption failure");
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());

	}

	@Test
	public void fSAdapterExceptionTest() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(filesystemCephAdapterImpl.checkFileExistence(anyString(), anyString()))
				.thenThrow(new FSAdapterException("", ""));

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow(new TablenotAccessibleException("") {
				});

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void supervisorStatusRejectedTest() throws ApisResourceAccessException {
		regEntity.setSupervisorStatus("REJECTED");

		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);

		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());

	}

	@Test
	public void testBaseUnCheckedExceptions()
			throws IdObjectValidationFailedException, IdObjectIOException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, IOException {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		Mockito.when(utility.getUIn(any())).thenReturn(null);


		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);

		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void testParsingException() throws Exception {
		Mockito.when(syncRegistrationService.findByRegistrationId(anyString())).thenReturn(regEntity);
		packetMetaInfo = new PacketMetaInfo();
		Identity identity = new Identity();

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("resupdate");

		FieldValue applicantType = new FieldValue();
		applicantType.setLabel("applicantType");
		applicantType.setValue("Child");

		FieldValue isVerified = new FieldValue();
		isVerified.setLabel("isVerified");
		isVerified.setValue("Verified");

		identity.setMetaData(Arrays.asList(registrationType, applicantType, isVerified));

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

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenThrow(new ParsingException());
		dto.setReg_type(RegistrationType.ACTIVATED);
		MessageDTO messageDto = packetValidateProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());

	}
}