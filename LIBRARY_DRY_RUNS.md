# Library Testing Dry Runs with Return Value Handling

Fourteen library examples wired into the existing TestGen pipeline.  Each one is
a `.spec`, a `.tests` and one or more plain-Java library sources; between them
they exercise every case the return-value handling has to get right, and every
kind of thing a data structure's contract has to say.

| # | Library | Test string | What it exercises |
|---|---------|-------------|-------------------|
| 1 | `Stack<String>` | `push -> push -> pop -> peek` | independent captures, no propagation |
| 2 | `HashMap<String,Integer>` | `put -> put -> getOldValue -> remove` | a nullable capture, produced but unconsumed |
| 3 | `TaskQueue` | `submit -> getResult -> cancelTask` | full two-hop forward propagation |
| 4 | `TicketService` | `numSeatsAvailable -> findAndHoldSeats -> reserveSeats -> numSeatsAvailable` | propagation beside a repeated read-only query |
| 5 | `IntArray` | `set -> set -> get -> sum -> indexOf` | primitive `int` captures over indexed state |
| 6 | `MathLib` | `abs -> gcd -> power -> factorial` | pure functions, specified by a property of the answer |
| 7 | `Queue<String>` | `enqueue -> enqueue -> dequeue -> front -> isEmpty` | FIFO order pinned down; a `boolean` capture |
| 8 | `SinglyLinkedList<Integer>` | `addFirst -> addLast -> removeFirst -> indexOf` | both ends of one structure, through a mirror |
| 9 | `StringSet` | `add -> add -> contains -> remove` | an idempotent call, and a `boolean` SERVER_OUTPUT |
| 10 | `BinarySearchTree` | `insert -> insert -> insert -> contains -> min -> delete` | a precondition bought to strengthen a postcondition |
| 11 | `MinHeap<Integer>` | `insert -> insert -> insert -> peekMin -> extractMin` | a value named before the call and after it |
| 12 | `Graph` | `addVertex -> addVertex -> addEdge -> degree -> hasEdge` | two parameters against one piece of state |
| 13 | `LruCache` **(custom)** | `put -> put -> put -> get -> restore` | a **nullable** SERVER_OUTPUT that is **propagated** |
| 14 | `OrderService` **(4 classes)** | `restock -> placeOrder -> ship -> stockLevel` | **libraries that interact** — one spec over four classes |

Examples 1–3 are the original three; 4 transcribes a published coding challenge;
5–12 are the standard containers and the integer maths library; 13 and 14 were
written for this set — 13 for the one shape of value the others leave out, and 14
because every other example is a single class.

### Why a custom library

`LruCache` is Example 13 and it is not a textbook container.  It exists because
the other twelve leave one combination uncovered: a value the callee **invents**,
which may legitimately be **null**, and which the caller then has to **use**.

* Example 2's `oldVal` is nullable, produced, never consumed.
* Example 3's `taskId` is never null, produced, consumed twice.
* Example 13's `evictedKey` is nullable, produced three times, consumed once.

`put` returns the key it evicted to make room — `null` for the first two calls,
and a key the cache chose by its own recency book-keeping for the third.
`restore` then takes that key as a formal parameter, so the propagation scan
threads the **third** put's value into the last block:

```
step 1 put:      availableServerOutputs(after) = [evictedKey]               propagated = []
step 2 put:      availableServerOutputs(after) = [evictedKey]               propagated = []
step 3 put:      availableServerOutputs(after) = [evictedKey]               propagated = []
step 4 get:      availableServerOutputs(after) = [evictedKey, cachedValue]  propagated = []
step 5 restore:  availableServerOutputs(after) = [evictedKey, cachedValue]  propagated = [evictedKey]
```

```java
{   // ── block 4: restore ──
    String evictedKey = evictedKey_2; // SERVER_OUTPUT propagated from put
    String value = "1";               // CLIENT_INPUT
    require(4, "restore", "evictedKey != null && !C.containsKey(evictedKey)", ...);
```

