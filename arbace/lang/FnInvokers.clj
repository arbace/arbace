;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (https://opensource.org/license/epl-1-0)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; Converted from clojure/lang/FnInvokers.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public FnInvokers
  (method ^:static encodeInvokerType ^char [^Class c]
    (cond
      (.equals Long/TYPE c) \L
      (.equals Double/TYPE c) \D
      (.equals Integer/TYPE c) \I
      (.equals Short/TYPE c) \S
      (.equals Byte/TYPE c) \B
      (.equals Float/TYPE c) \F
      :else \O))

  (method ^:public ^:static invokeL ^long [^IFn f0]
    (if (instance? IFn$L f0) (.invokePrim (cast IFn$L f0)) (RT/longCast (.invoke f0))))

  (method ^:public ^:static invokeI ^int [^IFn f0]
    (if (instance? IFn$L f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$L f0)))
        (RT/intCast (.invoke f0))))

  (method ^:public ^:static invokeS ^short [^IFn f0]
    (if (instance? IFn$L f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$L f0)))
        (RT/shortCast (.invoke f0))))

  (method ^:public ^:static invokeB ^byte [^IFn f0]
    (if (instance? IFn$L f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$L f0)))
        (RT/byteCast (.invoke f0))))

  (method ^:public ^:static invokeD ^double [^IFn f0]
    (if (instance? IFn$D f0) (.invokePrim (cast IFn$D f0)) (RT/doubleCast (.invoke f0))))

  (method ^:public ^:static invokeF ^float [^IFn f0]
    (if (instance? IFn$D f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$D f0)))
        (RT/floatCast (.invoke f0))))

  (method ^:public ^:static invokeO [^IFn f0] (.invoke f0))

  (method ^:public ^:static invokeLL ^long [^IFn f0 ^long a]
    (if (instance? IFn$LL f0) (.invokePrim (cast IFn$LL f0) a) (RT/longCast (.invoke f0 a))))

  (method ^:public ^:static invokeDL ^long [^IFn f0 ^double a]
    (if (instance? IFn$DL f0) (.invokePrim (cast IFn$DL f0) a) (RT/longCast (.invoke f0 a))))

  (method ^:public ^:static invokeOL ^long [^IFn f0 a]
    (if (instance? IFn$OL f0) (.invokePrim (cast IFn$OL f0) a) (RT/longCast (.invoke f0 a))))

  (method ^:public ^:static invokeLI ^int [^IFn f0 ^long a]
    (if (instance? IFn$LL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$LL f0) a))
        (RT/intCast (.invoke f0 a))))

  (method ^:public ^:static invokeDI ^int [^IFn f0 ^double a]
    (if (instance? IFn$DL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$DL f0) a))
        (RT/intCast (.invoke f0 a))))

  (method ^:public ^:static invokeOI ^int [^IFn f0 a]
    (if (instance? IFn$OL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$OL f0) a))
        (RT/intCast (.invoke f0 a))))

  (method ^:public ^:static invokeLS ^short [^IFn f0 ^long a]
    (if (instance? IFn$LL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$LL f0) a))
        (RT/shortCast (.invoke f0 a))))

  (method ^:public ^:static invokeDS ^short [^IFn f0 ^double a]
    (if (instance? IFn$DL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$DL f0) a))
        (RT/shortCast (.invoke f0 a))))

  (method ^:public ^:static invokeOS ^short [^IFn f0 a]
    (if (instance? IFn$OL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$OL f0) a))
        (RT/shortCast (.invoke f0 a))))

  (method ^:public ^:static invokeLB ^byte [^IFn f0 ^long a]
    (if (instance? IFn$LL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$LL f0) a))
        (RT/byteCast (.invoke f0 a))))

  (method ^:public ^:static invokeDB ^byte [^IFn f0 ^double a]
    (if (instance? IFn$DL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$DL f0) a))
        (RT/byteCast (.invoke f0 a))))

  (method ^:public ^:static invokeOB ^byte [^IFn f0 a]
    (if (instance? IFn$OL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$OL f0) a))
        (RT/byteCast (.invoke f0 a))))

  (method ^:public ^:static invokeLD ^double [^IFn f0 ^long a]
    (if (instance? IFn$LD f0) (.invokePrim (cast IFn$LD f0) a) (RT/doubleCast (.invoke f0 a))))

  (method ^:public ^:static invokeDD ^double [^IFn f0 ^double a]
    (if (instance? IFn$DD f0) (.invokePrim (cast IFn$DD f0) a) (RT/doubleCast (.invoke f0 a))))

  (method ^:public ^:static invokeOD ^double [^IFn f0 a]
    (if (instance? IFn$OD f0) (.invokePrim (cast IFn$OD f0) a) (RT/doubleCast (.invoke f0 a))))

  (method ^:public ^:static invokeLF ^float [^IFn f0 ^long a]
    (if (instance? IFn$LD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$LD f0) a))
        (RT/floatCast (.invoke f0 a))))

  (method ^:public ^:static invokeDF ^float [^IFn f0 ^double a]
    (if (instance? IFn$DD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$DD f0) a))
        (RT/floatCast (.invoke f0 a))))

  (method ^:public ^:static invokeOF ^float [^IFn f0 a]
    (if (instance? IFn$OD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$OD f0) a))
        (RT/floatCast (.invoke f0 a))))

  (method ^:public ^:static invokeLO [^IFn f0 ^long a]
    (if (instance? IFn$LO f0) (.invokePrim (cast IFn$LO f0) a) (.invoke f0 a)))

  (method ^:public ^:static invokeDO [^IFn f0 ^double a]
    (if (instance? IFn$DO f0) (.invokePrim (cast IFn$DO f0) a) (.invoke f0 a)))

  (method ^:public ^:static invokeOO [^IFn f0 a] (.invoke f0 a))

  (method ^:public ^:static invokeLLL ^long [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLL f0) (.invokePrim (cast IFn$LLL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOL ^long [^IFn f0 ^long a b]
    (if (instance? IFn$LOL f0) (.invokePrim (cast IFn$LOL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLL ^long [^IFn f0 a ^long b]
    (if (instance? IFn$OLL f0) (.invokePrim (cast IFn$OLL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDL ^long [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDL f0) (.invokePrim (cast IFn$DDL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDL ^long [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDL f0) (.invokePrim (cast IFn$LDL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLL ^long [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLL f0) (.invokePrim (cast IFn$DLL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOL ^long [^IFn f0 a b]
    (if (instance? IFn$OOL f0) (.invokePrim (cast IFn$OOL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODL ^long [^IFn f0 a ^double b]
    (if (instance? IFn$ODL f0) (.invokePrim (cast IFn$ODL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOL ^long [^IFn f0 ^double a b]
    (if (instance? IFn$DOL f0) (.invokePrim (cast IFn$DOL f0) a b) (RT/longCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLI ^int [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$LLL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOI ^int [^IFn f0 ^long a b]
    (if (instance? IFn$LOL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$LOL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLI ^int [^IFn f0 a ^long b]
    (if (instance? IFn$OLL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$OLL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDI ^int [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$DDL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDI ^int [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$LDL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLI ^int [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$DLL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOI ^int [^IFn f0 a b]
    (if (instance? IFn$OOL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$OOL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODI ^int [^IFn f0 a ^double b]
    (if (instance? IFn$ODL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$ODL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOI ^int [^IFn f0 ^double a b]
    (if (instance? IFn$DOL f0)
        (^[long] RT/intCast (.invokePrim (cast IFn$DOL f0) a b))
        (RT/intCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLS ^short [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$LLL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOS ^short [^IFn f0 ^long a b]
    (if (instance? IFn$LOL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$LOL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLS ^short [^IFn f0 a ^long b]
    (if (instance? IFn$OLL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$OLL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDS ^short [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$DDL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDS ^short [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$LDL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLS ^short [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$DLL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOS ^short [^IFn f0 a b]
    (if (instance? IFn$OOL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$OOL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODS ^short [^IFn f0 a ^double b]
    (if (instance? IFn$ODL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$ODL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOS ^short [^IFn f0 ^double a b]
    (if (instance? IFn$DOL f0)
        (^[long] RT/shortCast (.invokePrim (cast IFn$DOL f0) a b))
        (RT/shortCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLB ^byte [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$LLL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOB ^byte [^IFn f0 ^long a b]
    (if (instance? IFn$LOL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$LOL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLB ^byte [^IFn f0 a ^long b]
    (if (instance? IFn$OLL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$OLL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDB ^byte [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$DDL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDB ^byte [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$LDL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLB ^byte [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$DLL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOB ^byte [^IFn f0 a b]
    (if (instance? IFn$OOL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$OOL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODB ^byte [^IFn f0 a ^double b]
    (if (instance? IFn$ODL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$ODL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOB ^byte [^IFn f0 ^double a b]
    (if (instance? IFn$DOL f0)
        (^[long] RT/byteCast (.invokePrim (cast IFn$DOL f0) a b))
        (RT/byteCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLD ^double [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLD f0) (.invokePrim (cast IFn$LLD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOD ^double [^IFn f0 ^long a b]
    (if (instance? IFn$LOD f0) (.invokePrim (cast IFn$LOD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLD ^double [^IFn f0 a ^long b]
    (if (instance? IFn$OLD f0) (.invokePrim (cast IFn$OLD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDD ^double [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDD f0) (.invokePrim (cast IFn$DDD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDD ^double [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDD f0) (.invokePrim (cast IFn$LDD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLD ^double [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLD f0) (.invokePrim (cast IFn$DLD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOD ^double [^IFn f0 a b]
    (if (instance? IFn$OOD f0) (.invokePrim (cast IFn$OOD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODD ^double [^IFn f0 a ^double b]
    (if (instance? IFn$ODD f0) (.invokePrim (cast IFn$ODD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOD ^double [^IFn f0 ^double a b]
    (if (instance? IFn$DOD f0) (.invokePrim (cast IFn$DOD f0) a b) (RT/doubleCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLF ^float [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$LLD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLOF ^float [^IFn f0 ^long a b]
    (if (instance? IFn$LOD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$LOD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOLF ^float [^IFn f0 a ^long b]
    (if (instance? IFn$OLD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$OLD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDDF ^float [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$DDD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLDF ^float [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$LDD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDLF ^float [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$DLD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeOOF ^float [^IFn f0 a b]
    (if (instance? IFn$OOD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$OOD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeODF ^float [^IFn f0 a ^double b]
    (if (instance? IFn$ODD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$ODD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeDOF ^float [^IFn f0 ^double a b]
    (if (instance? IFn$DOD f0)
        (^[double] RT/floatCast (.invokePrim (cast IFn$DOD f0) a b))
        (RT/floatCast (.invoke f0 a b))))

  (method ^:public ^:static invokeLLO [^IFn f0 ^long a ^long b]
    (if (instance? IFn$LLO f0) (.invokePrim (cast IFn$LLO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeLOO [^IFn f0 ^long a b]
    (if (instance? IFn$LOO f0) (.invokePrim (cast IFn$LOO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeOLO [^IFn f0 a ^long b]
    (if (instance? IFn$OLO f0) (.invokePrim (cast IFn$OLO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeDDO [^IFn f0 ^double a ^double b]
    (if (instance? IFn$DDO f0) (.invokePrim (cast IFn$DDO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeLDO [^IFn f0 ^long a ^double b]
    (if (instance? IFn$LDO f0) (.invokePrim (cast IFn$LDO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeDLO [^IFn f0 ^double a ^long b]
    (if (instance? IFn$DLO f0) (.invokePrim (cast IFn$DLO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeOOO [^IFn f0 a b] (.invoke f0 a b))

  (method ^:public ^:static invokeODO [^IFn f0 a ^double b]
    (if (instance? IFn$ODO f0) (.invokePrim (cast IFn$ODO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeDOO [^IFn f0 ^double a b]
    (if (instance? IFn$DOO f0) (.invokePrim (cast IFn$DOO f0) a b) (.invoke f0 a b)))

  (method ^:public ^:static invokeOOOO [^IFn f0 a b c] (.invoke f0 a b c))

  (method ^:public ^:static invokeOOOOO [^IFn f0 a b c d]
    (.invoke f0 a b c d))

  (method ^:public ^:static invokeOOOOOO [^IFn f0 a b c d e]
    (.invoke f0 a b c d e))

  (method ^:public ^:static invokeOOOOOOO [^IFn f0 a b c d e f]
    (.invoke f0 a b c d e f))

  (method ^:public ^:static invokeOOOOOOOO [^IFn f0 a b c d e f g]
    (.invoke f0 a b c d e f g))

  (method ^:public ^:static invokeOOOOOOOOO [^IFn f0 a b c d e f g h]
    (.invoke f0 a b c d e f g h))

  (method ^:public ^:static invokeOOOOOOOOOO [^IFn f0 a b c d e f g h i]
    (.invoke f0 a b c d e f g h i))

  (method ^:public ^:static invokeOOOOOOOOOOO [^IFn f0 a b c d e f g h i j]
    (.invoke f0 a b c d e f g h i j)))
