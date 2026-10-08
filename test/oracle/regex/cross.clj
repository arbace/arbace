;; Regex corpus: a cross product of common patterns over varied inputs (ASCII, Latin-1, Greek,
;; Cyrillic, CJK, emoji, line terminators) and a few flag sets.

{:patterns ["\\w+" "\\d+" "\\s+" "[A-Za-z]+" "\\p{L}+" "\\p{Lu}\\p{Ll}*" "[^\\s,]+" "\\b\\w" "^\\s+|\\s+$" ".+?" "(\\w)(\\w*)"
            "(?<k>\\w+)=(?<v>[^;]*)" "\\S+@\\S+\\.\\w+" "(\\d+)\\.(\\d+)" "[aeiou\u00E9]" "\u00E9|\u03C3|\u0436" "(.)\\1"
            "^.*$" "\\R" "[.,;!?]" "x*" "\\B" "(?i)hello" "[\\u4E00-\\u9FFF]+" "[^\\x00-\\x7F]" "\\X" "(?=\\w)"
            "(\\w+)\\s(\\w+)" "\\$\\d+(?:\\.\\d\\d)?" "#[0-9a-fA-F]{6}\\b"]
 :flag-sets [[] [:i :u :U]]
 :inputs ["" "Hello, World!" "hello HELLO HeLLo" "  padded  " "a=1;b=two;c=" "user@example.com, x@y.z" "3.14 and 2.718"
          "caf\u00E9 CAF\u00C9 na\u00EFve" "\u03A3\u03AF\u03C3\u03C5\u03C6\u03BF\u03C2" "\u041F\u0440\u0438\u0432\u0435\u0442"
          "\u4E2D\u6587\u5B57 \u65E5\u672C" "\uD83D\uDE00 smile \uD83D\uDC4D\uD83C\uDFFD" "line1\nline2\r\nline3\rline4"
          "aabbcc" "$5 $12.50 $3.5" "#ff00AA #12345 #abcdefg" "e\u0301\u00E9 \u00DF\u1E9E" "\u0663\u0664\u0665 123"]
 :replace ["<$0>"]}
