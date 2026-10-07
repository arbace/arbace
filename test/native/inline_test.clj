(ns native.inline-test
  "The operators of doc/classes/SPEC.md §5.4 that arbace.core adds (bit-and-int ...
  unchecked-remainder) in ordinary code (§9.5): :inline expansions to arbace.lang.Numbers,
  emitted as instructions where the operands are primitive, Java's results on any operands, no
  reflection."
  (:require [arbace.test :refer :all]
            [arbace.java.io :as io]))

;; the same operators in a class body, where they are instructions (the reference)
(defclass ^:public JavaOps
  (method ^:public ^:static iand ^int [^int a ^int b] (bit-and-int a b))
  (method ^:public ^:static ior ^int [^int a ^int b] (bit-or-int a b))
  (method ^:public ^:static ixor ^int [^int a ^int b] (bit-xor-int a b))
  (method ^:public ^:static inot ^int [^int a] (bit-not-int a))
  (method ^:public ^:static ishl ^int [^int a ^int n] (bit-shift-left-int a n))
  (method ^:public ^:static ishr ^int [^int a ^int n] (bit-shift-right-int a n))
  (method ^:public ^:static iushr ^int [^int a ^int n] (unsigned-bit-shift-right-int a n))
  (method ^:public ^:static fadd ^float [^float a ^float b] (unchecked-add-float a b))
  (method ^:public ^:static fsub ^float [^float a ^float b] (unchecked-subtract-float a b))
  (method ^:public ^:static fmul ^float [^float a ^float b] (unchecked-multiply-float a b))
  (method ^:public ^:static fdiv ^float [^float a ^float b] (unchecked-divide-float a b))
  (method ^:public ^:static frem ^float [^float a ^float b] (unchecked-remainder-float a b))
  (method ^:public ^:static fneg ^float [^float a] (unchecked-negate-float a))
  (method ^:public ^:static ldiv ^long [^long a ^long b] (unchecked-divide a b))
  (method ^:public ^:static lrem ^long [^long a ^long b] (unchecked-remainder a b))
  (method ^:public ^:static ddiv ^double [^double a ^double b] (unchecked-divide a b))
  (method ^:public ^:static drem ^double [^double a ^double b] (unchecked-remainder a b)))

(def int-vals [0 1 -1 7 -7 31 32 33 0x7fffffff -0x80000000 0x12345678])
(def float-vals [(float 0) (float -0.0) (float 1.5) (float -2.25) (float 3) Float/MAX_VALUE
             Float/MIN_VALUE Float/POSITIVE_INFINITY Float/NaN])
(def long-vals [1 -1 7 -7 3 Long/MAX_VALUE Long/MIN_VALUE])
(def double-vals [0.0 -0.0 1.5 -2.25 7.0 1e300 Double/MIN_VALUE Double/NEGATIVE_INFINITY Double/NaN])

(defn- same? [x y]
  (or (= x y) (and (number? x) (number? y) (Double/isNaN (double x)) (Double/isNaN (double y)))))

(deftest primitive-operands
  ;; int locals: the fns of arbace.core and their inlined calls agree with the instructions
  (doseq [a int-vals b int-vals]
    (let [i (int a) j (int b)]
      (is (== (JavaOps/iand i j) (bit-and-int i j) (bit-and-int a b)))
      (is (== (JavaOps/ior i j) (bit-or-int i j) (bit-or-int a b)))
      (is (== (JavaOps/ixor i j) (bit-xor-int i j) (bit-xor-int a b)))
      (is (== (JavaOps/ishl i j) (bit-shift-left-int i j) (bit-shift-left-int a b)))
      (is (== (JavaOps/ishr i j) (bit-shift-right-int i j) (bit-shift-right-int a b)))
      (is (== (JavaOps/iushr i j) (unsigned-bit-shift-right-int i j) (unsigned-bit-shift-right-int a b)))))
  (doseq [a int-vals] (is (== (JavaOps/inot (int a)) (bit-not-int (int a)) (bit-not-int a))))
  (doseq [a float-vals b float-vals]
    (let [x (float a) y (float b)]
      (is (same? (JavaOps/fadd x y) (unchecked-add-float x y)))
      (is (same? (JavaOps/fsub x y) (unchecked-subtract-float x y)))
      (is (same? (JavaOps/fmul x y) (unchecked-multiply-float x y)))
      (is (same? (JavaOps/fdiv x y) (unchecked-divide-float x y)))
      (is (same? (JavaOps/frem x y) (unchecked-remainder-float x y)))
      (is (same? (JavaOps/fneg x) (unchecked-negate-float x)))))
  (doseq [a long-vals b long-vals]
    (let [x (long a) y (long b)]
      (is (== (JavaOps/ldiv x y) (unchecked-divide x y)))
      (is (== (JavaOps/lrem x y) (unchecked-remainder x y)))))
  (doseq [a double-vals b double-vals]
    (let [x (double a) y (double b)]
      (is (same? (JavaOps/ddiv x y) (unchecked-divide x y)))
      (is (same? (JavaOps/drem x y) (unchecked-remainder x y))))))

