# registration-processor-demo-dedupe-stage

This stages saves the Demographic data, i.e, Name, DOB and Gender. Post saving it performs Deduplication using exact one to one match of these parameters.

## Design

[Design - Approach for Demo-Dedupe Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_demo_dedupe.md)

## Default Port and Context Path
  
  * server.port=8091
  * eventbus.port=5717
  * server.servlet.path=/registrationprocessor/v1/demodedupe

## URL

 * https://{dns-name}:8091/registrationprocessor/v1/demodedupe/swagger-ui.html

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| id-repository  | id-repository-identity-service | /idrepository/v1/identity/uin|

## Important Note

The demo dedupe will be performed on exact match of name, date of birth and gender. If additional fields need to be included for demo dedupe match then code need to be modified. 
