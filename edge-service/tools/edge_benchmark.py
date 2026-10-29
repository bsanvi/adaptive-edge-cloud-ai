import argparse
import csv
import json
import statistics
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path


DEFAULT_BASE_URL = "http://localhost:8081"

WORKLOADS = [
    {
        "name": "LOW",
        "path": "/tmp/edge-test/low.jpg",
    },
    {
        "name": "MEDIUM",
        "path": "/tmp/edge-test/input.jpg",
    },
    {
        "name": "HIGH",
        "path": "/tmp/edge-test/high.jpg",
    },
]


def get_json(url):
    request = urllib.request.Request(
        url,
        method="GET",
    )

    with urllib.request.urlopen(
        request,
        timeout=10,
    ) as response:
        return json.loads(
            response.read().decode("utf-8")
        )


def post_json(url, payload):
    body = json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=body,
        headers={
            "Content-Type": "application/json"
        },
        method="POST",
    )

    with urllib.request.urlopen(
        request,
        timeout=30,
    ) as response:
        return json.loads(
            response.read().decode("utf-8")
        )


def check_server(base_url):
    try:
        health = get_json(
            f"{base_url}/api/v1/edge/health"
        )
    except Exception as exc:
        raise RuntimeError(
            "Edge Service is not reachable at "
            f"{base_url}. Start Spring Boot first."
        ) from exc

    if health.get("status") != "ok":
        raise RuntimeError(
            f"Unexpected health response: {health}"
        )


def validate_files():
    for workload in WORKLOADS:
        path = Path(workload["path"])

        if not path.exists():
            raise FileNotFoundError(
                f"Missing benchmark image: {path}"
            )


def profile_task(
    base_url,
    workload_name,
    input_path,
):
    return post_json(
        f"{base_url}/api/v1/edge/profile",
        {
            "task_id": f"BENCHMARK-PROFILE-{workload_name}",
            "input_path": input_path,
            "deadline_ms": 1000,
            "privacy_level": "NORMAL",
            "priority_profile": "BALANCED",
        },
    )


def run_prediction(
    base_url,
    input_path,
):
    start = time.perf_counter()

    result = post_json(
        f"{base_url}/api/v1/edge/predict",
        {
            "input_path": input_path
        },
    )

    end = time.perf_counter()

    round_trip_ms = (
        end - start
    ) * 1000.0

    return result, round_trip_ms


def estimate_energy(
    base_url,
    experiment_id,
    execution_time_ms,
    cpu_before,
    cpu_after,
):
    return post_json(
        f"{base_url}/api/v1/edge/energy/estimate",
        {
            "task_id": experiment_id,
            "execution_time_ms": execution_time_ms,
            "cpu_percent_before": cpu_before,
            "cpu_percent_after": cpu_after,
        },
    )


def mean(values):
    return statistics.mean(values)


def minimum(values):
    return min(values)


def maximum(values):
    return max(values)


def stddev(values):
    if len(values) < 2:
        return 0.0

    return statistics.stdev(values)


def write_detailed_csv(
    output_path,
    rows,
):
    fieldnames = [
        "experiment_id",
        "workload",
        "complexity",
        "trial",
        "input_path",
        "input_size_bytes",
        "image_width",
        "image_height",
        "total_pixels",
        "cpu_before_percent",
        "cpu_after_percent",
        "average_cpu_percent",
        "ram_before_percent",
        "ram_after_percent",
        "battery_before_percent",
        "battery_after_percent",
        "power_plugged_before",
        "power_plugged_after",
        "inference_time_ms",
        "http_round_trip_ms",
        "estimated_power_watts",
        "estimated_energy_joules",
        "predicted_class_index",
        "predicted_label",
        "confidence",
        "status",
    ]

    with output_path.open(
        "w",
        newline="",
        encoding="utf-8",
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=fieldnames,
        )

        writer.writeheader()
        writer.writerows(rows)


