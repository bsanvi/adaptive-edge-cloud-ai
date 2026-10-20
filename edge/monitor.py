from datetime import datetime

import psutil


def get_edge_metrics():
    """
    Collect the current resource usage of the Edge device.
    """

    cpu_percent = psutil.cpu_percent(interval=1)

    memory = psutil.virtual_memory()

    bytes_to_mb = 1024 * 1024

    ram_used_mb = memory.used / bytes_to_mb
    ram_available_mb = memory.available / bytes_to_mb

    try:
        battery = psutil.sensors_battery()
    except (AttributeError, NotImplementedError):
        battery = None

    if battery is not None:
        battery_percent = battery.percent
        power_plugged = battery.power_plugged
    else:
        battery_percent = None
        power_plugged = None

    metrics = {
        "cpu_percent": round(cpu_percent, 2),
        "ram_percent": round(memory.percent, 2),
        "ram_used_mb": round(ram_used_mb, 2),
        "ram_available_mb": round(ram_available_mb, 2),
        "battery_percent": battery_percent,
        "power_plugged": power_plugged,
        "timestamp": datetime.now().isoformat(timespec="seconds"),
    }

    return metrics


def print_edge_metrics(metrics):
    print("\n=== EDGE DEVICE METRICS ===\n")

    print(f"CPU Usage:        {metrics['cpu_percent']} %")
    print(f"RAM Usage:        {metrics['ram_percent']} %")
    print(f"RAM Used:         {metrics['ram_used_mb']} MB")
    print(f"RAM Available:    {metrics['ram_available_mb']} MB")

    if metrics["battery_percent"] is not None:
        print(f"Battery:          {metrics['battery_percent']} %")

        if metrics["power_plugged"]:
            print("Charging:         Yes")
        else:
            print("Charging:         No")
    else:
        print("Battery:          Not available")
        print("Charging:         Not available")

    print(f"Timestamp:        {metrics['timestamp']}")
    print()


if __name__ == "__main__":
    edge_metrics = get_edge_metrics()
    print_edge_metrics(edge_metrics)