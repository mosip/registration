# registration-processor-abis-handler-stage

This stage takes the count of abis and creates that many insert and identify request, saving them in the AbisRequest table for abis middleware to use.

## Design
[Design - Approach for ABIS Integration](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_ABIS_Integration.md)

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
