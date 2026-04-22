
package ast;

import java.util.List;

// ════════════════════════════════════════════════════════════════
//  AST Node Hierarchy
//  These are the Java objects the CUP parser builds.
//  Everything the parser produces ends up as one of these.
// ════════════════════════════════════════════════════════════════


// ── Top-level ────────────────────────────────────────────────

/**
 * Represents an entire .spec file.
 * Contains a list of SpecDecl, one per `spec { }` block.
 */
public class SpecFile {
    public final List<SpecDecl> specs;
    public SpecFile(List<SpecDecl> specs) { this.specs = specs; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("SpecFile [\n");
        for (SpecDecl s : specs) sb.append("  ").append(s).append("\n");
        return sb.append("]").toString();
    }
}


/**
 * One `spec name { signature / requires / ensures }` block.
 */
class SpecDecl {
    public final String    name;
    public final Signature signature;
    public final Condition requires;
    public final Condition ensures;

    public SpecDecl(String name, Signature sig, Condition req, Condition ens) {
        this.name      = name;
        this.signature = sig;
        this.requires  = req;
        this.ensures   = ens;
    }

    @Override public String toString() {
        return "SpecDecl(" + name + ")\n"
             + "    sig:      " + signature + "\n"
             + "    requires: " + requires  + "\n"
             + "    ensures:  " + ensures;
    }
}


// ── Signature ────────────────────────────────────────────────

/**
 * `type name(param, param, ...)`
 */
class Signature {
    public final String         returnType;
    public final String         methodName;
    public final List<Parameter> params;

    public Signature(String returnType, String methodName, List<Parameter> params) {
        this.returnType = returnType;
        this.methodName = methodName;
        this.params     = params;
    }

    @Override public String toString() {
        return returnType + " " + methodName + "(" + params + ")";
    }
}

/**
 * A single parameter: `type name`
 */
class Parameter {
    public final String type;
    public final String name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override public String toString() { return type + " " + name; }
}


// ── Condition nodes ──────────────────────────────────────────
// Conditions form a boolean-layer tree.

abstract class Condition {}

/** `left || right`  or  `left && right` */
class BinaryCondition extends Condition {
    public final Condition left, right;
    public final String    op;
    public BinaryCondition(Condition l, String op, Condition r) {
        this.left = l; this.op = op; this.right = r;
    }
    @Override public String toString() { return "(" + left + " " + op + " " + right + ")"; }
}

/** `! cond` */
class NotCondition extends Condition {
    public final Condition cond;
    public NotCondition(Condition c) { this.cond = c; }
    @Override public String toString() { return "(!" + cond + ")"; }
}

/** `( cond )` */
class ParenCondition extends Condition {
    public final Condition inner;
    public ParenCondition(Condition c) { this.inner = c; }
    @Override public String toString() { return "(" + inner + ")"; }
}

/** `expr op expr`  e.g.  `x > 0` */
class ComparisonCondition extends Condition {
    public final Expr   left, right;
    public final String op;
    public ComparisonCondition(Expr l, String op, Expr r) {
        this.left = l; this.op = op; this.right = r;
    }
    @Override public String toString() { return left + " " + op + " " + right; }
}

/** A bare expression used as a condition (e.g. `true`, `boolVar`) */
class ExprCondition extends Condition {
    public final Expr expr;
    public ExprCondition(Expr e) { this.expr = e; }
    @Override public String toString() { return expr.toString(); }
}


// ── Expression nodes ─────────────────────────────────────────
// Expressions form the arithmetic/access layer.

abstract class Expr {}

/** `left op right`  where op ∈ {+, -, *, /} */
class BinaryExpr extends Expr {
    public final Expr   left, right;
    public final String op;
    public BinaryExpr(Expr l, String op, Expr r) {
        this.left = l; this.op = op; this.right = r;
    }
    @Override public String toString() { return "(" + left + " " + op + " " + right + ")"; }
}

/** Unary minus: `-factor` */
class UnaryExpr extends Expr {
    public final String op;
    public final Expr   operand;
    public UnaryExpr(String op, Expr operand) { this.op = op; this.operand = operand; }
    @Override public String toString() { return op + operand; }
}

/** `( expr )` */
class ParenExpr extends Expr {
    public final Expr inner;
    public ParenExpr(Expr e) { this.inner = e; }
    @Override public String toString() { return "(" + inner + ")"; }
}

// ── Literals ─────────────────────────────────────────────────

class IntLiteral    extends Expr { public final int    value; public IntLiteral(int v)    { value=v; } @Override public String toString(){ return String.valueOf(value); } }
class DoubleLiteral extends Expr { public final double value; public DoubleLiteral(double v){ value=v; } @Override public String toString(){ return String.valueOf(value); } }
class StringLiteral extends Expr { public final String value; public StringLiteral(String v){ value=v; } @Override public String toString(){ return "\""+value+"\""; } }
class BoolLiteral   extends Expr { public final boolean value; public BoolLiteral(boolean v){ value=v; } @Override public String toString(){ return String.valueOf(value); } }
class NullLiteral   extends Expr { @Override public String toString(){ return "null"; } }

// ── JML special expressions ──────────────────────────────────

/** `\result` — the return value of the method */
class ResultExpr extends Expr {
    @Override public String toString() { return "\\result"; }
}

/** `\old(expr)` — value of expr in the pre-state */
class OldExpr extends Expr {
    public final Expr inner;
    public OldExpr(Expr e) { this.inner = e; }
    @Override public String toString() { return "\\old(" + inner + ")"; }
}

// ── Access expressions ───────────────────────────────────────

/** Plain variable or parameter: `x`, `balance` */
class VarExpr extends Expr {
    public final String name;
    public VarExpr(String name) { this.name = name; }
    @Override public String toString() { return name; }
}

/** Field access: `target.balance` */
class FieldExpr extends Expr {
    public final Expr   object;
    public final String field;
    public FieldExpr(Expr obj, String field) { this.object = obj; this.field = field; }
    @Override public String toString() { return object + "." + field; }
}

/** No-arg method call: `s.length()` */
class MethodCallExpr extends Expr {
    public final Expr   object;
    public final String method;
    public MethodCallExpr(Expr obj, String method) { this.object = obj; this.method = method; }
    @Override public String toString() { return object + "." + method + "()"; }
}

/** Array index: `arr[i]` */
class ArrayAccessExpr extends Expr {
    public final Expr object;
    public final Expr index;
    public ArrayAccessExpr(Expr obj, Expr idx) { this.object = obj; this.index = idx; }
    @Override public String toString() { return object + "[" + index + "]"; }
}
