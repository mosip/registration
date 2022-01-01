# Registration Processor Configuration Guide


* DB properties: Point to your postgres

## Custom subprocess (flow)
* registration.processor.sub-processes=BIOMETRIC_CORRECTION
If a subprocess by country (for eg document correction), then subprocess has to updated here.  Example:
  `registration.processor.sub-processes=BIOMETRIC_CORRECTION,DOCUMENT_CORRECTION`
Also add similar property for max retries:
mosip.regproc.workflow-manager.internal.action.max-allowed-iteration.BIOMETRIC_CORRECTION=5

Add here:
mosip.regproc.cmd-validator.center-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION
# Processes to enable machine validation, for processes not mentioned here machine validation will be skipped
mosip.regproc.cmd-validator.machine-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION
# Processes to enable device validation, for processes not mentioned here device validation will be skipped
mosip.regproc.cmd-validator.device-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION



* registration.processor.max.retry=10
Max number of retries allowed by registration client if sanity check of packet fails in [packet uploader stage]()
  
* mosip.regproc.virusscanner.provider=io.mosip.kernel.virusscanner.clamav.impl.VirusScannerImpl
Virus Scanner JAR is picked up on runtime.  If another scanner is used, the implementation class needs to be provided here.
Point to AV documentation https://github.com/mosip/mosip-infra/blob/1.2.0-rc2/deployment/sandbox-v2/docs/av.md
https://github.com/mosip/mosip-ref-impl/tree/1.2.0-rc2/kernel/kernel-virusscanner-clamav


## Notification settings

The templates need to be changed to according to country's messaging design.

mosip.regproc.notification_service.biometric_correction.email=RPR_PAUSED_FOR_ADD_INFO_EMAIL
# SMS template code for PauseForAdditionalInfo
mosip.regproc.notification_service.biometric_correction.sms=RPR_PAUSED_FOR_ADD_INFO_SMS
# SUBJECT template code for PauseForAdditionalInfo
mosip.regproc.notification_service.biometric_correction.subject=
registration.processor.uin.generated.subject=UIN Generated
registration.processor.duplicate.uin.subject=Registration Failed because you have already Registered
registration.processor.reregister.subject=Re-Register because there was a Technical Issue
registration.processor.uin.activated.subject=Uin is activated successfully
registration.processor.uin.deactivated.subject=Uin is deactivated
registration.processor.updated.subject=UIN Details Updated

Review all templates given here:
regproc.notification.template.*


Disabling this flag is sometimes useful in development mode.  NEVER disable this in production.
registration.processor.signature.isEnabled=true


## Reprocessor configuration


## Workflow manager configuration
mosip.regproc.workflow-manager.action.job.minutes=0,5,10,15,20,25,30,35,40,45,50,55


# Enable this flag only if infant biometrics are captured, and dedup is desired.
registration.processor.infant.dedupe=N

# ABIS

Specify the partner id of ABIS (as in `partner` table in `mosip_pms` DB).
registration.processor.subscriber.id=mpartner-default-abis

Specify the policy id (as in `auth_policy` table in `mosip_pms` DB) of the ABIS.
registration.processor.policy.id=mpolicy-default-abis

# Biosdk
To point to your biosdk service:
mosip.biosdk.default.host=${mosip.mock-biosdk-service.url}

Scores range based on biometric sdk scoring.
mosip.regproc.quality.classifier.tagging.quality.ranges={'Poor':'0-29','Average':'30-69','Good':'70-100'}

## Workflow rules:

Rules for pausing processing of a packet.
mosip.regproc.camelbridge.pause-settings=[{"ruleId" :"HOTLISTED_OPERATOR","matchExpression": "$.tags[?(@['HOTLISTED'] == 'operator')]","pauseFor": 432000,"defaultResumeAction": "STOP_PROCESSING","fromAddress": ".*","ruleDescription" : "Packet created by hotlisted operator"}]

## Template conversion
biometric.extraction.default.partner.policy.ids=[{'partnerId':'mpartner-default-auth','policyId':'mpolicy-default-auth'},{'partnerId':'mpartner-default-print','policyId':'mpolicy-default-print'},{'partnerId':'mpartner-default-print','policyId':'mpolicy-default-qrcode'},{'partnerId':'mpartner-default-print','policyId':'mpolicy-default-euin'}]

## Digital ID
mosip.regproc.cmd-validator.device.digital-id-timestamp-format=yyyy-MM-dd'T'HH:mm:ss'Z'

## Age group
mosip.regproc.packet.classifier.tagging.agegroup.ranges={'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}