(deftest boxed-operands
  ;; Object operands (through a fn and apply): the same results, boxed as Clojure boxes them
  (let [call (fn [f & args] (apply f args))]
    (doseq [a int-vals b int-vals]
      (is (= (JavaOps/iand a b) (call bit-and-int a b)))
      (is (= (JavaOps/iushr a b) (call unsigned-bit-shift-right-int a b))))
    (is (instance? Integer (call bit-xor-int 1 2)))
    (doseq [a float-vals b float-vals]
      (is (same? (JavaOps/fadd a b) (call unchecked-add-float a b)))
      (is (same? (JavaOps/frem a b) (call unchecked-remainder-float a b))))
    (is (instance? Float (call unchecked-multiply-float 2 3)))
    ;; float overflow is Java's infinity, not an error
    (is (= Float/POSITIVE_INFINITY (call unchecked-add-float Float/MAX_VALUE Float/MAX_VALUE)))
    (doseq [a long-vals b long-vals]
      (is (= (JavaOps/ldiv a b) (call unchecked-divide a b)))
      (is (= (JavaOps/lrem a b) (call unchecked-remainder a b))))
    (doseq [a double-vals b double-vals]
      (is (same? (JavaOps/ddiv a b) (call unchecked-divide a b)))
      (is (same? (JavaOps/drem a b) (call unchecked-remainder a b))))
    ;; long or double by the operands, as Java's / and %
    (is (= [3 3.5 3.5 -1 1.5] [(call unchecked-divide 7 2) (call unchecked-divide 7.0 2)
                               (call unchecked-divide 7 (float 2)) (call unchecked-remainder -7 2)
                               (call unchecked-remainder 5.5 2)]))
    (is (instance? Long (call unchecked-divide (int 7) (int 2))))
    (is (= Long/MIN_VALUE (call unchecked-divide Long/MIN_VALUE -1)))
    (is (thrown? ArithmeticException (call unchecked-divide 1 0)))
    (is (thrown? ArithmeticException (call unchecked-remainder 1 0)))
    ;; operands are converted as by int: out of range is an error
    (is (thrown? ArithmeticException (call bit-and-int 0x100000000 1)))))

(deftest mixed-operands
  (let [o (Long. 12) d (Double. 2.5)]
    (let [i (int 10)]
      (is (= 8 (bit-and-int o i)))
      (is (= 14 (bit-or-int i o)))
      (is (= 48 (bit-shift-left-int o 2)))
      (is (= 12 (bit-and-int o 0xff) (bit-and-int 0xff 12))))
    (is (= [4 2.0 4.8 4.8 4.8 0] [(unchecked-divide o 3) (unchecked-remainder o d) (unchecked-divide o d)
                                (unchecked-divide 12 d) (unchecked-divide (long o) 2.5) (unchecked-remainder 12 o)]))
    (is (= (float 14.5) (unchecked-add-float o (float d))))
    (is (= (float 3.5) (unchecked-add-float 1 2.5)))))

