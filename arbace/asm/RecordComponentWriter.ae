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
;; Converted from clojure/asm/RecordComponentWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final RecordComponentWriter
  :extends RecordComponentVisitor

  (field ^:private ^:final ^SymbolTable symbolTable)

  (field ^:private ^:final ^int nameIndex)

  (field ^:private ^:final ^int descriptorIndex)

  (field ^:private ^int signatureIndex)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeVisibleTypeAnnotation)

  (field ^:private ^AnnotationWriter lastRuntimeInvisibleTypeAnnotation)

  (field ^:private ^Attribute firstAttribute)

  (constructor [this ^:final ^SymbolTable symbolTable ^:final ^String name
                ^:final ^String descriptor ^:final ^String signature]
    (super. Opcodes/ASM9)
    (set! (.-symbolTable this) symbolTable)
    (set! (.-nameIndex this) (.addConstantUtf8 symbolTable name))
    (set! (.-descriptorIndex this) (.addConstantUtf8 symbolTable descriptor))
    (when (some? signature) (set! (.-signatureIndex this) (.addConstantUtf8 symbolTable signature))))

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

  (method ^:public visitAttribute ^void [this ^:final ^Attribute attribute]
    (set! (.-nextAttribute attribute) firstAttribute)
    (set! firstAttribute attribute))

  (method ^:public visitEnd ^void [this])

  (method computeRecordComponentInfoSize ^int [this]
    (let [^:mutable ^int size 6]
      (set! size
            (unchecked-add-int size (Attribute/computeAttributesSize symbolTable 0 signatureIndex)))
      (set! size
            (unchecked-add-int size
                               (AnnotationWriter/computeAnnotationsSize
                                 lastRuntimeVisibleAnnotation
                                 lastRuntimeInvisibleAnnotation
                                 lastRuntimeVisibleTypeAnnotation
                                 lastRuntimeInvisibleTypeAnnotation)))
      (when (some? firstAttribute)
        (set! size (unchecked-add-int size (.computeAttributesSize firstAttribute symbolTable))))
      size))

  (method putRecordComponentInfo ^void [this ^:final ^ByteVector output]
    (.putShort (.putShort output nameIndex) descriptorIndex)
    (let [^:mutable ^int attributesCount 0]
      (when-not (== signatureIndex 0) (set! attributesCount (unchecked-inc-int attributesCount)))
      (when (some? lastRuntimeVisibleAnnotation)
        (set! attributesCount (unchecked-inc-int attributesCount)))
      (when (some? lastRuntimeInvisibleAnnotation)
        (set! attributesCount (unchecked-inc-int attributesCount)))
      (when (some? lastRuntimeVisibleTypeAnnotation)
        (set! attributesCount (unchecked-inc-int attributesCount)))
      (when (some? lastRuntimeInvisibleTypeAnnotation)
        (set! attributesCount (unchecked-inc-int attributesCount)))
      (when (some? firstAttribute)
        (set! attributesCount
              (unchecked-add-int attributesCount (.getAttributeCount firstAttribute))))
      (.putShort output attributesCount)
      (Attribute/putAttributes symbolTable 0 signatureIndex output)
      (AnnotationWriter/putAnnotations symbolTable
                                       lastRuntimeVisibleAnnotation
                                       lastRuntimeInvisibleAnnotation
                                       lastRuntimeVisibleTypeAnnotation
                                       lastRuntimeInvisibleTypeAnnotation
                                       output)
      (when (some? firstAttribute) (.putAttributes firstAttribute symbolTable output))))

  (method ^:final collectAttributePrototypes ^void [this ^:final ^Attribute$Set attributePrototypes]
    (.addAttributes attributePrototypes firstAttribute)))
