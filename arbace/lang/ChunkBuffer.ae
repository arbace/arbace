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
;; /* rich May 26, 2009 */
;;
;; Converted from clojure/lang/ChunkBuffer.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:final ChunkBuffer
  :implements [Counted]

  (field ^Object/1 buffer)

  (field ^int end)

  (constructor ^:public [this ^int capacity]
    (set! buffer (new Object/1 capacity))
    (set! end 0))

  (method ^:public add ^void [this o]
    (aset buffer (let [old-1 end] (set! end (unchecked-inc-int end)) old-1) o))

  (method ^:public chunk ^IChunk [this]
    (let [ret (ArrayChunk. buffer 0 end)] (set! buffer nil) ret))

  (method ^:public count ^int [this] end))
