import java.util.concurrent.atomic.*;

/**
 * Code from H&S
 */

public class LockfreeConcurrentSkipListSet<T> {
    // Max level: 
    static final int MAX_LEVEL = 10; 

    // probability for randomLevel method (probability of haaving a 0)
    private static final double P = 0.75;  

    // <A> Head of the list, first element ? 
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    
    // <A> Tail of the list, last element ?
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    /**
     * Implementation found at
     * https://stackoverflow.com/questions/12067045/random-level-function-in-skip-list
     * Should validate "The randomLevel() method is designed based on empirical
     * measurements to maintain the skiplist property."
     * 
     * @return level
     */
    public static int randomLevel() {
        int lvl = (int) (Math.log(1. - Math.random()) / Math.log(1. - P));
        return Math.min(lvl, MAX_LEVEL);
    }

    public LockfreeConcurrentSkipListSet() {
        for (int i = 0; i < head.next.length; i++) {
            head.next[i] = new AtomicMarkableReference<LockfreeConcurrentSkipListSet.Node<T>>(tail, false);
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
            boolean found = find(x, preds, succs);
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

                // Set the 'next' of the predecessor to the new node, if was still the found 'succesor'
                // Otherwise restart process
                if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                    continue;
                }
                for (int level = bottomLevel + 1; level <= topLevel; level++) {
                    // For each level, update the succesor of the previous node (until success of CAS)
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
            boolean found = find(x, preds, succs);
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
                    boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt) {
                        find(x, preds, succs);
                        return true;
                    } else if (marked[0])
                        return false;
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
        return (curr.key == v);
    }

    public String stringify() {
        String result = "LockfreeConcurrentSkipListSet {";

        int bottomLevel = 0;
        final int lastValue = Integer.MAX_VALUE;
        Node<T> pred = head, curr = null;

        for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
            result += "\n    "+level+": ";
            curr = pred.next[level].getReference();
            while (curr.key < lastValue) {
                result += curr.value+" ";
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

        // <A> Key of the node, unique value corresponding to the stored object (actually hashcode) 
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
         * @param x Value to store
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
}
