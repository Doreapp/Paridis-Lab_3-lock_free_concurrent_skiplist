
/**
 * Authors: Antoine Mandin and Nada Lahlou
 */

import java.io.IOException;
import java.util.*;

public class Main {
    public static void printHelp() {
        System.out.println("    Usage:");
        System.out.println("        java Main [-h]");
        System.out.println("            -h - print this help message");
    }

    public static void main(String[] args) {
        if (args.length <= 0) {
            printHelp();
            return;
        }

        if (args[0].equals("-h")) {
            printHelp();
            return;
        }

        if (args[0].equals("first")) {
            populationTest(new FirstGenerator());
        }
    }

    interface Generator {
        public int generate();
    }

    public static class FirstGenerator implements Generator {
        int range;

        FirstGenerator() {
            this((int) 10e7);
        }

        FirstGenerator(int range) {
            this.range = range;
        }

        @Override
        public int generate() {
            return (int) (Math.random() * this.range);
        }
    }

    private static void populationTest(Generator generator) {
        populationTest(generator, (int) (10e7));
    }

    private static void populationTest(Generator generator, int length) {
        System.out.println("** First population test **");
        System.out.println("    length=" + length);

        LockfreeConcurrentSkipListSet<Integer> skiplist = new LockfreeConcurrentSkipListSet<Integer>();

        // For mean calculation
        double sum = 0;
        LinkedList<Integer> witnessList = new LinkedList<>();

        long duration = 0;
        long wholeDuration = 0;

        for (int i = 0; i < length; i++) {
            long wholeStart = System.nanoTime();
            int number = generator.generate();
            sum += number;
            witnessList.addFirst(number);

            long start = System.nanoTime();
            skiplist.add((Integer) number);
            
            long end = System.nanoTime();
            duration += end-start;
            wholeDuration += end - wholeStart;

            // sys.stdout.write("\033[K")
            if (i % 1000000 == 0) {
                System.out.println("Progress: " + ((int) (i * 100 / length)) + "% - adds took "+duration+", whole is "+wholeDuration);
                duration = 0;
                wholeDuration = 0;
            }
        }

        final double mean = sum / length;

        // Calculate the Variance
        double squaredDifferencesSum = 0;
        for (Integer value : witnessList) {
            double diff = value - mean;
            squaredDifferencesSum += diff * diff;
        }
        final double variance = squaredDifferencesSum / length;

        System.out.println("    Data stats:");
        System.out.println("        Mean=" + mean);
        System.out.println("        Variance=" + variance);
    }
}