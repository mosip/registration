package io.mosip.registration.processor.packet.uploader.stage.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import io.mosip.registration.processor.packet.uploader.stage.PacketUploaderStage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
/**
 * The Class PacketUploaderJobTest.
 * @author Rishabh Keshari
 */
@RunWith(SpringRunner.class)
public class PacketUploaderStageTest {
	MessageDTO dto = new MessageDTO();
	@Mock
	private PacketUploaderService<MessageDTO> packetUploaderService;

	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;
	@InjectMocks
	private PacketUploaderStage packetValidatorStage = new PacketUploaderStage() {
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

				@Override
				public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}

				@Override
				public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

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

		@Override
		public Integer getPort() {
			return 8080;
		};
	};
	@Test
	public void testStart()
	{
		Mockito.doNothing().when(router).setRoute(any());
		packetValidatorStage.start();
	}

	/**
	 * Test deploy verticle.
	 */
	@Test
	public void testDeployVerticle() {

		ReflectionTestUtils.setField(packetValidatorStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(packetValidatorStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(packetValidatorStage, "messageExpiryTimeLimit", Long.valueOf(0));
		packetValidatorStage.deployVerticle();
	}

	@Test
	public void testProcess() {
		MessageDTO result = new MessageDTO();
		result.setIsValid(true);
		Mockito.when(packetUploaderService.validateAndUploadPacket(any(), any())).thenReturn(result);
		dto = packetValidatorStage.process(dto);
		assertTrue(dto.getIsValid());

	}
}
