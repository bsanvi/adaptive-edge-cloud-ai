
import pytest
from pydantic import ValidationError

from decision.engine import make_decision
from decision.models import DecisionResponse
from test_engine import make_request


def test_selected_response_is_valid():
    request = make_request()

    result = make_decision(request)

    response = DecisionResponse.model_validate(result)

    assert response.status == "selected"
    assert response.selected_location is not None


def test_no_feasible_response_is_valid():
    request = make_request(
        privacy=True,
        deadline=100.0,
        edge_time=1000.0,
    )

    result = make_decision(request)

    response = DecisionResponse.model_validate(result)

    assert response.status == "no_feasible_route"
    assert response.selected_location is None


def test_invalid_response_rejected():
    with pytest.raises(ValidationError):
        DecisionResponse.model_validate({
            "task_id": "task_001",
            "status": "selected",
            "selected_location": "invalid",
        })
