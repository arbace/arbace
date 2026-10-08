;; arbace.lang.LispReader through RT.readString and LispReader.read on a PushbackReader, and
;; arbace.lang.EdnReader: the literals, the dispatch macros, syntax quote, reader errors.
;; The current namespace is user.

(each [s ["nil" "true" "false" "42" "-42" "+42" "0" "-0" "007" "08" "0x1F" "-0X1f" "0xG" "2r1010" "36rZZ" "37r1" "8r777" "1N" "0x10N" "9223372036854775807" "9223372036854775808" "-9223372036854775808" "-9223372036854775809"
          "1.5" "-1.5" "1e3" "1E-3" "1.5e300" "1e400" "1.5M" "1M" "0.1M" "1e2M" "1.5N" "1/2" "-1/2" "4/2" "0/5" "1/2N" "##Inf" "##-Inf" "##NaN" "##Foo" "1a" "1.2.3" "0x" "1_000"]]
  (RT/readString s))

(each [s ["\"\"" "\"abc\"" "\"a\\nb\\tc\\\\d\\\"e\"" "\"\\u00e9\\u0041\"" "\"\\101\"" "\"\\777\"" "\"\\q\"" "\"\\u12\"" "\"unterminated" "\"multi\nline\""
          "\\a" "\\newline" "\\space" "\\tab" "\\backspace" "\\formfeed" "\\return" "\\u0041" "\\u00e9" "\\o101" "\\o400" "\\ud800" "\\foo" "\\" "\\(" "\\1"]]
  (RT/readString s))

(each [s ["a" "a.b" "a/b" "a.b/c" "/" "a//" "a/b/c" "." ".." "a." ".a" "-" "+" "-a" "+1a" "a#" "a'" "a:b" "foo/" "/foo" "nil/x"
          ":a" ":a/b" ":a.b/c" "::a" "::user/a" ":" "::" ":a:" ":1" ":/" ":a/" "::a/b"]]
  (RT/readString s))

(each [s ["()" "(1 2 3)" "[]" "[1 [2 [3]]]" "{}" "{:a 1 :b 2}" "{:a 1 :a 2}" "{:a}" "#{}" "#{1 2 3}" "#{1 1}" "(1 2" "[1 2" "{:a 1" ")" "]" "}" "(1 2))"
          "(1 ; comment\n 2)" "(1 #_2 3)" "(#_#_1 2 3)" "[1,2,,3]" "'a" "'(1 2)" "@a" "#'a" "`a" "`(a b)" "`(a ~b ~@c)" "`:k" "`\"s\"" "`1" "`nil" "`a/b" "`String" "`[a]" "`{a 1}" "`#{a}" "~a" "~@a"
          "^:k a" "^{:a 1} [1]" "^String x" "^\"T\" x" "^1 x" "#^:k a" "#\"a.b\"" "#\"\\d+\"" "#\"[\"" "#(1)" "#(+ % %2)" "#(%&)" "#(#(%))" "#{1" "#?(:clj 1)" "#:a{:b 1}" "#:a{:b 1 :_/c 2 d 3}" "#::{:b 1}" "#_" "#!shebang\n1" "#inst \"2020-01-02T03:04:05.006Z\"" "#uuid \"00000000-0000-0000-0000-000000000001\"" "#uuid \"bad\"" "#foo 1" "#<thing>" "" "  " "; only a comment"]]
  (RT/readString s))

