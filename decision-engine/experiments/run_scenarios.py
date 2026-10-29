
import json
from pathlib import Path

from decision.engine import make_decision
from decision.models import DecisionRequest


SCENARIO_FILE = (
    Path(__file__).resolve().parent / "scenarios.json"
)


def run_scenarios():
    with open(
        SCENARIO_FILE, "r", encoding="utf-8"
    ) as file:
        scenarios = json.load(file)

    results = []

    for scenario in scenarios:
        request = DecisionRequest.model_validate(
            scenario["request"]
        )

        decision = make_decision(request)

        actual = decision["selected_location"]
        expected = scenario["expected_location"]

        passed = actual == expected

        results.append({
            "scenario": scenario["name"],
            "expected": expected,
            "actual": actual,
            "passed": passed,
            "status": decision["status"],
            "reason_code": decision["reason_code"],
        })

    return results


if __name__ == "__main__":
    for result in run_scenarios():
        status = "PASS" if result["passed"] else "FAIL"

        print(
            f"{status}: {result['scenario']} | "
            f"Expected={result['expected']} | "
            f"Actual={result['actual']}"
        )
