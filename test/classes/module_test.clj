(ns classes.module-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [classes.helpers :refer :all]
            [arbace.classes.compiler :as compiler]
            [arbace.classes.shape :as shape]))

(defn- javac-module []
  (let [dir (io/file tmp-root "module" (str (System/nanoTime)))
        src (io/file dir "src") out (io/file dir "out")
        files {"module-info.java" "@Deprecated
open module com.example.app {
  requires transitive java.logging;
  requires static java.desktop;
  exports com.example.api;
  exports com.example.spi to java.base, java.logging;
  uses com.example.spi.Plugin;
  provides com.example.spi.Plugin with com.example.impl.DefaultPlugin;
}"
               "com/example/api/A.java" "package com.example.api; public class A {}"
               "com/example/spi/Plugin.java" "package com.example.spi; public interface Plugin {}"
               "com/example/impl/DefaultPlugin.java" "package com.example.impl; public class DefaultPlugin implements com.example.spi.Plugin {}"
               "com/example/old/package-info.java" "@Deprecated\npackage com.example.old;\n"
               "com/example/old/X.java" "package com.example.old; class X {}"}]
    (doseq [[n s] files] (let [f (io/file src n)] (io/make-parents f) (spit f s)))
    (.mkdirs out)
    (let [rc (.run (javax.tools.ToolProvider/getSystemJavaCompiler) nil nil nil
                   (into-array String (concat ["-d" (.getPath out)] (map #(.getPath (io/file src %)) (keys files)))))]
      (assert (zero? rc)))
    out))

(deftest module-and-package-info
  (let [out (javac-module)
        ours (compiler/module-bytes *ns*
               '(^:open ^{Deprecated true} com.example.app
                 (requires ^:transitive java.logging)
                 (requires ^:static java.desktop)
                 (exports com.example.api)
                 (exports com.example.spi :to [java.base java.logging])
                 (uses com.example.spi.Plugin)
                 (provides com.example.spi.Plugin :with [com.example.impl.DefaultPlugin])))
        pkg (compiler/package-bytes *ns* (with-meta 'com.example.old {'Deprecated true}))
        d1 (shape/diff (shape/read-shape (io/file out "module-info.class")) (shape/shape ours))
        d2 (shape/diff (shape/read-shape (io/file out "com/example/old/package-info.class")) (shape/shape (:bytes pkg)))]
    (is (empty? d1) (pr-str d1))
    (is (empty? d2) (pr-str d2))))
