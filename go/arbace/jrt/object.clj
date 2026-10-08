;; jrt: the object model (C2G-SPEC §5.2, §5.6, §5.8; doc/go/JRT-NOTES.md, "The object model").
(in-ns 'go.arbace.jrt)

(go/file "object.go"
  :doc "Package jrt is the Java runtime of Arbace on Go: the hand-written edge of the JDK (the
object model, strings, Math, exceptions, ...) under the classes c2g translates from jdk26u
(doc/go/C2G-SPEC.md, doc/go/JRT-NOTES.md).\n"
  :imports [[fmt "fmt"] [atomic "sync/atomic"]])

;; ---------------------------------------------------------------------------------------
;; The header

(go/type Object
  "Object is the header every Java object embeds first (C2G-SPEC §5.2): one 64-bit word,
accessed atomically, holding the identity hash in its low 32 bits (0 until first asked) and
the lock word in its high 32 bits (monitor.go). It also makes every object non-empty, so that
distinct objects have distinct addresses.\n"
  (struct ^uint64 hdr))

(go/type Object_I
  "Object_I is java.lang.Object as an interface: every Java object implements it. Object's
struct implements Self_Object, HashCode__I and Equals_O__Z (promoted into every class); c2g
generates GetClass__Class, ToString__String and Clone__O for each concrete class.\n"
  (interface
    (Self_Object ^{:tag (* Object)} [])
    (GetClass__Class ^{:tag (* Class)} [])
    (HashCode__I ^int32 [])
    (Equals_O__Z ^bool [^any o])
    (ToString__String ^{:tag (* String)} [])
    (Clone__O ^any [])))

(go/method Self_Object "Self_Object returns the header, the object's identity.\n"
  ^{:tag (* Object)} [^{:tag (* Object)} o]
  o)

(go/method HashCode__I "HashCode__I is Object.hashCode: the identity hash.\n"
  ^int32 [^{:tag (* Object)} o]
  (.identityHash o))

(go/method Equals_O__Z "Equals_O__Z is Object.equals: identity, comparing headers.\n"
  ^bool [^{:tag (* Object)} o ^any x]
  (let [(values xo ok) (assert Object_I x)]
    (and ok (== (.Self_Object xo) o))))

(go/method ClearHeader
  "ClearHeader resets the header of a fresh shallow copy made by clone (no identity hash, not
locked), as c2g's Clone__O does after copying the struct.\n"
  [^{:tag (* Object)} o]
  (atomic/StoreUint64 (addr (.-hdr o)) 0))

;; ---------------------------------------------------------------------------------------
;; The identity hash (§5.8): assigned on first use, never the address; 31 bits, not 0, as
;; HotSpot's. A global sequence mixed by a 32-bit finalizer stands in for HotSpot's per-thread
;; xorshift (deviation V3: the values differ from the JVM's anyway).

(go/var ^{:tag atomic/Uint32} hashSeq)

(go/func nextIdentityHash ^uint32 []
  (while true
    (let [x (.Add hashSeq 0x9e3779b9)]
      (set! x (* (bit-xor x (>> x 16)) 0x85ebca6b))
      (set! x (* (bit-xor x (>> x 13)) 0xc2b2ae35))
      (set! x (bit-and (bit-xor x (>> x 16)) 0x7fffffff))
      (when (!= x 0)
        (return x)))))

(go/method identityHash ^int32 [^{:tag (* Object)} o]
  (while true
    (let [w (atomic/LoadUint64 (addr (.-hdr o)))]
      (when (!= (bit-and w 0xffffffff) 0)
        (return (conv int32 (conv uint32 w))))
      (let [h (nextIdentityHash)]
        (when (atomic/CompareAndSwapUint64 (addr (.-hdr o)) w (bit-or w (conv uint64 h)))
          (return (conv int32 h)))))))

(go/func IdentityHash
  "IdentityHash is System.identityHashCode: 0 for null, else the object's identity hash.\n"
  ^int32 [^any x]
  (when (== x nil)
    (return 0))
  (.identityHash (.Self_Object (asObject x))))

;; ---------------------------------------------------------------------------------------
;; Object's methods on values whose Go type is any (§5.8)

(go/func asObject ^Object_I [^any x]
  (when (== x nil)
    (panic (NPE)))
  (let [(values o ok) (assert Object_I x)]
    (when (not ok)
      (panic (fmt/Sprintf "jrt: not a Java object: %T" x)))
    o))

(go/func Equals "Equals is x.equals(y) on an Object: NullPointerException when x is null.\n"
  ^bool [^any x ^any y]
  (.Equals_O__Z (asObject x) y))

(go/func HashCode "HashCode is x.hashCode() on an Object.\n"
  ^int32 [^any x]
  (.HashCode__I (asObject x)))

(go/func ToString "ToString is x.toString() on an Object.\n"
  ^{:tag (* String)} [^any x]
  (.ToString__String (asObject x)))

(go/func GetClass "GetClass is x.getClass() on an Object.\n"
  ^{:tag (* Class)} [^any x]
  (.GetClass__Class (asObject x)))

(go/func Object_toString
  "Object_toString is Object.toString's body: the class name, @, the hash code in hex (the
dynamic hashCode, as Java's).\n"
  ^{:tag (* String)} [^Object_I x]
  (Str (+ (.GoName (.GetClass__Class x)) "@" (hexString (conv uint64 (conv uint32 (.HashCode__I x)))))))

(go/func hexString ^string [^uint64 v]
  (when (== v 0)
    (return "0"))
  (let [^{:tag (array 16 byte)} buf (zero (array 16 byte))
        i 16]
    (while (> v 0)
      (dec! i)
      (aset buf i (aget "0123456789abcdef" (bit-and v 15)))
      (set! v (>> v 4)))
    (conv string (subslice buf i))))

;; ---------------------------------------------------------------------------------------
;; new Object(): a plain instance of java.lang.Object (lock objects, sentinels)

(go/type objectInstance (struct Object))

(go/func Object_New "Object_New is new Object().\n"
  ^any []
  (addr (lit objectInstance)))

(go/method Ref ^any [^{:tag (* objectInstance)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* objectInstance)} t] Object_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* objectInstance)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* objectInstance)} t] (panic (CloneNotSupported t)))

(go/func CloneNotSupported
  "CloneNotSupported is the exception of Object.clone on an object whose class does not
implement Cloneable: CloneNotSupportedException with the class name, as the JVM's.\n"
  ^Throwable_I [^Object_I x]
  (CloneNotSupportedException_New_String (Str (.GoName (.GetClass__Class x)))))

;; ---------------------------------------------------------------------------------------
;; The receiver check (§5.6)

(go/func NN
  "NN is the null check of a call's receiver whose Go type is a pointer: it throws
NullPointerException on nil and returns p.\n"
  :type-params [T] ^{:tag (* T)} [^{:tag (* T)} p]
  (when (== p nil)
    (panic (NPE)))
  p)

(go/func NPE "NPE is a new NullPointerException without message, as an implicit null check
throws it (JRT-NOTES.md: helpful messages are not reproduced).\n"
  ^Throwable_I []
  (NullPointerException_New))
