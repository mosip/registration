package io.mosip.registration.processor.stages.packetclassifier.utility;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * The Class PacketClassifierUtilityTest.
 *
 * @author Satish Gohil
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class PacketClassifierUtilityTest {
	
	@InjectMocks
	PacketClassifierUtility packetClassifierUtility;
	
	private static List<String> mandatoryLanguages;
	private static List<String> optionalLanguages;
	
	@Test
	public void getLanguageBasedValueForSimpleTypeSuccess() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		mandatoryLanguages.add("eng");
		mandatoryLanguages.add("ara");
		
		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("fra");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"eng\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		assertEquals("Male", packetClassifierUtility.getLanguageBasedValueForSimpleType(value));
	}
	
	@Test
	public void getLanguageBasedValueForSimpleTypeFirstLangValueNull() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		mandatoryLanguages.add("eng");
		mandatoryLanguages.add("ara");

		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("fra");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);

		String value = "[{\"language\" : \"eng\", \"value\" : null}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		assertEquals(null, packetClassifierUtility.getLanguageBasedValueForSimpleType(value));
	}
	
	@Test(expected = BaseCheckedException.class)
	public void getLanguageBasedValueForSimpleTypeFirstLangNotAvailable() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		mandatoryLanguages.add("eng");
		mandatoryLanguages.add("ara");
		
		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("fra");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"fra\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		packetClassifierUtility.getLanguageBasedValueForSimpleType(value);
	}
	
	@Test
	public void getLanguageBasedValueForSimpleTypeOptionalFirstLangAvailable() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		
		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("eng");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"eng\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		assertEquals("Male", packetClassifierUtility.getLanguageBasedValueForSimpleType(value));
	}
	
	@Test
	public void getLanguageBasedValueForSimpleTypeOptionalFirstLangValueNull() throws Exception {

		mandatoryLanguages = new ArrayList<String>();

		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("eng");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);

		String value = "[{\"language\" : \"eng\", \"value\" : null}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		assertEquals(null, packetClassifierUtility.getLanguageBasedValueForSimpleType(value));
	}
	
	@Test
	public void getLanguageBasedValueForSimpleTypeOptionalSecondLangAvailable() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		
		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("eng");
		optionalLanguages.add("fra");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"fra\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		assertEquals("Male", packetClassifierUtility.getLanguageBasedValueForSimpleType(value));
	}
	
	@Test(expected = BaseCheckedException.class)
	public void getLanguageBasedValueForSimpleTypeEmptyLangForBoth() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		
		optionalLanguages = new ArrayList<String>();
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"fra\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		packetClassifierUtility.getLanguageBasedValueForSimpleType(value);
	}
	
	@Test(expected = BaseCheckedException.class)
	public void getLanguageBasedValueForSimpleTypeValueNotAvailableInBoth() throws Exception {

		mandatoryLanguages = new ArrayList<String>();
		mandatoryLanguages.add("eng");
		
		optionalLanguages = new ArrayList<String>();
		optionalLanguages.add("fra");
		Whitebox.setInternalState(packetClassifierUtility, "mandatoryLanguages", mandatoryLanguages);
		Whitebox.setInternalState(packetClassifierUtility, "optionalLanguages", optionalLanguages);
		
		String value = "[{\"language\" : \"tam\", \"value\" : \"Male\"}, {\"language\" : \"ara\",\"value\" : \"ذكر\"}]";
		packetClassifierUtility.getLanguageBasedValueForSimpleType(value);
	}

}
