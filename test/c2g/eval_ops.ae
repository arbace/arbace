;; The evaluator's direct static calls (B1a step 7b, doc/go/SPEED-NOTES.md, "The evaluator"):
;; writes arbace/lang/go/CompilerOps.clj, the classes Compiler$CodeOp0 .. CodeOp3 of the Go
;; build's Compiler variant. The closure compiler (arbace/lang/go/CompilerCode.clj) turns a
;; StaticMethodExpr whose method is listed here into a CodeOpN node: a switch over the listed
;; methods of that arity, each case calling the method directly (c2g makes it a Go call) with
;; its arguments in their primitive types, where the JVM's compiled code has an invokestatic.
;;
;; Listed: the public static methods of arbace.lang.Numbers, RT and Util, and of
;; java.lang.Math those jrt has (go/arbace/jrt/math.clj), whose parameters are long, int,
;; double, boolean or a reference type of java.lang or arbace.lang (or an array of a primitive
;; or of Object), whose return type is one of those or void, with at most 3 parameters.
;;
;; Usage (in the checkout, after bin/build-arbace):
;;   java -cp target/stage2:. arbace.lang.Main test/c2g/eval_ops.clj > arbace/lang/go/CompilerOps.clj
(ns eval-ops
  (:require [arbace.string :as str]))

(def classes [arbace.lang.Numbers arbace.lang.RT arbace.lang.Util java.lang.Math])

