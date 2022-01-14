# Registration flows and stage sequence

Sections here list the sequence in which various stages/services are called for a particular enrollment flow.

## New/Child/Update/Lost
* [Packet receiver](../registration-processor/init/registration-processor-packet-receiver-stage)
* [Securezone notification](../registration-processor/pre-processor/registration-processor-securezone-notification-stage)
* [Packet uploader](../registration-processor/pre-processor/registration-processor-packet-uploader-stage)
* [Packet validator](../registration-processor/pre-processor/registration-processor-packet-validator-stage)
* [Packet classifier](../registration-processor/pre-processor/registration-processor-packet-classifier-stage)
* [CMD validator](../registration-processor/pre-processor/registration-processor-cmd-validator-stage)
* [Operator validator](../registration-processor/pre-processor/registration-processor-operator-validator-stage)
* [Supervisor validator](../registration-processor/pre-processor/registration-processor-supervisor-validator-stage)
* [Introducer validator](../registration-processor/pre-processor/registration-processor-introducer-validator-stage)
* [Quality classifier](../registration-processor/pre-processor/registration-processor-quality-classifier-stage)
* [Biometric authentication](../registration-processor/core-processor/registration-processor-biometric-authentication-stage)
* [Demo dedupe](../registration-processor/core-processor/registration-processor-demo-dedupe-stage)
* [Bio dedupe](../registration-processor/core-processor/registration-processor-bio-dedupe-stage)
* Verification
* [ABIS handler](../registration-processor/core-processor/registration-processor-abis-handler-stage)
* ABIS middleware
* [ABIS middleware ](../registration-processor/core-processor/registration-processor-abis-middleware-stage)
* [Manual adjudication](../registration-processor/core-processor/registration-processor-manual-adjudication-stage)
* UIN generator
* Biometric extraction
* Finalization 
* Printing

The flows are specified in [Camel XMLs](https://github.com/mosip/mosip-config/tree/develop2-v2)

## Correction
The flow depends on the type of correction.  For biometric correction example is given [here](https://github.com/mosip/mosip-config/blob/develop2-v2/registration-processor-camel-routes-biometric-correction-default.xml)

## Activate/Deactivate
* [Packet receiver](../registration-processor/init/registration-processor-packet-receiver-stage)
* [Securezone notification](../registration-processor/pre-processor/registration-processor-securezone-notification-stage)
* [Packet uploader](../registration-processor/pre-processor/registration-processor-packet-uploader-stage)
* UIN generator
* Biometric extraction
* Finalization 

## Reprint
* [Packet receiver](../registration-processor/init/registration-processor-packet-receiver-stage)
* [Securezone notification](../registration-processor/pre-processor/registration-processor-securezone-notification-stage)
* [Packet uploader](../registration-processor/pre-processor/registration-processor-packet-uploader-stage)
* Printing

