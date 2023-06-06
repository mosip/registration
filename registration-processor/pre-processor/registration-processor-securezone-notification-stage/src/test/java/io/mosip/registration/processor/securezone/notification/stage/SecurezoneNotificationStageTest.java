package io.mosip.registration.processor.securezone.notification.stage;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
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
public class SecurezoneNotificationStageTest {

    private static final int maxRetryCount = 5;

    private static final InputStream stream = Mockito.mock(InputStream.class);

    MessageDTO messageDTO= new MessageDTO();

    /** The registration status service. */
    @Mock
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Mock
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

    @Mock
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    @Mock
    private AuditLogRequestBuilder auditLogRequestBuilder;

    private RoutingContext ctx;
    private Boolean responseObject;

    @Mock
    private MosipRouter router;

    @InjectMocks
    SecurezoneNotificationStage notificationStage = new SecurezoneNotificationStage() {

        @Override
        public void setResponse(RoutingContext ctx, Object object) {
            responseObject = Boolean.TRUE;
        }

        @Override
        public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
        }

        @Override
        public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
            return null;
        }

        @Override
        public void createServer(Router route, int port)
        {

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
                obj.put("rid", "2018701130000410092018110735");
                obj.put("isValid", true);
                obj.put("internalError", false);
                obj.put("reg_type", "NEW");
                obj.put("source", "REGISTRATIONCLIENT");
                obj.put("iteration", 1);
                obj.put("workflowInstanceId", "78fc3d34-03f5-11ec-9a03-0242ac130003");
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

    InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
    InternalRegistrationStatusDto registrationStatusDto1 = new InternalRegistrationStatusDto();
    List<SyncRegistrationEntity> entities = new ArrayList<SyncRegistrationEntity>();
    SyncRegistrationEntity entity = new SyncRegistrationEntity();
    SyncRegistrationEntity entity1 = new SyncRegistrationEntity();

    @Before
    public void setup() {

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("2018701130000410092018110735");
        registrationStatusDto.setStatusCode("SECUREZONE_NOTIFICATION_SUCCESS");
        registrationStatusDto.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130004");
        registrationStatusDto.setCreateDateTime(LocalDateTime.now().minusDays(1));

        registrationStatusDto1 = new InternalRegistrationStatusDto();
        registrationStatusDto1.setRegistrationId("2018701130000410092018110735");
        registrationStatusDto1.setStatusCode("SECUREZONE_NOTIFICATION_SUCCESS");
        registrationStatusDto1.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130003");
        registrationStatusDto1.setCreateDateTime(LocalDateTime.now());

        ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<AuditResponseDto>();
        AuditResponseDto auditResponseDto = new AuditResponseDto();
        responseWrapper.setResponse(auditResponseDto);

        entity.setAdditionalInfoReqId("abc");
        entity.setPacketId("2018701130000410092018110735");
        entity.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130003");

        entity1.setAdditionalInfoReqId(null);
        entity1.setPacketId("2018701130000410092018110735");
        entity1.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130003");

        ctx = setContext();
        ReflectionTestUtils.setField(notificationStage, "workerPoolSize", 10);
        ReflectionTestUtils.setField(notificationStage, "clusterManagerUrl", "/dummyPath");
        ReflectionTestUtils.setField(notificationStage, "messageExpiryTimeLimit", Long.valueOf(0));
        ReflectionTestUtils.setField(notificationStage, "mainProcesses", Arrays.asList("NEW","UPDATE","LOST"));
        //ReflectionTestUtils.setField(notificationStage, "port", "7999");
        Mockito.when(router.post(Mockito.any())).thenReturn(null);
        Mockito.doNothing().when(router).setRoute(Mockito.any());
        Mockito.doNothing().when(router).nonSecureHandler(Mockito.any(),Mockito.any());
        messageDTO.setRid("2018701130000410092018110735");
        messageDTO.setIsValid(true);
        messageDTO.setInternalError(false);
        messageDTO.setReg_type("NEW");
        messageDTO.setSource("REGISTRATIONCLIENT");
        messageDTO.setIteration(1);
        messageDTO.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130003");

        Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(),any(),any());
        Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
        entities.add(entity);
        Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("Something");
        Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(entity);
        Mockito.when(syncRegistrationService.findByAdditionalInfoReqId(anyString())).thenReturn(entities);
        Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(registrationStatusDto);
    }

    @Test
    public void processURLTest() {

        notificationStage.processURL(ctx);

        assertTrue(responseObject);
    }

    @Test
    public void ridNotFoundTest() {
        Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(null);

        notificationStage.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void duplicateRidFoundTest() {

        SyncRegistrationEntity syncEntity = new SyncRegistrationEntity();
        syncEntity.setAdditionalInfoReqId(null);
        syncEntity.setRegistrationType("NEW");
        syncEntity.setPacketId("2018701130000410092018110735");
        syncEntity.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130004");
        entities.add(syncEntity);
        entities.add(entity1);

        Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(registrationStatusDto).thenReturn(registrationStatusDto).thenReturn(registrationStatusDto1);
        Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(syncEntity);
        Mockito.when(syncRegistrationService.findByRegistrationId(any())).thenReturn(entities);
        notificationStage.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void duplicateAdditionalReqIdFoundTest() {

        SyncRegistrationEntity syncEntity = new SyncRegistrationEntity();
        syncEntity.setAdditionalInfoReqId("abc");
        syncEntity.setPacketId("2018701130000410092018110735");
        syncEntity.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130004");
        entities.add(syncEntity);
        entities.add(entity);

        Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(registrationStatusDto).thenReturn(registrationStatusDto).thenReturn(registrationStatusDto1);
        Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(entity);
        Mockito.when(syncRegistrationService.findByAdditionalInfoReqId(anyString())).thenReturn(entities);
        notificationStage.processURL(ctx);
        assertTrue(responseObject);
    }

    @Test
    public void processFailureURLTest() {
        RoutingContext routingContext = Mockito.mock(RoutingContext.class);
        routingContext.setBody(null);
        notificationStage.processURL(routingContext);
        assertEquals(responseObject, null);
    }

    @Test
    public void processTest() {
        MessageDTO inputDto= new MessageDTO();
        inputDto.setInternalError(Boolean.FALSE);
        inputDto.setIsValid(Boolean.TRUE);
        inputDto.setRid("2018701130000410092018110735");
        inputDto.setWorkflowInstanceId("78fc3d34-03f5-11ec-9a03-0242ac130003");

        Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(entity);
        Mockito.when(syncRegistrationService.findByAdditionalInfoReqId(anyString())).thenReturn(entities);
        MessageDTO messageDTO = notificationStage.process(inputDto);
        assertTrue(messageDTO.getIsValid());
    }

    @Test
    public void dbExceptionTest() {
        Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenThrow(new TablenotAccessibleException("exception"));
        Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION))
                .thenReturn("REPROCESS");
        MessageDTO result = notificationStage.process(messageDTO);
        assertTrue(result.getInternalError());
    }


    @Test
    public void genericExceptionTest() {
        Mockito.when(syncRegistrationService
                .findByWorkflowInstanceId(anyString())).thenThrow(new NullPointerException("exception"));
        Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION))
                .thenReturn("REPROCESS");
        MessageDTO result = notificationStage.process(messageDTO);
        assertFalse(result.getIsValid());
    }
}
