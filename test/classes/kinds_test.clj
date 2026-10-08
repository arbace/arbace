(ns classes.kinds-test
  (:require [arbace.test :refer :all]
            [classes.helpers :refer :all]))

(deftest enum-shapes
  (same-shapes? 'classes.kinds-test
    {"Color" "public enum Color { RED, GREEN, BLUE }"
     "Op" "enum Op {
       PLUS(\"+\") { int apply(int a, int b) { return a + b; } },
       TIMES(\"*\") { int apply(int a, int b) { return a * b; } };
       private final String sym;
       Op(String sym) { this.sym = sym; }
       abstract int apply(int a, int b);
       String sym() { return sym; }
     }"
     "E" "public enum E {
       A, B(\"b\") { int f() { return 2; } }, C;
       private final String s;
       E() { this(\"x\"); }
       E(String s) { this.s = s; }
       int f() { return 1; }
       static E first() { return values()[0]; }
     }"
     "Holder" "public class Holder { public static enum Category { INTEGER, FLOATING } }"}
    '[(^:public ^:enum Color (constants RED GREEN BLUE))
      (^:enum Op
        (constants (PLUS ["+"] (method apply ^int [this ^int a ^int b] (unchecked-add-int a b)))
                   (TIMES ["*"] (method apply ^int [this ^int a ^int b] (unchecked-multiply-int a b))))
        (field ^:private ^:final ^String sym)
        (constructor [this ^String sym] (set! (.-sym this) sym))
        (method ^:abstract apply ^int [this ^int a ^int b])
        (method sym ^String [this] sym))
      (^:public ^:enum E
        (constants A (B ["b"] (method f ^int [this] 2)) C)
        (field ^:private ^:final ^String s)
        (constructor [this] (this. "x"))
        (constructor [this ^String s] (set! (.-s this) s))
        (method f ^int [this] 1)
        (method ^:static first ^E [] (aget (E/values) 0)))
      (^:public Holder
        (defclass ^:public ^:static ^:enum Category (constants INTEGER FLOATING)))]))

(deftest enum-behaviour
  (let [[Op] (load-forms 'classes.kinds-test
               '[(^:public ^:enum Op2
                   (constants (PLUS ["+"] (method ^:public apply ^int [this ^int a ^int b] (unchecked-add-int a b)))
                              (TIMES ["*"] (method ^:public apply ^int [this ^int a ^int b] (unchecked-multiply-int a b))))
                   (field ^:private ^:final ^String sym)
                   (constructor [this ^String sym] (set! (.-sym this) sym))
                   (method ^:public ^:abstract apply ^int [this ^int a ^int b])
                   (method ^:public sym ^String [this] sym))])
        vs (arbace.lang.Reflector/invokeStaticMethod Op "values" (object-array 0))
        times (arbace.lang.Reflector/invokeStaticMethod Op "valueOf" (object-array ["TIMES"]))]
    (is (= ["PLUS" "TIMES"] (map str vs)))
    (is (= 12 (.apply times 3 4)))
    (is (= "*" (.sym times)))
    (is (= 1 (.ordinal times)))
    (is (.isEnum Op))))

(deftest record-shapes
  (same-shapes? 'classes.kinds-test
    {"Point" "public record Point(int x, int y) {}"
     "Range" "public record Range(int lo, int hi) implements Comparable<Range> {
       public Range { if (lo > hi) throw new IllegalArgumentException(\"lo > hi\"); }
       public int compareTo(Range o) { return Integer.compare(lo, o.lo()); }
       public int hi() { return hi; }
       static Range of(int x) { return new Range(x, x); }
     }"
     "Pair" "record Pair<A, B>(A a, B b) { }"
     "Named" "record Named(String name, long[] data) {
       Named(String name) { this(name, new long[0]); }
     }"}
    '[(^:public ^:record Point [^int x ^int y])
      (^:public ^:record Range [^int lo ^int hi]
        :implements [(Comparable Range)]
        (constructor ^:public ^:compact [this]
          (when (> lo hi) (throw (IllegalArgumentException. "lo > hi"))))
        (method ^:public compareTo ^int [this ^Range o] (Integer/compare lo (.lo o)))
        (method ^:public hi ^int [this] hi)
        (method ^:static of ^Range [^int x] (Range. x x)))
      (^:record Pair [^A a ^B b] :type-params [A B])
      (^:record Named [^String name ^long/1 data]
        (constructor [this ^String name] (this. name (new long/1 0))))]
))

