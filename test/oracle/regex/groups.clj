;; Regex corpus: capturing, non-capturing, named, nested and unmatched groups, groups in
;; repetitions (the last iteration stays), backreferences, atomic groups.

{:patterns ["(a)" "(a)(b)?" "(a)|(b)" "((a)(b))" "(?:a)(b)" "(?<x>a)(?<y>b)?" "(?<first>\\w)(?<second>\\w)" "((((a))))"
            "(a(b(c)))" "(a)?(b)?(c)?" "(a|(b))+" "(?:(a)|(b)|(c))+" "(?<A1>a)(?<b2B>b)" "()" "(())" "(a)()(b)"]
 :inputs ["" "a" "b" "ab" "ba" "abc" "cba" "abab" "aabbcc" "AB"]
 :replace ["[$1]" "${x}" "$2$1"]}

;; backreferences
{:patterns ["(a)\\1" "(a+)\\1" "(\\w+) \\1" "(?<w>\\w)\\k<w>" "(a)?\\1" "(a)|\\1" "(a)\\1?" "(.)(.)\\2\\1" "\\1(a)"
            "(a)\\10" "(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\10" "(a)(b)(c)(d)(e)(f)(g)(h)(i)\\10" "(?:(a)|b)\\1" "(a*)\\1b"
            "(\\x{1F600})\\1" "(.)\\1"]
 :flag-sets [[] [:i]]
 :inputs ["" "aa" "aA" "aaaa" "the the" "The the" "abba" "aBBa" "a" "a0" "abcdefghijj" "abcdefghiaa0" "bb" "aaab"
          "\uD83D\uDE00\uD83D\uDE00" "\u00E9\u00C9"]}

{:patterns ["(\\w)\\1" "(?<c>.)\\k<c>" "(\u00DF)\\1" "(\u03C3)\\1"]
 :flag-sets [[:i :u] [:i :U] [:u]]
 :inputs ["\u00E9\u00C9" "\u00C9\u00E9" "\u00DF\u1E9E" "\u00DFSS" "\u03C3\u03A3" "\u03C3\u03C2" "kK" "k\u212A" "\u0436\u0416"]}

;; atomic groups
{:patterns ["(?>a+)b" "(?>a+)a" "(?>a|ab)c" "(?>ab|a)c" "a(?>bc|b)c" "(?>(a+))b" "(?>x*)(x)?" "(?>\\d+)\\." "(?>a*?)b"]
 :inputs ["" "aab" "aaa" "abc" "ac" "abcc" "xx" "123.4" "123" "b"]}

;; named groups: replacement and named map
{:pattern "(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})"
 :inputs ["2024-01-31" "x 1999-12-01 and 2000-02-29 y" "24-1-3" ""]
 :replace ["${day}/${month}/${year}" "$3.$2.$1" "${year}${nosuch}" "$0$0" "${1}" "$4"]}
