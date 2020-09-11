package io.mosip.registration.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernatePersistenceConstant;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.service.security.ClientSecurity;
import io.mosip.registration.service.security.ClientSecurityFacade;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 *
 * Data source and properties loading from the Database.
 *
 * @author Omsai Eswar M
 *
 */
public class DaoConfig extends HibernateDaoConfig {

	private static final Logger LOGGER = AppConfig.getLogger(DaoConfig.class);

	private static final String LOGGER_CLASS_NAME = "REGISTRATION - DAO Config - DB";
	private static final String dbPath = "db/reg";
	private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String URL = "jdbc:derby:%s;bootPassword=%s";
	private static final String SHUTDOWN_URL = "jdbc:derby:;shutdown=true;deregister=false;";

	private static Properties keys;
	private static DataSource dataSource;

	static {
		try (InputStream keyStream = DaoConfig.class.getClassLoader().getResourceAsStream("spring.properties")) {

			keys = new Properties();
			keys.load(keyStream);

			dataSource = setupDataSource();

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
	@Override
	@Bean(name = "dataSource")
	public DataSource dataSource() {
		return dataSource;
	}

	/**
	 * setting datasource to jdbcTemplate
	 *
	 * @return JdbcTemplate
	 */
	@Bean
	public static JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

	/**
	 * setting jdbcTemplate to PropertiesConfig
	 *
	 * @return PropertiesConfig
	 */
	@Bean(name = "propertiesConfig")
	public static PropertiesConfig propertiesConfig() {
		return new PropertiesConfig(jdbcTemplate());
	}


	/**
	 * setting profile for spring properties
	 *
	 * @return the {@link PropertyPlaceholderConfigurer} after setting the properties
	 */
	@Bean
	//@Lazy(false)
	public static PropertySourcesPlaceholderConfigurer properties() {

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] {new ClassPathResource("spring.properties")};
		ppc.setLocations(resources);

		Properties properties = new Properties();
		properties.putAll(propertiesConfig().getDBProps());

		ppc.setProperties(properties);
		ppc.setTrimValues(true);
		return ppc;
	}

	@Bean(name= "clientSecurity")
	public ClientSecurity getClientSecurity() {
		return ClientSecurityFacade.getClientSecurity();
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

	/*private static DriverManagerDataSource setupDataSource(String dbPath, String dbKey) throws Exception {
		DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setUrl(String.format(URL, dbPath, new String(Base64.decodeBase64(dbKey.getBytes()))));
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "URL >>> " + driverManagerDataSource.getUrl());
		return driverManagerDataSource;
	}*/

	private static DriverManagerDataSource setupDataSource() throws Exception {
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** SETTING UP DATASOURCE *******");
		createDatabase(dbPath);
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setUrl(String.format(URL, dbPath, ClientSecurityFacade.getDBSecret()));
		return driverManagerDataSource;
	}

	/*private static DriverManagerDataSource setupDataSource(String dbPath, String dbKey) throws Exception {
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** SETTING UP DATASOURCE *******");
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbPath : " + dbPath);

		if(ClientSecurityFacade.isDBInitializeRequired()) {
			LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "DB pwd file not found !, initialization required");
			Connection connection = null;
			boolean initialSetup = false;
			try {
				connection = DriverManager.getConnection(String.format(URL, dbPath, new String(Base64.decodeBase64(dbKey.getBytes()))));
				Statement stmt = connection.createStatement();
				ResultSet result = stmt.executeQuery("select val from reg.global_param where name='mosip.registration.initial_setup'");
				while(result.next()) {
					if(RegistrationConstants.ENABLE.equalsIgnoreCase(result.getString(1))) {
						initialSetup = true;
						break;
					}
				}

				shutdownDatabase();

			} catch (SQLException ex) {
				LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
			} finally {
				if(connection != null)
					connection.close();
			}

			LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "INITIAL_SETUP val >>> " + initialSetup);
			if(initialSetup)
				reEncryptDB(dbPath, new String(Base64.decodeBase64(dbKey.getBytes())));
		}

		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setUrl(String.format(URL, dbPath, ClientSecurityFacade.getDBSecret()));
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "URL >>> " + driverManagerDataSource.getUrl());
		return driverManagerDataSource;
	}

	private static void reEncryptDB(String dbPath, String dbKey) throws IOException, NoSuchAlgorithmException, SQLException {
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "Its initial setup - reloading db with new boot pwd");
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(String.format(INITIALIZE_URL, dbPath,
					dbKey,	ClientSecurityFacade.getDBSecret()));

			shutdownDatabase();

		} catch (SQLException ex) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
		} finally {
			if(connection != null)
				connection.close();
		}
	}*/

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
	private static void createDatabase(String dbPath) throws Exception {
		if(createDb(dbPath)) {
			LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbPath : " + dbPath);
			Connection connection = null;
			try {
				connection = DriverManager.getConnection(String.format(URL + ";create=true;",
						dbPath, ClientSecurityFacade.getDBSecret()));

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

	private static boolean createDb(String dbPath) throws RegBaseCheckedException {
		if(!Files.isDirectory(Paths.get(dbPath)) && ClientSecurityFacade.isDBInitializeRequired())
			return true;

		if(Files.isDirectory(Paths.get(dbPath)) && !ClientSecurityFacade.isDBInitializeRequired())
			return false;

		throw new RegBaseCheckedException(RegistrationExceptionConstants.APP_INVALID_STATE.getErrorCode(),
				RegistrationExceptionConstants.APP_INVALID_STATE.getErrorMessage());
	}

}