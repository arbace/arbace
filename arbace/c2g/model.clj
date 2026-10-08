(ns arbace.c2g.model
  "The closed world of one c2g run (C2G-SPEC §4.1, §5): which classes exist in Go (translated
  ones, T, and jrt's hand-written ones, H), leaf classes (§5.3), the Go type of a Java type
  (§5.1), virtual method sets and their implementations (§5.4, §5.5)."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.c2g.names :as nm]
            [arbace.c2g.jrt :as jrt])
  (:import (arbace.asm Opcodes)))

(def ^:dynamic *w*
  "The world: {:jrt scan :T atom-or-set :jrt-classes {internal {:go :var :standin :file}}}."
  nil)

(defn has? [flags f] (not (zero? (bit-and (or flags 0) f))))

(defn T [] (let [x (:T *w*)] (if (instance? arbace.lang.IDeref x) @x x)))

(defn translated? [n] (contains? (T) n))

(defn jrt-class
  "jrt's hand-written (or stand-in) class n, when jrt defines it: {:go :var :standin :file}."
  [n]
  (get (:jrt-classes *w*) n))

(defn hand-written?
  "Is n a class jrt provides and c2g does not translate in this world?"
  [n]
  (and (jrt-class n) (not (translated? n))))

(defn in-world? [n]
  (or (= n "java/lang/Object") (translated? n) (boolean (jrt-class n))))

(defn translatable?
  "Is n in the class environment as class forms (c2g can translate it)?"
  [n]
  (boolean (a/decl n)))

