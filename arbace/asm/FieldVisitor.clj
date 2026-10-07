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
;; Converted from clojure/asm/FieldVisitor.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:abstract FieldVisitor
  (field ^:protected ^:final ^int api)

  (field ^:protected ^FieldVisitor fv)

  (constructor ^:protected [this ^:final ^int api] (this. api nil))

  (constructor ^:protected [this ^:final ^int api ^:final ^FieldVisitor fieldVisitor]
    (when (and (and (and (and (and (and (not (== api Opcodes/ASM9)) (not (== api Opcodes/ASM8)))
                                   (not (== api Opcodes/ASM7)))
                              (not (== api Opcodes/ASM6)))
                         (not (== api Opcodes/ASM5)))
                    (not (== api Opcodes/ASM4)))
               (not (== api Opcodes/ASM10_EXPERIMENTAL)))
      (throw (IllegalArgumentException. (java-str "Unsupported api " api))))
    (when (== api Opcodes/ASM10_EXPERIMENTAL) (Constants/checkAsmExperimental this))
    (set! (.-api this) api)
    (set! (.-fv this) fieldVisitor))

  (method ^:public getDelegate ^FieldVisitor [this] fv)

  (method ^:public visitAnnotation ^AnnotationVisitor [this ^:final ^String descriptor
                                                       ^:final ^boolean visible]
    (when (some? fv) (.visitAnnotation fv descriptor visible)))

  (method ^:public visitTypeAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                           ^:final ^TypePath typePath
                                                           ^:final ^String descriptor
                                                           ^:final ^boolean visible]
    (when (< api Opcodes/ASM5)
      (throw (UnsupportedOperationException. "This feature requires ASM5")))
    (when (some? fv) (.visitTypeAnnotation fv typeRef typePath descriptor visible)))

  (method ^:public visitAttribute ^void [this ^:final ^Attribute attribute]
    (when (some? fv) (.visitAttribute fv attribute)))

  (method ^:public visitEnd ^void [this] (when (some? fv) (.visitEnd fv))))
