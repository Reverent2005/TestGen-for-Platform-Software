#!/usr/bin/env bash
# Verifies every library dry run end to end:
#   1. regenerate every example the driver registers
#   2. compile each generated SPF file, JUnit file and library against
#      compile-time stubs for gov.nasa.jpf.symbc.Debug and org.junit
#   3. execute each generated sequence to confirm every assertion holds
#
# The list of examples is not written down here: whatever
# LibraryDryRunExamples writes into outputs/ is what gets verified, so adding a
# library to the driver adds it to this check too.
#
# Run from the repository root:  ./verify-generated.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$ROOT/pl-platform-testing"
STUBS="$ROOT/verify-stubs"
WORK="$ROOT/pl-platform-testing/target/verify"

cd "$PROJECT"
mvn -q -o compile
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples > /dev/null

rm -rf "$WORK"
mkdir -p "$WORK/stubs"
javac -nowarn -d "$WORK/stubs" $(find "$STUBS" -name '*.java')

status=0
for dir in $(ls -d "$PROJECT"/outputs/example*/ | sort -V); do
    example="$(basename "$dir")"
    out="$WORK/$example"
    mkdir -p "$out"

    echo "── $example ──────────────────────────────────────────────"
    if javac -nowarn -cp "$WORK/stubs" -d "$out" $(find "$dir" -name '*.java'); then
        echo "  compile : OK  (GeneratedATCs.java, GeneratedATCs_JUnit.java, SingularCase.java, Helper.java)"
    else
        echo "  compile : FAILED"
        status=1
        continue
    fi

    # -ea so the generated assert() statements are actually checked.
    junit_out="$(java -ea -cp "$WORK/stubs:$out" \
                     in.ac.iiitb.plproject.atc.generated.GeneratedATCs_JUnit 2>&1)" && junit_rc=0 || junit_rc=$?
    if [ "$junit_rc" -eq 0 ]; then
        echo "  run     : OK  (all generated assertions hold against the real library)"
    elif printf '%s' "$junit_out" | grep -q 'org.junit.AssumptionViolatedException'; then
        # The JUnit flavour binds CLIENT_INPUTs to dummy placeholders rather than to
        # values solved against the precondition, so a spec the dummies do not satisfy
        # never runs. That is a skip, not a failure; the singular case below is the
        # oracle for such an example.
        echo "  run     : SKIPPED  (placeholder CLIENT_INPUTs do not satisfy the precondition)"
    else
        echo "  run     : FAILED"
        printf '%s\n' "$junit_out" | sed 's/^/            /'
        status=1
    fi

    # The singular case reports every pre/postcondition and exits non-zero on any failure.
    echo "  singular case:"
    if java -cp "$WORK/stubs:$out" in.ac.iiitb.plproject.atc.generated.SingularCase \
            | sed 's/^/    /'; then
        :
    else
        echo "    FAILED"
        status=1
    fi
done

exit $status
