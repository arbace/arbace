;; The driver of Arbace's benchmark suite (bin/arbace-bench, doc/BENCHMARKS.md), run by the
;; frozen clojure/ (it is a tool, not a workload). It launches every measured JVM itself, one
;; at a time, and never measures inside its own JVM:
;;   startup  wall time of a launch (-e 1, a short REPL session on stdin), after warm-up runs
;;   load     (require LIB) timed inside a fresh JVM (test/bench/workload.clj load LIB)
;;   steady   each benchmark of test/bench/workload.clj in a fresh JVM per fork, warmed up,
;;            then sampled (workload.clj steady NAME WARMUP-MS MEASURE-MS)
;; Forks of the implementations are interleaved (round r runs every implementation once, in a
;; rotating order), so a change of machine load spreads over all of them. The load average
;; (/proc/loadavg, 1 minute) is recorded with every launch.
;;
;; Usage: java -cp ROOT clojure.main test/bench/driver.clj run CONFIG.edn OUT.edn
;;        java -cp ROOT clojure.main test/bench/driver.clj report OUT.edn
;; CONFIG.edn (written by bin/arbace-bench): {:root :impls {key {:label :cmd :env}} :steady-opts
;; :sections :forks :startup-runs :startup-warmup :load-forks :warmup-ms :measure-ms :only}
;; OUT.edn gets the raw results, appended as they come; `report` prints the Markdown tables.

(ns bench.driver
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp])
  (:import (java.lang ProcessBuilder ProcessBuilder$Redirect)
           (java.io File)))

