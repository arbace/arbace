(ns arbace.classes.emit
  "Bytecode generation for analyzed class declarations, with ASM (SPEC §6)."
  (:require [clojure.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a])
  (:import (arbace.asm ClassWriter ClassReader ClassVisitor MethodVisitor FieldVisitor
                       AnnotationVisitor Opcodes Label Type Handle ConstantDynamic)))

(def ^:dynamic *version*
  "The class file version: the running JDK's by default."
  (+ 44 (.feature (Runtime/version))))

(defn fail [msg] (a/fail msg))

;; ---------------------------------------------------------------------------------------------
;; generator state
;;
;; gen: {:mv MethodVisitor :class name :slots (atom {binding-id slot}) :next (atom next-slot)
;;       :ret desc :targets {target-id info} :cleanups [cleanup ...] :ctor? bool
;;       :outer-slot slot-or-nil :lambda? bool :unsafe IdentityHashMap}

(defn- ^MethodVisitor mv [gen] (:mv gen))

(defn- insn [gen op] (.visitInsn (mv gen) op))

(defn- opcode [desc base]
  (.getOpcode (Type/getType (if (keyword? desc) t/object-desc desc)) base))

(defn alloc-slot [gen desc]
  (let [s @(:next gen)]
    (swap! (:next gen) + (t/size desc))
    s))

(defn- xload [gen desc slot] (.visitVarInsn (mv gen) (opcode desc Opcodes/ILOAD) slot))
(defn- xstore [gen desc slot] (.visitVarInsn (mv gen) (opcode desc Opcodes/ISTORE) slot))

(defn- pop-type [gen t]
  (when-not (or (= t :none) (= t "V"))
    (insn gen (if (= 2 (t/size t)) Opcodes/POP2 Opcodes/POP))))

