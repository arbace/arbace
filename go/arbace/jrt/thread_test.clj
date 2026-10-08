;; jrt's tests: threads over the goroutine-local slot, thread locals, interrupts, handlers,
;; the main thread (doc/go/JRT-NOTES.md, phase 2a).
(in-ns 'go.arbace.jrt)

(go/file "thread_test.go"
  :imports [[io "io"] [strings "strings"] [sync "sync"] [atomic "sync/atomic"] [testing "testing"] [time "time"]])

(go/func TestCurrentThreadPerGoroutine
  "Each goroutine has its own Thread, found again on every call; Go's goroutines get adopted
daemon Threads; the numbers are distinct among live threads and small.\n"
  [^{:tag (* testing/T)} t]
  (let [^sync/WaitGroup wg (zero sync/WaitGroup)
        ^sync/Mutex mu (zero sync/Mutex)
        seen (make (map uint64 bool))
        start (make (chan (struct)))]
    (for [k 0] (< k 100) (inc! k)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (let [th (CurrentThread)]
               (for [j 0] (< j 100) (inc! j)
                 (when (!= (CurrentThread) th)
                   (.Error t "the slot changed")))
               (when (or (not (.IsDaemon__Z th)) (not (strings/HasPrefix (.String (.GetName__String th)) "Thread-")))
                 (.Errorf t "adopted thread %s, daemon %v" (.String (.GetName__String th)) (.IsDaemon__Z th)))
               (let [n (currentThreadID)]
                 (.Lock mu)
                 (when (aget seen n)
                   (.Errorf t "thread number %d twice" n))
                 (aset seen n true)
                 (.Unlock mu)
                 (when (or (== n 0) (> n thinOwnerMax))
                   (.Errorf t "thread number %d" n)))
               ;; stay alive until every goroutine has its number
               (<! start))))))
    (time/Sleep (* 20 time/Millisecond))
    (close start)
    (.Wait wg)))

(go/func TestThreadStartJoin [^{:tag (* testing/T)} t]
  (let [^atomic/Int32 ran (zero atomic/Int32)
        ^{:tag (* Thread)} inner nil
        th (Thread_New_Runnable_String
             (RunnableOf (fn []
                           (set! inner (CurrentThread))
                           (time/Sleep (* 10 time/Millisecond))
                           (.Add ran 1)))
             (Str "worker"))]
    (when (or (.IsAlive__Z th) (!= (.String (.ToString__String th)) (+ "Thread[#" (.String (StrOfLong (.ThreadId__J th))) ",worker,5,main]")))
      (.Errorf t "before start: %s" (.String (.ToString__String th))))
    (.Start__V th)
    (when (!= (res (fn ^string [] (.Start__V th) "x")) "!java.lang.IllegalThreadStateException")
      (.Error t "a second start"))
    (when (!= (res (fn ^string [] (.SetDaemon_Z__V th true) "x")) "!java.lang.IllegalThreadStateException")
      (.Error t "setDaemon after start"))
    (.Join__V th)
    (when (or (!= (.Load ran) 1) (.IsAlive__Z th) (!= inner th))
      (.Errorf t "join: ran %d, alive %v, the thread's CurrentThread %v" (.Load ran) (.IsAlive__Z th) (== inner th)))
    ;; join with a timeout returns while the thread runs
    (let [stop (make (chan (struct)))
          th2 (Thread_New_Runnable (RunnableOf (fn [] (<! stop))))
          t0 (time/Now)]
      (.Start__V th2)
      (.Join_J__V th2 20)
      (when (or (< (time/Since t0) (* 20 time/Millisecond)) (not (.IsAlive__Z th2)))
        (.Error t "join(20)"))
      (close stop)
      (.Join__V th2)
      (when (!= (res (fn ^string [] (.Join_J__V th2 -1) "x")) "!java.lang.IllegalArgumentException: timeout value is negative")
        (.Error t "join(-1)")))
    ;; names, priorities, ids
    (let [a (Thread_New)
          b (Thread_New)]
      (when (or (<= (.ThreadId__J b) (.ThreadId__J a)) (not (strings/HasPrefix (.String (.GetName__String a)) "Thread-")))
        (.Error t "ids, default names"))
      (.SetName_String__V a (Str "renamed"))
      (.SetPriority_I__V a 7)
      (when (or (!= (.String (.GetName__String a)) "renamed") (!= (.GetPriority__I a) 7))
        (.Error t "setName, setPriority"))
      (when (!= (res (fn ^string [] (.SetPriority_I__V a 11) "x")) "!java.lang.IllegalArgumentException")
        (.Error t "setPriority(11)"))
      (.Join__V a))))