```
singular case : LruCacheDryRun
test string   : put -> put -> put -> get -> restore

block 0  put          PRE   ok    key != null && value != null && capacity > 0
block 0  put          POST  ok    C.get(key) == value
block 0  put          POST  ok    mostRecent == key
block 0  put          POST  ok    size <= capacity
...
block 3  get          PRE   ok    C.containsKey(key)
block 3  get          POST  ok    cachedValue == C.get(key)
block 3  get          POST  ok    mostRecent == key
block 3  get          POST  ok    size == \old(size)
block 4  restore      PRE   ok    evictedKey != null && !C.containsKey(evictedKey)
block 4  restore      POST  ok    C.containsKey(evictedKey)
block 4  restore      POST  ok    mostRecent == evictedKey
block 4  restore      POST  ok    size <= capacity

20 condition(s) checked, all hold — the test string runs and meets its pre/postconditions
```

One call, two parameters, one of each kind: the caller still has the evicted
entry's value, and only the cache knows which key it threw away.  That asymmetry
inside a single call is the reason the library was written, and it is what makes
two of its mutants die against a **precondition** of a later block rather than a
postcondition of the one that was broken — the propagated value turns the rest of
the sequence into an oracle for the call that produced it.

### Libraries that interact

Examples 1 to 13 are each one class.  Example 14 is four, and it is here to
answer a question none of the others can: **does a specification written against
a façade still catch a fault planted one or two classes further in?**

```
          Helper            the façade, and the only class the ATC ever calls
         /      \
 Catalogue      Ledger      stock levels        orders and their ids
                   |
                 Audit      the append-only trail
```

Nothing about the generated code changes — it still calls `Helper.<method>(...)`
and still reads `Helper.<name>`.  What changes is that those names now stand for
state three other classes own:

* the **collections** are aliases, not copies: `Helper.Stock` *is*
  `Catalogue.Stock`, the same object, so there is no mirror for a fault to hide
  behind;
* the **scalars** cannot be aliased, because assigning an `int` copies it, so they
  are re-read from the collaborators after every call — including after a
  read-only one. Skipping that in `stockLevel` made its own
  `events == \old(events)` clause unfalsifiable, and a mutant survived on it
  until the refresh was put back.

**The clause a single-class example cannot have.** `stockUnits` is the
catalogue's counter and `orderedUnits` is the ledger's, and neither class can see
the other. `placeOrder` therefore asserts a conservation law across the two:

```
stockUnits   == \old(stockUnits)   - quantity
orderedUnits == \old(orderedUnits) + quantity
```

Units move between the two classes; they are never created or destroyed. A fault
in `Catalogue.take`, in `Ledger.open`, or in the way the façade sequences them
moves one half of that pair and leaves the other, and the conjunction sees it.
`events` does the same job one class further out: `Helper` never calls `Audit`,
only `Ledger` does, so every operation that must leave a trace says
`events == \old(events) + 1` and every operation that must not says
`events == \old(events)`.

`mutations/orderservice.mutants` measures exactly that, seeding faults at each
distance with the `file:` field:

| Class | Hops from the façade | Mutants | Killed |
|---|---|---|---|
| `Helper` | 0 | 3 | 3 |
| `Catalogue` | 1 | 5 | 4 |
| `Ledger` | 1 | 4 | 3 |
| `Audit` | 2 | 2 | 2 |

**Distance costs nothing.** Both survivors are gaps in what the specification
says — a counter the spec never relates to the map beside it, and the propagated-id
gap Example 3 already records — not in how far away the fault was planted.

The propagated `orderId` crosses a class boundary too: `Helper` does not see it
until `Ledger.open` returns it, and the scan threads it into `ship` exactly as it
threads `taskId` in Example 3. Propagation is a property of the specification, not
of the library's internal structure.

## Test strings

A test string is a file. `specs/<Library>.tests` holds the sequence of blocks and,
optionally, a concrete value for every CLIENT_INPUT:

