package suid;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public class Sample2 implements Serializable, Cloneable {
    static { System.getProperty("x"); }
    { hashCode(); }
    final Runnable r = () -> System.out.println("x");
    Supplier<Integer> s = this::hashCode;
    int sw(TimeUnit u) { switch (u) { case SECONDS: return 1; default: return 2; } }
    String sw2(Object o) { return switch (o) { case String x -> x; default -> "d"; }; }
    String cat(int i) { return "a" + i; }
    Sample2(String... xs) {}
    public Sample2 clone() { return this; }

    interface Konst extends Serializable { List<String> L = new ArrayList<>(); int C = 1; }
    interface WithAssert extends Serializable { default void a(int x) { assert x > 0; } }
    class In implements Serializable {
        Object deep(int z) {
            class Loc implements Serializable { int q = z; Object o = In.this; }
            return new Serializable() { int y = z; Object p = new Loc(); };
        }
    }
    class InSub extends In {}
    static class Covar implements Serializable, Comparable<Covar>, Iterable<String> {
        public int compareTo(Covar c) { return 0; }
        public Iterator<String> iterator() { return null; }
        protected Covar copy() { return this; }
        private static int priv;
        volatile long v;
        transient Object t;
        private transient Object pt;
        private static transient Object pst;
    }
    static class CovarSub extends Covar { @Override protected CovarSub copy() { return this; } }
    static class Ex extends Exception { Ex(String m) { super(m); } }
    static class G<K, V> extends HashMap<K, V> implements Serializable { public V get(Object k) { return null; } }
    static class Arr implements Serializable { int[][] a; String[] b; static final String S = "c"; static final Integer I = 3; }

    static Object[] objs() {
        Sample2 x = new Sample2();
        In in = x.new In();
        Object an = in.deep(1);
        return new Object[] { x, in, x.new InSub(), an, new Covar(), new CovarSub(), new Ex("m"), new G<String, String>(), new Arr(),
                              ((Object[]) new Object[]{ an })[0] };
    }
}
