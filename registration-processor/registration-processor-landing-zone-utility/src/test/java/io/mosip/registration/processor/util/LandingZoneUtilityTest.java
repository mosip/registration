package io.mosip.registration.processor.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;


@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ IOUtils.class, HMACUtils2.class})
public class LandingZoneUtilityTest {
	@InjectMocks
	private LandingZoneUtility landingZoneUtility;

	@Mock
	 private ObjectStoreAdapter objectStoreAdapter;

	    /**
	     * The sync registration service.
	     */
	@Mock
	Environment env;

	@Mock
	 private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	@Before
	public void setup() throws ApisResourceAccessException {
		ReflectionTestUtils.setField(landingZoneUtility, "extention", ".zip");
		ReflectionTestUtils.setField(landingZoneUtility, "landingZoneType", "ObjectStore");
		ReflectionTestUtils.setField(landingZoneUtility, "landingZoneAccount", "LandingZoneAccount");
		Mockito.when(env.getProperty(any())).thenReturn("/mnt/regproc/landing");
		
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenReturn(new byte[2]);
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(true);		
		
	}
	@Test
	public void movePacketsToObjectStoreTest() {
		landingZoneUtility.movePacketsToObjectStore();
	};
	@Test
	public void movePacketsToObjectStoreDMZServerFailuretest() throws ApisResourceAccessException {
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenReturn(null);
		landingZoneUtility.movePacketsToObjectStore();
	};
	@Test
	public void movePacketsToObjectStoreFailureTest() {
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(false);		
		landingZoneUtility.movePacketsToObjectStore();
	};
}
