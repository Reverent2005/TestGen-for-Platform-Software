# operator.md — generated test suites, and the operators that measure them

Everything in this document is about two generated artefacts and the relationship
between them:

* a **test suite** per library — a few hundred test strings, generated from the
  library's specification, each one run against the real library;
* a **mutation suite** per library — every mutation operator applied to every
  line of the library's source that admits one, each one run against the
  generated ATC.

The mutation suite exists to grade the test suite. The test suite exists to
answer the question the mutation suite keeps raising.

---

## 1. The question this answers

`mutations/*.mutants` holds hand-written faults, and roughly a fifth of them
survive. Each survivor carries a `note` explaining why, and a recurring theme in
those notes is a claim of this shape:

> *by the time `peek` runs, this test string has left exactly one element on the
> stack, so bottom and top are the same element. Reordering to
> `push -> push -> peek -> pop` kills this, without changing the spec.*

That is a claim about the **test string**, not about the specification — and every
library in this project ships exactly one test string, hand-written, sitting in
`specs/<Library>.tests`. So the claim was never tested. It was asserted, by the
same person who wrote the sequence that failed to catch the fault.

This machinery turns it into a measurement:

```
score with ONE hand-written sequence      →   score with a GENERATED FAMILY
```

The difference between those two numbers is the part of the score that was never
about the specification at all.

---

## 2. The flow

```
  specs/<Library>.spec ──┐
                         ├──► TestStringGenerator ──► operator-suites/teststrings/<key>/*.tests
  specs/<Library>.tests ─┘         (seeded, ~260)                    │
        (the seed: mined for                                         │  PHASE 1
         literals and edited                                         ▼
         into a family)                            the whole pipeline, once per sequence
                                                   spec + sequence ─► ATC ─► compile ─► run
                                                                       │
                                                         PASSED / STOPPED / FAILED
                                                                       │
  libraries/<key>/*.java ──► OperatorMutantGenerator ──► operator-suites/<key>.mutants
        (the source, walked            (17 operators,                  │
         line by line)                  every line)                    │  PHASE 2
                                                                       ▼
                                                   for each mutant: seed the fault into a
                                                   sandbox copy, regenerate the ATC from the
                                                   spec, compile, run
                                                                       │
                                                            KILLED / SURVIVED / INVALID
                                                                       │
                                                                       │  PHASE 3
                                                                       ▼
                                                   every SURVIVOR, re-run against the passing
                                                   sequences of the family, first kill wins
                                                                       │
                                                                       ▼
                                                        operator-suites/results/*.tsv
                                                        operator-suites/SUMMARY.txt
```

Three things are worth noticing about that diagram.

**The specification is never mutated and never generated.** It is the fixed point
of the whole exercise. What varies is the sequence the library is exercised with
(phase 1) and the library itself (phase 2).

**A mutant is re-run through the entire pipeline**, not against a cached test.
The spec is re-parsed, the propagation scan runs again, and
`GeneratedATCs.java`, `GeneratedATCs_JUnit.java` and `SingularCase.java` are
regenerated from scratch against the mutated library. What is measured is the
pipeline's ability to catch a fault, not a stored artefact's.

**Phase 3 is the only phase that uses both halves.** Phases 1 and 2 are each
useful alone; the cross is what the other two are for.

---

## 3. The operators

Seventeen, following the classical Mothra set, in the kebab-case the `.mutants`
files use, with the conventional abbreviation alongside. The list is closed and
lives in `MutationOperator.java`: the parser rejects an operator that is not on
it, so a typo or an invented name is a syntax error rather than a silent extra row
in the per-operator table.

