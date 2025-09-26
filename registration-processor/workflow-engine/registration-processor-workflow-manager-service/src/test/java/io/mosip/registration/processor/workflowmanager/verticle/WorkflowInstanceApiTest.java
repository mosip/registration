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
import io.mosip.registration.processor.core.exception.WorkflowInstanceException;
import io.mosip.registration.processor.core.exception.WorkflowInstanceRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowInstanceService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowActionRequestValidator;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowInstanceRequestValidator;
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
public class WorkflowInstanceApiTest {
    @Mock
    private MosipRouter router;
    @Mock
    MosipEventBus mosipEventBus;

    private Boolean responseObject;

    @Mock
    private WorkflowInstanceRequestValidator validator;

    @Mock
    private WorkflowInstanceService workflowInstanceService;

    private RoutingContext ctx;

    private InternalRegistrationStatusDto registrationStatusDto;

    @Mock
    AuditLogRequestBuilder auditLogRequestBuilder;


    @InjectMocks
    WorkflowInstanceApi workflowInstanceApi = new WorkflowInstanceApi() {
        @Override
        public void setResponse(RoutingContext ctx, Object object) {
            responseObject = Boolean.TRUE;
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
                obj.put("id", "mosip.registration.processor.workflow.create");
                obj.put("version", "1.0");
                obj.put("requesttime", "2021-03-15T10:02:45.474Z");
                JsonObject requestObject = new JsonObject();
                requestObject.put("registrationId", "10001104360003820230721101145");
                requestObject.put("process", "NEW");
                requestObject.put("source", "REGISTRATION_CLIENT");
                requestObject.put("additionalInfoReqId", "string");
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
    public void setup() throws Exception {
        ReflectionTestUtils.setField(workflowInstanceApi, "dateTimePattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(workflowInstanceApi, "version", "v1'");
        ReflectionTestUtils.setField(workflowInstanceApi, "id", "mosip.registration.processor.workflow.instance");
        ctx = setContext();
        registrationStatusDto = new InternalRegistrationStatusDto();

        Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        Mockito.when(workflowInstanceService.createWorkflowInstance(Mockito.any(), Mockito.anyString()))
                .thenReturn(registrationStatusDto);
    }

    @Test
    public void testProcessURL() {
        workflowInstanceApi.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void testWorkflowInstanceRequestValidationException() throws WorkflowInstanceRequestValidationException {
        Mockito.doThrow(new WorkflowInstanceRequestValidationException(
                PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getCode(),
                PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getMessage())).when(validator).validate(any());

        workflowInstanceApi.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void testWorkflowInstanceException() throws Exception {

        Mockito.doThrow(new WorkflowInstanceException(PlatformErrorMessages.RPR_WIS_UNKNOWN_EXCEPTION.getCode(),
                        PlatformErrorMessages.RPR_WIS_UNKNOWN_EXCEPTION.getMessage())).when(workflowInstanceService)
                .createWorkflowInstance(any(), any());

        workflowInstanceApi.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void testException() throws WorkflowInstanceRequestValidationException {
        Mockito.doThrow(new NullPointerException("", "")).when(validator).validate(any());

        workflowInstanceApi.processURL(ctx);
        assertTrue(responseObject);
    }

}