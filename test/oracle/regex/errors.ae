;; Regex corpus: invalid patterns, recorded with the PatternSyntaxException's description, index
;; and message; plus a few near-misses that do compile.

{:patterns ["(" "(a" "((a)" "(?:a" "(?<n>a" ")" "a)" "(a))" "[" "[a" "[a-" "[^" "[]" "[^]" "[a-z" "[[a]" "[a&&" "[\\" "\\"
            "a\\" "a{" "a{1" "a{1," "a{1,2" "{" "{1}" "a{x}" "a{2,1}" "a{99999999999}" "a{1}{2}" "*" "+" "?" "*a" "a**" "a+*"
            "a???" "a?*" "a{1}*" "(*)" "(?" "(?)" "(?<" "(?<>a)" "(?<1a>a)" "(?<a-b>a)" "(?<a_b>a)" "(?<a>a)(?<a>b)" "\\k" "\\ka"
            "\\k<" "\\k<a" "\\k<a>" "(?<a>x)\\k<b>" "\\k<1>" "(?<\u00E9>a)" "(?z)" "(?i" "(?i-)" "(?-)" "(?i-i)" "(?--i)"
            "(?ii)" "(?#comment)" "(?=" "(?<=" "(?<!" "(?<x" "(?P<n>a)" "(?'n'a)" "(?|a)" "(?R)" "\\p" "\\p{" "\\p{L"
            "\\p{Foo}" "\\p{IsFoo}" "\\p{InFoo}" "\\p{gc=Foo}" "\\p{script=Foo}" "\\p{foo=Lu}" "\\pX" "\\p{}" "\\P{IsLatin"
            "[z-a]" "[b-a]" "[a-\\d]" "[\\d-a]" "[a-\\w]" "[%--]" "[\\x{1F601}-\\x{1F600}]" "\\x" "\\xG" "\\x4" "\\x{" "\\x{}"
            "\\x{110000}" "\\x{FFFFFFFFF}" "\\x{zz}" "\\u" "\\u12" "\\u12G4" "\\0" "\\08" "\\c" "\\N" "\\N{" "\\N{NOT A NAME}"
            "\\N{}" "\\g" "\\i" "\\j" "\\l" "\\m" "\\o" "\\q" "\\y" "\\C" "\\E" "\\F" "\\I" "\\J" "\\K" "\\L" "\\M" "\\O" "\\T"
            "\\U" "\\Y" "\\b{" "\\b{x}" "\\b{g" "\\1" "(a)\\2" "\\Q" "\\Qabc" "a\\E" "(?<=a+b*c?)d" "(?<=(a+))b" "(?<=\\1)a"
            "(?<=a|b+)c" "(?<!x*)y" "a{1,2}{3}" "(?<n>" "[[:alpha:]]" "[a-]-z]" "a||" "|" "()" "(?:)" "(?=)" "a{0}" "a{0,}"]
 :inputs ["a"]}

;; error positions depend on flags too
{:patterns ["a {" "[ a" "( a" "# (" "(?x) [" "\\p{ Lu}" "a b*+ ?"]
 :flag-sets [[] [:x]]
 :inputs ["a"]}
