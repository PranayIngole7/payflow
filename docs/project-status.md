# PayFlow — Project Progress Report

Date: 31 August **2026** Project: PayFlow Developer: Pranay Ingole Current Phase: Phase 3 — Development Setup Status: ✅ **COMPLETE**

## Project Objective

PayFlow is a production-style digital payment/wallet backend designed to demonstrate strong Java/Spring Boot backend skills, including:

Java backend development **REST** APIs PostgreSQL Redis Kafka Event-driven architecture Transaction and ledger design Idempotency Security Testing Observability AI integration

The project will be built as a modular monolith first, with clear domain boundaries and production-oriented architecture.

## Completed Phases

Phase 1 — Product Requirements ✅

Defined the business foundation of PayFlow:

Actors Functional requirements Non-functional requirements Payment rules Wallet rules AI requirements Failure scenarios **MVP** scope Acceptance criteria Phase 2 — Domain Modeling ✅

Established the core financial domain:

User ↓ Wallet ↓ Payment ↓ Transaction ↓ ### Ledger Entry

Important principle:

Financial business concepts and rules should not be changed casually. Architectural changes will be documented through ADRs.

Phase 3 — Development Setup ✅

Completed the local development environment.

## Development Environment

Java Java: 25.0.4 **JDK**: /usr/lib/jvm/java-25-openjdk-amd64

Verified:

java -version javac -version

Maven Apache Maven: 3.9.16

Project uses the Maven Wrapper:

./mvnw

### Spring Boot

Spring Boot: 4.1.1

Initial project successfully generated.

Verified:

./mvnw clean test

Result:

**BUILD** **SUCCESS** Tests run: 1 Failures: 0 Errors: 0

Application successfully started on:

