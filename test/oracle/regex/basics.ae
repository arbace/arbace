;; Regex corpus: literals and escapes, the empty pattern and empty input, dot, alternation order.
;; Data forms read by test/oracle/runner.clj (regex-cases): every pattern x flag set is one
;; case over all the inputs. Non-ASCII characters are written as \uXXXX escapes.

;; plain literals
{:patterns ["a" "abc" "aa" "hello world" "-,=@~%'\"/"]
 :flag-sets [[] [:i]]
 :inputs ["" "a" "abc" "ABC" "aaa" "xabcx" "Hello World" "x-,=@~%'\"/" "aAaA"]}

;; the empty pattern
{:pattern ""
 :flag-sets [[] [:m] [:x]]
 :inputs ["" "a" "ab" "abc" "\n" "a\nb" "\uD83D\uDE00" "a\uD83D\uDE00b"]
 :replace ["-" "[$0]" "\\$"]}

;; control escapes
{:patterns ["\\t" "\\n" "\\r" "\\f" "\\a" "\\e" "\\r\\n" "\\t+" "a\\tb"]
 :inputs ["" "\t" "\n" "\r" "\f" "\u0007" "\u001B" "\r\n" "a\tb" "\t\t\t" "a b" "\\t" "x\n\ry"]}

;; hex, unicode, octal, control-letter and named-character escapes
{:patterns ["\\x41" "\\x61" "\\x{41}" "\\x{1F600}" "\\x{10FFFF}" "\\u0041" "\\u00e9" "\\u00E9"
            "\\uD83D\\uDE00" "\\uD83D" "\\uDE00" "\\0101" "\\0141" "\\00" "\\07" "\\0377" "\\0400" "\\cA" "\\cJ"
            "\\c@" "\\c?" "\\x{0}" "\\N{LATIN SMALL LETTER E WITH ACUTE}" "\\N{GRINNING FACE}" "\\N{SPACE}"]
 :inputs ["" "A" "a" "\u00E9" "\u00C9" "\uD83D\uDE00" "\uD83D" "\uDE00" "\u0000" "\u0001" "\n"
          "\u00FF" " 0" " 0" "\uDBFF\uDFFF" "x\uD83D\uDE00y\uD83D\uDE00" "\u007F"]}

{:patterns ["\\x41" "\\u00e9" "\\x{1F600}" "\\0101" "\\x{e9}"]
 :flags [:i]
 :inputs ["a" "A" "\u00E9" "\u00C9" "\uD83D\uDE00"]}

;; \Q..\E quoting and escaped metacharacters
{:patterns ["\\Qa.b\\E" "\\Q.*\\E" "\\Q(\\E" "\\Q\\E" "\\Qab" "a\\Q\\E*" "\\Q\\\\E" "\\Q[a]\\E+" "x\\Q$^\\Ey"
            "\\." "\\*" "\\+" "\\?" "\\(" "\\)" "\\[" "\\]" "\\{" "\\}" "\\|" "\\^" "\\$" "\\\\" "\\/" "\\-" "\\#"
            "\\ " "\\!" "\\@" "\\<" "\\>" "\\\"" "\\'" "\\=" "\\:" "\\,"]
 :inputs ["a.b" "axb" "x.*y(ab*" "\\E[a][a]" "x$^y?+*.|^$" "{}[]() a b#!@<>\"'=:,/-"]}

;; dot
{:patterns ["." "a.c" ".+" "^.$" "a.*b" "a.+?b"]
 :flag-sets [[] [:s] [:d] [:s :d]]
 :inputs ["" "abc" "a\nc" "a\rc" "a\r\nc" "a\u0085c" "a\u2028c" "a\u2029c" "\uD83D\uDE00" "a\uD83D\uDE00c"
          "\uD83D" "a\nb\nab"]}

;; alternation and its order
{:patterns ["a|ab" "ab|a" "a|b|c" "|a" "a|" "(a|ab)(c|bcd)(d*)" "(ab|a)(c|bcd)(d*)" "x(a|b)*y" "a||b" "(|a)+"
            "cat|category" "category|cat" "(?:a|b)+" "foo|foobar|fo" "\\d+|\\w+"]
 :inputs ["" "a" "ab" "abcd" "Abcd" "xaby" "xy" "category" "CAT" "foobar" "12ab" "aab"]
 :replace ["<$0>"]}

;; whitespace and comment characters outside COMMENTS mode
{:patterns ["a b" "a#b" "  " "a\\ b" "#"]
 :inputs ["" "a b" "ab" "a#b" "  " "   " "#"]}
