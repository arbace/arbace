;; jrt: the rest of java.lang.FdLibm (openjdk/jdk26u at baf63fb,
;; src/java.base/share/classes/java/lang/FdLibm.java), which StrictMath's sin, cos, tan, asin,
;; acos, atan, atan2, hypot, exp, log10, log1p, expm1, sinh, cosh, tanh and IEEEremainder are
;; (math.clj has Log, Cbrt and Pow): ported as written, in math.clj's style. Every product
;; goes through fmul, so that gc cannot fuse it with an addition (C2G-SPEC §7.4, §13.5); the
;; constants are given by their bits (Java writes them as hexadecimal floats);
;; KernelRemPio2 has only the precision RemPio2 uses (prec 2).
(in-ns 'go.arbace.jrt)

(go/file "fdlibm.go"
  :imports [[math "math"]])

;; ---------------------------------------------------------------------------------------
;; Constants

(go/var
   [fdksS1 (math/Float64frombits 0xbfc5555555555549)] ; -0x1.5555555555549p-3
   [fdksS2 (math/Float64frombits 0x3f8111111110f8a6)] ; 0x1.111111110f8a6p-7
   [fdksS3 (math/Float64frombits 0xbf2a01a019c161d5)] ; -0x1.a01a019c161d5p-13
   [fdksS4 (math/Float64frombits 0x3ec71de357b1fe7d)] ; 0x1.71de357b1fe7dp-19
   [fdksS5 (math/Float64frombits 0xbe5ae5e68a2b9ceb)] ; -0x1.ae5e68a2b9cebp-26
   [fdksS6 (math/Float64frombits 0x3de5d93a5acfd57c)] ; 0x1.5d93a5acfd57cp-33
   [fdkcC1 (math/Float64frombits 0x3fa555555555554c)] ; 0x1.555555555554cp-5
   [fdkcC2 (math/Float64frombits 0xbf56c16c16c15177)] ; -0x1.6c16c16c15177p-10
   [fdkcC3 (math/Float64frombits 0x3efa01a019cb1590)] ; 0x1.a01a019cb159p-16
   [fdkcC4 (math/Float64frombits 0xbe927e4f809c52ad)] ; -0x1.27e4f809c52adp-22
   [fdkcC5 (math/Float64frombits 0x3e21ee9ebdb4b1c4)] ; 0x1.1ee9ebdb4b1c4p-29
   [fdkcC6 (math/Float64frombits 0xbda8fae9be8838d4)] ; -0x1.8fae9be8838d4p-37
   [fdktPio4 (math/Float64frombits 0x3fe921fb54442d18)] ; 0x1.921fb54442d18p-1
   [fdktPio4lo (math/Float64frombits 0x3c81a62633145c07)] ; 0x1.1a62633145c07p-55
   [fdktT0 (math/Float64frombits 0x3fd5555555555563)] ; 0x1.5555555555563p-2
   [fdktT1 (math/Float64frombits 0x3fc111111110fe7a)] ; 0x1.111111110fe7ap-3
   [fdktT2 (math/Float64frombits 0x3faba1ba1bb341fe)] ; 0x1.ba1ba1bb341fep-5
   [fdktT3 (math/Float64frombits 0x3f9664f48406d637)] ; 0x1.664f48406d637p-6
   [fdktT4 (math/Float64frombits 0x3f8226e3e96e8493)] ; 0x1.226e3e96e8493p-7
   [fdktT5 (math/Float64frombits 0x3f6d6d22c9560328)] ; 0x1.d6d22c9560328p-9
   [fdktT6 (math/Float64frombits 0x3f57dbc8fee08315)] ; 0x1.7dbc8fee08315p-10
   [fdktT7 (math/Float64frombits 0x3f4344d8f2f26501)] ; 0x1.344d8f2f26501p-11
   [fdktT8 (math/Float64frombits 0x3f3026f71a8d1068)] ; 0x1.026f71a8d1068p-12
   [fdktT9 (math/Float64frombits 0x3f147e88a03792a6)] ; 0x1.47e88a03792a6p-14
   [fdktT10 (math/Float64frombits 0x3f12b80f32f0a7e9)] ; 0x1.2b80f32f0a7e9p-14
   [fdktT11 (math/Float64frombits 0xbef375cbdb605373)] ; -0x1.375cbdb605373p-16
   [fdktT12 (math/Float64frombits 0x3efb2a7074bf7ad4)] ; 0x1.b2a7074bf7ad4p-16
   [fdrpInvpio2 (math/Float64frombits 0x3fe45f306dc9c883)] ; 0x1.45f306dc9c883p-1
   [fdrpPio2_1 (math/Float64frombits 0x3ff921fb54400000)] ; 0x1.921fb544p0
   [fdrpPio2_1t (math/Float64frombits 0x3dd0b4611a626331)] ; 0x1.0b4611a626331p-34
   [fdrpPio2_2 (math/Float64frombits 0x3dd0b4611a600000)] ; 0x1.0b4611a6p-34
   [fdrpPio2_2t (math/Float64frombits 0x3ba3198a2e037073)] ; 0x1.3198a2e037073p-69
   [fdrpPio2_3 (math/Float64frombits 0x3ba3198a2e000000)] ; 0x1.3198a2ep-69
   [fdrpPio2_3t (math/Float64frombits 0x397b839a252049c1)] ; 0x1.b839a252049c1p-104
   [fdrpTWO24 (math/Float64frombits 0x4170000000000000)] ; 0x1.0p24
   [fdrpTwon24 (math/Float64frombits 0x3e70000000000000)] ; 0x1.0p-24
   [fdasPio2_hi (math/Float64frombits 0x3ff921fb54442d18)] ; 0x1.921fb54442d18p0
   [fdasPio2_lo (math/Float64frombits 0x3c91a62633145c07)] ; 0x1.1a62633145c07p-54
   [fdasPio4_hi (math/Float64frombits 0x3fe921fb54442d18)] ; 0x1.921fb54442d18p-1
   [fdasPS0 (math/Float64frombits 0x3fc5555555555555)] ; 0x1.5555555555555p-3
   [fdasPS1 (math/Float64frombits 0xbfd4d61203eb6f7d)] ; -0x1.4d61203eb6f7dp-2
   [fdasPS2 (math/Float64frombits 0x3fc9c1550e884455)] ; 0x1.9c1550e884455p-3
   [fdasPS3 (math/Float64frombits 0xbfa48228b5688f3b)] ; -0x1.48228b5688f3bp-5
   [fdasPS4 (math/Float64frombits 0x3f49efe07501b288)] ; 0x1.9efe07501b288p-11
   [fdasPS5 (math/Float64frombits 0x3f023de10dfdf709)] ; 0x1.23de10dfdf709p-15
   [fdasQS1 (math/Float64frombits 0xc0033a271c8a2d4b)] ; -0x1.33a271c8a2d4bp1
   [fdasQS2 (math/Float64frombits 0x40002ae59c598ac8)] ; 0x1.02ae59c598ac8p1
   [fdasQS3 (math/Float64frombits 0xbfe6066c1b8d0159)] ; -0x1.6066c1b8d0159p-1
   [fdasQS4 (math/Float64frombits 0x3fb3b8c5b12e9282)] ; 0x1.3b8c5b12e9282p-4
   [fdatAT0 (math/Float64frombits 0x3fd555555555550d)] ; 0x1.555555555550dp-2
   [fdatAT1 (math/Float64frombits 0xbfc999999998ebc4)] ; -0x1.999999998ebc4p-3
   [fdatAT2 (math/Float64frombits 0x3fc24924920083ff)] ; 0x1.24924920083ffp-3
   [fdatAT3 (math/Float64frombits 0xbfbc71c6fe231671)] ; -0x1.c71c6fe231671p-4
   [fdatAT4 (math/Float64frombits 0x3fb745cdc54c206e)] ; 0x1.745cdc54c206ep-4
   [fdatAT5 (math/Float64frombits 0xbfb3b0f2af749a6d)] ; -0x1.3b0f2af749a6dp-4
   [fdatAT6 (math/Float64frombits 0x3fb10d66a0d03d51)] ; 0x1.10d66a0d03d51p-4
   [fdatAT7 (math/Float64frombits 0xbfadde2d52defd9a)] ; -0x1.dde2d52defd9ap-5
   [fdatAT8 (math/Float64frombits 0x3fa97b4b24760deb)] ; 0x1.97b4b24760debp-5
   [fdatAT9 (math/Float64frombits 0xbfa2b4442c6a6c2f)] ; -0x1.2b4442c6a6c2fp-5
   [fdatAT10 (math/Float64frombits 0x3f90ad3ae322da11)] ; 0x1.0ad3ae322da11p-6
   [fda2Pi_o_4 (math/Float64frombits 0x3fe921fb54442d18)] ; 0x1.921fb54442d18p-1
   [fda2Pi_o_2 (math/Float64frombits 0x3ff921fb54442d18)] ; 0x1.921fb54442d18p0
   [fda2Pi_lo (math/Float64frombits 0x3ca1a62633145c07)] ; 0x1.1a62633145c07p-53
   [fdhyTWOm600 (math/Float64frombits 0x1a70000000000000)] ; 0x1.0p-600
   [fdhyTWO600 (math/Float64frombits 0x6570000000000000)] ; 0x1.0p+600
   [fdhyLarge (math/Float64frombits 0x5f300000ffffffff)] ; 0x1.00000ffffffffp500
   [fdhyTWOm500 (math/Float64frombits 0x20b0000000000000)] ; 0x1.0p-500
   [fdhyTWO1022 (math/Float64frombits 0x7fd0000000000000)] ; 0x1.0p1022
   [fdexTwom1000 (math/Float64frombits 0x0170000000000000)] ; 0x1.0p-1000
   [fdexO_threshold (math/Float64frombits 0x40862e42fefa39ef)] ; 0x1.62e42fefa39efp9
   [fdexU_threshold (math/Float64frombits 0xc0874910d52d3051)] ; -0x1.74910d52d3051p9
   [fdexLn2HI (math/Float64frombits 0x3fe62e42fee00000)] ; 0x1.62e42feep-1
   [fdexLn2LO0 (math/Float64frombits 0x3dea39ef35793c76)] ; 0x1.a39ef35793c76p-33
   [fdexLn2LO1 (math/Float64frombits 0xbdea39ef35793c76)] ; -0x1.a39ef35793c76p-33
   [fdexInvln2 (math/Float64frombits 0x3ff71547652b82fe)] ; 0x1.71547652b82fep0
   [fdexP1 (math/Float64frombits 0x3fc555555555553e)] ; 0x1.555555555553ep-3
   [fdexP2 (math/Float64frombits 0xbf66c16c16bebd93)] ; -0x1.6c16c16bebd93p-9
   [fdexP3 (math/Float64frombits 0x3f11566aaf25de2c)] ; 0x1.1566aaf25de2cp-14
   [fdexP4 (math/Float64frombits 0xbebbbd41c5d26bf1)] ; -0x1.bbd41c5d26bf1p-20
   [fdexP5 (math/Float64frombits 0x3e66376972bea4d0)] ; 0x1.6376972bea4d0p-25
   [fdlgIvln10 (math/Float64frombits 0x3fdbcb7b1526e50e)] ; 0x1.bcb7b1526e50ep-2
   [fdlgLog10_2hi (math/Float64frombits 0x3fd34413509f6000)] ; 0x1.34413509f6p-2
   [fdlgLog10_2lo (math/Float64frombits 0x3d59fef311f12b36)] ; 0x1.9fef311f12b36p-42
   [fdl1Ln2_hi (math/Float64frombits 0x3fe62e42fee00000)] ; 0x1.62e42feep-1
   [fdl1Ln2_lo (math/Float64frombits 0x3dea39ef35793c76)] ; 0x1.a39ef35793c76p-33
   [fdl1Lp1 (math/Float64frombits 0x3fe5555555555593)] ; 0x1.5555555555593p-1
   [fdl1Lp2 (math/Float64frombits 0x3fd999999997fa04)] ; 0x1.999999997fa04p-2
   [fdl1Lp3 (math/Float64frombits 0x3fd2492494229359)] ; 0x1.2492494229359p-2
   [fdl1Lp4 (math/Float64frombits 0x3fcc71c51d8e78af)] ; 0x1.c71c51d8e78afp-3
   [fdl1Lp5 (math/Float64frombits 0x3fc7466496cb03de)] ; 0x1.7466496cb03dep-3
   [fdl1Lp6 (math/Float64frombits 0x3fc39a09d078c69f)] ; 0x1.39a09d078c69fp-3
   [fdl1Lp7 (math/Float64frombits 0x3fc2f112df3e5244)] ; 0x1.2f112df3e5244p-3
   [fdemO_threshold (math/Float64frombits 0x40862e42fefa39ef)] ; 0x1.62e42fefa39efp9
   [fdemLn2_hi (math/Float64frombits 0x3fe62e42fee00000)] ; 0x1.62e42feep-1
   [fdemLn2_lo (math/Float64frombits 0x3dea39ef35793c76)] ; 0x1.a39ef35793c76p-33
   [fdemInvln2 (math/Float64frombits 0x3ff71547652b82fe)] ; 0x1.71547652b82fep0
   [fdemQ1 (math/Float64frombits 0xbfa11111111110f4)] ; -0x1.11111111110f4p-5
   [fdemQ2 (math/Float64frombits 0x3f5a01a019fe5585)] ; 0x1.a01a019fe5585p-10
   [fdemQ3 (math/Float64frombits 0xbf14ce199eaadbb7)] ; -0x1.4ce199eaadbb7p-14
   [fdemQ4 (math/Float64frombits 0x3ed0cfca86e65239)] ; 0x1.0cfca86e65239p-18
   [fdemQ5 (math/Float64frombits 0xbe8afdb76e09c32d)] ; -0x1.afdb76e09c32dp-23
   [fdPIo2 (lit (slice float64) (math/Float64frombits 0x3ff921fb40000000) (math/Float64frombits 0x3e74442d00000000) (math/Float64frombits 0x3cf8469880000000) (math/Float64frombits 0x3b78cc5160000000) (math/Float64frombits 0x39f01b8380000000) (math/Float64frombits 0x387a252040000000) (math/Float64frombits 0x36e3822280000000) (math/Float64frombits 0x3569f31d00000000))])

(go/var
   [fdTwoOverPi (lit (slice int32)
     0xA2F983 0x6E4E44 0x1529FC 0x2757D1 0xF534DD 0xC0DB62 0x95993C 0x439041 0xFE5163 0xABDEBB 0xC561B7 0x246E3A 0x424DD2 0xE00649 0x2EEA09 0xD1921C 0xFE1DEB 0x1CB129 0xA73EE8 0x8235F5 0x2EBB44 0x84E99C 0x7026B4 0x5F7E41 0x3991D6 0x398353 0x39F49C 0x845F8B 0xBDF928 0x3B1FF8 0x97FFDE 0x05980F 0xEF2F11 0x8B5A0A 0x6D1F6D 0x367ECF 0x27CB09 0xB74F46 0x3F669E 0x5FEA2D 0x7527BA 0xC7EBE5 0xF17B3D 0x0739F7 0x8A5292 0xEA6BFB 0x5FB11F 0x8D5D08 0x560330 0x46FC7B 0x6BABF0 0xCFBC20 0x9AF436 0x1DA9E3 0x91615E 0xE61B08 0x659985 0x5F14A0 0x68408D 0xFFD880 0x4D7327 0x310606 0x1556CA 0x73A8C9 0x60E27B 0xC08C6B)]
   [fdNpio2Hw (lit (slice int32)
     0x3FF921FB 0x400921FB 0x4012D97C 0x401921FB 0x401F6A7A 0x4022D97C 0x4025FDBB 0x402921FB 0x402C463A 0x402F6A7A 0x4031475C 0x4032D97C 0x40346B9C 0x4035FDBB 0x40378FDB 0x403921FB 0x403AB41B 0x403C463A 0x403DD85A 0x403F6A7A 0x40407E4C 0x4041475C 0x4042106C 0x4042D97C 0x4043A28C 0x40446B9C 0x404534AC 0x4045FDBB 0x4046C6CB 0x40478FDB 0x404858EB 0x404921FB)])

;; ---------------------------------------------------------------------------------------
;; Helpers

(go/func fdHiLo "fdHiLo is FdLibm.__HI_LO: the double of the two 32-bit words.\n" ^float64 [^int32 hi ^int32 lo]
  (math/Float64frombits (bit-or (<< (conv uint64 (conv uint32 hi)) 32) (conv uint64 (conv uint32 lo)))))

(go/func fdUshr "fdUshr is Java's x >>> n on an int (n in 0..31).\n" ^int32 [^int32 x ^int32 n]
  (conv int32 (>> (conv uint32 x) n)))

;; ---------------------------------------------------------------------------------------
;; Sin, Cos, Tan and their kernels

(go/func fdKernelSin "fdKernelSin is FdLibm.Sin.__kernel_sin.\n" ^float64 [^float64 x ^float64 y ^int32 iy]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (and (< ix 0x3e400000) (== (D2I x) 0))
      (return x))
    (let [z (fmul x x)
          v (fmul z x)
          r (+ fdksS2 (fmul z (+ fdksS3 (fmul z (+ fdksS4 (fmul z (+ fdksS5 (fmul z fdksS6))))))))]
      (when (== iy 0)
        (return (+ x (fmul v (+ fdksS1 (fmul z r))))))
      (- x (- (- (fmul z (- (fmul 0.5 y) (fmul v r))) y) (fmul v fdksS1))))))

