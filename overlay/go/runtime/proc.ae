;; Copyright 2014 The Go Authors. All rights reserved.
;; Use of this source code is governed by a BSD-style
;; license that can be found in the LICENSE file.

(in-ns 'go.runtime) (go/file "proc.go" :imports [


    ^{:line 8} [abi "internal/abi"]
    ^{:line 9} [cpu "internal/cpu"]
    ^{:line 10} [goarch "internal/goarch"]
    ^{:line 11} [goexperiment "internal/goexperiment"]
    ^{:line 12} [goos "internal/goos"]
    ^{:line 13} [atomic "internal/runtime/atomic"]
    ^{:line 14} [exithook "internal/runtime/exithook"]
    ^{:line 15} [maps "internal/runtime/maps"]
    ^{:line 16} [sys "internal/runtime/sys"]
    ^{:line 17} [strconv "internal/strconv"]
    ^{:line 18} [stringslite "internal/stringslite"]
    ^:alias ^{:line 19} [goos_overlay "runtime/goos"]
    ^{:line 20} [unsafe "unsafe"]])



(go/var ^{:tag string :doc "set using cmd/go/internal/modload.ModInfoProg\n"} modinfo)

;; Goroutine scheduler
;; The scheduler's job is to distribute ready-to-run goroutines over worker threads.
;;
;; The main concepts are:
;; G - goroutine.
;; M - worker thread, or machine.
;; P - processor, a resource that is required to execute Go code.
;;     M must have an associated P to execute Go code, however it can be
;;     blocked or in a syscall w/o an associated P.
;;
;; Design doc at https://golang.org/s/go11sched.

;; Worker thread parking/unparking.
;; We need to balance between keeping enough running worker threads to utilize
;; available hardware parallelism and parking excessive running worker threads
;; to conserve CPU resources and power. This is not simple for two reasons:
;; (1) scheduler state is intentionally distributed (in particular, per-P work
;; queues), so it is not possible to compute global predicates on fast paths;
;; (2) for optimal thread management we would need to know the future (don't park
;; a worker thread when a new goroutine will be readied in near future).
;;
;; Three rejected approaches that would work badly:
;; 1. Centralize all scheduler state (would inhibit scalability).
;; 2. Direct goroutine handoff. That is, when we ready a new goroutine and there
;;    is a spare P, unpark a thread and handoff it the thread and the goroutine.
;;    This would lead to thread state thrashing, as the thread that readied the
;;    goroutine can be out of work the very next moment, we will need to park it.
;;    Also, it would destroy locality of computation as we want to preserve
;;    dependent goroutines on the same thread; and introduce additional latency.
;; 3. Unpark an additional thread whenever we ready a goroutine and there is an
;;    idle P, but don't do handoff. This would lead to excessive thread parking/
;;    unparking as the additional threads will instantly park without discovering
;;    any work to do.
;;
;; The current approach:
;;
;; This approach applies to three primary sources of potential work: readying a
;; goroutine, new/modified-earlier timers, and idle-priority GC. See below for
;; additional details.
;;
;; We unpark an additional thread when we submit work if (this is wakep()):
;; 1. There is an idle P, and
;; 2. There are no "spinning" worker threads.
;;
;; A worker thread is considered spinning if it is out of local work and did
;; not find work in the global run queue or netpoller; the spinning state is
;; denoted in m.spinning and in sched.nmspinning. Threads unparked this way are
;; also considered spinning; we don't do goroutine handoff so such threads are
;; out of work initially. Spinning threads spin on looking for work in per-P
;; run queues and timer heaps or from the GC before parking. If a spinning
;; thread finds work it takes itself out of the spinning state and proceeds to
;; execution. If it does not find work it takes itself out of the spinning
;; state and then parks.
;;
;; If there is at least one spinning thread (sched.nmspinning>1), we don't
;; unpark new threads when submitting work. To compensate for that, if the last
;; spinning thread finds work and stops spinning, it must unpark a new spinning
;; thread. This approach smooths out unjustified spikes of thread unparking,
;; but at the same time guarantees eventual maximal CPU parallelism
;; utilization.
;;
;; The main implementation complication is that we need to be very careful
;; during spinning->non-spinning thread transition. This transition can race
;; with submission of new work, and either one part or another needs to unpark
;; another worker thread. If they both fail to do that, we can end up with
;; semi-persistent CPU underutilization.
;;
;; The general pattern for submission is:
;; 1. Submit work to the local or global run queue, timer heap, or GC state.
;; 2. #StoreLoad-style memory barrier.
;; 3. Check sched.nmspinning.
;;
;; The general pattern for spinning->non-spinning transition is:
;; 1. Decrement nmspinning.
;; 2. #StoreLoad-style memory barrier.
;; 3. Check all per-P work queues and GC for new work.
;;
;; Note that all this complexity does not apply to global run queue as we are
;; not sloppy about thread unparking when submitting to global queue. Also see
;; comments for nmspinning manipulation.
;;
;; How these different sources of work behave varies, though it doesn't affect
;; the synchronization approach:
;; * Ready goroutine: this is an obvious source of work; the goroutine is
;;   immediately ready and must run on some thread eventually.
;; * New/modified-earlier timer: The current timer implementation (see time.go)
;;   uses netpoll in a thread with no work available to wait for the soonest
;;   timer. If there is no thread waiting, we want a new spinning thread to go
;;   wait.
;; * Idle-priority GC: The GC wakes a stopped idle thread to contribute to
;;   background GC work (note: currently disabled per golang.org/issue/19112).
;;   Also see golang.org/issue/44313, as this should be extended to all GC
;;   workers.

(go/var
  ^{:line 121} [^m m0]
  ^{:line 122} [^g g0]
  ^{:line 123} [^{:tag (* mcache)} mcache0]
  ^{:line 124} [^uintptr raceprocctx0]
  ^{:line 125} [^mutex raceFiniLock])




(go/var ^{:tag (slice (* initTask)) :doc "This slice records the initializing tasks that need to be\ndone to start up the runtime. It is built by the linker.\n"} runtime_inittasks)



(go/var ^{:tag atomic/Bool :doc "mainInitDone is a signal used by cgocallbackg that initialization\nhas been completed. If this is false, wait on mainInitDoneChan.\n"} mainInitDone)




(go/var ^{:tag (chan bool) :doc "mainInitDoneChan is closed after initialization has been completed.\nIt is made before _cgo_notify_runtime_init_done, so all cgo\ncalls can rely on it existing.\n"} mainInitDoneChan)


(go/func ^:extern ^{:go/linkname "main_main main.main"} main_main [])


(go/var ^{:tag bool :doc "mainStarted indicates that the main M has started.\n"} mainStarted)


(go/var ^{:tag int64 :doc "runtimeInitTime is the nanotime() at which the runtime started.\n"} runtimeInitTime)


(go/var ^{:tag sigset :doc "Value to use for signal mask for newly created M's.\n"} initSigmask)


^{:go/end 347} (go/func main "The main goroutine.\n" []
  (let [mp (.-m (getg))]

    ;; Racectx of m0->g0 is used only as the parent of the main goroutine.
    ;; It must not be used for anything else.
    (set! (.-racectx (.-g0 mp)) 0)

    ;; Max stack size is 1 GB on 64-bit, 250 MB on 32-bit.
    ;; Using decimal instead of binary GB and MB because
    ;; they look nicer in the stack overflow failure message.
    (if (== goarch/PtrSize 8)
      (set! maxstacksize 1000000000)

      (set! maxstacksize 250000000))


    ;; An upper limit for max stack size. Used to avoid random crashes
    ;; after calling SetMaxStack and trying to allocate a stack that is too big,
    ;; since stackalloc works with 32-bit sizes.
    (set! maxstackceiling (* 2 maxstacksize))

    ;; Allow newproc to start new Ms.
    (set! mainStarted true)

    (when haveSysmon
      (systemstack ^{:go/end 181} (fn []
          (newm sysmon nil -1))))



    ;; Lock the main goroutine onto this, the main OS thread,
    ;; during initialization. Most programs won't care, but a few
    ;; do require certain calls to be made by the main thread.
    ;; Those can arrange for main.main to run in the main thread
    ;; by calling runtime.LockOSThread during initialization
    ;; to preserve the lock.
    (lockOSThread)

    (when (!= mp (addr m0))
      (throw "runtime.main not on m0"))


    ;; Record when the world started.
    ;; Must be before doInit for tracing init.
    (set! runtimeInitTime (nanotime))
    (when (== runtimeInitTime 0)
      (throw "nanotime returning zero"))


    (when (!= (.-inittrace debug) 0)
      (set! (.-id inittrace) (.-goid (getg)))
      (set! (.-active inittrace) true))


    (doInit runtime_inittasks) ; Must be before defer.

    ;; Defer unlock so that runtime.Goexit during init does the unlock too.
    (let [needUnlock true]
      (defer (^{:go/end 216} (fn []
            (when needUnlock
              (unlockOSThread)))))



      (gcenable)
      (defaultGOMAXPROCSUpdateEnable) ; don't STW before runtime initialized.

      ;; If we encountered a removed GODEBUG during startup we can panic now.
      (when [k (.-key invalidGODEBUG)] (!= k "")
        (let [v (.-value invalidGODEBUG)
            ^{:line 224} r (strconv/Itoa (.-removed invalidGODEBUG))]
          (fatal (+ "removed GODEBUG \"" k "\" set to old value \"" v "\" in environment (https://go.dev/doc/godebug#go-1" r ")"))))


      (set! mainInitDoneChan (make (chan bool)))
      (when iscgo
        (when (== _cgo_pthread_key_created nil)
          (throw "_cgo_pthread_key_created missing"))


        (when (!= GOOS "windows")
          (when (== _cgo_thread_start nil)
            (throw "_cgo_thread_start missing"))

          (when (== _cgo_setenv nil)
            (throw "_cgo_setenv missing"))

          (when (== _cgo_unsetenv nil)
            (throw "_cgo_unsetenv missing")))


        (when (== _cgo_notify_runtime_init_done nil)
          (throw "_cgo_notify_runtime_init_done missing"))


        ;; Set the x_crosscall2_ptr C function pointer variable point to crosscall2.
        (when (== set_crosscall2 nil)
          (throw "set_crosscall2 missing"))

        (set_crosscall2)

        ;; Start the template thread in case we enter Go from
        ;; a C-created thread and need to create a new thread.
        (startTemplateThread)
        (cgocall _cgo_notify_runtime_init_done nil))


      ;; Run the initializing tasks. Depending on build mode this
      ;; list can arrive a few different ways, but it will always
      ;; contain the init tasks computed by the linker for all the
      ;; packages in the program (excluding those added at runtime
      ;; by package plugin). Run through the modules in dependency
      ;; order (the order they are initialized by the dynamic
      ;; loader, i.e. they are added to the moduledata linked list).
      (let [last lastmoduledatap] ; grab before loop starts. Any added modules after this point will do their own doInit calls.
        (for [m (addr firstmoduledata)] true (set! m (.-next m))
          (doInit (.-inittasks m))
          (when (== m last)
            (break)))



        ;; Disable init tracing after main init done to avoid overhead
        ;; of collecting statistics in malloc and newproc
        (set! (.-active inittrace) false)

        (.Store mainInitDone true)
        (close mainInitDoneChan)

        (set! needUnlock false)
        (unlockOSThread)

        (when (or isarchive islibrary)
          ;; A program compiled with -buildmode=c-archive or c-shared
          ;; has a main, but it is not executed.
          (when (== GOARCH "wasm")
            ;; On Wasm, pause makes it return to the host.
            ;; Unlike cgo callbacks where Ms are created on demand,
            ;; on Wasm we have only one M. So we keep this M (and this
            ;; G) for callbacks.
            ;; Using the caller's SP unwinds this frame and backs to
            ;; goexit. The -16 is: 8 for goexit's (fake) return PC,
            ;; and pause's epilogue pops 8.
            (pause (- (sys/GetCallerSP) 16)) ; should not return
            (panic "unreachable"))

          (return))

        (let [fn main_main] ; make an indirect call, as the linker doesn't know the address of the main package when laying down the runtime
          (go/call fn)

          ;; Check for C memory leaks if using ASAN and we've made cgo calls,
          ;; or if we are running as a library in a C program.
          ;; We always make one cgo call, above, to notify_runtime_init_done,
          ;; so we ignore that one.
          ;; No point in leak checking if no cgo calls, since leak checking
          ;; just looks for objects allocated using malloc and friends.
          ;; Just checking iscgo doesn't help because asan implies iscgo.
          (let [exitHooksRun false]
            (when (and asanenabled ^:go/paren (or isarchive islibrary (> (NumCgoCall) 1)))
              (runExitHooks 0) ; lsandoleakcheck may not return
              (set! exitHooksRun true)
              (lsandoleakcheck))


            ;; Make racy client program work: if panicking on
            ;; another goroutine at the same time as main returns,
            ;; let the other goroutine finish printing the panic trace.
            ;; Once it does, it will exit. See issues 3934 and 20018.
            (when (!= (.Load runningPanicDefers) 0)
              ;; Running deferred functions should not take long.
              (for [c 0] (< c 1000) (inc! c)
                (when (== (.Load runningPanicDefers) 0)
                  (break))

                (Gosched)))


            (when (!= (.Load panicking) 0)
              (gopark nil nil waitReasonPanicWait traceBlockForever 1))

            (when (not exitHooksRun)
              (runExitHooks 0))

            (when raceenabled
              (racefini)) ; does not return


            (exit 0)
            (while true
              (let [^{:tag (* int32)} x (zero (* int32))]
                (set! @x 0)))))))))






^{:go/end 362} (go/func ^{:go/linkname "os_beforeExit os.runtime_beforeExit"} os_beforeExit "os_beforeExit is called from os.Exit(0).\n" [^int exitCode]
  (runExitHooks exitCode)
  (when (and (== exitCode 0) raceenabled)
    (racefini))


  ;; See comment in main, above.
  (when (and (== exitCode 0) asanenabled ^:go/paren (or isarchive islibrary (> (NumCgoCall) 1)))
    (lsandoleakcheck)))



^{:go/end 368} (go/func init []
  (set! exithook/Gosched Gosched)
  (set! exithook/Goid ^{:go/end 366} (fn ^uint64 [] (.-goid (getg))))
  (set! exithook/Throw throw))


^{:go/end 372} (go/func runExitHooks [^int code]
  (exithook/Run code))



^{:go/end 377} (go/func init "start forcegc helper goroutine\n" []
  (go (forcegchelper)))


^{:go/end 396} (go/func forcegchelper []
  (set! (.-g forcegc) (getg))
  (lockInit (addr (.-lock forcegc)) lockRankForcegc)
  (while true
    (lock (addr (.-lock forcegc)))
    (when (.Load (.-idle forcegc))
      (throw "forcegc: phase error"))

    (.Store (.-idle forcegc) true)
    (goparkunlock (addr (.-lock forcegc)) waitReasonForceGCIdle traceBlockSystemGoroutine 1)
    ;; this goroutine is explicitly resumed by sysmon
    (when (> (.-gctrace debug) 0)
      (println "GC forced"))

    ;; Time-triggered, fully concurrent.
    (gcStart (lit gcTrigger :kind gcTriggerTime :now (nanotime)))))







^{:go/end 405} (go/func ^:go/nosplit Gosched "Gosched yields the processor, allowing other goroutines to run. It does not\nsuspend the current goroutine, so execution resumes automatically.\n" []
  (checkTimeouts)
  (mcall gosched_m))






^{:go/end 413} (go/func ^:go/nosplit goschedguarded "goschedguarded yields the processor like gosched, but also checks\nfor forbidden states and opts out of the yield in those cases.\n" []
  (mcall goschedguarded_m))







^{:go/end 428} (go/func ^:go/nosplit goschedIfBusy "goschedIfBusy yields the processor like gosched, but only does so if\nthere are no idle Ps or if we're on the only P and there's nothing in\nthe run queue. In both cases, there is freely available idle time.\n" []
  (let [gp (getg)]
    ;; Call gosched if gp.preempt is set; we may be in a tight loop that
    ;; doesn't otherwise yield.
    (when (and (not (.-preempt gp)) (> (.Load (.-npidle sched)) 0))
      (return))

    (mcall gosched_m)))






























^{:go/end 476} (go/func ^{:go/linkname "gopark"} gopark "Puts the current goroutine into a waiting state and calls unlockf on the\nsystem stack.\n\nIf unlockf returns false, the goroutine is resumed.\n\nunlockf must not access this G's stack, as it may be moved between\nthe call to gopark and the call to unlockf.\n\nNote that because unlockf is called after putting the G into a waiting\nstate, the G may have already been readied by the time unlockf is called\nunless there is external synchronization preventing the G from being\nreadied. If unlockf returns false, it must guarantee that the G cannot be\nexternally readied.\n\nReason explains why the goroutine has been parked. It is displayed in stack\ntraces and heap dumps. Reasons should be unique and descriptive. Do not\nre-use reasons, add new ones.\n\ngopark should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n  - github.com/sagernet/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" [^{:tag (func [(* g) unsafe/Pointer] [bool])} unlockf ^unsafe/Pointer lock ^waitReason reason ^traceBlockReason traceReason ^int traceskip]
  (when (!= reason waitReasonSleep)
    (checkTimeouts)) ; timeouts may expire while two goroutines keep the scheduler busy

  (let [mp (acquirem)
      ^{:line 463} gp (.-curg mp)
      ^{:line 464} status (readgstatus gp)]
    (when (and (!= status _Grunning) (!= status _Gscanrunning))
      (throw "gopark: bad g status"))

    (set! (.-waitlock mp) lock)
    (set! (.-waitunlockf mp) unlockf)
    (set! (.-waitreason gp) reason)
    (set! (.-waitTraceBlockReason mp) traceReason)
    (set! (.-waitTraceSkip mp) traceskip)
    (releasem mp)
    ;; can't do anything that might move the G between Ms here.
    (mcall park_m)))




^{:go/end 482} (go/func goparkunlock "Puts the current goroutine into a waiting state and unlocks the lock.\nThe goroutine can be made runnable again by calling goready(gp).\n" [^{:tag (* mutex)} lock ^waitReason reason ^traceBlockReason traceReason ^int traceskip]
  (gopark parkunlock_c (conv unsafe/Pointer lock) reason traceReason traceskip))












^{:go/end 498} (go/func ^{:go/linkname "goready"} goready "goready should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n  - github.com/sagernet/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" [^{:tag (* g)} gp ^int traceskip]
  (systemstack ^{:go/end 497} (fn []
      (ready gp traceskip true))))




^{:go/end 536} (go/func ^:go/nosplit acquireSudog ^{:tag (* sudog)} []
  ;; Delicate dance: the semaphore implementation calls
  ;; acquireSudog, acquireSudog calls new(sudog),
  ;; new calls malloc, malloc can call the garbage collector,
  ;; and the garbage collector calls the semaphore implementation
  ;; in stopTheWorld.
  ;; Break the cycle by doing acquirem/releasem around new(sudog).
  ;; The acquirem/releasem increments m.locks during new(sudog),
  ;; which keeps the garbage collector from being invoked.
  (let [mp (acquirem)
      ^{:line 511} pp (.ptr (.-p mp))]
    (when (== (len (.-sudogcache pp)) 0)
      (lock (addr (.-sudoglock sched)))
      ;; First, try to grab a batch from central cache.
      (while (and (< (len (.-sudogcache pp)) (/ (cap (.-sudogcache pp)) 2)) (!= (.-sudogcache sched) nil))
        (let [s (.-sudogcache sched)]
          (set! (.-sudogcache sched) (.-next s))
          (set! (.-next s) nil)
          (set! (.-sudogcache pp) (append (.-sudogcache pp) s))))

      (unlock (addr (.-sudoglock sched)))
      ;; If the central cache is empty, allocate a new one.
      (when (== (len (.-sudogcache pp)) 0)
        (set! (.-sudogcache pp) (append (.-sudogcache pp) (new sudog)))))


    (let [n (len (.-sudogcache pp))
        ^{:line 528} s (aget (.-sudogcache pp) (- n 1))]
      (aset (.-sudogcache pp) (- n 1) nil)
      (set! (.-sudogcache pp) (subslice (.-sudogcache pp) _ (- n 1)))
      (when (!= (.get (.-elem s)) nil)
        (throw "acquireSudog: found s.elem != nil in cache"))

      (releasem mp)
      ^{:line 535} s)))



^{:go/end 586} (go/func ^:go/nosplit releaseSudog [^{:tag (* sudog)} s]
  (when (!= (.get (.-elem s)) nil)
    (throw "runtime: sudog with non-nil elem"))

  (when (.-isSelect s)
    (throw "runtime: sudog with non-false isSelect"))

  (when (!= (.-next s) nil)
    (throw "runtime: sudog with non-nil next"))

  (when (!= (.-prev s) nil)
    (throw "runtime: sudog with non-nil prev"))

  (when (!= (.-waitlink s) nil)
    (throw "runtime: sudog with non-nil waitlink"))

  (when (!= (.get (.-c s)) nil)
    (throw "runtime: sudog with non-nil c"))

  (let [gp (getg)]
    (when (!= (.-param gp) nil)
      (throw "runtime: releaseSudog with non-nil gp.param"))

    (let [mp (acquirem) ; avoid rescheduling to another P
        ^{:line 563} pp (.ptr (.-p mp))]
      (when (== (len (.-sudogcache pp)) (cap (.-sudogcache pp)))
        ;; Transfer half of local cache to the central cache.
        (let [(values ^{:tag (* sudog)} first ^{:tag (* sudog)} last) (zero (* sudog))]
          (while (> (len (.-sudogcache pp)) (/ (cap (.-sudogcache pp)) 2))
            (let [n (len (.-sudogcache pp))
                ^{:line 569} p (aget (.-sudogcache pp) (- n 1))]
              (aset (.-sudogcache pp) (- n 1) nil)
              (set! (.-sudogcache pp) (subslice (.-sudogcache pp) _ (- n 1)))
              (if (== first nil)
                (set! first p)

                (set! (.-next last) p))

              (set! last p)))

          (lock (addr (.-sudoglock sched)))
          (set! (.-next last) (.-sudogcache sched))
          (set! (.-sudogcache sched) first)
          (unlock (addr (.-sudoglock sched)))))

      (set! (.-sudogcache pp) (append (.-sudogcache pp) s))
      (releasem mp))))



^{:go/end 591} (go/func badmcall "called from assembly.\n" [^{:tag (func [(* g)])} fn]
  (throw "runtime: mcall called on m->g0 stack"))


^{:go/end 595} (go/func badmcall2 [^{:tag (func [(* g)])} fn]
  (throw "runtime: mcall function returned"))


^{:go/end 599} (go/func badreflectcall []
  (panic (conv plainError "arg size to reflect.call more than 1GB")))




^{:go/end 618} (go/func ^:go/nosplit ^:go/nowritebarrierrec badmorestackg0 []
  (when (not crashStackImplemented)
    (writeErrStr "fatal: morestack on g0\n")
    (return))


  (let [g (getg)]
    (switchToCrashStack ^{:go/end 617} (fn []
        (print "runtime: morestack on g0, stack [" (conv hex (.-lo (.-stack g))) " " (conv hex (.-hi (.-stack g))) "], sp=" (conv hex (.-sp (.-sched g))) ", called from\n")
        (set! (.-traceback (.-m g)) 2) ; include pc and sp in stack trace
        (traceback1 (.-pc (.-sched g)) (.-sp (.-sched g)) (.-lr (.-sched g)) g 0)
        (print "\n")

        (throw "morestack on g0")))))





^{:go/end 624} (go/func ^:go/nosplit ^:go/nowritebarrierrec badmorestackgsignal []
  (writeErrStr "fatal: morestack on gsignal\n"))



^{:go/end 629} (go/func ^:go/nosplit badctxt []
  (throw "ctxt != 0"))




(go/var ^{:tag g :doc "gcrash is a fake g that can be used when crashing due to bad\nstack conditions.\n"} gcrash)

(go/var ^{:tag (atomic/Pointer g)} crashingG)









^{:go/end 660} (go/func ^:go/nosplit ^:go/nowritebarrierrec switchToCrashStack "Switch to crashstack and call fn, with special handling of\nconcurrent and recursive cases.\n\nNosplit as it is called in a bad stack condition (we know\nmorestack would fail).\n" [^{:tag (func [])} fn]
  (let [me (getg)]
    (when (.CompareAndSwapNoWB crashingG nil me)
      (switchToCrashStack0 fn) ; should never return
      (abort))

    (when (== (.Load crashingG) me)
      ;; recursive crashing. too bad.
      (writeErrStr "fatal: recursive switchToCrashStack\n")
      (abort))

    ;; Another g is crashing. Give it some time, hopefully it will finish traceback.
    (usleep_no_g 100)
    (writeErrStr "fatal: concurrent switchToCrashStack\n")
    (abort)))





(go/const ^{:val true :doc "Disable crash stack on Windows for now. Apparently, throwing an exception\non a non-system-allocated crash stack causes EXCEPTION_STACK_OVERFLOW and\nhangs the process (see issue 63938).\n"} crashStackImplemented (!= GOOS "windows"))


(go/func ^:extern ^:go/noescape switchToCrashStack0 [^{:tag (func [])} fn]) ; in assembly

^{:go/end 673} (go/func lockedOSThread ^bool []
  (let [gp (getg)]
    (and (!= (.-lockedm gp) 0) (!= (.-lockedg (.-m gp)) 0))))


(go/var






  ^{:line 682} [^{:tag mutex :doc "allgs contains all Gs ever created (including dead Gs), and thus\nnever shrinks.\n\nAccess via the slice is protected by allglock or stop-the-world.\nReaders that cannot take the lock may (carefully!) use the atomic\nvariables below.\n"} allglock]
  ^{:line 683} [^{:tag (slice (* g))} allgs]













  ^{:line 697} [^{:tag uintptr :doc "allglen and allgptr are atomic variables that contain len(allgs) and\n&allgs[0] respectively. Proper ordering depends on totally-ordered\nloads and stores. Writes are protected by allglock.\n\nallgptr is updated before allglen. Readers should read allglen\nbefore allgptr to ensure that allglen is always <= len(allgptr). New\nGs appended during the race can be missed. For a consistent view of\nall Gs, allglock must be held.\n\nallgptr copies should always be stored as a concrete type or\nunsafe.Pointer, not uintptr, to ensure that GC can still reach it\neven if it points to a stale array.\n"} allglen]
  ^{:line 698} [^{:tag (* (* g))} allgptr])


^{:go/end 713} (go/func allgadd [^{:tag (* g)} gp]
  (when (== (readgstatus gp) _Gidle)
    (throw "allgadd: bad status Gidle"))


  (lock (addr allglock))
  (set! allgs (append allgs gp))
  (when (!= (addr (aget allgs 0)) allgptr)
    (atomicstorep (conv unsafe/Pointer (addr allgptr)) (conv unsafe/Pointer (addr (aget allgs 0)))))

  (atomic/Storeuintptr (addr allglen) (conv uintptr (len allgs)))
  (unlock (addr allglock)))





^{:go/end 727} (go/func allGsSnapshot "allGsSnapshot returns a snapshot of the slice of all Gs.\n\nThe world must be stopped or allglock must be held.\n" ^{:tag (slice (* g))} []
  (assertWorldStoppedOrLockHeld (addr allglock))

  ;; Because the world is stopped or allglock is held, allgadd
  ;; cannot happen concurrently with this. allgs grows
  ;; monotonically and existing entries never change, so we can
  ;; simply return a copy of the slice header. For added safety,
  ;; we trim everything past len because that can still change.
  (subslice allgs _ (len allgs) (len allgs)))



^{:go/end 734} (go/func atomicAllG "atomicAllG returns &allgs[0] and len(allgs) for use with atomicAllGIndex.\n" [] :results [(* (* g)) uintptr]
  (let [length (atomic/Loaduintptr (addr allglen))
      ^{:line 732} ptr (conv (* (* g)) (atomic/Loadp (conv unsafe/Pointer (addr allgptr))))]
    (return ptr length)))



^{:go/end 739} (go/func atomicAllGIndex "atomicAllGIndex returns ptr[i] with the allgptr returned from atomicAllG.\n" ^{:tag (* g)} [^{:tag (* (* g))} ptr ^uintptr i]
  ^{:line 738} @(conv (* (* g)) (add (conv unsafe/Pointer ptr) (* i goarch/PtrSize))))





^{:go/end 750} (go/func forEachG "forEachG calls fn on every G from allgs.\n\nforEachG takes a lock to exclude concurrent addition of new Gs.\n" [^{:tag (func [^{:tag (* g)} gp])} fn]
  (lock (addr allglock))
  (range [_ gp allgs]
    (go/call fn gp))

  (unlock (addr allglock)))






^{:go/end 763} (go/func forEachGRace "forEachGRace calls fn on every G from allgs.\n\nforEachGRace avoids locking, but does not exclude addition of new Gs during\nexecution, which may be missed.\n" [^{:tag (func [^{:tag (* g)} gp])} fn]
  (let [(values ptr length) (atomicAllG)]
    (for [i (conv uintptr 0)] (< i length) (inc! i)
      (let [gp (atomicAllGIndex ptr i)]
        (go/call fn gp)))

    (return)))


(go/const


  ^{:line 768} [^{:val 16 :doc "Number of goroutine ids to grab from sched.goidgen to local per-P cache at once.\n16 seems to provide enough amortization, but other than that it's mostly arbitrary number.\n"} _GoidCacheBatch 16])




^{:go/end 800} (go/func cpuinit "cpuinit sets up CPU feature flags and calls internal/cpu.Initialize. env should be the complete\nvalue of the GODEBUG environment variable.\n" [^string env]
  (cpu/Initialize env)

  ;; Support cpu feature variables are used in code generated by the compiler
  ;; to guard execution of instructions that can not be assumed to be always supported.
  (switch GOARCH
    (case ["386" "amd64"]
      (set! x86HasAVX (.-HasAVX cpu/X86))
      (set! x86HasFMA (.-HasFMA cpu/X86))
      (set! x86HasPOPCNT (.-HasPOPCNT cpu/X86))
      (set! x86HasSSE41 (.-HasSSE41 cpu/X86)))

    (case ["arm"]
      (set! armHasVFPv4 (.-HasVFPv4 cpu/ARM)))

    (case ["arm64"]
      (set! arm64HasATOMICS (.-HasATOMICS cpu/ARM64)))

    (case ["loong64"]
      (set! loong64HasLAMCAS (.-HasLAMCAS cpu/Loong64))
      (set! loong64HasLAM_BH (.-HasLAM_BH cpu/Loong64))
      (set! loong64HasDBAR_HINTS (.-HasDBAR_HINTS cpu/Loong64))
      (set! loong64HasLSX (.-HasLSX cpu/Loong64)))

    (case ["riscv64"]
      (set! riscv64HasZbb (.-HasZbb cpu/RISCV64)))))








^{:go/end 835} (go/func getGodebugEarly "getGodebugEarly extracts the environment variable GODEBUG from the environment on\nUnix-like operating systems and returns it. This function exists to extract GODEBUG\nearly before much of the runtime is initialized.\n\nReturns nil, false if OS doesn't provide env vars early in the init sequence.\n" [] :results [string bool]
  (let [^:const ^{:val "GODEBUG="} prefix "GODEBUG="
      ^{:line 809 :tag string} env (zero string)]
    (switch GOOS
      (case ["aix" "darwin" "ios" "dragonfly" "freebsd" "netbsd" "openbsd" "illumos" "solaris" "linux"]
        ;; Similar to goenv_unix but extracts the environment value for
        ;; GODEBUG directly.
        ;; TODO(moehrmann): remove when general goenvs() can be called before cpuinit()
        (let [n (conv int32 0)]
          (while (!= (argv_index argv (+ argc 1 n)) nil)
            (inc! n))


          (for [i (conv int32 0)] (< i n) (inc! i)
            (let [p (argv_index argv (+ argc 1 i))
                ^{:line 822} s (unsafe/String p (findnull p))]

              (when (stringslite/HasPrefix s prefix)
                (set! env (subslice (gostringnocopy p) (len prefix)))
                (break))))


          (break)))

      (default
        (return "" false)))

    (return env true)))










^{:go/end 975} (go/func schedinit "The bootstrap sequence is:\n\n\tcall osinit\n\tcall schedinit\n\tmake & queue new G\n\tcall runtime·mstart\n\nThe new G calls runtime·main.\n" []
  (lockInit (addr (.-lock sched)) lockRankSched)
  (lockInit (addr (.-sysmonlock sched)) lockRankSysmon)
  (lockInit (addr (.-deferlock sched)) lockRankDefer)
  (lockInit (addr (.-sudoglock sched)) lockRankSudog)
  (lockInit (addr deadlock) lockRankDeadlock)
  (lockInit (addr paniclk) lockRankPanic)
  (lockInit (addr allglock) lockRankAllg)
  (lockInit (addr allpLock) lockRankAllp)
  (lockInit (addr (.-lock reflectOffs)) lockRankReflectOffs)
  (lockInit (addr finlock) lockRankFin)
  (lockInit (addr (.-lock cpuprof)) lockRankCpuprof)
  (lockInit (addr computeMaxProcsLock) lockRankComputeMaxProcs)
  (.init allocmLock lockRankAllocmR lockRankAllocmRInternal lockRankAllocmW)
  (.init execLock lockRankExecR lockRankExecRInternal lockRankExecW)
  (traceLockInit)
  ;; Enforce that this lock is always a leaf lock.
  ;; All of this lock's critical sections should be
  ;; extremely short.
  (lockInit (addr (.-noPLock (.-heapStats memstats))) lockRankLeafRank)

  (lockVerifyMSize)

  (.init (.-midle sched) (unsafe/Offsetof (.-idleNode (lit m))))

  ;; raceinit must be the first call to race detector.
  ;; In particular, it must be done before mallocinit below calls racemapshadow.
  (let [gp (getg)]
    (when raceenabled
      (set! (values (.-racectx gp) raceprocctx0) (raceinit)))


    (set! (.-maxmcount sched) 10000)
    (.Store crashFD (bit-not (conv uintptr 0)))

    ;; The world starts stopped.
    (worldStopped)

    (let [(values godebug parsedGodebug) (getGodebugEarly)]
      (when parsedGodebug
        (parseRuntimeDebugVars godebug))

      (.init ticks) ; run as early as possible
      (moduledataverify)
      (stackinit)

      (when randomizeHeapBase
        (randinit)) ; must run before mallocinit

      (mallocinit)
      (when (not randomizeHeapBase)
        (randinit)) ; must run before alginit, mcommoninit


      (cpuinit godebug) ; must run before alginit
      (maps/AlgInit) ; maps, hash, rand must not be used before this call
      (mcommoninit (.-m gp) -1)
      (modulesinit) ; provides activeModules
      (typelinksinit) ; uses maps, activeModules
      (itabsinit) ; uses activeModules
      (stkobjinit) ; must run before GC starts

      (sigsave (addr (.-sigmask (.-m gp))))
      (set! initSigmask (.-sigmask (.-m gp)))

      (goargs)
      (goenvs)
      (secure)
      (checkfds)
      (when (not parsedGodebug)
        ;; Some platforms, e.g., Windows, didn't make env vars available "early",
        ;; so try again now.
        (parseRuntimeDebugVars (gogetenv "GODEBUG")))

      (finishDebugVarsSetup)
      (gcinit)

      ;; Allocate stack space that can be used when crashing due to bad stack
      ;; conditions, e.g. morestack on g0.
      (set! (.-stack gcrash) (stackalloc 16384))
      (set! (.-stackguard0 gcrash) (+ (.-lo (.-stack gcrash)) 1000))
      (set! (.-stackguard1 gcrash) (+ (.-lo (.-stack gcrash)) 1000))

      ;; if disableMemoryProfiling is set, update MemProfileRate to 0 to turn off memprofile.
      ;; Note: parsedebugvars may update MemProfileRate, but when disableMemoryProfiling is
      ;; set to true by the linker, it means that nothing is consuming the profile, it is
      ;; safe to set MemProfileRate to 0.
      (when disableMemoryProfiling
        (set! MemProfileRate 0))


      ;; mcommoninit runs before parsedebugvars, so init profstacks again.
      (mProfStackInit (.-m gp))
      (defaultGOMAXPROCSInit)

      (lock (addr (.-lock sched)))
      (.Store (.-lastpoll sched) (nanotime))
      (let [^int32 procs (zero int32)]
        (if [(values n err) (strconv/ParseInt (gogetenv "GOMAXPROCS") 10 32)] (and (== err nil) (> n 0)) (do
            (set! procs (conv int32 n))
            (set! (.-customGOMAXPROCS sched) true))

          ;; Use numCPUStartup for initial GOMAXPROCS for two reasons:
          ;;
          ;; 1. We just computed it in osinit, recomputing is (minorly) wasteful.
          ;;
          ;; 2. More importantly, if debug.containermaxprocs == 0 &&
          ;;    debug.updatemaxprocs == 0, we want to guarantee that
          ;;    runtime.GOMAXPROCS(0) always equals runtime.NumCPU (which is
          ;;    just numCPUStartup).
          (set! procs (defaultGOMAXPROCS numCPUStartup)))

        (when (!= (procresize procs) nil)
          (throw "unknown runnable goroutine during bootstrap"))

        (unlock (addr (.-lock sched)))

        ;; World is effectively started now, as P's can run.
        (worldStarted)

        (when (== buildVersion "")
          ;; Condition should never trigger. This code just serves
          ;; to ensure runtime·buildVersion is kept in the resulting binary.
          (set! buildVersion "unknown"))

        (when (== (len modinfo) 1)
          ;; Condition should never trigger. This code just serves
          ;; to ensure runtime·modinfo is kept in the resulting binary.
          (set! modinfo ""))))))



^{:go/end 981} (go/func dumpgstatus [^{:tag (* g)} gp]
  (let [thisg (getg)]
    (print "runtime:   gp: gp=" gp ", goid=" (.-goid gp) ", gp->atomicstatus=" (readgstatus gp) "\n")
    (print "runtime: getg:  g=" thisg ", goid=" (.-goid thisg) ",  g->atomicstatus=" (readgstatus thisg) "\n")))



^{:go/end 1000} (go/func checkmcount "sched.lock must be held.\n" []
  (assertLockHeld (addr (.-lock sched)))

  ;; Exclude extra M's, which are used for cgocallback from threads
  ;; created in C.
  ;;
  ;; The purpose of the SetMaxThreads limit is to avoid accidental fork
  ;; bomb from something like millions of goroutines blocking on system
  ;; calls, causing the runtime to create millions of threads. By
  ;; definition, this isn't a problem for threads created in C, so we
  ;; exclude them from the limit. See https://go.dev/issue/60004.
  (let [count (- (mcount) (conv int32 (.Load extraMInUse)) (conv int32 (.Load extraMLength)))]
    (when (> count (.-maxmcount sched))
      (print "runtime: program exceeds " (.-maxmcount sched) "-thread limit\n")
      (throw "thread exhaustion"))))







^{:go/end 1016} (go/func mReserveID "mReserveID returns the next ID to use for a new m. This new m is immediately\nconsidered 'running' by checkdead.\n\nsched.lock must be held.\n" ^int64 []
  (assertLockHeld (addr (.-lock sched)))

  (when (< (+ (.-mnext sched) 1) (.-mnext sched))
    (throw "runtime: thread ID overflow"))

  (let [id (.-mnext sched)]
    (inc! (.-mnext sched))
    (checkmcount)
    ^{:line 1015} id))



