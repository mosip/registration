# registration-processor-securezone-notification-stage

This stage receives notification from DMZ zone and routes it to the next stages of secure zone.

## Design
Not Applicable(This is a routing stage with no business logic. Hence no design is available.)

## Default Context Path and Port
```
eventbus.port=5712
server.port=8090
server.servlet.path=/registrationprocessor/v1/securezone
```
## Configurable Properties from Config Server
```
# Below configuration is set in camel dmz xmls -
<to uri="${mosip.base.url}/registrationprocessor/v1/securezone/notification" />
```
## Operations in Securezone Notification Stage
registration-processor-securezone-notification-stage acts as a connector between "DMZ zone" and "secure zone". This stage publishes a rest endpoint which is being called from dmz camel bridge to transfer the message to secure zone for further processing. The stage will route the message to next stage(as configured in camel xml) upon receiving, for further processing. This stage does not have any business logic related to packet processing.