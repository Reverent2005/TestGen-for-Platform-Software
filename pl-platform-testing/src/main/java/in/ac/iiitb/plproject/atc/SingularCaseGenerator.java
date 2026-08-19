package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
import in.ac.iiitb.plproject.parser.ast.FunctionSignature;
import in.ac.iiitb.plproject.parser.ast.JmlFunctionSpec;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.parser.ast.Variable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emits a SINGULAR CASE: one runnable Java file that executes a test string once,
 * with concrete inputs, and checks every precondition before its call and every
 * postcondition after it.
 *
 * <p>This is the third artifact generated from a test string, and it answers a
 * different question from the other two:
 *
 * <ul>
 *   <li>{@code GeneratedATCs.java} asks the solver "for which inputs can this
 *       sequence go wrong?"</li>
 *   <li>{@code GeneratedATCs_JUnit.java} is the harness that binds SERVER_OUTPUT
 *       values dynamically at runtime</li>
 *   <li>{@code SingularCase.java} asks "does THIS one concrete run satisfy the
 *       spec at every step?" — and says exactly which condition failed if not</li>
 * </ul>
 *
 * <p>The shape is deliberately flat: no helper methods, one braced scope per block
 * of the test string, in order.  That keeps each block's locals to itself (so two
 * {@code push} blocks can each have their own {@code elem} and {@code size_old})
 * while SERVER_OUTPUT captures are declared outside their block so later blocks
 * can consume them.
 *
 * <p><b>Library contract:</b> the case begins with {@code Helper.reset()} so the run
 * starts from the initial state the spec's {@code state} block describes.  A
 * library used this way must offer that method.
 */
public class SingularCaseGenerator {

    /** Class name of the generated singular case. */
    public static final String GENERATED_CLASS = "SingularCase";

    private static final String INDENT = "    ";

    private final StringBuilder out = new StringBuilder();

