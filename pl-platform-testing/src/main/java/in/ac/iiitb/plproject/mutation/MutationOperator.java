package in.ac.iiitb.plproject.mutation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The closed set of mutation operators a {@code .mutants} file may declare.
 *
 * <p>Until this existed the {@code operator:} field was free text that nothing
 * read, so two mutation sets could name the same kind of fault differently and
 * nobody would notice.  That is fine while the only question is "what fraction of
 * faults does the suite catch?", and useless for the more informative one:
 * <em>which kinds</em> of fault does it catch?  A score of 78% says nothing about
 * whether the specifications are good at relational faults and blind to return
 * values, or the other way round — and those two situations call for different
 * work.
 *
 * <p>Making the vocabulary closed is what turns the field into data.  The parser
 * rejects an operator that is not on this list, so a typo or an invented name is a
 * syntax error rather than a silent extra row in the per-operator table.
 *
 * <p>The names follow the classical Mothra operator set, in the kebab-case the
 * {@code .mutants} files already use, with the conventional abbreviation kept
 * alongside so the table can be read next to the mutation-testing literature.
 *
 * <p>{@code variable-replacement} was added for the generated suites in
 * {@code operator-suites/}.  A hand-written set names faults a person thought of,
 * and nobody thinks "what if this used the other field of the same type"; a
 * generated suite has no such blind spot, and on these libraries it is one of the
 * most productive operators there is, because a data structure is mostly names of
 * the same few types.
 *
 * <p>An adjacent-statement SWAP is deliberately not here.  A mutant in this project
 * is one line replaced by one line — that is what makes {@code find} unambiguous —
 * and a swap needs two, so it could be declared but never seeded.
 */
public final class MutationOperator {

    /** operator name -> {abbreviation, one-line definition}. */
    private static final Map<String, String[]> OPERATORS = new LinkedHashMap<String, String[]>();

    private static void define(String name, String abbreviation, String definition) {
        OPERATORS.put(name, new String[] { abbreviation, definition });
    }

    static {
        // ── operator faults ──────────────────────────────────────────────────
        define("arithmetic-operator-replacement", "AOR",
               "one arithmetic operator becomes another: + for -, * for /");
        define("arithmetic-operator-insertion", "AOI",
               "an arithmetic operator is introduced where there was none: x becomes x + 1");
        define("relational-operator-replacement", "ROR",
               "one comparison becomes another: < for <=, == for !=");
        define("conditional-operator-replacement", "COR",
               "one logical connective becomes another: && for ||");
        define("conditional-operator-insertion", "COI",
               "a branch condition is negated: if (c) becomes if (!c)");
        define("unary-operator-insertion", "UOI",
               "a unary operator is introduced: x becomes -x");
        define("absolute-value-insertion", "ABS",
               "a value is forced through an absolute value or a sign flip");

        // ── value faults ─────────────────────────────────────────────────────
        define("constant-replacement", "CRP",
               "a literal becomes a different literal: 0 for 1");
        define("condition-to-constant", "CTC",
               "a whole guard becomes true or false, so a branch is always or never taken");
        define("loop-bound-replacement", "LBR",
               "a loop's start or end bound moves by one, so it runs one iteration too few or too many");
        define("return-value-replacement", "RVR",
               "a method returns a different value: a constant, or the wrong variable");

        // ── statement and call faults ────────────────────────────────────────
        define("statement-deletion", "SDL",
               "one statement is removed entirely");
        define("statement-replacement", "STR",
               "one statement is replaced by a different one");
        define("argument-replacement", "ARP",
               "a call is made with a different argument: remove(0) for remove(size - 1)");
        define("method-call-replacement", "MCR",
               "a different method is called: clear() for remove(x)");
        define("side-effect-insertion", "SEI",
               "a read-only method is given a write, so a query stops being a query");
        define("variable-replacement", "VRO",
               "one name becomes another of the same declared type: the right operation on the wrong thing");
    }

    private MutationOperator() { }

    /** True when {@code name} is one of the declared operators. */
    public static boolean isDeclared(String name) {
        return OPERATORS.containsKey(name);
    }

    /** Every operator name, in the order this class documents them. */
    public static Iterable<String> names() {
        return Collections.unmodifiableSet(OPERATORS.keySet());
    }

    /** The conventional abbreviation, e.g. {@code SDL}, or {@code "?"} if undeclared. */
    public static String abbreviationOf(String name) {
        String[] entry = OPERATORS.get(name);
        return entry != null ? entry[0] : "?";
    }

    /** What the operator does, in one line. */
    public static String definitionOf(String name) {
        String[] entry = OPERATORS.get(name);
        return entry != null ? entry[1] : "";
    }

    /** The list an error message shows when an operator is not recognised. */
    public static String declaredNames() {
        return String.join(", ", OPERATORS.keySet());
    }
}
