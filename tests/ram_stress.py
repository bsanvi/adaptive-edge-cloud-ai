import time


def ram_stress(size_mb=1000, duration_seconds=20):
    print(f"Allocating approximately {size_mb} MB of RAM...")

    # Allocate memory in 50 MB chunks.
    memory_blocks = []

    block_size_mb = 50
    number_of_blocks = size_mb // block_size_mb

    for _ in range(number_of_blocks):
        block = bytearray(block_size_mb * 1024 * 1024)
        memory_blocks.append(block)

    print(f"Holding {size_mb} MB of RAM for {duration_seconds} seconds...")
    time.sleep(duration_seconds)

    print("RAM stress finished.")


if __name__ == "__main__":
    ram_stress()