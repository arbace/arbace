;; jrt: java.lang.Thread over goroutines and the goroutine-local slot of the patched runtime
;; (C2G-SPEC §8.4, §9.3; doc/go/JRT-NOTES.md, phase 2a): the current thread, thread numbers
;; for monitors, start, join, sleep, yield, interrupts, names, daemon status, the
;; uncaught-exception handlers, the thread entry, the main thread.
(in-ns 'go.arbace.jrt)

(go/file "thread.go"
  :imports [[reflect "reflect"] [runtime "runtime"] [strconv "strconv"] [sync "sync"]
            [atomic "sync/atomic"] [time "time"] [unsafe "unsafe"]])

;; ---------------------------------------------------------------------------------------
;; The goroutine-local slot (§9.3): two functions of the patched runtime (overlay/go/runtime,
;; arbace_local.go), pulled by linkname. A build of jrt without the overlay fails to link.

(go/func ^:extern ^{:go/linkname "getLocal runtime.arbace_getLocal"} getLocal
  "getLocal returns the current goroutine's slot: its *Thread, or nil.\n"
  ^unsafe/Pointer [])

(go/func ^:extern ^{:go/linkname "setLocal runtime.arbace_setLocal"} setLocal
  "setLocal sets the current goroutine's slot.\n"
  [^unsafe/Pointer p])

;; ---------------------------------------------------------------------------------------
;; Thread numbers: the owner field of thin locks (monitor.go) holds 22 bits, so numbers are
;; recycled: a jrt thread's number is freed when its run ends, an adopted goroutine's when
;; its Thread is collected (runtime.AddCleanup). 0 is no thread.

(go/var ^{:tag sync/Mutex} numMu)
(go/var ^{:tag (slice uint64)} numFree)
(go/var ^uint64 numNext 1)

(go/func allocNum ^uint64 []
  (.Lock numMu)
  (let [^uint64 n 0]
    (if (> (len numFree) 0)
      (do
        (set! n (aget numFree (- (len numFree) 1)))
        (set! numFree (subslice numFree _ (- (len numFree) 1))))
      (do
        (set! n numNext)
        (inc! numNext)))
    (.Unlock numMu)
    n))

(go/func freeNum [^uint64 n]
  (.Lock numMu)
  (set! numFree (append numFree n))
  (.Unlock numMu))

;; ---------------------------------------------------------------------------------------
;; Thread (a non-leaf class: ForkJoinWorkerThread extends it; Thread_I, Impl_..., §5.4)

(go/type Thread_UncaughtExceptionHandler
  "Thread_UncaughtExceptionHandler is java.lang.Thread$UncaughtExceptionHandler.\n"
  (interface Object_I
    (Is_Thread_UncaughtExceptionHandler [])
    (UncaughtException_Thread_Throwable__V [^Thread_I t ^Throwable_I e])))

(go/var Thread_UncaughtExceptionHandler_class
  (Define (addr (lit ClassInfo :Name "java.lang.Thread$UncaughtExceptionHandler" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccStatic AccInterface AccAbstract)
                     :Declaring Thread_class :Simple "UncaughtExceptionHandler"
                     :Go "arbace/jrt.Thread_UncaughtExceptionHandler"))))

(go/func Thread_UncaughtExceptionHandler_InstanceOf ^bool [^any x]
  (let [(values _ ok) (assert Thread_UncaughtExceptionHandler x)] ok))

(go/func Thread_UncaughtExceptionHandler_Cast ^Thread_UncaughtExceptionHandler [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Thread_UncaughtExceptionHandler x)]
    (when (not ok) (panic (ClassCast x Thread_UncaughtExceptionHandler_class)))
    v))

(go/type Thread_I
  "Thread_I is java.lang.Thread's class interface.\n"
  (interface Object_I
    (Self_Thread ^{:tag (* Thread)} [])
    (Is_Runnable [])
    (Run__V [])
    (Start__V [])
    (Interrupt__V [])
    (IsInterrupted__Z ^bool [])
    (IsAlive__Z ^bool [])
    (Join__V [])
    (Join_J__V [^int64 millis])
    (Join_J_I__V [^int64 millis ^int32 nanos])
    (GetName__String ^{:tag (* String)} [])
    (SetName_String__V [^{:tag (* String)} name])
    (IsDaemon__Z ^bool [])
    (SetDaemon_Z__V [^bool on])
    (GetPriority__I ^int32 [])
    (SetPriority_I__V [^int32 p])
    (GetId__J ^int64 [])
    (ThreadId__J ^int64 [])
    (IsVirtual__Z ^bool [])
    (GetState__Thread_State ^{:tag (* Thread_State)} [])
    (GetUncaughtExceptionHandler__Thread_UncaughtExceptionHandler ^Thread_UncaughtExceptionHandler [])
    (SetUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V [^Thread_UncaughtExceptionHandler h])
    (GetStackTrace__StackTraceElement1 ^{:tag (* RefArray)} [])))

(go/const
  [^{:tag int32 :val 0} threadNew 0]
  [^{:tag int32 :val 1} threadAlive 1]
  [^{:tag int32 :val 2} threadTerminated 2])

