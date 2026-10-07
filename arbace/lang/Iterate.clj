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
;; Converted from clojure/lang/Iterate.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException ObjectInputStream ObjectOutputStream))

(defclass ^:public Iterate
  :extends ASeq
  :implements [IReduce IPending]

  (field ^:private ^:static ^:final ^long serialVersionUID -78221705247226450)

  (field ^:private ^:static ^:final UNREALIZED_SEED (Object.))

  (field ^:private ^:final ^IFn f)

  (field ^:private ^:final prevSeed)

  (field ^:private ^:volatile _seed)

  (field ^:private ^:volatile ^ISeq _next)

  (constructor ^:private [this ^IFn f prevSeed seed]
    (set! (.-f this) f)
    (set! (.-prevSeed this) prevSeed)
    (set! (.-_seed this) seed))

  (constructor ^:private [this ^IPersistentMap meta ^IFn f prevSeed seed ^ISeq next]
    (super. meta)
    (set! (.-f this) f)
    (set! (.-prevSeed this) prevSeed)
    (set! (.-_seed this) seed)
    (set! (.-_next this) next))

  (method ^:public ^:static create ^ISeq [^IFn f seed]
    (Iterate. f nil seed))

  (method ^:public isRealized ^boolean [this]
    (not (identical? _seed UNREALIZED_SEED)))

  (method ^:public first [this]
    (when (identical? _seed UNREALIZED_SEED) (set! _seed (.invoke f prevSeed)))
    _seed)

  (method ^:public next ^ISeq [this]
    (when (nil? _next) (set! _next (Iterate. f (.first this) UNREALIZED_SEED)))
    _next)

  (method ^:public withMeta ^Iterate [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (Iterate. meta f prevSeed _seed _next)))

  (method ^:public reduce [this ^IFn rf]
    (let [first (.first this)
          ^:mutable ret first
          ^:mutable v (.invoke f first)]
      (while true
        (set! ret (.invoke rf ret v))
        (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
        (set! v (.invoke f v)))))

  (method ^:public reduce [this ^IFn rf start]
    (let [^:mutable ret start
          ^:mutable v (.first this)]
      (while true
        (set! ret (.invoke rf ret v))
        (when (RT/isReduced ret) (return (.deref (cast IDeref ret))))
        (set! v (.invoke f v)))))

  (method ^:public hashCode ^int [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public hasheq ^int [this]
    (throw (UnsupportedOperationException.)))

  (method ^:private writeObject :throws [IOException] ^void [this ^ObjectOutputStream out]
    (throw (UnsupportedOperationException.)))

  (method ^:private readObject :throws [IOException ClassNotFoundException] ^void [this
                                                                                   ^ObjectInputStream in]
    (throw (UnsupportedOperationException.))))
