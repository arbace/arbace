(ns classes.forms-test
  (:require [clojure.test :refer :all]
            [classes.helpers :refer :all]))

(deftest strings-asserts-loops-resources
  (same-shapes? 'classes.forms-test
    {"S" "import java.util.*;
     public class S {
       static final int K = 3;
       static class In { void f(int x) { assert x > 0 : \"neg\"; assert x < 100; } }
       static String cat(String a, int b, char c, Object o, long l, double d, boolean z, Integer w) {
         return a + b + \"-\" + c + o + l + d + z + null + 1 + 'x' + 2.5 + w + K;
       }
       static final String CONST = \"k\" + K + true;
       static int sum(int[] xs) { int s = 0; for (int x : xs) s += x; return s; }
       static long sumL(int[] xs) { long s = 0; for (long x : xs) s += x; return s; }
       static int len(List<String> xs) { int s = 0; for (String x : xs) { if (x.isEmpty()) continue; s += x.length(); } return s; }
       static int ints(List<Integer> xs) { int s = 0; for (int x : xs) s += x; return s; }
       static String rd(java.io.Reader r) throws Exception {
         try (java.io.BufferedReader b = new java.io.BufferedReader(r); java.io.Reader q = r) { return b.readLine(); }
       }
     }"}
    '[(^:public S
        (field ^:static ^:final ^int K 3)
        (defclass ^:static In
          (method f ^void [this ^int x] (java-assert (> x 0) "neg") (java-assert (< x 100))))
        (method ^:static cat ^String [^String a ^int b ^char c ^Object o ^long l ^double d ^boolean z ^Integer w]
          (java-str a b "-" c o l d z nil 1 \x 2.5 w K))
        (field ^:static ^:final ^String CONST (java-str "k" K true))
        (method ^:static sum ^int [^int/1 xs]
          (let [^:mutable ^int s 0] (for-each [^int x xs] (set! s (unchecked-add-int s x))) s))
        (method ^:static sumL ^long [^int/1 xs]
          (let [^:mutable ^long s 0] (for-each [^long x xs] (set! s (unchecked-add s x))) s))
        (method ^:static len ^int [^{:tag (java.util.List String)} xs]
          (let [^:mutable ^int s 0]
            (for-each [^String x xs]
              (when (.isEmpty x) (continue))
              (set! s (unchecked-add-int s (.length x))))
            s))
        (method ^:static ints ^int [^{:tag (java.util.List Integer)} xs]
          (let [^:mutable ^int s 0] (for-each [^int x xs] (set! s (unchecked-add-int s x))) s))
        (method ^:static rd :throws [Exception] ^String [^java.io.Reader r]
          (with-resources [^java.io.BufferedReader b (java.io.BufferedReader. r)
                           ^java.io.Reader q r]
            (.readLine b))))]))

(deftest forms-behaviour
  (let [[C] (load-forms 'classes.forms-test
              '[(^:public F2
                  (method ^:public ^:static strs ^String [^int i ^Object o]
                    (java-str "i=" i ", o=" o ", c=" \c ", n=" nil))
                  (method ^:public ^:static firstNeg ^Object [^int/1 xs]
                    (label :L (for-each [^int x xs] (when (< x 0) (break :L (Integer/valueOf x))))))
                  (method ^:public ^:static closeOrder ^String [^StringBuilder sb]
                    (with-resources [^AutoCloseable a (F2/res sb "a") ^AutoCloseable b (F2/res sb "b")]
                      (.append sb "body;"))
                    (.toString sb))
                  (method ^:public ^:static suppressed ^int [^StringBuilder sb]
                    (try
                      (with-resources [^AutoCloseable a (F2/bad)]
                        (throw (IllegalStateException. "body")))
                      (catch IllegalStateException e (alength (.getSuppressed e)))))
                  (method ^:static res ^AutoCloseable [^StringBuilder sb ^String n]
                    (anon AutoCloseable [] (method ^:public close ^void [this] (.append sb (java-str "close " n ";")))))
                  (method ^:static bad ^AutoCloseable []
                    (anon AutoCloseable [] (method ^:public close ^void [this] (throw (RuntimeException. "close"))))))])
        call (fn [m & args] (clojure.lang.Reflector/invokeStaticMethod C (name m) (object-array args)))]
    (is (= "i=5, o=[1 2], c=c, n=null" (call 'strs (int 5) [1 2])))
    (is (= -2 (call 'firstNeg (int-array [1 -2 -3]))))
    (is (nil? (call 'firstNeg (int-array [1 2]))))
    (is (= "body;close b;close a;" (call 'closeOrder (StringBuilder.))))
    (is (= 1 (call 'suppressed (StringBuilder.))))))

(deftest lambda-shapes
  (same-shapes? 'classes.forms-test
    {"L" "import java.util.function.*;
     public class L {
       int f = 1;
       static Runnable S = () -> System.out.println();
       Supplier<Integer> I = () -> f;
       Object m(String x) {
         Function<String, Supplier<String>> g = a -> () -> a + x;
         Supplier<Object> h = () -> new Object() { public String toString() { return x; } };
         Function<String,Integer> r1 = Integer::valueOf;
         ToIntFunction<String> r2 = String::length;
         Supplier<String> r3 = x::trim;
         Supplier<L> r4 = L::new;
         IntFunction<int[]> r5 = n -> new int[n];
         Supplier<String> r6 = super::toString;
         Runnable r7 = (Runnable & Cloneable) () -> {};
         Predicate<String> p = s -> !s.isBlank();
         return g;
       }
     }"}
    '[(^:public L
        (field ^int f 1)
        (field ^:static ^Runnable S (lambda Runnable [] (.println System/out)))
        (field ^{:tag (java.util.function.Supplier Integer)} I
          (lambda java.util.function.Supplier ^Integer [] (Integer/valueOf f)))
        (method m [this ^String x]
          (let [g (lambda java.util.function.Function ^java.util.function.Supplier [^String a]
                    (lambda java.util.function.Supplier ^String [] (java-str a x)))
                h (lambda java.util.function.Supplier []
                    (anon Object [] (method ^:public toString ^String [o] x)))
                r1 (method-ref java.util.function.Function ^Integer [String] Integer/valueOf)
                r2 (method-ref java.util.function.ToIntFunction [String] String/.length)
                r3 (method-ref java.util.function.Supplier ^String [] x String/.trim)
                r4 (method-ref java.util.function.Supplier ^L [] L/new)
                r5 (lambda java.util.function.IntFunction ^int/1 [^int n] (new int/1 n))
                r6 (method-ref java.util.function.Supplier ^String [] super Object/.toString)
                r7 (lambda (& Runnable Cloneable) [])
                p (lambda java.util.function.Predicate ^boolean [^String s] (not (.isBlank s)))]
            g)))]))

(deftest lambda-behaviour
  (let [[C] (load-forms 'classes.forms-test
              '[(^:public L2
                  (field ^int base 10)
                  (method ^:public adder ^java.util.function.IntUnaryOperator [this ^int k]
                    (lambda java.util.function.IntUnaryOperator [^int x]
                      (when (< x 0) (return 0))
                      (unchecked-add-int (unchecked-add-int x k) base)))
                  (method ^:public ^:static sorted ^java.util.List [^java.util.List xs]
                    (let [l (java.util.ArrayList. xs)]
                      (.sort l (method-ref java.util.Comparator [String String] String/.compareTo))
                      l))
                  (method ^:public ^:static upper ^java.util.List [^java.util.List xs]
                    (let [l (java.util.ArrayList.)]
                      (.forEach xs (lambda java.util.function.Consumer [o] (.add l (.toUpperCase (cast String o)))))
                      l)))])
        o (.newInstance (.getConstructor C (into-array Class [])) (object-array 0))]
    (is (= 15 (.applyAsInt (.adder o 2) 3)))
    (is (= 0 (.applyAsInt (.adder o 2) -3)))
    (is (= ["a" "b" "c"] (vec (clojure.lang.Reflector/invokeStaticMethod C "sorted" (object-array [["c" "a" "b"]])))))
    (is (= ["X" "Y"] (vec (clojure.lang.Reflector/invokeStaticMethod C "upper" (object-array [["x" "y"]])))))))

(deftest varargs-method-refs
  (same-shapes? 'classes.forms-test
    {"V" "import java.util.function.*;
     public class V {
       static Object f() { BiFunction<String, Object, String> f = String::format; return f; }
     }"}
    '[(^:public V
        (method ^:static f []
          (let [f (method-ref java.util.function.BiFunction ^String [String Object] String/format)] f)))]))
