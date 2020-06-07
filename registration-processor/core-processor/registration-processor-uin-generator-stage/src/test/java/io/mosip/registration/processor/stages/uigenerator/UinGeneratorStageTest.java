package io.mosip.registration.processor.stages.uigenerator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;

import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernateErrorCode;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.ApplicantDocument;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.IndividualDemographicDedupeEntity;
import io.mosip.registration.processor.packet.storage.entity.RegLostUinDetEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.stages.uingenerator.dto.VidResponseDto;
import io.mosip.registration.processor.stages.uingenerator.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.stages.uingenerator.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.stages.uingenerator.stage.UinGeneratorStage;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils.class, Utilities.class, Gson.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class UinGeneratorStageTest {

	@InjectMocks
	private UinGeneratorStage uinGeneratorStage = new UinGeneratorStage() {
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

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	@Mock
	private Object identity;

	/** The registration status service. */
	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

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
	private JsonUtil util;

	@Mock
	private IdRepoService idRepoService;

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
	private PacketReaderService packetReaderService;

	@Mock
	private IdSchemaUtils idSchemaUtils;

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(uinGeneratorStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(uinGeneratorStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(uinGeneratorStage, "defaultSource", "id");

		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString()))
				.thenReturn(new ByteArrayInputStream(new String("uingeneratorstage").getBytes()));
		Mockito.when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn("id");
		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("EXCEPTION");
		Mockito.doNothing().when(description).setCode(Mockito.anyString());
		Mockito.doNothing().when(description).setMessage(Mockito.anyString());
		Mockito.when(description.getCode()).thenReturn("CODE");
		Mockito.when(description.getMessage()).thenReturn("MESSAGE");
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

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID1.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(idJsonStream);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

		File identityMappingjson = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream identityMappingjsonStream = new FileInputStream(identityMappingjson);

		try {
			identityMappingjsonString = IOUtils.toString(identityMappingjsonStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.class, "getJson", CONFIG_SERVER_URL, "RegistrationProcessorIdentity.json")
				.thenReturn(identityMappingjsonString);
		Mockito.when(utility.getConfigServerFileStorageURL()).thenReturn(CONFIG_SERVER_URL);
		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");
		Mockito.when(utility.getGetRegProcessorIdentityJson()).thenReturn("RegistrationProcessorIdentity.json");

		Mockito.when(identityJson.get(anyString())).thenReturn(demographicIdentity);
		List<ApplicantDocument> applicantDocument = new ArrayList<>();
		ApplicantDocument appDocument = new ApplicantDocument();
		appDocument.setIsActive(true);
		appDocument.setDocName("POA");
		appDocument.setDocStore("ProofOfAddress".getBytes());
		applicantDocument.add(appDocument);
		Mockito.when(env.getProperty("registration.processor.id.repo.generate")).thenReturn("mosip.vid.create");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(env.getProperty("registration.processor.id.repo.vidVersion")).thenReturn("v1");
		Mockito.when(regLostUinDetRepository.getLostUinMatchedRegId(anyString()))
				.thenReturn("27847657360002520181210094052");
		demographicIdentity.put("UIN", Long.parseLong("9403107397"));

		Mockito.when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn("9403107397");
		File file = new File(classLoader.getResource("ID.json").getFile());
		inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream,"UTF-8");
		JSONObject mappingJsonObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson()).thenReturn(mappingJsonObject);

	}

	@Test
	public void testUinGenerationSuccessWithoutUIN() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		messageDTO.setReg_type(RegistrationType.NEW);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		VidResponseDto vidResponseDto = new VidResponseDto();
		vidResponseDto.setVID("123456");
		vidResponseDto.setVidStatus("ACTIVE");
		responseVid.setResponse(vidResponseDto);

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid).thenReturn(response);

		// Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),
		// any(), any(), any()));

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());

	}

	@Test
	public void testUinGenerationResponseNull() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);
		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);
		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);
		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		
		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		VidResponseDto vidResponseDto = new VidResponseDto();
		vidResponseDto.setVID("123456");
		vidResponseDto.setVidStatus("ACTIVE");
		vidResponseDto.setRestoredVid(null);
		vidResponseDto.setUIN(null);
		responseVid.setResponse(vidResponseDto);
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid);
		// Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),
		// any(), any(), any(Class.class)));
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertFalse(result.getInternalError());
	}
	@Test
	public void testUinGenerationF() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		List<ErrorDTO> errors = new ArrayList<>();
		ErrorDTO errorDTO = new ErrorDTO("tets", "error");
		errors.add(errorDTO);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.create");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		// List<ErrorDTO> errors = new ArrayList<>();
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		VidResponseDto vidResponseDto = new VidResponseDto();
		vidResponseDto.setVID("123456");
		vidResponseDto.setVidStatus("ACTIVE");
		responseVid.setResponse(vidResponseDto);

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid).thenReturn(response);

