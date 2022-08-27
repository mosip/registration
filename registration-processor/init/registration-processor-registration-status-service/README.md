# Registration Status Service

## About
Provides APIs to
* Query status of registration.
* Sync packet information (see below).

## Packet sync process
Before a registration packet is uploaded, the information of packet (packet hash code, size, applicant contact etc.) is first sent to the system using APIs of this service.  This information is matched against actual packet when it is uploaded. See [Packet Receiver](../registration-processor-packet-receiver-stage/)

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)

