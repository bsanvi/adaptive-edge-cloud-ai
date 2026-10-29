package com.team.aecai.orchestrator.research;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Fail-closed protection for research mutations. Does not replace real application auth. */
@Component
@Profile("integration")
public class ResearchOperationsAuth {
    private final String secret;
    public ResearchOperationsAuth(@Value("${research.operations.key:${P4_OPERATIONS_KEY:}}") String secret) {
        this.secret = secret == null ? "" : secret;
    }
    public void require(HttpServletRequest request) {
        if (secret.isBlank() || secret.length() < 24) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Operations disabled: set P4_OPERATIONS_KEY to a secret of at least 24 characters");
        }
        String supplied = request.getHeader("X-P4-Operations-Key");
        if (supplied == null || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Operations key required");
        }
    }
}
