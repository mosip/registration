package io.mosip.registration.processor.notification.service.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.config.IntentVerificationConfig;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;
import io.mosip.kernel.websub.api.verifier.AuthenticatedContentVerifier;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.TemplateDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.TemplateResponseDto;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.utility.NotificationTemplateCode;
import io.mosip.registration.processor.notification.service.dto.MessageSenderDto;
import io.mosip.registration.processor.notification.service.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.notification.service.impl.NotificationService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;

@SpringBootTest(classes = { NotificationServiceTestApplication.class })
@RunWith(SpringRunner.class)
public class NotificationServiceTest {

	@MockBean
	private MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> service;

	@MockBean
	private ObjectMapper mapper;

	@MockBean
	private RegistrationProcessorRestClientService<Object> restClientService;

    @MockBean
    private AuthenticatedContentVerifier authenticatedContentVerifier;
	
	@MockBean
	private AuditLogRequestBuilder auditLogRequestBuilder;


	/** The packet meta info. */
	private PacketMetaInfo packetMetaInfo = new PacketMetaInfo();

	/** The identity. */
	Identity identity = new Identity();

	@Autowired
	private NotificationService notificationService;
	
	@Value("${registration.processor.notification_service_subscriber_hub_url}")
	private String hubURL;
	
	
	@Value("${registration.processor.notification_service_subscriber_topic}")
	private String topic;
	
	@MockBean
	private  SubscriptionClient<SubscriptionChangeRequest,UnsubscriptionRequest, SubscriptionChangeResponse> subs; 
	

	@Before
	public void setup() throws Exception {
		SubscriptionChangeResponse subscriptionChangeResponse = new SubscriptionChangeResponse();
		subscriptionChangeResponse.setHubURL(hubURL);
		subscriptionChangeResponse.setTopic(topic);
		when(subs.subscribe(Mockito.any())).thenReturn(subscriptionChangeResponse);
		when(authenticatedContentVerifier.verifyAuthorizedContentVerified(any(), any())).thenReturn(true);

	}


	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("NEW");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}

	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("UPDATE");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("ACTIVATED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("DEACTIVATED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("REJECTED");
		completedEventDTO.setWorkflowType("DEACTIVATED");
		completedEventDTO.setErrorCode("MANUAL_VERIFICATION_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("REJECTED");
		completedEventDTO.setWorkflowType("DEACTIVATED");
		completedEventDTO.setErrorCode("OSI_VALIDATE_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(200, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentLostANDDuplicateUIN() throws Exception {
		
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("REJECTED");
		completedEventDTO.setWorkflowType("LOST");
		completedEventDTO.setErrorCode("MANUAL_VERIFICATION_FAILED");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
	}
	
	
	@SuppressWarnings("unchecked")
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
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
	}
	
	@SuppressWarnings("unchecked")
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new EmailIdNotFoundException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new TemplateGenerationFailedException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApisResourceAccessException());
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new PhoneNumberNotFoundException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new TemplateGenerationFailedException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenThrow(new ApisResourceAccessException());
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
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
		when(service.sendSmsNotification(any(), any(), any(), any(), any(), any())).thenReturn(smsResponse);
		when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseDto);
        when(restClientService.getApi(Mockito.eq(ApiName.TEMPLATES), any(), Mockito.eq(""), Mockito.eq(""), Mockito.eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		WorkflowCompletedEventDTO completedEventDTO= new WorkflowCompletedEventDTO();
		completedEventDTO.setInstanceId("85425022110000120190117110505");
		completedEventDTO.setResultType("PROCESSED");
		completedEventDTO.setWorkflowType("LOST");

		ResponseEntity<Void> res=notificationService.process(completedEventDTO);
		assertEquals(500, res.getStatusCodeValue());
	}

}
