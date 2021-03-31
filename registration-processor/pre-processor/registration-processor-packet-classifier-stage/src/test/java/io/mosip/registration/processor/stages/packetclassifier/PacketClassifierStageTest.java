package io.mosip.registration.processor.stages.packetclassifier;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@RunWith(MockitoJUnitRunner.class)
public class PacketClassifierStageTest {

	MessageDTO dto = new MessageDTO();

	@Mock
	private PacketClassificationProcessor packetClassificationProcessor;

	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;

	@InjectMocks
	private PacketClassifierStage packetClassifierStage = new PacketClassifierStage() {
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
		public void consumeAndSend(MosipEventBus eventbus, MessageBusAddress addressbus1,
				MessageBusAddress addressbus2, long messageExpiryTimeLimit) {
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
	public void testStart()
	{
		ReflectionTestUtils.setField(packetClassifierStage, "port", "2321");
		Mockito.doNothing().when(router).setRoute(any());
		packetClassifierStage.start();
	}

	@Test
	public void testDeployVerticle() {
		
		ReflectionTestUtils.setField(packetClassifierStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(packetClassifierStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(packetClassifierStage, "messageExpiryTimeLimit", Long.valueOf(0));
		packetClassifierStage.deployVerticle();
	}

	@Test
	public void testProcess() {
		MessageDTO result = new MessageDTO();
		result.setIsValid(true);
		Mockito.when(packetClassificationProcessor.process(any(), any())).thenReturn(result);
		dto = packetClassifierStage.process(dto);
		assertTrue(dto.getIsValid());

	}
}
