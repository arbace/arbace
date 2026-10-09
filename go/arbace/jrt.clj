;; jrt, the Java runtime of Arbace on Go (doc/go/JRT-NOTES.md): the package arbace/jrt, its
;; hand-written Go forms (C2G-SPEC §4.3, §16 Q19). bin/jrt builds and tests it.
(ns go.arbace.jrt
  (:require [arbace.go :as go]))

(go/package jrt
  :path "arbace/jrt"
  :files ["access.go" "array.go" "atomic.go" "charset.go" "class.go" "classloader.go" "codec.go" "date.go" "dyn.go"
          "enum.go" "executor.go" "fdlibm.go" "files.go" "filesystem.go" "forkjoin.go" "host.go" "hostfs.go" "hostfs_linux.go" "locale.go" "locks.go" "math.go" "monitor.go"
          "natives.go" "numconv.go" "object.go" "reference.go" "reflect.go" "reflect_array.go"
          "reflect_tables.go" "standin_character.go" "standin_lang.go" "standin_reflect.go"
          "standin_timeunit.go" "string.go" "stringbuilder.go" "system.go" "thread.go" "threadid.go"
          "threadlocal.go" "throwable.go" "unsafe.go" "volatile.go" "vm.go"]
  :test-files ["bench_test.go" "concurrent_test.go" "enum_test.go" "filesystem_test.go" "manifest_test.go" "math_test.go"
               "monitor_test.go" "object_test.go" "reflect_test.go" "shims_test.go" "string_test.go"
               "system_test.go" "testutil_test.go" "thread_test.go" "throwable_test.go"])

(load "jrt/access")
(load "jrt/array")
(load "jrt/atomic")
(load "jrt/charset")
(load "jrt/class")
(load "jrt/classloader")
(load "jrt/codec")
(load "jrt/date")
(load "jrt/dyn")
(load "jrt/enum")
(load "jrt/executor")
(load "jrt/fdlibm")
(load "jrt/files")
(load "jrt/filesystem")
(load "jrt/forkjoin")
(load "jrt/host")
(load "jrt/hostfs")
(load "jrt/hostfs_linux")
(load "jrt/locale")
(load "jrt/locks")
(load "jrt/math")
(load "jrt/monitor")
(load "jrt/natives")
(load "jrt/numconv")
(load "jrt/object")
(load "jrt/reference")
(load "jrt/reflect")
(load "jrt/reflect_array")
(load "jrt/reflect_tables")
(load "jrt/standin_character")
(load "jrt/standin_lang")
(load "jrt/standin_reflect")
(load "jrt/standin_timeunit")
(load "jrt/string")
(load "jrt/stringbuilder")
(load "jrt/system")
(load "jrt/thread")
(load "jrt/threadid")
(load "jrt/threadlocal")
(load "jrt/throwable")
(load "jrt/unsafe")
(load "jrt/volatile")
(load "jrt/vm")
(load "jrt/bench_test")
(load "jrt/concurrent_test")
(load "jrt/enum_test")
(load "jrt/filesystem_test")
(load "jrt/manifest_test")
(load "jrt/math_test")
(load "jrt/monitor_test")
(load "jrt/object_test")
(load "jrt/reflect_test")
(load "jrt/shims_test")
(load "jrt/string_test")
(load "jrt/system_test")
(load "jrt/testutil_test")
(load "jrt/thread_test")
(load "jrt/throwable_test")