    public String generate(JmlSpecAst specAst, TestStringAst testString, PropagationScan scan,
                           String packageName) {
        Set<String> stateVars = new LinkedHashSet<String>(specAst.getStateVars().keySet());
        List<String> calls = testString.getCalls();

        header(packageName, testString, calls);

        // SERVER_OUTPUT captures outlive their block, so they are declared first.
        Map<String, String> localNameFor = declareCaptures(scan);

        out.append(INDENT).append(INDENT).append("try {\n");
        for (int i = 0; i < calls.size(); i++) {
            emitBlock(i, calls.get(i), specAst, testString, scan, stateVars, localNameFor);
        }
        out.append(INDENT).append(INDENT).append("} catch (PreconditionViolated stop) {\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("// Past a broken precondition the spec says nothing about the library's\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("// behaviour, so there is nothing meaningful left to check.\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("System.out.println();\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("System.out.println(\"run stopped at \" + stop.getMessage()\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("        + \" — the test string calls it outside its contract\");\n");
        out.append(INDENT).append(INDENT).append("}\n\n");

        footer();
        return out.toString();
    }

    private void header(String packageName, TestStringAst testString, List<String> calls) {
        String name = testString.getName() != null ? testString.getName() : "unnamed";
        String sequence = String.join(" -> ", calls);

        out.append("package ").append(packageName).append(";\n\n");
        out.append("/**\n");
        out.append(" * SINGULAR CASE — generated, do not edit.\n");
        out.append(" *\n");
        out.append(" * Runs the test string once with concrete inputs and checks every\n");
        out.append(" * precondition before its call and every postcondition after it.\n");
        out.append(" *\n");
        out.append(" *   test string: ").append(sequence).append("\n");
        out.append(" *\n");
        out.append(" * Exits 0 when every condition holds, 1 otherwise.\n");
        out.append(" */\n");
        out.append("public class ").append(GENERATED_CLASS).append(" {\n\n");
        out.append(INDENT).append("private static int checks = 0;\n");
        out.append(INDENT).append("private static int failures = 0;\n\n");
        out.append(INDENT).append("public static void main(String[] args) {\n");
        out.append(INDENT).append(INDENT).append("System.out.println(\"singular case : ")
           .append(escape(name)).append("\");\n");
        out.append(INDENT).append(INDENT).append("System.out.println(\"test string   : ")
           .append(escape(sequence)).append("\");\n");
        out.append(INDENT).append(INDENT).append("System.out.println();\n\n");
        out.append(INDENT).append(INDENT)
           .append("Helper.reset(); // start from the initial state the spec declares\n\n");
    }

    /**
     * Declares one local per SERVER_OUTPUT the sequence produces.  A value produced
     * by two different blocks needs one local each ({@code oldVal_0}, {@code oldVal_1});
     * a value produced once keeps its plain spec name.
     */
    private Map<String, String> declareCaptures(PropagationScan scan) {
        Map<String, Integer> productionCount = new LinkedHashMap<String, Integer>();
        for (PropagationScan.Step step : scan.getSteps()) {
            if (step.getProducedVar() != null) {
                Integer seen = productionCount.get(step.getProducedVar());
                productionCount.put(step.getProducedVar(), seen == null ? 1 : seen + 1);
            }
        }

        Map<String, String> localNameFor = new LinkedHashMap<String, String>();
        boolean any = false;
        for (PropagationScan.Step step : scan.getSteps()) {
            String produced = step.getProducedVar();
            if (produced == null) continue;

            String local = productionCount.get(produced) > 1
                    ? produced + "_" + step.getIndex()
                    : produced;
            localNameFor.put(produced + "@" + step.getIndex(), local);

            if (!any) {
                out.append(INDENT).append(INDENT)
                   .append("// SERVER_OUTPUT captures — declared here so later blocks can consume them\n");
                any = true;
            }
            out.append(INDENT).append(INDENT)
               .append(scan.typeOf(produced)).append(" ").append(local).append(";\n");
        }
        if (any) out.append("\n");
        return localNameFor;
    }

    private void emitBlock(int index, String functionName, JmlSpecAst specAst,
                           TestStringAst testString, PropagationScan scan,
                           Set<String> stateVars, Map<String, String> localNameFor) {
        JmlFunctionSpec spec = specAst.findSpecFor(functionName);
        FunctionSignature signature = spec.getSignature();
        List<Variable> params = (signature != null && signature.getParameters() != null)
                ? signature.getParameters() : new ArrayList<Variable>();
        Set<String> propagated = scan.getSteps().get(index).getPropagatedParams();
        Map<String, ConcreteInput> bound = testString.inputsForBlock(index);

        String pad = INDENT + INDENT + INDENT;
        String body = pad + INDENT;

        out.append(pad).append("{   // ── block ").append(index).append(": ")
           .append(functionName).append(" ")
           .append(dashes(52 - functionName.length())).append("\n");

        // CLIENT_INPUTs get their concrete value; propagated values are already in scope.
        List<String> callArgs = new ArrayList<String>();
        for (Variable param : params) {
            if (propagated.contains(param.getName())) {
                String upstream = mostRecentLocalFor(param.getName(), index, scan, localNameFor);
                callArgs.add(param.getName());
                // Only alias when the upstream local goes by a different name; a value
                // produced once already stands in scope under the parameter's own name.
                if (!upstream.equals(param.getName())) {
                    out.append(body).append(param.getTypeName()).append(" ").append(param.getName())
                       .append(" = ").append(upstream)
                       .append("; // SERVER_OUTPUT propagated from ")
                       .append(scan.producerOf(param.getName())).append("\n");
                } else {
                    out.append(body).append("// ").append(param.getName())
                       .append(" is the SERVER_OUTPUT captured by ")
                       .append(scan.producerOf(param.getName()))
                       .append(", propagated into this block\n");
                }
            } else {
                ConcreteInput input = bound.get(param.getName());
                callArgs.add(param.getName());
                out.append(body).append(param.getTypeName()).append(" ").append(param.getName())
                   .append(" = ").append(input.getLiteral()).append("; // CLIENT_INPUT\n");
            }
        }

        // Precondition, checked before the call.
        Expr pre = spec.getPrecondition();
        if (pre != null) {
            String code = AstHelper.exprToJavaCode(AstHelper.rewriteLibraryExpr(pre, null, stateVars));
            out.append(body).append("require(").append(index).append(", \"").append(functionName)
               .append("\", \"").append(escape(specText(pre))).append("\", ")
               .append(code).append(");\n");
        }

        // Old-state snapshots, taken after the precondition and before the call.
        Expr post = spec.getPostcondition();
        for (String stateVar : AstHelper.collectOldVarNames(post)) {
            String declaredType = specAst.getStateVarType(stateVar);
            if (declaredType == null) declaredType = "Object";
            out.append(body).append(declaredType).append(" ").append(stateVar).append("_old = ")
               .append(NewGenATC.snapshotExpression(declaredType, stateVar)).append(";\n");
        }

        // The call itself.
        String resultVar = spec.getResultBinding();
        String call = "Helper." + signature.getName() + "(" + String.join(", ", callArgs) + ")";
        if (resultVar != null) {
            String local = localNameFor.get(resultVar + "@" + index);
            out.append(body).append(local).append(" = ").append(call)
               .append("; // SERVER_OUTPUT captured\n");
            if (!local.equals(resultVar)) {
                out.append(body).append(scan.typeOf(resultVar)).append(" ").append(resultVar)
                   .append(" = ").append(local).append("; // the name the postcondition uses\n");
            }
        } else {
            out.append(body).append(call).append(";\n");
        }

        // Postcondition, checked after the call.  The `\result == <bound>` conjunct
        // is the capture above, so it is not re-checked here.
        for (Expr conjunct : AstHelper.splitConjuncts(post)) {
            if (AstHelper.isResultBindingConjunct(conjunct, resultVar)) continue;
            String code = AstHelper.exprToJavaCode(
                    AstHelper.rewriteLibraryExpr(conjunct, resultVar, stateVars));
            out.append(body).append("ensure(").append(index).append(", \"").append(functionName)
               .append("\", \"").append(escape(specText(conjunct))).append("\", ")
               .append(code).append(");\n");
        }

        out.append(pad).append("}\n\n");
    }

    /** The local holding the most recent value of {@code varName} at this point. */
    private String mostRecentLocalFor(String varName, int blockIndex, PropagationScan scan,
                                      Map<String, String> localNameFor) {
        String local = varName;
        for (PropagationScan.Step step : scan.getSteps()) {
            if (step.getIndex() >= blockIndex) break;
            if (varName.equals(step.getProducedVar())) {
                local = localNameFor.get(varName + "@" + step.getIndex());
            }
        }
        return local;
    }

    private void footer() {
        out.append(INDENT).append(INDENT).append("System.exit(report());\n");
        out.append(INDENT).append("}\n\n");

        out.append(INDENT).append("/** Raised when a precondition does not hold, to stop the run. */\n");
        out.append(INDENT).append("private static class PreconditionViolated extends RuntimeException {\n");
        out.append(INDENT).append(INDENT).append("PreconditionViolated(String where) { super(where); }\n");
        out.append(INDENT).append("}\n\n");

        out.append(INDENT).append("/**\n");
        out.append(INDENT).append("  * A precondition, checked immediately before its call.\n");
        out.append(INDENT).append("  *\n");
        out.append(INDENT).append("  * A failure stops the run: calling the library outside its contract would\n");
        out.append(INDENT).append("  * measure behaviour the spec never promised, so nothing after it is evidence\n");
        out.append(INDENT).append("  * of anything.  Postcondition failures do NOT stop the run — each one is a\n");
        out.append(INDENT).append("  * real finding, and reporting them all is more useful than reporting one.\n");
        out.append(INDENT).append("  */\n");
        out.append(INDENT).append("private static void require(int block, String function, String condition, boolean holds) {\n");
        out.append(INDENT).append(INDENT).append("check(block, function, \"PRE \", condition, holds);\n");
        out.append(INDENT).append(INDENT).append("if (!holds) {\n");
        out.append(INDENT).append(INDENT).append(INDENT)
           .append("throw new PreconditionViolated(\"block \" + block + \" (\" + function + \")\");\n");
        out.append(INDENT).append(INDENT).append("}\n");
        out.append(INDENT).append("}\n\n");

        out.append(INDENT).append("/** A postcondition, checked immediately after its call. */\n");
        out.append(INDENT).append("private static void ensure(int block, String function, String condition, boolean holds) {\n");
        out.append(INDENT).append(INDENT).append("check(block, function, \"POST\", condition, holds);\n");
        out.append(INDENT).append("}\n\n");

        out.append(INDENT).append("private static void check(int block, String function, String kind,\n");
        out.append(INDENT).append("                          String condition, boolean holds) {\n");
        out.append(INDENT).append(INDENT).append("checks++;\n");
        out.append(INDENT).append(INDENT).append("if (!holds) failures++;\n");
        out.append(INDENT).append(INDENT).append("System.out.println(String.format(\"block %d  %-12s %s  %-4s  %s\",\n");
        out.append(INDENT).append(INDENT).append("        block, function, kind, holds ? \"ok\" : \"FAIL\", condition));\n");
        out.append(INDENT).append("}\n\n");

        out.append(INDENT).append("private static int report() {\n");
        out.append(INDENT).append(INDENT).append("System.out.println();\n");
        out.append(INDENT).append(INDENT).append("if (failures == 0) {\n");
        out.append(INDENT).append(INDENT).append(INDENT).append("System.out.println(checks\n");
        out.append(INDENT).append(INDENT).append(INDENT).append("        + \" condition(s) checked, all hold —\"\n");
        out.append(INDENT).append(INDENT).append(INDENT).append("        + \" the test string runs and meets its pre/postconditions\");\n");
        out.append(INDENT).append(INDENT).append(INDENT).append("return 0;\n");
        out.append(INDENT).append(INDENT).append("}\n");
        out.append(INDENT).append(INDENT).append("System.out.println(failures + \" of \" + checks + \" condition(s) FAILED\");\n");
        out.append(INDENT).append(INDENT).append("return 1;\n");
        out.append(INDENT).append("}\n");
        out.append("}\n");
    }

    /** The condition as the spec writes it, for the report line. */
    private String specText(Expr expr) {
        return AstHelper.exprToSpecSource(expr);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String dashes(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(3, count); i++) sb.append('─');
        return sb.toString();
    }
}
