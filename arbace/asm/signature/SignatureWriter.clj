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
;; Converted from clojure/asm/signature/SignatureWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.signature)

(import '(arbace.asm Opcodes))

(defclass ^:public SignatureWriter
  :extends SignatureVisitor

  (field ^:private ^:final ^StringBuilder stringBuilder)

  (field ^:private ^boolean hasFormals)

  (field ^:private ^boolean hasParameters)

  (field ^:private ^int argumentStack 1)

  (constructor ^:public [this] (this. (StringBuilder.)))

  (constructor ^:private [this ^:final ^StringBuilder stringBuilder]
    (super. Opcodes/ASM9)
    (set! (.-stringBuilder this) stringBuilder))

  (method ^:public visitFormalTypeParameter ^void [this ^:final ^String name]
    (when-not hasFormals (set! hasFormals true) (^[char] StringBuilder/.append stringBuilder \<))
    (^[char] StringBuilder/.append (.append stringBuilder name) \:))

  (method ^:public visitClassBound ^SignatureVisitor [this] this)

  (method ^:public visitInterfaceBound ^SignatureVisitor [this]
    (^[char] StringBuilder/.append stringBuilder \:)
    this)

  (method ^:public visitSuperclass ^SignatureVisitor [this]
    (.endFormals this)
    this)

  (method ^:public visitInterface ^SignatureVisitor [this] this)

  (method ^:public visitParameterType ^SignatureVisitor [this]
    (.endFormals this)
    (when-not hasParameters
      (set! hasParameters true)
      (^[char] StringBuilder/.append stringBuilder \())
    this)

  (method ^:public visitReturnType ^SignatureVisitor [this]
    (.endFormals this)
    (when-not hasParameters (^[char] StringBuilder/.append stringBuilder \())
    (^[char] StringBuilder/.append stringBuilder \))
    this)

  (method ^:public visitExceptionType ^SignatureVisitor [this]
    (^[char] StringBuilder/.append stringBuilder \^)
    this)

  (method ^:public visitBaseType ^void [this ^:final ^char descriptor]
    (^[char] StringBuilder/.append stringBuilder descriptor))

  (method ^:public visitTypeVariable ^void [this ^:final ^String name]
    (^[char] StringBuilder/.append (.append (^[char] StringBuilder/.append stringBuilder \T) name)
                                   \;))

  (method ^:public visitArrayType ^SignatureVisitor [this]
    (^[char] StringBuilder/.append stringBuilder \[)
    this)

  (method ^:public visitClassType ^void [this ^:final ^String name]
    (.append (^[char] StringBuilder/.append stringBuilder \L) name)
    (set! argumentStack (bit-shift-left-int argumentStack 1)))

  (method ^:public visitInnerClassType ^void [this ^:final ^String name]
    (.endArguments this)
    (.append (^[char] StringBuilder/.append stringBuilder \.) name)
    (set! argumentStack (bit-shift-left-int argumentStack 1)))

  (method ^:public visitTypeArgument ^void [this]
    (when (== (bit-and-int argumentStack 1) 0)
      (set! argumentStack (bit-or-int argumentStack 1))
      (^[char] StringBuilder/.append stringBuilder \<))
    (^[char] StringBuilder/.append stringBuilder \*))

  (method ^:public visitTypeArgument ^SignatureVisitor [this ^:final ^char wildcard]
    (when (== (bit-and-int argumentStack 1) 0)
      (set! argumentStack (bit-or-int argumentStack 1))
      (^[char] StringBuilder/.append stringBuilder \<))
    (when-not (== wildcard \=) (^[char] StringBuilder/.append stringBuilder wildcard))
    (if (== (bit-and-int argumentStack (bit-shift-left-int 1 31)) 0)
        ^SignatureVisitor this
        ^SignatureVisitor (SignatureWriter. stringBuilder)))

  (method ^:public visitEnd ^void [this]
    (.endArguments this)
    (^[char] StringBuilder/.append stringBuilder \;))

  (method ^:public toString ^String [this] (.toString stringBuilder))

  (method ^:private endFormals ^void [this]
    (when hasFormals (set! hasFormals false) (^[char] StringBuilder/.append stringBuilder \>)))

  (method ^:private endArguments ^void [this]
    (when (== (bit-and-int argumentStack 1) 1) (^[char] StringBuilder/.append stringBuilder \>))
    (set! argumentStack (unsigned-bit-shift-right-int argumentStack 1))))
