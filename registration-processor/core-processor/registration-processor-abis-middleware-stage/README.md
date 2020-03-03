# registration-processor-abis-middleware-stage

## Design

[Design - Approach for ABIS Integration](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_ABIS_Integration.md)

This stage gets insert/identify request created by ABIS Handler and sends them to ABIS Inbound Queues. It also consumes the response from ABIS outbound queues.

## Defualt context-path and Ports
```
server.port=8091
eventbus.port=5888
```
## Configurable properties from Configuration-server
```
registration.processor.abis.json=RegistrationProcessorAbis+{<profile>}.json
```
## Example of RegistrationProcessorAbis.json
```
{
	"abis": [{
			"name": "ABIS1",
			"host": "",
			"port": "",
			"brokerUrl": "tcp://104.211.200.46:61616",
			"inboundQueueName": "abis1-inbound-address_qa",
			"outboundQueueName": "abis1-outbound-address_qa",
			"pingInboundQueueName": "",
			"pingOutboundQueueName": "",
			"userName": "admin",
			"password": "admin",
		        "typeOfQueue": "ACTIVEMQ"
		},
		{
			"name": "ABIS2",
			"host": "",
			"port": "",
			"brokerUrl": "tcp://104.211.200.46:61616",
			"inboundQueueName": "abis2-inbound-address_qa",
			"outboundQueueName": "abis2-outbound-address_qa",
			"pingInboundQueueName": "",
			"pingOutboundQueueName": "",
			"userName": "admin",
			"password": "admin",
			"typeOfQueue": "ACTIVEMQ"
		},
		{
			"name": "ABIS3",
			"host": "",
			"port": "",
			"brokerUrl": "tcp://104.211.200.46:61616",
			"inboundQueueName": "abis3-inbound-address_qa",
			"outboundQueueName": "abis3-outbound-address_qa",
			"pingInboundQueueName": "",
			"pingOutboundQueueName": "",
			"userName": "admin",
			"password": "admin",
			"typeOfQueue": "ACTIVEMQ"
		}
	]

}
```
