;; jrt: java.lang.Math and StrictMath (hand-written: JAVA-SURFACE.md "shim"), with Java's
;; exact semantics. Math.log, pow and cbrt delegate to StrictMath in Java's source, which is
;; java.lang.FdLibm (jdk26u's src/java.base/share/classes/java/lang/FdLibm.java): ported here
;; as written, every product that gc could fuse into a multiply-add written through fmul
;; (C2G-SPEC §7.4, §13.5).
(in-ns 'go.arbace.jrt)

(go/file "math.go"
  :imports [[math "math"] [bits "math/bits"] [rand "math/rand/v2"]])

;; ---------------------------------------------------------------------------------------
;; Helpers

(go/func fmul
  "fmul is a*b rounded to float64 on its own: the explicit conversion forbids gc to fuse it
with an addition (Java has no fused multiply-add but Math.fma).\n"
  ^float64 [^float64 a ^float64 b]
  (conv float64 (* a b)))

(go/func fdHi "fdHi is FdLibm.__HI: the high 32 bits.\n" ^int32 [^float64 x]
  (conv int32 (>> (math/Float64bits x) 32)))

(go/func fdLo "fdLo is FdLibm.__LO: the low 32 bits.\n" ^int32 [^float64 x]
  (conv int32 (conv uint32 (math/Float64bits x))))

(go/func fdSetHi ^float64 [^float64 x ^int32 hi]
  (math/Float64frombits (bit-or (bit-and (math/Float64bits x) 0xffffffff) (<< (conv uint64 (conv uint32 hi)) 32))))

(go/func fdSetLo ^float64 [^float64 x ^int32 lo]
  (math/Float64frombits (bit-or (bit-and (math/Float64bits x) 0xffffffff00000000) (conv uint64 (conv uint32 lo)))))

(go/var
   [fdln2_hi (math/Float64frombits 0x3fe62e42fee00000)]
   [fdln2_lo (math/Float64frombits 0x3dea39ef35793c76)]
   [fdLg1 (math/Float64frombits 0x3fe5555555555593)]
   [fdLg2 (math/Float64frombits 0x3fd999999997fa04)]
   [fdLg3 (math/Float64frombits 0x3fd2492494229359)]
   [fdLg4 (math/Float64frombits 0x3fcc71c51d8e78af)]
   [fdLg5 (math/Float64frombits 0x3fc7466496cb03de)]
   [fdLg6 (math/Float64frombits 0x3fc39a09d078c69f)]
   [fdLg7 (math/Float64frombits 0x3fc2f112df3e5244)]
   [fdcbC (math/Float64frombits 0x3fe15f15f15f15f1)]
   [fdcbD (math/Float64frombits 0xbfe691de2532c834)]
   [fdcbE (math/Float64frombits 0x3ff6a0ea0ea0ea0f)]
   [fdcbF (math/Float64frombits 0x3ff9b6db6db6db6e)]
   [fdcbG (math/Float64frombits 0x3fd6db6db6db6db7)]
   [fdINV_LN2 (math/Float64frombits 0x3ff71547652b82fe)]
   [fdINV_LN2_H (math/Float64frombits 0x3ff7154760000000)]
   [fdINV_LN2_L (math/Float64frombits 0x3e54ae0bf85ddf44)]
   [fdCP (math/Float64frombits 0x3feec709dc3a03fd)]
   [fdCP_H (math/Float64frombits 0x3feec709e0000000)]
   [fdCP_L (math/Float64frombits 0xbe3e2fe0145b01f5)]
   [fdDP_H (math/Float64frombits 0x3fe2b80340000000)]
   [fdDP_L (math/Float64frombits 0x3e4cfdeb43cfd006)]
   [fdL1 (math/Float64frombits 0x3fe3333333333303)]
   [fdL2 (math/Float64frombits 0x3fdb6db6db6fabff)]
   [fdL3 (math/Float64frombits 0x3fd55555518f264d)]
   [fdL4 (math/Float64frombits 0x3fd17460a91d4101)]
   [fdL5 (math/Float64frombits 0x3fcd864a93c9db65)]
   [fdL6 (math/Float64frombits 0x3fca7e284a454eef)]
   [fdP1 (math/Float64frombits 0x3fc555555555553e)]
   [fdP2 (math/Float64frombits 0xbf66c16c16bebd93)]
   [fdP3 (math/Float64frombits 0x3f11566aaf25de2c)]
   [fdP4 (math/Float64frombits 0xbebbbd41c5d26bf1)]
   [fdP5 (math/Float64frombits 0x3e66376972bea4d0)]
   [fdLG2 (math/Float64frombits 0x3fe62e42fefa39ef)]
   [fdLG2_H (math/Float64frombits 0x3fe62e4300000000)]
   [fdLG2_L (math/Float64frombits 0xbe205c610ca86c39)]
   [fdyHuge (math/Float64frombits 0x41e00000ffffffff)]
   [fdxLow (math/Float64frombits 0x3fefffff00000000)]
   [fdxHigh (math/Float64frombits 0x3ff00000ffffffff)]
   [fdTWO54 (math/Float64frombits 0x4350000000000000)])

(go/func overflow ^Throwable_I [^string m] (ArithmeticException_New_String (Str m)))

;; ---------------------------------------------------------------------------------------
;; FdLibm's Log, Cbrt, Pow

(go/func fdLog "fdLog is FdLibm.Log.compute (StrictMath.log).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        lx (fdLo x)
        k (conv int32 0)]
    (when (< hx 0x00100000)
      (when (== (bit-or (bit-and hx 0x7fffffff) lx) 0)
        (return NegInf64))
      (when (< hx 0)
        (return NaN64))
      (set! k (- k 54))
      (set! x (fmul x fdTWO54))
      (set! hx (fdHi x)))
    (when (>= hx 0x7ff00000)
      (return (+ x x)))
    (set! k (+ k (- (>> hx 20) 1023)))
    (set! hx (bit-and hx 0x000fffff))
    (let [i (bit-and (+ hx 0x95f64) 0x100000)]
      (set! x (fdSetHi x (bit-or hx (bit-xor i 0x3ff00000))))
      (set! k (+ k (>> i 20)))
      (let [f (- x 1.0)]
        (when (< (bit-and 0x000fffff (+ 2 hx)) 3)
          (when (== f 0.0)
            (when (== k 0)
              (return 0.0))
            (let [dk (conv float64 k)]
              (return (+ (fmul dk fdln2_hi) (fmul dk fdln2_lo)))))
          (let [R (fmul (fmul f f) (- 0.5 (fmul 0.33333333333333333 f)))]
            (when (== k 0)
              (return (- f R)))
            (let [dk (conv float64 k)]
              (return (- (fmul dk fdln2_hi) (- (- R (fmul dk fdln2_lo)) f))))))
        (let [s (/ f (+ 2.0 f))
              dk (conv float64 k)
              z (fmul s s)
              i2 (- hx 0x6147a)
              w (fmul z z)
              j (- 0x6b851 hx)
              t1 (fmul w (+ fdLg2 (fmul w (+ fdLg4 (fmul w fdLg6)))))
              t2 (fmul z (+ fdLg1 (fmul w (+ fdLg3 (fmul w (+ fdLg5 (fmul w fdLg7)))))))
              R (+ t2 t1)]
          (set! i2 (bit-or i2 j))
          (if (> i2 0)
            (let [hfsq (fmul (fmul 0.5 f) f)]
              (when (== k 0)
                (return (- f (- hfsq (fmul s (+ hfsq R))))))
              (return (- (fmul dk fdln2_hi) (- (- hfsq (+ (fmul s (+ hfsq R)) (fmul dk fdln2_lo))) f))))
            (do
              (when (== k 0)
                (return (- f (fmul s (- f R)))))
              (return (- (fmul dk fdln2_hi) (- (- (fmul s (- f R)) (fmul dk fdln2_lo)) f))))))))))