^{:go/end 1058} (go/func mcommoninit "Pre-allocated ID may be passed as 'id', or omitted by passing -1.\n" [^{:tag (* m)} mp ^int64 id]
  (let [gp (getg)]

    ;; g0 stack won't make sense for user (and is not necessary unwindable).
    (when (!= gp (.-g0 (.-m gp)))
      (callers 1 (subslice (.-createstack mp))))


    (lock (addr (.-lock sched)))

    (if (>= id 0)
      (set! (.-id mp) id)

      (set! (.-id mp) (mReserveID)))


    (set! (.-self mp) (newMWeakPointer mp))

    (mrandinit mp)

    (mpreinit mp)
    (when (!= (.-gsignal mp) nil)
      (set! (.-stackguard1 (.-gsignal mp)) (+ (.-lo (.-stack (.-gsignal mp))) stackGuard)))


    ;; Add to allm so garbage collector doesn't free g->m
    ;; when it is just in a register or thread-local storage.
    (set! (.-alllink mp) allm)

    ;; NumCgoCall and others iterate over allm w/o schedlock,
    ;; so we need to publish it safely.
    (atomicstorep (conv unsafe/Pointer (addr allm)) (conv unsafe/Pointer mp))
    (unlock (addr (.-lock sched)))

    ;; Allocate memory to hold a cgo traceback if the cgo call crashes.
    (when (or iscgo (== GOOS "solaris") (== GOOS "illumos") (== GOOS "windows"))
      (set! (.-cgoCallers mp) (new cgoCallers)))

    (mProfStackInit mp)))






^{:go/end 1072} (go/func mProfStackInit "mProfStackInit is used to eagerly initialize stack trace buffers for\nprofiling. Lazy allocation would have to deal with reentrancy issues in\nmalloc and runtime locks for mLockProfile.\nTODO(mknyszek): Implement lazy allocation if this becomes a problem.\n" [^{:tag (* m)} mp]
  (when (== (.-profstackdepth debug) 0)
    ;; debug.profstack is set to 0 by the user, or we're being called from
    ;; schedinit before parsedebugvars.
    (return))

  (set! (.-profStack mp) (makeProfStackFP))
  (set! (.-stack (.-mLockProfile mp)) (makeProfStackFP)))





^{:go/end 1085} (go/func makeProfStackFP "makeProfStackFP creates a buffer large enough to hold a maximum-sized stack\ntrace as well as any additional frames needed for frame pointer unwinding\nwith delayed inline expansion.\n" ^{:tag (slice uintptr)} []
  ;; The "1" term is to account for the first stack entry being
  ;; taken up by a "skip" sentinel value for profilers which
  ;; defer inline frame expansion until the profile is reported.
  ;; The "maxSkip" term is for frame pointer unwinding, where we
  ;; want to end up with debug.profstackdebth frames but will discard
  ;; some "physical" frames to account for skipping.
  (make (slice uintptr) (+ 1 maxSkip (.-profstackdepth debug))))




^{:go/end 1089} (go/func makeProfStack "makeProfStack returns a buffer large enough to hold a maximum-sized stack\ntrace.\n" ^{:tag (slice uintptr)} [] (make (slice uintptr) (.-profstackdepth debug)))


^{:go/end 1092} (go/func ^{:go/linkname "pprof_makeProfStack"} pprof_makeProfStack ^{:tag (slice uintptr)} [] (makeProfStack))

^{:go/end 1098} (go/method becomeSpinning [^{:tag (* m)} mp]
  (set! (.-spinning mp) true)
  (.Add (.-nmspinning sched) 1)
  (.Store (.-needspinning sched) 0))









^{:go/end 1110} (go/method ^:go/yeswritebarrierrec snapshotAllp "Take a snapshot of allp, for use after dropping the P.\n\nMust be called with a P, but the returned slice may be used after dropping\nthe P. The M holds a reference on the snapshot to keep the backing array\nalive.\n" ^{:tag (slice (* p))} [^{:tag (* m)} mp]
  (set! (.-allpSnapshot mp) allp)
  (.-allpSnapshot mp))








^{:go/end 1120} (go/method ^:go/yeswritebarrierrec clearAllpSnapshot "Clear the saved allp snapshot. Should be called as soon as the snapshot is\nno longer required.\n\nMust be called after reacquiring a P, as it requires a write barrier.\n" [^{:tag (* m)} mp]
  (set! (.-allpSnapshot mp) nil))


^{:go/end 1124} (go/method hasCgoOnStack ^bool [^{:tag (* m)} mp]
  (or (> (.-ncgo mp) 0) (.-isextra mp)))


(go/const


  ^{:line 1129} [^{:val false :doc "osHasLowResTimer indicates that the platform's internal timer system has a low resolution,\ntypically on the order of 1 ms or more.\n"} osHasLowResTimer (or (== GOOS "windows") (== GOOS "openbsd") (== GOOS "netbsd"))]



  ^{:line 1133} [^{:val 0 :doc "osHasLowResClockInt is osHasLowResClock but in integer form, so it can be used to create\nconstants conditionally.\n"} osHasLowResClockInt goos/IsWindows]



  ^{:line 1137} [^{:val false :doc "osHasLowResClock indicates that timestamps produced by nanotime on the platform have a\nlow resolution, typically on the order of 1 ms or more.\n"} osHasLowResClock (> osHasLowResClockInt 0)])



^{:go/end 1161} (go/func ready "Mark gp ready to run.\n" [^{:tag (* g)} gp ^int traceskip ^bool next]
  (let [status (readgstatus gp)

      ;; Mark runnable.
      ^{:line 1145} mp (acquirem)] ; disable preemption because it can be holding p in a local var
    (when (!= (bit-and-not status _Gscan) _Gwaiting)
      (dumpgstatus gp)
      (throw "bad g->status in ready"))


    ;; status is Gwaiting or Gscanwaiting, make Grunnable and put on runq
    (let [trace (traceAcquire)]
      (casgstatus gp _Gwaiting _Grunnable)
      (when (.ok trace)
        (.GoUnpark trace gp traceskip)
        (traceRelease trace))

      (runqput (.ptr (.-p mp)) gp next)
      (wakep)
      (releasem mp))))




(go/const ^{:val 2147483647 :doc "freezeStopWait is a large value that freezetheworld sets\nsched.stopwait to in order to request that all Gs permanently stop.\n"} freezeStopWait 0x7fffffff)



(go/var ^{:tag atomic/Bool :doc "freezing is set to non-zero if the runtime is trying to freeze the\nworld.\n"} freezing)




^{:go/end 1222} (go/func freezetheworld "Similar to stopTheWorld but best-effort and can be called several times.\nThere is no reverse operation, used during crashing.\nThis function must not lock any mutexes.\n" []
  (.Store freezing true)
  (when (> (.-dontfreezetheworld debug) 0)
    ;; Don't prempt Ps to stop goroutines. That will perturb
    ;; scheduler state, making debugging more difficult. Instead,
    ;; allow goroutines to continue execution.
    ;;
    ;; fatalpanic will tracebackothers to trace all goroutines. It
    ;; is unsafe to trace a running goroutine, so tracebackothers
    ;; will skip running goroutines. That is OK and expected, we
    ;; expect users of dontfreezetheworld to use core files anyway.
    ;;
    ;; However, allowing the scheduler to continue running free
    ;; introduces a race: a goroutine may be stopped when
    ;; tracebackothers checks its status, and then start running
    ;; later when we are in the middle of traceback, potentially
    ;; causing a crash.
    ;;
    ;; To mitigate this, when an M naturally enters the scheduler,
    ;; schedule checks if freezing is set and if so stops
    ;; execution. This guarantees that while Gs can transition from
    ;; running to stopped, they can never transition from stopped
    ;; to running.
    ;;
    ;; The sleep here allows racing Ms that missed freezing and are
    ;; about to run a G to complete the transition to running
    ;; before we start traceback.
    (usleep 1000)
    (return))


  ;; stopwait and preemption requests can be lost
  ;; due to races with concurrently executing threads,
  ;; so try several times
  (for [i 0] (< i 5) (inc! i)
    ;; this should tell the scheduler to not start any new goroutines
    (set! (.-stopwait sched) freezeStopWait)
    (.Store (.-gcwaiting sched) true)
    ;; this should stop running goroutines
    (when (not (preemptall))
      (break)) ; no running goroutines

    (usleep 1000))

  ;; to be sure
  (usleep 1000)
  (preemptall)
  (usleep 1000))






^{:go/end 1230} (go/func ^:go/nosplit readgstatus "All reads and writes of g's status go through readgstatus, casgstatus\ncastogscanstatus, casfrom_Gscanstatus.\n" ^uint32 [^{:tag (* g)} gp]
  (.Load (.-atomicstatus gp)))






^{:go/end 1262} (go/func casfrom_Gscanstatus "The Gscanstatuses are acting like locks and this releases them.\nIf it proves to be a performance hit we should be able to make these\nsimple atomic stores but for now we are going to throw if\nwe see an inconsistent state.\n" [^{:tag (* g)} gp ^uint32 oldval ^uint32 newval]
  (let [success false]

    ;; Check that transition is valid.
    (switch oldval
      (default
        (print "runtime: casfrom_Gscanstatus bad oldval gp=" gp ", oldval=" (conv hex oldval) ", newval=" (conv hex newval) "\n")
        (dumpgstatus gp)
        (throw "casfrom_Gscanstatus:top gp->status is not in scan state"))
      (case [_Gscanrunnable
          ^{:line 1246} _Gscanwaiting
          ^{:line 1247} _Gscanrunning
          ^{:line 1248} _Gscansyscall
          ^{:line 1249} _Gscanleaked
          ^{:line 1250} _Gscanpreempted
          ^{:line 1251} _Gscandeadextra]
        (when (== newval (bit-and-not oldval _Gscan))
          (set! success (.CompareAndSwap (.-atomicstatus gp) oldval newval)))))


    (when (not success)
      (print "runtime: casfrom_Gscanstatus failed gp=" gp ", oldval=" (conv hex oldval) ", newval=" (conv hex newval) "\n")
      (dumpgstatus gp)
      (throw "casfrom_Gscanstatus: gp->status is not in scan state"))

    (releaseLockRankAndM lockRankGscan)))




^{:go/breaks [7] :go/end 1286} (go/func castogscanstatus "This will return false if the gp is not in the expected status and the cas fails.\nThis acts like a lock acquire while the casfromgstatus acts like a lock release.\n" ^bool [^{:tag (* g)} gp ^uint32 oldval ^uint32 newval]
  (switch oldval
    (case [_Grunnable
        ^{:line 1269} _Grunning
        ^{:line 1270} _Gwaiting
        ^{:line 1271} _Gleaked
        ^{:line 1272} _Gsyscall
        ^{:line 1273} _Gdeadextra]
      (when (== newval (bit-or oldval _Gscan))
        (let [r (.CompareAndSwap (.-atomicstatus gp) oldval newval)]
          (when r
            (acquireLockRankAndM lockRankGscan))

          (return r)))))



  (print "runtime: castogscanstatus oldval=" (conv hex oldval) " newval=" (conv hex newval) "\n")
  (throw "bad oldval passed to castogscanstatus")
  false)




(go/var ^{:doc "casgstatusAlwaysTrack is a debug flag that causes casgstatus to always track\nvarious latencies on every transition instead of sampling them.\n"} casgstatusAlwaysTrack false)







^{:go/end 1404} (go/func ^:go/nosplit casgstatus "If asked to move to or from a Gscanstatus this will throw. Use the castogscanstatus\nand casfrom_Gscanstatus instead.\ncasgstatus will loop if the g->atomicstatus is in a Gscan status until the routine that\nput it in the Gscan state is finished.\n" [^{:tag (* g)} gp ^uint32 oldval ^uint32 newval]
  (when (or ^:go/paren (!= (bit-and oldval _Gscan) 0) ^:go/paren (!= (bit-and newval _Gscan) 0) (== oldval newval))
    (systemstack ^{:go/end 1305} (fn []
        ;; Call on the systemstack to prevent print and throw from counting
        ;; against the nosplit stack reservation.
        (print "runtime: casgstatus: oldval=" (conv hex oldval) " newval=" (conv hex newval) "\n")
        (throw "casgstatus: bad incoming values"))))



  (lockWithRankMayAcquire nil lockRankGscan)

  ;; See https://golang.org/cl/21503 for justification of the yield delay.
  (let [^:const ^{:val 5000} yieldDelay (* 5 1000)
      ^{:line 1312 :tag int64} nextYield (zero int64)]

    ;; loop if gp->atomicstatus is in a scan state giving
    ;; GC time to finish and change the state to oldval.
    (for [i 0] (not (.CompareAndSwap (.-atomicstatus gp) oldval newval)) (inc! i)
      (when (and (== oldval _Gwaiting) (== (.Load (.-atomicstatus gp)) _Grunnable))
        (systemstack ^{:go/end 1322} (fn []
            ;; Call on the systemstack to prevent throw from counting
            ;; against the nosplit stack reservation.
            (throw "casgstatus: waiting for Gwaiting but is Grunnable"))))


      (when (== i 0)
        (set! nextYield (+ (nanotime) yieldDelay)))

      (if (< (nanotime) nextYield)
        (for [x 0] (and (< x 10) (!= (.Load (.-atomicstatus gp)) oldval)) (inc! x)
          (procyield 1))

        (do
          (osyield)
          (set! nextYield (+ (nanotime) (/ yieldDelay 2))))))



    (when (!= (.-bubble gp) nil)
      (systemstack ^{:go/end 1340} (fn []
          (.changegstatus (.-bubble gp) gp oldval newval))))



    (when (and ^:go/paren (or (== oldval _Grunning) (== oldval _Gsyscall)) ^:go/paren (and (!= newval _Grunning) (!= newval _Gsyscall)))
      ;; Track every gTrackingPeriod time a goroutine transitions out of _Grunning or _Gsyscall.
      ;; Do not track _Grunning <-> _Gsyscall transitions, since they're two very similar states.
      (when (or casgstatusAlwaysTrack (== (% (.-trackingSeq gp) gTrackingPeriod) 0))
        (set! (.-tracking gp) true))

      (inc! (.-trackingSeq gp)))

    (when (not (.-tracking gp))
      (return))


    ;; Handle various kinds of tracking.
    ;;
    ;; Currently:
    ;; - Time spent in runnable.
    ;; - Time spent blocked on a sync.Mutex or sync.RWMutex.
    (switch oldval
      (case [_Grunnable]
        ;; We transitioned out of runnable, so measure how much
        ;; time we spent in this state and add it to
        ;; runnableTime.
        (let [now (nanotime)]
          (set! (.-runnableTime gp) + (- now (.-trackingStamp gp)))
          (set! (.-trackingStamp gp) 0)))
      (case [_Gwaiting]
        (when (not (.isMutexWait (.-waitreason gp)))
          ;; Not blocking on a lock.
          (break))

        ;; Blocking on a lock, measure it. Note that because we're
        ;; sampling, we have to multiply by our sampling period to get
        ;; a more representative estimate of the absolute value.
        ;; gTrackingPeriod also represents an accurate sampling period
        ;; because we can only enter this state from _Grunning.
        (let [now (nanotime)]
          (.Add (.-totalMutexWaitTime sched) (* (- now (.-trackingStamp gp)) gTrackingPeriod))
          (set! (.-trackingStamp gp) 0))))

    (switch newval
      (case [_Gwaiting]
        (when (not (.isMutexWait (.-waitreason gp)))
          ;; Not blocking on a lock.
          (break))

        ;; Blocking on a lock. Write down the timestamp.
        (let [now (nanotime)]
          (set! (.-trackingStamp gp) now)))
      (case [_Grunnable]
        ;; We just transitioned into runnable, so record what
        ;; time that happened.
        (let [now (nanotime)]
          (set! (.-trackingStamp gp) now)))
      (case [_Grunning]
        ;; We're transitioning into running, so turn off
        ;; tracking and record how much time we spent in
        ;; runnable.
        (set! (.-tracking gp) false)
        (.record (.-timeToRun sched) (.-runnableTime gp))
        (set! (.-runnableTime gp) 0)))))






^{:go/end 1413} (go/func casGToWaiting "casGToWaiting transitions gp from old to _Gwaiting, and sets the wait reason.\n\nUse this over casgstatus when possible to ensure that a waitreason is set.\n" [^{:tag (* g)} gp ^uint32 old ^waitReason reason]
  ;; Set the wait reason before calling casgstatus, because casgstatus will use it.
  (set! (.-waitreason gp) reason)
  (casgstatus gp old _Gwaiting))









^{:go/end 1427} (go/func casGToWaitingForSuspendG "casGToWaitingForSuspendG transitions gp from old to _Gwaiting, and sets the wait reason.\nThe wait reason must be a valid isWaitingForSuspendG wait reason.\n\nWhile a goroutine is in this state, it's stack is effectively pinned.\nThe garbage collector must not shrink or otherwise mutate the goroutine's stack.\n\nUse this over casgstatus when possible to ensure that a waitreason is set.\n" [^{:tag (* g)} gp ^uint32 old ^waitReason reason]
  (when (not (.isWaitingForSuspendG reason))
    (throw "casGToWaitingForSuspendG with non-isWaitingForSuspendG wait reason"))

  (casGToWaiting gp old reason))






^{:go/end 1446} (go/func casGToPreemptScan "casGToPreemptScan transitions gp from _Grunning to _Gscan|_Gpreempted.\n\nTODO(austin): This is the only status operation that both changes\nthe status and locks the _Gscan bit. Rethink this.\n" [^{:tag (* g)} gp ^uint32 old ^uint32 new]
  (when (or (!= old _Grunning) (!= new (bit-or _Gscan _Gpreempted)))
    (throw "bad g transition"))

  (acquireLockRankAndM lockRankGscan)
  (while (not (.CompareAndSwap (.-atomicstatus gp) _Grunning (bit-or _Gscan _Gpreempted)))))

;; We never notify gp.bubble that the goroutine state has moved
;; from _Grunning to _Gpreempted. We call bubble.changegstatus
;; after status changes happen, but doing so here would violate the
;; ordering between the gscan and synctest locks. The bubble doesn't
;; distinguish between _Grunning and _Gpreempted anyway, so not
;; notifying it is fine.





^{:go/breaks [8] :go/end 1463} (go/func casGFromPreempted "casGFromPreempted attempts to transition gp from _Gpreempted to\n_Gwaiting. If successful, the caller is responsible for\nre-scheduling gp.\n" ^bool [^{:tag (* g)} gp ^uint32 old ^uint32 new]
  (when (or (!= old _Gpreempted) (!= new _Gwaiting))
    (throw "bad g transition"))

  (set! (.-waitreason gp) waitReasonPreempted)
  (when (not (.CompareAndSwap (.-atomicstatus gp) _Gpreempted _Gwaiting))
    (return false))

  (when [bubble (.-bubble gp)] (!= bubble nil)
    (.changegstatus bubble gp _Gpreempted _Gwaiting))

  true)



(go/type stwReason "stwReason is an enumeration of reasons the world is stopping.\n" uint8)




(go/const "Reasons to stop-the-world.\n\nAvoid reusing reasons and add new ones instead.\n"
  ^{:line 1472} [^{:tag stwReason :val 0} stwUnknown iota] ; "unknown"
  ^{:line 1473} [^{:val 1} stwGCMarkTerm] ; "GC mark termination"
  ^{:line 1474} [^{:val 2} stwGCSweepTerm] ; "GC sweep termination"
  ^{:line 1475} [^{:val 3} stwWriteHeapDump] ; "write heap dump"
  ^{:line 1476} [^{:val 4} stwGoroutineProfile] ; "goroutine profile"
  ^{:line 1477} [^{:val 5} stwGoroutineProfileCleanup] ; "goroutine profile cleanup"
  ^{:line 1478} [^{:val 6} stwAllGoroutinesStack] ; "all goroutines stack trace"
  ^{:line 1479} [^{:val 7} stwReadMemStats] ; "read mem stats"
  ^{:line 1480} [^{:val 8} stwAllThreadsSyscall] ; "AllThreadsSyscall"
  ^{:line 1481} [^{:val 9} stwGOMAXPROCS] ; "GOMAXPROCS"
  ^{:line 1482} [^{:val 10} stwStartTrace] ; "start trace"
  ^{:line 1483} [^{:val 11} stwStopTrace] ; "stop trace"
  ^{:line 1484} [^{:val 12} stwForTestCountPagesInUse] ; "CountPagesInUse (test)"
  ^{:line 1485} [^{:val 13} stwForTestReadMetricsSlow] ; "ReadMetricsSlow (test)"
  ^{:line 1486} [^{:val 14} stwForTestReadMemStatsSlow] ; "ReadMemStatsSlow (test)"
  ^{:line 1487} [^{:val 15} stwForTestPageCachePagesLeaked] ; "PageCachePagesLeaked (test)"
  ^{:line 1488} [^{:val 16} stwForTestResetDebugLog]) ; "ResetDebugLog (test)"


^{:go/end 1493} (go/method String ^string [^stwReason r]
  (aget stwReasonStrings r))


^{:go/end 1497} (go/method isGC ^bool [^stwReason r]
  (or (== r stwGCMarkTerm) (== r stwGCSweepTerm)))





(go/var ^{:doc "If you add to this list, also add it to src/internal/trace/parser.go.\nIf you change the values of any of the stw* constants, bump the trace\nversion number and make a copy of this.\n"} stwReasonStrings (lit (array ... string)
    ^{:line 1503} [stwUnknown "unknown"]
    ^{:line 1504} [stwGCMarkTerm "GC mark termination"]
    ^{:line 1505} [stwGCSweepTerm "GC sweep termination"]
    ^{:line 1506} [stwWriteHeapDump "write heap dump"]
    ^{:line 1507} [stwGoroutineProfile "goroutine profile"]
    ^{:line 1508} [stwGoroutineProfileCleanup "goroutine profile cleanup"]
    ^{:line 1509} [stwAllGoroutinesStack "all goroutines stack trace"]
    ^{:line 1510} [stwReadMemStats "read mem stats"]
    ^{:line 1511} [stwAllThreadsSyscall "AllThreadsSyscall"]
    ^{:line 1512} [stwGOMAXPROCS "GOMAXPROCS"]
    ^{:line 1513} [stwStartTrace "start trace"]
    ^{:line 1514} [stwStopTrace "stop trace"]
    ^{:line 1515} [stwForTestCountPagesInUse "CountPagesInUse (test)"]
    ^{:line 1516} [stwForTestReadMetricsSlow "ReadMetricsSlow (test)"]
    ^{:line 1517} [stwForTestReadMemStatsSlow "ReadMemStatsSlow (test)"]
    ^{:line 1518} [stwForTestPageCachePagesLeaked "PageCachePagesLeaked (test)"]
    ^{:line 1519} [stwForTestResetDebugLog "ResetDebugLog (test)"]))




(go/type worldStop "worldStop provides context from the stop-the-world required by the\nstart-the-world.\n" (struct
    ^{:line 1525 :tag stwReason} reason
    ^{:line 1526 :tag int64} startedStopping
    ^{:line 1527 :tag int64} finishedStopping
    ^{:line 1528 :tag int64} stoppingCPUTime))





(go/var ^{:tag worldStop :doc "Temporary variable for stopTheWorld, when it can't write to the stack.\n\nProtected by worldsema.\n"} stopTheWorldContext)


















^{:go/end 1561} (go/func stopTheWorld "stopTheWorld stops all P's from executing goroutines, interrupting\nall goroutines at GC safe points and records reason as the reason\nfor the stop. On return, only the current goroutine's P is running.\nstopTheWorld must not be called from a system stack and the caller\nmust not hold worldsema. The caller must call startTheWorld when\nother P's should resume execution.\n\nstopTheWorld is safe for multiple goroutines to call at the\nsame time. Each will execute its own stop, and the stops will\nbe serialized.\n\nThis is also used by routines that do stack dumps. If the system is\nin panic or being exited, this may not reliably stop all\ngoroutines.\n\nReturns the STW context. When starting the world, this context must be\npassed to startTheWorld.\n" ^worldStop [^stwReason reason]
  (semacquire (addr worldsema))
  (let [gp (getg)]
    (set! (.-preemptoff (.-m gp)) (.String reason))
    (systemstack ^{:go/end 1559} (fn []
        (set! stopTheWorldContext (stopTheWorldWithSema reason)))) ; avoid write to stack

    ^{:line 1560} stopTheWorldContext))





^{:go/end 1588} (go/func startTheWorld "startTheWorld undoes the effects of stopTheWorld.\n\nw must be the worldStop returned by stopTheWorld.\n" [^worldStop w]
  (systemstack ^{:go/end 1567} (fn [] (startTheWorldWithSema 0 w)))

  ;; worldsema must be held over startTheWorldWithSema to ensure
  ;; gomaxprocs cannot change while worldsema is held.
  ;;
  ;; Release worldsema with direct handoff to the next waiter, but
  ;; acquirem so that semrelease1 doesn't try to yield our time.
  ;;
  ;; Otherwise if e.g. ReadMemStats is being called in a loop,
  ;; it might stomp on other attempts to stop the world, such as
  ;; for starting or ending GC. The operation this blocks is
  ;; so heavy-weight that we should just try to be as fair as
  ;; possible here.
  ;;
  ;; We don't want to just allow us to get preempted between now
  ;; and releasing the semaphore because then we keep everyone
  ;; (including, for example, GCs) waiting longer.
  (let [mp (acquirem)]
    (set! (.-preemptoff mp) "")
    (semrelease1 (addr worldsema) true 0)
    (releasem mp)))





^{:go/end 1596} (go/func stopTheWorldGC "stopTheWorldGC has the same effect as stopTheWorld, but blocks\nuntil the GC is not running. It also blocks a GC from starting\nuntil startTheWorldGC is called.\n" ^worldStop [^stwReason reason]
  (semacquire (addr gcsema))
  (stopTheWorld reason))





^{:go/end 1604} (go/func startTheWorldGC "startTheWorldGC undoes the effects of stopTheWorldGC.\n\nw must be the worldStop returned by stopTheWorld.\n" [^worldStop w]
  (startTheWorld w)
  (semrelease (addr gcsema)))



(go/var ^{:tag uint32 :doc "Holding worldsema grants an M the right to try to stop the world.\n"} worldsema 1)







(go/var ^{:tag uint32 :doc "Holding gcsema grants the M the right to block a GC, and blocks\nuntil the current GC is done. In particular, it prevents gomaxprocs\nfrom changing concurrently.\n\nTODO(mknyszek): Once gomaxprocs and the execution tracer can handle\nbeing changed/enabled during a GC, remove this.\n"} gcsema 1)

































^{:go/end 1773} (go/func ^:go/systemstack stopTheWorldWithSema "stopTheWorldWithSema is the core implementation of stopTheWorld.\nThe caller is responsible for acquiring worldsema and disabling\npreemption first and then should stopTheWorldWithSema on the system\nstack:\n\n\tsemacquire(&worldsema, 0)\n\tm.preemptoff = \"reason\"\n\tvar stw worldStop\n\tsystemstack(func() {\n\t\tstw = stopTheWorldWithSema(reason)\n\t})\n\nWhen finished, the caller must either call startTheWorld or undo\nthese three operations separately:\n\n\tm.preemptoff = \"\"\n\tsystemstack(func() {\n\t\tnow = startTheWorldWithSema(stw)\n\t})\n\tsemrelease(&worldsema)\n\nIt is allowed to acquire worldsema once and then execute multiple\nstartTheWorldWithSema/stopTheWorldWithSema pairs.\nOther P's are able to execute between successive calls to\nstartTheWorldWithSema and stopTheWorldWithSema.\nHolding worldsema causes any other goroutines invoking\nstopTheWorld to block.\n\nReturns the STW context. When starting the world, this context must be\npassed to startTheWorldWithSema.\n" ^worldStop [^stwReason reason]
  ;; Mark the goroutine which called stopTheWorld preemptible so its
  ;; stack may be scanned by the GC or observed by the execution tracer.
  ;;
  ;; This lets a mark worker scan us or the execution tracer take our
  ;; stack while we try to stop the world since otherwise we could get
  ;; in a mutual preemption deadlock.
  ;;
  ;; casGToWaitingForSuspendG marks the goroutine as ineligible for a
  ;; stack shrink, effectively pinning the stack in memory for the duration.
  ;;
  ;; N.B. The execution tracer is not aware of this status transition and
  ;; handles it specially based on the wait reason.
  (casGToWaitingForSuspendG (.-curg (.-m (getg))) _Grunning waitReasonStoppingTheWorld)

  (let [trace (traceAcquire)]
    (when (.ok trace)
      (.STWStart trace reason)
      (traceRelease trace))

    (let [gp (getg)]

      ;; If we hold a lock, then we won't be able to stop another M
      ;; that is blocked trying to acquire the lock.
      (when (> (.-locks (.-m gp)) 0)
        (throw "stopTheWorld: holding locks"))


      (lock (addr (.-lock sched)))
      (let [start (nanotime)] ; exclude time waiting for sched.lock from start and total time metrics.
        (set! (.-stopwait sched) gomaxprocs)
        (.Store (.-gcwaiting sched) true)
        (preemptall)

        ;; Stop current P.
        (set! (.-status (.ptr (.-p (.-m gp)))) _Pgcstop) ; Pgcstop is only diagnostic.
        (set! (.-gcStopTime (.ptr (.-p (.-m gp)))) start)
        (dec! (.-stopwait sched))

        ;; Try to retake all P's in syscalls.
        (range [_ pp allp]
          (when [(values thread ok) (setBlockOnExitSyscall pp)] ok
            (.gcstopP thread)
            (.resume thread)))



        ;; Stop idle Ps.
        (let [now (nanotime)]
          (while true
            (let [(values pp _) (pidleget now)]
              (when (== pp nil)
                (break))

              (set! (.-status pp) _Pgcstop)
              (set! (.-gcStopTime pp) (nanotime))
              (dec! (.-stopwait sched))))

          (let [wait (> (.-stopwait sched) 0)]
            (unlock (addr (.-lock sched)))

            ;; Wait for remaining Ps to stop voluntarily.
            (when wait
              (while true
                ;; wait for 100us, then try to re-preempt in case of any races
                (when (notetsleep (addr (.-stopnote sched)) (* 100 1000))
                  (noteclear (addr (.-stopnote sched)))
                  (break))

                (preemptall)))



            (let [finish (nanotime)
                ^{:line 1723} startTime (- finish start)]
              (if (.isGC reason)
                (.record (.-stwStoppingTimeGC sched) startTime)

                (.record (.-stwStoppingTimeOther sched) startTime))


              ;; Double-check we actually stopped everything, and all the invariants hold.
              ;; Also accumulate all the time spent by each P in _Pgcstop up to the point
              ;; where everything was stopped. This will be accumulated into the total pause
              ;; CPU time by the caller.
              (let [stoppingCPUTime (conv int64 0)
                  ^{:line 1735} bad ""]
                (if (!= (.-stopwait sched) 0)
                  (set! bad "stopTheWorld: not stopped (stopwait != 0)")

                  (range [_ pp allp]
                    (when (!= (.-status pp) _Pgcstop)
                      (set! bad "stopTheWorld: not stopped (status != _Pgcstop)"))

                    (when (and (== (.-gcStopTime pp) 0) (== bad ""))
                      (set! bad "stopTheWorld: broken CPU time accounting"))

                    (set! stoppingCPUTime + (- finish (.-gcStopTime pp)))
                    (set! (.-gcStopTime pp) 0)))


                (when (.Load freezing)
                  ;; Some other thread is panicking. This can cause the
                  ;; sanity checks above to fail if the panic happens in
                  ;; the signal handler on a stopped thread. Either way,
                  ;; we should halt this thread.
                  (lock (addr deadlock))
                  (lock (addr deadlock)))

                (when (!= bad "")
                  (throw bad))


                (worldStopped)

                ;; Switch back to _Grunning, now that the world is stopped.
                (casgstatus (.-curg (.-m (getg))) _Gwaiting _Grunning)

                ^{:go/breaks [2 4 6 8]} (lit worldStop
                  :reason reason
                  :startedStopping start
                  :finishedStopping finish
                  :stoppingCPUTime stoppingCPUTime)))))))))









^{:go/end 1848} (go/func startTheWorldWithSema "reason is the same STW reason passed to stopTheWorld. start is the start\ntime returned by stopTheWorld.\n\nnow is the current time; prefer to pass 0 to capture a fresh timestamp.\n\nstattTheWorldWithSema returns now.\n" ^int64 [^int64 now ^worldStop w]
  (assertWorldStopped)

  (let [mp (acquirem)] ; disable preemption because it can be holding p in a local var
    (when (netpollinited)
      (let [(values list delta) (netpoll 0)] ; non-blocking
        (injectglist (addr list))
        (netpollAdjustWaiters delta)))

    (lock (addr (.-lock sched)))

    (let [procs gomaxprocs]
      (when (!= newprocs 0)
        (set! procs newprocs)
        (set! newprocs 0))

      (let [p1 (procresize procs)]
        (.Store (.-gcwaiting sched) false)
        (when (.Load (.-sysmonwait sched))
          (.Store (.-sysmonwait sched) false)
          (notewakeup (addr (.-sysmonnote sched))))

        (unlock (addr (.-lock sched)))

        (worldStarted)

        (while (!= p1 nil)
          (let [p p1]
            (set! p1 (.ptr (.-link p1)))
            (if (!= (.-m p) 0)
              (let [mp (.ptr (.-m p))]
                (set! (.-m p) 0)
                (when (!= (.-nextp mp) 0)
                  (throw "startTheWorld: inconsistent mp->nextp"))

                (.set (.-nextp mp) p)
                (notewakeup (addr (.-park mp))))

              ;; Start M to run P.  Do not start another M below.
              (newm nil p -1))))



        ;; Capture start-the-world time before doing clean-up tasks.
        (when (== now 0)
          (set! now (nanotime)))

        (let [totalTime (- now (.-startedStopping w))]
          (if (.isGC (.-reason w))
            (.record (.-stwTotalTimeGC sched) totalTime)

            (.record (.-stwTotalTimeOther sched) totalTime))

          (let [trace (traceAcquire)]
            (when (.ok trace)
              (.STWDone trace)
              (traceRelease trace))


            ;; Wakeup an additional proc in case we have excessive runnable goroutines
            ;; in local queues or in the global queue. If we don't, the proc will park itself.
            ;; If we have lots of excessive work, resetspinning will unpark additional procs as necessary.
            (wakep)

            (releasem mp)

            ^{:line 1847} now))))))




^{:go/breaks [5] :go/end 1858} (go/func usesLibcall "usesLibcall indicates whether this runtime performs system calls\nvia libcall.\n" ^bool []
  (switch GOOS
    (case ["aix" "darwin" "illumos" "ios" "openbsd" "solaris" "windows"]
      (return true)))

  false)




^{:go/breaks [5] :go/end 1868} (go/func mStackIsSystemAllocated "mStackIsSystemAllocated indicates whether this runtime starts on a\nsystem-allocated stack.\n" ^bool []
  (switch GOOS
    (case ["aix" "darwin" "plan9" "illumos" "ios" "openbsd" "solaris" "windows"]
      (return true)))

  false)




(go/func ^:extern mstart "mstart is the entry-point for new Ms.\nIt is written in assembly, uses ABI0, is marked TOPFRAME, and calls mstart0.\n" [])










^{:go/end 1919} (go/func ^:go/nosplit ^:go/nowritebarrierrec mstart0 "mstart0 is the Go entry-point for new Ms.\nThis must not split the stack because we may not even have stack\nbounds set up yet.\n\nMay run during STW (because it doesn't have a P yet), so write\nbarriers are not allowed.\n" []
  (let [gp (getg)

      ^{:line 1886} osStack (== (.-lo (.-stack gp)) 0)]
    (when osStack
      ;; Initialize stack bounds from system stack.
      ;; Cgo may have left stack size in stack.hi.
      ;; minit may update the stack bounds.
      ;;
      ;; Note: these bounds may not be very accurate.
      ;; We set hi to &size, but there are things above
      ;; it. The 1024 is supposed to compensate this,
      ;; but is somewhat arbitrary.
      (let [size (.-hi (.-stack gp))]
        (when (== size 0)
          (set! size (* 16384 sys/StackGuardMultiplier)))

        (set! (.-hi (.-stack gp)) (conv uintptr (noescape (conv unsafe/Pointer (addr size)))))
        (set! (.-lo (.-stack gp)) (+ (- (.-hi (.-stack gp)) size) 1024))))

    ;; Initialize stack guard so that we can start calling regular
    ;; Go code.
    (set! (.-stackguard0 gp) (+ (.-lo (.-stack gp)) stackGuard))
    ;; This is the g0, so we can also call go:systemstack
    ;; functions, which check stackguard1.
    (set! (.-stackguard1 gp) (.-stackguard0 gp))
    (mstart1)

    ;; Exit this thread.
    (when (mStackIsSystemAllocated)
      ;; Windows, Solaris, illumos, Darwin, AIX and Plan 9 always system-allocate
      ;; the stack, but put it in gp.stack before mstart,
      ;; so the logic above hasn't set osStack yet.
      (set! osStack true))

    (mexit osStack)))






^{:go/end 1964} (go/func ^:go/noinline mstart1 "The go:noinline is to guarantee the sys.GetCallerPC/sys.GetCallerSP below are safe,\nso that we can set up g0.sched to return to the call of mstart1 above.\n" []
  (let [gp (getg)]

    (when (!= gp (.-g0 (.-m gp)))
      (throw "bad runtime·mstart"))


    ;; Set up m.g0.sched as a label returning to just
    ;; after the mstart1 call in mstart0 above, for use by goexit0 and mcall.
    ;; We're never coming back to mstart1 after we call schedule,
    ;; so other calls can reuse the current frame.
    ;; And goexit0 does a gogo that needs to return from mstart1
    ;; and let mstart0 exit the thread.
    (set! (.-g (.-sched gp)) (conv guintptr (conv unsafe/Pointer gp)))
    (set! (.-pc (.-sched gp)) (sys/GetCallerPC))
    (set! (.-sp (.-sched gp)) (sys/GetCallerSP))

    (asminit)
    (minit)

    ;; Install signal handlers; after minit so that minit can
    ;; prepare the thread to be able to handle the signals.
    (when (== (.-m gp) (addr m0))
      (mstartm0))


    (when (== (.-dataindependenttiming debug) 1)
      (sys/EnableDIT))


    (when [fn (.-mstartfn (.-m gp))] (!= fn nil)
      (go/call fn))


    (when (!= (.-m gp) (addr m0))
      (acquirep (.ptr (.-nextp (.-m gp))))
      (set! (.-nextp (.-m gp)) 0))

    (schedule)))








^{:go/end 1981} (go/func ^:go/yeswritebarrierrec mstartm0 "mstartm0 implements part of mstart1 that only runs on the m0.\n\nWrite barriers are allowed here because we know the GC can't be\nrunning yet, so they'll be no-ops.\n" []
  ;; Create an extra M for callbacks on threads not created by Go.
  ;; An extra M is also needed on Windows for callbacks created by
  ;; syscall.NewCallback. See issue #6751 for details.
  (when (and ^:go/paren (or iscgo (== GOOS "windows")) (not cgoHasExtraM))
    (set! cgoHasExtraM true)
    (newextram))

  (initsig false))





^{:go/end 1995} (go/func ^:go/nosplit mPark "mPark causes a thread to park itself, returning once woken.\n" []
  (let [gp (getg)]
    ;; This M might stay parked through an entire GC cycle.
    ;; Erase any leftovers on the signal stack.
    (when goexperiment/RuntimeSecret
      (eraseSecretsSignalStk))

    (notesleep (addr (.-park (.-m gp))))
    (noteclear (addr (.-park (.-m gp))))))












