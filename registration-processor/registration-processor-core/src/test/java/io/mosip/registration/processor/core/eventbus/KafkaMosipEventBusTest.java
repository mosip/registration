package io.mosip.registration.processor.core.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.cache.CaffeineCacheManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.test.util.ReflectionTestUtils;

import brave.Tracing;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.MessageExpiredException;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.tracing.EventTracingHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.kafka.client.common.PartitionInfo;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordsImpl;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
@PrepareForTest({ KafkaConsumer.class, KafkaProducer.class })
public class KafkaMosipEventBusTest {

	private Vertx vertx;

	/** The Kafka mosip event bus. */
	private KafkaMosipEventBus kafkaMosipEventBus;

	@Mock
	private KafkaConsumer<String, String> kafkaConsumer;

	@Mock
	private CaffeineCacheManager caffeineCacheManager;

	@Mock
	private KafkaProducer<String, String> kafkaProducer;

	private Tracing tracing = Tracing.newBuilder().build();

	private EventTracingHandler eventTracingHandler;

	@Before
	public void setup(TestContext testContext) throws Exception {
		vertx = Vertx.vertx();
		PowerMockito.mockStatic(KafkaConsumer.class);
		PowerMockito.mockStatic(KafkaProducer.class);
		Mockito.when(KafkaConsumer.<String, String>create(any(), anyMap()))
			.thenReturn(kafkaConsumer);
		Mockito.when(KafkaProducer.<String, String>create(any(), anyMap()))
			.thenReturn(kafkaProducer);

		eventTracingHandler = new EventTracingHandler(tracing, "kafka");
	}

	@After
	public void tearDown(TestContext testContext) {
		vertx.close();
	}

	@Test
	public void testSend(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("1001");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		kafkaMosipEventBus.send(MessageBusAddress.PACKET_VALIDATOR_BUS_OUT, messageDTO);

		verify(kafkaProducer, times(1)).write(any(KafkaProducerRecord.class),any(Handler.class));
	}

