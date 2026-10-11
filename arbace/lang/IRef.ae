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
;; /* rich Nov 18, 2007 */
;;
;; Converted from clojure/lang/IRef.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:interface IRef
  :extends [IDeref]

  (method setValidator ^void [this ^IFn vf])

  (method getValidator ^IFn [this])

  (method getWatches ^IPersistentMap [this])

  (method addWatch ^IRef [this key ^IFn callback])

  (method removeWatch ^IRef [this key]))
