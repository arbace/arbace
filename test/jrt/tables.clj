(ns jrt.tables
  "jrt's member tables (C2G-SPEC §5.11, doc/go/JRT-NOTES.md, \"The member tables\"): for each
  class jrt hand-writes, the table of its public members that the manifest declares, with
  their invokers, written exactly as c2g writes the tables of the classes it translates; and
  java.lang.Object's table, whose invokers call jrt's helpers (§5.8). Reflection on jrt's own
  classes (String, Math, Class ...) reads them: Compiler resolves (.length s) through
  String's table.

  The file it makes, go/arbace/jrt/reflect_tables.clj, is generated and tracked.

  bin/jrt manifest   regenerates it after the manifest (ARBACE_CLASSPATH=test bin/arbace -m
                     jrt.tables FILE)."
  (:require [jrt.manifest :as m]
            [arbace.string :as str])
  (:import [java.lang.reflect Constructor Field Method Modifier]))

;; ---------------------------------------------------------------------------------------
;; Go types and class expressions of Java types (C2G-SPEC §5.1)

(def prim-go
  {Boolean/TYPE "bool" Byte/TYPE "int8" Character/TYPE "uint16" Short/TYPE "int16"
   Integer/TYPE "int32" Long/TYPE "int64" Float/TYPE "float32" Double/TYPE "float64"})

(def prim-class
  {Boolean/TYPE "Prim_boolean" Byte/TYPE "Prim_byte" Character/TYPE "Prim_char" Short/TYPE "Prim_short"
   Integer/TYPE "Prim_int" Long/TYPE "Prim_long" Float/TYPE "Prim_float" Double/TYPE "Prim_double"
   Void/TYPE "Prim_void"})

(def prim-array
  {Boolean/TYPE "BooleanArray" Byte/TYPE "ByteArray" Character/TYPE "CharArray" Short/TYPE "ShortArray"
   Integer/TYPE "IntArray" Long/TYPE "LongArray" Float/TYPE "FloatArray" Double/TYPE "DoubleArray"})

(defn go-type
  "The Go type of the Java type c, as a form's text; nil for Object (any)."
  [{:keys [types]} ^Class c]
  (cond
    (= c Object) nil
    (.isPrimitive c) (prim-go c)
    (.isArray c) (let [e (.getComponentType c)]
                   (if (.isPrimitive e) (str "(* " (prim-array e) ")") "(* RefArray)"))
    :else (let [n (m/go-type-name (.getName c))]
            (cond
              (.isInterface c) n
              (contains? types (symbol (str n "_I"))) (str n "_I")
              :else (str "(* " n ")")))))

(defn class-expr
  "The expression of the Class object of c."
  [^Class c]
  (cond
    (.isPrimitive c) (prim-class c)
    (.isArray c) (str "(.ArrayClass " (class-expr (.getComponentType c)) ")")
    :else (str (m/go-type-name (.getName c)) "_class")))

(defn arg-expr
  "Argument i of an invoker, converted to the parameter's Go type."
  [defs ^Class p i]
  (let [a (format "(aget args %d)" i)
        t (go-type defs p)]
    (cond
      (nil? t) a
      (.isPrimitive p) (format "(assert %s %s)" t a)
      :else (format "((inst As %s) %s)" t a))))

(defn receiver-expr [defs ^Class c]
  (format "(assert %s this)" (go-type defs c)))

(defn params-expr [params]
  (if (empty? params)
    "nil"
    (str "(lit (slice (* Class)) " (str/join " " (map class-expr params)) ")")))

(defn- args [defs params]
  (str/join "" (map-indexed (fn [i p] (str " " (arg-expr defs p i))) params)))

;; ---------------------------------------------------------------------------------------
;; Entries

(defn method-entry [defs ^Class c ^Method mt]
  (let [params (.getParameterTypes mt)
        ret (.getReturnType mt)
        g (m/method-go-name (.getName mt) params ret)
        t (m/go-type-name (.getName c))
        static? (Modifier/isStatic (.getModifiers mt))
        fname (str t "_" g)
        call (if static?
               (format "(%s%s)" (if (contains? (:values defs) fname) fname (str fname "_native")) (args defs params))
               (format "(.%s %s%s)" g (receiver-expr defs c) (args defs params)))]
    (format "(lit MethodInfo :Name %s :Params %s :Return %s :Modifiers 0x%x\n         :Invoke (fn ^any [^any this ^{:tag (slice any)} args] %s))"
            (pr-str (.getName mt)) (params-expr params) (class-expr ret) (.getModifiers mt)
            (if (= ret Void/TYPE) (str call " nil") call))))

(defn ctor-entry [defs ^Class c ^Constructor k]
  (let [params (.getParameterTypes k)
        n (str (m/go-type-name (.getName c)) "_New" (m/ctor-suffix params))]
    (when (contains? (:values defs) n)
      (format "(lit CtorInfo :Params %s :Modifiers 0x%x\n         :New (fn ^any [^{:tag (slice any)} args] (%s%s)))"
              (params-expr params) (.getModifiers k) n (args defs params)))))

(defn field-entry [defs ^Class c ^Field f]
  (let [n (str (m/go-type-name (.getName c)) "_" (.getName f))
        mods (.getModifiers f)
        t (.getType f)
        v (if (nil? (go-type defs t))
            "v"
            (if (.isPrimitive t) (format "(assert %s v)" (go-type defs t)) (format "((inst As %s) v)" (go-type defs t))))]
    (format "(lit FieldInfo :Name %s :Type %s :Modifiers 0x%x\n         :Get (fn ^any [^any o] %s)%s)"
            (pr-str (.getName f)) (class-expr t) mods n
            (if (Modifier/isFinal mods) "" (format "\n         :Set (fn [^any o ^any v] (set! %s %s))" n v)))))

