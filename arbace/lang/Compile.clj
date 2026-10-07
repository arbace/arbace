;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; Converted from clojure/lang/Compile.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException OutputStreamWriter))

(defclass ^:public Compile
  (field ^:private ^:static ^:final ^String PATH_PROP "arbace.compile.path")

  (field ^:private ^:static ^:final ^String REFLECTION_WARNING_PROP
    "arbace.compile.warn-on-reflection")

  (field ^:private ^:static ^:final ^String UNCHECKED_MATH_PROP "arbace.compile.unchecked-math")

  (field ^:private ^:static ^:final ^Var compile_path (RT/var "arbace.core" "*compile-path*"))

  (field ^:private ^:static ^:final ^Var compile (RT/var "arbace.core" "compile"))

  (field ^:private ^:static ^:final ^Var warn_on_reflection
    (RT/var "arbace.core" "*warn-on-reflection*"))

  (field ^:private ^:static ^:final ^Var unchecked_math (RT/var "arbace.core" "*unchecked-math*"))

  (method ^:public ^:static main :throws [IOException ClassNotFoundException] ^void [^String/1 args]
    (RT/init)
    (let [out (cast OutputStreamWriter (.deref RT/OUT))
          err (RT/errPrintWriter)
          path (System/getProperty PATH_PROP)
          count (alength args)]
      (when (nil? path)
        (.println
          err
          (java-str
            "ERROR: Must set system property "
            PATH_PROP
            "\nto the location for compiled .class files.\nThis directory must also be on your CLASSPATH."))
        (System/exit 1))
      (let [warnOnReflection (.equals (System/getProperty REFLECTION_WARNING_PROP "false") "true")
            uncheckedMathProp (System/getProperty UNCHECKED_MATH_PROP)
            ^:mutable ^Object uncheckedMath Boolean/FALSE]
        (cond
          (.equals "true" uncheckedMathProp) (set! uncheckedMath Boolean/TRUE)
          (.equals "warn-on-boxed" uncheckedMathProp)
            (set! uncheckedMath (Keyword/intern "warn-on-boxed")))
        ;; force load to avoid transitive compilation during lazy load
        (RT/load "arbace/core/specs/alpha")
        (try
          (Var/pushThreadBindings
            (^[Object/1] RT/map compile_path
                                path
                                warn_on_reflection
                                warnOnReflection
                                unchecked_math
                                uncheckedMath))
          (for-each [^String lib args]
            (.write out (java-str "Compiling " lib " to " path "\n"))
            (.flush out)
            (.invoke compile (Symbol/intern lib)))
          (finally
            (Var/popThreadBindings)
            (try (.flush out) (catch IOException e (.printStackTrace e err)))))))))
