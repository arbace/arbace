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
;; /* chouser Jun 23, 2010 */
;;
;; Converted from clojure/lang/BigInt.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:final BigInt
  :extends Number
  :implements [IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 5097771279236135022)

  (field ^:public ^:final ^long lpart)

  (field ^:public ^:final ^BigInteger bipart)

  (field ^:public ^:static ^:final ^BigInt ZERO (BigInt. 0 nil))

  (field ^:public ^:static ^:final ^BigInt ONE (BigInt. 1 nil))

  (method ^:public hashCode ^int [this]
    (if (nil? bipart)
        (unchecked-int (bit-xor (.-lpart this) (unsigned-bit-shift-right (.-lpart this) 32)))
        (.hashCode bipart)))

  (method ^:public hasheq ^int [this]
    (if (nil? bipart) (Murmur3/hashLong lpart) (.hashCode bipart)))

  (method ^:public equals ^boolean [this obj]
    (cond
      (identical? this obj) true
      (instance? BigInt obj)
        (let [o (cast BigInt obj)]
          (if (nil? bipart)
              (and (nil? (.-bipart o)) (== (.-lpart this) (.-lpart o)))
              (and (some? (.-bipart o)) (.equals (.-bipart this) (.-bipart o)))))
      :else false))

  (constructor ^:private [this ^long lpart ^BigInteger bipart]
    (set! (.-lpart this) lpart)
    (set! (.-bipart this) bipart))

  (method ^:public ^:static fromBigInteger ^BigInt [^BigInteger val]
    (if (< (.bitLength val) 64) (BigInt. (.longValue val) nil) (BigInt. 0 val)))

  (method ^:public ^:static fromLong ^BigInt [^long val]
    (BigInt. val nil))

  (method ^:public toBigInteger ^BigInteger [this]
    (if (nil? bipart) (BigInteger/valueOf lpart) bipart))

  (method ^:public toBigDecimal ^BigDecimal [this]
    (if (nil? bipart) (BigDecimal/valueOf lpart) (BigDecimal. bipart)))

  (method ^:public intValue ^int [this]
    (if (nil? bipart) (unchecked-int lpart) (.intValue bipart)))

  (method ^:public longValue ^long [this]
    (if (nil? bipart) lpart (.longValue bipart)))

  (method ^:public floatValue ^float [this]
    (if (nil? bipart) lpart (.floatValue bipart)))

  (method ^:public doubleValue ^double [this]
    (if (nil? bipart) lpart (.doubleValue bipart)))

  (method ^:public byteValue ^byte [this]
    (if (nil? bipart) (unchecked-byte lpart) (.byteValue bipart)))

  (method ^:public shortValue ^short [this]
    (if (nil? bipart) (unchecked-short lpart) (.shortValue bipart)))

  (method ^:public ^:static valueOf ^BigInt [^long val] (BigInt. val nil))

  (method ^:public toString ^String [this]
    (if (nil? bipart) (^[long] String/valueOf lpart) (.toString bipart)))

  (method ^:public bitLength ^int [this]
    (.bitLength (.toBigInteger this)))

  (method ^:public add ^BigInt [this ^BigInt y]
    (when (and (nil? bipart) (nil? (.-bipart y)))
      (let [ret (unchecked-add lpart (.-lpart y))]
        (when (or (>= (bit-xor ret lpart) 0) (>= (bit-xor ret (.-lpart y)) 0))
          (return (BigInt/valueOf ret)))))
    (BigInt/fromBigInteger (.add (.toBigInteger this) (.toBigInteger y))))

  (method ^:public multiply ^BigInt [this ^BigInt y]
    (when (and (nil? bipart) (nil? (.-bipart y)))
      (let [ret (unchecked-multiply lpart (.-lpart y))]
        (when (or (== (.-lpart y) 0)
                  (and (== (unchecked-divide ret (.-lpart y)) lpart)
                       (not (== lpart Long/MIN_VALUE))))
          (return (BigInt/valueOf ret)))))
    (BigInt/fromBigInteger (.multiply (.toBigInteger this) (.toBigInteger y))))

  (method ^:public quotient ^BigInt [this ^BigInt y]
    (if (and (nil? bipart) (nil? (.-bipart y)))
        (if (and (== lpart Long/MIN_VALUE) (== (.-lpart y) -1))
            (BigInt/fromBigInteger (.negate (.toBigInteger this)))
            (BigInt/valueOf (unchecked-divide lpart (.-lpart y))))
        (BigInt/fromBigInteger (.divide (.toBigInteger this) (.toBigInteger y)))))

  (method ^:public remainder ^BigInt [this ^BigInt y]
    (if (and (nil? bipart) (nil? (.-bipart y)))
        (BigInt/valueOf (unchecked-remainder lpart (.-lpart y)))
        (BigInt/fromBigInteger (.remainder (.toBigInteger this) (.toBigInteger y)))))

  (method ^:public lt ^boolean [this ^BigInt y]
    (if (and (nil? bipart) (nil? (.-bipart y)))
        (< lpart (.-lpart y))
        (< (.compareTo (.toBigInteger this) (.toBigInteger y)) 0))))
