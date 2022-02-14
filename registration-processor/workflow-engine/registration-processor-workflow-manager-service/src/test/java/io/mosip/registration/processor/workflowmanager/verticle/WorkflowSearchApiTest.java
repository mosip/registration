package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.workflowmanager.service.WorkflowSearchService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowSearchRequestValidator;
import io.vertx.codegen.annotations.Nullable;
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
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

@RunWith(SpringRunner.class)
public class WorkflowSearchApiTest {

	private Boolean responseObject;

	private RoutingContext ctx;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	WorkflowSearchRequestValidator workflowSearchRequestValidator;

	@Mock
	WorkflowSearchService workflowSearchService;

	@InjectMocks
	WorkflowSearchApi workflowSearchApi = new WorkflowSearchApi() {
		@Override
		public void setResponse(RoutingContext ctx, Object object) {
			responseObject = Boolean.TRUE;
		}
	};

	private RoutingContext setContext() {
		return new RoutingContext() {

			@Override
			public Vertx vertx() {
				return null;
			}

			@Override
			public User user() {
				return new User() {

					@Override
					public void setAuthProvider(AuthProvider authProvider) {
						// TODO Auto-generated method stub

					}

					@Override
					public JsonObject principal() {
						// TODO Auto-generated method stub
						JsonObject obj = new JsonObject();
						obj.put("username", "admin");
						return obj;
					}

					@Override
					public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public User clearCache() {
						// TODO Auto-generated method stub
						return null;
					}
				};
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
				JsonObject searchInfo = new JsonObject();
				JsonObject filterInfo = new JsonObject();
				JsonObject pagination = new JsonObject();
				JsonObject sort = new JsonObject();
				sort.put("sortField", "");
				sort.put("sortType", "");
				pagination.put("pageStart", 1);
				pagination.put("pageFetch", 5);
				filterInfo.put("columnName", "id");
				filterInfo.put("value", "45128164920495");
				searchInfo.put("filters", Arrays.asList(filterInfo));
				searchInfo.put("pagination", pagination);
				searchInfo.put("sort", sort);
				obj.put("request", searchInfo);
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

			@Override
			public HttpServerRequest request() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public HttpServerResponse response() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public RoutingContext put(String key, Object obj) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T remove(String key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public @Nullable Cookie removeCookie(String name, boolean invalidate) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<FileUpload> fileUploads() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public @Nullable Session session() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int statusCode() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean removeHeadersEndHandler(int handlerID) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean removeBodyEndHandler(int handlerID) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void setBody(Buffer body) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setSession(Session session) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setUser(User user) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setAcceptableContentType(@Nullable String contentType) {
				// TODO Auto-generated method stub

			}

			@Override
			public void reroute(HttpMethod method, String path) {
				// TODO Auto-generated method stub

			}

			@Override
			public MultiMap queryParams() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> queryParam(String name) {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	@Before
	public void setup() throws WorkflowActionRequestValidationException {
		ReflectionTestUtils.setField(workflowSearchApi, "id", "workflow.id");
		ReflectionTestUtils.setField(workflowSearchApi, "version", "1.0");
		ReflectionTestUtils.setField(workflowSearchApi, "contextPath", "/dummyPath");
		ReflectionTestUtils.setField(workflowSearchApi, "dateTimePattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ctx = setContext();
	}

	@Test
	public void testProcessURL() throws WorkFlowSearchException {
		WorkflowDetail workflowDetail = new WorkflowDetail();
		workflowDetail.setWorkflowId("45128164920495");
		workflowDetail.setWorkflowType("NEW");

		Page<WorkflowDetail> pageDtos = new PageImpl<WorkflowDetail>(Arrays.asList(workflowDetail));

		Mockito.doNothing().when(workflowSearchRequestValidator).validate(Mockito.any());
		Mockito.when(workflowSearchService.searchRegistrationDetails(any())).thenReturn(pageDtos);
		workflowSearchApi.processURL(ctx);
		assertTrue(responseObject);
	}

	@Test
	public void testProcessFailureURL() throws WorkFlowSearchException {

		WorkFlowSearchException exception = new WorkFlowSearchException("ERR-001", "exception occured");
		Mockito.doNothing().when(workflowSearchRequestValidator).validate(Mockito.any());
		Mockito.when(workflowSearchService.searchRegistrationDetails(any())).thenThrow(exception);
		workflowSearchApi.processURL(ctx);
		assertTrue(responseObject);
	}
	
	@Test
	public void testProcessUnkonwnFailureURL() throws WorkFlowSearchException {

		Mockito.doThrow(NullPointerException.class).when(workflowSearchRequestValidator).validate(Mockito.any());
		workflowSearchApi.processURL(ctx);
		assertTrue(responseObject);
	}
}