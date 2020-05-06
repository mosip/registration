# registration-processor-registration-status-service

Registration packets created by the registration clients will be periodically uploaded to the server for processing. The packets will be further processed and in each step status will be updated in registration status table. This component enables syncing of packet(s) and getting status of packet(s) via REST Api.

## Design

[Design](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_registration_status_module.md)


## Default Port and Context Path
  
  * server.port=8083
  * server.servlet.path=/registrationprocessor/v1/registrationstatus


## URL

 * https://{dns-name}:8099/registrationprocessor/v1/registrationstatus/swagger-ui.html
 

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel | kernel-cryptomanager-service | /cryptomanager/decrypt|
| commons/kernel  | kernel-signature-service | /signature/sign|
