# new_library.md — Testing a new library with GenATC

What was added to take the existing GenATC pipeline (which only handled a single
`appendExclamation` example with no return values) and make it test **real
libraries whose calls return values**, plus the exact test strings, functions and
file structure used for each of the three libraries.

Read `how_to_run.md` first for the JPF/SPF environment. This document is about the
*inputs you write* and the *artifacts you get back*.

---

## 1. The three libraries at a glance

| # | Library | Functions | Test string | What it exercises |
|---|---------|-----------|-------------|-------------------|
| 1 | `Stack<String>` | `push`, `pop`, `peek` | `push -> push -> pop -> peek` | two independent captures, **no** propagation |
| 2 | `HashMap<String,Integer>` | `put`, `getOldValue`, `remove` | `put -> put -> getOldValue -> remove` | a **nullable** return, produced but never consumed |
| 3 | `TaskQueue` | `submit`, `getResult`, `cancelTask` | `submit -> getResult -> cancelTask` | **multi-hop forward propagation** of one server value |

The three were chosen so that between them they cover every case the
return-value handling has to get right: a value that is captured and dropped, a
value that may legitimately be `null`, and a value that has to be threaded into
two later calls. Example 3 is structurally the `placeOrder -> shipOrder` REST
pattern — the algorithm does not care whether the callee is a Java library or an
HTTP API.

---

## 2. The structure followed

Every library needs exactly **three hand-written files** and produces **four
generated ones**. Nothing else is hardcoded in Java any more.

### 2.1 What you write

```
specs/<Library>.spec          the JML contract: state + one spec block per function
specs/<Library>.tests         the test string, and the concrete inputs for it
libraries/<name>/Helper.java  the plain-Java library under test
```

### 2.2 What the pipeline produces

```
pl-platform-testing/outputs/exampleN-<name>/
    in/ac/iiitb/plproject/atc/generated/
        GeneratedATCs.java        SPF flavour — symbolic placeholders, for the solver
        GeneratedATCs_JUnit.java  JUnit flavour — server values bound at runtime
        SingularCase.java         one concrete run, checking every pre/postcondition
        Helper.java               the library, copied in beside the generated code
    GeneratedATCs_main.jpf
    GeneratedATCs_<function>_helper.jpf   (one per function)
```

The three generated `.java` files answer three different questions:

| File | Question it answers |
|------|---------------------|
| `GeneratedATCs.java` | for which inputs *can* this sequence go wrong? (SPF solves) |
| `GeneratedATCs_JUnit.java` | does it still hold when SERVER_OUTPUTs are bound at runtime? |
| `SingularCase.java` | does *this one concrete run* satisfy the spec at every step? |

### 2.3 The stages it goes through

```
.spec  ──► SpecToAstConverter ──► JmlSpecAst ─┐
                                              ├─► PropagationScan (STEP A)
.tests ──► TestStringParser  ──► TestStringAst┘        │
                 │                                     ▼
                 └──► TestStringValidator        NewGenATC ──► ATC IR (AtcClass)
                      (checked BEFORE                          │
                       anything is generated)                  ▼
                                       AtcIrToSymbolicIrTransformer / TypeMapper
                                                               │
                        ┌──────────────────────────────────────┼───────────────┐
                        ▼                                      ▼               ▼
              AtcIrCodeGenerator                  AtcIrCodeGenerator   SingularCaseGenerator
              .generateFile()                     .generateJUnitFile()  .generate()
              GeneratedATCs.java                  GeneratedATCs_JUnit   SingularCase.java
                        │
                        ▼
                 SpfWrapper ──► the .jpf configuration files
```

Driver: `in.ac.iiitb.plproject.atc.LibraryDryRunExamples`.

---

## 3. The `.spec` file

Same grammar as before, plus one optional `state { ... }` block.

```
state {
    Map<Integer,String> Tasks;
    Map<Integer,String> Results;
    int nextId;
}

spec submit {
    signature: Integer submit(String payload);
    requires:  payload != null;
    ensures:   \result == taskId && Tasks.containsKey(taskId) && nextId == \old(nextId) + 1;
}
```

Why the `state` block was needed: library postconditions constrain **global
state**, not just parameters. The generator has to know which free names in a
condition are library state, so it can emit them as `Helper.<name>`, and what type
to give an `\old(...)` snapshot (`int nextId_old = Helper.nextId;`). A spec file
without a `state` block behaves exactly as it did before.

**`\result == someName` is how a SERVER_OUTPUT is declared.** The name on the
right becomes the captured variable. Detection is signature-aware: if the name is
already a formal parameter of that same function (`\result == x` in `abs`), it is
a CLIENT_INPUT, not a server value.

---

## 4. The `.tests` file — the test string

A test string is now a **file**, not a hardcoded `Arrays.asList(...)` in Java.

