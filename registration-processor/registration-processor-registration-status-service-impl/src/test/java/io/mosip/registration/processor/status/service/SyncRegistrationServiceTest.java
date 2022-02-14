package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import io.mosip.kernel.core.util.CryptoUtil;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernateErrorCode;
import io.mosip.kernel.idvalidator.rid.constant.RidExceptionProperty;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.decryptor.Decryptor;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.LostRidDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationSyncRequestDTO;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailureV2Dto;
import io.mosip.registration.processor.status.dto.SyncResponseSuccessDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.encryptor.Encryptor;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.EncryptionFailureException;
import io.mosip.registration.processor.status.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;

/**
 * The Class SyncRegistrationServiceTest.
 * 
 * @author Ranjitha Siddegowda
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({JsonUtils.class, CryptoUtil.class})
public class SyncRegistrationServiceTest {

	/** The sync registration dto. */
	private SyncRegistrationDto syncRegistrationDto;

	/** The sync registration dto. */
	private SyncRegistrationDto syncRegistrationDto1;

	/** The sync registration dto. */
	private SyncRegistrationDto syncRegistrationDto2;
	private SyncRegistrationDto syncRegistrationDto3;
	private SyncRegistrationDto syncRegistrationDto4;
	private SyncRegistrationDto syncRegistrationDto5;
	private SyncRegistrationDto syncRegistrationDto6;
	private SyncRegistrationDto syncRegistrationDto13;

	/** The sync registration entity. */
	private SyncRegistrationEntity syncRegistrationEntity;

	/** The entities. */
	private List<SyncRegistrationDto> entities;

	/** The entities. */
	private List<SyncRegistrationEntity> syncRegistrationEntities;

	/** The sync registration dao. */
	@Mock
	private SyncRegistrationDao syncRegistrationDao;

	/** The sync response dto. */
	@Mock
	private SyncResponseSuccessDto syncResponseDto;
	/** The anonymousProfileService */
	@Mock
	private AnonymousProfileService anonymousProfileService;
	
	@Mock
	private RegistrationUtility registrationUtility;

	/** The ridValidator. */
	@Mock
	private RidValidator<String> ridValidator;

	/** The sync registration service. */
	@InjectMocks
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService = new SyncRegistrationServiceImpl();

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private Decryptor decryptor;
	
	@Mock
	private Encryptor encryptor;

	RegistrationSyncRequestDTO registrationSyncRequestDTO;

	@Mock
	Environment env;

	@Mock
	LogDescription description;
	
	@Mock
	RestApiClient restApiClient;

	@Mock
	RestTemplate restTemplate;

	@Mock
	ObjectMapper mapper;

	/**
	 * Setup.
	 * @throws Exception 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() throws Exception {
		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.decodePlainBase64(anyString())).thenReturn("mosip".getBytes());
		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(anyString())).thenReturn("mosip".getBytes());
		registrationSyncRequestDTO = new RegistrationSyncRequestDTO();
		entities = new ArrayList<>();
		syncRegistrationEntities = new ArrayList<>();
		Mockito.doNothing().when(description).setMessage(any());

		syncRegistrationDto = new SyncRegistrationDto();

		syncRegistrationDto.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto.setLangCode("ENGLISH");
		syncRegistrationDto.setIsActive(true);
		syncRegistrationDto.setIsDeleted(false);
		syncRegistrationDto.setPacketHashValue("ab123");
		syncRegistrationDto.setSupervisorStatus("APPROVED");
		syncRegistrationDto.setSyncType(SyncTypeDto.NEW.getValue());
		syncRegistrationDto.setName("mosip");
		syncRegistrationDto.setPhone("1234567890");
		syncRegistrationDto.setEmail("mosip1@gmail.com");

		syncRegistrationDto1 = new SyncRegistrationDto();

		syncRegistrationDto1.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto1.setLangCode("eng");

		syncRegistrationDto1.setIsActive(true);
		syncRegistrationDto1.setIsDeleted(false);
		syncRegistrationDto1.setName("mosip");
		syncRegistrationDto1.setPhone("1234567890");
		syncRegistrationDto1.setEmail("mosip1@gmail.com");

		syncRegistrationDto1.setSyncType("NEW_REGISTRATION");
		syncRegistrationDto1.setPacketHashValue("ab123");
		syncRegistrationDto1.setSupervisorStatus("APPROVED");

		syncRegistrationDto2 = new SyncRegistrationDto();

		syncRegistrationDto2.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto2.setLangCode("eng");
		syncRegistrationDto2.setIsActive(true);
		syncRegistrationDto2.setIsDeleted(false);
		syncRegistrationDto1.setName("mosip");
		syncRegistrationDto1.setPhone("1234567890");
		syncRegistrationDto1.setEmail("mosip1@gmail.com");
		syncRegistrationDto2.setSyncType(SyncTypeDto.UPDATE.getValue());
		syncRegistrationDto2.setPacketHashValue("ab123");
		syncRegistrationDto2.setSupervisorStatus("APPROVED");

		syncRegistrationDto3 = new SyncRegistrationDto();
		syncRegistrationDto3.setRegistrationId("53718436135988");
		syncRegistrationDto3.setLangCode("eng");
		syncRegistrationDto3.setIsActive(true);
		syncRegistrationDto3.setIsDeleted(false);

		syncRegistrationDto3.setSyncType(SyncTypeDto.NEW.getValue());
		syncRegistrationDto3.setPacketHashValue("ab123");
		syncRegistrationDto3.setSupervisorStatus("APPROVED");

		syncRegistrationDto4 = new SyncRegistrationDto();
		syncRegistrationDto4.setRegistrationId("12345678901234567890123456799");
		syncRegistrationDto4.setLangCode("eng");
		syncRegistrationDto4.setIsActive(true);
		syncRegistrationDto4.setIsDeleted(false);

		syncRegistrationDto4.setSyncType(SyncTypeDto.NEW.getValue());
		syncRegistrationDto4.setPacketHashValue("ab123");
		syncRegistrationDto4.setSupervisorStatus("APPROVED");

		syncRegistrationDto5 = new SyncRegistrationDto();
		syncRegistrationDto5.setRegistrationId("1234567890123456789012345ABCD");
		syncRegistrationDto5.setLangCode("eng");
		syncRegistrationDto5.setIsActive(true);
		syncRegistrationDto5.setIsDeleted(false);

		syncRegistrationDto5.setSyncType(SyncTypeDto.NEW.getValue());
		syncRegistrationDto5.setPacketHashValue("ab123");
		syncRegistrationDto5.setSupervisorStatus("APPROVED");

		syncRegistrationDto6 = new SyncRegistrationDto();
		syncRegistrationDto6.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto6.setLangCode("eng");
		syncRegistrationDto6.setEmail("satish@gmail.com");
		syncRegistrationDto6.setPhone("9374838433");
		syncRegistrationDto6.setIsActive(true);
		syncRegistrationDto6.setIsDeleted(false);

		syncRegistrationDto6.setSyncType(SyncTypeDto.NEW.getValue());
		syncRegistrationDto6.setPacketHashValue("ab123");
		syncRegistrationDto6.setSupervisorStatus("APPROVED");
		
		SyncRegistrationDto syncRegistrationDto7 = new SyncRegistrationDto();
		syncRegistrationDto7.setRegistrationId(null);
		syncRegistrationDto7.setLangCode("eng");
		syncRegistrationDto7.setIsActive(true);
		syncRegistrationDto7.setIsDeleted(false);

		syncRegistrationDto7.setSyncType(SyncTypeDto.ACTIVATED.getValue());
		syncRegistrationDto7.setPacketHashValue("ab123");
		syncRegistrationDto7.setSupervisorStatus("APPROVED");

		SyncRegistrationDto syncRegistrationDto8 = new SyncRegistrationDto();
		syncRegistrationDto8.setRegistrationId("12345678901234567890123456789");
		syncRegistrationDto8.setLangCode("eng");
		syncRegistrationDto8.setIsActive(true);
		syncRegistrationDto8.setIsDeleted(false);

		syncRegistrationDto8.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto8.setPacketHashValue("ab123");
		syncRegistrationDto8.setSupervisorStatus("APPROVED");

		SyncRegistrationDto syncRegistrationDto9 = new SyncRegistrationDto();
		syncRegistrationDto9.setRegistrationId("27847657360002520181208183050");
		syncRegistrationDto9.setLangCode("eng");
		syncRegistrationDto9.setIsActive(true);
		syncRegistrationDto9.setIsDeleted(false);

		syncRegistrationDto9.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto9.setPacketHashValue("ab123");
		syncRegistrationDto9.setSupervisorStatus("APPROVED");

		SyncRegistrationDto syncRegistrationDto10 = new SyncRegistrationDto();
		syncRegistrationDto10.setRegistrationId("27847657360002520181208183050");
		syncRegistrationDto10.setLangCode("eng");
		syncRegistrationDto10.setIsActive(true);
		syncRegistrationDto10.setIsDeleted(false);

		syncRegistrationDto10.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto10.setPacketHashValue("ab123");
		syncRegistrationDto10.setSupervisorStatus("APPROVED");

		SyncRegistrationDto syncRegistrationDto11 = new SyncRegistrationDto();
		syncRegistrationDto11.setRegistrationId("27847657360002520181208183050");
		syncRegistrationDto11.setLangCode("eng");
		syncRegistrationDto11.setIsActive(true);
		syncRegistrationDto11.setIsDeleted(false);

		syncRegistrationDto11.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto11.setPacketHashValue("ab123");
		syncRegistrationDto11.setSupervisorStatus("APPROVED");

		SyncRegistrationDto syncRegistrationDto12 = new SyncRegistrationDto();
		syncRegistrationDto12.setRegistrationId("12345678901234567890123456799");
		syncRegistrationDto12.setLangCode("eng");
		syncRegistrationDto12.setIsActive(true);
		syncRegistrationDto12.setIsDeleted(false);

		syncRegistrationDto12.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto12.setPacketHashValue("ab123");
		syncRegistrationDto12.setSupervisorStatus("APPROVED");

		syncRegistrationDto13 = new SyncRegistrationDto();
		syncRegistrationDto13.setRegistrationId("1234567890123456789012345ABCD");
		syncRegistrationDto13.setLangCode("eng");
		syncRegistrationDto13.setIsActive(true);
		syncRegistrationDto13.setIsDeleted(false);

		syncRegistrationDto13.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto13.setPacketHashValue("ab123");
		syncRegistrationDto13.setSupervisorStatus("test");

		SyncRegistrationDto syncRegistrationDto14 = new SyncRegistrationDto();
		syncRegistrationDto14.setRegistrationId("27847657360002520181208183123");
		syncRegistrationDto14.setLangCode("eng");
		syncRegistrationDto14.setIsActive(true);
		syncRegistrationDto14.setIsDeleted(false);

		syncRegistrationDto14.setSyncType(SyncTypeDto.DEACTIVATED.getValue());

		SyncRegistrationDto syncRegistrationDto15 = new SyncRegistrationDto();
		syncRegistrationDto15.setRegistrationId("27847657360002520181208183124");
		syncRegistrationDto15.setLangCode("eng");
		syncRegistrationDto15.setIsActive(true);
		syncRegistrationDto15.setIsDeleted(false);

		syncRegistrationDto15.setSyncType(SyncTypeDto.DEACTIVATED.getValue());
		syncRegistrationDto15.setSupervisorStatus("test");

		entities.add(syncRegistrationDto);
		entities.add(syncRegistrationDto1);
		entities.add(syncRegistrationDto2);
		entities.add(syncRegistrationDto3);
		entities.add(syncRegistrationDto4);
		entities.add(syncRegistrationDto5);

		entities.add(syncRegistrationDto7);
		entities.add(syncRegistrationDto8);
		entities.add(syncRegistrationDto9);
		entities.add(syncRegistrationDto10);
		entities.add(syncRegistrationDto11);
		entities.add(syncRegistrationDto12);
		entities.add(syncRegistrationDto13);
		entities.add(syncRegistrationDto14);
		entities.add(syncRegistrationDto15);

		syncResponseDto = new SyncResponseSuccessDto();
		syncResponseDto.setRegistrationId(syncRegistrationDto.getRegistrationId());

		syncResponseDto.setStatus("Success");

		syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setWorkflowInstanceId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
		syncRegistrationEntity.setRegistrationId(syncRegistrationDto.getRegistrationId());
		syncRegistrationEntity.setRegistrationType(syncRegistrationDto.getRegistrationType().toString());

		syncRegistrationEntity.setLangCode(syncRegistrationDto.getLangCode());

		syncRegistrationEntity.setIsDeleted(syncRegistrationDto.getIsDeleted());
		syncRegistrationEntity.setCreateDateTime(LocalDateTime.now());
		syncRegistrationEntity.setUpdateDateTime(LocalDateTime.now());
		syncRegistrationEntity.setCreatedBy("MOSIP");
		syncRegistrationEntity.setUpdatedBy("MOSIP");
		syncRegistrationEntity.setPacketId("test1");
		syncRegistrationEntities.add(syncRegistrationEntity);
		Mockito.when(ridValidator.validateId(any())).thenReturn(true);
		Mockito.when(syncRegistrationDao.getSaltValue(any())).thenReturn("abc12");
		Mockito.when(restApiClient.getApi(any(), any()))
				.thenReturn("{\"response\":{\"registrationCenters\":[{\"locationCode\":\"401105\"}]}}");
		List<String> mainprocessList=new ArrayList<>();
		mainprocessList.add("NEW");
		mainprocessList.add("UPDATE");
		ReflectionTestUtils.setField(syncRegistrationService, "mainProcesses", mainprocessList);
		List<String> subprocessList=new ArrayList<>();
		subprocessList.add("BIOMETRIC_CORRECTION");
		ReflectionTestUtils.setField(syncRegistrationService, "subProcesses", subprocessList);

		String mappingJsonString = "{\"identity\":{\"IDSchemaVersion\":{\"value\":\"IDSchemaVersion\"},\"address\":{\"value\":\"permanentAddressLine1,permanentAddressLine2,permanentAddressLine3,permanentRegion,permanentProvince,permanentCity,permanentZone,permanentPostalcode\"},\"phone\":{\"value\":\"phone\"},\"email\":{\"value\":\"email\"}}}";

		org.json.simple.JSONObject mappingJsonObject = new JSONObject();
		LinkedHashMap identity = new LinkedHashMap();
		LinkedHashMap IDSchemaVersion = new LinkedHashMap();
		IDSchemaVersion.put("value", "IDSchemaVersion");
		LinkedHashMap address = new LinkedHashMap();
		address.put("value",
				"permanentAddressLine1,permanentAddressLine2,permanentAddressLine3,permanentRegion,permanentProvince,permanentCity,permanentZone,permanentPostalcode");
		LinkedHashMap phone = new LinkedHashMap();
		phone.put("value", "phone");
		LinkedHashMap email = new LinkedHashMap();
		email.put("value", "email");
		LinkedHashMap dateOfBirth = new LinkedHashMap();
		dateOfBirth.put("value", "dateOfBirth");
		LinkedHashMap gender = new LinkedHashMap();
		gender.put("value", "gender");
		LinkedHashMap locationHierarchyForProfiling = new LinkedHashMap();
		locationHierarchyForProfiling.put("value", "zone,postalCode");
		LinkedHashMap preferredLang = new LinkedHashMap();
		preferredLang.put("value", "preferredLang");

		identity.put("IDSchemaVersion", IDSchemaVersion);
		identity.put("address", address);
		identity.put("phone", phone);
		identity.put("email", email);
		identity.put("dob", dateOfBirth);
		identity.put("gender", gender);
		identity.put("locationHierarchyForProfiling", locationHierarchyForProfiling);
		mappingJsonObject.put("identity", identity);

		Mockito.when(registrationUtility.getMappingJson()).thenReturn(mappingJsonString);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(mappingJsonObject);

	}

	/**
	 * Gets the sync registration status test.
	 *
	 * @return the sync registration status test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void testGetSyncRegistrationStatusSuccess() throws EncryptionFailureException, ApisResourceAccessException {
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto6);
		byte[] encryptedInfo = "encryptedInfo".getBytes();
		Mockito.when(encryptor.encrypt(anyString(), anyString(), anyString())).thenReturn(encryptedInfo);
		Mockito.when(syncRegistrationDao.save(any())).thenReturn(syncRegistrationEntity);
		List<SyncResponseDto> syncResponse = syncRegistrationService.sync(request, "10011_10011", "");
		Mockito.doNothing().when(anonymousProfileService).saveAnonymousProfile(any(), any(), any());

		assertEquals("Verifing List returned", (syncResponse.get(0)).getRegistrationId(),
				syncRegistrationDto.getRegistrationId());

		Mockito.when(syncRegistrationDao.findByPacketId(any())).thenReturn(syncRegistrationEntity);
		Mockito.when(syncRegistrationDao.update(any())).thenReturn(syncRegistrationEntity);
		List<SyncResponseDto> syncResponseDto = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("Verifing if list is returned. Expected value should be 1002",
				syncRegistrationDto.getRegistrationId(),
				(syncResponseDto.get(0)).getRegistrationId());
		Mockito.doNothing().when(anonymousProfileService).saveAnonymousProfile(anyString(), anyString(), anyString());
	}

	@Test
	public void testGetSyncRegistrationStatusV2Success() throws EncryptionFailureException, ApisResourceAccessException {
		byte[] encryptedInfo = "encryptedInfo".getBytes();
		SyncRegistrationDto syncRegistrationDto16 = new SyncRegistrationDto();

		syncRegistrationDto16.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto16.setLangCode("eng");

		syncRegistrationDto16.setIsActive(true);
		syncRegistrationDto16.setIsDeleted(false);
		syncRegistrationDto16.setPacketId("1234");
		syncRegistrationDto16.setAdditionalInfoReqId("1234.NEW.1");
		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setPacketHashValue("ab123");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		List<SyncRegistrationDto> request=new ArrayList<>();
		request.add(syncRegistrationDto16);
		Mockito.when(encryptor.encrypt(anyString(), anyString(), anyString())).thenReturn(encryptedInfo);
		Mockito.when(syncRegistrationDao.save(any())).thenReturn(syncRegistrationEntity);
		Mockito.doNothing().when(anonymousProfileService).saveAnonymousProfile(any(), any(), any());

		List<SyncResponseDto> syncResponse = syncRegistrationService.syncV2(request, "", "");

		assertEquals("Verifing List returned", (syncResponse.get(0)).getRegistrationId(),
				syncRegistrationDto.getRegistrationId());

		Mockito.when(syncRegistrationDao.findByPacketId(any())).thenReturn(syncRegistrationEntity);
		Mockito.when(syncRegistrationDao.update(any())).thenReturn(syncRegistrationEntity);
		List<SyncResponseDto> syncResponseDto = syncRegistrationService.syncV2(request, "", "");
		assertEquals("Verifing if list is returned. Expected value should be 1002",
				syncRegistrationDto.getRegistrationId(),
				(syncResponseDto.get(0)).getRegistrationId());
	}

	@Test
	public void testGetSyncRegistrationStatusV2Failure() throws EncryptionFailureException, ApisResourceAccessException {
		byte[] encryptedInfo = "encryptedInfo".getBytes();
		SyncRegistrationDto syncRegistrationDto16 = new SyncRegistrationDto();

		syncRegistrationDto16.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto16.setLangCode("eng");

		syncRegistrationDto16.setIsActive(true);
		syncRegistrationDto16.setIsDeleted(false);
		syncRegistrationDto16.setPacketId("1234");
		syncRegistrationDto16.setAdditionalInfoReqId("");
		syncRegistrationDto16.setSyncType("NEW_REGISTRATION");
		syncRegistrationDto16.setPacketHashValue("ab123");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		List<SyncRegistrationDto> request=new ArrayList<>();
		request.add(syncRegistrationDto16);
		Mockito.when(encryptor.encrypt(anyString(), anyString(), anyString())).thenReturn(encryptedInfo);
		Mockito.when(syncRegistrationDao.save(any())).thenReturn(syncRegistrationEntity);
		List<SyncResponseDto> syncResponse = syncRegistrationService.syncV2(request, "", "");

		assertEquals("Invalid Sync Type", ((SyncResponseFailureV2Dto) syncResponse.get(0)).getMessage());

		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setSupervisorStatus(null);
		List<SyncRegistrationDto> request1=new ArrayList<>();
		request1.add(syncRegistrationDto16);
		List<SyncResponseDto> syncResponse1 = syncRegistrationService.syncV2(request1, "", "");

		assertEquals("Invalid Request Value - Supervisor Status can be APPROVED/REJECTED",
				((SyncResponseFailureV2Dto) syncResponse1.get(0)).getMessage());

		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		syncRegistrationDto16.setPacketHashValue(null);
		List<SyncRegistrationDto> request2=new ArrayList<>();
		request2.add(syncRegistrationDto16);
		List<SyncResponseDto> syncResponse2 = syncRegistrationService.syncV2(request2, "", "");

		assertEquals("Invalid Request Value - Hash Sequence is NULL",
				((SyncResponseFailureV2Dto) syncResponse2.get(0)).getMessage());

		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		syncRegistrationDto16.setPacketHashValue("abc");
		syncRegistrationDto16.setLangCode("english");
		List<SyncRegistrationDto> request3=new ArrayList<>();
		request3.add(syncRegistrationDto16);
		List<SyncResponseDto> syncResponse3 = syncRegistrationService.syncV2(request3, "", "");

		assertEquals("Invalid Language Code - Language Code must be of Three Characters",
				((SyncResponseFailureV2Dto) syncResponse3.get(0)).getMessage());

		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		syncRegistrationDto16.setPacketHashValue("abc");
		syncRegistrationDto16.setLangCode("eng");
		syncRegistrationDto16.setRegistrationId(null);
		List<SyncRegistrationDto> request4=new ArrayList<>();
		request4.add(syncRegistrationDto16);
		List<SyncResponseDto> syncResponse4 = syncRegistrationService.syncV2(request4, "", "");

		assertEquals("Invalid Request Value - RID cannot be NULL",
				((SyncResponseFailureV2Dto) syncResponse4.get(0)).getMessage());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void testGetSyncRegistrationStatusV2DataAccessLayerFailure()
			throws EncryptionFailureException, ApisResourceAccessException {
		byte[] encryptedInfo = "encryptedInfo".getBytes();
		SyncRegistrationDto syncRegistrationDto16 = new SyncRegistrationDto();

		syncRegistrationDto16.setRegistrationId("27847657360002520181208183052");
		syncRegistrationDto16.setLangCode("eng");

		syncRegistrationDto16.setIsActive(true);
		syncRegistrationDto16.setIsDeleted(false);
		syncRegistrationDto16.setPacketId("1234");
		syncRegistrationDto16.setAdditionalInfoReqId("");
		syncRegistrationDto16.setSyncType("NEW");
		syncRegistrationDto16.setPacketHashValue("ab123");
		syncRegistrationDto16.setSupervisorStatus("APPROVED");
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto16);

		DataAccessLayerException exception = new DataAccessLayerException("ERR-001", "exception occured", null);
		Mockito.when(encryptor.encrypt(anyString(), anyString(), anyString())).thenReturn(encryptedInfo);
		Mockito.when(syncRegistrationDao.save(any())).thenThrow(exception);
		syncRegistrationService.syncV2(request, "", "");
	}

	/**
	 * Gets the sync registration status failure test.
	 *
	 * @return the sync registration status failure test
	 * @throws TablenotAccessibleException
	 *             the tablenot accessible exception
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test(expected = TablenotAccessibleException.class)
	public void getSyncRegistrationStatusFailureTest() throws TablenotAccessibleException, EncryptionFailureException, ApisResourceAccessException {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());

		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto5);
		byte[] encryptedInfo = "encryptedInfo".getBytes();
		Mockito.when(encryptor.encrypt(anyString(), anyString(), anyString())).thenReturn(encryptedInfo);
		Mockito.when(syncRegistrationDao.save(any())).thenThrow(exp);
		syncRegistrationService.sync(request, "10011_10011", "");

	}

	/**
	 * Checks if is present success test.
	 */
	@Test
	public void testIsPresentSuccess() {
		Mockito.when(syncRegistrationDao.findByPacketId(any())).thenReturn(syncRegistrationEntity);
		boolean result = syncRegistrationService.isPresent("1001");
		assertEquals("Verifing if Registration Id is present in DB. Expected value is true", true, result);
	}

	/**
	 * Gets the sync registration id failure test.
	 *
	 * @return the sync registration id failure test
	 */
	@Test
	public void getSyncRegistrationIdFailureTest() {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID_LENGTH.getErrorCode(),
				RidExceptionProperty.INVALID_RID_LENGTH.getErrorMessage());
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto3);
		Mockito.when(ridValidator.validateId(any())).thenThrow(exp);
		syncRegistrationService.sync(request, "10011_10011", "");
	}

	/**
	 * Gets the sync rid in valid length failure test.
	 *
	 * @return the sync rid in valid length failure test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void getSyncRidInValidLengthFailureTest() throws EncryptionFailureException, ApisResourceAccessException {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID_LENGTH.getErrorCode(),
				RidExceptionProperty.INVALID_RID_LENGTH.getErrorMessage());
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto3);
		Mockito.when(ridValidator.validateId(anyString())).thenThrow(exp);
		List<SyncResponseDto> syncResponseList = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("FAILURE", syncResponseList.get(0).getStatus());
	}

	/**
	 * Gets the sync rid in valid time stamp failure test.
	 *
	 * @return the sync rid in valid time stamp failure test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void getSyncRidInValidTimeStampFailureTest() throws EncryptionFailureException, ApisResourceAccessException {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID_TIMESTAMP.getErrorCode(),
				RidExceptionProperty.INVALID_RID_TIMESTAMP.getErrorMessage());
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto4);
		Mockito.when(ridValidator.validateId(anyString())).thenThrow(exp);
		List<SyncResponseDto> syncResponseList = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("FAILURE", syncResponseList.get(0).getStatus());
	}

	/**
	 * Gets the sync prid in valid length failure test.
	 *
	 * @return the sync prid in valid length failure test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void getSyncPridInValidLengthFailureTest() throws EncryptionFailureException, ApisResourceAccessException {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID_LENGTH.getErrorCode(),
				RidExceptionProperty.INVALID_RID_LENGTH.getErrorMessage());

		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto3);
		Mockito.when(ridValidator.validateId(anyString())).thenThrow(exp);
		List<SyncResponseDto> syncResponseList = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("FAILURE", syncResponseList.get(0).getStatus());
	}

	/**
	 * Gets the sync prid in valid time stamp failure test.
	 *
	 * @return the sync prid in valid time stamp failure test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void getSyncPridInValidTimeStampFailureTest() throws EncryptionFailureException, ApisResourceAccessException {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID_TIMESTAMP.getErrorCode(),
				RidExceptionProperty.INVALID_RID_TIMESTAMP.getErrorMessage());
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto4);
		Mockito.when(ridValidator.validateId(anyString())).thenThrow(exp);
		List<SyncResponseDto> syncResponseList = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("FAILURE", syncResponseList.get(0).getStatus());
	}

	/**
	 * Gets the sync prid in valid format failure test.
	 *
	 * @return the sync prid in valid format failure test
	 * @throws ApisResourceAccessException 
	 * @throws EncryptionFailureException 
	 */
	@Test
	public void getSyncPridInValidFormatFailureTest() throws EncryptionFailureException, ApisResourceAccessException {
		InvalidIDException exp = new InvalidIDException(RidExceptionProperty.INVALID_RID.getErrorCode(),
				RidExceptionProperty.INVALID_RID.getErrorMessage());
		List<SyncRegistrationDto> request = new ArrayList<>();
		request.add(syncRegistrationDto5);
		Mockito.when(ridValidator.validateId(anyString())).thenThrow(exp);
		List<SyncResponseDto> syncResponseList = syncRegistrationService.sync(request, "10011_10011", "");
		assertEquals("FAILURE", syncResponseList.get(0).getStatus());
	}

	@Test
	public void testdecryptAndGetSyncRequest() throws PacketDecryptionFailureException, ApisResourceAccessException,
			JsonParseException, JsonMappingException, IOException {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenReturn("test");
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any())).thenReturn(registrationSyncRequestDTO);
		RegistrationSyncRequestDTO regSyncDto = syncRegistrationService.decryptAndGetSyncRequest("", "", "1234",
				syncResponseList);
		assertEquals("decrypted and return the dto", regSyncDto, registrationSyncRequestDTO);
	}

	@Test
	public void testdecryptAndGetSyncRequestIOException() throws PacketDecryptionFailureException, ApisResourceAccessException,
			JsonParseException, JsonMappingException, IOException {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		IOException exception = new IOException("ERR-001", "exception occured");
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenReturn("test");
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any())).thenThrow(exception);
		syncRegistrationService.decryptAndGetSyncRequest("", "", "1234",
				syncResponseList);
		SyncResponseFailDto dto = (SyncResponseFailDto) syncResponseList.get(0);
		assertEquals(dto.getMessage(), "IO EXCEPTION ");
	}

	@Test
	public void testDecryptionException() throws PacketDecryptionFailureException, ApisResourceAccessException,
			JsonParseException, JsonMappingException, IOException {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenThrow(new PacketDecryptionFailureException("", ""));
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any())).thenReturn(registrationSyncRequestDTO);
		RegistrationSyncRequestDTO regSyncDto = syncRegistrationService.decryptAndGetSyncRequest("", "", "1234",
				syncResponseList);
		assertEquals(1, syncResponseList.size());

	}

	@Test
	public void testJsonParseException() throws PacketDecryptionFailureException, ApisResourceAccessException,
			JsonParseException, JsonMappingException, IOException {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenReturn("test");
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any()))
				.thenThrow(new JsonParseException("", "", new Throwable()));
		RegistrationSyncRequestDTO regSyncDto = syncRegistrationService.decryptAndGetSyncRequest("", "", "1234",
				syncResponseList);
		assertEquals(1, syncResponseList.size());

	}

	@Test
	public void testJsonMappingException() throws PacketDecryptionFailureException, ApisResourceAccessException,
			JsonParseException, JsonMappingException, IOException {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenReturn("test");
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.when(JsonUtils.jsonStringToJavaObject(any(), any()))
				.thenThrow(new JsonMappingException("", "", new Throwable()));
		RegistrationSyncRequestDTO regSyncDto = syncRegistrationService.decryptAndGetSyncRequest("", "", "1234",
				syncResponseList);
		assertEquals(1, syncResponseList.size());

	}

	@Test
	public void testGetByIdsSuccess() {

		Mockito.when(syncRegistrationDao.getByIds(any())).thenReturn(syncRegistrationEntities);

		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);
		List<RegistrationStatusDto> list = syncRegistrationService.getByIds(registrationIds);
		assertEquals("UPLOAD_PENDING", list.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getByIdsFailureTest() {
		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);

		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(syncRegistrationDao.getByIds(any())).thenThrow(exp);
		syncRegistrationService.getByIds(registrationIds);
	}

	@Test
	public void testGetByPacketIdsSuccess() {

		Mockito.when(syncRegistrationDao.getByPacketIds(any())).thenReturn(syncRegistrationEntities);
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("test1");
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationService.getByPacketIds(packetIdList);
		assertEquals("test1", syncRegistrationEntityList.get(0).getPacketId());
	}
	@Test(expected = TablenotAccessibleException.class)
	public void testGetByPacketIdsFailure() {
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("test1");
         DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(syncRegistrationDao.getByPacketIds(any())).thenThrow(exp);
        syncRegistrationService.getByPacketIds(packetIdList);

	}

	@Test(expected = TablenotAccessibleException.class)
	public void searchLostRid() {
		
		SearchInfo searchInfo = new SearchInfo();
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		List<SortInfo> sortInfos = new ArrayList<SortInfo>();
		List<String> testIdList = new ArrayList<String>();
		FilterInfo filterInfo = new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		filterInfo.setType("equals");
		FilterInfo filterInfo1 = new FilterInfo();
		filterInfo1.setColumnName("email");
		filterInfo1.setValue("mosip1@gmail.com");
		filterInfo1.setType("equals");
		SortInfo sortInfo = new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		filterInfos.add(filterInfo1);
		sortInfos.add(sortInfo);
		testIdList.add("1001");
		searchInfo.setFilters(filterInfos);
		searchInfo.setSort(sortInfos);
		List<LostRidDto> lostRidDtos = syncRegistrationService.searchLostRid(searchInfo);
		assertEquals(lostRidDtos.get(0).getRegistrationId(), testIdList.get(0));
	}

	@Test
	public void searchLostRidVariousScenario() throws PacketDecryptionFailureException, ApisResourceAccessException {
		
		ReflectionTestUtils.setField(syncRegistrationService, "maxSearchResult", 2);
		Mockito.when(syncRegistrationDao.getSearchResults(any(),any())).thenReturn(syncRegistrationEntities);
		Mockito.when(decryptor.decrypt(any(), any(), any())).thenReturn("{\"name\":\"mosip\"}");
		SearchInfo searchInfo = new SearchInfo();
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		List<SortInfo> sortInfos = new ArrayList<SortInfo>();
		List<String> testIdList = new ArrayList<String>();
		FilterInfo filterInfo = new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		filterInfo.setType("equals");
		FilterInfo filterInfo1 = new FilterInfo();
		filterInfo1.setColumnName("email");
		filterInfo1.setValue("mosip1@gmail.com");
		filterInfo1.setType("equals");
		SortInfo sortInfo = new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		filterInfos.add(filterInfo1);
		sortInfos.add(sortInfo);
		testIdList.add("27847657360002520181208183052");
		searchInfo.setFilters(filterInfos);
		searchInfo.setSort(sortInfos);
		List<LostRidDto> lostRidDtos = syncRegistrationService.searchLostRid(searchInfo);
		assertEquals(lostRidDtos.get(0).getRegistrationId(), testIdList.get(0));
	}
	
	@Test
	public void getExternalStatusByIdsTest() {

		List<String> requestIds = Arrays.asList("1001");
		Mockito.when(syncRegistrationDao.getByIds(any())).thenReturn(syncRegistrationEntities);

		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);
		List<RegistrationStatusDto> list = syncRegistrationService.getExternalStatusByIds(requestIds);
		assertEquals("UPLOAD_PENDING", list.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getExternalStatusByIdsFailureTest() {
		List<String> requestIds = Arrays.asList("1001");
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(syncRegistrationDao.getByIds(any())).thenThrow(exp);
		syncRegistrationService.getExternalStatusByIds(requestIds);
	}

}