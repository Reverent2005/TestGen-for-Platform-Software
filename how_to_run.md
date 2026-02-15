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
export JPF_CP="/home/reverent/jpf-core/build/jpf.jar:/home/reverent/jpf-symbc/build/jpf-symbc.jar:/home/reverent/jpf-symbc/build/jpf-symbc-classes.jar:."

# 2. Create output directory
mkdir -p bin

# 3. Compile with Package Structure
javac -cp "$JPF_CP" -d bin \
in/ac/iiitb/plproject/atc/generated/Helper.java \
in/ac/iiitb/plproject/atc/generated/GeneratedATCs.java
```

Run JPF using the `RunJPF.jar` bootstrapper. Ensure your `.jpf` file points to the `./bin` directory by checking if the following properties are set in `GeneratedATCs_main.jpf`:
```properties
target = in.ac.iiitb.plproject.atc.generated.GeneratedATCs
classpath = ./bin
shell = gov.nasa.jpf.symbc.SymbolicInstructionFactory
symbolic.method = in.ac.iiitb.plproject.atc.generated.GeneratedATCs.main(sym)
listener = gov.nasa.jpf.symbc.SymbolicListener
```

Now while inside the `outputs` directory run:
```bash
java -jar ~/jpf-core/build/RunJPF.jar GeneratedATCs_main.jpf
```