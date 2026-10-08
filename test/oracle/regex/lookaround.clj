;; Regex corpus: lookahead and lookbehind, positive and negative, bounded lookbehind,
;; alternations of different lengths inside lookbehind, groups captured in lookarounds.

{:patterns ["a(?=b)" "a(?!b)" "(?<=a)b" "(?<!a)b" "(?=a)" "(?!a)" "(?<=a)" "(?<!a)" "a(?=b)b" "(?=(a))a" "(?=(\\w+))\\w"
            "(?<=(a))b" "(?<=ab|b)c" "(?<=a|bc)d" "(?<=a{1,3})b" "(?<=a?)b" "(?<!ab|c)d" "(?<=\\d{2})\\w" "(?<=^|,)\\w+"
            "\\w+(?=,|$)" "(?<=x(?=y))y" "(?=a(?<=a))a" "(?<=a(?!b)).+" "(?!)" "(?=)" "(?<=[a-c]{2})." "(?<=\\b)a"
            "(?<=\\uD83D\\uDE00)." "(?<=.)\\uDE00" "(?<=\\p{L}{2})x" "(?<=a.)c" "(?<=(?:ab){1,2})c" "(?<!^)" "(?<=a)(?=b)"
            "q(?!u)" "(?=\\d{3}$)" "\\d+(?=%)" "(?<![\\d.])\\d+(?![\\d.])"]
 :inputs ["" "ab" "ba" "aab" "bab" "abc" "bcd" "ad" "abd" "aaab" "12a,34b" "x,yy,zzz" "xy" "abab" "\uD83D\uDE00a"
          "\uD83D\uDE00\uD83D\uDE01" "\u00E9\u00E9x" "acc" "ababc" "quit qat" "123456" "50% 1.5 7"]
 :replace ["<$0>"]}

;; lookbehind with unbounded or hard-to-bound length
{:patterns ["(?<=a*)b" "(?<=a+)b" "(?<=a{1,})b" "(?<=.*)b" "(?<=(a|bc)+)d" "(?<=\\w+)x" "(?<=a{2}|b*)c"]
 :inputs ["aab" "b" "bcd"]}

;; lookarounds with flags
{:patterns ["(?<=A)b" "a(?=B)" "(?<=\u00C9)x" "(?i)(?<=a)b" "(?<=(?i)a)b"]
 :flag-sets [[] [:i] [:i :u]]
 :inputs ["ab" "AB" "Ab" "\u00E9x" "\u00C9x" "aB"]}
