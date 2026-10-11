;; arbace.lang.Numbers: comparisons, equivalence, the unary operations, bit operations,
;; conversions; over a table of the number types.

(def r13 (new Ratio (biginteger "1") (biginteger "3")))
(def r22 (new Ratio (biginteger "2") (biginteger "2")))
(def bigN (BigInt/fromBigInteger (biginteger "18446744073709551616")))
(def oneN (BigInt/fromLong 1))

(each [x [1 9223372036854775807 (int 1) 1.0 -0.0 ##NaN r13 bigN (bigdecimal "1.0")]
       y [1 9223372036854775807 (int 1) 1.0 -0.0 ##NaN r13 bigN (bigdecimal "1.0")]]
  ^{:sig [Object Object]} (Numbers/lt x y)
  ^{:sig [Object Object]} (Numbers/equiv x y)
  ^{:sig [java.lang.Number java.lang.Number]} (Numbers/compare x y))

(each [x [0 -0.0 (bigdecimal "0.00") (bigdecimal "1E+2") 100]
       y [0 0.0 (bigdecimal "100") ##Inf]]
  ^{:sig [Object Object]} (Numbers/lte x y)
  ^{:sig [Object Object]} (Numbers/gt x y)
  ^{:sig [java.lang.Number java.lang.Number]} (Numbers/equal x y))

^{:sig [Object Object]} (Numbers/lt nil 1)
^{:sig [Object Object]} (Numbers/equiv 1 nil)
^{:sig [double double]} (Numbers/equiv ##NaN ##NaN)
^{:sig [double double]} (Numbers/lt -0.0 0.0)
^{:sig [long long]} (Numbers/lt 1 2)
^{:sig [long double]} (Numbers/equiv 9007199254740993 9.007199254740992E15)
^{:sig [Object Object]} (Numbers/equiv 9007199254740993 9.007199254740992E15)
^{:sig [Object Object]} (Numbers/equiv 9007199254740993 (bigdecimal "9007199254740993"))

;; max and min: NaN wins, mixed types keep the operand
(each [x [1 2.0 ##NaN r13 (bigdecimal "1.5") -0.0]
       y [1 0.0 ##NaN bigN]]
  ^{:sig [Object Object]} (Numbers/max x y)
  ^{:sig [Object Object]} (Numbers/min x y))
^{:sig [double double]} (Numbers/max -0.0 0.0)
^{:sig [double double]} (Numbers/min -0.0 0.0)
^{:sig [long long]} (Numbers/max 3 4)

;; unary
(each [x [0 -1 9223372036854775807 -9223372036854775808 (int -7) (short 5) 1.5 -0.0 ##NaN ##-Inf (float -2.5) r13 bigN (biginteger "-9") (bigdecimal "-1.50")]]
  ^{:sig [Object]} (Numbers/inc x)
  ^{:sig [Object]} (Numbers/dec x)
  ^{:sig [Object]} (Numbers/minus x)
  ^{:sig [Object]} (Numbers/incP x)
  ^{:sig [Object]} (Numbers/minusP x)
  ^{:sig [Object]} (Numbers/abs x)
  ^{:sig [Object]} (Numbers/isZero x)
  ^{:sig [Object]} (Numbers/isPos x)
  ^{:sig [Object]} (Numbers/isNeg x)
  ^{:sig [Object]} (Numbers/num x)
  (Util/hasheq x))

;; conversions
(each [x [3 -4 0.5 0.1 -0.0 1.0E-5 1.0E20 r13 r22 bigN (biginteger "12") (bigdecimal "0.125") (bigdecimal "1E+3") (bigdecimal "-2.50") (float 0.1)]]
  ^{:sig [java.lang.Number]} (Numbers/rationalize x)
  (Numbers/toRatio x))
(Numbers/reduceBigInt bigN)
(Numbers/reduceBigInt oneN)
(def r42 (new Ratio (biginteger "4") (biginteger "2")))
(.toString r42)
(Numbers/num r42)
^{:sig [java.lang.Number java.lang.Number]} (Numbers/compare r42 r22)

;; bits: Object operands must be integers
(each [x [1 -1 -9223372036854775808 (int 12) 5.5]
       y [0 3 63 64 -1]]
  ^{:sig [Object Object]} (Numbers/and x y)
  ^{:sig [Object Object]} (Numbers/xor x y)
  ^{:sig [Object Object]} (Numbers/shiftLeft x y)
  ^{:sig [Object Object]} (Numbers/shiftRight x y)
  ^{:sig [Object Object]} (Numbers/unsignedShiftRight x y))
(each [x [12 -1 -9223372036854775808]
       y [0 2 63 64]]
  ^{:sig [long long]} (Numbers/or x y)
  ^{:sig [long long]} (Numbers/andNot x y)
  ^{:sig [long long]} (Numbers/setBit x y)
  ^{:sig [long long]} (Numbers/clearBit x y)
  ^{:sig [long long]} (Numbers/flipBit x y)
  ^{:sig [long long]} (Numbers/testBit x y))
^{:sig [Object]} (Numbers/not 0)
^{:sig [Object]} (Numbers/not -9223372036854775808)
^{:sig [Object]} (Numbers/not 1.5)
^{:sig [int int]} (Numbers/shiftLeftInt 1 31)
^{:sig [int int]} (Numbers/shiftLeftInt 1 32)
^{:sig [int int]} (Numbers/shiftRightInt -8 1)
^{:sig [int int]} (Numbers/unsignedShiftRightInt -8 1)
^{:sig [int]} (Numbers/notInt 0)

;; arrays from collections
(def v ^{:sig ["Object[]"]} (PersistentVector/create (array Object 1 2.5 (int 3))))
(def la ^{:sig [Object]} (Numbers/long_array v))
(RT/seq la)
(RT/count la)
(def da ^{:sig [Object]} (Numbers/double_array v))
(RT/count da)
^{:sig [int Object]} (Numbers/int_array 3 7)
(def ia ^{:sig [int Object]} (Numbers/int_array 4 v))
(RT/seq ia)
(RT/aget ia 3)
(def ba ^{:sig [Object]} (Numbers/byte_array v))
(RT/aget ba 1)
