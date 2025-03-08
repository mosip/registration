package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.exception.NullPointerException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowActionRequestValidator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

@RunWith(SpringRunner.class)
public class WorkflowActionApiTest {
	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;

	private Boolean responseObject;

	@Mock
	private WorkflowActionRequestValidator validator;

	@Mock
	private WorkflowActionService workflowActionService;

	private RoutingContext ctx;

	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;
	
	List<InternalRegistrationStatusDto> internalRegistrationStatusDtos;

	@Mock
	WorkflowSearchApi workflowSearchApi;

	@Mock
	WorkflowInstanceApi workflowInstanceApi;

	@InjectMocks
	WorkflowActionApi workflowActionApi = new WorkflowActionApi() {

		@Override
		public void setResponse(RoutingContext ctx, Object object) {
			responseObject = Boolean.TRUE;
		}

		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}

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
		public void createServer(Router route, int port) {

		}

		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;
		}
	};

	private RoutingContext setContext() {
		return new RoutingContext() {

			@Override
			public Set<FileUpload> fileUploads() {
				return null;
			}

			@Override
			public Vertx vertx() {
				return null;
			}

			@Override
			public User user() {
				return null;
			}

			@Override
			public int statusCode() {
				return 0;
			}

			@Override
			public void setUser(User user) {

			}

			@Override
			public void setSession(Session session) {
			}

			@Override
			public void setBody(Buffer body) {
			}

			@Override
			public void setAcceptableContentType(String contentType) {
			}

			@Override
			public Session session() {
				return null;
			}

			@Override
			public HttpServerResponse response() {
				return null;
			}

			@Override
			public void reroute(HttpMethod method, String path) {
			}

			@Override
			public HttpServerRequest request() {
				return null;
			}

			@Override
			public boolean removeHeadersEndHandler(int handlerID) {
				return false;
			}

			@Override
			public Cookie removeCookie(String name, boolean invalidate) {
				return null;
			}

			@Override
			public boolean removeBodyEndHandler(int handlerID) {
				return false;
			}

			@Override
			public <T> T remove(String key) {
				return null;
			}

			@Override
			public MultiMap queryParams() {
				return null;
			}

			@Override
			public List<String> queryParam(String query) {
				return null;
			}

			@Override
			public RoutingContext put(String key, Object obj) {
				return null;
			}

			@Override
			public Map<String, String> pathParams() {
				return null;
			}

			@Override
			public String pathParam(String name) {
				return null;
			}

			@Override
			public ParsedHeaderValues parsedHeaders() {
				return null;
			}

			@Override
			public String normalisedPath() {
				return null;
			}

			@Override
			public void next() {
			}

			@Override
			public String mountPoint() {
				return null;
			}

			@Override
			public Cookie getCookie(String name) {
				return null;
			}

			@Override
			public String getBodyAsString(String encoding) {
				return null;
			}

			@Override
			public String getBodyAsString() {
				return null;
			}

			@Override
			public JsonArray getBodyAsJsonArray() {
				return null;
			}

			@Override
			public JsonObject getBodyAsJson() {
				JsonObject obj = new JsonObject();
				obj.put("id", "mosip.registration.processor.workflow.action");
				obj.put("version", "1.0");
				obj.put("requesttime", "2021-03-15T10:02:45.474Z");
				JsonObject requestObject = new JsonObject();
				requestObject.put("workflowAction", "RESUME_PROCESSING");
				List<String> workflowIds = new ArrayList<String>();
				workflowIds.add("2018701130000410092018110735");
				requestObject.put("workflowIds", workflowIds);
				obj.put("request", requestObject);
				return obj;
			}

			@Override
			public Buffer getBody() {
				return null;
			}

			@Override
			public String getAcceptableContentType() {
				return null;
			}

			@Override
			public <T> T get(String key) {
				return null;
			}

			@Override
			public Throwable failure() {
				return null;
			}

			@Override
			public boolean failed() {
				return false;
			}

			@Override
			public void fail(Throwable throwable) {
			}

			@Override
			public void fail(int statusCode) {
			}

			@Override
			public Map<String, Object> data() {
				return null;
			}

			@Override
			public Route currentRoute() {
				return null;
			}

			@Override
			public Set<Cookie> cookies() {
				return null;
			}

			@Override
			public int cookieCount() {
				return 0;
			}

			@Override
			public void clearUser() {
			}

			@Override
			public int addHeadersEndHandler(Handler<Void> handler) {
				return 0;
			}

			@Override
			public RoutingContext addCookie(Cookie cookie) {
				return null;
			}

			@Override
			public int addBodyEndHandler(Handler<Void> handler) {
				return 0;
			}

			@Override
			public List<Locale> acceptableLocales() {
				return null;
			}

			@Override
			public RoutingContext addCookie(io.vertx.core.http.Cookie arg0) {
				return null;
			}

			@Override
			public int addEndHandler(Handler<AsyncResult<Void>> arg0) {
				return 0;
			}

			@Override
			public Map<String, io.vertx.core.http.Cookie> cookieMap() {
				return null;
			}

			@Override
			public void fail(int arg0, Throwable arg1) {

			}

			@Override
			public boolean isSessionAccessed() {
				return false;
			}

			@Override
			public boolean removeEndHandler(int arg0) {
				return false;
			}


		};
	}

	@Before
	public void setup() throws WorkflowActionRequestValidationException {
		ReflectionTestUtils.setField(workflowActionApi, "workerPoolSize", 10);
		ReflectionTestUtils.setField(workflowActionApi, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(workflowActionApi, "dateTimePattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ctx = setContext();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10003100030001520190422074511");
		registrationStatusDto.setRegistrationType("NEW");
		registrationStatusDto.setRegistrationStageName("SecurezoneNotificationStage");
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.name());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED.name());
		
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getByIdsAndTimestamp(Mockito.any()))
				.thenReturn(internalRegistrationStatusDtos);
	}

	@Test
	public void testDeployVerticle() {

		ReflectionTestUtils.setField(workflowActionApi, "workerPoolSize", 10);
		ReflectionTestUtils.setField(workflowActionApi, "clusterManagerUrl", "/dummyPath");
		workflowActionApi.deployVerticle();
	}

	@Test
	public void testStart() {
		ReflectionTestUtils.setField(workflowActionApi, "port", "2333");
		Mockito.doNothing().when(router).setRoute(any());
		workflowActionApi.start();
	}

	@Test
	public void testProcessURL() {
		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testWorkflowActionRequestValidationException() throws WorkflowActionRequestValidationException {
		Mockito.doThrow(new WorkflowActionRequestValidationException(
				PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
				PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage())).when(validator).validate(any());
			
		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testWorkflowActionException() throws WorkflowActionRequestValidationException, WorkflowActionException {

		Mockito.doThrow(new WorkflowActionException(PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getCode(),
				PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getMessage())).when(workflowActionService)
				.processWorkflowAction(any(), any());

		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testException() throws WorkflowActionRequestValidationException {
		Mockito.doThrow(new NullPointerException("", "")).when(validator).validate(any());

		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testWorkflowIdNotPresent() {
		internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		Mockito.when(registrationStatusService.getByIdsAndTimestamp(Mockito.any()))
				.thenReturn(internalRegistrationStatusDtos);
		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}
	
	@Test
	public void testWorkflowIdVariousScenario() {
		InternalRegistrationStatusDto registrationStatusDto1 = new InternalRegistrationStatusDto();
		registrationStatusDto1.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto1.setRegistrationType("NEW");
		registrationStatusDto1.setRegistrationStageName("SecurezoneNotificationStage");
		registrationStatusDto1.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.name());
		internalRegistrationStatusDtos.add(registrationStatusDto1);
		registrationStatusDto1.setStatusCode(RegistrationStatusCode.PAUSED.name());
		Mockito.when(registrationStatusService.getByIdsAndTimestamp(Mockito.any()))
				.thenReturn(internalRegistrationStatusDtos);
		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testWorkflowIdNotPaused() {
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
		workflowActionApi.processURL(ctx);
		assertTrue(responseObject);
	}
}
