;; Go-build variant of arbace.lang.Compiler, part 2 (read by c2g only): the class forms at the
;; REPL (doc/go/CLASSFORMS-REPL.md). On the JVM, defclass and the code forms (doc/classes/SPEC.md
;; §9.5) are compiled to bytecode by arbace.classes. The Go build has no bytecode: the classes a
;; class form makes at run time are classes made at run time (Dyn, C2G-SPEC §5.12) whose methods
;; are interpreted. The analysis is arbace.classes's own (arbace.classes.analyze, embedded and
;; evaluated); arbace.classes.interp (a namespace of the Go build, arbace/lang/go/ns/) builds the
;; nodes below from its typed node trees, once per method; the nodes evaluate themselves, typed
;; (long for int-like values, double for float and double, objects for the rest), so primitive
;; code does not box.
;;
;; Compiler$CF holds the interpreter (nodes, frames, classes, methods); Compiler$CFGo, the Go
;; build's host: the natives over Dyn (arbace.c2g.dyncf). CF itself uses only public members of
;; the runtime and the JDK, so that it compiles on the JVM too, where a test host (TestHost)
;; stands for the run-time classes (test/classforms/).
(in-ns 'arbace.lang)

(c2g/variant Compiler
  (c2g/add
    (defclass ^:public ^:static CF
      ;; the interpreter's state and helpers
      (defclass ^:public ^:static Rt
        ;; the host: how classes made at run time and their objects are represented (Go: Dyn)
        (field ^:public ^:static ^Host HOST)

        (method ^:public ^:static setHost ^void [^Host h] (set! HOST h))

        ;; the deepest nesting of interpreted calls on one thread before StackOverflowError
        ;; (C2G-SPEC §10.5: Go's stack overflow is fatal)
        (field ^:public ^:static ^:final ^int MAX_DEPTH 10000)

        ;; the depth at which calls from outside the interpreter start, per thread
        (field ^:static ^:final ^ThreadLocal DEPTH (ThreadLocal.))

        ;; jump codes (Frame.jump): 0 none, RETURN, otherwise a target's break (id * 2) or its
        ;; recur (id * 2 + 1)
        (field ^:public ^:static ^:final ^int RETURN -1)

        ;; ------------------------------------------------------------------------------------
        ;; values: a node's type is a char, the first char of its descriptor (I J S B C Z F D V),
        ;; L for references, N for null, X for none (it does not complete)

        ;; the slot kind of a type: J (long slots: I J S B C Z), D (double slots: F D), V, L
        (method ^:public ^:static kind ^char [^char t]
          (switch t
            (\I \J \S \B \C \Z) \J
            (\F \D) \D
            \V \V
            \L))

        (method ^:public ^:static typeOf ^char [^String desc]
          (cond
            (nil? desc) \X
            (.equals desc ":null") \N
            (.equals desc ":none") \X
            :else (let [c (.charAt desc 0)]
                    (if (or (== c \L) (== c \[)) \L c))))

        ;; a long slot's value as the object of type t
        (method ^:public ^:static box [^char t ^long v]
          (switch t
            \I (Integer/valueOf (unchecked-int v))
            \J (Long/valueOf v)
            \S (Short/valueOf (unchecked-short v))
            \B (Byte/valueOf (unchecked-byte v))
            \C (Character/valueOf (unchecked-char v))
            \Z (if (== v 0) Boolean/FALSE Boolean/TRUE)
            (Long/valueOf v)))

        (method ^:public ^:static boxD [^char t ^double v]
          (if (== t \F) (Float/valueOf (unchecked-float v)) (Double/valueOf v)))

        ;; an object (a wrapper) as a long slot's value
        (method ^:public ^:static toJ ^long [o]
          (cond
            (instance? Number o) (.longValue (cast Number o))
            (instance? Character o) (unchecked-long (.charValue (cast Character o)))
            (instance? Boolean o) (if (.booleanValue (cast Boolean o)) 1 0)
            (nil? o) (throw (NullPointerException.))
            :else (throw (ClassCastException. (java-str "class " (.getName (.getClass o))
                                                        " cannot be unboxed to a primitive")))))

        (method ^:public ^:static toD ^double [o]
          (cond
            (instance? Number o) (.doubleValue (cast Number o))
            (instance? Character o) (unchecked-double (.charValue (cast Character o)))
            (nil? o) (throw (NullPointerException.))
            :else (throw (ClassCastException. (java-str "class " (.getName (.getClass o))
                                                        " cannot be unboxed to a primitive")))))

        ;; an object as the value of a slot of type t, converted as the method's or field's type
        ;; wants it (an argument from outside: reflection, a Dyn method)
        (method ^:public ^:static narrow ^long [^char t ^long v]
          (switch t
            \I (unchecked-long (unchecked-int v))
            \S (unchecked-long (unchecked-short v))
            \B (unchecked-long (unchecked-byte v))
            \C (unchecked-long (unchecked-char v))
            v))

        ;; Clojure truth
        (method ^:public ^:static truth ^boolean [o]
          (and (some? o) (not (identical? o Boolean/FALSE))))

        ;; the zero value of a field or array element of type t
        (method ^:public ^:static zero [^char t]
          (switch t
            \I (Integer/valueOf 0)
            \J (Long/valueOf 0)
            \S (Short/valueOf (unchecked-short 0))
            \B (Byte/valueOf (unchecked-byte 0))
            \C (Character/valueOf (unchecked-char 0))
            \Z Boolean/FALSE
            \F (Float/valueOf (float 0.0))
            \D (Double/valueOf 0.0)
            nil))

        ;; the call depth of this thread outside the interpreter's own calls
        (method ^:public ^:static depth ^int []
          (let [a (cast int/1 (.get DEPTH))]
            (if (nil? a) 0 (aget a 0))))

        (method ^:public ^:static enter ^int/1 []
          (let [^:mutable a (cast int/1 (.get DEPTH))]
            (when (nil? a)
              (set! a (new int/1 1))
              (.set DEPTH a))
            a))

        ;; an exception thrown by a reflective call, as the call itself would have thrown it
        (method ^:public ^:static unwrap ^Throwable [^Throwable e]
          (if (and (instance? java.lang.reflect.InvocationTargetException e) (some? (.getCause e)))
              (.getCause e)
              e))

        (method ^:public ^:static isInstance ^boolean [^Class c ^Klass k o]
          (cond
            (nil? o) false
            (some? k) (let [ok (.klassOf HOST o)] (and (some? ok) (.isSubclassOf ok k)))
            :else (.isInstance c o)))

        ;; a new object of an interpreted class from outside (reflection): allocated, then the
        ;; constructor (which runs the field initializers after its super call)
        (method ^:public ^:static construct [^Meth ctor ^Object/1 args]
          (let [k (.-k ctor)]
            (.init k)
            (let [o (.alloc HOST k)]
              (.invoke ctor o args)
              o)))

        ;; the arguments of a call into the callee's frame g, by the parameters' kinds; false
        ;; when an argument jumped
        (method ^:public ^:static args ^boolean [^Meth m ^Node/1 args ^Frame f ^Frame g]
          (let [ps (.-pslots m) ts (.-ptypes m)]
            (loop [^int i 0]
              (if (< i (alength args))
                  (let [a (aget args i)]
                    (switch (Rt/kind (aget ts i))
                      \J (let [v (.evalJ a f)] (aset (.-p g) (aget ps i) v))
                      \D (let [v (.evalD a f)] (.setD g (aget ps i) v))
                      (let [v (.eval a f)] (aset (.-r g) (aget ps i) v)))
                    (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) false))
                  true))))

        ;; the arguments as objects (a reflective call), by the parameters' types
        (method ^:public ^:static boxed ^Object/1 [^Node/1 args ^char/1 ts ^Frame f]
          (let [vs (new Object/1 (alength args))]
            (loop [^int i 0]
              (if (< i (alength args))
                  (let [a (aget args i)
                        ty (aget ts i)]
                    (aset vs i (switch (Rt/kind ty)
                                 \J (Rt/box ty (.evalJ a f))
                                 \D (Rt/boxD ty (.evalD a f))
                                 (.eval a f)))
                    (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) nil))
                  vs))))

        (method ^:public ^:static invokeKey ^String [^int n]
          (let [sb (StringBuilder. "invoke(")]
            (loop [^int i 0]
              (when (< i n)
                (.append sb "Ljava/lang/Object;")
                (recur (unchecked-inc-int i))))
            (.append sb ")Ljava/lang/Object;")
            (.toString sb)))
        )

      ;; ------------------------------------------------------------------------------------
      ;; the host

      (defclass ^:public ^:abstract ^:static Host
        ;; makes the class of k (its Class object), before its members
        (method ^:public ^:abstract define ^Class [this ^Klass k])
        ;; k's members (Go: Dyn's slots and the member tables of reflection): a method (its
        ;; params and retClass set), a constructor, a field (an object's by index, or static)
        (method ^:public ^:abstract method ^void [this ^Klass k ^Meth m])
        (method ^:public ^:abstract ctor ^void [this ^Klass k ^Meth m])
        (method ^:public ^:abstract addField ^void [this ^Klass k ^String name ^Class type ^int flags
                                                    ^int i ^boolean isStatic])
        ;; a new object of k, its fields the zero values (not constructed)
        (method ^:public ^:abstract alloc [this ^Klass k])
        ;; the class of o when it is an object of an interpreted class, else nil
        (method ^:public ^:abstract klassOf ^Klass [this o])
        (method ^:public ^:abstract field [this o ^int i])
        (method ^:public ^:abstract setField ^void [this o ^int i v])
        ;; checkcast: o, or ClassCastException with the JVM's message
        (method ^:public ^:abstract checkCast [this ^Class c o])
        ;; a Clojure fn passed for functional interface c (Clojure's FISupport adapter)
        (method ^:public ^:abstract adaptFn [this ^Class c o])
        ;; Object's own toString, hashCode and equals of an object (super calls)
        (method ^:public ^:abstract objectMethod [this ^String name o arg]))

      ;; the JVM's stand-in for classes made at run time: objects are Obj (tests only)
      (defclass ^:public ^:static Obj
        (field ^:public ^:final ^Klass klass)
        (field ^:public ^:final ^Object/1 f)
        (constructor ^:public [this ^Klass klass ^Object/1 f]
          (set! (.-klass this) klass)
          (set! (.-f this) f))
        (method ^:public toString ^String [this]
          (let [m (.virtual klass "toString()Ljava/lang/String;")]
            (if (some? m)
                (cast String (.invoke m this (new Object/1 0)))
                (java-str (.-name klass) "@" (Integer/toHexString (System/identityHashCode this))))))
        (method ^:public hashCode ^int [this]
          (let [m (.virtual klass "hashCode()I")]
            (if (some? m)
                (.intValue (cast Integer (.invoke m this (new Object/1 0))))
                (System/identityHashCode this))))
        (method ^:public equals ^boolean [this o]
          (let [m (.virtual klass "equals(Ljava/lang/Object;)Z")]
            (if (some? m)
                (.booleanValue (cast Boolean (.invoke m this (new Object/1 [o]))))
                (identical? this o)))))

      (defclass ^:public ^:static TestHost
        :extends Host
        (method ^:public define ^Class [this ^Klass k] nil)
        (method ^:public method ^void [this ^Klass k ^Meth m] nil)
        (method ^:public ctor ^void [this ^Klass k ^Meth m] nil)
        (method ^:public addField ^void [this ^Klass k ^String name ^Class type ^int flags ^int i
                                         ^boolean isStatic]
          nil)
        (method ^:public alloc [this ^Klass k]
          (if (.-fnClass k)
              (FnObj. k (.clone (.-defaults k)))
              (Obj. k (.clone (.-defaults k)))))
        (method ^:public klassOf ^Klass [this o]
          (cond
            (instance? Obj o) (.-klass (cast Obj o))
            (instance? FnObj o) (.-klass (cast FnObj o))
            :else nil))
        (method ^:public field [this o ^int i]
          (if (instance? FnObj o)
              (aget (.-f (cast FnObj o)) i)
              (aget (.-f (cast Obj o)) i)))
        (method ^:public setField ^void [this o ^int i v]
          (if (instance? FnObj o)
              (aset (.-f (cast FnObj o)) i v)
              (aset (.-f (cast Obj o)) i v)))
        (method ^:public checkCast [this ^Class c o] (.cast c o))
        (method ^:public adaptFn [this ^Class c o] o)
        (method ^:public objectMethod [this ^String name o arg]
          (switch name
            "toString" (java-str (.getName (.getClass o)) "@" (Integer/toHexString (.hashCode o)))
            "hashCode" (Integer/valueOf (System/identityHashCode o))
            (Boolean/valueOf (identical? o arg)))))

      ;; ------------------------------------------------------------------------------------
      ;; frames

      (defclass ^:public ^:static Frame
        ;; reference slots and long slots (doubles as their bits)
        (field ^:public ^:final ^Object/1 r)
        (field ^:public ^:final ^long/1 p)
        ;; a pending jump (Rt/RETURN, a target's break or recur code) and its value
        (field ^:public ^int jump)
        (field ^:public jv)
        (field ^:public ^long jl)
        (field ^:public ^double jd)
        (field ^:public ^:final ^int depth)

        (constructor ^:public [this ^int nr ^int np ^int depth]
          (set! (.-r this) (new Object/1 nr))
          (set! (.-p this) (new long/1 np))
          (set! (.-depth this) depth)
          (when (> depth Rt/MAX_DEPTH) (throw (StackOverflowError.))))

        (method ^:public getD ^double [this ^int i] (Double/longBitsToDouble (aget p i)))
        (method ^:public setD ^void [this ^int i ^double v] (aset p i (Double/doubleToRawLongBits v))))

      ;; ------------------------------------------------------------------------------------
      ;; nodes

      (defclass ^:public ^:abstract ^:static Node
        ;; the type (Rt/typeOf)
        (field ^:public ^char t)

        ;; the value as an object. A node overrides the method of its kind: evalJ for long
        ;; slots, evalD for double slots, eval for the others (or for nodes whose value comes
        ;; boxed: calls, reflection), exec for statements
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind t)
            \J (Rt/box t (.evalJ this f))
            \D (Rt/boxD t (.evalD this f))
            \V (do (.exec this f) nil)
            (throw (IllegalStateException. (java-str "eval of " (.getName (.getClass this)))))))

        (method ^:public evalJ ^long [this ^Frame f]
          (let [v (.eval this f)]
            (if (== (.-jump f) 0) (Rt/toJ v) 0)))

        (method ^:public evalD ^double [this ^Frame f]
          (let [v (.eval this f)]
            (if (== (.-jump f) 0) (Rt/toD v) 0.0)))

        (method ^:public exec ^void [this ^Frame f]
          (switch (Rt/kind t)
            \J (.evalJ this f)
            \D (.evalD this f)
            (.eval this f)))

        ;; the value as a test: Java's for boolean, Clojure truth for references
        (method ^:public test ^boolean [this ^Frame f]
          (switch t
            \Z (not (== (.evalJ this f) 0))
            (\I \J \S \B \C) (do (.evalJ this f) true)
            (\F \D) (do (.evalD this f) true)
            (\N \X) (do (.exec this f) false)
            (Rt/truth (.eval this f)))))

      ;; a constant
      (defclass ^:public ^:static Const
        :extends Node
        (field ^:public ^:final v)
        (field ^:public ^:final ^long j)
        (field ^:public ^:final ^double d)
        (constructor ^:public [this ^char t v]
          (set! (.-t this) t)
          (set! (.-v this) v)
          (set! (.-j this) (if (== (Rt/kind t) \J) (Rt/toJ v) 0))
          (set! (.-d this) (if (== (Rt/kind t) \D) (Rt/toD v) 0.0)))
        (method ^:public eval [this ^Frame f] v)
        (method ^:public evalJ ^long [this ^Frame f] j)
        (method ^:public evalD ^double [this ^Frame f] d)
        (method ^:public exec ^void [this ^Frame f] nil)
        (method ^:public test ^boolean [this ^Frame f]
          (if (== (.-t this) \Z) (not (== j 0)) (Rt/truth v))))

      ;; a local in a slot
      (defclass ^:public ^:static Local
        :extends Node
        (field ^:public ^:final ^int i)
        (constructor ^:public [this ^char t ^int i]
          (set! (.-t this) t)
          (set! (.-i this) i))
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind (.-t this))
            \J (Rt/box (.-t this) (aget (.-p f) i))
            \D (Rt/boxD (.-t this) (.getD f i))
            (aget (.-r f) i)))
        (method ^:public evalJ ^long [this ^Frame f] (aget (.-p f) i))
        (method ^:public evalD ^double [this ^Frame f] (.getD f i))
        (method ^:public exec ^void [this ^Frame f] nil))

      ;; stores a value in a slot of kind k (the binding's type); the value of set!
      (defclass ^:public ^:static SetLocal
        :extends Node
        (field ^:public ^:final ^int i)
        (field ^:public ^:final ^char k)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^char t ^int i ^Node v]
          (set! (.-t this) t)
          (set! (.-i this) i)
          (set! (.-k this) (Rt/kind t))
          (set! (.-v this) v))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [x (.evalJ v f)]
            (when (== (.-jump f) 0) (aset (.-p f) i x))
            x))
        (method ^:public evalD ^double [this ^Frame f]
          (let [x (.evalD v f)]
            (when (== (.-jump f) 0) (.setD f i x))
            x))
        (method ^:public eval [this ^Frame f]
          (switch k
            \J (Rt/box (.-t this) (.evalJ this f))
            \D (Rt/boxD (.-t this) (.evalD this f))
            (let [x (.eval v f)]
              (when (== (.-jump f) 0) (aset (.-r f) i x))
              x)))
        (method ^:public exec ^void [this ^Frame f]
          (switch k
            \J (.evalJ this f)
            \D (.evalD this f)
            (.eval this f))))

      ;; the receiver's field (a captured local, an outer instance)
      (defclass ^:public ^:static SelfField
        :extends Node
        (field ^:public ^:final ^int self)
        (field ^:public ^:final ^int i)
        (constructor ^:public [this ^char t ^int self ^int i]
          (set! (.-t this) t)
          (set! (.-self this) self)
          (set! (.-i this) i))
        (method ^:public eval [this ^Frame f]
          (.field Rt/HOST (aget (.-r f) self) i)))

      ;; outer instances: the path of this$0 fields from an object
      (defclass ^:public ^:static OuterPath
        :extends Node
        (field ^:public ^:final ^Node base)
        (field ^:public ^:final ^int/1 path)
        (constructor ^:public [this ^Node base ^int/1 path]
          (set! (.-t this) \L)
          (set! (.-base this) base)
          (set! (.-path this) path))
        (method ^:public eval [this ^Frame f]
          (let [^:mutable o (.eval base f)]
            (loop [^int k 0]
              (when (< k (alength path))
                (set! o (.field Rt/HOST o (aget path k)))
                (recur (unchecked-inc-int k))))
            o)))

      ;; ------------------------------------------------------------------------------------
      ;; arithmetic, conversions, comparisons

      ;; operations: 0 + 1 - 2 * 3 / 4 % 5 neg 6 inc 7 dec 8 & 9 | 10 ^ 11 << 12 >> 13 >>>
      ;; 14 ~ 15 &~; on int (t I), long (J), or long with overflow checks (exact)
      (defclass ^:public ^:static ArithJ
        :extends Node
        (field ^:public ^:final ^int op)
        (field ^:public ^:final ^boolean exact)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^char t ^int op ^boolean exact ^Node a ^Node b]
          (set! (.-t this) t)
          (set! (.-op this) op)
          (set! (.-exact this) exact)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [x (.evalJ a f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (let [y (if (some? b) (.evalJ b f) 0)]
              (when (not (== (.-jump f) 0)) (return 0))
              (if (== (.-t this) \I)
                  (let [xi (unchecked-int x) yi (unchecked-int y)]
                    (switch op
                      0 (unchecked-add-int xi yi)
                      1 (unchecked-subtract-int xi yi)
                      2 (unchecked-multiply-int xi yi)
                      3 (unchecked-divide-int xi yi)
                      4 (unchecked-remainder-int xi yi)
                      5 (unchecked-negate-int xi)
                      6 (unchecked-inc-int xi)
                      7 (unchecked-dec-int xi)
                      8 (bit-and-int xi yi)
                      9 (bit-or-int xi yi)
                      10 (bit-xor-int xi yi)
                      11 (bit-shift-left-int xi yi)
                      12 (bit-shift-right-int xi yi)
                      13 (unsigned-bit-shift-right-int xi yi)
                      14 (bit-not-int xi)
                      (bit-and-int xi (bit-not-int yi))))
                  (if exact
                      (switch op
                        0 (Math/addExact x y)
                        1 (Math/subtractExact x y)
                        2 (Math/multiplyExact x y)
                        6 (Math/incrementExact x)
                        7 (Math/decrementExact x)
                        3 (unchecked-divide x y)
                        (unchecked-remainder x y))
                      (switch op
                        0 (unchecked-add x y)
                        1 (unchecked-subtract x y)
                        2 (unchecked-multiply x y)
                        3 (unchecked-divide x y)
                        4 (unchecked-remainder x y)
                        5 (unchecked-negate x)
                        6 (unchecked-inc x)
                        7 (unchecked-dec x)
                        8 (bit-and x y)
                        9 (bit-or x y)
                        10 (bit-xor x y)
                        11 (bit-shift-left x (unchecked-int y))
                        12 (bit-shift-right x (unchecked-int y))
                        13 (unsigned-bit-shift-right x (unchecked-int y))
                        14 (bit-not x)
                        (bit-and x (bit-not y)))))))))

      ;; on float (t F: each result rounded to float) or double
      (defclass ^:public ^:static ArithD
        :extends Node
        (field ^:public ^:final ^int op)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^char t ^int op ^Node a ^Node b]
          (set! (.-t this) t)
          (set! (.-op this) op)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public evalD ^double [this ^Frame f]
          (let [x (.evalD a f)]
            (when (not (== (.-jump f) 0)) (return 0.0))
            (let [y (if (some? b) (.evalD b f) 0.0)]
              (when (not (== (.-jump f) 0)) (return 0.0))
              (if (== (.-t this) \F)
                  (let [xf (unchecked-float x) yf (unchecked-float y)]
                    (unchecked-double
                      (switch op
                        0 (unchecked-add-float xf yf)
                        1 (unchecked-subtract-float xf yf)
                        2 (unchecked-multiply-float xf yf)
                        3 (unchecked-divide-float xf yf)
                        4 (unchecked-remainder-float xf yf)
                        5 (unchecked-negate-float xf)
                        6 (unchecked-add-float xf (float 1.0))
                        (unchecked-subtract-float xf (float 1.0)))))
                  (switch op
                    0 (unchecked-add x y)
                    1 (unchecked-subtract x y)
                    2 (unchecked-multiply x y)
                    3 (unchecked-divide x y)
                    4 (unchecked-remainder x y)
                    5 (unchecked-negate x)
                    6 (unchecked-add x 1.0)
                    (unchecked-subtract x 1.0)))))))

      ;; a primitive conversion between the types of e (from) and t
      (defclass ^:public ^:static Conv
        :extends Node
        (field ^:public ^:final ^char from)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^char t ^char from ^Node e]
          (set! (.-t this) t)
          (set! (.-from this) from)
          (set! (.-e this) e))
        (method ^:public evalJ ^long [this ^Frame f]
          (if (== (Rt/kind from) \D)
              (let [d (.evalD e f)]
                (switch (.-t this)
                  \J (unchecked-long d)
                  \I (unchecked-long (unchecked-int d))
                  \S (unchecked-long (unchecked-short (unchecked-int d)))
                  \B (unchecked-long (unchecked-byte (unchecked-int d)))
                  \C (unchecked-long (unchecked-char (unchecked-int d)))
                  (unchecked-long d)))
              (Rt/narrow (.-t this) (.evalJ e f))))
        (method ^:public evalD ^double [this ^Frame f]
          (if (== (Rt/kind from) \D)
              (let [d (.evalD e f)]
                (if (== (.-t this) \F) (unchecked-double (unchecked-float d)) d))
              (let [l (.evalJ e f)]
                (if (== (.-t this) \F)
                    (if (== from \J)
                        (unchecked-double (unchecked-float l))
                        (unchecked-double (unchecked-float (unchecked-int l))))
                    (unchecked-double l))))))

      ;; boxing a primitive (by its own type) and unboxing a wrapper (then converted to t)
      (defclass ^:public ^:static Boxing
        :extends Node
        (field ^:public ^:final ^char from)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^char from ^Node e]
          (set! (.-t this) \L)
          (set! (.-from this) from)
          (set! (.-e this) e))
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind from)
            \J (Rt/box from (.evalJ e f))
            (Rt/boxD from (.evalD e f)))))

      (defclass ^:public ^:static Unbox
        :extends Node
        ;; the wrapper's primitive type
        (field ^:public ^:final ^char w)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^char t ^char w ^Node e]
          (set! (.-t this) t)
          (set! (.-w this) w)
          (set! (.-e this) e))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [o (.eval e f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (if (== (Rt/kind w) \D)
                (let [d (if (== w \F) (unchecked-double (.floatValue (cast Float o))) (.doubleValue (cast Double o)))]
                  (switch (.-t this)
                    \J (unchecked-long d)
                    \I (unchecked-long (unchecked-int d))
                    \S (unchecked-long (unchecked-short (unchecked-int d)))
                    \B (unchecked-long (unchecked-byte (unchecked-int d)))
                    \C (unchecked-long (unchecked-char (unchecked-int d)))
                    (unchecked-long d)))
                (Rt/narrow (.-t this)
                  (switch w
                    \I (unchecked-long (.intValue (cast Integer o)))
                    \J (.longValue (cast Long o))
                    \S (unchecked-long (.shortValue (cast Short o)))
                    \B (unchecked-long (.byteValue (cast Byte o)))
                    \C (unchecked-long (.charValue (cast Character o)))
                    \Z (if (.booleanValue (cast Boolean o)) 1 0)
                    (Rt/toJ o))))))
        (method ^:public evalD ^double [this ^Frame f]
          (let [o (.eval e f)]
            (when (not (== (.-jump f) 0)) (return 0.0))
            (let [d (switch w
                      \F (unchecked-double (.floatValue (cast Float o)))
                      \D (.doubleValue (cast Double o))
                      \J (unchecked-double (.longValue (cast Long o)))
                      \I (unchecked-double (.intValue (cast Integer o)))
                      \S (unchecked-double (.shortValue (cast Short o)))
                      \B (unchecked-double (.byteValue (cast Byte o)))
                      \C (unchecked-double (.charValue (cast Character o)))
                      (Rt/toD o))]
              (if (and (== (.-t this) \F) (not (== w \F)))
                  (if (== w \J)
                      (unchecked-double (unchecked-float (.longValue (cast Long o))))
                      (unchecked-double (unchecked-float d)))
                  d)))))

      ;; comparisons: 0 < 1 <= 2 > 3 >= 4 == 5 !=, of long or double values (Java's NaN)
      (defclass ^:public ^:static Cmp
        :extends Node
        (field ^:public ^:final ^int op)
        (field ^:public ^:final ^boolean dbl)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^int op ^boolean dbl ^Node a ^Node b]
          (set! (.-t this) \Z)
          (set! (.-op this) op)
          (set! (.-dbl this) dbl)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public test ^boolean [this ^Frame f]
          (if dbl
              (let [x (.evalD a f)]
                (when (not (== (.-jump f) 0)) (return false))
                (let [y (.evalD b f)]
                  (switch op
                    0 (< x y) 1 (<= x y) 2 (> x y) 3 (>= x y) 4 (== x y)
                    (not (== x y)))))
              (let [x (.evalJ a f)]
                (when (not (== (.-jump f) 0)) (return false))
                (let [y (.evalJ b f)]
                  (switch op
                    0 (< x y) 1 (<= x y) 2 (> x y) 3 (>= x y) 4 (== x y)
                    (not (== x y)))))))
        (method ^:public evalJ ^long [this ^Frame f] (if (.test this f) 1 0)))

      ;; tests: 0 not, 1 nil?, 2 identical?, 3 boolean =, 4 the test of a value (Clojure truth)
      (defclass ^:public ^:static Test
        :extends Node
        (field ^:public ^:final ^int op)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^int op ^Node a ^Node b]
          (set! (.-t this) \Z)
          (set! (.-op this) op)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public test ^boolean [this ^Frame f]
          (switch op
            0 (not (.test a f))
            1 (nil? (.eval a f))
            2 (let [x (.eval a f)]
                (when (not (== (.-jump f) 0)) (return false))
                (identical? x (.eval b f)))
            3 (let [x (.evalJ a f)]
                (when (not (== (.-jump f) 0)) (return false))
                (== x (.evalJ b f)))
            (.test a f)))
        (method ^:public evalJ ^long [this ^Frame f] (if (.test this f) 1 0)))

      ;; instance? of a class (of an interpreted class: by its Klass, ancestors included)
      (defclass ^:public ^:static IsInst
        :extends Node
        (field ^:public ^:final ^Class c)
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^Class c ^Klass k ^Node e]
          (set! (.-t this) \Z)
          (set! (.-c this) c)
          (set! (.-k this) k)
          (set! (.-e this) e))
        (method ^:public test ^boolean [this ^Frame f]
          (Rt/isInstance c k (.eval e f)))
        (method ^:public evalJ ^long [this ^Frame f] (if (.test this f) 1 0)))



      ;; checkcast
      (defclass ^:public ^:static CheckCast
        :extends Node
        (field ^:public ^:final ^Class c)
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^Class c ^Klass k ^Node e]
          (set! (.-t this) \L)
          (set! (.-c this) c)
          (set! (.-k this) k)
          (set! (.-e this) e))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval e f)]
            (when (and (some? o) (== (.-jump f) 0) (not (Rt/isInstance c k o)))
              (if (some? c)
                  (.checkCast Rt/HOST c o)
                  (throw (ClassCastException. (java-str "class " (.getName (.getClass o))
                                                        " cannot be cast to class " (.-name k))))))
            o)))

      ;; Objects.requireNonNull, the value kept
      (defclass ^:public ^:static NullChecked
        :extends Node
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^Node e]
          (set! (.-t this) \L)
          (set! (.-e this) e))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval e f)]
            (when (== (.-jump f) 0) (java.util.Objects/requireNonNull o))
            o)))

      ;; ------------------------------------------------------------------------------------
      ;; control

      (defclass ^:public ^:static Do
        :extends Node
        (field ^:public ^:final ^Node/1 ss)
        (field ^:public ^:final ^Node ret)
        (constructor ^:public [this ^char t ^Node/1 ss ^Node ret]
          (set! (.-t this) t)
          (set! (.-ss this) ss)
          (set! (.-ret this) ret))
        (method ^:public stmts ^boolean [this ^Frame f]
          (loop [^int i 0]
            (if (< i (alength ss))
                (do (.exec (aget ss i) f)
                    (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) false))
                true)))
        (method ^:public eval [this ^Frame f] (if (.stmts this f) (.eval ret f) nil))
        (method ^:public evalJ ^long [this ^Frame f] (if (.stmts this f) (.evalJ ret f) 0))
        (method ^:public evalD ^double [this ^Frame f] (if (.stmts this f) (.evalD ret f) 0.0))
        (method ^:public exec ^void [this ^Frame f] (when (.stmts this f) (.exec ret f)))
        (method ^:public test ^boolean [this ^Frame f] (if (.stmts this f) (.test ret f) false)))

      ;; let and loop: stores the initial values in the slots, then the body; a loop restarts on
      ;; its recur code and ends on its break code (the break's value)
      (defclass ^:public ^:static Let
        :extends Node
        (field ^:public ^:final ^int/1 slots)
        (field ^:public ^:final ^char/1 kinds)
        (field ^:public ^:final ^Node/1 inits)
        (field ^:public ^:final ^Node body)
        ;; a loop's codes, 0 for let
        (field ^:public ^:final ^int brk)
        (field ^:public ^:final ^int rec)
        (constructor ^:public [this ^char t ^int/1 slots ^char/1 kinds ^Node/1 inits ^Node body
                               ^int brk ^int rec]
          (set! (.-t this) t)
          (set! (.-slots this) slots)
          (set! (.-kinds this) kinds)
          (set! (.-inits this) inits)
          (set! (.-body this) body)
          (set! (.-brk this) brk)
          (set! (.-rec this) rec))
        (method ^:public init ^boolean [this ^Frame f]
          (loop [^int i 0]
            (if (< i (alength inits))
                (let [n (aget inits i) s (aget slots i)]
                  (switch (aget kinds i)
                    \J (let [v (.evalJ n f)] (aset (.-p f) s v))
                    \D (let [v (.evalD n f)] (.setD f s v))
                    \X (.exec n f)
                    (let [v (.eval n f)] (aset (.-r f) s v)))
                  (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) false))
                true)))
        ;; after the body: whether the loop goes on (a recur of this loop)
        (method ^:public again ^boolean [this ^Frame f]
          (if (and (== (.-jump f) rec) (not (== rec 0)))
              (do (set! (.-jump f) 0) true)
              false))
        (method ^:public broke ^boolean [this ^Frame f]
          (if (and (== (.-jump f) brk) (not (== brk 0)))
              (do (set! (.-jump f) 0) true)
              false))
        (method ^:public eval [this ^Frame f]
          (if (.init this f)
              (loop []
                (let [v (.eval body f)]
                  (cond
                    (.again this f) (recur)
                    (.broke this f) (.-jv f)
                    :else v)))
              nil))
        (method ^:public evalJ ^long [this ^Frame f]
          (if (.init this f)
              (loop []
                (let [v (.evalJ body f)]
                  (cond
                    (.again this f) (recur)
                    (.broke this f) (.-jl f)
                    :else v)))
              0))
        (method ^:public evalD ^double [this ^Frame f]
          (if (.init this f)
              (loop []
                (let [v (.evalD body f)]
                  (cond
                    (.again this f) (recur)
                    (.broke this f) (.-jd f)
                    :else v)))
              0.0))
        (method ^:public exec ^void [this ^Frame f]
          (when (.init this f)
            (loop []
              (.exec body f)
              (cond
                (.again this f) (recur)
                (.broke this f) nil
                :else nil))))
        (method ^:public test ^boolean [this ^Frame f]
          (if (.init this f)
              (loop []
                (let [v (.test body f)]
                  (cond
                    (.again this f) (recur)
                    (.broke this f) (if (== (.-t this) \Z) (not (== (.-jl f) 0)) (Rt/truth (.-jv f)))
                    :else v)))
              false)))

      ;; a labeled form: ends on its break code
      (defclass ^:public ^:static Labeled
        :extends Node
        (field ^:public ^:final ^Node body)
        (field ^:public ^:final ^int brk)
        (constructor ^:public [this ^char t ^Node body ^int brk]
          (set! (.-t this) t)
          (set! (.-body this) body)
          (set! (.-brk this) brk))
        (method ^:public broke ^boolean [this ^Frame f]
          (if (== (.-jump f) brk) (do (set! (.-jump f) 0) true) false))
        (method ^:public eval [this ^Frame f]
          (let [v (.eval body f)] (if (.broke this f) (.-jv f) v)))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [v (.evalJ body f)] (if (.broke this f) (.-jl f) v)))
        (method ^:public evalD ^double [this ^Frame f]
          (let [v (.evalD body f)] (if (.broke this f) (.-jd f) v)))
        (method ^:public exec ^void [this ^Frame f]
          (.exec body f)
          (.broke this f)))

      ;; break (with a value of kind k, or none), return (code Rt/RETURN, the method's kind)
      (defclass ^:public ^:static Jump
        :extends Node
        (field ^:public ^:final ^int code)
        (field ^:public ^:final ^char k)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^int code ^char k ^Node v]
          (set! (.-t this) \X)
          (set! (.-code this) code)
          (set! (.-k this) k)
          (set! (.-v this) v))
        (method ^:public eval [this ^Frame f]
          (when (some? v)
            (switch k
              \J (let [x (.evalJ v f)] (set! (.-jl f) x))
              \D (let [x (.evalD v f)] (set! (.-jd f) x))
              (\V \X) (.exec v f)
              (let [x (.eval v f)] (set! (.-jv f) x))))
          (when (== (.-jump f) 0)
            (when (and (nil? v) (== k \L)) (set! (.-jv f) nil))
            (set! (.-jump f) code))
          nil)
        (method ^:public evalJ ^long [this ^Frame f] (.eval this f) 0)
        (method ^:public evalD ^double [this ^Frame f] (.eval this f) 0.0)
        (method ^:public exec ^void [this ^Frame f] (.eval this f))
        (method ^:public test ^boolean [this ^Frame f] (.eval this f) false))

      ;; recur and continue: the new values of the target's slots, then its recur code
      (defclass ^:public ^:static Recur
        :extends Node
        (field ^:public ^:final ^int code)
        (field ^:public ^:final ^int/1 slots)
        (field ^:public ^:final ^char/1 kinds)
        (field ^:public ^:final ^Node/1 args)
        (constructor ^:public [this ^int code ^int/1 slots ^char/1 kinds ^Node/1 args]
          (set! (.-t this) \X)
          (set! (.-code this) code)
          (set! (.-slots this) slots)
          (set! (.-kinds this) kinds)
          (set! (.-args this) args))
        (method ^:public eval [this ^Frame f]
          (let [n (alength args)
                ls (new long/1 n)
                rs (new Object/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (let [a (aget args i)]
                  (switch (aget kinds i)
                    \J (aset ls i (.evalJ a f))
                    \D (aset ls i (Double/doubleToRawLongBits (.evalD a f)))
                    (aset rs i (.eval a f))))
                (when (not (== (.-jump f) 0)) (return nil))
                (recur (unchecked-inc-int i))))
            (loop [^int i 0]
              (when (< i n)
                (switch (aget kinds i)
                  (\J \D) (aset (.-p f) (aget slots i) (aget ls i))
                  (aset (.-r f) (aget slots i) (aget rs i)))
                (recur (unchecked-inc-int i))))
            (set! (.-jump f) code)
            nil))
        (method ^:public evalJ ^long [this ^Frame f] (.eval this f) 0)
        (method ^:public evalD ^double [this ^Frame f] (.eval this f) 0.0)
        (method ^:public exec ^void [this ^Frame f] (.eval this f))
        (method ^:public test ^boolean [this ^Frame f] (.eval this f) false))

      (defclass ^:public ^:static If
        :extends Node
        (field ^:public ^:final ^Node c)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^char t ^Node c ^Node a ^Node b]
          (set! (.-t this) t)
          (set! (.-c this) c)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public branch ^Node [this ^Frame f]
          (let [x (.test c f)]
            (cond (not (== (.-jump f) 0)) nil x a :else b)))
        (method ^:public eval [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.eval n f) nil)))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.evalJ n f) 0)))
        (method ^:public evalD ^double [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.evalD n f) 0.0)))
        (method ^:public exec ^void [this ^Frame f]
          (let [n (.branch this f)] (when (some? n) (.exec n f))))
        (method ^:public test ^boolean [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.test n f) false))))

      ;; and, or: on boolean (t Z) Java's; on other values Clojure's (the deciding value)
      (defclass ^:public ^:static AndOr
        :extends Node
        (field ^:public ^:final ^boolean or)
        (field ^:public ^:final ^Node/1 args)
        (constructor ^:public [this ^char t ^boolean or ^Node/1 args]
          (set! (.-t this) t)
          (set! (.-or this) or)
          (set! (.-args this) args))
        (method ^:public test ^boolean [this ^Frame f]
          (if (== (.-t this) \Z)
              (loop [^int i 0]
                (if (< i (alength args))
                    (let [x (.test (aget args i) f)]
                      (cond (not (== (.-jump f) 0)) false
                            (= x or) x
                            :else (recur (unchecked-inc-int i))))
                    (not or)))
              (Rt/truth (.eval this f))))
        (method ^:public evalJ ^long [this ^Frame f]
          (if (== (.-t this) \Z)
              (if (.test this f) 1 0)
              ;; primitives are always true: or gives the first, and the last
              (if or
                  (.evalJ (aget args 0) f)
                  (loop [^int i 0]
                    (let [a (aget args i)]
                      (if (< i (unchecked-dec-int (alength args)))
                          (do (.exec a f)
                              (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) 0))
                          (.evalJ a f)))))))
        (method ^:public evalD ^double [this ^Frame f]
          (if or
              (.evalD (aget args 0) f)
              (loop [^int i 0]
                (let [a (aget args i)]
                  (if (< i (unchecked-dec-int (alength args)))
                      (do (.exec a f)
                          (if (== (.-jump f) 0) (recur (unchecked-inc-int i)) 0.0))
                      (.evalD a f))))))
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind (.-t this))
            \J (Rt/box (.-t this) (.evalJ this f))
            \D (Rt/boxD (.-t this) (.evalD this f))
            (loop [^int i 0]
              (let [x (.eval (aget args i) f)]
                (cond
                  (not (== (.-jump f) 0)) nil
                  (== i (unchecked-dec-int (alength args))) x
                  (= (Rt/truth x) or) x
                  :else (recur (unchecked-inc-int i))))))))

      (defclass ^:public ^:static Throw
        :extends Node
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^Node e]
          (set! (.-t this) \X)
          (set! (.-e this) e))
        (method ^:public eval [this ^Frame f]
          (let [x (.eval e f)]
            (when (== (.-jump f) 0)
              (when (nil? x) (throw (NullPointerException.)))
              (throw (cast Throwable x)))
            nil))
        (method ^:public evalJ ^long [this ^Frame f] (.eval this f) 0)
        (method ^:public evalD ^double [this ^Frame f] (.eval this f) 0.0)
        (method ^:public exec ^void [this ^Frame f] (.eval this f))
        (method ^:public test ^boolean [this ^Frame f] (.eval this f) false))

      ;; try: catch clauses in order (each a list of classes, its binding's slot, its body);
      ;; fin runs on every exit, nfin (with-resources) on the body's normal exit and jumps
      (defclass ^:public ^:static Try
        :extends Node
        (field ^:public ^:final ^Node body)
        (field ^:public ^:final ^Class/1 classes)
        (field ^:public ^:final ^Klass/1 klasses)
        (field ^:public ^:final ^int/1 clause)
        (field ^:public ^:final ^int/1 slots)
        (field ^:public ^:final ^Node/1 handlers)
        (field ^:public ^:final ^Node fin)
        (field ^:public ^:final ^Node nfin)
        (constructor ^:public [this ^char t ^Node body ^Class/1 classes ^Klass/1 klasses
                               ^int/1 clause ^int/1 slots ^Node/1 handlers ^Node fin
                               ^Node nfin]
          (set! (.-t this) t)
          (set! (.-body this) body)
          (set! (.-classes this) classes)
          (set! (.-klasses this) klasses)
          (set! (.-clause this) clause)
          (set! (.-slots this) slots)
          (set! (.-handlers this) handlers)
          (set! (.-fin this) fin)
          (set! (.-nfin this) nfin))
        ;; runs a finally node, keeping the pending jump unless it makes one of its own
        (method ^:public ^:static finish ^void [^Node n ^Frame f]
          (let [j (.-jump f) jv (.-jv f) jl (.-jl f) jd (.-jd f)]
            (set! (.-jump f) 0)
            (.exec n f)
            (when (== (.-jump f) 0)
              (set! (.-jump f) j)
              (set! (.-jv f) jv)
              (set! (.-jl f) jl)
              (set! (.-jd f) jd))))
        (method ^:public eval [this ^Frame f]
          (let [^:mutable r nil
                ^:mutable done false]
            (try
              (try
                (set! r (.eval body f))
                (set! done true)
                (catch Throwable e
                  (let [^:mutable ^int i -1]
                    (loop [^int k 0]
                      (when (< k (alength classes))
                        (if (Rt/isInstance (aget classes k) (aget klasses k) e)
                            (set! i k)
                            (recur (unchecked-inc-int k)))))
                    (when (< i 0) (throw e))
                    (let [c (aget clause i)]
                      (aset (.-r f) (aget slots c) e)
                      (set! r (.eval (aget handlers c) f))))))
              (when (and done (some? nfin)) (Try/finish nfin f))
              (finally
                (when (some? fin) (Try/finish fin f))))
            r)))

      ;; locking
      (defclass ^:public ^:static Monitor
        :extends Node
        (field ^:public ^:final ^Node lock)
        (field ^:public ^:final ^Node body)
        (constructor ^:public [this ^char t ^Node lock ^Node body]
          (set! (.-t this) t)
          (set! (.-lock this) lock)
          (set! (.-body this) body))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval lock f)]
            (if (== (.-jump f) 0)
                (locking o (.eval body f))
                nil))))

      ;; ------------------------------------------------------------------------------------
      ;; classes and methods

      (defclass ^:public ^:static Klass
        ;; the binary name (dotted) and the internal one
        (field ^:public ^String name)
        (field ^:public ^String iname)
        ;; the Class (made by the host; nil on the JVM's test host)
        (field ^:public ^Class cls)
        (field ^:public ^int flags)
        (field ^:public ^boolean iface)
        ;; the superclass when it is interpreted, else nil (the superclass is then superClass)
        (field ^:public ^Klass sup)
        (field ^:public ^Class superClass)
        ;; the interpreted interfaces and the others (Class objects)
        (field ^:public ^Klass/1 ifaces)
        (field ^:public ^Class/1 interfaces)
        ;; instance fields: count (the superclasses' included) and zero values
        (field ^:public ^int nfields)
        (field ^:public ^Object/1 defaults)
        ;; static fields
        (field ^:public ^Object/1 statics)
        ;; the methods it declares and the virtual ones it has (inherited, defaults), by
        ;; name and descriptor
        (field ^:public ^:final ^java.util.HashMap methods (java.util.HashMap.))
        (field ^:public ^:final ^java.util.HashMap vtable (java.util.HashMap.))
        (field ^:public ^:final ^java.util.HashMap ctors (java.util.HashMap.))
        ;; the static initializer and its state: 0 not run, 1 running, 2 done, 3 failed
        (field ^:public ^Node clinit)
        (field ^:public ^int clinitR)
        (field ^:public ^int clinitP)
        (field ^:volatile ^int state)
        (field ^Thread initThread)
        ;; a Clojure fn's class (arbace.lang.AFunction, RestFn): its objects are FnObj
        (field ^:public ^boolean fnClass)
        ;; the builder's description, for the host (reflection) and for later compilations
        (field ^:public info)

        (constructor ^:public [this ^String name] (set! (.-name this) name))

        (method ^:public isSubclassOf ^boolean [this ^Klass k]
          (if (identical? this k)
              true
              (or (and (some? sup) (.isSubclassOf sup k))
                  (loop [^int i 0]
                    (if (and (some? ifaces) (< i (alength ifaces)))
                        (if (.isSubclassOf (aget ifaces i) k) true (recur (unchecked-inc-int i)))
                        false)))))

        (method ^:public virtual ^Meth [this ^String key]
          (cast Meth (.get vtable key)))

        (method ^:public declared ^Meth [this ^String key]
          (cast Meth (.get methods key)))

        ;; class initialization (JLS 12.4.2, simplified): the superclass first, then the static
        ;; initializer, once; a failed one fails every later use
        (method ^:public init ^void [this]
          (when (== state 2) (return))
          (locking this
            (cond
              (== state 2) (return)
              (and (== state 1) (identical? initThread (Thread/currentThread))) (return)
              (== state 3) (throw (NoClassDefFoundError. (java-str "Could not initialize class " name))))
            (while (== state 1) (.wait this))
            (when (== state 2) (return))
            (when (== state 3) (throw (NoClassDefFoundError. (java-str "Could not initialize class " name))))
            (set! state 1)
            (set! initThread (Thread/currentThread)))
          (try
            (when (some? sup) (.init sup))
            (when (some? clinit)
              (let [f (Frame. clinitR clinitP (Rt/depth))]
                (.exec clinit f)))
            (locking this
              (set! state 2)
              (.notifyAll this))
            (catch Throwable e
              (locking this
                (set! state 3)
                (.notifyAll this))
              (if (instance? Error e)
                  (throw e)
                  (throw (ExceptionInInitializerError. e)))))))

      ;; a method (or constructor) of an interpreted class
      (defclass ^:public ^:static Meth
        (field ^:public ^Klass k)
        (field ^:public ^String name)
        (field ^:public ^String desc)
        (field ^:public ^int flags)
        (field ^:public ^boolean isStatic)
        (field ^:public ^Node body)
        (field ^:public ^int nr)
        (field ^:public ^int np)
        ;; the receiver's slot (-1 for a static method), the parameters' slots and types
        (field ^:public ^int self)
        (field ^:public ^int/1 pslots)
        (field ^:public ^char/1 ptypes)
        (field ^:public ^char ret)
        ;; for a lambda's method: the captured values' slots (its object's fields in order)
        (field ^:public ^int/1 cslots)
        (field ^:public ^char/1 ctypes)
        ;; built on first use (the builder's fn), so that classes of one compilation refer to
        ;; each other's methods
        (field ^:public ^IFn builder)
        ;; the class's Class parameters and return, for the host
        (field ^:public ^Class/1 params)
        (field ^:public ^Class retClass)

        (constructor ^:public [this ^Klass k ^String name ^String desc ^int flags]
          (set! (.-k this) k)
          (set! (.-name this) name)
          (set! (.-desc this) desc)
          (set! (.-flags this) flags)
          (set! (.-isStatic this) (not (== 0 (bit-and-int flags 8))))
          (set! (.-self this) -1))

        (method ^:public key ^String [this] (java-str name desc))

        (method ^:public ensure ^void [this]
          (when (and (nil? body) (some? builder))
            (locking this
              (when (some? builder)
                (.invoke builder this)
                (set! builder nil))))
          (when (nil? body)
            (throw (AbstractMethodError. (java-str (.-name k) "." name desc)))))

        ;; a new frame for a call
        (method ^:public frame ^Frame [this ^int depth]
          (.ensure this)
          (Frame. nr np (unchecked-inc-int depth)))

        ;; stores argument i (an object) in its slot, converted to the parameter's type
        (method ^:public arg ^void [this ^Frame f ^int i v]
          (let [s (aget pslots i) pt (aget ptypes i)]
            (switch (Rt/kind pt)
              \J (aset (.-p f) s (Rt/narrow pt (Rt/toJ v)))
              \D (.setD f s (if (== pt \F) (unchecked-double (unchecked-float (Rt/toD v))) (Rt/toD v)))
              (aset (.-r f) s v))))

        ;; runs the body in frame f (receiver and arguments stored): the result by kind
        (method ^:public run [this ^Frame f]
          (switch (Rt/kind ret)
            \J (Rt/box ret (.runJ this f))
            \D (Rt/boxD ret (.runD this f))
            \V (do (.runV this f) nil)
            (let [v (.eval body f)]
              (if (== (.-jump f) Rt/RETURN) (.-jv f) v))))

        (method ^:public runJ ^long [this ^Frame f]
          (let [v (.evalJ body f)]
            (if (== (.-jump f) Rt/RETURN) (.-jl f) v)))

        (method ^:public runD ^double [this ^Frame f]
          (let [v (.evalD body f)]
            (if (== (.-jump f) Rt/RETURN) (.-jd f) v)))

        (method ^:public runV ^void [this ^Frame f]
          (.exec body f))

        ;; a call from outside the interpreter: o the receiver (nil for a static method), the
        ;; arguments boxed; the result boxed
        (method ^:public invoke [this o ^Object/1 args]
          (.ensure this)
          (when isStatic (.init k))
          (let [d (Rt/enter)
                depth (aget d 0)
                f (Frame. nr np (unchecked-inc-int depth))]
            (when (>= self 0) (aset (.-r f) self o))
            (Meth/capture this f o)
            (loop [^int i 0]
              (when (< i (alength pslots))
                (.arg this f i (aget args i))
                (recur (unchecked-inc-int i))))
            (aset d 0 (unchecked-inc-int depth))
            (try
              (.run this f)
              (finally (aset d 0 depth)))))

        ;; a lambda's method: the captured values from the lambda object's fields
        (method ^:public ^:static capture ^void [^Meth m ^Frame f o]
          (when (some? (.-cslots m))
            (let [cs (.-cslots m) ct (.-ctypes m)]
              (loop [^int i 0]
                (when (< i (alength cs))
                  (let [v (.field Rt/HOST o i) s (aget cs i)]
                    (if (< s 0)
                        nil
                        (switch (Rt/kind (aget ct i))
                          \J (aset (.-p f) s (Rt/toJ v))
                          \D (.setD f s (Rt/toD v))
                          (aset (.-r f) s v))))
                  (recur (unchecked-inc-int i))))))))

      ;; a method as the fn a Dyn method (or reflection) calls: the receiver first
      (defclass ^:public ^:static MethodFn
        :extends AFn
        (field ^:public ^:final ^Meth m)
        (field ^:public ^:final ^boolean isStatic)
        (constructor ^:public [this ^Meth m]
          (set! (.-m this) m)
          (set! (.-isStatic this) (.-isStatic m)))
        (method ^:public call [this ^Object/1 a]
          (if isStatic
              (.invoke m nil a)
              (let [n (unchecked-dec-int (alength a))
                    args (new Object/1 n)]
                (System/arraycopy a 1 args 0 n)
                (.invoke m (aget a 0) args))))
        (method ^:public invoke [this] (.call this (new Object/1 0)))
        (method ^:public invoke [this a] (.call this (new Object/1 [a])))
        (method ^:public invoke [this a b] (.call this (new Object/1 [a b])))
        (method ^:public invoke [this a b c] (.call this (new Object/1 [a b c])))
        (method ^:public invoke [this a b c d] (.call this (new Object/1 [a b c d])))
        (method ^:public invoke [this a b c d e] (.call this (new Object/1 [a b c d e])))
        (method ^:public invoke [this a b c d e g] (.call this (new Object/1 [a b c d e g])))
        (method ^:public invoke [this a b c d e g h] (.call this (new Object/1 [a b c d e g h])))
        (method ^:public invoke [this a b c d e g h i] (.call this (new Object/1 [a b c d e g h i])))
        (method ^:public invoke [this a b c d e g h i j] (.call this (new Object/1 [a b c d e g h i j])))
        (method ^:public invoke [this a b c d e g h i j k] (.call this (new Object/1 [a b c d e g h i j k])))
        (method ^:public invoke [this a b c d e g h i j k l]
          (.call this (new Object/1 [a b c d e g h i j k l])))
        (method ^:public invoke [this a b c d e g h i j k l m2]
          (.call this (new Object/1 [a b c d e g h i j k l m2])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p q]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p q])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p q r]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p q r])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p q r s]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p q r s])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p q r s u]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p q r s u])))
        (method ^:public invoke [this a b c d e g h i j k l m2 n o p q r s u v]
          (.call this (new Object/1 [a b c d e g h i j k l m2 n o p q r s u v])))
        (method ^:public applyTo [this ^ISeq args]
          (.call this (RT/seqToArray args))))

      ;; a constructor as the fn reflection calls (Constructor.newInstance): the new object
      (defclass ^:public ^:static CtorFn
        :extends RestFn
        (field ^:public ^:final ^Meth m)
        (constructor ^:public [this ^Meth m] (set! (.-m this) m))
        (method ^:public getRequiredArity ^int [this] 0)
        (method ^:protected doInvoke [this args]
          (Rt/construct m (RT/seqToArray (RT/seq args)))))



      ;; a Clojure fn's object (an interpreted subclass of AFunction or RestFn): every call
      ;; arrives at doInvoke, which chooses the class's invoke or doInvoke by the arguments'
      ;; count, as the compiled class would (arbace.classes.lower/fn-methods)
      (defclass ^:public ^:static FnObj
        :extends RestFn
        (field ^:public ^:final ^Klass klass)
        (field ^:public ^:final ^Object/1 f)
        ;; the fn's class, as getClass answers it (c2g's rule for a field c2g$class)
        (field ^:public ^:final ^Class c2g$class)
        (constructor ^:public [this ^Klass klass ^Object/1 f]
          (set! (.-klass this) klass)
          (set! (.-f this) f)
          (set! (.-c2g$class this) (.-cls klass)))
        (method ^:public getRequiredArity ^int [this] 0)
        (method ^:protected doInvoke [this args]
          (let [a (RT/seqToArray (RT/seq args))
                n (alength a)
                fixed (.virtual klass (Rt/invokeKey n))]
            (if (some? fixed)
                (.invoke fixed this a)
                (let [v (.virtual klass "doInvoke")
                      req (if (some? v) (alength (.-pslots v)) 0)]
                  (if (and (some? v) (>= n (unchecked-dec-int req)))
                      (let [b (new Object/1 req)]
                        (System/arraycopy a 0 b 0 (unchecked-dec-int req))
                        (aset b (unchecked-dec-int req)
                              (if (> n (unchecked-dec-int req))
                                  (ArraySeq/create (java.util.Arrays/copyOfRange a (unchecked-dec-int req) n))
                                  nil))
                        (.invoke v this b))
                      (throw (ArityException. n (.-name klass))))))))
        (method ^:public toString ^String [this]
          (let [m (.virtual klass "toString()Ljava/lang/String;")]
            (if (some? m)
                (cast String (.invoke m this (new Object/1 0)))
                (java-str (.-name klass) "@" (Integer/toHexString (System/identityHashCode this)))))))

      ;; ------------------------------------------------------------------------------------
      ;; calls





      ;; a call of an interpreted method without dispatch: static, private, super, or a
      ;; constructor's this or super call (target: the receiver; nil for static)
      (defclass ^:public ^:static CallI
        :extends Node
        (field ^:public ^:final ^Meth m)
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^Node/1 args)
        (constructor ^:public [this ^char t ^Meth m ^Node target ^Node/1 args]
          (set! (.-t this) t)
          (set! (.-m this) m)
          (set! (.-target this) target)
          (set! (.-args this) args))
        (method ^:public call ^Frame [this ^Frame f]
          (let [o (if (some? target) (.eval target f) nil)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (and (some? target) (nil? o)) (throw (NullPointerException.)))
            (when (.-isStatic m) (.init (.-k m)))
            (let [g (.frame m (.-depth f))]
              (when (>= (.-self m) 0) (aset (.-r g) (.-self m) o))
              (if (Rt/args m args f g) g nil))))
        (method ^:public eval [this ^Frame f]
          (let [g (.call this f)] (if (some? g) (.run m g) nil)))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [g (.call this f)] (if (some? g) (.runJ m g) 0)))
        (method ^:public evalD ^double [this ^Frame f]
          (let [g (.call this f)] (if (some? g) (.runD m g) 0.0)))
        (method ^:public exec ^void [this ^Frame f]
          (let [g (.call this f)]
            (when (some? g)
              (switch (Rt/kind (.-ret m))
                \J (.runJ m g)
                \D (.runD m g)
                \V (.runV m g)
                (.run m g))))))

      ;; a virtual or interface call: an interpreted receiver's method by its class's vtable,
      ;; else (or when the class does not have it) by reflection
      (defclass ^:public ^:static CallV
        :extends Node
        (field ^:public ^:final ^String key)
        (field ^:public ^:final ^java.lang.reflect.Method rm)
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^Node/1 args)
        (field ^:public ^:final ^char/1 ptypes)
        ;; the last receiver class and its method
        (field ^Klass ck)
        (field ^Meth cm)
        (constructor ^:public [this ^char t ^String key ^java.lang.reflect.Method rm ^Node target
                               ^Node/1 args ^char/1 ptypes]
          (set! (.-t this) t)
          (set! (.-key this) key)
          (set! (.-rm this) rm)
          (set! (.-target this) target)
          (set! (.-args this) args)
          (set! (.-ptypes this) ptypes))
        ;; the interpreted method for receiver o, or nil
        (method ^:public lookup ^Meth [this o]
          (let [k (.klassOf Rt/HOST o)]
            (cond
              (nil? k) nil
              (identical? k ck) cm
              :else (let [m (.virtual k key)]
                      (set! cm m)
                      (set! ck k)
                      m))))
        ;; a reflective call (the receiver and the method's arguments evaluated)
        (method ^:public reflect [this o ^Frame f]
          (let [vs (Rt/boxed args ptypes f)]
            (when (nil? vs) (return nil))
            (when (nil? rm)
              (throw (AbstractMethodError. (java-str (.getName (.getClass o)) "." key))))
            (try
              (.invoke rm o vs)
              (catch Throwable e (throw (Rt/unwrap e))))))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval target f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (nil? o) (throw (NullPointerException.)))
            (let [m (.lookup this o)]
              (if (some? m)
                  (let [g (.frame m (.-depth f))]
                    (aset (.-r g) (.-self m) o)
                    (if (Rt/args m args f g) (.run m g) nil))
                  (.reflect this o f)))))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [o (.eval target f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (when (nil? o) (throw (NullPointerException.)))
            (let [m (.lookup this o)]
              (if (and (some? m) (== (Rt/kind (.-ret m)) \J))
                  (let [g (.frame m (.-depth f))]
                    (aset (.-r g) (.-self m) o)
                    (if (Rt/args m args f g) (.runJ m g) 0))
                  (let [v (if (some? m)
                              (let [g (.frame m (.-depth f))]
                                (aset (.-r g) (.-self m) o)
                                (if (Rt/args m args f g) (.run m g) nil))
                              (.reflect this o f))]
                    (if (== (.-jump f) 0) (Rt/toJ v) 0))))))
        (method ^:public evalD ^double [this ^Frame f]
          (let [o (.eval target f)]
            (when (not (== (.-jump f) 0)) (return 0.0))
            (when (nil? o) (throw (NullPointerException.)))
            (let [m (.lookup this o)]
              (if (and (some? m) (== (Rt/kind (.-ret m)) \D))
                  (let [g (.frame m (.-depth f))]
                    (aset (.-r g) (.-self m) o)
                    (if (Rt/args m args f g) (.runD m g) 0.0))
                  (let [v (if (some? m)
                              (let [g (.frame m (.-depth f))]
                                (aset (.-r g) (.-self m) o)
                                (if (Rt/args m args f g) (.run m g) nil))
                              (.reflect this o f))]
                    (if (== (.-jump f) 0) (Rt/toD v) 0.0)))))))

      ;; a call by reflection: a static method (target nil) or a method of a class that is not
      ;; interpreted
      (defclass ^:public ^:static CallR
        :extends Node
        (field ^:public ^:final ^java.lang.reflect.Method rm)
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^Node/1 args)
        (field ^:public ^:final ^char/1 ptypes)
        (constructor ^:public [this ^char t ^java.lang.reflect.Method rm ^Node target
                               ^Node/1 args ^char/1 ptypes]
          (set! (.-t this) t)
          (set! (.-rm this) rm)
          (set! (.-target this) target)
          (set! (.-args this) args)
          (set! (.-ptypes this) ptypes))
        (method ^:public eval [this ^Frame f]
          (let [o (if (some? target) (.eval target f) nil)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (and (some? target) (nil? o)) (throw (NullPointerException.)))
            (let [vs (Rt/boxed args ptypes f)]
              (if (nil? vs)
                  nil
                  (try
                    (.invoke rm o vs)
                    (catch Throwable e (throw (Rt/unwrap e)))))))))

      ;; a super call of Object's toString, hashCode or equals
      (defclass ^:public ^:static ObjectCall
        :extends Node
        (field ^:public ^:final ^String name)
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^Node arg)
        (constructor ^:public [this ^char t ^String name ^Node target ^Node arg]
          (set! (.-t this) t)
          (set! (.-name this) name)
          (set! (.-target this) target)
          (set! (.-arg this) arg))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval target f)
                a (if (some? arg) (.eval arg f) nil)]
            (if (== (.-jump f) 0) (.objectMethod Rt/HOST name o a) nil))))

      ;; a Clojure var's value, a call of it
      (defclass ^:public ^:static VarDeref
        :extends Node
        (field ^:public ^:final ^Var v)
        (constructor ^:public [this ^Var v]
          (set! (.-t this) \L)
          (set! (.-v this) v))
        (method ^:public eval [this ^Frame f] (.deref v)))

      (defclass ^:public ^:static VarInvoke
        :extends Node
        (field ^:public ^:final ^Var v)
        (field ^:public ^:final ^Node/1 args)
        (constructor ^:public [this ^Var v ^Node/1 args]
          (set! (.-t this) \L)
          (set! (.-v this) v)
          (set! (.-args this) args))
        (method ^:public eval [this ^Frame f]
          (let [fn (cast IFn (.deref v))
                n (alength args)
                vs (new Object/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (aset vs i (.eval (aget args i) f))
                (when (not (== (.-jump f) 0)) (return nil))
                (recur (unchecked-inc-int i))))
            (switch n
              0 (.invoke fn)
              1 (.invoke fn (aget vs 0))
              2 (.invoke fn (aget vs 0) (aget vs 1))
              3 (.invoke fn (aget vs 0) (aget vs 1) (aget vs 2))
              4 (.invoke fn (aget vs 0) (aget vs 1) (aget vs 2) (aget vs 3))
              (.applyTo fn (ArraySeq/create vs))))))

      ;; a Clojure fn passed where a functional interface is expected
      (defclass ^:public ^:static FiAdapter
        :extends Node
        (field ^:public ^:final ^Class c)
        (field ^:public ^:final ^Node e)
        (constructor ^:public [this ^Class c ^Node e]
          (set! (.-t this) \L)
          (set! (.-c this) c)
          (set! (.-e this) e))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval e f)]
            (cond
              (not (== (.-jump f) 0)) nil
              (and (instance? IFn o) (not (.isInstance c o))) (.adaptFn Rt/HOST c o)
              (and (some? o) (not (.isInstance c o))) (.checkCast Rt/HOST c o)
              :else o))))

      ;; ------------------------------------------------------------------------------------
      ;; objects

      ;; new of an interpreted class: allocated, its implicit fields stored (the outer
      ;; instance, captured values), then the constructor
      (defclass ^:public ^:static NewI
        :extends Node
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^Meth ctor)
        (field ^:public ^:final ^Node/1 args)
        (field ^:public ^:final ^int/1 xfields)
        (field ^:public ^:final ^Node/1 xs)
        (constructor ^:public [this ^Klass k ^Meth ctor ^Node/1 args ^int/1 xfields ^Node/1 xs]
          (set! (.-t this) \L)
          (set! (.-k this) k)
          (set! (.-ctor this) ctor)
          (set! (.-args this) args)
          (set! (.-xfields this) xfields)
          (set! (.-xs this) xs))
        (method ^:public eval [this ^Frame f]
          (.init k)
          (let [o (.alloc Rt/HOST k)]
            (loop [^int i 0]
              (when (< i (alength xs))
                (let [v (.eval (aget xs i) f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (.setField Rt/HOST o (aget xfields i) v))
                (recur (unchecked-inc-int i))))
            (let [g (.frame ctor (.-depth f))]
              (aset (.-r g) (.-self ctor) o)
              (when (not (Rt/args ctor args f g)) (return nil))
              (.runV ctor g)
              o))))

      ;; a constructor's call of another constructor on the same object (super or this): the
      ;; implicit fields of the superclass first (an anonymous class's superclass's outer
      ;; instance)
      (defclass ^:public ^:static CtorCall
        :extends Node
        (field ^:public ^:final ^Meth ctor)
        (field ^:public ^:final ^int self)
        (field ^:public ^:final ^Node/1 args)
        (field ^:public ^:final ^int/1 xfields)
        (field ^:public ^:final ^Node/1 xs)
        (constructor ^:public [this ^Meth ctor ^int self ^Node/1 args ^int/1 xfields ^Node/1 xs]
          (set! (.-t this) \V)
          (set! (.-ctor this) ctor)
          (set! (.-self this) self)
          (set! (.-args this) args)
          (set! (.-xfields this) xfields)
          (set! (.-xs this) xs))
        (method ^:public eval [this ^Frame f]
          (let [o (aget (.-r f) self)]
            (loop [^int i 0]
              (when (< i (alength xs))
                (let [v (.eval (aget xs i) f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (.setField Rt/HOST o (aget xfields i) v))
                (recur (unchecked-inc-int i))))
            (let [g (.frame ctor (.-depth f))]
              (aset (.-r g) (.-self ctor) o)
              (when (Rt/args ctor args f g)
                (.runV ctor g))
              nil))))

      ;; new of a class that is not interpreted
      (defclass ^:public ^:static NewR
        :extends Node
        (field ^:public ^:final ^java.lang.reflect.Constructor c)
        (field ^:public ^:final ^Node/1 args)
        (field ^:public ^:final ^char/1 ptypes)
        (constructor ^:public [this ^java.lang.reflect.Constructor c ^Node/1 args ^char/1 ptypes]
          (set! (.-t this) \L)
          (set! (.-c this) c)
          (set! (.-args this) args)
          (set! (.-ptypes this) ptypes))
        (method ^:public eval [this ^Frame f]
          (let [vs (Rt/boxed args ptypes f)]
            (if (nil? vs)
                nil
                (try
                  (.newInstance c vs)
                  (catch Throwable e (throw (Rt/unwrap e))))))))

      ;; fields of interpreted classes: an object's (by index) and static ones
      (defclass ^:public ^:static GetField
        :extends Node
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^int i)
        (constructor ^:public [this ^char t ^Node target ^int i]
          (set! (.-t this) t)
          (set! (.-target this) target)
          (set! (.-i this) i))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval target f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (nil? o) (throw (NullPointerException.)))
            (.field Rt/HOST o i))))

      (defclass ^:public ^:static SetField
        :extends Node
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^int i)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^char t ^Node target ^int i ^Node v]
          (set! (.-t this) t)
          (set! (.-target this) target)
          (set! (.-i this) i)
          (set! (.-v this) v))
        (method ^:public eval [this ^Frame f]
          (let [o (.eval target f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (let [x (switch (Rt/kind (.-t this))
                      \J (Rt/box (.-t this) (.evalJ v f))
                      \D (Rt/boxD (.-t this) (.evalD v f))
                      (.eval v f))]
              (when (not (== (.-jump f) 0)) (return nil))
              (when (nil? o) (throw (NullPointerException.)))
              (.setField Rt/HOST o i x)
              x))))

      (defclass ^:public ^:static GetStatic
        :extends Node
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^int i)
        (constructor ^:public [this ^char t ^Klass k ^int i]
          (set! (.-t this) t)
          (set! (.-k this) k)
          (set! (.-i this) i))
        (method ^:public eval [this ^Frame f]
          (.init k)
          (aget (.-statics k) i)))

      (defclass ^:public ^:static SetStatic
        :extends Node
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^int i)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^char t ^Klass k ^int i ^Node v]
          (set! (.-t this) t)
          (set! (.-k this) k)
          (set! (.-i this) i)
          (set! (.-v this) v))
        (method ^:public eval [this ^Frame f]
          (.init k)
          (let [x (switch (Rt/kind (.-t this))
                    \J (Rt/box (.-t this) (.evalJ v f))
                    \D (Rt/boxD (.-t this) (.evalD v f))
                    (.eval v f))]
            (when (== (.-jump f) 0) (aset (.-statics k) i x))
            x)))

      ;; fields of other classes, by reflection (target nil: static)
      (defclass ^:public ^:static FieldR
        :extends Node
        (field ^:public ^:final ^java.lang.reflect.Field fd)
        (field ^:public ^:final ^Node target)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^char t ^java.lang.reflect.Field fd ^Node target ^Node v]
          (set! (.-t this) t)
          (set! (.-fd this) fd)
          (set! (.-target this) target)
          (set! (.-v this) v))
        (method ^:public eval [this ^Frame f]
          (let [o (if (some? target) (.eval target f) nil)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (and (some? target) (nil? o)) (throw (NullPointerException.)))
            (if (some? v)
                (let [x (switch (Rt/kind (.-t this))
                          \J (Rt/box (.-t this) (.evalJ v f))
                          \D (Rt/boxD (.-t this) (.evalD v f))
                          (.eval v f))]
                  (when (== (.-jump f) 0) (.set fd o x))
                  x)
                (.get fd o)))))

      ;; ------------------------------------------------------------------------------------
      ;; arrays

      ;; new T[n] (c the component class), new T[n][m]... (c the element class of the dims)
      (defclass ^:public ^:static NewArray
        :extends Node
        (field ^:public ^:final ^Class c)
        (field ^:public ^:final ^Node/1 dims)
        (constructor ^:public [this ^Class c ^Node/1 dims]
          (set! (.-t this) \L)
          (set! (.-c this) c)
          (set! (.-dims this) dims))
        (method ^:public eval [this ^Frame f]
          (if (== (alength dims) 1)
              (let [n (.evalJ (aget dims 0) f)]
                (if (== (.-jump f) 0) (java.lang.reflect.Array/newInstance c (unchecked-int n)) nil))
              (let [ns (new int/1 (alength dims))]
                (loop [^int i 0]
                  (when (< i (alength dims))
                    (aset ns i (unchecked-int (.evalJ (aget dims i) f)))
                    (when (not (== (.-jump f) 0)) (return nil))
                    (recur (unchecked-inc-int i))))
                (java.lang.reflect.Array/newInstance c ns)))))

      ;; new T[] {e...}
      (defclass ^:public ^:static ArrayInit
        :extends Node
        (field ^:public ^:final ^Class c)
        (field ^:public ^:final ^char et)
        (field ^:public ^:final ^Node/1 elems)
        (constructor ^:public [this ^Class c ^char et ^Node/1 elems]
          (set! (.-t this) \L)
          (set! (.-c this) c)
          (set! (.-et this) et)
          (set! (.-elems this) elems))
        (method ^:public eval [this ^Frame f]
          (let [n (alength elems)
                vs (new Object/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (let [e (aget elems i)]
                  (aset vs i (switch (Rt/kind et)
                               \J (Rt/box et (.evalJ e f))
                               \D (Rt/boxD et (.evalD e f))
                               (.eval e f))))
                (when (not (== (.-jump f) 0)) (return nil))
                (recur (unchecked-inc-int i))))
            (let [a (java.lang.reflect.Array/newInstance c n)]
              (loop [^int i 0]
                (when (< i n)
                  (java.lang.reflect.Array/set a i (aget vs i))
                  (recur (unchecked-inc-int i))))
              a))))

      ;; a[i] (et the element type) and a[i] = v
      (defclass ^:public ^:static Aget
        :extends Node
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node i)
        (constructor ^:public [this ^char t ^Node a ^Node i]
          (set! (.-t this) t)
          (set! (.-a this) a)
          (set! (.-i this) i))
        (method ^:public array [this ^Frame f]
          (let [x (.eval a f)]
            (when (and (nil? x) (== (.-jump f) 0)) (throw (NullPointerException.)))
            x))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [x (.array this f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (let [k (unchecked-int (.evalJ i f))]
              (when (not (== (.-jump f) 0)) (return 0))
              (switch (.-t this)
                \I (aget (cast int/1 x) k)
                \J (aget (cast long/1 x) k)
                \S (aget (cast short/1 x) k)
                \B (aget (cast byte/1 x) k)
                \C (unchecked-long (aget (cast char/1 x) k))
                (if (aget (cast boolean/1 x) k) 1 0)))))
        (method ^:public evalD ^double [this ^Frame f]
          (let [x (.array this f)]
            (when (not (== (.-jump f) 0)) (return 0.0))
            (let [k (unchecked-int (.evalJ i f))]
              (when (not (== (.-jump f) 0)) (return 0.0))
              (if (== (.-t this) \F)
                  (unchecked-double (aget (cast float/1 x) k))
                  (aget (cast double/1 x) k)))))
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind (.-t this))
            \J (Rt/box (.-t this) (.evalJ this f))
            \D (Rt/boxD (.-t this) (.evalD this f))
            (let [x (.array this f)]
              (when (not (== (.-jump f) 0)) (return nil))
              (let [k (unchecked-int (.evalJ i f))]
                (when (not (== (.-jump f) 0)) (return nil))
                (aget (cast Object/1 x) k))))))

      (defclass ^:public ^:static Aset
        :extends Node
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node i)
        (field ^:public ^:final ^Node v)
        (constructor ^:public [this ^char t ^Node a ^Node i ^Node v]
          (set! (.-t this) t)
          (set! (.-a this) a)
          (set! (.-i this) i)
          (set! (.-v this) v))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [x (.eval a f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (let [k (unchecked-int (.evalJ i f))]
              (when (not (== (.-jump f) 0)) (return 0))
              (let [y (.evalJ v f)]
                (when (not (== (.-jump f) 0)) (return 0))
                (when (nil? x) (throw (NullPointerException.)))
                (switch (.-t this)
                  \I (aset (cast int/1 x) k (unchecked-int y))
                  \J (aset (cast long/1 x) k y)
                  \S (aset (cast short/1 x) k (unchecked-short y))
                  \B (aset (cast byte/1 x) k (unchecked-byte y))
                  \C (aset (cast char/1 x) k (unchecked-char y))
                  (aset (cast boolean/1 x) k (not (== y 0))))
                y))))
        (method ^:public evalD ^double [this ^Frame f]
          (let [x (.eval a f)]
            (when (not (== (.-jump f) 0)) (return 0.0))
            (let [k (unchecked-int (.evalJ i f))]
              (when (not (== (.-jump f) 0)) (return 0.0))
              (let [y (.evalD v f)]
                (when (not (== (.-jump f) 0)) (return 0.0))
                (when (nil? x) (throw (NullPointerException.)))
                (if (== (.-t this) \F)
                    (aset (cast float/1 x) k (unchecked-float y))
                    (aset (cast double/1 x) k y))
                y))))
        (method ^:public eval [this ^Frame f]
          (switch (Rt/kind (.-t this))
            \J (Rt/box (.-t this) (.evalJ this f))
            \D (Rt/boxD (.-t this) (.evalD this f))
            (let [x (.eval a f)]
              (when (not (== (.-jump f) 0)) (return nil))
              (let [k (unchecked-int (.evalJ i f))]
                (when (not (== (.-jump f) 0)) (return nil))
                (let [y (.eval v f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (when (nil? x) (throw (NullPointerException.)))
                  (aset (cast Object/1 x) k y)
                  y))))))

      (defclass ^:public ^:static Alength
        :extends Node
        (field ^:public ^:final ^Node a)
        (constructor ^:public [this ^Node a]
          (set! (.-t this) \I)
          (set! (.-a this) a))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [x (.eval a f)]
            (when (not (== (.-jump f) 0)) (return 0))
            (when (nil? x) (throw (NullPointerException.)))
            (java.lang.reflect.Array/getLength x))))

      ;; an array's clone()
      (defclass ^:public ^:static ArrayClone
        :extends Node
        (field ^:public ^:final ^Node a)
        (constructor ^:public [this ^Node a]
          (set! (.-t this) \L)
          (set! (.-a this) a))
        (method ^:public eval [this ^Frame f]
          (let [x (.eval a f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (when (nil? x) (throw (NullPointerException.)))
            (let [n (java.lang.reflect.Array/getLength x)
                  y (java.lang.reflect.Array/newInstance (.getComponentType (.getClass x)) n)]
              (System/arraycopy x 0 y 0 n)
              y))))

      ;; ------------------------------------------------------------------------------------
      ;; strings, loops over collections

      ;; java-str: literal parts (nil nodes) and values; eager values are converted at once,
      ;; the others after all are evaluated (as StringConcatFactory)
      (defclass ^:public ^:static JavaStr
        :extends Node
        (field ^:public ^:final ^String/1 lits)
        (field ^:public ^:final ^Node/1 parts)
        (field ^:public ^:final ^char/1 types)
        (field ^:public ^:final ^boolean/1 eager)
        (constructor ^:public [this ^String/1 lits ^Node/1 parts ^char/1 types ^boolean/1 eager]
          (set! (.-t this) \L)
          (set! (.-lits this) lits)
          (set! (.-parts this) parts)
          (set! (.-types this) types)
          (set! (.-eager this) eager))
        (method ^:public ^:static str ^String [^char ty v]
          (cond
            (nil? v) "null"
            (== ty \C) (String/valueOf (.charValue (cast Character v)))
            :else (.toString v)))
        (method ^:public eval [this ^Frame f]
          (let [n (alength parts)
                vs (new Object/1 n)]
            (loop [^int i 0]
              (when (< i n)
                (let [p (aget parts i)]
                  (when (some? p)
                    (let [ty (aget types i)
                          v (switch (Rt/kind ty)
                              \J (Rt/box ty (.evalJ p f))
                              \D (Rt/boxD ty (.evalD p f))
                              (.eval p f))]
                      (when (not (== (.-jump f) 0)) (return nil))
                      (aset vs i (if (aget eager i) (JavaStr/str ty v) v)))))
                (recur (unchecked-inc-int i))))
            (let [sb (StringBuilder.)]
              (loop [^int i 0]
                (when (< i n)
                  (if (nil? (aget parts i))
                      (.append sb (aget lits i))
                      (.append sb (JavaStr/str (aget types i) (aget vs i))))
                  (recur (unchecked-inc-int i))))
              (.toString sb)))))

      ;; for-each over an array (hidden slots arr, len, i) or an Iterable (hidden slot it):
      ;; elem (reading the hidden slots) is stored in the binding's slot, then the body; its
      ;; break ends the loop, its recur (continue) goes to the next element
      (defclass ^:public ^:static ForEach
        :extends Node
        (field ^:public ^:final ^Node coll)
        (field ^:public ^:final ^boolean array)
        (field ^:public ^:final ^int arr)
        (field ^:public ^:final ^int len)
        (field ^:public ^:final ^int i)
        (field ^:public ^:final ^Node elem)
        (field ^:public ^:final ^int slot)
        (field ^:public ^:final ^char kind)
        (field ^:public ^:final ^Node body)
        (field ^:public ^:final ^int brk)
        (field ^:public ^:final ^int rec)
        (constructor ^:public [this ^char t ^Node coll ^boolean array ^int arr ^int len ^int i
                               ^Node elem ^int slot ^char kind ^Node body ^int brk ^int rec]
          (set! (.-t this) t)
          (set! (.-coll this) coll)
          (set! (.-array this) array)
          (set! (.-arr this) arr)
          (set! (.-len this) len)
          (set! (.-i this) i)
          (set! (.-elem this) elem)
          (set! (.-slot this) slot)
          (set! (.-kind this) kind)
          (set! (.-body this) body)
          (set! (.-brk this) brk)
          (set! (.-rec this) rec))
        (method ^:public eval [this ^Frame f]
          (let [c (.eval coll f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (if array
                (do (when (nil? c) (throw (NullPointerException.)))
                    (aset (.-r f) arr c)
                    (aset (.-p f) len (java.lang.reflect.Array/getLength c))
                    (aset (.-p f) i 0))
                (aset (.-r f) arr c))
            (loop []
              (when (if array
                        (< (aget (.-p f) i) (aget (.-p f) len))
                        (.hasNext (cast java.util.Iterator (aget (.-r f) arr))))
                (switch kind
                  \J (let [v (.evalJ elem f)] (aset (.-p f) slot v))
                  \D (let [v (.evalD elem f)] (.setD f slot v))
                  (let [v (.eval elem f)] (aset (.-r f) slot v)))
                (when (not (== (.-jump f) 0)) (return nil))
                (.exec body f)
                (let [j (.-jump f)]
                  (cond
                    (== j brk) (do (set! (.-jump f) 0) (return (.-jv f)))
                    (and (not (== j 0)) (not (== j rec))) (return nil)
                    :else (do (set! (.-jump f) 0)
                              (when array (aset (.-p f) i (unchecked-inc (aget (.-p f) i))))
                              (recur))))))
            nil)))

      ;; ------------------------------------------------------------------------------------
      ;; switch and patterns

      ;; a pattern: a type test (c or k) unless untested, then a type pattern's binding (slot,
      ;; kind) or a record pattern's components (each an accessor and a pattern)
      (defclass ^:public ^:static Pat
        (field ^:public ^Class c)
        (field ^:public ^Klass k)
        (field ^:public ^boolean test)
        (field ^:public ^int slot)
        (field ^:public ^char kind)
        (field ^:public ^Node/1 accessors)
        (field ^:public ^Pat/1 comps)
        (field ^:public ^char/1 ctypes)
        (constructor ^:public [this] (set! (.-slot this) -1))
        ;; whether v (of the static type the pattern was analyzed against) matches, binding the
        ;; variables
        (method ^:public match ^boolean [this ^Frame f v]
          (when (and test (not (Rt/isInstance c k v))) (return false))
          (if (some? accessors)
              ;; the accessors read the record from slot `slot`
              (do (aset (.-r f) slot v)
                  (loop [^int i 0]
                    (if (< i (alength accessors))
                        (let [x (try
                                  (.eval (aget accessors i) f)
                                  (catch Throwable e
                                    (throw (MatchException. (.toString e) e))))]
                          (if (.match (aget comps i) f x)
                              (recur (unchecked-inc-int i))
                              false))
                        true)))
              (do (when (>= slot 0)
                    (switch kind
                      \J (aset (.-p f) slot (Rt/toJ v))
                      \D (.setD f slot (Rt/toD v))
                      (aset (.-r f) slot v)))
                  true))))

      ;; switch: kind 0 int, 1 String, 2 enum (by the constants' names), 3 patterns; the arms,
      ;; the default (nil: none), the arm of nil (-1: NullPointerException), the arm that is
      ;; also the default (-1: the default node)
      (defclass ^:public ^:static Switch
        :extends Node
        (field ^:public ^:final ^int kind)
        (field ^:public ^:final ^Node sel)
        (field ^:public ^:final ^Node/1 arms)
        (field ^:public ^:final ^Node dflt)
        (field ^:public ^:final ^java.util.HashMap keys)
        (field ^:public ^:final ^int nullArm)
        (field ^:public ^:final ^int dfltArm)
        ;; patterns: labels in order (pattern or constant), guard, arm
        (field ^:public ^:final ^Pat/1 pats)
        (field ^:public ^:final ^Object/1 consts)
        (field ^:public ^:final ^Node/1 guards)
        (field ^:public ^:final ^int/1 parms)
        (constructor ^:public [this ^char t ^int kind ^Node sel ^Node/1 arms ^Node dflt
                               ^java.util.HashMap keys ^int nullArm ^int dfltArm ^Pat/1 pats
                               ^Object/1 consts ^Node/1 guards ^int/1 parms]
          (set! (.-t this) t)
          (set! (.-kind this) kind)
          (set! (.-sel this) sel)
          (set! (.-arms this) arms)
          (set! (.-dflt this) dflt)
          (set! (.-keys this) keys)
          (set! (.-nullArm this) nullArm)
          (set! (.-dfltArm this) dfltArm)
          (set! (.-pats this) pats)
          (set! (.-consts this) consts)
          (set! (.-guards this) guards)
          (set! (.-parms this) parms))
        ;; the node to evaluate (nil: none, the value nil)
        (method ^:public choose ^Node [this ^Frame f]
          (let [^:mutable ^int arm -2]
            (switch kind
              0 (let [v (.evalJ sel f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (let [a (.get keys (Long/valueOf v))]
                    (when (some? a) (set! arm (.intValue (cast Integer a))))))
              1 (let [v (.eval sel f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (when (nil? v) (throw (NullPointerException.)))
                  (let [a (.get keys (.toString v))]
                    (when (some? a) (set! arm (.intValue (cast Integer a))))))
              2 (let [v (.eval sel f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (when (nil? v) (throw (NullPointerException.)))
                  (let [a (.get keys (.name (cast Enum v)))]
                    (when (some? a) (set! arm (.intValue (cast Integer a))))))
              (let [v (.eval sel f)]
                (when (not (== (.-jump f) 0)) (return nil))
                (if (nil? v)
                    (if (< nullArm 0) (throw (NullPointerException.)) (set! arm nullArm))
                    (loop [^int i 0]
                      (when (< i (alength parms))
                        (let [p (aget pats i)
                              c (aget consts i)
                              ok (cond
                                   (some? p) (.match p f v)
                                   (instance? String c) (if (instance? Enum v)
                                                            (.equals c (.name (cast Enum v)))
                                                            (.equals c v))
                                   :else (and (some? c) (Util/equals c v)))
                              ok (and ok (or (nil? (aget guards i)) (.test (aget guards i) f)))]
                          (when (not (== (.-jump f) 0)) (return nil))
                          (if ok
                              (set! arm (aget parms i))
                              (recur (unchecked-inc-int i)))))))))
            (cond
              (>= arm 0) (aget arms arm)
              (>= dfltArm 0) (aget arms dfltArm)
              :else dflt)))
        (method ^:public eval [this ^Frame f]
          (let [n (.choose this f)] (if (some? n) (.eval n f) nil)))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [n (.choose this f)] (if (some? n) (.evalJ n f) 0)))
        (method ^:public evalD ^double [this ^Frame f]
          (let [n (.choose this f)] (if (some? n) (.evalD n f) 0.0)))
        (method ^:public exec ^void [this ^Frame f]
          (let [n (.choose this f)] (when (some? n) (.exec n f)))))

      ;; if-instance: the pattern matched against e's value binds, then the then branch
      (defclass ^:public ^:static IfInstance
        :extends Node
        (field ^:public ^:final ^Node e)
        (field ^:public ^:final ^Pat pat)
        (field ^:public ^:final ^Node a)
        (field ^:public ^:final ^Node b)
        (constructor ^:public [this ^char t ^Node e ^Pat pat ^Node a ^Node b]
          (set! (.-t this) t)
          (set! (.-e this) e)
          (set! (.-pat this) pat)
          (set! (.-a this) a)
          (set! (.-b this) b))
        (method ^:public branch ^Node [this ^Frame f]
          (let [v (.eval e f)]
            (when (not (== (.-jump f) 0)) (return nil))
            (let [ok (.match pat f v)]
              (cond (not (== (.-jump f) 0)) nil ok a :else b))))
        (method ^:public eval [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.eval n f) nil)))
        (method ^:public evalJ ^long [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.evalJ n f) 0)))
        (method ^:public evalD ^double [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.evalD n f) 0.0)))
        (method ^:public exec ^void [this ^Frame f]
          (let [n (.branch this f)] (when (some? n) (.exec n f))))
        (method ^:public test ^boolean [this ^Frame f]
          (let [n (.branch this f)] (if (some? n) (.test n f) false))))

      ;; ------------------------------------------------------------------------------------
      ;; lambdas and method references: an object of the lambda's class (implementing the
      ;; functional interface), its fields the captured values (the enclosing instance first)

      (defclass ^:public ^:static MakeLambda
        :extends Node
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^Node/1 caps)
        (constructor ^:public [this ^Klass k ^Node/1 caps]
          (set! (.-t this) \L)
          (set! (.-k this) k)
          (set! (.-caps this) caps))
        (method ^:public eval [this ^Frame f]
          (let [o (.alloc Rt/HOST k)]
            (loop [^int i 0]
              (when (< i (alength caps))
                (let [v (.eval (aget caps i) f)]
                  (when (not (== (.-jump f) 0)) (return nil))
                  (.setField Rt/HOST o i v))
                (recur (unchecked-inc-int i))))
            o)))

      ;; a record's equals (op 0), hashCode (1), toString (2), as ObjectMethods makes them:
      ;; the components' fields (index, type), names for toString
      (defclass ^:public ^:static RecordOp
        :extends Node
        (field ^:public ^:final ^int op)
        (field ^:public ^:final ^Klass k)
        (field ^:public ^:final ^int self)
        (field ^:public ^:final ^String/1 names)
        (field ^:public ^:final ^int/1 fields)
        (field ^:public ^:final ^char/1 types)
        (constructor ^:public [this ^char t ^int op ^Klass k ^int self ^String/1 names ^int/1 fields
                               ^char/1 types]
          (set! (.-t this) t)
          (set! (.-op this) op)
          (set! (.-k this) k)
          (set! (.-self this) self)
          (set! (.-names this) names)
          (set! (.-fields this) fields)
          (set! (.-types this) types))
        (method ^:public ^:static hash ^int [^char ty v]
          (switch ty
            \I (Integer/hashCode (.intValue (cast Integer v)))
            \J (Long/hashCode (.longValue (cast Long v)))
            \S (Short/hashCode (.shortValue (cast Short v)))
            \B (Byte/hashCode (.byteValue (cast Byte v)))
            \C (Character/hashCode (.charValue (cast Character v)))
            \Z (Boolean/hashCode (.booleanValue (cast Boolean v)))
            \F (Float/hashCode (.floatValue (cast Float v)))
            \D (Double/hashCode (.doubleValue (cast Double v)))
            (java.util.Objects/hashCode v)))
        (method ^:public ^:static same ^boolean [^char ty a b]
          (switch ty
            \F (== 0 (Float/compare (.floatValue (cast Float a)) (.floatValue (cast Float b))))
            \D (== 0 (Double/compare (.doubleValue (cast Double a)) (.doubleValue (cast Double b))))
            (\I \J \S \B \C \Z) (.equals a b)
            (java.util.Objects/equals a b)))
        (method ^:public eval [this ^Frame f]
          (let [o (aget (.-r f) self)]
            (switch op
              0 (let [x (aget (.-r f) (unchecked-inc-int self))
                      xk (if (nil? x) nil (.klassOf Rt/HOST x))]
                  (if (not (identical? xk k))
                      Boolean/FALSE
                      (loop [^int i 0]
                        (if (< i (alength fields))
                            (if (RecordOp/same (aget types i) (.field Rt/HOST o (aget fields i))
                                               (.field Rt/HOST x (aget fields i)))
                                (recur (unchecked-inc-int i))
                                Boolean/FALSE)
                            Boolean/TRUE))))
              1 (let [^:mutable ^int h 0]
                  (loop [^int i 0]
                    (when (< i (alength fields))
                      (set! h (unchecked-add-int (unchecked-multiply-int h 31)
                                                 (RecordOp/hash (aget types i) (.field Rt/HOST o (aget fields i)))))
                      (recur (unchecked-inc-int i))))
                  (Integer/valueOf h))
              (let [sb (StringBuilder.)
                    nm (.-name k)]
                (.append sb (.substring nm (unchecked-inc-int (Math/max (.lastIndexOf nm ".") (.lastIndexOf nm "$")))))
                (.append sb "[")
                (loop [^int i 0]
                  (when (< i (alength fields))
                    (when (> i 0) (.append sb ", "))
                    (.append sb (aget names i))
                    (.append sb "=")
                    (.append sb (String/valueOf (.field Rt/HOST o (aget fields i))))
                    (recur (unchecked-inc-int i))))
                (.append sb "]")
                (.toString sb))))))
))

  ;; The Go build's host (Compiler$CF$Host): interpreted classes are classes made at run time,
  ;; their objects Dyn (C2G-SPEC §5.12) or, for Clojure fn classes, CF$FnObj; the natives are
  ;; c2g's (arbace.c2g.dyncf, arbace/lang's c2g_cf.go)
  (c2g/add
    (defclass ^:public ^:static CFGo
      (defclass ^:public ^:static GoHost
        :extends CF$Host

        (method ^:public define ^Class [this ^CF$Klass k]
          (let [ifs (.-interfaces k)
                kis (.-ifaces k)
                all (new Class/1 (unchecked-add-int (alength ifs) (alength kis)))]
            (loop [^int i 0]
              (when (< i (alength kis))
                (aset all i (.-cls (aget kis i)))
                (recur (unchecked-inc-int i))))
            (System/arraycopy ifs 0 all (alength kis) (alength ifs))
            (CFGo/defineClass (.-name k)
                              (if (some? (.-sup k)) (.-cls (.-sup k)) (.-superClass k))
                              all (.-flags k)
                              (int (cond (.-iface k) 1 (.-fnClass k) 2 :else 0))
                              k)))

        (method ^:public alloc [this ^CF$Klass k]
          (if (.-fnClass k)
              (CF$FnObj. k (.clone (.-defaults k)))
              (CFGo/alloc (.-cls k) (.clone (.-defaults k)))))

        (method ^:public klassOf ^CF$Klass [this o]
          (if (instance? CF$FnObj o)
              (.-klass (cast CF$FnObj o))
              (cast CF$Klass (CFGo/klassOf o))))

        (method ^:public field [this o ^int i]
          (if (instance? CF$FnObj o)
              (aget (.-f (cast CF$FnObj o)) i)
              (Dyn/getField o i)))

        (method ^:public setField ^void [this o ^int i v]
          (if (instance? CF$FnObj o)
              (aset (.-f (cast CF$FnObj o)) i v)
              (Dyn/setField o i v)))

        (method ^:public checkCast [this ^Class c o] (Evaluator/checkCast c o))

        (method ^:public adaptFn [this ^Class c o] (Reflector/boxArg c o))

        (method ^:public objectMethod [this ^String name o arg]
          (switch name
            "toString" (java-str (.getName (.getClass o)) "@" (Integer/toHexString (.hashCode o)))
            "hashCode" (Integer/valueOf (System/identityHashCode o))
            (Boolean/valueOf (identical? o arg))))

        (method ^:public method ^void [this ^CF$Klass k ^CF$Meth m]
          (when-not (.-fnClass k)
            (CFGo/setMethod (.-cls k) (.-name m) (.-params m) (.-retClass m) (.-flags m)
                            (CF$MethodFn. m))))

        (method ^:public ctor ^void [this ^CF$Klass k ^CF$Meth m]
          (CFGo/addCtor (.-cls k) (.-params m) (.-flags m) (CF$CtorFn. m)))

        (method ^:public addField ^void [this ^CF$Klass k ^String name ^Class type ^int flags ^int i
                                         ^boolean isStatic]
          (if isStatic
              (CFGo/addStatic (.-cls k) name type flags (.-statics k) i
                              (anon AFn []
                                (method ^:public invoke [this] (.init k) nil)))
              (when-not (.-fnClass k)
                (CFGo/addField (.-cls k) name type flags i)))))

      ;; a class made at run time named name: kind 0 a class (its objects Dyn, whose fields
      ;; are the defaults' count; super Object or an interpreted class), 1 an interface, 2 a
      ;; Clojure fn's class (super AFunction or RestFn, objects CF$FnObj); klass is its CF$Klass
      (method ^:public ^:static ^:native defineClass ^Class [^String name ^Class super ^Class/1 interfaces
                                                             ^int flags ^int kind klass])
      ;; a new object of class c (kind 0): its fields the values
      (method ^:public ^:static ^:native alloc [^Class c ^Object/1 values])
      ;; the CF$Klass of o's class when o is an object of an interpreted class, else nil
      (method ^:public ^:static ^:native klassOf [o])
      ;; a method: static (a static entry of the member table) or virtual (Dyn's slot of its name
      ;; and descriptor, the class's method by key, and a member table entry calling the
      ;; receiver's method of that key)
      (method ^:public ^:static ^:native setMethod ^void [^Class c ^String name ^Class/1 params ^Class ret
                                                          ^int flags ^IFn impl])
      ;; a constructor: impl makes the object from the arguments
      (method ^:public ^:static ^:native addCtor ^void [^Class c ^Class/1 params ^int flags ^IFn impl])
      ;; an object's field i (of type type), and a static field (store[i], init run first)
      (method ^:public ^:static ^:native addField ^void [^Class c ^String name ^Class type ^int flags ^int i])
      (method ^:public ^:static ^:native addStatic ^void [^Class c ^String name ^Class type ^int flags
                                                          ^Object/1 store ^int i ^IFn init]))))

;; arbace.classes.native, the boundary to the class forms (SPEC §9.5), is loaded on first use as
;; on the JVM; in the Go build it and the analysis it requires are evaluated from their embedded
;; sources, without checking the macros' specs (the JVM loads them compiled, unchecked)
(c2g/variant Compiler
  (method ^:static classForms ^IFn [^String name]
    (when (nil? (Namespace/find (Symbol/intern "arbace.classes.native")))
      (let [chk RT/CHECK_SPECS]
        (set! RT/CHECK_SPECS false)
        (try
          (.invoke (RT/var "arbace.core" "require") (Symbol/intern "arbace.classes.native"))
          (finally (set! RT/CHECK_SPECS chk)))))
    (RT/var "arbace.classes.native" name)))
