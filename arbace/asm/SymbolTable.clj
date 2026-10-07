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
;; Converted from clojure/asm/SymbolTable.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final SymbolTable
  (field ^:final ^ClassWriter classWriter)

  (field ^:private ^:final ^ClassReader sourceClassReader)

  (field ^:private ^int majorVersion)

  (field ^:private ^String className)

  (field ^:private ^int entryCount)

  (field ^:private ^Entry/1 entries)

  (field ^:private ^int constantPoolCount)

  (field ^:private ^ByteVector constantPool)

  (field ^:private ^int bootstrapMethodCount)

  (field ^:private ^ByteVector bootstrapMethods)

  (field ^:private ^int typeCount)

  (field ^:private ^Entry/1 typeTable)

  (field ^:private ^int labelCount)

  (field ^:private ^LabelEntry/1 labelTable)

  (field ^:private ^LabelEntry/1 labelEntries)

  (constructor [this ^:final ^ClassWriter classWriter]
    (set! (.-classWriter this) classWriter)
    (set! (.-sourceClassReader this) nil)
    (set! (.-entries this) (new Entry/1 256))
    (set! (.-constantPoolCount this) 1)
    (set! (.-constantPool this) (ByteVector.)))

  (constructor [this ^:final ^ClassWriter classWriter ^:final ^ClassReader classReader]
    (set! (.-classWriter this) classWriter)
    (set! (.-sourceClassReader this) classReader)
    (let [inputBytes (.-classFileBuffer classReader)
          constantPoolOffset (unchecked-subtract-int (.getItem classReader 1) 1)
          constantPoolLength (unchecked-subtract-int (.-header classReader) constantPoolOffset)]
      (set! constantPoolCount (.getItemCount classReader))
      (set! constantPool (ByteVector. constantPoolLength))
      (.putByteArray constantPool inputBytes constantPoolOffset constantPoolLength)
      (set! entries (new Entry/1 (unchecked-multiply-int constantPoolCount 2)))
      (let [charBuffer (new char/1 (.getMaxStringLength classReader))
            ^:mutable hasBootstrapMethods false
            ^:mutable ^int itemIndex 1]
        (while (< itemIndex constantPoolCount)
          (let [itemOffset (.getItem classReader itemIndex)
                ^int itemTag (aget inputBytes (unchecked-subtract-int itemOffset 1))
                ^:mutable ^int nameAndTypeItemOffset 0]
            (switch itemTag
              (Symbol/CONSTANT_FIELDREF_TAG Symbol/CONSTANT_METHODREF_TAG
                                            Symbol/CONSTANT_INTERFACE_METHODREF_TAG)
                (do
                  (set!
                    nameAndTypeItemOffset
                    (.getItem classReader
                              (.readUnsignedShort classReader (unchecked-add-int itemOffset 2))))
                  (.addConstantMemberReference this
                                               itemIndex
                                               itemTag
                                               (.readClass classReader itemOffset charBuffer)
                                               (.readUTF8
                                                 classReader
                                                 nameAndTypeItemOffset
                                                 charBuffer)
                                               (.readUTF8
                                                 classReader
                                                 (unchecked-add-int nameAndTypeItemOffset 2)
                                                 charBuffer)))
              (Symbol/CONSTANT_INTEGER_TAG Symbol/CONSTANT_FLOAT_TAG)
                (.addConstantIntegerOrFloat this
                                            itemIndex
                                            itemTag
                                            (.readInt classReader itemOffset))
              Symbol/CONSTANT_NAME_AND_TYPE_TAG
                (.addConstantNameAndType this
                                         itemIndex
                                         (.readUTF8 classReader itemOffset charBuffer)
                                         (.readUTF8
                                           classReader
                                           (unchecked-add-int itemOffset 2)
                                           charBuffer))
              (Symbol/CONSTANT_LONG_TAG Symbol/CONSTANT_DOUBLE_TAG)
                (.addConstantLongOrDouble this itemIndex itemTag (.readLong classReader itemOffset))
              Symbol/CONSTANT_UTF8_TAG
                (.addConstantUtf8 this itemIndex (.readUtf classReader itemIndex charBuffer))
              Symbol/CONSTANT_METHOD_HANDLE_TAG
                (let [memberRefItemOffset (.getItem
                                            classReader
                                            (.readUnsignedShort
                                              classReader
                                              (unchecked-add-int itemOffset 1)))]
                  (set! nameAndTypeItemOffset
                        (.getItem classReader
                                  (.readUnsignedShort
                                    classReader
                                    (unchecked-add-int memberRefItemOffset 2))))
                  (.addConstantMethodHandle this
                                            itemIndex
                                            (.readByte classReader itemOffset)
                                            (.readClass classReader memberRefItemOffset charBuffer)
                                            (.readUTF8 classReader nameAndTypeItemOffset charBuffer)
                                            (.readUTF8
                                              classReader
                                              (unchecked-add-int nameAndTypeItemOffset 2)
                                              charBuffer)
                                            (== (.readByte
                                                  classReader
                                                  (unchecked-subtract-int memberRefItemOffset 1))
                                                Symbol/CONSTANT_INTERFACE_METHODREF_TAG)))
              (Symbol/CONSTANT_DYNAMIC_TAG Symbol/CONSTANT_INVOKE_DYNAMIC_TAG)
                (do
                  (set! hasBootstrapMethods true)
                  (set!
                    nameAndTypeItemOffset
                    (.getItem classReader
                              (.readUnsignedShort classReader (unchecked-add-int itemOffset 2))))
                  (.addConstantDynamicOrInvokeDynamicReference
                    this
                    itemTag
                    itemIndex
                    (.readUTF8 classReader nameAndTypeItemOffset charBuffer)
                    (.readUTF8 classReader (unchecked-add-int nameAndTypeItemOffset 2) charBuffer)
                    (.readUnsignedShort classReader itemOffset)))
              (Symbol/CONSTANT_STRING_TAG Symbol/CONSTANT_CLASS_TAG
                                          Symbol/CONSTANT_METHOD_TYPE_TAG
                                          Symbol/CONSTANT_MODULE_TAG
                                          Symbol/CONSTANT_PACKAGE_TAG)
                (.addConstantUtf8Reference this
                                           itemIndex
                                           itemTag
                                           (.readUTF8 classReader itemOffset charBuffer))
              (throw (IllegalArgumentException.)))
            (set! itemIndex
                  (unchecked-add-int itemIndex
                                     (if (or (== itemTag Symbol/CONSTANT_LONG_TAG)
                                             (== itemTag Symbol/CONSTANT_DOUBLE_TAG))
                                         2
                                         1)))))
        (when hasBootstrapMethods (.copyBootstrapMethods this classReader charBuffer)))))

  (method ^:private copyBootstrapMethods ^void [this ^:final ^ClassReader classReader
                                                ^:final ^char/1 charBuffer]
    (let [inputBytes (.-classFileBuffer classReader)
          ^:mutable currentAttributeOffset (.getFirstAttributeOffset classReader)]
      (loop [^int i (.readUnsignedShort classReader
                                        (unchecked-subtract-int currentAttributeOffset 2))]
        (when (> i 0)
          (let [attributeName (.readUTF8 classReader currentAttributeOffset charBuffer)]
            (if (.equals Constants/BOOTSTRAP_METHODS attributeName)
                (set! bootstrapMethodCount
                      (.readUnsignedShort classReader (unchecked-add-int currentAttributeOffset 6)))
                (do
                  (set! currentAttributeOffset
                        (unchecked-add-int currentAttributeOffset
                                           (unchecked-add-int
                                             6
                                             (.readInt
                                               classReader
                                               (unchecked-add-int currentAttributeOffset 2)))))
                  (recur (unchecked-dec-int i)))))))
      (when (> bootstrapMethodCount 0)
        (let [bootstrapMethodsOffset (unchecked-add-int currentAttributeOffset 8)
              bootstrapMethodsLength (unchecked-subtract-int
                                       (.readInt
                                         classReader
                                         (unchecked-add-int currentAttributeOffset 2))
                                       2)]
          (set! bootstrapMethods (ByteVector. bootstrapMethodsLength))
          (.putByteArray bootstrapMethods inputBytes bootstrapMethodsOffset bootstrapMethodsLength)
          (let [^:mutable currentOffset bootstrapMethodsOffset]
            (loop [^int i 0]
              (when (< i bootstrapMethodCount)
                (let [offset (unchecked-subtract-int currentOffset bootstrapMethodsOffset)
                      bootstrapMethodRef (.readUnsignedShort classReader currentOffset)]
                  (set! currentOffset (unchecked-add-int currentOffset 2))
                  (let [^:mutable numBootstrapArguments (.readUnsignedShort
                                                          classReader
                                                          currentOffset)]
                    (set! currentOffset (unchecked-add-int currentOffset 2))
                    (let [^:mutable hashCode (.hashCode
                                               (.readConst
                                                 classReader
                                                 bootstrapMethodRef
                                                 charBuffer))]
                      (while (> (let [old-1 numBootstrapArguments]
                                  (set! numBootstrapArguments
                                        (unchecked-dec-int numBootstrapArguments))
                                  old-1)
                                0)
                        (let [bootstrapArgument (.readUnsignedShort classReader currentOffset)]
                          (set! currentOffset (unchecked-add-int currentOffset 2))
                          (set!
                            hashCode
                            (bit-xor-int hashCode
                                         (.hashCode
                                           (.readConst classReader bootstrapArgument charBuffer))))))
                      (.add this
                            (Entry. i
                                    Symbol/BOOTSTRAP_METHOD_TAG
                                    offset
                                    (bit-and-int hashCode 0x7FFFFFFF)))
                      (recur (unchecked-inc-int i))))))))))))

  (method getSource ^ClassReader [this] sourceClassReader)

  (method getMajorVersion ^int [this] majorVersion)

  (method getClassName ^String [this] className)

  (method setMajorVersionAndClassName ^int [this ^:final ^int majorVersion ^:final ^String className]
    (set! (.-majorVersion this) majorVersion)
    (set! (.-className this) className)
    (.-index (.addConstantClass this className)))

  (method getConstantPoolCount ^int [this] constantPoolCount)

  (method getConstantPoolLength ^int [this] (.-length constantPool))

  (method putConstantPool ^void [this ^:final ^ByteVector output]
    (.putByteArray (.putShort output constantPoolCount)
                   (.-data constantPool)
                   0
                   (.-length constantPool)))

  (method computeBootstrapMethodsSize ^int [this]
    (if (some? bootstrapMethods)
        (do
          (.addConstantUtf8 this Constants/BOOTSTRAP_METHODS)
          (unchecked-add-int 8 (.-length bootstrapMethods)))
        0))

  (method putBootstrapMethods ^void [this ^:final ^ByteVector output]
    (when (some? bootstrapMethods)
      (.putByteArray
        (.putShort (.putInt (.putShort output (.addConstantUtf8 this Constants/BOOTSTRAP_METHODS))
                            (unchecked-add-int (.-length bootstrapMethods) 2))
                   bootstrapMethodCount)
        (.-data bootstrapMethods)
        0
        (.-length bootstrapMethods))))

  (method ^:private get ^Entry [this ^:final ^int hashCode]
    (aget entries (unchecked-remainder-int hashCode (alength entries))))

  (method ^:private put ^Entry [this ^:final ^Entry entry]
    (when (> entryCount (unchecked-divide-int (unchecked-multiply-int (alength entries) 3) 4))
      (let [currentCapacity (alength entries)
            newCapacity (unchecked-add-int (unchecked-multiply-int currentCapacity 2) 1)
            newEntries (new Entry/1 newCapacity)]
        (loop [^int i (unchecked-subtract-int currentCapacity 1)]
          (when (>= i 0)
            (let [^:mutable currentEntry (aget entries i)]
              (while (some? currentEntry)
                (let [newCurrentEntryIndex (unchecked-remainder-int
                                             (.-hashCode currentEntry)
                                             newCapacity)
                      nextEntry (.-next currentEntry)]
                  (set! (.-next currentEntry) (aget newEntries newCurrentEntryIndex))
                  (aset newEntries newCurrentEntryIndex currentEntry)
                  (set! currentEntry nextEntry)))
              (recur (unchecked-dec-int i)))))
        (set! entries newEntries)))
    (set! entryCount (unchecked-inc-int entryCount))
    (let [index (unchecked-remainder-int (.-hashCode entry) (alength entries))]
      (set! (.-next entry) (aget entries index))
      (aset entries index entry)))

  (method ^:private add ^void [this ^:final ^Entry entry]
    (set! entryCount (unchecked-inc-int entryCount))
    (let [index (unchecked-remainder-int (.-hashCode entry) (alength entries))]
      (set! (.-next entry) (aget entries index))
      (aset entries index entry)))

  (method addConstant ^Symbol [this ^:final value]
    (cond
      (instance? Integer value) (.addConstantInteger this (.intValue (cast Integer value)))
      (instance? Byte value) (.addConstantInteger this (.intValue (cast Byte value)))
      (instance? Character value) (.addConstantInteger this (.charValue (cast Character value)))
      (instance? Short value) (.addConstantInteger this (.intValue (cast Short value)))
      (instance? Boolean value)
        (.addConstantInteger this (if (.booleanValue (cast Boolean value)) 1 0))
      (instance? Float value) (.addConstantFloat this (.floatValue (cast Float value)))
      (instance? Long value) (.addConstantLong this (.longValue (cast Long value)))
      (instance? Double value) (.addConstantDouble this (.doubleValue (cast Double value)))
      (instance? String value) (.addConstantString this (cast String value))
      (instance? Type value)
        (let [type (cast Type value)
              typeSort (.getSort type)]
          (cond
            (== typeSort Type/OBJECT) (.addConstantClass this (.getInternalName type))
            (== typeSort Type/METHOD) (.addConstantMethodType this (.getDescriptor type))
            :else (.addConstantClass this (.getDescriptor type))))
      (instance? Handle value)
        (let [handle (cast Handle value)]
          (.addConstantMethodHandle this
                                    (.getTag handle)
                                    (.getOwner handle)
                                    (.getName handle)
                                    (.getDesc handle)
                                    (.isInterface handle)))
      (instance? ConstantDynamic value)
        (let [constantDynamic (cast ConstantDynamic value)]
          (.addConstantDynamic this
                               (.getName constantDynamic)
                               (.getDescriptor constantDynamic)
                               (.getBootstrapMethod constantDynamic)
                               (.getBootstrapMethodArgumentsUnsafe constantDynamic)))
      :else (throw (IllegalArgumentException. (java-str "value " value)))))

  (method addConstantClass ^Symbol [this ^:final ^String value]
    (.addConstantUtf8Reference this Symbol/CONSTANT_CLASS_TAG value))

  (method addConstantFieldref ^Symbol [this ^:final ^String owner ^:final ^String name
                                       ^:final ^String descriptor]
    (.addConstantMemberReference this Symbol/CONSTANT_FIELDREF_TAG owner name descriptor))

  (method addConstantMethodref ^Symbol [this ^:final ^String owner ^:final ^String name
                                        ^:final ^String descriptor ^:final ^boolean isInterface]
    (let [tag (if isInterface Symbol/CONSTANT_INTERFACE_METHODREF_TAG Symbol/CONSTANT_METHODREF_TAG)]
      (.addConstantMemberReference this tag owner name descriptor)))

  (method ^:private addConstantMemberReference ^Entry [this ^:final ^int tag ^:final ^String owner
                                                       ^:final ^String name
                                                       ^:final ^String descriptor]
    (let [hashCode (SymbolTable/hash tag owner name descriptor)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                             (.equals (.-owner entry) owner))
                        (.equals (.-name entry) name))
                   (.equals (.-value entry) descriptor))
          (return entry))
        (set! entry (.-next entry)))
      (.put122 constantPool
               tag
               (.-index (.addConstantClass this owner))
               (.addConstantNameAndType this name descriptor))
      (.put this
            (Entry. (let [old-2 constantPoolCount]
                      (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                      old-2)
                    tag
                    owner
                    name
                    descriptor
                    0
                    hashCode))))

  (method ^:private addConstantMemberReference ^void [this ^:final ^int index ^:final ^int tag
                                                      ^:final ^String owner ^:final ^String name
                                                      ^:final ^String descriptor]
    (.add this
          (Entry. index tag owner name descriptor 0 (SymbolTable/hash tag owner name descriptor))))

  (method addConstantString ^Symbol [this ^:final ^String value]
    (.addConstantUtf8Reference this Symbol/CONSTANT_STRING_TAG value))

  (method addConstantInteger ^Symbol [this ^:final ^int value]
    (.addConstantIntegerOrFloat this Symbol/CONSTANT_INTEGER_TAG value))

  (method addConstantFloat ^Symbol [this ^:final ^float value]
    (.addConstantIntegerOrFloat this Symbol/CONSTANT_FLOAT_TAG (Float/floatToRawIntBits value)))

  (method ^:private addConstantIntegerOrFloat ^Symbol [this ^:final ^int tag ^:final ^int value]
    (let [hashCode (SymbolTable/hash tag value)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                   (== (.-data entry) value))
          (return entry))
        (set! entry (.-next entry)))
      (.putInt (.putByte constantPool tag) value)
      (.put this
            (Entry. (let [old-3 constantPoolCount]
                      (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                      old-3)
                    tag
                    value
                    hashCode))))

  (method ^:private addConstantIntegerOrFloat ^void [this ^:final ^int index ^:final ^int tag
                                                     ^:final ^int value]
    (.add this (Entry. index tag value (SymbolTable/hash tag value))))

  (method addConstantLong ^Symbol [this ^:final ^long value]
    (.addConstantLongOrDouble this Symbol/CONSTANT_LONG_TAG value))

  (method addConstantDouble ^Symbol [this ^:final ^double value]
    (.addConstantLongOrDouble this Symbol/CONSTANT_DOUBLE_TAG (Double/doubleToRawLongBits value)))

  (method ^:private addConstantLongOrDouble ^Symbol [this ^:final ^int tag ^:final ^long value]
    (let [hashCode (^[int long] SymbolTable/hash tag value)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                   (== (.-data entry) value))
          (return entry))
        (set! entry (.-next entry)))
      (let [index constantPoolCount]
        (.putLong (.putByte constantPool tag) value)
        (set! constantPoolCount (unchecked-add-int constantPoolCount 2))
        (.put this (Entry. index tag value hashCode)))))

  (method ^:private addConstantLongOrDouble ^void [this ^:final ^int index ^:final ^int tag
                                                   ^:final ^long value]
    (.add this (Entry. index tag value (^[int long] SymbolTable/hash tag value))))

  (method addConstantNameAndType ^int [this ^:final ^String name ^:final ^String descriptor]
    (let [^:const tag Symbol/CONSTANT_NAME_AND_TYPE_TAG
          hashCode (SymbolTable/hash tag name descriptor)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                        (.equals (.-name entry) name))
                   (.equals (.-value entry) descriptor))
          (return (.-index entry)))
        (set! entry (.-next entry)))
      (.put122 constantPool tag (.addConstantUtf8 this name) (.addConstantUtf8 this descriptor))
      (.-index (.put this
                     (Entry. (let [old-4 constantPoolCount]
                               (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                               old-4)
                             tag
                             name
                             descriptor
                             hashCode)))))

  (method ^:private addConstantNameAndType ^void [this ^:final ^int index ^:final ^String name
                                                  ^:final ^String descriptor]
    (let [^:const tag Symbol/CONSTANT_NAME_AND_TYPE_TAG]
      (.add this (Entry. index tag name descriptor (SymbolTable/hash tag name descriptor)))))

  (method addConstantUtf8 ^int [this ^:final ^String value]
    (let [hashCode (SymbolTable/hash Symbol/CONSTANT_UTF8_TAG value)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) Symbol/CONSTANT_UTF8_TAG)
                        (== (.-hashCode entry) hashCode))
                   (.equals (.-value entry) value))
          (return (.-index entry)))
        (set! entry (.-next entry)))
      (.putUTF8 (.putByte constantPool Symbol/CONSTANT_UTF8_TAG) value)
      (.-index (.put this
                     (Entry. (let [old-5 constantPoolCount]
                               (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                               old-5)
                             Symbol/CONSTANT_UTF8_TAG
                             value
                             hashCode)))))

  (method ^:private addConstantUtf8 ^void [this ^:final ^int index ^:final ^String value]
    (.add this
          (Entry. index
                  Symbol/CONSTANT_UTF8_TAG
                  value
                  (SymbolTable/hash Symbol/CONSTANT_UTF8_TAG value))))

  (method addConstantMethodHandle ^Symbol [this ^:final ^int referenceKind ^:final ^String owner
                                           ^:final ^String name ^:final ^String descriptor
                                           ^:final ^boolean isInterface]
    (let [^:const tag Symbol/CONSTANT_METHOD_HANDLE_TAG
          data (SymbolTable/getConstantMethodHandleSymbolData referenceKind isInterface)
          hashCode (SymbolTable/hash tag owner name descriptor data)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                                  (== (.-data entry) data))
                             (.equals (.-owner entry) owner))
                        (.equals (.-name entry) name))
                   (.equals (.-value entry) descriptor))
          (return entry))
        (set! entry (.-next entry)))
      (if (<= referenceKind Opcodes/H_PUTSTATIC)
          (.put112 constantPool
                   tag
                   referenceKind
                   (.-index (.addConstantFieldref this owner name descriptor)))
          (.put112 constantPool
                   tag
                   referenceKind
                   (.-index (.addConstantMethodref this owner name descriptor isInterface))))
      (.put this
            (Entry. (let [old-6 constantPoolCount]
                      (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                      old-6)
                    tag
                    owner
                    name
                    descriptor
                    data
                    hashCode))))

  (method ^:private addConstantMethodHandle ^void [this ^:final ^int index
                                                   ^:final ^int referenceKind ^:final ^String owner
                                                   ^:final ^String name ^:final ^String descriptor
                                                   ^:final ^boolean isInterface]
    (let [^:const tag Symbol/CONSTANT_METHOD_HANDLE_TAG
          data (SymbolTable/getConstantMethodHandleSymbolData referenceKind isInterface)
          hashCode (SymbolTable/hash tag owner name descriptor data)]
      (.add this (Entry. index tag owner name descriptor data hashCode))))

  (method ^:private ^:static getConstantMethodHandleSymbolData ^int [^:final ^int referenceKind
                                                                     ^:final ^boolean isInterface]
    (if (and (> referenceKind Opcodes/H_PUTSTATIC) isInterface)
        (bit-shift-left-int referenceKind 8)
        referenceKind))

  (method addConstantMethodType ^Symbol [this ^:final ^String methodDescriptor]
    (.addConstantUtf8Reference this Symbol/CONSTANT_METHOD_TYPE_TAG methodDescriptor))

  (method addConstantDynamic ^Symbol [this ^:final ^String name ^:final ^String descriptor
                                      ^:final ^Handle bootstrapMethodHandle &
                                      ^:final ^Object/1 bootstrapMethodArguments]
    (let [bootstrapMethod (.addBootstrapMethod this bootstrapMethodHandle bootstrapMethodArguments)]
      (.addConstantDynamicOrInvokeDynamicReference
        this
        Symbol/CONSTANT_DYNAMIC_TAG
        name
        descriptor
        (.-index bootstrapMethod))))

  (method addConstantInvokeDynamic ^Symbol [this ^:final ^String name ^:final ^String descriptor
                                            ^:final ^Handle bootstrapMethodHandle &
                                            ^:final ^Object/1 bootstrapMethodArguments]
    (let [bootstrapMethod (.addBootstrapMethod this bootstrapMethodHandle bootstrapMethodArguments)]
      (.addConstantDynamicOrInvokeDynamicReference
        this
        Symbol/CONSTANT_INVOKE_DYNAMIC_TAG
        name
        descriptor
        (.-index bootstrapMethod))))

  (method ^:private addConstantDynamicOrInvokeDynamicReference ^Symbol [this ^:final ^int tag
                                                                        ^:final ^String name
                                                                        ^:final ^String descriptor
                                                                        ^:final ^int bootstrapMethodIndex]
    (let [hashCode (SymbolTable/hash tag name descriptor bootstrapMethodIndex)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                             (== (.-data entry) bootstrapMethodIndex))
                        (.equals (.-name entry) name))
                   (.equals (.-value entry) descriptor))
          (return entry))
        (set! entry (.-next entry)))
      (.put122 constantPool tag bootstrapMethodIndex (.addConstantNameAndType this name descriptor))
      (.put this
            (Entry. (let [old-7 constantPoolCount]
                      (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                      old-7)
                    tag
                    nil
                    name
                    descriptor
                    bootstrapMethodIndex
                    hashCode))))

  (method ^:private addConstantDynamicOrInvokeDynamicReference ^void [this ^:final ^int tag
                                                                      ^:final ^int index
                                                                      ^:final ^String name
                                                                      ^:final ^String descriptor
                                                                      ^:final ^int bootstrapMethodIndex]
    (let [hashCode (SymbolTable/hash tag name descriptor bootstrapMethodIndex)]
      (.add this (Entry. index tag nil name descriptor bootstrapMethodIndex hashCode))))

  (method addConstantModule ^Symbol [this ^:final ^String moduleName]
    (.addConstantUtf8Reference this Symbol/CONSTANT_MODULE_TAG moduleName))

  (method addConstantPackage ^Symbol [this ^:final ^String packageName]
    (.addConstantUtf8Reference this Symbol/CONSTANT_PACKAGE_TAG packageName))

  (method ^:private addConstantUtf8Reference ^Symbol [this ^:final ^int tag ^:final ^String value]
    (let [hashCode (SymbolTable/hash tag value)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) tag) (== (.-hashCode entry) hashCode))
                   (.equals (.-value entry) value))
          (return entry))
        (set! entry (.-next entry)))
      (.put12 constantPool tag (.addConstantUtf8 this value))
      (.put this
            (Entry. (let [old-8 constantPoolCount]
                      (set! constantPoolCount (unchecked-inc-int constantPoolCount))
                      old-8)
                    tag
                    value
                    hashCode))))

  (method ^:private addConstantUtf8Reference ^void [this ^:final ^int index ^:final ^int tag
                                                    ^:final ^String value]
    (.add this (Entry. index tag value (SymbolTable/hash tag value))))

  (method addBootstrapMethod ^Symbol [this ^:final ^Handle bootstrapMethodHandle &
                                      ^:final ^Object/1 bootstrapMethodArguments]
    (let [^:mutable bootstrapMethodsAttribute bootstrapMethods]
      (when (nil? bootstrapMethodsAttribute)
        (set! bootstrapMethodsAttribute (set! bootstrapMethods (ByteVector.))))
      (let [numBootstrapArguments (alength bootstrapMethodArguments)
            bootstrapMethodArgumentIndexes (new int/1 numBootstrapArguments)]
        (loop [^int i 0]
          (when (< i numBootstrapArguments)
            (aset bootstrapMethodArgumentIndexes
                  i
                  (.-index (.addConstant this (aget bootstrapMethodArguments i))))
            (recur (unchecked-inc-int i))))
        (let [bootstrapMethodOffset (.-length bootstrapMethodsAttribute)]
          (.putShort bootstrapMethodsAttribute
                     (.-index (.addConstantMethodHandle
                                this
                                (.getTag bootstrapMethodHandle)
                                (.getOwner bootstrapMethodHandle)
                                (.getName bootstrapMethodHandle)
                                (.getDesc bootstrapMethodHandle)
                                (.isInterface bootstrapMethodHandle))))
          (.putShort bootstrapMethodsAttribute numBootstrapArguments)
          (loop [^int i 0]
            (when (< i numBootstrapArguments)
              (.putShort bootstrapMethodsAttribute (aget bootstrapMethodArgumentIndexes i))
              (recur (unchecked-inc-int i))))
          (let [bootstrapMethodlength (unchecked-subtract-int
                                        (.-length bootstrapMethodsAttribute)
                                        bootstrapMethodOffset)
                ^:mutable hashCode (.hashCode bootstrapMethodHandle)]
            (for-each [bootstrapMethodArgument bootstrapMethodArguments]
              (set! hashCode (bit-xor-int hashCode (.hashCode bootstrapMethodArgument))))
            (set! hashCode (bit-and-int hashCode 0x7FFFFFFF))
            (.addBootstrapMethod this bootstrapMethodOffset bootstrapMethodlength hashCode))))))

  (method ^:private addBootstrapMethod ^Symbol [this ^:final ^int offset ^:final ^int length
                                                ^:final ^int hashCode]
    (let [bootstrapMethodsData (.-data bootstrapMethods)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (== (.-tag entry) Symbol/BOOTSTRAP_METHOD_TAG) (== (.-hashCode entry) hashCode))
          (let [otherOffset (unchecked-int (.-data entry))
                ^:mutable isSameBootstrapMethod true]
            (loop [^int i 0]
              (if (< i length)
                  (if (not (== (aget bootstrapMethodsData (unchecked-add-int offset i))
                               (aget bootstrapMethodsData (unchecked-add-int otherOffset i))))
                      (set! isSameBootstrapMethod false)
                      (recur (unchecked-inc-int i)))
                  nil))
            (when isSameBootstrapMethod (set! (.-length bootstrapMethods) offset) (return entry))))
        (set! entry (.-next entry)))
      (.put this
            (Entry. (let [old-9 bootstrapMethodCount]
                      (set! bootstrapMethodCount (unchecked-inc-int bootstrapMethodCount))
                      old-9)
                    Symbol/BOOTSTRAP_METHOD_TAG
                    offset
                    hashCode))))

  (method getType ^Symbol [this ^:final ^int typeIndex]
    (aget typeTable typeIndex))

  (method getForwardUninitializedLabel ^Label [this ^:final ^int typeIndex]
    (.-label (aget labelTable (unchecked-int (.-data (aget typeTable typeIndex))))))

  (method addType ^int [this ^:final ^String value]
    (let [hashCode (SymbolTable/hash Symbol/TYPE_TAG value)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) Symbol/TYPE_TAG) (== (.-hashCode entry) hashCode))
                   (.equals (.-value entry) value))
          (return (.-index entry)))
        (set! entry (.-next entry)))
      (.addTypeInternal this (Entry. typeCount Symbol/TYPE_TAG value hashCode))))

  (method addUninitializedType ^int [this ^:final ^String value ^:final ^int bytecodeOffset]
    (let [hashCode (SymbolTable/hash Symbol/UNINITIALIZED_TYPE_TAG value bytecodeOffset)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (== (.-tag entry) Symbol/UNINITIALIZED_TYPE_TAG)
                             (== (.-hashCode entry) hashCode))
                        (== (.-data entry) bytecodeOffset))
                   (.equals (.-value entry) value))
          (return (.-index entry)))
        (set! entry (.-next entry)))
      (.addTypeInternal this
                        (Entry. typeCount
                                Symbol/UNINITIALIZED_TYPE_TAG
                                value
                                bytecodeOffset
                                hashCode))))

  (method addForwardUninitializedType ^int [this ^:final ^String value ^:final ^Label label]
    (let [labelIndex (.-index (.getOrAddLabelEntry this label))
          hashCode (SymbolTable/hash Symbol/FORWARD_UNINITIALIZED_TYPE_TAG value labelIndex)
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (and (== (.-tag entry) Symbol/FORWARD_UNINITIALIZED_TYPE_TAG)
                             (== (.-hashCode entry) hashCode))
                        (== (.-data entry) labelIndex))
                   (.equals (.-value entry) value))
          (return (.-index entry)))
        (set! entry (.-next entry)))
      (.addTypeInternal this
                        (Entry. typeCount
                                Symbol/FORWARD_UNINITIALIZED_TYPE_TAG
                                value
                                labelIndex
                                hashCode))))

  (method addMergedType ^int [this ^:final ^int typeTableIndex1 ^:final ^int typeTableIndex2]
    (let [data (if (< typeTableIndex1 typeTableIndex2)
                   (bit-or typeTableIndex1 (bit-shift-left (long typeTableIndex2) 32))
                   (bit-or typeTableIndex2 (bit-shift-left (long typeTableIndex1) 32)))
          hashCode (SymbolTable/hash Symbol/MERGED_TYPE_TAG
                                     (unchecked-add-int typeTableIndex1 typeTableIndex2))
          ^:mutable entry (.get this hashCode)]
      (while (some? entry)
        (when (and (and (== (.-tag entry) Symbol/MERGED_TYPE_TAG) (== (.-hashCode entry) hashCode))
                   (== (.-data entry) data))
          (return (.-info entry)))
        (set! entry (.-next entry)))
      (let [type1 (.-value (aget typeTable typeTableIndex1))
            type2 (.-value (aget typeTable typeTableIndex2))
            commonSuperTypeIndex (.addType this (.getCommonSuperClass classWriter type1 type2))]
        (set! (.-info (.put this (Entry. typeCount Symbol/MERGED_TYPE_TAG data hashCode)))
              commonSuperTypeIndex)
        commonSuperTypeIndex)))

  (method ^:private addTypeInternal ^int [this ^:final ^Entry entry]
    (when (nil? typeTable) (set! typeTable (new Entry/1 16)))
    (when (== typeCount (alength typeTable))
      (let [newTypeTable (new Entry/1 (unchecked-multiply-int 2 (alength typeTable)))]
        (System/arraycopy typeTable 0 newTypeTable 0 (alength typeTable))
        (set! typeTable newTypeTable)))
    (aset typeTable
          (let [old-10 typeCount] (set! typeCount (unchecked-inc-int typeCount)) old-10)
          entry)
    (.-index (.put this entry)))

  (method ^:private getOrAddLabelEntry ^LabelEntry [this ^:final ^Label label]
    (when (nil? labelEntries)
      (set! labelEntries (new LabelEntry/1 16))
      (set! labelTable (new LabelEntry/1 16)))
    (let [hashCode (System/identityHashCode label)
          ^:mutable labelEntry (aget labelEntries
                                     (unchecked-remainder-int hashCode (alength labelEntries)))]
      (while (and (some? labelEntry) (not (identical? (.-label labelEntry) label)))
        (set! labelEntry (.-next labelEntry)))
      (if (some? labelEntry)
          labelEntry
          (do
            (when (> labelCount
                     (unchecked-divide-int (unchecked-multiply-int (alength labelEntries) 3) 4))
              (let [currentCapacity (alength labelEntries)
                    newCapacity (unchecked-add-int (unchecked-multiply-int currentCapacity 2) 1)
                    newLabelEntries (new LabelEntry/1 newCapacity)]
                (loop [^int i (unchecked-subtract-int currentCapacity 1)]
                  (when (>= i 0)
                    (let [^:mutable currentEntry (aget labelEntries i)]
                      (while (some? currentEntry)
                        (let [newCurrentEntryIndex (unchecked-remainder-int
                                                     (System/identityHashCode
                                                       (.-label currentEntry))
                                                     newCapacity)
                              nextEntry (.-next currentEntry)]
                          (set! (.-next currentEntry) (aget newLabelEntries newCurrentEntryIndex))
                          (aset newLabelEntries newCurrentEntryIndex currentEntry)
                          (set! currentEntry nextEntry)))
                      (recur (unchecked-dec-int i)))))
                (set! labelEntries newLabelEntries)))
            (when (== labelCount (alength labelTable))
              (let [newLabelTable (new LabelEntry/1 (unchecked-multiply-int 2 (alength labelTable)))]
                (System/arraycopy labelTable 0 newLabelTable 0 (alength labelTable))
                (set! labelTable newLabelTable)))
            (set! labelEntry (LabelEntry. labelCount label))
            (let [index (unchecked-remainder-int hashCode (alength labelEntries))]
              (set! (.-next labelEntry) (aget labelEntries index))
              (aset labelEntries index labelEntry)
              (aset labelTable
                    (let [old-11 labelCount]
                      (set! labelCount (unchecked-inc-int labelCount))
                      old-11)
                    labelEntry)
              labelEntry)))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^int value]
    (bit-and-int 0x7FFFFFFF (unchecked-add-int tag value)))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^long value]
    (bit-and-int 0x7FFFFFFF
                 (unchecked-add-int (unchecked-add-int tag (unchecked-int value))
                                    (unchecked-int (unsigned-bit-shift-right value 32)))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value]
    (bit-and-int 0x7FFFFFFF (unchecked-add-int tag (.hashCode value))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value1 ^:final ^int value2]
    (bit-and-int 0x7FFFFFFF (unchecked-add-int (unchecked-add-int tag (.hashCode value1)) value2)))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value1
                                        ^:final ^String value2]
    (bit-and-int 0x7FFFFFFF
                 (unchecked-add-int tag
                                    (unchecked-multiply-int (.hashCode value1) (.hashCode value2)))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value1
                                        ^:final ^String value2 ^:final ^int value3]
    (bit-and-int 0x7FFFFFFF
                 (unchecked-add-int tag
                                    (unchecked-multiply-int
                                      (unchecked-multiply-int (.hashCode value1) (.hashCode value2))
                                      (unchecked-add-int value3 1)))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value1
                                        ^:final ^String value2 ^:final ^String value3]
    (bit-and-int 0x7FFFFFFF
                 (unchecked-add-int tag
                                    (unchecked-multiply-int
                                      (unchecked-multiply-int (.hashCode value1) (.hashCode value2))
                                      (.hashCode value3)))))

  (method ^:private ^:static hash ^int [^:final ^int tag ^:final ^String value1
                                        ^:final ^String value2 ^:final ^String value3
                                        ^:final ^int value4]
    (bit-and-int 0x7FFFFFFF
                 (unchecked-add-int tag
                                    (unchecked-multiply-int
                                      (unchecked-multiply-int
                                        (unchecked-multiply-int
                                          (.hashCode value1)
                                          (.hashCode value2))
                                        (.hashCode value3))
                                      value4))))

  (defclass ^:private ^:static ^:final Entry
    :extends Symbol

    (field ^:final ^int hashCode)

    (field ^Entry next)

    (constructor [this ^:final ^int index ^:final ^int tag ^:final ^String owner
                  ^:final ^String name ^:final ^String value ^:final ^long data
                  ^:final ^int hashCode]
      (super. index tag owner name value data)
      (set! (.-hashCode this) hashCode))

    (constructor [this ^:final ^int index ^:final ^int tag ^:final ^String value
                  ^:final ^int hashCode]
      (super. index tag nil nil value 0)
      (set! (.-hashCode this) hashCode))

    (constructor [this ^:final ^int index ^:final ^int tag ^:final ^String value ^:final ^long data
                  ^:final ^int hashCode]
      (super. index tag nil nil value data)
      (set! (.-hashCode this) hashCode))

    (constructor [this ^:final ^int index ^:final ^int tag ^:final ^String name
                  ^:final ^String value ^:final ^int hashCode]
      (super. index tag nil name value 0)
      (set! (.-hashCode this) hashCode))

    (constructor [this ^:final ^int index ^:final ^int tag ^:final ^long data ^:final ^int hashCode]
      (super. index tag nil nil nil data)
      (set! (.-hashCode this) hashCode)))

  (defclass ^:private ^:static ^:final LabelEntry
    (field ^:final ^int index)

    (field ^:final ^Label label)

    (field ^LabelEntry next)

    (constructor [this ^:final ^int index ^:final ^Label label]
      (set! (.-index this) index)
      (set! (.-label this) label))))
