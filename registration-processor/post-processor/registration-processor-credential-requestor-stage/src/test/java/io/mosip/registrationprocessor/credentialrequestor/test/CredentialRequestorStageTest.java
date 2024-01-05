package io.mosip.registrationprocessor.credentialrequestor.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartner;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartnersList;
import io.mosip.registration.processor.credentialrequestor.util.CredentialPartnerUtil;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.EventId;
import io.mosip.registration.processor.core.constant.EventName;
import io.mosip.registration.processor.core.constant.EventType;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.constant.VidType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.CredentialResponseDto;
import io.mosip.registration.processor.core.idrepo.dto.VidInfoDTO;
import io.mosip.registration.processor.core.idrepo.dto.VidsInfosDTO;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.PropertiesUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.credentialrequestor.stage.CredentialRequestorStage;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({})
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.*" })
@PropertySource("classpath:bootstrap.properties")
public class CredentialRequestorStageTest {

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	MosipRouter router;

	@Mock
	protected PropertiesUtil propertiesUtil;

	@Mock
	private CredentialPartnerUtil credentialPartnerUtil;

	@Mock
	private ObjectMapper objectMapper;

	private InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
	/** The rest template. */
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;



	/** The identity. */
	Identity identity = new Identity();

	@Mock
	private Environment env;

	private String response;

	@Mock
	private Utilities utitilites;

	@InjectMocks
	private CredentialRequestorStage stage = new CredentialRequestorStage() {
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
		public void consume(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
			long messageExpiryTimeLimit) {
		}


		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}