(go/type Thread
  "Thread is java.lang.Thread's struct. A started thread is a goroutine whose slot holds the
Thread; a goroutine jrt did not start gets a Thread on its first CurrentThread (adopted).
intr holds the interrupt status as a token (a receive in Object.wait, sleep, join, park
consumes it, as Java clears the status when it throws InterruptedException); park holds
LockSupport's permit.\n"
  (struct Object
          ^Thread_I self
          ^int64 tid
          ^{:tag atomic/Uint64} num
          ^{:tag (atomic/Pointer String)} name
          ^Runnable target
          ^{:tag atomic/Int32} state
          ^{:tag atomic/Bool} daemon
          ^bool virtual
          ^{:tag atomic/Int32} priority
          ^{:tag (chan (struct))} done
          ^{:tag (chan (struct))} intr
          ^{:tag (chan (struct))} park
          ^{:tag (Volatile any) :doc "LockSupport's blocker (getBlocker)\n"} parkBlocker
          ^{:tag (Volatile Thread_UncaughtExceptionHandler)} ueh
          ^{:tag (map (* ThreadLocal) any) :doc "thread locals, used by the thread itself only\n"} locals
          ^{:tag (map (* ThreadLocal) any)} inheritable
          ^{:tag int64 :doc "ThreadLocalRandom's seed, probe and secondary seed, which it reaches through Unsafe (JC4)\n"} F_threadLocalRandomSeed
          ^int32 F_threadLocalRandomProbe
          ^int32 F_threadLocalRandomSecondarySeed
          ^{:tag (* ForkJoinPool) :doc "the pool whose worker the thread is (forkjoin.clj)\n"} fjPool))

(go/var Thread_class
  (Define (addr (lit ClassInfo :Name "java.lang.Thread" :Kind KindClass :Modifiers AccPublic
                     :Super Object_class :Interfaces (lit (slice (* Class)) Runnable_class)
                     :Go "arbace/jrt.Thread"))))

(go/var ^{:tag atomic/Int64} threadIDs)
(go/var ^{:tag atomic/Int64} threadNames)
(go/var ^{:doc "Thread_defaultHandler is the default uncaught-exception handler.\n"
          :tag (Volatile Thread_UncaughtExceptionHandler)} Thread_defaultHandler)

(go/func newThreadStruct "newThreadStruct initializes the fields every Thread has.\n"
  [^{:tag (* Thread)} t ^Thread_I this]
  (set! (.-self t) this)
  (set! (.-tid t) (.Add threadIDs 1))
  (.Store (.-priority t) 5)
  (set! (.-done t) (make (chan (struct))))
  (set! (.-intr t) (make (chan (struct)) 1))
  (set! (.-park t) (make (chan (struct)) 1)))

(go/func Thread_New ^{:tag (* Thread)} []
  (let [t (addr (lit Thread))] (.Ctor t t) t))
(go/func Thread_New_Runnable ^{:tag (* Thread)} [^Runnable task]
  (let [t (addr (lit Thread))] (.Ctor_Runnable t t task) t))
(go/func Thread_New_String ^{:tag (* Thread)} [^{:tag (* String)} name]
  (let [t (addr (lit Thread))] (.Ctor_String t t name) t))
(go/func Thread_New_Runnable_String ^{:tag (* Thread)} [^Runnable task ^{:tag (* String)} name]
  (let [t (addr (lit Thread))] (.Ctor_Runnable_String t t task name) t))

(go/func genThreadName ^{:tag (* String)} []
  (Str (+ "Thread-" (strconv/FormatInt (- (.Add threadNames 1) 1) 10))))

(go/method Ctor "Ctor is Thread().\n" [^{:tag (* Thread)} t ^Thread_I this]
  (.Ctor_Runnable_String t this nil (genThreadName)))
(go/method Ctor_Runnable [^{:tag (* Thread)} t ^Thread_I this ^Runnable task]
  (.Ctor_Runnable_String t this task (genThreadName)))
(go/method Ctor_String [^{:tag (* Thread)} t ^Thread_I this ^{:tag (* String)} name]
  (.Ctor_Runnable_String t this nil name))
(go/method Ctor_Runnable_String
  "Ctor_Runnable_String is Thread(Runnable, String): the new thread is a daemon when the
current thread is one, as Java's.\n"
  [^{:tag (* Thread)} t ^Thread_I this ^Runnable task ^{:tag (* String)} name]
  (when (== name nil)
    (panic (NullPointerException_New_String (Str "'name' is null"))))
  (newThreadStruct t this)
  (.Store (.-name t) name)
  (set! (.-target t) task)
  (let [cur (CurrentThread)]
    (.Store (.-daemon t) (.Load (.-daemon cur)))))

;; markers and generated members
(go/method Self_Thread ^{:tag (* Thread)} [^{:tag (* Thread)} t] t)
(go/method Is_Runnable [^{:tag (* Thread)} t])
(go/method Ref ^any [^{:tag (* Thread)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Thread)} t] Thread_class)
(go/method Clone__O ^any [^{:tag (* Thread)} t] (.Impl_Clone__O t t))

;; the implementations
(go/method Impl_Clone__O "Impl_Clone__O is Thread.clone: CloneNotSupportedException.\n"
  ^any [^{:tag (* Thread)} t ^Thread_I this]
  (panic (CloneNotSupportedException_New)))

(go/method Impl_Run__V "Impl_Run__V is Thread.run: the target's run, if any.\n"
  [^{:tag (* Thread)} t ^Thread_I this]
  (when (!= (.-target t) nil)
    (.Run__V (.-target t))))

