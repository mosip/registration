package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ibm.icu.impl.Assert;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dao.impl.MasterSyncDaoImpl;
import io.mosip.registration.dto.ApplicantValidDocumentDto;
import io.mosip.registration.dto.IndividualTypeDto;
import io.mosip.registration.dto.mastersync.AppAuthenticationMethodDto;
import io.mosip.registration.dto.mastersync.AppDetailDto;
import io.mosip.registration.dto.mastersync.AppRolePriorityDto;
import io.mosip.registration.dto.mastersync.ApplicationDto;
import io.mosip.registration.dto.mastersync.BiometricAttributeDto;
import io.mosip.registration.dto.mastersync.BiometricTypeDto;
import io.mosip.registration.dto.mastersync.BlacklistedWordsDto;
import io.mosip.registration.dto.mastersync.DeviceDto;
import io.mosip.registration.dto.mastersync.DeviceSpecificationDto;
import io.mosip.registration.dto.mastersync.DeviceTypeDto;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.mastersync.DocumentTypeDto;
import io.mosip.registration.dto.mastersync.GenderDto;
import io.mosip.registration.dto.mastersync.HolidayDto;
import io.mosip.registration.dto.mastersync.IdTypeDto;
import io.mosip.registration.dto.mastersync.LanguageDto;
import io.mosip.registration.dto.mastersync.LocationDto;
import io.mosip.registration.dto.mastersync.MachineDto;
import io.mosip.registration.dto.mastersync.MachineSpecificationDto;
import io.mosip.registration.dto.mastersync.MachineTypeDto;
import io.mosip.registration.dto.mastersync.MasterDataResponseDto;
import io.mosip.registration.dto.mastersync.PostReasonCategoryDto;
import io.mosip.registration.dto.mastersync.ProcessListDto;
import io.mosip.registration.dto.mastersync.ReasonListDto;
import io.mosip.registration.dto.mastersync.RegisteredDeviceMasterDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterDeviceDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterMachineDeviceDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterMachineDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterTypeDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterUserDto;
import io.mosip.registration.dto.mastersync.RegistrationCenterUserMachineMappingDto;
import io.mosip.registration.dto.mastersync.ScreenAuthorizationDto;
import io.mosip.registration.dto.mastersync.ScreenDetailDto;
import io.mosip.registration.dto.mastersync.SyncJobDefDto;
import io.mosip.registration.dto.mastersync.TemplateDto;
import io.mosip.registration.dto.mastersync.TemplateFileFormatDto;
import io.mosip.registration.dto.mastersync.TemplateTypeDto;
import io.mosip.registration.dto.mastersync.TitleDto;
import io.mosip.registration.dto.response.SyncDataBaseDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.AppAuthenticationMethod;
import io.mosip.registration.entity.AppDetail;
import io.mosip.registration.entity.AppRolePriority;
import io.mosip.registration.entity.ApplicantValidDocument;
import io.mosip.registration.entity.BiometricAttribute;
import io.mosip.registration.entity.BiometricType;
import io.mosip.registration.entity.BlacklistedWords;
import io.mosip.registration.entity.DocumentCategory;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Gender;
import io.mosip.registration.entity.IdType;
import io.mosip.registration.entity.IndividualType;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.MachineType;
import io.mosip.registration.entity.ProcessList;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.RegDeviceMaster;
import io.mosip.registration.entity.RegDeviceSpec;
import io.mosip.registration.entity.RegDeviceType;
import io.mosip.registration.entity.RegisteredDeviceMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.RegistrationCenterType;
import io.mosip.registration.entity.RegistrationCommonFields;
import io.mosip.registration.entity.ScreenAuthorization;
import io.mosip.registration.entity.ScreenDetail;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.entity.Template;
import io.mosip.registration.entity.TemplateEmbeddedKeyCommonFields;
import io.mosip.registration.entity.TemplateFileFormat;
import io.mosip.registration.entity.TemplateType;
import io.mosip.registration.entity.Title;
import io.mosip.registration.entity.ValidDocument;
import io.mosip.registration.entity.id.AppRolePriorityId;
import io.mosip.registration.entity.id.ApplicantValidDocumentID;
import io.mosip.registration.entity.id.CodeAndLanguageCodeID;
import io.mosip.registration.entity.id.IndividualTypeId;
import io.mosip.registration.entity.id.RegDeviceTypeId;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.entity.id.ValidDocumentID;
import io.mosip.registration.exception.RegBaseCheckedException;
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
import io.mosip.registration.service.sync.impl.MasterSyncServiceImpl;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;

