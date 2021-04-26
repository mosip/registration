package io.mosip.registration.processor.manual.verification.stage;


import static org.mockito.Matchers.any;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.signatureutil.model.SignatureResponse;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.util.ManualVerificationRequestValidator;
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

public class ManualVerificationStageTest{

	@Mock
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;
	@Mock
	private MosipQueue mosipQueue;
	@Mock
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;
	@Mock
	private MosipRouter router;
	public RoutingContext ctx;
	@Mock
	SignatureResponse signatureResponse;
	@Mock
	private ManualVerificationRequestValidator manualVerificationRequestValidator;
	@Mock
	private ManualVerificationService manualAdjudicationService;
	@Mock
	private Environment env;
	@Mock
	private MosipEventBus mockEventbus;
	private File file;
	private String id = "2018782130000113112018183001.zip";
	private String newId = "2018782130000113112018183000.zip";
	public FileUpload fileUpload;
	private String serviceID="";
	private byte[] packetInfo;

	@InjectMocks
	private ManualVerificationStage manualverificationstage=new ManualVerificationStage()
	{
		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}

		@Override
		public MosipEventBus getEventBus(Object verticleName, String clusterManagerUrl, int instanceNumber) {
			return mockEventbus;
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
		
		@Override
		public Integer getPort() {
			return 8080;
		}
	};
	@Before
	public void setUp() throws java.io.IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		ReflectionTestUtils.setField(manualverificationstage, "mosipConnectionFactory", mosipConnectionFactory);
		ReflectionTestUtils.setField(manualverificationstage, "mosipQueueManager", mosipQueueManager);
		ReflectionTestUtils.setField(manualverificationstage, "contextPath", "/registrationprocessor/v1/manualverification");
		ReflectionTestUtils.setField(manualverificationstage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(manualverificationstage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(manualverificationstage, "clusterManagerUrl", "/dummyPath");
		//Mockito.when(env.getProperty(SwaggerConstant.SERVER_SERVLET_PATH)).thenReturn("/registrationprocessor/v1/manualverification");
		Mockito.when(mosipConnectionFactory.createConnection(any(),any(),any(),any())).thenReturn(mosipQueue);
		Mockito.doReturn(new String("str").getBytes()).when(mosipQueueManager).consume(any(), any(), any());
		Mockito.doNothing().when(router).setRoute(any());
		Mockito.when(router.post(any())).thenReturn(null);
		Mockito.when(router.get(any())).thenReturn(null);
		Mockito.doNothing().when(manualVerificationRequestValidator).validate(any(),any());
		Mockito.when(signatureResponse.getData()).thenReturn("gdshgsahjhghgsad");
		packetInfo="packetInfo".getBytes();
		Mockito.when(manualAdjudicationService.getApplicantFile(any(),any(), any())).thenReturn(packetInfo);
		//ClassLoader classLoader = getClass().getClassLoader();
		file = new File("/src/test/resources/0000.zip");
		//FileUtils.copyFile(file, new File(file.getParentFile().getPath() + "/" + id));
		//file = new File(classLoader.getResource(id).getFile());
		fileUpload = setFileUpload();
		ctx=setContext();
	}
	@Test
	public void testDeployeVerticle()
	{
		manualverificationstage.deployStage();
	}
	@Test
	public void testStart()
	{
		MessageDTO dto=new MessageDTO();
		manualverificationstage.process(dto);
		manualverificationstage.sendMessage(dto);
		manualverificationstage.start();
	}
/*	@Test
	public void testAllProcess() throws PacketDecryptionFailureException, ApisResourceAccessException, IOException, java.io.IOException, JsonProcessingException, PacketManagerException {
		
		testProcessAssignment();
		testProcessDecision();
		
	}
	
	private void testProcessAssignment()
	{
		serviceID="assign";
		Mockito.when(env.getProperty(any())).thenReturn("mosip.manual.verification.assignment");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ManualVerificationDTO manualVerificationDTO= new ManualVerificationDTO();
		Mockito.when(manualAdjudicationService.assignApplicant(any(UserDto.class))).thenReturn(manualVerificationDTO);
		manualverificationstage.processAssignment(ctx);
	}
	private void testProcessDecision()
	{
		serviceID="decision";
		Mockito.when(env.getProperty(any())).thenReturn("mosip.manual.verification.decision");
		Mockito.when(env.getProperty("mosip.registration.processor.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ManualVerificationDecisionDto updatedManualVerificationDTO=new ManualVerificationDecisionDto();
		Mockito.when(manualAdjudicationService.updatePacketStatus(any(),any())).thenReturn(updatedManualVerificationDTO);
		manualverificationstage.processDecision(ctx);
	}*/
	
	private FileUpload setFileUpload() {
		return new FileUpload() {

			@Override
			public String uploadedFileName() {
				return file.getPath();
			}

			@Override
			public long size() {
				return file.length();
			}

			@Override
			public String name() {
				return file.getName();
			}

			@Override
			public String fileName() {
				return newId;
			}

			@Override
			public String contentType() {
				return null;
			}

			@Override
			public String contentTransferEncoding() {
				return null;
			}

			@Override
			public String charSet() {
				return null;
			}
		};
	}
	private RoutingContext setContext() {
		return new RoutingContext() {

			@Override
			public Set<FileUpload> fileUploads() {
				Set<FileUpload> fileUploads = new HashSet<FileUpload>();
				fileUploads.add(fileUpload);
				return fileUploads;
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
				JsonObject obj= new JsonObject();
				obj.put("id", "51130282650000320190117144316");
				obj.put("version", "1.0");
				obj.put("requesttime", "51130282650000320190117");
				JsonObject obj1= new JsonObject();

				 if(serviceID=="assign") {
					obj1.put("userId", "51130282650000320190117");

				}else if(serviceID=="decision") {
					
					obj1.put("matchedRefType", "mono");
					obj1.put("mvUsrId", "mono");
					obj1.put("regId", "27847657360002520181208123456");
					obj1.put("statusCode", "APPROVED");
					obj1.put("reasonCode", "mono");
				}

				obj.put("request", obj1);

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
			public void fail(int statusCode, Throwable throwable) {
			}

			@Override
			public RoutingContext addCookie(io.vertx.core.http.Cookie cookie) {
				return null;
			}

			@Override
			public Map<String, io.vertx.core.http.Cookie> cookieMap() {
				return null;
			}

			@Override
			public boolean isSessionAccessed() {
				return false;
			}

			@Override
			public int addEndHandler(Handler<AsyncResult<Void>> handler) {
				return 0;
			}

			@Override
			public boolean removeEndHandler(int handlerID) {
				return false;
			}
		};
	}

}

