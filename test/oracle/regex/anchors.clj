;; Regex corpus: anchors ^ $ \A \z \Z \b \B \G with and without MULTILINE and UNIX_LINES, over
;; the line terminators \n \r\n \r \u0085 \u2028 \u2029.

{:patterns ["^" "$" "^a" "a$" "^a$" "^$" "\\A" "\\z" "\\Z" "\\Aa" "a\\z" "a\\Z" "^.*$" "^\\w+" "\\w+$" "(?:^|,)x" "x(?:$|,)"
            "$\\n?" "^\\s*$"]
 :flag-sets [[] [:m] [:d] [:m :d]]
 :inputs ["" "a" "a\n" "\na\nb" "a\r\nb\r\n" "a\rb\r" "a\u0085b" "a\u2028b\u2029" "\r\n" "a\n\nx,x\nx"]
 :replace ["<$0>"]}

{:patterns ["\\b" "\\B" "\\bfoo\\b" "\\Bo\\B" "\\b\\w" "\\w\\b" "\\b.\\b" "\\b{g}" "\\b{g}.\\b{g}"]
 :flag-sets [[] [:U]]
 :inputs ["" "foo" "foo bar" " foo." "foobar" "a_b-c" "\u00E9t\u00E9 x" "caf\u00E9" "\u0663x" "e\u0301 \u0301"
          "\uD83D\uDE00a" "\uD83C\uDDEB\uD83C\uDDF7\uD83C\uDDE9\uD83C\uDDEA"]
 :replace ["|"]}

;; \G: the end of the previous match
{:patterns ["\\Ga" "\\G\\w" "\\G(a|b)" "a\\G" "\\G" "\\G,?\\d" "(?:\\G|x)a"]
 :inputs ["" "aaa" "aab" "baa" "abab" "1,2,3,,4" "xaaxa" "a"]
 :replace ["[$0]"]}

;; empty lines and the end of the input
{:patterns ["(?m)^" "(?m)$" "(?m)^$" "(?m)^.+$" "(?m)\\Z" "(?m)^\\r?$" "(?md)^." "(?md)$"]
 :inputs ["" "\n" "a\n" "\na\n\n" "a\r\n\r\nb" "a\r\r\n" "\u2029" "a\rb"]
 :replace ["#"]}
