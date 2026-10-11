;; Go-build variant of java.util.HexFormat (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"): read
;; by c2g only. HexFormat is translated from jdk26u (URLDecoder's and ParseUtil's hex digits);
;; parseHex(char[], int, int) wraps the chars in a java.nio.CharBuffer, outside the closed world:
;; the variant parses a String of the same chars, after the same bounds check. Copyright (c) the
;; Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant HexFormat
  (method ^:public parseHex ^byte/1 [this ^char/1 chars ^int fromIndex ^int toIndex]
    (Objects/requireNonNull chars "chars")
    (Objects/checkFromToIndex fromIndex toIndex (alength chars))
    (.parseHex this (String. chars fromIndex (unchecked-subtract-int toIndex fromIndex)))))
