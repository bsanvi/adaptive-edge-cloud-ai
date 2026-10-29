# Decision API compatibility verification

Module-owned documentation, not a modification or team approval of shared contracts. This checkout has no shared contracts directory. Inspected cached Person 1/3/4 DTOs and clients; versions are listed in integration.md.

## Existing API retained

- GET `/health`: status and service.
- POST `/api/v1/decision/evaluate`: request task/device/network/currentRoute. Response taskId/status/selectedRoute/decisionTimeMs/evaluations/reasons, with the original nesting.
- Route names: EDGE, CLOUD, HYBRID. Status: ROUTE_SELECTED or NO_FEASIBLE_ROUTE; selectedRoute is null for the latter.
- Route evaluations retain route/feasible/estimatedLatencyMs/estimatedEnergyJ/estimatedCarbonG/communicationCost/switchingCost/score/rejectionReasons. Unknown estimates stay null. The established basic API represents rejected scores as 0; detailed scoring is null for rejected routes. These existing basic fields were not changed to match a conflicting draft.
- Scores are utilities in [0,1], higher is better; raw time/energy/cost/carbon are lower-is-better costs. Latency and deadlines are milliseconds, energy joules, carbon grams CO2e, CPU/RAM/battery percentages. Cost policy units must be agreed by the team.

## Additive APIs

- POST `/api/v1/decision/evaluate/detailed`: decision (the basic response) plus per-route details, rejection reasons, explanations and scoring (components, contributions, weights, baseScore, adjustedScore, deviceReadiness).
- POST `/api/v1/decision/what-if`: baseline plus 1 to 100 complete scenarios. Each has a unique name, request, hypothetical estimates, monetary/communication/switching policies. Response source=SIMULATED, tasksExecuted=false, baseline, scenarios and comparisons. Comparisons include field-level changes. Echoed estimate source/calibrationSource labels are normalized to SIMULATED, regardless of labels a caller supplied. Requests are not mutated. The endpoint does not recalculate physical costs or contact the live estimator.

## Validation and errors

Valid task types: SENSOR_ANOMALY; complexity LOW/MEDIUM/HIGH; privacy PUBLIC/PRIVATE or NORMAL/CRITICAL. Profiles BALANCED/LATENCY_PRIORITY/ENERGY_PRIORITY/COST_PRIORITY; null profile retains BALANCED. Current route is null/blank or an exact route name. Deadline must be positive finite, payload bytes 1..1048576, CPU/RAM and known battery in [0,100]. Device timestamp is required. Actual live freshness is enforced by the provenance gate; historical what-if timestamps remain valid as simulated context.

Invalid requests return HTTP 400 before provider lookup or estimation. All primitive device/network/task fields (CPU, RAM, battery value/availability, connectivity, RTT, bandwidth, deadline and byte size) cannot be omitted; explicit null primitive fields are rejected. Existing JSON field names/types are unchanged. Connected network requires positive bandwidth and nonnegative RTT. Disconnected numeric values are not claimed as measurements: the estimator adapter sends null bandwidth/RTT when connected=false. Battery is sent as null to the estimator when batteryAvailable=false. Nullable request telemetry remains a team contract issue.

Unconfigured or stale/untrusted supporting providers return HTTP 503 SERVICE_UNAVAILABLE. Estimator connection/timeout failures return HTTP 503 ESTIMATOR_UNAVAILABLE; upstream HTTP/malformed JSON failures return ESTIMATOR_ERROR. Empty or wrong-correlation responses return controlled 503. No automatic execution, retry, fallback or decision logging is added. Connect/read timeout properties default to 2000/5000 ms and must be positive.

## What was exercised

MockTeamIntegrationTests starts loopback HTTP peers on ephemeral ports. Its test-only Orchestrator fetches a synthetic snake_case Edge response, translates it, posts the normal request over HTTP, and consumes the Decision response through DTOs matching Person 4. The real Decision controller/pipeline/client calls the mock estimator over HTTP. Tests check byte units, RTT, costs, null battery, priority weights, offline mapping, unavailable estimates, upstream status/JSON/empty/id/timeout failures, missing/null telemetry and early validation. Production does not acquire an Edge or Orchestrator client.

A strict test demonstrates that Person 4's existing primitive-double route DTO cannot safely consume unavailable null costs; the service preserves truthful nulls. This is a detected compatibility gap, not a passed live-team integration claim.
