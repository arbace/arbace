/*
 * @test
 * @summary JLS 14.11, 15.28, 14.30: switch statements and expressions on ints, chars, strings and
 *          enums, fall through, yield, patterns in instanceof and switch, record patterns, guards
 */
public class Switches {
    enum Color { RED, GREEN, BLUE }
    sealed interface Shape permits Circle, Square, Rect {}
    record Circle(double r) implements Shape {}
    record Square(int side) implements Shape {}
    record Rect(int w, int h) implements Shape {}

    static String classic(int i) {
        StringBuilder sb = new StringBuilder();
        switch (i) {
            case 1: sb.append("one ");
            case 2: sb.append("two "); break;
            case 3: { sb.append("three "); break; }
            default: sb.append("other ");
            case 4: sb.append("four ");
        }
        return sb.toString().trim();
    }
    static int days(String m) {
        return switch (m) {
            case "feb" -> 28;
            case "apr", "jun", "sep", "nov" -> 30;
            default -> { int d = 31; yield d; }
        };
    }
    static String color(Color c) {
        return switch (c) { case RED -> "r"; case GREEN -> "g"; case BLUE -> "b"; };
    }
    static String describe(Object o) {
        return switch (o) {
            case null -> "null";
            case Integer i when i > 10 -> "big int " + i;
            case Integer i -> "int " + i;
            case String s -> "string of " + s.length();
            case int[] a -> "ints " + a.length;
            default -> "object " + o.getClass().getSimpleName();
        };
    }
    static double area(Shape s) {
        return switch (s) {
            case Circle c -> Math.PI * c.r() * c.r();
            case Square(int side) -> side * side;
            case Rect(int w, int h) when w == h -> -1;
            case Rect(var w, var h) -> w * h;
        };
    }
    public static void main(String[] args) {
        for (int i = 0; i <= 5; i++) System.out.print(classic(i) + "; ");
        System.out.println();
        System.out.println(days("feb") + " " + days("jun") + " " + days("jan"));
        for (Color c : Color.values()) System.out.print(color(c) + c.ordinal());
        System.out.println();
        char ch = 'b';
        switch (ch) { case 'a' -> System.out.println("a"); case 'b' -> System.out.println("b!"); default -> System.out.println("?"); }
        Object[] os = {null, 5, 50, "hello", new int[3], 2.5, Color.GREEN};
        for (Object o : os) System.out.println(describe(o));
        Shape[] shapes = {new Circle(1), new Square(3), new Rect(2, 2), new Rect(2, 5)};
        for (Shape s : shapes) System.out.printf("%.3f%n", area(s));
        Object o = "pattern";
        if (o instanceof String s && s.length() > 3) System.out.println("instanceof " + s);
        if (!(o instanceof Integer n)) System.out.println("not an Integer");
        else System.out.println(n + 1);
        System.out.println(new Rect(1, 2) + " " + new Rect(1, 2).equals(new Rect(1, 2)) + " " + (new Rect(1, 2).hashCode() == new Rect(1, 2).hashCode()));
        long big = 5;
        String which = big > 3 ? "gt" : "le";
        System.out.println(which);
    }
}