(go/func TestThreadLocal [^{:tag (* testing/T)} t]
  (let [tl (ThreadLocal_New)
        n 0
        counter (ThreadLocal_WithInitial_Supplier__ThreadLocal (supplierOf (fn ^any [] (inc! n) (Str "init"))))
        ^sync/WaitGroup wg (zero sync/WaitGroup)]
    (when (!= (.Get__O tl) nil)
      (.Error t "initial null"))
    (.Set_O__V tl (Str "main"))
    (for [k 0] (< k 8) (inc! k)
      (.Add wg 1)
      (Go "tl" (fn []
                 (defer (.Done wg))
                 (when (!= (.Get__O tl) nil)
                   (.Error t "a new thread sees another's value"))
                 (.Set_O__V tl (StrOfInt (conv int32 k)))
                 (time/Sleep time/Millisecond)
                 (when (!= (.String (StrOfObj (.Get__O tl))) (.String (StrOfInt (conv int32 k))))
                   (.Error t "per-thread value")))))
    (.Wait wg)
    (when (!= (.String (StrOfObj (.Get__O tl))) "main")
      (.Error t "the main value"))
    (.Remove__V tl)
    (when (!= (.Get__O tl) nil)
      (.Error t "remove"))
    ;; withInitial: one initialValue per thread, stored
    (.Get__O counter)
    (.Get__O counter)
    (when (or (!= n 1) (!= (.String (StrOfObj (.Get__O counter))) "init"))
      (.Errorf t "withInitial: %d calls" n))
    ;; InheritableThreadLocal: the child starts with the parent's value
    (let [itl (InheritableThreadLocal_New)
          ^any got nil]
      (.Set_O__V itl (Str "parent"))
      (.Join__V (Go "child" (fn [] (set! got (.Get__O itl)))))
      (when (!= (.String (StrOfObj got)) "parent")
        (.Error t "inheritable"))
      (when (or (not (InheritableThreadLocal_InstanceOf itl)) (InheritableThreadLocal_InstanceOf tl) (not (ThreadLocal_InstanceOf itl)))
        (.Error t "instanceof")))))

(go/type testSupplier (struct Object ^{:tag (func [] [any])} f))
(go/method Get__O ^any [^{:tag (* testSupplier)} s] ((.-f s)))
(go/method Is_Supplier [^{:tag (* testSupplier)} s])
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* testSupplier)} s] Object_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* testSupplier)} s] (Object_toString s))
(go/method Clone__O ^any [^{:tag (* testSupplier)} s] nil)
(go/func supplierOf ^Supplier [^{:tag (func [] [any])} f] (addr (lit testSupplier :f f)))

(go/func TestInterrupts [^{:tag (* testing/T)} t]
  ;; sleep, interrupted from another thread
  (let [^string got ""
        th (Thread_New_Runnable (RunnableOf (fn [] (set! got (res (fn ^string [] (Thread_Sleep_J__V 10000) "slept"))))))]
    (.Start__V th)
    (time/Sleep (* 10 time/Millisecond))
    (.Interrupt__V th)
    (.Join__V th)
    (when (!= got "!java.lang.InterruptedException: sleep interrupted")
      (.Errorf t "sleep: %s" got)))
  ;; the status: set, read, cleared by interrupted() and by the exception
  (let [me (CurrentThread)]
    (.Interrupt__V me)
    (when (or (not (.IsInterrupted__Z me)) (not (.IsInterrupted__Z me)))
      (.Error t "isInterrupted"))
    (when (or (not (Thread_Interrupted__Z)) (Thread_Interrupted__Z))
      (.Error t "interrupted() clears"))
    (.Interrupt__V me)
    (when (!= (res (fn ^string [] (Thread_Sleep_J__V 1) "x")) "!java.lang.InterruptedException: sleep interrupted")
      (.Error t "sleep when already interrupted"))
    (when (.IsInterrupted__Z me)
      (.Error t "the exception clears the status")))
  ;; Object.wait
  (let [o (Object_New)
        ^string got ""
        th (Thread_New_Runnable (RunnableOf (fn []
                                              (MonitorEnter o)
                                              (set! got (res (fn ^string [] (Wait o) "woken")))
                                              (when (not (HoldsLock o))
                                                (set! got "monitor not held after wait"))
                                              (MonitorExit o))))]
    (.Start__V th)
    (time/Sleep (* 10 time/Millisecond))
    (.Interrupt__V th)
    (.Join__V th)
    (when (!= got "!java.lang.InterruptedException")
      (.Errorf t "wait: %s" got)))
  ;; join
  (let [stop (make (chan (struct)))
        sleeper (Thread_New_Runnable (RunnableOf (fn [] (<! stop))))
        ^string got ""]
    (.Start__V sleeper)
    (let [joiner (Thread_New_Runnable (RunnableOf (fn [] (set! got (res (fn ^string [] (.Join__V sleeper) "joined"))))))]
      (.Start__V joiner)
      (time/Sleep (* 10 time/Millisecond))
      (.Interrupt__V joiner)
      (.Join__V joiner)
      (close stop)
      (when (!= got "!java.lang.InterruptedException")
        (.Errorf t "join: %s" got))))
  ;; park returns on interrupt and leaves the status set; unpark before park
  (let [me (CurrentThread)]
    (LockSupport_Unpark_Thread__V me)
    (LockSupport_Park__V)
    (let [^bool st false
          th (Thread_New_Runnable (RunnableOf (fn []
                                                (LockSupport_Park_O__V nil)
                                                (set! st (Thread_Interrupted__Z)))))]
      (.Start__V th)
      (time/Sleep (* 10 time/Millisecond))
      (.Interrupt__V th)
      (.Join__V th)
      (when (not st)
        (.Error t "park and the interrupt status")))
    (let [t0 (time/Now)]
      (LockSupport_ParkNanos_J__V 5000000)
      (when (< (time/Since t0) (* 4 time/Millisecond))
        (.Error t "parkNanos returned early")))))

