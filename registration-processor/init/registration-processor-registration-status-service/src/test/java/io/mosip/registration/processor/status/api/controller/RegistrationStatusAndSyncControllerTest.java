package io.mosip.registration.processor.status.api.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import org.mockito.Spy;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.digital.signature.dto.SignResponseDto;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.api.config.RegistrationStatusConfigTest;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.LostRidRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationSyncRequestDTO;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailureDto;
import io.mosip.registration.processor.status.dto.SyncResponseSuccessDto;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;
import io.mosip.registration.processor.status.validator.LostRidRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationStatusRequestValidator;
import io.mosip.registration.processor.status.validator.RegistrationSyncRequestValidator;

/**
 * The Class RegistrationStatusControllerTest.
 *
 * @author M1047487
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationStatusConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class RegistrationStatusAndSyncControllerTest {

	@MockBean
	@Qualifier("selfTokenRestTemplate")
	private RestTemplate restTemplate;

	/** The registration status controller. */
	@InjectMocks
	RegistrationStatusController registrationStatusController = new RegistrationStatusController();

	@InjectMocks
	RegistrationSyncController registrationSyncController = new RegistrationSyncController();

	/** The registration status service. */
	@MockBean
	RegistrationStatusServiceImpl registrationStatusService;

	/** The sync registration service. */
	@MockBean
	SyncRegistrationServiceImpl syncRegistrationService;

	/** The sync registration dto. */
	@MockBean
	SyncRegistrationDto syncRegistrationDto;

	RegistrationStatusRequestDTO registrationStatusRequestDTO;
	/** The mock mvc. */
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private WebApplicationContext webApplicationContext;

	/** The registration dto list. */
	private List<RegistrationStatusDto> registrationDtoList;

	/** The registration dto list. */
	private List<RegistrationStatusDto> registrationDtoList1;

	/** The array to json. */
	private String regStatusToJson;

	private String lostRidReqToJson;

	@MockBean
	private RegistrationProcessorRestClientService<Object> reprcrestclient;
	
	@MockBean
	RegistrationUtility registrationUtility;

	private ResponseWrapper dto = new ResponseWrapper();

	private SignResponseDto signresponse = new SignResponseDto();
	RegistrationSyncRequestDTO registrationSyncRequestDTO;

	@Mock
	private Environment env;

	LostRidRequestDto lostRidRequestDto;


	@MockBean
	RegistrationStatusRequestValidator registrationStatusRequestValidator;

	@MockBean
	LostRidRequestValidator lostRidRequestValidator;

	@MockBean
	private RegistrationSyncRequestValidator syncrequestvalidator;
	
	@Autowired
	ObjectMapper objMp=new ObjectMapper();
	private List<SyncResponseDto> syncResponseDtoList;
	private List<SyncRegistrationDto> list;
	SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();

	// @Autowired
