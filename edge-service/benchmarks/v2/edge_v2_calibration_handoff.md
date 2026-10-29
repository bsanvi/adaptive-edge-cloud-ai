# Person 1 — Edge v2 Calibration Handoff

Model: `squeezenet1.1-7-image-v2`

Measured runs: 30 per workload per condition after 5 warm-ups.

Conditions: IDLE_NORMAL and CPU_LOADED.

OSHI CPU/RAM values are whole-system measurements. Timing results are measured; energy is not physically measured.

| Condition | Workload | Edge inference median ms | p95 ms | Hybrid preprocess median ms | Postprocess median ms | CPU median % |
|---|---|---:|---:|---:|---:|---:|
| IDLE_NORMAL | LOW | 17.298 | 18.738 | 5.435 | 0.326 | 17.5 |
| IDLE_NORMAL | MEDIUM | 15.879 | 17.048 | 7.987 | 0.296 | 13.7 |
| IDLE_NORMAL | HIGH | 12.992 | 13.552 | 19.293 | 0.275 | 10.2 |
| CPU_LOADED | LOW | 9.860 | 11.511 | 3.725 | 0.220 | 56.2 |
| CPU_LOADED | MEDIUM | 9.792 | 10.828 | 5.785 | 0.217 | 57.4 |
| CPU_LOADED | HIGH | 9.811 | 12.847 | 17.194 | 0.228 | 57.6 |

`edge_postprocessing_ms` is the same ImageModel postprocessing path used by Hybrid.
