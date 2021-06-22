package io.mosip.registration.processor.status.api.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.api.config.RegistrationStatusConfigTest;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;
import io.mosip.registration.processor.status.validator.LostRidRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationStatusRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationSyncRequestValidator;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationStatusConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class RegistrationExternalStatusControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@InjectMocks
	RegistrationExternalStatusController registrationExternalStatusController = new RegistrationExternalStatusController();
	
	@MockBean
	RegistrationStatusRequestValidator registrationStatusRequestValidator;
	
	/** The registration status service. */
	@MockBean
	RegistrationStatusServiceImpl registrationStatusService;
	
	@MockBean
	SyncRegistrationServiceImpl syncRegistrationService;
	
	@MockBean
	LostRidRequestValidator lostRidRequestValidator;
	
	@MockBean
	private RegistrationSyncRequestValidator syncrequestvalidator;
	
	@Mock
	private Environment env;
	
	/** The registration dto list. */
	private List<RegistrationStatusDto> registrationDtoList;
	private List<RegistrationStatusDto> registrationDtoList1;
	
	RegistrationStatusRequestDTO registrationStatusRequestDTO;
	
	/** The array to json. */
	private String regStatusToJson;
	Gson gson = new GsonBuilder().serializeNulls().create();
	
	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {

		// mockMvc =
		// MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
		when(env.getProperty("mosip.registration.processor.registration.status.id"))
				.thenReturn("mosip.registration.status");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.application.version")).thenReturn("1.0");
		List<RegistrationStatusSubRequestDto> request = new ArrayList<>();
		RegistrationStatusSubRequestDto regitrationid1 = new RegistrationStatusSubRequestDto();
		RegistrationStatusSubRequestDto regitrationid2 = new RegistrationStatusSubRequestDto();
		RegistrationStatusSubRequestDto regitrationid3 = new RegistrationStatusSubRequestDto();
		regitrationid1.setRegistrationId("1001");
		regitrationid2.setRegistrationId("1002");
		regitrationid3.setRegistrationId("1003");
		request.add(regitrationid1);
		request.add(regitrationid2);
		request.add(regitrationid3);
		registrationStatusRequestDTO = new RegistrationStatusRequestDTO();
		registrationStatusRequestDTO.setRequest(request);
		registrationStatusRequestDTO.setId("mosip.registration.status");
		registrationStatusRequestDTO.setVersion("1.0");
		registrationStatusRequestDTO
				.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		regStatusToJson = gson.toJson(registrationStatusRequestDTO);
		registrationDtoList = new ArrayList<>();
		registrationDtoList1 = new ArrayList<>();
		RegistrationStatusDto registrationStatusDto1 = new RegistrationStatusDto();
		registrationStatusDto1.setRegistrationId("1001");

		RegistrationStatusDto registrationStatusDto2 = new RegistrationStatusDto();
		registrationStatusDto2.setRegistrationId("1002");

		registrationDtoList.add(registrationStatusDto1);
		registrationDtoList1.add(registrationStatusDto2);

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
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void searchSuccessTest() throws Exception {
		doNothing().when(registrationStatusRequestValidator).validate((registrationStatusRequestDTO),
				"mosip.registration.status");


		this.mockMvc.perform(post("/externalstatus/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}

	@Test
	public void searchRegstatusException() throws Exception {

		Mockito.doThrow(new RegStatusAppException()).when(registrationStatusRequestValidator)
				.validate(ArgumentMatchers.any(), ArgumentMatchers.any());
		this.mockMvc.perform(post("/externalstatus/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}

}
