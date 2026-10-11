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
;; Converted from clojure/asm/ByteVector.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ByteVector
  (field ^byte/1 data)

  (field ^int length)

  (constructor ^:public [this] (set! data (new byte/1 64)))

  (constructor ^:public [this ^:final ^int initialCapacity]
    (set! data (new byte/1 initialCapacity)))

  (constructor [this ^:final ^byte/1 data]
    (set! (.-data this) data)
    (set! (.-length this) (alength data)))

  (method ^:public size ^int [this] length)

  (method ^:public putByte ^ByteVector [this ^:final ^int byteValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 1) (alength data)) (.enlarge this 1))
      (aset data currentLength (unchecked-byte byteValue))
      (set! currentLength (unchecked-inc-int currentLength))
      (set! length currentLength)
      this))

  (method ^:final put11 ^ByteVector [this ^:final ^int byteValue1 ^:final ^int byteValue2]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 2) (alength data)) (.enlarge this 2))
      (let [currentData data]
        (aset currentData currentLength (unchecked-byte byteValue1))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte byteValue2))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:public putShort ^ByteVector [this ^:final ^int shortValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 2) (alength data)) (.enlarge this 2))
      (let [currentData data]
        (aset currentData
              currentLength
              (unchecked-byte (unsigned-bit-shift-right-int shortValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte shortValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:final put12 ^ByteVector [this ^:final ^int byteValue ^:final ^int shortValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 3) (alength data)) (.enlarge this 3))
      (let [currentData data]
        (aset currentData currentLength (unchecked-byte byteValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData
              currentLength
              (unchecked-byte (unsigned-bit-shift-right-int shortValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte shortValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:final put112 ^ByteVector [this ^:final ^int byteValue1 ^:final ^int byteValue2
                                      ^:final ^int shortValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 4) (alength data)) (.enlarge this 4))
      (let [currentData data]
        (aset currentData currentLength (unchecked-byte byteValue1))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte byteValue2))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData
              currentLength
              (unchecked-byte (unsigned-bit-shift-right-int shortValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte shortValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:public putInt ^ByteVector [this ^:final ^int intValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 4) (alength data)) (.enlarge this 4))
      (let [currentData data]
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 24)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 16)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte intValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:final put122 ^ByteVector [this ^:final ^int byteValue ^:final ^int shortValue1
                                      ^:final ^int shortValue2]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 5) (alength data)) (.enlarge this 5))
      (let [currentData data]
        (aset currentData currentLength (unchecked-byte byteValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData
              currentLength
              (unchecked-byte (unsigned-bit-shift-right-int shortValue1 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte shortValue1))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData
              currentLength
              (unchecked-byte (unsigned-bit-shift-right-int shortValue2 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte shortValue2))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:public putLong ^ByteVector [this ^:final ^long longValue]
    (let [^:mutable currentLength length]
      (when (> (unchecked-add-int currentLength 8) (alength data)) (.enlarge this 8))
      (let [currentData data
            ^:mutable intValue (unchecked-int (unsigned-bit-shift-right longValue 32))]
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 24)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 16)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte intValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! intValue (unchecked-int longValue))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 24)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 16)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte (unsigned-bit-shift-right-int intValue 8)))
        (set! currentLength (unchecked-inc-int currentLength))
        (aset currentData currentLength (unchecked-byte intValue))
        (set! currentLength (unchecked-inc-int currentLength))
        (set! length currentLength)
        this)))

  (method ^:public putUTF8 ^ByteVector [this ^:final ^String stringValue]
    (let [charLength (.length stringValue)]
      (when (> charLength 65535) (throw (IllegalArgumentException. "UTF8 string too large")))
      (let [^:mutable currentLength length]
        (when (> (unchecked-add-int (unchecked-add-int currentLength 2) charLength) (alength data))
          (.enlarge this (unchecked-add-int 2 charLength)))
        (let [currentData data]
          (aset currentData
                currentLength
                (unchecked-byte (unsigned-bit-shift-right-int charLength 8)))
          (set! currentLength (unchecked-inc-int currentLength))
          (aset currentData currentLength (unchecked-byte charLength))
          (set! currentLength (unchecked-inc-int currentLength))
          (loop [^int i 0]
            (when (< i charLength)
              (let [charValue (.charAt stringValue i)]
                (if (and (>= charValue \u0001) (<= charValue \u007f))
                    (do
                      (aset currentData currentLength (unchecked-byte charValue))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (recur (unchecked-inc-int i)))
                    (do (set! length currentLength) (return (.encodeUtf8 this stringValue i 65535)))))))
          (set! length currentLength)
          this))))

  (method ^:final encodeUtf8 ^ByteVector [this ^:final ^String stringValue ^:final ^int offset
                                          ^:final ^int maxByteLength]
    (let [charLength (.length stringValue)
          ^:mutable byteLength offset]
      (loop [^int i offset]
        (when (< i charLength)
          (let [charValue (.charAt stringValue i)]
            (cond
              (and (>= charValue 0x0001) (<= charValue 0x007F))
                (do (set! byteLength (unchecked-inc-int byteLength)) (recur (unchecked-inc-int i)))
              (<= charValue 0x07FF)
                (do
                  (set! byteLength (unchecked-add-int byteLength 2))
                  (recur (unchecked-inc-int i)))
              :else
                (do
                  (set! byteLength (unchecked-add-int byteLength 3))
                  (recur (unchecked-inc-int i)))))))
      (when (> byteLength maxByteLength)
        (throw (IllegalArgumentException. "UTF8 string too large")))
      (let [byteLengthOffset (unchecked-subtract-int (unchecked-subtract-int length offset) 2)]
        (when (>= byteLengthOffset 0)
          (aset data byteLengthOffset (unchecked-byte (unsigned-bit-shift-right-int byteLength 8)))
          (aset data (unchecked-add-int byteLengthOffset 1) (unchecked-byte byteLength)))
        (when (> (unchecked-subtract-int (unchecked-add-int length byteLength) offset)
                 (alength data))
          (.enlarge this (unchecked-subtract-int byteLength offset)))
        (let [^:mutable currentLength length]
          (loop [^int i offset]
            (when (< i charLength)
              (let [charValue (.charAt stringValue i)]
                (cond
                  (and (>= charValue 0x0001) (<= charValue 0x007F))
                    (do
                      (aset data currentLength (unchecked-byte charValue))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (recur (unchecked-inc-int i)))
                  (<= charValue 0x07FF)
                    (do
                      (aset
                        data
                        currentLength
                        (unchecked-byte
                          (bit-or-int 0xC0 (bit-and-int (bit-shift-right-int charValue 6) 0x1F))))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (aset data
                            currentLength
                            (unchecked-byte (bit-or-int 0x80 (bit-and-int charValue 0x3F))))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (recur (unchecked-inc-int i)))
                  :else
                    (do
                      (aset
                        data
                        currentLength
                        (unchecked-byte
                          (bit-or-int 0xE0 (bit-and-int (bit-shift-right-int charValue 12) 0xF))))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (aset
                        data
                        currentLength
                        (unchecked-byte
                          (bit-or-int 0x80 (bit-and-int (bit-shift-right-int charValue 6) 0x3F))))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (aset data
                            currentLength
                            (unchecked-byte (bit-or-int 0x80 (bit-and-int charValue 0x3F))))
                      (set! currentLength (unchecked-inc-int currentLength))
                      (recur (unchecked-inc-int i)))))))
          (set! length currentLength)
          this))))

  (method ^:public putByteArray ^ByteVector [this ^:final ^byte/1 byteArrayValue
                                             ^:final ^int byteOffset ^:final ^int byteLength]
    (when (> (unchecked-add-int length byteLength) (alength data)) (.enlarge this byteLength))
    (when (some? byteArrayValue)
      (System/arraycopy byteArrayValue byteOffset data length byteLength))
    (set! length (unchecked-add-int length byteLength))
    this)

  (method ^:private enlarge ^void [this ^:final ^int size]
    (when (> length (alength data)) (throw (AssertionError. "Internal error")))
    (let [doubleCapacity (unchecked-multiply-int 2 (alength data))
          minimalCapacity (unchecked-add-int length size)
          newData (new byte/1
                       (if (> doubleCapacity minimalCapacity) doubleCapacity minimalCapacity))]
      (System/arraycopy data 0 newData 0 length)
      (set! data newData))))
