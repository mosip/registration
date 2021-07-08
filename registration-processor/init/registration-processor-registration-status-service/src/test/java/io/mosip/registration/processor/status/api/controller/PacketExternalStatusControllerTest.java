package io.mosip.registration.processor.status.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Ignore;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.status.api.config.RegistrationStatusConfigTest;
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusRequestDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusSubRequestDTO;
import io.mosip.registration.processor.status.service.PacketExternalStatusService;
import io.mosip.registration.processor.status.validator.PacketExternalStatusRequestValidator;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = RegistrationStatusConfigTest.class)
@TestPropertySource(locations = "classpath:application.properties")
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class PacketExternalStatusControllerTest {
	@InjectMocks
	PacketExternalStatusController packetExternalStatusController = new PacketExternalStatusController();

	@MockBean
	PacketExternalStatusService packetExternalStatusService;

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	PacketExternalStatusRequestValidator packetStatusRequestValidator;

	@Mock
	private Environment env;

	private String regStatusToJson;

	Gson gson = new GsonBuilder().serializeNulls().create();

	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {

		when(env.getProperty("mosip.registration.processor.packet.external.status.id"))
				.thenReturn("mosip.registration.packet.external.status");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.packet.external.status.version")).thenReturn("1.0");

		PacketExternalStatusSubRequestDTO packetExternalStatusSubRequestDTO = new PacketExternalStatusSubRequestDTO();
		packetExternalStatusSubRequestDTO.setPacketId("test1");
		PacketExternalStatusSubRequestDTO packetExternalStatusSubRequestDTO1 = new PacketExternalStatusSubRequestDTO();
		packetExternalStatusSubRequestDTO1.setPacketId("test2");
		List<PacketExternalStatusSubRequestDTO> requestList = new ArrayList<>();
		requestList.add(packetExternalStatusSubRequestDTO);
		requestList.add(packetExternalStatusSubRequestDTO1);
		PacketExternalStatusRequestDTO packetExternalStatusRequestDTO = new PacketExternalStatusRequestDTO();
		packetExternalStatusRequestDTO.setId("mosip.registration.packet.external.status");
		packetExternalStatusRequestDTO.setVersion("1.0");
		packetExternalStatusRequestDTO
				.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		regStatusToJson = gson.toJson(packetExternalStatusRequestDTO);
		PacketExternalStatusDTO packetExternalStatusDTO = new PacketExternalStatusDTO();
		packetExternalStatusDTO.setPacketId("test1");
		packetExternalStatusDTO.setStatusCode("PROCESSED");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList = new ArrayList<PacketExternalStatusDTO>();
		packetExternalStatusDTOList.add(packetExternalStatusDTO);
		Mockito.doReturn(packetExternalStatusDTOList).when(packetExternalStatusService)
				.getByPacketIds(ArgumentMatchers.any());
	}
	@Test
	@Ignore
	public void packetExternalStatusSuccessTest() throws Exception {
		// doNothing().when(registrationStatusRequestValidator).validate((registrationStatusRequestDTO),
		// "mosip.registration.status");
		MvcResult result = this.mockMvc.perform(post("/packetexternalstatus").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", regStatusToJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(regStatusToJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk()).andReturn();
	}
}
