# Adaptive Edge-Cloud AI — Codex Instructions

Before making any changes, read:

- docs/Java Architecture Plan.txt
- README.md
- contracts/ if it exists

This is a Java/Spring Boot multi-module project developed by four people in parallel.

## Architecture

Person 1 owns:
edge-agent/

Person 2 owns:
decision-service/

Person 3 owns:
cloud-service/

Person 4 owns:
dashboard-orchestrator/

The four components communicate using HTTP + JSON only.

Do not directly call Java classes belonging to another module.

## Shared files

Do not modify these during normal development unless explicitly instructed:

- contracts/**
- root pom.xml
- .github/**
- .env.example
- model/**

## Technology

- Java 25
- Spring Boot 4.1.1
- Maven
- JUnit 5
- Mockito
- Jackson
- Jakarta Validation

## Git rules

Never develop directly on main.

Each developer must work on their assigned branch.

During parallel development, each developer should modify only their assigned module.

## Person 3

Person 3 owns only:

cloud-service/**

Person 3 implements:

- Cloud AI inference
- Cloud REST API
- Hybrid cloud inference
- Cloud health endpoint
- Network probe endpoint
- Cloud deployment configuration

Person 3 must not implement:

- Edge monitoring
- routing decisions
- EDGE/CLOUD/HYBRID scoring
- dashboard
- decision logging
- fallback routing

Person 3 service runs on port 8083.

During the two-laptop demo, it must bind to 0.0.0.0 so Laptop 1 can access it.

Before finishing any coding task:

1. Run the cloud-service tests.
2. Fix failing tests.
3. Review changed files.
4. Do not commit automatically unless explicitly asked.