```
test StackDryRun {
    sequence: push -> push -> pop -> peek;

    inputs {
        push[0].elem = "alpha";
        push[1].elem = "beta";
    }
}
```

* `sequence` **is** the test string: the blocks, in order. Separators `->`, `,`
  or whitespace all work. A bare one-liner (`push -> push -> pop`) is also
  accepted for a quick sequence with no concrete inputs.
* `inputs` is what turns the test string into a **singular case**. Each binding
  addresses one block as `function[blockIndex].param` — the index is 0-based and
  the function name is checked against that position, so a binding cannot
  silently drift onto the wrong block.
* **A SERVER_OUTPUT is never bound here.** The caller cannot know it before the
  call; it is captured and threaded forward by the propagation scan. The
  validator rejects any attempt to pin one down.

`TestStringValidator` runs before anything is generated and checks:

- every block names a function the spec declares
- every CLIENT_INPUT has a value (when the string is meant as a singular case)
- every binding addresses a real block and a real parameter of it
- no binding tries to pin down a SERVER_OUTPUT
- values produced but never consumed → **warning**, not an error

---

## 5. The libraries and their test strings, one by one

### Example 1 — `Stack<String>`

**Files:** `specs/Stack.spec`, `specs/Stack.tests`, `libraries/stack/Helper.java`

**State:** `List<String> S`, `int size`, `String top`
(`top` is a derived view of `S[size-1]`, held as its own field because the spec
grammar cannot index a list by a computed index.)

**Functions:**

| Function | Signature | requires | ensures |
|----------|-----------|----------|---------|
| `push` | `void push(String elem)` | `elem != null` | `size == \old(size) + 1 && top == elem` |
| `pop` | `String pop()` | `size > 0` | `\result == poppedElem && size == \old(size) - 1` |
| `peek` | `String peek()` | `size > 0` | `\result == peekedElem && peekedElem == top && size == \old(size)` |

**Test string:** `push -> push -> pop -> peek`
**Inputs:** `push[0].elem = "alpha"`, `push[1].elem = "beta"`

The two pushes deliberately use *different* elements so the run distinguishes
"pop returned the top" from "pop returned something". Neither `pop` nor `peek`
appears in `inputs` — they take no parameters, and what they return is a
SERVER_OUTPUT.

**Why this example exists:** `pop` and `peek` each capture a server value of their
own (`poppedElem`, `peekedElem`) and neither is threaded into the other — `peek`
takes no parameters, so the scan must *not* try to forward `poppedElem` into it.
Both helpers therefore have an **empty parameter list even though both return a
value**, which is precisely the case a naïve implementation gets wrong.

Propagation scan trace:

```
step 1 push:  availableServerOutputs(after) = []                       propagated = []
step 2 push:  availableServerOutputs(after) = []                       propagated = []
step 3 pop:   availableServerOutputs(after) = [poppedElem]             propagated = []
step 4 peek:  availableServerOutputs(after) = [poppedElem, peekedElem] propagated = []
```

Generated `main()`:

```java
instance.push_helper();
instance.push_helper();
String poppedElem = instance.pop_helper();
String peekedElem = instance.peek_helper();
```

### Example 2 — `HashMap<String,Integer>`

**Files:** `specs/HashMapLib.spec`, `specs/HashMapLib.tests`, `libraries/hashmap/Helper.java`

**State:** `Map<String,Integer> M`, `int size`

**Functions:**

| Function | Signature | requires | ensures |
|----------|-----------|----------|---------|
| `put` | `Integer put(String key, Integer value)` | `key != null && value != null` | `\result == oldVal && M.get(key) == value` |
| `getOldValue` | `Integer getOldValue(String key)` | `M.containsKey(key)` | `\result == curVal && curVal == M.get(key) && size == \old(size)` |
| `remove` | `void remove(String key)` | `M.containsKey(key)` | `!M.containsKey(key) && size == \old(size) - 1` |

**Test string:** `put -> put -> getOldValue -> remove`
**Inputs:** `put[0].key = "session"`, `put[0].value = 10`, `put[1].key = "session"`,
`put[1].value = 20`, `getOldValue[2].key = "session"`, `remove[3].key = "session"`

**Both puts use the same key on purpose.** The first finds no previous value and
returns `null`; the second returns the first put's value. That nullable return is
exactly what must **not** attract a non-null assertion, and running the case is
what demonstrates it. `getOldValue` and `remove` reuse the key so their
preconditions (`key ∈ dom(M)`) hold when they run.

Because the return may be null, the declared type stays the **boxed** `Integer`,
and `TypeMapper` routes it to `Debug.makeSymbolicInteger` rather than falling
through to `makeSymbolicRef`.

Two `put` blocks both produce a value called `oldVal`, so the generator
disambiguates them per block:

