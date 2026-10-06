(ns classes.spec-examples-test
  "The worked examples of SPEC §11, compared with javac's classes."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [classes.helpers :refer :all]
            [arbace.classes.shape :as shape]))

(defmacro throw-arity-invokes
  "SPEC §4.15: Keyword's invoke methods of 4 to 20 arguments."
  [from to]
  (cons 'do
        (for [n (range from (inc to))]
          (list 'method (with-meta 'invoke {:public true})
                (into '[this] (map #(symbol (str "arg" %)) (range 1 (inc n))))
                (list '.throwArity 'this n)))))

(def keyword-form
  '(^:public clojure.lang.Keyword
    :implements [IFn Comparable Named java.io.Serializable IHashEq]

    (field ^:private ^:static ^:final ^long serialVersionUID -2105088845257724163)

    (field ^:private ^:static ^{:tag (java.util.concurrent.ConcurrentHashMap Symbol (java.lang.ref.Reference Keyword))} table
      (java.util.concurrent.ConcurrentHashMap.))
    (field ^:static ^:final ^java.lang.ref.ReferenceQueue rq (java.lang.ref.ReferenceQueue.))
    (field ^:public ^:final ^Symbol sym)
    (field ^:final ^int hasheq)
    (field ^:transient ^String _str)

    (method ^:public ^:static intern ^Keyword [^:mutable ^Symbol sym]
      (let [^:mutable ^Keyword k nil
            ^:mutable ^{:tag (java.lang.ref.Reference Keyword)} existingRef (cast java.lang.ref.Reference (.get table sym))]
        (when (nil? existingRef)
          (Util/clearCache rq table)
          (when (some? (.meta sym))
            (set! sym (cast Symbol (.withMeta sym nil))))
          (set! k (Keyword. sym))
          (set! existingRef (cast java.lang.ref.Reference (.putIfAbsent table sym (java.lang.ref.WeakReference. k rq)))))
        (if (nil? existingRef)
          k
          (let [existingk (cast Keyword (.get existingRef))]
            (if (some? existingk)
              existingk
              (do ;; entry died in the interim, do over
                (.remove table sym existingRef)
                (Keyword/intern sym)))))))

    (method ^:public ^:static intern ^Keyword [^String ns ^String name]
      (Keyword/intern (Symbol/intern ns name)))

    (method ^:public ^:static intern ^Keyword [^String nsname]
      (Keyword/intern (Symbol/intern nsname)))

    (constructor ^:private [this ^Symbol sym]
      (set! (.-sym this) sym)
      (set! hasheq (unchecked-add-int (.hasheq sym) (unchecked-int 0x9e3779b9))))

    (method ^:public ^:static find ^Keyword [^Symbol sym]
      (let [^{:tag (java.lang.ref.Reference Keyword)} ref (cast java.lang.ref.Reference (.get table sym))]
        (if (some? ref)
          (cast Keyword (.get ref))
          nil)))

    (method ^:public ^:static find ^Keyword [^String ns ^String name]
      (Keyword/find (Symbol/intern ns name)))

    (method ^:public ^:static find ^Keyword [^String nsname]
      (Keyword/find (Symbol/intern nsname)))

    (method ^:public ^:final hashCode ^int [this]
      (unchecked-add-int (.hashCode sym) (unchecked-int 0x9e3779b9)))

    (method ^:public hasheq ^int [this]
      hasheq)

    (method ^:public toString ^String [this]
      (when (nil? _str)
        (set! _str (java-str ":" sym)))
      _str)

    (method ^:public ^:deprecated throwArity [this]
      (throw (IllegalArgumentException.
               (java-str "Wrong number of args passed to keyword: " (.toString this)))))

    (method throwArity [this ^int n]
      (throw (ArityException. n (.toString this))))

    (method ^:public call [this]
      (.throwArity this 0))

    (method ^:public run ^void [this]
      (throw (UnsupportedOperationException.)))

    (method ^:public invoke [this]
      (.throwArity this 0))

    (method ^:public compareTo ^int [this o]
      (.compareTo sym (.-sym (cast Keyword o))))

    (method ^:public getNamespace ^String [this]
      (.getNamespace sym))

    (method ^:public getName ^String [this]
      (.getName sym))

    (method ^:private readResolve :throws [java.io.ObjectStreamException] [this]
      (Keyword/intern sym))

    (method ^:public ^:final invoke [this obj]
      (if (instance? ILookup obj)
        (.valAt (cast ILookup obj) this)
        (RT/get obj this)))

    (method ^:public ^:final invoke [this obj notFound]
      (if (instance? ILookup obj)
        (.valAt (cast ILookup obj) this notFound)
        (RT/get obj this notFound)))

    (method ^:public invoke [this arg1 arg2 arg3]
      (.throwArity this 3))

    (classes.spec-examples-test/throw-arity-invokes 4 20)

    (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                             arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20 & ^Object/1 args]
      (.throwArity this (unchecked-add-int 20 (alength args))))

    (method ^:public applyTo [this ^ISeq arglist]
      (AFn/applyToHelper this arglist))))