(deftest record-behaviour
  (let [[P] (load-forms 'classes.kinds-test
              '[(^:public ^:record P2 [^int x ^String s]
                  (constructor ^:public ^:compact [this] (when (nil? s) (set! s "none"))))])
        ctor (.getConstructor P (into-array Class [Integer/TYPE String]))
        a (.newInstance ctor (object-array [(int 1) nil]))
        b (.newInstance ctor (object-array [(int 1) "none"]))]
    (is (= "P2[x=1, s=none]" (str a)))
    (is (= a b))
    (is (= (.hashCode a) (.hashCode b)))
    (is (.isRecord P))
    (is (= ["x" "s"] (map #(.getName %) (.getRecordComponents P))))))

(deftest annotation-shapes
  (same-shapes? 'classes.kinds-test
    {"Todo" "import java.lang.annotation.*;
     @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
     public @interface Todo {
       String value();
       int priority() default 1;
       String[] tags() default {\"a\", \"b\"};
       ElementType kind() default ElementType.FIELD;
       Class<?> type() default String.class;
       Deprecated nested() default @Deprecated(since = \"9\");
     }"
     "Uses" "@Todo(value = \"cls\", priority = 2)
     @SuppressWarnings(\"unchecked\")
     public class Uses {
       @Deprecated public int f;
       @Todo(\"m\") @Deprecated public void m(@Todo(\"p\") int x, int y) {}
       @Todo(value = \"t\", tags = \"one\", kind = java.lang.annotation.ElementType.TYPE, type = int[].class) void t() {}
     }"}
    '[(^:public ^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME
                               java.lang.annotation.Target [java.lang.annotation.ElementType/METHOD
                                                            java.lang.annotation.ElementType/TYPE java.lang.annotation.ElementType/PARAMETER]}
       Todo
       (method value ^String [this])
       (method priority :default 1 ^int [this])
       (method tags :default ["a" "b"] ^String/1 [this])
       (method kind :default java.lang.annotation.ElementType/FIELD ^java.lang.annotation.ElementType [this])
       (method type :default String ^{:tag (Class ?)} [this])
       (method nested :default (Deprecated {:since "9"}) ^Deprecated [this]))
      (^:public ^{Todo {:value "cls" :priority 2} SuppressWarnings "unchecked"} Uses
        (field ^:public ^{Deprecated true} ^int f)
        (method ^:public ^{Todo "m" Deprecated true} m ^void [this ^{Todo "p"} ^int x ^int y])
        (method ^{Todo {:value "t" :tags "one" :kind java.lang.annotation.ElementType/TYPE :type int/1}} t ^void [this]))]))

(deftest generics-and-bridges
  (same-shapes? 'classes.kinds-test
    {"Box" "import java.util.*;
     public class Box<T extends Comparable<? super T>> implements Comparable<Box<T>>, java.util.function.Supplier<T> {
       T value;
       List<? extends T> items;
       Map<String, List<T>> index;
       public Box(T v) { value = v; }
       public int compareTo(Box<T> o) { return value.compareTo(o.value); }
       public T get() { return value; }
       public <U extends Number & Comparable<U>> U pick(List<U> xs) throws Exception { return xs.get(0); }
       static <K, V extends K> Map<K, V> empty() { return null; }
     }"
     "Base" "abstract class Base<T> { abstract T make(T x); Object cov() { return null; } }"
     "Impl" "class Impl extends Base<String> { String make(String s) { return s; } String cov() { return \"\"; } }"
     "Impl2" "class Impl2 extends Impl { String make(String s) { return s; } }"
     "Iter" "abstract class Iter<E> implements java.util.Iterator<E> { public E next() { return null; } }"}
    '[(^:public Box
        :type-params [(T extends (Comparable (? super T)))]
        :implements [(Comparable (Box T)) (java.util.function.Supplier T)]
        (field ^T value)
        (field ^{:tag (java.util.List (? extends T))} items)
        (field ^{:tag (java.util.Map String (java.util.List T))} index)
        (constructor ^:public [this ^T v] (set! value v))
        (method ^:public compareTo ^int [this ^{:tag (Box T)} o] (.compareTo value (.-value o)))
        (method ^:public get ^T [this] value)
        (method ^:public pick :type-params [(U extends Number (Comparable U))] :throws [Exception]
          ^U [this ^{:tag (java.util.List U)} xs] (cast Number (.get xs 0)))
        (method ^:static empty :type-params [K (V extends K)] ^{:tag (java.util.Map K V)} [] nil))
      (^:abstract Base :type-params [T]
        (method ^:abstract make ^T [this ^T x])
        (method cov ^Object [this] nil))
      (Impl :extends (Base String)
        (method make ^String [this ^String s] s)
        (method cov ^String [this] ""))
      (Impl2 :extends Impl
        (method make ^String [this ^String s] s))
      (^:abstract Iter :type-params [E] :implements [(java.util.Iterator E)]
        (method ^:public next ^E [this] nil))]))

(deftest sealed-shapes
  (same-shapes? 'classes.kinds-test
    {"Shape" "public sealed interface Shape permits Circle, Square {}"
     "Circle" "public record Circle(double r) implements Shape {}"
     "Square" "public non-sealed class Square implements Shape {}"
     "Node" "public abstract sealed class Node { static final class Leaf extends Node {} static final class Pair extends Node {} }"}
    '[(^:public ^:sealed ^:interface Shape :permits [Circle Square])
      (^:public ^:record Circle [^double r] :implements [Shape])
      (^:public ^:non-sealed Square :implements [Shape])
      (^:public ^:abstract ^:sealed Node
        (defclass ^:static ^:final Leaf :extends Node)
        (defclass ^:static ^:final Pair :extends Node))]))

(deftest interface-asserts-and-record-annotation-targets
  (same-shapes? 'classes.kinds-test
    {"I" "public interface I {
       default void f(int x) { assert x > 0; }
       static void g(int x) { assert x > 1 : \"g\"; }
     }"
     "AnnF" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD}) @interface AnnF {}"
     "AnnM" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD}) @interface AnnM {}"
     "AnnP" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PARAMETER}) @interface AnnP {}"
     "AnnAny" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @interface AnnAny {}"
     "AnnRC" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.RECORD_COMPONENT}) @interface AnnRC {}"
     "R" "record R(@AnnF @AnnM @AnnP @AnnAny @AnnRC int a) {}"}
    '[(^:public ^:interface I
        (method f ^void [this ^int x] (java-assert (> x 0)))
        (method ^:static g ^void [^int x] (java-assert (> x 1) "g")))
      (^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME
                      java.lang.annotation.Target [java.lang.annotation.ElementType/FIELD]} AnnF)
      (^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME
                      java.lang.annotation.Target [java.lang.annotation.ElementType/METHOD]} AnnM)
      (^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME
                      java.lang.annotation.Target [java.lang.annotation.ElementType/PARAMETER]} AnnP)
      (^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME} AnnAny)
      (^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME
                      java.lang.annotation.Target [java.lang.annotation.ElementType/RECORD_COMPONENT]} AnnRC)
      (^:record R [^{AnnF true AnnM true AnnP true AnnAny true AnnRC true} ^int a])]))

