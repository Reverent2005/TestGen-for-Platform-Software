# JML Spec Language — Formal Grammar (v1)

## Overview

This is the BNF grammar for the standalone `.spec` file format.
It supports full JML-style preconditions and postconditions including
`\result` and `\old(expr)`.

---

## Token Definitions (for JFlex)

```
WHITESPACE   ::=  [ \t\r\n]+
LINE_COMMENT ::=  "//" [^\n]*
BLOCK_COMMENT::=  "/*" ... "*/"

keyword      ::=  "spec" | "signature" | "requires" | "ensures"
                | "true" | "false" | "null" | "void"
                | "int" | "double" | "boolean" | "float"
                | "long" | "char" | "byte" | "short" | "String"

RESULT       ::=  "\result"
OLD          ::=  "\old"

IDENT        ::=  [a-zA-Z_][a-zA-Z0-9_]*
INT_LIT      ::=  [0-9]+
DOUBLE_LIT   ::=  [0-9]+ "." [0-9]+
STRING_LIT   ::=  '"' [^"]* '"'
CHAR_LIT     ::=  "'" . "'"

LBRACE       ::=  "{"
RBRACE       ::=  "}"
LPAREN       ::=  "("
RPAREN       ::=  ")"
LBRACKET     ::=  "["
RBRACKET     ::=  "]"
SEMI         ::=  ";"
COLON        ::=  ":"
COMMA        ::=  ","
DOT          ::=  "."

EQ           ::=  "=="
NEQ          ::=  "!="
LT           ::=  "<"
GT           ::=  ">"
LEQ          ::=  "<="
GEQ          ::=  ">="
AND          ::=  "&&"
OR           ::=  "||"
NOT          ::=  "!"
PLUS         ::=  "+"
MINUS        ::=  "-"
TIMES        ::=  "*"
DIVIDE       ::=  "/"
ASSIGN       ::=  "="
```

---

## Grammar Productions (BNF)

```
spec_file     ::= spec_decl*

spec_decl     ::= "spec" IDENT "{"
                      sig_clause
                      req_clause
                      ens_clause
                  "}"

sig_clause    ::= "signature" ":" signature ";"

req_clause    ::= "requires"  ":" condition ";"

ens_clause    ::= "ensures"   ":" condition ";"


/* ── Signature ─────────────────────────────────────────────── */

signature     ::= type IDENT "(" param_list ")"

param_list    ::= param ("," param)*
              |   ε

param         ::= type IDENT

type          ::= primitive_type
              |   ref_type

primitive_type::= "int" | "double" | "boolean" | "float"
              |   "long" | "char" | "byte" | "short" | "void"

ref_type      ::= "String"
              |   IDENT
              |   type "[" "]"          /* array types: int[], String[] */


/* ── Conditions (boolean expressions) ──────────────────────── */

condition     ::= condition "||" and_expr
              |   and_expr

and_expr      ::= and_expr "&&" not_expr
              |   not_expr

not_expr      ::= "!" not_expr
              |   "(" condition ")"
              |   comparison

comparison    ::= expr rel_op expr
              |   expr

rel_op        ::= "==" | "!=" | "<" | ">" | "<=" | ">="


/* ── Expressions (arithmetic + field access) ────────────────── */

expr          ::= expr "+" term
              |   expr "-" term
              |   term

term          ::= term "*" factor
              |   term "/" factor
              |   factor

factor        ::= "-" factor
              |   primary

primary       ::= INT_LIT
              |   DOUBLE_LIT
              |   STRING_LIT
              |   "true"
              |   "false"
              |   "null"
              |   "\result"
              |   "\old" "(" expr ")"
              |   "(" expr ")"
              |   access_expr

access_expr   ::= IDENT                         /* simple var: x, balance */
              |   access_expr "." IDENT          /* field:  target.balance */
              |   access_expr "." IDENT "(" ")" /* no-arg call: s.length() */
              |   access_expr "[" expr "]"       /* array index: arr[i]    */
```

---

## Operator Precedence (lowest → highest)

| Level | Operators     | Associativity |
|-------|---------------|---------------|
| 1     | `\|\|`        | left          |
| 2     | `&&`          | left          |
| 3     | `!`           | right (unary) |
| 4     | `==` `!=` `<` `>` `<=` `>=` | left |
| 5     | `+` `-`       | left          |
| 6     | `*` `/`       | left          |
| 7     | unary `-`     | right         |
| 8     | `.` `[]` `()` | left          |

---

## Special JML Terminals

| Token      | Meaning                                        |
|------------|------------------------------------------------|
| `\result`  | The return value of the method                 |
| `\old(e)`  | The value of expression `e` before the call    |
| `true`     | Boolean literal true                           |
| `false`    | Boolean literal false                          |
| `null`     | Null reference                                 |

---

## Language Constraints (not enforced by grammar, checked later)

1. `\result` is only valid inside an `ensures` clause
2. `\old(e)` is only valid inside an `ensures` clause
3. A `void` method must not use `\result` in its `ensures`
4. Parameter names in `requires`/`ensures` must match `signature`
5. Each `spec` block must have exactly one of each clause

---

## Full Example

```
spec withdraw {
    signature: boolean withdraw(double amount);
    requires:  amount > 0.0 && amount <= balance;
    ensures:   (\result == true  && balance == \old(balance) - amount) ||
               (\result == false && balance == \old(balance));
}
```
