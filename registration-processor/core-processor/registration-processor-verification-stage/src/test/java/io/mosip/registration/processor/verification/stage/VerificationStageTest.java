package io.mosip.registration.processor.verification.stage;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.mosip.kernel.core.signatureutil.model.SignatureResponse;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.verification.response.dto.VerificationResponseDTO;
import io.mosip.registration.processor.verification.service.VerificationService;
import io.mosip.registration.processor.verification.util.ManualVerificationRequestValidator;
import io.vertx.core.Vertx;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.util.ByteSequence;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)

public class VerificationStageTest {

	private static final String STAGE_NAME = "VerificationStage";

	@Mock
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;
	@Mock
	private MosipQueue mosipQueue;
	@Mock
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;
	@Mock
	private MosipRouter router;
	public RoutingContext ctx;
	@Mock
	SignatureResponse signatureResponse;
	@Mock
	private ManualVerificationRequestValidator manualVerificationRequestValidator;
	@Mock
	private VerificationService verificationService;
	@Mock
	private Environment env;
	@Mock
	private MosipEventBus mockEventbus;
	private File file;
	public FileUpload fileUpload;
	private String serviceID="";
	private byte[] packetInfo;

	@InjectMocks
	private VerificationStage verificationstage =new VerificationStage()
	{
		@Override
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}

		@Override
		public MosipEventBus getEventBus(Object verticleName, String clusterManagerUrl, int instanceNumber) {
			return mockEventbus;
		}
		@Override
		public void createServer(Router router, int port) {

		}
		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;
		}

		@Override
		public void setResponseWithDigitalSignature(RoutingContext ctx, Object object, String contentType) {

		}
		
		@Override
		public Integer getPort() {
			return 8080;
		}
	};
	@Before
	public void setUp() throws java.io.IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		ReflectionTestUtils.setField(verificationstage, "mosipConnectionFactory", mosipConnectionFactory);
		ReflectionTestUtils.setField(verificationstage, "mosipQueueManager", mosipQueueManager);
		//ReflectionTestUtils.setField(manualverificationstage, "contextPath", "/registrationprocessor/v1/manualverification");
		ReflectionTestUtils.setField(verificationstage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(verificationstage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(verificationstage, "clusterManagerUrl", "/dummyPath");
		//Mockito.when(env.getProperty(SwaggerConstant.SERVER_SERVLET_PATH)).thenReturn("/registrationprocessor/v1/manualverification");
		Mockito.when(mosipConnectionFactory.createConnection(any(),any(),any(),any())).thenReturn(mosipQueue);
		Mockito.doReturn(new String("str").getBytes()).when(mosipQueueManager).consume(any(), any(), any());
		Mockito.doNothing().when(router).setRoute(any());
		Mockito.when(router.post(any())).thenReturn(null);
		Mockito.when(router.get(any())).thenReturn(null);
		Mockito.doNothing().when(manualVerificationRequestValidator).validate(any(),any());
		Mockito.when(signatureResponse.getData()).thenReturn("gdshgsahjhghgsad");
		packetInfo="packetInfo".getBytes();
	}
	@Test
	public void testDeployeVerticle()
	{
		verificationstage.deployVerticle();
	}
	@Test
	public void testStart()
	{
		MessageDTO dto=new MessageDTO();
		verificationstage.process(dto);
		verificationstage.sendMessage(dto);
		verificationstage.start();
	}


	@Test
	public void testAllProcess() {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("12345");
		messageDTO.setIsValid(true);

		Mockito.when(verificationService.process(messageDTO, mosipQueue, STAGE_NAME)).thenReturn(messageDTO);

		MessageDTO result = verificationstage.process(messageDTO);

		assertTrue(result.getIsValid());
	}

	@Test
	public void testConsumeListener() throws JsonProcessingException {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
		when(mockAppender.getName()).thenReturn("MOCK");
		root.addAppender(mockAppender);

		VerificationResponseDTO resp = new VerificationResponseDTO();
		resp.setId("verification");
		resp.setRequestId("e2e59a9b-ce7c-41ae-a953-effb854d1205");
		resp.setResponsetime(DateUtils.getCurrentDateTimeString());
		resp.setReturnValue(1);

		String response = JsonUtils.javaObjectToJsonString(resp);

		ActiveMQBytesMessage amq = new ActiveMQBytesMessage();
		ByteSequence byteSeq = new ByteSequence();
		byteSeq.setData(response.getBytes());
		amq.setContent(byteSeq);

		Mockito.when(verificationService.updatePacketStatus(any(), any(), any())).thenReturn(true);

		verificationstage.consumerListener(amq);

		verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {

			@Override
			public boolean matches(ILoggingEvent argument) {
				return ((LoggingEvent) argument).getFormattedMessage()
						.contains("ManualVerificationStage::processDecision::success");
			}
		}));
	}

}

