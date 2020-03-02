# registration-processor-biometric-authentication stage

This component validates update packets in case of adult registration.

## Design

[Desgin - Approach for Biometrics Authentication Stage - TBA](https://github.com/mosip/registration/tree/master/design/registration-processor)

## Default Port
```
server.port=8020
eventbus.port=5777
```
## Description of Validation
Checking whether the 'individualBiometrics' file is present, if not present, implies the packet to be a demographic update packet. We check 'authenticationBiometricFileName' and validate it against IDA.
