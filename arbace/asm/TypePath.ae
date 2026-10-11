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
;; Converted from clojure/asm/TypePath.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:final TypePath
  (field ^:public ^:static ^:final ^int ARRAY_ELEMENT 0)

  (field ^:public ^:static ^:final ^int INNER_TYPE 1)

  (field ^:public ^:static ^:final ^int WILDCARD_BOUND 2)

  (field ^:public ^:static ^:final ^int TYPE_ARGUMENT 3)

  (field ^:private ^:final ^byte/1 typePathContainer)

  (field ^:private ^:final ^int typePathOffset)

  (constructor [this ^:final ^byte/1 typePathContainer ^:final ^int typePathOffset]
    (set! (.-typePathContainer this) typePathContainer)
    (set! (.-typePathOffset this) typePathOffset))

  (method ^:public getLength ^int [this]
    (aget typePathContainer typePathOffset))

  (method ^:public getStep ^int [this ^:final ^int index]
    (aget typePathContainer
          (unchecked-add-int (unchecked-add-int typePathOffset (unchecked-multiply-int 2 index)) 1)))

  (method ^:public getStepArgument ^int [this ^:final ^int index]
    (aget typePathContainer
          (unchecked-add-int (unchecked-add-int typePathOffset (unchecked-multiply-int 2 index)) 2)))

  (method ^:public ^:static fromString ^TypePath [^:final ^String typePath]
    (when-not (or (nil? typePath) (== (.length typePath) 0))
      (let [typePathLength (.length typePath)
            output (ByteVector. typePathLength)]
        (.putByte output 0)
        (let [^:mutable ^int typePathIndex 0]
          (while (< typePathIndex typePathLength)
            (let [^:mutable c (.charAt typePath
                                       (let [old-1 typePathIndex]
                                         (set! typePathIndex (unchecked-inc-int typePathIndex))
                                         old-1))]
              (cond
                (== c \[) (.put11 output ARRAY_ELEMENT 0)
                (== c \.) (.put11 output INNER_TYPE 0)
                (== c \*) (.put11 output WILDCARD_BOUND 0)
                (and (>= c \0) (<= c \9))
                  (let [^:mutable typeArg (unchecked-subtract-int c \0)]
                    (while (< typePathIndex typePathLength)
                      (set! c (.charAt typePath typePathIndex))
                      (set! typePathIndex (unchecked-inc-int typePathIndex))
                      (cond
                        (and (>= c \0) (<= c \9))
                          (set! typeArg
                                (unchecked-subtract-int
                                  (unchecked-add-int (unchecked-multiply-int typeArg 10) c)
                                  \0))
                        (== c \;) (break)
                        :else (throw (IllegalArgumentException.))))
                    (.put11 output TYPE_ARGUMENT typeArg))
                :else (throw (IllegalArgumentException.)))))
          (aset (.-data output) 0 (unchecked-byte (unchecked-divide-int (.-length output) 2)))
          (TypePath. (.-data output) 0)))))

  (method ^:public toString ^String [this]
    (let [length (.getLength this)
          result (StringBuilder. (unchecked-multiply-int length 2))]
      (loop [^int i 0]
        (when (< i length)
          (switch (.getStep this i)
            ARRAY_ELEMENT
              (do (^[char] StringBuilder/.append result \[) (recur (unchecked-inc-int i)))
            INNER_TYPE (do (^[char] StringBuilder/.append result \.) (recur (unchecked-inc-int i)))
            WILDCARD_BOUND
              (do (^[char] StringBuilder/.append result \*) (recur (unchecked-inc-int i)))
            TYPE_ARGUMENT
              (do
                (^[char] StringBuilder/.append
                  (^[int] StringBuilder/.append result (.getStepArgument this i))
                  \;)
                (recur (unchecked-inc-int i)))
            (throw (AssertionError.)))))
      (.toString result)))

  (method ^:static put ^void [^:final ^TypePath typePath ^:final ^ByteVector output]
    (if (nil? typePath)
        (.putByte output 0)
        (let [length (unchecked-add-int
                       (unchecked-multiply-int
                         (aget (.-typePathContainer typePath) (.-typePathOffset typePath))
                         2)
                       1)]
          (.putByteArray output (.-typePathContainer typePath) (.-typePathOffset typePath) length)))))