(go/method Impl_Start__V
  "Impl_Start__V is Thread.start: the thread's run in a new goroutine (threadEntry).\n"
  [^{:tag (* Thread)} t ^Thread_I this]
  (when (not (.CompareAndSwap (.-state t) threadNew threadAlive))
    (panic (IllegalThreadStateException_New)))
  (let [cur (CurrentThread)]
    ;; InheritableThreadLocal: the child's values are the parent's, through childValue
    (range [k v (.-inheritable cur)]
      (when (== (.-inheritable t) nil)
        (set! (.-inheritable t) (make (map (* ThreadLocal) any))))
      (aset (.-inheritable t) k (.ChildValue_O__O (.-self k) v))))
  (when (not (.Load (.-daemon t)))
    (nonDaemonAdd 1))
  (go (threadEntry t)))

(go/func threadEntry
  "threadEntry is the goroutine of a started Thread (and the entry of every goroutine Java
code starts): it puts the Thread in the slot, runs run, hands an uncaught exception to the
handlers and ends the thread (joiners released, number freed).\n"
  [^{:tag (* Thread)} t]
  (.Store (.-num t) (allocNum))
  (setLocal (conv unsafe/Pointer t))
  (let [exc (runCatching (fn [] (.Run__V (.-self t))))]
    (when (!= exc nil)
      (.dispatchUncaught t exc)))
  (.terminate t))

(go/method terminate [^{:tag (* Thread)} t]
  (set! (.-locals t) nil)
  (set! (.-inheritable t) nil)
  (.Store (.-state t) threadTerminated)
  (close (.-done t))
  (setLocal nil)
  (freeNum (.Swap (.-num t) 0))
  (when (not (.Load (.-daemon t)))
    (nonDaemonAdd -1)))

(go/method dispatchUncaught
  "dispatchUncaught is the JVM's dispatchUncaughtException: the thread's handler, else the
default handler, else the message on the standard error. An exception thrown by a handler is
ignored, as the JVM ignores it.\n"
  [^{:tag (* Thread)} t ^Throwable_I e]
  (let [h (.Load (.-ueh t))]
    (when (== h nil)
      (set! h (.Load Thread_defaultHandler)))
    (if (!= h nil)
      (runCatching (fn [] (.UncaughtException_Thread_Throwable__V h (.-self t) e)))
      (runCatching (fn [] (Uncaught (.String (.Load (.-name t))) e))))))

(go/method Impl_Interrupt__V
  "Impl_Interrupt__V is Thread.interrupt: sets the status (the token), which wakes the
thread from wait, sleep, join, park and the interruptible waits of locks and executors.\n"
  [^{:tag (* Thread)} t ^Thread_I this]
  (select
    (case (>! (.-intr t) (lit (struct))))
    (default)))

(go/method Impl_IsInterrupted__Z ^bool [^{:tag (* Thread)} t ^Thread_I this]
  (> (len (.-intr t)) 0))

(go/method Impl_IsAlive__Z ^bool [^{:tag (* Thread)} t ^Thread_I this]
  (== (.Load (.-state t)) threadAlive))

(go/method Impl_Join__V [^{:tag (* Thread)} t ^Thread_I this]
  (.join t -1))

(go/method Impl_Join_J__V [^{:tag (* Thread)} t ^Thread_I this ^int64 millis]
  (when (< millis 0)
    (panic (IllegalArgumentException_New_String (Str "timeout value is negative"))))
  (if (== millis 0)
    (.join t -1)
    (.join t (durationOf millis 0))))

(go/method Impl_Join_J_I__V [^{:tag (* Thread)} t ^Thread_I this ^int64 millis ^int32 nanos]
  (when (< millis 0)
    (panic (IllegalArgumentException_New_String (Str "timeout value is negative"))))
  (when (or (< nanos 0) (> nanos 999999))
    (panic (IllegalArgumentException_New_String (Str "nanosecond timeout value out of range"))))
  (if (and (== millis 0) (== nanos 0))
    (.join t -1)
    (.join t (durationOf millis nanos))))

(go/method join
  "join waits until t ends, at most d (forever when d < 0); InterruptedException when the
current thread is interrupted. A thread never started is not alive: join returns.\n"
  [^{:tag (* Thread)} t ^{:tag time/Duration} d]
  (let [me (CurrentThread)]
    (when (== (.Load (.-state t)) threadNew)
      (when (interruptedNow me)
        (panic (InterruptedException_New)))
      (return))
    (if (< d 0)
      (select
        (case (<! (.-done t)))
        (case (<! (.-intr me)) (panic (InterruptedException_New))))
      (let [tm (time/NewTimer d)]
        (defer (.Stop tm))
        (select
          (case (<! (.-done t)))
          (case (<! (.-C tm)))
          (case (<! (.-intr me)) (panic (InterruptedException_New))))))))

(go/func interruptedNow
  "interruptedNow consumes t's interrupt status: whether it was set.\n"
  ^bool [^{:tag (* Thread)} t]
  (select
    (case (<! (.-intr t)) (return true))
    (default (return false))))

(go/method Impl_GetName__String ^{:tag (* String)} [^{:tag (* Thread)} t ^Thread_I this]
  (.Load (.-name t)))

(go/method Impl_SetName_String__V [^{:tag (* Thread)} t ^Thread_I this ^{:tag (* String)} name]
  (when (== name nil)
    (panic (NullPointerException_New_String (Str "'name' is null"))))
  (.Store (.-name t) name))

(go/method Impl_IsDaemon__Z ^bool [^{:tag (* Thread)} t ^Thread_I this]
  (.Load (.-daemon t)))

(go/method Impl_SetDaemon_Z__V [^{:tag (* Thread)} t ^Thread_I this ^bool on]
  (when (and (.-virtual t) (not on))
    (panic (IllegalArgumentException_New_String (Str "'false' not legal for virtual threads"))))
  (when (!= (.Load (.-state t)) threadNew)
    (panic (IllegalThreadStateException_New)))
  (.Store (.-daemon t) on))

