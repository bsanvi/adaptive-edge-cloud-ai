# Integration inspection, 28 October 2026

Inspected local cached remote-tracking source without switching or merging branches:

- Person 3 `origin/person-3`: `167f7ad40de0e4bd4c1e2dec8bda362efca4c782`.
- Person 4 `origin/person-4`: `052d76fd0f98c448bc7233d2b6d46021ed2a1900`.

These snapshots do not establish that remote GitHub heads or deployed laptops are identical. No live two-laptop test is claimed.

## Person 3

The actual estimator is a separate process on **8084**, POST `/api/v1/estimate/routes`; Cloud inference is on 8083. Request schema matches the existing adapter: requestId; task with taskId, taskType, complexity, dataSizeBytes, estimatedFeatureBytes and estimatedResultBytes; device with CPU/RAM/battery and source; network with connected, bandwidthMbps, networkLatencyMs, sampledAt and source; edgeCapable/cloudCapable; remainingDeadlineMs; two carbon intensities.

Response matches `EstimatorResponse` / `RouteEstimate`: requestId, calibrationVersion, calibrationSource, estimates keyed by EDGE/CLOUD/HYBRID. Route fields include estimateAvailable, reason, estimatedLatencyMs, energyEstimateJ, deviceEnergyEstimateJ, cloudEnergyEstimateJ, transferEnergyEstimateJ, carbonEstimateG, uploadBytes, downloadBytes, communicationMb, resourcePressure and source. Unavailable routes use null values. Total energy must not be summed with its subcomponents.

Units: milliseconds, joules, grams CO2e, bytes, decimal MB, Mbps; network latency is one full RTT. The estimator rejects future or >5000 ms old probes for remote estimates. Its prototype calibration source is ASSUMED, and output costs are ESTIMATED, even when input metrics are measured. Do not represent those costs as physical measurements.

## Authoritative input activation

`SupportingInputsGate` is the sole default `DecisionInputsProvider`. Enable `decision.supporting-inputs.enabled=true` only when integration supplies beans for:

1. `DecisionIntegrationPolicy`: validated payload assumptions, carbon intensities, monetary/switching/communication cost policies.
2. `NetworkMeasurementMetadataProvider`: original network sampling timestamp and source.
3. `ExecutionCapabilitiesProvider`: actual model/hardware availability, not a hardcoded true flag.
4. `DeviceProvenanceProvider`: original device sample timestamp and source.

All dependencies and explicit enablement are required. Device/network provenance must be MEASURED, no older than five seconds, not future dated; device timestamp must match the request. The gate then delegates to the existing `ConfiguredDecisionInputsProvider`. Missing/stale/simulated provenance produces controlled 503, without creating timestamps or metrics. Providers themselves are trusted adapters and must obtain data from authoritative context, not generate substitute measurements. No such adapters exist in this checkout, so live routing remains disabled. What-if is available separately.

## Person 4 compatibility and blockers

`DecisionApiClient` calls `/api/v1/decision/evaluate`. Its request and our request both use task/device/network/currentRoute. Top-level response fields remain taskId/status/selectedRoute/decisionTimeMs/evaluations/reasons. HTTP tests check these fields and the detailed wrapper. Scores remain higher-is-better, matching the existing Python/Java implementation.

Person 4's RouteEvaluation currently uses primitive doubles for fields our established response permits to be null when an estimate is unavailable. Primitive deserialization may reject null or coerce it to zero, depending on mapper configuration. Team action: agree nullable wrappers and test unavailable routes end to end. This module does not fabricate zero costs to hide that mismatch.

Person 4's network request has no sampling timestamp/source and its device request has no source or charging status. Authoritative provider adapters or an agreed additive contract must supply provenance before live activation. Its PUBLIC/PRIVATE privacy values remain supported; NORMAL/CRITICAL aliases now match Person 3's proposal. Priority names REAL_TIME/BATTERY_SAVER/SUSTAINABILITY from the architecture draft are not silently mapped onto the four requested profiles; coordinate any callers using those names.

Disconnected RTT/bandwidth are primitives in Person 4 and the existing DecisionRequest, while Person 3 permits null. The adapter now sends null RTT/bandwidth to Person 3 for disconnected requests. Person 4 still sends its established numeric request fields; these are ignored as remote measurements when disconnected. Coordinate nullable request telemetry before sending null from Person 4.

Shared contracts are absent from this working branch. Person 3's `contracts/DECISIONS.md` identifies its contract as a proposal requiring team review. No shared files or other modules were modified. Live deployment verification and a team contract freeze remain outstanding.

## Additional verified mock boundaries

Cached Person 1 snapshot `origin/person-1`: `2f38e5444a05f2db481ff56608dc50ad53441b40`. Its `edge-service` EdgeMetrics returns cpu_percent/ram_percent/battery_percent/power_plugged/timestamp; Person 4 directly deserializes that endpoint into camelCase DeviceMetrics. A team adapter/schema agreement is needed to avoid silent zero/default telemetry. New validation rejects missing CPU/RAM and explicit null primitive fields at Decision's boundary, but cannot detect an upstream client that already fabricated numeric defaults.

Person 1 currently implements an image/SqueezeNet workload, whereas the inspected Person 3 estimator/Cloud proposal is SENSOR_ANOMALY with a feature-vector model. That workload/model/Hybrid mismatch is a team integration dependency, not something Person 2 changes silently.

MockTeamIntegrationTests exercises synthetic Edge metrics -> test-only Orchestrator mapping -> real Decision HTTP -> real estimator client -> mock estimator -> Person 4-shaped consumer. Mock HTTP success/failure tests and standalone Decision what-if rehearsal passed. None of the actual teammate services was started or tested together. The current strict consumer regression explicitly demonstrates Person 4's primitive/null incompatibility rather than hiding it.
