;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich Jun 19, 2006 */
;;
;; Converted from clojure/lang/ArraySeq.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.lang.reflect Array))

(defclass ^:public ArraySeq
  :extends ASeq
  :implements [IndexedSeq IReduce]

  (field ^:private ^:static ^:final ^long serialVersionUID -9069152683729302290)

  (field ^:public ^:final ^Object/1 array)

  (field ^:final ^int i)

  (method ^:public ^:static create ^ArraySeq [] nil)

  (method ^:public ^:static create ^ArraySeq [& ^Object/1 array]
    (when-not (or (nil? array) (== (alength array) 0)) (ArraySeq. array 0)))

  (method ^:static createFromObject ^ISeq [array]
    (when-not (or (nil? array) (== (Array/getLength array) 0))
      (let [aclass (.getClass array)]
        (cond
          (identical? aclass int/1) (ArraySeq_int. nil (cast int/1 array) 0)
          (identical? aclass float/1) (ArraySeq_float. nil (cast float/1 array) 0)
          (identical? aclass double/1) (ArraySeq_double. nil (cast double/1 array) 0)
          (identical? aclass long/1) (ArraySeq_long. nil (cast long/1 array) 0)
          (identical? aclass byte/1) (ArraySeq_byte. nil (cast byte/1 array) 0)
          (identical? aclass char/1) (ArraySeq_char. nil (cast char/1 array) 0)
          (identical? aclass short/1) (ArraySeq_short. nil (cast short/1 array) 0)
          (identical? aclass boolean/1) (ArraySeq_boolean. nil (cast boolean/1 array) 0)
          :else (ArraySeq. array 0)))))

  (constructor [this array ^int i]
    (set! (.-i this) i)
    (set! (.-array this) (cast Object/1 array)))

  (constructor [this ^IPersistentMap meta array ^int i]
    (super. meta)
    (set! (.-i this) i)
    (set! (.-array this) (cast Object/1 array)))

  (method ^:public first [this] (when (some? array) (aget array i)))

  (method ^:public next ^ISeq [this]
    (when (and (some? array) (< (unchecked-add-int i 1) (alength array)))
      (ArraySeq. array (unchecked-add-int i 1))))

  (method ^:public count ^int [this]
    (if (some? array) (unchecked-subtract-int (alength array) i) 0))

  (method ^:public index ^int [this] i)

  (method ^:public withMeta ^ArraySeq [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (ArraySeq. meta array i)))

  (method ^:public reduce [this ^IFn f]
    (when (some? array)
      (let [^:mutable ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret)))

  (method ^:public reduce [this ^IFn f start]
    (when (some? array)
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret))))

  (method ^:public indexOf ^int [this o]
    (when (some? array)
      (loop [^int j i]
        (if (< j (alength array))
            (if (Util/equals o (aget array j))
                (return (unchecked-subtract-int j i))
                (recur (unchecked-inc-int j)))
            nil)))
    -1)

  (method ^:public lastIndexOf ^int [this o]
    (when (some? array)
      (if (nil? o)
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (nil? (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (.equals o (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
    -1)

  (method ^:public toArray ^Object/1 [this]
    (let [sz (unchecked-subtract-int (alength (.-array this)) (.-i this))
          ret (new Object/1 sz)]
      (System/arraycopy (.-array this) i ret 0 sz)
      ret))

  (defclass ^:public ^:static ArraySeq_int
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^int/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^int/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_int. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_int [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_int. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Number o)
        (let [k (.intValue (cast Number o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== k (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      -1)

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Number o)
        (let [k (.intValue (cast Number o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== k (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      -1))

  (defclass ^:public ^:static ArraySeq_float
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^float/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^float/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (^[float] Numbers/num (aget array i)))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_float. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_float [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_float. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (^[float] Numbers/num (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (^[float] Numbers/num (aget array x))))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (^[float] Numbers/num (aget array i)))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (^[float] Numbers/num (aget array x))))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Number o)
        (let [f (.floatValue (cast Number o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== f (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      -1)

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Number o)
        (let [f (.floatValue (cast Number o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== f (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      -1))

  (defclass ^:public ^:static ArraySeq_double
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^double/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^double/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_double. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_double [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_double. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Number o)
        (let [d (.doubleValue (cast Number o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== d (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      -1)

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Number o)
        (let [d (.doubleValue (cast Number o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== d (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      -1))

  (defclass ^:public ^:static ArraySeq_long
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^long/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^long/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (^[long] Numbers/num (aget array i)))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_long. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_long [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_long. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (^[long] Numbers/num (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (^[long] Numbers/num (aget array x))))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (^[long] Numbers/num (aget array i)))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (^[long] Numbers/num (aget array x))))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Number o)
        (let [l (.longValue (cast Number o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== l (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      -1)

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Number o)
        (let [l (.longValue (cast Number o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== l (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      -1))

  (defclass ^:public ^:static ArraySeq_byte
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^byte/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^byte/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_byte. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_byte [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_byte. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Byte o)
        (let [b (.byteValue (cast Byte o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== b (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j i]
              (if (< j (alength array))
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-inc-int j)))
                  nil))
            -1)))

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Byte o)
        (let [b (.byteValue (cast Byte o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== b (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j (unchecked-subtract-int (alength array) 1)]
              (if (>= j i)
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-dec-int j)))
                  nil))
            -1))))

  (defclass ^:public ^:static ArraySeq_char
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^char/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^char/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_char. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_char [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_char. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Character o)
        (let [c (.charValue (cast Character o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== c (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j i]
              (if (< j (alength array))
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-inc-int j)))
                  nil))
            -1)))

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Character o)
        (let [c (.charValue (cast Character o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== c (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j (unchecked-subtract-int (alength array) 1)]
              (if (>= j i)
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-dec-int j)))
                  nil))
            -1))))

  (defclass ^:public ^:static ArraySeq_short
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^short/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^short/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_short. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_short [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_short. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Short o)
        (let [s (.shortValue (cast Short o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (== s (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j i]
              (if (< j (alength array))
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-inc-int j)))
                  nil))
            -1)))

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Short o)
        (let [s (.shortValue (cast Short o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (== s (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j (unchecked-subtract-int (alength array) 1)]
              (if (>= j i)
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-dec-int j)))
                  nil))
            -1))))

  (defclass ^:public ^:static ArraySeq_boolean
    :extends ASeq
    :implements [IndexedSeq IReduce]

    (field ^:public ^:final ^boolean/1 array)

    (field ^:final ^int i)

    (constructor [this ^IPersistentMap meta ^boolean/1 array ^int i]
      (super. meta)
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public first [this] (aget array i))

    (method ^:public next ^ISeq [this]
      (when (< (unchecked-add-int i 1) (alength array))
        (ArraySeq_boolean. (.meta this) array (unchecked-add-int i 1))))

    (method ^:public count ^int [this]
      (unchecked-subtract-int (alength array) i))

    (method ^:public index ^int [this] i)

    (method ^:public withMeta ^ArraySeq_boolean [this ^IPersistentMap meta]
      (if (identical? (.meta this) meta) this (ArraySeq_boolean. meta array i)))

    (method ^:public reduce [this ^IFn f]
      (let [^:mutable ^Object ret (aget array i)]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (set! ret (.invoke f ret (aget array x)))
            (if (RT/isReduced ret)
                (return (.deref (cast IDeref ret)))
                (recur (unchecked-inc-int x)))))
        ret))

    (method ^:public reduce [this ^IFn f start]
      (let [^:mutable ret (.invoke f start (aget array i))]
        (loop [^int x (unchecked-add-int i 1)]
          (when (< x (alength array))
            (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
            (set! ret (.invoke f ret (aget array x)))
            (recur (unchecked-inc-int x))))
        (if (RT/isReduced ret) (.deref (cast IDeref ret)) ret)))

    (method ^:public indexOf ^int [this o]
      (when (instance? Boolean o)
        (let [b (.booleanValue (cast Boolean o))]
          (loop [^int j i]
            (if (< j (alength array))
                (if (= b (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-inc-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j i]
              (if (< j (alength array))
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-inc-int j)))
                  nil))
            -1)))

    (method ^:public lastIndexOf ^int [this o]
      (when (instance? Boolean o)
        (let [b (.booleanValue (cast Boolean o))]
          (loop [^int j (unchecked-subtract-int (alength array) 1)]
            (if (>= j i)
                (if (= b (aget array j))
                    (return (unchecked-subtract-int j i))
                    (recur (unchecked-dec-int j)))
                nil))))
      (if (nil? o)
          -1
          (do
            (loop [^int j (unchecked-subtract-int (alength array) 1)]
              (if (>= j i)
                  (if (.equals o (aget array j))
                      (return (unchecked-subtract-int j i))
                      (recur (unchecked-dec-int j)))
                  nil))
            -1)))))
