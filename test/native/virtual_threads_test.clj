(ns native.virtual-threads-test
  "The opt-in virtual-thread executor of send-off, future and pmap (arbace.lang.Agent): the system
  property arbace.virtual-threads=true at launch, or
  (set-agent-send-off-executor! (arbace.lang.Agent/newVirtualThreadExecutor)) at run time."
  (:require [arbace.test :refer :all]
            [arbace.string :as str])
  (:import (arbace.lang Agent)
           (java.util.concurrent ExecutorService RejectedExecutionException)))

(def ^:dynamic *b* :root)

(defn- thread-info []
  (let [t (Thread/currentThread)] [(.isVirtual t) (.getName t) (.isDaemon t)]))

(deftest default-executor
  (testing "platform threads of the cached pool, unless arbace.virtual-threads is set"
    (let [[virtual? name] @(future (thread-info))]
      (is (false? virtual?))
      (is (str/starts-with? name "arbace-agent-send-off-pool-") name))))

(deftest run-time-opt-in
  (let [old Agent/soloExecutor
        ^ExecutorService vexec (Agent/newVirtualThreadExecutor)]
    (set-agent-send-off-executor! vexec)
    (try
      (testing "future"
        (let [[virtual? name daemon?] @(future (thread-info))]
          (is (true? virtual?))
          (is (true? daemon?))
          (is (str/starts-with? name "arbace-agent-send-off-virtual-") name)))
      (testing "send-off"
        (let [a (agent nil)]
          (send-off a (fn [_] (thread-info)))
          (await a)
          (is (true? (first @a)))
          (is (str/starts-with? (second @a) "arbace-agent-send-off-virtual-"))))
      (testing "send stays on the fixed pool"
        (let [a (agent nil)]
          (send a (fn [_] (thread-info)))
          (await a)
          (is (false? (first @a)))
          (is (str/starts-with? (second @a) "arbace-agent-send-pool-"))))
      (testing "pmap, in order"
        (is (= (map inc (range 100)) (pmap inc (range 100))))
        (is (every? true? (pmap (fn [_] (.isVirtual (Thread/currentThread))) (range 20)))))
      (testing "binding conveyance"
        (binding [*b* :bound]
          (is (= :bound @(future *b*)))
          (let [a (agent nil)]
            (send-off a (fn [_] *b*))
            (await a)
            (is (= :bound @a)))))
      (testing "an action sending from a virtual thread, held until the action ends"
        (let [a (agent 0) b (agent 0)]
          (send-off a (fn [x] (send-off b inc) (inc x)))
          (await a)
          (await b)
          (is (= [1 1] [@a @b]))))
      (testing "many blocking futures at once"
        (let [fs (doall (repeatedly 2000 #(future (Thread/sleep 50) 1)))]
          (is (= 2000 (reduce + (map deref fs))))))
      (finally
        (set-agent-send-off-executor! old)
        (.shutdown vexec)))))

(defn- run-jvm [props expr]
  (let [p (-> (ProcessBuilder. ^java.util.List
                               (concat ["java"] props
                                       ["-cp" (System/getProperty "java.class.path")
                                        "arbace.lang.Main" "-e" expr]))
              (.redirectErrorStream true)
              .start)
        out (slurp (.getInputStream p))]
    [(.waitFor p) out]))

(deftest property-opt-in
  (testing "-Darbace.virtual-threads=true: send-off, future and pmap on virtual threads"
    (let [[exit out]
          (run-jvm ["-Darbace.virtual-threads=true"]
                   (str "(def ^:dynamic *b* 0)"
                        "(prn (binding [*b* 7]"
                        "  [(deref (future [(.isVirtual (Thread/currentThread)) *b*]))"
                        "   (.getName (deref (future (Thread/currentThread))))"
                        "   (let [a (agent 0)] (send-off a (fn [_] (.isVirtual (Thread/currentThread)))) (await a) @a)"
                        "   (vec (pmap inc (range 5)))]))"
                        "(shutdown-agents)"
                        "(prn (try (future 1) :accepted"
                        "       (catch java.util.concurrent.RejectedExecutionException e :rejected)))"))]
      (is (= 0 exit) out)
      (is (str/includes? out "[[true 7] \"arbace-agent-send-off-virtual-1\" true [1 2 3 4 5]]") out)
      (is (str/includes? out ":rejected") out)))
  (testing "virtual threads are daemon threads: the JVM does not wait for a pending future"
    (let [t0 (System/nanoTime)
          [exit out] (run-jvm ["-Darbace.virtual-threads=true"]
                              "(future (Thread/sleep 60000) (println :never)) (println :done)")]
      (is (= 0 exit) out)
      (is (str/includes? out ":done") out)
      (is (not (str/includes? out ":never")) out)
      (is (< (/ (- (System/nanoTime) t0) 1e9) 30.0)))))
