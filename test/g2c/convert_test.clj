(ns g2c.convert-test
  "Tests of g2c's converter (arbace.g2c.convert): the spec's worked examples (doc/go/SPEC.md
  §2, §14) and unit tests per section. Run by bin/g2c-convert-tests, which dumps the Go inputs
  first (test/g2c/convert/*.go, test/g2c/convert/units/*.go as single files; sort and
  unicode/utf8 from std) into the directory of the property g2c.dumps.

  The converted forms are written as text (arbace.g2c.layout) and read back with Arbace's
  reader; they are compared with the expected forms (test/g2c/convert/expected/*.clj) as
  data and metadata, without the layout's metadata (:line, :column, :go/end, :go/breaks),
  which the spec's examples leave out. An expected doc string ending in \"...\" is a prefix."
  (:require [arbace.test :refer [deftest is testing]]
            [arbace.g2c.convert :as cv]
            [arbace.g2c.layout :as layout]
            [arbace.g2c.lit :as lit]
            [arbace.g2c.main :as main]
            [arbace.g2c.read :as rd]
            [arbace.string :as str])
  (:import [java.io File]))

(def dumps (or (System/getProperty "g2c.dumps") ".tmp/g2c-convert-tests/dumps"))

(def expected-dir "test/g2c/convert/expected")

;;; Conversion through text

(defn convert
  "The forms of the dump at rel (under dumps), as read back from the text: {:package form
  :files [[form ...] ...]}, and the text of the first file."
  [rel & {:keys [positions] :or {positions :lines}}]
  (let [d (rd/read-dump (str dumps "/" rel))
        r (cv/convert-package d {:positions positions})]
    (binding [layout/*mode* positions]
      (let [texts (mapv #(layout/write-file (:forms %) (:comments %)) (:files r))]
        {:package (first (main/read-all (layout/text (:package r))))
         :files (mapv main/read-all texts)
         :texts texts
         :checks (mapv (fn [fl t] (main/check-text t (layout/prepare (:forms fl)))) (:files r) texts)}))))

(def layout-keys #{:line :column :go/end :go/breaks})

(defn- clean-meta [m] (not-empty (apply dissoc m layout-keys)))

(defn same
  "nil when actual matches expected (data and metadata without layout keys), else
  [path expected actual]."
  [e a path]
  (let [me (clean-meta (meta e)) ma (clean-meta (meta a))]
    (cond
      (and (string? e) (str/ends-with? e "...") (string? a))
      (when-not (str/starts-with? a (str/trimr (subs e 0 (- (count e) 3)))) [path e a])
      (and (seq? e) (seq? a) (= (count e) (count a)))
      (or (same me ma (conj path :meta))
          (some identity (map-indexed (fn [i [x y]] (same x y (conj path i))) (map vector e a))))
      (and (vector? e) (vector? a) (= (count e) (count a)))
      (or (same me ma (conj path :meta))
          (some identity (map-indexed (fn [i [x y]] (same x y (conj path i))) (map vector e a))))
      (and (map? e) (map? a) (= (set (keys e)) (set (keys a))))
      (some (fn [k] (same (get e k) (get a k) (conj path k))) (keys e))
      (and (symbol? e) (symbol? a) (= e a)) (same me ma (conj path :meta))
      (= e a) nil
      :else [path e a])))

(defn read-expected [name]
  (main/read-all (slurp (str expected-dir "/" name))))

(defn decl-key
  "A declaration's identity: head, name (the first spec's for a group), receiver type."
  [f]
  (let [[head & args] f
        args (remove string? args)
        a (first args)
        nm (cond (vector? a) (let [t (first a)] (if (seq? t) (second t) t))
                 (seq? a) (second a)
                 :else a)]
    [head nm (when (= head 'go/method) (:tag (meta (first (first (filter vector? args))))))]))

(defn decls [forms] (drop-while #(not= 'go/file (first %)) forms))

(defn check-decls
  "Each expected declaration equals the converted one of the same key."
  [expected actual]
  (let [by-key (into {} (map (juxt decl-key identity)) (rest (decls actual)))]
    (doseq [e expected]
      (let [a (by-key (decl-key e))]
        (is (some? a) (str "no declaration " (pr-str (decl-key e))))
        (when a
          (is (nil? (same e a [])) (str (decl-key e) ": " (pr-str (same e a [])))))))))

;;; The spec's worked examples

(deftest spec-examples
  (testing "§2 sort.Search"
    (let [r (convert "search.go.edn")]
      (is (every? nil? (:checks r)))
      (check-decls (read-expected "s02_search.clj") (first (:files r)))))
  (testing "§14.1 the sample"
    (let [r (convert "sample.go.edn")
          ds (rest (decls (first (:files r))))]
      (is (every? nil? (:checks r)))
      (is (= (count (read-expected "s14_1_sample.clj")) (count ds)))
      (doseq [[e a] (map vector (read-expected "s14_1_sample.clj") ds)]
        (is (nil? (same e a [])) (pr-str (same e a []))))
      (let [ep (first (read-expected "s14_1_package.clj"))
            ap (:package r)]
        (is (= "command-line-arguments" (second (drop-while #(not= :path %) ap))))
        (is (nil? (same (apply list (map #(if (= % "sample") "command-line-arguments" %) ep)) ap []))))
      (testing "the file header"
        (let [gf (first (decls (first (:files r))))]
          (is (= '(go/file "sample.go" :imports [[errors "errors"] [fmt "fmt"]]) gf))))))
  (testing "§14.2 unicode/utf8"
    (let [r (convert "std/unicode/utf8.edn")]
      (is (every? nil? (:checks r)))
      (check-decls (read-expected "s14_2_utf8.clj") (first (:files r)))))
  (testing "§14.3 sort"
    (let [r (convert "std/sort.edn")]
      (is (every? nil? (:checks r)))
      (check-decls (read-expected "s14_3_sort.clj") (apply concat (map #(cons (first (decls %)) (rest (decls %))) (:files r)))))))

;;; Units: each Go file of test/g2c/convert/units against its expected declarations

(def units ["s4_files" "s5_types" "s6_decls" "s7_code" "s8_literals"])

(deftest unit-files
  (doseq [u units]
    (testing u
      (let [r (convert (str "units/" u ".go.edn"))
            expected (read-expected (str "units/" u ".clj"))
            actual (decls (first (:files r)))]
        (is (every? nil? (:checks r)) (pr-str (:checks r)))
        (is (= (count expected) (count actual)) "declaration count")
        (doseq [[e a] (map vector expected actual)]
          (is (nil? (same e a [])) (pr-str (same e a []))))))))

(deftest layout-lines
  (testing "forms begin on their Go lines (§10.1)"
    (let [r (convert "units/s7_code.go.edn")
          fs (first (:files r))
          stmts (fn [f] (drop 3 f))]
      ;; Stmts is on Go line 15; its let on 16; `x, err = 2, nil` on 23
      (let [st (first (filter #(= 'Stmts (second %)) fs))]
        (is (= 15 (:line (meta st))))
        (is (= 138 (:go/end (meta st))))
        (is (= 16 (:line (meta (last st)))))))
    (let [r (convert "search.go.edn")
          f (first (filter #(= 'go/func (first %)) (first (:files r))))]
      ;; the final `return i` on Go line 19: a symbol with explicit :line
      (is (= 19 (:line (meta (last (last f)))))))))

(deftest positions-full
  (testing "mode :full writes positions and reads back"
    (let [r (convert "units/s7_code.go.edn" :positions :full)
          fs (first (:files r))
          mv (first (filter #(= 'Move (second %)) fs))]
      (is (every? nil? (:checks r)))
      (is (= {:name-pos [13 13]} (:go/pos (meta (second mv)))))
      (is (= [13 1] (:func (:go/pos (meta mv))))))))

;;; Literals and constant values (§8)

(deftest literals
  (is (= ["0x2A" 42] ((juxt :text :value) (lit/int-lit "0x2A"))))
  (is (= ["052" 42] ((juxt :text :value) (lit/int-lit "0o52"))))
  (is (= ["2r101010" 42] ((juxt :text :value) (lit/int-lit "0b101010"))))
  (is (= "1000" (:text (lit/int-lit "1_000"))))
  (is (= "1.0E10" (:text (lit/float-text-lit "1e10"))))
  (is (= "0.5" (:text (lit/float-text-lit ".5"))))
  (is (= "1E+400M" (:text (lit/float-text-lit "1e400"))))
  (is (= "1/4" (:text (lit/float-text-lit "0x1p-2"))))
  (is (= "16.0M" (:text (lit/float-text-lit "0x1p4"))))
  (is (= "0.1" (:text (lit/float-text-lit "0.1"))))
  (is (= "1E+1000000M" (:text (lit/float-text-lit "1e1000000"))))
  (is (= "\\a" (:text (lit/rune-form (lit/rune-value "'a'")))))
  (is (= "\\newline" (:text (lit/rune-form (lit/rune-value "'\\n'")))))
  (is (= '(rune "0x1F600") (let [[h n] (lit/rune-form 0x1F600)] (list h (:text n)))))
  (is (= "\"a\\\\b\"" (:text (lit/bytes-form (lit/string-bytes "`a\\b`")))))
  (is (= ["0xff" "\"\\u0000a\""] (map :text (rest (lit/bytes-form (lit/string-bytes "\"\\xff\\x00a\"")))))))

(deftest constant-values
  (let [t (comp :text #(apply lit/const-form %&))]
    (is (= "1.0" (t '(:float 1) :untyped-float)))
    ;; 1.0E100 denotes exactly 10^100 by §8.1's rule (the spec's §8.2 says otherwise)
    (is (= "1.0E100" (t (list :float (biginteger (.pow BigInteger/TEN 100))) :untyped-float)))
    (is (= "10000000000000000159028911097599180468360808563945281389781327557747838772170381060813469985856815104.0M"
           (t (list :float (biginteger (.toBigInteger (java.math.BigDecimal. 1.0E100)))) :untyped-float)))
    (is (= "1/3" (t '(:float 1/3) :untyped-float)))
    (is (= "0.1000000014901161" (t '(:float 13421773/134217728) :float)))
    (is (= \a (:value (lit/const-form 97 :rune))))
    (is (= 97 (:value (lit/const-form 97 :int))))
    (is (= '(complex "1.0" "0.0")
           (let [[h a b] (lit/const-form 1 :untyped-complex)] (list h (:text a) (:text b)))))
    (is (= '(binary-float "3" "100000")
           (let [[h a b] (lit/const-form '(:float 3 100000) :untyped-float)] (list h (:text a) (:text b)))))))
