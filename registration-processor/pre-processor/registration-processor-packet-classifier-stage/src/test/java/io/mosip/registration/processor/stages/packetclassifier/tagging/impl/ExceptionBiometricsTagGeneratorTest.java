package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

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
public class ExceptionBiometricsTagGeneratorTest {

	private static String tagName = "EXCEPTION_BIOMETRICS";

	@InjectMocks
	private ExceptionBiometricsTagGenerator exceptionBiometricsTagGenerator;

	@Before
	public void setup() throws Exception {
		Whitebox.setInternalState(exceptionBiometricsTagGenerator, "tagName", tagName);
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailable() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, 
			"[{\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}]");
		Map<String, String> tags = 
		exceptionBiometricsTagGenerator.generateTags("1234", "NEW", null, metaInfoMap);
		assertEquals("true", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsNotAvailable() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "[]");
		Map<String, String> tags = 
		exceptionBiometricsTagGenerator.generateTags("1234", "NEW", null, metaInfoMap);
		assertEquals("false", tags.get(tagName));
	}

	@Test(expected = ParsingException.class)
	public void testGenerateTagsForMetaInfoMapExceptionBiometricsContainingInvalidJSON() 
		throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, 
			"\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}]");
		exceptionBiometricsTagGenerator.generateTags("1234", "NEW", null, metaInfoMap);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForMetaInfoMapDoesNotContainExceptionBiometrics() 
		throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		exceptionBiometricsTagGenerator.generateTags("1234", "NEW", null, metaInfoMap);
	}
	
}
