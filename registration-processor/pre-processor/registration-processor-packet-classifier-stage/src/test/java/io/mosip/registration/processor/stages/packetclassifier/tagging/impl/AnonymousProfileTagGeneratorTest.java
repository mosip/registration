package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.registration.processor.core.anonymous.dto.AnonymousProfileDTO;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

/**
 * The Class AnonymousProfileTagGeneratorTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*",
	"javax.xml.*", "org.xml.*" })
public class AnonymousProfileTagGeneratorTest {

	private static final String TAG_NAME = "anonymous";

	private static final String WORKFLOW_INSTANCE_ID = "8e34c5d5-2ba1-4d69-9e60-31b0a1d1c1d0";

	private static final String REGISTRATION_ID = "10001100010000120260824";

	/**
	 * The profile as AnonymousProfileServiceImpl builds it: supervisorId comes from
	 * the packet operationsData, the decision and comment are not in the packet at
	 * all and start out null.
	 */
	private static final String PROFILE_JSON = "{\"processName\":\"NEW\",\"status\":\"PROCESSING\","
			+ "\"assisted\":[\"110024\",\"SUP001\"],\"supervisorId\":\"SUP001\","
			+ "\"supervisorDecision\":null,\"supervisorComment\":null}";

	@InjectMocks
	private AnonymousProfileTagGenerator anonymousProfileTagGenerator;

	@Mock
	private AnonymousProfileService anonymousProfileService;

	@Mock
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Before
	public void setup() throws Exception {
		Whitebox.setInternalState(anonymousProfileTagGenerator, "tagName", TAG_NAME);
		Mockito.when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString())).thenReturn(PROFILE_JSON);
	}

	private Map<String, String> generateTags() throws Exception {
		return anonymousProfileTagGenerator.generateTags(WORKFLOW_INSTANCE_ID, REGISTRATION_ID, "NEW",
				new HashMap<>(), null, 0);
	}

	private AnonymousProfileDTO taggedProfile(Map<String, String> tags) throws Exception {
		return JsonUtil.readValueWithUnknownProperties(tags.get(TAG_NAME), AnonymousProfileDTO.class);
	}

	@Test
	public void supervisorDecisionAndCommentAreAddedFromRegistrationListTest() throws Exception {
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setSupervisorStatus("APPROVED");
		syncRegistrationEntity.setSupervisorComment("Verified by supervisor");
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID))
			.thenReturn(syncRegistrationEntity);

		AnonymousProfileDTO profile = taggedProfile(generateTags());

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
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
			.thenReturn(syncRegistrationEntity);

		AnonymousProfileDTO profile = taggedProfile(generateTags());

		assertEquals("REJECTED", profile.getSupervisorDecision());
		assertEquals("Poor biometric quality", profile.getSupervisorComment());
	}

	/** A packet with no registration_list record must still get its profile tagged. */
	@Test
	public void profileIsStillTaggedWhenSyncRecordIsMissingTest() throws Exception {
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString())).thenReturn(null);

		Map<String, String> tags = generateTags();

		assertEquals(PROFILE_JSON, tags.get(TAG_NAME));
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
		Mockito.when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString())).thenThrow(new RuntimeException("profile build failed"));

		assertTrue(generateTags().isEmpty());
		Mockito.verify(syncRegistrationService, Mockito.never()).findByWorkflowInstanceId(anyString());
	}

	/** A registration_list read failure must not break packet classification. */
	@Test
	public void classificationContinuesWhenSupervisorLookupFailsTest() throws Exception {
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
			.thenThrow(new RuntimeException("registration_list unavailable"));

		assertTrue(generateTags().isEmpty());
	}

	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();
		assertTrue(result.isEmpty());
	}

}
