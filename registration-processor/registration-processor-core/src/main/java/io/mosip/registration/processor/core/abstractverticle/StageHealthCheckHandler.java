package io.mosip.registration.processor.core.abstractverticle;

import static io.vertx.ext.healthchecks.impl.StatusHelper.isUp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jms.*;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.queue.factory.MosipActiveMq;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.queue.impl.TransportExceptionListener;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.support.JdbcUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.constant.HealthConstant;
import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.healthchecks.impl.HealthChecksImpl;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Mukul Puspam
 *
 */
public class StageHealthCheckHandler implements HealthCheckHandler {
	private HealthChecks healthChecks;

	MosipQueue mosipQueue = null;
	boolean isConsumerStarted = false;
	private final AuthProvider authProvider;
	private ObjectMapper objectMapper;
	private String driver;
	private String url;
	private String username;
	private String password;
	private String virusScannerHost;
	private String nameNodeUrl;
	private String kdcDomain;
	private String keytabPath;
	private String queueUsername;
	private String queuePassword;
	private String queueBrokerUrl;
	private Boolean isAuthEnable;
	private int virusScannerPort;
	private File currentWorkingDirPath;
	private VirusScanner<Boolean, InputStream> virusScanner;
	private FileSystem configuredFileSystem;
	private Path hadoopLibPath;
	private static final String HADOOP_HOME = "hadoop-lib";
	private static final String WIN_UTIL = "winutils.exe";
	private static final String CLASSPATH_PREFIX = "classpath:";
	private static final int THRESHOLD = 10485760;
	javax.jms.Connection connection = null;
	private Session session = null;
	MessageConsumer messageConsumer;
	MessageProducer messageProducer;

	private static final String DEFAULT_QUERY = "SELECT 1";

	private StageHealthCheckHandler.JSONResultBuilder resultBuilder;
	/**
	 * The field for Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(StageHealthCheckHandler.class);

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	/**
	 * @param vertx
	 * @param provider
	 * @param objectMapper
	 * @param environment
	 */
	public StageHealthCheckHandler(Vertx vertx, AuthProvider provider, ObjectMapper objectMapper, VirusScanner virusScanner,
			Environment environment) {
		this.healthChecks = new HealthChecksImpl(vertx);
		this.authProvider = provider;
		this.objectMapper = objectMapper;
		this.driver = environment.getProperty(HealthConstant.DRIVER);
		this.url = environment.getProperty(HealthConstant.URL);
		this.username = environment.getProperty(HealthConstant.USER);
		this.password = environment.getProperty(HealthConstant.PASSWORD);
		this.queueUsername = environment.getProperty(HealthConstant.QUEUE_USERNAME);
		this.queuePassword = environment.getProperty(HealthConstant.QUEUE_PASSWORD);
		this.queueBrokerUrl = environment.getProperty(HealthConstant.QUEUE_BROKER_URL);
		this.currentWorkingDirPath = new File(System.getProperty(HealthConstant.CURRENT_WORKING_DIRECTORY));
		this.resultBuilder = new StageHealthCheckHandler.JSONResultBuilder();
		this.virusScanner = virusScanner;
	}

	@Override
	public StageHealthCheckHandler register(String name, Handler<Promise<Status>> procedure) {
		healthChecks.register(name, procedure);
		return this;
	}

	@Override
	public StageHealthCheckHandler register(String name, long timeout, Handler<Promise<Status>> procedure) {
		healthChecks.register(name, timeout, procedure);
		return this;
	}