def write_summary_csv(
    output_path,
    rows,
):
    workloads = sorted(
        set(
            row["workload"]
            for row in rows
        )
    )

    fieldnames = [
        "workload",
        "complexity",
        "trials",
        "mean_inference_ms",
        "min_inference_ms",
        "max_inference_ms",
        "stddev_inference_ms",
        "mean_round_trip_ms",
        "mean_cpu_percent",
        "mean_ram_percent",
        "mean_power_watts",
        "mean_energy_joules",
    ]

    with output_path.open(
        "w",
        newline="",
        encoding="utf-8",
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=fieldnames,
        )

        writer.writeheader()

        for workload in workloads:
            subset = [
                row
                for row in rows
                if row["workload"] == workload
            ]

            inference_values = [
                float(
                    row["inference_time_ms"]
                )
                for row in subset
            ]

            round_trip_values = [
                float(
                    row["http_round_trip_ms"]
                )
                for row in subset
            ]

            cpu_values = [
                float(
                    row["average_cpu_percent"]
                )
                for row in subset
            ]

            ram_values = [
                (
                    float(
                        row["ram_before_percent"]
                    )
                    +
                    float(
                        row["ram_after_percent"]
                    )
                )
                / 2.0
                for row in subset
            ]

            power_values = [
                float(
                    row["estimated_power_watts"]
                )
                for row in subset
            ]

            energy_values = [
                float(
                    row["estimated_energy_joules"]
                )
                for row in subset
            ]

            writer.writerow(
                {
                    "workload": workload,
                    "complexity": subset[0][
                        "complexity"
                    ],
                    "trials": len(subset),
                    "mean_inference_ms": round(
                        mean(inference_values),
                        4,
                    ),
                    "min_inference_ms": round(
                        minimum(inference_values),
                        4,
                    ),
                    "max_inference_ms": round(
                        maximum(inference_values),
                        4,
                    ),
                    "stddev_inference_ms": round(
                        stddev(inference_values),
                        4,
                    ),
                    "mean_round_trip_ms": round(
                        mean(round_trip_values),
                        4,
                    ),
                    "mean_cpu_percent": round(
                        mean(cpu_values),
                        4,
                    ),
                    "mean_ram_percent": round(
                        mean(ram_values),
                        4,
                    ),
                    "mean_power_watts": round(
                        mean(power_values),
                        4,
                    ),
                    "mean_energy_joules": round(
                        mean(energy_values),
                        6,
                    ),
                }
            )


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Run repeatable Edge inference "
            "benchmarks."
        )
    )

    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
    )

    parser.add_argument(
        "--warmups",
        type=int,
        default=3,
    )

    parser.add_argument(
        "--trials",
        type=int,
        default=10,
    )

    args = parser.parse_args()

    if args.warmups < 0:
        raise ValueError(
            "Warmups cannot be negative"
        )

    if args.trials <= 0:
        raise ValueError(
            "Trials must be greater than zero"
        )

    check_server(args.base_url)
    validate_files()

    timestamp = datetime.now().strftime(
        "%Y%m%d_%H%M%S"
    )

    output_dir = Path("benchmarks")
    output_dir.mkdir(exist_ok=True)

    detailed_path = (
        output_dir
        / f"edge_baseline_{timestamp}.csv"
    )

    summary_path = (
        output_dir
        / f"edge_baseline_summary_{timestamp}.csv"
    )

    all_rows = []

    print()
    print("EDGE PERFORMANCE BENCHMARK")
    print("==========================")
    print(
        f"Warm-up runs per workload: "
        f"{args.warmups}"
    )
    print(
        f"Measured runs per workload: "
        f"{args.trials}"
    )
    print()

    for workload in WORKLOADS:
        name = workload["name"]
        input_path = workload["path"]

        print(
            f"Profiling {name} workload..."
        )

        profile = profile_task(
            args.base_url,
            name,
            input_path,
        )

        complexity = profile[
            "complexity"
        ]

        print(
            f"  File: {input_path}"
        )
        print(
            f"  Resolution: "
            f"{profile['image_width']} x "
            f"{profile['image_height']}"
        )
        print(
            f"  Pixels: "
            f"{profile['total_pixels']}"
        )
        print(
            f"  Complexity: {complexity}"
        )

        print(
            f"  Running {args.warmups} "
            "warm-up inference(s)..."
        )

        for _ in range(args.warmups):
            run_prediction(
                args.base_url,
                input_path,
            )

        print(
            f"  Running {args.trials} "
            "measured trial(s)..."
        )

        for trial in range(
            1,
            args.trials + 1,
        ):
            metrics_before = get_json(
                f"{args.base_url}"
                "/api/v1/edge/metrics"
            )

            prediction, round_trip_ms = (
                run_prediction(
                    args.base_url,
                    input_path,
                )
            )

            metrics_after = get_json(
                f"{args.base_url}"
                "/api/v1/edge/metrics"
            )

            experiment_id = (
                f"EDGE-{name}-{trial:02d}"
            )

            energy = estimate_energy(
                args.base_url,
                experiment_id,
                prediction[
                    "execution_time_ms"
                ],
                metrics_before[
                    "cpu_percent"
                ],
                metrics_after[
                    "cpu_percent"
                ],
            )

            row = {
                "experiment_id":
                    experiment_id,

                "workload":
                    name,

                "complexity":
                    complexity,

                "trial":
                    trial,

                "input_path":
                    input_path,

                "input_size_bytes":
                    profile[
                        "input_data_size_bytes"
                    ],

                "image_width":
                    profile[
                        "image_width"
                    ],

                "image_height":
                    profile[
                        "image_height"
                    ],

                "total_pixels":
                    profile[
                        "total_pixels"
                    ],

                "cpu_before_percent":
                    metrics_before[
                        "cpu_percent"
                    ],

                "cpu_after_percent":
                    metrics_after[
                        "cpu_percent"
                    ],

                "average_cpu_percent":
                    energy[
                        "average_cpu_percent"
                    ],

                "ram_before_percent":
                    metrics_before[
                        "ram_percent"
                    ],

                "ram_after_percent":
                    metrics_after[
                        "ram_percent"
                    ],

                "battery_before_percent":
                    metrics_before[
                        "battery_percent"
                    ],

                "battery_after_percent":
                    metrics_after[
                        "battery_percent"
                    ],

                "power_plugged_before":
                    metrics_before[
                        "power_plugged"
                    ],

                "power_plugged_after":
                    metrics_after[
                        "power_plugged"
                    ],

                "inference_time_ms":
                    prediction[
                        "execution_time_ms"
                    ],

                "http_round_trip_ms":
                    round(
                        round_trip_ms,
                        4,
                    ),

                "estimated_power_watts":
                    energy[
                        "estimated_power_watts"
                    ],

                "estimated_energy_joules":
                    energy[
                        "estimated_energy_joules"
                    ],

                "predicted_class_index":
                    prediction[
                        "predicted_class_index"
                    ],

                "predicted_label":
                    prediction[
                        "predicted_label"
                    ],

                "confidence":
                    prediction[
                        "confidence"
                    ],

                "status":
                    prediction[
                        "status"
                    ],
            }

            all_rows.append(row)

            print(
                f"    Trial {trial:02d}: "
                f"inference="
                f"{prediction['execution_time_ms']}"
                f" ms, "
                f"CPU="
                f"{energy['average_cpu_percent']}"
                f"%, "
                f"energy="
                f"{energy['estimated_energy_joules']}"
                f" J"
            )

            time.sleep(0.25)

        print()

    write_detailed_csv(
        detailed_path,
        all_rows,
    )

    write_summary_csv(
        summary_path,
        all_rows,
    )

    print("Benchmark complete.")
    print()
    print(
        f"Detailed results: {detailed_path}"
    )
    print(
        f"Summary results:  {summary_path}"
    )


if __name__ == "__main__":
    main()