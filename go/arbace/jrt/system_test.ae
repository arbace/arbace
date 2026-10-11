;; jrt's tests: weak references with a forced GC, System, Runtime and the host
;; (doc/go/JRT-NOTES.md, phase 2a).
(in-ns 'go.arbace.jrt)

(go/file "system_test.go"
  :imports [[runtime "runtime"] [strings "strings"] [testing "testing"] [time "time"]])

(go/func gcUntil "gcUntil collects until ok or 50 tries; whether ok.\n" ^bool [^{:tag (func [] [bool])} ok]
  (for [i 0] (< i 50) (inc! i)
    (runtime/GC)
    (when (ok)
      (return true))
    (time/Sleep time/Millisecond))
  false)

(go/func TestWeakReferences
  "A weak referent that becomes unreachable is cleared and its reference enqueued (Keyword's
table); a reachable one stays; a soft one stays until clear.\n"
  [^{:tag (* testing/T)} t]
  (let [q (ReferenceQueue_New)
        kept (Str "kept")
        wk (WeakReference_New_O_ReferenceQueue kept q)
        wd (WeakReference_New_O_ReferenceQueue (Str "dropped") q)
        wn (WeakReference_New_O (StringBuilder_New))
        sr (SoftReference_New_O (Str "soft"))]
    (when (or (!= (.Get__O wk) kept) (not (.RefersTo_O__Z wk kept)))
      (.Error t "get of a live referent"))
    (when (not (gcUntil (fn ^bool [] (and (== (.Get__O wd) nil) (== (.Get__O wn) nil)))))
      (.Error t "unreachable referents not cleared after GC"))
    (when (not (gcUntil (fn ^bool [] (.IsEnqueued__Z wd))))
      (.Error t "not enqueued"))
    (let [r (.Poll__Reference q)]
      (when (!= r (conv Reference_I wd))
        (.Errorf t "poll: %v" r)))
    (when (!= (.Poll__Reference q) nil)
      (.Error t "a live referent was enqueued"))
    (when (or (!= (.Get__O wk) kept) (== (.Get__O sr) nil))
      (.Error t "live or soft referent cleared"))
    (runtime/KeepAlive kept)
    ;; the Java type of the referent survives the round trip through the weak pointer
    (let [sb (StringBuilder_New_String (Str "abc"))
          w (WeakReference_New_O sb)
          (values back ok) (assert (* StringBuilder) (.Get__O w))]
      (when (or (not ok) (!= back sb))
        (.Error t "the referent's type"))
      (runtime/KeepAlive sb))
    ;; clear and enqueue by hand; remove with a timeout
    (.Clear__V sr)
    (when (!= (.Get__O sr) nil)
      (.Error t "clear"))
    (let [q2 (ReferenceQueue_New)
          w (WeakReference_New_O_ReferenceQueue kept q2)
          t0 (time/Now)]
      (when (or (!= (.Remove_J__Reference q2 10) nil) (< (time/Since t0) (* 10 time/Millisecond)))
        (.Error t "remove(10) on an empty queue"))
      (when (or (not (.Enqueue__Z w)) (.Enqueue__Z w) (!= (.Get__O w) nil) (!= (.Remove__Reference q2) (conv Reference_I w)))
        (.Error t "enqueue"))
      (runtime/KeepAlive kept))))

(go/func TestSystem [^{:tag (* testing/T)} t]
  (when (or (!= (.String (System_LineSeparator__String)) "\n")
            (!= (.String (System_GetProperty_String__String (Str "line.separator"))) "\n")
            (!= (.String (System_GetProperty_String__String (Str "file.separator"))) "/")
            (!= (.String (System_GetProperty_String__String (Str "os.name"))) "Linux"))
    (.Error t "properties"))
  (let [arch (.String (System_GetProperty_String__String (Str "os.arch")))]
    (when (and (!= arch "amd64") (!= arch "aarch64"))
      (.Errorf t "os.arch %s" arch)))
  (when (!= (System_GetProperty_String__String (Str "no.such.property")) nil)
    (.Error t "a missing property"))
  (when (!= (.String (System_GetProperty_String_String__String (Str "no.such.property") (Str "dflt"))) "dflt")
    (.Error t "the default"))
  (when (or (!= (System_SetProperty_String_String__String (Str "jrt.test") (Str "1")) nil)
            (!= (.String (System_SetProperty_String_String__String (Str "jrt.test") (Str "2"))) "1")
            (!= (.String (System_ClearProperty_String__String (Str "jrt.test"))) "2")
            (!= (System_GetProperty_String__String (Str "jrt.test")) nil))
    (.Error t "setProperty, clearProperty"))
  (when (or (!= (res (fn ^string [] (System_GetProperty_String__String nil) "x")) "!java.lang.NullPointerException: key can't be null")
            (!= (res (fn ^string [] (System_GetProperty_String__String (Str "")) "x")) "!java.lang.IllegalArgumentException: key can't be empty"))
    (.Error t "the key checks"))
  (let [a (System_NanoTime__J)
        ms (System_CurrentTimeMillis__J)]
    (time/Sleep time/Millisecond)
    (when (< (- (System_NanoTime__J) a) 1000000)
      (.Error t "nanoTime"))
    (let [d (- ms (.UnixMilli (time/Now)))]
      (when (or (> d 1000) (< d -1000))
        (.Errorf t "currentTimeMillis off by %d ms" d))))
  (when (or (== (System_Getenv_String__String (Str "PATH")) nil) (!= (System_Getenv_String__String (Str "JRT_NO_SUCH_VARIABLE")) nil))
    (.Error t "getenv"))
  (when (< (.AvailableProcessors__I (Runtime_GetRuntime__Runtime)) 1)
    (.Error t "availableProcessors"))
  (let [names (strings/Join (PropertyNames) " ")]
    (when (not (strings/Contains names "user.dir"))
      (.Errorf t "property names: %s" names)))
  ;; exit goes to the host after the shutdown hooks
  (let [^bool hookRan false
        (values _ code) (withCapturedHost
                          (fn []
                            (.AddShutdownHook_Thread__V (Runtime_GetRuntime__Runtime)
                                                        (Thread_New_Runnable (RunnableOf (fn [] (set! hookRan true)))))
                            (System_Exit_I__V 3)))]
    (when (or (!= code 3) (not hookRan))
      (.Errorf t "exit: code %d, hook ran %v" code hookRan)))
  ;; the host's standard streams
  (let [(values out _) (withCapturedHost (fn [] (.WriteString Stderr "to stderr")))]
    (when (!= out "to stderr")
      (.Errorf t "Stderr: %q" out))))
