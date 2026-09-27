"""Smoke-test built executable jars on ephemeral loopback ports against OpenAPI.

Run after Maven verify. Requires contract-requirements.txt, JDK 25 and no running services.
Only this script's own subprocesses are stopped. Never changes firewall configuration.
"""
from pathlib import Path
import json
import os
import re
import shutil
import subprocess
import time
import urllib.request
import urllib.error
import yaml
from jsonschema import Draft202012Validator, FormatChecker

ROOT=Path(__file__).resolve().parents[2]
SPEC=yaml.safe_load((ROOT/"contracts/openapi.yaml").read_text(encoding="utf-8"))
TOOLS=ROOT/".tools"
TOOLS.mkdir(exist_ok=True)
JAVA=shutil.which("java")
if not JAVA:raise RuntimeError("Java not found on PATH")

def request(base,path,body=None):
    data=None if body is None else json.dumps(body,separators=(',',':')).encode()
    req=urllib.request.Request(base+path,data=data,headers={} if body is None else {"Content-Type":"application/json"})
    try:response=urllib.request.urlopen(req,timeout=10)
    except urllib.error.HTTPError as error:response=error
    with response:
        raw=response.read()
        if "application/json" in response.headers.get("Content-Type",""):
            parsed=json.loads(raw)
            operation=SPEC["paths"][path.split('?')[0]]["get" if body is None else "post"]
            schema=operation["responses"][str(response.status)]["content"]["application/json"]["schema"]
            Draft202012Validator({**schema,"components":SPEC["components"]},format_checker=FormatChecker()).validate(parsed)
            return response.status,parsed
        return response.status,raw

processes=[]
try:
    bases={}
    for module in ["cloud-service","route-estimator"]:
        jar=ROOT/module/"target"/(module+"-0.1.0-SNAPSHOT.jar")
        if not jar.exists():raise RuntimeError("Run Maven verify first: missing "+str(jar))
        log=TOOLS/(module+"-smoke.log")
        with log.open("w",encoding="utf-8") as output:
            process=subprocess.Popen([JAVA,"-jar",str(jar),"--server.port=0","--server.address=127.0.0.1"],cwd=ROOT,
                stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=="nt" else 0)
        processes.append(process)
        deadline=time.monotonic()+45
        while time.monotonic()<deadline:
            contents=log.read_text(encoding="utf-8",errors="replace")
            match=re.search(r"Tomcat started on port (\d+)",contents)
            if match:
                bases[module]="http://127.0.0.1:"+match[1]
                break
            if process.poll() is not None:raise RuntimeError(module+" exited; inspect "+str(log))
            time.sleep(0.2)
        else:raise RuntimeError(module+" startup timed out; inspect "+str(log))
    cloud=bases["cloud-service"]
    estimator=bases["route-estimator"]
    assert request(cloud,"/api/v1/cloud/health")[0]==200
    assert request(cloud,"/api/v1/cloud/ready")[0]==200
    assert request(estimator,"/api/v1/estimate/health")[0]==200
    assert request(cloud,"/api/v1/cloud/probe?bytes=1024")== (200,b'x'*1024)
    raw=json.loads((ROOT/"contracts/fixtures/cloud-request.json").read_text())
    hybrid=json.loads((ROOT/"contracts/fixtures/hybrid-request.json").read_text())
    status,prediction=request(cloud,"/api/v1/cloud/predict",raw)
    assert status==200
    status,output=request(cloud,"/api/v1/cloud/hybrid/infer",hybrid)
    assert status==200
    assert abs(prediction["confidence"]-output["classProbabilities"][prediction["prediction"]])<1e-5
    assert request(cloud,"/api/v1/cloud/predict",{**raw,"privacyLevel":"CRITICAL"})[0]==403
    estimate=json.loads((ROOT/"contracts/fixtures/estimate-offline-request.json").read_text())
    status,result=request(estimator,"/api/v1/estimate/routes",estimate)
    assert status==200 and result["estimates"]["EDGE"]["estimateAvailable"]
    assert result["estimates"]["CLOUD"]["reason"]=="NETWORK_UNAVAILABLE"
    from datetime import datetime,timezone
    estimate["network"]={"connected":True,"networkLatencyMs":10,"bandwidthMbps":30,"sampledAt":datetime.now(timezone.utc).isoformat(),"source":"MEASURED"}
    status,result=request(estimator,"/api/v1/estimate/routes",estimate)
    assert status==200 and all(value["estimateAvailable"] for value in result["estimates"].values())
    print("Packaged services: readiness, probe, raw/Hybrid equivalence, privacy rejection, offline/online estimates and response schemas passed.")
finally:
    for process in reversed(processes):
        if process.poll() is None:
            process.terminate()
            try:process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