```java
Integer oldVal_0 = instance.put_helper();
Integer oldVal_1 = instance.put_helper();
Integer curVal   = instance.getOldValue_helper();
instance.remove_helper();
```

The validator emits three warnings here, all of them expected and none of them
errors — `oldVal` and `curVal` are produced but never consumed, and `put`'s own
`Integer value` parameter has the *same type* as the available `oldVal` but a
different name, which the scan flags because propagation is name-based.

**Simplification recorded in the spec:** `put`'s real size clause is conditional
(`size' = size + 1` only when the key was absent). The `.spec` grammar has no
conditional expression, so that clause is omitted rather than asserted wrongly;
the `M'[key] = value` part is kept.

### Example 3 — `TaskQueue`

**Files:** `specs/TaskQueue.spec`, `specs/TaskQueue.tests`, `libraries/taskqueue/Helper.java`

**State:** `Map<Integer,String> Tasks`, `Map<Integer,String> Results`, `int nextId`

**Functions:**

| Function | Signature | requires | ensures |
|----------|-----------|----------|---------|
| `submit` | `Integer submit(String payload)` | `payload != null` | `\result == taskId && Tasks.containsKey(taskId) && nextId == \old(nextId) + 1` |
| `getResult` | `String getResult(Integer taskId)` | `Tasks.containsKey(taskId)` | `\result == result && Results.containsKey(taskId)` |
| `cancelTask` | `void cancelTask(Integer taskId)` | `Tasks.containsKey(taskId)` | `!Tasks.containsKey(taskId)` |

**Test string:** `submit -> getResult -> cancelTask`
**Inputs:** `submit[0].payload = "render-report"` — and nothing else.

`taskId` is the archetypal SERVER_OUTPUT: auto-assigned by the callee, unknowable
to the caller before the call. It is **not** bound in `inputs`; binding it would
be rejected. It is captured from `submit` and threaded across **two** hops:

```
step 1 submit:     availableServerOutputs(after) = [taskId]         propagated = []
step 2 getResult:  availableServerOutputs(after) = [taskId, result] propagated = [taskId]
step 3 cancelTask: availableServerOutputs(after) = [taskId, result] propagated = [taskId]
```

Both downstream helpers take it as a formal parameter and `main()` passes it
explicitly:

```java
Integer taskId = instance.submit_helper();
String result  = instance.getResult_helper(taskId);
instance.cancelTask_helper(taskId);
```

`getResult`'s own SERVER_OUTPUT, `result`, enters the available set but is never
consumed downstream — captured and returned, never forwarded.

**Known limitation:** `nextId` is an auto-incrementing global, so the solver
cannot infer the concrete `taskId` unless `nextId` is itself modelled as a
symbolic integer. Out of scope here; the JUnit flavour and the singular case both
cover it at runtime instead.

---

## 6. The singular case

`SingularCase.java` runs the test string **once, concretely**, and reports every
condition. Its shape is flat — one braced scope per block, in order — so each
block's locals stay its own (the two `push` blocks each get their own `elem` and
`size_old`), while SERVER_OUTPUT captures are declared *outside* the blocks so
later blocks can consume them.

```java
Helper.reset(); // start from the initial state the spec declares

Integer taskId;    // SERVER_OUTPUT captures
String  result;

{   // ── block 0: submit ──
    String payload = "render-report"; // CLIENT_INPUT
    require(0, "submit", "payload != null", (payload != null));
    int nextId_old = Helper.nextId;
    taskId = Helper.submit(payload);  // SERVER_OUTPUT captured
    ensure(0, "submit", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
    ensure(0, "submit", "nextId == \old(nextId) + 1", ...);
}

{   // ── block 1: getResult ──
    // taskId is the SERVER_OUTPUT captured by submit, propagated into this block
    require(1, "getResult", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
    result = Helper.getResult(taskId);
    ...
}
```

Rules it follows:

* **A failing precondition stops the run.** Past a broken precondition the spec
  promises nothing about the library's behaviour, so nothing after it is evidence
  of anything. (`pop -> push` on an empty stack stops at block 0.)
* **A failing postcondition does not stop the run.** Each one is a real finding
  about the library, and reporting all of them beats reporting the first.
* It begins with `Helper.reset()`, so a library tested this way must offer that
  method.
* It exits `0` when every condition holds and `1` otherwise, so it works as a
  check in a script.

---

## 7. Running it

From `pl-platform-testing/`:

```bash
mvn -o compile

# all three examples, with propagation traces and all three generated flavours
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples

# one at a time: stack | hashmap | taskqueue
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples taskqueue

# the regression suite (19 tests, includes golden-file comparison)
mvn -o test
```

From the repository root, to compile **and execute** everything generated:

```bash
./verify-generated.sh
```

