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
;; Converted from clojure/asm/ClassWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ClassWriter
  :extends ClassVisitor

  (field ^:public ^:static ^:final ^int COMPUTE_MAXS 1)

  (field ^:public ^:static ^:final ^int COMPUTE_FRAMES 2)

  (field ^:static ^:final ^int DEFAULT_MAX_MEMORY_LIMIT
    (unchecked-multiply-int (unchecked-multiply-int 10 1024) 1024))

  (field ^:static ^:final ^long DEFAULT_MAX_OPERATIONS_LIMIT 100000000)

  (field ^:private ^:final ^int flags)

  (field ^:private ^int version)

  (field ^:private ^:final ^SymbolTable symbolTable)

  (field ^:private ^int accessFlags)

  (field ^:private ^int thisClass)

  (field ^:private ^int superClass)

  (field ^:private ^int interfaceCount)

  (field ^:private ^int/1 interfaces)

  (field ^:private ^FieldWriter firstField)

  (field ^:private ^FieldWriter lastField)

  (field ^:private ^MethodWriter firstMethod)

  (field ^:private ^MethodWriter lastMethod)

  (field ^:private ^int numberOfInnerClasses)

  (field ^:private ^ByteVector innerClasses)

  (field ^:private ^int enclosingClassIndex)

  (field ^:private ^int enclosingMethodIndex)

  (field ^:private ^int signatureIndex)

  (field ^:private ^int sourceFileIndex)

  (field ^:private ^ByteVector debugExtension)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleTypeAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleTypeAnnotation)

  (field ^:private ^ModuleWriter moduleWriter)

  (field ^:private ^int nestHostClassIndex)

  (field ^:private ^int numberOfNestMemberClasses)

  (field ^:private ^ByteVector nestMemberClasses)

  (field ^:private ^int numberOfPermittedSubclasses)

  (field ^:private ^ByteVector permittedSubclasses)

  (field ^:private ^RecordComponentWriter firstRecordComponent)

  (field ^:private ^RecordComponentWriter lastRecordComponent)

  (field ^:private ^Attribute firstAttribute)

  (field ^:private ^int compute)

  (field ^:private ^ComputeLimits limits
    (ComputeLimits. DEFAULT_MAX_MEMORY_LIMIT DEFAULT_MAX_OPERATIONS_LIMIT))

  (constructor ^:public [this ^:final ^int flags] (this. nil flags))

  (constructor ^:public [this ^:final ^ClassReader classReader ^:final ^int flags]
    (super. Opcodes/ASM9)
    (set! (.-flags this) flags)
    (set! symbolTable (if (nil? classReader) (SymbolTable. this) (SymbolTable. this classReader)))
    (.setFlags this flags))

  (method ^:public hasFlags ^boolean [this ^:final ^int flags]
    (== (bit-and-int (.-flags this) flags) flags))

  (method ^:public setComputeLimits ^void [this ^:final ^int maxBytes ^:final ^long maxOperations]
    (set! limits (ComputeLimits. maxBytes maxOperations)))

  (method ^:public ^:final visit ^void [this ^:final ^int version ^:final ^int access
                                        ^:final ^String name ^:final ^String signature
                                        ^:final ^String superName ^:final ^String/1 interfaces]
    (set! (.-version this) version)
    (set! (.-accessFlags this) access)
    (set! (.-thisClass this)
          (.setMajorVersionAndClassName symbolTable (bit-and-int version 0xFFFF) name))
    (when (some? signature) (set! (.-signatureIndex this) (.addConstantUtf8 symbolTable signature)))
    (set! (.-superClass this)
          (if (nil? superName) 0 (.-index (.addConstantClass symbolTable superName))))
    (when (and (some? interfaces) (> (alength interfaces) 0))
      (set! interfaceCount (alength interfaces))
      (set! (.-interfaces this) (new int/1 interfaceCount))
      (loop [^int i 0]
        (when (< i interfaceCount)
          (aset (.-interfaces this) i (.-index (.addConstantClass symbolTable (aget interfaces i))))
          (recur (unchecked-inc-int i)))))
    (when (and (== compute MethodWriter/COMPUTE_MAX_STACK_AND_LOCAL)
               (>= (bit-and-int version 0xFFFF) Opcodes/V1_7))
      (set! compute MethodWriter/COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES)))

  (method ^:public ^:final visitSource ^void [this ^:final ^String file ^:final ^String debug]
    (when (some? file) (set! sourceFileIndex (.addConstantUtf8 symbolTable file)))
    (when (some? debug) (set! debugExtension (.encodeUtf8 (ByteVector.) debug 0 Integer/MAX_VALUE))))

  (method ^:public ^:final visitModule ^ModuleVisitor [this ^:final ^String name ^:final ^int access
                                                       ^:final ^String version]
    (set! moduleWriter
          (ModuleWriter. symbolTable
                         (.-index (.addConstantModule symbolTable name))
                         access
                         (if (nil? version) 0 (.addConstantUtf8 symbolTable version)))))

  (method ^:public ^:final visitNestHost ^void [this ^:final ^String nestHost]
    (set! nestHostClassIndex (.-index (.addConstantClass symbolTable nestHost))))

  (method ^:public ^:final visitOuterClass ^void [this ^:final ^String owner ^:final ^String name
                                                  ^:final ^String descriptor]
    (set! enclosingClassIndex (.-index (.addConstantClass symbolTable owner)))
    (when (and (some? name) (some? descriptor))
      (set! enclosingMethodIndex (.addConstantNameAndType symbolTable name descriptor))))

  (method ^:public ^:final visitAnnotation ^AnnotationVisitor [this ^:final ^String descriptor
                                                               ^:final ^boolean visible]
    (if visible
        (set! lastRuntimeVisibleAnnotation
              (AnnotationWriter/create symbolTable descriptor lastRuntimeVisibleAnnotation))
        (set! lastRuntimeInvisibleAnnotation
              (AnnotationWriter/create symbolTable descriptor lastRuntimeInvisibleAnnotation))))

  (method ^:public ^:final visitTypeAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
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

  (method ^:public ^:final visitAttribute ^void [this ^:final ^Attribute attribute]
    (set! (.-nextAttribute attribute) firstAttribute)
    (set! firstAttribute attribute))

  (method ^:public ^:final visitNestMember ^void [this ^:final ^String nestMember]
    (when (nil? nestMemberClasses) (set! nestMemberClasses (ByteVector.)))
    (set! numberOfNestMemberClasses (unchecked-inc-int numberOfNestMemberClasses))
    (.putShort nestMemberClasses (.-index (.addConstantClass symbolTable nestMember))))

  (method ^:public ^:final visitPermittedSubclass ^void [this ^:final ^String permittedSubclass]
    (when (nil? permittedSubclasses) (set! permittedSubclasses (ByteVector.)))
    (set! numberOfPermittedSubclasses (unchecked-inc-int numberOfPermittedSubclasses))
    (.putShort permittedSubclasses (.-index (.addConstantClass symbolTable permittedSubclass))))

  (method ^:public ^:final visitInnerClass ^void [this ^:final ^String name
                                                  ^:final ^String outerName
                                                  ^:final ^String innerName ^:final ^int access]
    (when (nil? innerClasses) (set! innerClasses (ByteVector.)))
    (let [nameSymbol (.addConstantClass symbolTable name)]
      (when (== (.-info nameSymbol) 0)
        (set! numberOfInnerClasses (unchecked-inc-int numberOfInnerClasses))
        (.putShort innerClasses (.-index nameSymbol))
        (.putShort innerClasses
                   (if (nil? outerName) 0 (.-index (.addConstantClass symbolTable outerName))))
        (.putShort innerClasses (if (nil? innerName) 0 (.addConstantUtf8 symbolTable innerName)))
        (.putShort innerClasses access)
        (set! (.-info nameSymbol) numberOfInnerClasses))))

  (method ^:public ^:final visitRecordComponent ^RecordComponentVisitor [this ^:final ^String name
                                                                         ^:final ^String descriptor
                                                                         ^:final ^String signature]
    (let [recordComponentWriter (RecordComponentWriter. symbolTable name descriptor signature)]
      (if (nil? firstRecordComponent)
          (set! firstRecordComponent recordComponentWriter)
          (set! (.-delegate lastRecordComponent) recordComponentWriter))
      (set! lastRecordComponent recordComponentWriter)))

  (method ^:public ^:final visitField ^FieldVisitor [this ^:final ^int access ^:final ^String name
                                                     ^:final ^String descriptor
                                                     ^:final ^String signature ^:final value]
    (let [fieldWriter (FieldWriter. symbolTable access name descriptor signature value)]
      (if (nil? firstField) (set! firstField fieldWriter) (set! (.-fv lastField) fieldWriter))
      (set! lastField fieldWriter)))

  (method ^:public ^:final visitMethod ^MethodVisitor [this ^:final ^int access ^:final ^String name
                                                       ^:final ^String descriptor
                                                       ^:final ^String signature
                                                       ^:final ^String/1 exceptions]
    (let [methodWriter (MethodWriter. symbolTable
                                      access
                                      name
                                      descriptor
                                      signature
                                      exceptions
                                      compute
                                      (ComputeLimits. limits))]
      (if (nil? firstMethod) (set! firstMethod methodWriter) (set! (.-mv lastMethod) methodWriter))
      (set! lastMethod methodWriter)))

  (method ^:public ^:final visitEnd ^void [this])

  (method ^:public toByteArray ^byte/1 [this]
    (loop [^int pass 0]
      (when (< pass 2)
        (let [^:mutable size (unchecked-add-int 24 (unchecked-multiply-int 2 interfaceCount))
              ^:mutable ^int fieldsCount 0
              ^:mutable fieldWriter firstField]
          (while (some? fieldWriter)
            (set! fieldsCount (unchecked-inc-int fieldsCount))
            (set! size (unchecked-add-int size (.computeFieldInfoSize fieldWriter)))
            (set! fieldWriter (cast FieldWriter (.-fv fieldWriter))))
          (let [^:mutable ^int methodsCount 0
                ^:mutable methodWriter firstMethod]
            (while (some? methodWriter)
              (set! methodsCount (unchecked-inc-int methodsCount))
              (set! size (unchecked-add-int size (.computeMethodInfoSize methodWriter)))
              (set! methodWriter (cast MethodWriter (.-mv methodWriter))))
            (let [^:mutable ^int attributesCount 0]
              (when (some? innerClasses)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size (unchecked-add-int 8 (.-length innerClasses))))
                (.addConstantUtf8 symbolTable Constants/INNER_CLASSES))
              (when-not (== enclosingClassIndex 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 10))
                (.addConstantUtf8 symbolTable Constants/ENCLOSING_METHOD))
              (when (and (not (== (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC) 0))
                         (< (bit-and-int version 0xFFFF) Opcodes/V1_5))
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 6))
                (.addConstantUtf8 symbolTable Constants/SYNTHETIC))
              (when-not (== signatureIndex 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 8))
                (.addConstantUtf8 symbolTable Constants/SIGNATURE))
              (when-not (== sourceFileIndex 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 8))
                (.addConstantUtf8 symbolTable Constants/SOURCE_FILE))
              (when (some? debugExtension)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size (unchecked-add-int 6 (.-length debugExtension))))
                (.addConstantUtf8 symbolTable Constants/SOURCE_DEBUG_EXTENSION))
              (when-not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 6))
                (.addConstantUtf8 symbolTable Constants/DEPRECATED))
              (when (some? lastRuntimeVisibleAnnotation)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size
                                         (.computeAnnotationsSize
                                           lastRuntimeVisibleAnnotation
                                           Constants/RUNTIME_VISIBLE_ANNOTATIONS))))
              (when (some? lastRuntimeInvisibleAnnotation)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size
                                         (.computeAnnotationsSize
                                           lastRuntimeInvisibleAnnotation
                                           Constants/RUNTIME_INVISIBLE_ANNOTATIONS))))
              (when (some? lastRuntimeVisibleTypeAnnotation)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size
                                         (.computeAnnotationsSize
                                           lastRuntimeVisibleTypeAnnotation
                                           Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS))))
              (when (some? lastRuntimeInvisibleTypeAnnotation)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size
                                         (.computeAnnotationsSize
                                           lastRuntimeInvisibleTypeAnnotation
                                           Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS))))
              (when (> (.computeBootstrapMethodsSize symbolTable) 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size (.computeBootstrapMethodsSize symbolTable))))
              (when (some? moduleWriter)
                (set! attributesCount
                      (unchecked-add-int attributesCount (.getAttributeCount moduleWriter)))
                (set! size (unchecked-add-int size (.computeAttributesSize moduleWriter))))
              (when-not (== nestHostClassIndex 0)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size (unchecked-add-int size 8))
                (.addConstantUtf8 symbolTable Constants/NEST_HOST))
              (when (some? nestMemberClasses)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size (unchecked-add-int 8 (.-length nestMemberClasses))))
                (.addConstantUtf8 symbolTable Constants/NEST_MEMBERS))
              (when (some? permittedSubclasses)
                (set! attributesCount (unchecked-inc-int attributesCount))
                (set! size
                      (unchecked-add-int size (unchecked-add-int 8 (.-length permittedSubclasses))))
                (.addConstantUtf8 symbolTable Constants/PERMITTED_SUBCLASSES))
              (let [^:mutable ^int recordComponentCount 0
                    ^:mutable ^int recordSize 0]
                (when (or (not (== (bit-and-int accessFlags Opcodes/ACC_RECORD) 0))
                          (some? firstRecordComponent))
                  (let [^:mutable recordComponentWriter firstRecordComponent]
                    (while (some? recordComponentWriter)
                      (set! recordComponentCount (unchecked-inc-int recordComponentCount))
                      (set! recordSize
                            (unchecked-add-int recordSize
                                               (.computeRecordComponentInfoSize
                                                 recordComponentWriter)))
                      (set! recordComponentWriter
                            (cast RecordComponentWriter (.-delegate recordComponentWriter))))
                    (set! attributesCount (unchecked-inc-int attributesCount))
                    (set! size (unchecked-add-int size (unchecked-add-int 8 recordSize)))
                    (.addConstantUtf8 symbolTable Constants/RECORD)))
                (when (some? firstAttribute)
                  (set! attributesCount
                        (unchecked-add-int attributesCount (.getAttributeCount firstAttribute)))
                  (set!
                    size
                    (unchecked-add-int size (.computeAttributesSize firstAttribute symbolTable))))
                (set! size (unchecked-add-int size (.getConstantPoolLength symbolTable)))
                (let [constantPoolCount (.getConstantPoolCount symbolTable)]
                  (when (> constantPoolCount 0xFFFF)
                    (throw (ClassTooLargeException. (.getClassName symbolTable) constantPoolCount)))
                  (let [result (ByteVector. size)]
                    (.putInt (.putInt result (unchecked-int 0xCAFEBABE)) version)
                    (.putConstantPool symbolTable result)
                    (let [mask (if (< (bit-and-int version 0xFFFF) Opcodes/V1_5)
                                   Opcodes/ACC_SYNTHETIC
                                   0)]
                      (.putShort (.putShort (.putShort
                                              result
                                              (bit-and-int accessFlags (bit-not-int mask)))
                                            thisClass)
                                 superClass)
                      (.putShort result interfaceCount)
                      (loop [^int i 0]
                        (when (< i interfaceCount)
                          (.putShort result (aget interfaces i))
                          (recur (unchecked-inc-int i))))
                      (.putShort result fieldsCount)
                      (set! fieldWriter firstField)
                      (while (some? fieldWriter)
                        (.putFieldInfo fieldWriter result)
                        (set! fieldWriter (cast FieldWriter (.-fv fieldWriter))))
                      (.putShort result methodsCount)
                      (let [^:mutable hasFrames false
                            ^:mutable hasAsmInstructions false]
                        (set! methodWriter firstMethod)
                        (while (some? methodWriter)
                          (set! hasFrames
                                (let [a-1 hasFrames b-2 (.hasFrames methodWriter)] (or a-1 b-2)))
                          (when (.hasAsmInstructions methodWriter)
                            (.completeAsmInstructions methodWriter)
                            (set! hasAsmInstructions true))
                          (.putMethodInfo methodWriter result)
                          (set! methodWriter (cast MethodWriter (.-mv methodWriter))))
                        (.putShort result attributesCount)
                        (when (some? innerClasses)
                          (.putByteArray
                            (.putShort (.putInt (.putShort
                                                  result
                                                  (.addConstantUtf8
                                                    symbolTable
                                                    Constants/INNER_CLASSES))
                                                (unchecked-add-int (.-length innerClasses) 2))
                                       numberOfInnerClasses)
                            (.-data innerClasses)
                            0
                            (.-length innerClasses)))
                        (when-not (== enclosingClassIndex 0)
                          (.putShort (.putShort (.putInt
                                                  (.putShort
                                                    result
                                                    (.addConstantUtf8
                                                      symbolTable
                                                      Constants/ENCLOSING_METHOD))
                                                  4)
                                                enclosingClassIndex)
                                     enclosingMethodIndex))
                        (when (and (not (== (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC) 0))
                                   (< (bit-and-int version 0xFFFF) Opcodes/V1_5))
                          (.putInt (.putShort result
                                              (.addConstantUtf8 symbolTable Constants/SYNTHETIC))
                                   0))
                        (when-not (== signatureIndex 0)
                          (.putShort (.putInt (.putShort
                                                result
                                                (.addConstantUtf8 symbolTable Constants/SIGNATURE))
                                              2)
                                     signatureIndex))
                        (when-not (== sourceFileIndex 0)
                          (.putShort (.putInt
                                       (.putShort
                                         result
                                         (.addConstantUtf8 symbolTable Constants/SOURCE_FILE))
                                       2)
                                     sourceFileIndex))
                        (when (some? debugExtension)
                          (let [length (.-length debugExtension)]
                            (.putByteArray
                              (.putInt (.putShort
                                         result
                                         (.addConstantUtf8
                                           symbolTable
                                           Constants/SOURCE_DEBUG_EXTENSION))
                                       length)
                              (.-data debugExtension)
                              0
                              length)))
                        (when-not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)
                          (.putInt (.putShort result
                                              (.addConstantUtf8 symbolTable Constants/DEPRECATED))
                                   0))
                        (AnnotationWriter/putAnnotations
                          symbolTable
                          lastRuntimeVisibleAnnotation
                          lastRuntimeInvisibleAnnotation
                          lastRuntimeVisibleTypeAnnotation
                          lastRuntimeInvisibleTypeAnnotation
                          result)
                        (.putBootstrapMethods symbolTable result)
                        (when (some? moduleWriter) (.putAttributes moduleWriter result))
                        (when-not (== nestHostClassIndex 0)
                          (.putShort (.putInt (.putShort
                                                result
                                                (.addConstantUtf8 symbolTable Constants/NEST_HOST))
                                              2)
                                     nestHostClassIndex))
                        (when (some? nestMemberClasses)
                          (.putByteArray
                            (.putShort (.putInt (.putShort
                                                  result
                                                  (.addConstantUtf8
                                                    symbolTable
                                                    Constants/NEST_MEMBERS))
                                                (unchecked-add-int (.-length nestMemberClasses) 2))
                                       numberOfNestMemberClasses)
                            (.-data nestMemberClasses)
                            0
                            (.-length nestMemberClasses)))
                        (when (some? permittedSubclasses)
                          (.putByteArray
                            (.putShort (.putInt
                                         (.putShort
                                           result
                                           (.addConstantUtf8
                                             symbolTable
                                             Constants/PERMITTED_SUBCLASSES))
                                         (unchecked-add-int (.-length permittedSubclasses) 2))
                                       numberOfPermittedSubclasses)
                            (.-data permittedSubclasses)
                            0
                            (.-length permittedSubclasses)))
                        (when (or (not (== (bit-and-int accessFlags Opcodes/ACC_RECORD) 0))
                                  (some? firstRecordComponent))
                          (.putShort (.putInt (.putShort
                                                result
                                                (.addConstantUtf8 symbolTable Constants/RECORD))
                                              (unchecked-add-int recordSize 2))
                                     recordComponentCount)
                          (let [^:mutable recordComponentWriter firstRecordComponent]
                            (while (some? recordComponentWriter)
                              (.putRecordComponentInfo recordComponentWriter result)
                              (set! recordComponentWriter
                                    (cast RecordComponentWriter (.-delegate recordComponentWriter))))))
                        (when (some? firstAttribute)
                          (.putAttributes firstAttribute symbolTable result))
                        (if hasAsmInstructions
                            (do
                              (.replaceAsmInstructions this (.-data result) hasFrames)
                              (recur (unchecked-inc-int pass)))
                            (return (.-data result)))))))))))))
    (throw (AssertionError.)))

  (method ^:private replaceAsmInstructions ^void [this ^:final ^byte/1 classFile
                                                  ^:final ^boolean hasFrames]
    (let [attributes (.getAttributePrototypes this)]
      (set! firstField nil)
      (set! lastField nil)
      (set! firstMethod nil)
      (set! lastMethod nil)
      (set! lastRuntimeVisibleAnnotation nil)
      (set! lastRuntimeInvisibleAnnotation nil)
      (set! lastRuntimeVisibleTypeAnnotation nil)
      (set! lastRuntimeInvisibleTypeAnnotation nil)
      (set! moduleWriter nil)
      (set! nestHostClassIndex 0)
      (set! numberOfNestMemberClasses 0)
      (set! nestMemberClasses nil)
      (set! numberOfPermittedSubclasses 0)
      (set! permittedSubclasses nil)
      (set! firstRecordComponent nil)
      (set! lastRecordComponent nil)
      (set! firstAttribute nil)
      (set! compute
            (if hasFrames MethodWriter/COMPUTE_INSERTED_FRAMES MethodWriter/COMPUTE_NOTHING))
      (.accept (^[byte/1 int boolean] ClassReader/new classFile 0 false)
               this
               attributes
               (bit-or-int (if hasFrames ClassReader/EXPAND_FRAMES 0) ClassReader/EXPAND_ASM_INSNS))))

  (method ^:private getAttributePrototypes ^Attribute/1 [this]
    (let [attributePrototypes (Attribute$Set.)]
      (.addAttributes attributePrototypes firstAttribute)
      (let [^:mutable fieldWriter firstField]
        (while (some? fieldWriter)
          (.collectAttributePrototypes fieldWriter attributePrototypes)
          (set! fieldWriter (cast FieldWriter (.-fv fieldWriter))))
        (let [^:mutable methodWriter firstMethod]
          (while (some? methodWriter)
            (.collectAttributePrototypes methodWriter attributePrototypes)
            (set! methodWriter (cast MethodWriter (.-mv methodWriter))))
          (let [^:mutable recordComponentWriter firstRecordComponent]
            (while (some? recordComponentWriter)
              (.collectAttributePrototypes recordComponentWriter attributePrototypes)
              (set! recordComponentWriter
                    (cast RecordComponentWriter (.-delegate recordComponentWriter))))
            (.toArray attributePrototypes))))))

  (method ^:public newConst ^int [this ^:final value]
    (.-index (.addConstant symbolTable value)))

  (method ^:public newUTF8 ^int [this ^:final ^String value]
    (.addConstantUtf8 symbolTable value))

  (method ^:public newClass ^int [this ^:final ^String value]
    (.-index (.addConstantClass symbolTable value)))

  (method ^:public newMethodType ^int [this ^:final ^String methodDescriptor]
    (.-index (.addConstantMethodType symbolTable methodDescriptor)))

  (method ^:public newModule ^int [this ^:final ^String moduleName]
    (.-index (.addConstantModule symbolTable moduleName)))

  (method ^:public newPackage ^int [this ^:final ^String packageName]
    (.-index (.addConstantPackage symbolTable packageName)))

  (method ^:public ^{Deprecated {:forRemoval false}} newHandle ^int [this ^:final ^int tag
                                                                     ^:final ^String owner
                                                                     ^:final ^String name
                                                                     ^:final ^String descriptor]
    (.newHandle this tag owner name descriptor (== tag Opcodes/H_INVOKEINTERFACE)))

  (method ^:public newHandle ^int [this ^:final ^int tag ^:final ^String owner ^:final ^String name
                                   ^:final ^String descriptor ^:final ^boolean isInterface]
    (.-index (.addConstantMethodHandle symbolTable tag owner name descriptor isInterface)))

  (method ^:public newConstantDynamic ^int [this ^:final ^String name ^:final ^String descriptor
                                            ^:final ^Handle bootstrapMethodHandle &
                                            ^:final ^Object/1 bootstrapMethodArguments]
    (.-index (.addConstantDynamic symbolTable
                                  name
                                  descriptor
                                  bootstrapMethodHandle
                                  bootstrapMethodArguments)))

  (method ^:public newInvokeDynamic ^int [this ^:final ^String name ^:final ^String descriptor
                                          ^:final ^Handle bootstrapMethodHandle &
                                          ^:final ^Object/1 bootstrapMethodArguments]
    (.-index (.addConstantInvokeDynamic symbolTable
                                        name
                                        descriptor
                                        bootstrapMethodHandle
                                        bootstrapMethodArguments)))

  (method ^:public newField ^int [this ^:final ^String owner ^:final ^String name
                                  ^:final ^String descriptor]
    (.-index (.addConstantFieldref symbolTable owner name descriptor)))

  (method ^:public newMethod ^int [this ^:final ^String owner ^:final ^String name
                                   ^:final ^String descriptor ^:final ^boolean isInterface]
    (.-index (.addConstantMethodref symbolTable owner name descriptor isInterface)))

  (method ^:public newNameType ^int [this ^:final ^String name ^:final ^String descriptor]
    (.addConstantNameAndType symbolTable name descriptor))

  (method ^:public ^:final setFlags ^void [this ^:final ^int flags]
    (cond
      (not (== (bit-and-int flags ClassWriter/COMPUTE_FRAMES) 0))
        (set! compute MethodWriter/COMPUTE_ALL_FRAMES)
      (not (== (bit-and-int flags ClassWriter/COMPUTE_MAXS) 0))
        (set! compute MethodWriter/COMPUTE_MAX_STACK_AND_LOCAL)
      :else (set! compute MethodWriter/COMPUTE_NOTHING)))

  (method ^:protected getCommonSuperClass ^String [this ^:final ^String type1 ^:final ^String type2]
    (let [classLoader (.getClassLoader this)
          ^:mutable ^{:tag (Class ?)} class1 nil]
      (try
        (set! class1 (Class/forName (.replace type1 \/ \.) false classLoader))
        (catch ClassNotFoundException e (throw (TypeNotPresentException. type1 e))))
      (let [^:mutable ^{:tag (Class ?)} class2 nil]
        (try
          (set! class2 (Class/forName (.replace type2 \/ \.) false classLoader))
          (catch ClassNotFoundException e (throw (TypeNotPresentException. type2 e))))
        (cond
          (.isAssignableFrom class1 class2) type1
          (.isAssignableFrom class2 class1) type2
          (or (.isInterface class1) (.isInterface class2)) "java/lang/Object"
          :else
            (do
              (loop []
                (set! class1 (.getSuperclass class1))
                (when-not (.isAssignableFrom class1 class2) (recur)))
              (.replace (.getName class1) \. \/))))))

  (method ^:protected getClassLoader ^ClassLoader [this]
    (.getClassLoader (.getClass this))))
