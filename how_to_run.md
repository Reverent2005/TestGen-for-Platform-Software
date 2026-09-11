# Run Guide: GenATC with Java Pathfinder

This guide outlines the steps to compile and execute Automated Test Cases (ATCs) generated from JML specifications using JPF and Symbolic Pathfinder (SPF) in a WSL environment.

---

## 📋 Prerequisites

Ensure your WSL (Ubuntu) environment has the following tools installed and configured:

### 1. Java 8 (Required)
JPF is strictly compatible with Java 8 bytecode. 
- **Install Java 8** `sudo apt install openjdk-8-jdk`
- **Check version:** `java -version` (Should be `1.8.x`) 
- **Switch version:** `sudo update-alternatives --config java`

### 2. Build Tools
```bash
sudo apt update
sudo apt install ant maven 
```

### 3. JPF And Project Setup
Clone the essentaial repos:
```bash
# Clone JPF Core
git clone https://github.com/javapathfinder/jpf-core.git ~/jpf-core

# Clone Symbolic Pathfinder (Extension)
git clone https://github.com/javapathfinder/jpf-symbc.git ~/jpf-symbc

# Clone the GenATC Project Repository
git clone https://github.com/Reverent2005/TestGen-for-Platform-Software.git
```

JPF uses a global configuration file to locate its extensions. Configure the site.properties file:
- **File Location** ~/.jpf/site.properties
- **File Location** `mkdir -p ~/.jpf`

Make a file named site.properties and paste your absolute paths. For Example:
```properties
jpf-core = /home/reverent/jpf-core
jpf-symbc = /home/reverent/jpf-symbc
extensions = ${jpf-core},${jpf-symbc}
```

### 4. Building JPF
Before running tests, you must build the JPF:
```bash
# Build JPF Core
cd ~/jpf-core
./gradlew build

# Build JPF Symbc
cd ~/jpf-symbc 
ant build
```

### 5.Running the GenATC
```bash
cd TestGen-for-Platform-Software/pl-platform-testing/
mvn clean 
mvn compile
#runs the simple exmample by default
mvn exec:java
```
You will find some `.jpf` files, `GenratedATCs.java` and a `Helper.java`.

