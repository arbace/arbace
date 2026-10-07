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
;; Converted from clojure/main.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace)

(import '(arbace.lang RT Symbol Var))

(defclass ^:public main
  (field ^:private ^:static ^:final ^Symbol CLOJURE_MAIN (Symbol/intern "arbace.main"))

  (field ^:private ^:static ^:final ^Var REQUIRE (RT/var "arbace.core" "require"))

  (field ^:private ^:static ^:final ^Var LEGACY_REPL (RT/var "arbace.main" "legacy-repl"))

  (field ^:private ^:static ^:final ^Var LEGACY_SCRIPT (RT/var "arbace.main" "legacy-script"))

  (field ^:private ^:static ^:final ^Var MAIN (RT/var "arbace.main" "main"))

  (method ^:public ^:static legacy_repl ^void [^String/1 args]
    (RT/init)
    (.invoke REQUIRE CLOJURE_MAIN)
    (.invoke LEGACY_REPL (RT/seq args)))

  (method ^:public ^:static legacy_script ^void [^String/1 args]
    (RT/init)
    (.invoke REQUIRE CLOJURE_MAIN)
    (.invoke LEGACY_SCRIPT (RT/seq args)))

  (method ^:public ^:static main ^void [^String/1 args]
    (RT/init)
    (.invoke REQUIRE CLOJURE_MAIN)
    (.applyTo MAIN (RT/seq args))))