(go/func fdCbrt "fdCbrt is FdLibm.Cbrt.compute (StrictMath.cbrt).\n" ^float64 [^float64 x]
  (when (or (== x 0.0) (math/IsInf x 0) (math/IsNaN x))
    (return x))
  (let [t 0.0
        sign 1.0]
    (when (< x 0.0)
      (set! sign -1.0))
    (set! x (math/Abs x))
    (if (< x 2.2250738585072014E-308)
      (do
        (set! t 18014398509481984.0)
        (set! t (fmul t x))
        (set! t (fdSetHi t (+ (/ (fdHi t) 3) 696219795))))
      (set! t (fdSetHi t (+ (/ (fdHi x) 3) 715094163))))
    (let [r (/ (fmul t t) x)
          s (+ fdcbC (fmul r t))]
      (set! t (fmul t (+ fdcbG (/ fdcbF (+ s fdcbE (/ fdcbD s))))))
      (set! t (fdSetLo t 0))
      (set! t (fdSetHi t (+ (fdHi t) 1)))
      (set! s (fmul t t))
      (set! r (/ x s))
      (let [w (+ t t)]
        (set! r (/ (- r t) (+ w r)))
        (set! t (+ t (fmul t r)))))
    (fmul sign t)))

(go/func fdPow "fdPow is FdLibm.Pow.compute (StrictMath.pow).\n" ^float64 [^float64 x ^float64 y]
  (when (== y 0.0)
    (return 1.0))
  (when (or (math/IsNaN x) (math/IsNaN y))
    (return (+ x y)))
  (let [yAbs (math/Abs y)
        xAbs (math/Abs x)]
    (cond
      (== y 2.0) (return (fmul x x))
      (== y 0.5) (when (>= x -1.7976931348623157E308)
                   (return (math/Sqrt (+ x 0.0))))
      (== yAbs 1.0) (do (when (== y 1.0) (return x)) (return (/ 1.0 x)))
      (math/IsInf yAbs 1)
      (do
        (cond
          (== xAbs 1.0) (return (- y y))
          (> xAbs 1.0) (do (when (>= y 0) (return y)) (return 0.0))
          :else (do (when (< y 0) (return (- y))) (return 0.0)))))
    (let [hx (fdHi x)
          ix (bit-and hx 0x7fffffff)
          yIsInt (conv int32 0)]
      (when (< hx 0)
        (cond
          (>= yAbs 9007199254740992.0) (set! yIsInt 2)
          (>= yAbs 1.0)
          (let [l (conv int64 yAbs)]
            (when (== (conv float64 l) yAbs)
              (set! yIsInt (- 2 (conv int32 (bit-and l 1))))))))
      (when (or (== xAbs 0.0) (math/IsInf xAbs 0) (== xAbs 1.0))
        (let [z xAbs]
          (when (< y 0.0)
            (set! z (/ 1.0 z)))
          (when (< hx 0)
            (cond
              (== (bit-or (- ix 0x3ff00000) yIsInt) 0) (set! z NaN64)
              (== yIsInt 1) (set! z (fmul -1.0 z))))
          (return z)))
      (let [n (+ (>> hx 31) 1)]
        (when (== (bit-or n yIsInt) 0)
          (return NaN64))
        (let [s 1.0
              ^float64 pH 0
              ^float64 pL 0
              ^float64 t1 0
              ^float64 t2 0
              ^float64 t 0
              ^float64 u 0
              ^float64 v 0
              ^float64 w 0
              ^float64 z 0
              ^float64 r 0
              ^int32 i 0
              ^int32 j 0
              ^int32 k 0]
          (when (== (bit-or n (- yIsInt 1)) 0)
            (set! s -1.0))
          (if (> yAbs fdyHuge)
            (do
              (when (< xAbs fdxLow)
                (when (< y 0.0) (return (fmul s PosInf64)))
                (return (fmul s 0.0)))
              (when (> xAbs fdxHigh)
                (when (> y 0.0) (return (fmul s PosInf64)))
                (return (fmul s 0.0)))
              (set! t (- xAbs 1.0))
              (set! w (fmul (fmul t t) (- 0.5 (fmul t (- 0.3333333333333333333333 (fmul t 0.25))))))
              (set! u (fmul fdINV_LN2_H t))
              (set! v (- (fmul t fdINV_LN2_L) (fmul w fdINV_LN2)))
              (set! t1 (+ u v))
              (set! t1 (fdSetLo t1 0))
              (set! t2 (- v (- t1 u))))
            (do
              (set! n 0)
              (when (< ix 0x00100000)
                (set! xAbs (fmul xAbs 9007199254740992.0))
                (set! n (- n 53))
                (set! ix (fdHi xAbs)))
              (set! n (+ n (- (>> ix 20) 0x3ff)))
              (set! j (bit-and ix 0x000fffff))
              (set! ix (bit-or j 0x3ff00000))
              (cond
                (<= j 0x3988E) (set! k 0)
                (< j 0xBB67A) (set! k 1)
                :else (do (set! k 0) (inc! n) (set! ix (- ix 0x00100000))))
              (set! xAbs (fdSetHi xAbs ix))
              (let [bpK (+ 1.0 (fmul 0.5 (conv float64 k)))]
                (set! u (- xAbs bpK))
                (set! v (/ 1.0 (+ xAbs bpK)))
                (let [ss (fmul u v)
                      sH (fdSetLo ss 0)
                      tH (fdSetHi 0.0 (+ (bit-or (>> ix 1) 0x20000000) 0x00080000 (<< k 18)))
                      tL (- xAbs (- tH bpK))
                      sL (fmul v (- (- u (fmul sH tH)) (fmul sH tL)))
                      s2 (fmul ss ss)]
                  (set! r (fmul (fmul s2 s2) (+ fdL1 (fmul s2 (+ fdL2 (fmul s2 (+ fdL3 (fmul s2 (+ fdL4 (fmul s2 (+ fdL5 (fmul s2 fdL6))))))))))))
                  (set! r (+ r (fmul sL (+ sH ss))))
                  (set! s2 (fmul sH sH))
                  (set! tH (+ 3.0 s2 r))
                  (set! tH (fdSetLo tH 0))
                  (set! tL (- r (- (- tH 3.0) s2)))
                  (set! u (fmul sH tH))
                  (set! v (+ (fmul sL tH) (fmul tL ss)))
                  (set! pH (+ u v))
                  (set! pH (fdSetLo pH 0))
                  (set! pL (- v (- pH u)))
                  (let [zH (fmul fdCP_H pH)
                        dk (conv float64 k)
                        zL (+ (fmul fdCP_L pH) (fmul pL fdCP) (fmul fdDP_L dk))]
                    (set! t (conv float64 n))
                    (set! t1 (+ (+ (+ zH zL) (fmul fdDP_H dk)) t))
                    (set! t1 (fdSetLo t1 0))
                    (set! t2 (- zL (- (- (- t1 t) (fmul fdDP_H dk)) zH))))))))
          ;; (y1 + y2) * (t1 + t2)
          (let [y1 (fdSetLo y 0)]
            (set! pL (+ (fmul (- y y1) t1) (fmul y t2)))
            (set! pH (fmul y1 t1))
            (set! z (+ pL pH))
            (set! j (fdHi z))
            (set! i (fdLo z))
            (cond
              (>= j 0x40900000)
              (do
                (when (!= (bit-or (- j 0x40900000) i) 0)
                  (return (fmul s PosInf64)))
                (when (> (+ pL 8.0085662595372944372E-17) (- z pH))
                  (return (fmul s PosInf64))))
              (>= (bit-and j 0x7fffffff) 0x4090cc00)
              (do
                (when (!= (bit-or (- j (conv int32 -1064252416)) i) 0)
                  (return (fmul s 0.0)))
                (when (<= pL (- z pH))
                  (return (fmul s 0.0)))))
            (set! i (bit-and j 0x7fffffff))
            (set! k (- (>> i 20) 0x3ff))
            (set! n 0)
            (when (> i 0x3fe00000)
              (set! n (+ j (>> 0x00100000 (+ k 1))))
              (set! k (- (>> (bit-and n 0x7fffffff) 20) 0x3ff))
              (set! t (fdSetHi 0.0 (bit-and n (bit-not (>> 0x000fffff k)))))
              (set! n (>> (bit-or (bit-and n 0x000fffff) 0x00100000) (- 20 k)))
              (when (< j 0)
                (set! n (- n)))
              (set! pH (- pH t)))
            (set! t (+ pL pH))
            (set! t (fdSetLo t 0))
            (set! u (fmul t fdLG2_H))
            (set! v (+ (fmul (- pL (- t pH)) fdLG2) (fmul t fdLG2_L)))
            (set! z (+ u v))
            (set! w (- v (- z u)))
            (set! t (fmul z z))
            (set! t1 (- z (fmul t (+ fdP1 (fmul t (+ fdP2 (fmul t (+ fdP3 (fmul t (+ fdP4 (fmul t fdP5)))))))))))
            (set! r (- (/ (fmul z t1) (- t1 2.0)) (+ w (fmul z w))))
            (set! z (- 1.0 (- r z)))
            (set! j (fdHi z))
            (set! j (+ j (<< n 20)))
            (if (<= (>> j 20) 0)
              (set! z (Math_Scalb_D_I__D z n))
              (set! z (fdSetHi z (+ (fdHi z) (<< n 20)))))
            (fmul s z)))))))

