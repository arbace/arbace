;; Regex corpus: Unicode properties, scripts, blocks and categories, surrogate pairs (a class
;; or . matches a whole code point, positions in UTF-16 units), Unicode case folding,
;; canonical equivalence. Non-ASCII characters are written as \uXXXX escapes.

{:patterns ["\\p{L}" "\\p{Lu}" "\\p{Ll}" "\\p{Lt}" "\\p{N}" "\\p{Nd}" "\\p{P}" "\\p{S}" "\\p{Sc}" "\\p{Z}" "\\p{Zs}"
            "\\p{C}" "\\p{Cc}" "\\p{Cf}" "\\p{Cs}" "\\p{Co}" "\\p{Cn}" "\\p{M}" "\\p{Mn}" "\\p{IsL}" "\\p{IsLu}"
            "\\p{gc=Nd}" "\\p{general_category=Lu}" "\\pL" "\\pN" "\\PL" "\\P{L}" "\\p{^L}" "[\\p{L}\\p{N}]+" "[^\\p{L}]+"
            "\\p{IsLatin}" "\\p{IsGreek}" "\\p{IsCyrillic}" "\\p{IsHan}" "\\p{script=Han}" "\\p{sc=Grek}" "\\p{IsCommon}"
            "\\p{InGreek}" "\\p{InBasic_Latin}" "\\p{InLatin-1 Supplement}" "\\p{block=CJKUnifiedIdeographs}"
            "\\p{blk=Emoticons}" "\\p{InEmoticons}" "\\p{IsEmoji}" "\\p{IsEmoji_Presentation}" "\\p{IsIdeographic}"
            "\\p{IsLetter}" "\\p{IsUppercase}" "\\p{IsLowercase}" "\\p{IsControl}" "\\p{IsHex_Digit}" "\\p{IsJoin_Control}"
            "\\p{IsNoncharacter_Code_Point}" "\\p{IsAssigned}" "\\p{IsTitlecase}"]
 :inputs ["aZ9 _" "\u00E9\u00C9\u00DF" "\u03B1\u03A9\u03C2\u0436\u0416" "\u4E2D\u3002\uD83D\uDE00" "\u0663\u00B2\u2167\u20AC$"
          "\u00A0\u2028\u3000" "e\u0301\u01C5" "\u200D\uFEFF\uD83D" "\uD800\uDC00\uDBFF\uDFFD\uD835\uDC00" "\uFFFE\uE000\u0378"]}

;; surrogate pairs: positions are UTF-16 units, classes and . take whole code points
{:patterns ["." ".." "\\uD83D" "\\uDE00" "[\\uD83D]" "[^a]" "[^a]+" "\\uD83D\\uDE00" "[\\uD800-\\uDBFF]" "[\\uDC00-\\uDFFF]"
            "\\x{1F600}+" "[\\x{1F600}-\\x{1F602}]{2}" "a.b" "\\P{L}" "(?<=\\uD83D)." "(?<=.)." "\\b" "\\B" "\\X" "^.{2}$"]
 :inputs ["" "\uD83D\uDE00" "\uD83D\uDE00\uD83D\uDE01" "a\uD83D\uDE00b" "\uD83D" "\uDE00" "\uDE00\uD83D" "a\uD83Db"
          "\uD83D\uDE02\uD83D\uDE01x" "ab"]
 :replace ["<$0>"]}

;; case folding: ASCII by default, Unicode with :u, and the special cases
{:patterns ["\u00E9" "caf\u00C9" "\u00DF" "ss" "\u03C3" "\u03A3" "\u03C2" "k" "\u212A" "i" "I" "\u0131" "\u0130"
            "\u017F" "\u1E9E" "\u01C5" "\u0436" "\uD801\uDC00" "[\u00E9]" "[\u03C3]" "[k-m]" "[\u0400-\u042F]" "\u00B5" "\u00FF"]
 :flag-sets [[] [:i] [:i :u]]
 :inputs ["\u00E9\u00C9" "CAF\u00C9 caf\u00E9" "\u00DF SS \u1E9E" "\u03C3\u03A3\u03C2" "kK\u212A" "iI\u0131\u0130" "\u017Fs"
          "\u01C4\u01C5\u01C6" "\u0436\u0416" "\uD801\uDC00\uD801\uDC28" "\u00B5\u039C\u03BC" "\u00FF\u0178lL"]}

{:patterns ["\u00E9" "\u03C3+" "\\w+" "k"]
 :flag-sets [[:u] [:i :U] [:U]]
 :inputs ["\u00C9" "\u03A3\u03C2\u03C3" "\u00E9\u00C9a" "\u212A"]}

;; canonical equivalence
{:patterns ["\u00E9" "e\u0301" "caf\u00E9" "[\u00E9]" "\u00E9+" "a\u030A" "\u00C5" "\u212B" "\u1E69" "s\u0323\u0307"
            "e\u0301?" "x\u00E9?y"]
 :flag-sets [[] [:canon-eq] [:canon-eq :i]]
 :inputs ["\u00E9" "e\u0301" "cafe\u0301" "E\u0301" "\u00E9e\u0301" "\u00C5" "A\u030A" "\u212B" "s\u0323\u0307" "s\u0307\u0323"
          "xe\u0301y"]}
