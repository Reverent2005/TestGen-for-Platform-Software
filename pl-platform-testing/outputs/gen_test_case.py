import subprocess
from parse_spf import paths
from solver import solve_paths

inputs = solve_paths(paths)

tests = []

for inp in inputs:

    age = inp["age"]

    proc = subprocess.run(
        ["java","in.ac.iiitb.plproject.atc.generated.Executor",str(age)],
        capture_output=True,
        text=True
    )

    output = proc.stdout.strip()

    tests.append((age,output))


with open("GeneratedTests.java","w") as f:

    f.write("""
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import in.ac.iiitb.plproject.atc.generated.Helper;

public class GeneratedTests {
""")

    for i,(age,out) in enumerate(tests):

        f.write(f"""
@Test
void test_{i}() {{
    assertEquals("{out}", Helper.ageCategoryPrint({age}));
}}
""")

    f.write("\n}")