[http://localhost:**8080**](http://localhost:**8080**)

Actuator health verified:

curl [http://localhost:**8080**/actuator/health](http://localhost:**8080**/actuator/health)

Result:

{*groups*:[*liveness*,*readiness*],*status*:*UP*}

## Git/GitHub

Repository:

payflow

Local path:

/home/dell/payflow

Default branch:

main

Remote:

origin → GitHub PayFlow repository

Git identity configured.

Repository is currently synchronized with GitHub.

## Git Checkpoints

Commit 1 f5ac162 chore: initialize PayFlow repository

Commit 2 d45efd6 chore: bootstrap Spring Boot application

Commit 3 7bc0b16 chore: add PostgreSQL development infrastructure

Commit 4 c403810 chore: add Redis development infrastructure

Commit 5 88f8f49 chore: add Kafka development infrastructure

Current repository state:

main == origin/main working tree clean

## PostgreSQL

Version:

PostgreSQL 16.15

Docker container:

payflow-postgres

Port:

**5432**

Database:

payflow

User:

payflow

Docker volume:

payflow_payflow_postgres_data

Health:

healthy

Connection verified using:

docker exec -it payflow-postgres psql -U payflow -d payflow

Database and user successfully verified.

7. Redis

Version:

Redis 7

Docker container:

payflow-redis

Port:

**6379**

Health:

healthy

Verified:

**PING** → **PONG** **SET**   → successful **GET**   → successful **DEL**   → successful

Important architecture rule:

Redis is **NOT** the source of truth for financial data.

Redis will later be used for appropriate use cases such as:

caching idempotency support rate limiting temporary state distributed coordination where appropriate

PostgreSQL remains authoritative for financial information.

8. Kafka

Version:

Apache Kafka 4.0.0

Docker container:

payflow-kafka

Port:

**9092**

Architecture:

KRaft mode

ZooKeeper is **NOT** being used.

Kafka health:

healthy

Kafka was tested end-to-end.

Temporary topic:

payflow-test

Configuration:

Partitions: 3 Replication factor: 1

Verified:

Producer ↓ Kafka ↓ Topic ↓ Consumer

Test message:

Hello PayFlow

Successfully consumed.

Temporary test topic was deleted afterward.

Kafka currently contains only the internal:

__consumer_offsets

topic.

Important architecture rule:

Kafka is for asynchronous/event-driven communication. It does not replace PostgreSQL as the financial source of truth.

## Current Docker Infrastructure

Current Compose services:

payflow-postgres   PostgreSQL 16   :**5432**   healthy payflow-redis      Redis 7         :**6379**   healthy payflow-kafka      Kafka 4.0       :**9092**   healthy

All services are connected through:

payflow_default

Compose file:

compose.yaml

## Current Architecture Direction

PayFlow will initially be implemented as a:

Production-style Modular Monolith

rather than immediately creating microservices.

Expected high-level structure:

PayFlow │ ├── User ├── Wallet ├── Payment ├── Transaction ├── Ledger ├── Notification ├── AI └── Infrastructure

Exact package/module boundaries will be finalized during Phase 4.

## Important Engineering Principles

Throughout the project we will follow these principles:

PostgreSQL is the financial source of truth. Ledger entries represent authoritative financial history. Redis is not authoritative financial storage. Kafka events must be designed around actual business requirements. Payment operations must consider idempotency. Financial operations require careful transaction boundaries. Domain rules belong in appropriate domain/application boundaries rather than controllers. Architectural changes should be documented through ADRs. Infrastructure will be verified independently before application integration. We will prefer production-quality design over unnecessary complexity. ## Next Execution Plan Phase 4 — Architecture ⏳ **NEXT**

We will design:

Modular monolith structure Package/module boundaries Dependency rules Controller layer Application/service layer Domain layer Infrastructure layer Aggregate boundaries Transaction boundaries Repository boundaries PostgreSQL responsibilities Redis responsibilities Kafka responsibilities AI integration boundary Error-handling architecture **API** versioning Security boundary Observability boundary **ADR** structure

We will **NOT** start coding business logic until the architecture is agreed upon.

Phase 5 — Database Design

After architecture:

Convert domain model into relational design ER diagram Tables Primary keys Foreign keys Constraints Indexes Unique constraints Money/decimal strategy Audit fields Optimistic locking Ledger schema Migration strategy Phase 6 — Core Backend

Implement the first real PayFlow functionality:

User ↓ Wallet ↓ Payment ↓ Transaction ↓ Ledger

Including:

**REST** APIs DTOs Validation Service/application logic Persistence Transactions Exception handling Idempotency Phase 7 — Event-Driven Architecture

Integrate Kafka with real PayFlow events.

Potential events:

PaymentCreated PaymentAuthorized PaymentCompleted PaymentFailed LedgerEntryCreated

We will decide the actual events only after architectural analysis.

Topics, keys, partitions, consumers, retries and dead-letter handling will be designed properly.

Phase 8 — Redis

Introduce Redis for appropriate production-style use cases:

Idempotency Caching Rate limiting Temporary state Distributed coordination where justified Phase 9 — AI Integration 🤖

AI will be integrated as a genuine backend capability rather than a decorative chatbot.

Potential capabilities include:

Transaction categorization Spending insights Suspicious transaction/risk signals Natural-language financial queries Payment anomaly analysis

AI will have a clear boundary from the core financial domain.

Phase 10 — Testing

Implement:

Unit tests Integration tests Repository tests **API** tests Kafka tests Redis tests Failure scenario tests Idempotency tests Transaction consistency tests Testcontainers where appropriate Phase 11 — Security

Implement:

Authentication Authorization **JWT**/security architecture Password/security practices **API** protection Input validation Sensitive-data handling Secure configuration Phase 12 — Production Readiness

Add:

Dockerized application Environment-specific configuration Secrets management approach Logging Metrics Health checks Observability Graceful shutdown Deployment strategy ## Current Starting Point

When continuing the project, use this exact checkpoint:

**PAYFLOW** ────────────────────────────────────── Phase 1: Product Requirements      ✅ Phase 2: Domain Modeling           ✅ Phase 3: Development Setup         ✅ Phase 4: Architecture             ⏳ **NEXT**

Current Git **HEAD**:

88f8f49 chore: add Kafka development infrastructure

Current infrastructure:

PostgreSQL 16   ✅ Redis 7         ✅ Kafka 4.0       ✅

Current repository state:

main == origin/main working tree clean

Next command/conversation instruction

To continue the project:

"Sir, start PayFlow Phase 4 from the Phase 3 checkpoint in this report."

We should then continue with Architecture Design, without repeating Phase 1–3.
