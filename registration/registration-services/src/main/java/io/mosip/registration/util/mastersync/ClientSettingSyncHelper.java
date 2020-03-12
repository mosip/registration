package io.mosip.registration.util.mastersync;


import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.response.SyncDataBaseDto;
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
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.repositories.TemplateFileFormatRepository;
import io.mosip.registration.repositories.TemplateRepository;
import io.mosip.registration.repositories.TemplateTypeRepository;
import io.mosip.registration.repositories.TitleRepository;
import io.mosip.registration.repositories.ValidDocumentRepository;
import io.mosip.registration.util.mastersync.MetaDataUtils;

@Component
public class ClientSettingSyncHelper {
	
	private static final Logger LOGGER = AppConfig.getLogger(ClientSettingSyncHelper.class);
	
	private static final String ENTITY_PACKAGE_NAME = "io.mosip.registration.entity.";
	
	/** Object for Sync Biometric Attribute Repository. */
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;

	/** Object for Sync Biometric Type Repository. */
	@Autowired
	private BiometricTypeRepository biometricTypeRepository;

	/** Object for Sync Blacklisted Words Repository. */
	@Autowired
	private BlacklistedWordsRepository blacklistedWordsRepository;

	/** Object for Sync Device Repository. */
	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/** Object for Sync Device Specification Repository. */
	@Autowired
	private DeviceSpecificationRepository deviceSpecificationRepository;

	/** Object for Sync Device Type Repository. */
	@Autowired
	private DeviceTypeRepository deviceTypeRepository;

	/** Object for Sync Document Category Repository. */
	@Autowired
	private DocumentCategoryRepository documentCategoryRepository;

	/** Object for Sync Document Type Repository. */
	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	/** Object for Sync Gender Type Repository. */
	@Autowired
	private GenderRepository genderRepository;

	/** Object for Sync Id Type Repository. */
	@Autowired
	private IdTypeRepository idTypeRepository;

	/** Object for Sync Location Repository. */
	@Autowired
	private LocationRepository locationRepository;

	/** Object for Sync Machine Repository. */
	@Autowired
	private MachineMasterRepository machineRepository;

	/** Object for Sync Machine Specification Repository. */
	@Autowired
	private MachineSpecificationRepository machineSpecificationRepository;

	/** Object for Sync Machine Type Repository. */
	@Autowired
	private MachineTypeRepository machineTypeRepository;

	/** Object for Sync Reason Category Repository. */
	@Autowired
	private ReasonCategoryRepository reasonCategoryRepository;

	/** Object for Sync Reason List Repository. */
	@Autowired
	private ReasonListRepository reasonListRepository;

	/** Object for Sync Template File Format Repository. */
	@Autowired
	private TemplateFileFormatRepository templateFileFormatRepository;

	/** Object for Sync Template Repository. */
	@Autowired
	private TemplateRepository templateRepository;

	/** Object for Sync Template Type Repository. */
	@Autowired
	private TemplateTypeRepository templateTypeRepository;

	/** Object for Sync Title Repository. */
	@Autowired
	private TitleRepository titleRepository;

	/** Object for Sync Applicant Valid Document Repository. */
	@Autowired
	private ApplicantValidDocumentRepository applicantValidDocumentRepository;

	/** Object for Sync Valid Document Repository. */
	@Autowired
	private ValidDocumentRepository validDocumentRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private LanguageRepository languageRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterDeviceRepository registrationCenterDeviceRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterMachineDeviceRepository registrationCenterMachineDeviceRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterUserRepository registrationCenterUserRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private CenterMachineRepository centerMachineRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterRepository registrationCenterRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterTypeRepository registrationCenterTypeRepository;

	/** Object for Sync Individual type Repository. */
	@Autowired
	private IndividualTypeRepository individualTypeRepository;

	/** Object for Sync app authentication Repository. */
	@Autowired
	private AppAuthenticationRepository appAuthenticationRepository;

	/** Object for Sync app role Repository. */
	@Autowired
	private AppRolePriorityRepository appRolePriorityRepository;

	/** Object for Sync app detail Repository. */
	@Autowired
	private AppDetailRepository appDetailRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private ScreenAuthorizationRepository screenAuthorizationRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private ProcessListRepository processListRepository;

	/** Object for screen detail Repository. */
	@Autowired
	private ScreenDetailRepository screenDetailRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private SyncJobDefRepository syncJobDefRepository;
	
	/** Object for Registered device Repository. */
	@Autowired
	private RegisteredDeviceRepository registeredDeviceRepository;

	@Autowired
	private RegisteredDeviceTypeRepository registeredDeviceTypeRepository;
	
	@Autowired
	private RegisteredSubDeviceTypeRepository registeredSubDeviceTypeRepository;
	
	@Autowired
	private MosipDeviceServiceRepository mosipDeviceServiceRepository;
	
	@Autowired
	private FoundationalTrustProviderRepository foundationalTrustProviderRepository;
	
