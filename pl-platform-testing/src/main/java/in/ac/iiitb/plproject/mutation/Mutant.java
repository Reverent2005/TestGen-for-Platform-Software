package in.ac.iiitb.plproject.mutation;

import java.util.ArrayList;
import java.util.List;

/**
 * One seeded fault: a single line of a library, rewritten.
 *
 * <p>A mutant is deliberately small.  Mutation testing measures whether a test
 * suite notices a fault, so a fault that changes several things at once tells you
 * nothing about which change was caught — and a fault that changes nothing at all
 * (an <em>equivalent</em> mutant) can never be caught by anyone.  Restricting a
 * mutant to one line, inside one named method, keeps both problems visible.
 *
 * @see MutantSetParser for the {@code .mutants} file format
 */
public class Mutant {

    private final String id;
    private final String libraryKey;
    private final String description;
    private final String operator;
    private final String method;
    private final String file;
    private final String find;
    private final String replacement;
    private final boolean expectedKilled;
    private final String note;
    private final String sourceFile;

    Mutant(String id, String libraryKey, String description, String operator,
           String method, String find, String replacement,
           boolean expectedKilled, String note, String sourceFile) {
        this(id, libraryKey, description, operator, method, null, find, replacement,
             expectedKilled, note, sourceFile);
    }

    Mutant(String id, String libraryKey, String description, String operator,
           String method, String file, String find, String replacement,
           boolean expectedKilled, String note, String sourceFile) {
        this.file = file;
        this.id = id;
        this.libraryKey = libraryKey;
        this.description = description;
        this.operator = operator;
        this.method = method;
        this.find = find;
        this.replacement = replacement;
        this.expectedKilled = expectedKilled;
        this.note = note;
        this.sourceFile = sourceFile;
    }

    /** Unique name, e.g. {@code STACK_PUSH_TOP_NOT_UPDATED}. */
    public String getId() { return id; }

    /** Which library this mutates: {@code stack}, {@code hashmap} or {@code taskqueue}. */
    public String getLibraryKey() { return libraryKey; }

    /** Plain-English statement of the fault, used as the finding's headline. */
    public String getDescription() { return description; }

    /** The mutation operator, e.g. {@code statement-deletion}. */
    public String getOperator() { return operator; }

    /** The method whose body is searched, so a line repeated elsewhere stays unambiguous. */
    public String getMethod() { return method; }

    /**
     * The library file the fault is seeded into, or null for the façade.
     *
     * <p>Only a multi-class library needs this.  When one library is several
     * classes that call each other, a fault can be planted in a collaborator
     * rather than in the class the generated ATC calls — which is the case worth
     * measuring, since it asks whether a specification written against the façade
     * still notices a defect one class further in.
     */
    public String getFile() { return file; }

    /** The line to replace, compared with surrounding whitespace stripped. */
    public String getFind() { return find; }

    /** What replaces it; empty means the line is deleted. */
    public String getReplacement() { return replacement; }

    /**
     * Whether the generated tests are expected to catch this fault.
     *
     * <p>Most mutants are expected to be killed.  A few are declared
     * {@code expect: survives} because the spec deliberately says nothing about
     * what they change — an unconstrained SERVER_OUTPUT, or a documented
     * limitation.  Declaring the expectation is what turns a survivor from
     * standing noise into a regression check: tightening the spec should make a
     * survivor start dying, and that change should be noticed, not ignored.
     */
    public boolean isExpectedKilled() { return expectedKilled; }

    /** Why this mutant is expected to survive, or empty when it is expected to die. */
    public String getNote() { return note; }

    /** The {@code .mutants} file this came from. */
    public String getSourceFile() { return sourceFile; }

    public boolean isDeletion() { return replacement.isEmpty(); }

    /**
     * Applies this mutant to a library source file.
     *
     * <p>The matched line keeps its original indentation and, when deleted, is
     * commented out rather than removed, so the mutated source stays line-for-line
     * comparable with the original and the seeded fault is visible when read.
     *
     * @throws MutationNotApplicable when {@code find} does not match exactly one
     *         line of the named method — an ambiguous or stale mutant is a defect
     *         in the mutation set, not something to apply approximately
     */
    public String applyTo(String librarySource) throws MutationNotApplicable {
        String[] lines = librarySource.split("\n", -1);
        int from = 0;
        int to = lines.length;
        if (method != null) {
            int[] bounds = bodyOf(lines, method);
            if (bounds == null) {
                throw new MutationNotApplicable(this,
                        "no method named " + method + " in the library source");
            }
            from = bounds[0];
            to = bounds[1];
        }

        List<Integer> matches = new ArrayList<Integer>();
        for (int index = from; index < to; index++) {
            if (lines[index].trim().equals(find)) matches.add(index);
        }
        if (matches.isEmpty()) {
            throw new MutationNotApplicable(this, "no line matches `" + find + "`"
                    + (method == null ? "" : " inside " + method + "()"));
        }
        if (matches.size() > 1) {
            throw new MutationNotApplicable(this, matches.size() + " lines match `" + find
                    + "`" + (method == null ? " — add an `in:` field to disambiguate"
                                            : " inside " + method + "()"));
        }

        int target = matches.get(0);
        String indent = lines[target].substring(0, lines[target].indexOf(find.charAt(0)));
        lines[target] = isDeletion()
                ? indent + "// " + find + "   // MUTANT " + id + ": statement deleted"
                : indent + replacement + "   // MUTANT " + id;

        StringBuilder mutated = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) mutated.append('\n');
            mutated.append(lines[index]);
        }
        return mutated.toString();
    }

    /**
     * The half-open line range of the named method's body, found by counting braces
     * from its declaration.  Good enough for the library sources here, which are
     * plain Java with no braces inside string literals.
     */
    private static int[] bodyOf(String[] lines, String methodName) {
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (!line.matches(".*\\b(?:public|private|protected|static)\\b.*\\b"
                    + java.util.regex.Pattern.quote(methodName) + "\\s*\\(.*")) {
                continue;
            }
            int depth = 0;
            boolean opened = false;
            for (int scan = index; scan < lines.length; scan++) {
                for (char character : lines[scan].toCharArray()) {
                    if (character == '{') { depth++; opened = true; }
                    else if (character == '}') depth--;
                }
                if (opened && depth == 0) return new int[] { index, scan + 1 };
            }
        }
        return null;
    }

    /** Raised when a mutant cannot be seeded — the mutation set has drifted from the library. */
    public static class MutationNotApplicable extends Exception {
        private final Mutant mutant;

        MutationNotApplicable(Mutant mutant, String reason) {
            super(mutant.getId() + " cannot be applied to " + mutant.getLibraryKey() + ": " + reason);
            this.mutant = mutant;
        }

        public Mutant getMutant() { return mutant; }
    }

    @Override
    public String toString() {
        return id + " (" + operator + ", " + libraryKey + "." + method + ")";
    }
}
