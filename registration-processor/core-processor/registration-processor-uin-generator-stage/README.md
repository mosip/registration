# registration-processor-uin-generator-stage

This stage is to generate uin and mapping the generated uin to the applicant registration Id and store the applicant details in ID Repository. UIN Generator will be called to allocate an unique identification number to the applicant.

## Design

[Design - Approach for UIN Generator Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_uin_generator.md)


## Default Port and Context Path
  
  * server.port=8099
  * server.servlet.path=/registrationprocessor/v1/uin-generator


## URL

 * https://{dns-name}:8099/registrationprocessor/v1/uin-generator/swagger-ui.html


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-uingenerator-service | /uingenerator/uin|
| commons/kernel  | kernel-auditmanager-service | /auditmanager/audits|
| id-repository   | id-repository-identity-service | /idrepository/v1/identity/|
|   |  | /idrepository/v1/identity/uin|
| id-repository   | id-repository-vid-service | /idrepository/v1/vid|
