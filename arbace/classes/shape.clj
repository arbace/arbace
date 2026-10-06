(ns arbace.classes.shape
  "Class shapes (SPEC §3): what makes two class files equivalent, as data, read with ASM's
  ClassReader. Used by the tests to compare the compiler's classes with javac's."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.set])
  (:import (arbace.asm ClassReader ClassVisitor MethodVisitor FieldVisitor AnnotationVisitor
                       RecordComponentVisitor Opcodes Handle Type Label ConstantDynamic)))

(defn- ann-visitor [store]
  ;; store: atom of a map, values collected by element name
  (proxy [AnnotationVisitor] [Opcodes/ASM9]
    (visit [n v] (swap! store assoc n (if (.isArray (class v)) (vec (seq v)) (if (instance? Type v) (str v) v))))
    (visitEnum [n d v] (swap! store assoc n [:enum d v]))
    (visitAnnotation [n d] (let [s (atom {})] (swap! store assoc n [:ann d s]) (ann-visitor s)))
    (visitArray [n] (let [s (atom {})] (swap! store assoc n [:array s]) (ann-visitor s)))))

(defn- realize [v]
  (cond (instance? clojure.lang.Atom v) (into (sorted-map) (map (fn [[k x]] [(str k) (realize x)]) @v))
        (vector? v) (mapv realize v)
        :else v))

