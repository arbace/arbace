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
;; Converted from clojure/lang/Cycle.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public Cycle
  :extends ASeq
  :implements [IReduce IPending]

  (field ^:private ^:static ^:final ^long serialVersionUID 4007270937279943908)

  (field ^:private ^:final ^ISeq all)

  (field ^:private ^:final ^ISeq prev)

  (field ^:private ^:volatile ^ISeq _current)

  (field ^:private ^:volatile ^ISeq _next)

  (constructor ^:private [this ^ISeq all ^ISeq prev ^ISeq current]
    (set! (.-all this) all)
    (set! (.-prev this) prev)
    (set! (.-_current this) current))

  (constructor ^:private [this ^IPersistentMap meta ^ISeq all ^ISeq prev ^ISeq current ^ISeq next]
    (super. meta)
    (set! (.-all this) all)
    (set! (.-prev this) prev)
    (set! (.-_current this) current)
    (set! (.-_next this) next))

  (method ^:public ^:static create ^ISeq [^ISeq vals]
    (if (nil? vals) PersistentList/EMPTY (Cycle. vals nil vals)))

  (method ^:private current ^ISeq [this]
    (when (nil? _current)
      (let [current (.next prev)] (set! _current (if (nil? current) all current))))
    _current)

  (method ^:public isRealized ^boolean [this] (some? _current))

  (method ^:public first [this] (.first (.current this)))

  (method ^:public next ^ISeq [this]
    (when (nil? _next) (set! _next (Cycle. all (.current this) nil)))
    _next)

  (method ^:public withMeta ^Cycle [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (Cycle. meta all prev _current _next)))

  (method ^:public reduce [this ^IFn f]
    (let [^:mutable s (.current this)
          ^:mutable ret (.first s)]
      (while true
        (set! s (.next s))
        (when (nil? s) (set! s all))
        (set! ret (.invoke f ret (.first s)))
        (when (RT/isReduced ret) (return (.deref (cast IDeref ret)))))))

  (method ^:public reduce [this ^IFn f start]
    (let [^:mutable ret start
          ^:mutable s (.current this)]
      (while true
        (set! ret (.invoke f ret (.first s)))
        (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
        (set! s (.next s))
        (when (nil? s) (set! s all)))))

  (method ^:public hashCode ^int [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public hasheq ^int [this]
    (throw (UnsupportedOperationException.))))
