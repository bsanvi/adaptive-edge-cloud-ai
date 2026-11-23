# Adaptive Edge–Cloud AI — Complete Security Hardening Report

## Purpose

This document records the complete security work carried out on the Adaptive Edge–Cloud AI project, from the initial unsecured integration to the final `security-integration` branch.

It is meant as a teammate handoff: what was changed, why it matters, how the secured architecture works, how to run it, what was tested, and what remains prototype-level.

---

## 1. Starting point

The project already had the integrated workflow:

- Edge service
- Cloud service
- Route Estimator
- Python Decision Engine
- Dashboard / Orchestrator
- Purple React research dashboard

The system was functional, but security was minimal. The main issues were:

- the purple dashboard could be opened without authentication,
- dashboard APIs were accessible without a logged-in user,
- internal Edge / Cloud / Estimator APIs could be called directly,
- the Python Decision Engine had no internal authentication,
- backend services did not authenticate each other,
- browser CSRF protection was not wired into the React client,
- there was no login-specific rate limiting,
- security headers were incomplete,
- H2 secure-mode storage was not configured for encryption,
- error responses could expose too much detail,
- the default Spring Security login page did not match the project UI.

The goal was to add layered security hardening without changing the core Edge–Cloud decision logic.

---

## 2. Final security branch and commits

Final branch:

```text
security-integration
```

Important commits:

```text
5fc476c
Add secure authentication to integrated research dashboard
```

Final consolidated commit:

```text
eedd808
Complete integrated security hardening across Edge Cloud Decision and dashboard
```

The remote `security-integration` branch was verified to point to the same final commit.

---

## 3. Final security architecture

The security design now separates human authentication from internal service authentication.

```text
                    USER
                     |
                     | Username + Password
                     v
          Dashboard / Orchestrator
          ------------------------
          Spring Security
          BCrypt
          Server-side session
          CSRF
          Rate limiting
          Security headers
                     |
                     | X-Internal-Api-Key
                     v
        Edge / Cloud / Estimator / Decision
```

Trust boundaries:

- User trust → authenticated dashboard session
- Browser request trust → CSRF token
- Service trust → internal API key
- Stored data protection → encrypted H2 configuration
- Abuse resistance → rate limiting and request limits

---

## 4. Dashboard authentication

Spring Security was added to the dashboard/orchestrator module.

Unauthenticated access to:

```text
/research/
```

is redirected to:

```text
/login
```

Unauthenticated protected API requests return an authentication error instead of exposing research data.

This protects:

- live telemetry,
- execution history,
- results,
- datasets,
- research controls,
- adaptive routing operations.

---

## 5. Custom purple Edge.Cloud login page

The stock Spring Security page was replaced with a custom dark-purple login screen matching the research dashboard.

Implemented through:

```text
dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/web/SecureLoginController.java
```

The page includes:

- Edge.Cloud branding,
- purple dashboard styling,
- login failure feedback,
- logout feedback,
- responsive layout,
- CSRF token support.

The research route is preserved through:

```text
ResearchDashboardController.java
```

---

## 6. BCrypt password handling

Spring Security uses:

```text
BCryptPasswordEncoder
```

for the configured dashboard password.

Credentials come from environment variables:

```text
DASHBOARD_USERNAME
DASHBOARD_PASSWORD
```

The password is not hard-coded into the repository.

---

## 7. Server-side session security

The dashboard uses authenticated server-side sessions.

Implemented controls include:

- session fixation protection,
- session migration after login,
- invalidation on logout,
- JSESSIONID deletion on logout,
- HttpOnly cookies,
- SameSite=Strict,
- 30-minute session timeout.

Relevant secure configuration:

```yaml
server:
  servlet:
    session:
      timeout: 30m
      cookie:
        http-only: true
        same-site: strict
```

---

## 8. CSRF protection

CSRF protection was kept enabled.

A secure CSRF endpoint was added:

```text
/api/v1/security/csrf
```

Relevant backend file:

```text
dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/security/CsrfController.java
```

Frontend support was added in:

```text
research-dashboard/src/lib/security.ts
```

The React request wrappers were updated:

```text
research-dashboard/src/lib/api.ts
research-dashboard/src/lib/operationsApi.ts
```

State-changing requests such as:

```text
POST
PUT
DELETE
```

now carry the Spring-generated CSRF token.

This mitigates cross-site request forgery using an already-authenticated browser session.

---

## 9. Internal API-key authentication

Protected internal service calls now use:

```text
X-Internal-Api-Key
```

The secret is loaded through:

```text
INTERNAL_API_KEY
```

This was implemented across:

- Edge
- Cloud
- Route Estimator
- Python Decision Engine

Without the correct key, protected internal operations are rejected.

---

## 10. Constant-time secret comparison

Internal secrets are compared using safer constant-time mechanisms.

Java:

```text
MessageDigest.isEqual
```

Python:

```text
hmac.compare_digest
```

This reduces timing-based information leakage during secret comparison.

---

## 11. Edge Service hardening

Relevant files:

