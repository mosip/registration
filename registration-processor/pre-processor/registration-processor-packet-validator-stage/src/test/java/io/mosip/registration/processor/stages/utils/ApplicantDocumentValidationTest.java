package io.mosip.registration.processor.stages.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class ApplicantDocumentValidationTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class,JsonUtil.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class ApplicantDocumentValidationTest {

	/** The utility. */
	@Mock
	Utilities utility;

	@Mock PacketReaderService packetReaderService;

	@Mock IdSchemaUtils idSchemaUtils;

	@InjectMocks
	private ApplicantDocumentValidation applicantDocumentValidation;

	JSONObject regProcessorIdentityJson=mock(JSONObject.class);
	JSONObject documentLabel=mock(JSONObject.class);
	String label="label";
	String source="source";
	JSONObject demographicIdentityJSONObject=mock(JSONObject.class);
	JSONObject proofOfDocument;

	/**
	 * Sets the up.
	 *
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 * @throws Exception
	 */
	@Before
	public void setUp()
			throws Exception {
		Map<String,String> map=new HashMap<>();
		map.put("value", "documentValue");
		proofOfDocument=new JSONObject(map);
		when(utility.getRegistrationProcessorMappingJson()).thenReturn(regProcessorIdentityJson);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any())
				.thenReturn(proofOfDocument);
		PowerMockito.when(JsonUtil.class, "getJSONValue", any(), anyString())
		.thenReturn(label);
		when(utility.getDemographicIdentityJSONObject(any(),anyString())).thenReturn(demographicIdentityJSONObject);

		when(idSchemaUtils.getSource(anyString(), anyDouble())).thenReturn(source);
		when(packetReaderService.checkFileExistence(anyString(),anyString(),anyString())).thenReturn(true);
		
	}

	/**
	 * Test applicant document validation  success.
	 *
	 * @throws Exception   exception
	 *
	 */
	@Test
	public void testApplicantDocumentValidationSuccess() throws Exception {
		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234");
		assertTrue("Test for successful Applicant Document Validation success for adult", isApplicantDocumentValidated);
	}

	/**
	 * Test applicant document validation  success.
	 *
	 * @throws Exception   exception
	 *
	 */
	@Test
	public void testApplicantDocumentValidationFailure() throws Exception {
		when(packetReaderService.checkFileExistence(anyString(),anyString(),anyString())).thenReturn(false);
		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234");
		assertFalse(isApplicantDocumentValidated);
	}


}