^{:go/end 2125} (go/func ^:go/yeswritebarrierrec mexit "mexit tears down and exits the current thread.\n\nDon't call this directly to exit the thread, since it must run at\nthe top of the thread stack. Instead, use gogo(&gp.m.g0.sched) to\nunwind the stack to the point that exits the thread.\n\nIt is entered with m.p != nil, so write barriers are allowed. It\nwill release the P before exiting.\n" [^bool osStack]
  (let [mp (.-m (getg))]

    (when (== mp (addr m0))
      ;; This is the main thread. Just wedge it.
      ;;
      ;; On Linux, exiting the main thread puts the process
      ;; into a non-waitable zombie state. On Plan 9,
      ;; exiting the main thread unblocks wait even though
      ;; other threads are still running. On Solaris we can
      ;; neither exitThread nor return from mstart. Other
      ;; bad things probably happen on other platforms.
      ;;
      ;; We could try to clean up this M more before wedging
      ;; it, but that complicates signal handling.
      (handoffp (releasep))
      (lock (addr (.-lock sched)))
      (inc! (.-nmfreed sched))
      (checkdead)
      (unlock (addr (.-lock sched)))
      (mPark)
      (throw "locked m0 woke up"))


    (sigblock true)
    (unminit)

    ;; Free the gsignal stack.
    (when (!= (.-gsignal mp) nil)
      (stackfree (.-stack (.-gsignal mp)))
      (when valgrindenabled
        (valgrindDeregisterStack (.-valgrindStackID (.-gsignal mp)))
        (set! (.-valgrindStackID (.-gsignal mp)) 0))

      ;; On some platforms, when calling into VDSO (e.g. nanotime)
      ;; we store our g on the gsignal stack, if there is one.
      ;; Now the stack is freed, unlink it from the m, so we
      ;; won't write to it when calling VDSO code.
      (set! (.-gsignal mp) nil))


    ;; Free vgetrandom state.
    (vgetrandomDestroy mp)

    ;; Clear the self pointer so Ps don't access this M after it is freed,
    ;; or keep it alive.
    (.clear (.-self mp))

    ;; Remove m from allm.
    (lock (addr (.-lock sched)))
    (for [pprev (addr allm)] (!= @pprev nil) (set! pprev (addr (.-alllink @pprev)))
      (when (== @pprev mp)
        (set! @pprev (.-alllink mp))
        (goto :found)))


    (throw "m not found in allm")
    (label :found
      ;; Events must not be traced after this point.

      ;; Delay reaping m until it's done with the stack.
      ;;
      ;; Put mp on the free list, though it will not be reaped while freeWait
      ;; is freeMWait. mp is no longer reachable via allm, so even if it is
      ;; on an OS stack, we must keep a reference to mp alive so that the GC
      ;; doesn't free mp while we are still using it.
      ;;
      ;; Note that the free list must not be linked through alllink because
      ;; some functions walk allm without locking, so may be using alllink.
      ;;
      ;; N.B. It's important that the M appears on the free list simultaneously
      ;; with it being removed so that the tracer can find it.
      (.Store (.-freeWait mp) freeMWait))
    (set! (.-freelink mp) (.-freem sched))
    (set! (.-freem sched) mp)
    (unlock (addr (.-lock sched)))

    (atomic/Xadd64 (addr ncgocall) (conv int64 (.-ncgocall mp)))
    (.Add (.-totalRuntimeLockWaitTime sched) (.Load (.-waitTime (.-mLockProfile mp))))

    ;; Release the P.
    (handoffp (releasep))
    ;; After this point we must not have write barriers.

    ;; Invoke the deadlock detector. This must happen after
    ;; handoffp because it may have started a new M to take our
    ;; P's work.
    (lock (addr (.-lock sched)))
    (inc! (.-nmfreed sched))
    (checkdead)
    (unlock (addr (.-lock sched)))

    (when (or (== GOOS "darwin") (== GOOS "ios"))
      ;; Make sure pendingPreemptSignals is correct when an M exits.
      ;; For #41702.
      (when (!= (.Load (.-signalPending mp)) 0)
        (.Add pendingPreemptSignals -1)))



    ;; Destroy all allocated resources. After this is called, we may no
    ;; longer take any locks.
    (mdestroy mp)

    (when osStack
      ;; No more uses of mp, so it is safe to drop the reference.
      (.Store (.-freeWait mp) freeMRef)

      ;; Return from mstart and let the system thread
      ;; library free the g0 stack and terminate the thread.
      (return))


    ;; mstart is the thread's entry point, so there's nothing to
    ;; return to. Exit the thread directly. exitThread will clear
    ;; m.freeWait when it's done with the stack and the m can be
    ;; reaped.
    (exitThread (addr (.-freeWait mp)))))












^{:go/end 2155} (go/func forEachP "forEachP calls fn(p) for every P p when p reaches a GC safe point.\nIf a P is currently executing code, this will bring the P to a GC\nsafe point and execute fn on that P. If the P is not executing code\n(it is idle or in a syscall), this will call fn(p) directly while\npreventing the P from exiting its state. This does not ensure that\nfn will run on every CPU executing Go code, but it acts as a global\nmemory barrier. GC uses this as a \"ragged barrier.\"\n\nThe caller must hold worldsema. fn must not refer to any\npart of the current goroutine's stack, since the GC may move it.\n" [^waitReason reason ^{:tag (func [(* p)])} fn]
  (systemstack ^{:go/end 2154} (fn []
      (let [gp (.-curg (.-m (getg)))]
        ;; Mark the user stack as preemptible so that it may be scanned
        ;; by the GC or observed by the execution tracer. Otherwise, our
        ;; attempt to force all P's to a safepoint could result in a
        ;; deadlock as we attempt to preempt a goroutine that's trying
        ;; to preempt us (e.g. for a stack scan).
        ;;
        ;; casGToWaitingForSuspendG marks the goroutine as ineligible for a
        ;; stack shrink, effectively pinning the stack in memory for the duration.
        ;;
        ;; N.B. The execution tracer is not aware of this status transition and
        ;; handles it specially based on the wait reason.
        (casGToWaitingForSuspendG gp _Grunning reason)
        (forEachPInternal fn)
        (casgstatus gp _Gwaiting _Grunning)))))












^{:go/end 2245} (go/func ^:go/systemstack forEachPInternal "forEachPInternal calls fn(p) for every P p when p reaches a GC safe point.\nIt is the internal implementation of forEachP.\n\nThe caller must hold worldsema and either must ensure that a GC is not\nrunning (otherwise this may deadlock with the GC trying to preempt this P)\nor it must leave its goroutine in a preemptible state before it switches\nto the systemstack. Due to these restrictions, prefer forEachP when possible.\n" [^{:tag (func [(* p)])} fn]
  (let [mp (acquirem)
      ^{:line 2168} pp (.ptr (.-p (.-m (getg))))]

    (lock (addr (.-lock sched)))
    (when (!= (.-safePointWait sched) 0)
      (throw "forEachP: sched.safePointWait != 0"))

    (set! (.-safePointWait sched) (- gomaxprocs 1))
    (set! (.-safePointFn sched) fn)

    ;; Ask all Ps to run the safe point function.
    (range [_ p2 allp]
      (when (!= p2 pp)
        (atomic/Store (addr (.-runSafePointFn p2)) 1)))


    (preemptall)

    ;; Any P entering _Pidle or a system call from now on will observe
    ;; p.runSafePointFn == 1 and will call runSafePointFn when
    ;; changing its status to _Pidle.

    ;; Run safe point function for all idle Ps. sched.pidle will
    ;; not change because we hold sched.lock.
    (for [p (.ptr (.-pidle sched))] (!= p nil) (set! p (.ptr (.-link p)))
      (when (atomic/Cas (addr (.-runSafePointFn p)) 1 0)
        (go/call fn p)
        (dec! (.-safePointWait sched))))



    (let [wait (> (.-safePointWait sched) 0)]
      (unlock (addr (.-lock sched)))

      ;; Run fn for the current P.
      (go/call fn pp)

      ;; Force Ps currently in a system call into _Pidle and hand them
      ;; off to induce safe point function execution.
      (range [_ p2 allp]
        (when (!= (atomic/Load (addr (.-runSafePointFn p2))) 1)
          ;; Already ran it.
          (continue))

        (when [(values thread ok) (setBlockOnExitSyscall p2)] ok
          (.takeP thread)
          (.resume thread)
          (handoffp p2)))



      ;; Wait for remaining Ps to run fn.
      (when wait
        (while true
          ;; Wait for 100us, then try to re-preempt in
          ;; case of any races.
          ;;
          ;; Requires system stack.
          (when (notetsleep (addr (.-safePointNote sched)) (* 100 1000))
            (noteclear (addr (.-safePointNote sched)))
            (break))

          (preemptall)))


      (when (!= (.-safePointWait sched) 0)
        (throw "forEachP: not done"))

      (range [_ p2 allp]
        (when (!= (.-runSafePointFn p2) 0)
          (throw "forEachP: P did not run fn")))



      (lock (addr (.-lock sched)))
      (set! (.-safePointFn sched) nil)
      (unlock (addr (.-lock sched)))
      (releasem mp))))













^{:go/end 2273} (go/func runSafePointFn "runSafePointFn runs the safe point function, if any, for this P.\nThis should be called like\n\n\tif getg().m.p.runSafePointFn != 0 {\n\t    runSafePointFn()\n\t}\n\nrunSafePointFn must be checked on any transition in to _Pidle or\nwhen entering a system call to avoid a race where forEachP sees\nthat the P is running just before the P goes into _Pidle/system call\nand neither forEachP nor the P run the safe-point function.\n" []
  (let [p (.ptr (.-p (.-m (getg))))]
    ;; Resolve the race between forEachP running the safe-point
    ;; function on this P's behalf and this P running the
    ;; safe-point function directly.
    (when (not (atomic/Cas (addr (.-runSafePointFn p)) 1 0))
      (return))

    ((.-safePointFn sched) p)
    (lock (addr (.-lock sched)))
    (dec! (.-safePointWait sched))
    (when (== (.-safePointWait sched) 0)
      (notewakeup (addr (.-safePointNote sched))))

    (unlock (addr (.-lock sched)))))





(go/var ^{:tag unsafe/Pointer :doc "When running with cgo, we call _cgo_thread_start\nto start threads for us so that we can play nicely with\nforeign code.\n"} cgoThreadStart)

(go/type cgothreadstart (struct
    ^{:line 2281 :tag guintptr} g
    ^{:line 2282 :tag (* uint64)} tls
    ^{:line 2283 :tag unsafe/Pointer} fn))











^{:go/end 2370} (go/func ^:go/yeswritebarrierrec allocm "Allocate a new m unassociated with any thread.\nCan use p for allocation context if needed.\nfn is recorded as the new m's m.mstartfn.\nid is optional pre-allocated m ID. Omit by passing -1.\n\nThis function is allowed to have write barriers even if the caller\nisn't because it borrows pp.\n" ^{:tag (* m)} [^{:tag (* p)} pp ^{:tag (func [])} fn ^int64 id]
  (.rlock allocmLock)

  ;; The caller owns pp, but we may borrow (i.e., acquirep) it. We must
  ;; disable preemption to ensure it is not stolen, which would make the
  ;; caller lose ownership.
  (acquirem)

  (let [gp (getg)]
    (when (== (.-p (.-m gp)) 0)
      (acquirep pp)) ; temporarily borrow p for mallocs in this function


    ;; Release the free M list. We need to do this somewhere and
    ;; this may free up a stack we can use.
    (when (!= (.-freem sched) nil)
      (lock (addr (.-lock sched)))
      (let [^{:tag (* m)} newList (zero (* m))]
        (for [freem (.-freem sched)] (!= freem nil) _
          ;; Wait for freeWait to indicate that freem's stack is unused.
          (let [wait (.Load (.-freeWait freem))]
            (when (== wait freeMWait)
              (let [next (.-freelink freem)]
                (set! (.-freelink freem) newList)
                (set! newList freem)
                (set! freem next)
                (continue)))

            ;; Drop any remaining trace resources.
            ;; Ms can continue to emit events all the way until wait != freeMWait,
            ;; so it's only safe to call traceThreadDestroy at this point.
            (when (or (traceEnabled) (traceShuttingDown))
              (traceThreadDestroy freem))

            ;; Free the stack if needed. For freeMRef, there is
            ;; nothing to do except drop freem from the sched.freem
            ;; list.
            (when (== wait freeMStack)
              ;; stackfree must be on the system stack, but allocm is
              ;; reachable off the system stack transitively from
              ;; startm.
              (systemstack ^{:go/end 2342} (fn []
                  (stackfree (.-stack (.-g0 freem)))
                  (when valgrindenabled
                    (valgrindDeregisterStack (.-valgrindStackID (.-g0 freem)))
                    (set! (.-valgrindStackID (.-g0 freem)) 0)))))



            (set! freem (.-freelink freem))))

        (set! (.-freem sched) newList)
        (unlock (addr (.-lock sched)))))


    (let [mp (addr (.-m (new mPadded)))]
      (set! (.-mstartfn mp) fn)
      (mcommoninit mp id)

      ;; In case of cgo or Solaris or illumos or Darwin, pthread_create will make us a stack.
      ;; Windows and Plan 9 will layout sched stack on OS stack.
      (if (or iscgo (mStackIsSystemAllocated))
        (set! (.-g0 mp) (malg -1))

        (set! (.-g0 mp) (malg (* 16384 sys/StackGuardMultiplier))))

      (set! (.-m (.-g0 mp)) mp)

      (when (== pp (.ptr (.-p (.-m gp))))
        (releasep))


      (releasem (.-m gp))
      (.runlock allocmLock)
      ^{:line 2369} mp)))









































^{:go/end 2502} (go/func ^:go/nosplit needm "needm is called when a cgo callback happens on a\nthread without an m (a thread not created by Go).\nIn this case, needm is expected to find an m to use\nand return with m, g initialized correctly.\nSince m and g are not set now (likely nil, but see below)\nneedm is limited in what routines it can call. In particular\nit can only call nosplit functions (textflag 7) and cannot\ndo any scheduling that requires an m.\n\nIn order to avoid needing heavy lifting here, we adopt\nthe following strategy: there is a stack of available m's\nthat can be stolen. Using compare-and-swap\nto pop from the stack has ABA races, so we simulate\na lock by doing an exchange (via Casuintptr) to steal the stack\nhead and replace the top pointer with MLOCKED (1).\nThis serves as a simple spin lock that we can use even\nwithout an m. The thread that locks the stack in this way\nunlocks the stack by storing a valid stack head pointer.\n\nIn order to make sure that there is always an m structure\navailable to be stolen, we maintain the invariant that there\nis always one more than needed. At the beginning of the\nprogram (if cgo is in use) the list is seeded with a single m.\nIf needm finds that it has taken the last m off the list, its job\nis - once it has installed its own m so that it can do things like\nallocate memory - to create a spare m and put it on the list.\n\nEach of these extra m's also has a g0 and a curg that are\npressed into service as the scheduling stack and current\ngoroutine for the duration of the cgo callback.\n\nIt calls dropm to put the m back on the list,\n1. when the callback is done with the m in non-pthread platforms,\n2. or when the C thread exiting on pthread platforms.\n\nThe signal argument indicates whether we're called from a signal\nhandler.\n" [^bool signal]
  (when (and ^:go/paren (or iscgo (== GOOS "windows")) (not cgoHasExtraM))
    ;; Can happen if C/C++ code calls Go from a global ctor.
    ;; Can also happen on Windows if a global ctor uses a
    ;; callback created by syscall.NewCallback. See issue #6751
    ;; for details.
    ;;
    ;; Can not throw, because scheduler is not initialized yet.
    (writeErrStr "fatal error: cgo callback before cgo call\n")
    (exit 1))


  ;; Save and block signals before getting an M.
  ;; The signal handler may call needm itself,
  ;; and we must avoid a deadlock. Also, once g is installed,
  ;; any incoming signals will try to execute,
  ;; but we won't have the sigaltstack settings and other data
  ;; set up appropriately until the end of minit, which will
  ;; unblock the signals. This is the same dance as when
  ;; starting a new m to run Go code via newosproc.
  (let [^sigset sigmask (zero sigset)]
    (sigsave (addr sigmask))
    (sigblock false)

    ;; getExtraM is safe here because of the invariant above,
    ;; that the extra list always contains or will soon contain
    ;; at least one m.
    (let [(values mp last) (getExtraM)]

      ;; Set needextram when we've just emptied the list,
      ;; so that the eventual call into cgocallbackg will
      ;; allocate a new m for the extra list. We delay the
      ;; allocation until then so that it can be done
      ;; after exitsyscall makes sure it is okay to be
      ;; running at all (that is, there's no garbage collection
      ;; running right now).
      (set! (.-needextram mp) last)

      ;; Store the original signal mask for use by minit.
      (set! (.-sigmask mp) sigmask)

      ;; Install TLS on some platforms (previously setg
      ;; would do this if necessary).
      (osSetupTLS mp)

      ;; Install g (= m->g0) and set the stack bounds
      ;; to match the current stack.
      (setg (.-g0 mp))
      (let [sp (sys/GetCallerSP)]
        (callbackUpdateSystemStack mp sp signal)

        ;; We must mark that we are already in Go now.
        ;; Otherwise, we may call needm again when we get a signal, before cgocallbackg1,
        ;; which means the extram list may be empty, that will cause a deadlock.
        (set! (.-isExtraInC mp) false)

        ;; Initialize this thread to use the m.
        (asminit)
        (minit)

        ;; Emit a trace event for this dead -> syscall transition,
        ;; but only if we're not in a signal handler.
        ;;
        ;; N.B. the tracer can run on a bare M just fine, we just have
        ;; to make sure to do this before setg(nil) and unminit.
        (let [^traceLocker trace (zero traceLocker)]
          (when (not signal)
            (set! trace (traceAcquire)))


          ;; mp.curg is now a real goroutine.
          (casgstatus (.-curg mp) _Gdeadextra _Gsyscall)
          (.Add (.-ngsys sched) -1)

          ;; This is technically inaccurate, but we set isExtraInC to false above,
          ;; and so we need to update addGSyscallNoP to keep the two pieces of state
          ;; consistent (it's only updated when isExtraInC is false). More specifically,
          ;; When we get to cgocallbackg and exitsyscall, we'll be looking for a P, and
          ;; since isExtraInC is false, we will decrement this metric.
          ;;
          ;; The inaccuracy is thankfully transient: only until this thread can get a P.
          ;; We're going into Go anyway, so it's okay to pretend we're a real goroutine now.
          (addGSyscallNoP mp)

          (when (not signal)
            (when (.ok trace)
              (.GoCreateSyscall trace (.-curg mp))
              (traceRelease trace)))


          (set! (.-isExtraInSig mp) signal))))))





^{:go/end 2513} (go/func ^:go/nosplit needAndBindM "Acquire an extra m and bind it to the C thread when a pthread key has been created.\n" []
  (needm false)

  (when (and (!= _cgo_pthread_key_created nil) (!= @(conv (* uintptr) _cgo_pthread_key_created) 0))
    (cgoBindM)))






^{:go/end 2528} (go/func newextram "newextram allocates m's and puts them on the extra list.\nIt is called with a working local m, so that it can do things\nlike call schedlock and allocate.\n" []
  (let [c (.Swap extraMWaiters 0)]
    (cond (> c 0)
      (for [i (conv uint32 0)] (< i c) (inc! i)
        (oneNewExtraM))

      (== (.Load extraMLength) 0)
      ;; Make sure there is at least one extra M.
      (oneNewExtraM))))




^{:go/end 2574} (go/func oneNewExtraM "oneNewExtraM allocates an m and puts it on the extra list.\n" []
  ;; Create extra goroutine locked to extra m.
  ;; The goroutine is the context in which the cgo callback will run.
  ;; The sched.pc will never be returned to, but setting it to
  ;; goexit makes clear to the traceback routines where
  ;; the goroutine stack ends.
  (let [mp (allocm nil nil -1)
      ^{:line 2538} gp (malg 4096)]
    (set! (.-pc (.-sched gp)) (+ (abi/FuncPCABI0 goexit) sys/PCQuantum))
    (set! (.-sp (.-sched gp)) (.-hi (.-stack gp)))
    (set! (.-sp (.-sched gp)) - (* 4 goarch/PtrSize)) ; extra space in case of reads slightly beyond frame
    (set! (.-lr (.-sched gp)) 0)
    (set! (.-g (.-sched gp)) (conv guintptr (conv unsafe/Pointer gp)))
    (set! (.-syscallpc gp) (.-pc (.-sched gp)))
    (set! (.-syscallsp gp) (.-sp (.-sched gp)))
    (set! (.-stktopsp gp) (.-sp (.-sched gp)))
    ;; malg returns status as _Gidle. Change to _Gdeadextra before
    ;; adding to allg where GC can see it. _Gdeadextra hides this
    ;; from traceback and stack scans.
    (casgstatus gp _Gidle _Gdeadextra)
    (set! (.-m gp) mp)
    (set! (.-curg mp) gp)
    (set! (.-isextra mp) true)
    ;; mark we are in C by default.
    (set! (.-isExtraInC mp) true)
    (inc! (.-lockedInt mp))
    (.set (.-lockedg mp) gp)
    (.set (.-lockedm gp) mp)
    (set! (.-goid gp) (.Add (.-goidgen sched) 1))
    (when raceenabled
      (set! (.-racectx gp) (racegostart (+ (abi/FuncPCABIInternal newextram) sys/PCQuantum))))

    ;; put on allg for garbage collector
    (allgadd gp)

    ;; gp is now on the allg list, but we don't want it to be
    ;; counted by gcount. It would be more "proper" to increment
    ;; sched.ngfree, but that requires locking. Incrementing ngsys
    ;; has the same effect.
    (.Add (.-ngsys sched) 1)

    ;; Add m to the extra list.
    (addExtraM mp)))



































^{:go/end 2695} (go/func ^:go/nosplit ^:go/nowritebarrierrec dropm "dropm puts the current m back onto the extra list.\n\n1. On systems without pthreads, like Windows\ndropm is called when a cgo callback has called needm but is now\ndone with the callback and returning back into the non-Go thread.\n\nThe main expense here is the call to signalstack to release the\nm's signal stack, and then the call to needm on the next callback\nfrom this thread. It is tempting to try to save the m for next time,\nwhich would eliminate both these costs, but there might not be\na next time: the current thread (which Go does not control) might exit.\nIf we saved the m for that thread, there would be an m leak each time\nsuch a thread exited. Instead, we acquire and release an m on each\ncall. These should typically not be scheduling operations, just a few\natomics, so the cost should be small.\n\n2. On systems with pthreads\ndropm is called while a non-Go thread is exiting.\nWe allocate a pthread per-thread variable using pthread_key_create,\nto register a thread-exit-time destructor.\nAnd store the g into a thread-specific value associated with the pthread key,\nwhen first return back to C.\nSo that the destructor would invoke dropm while the non-Go thread is exiting.\nThis is much faster since it avoids expensive signal-related syscalls.\n\nThis may run without a P, so //go:nowritebarrierrec is required.\n\nThis may run with a different stack than was recorded in g0 (there is no\ncall to callbackUpdateSystemStack prior to dropm), so this must be\n//go:nosplit to avoid the stack bounds check.\n" []
  ;; Clear m and g, and return m to the extra list.
  ;; After the call to setg we can only call nosplit functions
  ;; with no pointer manipulation.
  (let [mp (.-m (getg))

      ;; Emit a trace event for this syscall -> dead transition.
      ;;
      ;; N.B. the tracer can run on a bare M just fine, we just have
      ;; to make sure to do this before setg(nil) and unminit.
      ^{:line 2619 :tag traceLocker} trace (zero traceLocker)]
    (when (not (.-isExtraInSig mp))
      (set! trace (traceAcquire)))


    ;; Return mp.curg to _Gdeadextra state.
    (casgstatus (.-curg mp) _Gsyscall _Gdeadextra)
    (set! (.-preemptStop (.-curg mp)) false)
    (.Add (.-ngsys sched) 1)
    (decGSyscallNoP mp)

    (when (not (.-isExtraInSig mp))
      (when (.ok trace)
        (.GoDestroySyscall trace)
        (traceRelease trace)))



    ;; Trash syscalltick so that it doesn't line up with mp.old.syscalltick anymore.
    ;;
    ;; In the new tracer, we model needm and dropm and a goroutine being created and
    ;; destroyed respectively. The m then might get reused with a different procid but
    ;; still with a reference to oldp, and still with the same syscalltick. The next
    ;; time a G is "created" in needm, it'll return and quietly reacquire its P from a
    ;; different m with a different procid, which will confuse the trace parser. By
    ;; trashing syscalltick, we ensure that it'll appear as if we lost the P to the
    ;; tracer parser and that we just reacquired it.
    ;;
    ;; Trash the value by decrementing because that gets us as far away from the value
    ;; the syscall exit code expects as possible. Setting to zero is risky because
    ;; syscalltick could already be zero (and in fact, is initialized to zero).
    (dec! (.-syscalltick mp))

    ;; Reset trace state unconditionally. This goroutine is being 'destroyed'
    ;; from the perspective of the tracer.
    (.reset (.-trace (.-curg mp)))

    ;; Flush all the M's buffers. This is necessary because the M might
    ;; be used on a different thread with a different procid, so we have
    ;; to make sure we don't write into the same buffer.
    (when (or (traceEnabled) (traceShuttingDown))
      ;; Acquire sched.lock across thread destruction. One of the invariants of the tracer
      ;; is that a thread cannot disappear from the tracer's view (allm or freem) without
      ;; it noticing, so it requires that sched.lock be held over traceThreadDestroy.
      ;;
      ;; This isn't strictly necessary in this case, because this thread never leaves allm,
      ;; but the critical section is short and dropm is rare on pthread platforms, so just
      ;; take the lock and play it safe. traceThreadDestroy also asserts that the lock is held.
      (lock (addr (.-lock sched)))
      (traceThreadDestroy mp)
      (unlock (addr (.-lock sched))))

    (set! (.-isExtraInSig mp) false)

    ;; Block signals before unminit.
    ;; Unminit unregisters the signal handling stack (but needs g on some systems).
    ;; Setg(nil) clears g, which is the signal handler's cue not to run Go handlers.
    ;; It's important not to try to handle a signal between those two steps.
    (let [sigmask (.-sigmask mp)]
      (sigblock false)
      (unminit)

      (setg nil)

      ;; Clear g0 stack bounds to ensure that needm always refreshes the
      ;; bounds when reusing this M.
      (let [g0 (.-g0 mp)]
        (set! (.-hi (.-stack g0)) 0)
        (set! (.-lo (.-stack g0)) 0)
        (set! (.-stackguard0 g0) 0)
        (set! (.-stackguard1 g0) 0)
        (set! (.-g0StackAccurate mp) false)

        (putExtraM mp)

        (msigrestore sigmask)))))






















^{:go/end 2728} (go/func ^:go/nosplit ^:go/nowritebarrierrec cgoBindM "bindm store the g0 of the current m into a thread-specific value.\n\nWe allocate a pthread per-thread variable using pthread_key_create,\nto register a thread-exit-time destructor.\nWe are here setting the thread-specific value of the pthread key, to enable the destructor.\nSo that the pthread_key_destructor would dropm while the C thread is exiting.\n\nAnd the saved g will be used in pthread_key_destructor,\nsince the g stored in the TLS by Go might be cleared in some platforms,\nbefore the destructor invoked, so, we restore g by the stored g, before dropm.\n\nWe store g0 instead of m, to make the assembly code simpler,\nsince we need to restore g0 in runtime.cgocallback.\n\nOn systems without pthreads, like Windows, bindm shouldn't be used.\n\nNOTE: this always runs without a P, so, nowritebarrierrec required.\n" []
  (when (or (== GOOS "windows") (== GOOS "plan9"))
    (fatal "bindm in unexpected GOOS"))

  (let [g (getg)]
    (when (!= (.-g0 (.-m g)) g)
      (fatal "the current g is not g0"))

    (when (!= _cgo_bindm nil)
      (asmcgocall _cgo_bindm (conv unsafe/Pointer g)))))














^{:go/end 2743} (go/func ^{:go/linkname "getm"} getm "A helper function for EnsureDropM.\n\ngetm should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - fortio.org/log\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" ^uintptr []
  (conv uintptr (conv unsafe/Pointer (.-m (getg)))))


(go/var






  ^{:line 2752} [^{:tag atomic/Uintptr :doc "Locking linked list of extra M's, via mp.schedlink. Must be accessed\nonly via lockextra/unlockextra.\n\nCan't be atomic.Pointer[m] because we use an invalid pointer as a\n\"locked\" sentinel value. M's on this list remain visible to the GC\nbecause their mp.curg is on allgs.\n"} extraM]

  ^{:line 2754} [^{:tag atomic/Uint32 :doc "Number of M's in the extraM list.\n"} extraMLength]

  ^{:line 2756} [^{:tag atomic/Uint32 :doc "Number of waiters in lockextra.\n"} extraMWaiters]


  ^{:line 2759} [^{:tag atomic/Uint32 :doc "Number of extra M's in use by threads.\n"} extraMInUse])









^{:go/end 2796} (go/func ^:go/nosplit lockextra "lockextra locks the extra list and returns the list head.\nThe caller must unlock the list by storing a new list head\nto extram. If nilokay is true, then lockextra will\nreturn a nil list head if that's what it finds. If nilokay is false,\nlockextra will keep waiting until the list head is no longer nil.\n" ^{:tag (* m)} [^bool nilokay]
  (let [^:const ^{:val 1} locked 1

      ^{:line 2772} incr false]
    (while true
      (let [old (.Load extraM)]
        (when (== old locked)
          (osyield_no_g)
          (continue))

        (when (and (== old 0) (not nilokay))
          (when (not incr)
            ;; Add 1 to the number of threads
            ;; waiting for an M.
            ;; This is cleared by newextram.
            (.Add extraMWaiters 1)
            (set! incr true))

          (usleep_no_g 1)
          (continue))

        (when (.CompareAndSwap extraM old locked)
          (return (conv (* m) (conv unsafe/Pointer old))))

        (osyield_no_g)
        (continue)))))




^{:go/end 2802} (go/func ^:go/nosplit unlockextra [^{:tag (* m)} mp ^int32 delta]
  (.Add extraMLength delta)
  (.Store extraM (conv uintptr (conv unsafe/Pointer mp))))









^{:go/end 2816} (go/func ^:go/nosplit getExtraM "Return an M from the extra M list. Returns last == true if the list becomes\nempty because of this call.\n\nSpins waiting for an extra M, so caller must ensure that the list always\ncontains or will soon contain at least one M.\n" [] :results [^{:tag (* m)} mp ^bool last]
  (set! mp (lockextra false))
  (.Add extraMInUse 1)
  (unlockextra (.ptr (.-schedlink mp)) -1)
  (return mp (== (.ptr (.-schedlink mp)) nil)))






^{:go/end 2825} (go/func ^:go/nosplit putExtraM "Returns an extra M back to the list. mp must be from getExtraM. Newly\nallocated M's should use addExtraM.\n" [^{:tag (* m)} mp]
  (.Add extraMInUse -1)
  (addExtraM mp))





^{:go/end 2834} (go/func ^:go/nosplit addExtraM "Adds a newly allocated M to the extra M list.\n" [^{:tag (* m)} mp]
  (let [mnext (lockextra true)]
    (.set (.-schedlink mp) mnext)
    (unlockextra mp 1)))


(go/var



  ^{:line 2840} [^{:tag rwmutex :doc "allocmLock is locked for read when creating new Ms in allocm and their\naddition to allm. Thus acquiring this lock for write blocks the\ncreation of new Ms.\n"} allocmLock]




  ^{:line 2845} [^{:tag rwmutex :doc "execLock serializes exec and clone to avoid bugs or unspecified\nbehaviour around exec'ing while creating/destroying threads. See\nissue #19546.\n"} execLock])




(go/const "These errors are reported (via writeErrStr) by some OS-specific\nversions of newosproc and newosproc0.\n"
  ^{:line 2851} [^{:val "runtime: failed to create new OS thread\n"} failthreadcreate "runtime: failed to create new OS thread\n"]
  ^{:line 2852} [^{:val "runtime: failed to allocate stack for the new OS thread\n"} failallocatestack "runtime: failed to allocate stack for the new OS thread\n"])





(go/var ^{:tag (struct ^mutex lock ^{:tag muintptr :doc "newm points to a list of M structures that need new OS\nthreads. The list is linked through m.schedlink.\n"} newm ^{:tag bool :doc "waiting indicates that wake needs to be notified when an m\nis put on the list.\n"} waiting ^note wake ^{:tag uint32 :doc "haveTemplateThread indicates that the templateThread has\nbeen started. This is not protected by lock. Use cas to set\nto 1.\n"} haveTemplateThread) :doc "newmHandoff contains a list of m structures that need new OS threads.\nThis is used by newm in situations where newm itself can't safely\nstart an OS thread.\n"} newmHandoff)
























^{:go/end 2970} (go/func ^:go/nowritebarrierrec newm "Create a new m. It will start off with a call to fn, or else the scheduler.\nfn needs to be static and not a heap allocated closure.\nMay run with m.p==nil, so write barriers are not allowed.\n\nid is optional pre-allocated m ID. Omit by passing -1.\n" [^{:tag (func [])} fn ^{:tag (* p)} pp ^int64 id]
  ;; allocm adds a new M to allm, but they do not start until created by
  ;; the OS in newm1 or the template thread.
  ;;
  ;; doAllThreadsSyscall requires that every M in allm will eventually
  ;; start and be signal-able, even with a STW.
  ;;
  ;; Disable preemption here until we start the thread to ensure that
  ;; newm is not preempted between allocm and starting the new thread,
  ;; ensuring that anything added to allm is guaranteed to eventually
  ;; start.
  (acquirem)

  ;; On custom GOOS, to simplify SMP implementation requirements, Ms are
  ;; never dropped and mapped 1:1 to Ps. Therefore when a threading
  ;; implementation is detect we ensure an M count that never exceeds
  ;; GOMAXPROCS.
  (when (and (!= goos_overlay/Task nil) ^:go/paren (or (>= id (conv int64 gomaxprocs)) (< id 0)) (!= pp nil))
    (when (!= fn nil)
      ;; Caller incremented sched.nmspinning expecting a
      ;; spinning M to materialize. Since we're not creating
      ;; one, undo the increment.
      (when (< (.Add (.-nmspinning sched) -1) 0)
        (throw "newm: negative nmspinning")))



    ;; Drain pp's local runq into the global runq, pidleput
    ;; requires an empty runq, and pp may have G's queued (e.g.,
    ;; from STW where pp comes directly from procresize with its
    ;; full state). Any M that later acquires pp via findRunnable
    ;; will pull these G's from the global runq.
    (while true
      (let [(values gp _) (runqget pp)]
        (when (== gp nil)
          (break))

        (lock (addr (.-lock sched)))
        (globrunqput gp)
        (unlock (addr (.-lock sched)))))


    (lock (addr (.-lock sched)))
    (when (> id 0)
      (dec! (.-mnext sched)))

    (pidleput pp 0)
    (unlock (addr (.-lock sched)))

    (releasem (.-m (getg)))
    (return))


  (let [mp (allocm pp fn id)]
    (.set (.-nextp mp) pp)
    (set! (.-sigmask mp) initSigmask)
    (when [gp (getg)] (and (!= gp nil) (!= (.-m gp) nil) ^:go/paren (or (!= (.-lockedExt (.-m gp)) 0) (.-incgo (.-m gp))) ^:go/paren (and (!= GOOS "plan9") (!= GOOS "tamago")))
      ;; We're on a locked M or a thread that may have been
      ;; started by C. The kernel state of this thread may
      ;; be strange (the user may have locked it for that
      ;; purpose). We don't want to clone that into another
      ;; thread. Instead, ask a known-good thread to create
      ;; the thread for us.
      ;;
      ;; This is disabled on Plan 9. See golang.org/issue/22227.
      ;;
      ;; TODO: This may be unnecessary on Windows, which
      ;; doesn't model thread creation off fork.
      (lock (addr (.-lock newmHandoff)))
      (when (== (.-haveTemplateThread newmHandoff) 0)
        (throw "on a locked thread with no template thread"))

      (set! (.-schedlink mp) (.-newm newmHandoff))
      (.set (.-newm newmHandoff) mp)
      (when (.-waiting newmHandoff)
        (set! (.-waiting newmHandoff) false)
        (notewakeup (addr (.-wake newmHandoff))))

      (unlock (addr (.-lock newmHandoff)))
      ;; The M has not started yet, but the template thread does not
      ;; participate in STW, so it will always process queued Ms and
      ;; it is safe to releasem.
      (releasem (.-m (getg)))
      (return))

    (newm1 mp)
    (releasem (.-m (getg)))))


^{:go/end 2992} (go/func newm1 [^{:tag (* m)} mp]
  (when (and iscgo (!= _cgo_thread_start nil))
    (let [^cgothreadstart ts (zero cgothreadstart)]
      (.set (.-g ts) (.-g0 mp))
      (set! (.-tls ts) (conv (* uint64) (conv unsafe/Pointer (addr (aget (.-tls mp) 0)))))
      (set! (.-fn ts) (conv unsafe/Pointer (abi/FuncPCABI0 mstart)))
      (when msanenabled
        (msanwrite (conv unsafe/Pointer (addr ts)) (unsafe/Sizeof ts)))

      (when asanenabled
        (asanwrite (conv unsafe/Pointer (addr ts)) (unsafe/Sizeof ts)))

      (.rlock execLock) ; Prevent process clone.
      (asmcgocall _cgo_thread_start (conv unsafe/Pointer (addr ts)))
      (.runlock execLock)
      (return)))

  (.rlock execLock) ; Prevent process clone.
  (newosproc mp)
  (.runlock execLock))






^{:go/end 3013} (go/func startTemplateThread "startTemplateThread starts the template thread if it is not already\nrunning.\n\nThe calling thread must itself be in a known-good state.\n" []
  (when (or (== GOARCH "wasm") ; no threads on wasm yet
      (== GOOS "tamago")) ; Ms are bound to P on tamago
    (return))


  ;; Disable preemption to guarantee that the template thread will be
  ;; created before a park once haveTemplateThread is set.
  (let [mp (acquirem)]
    (when (not (atomic/Cas (addr (.-haveTemplateThread newmHandoff)) 0 1))
      (releasem mp)
      (return))

    (newm templateThread nil -1)
    (releasem mp)))














^{:go/end 3052} (go/func ^:go/nowritebarrierrec templateThread "templateThread is a thread in a known-good state that exists solely\nto start new threads in known-good states when the calling thread\nmay not be in a good state.\n\nMany programs never need this, so templateThread is started lazily\nwhen we first enter a state that might lead to running on a thread\nin an unknown state.\n\ntemplateThread runs on an M without a P, so it must not have write\nbarriers.\n" []
  (lock (addr (.-lock sched)))
  (inc! (.-nmsys sched))
  (checkdead)
  (unlock (addr (.-lock sched)))

  (while true
    (lock (addr (.-lock newmHandoff)))
    (while (!= (.-newm newmHandoff) 0)
      (let [newm (.ptr (.-newm newmHandoff))]
        (set! (.-newm newmHandoff) 0)
        (unlock (addr (.-lock newmHandoff)))
        (while (!= newm nil)
          (let [next (.ptr (.-schedlink newm))]
            (set! (.-schedlink newm) 0)
            (newm1 newm)
            (set! newm next)))

        (lock (addr (.-lock newmHandoff)))))

    (set! (.-waiting newmHandoff) true)
    (noteclear (addr (.-wake newmHandoff)))
    (unlock (addr (.-lock newmHandoff)))
    (notesleep (addr (.-wake newmHandoff)))))





