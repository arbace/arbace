;; Regex corpus: split (leading and trailing empties, zero-width matches, limit -1) and
;; replacement strings ($0 $n ${name} \$ \\, invalid references, a trailing backslash).

{:patterns ["," ",+" ",*" "" "a*" "\\s+" "\\b" "(?=,)" "(?<=,)" "(?=a)" "x" "^" "$" "(?m)^" "(,)" "\\d" "(?=\\d)"
            "\\R" "(?!^)"]
 :inputs ["" "," ",," "a,b" ",a,b" "a,b," "a,,b,," ",,a" "baaa" "aaa" "a b  c " "  a" "1a2b3" "a\r\nb\nc\r" "x"
          "\uD83D\uDE00,\uD83D\uDE00" "\uD83D\uDE00\uD83D\uDE01"]}

;; zero-width matches and empty matches after a non-empty one
{:patterns ["a*" "a*?" "b*" "(?:)" "\\B" "a|" "|a" "x*$"]
 :inputs ["" "a" "baaa" "aab" "bab" "\uD83D\uDE00a"]
 :replace ["-" "<$0>"]}

;; replacement strings
{:pattern "(a)(b)?(?<n>c)?"
 :inputs ["" "xabcx" "aac" "ab" "zzz"]
 :replace ["$0" "$1" "$2" "$3" "${n}" "[$1$2$3]" "$12" "$32" "$4" "$9" "${nosuch}" "${1}" "${n" "${}" "$" "$a" "\\$1" "\\\\$1"
           "\\" "a\\" "\\n" "\\x" "$$" "$-1" "${0n}" "${N}" "x$1y" "$01" "$00" "$1$1$1"]}

{:pattern "x"
 :inputs ["axbxc" "" "x"]
 :replace ["" "$0$0" "\\" "$1" "${g}" "\\\\" "\\$" "\\$\\$" "y\\$z" "\uD83D\uDE00"]}

{:pattern "(?<a1>\\w)(?<b>\\d)"
 :inputs ["a1b2c" "aa"]
 :replace ["${a1}" "${b}${a1}" "${a}" "${a1}0" "$11" "$20" "${A1}"]}

;; many groups: $10 vs $1 followed by 0
{:pattern "(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)"
 :inputs ["abcdefghijk" "-abcdefghijk-"]
 :replace ["$10" "$11" "$12" "$110" "$100" "${10}${11}" "$1-0"]}