(defn load-average []
  (Double/parseDouble (first (str/split (first (java.nio.file.Files/readAllLines (.toPath (io/file "/proc/loadavg")))) #" "))))

(defn launch
  "Run cmd (a vector) in dir with env added; stdin from the file `in` or nothing. Returns
  {:exit :ms :out}."
  [{:keys [dir env in discard]} cmd]
  (let [pb (ProcessBuilder. ^java.util.List cmd)
        _ (.directory pb (io/file dir))
        _ (doseq [[k v] env] (.put (.environment pb) k v))
        _ (when in (.redirectInput pb (io/file in)))
        _ (.redirectErrorStream pb true)
        _ (when discard (.redirectOutput pb ProcessBuilder$Redirect/DISCARD))
        t0 (System/nanoTime)
        p (.start pb)
        _ (when-not in (.close (.getOutputStream p)))
        out (if discard "" (slurp (.getInputStream p)))
        exit (.waitFor p)
        ms (/ (- (System/nanoTime) t0) 1e6)]
    {:exit exit :ms ms :out out}))

(defn with-opts
  "The command of impl with the JVM options opts added (through ARBACE_JAVA_OPTS for
  bin/arbace)."
  [{:keys [cmd env arbace]} opts]
  (if arbace
    [cmd (assoc env "ARBACE_JAVA_OPTS" (str/join " " opts))]
    [(into [(first cmd)] (concat opts (rest cmd))) env]))

(defn workload-result [out]
  (if-let [line (first (filter #(str/starts-with? % "#bench ") (str/split-lines out)))]
    (read-string (subs line 7))
    (throw (ex-info (str "no result:\n" out) {}))))

(defn append! [out-file x]
  (spit out-file (str (pr-str x) "\n") :append true))

(defn rotate [xs r] (let [n (mod r (count xs))] (concat (drop n xs) (take n xs))))

(defn log [& xs] (apply println xs) (flush))

(defn run-startup [{:keys [root impls startup startup-runs startup-warmup]} out-file]
  (doseq [[wname {:keys [args in]}] [[:e1 {:args ["-e" "1"]}]
                                     [:repl {:args [] :in (str root "/test/bench/repl-session.clj")}]]
          ikey startup]
    (let [{:keys [cmd env]} (impls ikey)
          run #(launch {:dir root :env env :in in :discard true} (into cmd args))]
      (dotimes [_ startup-warmup] (run))
      (let [rs (vec (for [_ (range startup-runs)]
                      (let [la (load-average) r (run)] (assoc r :load la))))]
        (when-let [bad (first (remove #(zero? (:exit %)) rs))]
          (throw (ex-info (str "startup failed: " ikey " " wname) bad)))
        (append! out-file {:kind :startup :impl ikey :workload wname
                           :ms (mapv :ms rs) :load (mapv :load rs)})
        (log "startup" ikey wname (format "%.0f ms" (nth (sort (map :ms rs)) (quot (count rs) 2))))))))

(defn run-workload [{:keys [root]} impl opts args]
  (let [[cmd env] (with-opts impl opts)
        la (load-average)
        r (launch {:dir root :env env} (into cmd (cons (str root "/test/bench/workload.clj") args)))]
    (when-not (zero? (:exit r)) (throw (ex-info (str "workload failed: " args "\n" (:out r)) r)))
    (let [res (workload-result (:out r))] (if (map? res) (assoc res :load la) res))))

(defn run-load [{:keys [impls loads libs load-forks skip] :as cfg} out-file]
  (doseq [r (range load-forks) lib libs ikey (rotate loads r)
          :when (not (contains? (get skip ikey) lib))]
    (let [res (run-workload cfg (impls ikey) [] ["load" lib])]
      (append! out-file (assoc res :impl ikey :fork r))
      (log "load" lib ikey r (format "%.0f ms" (:ms res))))))

(defn run-steady [{:keys [impls steady steady-opts forks warmup-ms measure-ms only] :as cfg} out-file]
  (let [names (map :name (run-workload cfg (impls (first steady)) [] ["list"]))
        names (if (seq only) (filter (set (map keyword only)) names) names)]
    (doseq [r (range forks) n names ikey (rotate steady r)]
      (let [res (run-workload cfg (impls ikey) steady-opts
                              ["steady" (name n) (str warmup-ms) (str measure-ms)])
            s (sort (:samples res))]
        (append! out-file (assoc res :impl ikey :fork r))
        (log "steady" n ikey r (format "%.4g ns" (nth s (quot (count s) 2))))))))

;; ---------------------------------------------------------------------------------------------
;; The report

(defn median [xs]
  (let [s (vec (sort xs)) n (count s)]
    (if (odd? n) (s (quot n 2)) (/ (+ (s (dec (quot n 2))) (s (quot n 2))) 2.0))))

(defn fmt-ns [ns]
  (cond (< ns 1e3) (format "%.3g ns" (double ns))
        (< ns 1e6) (format "%.3g µs" (/ ns 1e3))
        (< ns 1e9) (format "%.3g ms" (/ ns 1e6))
        :else (format "%.3g s" (/ ns 1e9))))

(defn spread
  "The spread of xs around their median m, as -a %/+b %."
  [xs m]
  (format "-%.0f/+%.0f %%" (* 100 (/ (- m (apply min xs)) m)) (* 100 (/ (- (apply max xs) m) m))))

(defn ratio [a b] (if (and a b) (format "%.2f" (double (/ a b))) "n/a"))

(defn read-results [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (doall (take-while some? (repeatedly #(read {:eof nil} r))))))

(defn report [file]
  (let [rs (read-results file)
        cfg (first (filter #(= :config (:kind %)) rs))
        label #(get-in cfg [:impls % :label] (name %))
        loads (fn [xs] (let [ls (mapcat #(if (coll? (:load %)) (:load %) [(:load %)]) xs)]
                         (if (seq ls) (format "%.0f / %.0f / %.0f" (apply min ls) (median ls) (apply max ls)) "")))]
    (println "Implementations:")
    (doseq [[k {:keys [label]}] (:impls cfg)] (println (str "- `" (name k) "`: " label)))
    (let [st (filter #(= :startup (:kind %)) rs)]
      (when (seq st)
        (println)
        (println (str "Startup: wall time of a launch, median of " (count (:ms (first st)))
                      " runs and its spread (min, max); load average min / median / max " (loads st) "."))
        (println)
        (println "| implementation | `-e 1` | spread | REPL session | spread |")
        (println "|---|---:|---:|---:|---:|")
        (doseq [ik (distinct (map :impl st))]
          (let [get-w (fn [w] (first (filter #(and (= ik (:impl %)) (= w (:workload %))) st)))
                cell (fn [w] (if-let [x (get-w w)]
                               (let [m (median (:ms x))] [(format "%.0f ms" m) (spread (:ms x) m)])
                               ["" ""]))]
            (println (str "| `" (name ik) "` | " (str/join " | " (concat (cell :e1) (cell :repl))) " |"))))))
    (let [ld (filter #(= :load (:kind %)) rs)
          impls (distinct (map :impl ld))]
      (when (seq ld)
        (println)
        (println (str "Load time: `(require LIB)` in a fresh JVM (a script), median over forks and its spread; load average " (loads ld) "."))
        (println)
        (println (str "| library | " (str/join " | " (map #(str "`" (name %) "`") impls)) " |"))
        (println (str "|---|" (str/join "|" (repeat (count impls) "---:")) "|"))
        (doseq [lib (distinct (map :name ld))]
          (println (str "| " lib " | "
                        (str/join " | " (for [ik impls]
                                          (let [ms (map :ms (filter #(and (= ik (:impl %)) (= lib (:name %))) ld))]
                                            (if (seq ms)
                                              (let [m (median ms)] (format "%.1f ms %s" m (spread ms m)))
                                              "n/a"))))
                        " |")))))
    (let [sd (filter #(= :steady (:kind %)) rs)
          impls (distinct (map :impl sd))
          [a b c] impls]
      (when (seq sd)
        (println)
        (println (str "Steady state: time of one call, the median over forks of each fork's median "
                      "sample, and the spread of the forks' medians; load average " (loads sd) "."))
        (println)
        (println (str "| benchmark | " (str/join " | " (map #(str "`" (name %) "`") impls))
                      " | " (name a) " / " (name b) (when c (str " | " (name c) " / " (name b))) " |"))
        (println (str "|---|" (str/join "|" (repeat (count impls) "---:")) "|---:" (when c "|---:") "|"))
        (doseq [n (distinct (map :name sd))]
          (let [med (fn [ik] (let [fs (map #(median (:samples %)) (filter #(and (= ik (:impl %)) (= n (:name %))) sd))]
                               (when (seq fs) [(median fs) fs])))
                ms (into {} (for [ik impls] [ik (med ik)]))]
            (println (str "| `" (name n) "` | "
                          (str/join " | " (for [ik impls]
                                            (if-let [[m fs] (ms ik)] (str (fmt-ns m) " " (spread fs m)) "n/a")))
                          " | " (ratio (first (ms a)) (first (ms b)))
                          (when c (str " | " (ratio (first (ms c)) (first (ms b)))))
                          " |"))))
        (println)
        (let [gm (fn [x y & [skip]] (let [rs (for [n (remove (set skip) (distinct (map :name sd)))
                                           :let [f (fn [ik] (median (map #(median (:samples %)) (filter #(and (= ik (:impl %)) (= n (:name %))) sd))))
                                                 p (f x) q (f y)]]
                                       (/ p q))]
                             (Math/exp (/ (reduce + (map #(Math/log %) rs)) (count rs)))))]
          (println (format "Geometric mean of the ratios: %s / %s %.2f%s" (name a) (name b) (gm a b)
                           (if c (format ", %s / %s %.2f" (name c) (name b) (gm c b)) "")))
          (let [skip [:interop-reflective :str-concat]]
            (println (format "Without %s (the two that Arbace's call sites change by 10x or more): %s / %s %.2f%s"
                             (str/join " and " (map #(str "`" (name %) "`") skip)) (name a) (name b) (gm a b skip)
                             (if c (format ", %s / %s %.2f" (name c) (name b) (gm c b skip)) "")))))
        (println)
        (println "Benchmarks:")
        (println)
        (doseq [{:keys [name desc]} (vals (into (sorted-map) (map (fn [x] [(:name x) x]) sd)))]
          (println (str "- `" (clojure.core/name name) "`: " desc)))))))

;; ---------------------------------------------------------------------------------------------

(let [[cmd a b] *command-line-args*]
  (case cmd
    "run" (let [cfg (read-string (slurp a))]
            (io/make-parents b)
            (append! b {:kind :config :impls (:impls cfg) :date (str (java.time.Instant/now))
                        :cfg (dissoc cfg :impls)})
            (when (contains? (:sections cfg) :startup) (run-startup cfg b))
            (when (contains? (:sections cfg) :load) (run-load cfg b))
            (when (contains? (:sections cfg) :steady) (run-steady cfg b))
            (log "results in" b))
    "report" (report a)))
