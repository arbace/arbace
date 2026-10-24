/*
 * @test
 * @summary JLS 15.7: operands, arguments, array references and compound assignments evaluated
 *          left to right, each printed in the order it happens
 */
public class EvalOrder {
    static StringBuilder log = new StringBuilder();
    static int t(String s, int v) { log.append(s).append(' '); return v; }
    static int[] arr(String s, int[] a) { log.append(s).append(' '); return a; }
    static int f(int a, int b, int c) { return a * 100 + b * 10 + c; }
    static int field = 1;

    public static void main(String[] args) {
        int r = t("a", 1) + t("b", 2) * t("c", 3);
        System.out.println(r + ": " + log); log.setLength(0);

        r = f(t("x", 1), t("y", 2), t("z", 3));
        System.out.println(r + ": " + log); log.setLength(0);

        int[] a = {0, 0, 0};
        arr("array", a)[t("index", 1)] = t("value", 7);
        System.out.println(java.util.Arrays.toString(a) + ": " + log); log.setLength(0);

        int i = 1;
        i = i++ + ++i;
        System.out.println("i = " + i);
        i = 5;
        i += i++ * 2;
        System.out.println("i = " + i);
        int j = 10;
        j = j-- - --j;
        System.out.println("j = " + j);

        int[] b = {1, 2, 3};
        int k = 0;
        b[k] = k = 2;
        System.out.println(java.util.Arrays.toString(b) + " k = " + k);

        field = 1;
        field += (field = 10) * 2;
        System.out.println("field = " + field);

        // the left operand of && and || decides whether the right one runs
        boolean x = t("p", 0) == 1 && t("q", 1) == 1;
        boolean y = t("r", 1) == 1 || t("s", 1) == 1;
        System.out.println(x + " " + y + ": " + log); log.setLength(0);

        // the conditional evaluates one branch
        int c = t("cond", 1) > 0 ? t("then", 2) : t("else", 3);
        System.out.println(c + ": " + log); log.setLength(0);

        // an exception from an operand: the right operand never runs
        try {
            int z = t("before", 1) / (t("zero", 0)) + t("after", 1);
            System.out.println(z);
        } catch (ArithmeticException e) {
            System.out.println("ArithmeticException: " + e.getMessage() + ": " + log);
        }
        log.setLength(0);

        // the array reference is evaluated before the index; a null array throws after the index
        int[] none = null;
        try {
            arr("null array", none)[t("its index", 0)] = t("its value", 1);
        } catch (NullPointerException e) {
            System.out.println("NullPointerException: " + log);
        }
    }
}
