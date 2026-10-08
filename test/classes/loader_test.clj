(ns classes.loader-test
  (:require [arbace.test :refer :all]
            [arbace.java.io :as io]
            [arbace.java.shell :as sh]
            [classes.helpers :refer :all]
            [arbace.classes.compiler :as compiler]))

(deftest package-access-across-forms
  ;; two separate class forms of one package: package-private access works (one package loader)
  (let [[A] (load-forms 'classes.loader-test '[(PkgA (method ^:static secret ^int [] 41))])
        [B] (load-forms 'classes.loader-test
                        '[(^:public PkgB (method ^:public ^:static peek ^int [] (unchecked-inc-int (PkgA/secret))))])]
    (is (= 42 (arbace.lang.Reflector/invokeStaticMethod B "peek" (object-array 0))))
    (is (identical? (.getClassLoader A) (.getClassLoader B)))))

(deftest redefinition-starts-a-generation
  (let [[c1] (load-forms 'classes.loader-test '[(^:public Gen (method ^:public ^:static v ^int [] 1))])
        [c2] (load-forms 'classes.loader-test '[(^:public Gen (method ^:public ^:static v ^int [] 2))])]
    (is (not (identical? c1 c2)))
    (is (not (identical? (.getClassLoader c1) (.getClassLoader c2))))
    (is (= 1 (arbace.lang.Reflector/invokeStaticMethod c1 "v" (object-array 0))))
    (is (= 2 (arbace.lang.Reflector/invokeStaticMethod c2 "v" (object-array 0))))
    (is (identical? c2 (Class/forName "classes.loader_test.Gen" false (arbace.lang.RT/makeClassLoader))))))

(deftest aot-compilation
  (let [out (io/file tmp-root "aot" (str (System/nanoTime)))]
    (.mkdirs out)
    (binding [*compile-path* (.getPath out)]
      (compile 'classes.aot.sample))
    (is (.exists (io/file out "classes/aot/sample/Greeter.class")))
    (is (.exists (io/file out "classes/aot/sample__init.class")))
    ;; a fresh JVM loads the compiled namespace from its classes
    (let [r (sh/sh "java" "-cp" (str (.getPath out) java.io.File/pathSeparator (System/getProperty "java.class.path"))
                   "arbace.lang.Main" "-e"
                   "(require 'classes.aot.sample) (println classes.aot.sample/answer (.getName (.getClassLoader classes.aot.sample.Greeter)))")]
      (is (= 0 (:exit r)) (:err r))
      (is (re-find #"^Hello, AOT app" (:out r)) (:out r)))))

(deftest source-path-cycles
  (require 'classes.srcpath)
  (let [ping (Class/forName "classes.srcpath.Ping" true (arbace.lang.RT/makeClassLoader))]
    (is (= 22 (arbace.lang.Reflector/invokeStaticMethod ping "ping" (object-array [(int 4)]))))
    ;; Pong/BASE, a constant of a class entered from source, is inlined
    (is (= 42 (.get (.getDeclaredField ping "LIMIT") nil)))))

(deftest repl-session
  ;; SPEC §10, typed into a REPL; then a redefinition, which old instances survive
  (let [input "(require 'arbace.classes.boot)
(defclass ^:public Counter
  (field ^:private ^int n)
  (method ^:public inc ^int [this] (set! n (unchecked-inc-int n))))
(def c1 (Counter.))
(let [c (Counter.)] (.inc c) (.inc c))
(defclass ^:public Counter
  (field ^:private ^int n)
  (method ^:public inc ^int [this] (set! n (unchecked-add-int n 10))))
(.inc (Counter.))
(.inc c1)
"
        r (sh/sh "java" "-cp" (System/getProperty "java.class.path") "arbace.lang.Main" :in input)]
    (is (= 0 (:exit r)) (:err r))
    (is (re-find #"(?s)user=> user\.Counter\n.*user=> 2\n.*user=> user\.Counter\n.*user=> 10\nuser=> 1\n" (:out r)) (:out r))))
