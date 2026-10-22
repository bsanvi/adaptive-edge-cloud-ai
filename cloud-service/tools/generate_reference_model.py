"""Offline integration fixture, not a trained anomaly detector. Requires onnx==1.18.0.

Reference model: p(ANOMALY) = sigmoid(2 * RMS - 2); NORMAL is its complement.
This deterministic hand-authored model proves transport and inference equivalence only.
No dataset, training, or predictive accuracy is claimed. Generated assets may be reused
under the CC0-1.0 dedication; ONNX tooling retains its own license.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
import onnx
from onnx import helper, TensorProto, numpy_helper
from onnx.reference import ReferenceEvaluator

ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "contracts" / "model"
FIXTURES = ROOT / "contracts" / "fixtures"
MODEL.mkdir(parents=True, exist_ok=True)
FIXTURES.mkdir(parents=True, exist_ok=True)
weights = np.zeros((8, 2), dtype=np.float32)
weights[4, 1] = 2
graph = helper.make_graph([
    helper.make_node("MatMul", ["features", "weights"], ["weighted"]),
    helper.make_node("Add", ["weighted", "bias"], ["logits"]),
    helper.make_node("Softmax", ["logits"], ["probabilities"], axis=1),
], "reference-anomaly", [helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, 8])],
   [helper.make_tensor_value_info("probabilities", TensorProto.FLOAT, [1, 2])],
   [numpy_helper.from_array(weights, "weights"), numpy_helper.from_array(np.array([0, -2], dtype=np.float32), "bias")])
model = helper.make_model(graph, producer_name="adaptive-edge-cloud-reference", opset_imports=[helper.make_opsetid("", 13)], ir_version=8)
helper.set_model_props(model, {"modelVersion": "reference-v1", "featureSchemaVersion": "features-v1", "labels": "NORMAL,ANOMALY"})
onnx.checker.check_model(model)
data = model.SerializeToString()
(MODEL / "reference.onnx").write_bytes(data)
checksum = hashlib.sha256(data).hexdigest()
(MODEL / "reference.sha256").write_text(checksum + "\n", encoding="utf-8")
metadata = {"modelVersion": "reference-v1", "featureSchemaVersion": "features-v1", "sha256": checksum,
            "purpose": "Untrained deterministic integration reference; no accuracy claim", "license": "CC0-1.0",
            "input": {"name": "features", "dtype": "float32", "shape": [1, 8]},
            "output": {"name": "probabilities", "dtype": "float32", "shape": [1, 2]},
            "featureOrder": ["mean", "standardDeviation", "minimum", "maximum", "rootMeanSquare", "peakToPeak", "zeroCrossingRate", "positiveFraction"],
            "normalization": "identity", "labels": ["NORMAL", "ANOMALY"], "anomalyThreshold": 0.5, "probabilityTolerance": 1e-5}
(MODEL / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
evaluator = ReferenceEvaluator(model)
fixtures = []
for name, raw in [("zeros", [0.0]*8), ("constant-positive", [2.0]*8),
                  ("alternating", [-1.0, 1.0]*4), ("mixed", [0.2,-0.1,0.3,0.4,-0.2,0.1,0.0,0.2])]:
    x = np.array(raw, dtype=np.float64)
    crossings = sum((a>0 and b<0) or (a<0 and b>0) for a,b in zip(raw,raw[1:]))
    features = np.array([x.mean(),x.std(),x.min(),x.max(),np.sqrt(np.mean(x*x)),np.ptp(x),crossings/(len(x)-1),np.mean(x>0)], dtype=np.float32)
    p = evaluator.run(None, {"features": features.reshape(1,8)})[0][0]
    fixtures.append({"name":name,"samples":raw,"features":features.tolist(),"probabilities":p.tolist(),
                     "prediction":"ANOMALY" if p[1]>=0.5 else "NORMAL"})
(FIXTURES / "golden.json").write_text(json.dumps(fixtures,indent=2)+"\n",encoding="utf-8")
print("Generated reference model and", len(fixtures), "golden fixtures. SHA-256:", checksum)