(deftest type-annotations-in-declarations
  (same-shapes? 'classes.kinds-test
    {"NN" "import java.lang.annotation.*; @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @Retention(RetentionPolicy.RUNTIME) @interface NN {}"
     "Both" "import java.lang.annotation.*; @Target({ElementType.TYPE_USE, ElementType.FIELD}) @Retention(RetentionPolicy.CLASS) @interface Both {}"
     "TA" "import java.util.*;
     class TA<@NN T extends @NN Comparable<T>> extends @NN Object implements @NN Comparable<TA<T>> {
       @NN String f;
       @Both String g;
       List<@NN String> l;
       String @NN [] arr;
       @NN String[] arr2;
       Map<? extends @NN Number, String> w;
       @NN String m(@NN int x, List<@NN T> y) throws @NN Exception { return null; }
       <@NN U> void n() {}
       public int compareTo(TA<T> o) { return 0; }
     }"}
    '[(^:annotation ^{java.lang.annotation.Target [java.lang.annotation.ElementType/TYPE_USE java.lang.annotation.ElementType/TYPE_PARAMETER]
                      java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME} NN)
      (^:annotation ^{java.lang.annotation.Target [java.lang.annotation.ElementType/TYPE_USE java.lang.annotation.ElementType/FIELD]
                      java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/CLASS} Both)
      (TA :type-params [(^{NN true} T extends ^{NN true} (Comparable T))]
          :extends ^{NN true} Object
          :implements [^{NN true} (Comparable (TA T))]
        (field ^{NN true} ^String f)
        (field ^{Both true} ^String g)
        (field ^{:tag (java.util.List ^{NN true} String)} l)
        (field ^{:tag ^{NN true} String/1} arr)
        (field ^{NN true} ^String/1 arr2)
        (field ^{:tag (java.util.Map (? extends ^{NN true} Number) String)} w)
        (method ^{NN true} m :throws [^{NN true} Exception] ^String [this ^{NN true} ^int x ^{:tag (java.util.List ^{NN true} T)} y] nil)
        (method n :type-params [^{NN true} U] ^void [this])
        (method ^:public compareTo ^int [this ^{:tag (TA T)} o] 0))]))

