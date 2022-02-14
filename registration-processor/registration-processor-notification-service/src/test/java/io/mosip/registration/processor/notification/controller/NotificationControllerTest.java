/*
package io.mosip.registration.processor.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.notification.service.NotificationService;

@RunWith(SpringRunner.class)
@WebMvcTest(value = NotificationController.class)
public class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private WebApplicationContext webApplicationContext;
	
	@InjectMocks
	NotificationController notificationController = new NotificationController();
	
	@MockBean
	private NotificationService notificationService;
	
	Gson gson = new GsonBuilder().serializeNulls().create();
	WorkflowCompletedEventDTO notifyDto = new WorkflowCompletedEventDTO();
	WorkflowPausedForAdditionalInfoEventDTO pausedDto = new WorkflowPausedForAdditionalInfoEventDTO();

	private String notifyDtoJson;
	private String pausedDtoJson;
	
	@Before
	public void setUp() throws JsonProcessingException, ApisResourceAccessException {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		notifyDtoJson = gson.toJson(notifyDto);
		pausedDtoJson = gson.toJson(pausedDto);
	}
	
	@Test
	public void processNotify() throws Exception {
		
		ResponseEntity<Void> response = new ResponseEntity<Void>(HttpStatus.OK);

		Mockito.doReturn(response).when(notificationService).process(notifyDto);
		this.mockMvc.perform(post("/callback/notify").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", notifyDtoJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(notifyDtoJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}
	
	@Test
	public void processNotifyPausedForAdditionalInfo() throws Exception {
		
		ResponseEntity<Void> response = new ResponseEntity<Void>(HttpStatus.OK);

		Mockito.doReturn(response).when(notificationService).process(pausedDto);
		this.mockMvc.perform(post("/callback/notifyPausedForAdditionalInfo").accept(MediaType.APPLICATION_JSON_VALUE)
				.cookie(new Cookie("Authorization", pausedDtoJson)).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(pausedDtoJson.getBytes()).header("timestamp", "2019-05-07T05:13:55.704Z"))
				.andExpect(status().isOk());
	}
	
}
*/
