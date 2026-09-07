package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.anonymous.dto.AnonymousProfileDTO;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*",
		"javax.xml.*", "org.xml.*" })
public class AnonymousProfileTagGeneratorTest {

	private static final String VALUE = "value";
	private static final String TAG_NAME = "ANONYMOUS";
	private static final String PROFILE_JSON = "{\"gender\":\"Female\"}";
	private static final String WORKFLOW_INSTANCE_ID = "8e34c5d5-2ba1-4d69-9e60-31b0a1d1c1d0";
	private static final String REGISTRATION_ID = "10001100010000120260824";

	/**
	 * The profile as AnonymousProfileServiceImpl builds it: supervisorId comes from
	 * the packet operationsData, the decision and comment are not in the packet at
	 * all and start out null.
	 */
	private static final String SUPERVISOR_PROFILE_JSON = "{\"processName\":\"NEW\",\"status\":\"PROCESSING\","
			+ "\"assisted\":[\"110024\",\"SUP001\"],\"supervisorId\":\"SUP001\","
			+ "\"supervisorDecision\":null,\"supervisorComment\":null}";

	@InjectMocks
	private AnonymousProfileTagGenerator anonymousProfileTagGenerator;

	@Mock
	private Utilities utility;

	@Mock
	private AnonymousProfileService anonymousProfileService;

	@Mock
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	private List<String> mappingFieldNames;

	@Before
	public void setup() throws Exception {
		mappingFieldNames = new ArrayList<>(Arrays.asList("dob", "gender", "email", "phone",
				"preferredLanguage", "locationHierarchyForProfiling"));
		Whitebox.setInternalState(anonymousProfileTagGenerator, "tagName", TAG_NAME);
		Whitebox.setInternalState(anonymousProfileTagGenerator, "mappingFieldNames", mappingFieldNames);

		JSONObject mappingJSON = new JSONObject();
		putMapping(mappingJSON, "dob", "dateOfBirth");
		putMapping(mappingJSON, "gender", "gender");
		putMapping(mappingJSON, "email", "email");
		putMapping(mappingJSON, "phone", "phone");
		putMapping(mappingJSON, "preferredLanguage", "preferredLang");
		putMapping(mappingJSON, "locationHierarchyForProfiling", "zone,postalCode");
		when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(mappingJSON);
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesResolvesMappingKeysAndSplitsLocation()
			throws BaseCheckedException {
		List<String> requiredIdObjectFieldNames = anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();

		Set<String> expected = new HashSet<>(Arrays.asList("dateOfBirth", "gender", "email", "phone",
				"preferredLang", "zone", "postalCode"));
		assertEquals(expected, new HashSet<>(requiredIdObjectFieldNames));
		assertEquals(expected.size(), requiredIdObjectFieldNames.size());
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesForEmptyMappingFieldNames() throws BaseCheckedException {
		Whitebox.setInternalState(anonymousProfileTagGenerator, "mappingFieldNames", new ArrayList<>());
		List<String> requiredIdObjectFieldNames = anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(0, requiredIdObjectFieldNames.size());
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForFieldNotAvailableInMappingJSON()
			throws BaseCheckedException {
		List<String> fieldNames = new ArrayList<>(Arrays.asList("dob", "unknownField"));
		Whitebox.setInternalState(anonymousProfileTagGenerator, "mappingFieldNames", fieldNames);
		anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForUtilityThrowingIOException()
			throws BaseCheckedException, IOException {
		when(utility.getRegistrationProcessorMappingJson(anyString())).thenThrow(new IOException());
		anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test
	public void testGenerateTagsUsesPrefetchedFieldsAndDoesNotCallGetFields() throws Exception {
		BiometricRecord biometricRecord = new BiometricRecord();
		when(priorityBasedPacketManagerService.getBiometrics(eq("1234"),
				eq(MappingJsonConstants.INDIVIDUAL_BIOMETRICS), eq("NEW"),
				eq(ProviderStageName.CLASSIFICATION))).thenReturn(biometricRecord);
		when(anonymousProfileService.buildJsonStringFromPacketInfo(eq(biometricRecord), any(), any(),
				any(), anyString(), anyString())).thenReturn(PROFILE_JSON);

		Map<String, FieldDTO> idObjectFieldDTOMap = new HashMap<>();
		idObjectFieldDTOMap.put("dateOfBirth", new FieldDTO("string", "1998/01/01"));
		idObjectFieldDTOMap.put("gender", new FieldDTO("simpleType", "[{\"language\":\"eng\",\"value\":\"Female\"}]"));
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put("metaData", "[{\"label\":\"registrationType\",\"value\":\"NEW\"}]");

		Map<String, String> tags = anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				idObjectFieldDTOMap, metaInfoMap, 0);

		assertEquals(1, tags.size());
		assertEquals(PROFILE_JSON, tags.get(TAG_NAME));
		verify(priorityBasedPacketManagerService, never()).getFields(any(), any(), any(), any());
		verify(priorityBasedPacketManagerService, times(1)).getBiometrics(eq("1234"),
				eq(MappingJsonConstants.INDIVIDUAL_BIOMETRICS), eq("NEW"),
				eq(ProviderStageName.CLASSIFICATION));
		verify(anonymousProfileService, times(1)).buildJsonStringFromPacketInfo(eq(biometricRecord),
				any(), any(), eq(metaInfoMap), anyString(), anyString());
	}

	@Test
	public void testGenerateTagsReturnsEmptyMapWhenProfileBuildFails() throws Exception {
		when(priorityBasedPacketManagerService.getBiometrics(any(), any(), any(), any()))
				.thenReturn(new BiometricRecord());
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(),
				anyString(), anyString())).thenThrow(new RuntimeException("profile failed"));

		Map<String, String> tags = anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				new HashMap<>(), new HashMap<>(), 0);

		assertTrue(tags.isEmpty());
		verify(priorityBasedPacketManagerService, never()).getFields(any(), any(), any(), any());
	}

	@Test
	public void testGenerateTagsReturnsEmptyMapWhenBiometricsFail() throws Exception {
		when(priorityBasedPacketManagerService.getBiometrics(any(), any(), any(), any()))
				.thenThrow(new RuntimeException("biometrics failed"));

		Map<String, String> tags = anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				new HashMap<>(), new HashMap<>(), 0);

		assertTrue(tags.isEmpty());
		verify(anonymousProfileService, never()).buildJsonStringFromPacketInfo(any(), any(), any(), any(),
				anyString(), anyString());
	}

