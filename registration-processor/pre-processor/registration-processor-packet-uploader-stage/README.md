# registration-processor-packet-uploader-stage

This stage accepts request via Rest-Api, connects to DMZ, gets in the required file and uploads the file to Distributed File System after performing certain sanity checks on the file. The dmz camel-bridge calls packet uploader API to upload the packet in file system(Hdfs, ceph etc). This service is a bridge between dmz and secure network. It accepts json request and connects to dmz VM to get the packet and move it to archive location.

## Design

[Design - Approach for Packet Uploader Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_packet_uploader.md)


## Default Port and Context Path
  
  * server.port=8087
  * eventbus.port=5714
  * server.servlet.path=/registrationprocessor/v1/uploader


## URL

 * https://{dns-name}:8087/registrationprocessor/v1/uploader/swagger-ui.html

## Sample Request Structure

```
{
  "rid": "10002100320001320190530131610",
  "isValid": true,
  "internalError": false,
  "messageBusAddress":null,
  "reg_type":"UPDATE",
  "retryCount": null
}
```

## Checks performed by this stage

1. In-memory virus scan of the encrypted packet
2. In-memory virus scan of the decrypted packet
3. Hash-sequence validation of the packet
4. Size validation of the packet based on Meta-data received during sync and the actual size of the packet.


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-cryptomanager-service |/decrypt|
| commons/kernel  | kernel-auditmanager-service | /audits|
