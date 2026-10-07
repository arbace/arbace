(ns native.inline-sample
  "Compiled ahead of time by native.inline-test, which reads the instructions of its fns.")

(defn and-or-not ^long [^long a ^long b ^long c]
  (let [i (int a) j (int b) k (int c)]
    (long (bit-and-int (bit-or-int i j) (bit-xor-int (bit-not-int k) i)))))

(defn shifts ^long [^long a ^long n]
  (let [i (int a) s (int n)]
    (long (unchecked-add-int (bit-shift-left-int i s)
                             (unchecked-add-int (bit-shift-right-int i s) (unsigned-bit-shift-right-int i s))))))

(defn float-ops ^double [^double a ^double b]
  (let [x (float a) y (float b)]
    (double (unchecked-add-float
              (unchecked-subtract-float (unchecked-multiply-float x y) (unchecked-divide-float x y))
              (unchecked-remainder-float (unchecked-negate-float x) y)))))

(defn long-ops ^long [^long a ^long b] (unchecked-add (unchecked-divide a b) (unchecked-remainder a b)))

(defn double-ops ^double [^double a ^double b] (unchecked-add (unchecked-divide a b) (unchecked-remainder a b)))
