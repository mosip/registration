package io.mosip.registration.test.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Properties;

import javax.sql.DataSource;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.service.security.ClientSecurityFacade;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.registration.config.PropertiesConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.tpm.spi.TPMUtil;
import io.mosip.registration.context.ApplicationContext;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;


@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
		"io.mosip.kernel.idobjectvalidator.impl.IdObjectCompositeValidator",
		"io.mosip.kernel.idobjectvalidator.impl.IdObjectMasterDataValidator",
		"io.mosip.kernel.packetmanager.impl.PacketDecryptorImpl",
		 "io.mosip.kernel.packetmanager.util.IdSchemaUtils"}), basePackages = {
				"io.mosip.registration", "io.mosip.kernel.core", 
				"io.mosip.kernel.idvalidator", "io.mosip.kernel.ridgenerator","io.mosip.kernel.qrcode",
				"io.mosip.kernel.core.signatureutil", "io.mosip.kernel.crypto", "io.mosip.kernel.jsonvalidator",
				"io.mosip.kernel.idgenerator", "io.mosip.kernel.virusscanner", "io.mosip.kernel.transliteration",
				"io.mosip.kernel.applicanttype", "io.mosip.kernel.core.pdfgenerator.spi",
				"io.mosip.kernel.pdfgenerator.itext.impl", "io.mosip.kernel.cryptosignature",
				"io.mosip.kernel.core.signatureutil", "io.mosip.kernel.idobjectvalidator.impl", 
				"io.mosip.kernel.packetmanager.impl", "io.mosip.kernel.packetmanager.util", 
				"io.mosip.kernel.biosdk.provider.factory"})
public class TestDaoConfig extends HibernateDaoConfig {

	
	private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
	private static final String DB_URL = "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS REG;";
	private static final String DB_AUTHENITICATION = ";bootPassword=";
	private static final String URL = "jdbc:derby:%s;bootPassword=%s";
	private static final String INITIALIZE_URL = "jdbc:derby:%s;bootPassword=%s;newBootPassword=%s";
	private static final String DB_PATH_VAR = "mosip.reg.dbpath";
	private static final String DB_AUTH_FILE_PATH = "mosip.reg.db.key";


	private static DataSource dataSource;
	
	private static Properties keys = new Properties();
	
	
	static {
		ApplicationContext.getInstance();
		
		try (InputStream keyStream = TestDaoConfig.class.getClassLoader().getResourceAsStream("spring-test.properties")) {		
			
			keys.load(keyStream);
			dataSource = setupDataSource(keys.getProperty(DB_PATH_VAR), keys.getProperty(DB_AUTH_FILE_PATH));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@Bean(name = "dataSource")
	public DataSource dataSource() {
		return dataSource;
	}

	
	@Bean
	public static JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

	
	@Bean(name = "propertiesConfig")
	public static PropertiesConfig propertiesConfig() {
		return new PropertiesConfig(jdbcTemplate());
	}
	
	
	@Bean
	@Lazy(false)
	public static PropertyPlaceholderConfigurer properties() {
		
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] {new ClassPathResource("spring-test.properties")};
		ppc.setLocations(resources);

		//Properties properties = new Properties();
		//properties.putAll(propertiesConfig().getDBProps());

		ppc.setProperties(keys);
		ppc.setTrimValues(true);

		return ppc;
	}
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setDatabase(Database.H2);
		vendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("io.mosip.registration", "io.mosip.kernel");
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaProperties(additionalProperties());

		return em;
	}
	
	private Properties additionalProperties() {
		Properties properties = new Properties();
		properties.setProperty("hibernate.hbm2ddl.auto", "create");
		properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		//properties.setProperty("hibernate.current_session_context_class", keys.getProperty("hibernate.current_session_context_class"));
		//properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", keys.getProperty("hibernate.jdbc.lob.non_contextual_creation"));
		properties.setProperty("hibernate.show_sql", "false");
		properties.setProperty("hibernate.format_sql", "false");
		return properties;
	}

	private static DriverManagerDataSource setupDataSource(String dbPath, String dbKey) throws Exception {
		if(ClientSecurityFacade.isDBInitializeRequired()) {
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

				DriverManager.getConnection("jdbc:derby:;shutdown=true");

			} catch (SQLException ex) {
				ex.printStackTrace();
			} finally {
				if(connection != null)
					connection.close();
			}

			if(initialSetup)
				reEncryptDB(dbPath, new String(Base64.decodeBase64(dbKey.getBytes())));
		}

		DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setUrl(String.format(URL, dbPath, ClientSecurityFacade.getDBSecret()));
		return driverManagerDataSource;
	}

	private static void reEncryptDB(String dbPath, String dbKey) throws IOException, NoSuchAlgorithmException, SQLException {
		Connection connection = null;
		try {
			DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());

			connection = DriverManager.getConnection(String.format(INITIALIZE_URL, dbPath,
					dbKey,	ClientSecurityFacade.getDBSecret()));

			DriverManager.getConnection("jdbc:derby:;shutdown=true");

			//connection = null;

			//connection = DriverManager.getConnection(String.format(URL, dbPath, ClientSecurityFacade.getDBSecret()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			if(connection != null)
				connection.close();
		}
	}
}
