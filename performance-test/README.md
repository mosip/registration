This folder contains performance test scripts & test data for Registration Processor Module.

### Environment Required:-
***Below modules should be running in kubernetes setup***

* Kernel Auth manager
* Kernel Syncdata
* Kernel Notification Manager
* Kernel Key manager
* Kernel Master data
* Kernel Audit manager
* All regproc & dmzregproc services

* Open source Tools used,
    1. [Apache JMeter](https://jmeter.apache.org/)

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module.
* Start JMeter by running the jmeter.bat file for Windows or jmeter file for Unix. 
* Validate the scripts for one user.
* Execute a dry run for 10 min.
* Execute performance run with various loads in order to achieve targeted NFR's. 
* From Jmeter plugins manager install synthesis report plugin - (jmeter-plugins-synthesis:version: 2.2) for reporting purpose and also this listner is added in the scripts so we need this plugin in our jmeter or else the script might not load properly in the jmeter. 



### How to create test data:-

* We need to run the packet generation utility. You can refer to the Part A section of [Packet Generation utility](https://mosip.atlassian.net/wiki/spaces/R1/pages/330825775/Automation+release+notes+and+deliverables). 

* We have a test element named 'User Defined Variables' in the script where some of the values are parameterized & can be changed based on our requirements which will further reflect in the entire script.



### How to run JMeter Helper & Test scripts:-

* We need to take care of the prerequisites before running our tests.

* In the Regproc_Test_script.jmx we have one thread group Auth Token Generation (Setup) for the creation of authorization token.

* The authorization token created will be saved to a file in the Run Time Files in bin folder of JMeter which will be used further by our test script for execution.

* Once all the prerequisites are taken care we will jump to the our actual execution scenario's which will take place for all the Regproc API's. The script is Regproc_Test_script.jmx

* In the script we have total 8 scenario's for which separate thread groups are there.

* The Regrpoc module scenario's which we are targetting in this test script are - Sync Registration Packet, Sync And Upload Registration Packet, Get Packet Status, Get Packet External Status From Reg Id, Get External Status From Packet Id, Get Transaction Details From Reg Id, Lost Rid Search & Workflow Search.

* All the thread groups will run in parallel & if we don't want to run all of them we can disable the one which we don't want to run.

* Also for viewing the results or output of our test we have added certain listener test elements at the end of our test script which are - View Results Tree, Endpoint Level Report, Scenario Level Report.



### Setup points for Execution

* Auth Token Generation (Setup) - In this thread we are creating the authorization token values of Regpoc, Resident and Regproc - Using User Id which will be saved to a file in the Run Time Files in bin folder of JMeter.

* Packet Generation (Preparation) - In the preparation thread group we will basically create the context with the help of existing center id's, machine id's & user id's present in our current environment & we will read them through a file named contextDetails.csv

* Packet Generation (Execution) - Once the contexts are created we will use the same in the execution thread group where basically the packet generation happens & then the packet path gets stored in a file naming as Run Time Files in bin folder of JMeter.

* Sync Registration Packet - v2 (Setup) - To create encrypted data for generated packets(test data to registration processor sync API). Which will basically create a file with the encrypted data's for all the packets created.

***Note: The centers, machines and users should be onboarded in the system before using as part of contextDetails.csv.***



### Designing the workload model for performance test execution

* Calculation of number of users depending on Transactions per second (TPS) provided by client

* Applying little's law
	* Users = TPS * (SLA of transaction + think time + pacing)
	* TPS --> Transaction per second.

* For the realistic approach we can keep (Think time + Pacing) = 1 second for API testing
	* Calculating number of users for 10 TPS
		* Users= 10 X (SLA of transaction + 1)
		       = 10 X (1 + 1)
			   = 20


			   
### Usage of Constant Throughput timer to control Hits/sec from JMeter

* In order to control hits/ minute in JMeter, it is better to use Timer called Constant Throughput Timer.

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
		  

		  
### Description of the scenarios

* Sync Registration Packet : This scenario is used to sync the generated packet.

* Sync And Upload Registration Packet : This scenario upload's the generated packet.

* Get Packet Status :  Get the status of the packet using a rid post packet is uploaded.

* Get Packet External Status From Reg Id : Get the packet external status using a rid once the packet is uploaded.

* Get External Status From Packet Id : Get the packet external status once the packet is uploaded, using a packet id which will be present in the response of sync endpoint. 

* Get Transaction Details From Reg Id  : Get the transaction details of the packet using a rid once the packet is uploaded.

* Lost Rid Search : In admin portal this endpoint is used to search the lost rid's and get the details of lost rid search on the basis of given filters.

* Workflow Search : Workflow search on the basis of given filters.


### How to run JMeter DB script:-

* The JMeter DB script [Regproc Packets Processing Details From DB.jmx is used for getting the packet processing status of the packets uploaded to the packet receiver.

* It contains two thread groups 'RegProc PacketProcessing Status From DB' (for getting packet status) & 'RegProc ProcessedPackets Details' (All details of the packets uploaded).

* Set the parameters of the environment database like dbHost,dbPort,dbName,dbUser,dbPassword and also the values for start_time_throughput & delay in the test element named 'User Defined Variables'-
  1. dbHost -- host name of the registration processor database
  2. dbPort -- port number of the registration processor database
  3. dbName -- name of the registration processor database
  4. dbUser -- user name of the registration processor database
  5. dbPassword -- password of the registration processor database
  6. start_time_throughput-- the 'cr_dtimes' for the first packet to reach packet receiver
  7. delay-- delay value in milliseconds between each packets processing

* Execute the script for desired number of packets uploaded.

* Calculate the transaction times by running the [RegProcTransactionDataUtil](https://github.com/mosip/mosip-performance-tests-mt/tree/1.2.0/utilities/regproc_transactiondata_util_v2.2).

* Check below property in config.properties file located in src/main/resources of [RegProcTransactionDataUtil](https://github.com/mosip/mosip-performance-tests-mt/tree/1.2.0/utilities/regproc_transactiondata_util_v2.2)-
  1. ENVIRONMENT= environment name
  2. REGID_LOG_FILE= C:\\MOSIP_PT\\test1\\kafka_softHSM\\regid_file1.csv (Provide generated regids in regid_file.csv)
  3. EXCEL_FILE = C:\\MOSIP_PT\\test1\\kafka_softHSM\\regid_transaction_data.xlsx (Once above java utility is executed ,It will generate the transaction_times.xlsx which has all the transaction times of each stages of each packets)

