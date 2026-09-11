# Errors Encountered

One error per dry-run library, recorded while bringing up the examples in
`LIBRARY_DRY_RUNS.md`. Each entry keeps the raw symptom, the reproduction, the
root cause, and the verdict — **real defect** or **false positive**.

These are the seed cases for the false-positive analysis. The pattern they share
is the point: an unguarded run of a library outside its contract produces a
crash, an assertion failure, or a silently wrong answer, and all three *look*
like library defects while none of them are. What separates a finding from a
false positive is whether the precondition held when the call was made.

| # | Library | Symptom | Verdict |
|---|---------|---------|---------|
| E1 | Stack | `peek()` throws on an empty stack | false positive — precondition violated |
| E2 | HashMap | `put()` returns `null`; non-null assertion fails, unboxing throws NPE | false positive — `null` is a legal return |
| E3 | TaskQueue | `getResult()` on an unconstrained `taskId` returns garbage, assertions still pass | false positive — precondition violated, and it hides itself |
| E4 | MathLib | the generated singular case does not compile: `int cannot be dereferenced` | **real defect in the generator** — fixed |
| E5 | Stack | a sequence of nothing but `push` emits the literal text `\old(size)` into Java | **real defect in the generator** — fixed |
| E6 | TaskQueue | `cancelTask -> submit` declares `taskId` twice, and passes it before it exists | **real defect in the generator** — fixed |
| E7 | any | a mutated library that loops forever hangs the whole run | **real defect in the harness** — fixed |

---

## E1 — Stack: `peek()` throws when the stack is empty

**Symptom.** Calling `peek()` with nothing on the stack terminates the run with
an unchecked exception rather than returning a value.

**Reproduction.** `libraries/stack/Helper.java`, immediately after `reset()`:

```java
Helper.reset();                 // S = [], size = 0, top = null
String x = Helper.peek();
```

Observed:

```
state: size=0 top=null
peek THREW java.lang.IndexOutOfBoundsException: Index -1 out of bounds for length 0
```

**Root cause.** `peek()` is `return S.get(S.size() - 1);` with no emptiness
check, so on an empty list the index is `-1`. The exception is a symptom of the
call, not of the implementation.

**Verdict — false positive.** `specs/Stack.spec` states `requires: size > 0` for
`peek`. With `size == 0` the caller is outside the contract, and past a broken
precondition the spec promises nothing about the library's behaviour: throwing,
returning `null`, or returning a stale element are all equally permitted. A
report of "peek crashes" is a report about the test string, not about `Stack`.

**What suppresses it.** The generated code checks the precondition *before* the
call and stops the run when it fails. `SingularCase.java` emits

```java
require(0, "peek", "size > 0", (Helper.size > 0));
```

and `require` throws `PreconditionViolated`, which aborts. Running the generated
shape against an empty stack gives:

```
singular case : StackDryRun
test string   : peek

block 0  peek         PRE   FAIL  size > 0

run stopped at block 0 (peek) — the test string calls it outside its contract

1 of 1 condition(s) FAILED
```

Exit code 1, and the exception never happens. The SPF flavour does the same job
with `Debug.assume(Helper.size > 0)`, which prunes the empty-stack path from the
solver's search rather than reporting it.

**Bearing on false-positive analysis.** This is the baseline shape: *the guard
must run before the call*. A checker that executes the sequence first and
inspects the outcome afterwards cannot tell this apart from a genuine crash,
because by then the crash has already happened.

---

## E2 — HashMap: `put()` returns `null` on a fresh key

**Symptom.** Two distinct failures from the same return value. A non-null
assertion on `put`'s result fails on the first insert, and assigning that result
to an `int` throws `NullPointerException`.

**Reproduction.** `libraries/hashmap/Helper.java`, from a clean map:

```java
Helper.reset();
Integer oldVal = Helper.put("session", 10);
assert oldVal != null;                       // \result != null
int unboxed   = Helper.put("fresh", 30);     // int, not Integer
```

Observed:

```
put#0 returned oldVal = null
non-null assertion on put#0 FAILED: java.lang.AssertionError: \result != null
put#1 returned oldVal = 10
unboxing put("fresh",30) THREW java.lang.NullPointerException: null
```

**Root cause.** `put` returns `M.put(key, value)`, which is the *previous*
mapping and is `null` when the key was absent. The second `put` on the same key
returns `10` as expected — the value is nullable, not wrong. The NPE is the
auto-unboxing of that `null` at the assignment, and is produced entirely by the
caller's choice of `int` over `Integer`.

