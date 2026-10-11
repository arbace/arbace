;; Regex corpus: greedy, lazy and possessive quantifiers, counted closures, their effect on
;; groups; patterns that backtrack a lot, on short inputs only.

{:patterns ["a*" "a+" "a?" "a{2}" "a{2,}" "a{1,3}" "a{0}" "a{0,0}" "a{0,1}" "a*?" "a+?" "a??" "a{2,}?" "a{1,3}?" "a*+"
            "a++" "a?+" "a{1,3}+" "a{2}+" "a{3,2}a"]
 :inputs ["" "a" "aa" "aaa" "aaaa" "baaa" "aaab" "abaab" "AAA"]
 :replace ["<$0>"]}

{:patterns ["(a*)b" "(a*?)b" "(a*+)b" "(a*+)a" "(a+)(a+)" "(a+?)(a+)" "(a+)(a+?)" "(a{1,2}){2}" "(a|b)*" "(a|b)*?c"
            "(a|ab)*c" "(ab|a)*c" "(a*)*" "(a*)+" "(a?)+b" "(a|)+" "(?:(a)|b)*" "(?:(a)|(b))+" "((a)|b)+" "(a(b)?)+"
            "(\\w)+" "(\\w)*?x" "(.)*" "(.)+?$" "(a{0,2}){3}" "x(.*)y(.*)z" "x(.*?)y(.*)z"]
 :inputs ["" "b" "ab" "aab" "aaa" "abab" "abac" "aabbc" "xyz" "xayybz" "aba" "c"]}

{:patterns [".*foo" ".*?foo" ".*+foo" "<.+>" "<.+?>" "<[^>]+>" "\\d{2,4}" "\\d{2,4}?" "\\d{2,4}+\\d"
            "[a-z]{2}[0-9]{2,}" "(?:ab){2,3}" "(?:ab){2,3}?" "a{,3}" "a{1,}b{,}"]
 :inputs ["foo" "xfooyfoo" "<a><b>" "<>" "12345678" "12" "ab12ab123" "ababababab" "a{,3}" "a{1,}b{,}"]}

;; backtracking-heavy patterns, kept to short inputs
{:patterns ["(a+)+b" "(a|a)*b" "(a|aa)+$" "(x+x+)+y" "(.*a){4}" "^(a?){5}a{5}$" "(\\w+\\s?)+$" "(?>a+)+b" "(a++)+b"
            "^(\\d+)*$" "(a*)*b"]
 :inputs ["" "ab" "aaaaaaaaaaaa" "aaaaaaaaaaaab" "xxxxxxxxxxy" "xxxxxxxxxx" "aaaaa" "aaaaaaaaaa" "aaaa aaaa aaaa!"
          "123456789012x"]}

;; quantified zero-width and empty constructs
{:patterns ["(?:)*" "()*" "(?:\\b)+" "(?=a)*" "(^)*" "(a|$)+" "(?:a?)*?b" "(|b)*?c" "\\b*" "^*" "$+"]
 :inputs ["" "a" "ab" "aab" "bc" "c"]}
