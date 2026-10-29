package com.adaptive.decision.scoring;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class ScoringBeans {
    @Bean
    public ScoringConfiguration scoringConfiguration(
            @Value("${decision.scoring.config:classpath:decision-config.json}") String location) {
        return ScoringConfiguration.load(location);
    }
}
