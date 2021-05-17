# registration-processor-supervisor-validator-stage

This component validates the Supervisor details from the Packet

## Design

[Design - Approach for OSI Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)

## Default Context-path and Port
```
server.port=8094
eventbus.port=5725
server.servlet.path=/registrationprocessor/v1/supervisorvalidator
```
## Configurable properties from Configuration Server
```
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. Supervisor Authentication : Supervisor Authentication using User ID & Biometrics.