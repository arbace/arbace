;; Regex corpus: character classes (ranges, negation, union, intersection, escapes inside
;; classes), predefined classes and POSIX classes, with and without UNICODE_CHARACTER_CLASS.
;; Non-ASCII characters are written as \uXXXX escapes.

;; simple classes, ranges, negation
{:patterns ["[abc]" "[^abc]" "[a-c]" "[a-cx-z]" "[a\\-z]" "[-a]" "[a-]" "[]a]" "[^]a]" "[\\]]" "[\\[]"
            "[\\^a]" "[a^]" "[.]" "[*+?$]" "[\\\\]" "[a-a]" "[\\x41-\\x43]" "[\\u00e0-\\u00ff]" "[\\t\\n]"
            "[\\Q]-[\\E]" "[ab]+" "[^\\n]+" "[\\x{1F600}-\\x{1F64F}]" "[^\\x{1F600}]" "[\\uD83D\\uDE00]" "[a&b]" "[a&&]"]
 :inputs ["" "abc" "zA-" "]" "[^." "\\" "x-y]" "\t\n" "\u00e9\u00c9" "\uD83D\uDE00" "\uD83D\uDE4F\uD83D\uDE50" "\uD83D&"]}

{:patterns ["[a-c]" "[^a-c]" "[A-Z]" "[\\u00e0-\\u00ff]" "[k]" "[s]" "[^k]"]
 :flag-sets [[:i] [:i :u]]
 :inputs ["a" "B" "d" "\u00e9" "\u00c9" "\u212A" "K" "k" "\u017F" "S" "\u0178"]}

;; union and intersection
{:patterns ["[a[b]]" "[a[^b]]" "[[a-c][x-z]]" "[a-z&&[^aeiou]]" "[a-z&&[def]]" "[a-z&&d-f]" "[^a-z&&[aeiou]]"
            "[a-z&&[^b-y]&&[^a]]" "[\\w&&[^\\d]]" "[\\p{L}&&[^\\p{Lu}]]" "[[a]&&[b]]" "[a-z&&]" "[&&a]" "[a&&&&b]"
            "[[^a]b]" "[^[a]b]" "[^a[b]]" "[ab&&b]" "[a-f&&b-z&&c]" "[\\d&&[^5]]"]
 :inputs ["" "a" "b" "c" "e" "z" "A" "57" "_" "\u00e9\u00c9" "-&"]}

;; predefined classes
{:patterns ["\\d" "\\D" "\\w" "\\W" "\\s" "\\S" "\\h" "\\H" "\\v" "\\V" "\\R" "\\X" "\\w+" "\\R+"
            "[\\d\\s]" "[^\\d\\s]" "[\\W\\d]" "\\b\\w+\\b"]
 :flag-sets [[] [:U]]
 :inputs ["a1_ \u0663\uFF11" "\u00e9\u00df\u03A3" "\u4E2D\uD83D\uDE00" "\t\n\u000B\f\r" "\u0085\u00A0\u1680\u2000\u2028\u3000"
          "\r\n\n\r" "e\u0301x" "\u1100\u1161\u11A8" "\uD83C\uDDEB\uD83C\uDDF7" "\uD83D\uDC69\u200D\uD83D\uDC67\u0000\u001C"]}

;; POSIX classes (US-ASCII only unless :U)
{:patterns ["\\p{Lower}" "\\p{Upper}" "\\p{ASCII}" "\\p{Alpha}" "\\p{Digit}" "\\p{Alnum}" "\\p{Punct}" "\\p{Graph}"
            "\\p{Print}" "\\p{Blank}" "\\p{Cntrl}" "\\p{XDigit}" "\\p{Space}" "\\P{Alpha}" "[\\p{Punct}&&[^.]]"
            "\\p{IsAlphabetic}" "\\p{IsPunctuation}" "\\p{IsWhite_Space}" "\\p{IsDigit}"]
 :flag-sets [[] [:U] [:i]]
 :inputs ["aZ5!" "\u00e9\u00c9\u0663" "\t \u00A0" "\u0000\u007F\u00A7" "fF9g~`{|}." "\u03C3\u03A3\uD835\uDC00"]}

;; java.lang.Character classes
{:patterns ["\\p{javaLowerCase}" "\\p{javaUpperCase}" "\\p{javaWhitespace}" "\\p{javaMirrored}" "\\p{javaLetter}"
            "\\p{javaDigit}" "\\p{javaLetterOrDigit}" "\\p{javaAlphabetic}" "\\p{javaIdeographic}" "\\p{javaTitleCase}"
            "\\p{javaSpaceChar}" "\\p{javaISOControl}" "\\p{javaIdentifierIgnorable}" "\\p{javaJavaIdentifierStart}"
            "\\p{javaJavaIdentifierPart}" "\\p{javaUnicodeIdentifierStart}" "\\p{javaDefined}" "\\P{javaLowerCase}"]
 :inputs ["aZ5_$" "\u00e9\u00c9\u01C5" "( ) <" "\t\u00A0\u2007\u001C" "\u4E2D\u0663" "\u0000\u00AD\u0378" "\uD83D\uDE00"]}
