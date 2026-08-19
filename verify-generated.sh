#!/usr/bin/env bash
# Verifies the three library dry runs end to end:
#   1. regenerate all three examples
#   2. compile each generated SPF file, JUnit file and library against
#      compile-time stubs for gov.nasa.jpf.symbc.Debug and org.junit
#   3. execute each generated sequence to confirm every assertion holds
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
for example in example1-stack example2-hashmap example3-taskqueue; do
    dir="$PROJECT/outputs/$example"
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
    if java -ea -cp "$WORK/stubs:$out" in.ac.iiitb.plproject.atc.generated.GeneratedATCs_JUnit; then
        echo "  run     : OK  (all generated assertions hold against the real library)"
    else
        echo "  run     : FAILED"
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
