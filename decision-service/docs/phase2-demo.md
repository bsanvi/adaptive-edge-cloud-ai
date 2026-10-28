# Friday Phase 2 evaluation: 30 October 2026

This procedure demonstrates Person 2 independently. It has been rehearsed against a standalone Decision application on 28 October 2026. It does not require teammate services, fabricate live measurements or execute inference.

## Prepare before the evaluation

Use Java 25 and Maven 3.9.11 (or the working wrapper), on `person2-decision-engine`. Open two PowerShell terminals at the repository root. Keep a copy of `docs/phase2-research-results.csv` and this guide. Do not stage or commit target outputs.

```powershell
git branch --show-current
git status --short
java -version
.\mvnw.cmd -f decision-service/pom.xml clean test
```

If this checkout's wrapper fails before Maven launches, use the tested module launcher:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action Test
```

Expected: **202 tests, 0 failures, 0 errors, 0 skipped**. The launcher uses installed/cached Maven and its local repository. The execution-policy override applies only to the child PowerShell process; system policy is not changed. This is needed here because direct `.ps1` execution is disabled. If no installed/cached Maven exists, install/restore it before the evaluation. Do not troubleshoot downloads during the presentation.

## 8-minute presentation

### 0:00-1:00: scope and architecture

Say: "My Java Decision service receives task requirements, device/network context and route estimates. It first rejects infeasible routes, then scores the remaining routes, applies device readiness and switching hysteresis, and returns an explanation. It does not execute tasks, perform fallback or store task history. Those are Orchestrator responsibilities."

Show the passing test summary, `docs/api-contract.md`, and the four profiles in `src/main/resources/decision-config.json`. Explain that higher utility is better in the existing Python/Java implementation. All shown metric and cost values in this independent demonstration are simulated.

### 1:00-2:00: start the actual Decision service

Terminal A:

```powershell
$env:DECISION_SCORING_CONFIG = 'classpath:decision-config.json'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action Run
```

Keep this terminal running. It binds to 127.0.0.1:8082. Do not enable the supporting-input gate or create substitute measurement/provider beans for this demonstration. No teammates need to start their services.

Terminal B:

```powershell
Invoke-RestMethod http://127.0.0.1:8082/health
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-Phase2Demo.ps1 -VerifyUnconfiguredGate
```

Expected: health UP; 21/21 expected decisions match; source SIMULATED; no tasks executed; invalid profile gives HTTP 400; `/evaluate` and `/evaluate/detailed` give controlled 503 because authoritative live providers are absent. The 503 is an intentional dependency status, not a successful live integration.

### 2:00-4:00: explain decisions and score details

The script writes `target/demo/phase2-response.json` and `target/demo/phase2-summary.csv`.

```powershell
$demo = Get-Content decision-service/target/demo/phase2-response.json -Raw | ConvertFrom-Json
$demo.baseline.result.decision
$demo.baseline.result.details | ConvertTo-Json -Depth 15
$demo.comparisons | Select-Object name, baselineRoute, scenarioRoute, routeChanged
```

Point to the normalized time/energy/cost/carbon components, their weighted contributions, the applied profile weights, readiness and final score. Contributions sum to the base utility; readiness is a separate adjustment. The pipeline and explanation use the same immutable configuration.

Highlight these rows:

- `normal-busy`: CLOUD; `device-ready`: EDGE. CPU/RAM readiness changes the tradeoff.
- `private` / `critical`: EDGE; remote routes are blocked regardless of priority.
- `offline`, `poor-rtt`, `poor-bandwidth`: EDGE; remote feasibility fails.
- `deadline-equality-cloud`: CLOUD at 100 ms; `deadline-impossible`: no feasible route.
- `hybrid-winner`: HYBRID wins a hypothetical cost scenario; this is route selection, not executed Hybrid inference.
- `same-costs-balanced`: CLOUD; `cost-profile`: EDGE with identical costs and a different priority.
- `hysteresis-keeps-cloud`: a tiny improvement cannot trigger switching; `privacy-overrides-hysteresis`: an infeasible previous route cannot be retained.

Show changed inputs and a rejection reason:

```powershell
($demo.comparisons | Where-Object name -eq 'cost-profile').changes | ConvertTo-Json -Depth 15
($demo.scenarios | Where-Object name -eq 'critical').result.details | ConvertTo-Json -Depth 15
```

Say: "These complete what-if snapshots include caller-supplied hypothetical estimates. If I change a network or CPU scenario, its estimates must reflect that scenario. The Decision service does not invent latency or energy measurements."

### 4:00-5:30: mock integration evidence

The automated mock suite starts local HTTP peers on ephemeral ports and runs the actual Decision controller, pipeline, adapter and estimator HTTP client. Its test-only Orchestrator maps a synthetic Edge response and reads the Decision response using Person 4 field names. It tests bytes/ms/joules, null unknown battery, offline null RTT/bandwidth, profile consistency, unavailable estimates, bad JSON, upstream HTTP errors, empty responses, wrong IDs, timeouts, and invalid/missing/null metrics.

If time permits, run in Terminal B:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action MockIntegration
```

