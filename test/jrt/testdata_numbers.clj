(ns jrt.testdata-numbers
  "jrt's differential test data for numbers (jrt.testdata): Double.toString and
  Float.toString, Math and StrictMath, the primitive conversions, Integer.toString and
  Long.toString. Inputs: special values and java.util.Random with fixed seeds. A double
  argument is its raw bits (16 hex digits), a float's 8; a double or float result is its
  bits with NaN canonical (doubleToLongBits, floatToIntBits), since NaN's payload is not
  portable (x86 and arm64 make different NaNs)."
  (:require [arbace.string :as str]
            [jrt.testdata-util :refer [esc result line write]])
  (:import [java.io File]
           [java.util Random]))

(defn hd [^double d] (format "%016x" (Double/doubleToLongBits d)))
(defn hf [^double f] (format "%08x" (Float/floatToIntBits (unchecked-float f))))
(defn rd [^double d] (format "%016x" (Double/doubleToRawLongBits d)))
(defn rf [f] (format "%08x" (Float/floatToRawIntBits (unchecked-float f))))

(def special-doubles
  (let [base [0.0 -0.0 1.0 -1.0 0.5 -0.5 1.5 -1.5 2.5 -2.5 0.49999999999999994 -0.49999999999999994
              Double/NaN Double/POSITIVE_INFINITY Double/NEGATIVE_INFINITY Double/MIN_VALUE
              (- Double/MIN_VALUE) Double/MAX_VALUE (- Double/MAX_VALUE) Double/MIN_NORMAL
              (/ Double/MIN_NORMAL 3) 2.0 3.0 10.0 100.0 1e7 9999999.999999998 1e-3 0.0009999999999999998
              1e21 1e22 1e23 1e-5 0.1 0.2 0.3 (/ 1.0 3) 2e-323 4.9e-324 9.007199254740992E15
              9.007199254740993E15 4.503599627370496E15 4.503599627370497E15 -4.503599627370497E15
              2147483647.0 2147483648.0 -2147483648.0 -2147483649.0 9.223372036854776E18 -9.223372036854776E18
              1.0E300 1.0E-300 123456.789 -0.0001 Math/PI Math/E 0.9999999999999999 1.0000000000000002
              1.7976931348623157E308 2.2250738585072009E-308 1.0E-323]]
    (vec (concat base (for [e (range -1074 1024 37)] (Math/scalb 1.0 (int e)))
                 (for [e (range -20 23)] (Math/pow 10.0 (double e)))))))

(defn rand-doubles
  "n doubles from seed: half arbitrary bit patterns, half values of moderate magnitude."
  [seed n]
  (let [r (Random. seed)]
    (vec (for [i (range n)]
           (if (even? i)
             (Double/longBitsToDouble (.nextLong r))
             (* (if (.nextBoolean r) 1.0 -1.0) (.nextDouble r) (Math/pow 10.0 (double (- (.nextInt r 40) 20)))))))))

(defn rand-floats [seed n]
  (let [r (Random. seed)]
    (vec (for [i (range n)]
           (if (even? i)
             (Float/intBitsToFloat (.nextInt r))
             (unchecked-float (* (if (.nextBoolean r) 1.0 -1.0) (.nextDouble r) (Math/pow 10.0 (double (- (.nextInt r 20) 10))))))))))

(def special-floats
  (vec (concat (map float [0.0 1.0 -1.0 0.5 1.5 2.5 -2.5 0.1 0.2 1e7 9999999.0 1e-3 Float/MAX_VALUE Float/MIN_NORMAL
                           Float/MIN_VALUE 16777216.0 16777217.0 2147483647.0 -2147483648.0 1e10 1e-10 123.456])
               [(unchecked-float -0.0) Float/NaN Float/POSITIVE_INFINITY Float/NEGATIVE_INFINITY (unchecked-float 0.49999997)]
               (for [e (range -149 128 7)] (Math/scalb (unchecked-float 1.0) (int e))))))

