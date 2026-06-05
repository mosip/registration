# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project Overview

This is the **MOSIP Registration Processor** — the server-side module that manages the ID lifecycle for MOSIP (Modular Open Source Identity Platform). It receives registration packets from the Registration Client (a separate repository), validates them, performs deduplication, and ultimately issues or updates a Unique Identification Number (UIN).

The processor follows a **SEDA (Staged Event-Driven Architecture)** where packets flow asynchronously through multiple stages, each running as a Vert.x verticle, connected via Kafka topics.

## Build Commands

All commands run from the `registration-processor/` directory (the Maven parent):

```bash
# Full build (skip Javadoc and GPG signing for local dev)
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Build a specific module only (run from registration-processor/)
mvn clean install -pl <module-path> -am -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Or target a module from the repo root
mvn test -pl registration-processor/<module-path>
mvn test -pl registration-processor/<module-path> -Dtest=MyTestClass

# Generate code coverage report (output: target/site/jacoco/index.html)
mvn clean verify

# SonarQube static analysis
mvn clean verify -Psonar
```

## Running a Stage Group

Each deployable unit is a **stage group** — a Spring Boot fat JAR that bundles `mosip-stage-executor` with one or more stage dependencies. The entry point for every stage group is `MosipStageExecutorApplication`.

```bash
java \
  -Dapplication.base.url=http://localhost:8090 \
  -Dspring.profiles.active=mz \
  -Dspring.cloud.config.uri=http://localhost:51000/config \
  -Dspring.cloud.config.label=master \
  -Dstage-group-name=stage-group-1 \
  -jar registration-processor-stage-group-1-<version>.jar
```

A running **Spring Cloud Config Server** on port `51000` is required before starting any service. Configuration files (`application-default.properties`, `registration-processor-default.properties`) live in the [mosip-config](https://github.com/mosip/mosip-config) repository and must have correct DB, IAM, and Kafka values for your environment. All runtime properties (DB credentials, IAM URLs, Camel flow XMLs, throttling limits) are fetched from the config server at startup — nothing is bundled in the JAR.

`kernel-auth-adapter.jar` must be on the classpath (or included as a Maven dependency); it intercepts outbound REST calls to add MOSIP authentication headers.

Initialize the database using scripts in `db_scripts/mosip_regprc/` against PostgreSQL before the first run.

## Architecture


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

### Key Technologies

| Concern | Technology |
|---|---|
| Stage runtime | Vert.x (verticles) |
| Inter-stage event bus | Kafka (default) or Vert.x EventBus |
| Stage routing / orchestration | Apache Camel (XML routes from config server) |
| Distributed cache | Hazelcast |
| Configuration | Spring Cloud Config |
| REST integration | Spring Boot + WebClient |

### Stage Implementation Pattern

Each stage is a Vert.x verticle that extends `MosipRouter` (from `registration-processor-core`). When bundled into a stage group:
- The stage's `@SpringBootApplication` is removed; the class becomes `@Configuration` with `@ComponentScans`.
- `getPropertyPrefix()` is implemented instead of `getPort()` / `getEventbusPort()`.
- `MosipStageExecutorApplication` auto-discovers stage beans via `mosip.regproc.stage-groups.stage-beans-base-packages.default` (base packages: `io.mosip.registration.processor`, `io.mosip.registrationprocessor`).

### Registration Flows

The following packet types are supported (routing differs per type — see Camel XML routes):
- **New**, **Update**, **Correction**, **Child**, **Lost**
- **Activate / Deactivate**, **Reprint**
- **CRVS New**, **CRVS Death**

### External Service Dependencies

Stages make synchronous REST calls to other MOSIP services:
- **Key Manager** — packet encryption/decryption
- **IDA (Identity Auth Service)** — biometric authentication
- **IDRepo** — UIN read/write
- **Datashare** — controlled biometric data sharing with ABIS
- **Packet Manager** — packet read/write abstraction (uses Hazelcast cache)
- **ABIS** — external Automated Biometric Identification System (async via ActiveMQ/Kafka queues)

## Module Map

```
registration-processor/
├── registration-processor-core/          # Shared abstractions: MosipRouter, MessageDTO, EventBus interfaces, constants, exceptions
├── registration-processor-rest-client/   # Spring RestTemplate/WebClient wrappers for MOSIP service calls
├── registration-processor-packet-manager/ # Packet read/write API (Hazelcast-cached)
├── registration-processor-common-camel-bridge/ # Camel bridge — loads XML routes, dispatches to stage Kafka topics
├── mosip-stage-executor/                 # Stage group runtime; discovers and wires stage beans
├── init/                                 # Group 1: Packet Receiver, Registration Status Service, DMZ Packet Server
├── pre-processor/                        # Groups 2, 5, 6: Securezone, Quality Classifier, validators, uploader, classifier
├── core-processor/                       # Groups 3, 4, 7: dedupe (bio/demo), ABIS, auth, manual adjudication, UIN generator, finalization
├── post-processor/                       # Group 7 tail: Message Sender, Credential Requestor, Transaction Service
├── stage-groups/                         # Deployable fat-JARs (stage-group-1 through stage-group-7)
├── workflow-engine/                      # Workflow Manager Service (pause/resume/additional-info APIs) + Reprocessor
├── registration-processor-notification-service/
├── registration-processor-bio-dedupe-service-impl/
└── registration-processor-registration-status-service-impl/
```

## Database

SQL initialization scripts are in `db_scripts/mosip_regprc/`. Upgrade/migration scripts are in `db_upgrade_scripts/`. Supported databases: PostgreSQL (production), MySQL, H2 (tests).

## CI/CD

GitHub Actions (`.github/workflows/push-trigger.yml`) builds on push to `develop`, `master`, and `release-1*` branches:
1. Maven build via reusable `mosip/kattu` workflow (Java 21)
2. Publish JARs to Maven Central (OSSRH) — skipped for PRs and master
3. Build 16 Docker images in parallel and push to Docker Hub
4. SonarCloud analysis (project key: `mosip_registration`)

Kubernetes deployment uses Helm charts in `helm/` (one chart per stage group and supporting service).

## Key Configuration Properties

Runtime configuration is entirely externalized to the Spring Cloud Config Server. The two main files to understand are:
- `application-default.properties` — MOSIP-wide settings (DB, IAM, service URLs)
- `registration-processor-default.properties` — registration-processor-specific overrides

Camel route XMLs are also fetched from the config server at startup (URL pattern: `${spring.cloud.config.uri}/${camel.bridge}/${spring.profiles.active}/${spring.cloud.config.label}/`).

## Testing Notes

- Unit tests use JUnit 4, Mockito, and PowerMock.
- The Surefire plugin is configured with `--add-opens` JVM args for Java 21 module compatibility.
- SonarQube excludes DTOs, entities, config classes, and exception handlers from coverage requirements; the target is ~70% for business logic.
