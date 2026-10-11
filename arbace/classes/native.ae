(ns arbace.classes.native
  "The boundary between arbace.lang.Compiler and the class forms compiler (SPEC §9.5).

  arbace.lang.Compiler knows the class forms' special forms (class*, label*, switch*, ...) but
  does not compile them itself: there is one implementation of the class forms, this library.
  The compiler loads this namespace on first use and calls the functions below:

  - `compile-top`, for (class* :top form) and (class* :tops forms), the expansions of
    `defclass` and `defclasses`: compiles and defines the classes, and returns the form the
    compiler analyzes in place of the class form (imports and the class or classes).
  - `compile-fn`, for a fn* whose code uses the class forms (a special form of theirs in it, or
    a form whose new meaning only they know): the compiler hands the whole fn over, at the
    innermost fn* that is not one of its own wrappers (`(fn* ^:once [] ...)`); this compiles
    it into a fn class, a subclass of arbace.lang.AFunction or RestFn named as the compiler
    names its fn classes, whose code is compiled with Clojure's meaning (SPEC §5.13), and
    returns the form the compiler analyzes in its place: (new C captured-local...).
    The locals of the compiler's environment that the fn uses become the class's fields.
  - `compile-deftype`, for a deftype* one of whose method bodies uses the class forms: compiles
    and defines the deftype class, with the shape arbace.lang.Compiler gives it, and returns
    nil, the value of deftype*."
  (:require [arbace.string :as str]
            [arbace.classes.env :as env]
            [arbace.classes.lower :as lower]
            [arbace.classes.compiler :as compiler]))

(defn compile-top
  "(class* :top form) or (class* :tops forms), each form the rest of a defclass form, in
  namespace ns: compiles the classes as one compilation, with `siblings` (the class forms of
  the enclosing top-level do, SPEC §9.2) entered as declarations, and defines them (and writes
  them under *compile-files*). Returns the form evaluating to the class (:top) or a vector of
  the classes (:tops) after importing them."
  [ns kind forms siblings]
  (let [forms (if (= :top kind) [forms] (vec forms))
        loaded (compiler/compile-and-load! ns forms :siblings siblings)]
    ;; the classes themselves, not their names: a name in the unnamed package would not resolve
    `(do ~@(for [[n] loaded] (list 'arbace.core/import* n))
         ~(if (= :top kind) (second (first loaded)) (mapv second loaded)))))

;; ---------------------------------------------------------------------------------------------
;; deftypes handed over by arbace.lang.Compiler

(defn compile-deftype
  "Compiles (deftype* tagname classname [fields] :implements [interfaces] ...) in namespace ns
  into the deftype class (arbace.classes.lower/deftype-class-form) and defines it (writing it
  under *compile-files*). Returns nil, the form to analyze in place of deftype*."
  [ns form]
  (binding [*ns* ns]
    (compiler/compile-and-load! ns [(lower/deftype-class-form form)]))
  nil)

;; ---------------------------------------------------------------------------------------------
;; fns handed over by arbace.lang.Compiler

(defn- public-class? [^Class c]
  (loop [c c]
    (cond (nil? c) true
          (not (java.lang.reflect.Modifier/isPublic (.getModifiers c))) false
          :else (recur (.getDeclaringClass c)))))

(defn- type-form
  "The type form of class c (SPEC §4.3): long, java.lang.String, java.lang.String/2."
  [^Class c]
  (if (.isArray c)
    (loop [c c n 0]
      (if (.isArray c) (recur (.getComponentType c) (inc n)) (symbol (.getName c) (str n))))
    (symbol (.getName c))))

(defn- local-class
  "The class of a local binding of the compiler, as precise as a field of a class compiled
  elsewhere can have it: its primitive type, its tag's class if public, else Object."
  [lb]
  (let [prim (try (.getPrimitiveType ^arbace.lang.Compiler$LocalBinding lb) (catch Throwable _ nil))]
    (or prim
        (let [c (try (when (.hasJavaClass ^arbace.lang.Compiler$LocalBinding lb)
                       (.getJavaClass ^arbace.lang.Compiler$LocalBinding lb))
                     (catch Throwable _ nil))]
          (if (and (instance? Class c) (not (.isPrimitive ^Class c)) (public-class? c)
                   (not (str/includes? (.getName ^Class c) "compile__stub")))
            c
            Object)))))

(defn- field-names
  "Unique field names for the captured locals."
  [syms]
  (loop [[s & more] syms seen #{} out []]
    (if-not s
      out
      (let [base (munge (name s))
            n (if (seen base) (str base "__" (count out)) base)]
        (recur more (conj seen n) (conj out n))))))

(defn fn-class-form
  "The class form (rest of a defclass form) of the fn class `cname` (dotted binary name) for
  fn* form `form`, capturing the locals `captured` ([[sym Class] ...])."
  [cname form captured]
  (let [{nm :name arities :arities} (lower/fn-parts form)
        i (.lastIndexOf ^String cname ".")
        pkg (if (neg? i) "" (subs cname 0 i))
        simple (subs cname (inc i))
        rcv (gensym "fn__this")
        fields (field-names (map first captured))
        super (if (some lower/variadic? arities) 'arbace.lang.RestFn 'arbace.lang.AFunction)
        typed (fn [s c] (if (= c Object) s (with-meta s {:tag (type-form c)})))]
    (concat
      [(with-meta (symbol simple) {:public true :final true :clojure-fn true :reflection :clojure})
       :package pkg
       :extends super
       :implements (lower/prim-interfaces arities)]
      (for [[f [_ c]] (map vector fields captured)]
        (list 'field (vary-meta (typed (symbol f) c) assoc :final true)))
      [(list* 'constructor (with-meta (vec (cons rcv (map (fn [f [_ c]] (typed (symbol f) c)) fields captured)))
                             {:public true})
              (list 'super.)
              (for [f fields] (list 'set! (list (symbol (str ".-" f)) rcv) (symbol f))))]
      (lower/fn-methods rcv nm arities
                        (vec (mapcat (fn [f [s _]] [s (list (symbol (str ".-" f)) rcv)]) fields captured))))))

(defn compile-fn
  "Compiles fn* form `form` in namespace ns into the fn class `cname` (the compiler's name for
  it) and defines it (writing it under *compile-files*). `env` is the compiler's local
  environment (symbol -> LocalBinding). Returns the form to analyze in place of the fn:
  (new cname captured-local...), with the fn's metadata."
  [ns cname form env]
  (let [host (into {} (for [[s lb] env] [s (local-class lb)]))
        mentioned (lower/symbols-in form)]
    (loop [syms (filterv #(contains? host %) (sort-by str mentioned))]
      (let [captured (mapv (fn [s] [s (host s)]) syms)
            r (try
                (binding [*ns* ns]
                  (compiler/compile-and-load! ns [(fn-class-form cname form captured)]))
                (catch arbace.lang.ExceptionInfo e
                  ;; a local of the compiler's environment used though not in the form (a
                  ;; macro's expansion): capture it too
                  (let [u (some #(:unresolved (ex-data %)) (take-while some? (iterate #(.getCause ^Throwable %) e)))]
                    (if (and u (contains? host u) (not (some #{u} syms)))
                      [::retry u]
                      (throw e)))))]
        (if (= ::retry (first r))
          (recur (conj syms (second r)))
          ;; the class itself, not its name: a name in the unnamed package would not resolve
          (let [new (list* 'new (second (first r)) syms)]
            (if-let [m (lower/fn-meta form)]
              (list 'arbace.core/with-meta new m)
              new)))))))
