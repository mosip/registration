# registration-processor-operator-validator-stage

This component validates the Operator details from the Packet

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

mosip.regproc.operator-validator.server.port=8093
mosip.regproc.operator-validator.eventbus.port=5723
mosip.regproc.operator-validator.server.servlet.path=/registrationprocessor/v1/operatorvalidator
mosip.regproc.operator-validator.message.expiry-time-limit=3600
```
## Configurable properties from Configuration Server
```
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
```
## Validations done by the stage
1. Operator Authentication : Operator Authentication using User ID & Biometrics.