**Verdict — false positive, in both forms.** `specs/HashMapLib.spec` writes

```
ensures: \result == oldVal && M.get(key) == value;
```

and places no non-null constraint on `oldVal`. `null` is inside the contract. The
generated code must therefore never emit `assert oldVal != null`, and must keep
the declared type boxed so `null` stays representable — which is why
`TypeMapper.isBoxedPrimitiveType` routes `Integer` to `makeSymbolicInteger`
instead of letting it fall through to `makeSymbolicRef` (gap #6 in
`LIBRARY_DRY_RUNS.md`). Before that fix, a symbolic `Integer` was modelled as an
opaque reference and the nullable case was mishandled.

Note what the singular case actually asserts for block 0:

```java
oldVal_0 = Helper.put(key, value);   // SERVER_OUTPUT captured
Integer oldVal = oldVal_0;           // the name the postcondition uses
ensure(0, "put", "M.get(key) == value", ...);
```

The capture is taken and named, and the only postcondition checked is the one the
spec states. `\result == oldVal` is the binding that *defines* the name, so it is
consumed rather than asserted — asserting it would be checking `oldVal == oldVal`.

**Bearing on false-positive analysis.** Two lessons. A nullable SERVER_OUTPUT
must not attract an implicit non-null assertion — the checker's own default is
the source of the false positive, not the spec. And a type-narrowing at the call
site (`int` for `Integer`) manufactures a crash that belongs to the harness.

---

## E3 — TaskQueue: `getResult()` on an unconstrained `taskId`

**Symptom.** The most dangerous of the three, because it does not announce
itself. Calling `getResult` with a `taskId` that was never issued returns a
string built from `null`, and both postconditions in the spec still hold.

**Reproduction.** `libraries/taskqueue/Helper.java`. `submit` issues `taskId = 1`;
a solver with no constraint on `taskId` is free to pick `0`:

```java
Helper.reset();
Integer real = Helper.submit("render-report");   // -> 1, Tasks = {1=render-report}
Integer solverChosen = 0;                        // never issued
String r = Helper.getResult(solverChosen);
Helper.cancelTask(solverChosen);
```

Observed:

```
submit returned taskId = 1  Tasks={1=render-report}
solver-chosen taskId = 0  Tasks.containsKey = false
getResult returned: result-for-null
POST Results.containsKey(taskId) = true
payload assertion FAILED: result matches the submitted payload
after cancelTask(0): Tasks={1=render-report}  real task still queued = true
```

**Root cause.** `getResult` is `"result-for-" + Tasks.get(taskId)`, and
`Tasks.get(0)` is `null`, so string concatenation yields the literal
`"result-for-null"` without throwing. It then writes that garbage into `Results`
under key `0` — which is why the postcondition `Results.containsKey(taskId)`
**passes**. `cancelTask(0)` removes a key that was never there, a silent no-op,
and `!Tasks.containsKey(taskId)` **passes** too. The real task stays queued.

**Verdict — false positive, and a trap.** The precondition
`Tasks.containsKey(taskId)` is false at the call, so nothing observed afterwards
is evidence about `TaskQueue`. But unlike E1 there is no crash to notice: every
postcondition the spec states is satisfied by a run that did the wrong thing to
the wrong task. A checker that only watches postconditions would report this
sequence as *passing*, and a checker that watches the payload would report a
defect. Both are wrong, for the same reason.

**Where the unconstrained `taskId` comes from.** This is limitation #1 in
`LIBRARY_DRY_RUNS.md`: `nextId` is a concrete auto-incrementing counter, so the
solver cannot infer that the issued id is `1`. Left alone it would explore
`taskId = 0`, `taskId = -7`, and every other integer. The generated SPF helper
closes the gap with an assumption rather than a model of the counter:

```java
public String getResult_helper(Integer taskId) {
    taskId = Debug.makeSymbolicInteger("taskId");
    Debug.assume(Helper.Tasks.containsKey(taskId));   // the precondition, as a constraint
```

The `assume` is what makes the cross-block equality hold: it restricts the
symbolic `taskId` to values `submit` could actually have issued. In
`SingularCase.java` the same job needs no solver at all — the concrete value is
simply carried forward in a variable:

```java
taskId = Helper.submit(payload);                      // block 0, captured
require(1, "getResult", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
```

**Bearing on false-positive analysis.** A propagated SERVER_OUTPUT must be
constrained to what its producer could have issued, or every downstream block is
tested against values the API would never hand out. And a passing postcondition
is not proof of a correct run — E3 satisfies its whole spec while corrupting
`Results` and leaving the real task queued. Precondition status has to be part of
the verdict, not just assertion status.