^{:go/end 3075} (go/func stopm "Stops execution of the current m until new work is available.\nReturns with acquired P.\n" []
  (let [gp (getg)]

    (when (!= (.-locks (.-m gp)) 0)
      (throw "stopm holding locks"))

    (when (!= (.-p (.-m gp)) 0)
      (throw "stopm holding p"))

    (when (.-spinning (.-m gp))
      (throw "stopm spinning"))


    (lock (addr (.-lock sched)))
    (mput (.-m gp))
    (unlock (addr (.-lock sched)))
    (mPark)
    (acquirep (.ptr (.-nextp (.-m gp))))
    (set! (.-nextp (.-m gp)) 0)))


^{:go/end 3080} (go/func mspinning []
  ;; startm's caller incremented nmspinning. Set the new M's spinning.
  (set! (.-spinning (.-m (getg))) true))



















^{:go/end 3189} (go/func ^:go/nowritebarrierrec startm "Schedules some M to run the p (creates an M if necessary).\nIf p==nil, tries to get an idle P, if no idle P's does nothing.\nMay run with m.p==nil, so write barriers are not allowed.\nIf spinning is set, the caller has incremented nmspinning and must provide a\nP. startm will set m.spinning in the newly started M.\n\nCallers passing a non-nil P must call from a non-preemptible context. See\ncomment on acquirem below.\n\nArgument lockheld indicates whether the caller already acquired the\nscheduler lock. Callers holding the lock when making the call must pass\ntrue. The lock might be temporarily dropped, but will be reacquired before\nreturning.\n\nMust not have write barriers because this may be called without a P.\n" [^{:tag (* p)} pp ^bool spinning ^bool lockheld]
  ;; Disable preemption.
  ;;
  ;; Every owned P must have an owner that will eventually stop it in the
  ;; event of a GC stop request. startm takes transient ownership of a P
  ;; (either from argument or pidleget below) and transfers ownership to
  ;; a started M, which will be responsible for performing the stop.
  ;;
  ;; Preemption must be disabled during this transient ownership,
  ;; otherwise the P this is running on may enter GC stop while still
  ;; holding the transient P, leaving that P in limbo and deadlocking the
  ;; STW.
  ;;
  ;; Callers passing a non-nil P must already be in non-preemptible
  ;; context, otherwise such preemption could occur on function entry to
  ;; startm. Callers passing a nil P may be preemptible, so we must
  ;; disable preemption before acquiring a P from pidleget below.
  (let [mp (acquirem)]
    (when (not lockheld)
      (lock (addr (.-lock sched))))

    (when (== pp nil)
      (when spinning
        ;; TODO(prattmic): All remaining calls to this function
        ;; with _p_ == nil could be cleaned up to find a P
        ;; before calling startm.
        (throw "startm: P required for spinning=true"))

      (set! (values pp _) (pidleget 0))
      (when (== pp nil)
        (when (not lockheld)
          (unlock (addr (.-lock sched))))

        (releasem mp)
        (return)))


    (let [nmp (mget)]
      (when (== nmp nil)
        ;; No M is available, we must drop sched.lock and call newm.
        ;; However, we already own a P to assign to the M.
        ;;
        ;; Once sched.lock is released, another G (e.g., in a syscall),
        ;; could find no idle P while checkdead finds a runnable G but
        ;; no running M's because this new M hasn't started yet, thus
        ;; throwing in an apparent deadlock.
        ;; This apparent deadlock is possible when startm is called
        ;; from sysmon, which doesn't count as a running M.
        ;;
        ;; Avoid this situation by pre-allocating the ID for the new M,
        ;; thus marking it as 'running' before we drop sched.lock. This
        ;; new M will eventually run the scheduler to execute any
        ;; queued G's.
        (let [id (mReserveID)]
          (unlock (addr (.-lock sched)))

          (let [^{:tag (func [])} fn (zero (func []))]
            (when spinning
              ;; The caller incremented nmspinning, so set m.spinning in the new M.
              (set! fn mspinning))

            (newm fn pp id)

            (when lockheld
              (lock (addr (.-lock sched))))

            ;; Ownership transfer of pp committed by start in newm.
            ;; Preemption is now safe.
            (releasem mp)
            (return))))

      (when (not lockheld)
        (unlock (addr (.-lock sched))))

      (when (.-spinning nmp)
        (throw "startm: m is spinning"))

      (when (!= (.-nextp nmp) 0)
        (throw "startm: m has p"))

      (when (and spinning (not (runqempty pp)))
        (throw "startm: p has runnable gs"))

      ;; The caller incremented nmspinning, so set m.spinning in the new M.
      (set! (.-spinning nmp) spinning)
      (.set (.-nextp nmp) pp)
      (notewakeup (addr (.-park nmp)))
      ;; Ownership transfer of pp committed by wakeup. Preemption is now
      ;; safe.
      (releasem mp))))






^{:go/end 3261} (go/func ^:go/nowritebarrierrec handoffp "Hands off P from syscall or locked M.\nAlways runs without a P, so write barriers are not allowed.\n" [^{:tag (* p)} pp]
  ;; handoffp must start an M in any situation where
  ;; findRunnable would return a G to run on pp.

  ;; if it has local work, start it straight away
  (when (or (not (runqempty pp)) (not (.empty (.-runq sched))))
    (startm pp false false)
    (return))

  ;; if there's trace work to do, start it straight away
  (when (and ^:go/paren (or (traceEnabled) (traceShuttingDown)) (!= (traceReaderAvailable) nil))
    (startm pp false false)
    (return))

  ;; if it has GC work, start it straight away
  (when (and (!= gcBlackenEnabled 0) (gcShouldScheduleWorker pp))
    (startm pp false false)
    (return))

  ;; no local work, check that there are no spinning/idle M's,
  ;; otherwise our help is not required
  (when (and (== (+ (.Load (.-nmspinning sched)) (.Load (.-npidle sched))) 0) (.CompareAndSwap (.-nmspinning sched) 0 1)) ; TODO: fast atomic
    (.Store (.-needspinning sched) 0)
    (startm pp true false)
    (return))

  (lock (addr (.-lock sched)))
  (when (.Load (.-gcwaiting sched))
    (set! (.-status pp) _Pgcstop)
    (set! (.-gcStopTime pp) (nanotime))
    (dec! (.-stopwait sched))
    (when (== (.-stopwait sched) 0)
      (notewakeup (addr (.-stopnote sched))))

    (unlock (addr (.-lock sched)))
    (return))

  (when (and (!= (.-runSafePointFn pp) 0) (atomic/Cas (addr (.-runSafePointFn pp)) 1 0))
    ((.-safePointFn sched) pp)
    (dec! (.-safePointWait sched))
    (when (== (.-safePointWait sched) 0)
      (notewakeup (addr (.-safePointNote sched)))))


  (when (not (.empty (.-runq sched)))
    (unlock (addr (.-lock sched)))
    (startm pp false false)
    (return))

  ;; If this is the last running P and nobody is polling network,
  ;; need to wakeup another M to poll network.
  (when (and (== (.Load (.-npidle sched)) (- gomaxprocs 1)) (!= (.Load (.-lastpoll sched)) 0))
    (unlock (addr (.-lock sched)))
    (startm pp false false)
    (return))


  ;; The scheduler lock cannot be held when calling wakeNetPoller below
  ;; because wakeNetPoller may call wakep which may call startm.
  (let [when (.wakeTime (.-timers pp))]
    (pidleput pp 0)
    (unlock (addr (.-lock sched)))

    (when (!= when 0)
      (wakeNetPoller when))))
















^{:go/end 3310} (go/func ^{:go/linkname "wakep"} wakep "Tries to add one more P to execute G's.\nCalled when a G is made runnable (newproc, ready).\nMust be called with a P.\n\nwakep should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  ;; Be conservative about spinning threads, only start one if none exist
  ;; already.
  (when (or (!= (.Load (.-nmspinning sched)) 0) (not (.CompareAndSwap (.-nmspinning sched) 0 1)))
    (return))


  ;; Disable preemption until ownership of pp transfers to the next M in
  ;; startm. Otherwise preemption here would leave pp stuck waiting to
  ;; enter _Pgcstop.
  ;;
  ;; See preemption comment on acquirem in startm for more details.
  (let [mp (acquirem)

      ^{:line 3290 :tag (* p)} pp (zero (* p))]
    (lock (addr (.-lock sched)))
    (set! (values pp _) (pidlegetSpinning 0))
    (when (== pp nil)
      (when (< (.Add (.-nmspinning sched) -1) 0)
        (throw "wakep: negative nmspinning"))

      (unlock (addr (.-lock sched)))
      (releasem mp)
      (return))

    ;; Since we always have a P, the race in the "No M is available"
    ;; comment in startm doesn't apply during the small window between the
    ;; unlock here and lock in startm. A checkdead in between will always
    ;; see at least one running M (ours).
    (unlock (addr (.-lock sched)))

    (startm pp true false)

    (releasem mp)))




^{:go/end 3336} (go/func stoplockedm "Stops execution of the current m that is locked to a g until the g is runnable again.\nReturns with acquired P.\n" []
  (let [gp (getg)]

    (when (or (== (.-lockedg (.-m gp)) 0) (!= (.ptr (.-lockedm (.ptr (.-lockedg (.-m gp))))) (.-m gp)))
      (throw "stoplockedm: inconsistent locking"))

    (when (!= (.-p (.-m gp)) 0)
      ;; Schedule another M to run this p.
      (let [pp (releasep)]
        (handoffp pp)))

    (incidlelocked 1)
    ;; Wait until another thread schedules lockedg again.
    (mPark)
    (let [status (readgstatus (.ptr (.-lockedg (.-m gp))))]
      (when (!= (bit-and-not status _Gscan) _Grunnable)
        (print "runtime:stoplockedm: lockedg (atomicstatus=" status ") is not Grunnable or Gscanrunnable\n")
        (dumpgstatus (.ptr (.-lockedg (.-m gp))))
        (throw "stoplockedm: not runnable"))

      (acquirep (.ptr (.-nextp (.-m gp))))
      (set! (.-nextp (.-m gp)) 0))))






^{:go/end 3356} (go/func ^:go/nowritebarrierrec startlockedm "Schedules the locked m to run the locked gp.\nMay run during STW, so write barriers are not allowed.\n" [^{:tag (* g)} gp]
  (let [mp (.ptr (.-lockedm gp))]
    (when (== mp (.-m (getg)))
      (throw "startlockedm: locked to me"))

    (when (!= (.-nextp mp) 0)
      (throw "startlockedm: m has p"))

    ;; directly handoff current P to the locked m
    (incidlelocked -1)
    (let [pp (releasep)]
      (.set (.-nextp mp) pp)
      (notewakeup (addr (.-park mp)))
      (stopm))))




^{:go/end 3384} (go/func gcstopm "Stops the current m for stopTheWorld.\nReturns when the world is restarted.\n" []
  (let [gp (getg)]

    (when (not (.Load (.-gcwaiting sched)))
      (throw "gcstopm: not waiting for gc"))

    (when (.-spinning (.-m gp))
      (set! (.-spinning (.-m gp)) false)
      ;; OK to just drop nmspinning here,
      ;; startTheWorld will unpark threads as necessary.
      (when (< (.Add (.-nmspinning sched) -1) 0)
        (throw "gcstopm: negative nmspinning")))


    (let [pp (releasep)]
      (lock (addr (.-lock sched)))
      (set! (.-status pp) _Pgcstop)
      (set! (.-gcStopTime pp) (nanotime))
      (dec! (.-stopwait sched))
      (when (== (.-stopwait sched) 0)
        (notewakeup (addr (.-stopnote sched))))

      (unlock (addr (.-lock sched)))
      (stopm))))











^{:go/end 3447} (go/func ^:go/yeswritebarrierrec execute "Schedules gp to run on the current M.\nIf inheritTime is true, gp inherits the remaining time in the\ncurrent time slice. Otherwise, it starts a new time slice.\nNever returns.\n\nWrite barriers are allowed because this is called immediately after\nacquiring a P in several places.\n" [^{:tag (* g)} gp ^bool inheritTime]
  (let [mp (.-m (getg))]

    (when (.-active goroutineProfile)
      ;; Make sure that gp has had its stack written out to the goroutine
      ;; profile, exactly as it was when the goroutine profiler first stopped
      ;; the world.
      (tryRecordGoroutineProfile gp nil osyield))


    ;; Assign gp.m before entering _Grunning so running Gs have an M.
    (set! (.-curg mp) gp)
    (set! (.-m gp) mp)
    (set! (.-syncSafePoint gp) false) ; Clear the flag, which may have been set by morestack.
    (casgstatus gp _Grunnable _Grunning)
    (set! (.-waitsince gp) 0)
    (set! (.-preempt gp) false)
    (set! (.-stackguard0 gp) (+ (.-lo (.-stack gp)) stackGuard))
    (when (not inheritTime)
      (inc! (.-schedtick (.ptr (.-p mp)))))


    (when (and sys/DITSupported (!= (.-dataindependenttiming debug) 1))
      (cond (and (.-ditWanted gp) (not (.-ditEnabled mp))) (do
          ;; The current M doesn't have DIT enabled, but the goroutine we're
          ;; executing does need it, so turn it on.
          (sys/EnableDIT)
          (set! (.-ditEnabled mp) true))
        (and (not (.-ditWanted gp)) (.-ditEnabled mp)) (do
          ;; The current M has DIT enabled, but the goroutine we're executing does
          ;; not need it, so turn it off.
          ;; NOTE: turning off DIT here means that the scheduler will have DIT enabled
          ;; when it runs after this goroutine yields or is preempted. This may have
          ;; a minor performance impact on the scheduler.
          (sys/DisableDIT)
          (set! (.-ditEnabled mp) false))))



    ;; Check whether the profiler needs to be turned on or off.
    (let [hz (.-profilehz sched)]
      (when (!= (.-profilehz mp) hz)
        (setThreadCPUProfiler hz))


      (let [trace (traceAcquire)]
        (when (.ok trace)
          (.GoStart trace)
          (traceRelease trace))


        (gogo (addr (.-sched gp)))))))