(deftest no-reflection
  (let [err (java.io.StringWriter.)]
    (binding [*err* err *warn-on-reflection* true *ns* (the-ns 'native.inline-test)]
      (doseq [f '[(fn [x y] (bit-and-int x y)) (fn [x] (bit-not-int x)) (fn [x] (bit-shift-left-int x 3))
                  (fn [x y] (unchecked-add-float x y)) (fn [x] (unchecked-negate-float x))
                  (fn [x y] (unchecked-divide x y)) (fn [x] (unchecked-remainder x 2))
                  (fn [x] (unchecked-remainder 2.5 x)) (fn [^long x y] (unchecked-divide x y))
                  (fn [^double x y] (unchecked-remainder x y)) (fn [x ^long y] (unchecked-divide (int x) y))]]
        (eval f)))
    (is (= "" (str err)))))

(defclass ^:public ObjectOps
  ;; Object operands in a class body: the operators' :inline expansions, compiled as Clojure
  ;; compiles them (one method of the name and arity, the arguments converted by RT.intCast...)
  (method ^:public ^:static ops [x y]
    [(bit-and-int x y) (bit-shift-left-int x y) (bit-not-int x) (unchecked-add-float x y)
     (unchecked-divide x y) (unchecked-remainder x y) (unchecked-add-int x y)]))

(deftest object-operands-in-class-bodies
  (is (= [8 12288 -13 22.0 1 2 22] (ObjectOps/ops 12 10)))
  (is (= [4.5 1.0] (subvec (ObjectOps/ops 9.0 2) 4 6)))
  ;; and in a fn handed over to the class forms compiler (it uses switch), without reflection
  (let [err (java.io.StringWriter.)
        f (binding [*err* err *warn-on-reflection* true *ns* (the-ns 'native.inline-test)]
            (eval '(fn [x y] (switch (int 1) 1 [(bit-xor-int x y) (unchecked-multiply-float x y)
                                                (unchecked-remainder x y)] nil))))]
    (is (= "" (str err)))
    (is (= [6 15.0 2] (f 5 3)))))

(defn- opcodes
  "The instructions (opcodes, and [owner name] of method calls) of the static method `method` of
  class file `f`."
  [f method]
  (let [ops (atom [])
        mv (proxy [arbace.asm.MethodVisitor] [arbace.asm.Opcodes/ASM9]
             (visitInsn [op] (swap! ops conj op))
             (visitMethodInsn
               ([op owner nm desc] (swap! ops conj [owner nm]))
               ([op owner nm desc itf] (swap! ops conj [owner nm]))))
        cv (proxy [arbace.asm.ClassVisitor] [arbace.asm.Opcodes/ASM9]
             (visitMethod [acc nm desc sig exs] (when (= nm method) mv)))]
    (.accept (arbace.asm.ClassReader. ^bytes (java.nio.file.Files/readAllBytes (.toPath (io/file f)))) cv 0)
    @ops))

(deftest instructions
  (let [dir (io/file (System/getProperty "arbace.tmp" ".tmp") "native-tests" "inline")]
    (doseq [f (reverse (file-seq dir))] (.delete ^java.io.File f))
    (.mkdirs dir)
    (binding [*compile-path* (str dir)]
      (compile 'native.inline-sample))
    (let [ops (fn [fname] (opcodes (io/file dir "native" (str "inline_sample$" fname ".class")) "invokeStatic"))
          numbers-calls (fn [os] (filter #(and (vector? %) (= "arbace/lang/Numbers" (first %))) os))]
      (doseq [[fname expected] {"and_or_not" [arbace.asm.Opcodes/IAND arbace.asm.Opcodes/IOR arbace.asm.Opcodes/IXOR]
                                "shifts" [arbace.asm.Opcodes/ISHL arbace.asm.Opcodes/ISHR arbace.asm.Opcodes/IUSHR]
                                "float_ops" [arbace.asm.Opcodes/FADD arbace.asm.Opcodes/FSUB arbace.asm.Opcodes/FMUL
                                          arbace.asm.Opcodes/FDIV arbace.asm.Opcodes/FREM arbace.asm.Opcodes/FNEG]
                                "long_ops" [arbace.asm.Opcodes/LDIV arbace.asm.Opcodes/LREM]
                                "double_ops" [arbace.asm.Opcodes/DDIV arbace.asm.Opcodes/DREM]}]
        (let [os (ops fname)]
          (is (empty? (numbers-calls os)) [fname os])
          (doseq [op expected] (is (some #{op} os) [fname op os])))))
    (require 'native.inline-sample)
    (is (= (bit-and-int (bit-or-int 12 10) (bit-xor-int (bit-not-int 3) 12))
           ((resolve 'native.inline-sample/and-or-not) 12 10 3)))
    (is (= 4 ((resolve 'native.inline-sample/long-ops) 7 2)))
    (is (= 4.5 ((resolve 'native.inline-sample/double-ops) 7.0 2.0)))))
