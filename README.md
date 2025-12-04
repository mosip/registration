[![Maven Package upon a push](https://github.com/mosip/registration/actions/workflows/push-trigger.yml/badge.svg?branch=release-1.3.x)](https://github.com/mosip/registration/actions/workflows/push-trigger.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.x&project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.3.x&id=mosip_registration)

# Registration Processor

## Overview

This repository contains source code and design documents for the MOSIP Registration Processor, a server-side module used to manage the ID lifecycle. The module exposes API endpoints and follows the SEDA architecture, where data flows through multiple stages until a Unique Identification Number (UIN) is issued.

[Overview of Registration Processor](https://docs.mosip.io/1.2.0/modules/registration-processor)

The front end UI application called Registration Client is available in a separate repo [here](https://github.com/mosip/registration-client)

## Features

-   Multi-stage processing pipeline (SEDA architecture) for scalable and asynchronous packet processing.
    
-   End-to-end ID lifecycle management supporting multiple registration flows (New, Update, Correction, Child, Lost, Activate/Deactivate, Reprint).
    
-   Packet validation and quality checks, including biometric quality assessment using external SDKs.
    
-   Biometric deduplication through integration with ABIS using middleware and messaging queues.
    
-   Manual adjudication support via queue-based integration with external adjudication systems.
    
-   Secure packet handling using Key Manager for encryption/decryption and Datashare for controlled data access.
    
-   Integration with core MOSIP services such as ID Repository, Masterdata, Authentication (IDA), Credential Service, and Datasync.
    
-   Automated notifications via email/SMS using Notification Service.
    
-   Credential Requestor stage for generating print-ready credentials after UIN creation.

-   High reliability with Kafka-based event bus, retries, and configurable stage flows.

## Registration stages and pipeline (Purpose Summary)

Staged architecture:

-   Group 1 – Packet Receiving
    -   [Packet receiver](registration-processor/init/registration-processor-packet-receiver-stage)
    -   Receives registration packets uploaded from the Registration Client and stores them securely for processing.
-   Group 2 – Pre-Processing
    -   [Securezone notification](registration-processor/pre-processor/registration-processor-securezone-notification-stage)
    -   Notifies the Pre-Registration or Securezone system after the packet is received or validated.
    -   [Quality classifier](registration-processor/pre-processor/registration-processor-quality-classifier-stage)
    -   Checks biometric quality (fingerprint/iris/face) using a biometric SDK to ensure data meets required quality standards.
    -   [Message sender](registration-processor/registration-processor-message-sender-impl)
    -   Message Sender – Sends internal notifications for stage transitions.
-   Group 3 – Biometric Processing
    -   [ABIS handler](registration-processor/core-processor/registration-processor-abis-handler-stage)
    -   Coordinates with ABIS (Automated Biometric Identification System) for deduplication operations.
    -   [ABIS middleware](registration-processor/core-processor/registration-processor-abis-middleware-stage)
    -   Packages and sends biometric data to ABIS via queues (Kafka/ActiveMQ) and receives match results.
    -   [Bio dedupe](registration-processor/core-processor/registration-processor-bio-dedupe-stage)
    -   Ensures the applicant is not already enrolled by checking ABIS biometric matches, except when the 
        updated packet was the one originally enrolled.
    -   [Manual adjudication](registration-processor/core-processor/registration-processor-manual-adjudication-stage)
    -   Sends cases to the external manual adjudication system and receives decisions for applications requiring human review.
-   Group 4 – Authentication & Demographic Checks
    -   [Biometric authentication](registration-processor/core-processor/registration-processor-biometric-authentication-stage)
    -   Authenticates applicant, operator, introducer, or supervisor using biometric data via IDA Authentication Service.
    -   [Demo dedupe](registration-processor/core-processor/registration-processor-demo-dedupe-stage)
    -   Performs demographic deduplication to detect duplicate records using demographic matching rules.
-   Group 5 – Validators
    -   [CMD validator](registration-processor/pre-processor/registration-processor-cmd-validator-stage)
    -   Validates center, machine, device (CMD) information captured in the registration packet.
    -   [Operator validator](registration-processor/pre-processor/registration-processor-operator-validator-stage)
    -   Validates the operator’s identity and authorization for performing the enrollment.
    -   [Supervisor validator](registration-processor/pre-processor/registration-processor-supervisor-validator-stage)
    -   Validates the supervisor’s credentials for approvals or validations required in the workflow.
    -   [Introducer validator](registration-processor/pre-processor/registration-processor-introducer-validator-stage)
    -   Validates introducer details for child enrollment or cases requiring an introducer.
    -   [Packet validator](registration-processor/pre-processor/registration-processor-packet-validator-stage)
    -   Performs overall Validations data integrity, schema, and compliance.
-   Group 6 – Packet Processing
    -   [Packet uploader](registration-processor/pre-processor/registration-processor-packet-uploader-stage)
    -   Uploads validated packets to Packet Manager or secure storage for long-term retention.
    -   [Packet classifier](registration-processor/pre-processor/registration-processor-packet-classifier-stage)
    -   Classifies packets into categories (e.g., New, Update, Child, Correction) to determine downstream workflow.
    -   [Verification](registration-processor/core-processor/registration-processor-verification-stage)
    -   Validates demographic and biometric information as per policy for authenticity before UIN creation.
-   Group 7 – Final Processing
    -   [UIN generator](registration-processor/core-processor/registration-processor-uin-generator-stage)
    -   Generates a Unique Identification Number (UIN) for the applicant after validation and dedupe are successful.
    -   [Biometric extraction](registration-processor/core-processor/registration-processor-biometric-extraction-stage)
    -   Extracts biometric data from the packet and prepares it for storage in ID Repository.
    -   [Finalization](registration-processor/core-processor/registration-processor-finalization-stage)
    -   Updates the final processing status, writes records to ID Repository, and completes the registration lifecycle.
    -   [Credential requestor](registration-processor/post-processor/registration-processor-credential-requestor-stage)
    -   Requests the Credential Service to create printable credentials (formerly the Print Stage, Sends credential creation request.).

The control and data flow in the stages is controlled by [Workflow engine](registration-processor/workflow-engine/)

### External / Dependent Services:

-   Datasync Service – Notifies Pre-Registration after packet validation.

-   D Repository (ID Repo) – Stores/updates identity data; manages UIN lifecycle.

-   Masterdata Service – Validates center, machine, operator, and location master data.

-   IDA Authentication Service – Authenticates applicant, operator, introducer, supervisor.

-   Credential Service – Generates digital/print credentials after UIN creation.

-   Notification Service – Sends SMS/email updates to applicants.

-   Datashare Service – Generates secure URLs for partner data access.

-   Key Manager Service – Manages encryption/decryption of packets and keys.

-   Virus Scanner Service – Scans packets for malware before ingestion.

-   ABIS (External) – Performs biometric deduplication.

-   Manual Adjudication System (External) – Handles human verification for ambiguous cases.

-   Packet Server – Stores and retrieves registration packets.

-   Registration Status Service – Tracks and updates the real-time processing status of packets.

-   Transaction Service – Manages transaction logs and audit trails for each processing stage.

### Registration flows

An overview of various enrollment scenarios (or flows) is described in [ID Lifecycle Management](https://docs.mosip.io/1.2.0/id-lifecycle-management). Registration Processor recognises the following flows:

-   New
-   Update
-   Child
-   Correction
-   Lost
-   Activate/deactivate
-   Reprint

The stage sequence against each flow refer [here](docs/flows.md)

## Local Setup

The project requires JDK 21.0.3 and mvn version 3.9.6

1.  To build jars:

    ```
    $ cd registration
    $ mvn clean install 
    ```

2.  To skip JUnit tests and Java Docs:

    ```
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true
    ```

There are two primary ways to set up the Registration Processor locally:

-   Using Docker Compose (Recommended for quick setup with existing images)

-   By Building Docker Images Locally (For customizing your build)

#### Pre-requisites

Before setting up the project locally, ensure you have the following installed and configured:

-   JDK version 21.0.3

-   Maven version 3.9.6

-   Docker (if using Docker Compose or building Docker images)

-   Kafka (for message queue and event bus communication)

-   Database (refer to the Database Setup section for DB setup)

#### Database Configuration:

-   Database URL:
Take the DB_URL from the application.properties file under the mosip-config section to match your environment's database URL.

-   Username:
Default: root 

-   Password:
Default: password

-   configure local DB (Postgres using above properties)
-   See [DB guide](db_scripts/README.md)

#### Local Setup Using Docker Compose

Clone the repository:

    ```
    $ git clone <repository_url>
    $ cd <repo_name>
    ```


Set up Docker Compose:

Ensure yml is in the root directory of the project.

Configure any environment variables in the .env file as required (e.g., database credentials, Kafka broker URLs).

Run Docker Compose:

    ```
    $ docker-compose up --build
    ```

#### Local Setup by Building Docker Image Locally

Clone the repository:

    ```
    $ git clone <repository_url>
    $ cd <repo_name>
    ```


Build the Docker image for the service:

    ```
    $ docker build -t <image_name> .
    ```


Run the container:

    ```
    $ docker run -d -p <port>:<port> <image_name>
    ```

## Deployment

### Documentation

#### API Documentation:
API endpoints, base URL (Registration Processor), and mock server details are available via Stoplight 
and Swagger documentation: API documentation is available [here](https://mosip.github.io/documentation/1.2.0/1.2.0.html)

#### Product Documentation:
To know more about Registration Processor in the perspective of functional and use cases you can refer to our main 
document: https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-processor



### Registration processor in sandbox

To deploy Registration Processor services on Kubernetes cluster using Dockers refer to [Sandbox Deployment](https://docs.mosip.io/1.2.0/deploymentnew/v3-installation).

## Contribution & Community:

We welcome contributions from everyone!

Check here https://docs.inji.io/readme/contribution/code-contribution to learn how you can contribute code to this application.

If you have any questions or run into issues while trying out the application, feel free to post them in the https://community.mosip.io/ — we’ll be happy to help you out.

Github issues


## Vertx

Vertx is a framework for stages. Stages run as Vertx.

## Kafka

Regprocessor stages are connected with eventbus. MOSIP supports two types of eventbus:

-   Vertx Eventbus
-   Kafka (default) - provides persistence across restarts (more robust), throttling capacity, better debugging

Kafka offers certain advantages over Vertx eventbus hence it is recommended as the default eventbus mechanism. All events between stages pass through Kafka queues. There is a separate Kafka topic for each stage.

One of the power features is to enable throttling in the pipeline. See [Throttling](docs/throttling.md)

## Hazelcast

Distributed cache - for packetmanager


## Registration Packet Structure

[Packetmanager](https://docs.mosip.io/1.2.0/modules/packet-manager)

## Configuration

Refer to the [configuration guide](docs/configuration.md).

## Test

Automated functional tests available in [DSL Automation](https://github.com/mosip/mosip-automation-tests)

## License

This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).