# Person 2 Decision Engine

This continues the existing Java 25 / Spring Boot 4.1.1 implementation. It selects routes; it never executes inference, performs fallback, or stores decision history.

## Implemented milestones

- J2.23: `POST /api/v1/decision/evaluate/detailed` wraps the existing `decision` response and adds `details`: feasibility, rejection reasons, route explanations, normalized utility components, weighted contributions, applied weights, base/adjusted scores and readiness. `DecisionPipelineResult` retains immutable route inputs, readiness, configuration and profile. The original `/evaluate` response has the same fields and nesting as before.
- J2.24: immutable validated JSON configuration replaces scoring constants. `src/main/resources/decision-config.json` uses the Python engine's `normalization` and `weights` names and values. Set `DECISION_SCORING_CONFIG` to an external JSON file before startup. Invalid bounds, nonnumeric values, negative weights or sums other than 1 fail startup. Missing optional Java profiles in a Python configuration use the bundled profile definitions. Configuration is loaded once, not changed during a request.
- J2.25: BALANCED, LATENCY_PRIORITY, ENERGY_PRIORITY and COST_PRIORITY. The table below gives time/energy/cost/carbon weights. Preferences change scoring only; privacy, network and deadlines remain hard constraints. Unknown profiles return validation errors when supporting inputs are available. Null profile retains BALANCED behavior.
- J2.26: `POST /api/v1/decision/what-if` takes a baseline and 1-100 uniquely named complete scenario snapshots. Each snapshot supplies a decision request, hypothetical route estimates and costs. The response echoes the simulated inputs, provides detailed decisions and compares selected routes with explanations. It reports `source=SIMULATED` and `tasksExecuted=false`. Neither baseline nor scenario is a live observation in this endpoint, even if incoming nested source labels say otherwise. No provider, estimator, inference or logging service is called. Changing network/CPU/deadline/profile is supported by replacing those fields in the scenario snapshot. Estimates must also reflect the hypothesized scenario: this service deliberately does not invent a physical cost model.
- J2.27: estimator default corrected to localhost:8084. The configured input provider is gated on actual authoritative dependencies; see the integration notes. Live two-laptop verification remains blocked by missing upstream providers.
- J2.28: HTTP tests cover both response contracts and what-if isolation. Person 4's existing top-level field names and EDGE/CLOUD/HYBRID names are preserved. The unavailable-value integration gap is documented separately.

| Profile | Time | Energy | Cost | Carbon |
|---|---:|---:|---:|---:|
| BALANCED | .50 | .20 | .20 | .10 |
| LATENCY_PRIORITY | .80 | .10 | .05 | .05 |
| ENERGY_PRIORITY | .20 | .60 | .10 | .10 |
| COST_PRIORITY | .20 | .10 | .60 | .10 |

Scores follow the existing Python engine: **higher utility is better**. Lower raw latency, energy, monetary cost and carbon produce higher normalized utility. Bounds clamp utility into [0,1]. EDGE/HYBRID utility is adjusted by 0.9 * rounded base + 0.1 * readiness; CLOUD keeps base utility. Readiness averages CPU/RAM availability. Battery remains excluded because Person 4 does not provide charging state. Detailed contributions describe the base score; the readiness adjustment is shown separately. Switching requires more than 0.05 absolute improvement. These existing semantics are preserved even though the architecture proposal describes a lower-is-better cost score.

Privacy accepts the existing PUBLIC/PRIVATE names and adds NORMAL/CRITICAL aliases. PRIVATE and CRITICAL prohibit both remote routes. A device-readiness preference never overrules privacy or other hard constraints. ENERGY_PRIORITY optimizes total estimated joules, not battery-only energy.

## Run and demonstrate (PowerShell from repository root)

```powershell
.\mvnw.cmd -f decision-service/pom.xml clean test
.\mvnw.cmd -f decision-service/pom.xml spring-boot:run
```

This checkout's shared wrapper currently fails before launching Maven (`Cannot index into a null array`). It was left unchanged because it is outside Person 2's scope. An equivalent installed/cached Maven command is:

```powershell
$maven = (Get-ChildItem "$env:USERPROFILE/.m2/wrapper/dists" -Recurse -Filter mvn.cmd | Select-Object -First 1).FullName
& $maven "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" -f decision-service/pom.xml clean test
& $maven "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" -f decision-service/pom.xml spring-boot:run
```

In a second terminal:

```powershell
$base = 'http://127.0.0.1:8082/api/v1/decision'
$body = Get-Content decision-service/examples/what-if.json -Raw
Invoke-RestMethod "$base/what-if" -Method Post -ContentType application/json -Body $body | ConvertTo-Json -Depth 30

# These two require authoritative supporting inputs and the estimator.
# The supplied request contains simulated, historical values and is a schema example only.
$body = Get-Content decision-service/examples/evaluate.json -Raw
Invoke-RestMethod "$base/evaluate" -Method Post -ContentType application/json -Body $body
Invoke-RestMethod "$base/evaluate/detailed" -Method Post -ContentType application/json -Body $body
```

Without authoritative integrations, basic and detailed evaluation intentionally return HTTP 503 `SERVICE_UNAVAILABLE`. What-if works independently. The decision server binds to loopback on 8082. Estimator URL is configurable through `ESTIMATOR_BASE_URL`.

See [integration findings](docs/integration.md) and [research results](docs/research.md). No commits or other modules were changed.

## Phase 2 completion

The verified current suite has **202 passing tests** (no failures/errors/skips). The Friday 30 October demonstration is fully scripted and rehearsed: [Phase 2 guide](docs/phase2-demo.md). [Expanded research](docs/phase2-research.md) covers 21 independently labeled scenarios and four strategies; all values are simulated.

Additional work completes shared request validation, required/null telemetry protection, configurable readiness/switching/network thresholds, capability defense, bounded estimator timeouts, upstream rejection explanations, what-if changed-input reporting and simulation labels. The original basic API field names and types remain unchanged. Offline network values are now null on the estimator wire. See [API contract verification](docs/api-contract.md).

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action Test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-DecisionMaven.ps1 -Action Run
# In another terminal:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File decision-service/tools/Invoke-Phase2Demo.ps1 -VerifyUnconfiguredGate
```

Python configuration settings `device_adjustment.strength`, `switching.minimum_improvement` and `network_rules` are now used consistently in the pipeline and detailed explanations, as well as scoring bounds and weights. Default behavior is preserved. Estimator HTTP settings `services.estimator.connect-timeout-ms` and `services.estimator.read-timeout-ms` default to 2000 and 5000 ms.
