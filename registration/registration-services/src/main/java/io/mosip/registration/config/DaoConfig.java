package io.mosip.registration.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

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
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.service.security.ClientSecurity;
import io.mosip.registration.service.security.impl.LocalClientSecurityImpl;
import io.mosip.registration.service.security.impl.TPMClientSecurityImpl;

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
	private static final String DB_PATH_VAR = "mosip.reg.dbpath";
	private static final String DB_AUTH_FILE_PATH = "mosip.reg.db.key";
	private static final String TPM_AVIALABLITY_KEY = "mosip.reg.client.tpm.availability";
	private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String URL = "jdbc:derby:%s;bootPassword=%s";
		
	private static Properties keys;
	private static DataSource dataSource;	
	private static ClientSecurity clientSecurity;
		
	static {
		try (InputStream keyStream = DaoConfig.class.getClassLoader().getResourceAsStream("spring.properties")) {
			
			keys = new Properties();
			keys.load(keyStream);
			
			boolean isTPMAvialable = RegistrationConstants.ENABLE.equalsIgnoreCase(keys.getProperty(TPM_AVIALABLITY_KEY));
			
			ApplicationContext.map().put(RegistrationConstants.TPM_AVAILABILITY, isTPMAvialable ? 
							RegistrationConstants.ENABLE : RegistrationConstants.DISABLE);
			
			clientSecurity = isTPMAvialable ? new TPMClientSecurityImpl() : new LocalClientSecurityImpl();
					
			dataSource = setupDataSource(keys.getProperty(DB_PATH_VAR), keys.getProperty(DB_AUTH_FILE_PATH), isTPMAvialable);
			
		} catch (Exception e) {
			LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
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
		return clientSecurity;		
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

	
	
	private static DriverManagerDataSource setupDataSource(String dbPath, String dbKey, boolean isTPMAvailable) throws Exception {
				
		LOGGER.info(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** SETTING UP DATASOURCE *******");
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbPath : " + dbPath);
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE dbKey : "+ dbKey);
		LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "****** DATASOURCE tpmAvailability : "+ isTPMAvailable);
		
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);

		String dbConnectionURL = String.format(URL, dbPath, isTPMAvailable ? 
						new String(clientSecurity.asymmetricDecrypt(Base64.decodeBase64(dbKey.getBytes()))) 
						: new String(Base64.decodeBase64(dbKey.getBytes())));	
		
		driverManagerDataSource.setUrl(dbConnectionURL);
		return driverManagerDataSource;
	}
	
}
