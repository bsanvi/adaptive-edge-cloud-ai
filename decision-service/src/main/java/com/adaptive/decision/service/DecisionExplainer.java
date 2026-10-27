
package com.adaptive.decision.service;

import java.util.ArrayList;
import java.util.List;

public final class DecisionExplainer {

    private DecisionExplainer() {
    }

    public static List<String> explain(
            RoutingResult result) {

        if (result == null || result.evaluations() == null) {
            throw new IllegalArgumentException(
                "Routing result is required"
            );
        }

        List<String> reasons = new ArrayList<>();

        if ("NO_FEASIBLE_ROUTE".equals(result.status())) {
            reasons.add(
                "No execution route satisfies all constraints"
            );
        } else if ("ROUTE_SELECTED".equals(result.status())
                && result.selectedRoute() != null) {

            reasons.add(
                result.selectedRoute()
                + " selected as a feasible route "
                + "after scoring and switching policy"
            );
        } else {
            throw new IllegalArgumentException(
                "Invalid routing result status"
            );
        }

        for (RoutingResult.RouteResult evaluation
                : result.evaluations()) {

            if (evaluation == null
                    || evaluation.rejectionReasons() == null) {
                throw new IllegalArgumentException(
                    "Invalid route evaluation"
                );
            }

            if (!evaluation.feasible()) {

                for (String rejection
                        : evaluation.rejectionReasons()) {

                    reasons.add(
                        evaluation.route()
                        + " rejected: "
                        + rejection
                    );
                }
            }
        }

        return List.copyOf(reasons);
    }
}
