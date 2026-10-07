;; /**
;;  *   Copyright (c) Rich Hickey and Contributors. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; Converted from clojure/java/api/Clojure.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.java.api)

(import '(arbace.lang IFn RT Symbol Var))

(defclass ^:public Clojure
  (constructor ^:private [this])

  (method ^:private ^:static asSym ^Symbol [o]
    (let [^Symbol s (if (instance? String o) (Symbol/intern (cast String o)) (cast Symbol o))] s))

  (method ^:public ^:static var ^IFn [qualifiedName]
    (let [s (Clojure/asSym qualifiedName)] (Clojure/var (.getNamespace s) (.getName s))))

  (method ^:public ^:static var ^IFn [ns name]
    (Var/intern (Clojure/asSym ns) (Clojure/asSym name)))

  (method ^:public ^:static read [^String s] (.invoke EDN_READ_STRING s))

  (static-initializer
    (RT/init)
    (let [edn (cast Symbol (.invoke (Clojure/var "arbace.core" "symbol") "arbace.edn"))]
      (.invoke (Clojure/var "arbace.core" "require") edn)))

  (field ^:private ^:static ^:final ^IFn EDN_READ_STRING (Clojure/var "arbace.edn" "read-string")))
