(ns arbace.c2g.embed
  "bin/c2g --program: the namespaces' sources the executable embeds (C2G-SPEC §10.3, §10.6;
  doc/go/EVAL-NOTES.md, \"Loading\"), and the classes they name that are outside the closed
  world (registered as cut classes, so that imports and hints resolve).

  The embedded tree is arbace/**/*.clj less the tools (the class forms compiler's back end, j2c,
  g2c, c2g),
  the runtime's class forms (arbace/lang, compiled into the program), ASM, and the namespaces the
  Go build leaves out (JAVA-SURFACE.md decision 6, B1-PLAN.md D6). The Go build's namespace
  variants, arbace/lang/go/ns/P.clj, replace arbace/P.clj; arbace/lang/go/ns/P.after.clj is
  appended to it; arbace/lang/go/ns/P.subst.clj, a vector [old new ...] of strings, replaces
  each old (found exactly once) in it."
  (:require [arbace.string :as str]
            [arbace.java.io :as io])
  (:import (java.io File PushbackReader StringReader)
           (arbace.lang LispReader$Resolver)))

(def excluded
  "Resource paths (prefixes) not embedded."
  [;; of the class forms compiler, the analysis is embedded (doc/go/CLASSFORMS-REPL.md): not the
   ;; bytecode back end and the tools around it
   "arbace/classes/emit.clj" "arbace/classes/compiler.clj" "arbace/classes/shape.clj"
   "arbace/classes/verify.clj" "arbace/classes/build.clj" "arbace/classes/boot.clj" "arbace/j2c/" "arbace/g2c/" "arbace/c2g/" "arbace/lang/" "arbace/asm/"
   "arbace/asm.clj" "arbace/lang.clj" "arbace/java/api"
   ;; decision 6: processes, URLs, sockets, browsers
   "arbace/core/server.clj" "arbace/repl/deps.clj" "arbace/java/basis" "arbace/tools/deps/"
   "arbace/java/process.clj" "arbace/java/shell.clj" "arbace/java/browse" "arbace/java/javadoc.clj"
   ;; D6: Swing, SAX
   "arbace/inspector.clj" "arbace/xml.clj"])

(def variants-dir "arbace/lang/go/ns")

