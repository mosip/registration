package io.mosip.registration.processor.core.eventbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.exception.ConfigurationServerFailureException;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordsImpl;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

/**
 * Implementation of MosipEventBus interface for Kafka based event bus
 *
 * @author Vishwanath V
 */
public class KafkaMosipEventBus implements MosipEventBus {

	/** The logger. */
	private Logger logger = LoggerFactory.getLogger(KafkaMosipEventBus.class);

	/** The vertx instance that will be used by this event bus */
	private Vertx vertx = null;

	private KafkaProducer<String, String> kafkaProducer;

	private KafkaConsumer<String, String> kafkaConsumer;

	private String commitType;

	private int pollFrequency;

	/**
	 * Instantiates a new kafka mosip event bus.
	 *
	 * @param vertx            The vertx instance
	 * @param bootstrapServers Kafka cluster server, that producer and consumer
	 *                         should connect to
	 * @param groupId          The group id that consumer should use to associate to
	 *                         a consumer group
	 * @param commitType       The commit type that should be used by kafka
	 *                         consumer, supported types: auto, batch and single
	 * @param maxPollRecords   Maximum records that can be received in one poll to kafka
	 * @param pollFrequency    Interval between each poll calls to kafka in milli sec
	 */
	public KafkaMosipEventBus(Vertx vertx, String bootstrapServers, String groupId, 
			String commitType, String maxPollRecords, int pollFrequency) {

		validateCommitType(commitType);
		this.vertx = vertx;
		this.commitType = commitType;
		this.pollFrequency = pollFrequency;

		Map<String, String> consumerConfig = new HashMap<>();
		consumerConfig.put("bootstrap.servers", bootstrapServers);
		consumerConfig.put("key.deserializer", 
			"org.apache.kafka.common.serialization.StringDeserializer");
		consumerConfig.put("value.deserializer", 
			"org.apache.kafka.common.serialization.StringDeserializer");
		consumerConfig.put("group.id", groupId);
		consumerConfig.put("auto.offset.reset", "latest");
		consumerConfig.put("max.poll.records", maxPollRecords);
		if (commitType == "auto")
			consumerConfig.put("enable.auto.commit", "true");
		else
			consumerConfig.put("enable.auto.commit", "false");
		this.kafkaConsumer = KafkaConsumer.create(vertx, consumerConfig);

		Map<String, String> producerConfig = new HashMap<>();
		producerConfig.put("bootstrap.servers", bootstrapServers);
		producerConfig.put("key.serializer", 
			"org.apache.kafka.common.serialization.StringSerializer");
		producerConfig.put("value.serializer", 
			"org.apache.kafka.common.serialization.StringSerializer");
		producerConfig.put("acks", "1");
		this.kafkaProducer = KafkaProducer.create(vertx, producerConfig);

		logger.info("KafkaMosipEventBus loaded with configuration: bootstrapServers:" + 
			bootstrapServers + " groupId:" + groupId + " commitType:" + commitType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * getEventbus()
	 */
	@Override
	public Vertx getEventbus() {
		return this.vertx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * consume()
	 */
	@Override
	public void consume(MessageBusAddress fromAddress,
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		logger.info("consume called with fromAddress " + fromAddress.getAddress());
		kafkaConsumer.subscribe(fromAddress.getAddress());
		poll(null, eventHandler);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * consumeAndSend()
	 */
	@Override
	public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		logger.info("consumeAndSend called with fromAddress " + fromAddress.getAddress() + 
			" and toAddress " + toAddress.getAddress());
		kafkaConsumer.subscribe(fromAddress.getAddress());
		poll(toAddress, eventHandler);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * send()
	 */
	@Override
	public void send(MessageBusAddress toAddress, MessageDTO message) {
		MessageBusAddress messageBusAddress = 
			new MessageBusAddress(toAddress, message.getReg_type());
		JsonObject jsonObject = JsonObject.mapFrom(message);
		logger.debug("send called with toAddress " + toAddress.getAddress() + 
			" for message " + jsonObject.toString());
		KafkaProducerRecord<String, String> producerRecord = 
			KafkaProducerRecord.create(messageBusAddress.getAddress(), message.getRid(), 
				jsonObject.toString());
  		kafkaProducer.write(producerRecord);
	}

	private void poll(MessageBusAddress toAddress,
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		this.kafkaConsumer.poll(100, pollResult -> {
			KafkaConsumerRecords<String, String> kafkaConsumerRecords = pollResult.result();
			if (kafkaConsumerRecords.size() == 0)
				logger.debug("Records size is zero");
			else
				logger.debug("Records size is "+kafkaConsumerRecords.size());

			Set<org.apache.kafka.common.TopicPartition> topicPartitions = 
				kafkaConsumerRecords.records().partitions();
			topicPartitions.forEach(topicPartition -> {
				KafkaConsumerRecords<String, String> consumerRecords = 
					getPartitionKafkaConsumerRecords(topicPartition, kafkaConsumerRecords);
				logger.debug("Partition: " + topicPartition.partition() + 
					" recordSize: " + consumerRecords.size());

				Future<Void> processingFuture = null;
				if(this.commitType.equals("single"))
					processingFuture = setupSingleCommitProcessing(consumerRecords, 
						toAddress, eventHandler);
				else if(this.commitType.equals("batch"))
					processingFuture = setupBatchCommitProcessing(consumerRecords, 
						toAddress, eventHandler);
				else if(this.commitType.equals("auto"))
					processingFuture = setupAutoCommitProcessing(consumerRecords, 
						toAddress, eventHandler);

				processingFuture.onSuccess(any -> {
					logger.debug(consumerRecords.size() + " messages processed for partition: " + 
						topicPartition.partition());
				}).onFailure(cause ->  {
					logger.debug("Error persisting and committing messages: " + cause);
				});
			});

			vertx.setTimer(pollFrequency, result -> {
				poll(toAddress, eventHandler);
			});
		});
	}

	Future<Void> setupSingleCommitProcessing(
			KafkaConsumerRecords<String, String> consumerRecords,
			MessageBusAddress toAddress, 
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

		TopicPartition vertxTopicPartition = new TopicPartition(
				consumerRecords.recordAt(0).topic(), consumerRecords.recordAt(0).partition());
		Promise<Void> pausePromise = Promise.promise();
		pausePartition(vertxTopicPartition, pausePromise);

		Future<Void> newFuture = pausePromise.future()
			.compose((Void) -> IntStream.range(0, consumerRecords.size())
				.mapToObj(consumerRecords::recordAt)
				.reduce(Future.<Void>succeededFuture(),
					(acc, record) -> acc.compose(it -> 
						processRecord(toAddress, eventHandler, record, true)),
					(a, b) -> a));

		return newFuture.compose(composite -> {
			Promise<Void> resumePromise = Promise.promise();
			resumePartition(vertxTopicPartition, resumePromise);
			return resumePromise.future();
		});
	}

	Future<Void> setupBatchCommitProcessing(
			KafkaConsumerRecords<String, String> consumerRecords,
			MessageBusAddress toAddress, 
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

		long commitOffset = consumerRecords.recordAt(consumerRecords.size()-1).offset();
		TopicPartition vertxTopicPartition = new TopicPartition(
				consumerRecords.recordAt(0).topic(), consumerRecords.recordAt(0).partition());
		Promise<Void> pausePromise = Promise.promise();
		pausePartition(vertxTopicPartition, pausePromise);

		Future<Void> future = pausePromise.future()
			.compose((Void) -> {
				List<Future<Void>> futures = IntStream.range(0, consumerRecords.size())
					.mapToObj(consumerRecords::recordAt)
					.map(record -> processRecord(toAddress, eventHandler, record, false))
					.collect(Collectors.toList());

				return CompositeFuture.all(new ArrayList<>(futures));
			})
			.compose(composite -> {
				Promise<Void> commitPromise = Promise.promise();
				kafkaConsumer.commit(
					getTopicPartitionOffsetMap(vertxTopicPartition, commitOffset), result -> {
					logger.debug("Commit status for partition:" + 
						vertxTopicPartition.getPartition() + " offset: " + commitOffset + 
						" status: " + result.succeeded()); 
					if(result.succeeded())
						resumePartition(vertxTopicPartition, commitPromise);
					else
						commitPromise.fail("Commit failed for partition:" + 
							vertxTopicPartition.getPartition() + " offset: " + commitOffset);
				});
				return commitPromise.future();
			});
		return future;
	}

	Future<Void> setupAutoCommitProcessing(
			KafkaConsumerRecords<String, String> consumerRecords,
			MessageBusAddress toAddress, 
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		
		Promise<Void> promise = Promise.promise();
		List<Future<Void>> futures = IntStream.range(0, consumerRecords.size())
			.mapToObj(consumerRecords::recordAt)
			.map(record -> processRecord(toAddress, eventHandler, record, false))
			.collect(Collectors.toList());
		
		CompositeFuture.all(new ArrayList<>(futures))
			.onSuccess(composite -> {
				promise.complete();
			})
			.onFailure(cause -> {
				promise.fail(cause);
			});

		return promise.future();
	}

	Future<Void> processRecord(MessageBusAddress toAddress,
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler,
		KafkaConsumerRecord<String, String> record, boolean commitRecord) {
		logger.debug("Processing key=" + record.key() + ",value=" + record.value() +
				",partition=" + record.partition() + ",offset=" + record.offset());

		EventDTO eventDTO = new EventDTO();
		eventDTO.setBody((JsonObject) new JsonObject(record.value()));
		Promise<Void> promise = Promise.promise();
		eventHandler.handle(eventDTO, res -> {
			if (!res.succeeded()) {
				logger.error("Event handling failed " + res.cause());
				promise.fail(res.cause());
			} else {
				if(toAddress != null) {
					MessageDTO messageDTO = res.result();
					MessageBusAddress messageBusToAddress = 
						new MessageBusAddress(toAddress, messageDTO.getReg_type());
					JsonObject jsonObject = JsonObject.mapFrom(messageDTO);
					KafkaProducerRecord<String, String> producerRecord = 
						KafkaProducerRecord.create(messageBusToAddress.getAddress(), 
							messageDTO.getRid(), jsonObject.toString());
					kafkaProducer.write(producerRecord);
				}
				if(commitRecord)
					commitOffset(record.topic(), record.partition(), 
						record.offset(), promise);
				else					
					promise.complete();
			}
		});
		return promise.future();
	}

	private void commitOffset(String topic, int partition, long offset, 
			Promise<Void> promise) {
		
		TopicPartition topicPartition = new TopicPartition(topic, partition);
		kafkaConsumer.commit(getTopicPartitionOffsetMap(topicPartition, offset), result -> {
			logger.debug("Commit status for partition:" + partition + " offset: " + 
				offset + " status: " + result.succeeded()); 
			if(result.succeeded())
				promise.complete();
			else 
				promise.fail("Commit failed for partition: " + partition + " offset: " + offset);
		});
	}

	private Map<TopicPartition,OffsetAndMetadata> getTopicPartitionOffsetMap(
			TopicPartition topicPartition, long offset) {
		
		Map<TopicPartition,OffsetAndMetadata> topicPartitionOffsetMap = 
			new HashMap<TopicPartition,OffsetAndMetadata>();
		//offset+1 is because we have to commit the next offset to start 
		//picking up from there
		topicPartitionOffsetMap.put(topicPartition, 
			new OffsetAndMetadata().setOffset(offset+1));
		return topicPartitionOffsetMap;
	}

	private KafkaConsumerRecords<String, String> getPartitionKafkaConsumerRecords(
			org.apache.kafka.common.TopicPartition topicPartition, 
			KafkaConsumerRecords<String, String> kafkaConsumerRecords) {
		List<ConsumerRecord<String,String>> partitionRecordList = 
			new ArrayList<>(kafkaConsumerRecords.records().records(topicPartition));
		partitionRecordList.sort((r1, r2) -> Long.compare(r1.offset(),r2.offset()));

		Map<org.apache.kafka.common.TopicPartition, 
		List<ConsumerRecord<String, String>>> topicPartitionConsumerRecordsMap = 
			new HashMap<org.apache.kafka.common.TopicPartition, 
			List<ConsumerRecord<String, String>>>();
		topicPartitionConsumerRecordsMap.put(topicPartition, partitionRecordList);

		KafkaConsumerRecords<String, String> partitionConsumerRecords = 
			new KafkaConsumerRecordsImpl<String, String>(
				new ConsumerRecords<String, String>(topicPartitionConsumerRecordsMap));
		return partitionConsumerRecords;
	}

	private void validateCommitType(String commitType) {
		String[] supportedCommitTyes = {"auto", "batch", "single"};
		if(!Arrays.asList(supportedCommitTyes).contains(commitType))
			throw new ConfigurationServerFailureException(
				"Commit type configuration not supported for "+ commitType);
	}

	private void pausePartition(TopicPartition topicPartition, Promise<Void> promise) {
		kafkaConsumer.pause(topicPartition, result -> {
			logger.debug("Partition is paused " + topicPartition.getPartition() + " " + 
				result.succeeded());
			if (result.succeeded())
				promise.complete();
			else
				promise.fail("Partition pausing failed for " + topicPartition.getPartition());
			
		});
	}
	private void resumePartition(TopicPartition topicPartition, Promise<Void> promise) {
		kafkaConsumer.resume(topicPartition, resumeResult -> {
			logger.debug("Partition is resumed " + topicPartition.getPartition() + 
				" " +resumeResult.succeeded());
			if (resumeResult.succeeded())
				promise.complete();
			else
				promise.fail("Partition resuming failed for " + topicPartition.getPartition());
		});
	}

}
