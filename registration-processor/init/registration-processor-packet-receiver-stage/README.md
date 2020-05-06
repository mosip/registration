# registration-processor-packet-receiver-stage

This API receives registration packet from reg-client. Before moving packet to landing zone virus scan is performed and then trustworthiness of the packet is validated using hash value and size.


## Design

[Design](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_packet_receiver_stage.md)

 
## Default Port and Context Path

  * server.port=8081
  * server.servlet.path=/registrationprocessor/v1/packetreceiver

## URL

* https://{dns-name}:8081/registrationprocessor/v1/packetreceiver/swagger-ui.html

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-cryptomanager-service | /cryptomanager/decrypt|
| commons/kernel  | kernel-auditmanager-service | /auditmanager/audits|

