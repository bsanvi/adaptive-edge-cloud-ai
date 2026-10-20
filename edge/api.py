from fastapi import FastAPI

from edge.models import EdgeTaskRequest
from edge.monitor import get_edge_metrics
from edge.service import run_edge_task


app = FastAPI(
    title="Edge Component API",
    description="API for the Intelligent Edge-Cloud Computing Framework",
    version="1.0.0",
)


@app.get("/health")
def health_check():
    """
    Check whether the Edge component is running.
    """
    return {
        "status": "ok",
        "component": "edge"
    }


@app.get("/edge/metrics")
def edge_metrics():
    """
    Return the current Edge device metrics.
    """
    return get_edge_metrics()


@app.post("/edge/execute")
def execute_edge_task(request: EdgeTaskRequest):
    """
    Execute a task locally on the Edge device.
    """

    return run_edge_task(
        task_id=request.task_id,
        number_count=request.number_count
    )