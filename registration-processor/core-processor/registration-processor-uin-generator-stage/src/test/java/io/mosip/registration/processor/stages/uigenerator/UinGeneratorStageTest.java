package io.mosip.registration.processor.stages.uigenerator;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.mosip.registration.processor.core.code.*;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernateErrorCode;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.ApplicantDocument;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.dto.ContainerInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.IndividualDemographicDedupeEntity;
import io.mosip.registration.processor.packet.storage.entity.RegLostUinDetEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.stages.uingenerator.stage.UinGeneratorStage;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils2.class, Utilities.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class UinGeneratorStageTest {

	@InjectMocks
	private UinGeneratorStage uinGeneratorStage = new UinGeneratorStage() {
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


				}

				@Override
				public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {


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

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	@Mock
	private Object identity;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	/** The registration status service. */
	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private IdrepoDraftService idrepoDraftService;

	/** The identity json. */
	@Mock
	private JSONObject identityJson;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

	@Mock
	private List<Documents> documents;

	@Mock
	private JSONObject demographicIdentity;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private BasePacketRepository<IndividualDemographicDedupeEntity, String> demographicDedupeRepository;

	@Mock
	private BasePacketRepository<RegLostUinDetEntity, String> regLostUinDetRepository;

	/** The registration status dto. */
	private InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	@Mock
	private Utilities utility;

	@Mock
	private IdRepoService idRepoService;

	@Mock
	private CbeffUtil cbeffutil;

	@Mock
	private BasePacketRepository<RegLostUinDetEntity, String> regLostUinDetEntity;

	/** The identitydemoinfo. */
	Identity identitydemoinfo = new Identity();

	/** The Constant CONFIG_SERVER_URL. */
	private static final String CONFIG_SERVER_URL = "url";

	private String identityMappingjsonString;

	@Mock
	private Environment env;

	@Mock
	private LogDescription description;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private IdSchemaUtil idSchemaUtil;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	JSONObject documentObj;
	JSONObject identityObj;

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(uinGeneratorStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(uinGeneratorStage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(uinGeneratorStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(uinGeneratorStage, "updateInfo", "phone");
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);
		LinkedHashMap hm = new ObjectMapper().readValue(idJsonStream1, LinkedHashMap.class);
		JSONObject jsonObject = new JSONObject(hm);
		identityMappingjsonString = jsonObject.toJSONString();
		documentObj = JsonUtil.getJSONObject(new ObjectMapper().readValue(identityMappingjsonString, JSONObject.class), MappingJsonConstants.DOCUMENT);
		identityObj = JsonUtil.getJSONObject(new ObjectMapper().readValue(identityMappingjsonString, JSONObject.class), MappingJsonConstants.IDENTITY);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(),
				anyString(), any(Class.class))).thenReturn(str);

		//when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("EXCEPTION");
		Mockito.doNothing().when(description).setCode(Mockito.anyString());
		Mockito.doNothing().when(description).setMessage(Mockito.anyString());
		when(description.getCode()).thenReturn("CODE");
		when(description.getMessage()).thenReturn("MESSAGE");
		MockitoAnnotations.initMocks(this);
		Field auditLog = AuditLogRequestBuilder.class.getDeclaredField("registrationProcessorRestService");
		auditLog.setAccessible(true);
		@SuppressWarnings("unchecked")
		RegistrationProcessorRestClientService<Object> mockObj = Mockito
				.mock(RegistrationProcessorRestClientService.class);
		auditLog.set(auditLogRequestBuilder, mockObj);
		AuditResponseDto auditResponseDto = new AuditResponseDto();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(auditResponseDto);
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_401.toString(), EventName.ADD.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		registrationStatusDto.setLatestTransactionStatusCode("SUCCESS");
		when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);

		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.class, "getJson", CONFIG_SERVER_URL, "RegistrationProcessorIdentity.json")
				.thenReturn(identityMappingjsonString);
		when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");

		when(identityJson.get(anyString())).thenReturn(demographicIdentity);
		List<ApplicantDocument> applicantDocument = new ArrayList<>();
		ApplicantDocument appDocument = new ApplicantDocument();
		appDocument.setIsActive(true);
		appDocument.setDocName("POA");
		appDocument.setDocStore("ProofOfAddress".getBytes());
		applicantDocument.add(appDocument);
		when(env.getProperty("registration.processor.id.repo.generate")).thenReturn("mosip.vid.create");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("registration.processor.id.repo.vidVersion")).thenReturn("v1");
		when(regLostUinDetRepository.getLostUinMatchedRegIdByWorkflowId(anyString()))
				.thenReturn("27847657360002520181210094052");
		demographicIdentity.put("UIN", Long.parseLong("9403107397"));

		when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn("9403107397");
		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn(null);
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

		Document document = new Document();
		document.setDocument("document".getBytes());

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");
		fieldMap.put("phone", "23456");
		fieldMap.put("dob", "11/11/2011");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");

		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(packetManagerService.getDocument(anyString(),anyString(),anyString(),any())).thenReturn(document);
		when(packetManagerService.getBiometrics(anyString(),anyString(),any(),any())).thenReturn(biometricRecord);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		when(utility.getMappingJsonValue(anyString(), any())).thenReturn("UIN");

		ContainerInfoDto containerInfoDto = new ContainerInfoDto();
		containerInfoDto.setSource("REGISTRATION_CLIENT");
		containerInfoDto.setProcess("NEW");

		when(packetManagerService.getBiometrics(anyString(),any(), any(), any())).thenReturn(biometricRecord);
		when(cbeffutil.createXML(any())).thenReturn("String".getBytes());

	}

	@Test
	public void testUinGenerationSuccessWithoutUIN() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testUinGenerationIDRepoDraftException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_EXCEPTION)).thenReturn("FAILED");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(IdrepoDraftException.class);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}
	
	@Test
	public void testUinGenerationIDRepoDraftAPiResourceException() throws Exception {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.UPDATE.name());

		when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinReActivationifAlreadyActivatedSuccess() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";

		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	
	@Test
	public void testUinReActivationResponseStatusAsActivated() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUinReActivationIDraftResponseActivated() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	
	@Test
	public void testUinReActivationWithoutResponseDTO() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(null);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";;

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUinReActivationWithResponseDTONull() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		
		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		idResponseDTO1.setResponse(null);

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO1);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}


	@Test
	public void testUinReActivationWithStatusAsAny() throws Exception {

		Map<String, String> fieldMaps = new HashMap<>();
		fieldMaps.put("UIN", "123456");
		fieldMaps.put("name", "mono");
		fieldMaps.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMaps);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ANY");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
		.thenReturn(idResponseDTO);
		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	@Ignore
	public void testUinReActivationIfNotActivatedSuccess() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	@Test
	public void testUinReActivationIfNotGotActivatedStaus() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ANY");
		idResponseDTO1.setResponse(responseDTO1);
		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUinReActivationFailure() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		List<ErrorDTO> errors = new ArrayList<>();
		ErrorDTO errorDTO = new ErrorDTO("tets", "error");
		errors.add(errorDTO);
		idResponseDTO1.setErrors(errors);
		idResponseDTO1.setId("mosip.id.update");
		idResponseDTO1.setResponse(null);
		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testExceptionInProcessTest() throws Exception {
		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(exp);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testApiResourceExceptionInSendIdRepoTest() throws Exception {

		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());
		String Str = "{\"uin\":\"6517036426\"}";
		
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(exp);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testApiResourceExceptionInUpdateIdRepoTest() throws Exception {
		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());

		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(exp);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void deactivateTestSuccess() throws ApisResourceAccessException, IOException, JSONException,
			JsonProcessingException, PacketManagerException, IdrepoDraftException, IdrepoDraftReprocessableException {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);

		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	
	@Test
	public void checkIsUinDeactivatedSuccess() throws ApisResourceAccessException, IOException, JSONException, JsonProcessingException, PacketManagerException {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		ResponseDTO responseDTO = new ResponseDTO();

		responseDTO.setStatus("DEACTIVATED");
		IdResponseDTO responsedto = new IdResponseDTO();
		responsedto.setResponse(responseDTO);

		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any(Class.class)))
				.thenReturn(responsedto);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	@Test
	public void deactivateTestWithDeactivate() throws ApisResourceAccessException, IOException, JSONException,
			JsonProcessingException, PacketManagerException, IdrepoDraftException, IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setStatus("DEACTIVATED");
		responsedto.setResponse(responseDTO1);
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}
			
	@Test
	public void deactivateTestWithNullResponseDTO()
			throws ApisResourceAccessException, PacketManagerException, IOException, JsonProcessingException,
			JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void deactivateTestForExistingUinTestSuccess()
			throws ApisResourceAccessException, PacketManagerException, IOException, JsonProcessingException,
			JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");

		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.update");
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO1);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void deactivateTestFailure() throws ApisResourceAccessException, PacketManagerException, IOException,
			JsonProcessingException, JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");

		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenThrow(exp);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void apisResourceAccessExceptionTest()
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}
	
	
	@Test
	public void testHttpServerErrorException() throws Exception {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());

	}
	@Test
	public void testHttpClientErrorException() throws Exception {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());
		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);
		
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());

	}

	@Test
	public void testUinGenerationHttpClientErrorException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		messageDTO.setReg_type(RegistrationType.NEW.name());
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());

	}

	@Test
	public void testUinGenerationHttpServerErrorException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}


	@Test
	public void clientErrorExceptionTest()
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpErrorErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		when(apisResourceAccessException.getCause()).thenReturn(httpErrorErrorException);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenThrow(apisResourceAccessException);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testIOException() {
		IOException exception = new IOException("File not found");

		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}


	@Test
	public void testDeployVerticle() {
		uinGeneratorStage.deployVerticle();
	}

	@Test
	@Ignore
	public void testApiResourceException()
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testApisResourceAccessExceptionPostApi()
			throws ApisResourceAccessException, IOException, IdrepoDraftException, IdrepoDraftReprocessableException {
		ApisResourceAccessException exc = new ApisResourceAccessException();
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");

		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(exc);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = null;
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testLinkSuccessForLostUin() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST.name());
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		
		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(regLostUinDetEntity.getLostUinMatchedRegIdByWorkflowId(any())).thenReturn("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testLinkSuccessForLostUinAndUpdateContactInfo() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");
		when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn("9403107397");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST.name());
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		when(packetManagerService.getField(any(), any(), any(),any())).thenReturn("989879234");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(packetManagerService.getFieldByMappingJsonKey(any(), any(), any(), any())).thenReturn("1.0");
		when(regLostUinDetEntity.getLostUinMatchedRegIdByWorkflowId(any())).thenReturn("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void updateTestSuccess() throws ApisResourceAccessException, IOException, JsonProcessingException,
			PacketManagerException, JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("UPDATE").name());
		IdResponseDTO responsedto = new IdResponseDTO();

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testLinkSuccessForLostUinisNull() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST.name());
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";
		when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn(null);
		when(regLostUinDetEntity.getLostUinMatchedRegIdByWorkflowId(any())).thenReturn("27847657360002520181210094052");
		
		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testLinkSuccessForLostUinIdResponseIsNUll() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST.name());
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		
		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		when(regLostUinDetEntity.getLostUinMatchedRegIdByWorkflowId(any())).thenReturn("27847657360002520181210094052");

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(null);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUpdateSuccess() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.UPDATE.name());

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);


		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");

		idResponseDTO.setErrors(null);
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);
		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}


	@Test
	public void testUpdateDraftFailed() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.UPDATE.name());

		IdResponseDTO responsedto = new IdResponseDTO();
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode("ERROR");
		errorDTO.setMessage("ERROR message");
		responsedto.setErrors(Lists.newArrayList(errorDTO));
		when(idrepoDraftService.idrepoUpdateDraft(any(), any(), any()))
				.thenReturn(responsedto);

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();

		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testUinAlreadyExists() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		
		when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		ErrorDTO errorDTO = new ErrorDTO("IDR-IDC-012", "Record already exists in DB");
		idResponseDTO.setErrors(Lists.newArrayList(errorDTO));
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}


	@Test
	public void testUinGenerationSuccessWithAllDocuments() throws Exception {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");
		fieldMap.put("phone", "23456");
		fieldMap.put("dob", "11/11/2011");
		fieldMap.put("proofOfIdentity", "{\"value\":\"POA_Rental contract\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
		fieldMap.put("proofOfRelationship", "{\"value\":\"POA_Rental contract\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
		fieldMap.put("proofOfDateOfBirth", "{\"value\":\"POA_Rental contract\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
		fieldMap.put("proofOfException", "{\"value\":\"POA_Rental contract\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
		fieldMap.put("proofOfAddress", "{\"value\":\"POA_Rental contract\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
		fieldMap.put("individualBiometrics", "{\"format\":\"cbeff\",\"version\":1,\"value\":\"applicant_bio_CBEFF\"}");

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(),
				anyString(), any(Class.class))).thenReturn(str);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testPacketFetchingException() throws Exception {
		when(packetManagerService.getFieldByMappingJsonKey(any(), any(),any(), any())).thenThrow(new PacketManagerException("", ""));
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION)).thenReturn("REPROCESS");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testJsonProcessingException() throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

		when(packetManagerService.getFieldByMappingJsonKey(any(), any(),any(), any())).thenThrow(new io.mosip.kernel.core.util.exception.JsonProcessingException(""));
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION)).thenReturn("ERROR");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testUinReActivationWithoutIDResponseDTO() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED").name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		idResponseDTO.setResponse(null);

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(null);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));



		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}



	@Test
	public void deactivateTestWithNullResponseDTOBeforeDeactivate() throws ApisResourceAccessException,
			PacketManagerException,
			IOException, JsonProcessingException, JSONException, IdrepoDraftException,
			IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);


		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void deactivateTesApiResourceClientException() throws ApisResourceAccessException,
			PacketManagerException,
			IOException, JsonProcessingException, JSONException, IdrepoDraftException,
			IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		ApisResourceAccessException ex=new ApisResourceAccessException("", new HttpClientErrorException(HttpStatus.BAD_REQUEST));
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(ex);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");


		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void deactivateTesApiResourceServerException() throws ApisResourceAccessException,
			PacketManagerException,
			IOException, JsonProcessingException, JSONException, IdrepoDraftException,
			IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		ApisResourceAccessException ex=new ApisResourceAccessException("", new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(ex);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");


		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void deactivateTesApiResourceException() throws ApisResourceAccessException,
			PacketManagerException,
			IOException, JsonProcessingException, JSONException, IdrepoDraftException,
			IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		ApisResourceAccessException ex=new ApisResourceAccessException("");
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(ex);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION)).thenReturn("REPROCESS");


		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void deactivateTestAlreadyDeactivated() throws ApisResourceAccessException,
			PacketManagerException,
			IOException, JsonProcessingException, JSONException, IdrepoDraftException,
			IdrepoDraftReprocessableException {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());
		IdResponseDTO responsedto = new IdResponseDTO();
		ResponseDTO dto=new ResponseDTO();
		dto.setStatus("DEACTIVATED");
		responsedto.setResponse(dto);
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);


		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUinAlreadyDeactivated() throws ApisResourceAccessException, PacketManagerException, IOException,
			JsonProcessingException, JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {
		Map<String, String> fieldsMap = new HashMap<>();
		fieldsMap.put("UIN", "123456");
		fieldsMap.put("name", "mono");
		fieldsMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("UIN");
		defaultFields.add("name");
		defaultFields.add("email");

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED").name());

		IdResponseDTO responsedto = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setStatus("DEACTIVATED");
		responsedto.setResponse(responseDTO1);
		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = null;
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record not found in DB");

		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);
		idResponseDTO.setErrors(errors);
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldsMap);
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		MessageDTO result = uinGeneratorStage.process(messageDTO);

		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testUinUpdationFaliure() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.error");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		String Str = "{\"uin\":\"6517036426\"}";

		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type("UPDATE");

		MessageDTO result = uinGeneratorStage.process(messageDTO);

		assertFalse(result.getInternalError());
		assertFalse(result.getIsValid());
	}
	
	@Test
	public void testUinUpdationIDRepoFaliure() throws Exception {

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("IDR-IDC-001");
		errorDto.setMessage("Record already Exists in DB");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.error");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type("UPDATE");

		MessageDTO result = uinGeneratorStage.process(messageDTO);

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinUpdationIOExceptionFaliure() throws Exception {
		
		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any()))
				.thenThrow(IOException.class);
		when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.PACKET_UIN_GENERATION_REPROCESS)).thenReturn("REPROCESS");
		
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type("UPDATE");

		MessageDTO result = uinGeneratorStage.process(messageDTO);

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinGenerationIDRepoDraftReprocessableException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION))
				.thenReturn("REPROCESS");
		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
				.thenThrow(IdrepoDraftReprocessableException.class);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinGenerationSuccessWithEmptyName() throws Exception {
		ReflectionTestUtils.setField(uinGeneratorStage,"trimWhitespaces",true);
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("firstName","[ {\n" +
				"  \"language\" : \"eng\",\n" +
				"  \"value\" : \" \"\n" +
				"} ]");
		fieldMap.put("email", "mono@mono.com");
		fieldMap.put("phone", "23456");
		fieldMap.put("dob", "11/11/2011");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		ArgumentCaptor<io.mosip.registration.processor.packet.manager.dto.IdRequestDto> argumentCaptor = ArgumentCaptor.forClass(IdRequestDto.class);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		verify(idrepoDraftService).idrepoUpdateDraft(any(), any(), argumentCaptor.capture());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonobject=objectMapper.writeValueAsString(argumentCaptor.getAllValues().get(0).getRequest().getIdentity());
		JsonNode jsonNode=objectMapper.readTree(jsonobject);

		assertEquals("",jsonNode.get("firstName").asText());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinGenerationSuccessWithSelectedHanhle() throws Exception {
		ReflectionTestUtils.setField(uinGeneratorStage,"trimWhitespaces",true);
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("selectedHandles","[\n" +
				"        \"nrcId\",\n" +
				"        \"email\",\n" +
				"        \"phoneNumber\"\n" +
				"      ]");
		fieldMap.put("email", "mono@mono.com");
		fieldMap.put("phoneNumber", "23456");
		fieldMap.put("dob", "11/11/2011");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		ArgumentCaptor<io.mosip.registration.processor.packet.manager.dto.IdRequestDto> argumentCaptor = ArgumentCaptor.forClass(IdRequestDto.class);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		verify(idrepoDraftService).idrepoUpdateDraft(any(), any(), argumentCaptor.capture());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonobject=objectMapper.writeValueAsString(argumentCaptor.getAllValues().get(0).getRequest().getIdentity());
		JsonNode jsonNode=objectMapper.readTree(jsonobject);

		assertEquals("nrcId",jsonNode.get("selectedHandles").get(0).asText());
		assertEquals("email",jsonNode.get("selectedHandles").get(1).asText());
		assertEquals("phoneNumber",jsonNode.get("selectedHandles").get(2).asText());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUinGenerationSuccessWithObjectDataType () throws Exception {
		ReflectionTestUtils.setField(uinGeneratorStage,"trimWhitespaces",true);
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("individualBiometrics","{\n" +
				"        \"format\": \"cbeff\",\n" +
				"        \"value\": \"individualBiometrics_bio_CBEFF\",\n" +
				"        \"version\": 1\n" +
				"      }");
		fieldMap.put("email", "mono@mono.com");
		fieldMap.put("phoneNumber", "23456");
		fieldMap.put("dob", "11/11/2011");
		when(packetManagerService.getFields(any(),any(),any(),any())).thenReturn(fieldMap);
		ArgumentCaptor<io.mosip.registration.processor.packet.manager.dto.IdRequestDto> argumentCaptor = ArgumentCaptor.forClass(IdRequestDto.class);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		verify(idrepoDraftService).idrepoUpdateDraft(any(), any(), argumentCaptor.capture());
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonobject=objectMapper.writeValueAsString(argumentCaptor.getAllValues().get(0).getRequest().getIdentity());
		JsonNode jsonNode=objectMapper.readTree(jsonobject);

		assertEquals("cbeff",jsonNode.get("individualBiometrics").get("format").asText());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void updateTestWithAdditionalProcess() throws ApisResourceAccessException, IOException, JsonProcessingException,
			PacketManagerException, JSONException, IdrepoDraftException, IdrepoDraftReprocessableException {
		Map<String ,String> externalInternalMap = new HashMap<>();
		externalInternalMap.put("CRVS_UPDATE", "UPDATE");
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type("CRVS_UPDATE");
		ReflectionTestUtils.setField(uinGeneratorStage, "additionalProcessCategoryMapping", externalInternalMap);
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("UIN", "123456");
		fieldMap.put("name", "mono");
		fieldMap.put("email", "mono@mono.com");

		List<String> defaultFields = new ArrayList<>();
		defaultFields.add("name");
		defaultFields.add("dob");
		defaultFields.add("gender");
		defaultFields.add("UIN");

		when(utility.getUIn(any(),any(),any(ProviderStageName.class))).thenReturn("123456");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);

		when(packetManagerService.getFieldByMappingJsonKey(anyString(),anyString(),any(),any())).thenReturn("0.1");
		when(packetManagerService.getFields(anyString(),anyList(),anyString(),any())).thenReturn(fieldMap);
		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);

		when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(defaultFields);
		IdResponseDTO responsedto = new IdResponseDTO();

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn(responsedto);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(documentObj);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void PacketManagerNonRecoverableExceptionTest() throws ApisResourceAccessException, IOException, JsonProcessingException,
			PacketManagerException {
		when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("FAILED");
		when(packetManagerService.getFields(any(), any(), any(), any())).thenThrow(new PacketManagerNonRecoverableException("errorCode","message"));
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type("CRVS_UPDATE");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}
}