(ns arbace.c2g.access
  "javac's accessor methods (access$NNN, arbace.classes.analyze's accessorize and
  outer-super-call): the analyzer turns a private member access across a nest into a call of a
  synthetic static method whose code it keeps only as a bytecode-writing function. Go has no
  private members across the classes of one package, so c2g undoes the indirection: it writes
  each accessor's code with ASM, reads back the one member instruction it holds, and gives the
  access node it stands for."
  (:require [arbace.string :as str]
            [arbace.classes.types :as t]
            [arbace.classes.env :as env]
            [arbace.classes.analyze :as a])
  (:import (arbace.asm ClassWriter ClassReader ClassVisitor MethodVisitor Opcodes)))

(def ^:private cache (atom {}))

(defn- decode-class
  "{accessor-name {:insn kw :owner :name :desc}} of class o's accessors."
  [o]
  (let [st @(a/state o)
        fs (:extra-methods st)]
    (if (empty? fs)
      {}
      (let [cw (ClassWriter. 0)
            _ (.visit cw Opcodes/V1_8 Opcodes/ACC_PUBLIC o nil "java/lang/Object" nil)
            _ (doseq [f fs] (f cw))
            _ (.visitEnd cw)
            bytes (.toByteArray cw)
            out (atom {})]
        (.accept (ClassReader. bytes)
                 (proxy [ClassVisitor] [Opcodes/ASM9]
                   (visitMethod [acc mname desc sig exs]
                     (let [rec (fn [m] (swap! out update mname #(or % m)))]
                       (proxy [MethodVisitor] [Opcodes/ASM9]
                         (visitFieldInsn [op owner fname fdesc]
                           (rec {:insn (condp = op Opcodes/GETFIELD :getfield Opcodes/PUTFIELD :putfield
                                              Opcodes/GETSTATIC :getstatic Opcodes/PUTSTATIC :putstatic)
                                 :owner owner :name fname :desc fdesc}))
                         (visitMethodInsn [op owner mn mdesc itf]
                           (rec {:insn (condp = op Opcodes/INVOKEVIRTUAL :invokevirtual
                                              Opcodes/INVOKESTATIC :invokestatic
                                              Opcodes/INVOKESPECIAL :invokespecial
                                              Opcodes/INVOKEINTERFACE :invokeinterface)
                                 :owner owner :name mn :desc mdesc :itf itf}))))))
                 0)
        @out))))

(defn accessor
  "The member instruction of accessor method mname of class o, or nil."
  [o mname]
  (when (and (string? mname) (str/starts-with? mname "access$") (a/decl o))
    (let [m (or (get @cache o) (let [m (decode-class o)] (swap! cache assoc o m) m))]
      (get m mname))))

(defn reset-cache! [] (reset! cache {}))

(defn- find-field [owner fname]
  (or (env/find-field owner fname)
      (throw (ex-info (str "c2g: accessor field not found " owner "." fname) {}))))

(defn unaccess
  "The access node an accessor call node stands for, or the node itself."
  [node]
  (if-let [acc (and (= :invoke (:op node)) (= :static (:kind node)) (accessor (:owner node) (:name node)))]
    (let [args (:args node)]
      (case (:insn acc)
        :getfield (let [f (find-field (:owner acc) (:name acc))]
                    {:op :get-field :field f :owner (:owner f) :target (first args) :type (:desc f)})
        :putfield (let [f (find-field (:owner acc) (:name acc))]
                    {:op :set-field :field f :owner (:owner f) :target (first args) :val (second args)
                     :type (:desc f)})
        :getstatic (let [f (find-field (:owner acc) (:name acc))]
                     {:op :get-static :field f :owner (:owner f) :type (:desc f)})
        :putstatic (let [f (find-field (:owner acc) (:name acc))]
                     {:op :set-static :field f :owner (:owner f) :val (first args) :type (:desc f)})
        (:invokevirtual :invokeinterface :invokestatic :invokespecial)
        (let [static? (= :invokestatic (:insn acc))
              m (some #(when (and (= (:name acc) (:name %)) (= (:desc acc) (:desc %))) %)
                      (:methods (env/info (:owner acc))))
              m (assoc (or m {:name (:name acc) :desc (:desc acc) :flags 0}) :owner (:owner acc))
              [_ r] (t/parse-method-desc (:desc acc))]
          {:op :invoke
           :kind (case (:insn acc) :invokestatic :static :invokespecial :special
                       :invokeinterface :interface :virtual)
           :owner (:owner acc) :itf (boolean (:itf acc)) :name (:name acc) :desc (:desc acc)
           :target (when-not static? (first args)) :args (vec (if static? args (rest args)))
           :type r :method m
           ;; an outer class's super call (Outer/super): the receiver is the outer instance
           :outer-super (= :invokespecial (:insn acc))})))
    node))