(defn emit-const [gen t v]
  (let [m (mv gen)]
    (case (if (keyword? t) t t)
      :null (.visitInsn m Opcodes/ACONST_NULL)
      ("I" "S" "B" "C" "Z")
      (let [i (cond (char? v) (int v) (boolean? v) (if v 1 0) :else (long v))]
        (cond (<= -1 i 5) (.visitInsn m (+ Opcodes/ICONST_0 i))
              (<= -128 i 127) (.visitIntInsn m Opcodes/BIPUSH i)
              (<= -32768 i 32767) (.visitIntInsn m Opcodes/SIPUSH i)
              :else (.visitLdcInsn m (Integer/valueOf (int i)))))
      "J" (let [l (long v)]
            (if (<= 0 l 1) (.visitInsn m (+ Opcodes/LCONST_0 l)) (.visitLdcInsn m (Long/valueOf l))))
      "F" (let [f (float v)
                bits (Float/floatToIntBits f)]
            (if (#{(Float/floatToIntBits 0.0) (Float/floatToIntBits 1.0) (Float/floatToIntBits 2.0)} bits)
              (.visitInsn m (+ Opcodes/FCONST_0 (int f)))
              (.visitLdcInsn m (Float/valueOf f))))
      "D" (let [d (double v)
                bits (Double/doubleToLongBits d)]
            (if (#{(Double/doubleToLongBits 0.0) (Double/doubleToLongBits 1.0)} bits)
              (.visitInsn m (+ Opcodes/DCONST_0 (int d)))
              (.visitLdcInsn m (Double/valueOf d))))
      (if (string? v) (.visitLdcInsn m v) (fail (str "Cannot emit constant " (pr-str v)))))))

;; ---------------------------------------------------------------------------------------------
;; conversions

(def ^:private prim-conv
  {["I" "J"] [Opcodes/I2L] ["I" "F"] [Opcodes/I2F] ["I" "D"] [Opcodes/I2D]
   ["J" "I"] [Opcodes/L2I] ["J" "F"] [Opcodes/L2F] ["J" "D"] [Opcodes/L2D]
   ["F" "I"] [Opcodes/F2I] ["F" "J"] [Opcodes/F2L] ["F" "D"] [Opcodes/F2D]
   ["D" "I"] [Opcodes/D2I] ["D" "J"] [Opcodes/D2L] ["D" "F"] [Opcodes/D2F]
   ["I" "B"] [Opcodes/I2B] ["I" "C"] [Opcodes/I2C] ["I" "S"] [Opcodes/I2S]})

(defn- prim-convert [gen from to]
  (let [f (if (t/int-like? from) "I" from)]
    (cond
      (= from to) nil
      (and (t/int-like? to) (not= to "I"))
      (do (when-not (= f "I") (prim-convert gen f "I"))
          ;; widening among int-likes needs no instruction (byte -> short, -> int)
          (when-not (or (= f to) (and (= from "B") (= to "S")))
            (when-not (= from to)
              (doseq [o (prim-conv ["I" to])] (insn gen o)))))
      (= f to) nil
      :else (doseq [o (or (prim-conv [f to]) (fail (str "No conversion " from " -> " to)))]
              (insn gen o)))))

(defn box [gen p]
  (let [w (t/box-of p)]
    (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC w "valueOf" (str "(" p ")L" w ";") false)))

(defn unbox [gen wdesc]
  (let [p (t/unbox-of wdesc)
        w (t/desc->internal wdesc)]
    (.visitMethodInsn (mv gen) Opcodes/INVOKEVIRTUAL w (str (t/prim-desc->name p) "Value")
                      (str "()" p) false)
    p))

(defn emit-convert
  "Converts the value on the stack from type `from` to `to` (as analyzed: widening,
  narrowing, boxing, unboxing)."
  [gen from to]
  (let [from (a/value-type from)]
    (cond
      (or (= from :none) (= from to) (= to "V") (nil? to)) nil
      (= from :null) nil
      (and (t/prim? from) (t/prim? to)) (prim-convert gen from to)
      (t/prim? from) (box gen from)
      (t/prim? to) (let [p (unbox gen from)] (prim-convert gen p to))
      :else nil)))

;; ---------------------------------------------------------------------------------------------
;; unsafe nodes: those containing a jump or an exception handler need an empty operand stack

(def ^:private unsafe-ops #{:try :monitor :break :recur :return :switch-unsafe})

(defn children [node]
  (case (:op node)
    (:const :local :this-path :class-lit :var-deref :enum-constants) []
    :let (concat (map second (:bindings node)) [(:body node)])
    :loop (concat (map second (:bindings node)) [(:body node)])
    :do (conj (vec (:statements node)) (:ret node))
    :if [(:test node) (:then node) (:else node)]
    :try (concat [(:body node)] (map :body (:catches node)) (when (:finally node) [(:finally node)]))
    (concat (when (map? (:target node)) (when (:op (:target node)) [(:target node)]))
            (filter map? [(:expr node) (:val node) (:test node) (:then node) (:else node) (:body node)
                          (:array node) (:index node) (:lock node) (:outer node) (:call node)])
            (:args node) (:elems node) (:dims node) (:prologue node) (:parts node)
            (mapcat (fn [c] [(:body c)]) (:cases node)))))

(defn unsafe? [gen node]
  (let [^java.util.IdentityHashMap cache (:unsafe gen)]
    (if (.containsKey cache node)
      (.get cache node)
      (let [r (boolean (or (unsafe-ops (:op node))
                           (some #(unsafe? gen %) (remove nil? (children node)))))]
        (.put cache node r)
        r))))

;; ---------------------------------------------------------------------------------------------
;; expressions

(declare emit emit-cond load-binding write-local-type-annotations write-insn-type-annotations emit-let)

(defmulti emit-extra
  "Emission of the later forms (switch, lambdas...): (emit-extra gen node ctx)."
  (fn [gen node ctx] (:op node)))

(defn emit-to
  "Emits node as a value converted to type `to`."
  [gen node to]
  (if (and (= :const (:op node)) (t/prim? to) (t/prim? (:type node)) (not= "Z" to))
    (emit-const gen to (a/const-of-type to (:val node)))
    (do (emit gen node :expr)
        (emit-convert gen (:type node) to))))

(defn- spill-operands
  "Emits nodes (each converted to its type in types) so that their values end up on the stack;
  when one of them is unsafe, those before it are computed into temporaries first.
  `before` emits what goes below the operands (such as new/dup) once nothing unsafe follows."
  ([gen nodes types] (spill-operands gen nodes types nil))
  ([gen nodes types before]
   (let [last-unsafe (last (keep-indexed (fn [i n] (when (unsafe? gen n) i)) nodes))]
     (if (and (nil? last-unsafe) true)
       (do (when before (before))
           (doseq [[n ty] (map vector nodes types)] (emit-to gen n ty)))
       (let [k (inc last-unsafe)
             temps (doall (for [[n ty] (map vector (take k nodes) types)]
                            (let [ty (or ty (a/value-type (:type n)))
                                  s (alloc-slot gen ty)]
                              (emit-to gen n ty)
                              (if (= ty :none) nil (xstore gen ty s))
                              [ty s])))]
         (when before (before))
         (doseq [[ty s] temps] (when s (xload gen ty s)))
         (doseq [[n ty] (map vector (drop k nodes) (drop k types))] (emit-to gen n ty)))))))

(defn- arg-types [m] (first (t/parse-method-desc (:desc m))))

(defn- this-field-path [gen path]
  (loop [[c & more] path first? true]
    (when c
      (let [d (a/decl! c)
            od (t/internal->desc (:outer d))]
        (if (and first? (:outer-slot gen) (= c (:class gen)))
          (.visitVarInsn (mv gen) Opcodes/ALOAD (:outer-slot gen))
          (.visitFieldInsn (mv gen) Opcodes/GETFIELD c "this$0" od))
        (recur more false)))))

(defn- emit-this-path [gen node]
  (let [path (:path node)]
    (if (and (seq path) (:outer-slot gen) (= (first path) (:class gen)))
      (this-field-path gen path)
      (do (.visitVarInsn (mv gen) Opcodes/ALOAD 0)
          (this-field-path gen path)))))

(defn load-binding
  "Loads the value of a local binding in gen: a slot, or a captured value (val$ field)."
  [gen b]
  (if-let [s (get @(:slots gen) (:id b))]
    (xload gen (:type b) s)
    (let [c (:class gen)
          caps (:captures @(a/state c))]
      (if (and (not (:lambda? gen)) (some #(= (:id b) (:id %)) caps))
        (do (.visitVarInsn (mv gen) Opcodes/ALOAD 0)
            (.visitFieldInsn (mv gen) Opcodes/GETFIELD c (str "val$" (:sym b)) (:type b)))
        (fail (str "Local not available here: " (:sym b)))))))

(defn- invoke-insn [gen {:keys [kind owner name desc itf]}]
  (.visitMethodInsn (mv gen)
                    (case kind :static Opcodes/INVOKESTATIC :special Opcodes/INVOKESPECIAL
                          :interface Opcodes/INVOKEINTERFACE :virtual Opcodes/INVOKEVIRTUAL)
                    owner name desc (boolean itf)))

(defn- ctor-extra-args
  "The values of the implicit leading/trailing constructor arguments of class cn, as
  [lead-thunks trail-thunks]: the outer instance and captured locals."
  [gen cn outer-node]
  (let [d (a/decl cn)]
    [(when (:outer-instance? (env/info cn))
       [(fn [] (if outer-node (emit gen outer-node :expr)
                   (fail (str "No outer instance for " cn))))])
     (when (and d (#{:local :anon} (:nesting d)))
       (for [b (:captures @(:state d))] (fn [] (load-binding gen b))))]))

(defn ctor-real-desc
  "The descriptor of a constructor of class cn with all implicit parameters."
  [cn m]
  (let [d (a/decl cn)]
    (if-not d
      (let [i (env/info cn)]
        (if (:outer-instance? i)
          (let [[ps _] (t/parse-method-desc (:desc m))]
            (t/method-desc (cons (t/internal->desc (:outer i)) ps) "V"))
          (:desc m)))
      (let [[ps _] (t/parse-method-desc (:desc m))]
        (t/method-desc (concat (when (:outer-instance? d) [(t/internal->desc (:outer d))])
                               (when-let [so (:super-outer d)] [so])
                               (when (or (= :enum (:kind d)) (:enum-body d)) ["Ljava/lang/String;" "I"])
                               ps
                               (when (#{:local :anon} (:nesting d))
                                 (map :type (:captures @(:state d)))))
                       "V")))))

(defn- emit-new [gen node]
  (let [cn (:class node)
        m (:ctor node)
        [lead trail] (ctor-extra-args gen cn (:outer node))
        before (fn []
                 (.visitTypeInsn (mv gen) Opcodes/NEW cn)
                 (write-insn-type-annotations gen arbace.asm.TypeReference/NEW nil (:type-anns node))
                 (insn gen Opcodes/DUP)
                 (when (:outer node)
                   (when-not (a/decl cn) nil))
                 (doseq [f lead] (f))
                 (when-let [so (:super-outer node)] (emit gen so :expr))
                 (when-let [ec (:enum-const node)]
                   (emit-const gen t/string-desc (:name ec))
                   (emit-const gen "I" (:ordinal ec))))]
    (spill-operands gen (:args node) (arg-types m) before)
    (doseq [f trail] (f))
    (.visitMethodInsn (mv gen) Opcodes/INVOKESPECIAL cn "<init>" (ctor-real-desc cn m) false)))

(defn- emit-arith [gen {:keys [family o args type]}]
  (let [t type]
    (if (= family :exact)
      (if (= t "J")
        (do (spill-operands gen args (repeat t))
            (case o
              :add (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/Math" "addExact" "(JJ)J" false)
              :sub (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/Math" "subtractExact" "(JJ)J" false)
              :mul (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/Math" "multiplyExact" "(JJ)J" false)
              :inc (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/Math" "incrementExact" "(J)J" false)
              :dec (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/Math" "decrementExact" "(J)J" false)
              :div (insn gen Opcodes/LDIV)
              :rem (insn gen Opcodes/LREM)))
        (emit-arith gen {:family :long :o o :args args :type t}))
      (do
        (case o
          (:inc :dec) (do (emit-to gen (first args) t) (emit-const gen t 1))
          :not (do (emit-to gen (first args) t) (emit-const gen t -1))
          :andnot (do (emit-to gen (first args) t) (emit-to gen (second args) t)
                      (emit-const gen t -1) (insn gen (opcode t Opcodes/IXOR)))
          (:shl :shr :ushr) (spill-operands gen args [t (:type (second args))])
          (spill-operands gen args (repeat t)))
        (when (and (#{:shl :shr :ushr} o) (= "J" (:type (second args))))
          (insn gen Opcodes/L2I))
        (insn gen (opcode t (case o
                              (:add :inc) Opcodes/IADD (:sub :dec) Opcodes/ISUB :mul Opcodes/IMUL
                              :div Opcodes/IDIV :rem Opcodes/IREM :neg Opcodes/INEG
                              (:and :andnot) Opcodes/IAND :or Opcodes/IOR (:xor :not) Opcodes/IXOR
                              :shl Opcodes/ISHL :shr Opcodes/ISHR :ushr Opcodes/IUSHR)))))))

(defn- emit-bool-value
  "Emits a boolean-valued test as 0/1."
  [gen node]
  (let [f (Label.) end (Label.)]
    (emit-cond gen node false f)
    (insn gen Opcodes/ICONST_1)
    (.visitJumpInsn (mv gen) Opcodes/GOTO end)
    (.visitLabel (mv gen) f)
    (insn gen Opcodes/ICONST_0)
    (.visitLabel (mv gen) end)))

(defn- emit-value-and-or [gen {:keys [op args type] :as node}]
  ;; Clojure's value semantics of and/or on non-boolean values
  (let [t (a/value-type type)]
    (cond
      (= t "Z") (emit-bool-value gen node)
      (t/prim? t)                       ; primitives are always truthy
      (if (= op :or)
        (emit-to gen (first args) t)
        (do (doseq [n (butlast args)] (emit gen n :stmt))
            (emit-to gen (last args) t)))
      :else
      (let [end (Label.)]
        (doseq [n (butlast args)]
          (emit-to gen n t)
          (insn gen Opcodes/DUP)
          (emit-cond gen {:op :stack-truth :type t} (= op :or) end)
          (insn gen Opcodes/POP))
        (emit-to gen (last args) t)
        (.visitLabel (mv gen) end)))))

(declare emit-try emit-monitor emit-loop emit-label emit-jump)

(defn- emit-field-target [gen node]
  (emit gen (:target node) :expr))

(defn emit-expr
  "Emits node, leaving its value on the stack."
  [gen node]
  (let [m (mv gen)]
    (case (:op node)
      :const (emit-const gen (:type node) (:val node))
      :local (load-binding gen (:b node))
      :this-path (emit-this-path gen node)
      :class-lit (let [d (:class node)]
                   (if (t/prim? d)
                     (.visitFieldInsn m Opcodes/GETSTATIC (t/box-of d) "TYPE" "Ljava/lang/Class;")
                     (.visitLdcInsn m (Type/getType ^String d))))
      :get-static (let [f (:field node)]
                    (.visitFieldInsn m Opcodes/GETSTATIC (:owner node) (:name f) (:desc f)))
      :get-field (let [f (:field node)]
                   (emit-to gen (:target node) (t/internal->desc (:owner node)))
                   (.visitFieldInsn m Opcodes/GETFIELD (:owner node) (:name f) (:desc f)))
      :set-static (let [f (:field node)]
                    (emit-to gen (:val node) (:desc f))
                    (insn gen (if (= 2 (t/size (:desc f))) Opcodes/DUP2 Opcodes/DUP))
                    (.visitFieldInsn m Opcodes/PUTSTATIC (:owner node) (:name f) (:desc f)))
      :set-field (let [f (:field node)]
                   (spill-operands gen [(:target node) (:val node)] [(t/internal->desc (:owner node)) (:desc f)])
                   (insn gen (if (= 2 (t/size (:desc f))) Opcodes/DUP2_X1 Opcodes/DUP_X1))
                   (.visitFieldInsn m Opcodes/PUTFIELD (:owner node) (:name f) (:desc f)))
      :set-local (let [b (:b node) s (get @(:slots gen) (:id b))]
                   (emit-to gen (:val node) (:type b))
                   (insn gen (if (= 2 (t/size (:type b))) Opcodes/DUP2 Opcodes/DUP))
                   (xstore gen (:type b) s))
      :invoke (do
                (if (:target node)
                  (spill-operands gen (cons (:target node) (:args node))
                                  (cons (if (:array-clone node) (:owner node)
                                            (let [tt (a/value-type (:type (:target node)))]
                                              (if (t/array? tt) tt (t/internal->desc (:owner node)))))
                                        (arg-types node)))
                  (spill-operands gen (:args node) (arg-types node)))
                (invoke-insn gen node)
                (when (:array-clone node)
                  (.visitTypeInsn m Opcodes/CHECKCAST (:owner node))))
      :new (emit-new gen node)
      :ctor-call (fail "Constructor call outside the constructor prologue")
      :new-array (let [d (:type node) dims (:dims node)]
                   (spill-operands gen dims (repeat "I"))
                   (cond
                     (> (count dims) 1) (.visitMultiANewArrayInsn m d (count dims))
                     (t/prim? (t/elem-type d))
                     (.visitIntInsn m Opcodes/NEWARRAY
                                    (case (t/elem-type d) "Z" Opcodes/T_BOOLEAN "C" Opcodes/T_CHAR
                                          "F" Opcodes/T_FLOAT "D" Opcodes/T_DOUBLE "B" Opcodes/T_BYTE
                                          "S" Opcodes/T_SHORT "I" Opcodes/T_INT "J" Opcodes/T_LONG))
                     :else (.visitTypeInsn m Opcodes/ANEWARRAY (t/desc->internal (t/elem-type d)))))
      :array-init (let [d (:type node) et (t/elem-type d) elems (:elems node)
                        unsafe (some #(unsafe? gen %) elems)
                        temps (when unsafe
                                (doall (for [e elems]
                                         (let [s (alloc-slot gen et)]
                                           (emit-to gen e et) (xstore gen et s) s))))]
                    (emit-const gen "I" (count elems))
                    (if (t/prim? et)
                      (.visitIntInsn m Opcodes/NEWARRAY
                                     (case et "Z" Opcodes/T_BOOLEAN "C" Opcodes/T_CHAR
                                           "F" Opcodes/T_FLOAT "D" Opcodes/T_DOUBLE "B" Opcodes/T_BYTE
                                           "S" Opcodes/T_SHORT "I" Opcodes/T_INT "J" Opcodes/T_LONG))
                      (.visitTypeInsn m Opcodes/ANEWARRAY (t/desc->internal et)))
                    (doseq [[i e] (map-indexed vector elems)]
                      (insn gen Opcodes/DUP)
                      (emit-const gen "I" i)
                      (if temps (xload gen et (nth temps i)) (emit-to gen e et))
                      (insn gen (opcode et Opcodes/IASTORE))))
      :aget (do (spill-operands gen [(:array node) (:index node)] [(a/value-type (:type (:array node))) "I"])
                (insn gen (opcode (:type node) Opcodes/IALOAD)))
      :aset (let [et (:type node)]
              (spill-operands gen [(:array node) (:index node) (:val node)]
                              [(a/value-type (:type (:array node))) "I" et])
              (insn gen (if (= 2 (t/size et)) Opcodes/DUP2_X2 Opcodes/DUP_X2))
              (insn gen (opcode et Opcodes/IASTORE)))
      :alength (do (emit gen (:array node) :expr) (insn gen Opcodes/ARRAYLENGTH))
      :arith (emit-arith gen node)
      :convert (do (emit gen (:expr node) :expr)
                   (emit-convert gen (:from node) (:to node)))
      :null-checked (do (emit gen (:expr node) :expr)
                        (insn gen Opcodes/DUP)
                        (.visitMethodInsn m Opcodes/INVOKESTATIC "java/util/Objects" "requireNonNull"
                                          "(Ljava/lang/Object;)Ljava/lang/Object;" false)
                        (insn gen Opcodes/POP))
      :cast (do (emit gen (:expr node) :expr)
                (when-not (= :none (:type (:expr node)))
                  (.visitTypeInsn m Opcodes/CHECKCAST (t/desc->internal (:class node)))
                  (write-insn-type-annotations gen arbace.asm.TypeReference/CAST (or (:type-arg node) 0)
                                               (:type-anns node))))
      (:compare :not :nil? :identical? :instance? :bool=) (emit-bool-value gen node)
      (:and :or) (emit-value-and-or gen node)
      :if (let [els (Label.) end (Label.) t (a/value-type (:type node))]
            (emit-cond gen (:test node) false els)
            (emit-to gen (:then node) t)
            (when-not (= :none (:type (:then node))) (.visitJumpInsn m Opcodes/GOTO end))
            (.visitLabel m els)
            (emit-to gen (:else node) t)
            (.visitLabel m end))
      :do (do (doseq [s (:statements node)] (emit gen s :stmt))
              (emit gen (:ret node) :expr))
      :let (emit-let gen node :expr)
      :loop (emit-loop gen node :expr)
      :label (emit-label gen node :expr)
      :try (emit-try gen node :expr)
      :monitor (emit-monitor gen node :expr)
      (:break :recur :return) (emit-jump gen node)
      :throw (do (emit gen (:expr node) :expr) (insn gen Opcodes/ATHROW))
      :var-deref (do (.visitFieldInsn m Opcodes/GETSTATIC (:owner (:field node)) (:name (:field node)) "Lclojure/lang/Var;")
                     (.visitMethodInsn m Opcodes/INVOKEVIRTUAL "clojure/lang/Var" "deref" "()Ljava/lang/Object;" false))
      :var-invoke (let [args (:args node)]
                    (when (> (count args) 20) (fail "Too many arguments for a var call"))
                    (spill-operands gen (cons {:op :var-deref :var (:var node) :field (:field node) :type t/object-desc} args)
                                    (cons "Lclojure/lang/IFn;" (repeat t/object-desc))
                                    nil)
                    (.visitMethodInsn m Opcodes/INVOKEINTERFACE "clojure/lang/IFn" "invoke"
                                      (t/method-desc (repeat (count args) t/object-desc) t/object-desc) true))
      (if-let [f (get-method emit-extra (:op node))]
        (f gen node :expr)
        (fail (str "Cannot emit " (:op node)))))))

(def ^:private pure-ops #{:const :local :this-path :class-lit})

(defn emit-let
  "let: each local gets a slot (freed after the body); type annotations of the locals cover
  their scope."
  [gen node ctx]
  (let [saved @(:next gen)
        opened (doall
                 (for [[b init] (:bindings node)]
                   (do (emit-to gen init (:type b))
                       (let [s (alloc-slot gen (:type b))]
                         (swap! (:slots gen) assoc (:id b) s)
                         (when-not (= :none (:type init)) (xstore gen (:type b) s))
                         (when (seq (:type-anns b))
                           (let [l (Label.)] (.visitLabel (mv gen) l) [b l s]))))))]
    (emit gen (:body node) ctx)
    (when (some some? opened)
      (let [end (Label.)]
        (.visitLabel (mv gen) end)
        (doseq [[b start s] (remove nil? opened)]
          (write-local-type-annotations gen b start end s))))
    (when-not (= ctx :return) (reset! (:next gen) saved))))

(defn emit
  "Emits node in context ctx: :expr leaves its value (void gives nil), :stmt leaves nothing,
  :return returns it from the method."
  [gen node ctx]
  (case ctx
    :return
    (case (:op node)
      :if (let [els (Label.)]
            (emit-cond gen (:test node) false els)
            (emit gen (:then node) :return)
            (.visitLabel (mv gen) els)
            (emit gen (:else node) :return))
      :do (do (doseq [s (:statements node)] (emit gen s :stmt))
              (emit gen (:ret node) :return))
      :let (emit-let gen node :return)
      :loop (emit-loop gen node :return)
      (:break :recur :return :throw) (emit gen node :expr)
      (let [ret (:ret gen)]
        (if (= ret "V")
          (do (emit gen node :stmt)
              (when-not (= :none (:type node)) (insn gen Opcodes/RETURN)))
          (do (emit-to gen node ret)
              (when-not (= :none (:type node)) (insn gen (opcode ret Opcodes/IRETURN)))))))
    :stmt
    (case (:op node)
      (:const :local :this-path :class-lit) nil
      :do (do (doseq [s (:statements node)] (emit gen s :stmt))
              (emit gen (:ret node) :stmt))
      :let (emit-let gen node :stmt)
      :if (let [els (Label.) end (Label.)]
            (emit-cond gen (:test node) false els)
            (emit gen (:then node) :stmt)
            (when-not (= :none (:type (:then node))) (.visitJumpInsn (mv gen) Opcodes/GOTO end))
            (.visitLabel (mv gen) els)
            (emit gen (:else node) :stmt)
            (.visitLabel (mv gen) end))
      :set-local (let [b (:b node) s (get @(:slots gen) (:id b))]
                   (emit-to gen (:val node) (:type b))
                   (xstore gen (:type b) s))
      :set-static (let [f (:field node)]
                    (emit-to gen (:val node) (:desc f))
                    (.visitFieldInsn (mv gen) Opcodes/PUTSTATIC (:owner node) (:name f) (:desc f)))
      :set-field (let [f (:field node)]
                   (spill-operands gen [(:target node) (:val node)] [(t/internal->desc (:owner node)) (:desc f)])
                   (.visitFieldInsn (mv gen) Opcodes/PUTFIELD (:owner node) (:name f) (:desc f)))
      :aset (let [et (:type node)]
              (spill-operands gen [(:array node) (:index node) (:val node)]
                              [(a/value-type (:type (:array node))) "I" et])
              (insn gen (opcode et Opcodes/IASTORE)))
      :loop (emit-loop gen node :stmt)
      :label (emit-label gen node :stmt)
      :try (emit-try gen node :stmt)
      :monitor (emit-monitor gen node :stmt)
      :ctor-call (fail "Constructor call outside the constructor prologue")
      (if (and (get-method emit-extra (:op node)) (not= (:op node) :default))
        ((get-method emit-extra (:op node)) gen node :stmt)
        (do (emit-expr gen node)
            (pop-type gen (:type node)))))
    :expr
    (do (emit-expr gen node)
        (when (= "V" (:type node)) (insn gen Opcodes/ACONST_NULL)))))

;; ---------------------------------------------------------------------------------------------
;; conditions

(def ^:private cmp-jumps
  ;; [jump-if-true opcode for int compare, for compare-to-zero after lcmp etc.]
  {:< [Opcodes/IF_ICMPLT Opcodes/IFLT] :<= [Opcodes/IF_ICMPLE Opcodes/IFLE]
   :> [Opcodes/IF_ICMPGT Opcodes/IFGT] :>= [Opcodes/IF_ICMPGE Opcodes/IFGE]
   :== [Opcodes/IF_ICMPEQ Opcodes/IFEQ]})

(def ^:private negate-cmp {:< :>= :<= :> :> :<= :>= :< :== :!= :!= :==})
(def ^:private cmp-jumps2
  (assoc cmp-jumps :!= [Opcodes/IF_ICMPNE Opcodes/IFNE]))

(defn emit-cond
  "Emits a test: jumps to label when the node's truth equals jump-if, else falls through."
  [gen node jump-if ^Label label]
  (let [m (mv gen)]
    (case (:op node)
      :const (let [v (:val node)
                   truth (not (or (nil? v) (false? v)))]
               (when (= truth jump-if) (.visitJumpInsn m Opcodes/GOTO label)))
      :not (emit-cond gen (:expr node) (not jump-if) label)
      :and (if (not= "Z" (:type node))
             (emit-cond gen {:op :truth :expr node :type (:type node)} jump-if label)
             (if jump-if
               (let [skip (Label.)]
                 (doseq [n (butlast (:args node))] (emit-cond gen n false skip))
                 (emit-cond gen (last (:args node)) true label)
                 (.visitLabel m skip))
               (doseq [n (:args node)] (emit-cond gen n false label))))
      :or (if (not= "Z" (:type node))
            (emit-cond gen {:op :truth :expr node :type (:type node)} jump-if label)
            (if jump-if
              (doseq [n (:args node)] (emit-cond gen n true label))
              (let [skip (Label.)]
                (doseq [n (butlast (:args node))] (emit-cond gen n true skip))
                (emit-cond gen (last (:args node)) false label)
                (.visitLabel m skip))))
      :nil? (do (emit gen (:expr node) :expr)
                (.visitJumpInsn m (if jump-if Opcodes/IFNULL Opcodes/IFNONNULL) label))
      :identical? (do (spill-operands gen (:args node) [nil nil])
                      (.visitJumpInsn m (if jump-if Opcodes/IF_ACMPEQ Opcodes/IF_ACMPNE) label))
      :instance? (do (emit gen (:expr node) :expr)
                     (.visitTypeInsn m Opcodes/INSTANCEOF (t/desc->internal (:class node)))
                     (write-insn-type-annotations gen arbace.asm.TypeReference/INSTANCEOF nil (:type-anns node))
                     (.visitJumpInsn m (if jump-if Opcodes/IFNE Opcodes/IFEQ) label))
      :bool= (do (spill-operands gen (:args node) ["Z" "Z"])
                 (.visitJumpInsn m (if jump-if Opcodes/IF_ICMPEQ Opcodes/IF_ICMPNE) label))
      :compare (let [{:keys [cmp t args]} node
                     c (if jump-if cmp (negate-cmp cmp))]
                 (spill-operands gen args [t t])
                 (case t
                   "I" (.visitJumpInsn m (first (cmp-jumps2 c)) label)
                   "J" (do (insn gen Opcodes/LCMP) (.visitJumpInsn m (second (cmp-jumps2 c)) label))
                   ("F" "D")
                   ;; NaN makes every comparison but != false: choose cmpg/cmpl so it does
                   (let [nan-true? (= c :!=)
                         ;; with cmpg NaN gives 1, with cmpl -1
                         g? (case c (:< :<=) true (:> :>=) false (:== :!=) true)]
                     (insn gen (if (= t "F") (if g? Opcodes/FCMPG Opcodes/FCMPL)
                                   (if g? Opcodes/DCMPG Opcodes/DCMPL)))
                     (.visitJumpInsn m (second (cmp-jumps2 c)) label))))
      :stack-truth (let [t (:type node)]       ; a reference value on the stack, consumed
                     (if (or (= t "Ljava/lang/Boolean;") (= t t/object-desc))
                       (let [no (Label.)]
                         (if jump-if
                           (do (insn gen Opcodes/DUP)
                               (.visitJumpInsn m Opcodes/IFNULL no)
                               (.visitFieldInsn m Opcodes/GETSTATIC "java/lang/Boolean" "FALSE" "Ljava/lang/Boolean;")
                               (.visitJumpInsn m Opcodes/IF_ACMPNE label)
                               (let [e (Label.)]
                                 (.visitJumpInsn m Opcodes/GOTO e)
                                 (.visitLabel m no) (insn gen Opcodes/POP)
                                 (.visitLabel m e)))
                           (do (insn gen Opcodes/DUP)
                               (.visitJumpInsn m Opcodes/IFNULL no)
                               (.visitFieldInsn m Opcodes/GETSTATIC "java/lang/Boolean" "FALSE" "Ljava/lang/Boolean;")
                               (.visitJumpInsn m Opcodes/IF_ACMPEQ label)
                               (let [e (Label.)]
                                 (.visitJumpInsn m Opcodes/GOTO e)
                                 (.visitLabel m no) (insn gen Opcodes/POP)
                                 (.visitJumpInsn m Opcodes/GOTO label)
                                 (.visitLabel m e)))))
                       (.visitJumpInsn m (if jump-if Opcodes/IFNONNULL Opcodes/IFNULL) label)))
      ;; any other value: Clojure truth
      (let [n (if (= :truth (:op node)) (:expr node) node)
            t (a/value-type (:type n))]
        (cond
          (= t :none) (emit gen n :expr)
          (= t "Z") (do (emit gen n :expr) (.visitJumpInsn m (if jump-if Opcodes/IFNE Opcodes/IFEQ) label))
          (t/prim? t) (do (emit gen n :stmt) (when jump-if (.visitJumpInsn m Opcodes/GOTO label)))
          (= t :null) (do (emit gen n :stmt) (when-not jump-if (.visitJumpInsn m Opcodes/GOTO label)))
          :else (do (emit gen n :expr)
                    (emit-cond gen {:op :stack-truth :type t} jump-if label)))))))

;; ---------------------------------------------------------------------------------------------
;; loops, labels, jumps, try, monitors
;;
;; targets: id -> {:start :end :depth (cleanups at the target) :ctx :type :slots}
;; cleanups: [{:kind :finally :node n :segments atom} {:kind :monitor :slot s :segments atom}]

(defn- open-segment! [gen cl]
  (let [l (Label.)] (.visitLabel (mv gen) l) (swap! (:segments cl) conj [l nil])))

(defn- close-segment! [gen cl]
  (let [l (Label.)]
    (.visitLabel (mv gen) l)
    (swap! (:segments cl) (fn [segs] (conj (pop segs) [(first (peek segs)) l])))))

(defn- run-cleanup [gen i]
  (let [cl (nth (:cleanups gen) i)
        inner (assoc gen :cleanups (subvec (:cleanups gen) 0 i))]
    (close-segment! gen cl)
    (case (:kind cl)
      :finally (emit inner (:node cl) :stmt)
      :monitor (do (.visitVarInsn (mv gen) Opcodes/ALOAD (:slot cl))
                   (insn gen Opcodes/MONITOREXIT)))
    (open-segment! gen cl)))

(defn- run-cleanups-to [gen depth]
  (doseq [i (range (dec (count (:cleanups gen))) (dec depth) -1)]
    (run-cleanup gen i)))

(defn emit-jump [gen node]
  (let [m (mv gen)]
    (case (:op node)
      :return
      (let [ret (:ret gen) v (:val node)]
        (if (= ret "V")
          (do (when v (emit gen v :stmt))
              (run-cleanups-to gen 0)
              (insn gen Opcodes/RETURN))
          (do (emit-to gen v ret)
              (when (seq (:cleanups gen))
                (let [s (alloc-slot gen ret)]
                  (xstore gen ret s)
                  (run-cleanups-to gen 0)
                  (xload gen ret s)))
              (insn gen (opcode ret Opcodes/IRETURN)))))
      :break
      (let [tg (or (get (:targets gen) (:id (:target node))) (fail "break target not in scope"))
            v (:val node)]
        (if (= :expr (:ctx tg))
          (let [ty (:type tg)]
            (emit-to gen v ty)
            (when (> (count (:cleanups gen)) (:depth tg))
              (let [s (alloc-slot gen ty)]
                (xstore gen ty s)
                (run-cleanups-to gen (:depth tg))
                (xload gen ty s))))
          (do (emit gen v :stmt)
              (run-cleanups-to gen (:depth tg))))
        (.visitJumpInsn m Opcodes/GOTO (:end tg)))
      :recur
      (let [tg (or (get (:targets gen) (:id (:target node))) (fail "recur target not in scope"))
            args (:args node)
            slots (:slots tg)
            types (:types tg)]
        (spill-operands gen args types)
        (if (> (count (:cleanups gen)) (:depth tg))
          (let [temps (doall (for [ty (reverse types)]
                               (let [s (alloc-slot gen ty)] (xstore gen ty s) s)))]
            (run-cleanups-to gen (:depth tg))
            (doseq [[ty tmp sl] (map vector (reverse types) temps (reverse slots))]
              (xload gen ty tmp) (xstore gen ty sl)))
          (doseq [[ty sl] (reverse (map vector types slots))]
            (xstore gen ty sl)))
        (.visitJumpInsn m Opcodes/GOTO (:start tg))))))

(defn emit-loop [gen node ctx]
  (let [m (mv gen)
        saved @(:next gen)
        bs (:bindings node)
        slots (doall (for [[b init] bs]
                       (do (emit-to gen init (:type b))
                           (let [s (alloc-slot gen (:type b))]
                             (swap! (:slots gen) assoc (:id b) s)
                             (xstore gen (:type b) s)
                             s))))
        start (Label.) end (Label.)
        t (a/value-type (:type node))
        tg {:start start :end end :depth (count (:cleanups gen)) :ctx (if (= ctx :stmt) :stmt :expr)
            :type t :slots (vec slots) :types (mapv (comp :type first) bs)}
        gen2 (update gen :targets assoc (:id (:target node)) tg)]
    (.visitLabel m start)
    (case ctx
      :return (emit gen2 (:body node) :return)
      :stmt (emit gen2 (:body node) :stmt)
      :expr (emit-to gen2 (:body node) t))
    (.visitLabel m end)
    (when (and (= ctx :return) (seq @(:breaks (:target node))))
      ;; a (break) leaves the loop with its value: return it
      (let [ret (:ret gen)]
        (if (= ret "V") (insn gen Opcodes/RETURN)
            (do (emit-convert gen t ret) (insn gen (opcode ret Opcodes/IRETURN))))))
    (reset! (:next gen) saved)))

(defn emit-label [gen node ctx]
  (let [end (Label.)
        t (a/value-type (:type node))
        tg {:end end :depth (count (:cleanups gen)) :ctx ctx :type t}
        gen2 (update gen :targets assoc (:id (:target node)) tg)]
    (if (= ctx :stmt)
      (emit gen2 (:body node) :stmt)
      (emit-to gen2 (:body node) t))
    (.visitLabel (mv gen) end)))

(defn- register-handlers [gen segments handler type]
  (doseq [[^Label s ^Label e] segments]
    (when (and s e (< (.getOffset s) (.getOffset e)))
      (.visitTryCatchBlock (mv gen) s e handler type))))

(defn emit-try [gen node ctx]
  (let [m (mv gen)
        nfin (:normal-finally node)
        fin (:finally node)
        t (a/value-type (:type node))
        expr? (and (= ctx :expr) (not= t :none))
        rt (if (= t :null) t/object-desc t)
        rslot (when expr? (alloc-slot gen rt))
        cl {:kind :finally :node (or fin nfin) :segments (atom [])}
        gen-body (if (or fin nfin) (update gen :cleanups conj cl) gen)
        end (Label.)
        complete (fn [n] (not= :none (:type n)))]
    ;; the body; a jump out of it runs the finally code inline, outside the handled ranges
    (open-segment! gen cl)
    (if expr? (emit-to gen-body (:body node) rt) (emit gen-body (:body node) :stmt))
    (close-segment! gen cl)
    (let [body-segs @(:segments cl)]
      (when (complete (:body node))
        (when expr? (xstore gen rt rslot))
        (when (or fin nfin) (emit gen (or fin nfin) :stmt))
        (.visitJumpInsn m Opcodes/GOTO end))
      (let [handlers
            (doall
              (for [c (:catches node)]
                (let [h (Label.)
                      b (:b c)
                      s (alloc-slot gen (:type b))]
                  (.visitLabel m h)
                  (swap! (:slots gen) assoc (:id b) s)
                  (swap! (:segments cl) conj [h nil])
                  (xstore gen (:type b) s)
                  (if expr? (emit-to (if nfin gen gen-body) (:body c) rt)
                      (emit (if nfin gen gen-body) (:body c) :stmt))
                  (close-segment! gen cl)
                  (when (complete (:body c))
                    (when expr? (xstore gen rt rslot))
                    (when fin (emit gen fin :stmt))
                    (.visitJumpInsn m Opcodes/GOTO end))
                  [h (:classes c)])))]
        (doseq [[h classes] handlers
                c classes]
          (register-handlers gen body-segs h c))
        (when fin
          (let [h (Label.) s (alloc-slot gen t/object-desc)]
            (.visitLabel m h)
            (.visitVarInsn m Opcodes/ASTORE s)
            (emit gen fin :stmt)
            (.visitVarInsn m Opcodes/ALOAD s)
            (insn gen Opcodes/ATHROW)
            (register-handlers gen @(:segments cl) h nil)))))
    (.visitLabel m end)
    (when expr? (xload gen rt rslot))))

(defn emit-monitor [gen node ctx]
  (let [m (mv gen)
        t (a/value-type (:type node))
        expr? (and (= ctx :expr) (not= t :none))
        rt (if (= t :null) t/object-desc t)
        lslot (alloc-slot gen t/object-desc)
        cl {:kind :monitor :slot lslot :segments (atom [])}
        gen-body (update gen :cleanups conj cl)
        end (Label.)]
    (emit gen (:lock node) :expr)
    (insn gen Opcodes/DUP)
    (.visitVarInsn m Opcodes/ASTORE lslot)
    (insn gen Opcodes/MONITORENTER)
    (open-segment! gen cl)
    (if expr? (emit-to gen-body (:body node) rt) (emit gen-body (:body node) :stmt))
    (let [rslot (when expr? (alloc-slot gen rt))]
      (when (not= :none (:type (:body node)))
        (when expr? (xstore gen rt rslot))
        (.visitVarInsn m Opcodes/ALOAD lslot)
        (insn gen Opcodes/MONITOREXIT))
      (close-segment! gen cl)
      (when (not= :none (:type (:body node)))
        (.visitJumpInsn m Opcodes/GOTO end))
      (let [h (Label.) h-end (Label.) s (alloc-slot gen t/object-desc)]
        (.visitLabel m h)
        (.visitVarInsn m Opcodes/ASTORE s)
        (.visitVarInsn m Opcodes/ALOAD lslot)
        (insn gen Opcodes/MONITOREXIT)
        (.visitLabel m h-end)
        (.visitVarInsn m Opcodes/ALOAD s)
        (insn gen Opcodes/ATHROW)
        (register-handlers gen @(:segments cl) h nil)
        (register-handlers gen [[h h-end]] h nil))
      (.visitLabel m end)
      (when expr? (xload gen rt rslot)))))

;; ---------------------------------------------------------------------------------------------
;; annotations

(defn- ann-const-value [{:keys [const]}]
  (let [v (:val const) t (:type const)]
    (case t
      "I" (int v) "J" (long v) "S" (short v) "B" (byte v) "C" (char v) "F" (float v)
      "D" (double v) "Z" (boolean v) v)))

(defn write-ann-value [^AnnotationVisitor av name v]
  (cond
    (:array v) (let [aav (.visitArray av name)]
                 (doseq [x (:array v)] (write-ann-value aav nil x))
                 (.visitEnd aav))
    (:annotation v) (let [an (:annotation v)
                          nav (.visitAnnotation av name (:type an))]
                      (doseq [[k x] (:values an)] (write-ann-value nav k x))
                      (.visitEnd nav))
    (:class v) (.visit av name (Type/getType ^String (:class v)))
    (:enum v) (.visitEnum av name (:enum v) (:name v))
    (:const v) (.visit av name (ann-const-value v))
    :else (fail (str "Bad annotation value " (pr-str v)))))

(defn write-type-annotations
  "visit-fn: (fn [type-ref type-path desc visible])."
  [visit-fn anns]
  (doseq [{:keys [ref path type visible values]} (force anns)]
    (let [^AnnotationVisitor av (visit-fn (int ref) (when (seq path) (arbace.asm.TypePath/fromString path)) type visible)]
      (doseq [[k v] values] (write-ann-value av k v))
      (.visitEnd av))))

(defn write-insn-type-annotations
  "Type annotations of the instruction just emitted (cast, instanceof, new)."
  [gen sort type-arg anns]
  (doseq [[path {:keys [type visible values]}] anns]
    (let [ref (if type-arg (arbace.asm.TypeReference/newTypeArgumentReference sort type-arg)
                  (arbace.asm.TypeReference/newTypeReference sort))
          ^AnnotationVisitor av (.visitInsnAnnotation (mv gen) (.getValue ref)
                                                      (when (seq path) (arbace.asm.TypePath/fromString path))
                                                      type visible)]
      (doseq [[k v] values] (write-ann-value av k v))
      (.visitEnd av))))

(defn write-local-type-annotations
  "Type annotations of a local variable live from start to end in slot."
  [gen b start end slot]
  (doseq [[path {:keys [type visible values]}] (:type-anns b)]
    (let [^AnnotationVisitor av (.visitLocalVariableAnnotation
                                  (mv gen) (.getValue (arbace.asm.TypeReference/newTypeReference
                                                        arbace.asm.TypeReference/LOCAL_VARIABLE))
                                  (when (seq path) (arbace.asm.TypePath/fromString path))
                                  (into-array Label [start]) (into-array Label [end]) (int-array [slot])
                                  type visible)]
      (doseq [[k v] values] (write-ann-value av k v))
      (.visitEnd av))))

(defn write-annotations [visit-fn anns]
  (doseq [{:keys [type visible values]} (force anns)]
    (let [^AnnotationVisitor av (visit-fn type visible)]
      (doseq [[k v] values] (write-ann-value av k v))
      (.visitEnd av))))

;; ---------------------------------------------------------------------------------------------
;; classes

(defn class-writer []
  (proxy [ClassWriter] [ClassWriter/COMPUTE_FRAMES]
    (getCommonSuperClass [a b] (env/common-super a b))))

(defn new-gen [^MethodVisitor mv cls ret static?]
  {:mv mv :class cls :slots (atom {}) :next (atom (if static? 0 1)) :ret ret :targets {}
   :cleanups [] :unsafe (java.util.IdentityHashMap.) :finish (atom {})})

(defn finish-gen!
  "Code emitted once at the end of a method (the MatchException handler of record patterns)."
  [gen]
  (doseq [[_ f] @(:finish gen) :when (fn? f)] (f)))

(defn- has? [flags f] (not (zero? (bit-and (or flags 0) f))))

(defn serializable? [n]
  (env/assignable? (t/internal->desc n) "Ljava/io/Serializable;"))

(defn this0-field? [d]
  (and (:outer-instance? d)
       (or (:uses-this0 @(:state d)) (serializable? (:name d)))))

(defn- const-attr-value [desc v]
  (case desc
    ("I" "S" "B") (Integer/valueOf (int v))
    "C" (Integer/valueOf (int (char v)))
    "Z" (Integer/valueOf (if v 1 0))
    "J" (Long/valueOf (long v)) "F" (Float/valueOf (float v)) "D" (Double/valueOf (double v))
    v))

(defn- bind-param-slots! [gen bs]
  (doseq [b bs]
    (let [s (alloc-slot gen (:type b))]
      (swap! (:slots gen) assoc (:id b) s))))

(defn- emit-assign-fields-from-params [gen d bs]
  (doseq [[b c] (map vector bs (:components d))]
    (.visitVarInsn (mv gen) Opcodes/ALOAD 0)
    (load-binding gen b)
    (.visitFieldInsn (mv gen) Opcodes/PUTFIELD (:name d) (:name c) (:desc c))))

(defn- emit-ctor-call [gen d ab]
  (let [call (:call ab)
        m (mv gen)]
    (cond
      (:enum-args call)
      (do (.visitVarInsn m Opcodes/ALOAD 0)
          (.visitVarInsn m Opcodes/ALOAD (:enum-slot gen))
          (.visitVarInsn m Opcodes/ILOAD (inc (:enum-slot gen)))
          (.visitMethodInsn m Opcodes/INVOKESPECIAL "java/lang/Enum" "<init>" "(Ljava/lang/String;I)V" false))

      (:anon-args call)
      (do (when-let [s (:super-outer-slot gen)]
            ;; javac null-checks the superclass's outer instance again
            (.visitVarInsn m Opcodes/ALOAD s)
            (.visitInsn m Opcodes/DUP)
            (.visitMethodInsn m Opcodes/INVOKESTATIC "java/util/Objects" "requireNonNull"
                              "(Ljava/lang/Object;)Ljava/lang/Object;" false)
            (.visitInsn m Opcodes/POP)
            (.visitInsn m Opcodes/POP))
          (.visitVarInsn m Opcodes/ALOAD 0)
          (when-let [s (:super-outer-slot gen)] (.visitVarInsn m Opcodes/ALOAD s))
          (when (:enum-slot gen)
            (.visitVarInsn m Opcodes/ALOAD (:enum-slot gen))
            (.visitVarInsn m Opcodes/ILOAD (inc (:enum-slot gen))))
          (doseq [b (:params ab)] (load-binding gen b))
          (.visitMethodInsn m Opcodes/INVOKESPECIAL (:class call) "<init>"
                            (ctor-real-desc (:class call) (:ctor call)) false))

      (= :this (:kind call))
      (let [self (:name d)]
        (spill-operands gen (:args call) (arg-types (:ctor call))
                        (fn []
                          (.visitVarInsn m Opcodes/ALOAD 0)
                          (when (:outer-slot gen) (.visitVarInsn m Opcodes/ALOAD (:outer-slot gen)))
                          (when (:enum-slot gen)
                            (.visitVarInsn m Opcodes/ALOAD (:enum-slot gen))
                            (.visitVarInsn m Opcodes/ILOAD (inc (:enum-slot gen))))))
        (when (#{:local :anon} (:nesting d))
          (doseq [b (:captures @(:state d))] (load-binding gen b)))
        (.visitMethodInsn m Opcodes/INVOKESPECIAL self "<init>" (ctor-real-desc self (:ctor call)) false))

      :else
      (let [cn (:class call)
            [lead trail] (ctor-extra-args gen cn (:outer call))]
        (spill-operands gen (:args call) (arg-types (:ctor call))
                        (fn [] (.visitVarInsn m Opcodes/ALOAD 0) (doseq [f lead] (f))))
        (doseq [f trail] (f))
        (.visitMethodInsn m Opcodes/INVOKESPECIAL cn "<init>" (ctor-real-desc cn (:ctor call)) false)))))

(defn- emit-ctor-code [^MethodVisitor mv d m ab]
  (let [n (:name d)
        gen (new-gen mv n "V" false)
        st @(:state d)
        gen (if (:outer-instance? d) (assoc gen :outer-slot (alloc-slot gen t/object-desc)) gen)
        gen (if (:super-outer d) (assoc gen :super-outer-slot (alloc-slot gen t/object-desc)) gen)
        gen (if (or (= :enum (:kind d)) (:enum-body d))
              (let [s (alloc-slot gen t/string-desc)] (alloc-slot gen "I") (assoc gen :enum-slot s))
              gen)
        _ (when-let [r (:recv ab)] (swap! (:slots gen) assoc (:id r) 0))
        _ (bind-param-slots! gen (:params ab))
        caps (when (#{:local :anon} (:nesting d)) (:captures st))
        cap-slots (doall (for [b caps] (let [s (alloc-slot gen (:type b))] [b s])))]
    (.visitCode mv)
    (when (:calls-super ab)
      (doseq [[b s] cap-slots]
        (.visitVarInsn mv Opcodes/ALOAD 0)
        (xload gen (:type b) s)
        (.visitFieldInsn mv Opcodes/PUTFIELD n (str "val$" (:sym b)) (:type b)))
      ;; javac null-checks the outer instance, and stores it when it is used
      (when (:outer-instance? d)
        (let [store? (this0-field? d)]
          (when store? (.visitVarInsn mv Opcodes/ALOAD 0))
          (.visitVarInsn mv Opcodes/ALOAD (:outer-slot gen))
          (when store? (.visitInsn mv Opcodes/DUP))
          (.visitMethodInsn mv Opcodes/INVOKESTATIC "java/util/Objects" "requireNonNull"
                            "(Ljava/lang/Object;)Ljava/lang/Object;" false)
          (.visitInsn mv Opcodes/POP)
          (when store?
            (.visitFieldInsn mv Opcodes/PUTFIELD n "this$0" (t/internal->desc (:outer d)))))))
    ;; before the superclass constructor call captured values come from the parameters, after
    ;; it from the val$ fields, as javac does
    (doseq [[b s] cap-slots] (swap! (:slots gen) assoc (:id b) s))
    (doseq [p (:prologue ab)] (emit gen p :stmt))
    (emit-ctor-call gen d ab)
    ;; field initializers read the outer instance and captured values from the fields (javac
    ;; translates them in the class), the constructor body from the parameters
    (when (:calls-super ab)
      (doseq [[b _] cap-slots] (swap! (:slots gen) dissoc (:id b)))
      (let [igen (if (this0-field? d) (dissoc gen :outer-slot) gen)]
        (doseq [i (:init st)] (emit igen i :stmt)))
      (doseq [[b s] cap-slots] (swap! (:slots gen) assoc (:id b) s)))
    (when-let [b (:body ab)] (emit gen b :stmt))
    (when (and (= :record (:kind d)) (or (:compact m) (= :record-canonical (:derived m))))
      (emit-assign-fields-from-params gen d (:params ab)))
    (when (or (nil? (:body ab)) (not= :none (:type (:body ab))))
      (.visitInsn mv Opcodes/RETURN))
    (finish-gen! gen)))

(defn- method-parameters
  "The MethodParameters entries [[name-or-nil flags] ...] javac writes for method m of d, or nil."
  [d m]
  (let [ctor? (= "<init>" (:name m))
        names? (and ctor? (= :record (:kind d)) (:canonical m))
        extra (when ctor?
                (concat (when (:outer-instance? d) [["this$0" (bit-or Opcodes/ACC_FINAL Opcodes/ACC_MANDATED)]])
                        (when (:super-outer d) [["x0" (bit-or Opcodes/ACC_FINAL Opcodes/ACC_MANDATED)]])
                        (when (or (= :enum (:kind d)) (:enum-body d)) [["$enum$name" Opcodes/ACC_SYNTHETIC]
                                                   ["$enum$ordinal" Opcodes/ACC_SYNTHETIC]])))
        params (or (:mparams m)
                   (map (fn [p] [(some-> (:sym p) name) (bit-and (or (:flags p) 0) Opcodes/ACC_FINAL)]) (:params m)))
        caps (when (and ctor? (#{:local :anon} (:nesting d)))
               (for [b (:captures @(:state d))] [(str "val$" (:sym b)) (bit-or Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC)]))
        all (concat extra params caps)
        flagged (some #(has? (second %) (bit-or Opcodes/ACC_SYNTHETIC Opcodes/ACC_MANDATED)) all)]
    (when (and (seq all) (or names? flagged))
      (mapv (fn [[nm fl]] [(when names? nm) fl]) all))))

(defn- emit-derived-code [^MethodVisitor mv d m]
  (let [n (:name d)
        self (t/internal->desc n)
        gen (new-gen mv n (:ret m) (has? (:flags m) Opcodes/ACC_STATIC))]
    (.visitCode mv)
    (case (:derived m)
      :enum-values
      (do (.visitFieldInsn mv Opcodes/GETSTATIC n "$VALUES" (str "[" self))
          (.visitMethodInsn mv Opcodes/INVOKEVIRTUAL (str "[" self) "clone" "()Ljava/lang/Object;" false)
          (.visitTypeInsn mv Opcodes/CHECKCAST (str "[" self))
          (.visitInsn mv Opcodes/ARETURN))
      :enum-valueOf
      (do (.visitLdcInsn mv (Type/getType ^String self))
          (.visitVarInsn mv Opcodes/ALOAD 0)
          (.visitMethodInsn mv Opcodes/INVOKESTATIC "java/lang/Enum" "valueOf"
                            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;" false)
          (.visitTypeInsn mv Opcodes/CHECKCAST n)
          (.visitInsn mv Opcodes/ARETURN))
      :enum-$values
      (let [cs (:constants d)]
        (emit-const gen "I" (count cs))
        (.visitTypeInsn mv Opcodes/ANEWARRAY n)
        (doseq [[i c] (map-indexed vector cs)]
          (.visitInsn mv Opcodes/DUP)
          (emit-const gen "I" i)
          (.visitFieldInsn mv Opcodes/GETSTATIC n (:name c) self)
          (.visitInsn mv Opcodes/AASTORE))
        (.visitInsn mv Opcodes/ARETURN))
      :bridge
      (let [target (:bridge-of m)
            [tps tr] (t/parse-method-desc (:desc target))
            [bps br] (t/parse-method-desc (:desc m))
            itf (env/interface? n)]
        (.visitVarInsn mv Opcodes/ALOAD 0)
        (loop [[[bp tp] & more] (map vector bps tps) slot 1]
          (when bp
            (xload gen bp slot)
            (when (and (not= bp tp) (t/ref? tp))
              (.visitTypeInsn mv Opcodes/CHECKCAST (t/desc->internal tp)))
            (recur more (+ slot (t/size bp)))))
        (if (:special m)
          (.visitMethodInsn mv Opcodes/INVOKESPECIAL (:owner target) (:name m) (:desc target)
                            (boolean (env/interface? (:owner target))))
          (.visitMethodInsn mv (if itf Opcodes/INVOKEINTERFACE Opcodes/INVOKEVIRTUAL) n (:name m) (:desc target) itf))
        (.visitInsn mv (opcode br Opcodes/IRETURN)))
      :record-accessor
      (let [c (:component m)]
        (.visitVarInsn mv Opcodes/ALOAD 0)
        (.visitFieldInsn mv Opcodes/GETFIELD n (:name c) (:desc c))
        (.visitInsn mv (opcode (:desc c) Opcodes/IRETURN)))
      :record-object-method
      (let [comps (:components d)
            bsm (Handle. Opcodes/H_INVOKESTATIC "java/lang/runtime/ObjectMethods" "bootstrap"
                         (str "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                              "Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;"
                              "[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;")
                         false)
            bargs (into [(Type/getType ^String self) (str/join ";" (map :name comps))]
                        (map #(Handle. Opcodes/H_GETFIELD n (:name %) (:desc %) false) comps))
            [ps r] (t/parse-method-desc (:desc m))]
        (.visitVarInsn mv Opcodes/ALOAD 0)
        (when (= "equals" (:name m)) (.visitVarInsn mv Opcodes/ALOAD 1))
        (.visitInvokeDynamicInsn mv (:name m) (t/method-desc (cons self ps) r) bsm (object-array bargs))
        (.visitInsn mv (opcode r Opcodes/IRETURN))))))

(defn- emit-method [^ClassWriter cw d m]
  (let [n (:name d)
        st @(:state d)
        ctor? (= "<init>" (:name m))
        desc (if ctor? (ctor-real-desc n m) (:desc m))
        exc (when (seq (:throws m)) (into-array String (:throws m)))
        ;; javac gives constructors with captured locals a Signature of the declared parameters,
        ;; except those of anonymous subclasses of classes
        sig (or (:sig m)
                (when (and ctor?
                           (or (and (#{:local :anon} (:nesting d)) (seq (:captures st))
                                    (or (= :local (:nesting d)) (seq (:interfaces d))))
                               (= :enum (:kind d))))
                  (str "(" (apply str (map #(if (:tn %) (t/signature (:tn %)) (:desc %)) (:params m))) ")V")))
        mv (.visitMethod cw (:flags m) (:name m) desc sig exc)
        ab (get (:bodies st) [(:name m) (:desc m)])]
    (when-let [mp (method-parameters d m)]
      (doseq [[nm fl] mp] (.visitParameter mv nm fl)))
    (write-annotations #(.visitAnnotation mv %1 %2) (:annotations m))
    (write-type-annotations #(.visitTypeAnnotation mv %1 %2 %3 %4) (:type-annotations m))
    (when (some (comp seq force) (:param-annotations m))
      (let [pas (map force (:param-annotations m))]
        (.visitAnnotableParameterCount mv (count pas) true)
        (.visitAnnotableParameterCount mv (count pas) false)
        (doseq [[i anns] (map-indexed vector pas)]
          (write-annotations #(.visitParameterAnnotation mv i %1 %2) anns))))
    (when (:has-default m)
      (let [av (.visitAnnotationDefault mv)]
        (write-ann-value av nil (#'a/ann-value (:scope m) (:ret m) (:default m)))
        (.visitEnd av)))
    (cond
      ctor? (emit-ctor-code mv d m ab)
      (:derived m) (emit-derived-code mv d m)
      ab (let [gen (new-gen mv n (:ret m) (has? (:flags m) Opcodes/ACC_STATIC))]
           (when-let [r (:recv ab)] (swap! (:slots gen) assoc (:id r) 0))
           (bind-param-slots! gen (:params ab))
           (.visitCode mv)
           (emit gen (:body ab) :return)
           (finish-gen! gen))
      (has? (:flags m) (bit-or Opcodes/ACC_ABSTRACT Opcodes/ACC_NATIVE)) nil
      :else (do (.visitCode mv)
                (if (= "V" (:ret m)) (.visitInsn mv Opcodes/RETURN)
                    (fail (str "Method " (:name m) (:desc m) " of " n " needs a body")))))
    (.visitMaxs mv 0 0)
    (.visitEnd mv)))

(declare assertion-clinit emit-lambda-method emit-deserialize-lambda clj-constants-clinit)

(defmulti emit-clinit-node (fn [gen node] (:op node)))
(defmethod emit-clinit-node :default [gen node] (emit gen node :stmt))

(defn- emit-clinit [^ClassWriter cw d]
  (let [st @(:state d)
        nodes (:clinit st)
        extra (cond->> (:clinit-extra st)
                (seq (:clj-consts st)) (cons (clj-constants-clinit (:clj-consts st)))
                (:uses-assert st) (cons (assertion-clinit (:name d)))
                (:interface-assert st)
                (cons (fn [gen]
                        (.visitFieldInsn (mv gen) Opcodes/GETSTATIC
                                         (get @(:assert-holders a/*unit*) (:interface-assert st))
                                         "$assertionsDisabled" "Z")
                        (insn gen Opcodes/POP))))]
    (when (or (seq nodes) (seq extra))
      (let [mv (.visitMethod cw Opcodes/ACC_STATIC "<clinit>" "()V" nil nil)
            gen (new-gen mv (:name d) "V" true)]
        (.visitCode mv)
        (doseq [f extra] (f gen))
        (doseq [nd nodes] (emit-clinit-node gen nd))
        (.visitInsn mv Opcodes/RETURN)
        (finish-gen! gen)
        (.visitMaxs mv 0 0)
        (.visitEnd mv)))))

(defn- inner-class-entries
  "InnerClasses entries for the nested classes class bytes refer to: [name outer simple flags]."
  [^bytes bytes self]
  (let [cr (ClassReader. bytes)
        buf (char-array (.getMaxStringLength cr))
        names (java.util.LinkedHashSet.)
        add-desc (fn [^String d]
                   ;; class names in descriptors and signatures (Outer<T>.Inner in signatures too)
                   (doseq [[_ c] (re-seq #"L([^;<>.]+)[;<.]" d)] (.add names c)))]
    (.add names self)
    ;; descriptors and signatures of the class's own members (javac enters their classes)
    (.accept cr (proxy [ClassVisitor] [Opcodes/ASM9]
                  (visit [v a n sig sup ifs] (when sig (add-desc sig)))
                  (visitField [a n d sig v] (add-desc d) (when sig (add-desc sig)) nil)
                  (visitMethod [a n d sig ex] (add-desc d) (when sig (add-desc sig)) nil)
                  (visitRecordComponent [n d sig] (add-desc d) (when sig (add-desc sig)) nil))
             (bit-or ClassReader/SKIP_CODE ClassReader/SKIP_DEBUG ClassReader/SKIP_FRAMES))
    (doseq [i (range 1 (.getItemCount cr))]
      (let [off (.getItem cr i)]
        (when (pos? off)
          (case (int (.readByte cr (dec off)))
            7 (let [c (.readUTF8 cr off buf)]
                (.add names (str/replace c #"^\[+L?|;$" "")))
            12 (add-desc (.readUTF8 cr (+ off 2) buf))
            16 (add-desc (.readUTF8 cr off buf))
            nil))))
    (let [entries (atom (array-map))
          add (fn add [c]
                (when-not (contains? @entries c)
                  (when-let [d (a/decl c)]
                    (when (not= :top (:nesting d))
                      (when (= :member (:nesting d)) (add (:outer d)))
                      (swap! entries assoc c [c (when (= :member (:nesting d)) (:outer d))
                                              (when-not (= :anon (:nesting d)) (:simple d))
                                              (bit-and (:inner-flags d) (bit-not Opcodes/ACC_SUPER))])))
                  (when-not (a/decl c)
                    (when-let [cls (env/load-class c)]
                      (when-let [o (.getDeclaringClass ^Class cls)]
                        (add (str/replace (.getName o) "." "/"))
                        (swap! entries assoc c [c (str/replace (.getName o) "." "/") (.getSimpleName ^Class cls)
                                                (bit-and (.getModifiers ^Class cls) 0x761f)]))))))]
      (doseq [c names] (add c))
      ;; member classes of the class itself
      (doseq [[_ c] (:member-classes (a/decl self))] (add c))
      (vals @entries))))

(defn- add-inner-classes [^bytes bytes self]
  (let [entries (inner-class-entries bytes self)
        cr (ClassReader. bytes)
        cw (ClassWriter. cr 0)
        cv (proxy [ClassVisitor] [Opcodes/ASM9 cw]
             (visitEnd []
               (doseq [[n o s f] entries] (.visitInnerClass cw n o s f))
               (.visitEnd cw)))]
    (.accept cr cv 0)
    (.toByteArray cw)))

(defn emit-class
  "The class file bytes of class n of the compilation."
  [n]
  (let [d (a/decl! n)
        st @(:state d)
        cw ^ClassWriter (class-writer)]
    (.visit cw *version* (bit-or (:flags d) (if (has? (:meta-flags d) 0) 0 0)
                                 (if (or (:deprecated (:meta d))
                                         (some #(and (symbol? %) (#{"Deprecated" "java.lang.Deprecated"} (str %)))
                                               (keys (:meta d))))
                                   Opcodes/ACC_DEPRECATED 0))
            n (:signature d) (:super d) (into-array String (:interfaces d)))
    (if (= :top (:nesting d))
      (doseq [c @(:order a/*unit*) :when (and (not= c n) (= n (:nest-host (a/decl c))))]
        (.visitNestMember cw c))
      (.visitNestHost cw (:nest-host d)))
    (when (#{:local :anon} (:nesting d))
      (let [em (:enclosing-method d)
            m (:method em)]
        (.visitOuterClass cw (:outer d) (when m (:name m))
                          (when m (if (= "<init>" (:name m)) (ctor-real-desc (:outer d) m) (:desc m))))))
    (write-annotations #(.visitAnnotation cw %1 %2) (:annotations d))
    (write-type-annotations #(.visitTypeAnnotation cw %1 %2 %3 %4) (:type-annotations d))
    (doseq [p (:permits-final d)] (.visitPermittedSubclass cw p))
    (doseq [c (:components d)]
      (let [rv (.visitRecordComponent cw (:name c) (:desc c) (:sig c))]
        (write-annotations #(.visitAnnotation rv %1 %2) (a/for-target (:annotations c) "RECORD_COMPONENT"))
        (.visitEnd rv)))
    (doseq [f (:fields d)]
      (let [c (when (:const f) (env/const-value f))
            fv (.visitField cw (:flags f) (:name f) (:desc f) (:sig f)
                            (when (some? c) (const-attr-value (:desc f) c)))]
        (write-annotations #(.visitAnnotation fv %1 %2) (:annotations f))
        (write-type-annotations #(.visitTypeAnnotation fv %1 %2 %3 %4) (:type-annotations f))
        (.visitEnd fv)))
    (when (this0-field? d)
      (.visitEnd (.visitField cw (bit-or Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC) "this$0"
                              (t/internal->desc (:outer d)) nil nil)))
    (when (#{:local :anon} (:nesting d))
      (doseq [b (:captures st)]
        (.visitEnd (.visitField cw (bit-or Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC) (str "val$" (:sym b))
                                (:type b) nil nil))))
    (when (:uses-assert st)
      (.visitEnd (.visitField cw (bit-or Opcodes/ACC_STATIC Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC)
                              "$assertionsDisabled" "Z" nil nil)))
    (doseq [[_ f] (:clj-consts st)]
      (.visitEnd (.visitField cw (bit-or (if (env/interface? n) Opcodes/ACC_PUBLIC Opcodes/ACC_PRIVATE)
                                         Opcodes/ACC_STATIC Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC)
                              (:name f) (:desc f) nil nil)))
    (doseq [f (:extra-fields st)]
      (.visitEnd (.visitField cw (:flags f) (:name f) (:desc f) nil nil)))
    (doseq [m (:methods d)] (emit-method cw d m))
    (doseq [f (:extra-methods st)] (f cw))
    (doseq [l (:lambda-nodes st)] (emit-lambda-method cw l))
    (when-let [sn (seq (:serial-nodes st))] (emit-deserialize-lambda cw n sn))
    (emit-clinit cw d)
    (.visitEnd cw)
    (add-inner-classes (.toByteArray cw) n)))

;; ---------------------------------------------------------------------------------------------
;; java-str, java-assert, for-each

(def string-concat-bsm
  (Handle. Opcodes/H_INVOKESTATIC "java/lang/invoke/StringConcatFactory" "makeConcatWithConstants"
           (str "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                "Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;")
           false))

(defmethod emit-extra :java-str [gen node ctx]
  (let [parts (:parts node)
        dyn (filter :node parts)]
    ;; operands: a valueOf call converts eagerly, as javac
    (let [last-unsafe (last (keep-indexed (fn [i p] (when (unsafe? gen (:node p)) i)) dyn))
          emit-part (fn [p]
                      (emit gen (:node p) :expr)
                      (when (:eager p)
                        (.visitMethodInsn (mv gen) Opcodes/INVOKESTATIC "java/lang/String" "valueOf"
                                          "(Ljava/lang/Object;)Ljava/lang/String;" false)))]
      (if (nil? last-unsafe)
        (doseq [p dyn] (emit-part p))
        (let [temps (doall (for [p (take (inc last-unsafe) dyn)]
                             (let [ty (:type p) s (alloc-slot gen ty)]
                               (emit-part p) (xstore gen ty s) [ty s])))]
          (doseq [[ty s] temps] (xload gen ty s))
          (doseq [p (drop (inc last-unsafe) dyn)] (emit-part p)))))
    (let [recipe (apply str (map (fn [p] (cond (:node p) "\u0001" (:tag-const p) "\u0002" :else (:literal p))) parts))
          consts (keep :tag-const parts)]
      (.visitInvokeDynamicInsn (mv gen) "makeConcatWithConstants"
                               (t/method-desc (map :type dyn) t/string-desc)
                               string-concat-bsm (object-array (cons recipe consts))))
    (when (= ctx :stmt) (insn gen Opcodes/POP))))

(defn- outermost [n] (loop [n n] (if-let [o (:outer (a/decl n))] (recur o) n)))

(defmethod emit-extra :assert [gen node ctx]
  (let [m (mv gen) end (Label.)
        owner (if-let [top (:holder-top node)] (get @(:assert-holders a/*unit*) top) (:class node))]
    (.visitFieldInsn m Opcodes/GETSTATIC owner "$assertionsDisabled" "Z")
    (.visitJumpInsn m Opcodes/IFNE end)
    (emit-cond gen (:test node) true end)
    (.visitTypeInsn m Opcodes/NEW "java/lang/AssertionError")
    (insn gen Opcodes/DUP)
    (if-let [mn (:msg node)]
      (do (emit-to gen mn (:msg-desc node))
          (.visitMethodInsn m Opcodes/INVOKESPECIAL "java/lang/AssertionError" "<init>"
                            (str "(" (:msg-desc node) ")V") false))
      (.visitMethodInsn m Opcodes/INVOKESPECIAL "java/lang/AssertionError" "<init>" "()V" false))
    (insn gen Opcodes/ATHROW)
    (.visitLabel m end)
    (when (= ctx :expr) (insn gen Opcodes/ACONST_NULL))))

(defn assertion-clinit
  "The <clinit> prefix of a class with java-assert: $assertionsDisabled from the outermost class."
  [n]
  (fn [gen]
    (let [m (mv gen) l1 (Label.) l2 (Label.)]
      (.visitLdcInsn m (Type/getObjectType (outermost n)))
      (.visitMethodInsn m Opcodes/INVOKEVIRTUAL "java/lang/Class" "desiredAssertionStatus" "()Z" false)
      (.visitJumpInsn m Opcodes/IFNE l1)
      (insn gen Opcodes/ICONST_1)
      (.visitJumpInsn m Opcodes/GOTO l2)
      (.visitLabel m l1)
      (insn gen Opcodes/ICONST_0)
      (.visitLabel m l2)
      (.visitFieldInsn m Opcodes/PUTSTATIC n "$assertionsDisabled" "Z"))))

(defn- bind-hidden! [gen b]
  (let [s (alloc-slot gen (:type b))] (swap! (:slots gen) assoc (:id b) s) s))

(defmethod emit-extra :for-each [gen node ctx]
  (let [m (mv gen)
        saved @(:next gen)
        test (Label.) upd (Label.) end (Label.) exit (Label.)
        h (:hidden node)
        tg {:start upd :end end :depth (count (:cleanups gen)) :ctx (if (= ctx :expr) :expr :stmt)
            :type :null :slots [] :types []}
        gen2 (update gen :targets assoc (:id (:target node)) tg)
        b (:b node)]
    (if (:array node)
      (let [arr (bind-hidden! gen (:arr h)) len (bind-hidden! gen (:len h)) i (bind-hidden! gen (:i h))]
        (emit gen (:coll node) :expr)
        (.visitVarInsn m Opcodes/ASTORE arr)
        (.visitVarInsn m Opcodes/ALOAD arr)
        (insn gen Opcodes/ARRAYLENGTH)
        (.visitVarInsn m Opcodes/ISTORE len)
        (insn gen Opcodes/ICONST_0)
        (.visitVarInsn m Opcodes/ISTORE i)
        (.visitLabel m test)
        (.visitVarInsn m Opcodes/ILOAD i)
        (.visitVarInsn m Opcodes/ILOAD len)
        (.visitJumpInsn m Opcodes/IF_ICMPGE exit)
        (emit gen (:elem node) :expr)
        (xstore gen (:type b) (bind-hidden! gen b))
        (emit gen2 (:body node) :stmt)
        (.visitLabel m upd)
        (.visitIincInsn m i 1)
        (.visitJumpInsn m Opcodes/GOTO test))
      (let [it (bind-hidden! gen (:it h))]
        (emit gen (:iterator node) :expr)
        (.visitVarInsn m Opcodes/ASTORE it)
        (.visitLabel m upd)
        (.visitVarInsn m Opcodes/ALOAD it)
        (.visitMethodInsn m Opcodes/INVOKEINTERFACE "java/util/Iterator" "hasNext" "()Z" true)
        (.visitJumpInsn m Opcodes/IFEQ exit)
        (emit gen (:elem node) :expr)
        (xstore gen (:type b) (bind-hidden! gen b))
        (emit gen2 (:body node) :stmt)
        (.visitJumpInsn m Opcodes/GOTO upd)))
    ;; the loop's normal end gives nil; a break arrives at end with its value
    (.visitLabel m exit)
    (when (= ctx :expr) (insn gen Opcodes/ACONST_NULL))
    (.visitLabel m end)
    (reset! (:next gen) saved)))

;; ---------------------------------------------------------------------------------------------
;; lambdas and method references

(def metafactory
  (Handle. Opcodes/H_INVOKESTATIC "java/lang/invoke/LambdaMetafactory" "metafactory"
           (str "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                "Ljava/lang/invoke/CallSite;")
           false))

(def alt-metafactory
  (Handle. Opcodes/H_INVOKESTATIC "java/lang/invoke/LambdaMetafactory" "altMetafactory"
           (str "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;")
           false))

(defn- lambda-instance? [node] @(:uses-this (:boundary node)))

(defn- lambda-impl-desc [node]
  (let [[ps r] (t/parse-method-desc (:inst-desc node))]
    (t/method-desc (concat (map :type @(:captures (:boundary node))) ps) r)))

(defn- emit-indy-lambda [gen node captured-types impl]
  (let [fi (:fi node)
        sam (:sam node)
        markers (:markers node)
        alt? (or (seq markers) (:serializable node))
        ;; javac: altMetafactory for intersection targets and serializable lambdas, with
        ;; FLAG_SERIALIZABLE, FLAG_MARKERS and FLAG_BRIDGES (no bridges)
        args (if alt?
               (object-array (concat [(Type/getMethodType ^String (:desc sam)) impl
                                      (Type/getMethodType ^String (:inst-desc node))
                                      (Integer/valueOf (bit-or (if (:serializable node) 1 0) (if (seq markers) 2 0) 4))]
                                     (when (seq markers)
                                       (cons (Integer/valueOf (count markers)) (map #(Type/getObjectType %) markers)))
                                     [(Integer/valueOf 0)]))
               (object-array [(Type/getMethodType ^String (:desc sam)) impl
                              (Type/getMethodType ^String (:inst-desc node))]))]
    (.visitInvokeDynamicInsn (mv gen) (:name sam) (t/method-desc captured-types (t/internal->desc fi))
                             (if alt? alt-metafactory metafactory) args)))

(defn- intersection-casts [gen node]
  ;; the intersection cast of the target type
  (doseq [c (:intersection node)] (.visitTypeInsn (mv gen) Opcodes/CHECKCAST c)))

(defn lambda-indy-spec
  "[captured-types impl-handle impl-kind] of a lambda or method reference node."
  [node]
  (case (:op node)
    :lambda (let [cls (:class node)
                  inst? (lambda-instance? node)
                  itf (env/interface? cls)
                  kind (if inst? (if itf Opcodes/H_INVOKEINTERFACE Opcodes/H_INVOKEVIRTUAL) Opcodes/H_INVOKESTATIC)]
              [(concat (when inst? [(t/internal->desc cls)]) (map :type @(:captures (:boundary node))))
               (Handle. kind cls (:name node) (lambda-impl-desc node) itf)
               kind])
    :method-ref (let [{:keys [kind owner name desc itf]} node
                      hk (case kind
                           :static Opcodes/H_INVOKESTATIC
                           :new Opcodes/H_NEWINVOKESPECIAL
                           (:bound :unbound) (if itf Opcodes/H_INVOKEINTERFACE Opcodes/H_INVOKEVIRTUAL))]
                  [(when (= kind :bound) [(a/value-type (:type (:recv node)))])
                   (Handle. hk owner (if (= kind :new) "<init>" name) desc (boolean (and itf (not= kind :new))))
                   hk])))

(defmethod emit-extra :lambda [gen node ctx]
  (let [inst? (lambda-instance? node)
        caps @(:captures (:boundary node))
        [ctypes h] (lambda-indy-spec node)]
    (when inst? (.visitVarInsn (mv gen) Opcodes/ALOAD 0))
    (doseq [b caps] (load-binding gen b))
    (emit-indy-lambda gen node ctypes h)
    (intersection-casts gen node)
    (when (= ctx :stmt) (insn gen Opcodes/POP))))

(defn emit-lambda-method
  "The synthetic method of a lambda."
  [^ClassWriter cw node]
  (let [cls (:class node)
        inst? (lambda-instance? node)
        ;; javac gives the lambda method the throws clause of the functional interface's method
        exc (seq (:throws (:sam node)))
        mv (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_SYNTHETIC (if inst? 0 Opcodes/ACC_STATIC))
                         (:name node) (lambda-impl-desc node) nil (when exc (into-array String exc)))
        gen (assoc (new-gen mv cls (:ret node) (not inst?)) :lambda? true)]
    (doseq [b @(:captures (:boundary node))]
      (swap! (:slots gen) assoc (:id b) (alloc-slot gen (:type b))))
    (doseq [b (:params node)]
      (swap! (:slots gen) assoc (:id b) (alloc-slot gen (:type b))))
    (.visitCode mv)
    (emit gen (:body node) :return)
    (finish-gen! gen)
    (.visitMaxs mv 0 0)
    (.visitEnd mv)))

(defmethod emit-extra :method-ref [gen node ctx]
  (let [m (mv gen)
        kind (:kind node)
        [ctypes h] (lambda-indy-spec node)]
    (when (= kind :bound)
      (emit gen (:recv node) :expr)
      (when (:null-check node)
        (insn gen Opcodes/DUP)
        (.visitMethodInsn m Opcodes/INVOKESTATIC "java/util/Objects" "requireNonNull"
                          "(Ljava/lang/Object;)Ljava/lang/Object;" false)
        (insn gen Opcodes/POP)))
    (emit-indy-lambda gen node ctypes h)
    (intersection-casts gen node)
    (when (= ctx :stmt) (insn gen Opcodes/POP))))

;; $deserializeLambda$ (javac's LambdaToMethod.makeDeserializeMethod)

(def ^:private serialized-lambda "java/lang/invoke/SerializedLambda")

(defmethod emit-extra :deser-indy [gen node ctx]
  (let [ln (:lambda node)
        [ctypes h] (lambda-indy-spec ln)]
    (doseq [[i ct] (map-indexed vector ctypes)]
      (.visitVarInsn (mv gen) Opcodes/ALOAD 0)
      (emit-const gen "I" i)
      (.visitMethodInsn (mv gen) Opcodes/INVOKEVIRTUAL serialized-lambda "getCapturedArg" "(I)Ljava/lang/Object;" false)
      (if (t/prim? ct)
        (do (.visitTypeInsn (mv gen) Opcodes/CHECKCAST (t/box-of ct))
            (unbox gen (t/internal->desc (t/box-of ct))))
        (.visitTypeInsn (mv gen) Opcodes/CHECKCAST (t/desc->internal ct))))
    (emit-indy-lambda gen ln ctypes h)
    (when (= ctx :stmt) (insn gen Opcodes/POP))))

(defn- deser-getter [name ret & args]
  {:op :invoke :kind :virtual :owner serialized-lambda :itf false :name name
   :desc (t/method-desc (map :type args) ret)
   :target {:op :param0 :type (t/internal->desc serialized-lambda)} :args (vec args) :type ret})

(defmethod emit-extra :param0 [gen node ctx]
  (.visitVarInsn (mv gen) Opcodes/ALOAD 0))

(defn- obj-equals [x s]
  {:op :invoke :kind :virtual :owner "java/lang/Object" :itf false :name "equals"
   :desc "(Ljava/lang/Object;)Z" :target x :args [(a/const-node t/string-desc s)] :type "Z"})

(defn emit-deserialize-lambda
  "The private static $deserializeLambda$ method of a class with serializable lambdas."
  [^ClassWriter cw cls nodes]
  (let [cases (reduce (fn [m ln]
                        (let [[_ ^Handle h kind] (lambda-indy-spec ln)
                              test {:op :and :type "Z"
                                    :args [{:op :compare :cmp :== :t "I" :type "Z"
                                            :args [(deser-getter "getImplMethodKind" "I") (a/const-node "I" (int kind))]}
                                           (obj-equals (deser-getter "getFunctionalInterfaceClass" t/string-desc) (:fi ln))
                                           (obj-equals (deser-getter "getFunctionalInterfaceMethodName" t/string-desc) (:name (:sam ln)))
                                           (obj-equals (deser-getter "getFunctionalInterfaceMethodSignature" t/string-desc) (:desc (:sam ln)))
                                           (obj-equals (deser-getter "getImplClass" t/string-desc) (.getOwner h))
                                           (obj-equals (deser-getter "getImplMethodSignature" t/string-desc) (.getDesc h))]}
                              stmt {:op :if :test test :type :null
                                    :then {:op :return :val {:op :deser-indy :lambda ln :type (t/internal->desc (:fi ln))} :type :none}
                                    :else (a/const-node :null nil)}]
                          (update m (.getName h) (fnil conj []) stmt)))
                      (array-map) nodes)
        sw {:op :switch :kind :string :sel (deser-getter "getImplMethodName" t/string-desc) :sel-type t/string-desc
            :cases (vec (for [[nm stmts] cases]
                          {:labels [{:const nm}] :body {:op :do :statements stmts :ret (a/const-node :null nil) :type :null}}))
            :default nil :type :null}
        body {:op :do :statements [sw]
              :ret {:op :throw :type :none
                    :expr {:op :new :class "java/lang/IllegalArgumentException"
                           :ctor {:name "<init>" :desc "(Ljava/lang/String;)V" :owner "java/lang/IllegalArgumentException"}
                           :args [(a/const-node t/string-desc "Invalid lambda deserialization")]
                           :type "Ljava/lang/IllegalArgumentException;"}}
              :type :none}
        mv (.visitMethod cw (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC)
                         "$deserializeLambda$" "(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;" nil nil)
        gen (new-gen mv cls t/object-desc true)]
    (swap! (:next gen) inc)
    (.visitCode mv)
    (emit gen body :return)
    (.visitMaxs mv 0 0)
    (.visitEnd mv)))

;; ---------------------------------------------------------------------------------------------
;; switch and patterns

(defn- emit-switch-insn
  "tableswitch or lookupswitch, chosen as javac chooses."
  [gen keyed dflt]
  (let [ks (sort (keys keyed))
        n (count ks)]
    (if (zero? n)
      (do (insn gen Opcodes/POP) (.visitJumpInsn (mv gen) Opcodes/GOTO dflt))
      (let [lo (long (first ks)) hi (long (last ks))
            table-space (+ 4 (- hi lo -1)) table-time 3
            lookup-space (+ 3 (* 2 n)) lookup-time n]
        (if (<= (+ table-space (* 3 table-time)) (+ lookup-space (* 3 lookup-time)))
          (.visitTableSwitchInsn (mv gen) (int lo) (int hi) dflt
                                 (into-array Label (for [k (range lo (inc hi))] (get keyed (int k) dflt))))
          (.visitLookupSwitchInsn (mv gen) dflt (int-array ks) (into-array Label (map keyed ks))))))))

(def ^:private match-exception-ctor "(Ljava/lang/String;Ljava/lang/Throwable;)V")

(defn- match-handler
  "The method's handler wrapping exceptions of record accessors in MatchException."
  [gen]
  (or (get @(:finish gen) :match-label)
      (let [h (Label.) m (mv gen)]
        (swap! (:finish gen) assoc :match-label h
               :match (fn []
                        (let [s (alloc-slot gen t/object-desc)]
                          (.visitLabel m h)
                          (.visitVarInsn m Opcodes/ASTORE s)
                          (.visitTypeInsn m Opcodes/NEW "java/lang/MatchException")
                          (insn gen Opcodes/DUP)
                          (.visitVarInsn m Opcodes/ALOAD s)
                          (.visitMethodInsn m Opcodes/INVOKEVIRTUAL "java/lang/Throwable" "toString" "()Ljava/lang/String;" false)
                          (.visitVarInsn m Opcodes/ALOAD s)
                          (.visitMethodInsn m Opcodes/INVOKESPECIAL "java/lang/MatchException" "<init>" match-exception-ctor false)
                          (insn gen Opcodes/ATHROW))))
        h)))

(defn- emit-pattern
  "Matches the value in `slot` (of type vt) against pattern p, binding its variables; jumps to
  fail when it does not match. tested: the type test was already done (typeSwitch)."
  [gen p vt slot fail tested]
  (let [m (mv gen)
        cd (:class p)]
    (when (and (:test p) (not tested))
      (.visitVarInsn m Opcodes/ALOAD slot)
      (.visitTypeInsn m Opcodes/INSTANCEOF (t/desc->internal cd))
      (.visitJumpInsn m Opcodes/IFEQ fail))
    (case (:kind p)
      :type (when-let [b (:b p)]
              (xload gen vt slot)
              (when (and (t/ref? cd) (not= cd vt) (not (env/assignable? vt cd)))
                (.visitTypeInsn m Opcodes/CHECKCAST (t/desc->internal cd)))
              (let [s (alloc-slot gen (:type b))]
                (swap! (:slots gen) assoc (:id b) s)
                (xstore gen (:type b) s)))
      :record (let [rs (alloc-slot gen cd)]
                (.visitVarInsn m Opcodes/ALOAD slot)
                (when-not (and (t/ref? vt) (env/assignable? vt cd))
                  (.visitTypeInsn m Opcodes/CHECKCAST (t/desc->internal cd)))
                (.visitVarInsn m Opcodes/ASTORE rs)
                (doseq [{:keys [accessor pattern]} (:comps p)]
                  (let [ct (subs (:desc accessor) 2)
                        cs (alloc-slot gen ct)
                        start (Label.) end (Label.)]
                    (.visitVarInsn m Opcodes/ALOAD rs)
                    (.visitLabel m start)
                    (.visitMethodInsn m Opcodes/INVOKEVIRTUAL (:owner accessor) (:name accessor) (:desc accessor) false)
                    (.visitLabel m end)
                    (.visitTryCatchBlock m start end (match-handler gen) "java/lang/Throwable")
                    (xstore gen ct cs)
                    (emit-pattern gen pattern ct cs fail false)))))))

(defmethod emit-extra :if-instance [gen node ctx]
  (let [m (mv gen)
        t (a/value-type (:type node))
        els (Label.) end (Label.)
        s (alloc-slot gen (a/value-type (:type (:expr node))))
        vt (a/value-type (:type (:expr node)))]
    (emit gen (:expr node) :expr)
    (.visitVarInsn m Opcodes/ASTORE s)
    (emit-pattern gen (:pattern node) vt s els false)
    (if (= ctx :expr) (emit-to gen (:then node) t) (emit gen (:then node) ctx))
    (when-not (= :none (:type (:then node))) (.visitJumpInsn m Opcodes/GOTO end))
    (.visitLabel m els)
    (if (= ctx :expr) (emit-to gen (:else node) t) (emit gen (:else node) ctx))
    (.visitLabel m end)))

(def ^:private switch-bsm-desc
  (str "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
       "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))

(defn- enum-ordinal [e c]
  (if-let [d (a/decl e)]
    (first (keep-indexed (fn [i k] (when (= c (:name k)) i)) (:constants d)))
    (.ordinal ^Enum (Enum/valueOf (env/load-class e) c))))

(defn switch-map-field [e] (str "$SwitchMap$" (str/replace e #"[/.]" "\\$")))

(defmethod emit-extra :switch [gen node ctx]
  (let [m (mv gen)
        t (a/value-type (:type node))
        expr? (= ctx :expr)
        end (Label.) dflt (Label.)
        cases (:cases node)
        arm-labels (vec (repeatedly (count cases) #(Label.)))
        emit-arm (fn [body]
                   (if expr? (emit-to gen body t) (emit gen body :stmt))
                   (when-not (= :none (:type body)) (.visitJumpInsn m Opcodes/GOTO end)))
        indexed (for [[ci c] (map-indexed vector cases) l (:labels c)] [ci l])
        emit-arms-and-default
        (fn [arm-code]
          (doseq [[ci c] (map-indexed vector cases)]
            (.visitLabel m (nth arm-labels ci))
            (arm-code ci c)
            (emit-arm (:body c)))
          (.visitLabel m dflt)
          (if-let [ci (some (fn [[ci l]] (when (:default l) ci)) indexed)]
            ;; (nil :default): that arm is also the default
            (.visitJumpInsn m Opcodes/GOTO (nth arm-labels ci))
            (if-let [d (:default node)]
              (emit-arm d)
              (when expr? (insn gen Opcodes/ACONST_NULL))))
          (.visitLabel m end))]
    (case (:kind node)
      :int
      (do (emit-to gen (:sel node) "I")
          (emit-switch-insn gen (into {} (for [[ci l] indexed]
                                           [(let [v (:const l)] (int (if (char? v) (int v) v))) (nth arm-labels ci)]))
                            dflt)
          (emit-arms-and-default (fn [_ _])))

      :string
      (let [ss (alloc-slot gen t/string-desc) ts (alloc-slot gen "I")
            second-switch (Label.)
            strs (vec (for [[ci l] indexed] [(:const l) ci]))
            buckets (group-by #(.hashCode ^String (first %)) (map-indexed (fn [i [sv ci]] [sv i ci]) strs))
            bucket-labels (into {} (for [h (keys buckets)] [(int h) (Label.)]))]
        (emit gen (:sel node) :expr)
        (.visitVarInsn m Opcodes/ASTORE ss)
        (insn gen Opcodes/ICONST_M1)
        (.visitVarInsn m Opcodes/ISTORE ts)
        (.visitVarInsn m Opcodes/ALOAD ss)
        (.visitMethodInsn m Opcodes/INVOKEVIRTUAL "java/lang/String" "hashCode" "()I" false)
        (emit-switch-insn gen bucket-labels second-switch)
        (doseq [[h entries] buckets]
          (.visitLabel m (bucket-labels (int h)))
          (doseq [[sv i _] (reverse entries)]
            (let [nxt (Label.)]
              (.visitVarInsn m Opcodes/ALOAD ss)
              (.visitLdcInsn m sv)
              (.visitMethodInsn m Opcodes/INVOKEVIRTUAL "java/lang/String" "equals" "(Ljava/lang/Object;)Z" false)
              (.visitJumpInsn m Opcodes/IFEQ nxt)
              (emit-const gen "I" i)
              (.visitVarInsn m Opcodes/ISTORE ts)
              (.visitJumpInsn m Opcodes/GOTO second-switch)
              (.visitLabel m nxt)))
          (.visitJumpInsn m Opcodes/GOTO second-switch))
        (.visitLabel m second-switch)
        (.visitVarInsn m Opcodes/ILOAD ts)
        (emit-switch-insn gen (into {} (map-indexed (fn [i [_ ci]] [(int i) (nth arm-labels ci)]) strs)) dflt)
        (emit-arms-and-default (fn [_ _])))

      :enum
      (let [e (t/desc->internal (:sel-type node))]
        (if (:enum-direct node)
          (do (emit gen (:sel node) :expr)
              (.visitMethodInsn m Opcodes/INVOKEVIRTUAL e "ordinal" "()I" false)
              (emit-switch-insn gen (into {} (for [[ci l] indexed] [(int (enum-ordinal e (:enum l))) (nth arm-labels ci)])) dflt))
          (let [holder (get @(:switch-holders a/*unit*) (:top node))
                values (get-in @(:switch-maps a/*unit*) [(:top node) e :values])]
            (.visitFieldInsn m Opcodes/GETSTATIC holder (switch-map-field e) "[I")
            (emit gen (:sel node) :expr)
            (.visitMethodInsn m Opcodes/INVOKEVIRTUAL e "ordinal" "()I" false)
            (insn gen Opcodes/IALOAD)
            (emit-switch-insn gen (into {} (for [[ci l] indexed] [(int (get values (:enum l))) (nth arm-labels ci)])) dflt)))
        (emit-arms-and-default (fn [_ _])))

      :pattern
      (let [st (:sel-type node)
            enum? (and (t/class-desc? st) (has? (:flags (env/info (t/desc->internal st))) Opcodes/ACC_ENUM))
            labels (vec (for [[ci l] indexed :when (not (:null l)) :when (not (:default l))] [ci l]))
            null-arm (some (fn [[ci l]] (when (:null l) ci)) indexed)
            default-too (some (fn [[ci l]] (when (:default l) ci)) indexed)
            ss (alloc-slot gen st) rs (alloc-slot gen "I")
            loop-l (Label.)
            case-labels (vec (repeatedly (count labels) #(Label.)))
            bsm-args (for [[_ l] labels]
                       (cond (:pattern l) (Type/getType ^String (:class (:pattern l)))
                             (:enum l) (:enum l)
                             :else (let [v (:const l)] (if (char? v) (Integer/valueOf (int v)) (if (integer? v) (Integer/valueOf (int v)) v)))))]
        (emit gen (:sel node) :expr)
        (when-not null-arm
          (insn gen Opcodes/DUP)
          (.visitMethodInsn m Opcodes/INVOKESTATIC "java/util/Objects" "requireNonNull"
                            "(Ljava/lang/Object;)Ljava/lang/Object;" false)
          (insn gen Opcodes/POP))
        (.visitVarInsn m Opcodes/ASTORE ss)
        (insn gen Opcodes/ICONST_0)
        (.visitVarInsn m Opcodes/ISTORE rs)
        (.visitLabel m loop-l)
        (.visitVarInsn m Opcodes/ALOAD ss)
        (.visitVarInsn m Opcodes/ILOAD rs)
        (.visitInvokeDynamicInsn m (if enum? "enumSwitch" "typeSwitch") (str "(" st "I)I")
                                 (Handle. Opcodes/H_INVOKESTATIC "java/lang/runtime/SwitchBootstraps"
                                          (if enum? "enumSwitch" "typeSwitch") switch-bsm-desc false)
                                 (object-array bsm-args))
        (emit-switch-insn gen (merge (into {} (map-indexed (fn [i _] [(int i) (nth case-labels i)]) labels))
                                     (when null-arm {(int -1) (nth arm-labels null-arm)}))
                          dflt)
        ;; per label: bind, test nested patterns and the guard, else restart after this label
        (doseq [[i [ci l]] (map-indexed vector labels)]
          (.visitLabel m (nth case-labels i))
          (let [c (nth cases ci)
                restart (Label.)]
            (when-let [p (:pattern l)]
              (emit-pattern gen p st ss restart true))
            (when-let [g (:guard c)] (emit-cond gen g false restart))
            (.visitJumpInsn m Opcodes/GOTO (nth arm-labels ci))
            (when (or (:pattern l) (:guard c))
              (.visitLabel m restart)
              (emit-const gen "I" (inc i))
              (.visitVarInsn m Opcodes/ISTORE rs)
              (.visitJumpInsn m Opcodes/GOTO loop-l))))
        (emit-arms-and-default (fn [_ _]))))))

(defn switch-holder-decl
  "javac's synthetic class holding the $SwitchMap$ arrays of the outermost class top."
  [top name enums]
  {:name name :kind :class :nesting :anon :outer top :nest-host top :simple nil
   :flags (bit-or Opcodes/ACC_SUPER Opcodes/ACC_SYNTHETIC)
   :inner-flags (bit-or Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC)
   :super "java/lang/Object" :interfaces [] :fields [] :methods [] :members [] :member-classes {}
   :switch-holder true
   :state (atom {:captures []
                 :extra-fields (for [[e _] enums]
                                 {:name (switch-map-field e) :desc "[I"
                                  :flags (bit-or Opcodes/ACC_STATIC Opcodes/ACC_FINAL Opcodes/ACC_SYNTHETIC)})
                 :clinit-extra
                 [(fn [gen]
                    (let [m (mv gen)]
                      (doseq [[e {:keys [order values]}] enums]
                        (.visitMethodInsn m Opcodes/INVOKESTATIC e "values" (str "()[L" e ";") false)
                        (insn gen Opcodes/ARRAYLENGTH)
                        (.visitIntInsn m Opcodes/NEWARRAY Opcodes/T_INT)
                        (.visitFieldInsn m Opcodes/PUTSTATIC name (switch-map-field e) "[I")
                        (doseq [c order]
                          (let [start (Label.) end (Label.) h (Label.) after (Label.)]
                            (.visitLabel m start)
                            (.visitFieldInsn m Opcodes/GETSTATIC name (switch-map-field e) "[I")
                            (.visitFieldInsn m Opcodes/GETSTATIC e c (str "L" e ";"))
                            (.visitMethodInsn m Opcodes/INVOKEVIRTUAL e "ordinal" "()I" false)
                            (emit-const gen "I" (get values c))
                            (insn gen Opcodes/IASTORE)
                            (.visitLabel m end)
                            (.visitJumpInsn m Opcodes/GOTO after)
                            (.visitLabel m h)
                            (insn gen Opcodes/POP)
                            (.visitLabel m after)
                            (.visitTryCatchBlock m start end h "java/lang/NoSuchFieldError"))))))]})})

(defn assert-holder-decl
  "javac's synthetic class holding $assertionsDisabled for asserts in interfaces."
  [top name]
  {:name name :kind :class :nesting :anon :outer top :nest-host top :simple nil
   :flags (bit-or Opcodes/ACC_SUPER Opcodes/ACC_SYNTHETIC)
   :inner-flags (bit-or Opcodes/ACC_STATIC Opcodes/ACC_SYNTHETIC)
   :super "java/lang/Object" :interfaces [] :fields [] :methods [] :members [] :member-classes {}
   :state (atom {:captures [] :uses-assert true})})

(defn clj-constants-clinit
  "<clinit> code initializing the Clojure constants of a class (vars, keywords, quoted data)."
  [consts]
  (fn [gen]
    (let [m (mv gen)]
      (doseq [[_ {:keys [owner name desc init]}] consts]
        (case (:kind init)
          :var (do (.visitLdcInsn m (:ns init))
                   (.visitLdcInsn m (:name init))
                   (.visitMethodInsn m Opcodes/INVOKESTATIC "clojure/lang/RT" "var"
                                     "(Ljava/lang/String;Ljava/lang/String;)Lclojure/lang/Var;" false))
          :read (do (.visitLdcInsn m (:text init))
                    (.visitMethodInsn m Opcodes/INVOKESTATIC "clojure/lang/RT" "readString"
                                      "(Ljava/lang/String;)Ljava/lang/Object;" false)
                    (when-not (= desc t/object-desc)
                      (.visitTypeInsn m Opcodes/CHECKCAST (t/desc->internal desc)))))
        (.visitFieldInsn m Opcodes/PUTSTATIC owner name desc)))))
