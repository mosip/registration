package io.mosip.registration.test.mapper;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.dto.demographic.DemographicDTO;
import io.mosip.registration.dto.demographic.DemographicInfoDTO;
import io.mosip.registration.dto.demographic.IndividualIdentity;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegCenterUser;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.RegCenterUserId;
import io.mosip.registration.entity.id.UserBiometricId;
import io.mosip.registration.entity.id.UserMachineMappingID;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mapper.CustomObjectMapper;
import io.mosip.registration.test.util.datastub.DataProvider;
import ma.glasnost.orika.MapperFacade;

public class CustomObjectMapperTest {

	private MapperFacade mapperFacade = CustomObjectMapper.MAPPER_FACADE;
	private static RegistrationDTO registrationDTO;
	private static DemographicInfoDTO demographicInfoDTO;
	private static DemographicDTO demographicDTO;

	@BeforeClass
	public static void initialize() throws RegBaseCheckedException {
		// RegistrationDTO
		registrationDTO = DataProvider.getPacketDTO();

		// Set DemographicDTO
		//demographicDTO = registrationDTO.getDemographicDTO();

		// DemographicInfoDTO
		//demographicInfoDTO = demographicDTO.getDemographicInfoDTO();

	}

	

	@Test
	public void testOffsetDateTimeConversion() {
		OffsetDateTime time = OffsetDateTime.now();
		OffsetDateTime convertedTime = mapperFacade.map(time, OffsetDateTime.class);
		assertEquals(time, convertedTime);
	}

	/*@Test
	public void testDemographicInfoConversion() {
		assertDemographicInfo(mapperFacade.map(demographicInfoDTO.getIdentity(), IndividualIdentity.class));
	}

	private void assertDemographicInfo(IndividualIdentity actualIdentity) {
		IndividualIdentity expectedIdentity = (IndividualIdentity) demographicInfoDTO.getIdentity();
		assertEquals(expectedIdentity.getDateOfBirth(), actualIdentity.getDateOfBirth());
		assertEquals(expectedIdentity.getGender().get(0).getValue(), actualIdentity.getGender().get(0).getValue());
		assertEquals(expectedIdentity.getAddressLine1().get(0).getValue(),
				actualIdentity.getAddressLine1().get(0).getValue());
		assertEquals(expectedIdentity.getAddressLine2().get(0).getValue(),
				actualIdentity.getAddressLine2().get(0).getValue());
		assertEquals(expectedIdentity.getAddressLine3().get(0).getValue(),
				actualIdentity.getAddressLine3().get(0).getValue());
		assertEquals(expectedIdentity.getRegion().get(0).getValue(), actualIdentity.getRegion().get(0).getValue());
		assertEquals(expectedIdentity.getProvince().get(0).getValue(), actualIdentity.getProvince().get(0).getValue());
		assertEquals(expectedIdentity.getCity().get(0).getValue(), actualIdentity.getCity().get(0).getValue());
		assertEquals(expectedIdentity.getResidenceStatus().get(0).getValue(), actualIdentity.getResidenceStatus().get(0).getValue());
		assertEquals(expectedIdentity.getEmail(), actualIdentity.getEmail());
		assertEquals(expectedIdentity.getPhone(), actualIdentity.getPhone());
	}*/
	
	@Test
	public void testUserDetailConversion() {		
		UserDTO user = mapperFacade.map(assertUserDetailInfo(), UserDTO.class);		
		assertEquals("mosip", user.getRegCenterUser().getUsrId());		
	}
	
	private UserDetail assertUserDetailInfo() {
		UserDetail userDetail = new UserDetail();
		
		Set<UserRole> roles = new HashSet<>();
		UserRole userRole = new UserRole();
		UserRoleId userRoleID = new UserRoleId();
		userRoleID.setRoleCode("REGISTRATION_OFFICER");
		userRole.setUserRoleId(userRoleID);
		userRole.setLangCode("eng");
		userRole.setIsActive(true);
		roles.add(userRole);
		userDetail.setUserRole(roles);
		
		Set<UserMachineMapping> userMachineMappings = new HashSet<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		UserMachineMappingID userMachineMappingId = new UserMachineMappingID();
		userMachineMappingId.setCntrId("10001");
		userMachineMappingId.setMachineId("10001");
		userMachineMappingId.setUsrId("mosip");
		userMachineMapping.setUserMachineMappingId(userMachineMappingId);
		userMachineMapping.setIsActive(true);
		userMachineMapping.setLangCode("eng");
		
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setMacAddress("test");
		machineMaster.setName("name");
		machineMaster.setSerialNum("serialnum");
		userMachineMapping.setMachineMaster(machineMaster);
		userMachineMappings.add(userMachineMapping);
		userDetail.setUserMachineMapping(userMachineMappings);
		
		Set<UserBiometric> userBiometrics = new HashSet<>();
		UserBiometric userBiometric = new UserBiometric();
		UserBiometricId userBiometricId = new UserBiometricId();
		userBiometricId.setBioAttributeCode("bioCode");
		userBiometricId.setBioTypeCode("bioCode");
		userBiometricId.setUsrId("mosip");
		userBiometric.setUserBiometricId(userBiometricId);
		userBiometrics.add(userBiometric);
		userDetail.setUserBiometric(userBiometrics);
		
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("10001");
		regCenterUserId.setUserId("mosip");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		return userDetail;
	}

}
