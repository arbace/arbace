;; /*
;;  * Copyright (C) 2011 The Guava Authors
;;  *
;;  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
;;  * in compliance with the License. You may obtain a copy of the License at
;;  *
;;  * http://www.apache.org/licenses/LICENSE-2.0
;;  *
;;  * Unless required by applicable law or agreed to in writing, software distributed under the License
;;  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
;;  * or implied. See the License for the specific language governing permissions and limitations under
;;  * the License.
;;  */
;;
;; /*
;;  * MurmurHash3 was written by Austin Appleby, and is placed in the public
;;  * domain. The author hereby disclaims copyright to this source code.
;;  */
;;
;; /*
;;  * Source:
;;  * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp
;;  * (Modified to adapt to Guava coding conventions and to use the HashFunction interface)
;;  */
;;
;; /**
;;  * Modified to remove stuff Clojure doesn't need, placed under clojure.lang namespace,
;;  * all fns made static, added hashOrdered/Unordered
;;  */
;;
;; Converted from clojure/lang/Murmur3.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(defclass ^:public ^:final Murmur3
  (field ^:private ^:static ^:final ^int seed 0)

  (field ^:private ^:static ^:final ^int C1 (unchecked-int 0xcc9e2d51))

  (field ^:private ^:static ^:final ^int C2 0x1b873593)

  (method ^:public ^:static hashInt ^int [^int input]
    (if (== input 0)
        0
        (let [k1 (Murmur3/mixK1 input) h1 (Murmur3/mixH1 seed k1)] (Murmur3/fmix h1 4))))

  (method ^:public ^:static hashLong ^int [^long input]
    (if (== input 0)
        0
        (let [low (unchecked-int input)
              high (unchecked-int (unsigned-bit-shift-right input 32))
              ^:mutable k1 (Murmur3/mixK1 low)
              ^:mutable h1 (Murmur3/mixH1 seed k1)]
          (set! k1 (Murmur3/mixK1 high))
          (set! h1 (Murmur3/mixH1 h1 k1))
          (Murmur3/fmix h1 8))))

  (method ^:public ^:static hashUnencodedChars ^int [^CharSequence input]
    (let [^:mutable h1 seed]
      (loop [^int i 1]
        (when (< i (.length input))
          (let [^:mutable k1 (bit-or-int (.charAt input (unchecked-subtract-int i 1))
                                         (bit-shift-left-int (.charAt input i) 16))]
            (set! k1 (Murmur3/mixK1 k1))
            (set! h1 (Murmur3/mixH1 h1 k1))
            (recur (unchecked-add-int i 2)))))
      (when (== (bit-and-int (.length input) 1) 1)
        (let [^:mutable ^int k1 (.charAt input (unchecked-subtract-int (.length input) 1))]
          (set! k1 (Murmur3/mixK1 k1))
          (set! h1 (bit-xor-int h1 k1))))
      (Murmur3/fmix h1 (unchecked-multiply-int 2 (.length input)))))

  (method ^:public ^:static mixCollHash ^int [^int hash ^int count]
    (let [^:mutable h1 seed
          k1 (Murmur3/mixK1 hash)]
      (set! h1 (Murmur3/mixH1 h1 k1))
      (Murmur3/fmix h1 count)))

  (method ^:public ^:static hashOrdered ^int [^Iterable xs]
    (let [^:mutable ^int n 0
          ^:mutable ^int hash 1]
      (for-each [x xs]
        (set! hash (unchecked-add-int (unchecked-multiply-int 31 hash) (Util/hasheq x)))
        (set! n (unchecked-inc-int n)))
      (Murmur3/mixCollHash hash n)))

  (method ^:public ^:static hashUnordered ^int [^Iterable xs]
    (let [^:mutable ^int hash 0
          ^:mutable ^int n 0]
      (for-each [x xs]
        (set! hash (unchecked-add-int hash (Util/hasheq x)))
        (set! n (unchecked-inc-int n)))
      (Murmur3/mixCollHash hash n)))

  (method ^:private ^:static mixK1 ^int [^:mutable ^int k1]
    (set! k1 (unchecked-multiply-int k1 C1))
    (set! k1 (Integer/rotateLeft k1 15))
    (set! k1 (unchecked-multiply-int k1 C2))
    k1)

  (method ^:private ^:static mixH1 ^int [^:mutable ^int h1 ^int k1]
    (set! h1 (bit-xor-int h1 k1))
    (set! h1 (Integer/rotateLeft h1 13))
    (set! h1 (unchecked-add-int (unchecked-multiply-int h1 5) (unchecked-int 0xe6546b64)))
    h1)

  (method ^:private ^:static fmix ^int [^:mutable ^int h1 ^int length]
    (set! h1 (bit-xor-int h1 length))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 16)))
    (set! h1 (unchecked-multiply-int h1 (unchecked-int 0x85ebca6b)))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 13)))
    (set! h1 (unchecked-multiply-int h1 (unchecked-int 0xc2b2ae35)))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 16)))
    h1))
