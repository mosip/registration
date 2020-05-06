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
| commons/kernel | kernel-signature-service | /signature/sign|
| commons/kernel | kernel-ridgenerator-service | /ridgenerator/generate/rid |
| commons/kernel | kernel-masterdata-service | /maserdata/registrationcenters |
|  |  | /maserdata/machines |
| commons/kernel | kernel-masterdata-service | /maserdata/registrationcenters |
| commons/kernel | kernel-keymanager-service | /keymanager/publickey |
| id-repository | id-repository-identity-service | /idrepository/v1/identity|
| id-repository | id-repository-vid-service | /idrepository/v1/vid|
