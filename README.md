# Adaptive Edge-Cloud AI

Java-based Adaptive Edge-Cloud AI workload orchestration project.

Person 3 implementation: cloud-service (8083) and local route-estimator (8084).
Requires JDK 25; the Maven Wrapper downloads Maven 3.9.11. Spring Boot is pinned to 4.1.1.

Start with [Person 3 setup and daily workflow](docs/PERSON_3_START_HERE.md).
Run `./mvnw clean verify` or `.\mvnw.cmd clean verify` on Windows.

The included ONNX reference model is untrained and intended for integration testing.
Estimation coefficients are prototype assumptions. Shared contracts are a proposed baseline
requiring team review; see [contract decisions](contracts/DECISIONS.md).
Other developers' modules have not been created by this Person 3 implementation.
