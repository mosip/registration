# Packet Uploader Stage

## About
* Picks packet from [Packet Server]() and starts processing.  
* Performs sanity checks (like virus scan, checksum validation, file size) on the decrypted packet. 
* Uploads sub-packets along with meta info in [Object Store]().

## Default context, path, port
Refert to [bootstrap properties](src/main/resources/bootstrap.properties)
