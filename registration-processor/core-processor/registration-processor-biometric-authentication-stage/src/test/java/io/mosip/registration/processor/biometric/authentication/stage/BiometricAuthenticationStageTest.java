package io.mosip.registration.processor.biometric.authentication.stage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

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
import org.powermock.api.mockito.PowerMockito;
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
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.masterdata.StatusResponseDto;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
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
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class, HMACUtils.class, Utilities.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class BiometricAuthenticationStageTest {

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	/** The filesystem ceph adapter impl. */
	@Mock
	private PacketManager filesystemManager;

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

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(biometricAuthenticationStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(biometricAuthenticationStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(biometricAuthenticationStage, "ageLimit", "5");

		list = new ArrayList<InternalRegistrationStatusDto>();

		listAppender = new ListAppender<>();

		dto.setRid("2018701130000410092018110735");
		dto.setReg_type(RegistrationType.valueOf("UPDATE"));

		MockitoAnnotations.initMocks(this);
		packetMetaInfo = new PacketMetaInfo();

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("update");

		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_405.toString(), EventName.UPDATE.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		String test = "1234567890";
		byte[] data = "1234567890".getBytes();
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setStatusCode("");
		listAppender.start();
		list.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getByStatus(anyString())).thenReturn(list);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		Mockito.when(filesystemManager.checkFileExistence(anyString(), anyString())).thenReturn(Boolean.TRUE);
		Mockito.when(identityIteratorUtil.getFieldValue(any(), any())).thenReturn("UPDATE");
		Mockito.when(filesystemManager.getFile(any(), any())).thenReturn(inputStream);
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("");
		PowerMockito.mockStatic(HMACUtils.class);
		PowerMockito.doNothing().when(HMACUtils.class, "update", data);
		PowerMockito.when(HMACUtils.class, "digestAsPlainText", anyString().getBytes()).thenReturn(test);

		statusResponseDto = new StatusResponseDto();
		statusResponseDto.setStatus("VALID");
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(statusResponseDto);

		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		Mockito.when(utility.getUIn(any())).thenReturn(12345678l);
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

		identity.setMetaData(metadata);
		packetMetaInfo.setIdentity(identity);
		Mockito.when(utility.getPacketMetaInfo(any())).thenReturn(packetMetaInfo);
		Mockito.when(utility.getApplicantAge(any())).thenReturn(21);
		HashMap<String, String> hashMap = new HashMap<String, String>();

		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(jSONObject);
	}

	@Test
	public void biometricAuthenticationSuccessTest()
			throws ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BiometricException,
			BioTypeException, IOException, ParserConfigurationException, SAXException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void biometricAuthenticationSuccessWithoutBiometricTest()
			throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void IDAuthFailureTest() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void childPacketTest() throws ApisResourceAccessException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException {

		Mockito.when(utility.getApplicantAge(any())).thenReturn(2);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullTest() throws PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, IOException {
		Mockito.when(filesystemManager.getFile(any(), any())).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullIndividualAuthTest()
			throws PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException {
		Mockito.when(filesystemManager.getFile(any(), any())).thenReturn(null);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testIOException() throws ApisResourceAccessException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException {

		Mockito.when(utility.getApplicantAge(any())).thenThrow(new IOException("IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testApisResourceAccessException() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {

		Mockito.when(utility.getApplicantAge(any()))
				.thenThrow(new ApisResourceAccessException("ApisResourceAccessException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testException() throws ApisResourceAccessException, IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException {

		Mockito.when(utility.getApplicantAge(any()))
				.thenThrow(new PacketDecryptionFailureException("testCode", "test message"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testEmptyJSONObject() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException {
		HashMap<String, String> hashMap = new HashMap<String, String>();

		hashMap.put("value", "");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(jSONObject);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void resupdatePacketTest() throws ApisResourceAccessException, InvalidKeySpecException,
			NoSuchAlgorithmException, BiometricException, BioTypeException, IOException, ParserConfigurationException,
			SAXException, PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {
		FieldValue fieldValue = new FieldValue();
		FieldValue fieldValue1 = new FieldValue();
		fieldValue1.setLabel("authenticationBiometricFileName");
		fieldValue1.setValue("biometricTestFileName");
		fieldValue.setLabel("registrationType");
		fieldValue.setValue("res_update");
		List<FieldValue> metadata = new ArrayList<>();
		metadata.add(fieldValue);
		metadata.add(fieldValue1);

		identity.setMetaData(metadata);
		packetMetaInfo.setIdentity(identity);
		Mockito.when(utility.getPacketMetaInfo(any())).thenReturn(packetMetaInfo);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void testNewPacket() throws IOException, PacketDecryptionFailureException, ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			BiometricException, BioTypeException, ParserConfigurationException, SAXException {
		FieldValue fieldValue = new FieldValue();
		FieldValue fieldValue1 = new FieldValue();
		fieldValue1.setLabel("authenticationBiometricFileName");
		fieldValue1.setValue("biometricTestFileName");
		fieldValue.setLabel("registrationType");
		fieldValue.setValue("new");
		List<FieldValue> metadata = new ArrayList<>();
		metadata.add(fieldValue);
		metadata.add(fieldValue1);

		identity.setMetaData(metadata);
		packetMetaInfo.setIdentity(identity);
		Mockito.when(utility.getPacketMetaInfo(any())).thenReturn(packetMetaInfo);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void deployVerticle() {

		biometricAuthenticationStage.deployVerticle();
	}
	@Test
	public void testAuthSystemException() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException, BiometricException, BioTypeException, ParserConfigurationException, SAXException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-007");
		error.setErrorMessage("system error from ida");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}
	@Test
	public void testAuthFailed() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, InvalidKeySpecException, NoSuchAlgorithmException, BiometricException, BioTypeException, ParserConfigurationException, SAXException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-008");
		error.setErrorMessage("biometric didnt match");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		Mockito.when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");
		JSONObject jSONObject = new JSONObject(hashMap);
		Mockito.when(utility.getDemographicIdentityJSONObject(any())).thenReturn(jSONObject);
		PowerMockito.when(JsonUtil.getJSONObject(jSONObject, "individualBiometrics")).thenReturn(null);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}
}
