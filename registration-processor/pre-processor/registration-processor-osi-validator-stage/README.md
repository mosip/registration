# registration-processor-osi-validator-stage

This component validates the Operator, Supervisor, Introducer and User, Machine, Centre details from the Packet. After successful user/machine/center validation, the packet packet meta info is stored in DB. The operator, supervisor and introducer biometric/password/pin will be further validated to check if the packet is created by authorized person.

## Design

[Design](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)


## Default Port and Context Path
  
  * server.port=8089
  * eventbus.port=5716
  * server.servlet.path=/registrationprocessor/v1/osivalidator


## URL

 * https://{dns-name}:8089/registrationprocessor/v1/osivalidator/swagger-ui.html
 
## Validations done by the stage

1. User, Centre and Machine Validation :  Validation against the Master-data
2. Device Validation : Validation of Devices against the Master-data
3. GPS Validation : Verification whether Latitude and Longitude is present
5. Operator, Supervisor and Introducer Authentication : Operator/Supervisor Authentication using User ID & Biometrics and Introducer Authentication for Child Packets using UIN & Biometrics.

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-masterdata-service | /users/{id}/{eff_dtimes}|
|   |  | /registrationcentershistory/{registrationCenterId}/{langcode}/{effectiveDate}|
|   |  | /machineshistories/{id}/{langcode}/{effdatetimes}|
|   |  | /deviceshistories/{id}/{langcode}/{effdatetimes}|
|   |  | /registrationcenterdevicehistory/{regcenterid}/{deviceid}/{effdatetimes}|
|   |  | /registrationcenters/validate/{id}/{langCode}/{timestamp}|
| commons/kernel  | kernel-auditmanager-service | /audits|
| id-authentication  | authentication-internal-service | /auth|
| commons/id-repository | id-repository-identity-service | /uin/{uin} |
