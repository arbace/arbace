;; jrt: the Go runtime's settings for a Java program, and profiling (doc/go/SPEED-NOTES.md).
(in-ns 'go.arbace.jrt)

(go/file "tuning.go"
  :imports [[os "os"] [debug "runtime/debug"] [pprof "runtime/pprof"] [strconv "strconv"]])

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

(go/func StartRuntime
  "StartRuntime applies jrt's settings of the Go runtime before a program's main runs
(RunMain): GOGC is DefaultGOGC unless the environment sets GOGC, the minimum heap
DefaultMinHeapMB MiB unless ARBACE_MIN_HEAP_MB sets it (0: none); ARBACE_CPUPROFILE=FILE writes
a CPU profile of the run to FILE (go tool pprof), ARBACE_MEMPROFILE=FILE a heap profile
(allocations sampled as Go samples them) when the program ends (RunMain's return or
System.exit).\n"
  []
  (when (== (os/Getenv "GOGC") "")
    (debug/SetGCPercent DefaultGOGC))
  (let [mb DefaultMinHeapMB]
    (let [(values n err) (strconv/Atoi (os/Getenv "ARBACE_MIN_HEAP_MB"))]
      (when (== err nil)
        (set! mb n)))
    (when (and (> mb 0) (== minHeap nil))
      (set! minHeap (make (slice byte) (<< mb 20)))))
  (set! memProfile (os/Getenv "ARBACE_MEMPROFILE"))
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
