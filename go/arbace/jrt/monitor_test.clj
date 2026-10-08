;; jrt's tests: monitors (C2G-SPEC §8.1): reentrancy, mutual exclusion, inflation and
;; deflation, wait and notify, the owner checks.
(in-ns 'go.arbace.jrt)

(go/file "monitor_test.go"
  :imports [[sync "sync"] [atomic "sync/atomic"] [testing "testing"] [time "time"]])

(go/func monitorCount ^int []
  (.Lock monMu)
  (let [n (len monitors)]
    (.Unlock monMu)
    n))

(go/func TestMonitorReentrant [^{:tag (* testing/T)} t]
  (let [o (Object_New)]
    ;; deep recursion overflows the thin lock's count and inflates
    (for [i 0] (< i 1000) (inc! i)
      (MonitorEnter o))
    (when (not (HoldsLock o))
      (.Error t "holdsLock"))
    (for [i 0] (< i 1000) (inc! i)
      (MonitorExit o))
    (when (HoldsLock o)
      (.Error t "held after the last exit"))
    (when (!= (res (fn ^string [] (MonitorExit o) "x")) "!java.lang.IllegalMonitorStateException: current thread is not owner")
      (.Error t "exit of an unowned monitor"))
    (when (!= (res (fn ^string [] (MonitorEnter nil) "x")) "!java.lang.NullPointerException")
      (.Error t "monitorenter null"))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))

(go/func TestMonitorExclusion
  "Goroutines incrementing a plain counter under one monitor lose no update (and the race
detector, bin/jrt test --race, sees the happens-before edges).\n"
  [^{:tag (* testing/T)} t]
  (let [o (Object_New)
        h (IdentityHash o)
        counter 0
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        before (.Load Inflated)]
    (for [g 0] (< g 8) (inc! g)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (for [i 0] (< i 200) (inc! i)
               (MonitorEnter o)
               (MonitorEnter o)
               (set! counter (+ counter 1))
               (MonitorExit o)
               (MonitorExit o))))))
    (.Wait wg)
    (when (!= counter 1600)
      (.Errorf t "counter %d" counter))
    (when (!= (IdentityHash o) h)
      (.Error t "identity hash lost through inflation"))
    (.Logf t "inflations: %d" (- (.Load Inflated) before))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))

(go/func TestWaitNotify [^{:tag (* testing/T)} t]
  (let [o (Object_New)
        ^{:tag (slice int)} queue nil
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        ^atomic/Int64 consumed (zero atomic/Int64)]
    ;; consumers wait until the queue has an item
    (for [c 0] (< c 4) (inc! c)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (for [k 0] (< k 25) (inc! k)
               (MonitorEnter o)
               (while (== (len queue) 0)
                 (Wait o))
               (.Add consumed (conv int64 (aget queue 0)))
               (set! queue (subslice queue 1))
               (MonitorExit o))))))
    (for [i 1] (<= i 100) (inc! i)
      (MonitorEnter o)
      (set! queue (append queue i))
      (Notify o)
      (MonitorExit o))
    (.Wait wg)
    (when (!= (.Load consumed) 5050)
      (.Errorf t "consumed %d" (.Load consumed))))
  ;; notifyAll wakes every waiter
  (let [o (Object_New)
        ready false
        ^sync/WaitGroup wg (zero sync/WaitGroup)
        ^atomic/Int32 woken (zero atomic/Int32)]
    (for [c 0] (< c 5) (inc! c)
      (.Add wg 1)
      (go ((fn []
             (defer (.Done wg))
             (MonitorEnter o)
             (while (not ready)
               (Wait o))
             (.Add woken 1)
             (MonitorExit o)))))
    (time/Sleep (* 20 time/Millisecond))
    (MonitorEnter o)
    (set! ready true)
    (NotifyAll o)
    (MonitorExit o)
    (.Wait wg)
    (when (!= (.Load woken) 5)
      (.Errorf t "woken %d" (.Load woken))))
  ;; a timed wait returns, and the monitor is held again with its recursion count
  (let [o (Object_New)
        start (time/Now)]
    (MonitorEnter o)
    (MonitorEnter o)
    (WaitTimeout o 30 0)
    (when (< (time/Since start) (* 30 time/Millisecond))
      (.Error t "wait(30) returned early"))
    (MonitorExit o)
    (when (not (HoldsLock o))
      (.Error t "the recursion count was not restored"))
    (MonitorExit o)
    (when (!= (res (fn ^string [] (Notify (Str "x")) "x")) "!java.lang.IllegalMonitorStateException: current thread is not owner")
      (.Error t "notify without the monitor"))
    (when (!= (monitorCount) 0)
      (.Errorf t "%d monitors left inflated" (monitorCount)))))