```text
edge-service/src/main/java/com/edgecloud/edge/security/ApiSecurityFilter.java
edge-service/src/main/java/com/edgecloud/edge/security/LegacyPathEndpointGuardFilter.java
edge-service/src/main/resources/application-secure.properties
```

Added controls include:

- internal API-key authentication,
- rate limiting,
- security headers,
- content-type checks,
- public health exceptions,
- secure-mode validation,
- legacy filesystem/path endpoint protection.

The Edge filter was also adjusted so image APIs can still accept multipart uploads rather than being incorrectly forced to JSON.

---

## 12. Cloud Service hardening

Relevant files:

```text
cloud-service/src/main/java/com/adaptive/cloud/security/ApiSecurityFilter.java
cloud-service/src/main/resources/application-secure.properties
```

Controls include:

- X-Internal-Api-Key authentication,
- constant-time comparison,
- rate limiting,
- no-store response handling,
- security headers,
- public health handling.

---

## 13. Route Estimator hardening

Relevant files:

```text
route-estimator/src/main/java/com/adaptive/estimate/security/ApiSecurityFilter.java
route-estimator/src/main/resources/application-secure.properties
```

Protected estimator operations now require the internal API key while appropriate health checks can remain available.

---

## 14. Python Decision Engine security

The Decision Engine originally exposed:

```text
POST /decision/evaluate
```

without internal authentication.

It was updated to include:

- internal API-key authentication,
- constant-time key comparison,
- request-size limits,
- request rate limiting,
- security response headers,
- public `/health` endpoint.

Relevant file:

```text
decision-engine/decision/api.py
```

Protected requests without the correct key return an authentication failure.

---

## 15. Service-to-service authentication forwarding

After securing Edge, Cloud, Estimator and Decision, the orchestrator also had to authenticate its own internal calls.

The orchestrator now forwards:

```text
X-Internal-Api-Key
```

for protected service requests.

Modified areas include:

```text
IntegratedOrchestrator.java
IntegrationServiceStatus.java
IntegrationTelemetry.java
ResearchNetworkProbeController.java
ResearchImageGatewayController.java
```

This keeps the integrated workflow functional while rejecting unauthenticated direct callers.

---

## 16. Dashboard rate limiting

A dashboard rate-limit filter was added:

```text
dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/security/RateLimitFilter.java
```

It controls:

- repeated login attempts,
- excessive API requests.

Excess requests return:

```text
429 Too Many Requests
```

with `Retry-After` where applicable.

This reduces brute-force attempts and basic request flooding.

The limiter is in-memory because this is a local research prototype.

---

## 17. Security headers

Security headers were added across secured components.

Examples:

```text
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Permissions-Policy
Content-Security-Policy
Cache-Control: no-store
```

These reduce risks such as:

- clickjacking,
- MIME sniffing,
- unnecessary browser capabilities,
- some injection scenarios,
- unwanted framing,
- sensitive response caching.

---

## 18. Content Security Policy

The dashboard security layer uses a CSP that restricts:

- scripts,
- styles,
- fonts,
- images,
- network connections,
- frames,
- form submissions.

The policy accounts for the dashboard's existing external font usage.

---

## 19. Request-size controls

Limits were added for unusually large requests.

This includes:

- dashboard API request size,
- Decision Engine request size,
- image upload limits,
- bounded telemetry response processing.

This reduces memory/resource abuse and simple denial-of-service attempts.

---

## 20. Secure image gateway

The research dashboard uses a controlled image gateway instead of sending arbitrary local filesystem paths.

Controls include:

- maximum image size,
- validated task IDs,
- validated deadlines,
- controlled multipart forwarding,
- authenticated Edge requests.

Relevant file:

```text
ResearchImageGatewayController.java
```

---

## 21. Legacy path protection

The Edge service includes:

```text
LegacyPathEndpointGuardFilter.java
```

to block unsafe legacy filesystem-style endpoints in secure mode.

This reduces risk from:

- arbitrary local path access,
- path traversal,
- unintended file exposure.

---

## 22. H2 database hardening

The secure dashboard profile uses AES-encrypted H2 file storage:

```text
jdbc:h2:file:./aecai-history-secure;CIPHER=AES;DB_CLOSE_ON_EXIT=FALSE
```

The H2 browser console is disabled in secure mode.

Relevant environment values:

```text
H2_USERNAME
H2_PASSWORD
```

This adds data-at-rest protection for stored research/execution history.

---

## 23. Reduced error leakage

Secure mode disables unnecessary error details:

```yaml
include-message: never
include-stacktrace: never
include-binding-errors: never
```

This reduces exposure of:

- internal classes,
- stack traces,
- implementation details,
- validation internals.

---

## 24. Localhost-by-default deployment

Secure configuration defaults to loopback:

```text
127.0.0.1
```

where configured.

This reduces accidental exposure to other machines on the LAN during development and demonstrations.

---

## 25. Existing research-operation authorization retained

The project already contained:

```text
ResearchOperationsAuth.java
```

using:

```text
P4_OPERATIONS_KEY
```

That mechanism was kept as an extra authorization layer for sensitive research operations.

---

## 26. Secret handling

Real secrets are kept outside Git.

Local environment values include:

