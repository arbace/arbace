;; arbace.lang.Numbers, arithmetic: the operations on Object operands over a table of the
;; number types (Long incl. the extremes, Integer, Double incl. -0.0 and NaN, Ratio, BigInt,
;; BigInteger, BigDecimal), then the primitive overloads.

(def r13 (new Ratio (biginteger "1") (biginteger "3")))
(def bigN (BigInt/fromBigInteger (biginteger "18446744073709551616")))
(def smallN (BigInt/fromLong 3))

;; checked arithmetic (NaN operands: NumbersCompare.clj): overflow throws, the mixed types contaminate (Long < BigInt < Ratio <
;; BigDecimal < Double)
(each [x [0 1 9223372036854775807 -9223372036854775808 (int 7) 1.5 r13 bigN (bigdecimal "1.50")]
       y [0 1 9223372036854775807 -9223372036854775808 (int 7) 1.5 r13 bigN (bigdecimal "1.50")]]
  ^{:sig [Object Object]} (Numbers/add x y)
  ^{:sig [Object Object]} (Numbers/minus x y)
  ^{:sig [Object Object]} (Numbers/multiply x y)
  ^{:sig [Object Object]} (Numbers/divide x y))

;; promoting arithmetic
(each [x [-1 9223372036854775807 -9223372036854775808 (int 7) 2.5 smallN]
       y [1 -1 9223372036854775807 -9223372036854775808 2.5]]
  ^{:sig [Object Object]} (Numbers/addP x y)
  ^{:sig [Object Object]} (Numbers/minusP x y)
  ^{:sig [Object Object]} (Numbers/multiplyP x y))

;; unchecked arithmetic wraps
(each [x [1 9223372036854775807 -9223372036854775808 1.5 bigN]
       y [1 -1 9223372036854775807 -9223372036854775808]]
  ^{:sig [Object Object]} (Numbers/unchecked_add x y)
  ^{:sig [Object Object]} (Numbers/unchecked_minus x y)
  ^{:sig [Object Object]} (Numbers/unchecked_multiply x y))

;; quotient and remainder truncate toward zero
(each [x [7 -7 0 -7.5 r13 bigN (bigdecimal "7.25") (biginteger "-9")]
       y [2 -2 0 1.5 r13 (bigdecimal "2.5")]]
  ^{:sig [Object Object]} (Numbers/quotient x y)
  ^{:sig [Object Object]} (Numbers/remainder x y))

;; ratios reduce, and a whole ratio becomes an integer
^{:sig [Object Object]} (Numbers/divide 6 4)
^{:sig [Object Object]} (Numbers/divide 6 -4)
^{:sig [Object Object]} (Numbers/divide -6 -3)
^{:sig [Object Object]} (Numbers/divide -9223372036854775808 -1)
^{:sig [Object Object]} (Numbers/quotient -9223372036854775808 -1)
^{:sig [Object Object]} (Numbers/remainder -9223372036854775808 -1)
^{:sig [Object Object]} (Numbers/add r13 r13)
^{:sig [Object Object]} (Numbers/add r13 (bigdecimal "1"))
^{:sig [Object Object]} (Numbers/multiply r13 3)
^{:sig [Object Object]} (Numbers/divide (bigdecimal "1") (bigdecimal "3"))
^{:sig [Object Object]} (Numbers/divide (bigdecimal "1") (bigdecimal "4"))
^{:sig [Object Object]} (Numbers/divide (bigdecimal "1.00") (bigdecimal "0.5"))
^{:sig [java.math.BigInteger java.math.BigInteger]} (Numbers/divide (biginteger "10") (biginteger "-4"))
^{:sig [java.math.BigInteger java.math.BigInteger]} (Numbers/divide (biginteger "10") (biginteger "0"))
^{:sig [java.math.BigInteger java.math.BigInteger]} (Numbers/divide (biginteger "10") (biginteger "5"))
^{:sig [java.math.BigInteger java.math.BigInteger]} (Numbers/divide (biginteger "0") (biginteger "-5"))

