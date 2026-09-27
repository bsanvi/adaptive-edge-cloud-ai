# Cloud service — Person 3

Start with [the Person 3 guide](../docs/PERSON_3_START_HERE.md).

Runs real ONNX inference on raw samples or eight Hybrid features. Default address is
127.0.0.1:8083; set SERVER_ADDRESS=0.0.0.0 in the Cloud terminal for the two-laptop demo.

From repository root:

```powershell
.\mvnw.cmd -pl cloud-service clean test
.\mvnw.cmd -pl cloud-service spring-boot:run
```

GET `/api/v1/cloud/health`, `/api/v1/cloud/ready`, `/api/v1/cloud/probe?bytes=65536`.
POST `/api/v1/cloud/predict`, `/api/v1/cloud/hybrid/infer`.
See [OpenAPI](../contracts/openapi.yaml), [request fixtures](../contracts/fixtures), and
[the proposed contract decisions](../contracts/DECISIONS.md). Missing/incompatible model
keeps HTTP health available but readiness/inference return 503.

The packaged reference model is **untrained**. Its reproducible generator and requirements
are in `tools/`; Python is only needed for regeneration/schema checks, never at runtime.
Golden tests run the native ONNX library and do not depend on another service or laptop.

External model configuration: CLOUD_MODEL_LOCATION (Spring resource URI, e.g.
file:C:/models/model.onnx), CLOUD_MODEL_SHA256, CLOUD_MODEL_VERSION. A replacement must
satisfy the metadata and tensor contract in contracts/DECISIONS.md.
