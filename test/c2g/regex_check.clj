(ns c2g.regex-check
  "bin/c2g-regex: the oracle's regex corpus (test/oracle/regex, recorded in
  test/oracle/expected/regex, doc/go/ORACLE.md) run by java.util.regex as c2g translates it
  from the JDK closure (doc/go/C2G-NOTES.md, phase 2C). c2g translates the closure with
  test/c2g/regex/RegexCheck.clj (what oracle.driver/regex does, for one case, returning the
  result as canonical text) as an input and its run method as the root; a Go main program
  reads the cases (one escaped line each), calls RegexCheck.run per case and prints the text;
  this namespace prints each recorded result in the same canonical text and compares, per
  case (pattern and flag set) and per input.

  The canonical text: maps with their keys in the recorder's order (:named's sorted), vectors
  in brackets, strings in double quotes with every character outside printable ASCII, and \"
  and \\, as \\uXXXX (lower-case hex), keywords, numbers, booleans and nil as Clojure prints
  them, separated by single spaces."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.c2g.main :as c2g]
            [arbace.c2g.out :as out]
            [arbace.c2g.world :as w]))

(def flag-bits
  "oracle.driver's regex-flags."
  {:d 1 :i 2 :x 4 :m 8 :literal 16 :s 32 :u 64 :canon-eq 128 :U 256})

