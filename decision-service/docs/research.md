# Reproducible decision-policy scenarios

Observed on 28 October 2026 using the passing Java test suite. These are **synthetic decision correctness cases**, not measurements from either laptop, trained-model accuracy, or evidence of real-world latency/energy improvements. Seven deliberately chosen cases cannot establish statistical superiority. Expected labels are stated in the test before evaluating the engine.

Reproduce with `mvn -f decision-service/pom.xml clean test` (or the PowerShell wrapper/cache equivalent in the README). `ResearchScenariosTests` writes `decision-service/target/research/policy-comparison.csv`; [research-results.csv](research-results.csv) is the exported result from this run for Person 4's Experiment Lab. The seed is 20261028 and each RANDOM policy draw is independent of feasibility. Baselines always choose their designated route without applying the adaptive engine's constraints; their failures are recorded, never executed.

## Expected versus observed

Shared synthetic estimates: EDGE 500 ms / 20 J; CLOUD 100 ms / 40 J; HYBRID 300 ms / 50 J. All carbon costs are .001 g and monetary costs zero. Network is 20 ms RTT / 50 Mbps where connected. These inputs deliberately exercise a tradeoff between time, total energy and readiness; they are not Person 3 calibration results. Device readiness is zero at CPU=RAM=100%, one at CPU=RAM=0%.

| Scenario | Expected | Observed | Reason |
|---|---|---|---|
| Normal, busy device | CLOUD | CLOUD | Best feasible adjusted utility |
| PRIVATE | EDGE | EDGE | Both remote routes prohibited |
| CRITICAL, cost profile | EDGE | EDGE | Privacy remains mandatory under preferences |
| Offline | EDGE | EDGE | Both remote routes prohibited |
| Deadline 100 ms | CLOUD | CLOUD | Only CLOUD meets the budget, including equality |
| Deadline 50 ms | No feasible route | No feasible route | All latency estimates exceed budget |
| Ready device | EDGE | EDGE | CPU/RAM readiness changes the utility tradeoff |

The device case evaluates readiness scoring rather than inventing a CPU-based hard capability prohibition. Capabilities are supplied to Person 3's estimator and represented through estimate availability. Existing tests separately cover missing estimates and hysteresis.

## Policy comparison

| Policy | Cases | Expected-route matches | Feasible selections | Abstentions |
|---|---:|---:|---:|---:|
| ALWAYS_EDGE | 7 | 4 | 5 | 0 |
| ALWAYS_CLOUD | 7 | 2 | 3 | 0 |
| RANDOM | 7 | 2 | 3 | 0 |
| ADAPTIVE | 7 | 7 | 6 | 1 |

Adaptive matched all seven expected outcomes, including the required abstention. It attempted no infeasible selection. The feasible-selection count is six because abstaining on the impossible deadline is correct. Always EDGE and always CLOUD violate constraints on two and four cases respectively; the seeded random policy violates constraints on four cases. Matching the expected adaptive route is separate from feasibility: a baseline can choose a feasible but lower-utility route.

CSV columns: scenario, source, policy, expectedAdaptiveRoute, selectedRoute, feasible, expectedMatch. The literal `null` means no selected route. `source` is always SIMULATED. Person 4 can import this as a scenario correctness dataset; it must be displayed separately from live execution timings, energy estimates and model prediction accuracy.

Remaining research work: paired real-laptop executions under the same task inputs; enough repeated trials for uncertainty estimates; calibrated cost coefficients; actual end-to-end deadline misses; per-stage Hybrid timings; hardware/model/provenance version logging. The engine's ranking correctness is not model classification accuracy. No real execution results are fabricated here.

The expanded Phase 2 study is available in [phase2-research.md](phase2-research.md), with 21 labeled cases and exported results.
