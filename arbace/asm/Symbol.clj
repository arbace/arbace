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
;; Converted from clojure/asm/Symbol.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:abstract Symbol
  (field ^:static ^:final ^int CONSTANT_CLASS_TAG 7)

  (field ^:static ^:final ^int CONSTANT_FIELDREF_TAG 9)

  (field ^:static ^:final ^int CONSTANT_METHODREF_TAG 10)

  (field ^:static ^:final ^int CONSTANT_INTERFACE_METHODREF_TAG 11)

  (field ^:static ^:final ^int CONSTANT_STRING_TAG 8)

  (field ^:static ^:final ^int CONSTANT_INTEGER_TAG 3)

  (field ^:static ^:final ^int CONSTANT_FLOAT_TAG 4)

  (field ^:static ^:final ^int CONSTANT_LONG_TAG 5)

  (field ^:static ^:final ^int CONSTANT_DOUBLE_TAG 6)

  (field ^:static ^:final ^int CONSTANT_NAME_AND_TYPE_TAG 12)

  (field ^:static ^:final ^int CONSTANT_UTF8_TAG 1)

  (field ^:static ^:final ^int CONSTANT_METHOD_HANDLE_TAG 15)

  (field ^:static ^:final ^int CONSTANT_METHOD_TYPE_TAG 16)

  (field ^:static ^:final ^int CONSTANT_DYNAMIC_TAG 17)

  (field ^:static ^:final ^int CONSTANT_INVOKE_DYNAMIC_TAG 18)

  (field ^:static ^:final ^int CONSTANT_MODULE_TAG 19)

  (field ^:static ^:final ^int CONSTANT_PACKAGE_TAG 20)

  (field ^:static ^:final ^int BOOTSTRAP_METHOD_TAG 64)

  (field ^:static ^:final ^int TYPE_TAG 128)

  (field ^:static ^:final ^int UNINITIALIZED_TYPE_TAG 129)

  (field ^:static ^:final ^int FORWARD_UNINITIALIZED_TYPE_TAG 130)

  (field ^:static ^:final ^int MERGED_TYPE_TAG 131)

  (field ^:final ^int index)

  (field ^:final ^int tag)

  (field ^:final ^String owner)

  (field ^:final ^String name)

  (field ^:final ^String value)

  (field ^:final ^long data)

  (field ^int info)

  (constructor [this ^:final ^int index ^:final ^int tag ^:final ^String owner ^:final ^String name
                ^:final ^String value ^:final ^long data]
    (set! (.-index this) index)
    (set! (.-tag this) tag)
    (set! (.-owner this) owner)
    (set! (.-name this) name)
    (set! (.-value this) value)
    (set! (.-data this) data))

  (method getArgumentsAndReturnSizes ^int [this]
    (when (== info 0) (set! info (Type/getArgumentsAndReturnSizes value)))
    info))
