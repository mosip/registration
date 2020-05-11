# registration-processor-bio-dedupe-service

This service provides applicant biometric CBEFF file for a ABIS Reference ID. Based on ABIS Reference ID it will fetch Registration ID from Database. The abis would call bio-dedupe callback API to get the biometric cbeff file

## Design

[Design - Approach for Bio Dedupe](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_bio_dedupe.md)

## Default Port and Context Path
  
  * server.port=9096
  * server.servlet.path=/registrationprocessor/v1/biodedupe


## URL

 * https://{dns-name}:9096/registrationprocessor/v1/biodedupe/swagger-ui.html


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-signature-service | /sign|
