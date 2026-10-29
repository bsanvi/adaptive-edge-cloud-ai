#!/usr/bin/env python3

import base64
import csv
import json
import math
import os
import statistics
import sys
import time
import urllib.error
import urllib.request

from datetime import datetime, timezone
from pathlib import Path


BASE_URL = os.environ.get(
    "EDGE_BASE_URL",
    "http://localhost:8081"
).rstrip("/")

MODEL_VERSION = "squeezenet1.1-7-image-v2"

EXPECTED_MODEL_SHA = (
    "1eeff551a67ae8d565ca33b572fc4b66"
    "e3ef357b0eb2863bb9ff47a918cc4088"
)

EXPECTED_LABEL_SHA = (
    "1f386e0d1cb6e28b9c2dac651c3dea6"
    "801e98ad1b41a14ce6bb1a093d72069f5"
)

WARMUPS = 5
MEASURED_RUNS = 30

WORKLOADS = [
    ("LOW", Path("/tmp/edge-test/low.jpg")),
    ("MEDIUM", Path("/tmp/edge-test/input.jpg")),
    ("HIGH", Path("/tmp/edge-test/high.jpg")),
]

OUTPUT_DIR = Path("benchmarks/v2")


def http_json(method, path, payload=None, timeout=60):
    url = BASE_URL + path

    data = None
    headers = {}

    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method,
    )

    start = time.perf_counter()

    try:
        with urllib.request.urlopen(
            request,
            timeout=timeout,
        ) as response:
            raw = response.read()

            elapsed_ms = (
                time.perf_counter() - start
            ) * 1000.0

            body = json.loads(
                raw.decode("utf-8")
            )

            return body, elapsed_ms

    except urllib.error.HTTPError as exc:
        raw = exc.read().decode(
            "utf-8",
            errors="replace",
        )

        raise RuntimeError(
            f"HTTP {exc.code} from {path}: {raw}"
        ) from exc

    except urllib.error.URLError as exc:
        raise RuntimeError(
            f"Could not reach {url}: {exc}"
        ) from exc


def image_payload(request_id, image_path):
    image_bytes = image_path.read_bytes()

    return {
        "requestId": request_id,
        "imageBase64": base64.b64encode(
            image_bytes
        ).decode("ascii"),
        "modelVersion": MODEL_VERSION,
        "privacy": "NORMAL",
    }


def p95(values):
    if not values:
        return None

    ordered = sorted(values)

    index = max(
        0,
        math.ceil(0.95 * len(ordered)) - 1,
    )

    return ordered[index]


def safe_mean(a, b):
    values = [
        value
        for value in (a, b)
        if isinstance(value, (int, float))
    ]

    if not values:
        return None

    return statistics.mean(values)


def numeric(values):
    return [
        float(value)
        for value in values
        if isinstance(value, (int, float))
    ]


def summarize_metric(
    rows,
    workload,
    metric,
    attempts,
):
    successful_rows = [
        row
        for row in rows
        if row["workload"] == workload
        and row["status"] == "SUCCESS"
    ]

    values = numeric(
        [
            row.get(metric)
            for row in successful_rows
        ]
    )

    errors = attempts - len(successful_rows)

    if not values:
        return {
            "workload": workload,
            "attempts": attempts,
            "successes": len(successful_rows),
            "errors": errors,
            "metric": metric,
            "n": 0,
            "mean": "",
            "median": "",
            "p95": "",
            "min": "",
            "max": "",
            "stddev": "",
        }

    return {
        "workload": workload,
        "attempts": attempts,
        "successes": len(successful_rows),
        "errors": errors,
        "metric": metric,
        "n": len(values),
        "mean": statistics.mean(values),
        "median": statistics.median(values),
        "p95": p95(values),
        "min": min(values),
        "max": max(values),
        "stddev": (
            statistics.stdev(values)
            if len(values) > 1
            else 0.0
        ),
    }


