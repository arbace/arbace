;; The class forms interpreter (doc/go/CLASSFORMS-REPL.md) on the JVM, against the compiled
;; classes: run with bin/arbace-j test/classforms/jvm_check.clj [FILE...] (default
;; test/oracle/forms/defclass.clj). Each defclass form of the file is compiled (as the file
;; runs) and also interpreted, over the test host (Compiler$CF$TestHost: no Class objects, objects
;; are CF$Obj); each top-level form that calls static methods of the classes, and nothing else of
;; them, is evaluated both ways and compared. The interpreter's classes are the Go build's
;; variant arbace/lang/go/ClassForms.clj, defined here by defclass; the builder is
;; arbace/lang/go/ns/classes/interp.clj.
(ns classforms.jvm-check
  (:require [arbace.string :as str]
            [arbace.walk :as walk]))

(defn variant-classes [file]
  (let [r (arbace.lang.LineNumberingPushbackReader. (java.io.FileReader. ^String file))
        forms (binding [*read-eval* false]
                (doall (take-while #(not= % ::eof) (repeatedly #(read {:eof ::eof} r)))))]
    (for [f forms
          :when (and (seq? f) (= 'c2g/variant (first f)) (= 'Compiler (second f)))
          m (drop 2 f)
          :when (and (seq? m) (= 'c2g/add (first m)))
          :let [[_ nm & more] (second m)]
          :when (= 'CF nm)]
      (list* 'defclass (with-meta (symbol (str "arbace.lang.Compiler$" nm)) {:public true}) more))))

(doseq [f (variant-classes "arbace/lang/go/ClassForms.clj")]
  (binding [*ns* (the-ns 'classforms.jvm-check)] (eval f)))
(arbace.lang.Compiler$CF$Rt/setHost (arbace.lang.Compiler$CF$TestHost.))
(load-file "arbace/lang/go/ns/classes/interp.clj")

(def interp-ns (the-ns 'arbace.classes.interp))
(def registry @(ns-resolve interp-ns 'registry))
(def define! @(ns-resolve interp-ns 'define!))

(defn st
  "A static method of interpreted class cn named m, by its arguments' count."
  [cn m & args]
  (let [k ^arbace.lang.Compiler$CF$Klass (get @registry cn)
        ms (for [[key ^arbace.lang.Compiler$CF$Meth mt] (.-methods k)
                 :when (and (.-isStatic mt) (= m (.-name mt)) (= (count args) (count (.-ptypes mt))))]
             mt)]
    (when (not= 1 (count ms)) (throw (ex-info (str "no single static " cn "." m) {})))
    (.invoke ^arbace.lang.Compiler$CF$Meth (first ms) nil (object-array args))))

(defn outcome [thunk]
  (try {:value (pr-str (thunk))}
       (catch Throwable e
         (let [e (if (instance? arbace.lang.Compiler$CompilerException e) (or (.getCause e) e) e)]
           {:ex [(.getName (class e)) (.getMessage e)]}))))

(defn chain [^Throwable e]
  (str/join " / " (for [x (take-while some? (iterate #(.getCause ^Throwable %) e))]
                    (str (.getName (class x)) ": " (.getMessage x)))))

(defn check-file [file]
  (let [r (arbace.lang.LineNumberingPushbackReader. (java.io.FileReader. ^String file))
        classes (atom #{})
        stats (atom {:ok 0 :fail 0 :defs 0 :def-fail 0 :skipped 0})]
    (binding [*ns* (create-ns (symbol (str "cfj" (System/nanoTime))))]
      (refer-clojure)
      (loop []
        (let [line (.getLineNumber r)
              f (binding [*read-eval* false] (read {:eof ::eof :read-cond :allow} r))]
          (when-not (= f ::eof)
            (cond
              (and (seq? f) (= 'defclass (first f)))
              (do (try (eval f) (catch Throwable e (println "compile" line (chain e))))
                  (swap! stats update :defs inc)
                  (try
                    (let [[[n]] (define! *ns* [(rest f)] nil)]
                      (swap! classes conj (symbol (last (str/split n #"\.")))))
                    (catch Throwable e
                      (swap! stats update :def-fail inc)
                      (println "INTERP DEFINE" line (chain e)))))
              :else
              (let [uses (atom #{})
                    other (atom false)
                    g (walk/prewalk
                        (fn [x]
                          (if (and (seq? x) (symbol? (first x)) (namespace (first x))
                                   (@classes (symbol (namespace (first x)))))
                            (do (swap! uses conj (first x))
                                (list* `st (str (str/replace (munge (ns-name *ns*)) "." "/") "/" (namespace (first x)))
                                       (name (first x)) (rest x)))
                            x))
                        f)
                    _ (walk/postwalk
                        (fn [x]
                          (when (and (symbol? x) (or (@classes x) (and (namespace x) (@classes (symbol (namespace x))))
                                                     (some #(str/starts-with? (name x) (str % ".")) @classes)))
                            (reset! other true))
                          x)
                        g)]
                (cond
                  (and (seq @uses) (not @other))
                  (let [a (outcome #(eval f)) b (outcome #(eval g))]
                    (if (= a b)
                      (swap! stats update :ok inc)
                      (do (swap! stats update :fail inc)
                          (println "MISMATCH" line (pr-str f))
                          (println "   jvm   " a)
                          (println "   interp" b))))
                  :else
                  (do (when (seq @uses) (swap! stats update :skipped inc))
                      (try (eval f) (catch Throwable _ nil))))))
            (recur)))))
    (println file @stats)))

(doseq [f (or (seq *command-line-args*) ["test/oracle/forms/defclass.clj"])]
  (check-file f))
