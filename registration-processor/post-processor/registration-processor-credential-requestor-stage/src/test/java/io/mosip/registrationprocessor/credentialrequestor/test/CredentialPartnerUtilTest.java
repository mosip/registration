package io.mosip.registrationprocessor.credentialrequestor.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartnersList;
import io.mosip.registration.processor.credentialrequestor.util.CredentialPartnerUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.*" })
public class CredentialPartnerUtilTest {

	@InjectMocks
	private CredentialPartnerUtil credentialPartnerUtil;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(credentialPartnerUtil, "configServerFileStorageURL", "http://config/");
		ReflectionTestUtils.setField(credentialPartnerUtil, "partnerProfileFileName", "partners.json");
	}

	@Test(expected = RegistrationProcessorCheckedException.class)
	public void loadPartnerDetails_GetJsonThrows_WrapsAsCheckedException() throws Exception {
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.getJson(anyString(), anyString())).thenThrow(new RuntimeException("config down"));

		credentialPartnerUtil.loadPartnerDetails();
	}

	@Test
	public void getAllCredentialPartners_ReloadsWhenNull() throws Exception {
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.getJson(anyString(), anyString())).thenReturn("{\"partners\":[]}");

		CredentialPartnersList result = credentialPartnerUtil.getAllCredentialPartners();

		assertNotNull(result);
		assertSame(result, credentialPartnerUtil.getAllCredentialPartners());
	}
}
