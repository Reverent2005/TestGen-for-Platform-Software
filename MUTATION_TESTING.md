# Mutation testing the generated suites

`verify-generated.sh` answers *does the generated test suite pass?*  That question
has a cheap wrong answer: a suite that asserts nothing passes too.  Mutation
testing asks the useful question instead — **when the library is broken, does the
generated suite notice?**

Each mutant seeds one small fault into one library and the whole pipeline is
re-run against it: the spec and test string are re-parsed, the propagation scan
runs again, `GeneratedATCs.java`, `GeneratedATCs_JUnit.java` and
`SingularCase.java` are regenerated, compiled, and executed.  Nothing is cached
between mutants, so the result is a measurement of the *pipeline*, not of one
stored artifact.

## Running it

```bash
./run-mutations.sh                       # every library
./run-mutations.sh stack queue lrucache  # a subset
```

or as part of the JUnit suite, where every mutant is its own test:

```bash
cd pl-platform-testing
mvn -o test                              # includes MutationScoreTest
mvn -o test -Dmutation.skip=true         # everything except the mutation run
```

## Current result

```
library         mutants   killed  survived  invalid score
arraylib              9        7         2        0  77.8%
bst                   8        7         1        0  87.5%
graph                 8        6         2        0  75.0%
hashmap              11        9         2        0  81.8%
heap                  9        8         1        0  88.9%
linkedlist            7        5         2        0  71.4%
lrucache              8        7         1        0  87.5%
mathlib              12        8         4        0  66.7%
orderservice         14       12         2        0  85.7%
queue                 8        7         1        0  87.5%
set                   6        4         2        0  66.7%
stack                11        8         3        0  72.7%
taskqueue            11        8         3        0  72.7%
ticketservice        13       11         2        0  84.6%
ALL                 135      107        28        0  79.3%
```

**Killed** means a generated precondition or postcondition rejected the fault.
**Survived** means every generated condition still held.  The score is
`killed / (killed + survived)`; **invalid** mutants — ones that could not be
seeded, or that produced a library which does not compile — are excluded,
because they measure nothing.

## The same mutants, counted by operator

A single percentage is a poor summary.  "79% of faults are caught" does not say
whether the specifications are sharp about arithmetic and blind to return values
or the other way round, and those two situations call for completely different
work.  Every mutant therefore declares which **kind** of fault it is, from a
closed vocabulary of sixteen operators (`MutationOperator`), and the run reports
the score both ways:

```
operator                           abbr   mutants   killed  survived score
arithmetic-operator-replacement    AOR          6        5         1  83.3%
arithmetic-operator-insertion      AOI          7        5         2  71.4%
relational-operator-replacement    ROR          5        2         3  40.0%
conditional-operator-replacement   COR          2        2         0 100.0%
conditional-operator-insertion     COI          4        2         2  50.0%
unary-operator-insertion           UOI          7        6         1  85.7%
absolute-value-insertion           ABS          2        2         0 100.0%
constant-replacement               CRP          7        6         1  85.7%
condition-to-constant              CTC          2        2         0 100.0%
loop-bound-replacement             LBR          4        1         3  25.0%
return-value-replacement           RVR         14        9         5  64.3%
statement-deletion                 SDL         41       40         1  97.6%
statement-replacement              STR          6        5         1  83.3%
argument-replacement               ARP         13        7         6  53.8%
method-call-replacement            MCR          5        3         2  60.0%
side-effect-insertion              SEI         10       10         0 100.0%
```

The vocabulary is **closed**: the parser rejects an operator that is not on the
list, so a typo or an invented name is a syntax error rather than a silent extra
row, and `MutationScoreTest` fails if any declared operator has no mutant at all.
Without both of those the table would be a tally of whatever words people wrote.

### What the table says

Read down the score column and the specifications sort themselves into two
kinds of clause.

