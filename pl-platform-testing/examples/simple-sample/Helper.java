import java.util.*;

// ===================================
// Utility Classes
// ===================================

// Replacement for std::pair
class Pair<K, V> {
    public K key;
    public V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}

// ===================================
// Program Structure
// ===================================

// Program class that uses NewGrammar's Stmt
class Program {
    final List<Stmt> statements;

    public Program(List<Stmt> statements) { this.statements = statements; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Stmt s : statements) sb.append(s).append("\n");
        return sb.toString();
    }
}

// ===================================
// Specification and Symbol Table
// ===================================

// Response represents a post-condition expression (using NewGrammar's Expr)
class Response {
    final Expr expr; // The post-condition

    public Response(Expr expr) { this.expr = expr; }

    @Override
    public String toString() { return "Response(" + expr + ")"; }
}

// FuncCallSpec represents a function call in a specification (using NewGrammar's MethodCallExpr)
class FuncCallSpec { // Like C++ 'Call'
    final MethodCallExpr call; // Using NewGrammar's MethodCallExpr

    public FuncCallSpec(MethodCallExpr call) { this.call = call; }

    @Override
    public String toString() { return "SpecCall(" + call + ")"; }
}

// FunctionSpec represents a complete function specification with pre-condition, call, and post-condition
class FunctionSpec { // Like C++ 'API'
    final Expr pre; // Pre-condition (using NewGrammar's Expr)
    final FuncCallSpec call;
    final Response response;

    public FunctionSpec(Expr pre, FuncCallSpec call, Response response) {
        this.pre = pre;
        this.call = call;
        this.response = response;
    }

    @Override
    public String toString() {
        return "FunctionSpec(Pre: " + pre + ", Call: " + call + ", Resp: " + response + ")";
    }
}

// Specification contains a list of function specifications
class Specification { // Like C++ 'Spec'
    final List<FunctionSpec> blocks;

    public Specification(List<FunctionSpec> blocks) { this.blocks = blocks; }

    @Override
    public String toString() { return "Specification" + blocks; }
}

class SymbolTable {
    Set<String> variables = new HashSet<>();
    List<SymbolTable> children = new ArrayList<>();

    public boolean exists(String name) { return variables.contains(name); }

    @Override
    public String toString() {
        return "SymbolTable(vars=" + variables + ", children=" + children.size() + ")";
    }
}

// TypeMap maps variable names to their types (using NewGrammar's Type)
class TypeMap {
    Map<String, Type> mapping = new HashMap<>();

    @Override
    public String toString() { return mapping.toString(); }
}

// ===================================
// Compatibility Classes for Collections
// ===================================

// These classes extend NewGrammar's Expr for backward compatibility
// with code that uses SetExpr, TupleExpr, and MapExpr

class SetExpr extends Expr {
    final List<Expr> elements;
    public SetExpr(List<Expr> elements) { this.elements = elements; }
    @Override public String toString() { return "{" + elements + "}"; }
}

class TupleExpr extends Expr {
    final List<Expr> elements;
    public TupleExpr(List<Expr> elements) { this.elements = elements; }
    @Override public String toString() { return "(" + elements + ")"; }
}

class MapExpr extends Expr {
    final List<Pair<NameExpr, Expr>> entries; // Using NameExpr instead of Var
    public MapExpr(List<Pair<NameExpr, Expr>> entries) { this.entries = entries; }
    @Override public String toString() { return "{" + entries + "}"; }
}
