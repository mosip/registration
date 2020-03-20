package io.mosip.registration.test.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BiometricTypeRepository;
import io.mosip.registration.test.config.TestDaoConfig;


/**
 * @author anusha
 *
 */
@RunWith(SpringRunner.class)
//@ContextConfiguration(classes= {PropertiesConfig.class, ApplicationContextProvider.class, AppConfig.class, TestDaoConfig.class})
@ContextConfiguration(classes= {TestDaoConfig.class})
//@SpringBootTest(classes= {PropertiesConfig.class, ApplicationContextProvider.class, AppConfig.class, DaoConfig.class})
//@PrepareForTest({ ApplicationContext.class, SessionContext.class })
public class MasterSyncDaoImplNonMockTest {
	
	@Autowired
	private MasterSyncDao masterSyncDaoImpl;
	
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;
	
	@Autowired
	private BiometricTypeRepository biometricTypeRepository;
	
	
	@BeforeClass
	public static void setup() throws Exception {		
		
	  Map<String, Object> appMap = new HashMap<String,Object>();
	  appMap.put(RegistrationConstants.DOC_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.FINGERPRINT_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.IRIS_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.FACE_DISABLE_FLAG, "Y");
	  appMap.put(RegistrationConstants.PRIMARY_LANGUAGE, "ara");
	  appMap.put(RegistrationConstants.SECONDARY_LANGUAGE, "fra");
	  appMap.put(RegistrationConstants.TPM_AVAILABILITY, RegistrationConstants.DISABLE);
	  //ApplicationContext.getInstance().setApplicationMap(appMap);
	  ApplicationContext.getInstance().loadResourceBundle();		 
		
	}
	
	@Test
	public void testSuccessBiometricSave() {
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");	
		
		assertEquals(2, syncDataResponseDto.getDataToSync().size());
		assertEquals(2, syncDataResponseDto.getDataToSync().get(0).getData().size());
		assertEquals(2, syncDataResponseDto.getDataToSync().get(1).getData().size());
			
		String response = masterSyncDaoImpl.saveSyncData(syncDataResponseDto);
		
		assertEquals(2, biometricTypeRepository.count());
		assertEquals(2, biometricAttributeRepository.count());
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@Test
	public void testSuccessSave() {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");	
				
		response = masterSyncDaoImpl.saveSyncData(syncDataResponseDto);
		
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	
	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {		
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
