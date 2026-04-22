package in.ac.iiitb.plproject.parser.ast;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;           // NEW
import java.util.HashSet;       // NEW
import java.util.regex.Matcher; // NEW
import java.util.regex.Pattern; // NEW
import in.ac.iiitb.plproject.ast.Expr;

/**
 * Represents a JML function specification with preconditions, postconditions,
 * and function signature.
 *
 * JML can have multiple requires and ensures clauses. They are combined with AND.
 * The precondition and postcondition fields store the combined expression (AST Expr).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CHANGES FROM ORIGINAL
 * ─────────────────────────────────────────────────────────────────────────────
 * Added:
 *   1. `serverOutputVars` Set<String> field – tracks variable names that are
 *      bound to \result in the postcondition (SERVER_OUTPUT origin).
 *   2. Auto-detection in both constructors via detectResultBindings().
 *   3. addServerOutputVar(String)  – manual registration (called by parser).
 *   4. getServerOutputVars()       – read by NewGenATC to tag Variables.
 *   5. isServerOutput(String)      – convenience predicate.
 *   6. static detectResultBindings(Expr) – walks postcondition string repr for
 *      "\result = varName" patterns and returns the bound variable names.
 *
 * Why:
 *   The spec postcondition for placeOrder contains:
 *       \result = orderId  ∧  Orders' = Orders ∪ {\result → uid}
 *   This tells us that `orderId` is server-generated. detectResultBindings()
 *   finds that binding automatically so NewGenATC can tag the corresponding
 *   Variable with VariableOrigin.SERVER_OUTPUT without needing to re-parse.
 *   The dry-run PDF §3 (Type Map) uses this exact distinction.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class JmlFunctionSpec {
    private String name;
    private FunctionSignature signature;
    private Expr precondition;
    private Expr postcondition;
    private List<Expr> requiresClauses;
    private List<Expr> ensuresClauses;

    // ── NEW field ─────────────────────────────────────────────────────────────
    /**
     * Variable names whose values are produced by the server (bound to \result
     * in the postcondition). Populated automatically by detectResultBindings()
     * and optionally extended by the caller via addServerOutputVar().
     *
     * Dry-run PDF §3: orderId has Origin = SERVER_OUTPUT because the
     * postcondition of placeOrder contains "\result = orderId".
     */
    private Set<String> serverOutputVars;
    // ── END NEW ───────────────────────────────────────────────────────────────

    /**
     * Constructor that takes individual requires and ensures clauses.
     * They will be combined with AND operations.
     */
    public JmlFunctionSpec(String name, FunctionSignature signature,
                           List<Expr> requiresClauses, List<Expr> ensuresClauses) {
        this.name = name;
        this.signature = signature;
        this.requiresClauses = requiresClauses != null
                ? new ArrayList<>(requiresClauses) : new ArrayList<>();
        this.ensuresClauses = ensuresClauses != null
                ? new ArrayList<>(ensuresClauses) : new ArrayList<>();
        this.precondition  = combineWithAnd(this.requiresClauses);
        this.postcondition = combineWithAnd(this.ensuresClauses);

        // ── NEW: auto-detect SERVER_OUTPUT vars from postcondition ────────────
        this.serverOutputVars = new HashSet<>();
        if (this.postcondition != null) {
            this.serverOutputVars.addAll(detectResultBindings(this.postcondition));
        }
        // ── END NEW ───────────────────────────────────────────────────────────
    }

    /**
     * Constructor for backward compatibility – takes single precondition and
     * postcondition (already combined with AND).
     */
    public JmlFunctionSpec(String name, FunctionSignature signature,
                           Expr precondition, Expr postcondition) {
        this.name = name;
        this.signature = signature;
        this.precondition = precondition;
        this.postcondition = postcondition;
        this.requiresClauses = new ArrayList<>();
        this.ensuresClauses = new ArrayList<>();
        if (precondition != null)  this.requiresClauses.add(precondition);
        if (postcondition != null) this.ensuresClauses.add(postcondition);

        // ── NEW: auto-detect SERVER_OUTPUT vars from postcondition ────────────
        this.serverOutputVars = new HashSet<>();
        if (this.postcondition != null) {
            this.serverOutputVars.addAll(detectResultBindings(this.postcondition));
        }
        // ── END NEW ───────────────────────────────────────────────────────────
    }

    /**
     * Helper method to combine multiple expressions with AND.
     */
    private Expr combineWithAnd(List<Expr> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return null;
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        Expr result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            result = in.ac.iiitb.plproject.ast.AstHelper
                        .createBinaryExpr(result, expressions.get(i), "AND");
        }
        return result;
    }

    // ── EXISTING getters – unchanged ─────────────────────────────────────────
    public String getName() { return name; }

    public FunctionSignature getSignature() { return signature; }

    /** Get the combined precondition (all requires clauses combined with AND). */
    public Expr getPrecondition() { return precondition; }

    /** Get the combined postcondition (all ensures clauses combined with AND). */
    public Expr getPostcondition() { return postcondition; }

    /** Get individual requires clauses. */
    public List<Expr> getRequiresClauses() { return new ArrayList<>(requiresClauses); }

    /** Get individual ensures clauses. */
    public List<Expr> getEnsuresClauses() { return new ArrayList<>(ensuresClauses); }

    // ── NEW methods ───────────────────────────────────────────────────────────

    /**
     * Manually register a variable name as SERVER_OUTPUT.
     *
     * The spec parser should call this when it encounters a \result binding in
     * an ensures clause that wasn't caught by the automatic regex scan, e.g.
     * when the binding is expressed through a custom type or alias.
     *
     * Example (called by JmlSpecParser):
     *   spec.addServerOutputVar("orderId");
     */
    public void addServerOutputVar(String varName) {
        serverOutputVars.add(varName);
    }

    /**
     * Returns the set of variable names that are server-generated return values.
     * NewGenATC reads this to construct Variables with the correct origin:
     *
     *   for (Variable v : spec.getSignature().getParameters()) {
     *       if (spec.isServerOutput(v.getName())) {
     *           v.setOrigin(VariableOrigin.SERVER_OUTPUT);
     *       }
     *   }
     */
    public Set<String> getServerOutputVars() {
        return new HashSet<>(serverOutputVars);
    }

    /**
     * Returns true if the named variable is a SERVER_OUTPUT for this function.
     * Called by NewGenATC when deciding whether to emit AtcReturnCaptureStmt
     * (for the producing block) or AtcPropagatedInputStmt (for consuming blocks).
     */
    public boolean isServerOutput(String varName) {
        return serverOutputVars.contains(varName);
    }

    /**
     * Walks the postcondition's string representation to find variable names
     * that are bound to {@code \result}.
     *
     * Recognised patterns (all whitespace-tolerant):
     *   \result = varName
     *   \result == varName
     *   result = varName          (JML without backslash)
     *   result == varName
     *
     * For the dry-run PDF example:
     *   postcondition = "\result = orderId ∧ Orders' = Orders ∪ {\result → uid}"
     *   → returns {"orderId"}
     *
     * Implementation note: the Expr AST nodes vary by subclass and some require
     * reflection to traverse (as seen in AtcIrCodeGenerator). Using the string
     * representation is more robust here and avoids coupling to AST internals.
     * If your Expr subclasses expose a proper visitor API, replace this with a
     * dedicated visitor for more precise matching.
     *
     * @param postcondition the combined postcondition expression
     * @return set of variable names bound to \result; empty if none found
     */
    public static Set<String> detectResultBindings(Expr postcondition) {
        Set<String> bindings = new HashSet<>();
        if (postcondition == null) return bindings;

        String repr = postcondition.toString();

        // Match: \result ==/= varName   OR   result ==/= varName
        // The variable name must start with a letter/underscore and contain only
        // word characters. We stop before operators, spaces, ∧, ;, ∪, {, }.
        Pattern p = Pattern.compile(
            "(?:\\\\result|\\bresult)\\s*==?\\s*([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher m = p.matcher(repr);
        while (m.find()) {
            String candidate = m.group(1);
            // Exclude Java keywords / boolean literals that can appear in specs
            if (!candidate.equals("null") && !candidate.equals("true")
                    && !candidate.equals("false")) {
                bindings.add(candidate);
            }
        }

        // Also match the reverse: varName = \result  (some spec styles write it
        // this way, though the dry-run PDF uses \result = varName)
        Pattern pReverse = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*==?\\s*(?:\\\\result|\\bresult)\\b");
        Matcher mReverse = pReverse.matcher(repr);
        while (mReverse.find()) {
            String candidate = mReverse.group(1);
            if (!candidate.equals("null") && !candidate.equals("true")
                    && !candidate.equals("false")) {
                bindings.add(candidate);
            }
        }

        return bindings;
    }
    // ── END NEW ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JmlFunctionSpec(");
        sb.append("Name: ").append(name);
        if (signature != null) sb.append(", Signature: ").append(signature);
        if (precondition != null)  sb.append(", Pre: ").append(precondition);
        else sb.append(", Pre: (none)");
        if (postcondition != null) sb.append(", Post: ").append(postcondition);
        else sb.append(", Post: (none)");
        // ── NEW: show server-output vars in toString ──────────────────────────
        if (!serverOutputVars.isEmpty()) {
            sb.append(", ServerOutputVars: ").append(serverOutputVars);
        }
        // ── END NEW ───────────────────────────────────────────────────────────
        sb.append(")");
        return sb.toString();
    }
}
