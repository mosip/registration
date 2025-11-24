This module describes how to conduct load test of packet upload and credential processing using provided JMeter script.

# Contains
* This directory contains 2 different JMeter scripts.
    * Script 1. Packet creator script to create packets [Credential_Processing_Test_Script.jmx](script/Credential_Processing_Test_Script.jmx) - Main Script
    * Script 2. Scenario-based results scripts for Credential Processing [Results_Script.jmx](script/Results_Script.jmx) - For post test result analysis only

* List of API endpoint categories in script 1:
    01. A00 Auth Token Generation (Preparation)
    02. P01 Packet Generation (Preparation)
    03. P02 Packet Creator Rid Sync (Preparation)
    04. P03 Sync And Upload Registration Packet (Preparation)
    05. Additional Script - [Results_Script.jmx](script/Results_Script.jmx)

* Open source tools used,
    01. [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
    02. [Apache JMeter 5.6.3](https://jmeter.apache.org/download_jmeter.cgi) 
    02. [mosip-packet-creator](https://github.com/mosip/mosip-automation-tests/tree/master/mosip-packet-creator)

# How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download JMeter Plugin Manager jar file from https://jmeter-plugins.org/get/ , and install by placing the it in "Jmeter/apache-jmeter-X.X.X/lib/ext" 
* Download scripts for the required module from the [script](script/) folder of this repo.
* Start JMeter by running the jmeter.bat/jmeter.sh as per your OS. 
* Load the downloaded [Credential_Processing_Test_Script.jmx](script/Credential_Processing_Test_Script.jmx) script onto JMeter. If prompted, install the required plugins.
* If plugins were installed, restart JMeter.
* Update "User Defined Variables" within the JMeter scripts. This list holds environment endpoint URL, protocols, users, secret keys, passwords, runtime file path, support file path etc.
* Complete [setup points before execution](#setup-points-before-execution)
* Validate the scripts is working functionally. 
    * Disable all "thread groups" within the test plan by clicking 'disable'. 
    * Enable and execute only one thread at one time during this step.  
    * Sequentially, execute each thread group with Number of VUser and Iteration set to 1. 
    * Go to [script execution steps](#script-execution-steps) for further detail.
* Take average scenario response time obtained from above test and update "Scenario Response time" column in [MOSIP_TPS_Thread_setting_calculator](MOSIP_TPS_Thread_setting_calculator-packet_credential_processing.xlsx). 
* Execute a dry run for 10 min. The execution duration is controlled by "testDuration" variable.
* Use [MOSIP_TPS_Thread_setting_calculator](MOSIP_TPS_Thread_setting_calculator-packet_credential_processing.xlsx) to calculate the thread settings required for your target load.
* Execute performance run with various loads in order to achieve targeted NFR's. For a performance run, all scenarios (S01, S02, S03....) should be enabled and executed at the same time.

# Setup points before execution
* Download and place following library into JMeter’s library (apache-jmeter-5.6.3\lib). JMeter restart is required afterwards.
  * [postgresql-42.7.8.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.8/postgresql-42.7.8.jar) - For [Results_Script.jmx](script/Results_Script.jmx)
* This script uses `mosip-packet-creator` tool as a background service to generate new packets. Follow the steps in [PacketCreatorToolSetup](../PacketCreatorToolSetup.md) first to start the service. The tool should be running in background while the JMeter script is running. 
* This script will be used to generate large number of packets for credential processing. Based on the workload model, at least 3000 packets need to be generated and uploaded to simulate significant load. Packet size should be at least 2MB in size.
* The "registration-processor-common-camel-bridge" pod should be stopped until all preparation (P01-P03) is completed. (i.e. Number of pods = 0). 
* Performance should be monitored during "P03 Sync And Upload Registration Packet (Preparation)" to measure how system handles large volume of packet upload.
* Once "P03 Sync And Upload Registration Packet (Preparation)" step is complete. The "registration-processor-common-camel-bridge" pod will be enabled again to allow registration to automatically continue to process the large volume of newly uploaded packets. Performance should be monitored to measure how system handles this processing load.
* Delete runtime files created during previous execution from {runTimeFilePath} folder.

# Script execution steps:

    * A00 Auth Token Generation (Preparation) - In this thread group we are creating the authorization token values of Regproc and Resident - Using User Id which will be saved to a file in the Run Time Files in bin folder of JMeter. The authorization tokens have expiration time which is controlled by MOSIP settings. Ensure the tokens remain valid throughout the duration of the test execution.

    * P01 Packet Generation (Preparation) - In this thread group we will create the context with the help of details present in the contextDetailJSON.txt. Documentation on how to create this file is in PacketCreatorToolSetup.md. Executing this thread group generate packet & stores the filepath in a file named as P01_store_packet_path.txt. (Optionally) This step also includes 'Packet Creator Rid Sync' and 'Sync Registration Packet - v2' to create preparatory test data. However, enabling these will make the data preparation process slower.

    * P02 Packet Creator Rid Sync (Preparation) - This thread group reads the packet paths generated by 'P01 Packet Generation' in the file P01_store_packet_path.txt and syncs them to PacketCreator. The list of synced packets paths are stored in P02_request_body_sync_packet.txt
    
    * P03 Sync And Upload Registration Packet (Preparation) - This thread group reads packets listed in P02_request_body_sync_packet.txt, and syncs and uploads them to the database. The resulting registration IDs are saved in P03_reg_id.txt. To build queue of a large number of unprocessed data, "registration-processor-common-camel-bridge" should be disabled during this step.
    
    * Credential processing - This part of test is not executed via JMeter script. Once a large number of unprocessed data is queued with 'P03 Sync And Upload Registration Packet', the processing can be triggered by starting "registration-processor-common-camel-bridge". 
    
    * Result Analysis:
      - Once enough time have passed to process all queued credentials, the performance can be analysed executing Results_Script.jmx in JMeter. 
      - This script will also require its own update of "User Defined Variables". 
      - Rename the file "P03_reg_id.txt" as "Results_reg_id.txt" and place it in this script's support folder. 
      - This script will analyse performance of all registrations supplied in Results_reg_id.txt and show time spent in each stage.


## Designing the workload model for performance test execution

* The script is preconfigured for 100 tps within our test environment. Performance may vary based on hardware and infrastructure settings.

* If you are testing for different tps or with different hardware settings, adjustment needs to made to thread group settings within the script.

* [MOSIP_TPS_Thread_setting_calculator-packet_credential_processing.xlsx](MOSIP_TPS_Thread_setting_calculator-packet_credential_processing.xlsx) applies Little's law to recommend required thread settings inputs.         
                    
## Support files required for this test execution:

1. [contextDetailJSON.txt](support-files/contextDetailJSON.txt) - This sample file contain various environment variables that need to be updated. It must contain a valid JSON. It is the primary input to use `mosip-packet-creator` tool. Documentation available in [PacketCreatorToolSetup](../PacketCreatorToolSetup.md)
2. [Results_reg_id.txt](support-files/Results_reg_id.txt.txt) - This is a sample of list of registration ids used by Results_Script.jmx
