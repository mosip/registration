package io.mosip.registration.processor.notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;
import io.mosip.kernel.websub.api.verifier.AuthenticatedContentVerifier;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.TemplateDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.TemplateResponseDto;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.notification.service.NotificationService;
import io.mosip.registration.processor.notification.service.impl.NotificationServiceImpl;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class NotificationServiceTest {

	@Mock
	private MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> service;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
    private AuthenticatedContentVerifier authenticatedContentVerifier;
	
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;


	/** The identity. */
	Identity identity = new Identity();

	@InjectMocks
	private NotificationService notificationService = new NotificationServiceImpl();
	
	@Value("${websub.hub.url}")
	private String hubURL;
	
	
	@Value("${mosip.regproc.workflow.complete.topic}")
	private String topic;
	
	@Mock
	private  SubscriptionClient<SubscriptionChangeRequest,UnsubscriptionRequest, SubscriptionChangeResponse> subs; 
	
	@Mock
	private Environment env;
	

	@Before
	public void setup() throws Exception {
		SubscriptionChangeResponse subscriptionChangeResponse = new SubscriptionChangeResponse();
		subscriptionChangeResponse.setHubURL(hubURL);
		subscriptionChangeResponse.setTopic(topic);
		ReflectionTestUtils.setField(notificationService, "notificationTypes", "SMS|EMAIL");
		ReflectionTestUtils.setField(notificationService, "notificationEmails", "abc@gmail.com");
		when(subs.subscribe(Mockito.any())).thenReturn(subscriptionChangeResponse);
		when(authenticatedContentVerifier.verifyAuthorizedContentVerified(any(), any())).thenReturn(true);
	}

	@Test
	public void testMessageSentUINGenerated() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_UIN_GEN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_UIN_GEN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_UIN_GEN_SMS").thenReturn("RPR_UIN_GEN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("NEW");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessageSentUINUpdate() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_UIN_UPD_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_UIN_UPD_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_UIN_UPD_SMS").thenReturn("RPR_UIN_UPD_EMAIL")
		.thenReturn("RPR_UIN_UPD_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("UPDATE");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentUINActivate() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_UIN_REAC_SMS").thenReturn("RPR_UIN_REAC_EMAIL")
		.thenReturn("RPR_UIN_REAC_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("ACTIVATED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentUINDeactivate() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_UIN_DEAC_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_UIN_DEAC_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_UIN_DEAC_SMS").thenReturn("RPR_UIN_DEAC_EMAIL")
		.thenReturn("RPR_UIN_DEAC_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("DEACTIVATED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentUINDuplicate() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_DUP_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_DUP_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_DUP_UIN_SMS").thenReturn("RPR_DUP_UIN_EMAIL")
		.thenReturn("RPR_DUP_UIN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("REJECTED");
		completedEventDTO.setWorkflowType("DEACTIVATED");
		completedEventDTO.setErrorCode("MANUAL_VERIFICATION_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentTechnicalIssue() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_TEC_ISSUE_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_TEC_ISSUE_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_TEC_ISSUE_SMS").thenReturn("RPR_TEC_ISSUE_EMAIL")
		.thenReturn("RPR_TEC_ISSUE_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("REJECTED");
		completedEventDTO.setWorkflowType("DEACTIVATED");
		completedEventDTO.setErrorCode("CMD_VALIDATION_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentLostUIN() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessageSentLostANDDuplicateUIN() throws Exception {
		
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("REJECTED");
		completedEventDTO.setWorkflowType("LOST");
		completedEventDTO.setErrorCode("MANUAL_VERIFICATION_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	@DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
	public void testMessageConfigurationException() throws Exception {
		ReflectionTestUtils.setField(notificationService, "notificationTypes", "");
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageTemplateException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSMSFAILED() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("failed");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageEMAILFAILED() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("failed");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSentEMAILIDNotFound() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new EmailIdNotFoundException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	
	@Test
	public void testMessageEmailTemplateGenerationFailedException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new TemplateGenerationFailedException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageEmailApisResourceAccessException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApisResourceAccessException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessagePhoneNumberNotFoundException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new PhoneNumberNotFoundException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	
	@Test
	public void testMessageSmsTemplateGenerationFailedException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new TemplateGenerationFailedException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@Test
	public void testMessageSmsApisResourceAccessException() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new ApisResourceAccessException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
    
	@Test
	public void testMessageSmsAndEmailFAILED() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper=new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("failed");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("failed");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_LOST_UIN_SMS").thenReturn("RPR_LOST_UIN_EMAIL")
		.thenReturn("RPR_UIN_GEN_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultCode("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessageSentPausedForAdditionalRequest() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS")
				.thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL").thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(responseDto);
		when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""),
				Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		workflowPausedForAdditionalInfoEventDTO.setInstanceId("85425022110000120190117110505");
		workflowPausedForAdditionalInfoEventDTO.setWorkflowType("NEW");
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoProcess("correction");
		ResponseEntity<Void> res = notificationService.process(workflowPausedForAdditionalInfoEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessageTemplateExceptionForPausedForAdditionalRequest() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		workflowPausedForAdditionalInfoEventDTO.setInstanceId("85425022110000120190117110505");
		workflowPausedForAdditionalInfoEventDTO.setWorkflowType("NEW");
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoProcess("correction");
		ResponseEntity<Void> res = notificationService.process(workflowPausedForAdditionalInfoEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	public void testMessageEmailTemplateGenerationFailedExceptionForPausedForAdditionalRequest() throws Exception {
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS")
		.thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL").thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(new TemplateGenerationFailedException());
		when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""),
				Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		workflowPausedForAdditionalInfoEventDTO.setInstanceId("85425022110000120190117110505");
		workflowPausedForAdditionalInfoEventDTO.setWorkflowType("NEW");
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoProcess("correction");
		ResponseEntity<Void> res = notificationService.process(workflowPausedForAdditionalInfoEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@Test
	@DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
	public void testMessageConfigurationExceptionForPausedForAdditionalRequest() throws Exception {
		ReflectionTestUtils.setField(notificationService, "notificationTypes", "");
		List<TemplateDto> templates = new ArrayList<TemplateDto>();
		TemplateDto templateEmail = new TemplateDto();
		TemplateDto templateSMS = new TemplateDto();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		templateSMS.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS");
		templates.add(templateSMS);
		templateEmail.setTemplateTypeCode("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL");
		templates.add(templateEmail);
		templateResponseDto.setTemplates(templates);
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		SmsResponseDto smsResponse = new SmsResponseDto();
		smsResponse.setStatus("success");
		ResponseDto responseDto = new ResponseDto();
		responseDto.setStatus("success");
		Mockito.when(env.getProperty(any())).thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_SMS")
		.thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL").thenReturn("RPR_PAUSED_FOR_ADDITIONAL_INFO_EMAIL_SUB");
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(responseDto);
		when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""),
				Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		workflowPausedForAdditionalInfoEventDTO.setInstanceId("85425022110000120190117110505");
		workflowPausedForAdditionalInfoEventDTO.setWorkflowType("NEW");
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoProcess("correction");
		ResponseEntity<Void> res = notificationService.process(workflowPausedForAdditionalInfoEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	}
