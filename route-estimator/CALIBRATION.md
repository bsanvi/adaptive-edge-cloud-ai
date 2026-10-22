# Calibration before evaluation

Defaults in application.properties are prototype assumptions. Do not publish them as
measured energy/carbon or measured timing. The service deliberately reports ASSUMED.

1. Build and start Cloud on the intended machine. Record OS, CPU, RAM, power mode, JDK,
   model checksum, device identifiers and date. Keep competing workloads controlled.
2. From repo root run `powershell -ExecutionPolicy Bypass -File cloud-service/tools/Measure-Cloud.ps1`.
   This warms five requests and measures thirty runs each at 128/512/4096 samples. Change
   BaseUrl for Laptop 2. The report is in ignored .tools/cloud-calibration.json. Record the
   server's hardware separately; clientDevice identifies the caller, not necessarily Cloud.
3. Use measured server inference medians for estimate.cloud-ms (LOW,MEDIUM,HIGH). HTTP
   medians include client overhead/transport: do not add them to RTT again.
4. Benchmark Hybrid inference separately using /cloud/hybrid/infer and shared feature
   fixtures, and use its median inferenceTimeMs for estimate.hybrid-cloud-ms. Ask Person 1
   for local prediction/preprocess/postprocess timings on the same inputs. Person 4 should
   measure local REST overhead and provide actual serialized raw/feature/result body sizes.
5. Set edge-ms, preprocess-ms, postprocess-ms, cloud-ms and hybrid-cloud-ms using agreed
   calibration. The CPU slowdown heuristic also needs experimental checking; baseline Edge
   timing should represent an idle machine so load is not counted twice.
6. Average power and transfer J/MB require instrumentation or a documented external
   assumption. Timing measurements alone do not measure joules. Record the link allocation
   assumption (currently half the link estimate charged to Laptop 1), and separate node
   and network carbon intensities. Do not label the whole profile CALIBRATED when only
   timing is measured; retain ASSUMED for this baseline and document measured components.
7. Create a new calibration-version and preserve the inputs/report with the experiment.
   Override properties with an external application.properties file using
   `--spring.config.additional-location=file:./route-estimator/calibration/` at launch,
   or Spring environment variables. Keep device-specific reports out of shared contracts
   until the team agrees. Coefficients are not automatically rewritten from benchmarks.
8. Compare estimated with observed latency in held-out repeated runs. Report median/p95,
   variability, sample size and errors. Freeze the profile during evaluation. If Cloud is
   slower for this lightweight workload, report it; do not tune until offloading always wins.