;; ---------------------------------------------------------------------------------------
;; Math: floating point

(go/func Math_Abs_D__D ^float64 [^float64 a] (math/Float64frombits (bit-and (math/Float64bits a) 0x7fffffffffffffff)))
(go/func Math_Abs_F__F ^float32 [^float32 a] (math/Float32frombits (bit-and (math/Float32bits a) 0x7fffffff)))
(go/func Math_Sqrt_D__D "Math_Sqrt_D__D: IEEE's correctly rounded square root, as Java's.\n" ^float64 [^float64 a] (math/Sqrt a))
(go/func Math_Cbrt_D__D ^float64 [^float64 a] (fdCbrt a))
(go/func Math_Log_D__D "Math_Log_D__D is StrictMath.log (FdLibm), what Java's Math.log delegates to.\n" ^float64 [^float64 a] (fdLog a))
(go/func Math_Pow_D_D__D "Math_Pow_D_D__D is StrictMath.pow (FdLibm), what Java's Math.pow delegates to.\n" ^float64 [^float64 a ^float64 b] (fdPow a b))
(go/func StrictMath_Log_D__D ^float64 [^float64 a] (fdLog a))
(go/func StrictMath_Sqrt_D__D ^float64 [^float64 a] (math/Sqrt a))
(go/func StrictMath_Cbrt_D__D ^float64 [^float64 a] (fdCbrt a))
(go/func StrictMath_Pow_D_D__D ^float64 [^float64 a ^float64 b] (fdPow a b))
(go/func Math_Ceil_D__D ^float64 [^float64 a] (math/Ceil a))
(go/func Math_Floor_D__D ^float64 [^float64 a] (math/Floor a))
(go/func Math_Rint_D__D "Math_Rint_D__D: the nearest integer, ties to even (StrictMath.rint).\n" ^float64 [^float64 a] (math/RoundToEven a))
(go/func StrictMath_Rint_D__D ^float64 [^float64 a] (math/RoundToEven a))
(go/func Math_Fma_D_D_D__D "Math_Fma_D_D_D__D is Math.fma: the one fused multiply-add Java has.\n"
  ^float64 [^float64 a ^float64 b ^float64 c] (math/FMA a b c))
(go/func Math_Fma_F_F_F__F
  "Math_Fma_F_F_F__F is Math.fma(float...): the exact a*b+c rounded once to float. The product
of floats is exact in double; the sum is rounded to double to odd (TwoSum's error decides
the last bit), which then rounds to float exactly as one rounding would.\n"
  ^float32 [^float32 a ^float32 b ^float32 c]
  (let [p (fmul (conv float64 a) (conv float64 b))
        d (conv float64 c)]
    (when (or (math/IsInf p 0) (math/IsNaN p) (math/IsInf d 0) (math/IsNaN d))
      (return (conv float32 (math/FMA (conv float64 a) (conv float64 b) d))))
    (when (or (== a 0) (== b 0))
      (return (+ (conv float32 (* a b)) c)))
    (let [s (+ p d)
          bb (- s p)
          err (+ (- p (- s bb)) (- d bb))]
      (when (and (!= err 0) (== (bit-and (math/Float64bits s) 1) 0))
        (if (> err 0)
          (set! s (math/Nextafter s PosInf64))
          (set! s (math/Nextafter s NegInf64))))
      (conv float32 s))))

(go/func Math_Max_D_D__D ^float64 [^float64 a ^float64 b]
  (when (!= a a) (return a))
  (when (and (== a 0.0) (== b 0.0) (== (math/Float64bits a) 0x8000000000000000)) (return b))
  (when (>= a b) (return a))
  b)
(go/func Math_Min_D_D__D ^float64 [^float64 a ^float64 b]
  (when (!= a a) (return a))
  (when (and (== a 0.0) (== b 0.0) (== (math/Float64bits b) 0x8000000000000000)) (return b))
  (when (<= a b) (return a))
  b)
(go/func Math_Max_F_F__F ^float32 [^float32 a ^float32 b]
  (when (!= a a) (return a))
  (when (and (== a 0.0) (== b 0.0) (== (math/Float32bits a) 0x80000000)) (return b))
  (when (>= a b) (return a))
  b)
(go/func Math_Min_F_F__F ^float32 [^float32 a ^float32 b]
  (when (!= a a) (return a))
  (when (and (== a 0.0) (== b 0.0) (== (math/Float32bits b) 0x80000000)) (return b))
  (when (<= a b) (return a))
  b)

(go/func Math_Round_D__J "Math_Round_D__J is JDK 26's Math.round(double): floor(a + 1/2) by bits.\n"
  ^int64 [^float64 a]
  (let [lb (conv int64 (math/Float64bits a))
        biased (>> (bit-and lb 0x7ff0000000000000) 52)
        shift (- (+ 51 1023) biased)]
    (when (== (bit-and shift -64) 0)
      (let [r (bit-or (bit-and lb 0x000fffffffffffff) 0x0010000000000000)]
        (when (< lb 0) (set! r (- r)))
        (return (>> (+ (>> r shift) 1) 1))))
    (D2L a)))

