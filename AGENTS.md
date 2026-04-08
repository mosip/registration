# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Build & Test

```bash
# Full build (skip javadoc and GPG signing)
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Build a single module
mvn clean install -pl registration-processor/<module-path> -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Run tests for a module
mvn test -pl registration-processor/<module-path>

# Run a specific test class
mvn test -pl registration-processor/<module-path> -Dtest=MyTestClass

# Build Docker image for a service
cd registration-processor/<service-directory>
docker build -t <service-name> .
```

**Requirements:** JDK 21.0.3, Maven 3.9.6

## Architecture

### SEDA Pipeline

Registration Processor implements a **Staged Event-Driven Architecture (SEDA)**. Registration packets submitted by the Registration Client flow through independent processing stages connected by Kafka topics. Each stage is a **Vert.x Verticle** that consumes from one Kafka topic and publishes to the next.

The **Workflow Engine** (Apache Camel, XML DSL) routes packets through stage sequences based on the registration flow type (New, Update, Lost, Child, CRVS, etc.). Flow definitions are loaded from the external config server, not hardcoded.

### Stage Groups

Stages are bundled into deployment groups (1–7), each running as a single JVM using the `mosip-stage-executor` pattern:

| Group | Key Stages |
|-------|-----------|
| 1 | Packet Receiver |
| 2 | Securezone Notification, Quality Classifier, Message Sender |
| 3 | ABIS Handler, ABIS Middleware, Bio Dedupe, Manual Adjudication |
| 4 | Biometric Authentication, Demo Dedupe |
| 5 | CMD/Operator/Supervisor/Introducer Validators, Packet Validator |
| 6 | Packet Uploader, Packet Classifier, Verification |
| 7 | UIN Generator, Biometric Extraction, Finalization, Credential Requestor |

Supporting services: Registration Status Service, Notification Service, Transaction Service.

### Module Layout

```
registration-processor/
  init/           - Packet Receiver, Registration Status Service
  pre-processor/  - Validators, Uploader, Classifier, Securezone
  core-processor/ - ABIS, deduplication, adjudication, UIN generation
  post-processor/ - Credential Requestor
  workflow-engine/            - Camel-based routing
  registration-processor-message-sender-impl/
  registration-processor-notification-service/
  common/         - Shared utilities, REST clients, DTOs
  mosip-stage-executor/ - Base for running multiple stages in one JVM
```

### Key Patterns

- **Stage base class:** All stages extend `MosipVerticleAPIManager` (Vert.x)
- **Stage executor:** Multiple stages share a JVM; the active group is selected via `-Dstage-group-name=stage-group-N` at startup
- **External config:** All runtime properties come from Spring Cloud Config Server (mosip-config repo). DB credentials, IAM URLs, Camel flow XMLs, throttling limits — nothing is bundled in the jar
- **Distributed cache:** Hazelcast used by the Packet Manager layer
- **Authentication:** `kernel-auth-adapter.jar` must be on the classpath; it intercepts outbound REST calls

### Running a Stage Locally

```bash
java -Dapplication.base.url=http://localhost:8090 \
  -Dspring.profiles.active=mz \
  -Dspring.cloud.config.uri=http://localhost:51000/config \
  -Dspring.cloud.config.label=master \
  -Dstage-group-name=stage-group-1 \
  -jar registration-processor-stage-group-1-<version>.jar
```

A running Config Server is required before starting any service. Initialize the database using scripts in `db_scripts/mosip_regprc/` against PostgreSQL before first run.

### External Integrations

- **ABIS** – Biometric deduplication via async message queues (ActiveMQ)
- **IDA** – Biometric authentication REST calls
- **Key Manager** – Packet encryption/decryption
- **Datashare** – Controlled packet data access
- **Config Server** – All runtime properties (see [mosip-config](https://github.com/mosip/mosip-config))
