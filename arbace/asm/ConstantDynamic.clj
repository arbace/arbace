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
;; Converted from clojure/asm/ConstantDynamic.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(import '(java.util Arrays))

(defclass ^:public ^:final ConstantDynamic
  (field ^:private ^:final ^String name)

  (field ^:private ^:final ^String descriptor)

  (field ^:private ^:final ^Handle bootstrapMethod)

  (field ^:private ^:final ^Object/1 bootstrapMethodArguments)

  (constructor ^:public [this ^:final ^String name ^:final ^String descriptor
                         ^:final ^Handle bootstrapMethod &
                         ^:final ^Object/1 bootstrapMethodArguments]
    (set! (.-name this) name)
    (set! (.-descriptor this) descriptor)
    (set! (.-bootstrapMethod this) bootstrapMethod)
    (set! (.-bootstrapMethodArguments this) bootstrapMethodArguments))

  (method ^:public getName ^String [this] name)

  (method ^:public getDescriptor ^String [this] descriptor)

  (method ^:public getBootstrapMethod ^Handle [this] bootstrapMethod)

  (method ^:public getBootstrapMethodArgumentCount ^int [this]
    (alength bootstrapMethodArguments))

  (method ^:public getBootstrapMethodArgument [this ^:final ^int index]
    (aget bootstrapMethodArguments index))

  (method getBootstrapMethodArgumentsUnsafe ^Object/1 [this]
    bootstrapMethodArguments)

  (method ^:public getSize ^int [this]
    (let [firstCharOfDescriptor (.charAt descriptor 0)]
      (if (or (== firstCharOfDescriptor \J) (== firstCharOfDescriptor \D)) 2 1)))

  (method ^:public equals ^boolean [this ^:final object]
    (cond
      (identical? object this) true
      (not (instance? ConstantDynamic object)) false
      :else
        (let [constantDynamic (cast ConstantDynamic object)]
          (and
            (and (and (.equals name (.-name constantDynamic))
                      (.equals descriptor (.-descriptor constantDynamic)))
                 (.equals bootstrapMethod (.-bootstrapMethod constantDynamic)))
            (Arrays/equals bootstrapMethodArguments (.-bootstrapMethodArguments constantDynamic))))))

  (method ^:public hashCode ^int [this]
    (bit-xor-int (bit-xor-int (bit-xor-int (.hashCode name)
                                           (Integer/rotateLeft (.hashCode descriptor) 8))
                              (Integer/rotateLeft (.hashCode bootstrapMethod) 16))
                 (Integer/rotateLeft (Arrays/hashCode bootstrapMethodArguments) 24)))

  (method ^:public toString ^String [this]
    (let [result (StringBuilder.)]
      (when-not (.toString this result) (.setLength result 16381) (.append result "..."))
      (.toString result)))

  (method ^:private toString ^boolean [this ^:final ^StringBuilder stringBuilder]
    (if (>= (.length stringBuilder) 16384)
        false
        (do
          (^[char] StringBuilder/.append
            (.append (^[char] StringBuilder/.append
                       (.append (.append (.append stringBuilder name) " : ") descriptor)
                       \space)
                     bootstrapMethod)
            \space)
          (for-each [argument bootstrapMethodArguments]
            (if (instance? ConstantDynamic argument)
                (when-not (.toString (cast ConstantDynamic argument) stringBuilder) (return false))
                (.append stringBuilder argument)))
          true)))

  (method subtractTreeSize ^int [this ^:mutable ^int size]
    (if (<= (set! size (unchecked-dec-int size)) 0)
        0
        (do
          (for-each [o bootstrapMethodArguments]
            (when (instance? ConstantDynamic o)
              (set! size (.subtractTreeSize (cast ConstantDynamic o) size))
              (when (<= size 0) (return 0))))
          size))))
