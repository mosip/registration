package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.ArrayList;
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

	private static String IDSchemaVersionLabel = "IDSchemaVersion";

	@InjectMocks
	private AgeGroupTagGenerator ageGroupTagGenerator;

	@Mock
	private Utilities utility;

	private static String tagName = "AGE_GROUP";
	private static List<String> ageGroupNames;
	private static List<Integer> ageBelowRanges;

	@Before
	public void setup() throws Exception {

		ageGroupNames = new ArrayList<String>();
		ageGroupNames.add("CHILD");
		ageGroupNames.add("ADULD");
		ageGroupNames.add("SENIOR_CITIZEN");

		ageBelowRanges = new ArrayList<Integer>();
		ageBelowRanges.add(18);
		ageBelowRanges.add(60);
		ageBelowRanges.add(200);

		Whitebox.setInternalState(ageGroupTagGenerator, "tagName", tagName);
		Whitebox.setInternalState(ageGroupTagGenerator, "ageGroupNames", ageGroupNames);
		Whitebox.setInternalState(ageGroupTagGenerator, "ageBelowRanges", ageBelowRanges);

	}

	@Test
	public void testGenerateTagsForChildGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString())).thenReturn(17);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("123", "NEW", null, null);
		assertEquals(tags.get(tagName), ageGroupNames.get(0));
	}

	@Test
	public void testGenerateTagsForAdultGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString())).thenReturn(30);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("123", "NEW", null, null);
		assertEquals(tags.get(tagName), ageGroupNames.get(1));
	}

	@Test
	public void testGenerateTagsForSeniorCitizenGroup() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString())).thenReturn(65);
		Map<String, String> tags = ageGroupTagGenerator.generateTags("123", "NEW", null, null);
		assertEquals(tags.get(tagName), ageGroupNames.get(2));
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForUtilityThrowningIOException() throws Exception {
		Mockito.when(utility.getApplicantAge(anyString(), anyString())).thenThrow(new IOException());
		Map<String, String> tags = ageGroupTagGenerator.generateTags("123", "NEW", null, null);
	}
	
}
