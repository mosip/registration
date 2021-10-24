# registration-processor-packet-classifier-stage

This stage will add tags to the packet based on the configuration. These tags can we used in camel routes and other places to quickly check and take necessary actions.

## Design

[Design - Approach for Packet Classifier Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_packet_classifier.md)

## Default Context Path and Port
```
mosip.regproc.packet.classifier.server.port=8092
mosip.regproc.packet.classifier.eventbus.port=5724
server.servlet.path=/registrationprocessor/v1/packetclassifier
```
## Configurable Properties from Config Server
```
# List of tag generator that should be run on every packet
# Available tag generators MosipIDObjectFields,MosipMetaInfo,MosipAgeGroup,MosipSupervisorApprovalStatus,MosipExceptionBiometrics // DocumentMissing,MosipBiometricsMissing
mosip.regproc.packet.classifier.tag-generators=MosipIDObjectFields,MosipMetaInfo,MosipAgeGroup,MosipSupervisorApprovalStatus,MosipExceptionBiometrics
# The tag value that will be used by default when the packet does not have value for the tag field
mosip.regproc.packet.classifier.tagging.not-available-tag-value=--TAG_VALUE_NOT_AVAILABLE--
# These field names should be as in keys of registraion-processor-identity.json file Identity segment
# and should have proper default source configured
mosip.regproc.packet.classifier.tagging.idobjectfields.mapping-field-names=gender,city,residenceStatus
# The tag name that will be prefixed with every idobjectfield tags
mosip.regproc.packet.classifier.tagging.idobjectfields.tag-name-prefix=ID_OBJECT-
# The language that should be used when dealing with field type that has values in multiple languages
# This is an existing configuration that is reused
mosip.primary-language=eng
# The tag name that will be prefixed with every metainfo operationsData tags
mosip.regproc.packet.classifier.tagging.metainfo.operationsdata.tag-name-prefix=META_INFO-OPERATIONS_DATA-
# The tag name that will be prefixed with every metainfo metaData tags
mosip.regproc.packet.classifier.tagging.metainfo.metadata.tag-name-prefix=META_INFO-META_DATA-
# The tag name that will be prefixed with every metainfo capturedRegisteredDevices tags 
mosip.regproc.packet.classifier.tagging.metainfo.capturedregistereddevices.tag-name-prefix=META_INFO-CAPTURED_REGISTERED_DEVICES-
# The labels on metainfo.operationsData array that needs to be tagged
mosip.regproc.packet.classifier.tagging.metainfo.operationsdata.tag-labels=officerId,supervisorId
# The labels on metainfo.metaData array that needs to be tagged
mosip.regproc.packet.classifier.tagging.metainfo.metadata.tag-labels=centerId,machineId
# The serial numbers of devices type on metainfo.capturedRegisteredDevices array that needs to be tagged
mosip.regproc.packet.classifier.tagging.metainfo.capturedregistereddevices.device-types=Face,Fingerprint
# Tag name that will used while tagging age group
mosip.regproc.packet.classifier.tagging.agegroup.tag-name=AGE_GROUP
# Below age ranges map should contain proper age group name and age range, any overlap of the age 
# range will result in a random behaviour of tagging. In range, upper and lower values are inclusive.
mosip.regproc.packet.classifier.tagging.agegroup.ranges={'CHILD':'0-17','ADULT':'18-59','SENIOR_CITIZEN':'60-200'}
# Tag name that will used while tagging supervisor approval status
mosip.regproc.packet.classifier.tagging.supervisorapprovalstatus.tag-name=SUPERVISOR_APPROVAL_STATUS
# Tag name that will used while tagging exception biometrics
mosip.regproc.packet.classifier.tagging.exceptionbiometrics.tag-name=EXCEPTION_BIOMETRICS
# This mapping will contain the short words for each missing biometrics, the values will used for concatenating in the tags
mosip.regproc.packet.classifier.tagging.exceptionbiometrics.bio-value-mapping={'leftLittle':'LL','leftRing':'LR','leftMiddle':'LM','leftIndex':'LI','leftThumb':'LT','rightLittle':'RL','rightRing':'RR','rightMiddle':'RM','rightIndex':'RI','rightThumb':'RT','leftEye':'LE','rightEye':'RE'}
```
