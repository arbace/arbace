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
;; Converted from clojure/asm/MethodVisitor.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:abstract MethodVisitor
  (field ^:private ^:static ^:final ^String REQUIRES_ASM5 "This feature requires ASM5")

  (field ^:protected ^:final ^int api)

  (field ^:protected ^MethodVisitor mv)

  (constructor ^:protected [this ^:final ^int api] (this. api nil))

  (constructor ^:protected [this ^:final ^int api ^:final ^MethodVisitor methodVisitor]
    (when (and (and (and (and (and (and (not (== api Opcodes/ASM9)) (not (== api Opcodes/ASM8)))
                                   (not (== api Opcodes/ASM7)))
                              (not (== api Opcodes/ASM6)))
                         (not (== api Opcodes/ASM5)))
                    (not (== api Opcodes/ASM4)))
               (not (== api Opcodes/ASM10_EXPERIMENTAL)))
      (throw (IllegalArgumentException. (java-str "Unsupported api " api))))
    (when (== api Opcodes/ASM10_EXPERIMENTAL) (Constants/checkAsmExperimental this))
    (set! (.-api this) api)
    (set! (.-mv this) methodVisitor))

  (method ^:public getDelegate ^MethodVisitor [this] mv)

  (method ^:public visitParameter ^void [this ^:final ^String name ^:final ^int access]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv) (.visitParameter mv name access)))

  (method ^:public visitAnnotationDefault ^AnnotationVisitor [this]
    (when (some? mv) (.visitAnnotationDefault mv)))

  (method ^:public visitAnnotation ^AnnotationVisitor [this ^:final ^String descriptor
                                                       ^:final ^boolean visible]
    (when (some? mv) (.visitAnnotation mv descriptor visible)))

  (method ^:public visitTypeAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv) (.visitTypeAnnotation mv typeRef typePath descriptor visible)))

  (method ^:public visitAnnotableParameterCount ^void [this ^:final ^int parameterCount
                                                       ^:final ^boolean visible]
    (when (some? mv) (.visitAnnotableParameterCount mv parameterCount visible)))

  (method ^:public visitParameterAnnotation ^AnnotationVisitor [this ^:final ^int parameter
                                                                ^:final ^String descriptor
                                                                ^:final ^boolean visible]
    (when (some? mv) (.visitParameterAnnotation mv parameter descriptor visible)))

  (method ^:public visitAttribute ^void [this ^:final ^Attribute attribute]
    (when (some? mv) (.visitAttribute mv attribute)))

  (method ^:public visitCode ^void [this]
    (when (some? mv) (.visitCode mv)))

  (method ^:public visitFrame ^void [this ^:final ^int type ^:final ^int numLocal
                                     ^:final ^Object/1 local ^:final ^int numStack
                                     ^:final ^Object/1 stack]
    (when (some? mv) (.visitFrame mv type numLocal local numStack stack)))

  (method ^:public visitInsn ^void [this ^:final ^int opcode]
    (when (some? mv) (.visitInsn mv opcode)))

  (method ^:public visitIntInsn ^void [this ^:final ^int opcode ^:final ^int operand]
    (when (some? mv) (.visitIntInsn mv opcode operand)))

  (method ^:public visitVarInsn ^void [this ^:final ^int opcode ^:final ^int varIndex]
    (when (some? mv) (.visitVarInsn mv opcode varIndex)))

  (method ^:public visitTypeInsn ^void [this ^:final ^int opcode ^:final ^String type]
    (when (some? mv) (.visitTypeInsn mv opcode type)))

  (method ^:public visitFieldInsn ^void [this ^:final ^int opcode ^:final ^String owner
                                         ^:final ^String name ^:final ^String descriptor]
    (when (some? mv) (.visitFieldInsn mv opcode owner name descriptor)))

  (method ^:public ^{Deprecated {:forRemoval false}} visitMethodInsn ^void [this ^:final ^int opcode
                                                                            ^:final ^String owner
                                                                            ^:final ^String name
                                                                            ^:final ^String descriptor]
    (let [opcodeAndSource (bit-or-int opcode (if (< api Opcodes/ASM5) Opcodes/SOURCE_DEPRECATED 0))]
      (.visitMethodInsn this
                        opcodeAndSource
                        owner
                        name
                        descriptor
                        (== opcode Opcodes/INVOKEINTERFACE))))

  (method ^:public visitMethodInsn ^void [this ^:final ^int opcode ^:final ^String owner
                                          ^:final ^String name ^:final ^String descriptor
                                          ^:final ^boolean isInterface]
    (cond
      (and (< api Opcodes/ASM5) (== (bit-and-int opcode Opcodes/SOURCE_DEPRECATED) 0))
        (do
          (when-not (= isInterface (== opcode Opcodes/INVOKEINTERFACE))
            (throw (UnsupportedOperationException.
                     "INVOKESPECIAL/STATIC on interfaces requires ASM5")))
          (.visitMethodInsn this opcode owner name descriptor))
      (some? mv)
        (.visitMethodInsn mv
                          (bit-and-int opcode (bit-not-int Opcodes/SOURCE_MASK))
                          owner
                          name
                          descriptor
                          isInterface)))

  (method ^:public visitInvokeDynamicInsn ^void [this ^:final ^String name
                                                 ^:final ^String descriptor
                                                 ^:final ^Handle bootstrapMethodHandle &
                                                 ^:final ^Object/1 bootstrapMethodArguments]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv)
      (.visitInvokeDynamicInsn mv name descriptor bootstrapMethodHandle bootstrapMethodArguments)))

  (method ^:public visitJumpInsn ^void [this ^:final ^int opcode ^:final ^Label label]
    (when (some? mv) (.visitJumpInsn mv opcode label)))

  (method ^:public visitLabel ^void [this ^:final ^Label label]
    (when (some? mv) (.visitLabel mv label)))

  (method ^:public visitLdcInsn ^void [this ^:final value]
    (when (and (< api Opcodes/ASM5)
               (or (instance? Handle value)
                   (and (instance? Type value) (== (.getSort (cast Type value)) Type/METHOD))))
      (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (and (< api Opcodes/ASM7) (instance? ConstantDynamic value))
      (throw (UnsupportedOperationException. "This feature requires ASM7")))
    (when (some? mv) (.visitLdcInsn mv value)))

  (method ^:public visitIincInsn ^void [this ^:final ^int varIndex ^:final ^int increment]
    (when (some? mv) (.visitIincInsn mv varIndex increment)))

  (method ^:public visitTableSwitchInsn ^void [this ^:final ^int min ^:final ^int max
                                               ^:final ^Label dflt & ^:final ^Label/1 labels]
    (when (some? mv) (.visitTableSwitchInsn mv min max dflt labels)))

  (method ^:public visitLookupSwitchInsn ^void [this ^:final ^Label dflt ^:final ^int/1 keys
                                                ^:final ^Label/1 labels]
    (when (some? mv) (.visitLookupSwitchInsn mv dflt keys labels)))

  (method ^:public visitMultiANewArrayInsn ^void [this ^:final ^String descriptor
                                                  ^:final ^int numDimensions]
    (when (some? mv) (.visitMultiANewArrayInsn mv descriptor numDimensions)))

  (method ^:public visitInsnAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv) (.visitInsnAnnotation mv typeRef typePath descriptor visible)))

  (method ^:public visitTryCatchBlock ^void [this ^:final ^Label start ^:final ^Label end
                                             ^:final ^Label handler ^:final ^String type]
    (when (some? mv) (.visitTryCatchBlock mv start end handler type)))

  (method ^:public visitTryCatchAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                               ^:final ^TypePath typePath
                                                               ^:final ^String descriptor
                                                               ^:final ^boolean visible]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv) (.visitTryCatchAnnotation mv typeRef typePath descriptor visible)))

  (method ^:public visitLocalVariable ^void [this ^:final ^String name ^:final ^String descriptor
                                             ^:final ^String signature ^:final ^Label start
                                             ^:final ^Label end ^:final ^int index]
    (when (some? mv) (.visitLocalVariable mv name descriptor signature start end index)))

  (method ^:public visitLocalVariableAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                                    ^:final ^TypePath typePath
                                                                    ^:final ^Label/1 start
                                                                    ^:final ^Label/1 end
                                                                    ^:final ^int/1 index
                                                                    ^:final ^String descriptor
                                                                    ^:final ^boolean visible]
    (when (< api Opcodes/ASM5) (throw (UnsupportedOperationException. REQUIRES_ASM5)))
    (when (some? mv)
      (.visitLocalVariableAnnotation mv typeRef typePath start end index descriptor visible)))

  (method ^:public visitLineNumber ^void [this ^:final ^int line ^:final ^Label start]
    (when (some? mv) (.visitLineNumber mv line start)))

  (method ^:public visitMaxs ^void [this ^:final ^int maxStack ^:final ^int maxLocals]
    (when (some? mv) (.visitMaxs mv maxStack maxLocals)))

  (method ^:public visitEnd ^void [this] (when (some? mv) (.visitEnd mv))))
