;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich Mar 31, 2008 */
;;
;; Converted from clojure/lang/Numbers.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.math MathContext))

(defclass ^:public Numbers
  (defclass ^:static ^:interface Ops
    (method combine ^Ops [this ^Ops y])

    (method opsWith ^Ops [this ^LongOps x])

    (method opsWith ^Ops [this ^DoubleOps x])

    (method opsWith ^Ops [this ^RatioOps x])

    (method opsWith ^Ops [this ^BigIntOps x])

    (method opsWith ^Ops [this ^BigDecimalOps x])

    (method ^:public isZero ^boolean [this ^Number x])

    (method ^:public isPos ^boolean [this ^Number x])

    (method ^:public isNeg ^boolean [this ^Number x])

    (method ^:public add ^Number [this ^Number x ^Number y])

    (method ^:public addP ^Number [this ^Number x ^Number y])

    (method ^:public unchecked_add ^Number [this ^Number x ^Number y])

    (method ^:public multiply ^Number [this ^Number x ^Number y])

    (method ^:public multiplyP ^Number [this ^Number x ^Number y])

    (method ^:public unchecked_multiply ^Number [this ^Number x ^Number y])

    (method ^:public divide ^Number [this ^Number x ^Number y])

    (method ^:public quotient ^Number [this ^Number x ^Number y])

    (method ^:public remainder ^Number [this ^Number x ^Number y])

    (method ^:public equiv ^boolean [this ^Number x ^Number y])

    (method ^:public lt ^boolean [this ^Number x ^Number y])

    (method ^:public lte ^boolean [this ^Number x ^Number y])

    (method ^:public gte ^boolean [this ^Number x ^Number y])

    (method ^:public negate ^Number [this ^Number x])

    (method ^:public negateP ^Number [this ^Number x])

    (method ^:public unchecked_negate ^Number [this ^Number x])

    (method ^:public inc ^Number [this ^Number x])

    (method ^:public incP ^Number [this ^Number x])

    (method ^:public unchecked_inc ^Number [this ^Number x])

    (method ^:public dec ^Number [this ^Number x])

    (method ^:public decP ^Number [this ^Number x])

    (method ^:public unchecked_dec ^Number [this ^Number x])

    (method ^:public abs ^Number [this ^Number x]))

  (defclass ^:abstract ^:static OpsP
    :implements [Ops]

    (method ^:public addP ^Number [this ^Number x ^Number y]
      (.add this x y))

    (method ^:public unchecked_add ^Number [this ^Number x ^Number y]
      (.add this x y))

    (method ^:public multiplyP ^Number [this ^Number x ^Number y]
      (.multiply this x y))

    (method ^:public unchecked_multiply ^Number [this ^Number x ^Number y]
      (.multiply this x y))

    (method ^:public negateP ^Number [this ^Number x] (.negate this x))

    (method ^:public unchecked_negate ^Number [this ^Number x]
      (.negate this x))

    (method ^:public incP ^Number [this ^Number x] (.inc this x))

    (method ^:public unchecked_inc ^Number [this ^Number x] (.inc this x))

    (method ^:public decP ^Number [this ^Number x] (.dec this x))

    (method ^:public unchecked_dec ^Number [this ^Number x] (.dec this x)))

  (method ^:public ^:static isZero ^boolean [x]
    (.isZero (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static isPos ^boolean [x]
    (.isPos (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static isNeg ^boolean [x]
    (.isNeg (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static minus ^Number [x]
    (.negate (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static minusP ^Number [x]
    (.negateP (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static inc ^Number [x]
    (.inc (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static incP ^Number [x]
    (.incP (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static dec ^Number [x]
    (.dec (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static decP ^Number [x]
    (.decP (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static add ^Number [x y]
    (.add (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static addP ^Number [x y]
    (.addP (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static minus ^Number [x y]
    (let [yops (Numbers/ops y)]
      (.add (.combine (Numbers/ops x) yops) (cast Number x) (.negate yops (cast Number y)))))

  (method ^:public ^:static minusP ^Number [x y]
    (let [yops (Numbers/ops y)
          negativeY (.negateP yops (cast Number y))
          negativeYOps (Numbers/ops negativeY)]
      (.addP (.combine (Numbers/ops x) negativeYOps) (cast Number x) negativeY)))

  (method ^:public ^:static multiply ^Number [x y]
    (.multiply (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static multiplyP ^Number [x y]
    (.multiplyP (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static divide ^Number [x y]
    (cond
      (Numbers/isNaN x) (cast Number x)
      (Numbers/isNaN y) (cast Number y)
      :else
        (let [yops (Numbers/ops y)]
          (when (.isZero yops (cast Number y)) (throw (ArithmeticException. "Divide by zero")))
          (.divide (.combine (Numbers/ops x) yops) (cast Number x) (cast Number y)))))

  (method ^:public ^:static quotient ^Number [x y]
    (let [yops (Numbers/ops y)]
      (when (.isZero yops (cast Number y)) (throw (ArithmeticException. "Divide by zero")))
      (.quotient (.combine (Numbers/ops x) yops) (cast Number x) (cast Number y))))

  (method ^:public ^:static remainder ^Number [x y]
    (let [yops (Numbers/ops y)]
      (when (.isZero yops (cast Number y)) (throw (ArithmeticException. "Divide by zero")))
      (.remainder (.combine (Numbers/ops x) yops) (cast Number x) (cast Number y))))

  (method ^:public ^:static quotient ^double [^double n ^double d]
    (when (== d 0.0) (throw (ArithmeticException. "Divide by zero")))
    (let [q (unchecked-divide n d)]
      (if (and (<= q Long/MAX_VALUE) (>= q Long/MIN_VALUE))
          (double (unchecked-long q))
          (.doubleValue (.toBigInteger (BigDecimal. q))))))

  (method ^:public ^:static remainder ^double [^double n ^double d]
    (when (== d 0.0) (throw (ArithmeticException. "Divide by zero")))
    (let [q (unchecked-divide n d)]
      (if (and (<= q Long/MAX_VALUE) (>= q Long/MIN_VALUE))
          (unchecked-subtract n (unchecked-multiply (unchecked-long q) d))
          (let [^Number bq (.toBigInteger (BigDecimal. q))]
            (unchecked-subtract n (unchecked-multiply (.doubleValue bq) d))))))

  (method ^:public ^:static equiv ^boolean [x y]
    (Numbers/equiv (cast Number x) (cast Number y)))

  (method ^:public ^:static equiv ^boolean [^Number x ^Number y]
    (.equiv (.combine (Numbers/ops x) (Numbers/ops y)) x y))

  (method ^:public ^:static equal ^boolean [^Number x ^Number y]
    (and (identical? (Numbers/category x) (Numbers/category y))
         (.equiv (.combine (Numbers/ops x) (Numbers/ops y)) x y)))

  (method ^:public ^:static lt ^boolean [x y]
    (.lt (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static lte ^boolean [x y]
    (.lte (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static gt ^boolean [x y]
    (.lt (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number y) (cast Number x)))

  (method ^:public ^:static gte ^boolean [x y]
    (.gte (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static compare ^int [^Number x ^Number y]
    (let [ops (.combine (Numbers/ops x) (Numbers/ops y))]
      (cond (.lt ops x y) -1 (.lt ops y x) 1 :else 0)))

  (method ^:static ^{WarnBoxedMath false} toBigInt ^BigInt [x]
    (cond
      (instance? BigInt x) (cast BigInt x)
      (instance? BigInteger x) (BigInt/fromBigInteger (cast BigInteger x))
      :else (BigInt/fromLong (.longValue (cast Number x)))))

  (method ^:static ^{WarnBoxedMath false} toBigInteger ^BigInteger [x]
    (cond
      (instance? BigInteger x) (cast BigInteger x)
      (instance? BigInt x) (.toBigInteger (cast BigInt x))
      :else (BigInteger/valueOf (.longValue (cast Number x)))))

  (method ^:static ^{WarnBoxedMath false} toBigDecimal ^BigDecimal [x]
    (cond
      (instance? BigDecimal x) (cast BigDecimal x)
      (instance? BigInt x)
        (let [bi (cast BigInt x)]
          (if (nil? (.-bipart bi)) (BigDecimal/valueOf (.-lpart bi)) (BigDecimal. (.-bipart bi))))
      (instance? BigInteger x) (BigDecimal. (cast BigInteger x))
      (instance? Double x) (BigDecimal. (.doubleValue (cast Number x)))
      (instance? Float x) (BigDecimal. (.doubleValue (cast Number x)))
      (instance? Ratio x)
        (let [r (cast Ratio x)]
          (cast BigDecimal (Numbers/divide (BigDecimal. (.-numerator r)) (.-denominator r))))
      :else (BigDecimal/valueOf (.longValue (cast Number x)))))

  (method ^:public ^:static ^{WarnBoxedMath false} toRatio ^Ratio [x]
    (cond
      (instance? Ratio x) (cast Ratio x)
      (instance? BigDecimal x)
        (let [bx (cast BigDecimal x)
              bv (.unscaledValue bx)
              scale (.scale bx)]
          (if (< scale 0)
              (Ratio. (.multiply bv (.pow BigInteger/TEN (unchecked-negate-int scale)))
                      BigInteger/ONE)
              (Ratio. bv (.pow BigInteger/TEN scale))))
      :else (Ratio. (Numbers/toBigInteger x) BigInteger/ONE)))

  (method ^:public ^:static ^{WarnBoxedMath false} rationalize ^Number [^Number x]
    (cond
      (or (instance? Float x) (instance? Double x))
        (Numbers/rationalize (BigDecimal/valueOf (.doubleValue x)))
      (instance? BigDecimal x)
        (let [bx (cast BigDecimal x)
              bv (.unscaledValue bx)
              scale (.scale bx)]
          (if (< scale 0)
              (BigInt/fromBigInteger
                (.multiply bv (.pow BigInteger/TEN (unchecked-negate-int scale))))
              (Numbers/divide bv (.pow BigInteger/TEN scale))))
      :else x))

  (method ^:public ^:static ^{WarnBoxedMath false} reduceBigInt ^Number [^BigInt val]
    (if (nil? (.-bipart val)) (^[long] Numbers/num (.-lpart val)) (.-bipart val)))

  (method ^:public ^:static divide ^Number [^:mutable ^BigInteger n ^:mutable ^BigInteger d]
    (when (.equals d BigInteger/ZERO) (throw (ArithmeticException. "Divide by zero")))
    (let [gcd (.gcd n d)]
      (if (.equals gcd BigInteger/ZERO)
          BigInt/ZERO
          (do
            (set! n (.divide n gcd))
            (set! d (.divide d gcd))
            (cond
              (.equals d BigInteger/ONE) (BigInt/fromBigInteger n)
              (.equals d (.negate BigInteger/ONE)) (BigInt/fromBigInteger (.negate n))
              :else
                (Ratio. (if (< (.signum d) 0) (.negate n) n) (if (< (.signum d) 0) (.negate d) d)))))))

  (method ^:public ^:static shiftLeftInt ^int [^int x ^int n]
    (bit-shift-left-int x n))

  (method ^:public ^:static shiftLeft ^long [x y]
    (^[long long] Numbers/shiftLeft (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static shiftLeft ^long [x ^long y]
    (^[long long] Numbers/shiftLeft (Numbers/bitOpsCast x) y))

  (method ^:public ^:static shiftLeft ^long [^long x y]
    (^[long long] Numbers/shiftLeft x (Numbers/bitOpsCast y)))

  (method ^:public ^:static shiftLeft ^long [^long x ^long n]
    (bit-shift-left x n))

  (method ^:public ^:static shiftRightInt ^int [^int x ^int n]
    (bit-shift-right-int x n))

  (method ^:public ^:static shiftRight ^long [x y]
    (^[long long] Numbers/shiftRight (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static shiftRight ^long [x ^long y]
    (^[long long] Numbers/shiftRight (Numbers/bitOpsCast x) y))

  (method ^:public ^:static shiftRight ^long [^long x y]
    (^[long long] Numbers/shiftRight x (Numbers/bitOpsCast y)))

  (method ^:public ^:static shiftRight ^long [^long x ^long n]
    (bit-shift-right x n))

  (method ^:public ^:static unsignedShiftRightInt ^int [^int x ^int n]
    (unsigned-bit-shift-right-int x n))

  (method ^:public ^:static unsignedShiftRight ^long [x y]
    (^[long long] Numbers/unsignedShiftRight (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static unsignedShiftRight ^long [x ^long y]
    (^[long long] Numbers/unsignedShiftRight (Numbers/bitOpsCast x) y))

  (method ^:public ^:static unsignedShiftRight ^long [^long x y]
    (^[long long] Numbers/unsignedShiftRight x (Numbers/bitOpsCast y)))

  (method ^:public ^:static unsignedShiftRight ^long [^long x ^long n]
    (unsigned-bit-shift-right x n))

  (defclass ^:static ^:final LongOps
    :implements [Ops]

    (method ^:public combine ^Ops [this ^Ops y] (.opsWith y this))

    (method ^:public ^:final opsWith ^Ops [this ^LongOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^DoubleOps x] DOUBLE_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^RatioOps x] RATIO_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^BigIntOps x] BIGINT_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^BigDecimalOps x]
      BIGDECIMAL_OPS)

    (method ^:public isZero ^boolean [this ^Number x] (== (.longValue x) 0))

    (method ^:public isPos ^boolean [this ^Number x] (> (.longValue x) 0))

    (method ^:public isNeg ^boolean [this ^Number x] (< (.longValue x) 0))

    (method ^:public ^:final add ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (^[long long] Numbers/add (.longValue x) (.longValue y))))

    (method ^:public ^:final addP ^Number [this ^Number x ^Number y]
      (let [lx (.longValue x)
            ly (.longValue y)
            ret (unchecked-add lx ly)]
        (if (and (< (bit-xor ret lx) 0) (< (bit-xor ret ly) 0))
            (.add BIGINT_OPS x y)
            (^[long] Numbers/num ret))))

    (method ^:public ^:final unchecked_add ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (^[long long] Numbers/unchecked_add (.longValue x) (.longValue y))))

    (method ^:public ^:final multiply ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (^[long long] Numbers/multiply (.longValue x) (.longValue y))))

    (method ^:public ^:final multiplyP ^Number [this ^Number x ^Number y]
      (let [lx (.longValue x)
            ly (.longValue y)]
        (if (and (== lx Long/MIN_VALUE) (< ly 0))
            (.multiply BIGINT_OPS x y)
            (let [ret (unchecked-multiply lx ly)]
              (if (and (not (== ly 0)) (not (== (unchecked-divide ret ly) lx)))
                  (.multiply BIGINT_OPS x y)
                  (^[long] Numbers/num ret))))))

    (method ^:public ^:final unchecked_multiply ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (^[long long] Numbers/unchecked_multiply (.longValue x) (.longValue y))))

    (method ^:static gcd ^long [^:mutable ^long u ^:mutable ^long v]
      (while (not (== v 0)) (let [r (unchecked-remainder u v)] (set! u v) (set! v r)))
      u)

    (method ^:public divide ^Number [this ^Number x ^Number y]
      (let [^:mutable n (.longValue x)
            val (.longValue y)]
        (if (or (== n Long/MIN_VALUE) (== val Long/MIN_VALUE))
            (.divide BIGINT_OPS x y)
            (let [gcd (LongOps/gcd n val)]
              (if (== gcd 0)
                  (^[long] Numbers/num 0)
                  (do
                    (set! n (unchecked-divide n gcd))
                    (let [^:mutable d (unchecked-divide val gcd)]
                      (if (== d 1)
                          (^[long] Numbers/num n)
                          (do
                            (when (< d 0)
                              (set! n (unchecked-negate n))
                              (set! d (unchecked-negate d)))
                            (Ratio. (BigInteger/valueOf n) (BigInteger/valueOf d)))))))))))

    (method ^:public quotient ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (unchecked-divide (.longValue x) (.longValue y))))

    (method ^:public remainder ^Number [this ^Number x ^Number y]
      (^[long] Numbers/num (unchecked-remainder (.longValue x) (.longValue y))))

    (method ^:public equiv ^boolean [this ^Number x ^Number y]
      (== (.longValue x) (.longValue y)))

    (method ^:public lt ^boolean [this ^Number x ^Number y]
      (< (.longValue x) (.longValue y)))

    (method ^:public lte ^boolean [this ^Number x ^Number y]
      (<= (.longValue x) (.longValue y)))

    (method ^:public gte ^boolean [this ^Number x ^Number y]
      (>= (.longValue x) (.longValue y)))

    (method ^:public ^:final negate ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/minus val))))

    (method ^:public ^:final negateP ^Number [this ^Number x]
      (let [val (.longValue x)]
        (if (> val Long/MIN_VALUE)
            (^[long] Numbers/num (unchecked-negate val))
            (BigInt/fromBigInteger (.negate (BigInteger/valueOf val))))))

    (method ^:public ^:final unchecked_negate ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/unchecked_minus val))))

    (method ^:public inc ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/inc val))))

    (method ^:public incP ^Number [this ^Number x]
      (let [val (.longValue x)]
        (if (< val Long/MAX_VALUE) (^[long] Numbers/num (unchecked-add val 1)) (.inc BIGINT_OPS x))))

    (method ^:public unchecked_inc ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/unchecked_inc val))))

    (method ^:public dec ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/dec val))))

    (method ^:public decP ^Number [this ^Number x]
      (let [val (.longValue x)]
        (if (> val Long/MIN_VALUE)
            (^[long] Numbers/num (unchecked-subtract val 1))
            (.dec BIGINT_OPS x))))

    (method ^:public unchecked_dec ^Number [this ^Number x]
      (let [val (.longValue x)] (^[long] Numbers/num (^[long] Numbers/unchecked_dec val))))

    (method ^:public abs ^Number [this ^Number x]
      (^[long] Numbers/num (^[long] Math/abs (.longValue x)))))

  (defclass ^:static ^:final DoubleOps
    :extends OpsP

    (method ^:public combine ^Ops [this ^Ops y] (.opsWith y this))

    (method ^:public ^:final opsWith ^Ops [this ^LongOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^DoubleOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^RatioOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigIntOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigDecimalOps x] this)

    (method ^:public isZero ^boolean [this ^Number x]
      (== (.doubleValue x) 0.0))

    (method ^:public isPos ^boolean [this ^Number x]
      (> (.doubleValue x) 0.0))

    (method ^:public isNeg ^boolean [this ^Number x]
      (< (.doubleValue x) 0.0))

    (method ^:public ^:final add ^Number [this ^Number x ^Number y]
      (Double/valueOf (unchecked-add (.doubleValue x) (.doubleValue y))))

    (method ^:public ^:final multiply ^Number [this ^Number x ^Number y]
      (Double/valueOf (unchecked-multiply (.doubleValue x) (.doubleValue y))))

    (method ^:public divide ^Number [this ^Number x ^Number y]
      (Double/valueOf (unchecked-divide (.doubleValue x) (.doubleValue y))))

    (method ^:public quotient ^Number [this ^Number x ^Number y]
      (^[double double] Numbers/quotient (.doubleValue x) (.doubleValue y)))

    (method ^:public remainder ^Number [this ^Number x ^Number y]
      (^[double double] Numbers/remainder (.doubleValue x) (.doubleValue y)))

    (method ^:public equiv ^boolean [this ^Number x ^Number y]
      (== (.doubleValue x) (.doubleValue y)))

    (method ^:public lt ^boolean [this ^Number x ^Number y]
      (< (.doubleValue x) (.doubleValue y)))

    (method ^:public lte ^boolean [this ^Number x ^Number y]
      (<= (.doubleValue x) (.doubleValue y)))

    (method ^:public gte ^boolean [this ^Number x ^Number y]
      (>= (.doubleValue x) (.doubleValue y)))

    (method ^:public ^:final negate ^Number [this ^Number x]
      (Double/valueOf (unchecked-negate (.doubleValue x))))

    (method ^:public inc ^Number [this ^Number x]
      (Double/valueOf (unchecked-add (.doubleValue x) 1.0)))

    (method ^:public dec ^Number [this ^Number x]
      (Double/valueOf (unchecked-subtract (.doubleValue x) 1.0)))

    (method ^:public abs ^Number [this ^Number x]
      (^[double] Numbers/num (^[double] Math/abs (.doubleValue x)))))

  (defclass ^:static ^:final RatioOps
    :extends OpsP

    (method ^:public combine ^Ops [this ^Ops y] (.opsWith y this))

    (method ^:public ^:final opsWith ^Ops [this ^LongOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^DoubleOps x] DOUBLE_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^RatioOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigIntOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigDecimalOps x]
      BIGDECIMAL_OPS)

    (method ^:public isZero ^boolean [this ^Number x]
      (let [r (cast Ratio x)] (== (.signum (.-numerator r)) 0)))

    (method ^:public isPos ^boolean [this ^Number x]
      (let [r (cast Ratio x)] (> (.signum (.-numerator r)) 0)))

    (method ^:public isNeg ^boolean [this ^Number x]
      (let [r (cast Ratio x)] (< (.signum (.-numerator r)) 0)))

    (method ^:static normalizeRet ^Number [^Number ret ^Number x ^Number y]
      ret)

    (method ^:public ^:final add ^Number [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)
            ret (.divide this
                         (.add (.multiply (.-numerator ry) (.-denominator rx))
                               (.multiply (.-numerator rx) (.-denominator ry)))
                         (.multiply (.-denominator ry) (.-denominator rx)))]
        (RatioOps/normalizeRet ret x y)))

    (method ^:public ^:final multiply ^Number [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)
            ret (Numbers/divide (.multiply (.-numerator ry) (.-numerator rx))
                                (.multiply (.-denominator ry) (.-denominator rx)))]
        (RatioOps/normalizeRet ret x y)))

    (method ^:public divide ^Number [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)
            ret (Numbers/divide (.multiply (.-denominator ry) (.-numerator rx))
                                (.multiply (.-numerator ry) (.-denominator rx)))]
        (RatioOps/normalizeRet ret x y)))

    (method ^:public quotient ^Number [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)
            q (.divide (.multiply (.-numerator rx) (.-denominator ry))
                       (.multiply (.-denominator rx) (.-numerator ry)))]
        (RatioOps/normalizeRet (BigInt/fromBigInteger q) x y)))

    (method ^:public remainder ^Number [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)
            q (.divide (.multiply (.-numerator rx) (.-denominator ry))
                       (.multiply (.-denominator rx) (.-numerator ry)))
            ret (Numbers/minus x (Numbers/multiply q y))]
        (RatioOps/normalizeRet ret x y)))

    (method ^:public equiv ^boolean [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)]
        (and (.equals (.-numerator rx) (.-numerator ry))
             (.equals (.-denominator rx) (.-denominator ry)))))

    (method ^:public lt ^boolean [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)]
        (Numbers/lt (.multiply (.-numerator rx) (.-denominator ry))
                    (.multiply (.-numerator ry) (.-denominator rx)))))

    (method ^:public lte ^boolean [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)]
        (Numbers/lte (.multiply (.-numerator rx) (.-denominator ry))
                     (.multiply (.-numerator ry) (.-denominator rx)))))

    (method ^:public gte ^boolean [this ^Number x ^Number y]
      (let [rx (Numbers/toRatio x)
            ry (Numbers/toRatio y)]
        (Numbers/gte (.multiply (.-numerator rx) (.-denominator ry))
                     (.multiply (.-numerator ry) (.-denominator rx)))))

    (method ^:public ^:final negate ^Number [this ^Number x]
      (let [r (cast Ratio x)] (Ratio. (.negate (.-numerator r)) (.-denominator r))))

    (method ^:public inc ^Number [this ^Number x]
      (^[Object long] Numbers/add x 1))

    (method ^:public dec ^Number [this ^Number x]
      (^[Object long] Numbers/add x -1))

    (method ^:public abs ^Number [this ^Number x]
      (let [r (cast Ratio x)] (Ratio. (.abs (.-numerator r)) (.-denominator r)))))

  (defclass ^:static ^:final BigIntOps
    :extends OpsP

    (method ^:public combine ^Ops [this ^Ops y] (.opsWith y this))

    (method ^:public ^:final opsWith ^Ops [this ^LongOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^DoubleOps x] DOUBLE_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^RatioOps x] RATIO_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^BigIntOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigDecimalOps x]
      BIGDECIMAL_OPS)

    (method ^:public isZero ^boolean [this ^Number x]
      (let [bx (Numbers/toBigInt x)]
        (if (nil? (.-bipart bx)) (== (.-lpart bx) 0) (== (.signum (.-bipart bx)) 0))))

    (method ^:public isPos ^boolean [this ^Number x]
      (let [bx (Numbers/toBigInt x)]
        (if (nil? (.-bipart bx)) (> (.-lpart bx) 0) (> (.signum (.-bipart bx)) 0))))

    (method ^:public isNeg ^boolean [this ^Number x]
      (let [bx (Numbers/toBigInt x)]
        (if (nil? (.-bipart bx)) (< (.-lpart bx) 0) (< (.signum (.-bipart bx)) 0))))

    (method ^:public ^:final add ^Number [this ^Number x ^Number y]
      (.add (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public ^:final multiply ^Number [this ^Number x ^Number y]
      (.multiply (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public divide ^Number [this ^Number x ^Number y]
      (Numbers/divide (Numbers/toBigInteger x) (Numbers/toBigInteger y)))

    (method ^:public quotient ^Number [this ^Number x ^Number y]
      (.quotient (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public remainder ^Number [this ^Number x ^Number y]
      (.remainder (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public equiv ^boolean [this ^Number x ^Number y]
      (.equals (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public lt ^boolean [this ^Number x ^Number y]
      (.lt (Numbers/toBigInt x) (Numbers/toBigInt y)))

    (method ^:public lte ^boolean [this ^Number x ^Number y]
      (<= (.compareTo (Numbers/toBigInteger x) (Numbers/toBigInteger y)) 0))

    (method ^:public gte ^boolean [this ^Number x ^Number y]
      (>= (.compareTo (Numbers/toBigInteger x) (Numbers/toBigInteger y)) 0))

    (method ^:public ^:final negate ^Number [this ^Number x]
      (BigInt/fromBigInteger (.negate (Numbers/toBigInteger x))))

    (method ^:public inc ^Number [this ^Number x]
      (let [bx (Numbers/toBigInteger x)] (BigInt/fromBigInteger (.add bx BigInteger/ONE))))

    (method ^:public dec ^Number [this ^Number x]
      (let [bx (Numbers/toBigInteger x)] (BigInt/fromBigInteger (.subtract bx BigInteger/ONE))))

    (method ^:public abs ^Number [this ^Number x]
      (BigInt/fromBigInteger (.abs (Numbers/toBigInteger x)))))

  (defclass ^:static ^:final BigDecimalOps
    :extends OpsP

    (field ^:static ^:final ^Var MATH_CONTEXT RT/MATH_CONTEXT)

    (method ^:public combine ^Ops [this ^Ops y] (.opsWith y this))

    (method ^:public ^:final opsWith ^Ops [this ^LongOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^DoubleOps x] DOUBLE_OPS)

    (method ^:public ^:final opsWith ^Ops [this ^RatioOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigIntOps x] this)

    (method ^:public ^:final opsWith ^Ops [this ^BigDecimalOps x] this)

    (method ^:public isZero ^boolean [this ^Number x]
      (let [bx (cast BigDecimal x)] (== (.signum bx) 0)))

    (method ^:public isPos ^boolean [this ^Number x]
      (let [bx (cast BigDecimal x)] (> (.signum bx) 0)))

    (method ^:public isNeg ^boolean [this ^Number x]
      (let [bx (cast BigDecimal x)] (< (.signum bx) 0)))

    (method ^:public ^:final add ^Number [this ^Number x ^Number y]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.add (Numbers/toBigDecimal x) (Numbers/toBigDecimal y))
            ^Number (.add (Numbers/toBigDecimal x) (Numbers/toBigDecimal y) mc))))

    (method ^:public ^:final multiply ^Number [this ^Number x ^Number y]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.multiply (Numbers/toBigDecimal x) (Numbers/toBigDecimal y))
            ^Number (.multiply (Numbers/toBigDecimal x) (Numbers/toBigDecimal y) mc))))

    (method ^:public divide ^Number [this ^Number x ^Number y]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.divide (Numbers/toBigDecimal x) (Numbers/toBigDecimal y))
            ^Number (.divide (Numbers/toBigDecimal x) (Numbers/toBigDecimal y) mc))))

    (method ^:public quotient ^Number [this ^Number x ^Number y]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.divideToIntegralValue (Numbers/toBigDecimal x) (Numbers/toBigDecimal y))
            ^Number (.divideToIntegralValue (Numbers/toBigDecimal x) (Numbers/toBigDecimal y) mc))))

    (method ^:public remainder ^Number [this ^Number x ^Number y]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.remainder (Numbers/toBigDecimal x) (Numbers/toBigDecimal y))
            ^Number (.remainder (Numbers/toBigDecimal x) (Numbers/toBigDecimal y) mc))))

    (method ^:public equiv ^boolean [this ^Number x ^Number y]
      (== (.compareTo (Numbers/toBigDecimal x) (Numbers/toBigDecimal y)) 0))

    (method ^:public lt ^boolean [this ^Number x ^Number y]
      (< (.compareTo (Numbers/toBigDecimal x) (Numbers/toBigDecimal y)) 0))

    (method ^:public lte ^boolean [this ^Number x ^Number y]
      (<= (.compareTo (Numbers/toBigDecimal x) (Numbers/toBigDecimal y)) 0))

    (method ^:public gte ^boolean [this ^Number x ^Number y]
      (>= (.compareTo (Numbers/toBigDecimal x) (Numbers/toBigDecimal y)) 0))

    (method ^:public ^:final negate ^Number [this ^Number x]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))]
        (if (nil? mc)
            ^Number (.negate (cast BigDecimal x))
            ^Number (.negate (cast BigDecimal x) mc))))

    (method ^:public inc ^Number [this ^Number x]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))
            bx (cast BigDecimal x)]
        (if (nil? mc) ^Number (.add bx BigDecimal/ONE) ^Number (.add bx BigDecimal/ONE mc))))

    (method ^:public dec ^Number [this ^Number x]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))
            bx (cast BigDecimal x)]
        (if (nil? mc)
            ^Number (.subtract bx BigDecimal/ONE)
            ^Number (.subtract bx BigDecimal/ONE mc))))

    (method ^:public abs ^Number [this ^Number x]
      (let [mc (cast MathContext (.deref MATH_CONTEXT))
            bx (cast BigDecimal x)]
        (if (nil? mc) ^Number (.abs (cast BigDecimal x)) ^Number (.abs (cast BigDecimal x) mc)))))

  (field ^:static ^:final ^LongOps LONG_OPS (LongOps.))

  (field ^:static ^:final ^DoubleOps DOUBLE_OPS (DoubleOps.))

  (field ^:static ^:final ^RatioOps RATIO_OPS (RatioOps.))

  (field ^:static ^:final ^BigIntOps BIGINT_OPS (BigIntOps.))

  (field ^:static ^:final ^BigDecimalOps BIGDECIMAL_OPS (BigDecimalOps.))

  (defclass ^:public ^:static ^:enum Category
    (constants INTEGER FLOATING DECIMAL RATIO))

  (method ^:static ops ^Ops [x]
    (let [xc (.getClass x)]
      (cond
        (identical? xc Long) LONG_OPS
        (identical? xc Double) DOUBLE_OPS
        (identical? xc Integer) LONG_OPS
        (identical? xc Float) DOUBLE_OPS
        (identical? xc BigInt) BIGINT_OPS
        (identical? xc BigInteger) BIGINT_OPS
        (identical? xc Ratio) RATIO_OPS
        (identical? xc BigDecimal) BIGDECIMAL_OPS
        :else LONG_OPS)))

  (method ^:static ^{WarnBoxedMath false} hasheqFrom ^int [^Number x ^Class xc]
    (cond
      (or (or (or (identical? xc Integer) (identical? xc Short)) (identical? xc Byte))
          (and (and (identical? xc BigInteger) (^[Object long] Numbers/lte x Long/MAX_VALUE))
               (^[Object long] Numbers/gte x Long/MIN_VALUE)))
        (let [lpart (.longValue x)] (Murmur3/hashLong lpart))
      (identical? xc BigDecimal)
        (if (Numbers/isZero x)
            (.hashCode BigDecimal/ZERO)
            (let [tmp (.stripTrailingZeros (cast BigDecimal x))] (.hashCode tmp)))
      (and (identical? xc Float) (.equals x (unchecked-negate-float (float 0.0)))) 0
      :else (.hashCode x)))

  (method ^:static ^{WarnBoxedMath false} hasheq ^int [^Number x]
    (let [xc (.getClass x)]
      (cond
        (identical? xc Long) (let [lpart (.longValue x)] (Murmur3/hashLong lpart))
        (identical? xc Double) (if (.equals x (unchecked-negate 0.0)) 0 (.hashCode x))
        :else (Numbers/hasheqFrom x xc))))

  (method ^:static category ^Category [x]
    (let [xc (.getClass x)]
      (cond
        (identical? xc Integer) Category/INTEGER
        (identical? xc Double) Category/FLOATING
        (identical? xc Long) Category/INTEGER
        (identical? xc Float) Category/FLOATING
        (identical? xc BigInt) Category/INTEGER
        (identical? xc Ratio) Category/RATIO
        (identical? xc BigDecimal) Category/DECIMAL
        :else Category/INTEGER)))

  (method ^:static bitOpsCast ^long [x]
    (let [xc (.getClass x)]
      (if (or (or (or (identical? xc Long) (identical? xc Integer)) (identical? xc Short))
              (identical? xc Byte))
          (RT/longCast x)
          (throw (IllegalArgumentException. (java-str "bit operation not supported for: " xc))))))

  (method ^:public ^:static ^{WarnBoxedMath false} float_array ^float/1 [^int size init]
    (let [ret (new float/1 size)]
      (if (instance? Number init)
          (let [f (.floatValue (cast Number init))]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i f) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.floatValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} float_array ^float/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new float/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new float/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.floatValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} double_array ^double/1 [^int size init]
    (let [ret (new double/1 size)]
      (if (instance? Number init)
          (let [f (.doubleValue (cast Number init))]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i f) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.doubleValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} double_array ^double/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new double/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new double/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.doubleValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} int_array ^int/1 [^int size init]
    (let [ret (new int/1 size)]
      (if (instance? Number init)
          (let [f (.intValue (cast Number init))]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i f) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.intValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} int_array ^int/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new int/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new int/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.intValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} long_array ^long/1 [^int size init]
    (let [ret (new long/1 size)]
      (if (instance? Number init)
          (let [f (.longValue (cast Number init))]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i f) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.longValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} long_array ^long/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new long/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new long/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.longValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} short_array ^short/1 [^int size init]
    (let [ret (new short/1 size)]
      (if (instance? Short init)
          (let [^short s (cast Short init)]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i s) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.shortValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} short_array ^short/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new short/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new short/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.shortValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} char_array ^char/1 [^int size init]
    (let [ret (new char/1 size)]
      (if (instance? Character init)
          (let [^char c (cast Character init)]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i c) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (cast Character (.first s)))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} char_array ^char/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new char/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new char/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (cast Character (.first s)))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} byte_array ^byte/1 [^int size init]
    (let [ret (new byte/1 size)]
      (if (instance? Byte init)
          (let [^byte b (cast Byte init)]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i b) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.byteValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} byte_array ^byte/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new byte/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new byte/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (.byteValue (cast Number (.first s))))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} boolean_array ^boolean/1 [^int size init]
    (let [ret (new boolean/1 size)]
      (if (instance? Boolean init)
          (let [^boolean b (cast Boolean init)]
            (loop [^int i 0]
              (when (< i (alength ret)) (aset ret i b) (recur (unchecked-inc-int i)))))
          (let [^:mutable s (RT/seq init)
                ^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (cast Boolean (.first s)))
              (set! i (unchecked-inc-int i))
              (set! s (.next s)))))
      ret))

  (method ^:public ^:static ^{WarnBoxedMath false} boolean_array ^boolean/1 [sizeOrSeq]
    (if (instance? Number sizeOrSeq)
        (new boolean/1 (.intValue (cast Number sizeOrSeq)))
        (let [^:mutable s (RT/seq sizeOrSeq)
              size (RT/count s)
              ret (new boolean/1 size)]
          (let [^:mutable ^int i 0]
            (while (and (< i size) (some? s))
              (aset ret i (cast Boolean (.first s)))
              (set! i (unchecked-inc-int i))
              (set! s (.next s))))
          ret)))

  (method ^:public ^:static ^{WarnBoxedMath false} booleans ^boolean/1 [array]
    (cast boolean/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} bytes ^byte/1 [array]
    (cast byte/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} chars ^char/1 [array]
    (cast char/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} shorts ^short/1 [array]
    (cast short/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} floats ^float/1 [array]
    (cast float/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} doubles ^double/1 [array]
    (cast double/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} ints ^int/1 [array]
    (cast int/1 array))

  (method ^:public ^:static ^{WarnBoxedMath false} longs ^long/1 [array]
    (cast long/1 array))

  (method ^:public ^:static num ^Number [x] (cast Number x))

  (method ^:public ^:static num ^Number [^float x] (Float/valueOf x))

  (method ^:public ^:static num ^Number [^double x] (Double/valueOf x))

  (method ^:public ^:static add ^double [^double x ^double y]
    (unchecked-add x y))

  (method ^:public ^:static addP ^double [^double x ^double y]
    (unchecked-add x y))

  (method ^:public ^:static minus ^double [^double x ^double y]
    (unchecked-subtract x y))

  (method ^:public ^:static minusP ^double [^double x ^double y]
    (unchecked-subtract x y))

  (method ^:public ^:static minus ^double [^double x]
    (unchecked-negate x))

  (method ^:public ^:static minusP ^double [^double x]
    (unchecked-negate x))

  (method ^:public ^:static inc ^double [^double x] (unchecked-add x 1.0))

  (method ^:public ^:static incP ^double [^double x]
    (unchecked-add x 1.0))

  (method ^:public ^:static dec ^double [^double x]
    (unchecked-subtract x 1.0))

  (method ^:public ^:static decP ^double [^double x]
    (unchecked-subtract x 1.0))

  (method ^:public ^:static multiply ^double [^double x ^double y]
    (unchecked-multiply x y))

  (method ^:public ^:static multiplyP ^double [^double x ^double y]
    (unchecked-multiply x y))

  (method ^:public ^:static divide ^double [^double x ^double y]
    (unchecked-divide x y))

  (method ^:public ^:static equiv ^boolean [^double x ^double y] (== x y))

  (method ^:public ^:static lt ^boolean [^double x ^double y] (< x y))

  (method ^:public ^:static lte ^boolean [^double x ^double y] (<= x y))

  (method ^:public ^:static gt ^boolean [^double x ^double y] (> x y))

  (method ^:public ^:static gte ^boolean [^double x ^double y] (>= x y))

  (method ^:public ^:static isPos ^boolean [^double x] (> x 0.0))

  (method ^:public ^:static isNeg ^boolean [^double x] (< x 0.0))

  (method ^:public ^:static isZero ^boolean [^double x] (== x 0.0))

  (method ^:static throwIntOverflow ^int []
    (throw (ArithmeticException. "integer overflow")))

  (method ^:public ^:static unchecked_int_add ^int [^int x ^int y]
    (unchecked-add-int x y))

  (method ^:public ^:static unchecked_int_subtract ^int [^int x ^int y]
    (unchecked-subtract-int x y))

  (method ^:public ^:static unchecked_int_negate ^int [^int x]
    (unchecked-negate-int x))

  (method ^:public ^:static unchecked_int_inc ^int [^int x]
    (unchecked-add-int x 1))

  (method ^:public ^:static unchecked_int_dec ^int [^int x]
    (unchecked-subtract-int x 1))

  (method ^:public ^:static unchecked_int_multiply ^int [^int x ^int y]
    (unchecked-multiply-int x y))

  (method ^:public ^:static not ^long [x]
    (^[long] Numbers/not (Numbers/bitOpsCast x)))

  (method ^:public ^:static not ^long [^long x] (bit-not x))

  (method ^:public ^:static and ^long [x y]
    (^[long long] Numbers/and (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static and ^long [x ^long y]
    (^[long long] Numbers/and (Numbers/bitOpsCast x) y))

  (method ^:public ^:static and ^long [^long x y]
    (^[long long] Numbers/and x (Numbers/bitOpsCast y)))

  (method ^:public ^:static and ^long [^long x ^long y] (bit-and x y))

  (method ^:public ^:static or ^long [x y]
    (^[long long] Numbers/or (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static or ^long [x ^long y]
    (^[long long] Numbers/or (Numbers/bitOpsCast x) y))

  (method ^:public ^:static or ^long [^long x y]
    (^[long long] Numbers/or x (Numbers/bitOpsCast y)))

  (method ^:public ^:static or ^long [^long x ^long y] (bit-or x y))

  (method ^:public ^:static xor ^long [x y]
    (^[long long] Numbers/xor (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static xor ^long [x ^long y]
    (^[long long] Numbers/xor (Numbers/bitOpsCast x) y))

  (method ^:public ^:static xor ^long [^long x y]
    (^[long long] Numbers/xor x (Numbers/bitOpsCast y)))

  (method ^:public ^:static xor ^long [^long x ^long y] (bit-xor x y))

  (method ^:public ^:static andNot ^long [x y]
    (^[long long] Numbers/andNot (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static andNot ^long [x ^long y]
    (^[long long] Numbers/andNot (Numbers/bitOpsCast x) y))

  (method ^:public ^:static andNot ^long [^long x y]
    (^[long long] Numbers/andNot x (Numbers/bitOpsCast y)))

  (method ^:public ^:static andNot ^long [^long x ^long y]
    (bit-and x (bit-not y)))

  (method ^:public ^:static clearBit ^long [x y]
    (^[long long] Numbers/clearBit (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static clearBit ^long [x ^long y]
    (^[long long] Numbers/clearBit (Numbers/bitOpsCast x) y))

  (method ^:public ^:static clearBit ^long [^long x y]
    (^[long long] Numbers/clearBit x (Numbers/bitOpsCast y)))

  (method ^:public ^:static clearBit ^long [^long x ^long n]
    (bit-and x (bit-not (bit-shift-left 1 n))))

  (method ^:public ^:static setBit ^long [x y]
    (^[long long] Numbers/setBit (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static setBit ^long [x ^long y]
    (^[long long] Numbers/setBit (Numbers/bitOpsCast x) y))

  (method ^:public ^:static setBit ^long [^long x y]
    (^[long long] Numbers/setBit x (Numbers/bitOpsCast y)))

  (method ^:public ^:static setBit ^long [^long x ^long n]
    (bit-or x (bit-shift-left 1 n)))

  (method ^:public ^:static flipBit ^long [x y]
    (^[long long] Numbers/flipBit (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static flipBit ^long [x ^long y]
    (^[long long] Numbers/flipBit (Numbers/bitOpsCast x) y))

  (method ^:public ^:static flipBit ^long [^long x y]
    (^[long long] Numbers/flipBit x (Numbers/bitOpsCast y)))

  (method ^:public ^:static flipBit ^long [^long x ^long n]
    (bit-xor x (bit-shift-left 1 n)))

  (method ^:public ^:static testBit ^boolean [x y]
    (^[long long] Numbers/testBit (Numbers/bitOpsCast x) (Numbers/bitOpsCast y)))

  (method ^:public ^:static testBit ^boolean [x ^long y]
    (^[long long] Numbers/testBit (Numbers/bitOpsCast x) y))

  (method ^:public ^:static testBit ^boolean [^long x y]
    (^[long long] Numbers/testBit x (Numbers/bitOpsCast y)))

  (method ^:public ^:static testBit ^boolean [^long x ^long n]
    (not (== (bit-and x (bit-shift-left 1 n)) 0)))

  (method ^:public ^:static unchecked_int_divide ^int [^int x ^int y]
    (unchecked-divide-int x y))

  (method ^:public ^:static unchecked_int_remainder ^int [^int x ^int y]
    (unchecked-remainder-int x y))

  (method ^:public ^:static num ^Number [^long x] (Long/valueOf x))

  (method ^:public ^:static unchecked_add ^long [^long x ^long y]
    (unchecked-add x y))

  (method ^:public ^:static unchecked_minus ^long [^long x ^long y]
    (unchecked-subtract x y))

  (method ^:public ^:static unchecked_multiply ^long [^long x ^long y]
    (unchecked-multiply x y))

  (method ^:public ^:static unchecked_minus ^long [^long x]
    (unchecked-negate x))

  (method ^:public ^:static unchecked_inc ^long [^long x]
    (unchecked-add x 1))

  (method ^:public ^:static unchecked_dec ^long [^long x]
    (unchecked-subtract x 1))

  (method ^:public ^:static unchecked_add ^Number [x y]
    (.unchecked_add (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static unchecked_minus ^Number [x y]
    (let [yops (Numbers/ops y)]
      (.unchecked_add (.combine (Numbers/ops x) yops)
                      (cast Number x)
                      (.unchecked_negate yops (cast Number y)))))

  (method ^:public ^:static unchecked_multiply ^Number [x y]
    (.unchecked_multiply (.combine (Numbers/ops x) (Numbers/ops y)) (cast Number x) (cast Number y)))

  (method ^:public ^:static unchecked_minus ^Number [x]
    (.unchecked_negate (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static unchecked_inc ^Number [x]
    (.unchecked_inc (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static unchecked_dec ^Number [x]
    (.unchecked_dec (Numbers/ops x) (cast Number x)))

  (method ^:public ^:static unchecked_add ^double [^double x ^double y]
    (^[double double] Numbers/add x y))

  (method ^:public ^:static unchecked_minus ^double [^double x ^double y]
    (^[double double] Numbers/minus x y))

  (method ^:public ^:static unchecked_multiply ^double [^double x ^double y]
    (^[double double] Numbers/multiply x y))

  (method ^:public ^:static unchecked_minus ^double [^double x]
    (^[double] Numbers/minus x))

  (method ^:public ^:static unchecked_inc ^double [^double x]
    (^[double] Numbers/inc x))

  (method ^:public ^:static unchecked_dec ^double [^double x]
    (^[double] Numbers/dec x))

  (method ^:public ^:static unchecked_add ^double [^double x y]
    (^[double Object] Numbers/add x y))

  (method ^:public ^:static unchecked_minus ^double [^double x y]
    (^[double Object] Numbers/minus x y))

  (method ^:public ^:static unchecked_multiply ^double [^double x y]
    (^[double Object] Numbers/multiply x y))

  (method ^:public ^:static unchecked_add ^double [x ^double y]
    (^[Object double] Numbers/add x y))

  (method ^:public ^:static unchecked_minus ^double [x ^double y]
    (^[Object double] Numbers/minus x y))

  (method ^:public ^:static unchecked_multiply ^double [x ^double y]
    (^[Object double] Numbers/multiply x y))

  (method ^:public ^:static unchecked_add ^double [^double x ^long y]
    (^[double long] Numbers/add x y))

  (method ^:public ^:static unchecked_minus ^double [^double x ^long y]
    (^[double long] Numbers/minus x y))

  (method ^:public ^:static unchecked_multiply ^double [^double x ^long y]
    (^[double long] Numbers/multiply x y))

  (method ^:public ^:static unchecked_add ^double [^long x ^double y]
    (^[long double] Numbers/add x y))

  (method ^:public ^:static unchecked_minus ^double [^long x ^double y]
    (^[long double] Numbers/minus x y))

  (method ^:public ^:static unchecked_multiply ^double [^long x ^double y]
    (^[long double] Numbers/multiply x y))

  (method ^:public ^:static unchecked_add ^Number [^long x y]
    (^[Object Object] Numbers/unchecked_add (Long/valueOf x) y))

  (method ^:public ^:static unchecked_minus ^Number [^long x y]
    (^[Object Object] Numbers/unchecked_minus (Long/valueOf x) y))

  (method ^:public ^:static unchecked_multiply ^Number [^long x y]
    (^[Object Object] Numbers/unchecked_multiply (Long/valueOf x) y))

  (method ^:public ^:static unchecked_add ^Number [x ^long y]
    (^[Object Object] Numbers/unchecked_add x (Long/valueOf y)))

  (method ^:public ^:static unchecked_minus ^Number [x ^long y]
    (^[Object Object] Numbers/unchecked_minus x (Long/valueOf y)))

  (method ^:public ^:static unchecked_multiply ^Number [x ^long y]
    (^[Object Object] Numbers/unchecked_multiply x (Long/valueOf y)))

  (method ^:public ^:static quotient ^Number [^double x y]
    (^[Object Object] Numbers/quotient (Double/valueOf x) y))

  (method ^:public ^:static quotient ^Number [x ^double y]
    (^[Object Object] Numbers/quotient x (Double/valueOf y)))

  (method ^:public ^:static quotient ^Number [^long x y]
    (^[Object Object] Numbers/quotient (Long/valueOf x) y))

  (method ^:public ^:static quotient ^Number [x ^long y]
    (^[Object Object] Numbers/quotient x (Long/valueOf y)))

  (method ^:public ^:static quotient ^double [^double x ^long y]
    (^[double double] Numbers/quotient x (double y)))

  (method ^:public ^:static quotient ^double [^long x ^double y]
    (^[double double] Numbers/quotient (double x) y))

  (method ^:public ^:static remainder ^Number [^double x y]
    (^[Object Object] Numbers/remainder (Double/valueOf x) y))

  (method ^:public ^:static remainder ^Number [x ^double y]
    (^[Object Object] Numbers/remainder x (Double/valueOf y)))

  (method ^:public ^:static remainder ^Number [^long x y]
    (^[Object Object] Numbers/remainder (Long/valueOf x) y))

  (method ^:public ^:static remainder ^Number [x ^long y]
    (^[Object Object] Numbers/remainder x (Long/valueOf y)))

  (method ^:public ^:static remainder ^double [^double x ^long y]
    (^[double double] Numbers/remainder x (double y)))

  (method ^:public ^:static remainder ^double [^long x ^double y]
    (^[double double] Numbers/remainder (double x) y))

  (method ^:public ^:static add ^long [^long x ^long y]
    (^[long long] Math/addExact x y))

  (method ^:public ^:static addP ^Number [^long x ^long y]
    (let [ret (unchecked-add x y)]
      (if (and (< (bit-xor ret x) 0) (< (bit-xor ret y) 0))
          (^[Object Object] Numbers/addP (Long/valueOf x) (Long/valueOf y))
          (^[long] Numbers/num ret))))

  (method ^:public ^:static minus ^long [^long x ^long y]
    (^[long long] Math/subtractExact x y))

  (method ^:public ^:static minusP ^Number [^long x ^long y]
    (let [ret (unchecked-subtract x y)]
      (if (and (< (bit-xor ret x) 0) (< (bit-xor ret (bit-not y)) 0))
          (^[Object Object] Numbers/minusP (Long/valueOf x) (Long/valueOf y))
          (^[long] Numbers/num ret))))

  (method ^:public ^:static minus ^long [^long x]
    (^[long] Math/negateExact x))

  (method ^:public ^:static minusP ^Number [^long x]
    (if (== x Long/MIN_VALUE)
        (BigInt/fromBigInteger (.negate (BigInteger/valueOf x)))
        (^[long] Numbers/num (unchecked-negate x))))

  (method ^:public ^:static inc ^long [^long x]
    (^[long] Math/incrementExact x))

  (method ^:public ^:static incP ^Number [^long x]
    (if (== x Long/MAX_VALUE) (.inc BIGINT_OPS x) (^[long] Numbers/num (unchecked-add x 1))))

  (method ^:public ^:static dec ^long [^long x]
    (^[long] Math/decrementExact x))

  (method ^:public ^:static decP ^Number [^long x]
    (if (== x Long/MIN_VALUE) (.dec BIGINT_OPS x) (^[long] Numbers/num (unchecked-subtract x 1))))

  (method ^:public ^:static multiply ^long [^long x ^long y]
    (^[long long] Math/multiplyExact x y))

  (method ^:public ^:static multiplyP ^Number [^long x ^long y]
    (if (and (== x Long/MIN_VALUE) (< y 0))
        (^[Object Object] Numbers/multiplyP (Long/valueOf x) (Long/valueOf y))
        (let [ret (unchecked-multiply x y)]
          (if (and (not (== y 0)) (not (== (unchecked-divide ret y) x)))
              (^[Object Object] Numbers/multiplyP (Long/valueOf x) (Long/valueOf y))
              (^[long] Numbers/num ret)))))

  (method ^:public ^:static quotient ^long [^long x ^long y]
    (unchecked-divide x y))

  (method ^:public ^:static remainder ^long [^long x ^long y]
    (unchecked-remainder x y))

  (method ^:public ^:static equiv ^boolean [^long x ^long y] (== x y))

  (method ^:public ^:static lt ^boolean [^long x ^long y] (< x y))

  (method ^:public ^:static lte ^boolean [^long x ^long y] (<= x y))

  (method ^:public ^:static gt ^boolean [^long x ^long y] (> x y))

  (method ^:public ^:static gte ^boolean [^long x ^long y] (>= x y))

  (method ^:public ^:static isPos ^boolean [^long x] (> x 0))

  (method ^:public ^:static isNeg ^boolean [^long x] (< x 0))

  (method ^:public ^:static isZero ^boolean [^long x] (== x 0))

  (method ^:public ^:static add ^Number [^long x y]
    (^[Object Object] Numbers/add (Long/valueOf x) y))

  (method ^:public ^:static add ^Number [x ^long y]
    (^[Object Object] Numbers/add x (Long/valueOf y)))

  (method ^:public ^:static addP ^Number [^long x y]
    (^[Object Object] Numbers/addP (Long/valueOf x) y))

  (method ^:public ^:static addP ^Number [x ^long y]
    (^[Object Object] Numbers/addP x (Long/valueOf y)))

  (method ^:public ^:static add ^double [^double x y]
    (^[double double] Numbers/add x (.doubleValue (cast Number y))))

  (method ^:public ^:static add ^double [x ^double y]
    (^[double double] Numbers/add (.doubleValue (cast Number x)) y))

  (method ^:public ^:static add ^double [^double x ^long y]
    (unchecked-add x y))

  (method ^:public ^:static add ^double [^long x ^double y]
    (unchecked-add x y))

  (method ^:public ^:static addP ^double [^double x y]
    (^[double double] Numbers/addP x (.doubleValue (cast Number y))))

  (method ^:public ^:static addP ^double [x ^double y]
    (^[double double] Numbers/addP (.doubleValue (cast Number x)) y))

  (method ^:public ^:static addP ^double [^double x ^long y]
    (unchecked-add x y))

  (method ^:public ^:static addP ^double [^long x ^double y]
    (unchecked-add x y))

  (method ^:public ^:static minus ^Number [^long x y]
    (^[Object Object] Numbers/minus (Long/valueOf x) y))

  (method ^:public ^:static minus ^Number [x ^long y]
    (^[Object Object] Numbers/minus x (Long/valueOf y)))

  (method ^:public ^:static minusP ^Number [^long x y]
    (^[Object Object] Numbers/minusP (Long/valueOf x) y))

  (method ^:public ^:static minusP ^Number [x ^long y]
    (^[Object Object] Numbers/minusP x (Long/valueOf y)))

  (method ^:public ^:static minus ^double [^double x y]
    (^[double double] Numbers/minus x (.doubleValue (cast Number y))))

  (method ^:public ^:static minus ^double [x ^double y]
    (^[double double] Numbers/minus (.doubleValue (cast Number x)) y))

  (method ^:public ^:static minus ^double [^double x ^long y]
    (unchecked-subtract x y))

  (method ^:public ^:static minus ^double [^long x ^double y]
    (unchecked-subtract x y))

  (method ^:public ^:static minusP ^double [^double x y]
    (^[double double] Numbers/minus x (.doubleValue (cast Number y))))

  (method ^:public ^:static minusP ^double [x ^double y]
    (^[double double] Numbers/minus (.doubleValue (cast Number x)) y))

  (method ^:public ^:static minusP ^double [^double x ^long y]
    (unchecked-subtract x y))

  (method ^:public ^:static minusP ^double [^long x ^double y]
    (unchecked-subtract x y))

  (method ^:public ^:static multiply ^Number [^long x y]
    (^[Object Object] Numbers/multiply (Long/valueOf x) y))

  (method ^:public ^:static multiply ^Number [x ^long y]
    (^[Object Object] Numbers/multiply x (Long/valueOf y)))

  (method ^:public ^:static multiplyP ^Number [^long x y]
    (^[Object Object] Numbers/multiplyP (Long/valueOf x) y))

  (method ^:public ^:static multiplyP ^Number [x ^long y]
    (^[Object Object] Numbers/multiplyP x (Long/valueOf y)))

  (method ^:public ^:static multiply ^double [^double x y]
    (^[double double] Numbers/multiply x (.doubleValue (cast Number y))))

  (method ^:public ^:static multiply ^double [x ^double y]
    (^[double double] Numbers/multiply (.doubleValue (cast Number x)) y))

  (method ^:public ^:static multiply ^double [^double x ^long y]
    (unchecked-multiply x y))

  (method ^:public ^:static multiply ^double [^long x ^double y]
    (unchecked-multiply x y))

  (method ^:public ^:static multiplyP ^double [^double x y]
    (^[double double] Numbers/multiplyP x (.doubleValue (cast Number y))))

  (method ^:public ^:static multiplyP ^double [x ^double y]
    (^[double double] Numbers/multiplyP (.doubleValue (cast Number x)) y))

  (method ^:public ^:static multiplyP ^double [^double x ^long y]
    (unchecked-multiply x y))

  (method ^:public ^:static multiplyP ^double [^long x ^double y]
    (unchecked-multiply x y))

  (method ^:public ^:static divide ^Number [^long x y]
    (^[Object Object] Numbers/divide (Long/valueOf x) y))

  (method ^:public ^:static divide ^Number [x ^long y]
    (^[Object Object] Numbers/divide x (Long/valueOf y)))

  (method ^:public ^:static divide ^double [^double x y]
    (unchecked-divide x (.doubleValue (cast Number y))))

  (method ^:public ^:static divide ^double [x ^double y]
    (unchecked-divide (.doubleValue (cast Number x)) y))

  (method ^:public ^:static divide ^double [^double x ^long y]
    (unchecked-divide x y))

  (method ^:public ^:static divide ^double [^long x ^double y]
    (unchecked-divide x y))

  (method ^:public ^:static divide ^Number [^long x ^long y]
    (^[Object Object] Numbers/divide (Long/valueOf x) (Long/valueOf y)))

  (method ^:public ^:static lt ^boolean [^long x y]
    (^[Object Object] Numbers/lt (Long/valueOf x) y))

  (method ^:public ^:static lt ^boolean [x ^long y]
    (^[Object Object] Numbers/lt x (Long/valueOf y)))

  (method ^:public ^:static lt ^boolean [^double x y]
    (< x (.doubleValue (cast Number y))))

  (method ^:public ^:static lt ^boolean [x ^double y]
    (< (.doubleValue (cast Number x)) y))

  (method ^:public ^:static lt ^boolean [^double x ^long y] (< x y))

  (method ^:public ^:static lt ^boolean [^long x ^double y] (< x y))

  (method ^:public ^:static lte ^boolean [^long x y]
    (^[Object Object] Numbers/lte (Long/valueOf x) y))

  (method ^:public ^:static lte ^boolean [x ^long y]
    (^[Object Object] Numbers/lte x (Long/valueOf y)))

  (method ^:public ^:static lte ^boolean [^double x y]
    (<= x (.doubleValue (cast Number y))))

  (method ^:public ^:static lte ^boolean [x ^double y]
    (<= (.doubleValue (cast Number x)) y))

  (method ^:public ^:static lte ^boolean [^double x ^long y] (<= x y))

  (method ^:public ^:static lte ^boolean [^long x ^double y] (<= x y))

  (method ^:public ^:static gt ^boolean [^long x y]
    (^[Object Object] Numbers/gt (Long/valueOf x) y))

  (method ^:public ^:static gt ^boolean [x ^long y]
    (^[Object Object] Numbers/gt x (Long/valueOf y)))

  (method ^:public ^:static gt ^boolean [^double x y]
    (> x (.doubleValue (cast Number y))))

  (method ^:public ^:static gt ^boolean [x ^double y]
    (> (.doubleValue (cast Number x)) y))

  (method ^:public ^:static gt ^boolean [^double x ^long y] (> x y))

  (method ^:public ^:static gt ^boolean [^long x ^double y] (> x y))

  (method ^:public ^:static gte ^boolean [^long x y]
    (^[Object Object] Numbers/gte (Long/valueOf x) y))

  (method ^:public ^:static gte ^boolean [x ^long y]
    (^[Object Object] Numbers/gte x (Long/valueOf y)))

  (method ^:public ^:static gte ^boolean [^double x y]
    (>= x (.doubleValue (cast Number y))))

  (method ^:public ^:static gte ^boolean [x ^double y]
    (>= (.doubleValue (cast Number x)) y))

  (method ^:public ^:static gte ^boolean [^double x ^long y] (>= x y))

  (method ^:public ^:static gte ^boolean [^long x ^double y] (>= x y))

  (method ^:public ^:static equiv ^boolean [^long x y]
    (^[Object Object] Numbers/equiv (Long/valueOf x) y))

  (method ^:public ^:static equiv ^boolean [x ^long y]
    (^[Object Object] Numbers/equiv x (Long/valueOf y)))

  (method ^:public ^:static equiv ^boolean [^double x y]
    (== x (.doubleValue (cast Number y))))

  (method ^:public ^:static equiv ^boolean [x ^double y]
    (== (.doubleValue (cast Number x)) y))

  (method ^:public ^:static equiv ^boolean [^double x ^long y] (== x y))

  (method ^:public ^:static equiv ^boolean [^long x ^double y] (== x y))

  (method ^:static isNaN ^boolean [x]
    (or (and (instance? Double x) (.isNaN (cast Double x)))
        (and (instance? Float x) (.isNaN (cast Float x)))))

  (method ^:public ^:static max ^double [^double x ^double y]
    (^[double double] Math/max x y))

  (method ^:public ^:static max [^double x ^long y]
    (cond (Double/isNaN x) x (> x y) x :else y))

  (method ^:public ^:static max [^double x y]
    (cond (Double/isNaN x) x (Numbers/isNaN y) y (> x (.doubleValue (cast Number y))) x :else y))

  (method ^:public ^:static max [^long x ^double y]
    (cond (Double/isNaN y) y (> x y) x :else y))

  (method ^:public ^:static max ^long [^long x ^long y]
    (^[long long] Math/max x y))

  (method ^:public ^:static max [^long x y]
    (cond (Numbers/isNaN y) y (^[long Object] Numbers/gt x y) x :else y))

  (method ^:public ^:static max [x ^long y]
    (cond (Numbers/isNaN x) x (^[Object long] Numbers/gt x y) x :else y))

  (method ^:public ^:static max [x ^double y]
    (cond (Numbers/isNaN x) x (Double/isNaN y) y (> (.doubleValue (cast Number x)) y) x :else y))

  (method ^:public ^:static max [x y]
    (cond (Numbers/isNaN x) x (Numbers/isNaN y) y (Numbers/gt x y) x :else y))

  (method ^:public ^:static min ^double [^double x ^double y]
    (^[double double] Math/min x y))

  (method ^:public ^:static min [^double x ^long y]
    (cond (Double/isNaN x) x (< x y) x :else y))

  (method ^:public ^:static min [^double x y]
    (cond (Double/isNaN x) x (Numbers/isNaN y) y (< x (.doubleValue (cast Number y))) x :else y))

  (method ^:public ^:static min [^long x ^double y]
    (cond (Double/isNaN y) y (< x y) x :else y))

  (method ^:public ^:static min ^long [^long x ^long y]
    (^[long long] Math/min x y))

  (method ^:public ^:static min [^long x y]
    (cond (Numbers/isNaN y) y (^[long Object] Numbers/lt x y) x :else y))

  (method ^:public ^:static min [x ^long y]
    (cond (Numbers/isNaN x) x (^[Object long] Numbers/lt x y) x :else y))

  (method ^:public ^:static min [x ^double y]
    (cond (Numbers/isNaN x) x (Double/isNaN y) y (< (.doubleValue (cast Number x)) y) x :else y))

  (method ^:public ^:static min [x y]
    (cond (Numbers/isNaN x) x (Numbers/isNaN y) y (Numbers/lt x y) x :else y))

  (method ^:public ^:static abs ^long [^long x] (^[long] Math/abs x))

  (method ^:public ^:static abs ^double [^double x]
    (^[double] Math/abs x))

  (method ^:public ^:static abs ^Number [x]
    (.abs (Numbers/ops x) (cast Number x)))

  ;; The operators of doc/classes/SPEC.md §5.4 that Clojure lacks (Arbace): the :inline
  ;; expansions of arbace.core's bit-and-int ... unchecked-remainder call these, and the
  ;; compiler emits the primitive ones as instructions (arbace.lang.Intrinsics). The int and
  ;; float operators have one method each, so the compiler converts any argument to the
  ;; parameter type as for unchecked-add-int (RT.intCast, RT.floatCast); unchecked-divide and
  ;; unchecked-remainder are long or double by their operands, as Java's / and %.

  (method ^:public ^:static andInt ^int [^int x ^int y] (bit-and-int x y))

  (method ^:public ^:static orInt ^int [^int x ^int y] (bit-or-int x y))

  (method ^:public ^:static xorInt ^int [^int x ^int y] (bit-xor-int x y))

  (method ^:public ^:static notInt ^int [^int x] (bit-not-int x))

  (method ^:public ^:static unchecked_float_add ^float [^float x ^float y]
    (unchecked-add-float x y))

  (method ^:public ^:static unchecked_float_subtract ^float [^float x ^float y]
    (unchecked-subtract-float x y))

  (method ^:public ^:static unchecked_float_multiply ^float [^float x ^float y]
    (unchecked-multiply-float x y))

  (method ^:public ^:static unchecked_float_divide ^float [^float x ^float y]
    (unchecked-divide-float x y))

  (method ^:public ^:static unchecked_float_remainder ^float [^float x ^float y]
    (unchecked-remainder-float x y))

  (method ^:public ^:static unchecked_float_negate ^float [^float x]
    (unchecked-negate-float x))

  (method ^:static floating ^boolean [x] (or (instance? Double x) (instance? Float x)))

  (method ^:public ^:static unchecked_divide ^long [^long x ^long y] (unchecked-divide x y))

  (method ^:public ^:static unchecked_divide ^double [^double x ^double y] (unchecked-divide x y))

  (method ^:public ^:static unchecked_divide ^double [^long x ^double y] (unchecked-divide x y))

  (method ^:public ^:static unchecked_divide ^double [^double x ^long y] (unchecked-divide x y))

  (method ^:public ^:static unchecked_divide ^double [^double x y]
    (unchecked-divide x (RT/doubleCast y)))

  (method ^:public ^:static unchecked_divide ^double [x ^double y]
    (unchecked-divide (RT/doubleCast x) y))

  (method ^:public ^:static unchecked_divide ^Number [^long x y]
    (if (Numbers/floating y)
        (Double/valueOf (unchecked-divide x (RT/doubleCast y)))
        (Long/valueOf (unchecked-divide x (RT/longCast y)))))

  (method ^:public ^:static unchecked_divide ^Number [x ^long y]
    (if (Numbers/floating x)
        (Double/valueOf (unchecked-divide (RT/doubleCast x) y))
        (Long/valueOf (unchecked-divide (RT/longCast x) y))))

  (method ^:public ^:static unchecked_divide ^Number [x y]
    (if (or (Numbers/floating x) (Numbers/floating y))
        (Double/valueOf (unchecked-divide (RT/doubleCast x) (RT/doubleCast y)))
        (Long/valueOf (unchecked-divide (RT/longCast x) (RT/longCast y)))))

  (method ^:public ^:static unchecked_remainder ^long [^long x ^long y] (unchecked-remainder x y))

  (method ^:public ^:static unchecked_remainder ^double [^double x ^double y]
    (unchecked-remainder x y))

  (method ^:public ^:static unchecked_remainder ^double [^long x ^double y]
    (unchecked-remainder x y))

  (method ^:public ^:static unchecked_remainder ^double [^double x ^long y]
    (unchecked-remainder x y))

  (method ^:public ^:static unchecked_remainder ^double [^double x y]
    (unchecked-remainder x (RT/doubleCast y)))

  (method ^:public ^:static unchecked_remainder ^double [x ^double y]
    (unchecked-remainder (RT/doubleCast x) y))

  (method ^:public ^:static unchecked_remainder ^Number [^long x y]
    (if (Numbers/floating y)
        (Double/valueOf (unchecked-remainder x (RT/doubleCast y)))
        (Long/valueOf (unchecked-remainder x (RT/longCast y)))))

  (method ^:public ^:static unchecked_remainder ^Number [x ^long y]
    (if (Numbers/floating x)
        (Double/valueOf (unchecked-remainder (RT/doubleCast x) y))
        (Long/valueOf (unchecked-remainder (RT/longCast x) y))))

  (method ^:public ^:static unchecked_remainder ^Number [x y]
    (if (or (Numbers/floating x) (Numbers/floating y))
        (Double/valueOf (unchecked-remainder (RT/doubleCast x) (RT/doubleCast y)))
        (Long/valueOf (unchecked-remainder (RT/longCast x) (RT/longCast y))))))