(defn sources
  "The embedded sources: {resource-path text}, from the checkout at root."
  [root]
  (let [base (io/file root "arbace")
        files (for [^File f (file-seq base)
                    :when (and (.isFile f) (str/ends-with? (.getName f) ".clj"))
                    :let [rel (str "arbace/" (str (.relativize (.toPath base) (.toPath f))))]
                    :when (not-any? #(str/starts-with? rel %) excluded)]
                [rel (slurp f)])
        vdir (io/file root variants-dir)
        variants (when (.isDirectory vdir)
                   (for [^File f (file-seq vdir)
                         :when (and (.isFile f) (str/ends-with? (.getName f) ".clj"))]
                     [(str (.relativize (.toPath vdir) (.toPath f))) (slurp f)]))]
    (reduce (fn [m [rel text]]
              (cond
                ;; P.subst.clj: [old new ...], each old found exactly once in arbace/P.clj
                (str/ends-with? rel ".subst.clj")
                (let [p (str "arbace/" (subs rel 0 (- (count rel) (count ".subst.clj"))) ".clj")
                      pairs (partition 2 (read-string text))]
                  (when-not (contains? m p)
                    (throw (ex-info (str "c2g: namespace variant " rel ": no " p) {})))
                  (update m p (fn [^String src]
                                (reduce (fn [^String src [^String old new]]
                                          (let [i (.indexOf src old)]
                                            (when (or (neg? i) (not= i (.lastIndexOf src old)))
                                              (throw (ex-info (str "c2g: namespace variant " rel ": "
                                                                   (pr-str old) " not found exactly once in " p) {})))
                                            (str (subs src 0 i) new (subs src (+ i (count old))))))
                                        src pairs))))
                (str/ends-with? rel ".after.clj")
                (let [p (str "arbace/" (subs rel 0 (- (count rel) (count ".after.clj"))) ".clj")]
                  (when-not (contains? m p)
                    (throw (ex-info (str "c2g: namespace variant " rel ": no " p) {})))
                  (update m p str text))
                :else (assoc m (str "arbace/" rel) text)))
            (into (sorted-map) files)
            (sort-by first variants))))

;; ---------------------------------------------------------------------------------------
;; The classes the sources name

(def ^:private resolver
  "Reads ::kw and `sym without the namespaces' aliases (only class names are wanted)."
  (reify LispReader$Resolver
    (currentNS [_] 'user)
    (resolveClass [_ s] s)
    (resolveAlias [_ s] s)
    (resolveVar [_ s] s)))

(defn- read-all [text]
  (let [r (PushbackReader. (StringReader. text))]
    (binding [*read-eval* false
              *reader-resolver* resolver
              *default-data-reader-fn* (fn [_ v] v)]
      (loop [acc []]
        (let [f (read {:eof ::eof :read-cond :allow} r)]
          (if (= f ::eof) acc (recur (conj acc f))))))))

(defn- class-like? [^String s]
  (let [i (.lastIndexOf s ".")]
    (and (pos? i) (< (inc i) (count s)) (Character/isUpperCase (.charAt s (inc i))))))

(defn- package-like? [^String s]
  (and (pos? (.indexOf s ".")) (not (class-like? s))))

(defn names
  "The class names (binary, dotted) the forms of text mention: qualified symbols, the
  namespace of a qualified symbol, and import lists (pkg Name ...)."
  [text]
  (let [acc (transient #{})
        walk (fn walk [x]
               (when-let [m (meta x)] (walk m))
               (cond
                 (symbol? x)
                 (do (when (and (nil? (namespace x)) (class-like? (name x)))
                       (conj! acc (name x)))
                     (when (and (namespace x) (class-like? (namespace x)))
                       (conj! acc (namespace x))))
                 (or (seq? x) (vector? x))
                 (do (when (and (symbol? (first x)) (nil? (namespace (first x)))
                                (package-like? (name (first x))) (seq (rest x))
                                (every? symbol? (rest x)))
                       (doseq [c (rest x)] (conj! acc (str (first x) "." c))))
                     (doseq [y x] (walk y)))
                 (map? x) (doseq [[k v] x] (walk k) (walk v))
                 (set? x) (doseq [y x] (walk y))))]
    (doseq [f (read-all text)] (walk f))
    (persistent! acc)))

(def library-cuts
  "Classes outside the world that libraries loaded from ARBACE_PATH import, cut as those the
  embedded sources name are: Clojure's suite's test.generative runner requires
  clojure.tools.namespace.find and clojure.java.classpath, which import java.util.jar's JarFile
  and JarEntry, java.io.FileReader, and extend java.net.URLClassLoader (the runner then lists no
  jars, and reads no files through them); clojure.tools.reader names java.text.SimpleDateFormat
  (its #inst reader's formatter)."
  ["java.util.jar.JarFile" "java.util.jar.JarEntry" "java.net.URLClassLoader" "java.io.FileReader"
   "java.text.SimpleDateFormat"])

(defn cut-candidates
  "The classes the sources name that exist on this JVM, from the JDK or arbace.lang/arbace.asm
  (not the classes the namespaces define themselves: deftype, defrecord, definterface), as
  internal names; and library-cuts."
  [srcs]
  (set (for [n (concat (for [[_ text] srcs n (names text)] n) library-cuts)
             :when (and (or (re-find #"^(java|javax|jdk|sun)\." n)
                            (re-find #"^arbace\.(lang|asm)\." n))
                        (try (Class/forName n false (ClassLoader/getSystemClassLoader)) true
                             (catch Throwable _ false)))]
         (str/replace n "." "/"))))

(defn with-member-types
  "The cut candidates (internal names) and the JDK classes their public members name that are
  outside the world (in-world?), one level deep: the cut classes' member tables then name them
  (Files/createTempFile returns a Path)."
  [cands in-world?]
  (let [types (fn [^Class c]
                (concat (mapcat (fn [^java.lang.reflect.Method m] (cons (.getReturnType m) (.getParameterTypes m))) (.getMethods c))
                        (map (fn [^java.lang.reflect.Field f] (.getType f)) (.getFields c))
                        (mapcat (fn [^java.lang.reflect.Constructor k] (.getParameterTypes k)) (.getConstructors c))))
        elem (fn [^Class c] (if (.isArray c) (recur (.getComponentType c)) c))
        more (for [n cands
                   :let [c (try (Class/forName (str/replace n "/" ".") false (ClassLoader/getSystemClassLoader))
                                (catch Throwable _ nil))]
                   :when (and c (not (in-world? n)))
                   ^Class t (map elem (types c))
                   :when (not (.isPrimitive t))
                   :let [tn (str/replace (.getName t) "." "/")]
                   :when (and (re-find #"^(java|javax|jdk|sun)/" tn)
                              (java.lang.reflect.Modifier/isPublic (.getModifiers t))
                              (not (in-world? tn)))]
               tn)]
    (sort (distinct (concat cands more)))))

;; ---------------------------------------------------------------------------------------
;; The image of prepared namespaces (doc/go/EXEC-NOTES.md): its encoding is hand-written forms
;; of the main package; decoding makes objects of the program's struct types by name, so the
;; main package lists them

(def image-forms
  "The image's encoding: Go forms of the main package, copied into the program."
  "go/arbace/cmd/arbace/image.clj")

(defn image-type-forms
  "The main package's table of the program's struct types (image_types.go): every exported,
  non-generic struct type of arbace/lang and arbace/jrt, as declared by the forms under
  prog-dir, registered by name for the image's decoder."
  [prog-dir]
  (let [re #"\(go/type ([A-Z][A-Za-z0-9_]*)\s+(?:\"(?:[^\"\\]|\\.)*\"\s+)?\(struct[ )]"
        types (for [[pkg alias] [["lang" "lang"] ["jrt" "jrt"]]
                    :let [dir (io/file prog-dir "go/arbace" pkg)]
                    ^File f (sort (.listFiles dir))
                    :when (and (.isFile f) (str/ends-with? (.getName f) ".clj")
                               (not (str/ends-with? (.getName f) "_test.clj")))
                    [_ n] (re-seq re (slurp f))]
                (symbol alias n))]
    [(list 'go/file "image_types.go" :imports '[[reflect "reflect"] [jrt "arbace/jrt"] [lang "arbace/lang"]])
     (list 'go/func 'init []
           (list 'registerImageTypes
                 (apply list 'lit '(slice reflect/Type)
                        (for [t (sort-by str (distinct types))]
                          (list 'reflect/TypeOf (list 'lit t))))))]))