//	private WebApplicationContext context;

	// @Autowired
	//// private Filter springSecurityFilterChain;

	/**
	 * Sets the up.
	 *
	 * @throws JsonProcessingException
	 * @throws ApisResourceAccessException
	 */
	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {
		ObjectMapper objMp=new ObjectMapper();
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		when(env.getProperty("mosip.registration.processor.registration.status.id"))
				.thenReturn("mosip.registration.status");
		when(env.getProperty("mosip.registration.processor.lostrid.id")).thenReturn("mosip.registration.lostrid");
		when(env.getProperty("mosip.registration.processor.lostrid.version")).thenReturn("1.0");
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
		regStatusToJson = objMp.writeValueAsString(registrationStatusRequestDTO);
		registrationDtoList = new ArrayList<>();
		registrationDtoList1 = new ArrayList<>();
		RegistrationStatusDto registrationStatusDto1 = new RegistrationStatusDto();
		registrationStatusDto1.setRegistrationId("1001");

		RegistrationStatusDto registrationStatusDto2 = new RegistrationStatusDto();
		registrationStatusDto2.setRegistrationId("1002");

		registrationDtoList.add(registrationStatusDto1);
		registrationDtoList1.add(registrationStatusDto2);
		SyncResponseSuccessDto syncResponseDto = new SyncResponseSuccessDto();
		SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
		syncResponseDto.setRegistrationId("1001");

		syncResponseDto.setStatus("SUCCESS");
		syncResponseFailureDto.setRegistrationId("1001");

		syncResponseFailureDto.setMessage("Registartion Id's are successfully synched in Sync table");
		syncResponseFailureDto.setStatus("FAILURE");
		syncResponseFailureDto.setErrorCode("Test");
		syncResponseDtoList = new ArrayList<>();
		syncResponseDtoList.add(syncResponseDto);
		syncResponseDtoList.add(syncResponseFailureDto);
		list = new ArrayList<>();
		SyncRegistrationDto syncRegistrationDto = new SyncRegistrationDto();
		syncRegistrationDto = new SyncRegistrationDto();
		syncRegistrationDto.setRegistrationId("1002");
		syncRegistrationDto.setLangCode("eng");
		syncRegistrationDto.setIsActive(true);
		list.add(syncRegistrationDto);
		registrationSyncRequestDTO = new RegistrationSyncRequestDTO();
		registrationSyncRequestDTO.setRequest(list);
		registrationSyncRequestDTO.setId("mosip.registration.sync");
		registrationSyncRequestDTO.setVersion("1.0");
		registrationSyncRequestDTO
				.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		lostRidRequestDto = new LostRidRequestDto();
		lostRidRequestDto.setId("mosip.registration.lostrid");
		lostRidRequestDto.setVersion("1.0");
		lostRidRequestDto.setRequesttime(LocalDateTime.now().toString());
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
		lostRidReqToJson =objMp.writeValueAsString(lostRidRequestDto);

		Mockito.doReturn(registrationDtoList).when(registrationStatusService).getByIds(ArgumentMatchers.any());
		Mockito.doReturn(registrationDtoList1).when(syncRegistrationService).getByIds(ArgumentMatchers.any());

		signresponse.setSignature("abcd");
		dto.setResponse(signresponse);
		Mockito.when(reprcrestclient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dto);
		Mockito.when(syncRegistrationService.sync(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(syncResponseDtoList);
		Mockito.when(
				syncrequestvalidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(Boolean.TRUE);

	}

	/**
	 * Search success test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void searchSuccessTest() throws Exception {
		doNothing().when(registrationStatusRequestValidator).validate((registrationStatusRequestDTO),
				"mosip.registration.status");

		this.mockMvc.perform(post("/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void searchRegstatusException() throws Exception {

		Mockito.doThrow(new RegStatusAppException()).when(registrationStatusRequestValidator)
				.validate(ArgumentMatchers.any(), ArgumentMatchers.any());
		this.mockMvc.perform(post("/search").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void testSyncController() throws Exception {

		RegistrationSyncRequestDTO registrationSyncRequestDTO = new RegistrationSyncRequestDTO();
		List<SyncRegistrationDto> request = new ArrayList<SyncRegistrationDto>();
		SyncRegistrationDto syncRegistrationDto = new SyncRegistrationDto("45128164920495", "NEW", null, null, "eng");
		request.add(syncRegistrationDto);
		String requestJson = objMp.writeValueAsString(request);

		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		SyncResponseDto syncResponseDto = new SyncResponseDto();
		syncResponseDto.setStatus("true");
		syncResponseDto.setRegistrationId("45128164920495");
		syncResponseList.add(syncResponseDto);

		Mockito.when(syncRegistrationService.decryptAndGetSyncRequest(ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(registrationSyncRequestDTO);
		Mockito.when(
				syncrequestvalidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(true);
		Mockito.when(
				syncRegistrationService.sync(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(syncResponseList);
		this.mockMvc.perform(
				post("/sync").accept(MediaType.APPLICATION_JSON_VALUE).header("timestamp", "2019-05-07T05:13:55.704Z")
						.header("Center-Machine-RefId", "abcd").contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(requestJson.getBytes()).cookie(new Cookie("Authorization", requestJson)))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "resident", roles = "RESIDENT")
	public void testSyncV2Controller() throws Exception {

		RegistrationSyncRequestDTO registrationSyncRequestDTO = new RegistrationSyncRequestDTO();
		List<SyncRegistrationDto> request = new ArrayList<SyncRegistrationDto>();
		SyncRegistrationDto syncRegistrationDto = new SyncRegistrationDto("45128164920495", "NEW", null, null, "eng");
		request.add(syncRegistrationDto);
		String requestJson = objMp.writeValueAsString(request);

		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		SyncResponseDto syncResponseDto = new SyncResponseDto();
		syncResponseDto.setStatus("true");
		syncResponseDto.setRegistrationId("45128164920495");
		syncResponseList.add(syncResponseDto);

		Mockito.when(syncRegistrationService.decryptAndGetSyncRequest(ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(registrationSyncRequestDTO);
		Mockito.when(
				syncrequestvalidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(true);
		Mockito.when(
				syncRegistrationService.syncV2(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(syncResponseList);
		this.mockMvc.perform(
				post("/syncV2").accept(MediaType.APPLICATION_JSON_VALUE).header("timestamp", "2019-05-07T05:13:55.704Z")
						.header("Center-Machine-RefId", "abcd").contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(requestJson.getBytes()).cookie(new Cookie("Authorization", requestJson)))
				.andExpect(status().isOk());
	}

	@Test
	public void testBuildRegistrationSyncResponse() throws JsonProcessingException {
		List<SyncResponseDto> syncResponseDtoList = new ArrayList<>();
		syncResponseFailureDto.setStatus("SUCCESS");
		syncResponseDtoList.add(syncResponseFailureDto);
		registrationSyncController.buildRegistrationSyncResponse(syncResponseDtoList);

	}

	@Test
	public void testBuildRegistrationSyncResponse1() throws JsonProcessingException {
		List<SyncResponseDto> syncResponseDtoList = new ArrayList<>();
		SyncResponseFailDto syncResponseFailDto = new SyncResponseFailDto();
		SyncResponseFailureDto syncResponseDto = new SyncResponseFailureDto();

		syncResponseDto.setStatus("Fail");
		syncResponseDtoList.add(syncResponseDto);

		syncResponseFailDto.setStatus("Fail");
		syncResponseDtoList.add(syncResponseFailDto);
		registrationSyncController.buildRegistrationSyncResponse(syncResponseDtoList);

	}

	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void lostRidSuccessTest() throws Exception {
		doNothing().when(lostRidRequestValidator).validate((lostRidRequestDto));

		this.mockMvc.perform(post("/lostridsearch").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", lostRidReqToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(lostRidReqToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void lostRidRegstatusException() throws Exception {

		Mockito.doThrow(new RegStatusAppException()).when(lostRidRequestValidator).validate(ArgumentMatchers.any());
		this.mockMvc.perform(post("/lostridsearch").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", lostRidReqToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(lostRidReqToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}
	
	@Test
	@WithMockUser(value = "admin", roles = "REGISTRATION_ADMIN")
	public void lostRidWorkFlowSearchException() throws Exception {

		Mockito.doThrow(new WorkFlowSearchException("ERR-001", "exception occured")).when(lostRidRequestValidator)
				.validate(ArgumentMatchers.any());
		this.mockMvc.perform(post("/lostridsearch").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", lostRidReqToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(lostRidReqToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}
}