**Caught almost always — the operators that change how much work happens.**
`statement-deletion` (97.6%) and `side-effect-insertion` (100%) are at the top
because nearly every spec in the set counts something: `size`, `writes`,
`calls`, `events`, `edgeCount`, `nextId`. A deleted statement stops a counter
moving and an inserted one moves a counter that should have stayed still, and a
counter is exactly what an `\old(...)` clause compares. `condition-to-constant`
and `conditional-operator-replacement` follow for the same reason: forcing a
branch usually changes a count too.

**Missed most often — the operators that change which value comes back.**
`loop-bound-replacement` (25%), `relational-operator-replacement` (40%) and
`argument-replacement` (53.8%) are at the bottom, and they fail together because
they fail in the same way. A loop that runs one iteration too few still returns
*a* number; a comparison flipped by one still returns *an* index; a call made
with the wrong argument still returns *an element*. The specs constrain the
**range** of an answer (`total >= 0`, `-1 <= position < size`, `powerValue > 0`)
because the grammar has no quantifier with which to constrain its **value** — so
every fault that lands inside the declared range walks through.

That is one finding, not fifteen: **this pipeline is good at catching faults in
bookkeeping and bad at catching faults in answers**, and the reason is a missing
quantifier rather than a missing test. It is also why `return-value-replacement`
sits in the middle at 64%: a return value that is *state-bearing* (a hold id, an
evicted key) is caught by the state clauses around it, and a return value that is
merely *computed* is not caught at all.

## Survivors are the finding

A survivor is not a defect in the runner.  It is a statement about what the spec
and the test string, taken together, do not pin down.  The twenty-eight of them
fall into groups, and the group a survivor is in says what it would take to kill
it.  The three tables below name twenty of them; the eight added with the
operator-coverage mutants belong to the same three groups — with one exception,
which is a group of its own and is described after them.  Every survivor carries
its own `note` in `mutations/*.mutants`.

### 1. The test string never asks

Seven survive because the sequence is too short or too uniform to tell the fault
apart from correct behaviour.  Every one of them dies against a longer test
string, with no change to any `.spec` file.

| Mutant | What would kill it |
| --- | --- |
| `STACK_PUSH_AT_BOTTOM` | reorder `specs/Stack.tests` to `push -> push -> peek -> pop` (verified) |
| `STACK_PEEK_RETURNS_BOTTOM` | the same reordering — peeking before the pop separates "top of the stack" from "end of the list" |
| `HASHMAP_REMOVE_CLEARS_MAP` | a second, untouched key in `specs/HashMapLib.tests` |
| `SET_REMOVE_CLEARS_EVERYTHING` | a second, different element — the two adds use the same one, so the set holds exactly one entry |
| `SET_CONTAINS_ALWAYS_TRUE` | a lookup of an element that was never added |
| `GRAPH_HASEDGE_ALWAYS_TRUE` | a third vertex, and a question about a pair that was never joined |
| `HEAP_EXTRACT_NO_REFILL` | a second `extractMin`, which would find the hole the first one left |

### 2. The spec could say it in this grammar, and does not

Six survive because a postcondition is weaker than it needs to be.  Each is
fixable by writing one more conjunct — sometimes after giving the library one
more derived field to name.

| Mutant | The clause that is missing |
| --- | --- |
| `STACK_POP_FROM_BOTTOM` | which element `pop` returned. `Queue.spec` writes exactly this clause — `dequeuedElem == \old(head)` — and `QUEUE_DEQUEUE_FROM_BACK`, the same fault, dies there. The two examples side by side are the clearest evidence in the set that a survivor is a property of the specification. |
| `QUEUE_ENQUEUE_AT_FRONT` | which end `enqueue` writes to. `head` is re-derived from `Q[0]` after every call, so a queue filled backwards stays self-consistent; naming the other end needs a second derived field, as `head` itself is. |
| `LRU_RESTORE_LOSES_VALUE` | `C.get(evictedKey) == value` in `restore`'s ensures. Nothing currently says the value comes back with the key. |
| `TASKQUEUE_RESULT_IS_NULL` | what `Results[taskId]` holds, as opposed to that it exists. |
| `TICKETSERVICE_HOLD_ID_FROM_SEAT_PRODUCT` | that a hold id is unique, or that it relates to `nextHoldId`. |
| `HASHMAP_PUT_OLDVAL_FABRICATED` | nothing — this one is **deliberate**. `oldVal` is a nullable SERVER_OUTPUT the spec constrains on purpose not at all, and no generated assertion mentions it. This survivor is the evidence for Section 5's invariant 2. |

