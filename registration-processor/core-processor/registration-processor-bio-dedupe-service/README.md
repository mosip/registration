# registration-processor-bio-dedupe-service

This service provides applicant biometric CBEFF file for a ABIS Reference ID. Based on ABIS Reference ID it will fetch Registration ID from Database.

## Design

[Design - Approach for Bio Dedupe](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_bio_dedupe.md)

## Default Context Path and Port
```
server.port=9097
server.servlet.path=/registrationprocessor/v1/bio-dedupe
```
## Configurable Properties from Config Server
```
registration.processor.signature.isEnabled=true
```
