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
;; Converted from clojure/asm/Frame.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass Frame
  (field ^:static ^:final ^int SAME_FRAME 0)

  (field ^:static ^:final ^int SAME_LOCALS_1_STACK_ITEM_FRAME 64)

  (field ^:static ^:final ^int RESERVED 128)

  (field ^:static ^:final ^int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED 247)

  (field ^:static ^:final ^int CHOP_FRAME 248)

  (field ^:static ^:final ^int SAME_FRAME_EXTENDED 251)

  (field ^:static ^:final ^int APPEND_FRAME 252)

  (field ^:static ^:final ^int FULL_FRAME 255)

  (field ^:static ^:final ^int ITEM_TOP 0)

  (field ^:static ^:final ^int ITEM_INTEGER 1)

  (field ^:static ^:final ^int ITEM_FLOAT 2)

  (field ^:static ^:final ^int ITEM_DOUBLE 3)

  (field ^:static ^:final ^int ITEM_LONG 4)

  (field ^:static ^:final ^int ITEM_NULL 5)

  (field ^:static ^:final ^int ITEM_UNINITIALIZED_THIS 6)

  (field ^:static ^:final ^int ITEM_OBJECT 7)

  (field ^:static ^:final ^int ITEM_UNINITIALIZED 8)

  (field ^:private ^:static ^:final ^int ITEM_ASM_BOOLEAN 9)

  (field ^:private ^:static ^:final ^int ITEM_ASM_BYTE 10)

  (field ^:private ^:static ^:final ^int ITEM_ASM_CHAR 11)

  (field ^:private ^:static ^:final ^int ITEM_ASM_SHORT 12)

  (field ^:private ^:static ^:final ^int DIM_SIZE 6)

  (field ^:private ^:static ^:final ^int KIND_SIZE 4)

  (field ^:private ^:static ^:final ^int FLAGS_SIZE 2)

  (field ^:private ^:static ^:final ^int VALUE_SIZE
    (unchecked-subtract-int (unchecked-subtract-int (unchecked-subtract-int 32 DIM_SIZE) KIND_SIZE)
                            FLAGS_SIZE))

  (field ^:private ^:static ^:final ^int DIM_SHIFT
    (unchecked-add-int (unchecked-add-int KIND_SIZE FLAGS_SIZE) VALUE_SIZE))

  (field ^:private ^:static ^:final ^int KIND_SHIFT (unchecked-add-int FLAGS_SIZE VALUE_SIZE))

  (field ^:private ^:static ^:final ^int FLAGS_SHIFT VALUE_SIZE)

  (field ^:private ^:static ^:final ^int DIM_MASK
    (bit-shift-left-int (unchecked-subtract-int (bit-shift-left-int 1 DIM_SIZE) 1) DIM_SHIFT))

  (field ^:private ^:static ^:final ^int KIND_MASK
    (bit-shift-left-int (unchecked-subtract-int (bit-shift-left-int 1 KIND_SIZE) 1) KIND_SHIFT))

  (field ^:private ^:static ^:final ^int VALUE_MASK
    (unchecked-subtract-int (bit-shift-left-int 1 VALUE_SIZE) 1))

  (field ^:private ^:static ^:final ^int ARRAY_OF (bit-shift-left-int 1 DIM_SHIFT))

  (field ^:private ^:static ^:final ^int ELEMENT_OF (bit-shift-left-int -1 DIM_SHIFT))

  (field ^:private ^:static ^:final ^int CONSTANT_KIND (bit-shift-left-int 1 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int REFERENCE_KIND (bit-shift-left-int 2 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int UNINITIALIZED_KIND (bit-shift-left-int 3 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int FORWARD_UNINITIALIZED_KIND
    (bit-shift-left-int 4 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int LOCAL_KIND (bit-shift-left-int 5 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int STACK_KIND (bit-shift-left-int 6 KIND_SHIFT))

  (field ^:private ^:static ^:final ^int TOP_IF_LONG_OR_DOUBLE_FLAG
    (bit-shift-left-int 1 FLAGS_SHIFT))

  (field ^:private ^:static ^:final ^int TOP (bit-or-int CONSTANT_KIND ITEM_TOP))

  (field ^:private ^:static ^:final ^int BOOLEAN (bit-or-int CONSTANT_KIND ITEM_ASM_BOOLEAN))

  (field ^:private ^:static ^:final ^int BYTE (bit-or-int CONSTANT_KIND ITEM_ASM_BYTE))

  (field ^:private ^:static ^:final ^int CHAR (bit-or-int CONSTANT_KIND ITEM_ASM_CHAR))

  (field ^:private ^:static ^:final ^int SHORT (bit-or-int CONSTANT_KIND ITEM_ASM_SHORT))

  (field ^:private ^:static ^:final ^int INTEGER (bit-or-int CONSTANT_KIND ITEM_INTEGER))

  (field ^:private ^:static ^:final ^int FLOAT (bit-or-int CONSTANT_KIND ITEM_FLOAT))

  (field ^:private ^:static ^:final ^int LONG (bit-or-int CONSTANT_KIND ITEM_LONG))

  (field ^:private ^:static ^:final ^int DOUBLE (bit-or-int CONSTANT_KIND ITEM_DOUBLE))

  (field ^:private ^:static ^:final ^int NULL (bit-or-int CONSTANT_KIND ITEM_NULL))

  (field ^:private ^:static ^:final ^int UNINITIALIZED_THIS
    (bit-or-int CONSTANT_KIND ITEM_UNINITIALIZED_THIS))

  (field ^:static ^:final ^int NUM_OPERATIONS_PER_MERGE 50)

  (field ^Label owner)

  (field ^:private ^int/1 inputLocals)

  (field ^:private ^int/1 inputStack)

  (field ^:private ^int/1 outputLocals)

  (field ^:private ^int/1 outputStack)

  (field ^:private ^short outputStackStart)

  (field ^:private ^short outputStackTop)

  (field ^:private ^int/1 initializations)

  (field ^:final ^ComputeLimits limits)

  (constructor [this ^:final ^Label owner ^:final ^ComputeLimits limits]
    (set! (.-owner this) owner)
    (set! (.-limits this) limits))

  (method ^:final copyFrom ^void [this ^:final ^Frame frame]
    (set! inputLocals (.-inputLocals frame))
    (set! inputStack (.-inputStack frame))
    (set! outputStackStart 0)
    (set! outputLocals (.-outputLocals frame))
    (set! outputStack (.-outputStack frame))
    (set! outputStackTop (.-outputStackTop frame))
    (set! initializations (.-initializations frame)))

  (method ^:static getAbstractTypeFromApiFormat ^int [^:final ^SymbolTable symbolTable ^:final type]
    (cond
      (instance? Integer type) (bit-or-int CONSTANT_KIND (.intValue (cast Integer type)))
      (instance? String type)
        (let [descriptor (.getDescriptor (Type/getObjectType (cast String type)))]
          (Frame/getAbstractTypeFromDescriptor symbolTable descriptor 0))
      :else
        (let [label (cast Label type)]
          (if (not (== (bit-and-int (.-flags label) Label/FLAG_RESOLVED) 0))
              (bit-or-int UNINITIALIZED_KIND
                          (.addUninitializedType symbolTable "" (.-bytecodeOffset label)))
              (bit-or-int FORWARD_UNINITIALIZED_KIND
                          (.addForwardUninitializedType symbolTable "" label))))))

  (method ^:static getAbstractTypeFromInternalName ^int [^:final ^SymbolTable symbolTable
                                                         ^:final ^String internalName]
    (bit-or-int REFERENCE_KIND (.addType symbolTable internalName)))

  (method ^:private ^:static getAbstractTypeFromDescriptor ^int [^:final ^SymbolTable symbolTable
                                                                 ^:final ^String buffer
                                                                 ^:final ^int offset]
    (let [^:mutable ^String internalName nil]
      (switch (.charAt buffer offset)
        \V 0
        (\Z \C \B \S \I) INTEGER
        \F FLOAT
        \J LONG
        \D DOUBLE
        \L
          (do
            (set! internalName
                  (.substring buffer
                              (unchecked-add-int offset 1)
                              (unchecked-subtract-int (.length buffer) 1)))
            (bit-or-int REFERENCE_KIND (.addType symbolTable internalName)))
        \[
          (let [^:mutable elementDescriptorOffset (unchecked-add-int offset 1)]
            (while (== (.charAt buffer elementDescriptorOffset) \[)
              (set! elementDescriptorOffset (unchecked-inc-int elementDescriptorOffset)))
            (let [^:mutable ^int typeValue 0]
              (switch (.charAt buffer elementDescriptorOffset)
                \Z (set! typeValue BOOLEAN)
                \C (set! typeValue CHAR)
                \B (set! typeValue BYTE)
                \S (set! typeValue SHORT)
                \I (set! typeValue INTEGER)
                \F (set! typeValue FLOAT)
                \J (set! typeValue LONG)
                \D (set! typeValue DOUBLE)
                \L
                  (do
                    (set! internalName
                          (.substring buffer
                                      (unchecked-add-int elementDescriptorOffset 1)
                                      (unchecked-subtract-int (.length buffer) 1)))
                    (set! typeValue (bit-or-int REFERENCE_KIND (.addType symbolTable internalName))))
                (throw (IllegalArgumentException.
                         (java-str "Invalid descriptor fragment: "
                                   (.substring buffer elementDescriptorOffset)))))
              (bit-or-int (bit-shift-left-int
                            (unchecked-subtract-int elementDescriptorOffset offset)
                            DIM_SHIFT)
                          typeValue)))
        (throw (IllegalArgumentException.
                 (java-str "Invalid descriptor: " (.substring buffer offset)))))))

  (method ^:final setInputFrameFromDescriptor ^void [this ^:final ^SymbolTable symbolTable
                                                     ^:final ^int access ^:final ^String descriptor
                                                     ^:final ^int maxLocals]
    (set! inputLocals (new int/1 maxLocals))
    (set! inputStack (new int/1 0))
    (let [^:mutable ^int inputLocalIndex 0]
      (when (== (bit-and-int access Opcodes/ACC_STATIC) 0)
        (if (== (bit-and-int access Constants/ACC_CONSTRUCTOR) 0)
            (do
              (aset inputLocals
                    inputLocalIndex
                    (bit-or-int REFERENCE_KIND (.addType symbolTable (.getClassName symbolTable))))
              (set! inputLocalIndex (unchecked-inc-int inputLocalIndex)))
            (do
              (aset inputLocals inputLocalIndex UNINITIALIZED_THIS)
              (set! inputLocalIndex (unchecked-inc-int inputLocalIndex)))))
      (for-each [^Type argumentType (Type/getArgumentTypes descriptor)]
        (let [abstractType (Frame/getAbstractTypeFromDescriptor
                             symbolTable
                             (.getDescriptor argumentType)
                             0)]
          (aset inputLocals inputLocalIndex abstractType)
          (set! inputLocalIndex (unchecked-inc-int inputLocalIndex))
          (when (or (== abstractType LONG) (== abstractType DOUBLE))
            (aset inputLocals inputLocalIndex TOP)
            (set! inputLocalIndex (unchecked-inc-int inputLocalIndex)))))
      (while (< inputLocalIndex maxLocals)
        (aset inputLocals inputLocalIndex TOP)
        (set! inputLocalIndex (unchecked-inc-int inputLocalIndex)))))

  (method ^:final setInputFrameFromApiFormat ^void [this ^:final ^SymbolTable symbolTable
                                                    ^:final ^int numLocal ^:final ^Object/1 local
                                                    ^:final ^int numStack ^:final ^Object/1 stack]
    (let [^:mutable ^int inputLocalIndex 0]
      (loop [^int i 0]
        (when (< i numLocal)
          (aset inputLocals
                inputLocalIndex
                (Frame/getAbstractTypeFromApiFormat symbolTable (aget local i)))
          (set! inputLocalIndex (unchecked-inc-int inputLocalIndex))
          (if (or (identical? (aget local i) Opcodes/LONG)
                  (identical? (aget local i) Opcodes/DOUBLE))
              (do
                (aset inputLocals inputLocalIndex TOP)
                (set! inputLocalIndex (unchecked-inc-int inputLocalIndex))
                (recur (unchecked-inc-int i)))
              (recur (unchecked-inc-int i)))))
      (while (< inputLocalIndex (alength inputLocals))
        (aset inputLocals inputLocalIndex TOP)
        (set! inputLocalIndex (unchecked-inc-int inputLocalIndex)))
      (let [^:mutable ^int numStackTop 0]
        (loop [^int i 0]
          (if (< i numStack)
              (if (or (identical? (aget stack i) Opcodes/LONG)
                      (identical? (aget stack i) Opcodes/DOUBLE))
                  (do
                    (set! numStackTop (unchecked-inc-int numStackTop))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil))
        (set! inputStack (.checkNewIntArray limits (unchecked-add-int numStack numStackTop)))
        (let [^:mutable ^int inputStackIndex 0]
          (loop [^int i 0]
            (when (< i numStack)
              (aset inputStack
                    inputStackIndex
                    (Frame/getAbstractTypeFromApiFormat symbolTable (aget stack i)))
              (set! inputStackIndex (unchecked-inc-int inputStackIndex))
              (if (or (identical? (aget stack i) Opcodes/LONG)
                      (identical? (aget stack i) Opcodes/DOUBLE))
                  (do
                    (aset inputStack inputStackIndex TOP)
                    (set! inputStackIndex (unchecked-inc-int inputStackIndex))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))))
          (set! outputStackTop 0)
          (set! initializations nil)))))

  (method ^:final getInputStackSize ^int [this] (alength inputStack))

  (method ^:private getLocal ^int [this ^:final ^int localIndex]
    (if (or (nil? outputLocals) (>= localIndex (alength outputLocals)))
        (bit-or-int LOCAL_KIND localIndex)
        (let [^:mutable abstractType (aget outputLocals localIndex)]
          (when (== abstractType 0)
            (set! abstractType (aset outputLocals localIndex (bit-or-int LOCAL_KIND localIndex))))
          abstractType)))

  (method ^:private setLocal ^void [this ^:final ^int localIndex ^:final ^int abstractType]
    (when (nil? outputLocals) (set! outputLocals (.checkNewIntArray limits 10)))
    (let [outputLocalsLength (alength outputLocals)]
      (when (>= localIndex outputLocalsLength)
        (let [newOutputLocals (.checkNewIntArray
                                limits
                                (Math/max (unchecked-add-int localIndex 1)
                                          (unchecked-multiply-int 2 outputLocalsLength)))]
          (System/arraycopy outputLocals 0 newOutputLocals 0 outputLocalsLength)
          (set! outputLocals newOutputLocals)))
      (aset outputLocals localIndex abstractType)))

  (method ^:private push ^void [this ^:final ^int abstractType]
    (when (nil? outputStack) (set! outputStack (.checkNewIntArray limits 10)))
    (let [outputStackLength (alength outputStack)]
      (when (>= outputStackTop outputStackLength)
        (let [newOutputStack (.checkNewIntArray limits
                                                (Math/max
                                                  (unchecked-add-int outputStackTop 1)
                                                  (unchecked-multiply-int 2 outputStackLength)))]
          (System/arraycopy outputStack 0 newOutputStack 0 outputStackLength)
          (set! outputStack newOutputStack)))
      (aset outputStack
            (let [old-1 outputStackTop]
              (set! outputStackTop (unchecked-short (unchecked-inc-int outputStackTop)))
              old-1)
            abstractType)
      (let [outputStackSize (unchecked-short (unchecked-add-int outputStackStart outputStackTop))]
        (when (> outputStackSize (.-outputStackMax owner))
          (set! (.-outputStackMax owner) outputStackSize)))))

  (method ^:private push ^void [this ^:final ^SymbolTable symbolTable ^:final ^String descriptor]
    (let [typeDescriptorOffset (if (== (.charAt descriptor 0) \()
                                   (Type/getReturnTypeOffset descriptor)
                                   0)
          abstractType (Frame/getAbstractTypeFromDescriptor
                         symbolTable
                         descriptor
                         typeDescriptorOffset)]
      (when-not (== abstractType 0)
        (.push this abstractType)
        (when (or (== abstractType LONG) (== abstractType DOUBLE)) (.push this TOP)))))

  (method ^:private pop ^int [this]
    (if (> outputStackTop 0)
        (aget outputStack
              (set! outputStackTop (unchecked-short (unchecked-dec-int outputStackTop))))
        (do
          (set! outputStackStart (unchecked-short (unchecked-dec-int outputStackStart)))
          (bit-or-int STACK_KIND (unchecked-negate-int outputStackStart)))))

  (method ^:private pop ^void [this ^:final ^int elements]
    (if (>= outputStackTop elements)
        (set! outputStackTop (unchecked-short (unchecked-subtract-int outputStackTop elements)))
        (do
          (set! outputStackStart
                (unchecked-short
                  (unchecked-subtract-int outputStackStart
                                          (unchecked-subtract-int elements outputStackTop))))
          (set! outputStackTop 0))))

  (method ^:private pop ^void [this ^:final ^String descriptor]
    (let [firstDescriptorChar (.charAt descriptor 0)]
      (cond
        (== firstDescriptorChar \()
          (.pop this
                (unchecked-subtract-int
                  (bit-shift-right-int (Type/getArgumentsAndReturnSizes descriptor) 2)
                  1))
        (or (== firstDescriptorChar \J) (== firstDescriptorChar \D)) (^[int] Frame/.pop this 2)
        :else (^[int] Frame/.pop this 1))))

  (method ^:private addInitializedType ^void [this ^:final ^int abstractType]
    (when (nil? initializations) (set! initializations (.checkNewIntArray limits 3)))
    (let [initializationCount (aget initializations 0)
          initializationsCapacity (unchecked-subtract-int (alength initializations) 1)]
      (when (>= initializationCount initializationsCapacity)
        (let [newInitializations (.checkNewIntArray
                                   limits
                                   (Math/max (unchecked-add-int initializationCount 2)
                                             (unchecked-add-int
                                               (unchecked-multiply-int 2 initializationsCapacity)
                                               1)))]
          (System/arraycopy initializations
                            0
                            newInitializations
                            0
                            (unchecked-add-int initializationsCapacity 1))
          (set! initializations newInitializations)))
      (aset initializations (unchecked-add-int initializationCount 1) abstractType)
      (aset initializations 0 (unchecked-inc-int (aget initializations 0)))))

  (method ^:private getInitializedType ^int [this ^:final ^SymbolTable symbolTable
                                             ^:final ^int abstractType]
    (when (and (some? initializations)
               (or (or (== abstractType UNINITIALIZED_THIS)
                       (== (bit-and-int abstractType (bit-or-int DIM_MASK KIND_MASK))
                           UNINITIALIZED_KIND))
                   (== (bit-and-int abstractType (bit-or-int DIM_MASK KIND_MASK))
                       FORWARD_UNINITIALIZED_KIND)))
      (let [initializationCount (aget initializations 0)]
        (loop [^int i 0]
          (when (< i initializationCount)
            (let [^:mutable initializedType (aget initializations (unchecked-add-int i 1))
                  dim (bit-and-int initializedType DIM_MASK)
                  kind (bit-and-int initializedType KIND_MASK)
                  value (bit-and-int initializedType VALUE_MASK)]
              (cond
                (== kind LOCAL_KIND)
                  (set! initializedType (unchecked-add-int dim (aget inputLocals value)))
                (== kind STACK_KIND)
                  (set! initializedType
                        (unchecked-add-int dim
                                           (aget
                                             inputStack
                                             (unchecked-subtract-int (alength inputStack) value)))))
              (if (== abstractType initializedType)
                  (if (== abstractType UNINITIALIZED_THIS)
                      (return (bit-or-int REFERENCE_KIND
                                          (.addType symbolTable (.getClassName symbolTable))))
                      (return (bit-or-int REFERENCE_KIND
                                          (.addType
                                            symbolTable
                                            (.-value
                                              (.getType
                                                symbolTable
                                                (bit-and-int abstractType VALUE_MASK)))))))
                  (recur (unchecked-inc-int i))))))))
    abstractType)

  (method execute ^void [this ^:final ^int opcode ^:final ^int arg ^:final ^Symbol argSymbol
                         ^:final ^SymbolTable symbolTable]
    (let [^:mutable ^int abstractType1 0
          ^:mutable ^int abstractType2 0
          ^:mutable ^int abstractType3 0
          ^:mutable ^int abstractType4 0]
      (switch opcode
        (Opcodes/NOP Opcodes/INEG
                     Opcodes/LNEG
                     Opcodes/FNEG
                     Opcodes/DNEG
                     Opcodes/I2B
                     Opcodes/I2C
                     Opcodes/I2S
                     Opcodes/GOTO
                     Opcodes/RETURN)
          nil
        Opcodes/ACONST_NULL (.push this NULL)
        (Opcodes/ICONST_M1 Opcodes/ICONST_0
                           Opcodes/ICONST_1
                           Opcodes/ICONST_2
                           Opcodes/ICONST_3
                           Opcodes/ICONST_4
                           Opcodes/ICONST_5
                           Opcodes/BIPUSH
                           Opcodes/SIPUSH
                           Opcodes/ILOAD)
          (.push this INTEGER)
        (Opcodes/LCONST_0 Opcodes/LCONST_1 Opcodes/LLOAD) (do (.push this LONG) (.push this TOP))
        (Opcodes/FCONST_0 Opcodes/FCONST_1 Opcodes/FCONST_2 Opcodes/FLOAD) (.push this FLOAT)
        (Opcodes/DCONST_0 Opcodes/DCONST_1 Opcodes/DLOAD) (do (.push this DOUBLE) (.push this TOP))
        Opcodes/LDC
          (switch (.-tag argSymbol)
            Symbol/CONSTANT_INTEGER_TAG (.push this INTEGER)
            Symbol/CONSTANT_LONG_TAG (do (.push this LONG) (.push this TOP))
            Symbol/CONSTANT_FLOAT_TAG (.push this FLOAT)
            Symbol/CONSTANT_DOUBLE_TAG (do (.push this DOUBLE) (.push this TOP))
            Symbol/CONSTANT_CLASS_TAG
              (.push this (bit-or-int REFERENCE_KIND (.addType symbolTable "java/lang/Class")))
            Symbol/CONSTANT_STRING_TAG
              (.push this (bit-or-int REFERENCE_KIND (.addType symbolTable "java/lang/String")))
            Symbol/CONSTANT_METHOD_TYPE_TAG
              (.push this
                     (bit-or-int REFERENCE_KIND
                                 (.addType symbolTable "java/lang/invoke/MethodType")))
            Symbol/CONSTANT_METHOD_HANDLE_TAG
              (.push this
                     (bit-or-int REFERENCE_KIND
                                 (.addType symbolTable "java/lang/invoke/MethodHandle")))
            Symbol/CONSTANT_DYNAMIC_TAG (.push this symbolTable (.-value argSymbol))
            (throw (AssertionError.)))
        Opcodes/ALOAD (.push this (.getLocal this arg))
        (Opcodes/LALOAD Opcodes/D2L)
          (do (^[int] Frame/.pop this 2) (.push this LONG) (.push this TOP))
        (Opcodes/DALOAD Opcodes/L2D)
          (do (^[int] Frame/.pop this 2) (.push this DOUBLE) (.push this TOP))
        Opcodes/AALOAD
          (do
            (^[int] Frame/.pop this 1)
            (set! abstractType1 (.pop this))
            (.push this
                   (if (== abstractType1 NULL)
                       abstractType1
                       (unchecked-add-int ELEMENT_OF abstractType1))))
        (Opcodes/ISTORE Opcodes/FSTORE Opcodes/ASTORE)
          (do
            (set! abstractType1 (.pop this))
            (.setLocal this arg abstractType1)
            (when (> arg 0)
              (let [previousLocalType (.getLocal this (unchecked-subtract-int arg 1))]
                (cond
                  (or (== previousLocalType LONG) (== previousLocalType DOUBLE))
                    (.setLocal this (unchecked-subtract-int arg 1) TOP)
                  (or (== (bit-and-int previousLocalType KIND_MASK) LOCAL_KIND)
                      (== (bit-and-int previousLocalType KIND_MASK) STACK_KIND))
                    (.setLocal this
                               (unchecked-subtract-int arg 1)
                               (bit-or-int previousLocalType TOP_IF_LONG_OR_DOUBLE_FLAG))))))
        (Opcodes/LSTORE Opcodes/DSTORE)
          (do
            (^[int] Frame/.pop this 1)
            (set! abstractType1 (.pop this))
            (.setLocal this arg abstractType1)
            (.setLocal this (unchecked-add-int arg 1) TOP)
            (when (> arg 0)
              (let [previousLocalType (.getLocal this (unchecked-subtract-int arg 1))]
                (cond
                  (or (== previousLocalType LONG) (== previousLocalType DOUBLE))
                    (.setLocal this (unchecked-subtract-int arg 1) TOP)
                  (or (== (bit-and-int previousLocalType KIND_MASK) LOCAL_KIND)
                      (== (bit-and-int previousLocalType KIND_MASK) STACK_KIND))
                    (.setLocal this
                               (unchecked-subtract-int arg 1)
                               (bit-or-int previousLocalType TOP_IF_LONG_OR_DOUBLE_FLAG))))))
        (Opcodes/IASTORE Opcodes/BASTORE
                         Opcodes/CASTORE
                         Opcodes/SASTORE
                         Opcodes/FASTORE
                         Opcodes/AASTORE)
          (^[int] Frame/.pop this 3)
        (Opcodes/LASTORE Opcodes/DASTORE) (^[int] Frame/.pop this 4)
        (Opcodes/POP Opcodes/IFEQ
                     Opcodes/IFNE
                     Opcodes/IFLT
                     Opcodes/IFGE
                     Opcodes/IFGT
                     Opcodes/IFLE
                     Opcodes/IRETURN
                     Opcodes/FRETURN
                     Opcodes/ARETURN
                     Opcodes/TABLESWITCH
                     Opcodes/LOOKUPSWITCH
                     Opcodes/ATHROW
                     Opcodes/MONITORENTER
                     Opcodes/MONITOREXIT
                     Opcodes/IFNULL
                     Opcodes/IFNONNULL)
          (^[int] Frame/.pop this 1)
        (Opcodes/POP2 Opcodes/IF_ICMPEQ
                      Opcodes/IF_ICMPNE
                      Opcodes/IF_ICMPLT
                      Opcodes/IF_ICMPGE
                      Opcodes/IF_ICMPGT
                      Opcodes/IF_ICMPLE
                      Opcodes/IF_ACMPEQ
                      Opcodes/IF_ACMPNE
                      Opcodes/LRETURN
                      Opcodes/DRETURN)
          (^[int] Frame/.pop this 2)
        Opcodes/DUP
          (do
            (set! abstractType1 (.pop this))
            (.push this abstractType1)
            (.push this abstractType1))
        Opcodes/DUP_X1
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (.push this abstractType1)
            (.push this abstractType2)
            (.push this abstractType1))
        Opcodes/DUP_X2
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (set! abstractType3 (.pop this))
            (.push this abstractType1)
            (.push this abstractType3)
            (.push this abstractType2)
            (.push this abstractType1))
        Opcodes/DUP2
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (.push this abstractType2)
            (.push this abstractType1)
            (.push this abstractType2)
            (.push this abstractType1))
        Opcodes/DUP2_X1
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (set! abstractType3 (.pop this))
            (.push this abstractType2)
            (.push this abstractType1)
            (.push this abstractType3)
            (.push this abstractType2)
            (.push this abstractType1))
        Opcodes/DUP2_X2
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (set! abstractType3 (.pop this))
            (set! abstractType4 (.pop this))
            (.push this abstractType2)
            (.push this abstractType1)
            (.push this abstractType4)
            (.push this abstractType3)
            (.push this abstractType2)
            (.push this abstractType1))
        Opcodes/SWAP
          (do
            (set! abstractType1 (.pop this))
            (set! abstractType2 (.pop this))
            (.push this abstractType1)
            (.push this abstractType2))
        (Opcodes/IALOAD Opcodes/BALOAD
                        Opcodes/CALOAD
                        Opcodes/SALOAD
                        Opcodes/IADD
                        Opcodes/ISUB
                        Opcodes/IMUL
                        Opcodes/IDIV
                        Opcodes/IREM
                        Opcodes/IAND
                        Opcodes/IOR
                        Opcodes/IXOR
                        Opcodes/ISHL
                        Opcodes/ISHR
                        Opcodes/IUSHR
                        Opcodes/L2I
                        Opcodes/D2I
                        Opcodes/FCMPL
                        Opcodes/FCMPG)
          (do (^[int] Frame/.pop this 2) (.push this INTEGER))
        (Opcodes/LADD Opcodes/LSUB
                      Opcodes/LMUL
                      Opcodes/LDIV
                      Opcodes/LREM
                      Opcodes/LAND
                      Opcodes/LOR
                      Opcodes/LXOR)
          (do (^[int] Frame/.pop this 4) (.push this LONG) (.push this TOP))
        (Opcodes/FALOAD Opcodes/FADD
                        Opcodes/FSUB
                        Opcodes/FMUL
                        Opcodes/FDIV
                        Opcodes/FREM
                        Opcodes/L2F
                        Opcodes/D2F)
          (do (^[int] Frame/.pop this 2) (.push this FLOAT))
        (Opcodes/DADD Opcodes/DSUB Opcodes/DMUL Opcodes/DDIV Opcodes/DREM)
          (do (^[int] Frame/.pop this 4) (.push this DOUBLE) (.push this TOP))
        (Opcodes/LSHL Opcodes/LSHR Opcodes/LUSHR)
          (do (^[int] Frame/.pop this 3) (.push this LONG) (.push this TOP))
        Opcodes/IINC (.setLocal this arg INTEGER)
        (Opcodes/I2L Opcodes/F2L) (do (^[int] Frame/.pop this 1) (.push this LONG) (.push this TOP))
        Opcodes/I2F (do (^[int] Frame/.pop this 1) (.push this FLOAT))
        (Opcodes/I2D Opcodes/F2D)
          (do (^[int] Frame/.pop this 1) (.push this DOUBLE) (.push this TOP))
        (Opcodes/F2I Opcodes/ARRAYLENGTH Opcodes/INSTANCEOF)
          (do (^[int] Frame/.pop this 1) (.push this INTEGER))
        (Opcodes/LCMP Opcodes/DCMPL Opcodes/DCMPG)
          (do (^[int] Frame/.pop this 4) (.push this INTEGER))
        (Opcodes/JSR Opcodes/RET)
          (throw (IllegalArgumentException. "JSR/RET are not supported with computeFrames option"))
        Opcodes/GETSTATIC (.push this symbolTable (.-value argSymbol))
        Opcodes/PUTSTATIC (.pop this (.-value argSymbol))
        Opcodes/GETFIELD
          (do (^[int] Frame/.pop this 1) (.push this symbolTable (.-value argSymbol)))
        Opcodes/PUTFIELD (do (.pop this (.-value argSymbol)) (.pop this))
        (Opcodes/INVOKEVIRTUAL Opcodes/INVOKESPECIAL Opcodes/INVOKESTATIC Opcodes/INVOKEINTERFACE)
          (do
            (.pop this (.-value argSymbol))
            (when-not (== opcode Opcodes/INVOKESTATIC)
              (set! abstractType1 (.pop this))
              (when (and (== opcode Opcodes/INVOKESPECIAL) (== (.charAt (.-name argSymbol) 0) \<))
                (.addInitializedType this abstractType1)))
            (.push this symbolTable (.-value argSymbol)))
        Opcodes/INVOKEDYNAMIC
          (do (.pop this (.-value argSymbol)) (.push this symbolTable (.-value argSymbol)))
        Opcodes/NEW
          (.push this
                 (bit-or-int UNINITIALIZED_KIND
                             (.addUninitializedType symbolTable (.-value argSymbol) arg)))
        Opcodes/NEWARRAY
          (do
            (.pop this)
            (switch arg
              Opcodes/T_BOOLEAN (.push this (bit-or-int ARRAY_OF BOOLEAN))
              Opcodes/T_CHAR (.push this (bit-or-int ARRAY_OF CHAR))
              Opcodes/T_BYTE (.push this (bit-or-int ARRAY_OF BYTE))
              Opcodes/T_SHORT (.push this (bit-or-int ARRAY_OF SHORT))
              Opcodes/T_INT (.push this (bit-or-int ARRAY_OF INTEGER))
              Opcodes/T_FLOAT (.push this (bit-or-int ARRAY_OF FLOAT))
              Opcodes/T_DOUBLE (.push this (bit-or-int ARRAY_OF DOUBLE))
              Opcodes/T_LONG (.push this (bit-or-int ARRAY_OF LONG))
              (throw (IllegalArgumentException.))))
        Opcodes/ANEWARRAY
          (let [arrayElementType (.-value argSymbol)]
            (.pop this)
            (if (== (.charAt arrayElementType 0) \[)
                (.push this symbolTable (java-str \[ arrayElementType))
                (.push this
                       (bit-or-int (bit-or-int ARRAY_OF REFERENCE_KIND)
                                   (.addType symbolTable arrayElementType)))))
        Opcodes/CHECKCAST
          (let [castType (.-value argSymbol)]
            (.pop this)
            (if (== (.charAt castType 0) \[)
                (.push this symbolTable castType)
                (.push this (bit-or-int REFERENCE_KIND (.addType symbolTable castType)))))
        Opcodes/MULTIANEWARRAY (do (.pop this arg) (.push this symbolTable (.-value argSymbol)))
        (throw (IllegalArgumentException.)))))

  (method ^:private getConcreteOutputType ^int [this ^:final ^int abstractOutputType
                                                ^:final ^int numStack]
    (let [dim (bit-and-int abstractOutputType DIM_MASK)
          kind (bit-and-int abstractOutputType KIND_MASK)]
      (cond
        (== kind LOCAL_KIND)
          (let [^:mutable concreteOutputType (unchecked-add-int
                                               dim
                                               (aget
                                                 inputLocals
                                                 (bit-and-int abstractOutputType VALUE_MASK)))]
            (when (and (not (== (bit-and-int abstractOutputType TOP_IF_LONG_OR_DOUBLE_FLAG) 0))
                       (or (== concreteOutputType LONG) (== concreteOutputType DOUBLE)))
              (set! concreteOutputType TOP))
            concreteOutputType)
        (== kind STACK_KIND)
          (let [^:mutable concreteOutputType (unchecked-add-int
                                               dim
                                               (aget
                                                 inputStack
                                                 (unchecked-subtract-int
                                                   numStack
                                                   (bit-and-int abstractOutputType VALUE_MASK))))]
            (when (and (not (== (bit-and-int abstractOutputType TOP_IF_LONG_OR_DOUBLE_FLAG) 0))
                       (or (== concreteOutputType LONG) (== concreteOutputType DOUBLE)))
              (set! concreteOutputType TOP))
            concreteOutputType)
        :else abstractOutputType)))

  (method ^:final merge ^boolean [this ^:final ^SymbolTable symbolTable ^:final ^Frame dstFrame
                                  ^:final ^int catchTypeIndex]
    (let [^:mutable frameChanged false
          numLocal (alength inputLocals)
          numStack (alength inputStack)]
      (when (nil? (.-inputLocals dstFrame))
        (set! (.-inputLocals dstFrame) (.checkNewIntArray limits numLocal))
        (set! frameChanged true))
      (let [^:mutable ^int numMerges 0]
        (loop [^int i 0]
          (when (< i numLocal)
            (let [^:mutable ^int concreteOutputType 0]
              (if (and (some? outputLocals) (< i (alength outputLocals)))
                  (let [abstractOutputType (aget outputLocals i)]
                    (if (== abstractOutputType 0)
                        (set! concreteOutputType (aget inputLocals i))
                        (set! concreteOutputType
                              (.getConcreteOutputType this abstractOutputType numStack))))
                  (set! concreteOutputType (aget inputLocals i)))
              (when (some? initializations)
                (set! concreteOutputType (.getInitializedType this symbolTable concreteOutputType)))
              (set! frameChanged
                    (let [a-2 frameChanged
                          b-3 (Frame/merge symbolTable
                                           concreteOutputType
                                           (.-inputLocals dstFrame)
                                           i)]
                      (or a-2 b-3)))
              (recur (unchecked-inc-int i)))))
        (set! numMerges (unchecked-add-int numMerges numLocal))
        (if (> catchTypeIndex 0)
            (do
              (loop [^int i 0]
                (when (< i numLocal)
                  (set! frameChanged
                        (let [a-4 frameChanged
                              b-5 (Frame/merge symbolTable
                                               (aget inputLocals i)
                                               (.-inputLocals dstFrame)
                                               i)]
                          (or a-4 b-5)))
                  (recur (unchecked-inc-int i))))
              (when (nil? (.-inputStack dstFrame))
                (set! (.-inputStack dstFrame) (.checkNewIntArray limits 1))
                (set! frameChanged true))
              (set! frameChanged
                    (let [a-6 frameChanged
                          b-7 (Frame/merge symbolTable catchTypeIndex (.-inputStack dstFrame) 0)]
                      (or a-6 b-7)))
              (set! numMerges (unchecked-add-int numMerges (unchecked-add-int numLocal 1)))
              (.checkNewOperations limits
                                   (unchecked-multiply-int numMerges NUM_OPERATIONS_PER_MERGE))
              frameChanged)
            (let [numInputStack (unchecked-add-int (alength inputStack) outputStackStart)]
              (when (nil? (.-inputStack dstFrame))
                (set! (.-inputStack dstFrame)
                      (.checkNewIntArray limits (unchecked-add-int numInputStack outputStackTop)))
                (set! frameChanged true))
              (loop [^int i 0]
                (when (< i numInputStack)
                  (let [^:mutable concreteOutputType (aget inputStack i)]
                    (when (some? initializations)
                      (set! concreteOutputType
                            (.getInitializedType this symbolTable concreteOutputType)))
                    (set! frameChanged
                          (let [a-8 frameChanged
                                b-9 (Frame/merge
                                      symbolTable
                                      concreteOutputType
                                      (.-inputStack dstFrame)
                                      i)]
                            (or a-8 b-9)))
                    (recur (unchecked-inc-int i)))))
              (set! numMerges (unchecked-add-int numMerges numInputStack))
              (loop [^int i 0]
                (when (< i outputStackTop)
                  (let [abstractOutputType (aget outputStack i)
                        ^:mutable concreteOutputType (.getConcreteOutputType
                                                       this
                                                       abstractOutputType
                                                       numStack)]
                    (when (some? initializations)
                      (set! concreteOutputType
                            (.getInitializedType this symbolTable concreteOutputType)))
                    (set! frameChanged
                          (let [a-10 frameChanged
                                b-11 (Frame/merge
                                       symbolTable
                                       concreteOutputType
                                       (.-inputStack dstFrame)
                                       (unchecked-add-int numInputStack i))]
                            (or a-10 b-11)))
                    (recur (unchecked-inc-int i)))))
              (set! numMerges (unchecked-add-int numMerges outputStackTop))
              (.checkNewOperations limits
                                   (unchecked-multiply-int numMerges NUM_OPERATIONS_PER_MERGE))
              frameChanged)))))

  (method ^:private ^:static merge ^boolean [^:final ^SymbolTable symbolTable
                                             ^:final ^int sourceType ^:final ^int/1 dstTypes
                                             ^:final ^int dstIndex]
    (let [dstType (aget dstTypes dstIndex)]
      (if (== dstType sourceType)
          false
          (let [^:mutable srcType sourceType]
            (when (== (bit-and-int sourceType (bit-not-int DIM_MASK)) NULL)
              (when (== dstType NULL) (return false))
              (set! srcType NULL))
            (if (== dstType 0)
                (do (aset dstTypes dstIndex srcType) true)
                (let [^:mutable ^int mergedType 0]
                  (cond
                    (or (not (== (bit-and-int dstType DIM_MASK) 0))
                        (== (bit-and-int dstType KIND_MASK) REFERENCE_KIND))
                      (cond
                        (== srcType NULL) (return false)
                        (== (bit-and-int srcType (bit-or-int DIM_MASK KIND_MASK))
                            (bit-and-int dstType (bit-or-int DIM_MASK KIND_MASK)))
                          (if (== (bit-and-int dstType KIND_MASK) REFERENCE_KIND)
                              (set! mergedType
                                    (bit-or-int (bit-or-int
                                                  (bit-and-int srcType DIM_MASK)
                                                  REFERENCE_KIND)
                                                (.addMergedType
                                                  symbolTable
                                                  (bit-and-int srcType VALUE_MASK)
                                                  (bit-and-int dstType VALUE_MASK))))
                              (let [mergedDim (unchecked-add-int
                                                ELEMENT_OF
                                                (bit-and-int srcType DIM_MASK))]
                                (set! mergedType
                                      (bit-or-int
                                        (bit-or-int mergedDim REFERENCE_KIND)
                                        (.addType symbolTable "java/lang/Object")))))
                        (or (not (== (bit-and-int srcType DIM_MASK) 0))
                            (== (bit-and-int srcType KIND_MASK) REFERENCE_KIND))
                          (let [^:mutable srcDim (bit-and-int srcType DIM_MASK)]
                            (when (and (not (== srcDim 0))
                                       (not (== (bit-and-int srcType KIND_MASK) REFERENCE_KIND)))
                              (set! srcDim (unchecked-add-int ELEMENT_OF srcDim)))
                            (let [^:mutable dstDim (bit-and-int dstType DIM_MASK)]
                              (when (and (not (== dstDim 0))
                                         (not (== (bit-and-int dstType KIND_MASK) REFERENCE_KIND)))
                                (set! dstDim (unchecked-add-int ELEMENT_OF dstDim)))
                              (set! mergedType
                                    (bit-or-int (bit-or-int (Math/min srcDim dstDim) REFERENCE_KIND)
                                                (.addType symbolTable "java/lang/Object")))))
                        :else (set! mergedType TOP))
                    (== dstType NULL)
                      (set! mergedType
                            (if (or (not (== (bit-and-int srcType DIM_MASK) 0))
                                    (== (bit-and-int srcType KIND_MASK) REFERENCE_KIND))
                                srcType
                                TOP))
                    :else (set! mergedType TOP))
                  (if (not (== mergedType dstType))
                      (do (aset dstTypes dstIndex mergedType) true)
                      false)))))))

  (method ^:final accept ^void [this ^:final ^MethodWriter methodWriter]
    (let [localTypes inputLocals
          ^:mutable ^int numLocal 0
          ^:mutable ^int numTrailingTop 0
          ^:mutable ^int i 0]
      (while (< i (alength localTypes))
        (let [localType (aget localTypes i)]
          (set! i (unchecked-add-int i (if (or (== localType LONG) (== localType DOUBLE)) 2 1)))
          (if (== localType TOP)
              (set! numTrailingTop (unchecked-inc-int numTrailingTop))
              (do
                (set! numLocal (unchecked-add-int numLocal (unchecked-add-int numTrailingTop 1)))
                (set! numTrailingTop 0)))))
      (let [stackTypes inputStack
            ^:mutable ^int numStack 0]
        (set! i 0)
        (while (< i (alength stackTypes))
          (let [stackType (aget stackTypes i)]
            (set! i (unchecked-add-int i (if (or (== stackType LONG) (== stackType DOUBLE)) 2 1)))
            (set! numStack (unchecked-inc-int numStack))))
        (let [^:mutable frameIndex (.visitFrameStart
                                     methodWriter
                                     (.-bytecodeOffset owner)
                                     numLocal
                                     numStack)]
          (set! i 0)
          (while (> (let [old-12 numLocal] (set! numLocal (unchecked-dec-int numLocal)) old-12) 0)
            (let [localType (aget localTypes i)]
              (set! i (unchecked-add-int i (if (or (== localType LONG) (== localType DOUBLE)) 2 1)))
              (.visitAbstractType methodWriter frameIndex localType)
              (set! frameIndex (unchecked-inc-int frameIndex))))
          (set! i 0)
          (while (> (let [old-13 numStack] (set! numStack (unchecked-dec-int numStack)) old-13) 0)
            (let [stackType (aget stackTypes i)]
              (set! i (unchecked-add-int i (if (or (== stackType LONG) (== stackType DOUBLE)) 2 1)))
              (.visitAbstractType methodWriter frameIndex stackType)
              (set! frameIndex (unchecked-inc-int frameIndex))))
          (.visitFrameEnd methodWriter)))))

  (method ^:static putAbstractType ^void [^:final ^SymbolTable symbolTable ^:final ^int abstractType
                                          ^:final ^ByteVector output]
    (let [^:mutable arrayDimensions (bit-shift-right-int
                                      (bit-and-int abstractType Frame/DIM_MASK)
                                      DIM_SHIFT)]
      (if (== arrayDimensions 0)
          (let [typeValue (bit-and-int abstractType VALUE_MASK)]
            (switch (bit-and-int abstractType KIND_MASK)
              CONSTANT_KIND (.putByte output typeValue)
              REFERENCE_KIND
                (.putShort (.putByte output ITEM_OBJECT)
                           (.-index (.addConstantClass
                                      symbolTable
                                      (.-value (.getType symbolTable typeValue)))))
              UNINITIALIZED_KIND
                (.putShort (.putByte output ITEM_UNINITIALIZED)
                           (unchecked-int (.-data (.getType symbolTable typeValue))))
              FORWARD_UNINITIALIZED_KIND
                (do
                  (.putByte output ITEM_UNINITIALIZED)
                  (.put (.getForwardUninitializedLabel symbolTable typeValue) output))
              (throw (AssertionError.))))
          (let [typeDescriptor (StringBuilder.)]
            (while (> (let [old-14 arrayDimensions]
                        (set! arrayDimensions (unchecked-dec-int arrayDimensions))
                        old-14)
                      0)
              (^[char] StringBuilder/.append typeDescriptor \[))
            (if (== (bit-and-int abstractType KIND_MASK) REFERENCE_KIND)
                (^[char] StringBuilder/.append
                  (.append (^[char] StringBuilder/.append typeDescriptor \L)
                           (.-value (.getType symbolTable (bit-and-int abstractType VALUE_MASK))))
                  \;)
                (switch (bit-and-int abstractType VALUE_MASK)
                  Frame/ITEM_ASM_BOOLEAN (^[char] StringBuilder/.append typeDescriptor \Z)
                  Frame/ITEM_ASM_BYTE (^[char] StringBuilder/.append typeDescriptor \B)
                  Frame/ITEM_ASM_CHAR (^[char] StringBuilder/.append typeDescriptor \C)
                  Frame/ITEM_ASM_SHORT (^[char] StringBuilder/.append typeDescriptor \S)
                  Frame/ITEM_INTEGER (^[char] StringBuilder/.append typeDescriptor \I)
                  Frame/ITEM_FLOAT (^[char] StringBuilder/.append typeDescriptor \F)
                  Frame/ITEM_LONG (^[char] StringBuilder/.append typeDescriptor \J)
                  Frame/ITEM_DOUBLE (^[char] StringBuilder/.append typeDescriptor \D)
                  (throw (AssertionError.))))
            (.putShort (.putByte output ITEM_OBJECT)
                       (.-index (.addConstantClass symbolTable (.toString typeDescriptor)))))))))
