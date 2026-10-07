;; // ASM: a very small and fast Java bytecode manipulation framework
;; // Copyright (c) 2000-2011 INRIA, France Telecom
;; // All rights reserved.
;; //
;; // Redistribution and use in source and binary forms, with or without
;; // modification, are permitted provided that the following conditions
;; // are met:
;; // 1. Redistributions of source code must retain the above copyright
;; //    notice, this list of conditions and the following disclaimer.
;; // 2. Redistributions in binary form must reproduce the above copyright
;; //    notice, this list of conditions and the following disclaimer in the
;; //    documentation and/or other materials provided with the distribution.
;; // 3. Neither the name of the copyright holders nor the names of its
;; //    contributors may be used to endorse or promote products derived from
;; //    this software without specific prior written permission.
;; //
;; // THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; // AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; // IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
;; // ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
;; // LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
;; // CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
;; // SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
;; // INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
;; // CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
;; // ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
;; // THE POSSIBILITY OF SUCH DAMAGE.
;;
;; Converted from clojure/asm/Handler.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final Handler
  (field ^:final ^Label startPc)

  (field ^:final ^Label endPc)

  (field ^:final ^Label handlerPc)

  (field ^:final ^int catchType)

  (field ^:final ^String catchTypeDescriptor)

  (field ^Handler nextHandler)

  (constructor [this ^:final ^Label startPc ^:final ^Label endPc ^:final ^Label handlerPc
                ^:final ^int catchType ^:final ^String catchTypeDescriptor]
    (set! (.-startPc this) startPc)
    (set! (.-endPc this) endPc)
    (set! (.-handlerPc this) handlerPc)
    (set! (.-catchType this) catchType)
    (set! (.-catchTypeDescriptor this) catchTypeDescriptor))

  (constructor [this ^:final ^Handler handler ^:final ^Label startPc ^:final ^Label endPc]
    (this. startPc
           endPc
           (.-handlerPc handler)
           (.-catchType handler)
           (.-catchTypeDescriptor handler))
    (set! (.-nextHandler this) (.-nextHandler handler)))

  (method ^:static removeRange ^Handler [^:final ^Handler firstHandler ^:final ^Label start
                                         ^:final ^Label end]
    (when (some? firstHandler)
      (let [rangeStart (.-bytecodeOffset start)
            rangeEnd (if (nil? end) Integer/MAX_VALUE (.-bytecodeOffset end))
            sentinel (Handler. firstHandler nil nil)
            ^:mutable lastHandler sentinel
            ^:mutable currentHandler firstHandler]
        (while (some? currentHandler)
          (let [handlerStart (.-bytecodeOffset (.-startPc currentHandler))
                handlerEnd (.-bytecodeOffset (.-endPc currentHandler))
                nextHandler (.-nextHandler currentHandler)]
            (set! (.-nextHandler currentHandler) nil)
            (cond
              (or (>= rangeStart handlerEnd) (<= rangeEnd handlerStart))
                (do
                  (set! (.-nextHandler lastHandler) currentHandler)
                  (set! lastHandler currentHandler))
              (<= rangeStart handlerStart)
                (when-not (>= rangeEnd handlerEnd)
                  (set! (.-nextHandler lastHandler)
                        (Handler. currentHandler end (.-endPc currentHandler)))
                  (set! lastHandler (.-nextHandler lastHandler)))
              (>= rangeEnd handlerEnd)
                (do
                  (set! (.-nextHandler lastHandler)
                        (Handler. currentHandler (.-startPc currentHandler) start))
                  (set! lastHandler (.-nextHandler lastHandler)))
              :else
                (do
                  (set! (.-nextHandler lastHandler)
                        (Handler. currentHandler (.-startPc currentHandler) start))
                  (set! lastHandler (.-nextHandler lastHandler))
                  (set! (.-nextHandler lastHandler)
                        (Handler. currentHandler end (.-endPc currentHandler)))
                  (set! lastHandler (.-nextHandler lastHandler))))
            (set! currentHandler nextHandler)))
        (.-nextHandler sentinel))))

  (method ^:static getExceptionTableLength ^int [^:final ^Handler firstHandler]
    (let [^:mutable ^int length 0
          ^:mutable handler firstHandler]
      (while (some? handler)
        (set! length (unchecked-inc-int length))
        (set! handler (.-nextHandler handler)))
      length))

  (method ^:static getExceptionTableSize ^int [^:final ^Handler firstHandler]
    (unchecked-add-int 2 (unchecked-multiply-int 8 (Handler/getExceptionTableLength firstHandler))))

  (method ^:static putExceptionTable ^void [^:final ^Handler firstHandler ^:final ^ByteVector output]
    (.putShort output (Handler/getExceptionTableLength firstHandler))
    (let [^:mutable handler firstHandler]
      (while (some? handler)
        (.putShort (.putShort (.putShort (.putShort output (.-bytecodeOffset (.-startPc handler)))
                                         (.-bytecodeOffset (.-endPc handler)))
                              (.-bytecodeOffset (.-handlerPc handler)))
                   (.-catchType handler))
        (set! handler (.-nextHandler handler))))))
