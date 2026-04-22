// Placeholder for Main.java

package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.parser.*;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    
    public static void main(String[] args) throws Exception {
        // ─────────────────────────────────────────────────────
        // PIPELINE START: Load .spec file
        // ─────────────────────────────────────────────────────
        
        String specFile = "../jml_parser/examples/MathUtils.spec";
        System.out.println("Loading spec file: " + specFile);
        String specText = Files.readString(Paths.get(specFile));
        
        // ─────────────────────────────────────────────────────
        // PIPELINE STEP 1: Parse text → AST
        // ─────────────────────────────────────────────────────
        
        System.out.println("\n[STEP 1] Converting spec text to AST...");
        JmlSpecAst ast = SpecToAstConverter.convertSpecToAst(specText);
        System.out.println(ast);
        
        // ─────────────────────────────────────────────────────
        // PIPELINE STEP 2: You can now use the AST for:
        // ─────────────────────────────────────────────────────
        // - Symbolic execution
        // - Test generation
        // - Constraint solving
        // - Code generation
        // ─────────────────────────────────────────────────────
        
        System.out.println("\n[STEP 2] AST ready for downstream processing!");
        System.out.println("Next: Feed this to symbolic executor / test generator");
    }
}