package io.mosip.registration.processor.message.sender.stage.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
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
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.message.sender.dto.MessageSenderDto;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.stage.MessageSenderStage;
import io.mosip.registration.processor.message.sender.utility.NotificationTemplateCode;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.service.TransactionService;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, JsonUtil.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class MessageSenderStageTest {

	@Mock
	private MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> service;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private TransactionService<TransactionDto> transcationStatusService;

	@Mock
	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationservice;


	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;


	/** The packet meta info. */
	private PacketMetaInfo packetMetaInfo = new PacketMetaInfo();

	/** The identity. */
	Identity identity = new Identity();

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	@Mock
	private RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Mock
	private LogDescription description;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	MessageSenderDto messageSenderDto = Mockito.mock(MessageSenderDto.class);

	private SmsResponseDto smsResponseDto = Mockito.mock(SmsResponseDto.class);

	private ResponseDto responseDto = Mockito.mock(ResponseDto.class);

	private SyncRegistrationEntity regentity = Mockito.mock(SyncRegistrationEntity.class);

	@InjectMocks
	private MessageSenderStage stage = new MessageSenderStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus(vertx) {
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(stage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(stage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(stage, "notificationTypes", "SMS|EMAIL");
		ReflectionTestUtils.setField(stage, "uinGeneratedSubject", "UIN generated");
		ReflectionTestUtils.setField(stage, "uinActivateSubject", "UIN activated");
		ReflectionTestUtils.setField(stage, "uinDeactivateSubject", "UIN deactivated");
		ReflectionTestUtils.setField(stage, "duplicateUinSubject", "duplicate uin");
		ReflectionTestUtils.setField(stage, "reregisterSubject", "re register");
		ReflectionTestUtils.setField(stage, "notificationEmails", "abc@gmail.com");

		Mockito.doNothing().when(registrationStatusDto).setStatusCode(any());
		Mockito.doNothing().when(registrationStatusDto).setStatusComment(any());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("NEW");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		Mockito.when(transcationStatusService.addRegistrationTransaction(any())).thenReturn(null);
		Mockito.doNothing().when(registrationStatusDto).setLatestTransactionTypeCode(any());
		Mockito.doNothing().when(registrationStatusDto).setRegistrationStageName(any());
		Mockito.doNothing().when(registrationStatusDto).setLatestTransactionStatusCode(any());
		Mockito.doNothing().when(description).setMessage(any());

		Mockito.doNothing().when(messageSenderDto).setEmailTemplateCode(any());
		Mockito.doNothing().when(messageSenderDto).setSmsTemplateCode(any());
		Mockito.doNothing().when(messageSenderDto).setIdType(any());
		Mockito.doNothing().when(messageSenderDto).setSubject(any());
		Mockito.doNothing().when(messageSenderDto).setTemplateAvailable(any(Boolean.class));

		Mockito.when(service.sendSmsNotification(any(), any(), any(), any(), any())).thenReturn(smsResponseDto);
		Mockito.when(service.sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(responseDto);

		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("ERROR");
		Mockito.when(registrationExceptionMapperUtil.getStatusCode(any())).thenReturn("ERROR");


		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("New");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);

		regentity.setRegistrationType("NEW");
		Mockito.when(syncRegistrationservice.findByRegistrationId(any())).thenReturn(regentity);
	}

	@Test
	public void testDeployVerticle() {
		stage.deployVerticle();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentUINGenerated() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_GEN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_GEN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_GEN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_GEN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.doReturn("success").when(smsResponseDto).getStatus();
		Mockito.doReturn("success").when(responseDto).getStatus();

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentUINUpdate() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_UPD_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_UPD_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_UPD_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_UPD_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);

		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);

		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());

		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentUINUpdatewithActivatedUIN() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(regentity.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("ACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentUINUpdatewithDeactivatedUIN() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_DEAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_DEAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_DEAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_DEAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);

		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		// Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PACKET_UIN_UPDATION_SUCCESS.name());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("DEACTIVATED");
		Mockito.when(regentity.getRegistrationType()).thenReturn("DEACTIVATED");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("DEACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentDuplicateUIN() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_DUP_UIN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_DUP_UIN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_DUP_UIN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_DUP_UIN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);

		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.MANUAL_VERIFICATION.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.FAILED.name());
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.REJECTED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentTechnicalIssue() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();
		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_TEC_ISSUE_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_TEC_ISSUE_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_TEC_ISSUE_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_TEC_ISSUE_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);

		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.OSI_VALIDATE.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.FAILED.name());
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.FAILED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@Test
	public void testConfigNotFoundException() throws Exception {
		ReflectionTestUtils.setField(stage, "notificationTypes", "");
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
	}

	@Test
	public void testTemplateNotFound() throws ApisResourceAccessException {
		ReflectionTestUtils.setField(stage, "notificationTypes", "OTP");

		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
	}

	@Test
	public void testException() throws ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.BIOGRAPHIC_VERIFICATION.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.FAILED.name());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
	}

	@Test
	public void testFsadapterException() throws ApisResourceAccessException {
		FSAdapterException e = new FSAdapterException(null, null);
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();

		templateDto.setTemplateTypeCode("RPR_TEC_ISSUE_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_TEC_ISSUE_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenThrow(e);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(regentity.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(registrationStatusDto.getStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.OSI_VALIDATE.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.FAILED.name());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getInternalError());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageSentFailedLostUin() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("LOST");
		Mockito.when(regentity.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.doReturn("failed").when(smsResponseDto).getStatus();
		Mockito.doReturn("failed").when(responseDto).getStatus();

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMessageUpdateApiException() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("LOST");
		Mockito.when(regentity.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		ApisResourceAccessException e = new ApisResourceAccessException();
		Mockito.doThrow(e).when(service).sendSmsNotification(any(), any(), any(), any(), any());
		Mockito.doThrow(e).when(service).sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());
	}

	@Test
	public void testNotificationTyeNoneSuccess() throws Exception {
		ReflectionTestUtils.setField(stage, "notificationTypes", "NONE");

		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(regentity.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("ACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}

	@Test
	public void testNotificationSentFailureForAllNotificationType() throws Exception {
		ReflectionTestUtils.setField(stage, "notificationTypes", "SMS|EMAIL");

		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(regentity.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("failed");
		Mockito.when(responseDto.getStatus()).thenReturn("failed");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("ACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());

	}

	@Test
	public void testNotificationSentFailureForOneNotificationType() throws Exception {
		ReflectionTestUtils.setField(stage, "notificationTypes", "SMS|EMAIL");

		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(regentity.getRegistrationType()).thenReturn("UPDATE");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("failed");
		Mockito.when(responseDto.getStatus()).thenReturn("success");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("ACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());

	}


	@Test
	public void testNotificationSentSuccessForOneNotificationType() throws Exception {
		ReflectionTestUtils.setField(stage, "notificationTypes", "SMS");

		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_UIN_REAC_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_UIN_REAC_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_UIN_REAC_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(regentity.getRegistrationType()).thenReturn("ACTIVATED");
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		Mockito.when(smsResponseDto.getStatus()).thenReturn("success");

		FieldValue registrationType = new FieldValue();
		registrationType.setLabel("registrationType");
		registrationType.setValue("ACTIVATED");
		List<FieldValue> fieldValueList = new ArrayList<>();
		fieldValueList.add(registrationType);
		identity.setMetaData(fieldValueList);
		packetMetaInfo.setIdentity(identity);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "inputStreamtoJavaObject", inputStream, PacketMetaInfo.class)
				.thenReturn(packetMetaInfo);

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertTrue(result.getIsValid());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testMessageUpdateEmailIdNotFoundException() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("LOST");
		Mockito.when(regentity.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		EmailIdNotFoundException e = new EmailIdNotFoundException();
		Mockito.doReturn("success").when(smsResponseDto).getStatus();
		Mockito.doReturn("success").when(responseDto).getStatus();
		Mockito.doThrow(e).when(service).sendEmailNotification(any(), any(), any(), any(), any(), any(), any(), any());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testMessageUpdatePhoneNumberNotFoundException() throws Exception {
		ResponseWrapper<TemplateResponseDto> responseWrapper = new ResponseWrapper<>();
		TemplateResponseDto templateResponseDto = new TemplateResponseDto();

		TemplateDto templateDto = new TemplateDto();
		TemplateDto templateDto1 = new TemplateDto();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_EMAIL).when(messageSenderDto).getEmailTemplateCode();
		Mockito.doReturn(NotificationTemplateCode.RPR_LOST_UIN_SMS).when(messageSenderDto).getSmsTemplateCode();
		Mockito.when(messageSenderDto.getIdType()).thenReturn(IdType.UIN);
		Mockito.when(messageSenderDto.getSubject()).thenReturn("");
		Mockito.when(messageSenderDto.isTemplateAvailable()).thenReturn(Boolean.TRUE);
		templateDto.setTemplateTypeCode("RPR_LOST_UIN_SMS");
		List<TemplateDto> list = new ArrayList<TemplateDto>();
		list.add(templateDto);
		templateDto1.setTemplateTypeCode("RPR_LOST_UIN_EMAIL");
		list.add(templateDto1);
		templateResponseDto.setTemplates(list);
		String s = templateResponseDto.toString();
		responseWrapper.setResponse(templateResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn(s);
		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(templateResponseDto);
		Mockito.when(registrationStatusService.getRegistrationStatus(any())).thenReturn(registrationStatusDto);
		Mockito.when(registrationStatusDto.getStatusCode()).thenReturn(RegistrationStatusCode.PROCESSED.name());
		Mockito.when(registrationStatusDto.getLatestTransactionTypeCode())
				.thenReturn(RegistrationTransactionTypeCode.UIN_GENERATOR.name());
		Mockito.when(registrationStatusDto.getRegistrationType()).thenReturn("LOST");
		Mockito.when(registrationStatusDto.getLatestTransactionStatusCode())
				.thenReturn(RegistrationTransactionStatusCode.PROCESSED.name());
		PhoneNumberNotFoundException e = new PhoneNumberNotFoundException();
		Mockito.doThrow(e).when(service).sendSmsNotification(any(), any(), any(), any(), any());

		MessageDTO dto = new MessageDTO();
		dto.setRid("85425022110000120190117110505");
		MessageDTO result = stage.process(dto);
		assertFalse(result.getIsValid());
	}
}
