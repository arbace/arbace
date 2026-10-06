(ns classes.loader-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [classes.helpers :refer :all]
            [arbace.classes.compiler :as compiler]))

(deftest package-access-across-forms
  ;; two separate class forms of one package: package-private access works (one package loader)
  (let [[A] (load-forms 'classes.loader-test '[(PkgA (method ^:static secret ^int [] 41))])
        [B] (load-forms 'classes.loader-test
                        '[(^:public PkgB (method ^:public ^:static peek ^int [] (unchecked-inc-int (PkgA/secret))))])]
    (is (= 42 (clojure.lang.Reflector/invokeStaticMethod B "peek" (object-array 0))))
    (is (identical? (.getClassLoader A) (.getClassLoader B)))))

(deftest redefinition-starts-a-generation
  (let [[c1] (load-forms 'classes.loader-test '[(^:public Gen (method ^:public ^:static v ^int [] 1))])
        [c2] (load-forms 'classes.loader-test '[(^:public Gen (method ^:public ^:static v ^int [] 2))])]
    (is (not (identical? c1 c2)))
    (is (not (identical? (.getClassLoader c1) (.getClassLoader c2))))
    (is (= 1 (clojure.lang.Reflector/invokeStaticMethod c1 "v" (object-array 0))))
    (is (= 2 (clojure.lang.Reflector/invokeStaticMethod c2 "v" (object-array 0))))
    (is (identical? c2 (Class/forName "classes.loader_test.Gen" false (clojure.lang.RT/makeClassLoader))))))

(deftest aot-compilation
  (let [out (io/file tmp-root "aot" (str (System/nanoTime)))]
    (.mkdirs out)
    (binding [*compile-path* (.getPath out)]
      (compile 'classes.aot.sample))
    (is (.exists (io/file out "classes/aot/sample/Greeter.class")))
    (is (.exists (io/file out "classes/aot/sample__init.class")))
    ;; a plain baseline JVM, without the class forms compiler, loads the compiled namespace
    (let [r (sh/sh "java" "-cp" (str (.getPath out) java.io.File/pathSeparator (System/getProperty "java.class.path"))
                   "clojure.main" "-e"
                   "(require 'classes.aot.sample) (println classes.aot.sample/answer (.getName (.getClassLoader classes.aot.sample.Greeter)))")]
      (is (= 0 (:exit r)) (:err r))
      (is (re-find #"^Hello, AOT app" (:out r)) (:out r)))))
