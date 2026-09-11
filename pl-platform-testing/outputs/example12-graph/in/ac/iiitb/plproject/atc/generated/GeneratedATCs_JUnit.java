package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void addVertex_helper() {
        String name = "name" /* CLIENT_INPUT: replace with the literal SPF solves for "name" */;
        assumeTrue(name != null);
        int edgeCount_old = Helper.edgeCount;
        Helper.addVertex(name);
        assert(Helper.V.contains(name) && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
    }

    public void addEdge_helper() {
        String from = "from" /* CLIENT_INPUT: replace with the literal SPF solves for "from" */;
        String to = "to" /* CLIENT_INPUT: replace with the literal SPF solves for "to" */;
        assumeTrue(Helper.V.contains(from) && Helper.V.contains(to));
        int edgeCount_old = Helper.edgeCount;
        Helper.addEdge(from, to);
        assert(Helper.Adj.get(from).contains(to) && Helper.Adj.get(to).contains(from) && java.util.Objects.equals(Helper.edgeCount, (edgeCount_old + 1)));
    }

    public int degree_helper() {
        String name = "name" /* CLIENT_INPUT: replace with the literal SPF solves for "name" */;
        assumeTrue(Helper.V.contains(name));
        int edgeCount_old = Helper.edgeCount;
        Response vertexDegreeResponse = executeApiCall(Helper.degree(name));
        int vertexDegree = extractFromResponse(vertexDegreeResponse, "vertexDegree");
        assert(java.util.Objects.equals(vertexDegree, Helper.Adj.get(name).size()) && vertexDegree >= 0 && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
        return vertexDegree;
    }

    public boolean hasEdge_helper() {
        String from = "from" /* CLIENT_INPUT: replace with the literal SPF solves for "from" */;
        String to = "to" /* CLIENT_INPUT: replace with the literal SPF solves for "to" */;
        assumeTrue(Helper.V.contains(from) && Helper.V.contains(to));
        int edgeCount_old = Helper.edgeCount;
        Response connectedResponse = executeApiCall(Helper.hasEdge(from, to));
        boolean connected = extractFromResponse(connectedResponse, "connected");
        assert(java.util.Objects.equals(connected, Helper.Adj.get(from).contains(to)) && java.util.Objects.equals(Helper.edgeCount, edgeCount_old));
        return connected;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.addVertex_helper();
        instance.addVertex_helper();
        instance.addEdge_helper();
        int vertexDegree = instance.degree_helper();
        boolean connected = instance.hasEdge_helper();
    }

    // ── Dynamic data binding support ──────────────────────────────────
    // SERVER_OUTPUT values are read back from the call at RUNTIME rather
    // than solved for, which is what separates this flavour from the SPF one.
    static class Response {
        private final Object payload;
        Response(Object payload) { this.payload = payload; }
        Object payload() { return payload; }
    }

    static Response executeApiCall(Object returnedValue) {
        return new Response(returnedValue);
    }

    @SuppressWarnings("unchecked")
    static <T> T extractFromResponse(Response response, String name) {
        return (T) response.payload();
    }

    @Test
    public void testSequence() {
        main(new String[0]);
    }
}
