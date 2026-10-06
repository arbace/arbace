(ns arbace.j2c.names
  "Naming classes in the output and laying out the files: one namespace per Java package
  (§9.1), so imports are shared by all files of a package. For every simple name at most one
  class is named by it; the others are written by binary name."
  (:require [arbace.j2c.forms :as f]
            [arbace.j2c.print :as pr]
            [clojure.string :as str])
  (:import [arbace.j2c.forms CRef]))

(defn package-of [^String binary]
  (let [i (.lastIndexOf binary ".")] (if (neg? i) "" (subs binary 0 i))))

(defn import-name [^String binary]
  (let [i (.lastIndexOf binary ".")] (subs binary (inc i))))

(defn crefs
  "All CRefs in form `x`, metadata included."
  [x]
  (let [out (volatile! [])]
    (letfn [(walk [x]
              (when (instance? clojure.lang.IObj x)
                (doseq [it (f/items x)]
                  (when (vector? it) (run! walk (rest it)))))
              (cond
                (instance? CRef x) (vswap! out conj x)
                (map? x) (doseq [[k v] x] (walk k) (walk v))
                (coll? x) (run! walk x)))]
      (walk x))
    @out))

(def default-imports
  "Simple name -> class name of the classes every Clojure namespace imports."
  (delay (into {} (for [[k v] clojure.lang.RT/DEFAULT_IMPORTS
                        :when (class? v)]
                    [(str k) (.getName ^Class v)]))))

(defn- java-lang? [n]
  (try (Class/forName (str "java.lang." n) false (ClassLoader/getPlatformClassLoader)) true
       (catch Throwable _ false)))

(defn naming
  "The naming table of package `pkg` from its converted `units`: {simple-name owner-binary}."
  [pkg units]
  (let [pkg-classes (into #{} (mapcat :pkg-classes units))
        uses (frequencies (for [u units, r (crefs (:forms u))] (:name r)))
        by-name (group-by import-name (keys uses))
        di @default-imports]
    (into {}
          (for [[n bins] by-name]
            [n (cond
                 (contains? di n) (di n)
                 (contains? pkg-classes (first (str/split n #"\$")))
                 (str (when (seq pkg) (str pkg ".")) n)
                 :else (let [same (filter #(= pkg (package-of %)) bins)
                             jl (str "java.lang." n)]
                         (cond
                           (seq same) (first same)
                           (and (some #{jl} bins) (java-lang? n)) jl
                           :else (apply max-key #(uses % 0) (sort bins)))))]))))

(defn how
  "How class `binary` is named in package `pkg` with naming `table`: :bare (needs no import),
  :import or :qualified."
  [table pkg binary]
  (let [n (import-name binary)]
    (cond
      (not= (table n) binary) :qualified
      (= (@default-imports n) binary) :bare
      (= pkg (package-of binary)) :bare
      (= binary (str "java.lang." n)) :bare
      :else :import)))

(defn resolver [table]
  (fn [^CRef r]
    (let [b (:name r)
          n (import-name b)]
      (if (and (= (table n) b) (not (contains? (:shadow r) n)))
        n
        b))))

(defn imports-form
  "The import call of a unit, or nil."
  [table pkg forms]
  (let [bins (sort (distinct (for [r (crefs forms)
                                   :when (= :import (how table pkg (:name r)))
                                   :when (not (contains? (:shadow r) (import-name (:name r))))]
                               (:name r))))
        groups (sort-by key (group-by package-of bins))]
    (when (seq groups)
      (apply list 'import
             (for [[p bs] groups]
               (list 'quote (apply list (symbol p) (map #(symbol (import-name %)) bs))))))))

(defn- import-text [form]
  ;; (import '(a B C) '(d E)) with one group per line
  (let [groups (rest form)]
    (str "(import "
         (str/join "\n        "
                   (for [[_ g] groups]
                     (str "'" (pr/pp g 9))))
         ")")))

(defn unit-text
  "The text of one converted file."
  [table pkg {:keys [forms source]}]
  (binding [pr/*resolve* (resolver table)]
    (str ";; Converted from " source " by arbace.j2c. Regenerate rather than edit.\n\n"
         (when (seq pkg) (str "(in-ns '" pkg ")\n\n"))
         (when-let [imp (imports-form table pkg forms)] (str (import-text imp) "\n\n"))
         (str/join "\n\n" (map pr/form-text forms))
         "\n")))

(defn topo-order
  "Units ordered so that supertypes in the same package come first, else by file name."
  [units]
  (let [by-class (into {} (for [u units, c (:classes u)] [c u]))
        units (sort-by :file units)]
    (loop [done [] seen #{} todo units]
      (if (empty? todo)
        done
        (let [visit (fn visit [[done seen] u path]
                      (if (or (seen (:file u)) (path (:file u)))
                        [done seen]
                        (let [deps (keep by-class (:supers u))
                              [done seen] (reduce #(visit %1 %2 (conj path (:file u))) [done seen] deps)]
                          (if (seen (:file u)) [done seen] [(conj done u) (conj seen (:file u))]))))
              [done seen] (visit [done seen] (first todo) #{})]
          (recur done seen (rest todo)))))))

(defn package-text
  "The namespace file of package `pkg`, loading the files `rel-paths` in order."
  [pkg rel-paths]
  (str ";; Converted by arbace.j2c: the namespace of Java package " pkg ".\n\n"
       "(ns " pkg ")\n\n"
       (str/join "\n" (for [p rel-paths] (str "(load " (pr/string-text p) ")")))
       "\n"))
