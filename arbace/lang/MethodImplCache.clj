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
;; /* rich Nov 8, 2009 */
;;
;; Converted from clojure/lang/MethodImplCache.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.util Map))

(defclass ^:public ^:final MethodImplCache
  (defclass ^:public ^:static Entry
    (field ^:public ^:final ^Class c)

    (field ^:public ^:final ^IFn fn)

    (constructor ^:public [this ^Class c ^IFn fn]
      (set! (.-c this) c)
      (set! (.-fn this) fn)))

  (field ^:public ^:final ^IPersistentMap protocol)

  (field ^:public ^:final ^Symbol sym)

  (field ^:public ^:final ^Keyword methodk)

  (field ^:public ^:final ^int shift)

  (field ^:public ^:final ^int mask)

  (field ^:public ^:final ^Object/1 table)

  (field ^:public ^:final ^Map map)

  (field ^Entry mre nil)

  (constructor ^:public [this ^Symbol sym ^IPersistentMap protocol ^Keyword methodk]
    (this. sym protocol methodk 0 0 RT/EMPTY_ARRAY))

  (constructor ^:public [this ^Symbol sym ^IPersistentMap protocol ^Keyword methodk ^int shift
                         ^int mask ^Object/1 table]
    (set! (.-sym this) sym)
    (set! (.-protocol this) protocol)
    (set! (.-methodk this) methodk)
    (set! (.-shift this) shift)
    (set! (.-mask this) mask)
    (set! (.-table this) table)
    (set! (.-map this) nil))

  (constructor ^:public [this ^Symbol sym ^IPersistentMap protocol ^Keyword methodk ^Map map]
    (set! (.-sym this) sym)
    (set! (.-protocol this) protocol)
    (set! (.-methodk this) methodk)
    (set! (.-shift this) 0)
    (set! (.-mask this) 0)
    (set! (.-table this) nil)
    (set! (.-map this) map))

  (method ^:public fnFor ^IFn [this ^Class c]
    (let [last mre]
      (if (and (some? last) (identical? (.-c last) c)) (.-fn last) (.findFnFor this c))))

  (method findFnFor ^IFn [this ^Class c]
    (if (some? map)
        (let [e (cast Entry (.get map c))] (set! mre e) (when (some? e) (.-fn e)))
        (let [idx (bit-shift-left-int (bit-and-int (bit-shift-right-int (Util/hash c) shift) mask)
                                      1)]
          (when (and (< idx (alength table)) (identical? (aget table idx) c))
            (let [e (cast Entry (aget table (unchecked-add-int idx 1)))]
              (set! mre e)
              (when (some? e) (.-fn e))))))))
