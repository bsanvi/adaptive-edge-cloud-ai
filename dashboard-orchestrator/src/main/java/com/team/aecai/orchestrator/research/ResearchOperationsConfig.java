package com.team.aecai.orchestrator.research;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the single-server, durable H2 queue worker only for integration demos. */
@Configuration
@Profile("integration")
@EnableScheduling
public class ResearchOperationsConfig {}
