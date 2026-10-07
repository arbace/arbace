(ns native.aot-test
  "Class forms, and fns and deftypes handed over to the class forms compiler, compiled ahead of time
  (doc/classes/SPEC.md §9.3): the classes are written to *compile-path*, and a fresh JVM loads
  them from there without compiling anything."
  (:require [arbace.test :refer :all]
            [arbace.string :as str]
            [arbace.java.io :as io]))

(deftest aot
  (let [dir (io/file (System/getProperty "arbace.tmp" ".tmp") "native-tests" "aot")]
    (doseq [f (reverse (file-seq dir))] (.delete ^java.io.File f))
    (.mkdirs dir)
    (binding [*compile-path* (str dir)]
      (compile 'native.aot-sample))
    (is (.exists (io/file dir "native/aot_sample/Pair.class")))
    (is (.exists (io/file dir "native/aot_sample/Ping.class")))
    (is (.exists (io/file dir "native/aot_sample/Box.class")))
    (is (seq (filter #(str/includes? (.getName ^java.io.File %) "kind") (file-seq dir))))
    (let [cp (str dir java.io.File/pathSeparator (System/getProperty "java.class.path"))
          p (-> (ProcessBuilder. ["java" "-cp" cp "arbace.lang.Main" "-e"
                                  "(require 'native.aot-sample) (prn (native.aot-sample/result))"])
                (.redirectErrorStream true)
                .start)
          out (slurp (.getInputStream p))]
      (.waitFor p)
      (is (str/includes? out "[[:zero :small :big] 1 1 native.aot_sample$kind") out)
      (is (str/includes? out "\"box3\"") out)
      ;; loaded from the class path, not compiled again
      (is (re-find #"AppClassLoader|app" out) out))))
