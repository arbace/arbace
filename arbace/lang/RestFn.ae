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
;; Converted from clojure/lang/RestFn.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:abstract RestFn
  :extends AFunction

  (field ^:private ^:static ^:final ^long serialVersionUID -4319097849151802009)

  (method ^:public ^:abstract getRequiredArity ^int [this])

  (method ^:protected doInvoke [this args] nil)

  (method ^:protected doInvoke [this arg1 args] nil)

  (method ^:protected doInvoke [this arg1 arg2 args] nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 args] nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 args] nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 args] nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 arg16 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 arg16 arg17 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 arg16 arg17 arg18 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 arg16 arg17 arg18 arg19 args]
    nil)

  (method ^:protected doInvoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                                arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20 args]
    nil)

  (method ^:public applyTo [this ^:mutable ^ISeq args]
    (if (<= (RT/boundedLength args (.getRequiredArity this)) (.getRequiredArity this))
        (AFn/applyToHelper this (Util/ret1 args (set! args nil)))
        (do
          (switch (.getRequiredArity this)
            0 (return (.doInvoke this (Util/ret1 args (set! args nil))))
            1 (return (.doInvoke this (.first args) (Util/ret1 (.next args) (set! args nil))))
            2
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            3
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            4
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            5
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            6
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            7
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            8
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            9
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            10
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            11
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            12
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            13
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            14
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            15
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            16
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            17
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            18
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            19
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil))))
            20
              (return (.doInvoke this
                                 (.first args)
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (.first (set! args (.next args)))
                                 (Util/ret1 (.next args) (set! args nil)))))
          (.throwArity this -1))))

  (method ^:public invoke [this]
    (switch (.getRequiredArity this) 0 (.doInvoke this nil) (.throwArity this 0)))

  (method ^:public invoke [this ^:mutable arg1]
    (switch (.getRequiredArity this)
      0 (.doInvoke this (ArraySeq/create (new Object/1 [(Util/ret1 arg1 (set! arg1 nil))])))
      1 (.doInvoke this (Util/ret1 arg1 (set! arg1 nil)) nil)
      (.throwArity this 1)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil)) (Util/ret1 arg2 (set! arg2 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg2 (set! arg2 nil))])))
      2 (.doInvoke this (Util/ret1 arg1 (set! arg1 nil)) (Util/ret1 arg2 (set! arg2 nil)) nil)
      (.throwArity this 2)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil)) (Util/ret1 arg3 (set! arg3 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg3 (set! arg3 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   nil)
      (.throwArity this 3)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil)) (Util/ret1 arg4 (set! arg4 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg4 (set! arg4 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   nil)
      (.throwArity this 4)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil)) (Util/ret1 arg5 (set! arg5 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg5 (set! arg5 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   nil)
      (.throwArity this 5)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil)) (Util/ret1 arg6 (set! arg6 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg6 (set! arg6 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   nil)
      (.throwArity this 6)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil)) (Util/ret1 arg7 (set! arg7 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg7 (set! arg7 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   nil)
      (.throwArity this 7)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil)) (Util/ret1 arg8 (set! arg8 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg8 (set! arg8 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   nil)
      (.throwArity this 8)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil)) (Util/ret1 arg9 (set! arg9 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg9 (set! arg9 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   nil)
      (.throwArity this 9)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil)) (Util/ret1 arg10 (set! arg10 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg10 (set! arg10 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   nil)
      (.throwArity this 10)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil)) (Util/ret1 arg11 (set! arg11 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg11 (set! arg11 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   nil)
      (.throwArity this 11)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil)) (Util/ret1 arg12 (set! arg12 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg12 (set! arg12 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   nil)
      (.throwArity this 12)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil)) (Util/ret1 arg13 (set! arg13 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg13 (set! arg13 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   nil)
      (.throwArity this 13)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil)) (Util/ret1 arg14 (set! arg14 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg14 (set! arg14 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   nil)
      (.throwArity this 14)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil)) (Util/ret1 arg15 (set! arg15 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg15 (set! arg15 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   nil)
      (.throwArity this 15)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg15 (set! arg15 nil)) (Util/ret1 arg16 (set! arg16 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg16 (set! arg16 nil))])))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   nil)
      (.throwArity this 16)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg16 (set! arg16 nil)) (Util/ret1 arg17 (set! arg17 nil))])))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg17 (set! arg17 nil))])))
      17
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   nil)
      (.throwArity this 17)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))])))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg17 (set! arg17 nil)) (Util/ret1 arg18 (set! arg18 nil))])))
      17
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg18 (set! arg18 nil))])))
      18
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   nil)
      (.throwArity this 18)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      1
        (let [^ISeq packed PersistentList/EMPTY]
          (.doInvoke this
                     (Util/ret1 arg1 (set! arg1 nil))
                     (ArraySeq/create
                       (new Object/1
                            [(Util/ret1 arg2 (set! arg2 nil))
                             (Util/ret1 arg3 (set! arg3 nil))
                             (Util/ret1 arg4 (set! arg4 nil))
                             (Util/ret1 arg5 (set! arg5 nil))
                             (Util/ret1 arg6 (set! arg6 nil))
                             (Util/ret1 arg7 (set! arg7 nil))
                             (Util/ret1 arg8 (set! arg8 nil))
                             (Util/ret1 arg9 (set! arg9 nil))
                             (Util/ret1 arg10 (set! arg10 nil))
                             (Util/ret1 arg11 (set! arg11 nil))
                             (Util/ret1 arg12 (set! arg12 nil))
                             (Util/ret1 arg13 (set! arg13 nil))
                             (Util/ret1 arg14 (set! arg14 nil))
                             (Util/ret1 arg15 (set! arg15 nil))
                             (Util/ret1 arg16 (set! arg16 nil))
                             (Util/ret1 arg17 (set! arg17 nil))
                             (Util/ret1 arg18 (set! arg18 nil))
                             (Util/ret1 arg19 (set! arg19 nil))]))))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))])))
      17
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg18 (set! arg18 nil)) (Util/ret1 arg19 (set! arg19 nil))])))
      18
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg19 (set! arg19 nil))])))
      19
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (Util/ret1 arg19 (set! arg19 nil))
                   nil)
      (.throwArity this 19)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19 ^:mutable arg20]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg1 (set! arg1 nil))
                           (Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg2 (set! arg2 nil))
                           (Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg3 (set! arg3 nil))
                           (Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg4 (set! arg4 nil))
                           (Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg5 (set! arg5 nil))
                           (Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg6 (set! arg6 nil))
                           (Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg7 (set! arg7 nil))
                           (Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg8 (set! arg8 nil))
                           (Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg9 (set! arg9 nil))
                           (Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg10 (set! arg10 nil))
                           (Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg11 (set! arg11 nil))
                           (Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg12 (set! arg12 nil))
                           (Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg13 (set! arg13 nil))
                           (Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg14 (set! arg14 nil))
                           (Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg15 (set! arg15 nil))
                           (Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg16 (set! arg16 nil))
                           (Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg17 (set! arg17 nil))
                           (Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      17
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg18 (set! arg18 nil))
                           (Util/ret1 arg19 (set! arg19 nil))
                           (Util/ret1 arg20 (set! arg20 nil))])))
      18
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (ArraySeq/create
                     (new Object/1
                          [(Util/ret1 arg19 (set! arg19 nil)) (Util/ret1 arg20 (set! arg20 nil))])))
      19
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (Util/ret1 arg19 (set! arg19 nil))
                   (ArraySeq/create (new Object/1 [(Util/ret1 arg20 (set! arg20 nil))])))
      20
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (Util/ret1 arg19 (set! arg19 nil))
                   (Util/ret1 arg20 (set! arg20 nil))
                   nil)
      (.throwArity this 20)))

  (method ^:public invoke [this ^:mutable arg1 ^:mutable arg2 ^:mutable arg3 ^:mutable arg4
                           ^:mutable arg5 ^:mutable arg6 ^:mutable arg7 ^:mutable arg8
                           ^:mutable arg9 ^:mutable arg10 ^:mutable arg11 ^:mutable arg12
                           ^:mutable arg13 ^:mutable arg14 ^:mutable arg15 ^:mutable arg16
                           ^:mutable arg17 ^:mutable arg18 ^:mutable arg19 ^:mutable arg20 &
                           ^Object/1 args]
    (switch (.getRequiredArity this)
      0
        (.doInvoke this
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg1 (set! arg1 nil))
                     (Util/ret1 arg2 (set! arg2 nil))
                     (Util/ret1 arg3 (set! arg3 nil))
                     (Util/ret1 arg4 (set! arg4 nil))
                     (Util/ret1 arg5 (set! arg5 nil))
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      1
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg2 (set! arg2 nil))
                     (Util/ret1 arg3 (set! arg3 nil))
                     (Util/ret1 arg4 (set! arg4 nil))
                     (Util/ret1 arg5 (set! arg5 nil))
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      2
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg3 (set! arg3 nil))
                     (Util/ret1 arg4 (set! arg4 nil))
                     (Util/ret1 arg5 (set! arg5 nil))
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      3
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg4 (set! arg4 nil))
                     (Util/ret1 arg5 (set! arg5 nil))
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      4
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg5 (set! arg5 nil))
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      5
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg6 (set! arg6 nil))
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      6
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg7 (set! arg7 nil))
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      7
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg8 (set! arg8 nil))
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      8
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg9 (set! arg9 nil))
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      9
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg10 (set! arg10 nil))
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      10
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg11 (set! arg11 nil))
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      11
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg12 (set! arg12 nil))
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      12
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg13 (set! arg13 nil))
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      13
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg14 (set! arg14 nil))
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      14
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg15 (set! arg15 nil))
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      15
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg16 (set! arg16 nil))
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      16
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg17 (set! arg17 nil))
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      17
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg18 (set! arg18 nil))
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      18
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (^[Object/1 Object/1] RestFn/ontoArrayPrepend
                     args
                     (Util/ret1 arg19 (set! arg19 nil))
                     (Util/ret1 arg20 (set! arg20 nil))))
      19
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (Util/ret1 arg19 (set! arg19 nil))
                   (RestFn/ontoArrayPrepend args (Util/ret1 arg20 (set! arg20 nil))))
      20
        (.doInvoke this
                   (Util/ret1 arg1 (set! arg1 nil))
                   (Util/ret1 arg2 (set! arg2 nil))
                   (Util/ret1 arg3 (set! arg3 nil))
                   (Util/ret1 arg4 (set! arg4 nil))
                   (Util/ret1 arg5 (set! arg5 nil))
                   (Util/ret1 arg6 (set! arg6 nil))
                   (Util/ret1 arg7 (set! arg7 nil))
                   (Util/ret1 arg8 (set! arg8 nil))
                   (Util/ret1 arg9 (set! arg9 nil))
                   (Util/ret1 arg10 (set! arg10 nil))
                   (Util/ret1 arg11 (set! arg11 nil))
                   (Util/ret1 arg12 (set! arg12 nil))
                   (Util/ret1 arg13 (set! arg13 nil))
                   (Util/ret1 arg14 (set! arg14 nil))
                   (Util/ret1 arg15 (set! arg15 nil))
                   (Util/ret1 arg16 (set! arg16 nil))
                   (Util/ret1 arg17 (set! arg17 nil))
                   (Util/ret1 arg18 (set! arg18 nil))
                   (Util/ret1 arg19 (set! arg19 nil))
                   (Util/ret1 arg20 (set! arg20 nil))
                   (ArraySeq/create args))
      (.throwArity this 21)))

  (method ^:protected ^:static ontoArrayPrepend ^ISeq [^Object/1 array & ^Object/1 args]
    (let [^:mutable ^ISeq ret (ArraySeq/create array)]
      (loop [^int i (unchecked-subtract-int (alength args) 1)]
        (when (>= i 0) (set! ret (RT/cons (aget args i) ret)) (recur (unchecked-dec-int i))))
      ret))

  (method ^:protected ^:static findKey ^ISeq [key ^:mutable ^ISeq args]
    (while (some? args)
      (when (identical? key (.first args)) (return (.next args)))
      (set! args (RT/next args))
      (set! args (RT/next args)))
    nil))
