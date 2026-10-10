(ns arbace.c2g.vh
  "VarHandles compiled statically (C2G-SPEC §8.5, amendment JC1): the JDK's concurrent classes
  make their VarHandles once, in static initializers, as static final fields
  (MethodHandles.lookup().findVarHandle(C.class, \"f\", T.class), MhUtil.findVarHandle,
  MethodHandles.arrayElementVarHandle(T[].class)), and use them only through those fields
  (NEXT.compareAndSet(node, a, b)). c2g reads the initializers into a table of constant handles
  and translates each access mode called on one of these fields into an atomic operation on the
  field it names (or on the array element), as Go code; java.lang.invoke itself stays outside
  the world (erased: the initializers' lookups are not run). Fields a handle names get the
  volatile representation (§8.2), so that every access to them, plain or by the handle, is
  atomic."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.analyze :as a]
            [arbace.c2g.access :as acc]
            [arbace.c2g.reach :as r]))

(def varhandle "java/lang/invoke/VarHandle")
(def vh-desc "Ljava/lang/invoke/VarHandle;")

(def ^:private box-prims
  {"java/lang/Boolean" "Z" "java/lang/Byte" "B" "java/lang/Character" "C" "java/lang/Short" "S"
   "java/lang/Integer" "I" "java/lang/Long" "J" "java/lang/Float" "F" "java/lang/Double" "D"})

(defn- class-arg
  "The descriptor a Class-valued node stands for (a class literal, Integer.TYPE and the like, or a
  final local bound to one), or nil."
  [node env]
  (let [node (acc/unaccess node)]
    (case (:op node)
      :class-lit (:class node)
      :get-static (let [f (:field node)]
                    (when (= "TYPE" (:name f)) (box-prims (or (:declarer f) (:owner f)))))
      :local (when-let [init (get env (:id (:b node)))] (class-arg init env))
      :cast (class-arg (:expr node) env)
      nil)))

(defn- string-arg [node]
  (let [node (acc/unaccess node)]
    (when (and (= :const (:op node)) (string? (:val node))) (:val node))))

