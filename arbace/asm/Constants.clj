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
;; Converted from clojure/asm/Constants.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(import '(java.io DataInputStream IOException InputStream)
        '(java.util.regex Pattern))

(defclass ^:final Constants
  (field ^:static ^:final ^String CONSTANT_VALUE "ConstantValue")

  (field ^:static ^:final ^String CODE "Code")

  (field ^:static ^:final ^String STACK_MAP_TABLE "StackMapTable")

  (field ^:static ^:final ^String EXCEPTIONS "Exceptions")

  (field ^:static ^:final ^String INNER_CLASSES "InnerClasses")

  (field ^:static ^:final ^String ENCLOSING_METHOD "EnclosingMethod")

  (field ^:static ^:final ^String SYNTHETIC "Synthetic")

  (field ^:static ^:final ^String SIGNATURE "Signature")

  (field ^:static ^:final ^String SOURCE_FILE "SourceFile")

  (field ^:static ^:final ^String SOURCE_DEBUG_EXTENSION "SourceDebugExtension")

  (field ^:static ^:final ^String LINE_NUMBER_TABLE "LineNumberTable")

  (field ^:static ^:final ^String LOCAL_VARIABLE_TABLE "LocalVariableTable")

  (field ^:static ^:final ^String LOCAL_VARIABLE_TYPE_TABLE "LocalVariableTypeTable")

  (field ^:static ^:final ^String DEPRECATED "Deprecated")

  (field ^:static ^:final ^String RUNTIME_VISIBLE_ANNOTATIONS "RuntimeVisibleAnnotations")

  (field ^:static ^:final ^String RUNTIME_INVISIBLE_ANNOTATIONS "RuntimeInvisibleAnnotations")

  (field ^:static ^:final ^String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS
    "RuntimeVisibleParameterAnnotations")

  (field ^:static ^:final ^String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS
    "RuntimeInvisibleParameterAnnotations")

  (field ^:static ^:final ^String RUNTIME_VISIBLE_TYPE_ANNOTATIONS "RuntimeVisibleTypeAnnotations")

  (field ^:static ^:final ^String RUNTIME_INVISIBLE_TYPE_ANNOTATIONS
    "RuntimeInvisibleTypeAnnotations")

  (field ^:static ^:final ^String ANNOTATION_DEFAULT "AnnotationDefault")

  (field ^:static ^:final ^String BOOTSTRAP_METHODS "BootstrapMethods")

  (field ^:static ^:final ^String METHOD_PARAMETERS "MethodParameters")

  (field ^:static ^:final ^String MODULE "Module")

  (field ^:static ^:final ^String MODULE_PACKAGES "ModulePackages")

  (field ^:static ^:final ^String MODULE_MAIN_CLASS "ModuleMainClass")

  (field ^:static ^:final ^String NEST_HOST "NestHost")

  (field ^:static ^:final ^String NEST_MEMBERS "NestMembers")

  (field ^:static ^:final ^String PERMITTED_SUBCLASSES "PermittedSubclasses")

  (field ^:static ^:final ^String RECORD "Record")

  (field ^:static ^:final ^int ACC_CONSTRUCTOR 0x40000)

  (field ^:static ^:final ^int F_INSERT 256)

  (field ^:static ^:final ^int LDC_W 19)

  (field ^:static ^:final ^int LDC2_W 20)

  (field ^:static ^:final ^int ILOAD_0 26)

  (field ^:static ^:final ^int ILOAD_1 27)

  (field ^:static ^:final ^int ILOAD_2 28)

  (field ^:static ^:final ^int ILOAD_3 29)

  (field ^:static ^:final ^int LLOAD_0 30)

  (field ^:static ^:final ^int LLOAD_1 31)

  (field ^:static ^:final ^int LLOAD_2 32)

  (field ^:static ^:final ^int LLOAD_3 33)

  (field ^:static ^:final ^int FLOAD_0 34)

  (field ^:static ^:final ^int FLOAD_1 35)

  (field ^:static ^:final ^int FLOAD_2 36)

  (field ^:static ^:final ^int FLOAD_3 37)

  (field ^:static ^:final ^int DLOAD_0 38)

  (field ^:static ^:final ^int DLOAD_1 39)

  (field ^:static ^:final ^int DLOAD_2 40)

  (field ^:static ^:final ^int DLOAD_3 41)

  (field ^:static ^:final ^int ALOAD_0 42)

  (field ^:static ^:final ^int ALOAD_1 43)

  (field ^:static ^:final ^int ALOAD_2 44)

  (field ^:static ^:final ^int ALOAD_3 45)

  (field ^:static ^:final ^int ISTORE_0 59)

  (field ^:static ^:final ^int ISTORE_1 60)

  (field ^:static ^:final ^int ISTORE_2 61)

  (field ^:static ^:final ^int ISTORE_3 62)

  (field ^:static ^:final ^int LSTORE_0 63)

  (field ^:static ^:final ^int LSTORE_1 64)

  (field ^:static ^:final ^int LSTORE_2 65)

  (field ^:static ^:final ^int LSTORE_3 66)

  (field ^:static ^:final ^int FSTORE_0 67)

  (field ^:static ^:final ^int FSTORE_1 68)

  (field ^:static ^:final ^int FSTORE_2 69)

  (field ^:static ^:final ^int FSTORE_3 70)

  (field ^:static ^:final ^int DSTORE_0 71)

  (field ^:static ^:final ^int DSTORE_1 72)

  (field ^:static ^:final ^int DSTORE_2 73)

  (field ^:static ^:final ^int DSTORE_3 74)

  (field ^:static ^:final ^int ASTORE_0 75)

  (field ^:static ^:final ^int ASTORE_1 76)

  (field ^:static ^:final ^int ASTORE_2 77)

  (field ^:static ^:final ^int ASTORE_3 78)

  (field ^:static ^:final ^int WIDE 196)

  (field ^:static ^:final ^int GOTO_W 200)

  (field ^:static ^:final ^int JSR_W 201)

  (field ^:static ^:final ^int WIDE_JUMP_OPCODE_DELTA (unchecked-subtract-int GOTO_W Opcodes/GOTO))

  (field ^:static ^:final ^int ASM_OPCODE_DELTA 49)

  (field ^:static ^:final ^int ASM_IFNULL_OPCODE_DELTA 20)

  (field ^:static ^:final ^int ASM_IFEQ (unchecked-add-int Opcodes/IFEQ ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFNE (unchecked-add-int Opcodes/IFNE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFLT (unchecked-add-int Opcodes/IFLT ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFGE (unchecked-add-int Opcodes/IFGE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFGT (unchecked-add-int Opcodes/IFGT ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFLE (unchecked-add-int Opcodes/IFLE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPEQ (unchecked-add-int Opcodes/IF_ICMPEQ ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPNE (unchecked-add-int Opcodes/IF_ICMPNE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPLT (unchecked-add-int Opcodes/IF_ICMPLT ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPGE (unchecked-add-int Opcodes/IF_ICMPGE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPGT (unchecked-add-int Opcodes/IF_ICMPGT ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ICMPLE (unchecked-add-int Opcodes/IF_ICMPLE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ACMPEQ (unchecked-add-int Opcodes/IF_ACMPEQ ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IF_ACMPNE (unchecked-add-int Opcodes/IF_ACMPNE ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_GOTO (unchecked-add-int Opcodes/GOTO ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_JSR (unchecked-add-int Opcodes/JSR ASM_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFNULL
    (unchecked-add-int Opcodes/IFNULL ASM_IFNULL_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_IFNONNULL
    (unchecked-add-int Opcodes/IFNONNULL ASM_IFNULL_OPCODE_DELTA))

  (field ^:static ^:final ^int ASM_GOTO_W 220)

  (constructor ^:private [this])

  (method ^:static checkAsmExperimental ^void [^:final caller]
    (let [^{:tag (Class ?)} callerClass (.getClass caller)
          internalName (.replace (.getName callerClass) \. \/)]
      (when-not (Constants/isWhitelisted internalName)
        (Constants/checkIsPreview
          (.getResourceAsStream (.getClassLoader callerClass) (java-str internalName ".class"))))))

  (method ^:static isWhitelisted ^boolean [^:final ^String internalName]
    (if (not (.startsWith internalName "org/objectweb/asm/"))
        false
        (let [member "(Annotation|Class|Field|Method|Module|RecordComponent|Signature)"]
          (or (or (.contains internalName "Test$")
                  (Pattern/matches
                    (java-str "org/objectweb/asm/util/Trace" member "Visitor(\\$.*)?")
                    internalName))
              (Pattern/matches (java-str "org/objectweb/asm/util/Check" member "Adapter(\\$.*)?")
                               internalName)))))

  (method ^:static checkIsPreview ^void [^:final ^InputStream classInputStream]
    (when (nil? classInputStream)
      (throw (IllegalStateException. "Bytecode not available, can't check class version")))
    (let [^:mutable ^int minorVersion 0]
      (try
        (with-resources [callerClassStream (DataInputStream. classInputStream)]
          (.readInt callerClassStream)
          (set! minorVersion (.readUnsignedShort callerClassStream)))
        (catch IOException ioe
          (throw (IllegalStateException. "I/O error, can't check class version" ioe))))
      (when-not (== minorVersion 0xFFFF)
        (throw (IllegalStateException.
                 "ASM10_EXPERIMENTAL can only be used by classes compiled with --enable-preview"))))))
