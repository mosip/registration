This document outlines how to use *`Regproc_Syncdata_Test_Script.jmx `* to simulate prod like load for **MOSIP Registration Processor** module.

# 1. Approach
* This script generates load that simulates 
	1. Sync and Upload of New Registration packet,
	2. Sync Data to server
	3. Get Transaction details from RID
* `Sync and Upload of New Registration packet` relies on `MOSIP PacketCreator tool`. The setup and use for this is detailed in "./PacketCreator". It is recommended to successfully run `./PacketCreator/PacketCreator_and_Upload_Test_Script.jmx` before attempting to execute this script.
* `Sync Data to server` requires a large number of Machines within the Master Database that contains unique keys. These unique keys can be generated from "KeyGenMadeEasy/justKeys.py" script.
* `Get Transaction details from RID` requires a existing list of RIDs in the database. If RIDs do not already exist, new RIDs can be created using `./PacketCreator/PacketCreator_and_Upload_Test_Script.jmx` as well. 


# 2. Prerequisites
1. Tools
    * Java 8 or higher
    * Jmeter 5.6.3 
		* additional Libraries
			- bcprov-jdk15on-1.70.jar
    * MOSIP PacketCreator tool (Check PacketCreatorToolSetup.md)
	* Python

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


# 3. Setup
1. Update user defined values
    Update all variable values within the *`user defined variables`* config. Values will include host environment settings, filepaths, secret keys, execution settings etc.

2. Update support files
    - ensure these files exists within "supportFilePath" or "runTimeFilePath" before load test. 
		- contextDetails.csv (userID,machineID,centerID from Master Data database).
		- D01_A01_zones.csv (list of zone and location codes from Master Database)
		- A02_S02_machine_id.csv (Machine details from Master DB/ it can be created from step A02)
		- D02_S02T03_KeyAlias.csv (Certificate details from database)
		- D03_S03_reg_id.txt (RIDs from DB/It can be created from PacketCreator_and_Upload_Test_Script.jmx)


# 4. Preparation

Each 'thread group' should be executed one at a time. Ensure all other 'thread groups' are  explicitly disabled (Ctrl-T), except for the one that is being executed. 

## 00 Auth Token Generation (Preparation)
* This is the first 'thread group' that needs to execute.
* It creates/replaces auth token files that are used by other 'thread groups'.
* The Auth token have expiration time which is controlled by MOSIP settings. Ensure the tokens do not expire before or during execution of other 'thread groups'.
* For execution, set all thread settings (Number of threads, Ramp-up period and Loop count) to 1. 

## A01 Create ZoneUsers and Centers (Setup)
* This will create a list of centers within the master DB and activate them.
* The list will be based on the list of zones and location provided in "D01_A01_zones.csv"
* This step only needs to be executed once per environment.

## A02 Create Machines (Setup)
* This will create a list of Machines within the master DB and activate them.
* The list will be based on the list of Centers created under step A01.
* The machines will also use the keys provided in the folderpath "{keyPath}/reg_######".
* These keys can be generated using `KeyGenMadeEasy` script.
* This step only needs to be executed once per environment.

## P01 Sync And Upload New Registration Packet(Preparation)
* This 'thread group' generates random unique identity packets and saves them as zipped files to 'mounthpath'
* As a prerequisite, PacketCreator tool is required as a background service. (Check PacketCreatorToolSetup.md to run this tool)
* The execution of this step is identical to `./PacketCreator/PacketCreator_and_Upload_Test_Script.jmx`

## P03 Generate RID (Preparation)
* This will create a list of new packet ready to be uploaded an synced. 
* This 'thread group' reads packets listed in *`request_body_sync_packet.txt`* as input and syncs and uploads them.
* The list of Ids are listed in *`${runTimeFilePath}/reg_id.txt`* as output.

# 5. Execution
* Complete all above Setup and Preparation Steps.
* Enable only execution threads (S01,S02,S03). Disable all Setup and Preparation Threads.
* Determine the load(TPS) required for the load test.
* Apply thread setting required for the load test. Use `MOSIP TPS Thread setting calculator - RegProc_SyncData` to calculate the thread settings.
* Execute the script.