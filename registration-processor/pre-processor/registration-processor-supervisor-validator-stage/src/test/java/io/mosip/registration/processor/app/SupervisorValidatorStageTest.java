package io.mosip.registration.processor.app;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidationProcessor;
import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidatorStage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@RunWith(MockitoJUnitRunner.class)
public class SupervisorValidatorStageTest {

	MessageDTO dto = new MessageDTO();

	@Mock
	private SupervisorValidationProcessor supervisorValidationProcessor;

	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;

	@InjectMocks
	private SupervisorValidatorStage supervisorValidatorStage = new SupervisorValidatorStage() {
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
		public Integer getPort() {
			return 8080;
		};

		@Override
		public void consumeAndSend(MosipEventBus eventbus, MessageBusAddress addressbus1, MessageBusAddress addressbus2,
				long messageExpiryTimeLimit) {
		}

		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;

		}

		@Override
		public void createServer(Router router, int port) {

		}
	};

	@Test
	public void testStart() {
		Mockito.doNothing().when(router).setRoute(any());
		supervisorValidatorStage.start();
	}

	@Test
	public void testDeployVerticle() {

		ReflectionTestUtils.setField(supervisorValidatorStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(supervisorValidatorStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(supervisorValidatorStage, "messageExpiryTimeLimit", Long.valueOf(0));
		supervisorValidatorStage.deployVerticle();
	}

	@Test
	public void testProcess() {
		MessageDTO result = new MessageDTO();
		result.setIsValid(true);
		Mockito.when(supervisorValidationProcessor.process(any(), any())).thenReturn(result);
		dto = supervisorValidatorStage.process(dto);
		assertTrue(dto.getIsValid());

	}

}
