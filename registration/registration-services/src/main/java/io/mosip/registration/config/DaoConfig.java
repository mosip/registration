package io.mosip.registration.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import io.mosip.kernel.clientcrypto.constant.ClientCryptoManagerConstant;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernatePersistenceConstant;
import io.mosip.registration.constants.RegistrationConstants;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 *
 * Data source and properties loading from the Database.
 *
 * @author Omsai Eswar M
 *
 */
@ComponentScan(basePackages = { "io.mosip.kernel.core", "io.mosip.kernel.clientcrypto.service.impl",
		"io.mosip.kernel.partnercertservice.service", "io.mosip.kernel.partnercertservice.helper" })
public class DaoConfig extends HibernateDaoConfig {

	private static final Logger LOGGER = AppConfig.getLogger(DaoConfig.class);

	private static final String LOGGER_CLASS_NAME = "REGISTRATION - DAO Config - DB";
	private static final String dbPath = "db/reg";
	private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String URL = "jdbc:derby:%s;bootPassword=%s";
	private static final String SHUTDOWN_URL = "jdbc:derby:;shutdown=true;deregister=false;";
	private static final String ENCRYPTION_URL_ATTRIBUTES = "dataEncryption=true;encryptionKeyLength=256;encryptionAlgorithm=AES/CFB/NoPadding;";
	private static final String SCHEMA_NAME = "REG";
	private static final String SEPARATOR = "-BREAK-";
	private static final String BOOTPWD_KEY = "bootPassword";
	private static final String USERNAME_KEY = "username";
	private static final String PWD_KEY = "password";
	private static final String STATE_KEY = "state";
	private static final String ERROR_STATE = "0";
	private static final String SAFE_STATE = "1";

	private static Properties keys;
	private static JdbcTemplate jdbcTemplate;

	private static final String GLOBAL_PARAM_PROPERTIES = "SELECT CODE, VAL FROM REG.GLOBAL_PARAM WHERE IS_ACTIVE=TRUE AND VAL IS NOT NULL";
	private static final String KEY = "CODE";
	private static final String VALUE = "VAL";

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private ConfigurableEnvironment environment;

	private static boolean isPPCUpdated = false;
	private static PropertySourcesPlaceholderConfigurer ppc = null;

	private DriverManagerDataSource driverManagerDataSource = null;

	static {

		// TODO - Remove this in next release
		ClientCryptoFacade
				.setIsTPMRequired(RegistrationConstants.ENABLE.equalsIgnoreCase(ApplicationContext.getTPMUsageFlag()));

		try (InputStream keyStream = DaoConfig.class.getClassLoader().getResourceAsStream("spring.properties")) {

			keys = new Properties();
			keys.load(keyStream);

		} catch (Exception e) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
					"Exception encountered during context initialization - DaoConfig "
							+ ExceptionUtils.getStackTrace(e));
			System.exit(0);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig#dataSource()
	 */
	@SneakyThrows
	@Override
	@Bean(name = "dataSource")
	public DataSource dataSource() {
		if (this.driverManagerDataSource == null) {
			setupDataSource();
			jdbcTemplate();
		}
		return this.driverManagerDataSource;
	}

	/**
	 * setting datasource to jdbcTemplate
	 *
	 * @return JdbcTemplate
	 */
	@Bean
	@DependsOn("dataSource")
	public JdbcTemplate jdbcTemplate() {
		if (jdbcTemplate == null)
			jdbcTemplate = new JdbcTemplate(this.driverManagerDataSource);
		updateGlobalParamsInProperties(jdbcTemplate);
		return jdbcTemplate;
	}