(go/func fdKernelCos "fdKernelCos is FdLibm.Cos.__kernel_cos.\n" ^float64 [^float64 x ^float64 y]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (and (< ix 0x3e400000) (== (D2I x) 0))
      (return 1.0))
    (let [z (fmul x x)
          r (fmul z (+ fdkcC1 (fmul z (+ fdkcC2 (fmul z (+ fdkcC3 (fmul z (+ fdkcC4 (fmul z (+ fdkcC5 (fmul z fdkcC6)))))))))))]
      (when (< ix 0x3fd33333)
        (return (- 1.0 (- (fmul 0.5 z) (- (fmul z r) (fmul x y))))))
      (let [qx 0.28125]
        (when (<= ix 0x3fe90000)
          (set! qx (fdHiLo (- ix 0x00200000) 0)))
        (let [hz (- (fmul 0.5 z) qx)
              a (- 1.0 qx)]
          (- a (- hz (- (fmul z r) (fmul x y)))))))))

(go/func fdKernelTan "fdKernelTan is FdLibm.Tan.__kernel_tan.\n" ^float64 [^float64 x ^float64 y ^int32 iy]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)]
    (when (and (< ix 0x3e300000) (== (D2I x) 0))
      (when (== (bit-or ix (fdLo x) (+ iy 1)) 0)
        (return (/ 1.0 (math/Abs x))))
      (when (== iy 1)
        (return x))
      (let [w (+ x y)
            z (fdSetLo w 0)
            v (- y (- z x))
            a (/ -1.0 w)
            t (fdSetLo a 0)
            s (+ 1.0 (fmul t z))]
        (return (+ t (fmul a (+ s (fmul t v)))))))
    (when (>= ix 0x3fe59428)
      (when (< hx 0)
        (set! x (- x))
        (set! y (- y)))
      (let [z (- fdktPio4 x)
            w (- fdktPio4lo y)]
        (set! x (+ z w))
        (set! y 0.0)))
    (let [z (fmul x x)
          w (fmul z z)
          r (+ fdktT1 (fmul w (+ fdktT3 (fmul w (+ fdktT5 (fmul w (+ fdktT7 (fmul w (+ fdktT9 (fmul w fdktT11))))))))))
          v (fmul z (+ fdktT2 (fmul w (+ fdktT4 (fmul w (+ fdktT6 (fmul w (+ fdktT8 (fmul w (+ fdktT10 (fmul w fdktT12)))))))))))
          s (fmul z x)]
      (set! r (+ y (fmul z (+ (fmul s (+ r v)) y))))
      (set! r (+ r (fmul fdktT0 s)))
      (set! w (+ x r))
      (when (>= ix 0x3fe59428)
        (set! v (conv float64 iy))
        (return (fmul (conv float64 (- 1 (bit-and (>> hx 30) 2)))
                      (- v (fmul 2.0 (- x (- (/ (fmul w w) (+ w v)) r)))))))
      (when (== iy 1)
        (return w))
      (let [z2 (fdSetLo w 0)
            v2 (- r (- z2 x))
            a (/ -1.0 w)
            t (fdSetLo a 0)
            s2 (+ 1.0 (fmul t z2))]
        (+ t (fmul a (+ s2 (fmul t v2))))))))

