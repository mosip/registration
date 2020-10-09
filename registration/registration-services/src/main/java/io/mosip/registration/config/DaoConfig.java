package io.mosip.registration.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import io.mosip.kernel.clientcrypto.constant.ClientCryptoManagerConstant;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
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
@ComponentScan(basePackages = {"io.mosip.kernel.core", "io.mosip.kernel.clientcrypto.service.impl"})
public class DaoConfig extends HibernateDaoConfig {

	private static final Logger LOGGER = AppConfig.getLogger(DaoConfig.class);

	private static final String LOGGER_CLASS_NAME = "REGISTRATION - DAO Config - DB";
	private static final String dbPath = "db/reg";
	private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String URL = "jdbc:derby:%s;bootPassword=%s";
	private static final String SHUTDOWN_URL = "jdbc:derby:;shutdown=true;deregister=false;";
	private static final String SCHEMA_NAME = "REG";

	private static Properties keys;
	private static JdbcTemplate jdbcTemplate;

	private static final String GLOBAL_PARAM_PROPERTIES = "SELECT CODE, VAL FROM REG.GLOBAL_PARAM WHERE IS_ACTIVE=TRUE AND VAL IS NOT NULL";
	private static final String KEY = "CODE";
	private static final String VALUE= "VAL";

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private ConfigurableEnvironment environment;

	private static boolean isPPCUpdated = false;
	private static PropertySourcesPlaceholderConfigurer ppc = null;

