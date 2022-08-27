# Packet Validator Stage

## About
Performs the following functions:
* Calls Packetmanager's "validate" API to validate packet contents.
* Checks if UIN in UPDATE packet.
* Validates if required applicant's documents are present in the packet.
* Validates if biometric schema conforms to CBEFF XSD.  
* Notifies Pre-Registration that the application is consumed in server. 
* Notifies applicant about the status of packet -- whether invalid or under processing.
* Saves audit JSON in the packet to `mosip-audit` DB.

## Default context, path, port
Refert to [bootstrap properties](src/main/resources/bootstrap.properties)
