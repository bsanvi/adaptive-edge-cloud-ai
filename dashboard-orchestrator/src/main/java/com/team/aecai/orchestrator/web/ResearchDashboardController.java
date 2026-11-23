package com.team.aecai.orchestrator.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ResearchDashboardController {

    @GetMapping({"/research", "/research/"})
    public String researchDashboard() {
        return "forward:/research/index.html";
    }
}
