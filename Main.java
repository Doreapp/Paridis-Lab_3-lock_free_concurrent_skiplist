
/**
 * Authors: Antoine Mandin and Nada Lahlou
 */

import java.util.*;

/**
 * Class to execute the tests
 */
public class Main {
    /**
     * Print the help message
     */
    public static void printHelp() {
        System.out.println("    Usage:");
        System.out.println(
                "        java Main [-h | first | second | threads | linear | linear-lock | large-linear-lock | all | tests]");
        System.out.println("            -h : print this help message");
        System.out.println("            first : start the first (population) test");
        System.out.println("            second : start the second (population) test");
        System.out
                .println("            threads : start the third test, running with several thread /!\\ Will take time");
        System.out.println("            linear : start the linearization test");
        System.out.println("            linear-lock : start the linearization test, using lock to prevent errors");
        System.out.println(
                "            large-linear-lock : start the linearization test, using a lock, and testing on a larger population");
        System.out.println("            all : Start all tests (first, second, threads)");
        System.out.println("            tests : run some aritary tests to understand the set");
    }

    /**
     * Format nano second time into a readable String
     */
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
        } else if (args[0].equals("threads")) {
            threadedTests();
        } else if (args[0].equals("tests")) {
            tests();
        } else if (args[0].equals("linear")) {
            linearizationTest();
        } else if (args[0].equals("linear-lock")) {
            linearizationTestLock(false);
        } else if (args[0].equals("large-linear-lock")) {
            linearizationTestLock(true);
        } else if (args[0].equals("threaded-counter")) {
            threadedCounterLinearisation();
        } else if (args[0].equals("all")) {
            System.out.println("## Tests with 2 populations ");
            populationTest(new FirstGenerator());
            populationTest(new SecondGenerator());
            threadedTests();
            linearizationTest();
            linearizationTestLock(false);
            linearizationTestLock(true);
            threadedCounterLinearisation();
        }

    }

    public interface Generator {
        public int generate();
    }

    /**
     * Generate a random uniformally distributed
     */
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

    /**
     * Generate a random normally distributed
     */
    public static class SecondGenerator implements Generator {
        int range;
        private int next = -1;

        SecondGenerator() {
            this((int) 1e7);
        }

        SecondGenerator(int range) {
            this.range = range;
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
            next = (int) (z1 * range / 2);
            int result = (int) (z2 * range / 2);

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
        System.out.println("### population test");
        System.out.println("With " + length + " members");

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
                int progress = (int) (i * 100.0 / length);
                System.out.print("Progress: " + (progress < 10 ? "0" + progress : progress) + "%");
            }
        }

        System.out.println();

        long duration = System.nanoTime() - tsStart;
        System.out.println("**Results**");
        System.out.println("* Execution time: `" + ((int) (duration / 10_000_000) / 100.00) + "s`");

        final double mean = sum / length;

        // Calculate the Variance
        double squaredDifferencesSum = 0;
        for (Integer value : witnessList) {
            double diff = value - mean;
            squaredDifferencesSum += diff * diff;
        }
        final double variance = squaredDifferencesSum / length;

        System.out.println("* Mean: `" + mean + "`");
        System.out.println("* Variance: `" + variance + "`");
    }

    private static void threadedTests() {
        System.out.println("## Tests with Several threads");

        int operationCount = (int) 1e6;
        int[][] distributions = { { 10, 10, 80 }, { 50, 50, 0 }, { 25, 25, 50 }, { 5, 5, 90 }, };
        int[] threadCounts = { 2, 12, 30, 46 };

        System.out.println("Building populations");
        Set<Integer> firstPopulation = new HashSet<>();
        Generator generator = new FirstGenerator();
        for (int i = 0; i < 1e6; i++)
            firstPopulation.add(generator.generate());

        Set<Integer> secondPopulation = new HashSet<>();
        generator = new SecondGenerator();
        for (int i = 0; i < 1e6; i++)
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

    private static void linearizationTest() {
        Generator generator = new FirstGenerator(20);
        LinearSkipListSet<Integer> skipListSet = new LinearSkipListSet<>();

        LinkedList<Integer> population = new LinkedList<>();
        for (int i = 0; i < 50; i++) {
            population.offer(generator.generate());
        }

        population.parallelStream().forEach((i) -> {
            double rand = Math.random();
            if (rand < 0.333) {
                skipListSet.add(i);
            } else if (rand < 0.667) {
                skipListSet.remove(i);
            } else {
                skipListSet.contains(i);
            }
        });

        System.out.println("## Linearization points");
        System.out.println("```");
        System.out.println("\n" + skipListSet.operationsString());
        System.out.println("```");
    }

    private static void flushLastLine() {
        System.out.print(
                "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
    }

    private static class ProgressCounter {
        volatile int progress = 0;
        int max = 0;

        public void printProgress() {
            // Not the first
            flushLastLine();
            System.out.print(((int) (progress * 1000 / max) / 10.0) + "%");
        }
    }

    private static void linearizationTestLock(boolean large) {
        final int range = large ? 5_000 : 20;
        final int operationCount = large ? 100_000 : 50;

        if (large) {
            System.out.println("### Large test of linearization with lock");
            System.out.println("Using " + operationCount + " operations, and number in a range of " + range);
            System.out.println();
        }

        Generator generator = new FirstGenerator(range);
        LockedSkipListSet<Integer> skipListSet = new LockedSkipListSet<>();

        System.out.print("Building population...");

        LinkedList<Integer> population = new LinkedList<>();
        for (int i = 0; i < operationCount; i++) {
            population.offer(generator.generate());
        }

        flushLastLine();
        final ProgressCounter counter = new ProgressCounter();
        counter.max = operationCount;
        System.out.print("Executing operations...");
        population.parallelStream().forEach((i) -> {
            double rand = Math.random();
            if (rand < 0.333) {
                skipListSet.add(i);
            } else if (rand < 0.667) {
                skipListSet.remove(i);
            } else {
                skipListSet.contains(i);
            }
            ++counter.progress;
            counter.printProgress();
        });
        flushLastLine();

        System.out.print("Building linear execution...");
        flushLastLine();

        if (large) {
            if (skipListSet.isLinearisable()) {
                System.out.println("The execution can be linearized without issue");
            } else {
                System.out.println("A error occured during the retracing of operations!");
            }
        } else {
            String resultDescription = skipListSet.operationsString();
            System.out.println("## Test of linearization points using a lock");
            System.out.println("Using " + operationCount + " operations, and number in a range of " + range);
            System.out.println("```");
            System.out.println(resultDescription);
            System.out.println("```");
        }
    }

    private static void threadedCounterLinearisation() {
        int[] operationCounts = { (int) 1e1, (int) 1e2, (int) 1e3, (int) 1e4, (int) 1e5, (int) 1e6 };
        int[][] distributions = { { 10, 10, 80 }, { 50, 50, 0 }, { 25, 25, 50 }, { 5, 5, 90 }, };
        int[] threadCounts = { 2, 12, 30, 46 };

        System.out.println("### With one counter by thread\n");

        for (int operationCount : operationCounts) {
            System.out.println("#### Running with " + operationCount + " operations\n");

            int indexDistrib = 0;
            for (int[] distribution : distributions) {
                System.out.println("**Distribution " + (++indexDistrib) + "**");
                System.out.println("* " + distribution[0] + "% add");
                System.out.println("* " + distribution[1] + "% remove");
                System.out.println("* " + distribution[2] + "% contains");
                System.out.println();

                System.out.println("| Number of threads       | Linearisation test      |");
                System.out.println("|-------------------------|-------------------------|");

                for (int threadCount : threadCounts) {
                    boolean result = CounterSkipListSet.run(threadCount, operationCount, distribution[0],
                            distribution[1], 0);
                    String str = result ? "PASS" : "FAILED";
                    System.out.println("| " + threadCount + "                      | " + str + "              |");
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