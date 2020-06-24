# Approach for paket receiver

**Background**
Registration packets created by the registration clients will be periodically uploaded to the server for processing. The packets will be stored in landing zone file system for further processing and packet status should be created in registration status table.

The target users are
-	Server application which will process the packets
-	Client application, which will consume the service and send packets to server.
-	System integrators for a country.

The key requirements are
-	Rest API to upload packets to server.
-	Basic sanity check of packet and send response to client.
-	Process the packet asynchronously to do virus scan.
-	Store the packet to LANDING_ZONE upon successful scan of the encrypted packet.

The key non-functional requirements are
-	Availability: The application should be highly available to support both online upload and upload through admin portal.
-	Modularity: The application should get the packet from client and call packet-manager module to upload packet in landing zone.
-	Performance: Should support uploading packets from multiple clients at same time.


**Solution**
The key solution considerations are
-	A new stage in DMZ zone - "registration-processor-packet-receiver-stage".
-	The stage to expose rest API to accept multipart file. The registration-client will call this rest api and pass registration pcaket in‘zip’format. 
- Upon receiving packet it will do below validations -
		- Validate if the registration id is already present in sync table. Fail the validation if packet was not synced before uploading to server.
		- Validate the checksum of the packet and match with the checksum recieved during sync.
		- Validate the size of the packet with packet size received during sync.
		- Check if the packet was already received which is not marked for resend in server. If packet is already received and not marked for resend then API should not allow to upload the packet again. Return error.
- A successful response will be sent to registration-client if all the above validations are passed. Packet will be passed to next handler which will asynchronously process the packet.
- This handler is responsible for below functionalities -
		- Use kernel-virusscanner to scan all the encrypted source packets. It will be processed further only if virus scan is successful.
		- After successful scan use registration-processor-packet-manager to store the packet in LANDING_ZONE location. Call io.mosip.registration.processor.core.spi.filesystem.manager.FileManager put(String fileName, F file, D workingDirectory) method.
-	Update registration status and registration transaction tables for successful or failed transaction.

Below components are required for this stage -
-	registration-processor-core
- registration-processor-packet-manager
- registration-processor-registration-status-service-impl
- kernel-virusscanner


**Class Diagram**
![Packet receiver class diagram](_images/packet_receiver_class_diagram.png)

**Sequence Diagram**
![Packet receiver sequence diagram](_images/packet_receiver_seq_diagram.png)
