(ns arbace.javalisp.tree
  "A generic, reflective view of javac trees: every syntactic field of every node, with
  positions reduced to line numbers. Used to compare a tree with its round-tripped twin."
  (:require [arbace.javalisp.javac :as javac])
  (:import [com.sun.tools.javac.tree JCTree JCTree$JCParens JCTree$JCCompilationUnit]
           [com.sun.tools.javac.util Name]
           [java.lang.reflect Field Modifier]))

(def skipped-fields
  "Fields that attribution or later phases fill in, or that are not syntax."
  #{"type" "sym" "packge" "modle" "locn" "toplevelScope" "namedImportScope" "starImportScope"
    "moduleImportScope" "sourcefile" "importScope" "polyKind" "target" "owner" "varargsElement"
    "constructor" "constructorType" "attribute" "directive" "record" "fullComponentTypes"
    "referentType" "refPolyKind" "ownerAccessible" "erasedExprOriginalType" "syntheticGuard"
    "patternMatchingCatch" "completesNormally" "finallyCanCompleteNormally"
    "hasUnconditionalPattern" "isExhaustive" "patternSwitch" "wasEnumSelector" "operator"
    "lineMap" "endPositions" "docComments" "wasMethodReference" "pos"})

(def position-fields #{"bracePos" "typePos"})

(def ^:private fields-of
  (memoize
   (fn [^Class c]
     (->> (.getFields c)
          (remove #(Modifier/isStatic (.getModifiers ^Field %)))
          (remove #(skipped-fields (.getName ^Field %)))
          (sort-by #(.getName ^Field %))
          vec))))

(def ^:dynamic *skip-parens* false)

(defn view
  "A data view of `t` from compilation unit `cu`: [ClassName line {field value}]."
  [cu t]
  (letfn [(v [x]
            (cond
              (nil? x) nil
              (and *skip-parens* (instance? JCTree$JCParens x)) (v (.-expr ^JCTree$JCParens x))
              (instance? JCTree x)
              (let [^JCTree t x]
                [(.getSimpleName (class t)) (javac/line cu (.-pos t))
                 (into (sorted-map)
                       (for [^Field f (fields-of (class t))]
                         [(.getName f)
                          (let [val (.get f t)]
                            (if (position-fields (.getName f))
                              (javac/line cu val)
                              (v val)))]))])
              (instance? Iterable x) (mapv v x)
              (instance? Name x) (str x)
              (instance? Enum x) (str x)
              :else x))]
    (v t)))

(defn diff
  "The first difference between views `a` and `b` as {:path .. :a .. :b .. :near line}, or
  nil; :near is the line of the innermost node around the difference in `a`."
  ([a b] (diff a b [] nil))
  ([a b path near]
   (cond
     (= a b) nil
     (and (vector? a) (vector? b) (string? (first a)) (string? (first b)))
     (let [near (or (second a) near)]
       (if (not= (first a) (first b))
         {:path path :a (first a) :b (first b) :near near}
         (or (when (not= (second a) (second b))
               {:path (conj path (first a) :line) :a (second a) :b (second b) :near near})
             (diff (nth a 2) (nth b 2) (conj path (first a)) near))))
     (and (map? a) (map? b))
     (some (fn [k] (diff (get a k) (get b k) (conj path k) near)) (distinct (concat (keys a) (keys b))))
     (and (vector? a) (vector? b))
     (if (not= (count a) (count b))
       {:path path :a (count a) :b (count b) :what :count :near near}
       (some identity (map-indexed (fn [i [x y]] (diff x y (conj path i) near)) (map vector a b))))
     :else {:path path :a a :b b :near near})))
