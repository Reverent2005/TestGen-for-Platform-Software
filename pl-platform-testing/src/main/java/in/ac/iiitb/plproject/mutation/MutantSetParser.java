package in.ac.iiitb.plproject.mutation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a {@code .mutants} file — the mutation set for one library.
 *
 * <p>The format follows the same shape as the project's {@code .spec} and
 * {@code .tests} files: block comments and {@code //} comments are ignored, and
 * each entry is a named block of {@code key: value} fields.
 *
 * <pre>
 *   mutant STACK_PUSH_TOP_NOT_UPDATED {
 *       description: push appends the element but never refreshes top
 *       operator:    statement-deletion
 *       in:          push
 *       find:        top = elem;
 *       replace:
 *   }
 * </pre>
 *
 * <p>{@code operator} must name one of the operators {@link MutationOperator}
 * declares — the vocabulary is closed so that the per-operator score is a
 * measurement rather than a tally of whatever words people happened to write.
 * {@code file} names which source of a multi-class library to seed the fault
 * into, and defaults to the façade, {@code Helper.java}.
 *
 * <p>Two optional fields record what the run should find: {@code expect: killed}
 * (the default) or {@code expect: survives}, and a {@code note} explaining a
 * declared survivor.  A survivor with no note is rejected, so the file cannot
 * quietly absorb a gap nobody meant to accept.
 *
 * <p>A field's value is the rest of its line, trimmed — there is no terminating
 * semicolon, because {@code find} and {@code replace} carry Java statements that
 * end in one.  An empty {@code replace} deletes the matched line.  The library a
 * set applies to comes from the file name: {@code mutations/stack.mutants} is the
 * mutation set for the {@code stack} example.
 */
public final class MutantSetParser {

    private MutantSetParser() { }

    private static final Pattern HEADER = Pattern.compile("^mutant\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\{$");
    private static final Pattern FIELD  = Pattern.compile("^([A-Za-z_]+)\\s*:(.*)$");

    /** Raised when a mutation set cannot be read as one. */
    public static class MutantSyntaxException extends RuntimeException {
        MutantSyntaxException(String message) { super(message); }
    }

    /** Reads every {@code *.mutants} file in a directory, in file-name order. */
    public static List<Mutant> parseDirectory(Path directory) throws IOException {
        List<Path> files = new ArrayList<Path>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory, "*.mutants")) {
            for (Path entry : entries) files.add(entry);
        }
        java.util.Collections.sort(files);
        if (files.isEmpty()) {
            throw new IOException("no .mutants files in " + directory.toAbsolutePath());
        }

        List<Mutant> mutants = new ArrayList<Mutant>();
        Set<String> ids = new LinkedHashSet<String>();
        for (Path file : files) {
            for (Mutant mutant : parse(file)) {
                if (!ids.add(mutant.getId())) {
                    throw new MutantSyntaxException("duplicate mutant id " + mutant.getId()
                            + " in " + file.getFileName());
                }
                mutants.add(mutant);
            }
        }
        return mutants;
    }

    /** Reads one mutation set; the library key is the file name without its extension. */
    public static List<Mutant> parse(Path file) throws IOException {
        String name = file.getFileName().toString();
        String libraryKey = name.endsWith(".mutants")
                ? name.substring(0, name.length() - ".mutants".length())
                : name;
        return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
                     libraryKey, name);
    }

    static List<Mutant> parse(String text, String libraryKey, String sourceFile) {
        List<Mutant> mutants = new ArrayList<Mutant>();
        String[] lines = stripBlockComments(text).split("\n", -1);

        for (int index = 0; index < lines.length; index++) {
            String line = stripLineComment(lines[index]).trim();
            if (line.isEmpty()) continue;

            Matcher header = HEADER.matcher(line);
            if (!header.matches()) {
                throw new MutantSyntaxException(sourceFile + ":" + (index + 1)
                        + ": expected `mutant <ID> {` but found: " + line);
            }

            String id = header.group(1);
            String description = null;
            String operator = null;
            String method = null;
            String file = null;
            String find = null;
            String replacement = "";
            String note = "";
            boolean expectedKilled = true;
            boolean replacementSeen = false;
            boolean closed = false;

            while (++index < lines.length) {
                String raw = lines[index];
                String body = stripLineComment(raw).trim();
                if (body.isEmpty()) continue;
                if ("}".equals(body)) { closed = true; break; }

                Matcher field = FIELD.matcher(body);
                if (!field.matches()) {
                    throw new MutantSyntaxException(sourceFile + ":" + (index + 1)
                            + ": expected `<field>: <value>` inside " + id + " but found: " + body);
                }
                String key = field.group(1);
                String value = field.group(2).trim();

                if ("description".equals(key))      description = value;
                else if ("operator".equals(key))    operator = value;
                else if ("in".equals(key))          method = value.isEmpty() ? null : value;
                else if ("file".equals(key))        file = value.isEmpty() ? null : value;
                else if ("find".equals(key))        find = value;
                else if ("replace".equals(key))   { replacement = value; replacementSeen = true; }
                else if ("note".equals(key))        note = value;
                else if ("expect".equals(key)) {
                    if ("killed".equals(value))        expectedKilled = true;
                    else if ("survives".equals(value)) expectedKilled = false;
                    else throw new MutantSyntaxException(sourceFile + ":" + (index + 1)
                            + ": `expect` in " + id + " must be `killed` or `survives`, not `"
                            + value + "`");
                }
                else throw new MutantSyntaxException(sourceFile + ":" + (index + 1)
                            + ": unknown field `" + key + "` in " + id
                            + " (expected description, operator, in, file, find, replace,"
                            + " expect or note)");
            }

            if (!closed) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id + " is never closed");
            }
            if (find == null || find.isEmpty()) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " has no `find` line to mutate");
            }
            if (!replacementSeen) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " has no `replace` field — write it empty to delete the line");
            }
            if (description == null || description.isEmpty()) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " has no `description`; a fault nobody can read is a fault nobody can judge");
            }

            // The operator vocabulary is closed, so that the per-operator score is a
            // measurement rather than a tally of whatever words people wrote.
            if (operator == null || operator.isEmpty()) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " has no `operator`; the per-operator score is only meaningful"
                        + " if every mutant declares which kind of fault it is");
            }
            if (!MutationOperator.isDeclared(operator)) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " declares an unknown operator `" + operator + "`."
                        + " Expected one of: " + MutationOperator.declaredNames());
            }

            if (!expectedKilled && note.isEmpty()) {
                throw new MutantSyntaxException(sourceFile + ": mutant " + id
                        + " is declared `expect: survives` with no `note` saying why;"
                        + " an unexplained survivor is indistinguishable from a missed one");
            }
            mutants.add(new Mutant(id, libraryKey, description, operator,
                    method, file, find, replacement, expectedKilled, note, sourceFile));
        }
        return mutants;
    }

    /**
     * Removes block comments, keeping newlines so line numbers in errors stay true.
     * A {@code //} inside a field value is a real Java comment fragment only when it
     * starts the value, so {@link #stripLineComment} is deliberately conservative.
     */
    private static String stripBlockComments(String text) {
        StringBuilder stripped = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            if (text.startsWith("/*", index)) {
                int end = text.indexOf("*/", index + 2);
                if (end < 0) end = text.length() - 2;
                for (int scan = index; scan < end + 2 && scan < text.length(); scan++) {
                    if (text.charAt(scan) == '\n') stripped.append('\n');
                }
                index = end + 2;
            } else {
                stripped.append(text.charAt(index++));
            }
        }
        return stripped.toString();
    }

    /**
     * Drops a trailing {@code //} comment, but only outside a field value: the
     * {@code replace} of a deletion mutant may itself be Java containing {@code //}.
     */
    private static String stripLineComment(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("//")) return "";
        if (FIELD.matcher(trimmed).matches()) return line;  // values are taken verbatim
        int comment = line.indexOf("//");
        return comment < 0 ? line : line.substring(0, comment);
    }
}
