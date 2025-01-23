### Contains
* This folder contains 2 different Jmeter sscripts 
	Script 1. Packet creator script to create packets befor the test run [Packet_Creator_Script.jmx]
	Script 2. Scenario-based performance test scripts for Packet and Credential Processing [Packet_Credential_Processing_Test_Script.jmx]
 
*List of  Thread Groups in script 1: 
	* 01 Auth Token Generation (Setup)
	* 02 Packet Generation (Setup)
	* 03 Sync Registration Packet (Setup)
	* 04 Sync And Upload Registration Packet (Preparation)

*List of  Thread Groups in script 2: 
	* S01 Secure Zone To Upload Packet
	* S02 Upload Packet To Validate Packet
	* S03 Validate Packet To Packet Classification
	* S04 Packet Classification To CMD Validation
	* S05 CMD Validation To Operator Validation
	* S06 Operator Validation To Supervisor Validation
	* S07 Supervisor Validation To Quality Classifiier
	* S08 Quality Classifiier To Demographic Verification
	* S09 Demographic Verification To Biographic Verification
	* S10 Biographic Verification To UIN Generation
	* S11 UIN Generation To Biometric Extraction
	* S12 Biometric Extraction To Finalization
	* S13 Finalization To Print Service
 	* S14 Print Service To Internal Workflow Action
	* S15 Complete Reg Proc Packet Processing 
	* S16 End to End Packet and Credential Processing 
	* S17 Overall Status Of  The Packets

