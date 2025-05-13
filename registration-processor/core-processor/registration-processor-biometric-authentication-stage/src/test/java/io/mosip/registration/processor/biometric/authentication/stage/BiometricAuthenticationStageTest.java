package io.mosip.registration.processor.biometric.authentication.stage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.mosip.registration.processor.core.exception.*;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
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
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
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
import io.mosip.registration.processor.packet.storage.utils.BioSdkUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils2.class, Utilities.class })
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
	private BioSdkUtil bioUtil;

	@Mock
	InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private IdentityIteratorUtil identityIteratorUtil;

	/** The dto. */
	MessageDTO dto = new MessageDTO();

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

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

				@Override
				public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}

				@Override
				public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}

			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		}
		
		@Override
		public Integer getPort() {
			return 8080;
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
	private SyncRegistrationRepository<SyncRegistrationEntity, String> registrationRepositary;

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
		ReflectionTestUtils.setField(biometricAuthenticationStage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(biometricAuthenticationStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(biometricAuthenticationStage, "ageLimit", "5");


		when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");


		list = new ArrayList<InternalRegistrationStatusDto>();

		listAppender = new ListAppender<>();

		dto.setRid("2018701130000410092018110735");
		dto.setReg_type("UPDATE");

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
		when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());

		when(identityIteratorUtil.getFieldValue(any(), any())).thenReturn("UPDATE");


		Mockito.doNothing().when(description).setMessage(any());
		when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("");


		statusResponseDto = new StatusResponseDto();
		statusResponseDto.setStatus("VALID");
		when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(statusResponseDto);

		JSONObject jsonObject = Mockito.mock(JSONObject.class);
		when(utility.getUIn(anyString(), anyString(), any())).thenReturn("12345678");
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
		when(utility.getApplicantAge(anyString(),anyString(), any())).thenReturn(21);

		regentity.setRegistrationType("update");
		when(syncRegistrationservice.findByWorkflowInstanceId(any())).thenReturn(regentity);

		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(true);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

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

		when(packetManagerService.getBiometricsByMappingJsonKey(any(), any(), any(), any())).thenReturn(biometricRecord);


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

		when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}
	
	@Test
	public void biometricAuthenticationBiometricNullTest() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");

		String individualBiometrics="{\"format\" : \"cbeff\",\"version\" : 1.0,\"value\" : \"individualBiometrics_bio_CBEFF\"}";
		when(packetManagerService.getFieldByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(individualBiometrics);
		
		when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(null);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}
	
	@Test
	public void individualBiometricAuthenticationBiometricNullTest() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");

		when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(null);
		
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}
	
	@Test
	public void biometricAuthenticationIndividualBiometricsValueNullTest() throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");

		String individualBiometrics="{\"format\" : \"cbeff\",\"version\" : 1.0,\"value\" : null}";
		when(packetManagerService.getFieldByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(individualBiometrics);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}

	@Test
	public void IDAuthFailureTest() throws IOException, ApisResourceAccessException, ValidationFailedException ,InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException, CertificateException , ValidationFailedException , Exception{
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.AUTH_SYSTEM_EXCEPTION)).thenReturn("REPROCESS");

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getIsValid());
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void childPacketTest() throws ApisResourceAccessException, JsonProcessingException, io.mosip.kernel.core.exception.IOException, PacketManagerException, IOException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		when(utility.getApplicantAge(anyString(),anyString(), any())).thenReturn(2);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void inputStreamNullIndividualAuthTest() throws ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException, IOException {

		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("value", "testFile");

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertFalse(messageDto.getIsValid());
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testIOException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString(), any())).thenThrow(new IOException("IOException"));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION)).thenReturn("ERROR");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testApisResourceAccessException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(anyString(),anyString(), any()))
				.thenThrow(new ApisResourceAccessException("ApisResourceAccessException"));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void testException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(anyString(),anyString(), any()))
				.thenThrow(
						new ApisResourceAccessException(
								"test message"));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertTrue(messageDto.getIsValid());
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
		when(packetManagerService.getBiometricsByMappingJsonKey(any(), any(), any(),any())).thenReturn(biometricRecord);

		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertTrue(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}

	@Test
	public void testNewPacket() throws IOException,
			ApisResourceAccessException, InvalidKeySpecException, NoSuchAlgorithmException, BioTypeException, CertificateException , ValidationFailedException ,Exception {

		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAuthStatus(false);
		when(regentity.getRegistrationType()).thenReturn("new");
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);

		assertTrue(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}

	@Test
	public void deployVerticle() {

		biometricAuthenticationStage.deployVerticle();
	}

	@Test
	public void testAuthSystemException() throws ApisResourceAccessException, IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BioTypeException, JsonProcessingException, PacketManagerException, CertificateException ,ValidationFailedException,Exception {
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

		when(packetManagerService.getBiometricsByMappingJsonKey(any(),
				any(), any(),any())).thenReturn(biometricRecord);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-007");
		error.setErrorMessage("system error from ida");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.AUTH_SYSTEM_EXCEPTION)).thenReturn("REPROCESS");
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertFalse(messageDto.getInternalError());
		assertTrue(messageDto.getIsValid());
	}
	@Test
	public void testAuthFailed() throws ApisResourceAccessException, IOException, InvalidKeySpecException,
			NoSuchAlgorithmException, BioTypeException, CertificateException ,ValidationFailedException,Exception{
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("IDA-MLC-008");
		error.setErrorMessage("biometric didnt match");
	
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		errors.add(error);
		authResponseDTO.setErrors(errors);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		/*File idJson = new File(classLoader.getResource("ID2.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		*//*String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class),
						MappingJsonConstants.IDENTITY));*/
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
		assertFalse(messageDto.getInternalError());
	}

	@Test
	public void testJsonProcessingException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString(), any())).thenThrow(new JsonProcessingException("IOException"));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION)).thenReturn("ERROR");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertFalse(messageDto.getIsValid());
	}

	@Test
	public void testPacketManagerException() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString(), any())).thenThrow(new PacketManagerException("errorcode","IOException"));
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void testChildPacketWithLessThanOneYear() throws ApisResourceAccessException, JsonProcessingException, io.mosip.kernel.core.exception.IOException, PacketManagerException, IOException {
		when(regentity.getRegistrationType()).thenReturn("UPDATE");
		when(utility.getApplicantAge(anyString(),anyString(), any())).thenReturn(0);
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getIsValid());
	}

	@Test
	public void PacketManagerNonRecoverableExceptionTest() throws ApisResourceAccessException, IOException, PacketManagerException, io.mosip.kernel.core.exception.IOException, JsonProcessingException {

		when(utility.getApplicantAge(any(),anyString(), any())).thenThrow(new PacketManagerNonRecoverableException("errorcode","IOException"));
		Mockito.when(packetManagerService.getFieldByMappingJsonKey(any(),any(),any(),any())).thenThrow(new PacketManagerNonRecoverableException("errorcode","IOException"));
		MessageDTO messageDto = biometricAuthenticationStage.process(dto);
		assertTrue(messageDto.getInternalError());
		assertFalse(messageDto.getIsValid());
	}
}
