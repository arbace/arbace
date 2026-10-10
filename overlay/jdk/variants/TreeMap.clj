;; Go-build variant of java.util.TreeMap (C2G-SPEC §4.6; doc/go/JAVA-BASE.md, amendment JB2):
;; read by c2g only. TreeMap builds a balanced tree from sorted data in buildFromSorted, whose
;; parameters include the ObjectInputStream of deserialization, outside the closed world: the
;; two methods do not exist in Go, and the copy constructor from a SortedMap, clone, putAll of a
;; SortedMap into an empty map and TreeSet's addAll of a SortedSet threw. The variant adds the
;; same two methods without the stream (jdk26u's code with its `it == null` branch, the
;; deserializing one, left out) and calls them from those four places, whose code is otherwise
;; jdk26u's (a replacing member's parameter tags are written as j2c writes the original's, by
;; which the variant finds it). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant TreeMap
  (constructor ^:public [this ^{:tag (SortedMap K (? extends V))} m]
    (set! comparator (.comparator m))
    (.buildFromIterator this (.size m) (.iterator (.entrySet m)) nil))

  (method ^:public putAll ^void [this ^{:tag (Map (? extends K) (? extends V))} map]
    (let [mapSize (.size map)]
      (when (and (and (== size 0) (not (== mapSize 0))) (instance? SortedMap map))
        (when (Objects/equals comparator (.comparator (cast SortedMap map)))
          (set! modCount (unchecked-inc-int modCount))
          (.buildFromIterator this mapSize (.iterator (.entrySet map)) nil)
          (return)))
      (.putAll super map)))

  (method ^:public clone [this]
    (let [^:mutable ^TreeMap clone nil]
      (try
        (set! clone (cast TreeMap (.clone super)))
        (catch CloneNotSupportedException e (throw (InternalError. e))))
      (set! (.-root clone) nil)
      (set! (.-size clone) 0)
      (set! (.-modCount clone) 0)
      (set! (.-entrySet clone) nil)
      (set! (.-navigableKeySet clone) nil)
      (set! (.-descendingMap clone) nil)
      (.buildFromIterator clone size (.iterator (.entrySet this)) nil)
      clone))

  (method addAllForTreeSet ^void [this ^{:tag (SortedSet (? extends K))} set ^V defaultVal]
    (.buildFromIterator this (.size set) (.iterator set) defaultVal))

  (c2g/add
    (method ^:private buildFromIterator ^void [this ^int size ^Iterator it ^Object defaultVal]
      (set! (.-size this) size)
      (set! root
            (.buildFromIterator this 0 0 (unchecked-subtract-int size 1) (TreeMap/computeRedLevel size)
                                it defaultVal))))

  (c2g/add
    (method ^:private ^:final buildFromIterator ^TreeMap$Entry [this ^int level ^int lo ^int hi
                                                              ^int redLevel ^Iterator it
                                                              ^Object defaultVal]
      (when-not (< hi lo)
        (let [mid (unsigned-bit-shift-right-int (unchecked-add-int lo hi) 1)
              ^:mutable ^TreeMap$Entry left nil]
          (when (< lo mid)
            (set! left (.buildFromIterator this (unchecked-add-int level 1) lo
                                           (unchecked-subtract-int mid 1) redLevel it defaultVal)))
          (let [^:mutable ^Object key nil
                ^:mutable ^Object value nil]
            (if (nil? defaultVal)
                (let [^Map$Entry entry (cast Map$Entry (.next it))]
                  (set! key (.getKey entry))
                  (set! value (.getValue entry)))
                (do (set! key (.next it)) (set! value defaultVal)))
            (let [^TreeMap$Entry middle (TreeMap$Entry. key value nil)]
              (when (== level redLevel) (set! (.-color middle) RED))
              (when (some? left) (set! (.-left middle) left) (set! (.-parent left) middle))
              (when (< mid hi)
                (let [^TreeMap$Entry right (.buildFromIterator this (unchecked-add-int level 1)
                                                               (unchecked-add-int mid 1) hi
                                                               redLevel it defaultVal)]
                  (set! (.-right middle) right)
                  (set! (.-parent right) middle)))
              middle)))))))