(deftest implicitly-parameterized-inner-classes
  (same-shapes? 'classes.kinds-test
    {"GO" "import java.util.*;
     public class GO<T> {
       class In { T x; }
       class In2<U> { U y; }
       In f;
       In2<String> g;
       In2<T> h(In a) { return null; }
       static class SN { }
       SN s;
       interface I { void m() throws java.io.IOException; }
       static class Impl implements I { public void m() { } }
     }"}
    '[(^:public GO :type-params [T]
        (defclass In (field ^T x))
        (defclass In2 :type-params [U] (field ^U y))
        (field ^In f)
        (field ^{:tag (In2 String)} g)
        (method h ^{:tag (In2 T)} [this ^In a] nil)
        (defclass ^:static SN)
        (field ^SN s)
        (defclass ^:interface I (method m :throws [java.io.IOException] ^void [this]))
        (defclass ^:static Impl :implements [I] (method ^:public m ^void [this])))]))

(deftest type-annotations-in-code
  (same-shapes? 'classes.kinds-test
    {"NC" "import java.lang.annotation.*; @Target({ElementType.TYPE_USE}) @Retention(RetentionPolicy.RUNTIME) @interface NC {}"
     "TC" "import java.util.*;
     class TC {
       Object f(Object o) {
         @NC String s = (@NC String) o;
         List<@NC String> l = new @NC ArrayList<>();
         boolean b = o instanceof @NC String;
         return s;
       }
     }"}
    '[(^:annotation ^{java.lang.annotation.Target [java.lang.annotation.ElementType/TYPE_USE]
                      java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME} NC)
      (TC
        (method f [this ^Object o]
          (let [^{NC true} ^String s (cast ^{NC true} String o)
                ^{:tag (java.util.List ^{NC true} String)} l (new ^{NC true} java.util.ArrayList)
                ^boolean b (instance? ^{NC true} String o)]
            s)))]))

(deftest catch-parameter-type-annotations
  (same-shapes? 'classes.kinds-test
    {"NC2" "import java.lang.annotation.*; @Target({ElementType.TYPE_USE}) @Retention(RetentionPolicy.RUNTIME) @interface NC2 {}"
     "TC2" "class TC2 { int f(String s) { try { return Integer.parseInt(s); } catch (@NC2 NumberFormatException e) { return -1; } } }"}
    '[(^:annotation ^{java.lang.annotation.Target [java.lang.annotation.ElementType/TYPE_USE]
                      java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME} NC2)
      (TC2
        (method f ^int [this ^String s]
          (try (Integer/parseInt s) (catch ^{NC2 true} NumberFormatException e -1))))]))

(deftest reflection-bridges
  (same-shapes? 'classes.kinds-test
    {"NPB" "abstract class NPB { public void f() {} public final void g() {} public abstract void h(); void p() {} public static void s() {} }"
     "PB" "public class PB extends NPB { public void h() {} }"}
    '[(^:abstract NPB
        (method ^:public f ^void [this]) (method ^:public ^:final g ^void [this])
        (method ^:public ^:abstract h ^void [this]) (method p ^void [this]) (method ^:public ^:static s ^void []))
      (^:public PB :extends NPB (method ^:public h ^void [this]))]))

(deftest bridge-annotations
  (same-shapes? 'classes.kinds-test
    {"BA1" "import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @interface BA1 {}"
     "BA2" "abstract class BA2 { abstract Object m(int x); }"
     "BA3" "class BA3 extends BA2 { @BA1 String m(@BA1 int x) { return null; } }"}
    '[(^:annotation ^{java.lang.annotation.Retention java.lang.annotation.RetentionPolicy/RUNTIME} BA1)
      (^:abstract BA2 (method ^:abstract m ^Object [this ^int x]))
      (BA3 :extends BA2 (method ^{BA1 true} m ^String [this ^{BA1 true} ^int x] nil))]))
