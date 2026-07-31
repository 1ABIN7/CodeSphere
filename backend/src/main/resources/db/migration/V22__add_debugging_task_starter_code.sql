-- A debugging task must open with the faulty program the candidate is expected
-- to repair, rather than the generic coding template.
UPDATE problems
SET starter_code = jsonb_build_object('java', $code$
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = scanner.nextInt();
        int target = scanner.nextInt();
        System.out.println(firstIndex(values, target));
    }

    static int firstIndex(int[] values, int target) {
        int left = 0;
        int right = values.length - 1;
        while (left <= right) {
            int middle = left + (right - left) / 2;
            if (values[middle] == target) return middle;
            if (values[middle] < target) left = middle + 1;
            else right = middle - 1;
        }
        return -1;
    }
}
$code$)
WHERE title = 'Debug: First Target Index'
  AND (starter_code IS NULL OR starter_code = '{}'::jsonb);