;; the primitive overloads
^{:sig [long long]} (Numbers/add 9223372036854775807 1)
^{:sig [long long]} (Numbers/add 1 2)
^{:sig [long long]} (Numbers/minus -9223372036854775808 1)
^{:sig [long long]} (Numbers/multiply 4611686018427387904 2)
^{:sig [long long]} (Numbers/multiply -1 -9223372036854775808)
^{:sig [long long]} (Numbers/multiply -9223372036854775808 -1)
^{:sig [long long]} (Numbers/divide 7 2)
^{:sig [long long]} (Numbers/divide 7 0)
^{:sig [long long]} (Numbers/quotient 7 2)
^{:sig [long long]} (Numbers/remainder -7 2)
^{:sig [long long]} (Numbers/quotient 7 0)
^{:sig [long long]} (Numbers/addP 9223372036854775807 1)
^{:sig [long long]} (Numbers/multiplyP 9223372036854775807 2)
^{:sig [long long]} (Numbers/minusP -9223372036854775808 1)
^{:sig [long long]} (Numbers/unchecked_add 9223372036854775807 1)
^{:sig [long long]} (Numbers/unchecked_multiply 9223372036854775807 3)
^{:sig [long long]} (Numbers/unchecked_minus -9223372036854775808 1)
^{:sig [long]} (Numbers/unchecked_inc 9223372036854775807)
^{:sig [long]} (Numbers/unchecked_dec -9223372036854775808)
^{:sig [long]} (Numbers/unchecked_minus -9223372036854775808)
^{:sig [long]} (Numbers/inc 9223372036854775807)
^{:sig [long]} (Numbers/dec -9223372036854775808)
^{:sig [long]} (Numbers/minus -9223372036854775808)
^{:sig [long]} (Numbers/incP 9223372036854775807)
^{:sig [long]} (Numbers/decP -9223372036854775808)
^{:sig [long]} (Numbers/minusP -9223372036854775808)
^{:sig [double double]} (Numbers/add 0.1 0.2)
^{:sig [double double]} (Numbers/divide 1.0 0.0)
^{:sig [double double]} (Numbers/divide -1.0 0.0)
^{:sig [double double]} (Numbers/divide 0.0 0.0)
^{:sig [double double]} (Numbers/quotient 7.5 2.0)
^{:sig [double double]} (Numbers/remainder -7.5 2.0)
^{:sig [double double]} (Numbers/quotient 7.5 0.0)
^{:sig [double double]} (Numbers/quotient 1.0E300 1.0E-10)
^{:sig [double double]} (Numbers/remainder 1.0E300 3.0)
^{:sig [long double]} (Numbers/add 1 0.5)
^{:sig [double long]} (Numbers/multiply 0.5 3)
^{:sig [Object long]} (Numbers/add r13 1)
^{:sig [Object double]} (Numbers/add r13 1.0)
^{:sig [long Object]} (Numbers/divide 1 bigN)
^{:sig [Object long]} (Numbers/divide bigN 0)
^{:sig [int int]} (Numbers/unchecked_int_add 2147483647 1)
^{:sig [int int]} (Numbers/unchecked_int_subtract -2147483648 1)
^{:sig [int int]} (Numbers/unchecked_int_multiply 65536 65536)
^{:sig [int int]} (Numbers/unchecked_int_divide -7 2)
^{:sig [int int]} (Numbers/unchecked_int_remainder -7 2)
^{:sig [int]} (Numbers/unchecked_int_negate -2147483648)
^{:sig [int]} (Numbers/unchecked_int_inc 2147483647)
^{:sig [int]} (Numbers/unchecked_int_dec -2147483648)
^{:sig [float float]} (Numbers/unchecked_float_add (float 0.1) (float 0.2))
^{:sig [float float]} (Numbers/unchecked_float_divide (float 1) (float 3))
