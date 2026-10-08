(ns arbace.g2c.main
  "Command line of g2c's converter (doc/go/CONVERTER-NOTES.md).

  bin/g2c convert ... runs
    -m arbace.g2c.main convert [--out DIR] [--full] [--jobs N] [--goroot DIR] [--no-check]
       [--report FILE] DUMP|DIR...
  Converts each helper dump (DIRs: every .edn below them) to Go forms under OUT (default
  .tmp/g2c/forms): go/<path>.clj and go/<path>/<file>.clj per package (doc/go/SPEC.md §4.1).
  A single-file program (path command-line-arguments) gets the namespace go.test. plus its
  dump's path relative to the DIR given. Unless --no-check, every file written is read back
  with Arbace's reader and compared with the forms in memory, metadata and the lines of lists
  included. Prints per dump a line, then totals and the coverage per node kind; --report
  writes them as data."
  (:require [arbace.g2c.convert :as cv]
            [arbace.g2c.layout :as layout]
            [arbace.g2c.lit :as lit]
            [arbace.g2c.read :as rd]
            [arbace.g2c.types :as ty]
            [arbace.string :as str])
  (:import [java.io File StringReader]
           [java.util.concurrent Executors TimeUnit]))

;;; Read-back check

(defn data-of
  "The data an in-memory form reads back as (without metadata)."
  [x]
  (cond
    (lit/lit? x) (:value x)
    (and (seq? x) (:arbace.g2c.convert/raw (meta x))) (list 'in-ns (list 'quote (second (second x))))
    (and (seq? x) (:arbace.g2c.convert/deref (meta x))) (list 'arbace.core/deref (data-of (second x)))
    (seq? x) (apply list (map data-of x))
    (vector? x) (mapv data-of x)
    (map? x) (into {} (map (fn [[k v]] [(data-of k) (data-of v)])) x)
    :else x))

(defn- expected-meta
  "The metadata x reads back with, as data, without :line/:column (checked apart)."
  [x]
  (let [m (layout/public-meta x)]
    (into {} (map (fn [[k v]] [k (data-of v)])) (dissoc m :line :column))))

(defn- read-meta [x]
  (when (instance? arbace.lang.IObj x)
    (dissoc (meta x) :line :column)))

