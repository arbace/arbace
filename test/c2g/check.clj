(ns c2g.check
  "bin/c2g-check: the differential check of c2g's translation (doc/go/C2G-NOTES.md, \"The
  harness\"). It translates the classes the oracle's class scripts call (test/oracle/classes,
  doc/go/ORACLE.md) with c2g, writes a Go program running every recorded step of the
  selected scripts with the translated methods (direct calls, by c2g's names), builds it for
  linux/amd64 and linux/arm64 (bin/g2c build), runs it (arm64 under qemu-aarch64) and compares
  each step's result with the JVM's recorded one (test/oracle/expected/classes/*.edn).

  A step whose method is not translated (outside the slice, or a stub) is counted as
  unavailable; a step whose expected result is a printed collection (:pr) needs the printer
  (RT.printString) and is counted apart until it is translated; the JVM's helpful
  NullPointerException messages are accepted as a null message (V11)."
  (:require [arbace.string :as str]
            [arbace.java.io :as io]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.classes.emit :as e]
            [arbace.c2g.main :as c2g]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.decls :as d]
            [arbace.c2g.out :as out]
            [arbace.c2g.world :as w]))

(defn- tag [sym type] (with-meta sym {:tag type}))

(defn read-expected [f] (first (w/read-forms f)))

;; ---------------------------------------------------------------------------------------
;; Java names and descriptors

(def prim-names {"int" "I" "long" "J" "double" "D" "float" "F" "boolean" "Z" "char" "C" "short" "S" "byte" "B" "void" "V"})

(defn name->desc [^String n]
  (cond (str/ends-with? n "[]") (str "[" (name->desc (subs n 0 (- (count n) 2))))
        (prim-names n) (prim-names n)
        :else (str "L" (str/replace n "." "/") ";")))

(defn- internal [n] (str/replace n "." "/"))

(defn find-method
  "The method of class cls (internal) named mname with parameter descriptors ps, as
  Class.getMethods finds it (public, inherited included; not a bridge): {:owner :name :desc
  :flags}."
  [cls mname ps static?]
  (let [cands (for [c (env/all-supertypes cls)
                    mm (:methods (env/info c))
                    :when (= mname (:name mm))
                    :when (= (vec ps) (first (t/parse-method-desc (:desc mm))))
                    :when (= (boolean static?) (m/static? mm))
                    :when (not (m/has? (:flags mm) 0x40))]
                (assoc mm :owner c))]
    (first cands)))

(defn find-ctor [cls ps]
  (first (for [mm (:methods (env/info cls))
               :when (= "<init>" (:name mm))
               :when (= (vec ps) (first (t/parse-method-desc (:desc mm))))]
           (assoc mm :owner cls))))

(defn step-member
  "The member a recorded step calls: [kind member] or nil."
  [s]
  (let [cls (some-> (:class s) internal)
        ps (mapv name->desc (:sig s))]
    (when cls
      (case (:op s)
        :static [:static (find-method cls (:name s) ps true)]
        :invoke [:invoke (let [mm (find-method cls (:name s) ps false)]
                           ;; a method jrt has on the step's class rather than on the declaring
                           ;; class (StringBuilder's, declared by AbstractStringBuilder)
                           (if (and mm (not= cls (:owner mm)) (m/hand-written? cls) (m/hand-written? (:owner mm))
                                    (not (m/hw-method-exists? (:owner mm) mm)) (m/hw-method-exists? cls mm))
                             (assoc mm :owner cls)
                             mm))]
        :new [:new (find-ctor cls ps)]
        (:get :get-static) [(:op s) (when-let [f (env/find-field cls (:name s))] (assoc f :owner (or (:owner f) cls)))]
        nil))))

(defn roots-of
  "c2g root specs for the steps of expected files: Class#name of each called member."
  [cases]
  (distinct
    (for [s cases
          :let [[k mm] (step-member s)]
          :when (and mm (a/decl (:owner mm)))]
      (str (:owner mm) "#" (if (= k :new) "<init>" (:name mm))))))

;; ---------------------------------------------------------------------------------------
;; Go code for the steps

(defn- units-lit [^String s]
  (list 'jrt/NewStringUTF16 (apply list 'lit '(slice uint16) (map int s))))

(defn- go-type [d] (m/go-type :main d))

(defn- go-type-main
  "Go types for the main package: arbace/lang's types qualified as lang/."
  [d]
  (let [g (m/go-type :lang d)]
    (letfn [(q [x] (cond (symbol? x) (if (namespace x) x
                                         (if (#{'any 'bool 'int8 'int16 'int32 'int64 'uint16 'float32 'float64} x) x
                                             (symbol "lang" (name x))))
                         (seq? x) (if (= '* (first x)) (list '* (q (second x))) (apply list (map q x)))
                         :else x))]
      (q g))))

(defn- class-sym-main [n suffix]
  (let [p (m/pkg n)] (symbol (nm/pkg-name p) (str (m/go-name n) suffix))))

(defn- box [d x]
  (let [w (t/box-of d)]
    (list (class-sym-main w (str "_" (nm/method-base "valueOf" (str "(" d ")L" w ";")))) x)))

(defn- prim-lit [d v]
  (case d
    "Z" (boolean v)
    "C" (long (if (char? v) (int v) v))
    ("D" "F") (let [x (double v)
                    g (cond (Double/isNaN x) (list 'math/NaN)
                            (Double/isInfinite x) (list 'math/Inf (if (pos? x) 1 -1))
                            (and (zero? x) (neg? (Math/copySign 1.0 x))) (list 'math/Copysign 0.0 -1.0)
                            :else x)]
                (if (and (= d "F") (seq? g)) (list 'conv 'float32 g) g))
    (long v)))

(declare class-val-main)

(defn- need!
  "An argument needs member [c name desc] translated: else the step is unavailable."
  [c name desc]
  (when-not (and (m/translated? c) (contains? arbace.c2g.decls/*reached* [c name desc]))
    (throw (ex-info (str "needs " c "." name) {::unavailable true}))))

(defn- arg-value
  "The Go expression of a recorded argument a for a parameter of descriptor d."
  [a d]
  (cond
    (map? a)
    (cond
      (contains? a :ref) (let [x (list 'aget 'B (:ref a))]
                           (if (t/prim? d)
                             (list 'assert (go-type-main d) (list 'unboxTo (class-val-main d) x))
                             (if (= d "Ljava/lang/Object;") x
                                 (list (list 'inst 'jrt/As (go-type-main d)) x))))
      (contains? a :box) (let [bd (prim-names (:box a))
                               x (box bd (prim-lit bd (:value a)))]
                           (if (t/prim? d)
                             (prim-lit d (:value a))
                             x))
      (contains? a :char) (if (t/prim? d) (long (:char a)) (box "C" (long (:char a))))
      (contains? a :static) (let [n (internal (:static a))
                                  f (env/find-field n (:name a))
                                  o (or (:owner f) n)
                                  g (str (m/go-name o) "_" (nm/munge-name (:name a)))]
                              (when-not (and f (m/in-world? o) (m/desc-in-world? (:desc f))
                                             (or (m/translated? o)
                                                 (contains? (:vars (:jrt m/*w*)) g)
                                                 (contains? (:consts (:jrt m/*w*)) g)))
                                (throw (ex-info (str "needs " o "." (:name a)) {})))
                              (class-sym-main o (str "_" (nm/munge-name (:name a)))))
      (contains? a :biginteger) (do (need! "java/math/BigInteger" "<init>" "(Ljava/lang/String;)V")
                                    (list 'jrt/BigInteger_New_String (units-lit (:biginteger a))))
      (contains? a :bigdecimal) (do (need! "java/math/BigDecimal" "<init>" "(Ljava/lang/String;)V")
                                    (list 'jrt/BigDecimal_New_String (units-lit (:bigdecimal a))))
      (contains? a :array) (let [et (name->desc (:array a))
                                 items (map #(arg-value % et) (:items a))]
                             (if (t/prim? et)
                               (apply list (symbol "jrt" (str (subs (m/prim-array-go et) 0 (- (count (m/prim-array-go et)) 5)) "ArrayOf")) items)
                               (apply list 'jrt/RefArrayOf (class-val-main et) (map #(list 'toAny %) items))))
      :else (throw (ex-info (str "c2g-check: argument " (pr-str a)) {})))
    (nil? a) nil
    (string? a) (if (t/prim? d) (throw (ex-info "string for primitive" {})) (units-lit a))
    (boolean? a) (if (t/prim? d) a (box "Z" a))
    (char? a) (if (t/prim? d) (long (int a)) (box "C" (long (int a))))
    (integer? a) (if (t/prim? d) (prim-lit d a) (box "J" (long a)))
    (float? a) (if (t/prim? d) (prim-lit d a) (box "D" (prim-lit "D" a)))
    :else (throw (ex-info (str "c2g-check: argument " (pr-str a)) {}))))

(defn class-val-main [d]
  (cond (t/prim? d) (symbol "jrt" (str "Prim_" ({"I" "int" "J" "long" "D" "double" "F" "float" "Z" "boolean" "C" "char" "S" "short" "B" "byte"} d)))
        (t/array? d) (list '.ArrayClass (class-val-main (t/elem-type d)))
        (= d "Ljava/lang/Object;") 'jrt/Object_class
        :else (class-sym-main (t/desc->internal d) "_class")))

(defn- recv-type [owner]
  (cond (= owner "java/lang/Object") 'any
        :else (go-type-main (str "L" owner ";"))))

(defn- callable?
  "Does the member exist in Go in this world (a translated class's member in the world, or
  jrt's)?"
  [kind mm]
  (and mm
       (m/in-world? (:owner mm))
       (if (= kind :new) (m/mdesc-in-world? (e/ctor-real-desc (:owner mm) mm)) (m/mdesc-in-world? (:desc mm)))
       (or (m/translated? (:owner mm))
           (= (:owner mm) "java/lang/Object")
           (case kind
             :static (contains? (:funcs (:jrt m/*w*)) (str (m/go-name (:owner mm)) "_" (nm/method-base (:name mm) (:desc mm))))
             :new (contains? (:funcs (:jrt m/*w*)) (nm/new-name (m/go-name (:owner mm)) (:desc mm)))
             :invoke (m/hw-method-exists? (:owner mm) mm)
             false))))

(def ^:private object-helpers
  {["equals" "(Ljava/lang/Object;)Z"] "Equals" ["hashCode" "()I"] "HashCode"
   ["toString" "()Ljava/lang/String;"] "ToString" ["getClass" "()Ljava/lang/Class;"] "GetClass"})

(defn step-code
  "Go statements for one step, or [:unavailable why]."
  [s]
  (let [[kind mm] (step-member s)]
    (cond
      (nil? mm) [:unavailable "member not found"]
      (#{:get-static :get} kind)
      (let [f mm o (:owner f)]
        (if-not (and (m/in-world? o) (m/desc-in-world? (:desc f))
                     (or (m/translated? o)
                         (and (= kind :get-static)
                              (let [g (str (m/go-name o) "_" (nm/munge-name (:name f)))]
                                (or (contains? (:vars (:jrt m/*w*)) g) (contains? (:consts (:jrt m/*w*)) g))))))
          [:unavailable (str "field " o "." (:name f))]
          (let [place (if (= kind :get-static)
                        (class-sym-main o (str "_" (nm/munge-name (:name f))))
                        (list (symbol (str ".-" (nm/field-name (:name f))))
                              (if (m/leaf? o) (list 'assert (recv-type o) (list 'nonNil (list 'aget 'B (:target s))))
                                  (list (symbol (str ".Self_" (m/go-name o))) (list 'assert (recv-type o) (list 'nonNil (list 'aget 'B (:target s))))))))
                d (:desc f)]
            [:ok (concat
                   (when (and (= kind :get-static) (m/translated? o) (not (m/trivial-init? o)))
                     [(list (class-sym-main o "_Init"))])
                   [(list 'let [(tag 'res (go-type-main d)) place]
                          (list 'record (:i s) (:bind s "")
                                (if (t/prim? d) (str "prim:" d) "ref")
                                (if (t/prim? d) 'res (list 'toAny 'res))))])])))
      (not (#{:static :invoke :new} kind)) [:unavailable (str "op " (:op s))]
      (not (callable? kind mm)) [:unavailable (str "not translated: " (:owner mm) "." (:name mm))]
      :else
      (let [[ps r] (t/parse-method-desc (:desc mm))
            r (if (= kind :new) (str "L" (:owner mm) ";") r)
            args (map arg-value (:args s) ps)
            owner (:owner mm)
            base (nm/method-base (:name mm) (:desc mm))
            call (case kind
                   :static (apply list (class-sym-main owner (str "_" base)) args)
                   :new (apply list (symbol (nm/pkg-name (m/pkg owner)) (nm/new-name (m/go-name owner) (e/ctor-real-desc owner mm))) args)
                   :invoke (let [recv (list 'aget 'B (:target s))]
                             (if-let [h (object-helpers [(:name mm) (:desc mm)])]
                               (apply list (symbol "jrt" h) recv args)
                               (apply list (symbol (str "." base))
                                       (list 'assert (recv-type owner) (list 'nonNil recv)) args))))
            rd (if (= r "V") "V" r)]
        [:ok
         (concat
           (if (= rd "V")
             [call (list 'record (:i s) (:bind s "") "void" nil)]
             [(list 'let [(tag 'res (go-type-main rd)) call]
                    (list 'record (:i s) (:bind s "")
                          (case rd
                            ("I" "J" "S" "B" "C" "Z" "F" "D") (str "prim:" rd)
                            "ref")
                          (if (t/prim? rd) 'res (list 'toAny 'res))))]))]))))

(def support-forms
  "The Go helpers of the check program (record, unboxTo, toAny, nonNil, runStep)."
  '[(go/var ^{:tag (map string any)} B (make (map string any)))
    (go/func nonNil ^any [^any x]
      (when (== x nil) (panic (jrt/Thrown (jrt/NPE))))
      x)
    (go/func toAny ^any [^any x]
      (when (== x nil) (return nil))
      (let [v (reflect/ValueOf x)]
        (when (and (== (.Kind v) reflect/Pointer) (.IsNil v)) (return nil)))
      x)
    (go/func unboxTo ^any [^{:tag (* jrt/Class)} c ^any x]
      (let [(values v ok) (jrt/Unbox c x)]
        (when (not ok) (panic (jrt/Thrown (jrt/NPE))))
        v))
    (go/func units ^string [^{:tag (* jrt/String)} s]
      (when (== s nil) (return "-"))
      (let [b (lit strings/Builder)]
        (range [i u (.UTF16 s)]
          (when (> i 0) (.WriteString b ","))
          (.WriteString b (strconv/FormatUint (conv uint64 u) 16)))
        (when (== (len (.UTF16 s)) 0) (return "="))
        (.String b)))
    (go/func record [^int i ^string bind ^string kind ^any v]
      (when (!= bind "")
        (if (strings/HasPrefix kind "prim:") (aset B bind (jrt/Box v)) (aset B bind v)))
      (switch kind
        (case ["void"] (fmt/Printf "@@c2g %d W\n" i))
        (case ["prim:I"] (fmt/Printf "@@c2g %d V java.lang.Integer %d\n" i v))
        (case ["prim:J"] (fmt/Printf "@@c2g %d V java.lang.Long %d\n" i v))
        (case ["prim:S"] (fmt/Printf "@@c2g %d V java.lang.Short %d\n" i v))
        (case ["prim:B"] (fmt/Printf "@@c2g %d V java.lang.Byte %d\n" i v))
        (case ["prim:C"] (fmt/Printf "@@c2g %d V java.lang.Character %d\n" i v))
        (case ["prim:Z"] (fmt/Printf "@@c2g %d V java.lang.Boolean %v\n" i v))
        (case ["prim:D"] (fmt/Printf "@@c2g %d V java.lang.Double %x\n" i (math/Float64bits (assert float64 v))))
        (case ["prim:F"] (fmt/Printf "@@c2g %d V java.lang.Float %x\n" i (math/Float32bits (assert float32 v))))
        (default (observe i v))))
    (go/func observe [^int i ^any v]
      (when (== v nil)
        (fmt/Printf "@@c2g %d N\n" i)
        (return))
      (let [c (.GoName (jrt/GetClass v))]
        (switch c
          (case ["java.lang.String"] (fmt/Printf "@@c2g %d S %s %s\n" i c (units (assert (* jrt/String) v))))
          (case ["java.lang.Integer" "java.lang.Long" "java.lang.Short" "java.lang.Byte" "java.lang.Character"
                 "java.lang.Boolean" "java.lang.Double" "java.lang.Float"]
            (let [(values p ok) (jrt/Unbox (primOf c) v)]
              (when (not ok) (fmt/Printf "@@c2g %d T %s\n" i c) (return))
              (type-switch [x p]
                (case [float64] (fmt/Printf "@@c2g %d V %s %x\n" i c (math/Float64bits x)))
                (case [float32] (fmt/Printf "@@c2g %d V %s %x\n" i c (math/Float32bits x)))
                (default (fmt/Printf "@@c2g %d V %s %v\n" i c x)))))
          (case ["java.math.BigInteger" "java.math.BigDecimal"] (fmt/Printf "@@c2g %d S %s %s\n" i c (units (jrt/ToString v))))
          (default
            (if (printable c v)
              (fmt/Printf "@@c2g %d P %s %s\n" i c (printed v))
              (fmt/Printf "@@c2g %d T %s\n" i c))))))
    (go/func throws [^int i ^{:tag jrt/Throwable_I} e]
      (let [b (lit strings/Builder)]
        (for [n 0] (and (!= e nil) (< n 8)) (inc! n)
          (when (> n 0) (.WriteString b " ; "))
          (.WriteString b (.GoName (jrt/GetClass e)))
          (.WriteString b " ")
          (.WriteString b (units (.GetMessage__String e)))
          (set! e (.GetCause__Throwable e)))
        (fmt/Printf "@@c2g %d X %s\n" i (.String b))))
    (go/func primOf ^{:tag (* jrt/Class)} [^string c]
      (switch c
        (case ["java.lang.Integer"] (return jrt/Prim_int))
        (case ["java.lang.Long"] (return jrt/Prim_long))
        (case ["java.lang.Short"] (return jrt/Prim_short))
        (case ["java.lang.Byte"] (return jrt/Prim_byte))
        (case ["java.lang.Character"] (return jrt/Prim_char))
        (case ["java.lang.Boolean"] (return jrt/Prim_boolean))
        (case ["java.lang.Double"] (return jrt/Prim_double))
        (default (return jrt/Prim_float))))
    (go/func runStep [^int i ^string bind ^{:tag (func [])} f]
      ;; a Go panic that is no Java exception (c2g's own) ends the step, not the program
      (defer ((fn []
                (let [r (recover)]
                  (when (!= r nil)
                    (fmt/Printf "@@c2g %d X go.panic %s\n" i (units (jrt/Str (fmt/Sprint r)))))))))
      (let [exc ((fn [] :results [^jrt/Throwable_I exc] (defer (jrt/Catch (addr exc))) (f) (return)))]
        (when (!= exc nil)
          (when (!= bind "") (aset B bind nil))
          (throws i exc))))])

(defn- unreached-call?
  "Does the step call a translated method c2g did not reach (a stub)?"
  [s]
  (let [[kind mm] (step-member s)]
    (and mm (#{:static :invoke :new} kind) (m/translated? (:owner mm))
         (not (contains? arbace.c2g.decls/*reached* [(:owner mm) (if (= kind :new) "<init>" (:name mm)) (:desc mm)])))))

(defn- refs-of [s]
  (concat (when (:target s) [(:target s)])
          (letfn [(r [a] (cond (and (map? a) (contains? a :ref)) [(:ref a)]
                               (and (map? a) (contains? a :items)) (mapcat r (:items a))
                               :else nil))]
            (mapcat r (:args s)))))

(defn- printer-forms
  "printable and printed: the oracle's observe (doc/go/ORACLE.md): a collection, seq,
  keyword, symbol, number or map entry is printed with RT.printString (when c2g translated
  it), not the lazy seqs (printing them would realize them)."
  []
  (let [have (fn [n] (and (m/translated? n) (m/in-world? n)))
        printer? (contains? arbace.c2g.decls/*reached* ["arbace/lang/RT" "printString" "(Ljava/lang/Object;)Ljava/lang/String;"])
        tests (for [n ["arbace/lang/IPersistentCollection" "arbace/lang/ISeq" "arbace/lang/Keyword" "arbace/lang/Symbol"
                       "java/lang/Number" "arbace/lang/MapEntry"]
                    :when (m/in-world? n)]
                (list (class-sym-main n "_InstanceOf") 'v))]
    [(list 'go/func 'printable (with-meta [(tag 'c 'string) (tag 'v 'any)] {:tag 'bool})
           (if (and printer? (seq tests))
             (list 'return (list 'and
                                 (list 'not (list 'aget (list 'lit '(map string bool)
                                                              ["arbace.lang.LazySeq" true] ["arbace.lang.Iterate" true]
                                                              ["arbace.lang.Cycle" true] ["arbace.lang.Repeat" true])
                                                  'c))
                                 (apply list 'or tests)))
             (list 'return false)))
     (if printer?
       '(go/func printed ^string [^any v]
          (let [(values s exc) ((fn [] :results [^{:tag (* jrt/String)} s ^jrt/Throwable_I exc]
                                  (defer (jrt/Catch (addr exc)))
                                  (set! s (lang/RT_PrintString_O__String v))
                                  (return)))]
            (when (!= exc nil) (return (units (jrt/Str (+ "#print-failed " (.GoName (jrt/GetClass exc)))))))
            (units s)))
       '(go/func printed ^string [^any v] "-"))]))

(defn program-forms
  "The main package's file: one function per step, main running them in order. A step using
  a binding an unavailable step should have made is unavailable too."
  [cases]
  (let [tainted (atom #{})
        steps (doall
                (for [s cases]
                  (let [[k body] (try (step-code s) (catch Exception ex [:unavailable (str "c2g-check: " (.getMessage ex))]))
                        [k body] (if (and (= k :ok) (unreached-call? s)) [:unavailable "a stub"] [k body])
                        [k body] (if-let [t (and (= k :ok) (some @tainted (refs-of s)))]
                                   [:unavailable (str "uses " t ", unavailable")] [k body])]
                    (when (and (not= k :ok) (:bind s)) (swap! tainted conj (:bind s)))
                    (when (and (= k :ok) (:bind s)) (swap! tainted disj (:bind s)))
                    {:i (:i s) :bind (:bind s "") :k k :body body})))]
    (concat
      support-forms
      (printer-forms)
      (for [st steps :when (= :ok (:k st))]
        (apply list 'go/func (symbol (str "step" (:i st))) [] (:body st)))
      [(apply list 'go/func 'main []
              (for [st steps]
                (if (= :ok (:k st))
                  (list 'runStep (:i st) (:bind st) (symbol (str "step" (:i st))))
                  (list 'fmt/Printf "@@c2g %d U %s\n" (:i st) (str/replace (str (:body st)) "\n" " ")))))])))

(defn write-program! [prog-dir cases]
  (let [dir (str prog-dir "/go/arbace/cmd")]
    (io/make-parents (io/file dir "x"))
    (spit (str dir "/c2gcheck.clj")
          (str (out/form-text '(ns go.arbace.cmd.c2gcheck (:require [arbace.go :as go]))) "\n"
               (out/form-text '(go/package main :path "arbace/cmd/c2gcheck" :files ["main.go"])) "\n"
               (out/form-text '(load "c2gcheck/main")) "\n"))
    (io/make-parents (io/file dir "c2gcheck" "x"))
    (spit (str dir "/c2gcheck/main.clj")
          (str "(in-ns 'go.arbace.cmd.c2gcheck)\n"
               (out/form-text '(go/file "main.go" :imports [[fmt "fmt"] [math "math"] [reflect "reflect"] [strconv "strconv"]
                                                            [strings "strings"] [jrt "arbace/jrt"] [lang "arbace/lang"]]))
               "\n"
               (str/join "\n" (map out/form-text (program-forms cases)))
               "\n"))))

;; ---------------------------------------------------------------------------------------
;; comparing

(defn- parse-units [s]
  (cond (= s "-") nil
        (= s "=") ""
        :else (apply str (map #(char (Integer/parseInt % 16)) (str/split s #",")))))

(def ^:private normalizers
  "The oracle runner's (test/oracle/runner.clj): what varies between runs and implementations."
  [[#"(#object\[[^ \]]+ )0x[0-9a-f]+" "$10xN"]
   [#"([A-Za-z_$][\w$.]*;?)@[0-9a-f]{4,}\b" "$1@N"]
   [#"__\d+__auto__" "__N__auto__"]
   [#"\bG__\d+" "G__N"]
   [#"\$eval\d+" "\\$evalN"]
   [#"\beval\d+([/$])" "evalN$1"]
   [#"(\w)--\d+\b" "$1--N"]
   [#"(DynamicClassLoader )@[0-9a-f]+" "$1@N"]
   [#"\b(fn|reify|let|loop|p\d+|rest|x|y|seq|map|vec|deftype|cond|nth|first|ex|and|or)__\d+" "$1__N"]])

(defn- normalize [x]
  (if (string? x) (reduce (fn [s [re rep]] (str/replace s re rep)) x normalizers) x))

(defn decode-line
  "A result line of the Go program as {:i :result|:throws|:unavailable}."
  [line]
  (let [[_ i k & more] (str/split line #" " 4)
        i (parse-long i)
        rest-s (first more)]
    (case k
      "W" {:i i :result {:type "void"}}
      "N" {:i i :result {:type "nil"}}
      "U" {:i i :unavailable (str/join " " more)}
      "T" {:i i :result {:type (first more)}}
      "V" (let [[ty v] (str/split rest-s #" ")]
            {:i i :result {:type ty :raw v}})
      "S" (let [[ty v] (str/split rest-s #" ")]
            {:i i :result {:type ty :value (normalize (parse-units v))}})
      "P" (let [[ty v] (str/split rest-s #" ")]
            {:i i :result {:type ty :pr (normalize (parse-units v))}})
      "X" {:i i :throws (vec (for [part (str/split rest-s #" ; ")]
                               (let [[c m] (str/split part #" " 2)] [c (normalize (parse-units m))])))}
      {:i i :bad line})))

(defn- value-match? [expected got]
  (let [ty (:type expected) ev (:value expected) raw (:raw got)]
    (case ty
      ("java.lang.Integer" "java.lang.Long" "java.lang.Short" "java.lang.Byte")
      (= (long ev) (parse-long raw))
      "java.lang.Character" (= (if (map? ev) (:char ev) (int ev)) (parse-long raw))
      "java.lang.Boolean" (= ev (= raw "true"))
      "java.lang.Double" (= (Double/doubleToLongBits (double ev)) (Double/doubleToLongBits (Double/longBitsToDouble (Long/parseUnsignedLong raw 16))))
      "java.lang.Float" (= (Float/floatToIntBits (float ev)) (Float/floatToIntBits (Float/intBitsToFloat (Integer/parseUnsignedInt raw 16))))
      false)))

(defn- npe-helpful? [[c m]]
  (and (= c "java.lang.NullPointerException") m (or (str/includes? m "because") (str/starts-with? m "Cannot "))))

(defn compare-case
  "{:status :pass|:fail|:unavailable|:pr :why} of a step against its expected record."
  [exp got]
  (cond
    (nil? got) {:status :fail :why "no result"}
    (:unavailable got) {:status :unavailable :why (:unavailable got)}
    (:throws exp)
    (if (and (:throws got)
             (= (count (:throws exp)) (count (:throws got)))
             (every? true? (map (fn [[ec em :as e] [gc gm]]
                                  (and (= ec gc) (or (= em gm) (and (npe-helpful? e) (nil? gm)))))
                                (:throws exp) (:throws got))))
      {:status :pass}
      {:status :fail :why (str "expected throws " (pr-str (:throws exp)) " got " (pr-str (or (:throws got) (:result got))))})
    (:throws got) {:status :fail :why (str "threw " (pr-str (:throws got)) " expected " (pr-str (:result exp)))}
    :else
    (let [er (:result exp) gr (:result got)]
      (cond
        (contains? er :pr) (cond
                             (not= (:type er) (:type gr)) {:status :fail :why (str "type " (:type gr) " expected " (:type er))}
                             (not (contains? gr :pr)) {:status :pr}
                             (= (:pr er) (:pr gr)) {:status :pass}
                             :else {:status :fail :why (str "printed " (pr-str (:pr gr)) " expected " (pr-str (:pr er)))})
        (not= (:type er) (:type gr)) {:status :fail :why (str "type " (pr-str (:type gr)) " expected " (pr-str (:type er)))}
        (contains? er :value)
        (cond
          (contains? gr :raw) (if (value-match? er gr) {:status :pass} {:status :fail :why (str "value " (:raw gr) " expected " (pr-str (:value er)))})
          :else (if (= (:value er) (:value gr)) {:status :pass} {:status :fail :why (str "value " (pr-str (:value gr)) " expected " (pr-str (:value er)))}))
        :else {:status :pass}))))

(defn compare-run [cases output]
  (let [got (into {} (for [l (str/split-lines output) :when (str/starts-with? l "@@c2g ")]
                       (let [r (decode-line l)] [(:i r) r])))]
    (for [s cases] (assoc (compare-case s (get got (:i s))) :i (:i s) :src (:src s)))))

;; ---------------------------------------------------------------------------------------
;; fixtures (test/c2g/fixtures): recorded on this JVM, in-process

(def fixtures-dir "test/c2g/fixtures")

(defn- fixture-observe [v]
  (cond (nil? v) {:type "nil"}
        (or (string? v) (instance? Boolean v) (instance? Integer v) (instance? Long v) (instance? Double v))
        {:type (.getName (class v)) :value v}
        :else {:type (.getName (class v))}))

(defn- fixture-chain [^Throwable e]
  (loop [e e acc [] n 0]
    (if (or (nil? e) (= n 8)) acc (recur (.getCause e) (conj acc [(.getName (class e)) (.getMessage e)]) (inc n)))))

(defn fixture-expected
  "{class-simple-name {:cases [...]}} of the fixtures: each public static method without
  parameters whose name starts with t, in name order, run on this JVM (the fixtures loaded
  here by load-file) and recorded as the oracle's class scripts are."
  []
  (let [files (sort (filter #(str/ends-with? % ".clj") (map #(.getPath ^java.io.File %) (.listFiles (io/file fixtures-dir)))))
        nsname 'c2g.fixtures]
    (binding [*ns* (create-ns nsname)]
      (refer 'arbace.core)
      (doseq [f files] (load-file f)))
    (let [tops (for [f files
                     cf (w/read-forms f)
                     :when (and (seq? cf) (= 'do (first cf)))
                     dc (rest cf)
                     :when (and (seq? dc) (= 'defclass (first dc)))]
                 (name (second dc)))]
      (into (array-map)
            (for [cn tops
                  :let [^Class c (ns-resolve (the-ns nsname) (symbol cn))
                        ms (->> (.getDeclaredMethods c)
                                (filter (fn [^java.lang.reflect.Method m]
                                          (and (re-matches #"t[A-Z].*" (.getName m)) (zero? (.getParameterCount m))
                                               (java.lang.reflect.Modifier/isStatic (.getModifiers m))
                                               (java.lang.reflect.Modifier/isPublic (.getModifiers m)))))
                                (sort-by #(.getName ^java.lang.reflect.Method %)))]
                  :when (seq ms)]
              [cn {:cases (vec (map-indexed
                                 (fn [i ^java.lang.reflect.Method m]
                                   (let [base {:i (inc i) :op :static :class (str nsname "." cn) :name (.getName m) :sig []
                                               :src (str "(" cn "/" (.getName m) ")")}]
                                     (try (assoc base :result (fixture-observe (.invoke m nil (object-array 0))))
                                          (catch java.lang.reflect.InvocationTargetException e
                                            (assoc base :throws (fixture-chain (.getCause e)))))))
                                 ms))}])))))

;; ---------------------------------------------------------------------------------------

(defn sh [& args]
  (let [pb (ProcessBuilder. ^java.util.List (map str args))
        _ (.redirectErrorStream pb true)
        p (.start pb)
        o (slurp (.getInputStream p))]
    [(.waitFor p) o]))

(defn -main [& args]
  (let [[files c2g-args] (split-with #(not= "--" %) args)
        c2g-args (rest c2g-args)
        arches (or (some-> (System/getenv "C2G_ARCH") (str/split #",")) ["amd64" "arm64"])
        out-dir (or (System/getenv "C2G_OUT") ".tmp/c2g/check")
        files (if (seq files) files (map #(str/replace % ".edn" "")
                                         (concat (sort (map #(.getName ^java.io.File %) (.listFiles (io/file "test/oracle/expected/classes")))) ["fixtures.edn"])))
        fixtures? (some #{"fixtures"} files)
        files (remove #{"fixtures"} files)
        expected (into (array-map) (concat (for [f files] [f (read-expected (str "test/oracle/expected/classes/" f ".edn"))])
                                           (when fixtures? (fixture-expected))))
        c2g-args (if fixtures?
                   (concat c2g-args ["--input" fixtures-dir] (when (some #{"--slice"} c2g-args) ["--slice" "^c2g/"]))
                   c2g-args)
        ;; each file runs alone (bindings are per script): one program per file, built once
        all-cases (vec (mapcat (fn [[f x]] (map #(assoc % ::file f) (:cases x))) expected))
        opts (c2g/parse-args (concat ["--out" out-dir] c2g-args))
        ;; roots: what the steps call; found in the world, so the run computes them itself
        results (atom {})]
    (c2g/run (assoc opts
                    :root-fn (fn [] (concat (roots-of all-cases)
                                            (when (a/decl "arbace/lang/RT") ["arbace/lang/RT#printString"])))
                    :after (fn [{:keys [prog-dir]}]
                             ;; one main per file: steps are numbered per file
                             (doseq [[f x] expected]
                               (let [dir (str prog-dir "-" f)]
                                 (sh "rm" "-rf" dir)
                                 (sh "cp" "-r" prog-dir dir)
                                 (write-program! dir (:cases x)))))))
    (let [overlay (str/trim (second (sh "bin/jrt" "overlay")))
          lines (atom [])]
      (doseq [[f x] expected
              arch arches]
        (let [dir (str out-dir "/prog-" f)
              exe (str out-dir "/bin/" arch "/" f)
              _ (io/make-parents (io/file exe))
              t0 (System/nanoTime)
              [code bo] (sh "bin/g2c" "build" "--arch" arch "--work" (str out-dir "/work-" f "-" arch) "-o" exe
                            "--overlay" overlay dir)
              tb (/ (Math/round (/ (- (System/nanoTime) t0) 1e7)) 100.0)]
          (if-not (zero? code)
            (do (println (str "c2g-check: " f " (" arch "): build failed"))
                (println (str/join "\n" (take 30 (remove #(str/includes? % "WARNING") (str/split-lines bo)))))
                (swap! results assoc [f arch] {:build-failed true}))
            (let [[rc ro] (if (= arch "arm64") (sh "/usr/bin/qemu-aarch64" exe) (sh exe))
                  cmp (compare-run (:cases x) ro)
                  counts (frequencies (map :status cmp))
                  size (.length (io/file exe))]
              (swap! results assoc [f arch] {:counts counts :cases (count (:cases x)) :build-s tb :size size :exit rc
                                             :fails (vec (take 12 (filter #(= :fail (:status %)) cmp)))})
              (println (format "c2g-check: %-20s %-5s %4d steps: %4d pass, %4d fail, %4d unavailable, %4d printer (build %.1f s, %d bytes)"
                               f arch (count (:cases x)) (:pass counts 0) (:fail counts 0) (:unavailable counts 0) (:pr counts 0)
                               tb size))
              (when (System/getenv "C2G_SHOW")
                (doseq [c (take (if (= "all" (System/getenv "C2G_SHOW")) 100000 20) (filter #(= :fail (:status %)) cmp))]
                  (println "   FAIL" (:i c) (:src c) "--" (:why c))))))))
      (spit (str out-dir "/results.edn") (with-out-str (arbace.pprint/pprint @results)))
      (shutdown-agents)
      (System/exit (if (some :build-failed (vals @results)) 1 0)))))