(go/func fdRemPio2
  "fdRemPio2 is FdLibm.RemPio2.__ieee754_rem_pio2: n and x - n*pi/2 as y0 + y1.\n"
  [^float64 x] :results [int32 float64 float64]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)]
    (when (<= ix 0x3fe921fb)
      (return 0 x 0.0))
    (when (< ix 0x4002d97c)
      (when (> hx 0)
        (let [z (- x fdrpPio2_1)]
          (when (!= ix 0x3ff921fb)
            (let [y0 (- z fdrpPio2_1t)]
              (return 1 y0 (- (- z y0) fdrpPio2_1t))))
          (set! z (- z fdrpPio2_2))
          (let [y0 (- z fdrpPio2_2t)]
            (return 1 y0 (- (- z y0) fdrpPio2_2t)))))
      (let [z (+ x fdrpPio2_1)]
        (when (!= ix 0x3ff921fb)
          (let [y0 (+ z fdrpPio2_1t)]
            (return -1 y0 (+ (- z y0) fdrpPio2_1t))))
        (set! z (+ z fdrpPio2_2))
        (let [y0 (+ z fdrpPio2_2t)]
          (return -1 y0 (+ (- z y0) fdrpPio2_2t)))))
    (when (<= ix 0x413921fb)
      (let [t (math/Abs x)
            n (D2I (+ (fmul t fdrpInvpio2) 0.5))
            fnn (conv float64 n)
            r (- t (fmul fnn fdrpPio2_1))
            w (fmul fnn fdrpPio2_1t)
            y0 0.0]
        (if (and (< n 32) (!= ix (aget fdNpio2Hw (- n 1))))
          (set! y0 (- r w))
          (let [j (>> ix 20)]
            (set! y0 (- r w))
            (let [i (- j (bit-and (>> (fdHi y0) 20) 0x7ff))]
              (when (> i 16)
                (set! t r)
                (set! w (fmul fnn fdrpPio2_2))
                (set! r (- t w))
                (set! w (- (fmul fnn fdrpPio2_2t) (- (- t r) w)))
                (set! y0 (- r w))
                (set! i (- j (bit-and (>> (fdHi y0) 20) 0x7ff)))
                (when (> i 49)
                  (set! t r)
                  (set! w (fmul fnn fdrpPio2_3))
                  (set! r (- t w))
                  (set! w (- (fmul fnn fdrpPio2_3t) (- (- t r) w)))
                  (set! y0 (- r w)))))))
        (let [y1 (- (- r y0) w)]
          (when (< hx 0)
            (return (- n) (- y0) (- y1)))
          (return n y0 y1))))
    (when (>= ix 0x7ff00000)
      (let [d (- x x)]
        (return 0 d d)))
    (let [e0 (- (>> ix 20) 1046)
          z (fdSetHi (fdSetLo 0.0 (fdLo x)) (- ix (<< e0 20)))
          tx (make (slice float64) 3)]
      (for [i 0] (< i 2) (inc! i)
        (aset tx i (conv float64 (D2I z)))
        (set! z (fmul (- z (aget tx i)) fdrpTWO24)))
      (aset tx 2 z)
      (let [nx 3]
        (while (== (aget tx (- nx 1)) 0.0)
          (dec! nx))
        (let [(values n y0 y1) (fdKernelRemPio2 (subslice tx _ nx) e0)]
          (when (< hx 0)
            (return (- n) (- y0) (- y1)))
          (return n y0 y1))))))

