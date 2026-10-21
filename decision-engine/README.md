
# Decision Engine — Person 2

## Overview

The Decision Engine chooses an execution location:

- Edge
- Cloud
- Hybrid

It uses hard constraints, weighted scoring, and switching
stability to make deterministic, explainable decisions.

This component does not execute tasks.

## Technology

- Python
- FastAPI
- Pydantic
- Pytest

## Installation

Run these commands from the `decision-engine` directory.

Windows PowerShell:

```powershell
py -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

## Start the Service

```powershell
python -m uvicorn decision.api:app --port 8001
```

## API Endpoints

GET /health

Returns service health information.

POST /decision/evaluate

Accepts:
- Task profile
- Edge metrics
- Network measurements
- Edge/Cloud/Hybrid execution estimates
- Previous execution location

Returns:
- Selected execution location
- Adjusted scores and score components
- Hard-rule rejection details
- Switching reason code
- Human-readable explanation

## Units

- CPU and RAM utilization: percent (0–100)
- Battery: percent (0–100) or null
- Network latency: milliseconds
- Bandwidth: Mbps
- Task data size: MB
- Execution time and deadline: milliseconds
- Energy: joules
- Carbon: grams CO2e
- Monetary cost: one agreed currency per task
- Task complexity: normalized value (0–1)

Execution-time estimates should include applicable
transfer and processing times.

Energy and carbon estimates must use consistent
measurement boundaries for all execution locations.

## Important Safety Rules

- Privacy-restricted tasks can run only on Edge.
- Routes with missing execution estimates are excluded.
- Routes exceeding the deadline are excluded.
- Network rules may exclude Cloud and Hybrid.
- Hard constraints override weighted scores.
- If no location is feasible, the response contains
  status = "no_feasible_route" and
  selected_location = null.

## Determinism

The same request and configuration produce the same
decision. Previous execution location must be supplied
explicitly if switching stability is required.

## Integration

Person 4 sends POST requests to:

http://127.0.0.1:8001/decision/evaluate

Interactive API documentation:

http://127.0.0.1:8001/docs

Person 4 is responsible for collecting upstream metrics,
executing the selected route, handling runtime failures,
and displaying results.

## Running Tests

```powershell
python -m pytest -q
```
