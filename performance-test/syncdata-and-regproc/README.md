
### Contains
* This folder contains performance Test script of below API endpoint categories.
    01. Auth Token Generation (Setup)
    02. Packet Generation (Setup)
    03. S01 and S03 Sync Registration Packet And Generate RID (Preparation)
    04. S01 Sync And Upload New Registration Packet (Execution)
    05. S02 Sync Data To The Server (Execution)
    06. S03 Get Transaction Details From Reg Id (Execution)


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
* We need some dependent jar files that needs to be in the lib folder of jmeter/bin, following are the dependent jar files:
	1. mock-mds-1.2.1-SNAPSHOT.jar
	2. kernel-syncdata-service-1.2.0.1.jar
	3. kernel-logger-logback-1.2.0.1.jar
	4. kernel-core-1.2.0.1.jar
	5. kernel-keymanager-service-1.2.0.1-lib.jar

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
	Step 3 - For packet generation, need to create context for it. Will require api-internal.cellbox1.mosip.net.12117.reg.key provided by the team or we can generate it from the dsl setup.
	Step 4 - Update the secret keys for all the client's, user id, machine id and center id in create context. Also, update the mountPath and authCertsPath path variable.
	Step 5 - Create packet.

*Once the packet utility is up and running please check this swagger link
	Swagger link - http://localhost:8080/v1/packetcreator/swagger-ui.html#/packet-controller

*To run packet utility use following command as reference:

java -jar -Dfile.encoding=UTF-8 -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=9999,suspend=n -jar dslrig-packetcreator-1.2.1-develop-SNAPSHOT.jar --spring.config.location=file:///C:\Users\61091460\Documents\centralized\mosip-packet-creator\config\application.properties>>C:\Users\61091460\Documents\centralized\mosip-packet-creator\PacketUtilityRunlog.txt

###  Setup for packet creation
	1. The centers, machines and users should be onboarded in the system before using as part of context_details.csv 	
	2. Add the document path of (document.pdf) present in the support-files folder to the file document_path.txt
	3. The mosip-packet-creator and mount volume folders need to be present with the latest jar.
	4. From the terminal run the command to start mosip-packet-creator as mentioned in above steps.
	5. Open the [Packet_Creator_Script.jmx] script and run the Auth Token Generation (Setup) thread group.
	6. Execute Packet Generation (Setup) thread group by specifying the no of packets it need to generate.
	7. Sync the packets to reg client using Sync Registration Packet (Setup) thread group.
	8. Finally Sync And Upload Registration Packet (Preparation) thread group for uploading the packets.

### Script execution steps:

	* Auth Token Generation (Setup) - In this thread group we are creating the authorization token values of Syncdata, Regpoc, Resident and Regproc - Using User Id which will be saved to a file in the Run Time Files in bin folder of JMeter.

	* Packet Generation (Setup) - In this thread group we will basically create the context with the help of existing center id's, machine id's & user id's present in our current environment & we will read them through a file named context_details.csv. Once the contexts are created we will use the same in the execution thread group where basically the packet generation happens & then the packet path gets stored in a file naming as Run Time Files in bin folder of JMeter.

	* S01 and S03 Sync Registration Packet And Generate RID (Preparation) - To create encrypted data for generated packets(test data to registration processor sync API). Which will basically create a file with the encrypted data's for all the packets created. Also, we prepare the reg id's required for the execution of S03 to get the transaction details.

	* S01 Sync And Upload Registration Packet (Execution) : 
		* S01 T01 Sync Registration Packet : This API endpoint will sync the packets.
		* S01 T02 Upload Registration Packet : This API endpoint will upload the packets to registration processor.
	
	* S02 Sync Data To The Server (Execution) :
		* S02 T01 Auth Token Details Encrypted Based On Machine Key : This API endpoint sync the auth token details based on machine key.
		* S02 T02 Public Key Verify : This API endpoint verifies the public key generated.
		* S02 T03 Get Certificate : This API endpoint gets the certificates.
		* S02 T04 Get User Details : This API endpoint gets the user details.
		* S02 T05 Get Client Settings : This API endpoint gets the client settings.
		* S02 T06 Get Configs : This API endpoint gets the config settings.
		* S02 T07 Get LatestId Schema : This API endpoint gets the latest schema ID.
		* S02 T08 Get CaCertificates : This API endpoint gets CA certificates.

	* S03 Get Transaction Details From Reg Id (Execution) :
		* S03 T01 Get Transaction Details From Reg Id : This API endpoint will fetch the transaction details using reg id.
 

### Downloading Plugin manager jar file for the purpose installing other JMeter specific plugins

* Download JMeter plugin manager from below url links.
	*https://jmeter-plugins.org/get/

* After downloading the jar file place it in below folder path.
	*lib/ext

* Please refer to following link to download JMeter jars.
	https://mosip.atlassian.net/wiki/spaces/PT/pages/1227751491/Steps+to+set+up+the+local+system#PluginManager
		
### Designing the workload model for performance test execution

* Calculation of number of users depending on Transactions per second (TPS) provided by client

* The script and the below calculation is preconfigured as per 100 tps, if you are testing for other tps, the below values needs to be adjusted.

* Applying little's law
	* Users = TPS * (SLA of transaction + think time + pacing)
	* TPS --> Transaction per second.
	
* For the realistic approach we can keep (Think time + Pacing) = 1 second for API testing
	* Calculating number of users for 10 TPS
		* Users= 100 X (SLA of transaction + 1)
		       = 100 X (1 + 1)
			   = 200
			   
### Usage of Constant Throughput timer to control Hits/sec from JMeter

* In order to control hits/ minute in JMeter, it is better to use Timer called Constant Throughput Timer.  This is calculated explicitly for each thread group based on the scenario's weightage

* If we are performing load test with 10TPS as hits / sec in one thread group. Then we need to provide value hits / minute as in Constant Throughput Timer
	* Value = 10 X 60
			= 600

* Dropdown option in Constant Throughput Timer
	* Calculate Throughput based on as = All active threads in current thread group
		* If we are performing load test with 10TPS as hits / sec in one thread group. Then we need to provide value hits / minute as in Constant Throughput Timer
	 			Value = 10 X 60
					  = 600
		  
	* Calculate Throughput based on as = this thread
		* If we are performing scalability testing we need to calculate throughput for 10 TPS as 
          Value = (10 * 60 )/(Number of users)

