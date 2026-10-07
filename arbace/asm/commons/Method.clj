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
;; Converted from clojure/asm/commons/Method.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm.commons)

(import '(arbace.asm Type)
        '(java.lang.reflect Constructor)
        '(java.util HashMap Map))

(defclass ^:public Method
  (field ^:private ^:final ^String name)

  (field ^:private ^:final ^String descriptor)

  (field ^:private ^:static ^:final ^{:tag (Map String String)} PRIMITIVE_TYPE_DESCRIPTORS)

  (static-initializer
    (let [^{:tag (HashMap String String)} descriptors (HashMap.)]
      (.put descriptors "void" "V")
      (.put descriptors "byte" "B")
      (.put descriptors "char" "C")
      (.put descriptors "double" "D")
      (.put descriptors "float" "F")
      (.put descriptors "int" "I")
      (.put descriptors "long" "J")
      (.put descriptors "short" "S")
      (.put descriptors "boolean" "Z")
      (set! PRIMITIVE_TYPE_DESCRIPTORS descriptors)))

  (constructor ^:public [this ^:final ^String name ^:final ^String descriptor]
    (set! (.-name this) name)
    (set! (.-descriptor this) descriptor))

  (constructor ^:public [this ^:final ^String name ^:final ^Type returnType
                         ^:final ^Type/1 argumentTypes]
    (this. name (Type/getMethodDescriptor returnType argumentTypes)))

  (method ^:public ^:static getMethod ^Method [^:final ^java.lang.reflect.Method method]
    (Method. (.getName method) (Type/getMethodDescriptor method)))

  (method ^:public ^:static getMethod ^Method [^:final ^{:tag (Constructor ?)} constructor]
    (Method. "<init>" (Type/getConstructorDescriptor constructor)))

  (method ^:public ^:static getMethod ^Method [^:final ^String method]
    (Method/getMethod method false))

  (method ^:public ^:static getMethod ^Method [^:final ^String method
                                               ^:final ^boolean defaultPackage]
    (let [spaceIndex (.indexOf method \space)
          ^:mutable currentArgumentStartIndex (unchecked-add-int (.indexOf method \( spaceIndex) 1)
          endIndex (.indexOf method \) currentArgumentStartIndex)]
      (when (or (or (== spaceIndex -1) (== currentArgumentStartIndex 0)) (== endIndex -1))
        (throw (IllegalArgumentException.)))
      (let [returnType (.substring method 0 spaceIndex)
            methodName (.trim (.substring method
                                          (unchecked-add-int spaceIndex 1)
                                          (unchecked-subtract-int currentArgumentStartIndex 1)))
            stringBuilder (StringBuilder.)]
        (^[char] StringBuilder/.append stringBuilder \()
        (let [^:mutable ^int currentArgumentEndIndex 0]
          (loop []
            (let [^:mutable ^String argumentDescriptor nil]
              (set! currentArgumentEndIndex (.indexOf method \, currentArgumentStartIndex))
              (if (== currentArgumentEndIndex -1)
                  (set! argumentDescriptor
                        (Method/getDescriptorInternal
                          (.trim (.substring method currentArgumentStartIndex endIndex))
                          defaultPackage))
                  (do
                    (set! argumentDescriptor
                          (Method/getDescriptorInternal
                            (.trim (.substring method
                                               currentArgumentStartIndex
                                               currentArgumentEndIndex))
                            defaultPackage))
                    (set! currentArgumentStartIndex (unchecked-add-int currentArgumentEndIndex 1))))
              (.append stringBuilder argumentDescriptor))
            (when-not (== currentArgumentEndIndex -1) (recur)))
          (.append (^[char] StringBuilder/.append stringBuilder \))
                   (Method/getDescriptorInternal returnType defaultPackage))
          (Method. methodName (.toString stringBuilder))))))

  (method ^:private ^:static getDescriptorInternal ^String [^:final ^String type
                                                            ^:final ^boolean defaultPackage]
    (if (.equals "" type)
        type
        (let [stringBuilder (StringBuilder.)
              ^:mutable ^int arrayBracketsIndex 0]
          (while (> (set! arrayBracketsIndex
                          (unchecked-add-int (.indexOf type "[]" arrayBracketsIndex) 1))
                    0)
            (^[char] StringBuilder/.append stringBuilder \[))
          (let [elementType (.substring type
                                        0
                                        (unchecked-subtract-int
                                          (.length type)
                                          (unchecked-multiply-int (.length stringBuilder) 2)))
                descriptor (cast String (.get PRIMITIVE_TYPE_DESCRIPTORS elementType))]
            (if (some? descriptor)
                (.append stringBuilder descriptor)
                (do
                  (^[char] StringBuilder/.append stringBuilder \L)
                  (if (< (.indexOf elementType \.) 0)
                      (do
                        (when-not defaultPackage (.append stringBuilder "java/lang/"))
                        (.append stringBuilder elementType))
                      (.append stringBuilder (.replace elementType \. \/)))
                  (^[char] StringBuilder/.append stringBuilder \;)))
            (.toString stringBuilder)))))

  (method ^:public getName ^String [this] name)

  (method ^:public getDescriptor ^String [this] descriptor)

  (method ^:public getReturnType ^Type [this]
    (Type/getReturnType descriptor))

  (method ^:public getArgumentTypes ^Type/1 [this]
    (Type/getArgumentTypes descriptor))

  (method ^:public toString ^String [this] (java-str name descriptor))

  (method ^:public equals ^boolean [this ^:final other]
    (if (not (instance? Method other))
        false
        (let [otherMethod (cast Method other)]
          (and (.equals name (.-name otherMethod)) (.equals descriptor (.-descriptor otherMethod))))))

  (method ^:public hashCode ^int [this]
    (bit-xor-int (.hashCode name) (.hashCode descriptor))))