(go/method Impl_GetPriority__I ^int32 [^{:tag (* Thread)} t ^Thread_I this]
  (.Load (.-priority t)))

(go/method Impl_SetPriority_I__V [^{:tag (* Thread)} t ^Thread_I this ^int32 p]
  (when (or (< p 1) (> p 10))
    (panic (IllegalArgumentException_New)))
  (when (not (.-virtual t))
    (.Store (.-priority t) p)))

(go/method Impl_GetId__J ^int64 [^{:tag (* Thread)} t ^Thread_I this] (.-tid t))
(go/method Impl_ThreadId__J ^int64 [^{:tag (* Thread)} t ^Thread_I this] (.-tid t))
(go/method Impl_IsVirtual__Z ^bool [^{:tag (* Thread)} t ^Thread_I this] (.-virtual t))
(go/method Impl_GetState__Thread_State
  "Impl_GetState__Thread_State is getState: NEW before start, TERMINATED after the run, else
RUNNABLE (jrt does not tell a thread blocked or waiting from a running one: JC8).\n"
  ^{:tag (* Thread_State)} [^{:tag (* Thread)} t ^Thread_I this]
  (switch (.Load (.-state t))
    (case [threadNew] (return Thread_State_NEW))
    (case [threadTerminated] (return Thread_State_TERMINATED)))
  Thread_State_RUNNABLE)

;; ---- java.lang.Thread.State, an enum (§7.13)

(go/type Thread_State "Thread_State is the enum java.lang.Thread.State.\n" (struct Enum))

(go/var Thread_State_class
  (Define (addr (lit ClassInfo :Name "java.lang.Thread$State" :Kind KindEnum
                     :Modifiers (bit-or AccPublic AccStatic AccFinal AccEnum) :Super Enum_class
                     :Declaring Thread_class :Simple "State" :Go "arbace/jrt.Thread_State"))))

(go/func newThreadState ^{:tag (* Thread_State)} [^string n ^int32 o]
  (let [t (addr (lit Thread_State))]
    (.Ctor_String_I (.-Enum t) t (Intern n) o)
    t))

(go/var
  [^{:tag (* Thread_State) :doc "Thread_State_NEW is Thread.State.NEW.\n"} Thread_State_NEW (newThreadState "NEW" 0)]
  [^{:tag (* Thread_State)} Thread_State_RUNNABLE (newThreadState "RUNNABLE" 1)]
  [^{:tag (* Thread_State)} Thread_State_BLOCKED (newThreadState "BLOCKED" 2)]
  [^{:tag (* Thread_State)} Thread_State_WAITING (newThreadState "WAITING" 3)]
  [^{:tag (* Thread_State)} Thread_State_TIMED_WAITING (newThreadState "TIMED_WAITING" 4)]
  [^{:tag (* Thread_State)} Thread_State_TERMINATED (newThreadState "TERMINATED" 5)])

(go/func Thread_State_Values__Thread_State1 ^{:tag (* RefArray)} []
  (RefArrayOf Thread_State_class Thread_State_NEW Thread_State_RUNNABLE Thread_State_BLOCKED
              Thread_State_WAITING Thread_State_TIMED_WAITING Thread_State_TERMINATED))

(go/func Thread_State_ValueOf_String__Thread_State ^{:tag (* Thread_State)} [^{:tag (* String)} n]
  (assert (* Thread_State) (Enum_ValueOf_Class_String__Enum Thread_State_class n)))

(go/method Ref ^any [^{:tag (* Thread_State)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* Thread_State)} t] Thread_State_class)
(go/method Clone__O ^any [^{:tag (* Thread_State)} t] (.Impl_Clone__O t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Thread_State)} t] (.Impl_ToString__String t t))
(go/method CompareTo_Enum__I ^int32 [^{:tag (* Thread_State)} t ^Enum_I o] (.Impl_CompareTo_Enum__I t t o))
(go/method CompareTo_O__I ^int32 [^{:tag (* Thread_State)} t ^any o] (.Impl_CompareTo_O__I t t o))
(go/method GetDeclaringClass__Class ^{:tag (* Class)} [^{:tag (* Thread_State)} t] (.Impl_GetDeclaringClass__Class t t))
(go/func Thread_State_Cast ^{:tag (* Thread_State)} [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert (* Thread_State) x)]
    (when (not ok) (panic (ClassCast x Thread_State_class)))
    v))
(go/func Thread_State_InstanceOf ^bool [^any x] (let [(values _ ok) (assert (* Thread_State) x)] ok))

(go/method Impl_GetUncaughtExceptionHandler__Thread_UncaughtExceptionHandler
  ^Thread_UncaughtExceptionHandler [^{:tag (* Thread)} t ^Thread_I this]
  (.Load (.-ueh t)))

(go/method Impl_SetUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V
  [^{:tag (* Thread)} t ^Thread_I this ^Thread_UncaughtExceptionHandler h]
  (.Store (.-ueh t) h))

(go/method Impl_GetStackTrace__StackTraceElement1
  "Impl_GetStackTrace__StackTraceElement1 is Thread.getStackTrace: the current thread's
frames (Java's frames, as an exception's); another thread's stack cannot be read from Go: an
empty array.\n"
  ^{:tag (* RefArray)} [^{:tag (* Thread)} t ^Thread_I this]
  (when (== (CurrentThread) t)
    (return (javaFrames (callers 2) (evalTrace))))
  (NewRefArray StackTraceElement_class 0))

