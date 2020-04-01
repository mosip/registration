/**
 * 
 */
package io.mosip.registration.processor.bio.dedupe.api.controller;

import static org.mockito.ArgumentMatchers.any;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.mosip.registration.processor.bio.dedupe.api.config.BioDedupeConfigTest;
import io.mosip.registration.processor.bio.dedupe.api.config.BioDedupeSecurityConfig;
import io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;

/**
 * @author M1022006
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = BioDedupeConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@Import(BioDedupeSecurityConfig.class)
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class BioDedupeControllerTest {

	@InjectMocks
	private BioDedupeController bioDedupeController;

	@MockBean
	private BioDedupeService bioDedupeService;

	@Autowired
	private MockMvc mockMvc;



	@MockBean
	private DigitalSignatureUtility digitalSignatureUtility;

	String regId;

	byte[] cbeffFile;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(bioDedupeController, "isEnabled", true);

		regId = "1234";
		cbeffFile = regId.getBytes();


	}

	@Test
	@WithUserDetails("reg-processor")
	public void getFileSuccessTest() throws Exception {
		Mockito.when(bioDedupeService.getFileByAbisRefId("1234")).thenReturn(cbeffFile);
		Mockito.when(digitalSignatureUtility.getDigitalSignature(any())).thenReturn("abc");
		this.mockMvc
				.perform(MockMvcRequestBuilders.get("/biometricfile/1234").cookie(new Cookie("Authorization", "token"))
						.param("regId", regId).accept(MediaType.ALL_VALUE).contentType(MediaType.ALL_VALUE))

				.andExpect(MockMvcResultMatchers.status().isOk());

	}
}
