;; java.util.concurrent from Clojure: the concurrent collections and queues, the synchronizers,
;; CompletableFuture, the executors, the atomic arrays, adders and accumulators, the stamped and
;; markable references, the locks, ThreadLocalRandom (doc/go/JRT-NOTES.md, "Concurrency").
;; Deterministic: threads meet through joins, latches and timed waits (a broken implementation
;; times out instead of hanging), and only their results are printed.

(import '(java.util.concurrent ConcurrentLinkedQueue ConcurrentLinkedDeque ConcurrentSkipListMap
                               ConcurrentSkipListSet CopyOnWriteArrayList CopyOnWriteArraySet
                               LinkedBlockingDeque LinkedBlockingQueue ArrayBlockingQueue
                               PriorityBlockingQueue DelayQueue Delayed SynchronousQueue
                               LinkedTransferQueue Exchanger Phaser CountDownLatch CyclicBarrier
                               Semaphore CompletableFuture CompletionException ExecutionException
                               TimeoutException CancellationException Executors ExecutorService
                               ThreadPoolExecutor ScheduledThreadPoolExecutor TimeUnit
                               ExecutorCompletionService RejectedExecutionException Callable
                               ThreadLocalRandom ConcurrentHashMap ForkJoinPool ForkJoinTask FutureTask)
        '(java.util.concurrent.atomic AtomicIntegerArray AtomicLongArray AtomicReferenceArray
                                      LongAdder DoubleAdder LongAccumulator DoubleAccumulator
                                      AtomicStampedReference AtomicMarkableReference)
        '(java.util.concurrent.locks StampedLock LockSupport ReentrantLock ReentrantReadWriteLock)
        '(java.util.function IntUnaryOperator IntBinaryOperator LongBinaryOperator
                             DoubleBinaryOperator Supplier Function BiFunction Consumer BiConsumer
                             UnaryOperator)
        '(java.util Comparator))

(defn timed [^java.util.concurrent.Future f] (.get f 10 TimeUnit/SECONDS))

