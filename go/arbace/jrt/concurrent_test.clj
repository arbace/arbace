;; jrt's tests: atomics, Unsafe, locks and conditions, executors and futures, latches, under
;; contention (bin/jrt test --race: the race detector checks the happens-before edges)
;; (doc/go/JRT-NOTES.md, phase 2a).
(in-ns 'go.arbace.jrt)

(go/file "concurrent_test.go"
  :imports [[bits "math/bits"] [reflect "reflect"] [strings "strings"] [sync "sync"] [atomic "sync/atomic"]
            [testing "testing"] [time "time"]])

(go/func parallel "parallel runs f(0) ... f(n-1) in n jrt threads and waits for them.\n"
  [^int n ^{:tag (func [int])} f]
  (let [ths (make (slice (* Thread)) n)]
    (for [k 0] (< k n) (inc! k)
      (aset ths k (Go "worker" (fn [] (f k)))))
    (range [_ th ths]
      (.Join__V th))))

(go/func TestAtomics [^{:tag (* testing/T)} t]
  (let [ai (AtomicInteger_New)
        al (AtomicLong_New_J 100)
        ar (AtomicReference_New_O (addr (lit testBox :v 0)))
        lock (AtomicBoolean_New)
        plain 0]
    (parallel 8 (fn [^int k]
                  (for [i 0] (< i 5000) (inc! i)
                    (.IncrementAndGet__I ai)
                    (while true
                      (let [v (.Get__J al)]
                        (when (.CompareAndSet_J_J__Z al v (+ v 2))
                          (break))))
                    (while true
                      (let [old (.Get__O ar)
                            nb (addr (lit testBox :v (+ (.-v (assert (* testBox) old)) 1)))]
                        (when (.CompareAndSet_O_O__Z ar old nb)
                          (break))))
                    ;; AtomicBoolean as a spin lock around a plain counter
                    (while (not (.CompareAndSet_Z_Z__Z lock false true))
                      (Thread_Yield__V))
                    (inc! plain)
                    (.Set_Z__V lock false))))
    (when (or (!= (.Get__I ai) 40000) (!= (.Get__J al) 80100) (!= (.-v (assert (* testBox) (.Get__O ar))) 40000) (!= plain 40000))
      (.Errorf t "atomics: %d %d %d %d" (.Get__I ai) (.Get__J al) (.-v (assert (* testBox) (.Get__O ar))) plain))
    (when (or (!= (.GetAndAdd_I__I ai 5) 40000) (!= (.GetAndSet_I__I ai 1) 40005) (!= (.String (.ToString__String ai)) "1")
              (!= (.DecrementAndGet__I ai) 0) (!= (.LongValue__J al) 80100))
      (.Error t "AtomicInteger's other operations"))
    ;; identity, not equality: a different but equal String does not match
    (let [r (AtomicReference_New_O (Str "a"))]
      (when (.CompareAndSet_O_O__Z r (Str "a") (Str "b"))
        (.Error t "CAS compared values, not identities"))
      (when (or (not (.CompareAndSet_O_O__Z r (.Get__O r) nil)) (!= (.Get__O r) nil) (!= (.String (.ToString__String r)) "null"))
        (.Error t "CAS to null")))))

(go/type testBox (struct Object ^int v))

;; a class as c2g writes one, with the fields Unsafe reaches
(go/type testCell
  (struct Object
          ^int32 F_count
          ^{:tag atomic/Int64} F_total
          ^{:tag atomic/Int32} F_vol
          ^{:tag (* String)} F_name
          ^{:tag (atomic/Pointer ByteArray)} F_buf
          ^any F_obj
          ^Runnable F_task
          ^{:tag (Volatile Runnable)} F_waiter
          ^float32 F_lf
          ^int8 F_b))
(go/type testCellSub (struct testCell ^int32 F_extra))
(go/var testCell_class
  (Define (addr (lit ClassInfo :Name "test.Cell" :Kind KindClass :Modifiers AccPublic :Super Object_class :Go "arbace/jrt.testCell"))))
(go/var testCellSub_class
  (Define (addr (lit ClassInfo :Name "test.CellSub" :Kind KindClass :Modifiers AccPublic :Super testCell_class :Go "arbace/jrt.testCellSub"))))
(go/func init []
  (RegisterGoType testCell_class ((inst reflect/TypeFor testCell)))
  (RegisterGoType testCellSub_class ((inst reflect/TypeFor testCellSub))))

(go/func TestUnsafeFields [^{:tag (* testing/T)} t]
  (let [u (Unsafe_GetUnsafe__Unsafe)
        off (fn ^int64 [^{:tag (* Class)} c ^string n] (.ObjectFieldOffset_Class_String__J u c (Str n)))
        COUNT (off testCell_class "count")
        TOTAL (off testCell_class "total")
        VOL (off testCell_class "vol")
        NAME (off testCell_class "name")
        BUF (off testCell_class "buf")
        OBJ (off testCell_class "obj")
        TASK (off testCell_class "task")
        WAITER (off testCell_class "waiter")
        LF (off testCell_class "lf")
        B (off testCell_class "b")
        SUBCOUNT (off testCellSub_class "count")
        EXTRA (off testCellSub_class "extra")
        c (addr (lit testCell))]
    (when (!= (off testCell_class "count") COUNT)
      (.Error t "objectFieldOffset is not stable"))
    (when (!= (res (fn ^string [] (off testCell_class "nope") "x")) "!java.lang.InternalError: nope")
      (.Error t "a missing field"))
    ;; CAS on an int and a long field under contention
    (parallel 8 (fn [^int k]
                  (for [i 0] (< i 2000) (inc! i)
                    (while true
                      (let [v (.GetIntVolatile_O_J__I u c COUNT)]
                        (when (.CompareAndSetInt_O_J_I_I__Z u c COUNT v (+ v 1))
                          (break))))
                    (.GetAndAddInt_O_J_I__I u c VOL 3)
                    (while true
                      (let [v (.Load (.-F_total c))]
                        (when (.CompareAndSetLong_O_J_J_J__Z u c TOTAL v (+ v 10))
                          (break)))))))
    (when (or (!= (atomic/LoadInt32 (addr (.-F_count c))) 16000) (!= (.Load (.-F_vol c)) 48000) (!= (.Load (.-F_total c)) 160000))
      (.Errorf t "int/long CAS: %d %d %d" (.-F_count c) (.Load (.-F_vol c)) (.Load (.-F_total c))))
    ;; reference slots of each Go representation
    (let [s1 (Str "one")
          s2 (Str "two")
          b1 (NewByteArray 4)
          r1 (RunnableOf (fn []))
          r2 (RunnableOf (fn []))]
      (.PutReference_O_J_O__V u c NAME s1)
      (when (or (!= (.-F_name c) s1) (!= (.GetReference_O_J__O u c NAME) s1))
        (.Error t "pointer field put/get"))
      (when (or (.CompareAndSetReference_O_J_O_O__Z u c NAME s2 s1) (not (.CompareAndSetReference_O_J_O_O__Z u c NAME s1 s2)) (!= (.-F_name c) s2))
        (.Error t "pointer field CAS"))
      (.PutReference_O_J_O__V u c NAME nil)
      (when (!= (.GetReference_O_J__O u c NAME) nil)
        (.Error t "pointer field null"))
      (when (or (not (.CompareAndSetReference_O_J_O_O__Z u c BUF nil b1)) (!= (.Load (.-F_buf c)) b1) (!= (.GetReferenceAcquire_O_J__O u c BUF) b1))
        (.Error t "atomic.Pointer field"))
      (when (or (not (.CompareAndSetReference_O_J_O_O__Z u c OBJ nil s1)) (!= (.-F_obj c) s1)
                (.CompareAndSetReference_O_J_O_O__Z u c OBJ nil s2))
        (.Error t "any field"))
      (when (or (not (.CompareAndSetReference_O_J_O_O__Z u c TASK nil r1)) (!= (.-F_task c) r1)
                (.CompareAndSetReference_O_J_O_O__Z u c TASK r2 r1) (!= (.GetReference_O_J__O u c TASK) r1))
        (.Error t "interface field"))
      (when (or (not (.CompareAndSetReference_O_J_O_O__Z u c WAITER nil r2)) (!= (.Load (.-F_waiter c)) r2)
                (not (.CompareAndSetReference_O_J_O_O__Z u c WAITER r2 nil)) (!= (.Load (.-F_waiter c)) nil))
        (.Error t "volatile interface field")))
    (.PutFloat_O_J_F__V u c LF 0.75)
    (.PutByte_O_J_B__V u c B -3)
    (when (or (!= (.-F_lf c) 0.75) (!= (.-F_b c) -3) (!= (.GetByte_O_J__B u c B) -3))
      (.Error t "float and byte fields"))
    ;; a field of the superclass, through the subclass's offset
    (let [sc (addr (lit testCellSub))]
      (.PutInt_O_J_I__V u sc SUBCOUNT 7)
      (.PutInt_O_J_I__V u sc EXTRA 9)
      (when (or (!= (.-F_count (.-testCell sc)) 7) (!= (.-F_extra sc) 9))
        (.Error t "embedded fields")))))

(go/func TestUnsafeArrays
  "ConcurrentHashMap's table access: tabAt, casTabAt, setTabAt by ABASE + (i << ASHIFT); the
unaligned reads of ArraysSupport.mismatch and the writes of DecimalDigits.\n"
  [^{:tag (* testing/T)} t]
  (let [u (Unsafe_GetUnsafe__Unsafe)
        abase (.ArrayBaseOffset_Class__J u (.ArrayClass Object_class))
        scale (.ArrayIndexScale_Class__I u (.ArrayClass Object_class))
        ashift (- 31 (conv int64 (bits/LeadingZeros32 (conv uint32 scale))))
        tab (NewRefArray Object_class 16)
        slot (fn ^int64 [^int i] (+ (<< (conv int64 i) ashift) abase))]
    (when (or (!= abase 16) (!= scale 4) (!= ashift 2))
      (.Errorf t "base %d scale %d" abase scale))
    (for [i 0] (< i 16) (inc! i)
      (.PutReferenceRelease_O_J_O__V u tab (slot i) (addr (lit testBox :v 0))))
    (parallel 8 (fn [^int k]
                  (for [i 0] (< i 2000) (inc! i)
                    (let [j (% (+ (* i 7) k) 16)]
                      (while true
                        (let [old (.GetReferenceAcquire_O_J__O u tab (slot j))
                              nb (addr (lit testBox :v (+ (.-v (assert (* testBox) old)) 1)))]
                          (when (.CompareAndSetReference_O_J_O_O__Z u tab (slot j) old nb)
                            (break))))))))
    (let [sum 0]
      (range [_ x (.-A tab)]
        (set! sum + (.-v (assert (* testBox) x))))
      (when (!= sum 16000)
        (.Errorf t "casTabAt: sum %d" sum)))
    ;; unaligned access to primitive arrays
    (let [ba (ByteArrayOf 1 2 3 4 5 6 7 8 9)
          ca (CharArrayOf 0x41 0x42 0x43 0x44)]
      (when (or (!= (.GetLongUnaligned_O_J__J u ba (+ Unsafe_ARRAY_BYTE_BASE_OFFSET 1)) 0x0908070605040302)
                (!= (.GetIntUnaligned_O_J__I u ba Unsafe_ARRAY_BYTE_BASE_OFFSET) 0x04030201)
                (!= (.GetLongUnaligned_O_J__J u ca Unsafe_ARRAY_CHAR_BASE_OFFSET) 0x0044004300420041))
        (.Error t "unaligned reads"))
      (.PutCharUnaligned_O_J_C__V u ba (+ Unsafe_ARRAY_BYTE_BASE_OFFSET 3) 0x7a79)
      (.PutByte_O_J_B__V u ba Unsafe_ARRAY_BYTE_BASE_OFFSET 42)
      (when (or (!= (aget (.-A ba) 3) 0x79) (!= (aget (.-A ba) 4) 0x7a) (!= (aget (.-A ba) 0) 42))
        (.Error t "putCharUnaligned, putByte"))
      (when (!= (res (fn ^string [] (.GetLongUnaligned_O_J__J u ba (+ Unsafe_ARRAY_BYTE_BASE_OFFSET 2)) "x"))
                "!java.lang.ArrayIndexOutOfBoundsException: Index 2 out of bounds for length 9")
        (.Error t "an access beyond the array")))
    (when (or (.IsBigEndian__Z u) (!= (.ArrayIndexScale_Class__I u (.ArrayClass Prim_long)) 8))
      (.Error t "isBigEndian, scales"))))

(go/func TestReentrantLock [^{:tag (* testing/T)} t]
  (let [l (ReentrantLock_New)
        counter 0]
    (parallel 8 (fn [^int k]
                  (for [i 0] (< i 2000) (inc! i)
                    (.Lock__V l)
                    (.Lock__V l)
                    (inc! counter)
                    (.Unlock__V l)
                    (.Unlock__V l))))
    (when (or (!= counter 16000) (.IsLocked__Z l))
      (.Errorf t "counter %d" counter))
    (when (!= (res (fn ^string [] (.Unlock__V l) "x")) "!java.lang.IllegalMonitorStateException")
      (.Error t "unlock without the lock"))
    ;; tryLock, timed tryLock and lockInterruptibly against a holder
    (.Lock__V l)
    (let [^string got ""]
      (.Join__V (Go "try" (fn []
                            (let [a (.TryLock__Z l)
                                  t0 (time/Now)
                                  b (.TryLock_J_TimeUnit__Z l 20 TimeUnit_MILLISECONDS)
                                  d (time/Since t0)]
                              (when (or a b (< d (* 20 time/Millisecond)))
                                (set! got "tryLock succeeded or returned early"))))))
      (let [th (Go "intr" (fn [] (set! got (res (fn ^string [] (.LockInterruptibly__V l) "locked")))))]
        (time/Sleep (* 10 time/Millisecond))
        (.Interrupt__V th)
        (.Join__V th)
        (when (!= got "!java.lang.InterruptedException")
          (.Errorf t "lockInterruptibly: %s" got))))
    (when (or (not (.IsHeldByCurrentThread__Z l)) (!= (.GetHoldCount__I l) 1)
              (not (strings/Contains (.String (.ToString__String l)) "[Locked by thread ")))
      (.Errorf t "holder: %s" (.String (.ToString__String l))))
    (.Unlock__V l)))

(go/func TestCondition
  "A bounded buffer (ArrayBlockingQueue's pattern): one lock, notEmpty and notFull.\n"
  [^{:tag (* testing/T)} t]
  (let [l (ReentrantLock_New)
        notEmpty (.NewCondition__Condition l)
        notFull (.NewCondition__Condition l)
        ^{:tag (slice int)} buf nil
        ^atomic/Int64 sum (zero atomic/Int64)]
    (parallel 6 (fn [^int k]
                  (if (< k 3)
                    (for [i 1] (<= i 1000) (inc! i)
                      (.Lock__V l)
                      (while (>= (len buf) 4)
                        (.Await__V notFull))
                      (set! buf (append buf i))
                      (.Signal__V notEmpty)
                      (.Unlock__V l))
                    (for [i 0] (< i 1000) (inc! i)
                      (.Lock__V l)
                      (while (== (len buf) 0)
                        (.Await__V notEmpty))
                      (.Add sum (conv int64 (aget buf 0)))
                      (set! buf (subslice buf 1))
                      (.Signal__V notFull)
                      (.Unlock__V l)))))
    (when (!= (.Load sum) 1501500)
      (.Errorf t "consumed %d" (.Load sum)))
    ;; awaitNanos times out and holds the lock again; await interrupted; signal without the lock
    (.Lock__V l)
    (.Lock__V l)
    (let [left (.AwaitNanos_J__J notEmpty 5000000)]
      (when (or (> left 0) (!= (.GetHoldCount__I l) 2) (.HasWaiters_Condition__Z l notEmpty))
        (.Errorf t "awaitNanos: left %d holds %d" left (.GetHoldCount__I l))))
    (.Unlock__V l)
    (.Unlock__V l)
    (let [^string got ""
          th (Go "await" (fn []
                           (.Lock__V l)
                           (set! got (res (fn ^string [] (.Await__V notEmpty) "signalled")))
                           (when (not (.IsHeldByCurrentThread__Z l))
                             (set! got "lock not held after await"))
                           (.Unlock__V l)))]
      (time/Sleep (* 10 time/Millisecond))
      (.Interrupt__V th)
      (.Join__V th)
      (when (!= got "!java.lang.InterruptedException")
        (.Errorf t "interrupted await: %s" got)))
    (when (!= (res (fn ^string [] (.Signal__V notEmpty) "x")) "!java.lang.IllegalMonitorStateException")
      (.Error t "signal without the lock"))))

(go/func TestReadWriteLock [^{:tag (* testing/T)} t]
  (let [rw (ReentrantReadWriteLock_New)
        r (.ReadLock__ReentrantReadWriteLock_ReadLock rw)
        w (.WriteLock__ReentrantReadWriteLock_WriteLock rw)
        value 0
        ^atomic/Int32 readers (zero atomic/Int32)
        ^atomic/Int32 maxReaders (zero atomic/Int32)]
    (parallel 8 (fn [^int k]
                  (for [i 0] (< i 500) (inc! i)
                    (if (== (% i 5) 0)
                      (do
                        (.Lock__V w)
                        (.Lock__V w)
                        (inc! value)
                        (when (!= (.Load readers) 0)
                          (.Error t "a reader inside the write lock"))
                        (.Unlock__V w)
                        (.Unlock__V w))
                      (do
                        (.Lock__V r)
                        (.Lock__V r)
                        (let [n (.Add readers 1)]
                          (when (> n (.Load maxReaders))
                            (.Store maxReaders n)))
                        (set! _ value)
                        (time/Sleep (* 10 time/Microsecond))
                        (.Add readers -1)
                        (.Unlock__V r)
                        (.Unlock__V r))))))
    (when (!= value 800)
      (.Errorf t "writes %d" value))
    (.Logf t "readers at once: %d" (.Load maxReaders))
    ;; downgrading; a timed write lock against a reader
    (.Lock__V w)
    (.Lock__V r)
    (.Unlock__V w)
    (let [^bool got true]
      (.Join__V (Go "w" (fn [] (set! got (.TryLock_J_TimeUnit__Z w 10 TimeUnit_MILLISECONDS)))))
      (when got
        (.Error t "the write lock while a read lock is held")))
    (.Unlock__V r)
    (when (!= (res (fn ^string [] (.Unlock__V r) "x"))
              "!java.lang.IllegalMonitorStateException: attempt to unlock read lock, not locked by current thread")
      (.Error t "read unlock without the lock"))
    (when (or (not (.TryLock__Z w)) (!= (.GetWriteHoldCount__I rw) 1))
      (.Error t "tryLock of the free write lock"))
    (.Unlock__V w)))

(go/type countingFactory "countingFactory names its daemon threads and counts them.\n"
  (struct Object ^string prefix ^{:tag atomic/Int32} n))
(go/method NewThread_Runnable__Thread ^Thread_I [^{:tag (* countingFactory)} f ^Runnable r]
  (let [th (Thread_New_Runnable_String r (Str (+ (.-prefix f) (.String (StrOfInt (.Add (.-n f) 1))))))]
    (.SetDaemon_Z__V th true)
    th))
(go/method Is_ThreadFactory [^{:tag (* countingFactory)} f])
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* countingFactory)} f] Object_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* countingFactory)} f] (Object_toString f))
(go/method Clone__O ^any [^{:tag (* countingFactory)} f] nil)

