;; // ASM: a very small and fast Java bytecode manipulation framework
;; // Copyright (c) 2000-2011 INRIA, France Telecom
;; // All rights reserved.
;; //
;; // Redistribution and use in source and binary forms, with or without
;; // modification, are permitted provided that the following conditions
;; // are met:
;; // 1. Redistributions of source code must retain the above copyright
;; // notice, this list of conditions and the following disclaimer.
;; // 2. Redistributions in binary form must reproduce the above copyright
;; // notice, this list of conditions and the following disclaimer in the
;; // documentation and/or other materials provided with the distribution.
;; // 3. Neither the name of the copyright holders nor the names of its
;; // contributors may be used to endorse or promote products derived from
;; // this software without specific prior written permission.
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
;; Converted from clojure/asm/signature/SignatureVisitor.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.signature)

(import '(arbace.asm Opcodes))

(defclass ^:public ^:abstract SignatureVisitor
  (field ^:public ^:static ^:final ^char EXTENDS \+)

  (field ^:public ^:static ^:final ^char SUPER \-)

  (field ^:public ^:static ^:final ^char INSTANCEOF \=)

  (field ^:protected ^:final ^int api)

  (constructor ^:protected [this ^:final ^int api]
    (when (and (and (and (and (and (and (not (== api Opcodes/ASM9)) (not (== api Opcodes/ASM8)))
                                   (not (== api Opcodes/ASM7)))
                              (not (== api Opcodes/ASM6)))
                         (not (== api Opcodes/ASM5)))
                    (not (== api Opcodes/ASM4)))
               (not (== api Opcodes/ASM10_EXPERIMENTAL)))
      (throw (IllegalArgumentException. (java-str "Unsupported api " api))))
    (set! (.-api this) api))

  (method ^:public visitFormalTypeParameter ^void [this ^:final ^String name])

  (method ^:public visitClassBound ^SignatureVisitor [this] this)

  (method ^:public visitInterfaceBound ^SignatureVisitor [this] this)

  (method ^:public visitSuperclass ^SignatureVisitor [this] this)

  (method ^:public visitInterface ^SignatureVisitor [this] this)

  (method ^:public visitParameterType ^SignatureVisitor [this] this)

  (method ^:public visitReturnType ^SignatureVisitor [this] this)

  (method ^:public visitExceptionType ^SignatureVisitor [this] this)

  (method ^:public visitBaseType ^void [this ^:final ^char descriptor])

  (method ^:public visitTypeVariable ^void [this ^:final ^String name])

  (method ^:public visitArrayType ^SignatureVisitor [this] this)

  (method ^:public visitClassType ^void [this ^:final ^String name])

  (method ^:public visitInnerClassType ^void [this ^:final ^String name])

  (method ^:public visitTypeArgument ^void [this])

  (method ^:public visitTypeArgument ^SignatureVisitor [this ^:final ^char wildcard]
    this)

  (method ^:public visitEnd ^void [this]))
