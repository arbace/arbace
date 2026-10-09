;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; The Go build's arbace/core_proxy.clj (doc/go/EVAL-NOTES.md, "Namespace variants"): it
;; replaces arbace/core_proxy.clj in the executable. proxy generates a class with ASM, which the
;; Go build has not (B1-PLAN.md D6: proxy comes later); the names exist and throw when used
;; (a proxy expression when it is evaluated, so that code using it loads).
;; method-sig is arbace/core_proxy.clj's.

(in-ns 'arbace.core)

;; arbace/core_proxy.clj's imports, less ASM's (the namespaces after it use them)
(import
 '(java.lang.reflect Modifier Constructor)
 '(java.io Serializable NotSerializableException)
 '(arbace.lang IProxy Reflector DynamicClassLoader IPersistentMap PersistentHashMap RT))

(defn method-sig [^java.lang.reflect.Method meth]
  [(. meth (getName)) (seq (. meth (getParameterTypes))) (. meth getReturnType)])

(defn- go-build-proxy [what]
  (throw (UnsupportedOperationException. (str what " is not available in the Go build"))))

(defn proxy-name {:added "1.0"} [super interfaces] (go-build-proxy "proxy-name"))
(defn get-proxy-class {:added "1.0"} [& bases] (go-build-proxy "get-proxy-class"))
(defn construct-proxy {:added "1.0"} [c & ctor-args] (go-build-proxy "construct-proxy"))
(defn init-proxy {:added "1.0"} [proxy mappings] (go-build-proxy "init-proxy"))
(defn update-proxy {:added "1.0"} [proxy mappings] (go-build-proxy "update-proxy"))
(defn proxy-mappings {:added "1.0"} [proxy] (go-build-proxy "proxy-mappings"))
(defmacro proxy
  "In the Go build, proxy makes no class (D6): the expression throws when evaluated."
  {:added "1.0"} [class-and-interfaces args & fs]
  `(throw (UnsupportedOperationException. "proxy is not available in the Go build")))
(defn proxy-call-with-super {:added "1.0"} [call this meth] (go-build-proxy "proxy-call-with-super"))
(defmacro proxy-super {:added "1.0"} [meth & args]
  `(throw (UnsupportedOperationException. "proxy-super is not available in the Go build")))
(defn bean {:added "1.0"} [x] (go-build-proxy "bean"))
