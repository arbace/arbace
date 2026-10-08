;; jrt, the Java runtime of Arbace on Go (doc/go/JRT-NOTES.md): the package arbace/jrt, its
;; hand-written Go forms (C2G-SPEC §4.3, §16 Q19). bin/jrt builds and tests it.
(ns go.arbace.jrt
  (:require [arbace.go :as go]))

(go/package jrt
  :path "arbace/jrt"
  :files ["array.go" "charset.go" "class.go" "classloader.go" "codec.go" "date.go" "enum.go" "locale.go" "math.go" "monitor.go" "numconv.go"
          "object.go" "reflect.go" "reflect_array.go" "reflect_tables.go" "standin_character.go" "standin_lang.go" "standin_reflect.go" "string.go" "stringbuilder.go"
          "threadid.go" "throwable.go" "volatile.go"]
  :test-files ["bench_test.go" "enum_test.go" "manifest_test.go" "math_test.go" "monitor_test.go"
               "object_test.go" "reflect_test.go" "shims_test.go" "string_test.go" "testutil_test.go" "throwable_test.go"])

(load "jrt/array")
(load "jrt/charset")
(load "jrt/class")
(load "jrt/classloader")
(load "jrt/codec")
(load "jrt/date")
(load "jrt/enum")
(load "jrt/locale")
(load "jrt/math")
(load "jrt/monitor")
(load "jrt/numconv")
(load "jrt/object")
(load "jrt/reflect")
(load "jrt/reflect_array")
(load "jrt/reflect_tables")
(load "jrt/standin_character")
(load "jrt/standin_lang")
(load "jrt/standin_reflect")
(load "jrt/string")
(load "jrt/stringbuilder")
(load "jrt/threadid")
(load "jrt/throwable")
(load "jrt/volatile")
(load "jrt/bench_test")
(load "jrt/enum_test")
(load "jrt/manifest_test")
(load "jrt/math_test")
(load "jrt/monitor_test")
(load "jrt/object_test")
(load "jrt/reflect_test")
(load "jrt/shims_test")
(load "jrt/string_test")
(load "jrt/testutil_test")
(load "jrt/throwable_test")
