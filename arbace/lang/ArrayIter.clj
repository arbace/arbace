;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *    the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; Converted from clojure/lang/ArrayIter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.lang.reflect Array)
        '(java.util Iterator NoSuchElementException))

(defclass ^:public ArrayIter
  :implements [Iterator]

  (field ^:final ^Object/1 array)

  (field ^int i)

  (field ^:public ^:static ^Iterator EMPTY_ITERATOR
    (anon Iterator []
      (method ^:public hasNext ^boolean [this] false)

      (method ^:public next [this] (throw (NoSuchElementException.)))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException. "remove() not supported")))))

  (method ^:public ^:static create ^Iterator [] EMPTY_ITERATOR)

  (method ^:public ^:static create ^Iterator [& ^Object/1 array]
    (if (or (nil? array) (== (alength array) 0)) EMPTY_ITERATOR (ArrayIter. array 0)))

  (method ^:public ^:static createFromObject ^Iterator [array]
    (if (or (nil? array) (== (Array/getLength array) 0))
        EMPTY_ITERATOR
        (let [aclass (.getClass array)]
          (cond
            (identical? aclass int/1) (ArrayIter_int. (cast int/1 array) 0)
            (identical? aclass float/1) (ArrayIter_float. (cast float/1 array) 0)
            (identical? aclass double/1) (ArrayIter_double. (cast double/1 array) 0)
            (identical? aclass long/1) (ArrayIter_long. (cast long/1 array) 0)
            (identical? aclass byte/1) (ArrayIter_byte. (cast byte/1 array) 0)
            (identical? aclass char/1) (ArrayIter_char. (cast char/1 array) 0)
            (identical? aclass short/1) (ArrayIter_short. (cast short/1 array) 0)
            (identical? aclass boolean/1) (ArrayIter_boolean. (cast boolean/1 array) 0)
            :else (ArrayIter. array 0)))))

  (constructor [this array ^int i]
    (set! (.-i this) i)
    (set! (.-array this) (cast Object/1 array)))

  (method ^:public hasNext ^boolean [this]
    (and (some? array) (< i (alength array))))

  (method ^:public next [this]
    (if (and (some? array) (< i (alength array)))
        (let [i-1 i] (set! i (unchecked-inc-int i)) (aget array i-1))
        (throw (NoSuchElementException.))))

  (method ^:public remove ^void [this]
    (throw (UnsupportedOperationException. "remove() not supported")))

  (defclass ^:public ^:static ArrayIter_int
    :implements [(Iterator Long)]

    (field ^:final ^int/1 array)

    (field ^int i)

    (constructor [this ^int/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Long [this]
      (if (and (some? array) (< i (alength array)))
          (Long/valueOf (aget array (let [old-2 i] (set! i (unchecked-inc-int i)) old-2)))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_float
    :implements [(Iterator Double)]

    (field ^:final ^float/1 array)

    (field ^int i)

    (constructor [this ^float/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Double [this]
      (if (and (some? array) (< i (alength array)))
          (Double/valueOf (aget array (let [old-3 i] (set! i (unchecked-inc-int i)) old-3)))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_double
    :implements [(Iterator Double)]

    (field ^:final ^double/1 array)

    (field ^int i)

    (constructor [this ^double/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Double [this]
      (if (and (some? array) (< i (alength array)))
          (let [i-4 i] (set! i (unchecked-inc-int i)) (aget array i-4))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_long
    :implements [(Iterator Long)]

    (field ^:final ^long/1 array)

    (field ^int i)

    (constructor [this ^long/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Long [this]
      (if (and (some? array) (< i (alength array)))
          (Long/valueOf (aget array (let [old-5 i] (set! i (unchecked-inc-int i)) old-5)))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_byte
    :implements [(Iterator Byte)]

    (field ^:final ^byte/1 array)

    (field ^int i)

    (constructor [this ^byte/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Byte [this]
      (if (and (some? array) (< i (alength array)))
          (let [i-6 i] (set! i (unchecked-inc-int i)) (aget array i-6))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_char
    :implements [(Iterator Character)]

    (field ^:final ^char/1 array)

    (field ^int i)

    (constructor [this ^char/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Character [this]
      (if (and (some? array) (< i (alength array)))
          (let [i-7 i] (set! i (unchecked-inc-int i)) (aget array i-7))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_short
    :implements [(Iterator Long)]

    (field ^:final ^short/1 array)

    (field ^int i)

    (constructor [this ^short/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Long [this]
      (if (and (some? array) (< i (alength array)))
          (Long/valueOf (aget array (let [old-8 i] (set! i (unchecked-inc-int i)) old-8)))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported"))))

  (defclass ^:public ^:static ArrayIter_boolean
    :implements [(Iterator Boolean)]

    (field ^:final ^boolean/1 array)

    (field ^int i)

    (constructor [this ^boolean/1 array ^int i]
      (set! (.-array this) array)
      (set! (.-i this) i))

    (method ^:public hasNext ^boolean [this]
      (and (some? array) (< i (alength array))))

    (method ^:public next ^Boolean [this]
      (if (and (some? array) (< i (alength array)))
          (Boolean/valueOf (aget array (let [old-9 i] (set! i (unchecked-inc-int i)) old-9)))
          (throw (NoSuchElementException.))))

    (method ^:public remove ^void [this]
      (throw (UnsupportedOperationException. "remove() not supported")))))
