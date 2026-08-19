package gov.nasa.jpf.symbc;

/**
 * COMPILE-TIME STUB ONLY — not a JPF implementation.
 *
 * Exists so the generated SPF file can be type-checked without the jpf-symbc
 * jars on the classpath.  Under a real run, JPF intercepts every one of these
 * calls; these bodies return harmless concrete defaults so the same file can also
 * be executed plainly to sanity-check the sequence.
 */
public class Debug {
    public static void assume(boolean condition) { }
    public static int makeSymbolicInteger(String name) { return 0; }
    public static double makeSymbolicReal(String name) { return 0.0; }
    public static String makeSymbolicString(String name) { return name; }
    public static boolean makeSymbolicBoolean(String name) { return true; }
    @SuppressWarnings("unchecked")
    public static <T> T makeSymbolicRef(String name, T defaultValue) { return defaultValue; }
    public static Object makeSymbolicObject(String name) { return null; }
}
