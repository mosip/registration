# registration-processor-packet-uploader-stage

This stage accepts request via Rest-Api, connects to DMZ, gets in the required file and uploads the file to Distributed File System after performing certain sanity checks on the file.

## Design

[Design - Approach for Packet Uploader Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_packet_uploader.md)

## Default Context-path and Port
```
server.port=8087
server.servlet.path=/registrationprocessor/v1/uploader
eventbus.port=5714
```
## Properties from Configuration Server
```
registration.processor.dmz.server.host=104.211.213.46
registration.processor.dmz.server.user=madmin
registration.processor.dmz.server.port=22
registration.processor.dmz.server.protocal=sftp
```
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