	@Test
	public void testGenerateTagsReturnsEmptyMapWhenProfileJsonNull() throws Exception {
		when(priorityBasedPacketManagerService.getBiometrics(any(), any(), any(), any()))
				.thenReturn(new BiometricRecord());
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(),
				anyString(), anyString())).thenReturn(null);

		assertTrue(anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				new HashMap<>(), new HashMap<>(), 0).isEmpty());
	}

	@Test
	public void testGenerateTagsReturnsEmptyMapWhenProfileJsonEmpty() throws Exception {
		when(priorityBasedPacketManagerService.getBiometrics(any(), any(), any(), any()))
				.thenReturn(new BiometricRecord());
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(),
				anyString(), anyString())).thenReturn("");

		assertTrue(anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				new HashMap<>(), new HashMap<>(), 0).isEmpty());
	}

	@Test
	public void testGenerateTagsSkipsNullFieldType() throws Exception {
		BiometricRecord biometricRecord = new BiometricRecord();
		when(priorityBasedPacketManagerService.getBiometrics(any(), any(), any(), any()))
				.thenReturn(biometricRecord);
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(),
				anyString(), anyString())).thenReturn(PROFILE_JSON);

		Map<String, FieldDTO> idObjectFieldDTOMap = new HashMap<>();
		idObjectFieldDTOMap.put("dateOfBirth", new FieldDTO(null, "1998/01/01"));
		idObjectFieldDTOMap.put("gender", new FieldDTO("simpleType", "[{\"language\":\"eng\",\"value\":\"Female\"}]"));

		Map<String, String> tags = anonymousProfileTagGenerator.generateTags("wf-1", "1234", "NEW",
				idObjectFieldDTOMap, new HashMap<>(), 0);

		assertEquals(PROFILE_JSON, tags.get(TAG_NAME));
		verify(anonymousProfileService).buildJsonStringFromPacketInfo(eq(biometricRecord), any(), any(),
				any(), anyString(), anyString());
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesSkipsEmptyLocationTokens() throws Exception {
		JSONObject mappingJSON = new JSONObject();
		putMapping(mappingJSON, "dob", "dateOfBirth");
		putMapping(mappingJSON, "gender", "gender");
		putMapping(mappingJSON, "email", "email");
		putMapping(mappingJSON, "phone", "phone");
		putMapping(mappingJSON, "preferredLanguage", "preferredLang");
		putMapping(mappingJSON, "locationHierarchyForProfiling", "zone, ,postalCode");
		when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(mappingJSON);

		List<String> requiredIdObjectFieldNames = anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();

		Set<String> expected = new HashSet<>(Arrays.asList("dateOfBirth", "gender", "email", "phone",
				"preferredLang", "zone", "postalCode"));
		assertEquals(expected, new HashSet<>(requiredIdObjectFieldNames));
		assertEquals(expected.size(), requiredIdObjectFieldNames.size());
	}

	@Test
	public void supervisorDecisionAndCommentAreAddedFromRegistrationListTest() throws Exception {
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setSupervisorStatus("APPROVED");
		syncRegistrationEntity.setSupervisorComment("Verified by supervisor");
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID))
				.thenReturn(syncRegistrationEntity);

		AnonymousProfileDTO profile = taggedProfile(generateSupervisorTags());

		assertEquals("APPROVED", profile.getSupervisorDecision());
		assertEquals("Verified by supervisor", profile.getSupervisorComment());
		// the packet-sourced field must survive the enrichment round trip
		assertEquals("SUP001", profile.getSupervisorId());
	}

	@Test
	public void rejectedDecisionIsCarriedThroughTest() throws Exception {
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setSupervisorStatus("REJECTED");
		syncRegistrationEntity.setSupervisorComment("Poor biometric quality");
		when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
				.thenReturn(syncRegistrationEntity);

		AnonymousProfileDTO profile = taggedProfile(generateSupervisorTags());

		assertEquals("REJECTED", profile.getSupervisorDecision());
		assertEquals("Poor biometric quality", profile.getSupervisorComment());
	}

	/** A packet with no registration_list record must still get its profile tagged. */
	@Test
	public void profileIsStillTaggedWhenSyncRecordIsMissingTest() throws Exception {
		when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(null);

		Map<String, String> tags = generateSupervisorTags();

		assertEquals(SUPERVISOR_PROFILE_JSON, tags.get(TAG_NAME));
		assertNull(taggedProfile(tags).getSupervisorDecision());
		assertNull(taggedProfile(tags).getSupervisorComment());
	}

	/**
	 * The lookup is only attempted once a profile exists, and a build failure still
	 * leaves classification unblocked with no tag - the workflow manager then falls
	 * back to building the profile from the packet.
	 */
	@Test
	public void noTagAndNoLookupWhenProfileBuildFailsTest() throws Exception {
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString())).thenThrow(new RuntimeException("profile build failed"));

		assertTrue(anonymousProfileTagGenerator.generateTags(WORKFLOW_INSTANCE_ID, REGISTRATION_ID, "NEW",
				new HashMap<>(), null, 0).isEmpty());
		verify(syncRegistrationService, never()).findByWorkflowInstanceId(anyString());
	}

	/**
	 * A registration_list read failure costs the two supervisor fields, not the
	 * profile - the tag is still written with the JSON as it was built from the
	 * packet, exactly as a biometrics fetch failure only drops the biometrics.
	 */
	@Test
	public void profileIsStillTaggedWhenSupervisorLookupFailsTest() throws Exception {
		when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
				.thenThrow(new RuntimeException("registration_list unavailable"));

		Map<String, String> tags = generateSupervisorTags();

		assertEquals(SUPERVISOR_PROFILE_JSON, tags.get(TAG_NAME));
		assertEquals("SUP001", taggedProfile(tags).getSupervisorId());
		assertNull(taggedProfile(tags).getSupervisorDecision());
		assertNull(taggedProfile(tags).getSupervisorComment());
	}

	private Map<String, String> generateSupervisorTags() throws Exception {
		when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString())).thenReturn(SUPERVISOR_PROFILE_JSON);
		return anonymousProfileTagGenerator.generateTags(WORKFLOW_INSTANCE_ID, REGISTRATION_ID, "NEW",
				new HashMap<>(), null, 0);
	}

	private AnonymousProfileDTO taggedProfile(Map<String, String> tags) throws Exception {
		return JsonUtil.readValueWithUnknownProperties(tags.get(TAG_NAME), AnonymousProfileDTO.class);
	}

	private void putMapping(JSONObject mappingJSON, String mappingKey, String actualFieldName) {
		LinkedHashMap<String, String> internalJSON = new LinkedHashMap<>();
		internalJSON.put(VALUE, actualFieldName);
		mappingJSON.put(mappingKey, internalJSON);
	}
}
