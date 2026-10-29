
from decision.models import DecisionRequest, Location
from decision.rules import apply_hard_rules
from decision.scorer import score_location
from decision.config_loader import load_config
from decision.switching import apply_switching
from decision.explainer import explain_decision


def make_decision(
    request: DecisionRequest,
    config: dict | None = None,
) -> dict:

    if config is None:
        config = load_config()

    # Step 1: Apply hard constraints
    rule_result = apply_hard_rules(request, config)

    allowed = rule_result["allowed_locations"]
    rejected = rule_result["rejected_locations"]

    # Step 2: Handle no feasible routes
    
    if not allowed:
        result = {
            "task_id": request.task.task_id,
            "status": "no_feasible_route",
            "selected_location": None,
            "scores": {},
            "rejected_locations": rejected,
            "rule_results": rule_result["rule_results"],
            "switch_applied": False,
            "switch_reason": "No feasible location.",
            "reason_code": "NO_FEASIBLE_ROUTE",
            "rejection_details": rule_result["rejection_details"],
        }

        result["explanation"] = explain_decision(result)
        return result


    # Step 3: Score only eligible routes
    scores = {}

    for location_name in allowed:
        location = Location(location_name)

        estimate = getattr(
            request.estimates, location_name
        )

        result = score_location(
            location,
            estimate,
            request.edge,
            config,
        )

        scores[location_name] = result

    
    # Step 4: Apply switching stability
    switching_result = apply_switching(
        scores=scores,
        previous_location=request.previous_location,
        minimum_improvement=(
            config["switching"]["minimum_improvement"]
        ),
    )

    selected = switching_result["selected_location"]


    # Step 5: Return structured decision
    
    result = {
        "task_id": request.task.task_id,
        "status": "selected",
        "selected_location": selected,
        "scores": scores,
        "rejected_locations": rejected,
        "rule_results": rule_result["rule_results"],
        "switch_applied": switching_result["switch_applied"],
        "switch_reason": switching_result["reason"],
        "reason_code": switching_result["reason_code"],
        "rejection_details": rule_result["rejection_details"],
    }

    result["explanation"] = explain_decision(result)

    return result


