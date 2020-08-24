package io.mosip.registration.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
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
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.RegistrationApprovalDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.packet.impl.RegistrationApprovalServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class })
public class RegistrationApprovalServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private RegistrationApprovalServiceImpl registrationApprovalServiceImpl;
	@Mock
	private RegistrationApprovalDTO registrationApprovalDTO;
	@Mock
	private AuditManagerSerivceImpl auditFactory;
	@Mock
	RegistrationDAO registrationDAO;

	@BeforeClass
	public static void setup() {
		SessionContext.destroySession();
	}
	
	@Before
	public void initialize() throws Exception {
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());
		List<String> roles = new ArrayList<>();
		roles.add("SUPERADMIN");
		roles.add("SUPERVISOR");
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip1214");
		PowerMockito.when(SessionContext.userContext().getRoles()).thenReturn(roles);
	}

	@Test
	public void testGetEnrollmentByStatus() throws RegBaseCheckedException {
		Timestamp time = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
		List<Registration> details = new ArrayList<>();
		Registration regobject = new Registration();
		UserDetail regUserDetail = new UserDetail();

		regUserDetail.setId("Mosip123");
		regUserDetail.setName("RegistrationOfficer");

		regobject.setId("123456");
		regobject.setClientStatusCode("R");
		regobject.setCrBy("Mosip123");
		regobject.setCrDtime(time);
		regobject.setAckFilename("file1");

		regobject.setUserdetail(regUserDetail);
		details.add(regobject);

		Mockito.when(registrationDAO.getEnrollmentByStatus("R")).thenReturn(details);

		ReflectionTestUtils.setField(registrationApprovalServiceImpl, "registrationDAO", registrationDAO);

		List<RegistrationApprovalDTO> enrollmentsByStatus = registrationApprovalServiceImpl
				.getEnrollmentByStatus("R");
		assertTrue(enrollmentsByStatus.size() > 0);
		assertEquals("123456", enrollmentsByStatus.get(0).getId());
		assertEquals("file1", enrollmentsByStatus.get(0).getAcknowledgementFormPath());

	}

	@Test
	public void testPacketUpdateStatus() throws RegBaseCheckedException {
		Registration regobject = new Registration();
		UserDetail regUserDetail = new UserDetail();

		regUserDetail.setId("Mosip1214");
		regUserDetail.setName("RegistrationOfficerName");

		regobject.setId("123456");
		regobject.setClientStatusCode("A");
		regobject.setCrBy("Mosip123");
		regobject.setUpdBy(SessionContext.userContext().getUserId());
		regobject.setApproverRoleCode(SessionContext.userContext().getRoles().get(0));
		regobject.setAckFilename("file1");

		regobject.setUserdetail(regUserDetail);

		Mockito.when(registrationDAO.updateRegistration("123456", "", "R")).thenReturn(regobject);

		ReflectionTestUtils.setField(registrationApprovalServiceImpl, "registrationDAO", registrationDAO);

		Registration updateStatus = registrationApprovalServiceImpl.updateRegistration("123456", "", "R");

		assertTrue(updateStatus.getId().equals("123456"));
		assertTrue(updateStatus.getClientStatusCode().equals("A"));
		assertTrue(updateStatus.getUpdBy().equals("mosip1214"));
		assertTrue(updateStatus.getApproverRoleCode().equals("SUPERADMIN"));
		assertTrue(updateStatus.getAckFilename().equals("file1"));

	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testValidateException() throws RegBaseCheckedException {
		when( registrationDAO.getEnrollmentByStatus(Mockito.anyString())).thenThrow(RegBaseUncheckedException.class);
		registrationApprovalServiceImpl.getEnrollmentByStatus("ON_HOLD");
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testValidateException1() throws RegBaseCheckedException {
		registrationApprovalServiceImpl.getEnrollmentByStatus("");
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testValidateException2() throws RegBaseCheckedException {
		registrationApprovalServiceImpl.updateRegistration("123","","");
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testValidateException3() throws RegBaseCheckedException {
		registrationApprovalServiceImpl.updateRegistration("","","REGISTERED");
	}
}
