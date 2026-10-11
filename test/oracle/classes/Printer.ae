;; RT.printString and RT.print (the printer, readably), of every kind of value; the values
;; are read with RT.readString or constructed.

(each [s ["nil" "true" "42" "-0.0" "1.0E300" "1.0E-300" "123456789.123" "1.0E7" "1.0E-4" "0.001" "##Inf" "##-Inf" "##NaN" "1/3" "-7N" "1.50M" "1E+5M" "0.000001M"
          "\"\"" "\"plain\"" "\"q\\\"b\\\\s\"" "\"\\n\\t\\r\\b\\f\"" "\"\\u0000\\u0007\\u001f\\u007f\"" "\"\\u00e9\\u2028\\uffff\"" "\"\\ud83d\\ude00\"" "\"\\ud800\""
          "\\a" "\\newline" "\\space" "\\tab" "\\return" "\\backspace" "\\formfeed" "\\u0000" "\\u00e9" "\\u2028" "\\\"" "\\\\"
          "sym" "ns/sym" ":kw" ":ns/kw" "a.b.C" "/"
          "()" "[]" "{}" "#{}" "(1 \"two\" :three \\4 nil)" "[[] [[]] {}]" "{:a {:b {:c [1 #{2}]}}}" "{nil nil}" "#{nil}" "{\"k\" \"v\", 1 2}" "#:a{:b 1 :c 2}" "{:a/b 1 :c/d 2}" "{:a/b 1}" "#:a{:b {:c 1}}"
          "(quote x)" "'x" "@x" "#'x" "(fn* [] 1)"]]
  (def v (RT/readString s))
  (RT/printString v))

;; RT.print to a writer
(each [s ["\"q\\\"b\\n\"" "\\newline" "{:a [1 #{2}] \"k\" (3)}" "1.5M" "##NaN" ":ns/kw"]]
  (def v (RT/readString s))
  (def sw (new java.io.StringWriter))
  (RT/print v sw)
  (.toString sw))

;; the number types without a literal
(each [x [(int -5) (short 7) (byte -1) (float 1.5) (float 0.1) (float -0.0) (biginteger "-123456789012345678901234567890") (bigdecimal "-0.00")]]
  (RT/printString x))
(def bi (BigInt/fromBigInteger (biginteger "99999999999999999999")))
(RT/printString bi)
(def r (new Ratio (biginteger "-6") (biginteger "4")))
(RT/printString r)

;; collections built by the classes
(def big (LongRange/create 0 100))
(def bigv ^{:sig [arbace.lang.ISeq]} (PersistentVector/create big))
(RT/printString bigv)
(def nested (RT/readString "{:vec [1 2 [3 4 [5 6 [7 8 [9]]]]] :map {:a {:b {:c {:d {:e \"deep\"}}}}} :set #{[1] [2]} :list (((())))}"))
(RT/printString nested)
(def me (new MapEntry 1 "one"))
(RT/printString me)
(def tms (ArraySeq/create (array Object 3 "c" 1 "a" 2 "b")))
(def tmap ^{:sig [arbace.lang.ISeq]} (PersistentTreeMap/create tms))
(RT/printString tmap)
(def ka (Keyword/intern "a"))
(def mt (RT/map (array Object ka 1)))
(def vm (.withMeta tmap mt))
(RT/printString vm)
(def sm (RT/readString "^{:x 1} sym"))
(RT/printString sm)
(def em (RT/map (array Object)))
(RT/printString em)

;; Java collections and objects print as their Clojure counterparts
(def al (new java.util.ArrayList))
(.add al 1)
(.add al "s")
(RT/printString al)
(def hm (new java.util.TreeMap))
(.put hm "k" 1)
(RT/printString hm)
(def hs (new java.util.TreeSet))
(.add hs 3)
(.add hs 1)
(RT/printString hs)
(def cls (Class/forName "java.lang.String"))
(RT/printString cls)
(def cls2 (Class/forName "arbace.lang.PersistentVector"))
(RT/printString cls2)
(def var (RT/var "arbace.core" "+"))
(RT/printString var)
(def kwv (Keyword/intern "weird name"))
(RT/printString kwv)
(def syv (Symbol/intern "weird name"))
(RT/printString syv)

;; RT.format: ~A, ~S, ~%, ~~
(def fsw (new java.io.StringWriter))
(RT/format fsw "a=~A s=~S n=~A~%~~" (array Object "str" "str" 1))
(.toString fsw)
(RT/format nil "~S" (array Object "x"))
(def fsw2 (new java.io.StringWriter))
(RT/formatAesthetic fsw2 nested)
(.toString fsw2)
(def fsw3 (new java.io.StringWriter))
(RT/formatStandard fsw3 "s")
(.toString fsw3)
