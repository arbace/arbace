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
;; Converted from clojure/asm/MethodWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final MethodWriter
  :extends MethodVisitor

  (field ^:static ^:final ^int COMPUTE_NOTHING 0)

  (field ^:static ^:final ^int COMPUTE_MAX_STACK_AND_LOCAL 1)

  (field ^:static ^:final ^int COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES 2)

  (field ^:static ^:final ^int COMPUTE_INSERTED_FRAMES 3)

  (field ^:static ^:final ^int COMPUTE_ALL_FRAMES 4)

  (field ^:private ^:static ^:final ^int NA 0)

  (field ^:private ^:static ^:final ^int/1 STACK_SIZE_DELTA
    (new int/1
         [0 1 1 1 1 1 1 1 1 2 2 1 1 1 2 2 1 1 1 NA NA 1 2 1 2 1 NA NA NA NA NA NA NA NA NA NA NA NA
          NA NA NA NA NA NA NA NA -1 0 -1 0 -1 -1 -1 -1 -1 -2 -1 -2 -1 NA NA NA NA NA NA NA NA NA NA
          NA NA NA NA NA NA NA NA NA NA -3 -4 -3 -4 -3 -3 -3 -3 -1 -2 1 1 1 2 2 2 0 -1 -2 -1 -2 -1
          -2 -1 -2 -1 -2 -1 -2 -1 -2 -1 -2 -1 -2 -1 -2 0 0 0 0 -1 -1 -1 -1 -1 -1 -1 -2 -1 -2 -1 -2 0
          1 0 1 -1 -1 0 0 1 1 -1 0 -1 0 0 0 -3 -1 -1 -3 -3 -1 -1 -1 -1 -1 -1 -2 -2 -2 -2 -2 -2 -2 -2
          0 1 0 -1 -1 -1 -2 -1 -2 -1 0 NA NA NA NA NA NA NA NA NA 1 0 0 0 NA 0 0 -1 -1 NA NA -1 -1
          NA NA]))

  (field ^:private ^:final ^SymbolTable symbolTable)

  (field ^:private ^:final ^int accessFlags)

  (field ^:private ^:final ^int nameIndex)

  (field ^:private ^:final ^String name)

  (field ^:private ^:final ^int descriptorIndex)

  (field ^:private ^:final ^String descriptor)

  (field ^:private ^int maxStack)

  (field ^:private ^int maxLocals)

  (field ^:private ^:final ^ByteVector code (ByteVector.))

  (field ^:private ^Handler firstHandler)

  (field ^:private ^Handler lastHandler)

  (field ^:private ^int lineNumberTableLength)

  (field ^:private ^ByteVector lineNumberTable)

  (field ^:private ^int localVariableTableLength)

  (field ^:private ^ByteVector localVariableTable)

  (field ^:private ^int localVariableTypeTableLength)

  (field ^:private ^ByteVector localVariableTypeTable)

  (field ^:private ^int stackMapTableNumberOfEntries)

  (field ^:private ^ByteVector stackMapTableEntries)

  (field ^:private ^AnnotationWriter lastCodeRuntimeVisibleTypeAnnotation)

  (field ^:private ^AnnotationWriter lastCodeRuntimeInvisibleTypeAnnotation)

  (field ^:private ^Attribute firstCodeAttribute)

  (field ^:private ^:final ^int numberOfExceptions)

  (field ^:private ^:final ^int/1 exceptionIndexTable)

  (field ^:private ^:final ^int signatureIndex)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleAnnotation)

  (field ^:private ^int visibleAnnotableParameterCount)

  (field ^:private ^AnnotationWriter/1 lastRuntimeVisibleParameterAnnotations)

  (field ^:private ^int invisibleAnnotableParameterCount)

  (field ^:private ^AnnotationWriter/1 lastRuntimeInvisibleParameterAnnotations)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleTypeAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleTypeAnnotation)

  (field ^:private ^ByteVector defaultValue)

  (field ^:private ^int parametersCount)

  (field ^:private ^ByteVector parameters)

  (field ^:private ^Attribute firstAttribute)

  (field ^:private ^:final ^int compute)

  (field ^:private ^:final ^ComputeLimits limits)

  (field ^:private ^Label firstBasicBlock)

  (field ^:private ^Label lastBasicBlock)

  (field ^:private ^Label currentBasicBlock)

  (field ^:private ^int relativeStackSize)

  (field ^:private ^int maxRelativeStackSize)

  (field ^:private ^int currentLocals)

  (field ^:private ^int previousFrameOffset)

  (field ^:private ^int/1 previousFrame)

  (field ^:private ^int/1 currentFrame)

  (field ^:private ^boolean hasSubroutines)

  (field ^:private ^boolean hasAsmInstructions)

  (field ^:private ^int lastBytecodeOffset)

  (field ^:private ^int sourceOffset)

  (field ^:private ^int sourceLength)

  (constructor [this ^:final ^SymbolTable symbolTable ^:final ^int access ^:final ^String name
                ^:final ^String descriptor ^:final ^String signature ^:final ^String/1 exceptions
                ^:final ^int compute ^:final ^ComputeLimits limits]
    (super. Opcodes/ASM9)
    (set! (.-symbolTable this) symbolTable)
    (set! (.-accessFlags this)
          (if (.equals "<init>" name) (bit-or-int access Constants/ACC_CONSTRUCTOR) access))
    (set! (.-nameIndex this) (.addConstantUtf8 symbolTable name))
    (set! (.-name this) name)
    (set! (.-descriptorIndex this) (.addConstantUtf8 symbolTable descriptor))
    (set! (.-descriptor this) descriptor)
    (set! (.-signatureIndex this) (if (nil? signature) 0 (.addConstantUtf8 symbolTable signature)))
    (if (and (some? exceptions) (> (alength exceptions) 0))
        (do
          (set! numberOfExceptions (alength exceptions))
          (set! (.-exceptionIndexTable this) (new int/1 numberOfExceptions))
          (loop [^int i 0]
            (when (< i numberOfExceptions)
              (aset (.-exceptionIndexTable this)
                    i
                    (.-index (.addConstantClass symbolTable (aget exceptions i))))
              (recur (unchecked-inc-int i)))))
        (do (set! numberOfExceptions 0) (set! (.-exceptionIndexTable this) nil)))
    (set! (.-compute this) compute)
    (set! (.-limits this) limits)
    (when-not (== compute COMPUTE_NOTHING)
      (let [^:mutable argumentsSize (bit-shift-right-int
                                      (Type/getArgumentsAndReturnSizes descriptor)
                                      2)]
        (when-not (== (bit-and-int access Opcodes/ACC_STATIC) 0)
          (set! argumentsSize (unchecked-dec-int argumentsSize)))
        (set! maxLocals argumentsSize)
        (set! currentLocals argumentsSize)
        (set! firstBasicBlock (Label.))
        (.visitLabel this firstBasicBlock))))

  (method hasFrames ^boolean [this] (> stackMapTableNumberOfEntries 0))

  (method hasAsmInstructions ^boolean [this] hasAsmInstructions)

  (method ^:public visitParameter ^void [this ^:final ^String name ^:final ^int access]
    (when (nil? parameters) (set! parameters (ByteVector.)))
    (set! parametersCount (unchecked-inc-int parametersCount))
    (.putShort (.putShort parameters (if (nil? name) 0 (.addConstantUtf8 symbolTable name))) access))

  (method ^:public visitAnnotationDefault ^AnnotationVisitor [this]
    (set! defaultValue (ByteVector.))
    (AnnotationWriter. symbolTable false defaultValue nil))

  (method ^:public visitAnnotation ^AnnotationVisitor [this ^:final ^String descriptor
                                                       ^:final ^boolean visible]
    (if visible
        (set! lastRuntimeVisibleAnnotation
              (AnnotationWriter/create symbolTable descriptor lastRuntimeVisibleAnnotation))
        (set! lastRuntimeInvisibleAnnotation
              (AnnotationWriter/create symbolTable descriptor lastRuntimeInvisibleAnnotation))))

  (method ^:public visitTypeAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (if visible
        (set! lastRuntimeVisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       typeRef
                                       typePath
                                       descriptor
                                       lastRuntimeVisibleTypeAnnotation))
        (set! lastRuntimeInvisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       typeRef
                                       typePath
                                       descriptor
                                       lastRuntimeInvisibleTypeAnnotation))))

  (method ^:public visitAnnotableParameterCount ^void [this ^:final ^int parameterCount
                                                       ^:final ^boolean visible]
    (if visible
        (set! visibleAnnotableParameterCount parameterCount)
        (set! invisibleAnnotableParameterCount parameterCount)))

  (method ^:public visitParameterAnnotation ^AnnotationVisitor [this ^:final ^int parameter
                                                                ^:final ^String annotationDescriptor
                                                                ^:final ^boolean visible]
    (if visible
        (do
          (when (nil? lastRuntimeVisibleParameterAnnotations)
            (set! lastRuntimeVisibleParameterAnnotations
                  (new AnnotationWriter/1 (Type/getArgumentCount descriptor))))
          (aset lastRuntimeVisibleParameterAnnotations
                parameter
                (AnnotationWriter/create symbolTable
                                         annotationDescriptor
                                         (aget lastRuntimeVisibleParameterAnnotations parameter))))
        (do
          (when (nil? lastRuntimeInvisibleParameterAnnotations)
            (set! lastRuntimeInvisibleParameterAnnotations
                  (new AnnotationWriter/1 (Type/getArgumentCount descriptor))))
          (aset lastRuntimeInvisibleParameterAnnotations
                parameter
                (AnnotationWriter/create symbolTable
                                         annotationDescriptor
                                         (aget lastRuntimeInvisibleParameterAnnotations parameter))))))

  (method ^:public visitAttribute ^void [this ^:final ^Attribute attribute]
    (if (.isCodeAttribute attribute)
        (do
          (set! (.-nextAttribute attribute) firstCodeAttribute)
          (set! firstCodeAttribute attribute))
        (do (set! (.-nextAttribute attribute) firstAttribute) (set! firstAttribute attribute))))

  (method ^:public visitCode ^void [this])

  (method ^:public visitFrame ^void [this ^:final ^int type ^:final ^int numLocal
                                     ^:final ^Object/1 local ^:final ^int numStack
                                     ^:final ^Object/1 stack]
    (when-not (== compute COMPUTE_ALL_FRAMES)
      (cond
        (== compute COMPUTE_INSERTED_FRAMES)
          (if (nil? (.-frame currentBasicBlock))
              (do
                (set! (.-frame currentBasicBlock) (CurrentFrame. currentBasicBlock limits))
                (.setInputFrameFromDescriptor (.-frame currentBasicBlock)
                                              symbolTable
                                              accessFlags
                                              descriptor
                                              numLocal)
                (.accept (.-frame currentBasicBlock) this))
              (do
                (when (== type Opcodes/F_NEW)
                  (.setInputFrameFromApiFormat (.-frame currentBasicBlock)
                                               symbolTable
                                               numLocal
                                               local
                                               numStack
                                               stack))
                (.accept (.-frame currentBasicBlock) this)))
        (== type Opcodes/F_NEW)
          (do
            (when (nil? previousFrame)
              (let [argumentsSize (bit-shift-right-int
                                    (Type/getArgumentsAndReturnSizes descriptor)
                                    2)
                    implicitFirstFrame (Frame. (Label.) limits)]
                (.setInputFrameFromDescriptor implicitFirstFrame
                                              symbolTable
                                              accessFlags
                                              descriptor
                                              argumentsSize)
                (.accept implicitFirstFrame this)))
            (set! currentLocals numLocal)
            (let [^:mutable frameIndex (.visitFrameStart this (.-length code) numLocal numStack)]
              (loop [^int i 0]
                (when (< i numLocal)
                  (aset currentFrame
                        frameIndex
                        (Frame/getAbstractTypeFromApiFormat symbolTable (aget local i)))
                  (set! frameIndex (unchecked-inc-int frameIndex))
                  (recur (unchecked-inc-int i))))
              (loop [^int i 0]
                (when (< i numStack)
                  (aset currentFrame
                        frameIndex
                        (Frame/getAbstractTypeFromApiFormat symbolTable (aget stack i)))
                  (set! frameIndex (unchecked-inc-int frameIndex))
                  (recur (unchecked-inc-int i))))
              (.visitFrameEnd this)))
        :else
          (do
            (when (< (.getMajorVersion symbolTable) Opcodes/V1_6)
              (throw (IllegalArgumentException.
                       "Class versions V1_5 or less must use F_NEW frames.")))
            (let [^:mutable ^int offsetDelta 0]
              (if (nil? stackMapTableEntries)
                  (do (set! stackMapTableEntries (ByteVector.)) (set! offsetDelta (.-length code)))
                  (do
                    (set! offsetDelta
                          (unchecked-subtract-int
                            (unchecked-subtract-int (.-length code) previousFrameOffset)
                            1))
                    (when (< offsetDelta 0)
                      (if (== type Opcodes/F_SAME) (return) (throw (IllegalStateException.))))))
              (switch type
                Opcodes/F_FULL
                  (do
                    (set! currentLocals numLocal)
                    (.putShort (.putShort (.putByte stackMapTableEntries Frame/FULL_FRAME)
                                          offsetDelta)
                               numLocal)
                    (loop [^int i 0]
                      (when (< i numLocal)
                        (.putFrameType this (aget local i))
                        (recur (unchecked-inc-int i))))
                    (.putShort stackMapTableEntries numStack)
                    (loop [^int i 0]
                      (when (< i numStack)
                        (.putFrameType this (aget stack i))
                        (recur (unchecked-inc-int i)))))
                Opcodes/F_APPEND
                  (do
                    (set! currentLocals (unchecked-add-int currentLocals numLocal))
                    (.putShort (.putByte stackMapTableEntries
                                         (unchecked-add-int Frame/SAME_FRAME_EXTENDED numLocal))
                               offsetDelta)
                    (loop [^int i 0]
                      (when (< i numLocal)
                        (.putFrameType this (aget local i))
                        (recur (unchecked-inc-int i)))))
                Opcodes/F_CHOP
                  (do
                    (set! currentLocals (unchecked-subtract-int currentLocals numLocal))
                    (.putShort (.putByte
                                 stackMapTableEntries
                                 (unchecked-subtract-int Frame/SAME_FRAME_EXTENDED numLocal))
                               offsetDelta))
                Opcodes/F_SAME
                  (if (< offsetDelta 64)
                      (.putByte stackMapTableEntries offsetDelta)
                      (.putShort (.putByte stackMapTableEntries Frame/SAME_FRAME_EXTENDED)
                                 offsetDelta))
                Opcodes/F_SAME1
                  (do
                    (if (< offsetDelta 64)
                        (.putByte stackMapTableEntries
                                  (unchecked-add-int
                                    Frame/SAME_LOCALS_1_STACK_ITEM_FRAME
                                    offsetDelta))
                        (.putShort (.putByte stackMapTableEntries
                                             Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                                   offsetDelta))
                    (.putFrameType this (aget stack 0)))
                (throw (IllegalArgumentException.)))
              (set! previousFrameOffset (.-length code))
              (set! stackMapTableNumberOfEntries (unchecked-inc-int stackMapTableNumberOfEntries)))))
      (when (== compute COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES)
        (set! relativeStackSize numStack)
        (loop [^int i 0]
          (if (< i numStack)
              (if (or (identical? (aget stack i) Opcodes/LONG)
                      (identical? (aget stack i) Opcodes/DOUBLE))
                  (do
                    (set! relativeStackSize (unchecked-inc-int relativeStackSize))
                    (recur (unchecked-inc-int i)))
                  (recur (unchecked-inc-int i)))
              nil))
        (when (> relativeStackSize maxRelativeStackSize)
          (set! maxRelativeStackSize relativeStackSize)))
      (set! maxStack (Math/max maxStack numStack))
      (set! maxLocals (Math/max maxLocals currentLocals))))

  (method ^:public visitInsn ^void [this ^:final ^int opcode]
    (set! lastBytecodeOffset (.-length code))
    (.putByte code opcode)
    (when (some? currentBasicBlock)
      (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
          (.execute (.-frame currentBasicBlock) opcode 0 nil nil)
          (let [size (unchecked-add-int relativeStackSize (aget STACK_SIZE_DELTA opcode))]
            (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
            (set! relativeStackSize size)))
      (when (or (and (>= opcode Opcodes/IRETURN) (<= opcode Opcodes/RETURN))
                (== opcode Opcodes/ATHROW))
        (.endCurrentBasicBlockWithNoSuccessor this))))

  (method ^:public visitIntInsn ^void [this ^:final ^int opcode ^:final ^int operand]
    (set! lastBytecodeOffset (.-length code))
    (if (== opcode Opcodes/SIPUSH) (.put12 code opcode operand) (.put11 code opcode operand))
    (when (some? currentBasicBlock)
      (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
          (.execute (.-frame currentBasicBlock) opcode operand nil nil)
          (when-not (== opcode Opcodes/NEWARRAY)
            (let [size (unchecked-add-int relativeStackSize 1)]
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitVarInsn ^void [this ^:final ^int opcode ^:final ^int varIndex]
    (set! lastBytecodeOffset (.-length code))
    (cond
      (and (< varIndex 4) (not (== opcode Opcodes/RET)))
        (let [^int optimizedOpcode (if (< opcode Opcodes/ISTORE)
                                       (unchecked-add-int
                                         (unchecked-add-int
                                           Constants/ILOAD_0
                                           (bit-shift-left-int
                                             (unchecked-subtract-int opcode Opcodes/ILOAD)
                                             2))
                                         varIndex)
                                       (unchecked-add-int
                                         (unchecked-add-int
                                           Constants/ISTORE_0
                                           (bit-shift-left-int
                                             (unchecked-subtract-int opcode Opcodes/ISTORE)
                                             2))
                                         varIndex))]
          (.putByte code optimizedOpcode))
      (>= varIndex 256) (.put12 (.putByte code Constants/WIDE) opcode varIndex)
      :else (.put11 code opcode varIndex))
    (when (some? currentBasicBlock)
      (cond
        (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
          (.execute (.-frame currentBasicBlock) opcode varIndex nil nil)
        (== opcode Opcodes/RET)
          (do
            (set! (.-flags currentBasicBlock)
                  (unchecked-short
                    (bit-or-int (.-flags currentBasicBlock) Label/FLAG_SUBROUTINE_END)))
            (set! (.-outputStackSize currentBasicBlock) (unchecked-short relativeStackSize))
            (.endCurrentBasicBlockWithNoSuccessor this))
        :else
          (let [size (unchecked-add-int relativeStackSize (aget STACK_SIZE_DELTA opcode))]
            (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
            (set! relativeStackSize size))))
    (when-not (== compute COMPUTE_NOTHING)
      (let [^int currentMaxLocals (if (or (or (or
                                                (== opcode Opcodes/LLOAD)
                                                (== opcode Opcodes/DLOAD))
                                              (== opcode Opcodes/LSTORE))
                                          (== opcode Opcodes/DSTORE))
                                      (unchecked-add-int varIndex 2)
                                      (unchecked-add-int varIndex 1))]
        (when (> currentMaxLocals maxLocals) (set! maxLocals currentMaxLocals))))
    (when (and (and (>= opcode Opcodes/ISTORE) (== compute COMPUTE_ALL_FRAMES))
               (some? firstHandler))
      (.visitLabel this (Label.))))

  (method ^:public visitTypeInsn ^void [this ^:final ^int opcode ^:final ^String type]
    (set! lastBytecodeOffset (.-length code))
    (let [typeSymbol (.addConstantClass symbolTable type)]
      (.put12 code opcode (.-index typeSymbol))
      (when (some? currentBasicBlock)
        (cond
          (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock) opcode lastBytecodeOffset typeSymbol symbolTable)
          (== opcode Opcodes/NEW)
            (let [size (unchecked-add-int relativeStackSize 1)]
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitFieldInsn ^void [this ^:final ^int opcode ^:final ^String owner
                                         ^:final ^String name ^:final ^String descriptor]
    (set! lastBytecodeOffset (.-length code))
    (let [fieldrefSymbol (.addConstantFieldref symbolTable owner name descriptor)]
      (.put12 code opcode (.-index fieldrefSymbol))
      (when (some? currentBasicBlock)
        (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock) opcode 0 fieldrefSymbol symbolTable)
            (let [^:mutable ^int size 0
                  firstDescChar (.charAt descriptor 0)]
              (switch opcode
                Opcodes/GETSTATIC
                  (set! size
                        (unchecked-add-int
                          relativeStackSize
                          (if (or (== firstDescChar \D) (== firstDescChar \J)) 2 1)))
                Opcodes/PUTSTATIC
                  (set! size
                        (unchecked-add-int relativeStackSize
                                           (if (or (== firstDescChar \D) (== firstDescChar \J))
                                               -2
                                               -1)))
                Opcodes/GETFIELD
                  (set! size
                        (unchecked-add-int
                          relativeStackSize
                          (if (or (== firstDescChar \D) (== firstDescChar \J)) 1 0)))
                (set! size
                      (unchecked-add-int
                        relativeStackSize
                        (if (or (== firstDescChar \D) (== firstDescChar \J)) -3 -2))))
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitMethodInsn ^void [this ^:final ^int opcode ^:final ^String owner
                                          ^:final ^String name ^:final ^String descriptor
                                          ^:final ^boolean isInterface]
    (set! lastBytecodeOffset (.-length code))
    (let [methodrefSymbol (.addConstantMethodref symbolTable owner name descriptor isInterface)]
      (if (== opcode Opcodes/INVOKEINTERFACE)
          (.put11 (.put12 code Opcodes/INVOKEINTERFACE (.-index methodrefSymbol))
                  (bit-shift-right-int (.getArgumentsAndReturnSizes methodrefSymbol) 2)
                  0)
          (.put12 code opcode (.-index methodrefSymbol)))
      (when (some? currentBasicBlock)
        (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock) opcode 0 methodrefSymbol symbolTable)
            (let [argumentsAndReturnSize (.getArgumentsAndReturnSizes methodrefSymbol)
                  stackSizeDelta (unchecked-subtract-int
                                   (bit-and-int argumentsAndReturnSize 3)
                                   (bit-shift-right-int argumentsAndReturnSize 2))
                  ^int size (if (== opcode Opcodes/INVOKESTATIC)
                                (unchecked-add-int
                                  (unchecked-add-int relativeStackSize stackSizeDelta)
                                  1)
                                (unchecked-add-int relativeStackSize stackSizeDelta))]
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitInvokeDynamicInsn ^void [this ^:final ^String name
                                                 ^:final ^String descriptor
                                                 ^:final ^Handle bootstrapMethodHandle &
                                                 ^:final ^Object/1 bootstrapMethodArguments]
    (set! lastBytecodeOffset (.-length code))
    (let [invokeDynamicSymbol (.addConstantInvokeDynamic
                                symbolTable
                                name
                                descriptor
                                bootstrapMethodHandle
                                bootstrapMethodArguments)]
      (.put12 code Opcodes/INVOKEDYNAMIC (.-index invokeDynamicSymbol))
      (.putShort code 0)
      (when (some? currentBasicBlock)
        (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock)
                      Opcodes/INVOKEDYNAMIC
                      0
                      invokeDynamicSymbol
                      symbolTable)
            (let [argumentsAndReturnSize (.getArgumentsAndReturnSizes invokeDynamicSymbol)
                  stackSizeDelta (unchecked-add-int
                                   (unchecked-subtract-int
                                     (bit-and-int argumentsAndReturnSize 3)
                                     (bit-shift-right-int argumentsAndReturnSize 2))
                                   1)
                  size (unchecked-add-int relativeStackSize stackSizeDelta)]
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitJumpInsn ^void [this ^:final ^int opcode ^:final ^Label label]
    (set! lastBytecodeOffset (.-length code))
    (let [baseOpcode (if (>= opcode Constants/GOTO_W)
                         (unchecked-subtract-int opcode Constants/WIDE_JUMP_OPCODE_DELTA)
                         opcode)
          ^:mutable nextInsnIsJumpTarget false]
      (cond
        (and (not (== (bit-and-int (.-flags label) Label/FLAG_RESOLVED) 0))
             (< (unchecked-subtract-int (.-bytecodeOffset label) (.-length code)) Short/MIN_VALUE))
          (do
            (cond
              (== baseOpcode Opcodes/GOTO) (.putByte code Constants/GOTO_W)
              (== baseOpcode Opcodes/JSR) (.putByte code Constants/JSR_W)
              :else
                (do
                  (.putByte code
                            (if (>= baseOpcode Opcodes/IFNULL)
                                (bit-xor-int baseOpcode 1)
                                (unchecked-subtract-int
                                  (bit-xor-int (unchecked-add-int baseOpcode 1) 1)
                                  1)))
                  (.putShort code 8)
                  (.putByte code Constants/ASM_GOTO_W)
                  (set! hasAsmInstructions true)
                  (set! nextInsnIsJumpTarget true)))
            (.put label code (unchecked-subtract-int (.-length code) 1) true))
        (not (== baseOpcode opcode))
          (do
            (.putByte code opcode)
            (.put label code (unchecked-subtract-int (.-length code) 1) true))
        :else
          (do
            (.putByte code baseOpcode)
            (.put label code (unchecked-subtract-int (.-length code) 1) false)))
      (when (some? currentBasicBlock)
        (let [^:mutable ^Label nextBasicBlock nil]
          (cond
            (== compute COMPUTE_ALL_FRAMES)
              (do
                (.execute (.-frame currentBasicBlock) baseOpcode 0 nil nil)
                (let [o-1 (.getCanonicalInstance label)]
                  (set! (.-flags o-1)
                        (unchecked-short (bit-or-int (.-flags o-1) Label/FLAG_JUMP_TARGET))))
                (.addSuccessorToCurrentBasicBlock this 0 label)
                (when-not (== baseOpcode Opcodes/GOTO) (set! nextBasicBlock (Label.))))
            (== compute COMPUTE_INSERTED_FRAMES)
              (.execute (.-frame currentBasicBlock) baseOpcode 0 nil nil)
            (== compute COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES)
              (set! relativeStackSize
                    (unchecked-add-int relativeStackSize (aget STACK_SIZE_DELTA baseOpcode)))
            (== baseOpcode Opcodes/JSR)
              (do
                (when (== (bit-and-int (.-flags label) Label/FLAG_SUBROUTINE_START) 0)
                  (set! (.-flags label)
                        (unchecked-short (bit-or-int (.-flags label) Label/FLAG_SUBROUTINE_START)))
                  (set! hasSubroutines true))
                (set! (.-flags currentBasicBlock)
                      (unchecked-short
                        (bit-or-int (.-flags currentBasicBlock) Label/FLAG_SUBROUTINE_CALLER)))
                (.addSuccessorToCurrentBasicBlock
                  this
                  (unchecked-add-int relativeStackSize 1)
                  label)
                (set! nextBasicBlock (Label.)))
            :else
              (do
                (set! relativeStackSize
                      (unchecked-add-int relativeStackSize (aget STACK_SIZE_DELTA baseOpcode)))
                (.addSuccessorToCurrentBasicBlock this relativeStackSize label)))
          (when (some? nextBasicBlock)
            (when nextInsnIsJumpTarget
              (set! (.-flags nextBasicBlock)
                    (unchecked-short (bit-or-int (.-flags nextBasicBlock) Label/FLAG_JUMP_TARGET))))
            (.visitLabel this nextBasicBlock))
          (when (== baseOpcode Opcodes/GOTO) (.endCurrentBasicBlockWithNoSuccessor this))))))

  (method ^:public visitLabel ^void [this ^:final ^Label label]
    (set! hasAsmInstructions
          (let [a-2 hasAsmInstructions
                b-3 (.resolve label (.-data code) stackMapTableEntries (.-length code))]
            (or a-2 b-3)))
    (when-not (not (== (bit-and-int (.-flags label) Label/FLAG_DEBUG_ONLY) 0))
      (cond
        (== compute COMPUTE_ALL_FRAMES)
          (do
            (when (some? currentBasicBlock)
              (when (== (.-bytecodeOffset label) (.-bytecodeOffset currentBasicBlock))
                (set! (.-flags currentBasicBlock)
                      (unchecked-short
                        (bit-or-int (.-flags currentBasicBlock)
                                    (bit-and-int (.-flags label) Label/FLAG_JUMP_TARGET))))
                (set! (.-frame label) (.-frame currentBasicBlock))
                (return))
              (.addSuccessorToCurrentBasicBlock this 0 label))
            (when (some? lastBasicBlock)
              (when (== (.-bytecodeOffset label) (.-bytecodeOffset lastBasicBlock))
                (set! (.-flags lastBasicBlock)
                      (unchecked-short
                        (bit-or-int (.-flags lastBasicBlock)
                                    (bit-and-int (.-flags label) Label/FLAG_JUMP_TARGET))))
                (set! (.-frame label) (.-frame lastBasicBlock))
                (set! currentBasicBlock lastBasicBlock)
                (return))
              (set! (.-nextBasicBlock lastBasicBlock) label))
            (set! lastBasicBlock label)
            (set! currentBasicBlock label)
            (set! (.-frame label) (Frame. label limits)))
        (== compute COMPUTE_INSERTED_FRAMES)
          (if (nil? currentBasicBlock)
              (set! currentBasicBlock label)
              (set! (.-owner (.-frame currentBasicBlock)) label))
        (== compute COMPUTE_MAX_STACK_AND_LOCAL)
          (do
            (when (some? currentBasicBlock)
              (set! (.-outputStackMax currentBasicBlock) (unchecked-short maxRelativeStackSize))
              (.addSuccessorToCurrentBasicBlock this relativeStackSize label))
            (set! currentBasicBlock label)
            (set! relativeStackSize 0)
            (set! maxRelativeStackSize 0)
            (when (some? lastBasicBlock) (set! (.-nextBasicBlock lastBasicBlock) label))
            (set! lastBasicBlock label))
        (and (== compute COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) (nil? currentBasicBlock))
          (set! currentBasicBlock label))))

  (method ^:public visitLdcInsn ^void [this ^:final value]
    (set! lastBytecodeOffset (.-length code))
    (let [constantSymbol (.addConstant symbolTable value)
          constantIndex (.-index constantSymbol)
          ^:mutable ^char firstDescriptorChar (unchecked-char 0)
          isLongOrDouble (or (or (== (.-tag constantSymbol) Symbol/CONSTANT_LONG_TAG)
                                 (== (.-tag constantSymbol) Symbol/CONSTANT_DOUBLE_TAG))
                             (and (== (.-tag constantSymbol) Symbol/CONSTANT_DYNAMIC_TAG)
                                  (or (== (set! firstDescriptorChar
                                                (.charAt (.-value constantSymbol) 0))
                                          \J)
                                      (== firstDescriptorChar \D))))]
      (cond
        isLongOrDouble (.put12 code Constants/LDC2_W constantIndex)
        (>= constantIndex 256) (.put12 code Constants/LDC_W constantIndex)
        :else (.put11 code Opcodes/LDC constantIndex))
      (when (some? currentBasicBlock)
        (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock) Opcodes/LDC 0 constantSymbol symbolTable)
            (let [size (unchecked-add-int relativeStackSize (if isLongOrDouble 2 1))]
              (when (> size maxRelativeStackSize) (set! maxRelativeStackSize size))
              (set! relativeStackSize size))))))

  (method ^:public visitIincInsn ^void [this ^:final ^int varIndex ^:final ^int increment]
    (set! lastBytecodeOffset (.-length code))
    (if (or (or (> varIndex 255) (> increment 127)) (< increment -128))
        (.putShort (.put12 (.putByte code Constants/WIDE) Opcodes/IINC varIndex) increment)
        (.put11 (.putByte code Opcodes/IINC) varIndex increment))
    (when (and (some? currentBasicBlock)
               (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES)))
      (.execute (.-frame currentBasicBlock) Opcodes/IINC varIndex nil nil))
    (when-not (== compute COMPUTE_NOTHING)
      (let [currentMaxLocals (unchecked-add-int varIndex 1)]
        (when (> currentMaxLocals maxLocals) (set! maxLocals currentMaxLocals)))))

  (method ^:public visitTableSwitchInsn ^void [this ^:final ^int min ^:final ^int max
                                               ^:final ^Label dflt & ^:final ^Label/1 labels]
    (set! lastBytecodeOffset (.-length code))
    (.putByteArray (.putByte code Opcodes/TABLESWITCH)
                   nil
                   0
                   (unchecked-remainder-int
                     (unchecked-subtract-int 4 (unchecked-remainder-int (.-length code) 4))
                     4))
    (.put dflt code lastBytecodeOffset true)
    (.putInt (.putInt code min) max)
    (for-each [^Label label labels] (.put label code lastBytecodeOffset true))
    (.visitSwitchInsn this dflt labels))

  (method ^:public visitLookupSwitchInsn ^void [this ^:final ^Label dflt ^:final ^int/1 keys
                                                ^:final ^Label/1 labels]
    (set! lastBytecodeOffset (.-length code))
    (.putByteArray (.putByte code Opcodes/LOOKUPSWITCH)
                   nil
                   0
                   (unchecked-remainder-int
                     (unchecked-subtract-int 4 (unchecked-remainder-int (.-length code) 4))
                     4))
    (.put dflt code lastBytecodeOffset true)
    (.putInt code (alength labels))
    (loop [^int i 0]
      (when (< i (alength labels))
        (.putInt code (aget keys i))
        (.put (aget labels i) code lastBytecodeOffset true)
        (recur (unchecked-inc-int i))))
    (.visitSwitchInsn this dflt labels))

  (method ^:private visitSwitchInsn ^void [this ^:final ^Label dflt ^:final ^Label/1 labels]
    (when (some? currentBasicBlock)
      (if (== compute COMPUTE_ALL_FRAMES)
          (do
            (.execute (.-frame currentBasicBlock) Opcodes/LOOKUPSWITCH 0 nil nil)
            (.addSuccessorToCurrentBasicBlock this 0 dflt)
            (let [o-4 (.getCanonicalInstance dflt)]
              (set! (.-flags o-4)
                    (unchecked-short (bit-or-int (.-flags o-4) Label/FLAG_JUMP_TARGET))))
            (for-each [^Label label labels]
              (.addSuccessorToCurrentBasicBlock this 0 label)
              (let [o-5 (.getCanonicalInstance label)]
                (set! (.-flags o-5)
                      (unchecked-short (bit-or-int (.-flags o-5) Label/FLAG_JUMP_TARGET))))))
          (when (== compute COMPUTE_MAX_STACK_AND_LOCAL)
            (set! relativeStackSize (unchecked-dec-int relativeStackSize))
            (.addSuccessorToCurrentBasicBlock this relativeStackSize dflt)
            (for-each [^Label label labels]
              (.addSuccessorToCurrentBasicBlock this relativeStackSize label))))
      (.endCurrentBasicBlockWithNoSuccessor this)))

  (method ^:public visitMultiANewArrayInsn ^void [this ^:final ^String descriptor
                                                  ^:final ^int numDimensions]
    (set! lastBytecodeOffset (.-length code))
    (let [descSymbol (.addConstantClass symbolTable descriptor)]
      (.putByte (.put12 code Opcodes/MULTIANEWARRAY (.-index descSymbol)) numDimensions)
      (when (some? currentBasicBlock)
        (if (or (== compute COMPUTE_ALL_FRAMES) (== compute COMPUTE_INSERTED_FRAMES))
            (.execute (.-frame currentBasicBlock)
                      Opcodes/MULTIANEWARRAY
                      numDimensions
                      descSymbol
                      symbolTable)
            (set! relativeStackSize
                  (unchecked-add-int relativeStackSize (unchecked-subtract-int 1 numDimensions)))))))

  (method ^:public visitInsnAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (if visible
        (set! lastCodeRuntimeVisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       (bit-or-int
                                         (bit-and-int typeRef (unchecked-int 0xFF0000FF))
                                         (bit-shift-left-int lastBytecodeOffset 8))
                                       typePath
                                       descriptor
                                       lastCodeRuntimeVisibleTypeAnnotation))
        (set! lastCodeRuntimeInvisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       (bit-or-int
                                         (bit-and-int typeRef (unchecked-int 0xFF0000FF))
                                         (bit-shift-left-int lastBytecodeOffset 8))
                                       typePath
                                       descriptor
                                       lastCodeRuntimeInvisibleTypeAnnotation))))

  (method ^:public visitTryCatchBlock ^void [this ^:final ^Label start ^:final ^Label end
                                             ^:final ^Label handler ^:final ^String type]
    (let [newHandler (Handler. start
                               end
                               handler
                               (if (some? type) (.-index (.addConstantClass symbolTable type)) 0)
                               type)]
      (if (nil? firstHandler)
          (set! firstHandler newHandler)
          (set! (.-nextHandler lastHandler) newHandler))
      (set! lastHandler newHandler)))

  (method ^:public visitTryCatchAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                               ^:final ^TypePath typePath
                                                               ^:final ^String descriptor
                                                               ^:final ^boolean visible]
    (if visible
        (set! lastCodeRuntimeVisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       typeRef
                                       typePath
                                       descriptor
                                       lastCodeRuntimeVisibleTypeAnnotation))
        (set! lastCodeRuntimeInvisibleTypeAnnotation
              (AnnotationWriter/create symbolTable
                                       typeRef
                                       typePath
                                       descriptor
                                       lastCodeRuntimeInvisibleTypeAnnotation))))

  (method ^:public visitLocalVariable ^void [this ^:final ^String name ^:final ^String descriptor
                                             ^:final ^String signature ^:final ^Label start
                                             ^:final ^Label end ^:final ^int index]
    (when (some? signature)
      (when (nil? localVariableTypeTable) (set! localVariableTypeTable (ByteVector.)))
      (set! localVariableTypeTableLength (unchecked-inc-int localVariableTypeTableLength))
      (.putShort (.putShort (.putShort (.putShort
                                         (.putShort localVariableTypeTable (.-bytecodeOffset start))
                                         (unchecked-subtract-int
                                           (.-bytecodeOffset end)
                                           (.-bytecodeOffset start)))
                                       (.addConstantUtf8 symbolTable name))
                            (.addConstantUtf8 symbolTable signature))
                 index))
    (when (nil? localVariableTable) (set! localVariableTable (ByteVector.)))
    (set! localVariableTableLength (unchecked-inc-int localVariableTableLength))
    (.putShort (.putShort (.putShort (.putShort (.putShort
                                                  localVariableTable
                                                  (.-bytecodeOffset start))
                                                (unchecked-subtract-int
                                                  (.-bytecodeOffset end)
                                                  (.-bytecodeOffset start)))
                                     (.addConstantUtf8 symbolTable name))
                          (.addConstantUtf8 symbolTable descriptor))
               index)
    (when-not (== compute COMPUTE_NOTHING)
      (let [firstDescChar (.charAt descriptor 0)
            currentMaxLocals (unchecked-add-int index
                                                (if (or (== firstDescChar \J) (== firstDescChar \D))
                                                    2
                                                    1))]
        (when (> currentMaxLocals maxLocals) (set! maxLocals currentMaxLocals)))))

  (method ^:public visitLocalVariableAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                                    ^:final ^TypePath typePath
                                                                    ^:final ^Label/1 start
                                                                    ^:final ^Label/1 end
                                                                    ^:final ^int/1 index
                                                                    ^:final ^String descriptor
                                                                    ^:final ^boolean visible]
    (let [typeAnnotation (ByteVector.)]
      (.putShort (.putByte typeAnnotation (unsigned-bit-shift-right-int typeRef 24))
                 (alength start))
      (loop [^int i 0]
        (when (< i (alength start))
          (.putShort (.putShort (.putShort typeAnnotation (.-bytecodeOffset (aget start i)))
                                (unchecked-subtract-int
                                  (.-bytecodeOffset (aget end i))
                                  (.-bytecodeOffset (aget start i))))
                     (aget index i))
          (recur (unchecked-inc-int i))))
      (TypePath/put typePath typeAnnotation)
      (.putShort (.putShort typeAnnotation (.addConstantUtf8 symbolTable descriptor)) 0)
      (if visible
          (set! lastCodeRuntimeVisibleTypeAnnotation
                (AnnotationWriter. symbolTable
                                   true
                                   typeAnnotation
                                   lastCodeRuntimeVisibleTypeAnnotation))
          (set! lastCodeRuntimeInvisibleTypeAnnotation
                (AnnotationWriter. symbolTable
                                   true
                                   typeAnnotation
                                   lastCodeRuntimeInvisibleTypeAnnotation)))))

  (method ^:public visitLineNumber ^void [this ^:final ^int line ^:final ^Label start]
    (when (nil? lineNumberTable) (set! lineNumberTable (ByteVector.)))
    (set! lineNumberTableLength (unchecked-inc-int lineNumberTableLength))
    (.putShort lineNumberTable (.-bytecodeOffset start))
    (.putShort lineNumberTable line))

  (method ^:public visitMaxs ^void [this ^:final ^int maxStack ^:final ^int maxLocals]
    (cond
      (== compute COMPUTE_ALL_FRAMES) (.computeAllFrames this)
      (== compute COMPUTE_MAX_STACK_AND_LOCAL) (.computeMaxStackAndLocal this)
      (== compute COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES)
        (set! (.-maxStack this) maxRelativeStackSize)
      :else (do (set! (.-maxStack this) maxStack) (set! (.-maxLocals this) maxLocals))))

  (method ^:private computeAllFrames ^void [this]
    (let [^:mutable handler firstHandler]
      (while (some? handler)
        (let [handlerBlock (.getCanonicalInstance (.-handlerPc handler))]
          (set! (.-flags handlerBlock)
                (unchecked-short (bit-or-int (.-flags handlerBlock) Label/FLAG_JUMP_TARGET)))
          (set! handler (.-nextHandler handler))))
      (let [firstFrame (.-frame firstBasicBlock)]
        (.setInputFrameFromDescriptor firstFrame
                                      symbolTable
                                      accessFlags
                                      descriptor
                                      (.-maxLocals this))
        (.accept firstFrame this)
        (let [^:mutable listOfBlocksToProcess firstBasicBlock]
          (set! (.-nextListElement listOfBlocksToProcess) Label/EMPTY_LIST)
          (let [^:mutable ^int maxStackSize 0]
            (while (not (identical? listOfBlocksToProcess Label/EMPTY_LIST))
              (let [basicBlock listOfBlocksToProcess]
                (set! listOfBlocksToProcess (.-nextListElement listOfBlocksToProcess))
                (set! (.-nextListElement basicBlock) nil)
                (set! (.-flags basicBlock)
                      (unchecked-short (bit-or-int (.-flags basicBlock) Label/FLAG_REACHABLE)))
                (let [maxBlockStackSize (unchecked-add-int
                                          (.getInputStackSize (.-frame basicBlock))
                                          (.-outputStackMax basicBlock))]
                  (when (> maxBlockStackSize maxStackSize) (set! maxStackSize maxBlockStackSize))
                  (let [^:mutable ^int numOperations 0
                        ^:mutable outgoingEdge (.-outgoingEdges basicBlock)]
                    (while (some? outgoingEdge)
                      (let [successorBlock (.getCanonicalInstance (.-successor outgoingEdge))
                            successorBlockChanged (.merge
                                                    (.-frame basicBlock)
                                                    symbolTable
                                                    (.-frame successorBlock)
                                                    0)]
                        (when (and successorBlockChanged (nil? (.-nextListElement successorBlock)))
                          (set! (.-nextListElement successorBlock) listOfBlocksToProcess)
                          (set! listOfBlocksToProcess successorBlock))
                        (set! outgoingEdge (.-nextEdge outgoingEdge))
                        (set! numOperations
                              (unchecked-add-int
                                numOperations
                                (unchecked-add-int Frame/NUM_OPERATIONS_PER_MERGE 5)))))
                    (let [basicBlockOffset (.-bytecodeOffset basicBlock)]
                      (set! handler firstHandler)
                      (while (some? handler)
                        (let [startOffset (.-bytecodeOffset (.-startPc handler))
                              endOffset (.-bytecodeOffset (.-endPc handler))]
                          (when (and (>= basicBlockOffset startOffset)
                                     (< basicBlockOffset endOffset))
                            (let [catchTypeDescriptor (if (nil? (.-catchTypeDescriptor handler))
                                                          "java/lang/Throwable"
                                                          (.-catchTypeDescriptor handler))
                                  catchType (Frame/getAbstractTypeFromInternalName
                                              symbolTable
                                              catchTypeDescriptor)
                                  successorBlock (.getCanonicalInstance (.-handlerPc handler))
                                  successorBlockChanged (.merge
                                                          (.-frame basicBlock)
                                                          symbolTable
                                                          (.-frame successorBlock)
                                                          catchType)]
                              (when (and successorBlockChanged
                                         (nil? (.-nextListElement successorBlock)))
                                (set! (.-nextListElement successorBlock) listOfBlocksToProcess)
                                (set! listOfBlocksToProcess successorBlock))
                              (set! numOperations
                                    (unchecked-add-int
                                      numOperations
                                      (unchecked-add-int Frame/NUM_OPERATIONS_PER_MERGE 5)))))
                          (set! numOperations (unchecked-add-int numOperations 3))
                          (set! handler (.-nextHandler handler))))
                      (.checkNewOperations limits numOperations))))))
            (let [^:mutable basicBlock firstBasicBlock]
              (while (some? basicBlock)
                (when (== (bit-and-int (.-flags basicBlock)
                                       (bit-or-int Label/FLAG_JUMP_TARGET Label/FLAG_REACHABLE))
                          (bit-or-int Label/FLAG_JUMP_TARGET Label/FLAG_REACHABLE))
                  (.accept (.-frame basicBlock) this))
                (when (== (bit-and-int (.-flags basicBlock) Label/FLAG_REACHABLE) 0)
                  (let [nextBasicBlock (.-nextBasicBlock basicBlock)
                        startOffset (.-bytecodeOffset basicBlock)
                        endOffset (unchecked-subtract-int
                                    (if (nil? nextBasicBlock)
                                        (.-length code)
                                        (.-bytecodeOffset nextBasicBlock))
                                    1)]
                    (when (>= endOffset startOffset)
                      (loop [^int i startOffset]
                        (when (< i endOffset)
                          (aset (.-data code) i (unchecked-byte Opcodes/NOP))
                          (recur (unchecked-inc-int i))))
                      (aset (.-data code) endOffset (unchecked-byte Opcodes/ATHROW))
                      (let [frameIndex (.visitFrameStart this startOffset 0 1)]
                        (aset currentFrame
                              frameIndex
                              (Frame/getAbstractTypeFromInternalName
                                symbolTable
                                "java/lang/Throwable"))
                        (.visitFrameEnd this)
                        (set! firstHandler
                              (Handler/removeRange firstHandler basicBlock nextBasicBlock))
                        (set! maxStackSize (^[int int] Math/max maxStackSize 1))))))
                (set! basicBlock (.-nextBasicBlock basicBlock)))
              (set! (.-maxStack this) maxStackSize)))))))

  (method ^:private computeMaxStackAndLocal ^void [this]
    (when hasSubroutines
      (let [^:mutable ^short numSubroutines 1]
        (.markSubroutine firstBasicBlock numSubroutines firstHandler limits)
        (loop [^short currentSubroutine 1]
          (when (<= currentSubroutine numSubroutines)
            (let [^:mutable basicBlock firstBasicBlock]
              (while (some? basicBlock)
                (when (and (not (== (bit-and-int (.-flags basicBlock) Label/FLAG_SUBROUTINE_CALLER)
                                    0))
                           (== (.-subroutineId basicBlock) currentSubroutine))
                  (let [jsrTarget (.-successor (.-nextEdge (.-outgoingEdges basicBlock)))]
                    (when (== (.-subroutineId jsrTarget) 0)
                      (.markSubroutine jsrTarget
                                       (set! numSubroutines
                                             (unchecked-short (unchecked-inc-int numSubroutines)))
                                       firstHandler
                                       limits))))
                (set! basicBlock (.-nextBasicBlock basicBlock)))
              (recur (unchecked-short (unchecked-inc-int currentSubroutine))))))
        (let [^:mutable basicBlock firstBasicBlock]
          (while (some? basicBlock)
            (when-not (== (bit-and-int (.-flags basicBlock) Label/FLAG_SUBROUTINE_CALLER) 0)
              (let [subroutine (.-successor (.-nextEdge (.-outgoingEdges basicBlock)))]
                (.addSubroutineRetSuccessors subroutine basicBlock firstHandler limits)))
            (set! basicBlock (.-nextBasicBlock basicBlock))))))
    (let [^:mutable listOfBlocksToProcess firstBasicBlock]
      (set! (.-nextListElement listOfBlocksToProcess) Label/EMPTY_LIST)
      (let [^:mutable maxStackSize maxStack]
        (while (not (identical? listOfBlocksToProcess Label/EMPTY_LIST))
          (let [basicBlock listOfBlocksToProcess]
            (set! listOfBlocksToProcess (.-nextListElement listOfBlocksToProcess))
            (let [^int inputStackTop (.-inputStackSize basicBlock)
                  maxBlockStackSize (unchecked-add-int inputStackTop (.-outputStackMax basicBlock))]
              (when (> maxBlockStackSize maxStackSize) (set! maxStackSize maxBlockStackSize))
              (let [^:mutable outgoingEdge (.-outgoingEdges basicBlock)]
                (when-not (== (bit-and-int (.-flags basicBlock) Label/FLAG_SUBROUTINE_CALLER) 0)
                  (set! outgoingEdge (.-nextEdge outgoingEdge)))
                (let [^:mutable ^int numOperations 10]
                  (while (some? outgoingEdge)
                    (let [successorBlock (.-successor outgoingEdge)]
                      (when (nil? (.-nextListElement successorBlock))
                        (set! (.-inputStackSize successorBlock)
                              (unchecked-short
                                (Math/max (.-inputStackSize successorBlock)
                                          (unchecked-add-int
                                            inputStackTop
                                            (.-stackSizeDelta outgoingEdge)))))
                        (set! (.-nextListElement successorBlock) listOfBlocksToProcess)
                        (set! listOfBlocksToProcess successorBlock))
                      (set! outgoingEdge (.-nextEdge outgoingEdge))
                      (set! numOperations (unchecked-add-int numOperations 5))))
                  (let [basicBlockOffset (.-bytecodeOffset basicBlock)
                        ^:mutable handler firstHandler]
                    (while (some? handler)
                      (let [startOffset (.-bytecodeOffset (.-startPc handler))
                            endOffset (.-bytecodeOffset (.-endPc handler))]
                        (when (and (>= basicBlockOffset startOffset) (< basicBlockOffset endOffset))
                          (let [successorBlock (.-handlerPc handler)]
                            (when (nil? (.-nextListElement successorBlock))
                              (set! (.-inputStackSize successorBlock)
                                    (unchecked-short
                                      (^[int int] Math/max (.-inputStackSize successorBlock) 1)))
                              (set! (.-nextListElement successorBlock) listOfBlocksToProcess)
                              (set! listOfBlocksToProcess successorBlock))))
                        (set! handler (.-nextHandler handler))
                        (set! numOperations (unchecked-add-int numOperations 10))))
                    (.checkNewOperations limits numOperations)))))))
        (set! (.-maxStack this) maxStackSize))))

  (method ^:public visitEnd ^void [this])

  (method ^:private addSuccessorToCurrentBasicBlock ^void [this ^:final ^int info
                                                           ^:final ^Label successor]
    (set! (.-outgoingEdges currentBasicBlock)
          (Edge. info successor (.-outgoingEdges currentBasicBlock))))

  (method ^:private endCurrentBasicBlockWithNoSuccessor ^void [this]
    (if (== compute COMPUTE_ALL_FRAMES)
        (let [nextBasicBlock (Label.)]
          (set! (.-frame nextBasicBlock) (Frame. nextBasicBlock limits))
          (.resolve nextBasicBlock (.-data code) stackMapTableEntries (.-length code))
          (set! (.-nextBasicBlock lastBasicBlock) nextBasicBlock)
          (set! lastBasicBlock nextBasicBlock)
          (set! currentBasicBlock nil))
        (when (== compute COMPUTE_MAX_STACK_AND_LOCAL)
          (set! (.-outputStackMax currentBasicBlock) (unchecked-short maxRelativeStackSize))
          (set! currentBasicBlock nil))))

  (method visitFrameStart ^int [this ^:final ^int offset ^:final ^int numLocal ^:final ^int numStack]
    (let [frameLength (unchecked-add-int (unchecked-add-int 3 numLocal) numStack)]
      (when (or (nil? currentFrame) (< (alength currentFrame) frameLength))
        (set! currentFrame (.checkNewIntArray limits frameLength)))
      (aset currentFrame 0 offset)
      (aset currentFrame 1 numLocal)
      (aset currentFrame 2 numStack)
      3))

  (method visitAbstractType ^void [this ^:final ^int frameIndex ^:final ^int abstractType]
    (aset currentFrame frameIndex abstractType))

  (method visitFrameEnd ^void [this]
    (when (some? previousFrame)
      (when (nil? stackMapTableEntries) (set! stackMapTableEntries (ByteVector.)))
      (.putFrame this)
      (set! stackMapTableNumberOfEntries (unchecked-inc-int stackMapTableNumberOfEntries)))
    (set! previousFrame currentFrame)
    (set! currentFrame nil))

  (method ^:private putFrame ^void [this]
    (let [numLocal (aget currentFrame 1)
          numStack (aget currentFrame 2)]
      (if (< (.getMajorVersion symbolTable) Opcodes/V1_6)
          (do
            (.putShort (.putShort stackMapTableEntries (aget currentFrame 0)) numLocal)
            (.putAbstractTypes this 3 (unchecked-add-int 3 numLocal))
            (.putShort stackMapTableEntries numStack)
            (.putAbstractTypes this
                               (unchecked-add-int 3 numLocal)
                               (unchecked-add-int (unchecked-add-int 3 numLocal) numStack)))
          (let [offsetDelta (if (== stackMapTableNumberOfEntries 0)
                                (aget currentFrame 0)
                                (unchecked-subtract-int
                                  (unchecked-subtract-int
                                    (aget currentFrame 0)
                                    (aget previousFrame 0))
                                  1))
                previousNumlocal (aget previousFrame 1)
                numLocalDelta (unchecked-subtract-int numLocal previousNumlocal)
                ^:mutable type Frame/FULL_FRAME]
            (cond
              (== numStack 0)
                (switch numLocalDelta
                  (-3 -2 -1) (set! type Frame/CHOP_FRAME)
                  0 (set! type (if (< offsetDelta 64) Frame/SAME_FRAME Frame/SAME_FRAME_EXTENDED))
                  (1 2 3) (set! type Frame/APPEND_FRAME)
                  nil)
              (and (== numLocalDelta 0) (== numStack 1))
                (set! type
                      (if (< offsetDelta 63)
                          Frame/SAME_LOCALS_1_STACK_ITEM_FRAME
                          Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)))
            (when-not (== type Frame/FULL_FRAME)
              (let [^:mutable ^int frameIndex 3]
                (loop [^int i 0]
                  (if (and (< i previousNumlocal) (< i numLocal))
                      (if (not (== (aget currentFrame frameIndex) (aget previousFrame frameIndex)))
                          (set! type Frame/FULL_FRAME)
                          (do
                            (set! frameIndex (unchecked-inc-int frameIndex))
                            (recur (unchecked-inc-int i))))
                      nil))))
            (switch type
              Frame/SAME_FRAME (.putByte stackMapTableEntries offsetDelta)
              Frame/SAME_LOCALS_1_STACK_ITEM_FRAME
                (do
                  (.putByte stackMapTableEntries
                            (unchecked-add-int Frame/SAME_LOCALS_1_STACK_ITEM_FRAME offsetDelta))
                  (.putAbstractTypes this
                                     (unchecked-add-int 3 numLocal)
                                     (unchecked-add-int 4 numLocal)))
              Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED
                (do
                  (.putShort (.putByte stackMapTableEntries
                                       Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                             offsetDelta)
                  (.putAbstractTypes this
                                     (unchecked-add-int 3 numLocal)
                                     (unchecked-add-int 4 numLocal)))
              Frame/SAME_FRAME_EXTENDED
                (.putShort (.putByte stackMapTableEntries Frame/SAME_FRAME_EXTENDED) offsetDelta)
              Frame/CHOP_FRAME
                (.putShort (.putByte stackMapTableEntries
                                     (unchecked-add-int Frame/SAME_FRAME_EXTENDED numLocalDelta))
                           offsetDelta)
              Frame/APPEND_FRAME
                (do
                  (.putShort (.putByte stackMapTableEntries
                                       (unchecked-add-int Frame/SAME_FRAME_EXTENDED numLocalDelta))
                             offsetDelta)
                  (.putAbstractTypes this
                                     (unchecked-add-int 3 previousNumlocal)
                                     (unchecked-add-int 3 numLocal)))
              (do
                (.putShort (.putShort (.putByte stackMapTableEntries Frame/FULL_FRAME) offsetDelta)
                           numLocal)
                (.putAbstractTypes this 3 (unchecked-add-int 3 numLocal))
                (.putShort stackMapTableEntries numStack)
                (.putAbstractTypes this
                                   (unchecked-add-int 3 numLocal)
                                   (unchecked-add-int (unchecked-add-int 3 numLocal) numStack))))))))

  (method ^:private putAbstractTypes ^void [this ^:final ^int start ^:final ^int end]
    (loop [^int i start]
      (when (< i end)
        (Frame/putAbstractType symbolTable (aget currentFrame i) stackMapTableEntries)
        (recur (unchecked-inc-int i)))))

  (method ^:private putFrameType ^void [this ^:final type]
    (cond
      (instance? Integer type) (.putByte stackMapTableEntries (.intValue (cast Integer type)))
      (instance? String type)
        (.putShort (.putByte stackMapTableEntries Frame/ITEM_OBJECT)
                   (.-index (.addConstantClass symbolTable (cast String type))))
      :else
        (do
          (.putByte stackMapTableEntries Frame/ITEM_UNINITIALIZED)
          (.put (cast Label type) stackMapTableEntries))))

  (method completeAsmInstructions ^void [this]
    (let [maxNumJumpInsns (unchecked-divide-int (.-length code) 3)
          asmInsnIndicesToProcess (new int/1 maxNumJumpInsns)
          ^:mutable ^int numAsmInsnsToProcess 0
          jumpIndices (new int/1 maxNumJumpInsns)
          jumpTargets (new int/1 maxNumJumpInsns)
          jumpOffsets (new int/1 maxNumJumpInsns)
          ^:mutable ^int numJumpInsns 0
          bytecode (.-data code)
          bytecodeLength (.-length code)
          ^:mutable ^int offset 0]
      (while (< offset bytecodeLength)
        (let [opcode (bit-and-int (aget bytecode offset) 0xFF)]
          (switch opcode
            (Opcodes/NOP Opcodes/ACONST_NULL
                         Opcodes/ICONST_M1
                         Opcodes/ICONST_0
                         Opcodes/ICONST_1
                         Opcodes/ICONST_2
                         Opcodes/ICONST_3
                         Opcodes/ICONST_4
                         Opcodes/ICONST_5
                         Opcodes/LCONST_0
                         Opcodes/LCONST_1
                         Opcodes/FCONST_0
                         Opcodes/FCONST_1
                         Opcodes/FCONST_2
                         Opcodes/DCONST_0
                         Opcodes/DCONST_1
                         Opcodes/IALOAD
                         Opcodes/LALOAD
                         Opcodes/FALOAD
                         Opcodes/DALOAD
                         Opcodes/AALOAD
                         Opcodes/BALOAD
                         Opcodes/CALOAD
                         Opcodes/SALOAD
                         Opcodes/IASTORE
                         Opcodes/LASTORE
                         Opcodes/FASTORE
                         Opcodes/DASTORE
                         Opcodes/AASTORE
                         Opcodes/BASTORE
                         Opcodes/CASTORE
                         Opcodes/SASTORE
                         Opcodes/POP
                         Opcodes/POP2
                         Opcodes/DUP
                         Opcodes/DUP_X1
                         Opcodes/DUP_X2
                         Opcodes/DUP2
                         Opcodes/DUP2_X1
                         Opcodes/DUP2_X2
                         Opcodes/SWAP
                         Opcodes/IADD
                         Opcodes/LADD
                         Opcodes/FADD
                         Opcodes/DADD
                         Opcodes/ISUB
                         Opcodes/LSUB
                         Opcodes/FSUB
                         Opcodes/DSUB
                         Opcodes/IMUL
                         Opcodes/LMUL
                         Opcodes/FMUL
                         Opcodes/DMUL
                         Opcodes/IDIV
                         Opcodes/LDIV
                         Opcodes/FDIV
                         Opcodes/DDIV
                         Opcodes/IREM
                         Opcodes/LREM
                         Opcodes/FREM
                         Opcodes/DREM
                         Opcodes/INEG
                         Opcodes/LNEG
                         Opcodes/FNEG
                         Opcodes/DNEG
                         Opcodes/ISHL
                         Opcodes/LSHL
                         Opcodes/ISHR
                         Opcodes/LSHR
                         Opcodes/IUSHR
                         Opcodes/LUSHR
                         Opcodes/IAND
                         Opcodes/LAND
                         Opcodes/IOR
                         Opcodes/LOR
                         Opcodes/IXOR
                         Opcodes/LXOR
                         Opcodes/I2L
                         Opcodes/I2F
                         Opcodes/I2D
                         Opcodes/L2I
                         Opcodes/L2F
                         Opcodes/L2D
                         Opcodes/F2I
                         Opcodes/F2L
                         Opcodes/F2D
                         Opcodes/D2I
                         Opcodes/D2L
                         Opcodes/D2F
                         Opcodes/I2B
                         Opcodes/I2C
                         Opcodes/I2S
                         Opcodes/LCMP
                         Opcodes/FCMPL
                         Opcodes/FCMPG
                         Opcodes/DCMPL
                         Opcodes/DCMPG
                         Opcodes/IRETURN
                         Opcodes/LRETURN
                         Opcodes/FRETURN
                         Opcodes/DRETURN
                         Opcodes/ARETURN
                         Opcodes/RETURN
                         Opcodes/ARRAYLENGTH
                         Opcodes/ATHROW
                         Opcodes/MONITORENTER
                         Opcodes/MONITOREXIT
                         Constants/ILOAD_0
                         Constants/ILOAD_1
                         Constants/ILOAD_2
                         Constants/ILOAD_3
                         Constants/LLOAD_0
                         Constants/LLOAD_1
                         Constants/LLOAD_2
                         Constants/LLOAD_3
                         Constants/FLOAD_0
                         Constants/FLOAD_1
                         Constants/FLOAD_2
                         Constants/FLOAD_3
                         Constants/DLOAD_0
                         Constants/DLOAD_1
                         Constants/DLOAD_2
                         Constants/DLOAD_3
                         Constants/ALOAD_0
                         Constants/ALOAD_1
                         Constants/ALOAD_2
                         Constants/ALOAD_3
                         Constants/ISTORE_0
                         Constants/ISTORE_1
                         Constants/ISTORE_2
                         Constants/ISTORE_3
                         Constants/LSTORE_0
                         Constants/LSTORE_1
                         Constants/LSTORE_2
                         Constants/LSTORE_3
                         Constants/FSTORE_0
                         Constants/FSTORE_1
                         Constants/FSTORE_2
                         Constants/FSTORE_3
                         Constants/DSTORE_0
                         Constants/DSTORE_1
                         Constants/DSTORE_2
                         Constants/DSTORE_3
                         Constants/ASTORE_0
                         Constants/ASTORE_1
                         Constants/ASTORE_2
                         Constants/ASTORE_3)
              (set! offset (unchecked-add-int offset 1))
            (Opcodes/IFEQ Opcodes/IFNE
                          Opcodes/IFLT
                          Opcodes/IFGE
                          Opcodes/IFGT
                          Opcodes/IFLE
                          Opcodes/IF_ICMPEQ
                          Opcodes/IF_ICMPNE
                          Opcodes/IF_ICMPLT
                          Opcodes/IF_ICMPGE
                          Opcodes/IF_ICMPGT
                          Opcodes/IF_ICMPLE
                          Opcodes/IF_ACMPEQ
                          Opcodes/IF_ACMPNE
                          Opcodes/GOTO
                          Opcodes/JSR
                          Opcodes/IFNULL
                          Opcodes/IFNONNULL)
              (let [^int jumpOffset (.readShort this (unchecked-add-int offset 1))
                    maxFinalJumpOffset (unchecked-add-int
                                         jumpOffset
                                         (unchecked-multiply-int
                                           5
                                           (unchecked-divide-int jumpOffset 3)))]
                (when (or (< maxFinalJumpOffset Short/MIN_VALUE)
                          (> maxFinalJumpOffset Short/MAX_VALUE))
                  (aset jumpIndices numJumpInsns offset)
                  (aset jumpTargets numJumpInsns (unchecked-add-int offset jumpOffset))
                  (aset jumpOffsets numJumpInsns jumpOffset)
                  (set! numJumpInsns (unchecked-inc-int numJumpInsns)))
                (set! offset (unchecked-add-int offset 3)))
            (Constants/GOTO_W Constants/JSR_W) (set! offset (unchecked-add-int offset 5))
            (Constants/ASM_IFEQ Constants/ASM_IFNE
                                Constants/ASM_IFLT
                                Constants/ASM_IFGE
                                Constants/ASM_IFGT
                                Constants/ASM_IFLE
                                Constants/ASM_IF_ICMPEQ
                                Constants/ASM_IF_ICMPNE
                                Constants/ASM_IF_ICMPLT
                                Constants/ASM_IF_ICMPGE
                                Constants/ASM_IF_ICMPGT
                                Constants/ASM_IF_ICMPLE
                                Constants/ASM_IF_ACMPEQ
                                Constants/ASM_IF_ACMPNE
                                Constants/ASM_GOTO
                                Constants/ASM_JSR
                                Constants/ASM_IFNULL
                                Constants/ASM_IFNONNULL)
              (do
                (aset asmInsnIndicesToProcess numAsmInsnsToProcess offset)
                (set! numAsmInsnsToProcess (unchecked-inc-int numAsmInsnsToProcess))
                (set! offset (unchecked-add-int offset 3)))
            Constants/ASM_GOTO_W
              (do
                (aset asmInsnIndicesToProcess numAsmInsnsToProcess offset)
                (set! numAsmInsnsToProcess (unchecked-inc-int numAsmInsnsToProcess))
                (set! offset (unchecked-add-int offset 5)))
            Constants/WIDE
              (set! offset
                    (unchecked-add-int offset
                                       (if (== (bit-and-int
                                                 (aget bytecode (unchecked-add-int offset 1))
                                                 0xFF)
                                               Opcodes/IINC)
                                           6
                                           4)))
            Opcodes/TABLESWITCH
              (do
                (aset asmInsnIndicesToProcess numAsmInsnsToProcess offset)
                (set! numAsmInsnsToProcess (unchecked-inc-int numAsmInsnsToProcess))
                (set! offset
                      (unchecked-add-int offset (unchecked-subtract-int 4 (bit-and-int offset 3))))
                (let [low (.readInt this (unchecked-add-int offset 4))
                      high (.readInt this (unchecked-add-int offset 8))]
                  (set! offset
                        (unchecked-add-int offset
                                           (unchecked-add-int
                                             12
                                             (unchecked-multiply-int
                                               4
                                               (unchecked-add-int
                                                 (unchecked-subtract-int high low)
                                                 1)))))))
            Opcodes/LOOKUPSWITCH
              (do
                (aset asmInsnIndicesToProcess numAsmInsnsToProcess offset)
                (set! numAsmInsnsToProcess (unchecked-inc-int numAsmInsnsToProcess))
                (set! offset
                      (unchecked-add-int offset (unchecked-subtract-int 4 (bit-and-int offset 3))))
                (let [numPairs (.readInt this (unchecked-add-int offset 4))]
                  (set! offset
                        (unchecked-add-int
                          offset
                          (unchecked-add-int 8 (unchecked-multiply-int 8 numPairs))))))
            (Opcodes/ILOAD Opcodes/LLOAD
                           Opcodes/FLOAD
                           Opcodes/DLOAD
                           Opcodes/ALOAD
                           Opcodes/ISTORE
                           Opcodes/LSTORE
                           Opcodes/FSTORE
                           Opcodes/DSTORE
                           Opcodes/ASTORE
                           Opcodes/RET
                           Opcodes/BIPUSH
                           Opcodes/NEWARRAY
                           Opcodes/LDC)
              (set! offset (unchecked-add-int offset 2))
            (Opcodes/SIPUSH Constants/LDC_W
                            Constants/LDC2_W
                            Opcodes/GETSTATIC
                            Opcodes/PUTSTATIC
                            Opcodes/GETFIELD
                            Opcodes/PUTFIELD
                            Opcodes/INVOKEVIRTUAL
                            Opcodes/INVOKESPECIAL
                            Opcodes/INVOKESTATIC
                            Opcodes/NEW
                            Opcodes/ANEWARRAY
                            Opcodes/CHECKCAST
                            Opcodes/INSTANCEOF
                            Opcodes/IINC)
              (set! offset (unchecked-add-int offset 3))
            (Opcodes/INVOKEINTERFACE Opcodes/INVOKEDYNAMIC)
              (set! offset (unchecked-add-int offset 5))
            Opcodes/MULTIANEWARRAY (set! offset (unchecked-add-int offset 4))
            (throw (IllegalArgumentException.)))))
      (while (> numAsmInsnsToProcess 0)
        (let [asmInsnIndex (aget asmInsnIndicesToProcess
                                 (set! numAsmInsnsToProcess
                                       (unchecked-dec-int numAsmInsnsToProcess)))
              asmInsnOpcode (bit-and-int (aget bytecode asmInsnIndex) 0xFF)
              insertedBytes (cond
                              (or (== asmInsnOpcode Opcodes/TABLESWITCH)
                                  (== asmInsnOpcode Opcodes/LOOKUPSWITCH))
                                (bit-and-int asmInsnIndex 3)
                              (or (== asmInsnOpcode Constants/ASM_GOTO)
                                  (== asmInsnOpcode Constants/ASM_JSR))
                                2
                              :else 5)
              ^:mutable ^int i 0]
          (while (< i numJumpInsns)
            (cond
              (and (< (aget jumpIndices i) asmInsnIndex) (< asmInsnIndex (aget jumpTargets i)))
                (aset jumpOffsets i (unchecked-add-int (aget jumpOffsets i) insertedBytes))
              (and (< (aget jumpTargets i) asmInsnIndex) (< asmInsnIndex (aget jumpIndices i)))
                (aset jumpOffsets i (unchecked-subtract-int (aget jumpOffsets i) insertedBytes)))
            (if (or (< (aget jumpOffsets i) Short/MIN_VALUE)
                    (> (aget jumpOffsets i) Short/MAX_VALUE))
                (let [opcode (bit-and-int (aget bytecode (aget jumpIndices i)) 0xFF)
                      asmInsn (unchecked-add-int
                                opcode
                                (if (< opcode Opcodes/IFNULL)
                                    Constants/ASM_OPCODE_DELTA
                                    Constants/ASM_IFNULL_OPCODE_DELTA))]
                  (aset bytecode (aget jumpIndices i) (unchecked-byte asmInsn))
                  (aset asmInsnIndicesToProcess numAsmInsnsToProcess (aget jumpIndices i))
                  (set! numAsmInsnsToProcess (unchecked-inc-int numAsmInsnsToProcess))
                  (set! numJumpInsns (unchecked-dec-int numJumpInsns))
                  (aset jumpIndices i (aget jumpIndices numJumpInsns))
                  (aset jumpTargets i (aget jumpTargets numJumpInsns))
                  (aset jumpOffsets i (aget jumpOffsets numJumpInsns)))
                (set! i (unchecked-inc-int i))))))))

  (method ^:private readShort ^short [this ^:final ^int offset]
    (let [bytecode (.-data code)]
      (unchecked-short
        (bit-or-int (bit-shift-left-int (bit-and-int (aget bytecode offset) 0xFF) 8)
                    (bit-and-int (aget bytecode (unchecked-add-int offset 1)) 0xFF)))))

  (method ^:private readInt ^int [this ^:final ^int offset]
    (let [bytecode (.-data code)]
      (bit-or-int (bit-or-int (bit-or-int (bit-shift-left-int
                                            (bit-and-int (aget bytecode offset) 0xFF)
                                            24)
                                          (bit-shift-left-int
                                            (bit-and-int
                                              (aget bytecode (unchecked-add-int offset 1))
                                              0xFF)
                                            16))
                              (bit-shift-left-int
                                (bit-and-int (aget bytecode (unchecked-add-int offset 2)) 0xFF)
                                8))
                  (bit-and-int (aget bytecode (unchecked-add-int offset 3)) 0xFF))))

  (method canCopyMethodAttributes ^boolean [this ^:final ^ClassReader source
                                            ^:final ^boolean hasSyntheticAttribute
                                            ^:final ^boolean hasDeprecatedAttribute
                                            ^:final ^int descriptorIndex ^:final ^int signatureIndex
                                            ^:final ^int exceptionsOffset]
    (if (or (or (or (not (identical? source (.getSource symbolTable)))
                    (not (== descriptorIndex (.-descriptorIndex this))))
                (not (== signatureIndex (.-signatureIndex this))))
            (not (= hasDeprecatedAttribute
                    (not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)))))
        false
        (let [needSyntheticAttribute (and (< (.getMajorVersion symbolTable) Opcodes/V1_5)
                                          (not (==
                                                 (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC)
                                                 0)))]
          (if (not (= hasSyntheticAttribute needSyntheticAttribute))
              false
              (do
                (cond
                  (== exceptionsOffset 0) (when-not (== numberOfExceptions 0) (return false))
                  (== (.readUnsignedShort source exceptionsOffset) numberOfExceptions)
                    (let [^:mutable currentExceptionOffset (unchecked-add-int exceptionsOffset 2)]
                      (loop [^int i 0]
                        (when (< i numberOfExceptions)
                          (when-not (== (.readUnsignedShort source currentExceptionOffset)
                                        (aget exceptionIndexTable i))
                            (return false))
                          (set! currentExceptionOffset (unchecked-add-int currentExceptionOffset 2))
                          (recur (unchecked-inc-int i))))))
                true)))))

  (method setMethodAttributesSource ^void [this ^:final ^int methodInfoOffset
                                           ^:final ^int methodInfoLength]
    (set! (.-sourceOffset this) (unchecked-add-int methodInfoOffset 6))
    (set! (.-sourceLength this) (unchecked-subtract-int methodInfoLength 6)))

  (method computeMethodInfoSize ^int [this]
    (if (not (== sourceOffset 0))
        (unchecked-add-int 6 sourceLength)
        (let [^:mutable ^int size 8]
          (when (> (.-length code) 0)
            (when (> (.-length code) 65535)
              (throw (MethodTooLargeException. (.getClassName symbolTable)
                                               name
                                               descriptor
                                               (.-length code))))
            (.addConstantUtf8 symbolTable Constants/CODE)
            (set! size
                  (unchecked-add-int size
                                     (unchecked-add-int
                                       (unchecked-add-int 16 (.-length code))
                                       (Handler/getExceptionTableSize firstHandler))))
            (when (some? stackMapTableEntries)
              (let [useStackMapTable (>= (.getMajorVersion symbolTable) Opcodes/V1_6)]
                (.addConstantUtf8 symbolTable
                                  (if useStackMapTable Constants/STACK_MAP_TABLE "StackMap"))
                (set!
                  size
                  (unchecked-add-int size (unchecked-add-int 8 (.-length stackMapTableEntries))))))
            (when (some? lineNumberTable)
              (.addConstantUtf8 symbolTable Constants/LINE_NUMBER_TABLE)
              (set! size (unchecked-add-int size (unchecked-add-int 8 (.-length lineNumberTable)))))
            (when (some? localVariableTable)
              (.addConstantUtf8 symbolTable Constants/LOCAL_VARIABLE_TABLE)
              (set! size
                    (unchecked-add-int size (unchecked-add-int 8 (.-length localVariableTable)))))
            (when (some? localVariableTypeTable)
              (.addConstantUtf8 symbolTable Constants/LOCAL_VARIABLE_TYPE_TABLE)
              (set!
                size
                (unchecked-add-int size (unchecked-add-int 8 (.-length localVariableTypeTable)))))
            (when (some? lastCodeRuntimeVisibleTypeAnnotation)
              (set! size
                    (unchecked-add-int size
                                       (.computeAnnotationsSize
                                         lastCodeRuntimeVisibleTypeAnnotation
                                         Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS))))
            (when (some? lastCodeRuntimeInvisibleTypeAnnotation)
              (set! size
                    (unchecked-add-int size
                                       (.computeAnnotationsSize
                                         lastCodeRuntimeInvisibleTypeAnnotation
                                         Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS))))
            (when (some? firstCodeAttribute)
              (set! size
                    (unchecked-add-int size
                                       (.computeAttributesSize
                                         firstCodeAttribute
                                         symbolTable
                                         (.-data code)
                                         (.-length code)
                                         maxStack
                                         maxLocals)))))
          (when (> numberOfExceptions 0)
            (.addConstantUtf8 symbolTable Constants/EXCEPTIONS)
            (set! size
                  (unchecked-add-int size
                                     (unchecked-add-int
                                       8
                                       (unchecked-multiply-int 2 numberOfExceptions)))))
          (set! size
                (unchecked-add-int size
                                   (Attribute/computeAttributesSize
                                     symbolTable
                                     accessFlags
                                     signatureIndex)))
          (set! size
                (unchecked-add-int size
                                   (AnnotationWriter/computeAnnotationsSize
                                     lastRuntimeVisibleAnnotation
                                     lastRuntimeInvisibleAnnotation
                                     lastRuntimeVisibleTypeAnnotation
                                     lastRuntimeInvisibleTypeAnnotation)))
          (when (some? lastRuntimeVisibleParameterAnnotations)
            (set! size
                  (unchecked-add-int size
                                     (AnnotationWriter/computeParameterAnnotationsSize
                                       Constants/RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS
                                       lastRuntimeVisibleParameterAnnotations
                                       (if (== visibleAnnotableParameterCount 0)
                                           (alength lastRuntimeVisibleParameterAnnotations)
                                           visibleAnnotableParameterCount)))))
          (when (some? lastRuntimeInvisibleParameterAnnotations)
            (set! size
                  (unchecked-add-int size
                                     (AnnotationWriter/computeParameterAnnotationsSize
                                       Constants/RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS
                                       lastRuntimeInvisibleParameterAnnotations
                                       (if (== invisibleAnnotableParameterCount 0)
                                           (alength lastRuntimeInvisibleParameterAnnotations)
                                           invisibleAnnotableParameterCount)))))
          (when (some? defaultValue)
            (.addConstantUtf8 symbolTable Constants/ANNOTATION_DEFAULT)
            (set! size (unchecked-add-int size (unchecked-add-int 6 (.-length defaultValue)))))
          (when (some? parameters)
            (.addConstantUtf8 symbolTable Constants/METHOD_PARAMETERS)
            (set! size (unchecked-add-int size (unchecked-add-int 7 (.-length parameters)))))
          (when (some? firstAttribute)
            (set! size (unchecked-add-int size (.computeAttributesSize firstAttribute symbolTable))))
          size)))

  (method putMethodInfo ^void [this ^:final ^ByteVector output]
    (let [useSyntheticAttribute (< (.getMajorVersion symbolTable) Opcodes/V1_5)
          mask (if useSyntheticAttribute Opcodes/ACC_SYNTHETIC 0)]
      (.putShort (.putShort (.putShort output (bit-and-int accessFlags (bit-not-int mask)))
                            nameIndex)
                 descriptorIndex)
      (if (not (== sourceOffset 0))
          (.putByteArray output
                         (.-classFileBuffer (.getSource symbolTable))
                         sourceOffset
                         sourceLength)
          (let [^:mutable ^int attributeCount 0]
            (when (> (.-length code) 0) (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (> numberOfExceptions 0) (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (and (not (== (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC) 0))
                       useSyntheticAttribute)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when-not (== signatureIndex 0)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when-not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeVisibleAnnotation)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeInvisibleAnnotation)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeVisibleParameterAnnotations)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeInvisibleParameterAnnotations)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeVisibleTypeAnnotation)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? lastRuntimeInvisibleTypeAnnotation)
              (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? defaultValue) (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? parameters) (set! attributeCount (unchecked-inc-int attributeCount)))
            (when (some? firstAttribute)
              (set! attributeCount
                    (unchecked-add-int attributeCount (.getAttributeCount firstAttribute))))
            (.putShort output attributeCount)
            (when (> (.-length code) 0)
              (let [^:mutable size (unchecked-add-int
                                     (unchecked-add-int 10 (.-length code))
                                     (Handler/getExceptionTableSize firstHandler))
                    ^:mutable ^int codeAttributeCount 0]
                (when (some? stackMapTableEntries)
                  (set! size
                        (unchecked-add-int size
                                           (unchecked-add-int 8 (.-length stackMapTableEntries))))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? lineNumberTable)
                  (set! size
                        (unchecked-add-int size (unchecked-add-int 8 (.-length lineNumberTable))))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? localVariableTable)
                  (set!
                    size
                    (unchecked-add-int size (unchecked-add-int 8 (.-length localVariableTable))))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? localVariableTypeTable)
                  (set! size
                        (unchecked-add-int size
                                           (unchecked-add-int 8 (.-length localVariableTypeTable))))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? lastCodeRuntimeVisibleTypeAnnotation)
                  (set! size
                        (unchecked-add-int size
                                           (.computeAnnotationsSize
                                             lastCodeRuntimeVisibleTypeAnnotation
                                             Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS)))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? lastCodeRuntimeInvisibleTypeAnnotation)
                  (set! size
                        (unchecked-add-int size
                                           (.computeAnnotationsSize
                                             lastCodeRuntimeInvisibleTypeAnnotation
                                             Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)))
                  (set! codeAttributeCount (unchecked-inc-int codeAttributeCount)))
                (when (some? firstCodeAttribute)
                  (set! size
                        (unchecked-add-int size
                                           (.computeAttributesSize
                                             firstCodeAttribute
                                             symbolTable
                                             (.-data code)
                                             (.-length code)
                                             maxStack
                                             maxLocals)))
                  (set! codeAttributeCount
                        (unchecked-add-int codeAttributeCount
                                           (.getAttributeCount firstCodeAttribute))))
                (.putByteArray
                  (.putInt (.putShort (.putShort
                                        (.putInt
                                          (.putShort
                                            output
                                            (.addConstantUtf8 symbolTable Constants/CODE))
                                          size)
                                        maxStack)
                                      maxLocals)
                           (.-length code))
                  (.-data code)
                  0
                  (.-length code))
                (Handler/putExceptionTable firstHandler output)
                (.putShort output codeAttributeCount)
                (when (some? stackMapTableEntries)
                  (let [useStackMapTable (>= (.getMajorVersion symbolTable) Opcodes/V1_6)]
                    (.putByteArray
                      (.putShort (.putInt (.putShort
                                            output
                                            (.addConstantUtf8
                                              symbolTable
                                              (if useStackMapTable
                                                  Constants/STACK_MAP_TABLE
                                                  "StackMap")))
                                          (unchecked-add-int 2 (.-length stackMapTableEntries)))
                                 stackMapTableNumberOfEntries)
                      (.-data stackMapTableEntries)
                      0
                      (.-length stackMapTableEntries))))
                (when (some? lineNumberTable)
                  (.putByteArray
                    (.putShort (.putInt
                                 (.putShort output
                                            (.addConstantUtf8
                                              symbolTable
                                              Constants/LINE_NUMBER_TABLE))
                                 (unchecked-add-int 2 (.-length lineNumberTable)))
                               lineNumberTableLength)
                    (.-data lineNumberTable)
                    0
                    (.-length lineNumberTable)))
                (when (some? localVariableTable)
                  (.putByteArray
                    (.putShort (.putInt (.putShort
                                          output
                                          (.addConstantUtf8
                                            symbolTable
                                            Constants/LOCAL_VARIABLE_TABLE))
                                        (unchecked-add-int 2 (.-length localVariableTable)))
                               localVariableTableLength)
                    (.-data localVariableTable)
                    0
                    (.-length localVariableTable)))
                (when (some? localVariableTypeTable)
                  (.putByteArray
                    (.putShort (.putInt (.putShort
                                          output
                                          (.addConstantUtf8
                                            symbolTable
                                            Constants/LOCAL_VARIABLE_TYPE_TABLE))
                                        (unchecked-add-int 2 (.-length localVariableTypeTable)))
                               localVariableTypeTableLength)
                    (.-data localVariableTypeTable)
                    0
                    (.-length localVariableTypeTable)))
                (when (some? lastCodeRuntimeVisibleTypeAnnotation)
                  (.putAnnotations lastCodeRuntimeVisibleTypeAnnotation
                                   (.addConstantUtf8
                                     symbolTable
                                     Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS)
                                   output))
                (when (some? lastCodeRuntimeInvisibleTypeAnnotation)
                  (.putAnnotations lastCodeRuntimeInvisibleTypeAnnotation
                                   (.addConstantUtf8
                                     symbolTable
                                     Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)
                                   output))
                (when (some? firstCodeAttribute)
                  (.putAttributes firstCodeAttribute
                                  symbolTable
                                  (.-data code)
                                  (.-length code)
                                  maxStack
                                  maxLocals
                                  output))))
            (when (> numberOfExceptions 0)
              (.putShort (.putInt (.putShort output
                                             (.addConstantUtf8 symbolTable Constants/EXCEPTIONS))
                                  (unchecked-add-int
                                    2
                                    (unchecked-multiply-int 2 numberOfExceptions)))
                         numberOfExceptions)
              (for-each [^int exceptionIndex exceptionIndexTable] (.putShort output exceptionIndex)))
            (Attribute/putAttributes symbolTable accessFlags signatureIndex output)
            (AnnotationWriter/putAnnotations symbolTable
                                             lastRuntimeVisibleAnnotation
                                             lastRuntimeInvisibleAnnotation
                                             lastRuntimeVisibleTypeAnnotation
                                             lastRuntimeInvisibleTypeAnnotation
                                             output)
            (when (some? lastRuntimeVisibleParameterAnnotations)
              (AnnotationWriter/putParameterAnnotations
                (.addConstantUtf8 symbolTable Constants/RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)
                lastRuntimeVisibleParameterAnnotations
                (if (== visibleAnnotableParameterCount 0)
                    (alength lastRuntimeVisibleParameterAnnotations)
                    visibleAnnotableParameterCount)
                output))
            (when (some? lastRuntimeInvisibleParameterAnnotations)
              (AnnotationWriter/putParameterAnnotations
                (.addConstantUtf8 symbolTable Constants/RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)
                lastRuntimeInvisibleParameterAnnotations
                (if (== invisibleAnnotableParameterCount 0)
                    (alength lastRuntimeInvisibleParameterAnnotations)
                    invisibleAnnotableParameterCount)
                output))
            (when (some? defaultValue)
              (.putByteArray
                (.putInt (.putShort output
                                    (.addConstantUtf8 symbolTable Constants/ANNOTATION_DEFAULT))
                         (.-length defaultValue))
                (.-data defaultValue)
                0
                (.-length defaultValue)))
            (when (some? parameters)
              (.putByteArray
                (.putByte (.putInt (.putShort output
                                              (.addConstantUtf8
                                                symbolTable
                                                Constants/METHOD_PARAMETERS))
                                   (unchecked-add-int 1 (.-length parameters)))
                          parametersCount)
                (.-data parameters)
                0
                (.-length parameters)))
            (when (some? firstAttribute) (.putAttributes firstAttribute symbolTable output))))))

  (method ^:final collectAttributePrototypes ^void [this ^:final ^Attribute$Set attributePrototypes]
    (.addAttributes attributePrototypes firstAttribute)
    (.addAttributes attributePrototypes firstCodeAttribute)))
