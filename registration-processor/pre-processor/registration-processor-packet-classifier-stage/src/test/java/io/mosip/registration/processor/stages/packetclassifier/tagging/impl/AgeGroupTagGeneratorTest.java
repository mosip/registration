package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
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

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class AgeGroupTagGeneratorTest {

	@InjectMocks
	private AgeGroupTagGenerator ageGroupTagGenerator;

	@Mock
	private Utilities utility;

	private static String tagName = "AGE_GROUP";
	Map<String,String> ageGroupRangeMap;

	@Before
	public void setup() throws Exception {

		ageGroupRangeMap = new HashMap<>();
		ageGroupRangeMap.put("CHILD", "0-17");
		ageGroupRangeMap.put("ADULT", "18-59");
		ageGroupRangeMap.put("SENIOR_CITIZEN", "60-200");

		Whitebox.setInternalState(ageGroupTagGenerator, "tagName", tagName);
		Whitebox.setInternalState(ageGroupTagGenerator, "notAvailableTagValue", "--TAG_VALUE_NOT_AVAILABLE--");
		Whitebox.setInternalState(ageGroupTagGenerator, "ageGroupRangeMap", ageGroupRangeMap);

		Whitebox.invokeMethod(ageGroupTagGenerator, "generateParsedAgeGroupRangeMap");

	}

	@Test
	public void testGenerateTagsForChildGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenReturn(17);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("1234", "123", "NEW", null, null, 0);
		assertEquals(tags.get(tagName), "CHILD");
	}

	@Test
	public void testGenerateTagsForAdultGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenReturn(30);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("1234", "123", "NEW", null, null, 0);
		assertEquals(tags.get(tagName), "ADULT");
	}

	@Test
	public void testGenerateTagsForSeniorCitizenGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenReturn(65);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("1234", "123", "NEW", null, null, 0);
		assertEquals(tags.get(tagName), "SENIOR_CITIZEN");
	}
	
	@Test
	public void testGenerateTagsForLostPacket() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenReturn(-1);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("1234", "123", "LOST", null, null, 0);
		assertEquals(tags.get(tagName), "--TAG_VALUE_NOT_AVAILABLE--");
	}
	
	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForAgeGroupNotFound() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenReturn(201);
		ageGroupTagGenerator.generateTags("1234", "123", "LOST", null, null, 0);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForUtilityThrowningIOException() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString(), any())).thenThrow(new IOException());
		ageGroupTagGenerator.generateTags("1234", "123", "NEW", null, null, 0);
	}
	
	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = ageGroupTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(result, null);
	}
	
}
