package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionJob;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowActionJobSchedularTest {
	/**
	 * Mocked Vertx instance
	 */
	@Mock
	public Vertx vertx;

	/**
	 * Mocked Vertx Async Handler
	 */
	@Mock
	AsyncResult<String> res;

	/**
	 * Mocked Spring Environment
	 */
	@Mock
	Environment env;

	private Logger fooLogger;

	private ListAppender<ILoggingEvent> listAppender;

	/**
	 * Setup for test
	 */
	@Before
	public void setup() {
		fooLogger = (Logger) LoggerFactory.getLogger(WorkflowActionJob.class);
		listAppender = new ListAppender<>();
		ReflectionTestUtils.setField(workflowActionJob, "clusterManagerUrl", "/dummyPath");
	}


	@InjectMocks
	WorkflowActionJob workflowActionJob = new WorkflowActionJob() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * io.mosip.registration.processor.core.abstractverticle.MosipVerticleManager#
		 * getEventBus(java.lang.Object, java.lang.String)
		 */
		@Override
		public MosipEventBus getEventBus(Object verticleName, String clusterManagerUrl) {
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
	};

	/**
	 * Success Test for deployment of ReprocessorVerticle
	 */
	@Test
	public void testDeploySuccess() {
		workflowActionJob.deployVerticle();
		assertNotNull(workflowActionJob.mosipEventBus);
	}

	/**
	 * Success Test for Chime Scheduler deployment
	 */
	@Test
	public void testDeploySchedulerTest() {
		listAppender.start();
		fooLogger.addAppender(listAppender);
		Mockito.when(res.succeeded()).thenReturn(true);
		Mockito.when(vertx.eventBus()).thenReturn(getMockEventBus());
		workflowActionJob.schedulerResult(res);
		Assertions.assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
				.contains(Tuple.tuple(Level.INFO,
						"WorkflowActionJob::schedular()::deployed"));
	}

	/**
	 * Failure Test for Chime Scheduler deployment
	 */
	@Test
	public void testDeploySchedulerFailureTest() {
		listAppender.start();
		fooLogger.addAppender(listAppender);
		Mockito.when(res.succeeded()).thenReturn(false);
		workflowActionJob.schedulerResult(res);
		Assertions.assertThat(listAppender.list).extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
				.contains(Tuple.tuple(Level.ERROR, "WorkflowActionJob::schedular()::deployment failure"));
	}

	/**
	 * Returns dummy eventbus instance
	 * 
	 * @return Eventbus
	 */
	public EventBus getMockEventBus() {
		return new EventBus() {

			@Override
			public boolean isMetricsEnabled() {
				return false;
			}

			@Override
			public EventBus unregisterDefaultCodec(Class clazz) {
				return null;
			}

			@Override
			public EventBus unregisterCodec(String name) {
				return null;
			}

			@Override
			public void start(Handler<AsyncResult<Void>> completionHandler) {
			}

			@Override
			public <T> MessageProducer<T> sender(String address, DeliveryOptions options) {
				return null;
			}

			@Override
			public <T> MessageProducer<T> sender(String address) {
				return null;
			}

			@Override
			public <T> EventBus send(String address, Object message, DeliveryOptions options,
					Handler<AsyncResult<Message<T>>> replyHandler) {
				return null;
			}

			@Override
			public EventBus send(String address, Object message, DeliveryOptions options) {
				return null;
			}

			@Override
			public <T> EventBus send(String address, Object message, Handler<AsyncResult<Message<T>>> replyHandler) {
				return null;
			}

			@Override
			public EventBus send(String address, Object message) {
				return null;
			}

			@Override
			public <T> EventBus registerDefaultCodec(Class<T> clazz, MessageCodec<T, ?> codec) {
				return null;
			}

			@Override
			public EventBus registerCodec(MessageCodec codec) {
				return null;
			}

			@Override
			public <T> MessageProducer<T> publisher(String address, DeliveryOptions options) {
				return null;
			}

			@Override
			public <T> MessageProducer<T> publisher(String address) {
				return null;
			}

			@Override
			public EventBus publish(String address, Object message, DeliveryOptions options) {
				return null;
			}

			@Override
			public EventBus publish(String address, Object message) {
				return null;
			}

			@Override
			public <T> MessageConsumer<T> localConsumer(String address, Handler<Message<T>> handler) {
				return null;
			}

			@Override
			public <T> MessageConsumer<T> localConsumer(String address) {
				return null;
			}

			@Override
			public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
				return null;
			}

			@Override
			public <T> MessageConsumer<T> consumer(String address) {
				return null;
			}

			@Override
			public void close(Handler<AsyncResult<Void>> completionHandler) {
			}

			@Override
			public <T> EventBus addOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
				return null;
			}

			@Override
			public <T> EventBus removeOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
				return null;
			}

			@Override
			public <T> EventBus addInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
				return null;
			}

			@Override
			public <T> EventBus removeInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
				return null;
			}

		};
	}
}
