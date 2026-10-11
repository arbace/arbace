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
;; Converted from clojure/asm/commons/LocalVariablesSorter.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.commons)

(import '(arbace.asm AnnotationVisitor Label MethodVisitor Opcodes Type TypePath))

(defclass ^:public LocalVariablesSorter
  :extends MethodVisitor

  (field ^:private ^:static ^:final ^Type OBJECT_TYPE (Type/getObjectType "java/lang/Object"))

  (field ^:private ^int/1 remappedVariableIndices (new int/1 40))

  (field ^:private ^Object/1 remappedLocalTypes (new Object/1 20))

  (field ^:protected ^:final ^int firstLocal)

  (field ^:protected ^int nextLocal)

  (constructor ^:public [this ^:final ^int access ^:final ^String descriptor
                         ^:final ^MethodVisitor methodVisitor]
    (this. Opcodes/ASM9 access descriptor methodVisitor)
    (when-not (identical? (.getClass this) LocalVariablesSorter) (throw (IllegalStateException.))))

  (constructor ^:protected [this ^:final ^int api ^:final ^int access ^:final ^String descriptor
                            ^:final ^MethodVisitor methodVisitor]
    (super. api methodVisitor)
    (set! nextLocal (if (== (bit-and-int Opcodes/ACC_STATIC access) 0) 1 0))
    (for-each [^Type argumentType (Type/getArgumentTypes descriptor)]
      (set! nextLocal (unchecked-add-int nextLocal (.getSize argumentType))))
    (set! firstLocal nextLocal))

  (method ^:public visitVarInsn ^void [this ^:final ^int opcode ^:final ^int varIndex]
    (let [^:mutable ^Type varType nil]
      (switch opcode
        (Opcodes/LLOAD Opcodes/LSTORE) (set! varType Type/LONG_TYPE)
        (Opcodes/DLOAD Opcodes/DSTORE) (set! varType Type/DOUBLE_TYPE)
        (Opcodes/FLOAD Opcodes/FSTORE) (set! varType Type/FLOAT_TYPE)
        (Opcodes/ILOAD Opcodes/ISTORE) (set! varType Type/INT_TYPE)
        (Opcodes/ALOAD Opcodes/ASTORE Opcodes/RET) (set! varType OBJECT_TYPE)
        (throw (IllegalArgumentException. (java-str "Invalid opcode " opcode))))
      (.visitVarInsn super opcode (.remap this varIndex varType))))

  (method ^:public visitIincInsn ^void [this ^:final ^int varIndex ^:final ^int increment]
    (.visitIincInsn super (.remap this varIndex Type/INT_TYPE) increment))

  (method ^:public visitMaxs ^void [this ^:final ^int maxStack ^:final ^int maxLocals]
    (.visitMaxs super maxStack nextLocal))

  (method ^:public visitLocalVariable ^void [this ^:final ^String name ^:final ^String descriptor
                                             ^:final ^String signature ^:final ^Label start
                                             ^:final ^Label end ^:final ^int index]
    (let [remappedIndex (.remap this index (Type/getType descriptor))]
      (.visitLocalVariable super name descriptor signature start end remappedIndex)))

  (method ^:public visitLocalVariableAnnotation ^AnnotationVisitor [this ^:final ^int typeRef
                                                                    ^:final ^TypePath typePath
                                                                    ^:final ^Label/1 start
                                                                    ^:final ^Label/1 end
                                                                    ^:final ^int/1 index
                                                                    ^:final ^String descriptor
                                                                    ^:final ^boolean visible]
    (let [type (Type/getType descriptor)
          remappedIndex (new int/1 (alength index))]
      (loop [^int i 0]
        (when (< i (alength remappedIndex))
          (aset remappedIndex i (.remap this (aget index i) type))
          (recur (unchecked-inc-int i))))
      (.visitLocalVariableAnnotation super
                                     typeRef
                                     typePath
                                     start
                                     end
                                     remappedIndex
                                     descriptor
                                     visible)))

  (method ^:public visitFrame ^void [this ^:final ^int type ^:final ^int numLocal
                                     ^:final ^Object/1 local ^:final ^int numStack
                                     ^:final ^Object/1 stack]
    (when-not (== type Opcodes/F_NEW)
      (throw (IllegalArgumentException.
               "LocalVariablesSorter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)")))
    (let [oldRemappedLocals (new Object/1 (alength remappedLocalTypes))]
      (System/arraycopy remappedLocalTypes 0 oldRemappedLocals 0 (alength oldRemappedLocals))
      (.updateNewLocals this remappedLocalTypes)
      (let [^:mutable ^int oldVar 0]
        (loop [^int i 0]
          (when (< i numLocal)
            (let [localType (aget local i)]
              (when-not (identical? localType Opcodes/TOP)
                (let [^:mutable varType OBJECT_TYPE]
                  (cond
                    (identical? localType Opcodes/INTEGER) (set! varType Type/INT_TYPE)
                    (identical? localType Opcodes/FLOAT) (set! varType Type/FLOAT_TYPE)
                    (identical? localType Opcodes/LONG) (set! varType Type/LONG_TYPE)
                    (identical? localType Opcodes/DOUBLE) (set! varType Type/DOUBLE_TYPE)
                    (instance? String localType)
                      (set! varType (Type/getObjectType (cast String localType))))
                  (.setFrameLocal this (.remap this oldVar varType) localType)))
              (set! oldVar
                    (unchecked-add-int oldVar
                                       (if (or (identical? localType Opcodes/LONG)
                                               (identical? localType Opcodes/DOUBLE))
                                           2
                                           1)))
              (recur (unchecked-inc-int i)))))
        (set! oldVar 0)
        (let [^:mutable ^int newVar 0
              ^:mutable ^int remappedNumLocal 0]
          (while (< oldVar (alength remappedLocalTypes))
            (let [localType (aget remappedLocalTypes oldVar)]
              (set! oldVar
                    (unchecked-add-int oldVar
                                       (if (or (identical? localType Opcodes/LONG)
                                               (identical? localType Opcodes/DOUBLE))
                                           2
                                           1)))
              (if (and (some? localType) (not (identical? localType Opcodes/TOP)))
                  (do
                    (aset remappedLocalTypes newVar localType)
                    (set! newVar (unchecked-inc-int newVar))
                    (set! remappedNumLocal newVar))
                  (do
                    (aset remappedLocalTypes newVar Opcodes/TOP)
                    (set! newVar (unchecked-inc-int newVar))))))
          (.visitFrame super type remappedNumLocal remappedLocalTypes numStack stack)
          (set! remappedLocalTypes oldRemappedLocals)))))

  (method ^:public newLocal ^int [this ^:final ^Type type]
    (let [^:mutable ^Object localType nil]
      (switch (.getSort type)
        (Type/BOOLEAN Type/CHAR Type/BYTE Type/SHORT Type/INT) (set! localType Opcodes/INTEGER)
        Type/FLOAT (set! localType Opcodes/FLOAT)
        Type/LONG (set! localType Opcodes/LONG)
        Type/DOUBLE (set! localType Opcodes/DOUBLE)
        Type/ARRAY (set! localType (.getDescriptor type))
        Type/OBJECT (set! localType (.getInternalName type))
        (throw (AssertionError.)))
      (let [local (.newLocalMapping this type)]
        (.setLocalType this local type)
        (.setFrameLocal this local localType)
        local)))

  (method ^:protected updateNewLocals ^void [this ^:final ^Object/1 newLocals])

  (method ^:protected setLocalType ^void [this ^:final ^int local ^:final ^Type type])

  (method ^:private setFrameLocal ^void [this ^:final ^int local ^:final type]
    (let [numLocals (alength remappedLocalTypes)]
      (when (>= local numLocals)
        (let [newRemappedLocalTypes (new Object/1
                                         (Math/max
                                           (unchecked-multiply-int 2 numLocals)
                                           (unchecked-add-int local 1)))]
          (System/arraycopy remappedLocalTypes 0 newRemappedLocalTypes 0 numLocals)
          (set! remappedLocalTypes newRemappedLocalTypes)))
      (aset remappedLocalTypes local type)))

  (method ^:private remap ^int [this ^:final ^int varIndex ^:final ^Type type]
    (if (<= (unchecked-add-int varIndex (.getSize type)) firstLocal)
        varIndex
        (let [key (unchecked-subtract-int
                    (unchecked-add-int (unchecked-multiply-int 2 varIndex) (.getSize type))
                    1)
              size (alength remappedVariableIndices)]
          (when (>= key size)
            (let [newRemappedVariableIndices (new
                                               int/1
                                               (Math/max
                                                 (unchecked-multiply-int 2 size)
                                                 (unchecked-add-int key 1)))]
              (System/arraycopy remappedVariableIndices 0 newRemappedVariableIndices 0 size)
              (set! remappedVariableIndices newRemappedVariableIndices)))
          (let [^:mutable value (aget remappedVariableIndices key)]
            (if (== value 0)
                (do
                  (set! value (.newLocalMapping this type))
                  (.setLocalType this value type)
                  (aset remappedVariableIndices key (unchecked-add-int value 1)))
                (set! value (unchecked-dec-int value)))
            value))))

  (method ^:protected newLocalMapping ^int [this ^:final ^Type type]
    (let [local nextLocal] (set! nextLocal (unchecked-add-int nextLocal (.getSize type))) local)))
