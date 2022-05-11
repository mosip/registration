package io.mosip.registration.processor.status.api.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.api.config.RegistrationStatusConfigTest;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.RegistrationExternalStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationExternalStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;
import io.mosip.registration.processor.status.validator.LostRidRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationExternalStatusRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationStatusRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationSyncRequestValidator;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationStatusConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class RegistrationExternalStatusControllerTest {

	@MockBean
	@Qualifier("selfTokenRestTemplate")
	private RestTemplate restTemplate;

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private WebApplicationContext webApplicationContext;

	@InjectMocks
	RegistrationExternalStatusController registrationExternalStatusController = new RegistrationExternalStatusController();

	@MockBean
	RegistrationExternalStatusRequestValidator registrationExternalStatusRequestValidator;

	@MockBean
	RegistrationStatusRequestValidator registrationStatusRequestValidator;

	/** The registration status service. */
	@MockBean
	RegistrationStatusServiceImpl registrationStatusService;

	@MockBean
	SyncRegistrationServiceImpl syncRegistrationService;
	
	@MockBean
	DigitalSignatureUtility digitalSignatureUtility;

	@MockBean
	LostRidRequestValidator lostRidRequestValidator;
	
	@MockBean
	RegistrationUtility registrationUtility;

	@MockBean
	private RegistrationSyncRequestValidator syncrequestvalidator;

	@Mock
	private Environment env;

	@Autowired
	public ObjectMapper objectMapper;

	/** The registration dto list. */
	private List<RegistrationStatusDto> registrationDtoList;
	private List<RegistrationStatusDto> registrationDtoList1;

	RegistrationExternalStatusRequestDTO registrationExternalStatusRequestDTO;

	/** The array to json. */
	private String regStatusToJson;

	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {

		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		when(env.getProperty("mosip.registration.processor.registration.external.status.id"))
				.thenReturn("mosip.registration.external.status");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.registration.external.status.version")).thenReturn("1.0");
		List<RegistrationExternalStatusSubRequestDto> request = new ArrayList<>();
		RegistrationExternalStatusSubRequestDto regitrationid1 = new RegistrationExternalStatusSubRequestDto();
		RegistrationExternalStatusSubRequestDto regitrationid2 = new RegistrationExternalStatusSubRequestDto();
		RegistrationExternalStatusSubRequestDto regitrationid3 = new RegistrationExternalStatusSubRequestDto();
		regitrationid1.setRegistrationId("1001");
		regitrationid2.setRegistrationId("1002");
		regitrationid3.setRegistrationId("1003");
		request.add(regitrationid1);
		request.add(regitrationid2);
		request.add(regitrationid3);
		registrationExternalStatusRequestDTO = new RegistrationExternalStatusRequestDTO();
		registrationExternalStatusRequestDTO.setRequest(request);
		registrationExternalStatusRequestDTO.setId("mosip.registration.external.status");
		registrationExternalStatusRequestDTO.setVersion("1.0");
		registrationExternalStatusRequestDTO
				.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		regStatusToJson =objectMapper.writeValueAsString(registrationExternalStatusRequestDTO);
		registrationDtoList = new ArrayList<>();
		registrationDtoList1 = new ArrayList<>();
		RegistrationStatusDto registrationStatusDto1 = new RegistrationStatusDto();
		registrationStatusDto1.setRegistrationId("1001");
		registrationStatusDto1.setStatusCode("PROCESSED");

		RegistrationStatusDto registrationStatusDto2 = new RegistrationStatusDto();
		registrationStatusDto2.setRegistrationId("1002");
		registrationStatusDto2.setStatusCode("PROCESSED");

		registrationDtoList.add(registrationStatusDto1);
		registrationDtoList1.add(registrationStatusDto2);

		Mockito.doReturn(registrationDtoList).when(registrationStatusService).getExternalStatusByIds(ArgumentMatchers.any());
		Mockito.doReturn(registrationDtoList1).when(syncRegistrationService).getExternalStatusByIds(ArgumentMatchers.any());
		SearchInfo searchInfo = new SearchInfo();
		List<FilterInfo> filterinfos = new ArrayList<FilterInfo>();
		List<SortInfo> sortInfos = new ArrayList<SortInfo>();
		FilterInfo filterInfo = new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		filterinfos.add(filterInfo);
		SortInfo sortInfo = new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		sortInfos.add(sortInfo);
		searchInfo.setFilters(filterinfos);
		searchInfo.setSort(sortInfos);

		Mockito.doReturn(registrationDtoList).when(registrationStatusService).getByIds(ArgumentMatchers.any());
		Mockito.doReturn(registrationDtoList1).when(syncRegistrationService).getByIds(ArgumentMatchers.any());

	}

	/**
	 * Search success test.
	 *
	 */
	@Test
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void searchSuccessTest() throws Exception {
		doNothing().when(registrationExternalStatusRequestValidator).validate((registrationExternalStatusRequestDTO),
				"mosip.registration.external.status");
		Mockito.doReturn("test").when(digitalSignatureUtility).getDigitalSignature(ArgumentMatchers.any());
		
		MvcResult result = this.mockMvc.perform(post("/externalstatus/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk()).andReturn();
		String resultContent = result.getResponse().getContentAsString();
		JSONObject object = (JSONObject) new JSONParser().parse(resultContent);
		JSONArray responseObject = (JSONArray) object.get("response");
		JSONArray errorObject = (JSONArray) object.get("errors");
		JSONObject registrationStatusDto = (JSONObject) responseObject.get(0);
		JSONObject registrationStatusDto1 = (JSONObject) responseObject.get(1);
		JSONObject registrationStatusErrorDto = (JSONObject) errorObject.get(0);

		assertEquals(registrationStatusDto.get("registrationId").toString(), "1001");
		assertEquals(registrationStatusDto.get("statusCode").toString(), "PROCESSED");
		assertEquals(registrationStatusDto1.get("registrationId").toString(), "1002");
		assertEquals(registrationStatusDto1.get("statusCode").toString(), "PROCESSED");
		assertEquals(registrationStatusErrorDto.get("registrationId").toString(), "1003");
		assertEquals(registrationStatusErrorDto.get("errorMessage").toString(), "RID Not Found");
	}

	@Test(expected = NestedServletException.class)
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void searchRegstatusException() throws Exception {

		Mockito.doThrow(new RegStatusAppException()).when(registrationExternalStatusRequestValidator)
				.validate(ArgumentMatchers.any(), ArgumentMatchers.any());
		this.mockMvc.perform(post("/externalstatus/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isInternalServerError());
	}

}
