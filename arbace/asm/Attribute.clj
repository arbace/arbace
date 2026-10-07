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
;; Converted from clojure/asm/Attribute.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public Attribute
  (field ^:public ^:final ^String type)

  (field ^:private ^ByteVector cachedContent)

  (field ^Attribute nextAttribute)

  (constructor ^:protected [this ^:final ^String type]
    (set! (.-type this) type))

  (method ^:public isUnknown ^boolean [this] true)

  (method ^:public isCodeAttribute ^boolean [this] false)

  (method ^:protected ^{Deprecated {:forRemoval false}} getLabels ^Label/1 [this]
    (new Label/1 0))

  (method ^:protected read ^Attribute [this ^:final ^ClassReader classReader ^:final ^int offset
                                       ^:final ^int length ^:final ^char/1 charBuffer
                                       ^:final ^int codeAttributeOffset ^:final ^Label/1 labels]
    (let [attribute (Attribute. type)]
      (set! (.-cachedContent attribute) (ByteVector. (.readBytes classReader offset length)))
      attribute))

  (method ^:public ^:static read ^Attribute [^:final ^Attribute attribute
                                             ^:final ^ClassReader classReader ^:final ^int offset
                                             ^:final ^int length ^:final ^char/1 charBuffer
                                             ^:final ^int codeAttributeOffset
                                             ^:final ^Label/1 labels]
    (.read attribute classReader offset length charBuffer codeAttributeOffset labels))

  (method ^:public ^:static readLabel ^Label [^:final ^ClassReader classReader
                                              ^:final ^int bytecodeOffset ^:final ^Label/1 labels]
    (.readLabel classReader bytecodeOffset labels))

  (method ^:private maybeWrite ^ByteVector [this ^:final ^ClassWriter classWriter
                                            ^:final ^byte/1 code ^:final ^int codeLength
                                            ^:final ^int maxStack ^:final ^int maxLocals]
    (when (nil? cachedContent)
      (set! cachedContent (.write this classWriter code codeLength maxStack maxLocals)))
    cachedContent)

  (method ^:protected write ^ByteVector [this ^:final ^ClassWriter classWriter ^:final ^byte/1 code
                                         ^:final ^int codeLength ^:final ^int maxStack
                                         ^:final ^int maxLocals]
    cachedContent)

  (method ^:public ^:static write ^byte/1 [^:final ^Attribute attribute
                                           ^:final ^ClassWriter classWriter ^:final ^byte/1 code
                                           ^:final ^int codeLength ^:final ^int maxStack
                                           ^:final ^int maxLocals]
    (let [content (.maybeWrite attribute classWriter code codeLength maxStack maxLocals)
          result (new byte/1 (.-length content))]
      (System/arraycopy (.-data content) 0 result 0 (.-length content))
      result))

  (method ^:final getAttributeCount ^int [this]
    (let [^:mutable ^int count 0
          ^:mutable attribute this]
      (while (some? attribute)
        (set! count (unchecked-add-int count 1))
        (set! attribute (.-nextAttribute attribute)))
      count))

  (method ^:final computeAttributesSize ^int [this ^:final ^SymbolTable symbolTable]
    (let [^byte/1 code nil
          ^:const ^int codeLength 0
          ^:const ^int maxStack -1
          ^:const ^int maxLocals -1]
      (.computeAttributesSize this symbolTable code codeLength maxStack maxLocals)))

  (method ^:final computeAttributesSize ^int [this ^:final ^SymbolTable symbolTable
                                              ^:final ^byte/1 code ^:final ^int codeLength
                                              ^:final ^int maxStack ^:final ^int maxLocals]
    (let [classWriter (.-classWriter symbolTable)
          ^:mutable ^int size 0
          ^:mutable attribute this]
      (while (some? attribute)
        (.addConstantUtf8 symbolTable (.-type attribute))
        (set! size
              (unchecked-add-int size
                                 (unchecked-add-int
                                   6
                                   (.-length (.maybeWrite
                                               attribute
                                               classWriter
                                               code
                                               codeLength
                                               maxStack
                                               maxLocals)))))
        (set! attribute (.-nextAttribute attribute)))
      size))

  (method ^:static computeAttributesSize ^int [^:final ^SymbolTable symbolTable
                                               ^:final ^int accessFlags ^:final ^int signatureIndex]
    (let [^:mutable ^int size 0]
      (when (and (not (== (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC) 0))
                 (< (.getMajorVersion symbolTable) Opcodes/V1_5))
        (.addConstantUtf8 symbolTable Constants/SYNTHETIC)
        (set! size (unchecked-add-int size 6)))
      (when-not (== signatureIndex 0)
        (.addConstantUtf8 symbolTable Constants/SIGNATURE)
        (set! size (unchecked-add-int size 8)))
      (when-not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)
        (.addConstantUtf8 symbolTable Constants/DEPRECATED)
        (set! size (unchecked-add-int size 6)))
      size))

  (method ^:final putAttributes ^void [this ^:final ^SymbolTable symbolTable
                                       ^:final ^ByteVector output]
    (let [^byte/1 code nil
          ^:const ^int codeLength 0
          ^:const ^int maxStack -1
          ^:const ^int maxLocals -1]
      (.putAttributes this symbolTable code codeLength maxStack maxLocals output)))

  (method ^:final putAttributes ^void [this ^:final ^SymbolTable symbolTable ^:final ^byte/1 code
                                       ^:final ^int codeLength ^:final ^int maxStack
                                       ^:final ^int maxLocals ^:final ^ByteVector output]
    (let [classWriter (.-classWriter symbolTable)
          ^:mutable attribute this]
      (while (some? attribute)
        (let [attributeContent (.maybeWrite attribute
                                            classWriter
                                            code
                                            codeLength
                                            maxStack
                                            maxLocals)]
          (.putInt (.putShort output (.addConstantUtf8 symbolTable (.-type attribute)))
                   (.-length attributeContent))
          (.putByteArray output (.-data attributeContent) 0 (.-length attributeContent))
          (set! attribute (.-nextAttribute attribute))))))

  (method ^:static putAttributes ^void [^:final ^SymbolTable symbolTable ^:final ^int accessFlags
                                        ^:final ^int signatureIndex ^:final ^ByteVector output]
    (when (and (not (== (bit-and-int accessFlags Opcodes/ACC_SYNTHETIC) 0))
               (< (.getMajorVersion symbolTable) Opcodes/V1_5))
      (.putInt (.putShort output (.addConstantUtf8 symbolTable Constants/SYNTHETIC)) 0))
    (when-not (== signatureIndex 0)
      (.putShort (.putInt (.putShort output (.addConstantUtf8 symbolTable Constants/SIGNATURE)) 2)
                 signatureIndex))
    (when-not (== (bit-and-int accessFlags Opcodes/ACC_DEPRECATED) 0)
      (.putInt (.putShort output (.addConstantUtf8 symbolTable Constants/DEPRECATED)) 0)))

  (defclass ^:static ^:final Set
    (field ^:private ^:static ^:final ^int SIZE_INCREMENT 6)

    (field ^:private ^int size)

    (field ^:private ^Attribute/1 data (new Attribute/1 SIZE_INCREMENT))

    (method addAttributes ^void [this ^:final ^Attribute attributeList]
      (let [^:mutable attribute attributeList]
        (while (some? attribute)
          (when-not (.contains this attribute) (.add this attribute))
          (set! attribute (.-nextAttribute attribute)))))

    (method toArray ^Attribute/1 [this]
      (let [result (new Attribute/1 size)] (System/arraycopy data 0 result 0 size) result))

    (method ^:private contains ^boolean [this ^:final ^Attribute attribute]
      (loop [^int i 0]
        (if (< i size)
            (if (.equals (.-type (aget data i)) (.-type attribute))
                (return true)
                (recur (unchecked-inc-int i)))
            nil))
      false)

    (method ^:private add ^void [this ^:final ^Attribute attribute]
      (when (>= size (alength data))
        (let [newData (new Attribute/1 (unchecked-add-int (alength data) SIZE_INCREMENT))]
          (System/arraycopy data 0 newData 0 size)
          (set! data newData)))
      (aset data (let [old-1 size] (set! size (unchecked-inc-int size)) old-1) attribute))))
