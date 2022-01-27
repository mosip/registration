# Bio Dedupe Stage

## About
The Bio dedupe stage is divided into 2 parts :
* PRE-ABIS Identification : functions performed before sending packet to abis
	* Verifies if biometric modalities are present as per configuration. Otherwise sends packet for verification.
	* 
* Post Abis Identification : functions performed after receiving response from abis
	* Forwards request to next stage if no duplicate is found.
	* If duplicates are found then decides if the duplicate ids should be considered based on duplicate id status in regproc db.
	* Forwards packet for Manual Adjudication when duplicates are found.

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)

