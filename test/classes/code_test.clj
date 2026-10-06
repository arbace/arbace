(ns classes.code-test
  (:require [clojure.test :refer :all]
            [classes.helpers :refer :all]))

(deftest control-flow-shape
  (same-shapes? 'classes.code-test
    {"Flow" "public class Flow {
       static int rowOf(int[][] grid, int v) {
         outer:
         for (int i = 0; i < grid.length; i++)
           for (int j = 0; j < grid[i].length; j++) {
             if (grid[i][j] < 0) continue outer;
             if (grid[i][j] == v) return i;
           }
         return -1;
       }
       static long fact(int n) { long acc = 1; while (n > 1) { acc *= n; n--; } return acc; }
       static String tryIt(Object o) {
         try { return o.toString(); }
         catch (NullPointerException | ClassCastException e) { return \"npe\"; }
         finally { System.out.println(\"done\"); }
       }
       static int sync(Object lock, int[] a) { synchronized (lock) { return a[0]++; } }
       static Integer box(int i) { Integer x = i; long l = x; return x; }
       static double arith(int a, long b, float c, double d) {
         int i = (a * 3 + 7) / 2 % 5 - (a & 6 | 1 ^ a) + (a << 2) + (a >> 1) + (a >>> 3) + ~a + -a;
         long l = b * b + (b & 3L) + (b << 2) + (b >>> 1) - b / 3 + b % 7;
         float f = c * 2.5f + c / 3f - c % 2f;
         return i + l + f + d * d / 2 - d % 3;
       }
       static boolean cmp(int a, long b, double d, Object o) {
         return a < b && d >= 0.5 && o != null || o == null && !(a == 3);
       }
       static char chr(char c) { return (char) (c + 1); }
       static byte narrow(long l) { return (byte) (int) l; }
     }"}
    '[(^:public Flow
        (method ^:static rowOf ^int [^int/2 grid ^int v]
          (label :outer
            (loop [^int i 0]
              (when (< i (alength grid))
                (loop [^int j 0]
                  (when (< j (alength (aget grid i)))
                    (when (< (aget grid i j) 0)
                      (continue :outer (unchecked-inc-int i)))
                    (when (== (aget grid i j) v)
                      (return i))
                    (recur (unchecked-inc-int j))))
                (recur (unchecked-inc-int i)))))
          -1)
        (method ^:static fact ^long [^:mutable ^int n]
          (let [^:mutable ^long acc 1]
            (while (> n 1)
              (set! acc (unchecked-multiply acc n))
              (set! n (unchecked-dec-int n)))
            acc))
        (method ^:static tryIt ^String [^Object o]
          (try (.toString o)
            (catch [NullPointerException ClassCastException] e "npe")
            (finally (.println System/out "done"))))
        (method ^:static sync ^int [^Object lock ^int/1 a]
          (locking lock
            (let [x (aget a 0)] (aset a 0 (unchecked-inc-int x)) x)))
        (method ^:static box ^Integer [^int i]
          (let [^Integer x i ^long l x] x))
        (method ^:static arith ^double [^int a ^long b ^float c ^double d]
          (let [i (unchecked-add-int
                      (unchecked-add-int
                        (unchecked-add-int
                          (unchecked-add-int
                            (unchecked-add-int
                              (unchecked-subtract-int
                                (unchecked-remainder-int (unchecked-divide-int (unchecked-add-int (unchecked-multiply-int a 3) 7) 2) 5)
                                (bit-or-int (bit-and-int a 6) (bit-xor-int 1 a)))
                              (bit-shift-left-int a 2))
                            (bit-shift-right-int a 1))
                          (unsigned-bit-shift-right-int a 3))
                        (bit-not-int a))
                      (unchecked-negate-int a))
                l (unchecked-add
                    (unchecked-subtract
                      (unchecked-add (unchecked-add (unchecked-add (unchecked-multiply b b) (bit-and b 3))
                                                    (bit-shift-left b 2))
                                     (unsigned-bit-shift-right b 1))
                      (unchecked-divide b 3))
                    (unchecked-remainder b 7))
                f (unchecked-subtract-float
                    (unchecked-add-float (unchecked-multiply-float c (float 2.5)) (unchecked-divide-float c (float 3)))
                    (unchecked-remainder-float c (float 2)))]
            (unchecked-subtract
              (unchecked-add (unchecked-add (unchecked-add (unchecked-long i) l) (unchecked-double f))
                             (unchecked-divide (unchecked-multiply d d) 2.0))
              (unchecked-remainder d 3.0))))
        (method ^:static cmp ^boolean [^int a ^long b ^double d ^Object o]
          (or (and (< a b) (>= d 0.5) (some? o))
              (and (nil? o) (not (== a 3)))))
        (method ^:static chr ^char [^char c] (unchecked-char (unchecked-add-int c 1)))
        (method ^:static narrow ^byte [^long l] (unchecked-byte (unchecked-int l))))]))

(deftest control-flow-behaviour
  (let [[Flow] (load-forms 'classes.code-test
                 '[(^:public Flow2
                     (method ^:public ^:static loops ^long [^int n]
                       (let [^:mutable ^long s 0]
                         (dotimes [i n] (set! s (unchecked-add s i)))
                         (loop [^int i 0]
                           (when (< i n)
                             (when (== i 3) (break))
                             (set! s (unchecked-add s 100))
                             (recur (unchecked-inc-int i))))
                         s))
                     (method ^:public ^:static labeled ^int [^int x]
                       (unchecked-add-int 1 (label :L (when (> x 0) (break :L (unchecked-multiply-int x 10))) 5)))
                     (method ^:public ^:static fin ^String [^StringBuilder sb ^int n]
                       (loop [^int i 0]
                         (when (< i n)
                           (try
                             (when (== i 1) (continue (unchecked-inc-int i)))
                             (when (== i 3) (return (.toString sb)))
                             (.append sb i)
                             (finally (.append sb "f")))
                           (recur (unchecked-inc-int i))))
                       (.toString sb))
                     (method ^:public ^:static tryExpr ^int [^String s]
                       (unchecked-add-int 1 (try (Integer/parseInt s) (catch NumberFormatException e -1))))
                     (method ^:public ^:static truth ^String [o]
                       (if o "yes" "no")))])
        call (fn [m & args] (clojure.lang.Reflector/invokeStaticMethod Flow (name m) (object-array args)))]
    (is (= (+ 45 300) (call 'loops 10)))
    (is (= 31 (call 'labeled 3)))
    (is (= 6 (call 'labeled -3)))
    (is (= "0ff2f" (call (quote fin) (StringBuilder.) 3)))
    (is (= "0ff2f" (call (quote fin) (StringBuilder.) 5)))
    (is (= 43 (call 'tryExpr "42")))
    (is (= 0 (call 'tryExpr "x")))
    (is (= ["yes" "no" "no" "yes"] (map #(call 'truth %) [1 nil false true])))))

(defn twice [x] (* 2 x))

(deftest clojure-in-class-bodies
  (let [[C] (load-forms 'classes.code-test
              '[(^:public ClojureData
                  (method ^:public ^:static data [^int n]
                    {:n n :kw :foo :sym 'bar :vec [1 "two" n] :set #{:a} :twice (twice n)
                     :quoted '(1 (2 3) {:x [y]})}))])]
    (is (= {:n 3 :kw :foo :sym 'bar :vec [1 "two" 3] :set #{:a} :twice 6 :quoted '(1 (2 3) {:x [y]})}
           (clojure.lang.Reflector/invokeStaticMethod C "data" (object-array [(int 3)]))))))

(deftest reflection-escape
  (let [[C] (load-forms 'classes.code-test
              '[(^:public ^{:reflection :warn} Refl
                  (method ^:public ^:static len [o] (.length o)))])]
    (is (= 3 (clojure.lang.Reflector/invokeStaticMethod C "len" (object-array ["abc"]))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (load-forms 'classes.code-test '[(^:public NoRefl (method ^:public ^:static len [o] (.length o)))])))))

(deftest jumps-and-handlers-in-argument-positions
  (let [[C] (load-forms 'classes.code-test
              '[(^:public Spill
                  (method ^:public ^:static a ^int [^String s]
                    (Integer/sum 1 (try (Integer/parseInt s) (catch NumberFormatException e 0))))
                  (method ^:public ^:static b ^String [^boolean f]
                    (.toString (StringBuilder. (label :L (when f (break :L "yes")) "no"))))
                  (method ^:public ^:static c ^int/1 [^String s]
                    (new int/1 [1 (try (Integer/parseInt s) (catch NumberFormatException e -1)) 3]))
                  (method ^:public ^:static d ^String [^int n]
                    (java-str "n=" n ", sum="
                              (loop [^int i 0 ^int acc 0]
                                (if (< i n) (recur (unchecked-inc-int i) (unchecked-add-int acc i)) acc))
                              ", t=" (try (if (> n 3) (throw (RuntimeException.)) "ok") (catch RuntimeException e "caught"))))
                  (method ^:public ^:static e ^int [^int/1 xs]
                    (unchecked-add-int (aget xs 0)
                                       (label :out
                                         (for-each [^int x xs] (when (< x 0) (break :out x)))
                                         0))))])
        call (fn [m & args] (clojure.lang.Reflector/invokeStaticMethod C (name m) (object-array args)))]
    (is (= 3 (call 'a "2")))
    (is (= 1 (call 'a "x")))
    (is (= ["yes" "no"] [(call 'b true) (call 'b false)]))
    (is (= [1 5 3] (vec (call 'c "5"))))
    (is (= [1 -1 3] (vec (call 'c "?"))))
    (is (= "n=3, sum=3, t=ok" (call 'd (int 3))))
    (is (= "n=5, sum=10, t=caught" (call 'd (int 5))))
    (is (= -2 (call 'e (int-array [1 -3 2]))))
    (is (= 4 (call 'e (int-array [4 3]))))))