import io.mosip.registration.util.mastersync.MapperUtils;
import io.mosip.registration.util.mastersync.MetaDataUtils;

/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@SpringBootTest
@PrepareForTest({ MetaDataUtils.class, RegBaseUncheckedException.class, SessionContext.class, BiometricAttributeRepository.class })
public class MasterSyncDaoImplTest {

	// private MapperFacade mapperFacade = CustomObjectMapper.MAPPER_FACADE;

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

	/** Object for Sync language Repository. */
	@Mock
	private RegistrationCenterDeviceRepository registrationCenterDeviceRepository;

	/** Object for Sync language Repository. */
	@Mock
	private RegistrationCenterMachineDeviceRepository registrationCenterMachineDeviceRepository;

	/** Object for Sync language Repository. */
	@Mock
	private UserMachineMappingRepository userMachineMappingRepository;

	/** Object for Sync language Repository. */
	@Mock
	private RegistrationCenterUserRepository registrationCenterUserRepository;

	/** Object for Sync language Repository. */
	@Mock
	private CenterMachineRepository centerMachineRepository;

	/** Object for Sync language Repository. */
	@Mock
	private RegistrationCenterRepository registrationCenterRepository;

	/** Object for Sync language Repository. */
	@Mock
	private RegistrationCenterTypeRepository registrationCenterTypeRepository;
	
	/** Object for screen detail Repository. */
	@Mock
	private ScreenDetailRepository screenDetailRepository;
	
	/** Object for Sync screen auth Repository. */
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


	@Mock
	private MasterSyncDao masterSyncDao;
	
	@Mock
	private MetaDataUtils metaDataUtils;
	
	@Mock
	private MapperUtils mapperUtils;
	
	@InjectMocks
	private MasterSyncServiceImpl masterSyncServiceImpl;
	
	@Mock
	private ClientSettingSyncHelper clientSettingSyncHelper;
	
	@InjectMocks
	private MasterSyncDaoImpl masterSyncDaoImpl;
	
	@Before
	public void initialize() throws Exception {
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
	}

	@BeforeClass
	public static void beforeClass() {

		List<RegistrationCenterType> registrationCenterType = new ArrayList<>();
		RegistrationCenterType MasterRegistrationCenterType = new RegistrationCenterType();
		MasterRegistrationCenterType.setCode("T1011");
		MasterRegistrationCenterType.setName("ENG");
		MasterRegistrationCenterType.setLangCode("Main");
		registrationCenterType.add(MasterRegistrationCenterType);
	}

