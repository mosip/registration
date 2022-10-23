[![Maven Package upon a push](https://github.com/mosip/registration/actions/workflows/push_trigger.yml/badge.svg?branch=release-1.2.0.1)](https://github.com/mosip/registration/actions/workflows/push_trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.2.0.1&project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.2.0.1&id=mosip_registration)

# Registration Processor

## Overview
This repository contains source code and design documents for MOSIP Registration Processor which is the server-side module to manage ID lifecycle.  The modules exposes API endpoints.  

[Overview of Registration Processor](https://docs.mosip.io/1.2.0/modules/registration-processor)

The front end UI application called Registration Client is available in a separate repo [here](https://github.com/mosip/registration-client)

## Registration stages and pipeline

Staged architecture:

  * Group 1 stages 
      * [Packet receiver](registration-processor/init/registration-processor-packet-receiver-stage)
  * Group 2 stages
      * [Securezone notification](registration-processor/pre-processor/registration-processor-securezone-notification-stage)
      * [Quality classifier](registration-processor/pre-processor/registration-processor-quality-classifier-stage)
      * [Message sender]()
  * Group 3 stages
      * [ABIS handler](registration-processor/core-processor/registration-processor-abis-handler-stage)
      * [ABIS middleware ](registration-processor/core-processor/registration-processor-abis-middleware-stage)
      * [Bio dedupe](registration-processor/core-processor/registration-processor-bio-dedupe-stage)
      * [Manual adjudication](registration-processor/core-processor/registration-processor-manual-adjudication-stage)
 * Group 4 stages
      * [Biometric authentication](registration-processor/core-processor/registration-processor-biometric-authentication-stage)
      * [Demo dedupe](registration-processor/core-processor/registration-processor-demo-dedupe-stage)
 * Group 5 stages
      * [CMD validator](registration-processor/pre-processor/registration-processor-cmd-validator-stage)
      * [Operator validator](registration-processor/pre-processor/registration-processor-operator-validator-stage)
      * [Supervisor validator](registration-processor/pre-processor/registration-processor-supervisor-validator-stage)
      * [Introducer validator](registration-processor/pre-processor/registration-processor-introducer-validator-stage)
      * [Packet validator](registration-processor/pre-processor/registration-processor-packet-validator-stage)
 * Group 6 stages
      * [Packet uploader](registration-processor/pre-processor/registration-processor-packet-uploader-stage)
      * [Packet classifier](registration-processor/pre-processor/registration-processor-packet-classifier-stage)
      * Verification
 * Group 7 stages
      * UIN generator
      * Biometric extraction
      * Finalization
      * Printing

The control and data flow in the stages is controlled by [Workflow engine](registration-processor/workflow-engine/)

Other services:
  * Packet Server
  * Registration status service
  * Notification service
  * Transaction service

### Registration flows
An overview of various enrollment scenarious (or flows) is described in [ID Lifecycle Management](https://docs.mosip.io/1.2.0/id-lifecycle-management).  Registration Processor recognises the following flows:

* New 
* Update
* Child
* Correction 
* Lost 
* Activate/deactivate
* Reprint

The stage sequence against each flow refer [here](docs/flows.md)
 
## Vertx
Vertx is a framework for stages. Stages run as Vertx.

## Kafka
Regprocessor stages are connected with eventbus.  MOSIP supports two types of eventbus: 
 - Vertx Eventbus 
 - Kafka (default) - provides persistence across restarts (more robust), throttling capacity, better debugging 

Kafka offers certain advantages over Vertx eventbus hence it is recommended as the default eventbus mechanism. All events between stages pass through Kafka queues. There is a separate Kafka topic for each stage.

One of the power features is to enable throttling in the pipeline.  See [Throttling](docs/throttling.md)

## Hazelcast 
Distributed cache - for packetmanager

## Database
See [DB guide](db_scripts/README.md)

## Registration Packet Structure
[Packetmanager](https://docs.mosip.io/1.2.0/modules/packet-manager)

## Build & run (for developers)
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

## Deploy

### Registration processor in sandbox
To deploy Registration Processor services on Kubernetes cluster using Dockers refer to [Sandbox Deployment](https://docs.mosip.io/1.2.0/deployment/sandbox-deployment).

## Configuration
Refer to the [configuration guide](docs/configuration.md).

## Test
Automated functional tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests)

## APIs
API documentation is available [here](https://docs.mosip.io/1.2.0/api)

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).