	@Test
	public void testConsumeAndSendWithAutoCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);
		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            
            return null;
		}).when(eventHandler).handle(any(), any());

		kafkaMosipEventBus.consumeAndSend(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, 
			MessageBusAddress.PACKET_UPLOADER_OUT, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());
		verify(kafkaConsumer, times(0)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(0)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(0)).commit(anyMap(), any());
		verify(kafkaProducer, times(testDataCount)).write(any(), any());
	}

	@Test
	public void testConsumeAndSendWithSingleBatchType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"batch", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
			
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);

		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            return null;
		}).when(eventHandler).handle(any(), any());
		
		kafkaMosipEventBus.consumeAndSend(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, 
			MessageBusAddress.PACKET_UPLOADER_OUT, eventHandler);
		async.await();

		InOrder inOrder = Mockito.inOrder(kafkaConsumer, eventHandler);
		inOrder.verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		inOrder.verify(eventHandler, times(testDataCount)).handle(any(), any());
		inOrder.verify(kafkaConsumer, times(1)).commit(anyMap(), any());
		inOrder.verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaProducer, times(testDataCount)).write(any(), any());
	}

	@Test
	public void testConsumeAndSendWithSingleCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"single", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
			
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);

		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            return null;
		}).when(eventHandler).handle(any(), any());
		
		kafkaMosipEventBus.consumeAndSend(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, 
			MessageBusAddress.PACKET_UPLOADER_OUT, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());

		InOrder inOrder = Mockito.inOrder(kafkaConsumer);
		inOrder.verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		inOrder.verify(kafkaConsumer, times(testDataCount)).commit(anyMap(), any());
		inOrder.verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());

		ArgumentCaptor<Map<io.vertx.kafka.client.common.TopicPartition,OffsetAndMetadata>> argument = 
			ArgumentCaptor.forClass(Map.class);
		verify(kafkaConsumer, atLeastOnce()).commit(argument.capture(), any());
		List<Map<io.vertx.kafka.client.common.TopicPartition,OffsetAndMetadata>> values = 
			argument.getAllValues();
		for(int i=0; i<testDataCount; i++){
			//offset+1 is because we have to commit the next offset to start 
			//picking up from there
			assertTrue("Commit method should be called in same order of offset for each partition", 
				values.get(i).entrySet().iterator().next().getValue().getOffset() == i+1);
		}
		verify(kafkaProducer, times(testDataCount)).write(any(), any());
	}

	@Test
	public void testConsumeWithAutoCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);
		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            
            return null;
		}).when(eventHandler).handle(any(), any());

		kafkaMosipEventBus.consume(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());
		verify(kafkaConsumer, times(0)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(0)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(0)).commit(anyMap(), any());
		verify(kafkaProducer, times(0)).write(any(), any());
	}

	@Test
	public void testConsumeWithSingleBatchType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"batch", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
			
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);

		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            return null;
		}).when(eventHandler).handle(any(), any());
		
		kafkaMosipEventBus.consume(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, eventHandler);
		async.await();

		InOrder inOrder = Mockito.inOrder(kafkaConsumer, eventHandler);
		inOrder.verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		inOrder.verify(eventHandler, times(testDataCount)).handle(any(), any());
		inOrder.verify(kafkaConsumer, times(1)).commit(anyMap(), any());
		inOrder.verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaProducer, times(0)).write(any(), any());
	}

	@Test
	public void testConsumeWithSingleCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"single", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
			
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);

		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(true);
			JsonObject jsonObject = (JsonObject) ((EventDTO) arguments.getArgument(0)).getBody();
			MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
			Mockito.when(asyncResultForMessageDTO.result()).thenReturn(messageDTO);
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            return null;
		}).when(eventHandler).handle(any(), any());
		
		kafkaMosipEventBus.consume(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());

		InOrder inOrder = Mockito.inOrder(kafkaConsumer);
		inOrder.verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		inOrder.verify(kafkaConsumer, times(testDataCount)).commit(anyMap(), any());
		inOrder.verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());

		ArgumentCaptor<Map<io.vertx.kafka.client.common.TopicPartition,OffsetAndMetadata>> argument = 
			ArgumentCaptor.forClass(Map.class);
		verify(kafkaConsumer, atLeastOnce()).commit(argument.capture(), any());
		List<Map<io.vertx.kafka.client.common.TopicPartition,OffsetAndMetadata>> values = 
			argument.getAllValues();
		for(int i=0; i<testDataCount; i++){
			//offset+1 is because we have to commit the next offset to start 
			//picking up from there
			assertTrue("Commit method should be called in same order of offset for each partition", 
				values.get(i).entrySet().iterator().next().getValue().getOffset() == i+1);
		}
		verify(kafkaProducer, times(0)).write(any(), any());
	}

	@Test
	public void testConsumeAndSendWithMessageExpiredException(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"batch", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);
		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(false);
			Mockito.when(asyncResultForMessageDTO.cause()).thenReturn(new MessageExpiredException());
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            
            return null;
		}).when(eventHandler).handle(any(), any());

		kafkaMosipEventBus.consumeAndSend(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, 
			MessageBusAddress.PACKET_UPLOADER_OUT, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());
		verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(1)).commit(anyMap(), any());
		verify(kafkaProducer, times(0)).write(any(), any());
	}

	@Test
	public void testConsumeWithMessageExpiredException(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"batch", "100", "30000", 60000, eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		
		AsyncResult<KafkaConsumerRecords<String, String>> asyncResult = 
			Mockito.mock(AsyncResult.class);
  		Mockito.when(asyncResult.succeeded()).thenReturn(true);
  		Mockito.when(asyncResult.result()).thenReturn(prepareKafkaConsumerRecords(testDataCount));
		doAnswer((Answer<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments -> {
			((Handler<AsyncResult<KafkaConsumerRecords<String, String>>>) arguments.getArgument(1))
				.handle(asyncResult);
            return null;
		}).when(kafkaConsumer).poll(anyLong(), any());

		AsyncResult<Void> voidAsyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(voidAsyncResult.succeeded()).thenReturn(true);
		  
		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).pause(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).resume(any(io.vertx.kafka.client.common.TopicPartition.class), any());

		doAnswer((Answer<AsyncResult<Void>>) arguments -> {
            ((Handler<AsyncResult<Void>>) arguments.getArgument(1)).handle(voidAsyncResult);
            return null;
		}).when(kafkaConsumer).commit(anyMap(), any());

		EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler = 
			Mockito.mock(EventHandler.class);
		doAnswer((Answer<AsyncResult<MessageDTO>>) arguments -> {
			AsyncResult<MessageDTO> asyncResultForMessageDTO = Mockito.mock(AsyncResult.class);
			Mockito.when(asyncResultForMessageDTO.succeeded()).thenReturn(false);
			Mockito.when(asyncResultForMessageDTO.cause()).thenReturn(new MessageExpiredException());
			((Handler<AsyncResult<MessageDTO>>) arguments.getArgument(1))
				.handle(asyncResultForMessageDTO);
			if (!async.isCompleted())
				async.complete();
            
            return null;
		}).when(eventHandler).handle(any(), any());

		kafkaMosipEventBus.consume(MessageBusAddress.PACKET_VALIDATOR_BUS_IN, eventHandler);
		async.await();

		verify(eventHandler, times(testDataCount)).handle(any(), any());
		verify(kafkaConsumer, times(1)).pause(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(1)).resume(
			any(io.vertx.kafka.client.common.TopicPartition.class), any());
		verify(kafkaConsumer, times(1)).commit(anyMap(), any());
		verify(kafkaProducer, times(0)).write(any(), any());
	}

	private KafkaConsumerRecords<String, String> prepareKafkaConsumerRecords(int recordCount) {
		List<ConsumerRecord<String,String>> consumerRecordList = new ArrayList<>();
		for(int i=0; i<recordCount; i++)
			consumerRecordList.add(
				new ConsumerRecord<String,String>(
					MessageBusAddress.PACKET_VALIDATOR_BUS_IN.getAddress(), 0, i, "1000"+i,
					"{\"rid\":\"1000"+i+"\", \"reg_type\": \"NEW\" }"));
		Map<TopicPartition, List<ConsumerRecord<String,String>>> topicPartitionConsumerRecordListMap = 
			new HashMap<TopicPartition, List<ConsumerRecord<String,String>>>();
		topicPartitionConsumerRecordListMap.put(
			new TopicPartition(MessageBusAddress.PACKET_VALIDATOR_BUS_IN.getAddress(), 0), 
				consumerRecordList);
		ConsumerRecords<String,String> consumerRecords = 
			new ConsumerRecords<String,String>(topicPartitionConsumerRecordListMap);
		KafkaConsumerRecords<String, String> kafkaConsumerRecords = 
			new KafkaConsumerRecordsImpl<String, String>(consumerRecords);
		return kafkaConsumerRecords;
	}

	@Test
	public void testConsumerHealthCheck(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", "batch", "100", "30000", 60000,
				eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		Handler<HealthCheckDTO> eventHandler = Mockito.mock(Handler.class);
		AsyncResult<Map<String, List<PartitionInfo>>> asyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(asyncResult.succeeded()).thenReturn(true);
		doAnswer((Answer<AsyncResult<Map<String, List<PartitionInfo>>>>) arguments -> {
			((Handler<AsyncResult<Map<String, List<PartitionInfo>>>>) arguments.getArgument(0)).handle(asyncResult);
			if (!async.isCompleted())
				async.complete();
			return null;
		}).when(kafkaConsumer).listTopics(any());
		kafkaMosipEventBus.consumerHealthCheck(eventHandler, MessageBusAddress.PACKET_VALIDATOR_BUS_IN.toString());
		async.await();
		ArgumentCaptor<HealthCheckDTO> argument = ArgumentCaptor.forClass(HealthCheckDTO.class);
		verify(eventHandler, times(1)).handle(argument.capture());
		assertTrue(argument.getValue().isEventBusConnected());
		verify(kafkaConsumer, times(1)).listTopics(any());
	}

	@Test
	public void testConsumerHealthCheckWithException(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", "batch", "100", "30000", 60000,
				eventTracingHandler, caffeineCacheManager);
		final Async async = testContext.async();
		Handler<HealthCheckDTO> eventHandler = Mockito.mock(Handler.class);
		AsyncResult<Map<String, List<PartitionInfo>>> asyncResult = Mockito.mock(AsyncResult.class);
		Mockito.when(asyncResult.succeeded()).thenReturn(false);
		Mockito.when(asyncResult.cause()).thenReturn(new Exception("kafka consumer failed"));
		doAnswer((Answer<AsyncResult<Map<String, List<PartitionInfo>>>>) arguments -> {
			((Handler<AsyncResult<Map<String, List<PartitionInfo>>>>) arguments.getArgument(0)).handle(asyncResult);
			if (!async.isCompleted())
				async.complete();
			return null;
		}).when(kafkaConsumer).listTopics(any());
		kafkaMosipEventBus.consumerHealthCheck(eventHandler, MessageBusAddress.PACKET_VALIDATOR_BUS_IN.toString());
		async.await();
		ArgumentCaptor<HealthCheckDTO> argument = ArgumentCaptor.forClass(HealthCheckDTO.class);
		verify(eventHandler, times(1)).handle(argument.capture());
		assertFalse(argument.getValue().isEventBusConnected());
		assertEquals("kafka consumer failed", argument.getValue().getFailureReason());
		verify(kafkaConsumer, times(1)).listTopics(any());
	}

	@Test(expected = TimeoutException.class)
	public void testConsumerHealthCheckWithTimeout(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", "batch", "100", "30000", 60000,
				eventTracingHandler, caffeineCacheManager);
		Handler<HealthCheckDTO> eventHandler = Mockito.mock(Handler.class);
		TimeoutException timeout = new TimeoutException();
		Mockito.when(kafkaConsumer.listTopics(any())).thenThrow(timeout);
		kafkaMosipEventBus.consumerHealthCheck(eventHandler, MessageBusAddress.PACKET_VALIDATOR_BUS_IN.toString());
		verify(kafkaConsumer, times(1)).listTopics(any());
	}

	@Test
	public void testSenderHealthCheck(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", "batch", "100", "30000", 60000,
				eventTracingHandler, caffeineCacheManager);
		Handler<HealthCheckDTO> eventHandler = Mockito.mock(Handler.class);
		kafkaMosipEventBus.senderHealthCheck(eventHandler, MessageBusAddress.PACKET_VALIDATOR_BUS_IN.toString());
		ArgumentCaptor<HealthCheckDTO> argument = ArgumentCaptor.forClass(HealthCheckDTO.class);
		verify(eventHandler, times(1)).handle(argument.capture());
		assertTrue(argument.getValue().isEventBusConnected());
	}

	@Test
	public void testSenderHealthCheckFail(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", "batch", "100", "30000", 60000,
				eventTracingHandler, caffeineCacheManager);
		ReflectionTestUtils.setField(kafkaMosipEventBus, "kafkaProducer", null);
		Handler<HealthCheckDTO> eventHandler = Mockito.mock(Handler.class);
		kafkaMosipEventBus.senderHealthCheck(eventHandler, MessageBusAddress.PACKET_VALIDATOR_BUS_IN.toString());
		ArgumentCaptor<HealthCheckDTO> argument = ArgumentCaptor.forClass(HealthCheckDTO.class);
		verify(eventHandler, times(1)).handle(argument.capture());
		assertFalse(argument.getValue().isEventBusConnected());
	}
}
