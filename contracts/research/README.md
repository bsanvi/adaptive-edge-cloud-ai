# Image research API v2

This is the coordinated integration proposal for all four module owners. It leaves
v1 endpoints in place and introduces a common image contract under `/api/v2`.
Team review is still required before adopting it on the shared integration branch.
Services communicate only over HTTP; generated DTO/code copies introduce no Java
module dependency. Run `python tools/sync_research_contract.py --check` to detect drift.

## Frozen model and preprocessing

- Model version: `squeezenet1.1-7-image-v2`; ONNX Runtime 1.22.0, one intra/inter-op thread.
- Model SHA-256: `1eeff551a67ae8d565ca33b572fc4b66e3ef357b0eb2863bb9ff47a918cc4088`.
- Label SHA-256: `1f386e0d1cb6e28b9c2dac651c3dea6801e98ad1b41a14ce6bb1a093d72069f5`.
- Assets are copied from the existing Person 1 SqueezeNet workload. Review upstream
  model and label distribution terms before external redistribution.
- JPEG/PNG only, maximum 8 MiB and 24 million pixels. Request JSON maximum 12 MiB.
- Decode; composite alpha over black; stretch to 224×224 using Java2D bilinear
  interpolation; RGB NCHW float32 `[1,3,224,224]`; divide channels by 255; subtract
  `[.485,.456,.406]`, divide by `[.229,.224,.225]`; softmax over 1,000 logits.
- This stretch preprocessing is a frozen integration choice. It is not a claim to
  reproduce the original model's published validation preprocessing or accuracy.
- Hybrid sends a lossless 224×224 PNG after Edge resize; Cloud applies normalization
  and inference; Edge computes softmax/top-5/label interpretation. Every stage is timed.
- Capability reports expose model/label hashes, preprocessing version, environment ID
  and hardware. Remote execution requires matching Edge/Cloud capabilities.
- Unit regression fixtures use three algorithmically generated patterns, not labeled
  natural photographs. Frozen expected top-1 indices/confidences are in both services'
  `ImageModelContractTest`. Prediction tolerance is 1e-5.

## HTTP surface

| Service | Method and path | Body / result |
|---|---|---|
| Edge | GET `/api/v2/edge/ready`, `/telemetry` | Capability / Device |
| Edge | POST `/api/v2/edge/profile`, `/predict`, `/hybrid/preprocess` | ImageInput → ImageProfile / Prediction / Prepared |
| Edge | POST `/api/v2/edge/hybrid/postprocess` | Logits → Prediction |
| Cloud | GET `/api/v2/cloud/ready`, `/telemetry` | Capability / Device |
| Cloud | GET `/api/v2/cloud/probe?bytes=262144` | Bounded binary download |
| Cloud | POST `/api/v2/cloud/probe` | Binary upload → received byte count |
| Cloud | POST `/api/v2/cloud/predict`, `/hybrid/infer` | ImageInput → Prediction / Logits |
| Estimator | POST `/api/v2/estimator/estimate`, `/observations` | EstimateInput / Observation |
| Estimator | GET `/api/v2/estimator/calibration` | Version and per-context buckets |
| Decision | POST `/api/v2/decision/evaluate` | DecisionInput → Decision |
| Decision | GET `/api/v2/decision/profiles` | Weights, caps and switching policy |
| Dashboard | POST `/api/v2/research/tasks` | Submission → 202 requestId |
| Dashboard | GET `/api/v2/research/tasks`, `/tasks/{id}`, `/stream`, `/telemetry` | Persisted audit / queue / observations |
| Dashboard | GET `/api/v2/research/dataset`, `/dataset/{id}/image` | Verified manifest/filtering / image |
| Dashboard | POST `/api/v2/research/experiments`, `/experiments/{id}/stop` | Start replay / stop future submissions |
| Dashboard | GET `/api/v2/research/experiments`, `/analytics`, `/export` | Replay plans / summaries / NDJSON |
| Dashboard | POST `/api/v2/research/sensitivity` | Fixed historical conditions, varying priorities; no execution |

Exact JSON fields are defined by records in `Research.java.template`, plus `Submission`
and `Plan` records in the orchestrator. Enums are uppercase, JSON names camelCase;
timestamps are ISO UTC, elapsed times milliseconds, bytes decimal, bandwidth Mbps,
energy joules, carbon grams CO2e. Unknown measurements/costs are null, never zero.

## Decision and measurement semantics

CRITICAL forbids both raw and derived data from remote transmission. The orchestrator
checks before every execution dispatch, including forced baselines; Cloud rejection
is defense in depth only. Baselines must still pass privacy, readiness, network and
deadline feasibility. Deadline starts at acceptance, includes queue/profiling/probing,
and is rechecked before execution and fallback. HTTP calls use the remaining budget;
a timeout does not prove that remote inference was cancelled.

Scores are **higher-is-better** in the Person 2 implementation used by v2. Do not apply
the older design document's lower-is-better convention. Profiles expose weights and
normalization caps; contribution sums explain scores. Existing-adaptive uses the
original scoring primitives and assumed v2 timings; it is a legacy-score adapter to
the common image task, not a claim of bit-for-bit original end-to-end system behavior.
Uncalibrated-adaptive isolates the new priority scoring with assumed timing coefficients.
Calibrated-adaptive uses the same scoring with measured timing buckets when available.

Network input is separately measured upload/download application goodput (256 KiB per
probe/full HTTP elapsed), plus median of three ready-request RTTs. Goodput includes
HTTP/server overhead; adding RTT in the transfer model is conservative and can
partially count fixed overhead twice. It is not link capacity. Service-stage subtraction
from route elapsed is a transport/dispatch residual, not pure network latency.

Timing EWMA alpha=.2 requires five observations per route/model/environment/image-size/
CPU-load bucket. Each accepted observation creates an immutable versioned snapshot;
repeated observation IDs are idempotent. Evaluation, warm-up and simulated context do
not update calibration through the orchestrator. The estimator rejects simulated context
but relies on the orchestrator to enforce dataset split eligibility. It is a trusted
loopback prototype, without authentication against arbitrary local clients.

Energy = compute watts × stage seconds plus bytes × assumed whole-link J/MB. Half of
link energy is allocated to the Edge device. Carbon = sum of location-specific joules
× configured gCO2e/kWh / 3,600,000. Energy/carbon exclude idle, orchestration and cooling;
failed/fallback total energy is unknown and reported null. All constants and estimate
versions are retained in audit records.
