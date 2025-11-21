package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.symex.SpfWrapper;
import in.ac.iiitb.plproject.atc.ir.AtcClass;
import java.util.List;

// Skeleton for TestGen class (based on img2.jpeg)
public class TestGen {

    private GenATC genAtcAlgorithm;
    private SpfWrapper symexWrapper;

    // The concrete implementation (e.g., LibTestGenATC) is injected
    public TestGen(GenATC genAtcAlgorithm, SpfWrapper symexWrapper) {
        this.genAtcAlgorithm = genAtcAlgorithm;
        this.symexWrapper = symexWrapper;
    }

    /**
     * Main method to generate and run tests.
     * @param jmlSpecAst The parsed AST of all JML specifications.
     * @param testStringAst The parsed AST of the test sequence (e.g., ["PUSH_OK", "POP_OK"]).
     * @return Concrete test inputs from SPF.
     */
    public List<ConcreteInput> generateTest(JmlSpecAst jmlSpecAst, TestStringAst testStringAst) {
        
        // 1. Generate the ATC IR structure
        AtcClass atcClass = this.genAtcAlgorithm.generateAtcFile(jmlSpecAst, testStringAst);
        
        // 2. Convert IR to JavaFile string for SPF using the prettyPrint method
        String javaCode = this.genAtcAlgorithm.prettyPrint(atcClass);
        JavaFile atcJavaFile = new JavaFile(javaCode);
        
        // 3. Run symbolic execution on that file
        // This calls the SPF wrapper, passing both the JavaFile and AtcClass for file generation
        List<ConcreteInput> testInputs = this.symexWrapper.run(atcJavaFile, atcClass);
        // 4. Plug back/return results
        return testInputs;
    }
}
