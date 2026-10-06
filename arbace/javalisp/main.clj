(ns arbace.javalisp.main
  "Command line entry: java2jls, jls2java, transcribe, check, classes, classes-each.
  See doc/javalisp/SPEC.md."
  (:require [arbace.javalisp.j2l :as j2l]
            [arbace.javalisp.l2j :as l2j]
            [arbace.javalisp.check :as check]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- report [path violations]
  (binding [*out* *err*]
    (doseq [v violations] (println (str path ": line violation " v)))))

(defn java2jls [path]
  (let [{:keys [text violations errors]} (j2l/transcribe path (slurp path))]
    (if (seq errors)
      (binding [*out* *err*] (run! println errors))
      (do (print text) (flush) (report path violations)))))

(defn jls2java [path]
  (let [{:keys [text violations]} (l2j/translate (slurp path))]
    (print text)
    (flush)
    (report path violations)))

(defn transcribe
  "Transcribe every .java file under directory `src` to a .jls file at the same relative
  path under `out`."
  [src out]
  (let [root (.getCanonicalPath (io/file src))
        files (check/java-files [src])
        results (doall (pmap (fn [f]
                               (let [{:keys [text violations errors]} (j2l/transcribe f (slurp f))
                                     rel (subs (.getCanonicalPath (io/file f)) (inc (count root)))
                                     o (io/file out (str/replace rel #"\.java$" ".jls"))]
                                 (when text
                                   (io/make-parents o)
                                   (spit o text))
                                 {:file f :errors errors :violations violations}))
                             files))]
    (doseq [{:keys [file errors violations]} results]
      (binding [*out* *err*] (run! #(println (str file ": " %)) errors))
      (report file violations))
    (println (format "%d files transcribed to %s" (count (remove (comp seq :errors) results)) out))))

(defn -main [cmd & args]
  (case cmd
    "java2jls" (run! java2jls args)
    "jls2java" (run! jls2java args)
    "transcribe" (apply transcribe args)
    "check" (let [[opts paths] (split-with #(.startsWith ^String % "-") args)
                  opts (set opts)]
              (check/check paths :verbose (opts "-v")
                           :out-dir (when (opts "-o") ".tmp/rt")))
    "classes" (check/compare-classes args ["-g" "-nowarn"])
    "classes-each" (check/compare-each args ["-g" "-nowarn" "--enable-preview"]))
  (shutdown-agents))
