package io.mosip.registration.processor.biometric.authentication.stage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.ErrorDTO;
import io.mosip.registration.processor.core.auth.dto.ResponseDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.masterdata.StatusResponseDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils.class, Utilities.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class BiometricAuthenticationStageTest {

	/** The input stream. */
	@Mock
	private InputStream inputStream;



	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The packet info manager. */
	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private AuthUtil authUtil;

	@Mock
	InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private IdentityIteratorUtil identityIteratorUtil;

	/** The dto. */
	MessageDTO dto = new MessageDTO();

	@Mock
	private PacketReaderService packetReaderService;

	@Mock
	private IdSchemaUtils idSchemaUtils;


	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationservice;

	/** The BiometricAuthenticationStage stage. */
	@InjectMocks
	private BiometricAuthenticationStage biometricAuthenticationStage = new BiometricAuthenticationStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus(vertx) {
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder = new AuditLogRequestBuilder();

	@Mock
	private Environment env;




	/** The dto. */
	InternalRegistrationStatusDto statusDto;
	/** The list. */
	List<InternalRegistrationStatusDto> list;

	/** The list appender. */
	private ListAppender<ILoggingEvent> listAppender;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Mock
	private Utilities utility;

	@Mock
	ObjectMapper mapIdentityJsonStringToObject;

	@Mock
	private RegistrationRepositary<SyncRegistrationEntity, String> registrationRepositary;

	StatusResponseDto statusResponseDto;

	@Mock
	LogDescription description;

	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	private ClassLoader classLoader;

	private SyncRegistrationEntity regentity = Mockito.mock(SyncRegistrationEntity.class);

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		classLoader = getClass().getClassLoader();
		ReflectionTestUtils.setField(biometricAuthenticationStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(biometricAuthenticationStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(biometricAuthenticationStage, "ageLimit", "5");


		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");


		list = new ArrayList<InternalRegistrationStatusDto>();

		listAppender = new ListAppender<>();

		dto.setRid("2018701130000410092018110735");
		dto.setReg_type(RegistrationType.valueOf("UPDATE"));

		MockitoAnnotations.initMocks(this);


		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_405.toString(), EventName.UPDATE.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);



		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setStatusCode("");
		listAppender.start();
		list.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getByStatus(anyString())).thenReturn(list);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());

		Mockito.when(identityIteratorUtil.getFieldValue(any(), any())).thenReturn("UPDATE");


		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("");


		statusResponseDto = new StatusResponseDto();
		statusResponseDto.setStatus("VALID");
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(statusResponseDto);

		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		Mockito.when(utility.getUIn(any())).thenReturn("12345678");
		Mockito.when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		FieldValue fieldValue = new FieldValue();
		FieldValue fieldValue1 = new FieldValue();
		fieldValue1.setLabel("authenticationBiometricFileName");
		fieldValue1.setValue("biometricTestFileName");
		fieldValue.setLabel("registrationType");
		fieldValue.setValue("update");
		List<FieldValue> metadata = new ArrayList<>();
		metadata.add(fieldValue);
		metadata.add(fieldValue1);


		Mockito.when(utility.getApplicantAge(any())).thenReturn(21);
		HashMap<String, String> hashMap = new HashMap<String, String>();

		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);

		File cbeffFile = new File(classLoader.getResource("cbeff.xml").getFile());
		InputStream cbeffInputstream = new FileInputStream(cbeffFile);
		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(cbeffInputstream);
		regentity.setRegistrationType("update");
		Mockito.when(syncRegistrationservice.findByRegistrationId(any())).thenReturn(regentity);


	}

	@Test
	public void biometricAuthenticationSuccessTest()
			throws ApisResourceAccessException, ApiNotAccessibleException, InvalidKeySpecException, NoSuchAlgorithmException, BiometricException,
			BioTypeException, IOException, ParserConfigurationException, SAXException,
			io.mosip.kernel.core.exception.IOException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}


	@Test
	public void IDAuthFailureTest() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);

		File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void childPacketTest() throws ApisResourceAccessException, ApiNotAccessibleException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(utility.getApplicantAge(any())).thenReturn(2);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullTest() throws PacketDecryptionFailureException, ApisResourceAccessException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, IOException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		HashMap<String, String> hashMap = new HashMap<String, String>();
		File mappingJsonFile = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream is = new FileInputStream(mappingJsonFile);
		String value = IOUtils.toString(is, "UTF-8");
		JSONObject mappingJsonObject = JsonUtil.objectMapperReadValue(value, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson()).thenReturn(mappingJsonObject);
		hashMap.put("value", "");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any(), any())).thenReturn(jSONObject);
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		File idJson = new File(classLoader.getResource("ID.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));

		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(null);
		Mockito.when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn("id");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullIndividualAuthTest()
			throws PacketDecryptionFailureException, ApisResourceAccessException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {

		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any(), any())).thenReturn(jSONObject);
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(null);
		Mockito.when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn("id");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testIOException() throws ApisResourceAccessException, ApiNotAccessibleException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {

		Mockito.when(utility.getApplicantAge(any())).thenThrow(new IOException("IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testApisResourceAccessException() throws ApisResourceAccessException, ApiNotAccessibleException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {

		Mockito.when(utility.getApplicantAge(any()))
				.thenThrow(new ApisResourceAccessException("ApisResourceAccessException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testException() throws ApisResourceAccessException, ApiNotAccessibleException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {

		Mockito.when(utility.getApplicantAge(any()))
				.thenThrow(
						new io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException(
								"test message"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testEmptyJSONObject() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException,
			ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		HashMap<String, String> hashMap = new HashMap<String, String>();
		File mappingJsonFile = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream is = new FileInputStream(mappingJsonFile);
		String value = IOUtils.toString(is, "UTF-8");
		JSONObject mappingJsonObject = JsonUtil.objectMapperReadValue(value, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson()).thenReturn(mappingJsonObject);
		hashMap.put("value", "");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any(), any())).thenReturn(jSONObject);
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		File idJson = new File(classLoader.getResource("ID.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));

		Mockito.when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn("id");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void resupdatePacketTest() throws ApisResourceAccessException, ApiNotAccessibleException, InvalidKeySpecException,
			NoSuchAlgorithmException, BiometricException, BioTypeException, IOException, ParserConfigurationException,
			SAXException, PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(regentity.getRegistrationType()).thenReturn("res_update");
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void testNewPacket() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {

		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(regentity.getRegistrationType()).thenReturn("new");
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void deployVerticle() {

		biometricAuthenticationStage.deployVerticle();
	}
	@Test
	public void testAuthSystemException() throws ApisResourceAccessException, ApiNotAccessibleException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-007");
		error.setErrorMessage("system error from ida");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}
	@Test
	public void testAuthFailed() throws ApisResourceAccessException, ApiNotAccessibleException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			RegistrationProcessorCheckedException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-008");
		error.setErrorMessage("biometric didnt match");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}
}
