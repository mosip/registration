package io.mosip.registration.processor.status.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import javax.servlet.http.Cookie;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.registration.processor.core.auth.dto.AuthRequestDTO;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.ResponseDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.status.api.config.RegistrationStatusConfigTest;
import io.mosip.registration.processor.status.service.InternalAuthDelegateService;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationStatusConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class InternalAuthDelegateServicesControllerTest {

	@InjectMocks
	InternalAuthDelegateServicesController internalAuthDelegateServicesController = new InternalAuthDelegateServicesController();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@MockBean
	private InternalAuthDelegateService internalAuthDelegateService;

	@MockBean
	private RegistrationStatusServiceImpl registrationStatusService;

	@MockBean
	SyncRegistrationServiceImpl syncRegistrationService;

	Gson gson = new GsonBuilder().serializeNulls().create();

	AuthRequestDTO authRequestDTO = new AuthRequestDTO();
	AuthResponseDTO authResponse = new AuthResponseDTO();
	ResponseDTO responseDto = new ResponseDTO();

	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {

		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		authRequestDTO.setEnv("Staging");
		authRequestDTO.setIndividualId("45128164920495");
		authRequestDTO.setIndividualIdType("UIN");
		authRequestDTO.setRequest("BFijjscahGoaaol");

		responseDto.setAuthStatus(true);
		authResponse.setId("");
		authResponse.setResponse(responseDto);
	}

	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void testAuthDelegateController() throws Exception {

		String authRequestDTOJson = gson.toJson(authRequestDTO);
		Mockito.when(internalAuthDelegateService.authenticate(any(), any())).thenReturn(authResponse);
		MvcResult result = this.mockMvc
				.perform(post("/auth").accept(MediaType.APPLICATION_JSON_VALUE)
						.header("timestamp", "2019-05-07T05:13:55.704Z").contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(authRequestDTOJson.getBytes()).cookie(new Cookie("Authorization", authRequestDTOJson)))
				.andExpect(status().isOk()).andReturn();
		String resultContent = result.getResponse().getContentAsString();
		JSONObject object = (JSONObject) new JSONParser().parse(resultContent);
		JSONObject responseDto = (JSONObject) object.get("response");
		assertEquals(responseDto.get("authStatus").toString(), "true");
	}

	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void testAuthDelegateFailureController() throws Exception {

		String authRequestDTOJson = gson.toJson(authRequestDTO);
		Mockito.when(internalAuthDelegateService.authenticate(any(), any())).thenReturn(authResponse);
		this.mockMvc.perform(post("/auth").accept(MediaType.APPLICATION_JSON_VALUE)
				.header("timestamp", "2019-05-07T05:13:55.704Z").contentType(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", authRequestDTOJson))).andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void getCertificateTestController() throws Exception {

		Mockito.when(internalAuthDelegateService.getCertificate(anyString(), any(), any())).thenReturn("");
		this.mockMvc
				.perform(get("/getCertificate").accept(MediaType.APPLICATION_JSON_VALUE)
						.header("timestamp", "2019-05-07T05:13:55.704Z").contentType(MediaType.APPLICATION_JSON_VALUE)
						.param("applicationId", "REG_CLIENT").param("referenceId", "KERNEL"))
				.andExpect(status().isOk());
	}

}