		@Override
		public void createServer(Router router, int port) {

		}

		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;
		}

		@Override
		public void setResponseWithDigitalSignature(RoutingContext ctx, Object object, String contentType) {

		}
	};

	@Before
	public void setup() throws Exception {
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.regproc.credentialrequestor.server.port")).thenReturn("8099");
		when(env.getProperty("mosip.registration.processor.issuer"))
				.thenReturn("mpartner-default-digitalcard:PDFCard:RPR_UIN_CARD_TEMPLATE;mpartner-default-print:euin:RPR_UIN_CARD_TEMPLATE");
		ReflectionTestUtils.setField(stage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(stage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(stage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(stage, "busOutHaltAddresses", Arrays.asList());
		ReflectionTestUtils.setField(stage, "defaultPartners", Arrays.asList("digitalcardPartner", "opencrvsPartner"));

		System.setProperty("server.port", "8099");

		//ReflectionTestUtils.setField(stage, "port", "8080");
		ReflectionTestUtils.setField(stage, "encrypt", false);
		registrationStatusDto.setRegistrationId("45451454520011220230523070548");
		registrationStatusDto.setRegistrationType("NEW");
		registrationStatusDto.setWorkflowInstanceId("33e1fb7f-8202-41dc-82a1-49e821364eb2");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(String.class), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(propertiesUtil.getIntegerProperty(any(), any())).thenReturn(8080);



		//Mockito.doNothing().when(registrationStatusDto).setStatusCode(any());
		//Mockito.doNothing().when(registrationStatusDto).setStatusComment(any());
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		//Mockito.doNothing().when(registrationStatusDto).setLatestTransactionTypeCode(any());
		//Mockito.doNothing().when(registrationStatusDto).setRegistrationStageName(any());
		//Mockito.doNothing().when(registrationStatusDto).setLatestTransactionStatusCode(any());
		Mockito.when(router.post(any())).thenReturn(null);
		Mockito.when(router.get(any())).thenReturn(null);


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
		Mockito.when(objectMapper.writeValueAsString(any())).thenReturn(response);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		List<VidInfoDTO> vidList = new ArrayList<>();
		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setVid("4796042638691271");
		vidInfoDTO.setVidType(VidType.PERPETUAL.name());
		vidList.add(vidInfoDTO);
		vidsInfosDTO.setResponse(vidList);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(vidsInfosDTO);
		Map<String, String> map1 = new HashMap<>();
		map1.put("UIN", "4238135072");
		JSONObject jsonObject = new JSONObject(map1);
		Mockito.when(utitilites.idrepoRetrieveIdentityByRid(any())).thenReturn(jsonObject);

		CredentialPartner partner1 = new CredentialPartner();
		partner1.setId("digitalcardPartner");
		partner1.setPartnerId("mpartner-default-digitalcard");
		partner1.setCredentialType("PDFCard");
		partner1.setTemplate("RPR_UIN_CARD_TEMPLATE");
		partner1.setAppIdBasedCredentialIdSuffix(".pdf");

		CredentialPartner partner2 = new CredentialPartner();
		partner2.setId("opencrvsPartner");
		partner2.setPartnerId("opencrvs-partner");
		partner2.setCredentialType("opencrvs");
		partner2.setTemplate("RPR_UIN_CARD_TEMPLATE");

		CredentialPartnersList partnersList = new CredentialPartnersList();
		partnersList.setPartners(Lists.newArrayList(partner1, partner2));
		when(credentialPartnerUtil.getAllCredentialPartners()).thenReturn(partnersList);



	}

	@Test
	public void testAll() throws Exception {
		testDeployVerticle();
        testStart();
        testPrintStageSuccess();
	}

	public void testStart() {
		stage.start();
	}

	public void testDeployVerticle() throws Exception {


		stage.deployVerticle();
	}




	@Test
	public void testPrintStageSuccess()
			throws ApisResourceAccessException, JsonParseException, JsonMappingException, IOException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<CredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		CredentialResponseDto credentialResponseDto = new CredentialResponseDto();
		credentialResponseDto.setRequestId("879664323421");
		Mockito.when(objectMapper.readValue(response, CredentialResponseDto.class))
				.thenReturn(credentialResponseDto);
		responseWrapper.setResponse(credentialResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenReturn(responseWrapper);
		Mockito.when(restClientService.postApi((ApiName) any(), any(MediaType.class),any(),any(), any(), any(), any()))
				.thenReturn(responseWrapper);
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
		assertFalse(result.getInternalError());
	}
	

	@Test
	public void testPrintStageFailure() throws ApisResourceAccessException {

		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<?> responseWrapper = new ResponseWrapper<>();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("IDR-CRG-004");
		error.setMessage("unknown exception");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(error);
		responseWrapper.setErrors(errors);

		Mockito.when(restClientService.postApi(any(ApiName.class), any(), any(), any(), any(), any(MediaType.class)))
				.thenReturn(responseWrapper);

		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
	}


	@Test
	public void testException()
			throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<CredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		CredentialResponseDto credentialResponseDto = new CredentialResponseDto();
		credentialResponseDto.setRequestId("879664323421");
		Mockito.when(objectMapper.readValue(response, CredentialResponseDto.class)).thenReturn(credentialResponseDto);
		responseWrapper.setResponse(credentialResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenReturn(null);
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testApisResourceAccessException()
			throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<CredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		CredentialResponseDto credentialResponseDto = new CredentialResponseDto();
		credentialResponseDto.setRequestId("879664323421");
		Mockito.when(objectMapper.readValue(response, CredentialResponseDto.class)).thenReturn(credentialResponseDto);
		responseWrapper.setResponse(credentialResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenThrow(new ApisResourceAccessException());
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testUINNotavailable()
			throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<CredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		CredentialResponseDto credentialResponseDto = new CredentialResponseDto();
		credentialResponseDto.setRequestId("879664323421");
		Mockito.when(objectMapper.readValue(response, CredentialResponseDto.class)).thenReturn(credentialResponseDto);
		responseWrapper.setResponse(credentialResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenThrow(new ApisResourceAccessException());
		Map<String, String> map1 = new HashMap<>();

		JSONObject jsonObject = new JSONObject(map1);
		Mockito.when(utitilites.idrepoRetrieveIdentityByRid(any())).thenReturn(jsonObject);
		MessageDTO result = stage.process(dto);
		
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testVidNotAvailableException()
			throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		ResponseWrapper<CredentialResponseDto> responseWrapper = new ResponseWrapper<>();
		CredentialResponseDto credentialResponseDto = new CredentialResponseDto();
		credentialResponseDto.setRequestId("879664323421");
		Mockito.when(objectMapper.readValue(response, CredentialResponseDto.class)).thenReturn(credentialResponseDto);
		responseWrapper.setResponse(credentialResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(), any(MediaType.class)))
				.thenThrow(new ApisResourceAccessException());
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		List<VidInfoDTO> vidList = new ArrayList<>();
		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setVid("4796042638691271");
		vidInfoDTO.setVidType(VidType.TEMPORARY.name());
		vidList.add(vidInfoDTO);
		vidsInfosDTO.setResponse(vidList);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(vidsInfosDTO);
		MessageDTO result = stage.process(dto);
		
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testVidNotAvailableGETAPIResponseNullException()
			throws ApisResourceAccessException, JsonParseException, JsonMappingException, IOException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(vidsInfosDTO);

		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testVidNotAvailableGETAPIException()
			throws ApisResourceAccessException, JsonParseException, JsonMappingException, IOException {
		MessageDTO dto = new MessageDTO();
		dto.setRid("1234567890987654321");

		dto.setReg_type(RegistrationType.NEW.name());

		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(null);
		ServiceError error = new ServiceError();
		error.setErrorCode("ERR-001");
		error.setMessage("exception occured");
		vidsInfosDTO.setErrors(Arrays.asList(error));
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(vidsInfosDTO);

		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

}