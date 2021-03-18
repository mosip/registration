package io.mosip.registration.util.mastersync;


import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCHEMA_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.*;

import java.io.IOException;
import java.io.SyncFailedException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.mastersync.DynamicFieldDto;
import io.mosip.registration.dto.response.SyncDataBaseDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.DynamicField;
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
import io.mosip.registration.repositories.DynamicFieldRepository;
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
import org.springframework.web.client.HttpClientErrorException;

@Component
public class ClientSettingSyncHelper {
	
	private static final Logger LOGGER = AppConfig.getLogger(ClientSettingSyncHelper.class);
	
	private static final String ENTITY_PACKAGE_NAME = "io.mosip.registration.entity.";	
	private static final String FIELD_TYPE_DYNAMIC = "dynamic";
		
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
	
	@Autowired
	private DynamicFieldRepository dynamicFieldRepository;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private ServiceDelegateUtil serviceDelegateUtil;

	@Autowired
	private IdentitySchemaDao identitySchemaDao;
		
	private static final Map<String, String> ENTITY_CLASS_NAMES = new HashMap<String, String>();
	
	
	static {
		ENTITY_CLASS_NAMES.put("Device", ENTITY_PACKAGE_NAME + "RegDeviceMaster");
		ENTITY_CLASS_NAMES.put("DeviceType", ENTITY_PACKAGE_NAME + "RegDeviceType");
		ENTITY_CLASS_NAMES.put("DeviceSpecification", ENTITY_PACKAGE_NAME + "RegDeviceSpec");
		ENTITY_CLASS_NAMES.put("MachineSpecification", ENTITY_PACKAGE_NAME + "RegMachineSpec");
		ENTITY_CLASS_NAMES.put("RegistrationCenterDevice", ENTITY_PACKAGE_NAME + "RegCenterDevice");
		ENTITY_CLASS_NAMES.put("RegistrationCenterUser", ENTITY_PACKAGE_NAME + "RegCenterUser");
		ENTITY_CLASS_NAMES.put("RegistrationCenterMachine", ENTITY_PACKAGE_NAME + "CenterMachine");
		ENTITY_CLASS_NAMES.put("RegistrationCenterMachineDevice", ENTITY_PACKAGE_NAME + "RegCentreMachineDevice");
		ENTITY_CLASS_NAMES.put("RegistrationDeviceMaster", ENTITY_PACKAGE_NAME + "RegDeviceMaster");
		ENTITY_CLASS_NAMES.put("DeviceService", ENTITY_PACKAGE_NAME + "MosipDeviceService");
		ENTITY_CLASS_NAMES.put("DeviceTypeDPM", ENTITY_PACKAGE_NAME + "RegisteredDeviceType");
		ENTITY_CLASS_NAMES.put("DeviceSubTypeDPM", ENTITY_PACKAGE_NAME + "RegisteredSubDeviceType");
		ENTITY_CLASS_NAMES.put("RegisteredDevice", ENTITY_PACKAGE_NAME + "RegisteredDeviceMaster");
		ENTITY_CLASS_NAMES.put("Machine", ENTITY_PACKAGE_NAME + "MachineMaster");
		ENTITY_CLASS_NAMES.put("RegistrationCenterUserMachine", ENTITY_PACKAGE_NAME + "UserMachineMapping");
	}
	