(def special-ints [0 1 -1 2 -2 7 -7 31 32 33 63 64 65 100 -100 Integer/MAX_VALUE Integer/MIN_VALUE
                   (dec Integer/MAX_VALUE) (inc Integer/MIN_VALUE) 46340 46341 65535 65536 -65536])
(def special-longs (concat special-ints [Long/MAX_VALUE Long/MIN_VALUE (dec Long/MAX_VALUE) (inc Long/MIN_VALUE)
                                         4294967296 -4294967296 3037000499 3037000500 9007199254740993]))

(defn rand-ints [seed n] (let [r (Random. seed)] (vec (repeatedly n #(.nextInt r)))))
(defn rand-longs [seed n] (let [r (Random. seed)] (vec (repeatedly n #(.nextLong r)))))

(defn drem
  "Java's double % (C's fmod): the exact remainder of truncating division, as BigDecimal
  computes it (fmod's result is always representable), signed as the dividend."
  [^double a ^double b]
  (cond
    (or (Double/isNaN a) (Double/isNaN b) (Double/isInfinite a) (== b 0.0)) Double/NaN
    (Double/isInfinite b) a
    :else (let [r (.doubleValue (.remainder (java.math.BigDecimal. a) (java.math.BigDecimal. b)))]
            (if (== r 0.0) (Math/copySign 0.0 a) r))))

(defn tostring-lines []
  (concat
    (for [d (concat special-doubles (rand-doubles 1 30000))]
      (line "Double.toString" (rd d) (esc (Double/toString d))))
    (for [f (concat special-floats (rand-floats 2 30000))]
      (line "Float.toString" (rf f) (esc (Float/toString (unchecked-float f)))))))

(defn double-lines []
  (let [ds (concat special-doubles (rand-doubles 3 3000))
        ds2 (rand-doubles 4 1500)
        pow-bases (concat [0.0 -0.0 1.0 -1.0 2.0 -2.0 0.5 -0.5 10.0 Double/NaN Double/POSITIVE_INFINITY
                           Double/NEGATIVE_INFINITY Double/MIN_VALUE 0.9999999 1.0000001 -3.0 1e300 1e-300]
                          (rand-doubles 5 120))
        pow-exps (concat [0.0 -0.0 1.0 -1.0 2.0 0.5 -0.5 3.0 -3.0 1e10 -1e10 0.3333333333333333 Double/NaN
                          Double/POSITIVE_INFINITY Double/NEGATIVE_INFINITY 1075.0 -1075.0 2147483648.0 9.007199254740992E15]
                         (rand-doubles 6 60))]
    (concat
      (for [d ds
            [op f] [["abs" #(hd (Math/abs ^double %))]
                    ["ceil" #(hd (Math/ceil ^double %))]
                    ["floor" #(hd (Math/floor ^double %))]
                    ["rint" #(hd (Math/rint ^double %))]
                    ["sqrt" #(hd (StrictMath/sqrt ^double %))]
                    ["Math.sqrt" #(hd (Math/sqrt ^double %))]
                    ["cbrt" #(hd (StrictMath/cbrt ^double %))]
                    ["Math.cbrt" #(hd (Math/cbrt ^double %))]
                    ["log" #(hd (StrictMath/log ^double %))]
                    ["Math.log" #(hd (Math/log ^double %))]
                    ["getExponent" #(str (Math/getExponent ^double %))]
                    ["nextUp" #(hd (Math/nextUp ^double %))]
                    ["nextDown" #(hd (Math/nextDown ^double %))]
                    ["ulp" #(hd (Math/ulp ^double %))]
                    ["signum" #(hd (Math/signum ^double %))]
                    ["round" #(str (Math/round ^double %))]
                    ["d2i" #(str (unchecked-int ^double %))]
                    ["d2l" #(str (unchecked-long ^double %))]
                    ["d2f" #(hf (unchecked-float %))]
                    ["scalb" #(hd (Math/scalb ^double % (int 1000)))]
                    ["scalb-1100" #(hd (Math/scalb ^double % (int -1100)))]
                    ["scalb-3" #(hd (Math/scalb ^double % (int -3)))]]]
        (line op (rd d) (result (f d))))
      (for [[a b] (map vector (concat special-doubles ds2) (concat (reverse special-doubles) (rand-doubles 7 1500)))
            [op f] [["max" #(hd (Math/max ^double %1 ^double %2))]
                    ["min" #(hd (Math/min ^double %1 ^double %2))]
                    ["copySign" #(hd (Math/copySign ^double %1 ^double %2))]
                    ["nextAfter" #(hd (Math/nextAfter ^double %1 ^double %2))]
                    ["rem" #(hd (drem %1 %2))]
                    ["fma" #(hd (Math/fma ^double %1 ^double %2 1.0))]]]
        (line op (rd a) (rd b) (result (f a b))))
      (for [x pow-bases y pow-exps
            [op f] [["pow" #(hd (StrictMath/pow ^double %1 ^double %2))]
                    ["Math.pow" #(hd (Math/pow ^double %1 ^double %2))]]]
        (line op (rd x) (rd y) (result (f x y))))
      (for [d (rand-doubles 8 3000)
            :let [y (Math/abs d)]
            [op f] [["pow1" #(hd (StrictMath/pow 1.0000001 ^double %))]
                    ["log" #(hd (StrictMath/log ^double %))]
                    ["Math.log" #(hd (Math/log ^double %))]]]
        (line op (rd y) (result (f y)))))))

(defn float-lines []
  (let [fs (concat special-floats (rand-floats 9 4000))]
    (concat
      (for [f fs
            [op g] [["fabs" #(hf (Math/abs (unchecked-float %)))]
                    ["fgetExponent" #(str (Math/getExponent (unchecked-float %)))]
                    ["fround" #(str (Math/round (unchecked-float %)))]
                    ["fnextUp" #(hf (Math/nextUp (unchecked-float %)))]
                    ["fulp" #(hf (Math/ulp (unchecked-float %)))]
                    ["fsignum" #(hf (Math/signum (unchecked-float %)))]
                    ["f2i" #(str (unchecked-int (unchecked-float %)))]
                    ["f2l" #(str (unchecked-long (unchecked-float %)))]
                    ["f2d" #(hd (double (unchecked-float %)))]
                    ["fscalb" #(hf (Math/scalb (unchecked-float %) (int 100)))]
                    ["fscalb-160" #(hf (Math/scalb (unchecked-float %) (int -160)))]]]
        (line op (rf f) (result (g f))))
      (for [[a b] (map vector fs (reverse fs))
            [op g] [["fmax" #(hf (Math/max (unchecked-float %1) (unchecked-float %2)))]
                    ["fmin" #(hf (Math/min (unchecked-float %1) (unchecked-float %2)))]
                    ["fcopySign" #(hf (Math/copySign (unchecked-float %1) (unchecked-float %2)))]
                    ["ffma" #(hf (Math/fma (unchecked-float %1) (unchecked-float %2) (unchecked-float 1.5)))]
                    ["fclamp" #(hf (Math/clamp (unchecked-float 0.25) (unchecked-float (Math/min (unchecked-float %1) (unchecked-float %2))) (unchecked-float (Math/max (unchecked-float %1) (unchecked-float %2)))))]]]
        (line op (rf a) (rf b) (result (g a b)))))))

(defmacro jcall [form] `(result ~form))

(defn int-lines []
  (let [is (concat special-ints (rand-ints 10 600))
        ls (concat special-longs (rand-longs 11 600))
        ipairs (for [a special-ints b special-ints] [a b])
        lpairs (for [a special-longs b special-longs] [a b])]
    (concat
      (for [i is] (line "Integer.toString" (str i) (esc (Integer/toString (int i)))))
      (for [l ls] (line "Long.toString" (str l) (esc (Long/toString (long l)))))
      (for [i is
            [op f] [["i2c" #(str (int (unchecked-char (int %))))]
                    ["i2s" #(str (unchecked-short (int %)))]
                    ["i2b" #(str (unchecked-byte (int %)))]
                    ["iabs" #(str (Math/abs (int %)))]
                    ["incrementExactI" #(str (Math/incrementExact (int %)))]
                    ["decrementExactI" #(str (Math/decrementExact (int %)))]
                    ["negateExactI" #(str (Math/negateExact (int %)))]
                    ["absExactI" #(str (Math/absExact (int %)))]
                    ["i2f" #(hf (unchecked-float (int %)))]
                    ["i2d" #(hd (double (int %)))]]]
        (line op (str i) (result (f i))))
      (for [l ls
            [op f] [["labs" #(str (Math/abs (long %)))]
                    ["l2i" #(str (unchecked-int (long %)))]
                    ["incrementExactJ" #(str (Math/incrementExact (long %)))]
                    ["decrementExactJ" #(str (Math/decrementExact (long %)))]
                    ["negateExactJ" #(str (Math/negateExact (long %)))]
                    ["absExactJ" #(str (Math/absExact (long %)))]
                    ["toIntExact" #(str (Math/toIntExact (long %)))]
                    ["l2f" #(hf (unchecked-float (long %)))]
                    ["l2d" #(hd (double (long %)))]]]
        (line op (str l) (result (f l))))
      (for [[a b] (concat ipairs (partition 2 (rand-ints 12 800)))
            [op f] [["addExactI" #(str (Math/addExact (int %1) (int %2)))]
                    ["subtractExactI" #(str (Math/subtractExact (int %1) (int %2)))]
                    ["multiplyExactI" #(str (Math/multiplyExact (int %1) (int %2)))]
                    ["floorDivI" #(str (Math/floorDiv (int %1) (int %2)))]
                    ["floorModI" #(str (Math/floorMod (int %1) (int %2)))]
                    ["ceilDivI" #(str (Math/ceilDiv (int %1) (int %2)))]
                    ["ceilModI" #(str (Math/ceilMod (int %1) (int %2)))]
                    ["maxI" #(str (Math/max (int %1) (int %2)))]
                    ["minI" #(str (Math/min (int %1) (int %2)))]
                    ["idiv" #(str (unchecked-divide-int (int %1) (int %2)))]
                    ["irem" #(str (unchecked-remainder-int (int %1) (int %2)))]]]
        (line op (str a) (str b) (result (f a b))))
      (for [[a b] (concat lpairs (partition 2 (rand-longs 13 800)))
            [op f] [["addExactJ" #(str (Math/addExact (long %1) (long %2)))]
                    ["subtractExactJ" #(str (Math/subtractExact (long %1) (long %2)))]
                    ["multiplyExactJ" #(str (Math/multiplyExact (long %1) (long %2)))]
                    ["multiplyExactJI" #(str (Math/multiplyExact (long %1) (unchecked-int %2)))]
                    ["floorDivJ" #(str (Math/floorDiv (long %1) (long %2)))]
                    ["floorModJ" #(str (Math/floorMod (long %1) (long %2)))]
                    ["floorModJI" #(str (Math/floorMod (long %1) (unchecked-int %2)))]
                    ["ceilDivJ" #(str (Math/ceilDiv (long %1) (long %2)))]
                    ["ceilModJ" #(str (Math/ceilMod (long %1) (long %2)))]
                    ["multiplyHigh" #(str (Math/multiplyHigh (long %1) (long %2)))]
                    ["unsignedMultiplyHigh" #(str (Math/unsignedMultiplyHigh (long %1) (long %2)))]
                    ["clampJII" #(str (Math/clamp (long %1) (unchecked-int (min %1 %2)) (unchecked-int (max %1 %2))))]
                    ["clampJJJ" #(str (Math/clamp (long 12345) (long (min %1 %2)) (long (max %1 %2))))]
                    ["clampJJJbad" #(str (Math/clamp (long 0) (long (max %1 %2)) (long (min %1 %2))))]
                    ["ldiv" #(str (quot (long %1) (long %2)))]
                    ["lrem" #(str (rem (long %1) (long %2)))]]]
        (line op (str a) (str b) (result (f a b)))))))

(defn write-all [^File dir]
  (write dir "tostring.txt" (tostring-lines))
  (write dir "doubles.txt" (double-lines))
  (write dir "floats.txt" (float-lines))
  (write dir "ints.txt" (int-lines)))