(defn diff
  "The first difference between the in-memory form x (prepared) and the form r read back, or
  nil: [path what expected actual]."
  [x r path]
  (let [dx (data-of x)]
    (cond
      (not= dx r) [path :data dx r]
      :else
      (or (let [em (expected-meta x)
                rm (or (read-meta r) {})]
            (when (and (or (seq em) (seq rm)) (not= (data-of em) (data-of rm)))
              [path :meta em rm]))
          (when (and (seq? x) (= layout/*mode* :lines) (not (:arbace.g2c.convert/raw (meta x))))
            (let [want (or (:line (meta x)) (:arbace.g2c.convert/l (meta x)))
                  got (:line (meta r))]
              (when (and want got (not= want got))
                [path :line want got])))
          (when (and (seq? x) (:arbace.g2c.convert/deref (meta x)))
            (diff (second x) (second r) (conj path 1)))
          (when (and (or (seq? x) (vector? x)) (not (:arbace.g2c.convert/deref (meta x))))
            (first (keep identity (map-indexed (fn [i [a b]] (diff a b (conj path i)))
                                               (map vector x r)))))))))

(defn read-all [^String text]
  (binding [*read-eval* false]
    (let [r (arbace.lang.LineNumberingPushbackReader. (StringReader. text))
          eof (Object.)]
      (loop [acc []]
        (let [f (read r false eof)]
          (if (identical? f eof) acc (recur (conj acc f))))))))

(defn check-text
  "nil when text reads back as the prepared forms, else a message."
  [text prepared]
  (try
    (let [rs (read-all text)]
      (if (not= (count rs) (count prepared))
        (str "read " (count rs) " forms, wrote " (count prepared))
        (some (fn [[i x r]] (when-let [d (diff x r [i])] (pr-str d)))
              (map vector (range) prepared rs))))
    (catch Throwable e (str "read error: " (.getMessage e)))))

;;; Conversion of dumps

(defn- munge-seg [^String s] (.replaceAll s "[.-]" "_"))

(defn program-ns
  "The namespace of a single-file program from its dump's relative path (x/y.go.edn)."
  [^String rel]
  (let [p (str/replace rel #"\.edn$" "")
        p (str/replace p #"\.go$" "")]
    (symbol (str "go.test." (str/join "." (map munge-seg (str/split p #"/")))))))

(defn ns-dir [ns-sym]
  (str/join "/" (str/split (str ns-sym) #"\.")))

(defn- spit-file [^File f ^String text]
  (.mkdirs (.getParentFile f))
  (spit f text))

(defn convert-dump
  "Converts the dump at path into out; returns a result map."
  [path rel {:keys [out positions goroot check]}]
  (let [t0 (System/nanoTime)]
    (try
      (let [d (rd/read-dump path)
            t1 (System/nanoTime)
            program? (= "command-line-arguments" (:package d))
            ns-sym (if program? (program-ns rel) (ty/ns-name-of (:package d)))
            src-dir (when (and goroot (not program?))
                      (str/replace (:dir d) "$GOROOT" goroot))
            r (cv/convert-package d {:positions positions :ns ns-sym :src-dir src-dir})
            dir (ns-dir ns-sym)
            last-seg (last (str/split dir #"/"))
            layout-stats (atom {})
            files (binding [layout/*mode* positions
                            layout/*stats* layout-stats]
                    (vec (for [fl (:files r)]
                           (let [text (layout/write-file (:forms fl) (:comments fl))
                                 bad (when check
                                       (check-text text (layout/prepare (:forms fl))))]
                             (assoc fl :text text :bad bad)))))
            pkg-text (layout/write-package ns-sym (:package r)
                                           (map #(str last-seg "/" (:stem %)) files))
            pkg-bad (when check
                      (try (read-all pkg-text) nil
                           (catch Throwable e (str "read error: " (.getMessage e)))))
            bytes (atom (count (.getBytes pkg-text "UTF-8")))]
        (spit-file (File. (str out "/" dir ".clj")) pkg-text)
        (doseq [fl files]
          (swap! bytes + (count (.getBytes ^String (:text fl) "UTF-8")))
          (spit-file (File. (str out "/" dir "/" (:stem fl) ".clj")) (:text fl)))
        (let [dump-nodes (frequencies (map first (mapcat (comp rd/nodes :ast) (:files d))))]
          {:path path :package (:package d) :ns ns-sym :files (count files)
           :bytes @bytes :ms (/ (- (System/nanoTime) t0) 1e6) :read-ms (/ (- t1 t0) 1e6)
           :stats (:stats r) :layout @layout-stats :dump-nodes dump-nodes
           :unplaced (vec (for [fl files u (:unplaced fl)] [(:name fl) u]))
           :bad (vec (concat (keep (fn [fl] (when (:bad fl) [(:name fl) (:bad fl)])) files)
                             (when pkg-bad [["package" pkg-bad]])))}))
      (catch Throwable e
        {:path path :fail (str (.getName (class e)) ": " (.getMessage e))
         :trace (vec (take 12 (map str (.getStackTrace e))))}))))

(defn dump-files
  "[path rel] of the .edn files of the paths, sorted; rel relative to the path given."
  [paths]
  (sort-by first
           (for [p paths
                 :let [root (File. ^String p)]
                 ^File f (file-seq root)
                 :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))]
             [(.getPath f)
              (if (.isDirectory root)
                (subs (.getPath f) (inc (count (.getPath root))))
                (.getName f))])))

(defn- merge-counts [ms] (apply merge-with + {} ms))

(defn -main [& args]
  (let [[cmd & args] args
        _ (when (not= cmd "convert")
            (println "usage: convert [--out DIR] [--full] [--jobs N] [--goroot DIR] [--no-check] [--report FILE] DUMP|DIR...")
            (System/exit 2))
        [opts paths] (loop [opts {:out ".tmp/g2c/forms" :positions :lines :jobs 1 :check true}
                            [a & more :as args] args]
                       (case a
                         "--out" (recur (assoc opts :out (first more)) (rest more))
                         "--full" (recur (assoc opts :positions :full) more)
                         "--jobs" (recur (assoc opts :jobs (parse-long (first more))) (rest more))
                         "--goroot" (recur (assoc opts :goroot (first more)) (rest more))
                         "--no-check" (recur (assoc opts :check false) more)
                         "--report" (recur (assoc opts :report (first more)) (rest more))
                         "--quiet" (recur (assoc opts :quiet true) more)
                         [opts args]))
        files (dump-files paths)
        t0 (System/nanoTime)
        pool (Executors/newFixedThreadPool (:jobs opts))
        futs (mapv (fn [[p rel]] (.submit pool ^Callable (fn [] (convert-dump p rel opts)))) files)
        rs (mapv #(.get ^java.util.concurrent.Future %) futs)
        wall (/ (- (System/nanoTime) t0) 1e9)
        fails (filter :fail rs)
        bad (filter #(seq (:bad %)) rs)
        ok (remove :fail rs)
        nodes (merge-counts (map #(get-in % [:stats :nodes]) ok))
        dump-nodes (merge-counts (map :dump-nodes ok))
        notes (merge-counts (map #(get-in % [:stats :notes]) ok))
        lay (merge-counts (map :layout ok))
        unplaced (mapcat (fn [r] (map #(vector (:package r) %) (:unplaced r))) ok)]
    (.shutdown pool)
    (.awaitTermination pool 1 TimeUnit/MINUTES)
    (doseq [r rs]
      (cond
        (:fail r) (do (println "FAIL" (:path r) (:fail r))
                      (doseq [t (:trace r)] (println "    " t)))
        (seq (:bad r)) (doseq [[f m] (:bad r)] (println "BAD" (:path r) f m))
        (not (:quiet opts))
        (println (format "%s: %s, %d files, %.1f KB, %.0f ms (read %.0f ms)"
                         (:path r) (:ns r) (:files r) (/ (:bytes r) 1e3) (:ms r) (:read-ms r)))))
    (doseq [[pkg u] unplaced] (println "UNPLACED directive" pkg u))
    (println "node kinds (converted / in the dumps):")
    (doseq [k (sort (set (concat (keys nodes) (keys dump-nodes))))]
      (println (format "  %-20s %9d / %9d%s" (name k) (get nodes k 0) (get dump-nodes k 0)
                       (if (= (get nodes k 0) (get dump-nodes k 0)) "" "  *"))))
    (println "notes:" (pr-str (into (sorted-map) notes)))
    (println "layout:" (pr-str (into (sorted-map) lay)))
    (println (format "converted %d of %d dumps (%d files, %.1f MB of forms) in %.1f s with %d threads; %d failed, %d did not read back"
                     (count ok) (count rs) (reduce + (map :files ok))
                     (/ (reduce + (map :bytes ok)) 1e6) wall (:jobs opts) (count fails) (count bad)))
    (when-let [rf (:report opts)]
      (spit rf (pr-str {:dumps (count rs) :failed (count fails) :bad (count bad)
                        :files (reduce + (map :files ok)) :bytes (reduce + (map :bytes ok))
                        :seconds wall :nodes (into (sorted-map) nodes)
                        :dump-nodes (into (sorted-map) dump-nodes)
                        :notes (into (sorted-map) notes) :layout (into (sorted-map) lay)
                        :unplaced (vec unplaced)})))
    (shutdown-agents)
    (System/exit (if (or (seq fails) (seq bad)) 1 0))))
