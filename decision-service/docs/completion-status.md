# Person 2 completion and team handoff

## Completed independently

- J2.23 detailed endpoint and retained route/configuration/readiness inputs; existing basic JSON fields/types preserved.
- J2.24 validated Python-compatible JSON configuration for weights, normalization, readiness strength, switching improvement and network limits. Pipeline and detailed explanation share configuration.
- J2.25 all four requested profiles, sum-to-one validation and hard-constraint invariance.
- J2.26 isolated what-if endpoint, complete simulated snapshots, source normalization, field-level changes, comparisons/explanations and no task execution.
- J2.27 Person 3 source/units/ports/schema inspection; correct 8084 default, bounded HTTP calls, controlled integration errors, offline null telemetry mapping and authoritative-input activation gate.
- J2.28 existing Person 4 basic fields/route names retained and tested over HTTP with mocked peer services/consumer DTOs. Missing/null telemetry guards and unavailable-estimate reasons added. The primitive/null mismatch is exposed by a regression test rather than hidden.
- Research: 21 labeled scenarios, independently specified feasible sets, four strategy comparisons, seed and limitations documented, CSV export supplied for Experiment Lab review.
- Friday 30 October demo guide, reusable PowerShell Maven/demo scripts, standalone rehearsal and 202-test clean build completed.

## Blocked by teammate or deployment dependencies

1. Authoritative device/network provenance, capability providers and agreed cost policies are not supplied by current upstream contracts. Live evaluation stays gated; no substituted measurements or timestamps are created.
2. Person 4 uses primitive route-cost fields while unavailable estimator values are null. Team needs nullable DTO/schema handling and an end-to-end unavailable-route check.
3. Cached Person 1 Edge returns snake_case metrics while Person 4 directly expects camelCase; agree a real translation/schema. Decision validation cannot recover telemetry already converted to zero by an upstream client.
4. Cached Person 1 uses image/SqueezeNet inference while Person 3's proposal uses SENSOR_ANOMALY and feature vectors. Agree a single workload/model/Hybrid contract before claiming execution compatibility.
5. No deployed two-laptop smoke test, actual Hybrid execution verification, calibrated physical costs or repeated measured policy campaign has been completed. These require the teammate applications and hardware.

Shared wrapper startup is also broken in this checkout, but a tested module-local installed/cache Maven launcher removes it as a blocker for Person 2's independent build/demo. Shared contracts are absent from this branch; inspected remote-tracking snapshots are not proof of latest remote/deployed state.

No independently implementable requirement is being deferred for those missing services; the completed test/demo path uses explicitly simulated context and mocks. Approval is still required before any commit, push or merge.
