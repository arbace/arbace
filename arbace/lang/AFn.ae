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
;; /* rich Mar 25, 2006 4:05:37 PM */
;;
;; Converted from clojure/lang/AFn.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:abstract AFn
  :implements [IFn]

  (method ^:public call [this] (.invoke this))

  (method ^:public run ^void [this] (.invoke this))

  (method ^:public invoke [this] (.throwArity this 0))

  (method ^:public invoke [this arg1] (.throwArity this 1))

  (method ^:public invoke [this arg1 arg2] (.throwArity this 2))

  (method ^:public invoke [this arg1 arg2 arg3] (.throwArity this 3))

  (method ^:public invoke [this arg1 arg2 arg3 arg4] (.throwArity this 4))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5]
    (.throwArity this 5))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6]
    (.throwArity this 6))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7]
    (.throwArity this 7))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8]
    (.throwArity this 8))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9]
    (.throwArity this 9))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10]
    (.throwArity this 10))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11]
    (.throwArity this 11))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12]
    (.throwArity this 12))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13]
    (.throwArity this 13))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14]
    (.throwArity this 14))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15]
    (.throwArity this 15))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16]
    (.throwArity this 16))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17]
    (.throwArity this 17))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18]
    (.throwArity this 18))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19]
    (.throwArity this 19))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20]
    (.throwArity this 20))

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13
                           arg14 arg15 arg16 arg17 arg18 arg19 arg20 & ^Object/1 args]
    (.throwArity this 21))

  (method ^:public applyTo [this ^:mutable ^ISeq arglist]
    (AFn/applyToHelper this (Util/ret1 arglist (set! arglist nil))))

  (method ^:public ^:static applyToHelper [^IFn ifn ^:mutable ^ISeq arglist]
    (switch (RT/boundedLength arglist 20)
      0 (do (set! arglist nil) (.invoke ifn))
      1 (.invoke ifn (Util/ret1 (.first arglist) (set! arglist nil)))
      2
        (.invoke ifn
                 (.first arglist)
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      3
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      4
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      5
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      6
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      7
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      8
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      9
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      10
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      11
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      12
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      13
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      14
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      15
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      16
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      17
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      18
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      19
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      20
        (.invoke ifn
                 (.first arglist)
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (.first (set! arglist (.next arglist)))
                 (Util/ret1 (.first (set! arglist (.next arglist))) (set! arglist nil)))
      (.invoke ifn
               (.first arglist)
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (.first (set! arglist (.next arglist)))
               (RT/seqToArray (Util/ret1 (.next arglist) (set! arglist nil))))))

  (method ^:public throwArity [this ^int n]
    (let [name (.getName (.getClass this))] (throw (ArityException. n name)))))
