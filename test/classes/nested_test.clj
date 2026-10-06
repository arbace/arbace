(ns classes.nested-test
  (:require [clojure.test :refer :all]
            [classes.helpers :refer :all])
  (:import (java.util Iterator ArrayList NoSuchElementException)))

(deftest nested-shapes
  (same-shapes? 'classes.nested-test
    {"Outer" "import java.util.*;
     public class Outer {
       int f = 5;
       private static int sp() { return 1; }
       public class Inner { public int get() { return f + sp(); } }
       class Unused { int g() { return 1; } }
       public static class Nested { public int get() { return 42; } }
       interface Cb { void call(); }
       private static final Runnable R = new Runnable() { public void run() { } };
       public Iterator<Integer> range(final int start, final int end) {
         return new Iterator<Integer>() {
           int i = start;
           public boolean hasNext() { return i < end; }
           public Integer next() {
             if (i < end) { int j = i; i = i + 1; return Integer.valueOf(j + f); }
             throw new NoSuchElementException();
           }
         };
       }
       Object locals(String s) {
         class Pair { Object b; Pair(Object a) { b = a; } public String toString() { return s.concat(String.valueOf(b)); } }
         return new Pair(\"z\");
       }
       static Object list(int n) { return new ArrayList<String>(n) { public int size() { return n; } }; }
       Inner mk() { return new Inner(); }
       Object nestedAnon(String x) {
         return new Object() { public String toString() { return new Object() { public String toString() { return x; } }.toString(); } };
       }
     }"}
    '[(^:public Outer
        (field ^int f 5)
        (method ^:private ^:static sp ^int [] 1)
        (defclass ^:public Inner (method ^:public get ^int [this] (unchecked-add-int f (Outer/sp))))
        (defclass Unused (method g ^int [this] 1))
        (defclass ^:public ^:static Nested (method ^:public get ^int [this] 42))
        (defclass ^:interface Cb (method call ^void [this]))
        (field ^:private ^:static ^:final ^Runnable R (anon Runnable [] (method ^:public run ^void [this])))
        (method ^:public range ^{:tag (Iterator Integer)} [this ^:final ^int start ^:final ^int end]
          (anon (Iterator Integer) []
            (field ^int i start)
            (method ^:public hasNext ^boolean [it] (< i end))
            (method ^:public next ^Integer [it]
              (when (< i end)
                (let [j i]
                  (set! i (unchecked-add-int i 1))
                  (return (Integer/valueOf (unchecked-add-int j f)))))
              (throw (NoSuchElementException.)))))
        (method locals [this ^String s]
          (letclass [(Pair (field b) (constructor [p a] (set! b a))
                       (method ^:public toString ^String [p] (.concat s (String/valueOf b))))]
            (Pair. "z")))
        (method ^:static list [^int n]
          (anon (ArrayList String) [n] (method ^:public size ^int [this] n)))
        (method mk ^Inner [this] (Inner.))
        (method nestedAnon [this ^String x]
          (anon Object []
            (method ^:public toString ^String [a]
              (.toString (anon Object [] (method ^:public toString ^String [b] x)))))))]
))

(deftest nested-behaviour
  (let [[O] (load-forms 'classes.nested-test
              '[(^:public Outer2
                  (field ^int f 5)
                  (defclass ^:public Inner
                    (field ^int g 1)
                    (defclass ^:public Deeper
                      (method ^:public get ^int [this] (unchecked-add-int f g)))
                    (method ^:public mk ^Deeper [this] (Deeper.)))
                  (method ^:public deeper ^int [this] (.get (.mk (Inner.))))
                  (method ^:public counter ^Runnable [this ^int/1 box]
                    (anon Runnable []
                      (method ^:public run ^void [r] (aset box 0 (unchecked-add-int (aget box 0) f)))))
                  (method ^:public ^:static ofStatic ^Runnable [^int/1 box ^int k]
                    (letclass [(Adder :implements [Runnable]
                                 (method ^:public run ^void [r] (aset box 0 (unchecked-add-int (aget box 0) k))))]
                      (anon Runnable []
                        (method ^:public run ^void [r] (.run (Adder.)) (.run (Adder.)))))))])
        o (.newInstance (.getConstructor O (into-array Class [])) (object-array 0))
        box (int-array [1])]
    (is (= 6 (.deeper o)))
    (.run (.counter o box))
    (is (= 6 (aget box 0)))
    (.run (clojure.lang.Reflector/invokeStaticMethod O "ofStatic" (object-array [box (int 10)])))
    (is (= 26 (aget box 0)))))

