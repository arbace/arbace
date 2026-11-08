package suid;

import java.io.Serializable;
import java.util.*;
import java.util.function.*;

public class Sample implements Serializable {
    private int a;
    protected transient String b;
    public static final int K = 3;
    static List<String> L = new ArrayList<>();
    private static final long Z = System.nanoTime();
    public Sample() {}
    Sample(int a) { this.a = a; }
    private Sample(String s) {}
    public int get() { return a; }
    protected synchronized void sync() {}
    static void stat(int... xs) {}
    private void hidden() {}
    public final String fin(Object o, long[] q) { return null; }

    public class Inner implements Serializable {
        int x;
        public class Deeper implements Serializable { String y; }
    }
    public static class Nested implements Serializable, Comparable<Nested> {
        public int compareTo(Nested o) { return 0; }
    }
    static final class Holder implements Serializable {
        private static final Object O = new Object();
        void check(int x) { assert x > 0 : "x"; }
    }
    interface Marker extends Serializable { void m(); default void d() {} static void s() {} }
    interface Empty extends Serializable {}
    public abstract static class Abs implements Serializable, Supplier<String> {
        public abstract String get();
        native void nat();
    }
    record Rec(int p, String q) implements Serializable {}
    enum Color { RED, GREEN }
    static class Gen<T extends Comparable<T>> implements Serializable {
        T t;
        T pick(T u) { return u; }
    }
    static class GenSub extends Gen<String> {
        String pick(String u) { return u; }
    }

    public Object anon(final int k, String s) {
        Serializable r = new Serializable() {
            int w = k;
            public String toString() { return s + w; }
        };
        class Local implements Serializable {
            int v() { return k; }
        }
        return List.of(r, new Local());
    }
    public static Object anonStatic(final int k) {
        return new Serializable() { int w = k; };
    }

}