;; ----- ConcurrentLinkedQueue
(def clq (ConcurrentLinkedQueue.))
(.isEmpty clq)
(.offer clq 1)
(.add clq 2)
(.addAll clq [3 4 5])
(vec clq)
(.size clq)
(.peek clq)
(.poll clq)
(vec clq)
(.contains clq 3)
(.contains clq 9)
(.remove clq 4)
(.remove clq 9)
(vec clq)
(str clq)
(seq (.toArray clq))
(let [it (.iterator clq)] [(.next it) (.next it) (.hasNext it)])
(do (.clear clq) [(.poll clq) (.peek clq) (.size clq)])
(.offer clq nil)
(let [q (ConcurrentLinkedQueue. [1 2 3])] (.removeIf q (reify java.util.function.Predicate (test [_ x] (odd? x)))) (vec q))
(let [q (ConcurrentLinkedQueue.)
      fs (doall (for [t (range 8)] (future (dotimes [i 500] (.offer q [t i])))))]
  (run! timed fs)
  [(.size q) (count (set q)) (count (filter #(= 3 (first %)) q))])
(let [q (ConcurrentLinkedQueue. (range 4000))
      fs (doall (for [_ (range 8)] (future (loop [n 0] (if (.poll q) (recur (inc n)) n)))))]
  [(reduce + (map timed fs)) (.isEmpty q)])

;; ----- ConcurrentLinkedDeque
(def cld (ConcurrentLinkedDeque. [2 3]))
(do (.addFirst cld 1) (.addLast cld 4) (.offerFirst cld 0) (vec cld))
[(.peekFirst cld) (.peekLast cld) (.getFirst cld) (.getLast cld)]
[(.pollFirst cld) (.pollLast cld) (vec cld)]
(vec (iterator-seq (.descendingIterator cld)))
(do (.push cld 9) [(.pop cld) (vec cld)])
(do (.addLast cld 2) [(.removeFirstOccurrence cld 2) (vec cld) (.removeLastOccurrence cld 2) (vec cld)])
(.size cld)
(vec (.reversed cld))
(do (.clear cld) [(.pollFirst cld) (.peekLast cld)])
(.getFirst (ConcurrentLinkedDeque.))
(let [d (ConcurrentLinkedDeque.)
      fs (doall (for [t (range 4)] (future (dotimes [i 250] (if (even? t) (.addFirst d i) (.addLast d i))))))]
  (run! timed fs)
  [(.size d) (reduce + d)])

;; ----- ConcurrentSkipListMap
(def cslm (ConcurrentSkipListMap.))
(do (doseq [k [5 3 9 1 7]] (.put cslm k (str "v" k))) (into [] cslm))
(.get cslm 3)
(.get cslm 4)
[(.firstKey cslm) (.lastKey cslm)]
[(.ceilingKey cslm 4) (.floorKey cslm 4) (.higherKey cslm 9) (.lowerKey cslm 1)]
(str (.ceilingEntry cslm 6))
(into [] (.headMap cslm 5))
(into [] (.headMap cslm 5 true))
(into [] (.tailMap cslm 5))
(into [] (.subMap cslm 3 8))
(into [] (.descendingMap cslm))
(vec (.keySet cslm))
(vec (.descendingKeySet cslm))
(vec (.values cslm))
(.putIfAbsent cslm 3 "x")
(.putIfAbsent cslm 4 "v4")
(.replace cslm 4 "four")
(.remove cslm 4 "v4")
(.remove cslm 4 "four")
(.merge cslm 1 "!" (reify BiFunction (apply [_ a b] (str a b))))
(.compute cslm 2 (reify BiFunction (apply [_ k v] (str "k" k v))))
(str (.pollFirstEntry cslm))
(str (.pollLastEntry cslm))
(str cslm)
(.size cslm)
(.containsValue cslm "v5")
(let [m (ConcurrentSkipListMap. (Comparator/reverseOrder))] (doseq [k "skiplist"] (.put m k 1)) (apply str (keys m)))
(let [m (ConcurrentSkipListMap. {"b" 2 "a" 1 "c" 3})] [(.firstKey m) (into {} m) (= m {"a" 1 "b" 2 "c" 3})])
(.put cslm nil 1)
(let [m (ConcurrentSkipListMap.)
      fs (doall (for [t (range 8)] (future (dotimes [i 300] (.merge m (mod (+ i t) 100) 1 (reify BiFunction (apply [_ a b] (+ a b))))))))]
  (run! timed fs)
  [(.size m) (reduce + (vals m)) (.firstKey m) (.lastKey m)])

;; ----- ConcurrentSkipListSet
(def csls (ConcurrentSkipListSet. [5 1 4 2]))
(vec csls)
[(.first csls) (.last csls) (.ceiling csls 3) (.floor csls 3) (.higher csls 5) (.lower csls 1)]
(vec (.headSet csls 4))
(vec (.tailSet csls 2 false))
(vec (.descendingSet csls))
[(.add csls 3) (.add csls 3) (.remove csls 1) (vec csls)]
[(.pollFirst csls) (.pollLast csls) (vec csls)]
(let [s (ConcurrentSkipListSet. (Comparator/reverseOrder))] (.addAll s ["b" "c" "a"]) (vec s))

;; ----- CopyOnWriteArrayList and CopyOnWriteArraySet
(def cow (CopyOnWriteArrayList. [1 2 3]))
(do (.add cow 4) (.add cow 0 0) (vec cow))
[(.get cow 2) (.indexOf cow 3) (.lastIndexOf cow 9) (.contains cow 4)]
(.set cow 1 10)
(vec cow)
(let [it (.iterator cow)] (.add cow 99) [(vec (iterator-seq it)) (vec cow)])
[(.addIfAbsent cow 99) (.addIfAbsent cow 100) (vec cow)]
(.remove cow (int 0))
(.remove cow (Integer/valueOf 99))
(vec (.subList cow 1 3))
(str cow)
(.get cow 42)
(let [it (.iterator cow)] (.next it) (.remove it))
(do (.replaceAll cow (reify UnaryOperator (apply [_ x] (* 2 x)))) (vec cow))
(do (.sort cow nil) (vec cow))
(= cow [4 6 20 200])
(let [s (CopyOnWriteArraySet. [3 1 3 2 1])] [(vec s) (.add s 1) (.add s 4) (vec s) (.size s)])
(let [l (CopyOnWriteArrayList.)
      fs (doall (for [t (range 4)] (future (dotimes [i 100] (.add l i)))))]
  (run! timed fs)
  [(.size l) (reduce + l)])

;; ----- blocking queues and deques
(def lbd (LinkedBlockingDeque. 3))
[(.offerFirst lbd 2) (.offerLast lbd 3) (.offerFirst lbd 1) (.offerLast lbd 4) (vec lbd)]
(.remainingCapacity lbd)
[(.takeFirst lbd) (.pollLast lbd) (.pollLast lbd 10 TimeUnit/MILLISECONDS) (.pollFirst lbd 10 TimeUnit/MILLISECONDS)]
(let [d (LinkedBlockingDeque.) out (java.util.ArrayList.)] (.addAll d [1 2 3 4 5]) [(.drainTo d out 3) (vec out) (vec d)])
(let [d (LinkedBlockingDeque. 1) f (future (.takeLast d))] (.putFirst d :x) (timed f))
(let [q (LinkedBlockingQueue.) f (future (.take q))] (.put q 42) [(timed f) (.size q)])
(let [q (ArrayBlockingQueue. 2 true [1 2])] [(.offer q 3) (.offer q 3 5 TimeUnit/MILLISECONDS) (.poll q) (.offer q 3) (vec q)])
(def pbq (PriorityBlockingQueue. 11 (Comparator/reverseOrder)))
(do (.addAll pbq [3 9 1 7 5]) [(.peek pbq) (.size pbq)])
(loop [acc []] (if-let [x (.poll pbq)] (recur (conj acc x)) acc))
(let [q (PriorityBlockingQueue. [5 2 8 1]) out (java.util.ArrayList.)] [(.take q) (.drainTo q out) (vec out)])
(let [q (PriorityBlockingQueue.) f (future (.take q))] (.put q :late) (timed f))
(.offer (PriorityBlockingQueue.) nil)
(defn delayed [ms tag]
  (let [t (+ (System/nanoTime) (* ms 1000000))]
    (reify Delayed
      (getDelay [_ unit] (.convert unit (- t (System/nanoTime)) TimeUnit/NANOSECONDS))
      (compareTo [this o] (compare (.getDelay this TimeUnit/NANOSECONDS) (.getDelay ^Delayed o TimeUnit/NANOSECONDS)))
      (toString [_] (str tag)))))
(let [q (DelayQueue.)] (.add q (delayed -20 :b)) (.add q (delayed -30 :a)) (.add q (delayed 60000 :later)) [(str (.poll q)) (str (.poll q)) (.poll q) (.size q)])
(let [q (DelayQueue.)] (.put q (delayed 30 :soon)) (str (.take q)))
(let [q (DelayQueue.)] (.put q (delayed 60000 :far)) [(.poll q 20 TimeUnit/MILLISECONDS) (.size q) (some? (.peek q))])

;; ----- SynchronousQueue and LinkedTransferQueue
(let [q (SynchronousQueue.)] [(.offer q 1) (.poll q) (.size q) (.isEmpty q) (.peek q)])
(let [q (SynchronousQueue.) f (future (.take q))] (.put q :handed) (timed f))
(let [q (SynchronousQueue. true) f (future (.poll q 10 TimeUnit/SECONDS))] [(.offer q :fair 10 TimeUnit/SECONDS) (timed f)])
(let [q (SynchronousQueue.) f (future (.put q :from-future))] [(.take q) (timed f)])
(let [q (LinkedTransferQueue.)] [(.tryTransfer q 1) (.offer q 2) (.offer q 3) (vec q) (.poll q) (.hasWaitingConsumer q)])
(let [q (LinkedTransferQueue.) f (future (.take q))] (.transfer q :transferred) (timed f))
(let [q (LinkedTransferQueue.) f (future (.tryTransfer q :t 10 TimeUnit/SECONDS))] [(.take q) (timed f)])
(let [q (LinkedTransferQueue. [1 2 3])] [(.size q) (.remainingCapacity q) (vec q) (str q)])
(let [q (LinkedTransferQueue.)
      prods (doall (for [t (range 4)] (future (dotimes [i 200] (.put q i)))))
      cons (doall (for [_ (range 4)] (future (loop [s 0 n 0] (if (< n 200) (recur (+ s (.take q)) (inc n)) s)))))]
  (run! timed prods)
  [(reduce + (map timed cons)) (.isEmpty q)])

;; ----- Exchanger, Phaser, CountDownLatch, CyclicBarrier, Semaphore
(let [x (Exchanger.) f (future (.exchange x :from-future))] [(.exchange x :from-main) (timed f)])
(let [x (Exchanger.)] (.exchange x :nobody 10 TimeUnit/MILLISECONDS))
(let [x (Exchanger.)
      fs (doall (for [i (range 6)] (future (.exchange x i 10 TimeUnit/SECONDS))))]
  (sort (map timed fs)))
(let [p (Phaser. 1)] [(.getPhase p) (.getRegisteredParties p) (.arrive p) (.getPhase p) (.register p) (.getRegisteredParties p) (.arriveAndDeregister p) (.getRegisteredParties p)])
(let [p (Phaser. 3)
      fs (doall (for [i (range 2)] (future (.arriveAndAwaitAdvance p) (.arriveAndAwaitAdvance p) i)))]
  (.arriveAndAwaitAdvance p)
  (.arriveAndAwaitAdvance p)
  [(mapv timed fs) (.getPhase p)])
(let [p (Phaser. 2)] (.arrive p) [(.awaitAdvanceInterruptibly p 0 10 TimeUnit/MILLISECONDS)])
(let [p (Phaser.)] (.forceTermination p) [(.isTerminated p) (.register p)])
(str (Phaser. 2))
(let [l (CountDownLatch. 3) fs (doall (for [_ (range 3)] (future (.countDown l))))] [(.await l 10 TimeUnit/SECONDS) (.getCount l)])
(let [b (CyclicBarrier. 3) fs (doall (for [_ (range 2)] (future (.await b 10 TimeUnit/SECONDS))))] [(.await b 10 TimeUnit/SECONDS) (sort (map timed fs))])
(let [s (Semaphore. 2)] [(.tryAcquire s) (.tryAcquire s) (.tryAcquire s) (.availablePermits s) (do (.release s 2) (.availablePermits s))])

;; ----- CompletableFuture
(.join (CompletableFuture/supplyAsync (reify Supplier (get [_] (+ 1 2)))))
(-> (CompletableFuture/supplyAsync (reify Supplier (get [_] 20)))
    (.thenApply (reify Function (apply [_ x] (* x 2))))
    (.thenCombine (CompletableFuture/completedFuture 2) (reify BiFunction (apply [_ a b] (+ a b))))
    (.get 10 TimeUnit/SECONDS))
(-> (CompletableFuture/completedFuture 5)
    (.thenCompose (reify Function (apply [_ x] (CompletableFuture/supplyAsync (reify Supplier (get [_] (inc x)))))))
    .join)
(let [cf (CompletableFuture.)] [(.isDone cf) (.getNow cf :absent) (.complete cf :v) (.complete cf :w) (.join cf) (.isDone cf)])
(let [cf (CompletableFuture.)] (.completeExceptionally cf (IllegalStateException. "boom")) [(.isCompletedExceptionally cf) (try (.get cf) (catch ExecutionException e [(class (.getCause e)) (.getMessage (.getCause e))]))])
(let [cf (CompletableFuture.)] (.completeExceptionally cf (IllegalStateException. "boom")) (.join cf))
(-> (CompletableFuture/supplyAsync (reify Supplier (get [_] (throw (ArithmeticException. "div")))))
    (.exceptionally (reify Function (apply [_ e] [:recovered (class e) (class (.getCause ^Throwable e))])))
    .join)
(-> (CompletableFuture/completedFuture 1)
    (.handle (reify BiFunction (apply [_ v e] [:handled v e])))
    .join)
(let [seen (atom nil)] (-> (CompletableFuture/completedFuture :x) (.whenComplete (reify BiConsumer (accept [_ v e] (reset! seen [v e])))) .join) @seen)
(let [fs (mapv #(CompletableFuture/supplyAsync (reify Supplier (get [_] (* % %)))) (range 6))]
  [(.join (CompletableFuture/allOf (into-array CompletableFuture fs))) (mapv #(.join ^CompletableFuture %) fs)])
(.join (CompletableFuture/anyOf (into-array CompletableFuture [(CompletableFuture/completedFuture :first) (CompletableFuture.)])))
(let [cf (CompletableFuture.)] [(.cancel cf true) (.isCancelled cf) (.isCompletedExceptionally cf) (try (.join cf) (catch CancellationException e :cancelled))])
(try (.get (.orTimeout (CompletableFuture.) 20 TimeUnit/MILLISECONDS)) (catch ExecutionException e (class (.getCause e))))
(.get (.completeOnTimeout (CompletableFuture.) :default 20 TimeUnit/MILLISECONDS) 10 TimeUnit/SECONDS)
(.get (.completeOnTimeout (CompletableFuture/completedFuture :early) :default 20 TimeUnit/MILLISECONDS) 10 TimeUnit/SECONDS)
(let [t0 (System/nanoTime) cf (CompletableFuture/supplyAsync (reify Supplier (get [_] :delayed)) (CompletableFuture/delayedExecutor 30 TimeUnit/MILLISECONDS))]
  [(.get cf 10 TimeUnit/SECONDS) (>= (- (System/nanoTime) t0) 30000000)])
(let [ex (Executors/newSingleThreadExecutor) cf (CompletableFuture/supplyAsync (reify Supplier (get [_] :on-executor)) ex)] (try (.join cf) (finally (.shutdown ex))))
(-> (CompletableFuture/completedFuture 3) (.thenAcceptBoth (CompletableFuture/completedFuture 4) (reify BiConsumer (accept [_ a b]))) .join)
(-> (CompletableFuture/completedFuture 3) (.applyToEither (CompletableFuture.) (reify Function (apply [_ x] (inc x)))) .join)
(.join (.thenApplyAsync (CompletableFuture/completedFuture 1) (reify Function (apply [_ x] (+ x 100)))))
(let [cf (CompletableFuture.)] (.obtrudeValue cf 7) [(.join cf) (.getNumberOfDependents cf)])
(.join (.toCompletableFuture (.minimalCompletionStage (CompletableFuture/completedFuture :minimal))))
(.join (CompletableFuture/failedFuture (RuntimeException. "failed")))
(str (CompletableFuture/completedFuture 1))
(str (CompletableFuture.))
(.resultNow (CompletableFuture/completedFuture :now))
(.join (.exceptionallyCompose (CompletableFuture/failedFuture (RuntimeException. "x")) (reify Function (apply [_ e] (CompletableFuture/completedFuture :composed)))))
(let [cf (CompletableFuture.) f (future (.get cf 10 TimeUnit/SECONDS))] (Thread/sleep 20) (.complete cf :waited) (timed f))
(reduce + (map #(.join ^CompletableFuture %) (mapv (fn [i] (CompletableFuture/supplyAsync (reify Supplier (get [_] i)))) (range 100))))

;; ----- executors
(def pool (Executors/newFixedThreadPool 3))
(.get (.submit ^ExecutorService pool ^Callable (fn [] :called)) 10 TimeUnit/SECONDS)
(.get (.submit ^ExecutorService pool ^Runnable (fn [] :ran)) 10 TimeUnit/SECONDS)
(mapv #(.get ^java.util.concurrent.Future % 10 TimeUnit/SECONDS) (.invokeAll ^ExecutorService pool (mapv (fn [i] ^Callable (fn [] (* i 10))) (range 5))))
(.invokeAny ^ExecutorService pool [^Callable (fn [] :only)])
(let [f (.submit ^ExecutorService pool ^Callable (fn [] (throw (IllegalArgumentException. "bad"))))] (try (.get f) (catch ExecutionException e [(class (.getCause e)) (.getMessage (.getCause e))])))
[(.getCorePoolSize ^ThreadPoolExecutor pool) (.getMaximumPoolSize ^ThreadPoolExecutor pool) (.isShutdown pool)]
(let [l (CountDownLatch. 1) f (.submit ^ExecutorService pool ^Callable (fn [] (.await l) :released))] [(.isDone f) (do (.countDown l) (.get f 10 TimeUnit/SECONDS))])
(let [l (CountDownLatch. 1) f (.submit ^ExecutorService pool ^Callable (fn [] (try (.await l) :no (catch InterruptedException _ :interrupted))))] (Thread/sleep 20) [(.cancel f true) (.isCancelled f) (.isDone f)])
(.shutdown pool)
(.awaitTermination pool 10 TimeUnit/SECONDS)
[(.isShutdown pool) (.isTerminated pool)]
(try (.execute pool (fn [])) (catch RejectedExecutionException e (class e)))
(let [p (Executors/newCachedThreadPool) fs (mapv (fn [i] (.submit p ^Callable (fn [] (inc i)))) (range 20))] (try (reduce + (map timed fs)) (finally (.shutdown p))))
(let [p (Executors/newSingleThreadExecutor) order (atom [])] (dotimes [i 5] (.execute p (fn [] (swap! order conj i)))) (.shutdown p) [(.awaitTermination p 10 TimeUnit/SECONDS) @order])
(let [p (ThreadPoolExecutor. 1 1 0 TimeUnit/MILLISECONDS (LinkedBlockingQueue.)) l (CountDownLatch. 1)]
  (.execute p (fn [] (.await l)))
  (.execute p (fn []))
  (.execute p (fn []))
  (Thread/sleep 20)
  (let [pending (.shutdownNow p)] (.countDown l) [(count pending) (.awaitTermination p 10 TimeUnit/SECONDS) (.getCompletedTaskCount p)]))
(let [p (ThreadPoolExecutor. 1 1 0 TimeUnit/MILLISECONDS (ArrayBlockingQueue. 1)) l (CountDownLatch. 1)]
  (.execute p (fn [] (.await l)))
  (.execute p (fn []))
  (let [r (try (.execute p (fn [])) :accepted (catch RejectedExecutionException _ :rejected))]
    (.countDown l) (.shutdown p) [r (.awaitTermination p 10 TimeUnit/SECONDS)]))
(let [p (ThreadPoolExecutor. 2 4 1 TimeUnit/SECONDS (LinkedBlockingQueue.))] (.allowCoreThreadTimeOut p true) [(.allowsCoreThreadTimeOut p) (.getKeepAliveTime p TimeUnit/MILLISECONDS) (do (.shutdown p) (.awaitTermination p 10 TimeUnit/SECONDS))])
(let [p (Executors/newVirtualThreadPerTaskExecutor) fs (mapv (fn [i] (.submit p ^Callable (fn [] (* i i)))) (range 10))] (try (reduce + (map timed fs)) (finally (.close p))))
(let [p (Executors/newThreadPerTaskExecutor (Executors/defaultThreadFactory))] (.close p) [(.isShutdown p) (.isTerminated p)])
(let [p (Executors/newFixedThreadPool 2) ecs (ExecutorCompletionService. p)]
  (doseq [i (range 4)] (.submit ecs ^Callable (fn [] i)))
  (try (sort (repeatedly 4 #(.get (.poll ecs 10 TimeUnit/SECONDS)))) (finally (.shutdown p))))
(let [ft (FutureTask. ^Callable (fn [] :task))] (.run ft) [(.isDone ft) (.get ft) (.resultNow ft) (str (.state ft))])
(let [ft (FutureTask. ^Callable (fn [] :task))] [(.cancel ft false) (.isCancelled ft) (try (.get ft) (catch CancellationException _ :cancelled))])
(let [t (.newThread (Executors/defaultThreadFactory) (fn []))] [(.isDaemon t) (.getPriority t) (boolean (re-matches #"pool-\d+-thread-1" (.getName t)))])
(.call (Executors/callable (fn [] :ignored) :result))

;; ----- scheduled executors
(def sched (Executors/newScheduledThreadPool 2))
(let [t0 (System/nanoTime) f (.schedule sched ^Callable (fn [] :scheduled) 30 TimeUnit/MILLISECONDS)] [(.get f 10 TimeUnit/SECONDS) (>= (- (System/nanoTime) t0) 30000000)])
(let [n (atom 0) l (CountDownLatch. 3) f (.scheduleAtFixedRate sched (fn [] (swap! n inc) (.countDown l)) 0 5 TimeUnit/MILLISECONDS)]
  [(.await l 10 TimeUnit/SECONDS) (.cancel f false) (.isCancelled f) (>= @n 3)])
(let [n (atom 0) l (CountDownLatch. 2) f (.scheduleWithFixedDelay sched (fn [] (swap! n inc) (.countDown l)) 1 5 TimeUnit/MILLISECONDS)]
  [(.await l 10 TimeUnit/SECONDS) (.cancel f true) (.isDone f)])
(let [f (.schedule sched ^Callable (fn [] :never) 1 TimeUnit/HOURS)] [(pos? (.getDelay f TimeUnit/SECONDS)) (.cancel f false) (.isCancelled f)])
(do (.shutdown sched) [(.awaitTermination sched 10 TimeUnit/SECONDS) (.isTerminated sched)])
(let [s (Executors/newSingleThreadScheduledExecutor)] (try (.get (.schedule s ^Callable (fn [] :single) 1 TimeUnit/MILLISECONDS) 10 TimeUnit/SECONDS) (finally (.shutdownNow s))))

;; ----- atomic arrays
(def aia (AtomicIntegerArray. 4))
[(.incrementAndGet aia 0) (.getAndAdd aia 1 5) (.addAndGet aia 1 5) (.compareAndSet aia 2 0 7) (.compareAndSet aia 2 0 8)]
(str aia)
[(.getAndUpdate aia 3 (reify IntUnaryOperator (applyAsInt [_ x] (+ x 3)))) (.accumulateAndGet aia 3 10 (reify IntBinaryOperator (applyAsInt [_ a b] (* a b))))]
[(.length aia) (.get aia 3) (.getAndSet aia 3 -1) (.getPlain aia 3) (.getAcquire aia 3) (.compareAndExchange aia 3 -1 4)]
(.get aia 4)
(let [a (AtomicIntegerArray. (int-array [1 2 3]))
      fs (doall (for [_ (range 8)] (future (dotimes [i 1000] (.incrementAndGet a (mod i 3))))))]
  (run! timed fs)
  (str a))
(def ala (AtomicLongArray. (long-array [10 20])))
[(.decrementAndGet ala 0) (.getAndIncrement ala 1) (.weakCompareAndSetVolatile ala 1 21 30) (str ala)]
(def ara (AtomicReferenceArray. (object-array [:a :b nil])))
[(.get ara 0) (.compareAndSet ara 2 nil :c) (.getAndSet ara 0 :z) (.updateAndGet ara 1 (reify UnaryOperator (apply [_ x] [x x]))) (str ara)]
(let [a (AtomicReferenceArray. 2)
      fs (doall (for [_ (range 8)] (future (dotimes [_ 500] (loop [] (let [v (.get a 0)] (when-not (.compareAndSet a 0 v (conj (or v []) 1)) (recur))))))))]
  (run! timed fs)
  (count (.get a 0)))
(.set (AtomicReferenceArray. (into-array String ["s"])) 0 :not-a-string)

;; ----- adders and accumulators
(let [a (LongAdder.) fs (doall (for [_ (range 8)] (future (dotimes [_ 5000] (.increment a)))))] (run! timed fs) [(.sum a) (.intValue a) (str a)])
(let [a (LongAdder.)] (.add a 5) (.decrement a) [(.sumThenReset a) (.sum a)])
(let [a (DoubleAdder.) fs (doall (for [_ (range 4)] (future (dotimes [_ 1000] (.add a 0.5)))))] (run! timed fs) [(.sum a) (.longValue a)])
(let [a (LongAccumulator. (reify LongBinaryOperator (applyAsLong [_ x y] (max x y))) Long/MIN_VALUE)
      fs (doall (for [t (range 8)] (future (dotimes [i 1000] (.accumulate a (+ (* t 1000) i))))))]
  (run! timed fs)
  [(.get a) (.getThenReset a) (.get a)])
(let [a (DoubleAccumulator. (reify DoubleBinaryOperator (applyAsDouble [_ x y] (+ x y))) 1.5)] (.accumulate a 2.0) [(.get a) (str a)])

;; ----- stamped and markable references
(def asr (AtomicStampedReference. :v0 0))
(let [h (int-array 1)] [(.get asr h) (aget h 0)])
[(.compareAndSet asr :v0 :v1 0 1) (.compareAndSet asr :v1 :v2 0 2) (.getReference asr) (.getStamp asr)]
[(.attemptStamp asr :v1 7) (.getStamp asr)]
(def amr (AtomicMarkableReference. "r" false))
[(.compareAndSet amr "r" "s" false true) (.isMarked amr) (.getReference amr) (.attemptMark amr "s" false) (.isMarked amr)]

;; ----- locks
(def sl (StampedLock.))
(let [s (.writeLock sl)] [(.isWriteLocked sl) (.validate sl (.tryOptimisticRead sl)) (do (.unlockWrite sl s) (.isWriteLocked sl))])
(let [o (.tryOptimisticRead sl)] [(pos? o) (.validate sl o)])
(let [r1 (.readLock sl) r2 (.tryReadLock sl)] [(.getReadLockCount sl) (.isReadLocked sl) (do (.unlockRead sl r1) (.unlockRead sl r2) (.getReadLockCount sl))])
(let [r (.readLock sl) w (.tryConvertToWriteLock sl r)] [(pos? w) (.isWriteLocked sl) (do (.unlock sl w) (.isWriteLocked sl))])
(let [l (.asWriteLock sl)] (.lock l) (let [t (.tryLock (.asReadLock sl))] (.unlock l) [t (.isWriteLocked sl)]))
(.unlockWrite sl 12345)
(let [n (long-array 1) fs (doall (for [_ (range 8)] (future (dotimes [_ 500] (let [s (.writeLock sl)] (aset n 0 (inc (aget n 0))) (.unlockWrite sl s))))))] (run! timed fs) (aget n 0))
(do (LockSupport/unpark (Thread/currentThread)) (LockSupport/park) :parked-with-permit)
(LockSupport/parkNanos 1000)
(LockSupport/getBlocker (Thread/currentThread))
(let [t (Thread/currentThread) f (future (Thread/sleep 20) (LockSupport/unpark t))] (LockSupport/park :blocker) (timed f) :unparked)
(let [l (ReentrantReadWriteLock.)] (.lock (.readLock l)) (.lock (.readLock l)) [(.getReadLockCount l) (.isWriteLocked l) (do (.unlock (.readLock l)) (.unlock (.readLock l)) (.getReadLockCount l))])

;; ----- ThreadLocalRandom
(instance? java.util.Random (ThreadLocalRandom/current))
(identical? (ThreadLocalRandom/current) (ThreadLocalRandom/current))
(.nextInt (ThreadLocalRandom/current) 5 6)
(.nextLong (ThreadLocalRandom/current) 1)
(let [r (ThreadLocalRandom/current)] (every? #(< -1 % 10) (repeatedly 200 #(.nextInt r 10))))
(let [r (ThreadLocalRandom/current)] (every? #(<= 0.0 % 1.0) (repeatedly 200 #(.nextDouble r))))
(count (distinct (repeatedly 100 #(.nextLong (ThreadLocalRandom/current)))))
(.setSeed (ThreadLocalRandom/current) 42)
(.nextInt (ThreadLocalRandom/current) 0)
(count (.toArray (.ints (ThreadLocalRandom/current) 5 0 3)))

;; ----- ConcurrentHashMap and ForkJoinPool from many threads
(let [m (ConcurrentHashMap.) fs (doall (for [t (range 8)] (future (dotimes [i 1000] (.merge m (mod i 50) 1 (reify BiFunction (apply [_ a b] (+ a b))))))))] (run! timed fs) [(.size m) (reduce + (vals m)) (.mappingCount m)])
(.invoke (ForkJoinPool/commonPool) (ForkJoinTask/adapt ^Callable (fn [] :fj)))
(.get (CompletableFuture/supplyAsync (reify Supplier (get [_] (.getParallelism (ForkJoinPool/commonPool))))) 10 TimeUnit/SECONDS)
