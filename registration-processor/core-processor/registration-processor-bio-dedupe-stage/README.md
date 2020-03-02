# registration-processor-bio-dedupe-stage

This stage processes any request that comes to it based on Registration Type and the biometric uniqueness will be verified through ABIS and appropriate DB statuses will be updated.

## Design

[Design - Approach for Bio Dedupe](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_bio_dedupe.md)

## Default Context Path and Port
```
server.port=9096
eventbus.port=5718
server.servlet.path=/registrationprocessor/v1/biodedupe
```

