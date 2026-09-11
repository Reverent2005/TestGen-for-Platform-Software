package in.ac.iiitb.plproject.atc.generated;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * EXAMPLE 10 — BinarySearchTree of ints.
 *
 * The library under test for the insert -&gt; insert -&gt; insert -&gt; contains -&gt;
 * min -&gt; delete dry run.  A real tree of {@link Node}s; {@code Keys} and
 * {@code minKey} are the spec-visible mirror of it, rebuilt after every
 * operation.
 *
 * Global state (mirrors specs/BinarySearchTree.spec):
 * <pre>
 *   Keys   : Set&lt;Integer&gt; = {}   // every key in the tree
 *   size   : int           = 0
 *   minKey : int           = 0    // smallest key, meaningful only when size &gt; 0
 * </pre>
 *
 * {@code minKey} is the leftmost node's key.  It is a field rather than a spec
 * expression because the grammar cannot walk to the leftmost node, and it is what
 * lets the spec say that min() returned the SMALLEST key rather than merely some
 * key that is in the tree.
 */
public class Helper {

    /** One node of the tree. */
    static class Node {
        int key;
        Node left;
        Node right;
        Node(int key) { this.key = key; }
    }

    static Node root = null;

    public static Set<Integer> Keys = new LinkedHashSet<Integer>();
    public static int size = 0;
    public static int minKey = 0;

    /** pre: key ∉ Keys;  post: Keys' = Keys ∪ {key}, size' = size + 1. */
    public static void insert(int key) {
        root = insertInto(root, key);
        refresh();
    }

    private static Node insertInto(Node node, int key) {
        if (node == null) {
            return new Node(key);
        }
        if (key < node.key) {
            node.left = insertInto(node.left, key);
        } else {
            node.right = insertInto(node.right, key);
        }
        return node;
    }

    /** pre: size &gt; 0;  post: \result = (key ∈ Keys), tree unchanged. */
    public static boolean contains(int key) {
        Node cursor = root;
        while (cursor != null) {
            if (key == cursor.key) {
                return true;
            }
            if (key < cursor.key) {
                cursor = cursor.left;
            } else {
                cursor = cursor.right;
            }
        }
        return false;
    }

    /**
     * pre: size &gt; 0;  post: \result = the smallest key, tree unchanged.
     *
     * The smallest key is a SERVER_OUTPUT: it is a property of what the tree holds,
     * which the caller cannot know without asking.
     */
    public static int min() {
        Node cursor = root;
        while (cursor.left != null) {
            cursor = cursor.left;
        }
        return cursor.key;
    }

    /** pre: key ∈ Keys;  post: Keys' = Keys \ {key}, size' = size - 1. */
    public static void delete(int key) {
        root = deleteFrom(root, key);
        refresh();
    }

    private static Node deleteFrom(Node node, int key) {
        if (node == null) {
            return null;
        }
        if (key < node.key) {
            node.left = deleteFrom(node.left, key);
        } else if (key > node.key) {
            node.right = deleteFrom(node.right, key);
        } else if (node.left == null) {
            return node.right;
        } else if (node.right == null) {
            return node.left;
        } else {
            Node successor = node.right;
            while (successor.left != null) {
                successor = successor.left;
            }
            node.key = successor.key;
            node.right = deleteFrom(node.right, successor.key);
        }
        return node;
    }

    /** Rebuilds the spec-visible mirror from the tree, which is the real structure. */
    private static void refresh() {
        Keys = new LinkedHashSet<Integer>();
        collect(root);
        size = Keys.size();
        minKey = 0;
        boolean seen = false;
        for (Integer key : Keys) {
            if (!seen || key < minKey) {
                minKey = key;
                seen = true;
            }
        }
    }

    private static void collect(Node node) {
        if (node == null) {
            return;
        }
        collect(node.left);
        Keys.add(node.key);
        collect(node.right);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        root = null;
        Keys = new LinkedHashSet<Integer>();
        size = 0;
        minKey = 0;
    }
}
