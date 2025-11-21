package in.ac.iiitb.plproject.ast;

import java.util.*;

// ===================================
// Base Node and Utilities
// ===================================

// Node is now public and moved to Node.java for access from other packages
// Expr is now public and moved to Expr.java for access from other packages

abstract class Stmt extends Node { }

// Type is now in Type.java
// Name and SimpleName are now public and moved to separate files

// ===================================
// Literals
// ===================================

class IntegerLiteralExpr extends Expr {
    final int value;
    public IntegerLiteralExpr(int value) { this.value = value; }
    @Override public String toString() { return Integer.toString(value); }
}

class DoubleLiteralExpr extends Expr {
    final double value;
    public DoubleLiteralExpr(double value) { this.value = value; }
    @Override public String toString() { return Double.toString(value); }
}

class StringLiteralExpr extends Expr {
    final String value;
    public StringLiteralExpr(String value) { this.value = value; }
    @Override public String toString() { return "\"" + value + "\""; }
}

class BooleanLiteralExpr extends Expr {
    final boolean value;
    public BooleanLiteralExpr(boolean value) { this.value = value; }
    @Override public String toString() { return Boolean.toString(value); }
}

// ===================================
// Variable and Field Access
// ===================================

class NameExpr extends Expr {
    final Name name;
    public NameExpr(Name name) { this.name = name; }
    @Override public String toString() { return name.toString(); }
}

class FieldAccessExpr extends Expr {
    final Expr scope;
    final SimpleName field;
    public FieldAccessExpr(Expr scope, SimpleName field) {
        this.scope = scope;
        this.field = field;
    }
    @Override public String toString() { return scope + "." + field; }
}

class ThisExpr extends Expr {
    @Override public String toString() { return "this"; }
}

// ===================================
// Expressions
// ===================================

class BinaryExpr extends Expr {
    public enum Operator { 
        PLUS, MINUS, MULTIPLY, DIVIDE, 
        AND, OR, 
        EQUALS, NOT_EQUALS, 
        GREATER_THAN, GREATER_THAN_OR_EQUAL,
        LESS_THAN, LESS_THAN_OR_EQUAL 
    }
    final Expr left, right;
    final Operator op;
    public BinaryExpr(Expr left, Expr right, Operator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }
    @Override public String toString() { 
        String opStr = op.toString().toLowerCase().replace("_", " ");
        return "(" + left + " " + opStr + " " + right + ")"; 
    }
}

class AssignExpr extends Expr {
    final Expr target;
    final Expr value;
    public AssignExpr(Expr target, Expr value) {
        this.target = target;
        this.value = value;
    }
    @Override public String toString() { return target + " = " + value; }
}

class ObjectCreationExpr extends Expr {
    final Type type;
    final List<Expr> args;
    public ObjectCreationExpr(Type type, List<Expr> args) {
        this.type = type;
        this.args = args;
    }
    @Override public String toString() { return "new " + type + "(" + args + ")"; }
}

// MethodCallExpr is now public and moved to MethodCallExpr.java for access from other packages

class UnaryExpr extends Expr {
    public enum Operator {
        LOGICAL_COMPLEMENT, // !
        MINUS, // - (unary minus)
        PLUS // + (unary plus)
    }
    final Expr expr;
    final Operator op;
    public UnaryExpr(Expr expr, Operator op) {
        this.expr = expr;
        this.op = op;
    }
    @Override public String toString() {
        String opStr = op == Operator.LOGICAL_COMPLEMENT ? "!" : 
                       op == Operator.MINUS ? "-" : "+";
        return opStr + expr;
    }
}

// ===================================
// Types
// ===================================

class VoidType extends Type {
    @Override public String toString() { return "void"; }
}

class PrimitiveType extends Type {
    final String name;
    public PrimitiveType(String name) { this.name = name; }
    @Override public String toString() { return name; }
}

class ClassOrInterfaceType extends Type {
    final Name name;
    public ClassOrInterfaceType(Name name) { this.name = name; }
    @Override public String toString() { return name.toString(); }
}

// ===================================
// Declarations
// ===================================

class VariableDeclarator extends Node {
    final String name;
    final Type type;
    final Expr initializer;

    public VariableDeclarator(Type type, String name, Expr initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        return type + " " + name + (initializer != null ? " = " + initializer : "");
    }
}

// ===================================
// Statements
// ===================================

class ExpressionStmt extends Stmt {
    final Expr expr;
    public ExpressionStmt(Expr expr) { this.expr = expr; }
    @Override public String toString() { return expr + ";"; }
}

class BlockStmt extends Stmt {
    final List<Stmt> statements;
    public BlockStmt(List<Stmt> statements) { this.statements = statements; }
    @Override public String toString() { return "{\n" + statements + "\n}"; }
}

class IfStmt extends Stmt {
    final Expr condition;
    final Stmt thenStmt;
    final Stmt elseStmt;
    public IfStmt(Expr condition, Stmt thenStmt, Stmt elseStmt) {
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }
    @Override public String toString() {
        return "if (" + condition + ") " + thenStmt + (elseStmt != null ? " else " + elseStmt : "");
    }
}

class ReturnStmt extends Stmt {
    final Expr expr;
    public ReturnStmt(Expr expr) { this.expr = expr; }
    @Override public String toString() { return "return " + expr + ";"; }
}

