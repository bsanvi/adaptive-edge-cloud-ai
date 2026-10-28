# Route estimator — Person 3

This service runs on Laptop 1 at 127.0.0.1:8084, independently of Cloud. It never calls
other services. It produces raw estimated costs, not feasibility decisions or route scores.

```powershell
.\mvnw.cmd -pl route-estimator clean test
.\mvnw.cmd -pl route-estimator spring-boot:run
```

GET `/api/v1/estimate/health`; POST `/api/v1/estimate/routes`.
The offline fixture in contracts/fixtures works immediately. For an online request supply
an ISO UTC sampledAt within 5 seconds, positive bandwidthMbps, nonnegative networkLatencyMs,
connected=true and cloudCapable=true. Do not reuse an old fixed timestamp.

Configuration is in src/main/resources/application.properties. Arrays correspond to
LOW, MEDIUM, HIGH. All default times/powers are **assumptions**, not hardware measurements.
See [calibration procedure](CALIBRATION.md) before using estimates in research results.
Energy fields distinguish Laptop 1's battery cost from total system energy. Unknown battery
does not become zero, disconnected networks retain Edge estimates, missing Edge permits
Cloud-only estimation, and unavailable costs are null.

[OpenAPI](../contracts/openapi.yaml) and [decisions](../contracts/DECISIONS.md) define units,
nullability, payload conventions and proposed choices requiring the other developers' review.