(go/method Impl_ToString__String
  "Impl_ToString__String is Thread.toString: Thread[#id,name,priority,group] (the group is
main for every platform thread), VirtualThread[#id,name]/state for a virtual one.\n"
  ^{:tag (* String)} [^{:tag (* Thread)} t ^Thread_I this]
  (let [id (strconv/FormatInt (.-tid t) 10)
        n (.String (.Load (.-name t)))]
    (when (.-virtual t)
      (let [s (+ "VirtualThread[#" id)]
        (when (!= n "")
          (set! s (+ s "," n)))
        (set! s (+ s "]/"))
        (switch (.Load (.-state t))
          (case [threadNew] (set! s (+ s "new")))
          (case [threadAlive] (set! s (+ s "runnable")))
          (default (set! s (+ s "terminated"))))
        (return (Str s))))
    (let [g ",main]"]
      (when (== (.Load (.-state t)) threadTerminated)
        (set! g ",]"))
      (Str (+ "Thread[#" id "," n "," (strconv/Itoa (conv int (.Load (.-priority t)))) g)))))

;; Thread's dispatch methods (§5.4)
(go/method Run__V [^{:tag (* Thread)} t] (.Impl_Run__V t t))
(go/method Start__V [^{:tag (* Thread)} t] (.Impl_Start__V t t))
(go/method Interrupt__V [^{:tag (* Thread)} t] (.Impl_Interrupt__V t t))
(go/method IsInterrupted__Z ^bool [^{:tag (* Thread)} t] (.Impl_IsInterrupted__Z t t))
(go/method IsAlive__Z ^bool [^{:tag (* Thread)} t] (.Impl_IsAlive__Z t t))
(go/method Join__V [^{:tag (* Thread)} t] (.Impl_Join__V t t))
(go/method Join_J__V [^{:tag (* Thread)} t ^int64 m] (.Impl_Join_J__V t t m))
(go/method Join_J_I__V [^{:tag (* Thread)} t ^int64 m ^int32 n] (.Impl_Join_J_I__V t t m n))
(go/method GetName__String ^{:tag (* String)} [^{:tag (* Thread)} t] (.Impl_GetName__String t t))
(go/method SetName_String__V [^{:tag (* Thread)} t ^{:tag (* String)} n] (.Impl_SetName_String__V t t n))
(go/method IsDaemon__Z ^bool [^{:tag (* Thread)} t] (.Impl_IsDaemon__Z t t))
(go/method SetDaemon_Z__V [^{:tag (* Thread)} t ^bool on] (.Impl_SetDaemon_Z__V t t on))
(go/method GetPriority__I ^int32 [^{:tag (* Thread)} t] (.Impl_GetPriority__I t t))
(go/method SetPriority_I__V [^{:tag (* Thread)} t ^int32 p] (.Impl_SetPriority_I__V t t p))
(go/method GetId__J ^int64 [^{:tag (* Thread)} t] (.Impl_GetId__J t t))
(go/method ThreadId__J ^int64 [^{:tag (* Thread)} t] (.Impl_ThreadId__J t t))
(go/method IsVirtual__Z ^bool [^{:tag (* Thread)} t] (.Impl_IsVirtual__Z t t))
(go/method GetState__Thread_State ^{:tag (* Thread_State)} [^{:tag (* Thread)} t] (.Impl_GetState__Thread_State t t))
(go/method GetUncaughtExceptionHandler__Thread_UncaughtExceptionHandler ^Thread_UncaughtExceptionHandler
  [^{:tag (* Thread)} t] (.Impl_GetUncaughtExceptionHandler__Thread_UncaughtExceptionHandler t t))
(go/method SetUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V
  [^{:tag (* Thread)} t ^Thread_UncaughtExceptionHandler h]
  (.Impl_SetUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V t t h))
(go/method GetStackTrace__StackTraceElement1 ^{:tag (* RefArray)} [^{:tag (* Thread)} t]
  (.Impl_GetStackTrace__StackTraceElement1 t t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* Thread)} t] (.Impl_ToString__String t t))

(go/func Thread_InstanceOf ^bool [^any x]
  (let [(values _ ok) (assert Thread_I x)] ok))

(go/func Thread_Cast ^Thread_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Thread_I x)]
    (when (not ok) (panic (ClassCast x Thread_class)))
    v))

;; ---------------------------------------------------------------------------------------
;; The current thread

(go/func nnIface
  "nnIface is x, or NullPointerException when x is null: NN for interface types.\n"
  :type-params [T] ^T [^T x]
  (when (== (conv any x) nil)
    (panic (NPE)))
  x)

(go/func CurrentThread
  "CurrentThread is the current thread's Thread, from the goroutine-local slot (about 4 ns);
a goroutine jrt did not start gets one on first use (adopt).\n"
  ^{:tag (* Thread)} []
  (let [p (getLocal)]
    (when (!= p nil)
      (return (conv (* Thread) p)))
    (adopt nil)))

(go/func adopt
  "adopt gives the current goroutine, which jrt did not start, a Thread: a daemon named
Thread-N, alive; its number is freed when the Thread is collected (the slot is cleared when
the goroutine ends).\n"
  ^{:tag (* Thread)} [^{:tag (* String)} name]
  (let [t (addr (lit Thread))
        n (allocNum)]
    (newThreadStruct t t)
    (when (== name nil)
      (set! name (genThreadName)))
    (.Store (.-name t) name)
    (.Store (.-daemon t) true)
    (.Store (.-state t) threadAlive)
    (.Store (.-num t) n)
    (setLocal (conv unsafe/Pointer t))
    (runtime/AddCleanup t freeNum n)
    t))

