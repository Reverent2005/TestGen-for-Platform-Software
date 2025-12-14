import java.util.*;

/**
 * Sample Example demonstrating the three versions of the test generation algorithm.
 * This example shows how to create specifications and run them through each version.
 */
public class SampleExample {
    
    public static void main(String[] args) {
        System.out.println("=== Test Generation Algorithm Sample Example ===\n");
        
        // Allow running specific demos via CLI argument
        if (args != null && args.length > 0) {
            String cmd = args[0].trim();
            if (cmd.equalsIgnoreCase("createComplexExample")) {
                createComplexExample();
                return;
            }
            // if (cmd.equalsIgnoreCase("showAdditionalExamples")) {
                // showAdditionalExamples();
                // return;
            // }
            if (cmd.equalsIgnoreCase("tripleExample")) {
                runTripleExample();
                return;
            }
            if (cmd.equalsIgnoreCase("v1") || cmd.equalsIgnoreCase("version1")) {
                SampleData sd = createSampleData();
                System.out.println("--- Testing Version 1 ---");
                // testVersion1(sd);
                return;
            }
            // if (cmd.equalsIgnoreCase("v2") || cmd.equalsIgnoreCase("version2")) {
            //     SampleData sd = createSampleData();
            //     System.out.println("--- Testing Version 2 ---");
            //     testVersion2(sd);
            //     return;
            // }
            // if (cmd.equalsIgnoreCase("v3") || cmd.equalsIgnoreCase("version3")) {
            //     SampleData sd = createSampleData();
            //     System.out.println("--- Testing Version 3 ---");
            //     testVersion3(sd);
            //     return;
            // }
        }
        
        // Default demo (Version 2 only, others commented by user)
        // Create sample data structures
        SampleData sampleData = createSampleData();
        
        // Test Version 1
        // System.out.println("--- Testing Version 1 ---");
        // testVersion1(sampleData);
        
        // Test Version 2
        System.out.println("\n--- Testing Version 1 ---");
        // testVersion1(sampleData);
        
        // Test Version 3
        // System.out.println("\n--- Testing Version 3 ---");
        // testVersion3(sampleData);
        
        // Show additional examples with different functions
        // System.out.println("\n--- Additional Function Examples ---");
        // showAdditionalExamples();
    }
    
    /**
     * Creates sample data for testing all three versions
     */
    private static SampleData createSampleData() {
        // Create symbol tables
        SymbolTable globalTable = new SymbolTable();
        globalTable.variables.add("x");
        globalTable.variables.add("y");
        globalTable.variables.add("z");
        
        // Create child symbol tables for each function
        SymbolTable child1 = new SymbolTable();
        child1.variables.add("x");
        child1.variables.add("y");
        
        SymbolTable child2 = new SymbolTable();
        child2.variables.add("y");
        child2.variables.add("z");
        
        globalTable.children.add(child1);
        globalTable.children.add(child2);
        
        // Create type map (using NewGrammar's Type classes)
        TypeMap typeMap = new TypeMap();
        typeMap.mapping.put("x", new PrimitiveType("int"));
        typeMap.mapping.put("y", new PrimitiveType("string"));
        typeMap.mapping.put("z", new PrimitiveType("bool"));
        
        // Create sample expressions using NewGrammar AST
        // Function 1: increment(x) - increments x by 1
        // Pre-condition: x > 0 (x must be positive)
        // Post-condition: x' > x (result is greater than original)
        Expr pre1 = new BinaryExpr(
            new NameExpr(new SimpleName("x")),
            new IntegerLiteralExpr(0),
            BinaryExpr.Operator.GREATER_THAN
        );
        MethodCallExpr call1 = new MethodCallExpr(
            null, // no scope for static/global function
            new SimpleName("increment"),
            Arrays.asList(new NameExpr(new SimpleName("x")))
        );
        // For post-condition: x' > x, we use a special naming convention
        // x' becomes x_post (or we can use a field access pattern)
        Expr post1 = new BinaryExpr(
            new NameExpr(new SimpleName("x_post")), // represents x'
            new NameExpr(new SimpleName("x")),
            BinaryExpr.Operator.GREATER_THAN
        );
        
        // Function 2: reverse(y) - reverses string y
        // Pre-condition: y != null (y is not empty/null)
        // Post-condition: y' == reverse(y) (result equals reverse of original)
        Expr pre2 = new BinaryExpr(
            new NameExpr(new SimpleName("y")),
            new NameExpr(new SimpleName("null")), // Using null check instead of 0
            BinaryExpr.Operator.NOT_EQUALS
        );
        MethodCallExpr call2 = new MethodCallExpr(
            null,
            new SimpleName("reverse"),
            Arrays.asList(new NameExpr(new SimpleName("y")))
        );
        // Post-condition: y' == reverse(y)
        Expr post2 = new BinaryExpr(
            new NameExpr(new SimpleName("y_post")), // represents y'
            new MethodCallExpr(
                null,
                new SimpleName("reverse"),
                Arrays.asList(new NameExpr(new SimpleName("y")))
            ),
            BinaryExpr.Operator.EQUALS
        );
        
        // Create function specifications
        FunctionSpec spec1 = new FunctionSpec(
            pre1,
            new FuncCallSpec(call1),
            new Response(post1)
        );
        
        FunctionSpec spec2 = new FunctionSpec(
            pre2,
            new FuncCallSpec(call2),
            new Response(post2)
        );
        
        // Create specification
        Specification spec = new Specification(Arrays.asList(spec1, spec2));
        
        return new SampleData(spec, globalTable, typeMap);
    }
    