* Open source tools used,
    1. [Apache JMeter](https://jmeter.apache.org/)

Pre-requisite to install the plugins:
1. Download JMeter Plugin Manager jar file with following link. "https://jmeter-plugins.org/get/"
2. Place the jar file under following JMeter folder path.
3. After adding the plugin restart the JMeter 
4. To download the necessary plugins, we have to click on the Plugin’s Manager. It will re-direct to list of available plugins. Choose the above mentioned plugin "jmeter-plugins-synthesis" to install and then restart the JMeter.

### Setup points before execution

* We need some jmeter plugin files that needs to be installed before opening of this script, PFA dependency links for your reference : 
	* jmeter-plugins-synthesis-2.2.jar
	* <!-- https://jmeter-plugins.org/files/packages/jpgc-synthesis-2.2.zip -->

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module.
* Place the support files in the bin folder of the jmeter, the paths in the scripts are defined to fetch the testdata from the bin folder.
* Start JMeter by running the jmeter.bat file for Windows or jmeter file for Unix. 
* Validate the scripts for one user.
* Execute a dry run for 10 min.
* Execute performance run with various loads in order to achieve targeted NFR's.

### Installation of Packet Utility as a pre-requisite: 

*The mentioned service is pre-requisite for Reg proc module and must be running in the system as it is used to create packets.

*Path for Packet Utility : https://github.com/mosip/mosip-automation-tests/tree/master/mosip-packet-creator/src/main/resources/dockersupport/centralized 

*Readme file is present to follow the steps : https://github.com/mosip/mosip-automation-tests/blob/master/README.md 

*For generating the packets, packet utility is required.
	Step 1 - Packet utility setup.
	Step 2 - We need device partner and device dsk partner certificate to be present in the auth certs.
	Step 3 - For packet generation, need to create context for it. Will require api-internal.qa-platform1.mosip.net.12117.reg.key provided by the team or we can generate it from the dsl 		setup.
	Step 4 - Update the secret keys for all the client's, user id, machine id and center id in create context. Also, update the mountPath and authCertsPath path variable.
	Step 5 - Create packet.

*Once the packet utility is up and running pls check this swagger link
	Swagger link - http://localhost:8080/v1/packetcreator/swagger-ui.html#/packet-controller

*To run packet utility use following command as reference:

java -jar -Dfile.encoding=UTF-8 -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=9999,suspend=n -jar dslrig-packetcreator-1.2.1-develop-SNAPSHOT.jar --spring.config.location=file:///C:\Users\61091460\Documents\centralized\mosip-packet-creator\config\application.properties>>C:\Users\61091460\Documents\centralized\mosip-packet-creator\PacketUtilityRunlog.txt

### Test Setup for Script 1
	1. The centers, machines and users should be onboarded in the system before using as part of context_details.csv 	
	2. Add the document path of (document.pdf) prsent in the support-files folder to the file document_path.txt
	3. The mosip-packet-creator and mount volume folders need to be present with the latest jar.
	4. From the terminal run the command to start mosip-packet-creator as mentioned in above steps.
	5. Open the [Packet_Creator_Script.jmx] script and run the Auth Token Generation (Setup) thread group.
	6. Excute Packet Generation (Setup) thread group by spcifying the no of packets it need to generate.
	7. Sync the packets to reg client using Sync Registration Packet (Setup) thread group.
	8. Finally Sync And Upload Registration Packet (Preparation) thread group for uploading the packets.

### Description of thread groups from script 1:

	*01 Auth Token Generation (Setup) - In this thread we are creating the authorization token values of Regpoc, Resident and Regproc - Using User Id which will be saved to a file in the 	Run Time Files in bin folder of JMeter.

	*02 Packet Generation (Setup) - In this thread group we will basically create the context with the help of existing center id's, machine id's & user id's present in our current 		environment & we will read them through a file named contextDetails.csv. Once the contexts are created we will use the same in the execution thread group where basically the 		packet generation happens & then the packet path gets stored in a file naming as Run Time Files in bin folder of JMeter.

	*03 Sync Registration Packet (Setup) - To create encrypted data for generated packets(test data to registration processor sync API). Which will basically create a file with the 		encrypted data's for all the packets created.

	*04 Sync And Upload Registration Packet (Preparation) : This step will sync and upload the generated packets to the registration processor.

### Test Setup for Script 2:

	1. From S01 to S17 thread groups will be run after the completion of the test which means most packets have finished processing. These are queries to the database which determine 	`	the reponse times between each stage and also the 	overall performance.
	2. List of RID's will be required as per environment which is valid and will be prepared while sync and upload to reg proc was run as part of script 1.
	3. Execute the Script 2 with all thread groups enabled, so that it will start printing the results in the aggregate report. 

### Description of the thread groups from script 2:

	* S01 Secure Zone To Upload Packet: Ensuring the packet is uploaded securely.

	* S02 Upload Packet To Validate Packet: Verifying the integrity and authenticity of the uploaded packet.

	* S03 Validate Packet To Packet Classification: Categorizing the packet based on predefined criteria.

	* S04 Packet Classification To CMD Validation: Checking the packet against Command (CMD) validation rules.

	* S05 CMD Validation To Operator Validation: Operator reviews and validates the packet.

	* S06 Operator Validation To Supervisor Validation: Supervisor performs an additional layer of validation.
	
	* S07 Supervisor Validation To Quality Classifier: Quality control checks are conducted.

	* S08 Quality Classifier To Demographic Verification: Verifying demographic details within the packet.

	* S09 Demographic Verification To Biographic Verification: Confirming biographic information.

	* S10 Biographic Verification To UIN Generation: Generating a Unique Identification Number (UIN) for the packet.

	* S11 UIN Generation To Biometric Extraction: Extracting biometric data from the packet.
	
	* S12 Biometric Extraction To Finalization: Finalizing the packet processing.

	* S13 Finalization To Print Service: Preparing the packet for printing services.

	* S14 Print Service To Internal Workflow Action: Internal actions are taken based on the printed packet.

	* S15 Complete Reg Proc Packet Processing: Completing the registration process for the packet.

	* S16 End to End Packet and Credential Processing: Full processing of the packet and associated credentials.

	* S17 Overall Status Of The Packets: Reporting the overall status of all packets.