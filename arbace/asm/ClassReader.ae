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
;; Converted from clojure/asm/ClassReader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(import '(java.io ByteArrayOutputStream IOException InputStream))

(defclass ^:public ClassReader
  (field ^:public ^:static ^:final ^int SKIP_CODE 1)

  (field ^:public ^:static ^:final ^int SKIP_DEBUG 2)

  (field ^:public ^:static ^:final ^int SKIP_FRAMES 4)

  (field ^:public ^:static ^:final ^int EXPAND_FRAMES 8)

  (field ^:static ^:final ^int EXPAND_ASM_INSNS 256)

  (field ^:private ^:static ^:final ^int MAX_BUFFER_SIZE (unchecked-multiply-int 1024 1024))

  (field ^:private ^:static ^:final ^int INPUT_STREAM_DATA_CHUNK_SIZE 4096)

  (field ^:public ^:final ^{Deprecated {:forRemoval false}} ^byte/1 b)

  (field ^:public ^:final ^int header)

  (field ^:final ^byte/1 classFileBuffer)

  (field ^:private ^:final ^int/1 cpInfoOffsets)

  (field ^:private ^:final ^String/1 constantUtf8Values)

  (field ^:private ^:final ^ConstantDynamic/1 constantDynamicValues)

  (field ^:private ^:final ^int/1 bootstrapMethodOffsets)

  (field ^:private ^:final ^int maxStringLength)

  (constructor ^:public [this ^:final ^byte/1 classFile]
    (this. classFile 0 (alength classFile)))

  (constructor ^:public [this ^:final ^byte/1 classFileBuffer ^:final ^int classFileOffset
                         ^:final ^int classFileLength]
    (this. classFileBuffer classFileOffset true))

  (constructor [this ^:final ^byte/1 classFileBuffer ^:final ^int classFileOffset
                ^:final ^boolean checkClassVersion]
    (set! (.-classFileBuffer this) classFileBuffer)
    (set! (.-b this) classFileBuffer)
    (let [^:mutable ^short major 0]
      (when (and checkClassVersion
                 (or (> (set! major (.readShort this (unchecked-add-int classFileOffset 6)))
                        Opcodes/V28)
                     (and (== major Opcodes/V28)
                          (not (== (.readShort this (unchecked-add-int classFileOffset 4)) 0)))))
        (throw (IllegalArgumentException.
                 (java-str "Unsupported class file version "
                           (.readShort this (unchecked-add-int classFileOffset 6))
                           "."
                           (bit-and-int (.readShort this (unchecked-add-int classFileOffset 4))
                                        0xFFFF)))))
      (let [constantPoolCount (.readUnsignedShort this (unchecked-add-int classFileOffset 8))]
        (set! cpInfoOffsets (new int/1 constantPoolCount))
        (set! constantUtf8Values (new String/1 constantPoolCount))
        (let [^:mutable ^int currentCpInfoIndex 1
              ^:mutable currentCpInfoOffset (unchecked-add-int classFileOffset 10)
              ^:mutable ^int currentMaxStringLength 0
              ^:mutable hasBootstrapMethods false
              ^:mutable hasConstantDynamic false]
          (while (< currentCpInfoIndex constantPoolCount)
            (aset cpInfoOffsets currentCpInfoIndex (unchecked-add-int currentCpInfoOffset 1))
            (set! currentCpInfoIndex (unchecked-inc-int currentCpInfoIndex))
            (let [^:mutable ^int cpInfoSize 0]
              (switch (aget classFileBuffer currentCpInfoOffset)
                (Symbol/CONSTANT_FIELDREF_TAG Symbol/CONSTANT_METHODREF_TAG
                                              Symbol/CONSTANT_INTERFACE_METHODREF_TAG
                                              Symbol/CONSTANT_INTEGER_TAG
                                              Symbol/CONSTANT_FLOAT_TAG
                                              Symbol/CONSTANT_NAME_AND_TYPE_TAG)
                  (set! cpInfoSize 5)
                Symbol/CONSTANT_DYNAMIC_TAG
                  (do
                    (set! cpInfoSize 5)
                    (set! hasBootstrapMethods true)
                    (set! hasConstantDynamic true))
                Symbol/CONSTANT_INVOKE_DYNAMIC_TAG
                  (do (set! cpInfoSize 5) (set! hasBootstrapMethods true))
                (Symbol/CONSTANT_LONG_TAG Symbol/CONSTANT_DOUBLE_TAG)
                  (do
                    (set! cpInfoSize 9)
                    (set! currentCpInfoIndex (unchecked-inc-int currentCpInfoIndex)))
                Symbol/CONSTANT_UTF8_TAG
                  (do
                    (set! cpInfoSize
                          (unchecked-add-int 3
                                             (.readUnsignedShort
                                               this
                                               (unchecked-add-int currentCpInfoOffset 1))))
                    (when (> cpInfoSize currentMaxStringLength)
                      (set! currentMaxStringLength cpInfoSize)))
                Symbol/CONSTANT_METHOD_HANDLE_TAG (set! cpInfoSize 4)
                (Symbol/CONSTANT_CLASS_TAG Symbol/CONSTANT_STRING_TAG
                                           Symbol/CONSTANT_METHOD_TYPE_TAG
                                           Symbol/CONSTANT_PACKAGE_TAG
                                           Symbol/CONSTANT_MODULE_TAG)
                  (set! cpInfoSize 3)
                (throw (IllegalArgumentException.)))
              (set! currentCpInfoOffset (unchecked-add-int currentCpInfoOffset cpInfoSize))))
          (set! maxStringLength currentMaxStringLength)
          (set! header currentCpInfoOffset)
          (set! constantDynamicValues
                (when hasConstantDynamic (new ConstantDynamic/1 constantPoolCount)))
          (set! bootstrapMethodOffsets
                (when hasBootstrapMethods
                  (.readBootstrapMethodsAttribute this currentMaxStringLength)))))))

  (constructor :throws [IOException] ^:public [this ^:final ^InputStream inputStream]
    (this. (ClassReader/readStream inputStream false)))

  (constructor :throws [IOException] ^:public [this ^:final ^String className]
    (this. (ClassReader/readStream
             (ClassLoader/getSystemResourceAsStream (java-str (.replace className \. \/) ".class"))
             true)))

  (method ^:private ^:static readStream :throws [IOException] ^byte/1 [^:final ^InputStream inputStream
                                                                       ^:final ^boolean close]
    (when (nil? inputStream) (throw (IOException. "Class not found")))
    (let [bufferSize (ClassReader/computeBufferSize inputStream)]
      (try
        (with-resources [outputStream (ByteArrayOutputStream.)]
          (let [data (new byte/1 bufferSize)
                ^:mutable ^int bytesRead 0
                ^:mutable ^int readCount 0]
            (while (not (== (set! bytesRead (.read inputStream data 0 bufferSize)) -1))
              (.write outputStream data 0 bytesRead)
              (set! readCount (unchecked-inc-int readCount)))
            (.flush outputStream)
            (if (== readCount 1) data (.toByteArray outputStream))))
        (finally (when close (.close inputStream))))))

  (method ^:private ^:static computeBufferSize :throws [IOException] ^int [^:final ^InputStream inputStream]
    (let [expectedLength (.available inputStream)]
      (if (< expectedLength 256)
          INPUT_STREAM_DATA_CHUNK_SIZE
          (Math/min expectedLength MAX_BUFFER_SIZE))))

  (method ^:public getAccess ^int [this] (.readUnsignedShort this header))

  (method ^:public getClassName ^String [this]
    (.readClass this (unchecked-add-int header 2) (new char/1 maxStringLength)))

  (method ^:public getSuperName ^String [this]
    (.readClass this (unchecked-add-int header 4) (new char/1 maxStringLength)))

  (method ^:public getInterfaces ^String/1 [this]
    (let [^:mutable currentOffset (unchecked-add-int header 6)
          interfacesCount (.readUnsignedShort this currentOffset)
          interfaces (new String/1 interfacesCount)]
      (when (> interfacesCount 0)
        (let [charBuffer (new char/1 maxStringLength)]
          (loop [^int i 0]
            (when (< i interfacesCount)
              (set! currentOffset (unchecked-add-int currentOffset 2))
              (aset interfaces i (.readClass this currentOffset charBuffer))
              (recur (unchecked-inc-int i))))))
      interfaces))

  (method ^:public accept ^void [this ^:final ^ClassVisitor classVisitor ^:final ^int parsingOptions]
    (.accept this classVisitor (new Attribute/1 0) parsingOptions))

  (method ^:public accept ^void [this ^:final ^ClassVisitor classVisitor
                                 ^:final ^Attribute/1 attributePrototypes
                                 ^:final ^int parsingOptions]
    (let [context (Context.)]
      (set! (.-attributePrototypes context) attributePrototypes)
      (set! (.-parsingOptions context) parsingOptions)
      (set! (.-charBuffer context) (new char/1 maxStringLength))
      (let [charBuffer (.-charBuffer context)
            ^:mutable currentOffset header
            ^:mutable accessFlags (.readUnsignedShort this currentOffset)
            thisClass (.readClass this (unchecked-add-int currentOffset 2) charBuffer)
            superClass (.readClass this (unchecked-add-int currentOffset 4) charBuffer)
            interfaces (new String/1 (.readUnsignedShort this (unchecked-add-int currentOffset 6)))]
        (set! currentOffset (unchecked-add-int currentOffset 8))
        (loop [^int i 0]
          (when (< i (alength interfaces))
            (aset interfaces i (.readClass this currentOffset charBuffer))
            (set! currentOffset (unchecked-add-int currentOffset 2))
            (recur (unchecked-inc-int i))))
        (let [^:mutable ^int innerClassesOffset 0
              ^:mutable ^int enclosingMethodOffset 0
              ^:mutable ^String signature nil
              ^:mutable ^String sourceFile nil
              ^:mutable ^String sourceDebugExtension nil
              ^:mutable ^int runtimeVisibleAnnotationsOffset 0
              ^:mutable ^int runtimeInvisibleAnnotationsOffset 0
              ^:mutable ^int runtimeVisibleTypeAnnotationsOffset 0
              ^:mutable ^int runtimeInvisibleTypeAnnotationsOffset 0
              ^:mutable ^int moduleOffset 0
              ^:mutable ^int modulePackagesOffset 0
              ^:mutable ^String moduleMainClass nil
              ^:mutable ^String nestHostClass nil
              ^:mutable ^int nestMembersOffset 0
              ^:mutable ^int permittedSubclassesOffset 0
              ^:mutable ^int recordOffset 0
              ^:mutable ^Attribute attributes nil
              ^:mutable currentAttributeOffset (.getFirstAttributeOffset this)]
          (loop [^int i (.readUnsignedShort this (unchecked-subtract-int currentAttributeOffset 2))]
            (when (> i 0)
              (let [attributeName (.readUTF8 this currentAttributeOffset charBuffer)
                    attributeLength (.readInt this (unchecked-add-int currentAttributeOffset 2))]
                (set! currentAttributeOffset (unchecked-add-int currentAttributeOffset 6))
                (cond
                  (.equals Constants/SOURCE_FILE attributeName)
                    (set! sourceFile (.readUTF8 this currentAttributeOffset charBuffer))
                  (.equals Constants/INNER_CLASSES attributeName)
                    (set! innerClassesOffset currentAttributeOffset)
                  (.equals Constants/ENCLOSING_METHOD attributeName)
                    (set! enclosingMethodOffset currentAttributeOffset)
                  (.equals Constants/NEST_HOST attributeName)
                    (set! nestHostClass (.readClass this currentAttributeOffset charBuffer))
                  (.equals Constants/NEST_MEMBERS attributeName)
                    (set! nestMembersOffset currentAttributeOffset)
                  (.equals Constants/PERMITTED_SUBCLASSES attributeName)
                    (set! permittedSubclassesOffset currentAttributeOffset)
                  (.equals Constants/SIGNATURE attributeName)
                    (set! signature (.readUTF8 this currentAttributeOffset charBuffer))
                  (.equals Constants/RUNTIME_VISIBLE_ANNOTATIONS attributeName)
                    (set! runtimeVisibleAnnotationsOffset currentAttributeOffset)
                  (.equals Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS attributeName)
                    (set! runtimeVisibleTypeAnnotationsOffset currentAttributeOffset)
                  (.equals Constants/DEPRECATED attributeName)
                    (set! accessFlags (bit-or-int accessFlags Opcodes/ACC_DEPRECATED))
                  (.equals Constants/SYNTHETIC attributeName)
                    (set! accessFlags (bit-or-int accessFlags Opcodes/ACC_SYNTHETIC))
                  (.equals Constants/SOURCE_DEBUG_EXTENSION attributeName)
                    (do
                      (when (> attributeLength
                               (unchecked-subtract-int
                                 (alength classFileBuffer)
                                 currentAttributeOffset))
                        (throw (IllegalArgumentException.)))
                      (set! sourceDebugExtension
                            (.readUtf this
                                      currentAttributeOffset
                                      attributeLength
                                      (new char/1 attributeLength))))
                  (.equals Constants/RUNTIME_INVISIBLE_ANNOTATIONS attributeName)
                    (set! runtimeInvisibleAnnotationsOffset currentAttributeOffset)
                  (.equals Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS attributeName)
                    (set! runtimeInvisibleTypeAnnotationsOffset currentAttributeOffset)
                  (.equals Constants/RECORD attributeName)
                    (do
                      (set! recordOffset currentAttributeOffset)
                      (set! accessFlags (bit-or-int accessFlags Opcodes/ACC_RECORD)))
                  (.equals Constants/MODULE attributeName)
                    (set! moduleOffset currentAttributeOffset)
                  (.equals Constants/MODULE_MAIN_CLASS attributeName)
                    (set! moduleMainClass (.readClass this currentAttributeOffset charBuffer))
                  (.equals Constants/MODULE_PACKAGES attributeName)
                    (set! modulePackagesOffset currentAttributeOffset)
                  :else
                    (when-not (.equals Constants/BOOTSTRAP_METHODS attributeName)
                      (let [attribute (.readAttribute
                                        this
                                        attributePrototypes
                                        attributeName
                                        currentAttributeOffset
                                        attributeLength
                                        charBuffer
                                        -1
                                        nil)]
                        (set! (.-nextAttribute attribute) attributes)
                        (set! attributes attribute))))
                (set! currentAttributeOffset
                      (unchecked-add-int currentAttributeOffset attributeLength))
                (recur (unchecked-dec-int i)))))
          (.visit classVisitor
                  (.readInt this (unchecked-subtract-int (aget cpInfoOffsets 1) 7))
                  accessFlags
                  thisClass
                  signature
                  superClass
                  interfaces)
          (when (and (== (bit-and-int parsingOptions SKIP_DEBUG) 0)
                     (or (some? sourceFile) (some? sourceDebugExtension)))
            (.visitSource classVisitor sourceFile sourceDebugExtension))
          (when-not (== moduleOffset 0)
            (.readModuleAttributes this
                                   classVisitor
                                   context
                                   moduleOffset
                                   modulePackagesOffset
                                   moduleMainClass))
          (when (some? nestHostClass) (.visitNestHost classVisitor nestHostClass))
          (when-not (== enclosingMethodOffset 0)
            (let [className (.readClass this enclosingMethodOffset charBuffer)
                  methodIndex (.readUnsignedShort this (unchecked-add-int enclosingMethodOffset 2))
                  name (when-not (== methodIndex 0)
                         (.readUTF8 this (aget cpInfoOffsets methodIndex) charBuffer))
                  type (when-not (== methodIndex 0)
                         (.readUTF8 this
                                    (unchecked-add-int (aget cpInfoOffsets methodIndex) 2)
                                    charBuffer))]
              (.visitOuterClass classVisitor className name type)))
          (when-not (== runtimeVisibleAnnotationsOffset 0)
            (let [^:mutable numAnnotations (.readUnsignedShort this runtimeVisibleAnnotationsOffset)
                  ^:mutable currentAnnotationOffset (unchecked-add-int
                                                      runtimeVisibleAnnotationsOffset
                                                      2)]
              (while (> (let [old-1 numAnnotations]
                          (set! numAnnotations (unchecked-dec-int numAnnotations))
                          old-1)
                        0)
                (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                  (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                  (set! currentAnnotationOffset
                        (.readElementValues this
                                            (.visitAnnotation
                                              classVisitor
                                              annotationDescriptor
                                              true)
                                            currentAnnotationOffset
                                            true
                                            charBuffer
                                            0))))))
          (when-not (== runtimeInvisibleAnnotationsOffset 0)
            (let [^:mutable numAnnotations (.readUnsignedShort
                                             this
                                             runtimeInvisibleAnnotationsOffset)
                  ^:mutable currentAnnotationOffset (unchecked-add-int
                                                      runtimeInvisibleAnnotationsOffset
                                                      2)]
              (while (> (let [old-2 numAnnotations]
                          (set! numAnnotations (unchecked-dec-int numAnnotations))
                          old-2)
                        0)
                (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                  (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                  (set! currentAnnotationOffset
                        (.readElementValues this
                                            (.visitAnnotation
                                              classVisitor
                                              annotationDescriptor
                                              false)
                                            currentAnnotationOffset
                                            true
                                            charBuffer
                                            0))))))
          (when-not (== runtimeVisibleTypeAnnotationsOffset 0)
            (let [^:mutable numAnnotations (.readUnsignedShort
                                             this
                                             runtimeVisibleTypeAnnotationsOffset)
                  ^:mutable currentAnnotationOffset (unchecked-add-int
                                                      runtimeVisibleTypeAnnotationsOffset
                                                      2)]
              (while (> (let [old-3 numAnnotations]
                          (set! numAnnotations (unchecked-dec-int numAnnotations))
                          old-3)
                        0)
                (set! currentAnnotationOffset
                      (.readTypeAnnotationTarget this context currentAnnotationOffset))
                (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                  (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                  (set! currentAnnotationOffset
                        (.readElementValues this
                                            (.visitTypeAnnotation
                                              classVisitor
                                              (.-currentTypeAnnotationTarget context)
                                              (.-currentTypeAnnotationTargetPath context)
                                              annotationDescriptor
                                              true)
                                            currentAnnotationOffset
                                            true
                                            charBuffer
                                            0))))))
          (when-not (== runtimeInvisibleTypeAnnotationsOffset 0)
            (let [^:mutable numAnnotations (.readUnsignedShort
                                             this
                                             runtimeInvisibleTypeAnnotationsOffset)
                  ^:mutable currentAnnotationOffset (unchecked-add-int
                                                      runtimeInvisibleTypeAnnotationsOffset
                                                      2)]
              (while (> (let [old-4 numAnnotations]
                          (set! numAnnotations (unchecked-dec-int numAnnotations))
                          old-4)
                        0)
                (set! currentAnnotationOffset
                      (.readTypeAnnotationTarget this context currentAnnotationOffset))
                (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                  (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                  (set! currentAnnotationOffset
                        (.readElementValues this
                                            (.visitTypeAnnotation
                                              classVisitor
                                              (.-currentTypeAnnotationTarget context)
                                              (.-currentTypeAnnotationTargetPath context)
                                              annotationDescriptor
                                              false)
                                            currentAnnotationOffset
                                            true
                                            charBuffer
                                            0))))))
          (while (some? attributes)
            (let [nextAttribute (.-nextAttribute attributes)]
              (set! (.-nextAttribute attributes) nil)
              (.visitAttribute classVisitor attributes)
              (set! attributes nextAttribute)))
          (when-not (== nestMembersOffset 0)
            (let [^:mutable numberOfNestMembers (.readUnsignedShort this nestMembersOffset)
                  ^:mutable currentNestMemberOffset (unchecked-add-int nestMembersOffset 2)]
              (while (> (let [old-5 numberOfNestMembers]
                          (set! numberOfNestMembers (unchecked-dec-int numberOfNestMembers))
                          old-5)
                        0)
                (.visitNestMember classVisitor (.readClass this currentNestMemberOffset charBuffer))
                (set! currentNestMemberOffset (unchecked-add-int currentNestMemberOffset 2)))))
          (when-not (== permittedSubclassesOffset 0)
            (let [^:mutable numberOfPermittedSubclasses (.readUnsignedShort
                                                          this
                                                          permittedSubclassesOffset)
                  ^:mutable currentPermittedSubclassesOffset (unchecked-add-int
                                                               permittedSubclassesOffset
                                                               2)]
              (while (> (let [old-6 numberOfPermittedSubclasses]
                          (set! numberOfPermittedSubclasses
                                (unchecked-dec-int numberOfPermittedSubclasses))
                          old-6)
                        0)
                (.visitPermittedSubclass classVisitor
                                         (.readClass
                                           this
                                           currentPermittedSubclassesOffset
                                           charBuffer))
                (set! currentPermittedSubclassesOffset
                      (unchecked-add-int currentPermittedSubclassesOffset 2)))))
          (when-not (== innerClassesOffset 0)
            (let [^:mutable numberOfClasses (.readUnsignedShort this innerClassesOffset)
                  ^:mutable currentClassesOffset (unchecked-add-int innerClassesOffset 2)]
              (while (> (let [old-7 numberOfClasses]
                          (set! numberOfClasses (unchecked-dec-int numberOfClasses))
                          old-7)
                        0)
                (.visitInnerClass classVisitor
                                  (.readClass this currentClassesOffset charBuffer)
                                  (.readClass this
                                              (unchecked-add-int currentClassesOffset 2)
                                              charBuffer)
                                  (.readUTF8 this
                                             (unchecked-add-int currentClassesOffset 4)
                                             charBuffer)
                                  (.readUnsignedShort
                                    this
                                    (unchecked-add-int currentClassesOffset 6)))
                (set! currentClassesOffset (unchecked-add-int currentClassesOffset 8)))))
          (when-not (== recordOffset 0)
            (let [^:mutable recordComponentsCount (.readUnsignedShort this recordOffset)]
              (set! recordOffset (unchecked-add-int recordOffset 2))
              (while (> (let [old-8 recordComponentsCount]
                          (set! recordComponentsCount (unchecked-dec-int recordComponentsCount))
                          old-8)
                        0)
                (set! recordOffset (.readRecordComponent this classVisitor context recordOffset)))))
          (let [^:mutable fieldsCount (.readUnsignedShort this currentOffset)]
            (set! currentOffset (unchecked-add-int currentOffset 2))
            (while (> (let [old-9 fieldsCount]
                        (set! fieldsCount (unchecked-dec-int fieldsCount))
                        old-9)
                      0)
              (set! currentOffset (.readField this classVisitor context currentOffset)))
            (let [^:mutable methodsCount (.readUnsignedShort this currentOffset)]
              (set! currentOffset (unchecked-add-int currentOffset 2))
              (while (> (let [old-10 methodsCount]
                          (set! methodsCount (unchecked-dec-int methodsCount))
                          old-10)
                        0)
                (set! currentOffset (.readMethod this classVisitor context currentOffset)))
              (.visitEnd classVisitor)))))))

  (method ^:private readModuleAttributes ^void [this ^:final ^ClassVisitor classVisitor
                                                ^:final ^Context context ^:final ^int moduleOffset
                                                ^:final ^int modulePackagesOffset
                                                ^:final ^String moduleMainClass]
    (let [buffer (.-charBuffer context)
          ^:mutable currentOffset moduleOffset
          moduleName (.readModule this currentOffset buffer)
          moduleFlags (.readUnsignedShort this (unchecked-add-int currentOffset 2))
          moduleVersion (.readUTF8 this (unchecked-add-int currentOffset 4) buffer)]
      (set! currentOffset (unchecked-add-int currentOffset 6))
      (let [moduleVisitor (.visitModule classVisitor moduleName moduleFlags moduleVersion)]
        (when (some? moduleVisitor)
          (when (some? moduleMainClass) (.visitMainClass moduleVisitor moduleMainClass))
          (when-not (== modulePackagesOffset 0)
            (let [^:mutable packageCount (.readUnsignedShort this modulePackagesOffset)
                  ^:mutable currentPackageOffset (unchecked-add-int modulePackagesOffset 2)]
              (while (> (let [old-11 packageCount]
                          (set! packageCount (unchecked-dec-int packageCount))
                          old-11)
                        0)
                (.visitPackage moduleVisitor (.readPackage this currentPackageOffset buffer))
                (set! currentPackageOffset (unchecked-add-int currentPackageOffset 2)))))
          (let [^:mutable requiresCount (.readUnsignedShort this currentOffset)]
            (set! currentOffset (unchecked-add-int currentOffset 2))
            (while (> (let [old-12 requiresCount]
                        (set! requiresCount (unchecked-dec-int requiresCount))
                        old-12)
                      0)
              (let [requires (.readModule this currentOffset buffer)
                    requiresFlags (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                    requiresVersion (.readUTF8 this (unchecked-add-int currentOffset 4) buffer)]
                (set! currentOffset (unchecked-add-int currentOffset 6))
                (.visitRequire moduleVisitor requires requiresFlags requiresVersion)))
            (let [^:mutable exportsCount (.readUnsignedShort this currentOffset)]
              (set! currentOffset (unchecked-add-int currentOffset 2))
              (while (> (let [old-13 exportsCount]
                          (set! exportsCount (unchecked-dec-int exportsCount))
                          old-13)
                        0)
                (let [exports (.readPackage this currentOffset buffer)
                      exportsFlags (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                      exportsToCount (.readUnsignedShort this (unchecked-add-int currentOffset 4))]
                  (set! currentOffset (unchecked-add-int currentOffset 6))
                  (let [^:mutable ^String/1 exportsTo nil]
                    (when-not (== exportsToCount 0)
                      (set! exportsTo (new String/1 exportsToCount))
                      (loop [^int i 0]
                        (when (< i exportsToCount)
                          (aset exportsTo i (.readModule this currentOffset buffer))
                          (set! currentOffset (unchecked-add-int currentOffset 2))
                          (recur (unchecked-inc-int i)))))
                    (.visitExport moduleVisitor exports exportsFlags exportsTo))))
              (let [^:mutable opensCount (.readUnsignedShort this currentOffset)]
                (set! currentOffset (unchecked-add-int currentOffset 2))
                (while (> (let [old-14 opensCount]
                            (set! opensCount (unchecked-dec-int opensCount))
                            old-14)
                          0)
                  (let [opens (.readPackage this currentOffset buffer)
                        opensFlags (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                        opensToCount (.readUnsignedShort this (unchecked-add-int currentOffset 4))]
                    (set! currentOffset (unchecked-add-int currentOffset 6))
                    (let [^:mutable ^String/1 opensTo nil]
                      (when-not (== opensToCount 0)
                        (set! opensTo (new String/1 opensToCount))
                        (loop [^int i 0]
                          (when (< i opensToCount)
                            (aset opensTo i (.readModule this currentOffset buffer))
                            (set! currentOffset (unchecked-add-int currentOffset 2))
                            (recur (unchecked-inc-int i)))))
                      (.visitOpen moduleVisitor opens opensFlags opensTo))))
                (let [^:mutable usesCount (.readUnsignedShort this currentOffset)]
                  (set! currentOffset (unchecked-add-int currentOffset 2))
                  (while (> (let [old-15 usesCount]
                              (set! usesCount (unchecked-dec-int usesCount))
                              old-15)
                            0)
                    (.visitUse moduleVisitor (.readClass this currentOffset buffer))
                    (set! currentOffset (unchecked-add-int currentOffset 2)))
                  (let [^:mutable providesCount (.readUnsignedShort this currentOffset)]
                    (set! currentOffset (unchecked-add-int currentOffset 2))
                    (while (> (let [old-16 providesCount]
                                (set! providesCount (unchecked-dec-int providesCount))
                                old-16)
                              0)
                      (let [provides (.readClass this currentOffset buffer)
                            providesWithCount (.readUnsignedShort
                                                this
                                                (unchecked-add-int currentOffset 2))]
                        (set! currentOffset (unchecked-add-int currentOffset 4))
                        (let [providesWith (new String/1 providesWithCount)]
                          (loop [^int i 0]
                            (when (< i providesWithCount)
                              (aset providesWith i (.readClass this currentOffset buffer))
                              (set! currentOffset (unchecked-add-int currentOffset 2))
                              (recur (unchecked-inc-int i))))
                          (.visitProvide moduleVisitor provides providesWith))))
                    (.visitEnd moduleVisitor))))))))))

  (method ^:private readRecordComponent ^int [this ^:final ^ClassVisitor classVisitor
                                              ^:final ^Context context
                                              ^:final ^int recordComponentOffset]
    (let [charBuffer (.-charBuffer context)
          ^:mutable currentOffset recordComponentOffset
          name (.readUTF8 this currentOffset charBuffer)
          descriptor (.readUTF8 this (unchecked-add-int currentOffset 2) charBuffer)]
      (set! currentOffset (unchecked-add-int currentOffset 4))
      (let [^:mutable ^String signature nil
            ^:mutable ^int runtimeVisibleAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleAnnotationsOffset 0
            ^:mutable ^int runtimeVisibleTypeAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleTypeAnnotationsOffset 0
            ^:mutable ^Attribute attributes nil
            ^:mutable attributesCount (.readUnsignedShort this currentOffset)]
        (set! currentOffset (unchecked-add-int currentOffset 2))
        (while (> (let [old-17 attributesCount]
                    (set! attributesCount (unchecked-dec-int attributesCount))
                    old-17)
                  0)
          (let [attributeName (.readUTF8 this currentOffset charBuffer)
                attributeLength (.readInt this (unchecked-add-int currentOffset 2))]
            (set! currentOffset (unchecked-add-int currentOffset 6))
            (cond
              (.equals Constants/SIGNATURE attributeName)
                (set! signature (.readUTF8 this currentOffset charBuffer))
              (.equals Constants/RUNTIME_VISIBLE_ANNOTATIONS attributeName)
                (set! runtimeVisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeVisibleTypeAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleTypeAnnotationsOffset currentOffset)
              :else
                (let [attribute (.readAttribute this
                                                (.-attributePrototypes context)
                                                attributeName
                                                currentOffset
                                                attributeLength
                                                charBuffer
                                                -1
                                                nil)]
                  (set! (.-nextAttribute attribute) attributes)
                  (set! attributes attribute)))
            (set! currentOffset (unchecked-add-int currentOffset attributeLength))))
        (let [recordComponentVisitor (.visitRecordComponent classVisitor name descriptor signature)]
          (if (nil? recordComponentVisitor)
              currentOffset
              (do
                (when-not (== runtimeVisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-18 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-18)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation recordComponentVisitor annotationDescriptor true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-19 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-19)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation recordComponentVisitor annotationDescriptor false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeVisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-20 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-20)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  recordComponentVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-21 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-21)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  recordComponentVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (while (some? attributes)
                  (let [nextAttribute (.-nextAttribute attributes)]
                    (set! (.-nextAttribute attributes) nil)
                    (.visitAttribute recordComponentVisitor attributes)
                    (set! attributes nextAttribute)))
                (.visitEnd recordComponentVisitor)
                currentOffset))))))

  (method ^:private readField ^int [this ^:final ^ClassVisitor classVisitor ^:final ^Context context
                                    ^:final ^int fieldInfoOffset]
    (let [charBuffer (.-charBuffer context)
          ^:mutable currentOffset fieldInfoOffset
          ^:mutable accessFlags (.readUnsignedShort this currentOffset)
          name (.readUTF8 this (unchecked-add-int currentOffset 2) charBuffer)
          descriptor (.readUTF8 this (unchecked-add-int currentOffset 4) charBuffer)]
      (set! currentOffset (unchecked-add-int currentOffset 6))
      (let [^:mutable ^Object constantValue nil
            ^:mutable ^String signature nil
            ^:mutable ^int runtimeVisibleAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleAnnotationsOffset 0
            ^:mutable ^int runtimeVisibleTypeAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleTypeAnnotationsOffset 0
            ^:mutable ^Attribute attributes nil
            ^:mutable attributesCount (.readUnsignedShort this currentOffset)]
        (set! currentOffset (unchecked-add-int currentOffset 2))
        (while (> (let [old-22 attributesCount]
                    (set! attributesCount (unchecked-dec-int attributesCount))
                    old-22)
                  0)
          (let [attributeName (.readUTF8 this currentOffset charBuffer)
                attributeLength (.readInt this (unchecked-add-int currentOffset 2))]
            (set! currentOffset (unchecked-add-int currentOffset 6))
            (cond
              (.equals Constants/CONSTANT_VALUE attributeName)
                (let [constantvalueIndex (.readUnsignedShort this currentOffset)]
                  (set! constantValue
                        (when-not (== constantvalueIndex 0)
                          (.readConst this constantvalueIndex charBuffer))))
              (.equals Constants/SIGNATURE attributeName)
                (set! signature (.readUTF8 this currentOffset charBuffer))
              (.equals Constants/DEPRECATED attributeName)
                (set! accessFlags (bit-or-int accessFlags Opcodes/ACC_DEPRECATED))
              (.equals Constants/SYNTHETIC attributeName)
                (set! accessFlags (bit-or-int accessFlags Opcodes/ACC_SYNTHETIC))
              (.equals Constants/RUNTIME_VISIBLE_ANNOTATIONS attributeName)
                (set! runtimeVisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeVisibleTypeAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleTypeAnnotationsOffset currentOffset)
              :else
                (let [attribute (.readAttribute this
                                                (.-attributePrototypes context)
                                                attributeName
                                                currentOffset
                                                attributeLength
                                                charBuffer
                                                -1
                                                nil)]
                  (set! (.-nextAttribute attribute) attributes)
                  (set! attributes attribute)))
            (set! currentOffset (unchecked-add-int currentOffset attributeLength))))
        (let [fieldVisitor (.visitField classVisitor
                                        accessFlags
                                        name
                                        descriptor
                                        signature
                                        constantValue)]
          (if (nil? fieldVisitor)
              currentOffset
              (do
                (when-not (== runtimeVisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-23 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-23)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation fieldVisitor annotationDescriptor true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-24 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-24)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation fieldVisitor annotationDescriptor false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeVisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-25 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-25)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  fieldVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-26 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-26)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  fieldVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (while (some? attributes)
                  (let [nextAttribute (.-nextAttribute attributes)]
                    (set! (.-nextAttribute attributes) nil)
                    (.visitAttribute fieldVisitor attributes)
                    (set! attributes nextAttribute)))
                (.visitEnd fieldVisitor)
                currentOffset))))))

  (method ^:private readMethod ^int [this ^:final ^ClassVisitor classVisitor
                                     ^:final ^Context context ^:final ^int methodInfoOffset]
    (let [charBuffer (.-charBuffer context)
          ^:mutable currentOffset methodInfoOffset]
      (set! (.-currentMethodAccessFlags context) (.readUnsignedShort this currentOffset))
      (set! (.-currentMethodName context)
            (.readUTF8 this (unchecked-add-int currentOffset 2) charBuffer))
      (set! (.-currentMethodDescriptor context)
            (.readUTF8 this (unchecked-add-int currentOffset 4) charBuffer))
      (set! currentOffset (unchecked-add-int currentOffset 6))
      (let [^:mutable ^int codeOffset 0
            ^:mutable ^int exceptionsOffset 0
            ^:mutable ^String/1 exceptions nil
            ^:mutable synthetic false
            ^:mutable ^int signatureIndex 0
            ^:mutable ^int runtimeVisibleAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleAnnotationsOffset 0
            ^:mutable ^int runtimeVisibleParameterAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleParameterAnnotationsOffset 0
            ^:mutable ^int runtimeVisibleTypeAnnotationsOffset 0
            ^:mutable ^int runtimeInvisibleTypeAnnotationsOffset 0
            ^:mutable ^int annotationDefaultOffset 0
            ^:mutable ^int methodParametersOffset 0
            ^:mutable ^Attribute attributes nil
            ^:mutable attributesCount (.readUnsignedShort this currentOffset)]
        (set! currentOffset (unchecked-add-int currentOffset 2))
        (while (> (let [old-27 attributesCount]
                    (set! attributesCount (unchecked-dec-int attributesCount))
                    old-27)
                  0)
          (let [attributeName (.readUTF8 this currentOffset charBuffer)
                attributeLength (.readInt this (unchecked-add-int currentOffset 2))]
            (set! currentOffset (unchecked-add-int currentOffset 6))
            (cond
              (.equals Constants/CODE attributeName)
                (when (== (bit-and-int (.-parsingOptions context) SKIP_CODE) 0)
                  (set! codeOffset currentOffset))
              (.equals Constants/EXCEPTIONS attributeName)
                (do
                  (set! exceptionsOffset currentOffset)
                  (set! exceptions (new String/1 (.readUnsignedShort this exceptionsOffset)))
                  (let [^:mutable currentExceptionOffset (unchecked-add-int exceptionsOffset 2)]
                    (loop [^int i 0]
                      (when (< i (alength exceptions))
                        (aset exceptions i (.readClass this currentExceptionOffset charBuffer))
                        (set! currentExceptionOffset (unchecked-add-int currentExceptionOffset 2))
                        (recur (unchecked-inc-int i))))))
              (.equals Constants/SIGNATURE attributeName)
                (set! signatureIndex (.readUnsignedShort this currentOffset))
              (.equals Constants/DEPRECATED attributeName)
                (set! (.-currentMethodAccessFlags context)
                      (bit-or-int (.-currentMethodAccessFlags context) Opcodes/ACC_DEPRECATED))
              (.equals Constants/RUNTIME_VISIBLE_ANNOTATIONS attributeName)
                (set! runtimeVisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeVisibleTypeAnnotationsOffset currentOffset)
              (.equals Constants/ANNOTATION_DEFAULT attributeName)
                (set! annotationDefaultOffset currentOffset)
              (.equals Constants/SYNTHETIC attributeName)
                (do
                  (set! synthetic true)
                  (set! (.-currentMethodAccessFlags context)
                        (bit-or-int (.-currentMethodAccessFlags context) Opcodes/ACC_SYNTHETIC)))
              (.equals Constants/RUNTIME_INVISIBLE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS attributeName)
                (set! runtimeInvisibleTypeAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS attributeName)
                (set! runtimeVisibleParameterAnnotationsOffset currentOffset)
              (.equals Constants/RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS attributeName)
                (set! runtimeInvisibleParameterAnnotationsOffset currentOffset)
              (.equals Constants/METHOD_PARAMETERS attributeName)
                (set! methodParametersOffset currentOffset)
              :else
                (let [attribute (.readAttribute this
                                                (.-attributePrototypes context)
                                                attributeName
                                                currentOffset
                                                attributeLength
                                                charBuffer
                                                -1
                                                nil)]
                  (set! (.-nextAttribute attribute) attributes)
                  (set! attributes attribute)))
            (set! currentOffset (unchecked-add-int currentOffset attributeLength))))
        (let [methodVisitor (.visitMethod classVisitor
                                          (.-currentMethodAccessFlags context)
                                          (.-currentMethodName context)
                                          (.-currentMethodDescriptor context)
                                          (when-not (== signatureIndex 0)
                                            (.readUtf this signatureIndex charBuffer))
                                          exceptions)]
          (if (nil? methodVisitor)
              currentOffset
              (do
                (when (instance? MethodWriter methodVisitor)
                  (let [methodWriter (cast MethodWriter methodVisitor)]
                    (when (.canCopyMethodAttributes
                            methodWriter
                            this
                            synthetic
                            (not (== (bit-and-int
                                       (.-currentMethodAccessFlags context)
                                       Opcodes/ACC_DEPRECATED)
                                     0))
                            (.readUnsignedShort this (unchecked-add-int methodInfoOffset 4))
                            signatureIndex
                            exceptionsOffset)
                      (.setMethodAttributesSource
                        methodWriter
                        methodInfoOffset
                        (unchecked-subtract-int currentOffset methodInfoOffset))
                      (return currentOffset))))
                (when (and (not (== methodParametersOffset 0))
                           (== (bit-and-int (.-parsingOptions context) SKIP_DEBUG) 0))
                  (let [^:mutable parametersCount (.readByte this methodParametersOffset)
                        ^:mutable currentParameterOffset (unchecked-add-int
                                                           methodParametersOffset
                                                           1)]
                    (while (> (let [old-28 parametersCount]
                                (set! parametersCount (unchecked-dec-int parametersCount))
                                old-28)
                              0)
                      (.visitParameter methodVisitor
                                       (.readUTF8 this currentParameterOffset charBuffer)
                                       (.readUnsignedShort
                                         this
                                         (unchecked-add-int currentParameterOffset 2)))
                      (set! currentParameterOffset (unchecked-add-int currentParameterOffset 4)))))
                (when-not (== annotationDefaultOffset 0)
                  (let [annotationVisitor (.visitAnnotationDefault methodVisitor)]
                    (.readElementValue this
                                       annotationVisitor
                                       annotationDefaultOffset
                                       nil
                                       charBuffer
                                       0)
                    (when (some? annotationVisitor) (.visitEnd annotationVisitor))))
                (when-not (== runtimeVisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-29 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-29)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation methodVisitor annotationDescriptor true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleAnnotationsOffset
                                                            2)]
                    (while (> (let [old-30 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-30)
                              0)
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitAnnotation methodVisitor annotationDescriptor false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeVisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeVisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeVisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-31 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-31)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  methodVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeInvisibleTypeAnnotationsOffset 0)
                  (let [^:mutable numAnnotations (.readUnsignedShort
                                                   this
                                                   runtimeInvisibleTypeAnnotationsOffset)
                        ^:mutable currentAnnotationOffset (unchecked-add-int
                                                            runtimeInvisibleTypeAnnotationsOffset
                                                            2)]
                    (while (> (let [old-32 numAnnotations]
                                (set! numAnnotations (unchecked-dec-int numAnnotations))
                                old-32)
                              0)
                      (set! currentAnnotationOffset
                            (.readTypeAnnotationTarget this context currentAnnotationOffset))
                      (let [annotationDescriptor (.readUTF8 this currentAnnotationOffset charBuffer)]
                        (set! currentAnnotationOffset (unchecked-add-int currentAnnotationOffset 2))
                        (set! currentAnnotationOffset
                              (.readElementValues
                                this
                                (.visitTypeAnnotation
                                  methodVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0))))))
                (when-not (== runtimeVisibleParameterAnnotationsOffset 0)
                  (.readParameterAnnotations this
                                             methodVisitor
                                             context
                                             runtimeVisibleParameterAnnotationsOffset
                                             true))
                (when-not (== runtimeInvisibleParameterAnnotationsOffset 0)
                  (.readParameterAnnotations this
                                             methodVisitor
                                             context
                                             runtimeInvisibleParameterAnnotationsOffset
                                             false))
                (while (some? attributes)
                  (let [nextAttribute (.-nextAttribute attributes)]
                    (set! (.-nextAttribute attributes) nil)
                    (.visitAttribute methodVisitor attributes)
                    (set! attributes nextAttribute)))
                (when-not (== codeOffset 0)
                  (.visitCode methodVisitor)
                  (.readCode this methodVisitor context codeOffset))
                (.visitEnd methodVisitor)
                currentOffset))))))

  (method ^:private readCode ^void [this ^:final ^MethodVisitor methodVisitor
                                    ^:final ^Context context ^:final ^int codeOffset]
    (let [^:mutable currentOffset codeOffset
          classBuffer classFileBuffer
          charBuffer (.-charBuffer context)
          maxStack (.readUnsignedShort this currentOffset)
          maxLocals (.readUnsignedShort this (unchecked-add-int currentOffset 2))
          codeLength (.readInt this (unchecked-add-int currentOffset 4))]
      (set! currentOffset (unchecked-add-int currentOffset 8))
      (when (or (> codeLength 65535)
                (> codeLength (unchecked-subtract-int (alength classFileBuffer) currentOffset)))
        (throw (IllegalArgumentException.)))
      (let [bytecodeStartOffset currentOffset
            bytecodeEndOffset (unchecked-add-int currentOffset codeLength)
            labels (set! (.-currentMethodLabels context)
                         (new Label/1 (unchecked-add-int codeLength 1)))]
        (while (< currentOffset bytecodeEndOffset)
          (let [bytecodeOffset (unchecked-subtract-int currentOffset bytecodeStartOffset)
                opcode (bit-and-int (aget classBuffer currentOffset) 0xFF)]
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
                (set! currentOffset (unchecked-add-int currentOffset 1))
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
                (do
                  (.createLabel this
                                (unchecked-add-int
                                  bytecodeOffset
                                  (.readShort this (unchecked-add-int currentOffset 1)))
                                labels)
                  (set! currentOffset (unchecked-add-int currentOffset 3)))
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
                  (.createLabel this
                                (unchecked-add-int
                                  bytecodeOffset
                                  (.readUnsignedShort this (unchecked-add-int currentOffset 1)))
                                labels)
                  (set! currentOffset (unchecked-add-int currentOffset 3)))
              (Constants/GOTO_W Constants/JSR_W Constants/ASM_GOTO_W)
                (do
                  (.createLabel this
                                (unchecked-add-int
                                  bytecodeOffset
                                  (.readInt this (unchecked-add-int currentOffset 1)))
                                labels)
                  (set! currentOffset (unchecked-add-int currentOffset 5)))
              Constants/WIDE
                (switch (bit-and-int (aget classBuffer (unchecked-add-int currentOffset 1)) 0xFF)
                  (Opcodes/ILOAD Opcodes/FLOAD
                                 Opcodes/ALOAD
                                 Opcodes/LLOAD
                                 Opcodes/DLOAD
                                 Opcodes/ISTORE
                                 Opcodes/FSTORE
                                 Opcodes/ASTORE
                                 Opcodes/LSTORE
                                 Opcodes/DSTORE
                                 Opcodes/RET)
                    (set! currentOffset (unchecked-add-int currentOffset 4))
                  Opcodes/IINC (set! currentOffset (unchecked-add-int currentOffset 6))
                  (throw (IllegalArgumentException.)))
              Opcodes/TABLESWITCH
                (do
                  (set! currentOffset
                        (unchecked-add-int
                          currentOffset
                          (unchecked-subtract-int 4 (bit-and-int bytecodeOffset 3))))
                  (.createLabel this
                                (unchecked-add-int bytecodeOffset (.readInt this currentOffset))
                                labels)
                  (let [^:mutable numTableEntries (unchecked-add-int
                                                    (unchecked-subtract-int
                                                      (.readInt
                                                        this
                                                        (unchecked-add-int currentOffset 8))
                                                      (.readInt
                                                        this
                                                        (unchecked-add-int currentOffset 4)))
                                                    1)]
                    (set! currentOffset (unchecked-add-int currentOffset 12))
                    (while (> (let [old-33 numTableEntries]
                                (set! numTableEntries (unchecked-dec-int numTableEntries))
                                old-33)
                              0)
                      (.createLabel this
                                    (unchecked-add-int bytecodeOffset (.readInt this currentOffset))
                                    labels)
                      (set! currentOffset (unchecked-add-int currentOffset 4)))))
              Opcodes/LOOKUPSWITCH
                (do
                  (set! currentOffset
                        (unchecked-add-int
                          currentOffset
                          (unchecked-subtract-int 4 (bit-and-int bytecodeOffset 3))))
                  (.createLabel this
                                (unchecked-add-int bytecodeOffset (.readInt this currentOffset))
                                labels)
                  (let [^:mutable numSwitchCases (.readInt this (unchecked-add-int currentOffset 4))]
                    (set! currentOffset (unchecked-add-int currentOffset 8))
                    (while (> (let [old-34 numSwitchCases]
                                (set! numSwitchCases (unchecked-dec-int numSwitchCases))
                                old-34)
                              0)
                      (.createLabel this
                                    (unchecked-add-int
                                      bytecodeOffset
                                      (.readInt this (unchecked-add-int currentOffset 4)))
                                    labels)
                      (set! currentOffset (unchecked-add-int currentOffset 8)))))
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
                (set! currentOffset (unchecked-add-int currentOffset 2))
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
                (set! currentOffset (unchecked-add-int currentOffset 3))
              (Opcodes/INVOKEINTERFACE Opcodes/INVOKEDYNAMIC)
                (set! currentOffset (unchecked-add-int currentOffset 5))
              Opcodes/MULTIANEWARRAY (set! currentOffset (unchecked-add-int currentOffset 4))
              (throw (IllegalArgumentException.)))))
        (let [^:mutable exceptionTableLength (.readUnsignedShort this currentOffset)]
          (set! currentOffset (unchecked-add-int currentOffset 2))
          (while (> (let [old-35 exceptionTableLength]
                      (set! exceptionTableLength (unchecked-dec-int exceptionTableLength))
                      old-35)
                    0)
            (let [start (.createLabel this (.readUnsignedShort this currentOffset) labels)
                  end (.createLabel this
                                    (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                                    labels)
                  handler (.createLabel this
                                        (.readUnsignedShort
                                          this
                                          (unchecked-add-int currentOffset 4))
                                        labels)
                  catchType (.readUTF8 this
                                       (aget cpInfoOffsets
                                             (.readUnsignedShort
                                               this
                                               (unchecked-add-int currentOffset 6)))
                                       charBuffer)]
              (set! currentOffset (unchecked-add-int currentOffset 8))
              (.visitTryCatchBlock methodVisitor start end handler catchType)))
          (let [^:mutable ^int stackMapFrameOffset 0
                ^:mutable ^int stackMapTableEndOffset 0
                ^:mutable compressedFrames true
                ^:mutable ^int localVariableTableOffset 0
                ^:mutable ^int localVariableTypeTableOffset 0
                ^:mutable ^int/1 visibleTypeAnnotationOffsets nil
                ^:mutable ^int/1 invisibleTypeAnnotationOffsets nil
                ^:mutable ^Attribute attributes nil
                ^:mutable attributesCount (.readUnsignedShort this currentOffset)]
            (set! currentOffset (unchecked-add-int currentOffset 2))
            (while (> (let [old-36 attributesCount]
                        (set! attributesCount (unchecked-dec-int attributesCount))
                        old-36)
                      0)
              (let [attributeName (.readUTF8 this currentOffset charBuffer)
                    attributeLength (.readInt this (unchecked-add-int currentOffset 2))]
                (set! currentOffset (unchecked-add-int currentOffset 6))
                (cond
                  (.equals Constants/LOCAL_VARIABLE_TABLE attributeName)
                    (when (== (bit-and-int (.-parsingOptions context) SKIP_DEBUG) 0)
                      (set! localVariableTableOffset currentOffset)
                      (let [^:mutable currentLocalVariableTableOffset currentOffset
                            ^:mutable localVariableTableLength (.readUnsignedShort
                                                                 this
                                                                 currentLocalVariableTableOffset)]
                        (set! currentLocalVariableTableOffset
                              (unchecked-add-int currentLocalVariableTableOffset 2))
                        (while (> (let [old-37 localVariableTableLength]
                                    (set! localVariableTableLength
                                          (unchecked-dec-int localVariableTableLength))
                                    old-37)
                                  0)
                          (let [startPc (.readUnsignedShort this currentLocalVariableTableOffset)]
                            (.createDebugLabel this startPc labels)
                            (let [length (.readUnsignedShort
                                           this
                                           (unchecked-add-int currentLocalVariableTableOffset 2))]
                              (.createDebugLabel this (unchecked-add-int startPc length) labels)
                              (set! currentLocalVariableTableOffset
                                    (unchecked-add-int currentLocalVariableTableOffset 10)))))))
                  (.equals Constants/LOCAL_VARIABLE_TYPE_TABLE attributeName)
                    (set! localVariableTypeTableOffset currentOffset)
                  (.equals Constants/LINE_NUMBER_TABLE attributeName)
                    (when (== (bit-and-int (.-parsingOptions context) SKIP_DEBUG) 0)
                      (let [^:mutable currentLineNumberTableOffset currentOffset
                            ^:mutable lineNumberTableLength (.readUnsignedShort
                                                              this
                                                              currentLineNumberTableOffset)]
                        (set! currentLineNumberTableOffset
                              (unchecked-add-int currentLineNumberTableOffset 2))
                        (while (> (let [old-38 lineNumberTableLength]
                                    (set! lineNumberTableLength
                                          (unchecked-dec-int lineNumberTableLength))
                                    old-38)
                                  0)
                          (let [startPc (.readUnsignedShort this currentLineNumberTableOffset)
                                lineNumber (.readUnsignedShort
                                             this
                                             (unchecked-add-int currentLineNumberTableOffset 2))]
                            (set! currentLineNumberTableOffset
                                  (unchecked-add-int currentLineNumberTableOffset 4))
                            (.createDebugLabel this startPc labels)
                            (.addLineNumber (aget labels startPc) lineNumber)))))
                  (.equals Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS attributeName)
                    (set! visibleTypeAnnotationOffsets
                          (.readTypeAnnotations this methodVisitor context currentOffset true))
                  (.equals Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS attributeName)
                    (set! invisibleTypeAnnotationOffsets
                          (.readTypeAnnotations this methodVisitor context currentOffset false))
                  (.equals Constants/STACK_MAP_TABLE attributeName)
                    (when (== (bit-and-int (.-parsingOptions context) SKIP_FRAMES) 0)
                      (set! stackMapFrameOffset (unchecked-add-int currentOffset 2))
                      (set! stackMapTableEndOffset
                            (unchecked-add-int currentOffset attributeLength)))
                  (.equals "StackMap" attributeName)
                    (when (== (bit-and-int (.-parsingOptions context) SKIP_FRAMES) 0)
                      (set! stackMapFrameOffset (unchecked-add-int currentOffset 2))
                      (set! stackMapTableEndOffset
                            (unchecked-add-int currentOffset attributeLength))
                      (set! compressedFrames false))
                  :else
                    (let [attribute (.readAttribute
                                      this
                                      (.-attributePrototypes context)
                                      attributeName
                                      currentOffset
                                      attributeLength
                                      charBuffer
                                      codeOffset
                                      labels)]
                      (set! (.-nextAttribute attribute) attributes)
                      (set! attributes attribute)))
                (set! currentOffset (unchecked-add-int currentOffset attributeLength))))
            (let [expandFrames (not (== (bit-and-int (.-parsingOptions context) EXPAND_FRAMES) 0))]
              (when-not (== stackMapFrameOffset 0)
                (set! (.-currentFrameOffset context) -1)
                (set! (.-currentFrameType context) 0)
                (set! (.-currentFrameLocalCount context) 0)
                (set! (.-currentFrameLocalCountDelta context) 0)
                (set! (.-currentFrameLocalTypes context) (new Object/1 maxLocals))
                (set! (.-currentFrameStackCount context) 0)
                (set! (.-currentFrameStackTypes context) (new Object/1 maxStack))
                (when expandFrames (.computeImplicitFrame this context))
                (loop [^int offset stackMapFrameOffset]
                  (if (< offset (unchecked-subtract-int stackMapTableEndOffset 2))
                      (if (== (aget classBuffer offset) Frame/ITEM_UNINITIALIZED)
                          (let [potentialBytecodeOffset (.readUnsignedShort
                                                          this
                                                          (unchecked-add-int offset 1))]
                            (if (and (and (>= potentialBytecodeOffset 0)
                                          (< potentialBytecodeOffset codeLength))
                                     (== (bit-and-int
                                           (aget
                                             classBuffer
                                             (unchecked-add-int
                                               bytecodeStartOffset
                                               potentialBytecodeOffset))
                                           0xFF)
                                         Opcodes/NEW))
                                (do
                                  (.createLabel this potentialBytecodeOffset labels)
                                  (recur (unchecked-inc-int offset)))
                                (recur (unchecked-inc-int offset))))
                          (recur (unchecked-inc-int offset)))
                      nil)))
              (when (and expandFrames
                         (not (== (bit-and-int (.-parsingOptions context) EXPAND_ASM_INSNS) 0)))
                (.visitFrame methodVisitor Opcodes/F_NEW maxLocals nil 0 nil))
              (let [^:mutable ^int currentVisibleTypeAnnotationIndex 0
                    ^:mutable currentVisibleTypeAnnotationBytecodeOffset
                      (.getTypeAnnotationBytecodeOffset this visibleTypeAnnotationOffsets 0)
                    ^:mutable ^int currentInvisibleTypeAnnotationIndex 0
                    ^:mutable currentInvisibleTypeAnnotationBytecodeOffset
                      (.getTypeAnnotationBytecodeOffset this invisibleTypeAnnotationOffsets 0)
                    ^:mutable insertFrame false
                    wideJumpOpcodeDelta (if (== (bit-and-int
                                                  (.-parsingOptions context)
                                                  EXPAND_ASM_INSNS)
                                                0)
                                            Constants/WIDE_JUMP_OPCODE_DELTA
                                            0)]
                (set! currentOffset bytecodeStartOffset)
                (while (< currentOffset bytecodeEndOffset)
                  (let [currentBytecodeOffset (unchecked-subtract-int
                                                currentOffset
                                                bytecodeStartOffset)]
                    (.readBytecodeInstructionOffset this currentBytecodeOffset)
                    (let [currentLabel (aget labels currentBytecodeOffset)]
                      (when (some? currentLabel)
                        (.accept currentLabel
                                 methodVisitor
                                 (== (bit-and-int (.-parsingOptions context) SKIP_DEBUG) 0)))
                      (while (and (not (== stackMapFrameOffset 0))
                                  (or (== (.-currentFrameOffset context) currentBytecodeOffset)
                                      (== (.-currentFrameOffset context) -1)))
                        (when-not (== (.-currentFrameOffset context) -1)
                          (if (or (not compressedFrames) expandFrames)
                              (.visitFrame methodVisitor
                                           Opcodes/F_NEW
                                           (.-currentFrameLocalCount context)
                                           (.-currentFrameLocalTypes context)
                                           (.-currentFrameStackCount context)
                                           (.-currentFrameStackTypes context))
                              (.visitFrame methodVisitor
                                           (.-currentFrameType context)
                                           (.-currentFrameLocalCountDelta context)
                                           (.-currentFrameLocalTypes context)
                                           (.-currentFrameStackCount context)
                                           (.-currentFrameStackTypes context)))
                          (set! insertFrame false))
                        (if (< stackMapFrameOffset stackMapTableEndOffset)
                            (set! stackMapFrameOffset
                                  (.readStackMapFrame
                                    this
                                    stackMapFrameOffset
                                    compressedFrames
                                    expandFrames
                                    context))
                            (set! stackMapFrameOffset 0)))
                      (when insertFrame
                        (when-not (== (bit-and-int (.-parsingOptions context) EXPAND_FRAMES) 0)
                          (.visitFrame methodVisitor Constants/F_INSERT 0 nil 0 nil))
                        (set! insertFrame false))
                      (let [^:mutable opcode (bit-and-int (aget classBuffer currentOffset) 0xFF)]
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
                                       Opcodes/MONITOREXIT)
                            (do
                              (.visitInsn methodVisitor opcode)
                              (set! currentOffset (unchecked-add-int currentOffset 1)))
                          (Constants/ILOAD_0 Constants/ILOAD_1
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
                                             Constants/ALOAD_3)
                            (do
                              (set! opcode (unchecked-subtract-int opcode Constants/ILOAD_0))
                              (.visitVarInsn methodVisitor
                                             (unchecked-add-int
                                               Opcodes/ILOAD
                                               (bit-shift-right-int opcode 2))
                                             (bit-and-int opcode 0x3))
                              (set! currentOffset (unchecked-add-int currentOffset 1)))
                          (Constants/ISTORE_0 Constants/ISTORE_1
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
                            (do
                              (set! opcode (unchecked-subtract-int opcode Constants/ISTORE_0))
                              (.visitVarInsn methodVisitor
                                             (unchecked-add-int
                                               Opcodes/ISTORE
                                               (bit-shift-right-int opcode 2))
                                             (bit-and-int opcode 0x3))
                              (set! currentOffset (unchecked-add-int currentOffset 1)))
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
                            (do
                              (.visitJumpInsn methodVisitor
                                              opcode
                                              (aget
                                                labels
                                                (unchecked-add-int
                                                  currentBytecodeOffset
                                                  (.readShort
                                                    this
                                                    (unchecked-add-int currentOffset 1)))))
                              (set! currentOffset (unchecked-add-int currentOffset 3)))
                          (Constants/GOTO_W Constants/JSR_W)
                            (do
                              (.visitJumpInsn methodVisitor
                                              (unchecked-subtract-int opcode wideJumpOpcodeDelta)
                                              (aget
                                                labels
                                                (unchecked-add-int
                                                  currentBytecodeOffset
                                                  (.readInt
                                                    this
                                                    (unchecked-add-int currentOffset 1)))))
                              (set! currentOffset (unchecked-add-int currentOffset 5)))
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
                              (set! opcode
                                    (if (< opcode Constants/ASM_IFNULL)
                                        (unchecked-subtract-int opcode Constants/ASM_OPCODE_DELTA)
                                        (unchecked-subtract-int
                                          opcode
                                          Constants/ASM_IFNULL_OPCODE_DELTA)))
                              (let [target (aget
                                             labels
                                             (unchecked-add-int
                                               currentBytecodeOffset
                                               (.readUnsignedShort
                                                 this
                                                 (unchecked-add-int currentOffset 1))))]
                                (if (or (== opcode Opcodes/GOTO) (== opcode Opcodes/JSR))
                                    (.visitJumpInsn
                                      methodVisitor
                                      (unchecked-add-int opcode Constants/WIDE_JUMP_OPCODE_DELTA)
                                      target)
                                    (do
                                      (set! opcode
                                            (if (< opcode Opcodes/GOTO)
                                                (unchecked-subtract-int
                                                  (bit-xor-int (unchecked-add-int opcode 1) 1)
                                                  1)
                                                (bit-xor-int opcode 1)))
                                      (let [endif (.createLabel
                                                    this
                                                    (unchecked-add-int currentBytecodeOffset 3)
                                                    labels)]
                                        (.visitJumpInsn methodVisitor opcode endif)
                                        (.visitJumpInsn methodVisitor Constants/GOTO_W target)
                                        (set! insertFrame true))))
                                (set! currentOffset (unchecked-add-int currentOffset 3))))
                          Constants/ASM_GOTO_W
                            (do
                              (.visitJumpInsn methodVisitor
                                              Constants/GOTO_W
                                              (aget
                                                labels
                                                (unchecked-add-int
                                                  currentBytecodeOffset
                                                  (.readInt
                                                    this
                                                    (unchecked-add-int currentOffset 1)))))
                              (set! insertFrame true)
                              (set! currentOffset (unchecked-add-int currentOffset 5)))
                          Constants/WIDE
                            (do
                              (set! opcode
                                    (bit-and-int
                                      (aget classBuffer (unchecked-add-int currentOffset 1))
                                      0xFF))
                              (if (== opcode Opcodes/IINC)
                                  (do
                                    (.visitIincInsn
                                      methodVisitor
                                      (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                                      (.readShort this (unchecked-add-int currentOffset 4)))
                                    (set! currentOffset (unchecked-add-int currentOffset 6)))
                                  (do
                                    (.visitVarInsn
                                      methodVisitor
                                      opcode
                                      (.readUnsignedShort this (unchecked-add-int currentOffset 2)))
                                    (set! currentOffset (unchecked-add-int currentOffset 4)))))
                          Opcodes/TABLESWITCH
                            (do
                              (set! currentOffset
                                    (unchecked-add-int
                                      currentOffset
                                      (unchecked-subtract-int
                                        4
                                        (bit-and-int currentBytecodeOffset 3))))
                              (let [defaultLabel (aget
                                                   labels
                                                   (unchecked-add-int
                                                     currentBytecodeOffset
                                                     (.readInt this currentOffset)))
                                    low (.readInt this (unchecked-add-int currentOffset 4))
                                    high (.readInt this (unchecked-add-int currentOffset 8))]
                                (set! currentOffset (unchecked-add-int currentOffset 12))
                                (let [table (new
                                              Label/1
                                              (unchecked-add-int
                                                (unchecked-subtract-int high low)
                                                1))]
                                  (loop [^int i 0]
                                    (when (< i (alength table))
                                      (aset table
                                            i
                                            (aget
                                              labels
                                              (unchecked-add-int
                                                currentBytecodeOffset
                                                (.readInt this currentOffset))))
                                      (set! currentOffset (unchecked-add-int currentOffset 4))
                                      (recur (unchecked-inc-int i))))
                                  (.visitTableSwitchInsn methodVisitor low high defaultLabel table))))
                          Opcodes/LOOKUPSWITCH
                            (do
                              (set! currentOffset
                                    (unchecked-add-int
                                      currentOffset
                                      (unchecked-subtract-int
                                        4
                                        (bit-and-int currentBytecodeOffset 3))))
                              (let [defaultLabel (aget
                                                   labels
                                                   (unchecked-add-int
                                                     currentBytecodeOffset
                                                     (.readInt this currentOffset)))
                                    numPairs (.readInt this (unchecked-add-int currentOffset 4))]
                                (set! currentOffset (unchecked-add-int currentOffset 8))
                                (let [keys (new int/1 numPairs)
                                      values (new Label/1 numPairs)]
                                  (loop [^int i 0]
                                    (when (< i numPairs)
                                      (aset keys i (.readInt this currentOffset))
                                      (aset
                                        values
                                        i
                                        (aget
                                          labels
                                          (unchecked-add-int
                                            currentBytecodeOffset
                                            (.readInt this (unchecked-add-int currentOffset 4)))))
                                      (set! currentOffset (unchecked-add-int currentOffset 8))
                                      (recur (unchecked-inc-int i))))
                                  (.visitLookupSwitchInsn methodVisitor defaultLabel keys values))))
                          (Opcodes/ILOAD Opcodes/LLOAD
                                         Opcodes/FLOAD
                                         Opcodes/DLOAD
                                         Opcodes/ALOAD
                                         Opcodes/ISTORE
                                         Opcodes/LSTORE
                                         Opcodes/FSTORE
                                         Opcodes/DSTORE
                                         Opcodes/ASTORE
                                         Opcodes/RET)
                            (do
                              (.visitVarInsn methodVisitor
                                             opcode
                                             (bit-and-int
                                               (aget
                                                 classBuffer
                                                 (unchecked-add-int currentOffset 1))
                                               0xFF))
                              (set! currentOffset (unchecked-add-int currentOffset 2)))
                          (Opcodes/BIPUSH Opcodes/NEWARRAY)
                            (do
                              (.visitIntInsn methodVisitor
                                             opcode
                                             (aget classBuffer (unchecked-add-int currentOffset 1)))
                              (set! currentOffset (unchecked-add-int currentOffset 2)))
                          Opcodes/SIPUSH
                            (do
                              (.visitIntInsn methodVisitor
                                             opcode
                                             (.readShort this (unchecked-add-int currentOffset 1)))
                              (set! currentOffset (unchecked-add-int currentOffset 3)))
                          Opcodes/LDC
                            (do
                              (.visitLdcInsn methodVisitor
                                             (.readConst
                                               this
                                               (bit-and-int
                                                 (aget
                                                   classBuffer
                                                   (unchecked-add-int currentOffset 1))
                                                 0xFF)
                                               charBuffer))
                              (set! currentOffset (unchecked-add-int currentOffset 2)))
                          (Constants/LDC_W Constants/LDC2_W)
                            (do
                              (.visitLdcInsn methodVisitor
                                             (.readConst
                                               this
                                               (.readUnsignedShort
                                                 this
                                                 (unchecked-add-int currentOffset 1))
                                               charBuffer))
                              (set! currentOffset (unchecked-add-int currentOffset 3)))
                          (Opcodes/GETSTATIC Opcodes/PUTSTATIC
                                             Opcodes/GETFIELD
                                             Opcodes/PUTFIELD
                                             Opcodes/INVOKEVIRTUAL
                                             Opcodes/INVOKESPECIAL
                                             Opcodes/INVOKESTATIC
                                             Opcodes/INVOKEINTERFACE)
                            (let [cpInfoOffset (aget
                                                 cpInfoOffsets
                                                 (.readUnsignedShort
                                                   this
                                                   (unchecked-add-int currentOffset 1)))
                                  nameAndTypeCpInfoOffset (aget
                                                            cpInfoOffsets
                                                            (.readUnsignedShort
                                                              this
                                                              (unchecked-add-int cpInfoOffset 2)))
                                  owner (.readClass this cpInfoOffset charBuffer)
                                  name (.readUTF8 this nameAndTypeCpInfoOffset charBuffer)
                                  descriptor (.readUTF8
                                               this
                                               (unchecked-add-int nameAndTypeCpInfoOffset 2)
                                               charBuffer)]
                              (if (< opcode Opcodes/INVOKEVIRTUAL)
                                  (.visitFieldInsn methodVisitor opcode owner name descriptor)
                                  (let [isInterface (==
                                                      (aget
                                                        classBuffer
                                                        (unchecked-subtract-int cpInfoOffset 1))
                                                      Symbol/CONSTANT_INTERFACE_METHODREF_TAG)]
                                    (.visitMethodInsn
                                      methodVisitor
                                      opcode
                                      owner
                                      name
                                      descriptor
                                      isInterface)))
                              (if (== opcode Opcodes/INVOKEINTERFACE)
                                  (set! currentOffset (unchecked-add-int currentOffset 5))
                                  (set! currentOffset (unchecked-add-int currentOffset 3))))
                          Opcodes/INVOKEDYNAMIC
                            (let [cpInfoOffset (aget
                                                 cpInfoOffsets
                                                 (.readUnsignedShort
                                                   this
                                                   (unchecked-add-int currentOffset 1)))
                                  nameAndTypeCpInfoOffset (aget
                                                            cpInfoOffsets
                                                            (.readUnsignedShort
                                                              this
                                                              (unchecked-add-int cpInfoOffset 2)))
                                  name (.readUTF8 this nameAndTypeCpInfoOffset charBuffer)
                                  descriptor (.readUTF8
                                               this
                                               (unchecked-add-int nameAndTypeCpInfoOffset 2)
                                               charBuffer)
                                  ^:mutable bootstrapMethodOffset (aget
                                                                    bootstrapMethodOffsets
                                                                    (.readUnsignedShort
                                                                      this
                                                                      cpInfoOffset))
                                  handle (cast Handle
                                               (.readConst
                                                 this
                                                 (.readUnsignedShort this bootstrapMethodOffset)
                                                 charBuffer))
                                  bootstrapMethodArguments (new
                                                             Object/1
                                                             (.readUnsignedShort
                                                               this
                                                               (unchecked-add-int
                                                                 bootstrapMethodOffset
                                                                 2)))]
                              (set! bootstrapMethodOffset
                                    (unchecked-add-int bootstrapMethodOffset 4))
                              (loop [^int i 0]
                                (when (< i (alength bootstrapMethodArguments))
                                  (aset bootstrapMethodArguments
                                        i
                                        (.readConst
                                          this
                                          (.readUnsignedShort this bootstrapMethodOffset)
                                          charBuffer))
                                  (set! bootstrapMethodOffset
                                        (unchecked-add-int bootstrapMethodOffset 2))
                                  (recur (unchecked-inc-int i))))
                              (.visitInvokeDynamicInsn
                                methodVisitor
                                name
                                descriptor
                                handle
                                bootstrapMethodArguments)
                              (set! currentOffset (unchecked-add-int currentOffset 5)))
                          (Opcodes/NEW Opcodes/ANEWARRAY Opcodes/CHECKCAST Opcodes/INSTANCEOF)
                            (do
                              (.visitTypeInsn methodVisitor
                                              opcode
                                              (.readClass
                                                this
                                                (unchecked-add-int currentOffset 1)
                                                charBuffer))
                              (set! currentOffset (unchecked-add-int currentOffset 3)))
                          Opcodes/IINC
                            (do
                              (.visitIincInsn
                                methodVisitor
                                (bit-and-int (aget classBuffer (unchecked-add-int currentOffset 1))
                                             0xFF)
                                (aget classBuffer (unchecked-add-int currentOffset 2)))
                              (set! currentOffset (unchecked-add-int currentOffset 3)))
                          Opcodes/MULTIANEWARRAY
                            (do
                              (.visitMultiANewArrayInsn
                                methodVisitor
                                (.readClass this (unchecked-add-int currentOffset 1) charBuffer)
                                (bit-and-int (aget classBuffer (unchecked-add-int currentOffset 3))
                                             0xFF))
                              (set! currentOffset (unchecked-add-int currentOffset 4)))
                          (throw (AssertionError.)))
                        (while (and (and (some? visibleTypeAnnotationOffsets)
                                         (< currentVisibleTypeAnnotationIndex
                                            (alength visibleTypeAnnotationOffsets)))
                                    (<= currentVisibleTypeAnnotationBytecodeOffset
                                        currentBytecodeOffset))
                          (when (== currentVisibleTypeAnnotationBytecodeOffset
                                    currentBytecodeOffset)
                            (let [^:mutable currentAnnotationOffset (.readTypeAnnotationTarget
                                                                      this
                                                                      context
                                                                      (aget
                                                                        visibleTypeAnnotationOffsets
                                                                        currentVisibleTypeAnnotationIndex))
                                  annotationDescriptor (.readUTF8
                                                         this
                                                         currentAnnotationOffset
                                                         charBuffer)]
                              (set! currentAnnotationOffset
                                    (unchecked-add-int currentAnnotationOffset 2))
                              (.readElementValues
                                this
                                (.visitInsnAnnotation
                                  methodVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  true)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0)))
                          (set! currentVisibleTypeAnnotationBytecodeOffset
                                (.getTypeAnnotationBytecodeOffset
                                  this
                                  visibleTypeAnnotationOffsets
                                  (set! currentVisibleTypeAnnotationIndex
                                        (unchecked-inc-int currentVisibleTypeAnnotationIndex)))))
                        (while (and (and (some? invisibleTypeAnnotationOffsets)
                                         (< currentInvisibleTypeAnnotationIndex
                                            (alength invisibleTypeAnnotationOffsets)))
                                    (<= currentInvisibleTypeAnnotationBytecodeOffset
                                        currentBytecodeOffset))
                          (when (== currentInvisibleTypeAnnotationBytecodeOffset
                                    currentBytecodeOffset)
                            (let [^:mutable currentAnnotationOffset (.readTypeAnnotationTarget
                                                                      this
                                                                      context
                                                                      (aget
                                                                        invisibleTypeAnnotationOffsets
                                                                        currentInvisibleTypeAnnotationIndex))
                                  annotationDescriptor (.readUTF8
                                                         this
                                                         currentAnnotationOffset
                                                         charBuffer)]
                              (set! currentAnnotationOffset
                                    (unchecked-add-int currentAnnotationOffset 2))
                              (.readElementValues
                                this
                                (.visitInsnAnnotation
                                  methodVisitor
                                  (.-currentTypeAnnotationTarget context)
                                  (.-currentTypeAnnotationTargetPath context)
                                  annotationDescriptor
                                  false)
                                currentAnnotationOffset
                                true
                                charBuffer
                                0)))
                          (set! currentInvisibleTypeAnnotationBytecodeOffset
                                (.getTypeAnnotationBytecodeOffset
                                  this
                                  invisibleTypeAnnotationOffsets
                                  (set! currentInvisibleTypeAnnotationIndex
                                        (unchecked-inc-int currentInvisibleTypeAnnotationIndex)))))))))
                (when (some? (aget labels codeLength))
                  (.visitLabel methodVisitor (aget labels codeLength)))
                (when (and (not (== localVariableTableOffset 0))
                           (== (bit-and-int (.-parsingOptions context) SKIP_DEBUG) 0))
                  (let [^:mutable ^int/1 typeTable nil]
                    (when-not (== localVariableTypeTableOffset 0)
                      (set! typeTable
                            (new int/1
                                 (unchecked-multiply-int
                                   (.readUnsignedShort this localVariableTypeTableOffset)
                                   3)))
                      (set! currentOffset (unchecked-add-int localVariableTypeTableOffset 2))
                      (let [^:mutable typeTableIndex (alength typeTable)]
                        (while (> typeTableIndex 0)
                          (aset typeTable
                                (set! typeTableIndex (unchecked-dec-int typeTableIndex))
                                (unchecked-add-int currentOffset 6))
                          (aset typeTable
                                (set! typeTableIndex (unchecked-dec-int typeTableIndex))
                                (.readUnsignedShort this (unchecked-add-int currentOffset 8)))
                          (aset typeTable
                                (set! typeTableIndex (unchecked-dec-int typeTableIndex))
                                (.readUnsignedShort this currentOffset))
                          (set! currentOffset (unchecked-add-int currentOffset 10)))))
                    (let [^:mutable localVariableTableLength (.readUnsignedShort
                                                               this
                                                               localVariableTableOffset)]
                      (set! currentOffset (unchecked-add-int localVariableTableOffset 2))
                      (while (> (let [old-39 localVariableTableLength]
                                  (set! localVariableTableLength
                                        (unchecked-dec-int localVariableTableLength))
                                  old-39)
                                0)
                        (let [startPc (.readUnsignedShort this currentOffset)
                              length (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                              name (.readUTF8 this (unchecked-add-int currentOffset 4) charBuffer)
                              descriptor (.readUTF8
                                           this
                                           (unchecked-add-int currentOffset 6)
                                           charBuffer)
                              index (.readUnsignedShort this (unchecked-add-int currentOffset 8))]
                          (set! currentOffset (unchecked-add-int currentOffset 10))
                          (let [^:mutable ^String signature nil]
                            (when (some? typeTable)
                              (loop [^int i 0]
                                (if (< i (alength typeTable))
                                    (if (and (== (aget typeTable i) startPc)
                                             (== (aget typeTable (unchecked-add-int i 1)) index))
                                        (set! signature
                                              (.readUTF8
                                                this
                                                (aget typeTable (unchecked-add-int i 2))
                                                charBuffer))
                                        (recur (unchecked-add-int i 3)))
                                    nil)))
                            (.visitLocalVariable
                              methodVisitor
                              name
                              descriptor
                              signature
                              (aget labels startPc)
                              (aget labels (unchecked-add-int startPc length))
                              index)))))))
                (when (some? visibleTypeAnnotationOffsets)
                  (for-each [^int typeAnnotationOffset visibleTypeAnnotationOffsets]
                    (let [targetType (.readByte this typeAnnotationOffset)]
                      (when (or (== targetType TypeReference/LOCAL_VARIABLE)
                                (== targetType TypeReference/RESOURCE_VARIABLE))
                        (set! currentOffset
                              (.readTypeAnnotationTarget this context typeAnnotationOffset))
                        (let [annotationDescriptor (.readUTF8 this currentOffset charBuffer)]
                          (set! currentOffset (unchecked-add-int currentOffset 2))
                          (.readElementValues this
                                              (.visitLocalVariableAnnotation
                                                methodVisitor
                                                (.-currentTypeAnnotationTarget context)
                                                (.-currentTypeAnnotationTargetPath context)
                                                (.-currentLocalVariableAnnotationRangeStarts
                                                  context)
                                                (.-currentLocalVariableAnnotationRangeEnds context)
                                                (.-currentLocalVariableAnnotationRangeIndices
                                                  context)
                                                annotationDescriptor
                                                true)
                                              currentOffset
                                              true
                                              charBuffer
                                              0))))))
                (when (some? invisibleTypeAnnotationOffsets)
                  (for-each [^int typeAnnotationOffset invisibleTypeAnnotationOffsets]
                    (let [targetType (.readByte this typeAnnotationOffset)]
                      (when (or (== targetType TypeReference/LOCAL_VARIABLE)
                                (== targetType TypeReference/RESOURCE_VARIABLE))
                        (set! currentOffset
                              (.readTypeAnnotationTarget this context typeAnnotationOffset))
                        (let [annotationDescriptor (.readUTF8 this currentOffset charBuffer)]
                          (set! currentOffset (unchecked-add-int currentOffset 2))
                          (.readElementValues this
                                              (.visitLocalVariableAnnotation
                                                methodVisitor
                                                (.-currentTypeAnnotationTarget context)
                                                (.-currentTypeAnnotationTargetPath context)
                                                (.-currentLocalVariableAnnotationRangeStarts
                                                  context)
                                                (.-currentLocalVariableAnnotationRangeEnds context)
                                                (.-currentLocalVariableAnnotationRangeIndices
                                                  context)
                                                annotationDescriptor
                                                false)
                                              currentOffset
                                              true
                                              charBuffer
                                              0))))))
                (while (some? attributes)
                  (let [nextAttribute (.-nextAttribute attributes)]
                    (set! (.-nextAttribute attributes) nil)
                    (.visitAttribute methodVisitor attributes)
                    (set! attributes nextAttribute)))
                (.visitMaxs methodVisitor maxStack maxLocals))))))))

  (method ^:protected readBytecodeInstructionOffset ^void [this ^:final ^int bytecodeOffset])

  (method ^:protected readLabel ^Label [this ^:final ^int bytecodeOffset ^:final ^Label/1 labels]
    (when (nil? (aget labels bytecodeOffset)) (aset labels bytecodeOffset (Label.)))
    (aget labels bytecodeOffset))

  (method ^:private createLabel ^Label [this ^:final ^int bytecodeOffset ^:final ^Label/1 labels]
    (let [label (.readLabel this bytecodeOffset labels)]
      (set! (.-flags label)
            (unchecked-short (bit-and-int (.-flags label) (bit-not-int Label/FLAG_DEBUG_ONLY))))
      label))

  (method ^:private createDebugLabel ^void [this ^:final ^int bytecodeOffset ^:final ^Label/1 labels]
    (when (nil? (aget labels bytecodeOffset))
      (let [o-40 (.readLabel this bytecodeOffset labels)]
        (set! (.-flags o-40) (unchecked-short (bit-or-int (.-flags o-40) Label/FLAG_DEBUG_ONLY))))))

  (method ^:private readTypeAnnotations ^int/1 [this ^:final ^MethodVisitor methodVisitor
                                                ^:final ^Context context
                                                ^:final ^int runtimeTypeAnnotationsOffset
                                                ^:final ^boolean visible]
    (let [charBuffer (.-charBuffer context)
          ^:mutable currentOffset runtimeTypeAnnotationsOffset
          typeAnnotationsOffsets (new int/1 (.readUnsignedShort this currentOffset))]
      (set! currentOffset (unchecked-add-int currentOffset 2))
      (loop [^int i 0]
        (when (< i (alength typeAnnotationsOffsets))
          (aset typeAnnotationsOffsets i currentOffset)
          (let [targetType (.readInt this currentOffset)]
            (switch (unsigned-bit-shift-right-int targetType 24)
              (TypeReference/LOCAL_VARIABLE TypeReference/RESOURCE_VARIABLE)
                (let [^:mutable tableLength (.readUnsignedShort
                                              this
                                              (unchecked-add-int currentOffset 1))]
                  (set! currentOffset (unchecked-add-int currentOffset 3))
                  (while (> (let [old-41 tableLength]
                              (set! tableLength (unchecked-dec-int tableLength))
                              old-41)
                            0)
                    (let [startPc (.readUnsignedShort this currentOffset)
                          length (.readUnsignedShort this (unchecked-add-int currentOffset 2))]
                      (set! currentOffset (unchecked-add-int currentOffset 6))
                      (.createLabel this startPc (.-currentMethodLabels context))
                      (.createLabel this
                                    (unchecked-add-int startPc length)
                                    (.-currentMethodLabels context)))))
              (TypeReference/CAST TypeReference/CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                                  TypeReference/METHOD_INVOCATION_TYPE_ARGUMENT
                                  TypeReference/CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                                  TypeReference/METHOD_REFERENCE_TYPE_ARGUMENT)
                (set! currentOffset (unchecked-add-int currentOffset 4))
              (TypeReference/CLASS_EXTENDS TypeReference/CLASS_TYPE_PARAMETER_BOUND
                                           TypeReference/METHOD_TYPE_PARAMETER_BOUND
                                           TypeReference/THROWS
                                           TypeReference/EXCEPTION_PARAMETER
                                           TypeReference/INSTANCEOF
                                           TypeReference/NEW
                                           TypeReference/CONSTRUCTOR_REFERENCE
                                           TypeReference/METHOD_REFERENCE)
                (set! currentOffset (unchecked-add-int currentOffset 3))
              (throw (IllegalArgumentException.)))
            (let [pathLength (.readByte this currentOffset)]
              (if (== (unsigned-bit-shift-right-int targetType 24)
                      TypeReference/EXCEPTION_PARAMETER)
                  (let [path (when-not (== pathLength 0) (TypePath. classFileBuffer currentOffset))]
                    (set! currentOffset
                          (unchecked-add-int currentOffset
                                             (unchecked-add-int
                                               1
                                               (unchecked-multiply-int 2 pathLength))))
                    (let [annotationDescriptor (.readUTF8 this currentOffset charBuffer)]
                      (set! currentOffset (unchecked-add-int currentOffset 2))
                      (set! currentOffset
                            (.readElementValues this
                                                (.visitTryCatchAnnotation
                                                  methodVisitor
                                                  (bit-and-int
                                                    targetType
                                                    (unchecked-int 0xFFFFFF00))
                                                  path
                                                  annotationDescriptor
                                                  visible)
                                                currentOffset
                                                true
                                                charBuffer
                                                0))
                      (recur (unchecked-inc-int i))))
                  (do
                    (set! currentOffset
                          (unchecked-add-int currentOffset
                                             (unchecked-add-int
                                               3
                                               (unchecked-multiply-int 2 pathLength))))
                    (set! currentOffset
                          (.readElementValues this nil currentOffset true charBuffer 0))
                    (recur (unchecked-inc-int i))))))))
      typeAnnotationsOffsets))

  (method ^:private getTypeAnnotationBytecodeOffset ^int [this ^:final ^int/1 typeAnnotationOffsets
                                                          ^:final ^int typeAnnotationIndex]
    (if (or (or (nil? typeAnnotationOffsets)
                (>= typeAnnotationIndex (alength typeAnnotationOffsets)))
            (< (.readByte this (aget typeAnnotationOffsets typeAnnotationIndex))
               TypeReference/INSTANCEOF))
        -1
        (.readUnsignedShort this
                            (unchecked-add-int (aget typeAnnotationOffsets typeAnnotationIndex) 1))))

  (method ^:private readTypeAnnotationTarget ^int [this ^:final ^Context context
                                                   ^:final ^int typeAnnotationOffset]
    (let [^:mutable currentOffset typeAnnotationOffset
          ^:mutable targetType (.readInt this typeAnnotationOffset)]
      (switch (unsigned-bit-shift-right-int targetType 24)
        (TypeReference/CLASS_TYPE_PARAMETER TypeReference/METHOD_TYPE_PARAMETER
                                            TypeReference/METHOD_FORMAL_PARAMETER)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFFFF0000)))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        (TypeReference/FIELD TypeReference/METHOD_RETURN TypeReference/METHOD_RECEIVER)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFF000000)))
            (set! currentOffset (unchecked-add-int currentOffset 1)))
        (TypeReference/LOCAL_VARIABLE TypeReference/RESOURCE_VARIABLE)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFF000000)))
            (let [tableLength (.readUnsignedShort this (unchecked-add-int currentOffset 1))]
              (set! currentOffset (unchecked-add-int currentOffset 3))
              (set! (.-currentLocalVariableAnnotationRangeStarts context) (new Label/1 tableLength))
              (set! (.-currentLocalVariableAnnotationRangeEnds context) (new Label/1 tableLength))
              (set! (.-currentLocalVariableAnnotationRangeIndices context) (new int/1 tableLength))
              (loop [^int i 0]
                (when (< i tableLength)
                  (let [startPc (.readUnsignedShort this currentOffset)
                        length (.readUnsignedShort this (unchecked-add-int currentOffset 2))
                        index (.readUnsignedShort this (unchecked-add-int currentOffset 4))]
                    (set! currentOffset (unchecked-add-int currentOffset 6))
                    (aset (.-currentLocalVariableAnnotationRangeStarts context)
                          i
                          (.createLabel this startPc (.-currentMethodLabels context)))
                    (aset (.-currentLocalVariableAnnotationRangeEnds context)
                          i
                          (.createLabel this
                                        (unchecked-add-int startPc length)
                                        (.-currentMethodLabels context)))
                    (aset (.-currentLocalVariableAnnotationRangeIndices context) i index)
                    (recur (unchecked-inc-int i)))))))
        (TypeReference/CAST TypeReference/CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                            TypeReference/METHOD_INVOCATION_TYPE_ARGUMENT
                            TypeReference/CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                            TypeReference/METHOD_REFERENCE_TYPE_ARGUMENT)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFF0000FF)))
            (set! currentOffset (unchecked-add-int currentOffset 4)))
        (TypeReference/CLASS_EXTENDS TypeReference/CLASS_TYPE_PARAMETER_BOUND
                                     TypeReference/METHOD_TYPE_PARAMETER_BOUND
                                     TypeReference/THROWS
                                     TypeReference/EXCEPTION_PARAMETER)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFFFFFF00)))
            (set! currentOffset (unchecked-add-int currentOffset 3)))
        (TypeReference/INSTANCEOF TypeReference/NEW
                                  TypeReference/CONSTRUCTOR_REFERENCE
                                  TypeReference/METHOD_REFERENCE)
          (do
            (set! targetType (bit-and-int targetType (unchecked-int 0xFF000000)))
            (set! currentOffset (unchecked-add-int currentOffset 3)))
        (throw (IllegalArgumentException.)))
      (set! (.-currentTypeAnnotationTarget context) targetType)
      (let [pathLength (.readByte this currentOffset)]
        (set! (.-currentTypeAnnotationTargetPath context)
              (when-not (== pathLength 0) (TypePath. classFileBuffer currentOffset)))
        (unchecked-add-int (unchecked-add-int currentOffset 1)
                           (unchecked-multiply-int 2 pathLength)))))

  (method ^:private readParameterAnnotations ^void [this ^:final ^MethodVisitor methodVisitor
                                                    ^:final ^Context context
                                                    ^:final ^int runtimeParameterAnnotationsOffset
                                                    ^:final ^boolean visible]
    (let [^:mutable currentOffset runtimeParameterAnnotationsOffset
          numParameters (bit-and-int (aget classFileBuffer
                                           (let [old-42 currentOffset]
                                             (set! currentOffset (unchecked-inc-int currentOffset))
                                             old-42))
                                     0xFF)]
      (.visitAnnotableParameterCount methodVisitor numParameters visible)
      (let [charBuffer (.-charBuffer context)]
        (loop [^int i 0]
          (when (< i numParameters)
            (let [^:mutable numAnnotations (.readUnsignedShort this currentOffset)]
              (set! currentOffset (unchecked-add-int currentOffset 2))
              (while (> (let [old-43 numAnnotations]
                          (set! numAnnotations (unchecked-dec-int numAnnotations))
                          old-43)
                        0)
                (let [annotationDescriptor (.readUTF8 this currentOffset charBuffer)]
                  (set! currentOffset (unchecked-add-int currentOffset 2))
                  (set! currentOffset
                        (.readElementValues this
                                            (.visitParameterAnnotation
                                              methodVisitor
                                              i
                                              annotationDescriptor
                                              visible)
                                            currentOffset
                                            true
                                            charBuffer
                                            0))))
              (recur (unchecked-inc-int i))))))))

  (method ^:private readElementValues ^int [this ^:final ^AnnotationVisitor annotationVisitor
                                            ^:final ^int annotationOffset ^:final ^boolean named
                                            ^:final ^char/1 charBuffer ^:final ^int depth]
    (when (>= depth 256) (throw (LimitExceededException. "Too many nested annotations")))
    (let [^:mutable currentOffset annotationOffset
          ^:mutable numElementValuePairs (.readUnsignedShort this currentOffset)]
      (set! currentOffset (unchecked-add-int currentOffset 2))
      (if named
          (while (> (let [old-44 numElementValuePairs]
                      (set! numElementValuePairs (unchecked-dec-int numElementValuePairs))
                      old-44)
                    0)
            (let [elementName (.readUTF8 this currentOffset charBuffer)]
              (set! currentOffset
                    (.readElementValue this
                                       annotationVisitor
                                       (unchecked-add-int currentOffset 2)
                                       elementName
                                       charBuffer
                                       depth))))
          (while (> (let [old-45 numElementValuePairs]
                      (set! numElementValuePairs (unchecked-dec-int numElementValuePairs))
                      old-45)
                    0)
            (set! currentOffset
                  (.readElementValue this annotationVisitor currentOffset nil charBuffer depth))))
      (when (some? annotationVisitor) (.visitEnd annotationVisitor))
      currentOffset))

  (method ^:private readElementValue ^int [this ^:final ^AnnotationVisitor annotationVisitor
                                           ^:final ^int elementValueOffset
                                           ^:final ^String elementName ^:final ^char/1 charBuffer
                                           ^:final ^int depth]
    (let [^:mutable currentOffset elementValueOffset]
      (when (nil? annotationVisitor)
        (switch (bit-and-int (aget classFileBuffer currentOffset) 0xFF)
          \e (return (unchecked-add-int currentOffset 5))
          \@
            (return (.readElementValues this
                                        nil
                                        (unchecked-add-int currentOffset 3)
                                        true
                                        charBuffer
                                        (unchecked-add-int depth 1)))
          \[
            (return (.readElementValues this
                                        nil
                                        (unchecked-add-int currentOffset 1)
                                        false
                                        charBuffer
                                        (unchecked-add-int depth 1)))
          (return (unchecked-add-int currentOffset 3))))
      (switch (bit-and-int (aget classFileBuffer
                                 (let [old-46 currentOffset]
                                   (set! currentOffset (unchecked-inc-int currentOffset))
                                   old-46))
                           0xFF)
        \B
          (do
            (.visit annotationVisitor
                    elementName
                    (unchecked-byte
                      (.readInt this (aget cpInfoOffsets (.readUnsignedShort this currentOffset)))))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \C
          (do
            (.visit annotationVisitor
                    elementName
                    (unchecked-char
                      (.readInt this (aget cpInfoOffsets (.readUnsignedShort this currentOffset)))))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        (\D \F \I \J)
          (do
            (.visit annotationVisitor
                    elementName
                    (.readConst this (.readUnsignedShort this currentOffset) charBuffer))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \S
          (do
            (.visit annotationVisitor
                    elementName
                    (unchecked-short
                      (.readInt this (aget cpInfoOffsets (.readUnsignedShort this currentOffset)))))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \Z
          (do
            (.visit annotationVisitor
                    elementName
                    (if (== (.readInt this
                                      (aget cpInfoOffsets (.readUnsignedShort this currentOffset)))
                            0)
                        Boolean/FALSE
                        Boolean/TRUE))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \s
          (do
            (.visit annotationVisitor elementName (.readUTF8 this currentOffset charBuffer))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \e
          (do
            (.visitEnum annotationVisitor
                        elementName
                        (.readUTF8 this currentOffset charBuffer)
                        (.readUTF8 this (unchecked-add-int currentOffset 2) charBuffer))
            (set! currentOffset (unchecked-add-int currentOffset 4)))
        \c
          (do
            (.visit annotationVisitor
                    elementName
                    (Type/getType (.readUTF8 this currentOffset charBuffer)))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        \@
          (set! currentOffset
                (.readElementValues this
                                    (.visitAnnotation
                                      annotationVisitor
                                      elementName
                                      (.readUTF8 this currentOffset charBuffer))
                                    (unchecked-add-int currentOffset 2)
                                    true
                                    charBuffer
                                    (unchecked-add-int depth 1)))
        \[
          (let [numValues (.readUnsignedShort this currentOffset)]
            (set! currentOffset (unchecked-add-int currentOffset 2))
            (when (== numValues 0)
              (return (.readElementValues this
                                          (.visitArray annotationVisitor elementName)
                                          (unchecked-subtract-int currentOffset 2)
                                          false
                                          charBuffer
                                          (unchecked-add-int depth 1))))
            (switch (bit-and-int (aget classFileBuffer currentOffset) 0xFF)
              \B
                (let [byteValues (new byte/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset byteValues
                            i
                            (unchecked-byte
                              (.readInt this
                                        (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int currentOffset 1))))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName byteValues))
              \Z
                (let [booleanValues (new boolean/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset booleanValues
                            i
                            (not (== (.readInt this
                                               (aget
                                                 cpInfoOffsets
                                                 (.readUnsignedShort
                                                   this
                                                   (unchecked-add-int currentOffset 1))))
                                     0)))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName booleanValues))
              \S
                (let [shortValues (new short/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset shortValues
                            i
                            (unchecked-short
                              (.readInt this
                                        (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int currentOffset 1))))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName shortValues))
              \C
                (let [charValues (new char/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset charValues
                            i
                            (unchecked-char
                              (.readInt this
                                        (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int currentOffset 1))))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName charValues))
              \I
                (let [intValues (new int/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset intValues
                            i
                            (.readInt this
                                      (aget cpInfoOffsets
                                            (.readUnsignedShort
                                              this
                                              (unchecked-add-int currentOffset 1)))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName intValues))
              \J
                (let [longValues (new long/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset longValues
                            i
                            (.readLong this
                                       (aget cpInfoOffsets
                                             (.readUnsignedShort
                                               this
                                               (unchecked-add-int currentOffset 1)))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName longValues))
              \F
                (let [floatValues (new float/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset floatValues
                            i
                            (Float/intBitsToFloat
                              (.readInt this
                                        (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int currentOffset 1))))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName floatValues))
              \D
                (let [doubleValues (new double/1 numValues)]
                  (loop [^int i 0]
                    (when (< i numValues)
                      (aset doubleValues
                            i
                            (Double/longBitsToDouble
                              (.readLong this
                                         (aget cpInfoOffsets
                                               (.readUnsignedShort
                                                 this
                                                 (unchecked-add-int currentOffset 1))))))
                      (set! currentOffset (unchecked-add-int currentOffset 3))
                      (recur (unchecked-inc-int i))))
                  (.visit annotationVisitor elementName doubleValues))
              (set! currentOffset
                    (.readElementValues this
                                        (.visitArray annotationVisitor elementName)
                                        (unchecked-subtract-int currentOffset 2)
                                        false
                                        charBuffer
                                        (unchecked-add-int depth 1)))))
        (throw (IllegalArgumentException.)))
      currentOffset))

  (method ^:private computeImplicitFrame ^void [this ^:final ^Context context]
    (let [methodDescriptor (.-currentMethodDescriptor context)
          locals (.-currentFrameLocalTypes context)
          ^:mutable ^int numLocal 0]
      (when (== (bit-and-int (.-currentMethodAccessFlags context) Opcodes/ACC_STATIC) 0)
        (if (.equals "<init>" (.-currentMethodName context))
            (do
              (aset locals numLocal Opcodes/UNINITIALIZED_THIS)
              (set! numLocal (unchecked-inc-int numLocal)))
            (do
              (aset locals
                    numLocal
                    (.readClass this (unchecked-add-int header 2) (.-charBuffer context)))
              (set! numLocal (unchecked-inc-int numLocal)))))
      (let [^:mutable ^int currentMethodDescritorOffset 1]
        (while true
          (let [currentArgumentDescriptorStartOffset currentMethodDescritorOffset]
            (switch (.charAt methodDescriptor
                             (let [old-47 currentMethodDescritorOffset]
                               (set! currentMethodDescritorOffset
                                     (unchecked-inc-int currentMethodDescritorOffset))
                               old-47))
              (\Z \C \B \S \I)
                (do
                  (aset locals numLocal Opcodes/INTEGER)
                  (set! numLocal (unchecked-inc-int numLocal)))
              \F
                (do
                  (aset locals numLocal Opcodes/FLOAT)
                  (set! numLocal (unchecked-inc-int numLocal)))
              \J
                (do
                  (aset locals numLocal Opcodes/LONG)
                  (set! numLocal (unchecked-inc-int numLocal)))
              \D
                (do
                  (aset locals numLocal Opcodes/DOUBLE)
                  (set! numLocal (unchecked-inc-int numLocal)))
              \[
                (do
                  (while (== (.charAt methodDescriptor currentMethodDescritorOffset) \[)
                    (set! currentMethodDescritorOffset
                          (unchecked-inc-int currentMethodDescritorOffset)))
                  (when (== (.charAt methodDescriptor currentMethodDescritorOffset) \L)
                    (set! currentMethodDescritorOffset
                          (unchecked-inc-int currentMethodDescritorOffset))
                    (while (not (== (.charAt methodDescriptor currentMethodDescritorOffset) \;))
                      (set! currentMethodDescritorOffset
                            (unchecked-inc-int currentMethodDescritorOffset))))
                  (aset locals
                        numLocal
                        (.substring methodDescriptor
                                    currentArgumentDescriptorStartOffset
                                    (set! currentMethodDescritorOffset
                                          (unchecked-inc-int currentMethodDescritorOffset))))
                  (set! numLocal (unchecked-inc-int numLocal)))
              \L
                (do
                  (while (not (== (.charAt methodDescriptor currentMethodDescritorOffset) \;))
                    (set! currentMethodDescritorOffset
                          (unchecked-inc-int currentMethodDescritorOffset)))
                  (aset locals
                        numLocal
                        (.substring methodDescriptor
                                    (unchecked-add-int currentArgumentDescriptorStartOffset 1)
                                    currentMethodDescritorOffset))
                  (set! numLocal (unchecked-inc-int numLocal))
                  (set! currentMethodDescritorOffset
                        (unchecked-inc-int currentMethodDescritorOffset)))
              (do (set! (.-currentFrameLocalCount context) numLocal) (return))))))))

  (method ^:private readStackMapFrame ^int [this ^:final ^int stackMapFrameOffset
                                            ^:final ^boolean compressed ^:final ^boolean expand
                                            ^:final ^Context context]
    (let [^:mutable currentOffset stackMapFrameOffset
          charBuffer (.-charBuffer context)
          labels (.-currentMethodLabels context)
          ^:mutable ^int frameType 0]
      (if compressed
          (do
            (set! frameType (bit-and-int (aget classFileBuffer currentOffset) 0xFF))
            (set! currentOffset (unchecked-inc-int currentOffset)))
          (do (set! frameType Frame/FULL_FRAME) (set! (.-currentFrameOffset context) -1)))
      (let [^:mutable ^int offsetDelta 0]
        (set! (.-currentFrameLocalCountDelta context) 0)
        (cond
          (< frameType Frame/SAME_LOCALS_1_STACK_ITEM_FRAME)
            (do
              (set! offsetDelta frameType)
              (set! (.-currentFrameType context) Opcodes/F_SAME)
              (set! (.-currentFrameStackCount context) 0))
          (< frameType Frame/RESERVED)
            (do
              (set! offsetDelta
                    (unchecked-subtract-int frameType Frame/SAME_LOCALS_1_STACK_ITEM_FRAME))
              (set! currentOffset
                    (.readVerificationTypeInfo this
                                               currentOffset
                                               (.-currentFrameStackTypes context)
                                               0
                                               charBuffer
                                               labels))
              (set! (.-currentFrameType context) Opcodes/F_SAME1)
              (set! (.-currentFrameStackCount context) 1))
          (>= frameType Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
            (do
              (set! offsetDelta (.readUnsignedShort this currentOffset))
              (set! currentOffset (unchecked-add-int currentOffset 2))
              (cond
                (== frameType Frame/SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                  (do
                    (set! currentOffset
                          (.readVerificationTypeInfo
                            this
                            currentOffset
                            (.-currentFrameStackTypes context)
                            0
                            charBuffer
                            labels))
                    (set! (.-currentFrameType context) Opcodes/F_SAME1)
                    (set! (.-currentFrameStackCount context) 1))
                (and (>= frameType Frame/CHOP_FRAME) (< frameType Frame/SAME_FRAME_EXTENDED))
                  (do
                    (set! (.-currentFrameType context) Opcodes/F_CHOP)
                    (set! (.-currentFrameLocalCountDelta context)
                          (unchecked-subtract-int Frame/SAME_FRAME_EXTENDED frameType))
                    (set! (.-currentFrameLocalCount context)
                          (unchecked-subtract-int
                            (.-currentFrameLocalCount context)
                            (.-currentFrameLocalCountDelta context)))
                    (set! (.-currentFrameStackCount context) 0))
                (== frameType Frame/SAME_FRAME_EXTENDED)
                  (do
                    (set! (.-currentFrameType context) Opcodes/F_SAME)
                    (set! (.-currentFrameStackCount context) 0))
                (< frameType Frame/FULL_FRAME)
                  (let [^:mutable local (if expand (.-currentFrameLocalCount context) 0)]
                    (loop [^int k (unchecked-subtract-int frameType Frame/SAME_FRAME_EXTENDED)]
                      (when (> k 0)
                        (set! currentOffset
                              (.readVerificationTypeInfo
                                this
                                currentOffset
                                (.-currentFrameLocalTypes context)
                                local
                                charBuffer
                                labels))
                        (set! local (unchecked-inc-int local))
                        (recur (unchecked-dec-int k))))
                    (set! (.-currentFrameType context) Opcodes/F_APPEND)
                    (set! (.-currentFrameLocalCountDelta context)
                          (unchecked-subtract-int frameType Frame/SAME_FRAME_EXTENDED))
                    (set! (.-currentFrameLocalCount context)
                          (unchecked-add-int (.-currentFrameLocalCount context)
                                             (.-currentFrameLocalCountDelta context)))
                    (set! (.-currentFrameStackCount context) 0))
                :else
                  (let [numberOfLocals (.readUnsignedShort this currentOffset)]
                    (set! currentOffset (unchecked-add-int currentOffset 2))
                    (set! (.-currentFrameType context) Opcodes/F_FULL)
                    (set! (.-currentFrameLocalCountDelta context) numberOfLocals)
                    (set! (.-currentFrameLocalCount context) numberOfLocals)
                    (loop [^int local 0]
                      (when (< local numberOfLocals)
                        (set! currentOffset
                              (.readVerificationTypeInfo
                                this
                                currentOffset
                                (.-currentFrameLocalTypes context)
                                local
                                charBuffer
                                labels))
                        (recur (unchecked-inc-int local))))
                    (let [numberOfStackItems (.readUnsignedShort this currentOffset)]
                      (set! currentOffset (unchecked-add-int currentOffset 2))
                      (set! (.-currentFrameStackCount context) numberOfStackItems)
                      (loop [^int stack 0]
                        (when (< stack numberOfStackItems)
                          (set! currentOffset
                                (.readVerificationTypeInfo
                                  this
                                  currentOffset
                                  (.-currentFrameStackTypes context)
                                  stack
                                  charBuffer
                                  labels))
                          (recur (unchecked-inc-int stack))))))))
          :else (throw (IllegalArgumentException.)))
        (set! (.-currentFrameOffset context)
              (unchecked-add-int (.-currentFrameOffset context) (unchecked-add-int offsetDelta 1)))
        (.createLabel this (.-currentFrameOffset context) labels)
        currentOffset)))

  (method ^:private readVerificationTypeInfo ^int [this ^:final ^int verificationTypeInfoOffset
                                                   ^:final ^Object/1 frame ^:final ^int index
                                                   ^:final ^char/1 charBuffer
                                                   ^:final ^Label/1 labels]
    (let [^:mutable currentOffset verificationTypeInfoOffset
          tag (bit-and-int (aget classFileBuffer
                                 (let [old-48 currentOffset]
                                   (set! currentOffset (unchecked-inc-int currentOffset))
                                   old-48))
                           0xFF)]
      (switch tag
        Frame/ITEM_TOP (aset frame index Opcodes/TOP)
        Frame/ITEM_INTEGER (aset frame index Opcodes/INTEGER)
        Frame/ITEM_FLOAT (aset frame index Opcodes/FLOAT)
        Frame/ITEM_DOUBLE (aset frame index Opcodes/DOUBLE)
        Frame/ITEM_LONG (aset frame index Opcodes/LONG)
        Frame/ITEM_NULL (aset frame index Opcodes/NULL)
        Frame/ITEM_UNINITIALIZED_THIS (aset frame index Opcodes/UNINITIALIZED_THIS)
        Frame/ITEM_OBJECT
          (do
            (aset frame index (.readClass this currentOffset charBuffer))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        Frame/ITEM_UNINITIALIZED
          (do
            (aset frame index (.createLabel this (.readUnsignedShort this currentOffset) labels))
            (set! currentOffset (unchecked-add-int currentOffset 2)))
        (throw (IllegalArgumentException.)))
      currentOffset))

  (method ^:final getFirstAttributeOffset ^int [this]
    (let [^:mutable currentOffset (unchecked-add-int
                                    (unchecked-add-int header 8)
                                    (unchecked-multiply-int
                                      (.readUnsignedShort this (unchecked-add-int header 6))
                                      2))
          ^:mutable fieldsCount (.readUnsignedShort this currentOffset)]
      (set! currentOffset (unchecked-add-int currentOffset 2))
      (while (> (let [old-49 fieldsCount] (set! fieldsCount (unchecked-dec-int fieldsCount)) old-49)
                0)
        (let [^:mutable attributesCount (.readUnsignedShort
                                          this
                                          (unchecked-add-int currentOffset 6))]
          (set! currentOffset (unchecked-add-int currentOffset 8))
          (while (> (let [old-50 attributesCount]
                      (set! attributesCount (unchecked-dec-int attributesCount))
                      old-50)
                    0)
            (set! currentOffset
                  (unchecked-add-int currentOffset
                                     (unchecked-add-int
                                       6
                                       (.readInt this (unchecked-add-int currentOffset 2))))))))
      (let [^:mutable methodsCount (.readUnsignedShort this currentOffset)]
        (set! currentOffset (unchecked-add-int currentOffset 2))
        (while (> (let [old-51 methodsCount]
                    (set! methodsCount (unchecked-dec-int methodsCount))
                    old-51)
                  0)
          (let [^:mutable attributesCount (.readUnsignedShort
                                            this
                                            (unchecked-add-int currentOffset 6))]
            (set! currentOffset (unchecked-add-int currentOffset 8))
            (while (> (let [old-52 attributesCount]
                        (set! attributesCount (unchecked-dec-int attributesCount))
                        old-52)
                      0)
              (set! currentOffset
                    (unchecked-add-int currentOffset
                                       (unchecked-add-int
                                         6
                                         (.readInt this (unchecked-add-int currentOffset 2))))))))
        (unchecked-add-int currentOffset 2))))

  (method ^:private readBootstrapMethodsAttribute ^int/1 [this ^:final ^int maxStringLength]
    (let [charBuffer (new char/1 maxStringLength)
          ^:mutable currentAttributeOffset (.getFirstAttributeOffset this)]
      (loop [^int i (.readUnsignedShort this (unchecked-subtract-int currentAttributeOffset 2))]
        (when (> i 0)
          (let [attributeName (.readUTF8 this currentAttributeOffset charBuffer)
                attributeLength (.readInt this (unchecked-add-int currentAttributeOffset 2))]
            (set! currentAttributeOffset (unchecked-add-int currentAttributeOffset 6))
            (when (.equals Constants/BOOTSTRAP_METHODS attributeName)
              (when (> attributeLength
                       (unchecked-subtract-int (alength classFileBuffer) currentAttributeOffset))
                (throw (IllegalArgumentException.)))
              (let [result (new int/1 (.readUnsignedShort this currentAttributeOffset))
                    ^:mutable currentBootstrapMethodOffset (unchecked-add-int
                                                             currentAttributeOffset
                                                             2)]
                (loop [^int j 0]
                  (when (< j (alength result))
                    (aset result j currentBootstrapMethodOffset)
                    (set! currentBootstrapMethodOffset
                          (unchecked-add-int currentBootstrapMethodOffset
                                             (unchecked-add-int
                                               4
                                               (unchecked-multiply-int
                                                 (.readUnsignedShort
                                                   this
                                                   (unchecked-add-int
                                                     currentBootstrapMethodOffset
                                                     2))
                                                 2))))
                    (recur (unchecked-inc-int j))))
                (return result)))
            (set! currentAttributeOffset (unchecked-add-int currentAttributeOffset attributeLength))
            (recur (unchecked-dec-int i)))))
      (throw (IllegalArgumentException.))))

  (method ^:private readAttribute ^Attribute [this ^:final ^Attribute/1 attributePrototypes
                                              ^:final ^String type ^:final ^int offset
                                              ^:final ^int length ^:final ^char/1 charBuffer
                                              ^:final ^int codeAttributeOffset
                                              ^:final ^Label/1 labels]
    (when (> length (unchecked-subtract-int (alength classFileBuffer) offset))
      (throw (IllegalArgumentException.)))
    (for-each [^Attribute attributePrototype attributePrototypes]
      (when (.equals (.-type attributePrototype) type)
        (return (.read attributePrototype this offset length charBuffer codeAttributeOffset labels))))
    (.read (Attribute. type) this offset length nil -1 nil))

  (method ^:public getItemCount ^int [this] (alength cpInfoOffsets))

  (method ^:public getItem ^int [this ^:final ^int constantPoolEntryIndex]
    (aget cpInfoOffsets constantPoolEntryIndex))

  (method ^:public getMaxStringLength ^int [this] maxStringLength)

  (method ^:public readByte ^int [this ^:final ^int offset]
    (bit-and-int (aget classFileBuffer offset) 0xFF))

  (method ^:public readBytes ^byte/1 [this ^:final ^int offset ^:final ^int length]
    (let [result (new byte/1 length)]
      (System/arraycopy classFileBuffer offset result 0 length)
      result))

  (method ^:public readUnsignedShort ^int [this ^:final ^int offset]
    (let [classBuffer classFileBuffer]
      (bit-or-int (bit-shift-left-int (bit-and-int (aget classBuffer offset) 0xFF) 8)
                  (bit-and-int (aget classBuffer (unchecked-add-int offset 1)) 0xFF))))

  (method ^:public readShort ^short [this ^:final ^int offset]
    (let [classBuffer classFileBuffer]
      (unchecked-short
        (bit-or-int (bit-shift-left-int (bit-and-int (aget classBuffer offset) 0xFF) 8)
                    (bit-and-int (aget classBuffer (unchecked-add-int offset 1)) 0xFF)))))

  (method ^:public readInt ^int [this ^:final ^int offset]
    (let [classBuffer classFileBuffer]
      (bit-or-int (bit-or-int (bit-or-int (bit-shift-left-int
                                            (bit-and-int (aget classBuffer offset) 0xFF)
                                            24)
                                          (bit-shift-left-int
                                            (bit-and-int
                                              (aget classBuffer (unchecked-add-int offset 1))
                                              0xFF)
                                            16))
                              (bit-shift-left-int
                                (bit-and-int (aget classBuffer (unchecked-add-int offset 2)) 0xFF)
                                8))
                  (bit-and-int (aget classBuffer (unchecked-add-int offset 3)) 0xFF))))

  (method ^:public readLong ^long [this ^:final ^int offset]
    (let [^long l1 (.readInt this offset)
          l0 (bit-and (.readInt this (unchecked-add-int offset 4)) 0xFFFFFFFF)]
      (bit-or (bit-shift-left l1 32) l0)))

  (method ^:public readUTF8 ^String [this ^:final ^int offset ^:final ^char/1 charBuffer]
    (let [constantPoolEntryIndex (.readUnsignedShort this offset)]
      (when-not (or (== offset 0) (== constantPoolEntryIndex 0))
        (.readUtf this constantPoolEntryIndex charBuffer))))

  (method ^:final readUtf ^String [this ^:final ^int constantPoolEntryIndex
                                   ^:final ^char/1 charBuffer]
    (let [value (aget constantUtf8Values constantPoolEntryIndex)]
      (if (some? value)
          value
          (let [cpInfoOffset (aget cpInfoOffsets constantPoolEntryIndex)]
            (aset constantUtf8Values
                  constantPoolEntryIndex
                  (.readUtf this
                            (unchecked-add-int cpInfoOffset 2)
                            (.readUnsignedShort this cpInfoOffset)
                            charBuffer))))))

  (method ^:private readUtf ^String [this ^:final ^int utfOffset ^:final ^int utfLength
                                     ^:final ^char/1 charBuffer]
    (let [^:mutable currentOffset utfOffset
          endOffset (unchecked-add-int currentOffset utfLength)
          ^:mutable ^int strLength 0
          classBuffer classFileBuffer]
      (while (< currentOffset endOffset)
        (let [^int currentByte (aget classBuffer
                                     (let [old-53 currentOffset]
                                       (set! currentOffset (unchecked-inc-int currentOffset))
                                       old-53))]
          (cond
            (== (bit-and-int currentByte 0x80) 0)
              (do
                (aset charBuffer strLength (unchecked-char (bit-and-int currentByte 0x7F)))
                (set! strLength (unchecked-inc-int strLength)))
            (== (bit-and-int currentByte 0xE0) 0xC0)
              (do
                (aset charBuffer
                      strLength
                      (unchecked-char
                        (unchecked-add-int (bit-shift-left-int (bit-and-int currentByte 0x1F) 6)
                                           (bit-and-int (aget classBuffer currentOffset) 0x3F))))
                (set! strLength (unchecked-inc-int strLength))
                (set! currentOffset (unchecked-inc-int currentOffset)))
            :else
              (do
                (aset charBuffer
                      strLength
                      (unchecked-char
                        (unchecked-add-int
                          (unchecked-add-int (bit-shift-left-int (bit-and-int currentByte 0xF) 12)
                                             (bit-shift-left-int
                                               (bit-and-int
                                                 (aget
                                                   classBuffer
                                                   (let [old-54 currentOffset]
                                                     (set!
                                                       currentOffset
                                                       (unchecked-inc-int currentOffset))
                                                     old-54))
                                                 0x3F)
                                               6))
                          (bit-and-int (aget classBuffer
                                             (let [old-55 currentOffset]
                                               (set!
                                                 currentOffset
                                                 (unchecked-inc-int currentOffset))
                                               old-55))
                                       0x3F))))
                (set! strLength (unchecked-inc-int strLength))))))
      (^[char/1 int int] String/new charBuffer 0 strLength)))

  (method ^:private readStringish ^String [this ^:final ^int offset ^:final ^char/1 charBuffer]
    (.readUTF8 this (aget cpInfoOffsets (.readUnsignedShort this offset)) charBuffer))

  (method ^:public readClass ^String [this ^:final ^int offset ^:final ^char/1 charBuffer]
    (.readStringish this offset charBuffer))

  (method ^:public readModule ^String [this ^:final ^int offset ^:final ^char/1 charBuffer]
    (.readStringish this offset charBuffer))

  (method ^:public readPackage ^String [this ^:final ^int offset ^:final ^char/1 charBuffer]
    (.readStringish this offset charBuffer))

  (method ^:private readConstantDynamic ^ConstantDynamic [this ^:final ^int constantPoolEntryIndex
                                                          ^:final ^char/1 charBuffer
                                                          ^:final ^int depthLimit]
    (when (== depthLimit 0) (throw (LimitExceededException. "Too many nested ConstantDynamic")))
    (let [constantDynamic (aget constantDynamicValues constantPoolEntryIndex)]
      (if (some? constantDynamic)
          constantDynamic
          (let [cpInfoOffset (aget cpInfoOffsets constantPoolEntryIndex)
                nameAndTypeCpInfoOffset (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int cpInfoOffset 2)))
                name (.readUTF8 this nameAndTypeCpInfoOffset charBuffer)
                descriptor (.readUTF8 this (unchecked-add-int nameAndTypeCpInfoOffset 2) charBuffer)
                ^:mutable bootstrapMethodOffset (aget
                                                  bootstrapMethodOffsets
                                                  (.readUnsignedShort this cpInfoOffset))
                handleCpIndex (.readUnsignedShort this bootstrapMethodOffset)]
            (when (.isConstantDynamic this handleCpIndex)
              (throw (ClassCastException. "ConstantDynamic cannot be cast to Handle")))
            (let [handle (cast Handle (.readConst this handleCpIndex charBuffer))
                  bootstrapMethodArguments (new Object/1
                                                (.readUnsignedShort
                                                  this
                                                  (unchecked-add-int bootstrapMethodOffset 2)))]
              (set! bootstrapMethodOffset (unchecked-add-int bootstrapMethodOffset 4))
              (loop [^int i 0]
                (when (< i (alength bootstrapMethodArguments))
                  (let [argumentCpIndex (.readUnsignedShort this bootstrapMethodOffset)]
                    (aset bootstrapMethodArguments
                          i
                          (if (.isConstantDynamic this argumentCpIndex)
                              ^Object (.readConstantDynamic
                                        this
                                        argumentCpIndex
                                        charBuffer
                                        (unchecked-subtract-int depthLimit 1))
                              (.readConst this argumentCpIndex charBuffer)))
                    (set! bootstrapMethodOffset (unchecked-add-int bootstrapMethodOffset 2))
                    (recur (unchecked-inc-int i)))))
              (aset constantDynamicValues
                    constantPoolEntryIndex
                    (ConstantDynamic. name descriptor handle bootstrapMethodArguments)))))))

  (method ^:private isConstantDynamic ^boolean [this ^:final ^int constantPoolEntryIndex]
    (let [cpInfoOffset (aget cpInfoOffsets constantPoolEntryIndex)]
      (== (aget classFileBuffer (unchecked-subtract-int cpInfoOffset 1))
          Symbol/CONSTANT_DYNAMIC_TAG)))

  (method ^:public readConst [this ^:final ^int constantPoolEntryIndex ^:final ^char/1 charBuffer]
    (let [cpInfoOffset (aget cpInfoOffsets constantPoolEntryIndex)]
      (switch (aget classFileBuffer (unchecked-subtract-int cpInfoOffset 1))
        Symbol/CONSTANT_INTEGER_TAG (.readInt this cpInfoOffset)
        Symbol/CONSTANT_FLOAT_TAG (Float/intBitsToFloat (.readInt this cpInfoOffset))
        Symbol/CONSTANT_LONG_TAG (.readLong this cpInfoOffset)
        Symbol/CONSTANT_DOUBLE_TAG (Double/longBitsToDouble (.readLong this cpInfoOffset))
        Symbol/CONSTANT_CLASS_TAG (Type/getObjectType (.readUTF8 this cpInfoOffset charBuffer))
        Symbol/CONSTANT_STRING_TAG (.readUTF8 this cpInfoOffset charBuffer)
        Symbol/CONSTANT_METHOD_TYPE_TAG
          (Type/getMethodType (.readUTF8 this cpInfoOffset charBuffer))
        Symbol/CONSTANT_METHOD_HANDLE_TAG
          (let [referenceKind (.readByte this cpInfoOffset)
                referenceCpInfoOffset (aget cpInfoOffsets
                                            (.readUnsignedShort
                                              this
                                              (unchecked-add-int cpInfoOffset 1)))
                nameAndTypeCpInfoOffset (aget cpInfoOffsets
                                              (.readUnsignedShort
                                                this
                                                (unchecked-add-int referenceCpInfoOffset 2)))
                owner (.readClass this referenceCpInfoOffset charBuffer)
                name (.readUTF8 this nameAndTypeCpInfoOffset charBuffer)
                descriptor (.readUTF8 this (unchecked-add-int nameAndTypeCpInfoOffset 2) charBuffer)
                isInterface (== (aget classFileBuffer
                                      (unchecked-subtract-int referenceCpInfoOffset 1))
                                Symbol/CONSTANT_INTERFACE_METHODREF_TAG)]
            (Handle. referenceKind owner name descriptor isInterface))
        Symbol/CONSTANT_DYNAMIC_TAG
          (let [result (.readConstantDynamic this constantPoolEntryIndex charBuffer 127)]
            (when (<= (.subtractTreeSize result 4096) 0)
              (throw (LimitExceededException. "Too many nested ConstantDynamic")))
            result)
        (throw (IllegalArgumentException.))))))
