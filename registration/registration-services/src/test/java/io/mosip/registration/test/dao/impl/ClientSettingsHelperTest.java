package io.mosip.registration.test.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.impl.MasterSyncDaoImpl;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.AppAuthenticationRepository;
import io.mosip.registration.repositories.AppDetailRepository;
import io.mosip.registration.repositories.AppRolePriorityRepository;
import io.mosip.registration.repositories.ApplicantValidDocumentRepository;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BiometricTypeRepository;
import io.mosip.registration.repositories.BlacklistedWordsRepository;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.DeviceMasterRepository;
import io.mosip.registration.repositories.DeviceProviderRepository;
import io.mosip.registration.repositories.DeviceSpecificationRepository;
import io.mosip.registration.repositories.DeviceTypeRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.FoundationalTrustProviderRepository;
import io.mosip.registration.repositories.GenderRepository;
import io.mosip.registration.repositories.IdTypeRepository;
import io.mosip.registration.repositories.IndividualTypeRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.MachineSpecificationRepository;
import io.mosip.registration.repositories.MachineTypeRepository;
import io.mosip.registration.repositories.MosipDeviceServiceRepository;
import io.mosip.registration.repositories.ProcessListRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.RegisteredDeviceRepository;
import io.mosip.registration.repositories.RegisteredDeviceTypeRepository;
import io.mosip.registration.repositories.RegisteredSubDeviceTypeRepository;
import io.mosip.registration.repositories.RegistrationCenterDeviceRepository;
import io.mosip.registration.repositories.RegistrationCenterMachineDeviceRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.repositories.RegistrationCenterTypeRepository;
import io.mosip.registration.repositories.RegistrationCenterUserRepository;
import io.mosip.registration.repositories.ScreenAuthorizationRepository;
import io.mosip.registration.repositories.ScreenDetailRepository;
import io.mosip.registration.repositories.SyncJobControlRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.repositories.TemplateFileFormatRepository;
import io.mosip.registration.repositories.TemplateRepository;
import io.mosip.registration.repositories.TemplateTypeRepository;
import io.mosip.registration.repositories.TitleRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.repositories.ValidDocumentRepository;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;
import io.mosip.registration.util.mastersync.MetaDataUtils;

@RunWith(PowerMockRunner.class)
@SpringBootTest
@PrepareForTest({ MetaDataUtils.class, RegBaseUncheckedException.class, SessionContext.class, MasterSyncDaoImpl.class,ClientSettingSyncHelper.class,BiometricAttributeRepository.class })
public class ClientSettingsHelperTest {
	

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Mock
	private SyncJobControlRepository syncStatusRepository;
	
	@Mock
	private BiometricAttributeRepository biometricAttributeRepository;
	
	@Mock
	private BiometricTypeRepository masterSyncBiometricTypeRepository;
	
	@Mock
	private BlacklistedWordsRepository masterSyncBlacklistedWordsRepository;
	
	@Mock
	private DeviceMasterRepository masterSyncDeviceRepository;
	
	@Mock
	private DeviceSpecificationRepository masterSyncDeviceSpecificationRepository;
	
	@Mock
	private DeviceTypeRepository masterSyncDeviceTypeRepository;
	
	@Mock
	private DocumentCategoryRepository masterSyncDocumentCategoryRepository;
	
	@Mock
	private DocumentTypeRepository masterSyncDocumentTypeRepository;
	
	@Mock
	private GenderRepository masterSyncGenderTypeRepository;
	
	@Mock
	private IdTypeRepository masterSyncIdTypeRepository;
	
	@Mock
	private LanguageRepository masterSyncLanguageRepository;
	
	@Mock
	private LocationRepository masterSyncLocationRepository;
	
	@Mock
	private MachineMasterRepository masterSyncMachineRepository;
	
	@Mock
	private MachineSpecificationRepository masterSyncMachineSpecificationRepository;
	
	@Mock
	private MachineTypeRepository masterSyncMachineTypeRepository;
	
	@Mock
	private ReasonCategoryRepository reasonCategoryRepository;
	
	@Mock
	private ReasonListRepository masterSyncReasonListRepository;
	
	@Mock
	private RegistrationCenterRepository masterSyncRegistrationCenterRepository;
	
	@Mock
	private RegistrationCenterTypeRepository masterSyncRegistrationCenterTypeRepository;
	
	@Mock
	private TemplateFileFormatRepository masterSyncTemplateFileFormatRepository;
	
	@Mock
	private TemplateRepository masterSyncTemplateRepository;
	
	@Mock
	private TemplateTypeRepository masterSyncTemplateTypeRepository;
	
	@Mock
	private TitleRepository masterSyncTitleRepository;
	
	@Mock
	private ApplicantValidDocumentRepository masterSyncValidDocumentRepository;
	
	@Mock
	private ValidDocumentRepository validDocumentRepository;
	
	@Mock
	private IndividualTypeRepository individualTypeRepository;
	
	@Mock
	private AppAuthenticationRepository appAuthenticationRepository;
	
	@Mock
	private AppRolePriorityRepository appRolePriorityRepository;
	
	@Mock
	private AppDetailRepository appDetailRepository;
	
	@Mock
	private ScreenAuthorizationRepository screenAuthorizationRepository;
	
	@Mock
	private ProcessListRepository processListRepository;
	
	@Mock
	private RegistrationCenterDeviceRepository registrationCenterDeviceRepository;
	
	@Mock
	private RegistrationCenterMachineDeviceRepository registrationCenterMachineDeviceRepository;

	
	@Mock
	private UserMachineMappingRepository userMachineMappingRepository;

	
	@Mock
	private RegistrationCenterUserRepository registrationCenterUserRepository;

	
	@Mock
	private CenterMachineRepository centerMachineRepository;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;


	@Mock
	private RegistrationCenterTypeRepository registrationCenterTypeRepository;
	
	
	@Mock
	private ScreenDetailRepository screenDetailRepository;

	@Mock
	private SyncJobDefRepository syncJobDefRepository;
	
	@Mock
	private RegisteredDeviceRepository registeredDeviceRepository;
	
	@Mock
	private RegisteredDeviceTypeRepository registeredDeviceTypeRepository;
	
	@Mock
	private RegisteredSubDeviceTypeRepository registeredSubDeviceTypeRepository;
	
	@Mock
	private MosipDeviceServiceRepository mosipDeviceServiceRepository;
	
	@Mock
	private FoundationalTrustProviderRepository foundationalTrustProviderRepository;
	
	@Mock
	private DeviceProviderRepository deviceProviderRepository;
		
	@InjectMocks
	private ClientSettingSyncHelper clientSettingSyncHelper;
	
	
	
	
	
	@SuppressWarnings("unchecked")
	@Test()
	public void testSingleEntity() {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");
		response = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testEmptyJsonRegBaseUncheckedException()  {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("emptyJson.json");
			clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testClientSettingsSyncForValidJson() {
        
        String response = null;
        SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");
		response= clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testInvalidJsonSyntaxJsonSyntaxException(){
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("invalidJson.json");
		clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);	
	}
	
	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {
		
		ObjectMapper mapper = new ObjectMapper();
        SyncDataResponseDto syncDataResponseDto = null;
		
			try {
				syncDataResponseDto = mapper.readValue(
						new File(getClass().getClassLoader().getResource(fileName).getFile()),SyncDataResponseDto.class);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return syncDataResponseDto;
	}

}
