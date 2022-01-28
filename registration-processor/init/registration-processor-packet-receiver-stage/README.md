# Packet Receiver Stage

## About
Packet receiver stage performs the following functions:
*  Receives registration packets uploaded by registration clients.
*  Performs sanity checks (like virus scan, checksum validation, file size) on the encrypted packet (without decrypting).
*  Stores packet in landing zone.

## Landing zone
Landing zone is a data store (disk, distributed storage etc) where uploaded packets from registration clients are stored. The uploaded packets maybe archived after processing. In standard sandbox installation the landing zone is mounted inside Packet Receiver container as given in the [persistence] properties of [`regproc-receiver` Helm Chart](https://github.com/mosip/mosip-helm/blob/1.2.0/charts/regproc-group1/values.yaml)

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)

