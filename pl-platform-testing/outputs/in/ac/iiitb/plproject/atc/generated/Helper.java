package in.ac.iiitb.plproject.atc.generated;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

public class Helper {
    public static void appendExclamation(String s) {
        if (s != null) {
        }
    }

    public static void increment(int[] x) {
        if (x != null && x.length > 0) {
            x[0] = x[0] + 1;
        }
    }

    public static void process(Set<Integer> data, Map<Integer, Integer> result) {
        if (data != null && result != null) {
            for (Integer item : data) {
                result.put(item, item * 2);
            }
        }
    }

    public static Map<?,?> update(Map<Integer, Integer> result, Set<Integer> data) {
        if (result == null) {
            return new HashMap<>();
        }
        Map<Integer, Integer> updated = new HashMap<>(result);
        if (data != null) {
            for (Integer item : data) {
                updated.put(item, item * 2);
            }
        }
        return updated;
    }

    // New void function for age branching
    public static void ageCategoryPrint(int age) {
        if (age < 0) {
            System.out.println("Invalid age");
        } else if (age <= 5) {
            System.out.println("Toddler");
        } else if (age <= 12) {
            System.out.println("Kid");
        } else if (age <= 20) {
            System.out.println("Teenager");
        } else if (age <= 45) {
            System.out.println("Adult");
        } else if (age <= 65) {
            System.out.println("Midlife");
        } else {
            System.out.println("Senior citizen");
        }
    }
}