```text
INTERNAL_API_KEY
DASHBOARD_USERNAME
DASHBOARD_PASSWORD
H2_PASSWORD
P4_OPERATIONS_KEY
SECURITY_RATE_LIMIT_PER_MINUTE
SECURITY_LOGIN_RATE_LIMIT_PER_MINUTE
```

Real secret values must never be committed.

---

## 27. Secure runtime profiles

Typical profiles:

```text
Edge               secure
Cloud              secure
Route Estimator    secure
Dashboard          integration,secure
Decision Engine    INTERNAL_API_KEY loaded in environment
```

---

## 28. Expected security behavior

Examples:

```text
GET /login
→ 200
```

```text
GET /research/ without login
→ redirect to /login
```

```text
GET /api/v1/integrated/telemetry without login
→ 401
```

Protected Edge / Cloud / Estimator endpoint without key:

```text
→ 401
```

With the correct key:

```text
→ 200
```

Decision health:

```text
GET /health
→ 200
```

Decision evaluate without key:

```text
POST /decision/evaluate
→ 401
```

---

## 29. Test and build work performed

### Purple React dashboard

Observed result:

```text
10 tests passed
0 failed
```

The production Vite build completed successfully and synchronized into:

```text
dashboard-orchestrator/src/main/resources/static/research/
```

### Python Decision Engine

Observed result:

```text
90 tests passed
```

### Java integration issue found and fixed

After adding internal-api-key constructor parameters, existing Java tests initially failed to compile because they still called the older constructors.

Backward-compatible constructors were added so the old integration tests could continue using the original signatures.

### Maven structure issue identified

The repository root Maven reactor contains:

```text
cloud-service
route-estimator
```

Edge is a standalone Maven project.

Correct test commands are therefore:

```text
mvn -f edge-service/pom.xml clean test
```

and:

```text
./mvnw -pl cloud-service,route-estimator clean test
```

rather than trying to include Edge in the root reactor.

---

## 30. Main files added or modified

Important security-related files include:

```text
dashboard-orchestrator/pom.xml

dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/security/
    SecurityConfig.java
    InsecureSecurityConfig.java
    CsrfController.java
    RateLimitFilter.java

dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/web/
    SecureLoginController.java
    ResearchDashboardController.java

dashboard-orchestrator/src/main/resources/application-secure.yml

dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/integration/
    IntegratedOrchestrator.java
    IntegrationServiceStatus.java
    IntegrationTelemetry.java

dashboard-orchestrator/src/main/java/com/team/aecai/orchestrator/research/
    ResearchNetworkProbeController.java
    ResearchImageGatewayController.java

research-dashboard/src/lib/
    security.ts
    api.ts
    operationsApi.ts

edge-service/src/main/java/com/edgecloud/edge/security/
    ApiSecurityFilter.java
    LegacyPathEndpointGuardFilter.java

edge-service/src/main/resources/application-secure.properties

cloud-service/src/main/java/com/adaptive/cloud/security/
    ApiSecurityFilter.java

cloud-service/src/main/resources/application-secure.properties

route-estimator/src/main/java/com/adaptive/estimate/security/
    ApiSecurityFilter.java

route-estimator/src/main/resources/application-secure.properties

decision-engine/decision/api.py
```

---

## 31. Attack classes these measures help mitigate

The controls reduce risk from:

- unauthorized dashboard access,
- direct internal API abuse,
- brute-force login attempts,
- CSRF,
- session fixation,
- clickjacking,
- MIME sniffing,
- basic request flooding,
- oversized-request abuse,
- unsafe file/path access,
- database-file exposure,
- verbose error/information leakage.

The system should not be described as unhackable.

---

## 32. Prototype limitation

This is:

> layered security hardening for an experimental Edge–Cloud research prototype.

It is not a claim of full production-grade security.

A production version should additionally use:

- HTTPS with trusted certificates,
- mTLS or OAuth2 workload identities,
- a real secret manager,
- distributed rate limiting,
- role-based access control,
- centralized audit logging,
- production database infrastructure,
- reverse proxy / WAF,
- firewall and network segmentation,
- automatic secret rotation,
- dependency/security scanning.

---

## 33. Short teammate/evaluator explanation

> The original integration was focused mainly on functionality, so the dashboard and backend service APIs had minimal access control. The security hardening added Spring Security authentication for the dashboard, BCrypt password handling, secure sessions, CSRF protection, rate limiting, security headers, encrypted H2 configuration, request-size controls, internal API-key authentication for Edge, Cloud, Route Estimator and Decision Engine, and authenticated service-to-service communication. These measures mitigate common risks such as unauthorized access, brute-force attempts, CSRF, direct API abuse, clickjacking, oversized-request abuse and information leakage. It is security hardening for the research prototype rather than a claim of complete production-grade security.

---

## 34. Final status

Final security branch:

```text
security-integration
```

Final consolidated security commit:

```text
eedd808
Complete integrated security hardening across Edge Cloud Decision and dashboard
```

The working tree was clean after the final commit, and the remote branch was verified at the same commit.

This document should be committed to the `security-integration` branch so the entire team has one reference for the completed security implementation.
