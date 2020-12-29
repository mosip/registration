/**
 * 
 */
package io.mosip.registration.processor.biodedupe.stage;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
import io.vertx.ext.web.impl.RouterImpl;

import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author M1022006
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BioDedupeStageTest {

	MessageDTO dto = new MessageDTO();

	@Mock
	private BioDedupeProcessor bioDedupeProcessor;

	private String stageName = "BioDedupeStage";
	
	@Mock
	private MosipRouter router;
	
	@Mock
	private Environment environment;

	@InjectMocks
	private BioDedupeStage bioDedupeStage = new BioDedupeStage() {
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
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	/**
	 * Test deploy verticle.
	 */
	@Test
	public void testDeployVerticle() {
		ReflectionTestUtils.setField(bioDedupeStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(bioDedupeStage, "clusterManagerUrl", "/dummyPath");
		bioDedupeStage.deployVerticle();
	}

	@Test
	public void testProcess() {
		MessageDTO result = new MessageDTO();
		result.setIsValid(true);
		Mockito.when(bioDedupeProcessor.process(any(), any())).thenReturn(result);
		dto = bioDedupeStage.process(dto);
		assertTrue(dto.getIsValid());
	}
	
	@Test
	public void testStart() {
		ReflectionTestUtils.setField(bioDedupeStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(bioDedupeStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(bioDedupeStage, "port", "1080");
		
		Mockito.when(environment.getProperty("mosip.kernel.virus-scanner.port")).thenReturn("8000");
		Mockito.when(environment.getProperty("server.servlet.path")).thenReturn("/test");
		bioDedupeStage.deployVerticle();
		Mockito.doNothing().when(router).setRoute(any());
		Router r = new RouterImpl(Vertx.vertx());
		Mockito.when(router.getRouter()).thenReturn(r);
		bioDedupeStage.start();
	}

}
