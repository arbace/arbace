(ns arbace.g2c.print
  "g2c's printer: Go forms (doc/go/SPEC.md) to Go source, milestone G2
  (doc/go/PRINTER-NOTES.md).

  bin/g2c-print [OPTIONS] FORMS.clj OUTDIR
    Reads the package file FORMS.clj (its ns, go/package and load forms, and the files it
    loads; or one file holding go/file forms and declarations) with *read-eval* false and
    writes one Go file per go/file into OUTDIR.
    --layout lines|gofmt  lines (default when the forms carry lines): each form on its
                          recorded line (§10.1, §12.3); gofmt: gofmt's layout, lines ignored
    --no-line-directives  never write //line or /*line*/ directives for lines layout cannot
                          reach
    --src DIR             the original package directory: copies :other-files and
                          :embed-files (hashes checked) into OUTDIR, and writes
                          OUTDIR/overlay.json mapping DIR's Go files to the printed ones
    --build               then compiles the package as §12.4 says: the toolchain of
                          $G2C_GOROOT (default /root/tamago-go), the :config's environment,
                          -trimpath, go build -overlay, in DIR
    --gofmt               run each printed file through $G2C_GOROOT/bin/gofmt

  The library: print-package, print-forms (forms in memory to {name text}), print-file."
  (:require [arbace.g2c.print-code :as code]
            [arbace.g2c.print-emit :as e]
            [arbace.string :as str])
  (:import [java.io File FileInputStream InputStreamReader]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files CopyOption StandardCopyOption]
           [java.security MessageDigest]
           [arbace.lang LineNumberingPushbackReader]))

;; ---------------------------------------------------------------------------------------
;; Reading (§12.1)

(defn read-forms
  "The forms of a file, read with line numbers and *read-eval* false."
  [path]
  (with-open [r (LineNumberingPushbackReader.
                  (InputStreamReader. (FileInputStream. (str path)) StandardCharsets/UTF_8))]
    (binding [*read-eval* false]
      (let [eof (Object.)]
        (loop [acc []]
          (let [f (read r false eof)]
            (if (identical? f eof) acc (recur (conj acc f)))))))))

