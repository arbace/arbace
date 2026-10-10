;; jrt: the Go runtime's settings for a Java program, and profiling (doc/go/SPEED-NOTES.md).
(in-ns 'go.arbace.jrt)

(go/file "tuning.go"
  :imports [[os "os"] [debug "runtime/debug"] [runtime "runtime"] [pprof "runtime/pprof"] [strconv "strconv"]])

(go/const ^{:tag int :val 200
            :doc "DefaultGOGC is the collector's GOGC when the environment sets none: Java programs on
jrt allocate as on a JVM, which allocates from thread-local buffers and runs with a heap several
times its live data; Go's default 100 makes the collector run four times as often and costs
twice the CPU (doc/go/SPEED-NOTES.md).\n"} DefaultGOGC 200)

(go/const ^{:tag int :val 64
            :doc "DefaultMinHeapMB is the heap, in MiB, below which the collector does not run (a JVM's initial
heap, -Xms): small programs allocating fast would otherwise collect every few MiB, hundreds of
times a second (doc/go/SPEED-NOTES.md).\n"} DefaultMinHeapMB 64)

(go/var ^{:tag (slice byte)
          :doc "minHeap is the ballast that raises the collector's goal to the minimum heap: never
written, without pointers, so neither scanned nor resident (the pages stay untouched).\n"}
  minHeap)

(go/var ^{:tag (* os/File)} cpuProfile)
(go/var ^string memProfile)

(go/func init
  "init applies jrt's settings of the Go runtime when the program starts, before any main
package's own (whose start may raise GOGC for a while and then restore this setting): GOGC is
DefaultGOGC unless the environment sets GOGC, the minimum heap DefaultMinHeapMB MiB unless
ARBACE_MIN_HEAP_MB sets it (0: none), and allocations are sampled for a heap profile only when
ARBACE_MEMPROFILE asks for one.\n"
  []
  (when (== (os/Getenv "GOGC") "")
    (debug/SetGCPercent DefaultGOGC))
  (let [mb DefaultMinHeapMB]
    (let [(values n err) (strconv/Atoi (os/Getenv "ARBACE_MIN_HEAP_MB"))]
      (when (== err nil)
        (set! mb n)))
    (when (> mb 0)
      (set! minHeap (make (slice byte) (<< mb 20)))))
  (set! memProfile (os/Getenv "ARBACE_MEMPROFILE"))
  ;; Go samples allocations for the heap profile whenever a program links it (pprof does here);
  ;; each sample walks the stack, and the evaluator's are deep
  (when (== memProfile "")
    (set! runtime/MemProfileRate 0)))

(go/func StartRuntime
  "StartRuntime starts the profiles a program's run asks for, before its main runs (RunMain):
ARBACE_CPUPROFILE=FILE writes a CPU profile of the run to FILE (go tool pprof),
ARBACE_MEMPROFILE=FILE a heap profile when the program ends (RunMain's return or
System.exit).\n"
  []
  (let [p (os/Getenv "ARBACE_CPUPROFILE")]
    (when (and (!= p "") (== cpuProfile nil))
      (let [(values f err) (os/Create p)]
        (when (== err nil)
          (when (== (pprof/StartCPUProfile f) nil)
            (set! cpuProfile f)))))))

(go/func StopProfile
  "StopProfile ends the profiles StartRuntime began, if any.\n"
  []
  (when (!= memProfile "")
    (let [(values f err) (os/Create memProfile)]
      (set! memProfile "")
      (when (== err nil)
        (set! _ (pprof/WriteHeapProfile f))
        (.Close f))))
  (when (!= cpuProfile nil)
    (pprof/StopCPUProfile)
    (.Close cpuProfile)
    (set! cpuProfile nil)))
