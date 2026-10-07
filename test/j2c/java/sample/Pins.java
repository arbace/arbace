package sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Overloads where the class forms compiler's resolution (integer literals are long) differs
 * from javac's, so the converter must pin them, and calls where it agrees, so it must not;
 * constructor calls of every kind; an anonymous subclass of an inner class with an explicit
 * outer instance.
 */
public class Pins {
  final String which;

  Pins(int x) { which = "int"; }
  Pins(long x) { which = "long"; }
  Pins(Object x) { which = "Object"; }
  Pins() { this(5); }
  Pins(boolean b) { this(b ? 1 : 2); }
  Pins(String s, long l) { this(l); }

  class In {
    final int v;
    In(int x) { v = x; }
    In(long x) { v = -1; }
    int get() { return v; }
  }

  static class Sub extends Pins { Sub() { super(6); } }

  static class SubIn extends Pins.In {
    SubIn(Pins o) { o.super(7); }
  }

  enum E {
    A(1), B(2L), C(3) { int k() { return 30; } };
    final long n;
    E(int x) { n = x; }
    E(long x) { n = -x; }
    int k() { return 0; }
  }

  int field = 9;

  static In make(Pins o) { return o.new In(8); }

  static Pins anonymous() { return new Pins(9) { }; }

  In anonymousInner(Pins o) {
    return o.new In(10) {
      int get() { return super.get() + field; }
    };
  }

  static long literals(List<Integer> xs, StringBuilder sb, long l, char c) {
    xs.remove(0);
    xs.add(1);
    sb.append(5).append('c').append(c).append(l).append(2.5);
    long m = Math.max(l, 3);
    int k = Math.max(c, 4);
    return m + k + Math.abs(-1) + Long.valueOf(7) + Integer.valueOf(8);
  }

  static List<String> collections() {
    List<String> xs = new ArrayList<>(10);
    xs.add("a");
    xs.add(0, "b");
    return xs;
  }
}
