import re

paths = []
current_constraints = []

constraint_pattern = re.compile(r'([a-zA-Z_]\w*)\s*(<=|>=|>|<|==|!=)\s*CONST_?(-?\d+)')

with open("spf_output.txt") as f:
    for line in f:

        line = line.strip()

        if "Testcaseconstraint" in line:
            if current_constraints:
                paths.append(current_constraints)
                current_constraints = []
            continue

        match = constraint_pattern.search(line)

        if match:
            var = match.group(1)
            op = match.group(2)
            val = int(match.group(3)) #limitation

            current_constraints.append((var, op, val))

if current_constraints:
    paths.append(current_constraints)

if __name__ == "__main__":
    print(paths)