(go/func fdKernelRemPio2
  "fdKernelRemPio2 is FdLibm.KernelRemPio2.__kernel_rem_pio2 with prec 2 (the one
RemPio2 uses) and ipio2 two_over_pi: x the 24-bit chunks of the argument.\n"
  [^{:tag (slice float64)} x ^int32 e0] :results [int32 float64 float64]
  (let [iq (lit (array 20 int32))
        f (lit (array 20 float64))
        fq (lit (array 20 float64))
        q (lit (array 20 float64))
        jk (conv int32 4)
        jp jk
        jx (- (conv int32 (len x)) 1)
        jv (/ (- e0 3) 24)]
    (when (< jv 0)
      (set! jv 0))
    (let [q0 (- e0 (* 24 (+ jv 1)))
          j (- jv jx)
          m (+ jx jk)
          z 0.0
          fw 0.0
          jz jk
          n (conv int32 0)
          ih (conv int32 0)
          carry (conv int32 0)
          k (conv int32 0)]
      (for [i (conv int32 0)] (<= i m) (inc! i)
        (if (< j 0)
          (aset f i 0.0)
          (aset f i (conv float64 (aget fdTwoOverPi j))))
        (inc! j))
      (for [i (conv int32 0)] (<= i jk) (inc! i)
        (set! fw 0.0)
        (for [jj (conv int32 0)] (<= jj jx) (inc! jj)
          (set! fw (+ fw (fmul (aget x jj) (aget f (- (+ jx i) jj))))))
        (aset q i fw))
      (while true
        (set! z (aget q jz))
        (let [i (conv int32 0)]
          (for [jj jz] (> jj 0) (dec! jj)
            (set! fw (conv float64 (D2I (fmul fdrpTwon24 z))))
            (aset iq i (D2I (- z (fmul fdrpTWO24 fw))))
            (set! z (+ (aget q (- jj 1)) fw))
            (inc! i)))
        (set! z (Math_Scalb_D_I__D z q0))
        (set! z (- z (fmul 8.0 (math/Floor (fmul z 0.125)))))
        (set! n (D2I z))
        (set! z (- z (conv float64 n)))
        (set! ih 0)
        (cond
          (> q0 0) (let [i (>> (aget iq (- jz 1)) (- 24 q0))]
                     (set! n (+ n i))
                     (aset iq (- jz 1) (- (aget iq (- jz 1)) (<< i (- 24 q0))))
                     (set! ih (>> (aget iq (- jz 1)) (- 23 q0))))
          (== q0 0) (set! ih (>> (aget iq (- jz 1)) 23))
          (>= z 0.5) (set! ih 2))
        (when (> ih 0)
          (inc! n)
          (set! carry 0)
          (for [i (conv int32 0)] (< i jz) (inc! i)
            (let [jj (aget iq i)]
              (if (== carry 0)
                (when (!= jj 0)
                  (set! carry 1)
                  (aset iq i (- 0x1000000 jj)))
                (aset iq i (- 0xffffff jj)))))
          (when (> q0 0)
            (cond
              (== q0 1) (aset iq (- jz 1) (bit-and (aget iq (- jz 1)) 0x7fffff))
              (== q0 2) (aset iq (- jz 1) (bit-and (aget iq (- jz 1)) 0x3fffff))))
          (when (== ih 2)
            (set! z (- 1.0 z))
            (when (!= carry 0)
              (set! z (- z (Math_Scalb_D_I__D 1.0 q0))))))
        (when (!= z 0.0)
          (break))
        (let [jj (conv int32 0)]
          (for [i (- jz 1)] (>= i jk) (dec! i)
            (set! jj (bit-or jj (aget iq i))))
          (when (!= jj 0)
            (break)))
        ;; need recomputation: k terms more
        (set! k 1)
        (while (== (aget iq (- jk k)) 0)
          (inc! k))
        (for [i (+ jz 1)] (<= i (+ jz k)) (inc! i)
          (aset f (+ jx i) (conv float64 (aget fdTwoOverPi (+ jv i))))
          (set! fw 0.0)
          (for [jj (conv int32 0)] (<= jj jx) (inc! jj)
            (set! fw (+ fw (fmul (aget x jj) (aget f (- (+ jx i) jj))))))
          (aset q i fw))
        (set! jz (+ jz k)))
      (if (== z 0.0)
        (do
          (set! jz (- jz 1))
          (set! q0 (- q0 24))
          (while (== (aget iq jz) 0)
            (dec! jz)
            (set! q0 (- q0 24))))
        (do
          (set! z (Math_Scalb_D_I__D z (- q0)))
          (if (>= z fdrpTWO24)
            (do
              (set! fw (conv float64 (D2I (fmul fdrpTwon24 z))))
              (aset iq jz (D2I (- z (fmul fdrpTWO24 fw))))
              (inc! jz)
              (set! q0 (+ q0 24))
              (aset iq jz (D2I fw)))
            (aset iq jz (D2I z)))))
      (set! fw (Math_Scalb_D_I__D 1.0 q0))
      (for [i jz] (>= i 0) (dec! i)
        (aset q i (fmul fw (conv float64 (aget iq i))))
        (set! fw (fmul fw fdrpTwon24)))
      (for [i jz] (>= i 0) (dec! i)
        (set! fw 0.0)
        (for [kk (conv int32 0)] (and (<= kk jp) (<= kk (- jz i))) (inc! kk)
          (set! fw (+ fw (fmul (aget fdPIo2 kk) (aget q (+ i kk))))))
        (aset fq (- jz i) fw))
      (set! fw 0.0)
      (for [i jz] (>= i 0) (dec! i)
        (set! fw (+ fw (aget fq i))))
      (let [y0 fw]
        (when (!= ih 0)
          (set! y0 (- fw)))
        (set! fw (- (aget fq 0) fw))
        (for [i (conv int32 1)] (<= i jz) (inc! i)
          (set! fw (+ fw (aget fq i))))
        (let [y1 fw]
          (when (!= ih 0)
            (set! y1 (- fw)))
          (return (bit-and n 7) y0 y1))))))

(go/func fdSin "fdSin is FdLibm.Sin.compute (StrictMath.sin).\n" ^float64 [^float64 x]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (<= ix 0x3fe921fb)
      (return (fdKernelSin x 0.0 0)))
    (when (>= ix 0x7ff00000)
      (return (- x x)))
    (let [(values n y0 y1) (fdRemPio2 x)]
      (switch (bit-and n 3)
        (case [0] (return (fdKernelSin y0 y1 1)))
        (case [1] (return (fdKernelCos y0 y1)))
        (case [2] (return (- (fdKernelSin y0 y1 1)))))
      (- (fdKernelCos y0 y1)))))

