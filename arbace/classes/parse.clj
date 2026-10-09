(ns arbace.classes.parse
  "Syntax of the class forms (SPEC §4): a class form becomes a class declaration map, before
  any name is resolved. Access flags follow from the metadata and Java's implicit modifiers."
  (:require [arbace.string :as str])
  (:import (arbace.asm Opcodes)))

(def member-heads '#{field method constructor initializer static-initializer defclass constants do})

(defn- kw-meta [m k] (boolean (get m k)))

(def modifier-flags
  {:public Opcodes/ACC_PUBLIC :private Opcodes/ACC_PRIVATE :protected Opcodes/ACC_PROTECTED
   :static Opcodes/ACC_STATIC :final Opcodes/ACC_FINAL :abstract Opcodes/ACC_ABSTRACT
   :synchronized Opcodes/ACC_SYNCHRONIZED :native Opcodes/ACC_NATIVE
   :transient Opcodes/ACC_TRANSIENT :volatile Opcodes/ACC_VOLATILE
   :synthetic Opcodes/ACC_SYNTHETIC :bridge Opcodes/ACC_BRIDGE})

(defn flags-of
  "Flags from modifier keywords in metadata m, limited to those in `allowed`."
  [m allowed]
  (reduce (fn [acc k] (if (and (get m k) (allowed k)) (bit-or acc (modifier-flags k)) acc))
          0 (keys modifier-flags)))

(defn has? [flags f] (not (zero? (bit-and flags f))))

(defn kind-of [m]
  (cond (:interface m) :interface (:enum m) :enum (:record m) :record
        (:annotation m) :annotation :else :class))

(defn split-options
  "Splits leading keyword/value options off a sequence. Returns [options-map rest]."
  [xs]
  (loop [xs xs opts {}]
    (if (and (keyword? (first xs)) (next xs))
      (recur (nnext xs) (assoc opts (first xs) (second xs)))
      [opts xs])))

(defn expand-members
  "Splices `do` and expands member macros, resolved in namespace ns."
  [ns forms]
  (mapcat (fn expand [f]
            (cond
              (not (seq? f)) (throw (ex-info (str "Bad class member: " (pr-str f)) {:form f}))
              (= 'do (first f)) (mapcat expand (rest f))
              (member-heads (first f)) [f]
              :else
              (let [v (and (symbol? (first f)) (binding [*ns* ns] (resolve (first f))))]
                (if (and (var? v) (:macro (meta v)))
                  (expand (apply @v f nil (rest f)))
                  (throw (ex-info (str "Bad class member: " (pr-str f)) {:form f}))))))
          forms))

(declare parse-class parse-member*)

(defn- parse-params [v]
  (when-not (vector? v) (throw (ex-info (str "Expected a parameter vector: " (pr-str v)) {:form v})))
  (let [[fixed [_ rest-param]] (split-with #(not= '& %) v)]
    {:params (vec (concat fixed (when rest-param [rest-param])))
     :varargs (boolean rest-param)
     :vmeta (meta v)}))

(def ^:private member-ids (atom 0))

(defn parse-member
  "A parsed member; :mid identifies it (members equal as values may differ in metadata)."
  [ns f]
  (cond-> (assoc (parse-member* ns f) :mid (swap! member-ids inc))
    (:line (meta f)) (assoc :line (:line (meta f)))))

(defn- parse-member* [ns f]
  (case (first f)
    field (let [[_ nm & init] f]
            {:kind :field :sym nm :name (name nm) :meta (meta nm)
             :has-init (boolean (seq init)) :init (first init)})
    method (let [[_ nm & more] f
                 [opts more] (split-options more)
                 [pv & body] more]
             (merge {:kind :method :sym nm :name (name nm) :meta (meta nm) :opts opts
                     :body body :has-body (boolean (seq body))}
                    (parse-params pv)))
    constructor (let [[_ & more] f
                      [opts more] (split-options more)
                      [pv & body] more
                      pp (parse-params pv)]
                  (merge {:kind :ctor :name "<init>" :opts opts :body body} pp))
    initializer {:kind :init :body (rest f)}
    static-initializer {:kind :clinit :body (rest f)}
    constants {:kind :constants
               :constants (mapv (fn [c]
                                  (if (symbol? c)
                                    {:sym c :name (name c) :meta (meta c) :args []}
                                    (let [[nm args & body] c]
                                      {:sym nm :name (name nm) :meta (meta nm) :args (vec args)
                                       :param-tags (:param-tags (meta args))
                                       :body body :has-body (some? body)})))
                                (rest f))}
    defclass {:kind :class :form f}))

(defn- interface-like? [kind] (contains? #{:interface :annotation} kind))

(defn class-flags
  "Class file access flags and InnerClasses flags of a declaration."
  [{:keys [kind meta nesting outer-kind constants members] :as decl}]
  (let [m meta
        member? (= nesting :member)
        in-interface? (interface-like? outer-kind)
        acc (cond
              (and member? in-interface?) Opcodes/ACC_PUBLIC
              (:public m) Opcodes/ACC_PUBLIC
              (:protected m) Opcodes/ACC_PROTECTED
              (:private m) Opcodes/ACC_PRIVATE
              :else 0)
        static? (or (:static m)
                    (and member? (or in-interface? (not= kind :class)))
                    (and (= nesting :local) (not= kind :class)))
        enum-bodies? (some :has-body constants)
        enum-abstract? (and (= kind :enum)
                            (some #(and (= :method (:kind %)) (:abstract (:meta %))) members))
        enum-body? (:enum-body decl)
        final? (or (:final m) (= kind :record) enum-body?
                   (and (= kind :enum) (not enum-bodies?)))
        abstract? (or (:abstract m) (interface-like? kind) (and enum-abstract? enum-bodies?))
        base (bit-or (if final? Opcodes/ACC_FINAL 0)
                     (if abstract? Opcodes/ACC_ABSTRACT 0)
                     (if (interface-like? kind) Opcodes/ACC_INTERFACE 0)
                     (if (= kind :annotation) Opcodes/ACC_ANNOTATION 0)
                     (if (or (= kind :enum) enum-body?) Opcodes/ACC_ENUM 0)
                     (if (:synthetic m) Opcodes/ACC_SYNTHETIC 0))
        inner (bit-or base acc (if static? Opcodes/ACC_STATIC 0))
        cls (bit-or base
                    (if (has? acc (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_PROTECTED)) Opcodes/ACC_PUBLIC 0)
                    (if (interface-like? kind) 0 Opcodes/ACC_SUPER))]
    {:flags cls :inner-flags inner :static? (boolean static?)}))

(defn parse-class
  "Parses a class form (name record-components? option* member*), the defclass head stripped.
  `ctx`: {:ns ns :nesting :top|:member|:local :outer outer-decl}. Returns a declaration:
  {:simple :kind :meta :nesting :opts :components :members [parsed member ...] ...}."
  [ctx [nm & more]]
  (when-not (symbol? nm) (throw (ex-info (str "Bad class name: " (pr-str nm)) {})))
  (let [m (meta nm)
        kind (kind-of m)
        [components more] (if (and (= kind :record) (vector? (first more)))
                            [(first more) (rest more)]
                            [nil more])
        [opts members] (split-options more)
        members (map #(parse-member (:ns ctx) %) (expand-members (:ns ctx) members))
        constants (some #(when (= :constants (:kind %)) (:constants %)) members)
        decl {:sym nm :simple (name nm) :kind kind :meta m :nesting (:nesting ctx)
              :outer-kind (:kind (:outer ctx))
              :opts opts :components components :constants constants
              :members (vec members) :ns (:ns ctx)}]
    (when (some #(str/includes? (name nm) %) [";" "[" "/" "<" ">"])
      (throw (ex-info (str "Bad class name: " nm) {})))
    (merge decl (class-flags decl))))
