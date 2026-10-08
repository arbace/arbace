;; jrt: the native methods of the translated JDK classes (C2G-SPEC §9.1): c2g translates a
;; ^:native method into a call of C_M..._native. Double's and Float's are numconv.clj's; the
;; fifth is here (added with c2g, doc/go/C2G-NOTES.md).
(in-ns 'go.arbace.jrt)

(go/file "natives.go")

(go/func NullPointerException_GetExtendedNPEMessage__String_native
  "NullPointerException_GetExtendedNPEMessage__String_native is the native
NullPointerException.getExtendedNPEMessage: null, so an implicit NullPointerException has no
message (V11, amendment J6).\n"
  ^{:tag (* String)} [^{:tag (* NullPointerException)} t]
  nil)