(deftest qualified-new-shape
  (same-shapes? 'classes.nested-test
    {"Q" "public class Q {
       int f = 1;
       public class In { public int g() { return f; } }
       static In make(Q q) { return q.new In(); }
       In self() { return this.new In(); }
     }"}
    '[(^:public Q
        (field ^int f 1)
        (defclass ^:public In (method ^:public g ^int [this] f))
        (method ^:static make ^In [^Q q] (.new q In))
        (method self ^In [this] (.new this In)))]))

(deftest anonymous-subclass-of-inner-class
  (same-shapes? 'classes.nested-test
    {"A" "public class A {
       class In { In(int x) {} }
       Object g() { return new In(2) { int y; }; }
     }"}
    '[(^:public A
        (defclass In (constructor [this ^int x]))
        (method g [this] (anon In [2] (field ^int y))))]))

(deftest outer-field-in-inner-constructor
  (same-shapes? 'classes.nested-test
    {"BA" "public class BA {
       java.util.Vector<Object> values = new java.util.Vector<>();
       class VE { java.util.Enumeration<Object> list; VE() { list = values.elements(); } }
       Object get() { return new VE(); }
     }"}
    '[(^:public BA
        (field ^{:tag (java.util.Vector Object)} values (java.util.Vector.))
        (defclass VE (field ^{:tag (java.util.Enumeration Object)} list) (constructor [this] (set! list (.elements values))))
        (method get [this] (VE.)))]))

(deftest qualified-super-constructor-call
  (same-shapes? 'classes.nested-test
    {"QS" "public class QS {
       class In { In(int x) {} }
       static class Sub extends QS.In { Sub(QS o) { o.super(5); } }
     }"}
    '[(^:public QS
        (defclass In (constructor [this ^int x]))
        (defclass ^:static Sub :extends In (constructor [this ^QS o] (.super o 5))))]))

(deftest local-records-enums-interfaces
  (same-shapes? 'classes.nested-test
    {"LR" "public class LR {
       Object f(int k) {
         record P(int a, int b) {}
         enum E { X, Y }
         interface I { int g(); }
         class C implements I { public int g() { return k; } }
         return new P(E.Y.ordinal(), new C().g());
       }
     }"}
    '[(^:public LR
        (method f [this ^int k]
          (letclass [(^:record P [^int a ^int b])
                     (^:enum E (constants X Y))
                     (^:interface I (method g ^int [this]))
                     (C :implements [I] (method ^:public g ^int [this] k))]
            (P. (.ordinal E/Y) (.g (C.))))))]))

(deftest anonymous-subclass-of-inner-class-using-outer
  (same-shapes? 'classes.nested-test
    {"A2" "public class A2 {
       int f = 1;
       class In { In(int x) {} int g() { return 0; } }
       Object g() { return new In(2) { int g() { return f; } }; }
     }"}
    '[(^:public A2
        (field ^int f 1)
        (defclass In (constructor [this ^int x]) (method g ^int [this] 0))
        (method g [this] (anon In [2] (method g ^int [a] f))))]))

(deftest anonymous-class-in-constructor
  (same-shapes? 'classes.nested-test
    {"AC" "public class AC {
       Object r;
       public AC() { super(); Object p = this; r = new Runnable() { public void run() { p.hashCode(); } }; }
     }"}
    '[(^:public AC
        (field r)
        (constructor ^:public [this]
          (super.)
          (let [^Object p this]
            (set! r (anon Runnable [] (method ^:public run ^void [a] (.hashCode p)))))))]))

(deftest inner-subclass-of-inner-class
  (same-shapes? 'classes.nested-test
    {"IS" "public class IS { class A { } class B extends A { } }"}
    '[(^:public IS (defclass A) (defclass B :extends A))]))
