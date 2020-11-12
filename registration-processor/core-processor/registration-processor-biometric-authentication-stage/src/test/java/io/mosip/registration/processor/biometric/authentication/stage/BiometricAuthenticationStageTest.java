package io.mosip.registration.processor.biometric.authentication.stage;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
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
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.masterdata.StatusResponseDto;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils.class, Utilities.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
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
	private PacketManagerService packetManagerService;


	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationservice;

	/** The BiometricAuthenticationStage stage. */
	@InjectMocks
	private BiometricAuthenticationStage biometricAuthenticationStage = new BiometricAuthenticationStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus() {

				@Override
				public Vertx getEventbus() {
					return vertx;
				}

				@Override
				public void consume(MessageBusAddress fromAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void send(MessageBusAddress toAddress, MessageDTO message) {

				}
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

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

	private SyncRegistrationEntity regentity = Mockito.mock(SyncRegistrationEntity.class);

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


		when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");


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
		registrationStatusDto.setRegistrationType("UPDATE");
		listAppender.start();
		list.add(registrationStatusDto);
		when(registrationStatusService.getByStatus(anyString())).thenReturn(list);
		when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());

		when(identityIteratorUtil.getFieldValue(any(), any())).thenReturn("UPDATE");


		Mockito.doNothing().when(description).setMessage(any());
		when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("");


		statusResponseDto = new StatusResponseDto();
		statusResponseDto.setStatus("VALID");
		when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(statusResponseDto);

		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		when(utility.getUIn(anyString(), anyString())).thenReturn("12345678");
		when(utility.retrieveIdrepoJson(any())).thenReturn(jsonObject);
		FieldValue fieldValue = new FieldValue();
		FieldValue fieldValue1 = new FieldValue();
		fieldValue1.setLabel("authenticationBiometricFileName");
		fieldValue1.setValue("biometricTestFileName");
		fieldValue.setLabel("registrationType");
		fieldValue.setValue("update");
		List<FieldValue> metadata = new ArrayList<>();
		metadata.add(fieldValue);
		metadata.add(fieldValue1);


		when(utility.getDefaultSource(any(), any())).thenReturn("reg-client");
		when(utility.getApplicantAge(anyString(),anyString())).thenReturn(21);

		regentity.setRegistrationType("update");
		when(syncRegistrationservice.findByRegistrationId(any())).thenReturn(regentity);

		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);

		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
		RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		QualityType quality = new QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.FINGER;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);

		when(packetManagerService.getBiometrics(any(),
				any(), any(),any())).thenReturn(biometricRecord);


	}

	@Test
	public void biometricAuthenticationSuccessTest() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
		RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		QualityType quality = new QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.FINGER;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);

		when(packetManagerService.getBiometrics(any(),
				any(), any(),any())).thenReturn(biometricRecord);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}


	@Test
	public void IDAuthFailureTest() throws IOException, ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void childPacketTest() throws ApisResourceAccessException, JsonProcessingException, io.mosip.kernel.core.exception.IOException, PacketManagerException, IOException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		when(utility.getApplicantAge(anyString(),anyString())).thenReturn(2);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullIndividualAuthTest() throws ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException, IOException {

		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testIOException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString())).thenThrow(new IOException("IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testApisResourceAccessException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(anyString(),anyString()))
				.thenThrow(new ApisResourceAccessException("ApisResourceAccessException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(anyString(),anyString()))
				.thenThrow(
						new ApisResourceAccessException(
								"test message"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void resupdatePacketTest() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(regentity.getRegistrationType()).thenReturn("res_update");
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
		RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		QualityType quality = new QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.FINGER;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);
		when(packetManagerService.getBiometrics(any(), any(), any(),any())).thenReturn(biometricRecord);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void testNewPacket() throws IOException,
			ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException {

		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		when(regentity.getRegistrationType()).thenReturn("new");
		when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void deployVerticle() {

		biometricAuthenticationStage.deployVerticle();
	}
	@Test
	public void testAuthSystemException() throws ApisResourceAccessException, IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BioTypeException, JsonProcessingException, PacketManagerException {
		List<BIR> birTypeList = new ArrayList<>();
		BIR birType1 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
		RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("Mosip");
		registryIDType.setType("257");
		QualityType quality = new QualityType();
		quality.setAlgorithm(registryIDType);
		quality.setScore(90l);
		bdbInfoType1.setQuality(quality);
		BiometricType singleType1 = BiometricType.FINGER;
		List<BiometricType> singleTypeList1 = new ArrayList<>();
		singleTypeList1.add(singleType1);
		List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
		bdbInfoType1.setSubtype(subtype1);
		bdbInfoType1.setType(singleTypeList1);
		birType1.setBdbInfo(bdbInfoType1);
		birTypeList.add(birType1);

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);

		when(packetManagerService.getBiometrics(any(),
				any(), any(),any())).thenReturn(biometricRecord);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-007");
		error.setErrorMessage("system error from ida");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}
	@Test
	public void testAuthFailed() throws ApisResourceAccessException, IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BioTypeException {
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-008");
		error.setErrorMessage("biometric didnt match");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		when(authUtil.authByIdAuthentication(any(), any(), any())).thenReturn(authResponseDTO);
		/*File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		*//*String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));*/
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testJsonProcessingException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString())).thenThrow(new JsonProcessingException("IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testPacketManagerException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString())).thenThrow(new PacketManagerException("errorcode","IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
	}
}
