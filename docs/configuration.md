# Registration Processor Configuration Guide
## Overview
The guide here lists down some of the important properties that may be customised for a given installation. Note that the listing here is not exhaustive, but a checklist to review properties that are likely to be different from default.  If you would like to see all the properites, then refer to the files listed below.

## Configuration files
Regprocessor uses the following configuration files:
```
application-default.properties
application-default-dmz.properties
registration-processor-default.properties
registration-processor-abis.json
registration-processor-camel-routes-activate-default-dmz.xml
registration-processor-camel-routes-activate-default.xml
registration-processor-camel-routes-biometric-correction-default-dmz.xml
registration-processor-camel-routes-biometric-correction-default.xml
registration-processor-camel-routes-deactivate-default-dmz.xml
registration-processor-camel-routes-deactivate-default.xml
registration-processor-camel-routes-lost-default-dmz.xml
registration-processor-camel-routes-lost-default.xml
registration-processor-camel-routes-new-default-dmz.xml
registration-processor-camel-routes-new-default.xml
registration-processor-camel-routes-res-reprint-default-dmz.xml
registration-processor-camel-routes-res-reprint-default.xml
registration-processor-camel-routes-res-update-default-dmz.xml
registration-processor-camel-routes-res-update-default.xml
registration-processor-camel-routes-update-default-dmz.xml
registration-processor-camel-routes-update-default.xml
registration-processor-default-dmz.properties
registration-processor-print-text-file.json
identity-mapping.json
```

See [Module Configuration](https://docs.mosip.io/1.2.0/modules/module-configuration) for location of these files.

## DB
* `mosip.registration.processor.database.hostname`
* `mosip.registration.processor.database.port`

Point the above to your DB and port.  Default is set to point to in-cluster Postgres installed with sandbox.

## Custom subprocess (flow)
* `registration.processor.sub-processes`
If a subprocess is added by a country (for e.g. document correction), then the same has to updated here.  Example:
  `registration.processor.sub-processes=BIOMETRIC_CORRECTION,DOCUMENT_CORRECTION`. Update the following properties as well (example):
* `mosip.regproc.workflow-manager.internal.action.max-allowed-iteration.DOCUMENT_CORRECTION=5` 
* `mosip.regproc.cmd-validator.center-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,DOCUMENT_CORRECTION`
* `mosip.regproc.cmd-validator.machine-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,DOCUMENT_CORRECTION`
* `mosip.regproc.cmd-validator.device-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,DOCUMENT_CORRECTION`

## Notification settings
Following templates need to be changed according to country's messaging design.
```
mosip.regproc.notification_service.biometric_correction.email
mosip.regproc.notification_service.biometric_correction.sms
mosip.regproc.notification_service.biometric_correction.subject
registration.processor.uin.generated.subject
registration.processor.duplicate.uin.subject
registration.processor.reregister.subject
registration.processor.uin.activated.subject
registration.processor.uin.deactivated.subject
registration.processor.updated.subject
regproc.notification.template.*
```

## Reprocessor configuration
TBD.

## Workflow manager configuration
`mosip.regproc.workflow-manager.action.job.minutes`: Frequency of job in development mode would typically higher than in production 

## ABIS
* `registration.processor.subscriber.id`: Specify the partner id of ABIS (as in `partner` table in `mosip_pms` DB).
* `registration.processor.policy.id`: Specify the policy id (as in `auth_policy` table in `mosip_pms` DB) of the ABIS.

## Biosdk
* `mosip.biosdk.default.host`: Point to your biosdk service
* `mosip.regproc.quality.classifier.tagging.quality.ranges`: Scores range based on biometric sdk scoring.

## Workflow rules
* `mosip.regproc.camelbridge.pause-settings`: Rules for pausing processing of a packet.

## Template conversion
* `biometric.extraction.default.partner.policy.ids`

## Digital ID
* `mosip.regproc.cmd-validator.device.digital-id-timestamp-format`

## Age group
* `mosip.regproc.packet.classifier.tagging.agegroup.ranges`

## Misc properties 
* `registration.processor.max.retry`: Max number of retries allowed by registration client if sanity check of packet fails in [packet uploader stage](registration-processor/pre-processor/registration-processor-packet-uploader-stage)
* `mosip.regproc.virusscanner.provider`: Virus Scanner JAR is picked up on runtime.  If another scanner is used, the implementation class needs to be provided here. See [Integrating Antivirus](https://github.com/mosip/mosip-ref-impl/tree/v1.2.0/kernel/kernel-virusscanner-clamav/docs/av.md)
* `registration.processor.signature.isEnabled`: Disabling this flag is sometimes useful in development mode.  NEVER disable this in production.
*  `registration.processor.infant.dedupe`: Enable this flag only if infant biometrics are captured, and dedup is desired.
