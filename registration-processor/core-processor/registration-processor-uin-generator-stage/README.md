# registration-processor-uin-generator-stage

This stage is to generate uin and mapping the generated uin to the applicant registration Id and store the applicant details in ID Repository.

## Design

[Design - Approach for UIN Generator Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_uin_generator.md)

## Default Context Path and Port
```
server.port=8099
eventbus.port=5719
```
## Configurable Properties from Config Server
```
IDREPOSITORY=${mosip.base.url}/idrepository/v1/identity/
IDREPOGETIDBYUIN=${mosip.base.url}/idrepository/v1/identity/uin
UINGENERATOR=${mosip.base.url}/v1/uingenerator/uin
RETRIEVEIDENTITYFROMRID=${mosip.base.url}/idrepository/v1/identity/rid
RETRIEVEIDENTITY=${mosip.base.url}/idrepository/v1/identity/uin
CREATEVID=${mosip.base.url}/idrepository/v1/vid
registration.processor.id.repo.create=mosip.id.create
registration.processor.id.repo.read=mosip.id.read
registration.processor.id.repo.update=mosip.id.update
registration.processor.id.repo.vidType=Perpetual
registration.processor.id.repo.generate=mosip.vid.create
registration.processor.id.repo.vidVersion=v1
```

