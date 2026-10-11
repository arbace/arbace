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
;; Converted from clojure/asm/commons/InstructionAdapter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.commons)

(import '(arbace.asm ConstantDynamic Handle Label MethodVisitor Opcodes Type))

(defclass ^:public InstructionAdapter
  :extends MethodVisitor

  (field ^:public ^:static ^:final ^Type OBJECT_TYPE (Type/getType "Ljava/lang/Object;"))

  (constructor ^:public [this ^:final ^MethodVisitor methodVisitor]
    (this. Opcodes/ASM9 methodVisitor)
    (when-not (identical? (.getClass this) InstructionAdapter) (throw (IllegalStateException.))))

  (constructor ^:protected [this ^:final ^int api ^:final ^MethodVisitor methodVisitor]
    (super. api methodVisitor))

  (method ^:public visitInsn ^void [this ^:final ^int opcode]
    (switch opcode
      Opcodes/NOP (.nop this)
      Opcodes/ACONST_NULL (.aconst this nil)
      (Opcodes/ICONST_M1 Opcodes/ICONST_0
                         Opcodes/ICONST_1
                         Opcodes/ICONST_2
                         Opcodes/ICONST_3
                         Opcodes/ICONST_4
                         Opcodes/ICONST_5)
        (.iconst this (unchecked-subtract-int opcode Opcodes/ICONST_0))
      (Opcodes/LCONST_0 Opcodes/LCONST_1)
        (.lconst this (long (unchecked-subtract-int opcode Opcodes/LCONST_0)))
      (Opcodes/FCONST_0 Opcodes/FCONST_1 Opcodes/FCONST_2)
        (.fconst this (float (unchecked-subtract-int opcode Opcodes/FCONST_0)))
      (Opcodes/DCONST_0 Opcodes/DCONST_1)
        (.dconst this (double (unchecked-subtract-int opcode Opcodes/DCONST_0)))
      Opcodes/IALOAD (.aload this Type/INT_TYPE)
      Opcodes/LALOAD (.aload this Type/LONG_TYPE)
      Opcodes/FALOAD (.aload this Type/FLOAT_TYPE)
      Opcodes/DALOAD (.aload this Type/DOUBLE_TYPE)
      Opcodes/AALOAD (.aload this OBJECT_TYPE)
      Opcodes/BALOAD (.aload this Type/BYTE_TYPE)
      Opcodes/CALOAD (.aload this Type/CHAR_TYPE)
      Opcodes/SALOAD (.aload this Type/SHORT_TYPE)
      Opcodes/IASTORE (.astore this Type/INT_TYPE)
      Opcodes/LASTORE (.astore this Type/LONG_TYPE)
      Opcodes/FASTORE (.astore this Type/FLOAT_TYPE)
      Opcodes/DASTORE (.astore this Type/DOUBLE_TYPE)
      Opcodes/AASTORE (.astore this OBJECT_TYPE)
      Opcodes/BASTORE (.astore this Type/BYTE_TYPE)
      Opcodes/CASTORE (.astore this Type/CHAR_TYPE)
      Opcodes/SASTORE (.astore this Type/SHORT_TYPE)
      Opcodes/POP (.pop this)
      Opcodes/POP2 (.pop2 this)
      Opcodes/DUP (.dup this)
      Opcodes/DUP_X1 (.dupX1 this)
      Opcodes/DUP_X2 (.dupX2 this)
      Opcodes/DUP2 (.dup2 this)
      Opcodes/DUP2_X1 (.dup2X1 this)
      Opcodes/DUP2_X2 (.dup2X2 this)
      Opcodes/SWAP (.swap this)
      Opcodes/IADD (.add this Type/INT_TYPE)
      Opcodes/LADD (.add this Type/LONG_TYPE)
      Opcodes/FADD (.add this Type/FLOAT_TYPE)
      Opcodes/DADD (.add this Type/DOUBLE_TYPE)
      Opcodes/ISUB (.sub this Type/INT_TYPE)
      Opcodes/LSUB (.sub this Type/LONG_TYPE)
      Opcodes/FSUB (.sub this Type/FLOAT_TYPE)
      Opcodes/DSUB (.sub this Type/DOUBLE_TYPE)
      Opcodes/IMUL (.mul this Type/INT_TYPE)
      Opcodes/LMUL (.mul this Type/LONG_TYPE)
      Opcodes/FMUL (.mul this Type/FLOAT_TYPE)
      Opcodes/DMUL (.mul this Type/DOUBLE_TYPE)
      Opcodes/IDIV (.div this Type/INT_TYPE)
      Opcodes/LDIV (.div this Type/LONG_TYPE)
      Opcodes/FDIV (.div this Type/FLOAT_TYPE)
      Opcodes/DDIV (.div this Type/DOUBLE_TYPE)
      Opcodes/IREM (.rem this Type/INT_TYPE)
      Opcodes/LREM (.rem this Type/LONG_TYPE)
      Opcodes/FREM (.rem this Type/FLOAT_TYPE)
      Opcodes/DREM (.rem this Type/DOUBLE_TYPE)
      Opcodes/INEG (.neg this Type/INT_TYPE)
      Opcodes/LNEG (.neg this Type/LONG_TYPE)
      Opcodes/FNEG (.neg this Type/FLOAT_TYPE)
      Opcodes/DNEG (.neg this Type/DOUBLE_TYPE)
      Opcodes/ISHL (.shl this Type/INT_TYPE)
      Opcodes/LSHL (.shl this Type/LONG_TYPE)
      Opcodes/ISHR (.shr this Type/INT_TYPE)
      Opcodes/LSHR (.shr this Type/LONG_TYPE)
      Opcodes/IUSHR (.ushr this Type/INT_TYPE)
      Opcodes/LUSHR (.ushr this Type/LONG_TYPE)
      Opcodes/IAND (.and this Type/INT_TYPE)
      Opcodes/LAND (.and this Type/LONG_TYPE)
      Opcodes/IOR (.or this Type/INT_TYPE)
      Opcodes/LOR (.or this Type/LONG_TYPE)
      Opcodes/IXOR (.xor this Type/INT_TYPE)
      Opcodes/LXOR (.xor this Type/LONG_TYPE)
      Opcodes/I2L (.cast this Type/INT_TYPE Type/LONG_TYPE)
      Opcodes/I2F (.cast this Type/INT_TYPE Type/FLOAT_TYPE)
      Opcodes/I2D (.cast this Type/INT_TYPE Type/DOUBLE_TYPE)
      Opcodes/L2I (.cast this Type/LONG_TYPE Type/INT_TYPE)
      Opcodes/L2F (.cast this Type/LONG_TYPE Type/FLOAT_TYPE)
      Opcodes/L2D (.cast this Type/LONG_TYPE Type/DOUBLE_TYPE)
      Opcodes/F2I (.cast this Type/FLOAT_TYPE Type/INT_TYPE)
      Opcodes/F2L (.cast this Type/FLOAT_TYPE Type/LONG_TYPE)
      Opcodes/F2D (.cast this Type/FLOAT_TYPE Type/DOUBLE_TYPE)
      Opcodes/D2I (.cast this Type/DOUBLE_TYPE Type/INT_TYPE)
      Opcodes/D2L (.cast this Type/DOUBLE_TYPE Type/LONG_TYPE)
      Opcodes/D2F (.cast this Type/DOUBLE_TYPE Type/FLOAT_TYPE)
      Opcodes/I2B (.cast this Type/INT_TYPE Type/BYTE_TYPE)
      Opcodes/I2C (.cast this Type/INT_TYPE Type/CHAR_TYPE)
      Opcodes/I2S (.cast this Type/INT_TYPE Type/SHORT_TYPE)
      Opcodes/LCMP (.lcmp this)
      Opcodes/FCMPL (.cmpl this Type/FLOAT_TYPE)
      Opcodes/FCMPG (.cmpg this Type/FLOAT_TYPE)
      Opcodes/DCMPL (.cmpl this Type/DOUBLE_TYPE)
      Opcodes/DCMPG (.cmpg this Type/DOUBLE_TYPE)
      Opcodes/IRETURN (.areturn this Type/INT_TYPE)
      Opcodes/LRETURN (.areturn this Type/LONG_TYPE)
      Opcodes/FRETURN (.areturn this Type/FLOAT_TYPE)
      Opcodes/DRETURN (.areturn this Type/DOUBLE_TYPE)
      Opcodes/ARETURN (.areturn this OBJECT_TYPE)
      Opcodes/RETURN (.areturn this Type/VOID_TYPE)
      Opcodes/ARRAYLENGTH (.arraylength this)
      Opcodes/ATHROW (.athrow this)
      Opcodes/MONITORENTER (.monitorenter this)
      Opcodes/MONITOREXIT (.monitorexit this)
      (throw (IllegalArgumentException.))))

  (method ^:public visitIntInsn ^void [this ^:final ^int opcode ^:final ^int operand]
    (switch opcode
      Opcodes/BIPUSH (.iconst this operand)
      Opcodes/SIPUSH (.iconst this operand)
      Opcodes/NEWARRAY
        (switch operand
          Opcodes/T_BOOLEAN (.newarray this Type/BOOLEAN_TYPE)
          Opcodes/T_CHAR (.newarray this Type/CHAR_TYPE)
          Opcodes/T_BYTE (.newarray this Type/BYTE_TYPE)
          Opcodes/T_SHORT (.newarray this Type/SHORT_TYPE)
          Opcodes/T_INT (.newarray this Type/INT_TYPE)
          Opcodes/T_FLOAT (.newarray this Type/FLOAT_TYPE)
          Opcodes/T_LONG (.newarray this Type/LONG_TYPE)
          Opcodes/T_DOUBLE (.newarray this Type/DOUBLE_TYPE)
          (throw (IllegalArgumentException.)))
      (throw (IllegalArgumentException.))))

  (method ^:public visitVarInsn ^void [this ^:final ^int opcode ^:final ^int varIndex]
    (switch opcode
      Opcodes/ILOAD (.load this varIndex Type/INT_TYPE)
      Opcodes/LLOAD (.load this varIndex Type/LONG_TYPE)
      Opcodes/FLOAD (.load this varIndex Type/FLOAT_TYPE)
      Opcodes/DLOAD (.load this varIndex Type/DOUBLE_TYPE)
      Opcodes/ALOAD (.load this varIndex OBJECT_TYPE)
      Opcodes/ISTORE (.store this varIndex Type/INT_TYPE)
      Opcodes/LSTORE (.store this varIndex Type/LONG_TYPE)
      Opcodes/FSTORE (.store this varIndex Type/FLOAT_TYPE)
      Opcodes/DSTORE (.store this varIndex Type/DOUBLE_TYPE)
      Opcodes/ASTORE (.store this varIndex OBJECT_TYPE)
      Opcodes/RET (.ret this varIndex)
      (throw (IllegalArgumentException.))))

  (method ^:public visitTypeInsn ^void [this ^:final ^int opcode ^:final ^String type]
    (let [objectType (Type/getObjectType type)]
      (switch opcode
        Opcodes/NEW (.anew this objectType)
        Opcodes/ANEWARRAY (.newarray this objectType)
        Opcodes/CHECKCAST (.checkcast this objectType)
        Opcodes/INSTANCEOF (.instanceOf this objectType)
        (throw (IllegalArgumentException.)))))

  (method ^:public visitFieldInsn ^void [this ^:final ^int opcode ^:final ^String owner
                                         ^:final ^String name ^:final ^String descriptor]
    (switch opcode
      Opcodes/GETSTATIC (.getstatic this owner name descriptor)
      Opcodes/PUTSTATIC (.putstatic this owner name descriptor)
      Opcodes/GETFIELD (.getfield this owner name descriptor)
      Opcodes/PUTFIELD (.putfield this owner name descriptor)
      (throw (IllegalArgumentException.))))

  (method ^:public visitMethodInsn ^void [this ^:final ^int opcodeAndSource ^:final ^String owner
                                          ^:final ^String name ^:final ^String descriptor
                                          ^:final ^boolean isInterface]
    (if (and (< (.-api this) Opcodes/ASM5)
             (== (bit-and-int opcodeAndSource Opcodes/SOURCE_DEPRECATED) 0))
        (.visitMethodInsn super opcodeAndSource owner name descriptor isInterface)
        (let [opcode (bit-and-int opcodeAndSource (bit-not-int Opcodes/SOURCE_MASK))]
          (switch opcode
            Opcodes/INVOKESPECIAL (.invokespecial this owner name descriptor isInterface)
            Opcodes/INVOKEVIRTUAL (.invokevirtual this owner name descriptor isInterface)
            Opcodes/INVOKESTATIC (.invokestatic this owner name descriptor isInterface)
            Opcodes/INVOKEINTERFACE (.invokeinterface this owner name descriptor)
            (throw (IllegalArgumentException.))))))

  (method ^:public visitInvokeDynamicInsn ^void [this ^:final ^String name
                                                 ^:final ^String descriptor
                                                 ^:final ^Handle bootstrapMethodHandle &
                                                 ^:final ^Object/1 bootstrapMethodArguments]
    (when (< (.-api this) Opcodes/ASM5)
      (throw (UnsupportedOperationException. "This feature requires ASM5")))
    (.invokedynamic this name descriptor bootstrapMethodHandle bootstrapMethodArguments))

  (method ^:public visitJumpInsn ^void [this ^:final ^int opcode ^:final ^Label label]
    (switch opcode
      Opcodes/IFEQ (.ifeq this label)
      Opcodes/IFNE (.ifne this label)
      Opcodes/IFLT (.iflt this label)
      Opcodes/IFGE (.ifge this label)
      Opcodes/IFGT (.ifgt this label)
      Opcodes/IFLE (.ifle this label)
      Opcodes/IF_ICMPEQ (.ificmpeq this label)
      Opcodes/IF_ICMPNE (.ificmpne this label)
      Opcodes/IF_ICMPLT (.ificmplt this label)
      Opcodes/IF_ICMPGE (.ificmpge this label)
      Opcodes/IF_ICMPGT (.ificmpgt this label)
      Opcodes/IF_ICMPLE (.ificmple this label)
      Opcodes/IF_ACMPEQ (.ifacmpeq this label)
      Opcodes/IF_ACMPNE (.ifacmpne this label)
      Opcodes/GOTO (.goTo this label)
      Opcodes/JSR (.jsr this label)
      Opcodes/IFNULL (.ifnull this label)
      Opcodes/IFNONNULL (.ifnonnull this label)
      (throw (IllegalArgumentException.))))

  (method ^:public visitLabel ^void [this ^:final ^Label label]
    (.mark this label))

  (method ^:public visitLdcInsn ^void [this ^:final value]
    (when (and (< (.-api this) Opcodes/ASM5)
               (or (instance? Handle value)
                   (and (instance? Type value) (== (.getSort (cast Type value)) Type/METHOD))))
      (throw (UnsupportedOperationException. "This feature requires ASM5")))
    (when (and (< (.-api this) Opcodes/ASM7) (instance? ConstantDynamic value))
      (throw (UnsupportedOperationException. "This feature requires ASM7")))
    (cond
      (instance? Integer value) (.iconst this (cast Integer value))
      (instance? Byte value) (.iconst this (.intValue (cast Byte value)))
      (instance? Character value) (.iconst this (.charValue (cast Character value)))
      (instance? Short value) (.iconst this (.intValue (cast Short value)))
      (instance? Boolean value) (.iconst this (if (.booleanValue (cast Boolean value)) 1 0))
      (instance? Float value) (.fconst this (cast Float value))
      (instance? Long value) (.lconst this (cast Long value))
      (instance? Double value) (.dconst this (cast Double value))
      (instance? String value) (.aconst this value)
      (instance? Type value) (.tconst this (cast Type value))
      (instance? Handle value) (.hconst this (cast Handle value))
      (instance? ConstantDynamic value) (.cconst this (cast ConstantDynamic value))
      :else (throw (IllegalArgumentException.))))

  (method ^:public visitIincInsn ^void [this ^:final ^int varIndex ^:final ^int increment]
    (.iinc this varIndex increment))

  (method ^:public visitTableSwitchInsn ^void [this ^:final ^int min ^:final ^int max
                                               ^:final ^Label dflt & ^:final ^Label/1 labels]
    (.tableswitch this min max dflt labels))

  (method ^:public visitLookupSwitchInsn ^void [this ^:final ^Label dflt ^:final ^int/1 keys
                                                ^:final ^Label/1 labels]
    (.lookupswitch this dflt keys labels))

  (method ^:public visitMultiANewArrayInsn ^void [this ^:final ^String descriptor
                                                  ^:final ^int numDimensions]
    (.multianewarray this descriptor numDimensions))

  (method ^:public nop ^void [this] (.visitInsn (.-mv this) Opcodes/NOP))

  (method ^:public aconst ^void [this ^:final value]
    (if (nil? value) (.visitInsn (.-mv this) Opcodes/ACONST_NULL) (.visitLdcInsn (.-mv this) value)))

  (method ^:public iconst ^void [this ^:final ^int intValue]
    (cond
      (and (>= intValue -1) (<= intValue 5))
        (.visitInsn (.-mv this) (unchecked-add-int Opcodes/ICONST_0 intValue))
      (and (>= intValue Byte/MIN_VALUE) (<= intValue Byte/MAX_VALUE))
        (.visitIntInsn (.-mv this) Opcodes/BIPUSH intValue)
      (and (>= intValue Short/MIN_VALUE) (<= intValue Short/MAX_VALUE))
        (.visitIntInsn (.-mv this) Opcodes/SIPUSH intValue)
      :else (.visitLdcInsn (.-mv this) intValue)))

  (method ^:public lconst ^void [this ^:final ^long longValue]
    (if (or (== longValue 0) (== longValue 1))
        (.visitInsn (.-mv this) (unchecked-add-int Opcodes/LCONST_0 (unchecked-int longValue)))
        (.visitLdcInsn (.-mv this) longValue)))

  (method ^:public fconst ^void [this ^:final ^float floatValue]
    (let [bits (Float/floatToIntBits floatValue)]
      (if (or (or (== bits 0) (== bits 0x3F800000)) (== bits 0x40000000))
          (.visitInsn (.-mv this) (unchecked-add-int Opcodes/FCONST_0 (unchecked-int floatValue)))
          (.visitLdcInsn (.-mv this) floatValue))))

  (method ^:public dconst ^void [this ^:final ^double doubleValue]
    (let [bits (Double/doubleToLongBits doubleValue)]
      (if (or (== bits 0) (== bits 0x3FF0000000000000))
          (.visitInsn (.-mv this) (unchecked-add-int Opcodes/DCONST_0 (unchecked-int doubleValue)))
          (.visitLdcInsn (.-mv this) doubleValue))))

  (method ^:public tconst ^void [this ^:final ^Type type]
    (.visitLdcInsn (.-mv this) type))

  (method ^:public hconst ^void [this ^:final ^Handle handle]
    (.visitLdcInsn (.-mv this) handle))

  (method ^:public cconst ^void [this ^:final ^ConstantDynamic constantDynamic]
    (.visitLdcInsn (.-mv this) constantDynamic))

  (method ^:public load ^void [this ^:final ^int varIndex ^:final ^Type type]
    (.visitVarInsn (.-mv this) (.getOpcode type Opcodes/ILOAD) varIndex))

  (method ^:public aload ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IALOAD)))

  (method ^:public store ^void [this ^:final ^int varIndex ^:final ^Type type]
    (.visitVarInsn (.-mv this) (.getOpcode type Opcodes/ISTORE) varIndex))

  (method ^:public astore ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IASTORE)))

  (method ^:public pop ^void [this] (.visitInsn (.-mv this) Opcodes/POP))

  (method ^:public pop2 ^void [this]
    (.visitInsn (.-mv this) Opcodes/POP2))

  (method ^:public dup ^void [this] (.visitInsn (.-mv this) Opcodes/DUP))

  (method ^:public dup2 ^void [this]
    (.visitInsn (.-mv this) Opcodes/DUP2))

  (method ^:public dupX1 ^void [this]
    (.visitInsn (.-mv this) Opcodes/DUP_X1))

  (method ^:public dupX2 ^void [this]
    (.visitInsn (.-mv this) Opcodes/DUP_X2))

  (method ^:public dup2X1 ^void [this]
    (.visitInsn (.-mv this) Opcodes/DUP2_X1))

  (method ^:public dup2X2 ^void [this]
    (.visitInsn (.-mv this) Opcodes/DUP2_X2))

  (method ^:public swap ^void [this]
    (.visitInsn (.-mv this) Opcodes/SWAP))

  (method ^:public add ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IADD)))

  (method ^:public sub ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/ISUB)))

  (method ^:public mul ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IMUL)))

  (method ^:public div ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IDIV)))

  (method ^:public rem ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IREM)))

  (method ^:public neg ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/INEG)))

  (method ^:public shl ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/ISHL)))

  (method ^:public shr ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/ISHR)))

  (method ^:public ushr ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IUSHR)))

  (method ^:public and ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IAND)))

  (method ^:public or ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IOR)))

  (method ^:public xor ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IXOR)))

  (method ^:public iinc ^void [this ^:final ^int varIndex ^:final ^int increment]
    (.visitIincInsn (.-mv this) varIndex increment))

  (method ^:public cast ^void [this ^:final ^Type from ^:final ^Type to]
    (InstructionAdapter/cast (.-mv this) from to))

  (method ^:static cast ^void [^:final ^MethodVisitor methodVisitor ^:final ^Type from
                               ^:final ^Type to]
    (when-not (identical? from to)
      (cond
        (identical? from Type/DOUBLE_TYPE)
          (cond
            (identical? to Type/FLOAT_TYPE) (.visitInsn methodVisitor Opcodes/D2F)
            (identical? to Type/LONG_TYPE) (.visitInsn methodVisitor Opcodes/D2L)
            :else
              (do
                (.visitInsn methodVisitor Opcodes/D2I)
                (InstructionAdapter/cast methodVisitor Type/INT_TYPE to)))
        (identical? from Type/FLOAT_TYPE)
          (cond
            (identical? to Type/DOUBLE_TYPE) (.visitInsn methodVisitor Opcodes/F2D)
            (identical? to Type/LONG_TYPE) (.visitInsn methodVisitor Opcodes/F2L)
            :else
              (do
                (.visitInsn methodVisitor Opcodes/F2I)
                (InstructionAdapter/cast methodVisitor Type/INT_TYPE to)))
        (identical? from Type/LONG_TYPE)
          (cond
            (identical? to Type/DOUBLE_TYPE) (.visitInsn methodVisitor Opcodes/L2D)
            (identical? to Type/FLOAT_TYPE) (.visitInsn methodVisitor Opcodes/L2F)
            :else
              (do
                (.visitInsn methodVisitor Opcodes/L2I)
                (InstructionAdapter/cast methodVisitor Type/INT_TYPE to)))
        (identical? to Type/BYTE_TYPE) (.visitInsn methodVisitor Opcodes/I2B)
        (identical? to Type/CHAR_TYPE) (.visitInsn methodVisitor Opcodes/I2C)
        (identical? to Type/DOUBLE_TYPE) (.visitInsn methodVisitor Opcodes/I2D)
        (identical? to Type/FLOAT_TYPE) (.visitInsn methodVisitor Opcodes/I2F)
        (identical? to Type/LONG_TYPE) (.visitInsn methodVisitor Opcodes/I2L)
        (identical? to Type/SHORT_TYPE) (.visitInsn methodVisitor Opcodes/I2S))))

  (method ^:public lcmp ^void [this]
    (.visitInsn (.-mv this) Opcodes/LCMP))

  (method ^:public cmpl ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (if (identical? type Type/FLOAT_TYPE) Opcodes/FCMPL Opcodes/DCMPL)))

  (method ^:public cmpg ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (if (identical? type Type/FLOAT_TYPE) Opcodes/FCMPG Opcodes/DCMPG)))

  (method ^:public ifeq ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFEQ label))

  (method ^:public ifne ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFNE label))

  (method ^:public iflt ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFLT label))

  (method ^:public ifge ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFGE label))

  (method ^:public ifgt ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFGT label))

  (method ^:public ifle ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFLE label))

  (method ^:public ificmpeq ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPEQ label))

  (method ^:public ificmpne ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPNE label))

  (method ^:public ificmplt ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPLT label))

  (method ^:public ificmpge ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPGE label))

  (method ^:public ificmpgt ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPGT label))

  (method ^:public ificmple ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ICMPLE label))

  (method ^:public ifacmpeq ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ACMPEQ label))

  (method ^:public ifacmpne ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IF_ACMPNE label))

  (method ^:public goTo ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/GOTO label))

  (method ^:public jsr ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/JSR label))

  (method ^:public ret ^void [this ^:final ^int varIndex]
    (.visitVarInsn (.-mv this) Opcodes/RET varIndex))

  (method ^:public tableswitch ^void [this ^:final ^int min ^:final ^int max ^:final ^Label dflt &
                                      ^:final ^Label/1 labels]
    (.visitTableSwitchInsn (.-mv this) min max dflt labels))

  (method ^:public lookupswitch ^void [this ^:final ^Label dflt ^:final ^int/1 keys
                                       ^:final ^Label/1 labels]
    (.visitLookupSwitchInsn (.-mv this) dflt keys labels))

  (method ^:public areturn ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IRETURN)))

  (method ^:public getstatic ^void [this ^:final ^String owner ^:final ^String name
                                    ^:final ^String descriptor]
    (.visitFieldInsn (.-mv this) Opcodes/GETSTATIC owner name descriptor))

  (method ^:public putstatic ^void [this ^:final ^String owner ^:final ^String name
                                    ^:final ^String descriptor]
    (.visitFieldInsn (.-mv this) Opcodes/PUTSTATIC owner name descriptor))

  (method ^:public getfield ^void [this ^:final ^String owner ^:final ^String name
                                   ^:final ^String descriptor]
    (.visitFieldInsn (.-mv this) Opcodes/GETFIELD owner name descriptor))

  (method ^:public putfield ^void [this ^:final ^String owner ^:final ^String name
                                   ^:final ^String descriptor]
    (.visitFieldInsn (.-mv this) Opcodes/PUTFIELD owner name descriptor))

  (method ^:public ^{Deprecated {:forRemoval false}} invokevirtual ^void [this ^:final ^String owner
                                                                          ^:final ^String name
                                                                          ^:final ^String descriptor]
    (if (>= (.-api this) Opcodes/ASM5)
        (.invokevirtual this owner name descriptor false)
        (.visitMethodInsn (.-mv this) Opcodes/INVOKEVIRTUAL owner name descriptor)))

  (method ^:public invokevirtual ^void [this ^:final ^String owner ^:final ^String name
                                        ^:final ^String descriptor ^:final ^boolean isInterface]
    (if (< (.-api this) Opcodes/ASM5)
        (do
          (when isInterface
            (throw (UnsupportedOperationException. "INVOKEVIRTUAL on interfaces require ASM 5")))
          (.invokevirtual this owner name descriptor))
        (.visitMethodInsn (.-mv this) Opcodes/INVOKEVIRTUAL owner name descriptor isInterface)))

  (method ^:public ^{Deprecated {:forRemoval false}} invokespecial ^void [this ^:final ^String owner
                                                                          ^:final ^String name
                                                                          ^:final ^String descriptor]
    (if (>= (.-api this) Opcodes/ASM5)
        (.invokespecial this owner name descriptor false)
        (.visitMethodInsn (.-mv this) Opcodes/INVOKESPECIAL owner name descriptor false)))

  (method ^:public invokespecial ^void [this ^:final ^String owner ^:final ^String name
                                        ^:final ^String descriptor ^:final ^boolean isInterface]
    (if (< (.-api this) Opcodes/ASM5)
        (do
          (when isInterface
            (throw (UnsupportedOperationException. "INVOKESPECIAL on interfaces require ASM 5")))
          (.invokespecial this owner name descriptor))
        (.visitMethodInsn (.-mv this) Opcodes/INVOKESPECIAL owner name descriptor isInterface)))

  (method ^:public ^{Deprecated {:forRemoval false}} invokestatic ^void [this ^:final ^String owner
                                                                         ^:final ^String name
                                                                         ^:final ^String descriptor]
    (if (>= (.-api this) Opcodes/ASM5)
        (.invokestatic this owner name descriptor false)
        (.visitMethodInsn (.-mv this) Opcodes/INVOKESTATIC owner name descriptor false)))

  (method ^:public invokestatic ^void [this ^:final ^String owner ^:final ^String name
                                       ^:final ^String descriptor ^:final ^boolean isInterface]
    (if (< (.-api this) Opcodes/ASM5)
        (do
          (when isInterface
            (throw (UnsupportedOperationException. "INVOKESTATIC on interfaces require ASM 5")))
          (.invokestatic this owner name descriptor))
        (.visitMethodInsn (.-mv this) Opcodes/INVOKESTATIC owner name descriptor isInterface)))

  (method ^:public invokeinterface ^void [this ^:final ^String owner ^:final ^String name
                                          ^:final ^String descriptor]
    (.visitMethodInsn (.-mv this) Opcodes/INVOKEINTERFACE owner name descriptor true))

  (method ^:public invokedynamic ^void [this ^:final ^String name ^:final ^String descriptor
                                        ^:final ^Handle bootstrapMethodHandle
                                        ^:final ^Object/1 bootstrapMethodArguments]
    (.visitInvokeDynamicInsn (.-mv this)
                             name
                             descriptor
                             bootstrapMethodHandle
                             bootstrapMethodArguments))

  (method ^:public anew ^void [this ^:final ^Type type]
    (.visitTypeInsn (.-mv this) Opcodes/NEW (.getInternalName type)))

  (method ^:public newarray ^void [this ^:final ^Type type]
    (InstructionAdapter/newarray (.-mv this) type))

  (method ^:static newarray ^void [^:final ^MethodVisitor methodVisitor ^:final ^Type type]
    (let [^:mutable ^int arrayType 0]
      (switch (.getSort type)
        Type/BOOLEAN (set! arrayType Opcodes/T_BOOLEAN)
        Type/CHAR (set! arrayType Opcodes/T_CHAR)
        Type/BYTE (set! arrayType Opcodes/T_BYTE)
        Type/SHORT (set! arrayType Opcodes/T_SHORT)
        Type/INT (set! arrayType Opcodes/T_INT)
        Type/FLOAT (set! arrayType Opcodes/T_FLOAT)
        Type/LONG (set! arrayType Opcodes/T_LONG)
        Type/DOUBLE (set! arrayType Opcodes/T_DOUBLE)
        (do (.visitTypeInsn methodVisitor Opcodes/ANEWARRAY (.getInternalName type)) (return)))
      (.visitIntInsn methodVisitor Opcodes/NEWARRAY arrayType)))

  (method ^:public arraylength ^void [this]
    (.visitInsn (.-mv this) Opcodes/ARRAYLENGTH))

  (method ^:public athrow ^void [this]
    (.visitInsn (.-mv this) Opcodes/ATHROW))

  (method ^:public checkcast ^void [this ^:final ^Type type]
    (.visitTypeInsn (.-mv this) Opcodes/CHECKCAST (.getInternalName type)))

  (method ^:public instanceOf ^void [this ^:final ^Type type]
    (.visitTypeInsn (.-mv this) Opcodes/INSTANCEOF (.getInternalName type)))

  (method ^:public monitorenter ^void [this]
    (.visitInsn (.-mv this) Opcodes/MONITORENTER))

  (method ^:public monitorexit ^void [this]
    (.visitInsn (.-mv this) Opcodes/MONITOREXIT))

  (method ^:public multianewarray ^void [this ^:final ^String descriptor ^:final ^int numDimensions]
    (.visitMultiANewArrayInsn (.-mv this) descriptor numDimensions))

  (method ^:public ifnull ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFNULL label))

  (method ^:public ifnonnull ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFNONNULL label))

  (method ^:public mark ^void [this ^:final ^Label label]
    (.visitLabel (.-mv this) label)))
