
from decision.models import (
    Location,
    TaskProfile,
    ExecutionEstimates,
    NetworkMetrics,
    DecisionRequest,
)
from decision.config_loader import load_config


def check_privacy(task: TaskProfile) -> dict:
    allowed = [
        Location.EDGE,
        Location.CLOUD,
        Location.HYBRID,
    ]

    rejected = {}
    rejection_codes = {}

    if task.privacy_required:
        allowed = [Location.EDGE]

        rejected["cloud"] = (
            "Cloud rejected: task requires local processing."
        )
        rejected["hybrid"] = (
            "Hybrid rejected: privacy-safe remote "
            "processing is not verified."
        )

        rejection_codes["cloud"] = ["PRIVACY_RESTRICTION"]
        rejection_codes["hybrid"] = ["PRIVACY_RESTRICTION"]

    return {
        "allowed_locations": [
            location.value for location in allowed
        ],
        "rejected_locations": rejected,
        "rejection_codes": rejection_codes,
    }


def check_deadline(
    task: TaskProfile,
    estimates: ExecutionEstimates,
) -> dict:
    allowed = []
    rejected = {}
    rejection_codes = {}

    for location in Location:
        estimate = getattr(estimates, location.value)

        if estimate is None:
            rejected[location.value] = (
                "No execution time estimate available."
            )
            rejection_codes[location.value] = [
                "MISSING_ESTIMATE"
            ]
            continue

        if estimate.estimated_time_ms <= task.deadline_ms:
            allowed.append(location.value)
        else:
            rejected[location.value] = (
                f"Estimated time "
                f"{estimate.estimated_time_ms} ms "
                f"exceeds deadline {task.deadline_ms} ms."
            )
            rejection_codes[location.value] = [
                "DEADLINE_EXCEEDED"
            ]

    return {
        "allowed_locations": allowed,
        "rejected_locations": rejected,
        "rejection_codes": rejection_codes,
    }


def check_network(
    network: NetworkMetrics,
    max_latency_ms: float = 200.0,
    min_bandwidth_mbps: float = 5.0,
) -> dict:
    allowed = ["edge", "cloud", "hybrid"]
    rejected = {}
    rejection_codes = {}
    reasons = []
    codes = []

    if network.latency_ms > max_latency_ms:
        reasons.append(
            f"Latency {network.latency_ms} ms exceeds "
            f"limit {max_latency_ms} ms."
        )
        codes.append("NETWORK_LATENCY_EXCEEDED")

    if network.bandwidth_mbps < min_bandwidth_mbps:
        reasons.append(
            f"Bandwidth {network.bandwidth_mbps} Mbps "
            f"is below minimum {min_bandwidth_mbps} Mbps."
        )
        codes.append("NETWORK_BANDWIDTH_LOW")

    if reasons:
        allowed = ["edge"]
        message = " ".join(reasons)

        rejected["cloud"] = message
        rejected["hybrid"] = message

        rejection_codes["cloud"] = codes.copy()
        rejection_codes["hybrid"] = codes.copy()

    return {
        "allowed_locations": allowed,
        "rejected_locations": rejected,
        "rejection_codes": rejection_codes,
    }


def apply_hard_rules(
    request: DecisionRequest,
    config: dict | None = None,
) -> dict:
    if config is None:
        config = load_config()

    network_settings = config["network_rules"]

    max_latency = network_settings["max_latency_ms"]
    min_bandwidth = network_settings["min_bandwidth_mbps"]

    if (
        isinstance(max_latency, bool)
        or not isinstance(max_latency, (int, float))
        or not 0 <= max_latency < float("inf")
    ):
        raise ValueError("Invalid maximum network latency.")

    if (
        isinstance(min_bandwidth, bool)
        or not isinstance(min_bandwidth, (int, float))
        or not 0 <= min_bandwidth < float("inf")
    ):
        raise ValueError("Invalid minimum bandwidth.")

    privacy_result = check_privacy(request.task)

    deadline_result = check_deadline(
        request.task,
        request.estimates,
    )

    network_result = check_network(
        request.network,
        max_latency_ms=max_latency,
        min_bandwidth_mbps=min_bandwidth,
    )

    rule_results = {
        "privacy": privacy_result,
        "deadline": deadline_result,
        "network": network_result,
    }

    all_locations = ["edge", "cloud", "hybrid"]
    allowed = []
    rejected = {}
    rejection_details = {}

    for location in all_locations:
        reasons = []
        details = []

        for result in rule_results.values():
            if location in result["rejected_locations"]:
                reasons.append(
                    result["rejected_locations"][location]
                )

            codes = result["rejection_codes"]
            messages = result["rejected_locations"]

            if location in codes:
                for code in codes[location]:
                    details.append({
                        "code": code,
                        "message": messages[location],
                    })

        if reasons:
            rejected[location] = reasons
        else:
            allowed.append(location)

        if details:
            rejection_details[location] = details

    return {
        "allowed_locations": allowed,
        "rejected_locations": rejected,
        "rejection_details": rejection_details,
        "rule_results": rule_results,
    }
