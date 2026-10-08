(ns arbace.g2c.build
  "g2c's program build: a Go program held as Go forms (doc/go/SPEC.md) to a static Linux
  executable (doc/go/BUILD.md).

  bin/g2c build [--arch amd64|arm64] [-o OUT] [--layout gofmt|lines] [--line-file]
                [--work DIR] [--overlay FILE] DIR
  bin/g2c build --print-only --work DIR [--tests] [--module PATH] [--layout gofmt|lines]
                [--line-file] SRC
  runs -m arbace.g2c.build with the same arguments. DIR holds the program's packages as forms
  under DIR/go/ (SPEC §4.1: go/<path>.clj, the package file, and the files it loads), and
  optionally DIR/program.edn, {:module \"path\" :go \"1.N\"} (go.mod's module and go lines; by
  default the main package's path and the toolchain's language version). Each package is
  printed (arbace.g2c.print) into a temporary module, its other and embedded files copied
  from its forms directory (go/<path>/), and the main package is built with

    CGO_ENABLED=0 GOOS=linux GOARCH=<arch> go build -trimpath -buildvcs=false -o OUT

  into a static executable. OUT defaults to the main package path's last element in the
  current directory. The toolchain is $G2C_GOROOT/bin/go (default /root/tamago-go, go1.27.1).
    --arch        amd64 (default) or arm64 (GOAMD64=v1, GOARM64=v8.0)
    --layout      the printed Go's layout: gofmt (default) or lines (SPEC §12.3)
    --line-file   layout lines, each file starting with a //line directive naming its forms
                  file, so that gc's positions (panics' tracebacks) name the forms files and
                  lines: <package path>/<file>.clj for DIR/go/<path>/<file>.clj
    --work DIR    print the module into DIR (kept); by default a temporary directory, removed
                  after the build
    --overlay FILE  passed to go build as -overlay FILE (absolute): a JSON file replacing
                  files of the build, such as patched Go runtime files (BUILD.md, \"Overlays\")
    --print-only  print the module into --work DIR/mod and stop: no build, and no main
                  package required (a library such as jrt, built and tested by bin/jrt)
    --tests       also print the packages' :test-files (SPEC §4.2), for go test
    --module PATH the module path when SRC has no program.edn (a library has no main
                  package to take it from)"
  (:require [arbace.g2c.print :as p]
            [arbace.string :as str])
  (:import [java.io File]
           [java.nio.file Files CopyOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(defn- fail [msg & [data]]
  (throw (ex-info msg (merge {::user true} data))))

;; ---------------------------------------------------------------------------------------
;; The program: its packages

(defn- clj-files [^File dir]
  (->> (file-seq dir)
       (filter #(and (.isFile ^File %) (str/ends-with? (.getName ^File %) ".clj")))
       (sort-by #(.getPath ^File %))))

(defn- package-file?
  "Whether a forms file is a package file: it holds an ns form and a go/package form."
  [^File f]
  (let [forms (p/read-forms f)
        ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))
        aliases (when ns-form (#'p/go-ns-aliases ns-form))]
    (boolean
      (and ns-form
           (some #(and (seq? %) (symbol? (first %))
                       (= "package" (name (first %)))
                       (contains? aliases (namespace (first %))))
                 forms)))))

(defn- rel-path [^File base ^File f]
  (str (.relativize (.toPath (.getAbsoluteFile base)) (.toPath (.getAbsoluteFile f)))))

(defn packages
  "The program's packages: the package files under DIR/go, each collected (arbace.g2c.print
  /collect) with :file (the package file) and :path."
  [^File dir]
  (let [root (File. dir "go")]
    (when-not (.isDirectory root)
      (fail (str "no " (.getPath root) ": the program's forms go under DIR/go (doc/go/BUILD.md)")))
    (let [pkgs (for [f (clj-files root) :when (package-file? f)]
                 (let [pkg (p/collect f)
                       path (:path (:package pkg))]
                   (when-not (string? path)
                     (fail (str (rel-path dir f) ": go/package without :path")))
                   (assoc pkg :file f :path path)))]
      (when (empty? pkgs)
        (fail (str "no package files (ns and go/package) under " (.getPath root))))
      (doseq [[path ps] (group-by :path pkgs) :when (> (count ps) 1)]
        (fail (str "two package files for " path ": "
                   (str/join ", " (map #(rel-path dir (:file %)) ps)))))
      (vec pkgs))))

(defn- read-program-edn [^File dir]
  (let [f (File. dir "program.edn")]
    (if (.isFile f)
      (let [forms (p/read-forms f)]
        (when-not (and (= 1 (count forms)) (map? (first forms)))
          (fail "program.edn must hold one map, {:module \"path\" :go \"1.N\"}"))
        (first forms))
      {})))

(defn- run
  "Runs cmd in dir with env added to the JVM's; [exit output] with output captured, or
  [exit nil] with the child's output on the JVM's when inherit?."
  [cmd ^File dir env inherit?]
  (let [pb (ProcessBuilder. ^java.util.List (map str cmd))]
    (.directory pb dir)
    (let [penv (.environment pb)]
      (doseq [[k v] env] (.put penv k v)))
    (if inherit?
      (do (.inheritIO pb)
          [(.waitFor (.start pb)) nil])
      (do (.redirectErrorStream pb true)
          (let [proc (.start pb)
                out (slurp (.getInputStream proc))]
            [(.waitFor proc) out])))))

(defn go-env
  "The build's environment: the toolchain, linux/ARCH, no cgo, nothing from the user's Go
  environment that could change the output."
  [goroot arch]
  (merge {"GOROOT" goroot "GOTOOLCHAIN" "local" "GOFLAGS" "" "GOWORK" "off"
          "GO111MODULE" "on" "GOPROXY" "off" "GOEXPERIMENT" "" "CGO_ENABLED" "0"
          "GOOS" "linux" "GOARCH" arch}
         (case arch
           "amd64" {"GOAMD64" "v1"}
           "arm64" {"GOARM64" "v8.0"})))

(defn- toolchain-lang
  "The toolchain's language version, 1.N (from go env GOVERSION)."
  [goroot]
  (let [[status out] (run [(str goroot "/bin/go") "env" "GOVERSION"] (File. ".")
                          {"GOROOT" goroot "GOTOOLCHAIN" "local"} false)
        v (str/trim (str out))]
    (if-let [[_ l] (and (zero? status) (re-find #"^go(\d+\.\d+)" v))]
      l
      (fail (str "go env GOVERSION: " v)))))

(defn program
  "The program in DIR: {:module :go :main pkg :packages [pkg ...]}, every package with :dir,
  its directory in the module (\"\" for the module's root). With :library? (opts), no main
  package is required and :module (opts) gives the module path when program.edn does not."
  ([^File dir goroot] (program dir goroot {}))
  ([^File dir goroot {:keys [library?] :as opts}]
  (let [pkgs (packages dir)
        mains (filter #(= "main" (str (:name %))) pkgs)
        _ (when-not (or (= 1 (count mains)) (and library? (empty? mains)))
            (fail (str "a program has one main package; found " (count mains)
                       (when (seq mains) (str ": " (str/join ", " (map :path mains)))))))
        main (first mains)
        edn (read-program-edn dir)
        module (or (:module edn) (:module opts) (:path main)
                   (fail "no module path: give program.edn's :module or --module"))
        golang (or (:go edn) (toolchain-lang goroot))
        pkgs (vec (for [pkg pkgs]
                    (let [path (:path pkg)]
                      (cond
                        (= path module) (assoc pkg :dir "")
                        (str/starts-with? path (str module "/"))
                        (assoc pkg :dir (subs path (inc (count module))))
                        :else (fail (str "package " path " is not in the module " module))))))]
    {:module module :go (str golang) :packages pkgs
     :main (first (filter #(= "main" (str (:name %))) pkgs))})))

;; ---------------------------------------------------------------------------------------
;; Printing the module

(defn- sha256 [^File f]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md (Files/readAllBytes (.toPath f)))))))

(defn- copy-file [^File from ^File to]
  (.mkdirs (.getParentFile to))
  (Files/copy (.toPath from) (.toPath to)
              ^"[Ljava.nio.file.CopyOption;" (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn- forms-dir
  "The package's forms directory: its package file without .clj (SPEC §4.1), where its
  other and embedded files are."
  ^File [pkg]
  (let [^File f (:file pkg)
        n (.getName f)]
    (File. (.getParentFile f) (subs n 0 (- (count n) 4)))))

(defn- with-line-files
  "The package's files with :src for --line-file: the forms file's path relative to the
  package's forms directory (go/<path>/), which gc resolves in the package's directory, so
  that -trimpath names it <package path>/<file>.clj, wherever DIR is. (A path outside the
  package's directory would escape -trimpath's rewriting and keep the host's path.)"
  [pkg]
  (let [fdir (forms-dir pkg)]
    (update pkg :files (fn [fs] (mapv #(update % :src (fn [s] (rel-path fdir (File. (str s))))) fs)))))

(defn print-module
  "Prints the program into the module directory out: go.mod, and per package its Go files
  and the other and embedded files it names. Returns the printed files, relative to out."
  [{:keys [module packages] golang :go} ^File dir ^File out {:keys [layout line-file tests]}]
  (.mkdirs out)
  (spit (File. out "go.mod") (str "module " module "\n\ngo " golang "\n"))
  (vec
    (cons "go.mod"
          (mapcat
            (fn [pkg]
              (let [pdir (if (= "" (:dir pkg)) out (File. out ^String (:dir pkg)))
                    opts (cond-> {:layout (or layout (if line-file :lines :gofmt))}
                           line-file (assoc :line-file true))
                    test-files (set (map str (:test-files (:package pkg))))
                    pkg (if tests
                          pkg
                          (update pkg :files (fn [fs] (filterv #(not (test-files (str (:name %)))) fs))))
                    texts (p/print-forms (cond-> pkg line-file with-line-files) opts)
                    declared (when-let [fs (:files (:package pkg))]
                               (concat fs (when tests (:test-files (:package pkg)))))
                    rel #(if (= "" (:dir pkg)) % (str (:dir pkg) "/" %))
                    src (forms-dir pkg)]
                (when (and declared (not= (set (map str declared)) (set (keys texts))))
                  (fail (str (:path pkg) ": go/package :files " (pr-str declared)
                             " but go/file forms " (pr-str (vec (keys texts))))))
                (.mkdirs pdir)
                (doseq [[n text] texts]
                  (spit (File. pdir ^String n) text))
                (doseq [f (:other-files (:package pkg))]
                  (copy-file (File. src (str f)) (File. pdir (str f))))
                (doseq [[f h] (:embed-files (:package pkg))]
                  (let [from (File. src (str f))]
                    (when-not (.isFile from)
                      (fail (str (:path pkg) ": no embedded file " (rel-path dir from))))
                    (when (and h (not= (sha256 from) (str h)))
                      (fail (str (:path pkg) ": embedded file " f " has sha256 " (sha256 from)
                                 ", the forms say " h)))
                    (copy-file from (File. pdir (str f)))))
                (map rel (concat (keys texts) (map str (:other-files (:package pkg)))
                                 (map (comp str first) (:embed-files (:package pkg)))))))
            packages))))

;; ---------------------------------------------------------------------------------------
;; The build

(defn- delete-tree [^File f]
  (when (.isDirectory f)
    (doseq [c (.listFiles f)] (delete-tree c)))
  (.delete f))

(defn build
  "Builds the program in dir for linux/arch into out (a File); returns go build's exit
  status, its output on the JVM's."
  [^File dir arch ^File out {:keys [work goroot] :as opts}]
  (let [prog (program dir goroot)
        main (:main prog)
        config (:config (:package main))
        tmp? (nil? work)
        ^File work (if work
                     (File. (str work))
                     (.toFile (Files/createTempDirectory "g2c-build" (make-array FileAttribute 0))))]
    (try
      (let [mod (File. work "mod")]
        (when (.exists mod) (delete-tree mod))
        (print-module prog dir mod opts)
        (let [tags (:tags config)
              cmd (concat [(str goroot "/bin/go") "build" "-trimpath" "-buildvcs=false"]
                          (when (seq tags) ["-tags" (str/join "," tags)])
                          (when-let [o (:overlay opts)]
                            ["-overlay" (.getPath (.getAbsoluteFile (File. (str o))))])
                          ["-o" (.getPath (.getAbsoluteFile out)) (:path main)])
              env (cond-> (go-env goroot arch)
                    (seq (:goexperiment config)) (assoc "GOEXPERIMENT" (:goexperiment config)))
              [status] (run cmd mod env true)]
          status))
      (finally
        (when tmp? (delete-tree work))))))

(defn -main [& argv]
  (let [goroot (or (System/getenv "G2C_GOROOT") "/root/tamago-go")
        usage (str "usage: bin/g2c build [--arch amd64|arm64] [-o OUT] [--layout gofmt|lines] [--line-file] [--work DIR] [--overlay FILE] DIR\n"
                   "       bin/g2c build --print-only --work DIR [--tests] [--module PATH] [--layout gofmt|lines] [--line-file] SRC")]
    (loop [as argv opts {:arch "amd64" :goroot goroot}]
      (case (first as)
        "--arch" (recur (nnext as) (assoc opts :arch (second as)))
        "-o" (recur (nnext as) (assoc opts :out (second as)))
        "--layout" (recur (nnext as) (assoc opts :layout (keyword (second as))))
        "--line-file" (recur (next as) (assoc opts :line-file true))
        "--work" (recur (nnext as) (assoc opts :work (second as)))
        "--overlay" (recur (nnext as) (assoc opts :overlay (second as)))
        "--print-only" (recur (next as) (assoc opts :print-only true))
        "--tests" (recur (next as) (assoc opts :tests true))
        "--module" (recur (nnext as) (assoc opts :module (second as)))
        (do
          (when-not (and (= 1 (count as)) (not (str/starts-with? (first as) "-"))
                         (#{"amd64" "arm64"} (:arch opts))
                         (contains? #{nil :gofmt :lines} (:layout opts))
                         (or (not (:print-only opts)) (:work opts)))
            (binding [*out* *err*] (println usage))
            (System/exit 2))
          (let [dir (File. ^String (first as))
                status (try
                         (if (:print-only opts)
                           (let [prog (program dir goroot {:library? true :module (:module opts)})
                                 mod (File. (File. (str (:work opts))) "mod")]
                             (when (.exists mod) (delete-tree mod))
                             (print-module prog dir mod opts)
                             0)
                         (let [prog-main (when-not (:out opts) (:main (program dir goroot)))
                               out (File. ^String (or (:out opts)
                                                      (last (str/split (:path prog-main) #"/"))))]
                           (build dir (:arch opts) out opts)))
                         (catch arbace.lang.ExceptionInfo e
                           (if (::user (ex-data e))
                             (do (binding [*out* *err*] (println (str "g2c build: " (.getMessage e))))
                                 1)
                             (throw e))))]
            (shutdown-agents)
            (System/exit status)))))))