| Operator | Abbr | What it does |
| --- | --- | --- |
| `arithmetic-operator-replacement` | AOR | one arithmetic operator becomes another: `+` for `-`, `*` for `/` |
| `arithmetic-operator-insertion` | AOI | an operator appears where there was none: `x` becomes `x + 1` |
| `relational-operator-replacement` | ROR | one comparison becomes another: `<` for `<=`, `==` for `!=` |
| `conditional-operator-replacement` | COR | one connective becomes another: `&&` for `\|\|` |
| `conditional-operator-insertion` | COI | a branch condition is negated |
| `unary-operator-insertion` | UOI | a unary operator appears: `x` becomes `-x` |
| `absolute-value-insertion` | ABS | a value is forced through `-Math.abs(...)` |
| `constant-replacement` | CRP | a literal becomes another: `0`, `1`, `-1`, `n±1` |
| `condition-to-constant` | CTC | a guard becomes `true` or `false` |
| `loop-bound-replacement` | LBR | a loop's start or end bound moves by one |
| `return-value-replacement` | RVR | a method returns a constant of its own return type |
| `statement-deletion` | SDL | one statement is removed |
| `statement-replacement` | STR | one statement becomes a different one — here, runs twice |
| `argument-replacement` | ARP | an argument is shifted, or two are swapped |
| `method-call-replacement` | MCR | a different method is called |
| `side-effect-insertion` | SEI | a write is inserted, so a query stops being a query |
| `variable-replacement` | VRO | one field becomes another of the same declared type |

`variable-replacement` was added for the generated suites. A hand-written set
names faults a person thought of, and nobody thinks *"what if this used the other
field of the same type"* — but on these libraries it is one of the most productive
operators there is, because a data structure is mostly a handful of names of the
same few types. (Two hand-written VRO mutants were added afterwards, so the
vocabulary stays fully exercised on both sides.)

An adjacent-statement **swap** is deliberately absent. A mutant here is one line
replaced by one line — that is what makes `find` unambiguous — and a swap needs
two, so it could be declared but never seeded.

---

## 4. How a mutant is generated

`OperatorMutantGenerator` walks each source file line by line and asks every
operator what it would like to do with that line. Each answer becomes one mutant.

**Every site, not the first.** A line with two comparisons yields two comparisons'
worth of mutants. That is where the size of these suites comes from.

**Types are used where they are known.** The generator collects every
`<type> <name> =` declaration in the file and the return type of every method, and
spends them: `-x` is only inserted where `x` is numeric, `return null` only where
the method returns a reference, `variable-replacement` only swaps two names of the
same declared type. A mutant that does not compile is `INVALID`, and an `INVALID`
mutant measures nothing — the type information is what keeps them rare.

**What it will not touch.**

* `reset()` and `refresh()`. `reset()` is how every dry run reaches the initial
  state the spec declares, so a fault seeded there breaks the run before the
  library under test is even exercised, and would be recorded as a detection of
  something that was never tested. `refresh()` is the same argument for the
  libraries that keep a spec-visible mirror.
* Comments, imports, package and class declarations.
* Any line that occurs twice inside the same method — `find` matches a line, and
  an ambiguous mutant is a defect in the suite rather than something to apply
  approximately.
* Generic type arguments. `List<String>` is not a less-than followed by a
  greater-than; before this was fixed, five of stack's mutants were
  `List<=String>`, every one of them `INVALID`.

The output is an ordinary `.mutants` file, so a generated suite is read by the
same parser, seeded by the same `Mutant.applyTo` and run by the same
`MutationRunner` as a hand-written one.

---

## 5. How a test string is generated

`TestStringGenerator` builds a family from the specification and the library's own
checked-in test string.

**A sequence** is either the checked-in one with one to three small edits (70% of
draws) or a fresh draw of 2–7 function names (30%). The four edits are the four
ways a reviewer describes a different test: *what if these two were the other way
round*, *what if it happened twice*, *what if that step were missing*, *what if
this were called as well*.

The 70/30 split is not arbitrary. A sequence drawn purely at random is usually
illegal — it calls `pop` on an empty stack and stops at the first block — and a
run that stops proves nothing about a mutant, because it proves nothing about the
library. Editing the seed raised stack's usable sequences from 55 to 120 out of
260.

**The inputs** come from a pool: first the literals the checked-in test string
already binds for that function and parameter, then the literals it binds anywhere
for a parameter of that type, then a generic fallback by type. Reuse is what makes
a generated sequence land inside the contract often enough to be worth running —
`M.containsKey(key)` is only ever true for a key something else put there.