---

## E4 — MathLib: the generated code does not compile

**Symptom.** Adding `specs/MathLib.spec` produced a `SingularCase.java` that
`javac` rejected:

```
SingularCase.java:38: error: bad operand types for binary operator '!='
    ensure(0, "abs", "lastResult == absValue",
           (Helper.lastResult != null && absValue != null && Helper.lastResult.equals(absValue)));
                             ^   first type: int   second type: <null>
SingularCase.java:38: error: int cannot be dereferenced
```

**Reproduction.** Any spec with an `int` state variable whose *name* ends in
`Result` or `Map`, compared with `==`:

```
state { int lastResult; }
spec abs { … ensures: \result == absValue && lastResult == absValue; }
```

**Root cause.** `AstHelper.exprToJavaCode` decides how to render an equality
between reference types by guessing whether either side is a `Map`, and the guess
is made from the variable's NAME: `isLikelyMapExpression` returns true for any
name ending in `Map` or `Result`. For a guessed map it emitted the explicit
null-checked form, `left != null && right != null && left.equals(right)`, which
is not valid Java when the operand is a primitive.

The guess had no business changing the emitted form in the first place:
`java.util.Objects.equals(a, b)` delegates to `a.equals(b)` for a map — the exact
comparison the branch was reaching for — and autoboxes a primitive. The branch
now emits that, and is right under either reading of the name.

**Verdict — real defect**, in the generator rather than in any library, and
latent until a spec happened to name an `int` that way. It was found by adding a
library, which is the argument for having thirteen of them rather than three.

---

## E5, E6 and E7 — found by generating test strings

The first four entries were found by running the libraries. These three were found
by running **sequences nobody had written**: `./run-operator-suites.sh` generates
around 260 test strings per library from the spec, and the first time it ran, three
of them broke the generator rather than the library.

**E5 — a code path chosen by the wrong question.** `NewGenATC` decided between its
library path and its original functional path by asking whether anything in the
test string produced or consumed a SERVER_OUTPUT. Every checked-in test string
calls something that returns a value, so the question had never been asked in
anger. `push -> push -> push` returns nothing anywhere and still constrains state:
it took the functional path, which neither qualifies state as `Helper.<name>` nor
turns `\old(...)` into a snapshot, and the emitted Java contained `\old(size)`
verbatim. The condition is now whether the spec declares a `state` block.

**E6 — per-block and per-function, confused.** `cancelTask -> submit` calls a
consumer before its producer. `taskId` is therefore a CLIENT_INPUT at block 0 and a
capture at block 1, and two places got that wrong: `SingularCase` declared the
block's local inside a brace where `main()` had already declared it, and
`GeneratedATCs`'s `main()` passed the captured local four blocks before it existed.
Both now treat propagation as the per-block fact it is.

**E7 — no timeout.** The harness ran generated programs with `process.waitFor()`
and nothing else. Hand-written mutants never hang; mechanically generated ones do,
routinely, because moving a loop bound is how a loop stops terminating. A
ten-second watchdog now kills the run and counts the mutant as killed.

**Bearing on false-positive analysis.** E1 to E3 are findings that looked like
library defects and were not. E5 to E7 are the mirror image: they looked like
nothing at all — no test failed, because no test existed — until the space of
sequences was explored mechanically. A checked-in test string is evidence about
the path it takes and silent about every path it does not.

---

## Cross-cutting

E1 to E3 are the same error under different lighting: **the library was called
outside its contract, and the resulting behaviour was read as a defect.** They
differ in how loudly they fail — E1 throws, E2 fails an assertion the checker
invented, E3 says nothing at all — which is what makes them useful as a set. Any
false-positive filter worth having has to catch the quiet one. E4 is of a
different kind and is included for the contrast: a finding that survived scrutiny
and turned out to be a genuine bug, in the generator itself.

The guards already in the generated code, for reference:

| Flavour | Guard | Effect |
|---------|-------|--------|
| `SingularCase.java` | `require(...)` → `PreconditionViolated` | stops the run at the offending block, exit 1 |
| `GeneratedATCs.java` (SPF) | `Debug.assume(...)` | prunes the path from the solver's search |
| `GeneratedATCs_JUnit.java` | `assumeTrue(...)` | marks the run skipped, not failed |

### Reproducing

Each snippet above runs against the library in `libraries/<name>/Helper.java`
with nothing but `javac` and `java`; the E2 assertion needs `-ea`. The generated
singular cases for the passing sequences are checked by:

```bash
./verify-generated.sh
```
