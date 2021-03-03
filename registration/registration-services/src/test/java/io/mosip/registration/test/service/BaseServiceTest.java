package io.mosip.registration.test.service;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.constants.DeviceTypes;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.BaseService;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class })
public class BaseServiceTest {

	@Mock
	private MachineMappingDAO machineMappingDAO;
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private BaseService baseService;

	@Mock
	private UserOnboardDAO onboardDAO;


	@Test
	public void getCeneterIdTest() {

		PowerMockito.mockStatic(SessionContext.class);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(SessionContext.userId()).thenReturn("MYUSERID");

		Assert.assertSame(baseService.getUserIdFromSession(), "MYUSERID");

	}

	@Test
	public void isNullTest() {
		Assert.assertSame(baseService.isNull(null), true);

	}

	@Test
	public void isEmptyTest() {
		Assert.assertSame(baseService.isEmpty(new LinkedList<>()), true);

	}

	@Test
	public void getStationIdTest() throws RegBaseCheckedException {

		Assert.assertSame(baseService.getStationId(), null);

		Assert.assertSame(baseService.getCenterId("MAC"), null);


	}
	
	

}
