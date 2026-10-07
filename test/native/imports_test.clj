(ns native.imports-test
  "RT's default imports (arbace/lang/RT.clj, DEFAULT_IMPORTS): `SecurityManager` was dropped
  (JEP 486, Java 24: the Security Manager is permanently disabled; doc/VENDOR-NOTES.md, hand
  change 8). The class itself is still there under its full name."
  (:require [arbace.test :refer :all]))

(deftest security-manager-not-imported
  (is (nil? (ns-resolve (create-ns 'native.imports-test.fresh) 'SecurityManager)))
  (is (= java.lang.SecurityException (ns-resolve (create-ns 'native.imports-test.fresh) 'SecurityException)))
  (is (= "java.lang.SecurityManager" (.getName (resolve 'java.lang.SecurityManager)))))
