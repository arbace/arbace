;; jrt: Java's primitive conversions (C2G-SPEC §7.4) and the decimal forms of float and
;; double (Float.toString, Double.toString, §7.5).
(in-ns 'go.arbace.jrt)

(go/file "numconv.go"
  :imports [[math "math"] [strconv "strconv"] [strings "strings"]])

;; ---------------------------------------------------------------------------------------
;; Constants Go's constants cannot express (§6.1)

(go/var
  [^{:doc "NaN32 ... NegZero64 are the float and double values Go constants cannot express:
Float.NaN and Double.NaN with Java's canonical bits, the infinities, the negative zeros.\n"}
   NaN32 (math/Float32frombits 0x7fc00000)]
  [NaN64 (math/Float64frombits 0x7ff8000000000000)]
  [PosInf32 (conv float32 (math/Inf 1))]
  [NegInf32 (conv float32 (math/Inf -1))]
  [PosInf64 (math/Inf 1)]
  [NegInf64 (math/Inf -1)]
  [NegZero32 (math/Float32frombits 0x80000000)]
  [NegZero64 (math/Float64frombits 0x8000000000000000)])

;; ---------------------------------------------------------------------------------------
;; Float to integer conversions (d2i, d2l, f2i, f2l): NaN gives 0, out of range saturates

(go/func D2I "D2I is d2i, Java's (int) of a double.\n" ^int32 [^float64 x]
  (cond
    (!= x x) (return 0)
    (>= x 2147483647.0) (return 2147483647)
    (<= x -2147483648.0) (return -2147483648))
  (conv int32 x))

(go/func D2L "D2L is d2l, Java's (long) of a double.\n" ^int64 [^float64 x]
  (cond
    (!= x x) (return 0)
    (>= x 9223372036854775807.0) (return 9223372036854775807)
    (<= x -9223372036854775808.0) (return -9223372036854775808))
  (conv int64 x))

(go/func F2I "F2I is f2i, Java's (int) of a float.\n" ^int32 [^float32 x]
  (D2I (conv float64 x)))

(go/func F2L "F2L is f2l, Java's (long) of a float.\n" ^int64 [^float32 x]
  (D2L (conv float64 x)))

;; ---------------------------------------------------------------------------------------
;; The natives of Float and Double (§9.1)

(go/func Double_DoubleToRawLongBits_D__J_native ^int64 [^float64 x]
  (conv int64 (math/Float64bits x)))
(go/func Double_LongBitsToDouble_J__D_native ^float64 [^int64 b]
  (math/Float64frombits (conv uint64 b)))
(go/func Float_FloatToRawIntBits_F__I_native ^int32 [^float32 x]
  (conv int32 (math/Float32bits x)))
(go/func Float_IntBitsToFloat_I__F_native ^float32 [^int32 b]
  (math/Float32frombits (conv uint32 b)))

;; ---------------------------------------------------------------------------------------
;; Double.toString and Float.toString (JDK 19 and later: Raffaello Giulietti's shortest
;; decimal). R is the set of decimals that round to x; m its minimal length; the chosen
;; decimal is the one of length m closest to x (of length 1 or 2 when m is 1), formatted
;; plain for 10^-3 <= |d| < 10^7, else as d.dddEn, always with a digit after the point.

(go/func FormatDouble "FormatDouble is Double.toString(d) as Go's text.\n"
  ^string [^float64 d]
  (javaDecimal (conv float64 d) 64))

(go/func FormatFloat "FormatFloat is Float.toString(f) as Go's text.\n"
  ^string [^float32 f]
  (javaDecimal (conv float64 f) 32))

(go/func javaDecimal ^string [^float64 v ^int bits]
  (cond
    (!= v v) (return "NaN")
    (math/IsInf v 1) (return "Infinity")
    (math/IsInf v -1) (return "-Infinity")
    (== v 0)
    (do
      (when (math/Signbit v)
        (return "-0.0"))
      (return "0.0")))
  (let [sign ""
        a v]
    (when (< v 0)
      (set! sign "-")
      (set! a (- v)))
    ;; the shortest digits, and the two-digit closest when the shortest has one digit
    (let [e (strconv/FormatFloat a \e -1 bits)
          (values digits exp) (splitE e)]
      (when (== (len digits) 1)
        (set! (values digits exp) (splitE (strconv/FormatFloat a \e 1 bits))))
      (set! digits (strings/TrimRight digits "0"))
      (when (== digits "")
        (set! digits "0"))
      ;; the value is D.IGITS x 10^exp: the point goes after exp+1 digits
      (if (and (>= exp -3) (< exp 7))
        (let [point (+ exp 1)]
          (cond
            (<= point 0)
            (return (+ sign "0." (strings/Repeat "0" (- point)) digits))
            (>= point (len digits))
            (return (+ sign digits (strings/Repeat "0" (- point (len digits))) ".0"))
            :else
            (return (+ sign (subslice digits _ point) "." (subslice digits point)))))
        (let [frac (subslice digits 1)]
          (when (== frac "")
            (set! frac "0"))
          (return (+ sign (subslice digits _ 1) "." frac "E" (strconv/Itoa exp))))))))

(go/func splitE
  "splitE splits Go's d.ddde±XX into the digits without the point and the exponent.\n"
  [^string s] :results [^string digits ^int exp]
  (let [i (strings/IndexByte s \e)
        m (subslice s _ i)
        (values x _) (strconv/Atoi (subslice s (+ i 1)))]
    (return (strings/Replace m "." "" 1) x)))