### 3. The grammar cannot say it at all

Seven need a richer specification *language*, not a longer test string or a
sharper spec.  No sequence over this grammar catches them.

| Mutant | What the grammar lacks |
| --- | --- |
| `ARRAY_SUM_SKIPS_FIRST` | a summing quantifier: `sum` can only be constrained by its range |
| `ARRAY_INDEXOF_ALWAYS_FIRST` | a conditional — `A[position] == value` holds only when the value was found |
| `LIST_INDEXOF_NEVER_MATCHES` | the same conditional, in the other direction: `-1` is inside the declared range |
| `MATH_GCD_IGNORES_FIRST_ARG` | quantification over the divisors the answer beats. The spec says "divides both", and 1 divides everything |
| `MATH_POWER_ONE_TOO_FEW` | an exponentiation operator: `powerValue > 0` is satisfied by 27 as well as by 81 |
| `TASKQUEUE_ID_OFF_BY_ONE` | a way to pin down an auto-incrementing global — shifting `taskId` moves every block of the sequence together |
| `TICKETSERVICE_ALLOCATES_WORST_SEATS` | any way to name an individual seat. "Find and hold the **best** available seats" is what the ticket-service problem is actually about, and every generated condition holds while the customer is given the worst seats in the house |

### 4. The precondition already rules the fault out

One survivor is in none of those groups.  `BST_INSERT_TIES_GO_LEFT` sends a key
equal to the node's own key down the wrong branch — and `insert`'s precondition
requires the key to be **absent**, so no two keys in the run are ever equal and
the changed branch is never the one taken.  The mutant is unreachable rather than
undetected.

That is worth separating from the other three, because it is the bill for a
choice made deliberately.  `BinarySearchTree.spec` narrows `insert`'s domain in
order to buy an unconditional `size == \old(size) + 1`; the same narrowing puts
the duplicate-key behaviour outside the contract, where the suite has nothing to
say about it.  A stronger precondition always buys a stronger postcondition with
exactly this currency.

That, and `TICKETSERVICE_ALLOCATES_WORST_SEATS`, are the sharpest results in the
table, and the reason the groups are worth separating: a score of 79.3% says
nothing on its own, while "these need a better test string, these need a better
spec, these need a better language, and this one is outside the contract by
design" is an agenda.

Making any of those changes is what `expect:` is for: the mutants would start
dying, `MutationScoreTest` would fail on the stale declaration, and the score
would only settle again once the improvement was written down.

## Declaring what should happen

Every mutant carries an expectation:

```
mutant STACK_PUSH_AT_BOTTOM {
    description: push inserts at the bottom of the list instead of the top
    operator:    call-argument-mutation
    in:          push
    find:        S.add(elem);
    replace:     S.add(0, elem);
    expect:      survives
    note:        Stack.spec constrains size and top, never the order of S. …
}
```

`expect: killed` is the default and is left implicit.  `expect: survives`
requires a `note` saying why — a survivor nobody explained is indistinguishable
from a gap nobody noticed, so the parser rejects one without it.

### The generated counterpart

The sets described here are hand-written: a person chose each fault and declared
what should happen to it. `operator-suites/` holds the other kind — every operator
applied to every line that admits one, several hundred per library, declaring
nothing and measuring instead. It also generates a family of test strings per
library, which is what finally tests the claim so many of the notes below make:
*"reorder the test string and this fault dies."* See [operator.md](operator.md).

This is why `MutationScoreTest` checks the declaration rather than simply failing
on every survivor.  A permanent block of red tests for known, documented gaps
teaches people to ignore red.  Declaring them turns the file into a two-way
regression check:

