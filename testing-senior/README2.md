# Test Generation Algorithm - Sample Examples

This project contains three versions of a test generation algorithm implemented in Java, along with sample examples demonstrating their usage.

## Files Overview

### Core Files
- **`NewGrammar.java`** - JML-based AST classes that mock jml-parser structure
- **`Helper.java`** - Contains specification classes, utility classes, and compatibility classes for collections
- **`Version1.java`** - First implementation of the test generation algorithm (updated to use new grammar)
- **`Version2.java`** - Second implementation with improved structure and documentation
- **`Version3.java`** - Third implementation with different approach

### Example Files
- **`SampleExample.java`** - Comprehensive example demonstrating all three versions
- **`run_example.bat`** - Windows batch script to compile and run examples
- **`README.md`** - This documentation file

## Algorithm Description

The test generation algorithm converts high-level specifications into verifiable programs. It takes:

1. **Specifications** - A collection of function specifications with pre-conditions, function calls, and post-conditions
2. **Symbol Tables** - Variable scope information for each function
3. **Type Maps** - Type information for variables

And generates:

1. **Input statements** - `input(variable)` for each input variable
2. **Assume statements** - `assume(precondition)` for pre-conditions
3. **State saving** - `old_variable = variable` for variables that appear in post-conditions
4. **Function calls** - The actual function calls with renamed variables
5. **Assert statements** - `assert(postcondition)` for post-conditions

## Key Features

### Variable Renaming
- Variables are renamed with suffixes (e.g., `x` becomes `x0`, `x1`) to avoid conflicts
- Dashed variables (e.g., `'x`) in post-conditions are handled specially

### Post-condition Transformation
- Variables marked with `'` (dash) or using `_post` suffix are converted to their base form
- Original state variables are renamed to `_old` version for comparison
- Post-state variables (e.g., `x_post`) represent the state after function execution

### Type System
- Supports primitive types via `PrimitiveType` - int, string, bool, double, etc.
- Supports class/interface types via `ClassOrInterfaceType` - Set, Map, List, etc.
- Handles complex data structures (sets, maps, tuples) via compatibility classes in `Helper.java`

## Running the Examples

### Method 1: Using the Batch Script (Windows)
```bash
run_example.bat
```

### Method 2: Manual Compilation and Execution
```bash
# Compile all files
javac *.java

# Run the sample example
java SampleExample

# Run with complex example
java -cp . SampleExample createComplexExample
```

## Sample Output

The algorithm generates programs like this:

```
input([x0]);
assume([(x0 greater than 0)]);
x0_old = x0;
process([x0]);
assert([(x0 greater than x0_old)]);
```

This represents:
1. `input(x0)` - Read input variable x0
2. `assume(x0 > 0)` - Assume precondition holds
3. `x0_old = x0` - Save original state
4. `process(x0)` - Call the function
5. `assert(x0 > x0_old)` - Verify postcondition

Note: The output format uses `ExpressionStmt` which wraps expressions, and binary operators are displayed with their operator names (e.g., "greater than" for `>`).

## Function Examples

The sample includes various function types:

### Arithmetic Functions
- `increment(x)` - adds 1 to x
- `decrement(x)` - subtracts 1 from x
- `multiply(x, y)` - returns x * y
- `divide(x, y)` - returns x / y

### String Functions
- `reverse(s)` - reverses string s
- `toUpperCase(s)` - converts s to uppercase
- `toLowerCase(s)` - converts s to lowercase
- `concat(s1, s2)` - concatenates s1 and s2

### Collection Functions
- `contains(set, item)` - checks if set contains item
- `add(set, item)` - adds item to set
- `remove(set, item)` - removes item from set
- `size(collection)` - returns size of collection

### Map Functions
- `put(map, key, value)` - adds key-value pair to map
- `get(map, key)` - retrieves value for key
- `update(map, data)` - updates map with new data
- `keys(map)` - returns all keys in map

