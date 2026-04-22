package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.parser.SpecToAstConverter;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.atc.GenATC;
import in.ac.iiitb.plproject.atc.NewGenATC;
import in.ac.iiitb.plproject.atc.ir.AtcClass;
import in.ac.iiitb.plproject.atc.ir.AtcIrCodeGenerator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println(">>> MY SPEC PIPELINE MAIN <<<");
        String specFile = "../jml_parser/examples/MathUtils.spec";
        if (args.length > 0) specFile = args[0];

        System.out.println("Loading spec file: " + specFile);
        String specText = Files.readString(Paths.get(specFile));

        System.out.println("\n[STEP 1] Converting spec text to AST...");
        JmlSpecAst ast = SpecToAstConverter.convertSpecToAst(specText);
        System.out.println(ast);

        System.out.println("\n[STEP 2] Building test string (sequence)...");
        // Replace these names with the functions you want in the test string
        TestStringAst testString = new TestStringAst(Arrays.asList("sqrt", "divide", "abs"));

        System.out.println("\n[STEP 3] Generating ATC (symbolic helpers)...");
        GenATC gen = new NewGenATC();
        AtcClass atc = gen.generateAtcFile(ast, testString);
        System.out.println("Parsed AST from file:\n" + ast);

        System.out.println("\n[STEP 4] Emitting Java for generated ATCs...");
        AtcIrCodeGenerator codeGen = new AtcIrCodeGenerator();
        String javaCode = codeGen.generateJavaFile(atc);
        System.out.println(javaCode);

        String outPath = "outputs/GeneratedATCs.java";
        Files.createDirectories(Paths.get("outputs"));
        Files.write(Paths.get(outPath), javaCode.getBytes());
        System.out.println("\n✓ Generated test file: " + outPath);
    }
}