(ns c2g.perf
  "bin/c2g-perf: c2g's micro benchmarks (doc/go/C2G-NOTES.md, \"Phase 2D: performance\"). The
  workloads of test/c2g/bench/BnWork.clj run on this JVM (the class forms compiled by the
  class forms compiler) and in a Go program c2g translates them into (linux/amd64, built
  with bin/g2c build); each prints ns per element and a checksum, which must agree.

  bin/c2g-perf [--only a,b] [--jvm-only|--go-only] [-- C2G-OPTIONS]
  Environment: C2G_PERF_OUT (default .tmp/c2g/perf), C2G_PERF_SECS (seconds per workload,
  default 2), GOGC passed through to the Go program."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.c2g.main :as c2g]
            [arbace.c2g.out :as out]))

(def workloads
  "[name method n]: the size each call works on."
  [["vecConj" "VecConj_I__J" 100000]
   ["hashMapAssoc" "HashMapAssoc_I__J" 100000]
   ["arrayMapSmall" "ArrayMapSmall_I__J" 10000]
   ["hashing" "Hashing_I__J" 100000]
   ["numbers" "Numbers_I__J" 100000]
   ["seqWalk" "SeqWalk_I__J" 100000]
   ["vecEquiv" "VecEquiv_I__J" 100]
   ["strings" "Strings_I__J" 100000]])

(defn- secs [] (Double/parseDouble (or (System/getenv "C2G_PERF_SECS") "2")))

(defn run-jvm
  "{name [ns-per-element checksum]} on this JVM."
  [ws]
  (binding [*ns* (create-ns 'c2g.bench)]
    (refer 'arbace.core)
    (load-file "test/c2g/bench/BnWork.clj"))
  (into (array-map)
        (for [[nm _ n] ws]
          (let [f (eval (list 'fn [] (list (symbol "c2g.bench.BnWork" nm) (int n))))
                budget (long (* 1e9 (secs)))
                warm (loop [t0 (System/nanoTime) reps 0]
                       (if (< (- (System/nanoTime) t0) budget) (do (f) (recur t0 (inc reps))) reps))
                t0 (System/nanoTime)
                _ (dotimes [_ warm] (f))
                el (- (System/nanoTime) t0)
                r [(/ (double el) (* warm n)) (f)]]
            (println (format "jvm %-14s %10.2f ns/element  checksum %d" nm (first r) (second r)))
            [nm r]))))

(defn- main-forms [ws]
  ['(go/file "main.go" :imports [[fmt "fmt"] [time "time"] [os "os"] [pprof "runtime/pprof"] [jrt "arbace/jrt"] [lang "arbace/lang"]])
   (list 'go/func 'bench [(with-meta 'name {:tag 'string}) (with-meta 'f {:tag '(func [int32] [int64])}) (with-meta 'n {:tag 'int32})]
         (list 'let ['budget (list 'conv 'time/Duration (list '* (secs) 1e9)) 'start '(time/Now) 'reps 0]
               '(while (< (time/Since start) budget) (f n) (inc! reps))
               '(let [t0 (time/Now)]
                  (for [i 0] (< i reps) (inc! i) (f n))
                  (let [el (time/Since t0)]
                    (fmt/Printf "@@bench %s %.4f %d\n" name (/ (conv float64 (.Nanoseconds el)) (* (conv float64 reps) (conv float64 n))) (f n))))))
   ;; C2G_PROF=FILE: a CPU profile of the run (go tool pprof)
   (list 'go/func 'main []
         '(when (!= (os/Getenv "C2G_PROF") "")
            (let [(values f err) (os/Create (os/Getenv "C2G_PROF"))]
              (when (!= err nil) (panic err))
              (set! _ (pprof/StartCPUProfile f))))
         (list 'let ['st (list 'jrt/RunMain
                               (apply list 'fn []
                                      (for [[nm m n] ws]
                                        (list 'bench nm (symbol (str "lang/BnWork_" m)) n))))]
               '(pprof/StopCPUProfile)
               '(os/Exit st)))])

(defn- sh [dir & args]
  (let [pb (ProcessBuilder. ^java.util.List (map str args))
        _ (.redirectErrorStream pb true)
        _ (when dir (.directory pb (io/file dir)))
        p (.start pb)
        o (slurp (.getInputStream p))]
    [(.waitFor p) o]))

(defn run-go
  "{name [ns-per-element checksum]} of the translated program (linux/amd64)."
  [ws c2g-args]
  (let [out-dir (or (System/getenv "C2G_PERF_OUT") ".tmp/c2g/perf")
        mdir (str out-dir "/main")
        opts (c2g/parse-args (concat ["--out" out-dir "--input" "test/c2g/bench" "--root" "c2g.bench.BnWork"
                                      "--main" mdir] c2g-args))]
    (io/make-parents (io/file mdir "go/arbace/cmd/bench/x"))
    (spit (str mdir "/go/arbace/cmd/bench.clj")
          (str (out/form-text '(ns go.arbace.cmd.bench (:require [arbace.go :as go]))) "\n"
               (out/form-text '(go/package main :path "arbace/cmd/bench" :files ["main.go"])) "\n"
               (out/form-text '(load "bench/main")) "\n"))
    (spit (str mdir "/go/arbace/cmd/bench/main.clj")
          (str "(in-ns 'go.arbace.cmd.bench)\n" (str/join "\n" (map out/form-text (main-forms ws))) "\n"))
    (c2g/run opts)
    (let [overlay (last (str/split-lines (str/trim (second (sh nil "bin/jrt" "overlay")))))
          exe (str out-dir "/bench")
          t0 (System/nanoTime)
          [code bo] (sh nil "bin/g2c" "build" "--line-file" "--arch" "amd64" "--work" (str out-dir "/work") "-o" exe
                        "--overlay" overlay (str out-dir "/prog"))]
      (when-not (zero? code) (println bo) (throw (ex-info "c2g-perf: build failed" {})))
      (println (format "c2g-perf: built in %.1f s" (/ (- (System/nanoTime) t0) 1e9)))
      (let [[rc o] (sh nil exe)]
        (when-not (zero? rc) (println o))
        (into (array-map)
              (for [l (str/split-lines o) :when (str/starts-with? l "@@bench ")]
                (let [[_ nm ns ck] (str/split l #" ")]
                  (println (format "go  %-14s %10.2f ns/element  checksum %s" nm (Double/parseDouble ns) ck))
                  [nm [(Double/parseDouble ns) (parse-long ck)]])))))))

(defn -main [& args]
  (let [[mine c2g-args] (split-with #(not= "--" %) args)
        c2g-args (rest c2g-args)
        only (some->> (second (drop-while #(not= "--only" %) mine)) (#(str/split % #",")) set)
        ws (if only (filter #(only (first %)) workloads) workloads)
        jvm (when-not (some #{"--go-only"} mine) (run-jvm ws))
        go (when-not (some #{"--jvm-only"} mine) (run-go ws c2g-args))]
    (println)
    (println "| workload | JVM ns/element | Go ns/element | Go/JVM | checksums |")
    (println "|---|---:|---:|---:|---|")
    (doseq [[nm] ws]
      (let [[jn jc] (get jvm nm) [gn gc] (get go nm)]
        (println (format "| %s | %s | %s | %s | %s |" nm
                         (if jn (format "%.2f" jn) "-") (if gn (format "%.2f" gn) "-")
                         (if (and jn gn) (format "%.2f" (/ gn jn)) "-")
                         (cond (or (nil? jc) (nil? gc)) "-" (= jc gc) "equal" :else (str "DIFFER " jc " " gc))))))
    (shutdown-agents)
    (System/exit 0)))