```
test StackDryRun {
    sequence: push -> push -> pop -> peek;

    inputs {
        push[0].elem = "alpha";
        push[1].elem = "beta";
    }
}
```

`sequence` is the test string itself. `inputs` is what turns it into a **singular
case**: one concrete run whose every precondition and postcondition can be
checked. Each binding addresses one block as `function[blockIndex].param`, 0-based,
and the function name is checked against that position so a binding cannot drift
onto the wrong block.

A SERVER_OUTPUT is never bound here — the caller cannot know it in advance. It is
captured from its call and threaded forward by the propagation scan, which is why
`TaskQueue.tests` binds only `submit[0].payload` and says nothing about `taskId`.

`TestStringParser` (the `TestStringParser -> TestStringAst` stage the README's
parsing layer describes, which did not previously exist) reads these files;
`TestStringValidator` checks them against the spec before anything is generated:

- every block names a function the spec declares
- every CLIENT_INPUT has a value, when the string is meant to be a singular case
- every binding addresses a real block and a real parameter of it
- no binding tries to pin down a SERVER_OUTPUT
- values produced but never consumed are reported as warnings, not errors

A bare one-liner is also accepted for a quick sequence: `push -> push -> pop`.

## The singular case

`SingularCase.java` is generated alongside the two symbolic flavours. It runs the
test string once, concretely, and reports every condition:

```
singular case : TaskQueueDryRun
test string   : submit -> getResult -> cancelTask

block 0  submit       PRE   ok    payload != null
block 0  submit       POST  ok    Tasks.containsKey(taskId)
block 0  submit       POST  ok    nextId == \old(nextId) + 1
block 1  getResult    PRE   ok    Tasks.containsKey(taskId)
block 1  getResult    POST  ok    Results.containsKey(taskId)
block 2  cancelTask   PRE   ok    Tasks.containsKey(taskId)
block 2  cancelTask   POST  ok    !Tasks.containsKey(taskId)

7 condition(s) checked, all hold — the test string runs and meets its pre/postconditions
```

It exits 0 when every condition holds and 1 otherwise, so it works as a check in a
script. The shape is flat — one braced scope per block, in order — which keeps each
block's locals to itself (two `push` blocks each get their own `elem` and
`size_old`) while SERVER_OUTPUT captures are declared outside their block so later
blocks can consume them.

A **failing precondition stops the run**: past a broken precondition the spec
promises nothing about the library's behaviour, so nothing after it is evidence of
anything. Feeding it `pop -> push` against an empty stack gives

```
block 0  pop          PRE   FAIL  size > 0

run stopped at block 0 (pop) — the test string calls it outside its contract
1 of 1 condition(s) FAILED
```

A failing *postcondition* does not stop the run — each one is a real finding about
the library, and reporting all of them is more useful than reporting the first.

The case begins with `Helper.reset()` so it starts from the initial state the
spec's `state` block describes; a library used this way must offer that method.

## Running them

From `pl-platform-testing/`:

```bash
mvn -o compile

# every example, with the propagation trace and both generated flavours
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples

# one example at a time: stack | hashmap | taskqueue | ticketservice | arraylib
#                      | mathlib | queue | linkedlist | set | bst | heap | graph | lrucache
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples lrucache

# the regression suite for Sections 4 and 5
mvn -o test
```

Each run writes, per example, into `pl-platform-testing/outputs/exampleN-<name>/`:

```
in/ac/iiitb/plproject/atc/generated/
    GeneratedATCs.java        # SPF flavour — symbolic placeholders
    GeneratedATCs_JUnit.java  # JUnit flavour — dynamic binding
    SingularCase.java         # one concrete run, checking every pre/postcondition
    Helper.java               # the library under test
GeneratedATCs_main.jpf, GeneratedATCs_<helper>.jpf
```

The three generated files answer three different questions:

