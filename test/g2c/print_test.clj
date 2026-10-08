(ns g2c.print-test
  "The tests of g2c's printer (bin/g2c-print-tests): the cases test/g2c/cases/NAME.clj,
  hand-written Go forms, each printed in the layouts gofmt and lines and checked against
  gofmt, against its original Go (NAME.go or the directory NAME/) by tools/gocmp, and in
  the lines layout by test/g2c/golines.

  A case's first lines may hold `;; g2c-test: KEY=VALUE ...`: levels=tree,export,code (the
  gocmp levels; none: no comparison), lines (compare lines too), error=TEXT (printing must
  fail with a message containing TEXT)."
  (:require [arbace.test :refer :all]
            [arbace.g2c.print :as print]
            [arbace.string :as str])
  (:import [java.io File]))

(def root (System/getProperty "g2c.root" "."))
(def work (System/getProperty "g2c.work" ".tmp/g2c-print-tests"))
(def goroot (System/getProperty "g2c.goroot" "/root/tamago-go"))
(def goarch (System/getProperty "g2c.goarch" "amd64"))
(def only (let [o (System/getProperty "g2c.only" "")] (when-not (str/blank? o) (re-pattern o))))

(defn- run
  "[exit-status output] of a command."
  [cmd env]
  (let [pb (ProcessBuilder. ^java.util.List (map str cmd))]
    (doseq [[k v] env] (.put (.environment pb) k v))
    (.redirectErrorStream pb true)
    (let [p (.start pb)
          out (slurp (.getInputStream p))]
      [(.waitFor p) out])))

(defn- case-options [^File f]
  (let [ls (take-while #(str/starts-with? % ";;") (str/split-lines (slurp f)))
        kvs (for [l ls
                  :when (str/starts-with? l ";; g2c-test:")
                  w (str/split (str/trim (subs l (count ";; g2c-test:"))) #"\s+")
                  :when (seq w)]
              (let [[k v] (str/split w #"=" 2)] [(keyword k) (or v true)]))]
    (into {} kvs)))

(defn cases []
  (let [dir (File. (str root "/test/g2c/cases"))]
    (for [^File f (sort (.listFiles dir))
          :let [n (.getName f)]
          :when (and (.isFile f) (str/ends-with? n ".clj"))
          :let [nm (subs n 0 (- (count n) 4))]
          :when (or (nil? only) (re-find only nm))]
      (let [go-file (File. dir (str nm ".go"))
            go-dir (File. dir nm)]
        {:name nm :forms f
         :original (let [o (:original (case-options f))]
                     (cond o (File. dir ^String o) (.isFile go-file) go-file (.isDirectory go-dir) go-dir))
         :opts (case-options f)}))))

(def gocmp-env {"GOROOT" goroot "GOOS" "tamago" "GOARCH" goarch "GOTOOLCHAIN" "local"
                "GOFLAGS" "" "GOWORK" "off" "CGO_ENABLED" "0"})

(defn check-case
  "Prints a case in both layouts and runs the checks; returns the results as data."
  [{:keys [forms original opts] :as c}]
  (try
    (let [levels (let [l (:levels opts "tree,export,code")] (when-not (= l "none") l))]
      (into {}
            (for [layout [:gofmt :lines]]
              (let [out (File. (str work "/" (:name c) "/" (name layout)))
                    _ (doseq [^File f (reverse (file-seq out))] (.delete f))
                    {:keys [files]} (print/print-package (.getPath ^File forms) (.getPath out) {:layout layout})
                    paths (map #(.getPath (File. out ^String %)) files)
                    [fs fo] (run (concat [(str goroot "/bin/gofmt") "-l"] paths) {})
                    single? (and original (.isFile ^File original))
                    b (if single? (first paths) (.getPath out))
                    cmp (when (and original levels)
                          (run [(str work "/bin/gocmp") "-levels" levels (.getPath ^File original) b] gocmp-env))
                    lines (when (and original (= layout :lines) (:lines opts) single?)
                            (run [(str work "/bin/golines") (.getPath ^File original) b] {}))]
                [layout {:files paths :gofmt [fs fo] :gocmp cmp :lines lines}]))))
    (catch Throwable t
      {:exception t})))

(deftest printer-cases
  (let [cs (vec (cases))
        results (vec (pmap check-case cs))]
    (is (pos? (count cs)) "no cases")
    (doseq [[c r] (map vector cs results)]
      (testing (:name c)
        (if-let [err (:error (:opts c))]
          (is (and (:exception r) (str/includes? (str (.getMessage ^Throwable (:exception r))) err))
              (str "printing fails with " err))
          (if-let [t (:exception r)]
            (is false (str "printing failed: " (.getMessage ^Throwable t)))
            (do
              (let [{[s o] :gofmt} (:gofmt r)]
                (is (zero? s) (str "gofmt layout parses: " o))
                (when-not (:nogofmt (:opts c))
                  (is (str/blank? o) (str "gofmt layout is gofmt's: " o))))
              (let [{[s o] :gofmt} (:lines r)]
                (is (zero? s) (str "lines layout parses: " o)))
              (doseq [layout [:gofmt :lines]]
                (when-let [[s o] (:gocmp (get r layout))]
                  (is (zero? s) (str (name layout) " layout, gocmp:\n" o))))
              (when-let [[s o] (:lines (:lines r))]
                (is (zero? s) (str "lines layout, golines: " o))))))))))

(deftest package-build
  ;; SPEC §12.4: the non-Go files copied (embedded ones checked by hash), the overlay, and
  ;; go build -overlay under the forms' configuration
  (when (or (nil? only) (re-find only "s09_directives"))
    (let [src (File. (str root "/test/g2c/cases/s09_directives"))
          out (File. (str work "/package-build/out"))
          _ (doseq [^File f (reverse (file-seq (.getParentFile out)))] (.delete f))
          {:keys [package files]} (print/print-package (str root "/test/g2c/cases/s09_directives.clj")
                                                       (.getPath out) {})]
      (print/copy-files package src out files)
      (doseq [f ["dirs.go" "asm_amd64.s" "a.txt" "b.txt" "overlay.json"]]
        (is (.isFile (File. out ^String f)) (str f " written")))
      (is (str/includes? (slurp (File. out "overlay.json"))
                         (str (.getPath (File. (.getAbsoluteFile src) "dirs.go")) "\": \""
                              (.getPath (File. (.getAbsoluteFile out) "dirs.go")))))
      (let [[status text] (print/build package src out goroot)]
        (is (zero? status) (str "go build -overlay: " text)))
      (let [bad (assoc-in package [:package :embed-files] [["a.txt" "00"]])]
        (is (thrown-with-msg? Exception #"embedded file a.txt" (print/copy-files bad src out files)))))))
