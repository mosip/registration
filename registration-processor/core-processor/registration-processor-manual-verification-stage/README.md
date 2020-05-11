# registration-processor-manual-verification-stage

This API is used to assign one single unassigned applicant record to the manual adjudicator.

## Design

[Design - Approach for Manual Verification Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_manual_adjudication.md)


## Default Port and Context Path
  
  * server.port=8084
  * eventbus.port=5720
  * server.servlet.path=/registrationprocessor/v1/manualverification

## URL

 * https://{dns-name}:8084/registrationprocessor/v1/manualverification/swagger-ui.html


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel | kernel-masterdata-service | /users |
| commons/kernel | kernel-signature-service | /sign |