(defn class-table
  "The init forms setting the member table of the hand-written class cname."
  [defs [cname _ non-leaf?]]
  (let [c (Class/forName cname false (ClassLoader/getPlatformClassLoader))
        ms (filter #(and (:ok %) (Modifier/isPublic (.getModifiers ^java.lang.reflect.Member (:member %))))
                   (m/members cname non-leaf? defs))
        by (fn [k] (map :member (filter #(instance? k (:member %)) ms)))
        ;; by name; a name's overloads in the order the JVM's getMethods gives them, which
        ;; Reflector's choice among applicable overloads follows (EVAL-NOTES.md)
        jvm-order (into {} (map-indexed (fn [i ^Method x] [x i]) (.getMethods c)))
        meths (keep #(method-entry defs c %) (sort-by (fn [^Method x] [(.getName x) (get jvm-order x Integer/MAX_VALUE)
                                                                         (m/descriptor (.getParameterTypes x) (.getReturnType x))])
                                                       (by Method)))
        ctors (keep #(ctor-entry defs c %) (sort-by (fn [^Constructor x] (m/descriptor (.getParameterTypes x) nil)) (by Constructor)))
        fields (keep #(field-entry defs c %) (sort-by (fn [^Field x] (.getName x)) (by Field)))
        info (str "(.Info " (m/go-type-name cname) "_class)")
        slice (fn [k xs] (str "(lit (slice " k ")\n        " (str/join "\n        " xs) ")"))]
    (str "  ;; " cname "\n"
         (when (seq meths) (format "  (set! (.-Methods %s)\n    %s)\n" info (slice "MethodInfo" meths)))
         (when (seq ctors) (format "  (set! (.-Ctors %s)\n    %s)\n" info (slice "CtorInfo" ctors)))
         (when (seq fields) (format "  (set! (.-Fields %s)\n    %s)\n" info (slice "FieldInfo" fields))))))

(def object-calls
  "java.lang.Object's public methods and its protected clone (a declared member that
  NewInstanceExpr.gatherMethods offers deftype and reify), by name and descriptor, as calls of
  jrt's helpers (§5.8) on the receiver this."
  {"equals(Ljava/lang/Object;)Z" "(Equals this (aget args 0))"
   "getClass()Ljava/lang/Class;" "(GetClass this)"
   "hashCode()I" "(HashCode this)"
   "toString()Ljava/lang/String;" "(ToString this)"
   "notify()V" "(Notify this) nil"
   "notifyAll()V" "(NotifyAll this) nil"
   "wait()V" "(Wait this) nil"
   "wait(J)V" "(WaitTimeout this (assert int64 (aget args 0)) 0) nil"
   "wait(JI)V" "(WaitTimeout this (assert int64 (aget args 0)) (assert int32 (aget args 1))) nil"
   "clone()Ljava/lang/Object;" "(.Clone__O (asObject this))"})

(defn object-table []
  (let [ms (sort-by (fn [^Method x] [(.getName x) (m/descriptor (.getParameterTypes x) (.getReturnType x))])
                    (filter #(object-calls (str (.getName ^Method %) (m/descriptor (.getParameterTypes ^Method %) (.getReturnType ^Method %))))
                            (.getDeclaredMethods Object)))]
    (str "  ;; java.lang.Object (its methods are jrt's helpers, §5.8)\n"
         "  (set! (.-Methods (.Info Object_class))\n    (lit (slice MethodInfo)\n        "
         (str/join "\n        "
                   (for [^Method mt ms
                         :let [params (.getParameterTypes mt)
                               d (str (.getName mt) (m/descriptor params (.getReturnType mt)))]]
                     (format "(lit MethodInfo :Name %s :Params %s :Return %s :Modifiers 0x%x\n         :Invoke (fn ^any [^any this ^{:tag (slice any)} args] %s))"
                             (pr-str (.getName mt)) (params-expr params) (class-expr (.getReturnType mt))
                             (.getModifiers mt) (object-calls d))))
         "))\n"
         "  (set! (.-Ctors (.Info Object_class))\n    (lit (slice CtorInfo) (lit CtorInfo :Modifiers 0x1 :New (fn ^any [^{:tag (slice any)} args] (Object_New)))))\n")))

(defn- has-class? [defs [cname]]
  (contains? (:values defs) (str (m/go-type-name cname) "_class")))

(defn text [defs]
  (str ";; Generated by test/jrt/tables.clj (bin/jrt manifest); do not edit.\n"
       ";; The member tables of jrt's hand-written classes (C2G-SPEC §5.11, doc/go/JRT-NOTES.md,\n"
       ";; \"The member tables\"): their public members the manifest declares, with invokers in the\n"
       ";; tables' value convention, as c2g writes them for the classes it translates.\n"
       "(in-ns 'go.arbace.jrt)\n\n"
       "(go/file \"reflect_tables.go\")\n\n"
       "(go/func init []\n"
       (object-table)
       (str/join "" (map #(class-table defs %) (filter #(has-class? defs %) m/hand-written)))
       ")\n"))

(defn -main [file]
  (spit file (text (m/defined "go/arbace/jrt.clj")))
  (shutdown-agents))
