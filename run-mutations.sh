#!/usr/bin/env bash
# Runs every mutation set through the whole pipeline and reports the score.
#
# For each mutant in mutations/*.mutants:
#   1. seed the fault into a private copy of the library
#   2. re-run the pipeline from the spec file: AST -> propagation scan -> ATC IR
#      -> GeneratedATCs.java, GeneratedATCs_JUnit.java, SingularCase.java
#   3. compile the generated sources against verify-stubs
#   4. execute them and see whether any generated pre/postcondition rejects the fault
#
# Exits non-zero when a mutant contradicts what its mutation set declared —
# either it could not be seeded, or `expect:` disagrees with what happened.
#
# Run from the repository root:  ./run-mutations.sh [stack|hashmap|taskqueue ...]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT/pl-platform-testing"

mvn -q -o compile
java -cp target/classes in.ac.iiitb.plproject.mutation.MutationRunner "$@"