	static {
		ClientCryptoFacade.setIsTPMRequired(RegistrationConstants.ENABLE.equalsIgnoreCase(ApplicationContext.getTPMUsageFlag()));

		try (InputStream keyStream = DaoConfig.class.getClassLoader().getResourceAsStream("spring.properties")) {

			keys = new Properties();
			keys.load(keyStream);

		} catch (Exception e) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
					"Exception encountered during context initialization - DaoConfig " + ExceptionUtils.getStackTrace(e));
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
		return setupDataSource();
	}

	/**
	 * setting datasource to jdbcTemplate
	 *
	 * @return JdbcTemplate
	 */
	@Bean
	@DependsOn("dataSource")
	public JdbcTemplate jdbcTemplate() {
		jdbcTemplate = new JdbcTemplate(dataSource());
		updateGlobalParamsInProperties(jdbcTemplate);
		return jdbcTemplate;
	}

	/**
	 * setting profile for spring properties
	 *
	 * @return the {@link PropertyPlaceholderConfigurer} after setting the properties
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer properties() {
		ppc = new PropertySourcesPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] {new ClassPathResource("spring.properties")};
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
		vendorAdapter.setGenerateDdl(Boolean.parseBoolean(keys.getProperty("hibernate.generate_ddl",
				HibernatePersistenceConstant.FALSE)));
		vendorAdapter.setShowSql(Boolean.parseBoolean(keys.getProperty(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL,
				HibernatePersistenceConstant.FALSE)));
		return vendorAdapter;
	}

	@Override
	public Map<String, Object> jpaProperties() {
		HashMap<String, Object> jpaProperties = new HashMap<>();
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_HBM2DDL_AUTO,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_HBM2DDL_AUTO, HibernatePersistenceConstant.UPDATE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_DIALECT,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_DIALECT, HibernatePersistenceConstant.MY_SQL5_DIALECT));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_SHOW_SQL, HibernatePersistenceConstant.TRUE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_FORMAT_SQL,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_FORMAT_SQL, HibernatePersistenceConstant.TRUE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CONNECTION_CHAR_SET,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CONNECTION_CHAR_SET, HibernatePersistenceConstant.UTF8));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_QUERY_CACHE,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_QUERY_CACHE, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_GENERATE_STATISTICS,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_GENERATE_STATISTICS, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_NON_CONTEXTUAL_CREATION,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_NON_CONTEXTUAL_CREATION, HibernatePersistenceConstant.FALSE));
		jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_CURRENT_SESSION_CONTEXT,
				keys.getProperty(HibernatePersistenceConstant.HIBERNATE_CURRENT_SESSION_CONTEXT, HibernatePersistenceConstant.JTA));
		return jpaProperties;
	}

	private DriverManagerDataSource setupDataSource() throws Exception {
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** SETTING UP DATASOURCE *******");
		createDatabase(dbPath);
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setSchema(SCHEMA_NAME);
		driverManagerDataSource.setUrl(String.format(URL, dbPath, getDBSecret()));
		return driverManagerDataSource;
	}

	private static void shutdownDatabase() {
		try {
			DriverManager.getConnection(SHUTDOWN_URL);
		} catch (SQLException ex) {
			if(((ex.getErrorCode() == 50000) && ("XJ015".equals(ex.getSQLState())))) {
				LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "Derby DB shutdown successful.");
			}
			else
				LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
		}
	}

	/**
	 * check if db/reg doesnot exists && db.conf doesnot exists
	 * if true
	 * 	-> creates db
	 * 	-> create DB secret
	 * 	-> runs initial DB script
	 * 	-> shutdown database
	 */
	private void createDatabase(String dbPath) throws Exception {
		if(createDb(dbPath)) {
			LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbPath : " + dbPath);
			Connection connection = null;
			try {
				connection = DriverManager.getConnection(String.format(URL + ";create=true;",
						dbPath, getDBSecret()));

				org.apache.derby.tools.ij.runScript(connection,
						DaoConfig.class.getClassLoader().getResourceAsStream("initial.sql"),
						"UTF-8",
						System.out,
						"UTF-8");

				shutdownDatabase();

			} finally {
				if(connection != null)
					connection.close();
			}
		}
	}

	private boolean createDb(String dbPath) throws RegBaseCheckedException {
		if(!Files.isDirectory(Paths.get(dbPath)) && isDBInitializeRequired())
			return true;

		if(Files.isDirectory(Paths.get(dbPath)) && !isDBInitializeRequired())
			return false;

		throw new RegBaseCheckedException(RegistrationExceptionConstants.APP_INVALID_STATE.getErrorCode(),
				RegistrationExceptionConstants.APP_INVALID_STATE.getErrorMessage());
	}

	public void updateGlobalParamsInProperties(JdbcTemplate jdbcTemplate) {
		if(!isPPCUpdated) {
			Properties properties = new Properties();
			properties.putAll(getDBProps(jdbcTemplate));
			PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("gobalparams", properties);
			environment.getPropertySources().addFirst(propertiesPropertySource);
			isPPCUpdated = true;
		}
	}

	/**
	 * Fetch all the active global param values from the DB and set it in a map
	 * @return Collection of Global param values
	 */
	private static Map<String,Object> getDBProps(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.query(GLOBAL_PARAM_PROPERTIES, new ResultSetExtractor<Map<String,Object>>(){
			@Override
			public Map<String,Object> extractData(ResultSet globalParamResultset) throws SQLException {
				Map<String,Object> globalParamProps= new WeakHashMap<>();
				while(globalParamResultset.next()){
					globalParamProps.put(globalParamResultset.getString(KEY),globalParamResultset.getString(VALUE));
				}
				globalParamProps.put("objectstore.adapter.name", "PosixAdapter");
		        globalParamProps.put("mosip.sign.refid", "SIGNATUREKEY");
				return globalParamProps;
			}
		});
	}

	private boolean isDBInitializeRequired() {
		File parentDir = new File(ClientCryptoManagerConstant.KEY_PATH + File.separator + ClientCryptoManagerConstant.KEYS_DIR);
		if(!parentDir.exists())
			parentDir.mkdirs();

		File dbConf = new File(ClientCryptoManagerConstant.KEY_PATH + File.separator +
				ClientCryptoManagerConstant.KEYS_DIR + File.separator + ClientCryptoManagerConstant.DB_PWD_FILE);
		if(dbConf.exists())
			return false;

		return true;
	}

	private String getDBSecret() throws IOException {
		File dbConf = new File(ClientCryptoManagerConstant.KEY_PATH + File.separator +
				ClientCryptoManagerConstant.KEYS_DIR + File.separator + ClientCryptoManagerConstant.DB_PWD_FILE);
		if(!dbConf.exists()) {
			LOGGER.info("REGISTRATION  - DaoConfig", APPLICATION_NAME, APPLICATION_ID,
					"getDBSecret invoked - DB_PWD_FILE not found !");
			String newBootPassowrd = RandomStringUtils.random(20, true, true);
			byte[] cipher = clientCryptoFacade.getClientSecurity().asymmetricEncrypt(newBootPassowrd.getBytes());

			try(FileOutputStream fos = new FileOutputStream(dbConf)) {
				fos.write(java.util.Base64.getEncoder().encode(cipher));
				LOGGER.debug("REGISTRATION  - DaoConfig", APPLICATION_NAME, APPLICATION_ID, "Generated new derby boot key");
			}
		}

		String key = new String(Files.readAllBytes(dbConf.toPath()));
		return new String( clientCryptoFacade.getClientSecurity().asymmetricDecrypt(Base64.getDecoder().decode(key)));
	}

}