It compiles each example against the compile-time stubs in `verify-stubs/`
(standing in for `gov.nasa.jpf.symbc.Debug` and `org.junit`), runs the JUnit
flavour with `-ea` so the generated `assert`s are actually checked, then runs the
singular case. Those stubs exist only for this check — a real symbolic run uses
JPF as described in `how_to_run.md`.

### Results as of the last run

`mvn -o test` — **19 tests, 0 failures.**
`./verify-generated.sh` — all three compile, run and pass:

```
── example1-stack ──
  compile : OK   run : OK
  block 0  push         PRE   ok    elem != null
  block 0  push         POST  ok    size == \old(size) + 1
  block 0  push         POST  ok    top == elem
  block 1  push         PRE   ok    elem != null
  block 1  push         POST  ok    size == \old(size) + 1
  block 1  push         POST  ok    top == elem
  block 2  pop          PRE   ok    size > 0
  block 2  pop          POST  ok    size == \old(size) - 1
  block 3  peek         PRE   ok    size > 0
  block 3  peek         POST  ok    peekedElem == top
  block 3  peek         POST  ok    size == \old(size)
  11 condition(s) checked, all hold

── example2-hashmap ──
  compile : OK   run : OK
  block 0  put          PRE   ok    key != null && value != null
  block 0  put          POST  ok    M.get(key) == value
  block 1  put          PRE   ok    key != null && value != null
  block 1  put          POST  ok    M.get(key) == value
  block 2  getOldValue  PRE   ok    M.containsKey(key)
  block 2  getOldValue  POST  ok    curVal == M.get(key)
  block 2  getOldValue  POST  ok    size == \old(size)
  block 3  remove       PRE   ok    M.containsKey(key)
  block 3  remove       POST  ok    !M.containsKey(key)
  block 3  remove       POST  ok    size == \old(size) - 1
  10 condition(s) checked, all hold

── example3-taskqueue ──
  compile : OK   run : OK
  block 0  submit       PRE   ok    payload != null
  block 0  submit       POST  ok    Tasks.containsKey(taskId)
  block 0  submit       POST  ok    nextId == \old(nextId) + 1
  block 1  getResult    PRE   ok    Tasks.containsKey(taskId)
  block 1  getResult    POST  ok    Results.containsKey(taskId)
  block 2  cancelTask   PRE   ok    Tasks.containsKey(taskId)
  block 2  cancelTask   POST  ok    !Tasks.containsKey(taskId)
  7 condition(s) checked, all hold
```

---

## 8. Adding a fourth library

1. Write `libraries/<name>/Helper.java` — plain Java, `public static` fields for
   state, `public static` methods, package `in.ac.iiitb.plproject.atc.generated`,
   and a `reset()` method.
2. Write `specs/<Library>.spec` — a `state { ... }` block naming those fields, one
   `spec` block per function, and `\result == <name>` in the postcondition of
   every function that returns a server-generated value.
3. Write `specs/<Library>.tests` — the `sequence`, and an `inputs` binding for
   every CLIENT_INPUT of every block. Do not bind anything that came back from a
   call.
4. Register it in `LibraryDryRunExamples` as one more `Example` constant (key,
   title, the three paths, and an output directory) and add it to `ALL`.
5. Run `java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples <key>`,
   read the validator report and the STEP A trace before trusting the generated
   code, then `./verify-generated.sh`.

If propagation does not fire where you expected it, the cause is almost always
name-based matching: the scan links an upstream `\result == taskId` to a
downstream parameter **only when the names are identical**. When the types match
but the names do not, it says so as a warning — rename the parameter.

---

## 9. What had to be fixed in the framework to make this work

Working through the three examples surfaced these; each fix is scoped to its bug.
(Details in `LIBRARY_DRY_RUNS.md`.)

1. `detectResultBindings` never fired — it matched `\result ==` against the
   postcondition's *string* form, but `BinaryExpr.toString()` renders EQUALS as
   the word `equals`. It now walks the AST.
2. A function's own parameter could be mistaken for a SERVER_OUTPUT; detection is
   now signature-aware.
3. No propagation scan existed — added as `PropagationScan` (STEP A).
4. No capture/propagation IR — added `AtcReturnCaptureStmt`,
   `AtcPropagatedInputStmt`, `AtcReturnStmt`.
5. `AtcTestMethod` was always `void name()`; it now carries a return type and a
   formal parameter list.
6. Boxed primitives fell through to `makeSymbolicRef`; `TypeMapper` now routes
   `Integer` to `makeSymbolicInteger` and keeps the type boxed so `null` stays
   representable.
7. `double` used two different factory names; both now emit `makeSymbolicReal`.
8. No JUnit flavour — added `AtcIrCodeGenerator.generateJUnitFile(...)`.
9. `.jpf` signatures could not distinguish overloads; `SpfWrapper` now qualifies
   boxed parameter types.
10. `TestStringParser` was documented in the README but absent — added, together
    with `TestStringValidator`.
