import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Code from H&S
 */

public class LockedSkipListSet<T> {
    // Max level
    static final int MAX_LEVEL = 10;

    // probability for randomLevel method (probability of haaving a 0)
    private static final double P = 0.75;

    // <A> Head of the list, first element ?
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);

    // <A> Tail of the list, last element ?
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    ConcurrentLinkedQueue<Operation<T>> operations = new ConcurrentLinkedQueue<>();

    /**
     * Implementation found at
     * https://stackoverflow.com/questions/12067045/random-level-function-in-skip-list
     * Should validate "The randomLevel() method is designed based on empirical
     * measurements to maintain the skiplist property."
     * 
     * @return a level between 0 and MAX_LEVEL
     */
    public static int randomLevel() {
        int lvl = (int) (Math.log(1. - Math.random()) / Math.log(1. - P));
        return Math.min(lvl, MAX_LEVEL);
    }

    public LockedSkipListSet() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LockedSkipListSet.Node<T>>(tail, false);
        }
    }

    boolean add(T x) {
        // <A> Top level, the maximum level where the node will be
        int topLevel = randomLevel();

        // <A> Bottom level: the level to start (what?) with
        int bottomLevel = 0;

        // <A> Predecessors and successors of the node to add
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];

        while (true) {
            boolean found;
            synchronized (operations) {
                found = find(x, preds, succs);
                if (found) {
                    Operation<T> op = new Operation<T>("add", false, x);
                    operations.offer(op);
                }
            }

            if (found) {
                // Do not add the value as already present: end and return false
                return false;
            } else {
                // The new node to add
                Node<T> newNode = new Node<T>(x, topLevel);

                // For all the levels, update the 'next' array of the node
                for (int level = bottomLevel; level <= topLevel; level++) {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }

                // Lower (closer) predecessor and succesor
                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];

                // Set the 'next' of the predecessor to the new node, if was still the found
                // 'succesor'
                // Otherwise restart process
                synchronized (operations) {
                    if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                        continue;
                    }
                    Operation<T> op = new Operation<T>("add", true, x);
                    operations.offer(op);
                }

                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                    // For each level, update the succesor of the previous node (until success of
                    // CAS)
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs);
                    }
                }
                return true;
            }
        }
    }

    boolean remove(T x) {
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        while (true) {
            boolean found;
            synchronized (operations) {
                found = find(x, preds, succs);
                if (!found) {
                    Operation<T> op = new Operation<T>("remove", false, x);
                    operations.offer(op);
                }
            }

            if (!found) {
                return false;
            } else {
                Node<T> nodeToRemove = succs[bottomLevel];
                for (int level = nodeToRemove.topLevel; level >= bottomLevel + 1; level--) {
                    boolean[] marked = { false };
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0]) {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                boolean[] marked = { false };
                succ = nodeToRemove.next[bottomLevel].get(marked);
                while (true) {
                    synchronized (operations) {
                        boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                        succ = succs[bottomLevel].next[bottomLevel].get(marked);
                        if (iMarkedIt) {
                            Operation<T> op = new Operation<T>("remove", true, x);
                            operations.offer(op);
                            find(x, preds, succs);
                            return true;
                        } else if (marked[0]) {
                            Operation<T> op = new Operation<T>("remove", false, x);
                            operations.offer(op);
                            return false;
                        }
                    }

                }
            }
        }
    }

    boolean find(T x, Node<T>[] preds, Node<T>[] succs) {
        int bottomLevel = 0;
        int key = x.hashCode();

        boolean[] marked = { false };
        boolean snip;

        Node<T> pred = null, curr = null, succ = null;
        retry: while (true) {
            pred = head;
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip)
                            continue retry;
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key < key) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return (curr.key == key);
        }
    }

    boolean contains(T x) {
        int bottomLevel = 0;
        int v = x.hashCode();
        boolean[] marked = { false };
        Node<T> pred = head, curr = null, succ = null;

        synchronized (operations) {
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference(); // Replace with pred ?
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.key < v) {
                        pred = curr;
                        curr = succ;
                    } else {
                        break;
                    }
                }
            }
            Operation<T> op = new Operation<T>("cont.", (curr.key == v), x);
            operations.offer(op);
        }

        return (curr.key == v);
    }

    public String stringify() {
        String result = "LockedSkipListSet {";

        int bottomLevel = 0;
        final int lastValue = Integer.MAX_VALUE;
        Node<T> pred = head, curr = null;

        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            result += "\n    " + level + ": ";
            curr = pred.next[level].getReference();
            while (curr.key < lastValue) {
                result += curr.value + " ";
                curr = curr.next[level].getReference();
            }
        }

        result += "\n}";

        return result;
    }

    /**
     * Node of the ListSet
     */
    public static final class Node<T> {
        // <A> Actual stored value by the node
        final T value;

        // <A> Key of the node, unique value corresponding to the stored object
        // (actually hashcode)
        final int key;

        // <A> Array of the next node ?
        final AtomicMarkableReference<Node<T>>[] next;

        // <A> ?
        private int topLevel;

        // constructor for sentinel nodes
        public Node(int key) {
            value = null; // No value

            this.key = key;

            // <A> Create the array of next node ?
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];

            // <A> Fill the 'next' array with unmarked reference to nothing (null)
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }

            topLevel = MAX_LEVEL;
        }

        /**
         * constructor for ordinary nodes
         * 
         * @param x      Value to store
         * @param height ?
         */
        public Node(T x, int height) {
            // Fill the values of the node
            value = x;
            key = x.hashCode();

            // <A> Create the 'next' with a length of 'height', why ??
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = height;
        }
    }

    /**
     * Check if the execution is linearisable
     * @return
     */
    public boolean isLinearisable() {

        // Array list of operations, that may be sorted
        List<Operation<T>> list = new ArrayList<>();
        list.addAll(operations);

        // Sort the list by time
        Collections.sort(list);

        // Current content of the set (during execution)
        HashSet<T> currentList = new HashSet<>();

        for (Operation<T> op : list) {
            switch (op.name) {
                case "cont.":
                    boolean real = currentList.contains(op.value);
                    if (real != op.result) {
                        return false;
                    }
                    break;
                case "add":
                    real = currentList.add(op.value);
                    if (real != op.result) {
                        return false;
                    }
                    break;
                case "remove":
                    real = currentList.remove(op.value);
                    if (real != op.result) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * Compute the operations saved and build a string retracing the execution as a
     * linear one
     * 
     * @return String - the description of the operations
     */
    public String operationsString() {

        // Array list of operations, that may be sorted
        List<Operation<T>> list = new ArrayList<>();
        list.addAll(operations);

        // Sort the list by time
        Collections.sort(list);

        // Current content of the set (during execution)
        HashSet<T> currentList = new HashSet<>();

        String result = "";
        for (Operation<T> op : list) {
            result += "\t" + op;
            switch (op.name) {
                case "cont.":
                    boolean real = currentList.contains(op.value);
                    if (real != op.result) {
                        result += "\tERROR";
                    }
                    break;
                case "add":
                    real = currentList.add(op.value);
                    if (real != op.result) {
                        result += "\tERROR";
                    } else if (real) {
                        result += "\t[";
                        for (T t : currentList)
                            result += t + ",";
                        result += "]";
                    }
                    break;
                case "remove":
                    real = currentList.remove(op.value);
                    if (real != op.result) {
                        result += "\tERROR";
                    } else if (real) {
                        result += "\t[";
                        for (T t : currentList)
                            result += t + ",";
                        result += "]";
                    }
                    break;
            }
            result += "\n";
        }
        return result;
    }

    /**
     * Operation class, representing a timestamped operation on the list
     */
    private static class Operation<T> implements Comparable {
        long time = -1;
        String name = "";
        boolean result;
        T value;

        Operation(String name, boolean result, T value) {
            this.time = System.nanoTime();
            this.name = name;
            this.value = value;
            this.result = result;
        }

        @Override
        public String toString() {
            return "at " + time + "\t" + name + "\t" + result + "\t(" + value + ")";
        }

        @Override
        public int compareTo(Object other) {
            if (other instanceof Operation) {
                return Long.compare(this.time, ((Operation) other).time);
            }
            return 0;
        }
    }
}