(go/func Thread_CurrentThread__Thread "Thread_CurrentThread__Thread is Thread.currentThread().\n"
  ^Thread_I []
  (.-self (CurrentThread)))

(go/func Thread_Interrupted__Z
  "Thread_Interrupted__Z is Thread.interrupted(): the current thread's status, cleared.\n"
  ^bool []
  (interruptedNow (CurrentThread)))

(go/func durationOf ^{:tag time/Duration} [^int64 millis ^int32 nanos]
  (when (> millis 9223372036854)
    (return (conv time/Duration 9223372036854775807)))
  (+ (* (conv time/Duration millis) time/Millisecond) (conv time/Duration nanos)))

(go/func sleepFor
  "sleepFor sleeps d; InterruptedException(\"sleep interrupted\") when the current thread is
interrupted before or during the sleep (the status cleared).\n"
  [^{:tag time/Duration} d]
  (let [me (CurrentThread)]
    (when (interruptedNow me)
      (panic (InterruptedException_New_String (Str "sleep interrupted"))))
    (when (<= d 0)
      (runtime/Gosched)
      (return))
    (let [tm (time/NewTimer d)]
      (defer (.Stop tm))
      (select
        (case (<! (.-C tm)))
        (case (<! (.-intr me))
          (panic (InterruptedException_New_String (Str "sleep interrupted"))))))))

(go/func Thread_Sleep_J__V "Thread_Sleep_J__V is Thread.sleep(long).\n" [^int64 millis]
  (when (< millis 0)
    (panic (IllegalArgumentException_New_String (Str "timeout value is negative"))))
  (sleepFor (durationOf millis 0)))

(go/func Thread_Sleep_J_I__V "Thread_Sleep_J_I__V is Thread.sleep(long, int).\n" [^int64 millis ^int32 nanos]
  (when (< millis 0)
    (panic (IllegalArgumentException_New_String (Str "timeout value is negative"))))
  (when (or (< nanos 0) (> nanos 999999))
    (panic (IllegalArgumentException_New_String (Str "nanosecond timeout value out of range"))))
  (sleepFor (durationOf millis nanos)))

(go/const
  [^{:tag int32 :val 1 :doc "Thread_MIN_PRIORITY is Thread.MIN_PRIORITY.\n"} Thread_MIN_PRIORITY 1]
  [^{:tag int32 :val 5} Thread_NORM_PRIORITY 5]
  [^{:tag int32 :val 10} Thread_MAX_PRIORITY 10])

(go/func Thread_DumpStack__V "Thread_DumpStack__V is Thread.dumpStack(): a stack trace on the standard error.\n" []
  (.PrintStackTrace__V (Exception_New_String (Str "Stack trace"))))

(go/func Thread_StartVirtualThread_Runnable__Thread
  "Thread_StartVirtualThread_Runnable__Thread is Thread.startVirtualThread: an unnamed virtual
thread, started.\n"
  ^Thread_I [^Runnable task]
  (let [t (newVirtualThread (Intern "") (nnIface task))]
    (.Start__V t)
    t))

(go/func Thread_Yield__V "Thread_Yield__V is Thread.yield(): runtime.Gosched.\n" []
  (runtime/Gosched))

(go/func Thread_OnSpinWait__V "Thread_OnSpinWait__V is Thread.onSpinWait(): a hint, nothing.\n" [])

(go/func Thread_HoldsLock_O__Z "Thread_HoldsLock_O__Z is Thread.holdsLock.\n" ^bool [^any x]
  (when (== x nil)
    (panic (NPE)))
  (HoldsLock x))

(go/func Thread_GetDefaultUncaughtExceptionHandler__Thread_UncaughtExceptionHandler
  ^Thread_UncaughtExceptionHandler []
  (.Load Thread_defaultHandler))

(go/func Thread_SetDefaultUncaughtExceptionHandler_Thread_UncaughtExceptionHandler__V
  [^Thread_UncaughtExceptionHandler h]
  (.Store Thread_defaultHandler h))

;; ---------------------------------------------------------------------------------------
;; Non-daemon threads and the main thread

(go/var ^{:tag sync/Mutex} nonDaemonMu)
(go/var ^{:tag sync/Cond} nonDaemonCond)
(go/var ^int64 nonDaemonCount)

(go/func nonDaemonAdd [^int64 d]
  (.Lock nonDaemonMu)
  (set! nonDaemonCount + d)
  (when (== nonDaemonCount 0)
    (.Broadcast nonDaemonCond))
  (.Unlock nonDaemonMu))

(go/func WaitNonDaemon
  "WaitNonDaemon waits until every started non-daemon thread has ended, as the JVM does
before it exits after main.\n"
  []
  (.Lock nonDaemonMu)
  (while (> nonDaemonCount 0)
    (.Wait nonDaemonCond))
  (.Unlock nonDaemonMu))

(go/var ^{:tag sync/Once} keepAliveOnce)