//		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),  any(), any(), any()));

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());

	}

	@Test
	public void testUinGenerationSuccessWithoutUINAndUinUnused() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		VidResponseDto vidResponseDto = new VidResponseDto();
		vidResponseDto.setVID("123456");
		vidResponseDto.setVidStatus("ACTIVE");
		vidResponseDto.setRestoredVid(null);
		vidResponseDto.setUIN(null);
		responseVid.setResponse(vidResponseDto);
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid);

		// Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),
		// any(), any(), any(Class.class)));

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());

	}
	
	@Test
	public void testUinReActivationifAlreadyActivatedSuccess() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());

	}
	
	@Test
	public void testUinReActivationResponseStatusAsActivated() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());

	}
	@Test
	public void testUinReActivationWithoutResponseDTO() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(null);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());

	}


	@Test
	public void testUinReActivationWithStatusAsAny() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ANY");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());

	}

	

	@Test
	public void testUinReActivationIfNotActivatedSuccess() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);

		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());

	}
	@Test
	public void testUinReActivationIfNotGotActivatedStaus() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("ANY");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.update");
		responseDTO1.setStatus("ANY");
		idResponseDTO1.setResponse(responseDTO1);
		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());

	}

	@Test
	public void testUinReActivationFailure() throws Exception {

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
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

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());

	}

	@Test
	public void testUinUpdationFaliure() throws Exception {

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.error");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		String Str = "{\"uin\":\"6517036426\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(Str);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
	}

	@Test
	public void testExceptionInProcessTest() throws Exception {
		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(exp);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void testApiResourceExceptionInSendIdRepoTest() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);
		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());
		String Str = "{\"uin\":\"6517036426\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(Str);
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(exp);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void testApiResourceExceptionInUpdateIdRepoTest() throws Exception {
		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());
		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID1.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(exp);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void deactivateTestSuccess() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void checkIsUinDeactivatedSuccess() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		responseDTO.setStatus("DEACTIVATED");
		IdResponseDTO responsedto = new IdResponseDTO();
		responsedto.setResponse(responseDTO);

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}
	@Test
	public void deactivateTestWithDeactivate() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		IdResponseDTO responsedto = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setStatus("DEACTIVATED");
		responsedto.setResponse(responseDTO1);
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());
	}
			
	@Test
	public void deactivateTestWithNullResponseDTO() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(null);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());
	}
	

	@Test
	public void deactivateTestForExistingUinTestSuccess() throws ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");

		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.update");
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO1);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}

	@Test
	public void deactivateTestFailure() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		ApisResourceAccessException exp = new ApisResourceAccessException(
				HibernateErrorCode.ERR_DATABASE.getErrorCode());

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		ErrorDTO errorDto = new ErrorDTO();
		errorDto.setErrorCode("KER-IDR-001");
		errorDto.setMessage("Record already Exists in DB");

		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDto);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(errors);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenThrow(exp);
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void apisResourceAccessExceptionTest() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		uinGeneratorStage.process(messageDTO);
	}
	
	
	@Test
	public void testHttpServerErrorException() throws Exception {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));

		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
		.thenThrow(apisResourceAccessException);
		
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());

	}
	@Test
	public void testHttpClientErrorException() throws Exception {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("ACTIVATED"));
		String idJsonData = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream = new ByteArrayInputStream(idJsonData.getBytes(StandardCharsets.UTF_8));
		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
		.thenThrow(apisResourceAccessException);
		

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertTrue(result.getIsValid());

	}

	@Test
	public void testUinGenerationHttpClientErrorException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(apisResourceAccessException);

		messageDTO.setReg_type(RegistrationType.NEW);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		 //Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),
		// any(), any(), any()));

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertFalse(result.getInternalError());

	}

	@Test
	public void testUinGenerationHttpServerErrorException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any())).thenThrow(apisResourceAccessException);

		messageDTO.setReg_type(RegistrationType.NEW);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		 //Mockito.when(registrationProcessorRestClientService.postApi(any(), any(),
		// any(), any(), any()));

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		//assertFalse(result.getInternalError());

	}


	@Test
	public void clientErrorExceptionTest() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpErrorErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpErrorErrorException);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void getApiExceptionTest() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void testFSAdapterException() throws ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		FSAdapterException fsAdapterException = new FSAdapterException("RPR-1001", "Unable to connect to HDFS");
		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenThrow(fsAdapterException);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
	}
	
	@Test
	public void testIOException() throws ApisResourceAccessException,
			io.mosip.kernel.core.exception.IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException, IOException {
		IOException exception = new IOException("File not found");
		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenThrow(exception);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getInternalError());
	}


	@Test
	public void testDeployVerticle() {
		uinGeneratorStage.deployVerticle();
	}

	@Test
	public void testApiResourceException() throws JsonParseException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException, ApisResourceAccessException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
	}
	@Test
	public void testJsonProcessingException() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));
		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":\"4215839851\"}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenThrow(new ApisResourceAccessException());

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}

	@Test
	public void testApisResourceAccessExceptionPostApi() throws ApisResourceAccessException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		ApisResourceAccessException exc = new ApisResourceAccessException();
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(exc);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = null;
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		uinGeneratorStage.process(messageDTO);
	}

	@Test
	public void testUindeactivate() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("DEACTIVATED"));

		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

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

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}

	@Test
	public void testLinkSuccessForLostUin() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(regLostUinDetEntity.getLostUinMatchedRegId(any())).thenReturn("27847657360002520181210094052");
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());

	}

	@Test
	public void updateTestSuccess() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.valueOf("UPDATE"));
		IdResponseDTO responsedto = new IdResponseDTO();
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile2 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		File idJsonFile3 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream3 = new FileInputStream(idJsonFile3);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.update");
		responseDTO.setStatus("DEACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-03-12T06:49:30.779Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream2).thenReturn(idJsonStream3);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());
	}

	@Test
	public void vidException() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		messageDTO.setReg_type(RegistrationType.NEW);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("INVALID UIN");
		errors.add(error);
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		responseVid.setResponse(null);

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid).thenReturn(response);
		MessageDTO result = uinGeneratorStage.process(messageDTO);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void vidJSONException() throws ApisResourceAccessException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, IOException, io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"uin\":\"6517036426\",\"status\":\"ASSIGNED\"}";
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		messageDTO.setReg_type(RegistrationType.NEW);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);

		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("INVALID UIN");
		errors.add(error);
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		responseVid.setResponse(null);

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenThrow(ApisResourceAccessException.class);
		MessageDTO result = uinGeneratorStage.process(messageDTO);

	}

	@Test
	public void testLinkSuccessForLostUinisNull() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";
		Mockito.when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn(null);
		Mockito.when(regLostUinDetEntity.getLostUinMatchedRegId(any())).thenReturn("27847657360002520181210094052");
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());

	}

	@Test
	public void testLinkSuccessForLostUinIdResponseIsNUll() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.LOST);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);
		Mockito.when(regLostUinDetEntity.getLostUinMatchedRegId(any())).thenReturn("27847657360002520181210094052");

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO);
		Mockito.when(
				registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any(Class.class)))
				.thenReturn(null);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}

	@Test
	public void testUpdateSuccess() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.UPDATE);

		IdResponseDTO responsedto = new IdResponseDTO();
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile2 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		File idJsonFile3 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream3 = new FileInputStream(idJsonFile3);


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

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream2).thenReturn(idJsonStream3);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		ResponseDTO responseDTO1 = new ResponseDTO();
		responseDTO1.setEntity("https://dev.mosip.io/idrepo/v1.0/identity/203560486746");
		idResponseDTO1.setErrors(null);
		idResponseDTO1.setId("mosip.id.create");
		responseDTO1.setStatus("ACTIVATED");
		idResponseDTO1.setResponse(responseDTO1);
		idResponseDTO1.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO1.setVersion("1.0");

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
	}

	
	@Test
	public void testUpdateWithoutIdResponseDto() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.UPDATE);

		IdResponseDTO responsedto = new IdResponseDTO();
		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile2 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		File idJsonFile3 = new File(classLoader1.getResource("ID2.json").getFile());
		InputStream idJsonStream3 = new FileInputStream(idJsonFile3);


		IdResponseDTO idResponseDTO = new IdResponseDTO();
		

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream2).thenReturn(idJsonStream3);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);

		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		

		Mockito.when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getInternalError());
	}
	@Test
	public void testUpdateunsuccess() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10031100110005020190313110030");
		messageDTO.setReg_type(RegistrationType.UPDATE);

		IdResponseDTO responsedto = new IdResponseDTO();
		String idJson = "{\"identity\":{\"IDSchemaVersion\":1.0,\"UIN\":4215839851}}";
		InputStream idJsonStream1 = new ByteArrayInputStream(idJson.getBytes(StandardCharsets.UTF_8));

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

		Mockito.when(packetReaderService.getFile("10031100110005020190313110030",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream1);
		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(responsedto);
		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertFalse(result.getIsValid());
	}

	@Test
	public void testUinAlreadyExists() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW);
		String str = "{\"id\":\"mosip.id.read\",\"version\":\"1.0\",\"responsetime\":\"2019-04-05\",\"metadata\":null,\"response\":{\"uin\":\"2812936908\"},\"errors\":[{\"errorCode\":null,\"errorMessage\":null}]}";
		String response = "{\"timestamp\":1553771083721,\"status\":404,\"errors\":[{\"errorCode\":\"KER-UIG-004\",\"errorMessage\":\"Given UIN is not in ISSUED status\"}]}";

		Mockito.when(registrationProcessorRestClientService.getApi(any(), any(), any(), any(), any())).thenReturn(str);
		Mockito.when(registrationProcessorRestClientService.putApi(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(response);

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream = new FileInputStream(idJsonFile);
		File idJsonFile2 = new File(classLoader.getResource("ID.json").getFile());
		InputStream idJsonStream2 = new FileInputStream(idJsonFile2);

		ClassLoader classLoader1 = getClass().getClassLoader();
		File idJsonFile1 = new File(classLoader1.getResource("packet_meta_info.json").getFile());
		InputStream idJsonStream1 = new FileInputStream(idJsonFile1);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052",
				PacketFiles.ID.name(), "id")).thenReturn(idJsonStream)
				.thenReturn(idJsonStream2);

		Mockito.when(packetReaderService.getFile("27847657360002520181210094052", PacketFiles.PACKET_META_INFO.name(), "id"))
				.thenReturn(idJsonStream1);

		IdResponseDTO idResponseDTO = new IdResponseDTO();
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.create");
		ErrorDTO errorDTO = new ErrorDTO("IDR-IDC-012", "Record already exists in DB");
		idResponseDTO.setErrors(Lists.newArrayList(errorDTO));
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");

		ResponseWrapper<VidResponseDto> responseVid = new ResponseWrapper<VidResponseDto>();
		List<ErrorDTO> errors = new ArrayList<>();
		responseVid.setErrors(errors);
		responseVid.setVersion("v1");
		responseVid.setMetadata(null);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), format);
		responseVid.setResponsetime(localdatetime);
		VidResponseDto vidResponseDto = new VidResponseDto();
		vidResponseDto.setVID("123456");
		vidResponseDto.setVidStatus("ACTIVE");
		vidResponseDto.setRestoredVid(null);
		vidResponseDto.setUIN(null);
		responseVid.setResponse(vidResponseDto);
		Mockito.when(registrationProcessorRestClientService.postApi(any(), any(), any(), any(), any(Class.class)))
				.thenReturn(idResponseDTO).thenReturn(responseVid);

		MessageDTO result = uinGeneratorStage.process(messageDTO);
		assertTrue(result.getIsValid());

	}

}