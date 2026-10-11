;; c2g's fixtures (see FxCode.clj): RT's runtime services that need no arbace.core (C2G-NOTES.md,
;; phase 2A): keyword interning across garbage collections (jrt's weak references), reflective
;; interop through Reflector (the Go build's variant, over jrt's member tables), the standard
;; streams' vars.

(in-ns 'c2g.fixtures)

(import '(arbace.lang Keyword LineNumberingPushbackReader Reflector RT Symbol Util Var))

(do

(defclass ^:public FxRuntime
  (method ^:static churn ^void [^String prefix]
    (loop [^int i 0]
      (when (< i 3000)
        (Keyword/intern (java-str prefix i))
        (recur (unchecked-inc-int i)))))

  ;; Keyword's table holds weak references: a keyword held stays the one interned, one dropped
  ;; may be collected and is interned anew, equal by name
  (method ^:public ^:static tKeywordWeak ^String []
    (let [held (Keyword/intern "fx-held")
          hash (.hashCode held)]
      (FxRuntime/churn "fx-drop-")
      (System/gc)
      (System/gc)
      (FxRuntime/churn "fx-drop2-")
      (let [again (Keyword/intern "fx-held")
            found (Keyword/find (Symbol/intern "fx-held"))
            d1 (Keyword/intern "fx-drop-7")
            d2 (Keyword/intern "fx-drop-7")]
        (java-str (identical? held again) " " (identical? held found) " " (== hash (.hashCode again))
                  " " (identical? d1 d2) " " d1 " " (Util/equiv d1 (Keyword/intern nil "fx-drop-7"))
                  " " (nil? (Keyword/find (Symbol/intern "fx-never-interned")))))))

  (method ^:static msg ^String [^Throwable t]
    (java-str (.getName (.getClass t)) ": " (.getMessage t)))

  ;; static members by name, overloads chosen at run time
  (method ^:public ^:static tReflectStatic ^String []
    (java-str (Reflector/invokeStaticMethod "arbace.lang.Numbers" "add" (new Object/1 [1 2]))
              " " (Reflector/invokeStaticMethod "arbace.lang.Numbers" "add" (new Object/1 [1 2.5]))
              " " (Reflector/invokeStaticMethod "arbace.lang.Util" "equiv" (new Object/1 [1 1]))
              " " (Reflector/invokeStaticMethod "java.lang.Integer" "parseInt" (new Object/1 ["42"]))
              " " (Reflector/invokeStaticMethod "java.lang.Math" "abs" (new Object/1 [-7]))
              " " (Reflector/getStaticField "arbace.lang.PersistentVector" "EMPTY")
              " " (Reflector/getStaticField "java.lang.Integer" "MAX_VALUE")
              " " (try (Reflector/invokeStaticMethod "arbace.lang.Numbers" "nope" (new Object/1 []))
                       (catch Exception e (FxRuntime/msg e)))
              " " (try (Reflector/getStaticField "arbace.lang.Numbers" "NOPE")
                       (catch Exception e (FxRuntime/msg e)))))

  ;; instance members by name: arity, argument conversion (a Long for an int), boolean results,
  ;; public fields, constructors
  (method ^:public ^:static tReflectInstance ^String []
    (let [v (RT/vector (new Object/1 [1 2 3]))
          sb (StringBuilder. "abc")
          var (RT/var "c2g.fixtures.rt" "x" 5)]
      (java-str (Reflector/invokeInstanceMethod v "count" (new Object/1 []))
                " " (Reflector/invokeInstanceMethod v "nth" (new Object/1 [1]))
                " " (Reflector/invokeInstanceMethod v "nth" (new Object/1 [7 "dflt"]))
                " " (Reflector/invokeNoArgInstanceMember sb "length")
                " " (Reflector/invokeInstanceMethod sb "append" (new Object/1 ["!"]))
                " " (Reflector/invokeInstanceMethod "abc" "toUpperCase" (new Object/1 []))
                " " (Reflector/invokeInstanceMethod "abc" "isEmpty" (new Object/1 []))
                " " (Reflector/invokeInstanceMethod "abc" "charAt" (new Object/1 [1]))
                " " (Reflector/invokeInstanceMethod "abc" "indexOf" (new Object/1 ["c"]))
                " " (Reflector/getInstanceField var "sym")
                " " (Reflector/invokeInstanceMember var "sym")
                " " (Reflector/invokeConstructor StringBuilder (new Object/1 ["xy"]))
                " " (Reflector/invokeConstructor arbace.lang.MapEntry (new Object/1 [1 2]))
                " " (try (Reflector/invokeInstanceMethod "abc" "nope" (new Object/1 []))
                         (catch Exception e (FxRuntime/msg e)))
                " " (try (Reflector/invokeInstanceMethod v "nth" (new Object/1 [9]))
                         (catch Exception e (.getName (.getClass e))))
                " " (try (Reflector/invokeInstanceMethod "abc" "charAt" (new Object/1 ["x"]))
                         (catch Exception e (FxRuntime/msg e))))))

  ;; *out*, *err*, *in*: writers and a reader before arbace.core loads
  (method ^:public ^:static tStreams ^String []
    (java-str (instance? java.io.Writer (.deref RT/OUT))
              " " (instance? java.io.PrintWriter (.deref RT/ERR))
              " " (instance? LineNumberingPushbackReader (.deref RT/IN))
              " " (instance? java.io.PrintWriter (RT/errPrintWriter))
              " " (.isDynamic RT/OUT) " " (.isDynamic RT/ERR) " " (.isDynamic RT/IN))))

)
