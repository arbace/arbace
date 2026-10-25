/*
 * @test
 * @summary JLS 5.2, 15.29, 14.11, 15.13, 4.9, 8.1.3, 6.3: what the Java forms' analysis and
 *          repr/java's converter left open before JC5 (doc/classes/JAVA-CONFORMANCE-SURVEY.md,
 *          "As built: JC5"): constants narrowed then boxed, constant locals of other classes'
 *          constants, switches of patterns without default and qualified enum labels, switch
 *          expressions of int literals in overloads, references to generic and intersection-typed
 *          methods, arrays' clone(), fields of a type variable assigned in expressions,
 *          Outer.super's fields and methods, local classes created in their own bodies or
 *          extended by their member classes, pattern variables in scope after a guard and a loop,
 *          a top-level class extending a later one, more than nine array dimensions
 */
import java.util.function.*;

public class AnalysisGaps {
    interface A { default String a() { return "a"; } }
    interface B { default String b() { return "b"; } }
    static class AB implements A, B {}
    enum E { X, Y }
    static class Box<T> { T v; Box(T v) { this.v = v; } T get() { return v; } }
    static class Parent { protected int i = 10; String m() { return "parent"; } }

    static String id(int x) { return "int"; }
    static String id(Object x) { return "Object"; }
    static <T extends A & B> String both(T t) { return t.a() + t.b(); }
    static String which(Object o) {
        return switch (o) {
            case E.X -> "E.X";
            case String s when o instanceof String t && t.length() > 1 -> "long " + t;
            case String s -> "short " + s;
            default -> "other";
        };
    }

    class Inner extends Parent {
        String up() { return AnalysisGaps.this.toString() + " " + new Object() {
            String f() { return "" + Inner.super.i + Inner.super.m(); } }.f(); }
    }
    public String toString() { return "outer"; }

    public static void main(String[] args) {
        Byte b = 1; Character c = 'a'; Short s = 2;
        Integer[] ints = new Integer[2]; ints[0] = +43; ints[1] = -1;
        System.out.println(b + " " + c + " " + s + " " + ints[0] + ints[1]);
        final long big = Long.MAX_VALUE;
        final int range = Double.MAX_EXPONENT - Double.MIN_EXPONENT + 1;
        System.out.println(big + " " + range);
        int k = 0;
        System.out.println(id(switch (k) { case 0 -> 1; default -> 2; }) + " " + id(switch (k) { default -> { k++; yield 7; } }));
        System.out.println(which(E.X) + " " + which("ab") + " " + which("a") + " " + which(3));
        Object o = new AB();
        Supplier<String> sup = ((A & B) o)::b;
        System.out.println(sup.get() + " " + both(new AB()));
        int[] xs = {1, 2};
        UnaryOperator<int[]> cl = int[]::clone;
        System.out.println(cl.apply(xs).length);
        Box<Integer> box = new Box<>(5);
        int old = box.v++;
        Integer y; y = box.v = box.get() + 1;
        System.out.println(old + " " + box.v + " " + y);
        System.out.println(new AnalysisGaps().new Inner().up());
        StringBuilder sb = new StringBuilder();
        class Local {
            Local(boolean again) { sb.append("L"); if (again) { Runnable r = () -> new Local(false); r.run(); } }
            class Sub extends Local { Sub() { super(false); sb.append("S"); } }
        }
        new Local(true).new Sub();
        System.out.println(sb);
        Object p = "pattern";
        while (!(p instanceof String str)) { p = "x"; }
        System.out.println(str.length());
        Object[][][][][][][][][][] deep = new Object[1][1][1][1][1][1][1][1][1][1];
        System.out.println(deep.length + " " + new Later().name());
        String lit = "k" + 1;
        System.out.println(lit == "k1");
    }
}

class Later extends Base { String name() { return "later " + base(); } }

class Base { String base() { return "base"; } }
