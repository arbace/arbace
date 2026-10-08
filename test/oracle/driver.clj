;; The oracle's driver (doc/go/ORACLE.md): sent first on the standard input of the
;; implementation under test, before the cases, by bin/oracle (test/oracle/runner.clj). It runs
;; inside that implementation: each case comes as a call of one of its functions (form, step,
;; regex), and each result goes to the standard output as one line "@@oracle " followed by a
;; record printed by `emit` (below), which is ASCII only: every string is written with \uXXXX
;; escapes for anything outside printable ASCII, so the channel is lossless for UTF-16 strings,
;; lone surrogates included. The records do not depend on the implementation's printer of
;; collections; the printed values inside them (:value) do, which is what is tested.
;;
;; Kept small and plain: only arbace.core basics, java.lang, java.io.StringWriter and, for the
;; class scripts, java.lang.reflect.
(ns oracle.driver
  (:import (java.io StringWriter StringReader)
           (arbace.lang LineNumberingPushbackReader)))

(set! *warn-on-reflection* false)

;; ----- the record printer

(def ^:dynamic *ascii*
  "true: strings are printed in printable ASCII; false: other characters stay as they are,
  but for controls and lone surrogates (the runner's expected files, UTF-8)."
  true)

(defn- lone-surrogate? [^String s i n]
  (cond
    (and (>= n 0xD800) (<= n 0xDBFF)) (not (and (< (inc i) (.length s))
                                               (Character/isLowSurrogate (.charAt s (inc i)))))
    (and (>= n 0xDC00) (<= n 0xDFFF)) (not (and (> i 0)
                                               (Character/isHighSurrogate (.charAt s (dec i)))))
    :else false))

(defn- esc-string [^String s ^StringBuilder sb]
  (.append sb \")
  (dotimes [i (.length s)]
    (let [c (.charAt s i) n (int c)]
      (cond
        (= c \") (.append sb "\\\"")
        (= c \\) (.append sb "\\\\")
        (= c \newline) (.append sb "\\n")
        (= c \tab) (.append sb "\\t")
        (= c \return) (.append sb "\\r")
        (and (>= n 32) (< n 127)) (.append sb c)
        (and (not *ascii*) (> n 159) (not (lone-surrogate? s i n))) (.append sb c)
        :else (let [h (Integer/toHexString n)]
                (.append sb "\\u")
                (dotimes [_ (- 4 (.length h))] (.append sb \0))
                (.append sb h)))))
  (.append sb \"))

(defn- emit-double [d ^StringBuilder sb]
  (let [d (double d)]
    (cond
      (Double/isNaN d) (.append sb "##NaN")
      (= d Double/POSITIVE_INFINITY) (.append sb "##Inf")
      (= d Double/NEGATIVE_INFINITY) (.append sb "##-Inf")
      :else (.append sb (Double/toString d)))))

(defn emit-to
  "Prints x, data made of nil, booleans, integers, doubles, strings, characters, keywords,
  symbols, vectors (and other sequential things, as vectors) and maps, to sb."
  [x ^StringBuilder sb]
  (cond
    (nil? x) (.append sb "nil")
    (true? x) (.append sb "true")
    (false? x) (.append sb "false")
    (string? x) (esc-string x sb)
    (or (instance? Long x) (instance? Integer x) (instance? Short x) (instance? Byte x))
    (.append sb (str x))
    (or (instance? Double x) (instance? Float x)) (emit-double x sb)
    (instance? Character x) (let [n (int x)]
                              (cond
                                (and (>= n 0xD800) (<= n 0xDFFF))
                                ;; the reader has no character literal for a surrogate
                                (do (.append sb "{:char ") (.append sb (str n)) (.append sb "}"))
                                (and (> n 32) (< n 127))
                                (.append sb (str "\\" x))
                                :else
                                (let [h (Integer/toHexString n)]
                                  (.append sb (str "\\u" (subs "0000" (count h)) h)))))
    (keyword? x) (.append sb (str x))
    (symbol? x) (.append sb (str x))
    (map? x) (do (.append sb "{")
                 (loop [s (seq x) first? true]
                   (when s
                     (when-not first? (.append sb " "))
                     (emit-to (key (first s)) sb)
                     (.append sb " ")
                     (emit-to (val (first s)) sb)
                     (recur (next s) false)))
                 (.append sb "}"))
    (or (sequential? x) (.isArray (class x)))
    (do (.append sb "[")
        (loop [s (seq x) first? true]
          (when s
            (when-not first? (.append sb " "))
            (emit-to (first s) sb)
            (recur (next s) false)))
        (.append sb "]"))
    :else (esc-string (str "#unprintable " (.getName (class x))) sb)))

(defn emit ^String [x]
  (let [sb (StringBuilder.)] (emit-to x sb) (.toString sb)))

(def ^:private stdout *out*)

(defn- out-record [r]
  (.write ^java.io.Writer stdout (str "@@oracle " (emit r) "\n"))
  (.flush ^java.io.Writer stdout))

;; ----- exceptions

(defn- ex-chain
  "The exception and its causes: [class message] each, with the printed ex-data when there is
  some."
  [^Throwable e]
  (loop [e e acc [] n 0]
    (if (or (nil? e) (= n 8))
      acc
      (let [d (ex-data e)
            entry [(.getName (class e)) (.getMessage e)]
            entry (if d (conj entry (try (pr-str d) (catch Throwable t (str "#print-failed " (.getName (class t)))))) entry)]
        (recur (.getCause e) (conj acc entry) (inc n))))))

;; ----- forms

(defn- read-case [^String text line]
  (let [r (LineNumberingPushbackReader. (StringReader. text))]
    (.setLineNumber r (int line))
    (read {:read-cond :allow} r)))

(defn form
  "Reads the case's text (its first line numbered `line` of the corpus file `file`), evaluates
  it in the current namespace and emits [:form id ...] with the printed value (pr-str) and its
  class, or the exception chain, and what it printed to *out* and *err*."
  [id file line text]
  (let [out (StringWriter.) err (StringWriter.)
        r (binding [*out* out *err* err *file* file *source-path* file]
            (try
              (let [v (eval (read-case text line))]
                (try
                  {:value (pr-str v) :class (if (nil? v) "nil" (.getName (class v)))}
                  (catch Throwable t {:print-ex (ex-chain t)})))
              (catch Throwable t {:ex (ex-chain t)})))
        o (str out) e (str err)
        r (if (= o "") r (assoc r :out o))
        r (if (= e "") r (assoc r :err e))]
    (out-record (assoc r :id id))))

;; ----- class scripts: java.lang.reflect over the classes under test

(def ^:private bindings (atom {}))

(def ^:private prims
  {"int" Integer/TYPE "long" Long/TYPE "double" Double/TYPE "float" Float/TYPE
   "boolean" Boolean/TYPE "char" Character/TYPE "short" Short/TYPE "byte" Byte/TYPE
   "void" Void/TYPE})

(defn- class-named ^Class [^String n]
  (if (.endsWith n "[]")
    (class (java.lang.reflect.Array/newInstance (class-named (subs n 0 (- (count n) 2))) 0))
    (or (prims n) (Class/forName n))))

(defn- class-name [^Class c]
  (if (.isArray c) (str (class-name (.getComponentType c)) "[]") (.getName c)))

(defn- coerce
  "The argument value of type name t (a Java type name) for arg."
  [t a]
  (case t
    "int" (int a) "long" (long a) "double" (double a) "float" (float a)
    "short" (short a) "byte" (byte a) "char" (char a) "boolean" (boolean a)
    a))

(defn- arg-value
  "The value of an argument given as data: a literal (nil, boolean, long, double, string,
  character), {:ref name}, {:static class :name field}, {:char n} (a surrogate), {:box type :value v} (a boxed primitive of that type),
  {:biginteger s}, {:bigdecimal s}, {:array type :items [...]}."
  [a]
  (cond
    (map? a)
    (cond
      (contains? a :ref) (let [b @bindings]
                           (if (contains? b (:ref a)) (get b (:ref a))
                               (throw (IllegalStateException. (str "oracle: no binding " (:ref a))))))
      (contains? a :box) (coerce (:box a) (:value a))
      (contains? a :static) (.get (.getField (class-named (:static a)) (:name a)) nil)
      (contains? a :char) (char (:char a))
      (contains? a :biginteger) (java.math.BigInteger. ^String (:biginteger a))
      (contains? a :bigdecimal) (java.math.BigDecimal. ^String (:bigdecimal a))
      (contains? a :array) (let [ct (class-named (:array a))
                                 items (mapv arg-value (:items a))
                                 arr (java.lang.reflect.Array/newInstance ct (count items))]
                             (dotimes [i (count items)]
                               (java.lang.reflect.Array/set arr i (coerce (:array a) (nth items i))))
                             arr)
      :else (throw (IllegalArgumentException. (str "oracle: bad argument " (emit a)))))
    :else a))

(defn- arg-class
  "The class an argument's value has for overload resolution (nil for nil)."
  [a v]
  (cond
    (and (map? a) (contains? a :box)) (prims (:box a))
    (nil? v) nil
    :else (class v)))

(def ^:private widen
  {Integer/TYPE #{Integer Short Byte Long}, Long/TYPE #{Long Integer Short Byte},
   Double/TYPE #{Double Float Long}, Float/TYPE #{Float Double},
   Short/TYPE #{Short Long}, Byte/TYPE #{Byte Long},
   Character/TYPE #{Character}, Boolean/TYPE #{Boolean}})

(defn- applicable? [^Class p ac]
  (cond
    (.isPrimitive p) (or (= p ac) (contains? (widen p) ac)
                         (and (contains? #{Integer/TYPE Long/TYPE Short/TYPE Byte/TYPE Double/TYPE Float/TYPE} ac)
                              (contains? (widen p) ({Integer/TYPE Integer Long/TYPE Long Short/TYPE Short
                                                     Byte/TYPE Byte Double/TYPE Double Float/TYPE Float} ac))))
    (nil? ac) true
    (.isPrimitive ^Class ac) (.isAssignableFrom p ({Integer/TYPE Integer Long/TYPE Long Short/TYPE Short Byte/TYPE Byte
                                                    Double/TYPE Double Float/TYPE Float Character/TYPE Character
                                                    Boolean/TYPE Boolean} ac))
    :else (.isAssignableFrom p ac)))

(defn- exact-score [ps acs]
  (count (filter true? (map (fn [^Class p ac] (or (= p ac) (and (.isPrimitive p) (= ({Integer/TYPE Integer Long/TYPE Long Double/TYPE Double
                                                                                    Float/TYPE Float Short/TYPE Short Byte/TYPE Byte
                                                                                    Character/TYPE Character Boolean/TYPE Boolean} p) ac))))
                            ps acs))))

(defn- more-specific? [ps qs]
  (every? true? (map (fn [^Class p ^Class q] (or (= p q) (.isAssignableFrom q p)
                                                 (and (.isPrimitive p) (not (.isPrimitive q)))))
                     ps qs)))

(defn- choose
  "The member among cands (each [params member]) for the argument classes acs: the applicable
  ones, then the most specific, then the most exact."
  [what cands acs]
  (let [app (filter (fn [[ps _]] (every? true? (map applicable? ps acs))) cands)
        best (filter (fn [[ps _]] (every? (fn [[qs _]] (more-specific? ps qs)) app)) app)
        best (if (seq best) best app)
        top (when (seq best) (apply max (map (fn [[ps _]] (exact-score ps acs)) best)))
        best (filter (fn [[ps _]] (= top (exact-score ps acs))) best)
        sigs (distinct (map (fn [[ps _]] (mapv class-name ps)) best))]
    (cond
      (empty? best) (throw (IllegalArgumentException. (str "oracle: no applicable " what)))
      (> (count sigs) 1) (throw (IllegalArgumentException. (str "oracle: ambiguous " what ": " (emit (vec sigs)))))
      :else (first best))))

(defn- find-member
  "[member sig] for the step, by its :sig or by overload resolution on the argument classes."
  [s ^Class c args acs]
  (let [nm (:name s) n (count args)
        sig (:sig s)
        cands (case (:op s)
                :new (map (fn [^java.lang.reflect.Constructor k] [(vec (.getParameterTypes k)) k]) (.getConstructors c))
                (:invoke :static)
                (->> (.getMethods c)
                     (filter (fn [^java.lang.reflect.Method m]
                               (and (= nm (.getName m))
                                    (= (= :static (:op s)) (java.lang.reflect.Modifier/isStatic (.getModifiers m))))))
                     (sort-by (fn [^java.lang.reflect.Method m] (if (.isBridge m) 1 0)))
                     (map (fn [^java.lang.reflect.Method m] [(vec (.getParameterTypes m)) m]))))
        cands (filter (fn [[ps _]] (= n (count ps))) cands)]
    (if sig
      (let [ps (mapv class-named sig)
            m (first (filter (fn [[qs _]] (= ps qs)) cands))]
        (if m [(second m) sig]
            (throw (NoSuchMethodException. (str "oracle: " (.getName c) "." nm (emit sig))))))
      (let [[ps m] (choose (str (.getName c) "." nm) cands acs)]
        [m (mapv class-name ps)]))))

(def ^:private lazy-classes
  #{"arbace.lang.LazySeq" "arbace.lang.Iterate" "arbace.lang.Cycle" "arbace.lang.Repeat"})

(defn- observe
  "The result's description: its type, and its value as data when it is one (nil, boolean,
  integer, floating point, string, character, BigInteger, BigDecimal), else its printed form
  (RT.printString) when it is a collection, seq, keyword, symbol or number and printing it
  cannot realize anything."
  [v ret]
  (cond
    (= ret "void") {:type "void"}
    (nil? v) {:type "nil"}
    :else
    (let [c (.getName (class v))]
      (cond
        (or (instance? Boolean v) (instance? String v) (instance? Character v)
            (instance? Long v) (instance? Integer v) (instance? Short v) (instance? Byte v)
            (instance? Double v) (instance? Float v))
        {:type c :value v}
        (or (instance? java.math.BigInteger v) (instance? java.math.BigDecimal v))
        {:type c :value (str v)}
        (and (or (coll? v) (seq? v) (keyword? v) (symbol? v) (instance? Number v)
                 (instance? arbace.lang.MapEntry v))
             (not (contains? lazy-classes c)))
        {:type c :pr (try (arbace.lang.RT/printString v)
                          (catch Throwable t (str "#print-failed " (.getName (class t)))))}
        :else {:type c}))))

(defn step
  "Runs one step of a class script, s a map: :i its index; :op :new, :invoke, :static, :get
  (instance field), :get-static; :class the class (whose public members are looked up, so
  inherited ones too); :name the method or field; :target the receiver's binding name (for
  :invoke, :get); :args the arguments as data (arg-value); :sig the parameter types (Java
  names; chosen by overload resolution when absent); :bind the name to bind the result to.
  Emits [:step ...] with :sig, :ret (the declared return type) and :result (observe), or
  :throws (the exception chain)."
  [s]
  (let [r (try
            (let [args (mapv arg-value (:args s))
                  acs (mapv arg-class (:args s) args)
                  target (when (contains? s :target) (arg-value {:ref (:target s)}))
                  c (cond (:class s) (class-named (:class s))
                          (nil? target) (throw (IllegalStateException. (str "oracle: the receiver " (:target s) " is nil")))
                          :else (class target))
                  [v sig ret]
                  (case (:op s)
                    :get-static (let [f (.getField c (:name s))]
                                  [(.get f nil) nil (class-name (.getType f))])
                    :get (let [f (.getField c (:name s))]
                           [(.get f target) nil (class-name (.getType f))])
                    (let [[m sig] (find-member s c args acs)
                          ^java.lang.reflect.AccessibleObject m m
                          _ (.setAccessible m true)
                          ps (mapv class-named sig)
                          as (object-array (map (fn [p a] (coerce (class-name p) a)) ps args))]
                      (try
                        (case (:op s)
                          :new [(.newInstance ^java.lang.reflect.Constructor m as) sig (.getName c)]
                          :invoke [(.invoke ^java.lang.reflect.Method m target as) sig
                                   (class-name (.getReturnType ^java.lang.reflect.Method m))]
                          :static [(.invoke ^java.lang.reflect.Method m nil as) sig
                                   (class-name (.getReturnType ^java.lang.reflect.Method m))])
                        (catch java.lang.reflect.InvocationTargetException e
                          (throw (ex-info "oracle: thrown" {::sig sig ::class (.getName c)} (.getCause e)))))))]
              (when (:bind s) (swap! bindings assoc (:bind s) v))
              (cond-> {:ret ret :result (observe v ret)}
                sig (assoc :sig sig)
                (not (:class s)) (assoc :class (.getName c))))
            (catch Throwable t
              (if-let [sig (::sig (ex-data t))]
                (do (when (:bind s) (swap! bindings assoc (:bind s) nil))
                    (cond-> {:sig sig :throws (ex-chain (.getCause t))}
                      (not (:class s)) (assoc :class (::class (ex-data t)))))
                {:error (ex-chain t)})))]
    (out-record (assoc r :i (:i s)))))

;; ----- regex: java.util.regex

(def ^:private regex-flags
  {:d java.util.regex.Pattern/UNIX_LINES :i java.util.regex.Pattern/CASE_INSENSITIVE
   :x java.util.regex.Pattern/COMMENTS :m java.util.regex.Pattern/MULTILINE
   :literal java.util.regex.Pattern/LITERAL :s java.util.regex.Pattern/DOTALL
   :u java.util.regex.Pattern/UNICODE_CASE :canon-eq java.util.regex.Pattern/CANON_EQ
   :U java.util.regex.Pattern/UNICODE_CHARACTER_CLASS})

(defn- spans [^java.util.regex.Matcher m]
  (vec (for [g (range (inc (.groupCount m)))] [(.start m (int g)) (.end m (int g))])))

(defn- regex-input [^java.util.regex.Pattern p ^String in reps]
  (let [m (.matcher p in)
        matches (.matches m)
        mspans (when matches (spans m))
        hit-end (.hitEnd m)
        m (.reset m)
        looking (.lookingAt m)
        m (.reset m)
        finds (loop [acc [] n 0]
                (if (and (< n 1000) (.find m)) (recur (conj acc (spans m)) (inc n)) acc))
        r {:matches matches :hit-end hit-end :looking-at looking :find finds
           :split (vec (.split p in)) :split-1 (vec (.split p in (int -1)))}
        r (if mspans (assoc r :match-groups mspans) r)]
    (if (seq reps)
      (assoc r :replace
             (vec (for [rep reps]
                    (try [(.replaceAll (.matcher p in) ^String rep) (.replaceFirst (.matcher p in) ^String rep)]
                         (catch Throwable t [:ex (.getName (class t)) (.getMessage t)])))))
      r)))

(defn regex
  "One regex case, c a map: :i, :pattern, :flags (keywords, regex-flags), :inputs, :replace
  (replacement strings). Emits [:regex ...] with :groups (the group count), :named (named
  groups) and per input :matches, :match-groups (each group's [start end] of the whole
  match), :hit-end (after matches), :looking-at, :find (each match's group spans), :split,
  :split-1 (limit -1) and :replace ([replaceAll replaceFirst] per replacement); or, when the
  pattern does not compile, :error [class description index message]."
  [c]
  (let [r (try
            (let [f (reduce bit-or 0 (map regex-flags (:flags c)))
                  p (java.util.regex.Pattern/compile ^String (:pattern c) (int f))]
              {:groups (.groupCount (.matcher p ""))
               :named (into (sorted-map) (.namedGroups p))
               :results (mapv (fn [in] (try (regex-input p in (:replace c))
                                            (catch Throwable t {:ex [(.getName (class t)) (.getMessage t)]})))
                              (:inputs c))})
            (catch java.util.regex.PatternSyntaxException e
              {:error [(.getName (class e)) (.getDescription e) (.getIndex e) (.getMessage e)]})
            (catch Throwable t {:error [(.getName (class t)) (.getMessage t)]}))]
    (out-record (assoc r :i (:i c)))))

(defn done [n]
  (out-record {:done n}))

(in-ns 'user)