(go/func Math_Round_F__I "Math_Round_F__I is JDK 26's Math.round(float).\n"
  ^int32 [^float32 a]
  (let [ib (conv int32 (math/Float32bits a))
        biased (>> (bit-and ib 0x7f800000) 23)
        shift (- (+ 22 127) biased)]
    (when (== (bit-and shift -32) 0)
      (let [r (bit-or (bit-and ib 0x007fffff) 0x00800000)]
        (when (< ib 0) (set! r (- r)))
        (return (>> (+ (>> r shift) 1) 1))))
    (F2I a)))

(go/func Math_GetExponent_D__I ^int32 [^float64 d]
  (- (conv int32 (>> (bit-and (math/Float64bits d) 0x7ff0000000000000) 52)) 1023))
(go/func Math_GetExponent_F__I ^int32 [^float32 f]
  (- (conv int32 (>> (bit-and (math/Float32bits f) 0x7f800000) 23)) 127))

(go/func powerOfTwoD ^float64 [^int32 n]
  (math/Float64frombits (<< (conv uint64 (+ n 1023)) 52)))

(go/func Math_Scalb_D_I__D "Math_Scalb_D_I__D is Math.scalb(double, int) as Java computes it.\n"
  ^float64 [^float64 d ^int32 sf]
  (let [up (powerOfTwoD 1023)
        down (math/Float64frombits 0x0008000000000000)]
    (when (> sf -1023)
      (when (<= sf 1023)
        (return (fmul d (powerOfTwoD sf))))
      (when (<= sf 2046)
        (return (fmul (fmul d (powerOfTwoD (- sf 1023))) up)))
      (when (< sf (+ 2046 52))
        (return (fmul (fmul (fmul d (powerOfTwoD (- sf 2046))) up) up)))
      (return (fmul (fmul (fmul d up) up) up)))
    (when (> sf -2046)
      (return (fmul (fmul d (powerOfTwoD (+ sf 1023))) down)))
    (when (> sf (- -2046 53))
      (return (fmul (fmul (fmul d (powerOfTwoD (+ sf 2046))) down) down)))
    (fmul (fmul d 4.9E-324) 4.9E-324)))

(go/func Math_Scalb_F_I__F ^float32 [^float32 f ^int32 sf]
  (let [maxScale (conv int32 (+ 127 126 24 1))]
    (when (> sf maxScale) (set! sf maxScale))
    (when (< sf (- maxScale)) (set! sf (- maxScale)))
    (conv float32 (fmul (conv float64 f) (powerOfTwoD sf)))))

(go/func Math_NextUp_D__D ^float64 [^float64 d]
  (when (not (< d PosInf64))
    (return d))
  (let [tr (conv int64 (math/Float64bits (+ d 0.0)))]
    (when (>= tr 0) (return (math/Float64frombits (conv uint64 (+ tr 1)))))
    (math/Float64frombits (conv uint64 (- tr 1)))))
(go/func Math_NextUp_F__F ^float32 [^float32 f]
  (when (not (< f PosInf32))
    (return f))
  (let [tr (conv int32 (math/Float32bits (+ f 0.0)))]
    (when (>= tr 0) (return (math/Float32frombits (conv uint32 (+ tr 1)))))
    (math/Float32frombits (conv uint32 (- tr 1)))))
(go/func Math_NextDown_D__D ^float64 [^float64 d]
  (when (or (math/IsNaN d) (== d NegInf64))
    (return d))
  (when (== d 0.0)
    (return -4.9E-324))
  (let [b (conv int64 (math/Float64bits d))]
    (when (> d 0.0) (return (math/Float64frombits (conv uint64 (- b 1)))))
    (math/Float64frombits (conv uint64 (+ b 1)))))
(go/func Math_NextAfter_D_D__D ^float64 [^float64 start ^float64 dir]
  (cond
    (> start dir)
    (do
      (when (== start 0.0) (return -4.9E-324))
      (let [tr (conv int64 (math/Float64bits start))]
        (when (> tr 0) (return (math/Float64frombits (conv uint64 (- tr 1)))))
        (return (math/Float64frombits (conv uint64 (+ tr 1))))))
    (< start dir)
    (let [tr (conv int64 (math/Float64bits (+ start 0.0)))]
      (when (>= tr 0) (return (math/Float64frombits (conv uint64 (+ tr 1)))))
      (return (math/Float64frombits (conv uint64 (- tr 1)))))
    (== start dir) (return dir))
  (+ start dir))

(go/func Math_Ulp_D__D ^float64 [^float64 d]
  (let [e (Math_GetExponent_D__I d)]
    (cond
      (== e 1024) (return (Math_Abs_D__D d))
      (== e -1023) (return 4.9E-324))
    (set! e (- e 52))
    (when (>= e -1022)
      (return (powerOfTwoD e)))
    (math/Float64frombits (<< (conv uint64 1) (- e (- -1022 52))))))
(go/func Math_Ulp_F__F ^float32 [^float32 f]
  (let [e (Math_GetExponent_F__I f)]
    (cond
      (== e 128) (return (Math_Abs_F__F f))
      (== e -127) (return (math/Float32frombits 1)))
    (set! e (- e 23))
    (when (>= e -126)
      (return (math/Float32frombits (<< (conv uint32 (+ e 127)) 23))))
    (math/Float32frombits (<< (conv uint32 1) (- e (- -126 23))))))

(go/func Math_CopySign_D_D__D ^float64 [^float64 m ^float64 s]
  (math/Float64frombits (bit-or (bit-and (math/Float64bits s) 0x8000000000000000)
                                (bit-and (math/Float64bits m) 0x7fffffffffffffff))))
(go/func Math_CopySign_F_F__F ^float32 [^float32 m ^float32 s]
  (math/Float32frombits (bit-or (bit-and (math/Float32bits s) 0x80000000)
                                (bit-and (math/Float32bits m) 0x7fffffff))))
(go/func Math_Signum_D__D ^float64 [^float64 d]
  (when (or (== d 0.0) (math/IsNaN d)) (return d))
  (Math_CopySign_D_D__D 1.0 d))
(go/func Math_Signum_F__F ^float32 [^float32 f]
  (when (or (== f 0.0) (!= f f)) (return f))
  (Math_CopySign_F_F__F 1.0 f))

(go/func Math_Clamp_F_F_F__F ^float32 [^float32 v ^float32 lo ^float32 hi]
  (when (not (< lo hi))
    (when (!= lo lo) (panic (IllegalArgumentException_New_String (Str "min is NaN"))))
    (when (!= hi hi) (panic (IllegalArgumentException_New_String (Str "max is NaN"))))
    (when (or (> lo hi) (and (== lo hi) (== (math/Float32bits lo) 0) (== (math/Float32bits hi) 0x80000000)))
      (panic (IllegalArgumentException_New_String (Concat (StrOfFloat lo) (Str " > ") (StrOfFloat hi))))))
  (Math_Min_F_F__F hi (Math_Max_F_F__F v lo)))
