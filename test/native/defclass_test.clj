(ns native.defclass-test
  "Class forms native to Arbace (doc/classes/SPEC.md §9.5): defclass and the other names are
  arbace.core vars, class* is a special form of arbace.lang.Compiler, with no boot step."
  (:require [arbace.test :refer :all]
            [arbace.string :as str]))

(defclass ^:public Counter
  (field ^:private ^int n)
  (method ^:public inc ^int [this] (set! n (unchecked-inc-int n))))

(deftest defclass-at-load
  (is (= 2 (let [c (Counter.)] (.inc c) (.inc c))))
  (is (= "native.defclass_test.Counter" (.getName Counter))))

(deftest names-are-core-vars
  (doseq [s '[defclass defclasses anon letclass defmodule defpackage label break continue return
              switch lambda method-ref java-str java-assert for-each with-resources if-instance
              when-instance bit-and-int unchecked-add-float]]
    (is (= "arbace.core" (str (:ns (meta (resolve s))))) (str s)))
  (doseq [s '[class* label* break* continue* return* switch* lambda* method-ref* java-str*
              java-assert* for-each* with-resources* if-instance*]]
    (is (special-symbol? s) (str s)))
  (is (= 4 (bit-and-int 7 12))))

;; a top-level do of mutually referring class forms (§9.2)
(do
  (defclass ^:public Even (method ^:public ^:static even ^boolean [^int n] (if (== n 0) true (Odd/odd (unchecked-dec-int n)))))
  (defclass ^:public Odd (method ^:public ^:static odd ^boolean [^int n] (if (== n 0) false (Even/even (unchecked-dec-int n))))))

(deftest top-level-do
  (is (true? (Even/even 10)))
  (is (true? (Odd/odd 7))))

(defn- eval-here [form] (binding [*ns* (the-ns 'native.defclass-test)] (eval form)))

(deftest defclasses-vector
  (let [cs (eval-here '(defclasses (defclass ^:public P1 (method ^:public ^:static f ^int [] (P2/g)))
                              (defclass ^:public P2 (method ^:public ^:static g ^int [] 7))))]
    (is (= ["native.defclass_test.P1" "native.defclass_test.P2"] (mapv #(.getName ^Class %) cs)))
    (is (= 7 (eval-here '(P1/f))))))

(defn- repl-session
  "Output of a REPL (arbace.main/repl) reading `input`, in a fresh namespace."
  [input]
  (let [ns (gensym "native.session")]
    (binding [*ns* (create-ns ns)]
      (refer-clojure)
      (with-out-str
        (with-in-str input
          (arbace.main/repl :prompt (fn [] (print "=> "))
                            :init (fn [])
                            :caught (fn [e] (println "ERROR" (.getMessage ^Throwable e)))))))))

(deftest repl-session-without-boot
  (let [out (repl-session (str "(defclass ^:public Pt (field ^:public ^int x) (constructor ^:public [this ^int x] (set! (.-x this) x)))\n"
                               "(.-x (Pt. 5))\n"
                               "(do (defclass ^:public R1 (method ^:public ^:static a ^int [] (R2/b)))\n"
                               "    (defclass ^:public R2 (method ^:public ^:static b ^int [] 9)))\n"
                               "(R1/a)\n"))]
    (is (not (str/includes? out "ERROR")) out)
    (is (re-find #"=> \S*\.Pt\n=> 5\n=> \S*\.R2\n=> 9" out) out)))
