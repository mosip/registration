
### Contains
* This folder contains performance Test script of below API endpoint categories.
    01. Auth Token Generation (Setup)
    02. Create Centers And Machines (Setup)
    03. S01 Packet Creation (Preparation)
    04. S01 Sync And Upload New Registration Packet (Execution)
    05. S02 Sync Data To The Server (Execution)
    06. S03 Get Transaction Details From Reg Id (Execution)


* Open source tools used,
    1. [Apache JMeter](https://jmeter.apache.org/)

Pre-requisite to install the plugins:
1. Download JMeter Plugin Manager jar file with following link. "https://jmeter-plugins.org/get/"
2. Place the jar file under following JMeter folder path (bin/lib/ext).
3. After adding the plugin restart the JMeter 
4. To download the necessary plugins, we have to click on the Plugin’s Manager. It will re-direct to list of available plugins. Choose the above mentioned plugin "jmeter-plugins-synthesis" to install and then restart the JMeter.

### Setup points before execution

* We need some jmeter plugin files that needs to be installed before opening of this script, PFA dependency links for your reference : 
	* jmeter-plugins-synthesis-2.2.jar
	* <!-- https://jmeter-plugins.org/files/packages/jpgc-synthesis-2.2.zip -->
* We need some dependent jar files that needs to be in the lib folder of jmeter, following are the dependent jar files:
	1. mock-mds-1.2.1.jar
	2. kernel-syncdata-service-1.2.0.1.jar
	3. kernel-logger-logback-1.2.0.1.jar
	4. kernel-core-1.2.0.1.jar
	5. kernel-keymanager-service-1.2.0.1-lib.jar
	6. jackson-databind-2.17.1.jar
	7. jackson-module-afterburner-2.17.1.jar
	8. TSS.Java-0.3.0.jar
	9. spring-context-6.1.9.jar

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module from the respective module repo's.
* Place the scripts and the support files in the jemter bin folder.
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
	Step 2 - We need environment specific auth certs to be present in the packet creator folder
	Step 3 - For packet generation, we need to create context for it and also a private key. Example for a private key would be something like this(api-internal.cellbox1.mosip.net.12117.reg.key). This can be generated from the dsl setup.
	Step 4 - Update the secret keys for all the client's, user id, machine id and center id in create context. Also, update the mountPath and authCertsPath path variable in the JMeter script.
	Step 5 - Create packet.

*Once the packet utility is up and running please check this swagger link
	Swagger link - http://localhost:8080/v1/packetcreator/swagger-ui.html#/packet-controller

*To run packet utility use following command as reference:

java -jar -Dfile.encoding=UTF-8 -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=9999,suspend=n -jar dslrig-packetcreator-1.2.1-develop-SNAPSHOT.jar --spring.config.location=file:///C:\Users\61091460\Documents\centralized\mosip-packet-creator\config\application.properties>>C:\Users\61091460\Documents\centralized\mosip-packet-creator\PacketUtilityRunlog.txt

###  Setup for packet creation
	1. The centers, machines and users should be onboarded in the platform before using them as part of context_details.csv 	
	2. The mosip-packet-creator and mount volume folders need to be present with the latest packet creator jar.
	3. From the terminal run the command to start mosip-packet-creator as mentioned in above steps.
	4. Open the [Regproc_Syncdata_Test_Script.jmx] script and run the Auth Token Generation (Setup) thread group.
	5. Execute Packet Generation (Setup) thread group by specifying the no of packets it needs to generate.
	6. Sync the packets to mosip-packet-creator using Sync Registration Packet (Setup) thread group.
	7. Finally Sync And Upload Registration Packet (Execution) thread group for uploading the packets.

### Script execution steps:

	* Auth Token Generation (Setup) - In this thread group we are creating the authorization token values of Syncdata, Regpoc, Resident and Regproc - Using User Id which will be saved to a file in the Run Time Files in bin folder of JMeter.

	* Create Centers And Machines (Setup) - In this thread group we create centres and machines before starting the test, basedata setup for any new environment.

	* Packet Creation (Preparation) - In this thread group we will basically create the context with the help of existing center id's, machine id's & user id's present in our current environment & we will read them through a file named context_details.csv. Once the contexts are created we will use the same in the execution thread group where basically the packet generation happens & then the packet path gets stored in a file naming as Run Time Files in bin folder of JMeter. This step also includes rid sync request to packet creator and sync, upload to reg proc to create preparatory test data for us. This step is also for the RID search scenario as a pre-requisite.


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
	* SLA --> Service level agreement
	* Think time --> Time pause given between transactions to simulate realistic user behaviour	
	* Pacing --> Used to control rate of iteration's during the test.
	
* For the realistic approach we can keep (Think time + Pacing) = 1 second for API testing
	* Calculating number of users for 100 TPS
		* Users= 100 X (SLA of transaction + 1)
		       = 100 X (1 + 1)
			   = 200
			   
### Usage of Constant Throughput timer to control Hits/sec from JMeter

* In order to control hits/ minute in JMeter, it is better to use Timer called Constant Throughput Timer.  This is calculated explicitly for each thread group based on the scenario's weightage

* If we are performing load test with 10TPS as hits / sec in one thread group. Then we need to provide value hits / minute as in Constant Throughput Timer
	* Value = 10 X 60
			= 600

* Dropdown option in Constant Throughput Timer (There are options in the Constant Throughput Timer based on what we want to achieve like throughput for each thread or for the complete thread group. Find the calculations for all active threads and for any given single thread )

* Calculate Throughput based on as = All active threads in current thread group (This is a dropdown option in the Constant Throughput Timer)
	* If we are performing load test with 10TPS as hits / sec in one thread group. Then we need to provide value hits / minute as in Constant Throughput Timer
	 			Value = 10 X 60
					= 600


### Support files required for this test execution:

1. app_machine_details.csv - This support file contains test data like appId, refId, machineName, public key and sign public key.
2. context_details.csv - This support file contains userId, password, center and machine details.
3. machine_details.csv - This support file contains all the information needed to create machines.
4. reg_centers.csv - This support file contains data needed to create new centers.