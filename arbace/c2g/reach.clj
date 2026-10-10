(ns arbace.c2g.reach
  "Reachability (JRT-SOURCES.md, the user's decision of 2026-10-08: translate the reached
  methods, stub the rest): rapid type analysis over the analyzed class forms from root methods.
  It decides the translated classes T (classes reached code uses: a class jrt provides stays
  jrt's, except stand-ins, which translated classes replace), the reached methods (translated;
  every other method of T is a stub throwing UnsupportedOperationException), and the methods
  whose code needs something the closed world lacks (stubbed too, and reported)."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a]
            [arbace.c2g.model :as m]
            [arbace.c2g.names :as nm]
            [arbace.c2g.access :as acc]
            [arbace.c2g.jrt :as jrt])
  (:import (arbace.asm Opcodes)))

;; ---------------------------------------------------------------------------------------
;; walking analyzed code

(defn node? [x] (and (map? x) (contains? x :op)))

(defn walk-nodes
  "Calls (f node) on node and every node in it (operands, branches, bindings, cases, catches,
  lambda bodies, patterns), each once, parents first."
  [f node]
  (letfn [(walk [n]
            (f n)
            (doseq [[k v] n :when (not (#{:b :field :method :ctor :boundary :sam :hidden :temp
                                          :sel-binding :restart :var :frame :scope} k))]
              (walk-val v)))
          (walk-val [v]
            (cond (node? v) (walk v)
                  (sequential? v) (doseq [x v]
                                    (cond (node? x) (walk x)
                                          (map? x) (walk-plain x)
                                          (sequential? x) (walk-val x)))
                  :else nil))
          (walk-plain [mp]
            (doseq [[k v] mp :when (not (#{:b :frame :scope :accessor :ctor} k))]
              (cond (node? v) (walk v)
                    (sequential? v) (walk-val v)
                    (and (map? v) (#{:pattern} k)) (walk-plain v))))]
    (when (node? node) (walk node))))

;; ---------------------------------------------------------------------------------------
;; Java's method resolution, independent of the world

(defn java-impl-of
  "The [class name desc] implementing virtual method [name desc] for class n (a body in n or a
  superclass, else an interface's default), by Java's rules on the class environment."
  [n [name desc]]
  (or (some (fn [c]
              (when-let [mm (some #(when (and (= name (:name %)) (= desc (:desc %))) %) (:methods (env/info c)))]
                (when (and (not (m/static? mm)) (not (m/abstract-m? mm)) (or (not (m/private? mm)) (= c n)))
                  [c name desc])))
            (take-while some? (iterate #(:super (env/info %)) n)))
      (let [cands (for [c (env/all-supertypes n)
                        :when (and (not= c n) (env/interface? c))
                        :let [mm (some #(when (and (= name (:name %)) (= desc (:desc %))) %) (:methods (env/info c)))]
                        :when (and mm (not (m/abstract-m? mm)) (not (m/static? mm)) (not (m/private? mm)))]
                    c)
            cands (remove (fn [x] (some #(and (not= % x) (env/subclass? % x)) cands)) cands)]
        (when-let [c (first cands)] [c name desc]))))

;; ---------------------------------------------------------------------------------------
;; the analysis

(def ^:dynamic *st* nil)
(def ^:dynamic *slice?*
  "Whether a class's code may be translated in this run (classes outside the slice exist as
  declarations with stub bodies)."
  (constantly true))
(def ^:dynamic *current* nil)

(defn- note-unavailable! [why]
  (when *current*
    (swap! (:unavailable *st*) update *current* (fnil conj #{}) why)))

(declare use! init! reach! vcall! instantiate!)

(defn- hw-has-static? [cls name desc]
  (let [g (m/go-name cls)
        base (nm/method-base name desc)
        scan (:jrt m/*w*)]
    (contains? (:funcs scan) (str g "_" base))))

(defn- hw-has-field? [cls fname static?]
  (let [g (m/go-name cls)
        scan (:jrt m/*w*)]
    (if static?
      (or (contains? (:vars scan) (nm/static-field-name g fname))
          (contains? (:consts scan) (nm/static-field-name g fname)))
      true)))

(defn use!
  "Makes class n exist in Go (translating it when it can and jrt does not provide it, or jrt
  provides only a stand-in). Returns false, noting why, when it cannot."
  [n]
  (cond
    (nil? n) true
    (= n "java/lang/Object") true
    (m/translated? n) true
    (contains? @(:failed-classes *st*) n) (do (note-unavailable! (str "class " n ": " (get @(:failed-classes *st*) n))) false)
    (and (m/jrt-class n) (not (:standin (m/jrt-class n)))) true
    ;; a stand-in is replaced only by a translation of the slice (jrt's code needs its members)
    (and (m/jrt-class n) (not (*slice?* n))) true
    (m/translatable? n)
    (let [d (a/decl n)]
      (if-let [why (:analysis-failed @(:state d))]
        (do (swap! (:failed-classes *st*) assoc n (str "analysis failed: " why))
            (note-unavailable! (str "class " n " failed analysis"))
            false)
        (do
          (swap! (:T *st*) conj n)
          (let [sup (:super d)
                ok (binding [*current* [n "<class>" ""]] (use! sup))]
            (if-not ok
              (do (swap! (:T *st*) disj n)
                  (swap! (:failed-classes *st*) assoc n (str "superclass " sup " unavailable"))
                  (note-unavailable! (str "class " n ": superclass " sup " unavailable"))
                  false)
              (do
                (doseq [i (:interfaces d)] (binding [*current* [n "<class>" ""]] (use! i)))
                ;; nested classes need their outer class (outer instance fields, statics)
                (when-let [o (:outer d)] (binding [*current* [n "<class>" ""]] (use! o)))
                ;; virtual calls already seen reach into the new class's subtypes when instantiated
                true))))))
    (m/jrt-class n) true
    ;; a JDK interface known only by reflection: declared, without code
    (m/reflectable-interface? n)
    (do (swap! (:T *st*) conj n)
        (doseq [i (:interfaces (env/info n))] (binding [*current* [n "<class>" ""]] (use! i)))
        true)
    :else (do (swap! (:missing *st*) update n (fnil conj #{}) *current*)
              (note-unavailable! (str "class " (str/replace n "/" ".") " is not in the closed world"))
              false)))

(defn- use-desc! [d]
  (doseq [c (m/desc-classes d)] (use! c)))

(defn reach!
  "Marks method [c name desc] reached (its body will be translated)."
  [[c name desc :as k]]
  (when (and c (m/translated? c) (a/decl c) (*slice?* c) (not (contains? @(:reached *st*) k)))
    (swap! (:reached *st*) conj k)
    (swap! (:queue *st*) conj k)))

(defn init!
  "Class c is initialized: its static initializer is reached (and its superclass's)."
  [c]
  (when (and c (m/translated? c) (a/decl c) (not (contains? @(:inited *st*) c)))
    (swap! (:inited *st*) conj c)
    (when-let [s (:super (a/decl c))] (init! s))
    (reach! [c "<clinit>" "()V"])))

(defn- external-keys
  "The virtual methods of class d's supertypes that jrt's own code may call: those of Object,
  of jrt's hand-written classes and interfaces and of the classes jrt has stand-ins for."
  [d]
  (for [s (env/all-supertypes d)
        :when (or (= s "java/lang/Object") (m/jrt-class s))
        mm (:methods (env/info s))
        :when (and (not (m/static? mm)) (not (m/private? mm)) (not= "<init>" (:name mm)))]
    [(:name mm) (:desc mm)]))

(defn instantiate!
  [d]
  (when (and (m/translated? d) (not (contains? @(:inst *st*) d)))
    (swap! (:inst *st*) conj d)
    (doseq [[o name desc] @(:vcalls *st*)
            :when (env/subclass? d o)]
      (when-let [k (java-impl-of d [name desc])] (reach! k)))
    (doseq [k (distinct (external-keys d))]
      (when-let [impl (java-impl-of d k)] (reach! impl)))))

(defn vcall!
  "A virtual call of [name desc] on a receiver of static type o."
  [o name desc]
  (let [k [o name desc]]
    (when-not (contains? @(:vcalls *st*) k)
      (swap! (:vcalls *st*) conj k)
      (doseq [d (concat @(:inst *st*) @(:lambda-fis *st*)) :when (env/subclass? d o)]
        (when-let [impl (java-impl-of d [name desc])] (reach! impl))))))

(defn lambda!
  "A lambda or method reference of functional interface fi: its adapter is an instance of fi,
  whose default methods virtual calls reach."
  [fi]
  (when-not (contains? @(:lambda-fis *st*) fi)
    (swap! (:lambda-fis *st*) conj fi)
    (doseq [[o name desc] @(:vcalls *st*) :when (env/subclass? fi o)]
      (when-let [impl (java-impl-of fi [name desc])] (reach! impl)))
    (doseq [k (distinct (external-keys fi))]
      (when-let [impl (java-impl-of fi k)] (reach! impl)))))

(defn- check-hw-call!
  "A call of a method of jrt's hand-written class: jrt must have it."
  [owner name desc kind]
  (when (and (m/hand-written? owner) (not= owner "java/lang/Object"))
    (let [mm {:name name :desc desc}]
      (when-not (if (= kind :static)
                  (hw-has-static? owner name desc)
                  (or (m/hw-method-exists? owner mm)
                      ;; inherited from a superclass jrt has
                      (some #(and (m/in-world? %) (or (= % "java/lang/Object") (m/hw-method-exists? % mm)))
                            (rest (env/all-supertypes owner)))))
        (note-unavailable! (str "jrt lacks " (str/replace owner "/" ".") "." name desc))))))

(defn- class-of-desc [d] (first (m/desc-classes d)))

(defn- vh-const?
  "Is field f a constant VarHandle of the world (C2G-SPEC §8.5)?"
  [f]
  (contains? (:handles (:vh m/*w*)) [(or (:declarer f) (:owner f)) (:name f)]))

(defn- vh-call?
  "A VarHandle call c2g compiles statically (C2G-SPEC §8.5): a fence, MethodHandles.lookup(),
  or an access mode on a constant handle; it names no class of java.lang.invoke in Go."
  [node]
  (let [{:keys [owner kind name]} node]
    (or (and (= :static kind) (= "java/lang/invoke/VarHandle" owner)
             (#{"fullFence" "acquireFence" "releaseFence" "loadLoadFence" "storeStoreFence"} name))
        (and (= :static kind) (= "java/lang/invoke/MethodHandles" owner) (= "lookup" name))
        (and (not= :static kind) (= "java/lang/invoke/VarHandle" owner)
             (let [tg (acc/unaccess (:target node))]
               (and (= :get-static (:op tg)) (vh-const? (:field tg))))))))

(defn- scan-node! [node]
  (let [node (acc/unaccess node)]
    (case (:op node)
      :invoke
      (let [mm (:method node)
            mowner (or (:owner mm) (:owner node))
            {:keys [name desc]} node]
        (cond
          (:array-clone node) (use-desc! (:owner node))
          (vh-call? node) (doseq [c (m/method-desc-classes desc)] (use! c))
          :else
          (do
            (use! (:owner node))
            (use! mowner)
            (use-desc! (str "L" (:owner node) ";"))
            (doseq [c (m/method-desc-classes desc)] (use! c))
            (case (:kind node)
              :static (do (init! mowner) (reach! [mowner name desc])
                          (check-hw-call! mowner name desc :static))
              :special (do (reach! [mowner name desc]) (check-hw-call! mowner name desc :special))
              (:virtual :interface)
              (if (and mm (m/private? mm))
                (reach! [mowner name desc])
                (do (vcall! (:owner node) name desc)
                    (check-hw-call! (:owner node) name desc :virtual)))))))
      :new (let [c (:class node)
                 ok (use! c)]
             (when ok
               (init! c)
               (reach! [c "<init>" (:desc (:ctor node))])
               (instantiate! c)
               (when (m/hand-written? c)
                 (let [g (m/go-name c)]
                   (when-not (contains? (:funcs (:jrt m/*w*)) (nm/new-name g (:desc (:ctor node))))
                     (note-unavailable! (str "jrt lacks constructor " c (:desc (:ctor node)))))))))
      :ctor-call (do (use! (:class node))
                     (reach! [(:class node) "<init>" (:desc (:ctor node))]))
      (:get-static :set-static)
      (let [f (:field node)
            o (or (:declarer f) (:owner f))]
        (when (and (not (vh-const? f)) (use! o))
          (init! o)
          (use-desc! (:desc f))
          (when (and (m/hand-written? o) (not (hw-has-field? o (:name f) true)))
            (note-unavailable! (str "jrt lacks static field " o "." (:name f))))))
      (:get-field :set-field)
      (let [f (:field node)] (use! (or (:declarer f) (:owner f))) (use-desc! (:desc f)))
      (:cast :instance?) (use-desc! (:class node))
      :class-lit (use-desc! (:class node))
      (:new-array :array-init) (use-desc! (:type node))
      :try (doseq [c (:catches node) cls (:classes c)] (use! cls))
      :lambda (do (when (use! (:fi node)) (lambda! (:fi node))) (doseq [mk (:markers node)] (use! mk)))
      :method-ref
      (do (when (use! (:fi node)) (lambda! (:fi node))) (use! (:owner node))
          (case (:kind node)
            :static (do (init! (:owner node)) (reach! [(:owner node) (:name node) (:desc node)]))
            :new (do (init! (:owner node)) (reach! [(:owner node) "<init>" (:desc node)])
                     (instantiate! (:owner node)))
            (:bound :unbound) (vcall! (:owner node) (:name node) (:desc node))))
      :java-str (doseq [p (:parts node) :when (:node p)
                        :let [ty (a/value-type (:type (:node p)))]
                        :when (and (t/ref? ty) (not= ty :null) (not= ty "Ljava/lang/String;"))]
                  (vcall! (if (t/array? ty) "java/lang/Object" (t/desc->internal ty))
                          "toString" "()Ljava/lang/String;"))
      :for-each (when-not (:array node)
                  (use! "java/util/Iterator")
                  (vcall! "java/util/Iterator" "hasNext" "()Z")
                  (vcall! "java/util/Iterator" "next" "()Ljava/lang/Object;"))
      :assert (do (use! "java/lang/AssertionError")
                  (let [d (str "(" (or (:msg-desc node) "") ")V")]
                    (reach! ["java/lang/AssertionError" "<init>" d])
                    (instantiate! "java/lang/AssertionError")))
      :if-instance (walk-nodes (fn [_]) nil)
      (:var-deref :var-invoke)
      (do (use! (t/lang-class "Var"))
          (vcall! (t/lang-class "Var") (if (= :var-deref (:op node)) "deref" "getRawRoot")
                  (if (= :var-deref (:op node)) "()Ljava/lang/Object;" "()Ljava/lang/Object;"))
          (when (= :var-invoke (:op node))
            (use! (t/lang-class "IFn"))
            (vcall! (t/lang-class "IFn") "invoke"
                    (t/method-desc (repeat (count (:args node)) "Ljava/lang/Object;") "Ljava/lang/Object;"))))
      ;; Clojure's adapter of an IFn to a functional interface (Compiler's FISupport): the
      ;; interface's adapter (as for a lambda) calling FnInvokers' invoker
      :fi-adapter
      (let [fi (t/desc->internal (:type node))
            fni (t/lang-class "FnInvokers")]
        (use! (t/lang-class "IFn"))
        (when (use! fi) (lambda! fi))
        (when (use! fni)
          (init! fni)
          (reach! [fni (:invoker node) (:invoker-desc node)])))
      nil)
    ;; patterns name classes too
    (when (= :if-instance (:op node))
      (letfn [(pat [p] (use-desc! (:class p))
                (doseq [c (:comps p)]
                  (vcall! (:owner (:accessor c)) (:name (:accessor c)) (:desc (:accessor c)))
                  (pat (:pattern c))))]
        (pat (:pattern node))))
    (when (= :switch (:op node))
      (doseq [c (:cases node) l (:labels c) :when (:pattern l)]
        (letfn [(pat [p] (use-desc! (:class p))
                  (doseq [cc (:comps p)]
                    (vcall! (:owner (:accessor cc)) (:name (:accessor cc)) (:desc (:accessor cc)))
                    (pat (:pattern cc))))]
          (pat (:pattern l)))))))

(defn scan-code! [node] (walk-nodes scan-node! node))

(defn method-code
  "The analyzed code of method [c name desc] of a translated class: a vector of nodes."
  [[c name desc]]
  (let [d (a/decl c)
        st @(:state d)]
    (if (= name "<clinit>")
      (vec (:clinit st))
      (let [ab (get (:bodies st) [name desc])]
        (if (= name "<init>")
          (vec (concat (:prologue ab) [(:call ab)] (when (:calls-super ab) (:init st)) [(:body ab)]))
          (if ab [(:body ab)] []))))))

(defn- scan-method! [[c name desc :as k]]
  (binding [*current* k]
    (let [d (a/decl c)
          st @(:state d)
          mm (when-not (= name "<clinit>") (m/find-method c name desc))]
      (when-let [why (:analysis-failed st)] (note-unavailable! (str "analysis failed: " why)))
      (when mm
        (doseq [cl (m/method-desc-classes desc)] (use! cl)))
      (case name
        "<clinit>"
        (do (doseq [nd (:clinit st)] (scan-code! nd))
            (when (seq (:clj-consts st))
              (use! (t/lang-class "RT"))
              (doseq [[_ {:keys [init]}] (:clj-consts st)]
                (case (:kind init)
                  :var (do (init! (t/lang-class "RT"))
                           (reach! [(t/lang-class "RT") "var" (str "(Ljava/lang/String;Ljava/lang/String;)" (t/lang-desc "Var"))]))
                  :read (do (init! (t/lang-class "RT"))
                            (reach! [(t/lang-class "RT") "readString" "(Ljava/lang/String;)Ljava/lang/Object;"])))))
            (when (or (:uses-assert st) (:interface-assert st)) nil))
        (cond
          (:derived mm)
          (case (:derived mm)
            :bridge (let [tg (:bridge-of mm)]
                      (if (:special mm)
                        (reach! [(:owner tg) (:name tg) (:desc tg)])
                        (vcall! c (:name tg) (:desc tg))))
            :record-object-method (doseq [cm (:components d)]
                                    (when (t/ref? (:desc cm))
                                      (vcall! "java/lang/Object" "hashCode" "()I")
                                      (vcall! "java/lang/Object" "equals" "(Ljava/lang/Object;)Z")
                                      (vcall! "java/lang/Object" "toString" "()Ljava/lang/String;")))
            (:default-ctor :anon-ctor :record-canonical)
            (let [ab (get (:bodies st) ["<init>" desc])]
              (when-let [call (:call ab)] (scan-code! call))
              (doseq [i (:init st)] (scan-code! i)))
            nil)
          :else (doseq [nd (method-code k)] (scan-code! nd)))))))

(defn run
  "Reachability from roots (method keys [class name desc]; classes to instantiate
  :instantiate; functional interfaces with adapters :fis). Returns {:T #{} :reached #{} :inst #{} :unavailable {k #{why}} :missing {}}."
  [{:keys [roots instantiate classes slice? fis]}]
  (let [st {:T (atom #{}) :reached (atom #{}) :queue (atom []) :inst (atom #{}) :vcalls (atom #{})
            :inited (atom #{}) :unavailable (atom {}) :missing (atom {}) :failed-classes (atom {})
            :lambda-fis (atom #{})}]
    (binding [*st* st
              *slice?* (or slice? (constantly true))
              m/*w* (assoc m/*w* :T (:T st))]
      (doseq [c classes] (use! c))
      (doseq [[c :as k] roots]
        (when (use! c)
          (reach! k)
          ;; a root constructor: its class is instantiated (by the caller of the roots)
          (when (and (= "<init>" (second k)) (*slice?* c)) (instantiate! c))))
      (doseq [c instantiate] (when (use! c) (instantiate! c)))
      ;; functional interfaces whose adapters exist without a lambda (FromFn, C2G-SPEC §7.11)
      (doseq [fi fis] (when (use! fi) (lambda! fi)))
      (loop []
        (when-let [k (first @(:queue st))]
          (swap! (:queue st) subvec 1)
          (scan-method! k)
          (recur))))
    {:T @(:T st) :reached @(:reached st) :inst @(:inst st) :unavailable @(:unavailable st)
     :missing @(:missing st) :failed-classes @(:failed-classes st) :vcalls @(:vcalls st)}))
