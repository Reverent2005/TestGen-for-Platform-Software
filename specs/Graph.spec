/*
 * EXAMPLE 12 — Graph (undirected, adjacency lists)
 * Pattern: a two-parameter call, and state that is a map to a collection.
 *
 * Test string: addVertex -> addVertex -> addEdge -> degree -> hasEdge
 *
 * addEdge is the first block in the set whose precondition constrains TWO
 * parameters against the same piece of state (`V.contains(from) && V.contains(to)`)
 * and whose postcondition has to say something about both directions, because the
 * graph is undirected.  Both directions are asserted separately, since an
 * implementation that only ever recorded one of them is the obvious fault and it
 * satisfies either half on its own.
 *
 * degree() reaches two levels into the state: `Adj.get(name).size()`.  That is
 * read as a single free name, and rewritten by its leading identifier, so it is
 * emitted as Helper.Adj.get(name).size().  The `name` inside the parentheses is a
 * formal parameter and is passed through untouched — which is exactly why an
 * inner identifier in such a term must never be a state variable: nothing
 * qualifies it.
 *
 * SIMPLIFICATION: nothing here says the graph is simple.  addEdge called twice
 * with the same pair adds a parallel edge and every clause still holds, which is
 * what GRAPH_ADDEDGE_ONE_DIRECTION and its neighbours in
 * mutations/graph.mutants are there to measure.
 */

state {
    Set<String> V;
    Map<String,List<String>> Adj;
    int edgeCount;
}

spec addVertex {
    signature: void addVertex(String name);
    requires:  name != null;
    ensures:   V.contains(name) && edgeCount == \old(edgeCount);
}

spec addEdge {
    signature: void addEdge(String from, String to);
    requires:  V.contains(from) && V.contains(to);
    ensures:   Adj.get(from).contains(to) && Adj.get(to).contains(from) && edgeCount == \old(edgeCount) + 1;
}

spec degree {
    signature: int degree(String name);
    requires:  V.contains(name);
    ensures:   \result == vertexDegree && vertexDegree == Adj.get(name).size() && vertexDegree >= 0 && edgeCount == \old(edgeCount);
}

spec hasEdge {
    signature: boolean hasEdge(String from, String to);
    requires:  V.contains(from) && V.contains(to);
    ensures:   \result == connected && connected == Adj.get(from).contains(to) && edgeCount == \old(edgeCount);
}
