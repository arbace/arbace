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
;; Converted from clojure/asm/ClassVisitor.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:abstract ClassVisitor
  (field ^:protected ^:final ^int api)

  (field ^:protected ^ClassVisitor cv)

  (constructor ^:protected [this ^:final ^int api] (this. api nil))

  (constructor ^:protected [this ^:final ^int api ^:final ^ClassVisitor classVisitor]
    (when (and (and (and (and (and (and (not (== api Opcodes/ASM9)) (not (== api Opcodes/ASM8)))
                                   (not (== api Opcodes/ASM7)))
                              (not (== api Opcodes/ASM6)))
                         (not (== api Opcodes/ASM5)))
                    (not (== api Opcodes/ASM4)))
               (not (== api Opcodes/ASM10_EXPERIMENTAL)))
      (throw (IllegalArgumentException. (java-str "Unsupported api " api))))
    (when (== api Opcodes/ASM10_EXPERIMENTAL) (Constants/checkAsmExperimental this))
    (set! (.-api this) api)
    (set! (.-cv this) classVisitor))

  (method ^:public getDelegate ^ClassVisitor [this] cv)

  (method ^:public visit ^void [this ^:final ^int version ^:final ^int access ^:final ^String name
                                ^:final ^String signature ^:final ^String superName
                                ^:final ^String/1 interfaces]
    (when (and (< api Opcodes/ASM8) (not (== (bit-and-int access Opcodes/ACC_RECORD) 0)))
      (throw (UnsupportedOperationException. "Records requires ASM8")))
    (when (some? cv) (.visit cv version access name signature superName interfaces)))

  (method ^:public visitSource ^void [this ^:final ^String source ^:final ^String debug]
    (when (some? cv) (.visitSource cv source debug)))

  (method ^:public visitModule ^ModuleVisitor [this ^:final ^String name ^:final ^int access
                                               ^:final ^String version]
    (when (< api Opcodes/ASM6) (throw (UnsupportedOperationException. "Module requires ASM6")))
    (when (some? cv) (.visitModule cv name access version)))

  (method ^:public visitNestHost ^void [this ^:final ^String nestHost]
    (when (< api Opcodes/ASM7) (throw (UnsupportedOperationException. "NestHost requires ASM7")))
    (when (some? cv) (.visitNestHost cv nestHost)))

  (method ^:public visitOuterClass ^void [this ^:final ^String owner ^:final ^String name
                                          ^:final ^String descriptor]
    (when (some? cv) (.visitOuterClass cv owner name descriptor)))

  (method ^:public visitAnnotation ^AnnotationVisitor [this ^:final ^String descriptor
                                                       ^:final ^boolean visible]
    (when (some? cv) (.visitAnnotation cv descriptor visible)))

  (method ^:public visitTypeAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (when (< api Opcodes/ASM5)
      (throw (UnsupportedOperationException. "TypeAnnotation requires ASM5")))
    (when (some? cv) (.visitTypeAnnotation cv typeRef typePath descriptor visible)))

  (method ^:public visitAttribute ^void [this ^:final ^Attribute attribute]
    (when (some? cv) (.visitAttribute cv attribute)))

  (method ^:public visitNestMember ^void [this ^:final ^String nestMember]
    (when (< api Opcodes/ASM7) (throw (UnsupportedOperationException. "NestMember requires ASM7")))
    (when (some? cv) (.visitNestMember cv nestMember)))

  (method ^:public visitPermittedSubclass ^void [this ^:final ^String permittedSubclass]
    (when (< api Opcodes/ASM9)
      (throw (UnsupportedOperationException. "PermittedSubclasses requires ASM9")))
    (when (some? cv) (.visitPermittedSubclass cv permittedSubclass)))

  (method ^:public visitInnerClass ^void [this ^:final ^String name ^:final ^String outerName
                                          ^:final ^String innerName ^:final ^int access]
    (when (some? cv) (.visitInnerClass cv name outerName innerName access)))

  (method ^:public visitRecordComponent ^RecordComponentVisitor [this ^:final ^String name
                                                                 ^:final ^String descriptor
                                                                 ^:final ^String signature]
    (when (< api Opcodes/ASM8) (throw (UnsupportedOperationException. "Record requires ASM8")))
    (when (some? cv) (.visitRecordComponent cv name descriptor signature)))

  (method ^:public visitField ^FieldVisitor [this ^:final ^int access ^:final ^String name
                                             ^:final ^String descriptor ^:final ^String signature
                                             ^:final value]
    (when (some? cv) (.visitField cv access name descriptor signature value)))

  (method ^:public visitMethod ^MethodVisitor [this ^:final ^int access ^:final ^String name
                                               ^:final ^String descriptor ^:final ^String signature
                                               ^:final ^String/1 exceptions]
    (when (some? cv) (.visitMethod cv access name descriptor signature exceptions)))

  (method ^:public visitEnd ^void [this] (when (some? cv) (.visitEnd cv))))
