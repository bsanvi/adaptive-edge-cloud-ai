"""Generate the proposed Person 3 OpenAPI baseline. Requires PyYAML.
Team approval is required before treating this baseline as frozen.
"""
from pathlib import Path
import json
import yaml

root=Path(__file__).resolve().parents[2]/"contracts"
root.mkdir(exist_ok=True)
def obj(properties,required=None):
    return {"type":"object","additionalProperties":False,"properties":properties,"required":list(properties) if required is None else required}
def ref(name):return {"$ref":"#/components/schemas/"+name}
def number(minimum=0,maximum=None,nullable=False):
    s={"type":["number","null"] if nullable else "number","minimum":minimum}
    if maximum is not None:s["maximum"]=maximum
    return s
def nullable(schema):return {"anyOf":[schema,{"type":"null"}]}
def array(items,minimum,maximum):return {"type":"array","items":items,"minItems":minimum,"maxItems":maximum}
identifier={"type":"string","minLength":1,"maxLength":100,"pattern":r"\S"}
text={"type":"string"}
boolean={"type":"boolean"}
source={"type":"string","enum":["MEASURED","SIMULATED","ESTIMATED","CALIBRATED"]}
privacy={"type":"string","enum":["NORMAL","CRITICAL"],"description":"CRITICAL is rejected with 403. Caller must prevent any outbound transmission before this check."}
timestamp={"type":"string","format":"date-time"}
byte_count={"type":"integer","minimum":1,"maximum":1048576}
base={"requestId":identifier,"taskId":identifier,"privacyLevel":privacy}
schemas={
 "RawRequest":obj({**base,"samples":array(number(-1000000,1000000),8,4096)}),
 "HybridRequest":obj({**base,"featureSchemaVersion":{"type":"string","const":"features-v1"},"features":array(number(-2000000,2000000),8,8)}),
 "Prediction":obj({"requestId":identifier,"prediction":{"type":"string","enum":["NORMAL","ANOMALY"]},"confidence":number(0,1),"modelVersion":text,"inferenceTimeMs":number()}),
 "HybridOutput":obj({"requestId":identifier,"modelVersion":text,"classProbabilities":obj({"NORMAL":number(0,1),"ANOMALY":number(0,1)}),"inferenceTimeMs":number()}),
 "Health":obj({"status":{"type":"string","const":"UP"},"service":text}),
 "Readiness":obj({"status":{"type":"string","enum":["READY","NOT_READY"]},"modelVersion":text,"modelSha256":text}),
 "Error":obj({"requestId":nullable(identifier),"code":text,"message":text,"details":{"type":"object"},"timestamp":timestamp}),
 "Task":obj({"taskId":identifier,"taskType":{"type":"string","const":"SENSOR_ANOMALY"},"complexity":{"type":"string","enum":["LOW","MEDIUM","HIGH"]},"dataSizeBytes":byte_count,"estimatedFeatureBytes":byte_count,"estimatedResultBytes":byte_count}),
 "Device":obj({"cpuPercent":number(0,100,True),"ramPercent":number(0,100,True),"batteryPercent":number(0,100,True),"source":source},["source"]),
 "Network":obj({"connected":boolean,"bandwidthMbps":nullable({"type":"number","exclusiveMinimum":0}),"networkLatencyMs":number(nullable=True),"sampledAt":nullable(timestamp),"source":source},["connected","source"]),
}
schemas["EstimateRequest"]=obj({"requestId":identifier,"task":ref("Task"),"device":nullable(ref("Device")),"network":ref("Network"),
    "edgeCapable":boolean,"cloudCapable":boolean,"remainingDeadlineMs":number(nullable=True),
    "edgeCarbonIntensityGPerKwh":number(),"cloudCarbonIntensityGPerKwh":number()},
    ["requestId","task","network","edgeCapable","cloudCapable","edgeCarbonIntensityGPerKwh","cloudCarbonIntensityGPerKwh"])
properties={"estimateAvailable":boolean,"reason":nullable(text)}
for name in ["estimatedLatencyMs","energyEstimateJ","deviceEnergyEstimateJ","cloudEnergyEstimateJ","transferEnergyEstimateJ","carbonEstimateG","communicationMb"]:
    properties[name]=number(nullable=True)
properties.update({"uploadBytes":{"type":["integer","null"],"minimum":0},"downloadBytes":{"type":["integer","null"],"minimum":0},"resourcePressure":number(0,1,True),"source":{"type":"string","const":"ESTIMATED"}})
schemas["RouteEstimate"]=obj(properties)
schemas["EstimateResponse"]=obj({"requestId":identifier,"calibrationVersion":text,"calibrationSource":{"type":"string","const":"ASSUMED"},"estimates":obj({r:ref("RouteEstimate") for r in ["EDGE","CLOUD","HYBRID"]})})
def response(name,description="Success"):
    return {"description":description,"content":{"application/json":{"schema":ref(name)}}}
