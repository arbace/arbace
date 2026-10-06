package sample;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.*;

/** Constructs of the Java language for the converter's tests: one of each tree kind. */
@SuppressWarnings("unchecked")
public class Features<T extends Number & Comparable<T>> implements Iterable<T>, Serializable {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface Todo {
    String value() default "";
    int priority() default 1;
    Class<?>[] types() default {};
    ElementType kind() default ElementType.TYPE;
  }

  enum Op {
    PLUS("+") { int apply(int a, int b) { return a + b; } },
    TIMES("*") { int apply(int a, int b) { return a * b; } };
    final String sym;
    Op(String sym) { this.sym = sym; }
    abstract int apply(int a, int b);
  }

  record Pair<A, B>(A first, B second) {}

  static final long serialVersionUID = 1L;
  static final int K = 0x7f;
  static int counter;
  private final List<T> items = new ArrayList<>();
  volatile transient int cache;
  static { counter = 1; }
  { cache = -1; }

  public Features() { super(); }
  Features(T first) { this(); items.add(first); }

  @Todo(value = "x", priority = 2, types = {String.class, int[].class}, kind = ElementType.METHOD)
  public Iterator<T> iterator() { return items.iterator(); }

  static int arithmetic(int a, long b, float c, double d, byte e, char ch, short sh) {
    int x = a + 1;
    x += e;
    x -= ch;
    x *= 3;
    x /= 2;
    x %= 7;
    x &= K;
    x |= 1 << 3;
    x ^= ~a;
    x <<= 2;
    x >>= 1;
    x >>>= 1;
    long y = b * a - (b >> 2) + (b >>> 3) + (long) d;
    float z = c * 2 + a;
    double w = d / c + (z < a ? 1 : 2);
    e++;
    ++ch;
    sh--;
    --x;
    boolean f = a < b && c >= d || !(e == 0) ^ (ch != 'a');
    f &= x > 0;
    f |= x <= 0;
    int[] arr = {1, 2, 3};
    arr[x & 1]++;
    arr[0] += arr[1]--;
    return (int) (x + y + (long) z + (long) w + e + ch + sh + (f ? 1 : 0) + -x + +x + arr[0]);
  }

  static String strings(Object o, int i, char c) {
    String s = "a" + o + i + c;
    s += 1.5f;
    String t = """
        text block
        """;
    return s + t + null + (i + c);
  }

  static int control(int[] xs, String key, Op op) {
    int total = 0;
    int i = 0;
    while (i < xs.length) {
      if (xs[i] < 0) { i++; continue; }
      total += xs[i++];
    }
    do { total--; } while (total > 100);
    for (int j = 0, n = xs.length; j < n; j++) total += j;
    for (int x : xs) { if (x == 42) break; total ^= x; }
    loop:
    for (;;) {
      switch (key) {
        case "a":
        case "b":
          total++;
        case "c":
          total += 2;
          break;
        case "d":
          break loop;
        default:
          total = -total;
      }
      if (total > 0) break;
    }
    switch (op) {
      case PLUS -> total = op.apply(total, 1);
      case TIMES -> total = op.apply(total, 2);
    }
    int r = switch (total % 3) {
      case 0 -> 10;
      case 1 -> { int q = total * 2; yield q; }
      default -> throw new IllegalStateException();
    };
    synchronized (xs) { total += r; }
    assert total != 0 : "zero";
    try {
      total = Integer.parseInt(key);
    } catch (NumberFormatException | NullPointerException e) {
      total = 0;
    } finally {
      total++;
    }
    return total;
  }

  static String patterns(Object o) {
    if (o instanceof String s && !s.isEmpty()) return s;
    if (!(o instanceof Integer n)) return "?";
    String desc = switch (o) {
      case Integer m when m > 10 -> "big";
      case Integer m -> "small " + m;
      default -> "other";
    };
    return desc + n;
  }

  static Object records(Object o) {
    if (o instanceof Pair(String a, var b)) return a + b;
    return switch (o) {
      case null -> "null";
      case Pair<?, ?>(Integer x, Integer y) -> x + y;
      case Pair<?, ?>(Long _, var unused) -> "long";
      case Pair<?, ?> p -> p.first();
      default -> o;
    };
  }

  <R> List<R> lambdas(Function<? super T, ? extends R> f) {
    List<R> out = new ArrayList<>();
    items.forEach(x -> out.add(f.apply(x)));
    Supplier<List<R>> sup = ArrayList::new;
    BiFunction<String, Integer, Character> at = String::charAt;
    Function<String, Integer> len = String::length;
    IntFunction<int[]> mk = int[]::new;
    Runnable r = (Runnable & Serializable) () -> counter++;
    Comparator<String> cmp = Comparator.comparing(String::length);
    UnaryOperator<String> up = s -> { if (s.isEmpty()) return s; return s.toUpperCase(); };
    Callable0 c = this::size;
    r.run();
    return out.isEmpty() ? sup.get() : out;
  }

  interface Callable0 { int call(); default int twice() { return call() * 2; } static int one() { return 1; } }

  int size() { return items.size(); }

  class Inner {
    int get() { return size() + Features.this.size() + counter; }
  }

  static class Nested extends Features<Integer> {
    @Override public int size() { return super.size() + 1; }
    Inner make() { return this.new Inner(); }
  }

  Object anonymousAndLocal(final int base) {
    class Local implements Supplier<Integer> {
      public Integer get() { return base + size(); }
    }
    Object a = new Object() {
      int n = base;
      @Override public String toString() { return "anon" + n + new Local().get(); }
    };
    return a;
  }

  static <E> E[] arrays(E[] xs, int n) {
    int[][] grid = new int[n][n];
    int[][] jag = new int[n][];
    String[] names = new String[] {"a", "b"};
    Object[] copy = xs.clone();
    grid[0][1] = jag.length + names.length + copy.length;
    return Arrays.copyOf(xs, grid[0][1]);
  }

  static Object casts(Object o, List<String> ls) {
    String s = (String) o;
    Comparable<String> c = (Comparable<String> & Serializable) s;
    int len = ls.get(0).length();
    Integer boxed = len;
    int unboxed = boxed;
    long widened = unboxed;
    Object prim = 1;
    return (Object) (c.compareTo("x") + widened + (prim instanceof Integer ? 1 : 0));
  }
}