(defn reflectable-interface?
  "Is n a JDK interface c2g knows only by reflection (no class forms: outside the converted
  closure), which it may declare: its methods, no code (C2G-NOTES.md, \"Reflected
  interfaces\")?"
  [n]
  (and (not (a/decl n))
       (or (str/starts-with? n "java/") (str/starts-with? n "javax/") (str/starts-with? n "jdk/") (str/starts-with? n "sun/"))
       (not (str/includes? n "$$"))
       (some? (env/info n))
       (env/interface? n)
       ;; annotation types are interfaces too: not these
       (not (has? (:flags (env/info n)) 0x2000))
       ;; marker interfaces only (RandomAccess): one with methods would bring the types of
       ;; its methods (IntStream, TemporalField) and jrt's classes would lack them
       (empty? (:methods (env/info n)))
       (every? #(or (in-world? %) (reflectable-interface? %)) (:interfaces (env/info n)))))

(defn reflected?
  "Is n a translated class declared from reflection (no class forms)?"
  [n]
  (and (translated? n) (not (a/decl n))))

(defn info [n] (env/info n))
(defn interface? [n] (env/interface? n))

(defn desc-classes
  "The classes a descriptor mentions (array element classes included)."
  [d]
  (cond (nil? d) nil
        (keyword? d) nil
        (t/array? d) (desc-classes (subs d (t/array-dims d)))
        (t/class-desc? d) [(t/desc->internal d)]
        :else nil))

(defn method-desc-classes [desc]
  (let [[ps r] (t/parse-method-desc desc)]
    (mapcat desc-classes (cons r ps))))

(defn desc-in-world? [d] (every? in-world? (desc-classes d)))
(defn mdesc-in-world? [desc] (every? in-world? (method-desc-classes desc)))

;; ---------------------------------------------------------------------------------------
;; classes

(defn go-name [n] (nm/go-class-name n))
(defn pkg [n] (nm/pkg-of n))

(defn abstract? [n] (has? (:flags (info n)) Opcodes/ACC_ABSTRACT))
(defn final? [n] (has? (:flags (info n)) Opcodes/ACC_FINAL))

(defn subclasses
  "Direct subclasses of class n in the world (memoized per world)."
  [n]
  (get ((:subclass-index *w*)) n))

(defn make-subclass-index [w]
  (let [cache (atom nil)]
    (fn []
      (or @cache
          (reset! cache
                  (binding [*w* w]
                    (reduce (fn [m c]
                              (let [s (:super (info c))]
                                (if s (update m s (fnil conj #{}) c) m)))
                            {} (concat (T) (keys (:jrt-classes w))))))))))

(defn leaf?
  "Is class n a leaf (§5.3): its values are *C pointers. Interfaces are not; jrt's classes are
  leaves when jrt gives them no class interface C_I."
  [n]
  (cond
    (= n "java/lang/Object") false
    (interface? n) false
    (hand-written? n) (not (contains? (:types (:jrt *w*)) (str (go-name n) "_I")))
    :else (and (not (abstract? n)) (empty? (subclasses n)))))

;; ---------------------------------------------------------------------------------------
;; Go types (§5.1)

(defn qualify
  "A Go identifier of package `of` (:jrt or :lang) as referred to from package `from`."
  [from of ident]
  (if (or (= from of) (nil? from)) (symbol ident) (symbol (nm/pkg-name of) ident)))

(defn class-sym
  "The Go identifier `suffix` of class n (\"\" for the type itself), as referred to from `from`."
  ([from n] (class-sym from n ""))
  ([from n suffix] (qualify from (pkg n) (str (go-name n) suffix))))

(defn jrt-sym [from ident] (qualify from :jrt ident))

(def prim-go '{"Z" bool "B" int8 "C" uint16 "S" int16 "I" int32 "J" int64 "F" float32 "D" float64})

(def prim-array-go {"Z" "BooleanArray" "B" "ByteArray" "C" "CharArray" "S" "ShortArray"
                    "I" "IntArray" "J" "LongArray" "F" "FloatArray" "D" "DoubleArray"})

(defn go-type
  "The Go type form of descriptor d in package `from` (§5.1)."
  [from d]
  (cond
    (= d :null) 'any
    (prim-go d) (prim-go d)
    (= d "V") nil
    (= d "Ljava/lang/Object;") 'any
    (t/array? d) (let [e (t/elem-type d)]
                   (if (t/prim? e)
                     (list '* (jrt-sym from (prim-array-go e)))
                     (list '* (jrt-sym from "RefArray"))))
    (t/class-desc? d)
    (let [n (t/desc->internal d)]
      (cond
        (not (in-world? n)) 'any
        (interface? n) (class-sym from n)
        (leaf? n) (list '* (class-sym from n))
        :else (class-sym from n "_I")))
    :else (throw (ex-info (str "c2g: no Go type for " (pr-str d)) {:desc d}))))

(defn pointer-desc?
  "Is the Go type of d a pointer (leaf class, String, array)?"
  [d]
  (cond (t/array? d) true
        (t/class-desc? d) (let [n (t/desc->internal d)]
                            (and (in-world? n) (not (interface? n)) (not= n "java/lang/Object")
                                 (leaf? n)))
        :else false))

(defn iface-desc?
  "Is the Go type of d an interface (any, C_I, J)?"
  [d]
  (and (t/ref? d) (not= d :null) (not (pointer-desc? d))))

;; ---------------------------------------------------------------------------------------
;; members

(defn static? [m] (has? (:flags m) Opcodes/ACC_STATIC))
(defn private? [m] (has? (:flags m) Opcodes/ACC_PRIVATE))
(defn abstract-m? [m] (has? (:flags m) Opcodes/ACC_ABSTRACT))

(defn methods-of [n] (:methods (info n)))
(defn fields-of [n] (:fields (info n)))

(defn find-method [n name desc]
  (some #(when (and (= name (:name %)) (= desc (:desc %))) %) (methods-of n)))

(defn virtual-member?
  "A method that takes part in dispatch: an instance method, not private, not a constructor."
  [m]
  (and (not (static? m)) (not (private? m)) (not= "<init>" (:name m)) (not= "<clinit>" (:name m))))

(def object-keys
  "Object's methods every class has through jrt.Object_I (§5.8)."
  #{["toString" "()Ljava/lang/String;"] ["hashCode" "()I"] ["equals" "(Ljava/lang/Object;)Z"]
    ["clone" "()Ljava/lang/Object;"] ["getClass" "()Ljava/lang/Class;"]})

(defn supertypes [n]
  (let [i (info n)] (remove nil? (cons (:super i) (:interfaces i)))))

(defn world-supertypes
  "n's supertypes that exist in the world (interfaces that do not are left out, §4.1)."
  [n]
  (filter in-world? (supertypes n)))

(defn all-supertypes
  "n and its supertypes that exist in the world, superclass chain first then interfaces
  (Java's supertypes: an interface in the world counts also when it is reached only through
  one that is not)."
  [n]
  (vec (filter in-world? (env/all-supertypes n))))

(defn hw-method-exists?
  "Does jrt's hand-written class or interface n have method m (name desc) in Go?"
  [n m]
  (let [scan (:jrt *w*)
        g (go-name n)
        base (nm/method-base (:name m) (:desc m))]
    (cond
      (= n "java/lang/Object") (contains? object-keys [(:name m) (:desc m)])
      (interface? n) (or (contains? (jrt/iface-methods scan g) base)
                         ;; a default method is a package function J_m
                         (contains? (:funcs scan) (str g "_" base)))
      :else (let [ms (jrt/struct-methods scan g)]
              (or (contains? ms base) (contains? ms (nm/impl-name base)))))))

(defn member-in-world?
  "Does method m of class n exist in Go: its descriptor's classes exist, and for a jrt class
  jrt has it."
  [n m]
  (and (mdesc-in-world? (:desc m))
       (or (not (hand-written? n)) (hw-method-exists? n m))))

(defn declared-vmethods
  "The virtual methods n declares that exist in Go, by [name desc]."
  [n]
  (for [m (methods-of n)
        :when (virtual-member? m)
        :when (member-in-world? n m)]
    (assoc m :owner n)))

(defn vmethods
  "Every virtual method of n (declared and inherited, abstract or not), in the world, Object's
  excluded: an ordered map [name desc] -> method (the most specific declaration: classes first)."
  [n]
  (let [cache (:vmethods-cache *w*)]
    (or (get @cache n)
        (let [r (reduce (fn [acc c]
                          (if (= c "java/lang/Object")
                            acc
                            (reduce (fn [acc m]
                                      (let [k [(:name m) (:desc m)]]
                                        (if (or (contains? acc k) (contains? object-keys k)) acc (assoc acc k m))))
                                    acc (declared-vmethods c))))
                        (array-map) (all-supertypes n))]
          (swap! cache assoc n r)
          r))))

(defn superclass-chain [n]
  (take-while some? (iterate #(:super (info %)) n)))

(defn impl-of
  "The implementation of virtual method k = [name desc] for class n: {:kind :class :owner c
  :method m} for a method with a body in n or a superclass (a jrt class's: :jrt-impl when jrt
  has Impl_, :promoted when the method itself is on its struct), {:kind :default :owner j
  :method m} for an interface's default method, or nil (abstract)."
  [n [name desc :as k]]
  (or (some (fn [c]
              (when-let [m (and (not (interface? c)) (find-method c name desc))]
                (when (and (not (static? m)) (not (abstract-m? m))
                           (or (not (private? m)) (= c n)))
                  (cond
                    (= c "java/lang/Object") {:kind :object :owner c :method m}
                    (hand-written? c)
                    (let [base (nm/method-base name desc)
                          ms (jrt/struct-methods (:jrt *w*) (go-name c))]
                      (cond (contains? ms (nm/impl-name base)) {:kind :jrt-impl :owner c :method m}
                            (contains? ms base) {:kind :promoted :owner c :method m}
                            :else nil))
                    :else {:kind :class :owner c :method m}))))
            (superclass-chain n))
      ;; default methods: the maximally specific one (JVMS 5.4.3.3)
      (let [cands (for [c (all-supertypes n)
                        :when (interface? c)
                        :let [m (find-method c name desc)]
                        :when (and m (not (abstract-m? m)) (not (static? m)) (not (private? m)))
                        :when (or (not (hand-written? c))
                                  (contains? (:funcs (:jrt *w*)) (str (go-name c) "_" (nm/method-base name desc))))]
                    {:kind :default :owner c :method m})
            cands (remove (fn [x] (some #(and (not= (:owner %) (:owner x)) (env/subclass? (:owner %) (:owner x))) cands)) cands)]
        (first cands))))

(defn trivial-init?
  "Does class n have trivial initialization (§6.2): no static initializer code, no
  non-constant static field initializer, a superclass with trivial initialization? jrt's
  classes count as trivial (jrt initializes them itself)."
  [n]
  (let [cache (:trivial-cache *w*)]
    (if (contains? @cache n)
      (get @cache n)
      (let [r (cond
                (not (translated? n)) true
                (nil? (a/decl n)) true
                :else (let [d (a/decl n)
                            st @(:state d)]
                        (and (empty? (:clinit st))
                             (empty? (:clj-consts st))
                             (not (:uses-assert st))
                             (not (:interface-assert st))
                             (or (nil? (:super d)) (trivial-init? (:super d))))))]
        (swap! cache assoc n r)
        r))))
