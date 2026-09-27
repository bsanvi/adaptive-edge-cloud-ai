# Start working as Person 3

## What is ready on this computer

- Branch: person-3. Nothing has been committed or pushed automatically.
- Existing JDK 25 is at C:\Program Files\Java\jdk-25; JAVA_HOME is set at user level.
- Existing VS Code Java Extension Pack includes language support, debugger, tests and Maven.
- Installed Spring Boot Extension Pack includes Spring tooling, Dashboard and Initializr.
- Maven Wrapper pins Maven 3.9.11. No separate global Maven install is needed.
- Both Java/Spring Boot 4.1.1 services, JUnit 5 tests, ONNX runtime, proposed contracts,
  a reference model, fixtures, and module READMEs are included.
- Optional offline model/schema tools are installed in ignored .tools/model-venv. Python
  is not required to run or build either Java service.

Restart VS Code once so new terminals inherit JAVA_HOME and extensions initialize.
Open the repository folder, allow Maven import, and wait for Java indexing to finish.
Do not create a new project with Initializr: this repository is already configured.

## First checks

Run these in PowerShell from the repository root:

```powershell
git branch --show-current
java -version
$env:JAVA_HOME
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

Expected: person-3, Java 25, Maven 3.9.11 and BUILD SUCCESS. If an existing terminal has
not refreshed, set `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'` in that terminal.
Maven's first run on another computer needs internet to download the pinned dependencies.
JDK 25 can emit Maven Unsafe/native-access warnings; distinguish those from build failures.

## Run locally without other developers

Terminal 1:

```powershell
.\mvnw.cmd -pl cloud-service spring-boot:run
```

Terminal 2:

```powershell
.\mvnw.cmd -pl route-estimator spring-boot:run
```

Terminal 3:

```powershell
curl.exe http://127.0.0.1:8083/api/v1/cloud/health
curl.exe http://127.0.0.1:8083/api/v1/cloud/ready
curl.exe http://127.0.0.1:8084/api/v1/estimate/health
curl.exe -H 'Content-Type: application/json' --data-binary '@contracts/fixtures/cloud-request.json' http://127.0.0.1:8083/api/v1/cloud/predict
curl.exe -H 'Content-Type: application/json' --data-binary '@contracts/fixtures/hybrid-request.json' http://127.0.0.1:8083/api/v1/cloud/hybrid/infer
curl.exe -H 'Content-Type: application/json' --data-binary '@contracts/fixtures/estimate-offline-request.json' http://127.0.0.1:8084/api/v1/estimate/routes
```

Use curl.exe, not PowerShell's curl alias. Health should be UP; Cloud readiness READY.
The offline fixture should return an Edge estimate and unavailable remote estimates.
For online estimation update connected, RTT, bandwidth and sampledAt to a fresh UTC value.
Stop a service with Ctrl+C in its terminal. Run only one process per port.

## Your daily development loop

1. Stay on the assigned person-3 branch. Do not work directly on main.
2. Change cloud-service/** or route-estimator/**. The initial shared setup is a one-time
   baseline; coordinate future shared-contract/parent changes with the integration owner.
3. Add meaningful tests for inference, validation, estimates or failure behavior you change.
4. Run `.\mvnw.cmd -pl cloud-service,route-estimator clean test`.
5. Run `git diff --check`, `git diff`, and `git status --short`; inspect new files too.
6. Commit only when ready and after review. First ask the integration owner to review the
   new shared baseline so the team can branch from compatible contracts. No push is needed
   merely to run locally. When publishing later, stage only the intended reviewed files.

The VS Code Testing view can run individual JUnit tests. Use Java Run/Debug on
CloudApplication or EstimatorApplication; their application.properties set the ports.
The Spring Boot Dashboard should discover both applications after Maven import.

## What you implement and what you need from others

You own remote raw/Hybrid inference, health/readiness/probe, Cloud deployment, and local
latency/communication/energy/carbon/resource estimates. Do not implement scoring, routing,
Edge monitoring, dashboard, database history or fallback.

Before other developers adopt this branch, review contracts/openapi.yaml and DECISIONS.md.
New required fields include inference privacyLevel and estimator capability/source/carbon
fields. Raw/Hybrid inference must use the same model/checksum/features as Person 1. Have
Person 2 agree which energy field BATTERY_SAVER uses. Person 4 must enforce privacy before
transmission, check readiness, use fresh probes, measure actual body sizes, and own timeout,
fallback and attempt logging. API examples are not a substitute for this agreement.

The reference model is real ONNX execution but **untrained**. It establishes deterministic
integration behavior only. Replace it with a validated model if the project requires
predictive accuracy, then regenerate golden fixtures and verify all three routes.
Estimator coefficients are prototype assumptions. Follow route-estimator/CALIBRATION.md.
The other three modules are intentionally not implemented in this Person 3 branch.

## Two-laptop demo

Build the same reviewed commit on both laptops with `.\mvnw.cmd clean verify`.
On Laptop 2, in the terminal that starts Cloud:

```powershell
$env:SERVER_ADDRESS = '0.0.0.0'
$env:SERVER_PORT = '8083'
.\mvnw.cmd -pl cloud-service spring-boot:run
```

Use ipconfig to find Laptop 2's private IPv4. If Windows Firewall blocks access, run this
once in an Administrator PowerShell on Laptop 2, on a trusted private network:

```powershell
New-NetFirewallRule -DisplayName 'Adaptive Cloud 8083 private LAN' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8083 -Profile Private -RemoteAddress LocalSubnet
```

No firewall rule was installed automatically: the final demo machine/network is not yet known.
Do not change the network to Public or expose this prototype to the internet.
From Laptop 1, replace the example address below with the actual Laptop 2 address:

```powershell
curl.exe http://192.168.1.25:8083/api/v1/cloud/ready
$env:CLOUD_BASE_URL = 'http://192.168.1.25:8083'
$env:ESTIMATOR_BASE_URL = 'http://127.0.0.1:8084'
.\mvnw.cmd -pl route-estimator spring-boot:run
```

Person 4 sets those same URLs in the terminal starting their orchestrator. The estimator
always stays on Laptop 1 and remains loopback-only. Test disconnected Cloud, stale probes,
CRITICAL privacy and real three-stage Hybrid once the other modules are available.

## Optional reproducibility tools

Do not regenerate frozen shared artifacts during ordinary development. For a coordinated
baseline revision only:

```powershell
python -m venv .tools/model-venv
.\.tools\model-venv\Scripts\python.exe -m pip install -r cloud-service/tools/model-requirements.txt -r cloud-service/tools/contract-requirements.txt
.\.tools\model-venv\Scripts\python.exe cloud-service/tools/generate_reference_model.py
.\.tools\model-venv\Scripts\python.exe cloud-service/tools/generate_contract.py
.\.tools\model-venv\Scripts\python.exe cloud-service/tools/validate_contract.py
.\mvnw.cmd clean verify
```

You can run validate_contract.py alone without changing any files. Binary model, checksum,
metadata and golden fixtures are packaged into Cloud by Maven from contracts/.
After packaging, optionally run `.\.tools\model-venv\Scripts\python.exe cloud-service/tools/verify_packaged.py`.
It launches both jars on temporary loopback ports, validates real responses against OpenAPI,
checks the raw/Hybrid result and stops its own processes automatically.
No database, Node.js, Docker, commercial cloud account or Postman is required for your modules.
