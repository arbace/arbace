(ns arbace.j2c.javac
  "Bridge to the JDK's own javac: parse and attribute Java sources (JavacTask.analyze), giving
  the attributed compilation units the converter works on."
  (:import [com.sun.tools.javac.api JavacTool JavacTaskImpl]
           [com.sun.tools.javac.tree JCTree$JCCompilationUnit]
           [com.sun.tools.javac.util Context]
           [com.sun.tools.javac.code Types Symtab]
           [javax.tools DiagnosticCollector Diagnostic Diagnostic$Kind JavaFileObject]
           [java.io File]))

(defn- diagnostics->errors [^DiagnosticCollector dc]
  (vec (for [^Diagnostic d (.getDiagnostics dc)
             :when (= Diagnostic$Kind/ERROR (.getKind d))]
         (str (some-> ^JavaFileObject (.getSource d) .getName) ":" (.getLineNumber d) ": "
              (.getMessage d nil)))))

(defn analyze
  "Parse and attribute the Java files `paths` together with the javac options `opts`.
  Returns {:units [JCCompilationUnit ...], :context Context, :errors [..]}. The trees are
  attributed (symbols, types, constant values) and flow-analyzed, but not desugared."
  [paths opts]
  (let [tool (JavacTool/create)
        dc (DiagnosticCollector.)
        fm (.getStandardFileManager tool dc nil nil)
        objs (.getJavaFileObjectsFromFiles fm (map #(File. ^String %) paths))
        task ^JavacTaskImpl (.getTask tool nil fm dc
                                      (into ["-proc:none" "-nowarn" "-Xlint:none" "-XDsuppressNotes"
                                             "-Xmaxerrs" "100000"] opts)
                                      nil objs)
        units (vec (.parse task))]
    (.analyze task)
    {:units units :context (.getContext task) :errors (diagnostics->errors dc)}))

(defn types ^Types [^Context ctx] (Types/instance ctx))
(defn symtab ^Symtab [^Context ctx] (Symtab/instance ctx))

(defn source-text
  "The source text of compilation unit `cu`."
  ^String [^JCTree$JCCompilationUnit cu]
  (str (.getCharContent (.getSourceFile cu) true)))
