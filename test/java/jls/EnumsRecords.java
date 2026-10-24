/*
 * @test
 * @summary JLS 8.9, 8.10: enums with constructors, bodies and abstract methods, valueOf, values,
 *          EnumMap and EnumSet; records' accessors, canonical and compact constructors, equals,
 *          hashCode and toString, static members, nested records
 */
import java.util.*;

public class EnumsRecords {
    enum Planet {
        MERCURY(3.303e+23, 2.4397e6), EARTH(5.976e+24, 6.37814e6);
        final double mass, radius;
        Planet(double mass, double radius) { this.mass = mass; this.radius = radius; }
        double gravity() { return 6.67300E-11 * mass / (radius * radius); }
    }
    enum Op {
        PLUS("+") { int apply(int a, int b) { return a + b; } },
        TIMES("*") { int apply(int a, int b) { return a * b; } };
        final String sym;
        Op(String sym) { this.sym = sym; }
        abstract int apply(int a, int b);
    }
    record Point(int x, int y) {
        static final Point ORIGIN = new Point(0, 0);
        Point {
            if (x < 0) throw new IllegalArgumentException("negative x");
        }
        Point scale(int k) { return new Point(x * k, y * k); }
        static Point of(int v) { return new Point(v, v); }
    }
    record Named<T>(String name, T value) {
        Named(String name) { this(name, null); }
        @Override public String name() { return name.toUpperCase(); }
    }
    record Line(Point a, Point b) {}

    public static void main(String[] args) {
        for (Planet p : Planet.values()) System.out.printf("%s %d %.2f%n", p, p.ordinal(), p.gravity());
        System.out.println(Planet.valueOf("EARTH").name() + " " + Planet.EARTH.compareTo(Planet.MERCURY) + " " + Planet.EARTH.getDeclaringClass().getSimpleName());
        try { Planet.valueOf("PLUTO"); } catch (IllegalArgumentException e) { System.out.println("no PLUTO"); }
        for (Op o : Op.values()) System.out.print(o + o.sym + o.apply(6, 7) + " " + (o.getClass() == Op.class) + " " + (o.getDeclaringClass() == Op.class) + "; ");
        System.out.println();
        EnumMap<Op, Integer> em = new EnumMap<>(Op.class);
        em.put(Op.TIMES, 2); em.put(Op.PLUS, 1);
        System.out.println(em + " " + EnumSet.allOf(Planet.class) + " " + EnumSet.noneOf(Op.class));
        Point p = new Point(1, 2);
        System.out.println(p + " " + p.x() + " " + p.scale(3) + " " + Point.ORIGIN + " " + Point.of(4) + " " + p.equals(new Point(1, 2)) + " " + (p.hashCode() == new Point(1, 2).hashCode()));
        try { new Point(-1, 0); } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        Named<Integer> n = new Named<>("n", 5);
        System.out.println(n + " " + n.name() + " " + new Named<String>("bare") + " " + new Named<>("a", 1).equals(new Named<>("a", 1)));
        System.out.println(new Line(p, Point.ORIGIN));
        Set<Point> set = new HashSet<>(List.of(new Point(1, 1), new Point(1, 1).scale(1), Point.of(1)));
        System.out.println(set.size());
        switch (Planet.EARTH) { case EARTH: System.out.println("switch EARTH"); break; default: System.out.println("other"); }
    }
}
