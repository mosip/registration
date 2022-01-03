
[![Build Status](https://travis-ci.org/mosip/registration.svg?branch=master)](https://travis-ci.org/mosip/registration)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?id=mosip_registration)
[![Join the chat at https://gitter.im/mosip-community/registration](https://badges.gitter.im/mosip-community/registration.svg)](https://gitter.im/mosip-community/registration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Registration Processor
This repository contains source code and design documents for MOSIP Registration Processor which is the server-side module to manage ID lifecycle.  The modules exposes API endpoints.  

For an overview of registration processor and its role in ID lifecycle management, refer [here](https://docs.mosip.io)

The front end UI registration application is available in a separate repo [here](https://github.com/mosip/registration-client/tree/develop)

## Registration stages and pipeline

Staged architecture:

  * Group 1 stages 
      * Packet receiver  
  * Group 2 stages
      * Securezone notification
      * Quality classifier
      * Message sender
  * Group 3 stages
      * ABIS handler
      * ABIS middleware 
      * Bio dedupe
      * Manual adjudication
 * Group 4 stages
      * Biometric authentication
      * Demo dedupe
 * Group 5 stages
      * CMD validator
      * Operator validator
      * Supervisor validator
      * Introducer validator
      * Packet validator
 * Group 6 stages
      * Packet uploader
      * [Packet classifier](registration-processor/pre-processor/registration-processor-packet-classifier-stage/README.md)
      * Verification
 * Group 7 stages
      * UIN generator
      * Biometric extraction
      * Finalization
      * Printing

The control and data flow in the stages is controlled by [Workflow engine](#workflow-engine).

Other services:
  * Packet Server
  * Registration status service
  * Notification service
  * Transaction service:  Used by Admin module

## Workflow engine
Consists of the following elements:
*  Camel bridge: For routing packet to different stages based on registration [flows](docs/flows.md).
*  Workflow manager service:  Enables certain workflow functions via exposed APIs:
    *  Pause: Pause processing of packets based on rules
    *  Resume: Resume proessing of packets  
    *  Request for additional information from user (like a new document) and continue registration as CORRECTION packet.
    * Reprocessor

The work flow can be controlled with Admin portal

# Registration flows

* New 
* Update
* Child
* Correction 
* Lost 
* Activate/deactivate
* Reprint

The stage sequence against each flow refer [here](docs/flows.md)
 
# Vertx
Vertx is a framework for stages. Stages run as Vertx.

# Kafka
Regprocessor stages are connected with eventbus.  MOSIP supports two types of eventbus: 
 - Vertx Eventbus 
 - Kafka (default) - provides persistence across restarts (more robust), throttling capacity, better debugging 

Kafka offers certain advantages over Vertx eventbus hence it is recommended as the default eventbus mechanism. All events between stages pass through Kafka queues. There is a separate Kafka topic for each stage.

One of the power features is to enable throttling in the pipeline.  See details on throttling [here](docs/throttling.md)

# Hazelcast 
Distributed cache - for packetmanager

# Database

See [DB guide](db_scripts/README.md)

# Registration Packet Structure
[Packetmanager](https://github.com/mosip/packet-manager/tree/develop/README.md)

# Build
The project requires JDK 1.11. 
1. To build jars:
    ```
    $ cd registration
    $ mvn clean install 
    ```
1. To skip JUnit tests and Java Docs:
    ```
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true
    ```
1. To build Docker for a service:
    ```
    $ cd <service folder>
    $ docker build -f Dockerfile
    ```

# Deploy

## PreReg in Sandbox
To deploy Registration on Kubernetes cluster using Dockers refer to [mosip-infra](https://github.com/mosip/mosip-infra/tree/1.2.0_v3/deployment/v3)

## Developer

1. As a developer, to run a service jar individually:
    ```
    `java -Dspring.profiles.active=<profile> -Dspring.cloud.config.uri=<config-url> -Dspring.cloud.config.label=<config-label> -jar <jar-name>.jar`
    ```
    Example:  
        _profile_: `env` (extension used on configuration property files*)    
        _config_label_: `master` (git branch of config repo*)  
        _config-url_: `http://localhost:51000` (Url of the config server*)  
	
	\* Refer to [kernel-config-server](https://github.com/mosip/commons/tree/master/kernel/kernel-config-server) for details


1. Note that you will have to run the dependent services like kernel-config-server to run any service successfully.
    
# Dependencies
Registration module depends on the following services:


# Configuration
Refer to the [configuration guide](docs/configuration.md).

# Test
Automated functaionl tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests)

# APIs
API documentation available on Wiki: [Registration APIs](https://github.com/mosip/documentation/wiki/Registration-APIs)

# License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)

Refer to README in respective folders for details.

