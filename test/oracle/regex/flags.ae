;; Regex corpus: inline flags (?i) (?-i) (?i:...) (?x) (?s) (?m) (?u) (?U) (?d), COMMENTS mode,
;; the LITERAL flag. Non-ASCII characters are written as \uXXXX escapes.

{:patterns ["(?i)abc" "a(?i)bc" "(?i)a(?-i)bc" "(?i:a)b" "a(?i:b)c" "(?i)a(?-i:b)c" "(?i)(?u)\u00e9" "(?iu)\u00e9"
            "(?i)\u00e9" "(?i:\u00e9)" "(?-i)abc" "((?i)a)b" "(?i)[a-c]+" "(?i)[^a]" "(?i)\\p{Lower}" "(?i)\\p{Ll}"
            "(?i)\\p{javaLowerCase}" "(?i)\\p{IsLowercase}" "(?i)(a)\\1"]
 :flag-sets [[] [:i]]
 :inputs ["abc" "ABC" "aBC" "Abc" "abC" "\u00e9\u00c9" "aA" "B\u01C5"]}

{:patterns ["(?s).+" "(?s:.)." "(?-s).+" "(?m)^b" "(?m:^b)|^a" "(?-m)^b" "(?d)a.b" "(?d)^b" "(?dm)^b$" "(?sd)."
            "(?U)\\w+" "(?U)\\d" "(?U)\\b.+?\\b" "(?U)[[:alpha:]]" "(?U)\\p{Alpha}+" "(?ims-d)^A.$" "(?u)\u00c9"
            "(?ismx-u) a . b"]
 :flag-sets [[] [:s :m]]
 :inputs ["a\nb" "a\rb" "a\r\nb" "\u00e9t\u00e9\u0663" "a\u2028b" "A\n\u00e9\n"]}

;; COMMENTS mode
{:patterns ["a b c" "a b # comment\nc" "a\\ b" "[a b]" "[a\\ b]" "a # b" "a#b" "a\\#b" "(?x) a b " "(?x: a ) b" "a{ 2 }"
            "a {2}" "\\p{ L }" "a\tb\nc" "( a ) \\1" "(?<n> a )\\k<n>" "a+ ?" "a+ +" "a \\Q b c\\E d" "[a-c #x]+" "\\x 41"
            "[^ ]" "# only a comment"]
 :flags [:x]
 :inputs ["" "abc" "a b" "a bc" "a#b" "aa" "a{ 2 }" "ab b cd" "a b cd" "ab cd" "x# ." "A" "aaa"]}

{:patterns ["(?x)a b" "(?-x)a b" "a b(?x) c d" "[a b](?i)"]
 :flag-sets [[] [:x] [:x :i]]
 :inputs ["ab" "a b" "A B" "a bcd" "a b c d"]}

;; LITERAL: the pattern is plain text; other flags still apply partly
{:patterns ["a.b" "a*" "\\d" "(a)" "[x]" "" "$1" "\\Q.\\E" "a b # c" "\u00e9" "^"]
 :flag-sets [[:literal] [:literal :i] [:literal :i :u] [:literal :x]]
 :inputs ["" "a.b A.B axb" "a*aa" "\\d1" "(a)[x]" "$1" "\\Q.\\E" "a b # c" "\u00c9" "^a"]
 :replace ["<$0>"]}