### Comparison Operators
- Binary expressions using `BinaryExpr` with operators:
  - `GREATER_THAN` - a > b
  - `LESS_THAN` - a < b
  - `EQUALS` - a == b
  - `NOT_EQUALS` - a != b
  - `GREATER_THAN_OR_EQUAL` - a >= b
  - `LESS_THAN_OR_EQUAL` - a <= b

### Special Functions
- `input(var)` - reads input for variable (via `MethodCallExpr`)
- `assume(expr)` - assumes expression is true (via `MethodCallExpr`)
- `assert(expr)` - asserts expression must be true (via `MethodCallExpr`)
- Post-state variables - represented as `_post` suffix (e.g., `x_post`) or via `MethodCallExpr` with name `"'"` for JML-style post-state notation

## Version Differences

### Version 1
- Basic implementation with traditional Java syntax
- Uses explicit casting and null checks
- More verbose but clear structure

### Version 2
- Improved documentation and method names
- Uses modern Java features where possible
- Better separation of concerns

### Version 3
- Different approach to some transformations
- Includes additional input statements for old variables
- Slightly different output format

## Data Structures

### Core Expression Classes (from NewGrammar.java)
- **`Expr`** - Abstract base class for all expressions
- **`NameExpr`** - Variable references (wraps `SimpleName` or `Name`)
- **`IntegerLiteralExpr`** - Integer literals (e.g., `42`)
- **`DoubleLiteralExpr`** - Double literals (e.g., `3.14`)
- **`StringLiteralExpr`** - String literals (e.g., `"hello"`)
- **`MethodCallExpr`** - Method/function calls (e.g., `obj.method(args)`)
- **`BinaryExpr`** - Binary operations (e.g., `a + b`, `x > y`)
- **`AssignExpr`** - Assignment expressions (e.g., `x = y`)
- **`ObjectCreationExpr`** - Object instantiation (e.g., `new Set([1,2,3])`)
- **`FieldAccessExpr`** - Field access (e.g., `obj.field`)
- **`ThisExpr`** - `this` reference

### Compatibility Classes (from Helper.java)
- **`SetExpr`** - Set literals (extends `Expr` for backward compatibility)
- **`MapExpr`** - Map literals (extends `Expr` for backward compatibility)
- **`TupleExpr`** - Tuple literals (extends `Expr` for backward compatibility)

### Type Classes (from NewGrammar.java)
- **`Type`** - Abstract base class for types
- **`PrimitiveType`** - Primitive types (int, string, bool, double, etc.)
- **`ClassOrInterfaceType`** - Class/interface types (Set, Map, List, etc.)
- **`VoidType`** - Void type

### Specification Classes (from Helper.java)
- **`Specification`** - Collection of function specifications
- **`FunctionSpec`** - Individual function specification (pre-condition, call, post-condition)
- **`FuncCallSpec`** - Function call wrapper (wraps `MethodCallExpr`)
- **`Response`** - Post-condition wrapper (wraps `Expr`)

### Statement Classes (from NewGrammar.java)
- **`Stmt`** - Abstract base class for statements
- **`ExpressionStmt`** - Expression statements (wraps an `Expr`)
- **`BlockStmt`** - Block statements (sequence of statements)
- **`IfStmt`** - If statements
- **`ReturnStmt`** - Return statements
- **`Program`** - Program structure (from Helper.java, contains list of `Stmt`)

### Utility Classes
- **`SymbolTable`** - Variable scope management
- **`TypeMap`** - Type information storage
- **`Pair<K,V>`** - Generic pair class

## Example Specifications

The sample creates specifications using the new JML-based grammar:

