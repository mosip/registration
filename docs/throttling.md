# Throttling packet processing in registration processor

## Introduction
The registration processor module is responsible for processing the registration packets and generating a unique UIN for each applicant. In the registration processor module, we have used SEDA (Staged event-driven architecture) pattern, so the packet processing happens in multiple stages.

Usually, by the end of a day of field registrations, packets are uploaded in bulk from multiple registration clients to the registration processor. Since packets have to flow through multiple stages of checks and processing, all the packets cannot be processed instantly. So, to streamline this processing operation we have throttling capability in the registration processor.

Registration processor currently supports two types of throttling.

## Stage wise throttling through Kafka
Stage wise throttling controls the number of packets consumed by each stage for a unit of time.

The stages are connected using Kafka queue, once a stage completes its processing it sends the packet handle to a Kafka topic which is consumed by the camel bridge to perform routing and it then sends the packet handle to a different Kafka topic for the next stage to pick up. 

So, the packets are waiting in the Kafka queue before the stage picks them for processing, in this throttling, we control the number of packets that a stage can pick up from Kafka using the below configurations.

`mosip.regproc.<stage_name>.eventbus.kafka.max.poll.records=100`
This config specifies the max number of packets it can fetch from Kafka in a single poll

`mosip.regproc.<stage_name>.eventbus.kafka.poll.frequency=100`
This config specifies in milliseconds, how frequent the stage can poll Kafka for new packets

`mosip.regproc.<stage_name>.eventbus.kafka.commit.type=single`
This config specifies how frequent the stage has to confirm back to Kafka that it completed the processing of the packets so, in case of stage replica crashing, it does not reprocess this packet with a different replica. 

### Commit types
* **auto**:  Commits immediately back to Kafka irrespective of the completion of processing. This allows maximum parallel processing but does guarantee that all the packets are processed by the stage. This setting is best suited in case the underlying infra is set for auto-scaling.

* **single**:  Commits back to Kafka once for each message, this provides a guarantee of processing all the packets at least once but the maximum parallel processing allowed depends on the number of Kafka partitions assign to one stage replica.

* **batch**: It balances between auto and single by maximum parallel processing possible and committing back to Kafka after each batch of item processing is completed.

`mosip.regproc.<stage_name>.message.expiry-time-limit=3600`
This config specifies after how long a packet staying in a queue for a particular stage can be considered as expired so it is safely reprocessed by the re-processor. 

Note: As Kafka has no way to expire a message, the expiry is respected when the stage picks it up.

Advantages:
1. Very efficient in using the hardware resource, since slowness in one stage will not affect the previous stage processing speed.

1. No extra manual effort is required to adjust the packet processing speed.

Disadvantages:
1. Since each stage will process at a different speed, there is no fixed timescale for the end-to-end processing of a single packet.

1. This works only in the case of the Kafka event bus which is the default event bus, but not supported in the vertx event bus

1. Little complex to manage and debug since packet processing can get struck in multiple stages and the issue might not be explicitly visible since all the queues will be of varying sizes even in normal cases.

## End to end throttling through Re-processor
End to end throttling controls the number of packets that gets into the initial processing stage for a unit of time.

In this type of throttling, when the packet reaches the secure zone notification stage of MZ, we stop the packet from moving further by making the below property to false.

`securezone.routing.enabled=false`

Later, packets are reprocessed from the next stage by the re-processor component, the below properties control the frequency and number of packets that will be reprocessed.

`registration.processor.reprocess.fetchsize=100`
This config specifies the number of packets that will be reprocessed in every cycle of reprocessing

```
registration.processor.reprocess.seconds=0
registration.processor.reprocess.minutes=0,5,10,15,20,25,30,35,40,45,50,55
registration.processor.reprocess.hours=*
registration.processor.reprocess.days_of_month=*
registration.processor.reprocess.months=*
registration.processor.reprocess.days_of_week=*
```
The above configs specify the CRON frequency for reprocessing cycle

`registration.processor.reprocess.elapse.time=7200`
This config specifies the gap required in seconds between the last stage process time and current time to allow reprocessing

Advantages:
1. Give more predictability in the packet processing speed, has a fixed timescale for a packet to pass through all the stages from reprocessing time to complete processing

1. This works in both Kafka and vertx based event bus, Though vertx is not the recommended event bus for a production setup.

Disadvantages
1. Waits for the reprocessing elapse time to begin processing of every packet

1. If one stage is slow, the other stages will also process fewer packets even if their capacity is high

1. If a stage goes down, we have to manually stop the re-processor, fix the issue and allow the packets in the queue to complete and then start the re-processor again.

Note:  An automatic back pressure will be available in the LTS. This will avoid the reason to stop the service. 

## Conclusion
Stage wise throttling provides effective utilization of the hardware resources but lacks predictability in cases of one stage becoming slow or crashing down.

End to end throttling gives predictability on the number of packets that will be processed within a fixed period of time but does not use utilize the hardware resources well.

When the infrastructure is fixed, it is better to opt for end-to-end throttling, but when the infrastructure has the elasticity to auto-scale up or scale down, we can use stage-wise throttling to scale the stage that is slowing down and scale the common services that are used by the stage to keep up with the packet incoming speed.