^{:go/end 3865} (go/func findRunnable "Finds a runnable goroutine to execute.\nTries to steal from other P's, get g from local or global queue, poll network.\ntryWakeP indicates that the returned goroutine is not normal (GC worker, trace\nreader) so the caller should try to wake a P.\n" [] :results [^{:tag (* g)} gp ^bool inheritTime ^bool tryWakeP]
  (let [mp (.-m (getg))]

    ;; The conditions here and in handoffp must agree: if
    ;; findRunnable would return a G to run, handoffp must start
    ;; an M.

    (label :top
      ;; We may have collected an allp snapshot below. The snapshot is only
      ;; required in each loop iteration. Clear it to all GC to collect the
      ;; slice.
      (.clearAllpSnapshot mp))

    (let [pp (.ptr (.-p mp))]
      (when (.Load (.-gcwaiting sched))
        (gcstopm)
        (goto :top))

      (when (!= (.-runSafePointFn pp) 0)
        (runSafePointFn))


      ;; now and pollUntil are saved for work stealing later,
      ;; which may steal timers. It's important that between now
      ;; and then, nothing blocks, so these numbers remain mostly
      ;; relevant.
      (let [(values now pollUntil _) (.check (.-timers pp) 0 nil)]

        ;; Try to schedule the trace reader.
        (when (or (traceEnabled) (traceShuttingDown))
          (let [gp (traceReader)]
            (when (!= gp nil)
              (let [trace (traceAcquire)]
                (casgstatus gp _Gwaiting _Grunnable)
                (when (.ok trace)
                  (.GoUnpark trace gp 0)
                  (traceRelease trace))

                (return gp false true)))))



        ;; Try to schedule a GC worker.
        (when (!= gcBlackenEnabled 0)
          (let [(values gp tnow) (.findRunnableGCWorker gcController pp now)]
            (when (!= gp nil)
              (return gp false true))

            (set! now tnow)))


        ;; Check the global runnable queue once in a while to ensure fairness.
        ;; Otherwise two goroutines can completely occupy the local runqueue
        ;; by constantly respawning each other.
        (when (and (== (% (.-schedtick pp) 61) 0) (not (.empty (.-runq sched))))
          (lock (addr (.-lock sched)))
          (let [gp (globrunqget)]
            (unlock (addr (.-lock sched)))
            (when (!= gp nil)
              (return gp false false))))



        ;; Wake up the finalizer G.
        (when (== (bit-and (.Load fingStatus) (bit-or fingWait fingWake)) (bit-or fingWait fingWake))
          (when [gp (wakefing)] (!= gp nil)
            (ready gp 0 true)))



        ;; Wake up one or more cleanup Gs.
        (when (.needsWake gcCleanups)
          (.wake gcCleanups))


        (when (!= @cgo_yield nil)
          (asmcgocall @cgo_yield nil))


        ;; local runq
        (when [(values gp inheritTime) (runqget pp)] (!= gp nil)
          (return gp inheritTime false))


        ;; global runq
        (when (not (.empty (.-runq sched)))
          (lock (addr (.-lock sched)))
          (let [(values gp q) (globrunqgetbatch (/ (conv int32 (len (.-runq pp))) 2))]
            (unlock (addr (.-lock sched)))
            (when (!= gp nil)
              (when [(runqputbatch pp (addr q))] (not (.empty q))
                (throw "Couldn't put Gs into empty local runq"))

              (return gp false false))))



        ;; Poll network.
        ;; This netpoll is only an optimization before we resort to stealing.
        ;; We can safely skip it if there are no waiters or a thread is blocked
        ;; in netpoll already. If there is any kind of logical race with that
        ;; blocked thread (e.g. it has already returned from netpoll, but does
        ;; not set lastpoll yet), this thread will do blocking netpoll below
        ;; anyway.
        ;; We only poll from one thread at a time to avoid kernel contention
        ;; on machines with many cores.
        (when (and (netpollinited) (netpollAnyWaiters) (!= (.Load (.-lastpoll sched)) 0) (== (.Swap (.-pollingNet sched) 1) 0))
          (let [(values list delta) (netpoll 0)]
            (.Store (.-pollingNet sched) 0)
            (when (not (.empty list)) ; non-blocking
              (let [gp (.pop list)]
                (injectglist (addr list))
                (netpollAdjustWaiters delta)
                (let [trace (traceAcquire)]
                  (casgstatus gp _Gwaiting _Grunnable)
                  (when (.ok trace)
                    (.GoUnpark trace gp 0)
                    (traceRelease trace))

                  (return gp false false))))))



        ;; Spinning Ms: steal work from other Ps.
        ;;
        ;; Limit the number of spinning Ms to half the number of busy Ps.
        ;; This is necessary to prevent excessive CPU consumption when
        ;; GOMAXPROCS>>1 but the program parallelism is low.
        (when (or (.-spinning mp) (< (* 2 (.Load (.-nmspinning sched))) (- gomaxprocs (.Load (.-npidle sched)))))
          (when (not (.-spinning mp))
            (.becomeSpinning mp))


          (let [(values gp inheritTime tnow w newWork) (stealWork now)]
            (when (!= gp nil)
              ;; Successfully stole.
              (return gp inheritTime false))

            (when newWork
              ;; There may be new timer or GC work; restart to
              ;; discover.
              (goto :top))


            (set! now tnow)
            (when (and (!= w 0) ^:go/paren (or (== pollUntil 0) (< w pollUntil)))
              ;; Earlier timer to wait for.
              (set! pollUntil w))))



        ;; We have nothing to do.
        ;;
        ;; If we're in the GC mark phase, can safely scan and blacken objects,
        ;; and have work to do, run idle-time marking rather than give up the P.
        (when (and (!= gcBlackenEnabled 0) (gcShouldScheduleWorker pp) (.addIdleMarkWorker gcController))
          (let [node (conv (* gcBgMarkWorkerNode) (.pop gcBgMarkWorkerPool))]
            (when (!= node nil)
              (set! (.-gcMarkWorkerMode pp) gcMarkWorkerIdleMode)
              (let [gp (.ptr (.-gp node))

                  ^{:line 3614} trace (traceAcquire)]
                (casgstatus gp _Gwaiting _Grunnable)
                (when (.ok trace)
                  (.GoUnpark trace gp 0)
                  (traceRelease trace))

                (return gp false false)))

            (.removeIdleMarkWorker gcController)))


        ;; wasm only:
        ;; If a callback returned and no other goroutine is awake,
        ;; then wake event handler goroutine which pauses execution
        ;; until a callback was triggered.
        ;;
        ;; tamago only:
        ;; invoke application callback for idle logic (e.g. cpu halt)
        (let [(values ^:assign gp otherReady) (beforeIdle now pollUntil)]
          (when (!= gp nil)
            (let [trace (traceAcquire)]
              (casgstatus gp _Gwaiting _Grunnable)
              (when (.ok trace)
                (.GoUnpark trace gp 0)
                (traceRelease trace))

              (return gp false false)))

          (when otherReady
            (goto :top))


          ;; Before we drop our P, make a snapshot of the allp slice,
          ;; which can change underfoot once we no longer block
          ;; safe-points. We don't need to snapshot the contents because
          ;; everything up to cap(allp) is immutable.
          ;;
          ;; We clear the snapshot from the M after return via
          ;; mp.clearAllpSnapshop (in schedule) and on each iteration of the top
          ;; loop.
          (let [allpSnapshot (.snapshotAllp mp)
              ;; Also snapshot masks. Value changes are OK, but we can't allow
              ;; len to change out from under us.
              ^{:line 3657} idlepMaskSnapshot idlepMask
              ^{:line 3658} timerpMaskSnapshot timerpMask]

            ;; return P and block
            (lock (addr (.-lock sched)))
            (when (or (.Load (.-gcwaiting sched)) (!= (.-runSafePointFn pp) 0))
              (unlock (addr (.-lock sched)))
              (goto :top))

            (when (not (.empty (.-runq sched)))
              (let [(values gp q) (globrunqgetbatch (/ (conv int32 (len (.-runq pp))) 2))]
                (unlock (addr (.-lock sched)))
                (when (== gp nil)
                  (throw "global runq empty with non-zero runqsize"))

                (when [(runqputbatch pp (addr q))] (not (.empty q))
                  (throw "Couldn't put Gs into empty local runq"))

                (return gp false false)))

            (when (and (not (.-spinning mp)) (== (.Load (.-needspinning sched)) 1))
              ;; See "Delicate dance" comment below.
              (.becomeSpinning mp)
              (unlock (addr (.-lock sched)))
              (goto :top))

            (when (!= (releasep) pp)
              (throw "findRunnable: wrong p"))

            (set! now (pidleput pp now))
            (unlock (addr (.-lock sched)))

            ;; Delicate dance: thread transitions from spinning to non-spinning
            ;; state, potentially concurrently with submission of new work. We must
            ;; drop nmspinning first and then check all sources again (with
            ;; #StoreLoad memory barrier in between). If we do it the other way
            ;; around, another thread can submit work after we've checked all
            ;; sources but before we drop nmspinning; as a result nobody will
            ;; unpark a thread to run the work.
            ;;
            ;; This applies to the following sources of work:
            ;;
            ;; * Goroutines added to the global or a per-P run queue.
            ;; * New/modified-earlier timers on a per-P timer heap.
            ;; * Idle-priority GC work (barring golang.org/issue/19112).
            ;;
            ;; If we discover new work below, we need to restore m.spinning as a
            ;; signal for resetspinning to unpark a new worker thread (because
            ;; there can be more than one starving goroutine).
            ;;
            ;; However, if after discovering new work we also observe no idle Ps
            ;; (either here or in resetspinning), we have a problem. We may be
            ;; racing with a non-spinning M in the block above, having found no
            ;; work and preparing to release its P and park. Allowing that P to go
            ;; idle will result in loss of work conservation (idle P while there is
            ;; runnable work). This could result in complete deadlock in the
            ;; unlikely event that we discover new work (from netpoll) right as we
            ;; are racing with _all_ other Ps going idle.
            ;;
            ;; We use sched.needspinning to synchronize with non-spinning Ms going
            ;; idle. If needspinning is set when they are about to drop their P,
            ;; they abort the drop and instead become a new spinning M on our
            ;; behalf. If we are not racing and the system is truly fully loaded
            ;; then no spinning threads are required, and the next thread to
            ;; naturally become spinning will clear the flag.
            ;;
            ;; Also see "Worker thread parking/unparking" comment at the top of the
            ;; file.
            (let [wasSpinning (.-spinning mp)]
              (when (.-spinning mp)
                (set! (.-spinning mp) false)
                (when (< (.Add (.-nmspinning sched) -1) 0)
                  (throw "findRunnable: negative nmspinning"))


                ;; Note the for correctness, only the last M transitioning from
                ;; spinning to non-spinning must perform these rechecks to
                ;; ensure no missed work. However, the runtime has some cases
                ;; of transient increments of nmspinning that are decremented
                ;; without going through this path, so we must be conservative
                ;; and perform the check on all spinning Ms.
                ;;
                ;; See https://go.dev/issue/43997.

                ;; Check global and P runqueues again.

                (lock (addr (.-lock sched)))
                (when (not (.empty (.-runq sched)))
                  (let [(values pp _) (pidlegetSpinning 0)]
                    (when (!= pp nil)
                      (let [(values gp q) (globrunqgetbatch (/ (conv int32 (len (.-runq pp))) 2))]
                        (unlock (addr (.-lock sched)))
                        (when (== gp nil)
                          (throw "global runq empty with non-zero runqsize"))

                        (when [(runqputbatch pp (addr q))] (not (.empty q))
                          (throw "Couldn't put Gs into empty local runq"))

                        (acquirep pp)
                        (.becomeSpinning mp)
                        (return gp false false)))))


                (unlock (addr (.-lock sched)))

                (let [pp (checkRunqsNoP allpSnapshot idlepMaskSnapshot)]
                  (when (!= pp nil)
                    (acquirep pp)
                    (.becomeSpinning mp)
                    (goto :top))


                  ;; Check for idle-priority GC work again.
                  (let [(values ^:assign pp gp) (checkIdleGCNoP)]
                    (when (!= pp nil)
                      (acquirep pp)
                      (.becomeSpinning mp)

                      ;; Run the idle worker.
                      (set! (.-gcMarkWorkerMode pp) gcMarkWorkerIdleMode)
                      (let [trace (traceAcquire)]
                        (casgstatus gp _Gwaiting _Grunnable)
                        (when (.ok trace)
                          (.GoUnpark trace gp 0)
                          (traceRelease trace))

                        (return gp false false)))


                    ;; Finally, check for timer creation or expiry concurrently with
                    ;; transitioning from spinning to non-spinning.
                    ;;
                    ;; Note that we cannot use checkTimers here because it calls
                    ;; adjusttimers which may need to allocate memory, and that isn't
                    ;; allowed when we don't have an active P.
                    (set! pollUntil (checkTimersNoP allpSnapshot timerpMaskSnapshot pollUntil)))))


              ;; We don't need allp anymore at this pointer, but can't clear the
              ;; snapshot without a P for the write barrier..

              ;; Poll network until next timer.
              (cond (and (netpollinited) ^:go/paren (or (netpollAnyWaiters) (!= pollUntil 0)) (!= (.Swap (.-lastpoll sched) 0) 0)) (do
                  (.Store (.-pollUntil sched) pollUntil)
                  (when (!= (.-p mp) 0)
                    (throw "findRunnable: netpoll with p"))

                  (when (.-spinning mp)
                    (throw "findRunnable: netpoll with spinning"))

                  (let [delay (conv int64 -1)]
                    (when (!= pollUntil 0)
                      (when (== now 0)
                        (set! now (nanotime)))

                      (set! delay (- pollUntil now))
                      (when (< delay 0)
                        (set! delay 0)))


                    (when (!= faketime 0)
                      ;; When using fake time, just poll.
                      (set! delay 0))

                    (let [(values list delta) (netpoll delay)] ; block until new work is available
                      ;; Refresh now again, after potentially blocking.
                      (set! now (nanotime))
                      (.Store (.-pollUntil sched) 0)
                      (.Store (.-lastpoll sched) now)
                      (when (and (!= faketime 0) (.empty list))
                        ;; Using fake time and nothing is ready; stop M.
                        ;; When all M's stop, checkdead will call timejump.
                        (stopm)
                        (goto :top))

                      (lock (addr (.-lock sched)))
                      (let [(values pp _) (pidleget now)]
                        (unlock (addr (.-lock sched)))
                        (if (== pp nil) (do
                            (injectglist (addr list))
                            (netpollAdjustWaiters delta))
                          (do
                            (acquirep pp)
                            (when (not (.empty list))
                              (let [gp (.pop list)]
                                (injectglist (addr list))
                                (netpollAdjustWaiters delta)
                                (let [trace (traceAcquire)]
                                  (casgstatus gp _Gwaiting _Grunnable)
                                  (when (.ok trace)
                                    (.GoUnpark trace gp 0)
                                    (traceRelease trace))

                                  (return gp false false))))

                            (when wasSpinning
                              (.becomeSpinning mp))

                            (goto :top)))))))

                (and (!= pollUntil 0) (netpollinited))
                (let [pollerPollUntil (.Load (.-pollUntil sched))]
                  (when (or (== pollerPollUntil 0) (> pollerPollUntil pollUntil))
                    (netpollBreak))))


              (stopm)
              (goto :top))))))))






^{:go/end 3887} (go/func pollWork "pollWork reports whether there is non-background work this P could\nbe doing. This is a fairly lightweight check to be used for\nbackground work loops, like idle GC. It checks a subset of the\nconditions checked by the actual scheduler.\n" ^bool []
  (when (not (.empty (.-runq sched)))
    (return true))

  ^{:go/breaks [4]} (let [p (.ptr (.-p (.-m (getg))))]
    (when (not (runqempty p))
      (return true))

    (when (and (netpollinited) (netpollAnyWaiters) (!= (.Load (.-lastpoll sched)) 0))
      (when [(values list delta) (netpoll 0)] (not (.empty list))
        (injectglist (addr list))
        (netpollAdjustWaiters delta)
        (return true)))


    false))








^{:go/end 3962} (go/func stealWork "stealWork attempts to steal a runnable goroutine or timer from any P.\n\nIf newWork is true, new work may have been readied.\n\nIf now is not 0 it is the current time. stealWork returns the passed time or\nthe current time if now was passed as 0.\n" [^int64 now] :results [^{:tag (* g)} gp ^bool inheritTime ^int64 rnow ^int64 pollUntil ^bool newWork]
  (let [pp (.ptr (.-p (.-m (getg))))

      ^{:line 3898} ranTimer false

      ^:const ^{:line 3900 :val 4} stealTries 4]
    (for [i 0] (< i stealTries) (inc! i)
      (let [stealTimersOrRunNextG (== i (- stealTries 1))]

        (for [enum (.start stealOrder (cheaprand))] (not (.done enum)) (.next enum)
          (when (.Load (.-gcwaiting sched))
            ;; GC work may be available.
            (return nil false now pollUntil true))

          (let [p2 (aget allp (.position enum))]
            (when (== pp p2)
              (continue))


            ;; Steal timers from p2. This call to checkTimers is the only place
            ;; where we might hold a lock on a different P's timers. We do this
            ;; once on the last pass before checking runnext because stealing
            ;; from the other P's runnext should be the last resort, so if there
            ;; are timers to steal do that first.
            ;;
            ;; We only check timers on one of the stealing iterations because
            ;; the time stored in now doesn't change in this loop and checking
            ;; the timers for each P more than once with the same value of now
            ;; is probably a waste of time.
            ;;
            ;; timerpMask tells us whether the P may have timers at all. If it
            ;; can't, no need to check at all.
            (when (and stealTimersOrRunNextG (.read timerpMask (.position enum)))
              (let [(values tnow w ran) (.check (.-timers p2) now nil)]
                (set! now tnow)
                (when (and (!= w 0) ^:go/paren (or (== pollUntil 0) (< w pollUntil)))
                  (set! pollUntil w))

                (when ran
                  ;; Running the timers may have
                  ;; made an arbitrary number of G's
                  ;; ready and added them to this P's
                  ;; local run queue. That invalidates
                  ;; the assumption of runqsteal
                  ;; that it always has room to add
                  ;; stolen G's. So check now if there
                  ;; is a local G to run.
                  (when [(values gp inheritTime) (runqget pp)] (!= gp nil)
                    (return gp inheritTime now pollUntil ranTimer))

                  (set! ranTimer true))))



            ;; Don't bother to attempt to steal if p2 is idle.
            (when (not (.read idlepMask (.position enum)))
              (when [gp (runqsteal pp p2 stealTimersOrRunNextG)] (!= gp nil)
                (return gp false now pollUntil ranTimer)))))))





    ;; No goroutines found to steal. Regardless, running a timer may have
    ;; made some goroutine ready that we missed. Indicate the next timer to
    ;; wait for.
    (return nil false now pollUntil ranTimer)))







^{:go/breaks [5] :go/end 3986} (go/func checkRunqsNoP "Check all Ps for a runnable G to steal.\n\nOn entry we have no P. If a G is available to steal and a P is available,\nthe P is returned which the caller should acquire and attempt to steal the\nwork to.\n" ^{:tag (* p)} [^{:tag (slice (* p))} allpSnapshot ^pMask idlepMaskSnapshot]
  (range [id p2 allpSnapshot]
    (when (and (not (.read idlepMaskSnapshot (conv uint32 id))) (not (runqempty p2)))
      (lock (addr (.-lock sched)))
      (let [(values pp _) (pidlegetSpinning 0)]
        (when (== pp nil)
          ;; Can't get a P, don't bother checking remaining Ps.
          (unlock (addr (.-lock sched)))
          (return nil))

        (unlock (addr (.-lock sched)))
        (return pp))))



  ;; No work available.
  nil)





^{:go/end 4002} (go/func checkTimersNoP "Check all Ps for a timer expiring sooner than pollUntil.\n\nReturns updated pollUntil value.\n" ^int64 [^{:tag (slice (* p))} allpSnapshot ^pMask timerpMaskSnapshot ^int64 pollUntil]
  (range [id p2 allpSnapshot]
    (when (.read timerpMaskSnapshot (conv uint32 id))
      (let [w (.wakeTime (.-timers p2))]
        (when (and (!= w 0) ^:go/paren (or (== pollUntil 0) (< w pollUntil)))
          (set! pollUntil w)))))




  ^{:line 4001} pollUntil)






^{:go/end 4064} (go/func checkIdleGCNoP "Check for idle-priority GC, without a P on entry.\n\nIf some GC work, a P, and a worker G are all available, the P and G will be\nreturned. The returned P has not been wired yet.\n" [] :results [(* p) (* g)]
  ;; N.B. Since we have no P, gcBlackenEnabled may change at any time; we
  ;; must check again after acquiring a P. As an optimization, we also check
  ;; if an idle mark worker is needed at all. This is OK here, because if we
  ;; observe that one isn't needed, at least one is currently running. Even if
  ;; it stops running, its own journey into the scheduler should schedule it
  ;; again, if need be (at which point, this check will pass, if relevant).
  (when (or (== (atomic/Load (addr gcBlackenEnabled)) 0) (not (.needIdleMarkWorker gcController)))
    (return nil nil))

  (when (not (gcShouldScheduleWorker nil))
    (return nil nil))


  ;; Work is available; we can start an idle GC worker only if there is
  ;; an available P and available worker G.
  ;;
  ;; We can attempt to acquire these in either order, though both have
  ;; synchronization concerns (see below). Workers are almost always
  ;; available (see comment in findRunnableGCWorker for the one case
  ;; there may be none). Since we're slightly less likely to find a P,
  ;; check for that first.
  ;;
  ;; Synchronization: note that we must hold sched.lock until we are
  ;; committed to keeping it. Otherwise we cannot put the unnecessary P
  ;; back in sched.pidle without performing the full set of idle
  ;; transition checks.
  ;;
  ;; If we were to check gcBgMarkWorkerPool first, we must somehow handle
  ;; the assumption in gcControllerState.findRunnableGCWorker that an
  ;; empty gcBgMarkWorkerPool is only possible if gcMarkDone is running.
  (lock (addr (.-lock sched)))
  (let [(values pp now) (pidlegetSpinning 0)]
    (when (== pp nil)
      (unlock (addr (.-lock sched)))
      (return nil nil))


    ;; Now that we own a P, gcBlackenEnabled can't change (as it requires STW).
    (when (or (== gcBlackenEnabled 0) (not (.addIdleMarkWorker gcController)))
      (pidleput pp now)
      (unlock (addr (.-lock sched)))
      (return nil nil))


    (let [node (conv (* gcBgMarkWorkerNode) (.pop gcBgMarkWorkerPool))]
      (when (== node nil)
        (pidleput pp now)
        (unlock (addr (.-lock sched)))
        (.removeIdleMarkWorker gcController)
        (return nil nil))


      (unlock (addr (.-lock sched)))

      (return pp (.ptr (.-gp node))))))





^{:go/end 4086} (go/func wakeNetPoller "wakeNetPoller wakes up the thread sleeping in the network poller if it isn't\ngoing to wake up before the when argument; or it wakes an idle P to service\ntimers and the network poller if there isn't one already.\n" [^int64 when]
  (if (== (.Load (.-lastpoll sched)) 0)
    ;; In findRunnable we ensure that when polling the pollUntil
    ;; field is either zero or the time to which the current
    ;; poll is expected to run. This can have a spurious wakeup
    ;; but should never miss a wakeup.
    (let [pollerPollUntil (.Load (.-pollUntil sched))]
      (when (or (== pollerPollUntil 0) (> pollerPollUntil when))
        (netpollBreak)))

    (do
      ;; There are no threads in the network poller, try to get
      ;; one there so it can handle new timers.
      (when (!= GOOS "plan9") ; Temporary workaround - see issue #42303.
        (wakep)))))




^{:go/end 4102} (go/func resetspinning []
  (let [gp (getg)]
    (when (not (.-spinning (.-m gp)))
      (throw "resetspinning: not a spinning m"))

    (set! (.-spinning (.-m gp)) false)
    (let [nmspinning (.Add (.-nmspinning sched) -1)]
      (when (< nmspinning 0)
        (throw "findRunnable: negative nmspinning"))

      ;; M wakeup policy is deliberately somewhat conservative, so check if we
      ;; need to wakeup another P here. See "Worker thread parking/unparking"
      ;; comment at the top of the file for details.
      (wakep))))










^{:go/end 4198} (go/func injectglist "injectglist adds each runnable G on the list to some run queue,\nand clears glist. If there is no current P, they are added to the\nglobal queue, and up to npidle M's are started to run them.\nOtherwise, for each idle P, this adds a G to the global queue\nand starts an M. Any remaining G's are added to the current P's\nlocal run queue.\nThis may temporarily acquire sched.lock.\nCan run concurrently with GC.\n" [^{:tag (* gList)} glist]
  (when (.empty glist)
    (return))


  ;; Mark all the goroutines as runnable before we put them
  ;; on the run queues.
  (let [^{:tag (* g)} tail (zero (* g))
      ^{:line 4120} trace (traceAcquire)]
    (for [gp (.ptr (.-head glist))] (!= gp nil) (set! gp (.ptr (.-schedlink gp)))
      (set! tail gp)
      (casgstatus gp _Gwaiting _Grunnable)
      (when (.ok trace)
        (.GoUnpark trace gp 0)))


    (when (.ok trace)
      (traceRelease trace))


    ;; Turn the gList into a gQueue.
    (let [q (lit gQueue (.-head glist) (.guintptr tail) (.-size glist))]
      (set! @glist (lit gList))

      (let [startIdle ^{:go/end 4152} (fn [^int32 n]
            (for [] (> n 0) (dec! n)
              (let [mp (acquirem)] ; See comment in startm.
                (lock (addr (.-lock sched)))

                (let [(values pp _) (pidlegetSpinning 0)]
                  (when (== pp nil)
                    (unlock (addr (.-lock sched)))
                    (releasem mp)
                    (break))


                  (startm pp false true)
                  (unlock (addr (.-lock sched)))
                  (releasem mp)))))



          ^{:line 4154} pp (.ptr (.-p (.-m (getg))))]
        (when (== pp nil)
          (let [n (.-size q)]
            (lock (addr (.-lock sched)))
            (globrunqputbatch (addr q))
            (unlock (addr (.-lock sched)))
            (startIdle n)
            (return)))


        (let [^gQueue globq (zero gQueue)
            ^{:line 4165} npidle (.Load (.-npidle sched))]
          (for [] (and (> npidle 0) (not (.empty q))) (dec! npidle)
            (let [g (.pop q)]
              (.pushBack globq g)))

          (when (not (.empty globq))
            (let [n (.-size globq)]
              (lock (addr (.-lock sched)))
              (globrunqputbatch (addr globq))
              (unlock (addr (.-lock sched)))
              (startIdle n)))


          (when [(runqputbatch pp (addr q))] (not (.empty q))
            (lock (addr (.-lock sched)))
            (globrunqputbatch (addr q))
            (unlock (addr (.-lock sched))))


          ;; Some P's might have become idle after we loaded `sched.npidle`
          ;; but before any goroutines were added to the queue, which could
          ;; lead to idle P's when there is work available in the global queue.
          ;; That could potentially last until other goroutines become ready
          ;; to run. That said, we need to find a way to hedge
          ;;
          ;; Calling wakep() here is the best bet, it will do nothing in the
          ;; common case (no racing on `sched.npidle`), while it could wake one
          ;; more P to execute G's, which might end up with >1 P's: the first one
          ;; wakes another P and so forth until there is no more work, but this
          ;; ought to be an extremely rare case.
          ;;
          ;; Also see "Worker thread parking/unparking" comment at the top of the file for details.
          (wakep))))))




^{:go/end 4298} (go/func schedule "One round of scheduler: find a runnable goroutine and execute it.\nNever returns.\n" []
  (let [mp (.-m (getg))]

    (when (!= (.-locks mp) 0)
      (throw "schedule: holding locks"))


    (when (!= (.-lockedg mp) 0)
      (stoplockedm)
      (execute (.ptr (.-lockedg mp)) false)) ; Never returns.


    ;; We should not schedule away from a g that is executing a cgo call,
    ;; since the cgo call is using the m's g0 stack.
    (when (.-incgo mp)
      (throw "schedule: in cgo"))


    (let [
        ^{:line 4221 :go/label :top} pp (.ptr (.-p mp))]
      (set! (.-preempt pp) false)

      ;; Safety check: if we are spinning, the run queue should be empty.
      ;; Check this before calling checkTimers, as that might call
      ;; goready to put a ready goroutine on the local run queue.
      (when (and (.-spinning mp) ^:go/paren (or (!= (.-runnext pp) 0) (!= (.-runqhead pp) (.-runqtail pp))))
        (throw "schedule: spinning with local work"))


      (let [(values gp inheritTime tryWakeP) (findRunnable)] ; blocks until work is available

        ;; May be on a new P.
        (set! pp (.ptr (.-p mp)))

        ;; findRunnable may have collected an allp snapshot. The snapshot is
        ;; only required within findRunnable. Clear it to all GC to collect the
        ;; slice.
        (.clearAllpSnapshot mp)

        ;; If the P was assigned a next GC mark worker but findRunnable
        ;; selected anything else, release the worker so another P may run it.
        ;;
        ;; N.B. If this occurs because a higher-priority goroutine was selected
        ;; (trace reader), then tryWakeP is set, which will wake another P to
        ;; run the worker. If this occurs because the GC is no longer active,
        ;; there is no need to wakep.
        (.releaseNextGCMarkWorker gcController pp)

        (when (and (> (.-dontfreezetheworld debug) 0) (.Load freezing))
          ;; See comment in freezetheworld. We don't want to perturb
          ;; scheduler state, so we didn't gcstopm in findRunnable, but
          ;; also don't want to allow new goroutines to run.
          ;;
          ;; Deadlock here rather than in the findRunnable loop so if
          ;; findRunnable is stuck in a loop we don't perturb that
          ;; either.
          (lock (addr deadlock))
          (lock (addr deadlock)))


        ;; This thread is going to run a goroutine and is not spinning anymore,
        ;; so if it was marked as spinning we need to reset it now and potentially
        ;; start a new spinning M.
        (when (.-spinning mp)
          (resetspinning))


        (when (and (.-user (.-disable sched)) (not (schedEnabled gp)))
          ;; Scheduling of this goroutine is disabled. Put it on
          ;; the list of pending runnable goroutines for when we
          ;; re-enable user scheduling and look again.
          (lock (addr (.-lock sched)))
          (if (schedEnabled gp)
            ;; Something re-enabled scheduling while we
            ;; were acquiring the lock.
            (unlock (addr (.-lock sched)))
            (do
              (.pushBack (.-runnable (.-disable sched)) gp)
              (unlock (addr (.-lock sched)))
              (goto :top))))



        ;; If about to schedule a not-normal goroutine (a GCworker or tracereader),
        ;; wake a P if there is one.
        (when tryWakeP
          (wakep))

        (when (!= (.-lockedm gp) 0)
          ;; Hands off own p to the locked m,
          ;; then blocks waiting for a new p.
          (startlockedm gp)
          (goto :top))


        (execute gp inheritTime)))))









^{:go/end 4312} (go/func dropg "dropg removes the association between m and the current goroutine m->curg (gp for short).\nTypically a caller sets gp's status away from Grunning and then\nimmediately calls dropg to finish the job. The caller is also responsible\nfor arranging that gp will be restarted using ready at an\nappropriate time. After calling dropg and arranging for gp to be\nreadied later, the caller can do other work but eventually should\ncall schedule to restart the scheduling of goroutines on this m.\n" []
  (let [gp (getg)]

    (setMNoWB (addr (.-m (.-curg (.-m gp)))) nil)
    (setGNoWB (addr (.-curg (.-m gp))) nil)))


^{:go/breaks [4] :go/end 4317} (go/func parkunlock_c ^bool [^{:tag (* g)} gp ^unsafe/Pointer lock]
  (unlock (conv (* mutex) lock))
  true)



^{:go/end 4372} (go/func park_m "park continuation on g0.\n" [^{:tag (* g)} gp]
  (let [mp (.-m (getg))

      ^{:line 4323} trace (traceAcquire)

      ;; If g is in a synctest group, we don't want to let the group
      ;; become idle until after the waitunlockf (if any) has confirmed
      ;; that the park is happening.
      ;; We need to record gp.bubble here, since waitunlockf can change it.
      ^{:line 4329} bubble (.-bubble gp)]
    (when (!= bubble nil)
      (.incActive bubble))


    (when (.ok trace)
      ;; Trace the event before the transition. It may take a
      ;; stack trace, but we won't own the stack after the
      ;; transition anymore.
      (.GoPark trace (.-waitTraceBlockReason mp) (.-waitTraceSkip mp)))

    ;; N.B. Not using casGToWaiting here because the waitreason is
    ;; set by park_m's caller.
    (casgstatus gp _Grunning _Gwaiting)
    (when (.ok trace)
      (traceRelease trace))


    (dropg)

    (when [fn (.-waitunlockf mp)] (!= fn nil)
      (let [ok (go/call fn gp (.-waitlock mp))]
        (set! (.-waitunlockf mp) nil)
        (set! (.-waitlock mp) nil)
        (when (not ok)
          (let [trace (traceAcquire)]
            (casgstatus gp _Gwaiting _Grunnable)
            (when (!= bubble nil)
              (.decActive bubble))

            (when (.ok trace)
              (.GoUnpark trace gp 2)
              (traceRelease trace))

            (execute gp true))))) ; Schedule it back, never returns.



    (when (!= bubble nil)
      (.decActive bubble))


    (schedule)))


^{:go/end 4413} (go/func goschedImpl [^{:tag (* g)} gp ^bool preempted]
  (let [pp (.ptr (.-p (.-m gp)))
      ^{:line 4376} trace (traceAcquire)
      ^{:line 4377} status (readgstatus gp)]
    (when (!= (bit-and-not status _Gscan) _Grunning)
      (dumpgstatus gp)
      (throw "bad g status"))

    (when (.ok trace)
      ;; Trace the event before the transition. It may take a
      ;; stack trace, but we won't own the stack after the
      ;; transition anymore.
      (if preempted
        (.GoPreempt trace)

        (.GoSched trace)))


    (casgstatus gp _Grunning _Grunnable)
    (when (.ok trace)
      (traceRelease trace))


    (dropg)
    (if (and preempted (.Load (.-gcwaiting sched)))
      ;; If preempted for STW, keep the G on the local P in runnext
      ;; so it can keep running immediately after the STW.
      (runqput pp gp true)
      (do
        (lock (addr (.-lock sched)))
        (globrunqput gp)
        (unlock (addr (.-lock sched)))))


    (when mainStarted
      (wakep))


    (schedule)))



^{:go/end 4418} (go/func gosched_m "Gosched continuation on g0.\n" [^{:tag (* g)} gp]
  (goschedImpl gp false))



^{:go/end 4426} (go/func goschedguarded_m "goschedguarded is a forbidden-states-avoided version of gosched_m.\n" [^{:tag (* g)} gp]
  (when (not (canPreemptM (.-m gp)))
    (gogo (addr (.-sched gp)))) ; never return

  (goschedImpl gp false))


^{:go/end 4430} (go/func gopreempt_m [^{:tag (* g)} gp]
  (goschedImpl gp true))





^{:go/end 4502} (go/func ^:go/systemstack preemptPark "preemptPark parks gp and puts it in _Gpreempted.\n" [^{:tag (* g)} gp]
  (let [status (readgstatus gp)]
    (when (!= (bit-and-not status _Gscan) _Grunning)
      (dumpgstatus gp)
      (throw "bad g status"))


    (when (.-asyncSafePoint gp)
      ;; Double-check that async preemption does not
      ;; happen in SPWRITE assembly functions.
      ;; isAsyncSafePoint must exclude this case.
      (let [f (findfunc (.-pc (.-sched gp)))]
        (when (not (.valid f))
          (throw "preempt at unknown pc"))

        (when (!= (bit-and ^{:go/via [_func]} (.-flag f) abi/FuncFlagSPWrite) 0)
          (println "runtime: unexpected SPWRITE function" (funcname f) "in async preempt")
          (throw "preempt SPWRITE"))))



    ;; Transition from _Grunning to _Gscan|_Gpreempted. We can't
    ;; be in _Grunning when we dropg because then we'd be running
    ;; without an M, but the moment we're in _Gpreempted,
    ;; something could claim this G before we've fully cleaned it
    ;; up. Hence, we set the scan bit to lock down further
    ;; transitions until we can dropg.
    (casGToPreemptScan gp _Grunning (bit-or _Gscan _Gpreempted))

    ;; Be careful about ownership as we trace this next event.
    ;;
    ;; According to the tracer invariants (trace.go) it's unsafe
    ;; for us to emit an event for a goroutine we do not own.
    ;; The moment we CAS into _Gpreempted, suspendG could CAS the
    ;; goroutine to _Gwaiting, effectively taking ownership. All of
    ;; this could happen before we even get the chance to emit
    ;; an event. The end result is that the events could appear
    ;; out of order, and the tracer generally assumes the scheduler
    ;; takes care of the ordering between GoPark and GoUnpark.
    ;;
    ;; The answer here is simple: emit the event while we still hold
    ;; the _Gscan bit on the goroutine, since the _Gscan bit means
    ;; ownership over transitions.
    ;;
    ;; We still need to traceAcquire and traceRelease across the CAS
    ;; because the tracer could be what's calling suspendG in the first
    ;; place. This also upholds the tracer invariant that we must hold
    ;; traceAcquire/traceRelease across the transition. However, we
    ;; specifically *only* emit the event while we still have ownership.
    (let [trace (traceAcquire)]
      (when (.ok trace)
        (.GoPark trace traceBlockPreempted 0))


      ;; Drop the goroutine from the M. Only do this after the tracer has
      ;; emitted an event, because it needs the association for GoPark to
      ;; work correctly.
      (dropg)

      ;; Drop the scan bit and release the trace locker if necessary.
      (casfrom_Gscanstatus gp (bit-or _Gscan _Gpreempted) _Gpreempted)
      (when (.ok trace)
        (traceRelease trace))


      ;; All done.
      (schedule))))
















^{:go/end 4521} (go/func ^{:go/linkname "goyield"} goyield "goyield is like Gosched, but it:\n- emits a GoPreempt trace event instead of a GoSched trace event\n- puts the current G on the runq of the current P instead of the globrunq\n\ngoyield should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n  - github.com/sagernet/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (checkTimeouts)
  (mcall goyield_m))


^{:go/end 4539} (go/func goyield_m [^{:tag (* g)} gp]
  (let [trace (traceAcquire)
      ^{:line 4525} pp (.ptr (.-p (.-m gp)))]
    (when (.ok trace)
      ;; Trace the event before the transition. It may take a
      ;; stack trace, but we won't own the stack after the
      ;; transition anymore.
      (.GoPreempt trace))

    (casgstatus gp _Grunning _Grunnable)
    (when (.ok trace)
      (traceRelease trace))

    (dropg)
    (runqput pp gp false)
    (schedule)))



^{:go/end 4555} (go/func goexit1 "Finishes execution of the current goroutine.\n" []
  (when raceenabled
    (when [gp (getg)] (!= (.-bubble gp) nil)
      (racereleasemergeg gp (.raceaddr (.-bubble gp))))

    (racegoend))

  (let [trace (traceAcquire)]
    (when (.ok trace)
      (.GoEnd trace)
      (traceRelease trace))

    (mcall goexit0)))



^{:go/end 4568} (go/func goexit0 "goexit continuation on g0.\n" [^{:tag (* g)} gp]
  (when (and goexperiment/RuntimeSecret (> (.-secret gp) 0))
    ;; Erase the whole stack. This path only occurs when
    ;; runtime.Goexit is called from within a runtime/secret.Do call.
    (memclrNoHeapPointers (conv unsafe/Pointer (.-lo (.-stack gp))) (- (.-hi (.-stack gp)) (.-lo (.-stack gp)))))
  ;; Since this is running on g0, our registers are already zeroed from going through
  ;; mcall in secret mode.

  (gdestroy gp)
  (schedule))


^{:go/end 4636} (go/func gdestroy [^{:tag (* g)} gp]
  (let [mp (.-m (getg))
      ^{:line 4572} pp (.ptr (.-p mp))]

    (casgstatus gp _Grunning _Gdead)
    (.addScannableStack gcController pp (- (conv int64 (- (.-hi (.-stack gp)) (.-lo (.-stack gp))))))
    (when (isSystemGoroutine gp false)
      (.Add (.-ngsys sched) -1))

    (set! (.-m gp) nil)
    (let [locked (!= (.-lockedm gp) 0)]
      (set! (.-lockedm gp) 0)
      (set! (.-lockedg mp) 0)
      (set! (.-preemptStop gp) false)
      (set! (.-paniconfault gp) false)
      (set! (.-_defer gp) nil) ; should be true already but just in case.
      (set! (.-_panic gp) nil) ; non-nil for Goexit during panic. points at stack-allocated data.
      (set! (.-writebuf gp) nil)
      (set! (.-waitreason gp) waitReasonZero)
      (set! (.-param gp) nil)
      (set! (.-labels gp) nil)
      (set! (.-timer gp) nil)
      (set! (.-bubble gp) nil)
      (set! (.-fipsOnlyBypass gp) false)
      (set! (.-secret gp) 0)
      (set! (.-arbaceLocal gp) nil) ; Arbace: a reused g starts with an empty slot

      (when (and (!= gcBlackenEnabled 0) (> (.-gcAssistBytes gp) 0))
        ;; Flush assist credit to the global pool. This gives
        ;; better information to pacing if the application is
        ;; rapidly creating an exiting goroutines.
        (let [assistWorkPerByte (.Load (.-assistWorkPerByte gcController))
            ^{:line 4601} scanCredit (conv int64 (* assistWorkPerByte (conv float64 (.-gcAssistBytes gp))))]
          (.Add (.-bgScanCredit gcController) scanCredit)
          (set! (.-gcAssistBytes gp) 0)))


      (dropg)

      (when (== GOARCH "wasm") ; no threads yet on wasm
        (gfput pp gp)
        (return))


      (when (and locked (!= (.-lockedInt mp) 0))
        (print "runtime: mp.lockedInt = " (.-lockedInt mp) "\n")
        (when (.-isextra mp)
          (throw "runtime.Goexit called in a thread that was not created by the Go runtime"))

        (throw "exited a goroutine internally locked to the OS thread"))

      (gfput pp gp)
      (when locked
        ;; The goroutine may have locked this thread because
        ;; it put it in an unusual kernel state. Kill it
        ;; rather than returning it to the thread pool.

        ;; Return to mstart, which will release the P and exit
        ;; the thread.
        (if (!= GOOS "plan9") ; See golang.org/issue/22227.
          (gogo (addr (.-sched (.-g0 mp))))

          ;; Clear lockedExt on plan9 since we may end up re-using
          ;; this thread.
          (set! (.-lockedExt mp) 0))))))












^{:go/end 4668} (go/func ^:go/nosplit ^:go/nowritebarrierrec save "save updates getg().sched to refer to pc and sp so that a following\ngogo will restore pc and sp.\n\nsave must not have write barriers because invoking a write barrier\ncan clobber getg().sched.\n" [^uintptr pc ^uintptr sp ^uintptr bp]
  (let [gp (getg)]

    (when (or (== gp (.-g0 (.-m gp))) (== gp (.-gsignal (.-m gp))))
      ;; m.g0.sched is special and must describe the context
      ;; for exiting the thread. mstart1 writes to it directly.
      ;; m.gsignal.sched should not be used at all.
      ;; This check makes sure save calls do not accidentally
      ;; run in contexts where they'd write to system g's.
      (throw "save on system g not allowed"))


    (set! (.-pc (.-sched gp)) pc)
    (set! (.-sp (.-sched gp)) sp)
    (set! (.-lr (.-sched gp)) 0)
    (set! (.-bp (.-sched gp)) bp)
    ;; We need to ensure ctxt is zero, but can't have a write
    ;; barrier here. However, it should always already be zero.
    ;; Assert that.
    (when (!= (.-ctxt (.-sched gp)) nil)
      (badctxt))))



























^{:go/end 4807} (go/func ^:go/nosplit reentersyscall "The goroutine g is about to enter a system call.\nRecord that it's not using the cpu anymore.\nThis is called only from the go syscall library and cgocall,\nnot from the low-level system calls used by the runtime.\n\nEntersyscall cannot split the stack: the save must\nmake g->sched refer to the caller's stack segment, because\nentersyscall is going to return immediately after.\n\nNothing entersyscall calls can split the stack either.\nWe cannot safely move the stack during an active call to syscall,\nbecause we do not know which of the uintptr arguments are\nreally pointers (back into the stack).\nIn practice, this means that we make the fast path run through\nentersyscall doing no-split things, and the slow path has to use systemstack\nto run bigger things on the system stack.\n\nreentersyscall is the entry point used by cgo callbacks, where explicitly\nsaved SP and PC are restored. This is needed when exitsyscall will be called\nfrom a function further up in the call stack than the parent, as g->syscallsp\nmust always point to a valid stack frame. entersyscall below is the normal\nentry point for syscalls, which obtains the SP and PC from the caller.\n" [^uintptr pc ^uintptr sp ^uintptr bp]
  (let [gp (getg)]

    ;; Disable preemption because during this function g is in Gsyscall status,
    ;; but can have inconsistent g->sched, do not let GC observe it.
    (inc! (.-locks (.-m gp)))

    ;; This M may have a signal stack that is dirtied with secret information
    ;; (see package "runtime/secret"). Since it's about to go into a syscall for
    ;; an arbitrary amount of time and the G that put the secret info there
    ;; might have returned from secret.Do, we have to zero it out now, lest we
    ;; break the guarantee that secrets are purged by the next GC after a return
    ;; to secret.Do.
    ;;
    ;; It might be tempting to think that we only need to zero out this if we're
    ;; not running in secret mode anymore, but that leaves an ABA problem. The G
    ;; that put the secrets onto our signal stack may not be the one that is
    ;; currently executing.
    ;;
    ;; Logically, we should erase this when we lose our P, not when we enter the
    ;; syscall. This would avoid a zeroing in the case where the call returns
    ;; almost immediately. Since we use this path for cgo calls as well, these
    ;; fast "syscalls" are quite common. However, since we only erase the signal
    ;; stack if we were delivered a signal in secret mode and considering the
    ;; cross-thread synchronization cost for the P, it hardly seems worth it.
    ;;
    ;; TODO(dmo): can we encode the goid into mp.signalSecret and avoid the ABA problem?
    (when goexperiment/RuntimeSecret
      (eraseSecretsSignalStk))


    ;; Entersyscall must not call any function that might split/grow the stack.
    ;; (See details in comment above.)
    ;; Catch calls that might, by replacing the stack guard with something that
    ;; will trip any stack check and leaving a flag to tell newstack to die.
    (set! (.-stackguard0 gp) stackPreempt)
    (set! (.-throwsplit gp) true)

    ;; Copy the syscalltick over so we can identify if the P got stolen later.
    (set! (.-syscalltick (.-m gp)) (.-syscalltick (.ptr (.-p (.-m gp)))))

    (let [pp (.ptr (.-p (.-m gp)))]
      (when (!= (.-runSafePointFn pp) 0)
        ;; runSafePointFn may stack split if run on this stack
        (systemstack runSafePointFn))

      (.set (.-oldp (.-m gp)) pp)

      ;; Leave SP around for GC and traceback.
      (save pc sp bp)
      (set! (.-syscallsp gp) sp)
      (set! (.-syscallpc gp) pc)
      (set! (.-syscallbp gp) bp)

      ;; Double-check sp and bp.
      (when (or (< (.-syscallsp gp) (.-lo (.-stack gp))) (< (.-hi (.-stack gp)) (.-syscallsp gp)))
        (systemstack ^{:go/end 4753} (fn []
            (print "entersyscall inconsistent sp " (conv hex (.-syscallsp gp)) " [" (conv hex (.-lo (.-stack gp))) "," (conv hex (.-hi (.-stack gp))) "]\n")
            (throw "entersyscall"))))


      (when (or (and (!= (.-syscallbp gp) 0) (< (.-syscallbp gp) (.-lo (.-stack gp)))) (< (.-hi (.-stack gp)) (.-syscallbp gp)))
        (systemstack ^{:go/end 4759} (fn []
            (print "entersyscall inconsistent bp " (conv hex (.-syscallbp gp)) " [" (conv hex (.-lo (.-stack gp))) "," (conv hex (.-hi (.-stack gp))) "]\n")
            (throw "entersyscall"))))


      (let [trace (traceAcquire)]
        (when (.ok trace)
          ;; Emit a trace event. Notably, actually emitting the event must happen before
          ;; the casgstatus because it mutates the P, but the traceLocker must be held
          ;; across the casgstatus since we're transitioning out of _Grunning
          ;; (see trace.go invariants).
          (systemstack ^{:go/end 4769} (fn []
              (.GoSysCall trace)))

          ;; systemstack clobbered gp.sched, so restore it.
          (save pc sp bp))

        (when (.Load (.-gcwaiting sched))
          ;; Optimization: If there's a pending STW, do the equivalent of
          ;; entersyscallblock here at the last minute and immediately give
          ;; away our P.
          (systemstack ^{:go/end 4779} (fn []
              (entersyscallHandleGCWait trace)))

          ;; systemstack clobbered gp.sched, so restore it.
          (save pc sp bp))

        ;; As soon as we switch to _Gsyscall, we are in danger of losing our P.
        ;; We must not touch it after this point.
        ;;
        ;; Try to do a quick CAS to avoid calling into casgstatus in the common case.
        ;; If we have a bubble, we need to fall into casgstatus.
        (when (or (!= (.-bubble gp) nil) (not (.CompareAndSwap (.-atomicstatus gp) _Grunning _Gsyscall)))
          (casgstatus gp _Grunning _Gsyscall))

        (when staticLockRanking
          ;; casgstatus clobbers gp.sched via systemstack under staticLockRanking. Restore it.
          (save pc sp bp))

        (when (.ok trace)
          ;; N.B. We don't need to go on the systemstack because traceRelease is very
          ;; carefully recursively nosplit. This also means we don't need to worry
          ;; about clobbering gp.sched.
          (traceRelease trace))

        (when (.Load (.-sysmonwait sched))
          (systemstack entersyscallWakeSysmon)
          ;; systemstack clobbered gp.sched, so restore it.
          (save pc sp bp))

        (dec! (.-locks (.-m gp)))))))





(go/const ^{:val false :doc "debugExtendGrunningNoP is a debug mode that extends the windows in which\nwe're _Grunning without a P in order to try to shake out bugs with code\nassuming this state is impossible.\n"} debugExtendGrunningNoP false)















^{:go/end 4835} (go/func ^:go/nosplit ^{:go/linkname "entersyscall"} entersyscall "Standard syscall entry used by the go syscall library and normal cgo calls.\n\nThis is exported via linkname to assembly in the syscall package and x/sys.\n\nOther packages should not be accessing entersyscall directly,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  ;; N.B. getcallerfp cannot be written directly as argument in the call
  ;; to reentersyscall because it forces spilling the other arguments to
  ;; the stack. This results in exceeding the nosplit stack requirements
  ;; on some platforms.
  (let [fp (getcallerfp)]
    (reentersyscall (sys/GetCallerPC) (sys/GetCallerSP) fp)))


^{:go/end 4844} (go/func entersyscallWakeSysmon []
  (lock (addr (.-lock sched)))
  (when (.Load (.-sysmonwait sched))
    (.Store (.-sysmonwait sched) false)
    (notewakeup (addr (.-sysmonnote sched))))

  (unlock (addr (.-lock sched))))


^{:go/end 4868} (go/func entersyscallHandleGCWait [^traceLocker trace]
  (let [gp (getg)]

    (lock (addr (.-lock sched)))
    (when (> (.-stopwait sched) 0)
      ;; Set our P to _Pgcstop so the STW can take it.
      (let [pp (.ptr (.-p (.-m gp)))]
        (set! (.-m pp) 0)
        (set! (.-p (.-m gp)) 0)
        (atomic/Store (addr (.-status pp)) _Pgcstop)

        (when (.ok trace)
          (.ProcStop trace pp))

        (addGSyscallNoP (.-m gp)) ; We gave up our P voluntarily.
        (set! (.-gcStopTime pp) (nanotime))
        (inc! (.-syscalltick pp))
        (when [(dec! (.-stopwait sched))] (== (.-stopwait sched) 0)
          (notewakeup (addr (.-stopnote sched))))))


    (unlock (addr (.-lock sched)))))


;; The same as entersyscall(), but with a hint that the syscall is blocking.











^{:go/end 4952} (go/func ^:go/nosplit ^{:go/linkname "entersyscallblock"} entersyscallblock "entersyscallblock should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (let [gp (getg)]

    (inc! (.-locks (.-m gp))) ; see comment in entersyscall
    (set! (.-throwsplit gp) true)
    (set! (.-stackguard0 gp) stackPreempt) ; see comment in entersyscall
    (set! (.-syscalltick (.-m gp)) (.-syscalltick (.ptr (.-p (.-m gp)))))
    (inc! (.-syscalltick (.ptr (.-p (.-m gp)))))

    (addGSyscallNoP (.-m gp)) ; We're going to give up our P.

    ;; Leave SP around for GC and traceback.
    (let [pc (sys/GetCallerPC)
        ^{:line 4895} sp (sys/GetCallerSP)
        ^{:line 4896} bp (getcallerfp)]
      (save pc sp bp)
      (set! (.-syscallsp gp) (.-sp (.-sched gp)))
      (set! (.-syscallpc gp) (.-pc (.-sched gp)))
      (set! (.-syscallbp gp) (.-bp (.-sched gp)))
      (when (or (< (.-syscallsp gp) (.-lo (.-stack gp))) (< (.-hi (.-stack gp)) (.-syscallsp gp)))
        (let [sp1 sp
            ^{:line 4903} sp2 (.-sp (.-sched gp))
            ^{:line 4904} sp3 (.-syscallsp gp)]
          (systemstack ^{:go/end 4908} (fn []
              (print "entersyscallblock inconsistent sp " (conv hex sp1) " " (conv hex sp2) " " (conv hex sp3) " [" (conv hex (.-lo (.-stack gp))) "," (conv hex (.-hi (.-stack gp))) "]\n")
              (throw "entersyscallblock")))))



      ;; Once we switch to _Gsyscall, we can't safely touch
      ;; our P anymore, so we need to hand it off beforehand.
      ;; The tracer also needs to see the syscall before the P
      ;; handoff, so the order here must be (1) trace,
      ;; (2) handoff, (3) _Gsyscall switch.
      (let [trace (traceAcquire)]
        (systemstack ^{:go/end 4922} (fn []
            (when (.ok trace)
              (.GoSysCall trace))

            (handoffp (releasep))))

        ;; <--
        ;; Caution: we're in a small window where we are in _Grunning without a P.
        ;; -->
        (when debugExtendGrunningNoP
          (usleep 10))

        (casgstatus gp _Grunning _Gsyscall)
        (when (or (< (.-syscallsp gp) (.-lo (.-stack gp))) (< (.-hi (.-stack gp)) (.-syscallsp gp)))
          (systemstack ^{:go/end 4934} (fn []
              (print "entersyscallblock inconsistent sp " (conv hex sp) " " (conv hex (.-sp (.-sched gp))) " " (conv hex (.-syscallsp gp)) " [" (conv hex (.-lo (.-stack gp))) "," (conv hex (.-hi (.-stack gp))) "]\n")
              (throw "entersyscallblock"))))


        (when (or (and (!= (.-syscallbp gp) 0) (< (.-syscallbp gp) (.-lo (.-stack gp)))) (< (.-hi (.-stack gp)) (.-syscallbp gp)))
          (systemstack ^{:go/end 4940} (fn []
              (print "entersyscallblock inconsistent bp " (conv hex bp) " " (conv hex (.-bp (.-sched gp))) " " (conv hex (.-syscallbp gp)) " [" (conv hex (.-lo (.-stack gp))) "," (conv hex (.-hi (.-stack gp))) "]\n")
              (throw "entersyscallblock"))))


        (when (.ok trace)
          (systemstack ^{:go/end 4945} (fn []
              (traceRelease trace))))



        ;; Resave for traceback during blocked call.
        (save (sys/GetCallerPC) (sys/GetCallerSP) (getcallerfp))

        (dec! (.-locks (.-m gp)))))))






















^{:go/end 5116} (go/func ^:go/nosplit ^:go/nowritebarrierrec ^{:go/linkname "exitsyscall"} exitsyscall "The goroutine g exited its system call.\nArrange for it to run on a cpu again.\nThis is called only from the go syscall library, not\nfrom the low-level system calls used by the runtime.\n\nWrite barriers are not allowed because our P may have been stolen.\n\nThis is exported via linkname to assembly in the syscall package.\n\nexitsyscall should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (let [gp (getg)]

    (inc! (.-locks (.-m gp))) ; see comment in entersyscall
    (when (> (sys/GetCallerSP) (.-syscallsp gp))
      (throw "exitsyscall: syscall frame is no longer valid"))

    (set! (.-waitsince gp) 0)

    (when (== (.-stopwait sched) freezeStopWait)
      ;; Wedge ourselves if there's an outstanding freezetheworld.
      ;; If we transition to running, we might end up with our traceback
      ;; being taken twice.
      (systemstack ^{:go/end 4990} (fn []
          (lock (addr deadlock))
          (lock (addr deadlock)))))



    ;; Optimistically assume we're going to keep running, and switch to running.
    ;; Before this point, our P wiring is not ours. Once we get past this point,
    ;; we can access our P if we have it, otherwise we lost it.
    ;;
    ;; N.B. Because we're transitioning to _Grunning here, traceAcquire doesn't
    ;; need to be held ahead of time. We're effectively atomic with respect to
    ;; the tracer because we're non-preemptible and in the runtime. It can't stop
    ;; us to read a bad status.
    ;;
    ;; Try to do a quick CAS to avoid calling into casgstatus in the common case.
    ;; If we have a bubble, we need to fall into casgstatus.
    (when (or (!= (.-bubble gp) nil) (not (.CompareAndSwap (.-atomicstatus gp) _Gsyscall _Grunning)))
      (casgstatus gp _Gsyscall _Grunning))


    ;; Caution: we're in a window where we may be in _Grunning without a P.
    ;; Either we will grab a P or call exitsyscall0, where we'll switch to
    ;; _Grunnable.
    (when debugExtendGrunningNoP
      (usleep 10))


    ;; Grab and clear our old P.
    (let [oldp (.ptr (.-oldp (.-m gp)))]
      (.set (.-oldp (.-m gp)) nil)

      ;; Check if we still have a P, and if not, try to acquire an idle P.
      (let [pp (.ptr (.-p (.-m gp)))]
        (if (!= pp nil)
          ;; Fast path: we still have our P. Just emit a syscall exit event.
          (when [trace (traceAcquire)] (.ok trace)
            (systemstack ^{:go/end 5046} (fn []
                ;; The truth is we truly never lost the P, but syscalltick
                ;; is used to indicate whether the P should be treated as
                ;; lost anyway. For example, when syscalltick is trashed by
                ;; dropm.
                ;;
                ;; TODO(mknyszek): Consider a more explicit mechanism for this.
                ;; Then syscalltick doesn't need to be trashed, and can be used
                ;; exclusively by sysmon for deciding when it's time to retake.
                (if (== (.-syscalltick pp) (.-syscalltick (.-m gp)))
                  (.GoSysExit trace false)
                  (do
                    ;; Since we need to pretend we lost the P, but nobody ever
                    ;; took it, we need a ProcSteal event to model the loss.
                    ;; Then, continue with everything else we'd do if we lost
                    ;; the P.
                    (.ProcSteal trace pp)
                    (.ProcStart trace)
                    (.GoSysExit trace true)
                    (.GoStart trace)))

                (traceRelease trace))))


          (do
            ;; Slow path: we lost our P. Try to get another one.
            (systemstack ^{:go/end 5064} (fn []
                ;; Try to get some other P.
                (when [pp (exitsyscallTryGetP oldp)] (!= pp nil)
                  ;; Install the P.
                  (acquirepNoTrace pp)

                  ;; We're going to start running again, so emit all the relevant events.
                  (when [trace (traceAcquire)] (.ok trace)
                    (.ProcStart trace)
                    (.GoSysExit trace true)
                    (.GoStart trace)
                    (traceRelease trace)))))



            (set! pp (.ptr (.-p (.-m gp))))))


        ;; If we have a P, clean up and exit.
        (when (!= pp nil)
          (when (.-active goroutineProfile)
            ;; Make sure that gp has had its stack written out to the goroutine
            ;; profile, exactly as it was when the goroutine profiler first
            ;; stopped the world.
            (systemstack ^{:go/end 5076} (fn []
                (tryRecordGoroutineProfileWB gp))))



          ;; Increment the syscalltick for P, since we're exiting a syscall.
          (inc! (.-syscalltick pp))

          ;; Garbage collector isn't running (since we are),
          ;; so okay to clear syscallsp.
          (set! (.-syscallsp gp) 0)
          (dec! (.-locks (.-m gp)))
          (if (.-preempt gp)
            ;; Restore the preemption request in case we cleared it in newstack.
            (set! (.-stackguard0 gp) stackPreempt)

            ;; Otherwise restore the real stackGuard, we clobbered it in entersyscall/entersyscallblock.
            (set! (.-stackguard0 gp) (+ (.-lo (.-stack gp)) stackGuard)))

          (set! (.-throwsplit gp) false)

          (when (and (.-user (.-disable sched)) (not (schedEnabled gp)))
            ;; Scheduling of this goroutine is disabled.
            (Gosched))

          (return))

        ;; Slowest path: We couldn't get a P, so call into the scheduler.
        (dec! (.-locks (.-m gp)))

        ;; Call the scheduler.
        (mcall exitsyscallNoP)

        ;; Scheduler returned, so we're allowed to run now.
        ;; Delete the syscallsp information that we left for
        ;; the garbage collector during the system call.
        ;; Must wait until now because until gosched returns
        ;; we don't know for sure that the garbage collector
        ;; is not running.
        (set! (.-syscallsp gp) 0)
        (inc! (.-syscalltick (.ptr (.-p (.-m gp)))))
        (set! (.-throwsplit gp) false)))))








^{:go/breaks [6] :go/end 5150} (go/func ^:go/systemstack exitsyscallTryGetP "exitsyscall's attempt to try to get any P, if it's missing one.\nReturns true on success.\n\nMust execute on the systemstack because exitsyscall is nosplit.\n" ^{:tag (* p)} [^{:tag (* p)} oldp]
  ;; Try to steal our old P back.
  (when (!= oldp nil)
    (when [(values thread ok) (setBlockOnExitSyscall oldp)] ok
      (.takeP thread)
      (decGSyscallNoP (.-m (getg))) ; We got a P for ourselves.
      (.resume thread)
      (return oldp)))



  ;; Try to get an idle P.
  (when (!= (.-pidle sched) 0)
    (lock (addr (.-lock sched)))
    (let [(values pp _) (pidleget 0)]
      (when (and (!= pp nil) (.Load (.-sysmonwait sched)))
        (.Store (.-sysmonwait sched) false)
        (notewakeup (addr (.-sysmonnote sched))))

      (unlock (addr (.-lock sched)))
      (when (!= pp nil)
        (decGSyscallNoP (.-m (getg))) ; We got a P for ourselves.
        (return pp))))


  nil)








^{:go/end 5207} (go/func ^:go/nowritebarrierrec exitsyscallNoP "exitsyscall slow path on g0.\nFailed to acquire P, enqueue gp as runnable.\n\nCalled via mcall, so gp is the calling g from this M.\n" [^{:tag (* g)} gp]
  (traceExitingSyscall)
  (let [trace (traceAcquire)]
    (casgstatus gp _Grunning _Grunnable)
    (traceExitedSyscall)
    (when (.ok trace)
      ;; Write out syscall exit eagerly.
      ;;
      ;; It's important that we write this *after* we know whether we
      ;; lost our P or not (determined by exitsyscallfast).
      (.GoSysExit trace true)
      (traceRelease trace))

    (decGSyscallNoP (.-m (getg)))
    (dropg)
    (lock (addr (.-lock sched)))
    (let [^{:tag (* p)} pp (zero (* p))]
      (when (schedEnabled gp)
        (set! (values pp _) (pidleget 0)))

      (let [^bool locked (zero bool)]
        (cond (== pp nil) (do
            (globrunqput gp)

            ;; Below, we stoplockedm if gp is locked. globrunqput releases
            ;; ownership of gp, so we must check if gp is locked prior to
            ;; committing the release by unlocking sched.lock, otherwise we
            ;; could race with another M transitioning gp from unlocked to
            ;; locked.
            (set! locked (!= (.-lockedm gp) 0)))
          (.Load (.-sysmonwait sched)) (do
            (.Store (.-sysmonwait sched) false)
            (notewakeup (addr (.-sysmonnote sched)))))

        (unlock (addr (.-lock sched)))
        (when (!= pp nil)
          (acquirep pp)
          (execute gp false)) ; Never returns.

        (when locked
          ;; Wait until another thread schedules gp and so m again.
          ;;
          ;; N.B. lockedm must be this M, as this g was running on this M
          ;; before entersyscall.
          (stoplockedm)
          (execute gp false)) ; Never returns.

        (stopm)
        (schedule))))) ; Never returns.








