
package io.mosip.registration.processor.packet.storage.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.BiometricRecordValidationException;
import io.mosip.registration.processor.core.exception.DataShareException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDetDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.Filter;
import io.mosip.registration.processor.core.packet.dto.abis.ShareableAttributes;
import io.mosip.registration.processor.core.packet.dto.abis.Source;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.mapper.PacketInfoMapper;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;

/**
 * The Class PacketInfoManagerImplTest.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, PacketInfoMapper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class ABISHandlerUtilTest {

	private static final String registrationId = "10002100820001420210108085956";
	private static final String registrationType = "NEW";
	private static final String latestTransactionId = "123-456-789";
	List<String> matchedRids = new ArrayList<>();


	@InjectMocks
	private ABISHandlerUtil abisHandlerUtil;

	@Mock
	private Utilities utilities;

	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Mock
	private PacketInfoDao packetInfoDao;

	@Mock
	private IdRepoService idRepoService;
	
	@Mock
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;


	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(utilities.getLatestTransactionId(registrationId)).thenReturn(latestTransactionId);

		List<String> regBioRefIds = new ArrayList<>();
		regBioRefIds.add("cf1c941a-142c-44f1-9543-4606b4a7884e");

		when(packetInfoDao.getAbisRefMatchedRefIdByRid(registrationId)).thenReturn(regBioRefIds);
		when(utilities.getGetRegProcessorDemographicIdentity()).thenReturn(new String());
		List<RegistrationStatusEntity> registrationStatusEntityList = new ArrayList<>();

		RegistrationStatusEntity registrationEntity1 = new RegistrationStatusEntity();
		registrationEntity1.setId("10002100820001420210108085103");
		registrationEntity1.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
		registrationStatusEntityList.add(registrationEntity1);
		RegistrationStatusEntity registrationEntity2 = new RegistrationStatusEntity();
		registrationEntity2.setId("10002100820001420210108085100");
		registrationEntity2.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
		registrationStatusEntityList.add(registrationEntity2);
		RegistrationStatusEntity registrationEntity3 = new RegistrationStatusEntity();
		registrationEntity3.setId("10002100820001420210108085102");
		registrationEntity3.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
		registrationStatusEntityList.add(registrationEntity3);
		matchedRids.add("10002100820001420210108085100");
		matchedRids.add("10002100820001420210108085103");
		matchedRids.add("10002100820001420210108085101");// REJECTED
		matchedRids.add("10002100820001420210108085102");
		List<AbisResponseDto> abisResponseDtoList = new ArrayList<>();
		matchedRids.forEach(matchedRid -> {
			AbisResponseDto abisResponseDto = new AbisResponseDto();
			abisResponseDto.setId(matchedRid);
			abisResponseDtoList.add(abisResponseDto);
		});

		when(packetInfoManager.getAbisResponseRecords(regBioRefIds.get(0),
				latestTransactionId, AbisConstant.IDENTIFY)).thenReturn(abisResponseDtoList);

		List<AbisResponseDetDto> abisResponseDetDtoList = new ArrayList<>();

		matchedRids.forEach(matchedRid -> {
			AbisResponseDetDto abisResponseDto = new AbisResponseDetDto();
			abisResponseDto.setMatchedBioRefId(matchedRid);
			abisResponseDetDtoList.add(abisResponseDto);
		});
		for (AbisResponseDetDto dto : abisResponseDetDtoList) {
			AbisResponseDetDto responseDetDto = new AbisResponseDetDto();
			responseDetDto.setMatchedBioRefId(dto.getMatchedBioRefId());
			when(packetInfoManager.getAbisResponseDetails(dto.getMatchedBioRefId())).thenReturn(Lists.newArrayList(responseDetDto));
		}

		when(packetInfoDao.getAbisRefRegIdsByMatchedRefIds(matchedRids)).thenReturn(matchedRids);

		when(packetInfoDao.getWithoutStatusCode(matchedRids, RegistrationStatusCode.REJECTED.toString()))
				.thenReturn(registrationStatusEntityList);
	

		when(idRepoService.getUinByRid("10002100820001420210108085103", new String())).thenReturn("123456789");
		when(idRepoService.getUinByRid("10002100820001420210108085102", new String())).thenReturn("987654321");

	}

	@Test
	public void testProcesssedWithUniqueUin() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

		List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, ProviderStageName.BIO_DEDUPE);
		// expected to pick 2 rids from processedMatchedIds list because different uin.
		// Total should be 1(inprogress) + 2(processed)
		assertEquals(3, uniqueRids.size());
	}

	@Test
	public void testProcesssedWithSameUin() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

		when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn("987654321");

		List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, ProviderStageName.BIO_DEDUPE);
		// expected to pick only 1 rid from processedMatchedIds list because same uin. Total should be 1(inprogress) + 1(processed)
		assertEquals(2, uniqueRids.size());
	}

	@Test
	public void testDonotReturnRejected() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

		List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, ProviderStageName.BIO_DEDUPE);
		// expected to pick only processingandprocessed list i.e 3 records.
		assertEquals(3, uniqueRids.size());
	}

	@Test
	public void testReturnAllInprogress() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

		when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn(null);

		List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, ProviderStageName.BIO_DEDUPE);
		// expected not to pick processedMatchedIds list i.e 1 records.
		assertEquals(1, uniqueRids.size());
	}

	private List<String> getIrisList() {
		return Arrays.asList("Left", "Right");

	}

	private List<String> getFingerList() {
		return Arrays.asList("Left Thumb", "Left LittleFinger", "Left IndexFinger", "Left MiddleFinger",
				"Left RingFinger", "Right Thumb", "Right LittleFinger", "Right IndexFinger", "Right MiddleFinger",
				"Right RingFinger");
	}

	private List<String> getFaceList() {
		return Arrays.asList("Face");
	}

	private BiometricRecord getBiometricRecord(List<String> bioAttributes, boolean isBdbEmpty) {
		BiometricRecord biometricRecord = new BiometricRecord();

		byte[] bdb = isBdbEmpty ? null : new byte[2048];
		for (String bioAttribute : bioAttributes) {
			BIR birType1 = new BIR.BIRBuilder().build();
			BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
			io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new RegistryIDType();
			registryIDType.setOrganization("Mosip");
			registryIDType.setType("257");
			io.mosip.kernel.biometrics.constant.QualityType quality = new QualityType();
			quality.setAlgorithm(registryIDType);
			quality.setScore(90l);
			bdbInfoType1.setQuality(quality);

			BiometricType singleType1 = bioAttribute.equalsIgnoreCase("face") ? BiometricType.FACE
					: bioAttribute.equalsIgnoreCase("left") || bioAttribute.equalsIgnoreCase("right")
							? BiometricType.IRIS
							: BiometricType.FINGER;
			List<BiometricType> singleTypeList1 = new ArrayList<>();
			singleTypeList1.add(singleType1);
			bdbInfoType1.setType(singleTypeList1);

			String[] bioAttributeArray = bioAttribute.split(" ");

			List<String> subtype = new ArrayList<>();
			for (String attribute : bioAttributeArray) {
				subtype.add(attribute);
			}
			bdbInfoType1.setSubtype(subtype);

			birType1.setBdbInfo(bdbInfoType1);
			birType1.setBdb(bdb);

			biometricRecord.getSegments().add(birType1);
		}

		return biometricRecord;
	}

	private void mockDataSharePolicy(List<BiometricType> shareableBiometricList) throws ApisResourceAccessException {
		when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
				.thenReturn(getMockDataSharePolicy(shareableBiometricList));
	}

	private ResponseWrapper<LinkedHashMap<String, Object>> getMockDataSharePolicy(
			List<BiometricType> shareableBiometricList) {

		ObjectMapper mapper = new ObjectMapper();

		List<ShareableAttributes> attr = new LinkedList<>();
		if (shareableBiometricList != null && !shareableBiometricList.isEmpty()) {

			ShareableAttributes shareableAttributes = new ShareableAttributes();
			List<Source> sourceList = new ArrayList<>();

			for (BiometricType bioType : shareableBiometricList) {
				Filter filter = new Filter();
				filter.setType(bioType.value());
				if (BiometricType.FINGER.equals(bioType)) {
					filter.setSubType(getFingerList());
				} else if (BiometricType.FINGER.equals(bioType)) {
					filter.setSubType(getIrisList());
				}

				Source src = new Source();
				src.setFilter(Lists.newArrayList(filter));
				sourceList.add(src);
			}

			shareableAttributes.setSource(sourceList);
			attr = Lists.newArrayList(shareableAttributes);
		}

		ResponseWrapper<LinkedHashMap<String, Object>> policy = new ResponseWrapper<>();
		LinkedHashMap<String, Object> policies = new LinkedHashMap<>();
		LinkedHashMap<String, Object> sharableAttributes = new LinkedHashMap<>();
		sharableAttributes.put(PolicyConstant.SHAREABLE_ATTRIBUTES, attr);
		policies.put(PolicyConstant.POLICIES, sharableAttributes);
		policy.setResponse(policies);

		return policy;
	}

	@Test
	public void testcreateTypeSubtypeMapping() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, DataShareException, com.fasterxml.jackson.core.JsonProcessingException, IOException {
		mockDataSharePolicy(Lists.newArrayList(BiometricType.FACE, BiometricType.FINGER, BiometricType.IRIS));
		Map<String, List<String>> typeAndSubtypMap = abisHandlerUtil.createBiometricTypeSubtypeMappingFromAbispolicy();
		assertEquals(3, typeAndSubtypMap.size());

	}

	@Test(expected = BiometricRecordValidationException.class)
	public void testValidateBiomtericWithModalitiesNull()
			throws JsonParseException, JsonMappingException, BiometricRecordValidationException, IOException {
		abisHandlerUtil.validateBiometricRecord(null, null);

	}

	@Test(expected = BiometricRecordValidationException.class)
	public void testValidateBiomtericWithBiometricRecordNull()
			throws JsonParseException, JsonMappingException, BiometricRecordValidationException, IOException {
		List<String> modalities = Arrays.asList("Left Thumb", "Right Thumb", "Left MiddleFinger", "Left RingFinger",
				"Left LittleFinger", "Left IndexFinger", "Right MiddleFinger", "Right RingFinger", "Right LittleFinger",
				"Right IndexFinger", "Left", "Right", "Face");
		abisHandlerUtil.validateBiometricRecord(null, modalities);

	}

	@Test(expected = BiometricRecordValidationException.class)
	public void testValidateBiometricWithEmptyBDB()
			throws JsonParseException, JsonMappingException, BiometricRecordValidationException, IOException {
		List<String> modalities=Arrays.asList("Left Thumb", "Right Thumb", "Left MiddleFinger",
				"Left RingFinger", "Left LittleFinger", "Left IndexFinger", "Right MiddleFinger",
				"Right RingFinger", "Right LittleFinger", "Right IndexFinger", "Left", "Right", "Face");
		abisHandlerUtil.validateBiometricRecord(getBiometricRecord(modalities, true), modalities);

	}

	@Test
	public void testValidateBiometricSuccess()
			throws JsonParseException, JsonMappingException, BiometricRecordValidationException, IOException {
		List<String> modalities = Arrays.asList("Left Thumb", "Right Thumb", "Left MiddleFinger", "Left RingFinger",
				"Left LittleFinger", "Left IndexFinger", "Right MiddleFinger", "Right RingFinger", "Right LittleFinger",
				"Right IndexFinger", "Left", "Right", "Face");
		abisHandlerUtil.validateBiometricRecord(getBiometricRecord(modalities, false), modalities);

	}
}