(go/type testCallable (struct Object ^{:tag (func [] [any])} f))
(go/method Call__O ^any [^{:tag (* testCallable)} c] ((.-f c)))
(go/method Is_Callable [^{:tag (* testCallable)} c])
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* testCallable)} c] Object_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* testCallable)} c] (Object_toString c))
(go/method Clone__O ^any [^{:tag (* testCallable)} c] nil)
(go/func callableOf ^Callable [^{:tag (func [] [any])} f] (addr (lit testCallable :f f)))

(go/func TestExecutors [^{:tag (* testing/T)} t]
  ;; a fixed pool: at most n tasks at once, all run, futures' results
  (let [f (addr (lit countingFactory :prefix "fixed-"))
        p (Executors_NewFixedThreadPool_I_ThreadFactory__ExecutorService 3 f)
        ^atomic/Int32 running (zero atomic/Int32)
        ^atomic/Int32 most (zero atomic/Int32)
        futs (make (slice Future) 20)]
    (for [i 0] (< i 20) (inc! i)
      (aset futs i (.Submit_Callable__Future p (callableOf (fn ^any []
                                                             (let [n (.Add running 1)]
                                                               (when (> n (.Load most)) (.Store most n)))
                                                             (time/Sleep time/Millisecond)
                                                             (.Add running -1)
                                                             (StrOfInt (conv int32 i)))))))
    (range [i fu futs]
      (when (!= (.String (StrOfObj (.Get__O fu))) (.String (StrOfInt (conv int32 i))))
        (.Errorf t "future %d" i)))
    (when (or (> (.Load most) 3) (> (.Load (.-n f)) 3))
      (.Errorf t "fixed pool: %d at once, %d threads" (.Load most) (.Load (.-n f))))
    (.Shutdown__V p)
    (when (not (strings/HasPrefix (res (fn ^string [] (.Execute_Runnable__V p (RunnableOf (fn []))) "x"))
                                  "!java.util.concurrent.RejectedExecutionException: Task "))
      (.Error t "execute after shutdown"))
    (when (or (not (.AwaitTermination_J_TimeUnit__Z p 5 TimeUnit_SECONDS)) (not (.IsTerminated__Z p)))
      (.Error t "awaitTermination")))
  ;; a cached pool reuses idle workers; exceptions, cancellation, timeouts
  (let [f (addr (lit countingFactory :prefix "cached-"))
        p (Executors_NewCachedThreadPool_ThreadFactory__ExecutorService f)]
    (for [i 0] (< i 5) (inc! i)
      (.Get__O (.Submit_Runnable__Future p (RunnableOf (fn [])))))
    (when (!= (.Load (.-n f)) 1)
      (.Errorf t "cached pool: %d threads for 5 sequential tasks" (.Load (.-n f))))
    (let [failing (.Submit_Callable__Future p (callableOf (fn ^any [] (panic (Thrown (IllegalStateException_New_String (Str "bad")))))))]
      (when (!= (res (fn ^string [] (.Get__O failing) "x")) "!java.util.concurrent.ExecutionException: java.lang.IllegalStateException: bad")
        (.Errorf t "a failing task: %s" (res (fn ^string [] (.Get__O failing) "x")))))
    (let [started (make (chan (struct)))
          ^atomic/Bool interrupted (zero atomic/Bool)
          slow (.Submit_Callable__Future p (callableOf (fn ^any []
                                                          (close started)
                                                          (.Store interrupted (!= (res (fn ^string [] (Thread_Sleep_J__V 5000) "")) ""))
                                                          nil)))]
      (when (!= (res (fn ^string [] (.Get_J_TimeUnit__O slow 10 TimeUnit_MILLISECONDS) "x")) "!java.util.concurrent.TimeoutException")
        (.Error t "get with a timeout"))
      (<! started)
      (when (or (not (.Cancel_Z__Z slow true)) (not (.IsCancelled__Z slow)) (not (.IsDone__Z slow)) (.Cancel_Z__Z slow true))
        (.Error t "cancel"))
      (when (!= (res (fn ^string [] (.Get__O slow) "x")) "!java.util.concurrent.CancellationException")
        (.Error t "get of a cancelled task"))
      (time/Sleep (* 20 time/Millisecond))
      (when (not (.Load interrupted))
        (.Error t "cancel(true) interrupts the runner")))
    ;; execute: a task that throws ends its worker; its exception goes to the handler
    (let [(values out _) (withCapturedHost
                           (fn []
                             (.Execute_Runnable__V p (RunnableOf (fn [] (panic (Thrown (ArithmeticException_New_String (Str "in execute")))))))
                             (time/Sleep (* 20 time/Millisecond))))]
      (when (not (strings/Contains out "java.lang.ArithmeticException: in execute"))
        (.Errorf t "an exception in execute: %q" out)))
    (.Close__V p)
    (when (not (.IsTerminated__Z p))
      (.Error t "close")))
  ;; thread per task, with virtual threads (Agent's)
  (let [p (Executors_NewThreadPerTaskExecutor_ThreadFactory__ExecutorService
            (.Factory__ThreadFactory (.Name_String_J__Thread_Builder_OfVirtual (Thread_OfVirtual__Thread_Builder_OfVirtual) (Str "agent-") 0)))
        ^sync/Mutex mu (zero sync/Mutex)
        names (make (map string bool))
        latch (CountDownLatch_New_I 10)]
    (for [i 0] (< i 10) (inc! i)
      (.Execute_Runnable__V p (RunnableOf (fn []
                                            (.Lock mu)
                                            (aset names (.String (.GetName__String (Thread_CurrentThread__Thread))) (.IsVirtual__Z (Thread_CurrentThread__Thread)))
                                            (.Unlock mu)
                                            (.CountDown__V latch)))))
    (when (not (.Await_J_TimeUnit__Z latch 5 TimeUnit_SECONDS))
      (.Error t "the latch"))
    (.Lock mu)
    (when (or (!= (len names) 10) (not (aget names "agent-0")) (not (aget names "agent-9")))
      (.Errorf t "per-task threads: %v" names))
    (.Unlock mu)
    (when (or (!= (.GetCount__J latch) 0) (not (strings/HasSuffix (.String (.ToString__String latch)) "[Count = 0]")))
      (.Error t "latch count"))
    (.Close__V p)))

(go/func TestCountDownLatch [^{:tag (* testing/T)} t]
  (let [l (CountDownLatch_New_I 3)
        ^atomic/Int32 passed (zero atomic/Int32)
        ths (make (slice (* Thread)) 0)]
    (for [i 0] (< i 4) (inc! i)
      (set! ths (append ths (Go "await" (fn [] (.Await__V l) (.Add passed 1))))))
    (time/Sleep (* 5 time/Millisecond))
    (.CountDown__V l)
    (.CountDown__V l)
    (when (!= (.Load passed) 0)
      (.Error t "passed before zero"))
    (.CountDown__V l)
    (range [_ th ths] (.Join__V th))
    (when (!= (.Load passed) 4)
      (.Error t "not every waiter passed"))
    (when (.Await_J_TimeUnit__Z (CountDownLatch_New_I 1) 5 TimeUnit_MILLISECONDS)
      (.Error t "a timed await"))
    (when (!= (res (fn ^string [] (CountDownLatch_New_I -1) "x")) "!java.lang.IllegalArgumentException: count < 0")
      (.Error t "a negative count"))))

;; ---------------------------------------------------------------------------------------
;; Measurements

(go/func BenchmarkReentrantLock "ReentrantLock lock and unlock, uncontended\n" [^{:tag (* testing/B)} b]
  (let [l (ReentrantLock_New)]
    (for [i 0] (< i (.-N b)) (inc! i)
      (.Lock__V l)
      (.Unlock__V l))))

(go/func BenchmarkAtomicIntegerIncrement [^{:tag (* testing/B)} b]
  (let [a (AtomicInteger_New)]
    (for [i 0] (< i (.-N b)) (inc! i)
      (.IncrementAndGet__I a))))

(go/func BenchmarkAtomicReferenceCAS "AtomicReference.compareAndSet, succeeding (a box allocated)\n" [^{:tag (* testing/B)} b]
  (let [a (AtomicReference_New_O nil)
        x (Str "x")
        y (Str "y")]
    (.Set_O__V a x)
    (for [i 0] (< i (.-N b)) (inc! i)
      (if (== (bit-and i 1) 0)
        (.CompareAndSet_O_O__Z a x y)
        (.CompareAndSet_O_O__Z a y x)))))

(go/func BenchmarkUnsafeCASTable "Unsafe.compareAndSetReference on an Object[] slot (a striped lock)\n" [^{:tag (* testing/B)} b]
  (let [u (Unsafe_GetUnsafe__Unsafe)
        tab (NewRefArray Object_class 16)
        x (Str "x")
        y (Str "y")]
    (aset (.-A tab) 3 x)
    (for [i 0] (< i (.-N b)) (inc! i)
      (if (== (bit-and i 1) 0)
        (.CompareAndSetReference_O_J_O_O__Z u tab 28 x y)
        (.CompareAndSetReference_O_J_O_O__Z u tab 28 y x)))))

(go/func BenchmarkUnsafeCASInt "Unsafe.compareAndSetInt on a field\n" [^{:tag (* testing/B)} b]
  (let [u (Unsafe_GetUnsafe__Unsafe)
        off (.ObjectFieldOffset_Class_String__J u testCell_class (Str "count"))
        c (addr (lit testCell))]
    (for [i 0] (< i (.-N b)) (inc! i)
      (.CompareAndSetInt_O_J_I_I__Z u c off (conv int32 i) (conv int32 (+ i 1))))))