^{:go/end 5227} (go/func ^:go/nosplit addGSyscallNoP "addGSyscallNoP must be called when a goroutine in a syscall loses its P.\nThis function updates all relevant accounting.\n\nnosplit because it's called on the syscall paths.\n" [^{:tag (* m)} mp]
  ;; It's safe to read isExtraInC here because it's only mutated
  ;; outside of _Gsyscall, and we know this thread is attached
  ;; to a goroutine in _Gsyscall and blocked from exiting.
  (when (not (.-isExtraInC mp))
    ;; Increment nGsyscallNoP since we're taking away a P
    ;; from a _Gsyscall goroutine, but only if isExtraInC
    ;; is not set on the M. If it is, then this thread is
    ;; back to being a full C thread, and will just inflate
    ;; the count of not-in-go goroutines. See go.dev/issue/76435.
    (.Add (.-nGsyscallNoP sched) 1)))









^{:go/end 5242} (go/func ^:go/nosplit decGSyscallNoP "decGSsyscallNoP must be called whenever a goroutine in a syscall without\na P exits the system call. This function updates all relevant accounting.\n\nnosplit because it's called from dropm.\n" [^{:tag (* m)} mp]
  ;; Update nGsyscallNoP, but only if this is not a thread coming
  ;; out of C. See the comment in addGSyscallNoP. This logic must match,
  ;; to avoid unmatched increments and decrements.
  (when (not (.-isExtraInC mp))
    (.Add (.-nGsyscallNoP sched) -1)))















^{:go/end 5271} (go/func ^:go/nosplit ^{:go/linkname "syscall_runtime_BeforeFork syscall.runtime_BeforeFork"} syscall_runtime_BeforeFork "Called from syscall package before fork.\n\nsyscall_runtime_BeforeFork is for package syscall,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (let [gp (.-curg (.-m (getg)))]

    ;; Block signals during a fork, so that the child does not run
    ;; a signal handler before exec if a signal is sent to the process
    ;; group. See issue #18600.
    (inc! (.-locks (.-m gp)))
    (sigsave (addr (.-sigmask (.-m gp))))
    (sigblock false)

    ;; This function is called before fork in syscall package.
    ;; Code between fork and exec must not allocate memory nor even try to grow stack.
    ;; Here we spoil g.stackguard0 to reliably detect any attempts to grow stack.
    ;; runtime_AfterFork will undo this in parent process, but not in child.
    (set! (.-stackguard0 gp) stackFork)))














^{:go/end 5294} (go/func ^:go/nosplit ^{:go/linkname "syscall_runtime_AfterFork syscall.runtime_AfterFork"} syscall_runtime_AfterFork "Called from syscall package after fork in parent.\n\nsyscall_runtime_AfterFork is for package syscall,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (let [gp (.-curg (.-m (getg)))]

    ;; See the comments in beforefork.
    (set! (.-stackguard0 gp) (+ (.-lo (.-stack gp)) stackGuard))

    (msigrestore (.-sigmask (.-m gp)))

    (dec! (.-locks (.-m gp)))))




(go/var ^{:tag bool :doc "inForkedChild is true while manipulating signals in the child process.\nThis is used to avoid calling libc functions in case we are using vfork.\n"} inForkedChild)




















^{:go/end 5333} (go/func ^:go/nosplit ^:go/nowritebarrierrec ^{:go/linkname "syscall_runtime_AfterForkInChild syscall.runtime_AfterForkInChild"} syscall_runtime_AfterForkInChild "Called from syscall package after fork in child.\nIt resets non-sigignored signals to the default handler, and\nrestores the signal mask in preparation for the exec.\n\nBecause this might be called during a vfork, and therefore may be\ntemporarily sharing address space with the parent process, this must\nnot change any global variables or calling into C code that may do so.\n\nsyscall_runtime_AfterForkInChild is for package syscall,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  ;; It's OK to change the global variable inForkedChild here
  ;; because we are going to change it back. There is no race here,
  ;; because if we are sharing address space with the parent process,
  ;; then the parent process can not be running concurrently.
  (set! inForkedChild true)

  (clearSignalHandlers)

  ;; When we are the child we are the only thread running,
  ;; so we know that nothing else has changed gp.m.sigmask.
  (msigrestore (.-sigmask (.-m (getg))))

  (set! inForkedChild false))





(go/var ^{:tag atomic/Int32 :doc "pendingPreemptSignals is the number of preemption signals\nthat have been sent but not received. This is only used on Darwin.\nFor #41702.\n"} pendingPreemptSignals)




^{:go/end 5354} (go/func ^{:go/linkname "syscall_runtime_BeforeExec syscall.runtime_BeforeExec"} syscall_runtime_BeforeExec "Called from syscall package before Exec.\n" []
  ;; Prevent thread creation during exec.
  (.lock execLock)

  ;; On Darwin, wait for all pending preemption signals to
  ;; be received. See issue #41702.
  (when (or (== GOOS "darwin") (== GOOS "ios"))
    (while (> (.Load pendingPreemptSignals) 0)
      (osyield))))







^{:go/end 5361} (go/func ^{:go/linkname "syscall_runtime_AfterExec syscall.runtime_AfterExec"} syscall_runtime_AfterExec "Called from syscall package after Exec.\n" []
  (.unlock execLock))



^{:go/end 5381} (go/func malg "Allocate a new g, with a stack big enough for stacksize bytes.\n" ^{:tag (* g)} [^int32 stacksize]
  (let [newg (new g)]
    (when (>= stacksize 0)
      (set! stacksize (round2 (+ stackSystem stacksize)))
      (systemstack ^{:go/end 5373} (fn []
          (set! (.-stack newg) (stackalloc (conv uint32 stacksize)))
          (when valgrindenabled
            (set! (.-valgrindStackID newg) (valgrindRegisterStack (conv unsafe/Pointer (.-lo (.-stack newg))) (conv unsafe/Pointer (.-hi (.-stack newg))))))))


      (set! (.-stackguard0 newg) (+ (.-lo (.-stack newg)) stackGuard))
      (set! (.-stackguard1 newg) (bit-not (conv uintptr 0)))
      ;; Clear the bottom word of the stack. We record g
      ;; there on gsignal stack during VDSO on ARM and ARM64.
      (set! @(conv (* uintptr) (conv unsafe/Pointer (.-lo (.-stack newg)))) 0))

    ^{:line 5380} newg))





^{:go/end 5399} (go/func newproc "Create a new g running fn.\nPut it on the queue of g's waiting to run.\nThe compiler turns a go statement into a call to this.\n" [^{:tag (* funcval)} fn]
  (let [gp (getg)
      ^{:line 5388} pc (sys/GetCallerPC)]
    (systemstack ^{:go/end 5398} (fn []
        (let [newg (newproc1 fn gp pc false waitReasonZero)

            ^{:line 5392} pp (.ptr (.-p (.-m (getg))))]
          (runqput pp newg true)

          (when mainStarted
            (wakep)))))))







^{:go/end 5530} (go/func newproc1 "Create a new g in state _Grunnable (or _Gwaiting if parked is true), starting at fn.\ncallerpc is the address of the go statement that created this. The caller is responsible\nfor adding the new g to the scheduler. If parked is true, waitreason must be non-zero.\n" ^{:tag (* g)} [^{:tag (* funcval)} fn ^{:tag (* g)} callergp ^uintptr callerpc ^bool parked ^waitReason waitreason]
  (when (== fn nil)
    (fatal "go of nil func value"))


  (let [mp (acquirem) ; disable preemption because we hold M and P in local vars.
      ^{:line 5410} pp (.ptr (.-p mp))
      ^{:line 5411} newg (gfget pp)]
    (when (== newg nil)
      (set! newg (malg stackMin))
      (casgstatus newg _Gidle _Gdead)
      (allgadd newg)) ; publishes with a g->status of Gdead so GC scanner doesn't look at uninitialized stack.

    (when (== (.-hi (.-stack newg)) 0)
      (throw "newproc1: newg missing stack"))


    (when (!= (readgstatus newg) _Gdead)
      (throw "newproc1: new g is not Gdead"))


    (let [totalSize (conv uintptr (+ (* 4 goarch/PtrSize) sys/MinFrameSize))] ; extra space in case of reads slightly beyond frame
      (set! totalSize (alignUp totalSize sys/StackAlign))
      (let [sp (- (.-hi (.-stack newg)) totalSize)]
        (when usesLR
          ;; caller's LR
          (set! @(conv (* uintptr) (conv unsafe/Pointer sp)) 0)
          (prepGoExitFrame sp))

        (when (== GOARCH "arm64")
          ;; caller's FP
          (set! @(conv (* uintptr) (conv unsafe/Pointer (- sp goarch/PtrSize))) 0))


        (memclrNoHeapPointers (conv unsafe/Pointer (addr (.-sched newg))) (unsafe/Sizeof (.-sched newg)))
        (set! (.-sp (.-sched newg)) sp)
        (set! (.-stktopsp newg) sp)
        (set! (.-pc (.-sched newg)) (+ (abi/FuncPCABI0 goexit) sys/PCQuantum)) ; +PCQuantum so that previous instruction is in same function
        (set! (.-g (.-sched newg)) (conv guintptr (conv unsafe/Pointer newg)))
        (gostartcallfn (addr (.-sched newg)) fn)
        (set! (.-parentGoid newg) (.-goid callergp))
        (set! (.-gopc newg) callerpc)
        (set! (.-ancestors newg) (saveAncestors callergp))
        (set! (.-startpc newg) (.-fn fn))
        (.Store (.-runningCleanups newg) false)
        (if (isSystemGoroutine newg false)
          (.Add (.-ngsys sched) 1)
          (do
            ;; Only user goroutines inherit synctest groups and pprof labels.
            (set! (.-bubble newg) (.-bubble callergp))
            (when (!= (.-curg mp) nil)
              (set! (.-labels newg) (.-labels (.-curg mp))))

            (when (.-active goroutineProfile)
              ;; A concurrent goroutine profile is running. It should include
              ;; exactly the set of goroutines that were alive when the goroutine
              ;; profiler first stopped the world. That does not include newg, so
              ;; mark it as not needing a profile before transitioning it from
              ;; _Gdead.
              (.Store (.-goroutineProfiled newg) goroutineProfileSatisfied))))


        ;; Track initial transition?
        (set! (.-trackingSeq newg) (conv uint8 (cheaprand)))
        (when (== (% (.-trackingSeq newg) gTrackingPeriod) 0)
          (set! (.-tracking newg) true))

        (.addScannableStack gcController pp (conv int64 (- (.-hi (.-stack newg)) (.-lo (.-stack newg)))))

        ;; Get a goid and switch to runnable. This needs to happen under traceAcquire
        ;; since it's a goroutine transition. See tracer invariants in trace.go.
        (let [trace (traceAcquire)
            ^{:line 5476 :tag uint32} status _Grunnable]
          (when parked
            (set! status _Gwaiting)
            (set! (.-waitreason newg) waitreason))

          (when (== (.-goidcache pp) (.-goidcacheend pp))
            ;; Sched.goidgen is the last allocated id,
            ;; this batch must be [sched.goidgen+1, sched.goidgen+GoidCacheBatch].
            ;; At startup sched.goidgen=0, so main goroutine receives goid=1.
            (set! (.-goidcache pp) (.Add (.-goidgen sched) _GoidCacheBatch))
            (set! (.-goidcache pp) - (- _GoidCacheBatch 1))
            (set! (.-goidcacheend pp) (+ (.-goidcache pp) _GoidCacheBatch)))

          (set! (.-goid newg) (.-goidcache pp))
          (casgstatus newg _Gdead status)
          (inc! (.-goidcache pp))
          (.reset (.-trace newg))
          (when (.ok trace)
            (.GoCreate trace newg (.-startpc newg) parked)
            (traceRelease trace))


          ;; fips140 bubble
          (set! (.-fipsOnlyBypass newg) (.-fipsOnlyBypass callergp))

          ;; dit bubble
          (set! (.-ditWanted newg) (.-ditWanted callergp))

          (when (and goexperiment/RuntimeSecret (> (.-secret callergp) 0))
            ;; while it might seem weird to have a non-zero gp.secret value
            ;; with no calls to secret.Do on the stack, this case is handled
            ;; just fine by the cleanup logic in goexit0
            ;; TODO: secret mode is invisible to the user if they don't ask about it via secret.Enabled
            ;; and can have severe performance penalties (at time of writing, wrapping the entire
            ;; tls handshake resulted in a 30% slowdown of the benchmarks).
            ;; Whether a goroutine is running in secret mode should be more visible,
            ;; maybe with a stack frame or some sort of bubble inspecting mechanism
            (set! (.-secret newg) 1))


          ;; Set up race context.
          (when raceenabled
            (set! (.-racectx newg) (racegostart callerpc))
            (set! (.-raceignore newg) 0)
            (when (!= (.-labels newg) nil)
              ;; See note in proflabel.go on labelSync's role in synchronizing
              ;; with the reads in the signal handler.
              (racereleasemergeg newg (conv unsafe/Pointer (addr labelSync)))))


          (inc! (.-goroutinesCreated pp))
          (releasem mp)

          ^{:line 5529} newg)))))





^{:go/end 5564} (go/func saveAncestors "saveAncestors copies previous ancestors of the given caller g and\nincludes info for the current caller into a new set of tracebacks for\na g being created.\n" ^{:tag (* (slice ancestorInfo))} [^{:tag (* g)} callergp]
  ;; Copy all prior info, except for the root goroutine (goid 0).
  (when (or (<= (.-tracebackancestors debug) 0) (== (.-goid callergp) 0))
    (return nil))

  (let [^{:tag (slice ancestorInfo)} callerAncestors (zero (slice ancestorInfo))]
    (when (!= (.-ancestors callergp) nil)
      (set! callerAncestors @(.-ancestors callergp)))

    (let [n (+ (conv int32 (len callerAncestors)) 1)]
      (when (> n (.-tracebackancestors debug))
        (set! n (.-tracebackancestors debug)))

      (let [ancestors (make (slice ancestorInfo) n)]
        (copy (subslice ancestors 1) callerAncestors)

        (let [^{:tag (array tracebackInnerFrames uintptr)} pcs (zero (array tracebackInnerFrames uintptr))
            ^{:line 5552} npcs (gcallers callergp 0 (subslice pcs))
            ^{:line 5553} ipcs (make (slice uintptr) npcs)]
          (copy ipcs (subslice pcs))
          (aset ancestors 0 ^{:go/breaks [2 4 6]} (lit ancestorInfo
              :pcs ipcs
              :goid (.-goid callergp)
              :gopc (.-gopc callergp)))


          (let [ancestorsp (new (slice ancestorInfo))]
            (set! @ancestorsp ancestors)
            ^{:line 5563} ancestorsp))))))




^{:go/end 5606} (go/func gfput "Put on gfree list.\nIf local list is too long, transfer a batch to the global list.\n" [^{:tag (* p)} pp ^{:tag (* g)} gp]
  (when (!= (readgstatus gp) _Gdead)
    (throw "gfput: bad status (not Gdead)"))


  (let [stksize (- (.-hi (.-stack gp)) (.-lo (.-stack gp)))]

    (when (!= stksize (conv uintptr startingStackSize))
      ;; non-standard stack size - free it.
      (stackfree (.-stack gp))
      (set! (.-lo (.-stack gp)) 0)
      (set! (.-hi (.-stack gp)) 0)
      (set! (.-stackguard0 gp) 0)
      (when valgrindenabled
        (valgrindDeregisterStack (.-valgrindStackID gp))
        (set! (.-valgrindStackID gp) 0)))



    (.push (.-gFree pp) gp)
    (when (>= (.-size (.-gFree pp)) 64)
      (let [
          ^{:line 5590 :tag gQueue} stackQ (zero gQueue)
          ^{:line 5591 :tag gQueue} noStackQ (zero gQueue)]

        (while (>= (.-size (.-gFree pp)) 32)
          (let [gp (.pop (.-gFree pp))]
            (if (== (.-lo (.-stack gp)) 0)
              (.push noStackQ gp)

              (.push stackQ gp))))


        (lock (addr (.-lock (.-gFree sched))))
        (.pushAll (.-noStack (.-gFree sched)) noStackQ)
        (.pushAll (.-stack (.-gFree sched)) stackQ)
        (unlock (addr (.-lock (.-gFree sched))))))))





^{:go/end 5669} (go/func gfget "Get from gfree list.\nIf local list is empty, grab a batch from global list.\n" ^{:tag (* g)} [^{:tag (* p)} pp]
  (label :retry
    (when (and (.empty (.-gFree pp)) ^:go/paren (or (not (.empty (.-stack (.-gFree sched)))) (not (.empty (.-noStack (.-gFree sched))))))
      (lock (addr (.-lock (.-gFree sched))))
      ;; Move a batch of free Gs to the P.
      (while (< (.-size (.-gFree pp)) 32)
        ;; Prefer Gs with stacks.
        (let [gp (.pop (.-stack (.-gFree sched)))]
          (when (== gp nil)
            (set! gp (.pop (.-noStack (.-gFree sched))))
            (when (== gp nil)
              (break)))


          (.push (.-gFree pp) gp)))

      (unlock (addr (.-lock (.-gFree sched))))
      (goto :retry)))

  (let [gp (.pop (.-gFree pp))]
    (when (== gp nil)
      (return nil))

    (when (and (!= (.-lo (.-stack gp)) 0) (!= (- (.-hi (.-stack gp)) (.-lo (.-stack gp))) (conv uintptr startingStackSize)))
      ;; Deallocate old stack. We kept it in gfput because it was the
      ;; right size when the goroutine was put on the free list, but
      ;; the right size has changed since then.
      (systemstack ^{:go/end 5646} (fn []
          (stackfree (.-stack gp))
          (set! (.-lo (.-stack gp)) 0)
          (set! (.-hi (.-stack gp)) 0)
          (set! (.-stackguard0 gp) 0)
          (when valgrindenabled
            (valgrindDeregisterStack (.-valgrindStackID gp))
            (set! (.-valgrindStackID gp) 0)))))



    (if (== (.-lo (.-stack gp)) 0) (do
        ;; Stack was deallocated in gfput or just above. Allocate a new one.
        (systemstack ^{:go/end 5655} (fn []
            (set! (.-stack gp) (stackalloc startingStackSize))
            (when valgrindenabled
              (set! (.-valgrindStackID gp) (valgrindRegisterStack (conv unsafe/Pointer (.-lo (.-stack gp))) (conv unsafe/Pointer (.-hi (.-stack gp))))))))


        (set! (.-stackguard0 gp) (+ (.-lo (.-stack gp)) stackGuard)))
      (do
        (when raceenabled
          (racemalloc (conv unsafe/Pointer (.-lo (.-stack gp))) (- (.-hi (.-stack gp)) (.-lo (.-stack gp)))))

        (when msanenabled
          (msanmalloc (conv unsafe/Pointer (.-lo (.-stack gp))) (- (.-hi (.-stack gp)) (.-lo (.-stack gp)))))

        (when asanenabled
          (asanunpoison (conv unsafe/Pointer (.-lo (.-stack gp))) (- (.-hi (.-stack gp)) (.-lo (.-stack gp)))))))


    ^{:line 5668} gp))



^{:go/end 5689} (go/func gfpurge "Purge all cached G's from gfree list to the global list.\n" [^{:tag (* p)} pp]
  (let [
      ^{:line 5674 :tag gQueue} stackQ (zero gQueue)
      ^{:line 5675 :tag gQueue} noStackQ (zero gQueue)]

    (while (not (.empty (.-gFree pp)))
      (let [gp (.pop (.-gFree pp))]
        (if (== (.-lo (.-stack gp)) 0)
          (.push noStackQ gp)

          (.push stackQ gp))))


    (lock (addr (.-lock (.-gFree sched))))
    (.pushAll (.-noStack (.-gFree sched)) noStackQ)
    (.pushAll (.-stack (.-gFree sched)) stackQ)
    (unlock (addr (.-lock (.-gFree sched))))))



^{:go/end 5694} (go/func Breakpoint "Breakpoint executes a breakpoint trap.\n" []
  (breakpoint))







^{:go/end 5709} (go/func ^:go/nosplit dolockOSThread "dolockOSThread is called by LockOSThread and lockOSThread below\nafter they modify m.locked. Do not allow preemption during this call,\nor else the m might be different in this function than in the caller.\n" []
  (when (or (== GOARCH "wasm") ; no threads on wasm yet
      (== GOOS "tamago")) ; Ms are bound to P on tamago
    (return))

  (let [gp (getg)]
    (.set (.-lockedg (.-m gp)) gp)
    (.set (.-lockedm gp) (.-m gp))))


















^{:go/end 5741} (go/func ^:go/nosplit LockOSThread "LockOSThread wires the calling goroutine to its current operating system thread.\nThe calling goroutine will always execute in that thread,\nand no other goroutine will execute in it,\nuntil the calling goroutine has made as many calls to\n[UnlockOSThread] as to LockOSThread.\nIf the calling goroutine exits without unlocking the thread,\nthe thread will be terminated.\n\nAll init functions are run on the startup thread. Calling LockOSThread\nfrom an init function will cause the main function to be invoked on\nthat thread.\n\nA goroutine should call LockOSThread before calling OS services or\nnon-Go library functions that depend on per-thread state.\n" []
  (when (and (== (atomic/Load (addr (.-haveTemplateThread newmHandoff))) 0) (!= GOOS "plan9"))
    ;; If we need to start a new thread from the locked
    ;; thread, we need the template thread. Start it now
    ;; while we're in a known-good state.
    (startTemplateThread))

  (let [gp (getg)]
    (inc! (.-lockedExt (.-m gp)))
    (when (== (.-lockedExt (.-m gp)) 0)
      (dec! (.-lockedExt (.-m gp)))
      (panic "LockOSThread nesting overflow"))

    (dolockOSThread)))



^{:go/end 5747} (go/func ^:go/nosplit lockOSThread []
  (inc! (.-lockedInt (.-m (getg))))
  (dolockOSThread))







^{:go/end 5765} (go/func ^:go/nosplit dounlockOSThread "dounlockOSThread is called by UnlockOSThread and unlockOSThread below\nafter they update m->locked. Do not allow preemption during this call,\nor else the m might be in different in this function than in the caller.\n" []
  (when (or (== GOARCH "wasm") ; no threads on wasm yet
      (== GOOS "tamago")) ; Ms are bound to P on tamago
    (return))

  (let [gp (getg)]
    (when (or (!= (.-lockedInt (.-m gp)) 0) (!= (.-lockedExt (.-m gp)) 0))
      (return))

    (set! (.-lockedg (.-m gp)) 0)
    (set! (.-lockedm gp) 0)))
















^{:go/end 5788} (go/func ^:go/nosplit UnlockOSThread "UnlockOSThread undoes an earlier call to LockOSThread.\nIf this drops the number of active LockOSThread calls on the\ncalling goroutine to zero, it unwires the calling goroutine from\nits fixed operating system thread.\nIf there are no active LockOSThread calls, this is a no-op.\n\nBefore calling UnlockOSThread, the caller must ensure that the OS\nthread is suitable for running other goroutines. If the caller made\nany permanent changes to the state of the thread that would affect\nother goroutines, it should not call this function and thus leave\nthe goroutine locked to the OS thread until the goroutine (and\nhence the thread) exits.\n" []
  (let [gp (getg)]
    (when (== (.-lockedExt (.-m gp)) 0)
      (return))

    (dec! (.-lockedExt (.-m gp)))
    (dounlockOSThread)))



^{:go/end 5801} (go/func ^:go/nosplit unlockOSThread []
  (when (== GOOS "tamago")
    (return)) ; Ms are bound to P on tamago

  (let [gp (getg)]
    (when (== (.-lockedInt (.-m gp)) 0)
      (systemstack badunlockosthread))

    (dec! (.-lockedInt (.-m gp)))
    (dounlockOSThread)))


^{:go/end 5805} (go/func badunlockosthread []
  (throw "runtime: internal error: misuse of lockOSThread/unlockOSThread"))


^{:go/end 5822} (go/func gcount ^int32 [^bool includeSys]
  (let [n (- (conv int32 (atomic/Loaduintptr (addr allglen))) (.-size (.-stack (.-gFree sched))) (.-size (.-noStack (.-gFree sched))))]
    (when (not includeSys)
      (set! n - (.Load (.-ngsys sched))))

    (range [_ pp allp]
      (set! n - (.-size (.-gFree pp))))


    ;; All these variables can be changed concurrently, so the result can be inconsistent.
    ;; But at least the current goroutine is running.
    (when (< n 1)
      (set! n 1))

    ^{:line 5821} n))






^{:go/end 5830} (go/func ^{:go/linkname "goroutineleakcount runtime/pprof.runtime_goroutineleakcount"} goroutineleakcount "goroutineleakcount returns the number of leaked goroutines last reported by\nthe runtime.\n" ^int []
  (.-count (.-goroutineLeak work)))


^{:go/end 5834} (go/func mcount ^int32 []
  (conv int32 (- (.-mnext sched) (.-nmfreed sched))))


(go/var ^{:tag (struct ^atomic/Uint32 signalLock ^{:tag atomic/Int32 :doc "Must hold signalLock to write. Reads may be lock-free, but\nsignalLock should be taken to synchronize with changes.\n"} hz)} prof)







^{:go/end 5844} (go/func _System [] (_System))
^{:go/end 5845} (go/func _ExternalCode [] (_ExternalCode))
^{:go/end 5846} (go/func _LostExternalCode [] (_LostExternalCode))
^{:go/end 5847} (go/func _GC [] (_GC))
^{:go/end 5848} (go/func _LostSIGPROFDuringAtomic64 [] (_LostSIGPROFDuringAtomic64))
^{:go/end 5849} (go/func _LostContendedRuntimeLock [] (_LostContendedRuntimeLock))
^{:go/end 5850} (go/func _VDSO [] (_VDSO))





^{:go/end 5972} (go/func ^:go/nowritebarrierrec sigprof "Called if we receive a SIGPROF signal.\nCalled by the signal handler, may run during STW.\n" [^uintptr pc ^uintptr sp ^uintptr lr ^{:tag (* g)} gp ^{:tag (* m)} mp]
  (when (== (.Load (.-hz prof)) 0)
    (return))


  ;; If mp.profilehz is 0, then profiling is not enabled for this thread.
  ;; We must check this to avoid a deadlock between setcpuprofilerate
  ;; and the call to cpuprof.add, below.
  (when (and (!= mp nil) (== (.-profilehz mp) 0))
    (return))


  ;; On mips{,le}/arm, 64bit atomics are emulated with spinlocks, in
  ;; internal/runtime/atomic. If SIGPROF arrives while the program is inside
  ;; the critical section, it creates a deadlock (when writing the sample).
  ;; As a workaround, create a counter of SIGPROFs while in critical section
  ;; to store the count, and pass it to sigprof.add() later when SIGPROF is
  ;; received from somewhere else (with _LostSIGPROFDuringAtomic64 as pc).
  (when (or (== GOARCH "mips") (== GOARCH "mipsle") (== GOARCH "arm"))
    (when [f (findfunc pc)] (.valid f)
      (when (stringslite/HasPrefix (funcname f) "internal/runtime/atomic")
        (inc! (.-lostAtomic cpuprof))
        (return)))


    (when (and (== GOARCH "arm") (< goarm 7) (== GOOS "linux") (== (bit-and pc 0xffff0000) 0xffff0000))
      ;; internal/runtime/atomic functions call into kernel
      ;; helpers on arm < 7. See
      ;; internal/runtime/atomic/sys_linux_arm.s.
      (inc! (.-lostAtomic cpuprof))
      (return)))



  ;; Profiling runs concurrently with GC, so it must not allocate.
  ;; Set a trap in case the code does allocate.
  ;; Note that on windows, one thread takes profiles of all the
  ;; other threads, so mp is usually not getg().m.
  ;; In fact mp may not even be stopped.
  ;; See golang.org/issue/17165.
  (inc! (.-mallocing (.-m (getg))))

  (let [^unwinder u (zero unwinder)
      ^{:line 5899 :tag (array maxCPUProfStack uintptr)} stk (zero (array maxCPUProfStack uintptr))
      ^{:line 5900} n 0]
    (cond (and (> (.-ncgo mp) 0) (!= (.-curg mp) nil) (!= (.-syscallpc (.-curg mp)) 0) (!= (.-syscallsp (.-curg mp)) 0))
      (let [cgoOff 0]
        ;; Check cgoCallersUse to make sure that we are not
        ;; interrupting other code that is fiddling with
        ;; cgoCallers.  We are running in a signal handler
        ;; with all signals blocked, so we don't have to worry
        ;; about any other code interrupting us.
        (when (and (== (.Load (.-cgoCallersUse mp)) 0) (!= (.-cgoCallers mp) nil) (!= (aget (.-cgoCallers mp) 0) 0))
          (while (and (< cgoOff (len (.-cgoCallers mp))) (!= (aget (.-cgoCallers mp) cgoOff) 0))
            (inc! cgoOff))

          (set! n + (copy (subslice stk) (subslice (.-cgoCallers mp) _ cgoOff)))
          (aset (.-cgoCallers mp) 0 0))


        ;; Collect Go stack that leads to the cgo call.
        (.initAt u (.-syscallpc (.-curg mp)) (.-syscallsp (.-curg mp)) 0 (.-curg mp) unwindSilentErrors))
      (and (usesLibcall) (!= (.-libcallg mp) 0) (!= (.-libcallpc mp) 0) (!= (.-libcallsp mp) 0))
      ;; Libcall, i.e. runtime syscall on windows.
      ;; Collect Go stack that leads to the call.
      (.initAt u (.-libcallpc mp) (.-libcallsp mp) 0 (.ptr (.-libcallg mp)) unwindSilentErrors)
      (and (!= mp nil) (!= (.-vdsoSP mp) 0))
      ;; VDSO call, e.g. nanotime1 on Linux.
      ;; Collect Go stack that leads to the call.
      (.initAt u (.-vdsoPC mp) (.-vdsoSP mp) 0 gp (bit-or unwindSilentErrors unwindJumpStack)) :else

      (.initAt u pc sp lr gp (bit-or unwindSilentErrors unwindTrap unwindJumpStack)))

    (set! n + (tracebackPCs (addr u) 0 (subslice stk n)))

    (when (<= n 0)
      ;; Normal traceback is impossible or has failed.
      ;; Account it against abstract "System" or "GC".
      (set! n 2)
      (cond (inVDSOPage pc)
        (set! pc (+ (abi/FuncPCABIInternal _VDSO) sys/PCQuantum))
        (> pc (.-etext firstmoduledata))
        ;; "ExternalCode" is better than "etext".
        (set! pc (+ (abi/FuncPCABIInternal _ExternalCode) sys/PCQuantum)))

      (aset stk 0 pc)
      (if (!= (.-preemptoff mp) "")
        (aset stk 1 (+ (abi/FuncPCABIInternal _GC) sys/PCQuantum))

        (aset stk 1 (+ (abi/FuncPCABIInternal _System) sys/PCQuantum))))



    (when (!= (.Load (.-hz prof)) 0)
      ;; Note: it can happen on Windows that we interrupted a system thread
      ;; with no g, so gp could nil. The other nil checks are done out of
      ;; caution, but not expected to be nil in practice.
      (let [^{:tag (* unsafe/Pointer)} tagPtr (zero (* unsafe/Pointer))]
        (when (and (!= gp nil) (!= (.-m gp) nil) (!= (.-curg (.-m gp)) nil))
          (set! tagPtr (addr (.-labels (.-curg (.-m gp))))))

        (.add cpuprof tagPtr (subslice stk _ n))

        (let [gprof gp
            ^{:line 5960 :tag (* m)} mp (zero (* m))
            ^{:line 5961 :tag (* p)} pp (zero (* p))]
          (when (and (!= gp nil) (!= (.-m gp) nil))
            (when (!= (.-curg (.-m gp)) nil)
              (set! gprof (.-curg (.-m gp))))

            (set! mp (.-m gp))
            (set! pp (.ptr (.-p (.-m gp)))))

          (traceCPUSample gprof mp pp (subslice stk _ n)))))

    (dec! (.-mallocing (.-m (getg))))))




^{:go/end 6010} (go/func setcpuprofilerate "setcpuprofilerate sets the CPU profiling rate to hz times per second.\nIf hz <= 0, setcpuprofilerate turns off CPU profiling.\n" [^int32 hz]
  ;; Force sane arguments.
  (when (< hz 0)
    (set! hz 0))


  ;; Disable preemption, otherwise we can be rescheduled to another thread
  ;; that has profiling enabled.
  (let [gp (getg)]
    (inc! (.-locks (.-m gp)))

    ;; Stop profiler on this thread so that it is safe to lock prof.
    ;; if a profiling signal came in while we had prof locked,
    ;; it would deadlock.
    (setThreadCPUProfiler 0)

    (while (not (.CompareAndSwap (.-signalLock prof) 0 1))
      (osyield))

    (when (!= (.Load (.-hz prof)) hz)
      (setProcessCPUProfiler hz)
      (.Store (.-hz prof) hz))

    (.Store (.-signalLock prof) 0)

    (lock (addr (.-lock sched)))
    (set! (.-profilehz sched) hz)
    (unlock (addr (.-lock sched)))

    (when (!= hz 0)
      (setThreadCPUProfiler hz))


    (dec! (.-locks (.-m gp)))))




^{:go/end 6049} (go/method init "init initializes pp, which may be a freshly allocated p or a\npreviously destroyed p, and transitions it to status _Pgcstop.\n" [^{:tag (* p)} pp ^int32 id]
  (set! (.-id pp) id)
  (set! (.-id (.-gcw pp)) id)
  (set! (.-status pp) _Pgcstop)
  (set! (.-sudogcache pp) (subslice (.-sudogbuf pp) _ 0))
  (set! (.-deferpool pp) (subslice (.-deferpoolbuf pp) _ 0))
  (.reset (.-wbBuf pp))
  (when (== (.-mcache pp) nil)
    (if (== id 0) (do
        (when (== mcache0 nil)
          (throw "missing mcache?"))

        ;; Use the bootstrap mcache0. Only one P will get
        ;; mcache0: the one with ID 0.
        (set! (.-mcache pp) mcache0))

      (set! (.-mcache pp) (allocmcache))))


  (when (and raceenabled (== (.-raceprocctx pp) 0))
    (if (== id 0) (do
        (set! (.-raceprocctx pp) raceprocctx0)
        (set! raceprocctx0 0)) ; bootstrap

      (set! (.-raceprocctx pp) (raceproccreate))))


  (lockInit (addr (.-mu (.-timers pp))) lockRankTimers)

  ;; This P may get timers when it starts running. Set the mask here
  ;; since the P may not go through pidleget (notably P 0 on startup).
  (.set timerpMask id)
  ;; Similarly, we may not go through pidleget before this P starts
  ;; running if it is P 0 on startup.
  (.clear idlepMask id))






^{:go/end 6128} (go/method destroy "destroy releases all of the resources associated with pp and\ntransitions it to status _Pdead.\n\nsched.lock must be held and the world must be stopped.\n" [^{:tag (* p)} pp]
  (assertLockHeld (addr (.-lock sched)))
  (assertWorldStopped)

  ;; Move all runnable goroutines to the global queue
  (while (!= (.-runqhead pp) (.-runqtail pp))
    ;; Pop from tail of local queue
    (dec! (.-runqtail pp))
    (let [gp (.ptr (aget (.-runq pp) (% (.-runqtail pp) (conv uint32 (len (.-runq pp))))))]
      ;; Push onto head of global queue
      (globrunqputhead gp)))

  (when (!= (.-runnext pp) 0)
    (globrunqputhead (.ptr (.-runnext pp)))
    (set! (.-runnext pp) 0))


  ;; Move all timers to the local P.
  (.take (.-timers (.ptr (.-p (.-m (getg))))) (addr (.-timers pp)))

  ;; No need to flush p's write barrier buffer or span queue, as Ps
  ;; cannot be destroyed during the mark phase.
  (when [phase gcphase] (!= phase _GCoff)
    (println "runtime: p id" (.-id pp) "destroyed during GC phase" phase)
    (throw "P destroyed while GC is running"))

  ;; We should free the queues though.
  (.destroy (.-spanq (.-gcw pp)))

  (clear (subslice (.-sudogbuf pp)))
  (set! (.-sudogcache pp) (subslice (.-sudogbuf pp) _ 0))
  (set! (.-pinnerCache pp) nil)
  (clear (subslice (.-deferpoolbuf pp)))
  (set! (.-deferpool pp) (subslice (.-deferpoolbuf pp) _ 0))
  (systemstack ^{:go/end 6098} (fn []
      (for [i 0] (< i (.-len (.-mspancache pp))) (inc! i)
        ;; Safe to call since the world is stopped.
        (.free (.-spanalloc mheap_) (conv unsafe/Pointer (aget (.-buf (.-mspancache pp)) i))))

      (set! (.-len (.-mspancache pp)) 0)
      (lock (addr (.-lock mheap_)))
      (.flush (.-pcache pp) (addr (.-pages mheap_)))
      (unlock (addr (.-lock mheap_)))))

  (freemcache (.-mcache pp))
  (set! (.-mcache pp) nil)
  (gfpurge pp)
  (when raceenabled
    (when (!= (.-raceCtx (.-timers pp)) 0)
      ;; The race detector code uses a callback to fetch
      ;; the proc context, so arrange for that callback
      ;; to see the right thing.
      ;; This hack only works because we are the only
      ;; thread running.
      (let [mp (.-m (getg))
          ^{:line 6110} phold (.ptr (.-p mp))]
        (.set (.-p mp) pp)

        (racectxend (.-raceCtx (.-timers pp)))
        (set! (.-raceCtx (.-timers pp)) 0)

        (.set (.-p mp) phold)))

    (raceprocdestroy (.-raceprocctx pp))
    (set! (.-raceprocctx pp) 0))

  (set! (.-gcAssistTime pp) 0)
  (set! (.-queued gcCleanups) + (.-cleanupsQueued pp))
  (set! (.-cleanupsQueued pp) 0)
  (.Add (.-goroutinesCreated sched) (conv int64 (.-goroutinesCreated pp)))
  (set! (.-goroutinesCreated pp) 0)
  (.free (.-xRegs pp))
  (set! (.-status pp) _Pdead))










