;; SPEC §8: literals (integers, floats exact, imaginary, runes, strings, byte strings) and
;; constant values (:val, ignored by the printer).
(ns go.literals
  (:require [arbace.go :as go]))

(go/package literals :path "literals" :files ["s08_literals.go"])

(go/file "s08_literals.go" :imports [])

(go/const
  [^{:val 42} i1 42]
  [^{:val 42} i2 0x2A]
  [^{:val 42} i3 052]
  [^{:val 42} i4 052]
  [^{:val 42} i5 2r101010]
  [^{:val 1000} i6 1000]
  [^{:val 4611686018427387904} i7 (<< 1 62)]
  [^{:val 123456789012345678901234567890N} i8 123456789012345678901234567890N]
  [^{:val 1.5} f1 1.5]
  [^{:val 1.0E10} f2 1.0E10]
  [^{:val 0.5} f3 0.5]
  [^{:val 1E+400M} f4 1E+400M]
  [^{:val 1/4} f5 1/4]
  [^{:val 0.1} f6 0.1]
  [^{:val 1.0} f7 1.0]
  [f8 1/202402253307310618352495346718917307049556649764142118356901358027430339567995346891960383701437124495187077864316811911389808737385793476867013399940738509921517424276566361364466907742093216341239767678472745068562007483424692698618103355649159556340810056512358769552333414615230502532186327508646006263307707741093494784]
  [^{:val 6.02214076E23} f9 6.02214076E23]
  [^{:val (complex 0.0 2.0)} c1 (imaginary 2)]
  [^{:val (complex 0.0 1500.0)} c2 (imaginary 1500.0)]
  [^{:val \a} r1 \a]
  [^{:val \newline} r2 \newline]
  [^{:val \é} r3 \é]
  [^{:val \u0000} r4 \u0000]
  [^{:val (rune 0x1F600)} r5 (rune 0x1F600)]
  [r6 \']
  [r7 \\]
  [r8 \u2028]
  [^{:val "abc"} s1 "abc"]
  [^{:val "a\\b"} s2 "a\\b"]
  [^{:val (byte-string 0xff "\u0000a")} s3 (byte-string 0xff "\u0000a")]
  [s4 "tab\tquote\"backslash\\"]
  [s5 "ünïcödé €"]
  [s6 "\u0007\b\f\r\u000b"]
  [s7 "\u00a0\ufeff"]
  [^{:val true} b1 true]
  [^{:val false} b2 false]
  [^{:val -1} n1 -1]
  [^{:val 0.0} n2 -0.0]
  [^{:val 1} n3 (- -1)]
  [^{:val -1.5} n4 -1.5]
  [^{:val -1/4} n5 -1/4])

(go/const ^{:val "line one\nline two\ttabbed\n"} raw "line one\nline two\ttabbed\n")

(go/const ^{:val 1267650600228229401496703205376N} big (<< 1 100))

(go/const ^{:val 1E+100M} bigf 1E+100M)

(go/var
  [^{:tag error} nilv nil]
  [neg (- i1)]
  [negBig -12345678901234567]
  [runeConv (conv rune 65)]
  [sum (+ \a 1)])
