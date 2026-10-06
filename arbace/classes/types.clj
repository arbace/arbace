(ns arbace.classes.types
  "Types of the class forms (SPEC §4.3): type forms as data, their erasure to JVM descriptors
  and their generic `Signature` strings.

  Code generation works on descriptors: \"I\", \"J\", \"Ljava/lang/String;\", \"[I\", \"V\".
  Two pseudo types are keywords: :null (the type of nil) and :none (a form that does not
  complete). Class names are internal names (\"java/lang/String\")."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------------------------
;; descriptors

(def prim-names
  {'int "I" 'long "J" 'short "S" 'byte "B" 'char "C" 'float "F" 'double "D" 'boolean "Z"
   'void "V"})

(def prim-desc->name (into {} (map (fn [[k v]] [v (name k)]) prim-names)))

(def clojure-array-hints
  {'ints "[I" 'longs "[J" 'shorts "[S" 'bytes "[B" 'chars "[C" 'floats "[F" 'doubles "[D"
   'booleans "[Z" 'objects "[Ljava/lang/Object;"})

(def object-desc "Ljava/lang/Object;")
(def string-desc "Ljava/lang/String;")

(defn prim? [t] (and (string? t) (= 1 (count t)) (not= t "V")))
(defn void? [t] (= t "V"))
(defn array? [t] (and (string? t) (str/starts-with? t "[")))
(defn ref? [t] (or (= t :null) (and (string? t) (or (str/starts-with? t "L") (array? t)))))
(defn class-desc? [t] (and (string? t) (str/starts-with? t "L")))

(defn internal->desc [n] (if (str/starts-with? n "[") n (str "L" n ";")))
(defn desc->internal
  "Internal name of a reference descriptor (array descriptors stay as they are)."
  [d]
  (if (str/starts-with? d "L") (subs d 1 (dec (count d))) d))

(defn elem-type [d] (subs d 1))
(defn array-of [d] (str "[" d))
(defn array-dims [d] (count (take-while #(= \[ %) d)))

(defn size [t] (if (or (= t "J") (= t "D")) 2 (if (= t "V") 0 1)))

(defn int-like? [t] (contains? #{"I" "S" "B" "C"} t))
(defn numeric? [t] (contains? #{"I" "S" "B" "C" "J" "F" "D"} t))

(def box-of {"I" "java/lang/Integer" "J" "java/lang/Long" "S" "java/lang/Short"
             "B" "java/lang/Byte" "C" "java/lang/Character" "F" "java/lang/Float"
             "D" "java/lang/Double" "Z" "java/lang/Boolean"})
(def unbox-of (into {} (map (fn [[k v]] [(internal->desc v) k]) box-of)))

(defn method-desc [params ret] (str "(" (apply str params) ")" ret))

(defn parse-method-desc
  "[param-descs ret-desc] of a method descriptor."
  [^String d]
  (loop [i 1 ps []]
    (if (= \) (.charAt d i))
      [ps (subs d (inc i))]
      (let [j (loop [j i] (if (= \[ (.charAt d j)) (recur (inc j)) j))
            end (if (= \L (.charAt d j)) (inc (.indexOf d ";" j)) (inc j))]
        (recur end (conj ps (subs d i end)))))))

(defn package-of [internal]
  (let [i (.lastIndexOf ^String internal "/")] (if (neg? i) "" (subs internal 0 i))))

(defn simple-name-of [internal]
  (let [n (subs internal (inc (.lastIndexOf ^String internal "/")))]
    (subs n (inc (.lastIndexOf ^String n "$")))))

(defn desc->class-name
  "The Java binary name for Class.forName: java.lang.String, [I, [Ljava.lang.String;"
  [d]
  (cond (array? d) (str/replace d "/" ".")
        :else (str/replace (desc->internal d) "/" ".")))

(defn class->desc [^Class c]
  (cond (.isPrimitive c) (prim-names (symbol (.getName c)))
        (.isArray c) (str/replace (.getName c) "." "/")
        :else (str "L" (str/replace (.getName c) "." "/") ";")))

;; ---------------------------------------------------------------------------------------------
;; type forms

;; A parsed type ("tnode") is one of
;;   {:t :prim :desc "I"}
;;   {:t :class :name "java/util/List" :args [tnode ...] :outer tnode-or-nil}
;;   {:t :array :elem tnode}
;;   {:t :tvar :sym T}
;;   {:t :wild :kind nil|:extends|:super :bound tnode}
;;   {:t :inter :types [tnode ...]}
;; Annotations on type nodes are kept as :anns (the metadata map), see §4.4.

(defn- array-sym? [s]
  (and (symbol? s) (namespace s) (re-matches #"[1-9]" (name s))))

(declare parse-type)

(defn- with-anns [tn form]
  (let [m (meta form)
        anns (when m (into {} (filter (fn [[k _]] (symbol? k)) m)))]
    (if (seq anns) (assoc tn :anns anns) tn)))

(defn parse-type
  "Parses a type form in `scope`: {:resolve (fn [sym] internal-name or nil) :tvars #{syms}}.
  Throws on unknown names."
  [scope form]
  (with-anns
    (cond
      (and (symbol? form) (prim-names form) (nil? (namespace form)))
      {:t :prim :desc (prim-names form)}

      (array-sym? form)
      (let [base (symbol (namespace form))
            n (parse-long (name form))]
        (nth (iterate (fn [e] {:t :array :elem e}) (parse-type scope base)) n))

      (and (symbol? form) (clojure-array-hints form))
      (let [d (clojure-array-hints form)]
        (if (= d "[Ljava/lang/Object;")
          {:t :array :elem {:t :class :name "java/lang/Object" :args []}}
          {:t :array :elem {:t :prim :desc (subs d 1)}}))

      (= form '?) {:t :wild :kind nil}

      (and (symbol? form) (contains? (:tvars scope) form))
      {:t :tvar :sym form}

      (symbol? form)
      (if-let [n ((:resolve scope) form)]
        (if (str/starts-with? n "[")
          (parse-type scope (symbol (str/replace (subs (str n) 0) "/" ".")))
          {:t :class :name n :args []})
        (throw (ex-info (str "Unknown type: " form) {:form form})))

      (string? form)                    ; Clojure's string tags for arrays
      (let [d (str/replace form "." "/")]
        (if (str/starts-with? d "[")
          (letfn [(p [d] (cond (str/starts-with? d "[") {:t :array :elem (p (subs d 1))}
                               (str/starts-with? d "L") {:t :class :name (desc->internal d) :args []}
                               :else {:t :prim :desc d}))]
            (p d))
          (parse-type scope (symbol form))))

      (class? form) (let [d (class->desc form)]
                      (parse-type scope (if (prim? d) (symbol (prim-desc->name d))
                                            (if (array? d) (desc->class-name d) (symbol (.getName ^Class form))))))

      (and (seq? form) (= 'array (first form)))
      {:t :array :elem (parse-type scope (second form))}

      (and (seq? form) (= '? (first form)))
      (let [[_ k b] form]
        {:t :wild :kind (keyword (name k)) :bound (parse-type scope b)})

      (and (seq? form) (= '& (first form)))
      {:t :inter :types (mapv #(parse-type scope %) (rest form))}

      (and (seq? form) (= '.. (first form)))
      (reduce (fn [outer f]
                (let [[c & args] (if (seq? f) f [f])
                      n (str (:name outer) "$" (name c))]
                  {:t :class :name n :args (mapv #(parse-type scope %) args) :outer outer}))
              (parse-type scope (second form))
              (nnext form))

      (seq? form)
      (let [c (parse-type scope (first form))]
        (when-not (= :class (:t c)) (throw (ex-info (str "Not a generic class: " form) {})))
        (assoc c :args (mapv #(parse-type scope %) (rest form))))

      :else (throw (ex-info (str "Bad type form: " (pr-str form)) {:form form})))
    form))

(defn erase
  "The descriptor of a parsed type. `bounds` maps type variable symbols to their bound tnodes."
  [bounds tn]
  (case (:t tn)
    :prim (:desc tn)
    :class (internal->desc (:name tn))
    :array (array-of (erase bounds (:elem tn)))
    :tvar (if-let [b (first (get bounds (:sym tn)))] (erase (dissoc bounds (:sym tn)) b) object-desc)
    :inter (erase bounds (first (:types tn)))
    :wild (if (= :extends (:kind tn)) (erase bounds (:bound tn)) object-desc)))

(defn generic?
  "Does a type need a Signature: does it involve type arguments or type variables?"
  [tn]
  (case (:t tn)
    :prim false
    :class (or (boolean (seq (:args tn))) (boolean (and (:outer tn) (generic? (:outer tn)))))
    :array (generic? (:elem tn))
    :tvar true
    :inter true
    :wild true))

(defn signature
  "The generic signature string of a parsed type."
  [tn]
  (case (:t tn)
    :prim (:desc tn)
    :class (if-let [o (:outer tn)]
             (let [os (signature o)]
               (str (subs os 0 (dec (count os))) "." (subs (:name tn) (inc (count (:name o))))
                    (when (seq (:args tn)) (str "<" (apply str (map signature (:args tn))) ">")) ";"))
             (str "L" (:name tn)
                  (when (seq (:args tn)) (str "<" (apply str (map signature (:args tn))) ">")) ";"))
    :array (str "[" (signature (:elem tn)))
    :tvar (str "T" (name (:sym tn)) ";")
    :wild (case (:kind tn) nil "*" :extends (str "+" (signature (:bound tn)))
                :super (str "-" (signature (:bound tn))))
    :inter (signature (first (:types tn)))))

(defn parse-type-params
  "Parses a :type-params vector: [T (U extends Number) (V extends Object Comparable)].
  Returns [{:sym T :bounds [tnode...]} ...]. The type variables themselves must already be in
  the scope's :tvars."
  [scope tps]
  (mapv (fn [tp]
          (if (symbol? tp)
            {:sym tp :bounds [] :anns (meta tp)}
            (let [[s kw & bs] tp]
              (when-not (= 'extends kw) (throw (ex-info (str "Bad type parameter: " tp) {})))
              {:sym s :bounds (mapv #(parse-type scope %) bs) :anns (meta s)})))
        tps))

(defn type-params-signature
  "<T:Ljava/lang/Object;U::Ljava/lang/Comparable<TU;>;> -- `interface?` tells whether a class
  name is an interface (an interface first bound leaves the class bound empty)."
  [interface? tps]
  (if (empty? tps)
    ""
    (str "<"
         (apply str
                (for [{:keys [sym bounds]} tps]
                  (str (name sym)
                       (if (empty? bounds)
                         ":Ljava/lang/Object;"
                         (apply str
                                (map-indexed
                                  (fn [i b]
                                    (if (and (zero? i) (= :class (:t b)) (interface? (:name b)))
                                      (str "::" (signature b))
                                      (str ":" (signature b))))
                                  bounds))))))
         ">")))

(defn bounds-map [tps] (into {} (map (fn [{:keys [sym bounds]}] [sym bounds]) tps)))
