### Contains
* This folder contains scenario-based performance test scripts for Packet and Credential Processing:
   
*List of  Scenarios: 
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

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module.
* Place the support files in the bin folder of the jmeter, the paths in the scripts are defined to fetch the testdata from the bin folder.
* Start JMeter by running the jmeter.bat file for Windows or jmeter file for Unix. 
* Validate the scripts for one user.
* Execute a dry run for 10 min.
* Execute performance run with various loads in order to achieve targeted NFR's.

### Setup points before execution

* We need some jmeter plugin files that needs to be installed before opening of this script, PFA dependency links for your reference : 
	* jmeter-plugins-synthesis-2.2.jar
	* <!-- https://jmeter-plugins.org/files/packages/jpgc-synthesis-2.2.zip -->
	
Pre-requisite to install the plugins:
1. Download JMeter Plugin Manager jar file with following link. "https://jmeter-plugins.org/get/"
2. Place the jar file under following JMeter folder path.
3. After adding the plugin restart the JMeter 
4. To download the necessary plugins, we have to click on the Plugin’s Manager. It will re-direct to list of available plugins. Choose the above mentioned plugin "jmeter-plugins-synthesis" to install and then restart the JMeter.

* Auth Token Generation (Setup) - In this thread we are creating the authorization token values of Regpoc, Resident and Regproc - Using User Id which will be saved to a file in the Run Time Files in bin folder of JMeter.

* Packet Generation (Setup) - In this thread group we will basically create the context with the help of existing center id's, machine id's & user id's present in our current environment & we will read them through a file named contextDetails.csv. Once the contexts are created we will use the same in the execution thread group where basically the packet generation happens & then the packet path gets stored in a file naming as Run Time Files in bin folder of JMeter.

* Sync Registration Packet - v2 (Setup) - To create encrypted data for generated packets(test data to registration processor sync API). Which will basically create a file with the encrypted data's for all the packets created.

* Sync And Upload Registration Packet (Preparation) : This step will sync and upload the generated packets to the registration processor.

*** Note: The centers, machines and users should be onboarded in the system before using as part of context_details_cellbox1.txt ***

* From S01 to S17 thread groups will be run after the completion of the test. These are queries to the database which determine the reponse times between each stage and also the overall performance.

### Data prerequisite

* List of RID's as per environment which is valid and will be prepared while sync and upload to reg proc will be run

### Description of the scenario's

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