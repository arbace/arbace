(ns classes.members-test
  (:require [arbace.test :refer :all]
            [classes.helpers :refer :all]))

(deftest fields-and-initializers
  (same-shapes? 'classes.members-test
    {"Fields" "public class Fields {
       public static final int K = 7;
       static final long L = -3L;
       private static final String S = \"s\" + K;
       static final double D = 1.5;
       static final char C = 'c';
       static final boolean B = true;
       static int counter;
       static final Object O = new Object();
       protected volatile int v;
       transient String t = \"t\";
       final int fin = 3;
       int[] arr = new int[K];
       static { counter = K * 2; }
       { v = fin + 1; }
       public Fields() {}
       Fields(int x) { this(); v = x; }
       Fields(String s, int... xs) { super(); t = s; }
       int k() { return K + fin; }
       static long l() { return L; }
     }"}
    '[(^:public Fields
        (field ^:public ^:static ^:final ^int K 7)
        (field ^:static ^:final ^long L -3)
        (field ^:private ^:static ^:final ^String S "s7")
        (field ^:static ^:final ^double D 1.5)
        (field ^:static ^:final ^char C \c)
        (field ^:static ^:final ^boolean B true)
        (field ^:static ^int counter)
        (field ^:static ^:final O (Object.))
        (field ^:protected ^:volatile ^int v)
        (field ^:transient ^String t "t")
        (field ^:final ^int fin 3)
        (field ^int/1 arr (new int/1 K))
        (static-initializer (set! counter (unchecked-multiply-int K 2)))
        (initializer (set! v (unchecked-add-int fin 1)))
        (constructor ^:public [this])
        (constructor [this ^int x] (this.) (set! v x))
        (constructor [this ^String s & ^int/1 xs] (super.) (set! t s))
        (method k ^int [this] (unchecked-add-int K fin))
        (method ^:static l ^long [] L))]))

(deftest methods-and-modifiers
  (same-shapes? 'classes.members-test
    {"Methods" "public abstract class Methods implements Runnable, Comparable<Object> {
       public abstract int size();
       protected static native long nat(int x);
       public synchronized void run() { }
       public int compareTo(Object o) { return 0; }
       @Deprecated public static String f(String a) { return a; }
       public static String f(Object a) { return \"o\"; }
       public static String f(int a, String... more) { return f(String.valueOf(a)); }
       static String g() throws java.io.IOException, InterruptedException { return f(1, \"a\", \"b\"); }
       private final void p() {}
       public static void main(String[] args) { new Thread(); }
     }"
     "Iface" "public interface Iface extends Runnable {
       int K = 1;
       String name();
       default String hello() { return name(); }
       static Iface of() { return null; }
       private int priv() { return K; }
     }"}
    '[(^:public ^:abstract Methods
        :implements [Runnable Comparable]
        (method ^:public ^:abstract size ^int [this])
        (method ^:protected ^:static ^:native nat ^long [^int x])
        (method ^:public ^:synchronized run ^void [this])
        (method ^:public compareTo ^int [this ^Object o] 0)
        (method ^:public ^:static ^{Deprecated true} f ^String [^String a] a)
        (method ^:public ^:static f ^String [^Object a] "o")
        (method ^:public ^:static f ^String [^int a & ^String/1 more] (Methods/f (String/valueOf a)))
        (method ^:static g :throws [java.io.IOException InterruptedException] ^String []
          (Methods/f 1 "a" "b"))
        (method ^:private ^:final p ^void [this])
        (method ^:public ^:static main ^void [^String/1 args] (Thread.)))
      (^:public ^:interface Iface
        :extends [Runnable]
        (field ^int K 1)
        (method name ^String [this])
        (method hello ^String [this] (.name this))
        (method ^:static of ^Iface [] nil)
        (method ^:private priv ^int [this] K))]
    :ignore [["classes/members_test/Methods" :signature]]))
