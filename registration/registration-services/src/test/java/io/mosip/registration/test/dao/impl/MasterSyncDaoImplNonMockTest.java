package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;

import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BiometricTypeRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.test.config.TestDaoConfig;


/**
 * @author anusha
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes= {TestDaoConfig.class})
public class MasterSyncDaoImplNonMockTest {
	
	@Autowired
	private MasterSyncDao masterSyncDaoImpl;
	
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;
	
	@Autowired
	private BiometricTypeRepository biometricTypeRepository;
	
	@Autowired
	private UserDetailRepository userDetailRepository;
	
	@Autowired
	private MachineMappingDAO machineMappingDAO;
	
	@Autowired
	private MachineMasterRepository machineMasterRepository;
	
	
	@BeforeClass
	public static void setup() throws Exception {		
		
	  Map<String, Object> appMap = new HashMap<String,Object>();
	  appMap.put(RegistrationConstants.DOC_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.FINGERPRINT_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.IRIS_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.FACE_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.PRIMARY_LANGUAGE, "eng");
	  appMap.put(RegistrationConstants.SECONDARY_LANGUAGE, "fra");
	  appMap.put(RegistrationConstants.TPM_AVAILABILITY, RegistrationConstants.DISABLE);
	  ApplicationContext.getInstance().setApplicationMap(appMap);
	  ApplicationContext.getInstance().loadResourceBundle();
	}
	
	private void mockRegcenterUserMapping() {
		List<UserDetail> users = new ArrayList<UserDetail>();
		UserDetail userDetail1 = new UserDetail();
		userDetail1.setId("110022");
		userDetail1.setRegid("10003");
		users.add(userDetail1);
		UserDetail userDetail2 = new UserDetail();
		userDetail2.setId("110024");
		userDetail2.setRegid("10003");
		users.add(userDetail2);
		UserDetail userDetail3 = new UserDetail();
		userDetail3.setId("110003");
		userDetail3.setRegid("10003");
		users.add(userDetail3);
		UserDetail userDetail4 = new UserDetail();
		userDetail4.setId("110002");
		userDetail4.setRegid("10003");
		users.add(userDetail4);		
		userDetailRepository.saveAll(users);
	}
	
	@Test
	public void testSuccessBiometricSave() {
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");	
		
		String response = masterSyncDaoImpl.saveSyncData(syncDataResponseDto);

		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@Test
	public void testSuccessSave() {
		String response=null;

		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");
		
		response = masterSyncDaoImpl.saveSyncData(syncDataResponseDto);
		
		assertEquals(RegistrationConstants.SUCCESS, response);
								
		MachineMaster machine = machineMasterRepository.findByIsActiveTrueAndNameAndRegMachineSpecIdLangCode("b2ml24784", "eng");
		 
		assertNotNull(machine);
		
		String keyIndexBasedOnMachineName = machineMappingDAO.getKeyIndexByMachineName("b2ml24784");
		assertNotNull(keyIndexBasedOnMachineName);
	}
	
	
	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {
		mockRegcenterUserMapping();
		
		ObjectMapper mapper = new ObjectMapper();
        SyncDataResponseDto syncDataResponseDto = null;
		
		try {
			syncDataResponseDto = mapper.readValue(new File(getClass().getClassLoader().getResource(fileName).getFile()), 
					SyncDataResponseDto.class);
					
		} catch (Exception e) {
			//it could throw exception for invalid json which is part of negative test case
			e.printStackTrace();
		}		
		return syncDataResponseDto;
	}

}
