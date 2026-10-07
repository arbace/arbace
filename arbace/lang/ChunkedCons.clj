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
;; /* rich May 25, 2009 */
;;
;; Converted from clojure/lang/ChunkedCons.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:final ChunkedCons
  :extends ASeq
  :implements [IChunkedSeq]

  (field ^:private ^:static ^:final ^long serialVersionUID 2773920188566401743)

  (field ^:final ^IChunk chunk)

  (field ^:final ^ISeq _more)

  (constructor [this ^IPersistentMap meta ^IChunk chunk ^ISeq more]
    (super. meta)
    (set! (.-chunk this) chunk)
    (set! (.-_more this) more))

  (constructor ^:public [this ^IChunk chunk ^ISeq more]
    (this. nil chunk more))

  (method ^:public withMeta ^Obj [this ^IPersistentMap meta]
    (if (not (identical? meta (.-_meta this))) (ChunkedCons. meta chunk _more) this))

  (method ^:public first [this] (.nth chunk 0))

  (method ^:public next ^ISeq [this]
    (if (> (.count chunk) 1) (ChunkedCons. (.dropFirst chunk) _more) (.chunkedNext this)))

  (method ^:public more ^ISeq [this]
    (cond
      (> (.count chunk) 1) (ChunkedCons. (.dropFirst chunk) _more)
      (nil? _more) PersistentList/EMPTY
      :else _more))

  (method ^:public chunkedFirst ^IChunk [this] chunk)

  (method ^:public chunkedNext ^ISeq [this] (.seq (.chunkedMore this)))

  (method ^:public chunkedMore ^ISeq [this]
    (if (nil? _more) PersistentList/EMPTY _more)))
