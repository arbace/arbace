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
;; Converted from clojure/lang/Ratio.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.math MathContext))

(defclass ^:public Ratio
  :extends Number
  :implements [Comparable]

  (field ^:private ^:static ^:final ^long serialVersionUID -576272795628662988)

  (field ^:public ^:final ^BigInteger numerator)

  (field ^:public ^:final ^BigInteger denominator)

  (constructor ^:public [this ^BigInteger numerator ^BigInteger denominator]
    (set! (.-numerator this) numerator)
    (set! (.-denominator this) denominator))

  (method ^:public equals ^boolean [this arg0]
    (and (and (and (some? arg0) (instance? Ratio arg0))
              (.equals (.-numerator (cast Ratio arg0)) numerator))
         (.equals (.-denominator (cast Ratio arg0)) denominator)))

  (method ^:public hashCode ^int [this]
    (bit-xor-int (.hashCode numerator) (.hashCode denominator)))

  (method ^:public toString ^String [this]
    (java-str (.toString numerator) "/" (.toString denominator)))

  (method ^:public intValue ^int [this]
    (unchecked-int (.doubleValue this)))

  (method ^:public longValue ^long [this]
    (.longValue (.bigIntegerValue this)))

  (method ^:public floatValue ^float [this]
    (unchecked-float (.doubleValue this)))

  (method ^:public doubleValue ^double [this]
    (.doubleValue (.decimalValue this MathContext/DECIMAL64)))

  (method ^:public decimalValue ^BigDecimal [this]
    (.decimalValue this MathContext/UNLIMITED))

  (method ^:public decimalValue ^BigDecimal [this ^MathContext mc]
    (let [numerator (BigDecimal. (.-numerator this))
          denominator (BigDecimal. (.-denominator this))]
      (.divide numerator denominator mc)))

  (method ^:public bigIntegerValue ^BigInteger [this]
    (.divide numerator denominator))

  (method ^:public compareTo ^int [this o]
    (let [other (cast Number o)] (Numbers/compare this other))))
