package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EXAMPLE 12 — Graph, an undirected graph held as adjacency lists.
 *
 * The library under test for the addVertex -&gt; addVertex -&gt; addEdge -&gt; degree
 * -&gt; hasEdge dry run.
 *
 * Global state (mirrors specs/Graph.spec):
 * <pre>
 *   V         : Set&lt;String&gt;              = {}   // the vertices
 *   Adj       : Map&lt;String,List&lt;String&gt;&gt; = {}   // vertex -&gt; its neighbours
 *   edgeCount : int                       = 0
 * </pre>
 *
 * {@code Adj} is the only state in the whole example set that is a map to a
 * COLLECTION, and it is there to show that the spec can still reach into it:
 * {@code Adj.get(name).size()} is read as one free name whose leading identifier
 * is a state variable, so it is emitted as {@code Helper.Adj.get(name).size()}.
 */
public class Helper {

    public static Set<String> V = new LinkedHashSet<String>();
    public static Map<String, List<String>> Adj = new LinkedHashMap<String, List<String>>();
    public static int edgeCount = 0;

    /** pre: name != null;  post: V' = V ∪ {name}, edges unchanged. */
    public static void addVertex(String name) {
        if (!V.contains(name)) {
            V.add(name);
            Adj.put(name, new ArrayList<String>());
        }
    }

    /** pre: from ∈ V &amp;&amp; to ∈ V;  post: to ∈ Adj'[from], from ∈ Adj'[to], edgeCount' = edgeCount + 1. */
    public static void addEdge(String from, String to) {
        Adj.get(from).add(to);
        Adj.get(to).add(from);
        edgeCount = edgeCount + 1;
    }

    /**
     * pre: name ∈ V;  post: \result = |Adj[name]|, graph unchanged.
     *
     * A vertex's degree is a SERVER_OUTPUT: it is a fact about the graph's shape,
     * which the caller would have to have tracked itself to know.
     */
    public static int degree(String name) {
        return Adj.get(name).size();
    }

    /** pre: from ∈ V &amp;&amp; to ∈ V;  post: \result = (to ∈ Adj[from]), graph unchanged. */
    public static boolean hasEdge(String from, String to) {
        return Adj.get(from).contains(to);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        V = new LinkedHashSet<String>();
        Adj = new LinkedHashMap<String, List<String>>();
        edgeCount = 0;
    }
}
