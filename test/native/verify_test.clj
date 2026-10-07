(ns native.verify-test
  "The JDK's class file verifier (arbace.classes.verify) on classes the compiler emits at run
  time: a sample namespace compiled ahead of time (fns, deftype, defrecord, protocols, reify,
  proxy, gen-class, gen-interface, class forms), and a broken class it must reject."
  (:require [arbace.test :refer :all]
            [arbace.java.io :as io]
            [arbace.classes.verify :as v])
  (:import (arbace.asm ClassWriter Opcodes)))

(deftest compiled-namespace
  (let [dir (io/file (System/getProperty "arbace.tmp" ".tmp") "native-tests" "verify")]
    (doseq [f (reverse (file-seq dir))] (.delete ^java.io.File f))
    (.mkdirs dir)
    (binding [*compile-path* (str dir)]
      (compile 'native.verify-sample))
    (is (= [3 :arith :few 3 1 6.0 "cell6" 3 "long 1" "reified" 3 5]
           ((resolve 'native.verify-sample/result))))
    (let [{:keys [classes failed]} (v/verify-tree dir)]
      (is (< 40 classes) classes)
      (is (empty? failed) (pr-str failed))
      (is (.exists (io/file dir "native/verify_sample.class")))
      (is (.exists (io/file dir "native/verify_sample/Acc.class"))))))

(deftest rejects-bad-bytecode
  (let [cw (ClassWriter. 0)]
    (.visit cw v/jdk-version Opcodes/ACC_PUBLIC "native/Bad" nil "java/lang/Object" nil)
    (doto (.visitMethod cw (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC) "f" "()I" nil nil)
      (.visitCode)
      (.visitLdcInsn "not an int")
      (.visitInsn Opcodes/IRETURN)
      (.visitMaxs 1 0)
      (.visitEnd))
    (.visitEnd cw)
    (let [cf (v/class-file [])]
      (is (seq (v/verify-bytes cf (.toByteArray cw))))
      (is (= 61 (v/major-version (doto (.toByteArray cw) (aset-byte 7 (byte 61))))))
      (is (some #(re-find #"major version 61" %)
                (v/verify-bytes cf (doto (.toByteArray cw) (aset-byte 7 (byte 61)))))))))