(go/func fdCos "fdCos is FdLibm.Cos.compute (StrictMath.cos).\n" ^float64 [^float64 x]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (<= ix 0x3fe921fb)
      (return (fdKernelCos x 0.0)))
    (when (>= ix 0x7ff00000)
      (return (- x x)))
    (let [(values n y0 y1) (fdRemPio2 x)]
      (switch (bit-and n 3)
        (case [0] (return (fdKernelCos y0 y1)))
        (case [1] (return (- (fdKernelSin y0 y1 1))))
        (case [2] (return (- (fdKernelCos y0 y1)))))
      (fdKernelSin y0 y1 1))))

(go/func fdTan "fdTan is FdLibm.Tan.compute (StrictMath.tan).\n" ^float64 [^float64 x]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (<= ix 0x3fe921fb)
      (return (fdKernelTan x 0.0 1)))
    (when (>= ix 0x7ff00000)
      (return (- x x)))
    (let [(values n y0 y1) (fdRemPio2 x)]
      (fdKernelTan y0 y1 (- 1 (<< (bit-and n 1) 1))))))

;; ---------------------------------------------------------------------------------------
;; Asin, Acos, Atan, Atan2

(go/func fdAsP "fdAsP is Asin's and Acos's numerator polynomial p(t).\n" ^float64 [^float64 t]
  (fmul t (+ fdasPS0 (fmul t (+ fdasPS1 (fmul t (+ fdasPS2 (fmul t (+ fdasPS3 (fmul t (+ fdasPS4 (fmul t fdasPS5))))))))))))

(go/func fdAsQ "fdAsQ is Asin's and Acos's denominator polynomial q(t).\n" ^float64 [^float64 t]
  (+ 1.0 (fmul t (+ fdasQS1 (fmul t (+ fdasQS2 (fmul t (+ fdasQS3 (fmul t fdasQS4)))))))))

(go/func fdAsin "fdAsin is FdLibm.Asin.compute (StrictMath.asin).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)
        t 0.0]
    (when (>= ix 0x3ff00000)
      (when (== (bit-or (- ix 0x3ff00000) (fdLo x)) 0)
        (return (+ (fmul x fdasPio2_hi) (fmul x fdasPio2_lo))))
      (return (/ (- x x) (- x x))))
    (when (< ix 0x3fe00000)
      (if (< ix 0x3e400000)
        (when (> (+ 1.0e300 x) 1.0)
          (return x))
        (set! t (fmul x x)))
      (let [w (/ (fdAsP t) (fdAsQ t))]
        (return (+ x (fmul x w)))))
    (let [w (- 1.0 (math/Abs x))]
      (set! t (fmul w 0.5))
      (let [p (fdAsP t)
            q (fdAsQ t)
            s (math/Sqrt t)]
        (if (>= ix 0x3fef3333)
          (do
            (set! w (/ p q))
            (set! t (- fdasPio2_hi (- (fmul 2.0 (+ s (fmul s w))) fdasPio2_lo))))
          (do
            (set! w (fdSetLo s 0))
            (let [c (/ (- t (fmul w w)) (+ s w))
                  r (/ p q)
                  p2 (- (fmul (fmul 2.0 s) r) (- fdasPio2_lo (fmul 2.0 c)))
                  q2 (- fdasPio4_hi (fmul 2.0 w))]
              (set! t (- fdasPio4_hi (- p2 q2))))))
        (when (> hx 0)
          (return t))
        (- t)))))

(go/func fdAcos "fdAcos is FdLibm.Acos.compute (StrictMath.acos).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)]
    (when (>= ix 0x3ff00000)
      (when (== (bit-or (- ix 0x3ff00000) (fdLo x)) 0)
        (when (> hx 0)
          (return 0.0))
        (return (+ Math_PI (fmul 2.0 fdasPio2_lo))))
      (return (/ (- x x) (- x x))))
    (when (< ix 0x3fe00000)
      (when (<= ix 0x3c600000)
        (return (+ fdasPio2_hi fdasPio2_lo)))
      (let [z (fmul x x)
            r (/ (fdAsP z) (fdAsQ z))]
        (return (- fdasPio2_hi (- x (- fdasPio2_lo (fmul x r)))))))
    (when (< hx 0)
      (let [z (fmul (+ 1.0 x) 0.5)
            p (fdAsP z)
            q (fdAsQ z)
            s (math/Sqrt z)
            r (/ p q)
            w (- (fmul r s) fdasPio2_lo)]
        (return (- Math_PI (fmul 2.0 (+ s w))))))
    (let [z (fmul (- 1.0 x) 0.5)
          s (math/Sqrt z)
          df (fdSetLo s 0)
          c (/ (- z (fmul df df)) (+ s df))
          p (fdAsP z)
          q (fdAsQ z)
          r (/ p q)
          w (+ (fmul r s) c)]
      (fmul 2.0 (+ df w)))))

(go/var
   [fdAtanHi (lit (slice float64) (math/Float64frombits 0x3fddac670561bb4f) (math/Float64frombits 0x3fe921fb54442d18)
                  (math/Float64frombits 0x3fef730bd281f69b) (math/Float64frombits 0x3ff921fb54442d18))]
   [fdAtanLo (lit (slice float64) (math/Float64frombits 0x3c7a2b7f222f65e2) (math/Float64frombits 0x3c81a62633145c07)
                  (math/Float64frombits 0x3c7007887af0cbbd) (math/Float64frombits 0x3c91a62633145c07))])

(go/func fdAtan "fdAtan is FdLibm.Atan.compute (StrictMath.atan).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)
        id (conv int32 0)]
    (when (>= ix 0x44100000)
      (when (or (> ix 0x7ff00000) (and (== ix 0x7ff00000) (!= (fdLo x) 0)))
        (return (+ x x)))
      (when (> hx 0)
        (return (+ (aget fdAtanHi 3) (aget fdAtanLo 3))))
      (return (- (- (aget fdAtanHi 3)) (aget fdAtanLo 3))))
    (if (< ix 0x3fdc0000)
      (do
        (when (and (< ix 0x3e200000) (> (+ 1.0e300 x) 1.0))
          (return x))
        (set! id -1))
      (do
        (set! x (math/Abs x))
        (if (< ix 0x3ff30000)
          (if (< ix 0x3fe60000)
            (do (set! id 0) (set! x (/ (- (fmul 2.0 x) 1.0) (+ 2.0 x))))
            (do (set! id 1) (set! x (/ (- x 1.0) (+ x 1.0)))))
          (if (< ix 0x40038000)
            (do (set! id 2) (set! x (/ (- x 1.5) (+ 1.0 (fmul 1.5 x)))))
            (do (set! id 3) (set! x (/ -1.0 x)))))))
    (let [z (fmul x x)
          w (fmul z z)
          s1 (fmul z (+ fdatAT0 (fmul w (+ fdatAT2 (fmul w (+ fdatAT4 (fmul w (+ fdatAT6 (fmul w (+ fdatAT8 (fmul w fdatAT10)))))))))))
          s2 (fmul w (+ fdatAT1 (fmul w (+ fdatAT3 (fmul w (+ fdatAT5 (fmul w (+ fdatAT7 (fmul w fdatAT9)))))))))]
      (when (< id 0)
        (return (- x (fmul x (+ s1 s2)))))
      (set! z (- (aget fdAtanHi id) (- (- (fmul x (+ s1 s2)) (aget fdAtanLo id)) x)))
      (when (< hx 0)
        (return (- z)))
      z)))

