;; jrt: the natives of arbace.lang.Compiler$Image (the Go build's Compiler variant; B1a step 6,
;; doc/go/EXEC-NOTES.md): the image of prepared namespaces. The image's encoding is the
;; program's (its main package's image.go, which knows every type of the program); jrt only
;; forwards to it, through the hooks the main package sets.
(in-ns 'go.arbace.jrt)

(go/file "image.go" :imports [[atomic "sync/atomic"] [utf16 "unicode/utf16"]])

(go/type ImageHooks
  "ImageHooks is the program's image of prepared namespaces (Compiler$Image's natives): set by
the main package when it has one; without it nothing is recorded or replayed.\n"
  (interface
    (Mode ^int32 [])
    (Begin ^bool [^string name])
    (Unit [^string name ^{:tag (* RefArray)} u])
    (End [^string name ^bool ok])
    (Has ^bool [^string name])
    (Next ^{:tag (* RefArray)} [^string name])
    (Resolve [^string name ^int32 k ^bool strict])
    (Started [])))

(go/var ^{:tag ImageHooks :doc "Image is the program's image hooks, or nil.\n"} Image)

(go/func Compiler_Image_Mode__I_native ^int32 []
  (when (== Image nil)
    (return 0))
  (.Mode Image))

(go/func Compiler_Image_Begin_String__Z_native ^bool [^{:tag (* String)} name]
  (when (== Image nil)
    (return false))
  (.Begin Image (.String (NN name))))

(go/func Compiler_Image_Unit_String_O1__V_native [^{:tag (* String)} name ^{:tag (* RefArray)} u]
  (when (!= Image nil)
    (.Unit Image (.String (NN name)) u)))

(go/func Compiler_Image_End_String_Z__V_native [^{:tag (* String)} name ^bool ok]
  (when (!= Image nil)
    (.End Image (.String (NN name)) ok)))

(go/func Compiler_Image_Has_String__Z_native ^bool [^{:tag (* String)} name]
  (when (== Image nil)
    (return false))
  (.Has Image (.String (NN name))))

(go/func Compiler_Image_Next_String__O1_native ^{:tag (* RefArray)} [^{:tag (* String)} name]
  (when (== Image nil)
    (return nil))
  (.Next Image (.String (NN name))))

(go/func Compiler_Image_Resolve_String_I_Z__V_native [^{:tag (* String)} name ^int32 k ^bool strict]
  (when (!= Image nil)
    (.Resolve Image (.String (NN name)) k strict)))

(go/func Compiler_Image_Started__V_native []
  (when (!= Image nil)
    (.Started Image)))

;; ---------------------------------------------------------------------------------------
;; The header, for the image's encoding: an object's identity hash as it is (0 when none was
;; asked for), and set on a decoded object

(go/func PeekIdentityHash
  "PeekIdentityHash is the identity hash x's header holds, 0 when none was assigned yet.\n"
  ^int32 [^any x]
  (let [(values o ok) (assert Object_I x)]
    (when (not ok)
      (return 0))
    (conv int32 (conv uint32 (atomic/LoadUint64 (addr (.-hdr (.Self_Object o))))))))

(go/func SetIdentityHash
  "SetIdentityHash gives the fresh object x the identity hash h (decoding: the hash it had
when it was encoded, so that hashed collections of it keep their layout).\n"
  [^any x ^int32 h]
  (let [(values o ok) (assert Object_I x)]
    (when ok
      (atomic/StoreUint64 (addr (.-hdr (.Self_Object o))) (conv uint64 (conv uint32 h))))))

(go/func presetClassHash
  "presetClassHash gives a new Class the identity hash of its name (String.hashCode's, 31
bits, never 0): classes hash alike in every run of the program, so that the hashed collections
of the image of prepared namespaces keep their layout where classes are keys (EXEC-NOTES.md).\n"
  [^{:tag (* Class)} c ^string name]
  (let [^uint32 h 0]
    (range [_ u (utf16/Encode (conv (slice rune) name))]
      (set! h (+ (* 31 h) (conv uint32 u))))
    (set! h (bit-and h 0x7fffffff))
    (when (== h 0)
      (set! h 1))
    (atomic/StoreUint64 (addr (.-hdr (.-Object c))) (conv uint64 h))))
