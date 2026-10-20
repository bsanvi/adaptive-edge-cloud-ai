import time


def cpu_stress(duration_seconds=10):
    print(f"Starting CPU stress for {duration_seconds} seconds...")

    end_time = time.time() + duration_seconds

    while time.time() < end_time:
        _ = sum(i * i for i in range(10000))

    print("CPU stress finished.")


if __name__ == "__main__":
    cpu_stress()