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
;; /* rich Jul 31, 2008 */
;;
;; Converted from clojure/lang/TransactionalHashMap.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util AbstractMap
                    AbstractSet
                    ArrayList
                    Collection
                    Collections
                    Iterator
                    Map
                    Map$Entry
                    Set)
        '(java.util.concurrent ConcurrentMap))

(defclass ^:public TransactionalHashMap
  :extends (AbstractMap K V)
  :implements [(ConcurrentMap K V)]
  :type-params [K V]

  (field ^:final ^Ref/1 bins)

  (method mapAt ^IPersistentMap [this ^int bin]
    (cast IPersistentMap (.deref (aget bins bin))))

  (method ^:final binFor ^int [this k]
    (let [^:mutable h (.hashCode k)]
      (set! h
            (bit-xor-int h
                         (bit-xor-int (unsigned-bit-shift-right-int h 20)
                                      (unsigned-bit-shift-right-int h 12))))
      (set! h
            (bit-xor-int h
                         (bit-xor-int (unsigned-bit-shift-right-int h 7)
                                      (unsigned-bit-shift-right-int h 4))))
      (unchecked-remainder-int h (alength bins))))

  (method entryAt ^Map$Entry [this k]
    (.entryAt (.mapAt this (.binFor this k)) k))

  (constructor ^:public [this] (this. 421))

  (constructor ^:public [this ^int nBins]
    (set! bins (new Ref/1 nBins))
    (loop [^int i 0]
      (when (< i nBins) (aset bins i (Ref. PersistentHashMap/EMPTY)) (recur (unchecked-inc-int i)))))

  (constructor ^:public [this ^{:tag (Map (? extends K) (? extends V))} m]
    (this. (.size m))
    (.putAll this m))

  (method ^:public size ^int [this]
    (let [^:mutable ^int n 0]
      (loop [^int i 0]
        (when (< i (alength bins))
          (set! n (unchecked-add-int n (.count (.mapAt this i))))
          (recur (unchecked-inc-int i))))
      n))

  (method ^:public isEmpty ^boolean [this] (== (.size this) 0))

  (method ^:public containsKey ^boolean [this k]
    (some? (.entryAt this k)))

  (method ^:public get ^V [this k]
    (let [e (.entryAt this k)] (when (some? e) (.getValue e))))

  (method ^:public put ^V [this ^K k ^V v]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ret (.valAt map k)]
      (.set r (.assoc map k v))
      ret))

  (method ^:public remove ^V [this k]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ret (.valAt map k)]
      (.set r (.without map k))
      ret))

  (method ^:public putAll ^void [this ^{:tag (Map (? extends K) (? extends V))} map]
    (loop [i (.iterator (.entrySet map))]
      (when (.hasNext i)
        (let [^{:tag (Map$Entry K V)} e (cast Map$Entry (.next i))]
          (.put this (.getKey e) (.getValue e))
          (recur i)))))

  (method ^:public clear ^void [this]
    (loop [^int i 0]
      (when (< i (alength bins))
        (let [r (aget bins i)
              map (cast IPersistentMap (.deref r))]
          (if (> (.count map) 0)
              (do (.set r PersistentHashMap/EMPTY) (recur (unchecked-inc-int i)))
              (recur (unchecked-inc-int i)))))))

  (method ^:public entrySet ^{:tag (Set (Map$Entry K V))} [this]
    (let [^{:tag (ArrayList (Map$Entry K V))} entries (ArrayList. (alength bins))]
      (loop [^int i 0]
        (when (< i (alength bins))
          (let [map (.mapAt this i)]
            (if (> (.count map) 0)
                (do (.addAll entries (cast Collection (RT/seq map))) (recur (unchecked-inc-int i)))
                (recur (unchecked-inc-int i))))))
      (anon (AbstractSet (Map$Entry K V)) []
        (method ^:public iterator ^Iterator [this]
          (.iterator (Collections/unmodifiableList entries)))

        (method ^:public size ^int [this] (.size entries)))))

  (method ^:public putIfAbsent ^V [this ^K k ^V v]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ^Map$Entry e (.entryAt map k)]
      (if (nil? e) (do (.set r (.assoc map k v)) nil) (.getValue e))))

  (method ^:public remove ^boolean [this k v]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ^Map$Entry e (.entryAt map k)]
      (if (and (some? e) (.equals (.getValue e) v)) (do (.set r (.without map k)) true) false)))

  (method ^:public replace ^boolean [this ^K k ^V oldv ^V newv]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ^Map$Entry e (.entryAt map k)]
      (if (and (some? e) (.equals (.getValue e) oldv)) (do (.set r (.assoc map k newv)) true) false)))

  (method ^:public replace ^V [this ^K k ^V v]
    (let [r (aget bins (.binFor this k))
          map (cast IPersistentMap (.deref r))
          ^Map$Entry e (.entryAt map k)]
      (when (some? e) (.set r (.assoc map k v)) (.getValue e)))))
