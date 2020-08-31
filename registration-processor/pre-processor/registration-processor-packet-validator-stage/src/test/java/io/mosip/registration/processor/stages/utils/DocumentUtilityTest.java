package io.mosip.registration.processor.stages.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class DocumentUtilityTest.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ IOUtils.class, Utilities.class })
public class DocumentUtilityTest {

	/** The reg processor identity json. */
	@Mock
	private JSONObject regProcessorIdentityJson;

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	/** The filesystem adapter impl. */
	@Mock
	private PacketManager packetManager;

	/** The utility. */
	@Mock
	private Utilities utility;

	/** The document utility. */
	@InjectMocks
	DocumentUtility documentUtility = new DocumentUtility();

	/**
	 * Test structural validation success.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testDocumentUtility() throws Exception {
		ObjectMapper objMapper = new ObjectMapper();
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream is = new FileInputStream(file);
		String identityMapping = IOUtils.toString(is);
		JSONObject identityMappingJson = JsonUtil.getJSONObject(objMapper.readValue(identityMapping, JSONObject.class),
				"identity");
		;
		Mockito.when(utility.getRegistrationProcessorMappingJson()).thenReturn(identityMappingJson);
		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn(MappingJsonConstants.IDENTITY);
		FileInputStream fstream = new FileInputStream("src/test/resources/ID.json");
		byte[] bytes = IOUtils.toByteArray(fstream);
		String docCat = null;
		List<Document> documentList = documentUtility.getDocumentList(bytes);
		for (Document docObjects : documentList) {
			if (docObjects.getDocumentCategory() != null)
				docCat = docObjects.getDocumentCategory();
			break;
		}
		assertEquals("Comparing the first Document Category", "proofOfAddress", docCat);
	}

}
