;; jrt: the current thread's number, for monitors and class initialization.
;;
;; Phase 2a: the number of the current Thread, found through the goroutine-local slot of the
;; patched runtime (C2G-SPEC §9.3; thread.go). Phase 1's stopgap parsed the goroutine id from
;; runtime.Stack's header (about 3 µs).
(in-ns 'go.arbace.jrt)

(go/file "threadid.go")

(go/func currentThreadID
  "currentThreadID is a number identifying the current thread, never 0, below 2^22 while
fewer than 2^22 threads exist (thread.go recycles them).\n"
  ^uint64 []
  (.Load (.-num (CurrentThread))))
