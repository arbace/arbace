(ns arbace.classes.verify
  "The JDK's class file verifier (java.lang.classfile.ClassFile/verify, JEP 484) as a test oracle
  for the class files Arbace emits: the class forms compiler's and the Clojure compiler's.
  bin/build-arbace runs it on every stage it builds, and test/native/verify_test.clj on classes
  the compiler writes at run time.

  Class hierarchy questions are answered from the class files themselves: first from the
  directories given (the tree being verified and its class path), then from the running JDK.
  So a tree is verified against its own classes, never against same-named classes loaded in the
  verifying runtime.

  (verify-tree dir) or, from a command line,
  java -cp target/stageN:. arbace.lang.Main -m arbace.classes.verify DIR [CLASS-DIR...]
  verifies every .class file under DIR and prints a report; the exit status is 1 when any class
  has errors, or the major version of a class is not the running JDK's."
  (:require [arbace.java.io :as io]
            [arbace.string :as str])
  (:import (java.lang.classfile ClassFile ClassFile$Option ClassFile$ClassHierarchyResolverOption
                                ClassHierarchyResolver)
           (java.io File FileInputStream)
           (java.nio.file Files)
           (java.util.concurrent ConcurrentHashMap)
           (java.util.function Function Supplier)))

(def jdk-version
  "The class file major version of the running JDK."
  (+ 44 (.feature (Runtime/version))))

(defn- resolver
  "A thread-safe ClassHierarchyResolver that parses classes from the directories dirs, then
  falls back to the JDK's default resolver."
  ^ClassHierarchyResolver [dirs]
  (let [dirs (mapv io/file dirs)]
    (-> (ClassHierarchyResolver/ofResourceParsing
          (reify Function
            (apply [_ cd]
              (let [d (.descriptorString ^java.lang.constant.ClassDesc cd)
                    path (str (subs d 1 (dec (count d))) ".class")]
                (some (fn [^File dir]
                        (let [f (File. dir path)]
                          (when (.isFile f) (FileInputStream. f))))
                      dirs)))))
        (.orElse (ClassHierarchyResolver/defaultResolver))
        (.cached (reify Supplier (get [_] (ConcurrentHashMap.)))))))

(defn class-file
  "A ClassFile context that verifies against the class files under dirs, then the JDK."
  ^ClassFile [dirs]
  (ClassFile/of (into-array ClassFile$Option
                            [(ClassFile$ClassHierarchyResolverOption/of (resolver dirs))])))

(defn major-version
  "The major version of the class file bytes."
  [^bytes b]
  (bit-or (bit-shift-left (bit-and (aget b 6) 0xff) 8) (bit-and (aget b 7) 0xff)))

(defn verify-bytes
  "The verify errors of class file bytes b, as messages; a major version other than the running
  JDK's counts as an error."
  [^ClassFile cf ^bytes b]
  (let [v (major-version b)]
    (cond-> (mapv #(.getMessage ^Throwable %) (.verify cf b))
      (not= v jdk-version) (conj (str "major version " v ", not " jdk-version)))))

(defn verify-tree
  "Verifies every .class file under dir (in parallel), resolving the class hierarchy from dir,
  then the directories cp, then the JDK. Returns {:classes n :failed [[path messages]...]
  :ms elapsed}."
  [dir & cp]
  (let [t0 (System/nanoTime)
        root (.toPath (io/file dir))
        cf (class-file (cons dir cp))
        files (->> (file-seq (io/file dir))
                   (filter #(str/ends-with? (.getName ^File %) ".class"))
                   (sort-by #(.getPath ^File %)))
        check (fn [^File f]
                (let [errs (try (verify-bytes cf (Files/readAllBytes (.toPath f)))
                                (catch Throwable e [(str "verifier threw: " e)]))]
                  (when (seq errs) [(str (.relativize root (.toPath f))) errs])))
        failed (->> files (partition-all 32) (pmap #(doall (keep check %))) (apply concat) vec)]
    {:classes (count files) :failed failed :ms (quot (- (System/nanoTime) t0) 1000000)}))

(defn report
  "Prints the result of verify-tree for dir; returns true when no class failed."
  [dir {:keys [classes failed ms]}]
  (println (str "   verified " dir ": " classes " classes, " (count failed) " with errors ("
                ms " ms, class file version " jdk-version ")"))
  (doseq [[kind n] (->> failed
                        (map #(-> % second first (str/replace #"\d+" "N")
                                  (str/replace #" in method .*| for .*" "")))
                        frequencies (sort-by (comp - val)))]
    (println (str "     " n " x " kind)))
  (doseq [[path errs] (take 20 failed)]
    (println (str "     " path))
    (doseq [e (take 3 errs)] (println (str "       " e))))
  (when (> (count failed) 20) (println (str "     ... and " (- (count failed) 20) " more")))
  (and (pos? classes) (empty? failed)))

(defn -main [dir & cp]
  (let [ok (report dir (apply verify-tree dir cp))]
    (shutdown-agents)
    (System/exit (if ok 0 1))))
