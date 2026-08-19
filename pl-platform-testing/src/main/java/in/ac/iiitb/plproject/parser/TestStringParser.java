package in.ac.iiitb.plproject.parser;

import in.ac.iiitb.plproject.atc.ConcreteInput;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a {@code .tests} file into a {@link TestStringAst}.
 *
 * <p>This is the {@code TestStringParser -> TestStringAst} stage the README's
 * parsing layer describes.  Until now a test string could only be written as a
 * hardcoded {@code Arrays.asList("push", "push", ...)} in Java; making it a file
 * means the sequence can be edited, validated and version-controlled alongside
 * the {@code .spec} it runs against.
 *
 * <h3>Format</h3>
 *
 * <pre>
 * test StackDryRun {
 *     sequence: push -&gt; push -&gt; pop -&gt; peek;
 *
 *     inputs {
 *         push[0].elem = "alpha";
 *         push[1].elem = "beta";
 *     }
 * }
 * </pre>
 *
 * <p>{@code sequence} is the test string itself: the blocks, in order.  Blocks may
 * be separated by {@code ->}, {@code ,} or whitespace.
 *
 * <p>{@code inputs} is optional and is what turns a test string into a SINGULAR
 * CASE.  Each line binds one CLIENT_INPUT of one block, addressed as
 * {@code function[blockIndex].param}, to a Java literal.  The block index is a
 * position in the sequence, 0-based, and the function name is checked against
 * the sequence so a binding cannot silently drift onto the wrong block.
 *
 * <p>SERVER_OUTPUT values must NOT be bound here — the caller cannot know them in
 * advance; they come from the call and are threaded forward by the propagation
 * scan.  {@link TestStringValidator} rejects an attempt to bind one.
 *
 * <p>A bare sequence with no wrapper is also accepted, for a quick one-liner:
 * <pre>push -&gt; push -&gt; pop -&gt; peek</pre>
 */
public class TestStringParser {

    private static final Pattern TEST_BLOCK = Pattern.compile(
            "test\\s+(\\w+)\\s*\\{(.*)\\}", Pattern.DOTALL);

    private static final Pattern SEQUENCE_CLAUSE = Pattern.compile(
            "sequence\\s*:\\s*([^;]+);", Pattern.DOTALL);

    private static final Pattern INPUTS_BLOCK = Pattern.compile(
            "inputs\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    /** {@code push[0].elem = "alpha"} */
    private static final Pattern INPUT_BINDING = Pattern.compile(
            "(\\w+)\\s*\\[\\s*(\\d+)\\s*\\]\\s*\\.\\s*(\\w+)\\s*=\\s*(.+)");

    /** Raised when the file cannot be read as a test string at all. */
    public static class TestStringSyntaxException extends RuntimeException {
        public TestStringSyntaxException(String message) {
            super(message);
        }
    }

    public static TestStringAst parse(String text) {
        String stripped = stripComments(text);

        Matcher block = TEST_BLOCK.matcher(stripped);
        if (!block.find()) {
            // Bare form: the whole file is just the sequence.
            List<String> calls = parseSequence(stripped);
            if (calls.isEmpty()) {
                throw new TestStringSyntaxException(
                        "no test string found: expected `test <Name> { sequence: a -> b; }`"
                      + " or a bare sequence such as `push -> push -> pop`");
            }
            return new TestStringAst(null, calls, null);
        }

        String name = block.group(1);
        String body = block.group(2);

        Matcher sequence = SEQUENCE_CLAUSE.matcher(body);
        if (!sequence.find()) {
            throw new TestStringSyntaxException(
                    "test " + name + " has no `sequence:` clause");
        }
        List<String> calls = parseSequence(sequence.group(1));
        if (calls.isEmpty()) {
            throw new TestStringSyntaxException(
                    "test " + name + " has an empty sequence");
        }

        return new TestStringAst(name, calls, parseInputs(name, body, calls));
    }

    private static List<String> parseSequence(String text) {
        List<String> calls = new ArrayList<String>();
        for (String token : text.split("->|,|\\s+")) {
            String call = token.trim();
            if (!call.isEmpty()) calls.add(call);
        }
        return calls;
    }

    private static List<ConcreteInput> parseInputs(String testName, String body, List<String> calls) {
        List<ConcreteInput> inputs = new ArrayList<ConcreteInput>();

        Matcher inputsBlock = INPUTS_BLOCK.matcher(body);
        if (!inputsBlock.find()) return inputs;

        for (String line : inputsBlock.group(1).split(";")) {
            String binding = line.trim();
            if (binding.isEmpty()) continue;

            Matcher matcher = INPUT_BINDING.matcher(binding);
            if (!matcher.matches()) {
                throw new TestStringSyntaxException(
                        "test " + testName + ": cannot read input binding `" + binding
                      + "`, expected `function[blockIndex].param = <literal>`");
            }
            inputs.add(new ConcreteInput(
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(1),
                    matcher.group(3),
                    matcher.group(4).trim()));
        }
        return inputs;
    }

    /** Drops block and line comments, matching the .spec file conventions. */
    static String stripComments(String text) {
        return text.replaceAll("(?s)/\\*.*?\\*/", " ")
                   .replaceAll("(?m)//[^\n]*", " ");
    }
}
