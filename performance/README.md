### High level Approach:
* Simulate load by running the jmeter script for a particular application to understand the response time behaviour and find the bottlenecks areas
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
    1. Registration Processor
* Open source Tools used,
    1. [Apache JMeter](https://jmeter.apache.org/)

### How to run performance scripts using Apache JMeter tool
* Download Apache JMeter from https://jmeter.apache.org/download_jmeter.cgi
* Download scripts for the required module.
* Start JMeter by running the jmeter.bat file for Windows or jmeter file for Unix. 
* Validate the scripts for one user.
* Execute a dry run for 10 min.
* Execute performance run with various loads in order to achieve targeted NFR's.

### Documentation

MOSIP documentation is available on [Wiki](https://github.com/mosip/documentation/wiki)

### License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)
