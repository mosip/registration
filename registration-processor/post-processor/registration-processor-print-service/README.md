# registration-processor-print-service

This stage provides downloadable pdf for a uin or rid.

## Design

[Design - Approach for Re-printing](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_Reprinting.md)


## Default Port and Context Path
  
  * server.port=9099
  * server.servlet.path=/registrationprocessor/v1/print


## URL

 * https://{dns-name}:9099/registrationprocessor/v1/print/swagger-ui.html


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-signature-service | /sign|
| commons/id-repository | id-repository-identity-service | /uin/{uin} |
|  |  | /rid/{rid}|
| commons/id-repository | id-repository-vid-service | /vid|
