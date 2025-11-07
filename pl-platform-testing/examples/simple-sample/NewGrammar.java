import java.util.*;

// ===================================
// Base Node and Utilities
// ===================================

abstract class Node { }

abstract class Expr extends Node { }

abstract class Stmt extends Node { }

abstract class Type extends Node { }

class Name extends Node {
    final String identifier;
    public Name(String identifier) { this.identifier = identifier; }
    @Override public String toString() { return identifier; }
}

class SimpleName extends Name {
    public SimpleName(String identifier) { super(identifier); }
}

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
    enum Operator { 
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

class MethodCallExpr extends Expr {
    final Expr scope;
    final SimpleName name;
    final List<Expr> args;
    public MethodCallExpr(Expr scope, SimpleName name, List<Expr> args) {
        this.scope = scope;
        this.name = name;
        this.args = args;
    }
    @Override public String toString() { return (scope != null ? scope + "." : "") + name + "(" + args + ")"; }
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
// JML Documentation Nodes
// ===================================

class JmlDoc extends Node {
    final String content; // The actual JML comment content (without //@ or /*@ ... @*/)
    public JmlDoc(String content) { this.content = content; }
    @Override public String toString() { return "/*@ " + content + " @*/"; }
}

class JmlDocDeclaration extends Node {
    final JmlDoc jmlDoc;
    final Node target; // The AST element (method, class, field, etc.) that this doc annotates

    public JmlDocDeclaration(JmlDoc jmlDoc, Node target) {
        this.jmlDoc = jmlDoc;
        this.target = target;
    }

    @Override public String toString() { 
        return jmlDoc + "\n" + target; 
    }
}

class FieldDeclaration extends Node {
    final List<Modifier> modifiers;
    final VariableDeclarator var;

    public FieldDeclaration(List<Modifier> modifiers, VariableDeclarator var) {
        this.modifiers = modifiers;
        this.var = var;
    }

    @Override
    public String toString() { return modifiers + " " + var + ";"; }
}

enum Modifier { PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL }

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

class BreakStmt extends Stmt {
    @Override public String toString() { return "break;"; }
}

class ThrowStmt extends Stmt {
    final Expr expr;
    public ThrowStmt(Expr expr) { this.expr = expr; }
    @Override public String toString() { return "throw " + expr + ";"; }
}

class TryStmt extends Stmt {
    final BlockStmt tryBlock;
    final List<CatchClause> catches;
    public TryStmt(BlockStmt tryBlock, List<CatchClause> catches) {
        this.tryBlock = tryBlock;
        this.catches = catches;
    }
    @Override public String toString() { return "try " + tryBlock + " " + catches; }
}

class CatchClause extends Node {
    final Parameter param;
    final BlockStmt body;
    public CatchClause(Parameter param, BlockStmt body) { this.param = param; this.body = body; }
    @Override public String toString() { return "catch(" + param + ") " + body; }
}

// ===================================
// Control Structures
// ===================================

class ForEachStmt extends Stmt {
    final VariableDeclarator var;
    final Expr iterable;
    final Stmt body;
    public ForEachStmt(VariableDeclarator var, Expr iterable, Stmt body) {
        this.var = var;
        this.iterable = iterable;
        this.body = body;
    }
    @Override public String toString() {
        return "for (" + var + " : " + iterable + ") " + body;
    }
}

class SwitchStmt extends Stmt {
    final Expr selector;
    final List<SwitchEntry> entries;
    public SwitchStmt(Expr selector, List<SwitchEntry> entries) {
        this.selector = selector;
        this.entries = entries;
    }
    @Override public String toString() { return "switch(" + selector + ") " + entries; }
}

class SwitchEntry extends Node {
    final Expr label;
    final List<Stmt> statements;
    public SwitchEntry(Expr label, List<Stmt> statements) {
        this.label = label;
        this.statements = statements;
    }
    @Override public String toString() { return "case " + label + ": " + statements; }
}

// ===================================
// Methods, Classes, Enums
// ===================================

class Parameter extends Node {
    final Type type;
    final String name;
    public Parameter(Type type, String name) { this.type = type; this.name = name; }
    @Override public String toString() { return type + " " + name; }
}

class MethodDeclaration extends Node {
    final List<Modifier> modifiers;
    final Type returnType;
    final String name;
    final List<Parameter> params;
    final BlockStmt body;

    public MethodDeclaration(List<Modifier> modifiers, Type returnType, String name, List<Parameter> params, BlockStmt body) {
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override public String toString() {
        return modifiers + " " + returnType + " " + name + "(" + params + ") " + body;
    }
}

class ConstructorDeclaration extends Node {
    final String name;
    final List<Parameter> params;
    final BlockStmt body;

    public ConstructorDeclaration(String name, List<Parameter> params, BlockStmt body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }

    @Override public String toString() { return name + "(" + params + ") " + body; }
}

class ClassOrInterfaceDeclaration extends Node {
    final String name;
    final List<Modifier> modifiers;
    final List<Node> members;

    public ClassOrInterfaceDeclaration(String name, List<Modifier> modifiers, List<Node> members) {
        this.name = name;
        this.modifiers = modifiers;
        this.members = members;
    }

    @Override public String toString() {
        return modifiers + " class " + name + " " + members;
    }
}

class EnumConstantDeclaration extends Node {
    final String name;
    public EnumConstantDeclaration(String name) { this.name = name; }
    @Override public String toString() { return name; }
}

class EnumDeclaration extends Node {
    final String name;
    final List<EnumConstantDeclaration> entries;
    public EnumDeclaration(String name, List<EnumConstantDeclaration> entries) {
        this.name = name;
        this.entries = entries;
    }
    @Override public String toString() { return "enum " + name + " " + entries; }
}

// ===================================
// Compilation Unit
// ===================================

class PackageDeclaration extends Node {
    final Name name;
    public PackageDeclaration(Name name) { this.name = name; }
    @Override public String toString() { return "package " + name + ";"; }
}

class ImportDeclaration extends Node {
    final Name name;
    public ImportDeclaration(Name name) { this.name = name; }
    @Override public String toString() { return "import " + name + ";"; }
}

class CompilationUnit extends Node {
    final PackageDeclaration pkg;
    final List<ImportDeclaration> imports;
    final List<Node> types;

    public CompilationUnit(PackageDeclaration pkg, List<ImportDeclaration> imports, List<Node> types) {
        this.pkg = pkg;
        this.imports = imports;
        this.types = types;
    }

    @Override
    public String toString() {
        return (pkg != null ? pkg + "\n" : "") + imports + "\n" + types;
    }
}
