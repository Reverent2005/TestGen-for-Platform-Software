# JML Spec Parser — v1

A standalone parser for `.spec` files describing Java method contracts
(preconditions, postconditions, signatures) using JFlex + CUP.

---

## Project Structure

```
jml-parser/
├── examples/
│   ├── MathUtils.spec       ← sample contracts for math functions
│   ├── BankAccount.spec     ← sample contracts with \old() and state
│   └── ArrayUtils.spec      ← sample contracts with arrays and null checks
│
├── grammar/
│   └── grammar.md           ← full BNF grammar reference
│
└── src/
    ├── lexer/
    │   └── SpecLexer.flex   ← JFlex lexer spec
    ├── parser/
    │   └── SpecParser.cup   ← CUP grammar + action code
    └── ast/
        └── ASTNodes.java    ← All AST node classes
```

---

## The Spec Language (quick reference)

```
spec <name> {
    signature: <returnType> <methodName>(<type> <param>, ...);
    requires:  <boolean expression>;
    ensures:   <boolean expression>;
}
```

### Supported types
`int`, `double`, `boolean`, `float`, `long`, `char`, `byte`, `short`, `void`,
`String`, any `IDENT` (user-defined class), and array variants like `int[]`.

### Condition expressions support
- Comparisons:  `==`, `!=`, `<`, `>`, `<=`, `>=`
- Logic:        `&&`, `||`, `!`
- Arithmetic:   `+`, `-`, `*`, `/`, unary `-`
- Access:       `obj.field`, `obj.method()`, `arr[i]`
- JML tokens:   `\result`, `\old(expr)`
- Literals:     integers, doubles, strings, `true`, `false`, `null`

---

## Build Instructions

### Prerequisites
- Java 8+
- [JFlex 1.9+](https://jflex.de/)  — `jflex` on PATH
- [CUP 0.11+](http://www2.cs.tum.edu/projects/cup/) — `cup.jar` available

### Step 1 — Generate the lexer
```bash
jflex src/lexer/SpecLexer.flex
# Produces: src/lexer/SpecLexer.java
```

### Step 2 — Generate the parser
```bash
java -jar cup.jar \
  -package parser \
  -parser SpecParser \
  -symbols sym \
  src/parser/SpecParser.cup
# Produces: src/parser/SpecParser.java
#           src/parser/sym.java
```

### Step 3 — Compile everything
```bash
javac -cp .:cup-runtime.jar \
  src/lexer/SpecLexer.java \
  src/parser/sym.java \
  src/parser/SpecParser.java \
  src/ast/ASTNodes.java
```

### Step 4 — Run against an example
```bash
java -cp .:cup-runtime.jar parser.SpecParser < examples/MathUtils.spec
```

---

## What the Parser Produces

For each `spec` block the parser builds a `SpecDecl` object containing:

| Field       | Type        | Contains                              |
|-------------|-------------|---------------------------------------|
| `name`      | `String`    | The spec/method name                  |
| `signature` | `Signature` | Return type, method name, params list |
| `requires`  | `Condition` | AST tree of the precondition          |
| `ensures`   | `Condition` | AST tree of the postcondition         |

These objects are the input to your contract testing framework.

---

## Example Parse Output

Input:
```
spec divide {
    signature: double divide(double a, double b);
    requires:  b != 0.0;
    ensures:   \result == a / b;
}
```

Produces:
```
SpecDecl(divide)
    sig:      double divide([double a, double b])
    requires: b != 0.0
    ensures:  \result == (a / b)
```

---

## Next Steps (v2)

- [ ] Semantic checker: validate `\result` only in `ensures`, param names match signature
- [ ] Multiple `requires` / `ensures` clauses per spec
- [ ] `\forall`, `\exists` quantifiers
- [ ] Export contracts as JSON for test framework consumption
- [ ] Main driver class to parse a file and print the AST
