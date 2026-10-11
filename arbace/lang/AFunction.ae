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
;; /* rich Dec 16, 2008 */
;;
;; Converted from clojure/lang/AFunction.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io Serializable)
        '(java.util Comparator))

(defclass ^:public ^:abstract AFunction
  :extends AFn
  :implements [IObj Comparator Fn Serializable]

  (field ^:private ^:static ^:final ^long serialVersionUID 4469383498184457675)

  (field ^:public ^:volatile ^MethodImplCache __methodImplCache)

  (method ^:public meta ^IPersistentMap [this] nil)

  (method ^:public withMeta ^IObj [this ^:final ^IPersistentMap meta]
    (if (nil? meta)
        this
        (anon RestFn []
          (method ^:protected doInvoke [this args]
            (.applyTo AFunction/this (cast ISeq args)))

          (method ^:public meta ^IPersistentMap [this] meta)

          (method ^:public withMeta ^IObj [this ^IPersistentMap newMeta]
            (if (identical? meta newMeta) this (.withMeta AFunction/this newMeta)))

          (method ^:public getRequiredArity ^int [this] 0))))

  (method ^:public compare ^int [this o1 o2]
    (let [o (.invoke this o1 o2)]
      (if (instance? Boolean o)
          (cond (RT/booleanCast o) -1 (RT/booleanCast (.invoke this o2 o1)) 1 :else 0)
          (let [n (cast Number o)] (.intValue n))))))
