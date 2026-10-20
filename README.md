# Adaptive Edge-Cloud AI

An adaptive workload orchestration system that evaluates runtime conditions and task requirements to determine whether a workload should execute locally at the edge or remotely in the cloud.

The project aims to develop and experimentally evaluate an adaptive decision mechanism that considers factors such as network conditions, device resource utilization, latency, privacy requirements, and task complexity.

## Project Structure

- `edge/`: Core package for monitoring, execution, models, services, and API endpoints.
- `tests/`: Unit tests for monitoring, execution, service, and API functionality.
- `requirements.txt`: Python project dependencies.

## Features

- **Edge monitoring:** Collects system telemetry and identifies threshold-based alerts.
- **Workload execution:** Provides components for executing processing tasks.
- **Workflow orchestration:** Coordinates monitoring, decision-making, and execution.
- **API integration:** Exposes a FastAPI application for interacting with the system.
- **Testing:** Includes automated tests for core functionality.
- **Adaptive execution:** Provides the foundation for selecting suitable execution environments based on runtime conditions and task requirements.

## Technology Stack

- Python
- FastAPI
- Pytest

Additional edge devices, cloud infrastructure, and workload-specific inference components can be integrated as the project develops.

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/bsanvi/adaptive-edge-cloud-ai.git
cd adaptive-edge-cloud-ai
```

### 2. Create and activate a virtual environment

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
- `edge.models`: Defines the system's data models.
- `edge.service`: Coordinates the edge workflow.
- `edge.api`: Exposes the FastAPI application.

## Project Goal

The goal is to build and evaluate an adaptive edge-cloud orchestration mechanism that can make execution decisions according to changing runtime conditions and workload requirements.

The project is under development. Implementation details and supported execution environments may evolve as the system progresses.