(defn- find-field
  "The instance field `fname` of class n or a superclass (as findVarHandle resolves it)."
  [n fname]
  (loop [c n]
    (when-let [d (and c (a/decl c))]
      (or (some #(when (= fname (:name %)) %) (:fields d))
          (recur (:super d))))))

(defn- static-flag? [f] (not (zero? (bit-and (or (:flags f) 0) 0x8))))

(defn- handle-spec
  "The handle the initializer node `init` makes in class n's static initializer, or nil:
  {:kind :field :class C :name f :desc T :field field} or {:kind :array :desc \"[T\"}."
  [n init env]
  (let [init (acc/unaccess init)
        field-spec (fn [c fname tdesc]
                     (when (and c fname tdesc (t/class-desc? c))
                       (let [cn (t/desc->internal c)
                             f (find-field cn fname)]
                         (when (and f (= tdesc (:desc f)) (not (static-flag? f)))
                           {:kind :field :class (or (:owner f) cn) :name fname :desc tdesc :field f}))))]
    (when (= :invoke (:op init))
      (let [{:keys [owner name args kind]} init]
        (cond
          (and (= owner "java/lang/invoke/MethodHandles$Lookup") (= name "findVarHandle") (= 3 (count args)))
          (field-spec (class-arg (nth args 0) env) (string-arg (nth args 1)) (class-arg (nth args 2) env))

          (and (= owner "jdk/internal/invoke/MhUtil") (= name "findVarHandle") (= kind :static))
          (case (count args)
            ;; (lookup, name, type): the lookup's class, the class whose code made it
            3 (field-spec (str "L" n ";") (string-arg (nth args 1)) (class-arg (nth args 2) env))
            4 (field-spec (class-arg (nth args 1) env) (string-arg (nth args 2)) (class-arg (nth args 3) env))
            nil)

          (and (= owner "java/lang/invoke/MethodHandles") (= name "arrayElementVarHandle") (= kind :static)
               (= 1 (count args)))
          (let [d (class-arg (first args) env)]
            (when (and d (t/array? d)) {:kind :array :desc d}))

          :else nil)))))

(defn- class-handles
  "{[owner fieldname] spec} of the VarHandle constants class n's static initializer makes."
  [n]
  (let [st @(:state (a/decl n))
        code (:clinit st)
        env (atom {})
        out (atom {})]
    (doseq [nd code]
      (r/walk-nodes (fn [x]
                      (when (#{:let :loop} (:op x))
                        (doseq [[b init] (:bindings x) :when (and (map? b) (not (:mutable b)))]
                          (swap! env assoc (:id b) init))))
                    nd))
    (doseq [nd code]
      (r/walk-nodes (fn [x]
                      (when (and (= :set-static (:op x)) (= vh-desc (:desc (:field x))))
                        (let [f (:field x)
                              o (or (:declarer f) (:owner f))]
                          (when-let [spec (handle-spec n (:val x) @env)]
                            (swap! out assoc [o (:name f)] spec)))))
                    nd))
    @out))

(defn handles
  "The VarHandle constants of every class of the world (run with the world bound): {:handles
  {[owner fieldname] spec} :fields #{[owner fieldname]}}, the fields handles name."
  [class-names]
  (let [hs (into {} (for [n class-names
                          :let [d (a/decl n)]
                          :when (and d (some #(= vh-desc (:desc %)) (:fields d)))
                          e (try (class-handles n) (catch Throwable _ nil))]
                      e))]
    {:handles hs
     :fields (set (for [[_ s] hs :when (= :field (:kind s))] [(:class s) (:name s)]))}))

;; ---------------------------------------------------------------------------------------
;; access modes

(def fences #{"fullFence" "acquireFence" "releaseFence" "loadLoadFence" "storeStoreFence"})

(defn mode
  "The operation of access mode `name`: :get :set :cas :cmpxchg :swap :add :or :and :xor, or nil."
  [name]
  (cond
    (#{"get" "getVolatile" "getAcquire" "getOpaque"} name) :get
    (#{"set" "setVolatile" "setRelease" "setOpaque"} name) :set
    (#{"compareAndSet" "weakCompareAndSet" "weakCompareAndSetPlain" "weakCompareAndSetAcquire"
       "weakCompareAndSetRelease"} name) :cas
    (#{"compareAndExchange" "compareAndExchangeAcquire" "compareAndExchangeRelease"} name) :cmpxchg
    (#{"getAndSet" "getAndSetAcquire" "getAndSetRelease"} name) :swap
    (#{"getAndAdd" "getAndAddAcquire" "getAndAddRelease"} name) :add
    (#{"getAndBitwiseOr" "getAndBitwiseOrAcquire" "getAndBitwiseOrRelease"} name) :or
    (#{"getAndBitwiseAnd" "getAndBitwiseAndAcquire" "getAndBitwiseAndRelease"} name) :and
    (#{"getAndBitwiseXor" "getAndBitwiseXorAcquire" "getAndBitwiseXorRelease"} name) :xor
    :else nil))

(defn fence? [node]
  (and (= :invoke (:op node)) (= :static (:kind node)) (= varhandle (:owner node)) (fences (:name node))))

(defn op-handle
  "The handle spec of a VarHandle access-mode call `node` on a constant handle of table hs
  ({[owner name] spec}), with its operation: (assoc spec :mode m), or nil."
  [hs node]
  (when (and (= :invoke (:op node)) (= varhandle (:owner node)) (not= :static (:kind node)))
    (let [tg (acc/unaccess (:target node))]
      (when (= :get-static (:op tg))
        (let [f (:field tg)
              spec (get hs [(or (:declarer f) (:owner f)) (:name f)])
              m (mode (:name node))]
          (when (and spec m) (assoc spec :mode m)))))))
