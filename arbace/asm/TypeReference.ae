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
;; Converted from clojure/asm/TypeReference.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public TypeReference
  (field ^:public ^:static ^:final ^int CLASS_TYPE_PARAMETER 0x00)

  (field ^:public ^:static ^:final ^int METHOD_TYPE_PARAMETER 0x01)

  (field ^:public ^:static ^:final ^int CLASS_EXTENDS 0x10)

  (field ^:public ^:static ^:final ^int CLASS_TYPE_PARAMETER_BOUND 0x11)

  (field ^:public ^:static ^:final ^int METHOD_TYPE_PARAMETER_BOUND 0x12)

  (field ^:public ^:static ^:final ^int FIELD 0x13)

  (field ^:public ^:static ^:final ^int METHOD_RETURN 0x14)

  (field ^:public ^:static ^:final ^int METHOD_RECEIVER 0x15)

  (field ^:public ^:static ^:final ^int METHOD_FORMAL_PARAMETER 0x16)

  (field ^:public ^:static ^:final ^int THROWS 0x17)

  (field ^:public ^:static ^:final ^int LOCAL_VARIABLE 0x40)

  (field ^:public ^:static ^:final ^int RESOURCE_VARIABLE 0x41)

  (field ^:public ^:static ^:final ^int EXCEPTION_PARAMETER 0x42)

  (field ^:public ^:static ^:final ^int INSTANCEOF 0x43)

  (field ^:public ^:static ^:final ^int NEW 0x44)

  (field ^:public ^:static ^:final ^int CONSTRUCTOR_REFERENCE 0x45)

  (field ^:public ^:static ^:final ^int METHOD_REFERENCE 0x46)

  (field ^:public ^:static ^:final ^int CAST 0x47)

  (field ^:public ^:static ^:final ^int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT 0x48)

  (field ^:public ^:static ^:final ^int METHOD_INVOCATION_TYPE_ARGUMENT 0x49)

  (field ^:public ^:static ^:final ^int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT 0x4A)

  (field ^:public ^:static ^:final ^int METHOD_REFERENCE_TYPE_ARGUMENT 0x4B)

  (field ^:private ^:final ^int targetTypeAndInfo)

  (constructor ^:public [this ^:final ^int typeRef]
    (set! (.-targetTypeAndInfo this) typeRef))

  (method ^:public ^:static newTypeReference ^TypeReference [^:final ^int sort]
    (TypeReference. (bit-shift-left-int sort 24)))

  (method ^:public ^:static newTypeParameterReference ^TypeReference [^:final ^int sort
                                                                      ^:final ^int paramIndex]
    (TypeReference. (bit-or-int (bit-shift-left-int sort 24) (bit-shift-left-int paramIndex 16))))

  (method ^:public ^:static newTypeParameterBoundReference ^TypeReference [^:final ^int sort
                                                                           ^:final ^int paramIndex
                                                                           ^:final ^int boundIndex]
    (TypeReference.
      (bit-or-int (bit-or-int (bit-shift-left-int sort 24) (bit-shift-left-int paramIndex 16))
                  (bit-shift-left-int boundIndex 8))))

  (method ^:public ^:static newSuperTypeReference ^TypeReference [^:final ^int itfIndex]
    (TypeReference.
      (bit-or-int (bit-shift-left-int CLASS_EXTENDS 24)
                  (bit-shift-left-int (bit-and-int itfIndex 0xFFFF) 8))))

  (method ^:public ^:static newFormalParameterReference ^TypeReference [^:final ^int paramIndex]
    (TypeReference.
      (bit-or-int (bit-shift-left-int METHOD_FORMAL_PARAMETER 24)
                  (bit-shift-left-int paramIndex 16))))

  (method ^:public ^:static newExceptionReference ^TypeReference [^:final ^int exceptionIndex]
    (TypeReference.
      (bit-or-int (bit-shift-left-int THROWS 24) (bit-shift-left-int exceptionIndex 8))))

  (method ^:public ^:static newTryCatchReference ^TypeReference [^:final ^int tryCatchBlockIndex]
    (TypeReference.
      (bit-or-int (bit-shift-left-int EXCEPTION_PARAMETER 24)
                  (bit-shift-left-int tryCatchBlockIndex 8))))

  (method ^:public ^:static newTypeArgumentReference ^TypeReference [^:final ^int sort
                                                                     ^:final ^int argIndex]
    (TypeReference. (bit-or-int (bit-shift-left-int sort 24) argIndex)))

  (method ^:public getSort ^int [this]
    (unsigned-bit-shift-right-int targetTypeAndInfo 24))

  (method ^:public getTypeParameterIndex ^int [this]
    (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x00FF0000) 16))

  (method ^:public getTypeParameterBoundIndex ^int [this]
    (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x0000FF00) 8))

  (method ^:public getSuperTypeIndex ^int [this]
    (unchecked-short (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x00FFFF00) 8)))

  (method ^:public getFormalParameterIndex ^int [this]
    (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x00FF0000) 16))

  (method ^:public getExceptionIndex ^int [this]
    (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x00FFFF00) 8))

  (method ^:public getTryCatchBlockIndex ^int [this]
    (bit-shift-right-int (bit-and-int targetTypeAndInfo 0x00FFFF00) 8))

  (method ^:public getTypeArgumentIndex ^int [this]
    (bit-and-int targetTypeAndInfo 0xFF))

  (method ^:public getValue ^int [this] targetTypeAndInfo)

  (method ^:static putTarget ^void [^:final ^int targetTypeAndInfo ^:final ^ByteVector output]
    (switch (unsigned-bit-shift-right-int targetTypeAndInfo 24)
      (CLASS_TYPE_PARAMETER METHOD_TYPE_PARAMETER METHOD_FORMAL_PARAMETER)
        (.putShort output (unsigned-bit-shift-right-int targetTypeAndInfo 16))
      (FIELD METHOD_RETURN METHOD_RECEIVER)
        (.putByte output (unsigned-bit-shift-right-int targetTypeAndInfo 24))
      (CAST CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
            METHOD_INVOCATION_TYPE_ARGUMENT
            CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
            METHOD_REFERENCE_TYPE_ARGUMENT)
        (.putInt output targetTypeAndInfo)
      (CLASS_EXTENDS CLASS_TYPE_PARAMETER_BOUND
                     METHOD_TYPE_PARAMETER_BOUND
                     THROWS
                     EXCEPTION_PARAMETER
                     INSTANCEOF
                     NEW
                     CONSTRUCTOR_REFERENCE
                     METHOD_REFERENCE)
        (.put12 output
                (unsigned-bit-shift-right-int targetTypeAndInfo 24)
                (bit-shift-right-int (bit-and-int targetTypeAndInfo 0xFFFF00) 8))
      (throw (IllegalArgumentException.)))))
