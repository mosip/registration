# registration-processor-introducer-validator-stage

This component validates the Introducer details from the Packet

## Design

[Design - Approach for OSI Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)

## Default Context-path and Port
```
server.port=8095
eventbus.port=5728
server.servlet.path=/registrationprocessor/v1/introducervalidator
```
## Configurable properties from Configuration Server
```
registration.processor.applicant.dob.format=yyyy/MM/dd
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. Introducer Authentication : Introducer Authentication for Child Packets using UIN & Biometrics.