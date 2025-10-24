This document outlines how to use *`PacketCreator_and_Upload_Test_Script.jmx`* to create, sync and upload identity packets into **MOSIP Registration Processor** module in bulk.

# 1. Approach
* This script will be used to generate large number of packets for this test. Based on the workload model, atleast  3000 packets need to be generated and uploaded to simulate significant load. Packet size should be at least 2MB in size.
* The "registration-processor-common-camel-bridge" pod in the rancher deployments should be paused until all preparation (P01-P03) is completed. (i.e. Number of pods = 0). 
* Performance should be monitored during "P03 Sync And Upload Registration Packet (Preparation)" to measure how system handles large volume of packet upload.
* Once "P03 Sync And Upload Registration Packet (Preparation)" step is complete. The "registration-processor-common-camel-bridge" pod will be enabled again to allow resgistration to automatically continue to process the large volume of newly uploaded packets. Performance should be monitored to measure how systhem handles this processing load.


# 1. Prerequisites
1. Tools
    * Java 8 or higher
    * Jmeter 5.6.3 
    * MOSIP PacketCreator tool (Check PacketCreatorToolSetup.md)

2. Mosip modules
	* Id repository	1.2.1.0
	* registration-processor	1.2.0.1
	* ida	1.2.0.1
	* Mock abis	1.2.0.2
	* Mock Mv	1.2.0.2
	* Packet manager	1.2.0.1
	* Key Manager	1.2.0.1
	* Auth Manager	1.2.0.1
	* Bio Sdk	1.2.0.1
	* prereg	1.2.0.1
	* Kernel Master Data	1.2.1.0
	* Partner Management	1.2.1.0
	* Kernel Notifier	1.2.0.1
	* Kernel Audit manager	1.2.0.1

# 2. Setup
1. Update user defined values
    Update all variable values within the *`user defined variables`* config. Values will include host environment settings, filepaths, secret keys, execution settings etc.

2. Update support files
    ensure the file "contextDetails.csv" exists within "supportFilePath". It should contain valid userID,machineID,centerID and password from Master Data database.

# 3. Preparation

Each 'thread group' should be executed one at a time. Ensure all other 'thread groups' are  explicitly disabled (Ctrl-T), except for the one that is being executed. 

## A00 Auth Token Generation (Preparation)
* This is the first 'thread group' that needs to execute.
* It creates/replaces auth token files that are used by other 'thread groups'.
* The Auth token have expiration time which is controlled by MOSIP settings. Ensure the tokens do not expire before or during execution of other 'thread groups'.
* For execution, set all thread settings (Number of threads, Ramp-up period and Loop count) to 1. 

## P01 Packet Generation (Preparation)
* This 'thread group' generates random unique identity packets and saves them as zipped files to 'mounthpath'
* As a prerequisite, PacketCreator tool is required as a background service. (Check PacketCreatorToolSetup.md to run this tool)
* This thread group supports multithreading. Max of 7 parallel threads were used for internal testing due to limitation of PacketCreator tool.
* Number of packets created is controlled by below formula
	
	Number of packets = a X b X c
		
		a = number of thread (Thread Properties) 
		b = Loop count (Thread Properties)
		c =  packetCreationCountPerUser (User Defined Variables)
* This thread group will save the list of newly created packets  in *`${runTimeFilePath}/store_packet_path.txt`*

**Optional**: P01S05 and P01S06 are disabled by default. This  allows us to supply large volume of requests to the next Thread groups in short period of time. If enabled, the script will sync and upload the packet as soon as they are created and, P02 and P03 do not need to execute.

## P02 Packet Creator Rid Sync (Preparation)
* This 'thread group' reads packets listed in *`store_packet_path.txt`* as input and performs RID sync.
* The list of synced packets are stored in *`${runTimeFilePath}/request_body_sync_packet.txt`* as output.

## P03 Sync And Upload Registration Packet (Preparation)
* "registration-processor-common-camel-bridge" pod should be paused during this step. 
* This 'thread group' reads packets listed in *`request_body_sync_packet.txt`* as input and syncs and uploads them.
* The list of Ids are listed in *`${runTimeFilePath}/reg_id.txt`* as output.

## Registration processing
* This step is not executed with this Jmeter script.
* "registration-processor-common-camel-bridge" pod should be resumed. 
* Once the pod is resumed, the system will automatically start processing the large volume of packets uploaded during step P03. 
* To measure performance/TPS, the database can be queried to check for time taken to process all of new reg_id.

