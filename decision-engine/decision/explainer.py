
def explain_decision(result: dict) -> str:
    """
    Create a human-readable explanation
    from a Decision Engine result.
    """

    selected = result["selected_location"]
    rejected = result["rejected_locations"]

    # Case 1: No execution location is feasible
    if result["status"] == "no_feasible_route":
        message = (
            "No feasible execution location was found. "
            "All available routes failed one or more "
            "hard constraints. "
        )

    # Case 2: A location has been selected
    else:
        score = result["scores"][selected][
            "adjusted_score"
        ]

        message = (
            f"{selected.capitalize()} selected "
            f"with an adjusted score of {score:.4f}. "
        )

        reason_code = result.get("reason_code")

        # Support older results without reason_code
        if reason_code is None:
            switch_reason = result.get(
                "switch_reason", ""
            )

            if "too small" in switch_reason:
                reason_code = "SWITCH_AVOIDED"

            elif "No previous location" in switch_reason:
                reason_code = "BEST_SCORE"

            elif result.get("switch_applied"):
                reason_code = "SCORE_IMPROVEMENT"

        # Explain why this route was selected
        if reason_code == "BEST_SCORE":
            message += (
                "The highest-scoring feasible "
                "location was selected. "
            )

        elif reason_code == "SWITCH_AVOIDED":
            message += (
                "The previous location was retained "
                "to avoid unnecessary switching. "
            )

        elif reason_code == "SCORE_IMPROVEMENT":
            message += (
                "The engine switched because another "
                "location was sufficiently better. "
            )

        elif reason_code == "PREVIOUS_INFEASIBLE":
            message += (
                "The previous location was infeasible, "
                "so a feasible alternative was selected. "
            )

    # Explain why other locations were rejected
    
    for location, reasons in rejected.items():
        prefix = f"{location.capitalize()} rejected:"

        for reason in reasons:
            if reason.lower().startswith(prefix.lower()):
                message += f"{reason} "
            else:
                message += f"{prefix} {reason} "


    return message.strip()
