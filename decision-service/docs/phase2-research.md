# Phase 2 decision accuracy experiments

Run date: 28 October 2026. All conditions and cost estimates are **SIMULATED**. No Edge, Cloud inference or deployed Orchestrator service was used. This evaluates route-decision correctness, not classifier accuracy, measured energy savings or real-laptop speedup.

The 21-case fixture independently specifies both the expected route and the set of feasible routes before calling the engine. Parameterized tests compare both, making it harder for a routing regression to hide behind an unchanged final selection. Cases cover all three route winners, four profiles, both privacy naming schemes, offline/poor network, threshold equality, deadline equality/misses, readiness, hysteresis overrides and unavailable estimates. No runtime samples are invented.

## Observed versus expected

| Scenario | Expected | Observed | Purpose |
|---|---|---|---|
| normal-busy | CLOUD | CLOUD | Busy local device; cloud wins adjusted utility |
| private | EDGE | EDGE | PRIVATE blocks remote routes |
| critical | EDGE | EDGE | CRITICAL blocks remote even with cost priority |
| offline | EDGE | EDGE | Disconnected remote routes rejected |
| deadline-equality-cloud | CLOUD | CLOUD | 100ms cloud estimate equals deadline |
| deadline-impossible | NONE | NONE | All route estimates exceed 50ms budget |
| device-ready | EDGE | EDGE | CPU/RAM readiness tips the tradeoff |
| hybrid-winner | HYBRID | HYBRID | Hypothetical reduced Hybrid estimate wins |
| latency-profile | CLOUD | CLOUD | Latency profile favors cloud |
| energy-profile | EDGE | EDGE | Total energy profile favors lower-joule edge |
| cost-profile | EDGE | EDGE | Monetary preference changes route |
| same-costs-balanced | CLOUD | CLOUD | Balanced comparison with same cost estimates |
| hysteresis-keeps-cloud | CLOUD | CLOUD | Ready Edge gain below .05 retains cloud |
| privacy-overrides-hysteresis | EDGE | EDGE | Infeasible previous cloud cannot be retained |
| poor-rtt | EDGE | EDGE | RTT above configured 200ms bound |
| poor-bandwidth | EDGE | EDGE | Bandwidth below configured 5Mbps bound |
| network-boundary | CLOUD | CLOUD | 200ms RTT and 5Mbps equality accepted |
| private-deadline-equality | EDGE | EDGE | Private Edge finishes exactly at budget |
| private-deadline-miss | NONE | NONE | Privacy and deadline together leave no route |
| edge-unavailable | CLOUD | CLOUD | Local capability estimates unavailable |
| all-estimates-unavailable | NONE | NONE | Missing estimates are never zero costs |

## Strategy comparison

| Strategy | Cases | Expected-route matches | Feasible selections | Abstentions | Constraint violations |
|---|---:|---:|---:|---:|---:|
| ALWAYS_EDGE | 21 | 10 | 16 | 0 | 5 |
| ALWAYS_CLOUD | 21 | 7 | 11 | 0 | 10 |
| RANDOM | 21 | 10 | 13 | 0 | 8 |
| ADAPTIVE | 21 | 21 | 18 | 3 | 0 |

Adaptive matches all 21 labels (100% on this deliberately selected correctness suite). Abstention is correct when the independent feasible set is empty, so abstentions are not failures or attempted routes. Fixed and random baselines intentionally choose without adaptive constraints; violations are recorded, never executed. A feasible selection can still differ from the expected utility/hysteresis winner.

Random uses Java Random seed 20261028, one draw per case from EDGE/CLOUD/HYBRID. This single seeded realization is reproducibility evidence, not a statistically precise estimate of random policy performance. The suite is hand-selected and cannot establish out-of-sample accuracy or empirical superiority. Synthetic estimates exercise logic; they are not the actual estimator's calibration output.

## Reproduce and export to Person 4

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action Experiments
```

Test data: `src/test/resources/contracts/accuracy-cases.json`. Output: `target/research/expanded-policy-comparison.csv`. Exported evidence from this run: [phase2-research-results.csv](phase2-research-results.csv), 84 rows (21 cases x 4 policies). Columns identify source, policy, expected/selected routes, feasibility, match, abstention and violation. The `null` route means no selection. Person 4 can import this CSV as **simulated decision correctness** evidence, separate from live execution history and model predictions. No claim is made that an Experiment Lab import endpoint exists.

The original seven-case study remains in [research.md](research.md) as historical evidence. This 21-case matrix supersedes it for Phase 2 coverage.

Remaining empirical study: run paired task inputs across real policies, repeat enough trials to report variability, capture actual deadline misses and Hybrid stages, version hardware/model/calibration, and retain separate measured/simulated/estimated fields. These depend on teammate services and authoritative context.
