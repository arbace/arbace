(ns classes.pins-test
  "Param-tags on constructor calls (SPEC §4.7, §4.8, §4.10, §5.6), anonymous subclasses of inner
  classes with an explicit outer instance (§4.8), all-literal conditionals as operands (§5.4)."
  (:require [arbace.test :refer :all]
            [classes.helpers :refer :all]))

(deftest constructor-call-param-tags
  ;; integer literals are long: without the pins the compiler would choose the (long) overloads
  (same-shapes? 'classes.pins-test
    {"K" "public class K {
       K(int x) {}
       K(long x) {}
       K() { this(5); }
       class In { In(int x) {} In(long x) {} }
       static class Sub extends K { Sub() { super(6); } }
       static class Sub2 extends K.In { Sub2(K o) { o.super(7); } }
       static Object mk(K o) { return o.new In(8); }
       static Object an() { return new K(9) { }; }
       Object an2(K o) { return o.new In(10) { }; }
       enum E {
         A(1), B(2) { public String toString() { return \"b\"; } };
         E(int x) {}
         E(long x) {}
       }
     }"}
    '[(^:public K
        (constructor [this ^int x])
        (constructor [this ^long x])
        (constructor [this] (^[int] this. 5))
        (defclass In (constructor [this ^int x]) (constructor [this ^long x]))
        (defclass ^:static Sub :extends K (constructor [this] (^[int] super. 6)))
        (defclass ^:static Sub2 :extends In (constructor [this ^K o] (^[int] .super o 7)))
        (method ^:static mk [^K o] (^[int] .new o In 8))
        (method ^:static an [] (anon K ^[int] [9]))
        (method an2 [this ^K o] (anon In ^[int] [10] :outer o))
        (defclass ^:enum E
          (constants (A ^[int] [1])
                     (B ^[int] [2] (method ^:public toString ^String [this] "b")))
          (constructor [this ^int x])
          (constructor [this ^long x])))]))

(deftest constructor-calls-without-param-tags-choose-long
  (let [[c] (load-forms 'classes.pins-test
              '[(^:public KL
                  (field ^:public ^String which)
                  (constructor ^:public [this ^int x] (set! which "int"))
                  (constructor ^:public [this ^long x] (set! which "long"))
                  (constructor ^:public [this ^boolean pinned] (^[int] this. 1))
                  (constructor ^:public [this ^String unpinned] (this. 1))
                  (method ^:public ^:static anonWhich ^String [^boolean pinned]
                    (.-which (if pinned (anon KL ^[int] [1]) (anon KL [1])))))])]
    (is (= "long" (.-which (arbace.lang.Reflector/invokeConstructor c (object-array ["x"])))))
    (is (= "int" (.-which (arbace.lang.Reflector/invokeConstructor c (object-array [true])))))
    (is (= "long" (arbace.lang.Reflector/invokeStaticMethod c "anonWhich" (object-array [false]))))
    (is (= "int" (arbace.lang.Reflector/invokeStaticMethod c "anonWhich" (object-array [true]))))))

(deftest anonymous-subclass-with-outer
  (same-shapes? 'classes.pins-test
    {"PO" "public class PO {
       int f = 1;
       class In { In(int x) {} int g() { return 0; } }
       static Object s(PO o) { return o.new In(2) { int y; }; }
       Object t(PO o) { return o.new In(2) { int g() { return f; } }; }
       Object u() { return this.new In(2) { int g() { return 3; } }; }
       Object v(PO o) { return o.new In(2) { }; }
     }"
     "QO" "public class QO {
       int h = 4;
       Object w(PO o) { return o.new In(3) { int g() { return h; } }; }
       static Object x(PO o, int k) { return o.new In(k) { int g() { return k; } }; }
     }"}
    '[(^:public PO
        (field ^int f 1)
        (defclass In (constructor [this ^int x]) (method g ^int [this] 0))
        (method ^:static s [^PO o] (anon In [2] :outer o (field ^int y)))
        (method t [this ^PO o] (anon In [2] :outer o (method g ^int [a] f)))
        (method u [this] (anon In [2] :outer this (method g ^int [a] 3)))
        (method v [this ^PO o] (anon In [2] :outer o)))
      (^:public QO
        (field ^int h 4)
        (method w [this ^PO o] (anon PO$In [3] :outer o (method g ^int [a] h)))
        (method ^:static x [^PO o ^int k] (anon PO$In [k] :outer o (method g ^int [a] k))))]))

(deftest anonymous-subclass-with-outer-behaviour
  (let [[c] (load-forms 'classes.pins-test
              '[(^:public PB
                  (field ^:public ^int f)
                  (constructor ^:public [this ^int f] (set! (.-f this) f))
                  (defclass ^:public In
                    (field ^int k)
                    (constructor [this ^int k] (set! (.-k this) k))
                    (method ^:public g ^int [this] (unchecked-add-int k f)))
                  (method ^:public ^:static make ^In [^PB o ^int k]
                    (anon In [k] :outer o
                      (method ^:public g ^int [this] (unchecked-multiply-int 10 (.g super))))))])
        o (arbace.lang.Reflector/invokeConstructor c (object-array [(int 3)]))
        in (arbace.lang.Reflector/invokeStaticMethod c "make" (object-array [o (int 4)]))]
    (is (= 70 (arbace.lang.Reflector/invokeInstanceMethod in "g" (object-array 0))))
    (is (thrown? NullPointerException
                 (arbace.lang.Reflector/invokeStaticMethod c "make" (object-array [nil (int 4)]))))))

(deftest literal-conditionals-as-operands
  (same-shapes? 'classes.pins-test
    {"LC" "public class LC {
       static double d(double d, boolean z) { return d + (z ? 1 : 2); }
       static long l(long l, boolean z) { return l * (z ? 3 : -4); }
       static float f(float f, boolean z) { return f - (z ? 5 : 6); }
       static boolean c(double d, boolean z) { return d < (z ? 7 : 8); }
       static long big(long l, boolean z) { return l + (z ? 1L : 3000000000L); }
     }"}
    '[(^:public LC
        (method ^:static d ^double [^double d ^boolean z] (unchecked-add d (if z 1 2)))
        (method ^:static l ^long [^long l ^boolean z] (unchecked-multiply l (if z 3 -4)))
        (method ^:static f ^float [^float f ^boolean z] (unchecked-subtract-float f (if z 5 6)))
        (method ^:static c ^boolean [^double d ^boolean z] (< d (if z 7 8)))
        (method ^:static big ^long [^long l ^boolean z] (unchecked-add l (if z 1 3000000000))))]))