^{:go/end 6359} (go/func procresize "Change number of processors.\n\nsched.lock must be held, and the world must be stopped.\n\ngcworkbufs must not be being modified by either the GC or the write barrier\ncode, so the GC must not be running if the number of Ps actually changes.\n\nReturns list of Ps with local work, they need to be scheduled by the caller.\n" ^{:tag (* p)} [^int32 nprocs]
  (assertLockHeld (addr (.-lock sched)))
  (assertWorldStopped)

  (let [old gomaxprocs]
    (when (or (< old 0) (<= nprocs 0))
      (throw "procresize: invalid arg"))

    (let [trace (traceAcquire)]
      (when (.ok trace)
        (.Gomaxprocs trace nprocs)
        (traceRelease trace))


      ;; update statistics
      (let [now (nanotime)]
        (when (!= (.-procresizetime sched) 0)
          (set! (.-totaltime sched) + (* (conv int64 old) (- now (.-procresizetime sched)))))

        (set! (.-procresizetime sched) now)

        ;; Grow allp if necessary.
        (when (> nprocs (conv int32 (len allp)))
          ;; Synchronize with retake, which could be running
          ;; concurrently since it doesn't run on a P.
          (lock (addr allpLock))
          (if (<= nprocs (conv int32 (cap allp)))
            (set! allp (subslice allp _ nprocs))

            (let [nallp (make (slice (* p)) nprocs)]
              ;; Copy everything up to allp's cap so we
              ;; never lose old allocated Ps.
              (copy nallp (subslice allp _ (cap allp)))
              (set! allp nallp)))


          (set! idlepMask (.resize idlepMask nprocs))
          (set! timerpMask (.resize timerpMask nprocs))
          (set! (.-spanqMask work) (.resize (.-spanqMask work) nprocs))
          (unlock (addr allpLock)))


        ;; initialize new P's
        (for [i old] (< i nprocs) (inc! i)
          (let [pp (aget allp i)]
            (when (== pp nil)
              (set! pp (new p)))

            (.init pp i)
            (atomicstorep (conv unsafe/Pointer (addr (aget allp i))) (conv unsafe/Pointer pp))))


        (let [gp (getg)]
          (if (and (!= (.-p (.-m gp)) 0) (< (.-id (.ptr (.-p (.-m gp)))) nprocs)) (do
              ;; continue to use the current P
              (set! (.-status (.ptr (.-p (.-m gp)))) _Prunning)
              (.prepareForSweep (.-mcache (.ptr (.-p (.-m gp))))))
            (do
              ;; release the current P and acquire allp[0].
              ;;
              ;; We must do this before destroying our current P
              ;; because p.destroy itself has write barriers, so we
              ;; need to do that from a valid P.
              (when (!= (.-p (.-m gp)) 0)
                (let [trace (traceAcquire)]
                  (when (.ok trace)
                    ;; Pretend that we were descheduled
                    ;; and then scheduled again to keep
                    ;; the trace consistent.
                    (.GoSched trace)
                    (.ProcStop trace (.ptr (.-p (.-m gp))))
                    (traceRelease trace))

                  (set! (.-m (.ptr (.-p (.-m gp)))) 0)))

              (set! (.-p (.-m gp)) 0)
              (let [pp (aget allp 0)]
                (set! (.-m pp) 0)
                (set! (.-status pp) _Pidle)
                (acquirep pp)
                (let [trace (traceAcquire)]
                  (when (.ok trace)
                    (.GoStart trace)
                    (traceRelease trace))))))



          ;; g.m.p is now set, so we no longer need mcache0 for bootstrapping.
          (set! mcache0 nil)

          ;; release resources from unused P's
          (for [i nprocs] (< i old) (inc! i)
            (let [pp (aget allp i)]
              (.destroy pp)))
          ;; can't free P itself because it can be referenced by an M in syscall


          ;; Trim allp.
          (when (!= (conv int32 (len allp)) nprocs)
            (lock (addr allpLock))
            (set! allp (subslice allp _ nprocs))
            (set! idlepMask (.resize idlepMask nprocs))
            (set! timerpMask (.resize timerpMask nprocs))
            (set! (.-spanqMask work) (.resize (.-spanqMask work) nprocs))
            (unlock (addr allpLock)))


          ;; Assign Ms to Ps with runnable goroutines.
          (let [^{:tag (* p)} runnablePs (zero (* p))
              ^{:line 6247 :tag (* p)} runnablePsNeedM (zero (* p))
              ^{:line 6248 :tag (* p)} idlePs (zero (* p))]
            (for [i (- nprocs 1)] (>= i 0) (dec! i)
              (let [pp (aget allp i)]
                (when (== (.ptr (.-p (.-m gp))) pp)
                  (continue))

                (set! (.-status pp) _Pidle)
                (when (runqempty pp)
                  (.set (.-link pp) idlePs)
                  (set! idlePs pp)
                  (continue))


                ;; Prefer to run on the most recent M if it is
                ;; available.
                ;;
                ;; Ps with no oldm (or for which oldm is already taken
                ;; by an earlier P), we delay until all oldm Ps are
                ;; handled. Otherwise, mget may return an M that a
                ;; later P has in oldm.
                (let [^{:tag (* m)} mp (zero (* m))]
                  (when [oldm (.get (.-oldm pp))] (!= oldm nil)
                    ;; Returns nil if oldm is not idle.
                    (set! mp (mgetSpecific oldm)))

                  (when (== mp nil)
                    ;; Call mget later.
                    (.set (.-link pp) runnablePsNeedM)
                    (set! runnablePsNeedM pp)
                    (continue))

                  (.set (.-m pp) mp)
                  (.set (.-link pp) runnablePs)
                  (set! runnablePs pp))))

            ;; Assign Ms to remaining runnable Ps without usable oldm. See comment
            ;; above.
            (while (!= runnablePsNeedM nil)
              (let [pp runnablePsNeedM]
                (set! runnablePsNeedM (.ptr (.-link pp)))

                (let [mp (mget)]
                  (.set (.-m pp) mp)
                  (.set (.-link pp) runnablePs)
                  (set! runnablePs pp))))


            ;; Now that we've assigned Ms to Ps with runnable goroutines, assign GC
            ;; mark workers to remaining idle Ps, if needed.
            ;;
            ;; By assigning GC workers to Ps here, we slightly speed up starting
            ;; the world, as we will start enough Ps to run all of the user
            ;; goroutines and GC mark workers all at once, rather than using a
            ;; sequence of wakep calls as each P's findRunnable realizes it needs
            ;; to run a mark worker instead of a user goroutine.
            ;;
            ;; By assigning GC workers to Ps only _after_ previously-running Ps are
            ;; assigned Ms, we ensure that goroutines previously running on a P
            ;; continue to run on the same P, with GC mark workers preferring
            ;; previously-idle Ps. This helps prevent goroutines from shuffling
            ;; around too much across STW.
            ;;
            ;; N.B., if there aren't enough Ps left in idlePs for all of the GC
            ;; mark workers, then findRunnable will still choose to run mark
            ;; workers on Ps assigned above.
            ;;
            ;; N.B., we do this during any STW in the mark phase, not just the
            ;; sweep termination STW that starts the mark phase. gcBgMarkWorker
            ;; always preempts by removing itself from the P, so even unrelated
            ;; STWs during the mark require that Ps reselect mark workers upon
            ;; restart.
            (when (!= gcBlackenEnabled 0)
              (while (!= idlePs nil)
                (let [pp idlePs

                    (values ok _) (.assignWaitingGCWorker gcController pp now)]
                  (when (not ok)
                    ;; No more mark workers needed.
                    (break))


                  ;; Got a worker, P is now runnable.
                  ;;
                  ;; mget may return nil if there aren't enough Ms, in
                  ;; which case startTheWorldWithSema will start one.
                  ;;
                  ;; N.B. findRunnableGCWorker will make the worker G
                  ;; itself runnable.
                  (set! idlePs (.ptr (.-link pp)))
                  (let [mp (mget)]
                    (.set (.-m pp) mp)
                    (.set (.-link pp) runnablePs)
                    (set! runnablePs pp)))))



            ;; Finally, any remaining Ps are truly idle.
            (while (!= idlePs nil)
              (let [pp idlePs]
                (set! idlePs (.ptr (.-link pp)))
                (pidleput pp now)))


            (.reset stealOrder (conv uint32 nprocs))
            (let [^{:tag (* int32)} int32p (addr gomaxprocs)] ; make compiler check that gomaxprocs is an int32
              (atomic/Store (conv (* uint32) (conv unsafe/Pointer int32p)) (conv uint32 nprocs))
              (when (!= old nprocs)
                ;; Notify the limiter that the amount of procs has changed.
                (.resetCapacity gcCPULimiter now nprocs))

              ^{:line 6358} runnablePs)))))))








^{:go/end 6377} (go/func ^:go/yeswritebarrierrec acquirep "Associate p and the current m.\n\nThis function is allowed to have write barriers even if the caller\nisn't because it immediately acquires pp.\n" [^{:tag (* p)} pp]
  ;; Do the work.
  (acquirepNoTrace pp)

  ;; Emit the event.
  (let [trace (traceAcquire)]
    (when (.ok trace)
      (.ProcStart trace)
      (traceRelease trace))))






^{:go/end 6396} (go/func ^:go/yeswritebarrierrec acquirepNoTrace "Internals of acquirep, just skipping the trace events.\n" [^{:tag (* p)} pp]
  ;; Do the part that isn't allowed to have write barriers.
  (wirep pp)

  ;; Have p; write barriers now allowed.

  ;; The M we're associating with will be the old M after the next
  ;; releasep. We must set this here because write barriers are not
  ;; allowed in releasep.
  (set! (.-oldm pp) (.-self (.ptr (.-m pp))))

  ;; Perform deferred mcache flush before this P can allocate
  ;; from a potentially stale mcache.
  (.prepareForSweep (.-mcache pp)))








^{:go/end 6429} (go/func ^:go/nosplit ^:go/nowritebarrierrec wirep "wirep is the first step of acquirep, which actually associates the\ncurrent M to pp. This is broken out so we can disallow write\nbarriers for this part, since we don't yet have a P.\n" [^{:tag (* p)} pp]
  (let [gp (getg)]

    (when (!= (.-p (.-m gp)) 0)
      ;; Call on the systemstack to avoid a nosplit overflow build failure
      ;; on some platforms when built with -N -l. See #64113.
      (systemstack ^{:go/end 6412} (fn []
          (throw "wirep: already in go"))))


    (when (or (!= (.-m pp) 0) (!= (.-status pp) _Pidle))
      ;; Call on the systemstack to avoid a nosplit overflow build failure
      ;; on some platforms when built with -N -l. See #64113.
      (systemstack ^{:go/end 6424} (fn []
          (let [id (conv int64 0)]
            (when (!= (.-m pp) 0)
              (set! id (.-id (.ptr (.-m pp)))))

            (print "wirep: p->m=" (.-m pp) "(" id ") p->status=" (.-status pp) "\n")
            (throw "wirep: invalid p state")))))


    (.set (.-p (.-m gp)) pp)
    (.set (.-m pp) (.-m gp))
    (set! (.-status pp) _Prunning)))



^{:go/end 6439} (go/func releasep "Disassociate p and the current m.\n" ^{:tag (* p)} []
  (let [trace (traceAcquire)]
    (when (.ok trace)
      (.ProcStop trace (.ptr (.-p (.-m (getg)))))
      (traceRelease trace))

    (releasepNoTrace)))



^{:go/end 6461} (go/func releasepNoTrace "Disassociate p and the current m without tracing an event.\n" ^{:tag (* p)} []
  (let [gp (getg)]

    (when (== (.-p (.-m gp)) 0)
      (throw "releasep: invalid arg"))

    (let [pp (.ptr (.-p (.-m gp)))]
      (when (or (!= (.ptr (.-m pp)) (.-m gp)) (!= (.-status pp) _Prunning))
        (print "releasep: m=" (.-m gp) " m->p=" (.ptr (.-p (.-m gp))) " p->m=" (conv hex (.-m pp)) " p->status=" (.-status pp) "\n")
        (throw "releasep: invalid p state"))


      ;; P must clear if nextGCMarkWorker if it stops.
      (.releaseNextGCMarkWorker gcController pp)

      (set! (.-p (.-m gp)) 0)
      (set! (.-m pp) 0)
      (set! (.-status pp) _Pidle)
      ^{:line 6460} pp)))


^{:go/end 6470} (go/func incidlelocked [^int32 v]
  (lock (addr (.-lock sched)))
  (set! (.-nmidlelocked sched) + v)
  (when (> v 0)
    (checkdead))

  (unlock (addr (.-lock sched))))





^{:go/end 6577} (go/func checkdead "Check for deadlock situation.\nThe check is based on number of running M's, if 0 -> deadlock.\nsched.lock must be held.\n" []
  (assertLockHeld (addr (.-lock sched)))

  ;; For -buildmode=c-shared or -buildmode=c-archive it's OK if
  ;; there are no running goroutines. The calling program is
  ;; assumed to be running.
  ;; One exception is Wasm, which is single-threaded. If we are
  ;; in Go and all goroutines are blocked, it deadlocks.
  (when (and ^:go/paren (or islibrary isarchive) (!= GOARCH "wasm"))
    (return))


  ;; If we are dying because of a signal caught on an already idle thread,
  ;; freezetheworld will cause all running threads to block.
  ;; And runtime will essentially enter into deadlock state,
  ;; except that there is a thread that will call exit soon.
  (when (> (.Load panicking) 0)
    (return))


  ;; If we are not running under cgo, but we have an extra M then account
  ;; for it. (It is possible to have an extra M on Windows without cgo to
  ;; accommodate callbacks created by syscall.NewCallback. See issue #6751
  ;; for details.)
  (let [^int32 run0 (zero int32)]
    (when (and (not iscgo) cgoHasExtraM (> (.Load extraMLength) 0))
      (set! run0 1))


    (let [run (- (mcount) (.-nmidle sched) (.-nmidlelocked sched) (.-nmsys sched))]
      (when (> run run0)
        (return))

      (when (< run 0)
        (print "runtime: checkdead: nmidle=" (.-nmidle sched) " nmidlelocked=" (.-nmidlelocked sched) " mcount=" (mcount) " nmsys=" (.-nmsys sched) "\n")
        (unlock (addr (.-lock sched)))
        (throw "checkdead: inconsistent counts"))


      (let [grunning 0]
        (forEachG ^{:go/end 6531} (fn [^{:tag (* g)} gp]
            (when (isSystemGoroutine gp false)
              (return))

            (let [s (readgstatus gp)]
              (switch (bit-and-not s _Gscan)
                (case [_Gwaiting
                    ^{:line 6522} _Gpreempted]
                  (inc! grunning))
                (case [_Grunnable
                    ^{:line 6525} _Grunning
                    ^{:line 6526} _Gsyscall]
                  (print "runtime: checkdead: find g " (.-goid gp) " in status " s "\n")
                  (unlock (addr (.-lock sched)))
                  (throw "checkdead: runnable g"))))))


        (when (== grunning 0) ; possible if main goroutine calls runtime·Goexit()
          (unlock (addr (.-lock sched))) ; unlock so that GODEBUG=scheddetail=1 doesn't hang
          (fatal "no goroutines (main called runtime.Goexit) - deadlock!"))


        ;; Maybe jump time forward for playground.
        (when (!= faketime 0)
          (when [when (timeSleepUntil)] (< when maxWhen)
            (set! faketime when)

            ;; Start an M to steal the timer.
            (let [(values pp _) (pidleget faketime)]
              (when (== pp nil)
                ;; There should always be a free P since
                ;; nothing is running.
                (unlock (addr (.-lock sched)))
                (throw "checkdead: no p for timer"))

              (let [mp (mget)]
                (when (== mp nil)
                  ;; There should always be a free M since
                  ;; nothing is running.
                  (unlock (addr (.-lock sched)))
                  (throw "checkdead: no m for timer"))

                ;; M must be spinning to steal. We set this to be
                ;; explicit, but since this is the only M it would
                ;; become spinning on its own anyways.
                (.Add (.-nmspinning sched) 1)
                (set! (.-spinning mp) true)
                (.set (.-nextp mp) pp)
                (notewakeup (addr (.-park mp)))
                (return)))))



        ;; There are no goroutines running, so we can look at the P's.
        (range [_ pp allp]
          (when (> (len (.-heap (.-timers pp))) 0)
            (return)))



        (unlock (addr (.-lock sched))) ; unlock so that GODEBUG=scheddetail=1 doesn't hang
        (fatal "all goroutines are asleep - deadlock!")))))







(go/var ^{:tag int64 :doc "forcegcperiod is the maximum time in nanoseconds between garbage\ncollections. If we go this long without a garbage collection, one\nis forced to run.\n\nThis is a variable for testing purposes. It normally doesn't change.\n"} forcegcperiod (* 2 60 1.0E9))




(go/const ^{:val true :doc "haveSysmon indicates whether there is sysmon thread support.\n\nNo threads on wasm yet, so no sysmon.\n"} haveSysmon (and (!= GOARCH "wasm") (!= GOOS "tamago")))




^{:go/end 6725} (go/func ^:go/nowritebarrierrec sysmon "Always runs without a P, so write barriers are not allowed.\n" []
  (lock (addr (.-lock sched)))
  (inc! (.-nmsys sched))
  (checkdead)
  (unlock (addr (.-lock sched)))

  (let [lastgomaxprocs (conv int64 0)
      ^{:line 6601} lasttrace (conv int64 0)
      ^{:line 6602} idle 0 ; how many cycles in succession we had not wokeup somebody
      ^{:line 6603} delay (conv uint32 0)]

    (while true
      (cond (== idle 0) ; start with 20us sleep...
        (set! delay 20)
        (> idle 50) ; start doubling the sleep after 1ms...
        (set! delay * 2))

      (when (> delay (* 10 1000)) ; up to 10ms
        (set! delay (* 10 1000)))

      (usleep delay)

      ;; sysmon should not enter deep sleep if schedtrace is enabled so that
      ;; it can print that information at the right time.
      ;;
      ;; It should also not enter deep sleep if there are any active P's so
      ;; that it can retake P's from syscalls, preempt long running G's, and
      ;; poll the network if all P's are busy for long stretches.
      ;;
      ;; It should wakeup from deep sleep if any P's become active either due
      ;; to exiting a syscall or waking up due to a timer expiring so that it
      ;; can resume performing those duties. If it wakes from a syscall it
      ;; resets idle and delay as a bet that since it had retaken a P from a
      ;; syscall before, it may need to do it again shortly after the
      ;; application starts work again. It does not reset idle when waking
      ;; from a timer to avoid adding system load to applications that spend
      ;; most of their time sleeping.
      (let [now (nanotime)]
        (when (and (<= (.-schedtrace debug) 0) ^:go/paren (or (.Load (.-gcwaiting sched)) (== (.Load (.-npidle sched)) gomaxprocs)))
          (lock (addr (.-lock sched)))
          (when (or (.Load (.-gcwaiting sched)) (== (.Load (.-npidle sched)) gomaxprocs))
            (let [syscallWake false
                ^{:line 6636} next (timeSleepUntil)]
              (when (> next now)
                (.Store (.-sysmonwait sched) true)
                (unlock (addr (.-lock sched)))
                ;; Make wake-up period small enough
                ;; for the sampling to be correct.
                (let [sleep (/ forcegcperiod 2)]
                  (when (< (- next now) sleep)
                    (set! sleep (- next now)))

                  (let [shouldRelax (>= sleep osRelaxMinNS)]
                    (when shouldRelax
                      (osRelax true))

                    (set! syscallWake (notetsleep (addr (.-sysmonnote sched)) sleep))
                    (when shouldRelax
                      (osRelax false))

                    (lock (addr (.-lock sched)))
                    (.Store (.-sysmonwait sched) false)
                    (noteclear (addr (.-sysmonnote sched))))))

              (when syscallWake
                (set! idle 0)
                (set! delay 20))))


          (unlock (addr (.-lock sched))))


        (lock (addr (.-sysmonlock sched)))
        ;; Update now in case we blocked on sysmonnote or spent a long time
        ;; blocked on schedlock or sysmonlock above.
        (set! now (nanotime))

        ;; trigger libc interceptors if needed
        (when (!= @cgo_yield nil)
          (asmcgocall @cgo_yield nil))

        ;; poll network if not polled for more than 10ms
        (let [lastpoll (.Load (.-lastpoll sched))]
          (when (and (netpollinited) (!= lastpoll 0) (< (+ lastpoll (* 10 1000 1000)) now))
            (.CompareAndSwap (.-lastpoll sched) lastpoll now)
            (let [(values list delta) (netpoll 0)] ; non-blocking - returns list of goroutines
              (when (not (.empty list))
                ;; Need to decrement number of idle locked M's
                ;; (pretending that one more is running) before injectglist.
                ;; Otherwise it can lead to the following situation:
                ;; injectglist grabs all P's but before it starts M's to run the P's,
                ;; another M returns from syscall, finishes running its G,
                ;; observes that there is no work to do and no other running M's
                ;; and reports deadlock.
                (incidlelocked -1)
                (injectglist (addr list))
                (incidlelocked 1)
                (netpollAdjustWaiters delta))))


          ;; Check if we need to update GOMAXPROCS at most once per second.
          (when (and (!= (.-updatemaxprocs debug) 0) (<= (+ lastgomaxprocs 1.0E9) now))
            (sysmonUpdateGOMAXPROCS)
            (set! lastgomaxprocs now))

          (when (!= (.Load (.-sysmonWake scavenger)) 0)
            ;; Kick the scavenger awake if someone requested it.
            (.wake scavenger))

          ;; retake P's blocked in syscalls
          ;; and preempt long running G's
          (if (!= (retake now) 0)
            (set! idle 0)

            (inc! idle))

          ;; check if we need to force a GC
          (when [t (lit gcTrigger :kind gcTriggerTime :now now)] (and (.test t) (.Load (.-idle forcegc)))
            (lock (addr (.-lock forcegc)))
            (.Store (.-idle forcegc) false)
            (let [^gList list (zero gList)]
              (.push list (.-g forcegc))
              (injectglist (addr list))
              (unlock (addr (.-lock forcegc)))))

          (when (and (> (.-schedtrace debug) 0) (<= (+ lasttrace (* (conv int64 (.-schedtrace debug)) 1000000)) now))
            (set! lasttrace now)
            (schedtrace (> (.-scheddetail debug) 0)))

          (unlock (addr (.-sysmonlock sched))))))))



(go/type sysmontick (struct
    ^{:line 6728 :tag uint32} schedtick
    ^{:line 6729 :tag uint32} syscalltick
    ^{:line 6730 :tag int64} schedwhen
    ^{:line 6731 :tag int64} syscallwhen))




(go/const ^{:val 10000000 :doc "forcePreemptNS is the time slice given to a G before it is\npreempted.\n"} forcePreemptNS (* 10 1000 1000)) ; 10ms

^{:go/end 6833} (go/func retake ^uint32 [^int64 now]
  (let [n 0]
    ;; Prevent allp slice changes. This lock will be completely
    ;; uncontended unless we're already stopping the world.
    (lock (addr allpLock))
    ;; We can't use a range loop over allp because we may
    ;; temporarily drop the allpLock. Hence, we need to re-fetch
    ;; allp each time around the loop.
    (for [i 0] (< i (len allp)) (inc! i)
      ;; Quickly filter out non-running Ps. Running Ps are either
      ;; in a syscall or are actually executing. Idle Ps don't
      ;; need to be retaken.
      ;;
      ;; This is best-effort, so it's OK that it's racy. Our target
      ;; is to retake Ps that have been running or in a syscall for
      ;; a long time (milliseconds), so the state has plenty of time
      ;; to stabilize.
      (let [pp (aget allp i)]
        (when (or (== pp nil) (!= (atomic/Load (addr (.-status pp))) _Prunning))
          ;; pp can be nil if procresize has grown
          ;; allp but not yet created new Ps.
          (continue))

        (let [pd (addr (.-sysmontick pp))
            ^{:line 6762} sysretake false

            ;; Preempt G if it's running on the same schedtick for
            ;; too long. This could be from a single long-running
            ;; goroutine or a sequence of goroutines run via
            ;; runnext, which share a single schedtick time slice.
            ^{:line 6768} schedt (conv int64 (.-schedtick pp))]
          (cond (!= (conv int64 (.-schedtick pd)) schedt) (do
              (set! (.-schedtick pd) (conv uint32 schedt))
              (set! (.-schedwhen pd) now))
            (<= (+ (.-schedwhen pd) forcePreemptNS) now) (do
              (preemptone pp)
              ;; If pp is in a syscall, preemptone doesn't work.
              ;; The goroutine nor the thread can respond to a
              ;; preemption request because they're not in Go code,
              ;; so we need to take the P ourselves.
              (set! sysretake true)))


          ;; Drop allpLock so we can take sched.lock.
          (unlock (addr allpLock))

          ;; Need to decrement number of idle locked M's (pretending that
          ;; one more is running) before we take the P and resume.
          ;; Otherwise the M from which we retake can exit the syscall,
          ;; increment nmidle and report deadlock.
          ;;
          ;; Can't call incidlelocked once we setBlockOnExitSyscall, due
          ;; to a lock ordering violation between sched.lock and _Gscan.
          (incidlelocked -1)

          ;; Try to prevent the P from continuing in the syscall, if it's in one at all.
          (let [(values thread ok) (setBlockOnExitSyscall pp)]
            (when (not ok)
              ;; Not in a syscall, or something changed out from under us.
              (goto :done))


            ;; Retake the P if it's there for more than 1 sysmon tick (at least 20us).
            (when [syst (conv int64 (.-syscalltick pp))] (and (not sysretake) (!= (conv int64 (.-syscalltick pd)) syst))
              (set! (.-syscalltick pd) (conv uint32 syst))
              (set! (.-syscallwhen pd) now)
              (.resume thread)
              (goto :done))


            ;; On the one hand we don't want to retake Ps if there is no other work to do,
            ;; but on the other hand we want to retake them eventually
            ;; because they can prevent the sysmon thread from deep sleep.
            (when (and (runqempty pp) (> (+ (.Load (.-nmspinning sched)) (.Load (.-npidle sched))) 0) (> (+ (.-syscallwhen pd) (* 10 1000 1000)) now))
              (.resume thread)
              (goto :done))


            ;; Take the P. Note: because we have the scan bit, the goroutine
            ;; is at worst stuck spinning in exitsyscall.
            (.takeP thread)
            (.resume thread)
            (inc! n)

            ;; Handoff the P for some other thread to run it.
            (handoffp pp)

            ;; The P has been handed off to another thread, so risk of a false
            ;; deadlock report while we hold onto it is gone.
            (label :done
              (incidlelocked 1))
            (lock (addr allpLock))))))

    (unlock (addr allpLock))
    (conv uint32 n)))




(go/type syscallingThread "syscallingThread represents a thread in a system call that temporarily\ncannot advance out of the system call.\n" (struct
    ^{:line 6838 :tag (* g)} gp
    ^{:line 6839 :tag (* m)} mp
    ^{:line 6840 :tag (* p)} pp
    ^{:line 6841 :tag uint32} status))
















^{:go/end 6902} (go/func setBlockOnExitSyscall "setBlockOnExitSyscall prevents pp's thread from advancing out of\nexitsyscall. On success, returns the g/m/p state of the thread\nand true. At that point, the caller owns the g/m/p links referenced,\nthe goroutine is in _Gsyscall, and prevented from transitioning out\nof it. On failure, it returns false, and none of these guarantees are\nmade.\n\nCallers must call resume on the resulting thread state once\nthey're done with thread, otherwise it will remain blocked forever.\n\nThis function races with state changes on pp, and thus may fail\nif pp is not in a system call, or exits a system call concurrently\nwith this function. However, this function is safe to call without\nany additional synchronization.\n" [^{:tag (* p)} pp] :results [syscallingThread bool]
  (when (!= (.-status pp) _Prunning)
    (return (lit syscallingThread) false))

  ;; Be very careful here, these reads are intentionally racy.
  ;; Once we notice the G is in _Gsyscall, acquire its scan bit,
  ;; and validate that it's still connected to the *same* M and P,
  ;; we can actually get to work. Holding the scan bit will prevent
  ;; the G from exiting the syscall.
  ;;
  ;; Our goal here is to interrupt long syscalls. If it turns out
  ;; that we're wrong and the G switched to another syscall while
  ;; we were trying to do this, that's completely fine. It's
  ;; probably making more frequent syscalls and the typical
  ;; preemption paths should be effective.
  (let [mp (.ptr (.-m pp))]
    (when (== mp nil)
      ;; Nothing to do.
      (return (lit syscallingThread) false))

    (let [gp (.-curg mp)]
      (when (== gp nil)
        ;; Nothing to do.
        (return (lit syscallingThread) false))

      (let [status (bit-and-not (readgstatus gp) _Gscan)]

        ;; A goroutine is considered in a syscall, and may have a corresponding
        ;; P, if it's in _Gsyscall *or* _Gdeadextra. In the latter case, it's an
        ;; extra M goroutine.
        (when (and (!= status _Gsyscall) (!= status _Gdeadextra))
          ;; Not in a syscall, nothing to do.
          (return (lit syscallingThread) false))

        (when (not (castogscanstatus gp status (bit-or status _Gscan)))
          ;; Not in _Gsyscall or _Gdeadextra anymore. Nothing to do.
          (return (lit syscallingThread) false))

        (when (or (!= (.-m gp) mp) (!= (.ptr (.-p (.-m gp))) pp))
          ;; This is not what we originally observed. Nothing to do.
          (casfrom_Gscanstatus gp (bit-or status _Gscan) status)
          (return (lit syscallingThread) false))

        (return (lit syscallingThread gp mp pp status) true)))))






^{:go/end 6914} (go/method gcstopP "gcstopP unwires the P attached to the syscalling thread\nand moves it into the _Pgcstop state.\n\nThe caller must be stopping the world.\n" [^syscallingThread s]
  (assertLockHeld (addr (.-lock sched)))

  (.releaseP s _Pgcstop)
  (set! (.-gcStopTime (.-pp s)) (nanotime))
  (dec! (.-stopwait sched)))




^{:go/end 6920} (go/method takeP "takeP unwires the P attached to the syscalling thread\nand moves it into the _Pidle state.\n" [^syscallingThread s]
  (.releaseP s _Pidle))





^{:go/end 6939} (go/method releaseP "releaseP unwires the P from the syscalling thread, moving\nit to the provided state. Callers should prefer to use\ntakeP and gcstopP.\n" [^syscallingThread s ^uint32 state]
  (when (and (!= state _Pidle) (!= state _Pgcstop))
    (throw "attempted to release P into a bad state"))

  (let [trace (traceAcquire)]
    (set! (.-m (.-pp s)) 0)
    (set! (.-p (.-mp s)) 0)
    (atomic/Store (addr (.-status (.-pp s))) state)
    (when (.ok trace)
      (.ProcSteal trace (.-pp s))
      (traceRelease trace))

    (addGSyscallNoP (.-mp s))
    (inc! (.-syscalltick (.-pp s)))))



^{:go/end 6944} (go/method resume "resume allows a syscalling thread to advance beyond exitsyscall.\n" [^syscallingThread s]
  (casfrom_Gscanstatus (.-gp s) (bit-or (.-status s) _Gscan) (.-status s)))







^{:go/end 6962} (go/func preemptall "Tell all goroutines that they have been preempted and they should stop.\nThis function is purely best-effort. It can fail to inform a goroutine if a\nprocessor just started running it.\nNo locks need to be held.\nReturns true if preemption request was issued to at least one goroutine.\n" ^bool []
  (let [res false]
    (range [_ pp allp]
      (when (!= (.-status pp) _Prunning)
        (continue))

      (when (preemptone pp)
        (set! res true)))


    ^{:line 6961} res))












^{:go/end 7003} (go/func preemptone "Tell the goroutine running on processor P to stop.\nThis function is purely best-effort. It can incorrectly fail to inform the\ngoroutine. It can inform the wrong goroutine. Even if it informs the\ncorrect goroutine, that goroutine might ignore the request if it is\nsimultaneously executing newstack.\nNo lock needs to be held.\nReturns true if preemption request was issued.\nThe actual preemption will happen at some point in the future\nand will be indicated by the gp->status no longer being\nGrunning\n" ^bool [^{:tag (* p)} pp]
  (let [mp (.ptr (.-m pp))]
    (when (or (== mp nil) (== mp (.-m (getg))))
      (return false))

    ^{:go/breaks [7]} (let [gp (.-curg mp)]
      (when (or (== gp nil) (== gp (.-g0 mp)))
        (return false))

      (when (== (bit-and-not (readgstatus gp) _Gscan) _Gsyscall)
        ;; Don't bother trying to preempt a goroutine in a syscall.
        (return false))


      (set! (.-preempt gp) true)

      ;; Every call in a goroutine checks for stack overflow by
      ;; comparing the current stack pointer to gp->stackguard0.
      ;; Setting gp->stackguard0 to StackPreempt folds
      ;; preemption into the normal stack overflow check.
      (set! (.-stackguard0 gp) stackPreempt)

      ;; Request an async preemption of this P.
      (when (and preemptMSupported (== (.-asyncpreemptoff debug) 0))
        (set! (.-preempt pp) true)
        (preemptM mp))


      true)))


(go/var ^int64 starttime)

^{:go/end 7101} (go/func schedtrace [^bool detailed]
  (let [now (nanotime)]
    (when (== starttime 0)
      (set! starttime now))


    (lock (addr (.-lock sched)))
    (print "SCHED " (/ (- now starttime) 1000000.0) "ms: gomaxprocs=" gomaxprocs " idleprocs=" (.Load (.-npidle sched)) " threads=" (mcount) " spinningthreads=" (.Load (.-nmspinning sched)) " needspinning=" (.Load (.-needspinning sched)) " idlethreads=" (.-nmidle sched) " runqueue=" (.-size (.-runq sched)))
    (when detailed
      (print " gcwaiting=" (.Load (.-gcwaiting sched)) " nmidlelocked=" (.-nmidlelocked sched) " stopwait=" (.-stopwait sched) " sysmonwait=" (.Load (.-sysmonwait sched)) "\n"))

    ;; We must be careful while reading data from P's, M's and G's.
    ;; Even if we hold schedlock, most data can be changed concurrently.
    ;; E.g. (p->m ? p->m->id : -1) can crash if p->m changes from non-nil to nil.
    (range [i pp allp]
      (let [h (atomic/Load (addr (.-runqhead pp)))
          ^{:line 7023} t (atomic/Load (addr (.-runqtail pp)))]
        (if detailed (do
            (print "  P" i ": status=" (.-status pp) " schedtick=" (.-schedtick pp) " syscalltick=" (.-syscalltick pp) " m=")
            (let [mp (.ptr (.-m pp))]
              (if (!= mp nil)
                (print (.-id mp))

                (print "nil"))

              (print " runqsize=" (- t h) " gfreecnt=" (.-size (.-gFree pp)) " timerslen=" (len (.-heap (.-timers pp))) "\n")))
          (do
            ;; In non-detailed mode format lengths of per-P run queues as:
            ;; [ len1 len2 len3 len4 ]
            (print " ")
            (when (== i 0)
              (print "[ "))

            (print (- t h))
            (when (== i (- (len allp) 1))
              (print " ]"))))))




    (when (not detailed)
      ;; Format per-P schedticks as: schedticks=[ tick1 tick2 tick3 tick4 ].
      (print " schedticks=[ ")
      (range [_ pp allp]
        (print (.-schedtick pp))
        (print " "))

      (print "]\n"))


    (when (not detailed)
      (unlock (addr (.-lock sched)))
      (return))


    (for [mp allm] (!= mp nil) (set! mp (.-alllink mp))
      (let [pp (.ptr (.-p mp))]
        (print "  M" (.-id mp) ": p=")
        (if (!= pp nil)
          (print (.-id pp))

          (print "nil"))

        (print " curg=")
        (if (!= (.-curg mp) nil)
          (print (.-goid (.-curg mp)))

          (print "nil"))

        (print " mallocing=" (.-mallocing mp) " throwing=" (.-throwing mp) " preemptoff=" (.-preemptoff mp) " locks=" (.-locks mp) " dying=" (.-dying mp) " spinning=" (.-spinning mp) " blocked=" (.-blocked mp) " lockedg=")
        (if [lockedg (.ptr (.-lockedg mp))] (!= lockedg nil)
          (print (.-goid lockedg))

          (print "nil"))

        (print "\n")))


    (forEachG ^{:go/end 7099} (fn [^{:tag (* g)} gp]
        (print "  G" (.-goid gp) ": status=" (readgstatus gp) "(" (.String (.-waitreason gp)) ") m=")
        (if (!= (.-m gp) nil)
          (print (.-id (.-m gp)))

          (print "nil"))

        (print " lockedm=")
        (if [lockedm (.ptr (.-lockedm gp))] (!= lockedm nil)
          (print (.-id lockedm))

          (print "nil"))

        (print "\n")))

    (unlock (addr (.-lock sched)))))


(go/type updateMaxProcsGState (struct
    ^{:line 7104 :tag mutex} lock
    ^{:line 7105 :tag (* g)} g
    ^{:line 7106 :tag atomic/Bool} idle


    ^{:line 7109 :tag int32 :doc "Readable when idle == false, writable when idle == true.\n"} procs)) ; new GOMAXPROCS value


(go/var


  ^{:line 7115} [^{:doc "GOMAXPROCS update godebug metric. Incremented if automatic\nGOMAXPROCS updates actually change the value of GOMAXPROCS.\n"} updatemaxprocs (addr (lit godebugInc :name "updatemaxprocs"))]



  ^{:line 7119} [^{:tag updateMaxProcsGState :doc "Synchronization and state between updateMaxProcsGoroutine and\nsysmon.\n"} updateMaxProcsG]
















































  ^{:line 7168} [^{:tag mutex :doc "Synchronization between GOMAXPROCS and sysmon.\n\nSetting GOMAXPROCS via a call to GOMAXPROCS disables automatic\nGOMAXPROCS updates.\n\nWe want to make two guarantees to callers of GOMAXPROCS. After\nGOMAXPROCS returns:\n\n1. The runtime will not make any automatic changes to GOMAXPROCS.\n\n2. The runtime will not perform any of the system calls used to\n   determine the appropriate value of GOMAXPROCS (i.e., it won't\n   call defaultGOMAXPROCS).\n\n(1) is the baseline guarantee that everyone needs. The GOMAXPROCS\nAPI isn't useful to anyone if automatic updates may occur after it\nreturns. This is easily achieved by double-checking the state under\nSTW before committing an automatic GOMAXPROCS update.\n\n(2) doesn't matter to most users, as it is isn't observable as long\nas (1) holds. However, it can be important to users sandboxing Go.\nThey want disable these system calls and need some way to know when\nthey are guaranteed the calls will stop.\n\nThis would be simple to achieve if we simply called\ndefaultGOMAXPROCS under STW in updateMaxProcsGoroutine below.\nHowever, we would like to avoid scheduling this goroutine every\nsecond when it will almost never do anything. Instead, sysmon calls\ndefaultGOMAXPROCS to decide whether to schedule\nupdateMaxProcsGoroutine. Thus we need to synchronize between sysmon\nand GOMAXPROCS calls.\n\nGOMAXPROCS can't hold a runtime mutex across STW. It could hold a\nsemaphore, but sysmon cannot take semaphores. Instead, we have a\nmore complex scheme:\n\n* sysmon holds computeMaxProcsLock while calling defaultGOMAXPROCS.\n* sysmon skips the current update if sched.customGOMAXPROCS is\n  set.\n* GOMAXPROCS sets sched.customGOMAXPROCS once it is committed to\n  changing GOMAXPROCS.\n* GOMAXPROCS takes computeMaxProcsLock to wait for outstanding\n  defaultGOMAXPROCS calls to complete.\n\nN.B. computeMaxProcsLock could simply be sched.lock, but we want to\navoid holding that lock during the potentially slow\ndefaultGOMAXPROCS.\n"} computeMaxProcsLock])





