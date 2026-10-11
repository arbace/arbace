(ns arbace.j2c.resolve
  "The class forms compiler's overload resolution (doc/classes/SPEC.md §5.6, §7.3), asked by the
  converter to decide where a call needs param-tags. The compiler (arbace.classes, loaded through
  arbace.classes.boot) resolves over class infos made from javac's symbols, so the classes being
  converted need not be loadable, and arguments are stubs with the types the converted forms
  will have."
  (:require [arbace.classes.boot]
            [arbace.classes.analyze :as a]
            [arbace.classes.env :as env]
            [arbace.j2c.jtypes :as jt]
            [arbace.string :as str])
  (:import [com.sun.tools.javac.code Type Type$ArrayType Symbol Symbol$ClassSymbol
            Symbol$MethodSymbol Flags]))

;;; ------------------------------------------------------------------------------------------
;;; javac symbols as the compiler's class infos (arbace.classes.env)

(defn internal
  "The internal name of class symbol `c` (not renamed: resolution is on the source's names)."
  [^Symbol$ClassSymbol c]
  (str/replace (str (.flatName c)) "." "/"))

(defn desc
  "The descriptor of the erasure of javac type `t`."
  [^Type t]
  (let [t (jt/erasure t)]
    (case (jt/tag-name t)
      "BOOLEAN" "Z" "BYTE" "B" "SHORT" "S" "CHAR" "C" "INT" "I" "LONG" "J" "FLOAT" "F"
      "DOUBLE" "D" "VOID" "V"
      "ARRAY" (str "[" (desc (.elemtype ^Type$ArrayType t)))
      "CLASS" (str "L" (internal (.tsym t)) ";")
      "BOT" :null
      (throw (ex-info (str "No descriptor for type " t) {:type (str t)})))))

(defn- method-flags
  "Class file access flags of method symbol `m` (javac keeps varargs and bridge elsewhere)."
  [^Symbol m]
  (let [f (.flags m)]
    (bit-or (bit-and f 0x1D3F)
            (if (zero? (bit-and f Flags/VARARGS)) 0 0x80)
            (if (zero? (bit-and f Flags/BRIDGE)) 0 0x40))))

;; Per javac run (Symtab): internal name -> ClassSymbol met so far, and the infos made.
(def ^:private registries (java.util.WeakHashMap.))

(defn- registry []
  (locking registries
    (or (.get registries jt/*syms*)
        (let [r {:syms (atom {}) :infos (atom {})}]
          (.put registries jt/*syms* r)
          r))))

(defn- register! [^Symbol$ClassSymbol c]
  (when c (swap! (:syms (registry)) assoc (internal c) c))
  c)

(defn- class-of-type [^Type t]
  (when (and t (= "CLASS" (jt/tag-name t))) (register! (.tsym (jt/erasure t)))))

(defn- find-class ^Symbol$ClassSymbol [n]
  (or (get @(:syms (registry)) n)
      (register! (first (.getClassesForName jt/*syms* (jt/jname (str/replace n "/" ".")))))))

(defn- outer-class [^Symbol$ClassSymbol c]
  (let [o (.owner c)]
    (when-not (instance? com.sun.tools.javac.code.Symbol$PackageSymbol o)
      (register! (.enclClass o)))))

(defn class-info
  "The compiler's class info of class symbol `c` (only what resolution and access need)."
  [^Symbol$ClassSymbol c]
  (let [n (internal c)
        infos (:infos (registry))]
    (or (get @infos n)
        (let [info (try
                     (.complete c)
                     (let [iface? (.isInterface c)
                           sup (when-not iface? (class-of-type (.getSuperclass c)))]
                       {:name n
                        :flags (bit-and (.flags c) 0x761F)
                        :interface? iface?
                        :super (some-> sup internal)
                        :interfaces (vec (keep #(some-> (class-of-type %) internal) (.getInterfaces c)))
                        :nest-host (internal (.outermostClass c))
                        :outer (some-> (outer-class c) internal)
                        :fields []
                        :methods (vec (for [^Symbol s (.getSymbols (.members c))
                                            :when (instance? Symbol$MethodSymbol s)
                                            :let [ps (.getParameterTypes (.erasure s jt/*types*))
                                                  r (.getReturnType (.erasure s jt/*types*))]]
                                        {:name (str (.name s))
                                         :desc (str "(" (apply str (map desc ps)) ")" (desc r))
                                         :flags (method-flags s) :owner n}))})
                     (catch com.sun.tools.javac.code.Symbol$CompletionFailure _ nil))]
          (when info (swap! infos assoc n info))
          info))))

(def ^:private lookup
  (reify arbace.lang.ILookup
    (valAt [this k] (.valAt this k nil))
    (valAt [_ k nf]
      (or (when (string? k) (some-> (find-class k) class-info)) nf))))

(def ^:private compile-set (reify arbace.lang.IDeref (deref [_] lookup)))

;;; ------------------------------------------------------------------------------------------
;;; Arguments

(defn literal-node
  "The node of an integer literal, or of a conditional whose values are the integer literals
  `vals` (a long to Clojure, narrowed by the context, §5.4)."
  [vals]
  (let [c (fn [v] {:op :const :type "J" :val (long v) :literal true})]
    (if (= 1 (count vals))
      (c (first vals))
      (reduce (fn [e v] {:op :if :then (c v) :else e :type "J"})
              (c (last vals)) (rest (reverse vals))))))

(defn arg-node
  "A stub node for an argument whose converted form has type `t` (a javac type, :null, or
  :lit-int with its literal values `vals`). nil when it cannot be made."
  [t vals]
  (cond
    (= t :null) {:op :const :type :null :val nil}
    (= t :lit-int) (when (seq vals) (literal-node vals))
    (instance? Type t) {:op :local :type (desc t)}
    :else nil))

;;; ------------------------------------------------------------------------------------------
;;; Resolution

(defn choice
  "What the compiler chooses for a call written without param-tags: kind :instance (receiver
  of static type `site`), :static (class `site`) or :ctor (of class `site`), from class `from`
  (symbols), method name `mname`, argument stubs `args`. Returns [descriptor varargs?], or nil
  when the compiler finds no method or an ambiguous call."
  [kind ^Symbol$ClassSymbol from site mname args]
  (when (every? some? args)
    (binding [env/*compile-set* compile-set]
      (register! from)
      (let [fname (internal from)
            sname (if (instance? Type site)
                    (if (= "ARRAY" (jt/tag-name site)) "java/lang/Object" (internal (register! (.tsym (jt/erasure site)))))
                    (internal (register! site)))]
        (try
          (let [cands (case kind
                        :instance (a/instance-candidates fname sname mname)
                        :static (a/static-candidates fname sname mname)
                        :ctor (a/ctor-candidates fname sname))]
            (when (seq cands)
              (let [[m va] (a/select-method {} cands (vec args) mname nil)]
                [(:desc m) (boolean va)])))
          (catch arbace.lang.ExceptionInfo e
            (if (:arbace/compile-error (ex-data e)) nil (throw e))))))))

(defn- params-part [^String d] (subs d 0 (inc (.indexOf d ")"))))

(defn pin-needed?
  "True unless the compiler, given the call without param-tags, chooses javac's method `m` in
  the same arity mode (`varargs?`: the variable arguments are written unpacked)."
  [kind from site ^Symbol$MethodSymbol m args varargs?]
  (let [[d va] (choice kind from site (str (.name m)) args)
        ps (.getParameterTypes (.erasure m jt/*types*))
        want (str "(" (apply str (map desc ps)) ")")]
    (not (and d (= (params-part d) want) (= va (boolean varargs?))))))
