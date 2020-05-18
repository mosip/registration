# registration-processor-request-handler-service

The residence service portal would call this api to reprint uin card upon receiving request from the applicant.

## Default Port and Context Path
  
  * server.port=8082
  * eventbus.port=5713
  * server.servlet.path=/registrationprocessor/v1/requesthandler


## URL

 * https://{dns-name}:8082/registrationprocessor/v1/requesthandler/swagger-ui.html


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel | kernel-signature-service | /sign|
| commons/kernel | kernel-ridgenerator-service | /generate/rid/{centerid}/{machineid} |
| commons/kernel | kernel-masterdata-service | /registrationcenters |
|  |  | /machines |
| commons/kernel | kernel-keymanager-service | /publickey/{applicationId} |
| commons/id-repository | id-repository-identity-service | /uin/{uin} |
| commons/id-repository | id-repository-vid-service | /vid|
