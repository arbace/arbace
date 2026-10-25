/*
 * @test
 * @summary JLS 15.10.4, 15.17.2, 14.30, 8.10.3: what a compiler must leave to run time: an
 *          array access at a constant negative index, an integer division and remainder by a
 *          constant zero (and floating point's), patterns whose bindings or components the
 *          code does not read, a record without components, instanceof of the null literal
 *          (j2g's Go, doc/classes/JAVA-CONFORMANCE-SURVEY.md, JC2)
 */
public class RuntimeConstants {
    record Empty() {}
    record Pair(Object a, int b) {}
    sealed interface Opt permits Some, None {}
    record Some(Object v) implements Opt {}
    record None() implements Opt {}

    static String some(Opt o) {
        return switch (o) {
            case Some(Object v) -> "some";
            case None n -> "none";
        };
    }

    public static void main(String[] args) {
        int[] a = {1, 2, 3};
        try { System.out.println(a[-1]); } catch (ArrayIndexOutOfBoundsException e) { System.out.println("index: " + e.getMessage()); }
        try { a[-2] = 5; } catch (ArrayIndexOutOfBoundsException e) { System.out.println("store: " + e.getMessage()); }
        int x = a.length;
        try { System.out.println(x / 0); } catch (ArithmeticException e) { System.out.println("div: " + e.getMessage()); }
        try { System.out.println(x % 0); } catch (ArithmeticException e) { System.out.println("rem: " + e.getMessage()); }
        long y = x;
        try { System.out.println(y / 0L); } catch (ArithmeticException e) { System.out.println("ldiv: " + e.getMessage()); }
        double d = x;
        System.out.println("fdiv: " + (d / 0.0) + " " + (-d / 0.0) + " " + (0.0 / 0.0 + d));
        Object p = new Pair("p", 2);
        boolean isPair = p instanceof Pair(var c, var b);
        boolean firstIsString = p instanceof Pair(String s, int _);
        System.out.println("patterns: " + isPair + " " + firstIsString);
        System.out.println("empty: " + new Empty().equals(new Empty()) + " " + new Empty().equals(null) + " " + new Empty());
        System.out.println("switch: " + some(new Some(1)) + " " + some(new None()));
        System.out.println("null: " + (null instanceof String t ? t : "not a string"));
    }
}
