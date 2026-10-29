
from fastapi import FastAPI

from decision.models import (
    DecisionRequest,
    DecisionResponse,
)
from decision.engine import make_decision


app = FastAPI(
    title="Intelligent Edge-Cloud Decision Engine",
    description=(
        "Rule-based and explainable workload "
        "placement Decision Engine."
    ),
    version="0.1.0",
)


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "component": "decision-engine",
    }


@app.post(
    "/decision/evaluate",
    response_model=DecisionResponse,
)
def evaluate_decision(request: DecisionRequest):
    return make_decision(request)
