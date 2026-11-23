import java.util.*;
import java.util.function.*;

interface Shape { double area(); default String describe() { return name() + ":" + Math.round(area()); } String name(); static Shape unit() { return new Sq(1); } }
abstract class Base implements Shape { protected int id; static int count; Base() { id = ++count; } public String name() { return getClass().getSimpleName() + id; } }
class Sq extends Base { final double s; Sq(double s) { super(); this.s = s; } public double area() { return s * s; } }
class Circ extends Base { final double r; Circ(double r) { this.r = r; } public double area() { return Math.PI * r * r; } @Override public String describe() { return "circle " + super.describe(); } }
record Point(int x, int y) implements Comparable<Point> { Point { if (x < 0) throw new IllegalArgumentException("neg"); } public int compareTo(Point o) { return Integer.compare(x * x + y * y, o.x * o.x + o.y * o.y); } }
enum Color { RED, GREEN("g"), BLUE { String code() { return "bb"; } }; final String c; Color() { this("x"); } Color(String c) { this.c = c; } String code() { return c; } }
class MyEx extends Exception { final int code; MyEx(String m, int code) { super(m); this.code = code; } }
sealed interface Expr permits Num, Add {}
record Num(int v) implements Expr {}
record Add(Expr l, Expr r) implements Expr {}

public class T2 {
    static int eval(Expr e) { return switch (e) { case Num n -> n.v(); case Add(Expr l, Expr r) -> eval(l) + eval(r); }; }
    int inst = 7;
    class Inner { int get() { return inst * 2; } }
    static String sw(Object o) {
        return switch (o) { case null -> "null"; case Integer i when i > 10 -> "big" + i; case Integer i -> "int" + i; case String s -> "str" + s.length(); default -> "other"; };
    }
    static long fact(int n) { long r = 1; for (int i = 2; i <= n; i++) r *= i; return r; }
    static String fin() { StringBuilder sb = new StringBuilder(); try { try { throw new MyEx("boom", 42); } finally { sb.append("f1;"); } } catch (MyEx e) { sb.append(e.getMessage()).append(e.code).append(';'); } finally { sb.append("f2"); } return sb.toString(); }
    static int varargs(int... xs) { int s = 0; for (int x : xs) s += x; return s; }
    public static String main2() {
        StringBuilder out = new StringBuilder();
        List<Shape> shapes = new ArrayList<>(List.of(new Sq(2), new Circ(1), Shape.unit()));
        shapes.sort(Comparator.comparingDouble(Shape::area));
        for (Shape s : shapes) out.append(s.describe()).append(' ');
        Point[] ps = { new Point(3, 4), new Point(1, 1), new Point(0, 2) };
        Arrays.sort(ps);
        out.append(Arrays.toString(ps)).append(' ');
        out.append(ps[0].equals(new Point(1, 1))).append(ps[0].hashCode() == new Point(1,1).hashCode()).append(' ');
        try { new Point(-1, 0); } catch (IllegalArgumentException e) { out.append("iae ").append(e.getMessage()).append(' '); }
        for (Color c : Color.values()) out.append(c).append(c.ordinal()).append(c.code()).append(' ');
        out.append(Color.valueOf("GREEN").c).append(' ');
        out.append(eval(new Add(new Num(3), new Add(new Num(4), new Num(5))))).append(' ');
        out.append(new T2().new Inner().get()).append(' ');
        out.append(sw(null)).append(sw(5)).append(sw(50)).append(sw("abc")).append(sw(2.0)).append(' ');
        out.append(fact(20)).append(' ').append(Long.MAX_VALUE / 3).append(' ').append(fin()).append(' ').append(varargs(1, 2, 3)).append(' ');
        Map<String, Integer> m = new TreeMap<>();
        for (String w : "a b a c b a".split(" ")) m.merge(w, 1, Integer::sum);
        out.append(m).append(' ');
        Runnable r = () -> out.append("run ");
        r.run();
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        out.append(add.apply(2, 3)).append(' ');
        int[] arr = {3, 1, 2}; int[] cl = arr.clone(); cl[0] = 9; out.append(arr[0]).append(cl[0]).append(' ');
        Object o = new Sq(3); out.append(o instanceof Shape sh ? sh.name() : "no").append(' ');
        char ch = 'x'; ch += 2; out.append(ch).append((int) ch).append(' ');
        double d = 0.1 + 0.2; float f = 1.1f * 3; out.append(d).append(' ').append(f).append(' ').append(1.0 / 0).append(' ').append(Double.NaN > 1).append(' ');
        out.append(Integer.MIN_VALUE / -1).append(' ').append(-7 % 3).append(' ').append(-7 >> 1).append(' ').append(-7 >>> 28).append(' ').append(5L << 62).append(' ');
        Object lock = new Object(); synchronized (lock) { out.append("locked "); }
        out.append(new Object() { public String toString() { return "anon"; } });
        return out.toString();
    }
}
