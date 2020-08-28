package io.mosip.registration.test.clientmachinemapping;

import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
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

import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.DeviceTypes;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.MachineMappingDAOImpl;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegDeviceMaster;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.entity.id.UserMachineMappingID;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.DeviceMasterRepository;
import io.mosip.registration.repositories.DeviceTypeRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterDeviceRepository;
import io.mosip.registration.repositories.RegistrationCenterMachineDeviceRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class})
public class UserClientMachineMappingDAOTest {

	@Mock
	private UserMachineMappingRepository machineMappingRepository;
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private MachineMappingDAOImpl machineMappingDAOImpl;
	@Mock
	private CenterMachineRepository centerMachineRepository;
	@Mock
	private MachineMasterRepository machineMasterRepository;
	@Mock
	private UserDetailRepository userDetailRepository;
	@Mock
	private AuditManagerSerivceImpl auditFactory;
	@Mock
	private DeviceTypeRepository deviceTypeRepository;
	@Mock
	private RegistrationCenterDeviceRepository registrationCenterDeviceRepository;
	@Mock
	private RegistrationCenterMachineDeviceRepository registrationCenterMachineDeviceRepository;
	@Mock
	private DeviceMasterRepository deviceMasterRepository;

	@Before
	public void initialize() throws IOException, URISyntaxException {
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void getStationIDRunException() throws RegBaseCheckedException {
		Mockito.when(machineMasterRepository.findByIsActiveTrueAndNameIgnoreCaseAndRegMachineSpecIdLangCode(Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new RegBaseUncheckedException());
		machineMappingDAOImpl.getStationID("localhost");
	}

	@Test
	public void getStationID() throws RegBaseCheckedException {
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setName("localhost");
		machineMaster.setMacAddress("8C-16-45-88-E7-0C");
		RegMachineSpecId specId = new RegMachineSpecId();
		specId.setId("100131");
		specId.setLangCode("eng");
		machineMaster.setRegMachineSpecId(specId);
		Mockito.when(machineMasterRepository.findByIsActiveTrueAndNameIgnoreCaseAndRegMachineSpecIdLangCode(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(machineMaster);
		String stationId = machineMappingDAOImpl.getStationID("localhost");
		Assert.assertSame("100131", stationId);
	}

	@Test
	public void getStationIDNullTest() {
		Mockito.when(centerMachineRepository.findByIsActiveTrueAndCenterMachineIdMachineId(Mockito.anyString()))
				.thenReturn(null);
		try {
			machineMappingDAOImpl.getStationID("localhost");
		} catch (RegBaseCheckedException regBaseCheckedException) {
			Assert.assertNotNull(regBaseCheckedException);
		}
	}

	@Test
	public void isValidDeviceTest() {
		Mockito.when(deviceMasterRepository.countBySerialNumAndNameAndIsActiveTrueAndValidityEndDtimesGreaterThan(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyObject())).thenReturn(1L);
		boolean a = machineMappingDAOImpl.isValidDevice(DeviceTypes.FINGERPRINT, "SF001");
		Assert.assertSame(true, a);
	}

	@Test
	public void getUserMappingDetailsTest() {
		List<UserMachineMapping> list = new ArrayList<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		UserDetail userDetail = new UserDetail();
		userDetail.setId("mosip");
		userMachineMapping.setUserDetail(userDetail);
		list.add(userMachineMapping);
		Mockito.when(machineMappingRepository.findByIsActiveTrueAndUserMachineMappingIdMachineId(Mockito.anyString()))
				.thenReturn(list);
		Assert.assertEquals(userMachineMapping.getUserDetail().getId(),
				machineMappingDAOImpl.getUserMappingDetails("machineId").get(0).getUserDetail().getId());
	}

	@Test
	public void isExistsNullTest() {
		UserMachineMapping machineMapping = null;
		Mockito.when(
				machineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(RegistrationConstants.JOB_TRIGGER_POINT_USER))
				.thenReturn(machineMapping);
		Assert.assertFalse(machineMappingDAOImpl.isExists(RegistrationConstants.JOB_TRIGGER_POINT_USER));
	}
	
	@Test
	public void isExistsTest() {
		UserMachineMapping machineMapping = new UserMachineMapping();		
		UserMachineMappingID machineMapId=new UserMachineMappingID();
		machineMapId.setUsrId("1234");				
		machineMapping.setUserMachineMappingId(machineMapId);
		List<UserMachineMapping> deviceList = new ArrayList<>();
		deviceList.add(machineMapping);
		Mockito.when(
				machineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(RegistrationConstants.JOB_TRIGGER_POINT_USER))
				.thenReturn(machineMapping);
		Assert.assertTrue(machineMappingDAOImpl.isExists(RegistrationConstants.JOB_TRIGGER_POINT_USER));
	}

	@Test
	public void findByRegMachineSpecIdLangCodeTest() {
		RegDeviceMaster deviceMaster = new RegDeviceMaster();
		deviceMaster.setName("Lenovo");
		deviceMaster.setSerialNum("QWAS9087");
		deviceMaster.setDeviceSpecId("1234");
		RegMachineSpecId machineSpecId = new RegMachineSpecId();
		machineSpecId.setLangCode("eng");
		machineSpecId.setId("1");
		deviceMaster.setRegMachineSpecId(machineSpecId);

		List<RegDeviceMaster> deviceList = new ArrayList<>();
		deviceList.add(deviceMaster);
		Mockito.when(deviceMasterRepository.findByRegMachineSpecIdLangCode("eng")).thenReturn(deviceList);
		Assert.assertNotNull((machineMappingDAOImpl.getDevicesMappedToRegCenter("eng")));
	}

	@Test
	public void getKeyIndexByNameTest() {
		MachineMaster machineMaster = PowerMockito.mock(MachineMaster.class);
		machineMaster.setKeyIndex("keyIndex");

		PowerMockito.when(machineMasterRepository.findByIsActiveTrueAndNameIgnoreCaseAndRegMachineSpecIdLangCode(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(machineMaster);

		Assert.assertEquals(machineMaster.getKeyIndex(), machineMappingDAOImpl.getKeyIndexByMachineName("name"));

	}

	@Test
	public void getKeyIndexByMachineNameNullTest() {

		PowerMockito.when(machineMasterRepository.findByIsActiveTrueAndNameIgnoreCaseAndRegMachineSpecIdLangCode(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(null);

		Assert.assertNull(machineMappingDAOImpl.getKeyIndexByMachineName("name"));

	}

}