(go/func Math_Clamp_J_I_I__I ^int32 [^int64 v ^int32 lo ^int32 hi]
  (when (> lo hi)
    (panic (IllegalArgumentException_New_String (Concat (StrOfInt lo) (Str " > ") (StrOfInt hi)))))
  (conv int32 (min (conv int64 hi) (max v (conv int64 lo)))))
(go/func Math_Clamp_J_J_J__J ^int64 [^int64 v ^int64 lo ^int64 hi]
  (when (> lo hi)
    (panic (IllegalArgumentException_New_String (Concat (StrOfLong lo) (Str " > ") (StrOfLong hi)))))
  (min hi (max v lo)))

(go/func Math_Random__D "Math_Random__D is Math.random: uniform in [0, 1) (Go's generator, not java.util.Random's sequence).\n"
  ^float64 []
  (rand/Float64))

;; ---------------------------------------------------------------------------------------
;; Math: integers

(go/func Math_Abs_I__I ^int32 [^int32 a] (when (< a 0) (return (- a))) a)
(go/func Math_Abs_J__J ^int64 [^int64 a] (when (< a 0) (return (- a))) a)
(go/func Math_Max_I_I__I ^int32 [^int32 a ^int32 b] (max a b))
(go/func Math_Min_I_I__I ^int32 [^int32 a ^int32 b] (min a b))
(go/func Math_Max_J_J__J ^int64 [^int64 a ^int64 b] (max a b))
(go/func Math_Min_J_J__J ^int64 [^int64 a ^int64 b] (min a b))

(go/func Math_AddExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (+ x y)]
    (when (< (bit-and (bit-xor x r) (bit-xor y r)) 0) (panic (overflow "integer overflow")))
    r))
(go/func Math_AddExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [r (+ x y)]
    (when (< (bit-and (bit-xor x r) (bit-xor y r)) 0) (panic (overflow "long overflow")))
    r))
(go/func Math_SubtractExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (- x y)]
    (when (< (bit-and (bit-xor x y) (bit-xor x r)) 0) (panic (overflow "integer overflow")))
    r))
(go/func Math_SubtractExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [r (- x y)]
    (when (< (bit-and (bit-xor x y) (bit-xor x r)) 0) (panic (overflow "long overflow")))
    r))
(go/func Math_MultiplyExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (* (conv int64 x) (conv int64 y))]
    (when (!= (conv int64 (conv int32 r)) r) (panic (overflow "integer overflow")))
    (conv int32 r)))
(go/func Math_MultiplyExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [r (* x y)
        ax (Math_Abs_J__J x)
        ay (Math_Abs_J__J y)]
    (when (!= (>> (conv uint64 (bit-or ax ay)) 31) 0)
      (when (or (and (!= y 0) (!= (/ r y) x)) (and (== x -9223372036854775808) (== y -1)))
        (panic (overflow "long overflow"))))
    r))
(go/func Math_MultiplyExact_J_I__J ^int64 [^int64 x ^int32 y] (Math_MultiplyExact_J_J__J x (conv int64 y)))
(go/func Math_IncrementExact_I__I ^int32 [^int32 a]
  (when (== a 2147483647) (panic (overflow "integer overflow")))
  (+ a 1))
(go/func Math_IncrementExact_J__J ^int64 [^int64 a]
  (when (== a 9223372036854775807) (panic (overflow "long overflow")))
  (+ a 1))
(go/func Math_DecrementExact_I__I ^int32 [^int32 a]
  (when (== a -2147483648) (panic (overflow "integer overflow")))
  (- a 1))
(go/func Math_DecrementExact_J__J ^int64 [^int64 a]
  (when (== a -9223372036854775808) (panic (overflow "long overflow")))
  (- a 1))
(go/func Math_NegateExact_I__I ^int32 [^int32 a]
  (when (== a -2147483648) (panic (overflow "integer overflow")))
  (- a))
(go/func Math_NegateExact_J__J ^int64 [^int64 a]
  (when (== a -9223372036854775808) (panic (overflow "long overflow")))
  (- a))
(go/func Math_ToIntExact_J__I ^int32 [^int64 v]
  (when (!= (conv int64 (conv int32 v)) v) (panic (overflow "integer overflow")))
  (conv int32 v))
(go/func Math_AbsExact_I__I ^int32 [^int32 a]
  (when (== a -2147483648) (panic (overflow "Overflow to represent absolute value of Integer.MIN_VALUE")))
  (Math_Abs_I__I a))
(go/func Math_AbsExact_J__J ^int64 [^int64 a]
  (when (== a -9223372036854775808) (panic (overflow "Overflow to represent absolute value of Long.MIN_VALUE")))
  (Math_Abs_J__J a))

(go/func Math_MultiplyHigh_J_J__J ^int64 [^int64 x ^int64 y]
  (let [(values hi _) (bits/Mul64 (conv uint64 x) (conv uint64 y))
        r (conv int64 hi)]
    ;; the signed high word from the unsigned one
    (when (< x 0) (set! r (- r y)))
    (when (< y 0) (set! r (- r x)))
    r))
(go/func Math_UnsignedMultiplyHigh_J_J__J ^int64 [^int64 x ^int64 y]
  (let [(values hi _) (bits/Mul64 (conv uint64 x) (conv uint64 y))]
    (conv int64 hi)))

;; floorDiv, floorMod, ceilDiv, ceilMod; division by zero is Go's panic, which jrt.Catch
;; turns into ArithmeticException("/ by zero"); MIN_VALUE / -1 is MIN_VALUE in both

(go/func Math_FloorDiv_I_I__I ^int32 [^int32 x ^int32 y]
  (let [q (/ x y)]
    (when (and (< (bit-xor x y) 0) (!= (* q y) x)) (return (- q 1)))
    q))
(go/func Math_FloorDiv_J_J__J ^int64 [^int64 x ^int64 y]
  (let [q (/ x y)]
    (when (and (< (bit-xor x y) 0) (!= (* q y) x)) (return (- q 1)))
    q))
(go/func Math_FloorDiv_J_I__J ^int64 [^int64 x ^int32 y] (Math_FloorDiv_J_J__J x (conv int64 y)))
(go/func Math_FloorMod_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (% x y)]
    (when (and (< (bit-xor x y) 0) (!= r 0)) (return (+ r y)))
    r))
(go/func Math_FloorMod_J_J__J ^int64 [^int64 x ^int64 y]
  (let [r (% x y)]
    (when (and (< (bit-xor x y) 0) (!= r 0)) (return (+ r y)))
    r))
(go/func Math_FloorMod_J_I__I ^int32 [^int64 x ^int32 y] (conv int32 (Math_FloorMod_J_J__J x (conv int64 y))))
(go/func Math_CeilDiv_I_I__I ^int32 [^int32 x ^int32 y]
  (let [q (/ x y)]
    (when (and (>= (bit-xor x y) 0) (!= (* q y) x)) (return (+ q 1)))
    q))
(go/func Math_CeilDiv_J_J__J ^int64 [^int64 x ^int64 y]
  (let [q (/ x y)]
    (when (and (>= (bit-xor x y) 0) (!= (* q y) x)) (return (+ q 1)))
    q))
(go/func Math_CeilDiv_J_I__J ^int64 [^int64 x ^int32 y] (Math_CeilDiv_J_J__J x (conv int64 y)))
(go/func Math_CeilMod_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (% x y)]
    (when (and (>= (bit-xor x y) 0) (!= r 0)) (return (- r y)))
    r))