(defn- go-ns-aliases
  "The qualifiers naming arbace.go in an ns form's :require."
  [ns-form]
  (into #{"go" "arbace.go"}
        (for [clause (rest ns-form)
              :when (and (seq? clause) (= :require (first clause)))
              spec (rest clause)
              :when (and (vector? spec) (= 'arbace.go (first spec)))
              :let [m (apply hash-map (rest spec))]
              :when (:as m)]
          (name (:as m)))))

(defn- top-op [f]
  (binding [code/*go-ns* code/*go-ns*] (code/op f)))

(defn collect
  "The package of a forms file: {:package {opts} :name sym :files [{:file form :decls [...]}]},
  following load forms (relative to the loading file's directory, or with a leading / to
  the root the ns name implies)."
  [path]
  (let [state (volatile! {:package nil :files [] :aliases #{"go" "arbace.go"}})
        root (volatile! nil)]
    (letfn [(walk [^File file]
              (doseq [f (read-forms file)]
                (binding [code/*go-ns* (:aliases @state)]
                  (let [o (when (seq? f) (code/op f))]
                    (cond
                      (= o "ns")
                      (let [nsname (str (second f))
                            depth (count (re-seq #"\." nsname))]
                        (vswap! state update :aliases into (go-ns-aliases f))
                        (when-not @root
                          (vreset! root (nth (iterate #(.getParentFile ^File %) (.getAbsoluteFile file))
                                             (inc depth)))))
                      (= o "in-ns") nil
                      (= o "load")
                      (doseq [p (rest f)]
                        (let [p (str p)
                              target (if (str/starts-with? p "/")
                                       (File. ^File @root (str (subs p 1) ".clj"))
                                       (File. (.getParentFile (.getAbsoluteFile file)) (str p ".clj")))]
                          (walk target)))
                      (= o "go/package")
                      (let [[_ nm & opts] f]
                        (vswap! state assoc :package (apply hash-map opts) :name nm))
                      (= o "go/file")
                      (let [[_ nm & opts] f]
                        (vswap! state update :files conj {:name nm :file f :opts (apply hash-map opts)
                                                          :decls []}))
                      (and (seq? f) (str/starts-with? (str o) "go/"))
                      (if (empty? (:files @state))
                        (throw (ex-info (str "a declaration before any go/file: " (pr-str f)) {:form f}))
                        (vswap! state update :files
                                (fn [fs] (conj (pop fs) (update (peek fs) :decls conj f)))))
                      (nil? f) nil
                      :else (throw (ex-info (str "not a Go forms top-level form: " (pr-str f)) {:form f})))))))]
      (walk (File. (str path)))
      @state)))

;; ---------------------------------------------------------------------------------------
;; Files (§4.3, §12.2)

(defn- import-spec [v]
  (let [[nm path] v
        explicit? (or (:alias (meta v)) (#{"_" "."} (str nm)))]
    (when explicit? (e/tok (str nm)) (e/sp))
    (e/tok (arbace.g2c.print-text/go-string path))))

(defn- write-imports [imports]
  (when (seq imports)
    (e/nl 0)
    (if (= 1 (count imports))
      (do (e/tok "import") (e/sp) (import-spec (first imports)) (e/nl 0))
      (do (e/tok "import") (e/sp) (e/tok "(")
          (e/nl 1)
          (loop [[v & more] imports prev nil]
            (when v
              ;; a path sorting before the previous one starts a block of its own, which
              ;; gofmt's import sorting then leaves in place
              (when (and prev (neg? (compare (str (second v)) (str (second prev)))))
                (e/nl 1))
              (import-spec v)
              (e/nl (if more 1 0))
              (recur more v)))
          (e/tok ")") (e/nl 0)))))

(defn print-file
  "The Go text of one file: {:name :opts :decls} as collect gives it. pkg-name is the
  package clause's name unless the file's :package says otherwise."
  [{:keys [opts decls file]} pkg-name {:keys [layout line-directives?] :as popts}]
  (let [lines? (case layout :lines true :gofmt false
                 (boolean (:line (meta file))))]
    (binding [e/*p* (e/new-state {:lines? lines? :line-directives? (if (nil? line-directives?) true line-directives?)})
              e/*next* nil]
      (doseq [b (:build opts)] (e/comment-line (str b) 0))
      (when (seq (:build opts)) (e/nl 0))
      (doseq [d (:directives opts)] (e/comment-line (str d) 0))
      (when (seq (:directives opts)) (e/nl 0))
      (code/write-doc (:doc opts) 0)
      (e/tok "package") (e/sp) (e/tok (str (or (:package opts) pkg-name)))
      (e/nl 0)
      (write-imports (:imports opts))
      (let [decls (vec decls)]
        (dotimes [i (count decls)]
          (let [f (nth decls i)
                t (e/target f)
                directive? (= (code/op f) "go/directive")
                [doc dirs] (if directive? [nil []] (code/decl-doc-and-directives f))
                pre (code/pre-lines doc dirs)]
            (binding [e/*next* (some (fn [d] (when-let [t (e/target d)]
                                               (let [[doc dirs] (if (= (code/op d) "go/directive")
                                                                  [nil []]
                                                                  (code/decl-doc-and-directives d))]
                                                 (- (long t) (code/pre-lines doc dirs)))))
                                     (subvec decls (inc i)))]
              ;; form feeds: a top-level line never aligns with the lines before it
              (cond
                (not lines?) (do (when-not (e/bol?) (e/nl 0 true)) (e/nl 0 true))
                (nil? t) (do (when-not (e/bol?) (e/nl 0 true)) (e/nl 0 true))
                :else (let [want (- (long t) pre)]
                        (when-not (e/bol?) (e/nl 0 true))
                        (while (< (e/line) want) (e/nl 0 true))))
              (if directive?
                (e/comment-line (str (second f)) 0)
                (do
                  (code/write-doc doc 0)
                  (doseq [d dirs] (e/comment-line d 0))
                  (when (and lines? t (not= (long t) (e/line)) (:line-directives? (or popts {}) true))
                    (cond
                      (> (long t) (e/line)) (while (< (e/line) (long t)) (e/nl 0))
                      :else (e/line-directive-own t)))
                  (code/top-decl f)))))))
      (when-not (e/bol?) (e/nl 0))
      (e/result))))

(defn print-forms
  "The Go files of a collected package: {name text}."
  [pkg opts]
  (into (sorted-map)
        (for [fl (:files pkg)]
          [(str (:name fl)) (print-file fl (:name pkg) opts)])))

;; ---------------------------------------------------------------------------------------
;; The package build (§12.4)

(defn- sha256 [^File f]
  (let [md (MessageDigest/getInstance "SHA-256")
        bs (Files/readAllBytes (.toPath f))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md bs)))))

(defn- copy-file [^File from ^File to]
  (.mkdirs (.getParentFile to))
  (Files/copy (.toPath from) (.toPath to)
              ^"[Ljava.nio.file.CopyOption;" (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn- json-string [s]
  (str "\"" (-> (str s) (str/replace "\\" "\\\\") (str/replace "\"" "\\\"")) "\""))

(defn copy-files
  "Copies the package's :other-files and :embed-files (checking the hashes) from src to
  out; writes out/overlay.json for go build -overlay (src's Go files to out's)."
  [pkg ^File src ^File out written]
  (let [opts (:package pkg)]
    (doseq [f (:other-files opts)]
      (copy-file (File. src (str f)) (File. out (str f))))
    (doseq [[f h] (:embed-files opts)]
      (let [from (File. src (str f))
            got (sha256 from)]
        (when (and h (not= got (str h)))
          (throw (ex-info (str "embedded file " f ": sha256 " got ", the forms say " h) {:file f})))
        (copy-file from (File. out (str f)))))
    (spit (File. out "overlay.json")
          (str "{\"Replace\": {\n"
               (str/join ",\n" (for [n written]
                                 (str "  " (json-string (.getPath (File. (.getAbsoluteFile src) (str n))))
                                      ": " (json-string (.getPath (File. (.getAbsoluteFile out) (str n)))))))
               "\n}}\n"))))

(defn build-env
  "The environment of the package's configuration (§4.2, §12.4)."
  [config goroot]
  (cond-> {"GOROOT" goroot "GOTOOLCHAIN" "local" "GOFLAGS" "" "GOWORK" "off"
           "CGO_ENABLED" (if (:cgo config) "1" "0")}
    (:goos config) (assoc "GOOS" (:goos config))
    (:goarch config) (assoc "GOARCH" (:goarch config))
    (:goamd64 config) (assoc "GOAMD64" (:goamd64 config))
    (:goarm64 config) (assoc "GOARM64" (:goarm64 config))
    (some? (:goexperiment config)) (assoc "GOEXPERIMENT" (:goexperiment config))))

(defn- run [cmd ^File dir env]
  (let [pb (ProcessBuilder. ^java.util.List (map str cmd))]
    (.directory pb dir)
    (let [penv (.environment pb)]
      (doseq [[k v] env] (.put penv k v)))
    (.redirectErrorStream pb true)
    (let [p (.start pb)
          out (slurp (.getInputStream p))]
      [(.waitFor p) out])))

(defn build
  "go build -trimpath -overlay out/overlay.json in src, under the package's configuration."
  [pkg ^File src ^File out goroot]
  (let [config (:config (:package pkg))
        tags (:tags config)
        cmd (concat [(str goroot "/bin/go") "build" "-trimpath" "-overlay"
                     (.getPath (File. (.getAbsoluteFile out) "overlay.json"))]
                    (when (seq tags) ["-tags" (str/join "," tags)])
                    ["."])]
    (run cmd src (build-env config goroot))))

(defn print-package
  "Prints the package of the forms file at path into out-dir; returns the file names."
  [path out-dir opts]
  (let [pkg (collect path)
        out (File. (str out-dir))
        layout (:layout opts)
        files (print-forms pkg (assoc opts :layout (or layout (when (= :full (:positions (:package pkg))) :lines))))]
    (.mkdirs out)
    (doseq [[n text] files]
      (spit (File. out ^String n) text))
    {:package pkg :files (keys files)}))

(defn- gofmt-file [goroot ^File f]
  (let [[status out] (run [(str goroot "/bin/gofmt") "-w" (.getPath f)] (.getParentFile (.getAbsoluteFile f)) {})]
    (when-not (zero? status)
      (throw (ex-info (str "gofmt " f ": " out) {:file f})))))

(defn -main [& argv]
  (let [goroot (or (System/getenv "G2C_GOROOT") "/root/tamago-go")]
    (loop [as argv opts {}]
      (case (first as)
        "--layout" (recur (nnext as) (assoc opts :layout (keyword (second as))))
        "--no-line-directives" (recur (next as) (assoc opts :line-directives? false))
        "--src" (recur (nnext as) (assoc opts :src (second as)))
        "--build" (recur (next as) (assoc opts :build true))
        "--gofmt" (recur (next as) (assoc opts :gofmt true))
        (let [[forms out] as]
          (when-not (and forms out (= 2 (count as)))
            (println "usage: bin/g2c-print [--layout lines|gofmt] [--no-line-directives] [--src DIR [--build]] [--gofmt] FORMS.clj OUTDIR")
            (System/exit 2))
          (let [{:keys [package files]} (print-package forms out opts)
                outf (File. ^String out)]
            (doseq [f files] (println (str out "/" f)))
            (when (:gofmt opts)
              (doseq [f files] (gofmt-file goroot (File. outf ^String f))))
            (when-let [src (:src opts)]
              (copy-files package (File. ^String src) outf files)
              (when (:build opts)
                (let [[status text] (build package (File. ^String src) outf goroot)]
                  (print text)
                  (flush)
                  (when-not (zero? status) (System/exit 1)))))
            (shutdown-agents)))))))