(def jrt-math
  (set (map second (re-seq #"go/func (Math_[A-Za-z0-9_]+)" (slurp "go/arbace/jrt/math.clj")))))

(defn kind
  "The kind of a parameter or return class: J I D Z V, O for a usable reference, nil otherwise."
  [^Class c]
  (cond
    (= c Long/TYPE) "J"
    (= c Integer/TYPE) "I"
    (= c Double/TYPE) "D"
    (= c Boolean/TYPE) "Z"
    (= c Void/TYPE) "V"
    (.isPrimitive c) nil
    (.isArray c) (let [k (.getComponentType c)]
                   (when (or (.isPrimitive k) (= k Object)) "O"))
    (= (.getName c) "java.lang.Object") "O"
    (or (str/starts-with? (.getName c) "java.lang.") (str/starts-with? (.getName c) "arbace.lang."))
    (when (and (not (str/includes? (.getName c) "$")) (java.lang.reflect.Modifier/isPublic (.getModifiers c))
               (not (str/starts-with? (.getName c) "java.lang.reflect.")))
      "O")
    :else nil))

(defn desc [^Class c]
  (cond
    (= c Long/TYPE) "J" (= c Integer/TYPE) "I" (= c Double/TYPE) "D" (= c Boolean/TYPE) "Z"
    (= c Void/TYPE) "V" (= c Character/TYPE) "C" (= c Float/TYPE) "F" (= c Short/TYPE) "S"
    (= c Byte/TYPE) "B"
    (.isArray c) (str/replace (.getName c) "." "/")
    :else (str "L" (str/replace (.getName c) "." "/") ";")))

(defn sig
  "The key the evaluator looks a method up by: class.name(descriptor)."
  [^java.lang.reflect.Method m]
  (str (.getName (.getDeclaringClass m)) "." (.getName m) "("
       (apply str (map desc (.getParameterTypes m))) ")" (desc (.getReturnType m))))

(defn go-name
  "jrt's Go name of a Math method (c2g's: Math_Abs_D__D)."
  [^java.lang.reflect.Method m]
  (let [code (fn [^Class c] (cond (= c Long/TYPE) "J" (= c Integer/TYPE) "I" (= c Double/TYPE) "D"
                                  (= c Float/TYPE) "F" (= c Boolean/TYPE) "Z" :else "O"))]
    (str "Math_" (str/capitalize (subs (.getName m) 0 1)) (subs (.getName m) 1)
         (apply str (map #(str "_" (code %)) (.getParameterTypes m)))
         "__" (code (.getReturnType m)))))

(defn usable? [^java.lang.reflect.Method m]
  (let [mods (.getModifiers m)]
    (and (java.lang.reflect.Modifier/isStatic mods)
         (java.lang.reflect.Modifier/isPublic mods)
         (not (.isSynthetic m))
         (<= (.getParameterCount m) 3)
         (every? kind (.getParameterTypes m))
         (kind (.getReturnType m))
         (or (not= (.getDeclaringClass m) java.lang.Math)
             (and (every? #(not= "O" (kind %)) (cons (.getReturnType m) (.getParameterTypes m)))
                  (contains? jrt-math (go-name m)))))))

(def listed
  (->> classes
       (mapcat #(.getDeclaredMethods ^Class %))
       (filter usable?)
       (sort-by sig)
       vec))

(defn type-sym
  "A class as class forms name it."
  [^Class c]
  (cond
    (= c Long/TYPE) "long" (= c Integer/TYPE) "int" (= c Double/TYPE) "double"
    (= c Boolean/TYPE) "boolean"
    (.isArray c) (str (type-sym (.getComponentType c)) "/1")
    (= c Object) "Object"
    :else (.getName c)))

(def arg-names ["a" "b" "c"])

(defn arg-expr [^Class c i]
  (let [a (arg-names i)]
    (case (kind c)
      "J" (str "(.runLong " a " f)")
      "I" (str "(unchecked-int (.runLong " a " f))")
      "D" (str "(.runDouble " a " f)")
      "Z" (str "(.runBool " a " f)")
      "O" (if (= c Object) (str "(.run " a " f)") (str "(cast " (type-sym c) " (.run " a " f))")))))

(defn call
  "The call of m on its arguments x0 x1 x2, after they are computed and the frame's line set
  (the line of the call in stack traces), its value passed to wrap."
  [^java.lang.reflect.Method m wrap]
  (let [ps (.getParameterTypes m)
        c (str "(^[" (str/join " " (map type-sym ps)) "] "
               (.getName (.getDeclaringClass m)) "/" (.getName m)
               (apply str (map-indexed (fn [i _] (str " x" i)) ps))
               ")")]
    (str "(let [" (str/join " " (map-indexed (fn [i p] (str "x" i " " (arg-expr p i))) ps)) "] "
         "(set! (.-line f) line) " (wrap c) ")")))

(defn call-expr [m] (call m identity))

(defn boxed [^java.lang.reflect.Method m]
  (call m (fn [c]
            (case (kind (.getReturnType m))
              "J" (str "(Long/valueOf " c ")")
              "I" (str "(Integer/valueOf " c ")")
              "D" (str "(Double/valueOf " c ")")
              "Z" (str "(if " c " Boolean/TRUE Boolean/FALSE)")
              "V" (str "(do " c " nil)")
              "O" c))))

(defn op-class [n ms]
  (let [indexed (map-indexed (fn [i m] {:op i :m m}) ms)
        of-kind (fn [ks] (filter #(contains? ks (kind (.getReturnType ^java.lang.reflect.Method (:m %)))) indexed))
        sw (fn [ms f]
             (if (empty? ms)
               (str "(throw (IllegalStateException. \"CodeOp" n ": not of this kind\"))")
             (str "(switch op"
                  (apply str (map (fn [{:keys [op m]}] (str "\n          " op " " (f m))) ms))
                  "\n          (throw (IllegalStateException. \"CodeOp" n ": not of this kind\")))")))
        fields (apply str (map #(str "\n      (field ^:public ^Compiler$Code " % ")") (take n arg-names)))
        ctor (str "\n\n      (constructor ^:public [this ^int op ^int line"
                  (apply str (map #(str " ^Compiler$Code " %) (take n arg-names))) "]"
                  "\n        (set! (.-op this) op)\n        (set! (.-line this) line)"
                  (apply str (map #(str "\n        (set! (.-" % " this) " % ")") (take n arg-names))) ")")]
    (str "
  (c2g/add
    (defclass ^:public ^:static CodeOp" n "
      :extends Compiler$Code
      ;; a direct call of one of the methods of arity " n " listed by CodeOps/table (generated)
      (field ^:public ^int op)
      ;; the line of the call
      (field ^:public ^int line)" fields ctor "

      (method ^:public run [this ^Compiler$Frame f]
        " (sw indexed boxed) ")

      (method ^:public runLong ^long [this ^Compiler$Frame f]
        " (sw (of-kind #{"J" "I"}) call-expr) ")

      (method ^:public runDouble ^double [this ^Compiler$Frame f]
        " (sw (of-kind #{"D"}) call-expr) ")

      (method ^:public runBool ^boolean [this ^Compiler$Frame f]
        " (sw (of-kind #{"Z"}) call-expr) ")))
")))

(let [by-arity (group-by #(.getParameterCount ^java.lang.reflect.Method %) listed)]
  (println ";; Generated by test/c2g/eval_ops.clj from the JVM's arbace.lang.Numbers, RT, Util and")
  (println ";; java.lang.Math (those of jrt's Math): do not edit. The evaluator's direct static calls")
  (println ";; (doc/go/SPEED-NOTES.md, \"The evaluator\"): " (count listed) " methods.")
  (println "(in-ns 'arbace.lang)")
  (println)
  (println "(c2g/variant Compiler")
  (println "  (c2g/add
    (defclass ^:public ^:static CodeOps
      ;; the listed methods by their key (class.name(descriptor)): the arity times 10000 plus the
      ;; method's case in CodeOpN
      (method ^:public ^:static table ^java.util.HashMap []
        (let [m (java.util.HashMap.)]")
  (doseq [n (range 4) [i m] (map-indexed vector (by-arity n))]
    (println (str "          (.put m \"" (sig m) "\" (Integer/valueOf " (+ (* n 10000) i) "))")))
  (println "          m))))")
  (doseq [n (range 4)]
    (print (op-class n (by-arity n))))
  (println ")"))
