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

## Library dry runs (Stack / HashMap / TaskQueue)

The three library examples with return-value handling have their own driver:

```bash
cd pl-platform-testing/
mvn -o compile
java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples
mvn -o test          # regression suite for the propagation invariants
```

Each example writes three generated files: the SPF flavour, the JUnit flavour, and
`SingularCase.java` — one concrete run of the test string that checks every
precondition before its call and every postcondition after it, and exits non-zero
if any fails. The test strings themselves live in `specs/*.tests`.

```bash
cd ../ && ./verify-generated.sh    # compile and run all three, singular cases included
```

See [LIBRARY_DRY_RUNS.md](LIBRARY_DRY_RUNS.md) for the outputs, the spec files and
the known limitations.