	/**
	 * Save the SyncDataResponseDto 
	 * 
	 * @param syncDataResponseDto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String saveClientSettings(SyncDataResponseDto syncDataResponseDto) throws RegBaseUncheckedException {
		long start = System.currentTimeMillis();
		try {
			List<CompletableFuture> futures = new ArrayList<CompletableFuture>();
			futures.add(handleDeviceSync(syncDataResponseDto));
			futures.add(handleMachineSync(syncDataResponseDto));
			futures.add(handleRegistrationCenterSync(syncDataResponseDto));
			futures.add(handleAppDetailSync(syncDataResponseDto));
			futures.add(handleTemplateSync(syncDataResponseDto));
			futures.add(handleDocumentSync(syncDataResponseDto));
			futures.add(handleIdSchemaPossibleValuesSync(syncDataResponseDto));
			futures.add(handleMisellaneousSync1(syncDataResponseDto));
			futures.add(handleMisellaneousSync2(syncDataResponseDto));
			futures.add(handleDynamicFieldSync(syncDataResponseDto));
			futures.add(syncSchema("System"));

			CompletableFuture array [] = new CompletableFuture[futures.size()];
			CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(array));

			try {
				future.join();
			} catch (CompletionException e) {
				throw e.getCause();
			}

			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Complete master sync completed in (ms) : " + (System.currentTimeMillis() - start));
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
			if(syncDataBaseDto == null || syncDataBaseDto.getData() == null || syncDataBaseDto.getData().isEmpty())
				return entities;

			LOGGER.debug(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Building entity of type : " +
					syncDataBaseDto.getEntityName());

			byte[] data = clientCryptoFacade.decrypt(CryptoUtil.decodeBase64(syncDataBaseDto.getData()));
			JSONArray jsonArray = new JSONArray(new String(data));

			for(int i =0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = new JSONObject(jsonArray.getString(i));
				Object entity = MetaDataUtils.setCreateJSONObjectToMetaData(jsonObject, getEntityClass(syncDataBaseDto.getEntityName()));
				entities.add(entity);
			}

			return entities;
		} catch (Throwable e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			throw new SyncFailedException("Building entities is failed..." + e.getMessage());
		}
	}
	
	private SyncDataBaseDto getSyncDataBaseDto(SyncDataResponseDto syncDataResponseDto, String entityName) throws Exception {
		SyncDataBaseDto syncDataBaseDto = syncDataResponseDto.getDataToSync().stream()
				.filter(obj -> entityName.equals(obj.getEntityName()) && !obj.getEntityType().equalsIgnoreCase(FIELD_TYPE_DYNAMIC))
				.findAny()
				.orElse(null);
		 
		return syncDataBaseDto;
	}
	
	private Class getEntityClass(String entityName) throws ClassNotFoundException {
		try {
			
			return ENTITY_CLASS_NAMES.containsKey(entityName) ? Class.forName(ENTITY_CLASS_NAMES.get(entityName)) : 
				Class.forName(ENTITY_PACKAGE_NAME + entityName);
			
		} catch(ClassNotFoundException ex) {
			return Class.forName(ENTITY_PACKAGE_NAME + "Reg" + entityName);
		}
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws Exception 
	 */
	@Async
	private CompletableFuture<Boolean> handleDeviceSync(SyncDataResponseDto syncDataResponseDto) throws Exception {
		try {		
			deviceTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DeviceType")));
			deviceSpecificationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"DeviceSpecification")));
			deviceMasterRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto,"Device")));
			foundationalTrustProviderRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "FoundationalTrustProvider")));
		} catch (Exception e) {
			throw new SyncFailedException(e.getMessage()+"Saving the entities into machine sync is failed ");
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMachineSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			machineTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineType")));
			machineSpecificationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineSpecification")));
			machineRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Machine")));
		}  catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Machine data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleRegistrationCenterSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			registrationCenterTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterType")));
			registrationCenterRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenter")));			
			registrationCenterDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterDevice")));
			centerMachineRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterMachine")));
			registrationCenterMachineDeviceRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterMachineDevice")));
			registrationCenterUserRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterUser")));
		} catch (Exception e ) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("RegistrationCenter data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleAppDetailSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			appDetailRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppDetail")));
			appRolePriorityRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppRolePriority")));
			appAuthenticationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppAuthenticationMethod")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("AppDetail data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleTemplateSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException{
		try {
			templateFileFormatRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "TemplateFileFormat")));
			templateTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "TemplateType")));
			templateRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Template")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Template data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleDocumentSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			documentTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentType")));
			documentCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentCategory")));
			applicantValidDocumentRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ApplicantValidDocument")));
			validDocumentRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ValidDocument")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Document data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleIdSchemaPossibleValuesSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			biometricTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricType")));
			biometricAttributeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricAttribute")));
			genderRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Gender")));
			idTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "IdType")));
			locationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Location")));
			titleRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Title")));
			individualTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "IndividualType")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("IdSchema data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMisellaneousSync1(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			blacklistedWordsRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BlacklistedWords")));
			processListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ProcessList")));
			screenDetailRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenDetail")));
			screenAuthorizationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenAuthorization")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Miscellaneous data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMisellaneousSync2(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			languageRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Language")));
			reasonCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ReasonCategory")));
			reasonListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ReasonList")));
			syncJobDefRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "SyncJobDef")));
		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Miscellaneous data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save dynamic fields with value json
	 * @param syncDataResponseDto
	 */
	@Async
	private CompletableFuture handleDynamicFieldSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			Iterator<SyncDataBaseDto> iterator = syncDataResponseDto.getDataToSync().stream()
					.filter(obj -> FIELD_TYPE_DYNAMIC.equalsIgnoreCase(obj.getEntityType()))
					.iterator();
			
			List<DynamicField> fields = new ArrayList<DynamicField>();
			while(iterator.hasNext()) {
				SyncDataBaseDto syncDataBaseDto = iterator.next();
				
				if(syncDataBaseDto != null && syncDataBaseDto.getData() != null && !syncDataBaseDto.getData().isEmpty()) {
					byte[] data = clientCryptoFacade.decrypt(CryptoUtil.decodeBase64(syncDataBaseDto.getData()));
					JSONArray jsonArray = new JSONArray(new String(data));

					for(int i=0; i< jsonArray.length(); i++) {
						DynamicFieldDto dynamicFieldDto = MapperUtils.convertJSONStringToDto(jsonArray.getString(i),
								new TypeReference<DynamicFieldDto>() {});
						DynamicField dynamicField = new DynamicField();
						dynamicField.setId(dynamicFieldDto.getId());
						dynamicField.setDataType(dynamicFieldDto.getDataType());
						dynamicField.setName(dynamicFieldDto.getName());
						dynamicField.setLangCode(dynamicFieldDto.getLangCode());
						dynamicField.setValueJson(dynamicFieldDto.getFieldVal() == null ?
								"[]" : MapperUtils.convertObjectToJsonString(dynamicFieldDto.getFieldVal()));
						dynamicField.setActive(dynamicFieldDto.isActive());
						fields.add(dynamicField);
					}
				}
			}
			
			if (!fields.isEmpty()) {
				dynamicFieldRepository.saveAll(fields);
			}
				
		} catch(IOException e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
			throw new SyncFailedException("Dynamic field sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	private void checkForDuplicates(List<DynamicField> fields, List<DynamicField> existingFields) {
		for (DynamicField tobeUpdatedField : fields) {
			for (DynamicField existingField : existingFields) {
				if(tobeUpdatedField.getName().equalsIgnoreCase(existingField.getName())) {
					dynamicFieldRepository.delete(existingField);
				}
			}
		}
	}

	@Async
	public CompletableFuture syncSchema(String triggerPoint) throws RegBaseCheckedException, SyncFailedException {
		LOGGER.info(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID, "ID Schema sync started .....");

		if (RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			try {
				LinkedHashMap<String, Object> syncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(
						RegistrationConstants.ID_SCHEMA_SYNC_SERVICE, new HashMap<String, String>(), true,
						triggerPoint);

				if (null != syncResponse.get(RegistrationConstants.RESPONSE)) {
					LOGGER.info(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID,
							"ID Schema sync fetched from server.");

					String jsonString = MapperUtils
							.convertObjectToJsonString(syncResponse.get(RegistrationConstants.RESPONSE));
					SchemaDto schemaDto = MapperUtils.convertJSONStringToDto(jsonString,
							new TypeReference<SchemaDto>() {});

					identitySchemaDao.createIdentitySchema(schemaDto);
				} else {
					throw new SyncFailedException("Schema sync failed");
				}

			} catch (Exception e) {
				LOGGER.error(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
				throw new SyncFailedException("Schema sync failed due to " +  e.getMessage());
			}
		} else
			throw new SyncFailedException(RegistrationConstants.NO_INTERNET);

		return CompletableFuture.completedFuture(true);
	}
}
