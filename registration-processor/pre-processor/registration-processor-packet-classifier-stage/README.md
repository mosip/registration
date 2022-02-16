# Packet Classifier Stage

## About
This stage is responsible to create tags and attaching them to evernts. These tags are utilized in the Camel routes to make intelligent routing decisions.  For instance, a Child packet does not need biometric deduplication check.
Certain packet info - as per the configuration -  is pulled out from the packet and attached to events.  An example of such tag based camel routing can be seen [here](../../../docs/example.correction.xml).

## Default context, path, port
Refert to [bootstrap properties](src/main/resources/bootstrap.properties)
