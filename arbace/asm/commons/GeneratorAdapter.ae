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
;; Converted from clojure/asm/commons/GeneratorAdapter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.commons)

(import '(arbace.asm ClassVisitor ConstantDynamic Handle Label MethodVisitor Opcodes Type)
        '(java.util ArrayList Arrays List))

(defclass ^:public GeneratorAdapter
  :extends LocalVariablesSorter

  (field ^:private ^:static ^:final ^String CLASS_DESCRIPTOR "Ljava/lang/Class;")

  (field ^:private ^:static ^:final ^Type BYTE_TYPE (Type/getObjectType "java/lang/Byte"))

  (field ^:private ^:static ^:final ^Type BOOLEAN_TYPE (Type/getObjectType "java/lang/Boolean"))

  (field ^:private ^:static ^:final ^Type SHORT_TYPE (Type/getObjectType "java/lang/Short"))

  (field ^:private ^:static ^:final ^Type CHARACTER_TYPE (Type/getObjectType "java/lang/Character"))

  (field ^:private ^:static ^:final ^Type INTEGER_TYPE (Type/getObjectType "java/lang/Integer"))

  (field ^:private ^:static ^:final ^Type FLOAT_TYPE (Type/getObjectType "java/lang/Float"))

  (field ^:private ^:static ^:final ^Type LONG_TYPE (Type/getObjectType "java/lang/Long"))

  (field ^:private ^:static ^:final ^Type DOUBLE_TYPE (Type/getObjectType "java/lang/Double"))

  (field ^:private ^:static ^:final ^Type NUMBER_TYPE (Type/getObjectType "java/lang/Number"))

  (field ^:private ^:static ^:final ^Type OBJECT_TYPE (Type/getObjectType "java/lang/Object"))

  (field ^:private ^:static ^:final ^Method BOOLEAN_VALUE
    (Method/getMethod "boolean booleanValue()"))

  (field ^:private ^:static ^:final ^Method CHAR_VALUE (Method/getMethod "char charValue()"))

  (field ^:private ^:static ^:final ^Method INT_VALUE (Method/getMethod "int intValue()"))

  (field ^:private ^:static ^:final ^Method FLOAT_VALUE (Method/getMethod "float floatValue()"))

  (field ^:private ^:static ^:final ^Method LONG_VALUE (Method/getMethod "long longValue()"))

  (field ^:private ^:static ^:final ^Method DOUBLE_VALUE (Method/getMethod "double doubleValue()"))

  (field ^:public ^:static ^:final ^int ADD Opcodes/IADD)

  (field ^:public ^:static ^:final ^int SUB Opcodes/ISUB)

  (field ^:public ^:static ^:final ^int MUL Opcodes/IMUL)

  (field ^:public ^:static ^:final ^int DIV Opcodes/IDIV)

  (field ^:public ^:static ^:final ^int REM Opcodes/IREM)

  (field ^:public ^:static ^:final ^int NEG Opcodes/INEG)

  (field ^:public ^:static ^:final ^int SHL Opcodes/ISHL)

  (field ^:public ^:static ^:final ^int SHR Opcodes/ISHR)

  (field ^:public ^:static ^:final ^int USHR Opcodes/IUSHR)

  (field ^:public ^:static ^:final ^int AND Opcodes/IAND)

  (field ^:public ^:static ^:final ^int OR Opcodes/IOR)

  (field ^:public ^:static ^:final ^int XOR Opcodes/IXOR)

  (field ^:public ^:static ^:final ^int EQ Opcodes/IFEQ)

  (field ^:public ^:static ^:final ^int NE Opcodes/IFNE)

  (field ^:public ^:static ^:final ^int LT Opcodes/IFLT)

  (field ^:public ^:static ^:final ^int GE Opcodes/IFGE)

  (field ^:public ^:static ^:final ^int GT Opcodes/IFGT)

  (field ^:public ^:static ^:final ^int LE Opcodes/IFLE)

  (field ^:private ^:final ^int access)

  (field ^:private ^:final ^String name)

  (field ^:private ^:final ^Type returnType)

  (field ^:private ^:final ^Type/1 argumentTypes)

  (field ^:private ^:final ^{:tag (List Type)} localTypes (ArrayList.))

  (constructor ^:public [this ^:final ^MethodVisitor methodVisitor ^:final ^int access
                         ^:final ^String name ^:final ^String descriptor]
    (this. Opcodes/ASM9 methodVisitor access name descriptor)
    (when-not (identical? (.getClass this) GeneratorAdapter) (throw (IllegalStateException.))))

  (constructor ^:protected [this ^:final ^int api ^:final ^MethodVisitor methodVisitor
                            ^:final ^int access ^:final ^String name ^:final ^String descriptor]
    (super. api access descriptor methodVisitor)
    (set! (.-access this) access)
    (set! (.-name this) name)
    (set! (.-returnType this) (Type/getReturnType descriptor))
    (set! (.-argumentTypes this) (Type/getArgumentTypes descriptor)))

  (constructor ^:public [this ^:final ^int access ^:final ^Method method
                         ^:final ^MethodVisitor methodVisitor]
    (this. methodVisitor access (.getName method) (.getDescriptor method)))

  (constructor ^:public [this ^:final ^int access ^:final ^Method method ^:final ^String signature
                         ^:final ^Type/1 exceptions ^:final ^ClassVisitor classVisitor]
    (this. access
           method
           (.visitMethod classVisitor
                         access
                         (.getName method)
                         (.getDescriptor method)
                         signature
                         (when (some? exceptions) (GeneratorAdapter/getInternalNames exceptions)))))

  (method ^:private ^:static getInternalNames ^String/1 [^:final ^Type/1 types]
    (let [names (new String/1 (alength types))]
      (loop [^int i 0]
        (when (< i (alength names))
          (aset names i (.getInternalName (aget types i)))
          (recur (unchecked-inc-int i))))
      names))

  (method ^:public getAccess ^int [this] access)

  (method ^:public getName ^String [this] name)

  (method ^:public getReturnType ^Type [this] returnType)

  (method ^:public getArgumentTypes ^Type/1 [this] (.clone argumentTypes))

  (method ^:public push ^void [this ^:final ^boolean value]
    (^[int] GeneratorAdapter/.push this (if value 1 0)))

  (method ^:public push ^void [this ^:final ^int value]
    (cond
      (and (>= value -1) (<= value 5))
        (.visitInsn (.-mv this) (unchecked-add-int Opcodes/ICONST_0 value))
      (and (>= value Byte/MIN_VALUE) (<= value Byte/MAX_VALUE))
        (.visitIntInsn (.-mv this) Opcodes/BIPUSH value)
      (and (>= value Short/MIN_VALUE) (<= value Short/MAX_VALUE))
        (.visitIntInsn (.-mv this) Opcodes/SIPUSH value)
      :else (.visitLdcInsn (.-mv this) value)))

  (method ^:public push ^void [this ^:final ^long value]
    (if (or (== value 0) (== value 1))
        (.visitInsn (.-mv this) (unchecked-add-int Opcodes/LCONST_0 (unchecked-int value)))
        (.visitLdcInsn (.-mv this) value)))

  (method ^:public push ^void [this ^:final ^float value]
    (let [bits (Float/floatToIntBits value)]
      (if (or (or (== bits 0) (== bits 0x3F800000)) (== bits 0x40000000))
          (.visitInsn (.-mv this) (unchecked-add-int Opcodes/FCONST_0 (unchecked-int value)))
          (.visitLdcInsn (.-mv this) value))))

  (method ^:public push ^void [this ^:final ^double value]
    (let [bits (Double/doubleToLongBits value)]
      (if (or (== bits 0) (== bits 0x3FF0000000000000))
          (.visitInsn (.-mv this) (unchecked-add-int Opcodes/DCONST_0 (unchecked-int value)))
          (.visitLdcInsn (.-mv this) value))))

  (method ^:public push ^void [this ^:final ^String value]
    (if (nil? value) (.visitInsn (.-mv this) Opcodes/ACONST_NULL) (.visitLdcInsn (.-mv this) value)))

  (method ^:public push ^void [this ^:final ^Type value]
    (if (nil? value)
        (.visitInsn (.-mv this) Opcodes/ACONST_NULL)
        (switch (.getSort value)
          Type/VOID
            (.visitFieldInsn (.-mv this) Opcodes/GETSTATIC "java/lang/Void" "TYPE" CLASS_DESCRIPTOR)
          Type/BOOLEAN
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Boolean"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          Type/CHAR
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Character"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          Type/BYTE
            (.visitFieldInsn (.-mv this) Opcodes/GETSTATIC "java/lang/Byte" "TYPE" CLASS_DESCRIPTOR)
          Type/SHORT
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Short"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          Type/INT
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Integer"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          Type/FLOAT
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Float"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          Type/LONG
            (.visitFieldInsn (.-mv this) Opcodes/GETSTATIC "java/lang/Long" "TYPE" CLASS_DESCRIPTOR)
          Type/DOUBLE
            (.visitFieldInsn (.-mv this)
                             Opcodes/GETSTATIC
                             "java/lang/Double"
                             "TYPE"
                             CLASS_DESCRIPTOR)
          (.visitLdcInsn (.-mv this) value))))

  (method ^:public push ^void [this ^:final ^Handle handle]
    (if (nil? handle)
        (.visitInsn (.-mv this) Opcodes/ACONST_NULL)
        (.visitLdcInsn (.-mv this) handle)))

  (method ^:public push ^void [this ^:final ^ConstantDynamic constantDynamic]
    (if (nil? constantDynamic)
        (.visitInsn (.-mv this) Opcodes/ACONST_NULL)
        (.visitLdcInsn (.-mv this) constantDynamic)))

  (method ^:private getArgIndex ^int [this ^:final ^int arg]
    (let [^:mutable ^int index (if (== (bit-and-int access Opcodes/ACC_STATIC) 0) 1 0)]
      (loop [^int i 0]
        (when (< i arg)
          (set! index (unchecked-add-int index (.getSize (aget argumentTypes i))))
          (recur (unchecked-inc-int i))))
      index))

  (method ^:private loadInsn ^void [this ^:final ^Type type ^:final ^int index]
    (.visitVarInsn (.-mv this) (.getOpcode type Opcodes/ILOAD) index))

  (method ^:private storeInsn ^void [this ^:final ^Type type ^:final ^int index]
    (.visitVarInsn (.-mv this) (.getOpcode type Opcodes/ISTORE) index))

  (method ^:public loadThis ^void [this]
    (when-not (== (bit-and-int access Opcodes/ACC_STATIC) 0)
      (throw (IllegalStateException. "no 'this' pointer within static method")))
    (.visitVarInsn (.-mv this) Opcodes/ALOAD 0))

  (method ^:public loadArg ^void [this ^:final ^int arg]
    (.loadInsn this (aget argumentTypes arg) (.getArgIndex this arg)))

  (method ^:public loadArgs ^void [this ^:final ^int arg ^:final ^int count]
    (let [^:mutable index (.getArgIndex this arg)]
      (loop [^int i 0]
        (when (< i count)
          (let [argumentType (aget argumentTypes (unchecked-add-int arg i))]
            (.loadInsn this argumentType index)
            (set! index (unchecked-add-int index (.getSize argumentType)))
            (recur (unchecked-inc-int i)))))))

  (method ^:public loadArgs ^void [this]
    (.loadArgs this 0 (alength argumentTypes)))

  (method ^:public loadArgArray ^void [this]
    (.push this (alength argumentTypes))
    (.newArray this OBJECT_TYPE)
    (loop [^int i 0]
      (when (< i (alength argumentTypes))
        (.dup this)
        (.push this i)
        (.loadArg this i)
        (.box this (aget argumentTypes i))
        (.arrayStore this OBJECT_TYPE)
        (recur (unchecked-inc-int i)))))

  (method ^:public storeArg ^void [this ^:final ^int arg]
    (.storeInsn this (aget argumentTypes arg) (.getArgIndex this arg)))

  (method ^:public getLocalType ^Type [this ^:final ^int local]
    (cast Type (.get localTypes (unchecked-subtract-int local (.-firstLocal this)))))

  (method ^:protected setLocalType ^void [this ^:final ^int local ^:final ^Type type]
    (let [index (unchecked-subtract-int local (.-firstLocal this))]
      (while (< (.size localTypes) (unchecked-add-int index 1)) (.add localTypes nil))
      (.set localTypes index type)))

  (method ^:public loadLocal ^void [this ^:final ^int local]
    (.loadInsn this (.getLocalType this local) local))

  (method ^:public loadLocal ^void [this ^:final ^int local ^:final ^Type type]
    (.setLocalType this local type)
    (.loadInsn this type local))

  (method ^:public storeLocal ^void [this ^:final ^int local]
    (.storeInsn this (.getLocalType this local) local))

  (method ^:public storeLocal ^void [this ^:final ^int local ^:final ^Type type]
    (.setLocalType this local type)
    (.storeInsn this type local))

  (method ^:public arrayLoad ^void [this ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type Opcodes/IALOAD)))

  (method ^:public arrayStore ^void [this ^:final ^Type type]
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

  (method ^:public swap ^void [this ^:final ^Type prev ^:final ^Type type]
    (cond
      (== (.getSize type) 1) (if (== (.getSize prev) 1) (.swap this) (do (.dupX2 this) (.pop this)))
      (== (.getSize prev) 1) (do (.dup2X1 this) (.pop2 this))
      :else (do (.dup2X2 this) (.pop2 this))))

  (method ^:public math ^void [this ^:final ^int op ^:final ^Type type]
    (.visitInsn (.-mv this) (.getOpcode type op)))

  (method ^:public not ^void [this]
    (.visitInsn (.-mv this) Opcodes/ICONST_1)
    (.visitInsn (.-mv this) Opcodes/IXOR))

  (method ^:public iinc ^void [this ^:final ^int local ^:final ^int amount]
    (.visitIincInsn (.-mv this) local amount))

  (method ^:public cast ^void [this ^:final ^Type from ^:final ^Type to]
    (when-not (identical? from to)
      (when (or (or (or (< (.getSort from) Type/BOOLEAN) (> (.getSort from) Type/DOUBLE))
                    (< (.getSort to) Type/BOOLEAN))
                (> (.getSort to) Type/DOUBLE))
        (throw (IllegalArgumentException. (java-str "Cannot cast from " from " to " to))))
      (InstructionAdapter/cast (.-mv this) from to)))

  (method ^:private ^:static getBoxedType ^Type [^:final ^Type type]
    (switch (.getSort type)
      Type/BYTE BYTE_TYPE
      Type/BOOLEAN BOOLEAN_TYPE
      Type/SHORT SHORT_TYPE
      Type/CHAR CHARACTER_TYPE
      Type/INT INTEGER_TYPE
      Type/FLOAT FLOAT_TYPE
      Type/LONG LONG_TYPE
      Type/DOUBLE DOUBLE_TYPE
      type))

  (method ^:public box ^void [this ^:final ^Type type]
    (when-not (or (== (.getSort type) Type/OBJECT) (== (.getSort type) Type/ARRAY))
      (if (identical? type Type/VOID_TYPE)
          (.push this (cast String nil))
          (let [boxedType (GeneratorAdapter/getBoxedType type)]
            (.newInstance this boxedType)
            (if (== (.getSize type) 2)
                (do (.dupX2 this) (.dupX2 this) (.pop this))
                (do (.dupX1 this) (.swap this)))
            (.invokeConstructor this
                                boxedType
                                (Method. "<init>" Type/VOID_TYPE (new Type/1 [type])))))))

  (method ^:public valueOf ^void [this ^:final ^Type type]
    (when-not (or (== (.getSort type) Type/OBJECT) (== (.getSort type) Type/ARRAY))
      (if (identical? type Type/VOID_TYPE)
          (.push this (cast String nil))
          (let [boxedType (GeneratorAdapter/getBoxedType type)]
            (.invokeStatic this boxedType (Method. "valueOf" boxedType (new Type/1 [type])))))))

  (method ^:public unbox ^void [this ^:final ^Type type]
    (let [^:mutable boxedType NUMBER_TYPE
          ^:mutable ^Method unboxMethod nil]
      (switch (.getSort type)
        Type/VOID (return)
        Type/CHAR (do (set! boxedType CHARACTER_TYPE) (set! unboxMethod CHAR_VALUE))
        Type/BOOLEAN (do (set! boxedType BOOLEAN_TYPE) (set! unboxMethod BOOLEAN_VALUE))
        Type/DOUBLE (set! unboxMethod DOUBLE_VALUE)
        Type/FLOAT (set! unboxMethod FLOAT_VALUE)
        Type/LONG (set! unboxMethod LONG_VALUE)
        (Type/INT Type/SHORT Type/BYTE) (set! unboxMethod INT_VALUE)
        (set! unboxMethod nil))
      (if (nil? unboxMethod)
          (.checkCast this type)
          (do (.checkCast this boxedType) (.invokeVirtual this boxedType unboxMethod)))))

  (method ^:public newLabel ^Label [this] (Label.))

  (method ^:public mark ^void [this ^:final ^Label label]
    (.visitLabel (.-mv this) label))

  (method ^:public mark ^Label [this]
    (let [label (Label.)] (.visitLabel (.-mv this) label) label))

  (method ^:public ifCmp ^void [this ^:final ^Type type ^:final ^int mode ^:final ^Label label]
    (switch (.getSort type)
      Type/LONG (.visitInsn (.-mv this) Opcodes/LCMP)
      Type/DOUBLE
        (.visitInsn (.-mv this) (if (or (== mode GE) (== mode GT)) Opcodes/DCMPL Opcodes/DCMPG))
      Type/FLOAT
        (.visitInsn (.-mv this) (if (or (== mode GE) (== mode GT)) Opcodes/FCMPL Opcodes/FCMPG))
      (Type/ARRAY Type/OBJECT)
        (cond
          (== mode EQ) (do (.visitJumpInsn (.-mv this) Opcodes/IF_ACMPEQ label) (return))
          (== mode NE) (do (.visitJumpInsn (.-mv this) Opcodes/IF_ACMPNE label) (return))
          :else (throw (IllegalArgumentException. (java-str "Bad comparison for type " type))))
      (let [^:mutable ^int intOp -1]
        (switch mode
          EQ (set! intOp Opcodes/IF_ICMPEQ)
          NE (set! intOp Opcodes/IF_ICMPNE)
          GE (set! intOp Opcodes/IF_ICMPGE)
          LT (set! intOp Opcodes/IF_ICMPLT)
          LE (set! intOp Opcodes/IF_ICMPLE)
          GT (set! intOp Opcodes/IF_ICMPGT)
          (throw (IllegalArgumentException. (java-str "Bad comparison mode " mode))))
        (.visitJumpInsn (.-mv this) intOp label)
        (return)))
    (.visitJumpInsn (.-mv this) mode label))

  (method ^:public ifICmp ^void [this ^:final ^int mode ^:final ^Label label]
    (.ifCmp this Type/INT_TYPE mode label))

  (method ^:public ifZCmp ^void [this ^:final ^int mode ^:final ^Label label]
    (.visitJumpInsn (.-mv this) mode label))

  (method ^:public ifNull ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFNULL label))

  (method ^:public ifNonNull ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/IFNONNULL label))

  (method ^:public goTo ^void [this ^:final ^Label label]
    (.visitJumpInsn (.-mv this) Opcodes/GOTO label))

  (method ^:public ret ^void [this ^:final ^int local]
    (.visitVarInsn (.-mv this) Opcodes/RET local))

  (method ^:public tableSwitch ^void [this ^:final ^int/1 keys
                                      ^:final ^TableSwitchGenerator generator]
    (let [^float density (if (== (alength keys) 0)
                             (float 0.0)
                             (unchecked-divide-float
                               (float (alength keys))
                               (unchecked-add-int
                                 (unchecked-subtract-int
                                   (aget keys (unchecked-subtract-int (alength keys) 1))
                                   (aget keys 0))
                                 1)))]
      (.tableSwitch this keys generator (>= density (float 0.5)))))

  (method ^:public tableSwitch ^void [this ^:final ^int/1 keys
                                      ^:final ^TableSwitchGenerator generator
                                      ^:final ^boolean useTable]
    (loop [^int i 1]
      (if (< i (alength keys))
          (if (< (aget keys i) (aget keys (unchecked-subtract-int i 1)))
              (throw (IllegalArgumentException. "keys must be sorted in ascending order"))
              (recur (unchecked-inc-int i)))
          nil))
    (let [defaultLabel (.newLabel this)
          endLabel (.newLabel this)]
      (when (> (alength keys) 0)
        (let [numKeys (alength keys)]
          (if useTable
              (let [min (aget keys 0)
                    max (aget keys (unchecked-subtract-int numKeys 1))
                    range (unchecked-add-int (unchecked-subtract-int max min) 1)
                    labels (new Label/1 range)]
                (Arrays/fill labels defaultLabel)
                (loop [^int i 0]
                  (when (< i numKeys)
                    (aset labels (unchecked-subtract-int (aget keys i) min) (.newLabel this))
                    (recur (unchecked-inc-int i))))
                (.visitTableSwitchInsn (.-mv this) min max defaultLabel labels)
                (loop [^int i 0]
                  (when (< i range)
                    (let [label (aget labels i)]
                      (if (not (identical? label defaultLabel))
                          (do
                            (.mark this label)
                            (.generateCase generator (unchecked-add-int i min) endLabel)
                            (recur (unchecked-inc-int i)))
                          (recur (unchecked-inc-int i)))))))
              (let [labels (new Label/1 numKeys)]
                (loop [^int i 0]
                  (when (< i numKeys)
                    (aset labels i (.newLabel this))
                    (recur (unchecked-inc-int i))))
                (.visitLookupSwitchInsn (.-mv this) defaultLabel keys labels)
                (loop [^int i 0]
                  (when (< i numKeys)
                    (.mark this (aget labels i))
                    (.generateCase generator (aget keys i) endLabel)
                    (recur (unchecked-inc-int i))))))))
      (.mark this defaultLabel)
      (.generateDefault generator)
      (.mark this endLabel)))

  (method ^:public returnValue ^void [this]
    (.visitInsn (.-mv this) (.getOpcode returnType Opcodes/IRETURN)))

  (method ^:private fieldInsn ^void [this ^:final ^int opcode ^:final ^Type ownerType
                                     ^:final ^String name ^:final ^Type fieldType]
    (.visitFieldInsn (.-mv this)
                     opcode
                     (.getInternalName ownerType)
                     name
                     (.getDescriptor fieldType)))

  (method ^:public getStatic ^void [this ^:final ^Type owner ^:final ^String name ^:final ^Type type]
    (.fieldInsn this Opcodes/GETSTATIC owner name type))

  (method ^:public putStatic ^void [this ^:final ^Type owner ^:final ^String name ^:final ^Type type]
    (.fieldInsn this Opcodes/PUTSTATIC owner name type))

  (method ^:public getField ^void [this ^:final ^Type owner ^:final ^String name ^:final ^Type type]
    (.fieldInsn this Opcodes/GETFIELD owner name type))

  (method ^:public putField ^void [this ^:final ^Type owner ^:final ^String name ^:final ^Type type]
    (.fieldInsn this Opcodes/PUTFIELD owner name type))

  (method ^:private invokeInsn ^void [this ^:final ^int opcode ^:final ^Type type
                                      ^:final ^Method method ^:final ^boolean isInterface]
    (let [owner (if (== (.getSort type) Type/ARRAY) (.getDescriptor type) (.getInternalName type))]
      (.visitMethodInsn (.-mv this)
                        opcode
                        owner
                        (.getName method)
                        (.getDescriptor method)
                        isInterface)))

  (method ^:public invokeVirtual ^void [this ^:final ^Type owner ^:final ^Method method]
    (.invokeInsn this Opcodes/INVOKEVIRTUAL owner method false))

  (method ^:public invokeConstructor ^void [this ^:final ^Type type ^:final ^Method method]
    (.invokeInsn this Opcodes/INVOKESPECIAL type method false))

  (method ^:public invokeStatic ^void [this ^:final ^Type owner ^:final ^Method method]
    (.invokeInsn this Opcodes/INVOKESTATIC owner method false))

  (method ^:public invokeInterface ^void [this ^:final ^Type owner ^:final ^Method method]
    (.invokeInsn this Opcodes/INVOKEINTERFACE owner method true))

  (method ^:public invokeDynamic ^void [this ^:final ^String name ^:final ^String descriptor
                                        ^:final ^Handle bootstrapMethodHandle &
                                        ^:final ^Object/1 bootstrapMethodArguments]
    (.visitInvokeDynamicInsn (.-mv this)
                             name
                             descriptor
                             bootstrapMethodHandle
                             bootstrapMethodArguments))

  (method ^:private typeInsn ^void [this ^:final ^int opcode ^:final ^Type type]
    (.visitTypeInsn (.-mv this) opcode (.getInternalName type)))

  (method ^:public newInstance ^void [this ^:final ^Type type]
    (.typeInsn this Opcodes/NEW type))

  (method ^:public newArray ^void [this ^:final ^Type type]
    (InstructionAdapter/newarray (.-mv this) type))

  (method ^:public arrayLength ^void [this]
    (.visitInsn (.-mv this) Opcodes/ARRAYLENGTH))

  (method ^:public throwException ^void [this]
    (.visitInsn (.-mv this) Opcodes/ATHROW))

  (method ^:public throwException ^void [this ^:final ^Type type ^:final ^String message]
    (.newInstance this type)
    (.dup this)
    (.push this message)
    (.invokeConstructor this type (Method/getMethod "void <init> (String)"))
    (.throwException this))

  (method ^:public checkCast ^void [this ^:final ^Type type]
    (when-not (.equals type OBJECT_TYPE) (.typeInsn this Opcodes/CHECKCAST type)))

  (method ^:public instanceOf ^void [this ^:final ^Type type]
    (.typeInsn this Opcodes/INSTANCEOF type))

  (method ^:public monitorEnter ^void [this]
    (.visitInsn (.-mv this) Opcodes/MONITORENTER))

  (method ^:public monitorExit ^void [this]
    (.visitInsn (.-mv this) Opcodes/MONITOREXIT))

  (method ^:public endMethod ^void [this]
    (when (== (bit-and-int access Opcodes/ACC_ABSTRACT) 0) (.visitMaxs (.-mv this) 0 0))
    (.visitEnd (.-mv this)))

  (method ^:public catchException ^void [this ^:final ^Label start ^:final ^Label end
                                         ^:final ^Type exception]
    (let [catchLabel (Label.)]
      (if (nil? exception)
          (.visitTryCatchBlock (.-mv this) start end catchLabel nil)
          (.visitTryCatchBlock (.-mv this) start end catchLabel (.getInternalName exception)))
      (.mark this catchLabel))))