def operation(name,output,input_name=None):
    op={"operationId":name,"responses":{"200":response(output)}}
    if input_name:
        op["requestBody"]={"required":True,"content":{"application/json":{"schema":ref(input_name)}}}
        for status in ["400","413"]:op["responses"][status]=response("Error","Invalid or oversized request")
    return op
paths={
 "/api/v1/cloud/health":{"get":operation("cloudHealth","Health")},
 "/api/v1/cloud/ready":{"get":operation("cloudReadiness","Readiness")},
 "/api/v1/cloud/predict":{"post":operation("cloudPredict","Prediction","RawRequest")},
 "/api/v1/cloud/hybrid/infer":{"post":operation("cloudHybridInfer","HybridOutput","HybridRequest")},
 "/api/v1/estimate/health":{"get":operation("estimateHealth","Health")},
 "/api/v1/estimate/routes":{"post":operation("estimateRoutes","EstimateResponse","EstimateRequest")},
}
paths["/api/v1/cloud/ready"]["get"]["responses"]["503"]=response("Readiness","Model unavailable or incompatible")
for path in ["/api/v1/cloud/predict","/api/v1/cloud/hybrid/infer"]:
    for status in ["403","502","503"]:paths[path]["post"]["responses"][status]=response("Error","Privacy denial, inference failure, or unavailable model")
paths["/api/v1/cloud/hybrid/infer"]["post"]["responses"]["409"]=response("Error","Feature schema version mismatch")
paths["/api/v1/cloud/probe"]={"get":{"operationId":"cloudProbe","parameters":[{"name":"bytes","in":"query","schema":{**byte_count,"default":65536}}],"responses":{
 "200":{"description":"Exactly bytes ASCII x bytes; metadata excluded from payload length. No compression or caching.","headers":{"X-Server-Time":{"schema":timestamp},"Content-Length":{"schema":{"type":"integer"}},"Cache-Control":{"schema":{"type":"string","const":"no-store, no-transform"}}},"content":{"application/octet-stream":{"schema":{"type":"string","format":"binary"}}}},
 "400":response("Error","Invalid byte count")}}}
for path, item in paths.items():
    item["servers"]=[{"url":"http://127.0.0.1:"+("8083" if "/cloud/" in path else "8084")}]
document={"openapi":"3.1.0","info":{"title":"Person 3 proposed integration baseline","version":"0.1.0","description":"Requires team approval. Reference model is untrained. POST JSON bodies limited to 1048576 UTF-8 bytes. No deduplication or automatic remote retry."},"paths":paths,"components":{"schemas":schemas}}
(root/"openapi.yaml").write_text(yaml.safe_dump(document,sort_keys=False),encoding="utf-8")
fixtures=root/"fixtures"
fixtures.mkdir(exist_ok=True)
raw={"requestId":"req-001","taskId":"sample-001","privacyLevel":"NORMAL","samples":[0.2,-0.1,0.3,0.4,-0.2,0.1,0,0.2]}
(fixtures/"cloud-request.json").write_text(json.dumps(raw,indent=2)+"\n",encoding="utf-8")
golden=json.loads((fixtures/"golden.json").read_text(encoding="utf-8"))[-1]
hybrid={"requestId":"req-001","taskId":"sample-001","privacyLevel":"NORMAL","featureSchemaVersion":"features-v1","features":golden["features"]}
(fixtures/"hybrid-request.json").write_text(json.dumps(hybrid,indent=2)+"\n",encoding="utf-8")
estimate={"requestId":"req-estimate-001","task":{"taskId":"sample-001","taskType":"SENSOR_ANOMALY","complexity":"LOW","dataSizeBytes":len(json.dumps(raw,separators=(',',':')).encode()),"estimatedFeatureBytes":len(json.dumps(hybrid,separators=(',',':')).encode()),"estimatedResultBytes":160},
 "device":{"cpuPercent":30,"ramPercent":40,"batteryPercent":None,"source":"MEASURED"},"network":{"connected":False,"networkLatencyMs":None,"bandwidthMbps":None,"sampledAt":None,"source":"MEASURED"},
 "edgeCapable":True,"cloudCapable":True,"remainingDeadlineMs":1000,"edgeCarbonIntensityGPerKwh":500,"cloudCarbonIntensityGPerKwh":500}
(fixtures/"estimate-offline-request.json").write_text(json.dumps(estimate,indent=2)+"\n",encoding="utf-8")
print("Generated proposed OpenAPI and HTTP request fixtures")