(go/func fdAtan2 "fdAtan2 is FdLibm.Atan2.compute (StrictMath.atan2).\n" ^float64 [^float64 y ^float64 x]
  (let [hx (fdHi x)
        ix (bit-and hx 0x7fffffff)
        lx (fdLo x)
        hy (fdHi y)
        iy (bit-and hy 0x7fffffff)
        ly (fdLo y)
        tiny 1.0e-300]
    (when (or (math/IsNaN x) (math/IsNaN y))
      (return (+ x y)))
    (when (== (bit-or (- hx 0x3ff00000) lx) 0)
      (return (fdAtan y)))
    (let [m (bit-or (bit-and (>> hy 31) 1) (bit-and (>> hx 30) 2))]
      (when (== (bit-or iy ly) 0)
        (switch m
          (case [0 1] (return y))
          (case [2] (return (+ Math_PI tiny)))
          (case [3] (return (- (- Math_PI) tiny)))))
      (when (== (bit-or ix lx) 0)
        (when (< hy 0)
          (return (- (- fda2Pi_o_2) tiny)))
        (return (+ fda2Pi_o_2 tiny)))
      (when (== ix 0x7ff00000)
        (if (== iy 0x7ff00000)
          (switch m
            (case [0] (return (+ fda2Pi_o_4 tiny)))
            (case [1] (return (- (- fda2Pi_o_4) tiny)))
            (case [2] (return (+ (fmul 3.0 fda2Pi_o_4) tiny)))
            (case [3] (return (- (fmul -3.0 fda2Pi_o_4) tiny))))
          (switch m
            (case [0] (return 0.0))
            (case [1] (return NegZero64))
            (case [2] (return (+ Math_PI tiny)))
            (case [3] (return (- (- Math_PI) tiny))))))
      (when (== iy 0x7ff00000)
        (when (< hy 0)
          (return (- (- fda2Pi_o_2) tiny)))
        (return (+ fda2Pi_o_2 tiny)))
      (let [k (>> (- iy ix) 20)
            z 0.0]
        (cond
          (> k 60) (set! z (+ fda2Pi_o_2 (fmul 0.5 fda2Pi_lo)))
          (and (< hx 0) (< k -60)) (set! z 0.0)
          :else (set! z (fdAtan (math/Abs (/ y x)))))
        (switch m
          (case [0] (return z))
          (case [1] (return (- z)))
          (case [2] (return (- Math_PI (- z fda2Pi_lo)))))
        (- (- z fda2Pi_lo) Math_PI)))))

;; ---------------------------------------------------------------------------------------
;; Hypot

(go/func fdHypot "fdHypot is FdLibm.Hypot.compute (StrictMath.hypot).\n" ^float64 [^float64 x ^float64 y]
  (let [a (math/Abs x)
        b (math/Abs y)]
    (when (or (math/IsInf a 0) (math/IsNaN a) (math/IsInf b 0) (math/IsNaN b))
      (when (or (== a PosInf64) (== b PosInf64))
        (return PosInf64))
      (return (+ a b)))
    (when (> b a)
      (set! (values a b) (values b a)))
    (let [ha (fdHi a)
          hb (fdHi b)]
      (when (> (- ha hb) 0x3c00000)
        (return (+ a b)))
      (let [k (conv int32 0)
            t1 0.0
            t2 0.0]
        (when (> a fdhyLarge)
          (set! ha (- ha 0x25800000))
          (set! hb (- hb 0x25800000))
          (set! a (fmul a fdhyTWOm600))
          (set! b (fmul b fdhyTWOm600))
          (set! k (+ k 600)))
        (when (< b fdhyTWOm500)
          (if (< b 2.2250738585072014E-308)
            (do
              (when (== b 0.0)
                (return a))
              (set! t1 fdhyTWO1022)
              (set! b (fmul b t1))
              (set! a (fmul a t1))
              (set! k (- k 1022)))
            (do
              (set! ha (+ ha 0x25800000))
              (set! hb (+ hb 0x25800000))
              (set! a (fmul a fdhyTWO600))
              (set! b (fmul b fdhyTWO600))
              (set! k (- k 600)))))
        (let [w (- a b)]
          (if (> w b)
            (do
              (set! t1 (fdSetHi 0.0 ha))
              (set! t2 (- a t1))
              (set! w (math/Sqrt (- (fmul t1 t1) (- (fmul b (- b)) (fmul t2 (+ a t1)))))))
            (do
              (set! a (+ a a))
              (let [y1 (fdSetHi 0.0 hb)
                    y2 (- b y1)]
                (set! t1 (fdSetHi 0.0 (+ ha 0x00100000)))
                (set! t2 (- a t1))
                (set! w (math/Sqrt (- (fmul t1 y1) (- (fmul w (- w)) (+ (fmul t1 y2) (fmul t2 b)))))))))
          (when (!= k 0)
            (return (fmul (powerOfTwoD k) w)))
          w)))))

;; ---------------------------------------------------------------------------------------
;; Exp, Log10, Log1p, Expm1

(go/func fdExp "fdExp is FdLibm.Exp.compute (StrictMath.exp).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        xsb (bit-and (>> hx 31) 1)
        hi 0.0
        lo 0.0
        k (conv int32 0)]
    (set! hx (bit-and hx 0x7fffffff))
    (when (>= hx 0x40862E42)
      (when (>= hx 0x7ff00000)
        (when (!= (bit-or (bit-and hx 0xfffff) (fdLo x)) 0)
          (return (+ x x)))
        (when (== xsb 0)
          (return x))
        (return 0.0))
      (when (> x fdexO_threshold)
        (return PosInf64))
      (when (< x fdexU_threshold)
        (return (fmul fdexTwom1000 fdexTwom1000))))
    (cond
      (> hx 0x3fd62e42)
      (do
        (if (< hx 0x3FF0A2B2)
          (do
            (set! hi (- x (fmul fdexLn2HI (conv float64 (- 1 (* 2 xsb))))))
            (if (== xsb 0) (set! lo fdexLn2LO0) (set! lo fdexLn2LO1))
            (set! k (- 1 xsb xsb)))
          (do
            (set! k (D2I (+ (fmul fdexInvln2 x) (fmul 0.5 (conv float64 (- 1 (* 2 xsb)))))))
            (let [t (conv float64 k)]
              (set! hi (- x (fmul t fdexLn2HI)))
              (set! lo (fmul t fdexLn2LO0)))))
        (set! x (- hi lo)))
      (< hx 0x3e300000)
      (when (> (+ 1.0e300 x) 1.0)
        (return (+ 1.0 x)))
      :else (set! k 0))
    (let [t (fmul x x)
          c (- x (fmul t (+ fdexP1 (fmul t (+ fdexP2 (fmul t (+ fdexP3 (fmul t (+ fdexP4 (fmul t fdexP5))))))))))]
      (when (== k 0)
        (return (- 1.0 (- (/ (fmul x c) (- c 2.0)) x))))
      (let [y (- 1.0 (- (- lo (/ (fmul x c) (- 2.0 c))) hi))]
        (when (>= k -1021)
          (return (fdSetHi y (+ (fdHi y) (<< k 20)))))
        (set! y (fdSetHi y (+ (fdHi y) (<< (+ k 1000) 20))))
        (fmul y fdexTwom1000)))))

(go/func fdLog10 "fdLog10 is FdLibm.Log10.compute (StrictMath.log10).\n" ^float64 [^float64 x]
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
    (let [i (fdUshr k 31)
          y (conv float64 (+ k i))]
      (set! hx (bit-or (bit-and hx 0x000fffff) (<< (- 0x3ff i) 20)))
      (set! x (fdSetHi x hx))
      (let [z (+ (fmul y fdlgLog10_2lo) (fmul fdlgIvln10 (fdLog x)))]
        (+ z (fmul y fdlgLog10_2hi))))))

