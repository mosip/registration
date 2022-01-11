package io.mosip.registration.processor.reprocessor.verticle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(MockitoJUnitRunner.class)
public class ReprocessorVerticleTest {

	MessageDTO dto = new MessageDTO();
	@InjectMocks
	private ReprocessorVerticle reprocessorVerticle = new ReprocessorVerticle() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus() {

				@Override
				public Vertx getEventbus() {
					return vertx;
				}

				@Override
				public void consume(MessageBusAddress fromAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void send(MessageBusAddress toAddress, MessageDTO message) {

				}
			};
		}

		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}
	};

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private LogDescription description;
	

	@Before
	public void setup() throws Exception {
		 //Mockito.doNothing().when(description).setCode(Mockito.anyString());
		 //Mockito.doNothing().when(description).setMessage(Mockito.anyString());
		 //Mockito.when(description.getCode()).thenReturn("CODE");
		 //Mockito.when(description.getMessage()).thenReturn("MESSAGE");
		 ReflectionTestUtils.setField(reprocessorVerticle, "fetchSize", 2);
         ReflectionTestUtils.setField(reprocessorVerticle, "elapseTime", 21600);
         ReflectionTestUtils.setField(reprocessorVerticle, "reprocessCount", 3);
         Field auditLog = AuditLogRequestBuilder.class.getDeclaredField("registrationProcessorRestService");
         auditLog.setAccessible(true);
         @SuppressWarnings("unchecked")
         RegistrationProcessorRestClientService<Object> mockObj = Mockito
                                     .mock(RegistrationProcessorRestClientService.class);
         auditLog.set(auditLogRequestBuilder, mockObj);
         AuditResponseDto auditResponseDto = new AuditResponseDto();
         ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
         responseWrapper.setResponse(auditResponseDto);
//         Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
//                                      "test case description", EventId.RPR_401.toString(), EventName.ADD.toString(),
//                                      EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);
         auditLogRequestBuilder.createAuditRequestBuilder("test case description", EventId.RPR_401.toString(), EventName.ADD.toString(),
                                      EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);
	}

	@Test
	public void testProcessValid() throws TablenotAccessibleException, PacketManagerException,
			ApisResourceAccessException, WorkflowActionException {

		List<InternalRegistrationStatusDto> dtolist = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationType(RegistrationType.NEW.toString());
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		dtolist.add(registrationStatusDto);
		InternalRegistrationStatusDto registrationStatusDto1 = new InternalRegistrationStatusDto();

		registrationStatusDto1.setRegistrationId("2018701130000410092018110734");
		registrationStatusDto1.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto1.setReProcessRetryCount(1);
		registrationStatusDto1.setRegistrationType("NEW");
		registrationStatusDto1.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
		dtolist.add(registrationStatusDto1);
		Mockito.when(registrationStatusService.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenReturn(dtolist);
		reprocessorVerticle.process(dto);

	}
	
	@Test
	public void testProcessFailure() throws TablenotAccessibleException, PacketManagerException,
			ApisResourceAccessException, WorkflowActionException {

		List<InternalRegistrationStatusDto> dtolist = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");

		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setRegistrationType("NEW");
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		dtolist.add(registrationStatusDto);
		InternalRegistrationStatusDto registrationStatusDto1 = new InternalRegistrationStatusDto();

		registrationStatusDto1.setRegistrationId("2018701130000410092018110734");
		registrationStatusDto1.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto1.setReProcessRetryCount(3);
		registrationStatusDto1.setRegistrationType("NEW");
		registrationStatusDto1.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
		dtolist.add(registrationStatusDto1);
		Mockito.when(registrationStatusService.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenReturn(dtolist);
		reprocessorVerticle.process(dto);

	}

	/**
	 * Exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void exceptionTest() throws Exception {
		Mockito.when(registrationStatusService.getUnProcessedPackets(anyInt(),anyLong(), anyInt(), anyList()))
				.thenReturn(null);
		dto = reprocessorVerticle.process(dto);
		assertEquals(null, dto.getIsValid());

	}
	
	@Test
	public void nullPointerExceptionTest() throws Exception {
		Mockito.when(registrationStatusService.getResumablePackets(anyInt()))
				.thenThrow(NullPointerException.class);
		dto = reprocessorVerticle.process(dto);
		assertEquals(null, dto.getIsValid());
	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(registrationStatusService.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenThrow(new TablenotAccessibleException("") {
				});

		dto = reprocessorVerticle.process(dto);
		assertEquals(true, dto.getInternalError());

	}

	@Test
	public void testProcessValidWithResumablePackets() throws TablenotAccessibleException, PacketManagerException,
			ApisResourceAccessException, WorkflowActionException {

		List<InternalRegistrationStatusDto> dtolist = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationType(RegistrationType.NEW.toString());
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		dtolist.add(registrationStatusDto);
		List<InternalRegistrationStatusDto> reprocessorDtoList = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto1 = new InternalRegistrationStatusDto();

		registrationStatusDto1.setRegistrationId("2018701130000410092018110734");
		registrationStatusDto1.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto1.setReProcessRetryCount(1);
		registrationStatusDto1.setRegistrationType("NEW");
		registrationStatusDto1.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
		reprocessorDtoList.add(registrationStatusDto1);
		Mockito.when(registrationStatusService.getResumablePackets(anyInt()))
				.thenReturn(dtolist);
		Mockito.when(registrationStatusService.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenReturn(reprocessorDtoList);
		reprocessorVerticle.process(dto);

	}
}
