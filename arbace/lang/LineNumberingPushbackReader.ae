;; /**
;;  * Copyright (c) Rich Hickey. All rights reserved.
;;  * The use and distribution terms for this software are covered by the
;;  * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  * which can be found in the file epl-v10.html at the root of this distribution.
;;  * By using this software in any fashion, you are agreeing to be bound by
;;  * the terms of this license.
;;  * You must not remove this notice, or any other, from this software.
;;  */
;;
;; Converted from clojure/lang/LineNumberingPushbackReader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException LineNumberReader PushbackReader Reader))

(defclass ^:public LineNumberingPushbackReader
  :extends PushbackReader

  (field ^:private ^:static ^:final ^int newline (int \newline))

  (field ^:private ^boolean _atLineStart true)

  (field ^:private ^boolean _prev)

  (field ^:private ^int _columnNumber 1)

  (field ^:private ^StringBuilder sb nil)

  (constructor ^:public [this ^Reader r] (super. (LineNumberReader. r)))

  (constructor ^:public [this ^Reader r ^int size]
    (super. (LineNumberReader. r size)))

  (method ^:public getLineNumber ^int [this]
    (unchecked-add-int (.getLineNumber (cast LineNumberReader (.-in this))) 1))

  (method ^:public setLineNumber ^void [this ^int line]
    (.setLineNumber (cast LineNumberReader (.-in this)) (unchecked-subtract-int line 1)))

  (method ^:public captureString ^void [this]
    (set! (.-sb this) (StringBuilder.)))

  (method ^:public getString ^String [this]
    (when (some? sb) (let [ret (.toString sb)] (set! sb nil) ret)))

  (method ^:public getColumnNumber ^int [this] _columnNumber)

  (method ^:public read :throws [IOException] ^int [this]
    (let [c (.read super)]
      (set! _prev _atLineStart)
      (if (or (== c newline) (== c -1))
          (do (set! _atLineStart true) (set! _columnNumber 1))
          (do (set! _atLineStart false) (set! _columnNumber (unchecked-inc-int _columnNumber))))
      (when (and (some? sb) (not (== c -1))) (^[char] StringBuilder/.append sb (unchecked-char c)))
      c))

  (method ^:public unread :throws [IOException] ^void [this ^int c]
    (.unread super c)
    (set! _atLineStart _prev)
    (set! _columnNumber (unchecked-dec-int _columnNumber))
    (when (some? sb) (.deleteCharAt sb (unchecked-subtract-int (.length sb) 1))))

  (method ^:public readLine :throws [IOException] ^String [this]
    (let [c (.read this)
          ^:mutable ^String line nil]
      (switch c
        -1 (set! line nil)
        newline (set! line "")
        (let [first (^[char] String/valueOf (unchecked-char c))
              rest (.readLine (cast LineNumberReader (.-in this)))]
          (when (some? sb) (.append sb (java-str rest "\n")))
          (set! line (if (nil? rest) first (java-str first rest)))
          (set! _prev false)
          (set! _atLineStart true)
          (set! _columnNumber 1)))
      line))

  (method ^:public atLineStart ^boolean [this] _atLineStart))
