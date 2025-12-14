# NewGenATC Algorithm - Setup and Testing Guide

This document explains how to use the NewGenATC algorithm setup for incremental development and testing.

## Architecture

The algorithm follows the GenATC interface pattern (matching the existing architecture):

1. **GenATC Interface** (`in.ac.iiitb.plproject.atc` package):
   - Defines `generateAtcFile(JmlSpecAst, TestStringAst) -> JavaFile`
   - Generates Java test code as strings (not AST Programs)
   - Used by `TestGen` class to generate and run tests

2. **NewGenATC Implementation** (`in.ac.iiitb.plproject.atc` package):
   - `NewGenATC.java` - Your algorithm implementation
   - Implements `GenATC` interface
   - Uses AST classes internally for expression manipulation
   - Generates Java code strings (JavaFile)
   - Main method: `generateTestFunction(JmlFunctionSpec)` - implement this!

3. **AST Classes** (`in.ac.iiitb.plproject.ast` package):
   - `NewGrammar.java` - JML-based AST classes (Expr, Stmt, Type, etc.) - package-private
   - `Type.java` - Abstract Type class - package-private
   - `Helper.java` - Utility classes (for internal use if needed) - package-private
   - `AstHelper.java` - **Public helper class** for working with AST expressions from other packages
     - Factory methods: `createBinaryExpr()`, `createMethodCallExpr()`, etc.
     - Utility methods: `getNameFromExpr()`, `transformPostCondition()`, etc.

4. **JML Parser AST** (`in.ac.iiitb.plproject.parser.ast` package):
   - `JmlSpecAst` - Collection of JML function specs
   - `JmlFunctionSpec` - Individual JML spec with pre/post conditions (uses Object for Expr)
   - `TestStringAst` - Sequence of function calls to test
   - `FunctionSignature` - Function signature (name, params, return type)
   - `Variable` - Variable with name and type

5. **Testing** (`in.ac.iiitb.plproject.atc` package):
   - `IncrementalTestExample.java` - Test cases for incremental testing
   - Uses `AstHelper` to create expressions (since AST classes are package-private)

## How to Use

### Quick Start with Maven (Recommended)

**Note:** If Maven is installed in WSL, use `wsl mvn` instead of `mvn` in Windows.

**Troubleshooting:** If you get `NoClassDefFoundError: com/google/inject/Module` errors, your Maven installation may be corrupted. See the "Fixing Maven Issues" section below, or use the javac scripts instead.

From the `pl-platform-testing` directory:

**1. Compile the Code:**
```bash
# In WSL or Linux
mvn compile

# In Windows (if Maven is in WSL)
wsl mvn compile
```

**2. Run Tests:**
```bash
# Run simple test case (default)
mvn exec:java
# Or in Windows: wsl mvn exec:java

# Run specific test case
mvn exec:java -Dexec.args="simple"
mvn exec:java -Dexec.args="complex"
mvn exec:java -Dexec.args="all"
# Or in Windows: wsl mvn exec:java -Dexec.args="simple"
```

**3. Compile and Run in One Command:**
```bash
mvn compile exec:java -Dexec.args="simple"
# Or in Windows: wsl mvn compile exec:java -Dexec.args="simple"
```

### Quick Start with javac (Alternative - Works when Maven has issues)

If Maven is not working, you can use the direct javac compilation scripts:

**In WSL:**
```bash
# Compile
bash scripts/compile.sh

# Run tests
bash scripts/run-tests.sh simple
bash scripts/run-tests.sh complex
bash scripts/run-tests.sh all
```

**In Windows:**
```batch
REM Compile
scripts\compile-javac.bat

REM Run tests
scripts\run-tests-javac.bat simple
scripts\run-tests-javac.bat complex
scripts\run-tests-javac.bat all
```

### Fixing Maven Issues

If you encounter `NoClassDefFoundError: com/google/inject/Module` errors:

**Option 1: Reinstall Maven (Recommended)**
```bash
# In WSL (as root)
sudo apt-get update
sudo apt-get install --reinstall maven
```

**Option 2: Use the fix script**
```bash
# In WSL (as root)
sudo bash scripts/fix-maven.sh
```

**Option 3: Use javac scripts instead**
The javac scripts work without Maven and are a reliable fallback.

### Alternative: Batch Scripts (Windows)

If you prefer using batch scripts, they are available in the `scripts/` folder:

```batch
REM Run simple test case (default)
scripts\run-tests.bat

REM Run specific test case
scripts\run-tests.bat simple
scripts\run-tests.bat complex
scripts\run-tests.bat all

REM Or use the convenience script for all tests
scripts\run-all-tests.bat

REM Compile only (without running)
scripts\compile.bat
```

