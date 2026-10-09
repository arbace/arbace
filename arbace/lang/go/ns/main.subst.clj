;; The Go build's arbace/main.clj (doc/go/EVAL-NOTES.md, "Namespace variants"):
;; - the REPL makes no DynamicClassLoader for the thread (one loader, C2G-SPEC §10.3);
;; - an error report is printed with prn, not pprint (whose writers are proxies, D6), into a
;;   file of java.io.tmpdir written through FileOutputStream (java.nio's Files is cut, D6).
["(.setContextClassLoader (Thread/currentThread) (arbace.lang.DynamicClassLoader. cl))"
 "(.setContextClassLoader (Thread/currentThread) cl)"

 "((requiring-resolve 'arbace.pprint/pprint) report)"
 "(prn report)"

 "(let [f (.toFile (Files/createTempFile \"arbace-\" \".edn\" (into-array FileAttribute [])))]
                         (with-open [w (BufferedWriter. (FileWriter. f))]
                           (binding [*out* w] (println report-str)))
                         (.getAbsolutePath f))"
 "(let [f (str (System/getProperty \"java.io.tmpdir\") \"/arbace-\" (System/nanoTime) \".edn\")]
                         (with-open [w (java.io.OutputStreamWriter. (java.io.FileOutputStream. f) \"UTF-8\")]
                           (binding [*out* w] (println report-str)))
                         f)"]
