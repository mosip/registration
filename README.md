[![Maven Package upon a push](https://github.com/mosip/registration/actions/workflows/push-trigger.yml/badge.svg?branch=release-1.3.x)](https://github.com/mosip/registration/actions/workflows/push-trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.x&project=mosip_registration&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.3.x&id=mosip_registration)

# Registration Processor

## Overview

This repository contains source code and design documents for the MOSIP Registration Processor, a server-side module used to manage the ID lifecycle. The module exposes API endpoints and follows the SEDA architecture, where data flows through multiple stages.

[Overview of Registration Processor](https://docs.mosip.io/1.2.0/modules/registration-processor)

The front end UI application called Registration Client is available in a separate repository [here](https://github.com/mosip/registration-client)

The registration packet structure is available here : [Packet structure](https://docs.mosip.io/1.2.0/id-lifecycle-management/supporting-components/packet-manager/registration-packet-structure)

Vertx is the framework used to run the stages, and each stage operates as a Vertx component.

### Event Bus:
Registration processor stages are connected with eventbus. MOSIP supports two types of eventbus:
- Vertx Eventbus
- Kafka (default) : Provides persistence across restarts (more robust), throttling capacity and better debugging. 

Kafka offers certain advantages over Vertx eventbus hence it is recommended as the default eventbus mechanism. All events between stages pass through Kafka queues. There is a separate Kafka topic for each stage.

One of the power features is to enable throttling in the pipeline.  See [Throttling](docs/throttling.md)

### Distributes Cache:
Hazelcast is used as a distributed cache for packet manager.


## Features

-   Multi-stage processing pipeline (SEDA architecture) for scalable and asynchronous packet processing.

-   Packet validation and quality checks, including biometric quality assessment using external SDKs.

-   Biometric deduplication through integration with ABIS using middleware and messaging queues.

-   Manual adjudication support via queue-based integration with external adjudication systems.

-   Secure packet handling using Key Manager for encryption/decryption and Datashare for controlled data access.

-   Automated notifications via email/SMS using Notification Service.

-   High reliability with Kafka-based event bus, retries and configurable stage flows.
  
-   End-to-end ID lifecycle management supporting multiple registration flows such as :(New, Update, Correction, Child, Lost, Activate/Deactivate, Reprint, CRVS New, CRVS Death).
    - New
    - Update
    - Child
    - Correction
    - Lost 
    - Activate/deactivate 
    - Reprint
    - CRVS New
    - CRVS Death
    - The stage sequence against each flow refer [here](docs/flows.md)

## Services 
The Registration Processor organizes its processing flow into distinct stage groups. These stage groups are outlined below.
-   Group 1
    -   [Packet receiver](registration-processor/init/registration-processor-packet-receiver-stage) : Receives registration packets uploaded from the Registration Client and stores them securely for processing.
-   Group 2
    -   [Securezone notification](registration-processor/pre-processor/registration-processor-securezone-notification-stage) : Sends notifications to the SecureZone system following the initial sanity checks and assists in controlling the traffic flow to the next processing stages.
    -   [Quality classifier](registration-processor/pre-processor/registration-processor-quality-classifier-stage) : Checks biometric quality (fingerprint/iris/face) using a biometric SDK to ensure data meets required quality standards.
    -   [Message sender](registration-processor/registration-processor-message-sender-impl) : Responsible for sending notifications as the packet progresses through the stages.
-   Group 3
    -   [ABIS handler](registration-processor/core-processor/registration-processor-abis-handler-stage) : Responsible for preparing the request data to be sent to ABIS for deduplication.
    -   [ABIS middleware](registration-processor/core-processor/registration-processor-abis-middleware-stage) : Responsible for communicating with ABIS asynchronously.
    -   [Bio dedupe](registration-processor/core-processor/registration-processor-bio-dedupe-stage) : Performs biometric deduplication with the help of ABIS to ensure the uniqueness of biometric data.
    -   [Manual adjudication](registration-processor/core-processor/registration-processor-manual-adjudication-stage) : Asynchronously sends cases to the external manual adjudication system and receives decisions for applications requiring human review due to biometric duplicate issues.
-   Group 4
    -   [Biometric authentication](registration-processor/core-processor/registration-processor-biometric-authentication-stage) : Authenticates applicant, operator, introducer, or supervisor using biometric data via IDA Authentication Service.
    -   [Demo dedupe](registration-processor/core-processor/registration-processor-demo-dedupe-stage) : Performs demographic deduplication to detect duplicate records using demographic matching rules.
-   Group 5
    -   [CMD validator](registration-processor/pre-processor/registration-processor-cmd-validator-stage) : Validates center, machine, device (CMD) information captured in the registration packet.
    -   [Operator validator](registration-processor/pre-processor/registration-processor-operator-validator-stage) : Validates the operator’s identity and authorization for performing the enrollment.
    -   [Supervisor validator](registration-processor/pre-processor/registration-processor-supervisor-validator-stage) : Validates the supervisor’s identity and authentication for performing approval.
    -   [Introducer validator](registration-processor/pre-processor/registration-processor-introducer-validator-stage) : Validates introducer details for child enrollment.
    -   [Packet validator](registration-processor/pre-processor/registration-processor-packet-validator-stage) : Performs overall validation, including schema checks and verification of supervisor approval.
-   Group 6
    -   [Packet uploader](registration-processor/pre-processor/registration-processor-packet-uploader-stage) : Uploads sanitized registration packets for processing in MOSIP.
    -   [Packet classifier](registration-processor/pre-processor/registration-processor-packet-classifier-stage) : Classifies packets into categories, such as by age or exceptional biometrics, to ensure proper handling in subsequent stages.
    -   [Verification](registration-processor/core-processor/registration-processor-verification-stage) : Asynchronously sends cases to the external manual verification system and receives decisions for applications requiring human review of documents.
-   Group 7 – Final Processing
    -   [UIN generator](registration-processor/core-processor/registration-processor-uin-generator-stage) : Creates or updates a UIN draft using the applicant’s biometric and demographic data.
    -   [Biometric extraction](registration-processor/core-processor/registration-processor-biometric-extraction-stage) : Performs biometric extraction to extract biometric templates from raw biometric data for storage and matching.  
    -   [Finalization](registration-processor/core-processor/registration-processor-finalization-stage) : Updates the processing status and finalizes the UIN draft in IDREPO as a UIN entry.
    -   [Credential requestor](registration-processor/post-processor/registration-processor-credential-requestor-stage) : Responsible for generating credentials for different partners, formerly known as the Print Service.

The control and data flow in the stages is controlled by [Workflow engine](registration-processor/workflow-engine)

Supporting services:
- [Registration status service](registration-processor/init/registration-processor-registration-status-service): Responsible for sync the registration packets from field. 
- [Notification service](registration-processor/registration-processor-notification-service) : Responsible for sending email/SMS notifications as the packet progresses through the stages.
- [Transaction Service](registration-processor/init/registration-processor-registration-status-service) : Responsible for proving the transaction status of registration packets.


## Database

Before starting the local setup, execute the required SQL scripts to initialize the database.

All database SQL scripts are available in the [db scripts](./db_scripts) directory.

## Project Setup

The project can be set up in two ways:

1. [Local Setup (for Development or Contribution)](#local-setup-for-development-or-contribution)
2. [Local Setup with Docker (Easy Setup for Demos)](#local-setup-with-docker-easy-setup-for-demos)

### Prerequisites

Before you begin, ensure you have the following installed:

- **JDK**: 21.0.3
- **Maven**: 3.9.6
- **Docker**: Latest stable version

### Runtime Dependencies

- Add `kernel-auth-adapter.jar` to the classpath or include it as a Maven dependency

### Configuration

Registration processor module uses the following configuration files that are accessible in this [repository](https://github.com/mosip/mosip-config/tree/master).
Please refer to the required released tagged version for configuration.
- [application-default.properties](https://github.com/mosip/mosip-config/blob/master/application-default.properties) : Contains common configurations which are required across MOSIP modules.
- [registration-processor-default.properties](https://github.com/mosip/mosip-config/blob/master/registration-processor-default.properties) : Contains configurations required or to be overridden for Registration Processor module.

### Local Setup (for Development or Contribution)

1. Ensure the Config Server is running. For setup and startup instructions, refer to the  [MOSIP Config Server Setup Guide](https://docs.mosip.io/1.2.0/modules/registration-processor/registration-processor-developers-guide#environment-setup).
   
    **Note:** Verify that all required configuration properties (e.g., DB credentials, IAM credentials, URLs) are correctly updated with your environment-specific values.

2. Clone the repository:

```text
git clone <repo-url>
cd registration
```

3. Build the project:

```text
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

4. Start the application:
    - Click the Run button in your IDE, or
    - Run via command:
      ```text
      java -jar target/specific-service:<$version>.jar
      ```

### Local Setup with Docker (Easy Setup for Demos)

#### Option 1: Pull from Docker Hub

Recommended for users who want a quick, ready-to-use setup — testers, students, and external users.

Pull the latest pre-built images from Docker Hub using the following commands:

```text
docker pull mosipid/registration-processor-common-camel-bridge:1.3.0
...
```

#### Option 2: Build Docker Images Locally

Recommended for contributors or developers who want to modify or build the services from source.

1. Clone and build the project:

```text
git clone <repo-url>
cd registration
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

2. Navigate to each service directory and build the Docker image:

```text
cd registration/<service-directory>
docker build -t <service-name> .
```
 
#### Running the Services

Start each service using Docker:

```text
docker run -d -p <port>:<port> --name <service-name> <service-name>
```

#### Verify Installation

Check that all containers are running:

```text
docker ps
```

Access the services at `http://localhost:<port>` using the port mappings listed above.

## Documentation

### API Documentation:
API endpoints, base URL (Registration Processor), and mock server details are available via Stoplight
and Swagger documentation: API documentation is available [here](https://mosip.github.io/documentation/1.2.0/1.2.0.html)

## Sanbox deployment

To deploy Registration Processor services on Kubernetes cluster using Dockers refer to [Sandbox Deployment](https://docs.mosip.io/1.2.0/deploymentnew/v3-installation).

## Testing

Automated functional tests available in [DSL Automation](https://github.com/mosip/mosip-automation-tests)

## Contribution & Community

• To learn how you can contribute code to this application, [click here](https://docs.mosip.io/1.2.0/community/code-contributions).

• If you have questions or encounter issues, visit the [MOSIP Community](https://community.mosip.io/) for support.

• For any GitHub issues: [Report here](https://github.com/mosip/registration/issues)

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).
