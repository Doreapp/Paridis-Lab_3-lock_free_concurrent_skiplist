
/**
 * Authors: Antoine Mandin and Nada Lahlou
 */

import java.io.IOException;
import java.util.*;

public class Main {
    public static void printHelp() {
        System.out.println("    Usage:");
        System.out.println("        java Main [-h | first | tests]");
        System.out.println("            -h : print this help message");
        System.out.println("            first : start the first test");
        System.out.println("            tests : run some aritary tests to understand the set");
    }

    public static String formatNano(long nano) {
        long ns = nano % 1000L;
        long us = (int) ((nano / 1000L) % 1000L);
        long ms = (int) ((nano / 1_000_000L) % 1000L);
        long s = (int) (nano / 1_000_000_000L);

        String nsString = ns < 10 ? "00" + ns : ns < 100 ? "0" + ns : "" + ns;
        String usString = us < 10 ? "00" + us : us < 100 ? "0" + us : "" + us;
        String msString = ms < 10 ? "00" + ms : ms < 100 ? "0" + ms : "" + ms;

        return s + "'" + msString + "'" + usString + "'" + nsString + "'";
    }

    public static void main(String[] args) {
        if (args.length <= 0) {
            printHelp();
            return;
        }

        if (args[0].equals("-h")) {
            printHelp();
        } else if (args[0].equals("first")) {
            populationTest(new FirstGenerator());
        } else if (args[0].equals("second")) {
            populationTest(new SecondGenerator());
        } else if (args[0].equals("threaded-test")) {
            threadedTests();
        } else if (args[0].equals("tests")) {
            tests();
        }

    }

    public interface Generator {
        public int generate();
    }

    public static class FirstGenerator implements Generator {
        int range;

        FirstGenerator() {
            this((int) 1e7);
        }

        FirstGenerator(int range) {
            this.range = range;
        }

        @Override
        public int generate() {
            return (int) (Math.random() * this.range);
        }
    }

    public static class SecondGenerator implements Generator {
        int range;
        int mean, var;
        private int next = -1;

        SecondGenerator() {
            this((int) 1e7);
        }

        SecondGenerator(int range) {
            this(range, range / 2, range / 6);
        }

        SecondGenerator(int range, int mean, int var) {
            this.range = range;
            this.mean = mean;
            this.var = var;
        }

        @Override
        public int generate() {
            if (next >= 0) {
                int res = next;
                next = -1;
                return res;
            }

            // Following calculation from
            // https://www.baeldung.com/cs/uniform-to-normal-distribution
            double r1 = Math.random(), r2 = Math.random();
            double firstPart = Math.sqrt(-2 * Math.log(r1));
            double z1 = firstPart * Math.cos(2 * Math.PI * r2) / 5.0 + 1.0;
            double z2 = firstPart * Math.sin(2 * Math.PI * r2) / 5.0 + 1.0;
            next = (int) (z1 * mean);
            int result = (int) (z2 * mean);

            if (next < 0) {
                next = 0;
            } else if (next >= this.range) {
                next = this.range - 1;
            }

            if (result < 0) {
                result = 0;
            } else if (result >= this.range) {
                result = this.range - 1;
            }

            return result;
        }

    }

    private static void populationTest(Generator generator) {
        populationTest(generator, (int) (1e7));
    }

    private static void populationTest(Generator generator, final int length) {
        System.out.println("** First population test **");
        System.out.println("    length=" + length);

        LockfreeConcurrentSkipListSet<Integer> skiplist = new LockfreeConcurrentSkipListSet<Integer>();

        // For mean calculation
        double sum = 0;
        LinkedList<Integer> witnessList = new LinkedList<>();

        long tsStart = System.nanoTime();

        for (int i = 0; i < length; i++) {
            int number = generator.generate();
            sum += number;
            witnessList.addFirst(number);

            skiplist.add((Integer) number);
            // sys.stdout.write("\033[K")
            if (i % 100000 == 0) {
                if (i != 0)
                    System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b"); // Remove the last printed progress line
                System.out.print("Progress: " + ((int) (i * 100.0 / length)) + "%");
            }
        }

        System.out.println();
        System.out.println("Duration: " + formatNano(System.nanoTime() - tsStart) + " ns");

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

    private static void threadedTests() {
        System.out.println("** Threaded tests **");

        int operationCount = (int) 1e6;
        int[][] distributions = { { 10, 10, 80 }, { 50, 50, 0 }, { 25, 25, 50 }, { 5, 5, 90 }, };
        int[] threadCounts = { 2, 12, 30, 46 };

        System.out.println("Building populations");
        Set<Integer> firstPopulation = new HashSet<>();
        Generator generator = new FirstGenerator();
        for (int i = 0; i < 1e7; i++)
            firstPopulation.add(generator.generate());

        Set<Integer> secondPopulation = new HashSet<>();
        generator = new SecondGenerator();
        for (int i = 0; i < 1e7; i++)
            secondPopulation.add(generator.generate());

        int indexDistrib = 0;
        for (int[] distribution : distributions) {
            System.out.println("## Distribution " + (++indexDistrib));
            System.out.println("* " + distribution[0] + "% add");
            System.out.println("* " + distribution[1] + "% remove");
            System.out.println("* " + distribution[2] + "% contains");
            System.out.println();

            for (int generatorType = 0; generatorType < 2; generatorType++) {
                System.out.println("### " + (generatorType == 0 ? "First" : "Second") + " population");

                System.out.println("| Number of threads       | Average time            |");
                System.out.println("|-------------------------|-------------------------|");

                for (int threadCount : threadCounts) {
                    long totalDuration = 0;
                    for (int exec = 0; exec < 10; exec++) {
                        ThreadedTests test = new ThreadedTests(threadCount, operationCount, distribution[0],
                                distribution[1]);
                        test.fillUpListWithSet(generatorType == 0 ? firstPopulation : secondPopulation);
                        long duration = test.run(generatorType);
                        totalDuration += duration;
                        // System.out.print(formatNano(duration) + " ");
                    }
                    // System.out.println("Total duration: " + formatNano(totalDuration));
                    // System.out.println("Average duration: " + formatNano(totalDuration / 10L));
                    String timeS = (totalDuration / 1_000_000L) / 100.0 + "s";
                    System.out.println("| " + threadCount + "                      | " + timeS + "                  |");
                }
                System.out.println();
            }
        }
    }

    private static void tests() {
        LockfreeConcurrentSkipListSet<Integer> set = new LockfreeConcurrentSkipListSet<>();

        System.out.println("Testing some things...");

        System.out.println("-- Initial Set --");
        System.out.println(set.stringify());
        System.out.println("--- ---------- --");

        set.add(5);
        set.add(-7);
        set.add(123);

        System.out.println("-- Set w/ some values --");
        System.out.println(set.stringify());
        System.out.println("--- ---------- --");

        for (int i = 0; i < 20; i++) {
            set.add(i);
        }

        System.out.println("-- Set w/ 20 more values --");
        System.out.println(set.stringify());
        System.out.println("--- ---------- --");

        System.out.println("Set contains 13: " + set.contains(13));

        for (int i = 0; i < 10; i++) {
            set.remove(i);
        }

        System.out.println("-- Set w/ 10 less values --");
        System.out.println(set.stringify());
        System.out.println("--- ---------- --");
    }
}