	@Test
	public void testMasterSyncDaoSucess() throws RegBaseCheckedException {

		SyncControl masterSyncDetails = new SyncControl();

		masterSyncDetails.setSyncJobId("MDS_J00001");
		masterSyncDetails.setLastSyncDtimes(new Timestamp(System.currentTimeMillis()));
		masterSyncDetails.setCrBy("mosip");
		masterSyncDetails.setIsActive(true);
		masterSyncDetails.setLangCode("eng");
		masterSyncDetails.setCrDtime(new Timestamp(System.currentTimeMillis()));

		Mockito.when(syncStatusRepository.findBySyncJobId(Mockito.anyString())).thenReturn(masterSyncDetails);

		masterSyncDaoImpl.syncJobDetails("MDS_J00001");

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMasterSyncExceptionThrown() throws RegBaseUncheckedException {

		try {
			Mockito.when(masterSyncDaoImpl.syncJobDetails(Mockito.anyString()))
					.thenThrow(RegBaseUncheckedException.class);
			masterSyncDaoImpl.syncJobDetails("MDS_J00001");
		} catch (Exception exception) {

		}
	}

	@Test
	public void findLocationByLangCode() throws RegBaseCheckedException {

		List<Location> locations = new ArrayList<>();
		Location locattion = new Location();
		locattion.setCode("LOC01");
		locattion.setName("english");
		locattion.setLangCode("ENG");
		locattion.setHierarchyLevel(1);
		locattion.setHierarchyName("english");
		locattion.setParentLocCode("english");
		locations.add(locattion);

		Mockito.when(masterSyncLocationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(locations);

		masterSyncDaoImpl.findLocationByLangCode(1, "ENG");

		assertTrue(locations != null);
	}

	@Test
	public void findLocationByParentLocCode() throws RegBaseCheckedException {

		List<Location> locations = new ArrayList<>();
		Location locattion = new Location();
		locattion.setCode("LOC01");
		locattion.setName("english");
		locattion.setLangCode("ENG");
		locattion.setHierarchyLevel(1);
		locattion.setHierarchyName("english");
		locattion.setParentLocCode("english");
		locations.add(locattion);

		Mockito.when(masterSyncLocationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(locations);

		masterSyncDaoImpl.findLocationByParentLocCode("TPT", "eng");

		assertTrue(locations != null);
	}

	@Test
	public void findAllReason() throws RegBaseCheckedException {

		List<ReasonCategory> allReason = new ArrayList<>();
		ReasonCategory reasons = new ReasonCategory();
		reasons.setCode("DEMO");
		reasons.setName("InvalidData");
		reasons.setLangCode("FRE");
		allReason.add(reasons);

		Mockito.when(reasonCategoryRepository.findByIsActiveTrueAndLangCode(Mockito.anyString())).thenReturn(allReason);

		masterSyncDaoImpl.getAllReasonCatogery(Mockito.anyString());

		assertTrue(allReason != null);
	}

	@Test
	public void findAllReasonList() throws RegBaseCheckedException {

		List<String> reasonCat = new ArrayList<>();
		List<ReasonList> allReason = new ArrayList<>();
		ReasonList reasons = new ReasonList();
		reasons.setCode("DEMO");
		reasons.setName("InvalidData");
		reasons.setLangCode("FRE");
		allReason.add(reasons);

		Mockito.when(masterSyncReasonListRepository
				.findByIsActiveTrueAndLangCodeAndReasonCategoryCodeIn(Mockito.anyString(), Mockito.anyList()))
				.thenReturn(allReason);

		masterSyncDaoImpl.getReasonList("FRE", reasonCat);

		assertTrue(allReason != null);
	}

	@Test
	public void findBlackWords() throws RegBaseCheckedException {

		List<BlacklistedWords> allBlackWords = new ArrayList<>();
		BlacklistedWords blackWord = new BlacklistedWords();
		blackWord.setWord("asdfg");
		blackWord.setDescription("asdfg");
		blackWord.setLangCode("ENG");
		allBlackWords.add(blackWord);
		allBlackWords.add(blackWord);

		Mockito.when(
				masterSyncBlacklistedWordsRepository.findBlackListedWordsByIsActiveTrueAndLangCode(Mockito.anyString()))
				.thenReturn(allBlackWords);

		masterSyncDaoImpl.getBlackListedWords("ENG");

		assertTrue(allBlackWords != null);
	}

	@Test
	public void findDocumentCategories() throws RegBaseCheckedException {

		List<DocumentType> documents = new ArrayList<>();
		DocumentType document = new DocumentType();
		document.setName("Aadhar");
		document.setDescription("Aadhar card");
		document.setLangCode("ENG");
		documents.add(document);
		documents.add(document);
		List<String> validDocuments = new ArrayList<>();
		validDocuments.add("MNA");
		validDocuments.add("CLR");
		Mockito.when(masterSyncDao.getDocumentTypes(Mockito.anyList(), Mockito.anyString())).thenReturn(documents);

		masterSyncDaoImpl.getDocumentTypes(validDocuments, "test");

		assertTrue(documents != null);

	}

	@Test
	public void findGenders() throws RegBaseCheckedException {

		List<Gender> genderList = new ArrayList<>();
		Gender gender = new Gender();
		gender.setCode("1");
		gender.setGenderName("male");
		gender.setLangCode("ENG");
		gender.setIsActive(true);
		genderList.add(gender);

		Mockito.when(masterSyncDao.getGenderDtls(Mockito.anyString())).thenReturn(genderList);

		masterSyncDaoImpl.getGenderDtls("ENG");

		assertTrue(genderList != null);

	}

	@Test
	public void findValidDoc() {

		List<ValidDocument> docList = new ArrayList<>();
		ValidDocument docs = new ValidDocument();
		ValidDocumentID validDocumentId = new ValidDocumentID();
		validDocumentId.setDocCategoryCode("D101");
		validDocumentId.setDocTypeCode("DC101");
		docs.setLangCode("eng");
		docList.add(docs);

		Mockito.when(masterSyncDao.getValidDocumets(Mockito.anyString())).thenReturn(docList);

		masterSyncDaoImpl.getValidDocumets("POA");

		assertTrue(docList != null);

	}

	@Test
	public void individualTypes() {

		List<IndividualType> masterIndividualType = new ArrayList<>();
		IndividualType individualTypeEntity = new IndividualType();
		IndividualTypeId individualTypeId = new IndividualTypeId();
		individualTypeId.setCode("NFR");
		individualTypeId.setLangCode("eng");
		individualTypeEntity.setIndividualTypeId(individualTypeId);
		individualTypeEntity.setName("National");
		individualTypeEntity.setIsActive(true);
		masterIndividualType.add(individualTypeEntity);

		Mockito.when(masterSyncDao.getIndividulType(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(masterIndividualType);

		masterSyncDaoImpl.getIndividulType("NFR", "eng");

		assertTrue(masterIndividualType != null);

	}
	
	@Test
	public void getBiometricType() {
		
		List<String> biometricType = new LinkedList<>(Arrays.asList(RegistrationConstants.FNR, RegistrationConstants.IRS));
		List<BiometricAttribute> biometricAttributes = new ArrayList<>();
		BiometricAttribute biometricAttribute = new BiometricAttribute();
		biometricAttribute.setCode("RS");
		biometricAttribute.setBiometricTypeCode("FNR");
		biometricAttribute.setName("Right Slap");
		biometricAttribute.setLangCode("eng");
		biometricAttributes.add(biometricAttribute);
		
		Mockito.when(biometricAttributeRepository.findByLangCodeAndBiometricTypeCodeIn("eng",biometricType)).thenReturn(biometricAttributes);
		assertNotNull(masterSyncDaoImpl.getBiometricType("eng", biometricType));
		
	}
	
	@SuppressWarnings("unchecked")
	@Test()
	public void testSingleEntity() {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenReturn(RegistrationConstants.SUCCESS);
		response= masterSyncDaoImpl.saveSyncData(syncDataResponseDto);		
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testClientSettingsSyncForJson() {
		
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenReturn(RegistrationConstants.SUCCESS);
		response= masterSyncDaoImpl.saveSyncData(syncDataResponseDto);		
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
		
	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testInvalidJsonSyntaxJsonSyntaxException() {		
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("invalidJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenThrow(JsonSyntaxException.class);
		masterSyncDaoImpl.saveSyncData(syncDataResponseDto);		
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testEmptyJsonRegBaseUncheckedException() {		
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("emptyJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenThrow(RegBaseUncheckedException.class);
		masterSyncDaoImpl.saveSyncData(syncDataResponseDto);			
	}
	
	
	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {
		
		ObjectMapper mapper = new ObjectMapper();
        SyncDataResponseDto syncDataResponseDto = null;
		
			try {
				syncDataResponseDto = mapper.readValue(
						new File(getClass().getClassLoader().getResource(fileName).getFile()),SyncDataResponseDto.class);
			} catch (Exception e) {
				//it could throw exception for invalid json which is part of negative test case
			} 
		
		return syncDataResponseDto;
	}

}
