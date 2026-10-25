package sample;

import java.util.function.*;

/** The printer's syntax (REAP5b, RPS60-RPS63): forms Java writes with parentheses, casts or names of its own. */
public class PrintSyntax<T> {
  interface Shout { String shout(); }
  interface A { void a(); }
  interface B { default void b() {} }

  static int g() { return 1; }

  class Inner {
    void m() {}
  }

  static <E extends Enum<E> & Supplier<String>> Function<E, String> getter() {
    return E::get;
  }

  static void take(Object o) {}

  static int compute(int n, String s) {
    int sum = 0;
    for (int i = 0; i < n; i++) (new byte[i + 1])[0] = 1;
    Function<int[], int[]> f = int[]::clone;
    Supplier<String[][]> s0 = new String[2][]::clone;
    int y = switch (n) { case 3 -> 1; default -> 2; } + 1;
    String z = (switch (n) { case 3 -> "a"; default -> "b"; }).trim();
    Integer o = ((Supplier<Integer>) () -> 3).get();
    int len = ((Function<String, Integer>) String::length).apply(s);
    Runnable r = ((A & B) () -> {})::b;
    take((Runnable) () -> {});
    Object[] fs = {"a", (Runnable) () -> {}};
    int x;
    int w = n > 0 ? (x = 1) : (x = 2);
    Shout sh = (switch (n) {
      case 1 -> new Shout() { public String shout() { return "one"; } };
      default -> (Shout) () -> "other";
    });
    String said = (switch (n) {
      case 1 -> new Shout() { public String shout() { return "one"; } };
      default -> (Shout) () -> "other";
    }).shout();
    Class<?> c = PrintSyntax.Inner.class;
    var anon = n > 5 ? new Object() {} : null;
    class Local {
      class Member { int k = 2; }
      Member make() { return new Member(); }
    }
    Local.Member mem = new Local().make();
    Object q = mem;
    {
      while (!(q instanceof Local.Member m)) {
      }
      sum += m.k;
    }
    if (q instanceof Local.Member m) {
      sum += m.k;
    }
    for (Integer v : (Iterable<Integer>) java.util.List.of(1, 2).stream()::iterator) {
      sum += v;
    }
    int \ud801\udc00abc = 1;
    int \ud802\udc00abc = 2;
    sum += \ud801\udc00abc + \ud802\udc00abc;
    return sum + y + z.length() + o + len + w + x + f.apply(new int[1]).length + s0.get().length + fs.length
        + c.getName().length() + (anon == null ? 0 : 1) + sh.shout().length() + said.length();
  }

  String which(int i, int j) {
    switch (i) {
      case 0:
        return switch (j) {
          case 0:
            if (j == 0) yield "0-0";
          default:
            yield "0-X";
        };
      default:
        return "X";
    }
  }

  static String tried(Supplier<String> sup) {
    String v;
    try {
      v = sup.get();
    } catch (RuntimeException e) {
      throw new IllegalStateException(e);
    }
    Supplier<String> again = () -> v;
    return again.get();
  }

  void anonymousThis() {
    new Runnable() {
      public void run() {
        new Runnable() {
          public void run() { touch(); }
        }.run();
      }
      void touch() {}
    }.run();
  }
}