(go/func Math_CeilMod_J_J__J ^int64 [^int64 x ^int64 y]
  (let [r (% x y)]
    (when (and (>= (bit-xor x y) 0) (!= r 0)) (return (- r y)))
    r))
(go/func Math_CeilMod_J_I__I ^int32 [^int64 x ^int32 y] (conv int32 (Math_CeilMod_J_J__J x (conv int64 y))))

;; ---------------------------------------------------------------------------------------
;; Math: the functions of FdLibm (fdlibm.clj). Java's Math delegates them to StrictMath; HotSpot
;; may replace some by intrinsics that differ in the last bit (JRT-NOTES.md, phase 2B): jrt's
;; Math is StrictMath, the same on every architecture.

(go/func Math_Sin_D__D ^float64 [^float64 a] (fdSin a))
(go/func Math_Cos_D__D ^float64 [^float64 a] (fdCos a))
(go/func Math_Tan_D__D ^float64 [^float64 a] (fdTan a))
(go/func Math_Asin_D__D ^float64 [^float64 a] (fdAsin a))
(go/func Math_Acos_D__D ^float64 [^float64 a] (fdAcos a))
(go/func Math_Atan_D__D ^float64 [^float64 a] (fdAtan a))
(go/func Math_Atan2_D_D__D ^float64 [^float64 y ^float64 x] (fdAtan2 y x))
(go/func Math_Exp_D__D ^float64 [^float64 a] (fdExp a))
(go/func Math_Log10_D__D ^float64 [^float64 a] (fdLog10 a))
(go/func Math_Log1p_D__D ^float64 [^float64 a] (fdLog1p a))
(go/func Math_Expm1_D__D ^float64 [^float64 a] (fdExpm1 a))
(go/func Math_Sinh_D__D ^float64 [^float64 a] (fdSinh a))
(go/func Math_Cosh_D__D ^float64 [^float64 a] (fdCosh a))
(go/func Math_Tanh_D__D ^float64 [^float64 a] (fdTanh a))
(go/func Math_Hypot_D_D__D ^float64 [^float64 x ^float64 y] (fdHypot x y))
(go/func Math_IEEEremainder_D_D__D ^float64 [^float64 x ^float64 y] (fdIEEEremainder x y))
(go/func Math_ToRadians_D__D "Math_ToRadians_D__D is Math.toRadians: one product with DEGREES_TO_RADIANS.\n"
  ^float64 [^float64 a] (fmul a 0.017453292519943295))
(go/func Math_ToDegrees_D__D "Math_ToDegrees_D__D is Math.toDegrees: one product with RADIANS_TO_DEGREES.\n"
  ^float64 [^float64 a] (fmul a 57.29577951308232))

(go/func Math_Clamp_D_D_D__D ^float64 [^float64 v ^float64 lo ^float64 hi]
  (when (not (< lo hi))
    (when (!= lo lo) (panic (IllegalArgumentException_New_String (Str "min is NaN"))))
    (when (!= hi hi) (panic (IllegalArgumentException_New_String (Str "max is NaN"))))
    (when (or (> lo hi) (and (== lo hi) (== (math/Float64bits lo) 0) (== (math/Float64bits hi) 0x8000000000000000)))
      (panic (IllegalArgumentException_New_String (Concat (StrOfDouble lo) (Str " > ") (StrOfDouble hi))))))
  (Math_Min_D_D__D hi (Math_Max_D_D__D v lo)))

(go/func Math_NextAfter_F_D__F ^float32 [^float32 start ^float64 dir]
  (cond
    (> (conv float64 start) dir)
    (do
      (when (== start 0.0) (return (- (math/Float32frombits 1))))
      (let [tr (conv int32 (math/Float32bits start))]
        (when (> tr 0) (return (math/Float32frombits (conv uint32 (- tr 1)))))
        (return (math/Float32frombits (conv uint32 (+ tr 1))))))
    (< (conv float64 start) dir)
    (let [tr (conv int32 (math/Float32bits (+ start 0.0)))]
      (when (>= tr 0) (return (math/Float32frombits (conv uint32 (+ tr 1)))))
      (return (math/Float32frombits (conv uint32 (- tr 1)))))
    (== (conv float64 start) dir) (return (conv float32 dir)))
  (+ start (conv float32 dir)))
(go/func Math_NextDown_F__F ^float32 [^float32 f]
  (when (or (!= f f) (== f NegInf32))
    (return f))
  (when (== f 0.0)
    (return (- (math/Float32frombits 1))))
  (let [b (conv int32 (math/Float32bits f))]
    (when (> f 0.0) (return (math/Float32frombits (conv uint32 (- b 1)))))
    (math/Float32frombits (conv uint32 (+ b 1)))))

(go/func Math_DivideExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0) (return q))
    (panic (overflow "integer overflow"))))
(go/func Math_DivideExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0) (return q))
    (panic (overflow "long overflow"))))
(go/func Math_FloorDivExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0)
      (when (and (< (bit-xor x y) 0) (!= (* q y) x)) (return (- q 1)))
      (return q))
    (panic (overflow "integer overflow"))))
(go/func Math_FloorDivExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0)
      (when (and (< (bit-xor x y) 0) (!= (* q y) x)) (return (- q 1)))
      (return q))
    (panic (overflow "long overflow"))))
(go/func Math_CeilDivExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0)
      (when (and (>= (bit-xor x y) 0) (!= (* q y) x)) (return (+ q 1)))
      (return q))
    (panic (overflow "integer overflow"))))
(go/func Math_CeilDivExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [q (/ x y)]
    (when (>= (bit-and x y q) 0)
      (when (and (>= (bit-xor x y) 0) (!= (* q y) x)) (return (+ q 1)))
      (return q))
    (panic (overflow "long overflow"))))
(go/func Math_MultiplyFull_I_I__J ^int64 [^int32 x ^int32 y] (* (conv int64 x) (conv int64 y)))

(go/func Math_UnsignedMultiplyExact_I_I__I ^int32 [^int32 x ^int32 y]
  (let [r (* (conv uint64 (conv uint32 x)) (conv uint64 (conv uint32 y)))]
    (when (!= (>> r 32) 0) (panic (overflow "unsigned integer overflow")))
    (conv int32 r)))
(go/func Math_UnsignedMultiplyExact_J_J__J ^int64 [^int64 x ^int64 y]
  (let [(values hi lo) (bits/Mul64 (conv uint64 x) (conv uint64 y))]
    (when (== hi 0) (return (conv int64 lo)))
    (panic (overflow "unsigned long overflow"))))
(go/func Math_UnsignedMultiplyExact_J_I__J ^int64 [^int64 x ^int32 y]
  (Math_UnsignedMultiplyExact_J_J__J x (conv int64 (conv uint32 y))))

(go/func Math_PowExact_I_I__I ^int32 [^int32 x ^int32 n]
  (when (< n 0) (panic (overflow "negative exponent")))
  (when (== n 0) (return 1))
  (when (or (== x 0) (== x 1)) (return x))
  (when (== x -1)
    (when (== (bit-and n 1) 0) (return 1))
    (return -1))
  (let [p (conv int32 1)]
    (while (> n 1)
      (when (!= (bit-and n 1) 0) (set! p (* p x)))
      (set! x (Math_MultiplyExact_I_I__I x x))
      (set! n (fdUshr n 1)))
    (Math_MultiplyExact_I_I__I p x)))