| File | Question |
|------|----------|
| `GeneratedATCs.java` | for which inputs *can* this sequence go wrong? (solver) |
| `GeneratedATCs_JUnit.java` | does it hold when SERVER_OUTPUTs are bound at runtime? |
| `SingularCase.java` | does *this one concrete run* satisfy the spec at every step? |

To check the generated files actually compile and that every generated assertion
holds against the real library, run from the repository root:

```bash
./verify-generated.sh
```

It compiles each example against compile-time stubs in `verify-stubs/` (standing
in for `gov.nasa.jpf.symbc.Debug` and `org.junit`) and then executes the sequence
with `-ea`. Those stubs exist only for this check; a real symbolic run uses JPF as
described in `how_to_run.md`.

## What was added

| Path | Contents |
|------|----------|
| `specs/*.spec` | the JML specs, with `\result` bindings and a `state` block |
| `specs/*.tests` | the test strings and their concrete inputs |
| `libraries/<key>/*.java` | the plain-Java libraries under test, one directory each — usually one `Helper.java`, four files for Example 14 |
| `mutations/<key>.mutants` | the seeded faults each library's suite is measured against |
| `pl-platform-testing/golden/<example>/` | golden copies of both generated flavours |
| `verify-stubs/`, `verify-generated.sh` | compile-and-run verification of the generated code |

Spec files gained one optional clause, a `state { ... }` block:

```
state {
    Map<Integer,String> Tasks;
    int nextId;
}
```

Library postconditions constrain global state rather than parameters, so the
generator needs to know which free names in a condition are library state — those
are emitted as `Helper.<name>` — and what type to give an `\old(...)` snapshot. A
spec file without the block behaves exactly as before.

## Gaps fixed in the existing framework

Working through the three examples surfaced these; each fix is scoped to the bug.

1. **`detectResultBindings` never fired.** It matched `\result =` / `\result ==`
   against the postcondition's *string* form, but `BinaryExpr.toString()` renders
   EQUALS as the word `equals` (`(\result equals taskId)`), so no binding was ever
   found. It now walks the AST (`AstHelper.detectResultBindings`).
2. **A function's own parameter could be mistaken for a SERVER_OUTPUT.** `abs`'s
   `\result == x` binds its own input. Detection is now signature-aware: a name
   that is already a formal parameter is CLIENT_INPUT, never server-generated.
3. **No propagation scan.** Step A did not exist; added as `PropagationScan`,
   which builds `availableServerOutputs` and `propagatedParamsPerFunc` and exposes
   the per-block trace.
4. **No capture or propagation IR.** Added `AtcReturnCaptureStmt`,
   `AtcPropagatedInputStmt` and `AtcReturnStmt`.
5. **Helpers had no signature model.** `AtcTestMethod` was always `void name()`.
   It now carries a return type and a formal parameter list, which is what lets a
   no-parameter function return a value and a propagated value be threaded in.
6. **Boxed primitives fell through to `makeSymbolicRef`.** `TypeMapper` now
   recognises them (`isBoxedPrimitiveType`, `symbolicFactoryFor`) and routes
   `Integer` to `makeSymbolicInteger`, keeping the declared type boxed so that
   null stays representable.
7. **`double` used two different factory names** across the transformer
   (`makeSymbolicDouble`) and the code generator (`makeSymbolicReal`). Both now
   emit `makeSymbolicReal`, which is the SPF API.
8. **No JUnit flavour.** `AtcIrCodeGenerator` gained
   `generateJUnitFile(...)`, emitting the same signatures and the same assertions
   with `executeApiCall` / `extractFromResponse` in place of the placeholders.
9. **`.jpf` signatures could not distinguish overloads.** `SpfWrapper` now
   qualifies boxed parameter types, producing
   `symbolic.method = ...getResult_helper(java.lang.Integer)`.
10. **`TestStringParser` was documented but absent.** The README's parsing layer
    lists `TestStringParser -> TestStringAst`, but a test string could only be
    written as a hardcoded `Arrays.asList(...)` in Java. Added, together with
    `TestStringValidator`.
