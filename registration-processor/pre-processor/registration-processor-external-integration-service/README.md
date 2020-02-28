# registration-processor-external-integration-service

## Design
[Design - Approach for External System Integration](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_external_system_integration.md)

[Design - Approach for Adding HTTP Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_http_integration.md)

## Default Context-path and Port
```
server.port=8201
server.servlet.path=/registrationprocessor/v1/eis
```

## Operations done by the Service
1. It returns boolean value true for every non null requests