(deftest keyword-against-baseline
  (let [ours (get (forms-classes 'classes.spec-examples-test [keyword-form]) "clojure/lang/Keyword")
        javacs (with-open [in (io/input-stream (io/resource "clojure/lang/Keyword.class"))] (.readAllBytes in))
        ds (shape/diff (shape/shape javacs) (shape/shape ours))]
    (is (empty? ds) (with-out-str (doseq [d ds] (prn d))))))

(deftest newer-java
  (same-shapes? 'classes.spec-examples-test
    {"Shape" "public sealed interface Shape permits Circle, Square {}"
     "Circle" "public record Circle(double r) implements Shape {}"
     "Square" "public record Square(double side) implements Shape {
                 public Square { if (side < 0) throw new IllegalArgumentException(\"side\"); } }"
     "Shapes" "import java.util.*; import java.nio.file.*; import java.io.IOException;
     public final class Shapes {
       private Shapes() {}
       public static double area(Shape s) {
         return switch (s) {
           case Circle c -> Math.PI * c.r() * c.r();
           case Square(double side) -> side * side;
         };
       }
       public static <T extends Comparable<? super T>> T max(List<? extends T> xs) {
         T best = null;
         for (T x : xs)
           if (best == null || x.compareTo(best) > 0) best = x;
         return best;
       }
       static long nonBlank(Path p) throws IOException {
         try (var lines = Files.lines(p)) {
           return lines.filter(l -> !l.isBlank()).count();
         }
       }
       static int rowOf(int[][] grid, int v) {
         outer:
         for (int i = 0; i < grid.length; i++)
           for (int j = 0; j < grid[i].length; j++) {
             if (grid[i][j] < 0) continue outer;
             if (grid[i][j] == v) return i;
           }
         return -1;
       }
     }"}
    '[(^:public ^:sealed ^:interface Shape
        :permits [Circle Square])
      (^:public ^:record Circle [^double r]
        :implements [Shape])
      (^:public ^:record Square [^double side]
        :implements [Shape]
        (constructor ^:public ^:compact [this]
          (when (< side 0.0)
            (throw (IllegalArgumentException. "side")))))
      (^:public ^:final Shapes
        (constructor ^:private [this])
        (method ^:public ^:static area ^double [^Shape s]
          (switch s
            [^Circle c] (unchecked-multiply (unchecked-multiply Math/PI (.r c)) (.r c))
            [(Square ^double side)] (unchecked-multiply side side)
            (throw (MatchException. nil nil))))
        (method ^:public ^:static max
          :type-params [(T extends (Comparable (? super T)))]
          ^T [^{:tag (java.util.List (? extends T))} xs]
          (let [^:mutable ^T best nil]
            (for-each [^T x xs]
              (when (or (nil? best) (> (.compareTo x best) 0))
                (set! best x)))
            best))
        (method ^:static nonBlank :throws [java.io.IOException] ^long [^java.nio.file.Path p]
          (with-resources [^java.util.stream.Stream lines (java.nio.file.Files/lines p)]
            (.count (.filter lines (lambda java.util.function.Predicate ^boolean [^String l] (not (.isBlank l)))))))
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
          -1))]))