**A propagated value is never bound.** The propagation scan runs over the draft
sequence first; a parameter that arrives from an upstream SERVER_OUTPUT is left
alone, exactly as `TestStringValidator` requires.

**Every generated string goes through that same validator** before it is kept, so
a generated test string is legal in precisely the sense a hand-written one is.

**Generation is seeded from the library key**, so a family is identical on every
machine and in every run. A suite that changed between runs could not be a
regression baseline.

---

## 6. What is stored where

Everything generated lives under `operator-suites/`, at the repository root:

```
operator-suites/
├── SUMMARY.txt                       the three tables below, as text
├── <key>.mutants                     the generated mutation suite for one library
│                                     — same format as mutations/<key>.mutants
├── teststrings/
│   └── <key>/
│       ├── <key>-001.tests           one generated test string per file, in the
│       ├── <key>-002.tests           ordinary .tests grammar: runnable as-is with
│       └── …                         LibraryDryRunExamples
└── results/
    ├── <key>.teststrings.tsv         one row per test string:
    │                                   name, status, conditions checked, sequence
    └── <key>.mutants.tsv             one row per mutant:
                                        id, status, operator, method, file,
                                        the sequence that killed it in phase 3, detail
```

Scratch space — compiled classes, sandboxed copies of mutated libraries, the
regenerated ATCs — goes to `pl-platform-testing/target/operator-suites/` and is
disposable.

Nothing here overwrites the hand-written material. `mutations/*.mutants`,
`specs/*.tests` and `pl-platform-testing/golden/` are untouched by this run.

The whole directory is **reproducible**: generation is seeded from the library
key, so re-running the script on any machine rewrites byte-identical suites. It is
checked in because a generated test string is a perfectly ordinary `.tests` file —
`teststrings/stack/stack-007.tests` can be pointed at
`LibraryDryRunExamples` on its own, and the results files are what the tables
below are read from — but nothing is lost by ignoring it and regenerating instead.

### The two suites, side by side

|   | hand-written | generated |
| --- | --- | --- |
| mutants | `mutations/*.mutants` | `operator-suites/*.mutants` |
| how many | 137 across 14 libraries | see the table in §8 |
| chosen by | a person | every operator × every line |
| expectation | declared (`expect:`), and checked | none — it measures |
| asks | are the faults we thought of caught? | what fraction of each *kind* is caught? |
| runner | `./run-mutations.sh` | `./run-operator-suites.sh` |
| in `mvn -o test` | yes, as `MutationScoreTest` | no — far too slow |

Neither replaces the other. The hand-written set is a regression check with
declared expectations, so a spec that gets weaker fails a test. The generated set
is a measurement, and it has no opinion about what *should* happen.

---

## 7. How to run it

```bash
./run-operator-suites.sh                 # every library, all three phases
./run-operator-suites.sh stack queue     # a subset
```

From the repository root. It compiles the project first, so there is nothing to do
beforehand.

**It is slow.** Every run in every phase forks a compiler and two JVMs, and there
are thousands of them. A single library takes one to three minutes; the whole set
takes the better part of an hour. Pass keys while iterating.

The other two scripts are unaffected and stay fast:

```bash
./verify-generated.sh                    # the 14 checked-in examples compile and pass
./run-mutations.sh                       # the hand-written mutation sets, ~30s
cd pl-platform-testing && mvn -o test    # everything as JUnit, ~45s
```

The generators themselves *are* covered by `mvn -o test`, without running a single
mutant: `GeneratedSuitesTest` checks that every generated mutant can actually be
seeded into the file it names, that every generated test string passes the same
validator the checked-in ones do, that nothing seeds a fault into `reset()`, and
that the same seed produces the same family twice. Those are the four properties
that make the numbers below mean anything, and all four are checkable without
executing anything.

### Reading the output

Phase 1 classifies each generated test string:

| Status | Meaning |
| --- | --- |
| `PASSED` | every condition it reached holds — a usable oracle |
| `STOPPED` | a **precondition** failed: the sequence stepped outside the library's contract. Not a defect; the spec promises nothing there |
| `FAILED` | a **postcondition** failed — a genuine finding against the library |
| `INVALID` | the generated sources did not compile |