### 6. Running the JPF
Now you need to generate the bytecode(.class) of the java files while in the `outputs` directory:
```bash
# 1. Define JPF Classpath (Absolute Paths)
export JPF_HOME=/home/akshatbetalol/jpf-core
# 2. Create output directory
javac -cp ".:$JPF_HOME/build/jpf.jar:/home/akshatbetalol/jpf-symbc/build/jpf-symbc.jar:/home/akshatbetalol/jpf-symbc/build/jpf-symbc-classes.jar" \
-d target/classes \
$(find src/main/java -name "*.java") \
outputs/*.java
```java -cp target/classes in.ac.iiitb.plproject.Main
java -jar $JPF_HOME/build/RunJPF.jar run.jpf'''
---

## Library dry runs (fourteen libraries)

The library examples with return-value handling have their own driver:

```bash
cd pl-platform-testing/
mvn -o compile
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples lrucache
mvn -o test          # regression suite for the propagation invariants
```

The examples are:

| key | library | test string |
| --- | --- | --- |
| `stack` | `Stack<String>` | `push -> push -> pop -> peek` |
| `hashmap` | `HashMap<String,Integer>` | `put -> put -> getOldValue -> remove` |
| `taskqueue` | TaskQueue | `submit -> getResult -> cancelTask` |
| `ticketservice` | TicketService | `numSeatsAvailable -> findAndHoldSeats -> reserveSeats -> numSeatsAvailable` |
| `arraylib` | IntArray (fixed-size `int[]`) | `set -> set -> get -> sum -> indexOf` |
| `mathlib` | MathLib (integer maths) | `abs -> gcd -> power -> factorial` |
| `queue` | `Queue<String>` | `enqueue -> enqueue -> dequeue -> front -> isEmpty` |
| `linkedlist` | `SinglyLinkedList<Integer>` | `addFirst -> addLast -> removeFirst -> indexOf` |
| `set` | StringSet | `add -> add -> contains -> remove` |
| `bst` | BinarySearchTree | `insert -> insert -> insert -> contains -> min -> delete` |
| `heap` | `MinHeap<Integer>` | `insert -> insert -> insert -> peekMin -> extractMin` |
| `graph` | Graph (adjacency lists) | `addVertex -> addVertex -> addEdge -> degree -> hasEdge` |
| `lrucache` | LruCache — **the custom library** | `put -> put -> put -> get -> restore` |
| `orderservice` | OrderService — **four classes interacting** | `restock -> placeOrder -> ship -> stockLevel` |

`ticketservice` is modelled on the ticket-service coding challenge at
[yingw787/walmart_challenge_07_07_2017](https://github.com/yingw787/walmart_challenge_07_07_2017).
`lrucache` is not a standard container: it was written for this set because it is
the smallest library in which the callee invents a value the caller cannot know —
the key it evicted — which may legitimately be `null` and which a later block then
has to consume.  `orderservice` is the one library that is not a single class: a
façade over a catalogue, a ledger and an audit trail, specified entirely through
the façade.  See [LIBRARY_DRY_RUNS.md](LIBRARY_DRY_RUNS.md#libraries-that-interact).

Each example writes three generated files: the SPF flavour, the JUnit flavour, and
`SingularCase.java` — one concrete run of the test string that checks every
precondition before its call and every postcondition after it, and exits non-zero
if any fails. The test strings themselves live in `specs/*.tests`.

```bash
cd ../ && ./verify-generated.sh    # compile and run every example, singular cases included
```

See [LIBRARY_DRY_RUNS.md](LIBRARY_DRY_RUNS.md) for the outputs, the spec files and
the known limitations.

## The test cases in JUnit

`mvn -o test` runs the test strings in `specs/*.tests` through the whole pipeline
and reports them to JUnit **condition by condition**, so the surefire report names
every precondition and postcondition the generated singular case checked:

```
StackDryRun : push -> push -> pop -> peek   [specs/Stack.tests]
  ├─ the generated sources compile
  ├─ block 0  push  requires  elem != null
  ├─ block 0  push  ensures   size == \old(size) + 1
  ├─ …
  └─ GeneratedATCs_JUnit replays the sequence with assertions enabled
```

That is `GeneratedTestCasesTest`.  `LibraryDryRunInvariantsTest` sits next to it
and asserts on the generated *code*; this one asserts on what that code does when
it runs against the real library.

## Mutation testing

```bash
./run-mutations.sh                  # from the repository root
./run-mutations.sh stack lrucache   # a subset
```

Every mutant in `mutations/*.mutants` seeds one fault into one library, the whole
pipeline is re-run against it, and the generated suite either catches it or does
not.  The same run happens inside `mvn -o test` as `MutationScoreTest`, one JUnit
test per mutant; skip it with `-Dmutation.skip=true`.

135 mutants, 107 killed — **79.3%**.  Every mutant declares which of sixteen
mutation operators it is, and the run scores the suite both per library and per
operator, which is the more useful of the two: it says that faults in
book-keeping are caught almost always (`statement-deletion` 97.6%,
`side-effect-insertion` 100%) and faults in a computed answer are not
(`loop-bound-replacement` 25%, `relational-operator-replacement` 40%).

See [MUTATION_TESTING.md](MUTATION_TESTING.md) for the current score, the format
of a mutation set, and what each surviving mutant says about the specs.

## Generated test suites

The libraries ship one hand-written test string each, and a good share of the
surviving mutants survive only because of that one sequence. To measure how much:

```bash
./run-operator-suites.sh            # every library — SLOW, budget an hour
./run-operator-suites.sh stack      # one library, a minute or two
```

It generates ~260 test strings per library from the spec, runs each one, then
generates the operator mutants, runs those, and finally re-runs every survivor
against the generated family to see how many die once the sequence changes.
Everything it writes lands in `operator-suites/`.

See [operator.md](operator.md) for the operators, the generation rules, the flow,
what is stored where, and the results.
