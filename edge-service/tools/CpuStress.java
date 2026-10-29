public class CpuStress {

    public static void main(String[] args) {

        System.out.println("Starting CPU stress for 10 seconds...");

        long endTime =
                System.currentTimeMillis() + 10_000;

        double value = 0;

        while (System.currentTimeMillis() < endTime) {

            for (int i = 1; i < 1_000_000; i++) {
                value += Math.sqrt(i) * Math.random();
            }
        }

        System.out.println("CPU stress completed.");
    }
}