(go/type testHandler (struct Object ^{:tag (func [Thread_I Throwable_I])} f))
(go/method UncaughtException_Thread_Throwable__V [^{:tag (* testHandler)} h ^Thread_I th ^Throwable_I e] ((.-f h) th e))
(go/method Is_Thread_UncaughtExceptionHandler [^{:tag (* testHandler)} h])
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* testHandler)} s] Object_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* testHandler)} s] (Object_toString s))
(go/method Clone__O ^any [^{:tag (* testHandler)} s] nil)

(go/type captureHost "captureHost is the OS host with the standard error and exit captured.\n"
  (struct OSHost ^{:tag (* strings/Builder)} err ^{:tag (* sync/Mutex)} mu ^{:tag (* int)} code))
(go/method Stderr ^{:tag io/Writer} [^captureHost h] (lockedWriter h))
(go/method Exit [^captureHost h ^int code] (set! @(.-code h) code))

(go/type lockedBuilder (struct ^{:tag (* strings/Builder)} b ^{:tag (* sync/Mutex)} mu))
(go/method Write [^lockedBuilder w ^{:tag (slice byte)} p] :results [int error]
  (.Lock (.-mu w))
  (defer (.Unlock (.-mu w)))
  (.Write (.-b w) p))
(go/func lockedWriter ^lockedBuilder [^captureHost h] (lit lockedBuilder :b (.-err h) :mu (.-mu h)))

(go/func withCapturedHost
  "withCapturedHost runs f with the host's standard error captured, and returns it and the
last exit code (-1: none).\n"
  [^{:tag (func [])} f] :results [string int]
  (let [h (lit captureHost :err (new strings/Builder) :mu (new sync/Mutex) :code (new int))]
    (set! @(.-code h) -1)
    (let [old (CurrentHost)]
      (SetHost h)
      (defer (SetHost old))
      (f))
    (.Lock (.-mu h))
    (defer (.Unlock (.-mu h)))
    (return (.String (.-err h)) @(.-code h))))

(go/func TestUncaughtExceptions [^{:tag (* testing/T)} t]
  ;; no handler: the JVM's message on the standard error (System.err)
  (let [(values out _) (withCapturedHost
                         (fn []
                           (.Join__V (Go "failing" (fn [] (panic (Thrown (IllegalStateException_New_String (Str "boom")))))))))]
    (when (not (strings/HasPrefix out "Exception in thread \"failing\" java.lang.IllegalStateException: boom\n\tat "))
      (.Errorf t "uncaught: %q" out)))
  ;; the thread's handler, then the default handler
  (let [^string got ""
        h (addr (lit testHandler :f (fn [^Thread_I th ^Throwable_I e]
                                      (set! got (+ (.String (.GetName__String th)) ": " (.String (.ToString__String e)))))))
        th (Thread_New_Runnable_String (RunnableOf (fn [] (panic (Thrown (ArithmeticException_New_String (Str "x")))))) (Str "h1"))]
    (.SetUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V th h)
    (.Start__V th)
    (.Join__V th)
    (when (!= got "h1: java.lang.ArithmeticException: x")
      (.Errorf t "thread handler: %q" got))
    (Thread_SetDefaultUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V h)
    (.Join__V (Go "h2" (fn [] (panic (Thrown (ArithmeticException_New_String (Str "y")))))))
    (Thread_SetDefaultUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V nil)
    (when (!= got "h2: java.lang.ArithmeticException: y")
      (.Errorf t "default handler: %q" got)))
  ;; printStackTrace writes to the standard error
  (let [(values out _) (withCapturedHost (fn [] (.PrintStackTrace__V (IllegalStateException_New_String (Str "trace")))))]
    (when (not (strings/HasPrefix out "java.lang.IllegalStateException: trace\n"))
      (.Errorf t "printStackTrace: %q" out))))

