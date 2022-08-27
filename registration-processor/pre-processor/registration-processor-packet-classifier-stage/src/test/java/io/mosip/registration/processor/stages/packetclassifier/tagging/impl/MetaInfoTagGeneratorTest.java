package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class MetaInfoTagGeneratorTest {

	@InjectMocks
	private MetaInfoTagGenerator metaInfoTagGenerator;

    private static String operationsDataTagNamePrefix = "META_INFO-OPERATIONS_DATA-";
    private static String metaDataTagNamePrefix = "META_INFO-META_DATA-";
	private static String capturedRegisteredDevicesTagNamePrefix = 
						"META_INFO-CAPTURED_REGISTERED_DEVICES-";
	private static String notAvailableTagValue = "--TAG_VALUE_NOT_AVAILABLE--";

	private static List<String> operationsDataTagLabels;
	private static List<String> metaDataTagLabels;
	private static List<String> capturedRegisteredDeviceTypes;

	@Before
	public void setup() throws Exception {

		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagNamePrefix", 
			operationsDataTagNamePrefix);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagNamePrefix", metaDataTagNamePrefix);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDevicesTagNamePrefix", 
			capturedRegisteredDevicesTagNamePrefix);
		Whitebox.setInternalState(metaInfoTagGenerator, "notAvailableTagValue", notAvailableTagValue);


		operationsDataTagLabels = new ArrayList<>();
		metaDataTagLabels = new ArrayList<>();
		capturedRegisteredDeviceTypes = new ArrayList<>();

		Whitebox.setInternalState(metaInfoTagGenerator, "objectMapper", new ObjectMapper());
	}

	@Test
	public void testGenerateTagsForOperationsData() throws BaseCheckedException {
		operationsDataTagLabels.add("officerId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[ {\n  \"label\" : \"officerId\",\n  \"value\" : \"110119\"\n}]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals(tags.get(operationsDataTagNamePrefix + "officerId"), "110119");
	}
	
	@Test
	public void testGenerateTagsForOperationsDataNotAvailable() throws BaseCheckedException {
		operationsDataTagLabels.add("officerId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);
		
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals(tags.get(operationsDataTagNamePrefix + "officerId"), notAvailableTagValue);
	}
	
	@Test
	public void testGenerateTagsForMetaData() throws BaseCheckedException {
		metaDataTagLabels.add("centerId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[{\n  \"label\" : \"centerId\",\n  \"value\" : \"11016\"\n}]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals("11016", tags.get(metaDataTagNamePrefix + "centerId"));
	}
	
	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForMetaDataIOException() throws BaseCheckedException {

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA,
				"[ {\n  \"label\" : \"officerId\",\n  \"values\" : \"110119\"\n}]");
		metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	@Test
	public void testGenerateTagsForCapturedRegisteredDevices() throws BaseCheckedException {
		capturedRegisteredDeviceTypes.add("Finger");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[ {\n  \"deviceServiceVersion\" : \"0.9.5\",\n  \"digitalId\" : {\n    \"dateTime\" : \"2020-11-23T11:29:21.468+05:30\",\n    \"deviceSubType\" : \"Slap\",\n    \"model\" : \"SLAP01\",\n    \"type\" : \"Finger\",\n    \"make\" : \"MOSIP\",\n    \"serialNo\" : \"1234567890\",\n    \"deviceProviderId\" : \"MOSIP.PROXY.SBI\",\n    \"deviceProvider\" : \"MOSIP\"\n  },\n  \"deviceCode\" : \"b692b595-3523-slap-99fc-bd76e35f190f\"\n}]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals("MOSIP-SLAP01-1234567890", tags.get(capturedRegisteredDevicesTagNamePrefix + "Finger"));
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForOperationsDataEntryIsNull() throws BaseCheckedException {
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForMetaDataEntryIsNull() throws BaseCheckedException {
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForCapturedRegisteredDevicesEntryIsNull() 
		throws BaseCheckedException {
			Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
			Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
			Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
				capturedRegisteredDeviceTypes);
	
			Map<String, String> metaInfoMap = new HashMap<>();
			metaInfoMap.put(JsonConstant.METADATA, "[]");
			metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
			metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	@Test(expected = ParsingException.class)
	public void testGenerateTagsForInvalidJSONInMetaInfo() throws BaseCheckedException {
		operationsDataTagLabels.add("officerId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[ {\n  \"label\" : \"officerId\",\n  \"value\" : \"110119\"\n}");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		metaInfoTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	public void testGenerateTagsForOperationsDataLabelNotAvailable() throws BaseCheckedException {
		operationsDataTagLabels.add("supervisorId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[ {\n  \"label\" : \"officerId\",\n  \"value\" : \"110119\"\n}]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals(tags.get(operationsDataTagNamePrefix + "supervisorId"), notAvailableTagValue);
	}

	@Test
	public void testGenerateTagsForMetaDataLabelNotAvailable() throws BaseCheckedException {
		metaDataTagLabels.add("machineId");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[{\n  \"label\" : \"centerId\",\n  \"value\" : \"11016\"\n}]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(metaDataTagNamePrefix + "machineId"));
	}

	@Test
	public void testGenerateTagsForCapturedRegisteredDevicesLabelNotAvailable() throws BaseCheckedException {
		capturedRegisteredDeviceTypes.add("Face");
		Whitebox.setInternalState(metaInfoTagGenerator, "operationsDataTagLabels", operationsDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "metaDataTagLabels", metaDataTagLabels);
		Whitebox.setInternalState(metaInfoTagGenerator, "capturedRegisteredDeviceTypes", 
			capturedRegisteredDeviceTypes);

		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.OPERATIONSDATA, "[]");
		metaInfoMap.put(JsonConstant.METADATA, "[]");
		metaInfoMap.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "[ {\n  \"deviceServiceVersion\" : \"0.9.5\",\n  \"digitalId\" : {\n    \"dateTime\" : \"2020-11-23T11:29:21.468+05:30\",\n    \"deviceSubType\" : \"Slap\",\n    \"model\" : \"SLAP01\",\n    \"type\" : \"Finger\",\n    \"make\" : \"MOSIP\",\n    \"serialNo\" : \"1234567890\",\n    \"deviceProviderId\" : \"MOSIP.PROXY.SBI\",\n    \"deviceProvider\" : \"MOSIP\"\n  },\n  \"deviceCode\" : \"b692b595-3523-slap-99fc-bd76e35f190f\"\n}]");
		Map<String, String> tags = metaInfoTagGenerator.generateTags("12345", "1234", "NEW", 
			null, metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(capturedRegisteredDevicesTagNamePrefix + "Face"));
	}
	
	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = metaInfoTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(result, null);
	}

}
