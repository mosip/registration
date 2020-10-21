package io.mosip.registration.test.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.mosip.registration.config.DaoConfig;
import io.mosip.registration.context.ApplicationContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, ClientCryptoFacade.class, ApplicationContext.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class DaoConfigTest {

	DaoConfig daoConfig;

	@Mock
	Properties props;

	@Test
	@Ignore
	public void dataSourceTest() throws Exception {
		System.setProperty("mosip.reg.db.key", "src/test/resources/labels_en.properties");
		
		Map<String, Object> applicationMap = new HashMap<>();		
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);

		PowerMockito.mockStatic(ClientCryptoFacade.class);

		byte[] decryptedData = "decryptedData".getBytes();

		PowerMockito.doReturn(decryptedData).when(ClientCryptoFacade.class, "encrypt", Mockito.any());

		daoConfig = new DaoConfig();
		assertEquals(daoConfig.dataSource().getClass(), DriverManagerDataSource.class);
		assertEquals(daoConfig.jdbcTemplate().getClass(), JdbcTemplate.class);
		//assertEquals(daoConfig.propertiesConfig().getClass(), PropertiesConfig.class);
	}
	
	@Test
	@Ignore
	public void dataSourceTestTPMEnabled() throws Exception {
		System.setProperty("mosip.reg.db.key", "src/test/resources/messages_en.properties");
		
		Map<String, Object> applicationMap = new HashMap<>();		
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);

		PowerMockito.mockStatic(ClientCryptoFacade.class);

		byte[] decryptedData = "decryptedData".getBytes();

		PowerMockito.doReturn(decryptedData).when(ClientCryptoFacade.class, "decrypt", Mockito.any());

		daoConfig = new DaoConfig();
		assertEquals(daoConfig.dataSource().getClass(), DriverManagerDataSource.class);
		assertEquals(daoConfig.jdbcTemplate().getClass(), JdbcTemplate.class);
		//assertEquals(daoConfig.propertiesConfig().getClass(), PropertiesConfig.class);
	}

	@Test(expected = RuntimeException.class)
	@Ignore
	public void propertiesTest() throws Exception {
		System.setProperty("mosip.reg.db.key", "src/test/resources/labels_en.properties");

		Map<String, Object> applicationMap = new HashMap<>();		
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		
		PowerMockito.mockStatic(ClientCryptoFacade.class);

		byte[] decryptedData = "decryptedData".getBytes();

		PowerMockito.doReturn(decryptedData).when(ClientCryptoFacade.class, "decrypt", Mockito.any());
		daoConfig.properties();
	}
}