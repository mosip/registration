package io.mosip.registration.processor.message.sender.stage.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

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

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.exception.MessageSenderRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderResponse;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.message.sender.stage.MessageSenderApi;
import io.mosip.registration.processor.message.sender.validator.MessageSenderRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
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
public class MessageSenderApiTest {

	@Mock
	private MosipRouter router;

	@Mock
	MosipEventBus mosipEventBus;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	MessageSenderRequestValidator messageSenderRequestValidator;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	private MessageSenderResponse messageSenderResponse;

	private InternalRegistrationStatusDto registrationStatusDto;

	private RoutingContext ctx;

	private String testurl;

	private int messageSenderApiPort;

	@InjectMocks
	MessageSenderApi messageSenderApi = new MessageSenderApi() {

		@Override
		public void setResponse(RoutingContext ctx, Object object) {
			messageSenderResponse = (MessageSenderResponse) object;

		}

		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}

		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();
			testurl = url;
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
		public void createServer(Router route, int port) {
			messageSenderApiPort = port;
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
				obj.put("id", "mosip.registration.processor.message.sender");
				obj.put("version", "1.0");
				obj.put("requesttime", "2021-03-15T10:02:45.474Z");
				JsonObject requestObject = new JsonObject();
				requestObject.put("rid", "10003100030001520190422074511");
				requestObject.put("regType", "NEW");
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
	public void setup() {
		testurl = null;
		messageSenderApiPort = 0;
		messageSenderResponse = null;
		ReflectionTestUtils.setField(messageSenderApi, "workerPoolSize", 10);
		ReflectionTestUtils.setField(messageSenderApi, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(messageSenderApi, "dateTimePattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ctx = setContext();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10003100030001520190422074511");
		registrationStatusDto.setRegistrationType("NEW");

		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any(),
				any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any())).thenReturn(registrationStatusDto);

	}

	@Test
	public void testDeployVerticle() {
		messageSenderApi.deployVerticle();
		assertEquals(testurl, "/dummyPath");
	}

	@Test
	public void testStart() {
		ReflectionTestUtils.setField(messageSenderApi, "port", "8096");
		Mockito.doNothing().when(router).setRoute(any());
		messageSenderApi.start();
		assertEquals(messageSenderApiPort, 8096);

	}

	@Test
	public void testProcessURL() {
		messageSenderApi.processURL(ctx);
		assertEquals(messageSenderResponse.getResponse().getStatusMessage(),
				PlatformSuccessMessages.RPR_MESSAGE_SENDER_API_SUCCESS.getMessage());
	}

	@Test
	public void testRidNotPresent() {

		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any()))
				.thenReturn(null);
		messageSenderApi.processURL(ctx);
		assertEquals(messageSenderResponse.getErrors().get(0).getErrorCode(),
				PlatformErrorMessages.RPR_MAS_RID_NOT_FOUND.getCode());
	}

	@Test
	public void testRegTypNotMatching() {
		registrationStatusDto.setRegistrationType("update");
		messageSenderApi.processURL(ctx);
		assertEquals(messageSenderResponse.getErrors().get(0).getErrorCode(),
				PlatformErrorMessages.RPR_MAS_REGTYPE_NOT_MATCHING.getCode());
	}

	@Test
	public void testMessageSenderRequestValidationException() throws MessageSenderRequestValidationException {
		Mockito.doThrow(new MessageSenderRequestValidationException(
				PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode(),
				PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage())).when(messageSenderRequestValidator)
				.validate(any());

		messageSenderApi.processURL(ctx);
		assertEquals(messageSenderResponse.getErrors().get(0).getErrorCode(),
				PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode());
	}
}