11. **`ConcreteInput` was an empty placeholder** that `SpfWrapper.run` already
    returned a `List` of. It now carries a concrete value bound to one CLIENT_INPUT
    of one block, which is what a singular case is made of.

The original generation path is untouched for specs with no `\result` binding:
`java -cp target/classes in.ac.iiitb.plproject.Main` and `mvn exec:java` produce
byte-identical output to before.

## Two deliberate design decisions

**A propagated value is a formal parameter that the SPF body re-binds.** Step C
asks for a fresh symbolic variable per consuming block; Step D asks for the value
to be a formal parameter threaded from main(). Both hold at once by making the
parameter real and re-binding it in the SPF body only:

```java
public String getResult_helper(Integer taskId) {
    taskId = Debug.makeSymbolicInteger("taskId");   // fresh symbolic, for the solver
    Debug.assume(Helper.Tasks.containsKey(taskId)); // cross-block equality via constraints
```

The JUnit body omits the re-binding and uses the real argument.

**The JUnit flavour is generated from the ATC IR, the SPF flavour from the
Symbolic IR.** Both come from one `AtcClass`, so signatures and assertions are
identical by construction. Generating JUnit before the symbolic transformation is
what lets it see the capture node (to emit dynamic binding rather than a discarded
placeholder) and the assume node (to emit `assumeTrue` rather than
`Debug.assume`). The regression suite checks the two files line up.

The JUnit file also carries a `@Test public void testSequence()` entry point,
which the SPF file does not: it is the harness hook, and the signature-identity
check covers the helpers.

## Known limitations

1. **Auto-incrementing counters.** `nextId` is concrete, so the solver cannot infer
   the actual `taskId` unless the counter is itself modelled symbolically. Out of
   scope.
2. **Custom return types** fall back to `Debug.makeSymbolicRef`, which may
   under-constrain individual fields. Flagged with a comment where the fallback is
   taken; none of the thirteen examples hits it — every one of them returns a
   primitive, a boxed primitive or a `String`.
3. **Propagation is name-based.** `taskId` upstream and `id` downstream would not
   connect. As a mitigation the scan emits a warning when a parameter's *type*
   matches an available SERVER_OUTPUT but its name does not — which is why the
   HashMap run prints a hint about `put(Integer value)` and `oldVal`. It is a hint,
   not an error.
4. **Multi-valued returns** (tuples, records, `login() -> (token, status)`) are not
   supported: a capture holds one bound name. A spec binding several reports only
   the first.
5. **`put`'s size postcondition is conditional** (`size' = size + 1` only when the
   key was absent) and the `.spec` grammar has no conditional expression, so that
   clause is left out of `HashMapLib.spec` rather than asserted wrongly. The
   `M'[key] = value` part is kept. Noted in the spec file itself.
6. **`\old()` over a compound expression** is not modelled — only `\old(<name>)`
   snapshots. The rewriter falls back to rewriting the inner expression in place.
7. **A structure the grammar cannot walk needs a mirror.** `LinkedList`, `BST`
   and `MinHeap` are real chains, trees and heaps, but a spec cannot follow a
   `next` pointer or index a heap array, so each library exposes a mirror (`L`,
   `Keys`, `H`, and the derived `head`/`minKey`/`minValue`) that it rebuilds from
   the real structure after every operation. Rebuilding it *from* the structure
   rather than maintaining it beside one is what keeps a seeded fault visible:
   a broken chain produces a broken mirror. Where the mirror is derived matters
   too — `minValue` is found by scanning the heap rather than by reading its
   root, or `smallest == minValue` would compare `peekMin`'s answer with itself.
8. **A singular case validates one path, not the sequence in general.** It shows
   that these concrete inputs satisfy the spec at every step; the symbolic flavour
   is what asks whether *any* inputs would not. The two are complementary, which is
   why both are generated from the same test string.
