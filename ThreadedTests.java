import java.util.*;

/**
 * Class to execute thread tests
 */
public class ThreadedTests {
    // Number of operations
    int addCount, removeCount, containsCount;
    int operationCount;

    // Number of threads
    int threadCount;

    // Limit of the random for add and remove
    double addLimit, removeLimit;

    // List to test
    private LockfreeConcurrentSkipListSet<Integer> skipListSet;

    /**
     * 
     * @param threadCount number of threads to use
     * @param operationCount number of operations
     * @param addPercentage percentage of add
     * @param removePercentage percentage of remove
     */
    public ThreadedTests(int threadCount, int operationCount, int addPercentage, int removePercentage) {
        this.threadCount = threadCount;
        this.operationCount = operationCount;

        addCount = operationCount * addPercentage / 100;
        removeCount = removePercentage * operationCount / 100;
        containsCount = operationCount - addCount - removeCount;

        addLimit = addPercentage / 100.0;
        removeLimit = addLimit + removePercentage / 100.0;

        assert (addCount >= 0);
        assert (removeCount >= 0);
        assert (containsCount >= 0);

        this.skipListSet = new LockfreeConcurrentSkipListSet<>();
    }

    /**
     * Fill up the list using a given generator type. Add 10^7 elements
     * @param generatorType 0 for uniform, 1 for normal
     */
    public void fillUpList(int generatorType) {
        Main.Generator generator = generatorType == 0 ? new Main.FirstGenerator() : new Main.SecondGenerator();
        for(int i = 0; i < 1e7; i++){
            skipListSet.add(generator.generate());
        }
    }

    /**
     * Fill up the list with the input set
     * @param set population to use
     */
    public void fillUpListWithSet(Set<Integer> set){
        for(Integer i : set){
            skipListSet.add(i);
        }
    }

    public void setList(LockfreeConcurrentSkipListSet<Integer> skipListSet) {
        this.skipListSet = skipListSet;
    }

    /**
     * Run the test 
     * @param generatorType 0 for uniform, 1 for normal
     * @return execution time
     */
    public long run(int generatorType) {
        // Operations counts (add,remove,contains) to execution by thread
        int[][] threadOperations = new int[3][threadCount];

        // Current total operation counts (for loop)
        int currOperationCount = 0;
        int currAddCount = 0, currRemoveCount = 0, currContainsCount = 0;

        for (int i = 0; i < threadCount; i++) {
            // Fill up the operations for this thread
            while (currOperationCount < operationCount * (i + 1) / threadCount) {
                double rand = Math.random();
                if (rand <= addLimit) {
                    if (currAddCount < addCount) {
                        threadOperations[0][i]++;
                        currAddCount++;
                        currOperationCount++;
                    }
                } else if (rand <= removeLimit) {
                    if (currRemoveCount < removeCount) {
                        threadOperations[1][i]++;
                        currRemoveCount++;
                        currOperationCount++;
                    }
                } else {
                    if (currContainsCount < containsCount) {
                        threadOperations[2][i]++;
                        currContainsCount++;
                        currOperationCount++;
                    }
                }
            }
        }

        // Create the worker threads
        Worker[] workers = new Worker[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Main.Generator generator = generatorType == 0 ? new Main.FirstGenerator() : new Main.SecondGenerator();
            workers[i] = new Worker(generator, threadOperations[0][i], threadOperations[1][i], threadOperations[2][i]);
        }

        // Start the execution
        long start = System.nanoTime();
        for(Worker worker : workers)
            worker.start();

        // Wait for the end of every worker
        for(Worker worker : workers){
            try {
                worker.join();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        long duration = System.nanoTime() - start;
        return duration;
    }

    /**
     * Thread executing operation on the skiplist
     */
    public class Worker extends Thread {
        int addCount, removeCount, containsCount;
        Main.Generator generator;

        public Worker(Main.Generator generator, int addCount, int removeCount, int containsCount) {
            this.generator = generator;
            this.addCount = addCount;
            this.removeCount = removeCount;
            this.containsCount = containsCount;
        }

        @Override
        public void run() {
            int totalOp = addCount + removeCount + containsCount;

            // Run the opperations
            while (totalOp > 0) {
                int rand = (int) (Math.random() * totalOp);
                if (rand < addCount && addCount > 0) {
                    skipListSet.add(generator.generate());
                    addCount--;
                    totalOp--;

                } else if (rand < addCount + removeCount && removeCount > 0) {
                    skipListSet.remove(generator.generate());
                    removeCount--;
                    totalOp--;

                } else if (containsCount > 0) {
                    skipListSet.contains(generator.generate());
                    containsCount--;
                    totalOp--;
                }
            }
        }
    }
}
