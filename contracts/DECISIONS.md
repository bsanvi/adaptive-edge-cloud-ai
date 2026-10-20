# Proposed Person 3 integration baseline

Status: implemented and locally tested proposal, **not yet approved by all four developers**.
The architecture plan is the baseline. This file resolves its previously unspecified details
for Person 3. Do not silently change these choices after the team freezes them.

## Scope and adoption

Person 3 owns cloud-service and route-estimator. The parent currently aggregates only these
existing modules; the integration owner adds the other three when their skeletons exist.
No module imports another developer's implementation. The OpenAPI file covers Person 3 only.
People 1, 2 and 4 must review the additions below before adopting these contracts.

## Cloud contract additions

- Both inference requests require privacyLevel (NORMAL or CRITICAL). Cloud rejects CRITICAL
  with 403, but this does **not** undo transmission: Person 4 must block raw and derived
  CRITICAL data before any outbound call. Probe contains no task data.
- GET /cloud/health is process health; GET /cloud/ready returns 200 READY only if checksum,
  model metadata, tensor execution and probability validation succeed, otherwise 503.
  Person 4 sets cloudCapable from readiness, not process health/probe alone.
- Probe is application/octet-stream: exactly the requested number of ASCII x bytes, 1 to
  1,048,576. Server timestamp is X-Server-Time, excluded from payload bytes. No compression,
  caching or task data. RTT and throughput measurements belong to Person 4. A GET download
  probe does not measure upload throughput; using its bandwidth for both directions is an
  explicit symmetry assumption. Use repeated measurements; do not double-count RTT.
- Each POST body is limited to 1,048,576 bytes even for chunked requests; exceeding returns
  413. Unknown fields, scalar coercions and null numeric array elements are rejected.
- Raw samples: 8..4096 finite doubles, magnitude <=1,000,000. Compute features in double,
  then cast each result to float32. Hybrid features: exactly 8 finite float32 values with
  magnitude <=2,000,000. Zero crossings count only opposite, nonzero adjacent signs and are
  divided by n-1. Population standard deviation divides by n. Positive fraction counts >0.
  Scaling is identity. Feature order is in model/metadata.json.
- Class order NORMAL, ANOMALY; anomaly probability >=0.5 maps to ANOMALY, including ties.
  Probabilities must be finite, within [0,1], and sum to one within 1e-5. Compare golden
  probabilities with absolute tolerance 1e-5 and features with 1e-6.
- inferenceTimeMs covers the handler's inference work: raw includes feature extraction;
  Hybrid is model execution. It excludes HTTP transport and request parsing. Person 4
  measures end-to-end elapsed time separately with a monotonic clock.
- Requests are stateless and **not deduplicated**. requestId is correlation only. Do not
  automatically retry remote inference. Timeout disconnects do not guarantee server
  cancellation. Person 4 must log failed/overlapping attempts and not claim their energy
  was zero when it is unknown. Fallback, deadline rechecks and cancellation policy remain
  Person 4 work; these modules do not implement them.
- Malformed bodies may have null requestId because they cannot be decoded safely. Other
  validated application errors echo requestId. Health/probe need no correlation ID.

## Reference model

The included reference-v1 ONNX file is a hand-authored, untrained, deterministic model:
p(ANOMALY)=sigmoid(2*RMS-2). It demonstrates real Java ONNX execution and Hybrid equivalence,
not anomaly-detection accuracy. No training dataset exists for this model. The generator,
checksum, metadata, provenance and golden fixtures are included. Generated model/fixtures
are dedicated under CC0-1.0; the tools retain their own licenses.

Replacing it requires team approval and new metadata/fixtures, then validation on both
laptops. External models require location, expected SHA-256 and modelVersion configuration;
metadata must contain modelVersion, featureSchemaVersion=features-v1 and labels=NORMAL,ANOMALY.
Input name features is float32 [1,8]; output probabilities is float32 [1,2]. No automatic
training, downloading of arbitrary models or silent mock inference occurs.

## Estimator assumptions and units

- Requests carry capabilities, metric source, probe timestamp, optional remaining deadline,
  and separate Edge/Cloud carbon intensities. Unknown battery is null. Missing Edge metrics
  make local-dependent estimates unavailable; Cloud remains estimable from network/calibration.
- CPU and RAM are nullable percentages. Source is mandatory on provided device/network objects.
  A disconnected network has null RTT/bandwidth. Connected but missing network measurements
  produce NETWORK_METRICS_UNAVAILABLE. Future or >5000 ms old probe gives STALE_PROBE.
- No privacy decisions, deadline rejection, normalization, scoring, hysteresis or fallback
  happen here. remainingDeadlineMs is validated context; Person 2 compares it with estimates.
- Every estimate is ESTIMATED. Coefficients are ASSUMED (prototype-v1), not measured. The
  incoming metric source remains part of Person 4's context; simulated input must retain
  its badge when Person 4 displays the resulting estimates.
- Transfer milliseconds = 1000*8*bytes/(Mbps*1,000,000). Count one RTT per remote inference.
  Sizes are actual UTF-8 JSON body bytes, excluding HTTP headers and link overhead; byte
  observations must state this convention. Result sizes are initially assumptions and
  should come from representative serialized results. Tiny inputs can make Hybrid larger.
- Prototype Edge slowdown = 1+cpuPercent/100. Edge pressure=min(1,max(cpu,ram)/100+0.1),
  Hybrid pressure=min(1,max(cpu,ram)/100+0.05), Cloud local incremental pressure=0.05.
  These are heuristic assumptions, not calibrated measurements. Local HTTP hop overhead,
  queueing and concurrent load are not modeled yet; calibration must account for them.
- Energy is watts*seconds. Transfer energy = bidirectional decimal MB * configured J/MB.
  This is the whole-link estimate. Half is allocated to Laptop 1 for deviceEnergyEstimateJ.
  energyEstimateJ is total Edge compute + Cloud compute + link transfer energy.
  cloudEnergyEstimateJ excludes transfer; transferEnergyEstimateJ is reported separately.
  Device energy includes half the transfer, so do not sum all reported energy fields.
- Carbon = (Edge compute J*Edge intensity + Cloud compute J*Cloud intensity + transfer J*
  network intensity)/3,600,000, in grams CO2e. Intensities are configurable assumptions.
- Person 2 should explicitly choose deviceEnergyEstimateJ for BATTERY_SAVER and
  energyEstimateJ for total-energy optimization; this policy change needs team approval.
- Unavailable estimates contain null costs and a machine-readable reason, never zero costs.
  No network requests are made by the estimator, including when Cloud is unavailable.

## Remaining integration gates

Review/freeze this contract; benchmark the real workload; obtain a validated trained model
if accuracy is part of the research; measure hardware calibration; implement Persons 1/2/4
contracts and test the actual three-stage Hybrid flow. This branch cannot certify another
module's privacy, fallback, persistence or deadlines. A small workload may never benefit
from offloading: report that result honestly rather than forcing a route.
