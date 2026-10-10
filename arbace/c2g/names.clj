(ns arbace.c2g.names
  "c2g's names (doc/go/C2G-SPEC.md §4.2, §4.4): Go packages of Java classes, Go identifiers of
  classes, members and locals, the descriptor codes of mangled method names, and the Go type of
  a Java type (§5.1) given the class facts of the closed world."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]))

;; ---------------------------------------------------------------------------------------
;; Go packages (§4.2)

(defn pkg-of
  "The Go package of a class (internal name): :jrt for the JDK's packages, :lang for Arbace's."
  [internal]
  (cond
    (or (str/starts-with? internal "java/") (str/starts-with? internal "jdk/")
        (str/starts-with? internal "sun/") (str/starts-with? internal "javax/")) :jrt
    :else :lang))

(def pkg-path {:jrt "arbace/jrt" :lang "arbace/lang"})
(def pkg-name {:jrt "jrt" :lang "lang"})
(def pkg-ns {:jrt "go.arbace.jrt" :lang "go.arbace.lang"})

;; ---------------------------------------------------------------------------------------
;; munging

(def go-keywords
  #{"break" "case" "chan" "const" "continue" "default" "defer" "else" "fallthrough" "for"
    "func" "go" "goto" "if" "import" "interface" "map" "package" "range" "return" "select"
    "struct" "switch" "type" "var"})

(def go-predeclared
  #{"any" "bool" "byte" "comparable" "complex64" "complex128" "error" "float32" "float64" "int"
    "int8" "int16" "int32" "int64" "rune" "string" "uint" "uint8" "uint16" "uint32" "uint64"
    "uintptr" "true" "false" "iota" "nil" "append" "cap" "clear" "close" "complex" "copy"
    "delete" "imag" "len" "make" "max" "min" "new" "panic" "print" "println" "real" "recover"})

(def reserved-locals
  "Names c2g's generated code uses for itself (receivers, imports, temporaries)."
  #{"t" "this" "jrt" "math" "atomic" "reflect" "unsafe" "lang" "exc" "ctl" "rv" "_"})

(defn munge-name
  "A Java or Clojure name as a Go identifier fragment: Clojure's munge, $ as _."
  [s]
  (let [m (str/replace (str (munge (str s))) "$" "_")]
    ;; munge leaves characters Go rejects that Clojure allows in symbols only rarely; anything
    ;; else not a letter, digit or _ becomes _
    (apply str (map (fn [c] (if (or (Character/isLetterOrDigit (char c)) (= c \_)) c \_)) m))))

(defn local-name
  "The Go name of a Java local or parameter (§4.4): munged, with _ appended when it would be a
  Go keyword, a predeclared or reserved name, or would start with an upper-case letter (which
  could shadow a type of the package)."
  [sym]
  (let [s (munge-name (name sym))
        s (if (str/blank? s) "x" s)]
    (if (or (go-keywords s) (go-predeclared s) (reserved-locals s)
            (Character/isUpperCase (.charAt ^String s 0))
            (str/starts-with? s "c2g_") (re-matches #"tmp\d+.*" s) (re-matches #"[lL]\d+" s))
      (str s "_")
      s)))

;; ---------------------------------------------------------------------------------------
;; classes (§4.4)

(def renames
  "The rename table (§4.4, Collisions): classes whose Go name would collide with another of the
  closed world's or with jrt's own names."
  {;; jrt's byte[] is ByteArray (§5.9)
   "jdk/internal/util/ByteArray" "Jdk_ByteArray"
   ;; jrt's java.util.Date is Date (JRT-NOTES.md, phase 2B "Dates")
   "java/sql/Date" "Sql_Date"
   ;; java.util.Tripwire's twin in java.util.stream (step 5 phase 2B, streams)
   "java/util/stream/Tripwire" "Stream_Tripwire"
   ;; java.net.URLConnection's subclass in sun.net.www, and the URL handlers, all named Handler
   ;; (JRT-NOTES.md, "Files")
   "sun/net/www/URLConnection" "Www_URLConnection"
   "sun/net/www/protocol/file/Handler" "File_Handler"
   "sun/net/www/protocol/http/Handler" "Http_Handler"
   "sun/net/www/protocol/https/Handler" "Https_Handler"
   "sun/net/www/protocol/jar/Handler" "Jar_Handler"
   ;; jrt's java.lang.reflect.Proxy is Proxy; java.net.Proxy, which URL's members and
   ;; Socket(Proxy) name (JRT-NOTES.md, "Files", "Sockets")
   "java/net/Proxy" "Net_Proxy"})

(defn go-class-name
  "The Go type name of a class: its binary name after the package, $ as _; X prepended when it
  does not start with an upper-case letter."
  [internal]
  (or (get renames internal)
  (let [s (subs internal (inc (.lastIndexOf ^String internal "/")))
        s (munge-name s)]
    (if (and (seq s) (Character/isUpperCase (.charAt ^String s 0)))
      s
      (str "X" s)))))

(defn capitalize [^String s]
  (if (empty? s) s (str (Character/toUpperCase (.charAt s 0)) (subs s 1))))

;; ---------------------------------------------------------------------------------------
;; descriptor codes (§4.4)

(defn desc-code
  "The code of a parameter or return descriptor in a mangled name."
  [d]
  (cond
    (t/array? d) (let [n (t/array-dims d)
                       e (subs d n)]
                   (str (desc-code e) n))
    (= d "Ljava/lang/Object;") "O"
    (t/class-desc? d) (go-class-name (t/desc->internal d))
    :else d))

(defn method-base
  "The mangled method name without a class prefix: M_P1_..._Pn__R."
  [name desc]
  (let [[ps r] (t/parse-method-desc desc)
        m (capitalize (munge-name name))
        m (if (str/starts-with? m "_") (str "X" m) m)]
    (str m (apply str (map #(str "_" (desc-code %)) ps)) "__" (desc-code r))))

(defn ctor-base
  "Ctor_P1_..._Pn, or Ctor; desc is the constructor's full descriptor (implicit parameters
  included)."
  [desc]
  (let [[ps _] (t/parse-method-desc desc)]
    (str "Ctor" (apply str (map #(str "_" (desc-code %)) ps)))))

(defn new-name
  "C_New_P1_..._Pn, or C_New."
  [cls-go desc]
  (let [[ps _] (t/parse-method-desc desc)]
    (str cls-go "_New" (apply str (map #(str "_" (desc-code %)) ps)))))

(defn field-name [fname] (str "F_" (munge-name fname)))

(defn static-field-name [cls-go fname] (str cls-go "_" (munge-name fname)))

(defn static-method-name [cls-go name desc] (str cls-go "_" (method-base name desc)))

(defn impl-name [mbase] (str "Impl_" mbase))
