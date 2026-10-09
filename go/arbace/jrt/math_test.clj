;; jrt's tests: Math, StrictMath, the conversions and Double/Float.toString against the JVM
;; (testdata/tostring.txt, doubles.txt, floats.txt, ints.txt, from test/jrt/testdata_numbers.clj).
(in-ns 'go.arbace.jrt)

(go/file "math_test.go"
  :imports [[math "math"] [strconv "strconv"] [strings "strings"] [testing "testing"]])

(go/func mhd "mhd: a double result as the files write it (canonical NaN).\n" ^string [^float64 d]
  (when (!= d d) (set! d NaN64))
  (let [s (strconv/FormatUint (math/Float64bits d) 16)]
    (+ (strings/Repeat "0" (- 16 (len s))) s)))

(go/func mhf ^string [^float32 f]
  (when (!= f f) (set! f NaN32))
  (let [s (strconv/FormatUint (conv uint64 (math/Float32bits f)) 16)]
    (+ (strings/Repeat "0" (- 8 (len s))) s)))

(go/func md ^float64 [^string s]
  (let [(values b _) (strconv/ParseUint s 16 64)] (math/Float64frombits b)))
(go/func mf ^float32 [^string s]
  (let [(values b _) (strconv/ParseUint s 16 32)] (math/Float32frombits (conv uint32 b))))
(go/func mi ^int32 [^string s] (let [(values n _) (strconv/ParseInt s 10 32)] (conv int32 n)))
(go/func ml ^int64 [^string s] (let [(values n _) (strconv/ParseInt s 10 64)] n))
(go/func mlres ^string [^int64 n] (strconv/FormatInt n 10))

(go/func TestToString "TestToString: Double.toString and Float.toString (FormatDouble, FormatFloat).\n"
  [^{:tag (* testing/T)} t]
  (let [n 0]
    (range [_ c (readCases t "tostring.txt")]
      (switch (aget c 0)
        (case ["Double.toString"] (check t c (FormatDouble (md (aget c 1)))))
        (case ["Float.toString"] (check t c (FormatFloat (mf (aget c 1))))))
      (inc! n))
    (failuresReport t)
    (.Logf t "%d cases" n)))

(go/var ^{:tag (map string (func [float64] [string]))} doubleOps
  (lit (map string (func [float64] [string]))
    ["abs" (fn ^string [^float64 d] (mhd (Math_Abs_D__D d)))]
    ["ceil" (fn ^string [^float64 d] (mhd (Math_Ceil_D__D d)))]
    ["floor" (fn ^string [^float64 d] (mhd (Math_Floor_D__D d)))]
    ["rint" (fn ^string [^float64 d] (mhd (Math_Rint_D__D d)))]
    ["sqrt" (fn ^string [^float64 d] (mhd (StrictMath_Sqrt_D__D d)))]
    ["Math.sqrt" (fn ^string [^float64 d] (mhd (Math_Sqrt_D__D d)))]
    ["cbrt" (fn ^string [^float64 d] (mhd (StrictMath_Cbrt_D__D d)))]
    ["Math.cbrt" (fn ^string [^float64 d] (mhd (Math_Cbrt_D__D d)))]
    ["log" (fn ^string [^float64 d] (mhd (StrictMath_Log_D__D d)))]
    ["pow1" (fn ^string [^float64 d] (mhd (StrictMath_Pow_D_D__D 1.0000001 d)))]
    ["Math.log" (fn ^string [^float64 d] (mhd (Math_Log_D__D d)))]
    ["getExponent" (fn ^string [^float64 d] (ires (Math_GetExponent_D__I d)))]
    ["nextUp" (fn ^string [^float64 d] (mhd (Math_NextUp_D__D d)))]
    ["nextDown" (fn ^string [^float64 d] (mhd (Math_NextDown_D__D d)))]
    ["ulp" (fn ^string [^float64 d] (mhd (Math_Ulp_D__D d)))]
    ["signum" (fn ^string [^float64 d] (mhd (Math_Signum_D__D d)))]
    ["round" (fn ^string [^float64 d] (mlres (Math_Round_D__J d)))]
    ["d2i" (fn ^string [^float64 d] (ires (D2I d)))]
    ["d2l" (fn ^string [^float64 d] (mlres (D2L d)))]
    ["d2f" (fn ^string [^float64 d] (mhf (conv float32 d)))]
    ["scalb" (fn ^string [^float64 d] (mhd (Math_Scalb_D_I__D d 1000)))]
    ["scalb-1100" (fn ^string [^float64 d] (mhd (Math_Scalb_D_I__D d -1100)))]
    ["scalb-3" (fn ^string [^float64 d] (mhd (Math_Scalb_D_I__D d -3)))]
    ["sin" (fn ^string [^float64 d] (mhd (StrictMath_Sin_D__D d)))]
    ["Math.sin" (fn ^string [^float64 d] (mhd (Math_Sin_D__D d)))]
    ["cos" (fn ^string [^float64 d] (mhd (StrictMath_Cos_D__D d)))]
    ["Math.cos" (fn ^string [^float64 d] (mhd (Math_Cos_D__D d)))]
    ["tan" (fn ^string [^float64 d] (mhd (StrictMath_Tan_D__D d)))]
    ["Math.tan" (fn ^string [^float64 d] (mhd (Math_Tan_D__D d)))]
    ["asin" (fn ^string [^float64 d] (mhd (StrictMath_Asin_D__D d)))]
    ["Math.asin" (fn ^string [^float64 d] (mhd (Math_Asin_D__D d)))]
    ["acos" (fn ^string [^float64 d] (mhd (StrictMath_Acos_D__D d)))]
    ["Math.acos" (fn ^string [^float64 d] (mhd (Math_Acos_D__D d)))]
    ["atan" (fn ^string [^float64 d] (mhd (StrictMath_Atan_D__D d)))]
    ["Math.atan" (fn ^string [^float64 d] (mhd (Math_Atan_D__D d)))]
    ["exp" (fn ^string [^float64 d] (mhd (StrictMath_Exp_D__D d)))]
    ["Math.exp" (fn ^string [^float64 d] (mhd (Math_Exp_D__D d)))]
    ["log10" (fn ^string [^float64 d] (mhd (StrictMath_Log10_D__D d)))]
    ["Math.log10" (fn ^string [^float64 d] (mhd (Math_Log10_D__D d)))]
    ["log1p" (fn ^string [^float64 d] (mhd (StrictMath_Log1p_D__D d)))]
    ["Math.log1p" (fn ^string [^float64 d] (mhd (Math_Log1p_D__D d)))]
    ["expm1" (fn ^string [^float64 d] (mhd (StrictMath_Expm1_D__D d)))]
    ["Math.expm1" (fn ^string [^float64 d] (mhd (Math_Expm1_D__D d)))]
    ["sinh" (fn ^string [^float64 d] (mhd (StrictMath_Sinh_D__D d)))]
    ["Math.sinh" (fn ^string [^float64 d] (mhd (Math_Sinh_D__D d)))]
    ["cosh" (fn ^string [^float64 d] (mhd (StrictMath_Cosh_D__D d)))]
    ["Math.cosh" (fn ^string [^float64 d] (mhd (Math_Cosh_D__D d)))]
    ["tanh" (fn ^string [^float64 d] (mhd (StrictMath_Tanh_D__D d)))]
    ["Math.tanh" (fn ^string [^float64 d] (mhd (Math_Tanh_D__D d)))]
    ["toRadians" (fn ^string [^float64 d] (mhd (Math_ToRadians_D__D d)))]
    ["toDegrees" (fn ^string [^float64 d] (mhd (Math_ToDegrees_D__D d)))]))

(go/var ^{:tag (map string (func [float64 float64] [string]))} doubleOps2
  (lit (map string (func [float64 float64] [string]))
    ["max" (fn ^string [^float64 a ^float64 b] (mhd (Math_Max_D_D__D a b)))]
    ["min" (fn ^string [^float64 a ^float64 b] (mhd (Math_Min_D_D__D a b)))]
    ["copySign" (fn ^string [^float64 a ^float64 b] (mhd (Math_CopySign_D_D__D a b)))]
    ["nextAfter" (fn ^string [^float64 a ^float64 b] (mhd (Math_NextAfter_D_D__D a b)))]
    ["rem" (fn ^string [^float64 a ^float64 b] (mhd (math/Mod a b)))]
    ["fma" (fn ^string [^float64 a ^float64 b] (mhd (Math_Fma_D_D_D__D a b 1.0)))]
    ["pow" (fn ^string [^float64 a ^float64 b] (mhd (StrictMath_Pow_D_D__D a b)))]
    ["Math.pow" (fn ^string [^float64 a ^float64 b] (mhd (Math_Pow_D_D__D a b)))]
    ["atan2" (fn ^string [^float64 a ^float64 b] (mhd (StrictMath_Atan2_D_D__D a b)))]
    ["Math.atan2" (fn ^string [^float64 a ^float64 b] (mhd (Math_Atan2_D_D__D a b)))]
    ["hypot" (fn ^string [^float64 a ^float64 b] (mhd (StrictMath_Hypot_D_D__D a b)))]
    ["Math.hypot" (fn ^string [^float64 a ^float64 b] (mhd (Math_Hypot_D_D__D a b)))]
    ["IEEEremainder" (fn ^string [^float64 a ^float64 b] (mhd (StrictMath_IEEEremainder_D_D__D a b)))]
    ["Math.IEEEremainder" (fn ^string [^float64 a ^float64 b] (mhd (Math_IEEEremainder_D_D__D a b)))]
    ["strictCopySign" (fn ^string [^float64 a ^float64 b] (mhd (StrictMath_CopySign_D_D__D a b)))]
    ["clampD" (fn ^string [^float64 a ^float64 b] (mhd (Math_Clamp_D_D_D__D 0.25 (Math_Min_D_D__D a b) (Math_Max_D_D__D a b))))]
    ["clampDbad" (fn ^string [^float64 a ^float64 b] (mhd (Math_Clamp_D_D_D__D 0.25 (Math_Max_D_D__D a b) (Math_Min_D_D__D a b))))]))

(go/func TestDoubles
  "TestDoubles: Math's and StrictMath's double functions. jrt's Math functions of FdLibm are
StrictMath's; the JVM's Math may use intrinsics that differ in the last bits: those cases
(Math.*) are counted and logged, not failed, and must be within one ulp of StrictMath's, two
for sinh, cosh and tanh (whose specification allows 2.5 ulps of error, against 1).\n"
  [^{:tag (* testing/T)} t]
  (let [n 0
        intrinsic (make (map string int))
        maxUlps (make (map string int64))]
    (range [_ c (readCases t "doubles.txt")]
      (let [op (aget c 0)
            ^{:tag (func [] [string])} f nil]
        (if (== (len c) 3)
          (let [g (aget doubleOps op)
                a (md (aget c 1))]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (set! f (fn ^string [] (g a))))
          (let [g (aget doubleOps2 op)
                a (md (aget c 1))
                b (md (aget c 2))]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (set! f (fn ^string [] (g a b)))))
        (inc! n)
        (if (strings/HasPrefix op "Math.")
          (let [got (res f)
                want (aget c (- (len c) 1))]
            (when (!= got want)
              (aset intrinsic op (+ (aget intrinsic op) 1))
              (let [d (- (conv int64 (math/Float64bits (md got))) (conv int64 (math/Float64bits (md want))))
                    tol (conv int64 1)]
                (when (< d 0) (set! d (- d)))
                (when (> d (aget maxUlps op)) (aset maxUlps op d))
                (when (strings/HasSuffix op "h") (set! tol 2))
                (when (> d tol)
                  (.Errorf t "%v: got %s, want %s: %d ulps apart" c got want d)))))
          (check t c (res f)))))
    (range [op k intrinsic]
      (.Logf t "%s: the JVM's Math differs from StrictMath (jrt) in %d cases, by at most %d ulps" op k (aget maxUlps op)))
    (failuresReport t)
    (.Logf t "%d cases" n)))

(go/var ^{:tag (map string (func [float32] [string]))} floatOps
  (lit (map string (func [float32] [string]))
    ["fabs" (fn ^string [^float32 f] (mhf (Math_Abs_F__F f)))]
    ["fgetExponent" (fn ^string [^float32 f] (ires (Math_GetExponent_F__I f)))]
    ["fround" (fn ^string [^float32 f] (ires (Math_Round_F__I f)))]
    ["fnextUp" (fn ^string [^float32 f] (mhf (Math_NextUp_F__F f)))]
    ["fulp" (fn ^string [^float32 f] (mhf (Math_Ulp_F__F f)))]
    ["fsignum" (fn ^string [^float32 f] (mhf (Math_Signum_F__F f)))]
    ["f2i" (fn ^string [^float32 f] (ires (F2I f)))]
    ["f2l" (fn ^string [^float32 f] (mlres (F2L f)))]
    ["f2d" (fn ^string [^float32 f] (mhd (conv float64 f)))]
    ["fscalb" (fn ^string [^float32 f] (mhf (Math_Scalb_F_I__F f 100)))]
    ["fscalb-160" (fn ^string [^float32 f] (mhf (Math_Scalb_F_I__F f -160)))]
    ["fnextDown" (fn ^string [^float32 f] (mhf (Math_NextDown_F__F f)))]))

(go/var ^{:tag (map string (func [float32 float32] [string]))} floatOps2
  (lit (map string (func [float32 float32] [string]))
    ["fmax" (fn ^string [^float32 a ^float32 b] (mhf (Math_Max_F_F__F a b)))]
    ["fmin" (fn ^string [^float32 a ^float32 b] (mhf (Math_Min_F_F__F a b)))]
    ["fcopySign" (fn ^string [^float32 a ^float32 b] (mhf (Math_CopySign_F_F__F a b)))]
    ["fnextAfter" (fn ^string [^float32 a ^float32 b] (mhf (Math_NextAfter_F_D__F a (conv float64 b))))]
    ["fstrictCopySign" (fn ^string [^float32 a ^float32 b] (mhf (StrictMath_CopySign_F_F__F a b)))]
    ["ffma" (fn ^string [^float32 a ^float32 b] (mhf (Math_Fma_F_F_F__F a b 1.5)))]
    ["fclamp" (fn ^string [^float32 a ^float32 b]
                (mhf (Math_Clamp_F_F_F__F 0.25 (Math_Min_F_F__F a b) (Math_Max_F_F__F a b))))]))

(go/func TestFloats [^{:tag (* testing/T)} t]
  (let [n 0]
    (range [_ c (readCases t "floats.txt")]
      (let [op (aget c 0)]
        (if (== (len c) 3)
          (let [g (aget floatOps op)
                a (mf (aget c 1))]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (check t c (res (fn ^string [] (g a)))))
          (let [g (aget floatOps2 op)
                a (mf (aget c 1))
                b (mf (aget c 2))]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (check t c (res (fn ^string [] (g a b))))))
        (inc! n)))
    (failuresReport t)
    (.Logf t "%d cases" n)))

(go/var ^{:tag (map string (func [string] [string]))} intOps
  (lit (map string (func [string] [string]))
    ["Integer.toString" (fn ^string [^string s] (.String (StrOfInt (mi s))))]
    ["Long.toString" (fn ^string [^string s] (.String (StrOfLong (ml s))))]
    ["i2c" (fn ^string [^string s] (ires (conv int32 (conv uint16 (mi s)))))]
    ["i2s" (fn ^string [^string s] (ires (conv int32 (conv int16 (mi s)))))]
    ["i2b" (fn ^string [^string s] (ires (conv int32 (conv int8 (mi s)))))]
    ["iabs" (fn ^string [^string s] (ires (Math_Abs_I__I (mi s))))]
    ["incrementExactI" (fn ^string [^string s] (ires (Math_IncrementExact_I__I (mi s))))]
    ["decrementExactI" (fn ^string [^string s] (ires (Math_DecrementExact_I__I (mi s))))]
    ["negateExactI" (fn ^string [^string s] (ires (Math_NegateExact_I__I (mi s))))]
    ["absExactI" (fn ^string [^string s] (ires (Math_AbsExact_I__I (mi s))))]
    ["i2f" (fn ^string [^string s] (mhf (conv float32 (mi s))))]
    ["i2d" (fn ^string [^string s] (mhd (conv float64 (mi s))))]
    ["labs" (fn ^string [^string s] (mlres (Math_Abs_J__J (ml s))))]
    ["l2i" (fn ^string [^string s] (ires (conv int32 (ml s))))]
    ["incrementExactJ" (fn ^string [^string s] (mlres (Math_IncrementExact_J__J (ml s))))]
    ["decrementExactJ" (fn ^string [^string s] (mlres (Math_DecrementExact_J__J (ml s))))]
    ["negateExactJ" (fn ^string [^string s] (mlres (Math_NegateExact_J__J (ml s))))]
    ["absExactJ" (fn ^string [^string s] (mlres (Math_AbsExact_J__J (ml s))))]
    ["toIntExact" (fn ^string [^string s] (ires (Math_ToIntExact_J__I (ml s))))]
    ["l2f" (fn ^string [^string s] (mhf (conv float32 (ml s))))]
    ["l2d" (fn ^string [^string s] (mhd (conv float64 (ml s))))]))

(go/var ^{:tag (map string (func [string string] [string]))} intOps2
  (lit (map string (func [string string] [string]))
    ["addExactI" (fn ^string [^string a ^string b] (ires (Math_AddExact_I_I__I (mi a) (mi b))))]
    ["subtractExactI" (fn ^string [^string a ^string b] (ires (Math_SubtractExact_I_I__I (mi a) (mi b))))]
    ["multiplyExactI" (fn ^string [^string a ^string b] (ires (Math_MultiplyExact_I_I__I (mi a) (mi b))))]
    ["floorDivI" (fn ^string [^string a ^string b] (ires (Math_FloorDiv_I_I__I (mi a) (mi b))))]
    ["floorModI" (fn ^string [^string a ^string b] (ires (Math_FloorMod_I_I__I (mi a) (mi b))))]
    ["ceilDivI" (fn ^string [^string a ^string b] (ires (Math_CeilDiv_I_I__I (mi a) (mi b))))]
    ["ceilModI" (fn ^string [^string a ^string b] (ires (Math_CeilMod_I_I__I (mi a) (mi b))))]
    ["maxI" (fn ^string [^string a ^string b] (ires (Math_Max_I_I__I (mi a) (mi b))))]
    ["minI" (fn ^string [^string a ^string b] (ires (Math_Min_I_I__I (mi a) (mi b))))]
    ["divideExactI" (fn ^string [^string a ^string b] (ires (Math_DivideExact_I_I__I (mi a) (mi b))))]
    ["floorDivExactI" (fn ^string [^string a ^string b] (ires (Math_FloorDivExact_I_I__I (mi a) (mi b))))]
    ["ceilDivExactI" (fn ^string [^string a ^string b] (ires (Math_CeilDivExact_I_I__I (mi a) (mi b))))]
    ["multiplyFull" (fn ^string [^string a ^string b] (mlres (Math_MultiplyFull_I_I__J (mi a) (mi b))))]
    ["unsignedMultiplyExactI" (fn ^string [^string a ^string b] (ires (Math_UnsignedMultiplyExact_I_I__I (mi a) (mi b))))]
    ["powExactI" (fn ^string [^string a ^string b] (ires (Math_PowExact_I_I__I (mi a) (pexp (ml b) 40 3))))]
    ["unsignedPowExactI" (fn ^string [^string a ^string b] (ires (Math_UnsignedPowExact_I_I__I (mi a) (pexp (ml b) 40 3))))]
    ["powExactIsmall" (fn ^string [^string a ^string b] (ires (Math_PowExact_I_I__I (- (pexp (ml a) 13 0) 6) (pexp (ml b) 34 0))))]
    ["idiv" (fn ^string [^string a ^string b] (ires (/ (mi a) (mi b))))]
    ["irem" (fn ^string [^string a ^string b] (ires (% (mi a) (mi b))))]
    ["addExactJ" (fn ^string [^string a ^string b] (mlres (Math_AddExact_J_J__J (ml a) (ml b))))]
    ["subtractExactJ" (fn ^string [^string a ^string b] (mlres (Math_SubtractExact_J_J__J (ml a) (ml b))))]
    ["multiplyExactJ" (fn ^string [^string a ^string b] (mlres (Math_MultiplyExact_J_J__J (ml a) (ml b))))]
    ["multiplyExactJI" (fn ^string [^string a ^string b] (mlres (Math_MultiplyExact_J_I__J (ml a) (conv int32 (ml b)))))]
    ["floorDivJ" (fn ^string [^string a ^string b] (mlres (Math_FloorDiv_J_J__J (ml a) (ml b))))]
    ["floorModJ" (fn ^string [^string a ^string b] (mlres (Math_FloorMod_J_J__J (ml a) (ml b))))]
    ["floorModJI" (fn ^string [^string a ^string b] (ires (Math_FloorMod_J_I__I (ml a) (conv int32 (ml b)))))]
    ["ceilDivJ" (fn ^string [^string a ^string b] (mlres (Math_CeilDiv_J_J__J (ml a) (ml b))))]
    ["ceilModJ" (fn ^string [^string a ^string b] (mlres (Math_CeilMod_J_J__J (ml a) (ml b))))]
    ["multiplyHigh" (fn ^string [^string a ^string b] (mlres (Math_MultiplyHigh_J_J__J (ml a) (ml b))))]
    ["unsignedMultiplyHigh" (fn ^string [^string a ^string b] (mlres (Math_UnsignedMultiplyHigh_J_J__J (ml a) (ml b))))]
    ["clampJII" (fn ^string [^string a ^string b]
                  (let [x (ml a) y (ml b)]
                    (ires (Math_Clamp_J_I_I__I x (conv int32 (min x y)) (conv int32 (max x y))))))]
    ["clampJJJ" (fn ^string [^string a ^string b]
                  (let [x (ml a) y (ml b)]
                    (mlres (Math_Clamp_J_J_J__J 12345 (min x y) (max x y)))))]
    ["clampJJJbad" (fn ^string [^string a ^string b]
                     (let [x (ml a) y (ml b)]
                       (mlres (Math_Clamp_J_J_J__J 0 (max x y) (min x y)))))]
    ["divideExactJ" (fn ^string [^string a ^string b] (mlres (Math_DivideExact_J_J__J (ml a) (ml b))))]
    ["floorDivExactJ" (fn ^string [^string a ^string b] (mlres (Math_FloorDivExact_J_J__J (ml a) (ml b))))]
    ["ceilDivExactJ" (fn ^string [^string a ^string b] (mlres (Math_CeilDivExact_J_J__J (ml a) (ml b))))]
    ["unsignedMultiplyExactJ" (fn ^string [^string a ^string b] (mlres (Math_UnsignedMultiplyExact_J_J__J (ml a) (ml b))))]
    ["unsignedMultiplyExactJI" (fn ^string [^string a ^string b] (mlres (Math_UnsignedMultiplyExact_J_I__J (ml a) (conv int32 (ml b)))))]
    ["powExactJ" (fn ^string [^string a ^string b] (mlres (Math_PowExact_J_I__J (ml a) (pexp (ml b) 70 3))))]
    ["unsignedPowExactJ" (fn ^string [^string a ^string b] (mlres (Math_UnsignedPowExact_J_I__J (ml a) (pexp (ml b) 70 3))))]
    ["powExactJsmall" (fn ^string [^string a ^string b] (mlres (Math_PowExact_J_I__J (conv int64 (- (pexp (ml a) 21 0) 10)) (pexp (ml b) 66 0))))]
    ["ldiv" (fn ^string [^string a ^string b] (mlres (/ (ml a) (ml b))))]
    ["lrem" (fn ^string [^string a ^string b] (mlres (% (ml a) (ml b))))]))

(go/func pexp "pexp is the tests' exponent: Clojure's (mod b m) minus d, as an int.\n" ^int32 [^int64 b ^int64 m ^int64 d]
  (let [r (% b m)]
    (when (< r 0) (set! r (+ r m)))
    (conv int32 (- r d))))

(go/func TestInts [^{:tag (* testing/T)} t]
  (let [n 0]
    (range [_ c (readCases t "ints.txt")]
      (let [op (aget c 0)]
        (if (== (len c) 3)
          (let [g (aget intOps op)
                a (aget c 1)]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (check t c (res (fn ^string [] (g a)))))
          (let [g (aget intOps2 op)
                a (aget c 1)
                b (aget c 2)]
            (when (== g nil) (.Errorf t "unknown op %q" op) (continue))
            (check t c (res (fn ^string [] (g a b))))))
        (inc! n)))
    (failuresReport t)
    (.Logf t "%d cases" n)))

(go/var ^{:tag float64} fmaX (+ 1.0 (/ 1.0 134217728.0)))
(go/var ^{:tag float64} fmaZ (- (+ 1.0 (/ 1.0 67108864.0))))

(go/func TestNoFusion
  "TestNoFusion: x*x + z with x = 1+2^-27, z = -(1+2^-26) is 0 rounded per operation (Java)
and 2^-54 fused. The idiom (conv float64 (* x y)), fmul, gives 0 on both architectures;
Math.fma gives 2^-54; a plain Go x*y + z is logged (gc fuses it on arm64).\n"
  [^{:tag (* testing/T)} t]
  (let [x fmaX
        z fmaZ
        java (+ (conv float64 (* x x)) z)
        viaFmul (+ (fmul x x) z)
        plain (+ (* x x) z)]
    (when (or (!= java 0) (!= viaFmul 0))
      (.Errorf t "unfused x*x+z: %g %g, want 0" java viaFmul))
    (when (!= (Math_Fma_D_D_D__D x x z) (math/Ldexp 1 -54))
      (.Errorf t "Math.fma: %g" (Math_Fma_D_D_D__D x x z)))
    (.Logf t "plain Go x*x+z (fusion allowed): %g" plain)))

(go/func TestMathExceptions [^{:tag (* testing/T)} t]
  (let [cases (lit (slice (slice string))
                (lit (slice string) "addExact" "!java.lang.ArithmeticException: integer overflow")
                (lit (slice string) "floorDivZero" "!java.lang.ArithmeticException: / by zero")
                (lit (slice string) "clampNaN" "!java.lang.IllegalArgumentException: min is NaN"))
        fs (lit (slice (func [] [string]))
             (fn ^string [] (ires (Math_AddExact_I_I__I 2147483647 1)))
             (fn ^string [] (ires (Math_FloorDiv_I_I__I 1 0)))
             (fn ^string [] (mhf (Math_Clamp_F_F_F__F 1 NaN32 2))))]
    (range [i c cases]
      (check t c (res (aget fs i))))))
