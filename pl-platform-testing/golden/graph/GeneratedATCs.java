package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void addVertex_helper() {
        String name = Debug.makeSymbolicString("name");
        Debug.assume((name != null));
        int edgeCount_old = Helper.edgeCount;
        Helper.addVertex(name);
        assert(Helper.V.contains(name) && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
    }

    public void addEdge_helper() {
        String from = Debug.makeSymbolicString("from");
        String to = Debug.makeSymbolicString("to");
        Debug.assume(Helper.V.contains(from) && Helper.V.contains(to));
        int edgeCount_old = Helper.edgeCount;
        Helper.addEdge(from, to);
        assert(Helper.Adj.get(from).contains(to) && Helper.Adj.get(to).contains(from) && java.util.Objects.equals(Helper.edgeCount, (edgeCount_old + 1)));
    }

    public int degree_helper() {
        String name = Debug.makeSymbolicString("name");
        Debug.assume(Helper.V.contains(name));
        int edgeCount_old = Helper.edgeCount;
        int vertexDegree = Debug.makeSymbolicInteger("vertexDegree");
        vertexDegree = Helper.degree(name);
        assert(java.util.Objects.equals(vertexDegree, Helper.Adj.get(name).size()) && vertexDegree >= 0 && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
        return vertexDegree;
    }

    public boolean hasEdge_helper() {
        String from = Debug.makeSymbolicString("from");
        String to = Debug.makeSymbolicString("to");
        Debug.assume(Helper.V.contains(from) && Helper.V.contains(to));
        int edgeCount_old = Helper.edgeCount;
        boolean connected = (!java.util.Objects.equals(Debug.makeSymbolicInteger("connected"), 0));
        connected = Helper.hasEdge(from, to);
        assert(java.util.Objects.equals(connected, Helper.Adj.get(from).contains(to)) && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
        return connected;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.addVertex_helper();
        instance.addVertex_helper();
        instance.addEdge_helper();
        int vertexDegree = instance.degree_helper();
        boolean connected = instance.hasEdge_helper();
    }
}
