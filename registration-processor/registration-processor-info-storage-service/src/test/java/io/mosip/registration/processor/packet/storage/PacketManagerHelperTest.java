package io.mosip.registration.processor.packet.storage;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.packet.storage.helper.PacketManagerHelper;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@RunWith(MockitoJUnitRunner.class)
public class PacketManagerHelperTest {

	@InjectMocks
	PacketManagerHelper packetManagerHelper;
	@Mock
	private Utilities utilities;
	
	@Before
	public void setup() {
		ReflectionTestUtils.setField(packetManagerHelper, "defaultSource", "REGISTRATION_CLIENT");
	}
	@Test
	public void isFieldPresentTest() throws IOException {
		Map<String,String> providerConfiguration=new HashMap<>();
		LinkedHashMap<String,Object> obj=new LinkedHashMap<>();
		obj.put("value", "individualBiometrics");
		LinkedHashMap<String,Object> obj1=new LinkedHashMap<>();
		obj1.put("individualBiometrics", obj);
		
		Mockito.when(utilities.getRegistrationProcessorMappingJson(
                Mockito.anyString())).thenReturn(new JSONObject(obj1));
		providerConfiguration.put("uingenerator.individualBiometrics[Finger]", "source:EXT_PACKET\\/process:NEW|UPDATE|LOST|BIOMETRIC_CORRECTION,source:REGISTRATION_CLIENT\\/process:NEW|UPDATE|LOST,source:RESIDENT\\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT");
		assertTrue(packetManagerHelper.isFieldPresent("individualBiometrics",ProviderStageName.UIN_GENERATOR, providerConfiguration));
	}
	
}