(go/func Math_PowExact_J_I__J ^int64 [^int64 x ^int32 n]
  (when (< n 0) (panic (overflow "negative exponent")))
  (when (== n 0) (return 1))
  (when (or (== x 0) (== x 1)) (return x))
  (when (== x -1)
    (when (!= (bit-and n 1) 0) (return -1))
    (return 1))
  (let [p (conv int64 1)]
    (while (> n 1)
      (when (!= (bit-and n 1) 0) (set! p (* p x)))
      (set! x (Math_MultiplyExact_J_J__J x x))
      (set! n (fdUshr n 1)))
    (Math_MultiplyExact_J_J__J p x)))
(go/func Math_UnsignedPowExact_I_I__I ^int32 [^int32 x ^int32 n]
  (when (< n 0) (panic (overflow "negative exponent")))
  (when (== n 0) (return 1))
  (when (or (== x 0) (== x 1)) (return x))
  (let [p (conv int32 1)]
    (while (> n 1)
      (when (!= (bit-and n 1) 0) (set! p (* p x)))
      (set! x (Math_UnsignedMultiplyExact_I_I__I x x))
      (set! n (fdUshr n 1)))
    (Math_UnsignedMultiplyExact_I_I__I p x)))
(go/func Math_UnsignedPowExact_J_I__J ^int64 [^int64 x ^int32 n]
  (when (< n 0) (panic (overflow "negative exponent")))
  (when (== n 0) (return 1))
  (when (or (== x 0) (== x 1)) (return x))
  (let [p (conv int64 1)]
    (while (> n 1)
      (when (!= (bit-and n 1) 0) (set! p (* p x)))
      (set! x (Math_UnsignedMultiplyExact_J_J__J x x))
      (set! n (fdUshr n 1)))
    (Math_UnsignedMultiplyExact_J_J__J p x)))

;; ---------------------------------------------------------------------------------------
;; StrictMath: FdLibm's functions, the rest delegating to Math as StrictMath.java does
;; (ceil, floor and rint are StrictMath's own Java code there, with the same results as
;; Math's: they are exact)

(go/func StrictMath_Sin_D__D ^float64 [^float64 a] (fdSin a))
(go/func StrictMath_Cos_D__D ^float64 [^float64 a] (fdCos a))
(go/func StrictMath_Tan_D__D ^float64 [^float64 a] (fdTan a))
(go/func StrictMath_Asin_D__D ^float64 [^float64 a] (fdAsin a))
(go/func StrictMath_Acos_D__D ^float64 [^float64 a] (fdAcos a))
(go/func StrictMath_Atan_D__D ^float64 [^float64 a] (fdAtan a))
(go/func StrictMath_Atan2_D_D__D ^float64 [^float64 y ^float64 x] (fdAtan2 y x))
(go/func StrictMath_Exp_D__D ^float64 [^float64 a] (fdExp a))
(go/func StrictMath_Log10_D__D ^float64 [^float64 a] (fdLog10 a))
(go/func StrictMath_Log1p_D__D ^float64 [^float64 a] (fdLog1p a))
(go/func StrictMath_Expm1_D__D ^float64 [^float64 a] (fdExpm1 a))
(go/func StrictMath_Sinh_D__D ^float64 [^float64 a] (fdSinh a))
(go/func StrictMath_Cosh_D__D ^float64 [^float64 a] (fdCosh a))
(go/func StrictMath_Tanh_D__D ^float64 [^float64 a] (fdTanh a))
(go/func StrictMath_Hypot_D_D__D ^float64 [^float64 x ^float64 y] (fdHypot x y))
(go/func StrictMath_IEEEremainder_D_D__D ^float64 [^float64 x ^float64 y] (fdIEEEremainder x y))
(go/func StrictMath_ToRadians_D__D ^float64 [^float64 a] (Math_ToRadians_D__D a))
(go/func StrictMath_ToDegrees_D__D ^float64 [^float64 a] (Math_ToDegrees_D__D a))
(go/func StrictMath_Ceil_D__D ^float64 [^float64 a] (math/Ceil a))
(go/func StrictMath_Floor_D__D ^float64 [^float64 a] (math/Floor a))
(go/func StrictMath_Round_F__I ^int32 [^float32 a] (Math_Round_F__I a))
(go/func StrictMath_Round_D__J ^int64 [^float64 a] (Math_Round_D__J a))
(go/func StrictMath_Random__D "StrictMath_Random__D is StrictMath.random: Go's generator, as Math.random.\n" ^float64 [] (rand/Float64))
(go/func StrictMath_AddExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_AddExact_I_I__I x y))
(go/func StrictMath_AddExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_AddExact_J_J__J x y))
(go/func StrictMath_SubtractExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_SubtractExact_I_I__I x y))
(go/func StrictMath_SubtractExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_SubtractExact_J_J__J x y))
(go/func StrictMath_MultiplyExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_MultiplyExact_I_I__I x y))
(go/func StrictMath_MultiplyExact_J_I__J ^int64 [^int64 x ^int32 y] (Math_MultiplyExact_J_I__J x y))
(go/func StrictMath_MultiplyExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_MultiplyExact_J_J__J x y))
(go/func StrictMath_DivideExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_DivideExact_I_I__I x y))
(go/func StrictMath_DivideExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_DivideExact_J_J__J x y))
(go/func StrictMath_FloorDivExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_FloorDivExact_I_I__I x y))
(go/func StrictMath_FloorDivExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_FloorDivExact_J_J__J x y))
(go/func StrictMath_CeilDivExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_CeilDivExact_I_I__I x y))
(go/func StrictMath_CeilDivExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_CeilDivExact_J_J__J x y))
(go/func StrictMath_IncrementExact_I__I ^int32 [^int32 a] (Math_IncrementExact_I__I a))
(go/func StrictMath_IncrementExact_J__J ^int64 [^int64 a] (Math_IncrementExact_J__J a))
(go/func StrictMath_DecrementExact_I__I ^int32 [^int32 a] (Math_DecrementExact_I__I a))
(go/func StrictMath_DecrementExact_J__J ^int64 [^int64 a] (Math_DecrementExact_J__J a))
(go/func StrictMath_NegateExact_I__I ^int32 [^int32 a] (Math_NegateExact_I__I a))
(go/func StrictMath_NegateExact_J__J ^int64 [^int64 a] (Math_NegateExact_J__J a))
(go/func StrictMath_ToIntExact_J__I ^int32 [^int64 a] (Math_ToIntExact_J__I a))
(go/func StrictMath_MultiplyFull_I_I__J ^int64 [^int32 x ^int32 y] (Math_MultiplyFull_I_I__J x y))
(go/func StrictMath_MultiplyHigh_J_J__J ^int64 [^int64 x ^int64 y] (Math_MultiplyHigh_J_J__J x y))
(go/func StrictMath_UnsignedMultiplyHigh_J_J__J ^int64 [^int64 x ^int64 y] (Math_UnsignedMultiplyHigh_J_J__J x y))
(go/func StrictMath_FloorDiv_I_I__I ^int32 [^int32 x ^int32 y] (Math_FloorDiv_I_I__I x y))
(go/func StrictMath_FloorDiv_J_I__J ^int64 [^int64 x ^int32 y] (Math_FloorDiv_J_I__J x y))
(go/func StrictMath_FloorDiv_J_J__J ^int64 [^int64 x ^int64 y] (Math_FloorDiv_J_J__J x y))
(go/func StrictMath_FloorMod_I_I__I ^int32 [^int32 x ^int32 y] (Math_FloorMod_I_I__I x y))
(go/func StrictMath_FloorMod_J_I__I ^int32 [^int64 x ^int32 y] (Math_FloorMod_J_I__I x y))
(go/func StrictMath_FloorMod_J_J__J ^int64 [^int64 x ^int64 y] (Math_FloorMod_J_J__J x y))
(go/func StrictMath_CeilDiv_I_I__I ^int32 [^int32 x ^int32 y] (Math_CeilDiv_I_I__I x y))
(go/func StrictMath_CeilDiv_J_I__J ^int64 [^int64 x ^int32 y] (Math_CeilDiv_J_I__J x y))
(go/func StrictMath_CeilDiv_J_J__J ^int64 [^int64 x ^int64 y] (Math_CeilDiv_J_J__J x y))
(go/func StrictMath_CeilMod_I_I__I ^int32 [^int32 x ^int32 y] (Math_CeilMod_I_I__I x y))
(go/func StrictMath_CeilMod_J_I__I ^int32 [^int64 x ^int32 y] (Math_CeilMod_J_I__I x y))
(go/func StrictMath_CeilMod_J_J__J ^int64 [^int64 x ^int64 y] (Math_CeilMod_J_J__J x y))
(go/func StrictMath_Abs_I__I ^int32 [^int32 a] (Math_Abs_I__I a))
(go/func StrictMath_Abs_J__J ^int64 [^int64 a] (Math_Abs_J__J a))
(go/func StrictMath_Abs_F__F ^float32 [^float32 a] (Math_Abs_F__F a))
(go/func StrictMath_Abs_D__D ^float64 [^float64 a] (Math_Abs_D__D a))
(go/func StrictMath_AbsExact_I__I ^int32 [^int32 a] (Math_AbsExact_I__I a))
(go/func StrictMath_AbsExact_J__J ^int64 [^int64 a] (Math_AbsExact_J__J a))
(go/func StrictMath_Max_I_I__I ^int32 [^int32 a ^int32 b] (Math_Max_I_I__I a b))
(go/func StrictMath_Max_J_J__J ^int64 [^int64 a ^int64 b] (Math_Max_J_J__J a b))
(go/func StrictMath_Max_F_F__F ^float32 [^float32 a ^float32 b] (Math_Max_F_F__F a b))
(go/func StrictMath_Max_D_D__D ^float64 [^float64 a ^float64 b] (Math_Max_D_D__D a b))
(go/func StrictMath_Min_I_I__I ^int32 [^int32 a ^int32 b] (Math_Min_I_I__I a b))
(go/func StrictMath_Min_J_J__J ^int64 [^int64 a ^int64 b] (Math_Min_J_J__J a b))
(go/func StrictMath_Min_F_F__F ^float32 [^float32 a ^float32 b] (Math_Min_F_F__F a b))
(go/func StrictMath_Min_D_D__D ^float64 [^float64 a ^float64 b] (Math_Min_D_D__D a b))
(go/func StrictMath_Clamp_J_I_I__I ^int32 [^int64 v ^int32 lo ^int32 hi] (Math_Clamp_J_I_I__I v lo hi))
(go/func StrictMath_Clamp_J_J_J__J ^int64 [^int64 v ^int64 lo ^int64 hi] (Math_Clamp_J_J_J__J v lo hi))
(go/func StrictMath_Clamp_D_D_D__D ^float64 [^float64 v ^float64 lo ^float64 hi] (Math_Clamp_D_D_D__D v lo hi))
(go/func StrictMath_Clamp_F_F_F__F ^float32 [^float32 v ^float32 lo ^float32 hi] (Math_Clamp_F_F_F__F v lo hi))
(go/func StrictMath_Fma_D_D_D__D ^float64 [^float64 a ^float64 b ^float64 c] (Math_Fma_D_D_D__D a b c))
(go/func StrictMath_Fma_F_F_F__F ^float32 [^float32 a ^float32 b ^float32 c] (Math_Fma_F_F_F__F a b c))
(go/func StrictMath_Ulp_D__D ^float64 [^float64 d] (Math_Ulp_D__D d))
(go/func StrictMath_Ulp_F__F ^float32 [^float32 f] (Math_Ulp_F__F f))
(go/func StrictMath_Signum_D__D ^float64 [^float64 d] (Math_Signum_D__D d))
(go/func StrictMath_Signum_F__F ^float32 [^float32 f] (Math_Signum_F__F f))
(go/func StrictMath_CopySign_D_D__D "StrictMath_CopySign_D_D__D is StrictMath.copySign: a NaN sign counts as positive.\n"
  ^float64 [^float64 m ^float64 s]
  (when (!= s s) (set! s 1.0))
  (Math_CopySign_D_D__D m s))
