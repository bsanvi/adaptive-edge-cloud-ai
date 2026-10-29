public class CpuLoadGenerator {
    public static void main(String[] args) throws InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, Math.min(4, cpus / 2));

        System.out.println("Logical processors: " + cpus);
        System.out.println("CPU stress workers: " + workers);
        System.out.println("Press Ctrl+C to stop.");

        Thread[] threads = new Thread[workers];

        for (int w = 0; w < workers; w++) {
            final int id = w;

            threads[w] = new Thread(() -> {
                double value = id + 1.0;

                while (!Thread.currentThread().isInterrupted()) {
                    for (int i = 1; i < 500000; i++) {
                        value += Math.sqrt(i + value % 7.0);
                    }

                    if (value > 1_000_000_000.0) {
                        value = id + 1.0;
                    }
                }
            });

            threads[w].start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }
}
