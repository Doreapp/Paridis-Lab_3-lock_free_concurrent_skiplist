public class ThreadedTests {
    int addCount, removeCount, containsCount;
    int threadCount;
    double addLimit, removeLimit;
    int operationCount;

    private LockfreeConcurrentSkipListSet<Integer> skipListSet;

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

    public void fillUpList(int generatorType) {
        Main.Generator generator = generatorType == 0 ? new Main.FirstGenerator() : new Main.SecondGenerator();
        for(int i = 0; i < 1e7; i++){
            skipListSet.add(generator.generate());
        }
    }

    public long run(int generatorType) {
        int[][] threadOperations = new int[3][threadCount];
        int currOperationCount = 0;
        int currAddCount = 0, currRemoveCount = 0, currContainsCount = 0;

        for (int i = 0; i < threadCount; i++) {
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

        Worker[] workers = new Worker[threadCount];

        for (int i = 0; i < threadCount; i++) {
            Main.Generator generator = generatorType == 0 ? new Main.FirstGenerator() : new Main.SecondGenerator();
            workers[i] = new Worker(generator, threadOperations[0][i], threadOperations[1][i], threadOperations[2][i]);
        }

        long start = System.nanoTime();
        for(Worker worker : workers)
            worker.start();

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
