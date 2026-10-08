(ns arbace.g2c.read
  "Reads the dumps of g2c's front end helper (tools/godump, doc/go/HELPER-NOTES.md) with
  Arbace's reader into data.

  bin/g2c read [--jobs N] [--quiet] FILE|DIR...
    Reads each dump (DIRs: every .edn below them) with arbace.core/read and *read-eval*
    false, and prints per dump the time and a summary: files, nodes, annotated nodes, type
    errors, named types, directives and type table entries. Exits 1 if a dump does not read
    as one form, or if its type table is not in dependency order (check-type-table)."
  (:require [arbace.string :as str])
  (:import [java.io File FileInputStream InputStreamReader PushbackReader]
           [java.nio.charset StandardCharsets]
           [java.util.concurrent Executors TimeUnit]))

(defn read-dump
  "Reads the dump at path (one form) with *read-eval* false. Throws if anything but
  whitespace and comments follows the form."
  [path]
  (with-open [r (PushbackReader.
                  (InputStreamReader. (FileInputStream. (str path)) StandardCharsets/UTF_8)
                  (* 1 1024 1024))]
    (binding [*read-eval* false]
      (let [eof (Object.)
            d (read r false eof)]
        (when (identical? d eof)
          (throw (ex-info (str path ": empty dump") {:path (str path)})))
        (when-not (identical? (read r false eof) eof)
          (throw (ex-info (str path ": more than one form") {:path (str path)})))
        d))))

(defn node?
  "A node form: (:kebab-type {fields})."
  [x]
  (and (seq? x) (keyword? (first x)) (map? (second x)) (nil? (nnext x))))

(defn nodes
  "The node forms of a tree, depth first in source order."
  [tree]
  (filter node? (tree-seq coll? (fn [x] (if (map? x) (vals x) (seq x))) tree)))

(defn type-entry
  "The entry of type id in the dump's :type-table (doc/go/HELPER-NOTES.md, \"Types\")."
  [d id]
  (nth (:type-table d) id))

(defn- type-refs
  "The ids an entry of the type table refers to."
  [e]
  (concat (keep e [:elem :key :origin :actual])
          (:args e) (:embeddeds e)
          (map :t (concat (:params e) (:results e) (:fields e) (:methods e) (:terms e) (:vars e)))
          (mapcat (juxt :t :constraint) (:type-params e))))

(defn check-type-table
  "Throws unless every entry of the dump's type table is a map with a :kind and refers only to
  earlier entries (the table is in dependency order; named types are leaves)."
  [d]
  (doseq [[i e] (map-indexed vector (:type-table d))]
    (when-not (and (map? e) (keyword? (:kind e)))
      (throw (ex-info (str "type " i ": not an entry: " (pr-str e)) {:id i})))
    (doseq [r (type-refs e)]
      (when-not (and (int? r) (<= 0 r) (< r i))
        (throw (ex-info (str "type " i " refers to " (pr-str r)) {:id i :ref r})))))
  d)

(defn summary
  "Counts of a dump."
  [d]
  (let [ns (mapcat (comp nodes :ast) (:files d))]
    {:package (:package d)
     :files (count (:files d))
     :nodes (count ns)
     :annotated (count (filter meta ns))
     :errors (count (:errors d))
     :types (count (:types d))
     :directives (count (:directives d))
     :type-entries (count (:type-table d))}))

(defn dump-files
  "The .edn files of the paths (files, or directories searched recursively), sorted."
  [paths]
  (sort (for [p paths
              ^File f (file-seq (File. ^String p))
              :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))]
          (.getPath f))))

(defn- check [path]
  (let [t0 (System/nanoTime)]
    (try
      (let [d (check-type-table (read-dump path))
            ms (/ (- (System/nanoTime) t0) 1e6)]
        (assoc (summary d) :path path :ms ms :bytes (.length (File. ^String path))))
      (catch Throwable e
        {:path path :fail (str (.getName (class e)) ": " (.getMessage e))}))))

(defn -main [& args]
  (let [[opts paths] (loop [opts {:jobs 1} [a & more :as args] args]
                       (case a
                         "--jobs" (recur (assoc opts :jobs (parse-long (first more))) (rest more))
                         "--quiet" (recur (assoc opts :quiet true) more)
                         [opts args]))
        files (dump-files paths)
        t0 (System/nanoTime)
        pool (Executors/newFixedThreadPool (:jobs opts))
        futs (mapv (fn [f] (.submit pool ^Callable (fn [] (check f)))) files)
        rs (mapv #(.get ^java.util.concurrent.Future %) futs)
        wall (/ (- (System/nanoTime) t0) 1e9)
        fails (filter :fail rs)
        ok (remove :fail rs)]
    (.shutdown pool)
    (.awaitTermination pool 1 TimeUnit/MINUTES)
    (doseq [r rs]
      (cond
        (:fail r) (println "FAIL" (:path r) (:fail r))
        (not (:quiet opts))
        (println (format "%s: %s, %d files, %d nodes (%d annotated), %d errors, %d types, %d directives, %d type entries, %.1f MB, read in %.0f ms"
                         (:path r) (:package r) (:files r) (:nodes r) (:annotated r) (:errors r)
                         (:types r) (:directives r) (:type-entries r) (/ (:bytes r) 1e6) (:ms r)))))
    (println (format "read %d of %d dumps (%.1f MB, %d nodes, %d type entries, %d type errors) in %.1f s with %d threads; %d failed"
                     (count ok) (count rs) (/ (reduce + (map :bytes ok)) 1e6)
                     (reduce + (map :nodes ok)) (reduce + (map :type-entries ok))
                     (reduce + (map :errors ok)) wall (:jobs opts)
                     (count fails)))
    (shutdown-agents)
    (System/exit (if (seq fails) 1 0))))