	/**
	 * setting profile for spring properties
	 *
	 * @return the {@link PropertyPlaceholderConfigurer} after setting the
	 *         properties
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer properties() {
		ppc = new PropertySourcesPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] { new ClassPathResource("spring.properties") };
		ppc.setLocations(resources);
		ppc.setTrimValues(true);
		return ppc;
	}

	@Override
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactory.setDataSource(dataSource());
		entityManagerFactory.setPackagesToScan(HibernatePersistenceConstant.MOSIP_PACKAGE);
		entityManagerFactory.setPersistenceUnitName(HibernatePersistenceConstant.HIBERNATE);
		entityManagerFactory.setJpaPropertyMap(jpaProperties());
		entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter());
		entityManagerFactory.setJpaDialect(jpaDialect());
		return entityManagerFactory;
	}

	@Override
	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(
				Boolean.parseBoolean(keys.getProperty("hibernate.generate_ddl", HibernatePersistenceConstant.FALSE)));
		vendorAdapter.setShowSql(Boolean.parseBoolean(
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL, HibernatePersistenceConstant.FALSE)));
		return vendorAdapter;
	}

	@Override
	public Map<String, Object> jpaProperties() {
		HashMap<String, Object> jpaProperties = new HashMap<>();
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_HBM2DDL_AUTO, keys
				.getProperty(HibernatePersistenceConstant.HIBERNATE_HBM2DDL_AUTO, HibernatePersistenceConstant.UPDATE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_DIALECT, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_DIALECT, HibernatePersistenceConstant.MY_SQL5_DIALECT));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL, HibernatePersistenceConstant.TRUE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_FORMAT_SQL,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_FORMAT_SQL, HibernatePersistenceConstant.TRUE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CONNECTION_CHAR_SET, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_CONNECTION_CHAR_SET, HibernatePersistenceConstant.UTF8));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE,
						HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_QUERY_CACHE, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_CACHE_USE_QUERY_CACHE, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES,
						HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_GENERATE_STATISTICS, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_GENERATE_STATISTICS, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_NON_CONTEXTUAL_CREATION, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_NON_CONTEXTUAL_CREATION, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CURRENT_SESSION_CONTEXT, keys.getProperty(
				HibernatePersistenceConstant.HIBERNATE_CURRENT_SESSION_CONTEXT, HibernatePersistenceConstant.JTA));
		return jpaProperties;
	}

	private void setupDataSource() throws Exception {
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** SETTING UP DATASOURCE *******");
		createDatabase();
		reEncryptExistingDB();
		setupUserAndPermits();
		Map<String, String> dbConf = getDBConf();
		this.driverManagerDataSource = new DriverManagerDataSource();
		this.driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		this.driverManagerDataSource.setSchema(SCHEMA_NAME);
		this.driverManagerDataSource.setUrl(String.format(URL, dbPath, dbConf.get(BOOTPWD_KEY)));
		this.driverManagerDataSource.setUsername(dbConf.get(USERNAME_KEY));
		this.driverManagerDataSource.setPassword(dbConf.get(PWD_KEY));
	}

	private static void shutdownDatabase() {
		try {
			DriverManager.getConnection(SHUTDOWN_URL);
		} catch (SQLException ex) {
			if (((ex.getErrorCode() == 50000) && ("XJ015".equals(ex.getSQLState())))) {
				LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "Derby DB shutdown successful.");
			} else
				LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
		}
	}

	/**
	 * check if db/reg doesnot exists && db.conf doesnot exists if true -> creates
	 * db -> create DB secret -> runs initial DB script -> shutdown database
	 */
	private void createDatabase() throws Exception {
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbPath : " + dbPath);
		Connection connection = null;
		try {
			if (createDb(dbPath)) {
				Map<String, String> dbConf = getDBConf();
				connection = DriverManager.getConnection(String
						.format(URL + ";create=true;" + ENCRYPTION_URL_ATTRIBUTES, dbPath, dbConf.get(BOOTPWD_KEY)),
						dbConf.get(USERNAME_KEY), dbConf.get(PWD_KEY));
				SQLWarning sqlWarning = connection.getWarnings();
				if (sqlWarning != null) {
					LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
							ExceptionUtils.getStackTrace(sqlWarning.getCause()));
					throw new Exception(sqlWarning.getCause());// SQLWarning will not be available once connection is
																// closed.
				}
				org.apache.derby.tools.ij.runScript(connection,
						DaoConfig.class.getClassLoader().getResourceAsStream("initial.sql"), "UTF-8", System.out,
						"UTF-8");
				shutdownDatabase();
				dbConf.put(STATE_KEY, SAFE_STATE);
				saveDbConf(dbConf);
			}
		} finally {
			if (connection != null)
				connection.close();
		}
	}

	private void reEncryptExistingDB() throws Exception {
		Connection connection = null;
		try {
			Map<String, String> dbConf = getDBConf();
			if (dbConf.get(STATE_KEY).equals(ERROR_STATE)) {
				shutdownDatabase(); // We need to shutdown DB before encrypting
				LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
						"IMP : (Re)Encrypting DB started ......");
				connection = DriverManager.getConnection("jdbc:derby:" + dbPath + ";" + ENCRYPTION_URL_ATTRIBUTES
						+ ";bootPassword=" + dbConf.get(BOOTPWD_KEY), dbConf.get(USERNAME_KEY), dbConf.get(PWD_KEY));
				SQLWarning sqlWarning = connection.getWarnings();
				if (sqlWarning != null) {
					LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
							ExceptionUtils.getStackTrace(sqlWarning.getCause()));
					throw new Exception(sqlWarning.getCause()); // SQLWarning will not be available once connection is
																// closed.
				}
				LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "IMP : (Re)Encrypting DB Done ......");
				dbConf.put(STATE_KEY, SAFE_STATE);
				saveDbConf(dbConf);
			}
		} finally {
			if (connection != null)
				connection.close();
		}
	}

	private void setupUserAndPermits() throws Exception {
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, "Checking Derby Security properties", "Started ... ");
		Connection connection = null;
		try {
			Map<String, String> dbConf = getDBConf();
			connection = DriverManager.getConnection(String.format(URL, dbPath, getDBConf().get(BOOTPWD_KEY)),
					dbConf.get(USERNAME_KEY), dbConf.get(PWD_KEY));
			if (!isUserSetupComplete(connection, dbConf)) {
				try (Statement statement = connection.createStatement()) {
					LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
							"Started setting up DB user and access permits...");
					// setting requireAuthentication
					statement.executeUpdate(
							"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'true')");
					// Setting authentication scheme to derby
					statement.executeUpdate(
							"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.authentication.provider', 'BUILTIN')");
					// creating user
					statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user."
							+ dbConf.get(USERNAME_KEY) + "', '" + dbConf.get(PWD_KEY) + "')");
					// setting default connection mode to noaccess
					statement.executeUpdate(
							"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.defaultConnectionMode', 'noAccess')");
					// setting read-write access to only one user
					statement.executeUpdate(
							"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.fullAccessUsers', '"
									+ dbConf.get(USERNAME_KEY) + "')");
					// property ensures that database-wide properties cannot be overridden by
					// system-wide properties
					statement.executeUpdate(
							"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.propertiesOnly', 'true')");
					// shutdown derby db, for the changes to be applied
					shutdownDatabase();
				} catch (Throwable t) {
					LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(t));
					cleanupUserAuthAndPermits(connection, dbConf);
				}
				LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "Security setup check completed.");
			}
		} finally {
			if (connection != null)
				connection.close();
		}
	}

	private boolean isUserSetupComplete(Connection connection, Map<String, String> dbConf) {
		boolean completed = false;
		try (Statement statement = connection.createStatement()) {
			isKeySet(statement, "derby.connection.requireAuthentication", "true");
			isKeySet(statement, "derby.authentication.provider", "BUILTIN");
			isKeySet(statement, "derby.user." + dbConf.get(USERNAME_KEY), dbConf.get(PWD_KEY));
			isKeySet(statement, "derby.database.defaultConnectionMode", "noAccess");
			isKeySet(statement, "derby.database.fullAccessUsers", dbConf.get(USERNAME_KEY));
			isKeySet(statement, "derby.database.propertiesOnly", "true");
			completed = true;
			LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
					"Security setup check is complete & success.");
		} catch (RegBaseCheckedException | SQLException regBaseCheckedException) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return completed;
	}

	private void cleanupUserAuthAndPermits(Connection connection, Map<String, String> dbConf) {
		try (Statement statement = connection.createStatement()) {
			// setting requireAuthentication
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'false')");
			// Setting authentication scheme to derby
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.authentication.provider', null)");
			// creating user
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user." + dbConf.get(USERNAME_KEY) + "', null)");
			// setting default connection mode to noaccess
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.defaultConnectionMode', 'fullAccess')");
			// setting read-write access to only one user
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.fullAccessUsers', null)");
			// property ensures that database-wide properties cannot be overridden by
			// system-wide properties
			statement.executeUpdate(
					"CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.propertiesOnly', 'false')");
		} catch (SQLException sqlException) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, "Failed to cleanup security properties",
					ExceptionUtils.getStackTrace(sqlException));
		} finally {
			shutdownDatabase();// shutdown derby db, for the changes to be applied
		}
	}

	private void isKeySet(Statement statement, String key, String value) throws SQLException, RegBaseCheckedException {
		ResultSet rs = statement.executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('" + key + "')");
		if (rs.next() && value.equalsIgnoreCase(rs.getString(1)))
			return;
		throw new RegBaseCheckedException("", key + " : is not set to preferred value!");
	}

	private boolean createDb(String dbPath) throws RegBaseCheckedException {
		if (!Files.isDirectory(Paths.get(dbPath)) && isDBInitializeRequired())
			return true;

		if (Files.isDirectory(Paths.get(dbPath)) && !isDBInitializeRequired())
			return false;

		throw new RegBaseCheckedException(RegistrationExceptionConstants.APP_INVALID_STATE.getErrorCode(),
				RegistrationExceptionConstants.APP_INVALID_STATE.getErrorMessage());
	}

	public void updateGlobalParamsInProperties(JdbcTemplate jdbcTemplate) {
		if (!isPPCUpdated) {
			Properties properties = new Properties();
			properties.putAll(getDBProps(jdbcTemplate));
			PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("gobalparams", properties);
			environment.getPropertySources().addFirst(propertiesPropertySource);
			isPPCUpdated = true;
		}
	}

	/**
	 * Fetch all the active global param values from the DB and set it in a map
	 * 
	 * @return Collection of Global param values
	 */
	private static Map<String, Object> getDBProps(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.query(GLOBAL_PARAM_PROPERTIES, new ResultSetExtractor<Map<String, Object>>() {
			@Override
			public Map<String, Object> extractData(ResultSet globalParamResultset) throws SQLException {
				Map<String, Object> globalParamProps = new WeakHashMap<>();
				while (globalParamResultset.next()) {
					globalParamProps.put(globalParamResultset.getString(KEY), globalParamResultset.getString(VALUE));
				}
				globalParamProps.put("objectstore.adapter.name", "PosixAdapter");
				globalParamProps.put("mosip.sign.refid", keys.getProperty("mosip.sign.refid", "SIGN"));
				return globalParamProps;
			}
		});
	}

	private boolean isDBInitializeRequired() {
		File parentDir = new File(
				ClientCryptoManagerConstant.KEY_PATH + File.separator + ClientCryptoManagerConstant.KEYS_DIR);
		if (!parentDir.exists())
			parentDir.mkdirs();

		File dbConf = new File(ClientCryptoManagerConstant.KEY_PATH + File.separator
				+ ClientCryptoManagerConstant.KEYS_DIR + File.separator + ClientCryptoManagerConstant.DB_PWD_FILE);
		if (dbConf.exists())
			return false;

		return true;
	}

	private Map<String, String> getDBConf() throws IOException {
		Path path = Paths.get(ClientCryptoManagerConstant.KEY_PATH, ClientCryptoManagerConstant.KEYS_DIR,
				ClientCryptoManagerConstant.DB_PWD_FILE);
		if (!path.toFile().exists()) {
			LOGGER.info("REGISTRATION  - DaoConfig", APPLICATION_NAME, APPLICATION_ID,
					"getDBSecret invoked - DB_PWD_FILE not found !");
			StringBuilder dbConf = new StringBuilder();
			dbConf.append(RandomStringUtils.randomAlphanumeric(20));
			dbConf.append(SEPARATOR);
			dbConf.append(RandomStringUtils.randomAlphanumeric(10));
			dbConf.append(SEPARATOR);
			dbConf.append(RandomStringUtils.randomAlphanumeric(20));
			dbConf.append(SEPARATOR);
			dbConf.append(ERROR_STATE); // states if successful db conf. 1 = SAFE_STATE, 0 = ERROR_STATE
			saveDbConf(dbConf.toString());
		}
		return parseDbConf(Files.readAllBytes(path));
	}

	private Map<String, String> parseDbConf(byte[] dbConf) {
		String decryptedConf = new String(
				clientCryptoFacade.getClientSecurity().asymmetricDecrypt(Base64.getDecoder().decode(dbConf)));
		String[] parts = decryptedConf.split(SEPARATOR);
		Map<String, String> conf = new HashMap<>();
		// older versions of reg-cli, re-encrypt db and set the new flags
		if (parts.length == 1) {
			conf.put(BOOTPWD_KEY, parts[0]);
			conf.put(USERNAME_KEY, RandomStringUtils.randomAlphanumeric(20));
			conf.put(PWD_KEY, RandomStringUtils.randomAlphanumeric(20));
			conf.put(STATE_KEY, ERROR_STATE);
		} else {
			conf.put(BOOTPWD_KEY, parts[0]);
			conf.put(USERNAME_KEY, parts[1]);
			conf.put(PWD_KEY, parts[2]);
			conf.put(STATE_KEY, parts[3]);
		}
		return conf;
	}

	private void saveDbConf(Map<String, String> dbConf) throws IOException {
		StringBuilder dbConfBuilder = new StringBuilder();
		dbConfBuilder.append(dbConf.get(BOOTPWD_KEY));
		dbConfBuilder.append(SEPARATOR);
		dbConfBuilder.append(dbConf.get(USERNAME_KEY));
		dbConfBuilder.append(SEPARATOR);
		dbConfBuilder.append(dbConf.get(PWD_KEY));
		dbConfBuilder.append(SEPARATOR);
		dbConfBuilder.append(dbConf.get(STATE_KEY));
		saveDbConf(dbConfBuilder.toString());
	}

	private void saveDbConf(String dbConf) throws IOException {
		byte[] cipher = clientCryptoFacade.getClientSecurity().asymmetricEncrypt(dbConf.getBytes());

		try (FileOutputStream fos = new FileOutputStream(Paths.get(ClientCryptoManagerConstant.KEY_PATH,
				ClientCryptoManagerConstant.KEYS_DIR, ClientCryptoManagerConstant.DB_PWD_FILE).toFile())) {
			fos.write(java.util.Base64.getEncoder().encode(cipher));
			LOGGER.debug("REGISTRATION  - DaoConfig", APPLICATION_NAME, APPLICATION_ID, "Saved DB configuration");
		}
	}

}