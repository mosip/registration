# registration-processor-osi-validator-stage

This component validates the Operator, Supervisor, Introducer and User, Machine, Centre details from the Packet

## Design

[Design - Approach for OSI Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)

[Design - Approac for User, Machine, Center Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_umc_validation.md)

## Default Context-path and Port
```
server.port=8089
eventbus.port=5716
server.servlet.path=/registrationprocessor/v1/osivalidator
```
## Configurable properties from Configuration Server
```
mosip.workinghour.validation.required=true
registration.processor.applicant.dob.format=yyyy/MM/dd
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. User, Centre and Machine Validation :  Validation against the Master-data
2. Device Validation : Validation of Devices against the Master-data
3. GPS Validation : Verification whether Latitude and Longitude is present
5. Operator, Supervisor and Introducer Authentication : Operator/Supervisor Authentication using User ID & Biometrics and Introducer Authentication for Child Packets using UIN & Biometrics.