package io.mosip.registration.processor.transaction.api.transaction.api.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.dto.RegistrationTransactionDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.exception.RegTransactionAppException;
import io.mosip.registration.processor.status.exception.TransactionsUnavailableException;
import io.mosip.registration.processor.status.service.TransactionService;
import io.mosip.registration.processor.transaction.api.controller.RegistrationTransactionController;
import io.mosip.registration.processor.transaction.api.transaction.api.config.RegistrationTransactionBeanConfigTest;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationTransactionBeanConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
@Ignore
public class RegistrationTransactionControllerTest {
	@InjectMocks
	RegistrationTransactionController registrationTransactionController;

	@MockBean
	TransactionService<TransactionDto> transactionService;

	@Mock
	private Environment env;

	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;

	@MockBean
	private DigitalSignatureUtility digitalSignatureUtility;

	@Autowired
	private MockMvc mockMvc;

	@Before
	public void setUp() {
		Mockito.doReturn("").when(digitalSignatureUtility).getDigitalSignature(ArgumentMatchers.any());
	}

	@WithUserDetails("reg-admin")
	@Test
	public void testSyncController() throws Exception {
		List<RegistrationTransactionDto> dtoList = new ArrayList<>();
		dtoList.add(new RegistrationTransactionDto("id", "registrationId", "transactionTypeCode", "parentTransactionId",
				"statusCode", "subStatusCode", "statusComment", null));
		Mockito.when(transactionService.getTransactionByRegId(ArgumentMatchers.any()))
				.thenReturn(dtoList);
		this.mockMvc.perform(get("/search/27847657360002520190320095010").accept(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());
	}

	@WithUserDetails("reg-admin")
	@Test
	public void testTransactionsUnavailableException() throws Exception {

		Mockito.doThrow(new TransactionsUnavailableException("", "")).when(transactionService)
				.getTransactionByRegId(ArgumentMatchers.any());
		this.mockMvc.perform(get("/search/27847657360002520190320095010").accept(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());
	}

	@WithUserDetails("reg-admin")
	@Test
	public void testRegTransactionAppException() throws Exception {

		Mockito.doThrow(new RegTransactionAppException("", "")).when(transactionService)
				.getTransactionByRegId(ArgumentMatchers.any());
		this.mockMvc.perform(get("/search/27847657360002520190320095010").accept(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk());
	}

	@WithUserDetails("reg-admin")
	@Test
	public void testUnknownException() throws Exception {

		Mockito.doThrow(new NullPointerException()).when(transactionService)
				.getTransactionByRegId(ArgumentMatchers.any());
		this.mockMvc.perform(get("/search/27847657360002520190320095010").accept(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk()).andExpect(jsonPath("$.errors[0].errorCode", is("RPR-RTS-002")));
	}
	
}
