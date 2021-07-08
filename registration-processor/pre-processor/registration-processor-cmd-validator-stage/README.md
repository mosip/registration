# registration-processor-cmd-validator-stage

This component validates the User, Machine, Centre details from the Packet

## Design

[Design - Approac for User, Machine, Center Validation](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_umc_validation.md)

## Default Context-path and Port
```
registration.processor.zone=secure
vertx.cluster.configuration=${spring.cloud.config.uri}/*/${spring.profiles.active}/${spring.cloud.config.label}/hazelcast_${registration.processor.zone}.xml
worker.pool.size=10

mosip.regproc.cmd-validator.server.port=8089
mosip.regproc.cmd-validator.eventbus.port=5716
mosip.regproc.cmd-validator.server.servlet.path=/registrationprocessor/v1/cmdvalidator
mosip.regproc.cmd-validator.message.expiry-time-limit=3600
```
## Configurable properties from Configuration Server
```
mosip.regproc.cmd-validator.working-hour-validation-required=true
mosip.identity.auth.internal.requestid=mosip.identity.auth.internal
mosip.registration.gps_device_enable_flag=true
mosip.primary-language=eng
mosip.kernel.device.validate.history.id=""
mosip.registration.processor.validate-machine=true
mosip.registration.processor.validate-device=true
mosip.registration.processor.validate-center=true

# To enable/disable trust validation of a digital id signature of a device
mosip.regproc.cmd-validator.device.disable-trust-validation=false
# Maximum duration in minutes permissible betwwen digital id timestamp and packet creation time
mosip.regproc.cmd-validator.device.allowed-digital-id-timestamp-variation=525600
# Timestamp format followed in digital id and biometrics payload
mosip.regproc.cmd-validator.device.digital-id-timestamp-format=yyyy-MM-dd'T'HH:mm:ss'Z'

```
## Validations done by the stage
1. User, Centre and Machine Validation :  Validation against the Master-data
2. Device Validation : Validation of Devices against the Master-data
3. GPS Validation : Verification whether Latitude and Longitude is present