    /**
     * Test Version 1 of the algorithm
     */
    private static void testVersion1(SampleData data) {
        try {
            System.out.println("Input Specification:");
            System.out.println("  Function 1: increment(x) - Pre: x > 0, Post: x' > x");
            System.out.println("  Function 2: reverse(y) - Pre: y != 0, Post: y' == reverse(y)");
            System.out.println(data.spec);
            
            Program result = Version1.convert(data.spec, data.symbolTable, data.typeMap);
            
            System.out.println("Generated Program (Version 1):");
            System.out.println(result);
            
            System.out.println("Final Type Map:");
            System.out.println(data.typeMap);
            
        } catch (Exception e) {
            System.err.println("Error in Version 1: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test Version 2 of the algorithm
     */
    // private static void testVersion2(SampleData data) {
    //     try {
    //         System.out.println("Input Specification:");
    //         System.out.println("  Function 1: increment(x) - Pre: x > 0, Post: x' > x");
    //         System.out.println("  Function 2: reverse(y) - Pre: y != 0, Post: y' == reverse(y)");
    //         System.out.println(data.spec);
            
    //         Program result = Version2.convertSpecification(data.spec, data.symbolTable, data.typeMap);
            
    //         System.out.println("Generated Program (Version 2):");
    //         System.out.println(result);
            
    //         System.out.println("Final Type Map:");
    //         System.out.println(data.typeMap);
            
    //     } catch (Exception e) {
    //         System.err.println("Error in Version 2: " + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }
    
    // /**
    //  * Test Version 3 of the algorithm
    //  */
    // private static void testVersion3(SampleData data) {
    //     try {
    //         System.out.println("Input Specification:");
    //         System.out.println("  Function 1: increment(x) - Pre: x > 0, Post: x' > x");
    //         System.out.println("  Function 2: reverse(y) - Pre: y != 0, Post: y' == reverse(y)");
    //         System.out.println(data.spec);
            
    //         Program result = Version3.convert(data.spec, data.symbolTable, data.typeMap);
            
    //         System.out.println("Generated Program (Version 3):");
    //         System.out.println(result);
            
    //         System.out.println("Final Type Map:");
    //         System.out.println(data.typeMap);
            
    //     } catch (Exception e) {
    //         System.err.println("Error in Version 3: " + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }
    
    /**
     * Helper class to hold sample data
     */
    private static class SampleData {
        final Specification spec;
        final SymbolTable symbolTable;
        final TypeMap typeMap;
        
        SampleData(Specification spec, SymbolTable symbolTable, TypeMap typeMap) {
            this.spec = spec;
            this.symbolTable = symbolTable;
            this.typeMap = typeMap;
        }
    }

    /**
     * Additional helper method to create more complex examples
     */
    public static void createComplexExample() {
        System.out.println("\n=== Complex Example with Sets and Maps ===");
        
        // Create a more complex specification with sets and maps
        SymbolTable complexTable = new SymbolTable();
        complexTable.variables.add("data");
        complexTable.variables.add("result");
        
        SymbolTable child = new SymbolTable();
        child.variables.add("data");
        child.variables.add("result");
        complexTable.children.add(child);
        
        TypeMap complexTypeMap = new TypeMap();
        complexTypeMap.mapping.put("data", new ClassOrInterfaceType(new SimpleName("Set")));
        complexTypeMap.mapping.put("result", new ClassOrInterfaceType(new SimpleName("Map")));
        
        // Create complex expressions using NewGrammar AST
        // Function 3: process(data, result) - processes a set of data and updates result map
        // Pre-condition: contains({1,2,3}, 2) (set contains the value 2)
        // Post-condition: result' == update(result, data) (result is updated with data)
        // Note: For sets/maps, we'll use method calls since NewGrammar doesn't have SetExpr/MapExpr
        // We'll create them as object creation or method calls
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
        
        Expr pre = containsCall;
        MethodCallExpr call = new MethodCallExpr(
            null,
            new SimpleName("process"),
            Arrays.asList(
                new NameExpr(new SimpleName("data")),
                new NameExpr(new SimpleName("result"))
            )
        );
        Expr post = new BinaryExpr(
            new NameExpr(new SimpleName("result_post")), // represents result'
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
        
        FunctionSpec complexSpec = new FunctionSpec(
            pre,
            new FuncCallSpec(call),
            new Response(post)
        );
        
        Specification spec = new Specification(Arrays.asList(complexSpec));
        
        System.out.println("Input Specification:");
        System.out.println("  Function: process(data, result)");
        System.out.println("  Pre: contains({1, 2, 3}, 2)");
        System.out.println("  Post: result' == update(result, data)");
        
        // System.out.println("Complex Specification:");
        System.out.println(spec);
        
        // Test with Version 2 (most complete)
        // try {
        //     Program result = Version1.convert(spec, complexTable, complexTypeMap);
        //     System.out.println("Generated Complex Program:");
        //     System.out.println(result);
        // } catch (Exception e) {
        //     System.err.println("Error in complex example: " + e.getMessage());
        //     e.printStackTrace();
        // }
    }

    /**
     * Triple example: one spec with three blocks calling increment, process, reverse
     */
    public static void runTripleExample() {
        System.out.println("\n=== Triple Example: increment, process, reverse ===");

        // Global symbol table
        SymbolTable global = new SymbolTable();
        global.variables.add("x");
        global.variables.add("y");
        global.variables.add("data");
        global.variables.add("result");

        // Child tables for each block
        SymbolTable child0 = new SymbolTable(); // increment(x)
        child0.variables.add("x");

        SymbolTable child1 = new SymbolTable(); // process(data, result)
        child1.variables.add("data");
        child1.variables.add("result");

        SymbolTable child2 = new SymbolTable(); // reverse(y)
        child2.variables.add("y");

        global.children.add(child0);
        global.children.add(child1);
        global.children.add(child2);

        // Types (using NewGrammar's Type classes)
        TypeMap tm = new TypeMap();
        tm.mapping.put("x", new PrimitiveType("int"));
        tm.mapping.put("y", new PrimitiveType("string"));
        tm.mapping.put("data", new ClassOrInterfaceType(new SimpleName("Set")));
        tm.mapping.put("result", new ClassOrInterfaceType(new SimpleName("Map")));
        
        // Block 0: increment(x)
        Expr pre0 = new BinaryExpr(
            new NameExpr(new SimpleName("x")),
            new IntegerLiteralExpr(0),
            BinaryExpr.Operator.GREATER_THAN
        );
        MethodCallExpr call0 = new MethodCallExpr(
            null,
            new SimpleName("increment"),
            Arrays.asList(new NameExpr(new SimpleName("x")))
        );
        Expr post0 = new BinaryExpr(
            new NameExpr(new SimpleName("x_post")),
            new NameExpr(new SimpleName("x")),
            BinaryExpr.Operator.GREATER_THAN
        );
        FunctionSpec fs0 = new FunctionSpec(pre0, new FuncCallSpec(call0), new Response(post0));
        
        // Block 1: process(data, result)
        MethodCallExpr containsCall1 = new MethodCallExpr(
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
        Expr pre1 = containsCall1;
        MethodCallExpr call1 = new MethodCallExpr(
            null,
            new SimpleName("process"),
            Arrays.asList(
                new NameExpr(new SimpleName("data")),
                new NameExpr(new SimpleName("result"))
            )
        );
        Expr post1 = new BinaryExpr(
            new NameExpr(new SimpleName("result_post")),
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
        FunctionSpec fs1 = new FunctionSpec(pre1, new FuncCallSpec(call1), new Response(post1));
        
        // Block 2: reverse(y)
        Expr pre2 = new BinaryExpr(
            new NameExpr(new SimpleName("y")),
            new NameExpr(new SimpleName("null")),
            BinaryExpr.Operator.NOT_EQUALS
        );
        MethodCallExpr call2 = new MethodCallExpr(
            null,
            new SimpleName("reverse"),
            Arrays.asList(new NameExpr(new SimpleName("y")))
        );
        Expr post2 = new BinaryExpr(
            new NameExpr(new SimpleName("y_post")),
            new MethodCallExpr(
                null,
                new SimpleName("reverse"),
                Arrays.asList(new NameExpr(new SimpleName("y")))
            ),
            BinaryExpr.Operator.EQUALS
        );
        FunctionSpec fs2 = new FunctionSpec(pre2, new FuncCallSpec(call2), new Response(post2));

        // Build specification
        Specification spec = new Specification(Arrays.asList(fs0, fs1, fs2));

        // Print input spec summary
        System.out.println("Input Specification:");
        System.out.println("  1) increment(x)  Pre: x > 0    Post: x' > x");
        System.out.println("  2) process(data,result)  Pre: contains({1,2,3},2)  Post: result' == update(result,data)");
        System.out.println("  3) reverse(y)    Pre: y != 0  Post: y' == reverse(y)");
        
        // System.out.println("Full Specification:");
        System.out.println(spec);

        // try {
        //     Program result = Version1.convert(spec, global, tm);
        //     System.out.println("Generated Triple Program:");
        //     System.out.println(result);
        // } catch (Exception e) {
        //     System.err.println("Error in triple example: " + e.getMessage());
        //     e.printStackTrace();
        // }
    }
}