```java
// Function 1: increment(x) where x > 0, result: x' > x
Expr pre1 = new BinaryExpr(
    new NameExpr(new SimpleName("x")),
    new IntegerLiteralExpr(0),
    BinaryExpr.Operator.GREATER_THAN
);

MethodCallExpr call1 = new MethodCallExpr(
    null,  // no scope for static/global function
    new SimpleName("increment"),
    Arrays.asList(new NameExpr(new SimpleName("x")))
);

// Post-condition: x_post > x (x_post represents x')
Expr post1 = new BinaryExpr(
    new NameExpr(new SimpleName("x_post")),  // represents x'
    new NameExpr(new SimpleName("x")),
    BinaryExpr.Operator.GREATER_THAN
);

FunctionSpec spec1 = new FunctionSpec(
    pre1,                                    // pre: x > 0
    new FuncCallSpec(call1),                 // call: increment(x)
    new Response(post1)                      // post: x' > x
);
```

### Complex Example with Object Creation

```java
// Function: process(data, result) where Set contains 2, result' == update(result, data)
MethodCallExpr containsCall = new MethodCallExpr(
    new ObjectCreationExpr(
        new ClassOrInterfaceType(new SimpleName("Set")),
        Arrays.asList(
            new IntegerLiteralExpr(1),
            new IntegerLiteralExpr(2),
            new IntegerLiteralExpr(3)
        )
    ),
    new SimpleName("contains"),
    Arrays.asList(new IntegerLiteralExpr(2))
);

MethodCallExpr call = new MethodCallExpr(
    null,
    new SimpleName("process"),
    Arrays.asList(
        new NameExpr(new SimpleName("data")),
        new NameExpr(new SimpleName("result"))
    )
);

Expr post = new BinaryExpr(
    new NameExpr(new SimpleName("result_post")),  // represents result'
    new MethodCallExpr(
        null,
        new SimpleName("update"),
        Arrays.asList(
            new NameExpr(new SimpleName("result")),
            new NameExpr(new SimpleName("data"))
        )
    ),
    BinaryExpr.Operator.EQUALS
);
```

## Extending the Examples

To create your own examples:

1. **Define variables** in symbol tables (`SymbolTable`)
2. **Define types** in type maps (`TypeMap`) using `PrimitiveType` or `ClassOrInterfaceType`
3. **Create expressions** using the new AST classes:
   - Use `NameExpr(new SimpleName("varName"))` for variables
   - Use `IntegerLiteralExpr`, `DoubleLiteralExpr`, `StringLiteralExpr` for literals
   - Use `BinaryExpr` for comparisons and arithmetic operations
   - Use `MethodCallExpr` for function/method calls
   - Use `ObjectCreationExpr` for creating objects
4. **Build specifications** with pre-conditions, calls, and post-conditions
5. **Run through any version** of the algorithm
6. **Examine the generated program**

### Expression Creation Helpers

```java
// Create a variable reference
NameExpr varX = new NameExpr(new SimpleName("x"));

// Create a number literal
IntegerLiteralExpr zero = new IntegerLiteralExpr(0);

// Create a binary comparison
BinaryExpr comparison = new BinaryExpr(
    varX, zero, BinaryExpr.Operator.GREATER_THAN
);

// Create a method call
MethodCallExpr methodCall = new MethodCallExpr(
    null,  // scope (null for static/global)
    new SimpleName("increment"),
    Arrays.asList(varX)
);
```

## Requirements

- Java 8 or higher
- No external dependencies

## Notes

- **Grammar Update**: The codebase has been updated to use a JML-based grammar (`NewGrammar.java`) that mocks jml-parser structure
- All three versions produce similar but not identical output
- Version 3 includes additional `input()` statements for old variables
- The algorithm handles nested expressions and complex data structures
- Variable renaming ensures no naming conflicts between different function calls
- Post-state variables can be represented as:
  - `_post` suffix (e.g., `x_post` for `x'`)
  - `MethodCallExpr` with name `"'"` for JML-style notation
- The new grammar supports full Java-like expressions including object creation, field access, and method calls
- Compatibility classes (`SetExpr`, `MapExpr`, `TupleExpr`) are provided in `Helper.java` for backward compatibility
