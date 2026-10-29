# Edge Service — Adaptive Edge-Cloud AI

## Overview

The Edge Service is the edge-side component of the Adaptive Edge-Cloud AI framework.

It is responsible for:

- Device monitoring
- Task profiling
- Local AI inference
- Hybrid preprocessing
- Hybrid postprocessing
- Edge energy estimation
- REST API integration

The Edge Service does not decide whether a task runs on EDGE, CLOUD, or HYBRID. That responsibility belongs to the Decision Engine.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Maven
- OpenCV
- ONNX
- SqueezeNet 1.1
- OSHI
- JUnit 5
- Mockito
- MockMvc

## Port

The Edge Service runs on:

http://localhost:8081

## Running the Service

From the repository:

    cd edge-service
    ./mvnw clean test
    ./mvnw spring-boot:run

Final validation:

    Tests run: 32
    Failures: 0
    Errors: 0
    Skipped: 0
    BUILD SUCCESS

## Integration APIs

    GET  /api/v1/edge/health
    GET  /api/v1/edge/metrics

    POST /api/v1/edge/profile
    POST /api/v1/edge/predict
    POST /api/v1/edge/hybrid/preprocess
    POST /api/v1/edge/hybrid/postprocess
    POST /api/v1/edge/energy/estimate

## Device Monitoring

The Edge Service monitors:

- CPU usage
- RAM usage
- RAM used and available
- Battery percentage
- Power connection state
- Timestamp

Endpoint:

    GET /api/v1/edge/metrics

## Local AI Inference

Endpoint:

    POST /api/v1/edge/predict

The service performs image classification using:

- SqueezeNet 1.1
- ONNX model format
- OpenCV DNN
- ImageNet labels

Before inference, images are resized to 224 x 224 and normalized using ImageNet preprocessing.

The response includes:

- predicted class index
- predicted label
- confidence
- execution time
- status

## Task Profiling

Endpoint:

    POST /api/v1/edge/profile

The profiler records:

- input file size
- image width
- image height
- total pixels
- complexity
- deadline
- privacy level
- priority profile

Complexity rules:

LOW:
- pixels <= 250,000
- file size <= 1 MB

MEDIUM:
- pixels <= 2,000,000
- file size <= 5 MB

HIGH:
- anything above the previous thresholds

Verified results:

    200 x 200
    40,000 pixels
    LOW

    800 x 600
    480,000 pixels
    MEDIUM

    2000 x 1200
    2,400,000 pixels
    HIGH

These complexity levels are explainable engineering heuristics and are not ML predictions.

## Hybrid Preprocessing

Endpoint:

    POST /api/v1/edge/hybrid/preprocess

The Edge Service preprocesses data before Cloud execution.

Verified experiment:

    Original resolution: 800 x 600
    Processed resolution: 224 x 224

    Original size: 15066 bytes
    Processed size: 5909 bytes

    Size reduction: 60.78%
    Processing time: 0.748 ms

This can reduce the communication payload sent to the Cloud.

## Hybrid Postprocessing

Endpoint:

    POST /api/v1/edge/hybrid/postprocess

The Cloud can return a class index and confidence value.

The Edge Service maps the class index to a human-readable ImageNet label.

Verified experiment:

    Class index: 714
    Predicted label: pick
    Confidence: 0.1098
    Processing time: 0.005 ms

## Edge Energy Estimation

Endpoint:

    POST /api/v1/edge/energy/estimate

Prototype assumptions:

    Idle power = 5 W
    Maximum power = 30 W

Energy is estimated using:

    Energy = Power x Execution Time

The energy value is estimated and is not a direct hardware power measurement.

## Baseline Experiment

Normal Edge inference:

    CPU before: 10.5%
    CPU after: 11.0%
    Average CPU: 10.75%

    Inference time: 10.482 ms

    Estimated power: 7.688 W
    Estimated energy: 0.08058 J

## CPU Stress Experiment

Under artificial CPU stress:

    CPU before: 24.2%
    CPU after: 24.7%
    Average CPU: 24.45%

    Inference time: 11.58 ms

The prediction remained the same while execution time increased under higher system load.

## Stress Tools

CPU stress:

    java tools/CpuStress.java

RAM stress:

    java tools/RamStress.java

These utilities are used only for controlled experiments.

## Component Separation

### Edge Service

Responsible for:

- Device metrics
- Task profiling
- Local inference
- Hybrid preprocessing
- Hybrid postprocessing
- Edge energy estimation

### Decision Engine

Responsible for:

- Privacy rules
- Deadline rules
- Network constraints
- Weighted scoring
- Switching logic
- EDGE / CLOUD / HYBRID selection
- Decision explanations

### Cloud Service

Responsible for Cloud-side processing and Cloud-related estimates.

### Orchestrator

Responsible for coordinating the services and connecting the system to the dashboard.

## Final Status

Completed:

- Device monitoring
- Local image processing
- Local ML inference
- Task profiling
- Hybrid preprocessing
- Hybrid postprocessing
- Edge energy estimation
- REST integration APIs
- CPU stress experiments
- Automated tests

Final validation:

    Tests run: 32
    Failures: 0
    Errors: 0
    Skipped: 0
    BUILD SUCCESS

The Edge Service is ready for integration testing with the Decision Engine, Cloud Service, and Orchestrator.
