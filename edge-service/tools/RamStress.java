import java.util.Arrays;

public class RamStress {

    public static void main(String[] args) throws Exception {

        System.out.println("Allocating approximately 512 MB of RAM...");

        byte[][] blocks = new byte[8][];

        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new byte[64 * 1024 * 1024];

            // Touch the memory so it is actually committed
            Arrays.fill(blocks[i], (byte) 1);
        }

        System.out.println("512 MB allocated.");
        System.out.println("Holding memory for 15 seconds...");

        Thread.sleep(15_000);

        System.out.println("RAM stress completed.");
    }
}