(defn- add-ann [store kind d visible]
  (let [s (atom {})]
    (swap! store update kind (fnil conj #{}) [d visible s])
    (ann-visitor s)))

(defn- add-type-ann
  "A type annotation with its type reference (sort and arguments, not code offsets) and path."
  [store kind ref path d visible]
  (add-ann store kind (str (Integer/toHexString (bit-and (int ref) (if (>= (bit-shift-right (int ref) 24) 0x40) (unchecked-int 0xFF0000FF) -1)))
                           ":" path ":" d)
           visible))

(defn- realize-anns [m]
  (into {} (for [[k v] m]
             [k (if (set? v) (set (map (fn [[d vis s]] [d vis (realize s)]) v)) v)])))

(defn- code-symbols
  "The symbolic content of code (SPEC §3.5): member references, class references, call sites,
  constants, as a set (order and instruction choice may differ)."
  [store]
  (proxy [MethodVisitor] [Opcodes/ASM9]
    (visitFieldInsn [op owner name desc] (swap! store conj [:field op owner name desc]))
    (visitMethodInsn
      ([op owner name desc itf] (swap! store conj [:method op owner name desc itf])))
    (visitTypeInsn [op t] (swap! store conj [:type op t]))
    (visitMultiANewArrayInsn [d n] (swap! store conj [:type :multianewarray d]))
    (visitLdcInsn [v] (swap! store conj [:const (cond (instance? Type v) (str "class " v)
                                                      (instance? Double v) [:double (str v)]
                                                      (instance? Float v) [:float (str v)]
                                                      :else v)]))
    (visitInvokeDynamicInsn [name desc ^Handle bsm bargs]
      (swap! store conj [:indy (if (re-matches #"lambda\$.*" name) name name) desc (str bsm)
                         (mapv str bargs)]))))

(defn shape
  "The shape of a class file: a map of everything §3 compares."
  [^bytes bytes & {:keys [code] :or {code true}}]
  (let [cr (ClassReader. bytes)
        c (atom {})
        fields (atom {})
        methods (atom {})
        cv (proxy [ClassVisitor] [Opcodes/ASM9]
             (visit [version access name sig super ifaces]
               (swap! c assoc :version version :flags access :name name :signature sig
                      :super super :interfaces (set ifaces)))
             (visitModule [n acc v]
               (let [mm (atom {:name n :flags acc :version v})]
                 (swap! c assoc :module mm)
                 (proxy [arbace.asm.ModuleVisitor] [Opcodes/ASM9]
                   (visitRequire [m fl ver] (swap! mm update :requires (fnil conj #{}) [m fl ver]))
                   (visitExport [p fl ms] (swap! mm update :exports (fnil conj #{}) [p fl (set ms)]))
                   (visitOpen [p fl ms] (swap! mm update :opens (fnil conj #{}) [p fl (set ms)]))
                   (visitUse [s] (swap! mm update :uses (fnil conj #{}) s))
                   (visitProvide [s ps] (swap! mm update :provides (fnil conj #{}) [s (vec ps)]))
                   (visitPackage [p] (swap! mm update :packages (fnil conj #{}) p))
                   (visitMainClass [m] (swap! mm assoc :main-class m)))))
             (visitNestHost [h] (swap! c assoc :nest-host h))
             (visitNestMember [m] (swap! c update :nest-members (fnil conj #{}) m))
             (visitPermittedSubclass [p] (swap! c update :permitted (fnil conj #{}) p))
             (visitOuterClass [o n d] (swap! c assoc :enclosing-method [o n d]))
             (visitInnerClass [n o s f] (swap! c update :inner-classes (fnil conj #{}) [n o s f]))
             (visitAnnotation [d vis] (add-ann c :annotations d vis))
             (visitTypeAnnotation [r p d vis] (add-type-ann c :type-annotations r p d vis))
             (visitRecordComponent [n d sig]
               (swap! c update :record (fnil conj []) [n d sig])
               nil)
             (visitAttribute [a] (swap! c update :attributes (fnil conj #{}) (.-type a)))
             (visitField [access name desc sig value]
               (let [f (atom {:flags access :signature sig :value value})]
                 (swap! fields assoc [name desc] f)
                 (proxy [FieldVisitor] [Opcodes/ASM9]
                   (visitAnnotation [d vis] (add-ann f :annotations d vis))
                   (visitTypeAnnotation [r p d vis] (add-type-ann f :type-annotations r p d vis)))))
             (visitMethod [access name desc sig excs]
               (let [m (atom {:flags access :signature sig :exceptions (set excs)})
                     syms (atom #{})
                     cs (code-symbols syms)]
                 (swap! methods assoc [name desc] m)
                 (swap! m assoc :code syms)
                 (proxy [MethodVisitor] [Opcodes/ASM9 (when code cs)]
                   (visitParameter [n fl] (swap! m update :parameters (fnil conj []) [n fl]))
                   (visitAnnotation [d vis] (add-ann m :annotations d vis))
                   (visitTypeAnnotation [r p d vis] (add-type-ann m :type-annotations r p d vis))
                   (visitInsnAnnotation [r p d vis] (add-type-ann m :code-type-annotations r p d vis))
                   (visitLocalVariableAnnotation [r p starts ends idx d vis]
                     (add-type-ann m :code-type-annotations r p d vis))
                   (visitTryCatchAnnotation [r p d vis] (add-type-ann m :code-type-annotations r p d vis))
                   (visitParameterAnnotation [i d vis]
                     (let [s (atom {})]
                       (swap! m update :param-annotations (fnil conj #{}) [i d vis s])
                       (ann-visitor s)))
                   (visitAnnotationDefault []
                     (let [s (atom {})] (swap! m assoc :default s) (ann-visitor s)))))))]
    (.accept cr cv ClassReader/SKIP_DEBUG)
    (assoc (cond-> (realize-anns @c) (:module @c) (update :module deref))
           :fields (into (sorted-map) (for [[k f] @fields] [k (realize-anns @f)]))
           :methods (into (sorted-map)
                          (for [[k m] @methods]
                            [k (-> (realize-anns @m)
                                   (update :code deref)
                                   (update :default #(some-> % realize))
                                   (update :param-annotations
                                           (fn [ps] (set (map (fn [[i d v s]] [i d v (realize s)]) ps)))))])))))

(defn read-shape [file & opts] (apply shape (java.nio.file.Files/readAllBytes (.toPath (io/file file))) opts))

(defn diff
  "Differences between two shapes as [path a b] triples (empty when equivalent)."
  ([a b] (diff [] a b))
  ([path a b]
   (cond
     (= a b) []
     (and (map? a) (map? b))
     (vec (mapcat (fn [k] (diff (conj path k) (get a k) (get b k)))
                  (distinct (concat (keys a) (keys b)))))
     (and (set? a) (set? b))
     [[path (clojure.set/difference a b) (clojure.set/difference b a)]]
     :else [[path a b]])))