`FAILED` is the one to look at. Against a healthy library it should be zero, and
it is.

Phases 2 and 3 classify each mutant as `KILLED`, `SURVIVED` or `INVALID`, and for
a survivor the results file names the generated sequence that killed it, if one
did.

---

## 8. Results

<!--RESULTS-->

---

## 9. Limitations

**Equivalent mutants are not detected.** Some generated mutants do not change the
library's behaviour at all — `if (key < node.key)` becoming `if (key <= node.key)`
in a tree that never holds two equal keys, for instance. Nobody can kill those, so
they depress the generated score by an amount that cannot be measured without
reading every survivor. This is the standard limitation of generated mutation
testing, and it is why the generated score is reported *beside* the hand-written
one rather than instead of it: the hand-written set has no equivalent mutants,
because a person checked.

**The cross is bounded.** A survivor is re-tried against at most 20 of the family's
passing sequences, in generation order. A mutant that none of those 20 kills is
recorded as surviving the family — so the "with family" column understates the
family rather than overstating it.

**The generator is line-based, not syntax-tree-based.** It matches regular
expressions against source lines, which is a deliberate match to the `.mutants`
format — `find` matches a line — and it is why the skips in §4 are necessary. A
handful of mutants still fail to compile and are reported as `INVALID`.

**A stopped sequence is a weak test.** Slightly over half the generated sequences
stop at a precondition. They are legitimate — each one documents a boundary of the
contract — but they cannot grade a mutant, so only the passing ones are used in
phase 3.

**The families are seeded from the checked-in sequence.** 70% of each family is
that sequence with small edits, which is what makes so many of them legal, and it
also means the family explores the neighbourhood of one point more thoroughly than
it explores the whole space.

---

## 10. Four defects this found

Generating suites tests the pipeline as well as the specifications. Four bugs
turned up that no hand-written test string could have reached, because every
hand-written one is written by someone who already knows what the generator does.

**1. A whole code path was selected by the wrong question.** `NewGenATC` chose
between its library path and its original functional path by asking *does anything
in this test string produce or consume a SERVER_OUTPUT?* — and every checked-in
test string happens to call something that returns a value, so the question was
never asked in anger. A generated sequence of nothing but `push` calls has no
return-value handling and still constrains state, so it took the functional path,
which does not qualify state names or snapshot `\old(...)`. The generated Java
contained the literal text `\old(size)` and did not compile. The test is now
*does the spec declare a `state` block*, which is what actually distinguishes the
two, and `LibraryDryRunInvariantsTest` pins it.

**2. A CLIENT_INPUT could collide with a capture.** `SingularCase` declares every
SERVER_OUTPUT capture as a local of `main()`, and each block's inputs inside a
nested brace. The sequence `cancelTask -> submit` calls a consumer *before* its
producer, so `taskId` is a CLIENT_INPUT at block 0 and a capture from block 1 —
and the block tried to declare a local Java had already declared outside it. The
block now assigns to the outer local instead of shadowing it.

**3. `main()` could pass a value before it existed.** The same sequence in the
other two flavours: a helper's parameter list is per *function* while propagation
is per *block*, so `cancelTask_helper(Integer taskId)` was called with `taskId`
four blocks before `submit_helper()` returned one. Such a call site now passes a
placeholder, which is what the value is at that point — a CLIENT_INPUT the caller
invents — and the SPF body re-binds every parameter symbolically anyway.

**4. The harness could hang forever.** `GeneratedSuiteHarness` ran each generated
program with `process.waitFor()` and no timeout. With hand-written mutants that
never mattered; with mechanically generated ones it is a certainty, because moving
a loop bound is exactly how a loop stops terminating. There is now a ten-second
watchdog — two orders of magnitude more than a healthy run needs — and a mutant
that reaches it is counted as `KILLED`, which is what `TIMED_OUT` means in the
mutation-testing literature: the suite noticed, by never finishing.

The first three share a shape worth naming: each is a case the pipeline handles
per *block* in one place and per *function* in another, and the checked-in test
strings never distinguished the two. Generating sequences is what made the
distinction visible.