(defn esc
  "s with every character outside printable ASCII, and \" and \\, as \\uXXXX."
  [^String s]
  (apply str (map (fn [c] (let [i (int c)]
                            (if (and (<= 32 i 126) (not= c \") (not= c \\))
                              c
                              (format "\\u%04x" i))))
                  s)))

(def key-order [:groups :named :results :error :matches :hit-end :looking-at :find :split :split-1
                :match-groups :replace :ex])

(defn canon [x]
  (cond
    (nil? x) "nil"
    (string? x) (str "\"" (esc x) "\"")
    (map? x) (str "{" (str/join " " (for [[k v] (if (every? keyword? (keys x))
                                                   (sort-by #(.indexOf ^java.util.List key-order (key %)) x)
                                                   (sort-by key x))]
                                      (str (canon k) " " (canon v))))
                  "}")
    (sequential? x) (str "[" (str/join " " (map canon x)) "]")
    :else (str x)))

(defn expected-text [c]
  (canon (if (:error c)
           {:error (:error c)}
           {:groups (:groups c) :named (:named c) :results (:results c)})))

(defn case-line
  "The fields of case c for RegexCheck.run: pattern, flags, the number of inputs, inputs,
  replacements, escaped (\\uXXXX) and separated by tabs."
  [c]
  (str/join "\t" (concat [(esc (:pattern c)) (reduce + 0 (map flag-bits (:flags c))) (count (:inputs c))]
                         (map esc (:inputs c)) (map esc (:replace c)))))

(def main-forms
  '[(go/func runCase [^string line] :results [^string out]
      (defer ((fn []
                (let [r (recover)]
                  (when (!= r nil)
                    (set! out (+ "{:go-panic " (fmt/Sprint r) "}")))))))
      (let [exc ((fn [] :results [^jrt/Throwable_I exc]
                   (defer (jrt/Catch (addr exc)))
                   (set! out (.String (lang/RegexCheck_Run_String__String (jrt/Str line))))
                   (return)))]
        (when (!= exc nil)
          (set! out (+ "{:uncaught " (.GoName (jrt/GetClass exc)) " " (.String (.GetMessage__String exc)) "}"))))
      (return))
    (go/func main []
      (let [(values f _) (os/Open (aget os/Args 1))
            sc (bufio/NewScanner f)]
        (.Buffer sc (make (slice byte) (* 1024 1024)) (* 64 1024 1024))
        (while (.Scan sc)
          (let [line (.Text sc)
                i (strings/Index line "\t")]
            (fmt/Printf "@@rx %s %s\n" (subslice line 0 i) (runCase (subslice line (+ i 1))))))))])

(defn write-program! [prog-dir]
  (let [dir (str prog-dir "/go/arbace/cmd")]
    (io/make-parents (io/file dir "regexcheck" "x"))
    (spit (str dir "/regexcheck.clj")
          (str (out/form-text '(ns go.arbace.cmd.regexcheck (:require [arbace.go :as go]))) "\n"
               (out/form-text '(go/package main :path "arbace/cmd/regexcheck" :files ["main.go"])) "\n"
               (out/form-text '(load "regexcheck/main")) "\n"))
    (spit (str dir "/regexcheck/main.clj")
          (str "(in-ns 'go.arbace.cmd.regexcheck)\n"
               (out/form-text '(go/file "main.go" :imports [[bufio "bufio"] [fmt "fmt"] [os "os"] [strings "strings"]
                                                            [jrt "arbace/jrt"] [lang "arbace/lang"]]))
               "\n"
               (str/join "\n" (map out/form-text main-forms))
               "\n"))))

(def ^:dynamic *env*
  "Environment variables added to the processes sh starts."
  {})

(defn sh [& args]
  (let [pb (ProcessBuilder. ^java.util.List (map str args))
        _ (doseq [[k v] *env*] (.put (.environment pb) k v))
        _ (.redirectErrorStream pb true)
        p (.start pb)
        o (slurp (.getInputStream p))]
    [(.waitFor p) o]))

(defn- parse-text
  "The Go program's text of a result, read back (the canonical text is Clojure data)."
  [s]
  (try (binding [*read-eval* false] (read-string s)) (catch Throwable _ nil)))

(defn -main [& args]
  (let [arches (or (some-> (System/getenv "C2G_ARCH") (str/split #",")) ["amd64"])
        out-dir (or (System/getenv "C2G_OUT") ".tmp/c2g/regex")
        files (sort (map #(.getPath ^java.io.File %)
                         (filter #(str/ends-with? (.getName ^java.io.File %) ".edn")
                                 (.listFiles (io/file "test/oracle/expected/regex")))))
        files (if (seq args) (filter (fn [f] (some #(str/includes? f %) args)) files) files)
        cases (vec (for [f files
                         :let [x (first (w/read-forms f))
                               fname (str/replace (.getName (io/file f)) ".edn" "")]
                         c (:cases x)]
                     (assoc c ::file fname ::id (str fname ":" (:i c)))))
        cases-file (str out-dir "/cases.txt")
        opts (c2g/parse-args ["--out" out-dir "--input" "test/c2g/regex" "--root" "c2g/regex/RegexCheck#run"])]
    (io/make-parents (io/file cases-file))
    (spit cases-file (apply str (for [c cases] (str (::id c) "\t" (case-line c) "\n"))))
    (c2g/run (assoc opts :after (fn [{:keys [prog-dir]}] (write-program! prog-dir))))
    (let [overlay (str/trim (second (sh "bin/jrt" "overlay")))
          summary (atom {})]
      (doseq [arch arches]
        (let [exe (str out-dir "/bin/" arch "/regexcheck")
              _ (io/make-parents (io/file exe))
              [code bo] (sh "bin/g2c" "build" "--arch" arch "--work" (str out-dir "/work-" arch) "-o" exe
                            "--overlay" overlay (str out-dir "/prog"))]
          (if-not (zero? code)
            (do (println (str "c2g-regex: build failed (" arch ")"))
                (println (str/join "\n" (take 30 (remove #(str/includes? % "WARNING") (str/split-lines bo)))))
                (shutdown-agents)
                (System/exit 1))
            (let [t0 (System/nanoTime)
                  ;; the JDK's resource data (\N{name}, CANON_EQ) from bin/jrt-convert's data
                  ;; directory, which the program finds as ARBACE_PATH's resources (it embeds none)
                  [rc ro] (binding [*env* {"ARBACE_PATH" (.getAbsolutePath (io/file ".tmp/jrt/data"))}]
                            (if (= arch "arm64") (sh "/usr/bin/qemu-aarch64" exe cases-file) (sh exe cases-file)))
                  secs (/ (Math/round (/ (- (System/nanoTime) t0) 1e7)) 100.0)
                  got (into {} (for [l (str/split-lines ro)
                                     :when (str/starts-with? l "@@rx ")
                                     :let [[_ id text] (str/split l #" " 3)]]
                                 [id text]))
                  per-file (atom (sorted-map))
                  fails (atom [])]
              (doseq [c cases
                      :let [want (expected-text c)
                            text (get got (::id c))
                            ok (= want text)
                            g (when (and text (not ok)) (parse-text text))
                            n (count (:inputs c))
                            ;; inputs: each result compared on its own (an :error case counts all
                            ;; its inputs together)
                            in-ok (cond ok n
                                        (or (:error c) (nil? g) (:error g)) 0
                                        :else (count (filter true? (map #(= (canon %1) (canon %2))
                                                                        (:results c) (:results g)))))]]
                (swap! per-file update (::file c) (fnil (fn [m] (-> m (update :cases inc) (update :pass + (if ok 1 0))
                                                                     (update :inputs + n) (update :inputs-pass + in-ok)))
                                                        {:cases 0 :pass 0 :inputs 0 :inputs-pass 0}))
                (when-not ok (swap! fails conj {:id (::id c) :pattern (:pattern c) :flags (:flags c)
                                                :want want :got text})))
              (let [tot (reduce (fn [a m] (merge-with + a m)) {:cases 0 :pass 0 :inputs 0 :inputs-pass 0} (vals @per-file))]
                (doseq [[f m] @per-file]
                  (println (format "c2g-regex: %-5s %-14s %4d cases: %4d pass   %5d inputs: %5d pass"
                                   arch f (:cases m) (:pass m) (:inputs m) (:inputs-pass m))))
                (println (format "c2g-regex: %-5s %-14s %4d cases: %4d pass   %5d inputs: %5d pass   (run %.1f s, exit %d)"
                                 arch "total" (:cases tot) (:pass tot) (:inputs tot) (:inputs-pass tot) secs rc))
                (swap! summary assoc arch {:files @per-file :total tot :fails @fails :exit rc :run-s secs})
                (when (System/getenv "C2G_SHOW")
                  (doseq [f (take (if (= "all" (System/getenv "C2G_SHOW")) 100000 20) @fails)]
                    (println "   FAIL" (:id f) (pr-str (:pattern f)) (:flags f))
                    (println "     want" (subs (:want f) 0 (min 400 (count (:want f)))))
                    (println "     got " (some-> (:got f) (#(subs % 0 (min 400 (count %)))))))))))))
      (spit (str out-dir "/results.edn") (with-out-str (arbace.pprint/pprint @summary)))
      (shutdown-agents)
      (System/exit 0))))