(go/func StrictMath_CopySign_F_F__F ^float32 [^float32 m ^float32 s]
  (when (!= s s) (set! s 1.0))
  (Math_CopySign_F_F__F m s))
(go/func StrictMath_GetExponent_F__I ^int32 [^float32 f] (Math_GetExponent_F__I f))
(go/func StrictMath_GetExponent_D__I ^int32 [^float64 d] (Math_GetExponent_D__I d))
(go/func StrictMath_NextAfter_D_D__D ^float64 [^float64 s ^float64 d] (Math_NextAfter_D_D__D s d))
(go/func StrictMath_NextAfter_F_D__F ^float32 [^float32 s ^float64 d] (Math_NextAfter_F_D__F s d))
(go/func StrictMath_NextUp_D__D ^float64 [^float64 d] (Math_NextUp_D__D d))
(go/func StrictMath_NextUp_F__F ^float32 [^float32 f] (Math_NextUp_F__F f))
(go/func StrictMath_NextDown_D__D ^float64 [^float64 d] (Math_NextDown_D__D d))
(go/func StrictMath_NextDown_F__F ^float32 [^float32 f] (Math_NextDown_F__F f))
(go/func StrictMath_Scalb_D_I__D ^float64 [^float64 d ^int32 n] (Math_Scalb_D_I__D d n))
(go/func StrictMath_Scalb_F_I__F ^float32 [^float32 f ^int32 n] (Math_Scalb_F_I__F f n))
(go/func StrictMath_UnsignedMultiplyExact_I_I__I ^int32 [^int32 x ^int32 y] (Math_UnsignedMultiplyExact_I_I__I x y))
(go/func StrictMath_UnsignedMultiplyExact_J_I__J ^int64 [^int64 x ^int32 y] (Math_UnsignedMultiplyExact_J_I__J x y))
(go/func StrictMath_UnsignedMultiplyExact_J_J__J ^int64 [^int64 x ^int64 y] (Math_UnsignedMultiplyExact_J_J__J x y))
(go/func StrictMath_PowExact_I_I__I ^int32 [^int32 x ^int32 n] (Math_PowExact_I_I__I x n))
(go/func StrictMath_PowExact_J_I__J ^int64 [^int64 x ^int32 n] (Math_PowExact_J_I__J x n))
(go/func StrictMath_UnsignedPowExact_I_I__I ^int32 [^int32 x ^int32 n] (Math_UnsignedPowExact_I_I__I x n))
(go/func StrictMath_UnsignedPowExact_J_I__J ^int64 [^int64 x ^int32 n] (Math_UnsignedPowExact_J_I__J x n))
