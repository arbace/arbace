(ns native.nan-test
  "Comparisons with NaN (doc/classes/SPEC.md §5.4): every ordered comparison and == are false,
  != is true, whichever way the compiler arranges the jump. The class forms compiler once chose
  dcmpg/dcmpl from the negated comparison, so (< 1.0 ##NaN) was true on the JVM Arbace (found by
  c2g's differential check, 2026-10-08)."
  (:require [arbace.test :refer :all]))

(defclass ^:public NanOps
  ;; jump on the negation (if c then else): the common shape
  (method ^:public ^:static lt ^boolean [^double a ^double b] (if (< a b) true false))
  (method ^:public ^:static le ^boolean [^double a ^double b] (if (<= a b) true false))
  (method ^:public ^:static gt ^boolean [^double a ^double b] (if (> a b) true false))
  (method ^:public ^:static ge ^boolean [^double a ^double b] (if (>= a b) true false))
  (method ^:public ^:static eq ^boolean [^double a ^double b] (if (== a b) true false))
  ;; jump on the comparison itself (if (not c) ...)
  (method ^:public ^:static nlt ^boolean [^double a ^double b] (if (not (< a b)) false true))
  (method ^:public ^:static nge ^boolean [^double a ^double b] (if (not (>= a b)) false true))
  ;; inside && and ||
  (method ^:public ^:static andLt ^boolean [^double a ^double b] (and (< a b) (< a b)))
  (method ^:public ^:static orGt ^boolean [^double a ^double b] (or (> a b) (> a b)))
  ;; floats
  (method ^:public ^:static flt ^boolean [^float a ^float b] (if (< a b) true false))
  (method ^:public ^:static fge ^boolean [^float a ^float b] (if (>= a b) true false)))

(def nan ##NaN)

(deftest class-body-comparisons
  (doseq [[a b] [[1.0 nan] [nan 1.0] [nan nan]]]
    (is (false? (NanOps/lt a b)))
    (is (false? (NanOps/le a b)))
    (is (false? (NanOps/gt a b)))
    (is (false? (NanOps/ge a b)))
    (is (false? (NanOps/eq a b)))
    (is (false? (NanOps/nlt a b)))
    (is (false? (NanOps/nge a b)))
    (is (false? (NanOps/andLt a b)))
    (is (false? (NanOps/orGt a b)))
    (is (false? (NanOps/flt (float a) (float b))))
    (is (false? (NanOps/fge (float a) (float b)))))
  (is (true? (NanOps/lt 1.0 2.0)))
  (is (true? (NanOps/ge 2.0 2.0))))

(deftest clojure-comparisons
  (doseq [[a b] [[1.0 nan] [nan 1.0] [nan nan] [1 nan] [nan 1N] [1/2 nan]]]
    (is (false? (< a b)))
    (is (false? (<= a b)))
    (is (false? (> a b)))
    (is (false? (>= a b)))
    (is (false? (== a b))))
  (is (false? (< 1.0 2.0 nan)))
  (is (= [false false false false] (mapv #(% 1.0 nan) [< <= > >=]))))
