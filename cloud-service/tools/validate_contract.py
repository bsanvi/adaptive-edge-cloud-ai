from pathlib import Path
import json
import yaml
from jsonschema import Draft202012Validator, FormatChecker
from openapi_spec_validator import validate_spec

root=Path(__file__).resolve().parents[2]/"contracts"
document=yaml.safe_load((root/"openapi.yaml").read_text(encoding="utf-8"))
validate_spec(document)
for filename,schema in [("cloud-request.json","RawRequest"),("hybrid-request.json","HybridRequest"),("estimate-offline-request.json","EstimateRequest")]:
    validator=Draft202012Validator({"$ref":"#/components/schemas/"+schema,"components":document["components"]},format_checker=FormatChecker())
    validator.validate(json.loads((root/"fixtures"/filename).read_text(encoding="utf-8")))
print("OpenAPI specification and all three request fixtures validated")
