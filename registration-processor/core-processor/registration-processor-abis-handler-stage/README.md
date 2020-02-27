### registration-processor-abis-handler-stage

## Design
[Design](https://github.com/mosip/mosip/wiki/Registration-Processor)

This stage takes the count of abis devices and creates that many insert and identify request, saving them in the AbisRequest table for abis middleware to use.

## Default Context Path and Port
```
server.port=9071
eventbus.port=5726
```
## Configurable Properties from Config Server
```
registration.processor.biometric.reference.url=${mosip.base.url}/registrationprocessor/v1/bio-dedupe/biometricfile
registration.processor.abis.maxResults=30
registration.processor.abis.targetFPIR=30
```
