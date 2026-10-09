;; Helper for bin/clojure-tests: runs Clojure's upstream test suite (clojure/clojure test/) against
;; the Clojure on the class path and compares the outcome with a recorded reference.
;;
;;   list DIR EXPECTED
;;       Print the test namespaces under DIR, one per line, in the order in which upstream's
;;       src/script/run_test.clj finds and requires them (clojure.tools.namespace.find), minus
;;       the :skipped ones of the EXPECTED results file.
;;   run NS OUT NSLIST
;;       Require every namespace listed in the file NSLIST, in order, as run_test.clj does before
;;       running anything (so that helpers such as clojure.test-helper's assert-expr methods are
;;       loaded), then run NS's clojure.test tests and write a result map to OUT:
;;         {:ns NS :test n :pass n :fail n :error n
;;          :failing {test-name {:fail n :error n}}  ; per top-level deftest
;;          :load-error "message"}                   ; only if NS itself failed to load
;;       Unlike run_test.clj, a namespace that fails to load is reported instead of aborting.
;;   generative DIR OUT [NSLIST]
;;       What src/script/run_test_generative.clj does (clojure.test.generative.runner/-main on
;;       DIR), except that a namespace that fails to load is recorded instead of aborting the
;;       run; with NSLIST, the namespaces listed in that file instead of those found in DIR (the
;;       Go build, which has no java.io.File for tools.namespace to search directories with). Writes {:tests n :failures n :failing #{spec} :load-errors #{ns}} to OUT.
;;   report OUT EXPECTED RESULTS NSS
;;       Read the OUT/<ns>.edn results of the namespaces NSS (a space-separated string) and
;;       OUT/generative.edn, print per-namespace counts and totals, write all results to RESULTS
;;       (shaped like EXPECTED) and compare them with EXPECTED. Exits 1 on any regression.
;;
;; Like run_test.clj it sets java.awt.headless. The run and generative modes also run on the Go
;; build (bin/clojure-tests with CLOJURE_TESTS_GO), which has no java.io.File: they read and
;; write files through FileInputStream and FileOutputStream only.

(System/setProperty "java.awt.headless" "true")
(require '[clojure.test :as t])

(defn- read-edn [f]
  (let [f (java.io.File. ^String f)]
    (when (.exists f)
      (read-string (slurp f)))))

(defn- slurp-file [^String f] (slurp (java.io.FileInputStream. f) :encoding "UTF-8"))
(defn- spit-file [^String f s] (spit (java.io.FileOutputStream. f) s :encoding "UTF-8"))

(defn- try-require
  "Require ns-sym; on failure print the stack trace and return the error message."
  [ns-sym]
  (try
    (require ns-sym)
    nil
    (catch Throwable e
      (binding [*out* *err*]
        (println "failed to load" ns-sym))
      (.printStackTrace e)
      (str (.getName (class e)) ": " (.getMessage e)))))

(defn- top-test-name
  "The name of the outermost deftest being run, as a symbol, or nil outside any test."
  []
  (when-let [v (last t/*testing-vars*)]
    (let [{:keys [ns name]} (meta v)]
      (symbol (str (ns-name ns)) (str name)))))

(defn- run-ns [ns-sym out nslist]
  (let [nss       (map symbol (re-seq #"\S+" (slurp-file nslist)))
        load-errs (into {} (for [n (distinct (concat nss [ns-sym]))
                                 :let [err (try-require n)]
                                 :when err]
                             [n err]))
        failing   (atom (sorted-map))
        result    (if-let [err (load-errs ns-sym)]
                    {:test 0 :pass 0 :fail 0 :error 0 :load-error err}
                    (let [report t/report]
                      (binding [t/report (fn [m]
                                           (when (#{:fail :error} (:type m))
                                             (swap! failing update
                                                    (or (top-test-name) 'outside-any-test)
                                                    (fnil update {:fail 0 :error 0})
                                                    (:type m) inc))
                                           (report m))]
                        (select-keys (t/run-tests ns-sym) [:test :pass :fail :error]))))]
    (spit-file out (pr-str (assoc result :ns ns-sym :failing @failing)))
    (shutdown-agents)
    (System/exit 0)))

(defn- find-namespaces [dir]
  (require 'clojure.tools.namespace.find)
  ((resolve 'clojure.tools.namespace.find/find-namespaces-in-dir) (java.io.File. ^String dir)))

(defn- run-generative [dir out nslist]
  (require 'clojure.test.generative.runner)
  (let [config    (resolve 'clojure.test.generative.runner/config)
        get-tests (resolve 'clojure.test.generative.runner/get-tests)
        run-n     (resolve 'clojure.test.generative.runner/run-n)
        nss       (if nslist
                    (map symbol (re-seq #"\S+" (slurp-file nslist)))
                    (find-namespaces dir))
        load-errs (set (filter try-require nss))
        loaded    (remove load-errs nss)
        tests     (mapcat get-tests (mapcat (comp vals ns-interns) loaded))
        cfg       (config)
        results   (doall (run-n cfg tests))
        failed    (filter :exception results)]
    (doseq [r failed]
      (.printStackTrace ^Throwable (:exception r))
      (prn (dissoc r :exception)))
    (let [summary {:tests (count tests)
                   :failures (count failed)
                   :failing (into (sorted-set) (map :test failed))
                   :load-errors (into (sorted-set) load-errs)
                   :iters (reduce + (keep :iter results))
                   :config cfg}]
      (prn summary)
      (spit-file out (pr-str summary)))
    (shutdown-agents)
    (System/exit 0)))

(defn- regressions
  "Why the actual result r of a namespace is worse than the expected one e, or nil."
  [e r]
  (let [e (or e {:test 0 :pass 0 :fail 0 :error 0})]
    (cond
      (nil? r) ["no result (crashed or timed out)"]
      (and (:load-error r) (not (:load-error e))) [(str "load error: " (:load-error r))]
      :else
      (concat
        (when (not= (:test r) (:test e)) [(str "ran " (:test r) " tests, expected " (:test e))])
        (when (< (:pass r) (:pass e)) [(str (:pass r) " assertions passed, expected " (:pass e))])
        (for [[t c] (:failing r)
              :let [x (get-in e [:failing t] {:fail 0 :error 0})]
              :when (or (> (:fail c) (:fail x)) (> (:error c) (:error x)))]
          (str t " " c ", expected " x))))))

(defn- generative-regressions [e r]
  (let [e (or e {:failing #{} :load-errors #{}})]
    (cond
      (nil? r) ["no result (crashed or timed out)"]
      :else (concat
              (for [s (:failing r) :when (not (contains? (:failing e) s))] (str "spec " s " failed"))
              (for [n (:load-errors r) :when (not (contains? (:load-errors e) n))]
                (str n " failed to load"))
              (when (not= (:tests r) (:tests e)) [(str (:tests r) " specs, expected " (:tests e))])))))

(defn- report [out expected-file results-file nss]
  (let [expected (or (read-edn expected-file) {})
        nss      (map symbol (re-seq #"\S+" nss))
        results  (into (sorted-map)
                       (for [n nss] [n (some-> (read-edn (str out "/" n ".edn")) (dissoc :ns))]))
        gen-file (str out "/generative.edn")
        gen-ran  (.exists (java.io.File. (str out "/generative.log")))
        gen      (some-> (read-edn gen-file) (select-keys [:tests :failures :failing :load-errors]))
        bad      (atom [])
        line     (fn [n t a p f e s]
                   (println (format "%-62s %5s %6s %6s %5s %5s  %s" n t a p f e s)))]
    (line "namespace" "tests" "assert" "pass" "fail" "error" "")
    (doseq [[n r] results
            :let [e (get-in expected [:namespaces n])
                  why (regressions e r)]]
      (when (seq why) (swap! bad conj [n why]))
      (if r
        (line n (:test r) (+ (:pass r) (:fail r) (:error r)) (:pass r) (:fail r) (:error r)
              (cond (seq why) "REGRESSION"
                    (:load-error r) "load error (expected)"
                    (not= (seq (:failing r)) (seq (:failing e))) "improved"
                    (seq (:failing r)) "expected failures"
                    :else ""))
        (line n "-" "-" "-" "-" "-" "REGRESSION")))
    (let [rs (keep val results)
          sum (fn [k] (reduce + (keep k rs)))]
      (line (format "TOTAL: %d namespaces (%d failed to load), %d skipped"
                    (count results) (count (filter :load-error rs)) (count (:skipped expected)))
            (sum :test) (+ (sum :pass) (sum :fail) (sum :error))
            (sum :pass) (sum :fail) (sum :error) ""))
    (when gen-ran
      (println "test.generative:" (or gen "no result (crashed?)"))
      (when-let [why (seq (generative-regressions (:generative expected) gen))]
        (swap! bad conj ['test.generative why])))
    (spit results-file
          (with-out-str
            ((requiring-resolve 'clojure.pprint/pprint)
             (cond-> {:revision (:revision expected)
                      :skipped (:skipped expected)
                      :namespaces results}
               gen (assoc :generative gen)))))
    (println "results written to" results-file)
    (shutdown-agents)
    (if (seq @bad)
      (do (println "\nREGRESSIONS against" expected-file)
          (doseq [[n why] @bad, w why] (println " " n "-" w))
          (System/exit 1))
      (do (println "\nno regressions against" expected-file)
          (System/exit 0)))))

(let [[mode a b c d] *command-line-args*]
  (case mode
    "list"       (let [skipped (set (keys (:skipped (read-edn b))))]
                   (doseq [n (find-namespaces a) :when (not (skipped n))]
                     (println n))
                   (shutdown-agents))
    "run"        (run-ns (symbol a) b c)
    "generative" (run-generative a b c)
    "report"     (report a b c d)))
