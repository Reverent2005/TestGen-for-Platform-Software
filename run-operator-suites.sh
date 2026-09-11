#!/usr/bin/env bash
# Builds and runs the generated test suites, and measures them with the
# generated mutation suites. Three phases per library:
#
#   1. generate ~260 test strings from the spec, run each one against the real
#      library, and record whether it passed, stopped at a precondition, or
#      failed a postcondition. Each run is one test.
#   2. generate the operator mutants — every operator in MutationOperator applied
#      to every line of the library source that admits one — and run each against
#      the library's own checked-in test string. This is the per-operator score.
#   3. re-run every mutant that survived phase 2 against the family from phase 1,
#      stopping at the first sequence that kills it. This measures how much of
#      the survival was the specification and how much was the single sequence.
#
# This is the mechanical counterpart of ./run-mutations.sh, which runs the
# hand-written sets in mutations/ and checks their declared expectations. This
# one declares nothing: it measures.
#
# What it writes, all under operator-suites/:
#   <key>.mutants                    the generated mutation suite
#   teststrings/<key>/*.tests        the generated test strings, one file each
#   results/<key>.mutants.tsv        one row per mutant
#   results/<key>.teststrings.tsv    one row per test string
#   SUMMARY.txt                      the three tables
#
# It is SLOW — thousands of runs, each forking a compiler and two JVMs. Budget
# an hour for the whole set; pass keys for a subset.
#
# Run from the repository root:  ./run-operator-suites.sh [stack queue ...]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT/pl-platform-testing"

mvn -q -o compile
java -cp target/classes in.ac.iiitb.plproject.mutation.GeneratedSuiteRunner "$@"