(go/func fdLog1p "fdLog1p is FdLibm.Log1p.compute (StrictMath.log1p).\n" ^float64 [^float64 x]
  (let [f 0.0
        c 0.0
        u 0.0
        hx (fdHi x)
        ax (bit-and hx 0x7fffffff)
        k (conv int32 1)
        hu (conv int32 0)]
    (when (< hx 0x3FDA827A)
      (when (>= ax 0x3ff00000)
        (when (== x -1.0)
          (return NegInf64))
        (return NaN64))
      (when (< ax 0x3e200000)
        (when (and (> (+ fdTWO54 x) 0.0) (< ax 0x3c900000))
          (return x))
        (return (- x (fmul (fmul x x) 0.5))))
      (when (or (> hx 0) (<= hx -1076707645))
        (set! k 0)
        (set! f x)
        (set! hu 1)))
    (when (>= hx 0x7ff00000)
      (return (+ x x)))
    (when (!= k 0)
      (if (< hx 0x43400000)
        (do
          (set! u (+ 1.0 x))
          (set! hu (fdHi u))
          (set! k (- (>> hu 20) 1023))
          (if (> k 0)
            (set! c (- 1.0 (- u x)))
            (set! c (- x (- u 1.0))))
          (set! c (/ c u)))
        (do
          (set! u x)
          (set! hu (fdHi u))
          (set! k (- (>> hu 20) 1023))
          (set! c 0.0)))
      (set! hu (bit-and hu 0x000fffff))
      (if (< hu 0x6a09e)
        (set! u (fdSetHi u (bit-or hu 0x3ff00000)))
        (do
          (inc! k)
          (set! u (fdSetHi u (bit-or hu 0x3fe00000)))
          (set! hu (>> (- 0x00100000 hu) 2))))
      (set! f (- u 1.0)))
    (let [hfsq (fmul (fmul 0.5 f) f)
          dk (conv float64 k)]
      (when (== hu 0)
        (when (== f 0.0)
          (when (== k 0)
            (return 0.0))
          (set! c (+ c (fmul dk fdl1Ln2_lo)))
          (return (+ (fmul dk fdl1Ln2_hi) c)))
        (let [R (fmul hfsq (- 1.0 (fmul 0.66666666666666666 f)))]
          (when (== k 0)
            (return (- f R)))
          (return (- (fmul dk fdl1Ln2_hi) (- (- R (+ (fmul dk fdl1Ln2_lo) c)) f)))))
      (let [s (/ f (+ 2.0 f))
            z (fmul s s)
            R (fmul z (+ fdl1Lp1 (fmul z (+ fdl1Lp2 (fmul z (+ fdl1Lp3 (fmul z (+ fdl1Lp4 (fmul z (+ fdl1Lp5 (fmul z (+ fdl1Lp6 (fmul z fdl1Lp7)))))))))))))]
        (when (== k 0)
          (return (- f (- hfsq (fmul s (+ hfsq R))))))
        (- (fmul dk fdl1Ln2_hi) (- (- hfsq (+ (fmul s (+ hfsq R)) (+ (fmul dk fdl1Ln2_lo) c))) f))))))

(go/func fdExpm1 "fdExpm1 is FdLibm.Expm1.compute (StrictMath.expm1).\n" ^float64 [^float64 x]
  (let [hx (fdHi x)
        xsb (bit-and hx (conv int32 -2147483648))
        k (conv int32 0)
        c 0.0
        hi 0.0
        lo 0.0
        y 0.0]
    (set! hx (bit-and hx 0x7fffffff))
    (when (>= hx 0x4043687A)
      (when (>= hx 0x40862E42)
        (when (>= hx 0x7ff00000)
          (when (!= (bit-or (bit-and hx 0xfffff) (fdLo x)) 0)
            (return (+ x x)))
          (when (== xsb 0)
            (return x))
          (return -1.0))
        (when (> x fdemO_threshold)
          (return PosInf64)))
      (when (and (!= xsb 0) (< (+ x 1.0e-300) 0.0))
        (return -1.0)))
    (cond
      (> hx 0x3fd62e42)
      (do
        (if (< hx 0x3FF0A2B2)
          (if (== xsb 0)
            (do (set! hi (- x fdemLn2_hi)) (set! lo fdemLn2_lo) (set! k 1))
            (do (set! hi (+ x fdemLn2_hi)) (set! lo (- fdemLn2_lo)) (set! k -1)))
          (do
            (if (== xsb 0)
              (set! k (D2I (+ (fmul fdemInvln2 x) 0.5)))
              (set! k (D2I (+ (fmul fdemInvln2 x) -0.5))))
            (let [t (conv float64 k)]
              (set! hi (- x (fmul t fdemLn2_hi)))
              (set! lo (fmul t fdemLn2_lo)))))
        (set! x (- hi lo))
        (set! c (- (- hi x) lo)))
      (< hx 0x3c900000)
      (let [t (+ 1.0e300 x)]
        (return (- x (- t (+ 1.0e300 x)))))
      :else (set! k 0))
    (let [hfx (fmul 0.5 x)
          hxs (fmul x hfx)
          r1 (+ 1.0 (fmul hxs (+ fdemQ1 (fmul hxs (+ fdemQ2 (fmul hxs (+ fdemQ3 (fmul hxs (+ fdemQ4 (fmul hxs fdemQ5))))))))))
          t (- 3.0 (fmul r1 hfx))
          e (fmul hxs (/ (- r1 t) (- 6.0 (fmul x t))))]
      (when (== k 0)
        (return (- x (- (fmul x e) hxs))))
      (set! e (- (fmul x (- e c)) c))
      (set! e (- e hxs))
      (when (== k -1)
        (return (- (fmul 0.5 (- x e)) 0.5)))
      (when (== k 1)
        (when (< x -0.25)
          (return (fmul -2.0 (- e (+ x 0.5)))))
        (return (+ 1.0 (fmul 2.0 (- x e)))))
      (when (or (<= k -2) (> k 56))
        (set! y (- 1.0 (- e x)))
        (set! y (fdSetHi y (+ (fdHi y) (<< k 20))))
        (return (- y 1.0)))
      (set! t 1.0)
      (if (< k 20)
        (do
          (set! t (fdSetHi t (- 0x3ff00000 (>> (conv int32 0x200000) k))))
          (set! y (- t (- e x)))
          (set! y (fdSetHi y (+ (fdHi y) (<< k 20)))))
        (do
          (set! t (fdSetHi t (<< (- 0x3ff k) 20)))
          (set! y (- x (+ e t)))
          (set! y (+ y 1.0))
          (set! y (fdSetHi y (+ (fdHi y) (<< k 20))))))
      y)))

;; ---------------------------------------------------------------------------------------
;; Sinh, Cosh, Tanh

(go/func fdSinh "fdSinh is FdLibm.Sinh.compute (StrictMath.sinh).\n" ^float64 [^float64 x]
  (let [jx (fdHi x)
        ix (bit-and jx 0x7fffffff)
        h 0.5]
    (when (>= ix 0x7ff00000)
      (return (+ x x)))
    (when (< jx 0)
      (set! h (- h)))
    (when (< ix 0x40360000)
      (when (and (< ix 0x3e300000) (> (+ 1.0e307 x) 1.0))
        (return x))
      (let [t (fdExpm1 (math/Abs x))]
        (when (< ix 0x3ff00000)
          (return (fmul h (- (fmul 2.0 t) (/ (fmul t t) (+ t 1.0))))))
        (return (fmul h (+ t (/ t (+ t 1.0)))))))
    (when (< ix 0x40862E42)
      (return (fmul h (fdExp (math/Abs x)))))
    (when (or (< ix 0x408633CE) (and (== ix 0x408633CE) (<= (conv uint32 (fdLo x)) 0x8fb9f87d)))
      (let [w (fdExp (fmul 0.5 (math/Abs x)))
            t (fmul h w)]
        (return (fmul t w))))
    (fmul x 1.0e307)))