def main():
    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    print(
        f"Edge v2 formal benchmark"
    )
    print(
        f"Base URL: {BASE_URL}"
    )
    print(
        f"Warm-ups: {WARMUPS}"
    )
    print(
        f"Measured runs/workload: {MEASURED_RUNS}"
    )

    print("\nChecking Edge capability...")

    capability, ready_http_ms = http_json(
        "GET",
        "/api/v2/edge/ready",
    )

    if not capability.get("ready"):
        raise RuntimeError(
            f"Edge model is not ready: {capability}"
        )

    if (
        capability.get("modelVersion")
        != MODEL_VERSION
    ):
        raise RuntimeError(
            "Unexpected model version: "
            + str(
                capability.get(
                    "modelVersion"
                )
            )
        )

    if (
        capability.get("modelSha256")
        != EXPECTED_MODEL_SHA
    ):
        raise RuntimeError(
            "Model SHA-256 does not match "
            "the frozen v2 contract"
        )

    if (
        capability.get("labelsSha256")
        != EXPECTED_LABEL_SHA
    ):
        raise RuntimeError(
            "Label SHA-256 does not match "
            "the frozen v2 contract"
        )

    print("Capability check: PASS")
    print(
        "Environment:",
        capability.get("environmentId"),
    )

    for workload, path in WORKLOADS:
        if not path.exists():
            raise FileNotFoundError(
                f"Missing {workload} image: {path}"
            )

    profiles = {}

    print("\nProfiling workloads...")

    for workload, path in WORKLOADS:
        payload = image_payload(
            f"PROFILE-{workload}",
            path,
        )

        profile_response, profile_http_ms = (
            http_json(
                "POST",
                "/api/v2/edge/profile",
                payload,
            )
        )

        profile = profile_response["profile"]

        profiles[workload] = {
            "profile": profile,
            "profilingMs": (
                profile_response[
                    "profilingMs"
                ]
            ),
            "profileHttpMs": (
                profile_http_ms
            ),
        }

        actual = profile["complexity"]

        if actual != workload:
            raise RuntimeError(
                f"{workload} image was "
                f"classified as {actual}"
            )

        print(
            f"{workload}: "
            f"{profile['width']}x"
            f"{profile['height']}, "
            f"{profile['pixelCount']} pixels, "
            f"{profile['fileSizeBytes']} bytes, "
            f"complexity={actual}"
        )

    print("\nRunning warm-ups...")

    for workload, path in WORKLOADS:
        payload = image_payload(
            f"WARMUP-{workload}",
            path,
        )

        for warmup in range(
            1,
            WARMUPS + 1,
        ):
            http_json(
                "POST",
                "/api/v2/edge/predict",
                payload,
            )

            http_json(
                "POST",
                "/api/v2/edge/hybrid/preprocess",
                payload,
            )

            print(
                f"{workload} warm-up "
                f"{warmup}/{WARMUPS}"
            )

    rows = []

    print("\nStarting measured runs...")

    for workload, path in WORKLOADS:

        print(
            f"\n===== {workload} ====="
        )

        for run in range(
            1,
            MEASURED_RUNS + 1,
        ):

            row = {
                "workload": workload,
                "run": run,
                "status": "ERROR",
                "error": "",
            }

            try:
                request_id = (
                    f"EDGE-IDLE-{workload}-{run:02d}"
                )

                payload = image_payload(
                    request_id,
                    path,
                )

                before, telemetry_before_http_ms = (
                    http_json(
                        "GET",
                        "/api/v2/edge/telemetry",
                    )
                )

                prediction, predict_http_ms = (
                    http_json(
                        "POST",
                        "/api/v2/edge/predict",
                        payload,
                    )
                )

                prepared, hybrid_http_ms = (
                    http_json(
                        "POST",
                        "/api/v2/edge/hybrid/preprocess",
                        payload,
                    )
                )

                after, telemetry_after_http_ms = (
                    http_json(
                        "GET",
                        "/api/v2/edge/telemetry",
                    )
                )

                stages = prediction["stages"]

                profile_data = (
                    profiles[workload]
                )

                row.update(
                    {
                        "status": "SUCCESS",

                        "file_size_bytes":
                            profile_data[
                                "profile"
                            ][
                                "fileSizeBytes"
                            ],

                        "width":
                            profile_data[
                                "profile"
                            ]["width"],

                        "height":
                            profile_data[
                                "profile"
                            ]["height"],

                        "pixel_count":
                            profile_data[
                                "profile"
                            ][
                                "pixelCount"
                            ],

                        "complexity":
                            profile_data[
                                "profile"
                            ][
                                "complexity"
                            ],

                        "profiling_ms":
                            profile_data[
                                "profilingMs"
                            ],

                        "profile_http_ms":
                            profile_data[
                                "profileHttpMs"
                            ],

                        "predict_http_ms":
                            predict_http_ms,

                        "predict_preprocessing_ms":
                            stages[
                                "preprocessingMs"
                            ],

                        "predict_inference_ms":
                            stages[
                                "inferenceMs"
                            ],

                        "predict_postprocessing_ms":
                            stages[
                                "postprocessingMs"
                            ],

                        "hybrid_preprocess_http_ms":
                            hybrid_http_ms,

                        "hybrid_preprocessing_ms":
                            prepared[
                                "preprocessingMs"
                            ],

                        "prepared_bytes":
                            prepared[
                                "preparedBytes"
                            ],

                        "cpu_before":
                            before.get(
                                "cpuPercent"
                            ),

                        "cpu_after":
                            after.get(
                                "cpuPercent"
                            ),

                        "cpu_mean":
                            safe_mean(
                                before.get(
                                    "cpuPercent"
                                ),
                                after.get(
                                    "cpuPercent"
                                ),
                            ),

                        "ram_before":
                            before.get(
                                "ramPercent"
                            ),

                        "ram_after":
                            after.get(
                                "ramPercent"
                            ),

                        "ram_mean":
                            safe_mean(
                                before.get(
                                    "ramPercent"
                                ),
                                after.get(
                                    "ramPercent"
                                ),
                            ),

                        "battery_before":
                            before.get(
                                "batteryPercent"
                            ),

                        "battery_after":
                            after.get(
                                "batteryPercent"
                            ),

                        "power_plugged_before":
                            before.get(
                                "powerPlugged"
                            ),

                        "power_plugged_after":
                            after.get(
                                "powerPlugged"
                            ),

                        "telemetry_before_http_ms":
                            telemetry_before_http_ms,

                        "telemetry_after_http_ms":
                            telemetry_after_http_ms,

                        "class_index":
                            prediction[
                                "classIndex"
                            ],

                        "label":
                            prediction[
                                "label"
                            ],

                        "confidence":
                            prediction[
                                "confidence"
                            ],
                    }
                )

                print(
                    f"{workload} "
                    f"{run:02d}/{MEASURED_RUNS}: "
                    f"inference="
                    f"{stages['inferenceMs']:.3f} ms, "
                    f"HTTP="
                    f"{predict_http_ms:.3f} ms"
                )

            except Exception as exc:
                row["error"] = str(exc)

                print(
                    f"{workload} "
                    f"{run:02d}/{MEASURED_RUNS}: "
                    f"ERROR: {exc}",
                    file=sys.stderr,
                )

            rows.append(row)

    timestamp = datetime.now(
        timezone.utc
    ).strftime(
        "%Y%m%dT%H%M%SZ"
    )

    runs_file = (
        OUTPUT_DIR
        / f"edge_v2_idle_runs_{timestamp}.csv"
    )

    summary_file = (
        OUTPUT_DIR
        / f"edge_v2_idle_summary_{timestamp}.csv"
    )

    metadata_file = (
        OUTPUT_DIR
        / f"edge_v2_idle_metadata_{timestamp}.json"
    )

    all_fields = [
        "workload",
        "run",
        "status",
        "error",
        "file_size_bytes",
        "width",
        "height",
        "pixel_count",
        "complexity",
        "profiling_ms",
        "profile_http_ms",
        "predict_http_ms",
        "predict_preprocessing_ms",
        "predict_inference_ms",
        "predict_postprocessing_ms",
        "hybrid_preprocess_http_ms",
        "hybrid_preprocessing_ms",
        "prepared_bytes",
        "cpu_before",
        "cpu_after",
        "cpu_mean",
        "ram_before",
        "ram_after",
        "ram_mean",
        "battery_before",
        "battery_after",
        "power_plugged_before",
        "power_plugged_after",
        "telemetry_before_http_ms",
        "telemetry_after_http_ms",
        "class_index",
        "label",
        "confidence",
    ]

    with runs_file.open(
        "w",
        newline="",
        encoding="utf-8",
    ) as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=all_fields,
        )

        writer.writeheader()

        for row in rows:
            writer.writerow(row)

    metrics = [
        "predict_http_ms",
        "predict_preprocessing_ms",
        "predict_inference_ms",
        "predict_postprocessing_ms",
        "hybrid_preprocess_http_ms",
        "hybrid_preprocessing_ms",
        "cpu_mean",
        "ram_mean",
        "prepared_bytes",
    ]

    summaries = []

    for workload, _ in WORKLOADS:
        for metric in metrics:
            summaries.append(
                summarize_metric(
                    rows,
                    workload,
                    metric,
                    MEASURED_RUNS,
                )
            )

    summary_fields = [
        "workload",
        "attempts",
        "successes",
        "errors",
        "metric",
        "n",
        "mean",
        "median",
        "p95",
        "min",
        "max",
        "stddev",
    ]

    with summary_file.open(
        "w",
        newline="",
        encoding="utf-8",
    ) as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=summary_fields,
        )

        writer.writeheader()
        writer.writerows(summaries)

    metadata = {
        "timestampUtc": timestamp,
        "condition": "IDLE_NORMAL",
        "baseUrl": BASE_URL,
        "warmups": WARMUPS,
        "measuredRunsPerWorkload":
            MEASURED_RUNS,
        "readyHttpMs": ready_http_ms,
        "capability": capability,
        "profiles": profiles,
        "notes": [
            (
                "CPU and RAM are OSHI "
                "whole-system measurements."
            ),
            (
                "HTTP timings include local "
                "REST/client overhead."
            ),
            (
                "Inference stage timing is "
                "measured inside ImageModel."
            ),
            (
                "No joule value is claimed "
                "as physically measured."
            ),
        ],
    }

    with metadata_file.open(
        "w",
        encoding="utf-8",
    ) as handle:
        json.dump(
            metadata,
            handle,
            indent=2,
        )

    print("\n===== COMPLETE =====")
    print("Runs:")
    print(runs_file)
    print("Summary:")
    print(summary_file)
    print("Metadata:")
    print(metadata_file)

    total_errors = sum(
        1
        for row in rows
        if row["status"] != "SUCCESS"
    )

    print(
        f"Measured attempts: {len(rows)}"
    )
    print(
        f"Errors: {total_errors}"
    )

    if total_errors:
        sys.exit(2)


if __name__ == "__main__":
    main()
