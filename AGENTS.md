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
route-estimator/

Person 4 owns:
dashboard-orchestrator/

The five services communicate using HTTP + JSON only. Person 3 owns two services,
as authorized by the project owner using docs/Java Architecture Plan.txt as baseline.

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
route-estimator/**

Person 3 implements:

- Cloud AI inference
- Cloud REST API
- Hybrid cloud inference
- Cloud health endpoint
- Network probe endpoint
- Cloud deployment configuration
- Local route estimates, calibration, latency, device/system energy, communication and carbon

Person 3 must not implement:

- Edge monitoring
- routing decisions
- EDGE/CLOUD/HYBRID scoring
- dashboard
- decision logging
- fallback routing

Person 3 cloud-service runs on port 8083.
Person 3 route-estimator runs locally on Laptop 1 at 127.0.0.1:8084 and makes no remote calls.

During the two-laptop demo, cloud-service must bind to 0.0.0.0 so Laptop 1 can access it.

Before finishing any coding task:

1. Run the cloud-service and route-estimator tests.
2. Fix failing tests.
3. Review changed files.
4. Do not commit automatically unless explicitly asked.
