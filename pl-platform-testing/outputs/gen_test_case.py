import subprocess
from solver import solve_one
from parse_spf import paths

inp = solve_one(paths)

if inp is None:
    print("No satisfying input found")
    exit()

age = inp["age"]

proc = subprocess.run(
    ["java","in.ac.iiitb.plproject.atc.generated.Executor",str(age)],
    capture_output=True,
    text=True
)

output = proc.stdout.strip()

print(age, output)


with open("GeneratedTest.java","w") as f:

    f.write("""
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import in.ac.iiitb.plproject.atc.generated.Helper;

public class GeneratedTest {

    @Test
    void generated_test() {
""")

    f.write(f"""
        assertEquals("{output}", Helper.ageCategoryPrint({age}));
""")

    f.write("""
    }
}
""")

print("JUnit test written to GeneratedTest.java")