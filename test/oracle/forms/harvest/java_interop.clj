;; Harvested by bin/oracle harvest (test/oracle/harvest.clj) from Clojure's test suite,
;; clojure/clojure 98d735fab02f337cee654cb0629bddc09883a75a test/clojure/test_clojure/java_interop.clj,
;; renamed to arbace.* by bin/clojure-tests: the expressions of its assertions that are
;; self-contained and deterministic. Eclipse Public License 1.0 (LICENSE.md). Do not edit.
(require (quote [arbace.data :as data]))
(require (quote [arbace.pprint :as pp]))
(require (quote [arbace.set :as set]))
(require (quote [arbace.string :as str]))
(import (quote (java.io File FileFilter FilenameFilter)))
(import (quote (java.util UUID)))
(import (quote (java.util.concurrent.atomic AtomicLong AtomicInteger)))
(-> (get-proxy-class Object) construct-proxy (init-proxy {}) (update-proxy {"toString" (fn [_] "chain chain chain")}) str)
(-> (proxy [Object] [] (toString [] "superfuzz bigmuff")) (update-proxy {"toString" (fn [_] "chain chain chain")}) str)
(-> (java.io.ByteArrayOutputStream.) (java.io.ObjectOutputStream.) (.writeObject (proxy [Object java.io.Serializable] [])))
