package in.ac.iiitb.plproject.mutation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a whole mutation suite for a library by applying every operator
 * {@link MutationOperator} declares to every line of its source that admits one.
 *
 * <h3>Why generate them</h3>
 *
 * The sets in {@code mutations/} are hand-written, and that is their value: each
 * one is a fault somebody chose, with a declared expectation and, for a survivor,
 * a written reason.  It is also their limit.  Ten hand-picked faults per library
 * measure the specification against ten guesses about how it might be wrong, and
 * the guesses come from the same person who wrote the specification.
 *
 * <p>A generated suite has the opposite shape.  It chooses nothing: it walks the
 * source and rewrites whatever each operator matches, several hundred times per
 * library, including the rewrites nobody would think to make.  The two answer
 * different questions — "does the suite catch the faults we thought of?" and
 * "what fraction of all the faults of this KIND does it catch?" — and the second
 * is the one a per-operator score is really about.
 *
 * <h3>What it does not do</h3>
 *
 * This is a source-to-source generator working on lines and regular expressions,
 * not on a parsed syntax tree.  That is a deliberate match to the rest of the
 * project — a {@code .mutants} file is a line-oriented format, and
 * {@link Mutant#applyTo} matches a line — and it has consequences a reader should
 * know about:
 *
 * <ul>
 *   <li>an operator inside a string literal or a comment is skipped by
 *       {@link #isMutableCode}, but only by the rules written there;</li>
 *   <li>a line matched by two operators yields two mutants, which is intended;</li>
 *   <li>nothing checks that a mutant is not <em>equivalent</em> to the original.
 *       Equivalent mutants cannot be killed by anyone, so they depress the score
 *       by an amount nobody can measure without reading them. This is the
 *       well-known limitation of generated mutation testing, and the reason the
 *       generated score is reported beside the hand-written one rather than
 *       instead of it.</li>
 * </ul>
 *
 * <p>The output is an ordinary {@code .mutants} file, so a generated suite is read
 * by the same parser, seeded by the same {@link Mutant#applyTo} and run by the
 * same {@link MutationRunner} as a hand-written one.
 */
public class OperatorMutantGenerator {

    /** One rewrite an operator wants to make to one line. */
    private static class Rewrite {
        final String operator;
        final String replacement;
        final String what;

        Rewrite(String operator, String replacement, String what) {
            this.operator = operator;
            this.replacement = replacement;
            this.what = what;
        }
    }

    private static final Pattern METHOD_HEADER = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+|static\\s+|final\\s+)*"
          + "([\\w<>\\[\\],.]+)\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{\\s*$");

    /** Declared types, so a mutant never negates a String or adds one to a null. */
    private static final Pattern DECLARATION = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+|static\\s+|final\\s+)*"
          + "([\\w.]+(?:<[^;=]*>)?(?:\\[\\])?)\\s+(\\w+)\\s*=[^=]");

    private static final Set<String> NUMERIC_TYPES = new java.util.HashSet<String>(
            java.util.Arrays.asList("int", "long", "short", "byte", "double", "float",
                                    "Integer", "Long", "Short", "Byte", "Double", "Float"));

    private static final Pattern RETURN = Pattern.compile("^(\\s*)return\\s+([^;]+);\\s*$");

    /** A static int field of the class, for the one operator that has to write somewhere. */
    private static final Pattern INT_FIELD = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+)?static\\s+int\\s+(\\w+)\\s*=");

    /** Everything the generator knows about the file it is walking. */
    private static class FileContext {
        final String intField;
        final java.util.Map<String, String> declaredTypes;
        /**
         * The subset of {@link #declaredTypes} declared at class level.
         *
         * <p>Only these can be swapped for one another by {@code variable-replacement}:
         * a local is in scope in one method and nowhere else, so swapping two locals
         * from different methods produces a mutant that does not compile — three of
         * the first five VRO mutants of the stack library were exactly that.
         */
        final java.util.Map<String, String> fieldTypes;

        FileContext(String intField, java.util.Map<String, String> declaredTypes,
                    java.util.Map<String, String> fieldTypes) {
            this.intField = intField;
            this.declaredTypes = declaredTypes;
            this.fieldTypes = fieldTypes;
        }

        /** True when the name is declared somewhere in this file with a numeric type. */
        boolean isNumeric(String name) {
            String type = declaredTypes.get(name.trim());
            return type != null && NUMERIC_TYPES.contains(type);
        }
    }

    /** Which method the walk is inside, and what it returns. */
    private static class MethodContext {
        final String name;
        final String returnType;

        MethodContext(String name, String returnType) {
            this.name = name;
            this.returnType = returnType;
        }

        boolean returnsNumber() {
            return returnType.equals("int") || returnType.equals("long")
                || returnType.equals("short") || returnType.equals("byte")
                || returnType.equals("double") || returnType.equals("float")
                || returnType.equals("Integer") || returnType.equals("Long")
                || returnType.equals("Double") || returnType.equals("Short");
        }

        boolean returnsBoolean() {
            return returnType.equals("boolean") || returnType.equals("Boolean");
        }

        /** A reference type can hold null; a primitive cannot. */
        boolean returnsReference() {
            return !returnType.equals("int") && !returnType.equals("long")
                && !returnType.equals("short") && !returnType.equals("byte")
                && !returnType.equals("double") && !returnType.equals("float")
                && !returnType.equals("boolean") && !returnType.equals("char")
                && !returnType.equals("void");
        }
    }

    /** Lines that carry no behaviour, or that a mutant must not touch. */
    private static boolean isMutableCode(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;
        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) return false;
        if (trimmed.startsWith("package ") || trimmed.startsWith("import ")) return false;
        if (trimmed.equals("}") || trimmed.equals("{") || trimmed.equals("} else {")) return false;
        if (trimmed.startsWith("@")) return false;
        // A class or interface header, and the reset() contract the harness needs.
        if (trimmed.matches("^(public\\s+)?(final\\s+)?(static\\s+)?class\\s+.*")) return false;
        return true;
    }

    /**
     * The methods a mutant must leave alone.
     *
     * <p>{@code reset()} is how every dry run reaches the initial state the spec
     * declares, so a fault seeded there breaks the run before the library under test
     * is even exercised, and would be recorded as a detection of something that was
     * never tested.  {@code refresh()} is the same argument for the libraries that
     * keep a spec-visible mirror.  This is the one place the generator exercises
     * judgement, and it is judgement about the harness rather than about the
     * specification under test.
     */
    private static boolean isHarnessMethod(String method) {
        return method != null && (method.equals("reset") || method.equals("refresh"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // the operators
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Every rewrite every operator wants to make to this line.
     *
     * <p>Each operator is applied at EVERY site on the line, not just the first: a
     * line with two comparisons yields two comparisons' worth of mutants. That is
     * what a mutation suite means by systematic, and it is where the size of these
     * suites comes from.
     *
     * <p>The declared types in {@code file} are what keep the numeric operators off a
     * {@code String} and the null-returning one off an {@code int}. An INVALID mutant
     * measures nothing, so the generator spends the type information it has rather
     * than producing one.
     */
    private static List<Rewrite> rewritesFor(String line, MethodContext method, FileContext file) {
        List<Rewrite> rewrites = new ArrayList<Rewrite>();
        String code = maskNonCode(line);

        arithmeticOperatorReplacement(line, code, rewrites);
        arithmeticOperatorInsertion(line, code, file, rewrites);
        relationalOperatorReplacement(line, code, rewrites);
        conditionalOperatorReplacement(line, code, rewrites);
        conditionalOperatorInsertion(line, code, rewrites);
        unaryOperatorInsertion(line, code, method, file, rewrites);
        absoluteValueInsertion(line, code, method, file, rewrites);
        constantReplacement(line, code, rewrites);
        conditionToConstant(line, code, rewrites);
        loopBoundReplacement(line, code, rewrites);
        returnValueReplacement(line, code, method, rewrites);
        statementDeletion(line, code, rewrites);
        statementReplacement(line, code, method, rewrites);
        argumentReplacement(line, code, rewrites);
        methodCallReplacement(line, code, rewrites);
        sideEffectInsertion(line, code, file, rewrites);
        variableReplacement(line, code, file, rewrites);
        return rewrites;
    }

    /** AOR — every binary arithmetic operator, replaced by each of the others. */
    private static void arithmeticOperatorReplacement(String line, String code, List<Rewrite> out) {
        String[] operators = { "+", "-", "*", "/", "%" };
        for (String from : operators) {
            Matcher site = Pattern.compile("(?<=[\\w)\\]])\\s*\\" + from + "\\s*(?=[\\w(])").matcher(code);
            while (site.find()) {
                int at = code.indexOf(from, site.start());
                if (at < 0 || at >= site.end()) continue;
                for (String to : operators) {
                    if (to.equals(from)) continue;
                    out.add(new Rewrite("arithmetic-operator-replacement",
                            replaceAt(line, at, from.length(), to), from + " becomes " + to));
                }
            }
        }
    }

    /** AOI — an arithmetic operator introduced where a numeric assignment had none. */
    private static void arithmeticOperatorInsertion(String line, String code, FileContext file,
                                                    List<Rewrite> out) {
        Matcher assignment = Pattern.compile("^(\\s*)([\\w.]+)\\s*=\\s*([^;=][^;]*);\\s*$").matcher(line);
        if (!assignment.matches()) return;
        String target = simpleName(assignment.group(2));
        if (!file.isNumeric(target)) return;
        String value = assignment.group(3).trim();
        if (value.contains("?") || value.contains("\"")) return;
        for (String shift : new String[] { " + 1", " - 1" }) {
            out.add(new Rewrite("arithmetic-operator-insertion",
                    assignment.group(1) + assignment.group(2) + " = " + value + shift + ";",
                    "the value assigned to " + target + " is shifted by" + shift));
        }
    }

    /** ROR — every comparison, replaced by each of the others. */
    private static void relationalOperatorReplacement(String line, String code, List<Rewrite> out) {
        String[] operators = { "<=", ">=", "==", "!=", "<", ">" };
        Set<Integer> taken = new LinkedHashSet<Integer>();
        for (String from : operators) {
            int at = -1;
            while ((at = indexOfComparison(code, from, at + 1)) >= 0) {
                if (!taken.add(at)) continue;
                for (String to : operators) {
                    if (to.equals(from)) continue;
                    out.add(new Rewrite("relational-operator-replacement",
                            replaceAt(line, at, from.length(), to), from + " becomes " + to));
                }
            }
        }
    }

    /** COR — every && becomes ||, and every || becomes &&. */
    private static void conditionalOperatorReplacement(String line, String code, List<Rewrite> out) {
        for (String[] swap : new String[][] { { "&&", "||" }, { "||", "&&" } }) {
            int at = -1;
            while ((at = code.indexOf(swap[0], at + 1)) >= 0) {
                out.add(new Rewrite("conditional-operator-replacement",
                        replaceAt(line, at, 2, swap[1]), swap[0] + " becomes " + swap[1]));
            }
        }
    }

    /** COI — the condition of an if or a while, negated. */
    private static void conditionalOperatorInsertion(String line, String code, List<Rewrite> out) {
        Matcher guard = Pattern.compile("^(\\s*)(if|while)\\s*\\((.*)\\)\\s*\\{\\s*$").matcher(line);
        if (!guard.matches()) return;
        out.add(new Rewrite("conditional-operator-insertion",
                guard.group(1) + guard.group(2) + " (!(" + guard.group(3) + ")) {",
                "the guard is negated"));
    }

    /** UOI — a unary minus put in front of a numeric value, or a negation dropped. */
    private static void unaryOperatorInsertion(String line, String code, MethodContext method,
                                               FileContext file, List<Rewrite> out) {
        Matcher returned = RETURN.matcher(line);
        if (returned.matches() && method != null && method.returnsNumber()
                && isSimpleExpression(returned.group(2))) {
            out.add(new Rewrite("unary-operator-insertion",
                    returned.group(1) + "return -(" + returned.group(2) + ");",
                    "the returned value is negated"));
        }

        Matcher assigned = Pattern.compile("^(\\s*)([\\w.]+)\\s*=\\s*([^;=][^;]*);\\s*$").matcher(line);
        if (assigned.matches() && file.isNumeric(simpleName(assigned.group(2)))
                && isSimpleExpression(assigned.group(3))) {
            out.add(new Rewrite("unary-operator-insertion",
                    assigned.group(1) + assigned.group(2) + " = -(" + assigned.group(3) + ");",
                    "the value assigned to " + simpleName(assigned.group(2)) + " is negated"));
        }

        int bang = code.indexOf('!');
        if (bang >= 0 && !code.startsWith("!=", bang) && (bang == 0 || code.charAt(bang - 1) != '=')) {
            out.add(new Rewrite("unary-operator-insertion",
                    replaceAt(line, bang, 1, ""), "a negation is dropped"));
        }
    }

    /** ABS — a value forced through -abs(...), so its sign can never be right. */
    private static void absoluteValueInsertion(String line, String code, MethodContext method,
                                               FileContext file, List<Rewrite> out) {
        Matcher returned = RETURN.matcher(line);
        if (returned.matches() && method != null && method.returnsNumber()
                && isSimpleExpression(returned.group(2))) {
            out.add(new Rewrite("absolute-value-insertion",
                    returned.group(1) + "return -Math.abs(" + returned.group(2) + ");",
                    "the returned value is forced negative"));
        }
        Matcher assigned = Pattern.compile("^(\\s*)([\\w.]+)\\s*=\\s*([^;=][^;]*);\\s*$").matcher(line);
        if (assigned.matches() && file.isNumeric(simpleName(assigned.group(2)))
                && isSimpleExpression(assigned.group(3))) {
            out.add(new Rewrite("absolute-value-insertion",
                    assigned.group(1) + assigned.group(2) + " = -Math.abs("
                            + assigned.group(3) + ");",
                    "the value assigned to " + simpleName(assigned.group(2)) + " is forced negative"));
        }
    }

    /** CRP — every integer literal, replaced by each of a small set of others. */
    private static void constantReplacement(String line, String code, List<Rewrite> out) {
        Matcher literal = Pattern.compile("(?<![\\w.])(\\d+)(?![\\w.])").matcher(code);
        while (literal.find()) {
            String found = literal.group(1);
            long value = Long.parseLong(found);
            for (String to : new String[] { "0", "1", "-1",
                                            String.valueOf(value + 1), String.valueOf(value - 1) }) {
                if (to.equals(found)) continue;
                out.add(new Rewrite("constant-replacement",
                        replaceAt(line, literal.start(1), found.length(), to),
                        "the literal " + found + " becomes " + to));
            }
        }
    }

    /** CTC — a guard forced to true and to false. */
    private static void conditionToConstant(String line, String code, List<Rewrite> out) {
        Matcher guard = Pattern.compile("^(\\s*)if\\s*\\((.*)\\)\\s*\\{\\s*$").matcher(line);
        if (!guard.matches()) return;
        // `while (false)` makes the body unreachable, which does not compile, so only
        // `if` is forced: an INVALID mutant measures nothing.
        out.add(new Rewrite("condition-to-constant",
                guard.group(1) + "if (true) {", "the branch is always taken"));
        out.add(new Rewrite("condition-to-constant",
                guard.group(1) + "if (false) {", "the branch is never taken"));
    }

    /** LBR — a for loop's start and end bound, each moved by one. */
    private static void loopBoundReplacement(String line, String code, List<Rewrite> out) {
        Matcher loop = Pattern.compile(
                "^(\\s*)for\\s*\\(\\s*(\\w[\\w\\[\\]<>]*)\\s+(\\w+)\\s*=\\s*([^;]+);([^;]+);([^)]+)\\)\\s*\\{\\s*$")
                .matcher(line);
        if (!loop.matches()) return;
        String indent = loop.group(1), type = loop.group(2), name = loop.group(3);
        String start = loop.group(4).trim(), guard = loop.group(5).trim(), step = loop.group(6).trim();

        out.add(new Rewrite("loop-bound-replacement",
                indent + "for (" + type + " " + name + " = " + start + " + 1; " + guard + "; " + step + ") {",
                "the loop starts one later"));
        out.add(new Rewrite("loop-bound-replacement",
                indent + "for (" + type + " " + name + " = " + start + " - 1; " + guard + "; " + step + ") {",
                "the loop starts one earlier"));
        if (guard.contains("<") && !guard.contains("<=")) {
            out.add(new Rewrite("loop-bound-replacement",
                    indent + "for (" + type + " " + name + " = " + start + "; "
                            + guard.replace("<", "<=") + "; " + step + ") {",
                    "the loop runs one iteration longer"));
        }
    }

    /** RVR — a return, replaced by a constant of the method's own return type. */
    private static void returnValueReplacement(String line, String code, MethodContext method,
                                               List<Rewrite> out) {
        Matcher returned = RETURN.matcher(line);
        if (!returned.matches() || method == null) return;
        String indent = returned.group(1), value = returned.group(2).trim();

        if (method.returnsBoolean()) {
            for (String to : new String[] { "true", "false" }) {
                if (!value.equals(to)) {
                    out.add(new Rewrite("return-value-replacement", indent + "return " + to + ";",
                            "the answer is always " + to));
                }
            }
            return;
        }
        if (method.returnsNumber()) {
            for (String to : new String[] { "0", "1", "-1" }) {
                if (value.equals(to)) continue;
                out.add(new Rewrite("return-value-replacement", indent + "return " + to + ";",
                        "the answer becomes " + to));
            }
            return;
        }
        if (method.returnsReference() && !value.equals("null")) {
            out.add(new Rewrite("return-value-replacement", indent + "return null;",
                    "the answer becomes null"));
        }
    }

    /** SDL — the statement, removed. */
    private static void statementDeletion(String line, String code, List<Rewrite> out) {
        if (!isDeletableStatement(code)) return;
        out.add(new Rewrite("statement-deletion", "", "the statement is removed"));
    }

    /** STR — the statement, replaced by a different call the class already makes. */
    private static void statementReplacement(String line, String code, MethodContext method,
                                             List<Rewrite> out) {
        if (!isDeletableStatement(code)) return;
        Matcher call = Pattern.compile("^\\s*([\\w.]+)\\.(\\w+)\\((.*)\\);\\s*$").matcher(line);
        if (!call.matches()) return;
        // Running the same call twice: an idempotent operation is unchanged by it and
        // a counting one is not, which is exactly the distinction worth measuring.
        out.add(new Rewrite("statement-replacement",
                line.trim() + " " + line.trim(), "the statement runs twice"));
    }

    /** ARP — an argument shifted, and the arguments of a two-argument call swapped. */
    private static void argumentReplacement(String line, String code, List<Rewrite> out) {
        Matcher single = Pattern.compile("\\.(\\w+)\\(\\s*([\\w.\\[\\]]+)\\s*\\)").matcher(code);
        while (single.find()) {
            String argument = single.group(2).trim();
            if (argument.isEmpty() || argument.equals("null")) continue;
            for (String shift : new String[] { " + 1", " - 1" }) {
                out.add(new Rewrite("argument-replacement",
                        replaceAt(line, single.start(2), single.group(2).length(), argument + shift),
                        "the argument to " + single.group(1) + "() is shifted by" + shift));
            }
        }

        Matcher pair = Pattern.compile("\\.(\\w+)\\(\\s*([\\w.\\[\\]]+)\\s*,\\s*([\\w.\\[\\]]+)\\s*\\)")
                              .matcher(code);
        while (pair.find()) {
            String swapped = "." + pair.group(1) + "(" + pair.group(3) + ", " + pair.group(2) + ")";
            out.add(new Rewrite("argument-replacement",
                    replaceAt(line, pair.start(), pair.end() - pair.start(), swapped),
                    "the two arguments to " + pair.group(1) + "() are swapped"));
        }
    }

    /** MCR — a call on a collection, replaced by a neighbouring one. */
    private static void methodCallReplacement(String line, String code, List<Rewrite> out) {
        String[][] swaps = {
                { ".remove(", ".get(" }, { ".add(", ".contains(" }, { ".put(", ".putIfAbsent(" },
                { ".isEmpty()", ".equals(null)" }, { ".size()", ".hashCode()" },
                { ".containsKey(", ".containsValue(" }, { ".get(", ".remove(" },
        };
        for (String[] swap : swaps) {
            int at = code.indexOf(swap[0]);
            if (at < 0) continue;
            out.add(new Rewrite("method-call-replacement",
                    replaceAt(line, at, swap[0].length(), swap[1]),
                    swap[0] + " becomes " + swap[1]));
        }
    }

    /**
     * SEI — a state write bolted onto a statement that had none.
     *
     * <p>The write has to land on a field that exists, so the generator uses the
     * first {@code static int} of the file. That is nearly always a field the
     * specification names — {@code size}, {@code calls}, {@code events} — which is
     * the point: it turns a query into something a postcondition is about.
     */
    private static void sideEffectInsertion(String line, String code, FileContext file,
                                            List<Rewrite> out) {
        if (file.intField == null) return;
        String write = file.intField + " = " + file.intField + " + 1;";

        Matcher returned = RETURN.matcher(line);
        if (returned.matches()) {
            out.add(new Rewrite("side-effect-insertion",
                    returned.group(1) + write + " return " + returned.group(2) + ";",
                    "a write to " + file.intField + " is inserted before the return"));
            return;
        }
        if (isDeletableStatement(code) && !code.contains(file.intField)) {
            out.add(new Rewrite("side-effect-insertion", line.trim() + " " + write,
                    "a write to " + file.intField + " is inserted after the statement"));
        }
    }

    /**
     * VRO — one name replaced by another of the same declared type.
     *
     * <p>The most productive operator on these libraries, and the one a person would
     * never write by hand: a data structure is mostly a handful of names of the same
     * few types, so "the right operation on the wrong thing" is both the commonest
     * real mistake and the one nobody thinks to seed. Restricting the swap to names
     * of the SAME type is what keeps the mutant compilable — and therefore a
     * measurement rather than an INVALID row.
     */
    private static void variableReplacement(String line, String code, FileContext file,
                                            List<Rewrite> out) {
        // Never on a declaration line. Renaming a field where it is DECLARED does not
        // seed a fault, it declares one field twice and leaves the other missing —
        // 58 of ticketservice's first 71 INVALID mutants were exactly that.
        Matcher declaration = DECLARATION.matcher(line);
        String declaredHere = declaration.find() ? declaration.group(2) : null;

        Matcher name = Pattern.compile("(?<![\\w.$])([a-zA-Z_]\\w*)(?![\\w(])").matcher(code);
        while (name.find()) {
            String found = name.group(1);
            if (found.equals(declaredHere)) continue;
            String type = file.fieldTypes.get(found);
            if (type == null) continue;

            for (java.util.Map.Entry<String, String> candidate : file.fieldTypes.entrySet()) {
                if (candidate.getKey().equals(found)) continue;
                if (!candidate.getValue().equals(type)) continue;
                out.add(new Rewrite("variable-replacement",
                        replaceAt(line, name.start(1), found.length(), candidate.getKey()),
                        found + " becomes " + candidate.getKey()));
            }
        }
    }

    /** A statement that can be removed or duplicated without breaking the code around it. */
    private static boolean isDeletableStatement(String code) {
        String trimmed = code.trim();
        if (!trimmed.endsWith(";")) return false;
        if (trimmed.startsWith("return")) return false;      // a method must still return
        // A declaration cannot simply vanish: whatever it declares is used below.
        return !DECLARATION.matcher(code).find();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the suite for one library.
     *
     * @param libraryKey    the example key, which becomes the mutant id prefix
     * @param sourcePaths   every source of the library, façade first
     * @param limit         at most this many mutants; the walk stops there
     */
    public static List<Mutant> generate(String libraryKey, List<String> sourcePaths, int limit)
            throws IOException {
        List<Mutant> mutants = new ArrayList<Mutant>();
        Set<String> seen = new LinkedHashSet<String>();
        int ordinal = 0;

        for (String sourcePath : sourcePaths) {
            Path path = Paths.get(sourcePath);
            String fileName = path.getFileName().toString();
            List<String> lines = readLines(path);
            MethodContext method = null;
            FileContext context = new FileContext(firstStaticIntField(lines),
                    declaredTypes(lines, false), declaredTypes(lines, true));

            // A line that appears twice inside one method cannot be addressed
            // unambiguously by `find`, so it is skipped rather than seeded wrongly.
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                Matcher header = METHOD_HEADER.matcher(line);
                if (header.matches()) {
                    method = new MethodContext(header.group(2), header.group(1));
                    continue;
                }
                if (!isMutableCode(line)) continue;
                if (method != null && isHarnessMethod(method.name)) continue;
                if (countOccurrencesInMethod(lines, index, line.trim()) != 1) continue;

                for (Rewrite rewrite : rewritesFor(line, method, context)) {
                    if (mutants.size() >= limit) return mutants;
                    String replacement = rewrite.replacement.trim();
                    if (replacement.equals(line.trim())) continue;      // a no-op rewrite

                    String key = fileName + "|" + line.trim() + "|" + replacement;
                    if (!seen.add(key)) continue;                        // the same fault twice

                    ordinal++;
                    String id = libraryKey.toUpperCase() + "_GEN_"
                              + MutationOperator.abbreviationOf(rewrite.operator) + "_" + ordinal;
                    mutants.add(new Mutant(id, libraryKey,
                            rewrite.what + ", in "
                                    + (method == null ? fileName : method.name + "()"),
                            rewrite.operator, method == null ? null : method.name, fileName,
                            line.trim(), replacement, true, "", "generated"));
                }
            }
        }
        return mutants;
    }

    /** Renders a generated suite as a {@code .mutants} file the ordinary parser reads. */
    public static String render(String libraryKey, List<String> sourcePaths, List<Mutant> mutants) {
        StringBuilder out = new StringBuilder();
        out.append("/*\n");
        out.append(" * GENERATED MUTATION SUITE — ").append(libraryKey).append("\n");
        out.append(" *\n");
        out.append(" * Produced by OperatorMutantGenerator from:\n");
        for (String source : sourcePaths) out.append(" *   ").append(source).append("\n");
        out.append(" *\n");
        out.append(" * DO NOT EDIT. Regenerate with ./run-operator-suites.sh ")
           .append(libraryKey).append("\n");
        out.append(" *\n");
        out.append(" * Every operator in MutationOperator is applied to every line of the library\n");
        out.append(" * that admits it, so this file is what the specification is measured against\n");
        out.append(" * when nobody is choosing the faults. It is the counterpart of the\n");
        out.append(" * hand-written set in mutations/").append(libraryKey).append(".mutants:\n");
        out.append(" * that one asks whether the suite catches the faults we thought of, this one\n");
        out.append(" * asks what fraction of all faults of each KIND it catches.\n");
        out.append(" *\n");
        out.append(" * `expect` is absent throughout: a generated mutant carries no prediction,\n");
        out.append(" * and some of these are equivalent to the original and cannot be killed by\n");
        out.append(" * anyone. The run measures; it does not check a declaration.\n");
        out.append(" */\n\n");

        for (Mutant mutant : mutants) {
            out.append("mutant ").append(mutant.getId()).append(" {\n");
            out.append("    description: ").append(mutant.getDescription()).append("\n");
            out.append("    operator:    ").append(mutant.getOperator()).append("\n");
            if (mutant.getMethod() != null) {
                out.append("    in:          ").append(mutant.getMethod()).append("\n");
            }
            if (mutant.getFile() != null) {
                out.append("    file:        ").append(mutant.getFile()).append("\n");
            }
            out.append("    find:        ").append(mutant.getFind()).append("\n");
            out.append("    replace:     ").append(mutant.getReplacement()).append("\n");
            out.append("}\n\n");
        }
        return out.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // small helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Every {@code <type> <name> =} the file declares, field or local.
     *
     * <p>One flat map per file rather than a scope-aware one: the libraries are small
     * and their names do not collide, and the only question asked of it is whether a
     * name is numeric. A name declared twice with different types would be read
     * wrongly — no library here does that, and a mutant built on a wrong answer shows
     * up as INVALID rather than as a bad measurement.
     *
     * @param fieldsOnly stop at the first method, so only class-level declarations
     *        are collected — the ones every method can see
     */
    private static java.util.Map<String, String> declaredTypes(List<String> lines,
                                                               boolean fieldsOnly) {
        java.util.Map<String, String> types = new java.util.LinkedHashMap<String, String>();
        boolean insideAMethod = false;
        for (String line : lines) {
            if (METHOD_HEADER.matcher(line).matches()) insideAMethod = true;
            if (fieldsOnly && insideAMethod) continue;
            Matcher declaration = DECLARATION.matcher(line);
            if (declaration.find()) {
                types.put(declaration.group(2), declaration.group(1).trim());
            }
        }
        return types;
    }

    /** The first {@code static int} field of a file, or null when it has none. */
    private static String firstStaticIntField(List<String> lines) {
        for (String line : lines) {
            Matcher field = INT_FIELD.matcher(line);
            if (field.find()) return field.group(1);
        }
        return null;
    }

    private static List<String> readLines(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        List<String> lines = new ArrayList<String>();
        for (String line : text.split("\n", -1)) lines.add(line);
        return lines;
    }

    /** How many times this exact line occurs inside the method the index sits in. */
    private static int countOccurrencesInMethod(List<String> lines, int index, String needle) {
        int from = index, to = index;
        while (from > 0 && !METHOD_HEADER.matcher(lines.get(from)).matches()) from--;
        while (to < lines.size() - 1 && !METHOD_HEADER.matcher(lines.get(to)).matches()) to++;
        int count = 0;
        for (int scan = from; scan <= to && scan < lines.size(); scan++) {
            if (lines.get(scan).trim().equals(needle)) count++;
        }
        return count;
    }

    /**
     * The line as the operators see it: string literals and generic type arguments
     * blanked out.
     *
     * <p>Blanking the generics is what stops {@code List<String>} being read as a
     * less-than followed by a greater-than. That mistake is not merely noisy — the
     * first run produced five mutants of {@code List<=String>}, every one of them
     * INVALID, which is a measurement of nothing dressed up as five data points.
     */
    private static String maskNonCode(String line) {
        return maskGenerics(withoutStringLiterals(line));
    }

    /** Blanks {@code <...>} where it is a type argument rather than a comparison. */
    private static String maskGenerics(String code) {
        String masked = code;
        // Two passes, so a nested Map<String,List<String>> is covered as well.
        for (int pass = 0; pass < 2; pass++) {
            StringBuilder out = new StringBuilder(masked);
            Matcher generic = Pattern.compile("\\b[A-Z]\\w*\\s*<([^<>]*)>").matcher(masked);
            while (generic.find()) {
                for (int index = generic.start(); index < generic.end(); index++) {
                    char character = masked.charAt(index);
                    if (character == '<' || character == '>') out.setCharAt(index, ' ');
                }
            }
            masked = out.toString();
        }
        return masked;
    }

    /** The line with string literals blanked out, so no operator matches inside one. */
    private static String withoutStringLiterals(String line) {
        StringBuilder out = new StringBuilder(line.length());
        boolean inString = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"' && (index == 0 || line.charAt(index - 1) != '\\')) {
                inString = !inString;
                out.append(' ');
            } else {
                out.append(inString ? ' ' : character);
            }
        }
        return out.toString();
    }

    /** An expression simple enough to wrap in a minus sign without changing its shape. */
    private static boolean isSimpleExpression(String expression) {
        String trimmed = expression.trim();
        if (trimmed.equals("null") || trimmed.contains("\"")) return false;
        if (trimmed.equals("true") || trimmed.equals("false")) return false;
        if (trimmed.contains("?") || trimmed.contains("new ")) return false;
        return trimmed.matches("[\\w.()\\[\\]+\\-*/% ]+");
    }

    /** {@code Helper.size} -> {@code size}: the name a declaration would have used. */
    private static String simpleName(String reference) {
        int dot = reference.lastIndexOf('.');
        return dot < 0 ? reference.trim() : reference.substring(dot + 1).trim();
    }

    /**
     * Finds a comparison at or after {@code from}, skipping the one that is really
     * part of a longer operator — the {@code =} inside {@code <=}, the {@code <}
     * inside {@code <=} — and skipping a plain assignment, which is not a comparison
     * at all.
     */
    private static int indexOfComparison(String code, String operator, int from) {
        int index = code.indexOf(operator, from);
        while (index >= 0) {
            char before = index > 0 ? code.charAt(index - 1) : ' ';
            char after = index + operator.length() < code.length()
                    ? code.charAt(index + operator.length()) : ' ';
            boolean partOfLonger =
                    (operator.equals("<") && after == '=')
                 || (operator.equals(">") && after == '=')
                 || before == '<' || before == '>' || before == '!' || before == '=';
            if (!partOfLonger) return index;
            index = code.indexOf(operator, index + 1);
        }
        return -1;
    }

    private static String replaceAt(String line, int at, int length, String replacement) {
        return line.substring(0, at) + replacement + line.substring(at + length);
    }

    private static String replaceFirst(String line, String needle, String replacement) {
        int at = line.indexOf(needle);
        return at < 0 ? line : replaceAt(line, at, needle.length(), replacement);
    }
}
