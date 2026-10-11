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
;; Converted from clojure/asm/Type.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(import '(java.lang.reflect Constructor Method))

(defclass ^:public ^:final Type
  (field ^:public ^:static ^:final ^int VOID 0)

  (field ^:public ^:static ^:final ^int BOOLEAN 1)

  (field ^:public ^:static ^:final ^int CHAR 2)

  (field ^:public ^:static ^:final ^int BYTE 3)

  (field ^:public ^:static ^:final ^int SHORT 4)

  (field ^:public ^:static ^:final ^int INT 5)

  (field ^:public ^:static ^:final ^int FLOAT 6)

  (field ^:public ^:static ^:final ^int LONG 7)

  (field ^:public ^:static ^:final ^int DOUBLE 8)

  (field ^:public ^:static ^:final ^int ARRAY 9)

  (field ^:public ^:static ^:final ^int OBJECT 10)

  (field ^:public ^:static ^:final ^int METHOD 11)

  (field ^:private ^:static ^:final ^int INTERNAL 12)

  (field ^:private ^:static ^:final ^String PRIMITIVE_DESCRIPTORS "VZCBSIFJD")

  (field ^:public ^:static ^:final ^Type VOID_TYPE
    (Type. VOID PRIMITIVE_DESCRIPTORS VOID (unchecked-add-int VOID 1)))

  (field ^:public ^:static ^:final ^Type BOOLEAN_TYPE
    (Type. BOOLEAN PRIMITIVE_DESCRIPTORS BOOLEAN (unchecked-add-int BOOLEAN 1)))

  (field ^:public ^:static ^:final ^Type CHAR_TYPE
    (Type. CHAR PRIMITIVE_DESCRIPTORS CHAR (unchecked-add-int CHAR 1)))

  (field ^:public ^:static ^:final ^Type BYTE_TYPE
    (Type. BYTE PRIMITIVE_DESCRIPTORS BYTE (unchecked-add-int BYTE 1)))

  (field ^:public ^:static ^:final ^Type SHORT_TYPE
    (Type. SHORT PRIMITIVE_DESCRIPTORS SHORT (unchecked-add-int SHORT 1)))

  (field ^:public ^:static ^:final ^Type INT_TYPE
    (Type. INT PRIMITIVE_DESCRIPTORS INT (unchecked-add-int INT 1)))

  (field ^:public ^:static ^:final ^Type FLOAT_TYPE
    (Type. FLOAT PRIMITIVE_DESCRIPTORS FLOAT (unchecked-add-int FLOAT 1)))

  (field ^:public ^:static ^:final ^Type LONG_TYPE
    (Type. LONG PRIMITIVE_DESCRIPTORS LONG (unchecked-add-int LONG 1)))

  (field ^:public ^:static ^:final ^Type DOUBLE_TYPE
    (Type. DOUBLE PRIMITIVE_DESCRIPTORS DOUBLE (unchecked-add-int DOUBLE 1)))

  (field ^:private ^:final ^int sort)

  (field ^:private ^:final ^String valueBuffer)

  (field ^:private ^:final ^int valueBegin)

  (field ^:private ^:final ^int valueEnd)

  (constructor ^:private [this ^:final ^int sort ^:final ^String valueBuffer ^:final ^int valueBegin
                          ^:final ^int valueEnd]
    (set! (.-sort this) sort)
    (set! (.-valueBuffer this) valueBuffer)
    (set! (.-valueBegin this) valueBegin)
    (set! (.-valueEnd this) valueEnd))

  (method ^:public ^:static getType ^Type [^:final ^String typeDescriptor]
    (Type/getTypeInternal typeDescriptor 0 (.length typeDescriptor)))

  (method ^:public ^:static getType ^Type [^:final ^{:tag (Class ?)} clazz]
    (if (.isPrimitive clazz)
        (cond
          (identical? clazz Integer/TYPE) INT_TYPE
          (identical? clazz Void/TYPE) VOID_TYPE
          (identical? clazz Boolean/TYPE) BOOLEAN_TYPE
          (identical? clazz Byte/TYPE) BYTE_TYPE
          (identical? clazz Character/TYPE) CHAR_TYPE
          (identical? clazz Short/TYPE) SHORT_TYPE
          (identical? clazz Double/TYPE) DOUBLE_TYPE
          (identical? clazz Float/TYPE) FLOAT_TYPE
          (identical? clazz Long/TYPE) LONG_TYPE
          :else (throw (AssertionError.)))
        (Type/getType (Type/getDescriptor clazz))))

  (method ^:public ^:static getType ^Type [^:final ^{:tag (Constructor ?)} constructor]
    (Type/getType (Type/getConstructorDescriptor constructor)))

  (method ^:public ^:static getType ^Type [^:final ^Method method]
    (Type/getType (Type/getMethodDescriptor method)))

  (method ^:public getElementType ^Type [this]
    (let [numDimensions (.getDimensions this)]
      (Type/getTypeInternal valueBuffer (unchecked-add-int valueBegin numDimensions) valueEnd)))

  (method ^:public ^:static getObjectType ^Type [^:final ^String internalName]
    (Type. (if (== (.charAt internalName 0) \[) ARRAY INTERNAL)
           internalName
           0
           (.length internalName)))

  (method ^:public ^:static getMethodType ^Type [^:final ^String methodDescriptor]
    (Type. METHOD methodDescriptor 0 (.length methodDescriptor)))

  (method ^:public ^:static getMethodType ^Type [^:final ^Type returnType &
                                                 ^:final ^Type/1 argumentTypes]
    (Type/getType (Type/getMethodDescriptor returnType argumentTypes)))

  (method ^:public getArgumentTypes ^Type/1 [this]
    (Type/getArgumentTypes (.getDescriptor this)))

  (method ^:public ^:static getArgumentTypes ^Type/1 [^:final ^String methodDescriptor]
    (let [numArgumentTypes (Type/getArgumentCount methodDescriptor)
          argumentTypes (new Type/1 numArgumentTypes)
          ^:mutable ^int currentOffset 1
          ^:mutable ^int currentArgumentTypeIndex 0]
      (while (not (== (.charAt methodDescriptor currentOffset) \)))
        (let [currentArgumentTypeOffset currentOffset]
          (while (== (.charAt methodDescriptor currentOffset) \[)
            (set! currentOffset (unchecked-inc-int currentOffset)))
          (when (== (.charAt methodDescriptor
                             (let [old-1 currentOffset]
                               (set! currentOffset (unchecked-inc-int currentOffset))
                               old-1))
                    \L)
            (let [semiColumnOffset (.indexOf methodDescriptor \; currentOffset)]
              (set! currentOffset (Math/max currentOffset (unchecked-add-int semiColumnOffset 1)))))
          (when (== (.charAt methodDescriptor currentArgumentTypeOffset) \()
            (throw (IllegalArgumentException. (java-str "Invalid descriptor: " methodDescriptor))))
          (aset argumentTypes
                currentArgumentTypeIndex
                (Type/getTypeInternal methodDescriptor currentArgumentTypeOffset currentOffset))
          (set! currentArgumentTypeIndex (unchecked-inc-int currentArgumentTypeIndex))))
      argumentTypes))

  (method ^:public ^:static getArgumentTypes ^Type/1 [^:final ^Method method]
    (let [^{:tag (array (Class ?))} classes (.getParameterTypes method)
          types (new Type/1 (alength classes))]
      (loop [^int i (unchecked-subtract-int (alength classes) 1)]
        (when (>= i 0) (aset types i (Type/getType (aget classes i))) (recur (unchecked-dec-int i))))
      types))

  (method ^:public getReturnType ^Type [this]
    (Type/getReturnType (.getDescriptor this)))

  (method ^:public ^:static getReturnType ^Type [^:final ^String methodDescriptor]
    (Type/getTypeInternal methodDescriptor
                          (Type/getReturnTypeOffset methodDescriptor)
                          (.length methodDescriptor)))

  (method ^:public ^:static getReturnType ^Type [^:final ^Method method]
    (Type/getType (.getReturnType method)))

  (method ^:static getReturnTypeOffset ^int [^:final ^String methodDescriptor]
    (let [^:mutable ^int currentOffset 1]
      (while (not (== (.charAt methodDescriptor currentOffset) \)))
        (while (== (.charAt methodDescriptor currentOffset) \[)
          (set! currentOffset (unchecked-inc-int currentOffset)))
        (when (== (.charAt methodDescriptor
                           (let [old-2 currentOffset]
                             (set! currentOffset (unchecked-inc-int currentOffset))
                             old-2))
                  \L)
          (let [semiColumnOffset (.indexOf methodDescriptor \; currentOffset)]
            (set! currentOffset (Math/max currentOffset (unchecked-add-int semiColumnOffset 1))))))
      (when (== (.charAt methodDescriptor (unchecked-add-int currentOffset 1)) \()
        (throw (IllegalArgumentException. (java-str "Invalid descriptor: " methodDescriptor))))
      (unchecked-add-int currentOffset 1)))

  (method ^:private ^:static getTypeInternal ^Type [^:final ^String descriptorBuffer
                                                    ^:final ^int descriptorBegin
                                                    ^:final ^int descriptorEnd]
    (switch (.charAt descriptorBuffer descriptorBegin)
      \V VOID_TYPE
      \Z BOOLEAN_TYPE
      \C CHAR_TYPE
      \B BYTE_TYPE
      \S SHORT_TYPE
      \I INT_TYPE
      \F FLOAT_TYPE
      \J LONG_TYPE
      \D DOUBLE_TYPE
      \[ (Type. ARRAY descriptorBuffer descriptorBegin descriptorEnd)
      \L
        (Type. OBJECT
               descriptorBuffer
               (unchecked-add-int descriptorBegin 1)
               (unchecked-subtract-int descriptorEnd 1))
      \( (Type. METHOD descriptorBuffer descriptorBegin descriptorEnd)
      (throw (IllegalArgumentException. (java-str "Invalid descriptor: " descriptorBuffer)))))

  (method ^:public getClassName ^String [this]
    (switch sort
      VOID "void"
      BOOLEAN "boolean"
      CHAR "char"
      BYTE "byte"
      SHORT "short"
      INT "int"
      FLOAT "float"
      LONG "long"
      DOUBLE "double"
      ARRAY
        (let [stringBuilder (StringBuilder. (.getClassName (.getElementType this)))]
          (loop [^int i (.getDimensions this)]
            (when (> i 0) (.append stringBuilder "[]") (recur (unchecked-dec-int i))))
          (.toString stringBuilder))
      (OBJECT INTERNAL) (.replace (.substring valueBuffer valueBegin valueEnd) \/ \.)
      (throw (AssertionError.))))

  (method ^:public getInternalName ^String [this]
    (.substring valueBuffer valueBegin valueEnd))

  (method ^:public ^:static getInternalName ^String [^:final ^{:tag (Class ?)} clazz]
    (.replace (.getName clazz) \. \/))

  (method ^:public getDescriptor ^String [this]
    (cond
      (== sort OBJECT)
        (.substring valueBuffer
                    (unchecked-subtract-int valueBegin 1)
                    (unchecked-add-int valueEnd 1))
      (== sort INTERNAL) (java-str \L (.substring valueBuffer valueBegin valueEnd) \;)
      :else (.substring valueBuffer valueBegin valueEnd)))

  (method ^:public ^:static getDescriptor ^String [^:final ^{:tag (Class ?)} clazz]
    (let [stringBuilder (StringBuilder.)]
      (Type/appendDescriptor clazz stringBuilder)
      (.toString stringBuilder)))

  (method ^:public ^:static getConstructorDescriptor ^String [^:final ^{:tag (Constructor ?)} constructor]
    (let [stringBuilder (StringBuilder.)]
      (^[char] StringBuilder/.append stringBuilder \()
      (let [^{:tag (array (Class ?))} parameters (.getParameterTypes constructor)]
        (for-each [^{:tag (Class ?)} parameter parameters]
          (Type/appendDescriptor parameter stringBuilder))
        (.toString (.append stringBuilder ")V")))))

  (method ^:public ^:static getMethodDescriptor ^String [^:final ^Type returnType &
                                                         ^:final ^Type/1 argumentTypes]
    (let [stringBuilder (StringBuilder.)]
      (^[char] StringBuilder/.append stringBuilder \()
      (for-each [^Type argumentType argumentTypes] (.appendDescriptor argumentType stringBuilder))
      (^[char] StringBuilder/.append stringBuilder \))
      (.appendDescriptor returnType stringBuilder)
      (.toString stringBuilder)))

  (method ^:public ^:static getMethodDescriptor ^String [^:final ^Method method]
    (let [stringBuilder (StringBuilder.)]
      (^[char] StringBuilder/.append stringBuilder \()
      (let [^{:tag (array (Class ?))} parameters (.getParameterTypes method)]
        (for-each [^{:tag (Class ?)} parameter parameters]
          (Type/appendDescriptor parameter stringBuilder))
        (^[char] StringBuilder/.append stringBuilder \))
        (Type/appendDescriptor (.getReturnType method) stringBuilder)
        (.toString stringBuilder))))

  (method ^:private appendDescriptor ^void [this ^:final ^StringBuilder stringBuilder]
    (cond
      (== sort OBJECT)
        (.append stringBuilder
                 valueBuffer
                 (unchecked-subtract-int valueBegin 1)
                 (unchecked-add-int valueEnd 1))
      (== sort INTERNAL)
        (^[char] StringBuilder/.append
          (.append (^[char] StringBuilder/.append stringBuilder \L) valueBuffer valueBegin valueEnd)
          \;)
      :else (.append stringBuilder valueBuffer valueBegin valueEnd)))

  (method ^:private ^:static appendDescriptor ^void [^:final ^{:tag (Class ?)} clazz
                                                     ^:final ^StringBuilder stringBuilder]
    (let [^:mutable ^{:tag (Class ?)} currentClass clazz]
      (while (.isArray currentClass)
        (^[char] StringBuilder/.append stringBuilder \[)
        (set! currentClass (.getComponentType currentClass)))
      (if (.isPrimitive currentClass)
          (let [^:mutable ^char descriptor (unchecked-char 0)]
            (cond
              (identical? currentClass Integer/TYPE) (set! descriptor \I)
              (identical? currentClass Void/TYPE) (set! descriptor \V)
              (identical? currentClass Boolean/TYPE) (set! descriptor \Z)
              (identical? currentClass Byte/TYPE) (set! descriptor \B)
              (identical? currentClass Character/TYPE) (set! descriptor \C)
              (identical? currentClass Short/TYPE) (set! descriptor \S)
              (identical? currentClass Double/TYPE) (set! descriptor \D)
              (identical? currentClass Float/TYPE) (set! descriptor \F)
              (identical? currentClass Long/TYPE) (set! descriptor \J)
              :else (throw (AssertionError.)))
            (^[char] StringBuilder/.append stringBuilder descriptor))
          (^[char] StringBuilder/.append
            (.append (^[char] StringBuilder/.append stringBuilder \L)
                     (Type/getInternalName currentClass))
            \;))))

  (method ^:public getSort ^int [this]
    (if (== sort INTERNAL) OBJECT sort))

  (method ^:public getDimensions ^int [this]
    (let [^:mutable ^int numDimensions 1]
      (while (== (.charAt valueBuffer (unchecked-add-int valueBegin numDimensions)) \[)
        (set! numDimensions (unchecked-inc-int numDimensions)))
      numDimensions))

  (method ^:public getSize ^int [this]
    (switch sort
      VOID 0
      (BOOLEAN CHAR BYTE SHORT INT FLOAT ARRAY OBJECT INTERNAL) 1
      (LONG DOUBLE) 2
      (throw (AssertionError.))))

  (method ^:public getArgumentCount ^int [this]
    (Type/getArgumentCount (.getDescriptor this)))

  (method ^:public ^:static getArgumentCount ^int [^:final ^String methodDescriptor]
    (let [^:mutable ^int argumentCount 0
          ^:mutable ^int currentOffset 1]
      (while (not (== (.charAt methodDescriptor currentOffset) \)))
        (while (== (.charAt methodDescriptor currentOffset) \[)
          (set! currentOffset (unchecked-inc-int currentOffset)))
        (when (== (.charAt methodDescriptor
                           (let [old-3 currentOffset]
                             (set! currentOffset (unchecked-inc-int currentOffset))
                             old-3))
                  \L)
          (let [semiColumnOffset (.indexOf methodDescriptor \; currentOffset)]
            (set! currentOffset (Math/max currentOffset (unchecked-add-int semiColumnOffset 1)))))
        (set! argumentCount (unchecked-inc-int argumentCount)))
      argumentCount))

  (method ^:public getArgumentsAndReturnSizes ^int [this]
    (Type/getArgumentsAndReturnSizes (.getDescriptor this)))

  (method ^:public ^:static getArgumentsAndReturnSizes ^int [^:final ^String methodDescriptor]
    (let [^:mutable ^int argumentsSize 1
          ^:mutable ^int currentOffset 1
          ^:mutable ^int currentChar (.charAt methodDescriptor currentOffset)]
      (while (not (== currentChar \)))
        (if (or (== currentChar \J) (== currentChar \D))
            (do
              (set! currentOffset (unchecked-inc-int currentOffset))
              (set! argumentsSize (unchecked-add-int argumentsSize 2)))
            (do
              (while (== (.charAt methodDescriptor currentOffset) \[)
                (set! currentOffset (unchecked-inc-int currentOffset)))
              (when (== (.charAt methodDescriptor
                                 (let [old-4 currentOffset]
                                   (set! currentOffset (unchecked-inc-int currentOffset))
                                   old-4))
                        \L)
                (let [semiColumnOffset (.indexOf methodDescriptor \; currentOffset)]
                  (set! currentOffset
                        (Math/max currentOffset (unchecked-add-int semiColumnOffset 1)))))
              (set! argumentsSize (unchecked-add-int argumentsSize 1))))
        (set! currentChar (.charAt methodDescriptor currentOffset)))
      (set! currentChar (.charAt methodDescriptor (unchecked-add-int currentOffset 1)))
      (if (== currentChar \V)
          (bit-shift-left-int argumentsSize 2)
          (let [^int returnSize (if (or (== currentChar \J) (== currentChar \D)) 2 1)]
            (bit-or-int (bit-shift-left-int argumentsSize 2) returnSize)))))

  (method ^:public getOpcode ^int [this ^:final ^int opcode]
    (if (or (== opcode Opcodes/IALOAD) (== opcode Opcodes/IASTORE))
        (switch sort
          (BOOLEAN BYTE)
            (unchecked-add-int opcode (unchecked-subtract-int Opcodes/BALOAD Opcodes/IALOAD))
          CHAR (unchecked-add-int opcode (unchecked-subtract-int Opcodes/CALOAD Opcodes/IALOAD))
          SHORT (unchecked-add-int opcode (unchecked-subtract-int Opcodes/SALOAD Opcodes/IALOAD))
          INT opcode
          FLOAT (unchecked-add-int opcode (unchecked-subtract-int Opcodes/FALOAD Opcodes/IALOAD))
          LONG (unchecked-add-int opcode (unchecked-subtract-int Opcodes/LALOAD Opcodes/IALOAD))
          DOUBLE (unchecked-add-int opcode (unchecked-subtract-int Opcodes/DALOAD Opcodes/IALOAD))
          (ARRAY OBJECT INTERNAL)
            (unchecked-add-int opcode (unchecked-subtract-int Opcodes/AALOAD Opcodes/IALOAD))
          (METHOD VOID) (throw (UnsupportedOperationException.))
          (throw (AssertionError.)))
        (switch sort
          VOID
            (do
              (when-not (== opcode Opcodes/IRETURN) (throw (UnsupportedOperationException.)))
              Opcodes/RETURN)
          (BOOLEAN BYTE CHAR SHORT INT) opcode
          FLOAT (unchecked-add-int opcode (unchecked-subtract-int Opcodes/FRETURN Opcodes/IRETURN))
          LONG (unchecked-add-int opcode (unchecked-subtract-int Opcodes/LRETURN Opcodes/IRETURN))
          DOUBLE (unchecked-add-int opcode (unchecked-subtract-int Opcodes/DRETURN Opcodes/IRETURN))
          (ARRAY OBJECT INTERNAL)
            (do
              (when (and (and (not (== opcode Opcodes/ILOAD)) (not (== opcode Opcodes/ISTORE)))
                         (not (== opcode Opcodes/IRETURN)))
                (throw (UnsupportedOperationException.)))
              (unchecked-add-int opcode (unchecked-subtract-int Opcodes/ARETURN Opcodes/IRETURN)))
          METHOD (throw (UnsupportedOperationException.))
          (throw (AssertionError.)))))

  (method ^:public equals ^boolean [this ^:final object]
    (cond
      (identical? this object) true
      (not (instance? Type object)) false
      :else
        (let [other (cast Type object)]
          (if (not (== (if (== sort INTERNAL) OBJECT sort)
                       (if (== (.-sort other) INTERNAL) OBJECT (.-sort other))))
              false
              (let [begin valueBegin
                    end valueEnd
                    otherBegin (.-valueBegin other)
                    otherEnd (.-valueEnd other)]
                (if (not (== (unchecked-subtract-int end begin)
                             (unchecked-subtract-int otherEnd otherBegin)))
                    false
                    (do
                      (loop [^int i begin
                             ^int j otherBegin]
                        (if (< i end)
                            (if (not (== (.charAt valueBuffer i) (.charAt (.-valueBuffer other) j)))
                                (return false)
                                (recur (unchecked-inc-int i) (unchecked-inc-int j)))
                            nil))
                      true)))))))

  (method ^:public hashCode ^int [this]
    (let [^:mutable hashCode (unchecked-multiply-int 13 (if (== sort INTERNAL) OBJECT sort))]
      (when (>= sort ARRAY)
        (loop [^int i valueBegin
               ^int end valueEnd]
          (when (< i end)
            (set! hashCode
                  (unchecked-multiply-int 17 (unchecked-add-int hashCode (.charAt valueBuffer i))))
            (recur (unchecked-inc-int i) end))))
      hashCode))

  (method ^:public toString ^String [this] (.getDescriptor this)))
