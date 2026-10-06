(ns arbace.javalisp.javac
  "Bridge to the JDK's own javac: parse Java source text into JCTree compilation units,
  and compile sources in memory to class bytes."
  (:import [com.sun.tools.javac.api JavacTool JavacTaskImpl]
           [com.sun.tools.javac.tree JCTree JCTree$JCCompilationUnit]
           [com.sun.tools.javac.util Position]
           [javax.tools DiagnosticCollector Diagnostic Diagnostic$Kind ForwardingJavaFileManager
                        JavaFileObject JavaFileObject$Kind SimpleJavaFileObject StandardLocation]
           [java.io ByteArrayOutputStream]
           [java.net URI]))

(def release "26")

(defn source-object
  "An in-memory JavaFileObject named `path` holding `text`."
  ^JavaFileObject [^String path ^String text]
  (proxy [SimpleJavaFileObject] [(URI/create (str "string:///" path)) JavaFileObject$Kind/SOURCE]
    (getCharContent [_] text)))

(defn- diagnostics->errors [^DiagnosticCollector dc]
  (vec (for [^Diagnostic d (.getDiagnostics dc)
             :when (= Diagnostic$Kind/ERROR (.getKind d))]
         (str (some-> ^JavaFileObject (.getSource d) .getName) ":" (.getLineNumber d) ": "
              (.getMessage d nil)))))

(defn parse
  "Parse Java source `text` (named `path`, which matters only for module-info.java)
  into {:cu JCCompilationUnit, :text text, :errors [..]}."
  [path text]
  (let [tool (JavacTool/create)
        dc (DiagnosticCollector.)
        fm (.getStandardFileManager tool dc nil nil)
        task ^JavacTaskImpl (.getTask tool nil fm dc
                                      ["-proc:none" "--enable-preview" "--release" release
                                       "-XDshould-stop.at=ATTR"]
                                      nil [(source-object path text)])
        [cu] (seq (.parse task))]
    {:cu cu :text text :errors (diagnostics->errors dc)}))

(defn line
  "The 1-based line of character position `pos` in `cu`, or nil for NOPOS."
  [^JCTree$JCCompilationUnit cu pos]
  (when (and pos (not= pos Position/NOPOS))
    (.getLineNumber (.getLineMap cu) (long pos))))

(defn compile-sources
  "Compile the sources {path text} together in memory with `opts` (javac options) and
  return {:classes {binary-name bytes}, :errors [..]}."
  [sources opts]
  (let [tool (JavacTool/create)
        dc (DiagnosticCollector.)
        sfm (.getStandardFileManager tool dc nil nil)
        out (atom {})
        fm (proxy [ForwardingJavaFileManager] [sfm]
             (getJavaFileForOutput [loc ^String class-name kind sibling]
               (proxy [SimpleJavaFileObject] [(URI/create (str "mem:///" class-name ".class")) kind]
                 (openOutputStream []
                   (proxy [ByteArrayOutputStream] []
                     (close []
                       (swap! out assoc class-name (.toByteArray ^ByteArrayOutputStream this))))))))
        task (.getTask tool nil fm dc (into ["-proc:none" "--release" release "-XDrawDiagnostics"] opts)
                       nil (for [[p t] sources] (source-object p t)))]
    (.call task)
    {:classes @out :errors (diagnostics->errors dc)}))
