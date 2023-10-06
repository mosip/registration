package io.mosip.registration.processor.workflowmanager.service.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.sun.source.tree.ModuleTree;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.abstractverticle.*;

import io.mosip.registration.processor.core.exception.UserNotificationRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.AuditResponseDTO;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationResponse;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;
import io.mosip.registration.processor.workflowmanager.validator.UserNotificationRequestValidator;
import io.mosip.registration.processor.core.exception.UserNotificationException;
import io.mosip.registration.processor.workflowmanager.verticle.UserNotificationApi;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.*;
import org.junit.Assert;



import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class UserNotificationApiTest {

    @Mock
    private MosipRouter router;

    @Mock
    MosipEventBus mosipEventBus;

    @Mock
    AuditLogRequestBuilder auditLogRequestBuilder;

    @Mock
    UserNotificationRequestValidator userNotificationRequestValidator;

    @Mock
    WebSubUtil webSubUtil;


    @Mock
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;


    
    private UserNotificationResponse userNotificationResponse;

    private InternalRegistrationStatusDto registrationStatusDto;

    private RoutingContext ctx;

    private String testurl;

    private int userNotificationApiPort;

    @InjectMocks
    UserNotificationApi userNotificationApi = new UserNotificationApi() {

        @Override
        public void setResponse(RoutingContext ctx, Object object) {
            userNotificationResponse = (UserNotificationResponse) object;

        }

        @Override
        public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
        }

        @Override
        public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
            Vertx vertx = Vertx.vertx();
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

                @Override
                public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {

                }

                @Override
                public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {

                }

            };
        }

        @Override
        public void createServer(Router route, int port) {
            userNotificationApiPort = port;
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
            public io.vertx.ext.web.@Nullable Cookie getCookie(String s) {
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
            public String getBodyAsString(String encoding) {
                return null;
            }

            @Override
            public String getBodyAsString() {
                return null;
            }

//            @Override
//            public JsonArray getBodyAsJsonArray() {
//                return null;
//            }

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
            public @Nullable JsonArray getBodyAsJsonArray() {
                return null;
            }

            @Override
            public @Nullable Buffer getBody() {
                return null;
            }

//            @Override
//            public Buffer getBody() {
//                return null;
//            }

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
            public void setBody(io.vertx.core.buffer.Buffer buffer) {

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
            public int cookieCount() {
                return 0;
            }

            @Override
            public Set<io.vertx.ext.web.Cookie> cookies() {
                return null;
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
            public RoutingContext addCookie(io.vertx.ext.web.Cookie cookie) {
                return null;
            }

            @Override
            public io.vertx.ext.web.@Nullable Cookie removeCookie(String s, boolean b) {
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
        userNotificationApiPort = 0;
        userNotificationResponse = null;
        ReflectionTestUtils.setField(userNotificationApi, "workerPoolSize", 10);
        ReflectionTestUtils.setField(userNotificationApi, "clusterManagerUrl", "/dummyPath");
        ReflectionTestUtils.setField(userNotificationApi, "dateTimePattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ctx = setContext();
        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("10003100030001520190422074511");
        registrationStatusDto.setRegistrationType("NEW");

        Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(),any(),any(),any(),any(),any()))
                .thenReturn(null);
        Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(registrationStatusDto);

    }

    @Test
    public void testDeployVerticle() {
        userNotificationApi.deployVerticle();
        Assert.assertEquals(testurl, "/dummyPath");
    }

    @Test
    public void testStart() {
        ReflectionTestUtils.setField(userNotificationApi, "port", "8096");
        Mockito.doNothing().when(router).setRoute(any());
        userNotificationApi.start();
        assertEquals(userNotificationApiPort, 8096);

    }

    @Test
    public void testProcessURL() {
        registrationStatusDto.setStatusCode(PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getCode());
        userNotificationApi.processURL(ctx);
        Assert.assertEquals(userNotificationResponse.getResponse().getStatusMessage(),
                PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getMessage());
    }

    @Test
    public void testRidNotPresent() {
        Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any()))
                .thenReturn(null);
        userNotificationApi.processURL(ctx);
        assertEquals(userNotificationResponse.getErrors().get(0).getErrorCode(),
                PlatformErrorMessages.RPR_UNA_RID_NOT_FOUND.getCode());
    }

    @Test
    public void testRegTypNotMatching() {
        registrationStatusDto.setRegistrationType("update");
        userNotificationApi.processURL(ctx);
        assertEquals(userNotificationResponse.getErrors().get(0).getErrorCode(),
                PlatformErrorMessages.RPR_UNA_REGTYPE_NOT_MATCHING.getCode());
    }

    @Test
    public void testUserNotificationRequestValidationException() throws UserNotificationRequestValidationException {
        Mockito.doThrow(new UserNotificationRequestValidationException(
                        PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode(),
                        PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage())).when(userNotificationRequestValidator)
                .validate(any());
        registrationStatusDto.setStatusCode(PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getCode());
        userNotificationApi.processURL(ctx);
        assertEquals(userNotificationResponse.getErrors().get(0).getErrorCode(),
                PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode());
    }
}
