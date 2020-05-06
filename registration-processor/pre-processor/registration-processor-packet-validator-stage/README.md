# registration-processor-packet-validator-stage

This stage validates the essentials of a packet before sending the packet for further processing. This API receives registration packet from reg-client. Before moving packet to landing zone virus scan is performed and then trustworthiness of the packet is validated using hash value and size.

## Design

[Design - Approach for Packet Uploader Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_packet_validator.md)


## Default Port and Context Path
  
  * server.port=8088
  * eventbus.port=5715
  * server.servlet.path=/registrationprocessor/v1/packetvalidator


## URL

 * https://{dns-name}:8088/registrationprocessor/v1/packetvalidator/swagger-ui.html
 
## Validations in Packet Validator

1. Validation of ID Schema : ID Json Validation
2. Validation of Master Data : Based on the key 'registration.processor.validateMasterData' in configuration, the values present in 'registration.processor.masterdata.validation.attributes' are validated against the Master data.
3. Validation of Files : Checking of all files present in hashsequence of packet_meta_info to be actually present inside the packet.
4. Internal Checksum Validation : Matching of checksum received by client with the checksum calculated inside registration-processor.
5. Document Validation : Validation of Documents present in packet_meta_info in correspondance to the value of field 'registration.processor.document.category'.

Note: All these validations can be turned on/off by changing appropriate keys in config server as true/false.

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-masterdata-service | /maserdata/gendertypes/validate|
|   |  | /maserdata/locations/validate|
| commons/kernel  | kernel-auditmanager-service | /auditmanager/audits|
| id-repository  | id-repository-identity-service | /idrepository/v1/identity/|
| pre-registration  | pre-registration-datasync-service | /preregistration/v1/sync/consumedPreRegIds|