	@Autowired
	private DeviceProviderRepository deviceProviderRepository;

	/**
	 * Save the SyncDataResponseDto 
	 * 
	 * @param syncDataResponseDto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String saveClientSettings(SyncDataResponseDto syncDataResponseDto) throws RegBaseUncheckedException {
		try {
			handleDeviceSync(syncDataResponseDto);
			handleMachineSync(syncDataResponseDto);
			handleRegistrationCenterSync(syncDataResponseDto);
			handleAppDetailSync(syncDataResponseDto);
			handleTemplateSync(syncDataResponseDto);
			handleDocumentSync(syncDataResponseDto);
			handleIdSchemaPossibleValuesSync(syncDataResponseDto);
			handleMisellaneousSync(syncDataResponseDto);
			return RegistrationConstants.SUCCESS;
		} catch (Throwable e) {	
			throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_EXCEPTION + RegistrationConstants.FAILURE,
					e.getMessage());
		}
	}
	
	/**
	 * creating meta data for building the entities from SyncDataBaseDto
	 * 
	 * @param syncDataBaseDto
	 * @return
	 * @throws Exception
	 */
	private List buildEntities(SyncDataBaseDto syncDataBaseDto) throws SyncFailedException {	
		try {		
			List<Object> entities = new ArrayList<Object>();
			if(syncDataBaseDto == null||syncDataBaseDto.getData() == null || syncDataBaseDto.getData().isEmpty())
				return entities;
			for(String jsonString : syncDataBaseDto.getData()) {
				JSONObject jsonObject = new JSONObject(jsonString);	
				Object entity = MetaDataUtils.setCreateJSONObjectToMetaData(jsonObject, 
						Class.forName(ENTITY_PACKAGE_NAME + syncDataBaseDto.getEntityName()));
				entities.add(entity);
			}
			return entities;
		} catch (JSONException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
			throw new SyncFailedException(e.getMessage() + " building entities is failed...");
		}
	}
	
	private SyncDataBaseDto getSyncDataBaseDto(SyncDataResponseDto syncDataResponseDto, String entityName) throws Exception {
		SyncDataBaseDto syncDataBaseDto = syncDataResponseDto.getDataToSync().stream()
				.filter(obj -> obj.getEntityName() == entityName)
				.findAny()
				.orElse(null);
		 
		return syncDataBaseDto;
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private void handleDeviceSync(SyncDataResponseDto syncDataResponseDto) throws Exception{
		try {		
			deviceTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DeviceType")));
			deviceSpecificationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"DeviceSpecification")));
			deviceMasterRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"DeviceMaster")));
			registeredDeviceTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"RegisteredDeviceType")));
			registeredSubDeviceTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"RegisteredSubDeviceType")));
			mosipDeviceServiceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"MosipDeviceService")));
			deviceProviderRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"DeviceProvider")));
			registeredDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"RegisteredDeviceMaster")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the entities into machine sync is failed ");
		}		
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleMachineSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			machineTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineType")));
			machineSpecificationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineSpecification")));
			machineTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineType")));
			machineRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineMaster")));
		}  catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the entities into machine sync  is failed ");
		}	
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleRegistrationCenterSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			registrationCenterTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterType")));
			registrationCenterRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenter")));
			registrationCenterUserRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterUser")));
			registrationCenterDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterDevice")));
			registrationCenterMachineDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterMachine")));
			registrationCenterMachineDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterMachineDevice")));
		} catch (Exception e ) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into registration center sync is failed ");
		} 
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleAppDetailSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			appDetailRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppDetail")));
			appRolePriorityRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppRolePriority")));
			appAuthenticationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppAuthenticationMethod")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into app detail sync is failed ");
		}
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleTemplateSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException{	
		try {
			templateFileFormatRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "TemplateFileFormat")));
			templateTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "TemplateType")));
			templateRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Template")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into template sync is failed ");
		}
		
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleDocumentSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			documentTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentType")));
			documentCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentCategory")));
			applicantValidDocumentRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ApplicantValidDocument")));
			validDocumentRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ValidDocument")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into document sync is failed ");
		}
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleIdSchemaPossibleValuesSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			biometricTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricType")));
			biometricAttributeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricAttribute")));
			genderRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Gender")));
			idTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "IdType")));
			locationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Location")));
			titleRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Title")));
			individualTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "IndividualType")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into scheam possible value sync is failed ");
		}		
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@SuppressWarnings("unchecked")
	private void handleMisellaneousSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			blacklistedWordsRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BlacklistedWords")));
			processListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ProcessList")));
			screenDetailRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenDetail")));
			screenAuthorizationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenAuthorization")));
			foundationalTrustProviderRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "FoundationTrustProvider")));
			languageRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Language")));
			reasonCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ReasonCategory")));
			reasonListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricType")));
			syncJobDefRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "SyncJobDef")));
			
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the respective entities into  sync is failed ");
		}
	}
}
