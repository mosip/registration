# registration-processor-introducer-validator-stage

This component validates the Introducer details from the Packet

## Design

[Design - Approach for OSI Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_OSI_validation.md)

## Default Context-path and Port
```
spring.cloud.config.uri=localhost
spring.cloud.config.label=master
spring.profiles.active=mz
registration.processor.zone=secure
vertx.cluster.configuration=${spring.cloud.config.uri}/*/${spring.profiles.active}/${spring.cloud.config.label}/hazelcast_${registration.processor.zone}.xml
worker.pool.size=10

mosip.regproc.introducer-validator.server.port=8095
mosip.regproc.introducer-validator.eventbus.port=5728
mosip.regproc.introducer-validator.server.servlet.path=/registrationprocessor/v1/introducervalidator
mosip.regproc.introducer-validator.message.expiry-time-limit=3600
```
## Configurable properties from Configuration Server
```
registration.processor.applicant.dob.format=yyyy/MM/dd
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. Introducer Authentication : Introducer Authentication for Child Packets using UIN & Biometrics.