(go/func TestRunMain
  "RunMain: an uncaught exception gives status 1 after the message; it waits for the
non-daemon threads, not for daemons.\n"
  [^{:tag (* testing/T)} t]
  (let [^int status 0
        ^atomic/Bool finished (zero atomic/Bool)
        (values out _) (withCapturedHost
                         (fn []
                           (let [done (make (chan (struct)))]
                             (go ((fn []
                                    (defer (close done))
                                    (set! status (RunMain (fn []
                                                            (let [th (Thread_New_Runnable
                                                                       (RunnableOf (fn []
                                                                                     (time/Sleep (* 20 time/Millisecond))
                                                                                     (.Store finished true))))]
                                                              (.SetDaemon_Z__V th false)
                                                              (.Start__V th))
                                                            (panic (Thrown (RuntimeException_New_String (Str "main failed"))))))))))
                             (<! done))))]
    (when (or (!= status 1) (not (.Load finished)))
      (.Errorf t "status %d, non-daemon thread finished %v" status (.Load finished)))
    (when (not (strings/HasPrefix out "Exception in thread \"main\" java.lang.RuntimeException: main failed\n"))
      (.Errorf t "main's message: %q" out))))

(go/func TestThreadNumbersRecycled
  "Thread numbers stay small however many threads come and go (the lock word's 22 bits).\n"
  [^{:tag (* testing/T)} t]
  (let [^uint64 maxNum 0]
    (for [k 0] (< k 2000) (inc! k)
      (let [th (Go "n" (fn []
                         (let [n (currentThreadID)]
                           (when (> n (atomic/LoadUint64 (addr maxNum)))
                             (atomic/StoreUint64 (addr maxNum) n)))))]
        (.Join__V th)))
    (when (> (atomic/LoadUint64 (addr maxNum)) 1000)
      (.Errorf t "thread numbers up to %d for 2000 sequential threads" maxNum))))

(go/func TestVirtualThreads [^{:tag (* testing/T)} t]
  (let [f (.Factory__ThreadFactory (.Name_String_J__Thread_Builder_OfVirtual (Thread_OfVirtual__Thread_Builder_OfVirtual) (Str "vt-") 5))
        a (.NewThread_Runnable__Thread f (RunnableOf (fn [])))
        b (.NewThread_Runnable__Thread f (RunnableOf (fn [])))]
    (when (or (!= (.String (.GetName__String a)) "vt-5") (!= (.String (.GetName__String b)) "vt-6")
              (not (.IsVirtual__Z a)) (not (.IsDaemon__Z a)))
      (.Errorf t "virtual threads: %s %s" (.String (.GetName__String a)) (.String (.GetName__String b))))
    (when (!= (res (fn ^string [] (.SetDaemon_Z__V a false) "x")) "!java.lang.IllegalArgumentException: 'false' not legal for virtual threads")
      (.Error t "setDaemon(false) on a virtual thread"))
    (let [^atomic/Bool ran (zero atomic/Bool)
          v (.Start_Runnable__Thread (Thread_OfVirtual__Thread_Builder_OfVirtual) (RunnableOf (fn [] (.Store ran (.IsVirtual__Z (Thread_CurrentThread__Thread))))))]
      (.Join__V v)
      (when (not (.Load ran))
        (.Error t "a started virtual thread")))))

;; ---------------------------------------------------------------------------------------
;; Measurements (JRT-NOTES.md, phase 2a)

(go/func BenchmarkCurrentThread "Thread.currentThread() through the slot\n" [^{:tag (* testing/B)} b]
  (for [i 0] (< i (.-N b)) (inc! i)
    (set! sinkA (Thread_CurrentThread__Thread))))

(go/func BenchmarkThreadLocalGet "ThreadLocal.get, a value set\n" [^{:tag (* testing/B)} b]
  (let [tl (ThreadLocal_New)]
    (.Set_O__V tl (Str "v"))
    (for [i 0] (< i (.-N b)) (inc! i)
      (set! sinkA (.Get__O tl)))))

(go/func BenchmarkThreadStartJoin "new Thread, start, join\n" [^{:tag (* testing/B)} b]
  (let [r (RunnableOf (fn []))]
    (for [i 0] (< i (.-N b)) (inc! i)
      (let [th (Thread_New_Runnable r)]
        (.Start__V th)
        (.Join__V th)))))
