### High level Approach:
* Simulate load to a particular application to understand the response time behaviour and find the bottlenecks areas
	* Run all the dependent services so the bottleneck on the test application is exposed while profiling
* There are 3 types of thread groups
	1. Setup - run once for the prerequesites on a new environment. Prefixed with A01,A02 ...
        
	2. Preparation - Run once before "Execution" threadgroup. Prefixed with P01,P02 ...
	3. Execution - Actual load test will be performed by this thread group
* Thread Group names are prefixed with A0# for Setup, P0# for Preparation and S0# for Execution.
* "A00 Auth Token Generation (Setup)" generates authentication tokens for other threads. It needs to be run before  running other thread groups. 
* Execution thread correspond to Performance Test Scenarios. Preparation threads are numbered according to the Test Scenario it supports. E.g. P03 needs to run to be able to prepare for execution of S03. 

### QA Acceptance Criteria:
* Readme document should have sufficient information for someone with Jmeter expertise to run the load testing for a particular module
* All environment specific modification should be done only in the user defined variables
* All preparation thread groups should be able to run in sequence that is required to run execution for 15 min
* All execution thread groups should be able to run in sequence for 15 min each
* Multiple preparation thread groups should be able to run in parallel that is required to run executions for 30 min
* Multiple execution thread groups should be able to run in parallel for 30 min
* The sample count on prepration should be always less than the execution sample count except for the API endpoints readme explictly says otherwise.
* Load can be very less but atleast 3 users should be targeted for execution and preparation thread groups (Unless prepration thread group explictly says in readme that it has to run with single thread)
	
### Contains
* This Repository contains performance test scripts, test data, utilities, summary reports of below MOSIP modules, 
    1. Pre-registration (UI and batch Jobs)
    2. Registration Processor
    3. ID Repository
    4. ID Authentication
    5. Kernel
	5. Resident services
* Open source Tools used,
    1. [Apache JMeter](https://jmeter.apache.org/)
    2. [Glowroot](https://glowroot.org/)

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module.
* Start JMeter by running the jmeter.bat file for Windows or jmeter file for Unix. 
* Validate the scripts for one user.
* Execute a dry run for 10 min.
* Execute performance run with various loads in order to achieve targeted NFR's.

### Apache JMeter scripts module wise
* JMeter scripts for each module are available in the respective scripts folders.
	* [Pre-registration scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/pre-registration/scripts).
	* [Registration Processor 	scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/registration/registrationprocessor/scripts).
	* [ID Authentication scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/id-authentication/scripts).
	* [ID Repository scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/commons/id-repository/scripts).
	* [Kernel scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/commons/kernel/scripts).
	* [Resident Services scripts](https://github.com/mosip/mosip-performance-tests-mt/tree/master/resident-services/scripts)
* [JAVA utilities](https://github.com/mosip/mosip-performance-tests-mt/tree/master/utilities).

### Execution steps for module wise
* [Pre-registration execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/pre-registration/README.md)
* [Registration Processor execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/registration/registrationprocessor/README.md)
* [ID Authentication execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/id-authentication/README.md)
* [ID Repository execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/commons/id-repository/README.md)
* [Kernel execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/commons/kernel/README.md)
* [Resident Services execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/resident-services/README.md)
* [JAVA utilities execution steps](https://github.com/mosip/mosip-performance-tests-mt/blob/master/utilities/README.md)

### Documentation

MOSIP documentation is available on [Wiki](https://github.com/mosip/documentation/wiki)

### License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)