**Note:** These scripts can be run from anywhere - they automatically change to the project root directory.

### Manual Compilation and Execution (Without Maven)

From the `pl-platform-testing` directory:

**1. Compile the Code:**

```bash
# Compile all source files
javac -d target/classes -sourcepath src/main/java \
  src/main/java/in/ac/iiitb/plproject/ast/*.java \
  src/main/java/in/ac/iiitb/plproject/parser/ast/*.java \
  src/main/java/in/ac/iiitb/plproject/atc/*.java
```

**2. Run Tests:**

```bash
# Run simple test case
java -cp target/classes in.ac.iiitb.plproject.atc.IncrementalTestExample simple

# Run complex test case
java -cp target/classes in.ac.iiitb.plproject.atc.IncrementalTestExample complex

# Run all test cases
java -cp target/classes in.ac.iiitb.plproject.atc.IncrementalTestExample all
```

### 3. Incremental Development

1. **Start with `generateTestFunction()`**: The main algorithm is in `NewGenATC.generateTestFunction()`
   - This method takes a `JmlFunctionSpec` and returns Java code as a string
   - Access pre/post conditions: `spec.getPrecondition<Expr>()`, `spec.getPostcondition<Expr>()`
   - Use `AstHelper` methods to work with AST expressions:
     - `AstHelper.createBinaryExpr()` - Create binary expressions
     - `AstHelper.createMethodCallExpr()` - Create method calls
     - `AstHelper.getNameFromExpr()` - Extract variable names
     - `AstHelper.exprToJavaCode()` - Convert to Java code
   - Then convert them to Java code strings

2. **Implement helper methods**: As you need them:
   - `exprToJavaCode()` - Convert AST Expr to Java code string (use `AstHelper.exprToJavaCode()`)
   - `transformPostCondition()` - Transform post-state expressions (use `AstHelper.transformPostCondition()`)
   - `collectPostStateVariables()` - Collect variables needing old state (use `AstHelper.collectPostStateVariables()`)
   - Or implement directly in `AstHelper.java` if you need package-private access

3. **Test incrementally**: After implementing each part, run the test cases:
   ```bash
   # Using Maven (recommended)
   mvn compile exec:java -Dexec.args="simple"
   
   # Or using batch script
   run-tests.bat simple
   
   # Or manually
   java -cp target/classes in.ac.iiitb.plproject.atc.IncrementalTestExample simple
   ```

4. **Add more test cases**: You can add more test cases to `IncrementalTestExample.java`
   - Use `AstHelper` factory methods to create expressions
   - Create mock `JmlFunctionSpec` objects with pre/post conditions

## Algorithm Steps

The algorithm should:

1. **Extract function signature** from `JmlFunctionSpec.getSignature()`
   - Get parameters and their types
   - Create symbolic input variables (e.g., `Debug.makeSymbolicInt("x")`)

2. **Translate preconditions** from `JmlFunctionSpec.getPrecondition()`
   - Convert AST Expr to Java code
   - Generate `Debug.assume(...)` statements

3. **Snapshot old state** for postconditions
   - Identify variables used in postconditions
   - Save old values (e.g., `int old_x = x;`)

4. **Generate the method call**
   - Based on function signature
   - Use symbolic variables

5. **Translate postconditions** from `JmlFunctionSpec.getPostcondition()`
   - Transform post-state variables (x' -> result, x -> old_x)
   - Convert to Java assertions
   - Generate `assert(...)` statements

## Example Test Cases

### Simple Example
- **Function**: `increment(x: int)`
- **Pre-condition**: `x > 0`
- **Post-condition**: `x' > x`
- **Test String**: `[increment]`

### Complex Example
- **Function**: `process(data: Set, result: Map)`
- **Pre-condition**: `new Set([1,2,3]).contains(2)`
- **Post-condition**: `result' == update(result, data)`
- **Test String**: `[process]`

## Notes

- **Architecture**: Follows the GenATC interface pattern - no standalone Version4 class
- The algorithm generates **Java code strings** (JavaFile), not AST Programs
- AST classes are package-private - use `AstHelper` public methods to work with them
- Post-state variables are represented as `_post` suffix (e.g., `x_post` for `x'`)
- The output is a complete Java test class with `@Test` methods
- Each function in the test string generates one test method
- `JmlFunctionSpec` uses `Object` for pre/post conditions (cast to `Expr` when needed)
- Use `AstHelper` factory methods to create expressions from other packages
