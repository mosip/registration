package io.mosip.registration.processor.core.eventbus;

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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
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

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
	private KafkaProducer<String, String> kafkaProducer;

	@Before
	public void setup(TestContext testContext) throws Exception {
		vertx = Vertx.vertx();
		PowerMockito.mockStatic(KafkaConsumer.class);
		PowerMockito.mockStatic(KafkaProducer.class);
		Mockito.when(KafkaConsumer.<String, String>create(any(), anyMap()))
			.thenReturn(kafkaConsumer);
		Mockito.when(KafkaProducer.<String, String>create(any(), anyMap()))
			.thenReturn(kafkaProducer);
		
	}

	@After
	public void tearDown(TestContext testContext) {
		vertx.close();
	}

	@Test
	public void testSend(TestContext testContext) {
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", 60000);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("1001");
		messageDTO.setReg_type(RegistrationType.NEW);
		kafkaMosipEventBus.send(MessageBusAddress.PACKET_VALIDATOR_BUS_OUT, messageDTO);

		verify(kafkaProducer, times(1)).write(any(KafkaProducerRecord.class));
	}

	@Test
	public void testComsumeAndSendWithAutoCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", 60000);
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
	}

	@Test
	public void testComsumeAndSendWithSingleBatchType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"batch", "100", 60000);
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
	}

	@Test
	public void testComsumeAndSendWithSingleCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"single", "100", 60000);
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
	}

	@Test
	public void testComsumeWithAutoCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(vertx, "localhost:9091", "group_1", 
			"auto", "100", 60000);
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
	}

	@Test
	public void testComsumeWithSingleBatchType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"batch", "100", 60000);
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
	}

	@Test
	public void testComsumeWithSingleCommitType(TestContext testContext) {
		int testDataCount = 20;
		kafkaMosipEventBus = new KafkaMosipEventBus(Vertx.vertx(), "localhost:9091", "group_1", 
			"single", "100", 60000);
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
	}

	private KafkaConsumerRecords<String, String> prepareKafkaConsumerRecords(int recordCount) {
		List<ConsumerRecord<String,String>> consumerRecordList = new ArrayList<>();
		for(int i=0; i<recordCount; i++)
			consumerRecordList.add(
				new ConsumerRecord<String,String>(
					MessageBusAddress.PACKET_VALIDATOR_BUS_IN.getAddress(), 0, i, null, 
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

}
