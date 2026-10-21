
from enum import Enum
from pydantic import BaseModel, ConfigDict, Field
from typing import Literal


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

    
from typing import Literal


class ScoreDetails(ValidatedModel):
    score: float
    components: dict[str, float]
    weighted_contributions: dict[str, float]
    base_score: float
    device_readiness: float | None
    adjusted_score: float


class RejectionDetail(ValidatedModel):
    code: str
    message: str


class RuleResult(ValidatedModel):
    allowed_locations: list[str]
    rejected_locations: dict[str, str]
    rejection_codes: dict[str, list[str]]


class DecisionResponse(ValidatedModel):
    task_id: str
    status: Literal["selected", "no_feasible_route"]
    selected_location: Literal["edge", "cloud", "hybrid"] | None

    scores: dict[str, ScoreDetails]
    rejected_locations: dict[str, list[str]]
    rejection_details: dict[str, list[RejectionDetail]]

    rule_results: dict[str, RuleResult]

    switch_applied: bool
    switch_reason: str
    reason_code: str
    explanation: str