	/**
	 * @param promise
	 */
	public void queueHealthChecker(Promise<Status> promise, MosipQueueManager<MosipQueue, byte[]> mosipQueueManager, MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory) {

		try {
			final String msg = "Ping";

			if (mosipQueue == null)
				mosipQueue = mosipConnectionFactory.createConnection("ACTIVEMQ", queueUsername,
						queuePassword, queueBrokerUrl);

			mosipQueueManager.send(mosipQueue, msg.getBytes(), HealthConstant.QUEUE_ADDRESS);

			if (!isConsumerStarted) {
				QueueListener listener = new QueueListener() {
					@Override
					public void setListener(Message message) {
						try {
							consumerListener(message, promise, msg);
						} catch (Exception e) {
							LOGGER.error(LoggerFileConstant.SESSIONID.toString(),
									LoggerFileConstant.REGISTRATIONID.toString(), "Unable to check activemq health", ExceptionUtils.getStackTrace(e));
						}
					}
				};
				mosipQueueManager.consume(mosipQueue, HealthConstant.QUEUE_ADDRESS, listener);
				isConsumerStarted = true;
			}
		} catch (Exception e) {
			isConsumerStarted = false;
			mosipQueue = null;
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, e.getMessage()).build();
			promise.complete(Status.KO(result));
		}
	}
	public void consumerListener(Message message, Promise<Status> promise, String msg) {
		String res = new String(((ActiveMQBytesMessage) message).getContent().data);
		if (res == null || !msg.equalsIgnoreCase(res)) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, "Could not read response from queue").build();
			promise.complete(Status.KO(result));
		}
		final JsonObject result = resultBuilder.create().add(HealthConstant.RESPONSE, res).build();
		promise.complete(Status.OK(result));
	}

	/**
	 * @param configuration
	 * @return
	 * @throws Exception
	 */
	private void initSecurityConfiguration(Configuration configuration) throws Exception {
		configuration.set("dfs.data.transfer.protection", "authentication");
		configuration.set("hadoop.security.authentication", "kerberos");
		InputStream krbStream = getClass().getClassLoader().getResourceAsStream("krb5.conf");
		File krbPath = FileUtils.getFile(hadoopLibPath.toString(), "krb5.conf");
		FileUtils.copyInputStreamToFile(krbStream, krbPath);
		System.setProperty("java.security.krb5.conf", krbPath.toString());
		UserGroupInformation.setConfiguration(configuration);
	}

	/**
	 * @param user
	 * @param keytabPath
	 * @throws Exception
	 */
	private void loginWithKeyTab(String user, String keytabPath) throws Exception {
		File keyPath = null;
		Resource resource = resourceLoader.getResource(keytabPath);
		File dataPath = FileUtils.getFile(hadoopLibPath.toString(), "data");
		boolean created = dataPath.mkdir();
		if (resource.exists() && created) {
			keyPath = FileUtils.getFile(dataPath.toString(), resource.getFilename());
			FileUtils.copyInputStreamToFile(resource.getInputStream(), keyPath);
		} else {
			throw new Exception("KEYTAB_FILE_NOT_FOUND_EXCEPTION: " + keytabPath);
		}
		try {
			UserGroupInformation.loginUserFromKeytab(user, keyPath.toString());
		} catch (IOException e) {
			throw new Exception("LOGIN_EXCEPTION", e);
		}
	}

	/**
	 * @param promise
	 */
	public void virusScanHealthChecker(Promise<Status> promise) {
		try {
			File file = new File("virus_scan_test.txt");
			String fileData = "virus scan test";
			Files.write(Paths.get("virus_scan_test.txt"), fileData.getBytes());
			boolean scanResult = this.virusScanner.scanFile(new FileInputStream(file));

			final JsonObject result = resultBuilder.create().add(HealthConstant.RESPONSE, scanResult)
					.build();
			promise.complete(Status.OK(result));

		} catch (IOException e) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, e.getMessage()).build();
			promise.complete(Status.KO(result));
		}
	}

	/**
	 * Database health check handler
	 * 
	 * @param promise {@link Promise} instance from handler
	 */
	public void databaseHealthChecker(Promise<Status> promise) {

		try {
			Class.forName(driver);
		} catch (ClassNotFoundException exception) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, exception.getMessage()).build();
			promise.complete(Status.KO(result));
		}
		try (Connection conn = DriverManager.getConnection(url, username, password)) {
			try (final Statement statement = conn.createStatement()) {

				try (final ResultSet rs = statement.executeQuery(DEFAULT_QUERY)) {

					if (rs.next()) {
						final JsonObject result = resultBuilder.create()
								.add(HealthConstant.DATABASE, conn.getMetaData().getDatabaseProductName())
								.add(HealthConstant.HELLO, JdbcUtils.getResultSetValue(rs, 1)).build();
						promise.complete(Status.OK(result));

					}
				}
			}
		} catch (SQLException exception) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, exception.getMessage()).build();
			promise.complete(Status.KO(result));
		}
	}

	/**
	 * Disk-Space health check Handler
	 * 
	 * @param promise {@link Promise} instance from handler
	 */
	public void dispSpaceHealthChecker(Promise<Status> promise) {

		final long diskFreeInBytes = this.currentWorkingDirPath.getUsableSpace();
		if (diskFreeInBytes >= THRESHOLD) {
			final JsonObject result = resultBuilder.create()
					.add(HealthConstant.TOTAL, this.currentWorkingDirPath.getTotalSpace())
					.add(HealthConstant.FREE, diskFreeInBytes).add(HealthConstant.THRESHOLD, THRESHOLD).build();
			promise.complete(Status.OK(result));
		} else {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR,
					String.format(HealthConstant.THRESHOLD_ERROR, diskFreeInBytes, THRESHOLD)).build();
			promise.complete(Status.KO(result));
		}

	}

	/**
	 * Send Verticle health check handler
	 * 
	 * @param promise {@link Promise} instance from handler
	 * @param vertx  {@link Vertx} instance
	 */
	public void senderHealthHandler(Promise<Status> promise, Vertx vertx, String address) {
		try {
			vertx.eventBus().send(address, HealthConstant.PING);
			final JsonObject result = resultBuilder.create().add(HealthConstant.RESPONSE, HealthConstant.PING).build();
			promise.complete(Status.OK(result));
		} catch (Exception e) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, e.getMessage()).build();
			promise.complete(Status.KO(result));
		}
	}

	/**
	 * @param promise
	 * @param vertx
	 * @param address
	 */
	public void consumerHealthHandler(Promise<Status> promise, Vertx vertx, String address) {
		try {
			Boolean isRegistered = vertx.eventBus().consumer(address).isRegistered();
			final JsonObject result = resultBuilder.create().add(HealthConstant.RESPONSE, isRegistered).build();
			promise.complete(Status.OK(result));
		} catch (Exception e) {
			final JsonObject result = resultBuilder.create().add(HealthConstant.ERROR, e.getMessage()).build();
			promise.complete(Status.KO(result));
		}
	}

	@Override
	public void handle(RoutingContext rc) {
		String path = rc.request().path();
		String mount = rc.mountPoint();
		String route = rc.currentRoute().getPath();

		String id;

		// We are under a sub-router.
		// Remove the mount prefix from the path
		if (mount != null && path.startsWith(mount)) {
			path = path.substring(mount.length());
		}

		// The route has a path, remove this path from the path
		if (route != null && path.startsWith(route)) {
			id = path.substring(route.length());
		} else {
			id = path;
		}

		if (authProvider != null) {
			// Copy all HTTP header in a json array and params
			JsonObject authData = new JsonObject();
			rc.request().headers().forEach(entry -> authData.put(entry.getKey(), entry.getValue()));
			rc.request().params().forEach(entry -> authData.put(entry.getKey(), entry.getValue()));
			if (rc.request().method() == HttpMethod.POST && rc.request().getHeader(HttpHeaders.CONTENT_TYPE) != null
					&& rc.request().getHeader(HttpHeaders.CONTENT_TYPE).contains(HealthConstant.CONTENT_TYPE)) {
				authData.mergeIn(rc.getBodyAsJson());
			}
			authProvider.authenticate(authData, ar -> {
				if (ar.failed()) {
					rc.response().setStatusCode(403).end();
				} else {
					healthChecks.invoke(id, this.healthSummaryHandler(rc));
				}
			});
		} else {
			healthChecks.invoke(id, this.healthSummaryHandler(rc));
		}
	}

	/**
	 * Create health check summary
	 * 
	 * @param rc {@link RoutingContext} instance
	 * @return {@link Handler}
	 */
	private Handler<AsyncResult<JsonObject>> healthSummaryHandler(RoutingContext rc) {
		return json -> {
			HttpServerResponse response = rc.response().putHeader(HttpHeaders.CONTENT_TYPE,
					"application/json;charset=UTF-8");
			if (json.failed()) {
				if (json.cause().getMessage().toLowerCase().contains("not found")) {
					response.setStatusCode(404);
				} else {
					response.setStatusCode(400);
				}
				response.end("{\"message\": \"" + json.cause().getMessage() + "\"}");
			} else {
				createResponse(json.result(), response);
			}
		};
	}

	/**
	 * Create a json response
	 * 
	 * @param json     summary json
	 * @param response {@link HttpResponse}
	 */
	private void createResponse(JsonObject json, HttpServerResponse response) {
		int status = isUp(json) ? 200 : 503;

		if (status == 503 && hasErrors(json)) {
			status = 500;
		}

		json.put(HealthConstant.DETAILS, new JsonObject());

		JsonArray checks = json.getJsonArray(HealthConstant.CHECKS);

		if (status == 200 && checks.isEmpty()) {
			// Special case, no procedure installed.
			response.setStatusCode(204).end();
			return;
		}
		if (checks != null && !checks.isEmpty()) {
			createResponse(json, checks);
		}
		response.setStatusCode(status).end(encode(json));
	}

	/**
	 * Copy actual response to Spring actuator like response
	 * 
	 * @param json   Summary json
	 * @param checks Json array of all registered parameters with details
	 */
	private void createResponse(JsonObject json, JsonArray checks) {
		for (int i = 0; i < checks.size(); i++) {
			JsonObject jsonobject = checks.getJsonObject(i);
			String id = jsonobject.getString(HealthConstant.ID);
			BaseHealthCheckModel healthCheckModel = new BaseHealthCheckModel();
			healthCheckModel.setStatus(jsonobject.getString(HealthConstant.STATUS));
			JsonObject result = null;
			try {
				if (jsonobject.containsKey(HealthConstant.DATA)) {
					healthCheckModel.setDetails(jsonobject.getJsonObject(HealthConstant.DATA).getMap());
					result = new JsonObject(objectMapper.writeValueAsString(healthCheckModel));

				} else {
					result = new JsonObject(objectMapper.writeValueAsString(healthCheckModel));
					result.remove("details");
				}
			} catch (JsonProcessingException e) {
				LOGGER.error(e.getMessage());
			}

			json.getJsonObject(HealthConstant.DETAILS).put(id, result);

		}
	}

	@Override
	public synchronized StageHealthCheckHandler unregister(String name) {
		healthChecks.unregister(name);
		return this;
	}

	/**
	 * Check if error has occurred or not
	 * 
	 * @param json Summary json
	 * @return True if has Error;else False
	 */
	private boolean hasErrors(JsonObject json) {
		JsonObject data = json.getJsonObject(HealthConstant.DATA);
		if (data != null && data.getBoolean("procedure-execution-failure", false)) {
			return true;
		}

		JsonArray checks = json.getJsonArray(HealthConstant.CHECKS);
		if (checks != null) {
			for (int i = 0; i < checks.size(); i++) {
				JsonObject check = checks.getJsonObject(i);
				if (hasErrors(check)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Encode the json object
	 * 
	 * @param json Result json
	 * @return Encoded Json String
	 */
	private String encode(JsonObject json) {
		final String outcome = json.getString(HealthConstant.OUTCOME);
		json.remove(HealthConstant.OUTCOME);
		json.put(HealthConstant.STATUS, outcome);
		return json.encode();
	}

	static class JSONResultBuilder {

		private JsonObject jsonObject;

		public JSONResultBuilder create() {
			jsonObject = new JsonObject();
			return this;
		}

		public JSONResultBuilder add(String key, Object object) {
			jsonObject.put(key, object);
			return this;
		}

		public JsonObject build() {
			return jsonObject;
		}

	}

}
