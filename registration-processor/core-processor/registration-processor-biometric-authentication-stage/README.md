# registration-processor-biometric-authentication stage

This component validates update packets in case of adult registration.

## Design

[Desgin - Approach for Biometrics Authentication Stage - TBA](https://github.com/mosip/registration/tree/master/design/registration-processor)


## Default Port and Context Path
  
  * server.port=8020
  * eventbus.port=5777
  * server.servlet.path=/registrationprocessor/v1/bioauth
  
## URL

 * https://{dns-name}:8020/registrationprocessor/v1/bioauth/swagger-ui.html
 
## Description of Validation

Checking whether the 'individualBiometrics' file is present, if not present, implies the packet to be a demographic update packet. We check 'authenticationBiometricFileName' and validate it against IDA.

## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel | kernel-auditmanager-service | /auditmanager/auditsn |
| id-authentication | authentication-internal-service | /idauthentication/v1/internal/auth |
| id-repository | id-repository-identity-service | /idrepository/v1/identity/uin |
