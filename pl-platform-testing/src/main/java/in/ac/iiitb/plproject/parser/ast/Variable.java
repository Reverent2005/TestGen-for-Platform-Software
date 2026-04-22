package in.ac.iiitb.plproject.parser.ast;

/**
 * Represents a variable with a name, type, and origin classification.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CHANGES FROM ORIGINAL
 * ─────────────────────────────────────────────────────────────────────────────
 * Added:
 *   1. VariableOrigin enum (CLIENT_INPUT / SERVER_OUTPUT)
 *   2. `origin` field, defaulting to CLIENT_INPUT in all existing constructors
 *   3. Two new constructors that accept an explicit VariableOrigin
 *   4. getOrigin(), setOrigin(), isServerOutput() methods
 *   5. Updated toString() to include origin
 *
 * Why:
 *   The dry-run PDF (§3 Type Map, §10 Test Case Generation) distinguishes two
 *   classes of variables:
 *     • CLIENT_INPUT  – uid, item.  SPF solves a concrete value; the JUnit
 *                       generator hardens it as a literal in the test.
 *     • SERVER_OUTPUT – orderId.  SPF creates a symbolic placeholder solely for
 *                       constraint solving; the JUnit generator ignores that
 *                       placeholder and instead emits code that extracts the
 *                       real value from the HTTP response at runtime, piping it
 *                       into downstream blocks (dynamic data binding).
 *   Every other layer (JmlFunctionSpec, transformer, code-gen) reads this flag
 *   to decide how to treat the variable.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class Variable {

    // ── NEW ──────────────────────────────────────────────────────────────────
    /**
     * Classifies where a variable's concrete value comes from at test runtime.
     *
     * Dry-run PDF §3 uses the labels CLIENT_INPUT and SERVER_OUTPUT verbatim.
     */
    public enum VariableOrigin {
        /**
         * The caller supplies or SPF solves this value.
         * In generated JUnit code it becomes a hardcoded literal
         * (e.g. String uid = "user_abc";).
         */
        CLIENT_INPUT,

        /**
         * The server generates this value during the API call
         * (e.g. an auto-incremented order ID).
         * SPF still creates a Debug.makeSymbolicString placeholder so the
         * solver can reason about constraints, but the JUnit generator
         * discards that string and instead emits runtime extraction code
         * (e.g. String orderId = response.jsonPath().getString("orderId");).
         */
        SERVER_OUTPUT
    }
    // ── END NEW ──────────────────────────────────────────────────────────────

    private String name;
    private String typeName;
    private VariableOrigin origin; // NEW field

    // ── EXISTING constructor – unchanged signature, defaults to CLIENT_INPUT ─
    public Variable(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
        this.origin = VariableOrigin.CLIENT_INPUT; // NEW default assignment
    }

    // ── NEW constructor ───────────────────────────────────────────────────────
    /**
     * Use this when the spec parser (or NewGenATC) already knows the origin.
     * Example:
     *   new Variable("orderId", "String", VariableOrigin.SERVER_OUTPUT)
     */
    public Variable(String name, String typeName, VariableOrigin origin) {
        this.name = name;
        this.typeName = typeName;
        this.origin = origin;
    }
    // ── END NEW ──────────────────────────────────────────────────────────────

    // ── EXISTING constructor – unchanged signature, defaults to CLIENT_INPUT ─
    public Variable(String name, Object type) {
        this.name = name;
        this.typeName = type.toString();
        this.origin = VariableOrigin.CLIENT_INPUT; // NEW default assignment
    }

    // ── NEW constructor ───────────────────────────────────────────────────────
    /**
     * Overload accepting a Type object plus an explicit origin.
     */
    public Variable(String name, Object type, VariableOrigin origin) {
        this.name = name;
        this.typeName = type.toString();
        this.origin = origin;
    }
    // ── END NEW ──────────────────────────────────────────────────────────────

    // ── EXISTING getters – unchanged ─────────────────────────────────────────
    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    // ── NEW getters / setters / helpers ──────────────────────────────────────
    /**
     * Returns the origin tag for this variable.
     * Read by AtcIrToSymbolicIrTransformer and AtcIrCodeGenerator.
     */
    public VariableOrigin getOrigin() {
        return origin;
    }

    /**
     * Allows the spec parser or NewGenATC to promote a variable to
     * SERVER_OUTPUT after construction (e.g. once a \result binding is found).
     */
    public void setOrigin(VariableOrigin origin) {
        this.origin = origin;
    }

    /**
     * Convenience helper: returns true when the JUnit generator must emit
     * runtime response-extraction code instead of a hardcoded literal.
     *
     * Dry-run PDF §10: "Variables tagged SERVER_OUTPUT trigger dynamic data
     * binding. The generator ignores SPF's concrete string and writes JUnit
     * code that extracts the ID from the HTTP response at runtime."
     */
    public boolean isServerOutput() {
        return origin == VariableOrigin.SERVER_OUTPUT;
    }
    // ── END NEW ──────────────────────────────────────────────────────────────

    // ── EXISTING toString – extended to show origin ───────────────────────────
    @Override
    public String toString() {
        return name + ": " + typeName + " [" + origin + "]";
    }
}
