# registration-processor-cmd-validator-stage

This component validates the User, Machine, Centre details from the Packet

## Design

[Design - Approac for User, Machine, Center Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_umc_validation.md)

## Default Context-path and Port
```
server.port=8089
eventbus.port=5716
server.servlet.path=/registrationprocessor/v1/cmdvalidator
```
## Configurable properties from Configuration Server
```
mosip.regproc.cmd-validator.working-hour-validation-required=true
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
mosip.registration.processor.validate-machine=true
mosip.registration.processor.validate-device=true
mosip.registration.processor.validate-center=true
```
## Validations done by the stage
1. User, Centre and Machine Validation :  Validation against the Master-data
2. Device Validation : Validation of Devices against the Master-data
3. GPS Validation : Verification whether Latitude and Longitude is present