^{:go/end 7192} (go/func defaultGOMAXPROCSUpdateEnable "Start GOMAXPROCS update helper goroutine.\n\nThis is based on forcegchelper.\n" []
  (when (or (== (.-updatemaxprocs debug) 0) (not haveSysmon))
    ;; Unconditionally increment the metric when updates are disabled.
    ;;
    ;; It would be more descriptive if we did a dry run of the
    ;; complete update, determining the appropriate value of
    ;; GOMAXPROCS and the bailing out and just incrementing the
    ;; metric if a change would occur.
    ;;
    ;; Not only is that a lot of ongoing work for a disabled
    ;; feature, but some users need to be able to completely
    ;; disable the update system calls (such as sandboxes).
    ;; Currently, updatemaxprocs=0 serves that purpose.
    (.IncNonDefault updatemaxprocs)
    (return))


  (go (updateMaxProcsGoroutine)))


^{:go/end 7228} (go/func updateMaxProcsGoroutine []
  (set! (.-g updateMaxProcsG) (getg))
  (lockInit (addr (.-lock updateMaxProcsG)) lockRankUpdateMaxProcsG)
  (while true
    (lock (addr (.-lock updateMaxProcsG)))
    (when (.Load (.-idle updateMaxProcsG))
      (throw "updateMaxProcsGoroutine: phase error"))

    (.Store (.-idle updateMaxProcsG) true)
    (goparkunlock (addr (.-lock updateMaxProcsG)) waitReasonUpdateGOMAXPROCSIdle traceBlockSystemGoroutine 1)
    ;; This goroutine is explicitly resumed by sysmon.

    (let [stw (stopTheWorldGC stwGOMAXPROCS)]

      ;; Still OK to update?
      (lock (addr (.-lock sched)))
      (let [custom (.-customGOMAXPROCS sched)]
        (unlock (addr (.-lock sched)))
        (when custom
          (startTheWorldGC stw)
          (return))


        ;; newprocs will be processed by startTheWorld
        ;;
        ;; TODO(prattmic): this could use a nicer API. Perhaps add it to the
        ;; stw parameter?
        (set! newprocs (.-procs updateMaxProcsG))
        (lock (addr (.-lock sched)))
        (set! (.-customGOMAXPROCS sched) false)
        (unlock (addr (.-lock sched)))

        (startTheWorldGC stw)))))



^{:go/end 7264} (go/func sysmonUpdateGOMAXPROCS []
  ;; Synchronize with GOMAXPROCS. See comment on computeMaxProcsLock.
  (lock (addr computeMaxProcsLock))

  ;; No update if GOMAXPROCS was set manually.
  (lock (addr (.-lock sched)))
  (let [custom (.-customGOMAXPROCS sched)
      ^{:line 7237} curr gomaxprocs]
    (unlock (addr (.-lock sched)))
    (when custom
      (unlock (addr computeMaxProcsLock))
      (return))


    ;; Don't hold sched.lock while we read the filesystem.
    (let [procs (defaultGOMAXPROCS 0)]
      (unlock (addr computeMaxProcsLock))
      (when (== procs curr)
        ;; Nothing to do.
        (return))


      ;; Sysmon can't directly stop the world. Run the helper to do so on our
      ;; behalf. If updateGOMAXPROCS.idle is false, then a previous update is
      ;; still pending.
      (when (.Load (.-idle updateMaxProcsG))
        (lock (addr (.-lock updateMaxProcsG)))
        (set! (.-procs updateMaxProcsG) procs)
        (.Store (.-idle updateMaxProcsG) false)
        (let [^gList list (zero gList)]
          (.push list (.-g updateMaxProcsG))
          (injectglist (addr list))
          (unlock (addr (.-lock updateMaxProcsG))))))))








^{:go/end 7288} (go/func schedEnableUser "schedEnableUser enables or disables the scheduling of user\ngoroutines.\n\nThis does not stop already running user goroutines, so the caller\nshould first stop the world when disabling user goroutines.\n" [^bool enable]
  (lock (addr (.-lock sched)))
  (when (== (.-user (.-disable sched)) (not enable))
    (unlock (addr (.-lock sched)))
    (return))

  (set! (.-user (.-disable sched)) (not enable))
  (if enable
    (let [n (.-size (.-runnable (.-disable sched)))]
      (globrunqputbatch (addr (.-runnable (.-disable sched))))
      (unlock (addr (.-lock sched)))
      (for [] (and (!= n 0) (!= (.Load (.-npidle sched)) 0)) (dec! n)
        (startm nil false false)))


    (unlock (addr (.-lock sched)))))







^{:go/breaks [6] :go/end 7301} (go/func schedEnabled "schedEnabled reports whether gp should be scheduled. It returns\nfalse is scheduling of gp is disabled.\n\nsched.lock must be held.\n" ^bool [^{:tag (* g)} gp]
  (assertLockHeld (addr (.-lock sched)))

  (when (.-user (.-disable sched))
    (return (isSystemGoroutine gp true)))

  true)







^{:go/end 7314} (go/func ^:go/nowritebarrierrec mput "Put mp on midle list.\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" [^{:tag (* m)} mp]
  (assertLockHeld (addr (.-lock sched)))

  (.push (.-midle sched) (conv unsafe/Pointer mp))
  (inc! (.-nmidle sched))
  (checkdead))







^{:go/end 7329} (go/func ^:go/nowritebarrierrec mget "Try to get an m from midle list.\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" ^{:tag (* m)} []
  (assertLockHeld (addr (.-lock sched)))

  (let [mp (conv (* m) (.pop (.-midle sched)))]
    (when (!= mp nil)
      (dec! (.-nmidle sched)))

    ^{:line 7328} mp))









^{:go/end 7350} (go/func ^:go/nowritebarrierrec mgetSpecific "Try to get a specific m from midle list. Returns nil if it isn't on the\nmidle list.\n\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" ^{:tag (* m)} [^{:tag (* m)} mp]
  (assertLockHeld (addr (.-lock sched)))

  (when (and (== (.-prev (.-idleNode mp)) 0) (== (.-next (.-idleNode mp)) 0))
    ;; Not on the list.
    (return nil))


  (.remove (.-midle sched) (conv unsafe/Pointer mp))
  (dec! (.-nmidle sched))

  ^{:line 7349} mp)







^{:go/end 7361} (go/func ^:go/nowritebarrierrec globrunqput "Put gp on the global runnable queue.\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" [^{:tag (* g)} gp]
  (assertLockHeld (addr (.-lock sched)))

  (.pushBack (.-runq sched) gp))







^{:go/end 7372} (go/func ^:go/nowritebarrierrec globrunqputhead "Put gp at the head of the global runnable queue.\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" [^{:tag (* g)} gp]
  (assertLockHeld (addr (.-lock sched)))

  (.push (.-runq sched) gp))








^{:go/end 7385} (go/func ^:go/nowritebarrierrec globrunqputbatch "Put a batch of runnable goroutines on the global runnable queue.\nThis clears *batch.\nsched.lock must be held.\nMay run during STW, so write barriers are not allowed.\n" [^{:tag (* gQueue)} batch]
  (assertLockHeld (addr (.-lock sched)))

  (.pushBackAll (.-runq sched) @batch)
  (set! @batch (lit gQueue)))




^{:go/end 7397} (go/func globrunqget "Try get a single G from the global runnable queue.\nsched.lock must be held.\n" ^{:tag (* g)} []
  (assertLockHeld (addr (.-lock sched)))

  (when (== (.-size (.-runq sched)) 0)
    (return nil))


  (.pop (.-runq sched)))




^{:go/end 7418} (go/func globrunqgetbatch "Try get a batch of G's from the global runnable queue.\nsched.lock must be held.\n" [^int32 n] :results [^{:tag (* g)} gp ^gQueue q]
  (assertLockHeld (addr (.-lock sched)))

  (when (== (.-size (.-runq sched)) 0)
    (return))


  (set! n (min n (.-size (.-runq sched)) (+ (/ (.-size (.-runq sched)) gomaxprocs) 1)))

  (set! gp (.pop (.-runq sched)))
  (dec! n)

  (for [] (> n 0) (dec! n)
    (let [gp1 (.pop (.-runq sched))]
      (.pushBack q gp1)))

  (return))



(go/type pMask "pMask is an atomic bitstring with one bit per P.\n" (slice uint32))


^{:go/end 7428} (go/method read "read returns true if P id's bit is set.\n" ^bool [^pMask p ^uint32 id]
  (let [word (/ id 32)
      ^{:line 7426} mask (<< (conv uint32 1) (% id 32))]
    (!= (bit-and (atomic/Load (addr (aget p word))) mask) 0)))



^{:go/end 7435} (go/method set "set sets P id's bit.\n" [^pMask p ^int32 id]
  (let [word (/ id 32)
      ^{:line 7433} mask (<< (conv uint32 1) (% id 32))]
    (atomic/Or (addr (aget p word)) mask)))



^{:go/end 7442} (go/method clear "clear clears P id's bit.\n" [^pMask p ^int32 id]
  (let [word (/ id 32)
      ^{:line 7440} mask (<< (conv uint32 1) (% id 32))]
    (atomic/And (addr (aget p word)) (bit-not mask))))



^{:go/breaks [5] :go/end 7452} (go/method any "any returns true if any bit in p is set.\n" ^bool [^pMask p]
  (range [i p]
    (when (!= (atomic/Load (addr (aget p i))) 0)
      (return true)))


  false)






^{:go/end 7468} (go/method resize "resize resizes the pMask and returns a new one.\n\nThe result may alias p, so callers are encouraged to\ndiscard p. Not safe for concurrent use.\n" ^pMask [^pMask p ^int32 nprocs]
  (let [maskWords (/ (+ nprocs 31) 32)]

    (when (<= maskWords (conv int32 (cap p)))
      (return (subslice p _ maskWords)))

    (let [newMask (make (slice uint32) maskWords)]
      ;; No need to copy beyond len, old Ps are irrelevant.
      (copy newMask p)
      ^{:line 7467} newMask)))













^{:go/end 7501} (go/func ^:go/nowritebarrierrec pidleput "pidleput puts p on the _Pidle list. now must be a relatively recent call\nto nanotime or zero. Returns now or the current time if now was zero.\n\nThis releases ownership of p. Once sched.lock is released it is no longer\nsafe to use p.\n\nsched.lock must be held.\n\nMay run during STW, so write barriers are not allowed.\n" ^int64 [^{:tag (* p)} pp ^int64 now]
  (assertLockHeld (addr (.-lock sched)))

  (when (not (runqempty pp))
    (throw "pidleput: P has non-empty run queue"))

  (when (== now 0)
    (set! now (nanotime)))

  (when (== (.Load (.-len (.-timers pp))) 0)
    (.clear timerpMask (.-id pp)))

  (.set idlepMask (.-id pp))
  (set! (.-link pp) (.-pidle sched))
  (.set (.-pidle sched) pp)
  (.Add (.-npidle sched) 1)
  (when (not (.start (.-limiterEvent pp) limiterEventIdle now))
    (throw "must be able to track idle limiter event"))

  ^{:line 7500} now)









^{:go/end 7526} (go/func ^:go/nowritebarrierrec pidleget "pidleget tries to get a p from the _Pidle list, acquiring ownership.\n\nsched.lock must be held.\n\nMay run during STW, so write barriers are not allowed.\n" [^int64 now] :results [(* p) int64]
  (assertLockHeld (addr (.-lock sched)))

  (let [pp (.ptr (.-pidle sched))]
    (when (!= pp nil)
      ;; Timer may get added at any time now.
      (when (== now 0)
        (set! now (nanotime)))

      (.set timerpMask (.-id pp))
      (.clear idlepMask (.-id pp))
      (set! (.-pidle sched) (.-link pp))
      (.Add (.-npidle sched) -1)
      (.stop (.-limiterEvent pp) limiterEventIdle now))

    (return pp now)))












^{:go/end 7551} (go/func ^:go/nowritebarrierrec pidlegetSpinning "pidlegetSpinning tries to get a p from the _Pidle list, acquiring ownership.\nThis is called by spinning Ms (or callers than need a spinning M) that have\nfound work. If no P is available, this must synchronized with non-spinning\nMs that may be preparing to drop their P without discovering this work.\n\nsched.lock must be held.\n\nMay run during STW, so write barriers are not allowed.\n" [^int64 now] :results [(* p) int64]
  (assertLockHeld (addr (.-lock sched)))

  (let [(values pp ^:assign now) (pidleget now)]
    (when (== pp nil)
      ;; See "Delicate dance" comment in findRunnable. We found work
      ;; that we cannot take, we must synchronize with non-spinning
      ;; Ms that may be preparing to drop their P.
      (.Store (.-needspinning sched) 1)
      (return nil now))


    (return pp now)))




^{:go/end 7568} (go/func runqempty "runqempty reports whether pp has no Gs on its local run queue.\nIt never returns true spuriously.\n" ^bool [^{:tag (* p)} pp]
  ;; Defend against a race where 1) pp has G1 in runqnext but runqhead == runqtail,
  ;; 2) runqput on pp kicks G1 to the runq, 3) runqget on pp empties runqnext.
  ;; Simply observing that runqhead == runqtail and then observing that runqnext == nil
  ;; does not mean the queue is empty.
  (while true
    (let [head (atomic/Load (addr (.-runqhead pp)))
        ^{:line 7562} tail (atomic/Load (addr (.-runqtail pp)))
        ^{:line 7563} runnext (atomic/Loaduintptr (conv (* uintptr) (conv unsafe/Pointer (addr (.-runnext pp)))))]
      (when (== tail (atomic/Load (addr (.-runqtail pp))))
        (return (and (== head tail) (== runnext 0)))))))













(go/const ^{:val false :doc "To shake out latent assumptions about scheduling order,\nwe introduce some randomness into scheduling decisions\nwhen running with the race detector.\nThe need for this was made obvious by changing the\n(deterministic) scheduling order in Go 1.5 and breaking\nmany poorly-written tests.\nWith the randomness here, as long as the tests pass\nconsistently with -race, they shouldn't have latent scheduling\nassumptions.\n"} randomizeScheduler raceenabled)






^{:go/end 7628} (go/func runqput "runqput tries to put g on the local runnable queue.\nIf next is false, runqput adds g to the tail of the runnable queue.\nIf next is true, runqput puts g in the pp.runnext slot.\nIf the run queue is full, runnext puts g on the global queue.\nExecuted only by the owner P.\n" [^{:tag (* p)} pp ^{:tag (* g)} gp ^bool next]
  (when (and (not haveSysmon) next)
    ;; A runnext goroutine shares the same time slice as the
    ;; current goroutine (inheritTime from runqget). To prevent a
    ;; ping-pong pair of goroutines from starving all others, we
    ;; depend on sysmon to preempt "long-running goroutines". That
    ;; is, any set of goroutines sharing the same time slice.
    ;;
    ;; If there is no sysmon, we must avoid runnext entirely or
    ;; risk starvation.
    (set! next false))

  (when (and randomizeScheduler next (== (randn 2) 0))
    (set! next false))


  (when next
    (let [
        ^{:line 7604 :go/label :retryNext} oldnext (.-runnext pp)]
      (when (not (.cas (.-runnext pp) oldnext (conv guintptr (conv unsafe/Pointer gp))))
        (goto :retryNext))

      (when (== oldnext 0)
        (return))

      ;; Kick the old runnext out to the regular run queue.
      (set! gp (.ptr oldnext))))


  (let [
      ^{:line 7616 :go/label :retry} h (atomic/LoadAcq (addr (.-runqhead pp))) ; load-acquire, synchronize with consumers
      ^{:line 7617} t (.-runqtail pp)]
    (when (< (- t h) (conv uint32 (len (.-runq pp))))
      (.set (aget (.-runq pp) (% t (conv uint32 (len (.-runq pp))))) gp)
      (atomic/StoreRel (addr (.-runqtail pp)) (+ t 1)) ; store-release, makes the item available for consumption
      (return))

    (when (runqputslow pp gp h t)
      (return))

    ;; the queue is not full, now the put above must succeed
    (goto :retry)))




^{:go/end 7668} (go/func runqputslow "Put g and a batch of work from local runnable queue on global queue.\nExecuted only by the owner P.\n" ^bool [^{:tag (* p)} pp ^{:tag (* g)} gp ^uint32 h ^uint32 t]
  (let [^{:tag (array (+ (/ (len (.-runq pp)) 2) 1) (* g))} batch (zero (array (+ (/ (len (.-runq pp)) 2) 1) (* g)))

      ;; First, grab a batch from local queue.
      ^{:line 7636} n (- t h)]
    (set! n (/ n 2))
    (when (!= n (conv uint32 (/ (len (.-runq pp)) 2)))
      (throw "runqputslow: queue is not full"))

    (for [i (conv uint32 0)] (< i n) (inc! i)
      (aset batch i (.ptr (aget (.-runq pp) (% (+ h i) (conv uint32 (len (.-runq pp))))))))

    (when (not (atomic/CasRel (addr (.-runqhead pp)) h (+ h n))) ; cas-release, commits consume
      (return false))

    (aset batch n gp)

    (when randomizeScheduler
      (for [i (conv uint32 1)] (<= i n) (inc! i)
        (let [j (cheaprandn (+ i 1))]
          (set! (values (aget batch i) (aget batch j)) (values (aget batch j) (aget batch i))))))



    ;; Link the goroutines.
    (for [i (conv uint32 0)] (< i n) (inc! i)
      (.set (.-schedlink (aget batch i)) (aget batch (+ i 1))))


    ^{:go/breaks [5]} (let [q (lit gQueue (.guintptr (aget batch 0)) (.guintptr (aget batch n)) (conv int32 (+ n 1)))]

      ;; Now put the batch on global queue.
      (lock (addr (.-lock sched)))
      (globrunqputbatch (addr q))
      (unlock (addr (.-lock sched)))
      true)))





^{:go/end 7700} (go/func runqputbatch "runqputbatch tries to put all the G's on q on the local runnable queue.\nIf the local runq is full the input queue still contains unqueued Gs.\nExecuted only by the owner P.\n" [^{:tag (* p)} pp ^{:tag (* gQueue)} q]
  (when (.empty q)
    (return))

  (let [h (atomic/LoadAcq (addr (.-runqhead pp)))
      ^{:line 7678} t (.-runqtail pp)
      ^{:line 7679} n (conv uint32 0)]
    (while (and (not (.empty q)) (< (- t h) (conv uint32 (len (.-runq pp)))))
      (let [gp (.pop q)]
        (.set (aget (.-runq pp) (% t (conv uint32 (len (.-runq pp))))) gp)
        (inc! t)
        (inc! n)))


    (when randomizeScheduler
      (let [off ^{:go/end 7690} (fn ^uint32 [^uint32 o]
            (% (+ (.-runqtail pp) o) (conv uint32 (len (.-runq pp)))))]

        (for [i (conv uint32 1)] (< i n) (inc! i)
          (let [j (cheaprandn (+ i 1))]
            (set! (values (aget (.-runq pp) (off i)) (aget (.-runq pp) (off j))) (values (aget (.-runq pp) (off j)) (aget (.-runq pp) (off i))))))))



    (atomic/StoreRel (addr (.-runqtail pp)) t)

    (return)))






^{:go/end 7727} (go/func runqget "Get g from local runnable queue.\nIf inheritTime is true, gp should inherit the remaining time in the\ncurrent time slice. Otherwise, it should start a new time slice.\nExecuted only by the owner P.\n" [^{:tag (* p)} pp] :results [^{:tag (* g)} gp ^bool inheritTime]
  ;; If there's a runnext, it's the next G to run.
  (let [next (.-runnext pp)]
    ;; If the runnext is non-0 and the CAS fails, it could only have been stolen by another P,
    ;; because other Ps can race to set runnext to 0, but only the current P can set it to non-0.
    ;; Hence, there's no need to retry this CAS if it fails.
    (when (and (!= next 0) (.cas (.-runnext pp) next 0))
      (return (.ptr next) true))


    (while true
      (let [h (atomic/LoadAcq (addr (.-runqhead pp))) ; load-acquire, synchronize with other consumers
          ^{:line 7718} t (.-runqtail pp)]
        (when (== t h)
          (return nil false))

        (let [gp (.ptr (aget (.-runq pp) (% h (conv uint32 (len (.-runq pp))))))]
          (when (atomic/CasRel (addr (.-runqhead pp)) h (+ h 1)) ; cas-release, commits consume
            (return gp false)))))))






^{:go/end 7764} (go/func runqdrain "runqdrain drains the local runnable queue of pp and returns all goroutines in it.\nExecuted only by the owner P.\n" [^{:tag (* p)} pp] :results [^gQueue drainQ]
  (let [oldNext (.-runnext pp)]
    (when (and (!= oldNext 0) (.cas (.-runnext pp) oldNext 0))
      (.pushBack drainQ (.ptr oldNext)))


    (let [
        ^{:line 7738 :go/label :retry} h (atomic/LoadAcq (addr (.-runqhead pp))) ; load-acquire, synchronize with other consumers
        ^{:line 7739} t (.-runqtail pp)
        ^{:line 7740} qn (- t h)]
      (when (== qn 0)
        (return))

      (when (> qn (conv uint32 (len (.-runq pp)))) ; read inconsistent h and t
        (goto :retry))


      (when (not (atomic/CasRel (addr (.-runqhead pp)) h (+ h qn))) ; cas-release, commits consume
        (goto :retry))


      ;; We've inverted the order in which it gets G's from the local P's runnable queue
      ;; and then advances the head pointer because we don't want to mess up the statuses of G's
      ;; while runqdrain() and runqsteal() are running in parallel.
      ;; Thus we should advance the head pointer before draining the local P into a gQueue,
      ;; so that we can update any gp.schedlink only after we take the full ownership of G,
      ;; meanwhile, other P's can't access to all G's in local P's runnable queue and steal them.
      ;; See https://groups.google.com/g/golang-dev/c/0pTKxEKhHSc/m/6Q85QjdVBQAJ for more details.
      (for [i (conv uint32 0)] (< i qn) (inc! i)
        (let [gp (.ptr (aget (.-runq pp) (% (+ h i) (conv uint32 (len (.-runq pp))))))]
          (.pushBack drainQ gp)))

      (return))))






^{:go/end 7833} (go/func runqgrab "Grabs a batch of goroutines from pp's runnable queue into batch.\nBatch is a ring buffer starting at batchHead.\nReturns number of grabbed goroutines.\nCan be executed by any P.\n" ^uint32 [^{:tag (* p)} pp ^{:tag (* (array 256 guintptr))} batch ^uint32 batchHead ^bool stealRunNextG]
  (while true
    (let [h (atomic/LoadAcq (addr (.-runqhead pp))) ; load-acquire, synchronize with other consumers
        ^{:line 7773} t (atomic/LoadAcq (addr (.-runqtail pp))) ; load-acquire, synchronize with the producer
        ^{:line 7774} n (- t h)]
      (set! n (- n (/ n 2)))
      (when (== n 0)
        (when stealRunNextG
          ;; Try to steal from pp.runnext.
          (when [next (.-runnext pp)] (!= next 0)
            (when (== (.-status pp) _Prunning)
              (when [mp (.ptr (.-m pp))] (!= mp nil)
                (when [gp (.-curg mp)] (or (== gp nil) (!= (bit-and-not (readgstatus gp) _Gscan) _Gsyscall))
                  ;; Sleep to ensure that pp isn't about to run the g
                  ;; we are about to steal.
                  ;; The important use case here is when the g running
                  ;; on pp ready()s another g and then almost
                  ;; immediately blocks. Instead of stealing runnext
                  ;; in this window, back off to give pp a chance to
                  ;; schedule runnext. This will avoid thrashing gs
                  ;; between different Ps.
                  ;; A sync chan send/recv takes ~50ns as of time of
                  ;; writing, so 3us gives ~50x overshoot.
                  ;; If curg is nil, we assume that the P is likely
                  ;; to be in the scheduler. If curg isn't nil and isn't
                  ;; in a syscall, then it's either running, waiting, or
                  ;; runnable. In this case we want to sleep because the
                  ;; P might either call into the scheduler soon (running),
                  ;; or already is (since we found a waiting or runnable
                  ;; goroutine hanging off of a running P, suggesting it
                  ;; either recently transitioned out of running, or will
                  ;; transition to running shortly).
                  (if (not osHasLowResTimer)
                    (usleep 3)

                    ;; On some platforms system timer granularity is
                    ;; 1-15ms, which is way too much for this
                    ;; optimization. So just yield.
                    (osyield)))))




            (when (not (.cas (.-runnext pp) next 0))
              (continue))

            (aset batch (% batchHead (conv uint32 (len batch))) next)
            (return 1)))


        (return 0))

      (when (> n (conv uint32 (/ (len (.-runq pp)) 2))) ; read inconsistent h and t
        (continue))

      (for [i (conv uint32 0)] (< i n) (inc! i)
        (let [g (aget (.-runq pp) (% (+ h i) (conv uint32 (len (.-runq pp)))))]
          (aset batch (% (+ batchHead i) (conv uint32 (len batch))) g)))

      (when (atomic/CasRel (addr (.-runqhead pp)) h (+ h n)) ; cas-release, commits consume
        (return n)))))







^{:go/end 7855} (go/func runqsteal "Steal half of elements from local runnable queue of p2\nand put onto local runnable queue of p.\nReturns one of the stolen elements (or nil if failed).\n" ^{:tag (* g)} [^{:tag (* p)} pp ^{:tag (* p)} p2 ^bool stealRunNextG]
  (let [t (.-runqtail pp)
      ^{:line 7840} n (runqgrab p2 (addr (.-runq pp)) t stealRunNextG)]
    (when (== n 0)
      (return nil))

    (dec! n)
    (let [gp (.ptr (aget (.-runq pp) (% (+ t n) (conv uint32 (len (.-runq pp))))))]
      (when (== n 0)
        (return gp))

      (let [h (atomic/LoadAcq (addr (.-runqhead pp)))] ; load-acquire, synchronize with consumers
        (when (>= (+ (- t h) n) (conv uint32 (len (.-runq pp))))
          (throw "runqsteal: runq overflow"))

        (atomic/StoreRel (addr (.-runqtail pp)) (+ t n)) ; store-release, makes the item available for consumption
        ^{:line 7854} gp))))




(go/type gQueue "A gQueue is a dequeue of Gs linked through g.schedlink. A G can only\nbe on one gQueue or gList at a time.\n" (struct
    ^{:line 7860 :tag guintptr} head
    ^{:line 7861 :tag guintptr} tail
    ^{:line 7862 :tag int32} size))



^{:go/end 7868} (go/method empty "empty reports whether q is empty.\n" ^bool [^{:tag (* gQueue)} q]
  (== (.-head q) 0))



^{:go/end 7878} (go/method push "push adds gp to the head of q.\n" [^{:tag (* gQueue)} q ^{:tag (* g)} gp]
  (set! (.-schedlink gp) (.-head q))
  (.set (.-head q) gp)
  (when (== (.-tail q) 0)
    (.set (.-tail q) gp))

  (inc! (.-size q)))



^{:go/end 7890} (go/method pushBack "pushBack adds gp to the tail of q.\n" [^{:tag (* gQueue)} q ^{:tag (* g)} gp]
  (set! (.-schedlink gp) 0)
  (if (!= (.-tail q) 0)
    (.set (.-schedlink (.ptr (.-tail q))) gp)

    (.set (.-head q) gp))

  (.set (.-tail q) gp)
  (inc! (.-size q)))




^{:go/end 7906} (go/method pushBackAll "pushBackAll adds all Gs in q2 to the tail of q. After this q2 must\nnot be used.\n" [^{:tag (* gQueue)} q ^gQueue q2]
  (when (== (.-tail q2) 0)
    (return))

  (set! (.-schedlink (.ptr (.-tail q2))) 0)
  (if (!= (.-tail q) 0)
    (set! (.-schedlink (.ptr (.-tail q))) (.-head q2))

    (set! (.-head q) (.-head q2)))

  (set! (.-tail q) (.-tail q2))
  (set! (.-size q) + (.-size q2)))




^{:go/end 7920} (go/method pop "pop removes and returns the head of queue q. It returns nil if\nq is empty.\n" ^{:tag (* g)} [^{:tag (* gQueue)} q]
  (let [gp (.ptr (.-head q))]
    (when (!= gp nil)
      (set! (.-head q) (.-schedlink gp))
      (when (== (.-head q) 0)
        (set! (.-tail q) 0))

      (dec! (.-size q)))

    ^{:line 7919} gp))



^{:go/end 7927} (go/method popList "popList takes all Gs in q and returns them as a gList.\n" ^gList [^{:tag (* gQueue)} q]
  (let [stack (lit gList (.-head q) (.-size q))]
    (set! @q (lit gQueue))
    ^{:line 7926} stack))




(go/type gList "A gList is a list of Gs linked through g.schedlink. A G can only be\non one gQueue or gList at a time.\n" (struct
    ^{:line 7932 :tag guintptr} head
    ^{:line 7933 :tag int32} size))



^{:go/end 7939} (go/method empty "empty reports whether l is empty.\n" ^bool [^{:tag (* gList)} l]
  (== (.-head l) 0))



^{:go/end 7946} (go/method push "push adds gp to the head of l.\n" [^{:tag (* gList)} l ^{:tag (* g)} gp]
  (set! (.-schedlink gp) (.-head l))
  (.set (.-head l) gp)
  (inc! (.-size l)))



^{:go/end 7955} (go/method pushAll "pushAll prepends all Gs in q to l. After this q must not be used.\n" [^{:tag (* gList)} l ^gQueue q]
  (when (not (.empty q))
    (set! (.-schedlink (.ptr (.-tail q))) (.-head l))
    (set! (.-head l) (.-head q))
    (set! (.-size l) + (.-size q))))




^{:go/end 7965} (go/method pop "pop removes and returns the head of l. If l is empty, it returns nil.\n" ^{:tag (* g)} [^{:tag (* gList)} l]
  (let [gp (.ptr (.-head l))]
    (when (!= gp nil)
      (set! (.-head l) (.-schedlink gp))
      (dec! (.-size l)))

    ^{:line 7964} gp))



^{:go/end 7979} (go/func ^{:go/linkname "setMaxThreads runtime/debug.setMaxThreads"} setMaxThreads [^int in] :results [^int out]
  (lock (addr (.-lock sched)))
  (set! out (conv int (.-maxmcount sched)))
  (if (> in 0x7fffffff) ; MaxInt32
    (set! (.-maxmcount sched) 0x7fffffff)

    (set! (.-maxmcount sched) (conv int32 in)))

  (checkmcount)
  (unlock (addr (.-lock sched)))
  (return))














^{:go/end 7999} (go/func ^:go/nosplit ^{:go/linkname "procPin"} procPin "procPin should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - github.com/bytedance/gopkg\n  - github.com/choleraehyq/pid\n  - github.com/songzhibin97/gkit\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" ^int []
  (let [gp (getg)
      ^{:line 7995} mp (.-m gp)]

    (inc! (.-locks mp))
    (conv int (.-id (.ptr (.-p mp))))))














^{:go/end 8016} (go/func ^:go/nosplit ^{:go/linkname "procUnpin"} procUnpin "procUnpin should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - github.com/bytedance/gopkg\n  - github.com/choleraehyq/pid\n  - github.com/songzhibin97/gkit\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (let [gp (getg)]
    (dec! (.-locks (.-m gp)))))




^{:go/end 8022} (go/func ^:go/nosplit ^{:go/linkname "sync_runtime_procPin sync.runtime_procPin"} sync_runtime_procPin ^int []
  (procPin))




^{:go/end 8028} (go/func ^:go/nosplit ^{:go/linkname "sync_runtime_procUnpin sync.runtime_procUnpin"} sync_runtime_procUnpin []
  (procUnpin))




^{:go/end 8034} (go/func ^:go/nosplit ^{:go/linkname "sync_atomic_runtime_procPin sync/atomic.runtime_procPin"} sync_atomic_runtime_procPin ^int []
  (procPin))




^{:go/end 8040} (go/func ^:go/nosplit ^{:go/linkname "sync_atomic_runtime_procUnpin sync/atomic.runtime_procUnpin"} sync_atomic_runtime_procUnpin []
  (procUnpin))






^{:go/breaks [6] :go/end 8059} (go/func ^:go/nosplit ^{:go/linkname "internal_sync_runtime_canSpin internal/sync.runtime_canSpin"} internal_sync_runtime_canSpin "Active spinning for sync.Mutex.\n" ^bool [^int i]
  ;; sync.Mutex is cooperative, so we are conservative with spinning.
  ;; Spin only few times and only if running on a multicore machine and
  ;; GOMAXPROCS>1 and there is at least one other running P and local runq is empty.
  ;; As opposed to runtime mutex we don't do passive spinning here,
  ;; because there can be work on global runq or on other Ps.
  (when (or (>= i active_spin) (<= numCPUStartup 1) (<= gomaxprocs (+ (.Load (.-npidle sched)) (.Load (.-nmspinning sched)) 1)))
    (return false))

  (when [p (.ptr (.-p (.-m (getg))))] (not (runqempty p))
    (return false))

  true)




^{:go/end 8065} (go/func ^:go/nosplit ^{:go/linkname "internal_sync_runtime_doSpin internal/sync.runtime_doSpin"} internal_sync_runtime_doSpin []
  (procyield active_spin_cnt))
















^{:go/end 8083} (go/func ^:go/nosplit ^{:go/linkname "sync_runtime_canSpin sync.runtime_canSpin"} sync_runtime_canSpin "Active spinning for sync.Mutex.\n\nsync_runtime_canSpin should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - github.com/livekit/protocol\n  - github.com/sagernet/gvisor\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" ^bool [^int i]
  (internal_sync_runtime_canSpin i))














^{:go/end 8099} (go/func ^:go/nosplit ^{:go/linkname "sync_runtime_doSpin sync.runtime_doSpin"} sync_runtime_doSpin "sync_runtime_doSpin should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - github.com/livekit/protocol\n  - github.com/sagernet/gvisor\n  - gvisor.dev/gvisor\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n" []
  (internal_sync_runtime_doSpin))


(go/var ^randomOrder stealOrder)





(go/type randomOrder "randomOrder/randomEnum are helper types for randomized work stealing.\nThey allow to enumerate all Ps in different pseudo-random orders without repetitions.\nThe algorithm is based on the fact that if we have X such that X and GOMAXPROCS\nare coprime, then a sequences of (i + X) % GOMAXPROCS gives the required enumeration.\n" (struct
    ^{:line 8108 :tag uint32} count
    ^{:line 8109 :tag (slice uint32)} coprimes))


(go/type randomEnum (struct
    ^{:line 8113 :tag uint32} i
    ^{:line 8114 :tag uint32} count
    ^{:line 8115 :tag uint32} pos
    ^{:line 8116 :tag uint32} inc))


^{:go/end 8127} (go/method reset [^{:tag (* randomOrder)} ord ^uint32 count]
  (set! (.-count ord) count)
  (set! (.-coprimes ord) (subslice (.-coprimes ord) _ 0))
  (for [i (conv uint32 1)] (<= i count) (inc! i)
    (when (== (gcd i count) 1)
      (set! (.-coprimes ord) (append (.-coprimes ord) i)))))




^{:go/end 8135} (go/method start ^randomEnum [^{:tag (* randomOrder)} ord ^uint32 i]
  ^{:go/breaks [2 4 6]} (lit randomEnum
    :count (.-count ord)
    :pos (% i (.-count ord))
    :inc (aget (.-coprimes ord) (% (/ i (.-count ord)) (conv uint32 (len (.-coprimes ord)))))))



^{:go/end 8139} (go/method done ^bool [^{:tag (* randomEnum)} enum]
  (== (.-i enum) (.-count enum)))


^{:go/end 8144} (go/method next [^{:tag (* randomEnum)} enum]
  (inc! (.-i enum))
  (set! (.-pos enum) (% (+ (.-pos enum) (.-inc enum)) (.-count enum))))


^{:go/end 8148} (go/method position ^uint32 [^{:tag (* randomEnum)} enum]
  (.-pos enum))


^{:go/end 8155} (go/func gcd ^uint32 [^uint32 a ^uint32 b]
  (while (!= b 0)
    (set! (values a b) (values b (% a b))))

  ^{:line 8154} a)




(go/type initTask "An initTask represents the set of initializations that need to be done for a package.\nKeep in sync with ../../test/noinit.go:initTask\n" (struct
    ^{:line 8160 :tag uint32} state ; 0 = uninitialized, 1 = in progress, 2 = done
    ^{:line 8161 :tag uint32} nfns))
;; followed by nfns pcs, uintptr sized, one per init function to run




(go/var ^{:tag tracestat :doc "inittrace stores statistics for init functions which are\nupdated by malloc and newproc when active is true.\n"} inittrace)

(go/type tracestat (struct
    ^{:line 8170 :tag bool} active ; init tracing activation status
    ^{:line 8171 :tag uint64} id ; init goroutine id
    ^{:line 8172 :tag uint64} allocs ; heap allocations
    ^{:line 8173 :tag uint64} bytes)) ; heap allocated bytes


^{:go/end 8180} (go/func doInit [^{:tag (slice (* initTask))} ts]
  (range [_ t ts]
    (doInit1 t)))



^{:go/end 8233} (go/func doInit1 [^{:tag (* initTask)} t]
  (switch (.-state t)
    (case [2] ; fully initialized
      (return))
    (case [1] ; initialization in progress
      (throw "recursive call during initialization - linker skew"))
    (default ; not initialized yet
      (set! (.-state t) 1) ; initialization in progress

      (let [
          ^{:line 8192 :tag int64} start (zero int64)
          ^{:line 8193 :tag tracestat} before (zero tracestat)]


        (when (.-active inittrace)
          (set! start (nanotime))
          ;; Load stats non-atomically since tracinit is updated only by this init goroutine.
          (set! before inittrace))


        (when (== (.-nfns t) 0)
          ;; We should have pruned all of these in the linker.
          (throw "inittask with no functions"))


        (let [firstFunc (add (conv unsafe/Pointer t) 8)]
          (for [i (conv uint32 0)] (< i (.-nfns t)) (inc! i)
            (let [p (add firstFunc (* (conv uintptr i) goarch/PtrSize))
                ^{:line 8210} f @(conv (* (func [])) (conv unsafe/Pointer (addr p)))]
              (f)))


          (when (.-active inittrace)
            (let [end (nanotime)
                ;; Load stats non-atomically since tracinit is updated only by this init goroutine.
                ^{:line 8217} after inittrace

                ^{:line 8219} f @(conv (* (func [])) (conv unsafe/Pointer (addr firstFunc)))
                ^{:line 8220} pkg (funcpkgpath (findfunc (abi/FuncPCABIInternal f)))

                ^{:line 8222 :tag (array 24 byte)} sbuf (zero (array 24 byte))]
              (print "init " pkg " @")
              (print (conv string (fmtNSAsMS (subslice sbuf) (conv uint64 (- start runtimeInitTime)))) " ms, ")
              (print (conv string (fmtNSAsMS (subslice sbuf) (conv uint64 (- end start)))) " ms clock, ")
              (print (conv string (itoa (subslice sbuf) (- (.-bytes after) (.-bytes before)))) " bytes, ")
              (print (conv string (itoa (subslice sbuf) (- (.-allocs after) (.-allocs before)))) " allocs")
              (print "\n")))


          (set! (.-state t) 2)))))) ; initialization done
