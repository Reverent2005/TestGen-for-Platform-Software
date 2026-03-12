from z3 import *
from parse_spf import paths


def solve_one(paths):

    for path in paths:

        solver = Solver()
        variables = {}

        for var, op, val in path:

            if var not in variables:
                variables[var] = Int(var)

            v = variables[var]

            if op == "<=":
                solver.add(v <= val)
            elif op == "<":
                solver.add(v < val)
            elif op == ">=":
                solver.add(v >= val)
            elif op == ">":
                solver.add(v > val)
            elif op == "==":
                solver.add(v == val)
            elif op == "!=":
                solver.add(v != val)

        if solver.check() == sat:

            model = solver.model()
            result = {}

            for var in variables:
                result[var] = model[variables[var]].as_long()

            return result

    return None


if __name__ == "__main__":

    sol = solve_one(paths)

    print("Generated Input:")
    print(sol)