(go/func keepAlive
  "keepAlive starts, once, a goroutine that waits on a ticker for the rest of the program, so
that Go's run time never ends it with \"all goroutines are asleep - deadlock!\": a JVM whose
threads all wait forever (a fixed pool's idle workers that no one shut down, which main's end
waits for, or a deadlock) waits forever too, and the Go build does as the JVM does
(JRT-NOTES.md, \"Concurrency\", JC9). A pending timer is what Go's deadlock check looks for;
the ticker fires once an hour.\n"
  []
  (.Do keepAliveOnce
       (fn []
         (go ((fn []
                (let [tk (time/NewTicker time/Hour)]
                  (while true
                    (<! (.-C tk))))))))))

(go/func init []
  (set! (.-L nonDaemonCond) (addr nonDaemonMu))
  ;; ThreadLocalRandom's fields, for Unsafe.objectFieldOffset (JC4)
  (RegisterGoType Thread_class ((inst reflect/TypeFor Thread)))
  (set! (.-IsInstance (.Info Thread_class)) Thread_InstanceOf)
  (set! (.-IsInstance (.Info Thread_UncaughtExceptionHandler_class)) Thread_UncaughtExceptionHandler_InstanceOf)
  ;; Object.wait selects on the current thread's interrupt token (monitor.go)
  (set! interruptChan (fn ^{:tag (chan :recv (struct))} [] (.-intr (CurrentThread)))))

(go/func RunMain
  "RunMain runs a program's main as the JVM runs it: on the calling goroutine, made the
thread main (#1 when called first, non-daemon); an uncaught exception goes to the handlers
(Exception in thread \"main\" ...) and makes the status 1; then it waits for the non-daemon
threads, runs the shutdown hooks (as the JVM's DestroyJavaVM does) and returns the exit status
(System.exit ends the process before).\n"
  ^int [^{:tag (func [])} run]
  (let [^{:tag (* Thread)} t nil]
    (if (== (getLocal) nil)
      (set! t (adopt (Str "main")))
      (do
        (set! t (CurrentThread))
        (.Store (.-name t) (Str "main"))))
    (.Store (.-daemon t) false)
    (keepAlive)
    (let [exc (runCatching run)
          status 0]
      (when (!= exc nil)
        (.dispatchUncaught t exc)
        (set! status 1))
      (WaitNonDaemon)
      (runShutdownHooks)
      status)))

(go/func Go
  "Go starts f in a new jrt thread (a daemon goroutine with its Thread in the slot), named
name, as the thread entry runs Java code: an uncaught exception goes to the handlers. It
returns the Thread.\n"
  ^{:tag (* Thread)} [^string name ^{:tag (func [])} f]
  (let [t (Thread_New_Runnable_String (RunnableOf f) (Str name))]
    (.Store (.-daemon t) true)
    (.Start__V t)
    t))

;; ---------------------------------------------------------------------------------------
;; A Runnable of a Go function (jrt's executors and tests)

(go/type goRunnable
  "goRunnable is a Runnable whose run calls a Go function.\n"
  (struct Object ^{:tag (func [])} f))

(go/var goRunnable_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.jrt.GoRunnable" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class
                     :Interfaces (lit (slice (* Class)) Runnable_class)
                     :Go "arbace/jrt.goRunnable"))))

(go/func RunnableOf "RunnableOf is a Runnable whose run calls f.\n" ^Runnable [^{:tag (func [])} f]
  (addr (lit goRunnable :f f)))

(go/method Run__V [^{:tag (* goRunnable)} r] ((.-f r)))
(go/method Is_Runnable [^{:tag (* goRunnable)} r])
(go/method Ref ^any [^{:tag (* goRunnable)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* goRunnable)} t] goRunnable_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* goRunnable)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* goRunnable)} t] (panic (CloneNotSupported t)))

;; ---------------------------------------------------------------------------------------
;; Virtual threads: Thread.ofVirtual() (Agent's executor); goroutines, as every thread

(go/type Thread_Builder
  "Thread_Builder is java.lang.Thread$Builder (the members of it jrt has: the builder's thread
factory and threads; Executors.newVirtualThreadPerTaskExecutor calls factory() on it).\n"
  (interface Object_I
    (Is_Thread_Builder [])
    (Factory__ThreadFactory ^ThreadFactory [])
    (Unstarted_Runnable__Thread ^Thread_I [^Runnable task])
    (Start_Runnable__Thread ^Thread_I [^Runnable task])))

(go/var Thread_Builder_class
  (Define (addr (lit ClassInfo :Name "java.lang.Thread$Builder" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccStatic AccInterface AccAbstract)
                     :Declaring Thread_class :Simple "Builder" :Go "arbace/jrt.Thread_Builder"))))

(go/func Thread_Builder_Cast ^Thread_Builder [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert Thread_Builder x)]
    (when (not ok) (panic (ClassCast x Thread_Builder_class)))
    v))

(go/type Thread_Builder_OfVirtual
  "Thread_Builder_OfVirtual is java.lang.Thread$Builder$OfVirtual (the members Arbace uses).\n"
  (interface Thread_Builder
    (Is_Thread_Builder_OfVirtual [])
    (Name_String__Thread_Builder_OfVirtual ^Thread_Builder_OfVirtual [^{:tag (* String)} name])
    (Name_String_J__Thread_Builder_OfVirtual ^Thread_Builder_OfVirtual [^{:tag (* String)} prefix ^int64 start])))

(go/var Thread_Builder_OfVirtual_class
  (Define (addr (lit ClassInfo :Name "java.lang.Thread$Builder$OfVirtual" :Kind KindInterface
                     :Modifiers (bit-or AccPublic AccStatic AccInterface AccAbstract)
                     :Interfaces (lit (slice (* Class)) Thread_Builder_class)
                     :Simple "OfVirtual" :Go "arbace/jrt.Thread_Builder_OfVirtual"))))