(go/func fdCosh "fdCosh is FdLibm.Cosh.compute (StrictMath.cosh).\n" ^float64 [^float64 x]
  (let [ix (bit-and (fdHi x) 0x7fffffff)]
    (when (>= ix 0x7ff00000)
      (return (fmul x x)))
    (when (< ix 0x3fd62e43)
      (let [t (fdExpm1 (math/Abs x))
            w (+ 1.0 t)]
        (when (< ix 0x3c800000)
          (return w))
        (return (+ 1.0 (/ (fmul t t) (+ w w))))))
    (when (< ix 0x40360000)
      (let [t (fdExp (math/Abs x))]
        (return (+ (fmul 0.5 t) (/ 0.5 t)))))
    (when (< ix 0x40862E42)
      (return (fmul 0.5 (fdExp (math/Abs x)))))
    (when (or (< ix 0x408633CE) (and (== ix 0x408633CE) (<= (conv uint32 (fdLo x)) 0x8fb9f87d)))
      (let [w (fdExp (fmul 0.5 (math/Abs x)))
            t (fmul 0.5 w)]
        (return (fmul t w))))
    PosInf64))

(go/func fdTanh "fdTanh is FdLibm.Tanh.compute (StrictMath.tanh).\n" ^float64 [^float64 x]
  (let [jx (fdHi x)
        ix (bit-and jx 0x7fffffff)
        z 0.0]
    (when (>= ix 0x7ff00000)
      (when (>= jx 0)
        (return (+ (/ 1.0 x) 1.0)))
      (return (- (/ 1.0 x) 1.0)))
    (if (< ix 0x40360000)
      (do
        (when (< ix 0x3c800000)
          (return (fmul x (+ 1.0 x))))
        (if (>= ix 0x3ff00000)
          (let [t (fdExpm1 (fmul 2.0 (math/Abs x)))]
            (set! z (- 1.0 (/ 2.0 (+ t 2.0)))))
          (let [t (fdExpm1 (fmul -2.0 (math/Abs x)))]
            (set! z (/ (- t) (+ t 2.0))))))
      (set! z 1.0))
    (when (>= jx 0)
      (return z))
    (- z)))

;; ---------------------------------------------------------------------------------------
;; IEEEremainder

(go/func fdSignedZero ^float64 [^int32 sign] (fmul 0.0 (conv float64 sign)))

(go/func fdIlogb ^int32 [^int32 hz ^int32 lz]
  (let [iz (conv int32 0)]
    (if (< hz 0x00100000)
      (if (== hz 0)
        (do
          (set! iz -1043)
          (for [i lz] (> i 0) (set! i (<< i 1))
            (set! iz (- iz 1))))
        (do
          (set! iz -1022)
          (for [i (<< hz 11)] (> i 0) (set! i (<< i 1))
            (set! iz (- iz 1)))))
      (set! iz (- (>> hz 20) 1023)))
    iz))

(go/func fdFmod "fdFmod is FdLibm.IEEEremainder.__ieee754_fmod.\n" ^float64 [^float64 x ^float64 y]
  (let [hx (fdHi x)
        lx (fdLo x)
        hy (fdHi y)
        ly (fdLo y)
        sx (bit-and hx (conv int32 -2147483648))
        n (conv int32 0)
        hz (conv int32 0)
        lz (conv int32 0)]
    (set! hx (bit-xor hx sx))
    (set! hy (bit-and hy 0x7fffffff))
    (when (or (== (bit-or hy ly) 0) (>= hx 0x7ff00000)
              (> (bit-or hy (fdUshr (bit-or ly (- ly)) 31)) 0x7ff00000))
      (return (/ (fmul x y) (fmul x y))))
    (when (<= hx hy)
      (when (or (< hx hy) (< (conv uint32 lx) (conv uint32 ly)))
        (return x))
      (when (== lx ly)
        (return (fdSignedZero sx))))
    (let [ix (fdIlogb hx lx)
          iy (fdIlogb hy ly)]
      (if (>= ix -1022)
        (set! hx (bit-or 0x00100000 (bit-and 0x000fffff hx)))
        (do
          (set! n (- -1022 ix))
          (if (<= n 31)
            (do
              (set! hx (bit-or (<< hx n) (fdUshr lx (- 32 n))))
              (set! lx (<< lx n)))
            (do
              (set! hx (<< lx (- n 32)))
              (set! lx 0)))))
      (if (>= iy -1022)
        (set! hy (bit-or 0x00100000 (bit-and 0x000fffff hy)))
        (do
          (set! n (- -1022 iy))
          (if (<= n 31)
            (do
              (set! hy (bit-or (<< hy n) (fdUshr ly (- 32 n))))
              (set! ly (<< ly n)))
            (do
              (set! hy (<< ly (- n 32)))
              (set! ly 0)))))
      (set! n (- ix iy))
      (while (!= n 0)
        (dec! n)
        (set! hz (- hx hy))
        (set! lz (- lx ly))
        (when (< (conv uint32 lx) (conv uint32 ly))
          (set! hz (- hz 1)))
        (if (< hz 0)
          (do
            (set! hx (+ hx hx (fdUshr lx 31)))
            (set! lx (+ lx lx)))
          (do
            (when (== (bit-or hz lz) 0)
              (return (fdSignedZero sx)))
            (set! hx (+ hz hz (fdUshr lz 31)))
            (set! lx (+ lz lz)))))
      (set! hz (- hx hy))
      (set! lz (- lx ly))
      (when (< (conv uint32 lx) (conv uint32 ly))
        (set! hz (- hz 1)))
      (when (>= hz 0)
        (set! hx hz)
        (set! lx lz))
      (when (== (bit-or hx lx) 0)
        (return (fdSignedZero sx)))
      (while (< hx 0x00100000)
        (set! hx (+ hx hx (fdUshr lx 31)))
        (set! lx (+ lx lx))
        (set! iy (- iy 1)))
      (if (>= iy -1022)
        (do
          (set! hx (bit-or (- hx 0x00100000) (<< (+ iy 1023) 20)))
          (set! x (fdHiLo (bit-or hx sx) lx)))
        (do
          (set! n (- -1022 iy))
          (cond
            (<= n 20) (do
                        (set! lx (bit-or (fdUshr lx n) (<< hx (- 32 n))))
                        (set! hx (>> hx n)))
            (<= n 31) (do
                        (set! lx (bit-or (<< hx (- 32 n)) (fdUshr lx n)))
                        (set! hx sx))
            :else (do
                    (set! lx (>> hx (- n 32)))
                    (set! hx sx)))
          (set! x (fdHiLo (bit-or hx sx) lx))
          (set! x (fmul x 1.0))))
      x)))

(go/func fdIEEEremainder "fdIEEEremainder is FdLibm.IEEEremainder.compute (StrictMath.IEEEremainder).\n"
  ^float64 [^float64 x ^float64 p]
  (let [hx (fdHi x)
        lx (fdLo x)
        hp (fdHi p)
        lp (fdLo p)
        sx (bit-and hx (conv int32 -2147483648))]
    (set! hp (bit-and hp 0x7fffffff))
    (set! hx (bit-and hx 0x7fffffff))
    (when (== (bit-or hp lp) 0)
      (return (/ (fmul x p) (fmul x p))))
    (when (or (>= hx 0x7ff00000) (and (>= hp 0x7ff00000) (!= (bit-or (- hp 0x7ff00000) lp) 0)))
      (return (/ (fmul x p) (fmul x p))))
    (when (<= hp 0x7fdfffff)
      (set! x (fdFmod x (+ p p))))
    (when (== (bit-or (- hx hp) (- lx lp)) 0)
      (return (fmul 0.0 x)))
    (set! x (math/Abs x))
    (set! p (math/Abs p))
    (if (< hp 0x00200000)
      (when (> (+ x x) p)
        (set! x (- x p))
        (when (>= (+ x x) p)
          (set! x (- x p))))
      (let [pHalf (fmul 0.5 p)]
        (when (> x pHalf)
          (set! x (- x p))
          (when (>= x pHalf)
            (set! x (- x p))))))
    (fdSetHi x (bit-xor (fdHi x) sx))))
