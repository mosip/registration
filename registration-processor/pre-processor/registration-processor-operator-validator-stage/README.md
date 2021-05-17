# registration-processor-operator-validator-stage

This component validates the Operator details from the Packet

## Design

[Design - Approach for OSI Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)

## Default Context-path and Port
```
server.port=8093
eventbus.port=5723
server.servlet.path=/registrationprocessor/v1/operatorvalidator
```
## Configurable properties from Configuration Server
```
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. Operator Authentication : Operator Authentication using User ID & Biometrics.