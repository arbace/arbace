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
;; Converted from clojure/asm/ModuleWriter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:final ModuleWriter
  :extends ModuleVisitor

  (field ^:private ^:final ^SymbolTable symbolTable)

  (field ^:private ^:final ^int moduleNameIndex)

  (field ^:private ^:final ^int moduleFlags)

  (field ^:private ^:final ^int moduleVersionIndex)

  (field ^:private ^int requiresCount)

  (field ^:private ^:final ^ByteVector requires)

  (field ^:private ^int exportsCount)

  (field ^:private ^:final ^ByteVector exports)

  (field ^:private ^int opensCount)

  (field ^:private ^:final ^ByteVector opens)

  (field ^:private ^int usesCount)

  (field ^:private ^:final ^ByteVector usesIndex)

  (field ^:private ^int providesCount)

  (field ^:private ^:final ^ByteVector provides)

  (field ^:private ^int packageCount)

  (field ^:private ^:final ^ByteVector packageIndex)

  (field ^:private ^int mainClassIndex)

  (constructor [this ^:final ^SymbolTable symbolTable ^:final ^int name ^:final ^int access
                ^:final ^int version]
    (super. Opcodes/ASM9)
    (set! (.-symbolTable this) symbolTable)
    (set! (.-moduleNameIndex this) name)
    (set! (.-moduleFlags this) access)
    (set! (.-moduleVersionIndex this) version)
    (set! (.-requires this) (ByteVector.))
    (set! (.-exports this) (ByteVector.))
    (set! (.-opens this) (ByteVector.))
    (set! (.-usesIndex this) (ByteVector.))
    (set! (.-provides this) (ByteVector.))
    (set! (.-packageIndex this) (ByteVector.)))

  (method ^:public visitMainClass ^void [this ^:final ^String mainClass]
    (set! (.-mainClassIndex this) (.-index (.addConstantClass symbolTable mainClass))))

  (method ^:public visitPackage ^void [this ^:final ^String packaze]
    (.putShort packageIndex (.-index (.addConstantPackage symbolTable packaze)))
    (set! packageCount (unchecked-inc-int packageCount)))

  (method ^:public visitRequire ^void [this ^:final ^String module ^:final ^int access
                                       ^:final ^String version]
    (.putShort (.putShort (.putShort requires (.-index (.addConstantModule symbolTable module)))
                          access)
               (if (nil? version) 0 (.addConstantUtf8 symbolTable version)))
    (set! requiresCount (unchecked-inc-int requiresCount)))

  (method ^:public visitExport ^void [this ^:final ^String packaze ^:final ^int access &
                                      ^:final ^String/1 modules]
    (.putShort (.putShort exports (.-index (.addConstantPackage symbolTable packaze))) access)
    (if (nil? modules)
        (.putShort exports 0)
        (do
          (.putShort exports (alength modules))
          (for-each [^String module modules]
            (.putShort exports (.-index (.addConstantModule symbolTable module))))))
    (set! exportsCount (unchecked-inc-int exportsCount)))

  (method ^:public visitOpen ^void [this ^:final ^String packaze ^:final ^int access &
                                    ^:final ^String/1 modules]
    (.putShort (.putShort opens (.-index (.addConstantPackage symbolTable packaze))) access)
    (if (nil? modules)
        (.putShort opens 0)
        (do
          (.putShort opens (alength modules))
          (for-each [^String module modules]
            (.putShort opens (.-index (.addConstantModule symbolTable module))))))
    (set! opensCount (unchecked-inc-int opensCount)))

  (method ^:public visitUse ^void [this ^:final ^String service]
    (.putShort usesIndex (.-index (.addConstantClass symbolTable service)))
    (set! usesCount (unchecked-inc-int usesCount)))

  (method ^:public visitProvide ^void [this ^:final ^String service & ^:final ^String/1 providers]
    (.putShort provides (.-index (.addConstantClass symbolTable service)))
    (.putShort provides (alength providers))
    (for-each [^String provider providers]
      (.putShort provides (.-index (.addConstantClass symbolTable provider))))
    (set! providesCount (unchecked-inc-int providesCount)))

  (method ^:public visitEnd ^void [this])

  (method getAttributeCount ^int [this]
    (unchecked-add-int (unchecked-add-int 1 (if (> packageCount 0) 1 0))
                       (if (> mainClassIndex 0) 1 0)))

  (method computeAttributesSize ^int [this]
    (.addConstantUtf8 symbolTable Constants/MODULE)
    (let [^:mutable size (unchecked-add-int
                           (unchecked-add-int
                             (unchecked-add-int
                               (unchecked-add-int
                                 (unchecked-add-int 22 (.-length requires))
                                 (.-length exports))
                               (.-length opens))
                             (.-length usesIndex))
                           (.-length provides))]
      (when (> packageCount 0)
        (.addConstantUtf8 symbolTable Constants/MODULE_PACKAGES)
        (set! size (unchecked-add-int size (unchecked-add-int 8 (.-length packageIndex)))))
      (when (> mainClassIndex 0)
        (.addConstantUtf8 symbolTable Constants/MODULE_MAIN_CLASS)
        (set! size (unchecked-add-int size 8)))
      size))

  (method putAttributes ^void [this ^:final ^ByteVector output]
    (let [moduleAttributeLength (unchecked-add-int
                                  (unchecked-add-int
                                    (unchecked-add-int
                                      (unchecked-add-int
                                        (unchecked-add-int 16 (.-length requires))
                                        (.-length exports))
                                      (.-length opens))
                                    (.-length usesIndex))
                                  (.-length provides))]
      (.putByteArray
        (.putShort (.putByteArray
                     (.putShort (.putByteArray
                                  (.putShort (.putByteArray
                                               (.putShort
                                                 (.putByteArray
                                                   (.putShort
                                                     (.putShort
                                                       (.putShort
                                                         (.putShort
                                                           (.putInt
                                                             (.putShort
                                                               output
                                                               (.addConstantUtf8
                                                                 symbolTable
                                                                 Constants/MODULE))
                                                             moduleAttributeLength)
                                                           moduleNameIndex)
                                                         moduleFlags)
                                                       moduleVersionIndex)
                                                     requiresCount)
                                                   (.-data requires)
                                                   0
                                                   (.-length requires))
                                                 exportsCount)
                                               (.-data exports)
                                               0
                                               (.-length exports))
                                             opensCount)
                                  (.-data opens)
                                  0
                                  (.-length opens))
                                usesCount)
                     (.-data usesIndex)
                     0
                     (.-length usesIndex))
                   providesCount)
        (.-data provides)
        0
        (.-length provides))
      (when (> packageCount 0)
        (.putByteArray
          (.putShort (.putInt (.putShort output
                                         (.addConstantUtf8 symbolTable Constants/MODULE_PACKAGES))
                              (unchecked-add-int 2 (.-length packageIndex)))
                     packageCount)
          (.-data packageIndex)
          0
          (.-length packageIndex)))
      (when (> mainClassIndex 0)
        (.putShort (.putInt (.putShort output
                                       (.addConstantUtf8 symbolTable Constants/MODULE_MAIN_CLASS))
                            2)
                   mainClassIndex)))))
