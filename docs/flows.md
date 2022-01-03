# Registration flows and stage sequence

## New/Child/Update/Lost
* Packet receiver
* Securezone notification
* Packet uploader
* Packet validator 
* Packet classifier
* CMD validator 
* Operator validator
* Supervisor validator
* Introducer validator
* Quality classifier
* Biometric authentication (only for Update)
* Demo-dedupe
* Bio-dedupe
* Verification
* ABIS handler
* ABIS middleware
* Manual adjudication 
* UIN generator
* Biometric extraction
* Finalization 
* Printing

The flows are specified in [Camel XMLs](https://github.com/mosip/mosip-config/tree/develop2-v2)

## Correction

The flow depends on the type of correction.  For biometric correction example is given [here](https://github.com/mosip/mosip-config/blob/develop2-v2/registration-processor-camel-routes-biometric-correction-default.xml)

## Activate/Deactivate
* Packet receiver
* Securezone notification
* Packet uploader
* UIN generator
* Biometric extraction
* Finalization 

## Reprint
* Packet receiver
* Securezone notification
* Packet uploader
* Printing