* tighten a postcondition and a declared survivor starts dying — the test fails
  until the declaration is updated, so the improvement is recorded;
* weaken a postcondition and a mutant that used to die starts surviving — the
  test fails too, so the regression cannot pass quietly.

## The mutation-set format

One file per library, named after it: `mutations/stack.mutants` is the set for
the `stack` example.  Comments follow the same rules as `.spec` and `.tests`
files.  Each entry is a named block of `key: value` fields, where the value is
the rest of the line, trimmed — there is no terminating semicolon, because
`find` and `replace` carry Java statements that already end in one.

| Field | Meaning |
| --- | --- |
| `description` | Plain-English statement of the fault. Required. |
| `operator` | Which kind of fault this is. Required, and must be one of the sixteen below. |
| `in` | The method whose body is searched, so a line that appears in two methods stays unambiguous. |
| `file` | Which source of a multi-class library to seed into. Defaults to the façade, `Helper.java`. |
| `find` | The line to replace, compared with surrounding whitespace stripped. Required. |
| `replace` | What replaces it. Empty deletes the line. Required — write it empty deliberately. |
| `expect` | `killed` (default) or `survives`. |
| `note` | Why a declared survivor survives. Required when `expect: survives`. |

### The operator vocabulary

Seventeen operators, following the classical Mothra set, with the conventional
abbreviation kept so the table above can be read next to the literature. The list
lives in `MutationOperator.java` and the parser enforces it.

| Operator | Abbr | What it does |
| --- | --- | --- |
| `arithmetic-operator-replacement` | AOR | one arithmetic operator becomes another: `+` for `-`, `*` for `/` |
| `arithmetic-operator-insertion` | AOI | an operator appears where there was none: `x` becomes `x + 1` |
| `relational-operator-replacement` | ROR | one comparison becomes another: `<` for `<=`, `==` for `!=` |
| `conditional-operator-replacement` | COR | one connective becomes another: `&&` for `\|\|` |
| `conditional-operator-insertion` | COI | a branch condition is negated |
| `unary-operator-insertion` | UOI | a unary operator appears: `x` becomes `-x` |
| `absolute-value-insertion` | ABS | a value is forced through an absolute value or a sign flip |
| `constant-replacement` | CRP | a literal becomes a different literal |
| `condition-to-constant` | CTC | a guard becomes `true` or `false`, so a branch always or never runs |
| `loop-bound-replacement` | LBR | a loop's start or end bound moves by one |
| `return-value-replacement` | RVR | a method returns a different value |
| `statement-deletion` | SDL | one statement is removed |
| `statement-replacement` | STR | one statement becomes a different one |
| `argument-replacement` | ARP | a call is made with a different argument |
| `method-call-replacement` | MCR | a different method is called |
| `side-effect-insertion` | SEI | a read-only method is given a write |
| `variable-replacement` | VRO | one field becomes another of the same declared type |

### Seeding a fault into a collaborator

A library that is several classes needs one more field. `file` names the source to
mutate; without it the fault goes into the façade, which is the only class the
generated ATC ever calls — and the interesting question for a multi-class library
is whether a spec written at the façade still catches a fault one or two classes
further in:

```
mutant AUDIT_RECORD_NOT_COUNTED {
    description: the trail is appended to but the event counter never moves
    operator:    statement-deletion
    file:        Audit.java
    in:          record
    find:        events = events + 1;
    replace:
}
```

`Audit` is two calls from the façade — `Helper` never mentions it, only `Ledger`
does — and the mutant dies on `events == \old(events) + 1`, a clause written
against `Helper`. Every source of the library is copied into the mutant's sandbox
so it still compiles as the library it is; exactly one of them is rewritten on the
way in, and a `file` that names no source of that library is an `INVALID` mutant
rather than a silent fallback to the façade.

A mutant is kept to a single line on purpose.  A fault that changes several
things at once tells you nothing about which change was caught, and `find` must
match exactly one line inside `in` — zero or several matches make the mutant
`INVALID` rather than approximately applied, so a mutation set cannot silently
drift away from the library it describes.