Say: "This proves the tested HTTP boundaries against mocks. It does not prove the deployed teammate applications work together. A strict consumer test detects Person 4's primitive/null cost mismatch."

### 5:30-7:00: research results

Open `docs/phase2-research.md` and `docs/phase2-research-results.csv`.

| Strategy | Matches / 21 | Constraint violations | Correct abstentions |
|---|---:|---:|---:|
| Always EDGE | 10 | 5 | 0 |
| Always CLOUD | 7 | 10 | 0 |
| Seeded RANDOM | 10 | 8 | 0 |
| ADAPTIVE | 21 | 0 | 3 |

Say: "These are 21 hand-selected correctness scenarios with independent expected-route and feasibility labels. Adaptive matches them all. This is not real-world accuracy, physical energy measurement or proof of speedup. The random result is one seeded realization. Live empirical evaluation is a separate team task."

### 7:00-8:00: limitations and next integration steps

Show `docs/integration.md`. Remaining team dependencies are authoritative provenance and capability adapters, nullable Orchestrator route fields, Edge snake_case translation and workload/model contract alignment, plus a two-laptop smoke test and measured experiment campaign. Do not say that these integrations passed. No commit/push/merge was made.

Stop Terminal A with Ctrl+C when finished. Evidence in target is removed by `clean`; copy only intended simulated CSV/report artifacts outside target if needed.

## Short answers for the evaluator

- Why constraints first? A fast or cheap route must never bypass privacy, connectivity, capability or deadline requirements.
- Why normalization? It puts milliseconds, joules, monetary cost and grams onto comparable fixed scales. Bounds and weights are configurable and validated.
- Why priorities? They express user tradeoffs among feasible routes; each profile sums to one.
- Why hysteresis? It prevents unnecessary switching for insignificant utility gains. Invalid previous routes are rejected immediately.
- Is battery energy measured? No. ENERGY_PRIORITY uses estimated total route joules. Battery readiness is omitted until charging state is available.
- Why 503? Required authoritative live context is unavailable. Returning a route with invented measurements would be misleading.
- What runs on the laptops? This independent demo runs only Decision. Actual Edge/Cloud inference and Orchestrator execution are teammate responsibilities and require separate integration testing.
- What remains for research? Repeated paired executions, measured deadlines/timings, real model equivalence and calibrated estimates, with provenance and uncertainty reported.

## Troubleshooting

- Port 8082 in use: identify the owner; stop only your own prior Decision process, or agree a different port and pass `-BaseUrl` to the demo script. Do not stop arbitrary Java processes.
- Fixed-label scenario mismatch: use the bundled default configuration in Terminal A; custom weights/thresholds intentionally change results.
- Wrapper failure: use the provided Maven launcher; no shared wrapper changes were made.
- No Maven cache/network: prepare dependencies before Friday; the already exported simulated CSV remains reviewable evidence, not a substitute claim of a passing current build.
- No teammate network: continue the independent simulation/mock demonstration. Do not label it live integration.