(go/type virtualBuilder
  "virtualBuilder is the builder of Thread.ofVirtual(): a name or a counted name prefix.\n"
  (struct Object
          ^{:tag (* String)} name
          ^bool counted
          ^{:tag (* atomic/Int64)} next))

(go/var virtualBuilder_class
  (Define (addr (lit ClassInfo :Name "java.lang.ThreadBuilders$VirtualThreadBuilder" :Kind KindClass
                     :Modifiers AccFinal :Super Object_class
                     :Interfaces (lit (slice (* Class)) Thread_Builder_OfVirtual_class)
                     :Go "arbace/jrt.virtualBuilder"))))

(go/func Thread_OfVirtual__Thread_Builder_OfVirtual "Thread_OfVirtual__Thread_Builder_OfVirtual is Thread.ofVirtual().\n"
  ^Thread_Builder_OfVirtual []
  (addr (lit virtualBuilder :name (Str ""))))

(go/method Name_String__Thread_Builder_OfVirtual ^Thread_Builder_OfVirtual
  [^{:tag (* virtualBuilder)} b ^{:tag (* String)} name]
  (set! (.-name b) (NN name))
  (set! (.-counted b) false)
  b)

(go/method Name_String_J__Thread_Builder_OfVirtual ^Thread_Builder_OfVirtual
  [^{:tag (* virtualBuilder)} b ^{:tag (* String)} prefix ^int64 start]
  (when (< start 0)
    (panic (IllegalArgumentException_New_String (Str (+ "'start' is negative: " (strconv/FormatInt start 10))))))
  (set! (.-name b) (NN prefix))
  (set! (.-counted b) true)
  (set! (.-next b) (new atomic/Int64))
  (.Store (.-next b) start)
  b)

(go/method nextName ^{:tag (* String)} [^{:tag (* virtualBuilder)} b]
  (when (.-counted b)
    (return (Concat (.-name b) (StrOfLong (- (.Add (.-next b) 1) 1)))))
  (.-name b))

(go/method Unstarted_Runnable__Thread ^Thread_I [^{:tag (* virtualBuilder)} b ^Runnable task]
  (newVirtualThread (.nextName b) (nnIface task)))

(go/method Start_Runnable__Thread ^Thread_I [^{:tag (* virtualBuilder)} b ^Runnable task]
  (let [t (newVirtualThread (.nextName b) (nnIface task))]
    (.Start__V t)
    t))

(go/func newVirtualThread "newVirtualThread is a virtual thread: a daemon, never a platform thread.\n"
  ^{:tag (* Thread)} [^{:tag (* String)} name ^Runnable task]
  (let [t (addr (lit Thread))]
    (newThreadStruct t t)
    (.Store (.-name t) name)
    (set! (.-target t) task)
    (set! (.-virtual t) true)
    (.Store (.-daemon t) true)
    t))

(go/method Factory__ThreadFactory ^ThreadFactory [^{:tag (* virtualBuilder)} b]
  (let [f (addr (lit virtualBuilder :name (.-name b) :counted (.-counted b) :next (.-next b)))]
    (addr (lit virtualFactory :b f))))

(go/method Is_Thread_Builder_OfVirtual [^{:tag (* virtualBuilder)} b])
(go/method Is_Thread_Builder [^{:tag (* virtualBuilder)} b])
(go/method Ref ^any [^{:tag (* virtualBuilder)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* virtualBuilder)} t] virtualBuilder_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* virtualBuilder)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* virtualBuilder)} t] (panic (CloneNotSupported t)))

(go/type virtualFactory "virtualFactory is the ThreadFactory of a virtual-thread builder.\n"
  (struct Object ^{:tag (* virtualBuilder)} b))

(go/var virtualFactory_class
  (Define (addr (lit ClassInfo :Name "java.lang.ThreadBuilders$VirtualThreadFactory" :Kind KindClass
                     :Modifiers AccFinal :Super Object_class
                     :Interfaces (lit (slice (* Class)) ThreadFactory_class)
                     :Go "arbace/jrt.virtualFactory"))))

(go/method NewThread_Runnable__Thread ^Thread_I [^{:tag (* virtualFactory)} f ^Runnable task]
  (newVirtualThread (.nextName (.-b f)) (nnIface task)))
(go/method Is_ThreadFactory [^{:tag (* virtualFactory)} f])
(go/method Ref ^any [^{:tag (* virtualFactory)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* virtualFactory)} t] virtualFactory_class)
(go/method ToString__String ^{:tag (* String)} [^{:tag (* virtualFactory)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* virtualFactory)} t] (panic (CloneNotSupported t)))

(go/func init []
  (set! (.-IsInstance (.Info Thread_State_class)) Thread_State_InstanceOf)
  (set! (.-Enum (.Info Thread_State_class)) Thread_State_Values__Thread_State1)
  (set! (.-IsInstance (.Info Thread_Builder_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert Thread_Builder x)] ok)))
  (set! (.-IsInstance (.Info Thread_Builder_OfVirtual_class))
        (fn ^bool [^any x] (let [(values _ ok) (assert Thread_Builder_OfVirtual x)] ok))))

;; ---------------------------------------------------------------------------------------
;; The header of any Java object without an interface call (monitors' fast path)

(go/func header
  "header is x's header: every Java object's struct begins with the header (§5.2), so its
address is the object's; NullPointerException on null.\n"
  ^{:tag (* Object)} [^any x]
  (when (== x nil)
    (panic (NPE)))
  (conv (* Object) (dataOf x)))
