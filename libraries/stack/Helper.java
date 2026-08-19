package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.List;

/**
 * EXAMPLE 1 — Stack&lt;String&gt;.
 *
 * The library under test for the push -&gt; push -&gt; pop -&gt; peek dry run.
 * Plain Java, no framework dependencies: the generated ATC calls it through the
 * {@code Helper.<method>(...)} convention the code generator emits.
 *
 * Global state (mirrors specs/Stack.spec):
 * <pre>
 *   S    : List&lt;String&gt; = []
 *   size : int           = 0
 *   top  : String        = null   // derived: S[size-1], the element peek() returns
 * </pre>
 *
 * {@code top} is a derived view of {@code S[size-1]}, exposed as its own field
 * because the .spec grammar cannot index a list by a computed index.
 */
public class Helper {

    public static List<String> S = new ArrayList<String>();
    public static int size = 0;
    public static String top = null;

    /** pre: elem != null;  post: S' = S + [elem], size' = size + 1, top' = elem. */
    public static void push(String elem) {
        S.add(elem);
        size = S.size();
        top = elem;
    }

    /**
     * pre: size &gt; 0;  post: \result = poppedElem, S' = S[0..size-2], size' = size - 1.
     *
     * The popped element is a SERVER_OUTPUT: the caller cannot know which element
     * comes back without asking the library.
     */
    public static String pop() {
        String poppedElem = S.remove(S.size() - 1);
        size = S.size();
        top = S.isEmpty() ? null : S.get(S.size() - 1);
        return poppedElem;
    }

    /** pre: size &gt; 0;  post: \result = S[size-1], S' = S, size' = size. */
    public static String peek() {
        return S.get(S.size() - 1);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        S = new ArrayList<String>();
        size = 0;
        top = null;
    }
}
