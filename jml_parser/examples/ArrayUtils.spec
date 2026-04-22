/*
 * Spec file for ArrayUtils
 * Contract definitions for array and string operations
 */

spec findMax {
    signature: int findMax(int[] arr);
    requires:  arr != null && arr.length > 0;
    ensures:   \result >= arr[0];
}

spec binarySearch {
    signature: int binarySearch(int[] arr, int target);
    requires:  arr != null && arr.length > 0;
    ensures:   (\result >= 0 && arr[\result] == target) ||
               (\result == -1);
}

spec reverse {
    signature: String reverse(String s);
    requires:  s != null;
    ensures:   \result != null && \result.length() == s.length();
}

spec clamp {
    signature: int clamp(int val, int lo, int hi);
    requires:  lo <= hi;
    ensures:   \result >= lo && \result <= hi;
}
