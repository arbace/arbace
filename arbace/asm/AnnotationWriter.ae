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
;; Converted from clojure/asm/AnnotationWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final AnnotationWriter
  :extends AnnotationVisitor

  (field ^:private ^:final ^SymbolTable symbolTable)

  (field ^:private ^:final ^boolean useNamedValues)

  (field ^:private ^:final ^ByteVector annotation)

  (field ^:private ^:final ^int numElementValuePairsOffset)

  (field ^:private ^int numElementValuePairs)

  (field ^:private ^:final ^AnnotationWriter previousAnnotation)

  (field ^:private ^AnnotationWriter nextAnnotation)

  (constructor [this ^:final ^SymbolTable symbolTable ^:final ^boolean useNamedValues
                ^:final ^ByteVector annotation ^:final ^AnnotationWriter previousAnnotation]
    (super. Opcodes/ASM9)
    (set! (.-symbolTable this) symbolTable)
    (set! (.-useNamedValues this) useNamedValues)
    (set! (.-annotation this) annotation)
    (set! (.-numElementValuePairsOffset this)
          (if (== (.-length annotation) 0) -1 (unchecked-subtract-int (.-length annotation) 2)))
    (set! (.-previousAnnotation this) previousAnnotation)
    (when (some? previousAnnotation) (set! (.-nextAnnotation previousAnnotation) this)))

  (method ^:static create ^AnnotationWriter [^:final ^SymbolTable symbolTable
                                             ^:final ^String descriptor
                                             ^:final ^AnnotationWriter previousAnnotation]
    (let [annotation (ByteVector.)]
      (.putShort (.putShort annotation (.addConstantUtf8 symbolTable descriptor)) 0)
      (AnnotationWriter. symbolTable true annotation previousAnnotation)))

  (method ^:static create ^AnnotationWriter [^:final ^SymbolTable symbolTable ^:final ^int typeRef
                                             ^:final ^TypePath typePath ^:final ^String descriptor
                                             ^:final ^AnnotationWriter previousAnnotation]
    (let [typeAnnotation (ByteVector.)]
      (TypeReference/putTarget typeRef typeAnnotation)
      (TypePath/put typePath typeAnnotation)
      (.putShort (.putShort typeAnnotation (.addConstantUtf8 symbolTable descriptor)) 0)
      (AnnotationWriter. symbolTable true typeAnnotation previousAnnotation)))

  (method ^:public visit ^void [this ^:final ^String name ^:final value]
    (set! numElementValuePairs (unchecked-inc-int numElementValuePairs))
    (when useNamedValues (.putShort annotation (.addConstantUtf8 symbolTable name)))
    (cond
      (instance? String value)
        (.put12 annotation \s (.addConstantUtf8 symbolTable (cast String value)))
      (instance? Byte value)
        (.put12 annotation
                \B
                (.-index (.addConstantInteger symbolTable (.byteValue (cast Byte value)))))
      (instance? Boolean value)
        (let [^int booleanValue (if (.booleanValue (cast Boolean value)) 1 0)]
          (.put12 annotation \Z (.-index (.addConstantInteger symbolTable booleanValue))))
      (instance? Character value)
        (.put12 annotation
                \C
                (.-index (.addConstantInteger symbolTable (.charValue (cast Character value)))))
      (instance? Short value)
        (.put12 annotation
                \S
                (.-index (.addConstantInteger symbolTable (.shortValue (cast Short value)))))
      (instance? Type value)
        (.put12 annotation \c (.addConstantUtf8 symbolTable (.getDescriptor (cast Type value))))
      (instance? byte/1 value)
        (let [byteArray (cast byte/1 value)]
          (.put12 annotation \[ (alength byteArray))
          (for-each [^byte byteValue byteArray]
            (.put12 annotation \B (.-index (.addConstantInteger symbolTable byteValue)))))
      (instance? boolean/1 value)
        (let [booleanArray (cast boolean/1 value)]
          (.put12 annotation \[ (alength booleanArray))
          (for-each [^boolean booleanValue booleanArray]
            (.put12 annotation \Z (.-index (.addConstantInteger symbolTable (if booleanValue 1 0))))))
      (instance? short/1 value)
        (let [shortArray (cast short/1 value)]
          (.put12 annotation \[ (alength shortArray))
          (for-each [^short shortValue shortArray]
            (.put12 annotation \S (.-index (.addConstantInteger symbolTable shortValue)))))
      (instance? char/1 value)
        (let [charArray (cast char/1 value)]
          (.put12 annotation \[ (alength charArray))
          (for-each [^char charValue charArray]
            (.put12 annotation \C (.-index (.addConstantInteger symbolTable charValue)))))
      (instance? int/1 value)
        (let [intArray (cast int/1 value)]
          (.put12 annotation \[ (alength intArray))
          (for-each [^int intValue intArray]
            (.put12 annotation \I (.-index (.addConstantInteger symbolTable intValue)))))
      (instance? long/1 value)
        (let [longArray (cast long/1 value)]
          (.put12 annotation \[ (alength longArray))
          (for-each [^long longValue longArray]
            (.put12 annotation \J (.-index (.addConstantLong symbolTable longValue)))))
      (instance? float/1 value)
        (let [floatArray (cast float/1 value)]
          (.put12 annotation \[ (alength floatArray))
          (for-each [^float floatValue floatArray]
            (.put12 annotation \F (.-index (.addConstantFloat symbolTable floatValue)))))
      (instance? double/1 value)
        (let [doubleArray (cast double/1 value)]
          (.put12 annotation \[ (alength doubleArray))
          (for-each [^double doubleValue doubleArray]
            (.put12 annotation \D (.-index (.addConstantDouble symbolTable doubleValue)))))
      :else
        (let [symbol (.addConstant symbolTable value)]
          (.put12 annotation (.charAt ".s.IFJDCS" (.-tag symbol)) (.-index symbol)))))

  (method ^:public visitEnum ^void [this ^:final ^String name ^:final ^String descriptor
                                    ^:final ^String value]
    (set! numElementValuePairs (unchecked-inc-int numElementValuePairs))
    (when useNamedValues (.putShort annotation (.addConstantUtf8 symbolTable name)))
    (.putShort (.put12 annotation \e (.addConstantUtf8 symbolTable descriptor))
               (.addConstantUtf8 symbolTable value)))

  (method ^:public visitAnnotation ^AnnotationVisitor [this ^:final ^String name
                                                       ^:final ^String descriptor]
    (set! numElementValuePairs (unchecked-inc-int numElementValuePairs))
    (when useNamedValues (.putShort annotation (.addConstantUtf8 symbolTable name)))
    (.putShort (.put12 annotation \@ (.addConstantUtf8 symbolTable descriptor)) 0)
    (AnnotationWriter. symbolTable true annotation nil))

  (method ^:public visitArray ^AnnotationVisitor [this ^:final ^String name]
    (set! numElementValuePairs (unchecked-inc-int numElementValuePairs))
    (when useNamedValues (.putShort annotation (.addConstantUtf8 symbolTable name)))
    (.put12 annotation \[ 0)
    (AnnotationWriter. symbolTable false annotation nil))

  (method ^:public visitEnd ^void [this]
    (when-not (== numElementValuePairsOffset -1)
      (let [data (.-data annotation)]
        (aset data
              numElementValuePairsOffset
              (unchecked-byte (unsigned-bit-shift-right-int numElementValuePairs 8)))
        (aset data
              (unchecked-add-int numElementValuePairsOffset 1)
              (unchecked-byte numElementValuePairs)))))

  (method computeAnnotationsSize ^int [this ^:final ^String attributeName]
    (when (some? attributeName) (.addConstantUtf8 symbolTable attributeName))
    (let [^:mutable ^int attributeSize 8
          ^:mutable annotationWriter this]
      (while (some? annotationWriter)
        (set! attributeSize
              (unchecked-add-int attributeSize (.-length (.-annotation annotationWriter))))
        (set! annotationWriter (.-previousAnnotation annotationWriter)))
      attributeSize))

  (method ^:static computeAnnotationsSize ^int [^:final ^AnnotationWriter lastRuntimeVisibleAnnotation
                                                ^:final ^AnnotationWriter lastRuntimeInvisibleAnnotation
                                                ^:final ^AnnotationWriter lastRuntimeVisibleTypeAnnotation
                                                ^:final ^AnnotationWriter lastRuntimeInvisibleTypeAnnotation]
    (let [^:mutable ^int size 0]
      (when (some? lastRuntimeVisibleAnnotation)
        (set! size
              (unchecked-add-int size
                                 (.computeAnnotationsSize
                                   lastRuntimeVisibleAnnotation
                                   Constants/RUNTIME_VISIBLE_ANNOTATIONS))))
      (when (some? lastRuntimeInvisibleAnnotation)
        (set! size
              (unchecked-add-int size
                                 (.computeAnnotationsSize
                                   lastRuntimeInvisibleAnnotation
                                   Constants/RUNTIME_INVISIBLE_ANNOTATIONS))))
      (when (some? lastRuntimeVisibleTypeAnnotation)
        (set! size
              (unchecked-add-int size
                                 (.computeAnnotationsSize
                                   lastRuntimeVisibleTypeAnnotation
                                   Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS))))
      (when (some? lastRuntimeInvisibleTypeAnnotation)
        (set! size
              (unchecked-add-int size
                                 (.computeAnnotationsSize
                                   lastRuntimeInvisibleTypeAnnotation
                                   Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS))))
      size))

  (method putAnnotations ^void [this ^:final ^int attributeNameIndex ^:final ^ByteVector output]
    (let [^:mutable ^int attributeLength 2
          ^:mutable ^int numAnnotations 0
          ^:mutable annotationWriter this
          ^:mutable ^AnnotationWriter firstAnnotation nil]
      (while (some? annotationWriter)
        (.visitEnd annotationWriter)
        (set! attributeLength
              (unchecked-add-int attributeLength (.-length (.-annotation annotationWriter))))
        (set! numAnnotations (unchecked-inc-int numAnnotations))
        (set! firstAnnotation annotationWriter)
        (set! annotationWriter (.-previousAnnotation annotationWriter)))
      (.putShort output attributeNameIndex)
      (.putInt output attributeLength)
      (.putShort output numAnnotations)
      (set! annotationWriter firstAnnotation)
      (while (some? annotationWriter)
        (.putByteArray output
                       (.-data (.-annotation annotationWriter))
                       0
                       (.-length (.-annotation annotationWriter)))
        (set! annotationWriter (.-nextAnnotation annotationWriter)))))

  (method ^:static putAnnotations ^void [^:final ^SymbolTable symbolTable
                                         ^:final ^AnnotationWriter lastRuntimeVisibleAnnotation
                                         ^:final ^AnnotationWriter lastRuntimeInvisibleAnnotation
                                         ^:final ^AnnotationWriter lastRuntimeVisibleTypeAnnotation
                                         ^:final ^AnnotationWriter lastRuntimeInvisibleTypeAnnotation
                                         ^:final ^ByteVector output]
    (when (some? lastRuntimeVisibleAnnotation)
      (.putAnnotations lastRuntimeVisibleAnnotation
                       (.addConstantUtf8 symbolTable Constants/RUNTIME_VISIBLE_ANNOTATIONS)
                       output))
    (when (some? lastRuntimeInvisibleAnnotation)
      (.putAnnotations lastRuntimeInvisibleAnnotation
                       (.addConstantUtf8 symbolTable Constants/RUNTIME_INVISIBLE_ANNOTATIONS)
                       output))
    (when (some? lastRuntimeVisibleTypeAnnotation)
      (.putAnnotations lastRuntimeVisibleTypeAnnotation
                       (.addConstantUtf8 symbolTable Constants/RUNTIME_VISIBLE_TYPE_ANNOTATIONS)
                       output))
    (when (some? lastRuntimeInvisibleTypeAnnotation)
      (.putAnnotations lastRuntimeInvisibleTypeAnnotation
                       (.addConstantUtf8 symbolTable Constants/RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)
                       output)))

  (method ^:static computeParameterAnnotationsSize ^int [^:final ^String attributeName
                                                         ^:final ^AnnotationWriter/1 annotationWriters
                                                         ^:final ^int annotableParameterCount]
    (let [^:mutable attributeSize (unchecked-add-int
                                    7
                                    (unchecked-multiply-int 2 annotableParameterCount))]
      (loop [^int i 0]
        (when (< i annotableParameterCount)
          (let [annotationWriter (aget annotationWriters i)]
            (set! attributeSize
                  (unchecked-add-int attributeSize
                                     (if (nil? annotationWriter)
                                         0
                                         (unchecked-subtract-int
                                           (.computeAnnotationsSize annotationWriter attributeName)
                                           8))))
            (recur (unchecked-inc-int i)))))
      attributeSize))

  (method ^:static putParameterAnnotations ^void [^:final ^int attributeNameIndex
                                                  ^:final ^AnnotationWriter/1 annotationWriters
                                                  ^:final ^int annotableParameterCount
                                                  ^:final ^ByteVector output]
    (let [^:mutable attributeLength (unchecked-add-int
                                      1
                                      (unchecked-multiply-int 2 annotableParameterCount))]
      (loop [^int i 0]
        (when (< i annotableParameterCount)
          (let [annotationWriter (aget annotationWriters i)]
            (set! attributeLength
                  (unchecked-add-int attributeLength
                                     (if (nil? annotationWriter)
                                         0
                                         (unchecked-subtract-int
                                           (.computeAnnotationsSize annotationWriter nil)
                                           8))))
            (recur (unchecked-inc-int i)))))
      (.putShort output attributeNameIndex)
      (.putInt output attributeLength)
      (.putByte output annotableParameterCount)
      (loop [^int i 0]
        (when (< i annotableParameterCount)
          (let [^:mutable annotationWriter (aget annotationWriters i)
                ^:mutable ^AnnotationWriter firstAnnotation nil
                ^:mutable ^int numAnnotations 0]
            (while (some? annotationWriter)
              (.visitEnd annotationWriter)
              (set! numAnnotations (unchecked-inc-int numAnnotations))
              (set! firstAnnotation annotationWriter)
              (set! annotationWriter (.-previousAnnotation annotationWriter)))
            (.putShort output numAnnotations)
            (set! annotationWriter firstAnnotation)
            (while (some? annotationWriter)
              (.putByteArray output
                             (.-data (.-annotation annotationWriter))
                             0
                             (.-length (.-annotation annotationWriter)))
              (set! annotationWriter (.-nextAnnotation annotationWriter)))
            (recur (unchecked-inc-int i))))))))