;; metadata, reader conditionals and options
(def kw (RT/readString "^:k ^{:a 1} [x]"))
(RT/meta kw)
(def kt (RT/readString "^String s"))
(RT/meta kt)
(def kl (RT/readString "(a\n b)"))
(RT/meta kl)
(def ka (Keyword/intern "read-cond"))
(def kallow (Keyword/intern "allow"))
(def kpreserve (Keyword/intern "preserve"))
(def kfeatures (Keyword/intern "features"))
(def opts (RT/map (array Object ka kallow)))
(RT/readString "#?(:clj 1 :default 2)" opts)
(RT/readString "#?(:cljs 1 :default 2)" opts)
(RT/readString "#?(:cljs 1)" opts)
(RT/readString "[#?(:cljs 1) 2]" opts)
(RT/readString "[#?@(:clj [1 2] :cljs [3]) 4]" opts)
(RT/readString "#?@(:clj [1 2])" opts)
(RT/readString "#?(:clj 1 :clj 2)" opts)
(RT/readString "#?(:clj)" opts)
(RT/readString "#?[:clj 1]" opts)
(RT/readString "#?(:arbace 1 :clj 2)" opts)
(def popts (RT/map (array Object ka kpreserve)))
(RT/readString "#?(:clj 1 :cljs 2)" popts)
(RT/readString "#?@(:clj [1] :cljs [2])" popts)
(def cljs (Keyword/intern "cljs"))
(def fset (RT/set (array Object cljs)))
(def fopts (RT/map (array Object ka kallow kfeatures fset)))
(RT/readString "#?(:cljs 1 :clj 2)" fopts)

;; LispReader.read on a reader: successive forms, end of input
(def sr (new java.io.StringReader "(1 2) [3] :four \"five\" 6 ; done"))
(def pr (new java.io.PushbackReader sr))
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr nil)
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr nil)
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr nil)
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr nil)
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr nil)
^{:sig [java.io.PushbackReader boolean Object boolean]} (LispReader/read pr false "EOF!" false)
^{:sig [java.io.PushbackReader boolean Object boolean]} (LispReader/read pr true nil false)
(def sr2 (new java.io.StringReader "abc)"))
(def pr2 (new java.io.PushbackReader sr2))
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr2 nil)
^{:sig [java.io.PushbackReader Object]} (LispReader/read pr2 nil)
(def sr3 (new java.io.StringReader "1\n  (2\n 3)"))
(def lr (new LineNumberingPushbackReader sr3))
^{:sig [java.io.PushbackReader Object]} (LispReader/read lr nil)
(.getLineNumber lr)
(def form2 ^{:sig [java.io.PushbackReader Object]} (LispReader/read lr nil))
(RT/meta form2)
(.getLineNumber lr)
(.getColumnNumber lr)
(def sr4 (new java.io.StringReader "[1 2 (3"))
(def lr4 (new LineNumberingPushbackReader sr4))
^{:sig [java.io.PushbackReader Object]} (LispReader/read lr4 nil)

;; EdnReader: no syntax quote, no reader eval, no ::
(def eopts PersistentArrayMap/EMPTY)
(each [s ["{:a [1 2.5 \"s\" \\c nil true]}" "#{1 2}" "(a b)" "'a" "`a" "::a" "#=(+ 1 2)" "#inst \"2020-01-01\"" "#_1 2" "1N" "1M" "1/2" "#foo 1" "\\u00e9" "@a" "^:m x" "#\"re\"" "a/b/c"]]
  (EdnReader/readString s eopts))

;; values that are observed through their printed form
(def re (RT/readString "#\"a\\.b\\\\d\""))
(RT/printString re)
(.pattern re)
(def dt (RT/readString "#inst \"2020-01-02T03:04:05.006Z\""))
(RT/printString dt)
(.getTime dt)
(def dt2 (RT/readString "#inst \"2020-01-02T03:04:05.006+01:00\""))
(.getTime dt2)
(def uu (RT/readString "#uuid \"00000000-0000-0000-0000-00000000000A\""))
(RT/printString uu)
(.toString uu)
(def rc (RT/readString "#?(:clj 1 :cljs [2])" popts))
(RT/printString rc)
(.-form rc)
(.-splicing rc)
(def rcs (RT/readString "#?@(:clj [1] :cljs [2])" popts))
(RT/printString rcs)
(def tl (RT/readString "#?(:cljs #js [1])" popts))
(RT/printString tl)
