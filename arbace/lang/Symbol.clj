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
;; /* rich Mar 25, 2006 11:42:47 AM */
;;
;; Converted from clojure/lang/Symbol.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io ObjectStreamException Serializable))

(defclass ^:public Symbol
  :extends AFn
  :implements [IObj Comparable Named Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID 1191039485148212259)

  (field ^:final ^String ns)

  (field ^:final ^String name)

  (field ^:private ^int _hasheq)

  (field ^:final ^IPersistentMap _meta)

  (field ^:transient ^String _str)

  (method ^:public toString ^String [this]
    (when (nil? _str) (if (some? ns) (set! _str (java-str ns "/" name)) (set! _str name)))
    _str)

  (method ^:public getNamespace ^String [this] ns)

  (method ^:public getName ^String [this] name)

  (method ^:public ^:static create ^Symbol [^String ns ^String name]
    (Symbol/intern ns name))

  (method ^:public ^:static create ^Symbol [^String nsname]
    (Symbol/intern nsname))

  (method ^:public ^:static intern ^Symbol [^String ns ^String name]
    (Symbol. ns name))

  (method ^:public ^:static intern ^Symbol [^String nsname]
    (let [i (.indexOf nsname \/)]
      (if (or (== i -1) (.equals nsname "/"))
          (Symbol. nil nsname)
          (Symbol. (.substring nsname 0 i) (.substring nsname (unchecked-add-int i 1))))))

  (constructor ^:private [this ^String ns_interned ^String name_interned]
    (set! (.-name this) name_interned)
    (set! (.-ns this) ns_interned)
    (set! (.-_meta this) nil))

  (method ^:public equals ^boolean [this o]
    (cond
      (identical? this o) true
      (not (instance? Symbol o)) false
      :else
        (let [symbol (cast Symbol o)]
          (and (Util/equals ns (.-ns symbol)) (.equals name (.-name symbol))))))

  (method ^:public hashCode ^int [this]
    (Util/hashCombine (.hashCode name) (Util/hash ns)))

  (method ^:public hasheq ^int [this]
    (when (== _hasheq 0)
      (set! _hasheq (Util/hashCombine (Murmur3/hashUnencodedChars name) (Util/hash ns))))
    _hasheq)

  (method ^:public withMeta ^IObj [this ^IPersistentMap meta]
    (if (identical? (.meta this) meta) this (Symbol. meta ns name)))

  (constructor ^:private [this ^IPersistentMap meta ^String ns ^String name]
    (set! (.-name this) name)
    (set! (.-ns this) ns)
    (set! (.-_meta this) meta))

  (method ^:public compareTo ^int [this o]
    (let [s (cast Symbol o)]
      (cond
        (.equals this o) 0
        (and (nil? (.-ns this)) (some? (.-ns s))) -1
        :else
          (do
            (when (some? (.-ns this))
              (when (nil? (.-ns s)) (return 1))
              (let [nsc (.compareTo (.-ns this) (.-ns s))] (when-not (== nsc 0) (return nsc))))
            (.compareTo (.-name this) (.-name s))))))

  (method ^:private readResolve :throws [ObjectStreamException] [this]
    (Symbol/intern ns name))

  (method ^:public invoke [this obj] (RT/get obj this))

  (method ^:public invoke [this obj notFound] (RT/get obj this notFound))

  (method ^:public meta ^IPersistentMap [this] _meta))
