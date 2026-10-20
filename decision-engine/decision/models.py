
from enum import Enum
from pydantic import BaseModel, ConfigDict, Field


class ValidatedModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


class Location(str, Enum):
    EDGE = "edge"
    CLOUD = "cloud"
    HYBRID = "hybrid"


class TaskProfile(ValidatedModel):
    task_id: str = Field(min_length=1)
    task_type: str = Field(min_length=1)
    data_size_mb: float = Field(ge=0)
    complexity: float = Field(ge=0, le=1)
    deadline_ms: float = Field(gt=0)
    privacy_required: bool


class EdgeMetrics(ValidatedModel):
    cpu_percent: float = Field(ge=0, le=100)
    ram_percent: float = Field(ge=0, le=100)
    battery_percent: float | None = Field(
        default=None, ge=0, le=100
    )
    power_plugged: bool | None = None


class NetworkMetrics(ValidatedModel):
    latency_ms: float = Field(ge=0)
    bandwidth_mbps: float = Field(ge=0)


class RouteEstimate(ValidatedModel):
    estimated_time_ms: float = Field(ge=0)
    estimated_energy_j: float = Field(ge=0)
    estimated_cost: float = Field(ge=0)
    estimated_carbon_g: float = Field(ge=0)


class ExecutionEstimates(ValidatedModel):
    edge: RouteEstimate | None = None
    cloud: RouteEstimate | None = None
    hybrid: RouteEstimate | None = None


class DecisionRequest(ValidatedModel):
    task: TaskProfile
    edge: EdgeMetrics
    network: NetworkMetrics
    estimates: ExecutionEstimates
    previous_location: Location | None = None
