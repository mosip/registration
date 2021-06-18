# registration-processor-quality-checker-stage

This stage validates the quality scores of the applicant's biometric.

## Design

[Design - Approcach for quality Stage - TBA](https://github.com/mosip/registration/tree/master/design/registration-processor)

## Default Context Path and Port
```
server.port=9072
eventbus.port=5727
```
## Configurable Properties from Config Server
```
mosip.registration.iris_threshold=70
mosip.registration.leftslap_fingerprint_threshold=80
mosip.registration.rightslap_fingerprint_threshold=80
mosip.registration.thumbs_fingerprint_threshold=80
mosip.registration.facequalitythreshold=25

## Quality Tag Prefix
mosip.regproc.quality.classifier.tagging.quality.prefix=Biometric_Quality-
## The tag value that will be used by default when the packet does not have biometrics
mosip.regproc.quality.classifier.tagging.quality.biometric-not-available-tag-value=--Biometrics-Not-Available--
## modality arrays that needs to be tagged
mosip.regproc.quality.classifier.tagging.quality.modalities=Iris,Finger,Face
## Below quality ranges map should contain proper quality group name and quality range, any overlap of the quality 
## range will result in a random behaviour of tagging. In range, upper and lower values are inclusive.
mosip.regproc.quality.classifier.tagging.quality.ranges={'Poor':'0-29','Average':'30-69','Good':'70-100'}
```
## Validations in Quality Checker Stage
1. Validation of all the quality values of biometric types of applicant cbeff with the values from config server. Passing the stage if 
all qualities are greater than or equal to threshold quality values mentioned in config server for each biometric types.

