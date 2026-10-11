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
;; Converted from clojure/asm/signature/SignatureReader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.signature)

(import '(arbace.asm LimitExceededException))

(defclass ^:public SignatureReader
  (field ^:private ^:static ^:final ^int MAX_DEPTH 256)

  (field ^:private ^:final ^String signatureValue)

  (constructor ^:public [this ^:final ^String signature]
    (set! (.-signatureValue this) signature))

  (method ^:public accept ^void [this ^:final ^SignatureVisitor signatureVistor]
    (let [signature (.-signatureValue this)
          length (.length signature)
          ^:mutable ^int offset 0
          ^:mutable ^char currentChar (unchecked-char 0)]
      (if (== (.charAt signature 0) \<)
          (do
            (set! offset 2)
            (loop []
              (let [classBoundStartOffset (.indexOf signature \: offset)]
                (.visitFormalTypeParameter signatureVistor
                                           (.substring
                                             signature
                                             (unchecked-subtract-int offset 1)
                                             classBoundStartOffset))
                (set! offset (unchecked-add-int classBoundStartOffset 1))
                (set! currentChar (.charAt signature offset))
                (when (or (or (== currentChar \L) (== currentChar \[)) (== currentChar \T))
                  (set! offset
                        (SignatureReader/parseType
                          signature
                          offset
                          (.visitClassBound signatureVistor)
                          MAX_DEPTH)))
                (while (== (set! currentChar
                                 (.charAt signature
                                          (let [old-1 offset]
                                            (set! offset (unchecked-inc-int offset))
                                            old-1)))
                           \:)
                  (set! offset
                        (SignatureReader/parseType
                          signature
                          offset
                          (.visitInterfaceBound signatureVistor)
                          MAX_DEPTH))))
              (when-not (== currentChar \>) (recur))))
          (set! offset 0))
      (if (== (.charAt signature offset) \()
          (do
            (set! offset (unchecked-inc-int offset))
            (while (not (== (.charAt signature offset) \)))
              (set! offset
                    (SignatureReader/parseType signature
                                               offset
                                               (.visitParameterType signatureVistor)
                                               MAX_DEPTH)))
            (set! offset
                  (SignatureReader/parseType signature
                                             (unchecked-add-int offset 1)
                                             (.visitReturnType signatureVistor)
                                             MAX_DEPTH))
            (while (< offset length)
              (set! offset
                    (SignatureReader/parseType signature
                                               (unchecked-add-int offset 1)
                                               (.visitExceptionType signatureVistor)
                                               MAX_DEPTH))))
          (do
            (set! offset
                  (SignatureReader/parseType signature
                                             offset
                                             (.visitSuperclass signatureVistor)
                                             MAX_DEPTH))
            (while (< offset length)
              (set! offset
                    (SignatureReader/parseType signature
                                               offset
                                               (.visitInterface signatureVistor)
                                               MAX_DEPTH)))))))

  (method ^:public acceptType ^void [this ^:final ^SignatureVisitor signatureVisitor]
    (SignatureReader/parseType signatureValue 0 signatureVisitor MAX_DEPTH))

  (method ^:private ^:static parseType ^int [^:final ^String signature ^:mutable ^int offset
                                             ^:mutable ^SignatureVisitor visitor
                                             ^:final ^int depthLimit]
    (when (== depthLimit 0) (throw (LimitExceededException. "Too many nested type arguments")))
    (while true
      (let [^:mutable currentChar (.charAt signature
                                           (let [old-2 offset]
                                             (set! offset (unchecked-inc-int offset))
                                             old-2))]
        (switch currentChar
          (\Z \C \B \S \I \F \J \D \V) (do (.visitBaseType visitor currentChar) (return offset))
          \[ (set! visitor (.visitArrayType visitor))
          \T
            (let [endOffset (.indexOf signature \; offset)]
              (.visitTypeVariable visitor (.substring signature offset endOffset))
              (return (unchecked-add-int endOffset 1)))
          \L
            (let [^:mutable start offset
                  ^:mutable visited false
                  ^:mutable inner false]
              (while true
                (set! currentChar (.charAt signature offset))
                (set! offset (unchecked-inc-int offset))
                (cond
                  (or (== currentChar \.) (== currentChar \;))
                    (do
                      (when-not visited
                        (let [name (.substring signature start (unchecked-subtract-int offset 1))]
                          (if inner
                              (.visitInnerClassType visitor name)
                              (.visitClassType visitor name))))
                      (when (== currentChar \;) (.visitEnd visitor) (break))
                      (set! start offset)
                      (set! visited false)
                      (set! inner true))
                  (== currentChar \<)
                    (let [name (.substring signature start (unchecked-subtract-int offset 1))]
                      (if inner (.visitInnerClassType visitor name) (.visitClassType visitor name))
                      (set! visited true)
                      (while (not (== (set! currentChar (.charAt signature offset)) \>))
                        (switch currentChar
                          \*
                            (do
                              (set! offset (unchecked-inc-int offset))
                              (.visitTypeArgument visitor))
                          (\+ \-)
                            (set! offset
                                  (SignatureReader/parseType
                                    signature
                                    (unchecked-add-int offset 1)
                                    (.visitTypeArgument visitor currentChar)
                                    (unchecked-subtract-int depthLimit 1)))
                          (set! offset
                                (SignatureReader/parseType
                                  signature
                                  offset
                                  (.visitTypeArgument visitor \=)
                                  (unchecked-subtract-int depthLimit 1))))))))
              (return offset))
          (throw (IllegalArgumentException.)))))))
