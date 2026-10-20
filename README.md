# Adaptive Edge-Cloud AI

An adaptive workload orchestration system that evaluates runtime conditions and selects an appropriate execution environment for processing tasks at the edge or in the cloud.

## Project Structure

- `edge/`: Core package for monitoring, execution, decision-making models, services, and API endpoints.
- `tests/`: Unit tests for the monitoring, execution, service, and API modules.
- `requirements.txt`: Python project dependencies.

## Features

- **Edge monitoring:** Collects system telemetry and identifies threshold-based alerts.
- **Workload execution:** Provides the foundation for executing processing tasks.
- **Workflow orchestration:** Coordinates monitoring, decision-making, and execution.
- **API integration:** Exposes a minimal FastAPI application for interacting with the system.
- **Testing:** Includes unit tests to verify core functionality.

## Technology Stack

- Python
- FastAPI
- Pytest

Additional edge devices, cloud services, and adaptive decision-making components can be integrated as the project develops.

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/bsanvi/adaptive-edge-cloud-ai.git
cd adaptive-edge-cloud-ai
```

### 2. Create a virtual environment

```bash
python3 -m venv .venv
source .venv/bin/activate
```

### 3. Install dependencies

```bash
pip install -r requirements.txt
```

### 4. Run the tests

```bash
pytest
```

## Core Modules

- `edge.monitor`: Collects telemetry and identifies threshold alerts.
- `edge.executor`: Handles task execution and remediation actions.
- `edge.models`: Defines the models used by the system.
- `edge.service`: Orchestrates the edge workflow.
- `edge.api`: Exposes the FastAPI application.

## Project Goal

The goal is to develop and experimentally evaluate an adaptive orchestration mechanism that considers runtime conditions and task requirements when deciding whether workloads should execute locally at the edge or remotely in the cloud.

*This project is under development.*
