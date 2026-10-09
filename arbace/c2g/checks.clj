(ns arbace.c2g.checks
  "c2g's checks of its output (C2G-SPEC §4.4, §8.3): duplicate Go names in a package or a
  method set, package-private methods that Go would let a subclass in another Java package
  override, and the report of candidate two-word races."
  (:require [arbace.string :as str]
            [arbace.classes.analyze :as a]
            [arbace.c2g.model :as m]
            [arbace.c2g.jrt :as jrt])
  (:import (arbace.asm Opcodes)))

(defn- sym-name [x] (when (symbol? x) (name x)))

(defn- recv-type
  "The receiver's type name of a go/method form: T for (* T) or T."
  [f]
  (let [params (first (filter vector? (drop 2 f)))
        tg (:tag (meta (first params)))]
    (cond (symbol? tg) (name tg)
          (and (seq? tg) (= '* (first tg))) (sym-name (second tg))
          :else nil)))

(defn declared-names
  "[kind name] of the Go declarations of forms: [:pkg n] for package-level names, [T m] for
  a method m of type T. init functions and blank names are left out."
  [forms]
  (for [f forms
        :when (and (seq? f) (symbol? (first f)))
        :let [h (name (first f))]
        k (case h
            ("func" "type" "var" "const")
            (let [x (second f)]
              (cond (symbol? x) [[:pkg (name x)]]
                    ;; groups (go/var [a e] [b e])
                    :else (for [sp (rest f) :when (and (vector? sp) (symbol? (first sp)))] [:pkg (name (first sp))])))
            "method" (when-let [t (recv-type f)] [[t (sym-name (second f))]])
            nil)
        :when (and (second k) (not (#{"init" "_"} (second k))))]
    k))

(defn collisions
  "Duplicate Go names among the generated files (files: {[pkg file] forms}) and jrt's
  hand-written declarations (scan; the stand-in files left out, as translated classes replace
  them): [{:pkg :name :sources [...]}]."
  [files scan]
  (let [gen (for [[[pkg fname] forms] files
                  k (declared-names forms)]
              [[pkg k] fname])
        hw (for [[fname fs] (:files scan)
                 :when (not (str/starts-with? fname "standin_"))
                 k (concat (for [n (concat (:funcs fs) (:vars fs) (:consts fs) (keys (:types fs)))] [:pkg n])
                           (for [[t ms] (:methods fs) mn ms] [t mn]))
                 :when (not (#{"init" "_"} (second k)))]
             [[:jrt k] (str "jrt/" fname)])
        by (group-by first (concat gen hw))]
    (vec (for [[[pkg [kind n]] srcs] (sort-by (comp str key) by)
               :when (> (count srcs) 1)]
           {:pkg pkg :name (if (= :pkg kind) n (str "(" kind ")." n)) :sources (vec (sort (map second srcs)))}))))

(defn- jpkg [n] (let [i (.lastIndexOf ^String n "/")] (if (neg? i) "" (subs n 0 i))))

(defn package-private-overrides
  "Package-private instance methods of translated classes that a translated subclass in
  another Java package declares again with the same name and descriptor: Java does not
  override them (JVMS 5.4.5), Go's method sets would (C2G-SPEC §4.4). [{:method :hidden-by}]."
  [T]
  (vec
    (for [n (sort T)
          :let [d (a/decl n)]
          :when (and d (not (m/interface? n)))
          mm (:methods d)
          :when (and (not= "<init>" (:name mm)) (not (m/static? mm)) (not (m/private? mm))
                     (zero? (bit-and (:flags mm) (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_PROTECTED))))
          s (sort T)
          :when (and (not= s n) (not= (jpkg s) (jpkg n)) (some #{n} (rest (m/superclass-chain s))))
          sm (:methods (a/decl s))
          :when (and (= (:name sm) (:name mm)) (= (:desc sm) (:desc mm)) (not (m/static? sm)))]
      {:method (str n "." (:name mm) (:desc mm)) :hidden-by s})))

(defn race-report
  "The candidate two-word races (§8.3) as {class [\"field (writers...)\"]}."
  [races]
  (into (sorted-map)
        (for [[c rs] (group-by first races)]
          [c (vec (sort (for [[[_ f d st] ws] (group-by #(subvec % 0 4) rs)]
                          (str (when st "static ") f " " d " written in " (str/join ", " (sort (distinct (map peek ws))))))))])))
