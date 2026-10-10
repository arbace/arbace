;; Go-build variant of java.util.BitSet (C2G-SPEC §4.6; doc/go/JAVA-BASE.md, amendment JB2): read
;; by c2g only. BitSet is translated from jdk26u; valueOf(byte[]) and toByteArray go through a
;; little-endian java.nio.ByteBuffer, outside the closed world: the variant reads and writes the
;; same little-endian bytes with shifts (the ByteBuffer and LongBuffer overloads stay outside).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant BitSet
  (method ^:public ^:static valueOf ^BitSet [^byte/1 bytes]
    (let [^:mutable ^int n (alength bytes)]
      (while (and (> n 0) (== (aget bytes (unchecked-subtract-int n 1)) 0))
        (set! n (unchecked-dec-int n)))
      (let [words (new long/1 (unchecked-divide-int (unchecked-add-int n 7) 8))]
        (loop [^int j 0]
          (when (< j n)
            (let [w (unsigned-bit-shift-right-int j 3)]
              (aset words w (bit-or (aget words w)
                                    (bit-shift-left (bit-and (long (aget bytes j)) 0xff)
                                                    (unchecked-multiply-int 8 (bit-and j 7))))))
            (recur (unchecked-inc-int j))))
        (BitSet. words))))

  (method ^:public toByteArray ^byte/1 [this]
    (let [n wordsInUse]
      (if (== n 0)
          (new byte/1 0)
          (let [^:mutable len (unchecked-multiply-int 8 (unchecked-subtract-int n 1))]
            (loop [x (aget words (unchecked-subtract-int n 1))]
              (when-not (== x 0)
                (set! len (unchecked-inc-int len))
                (recur (unsigned-bit-shift-right x 8))))
            (let [bytes (new byte/1 len)]
              (loop [^int j 0]
                (when (< j len)
                  (aset bytes j (unchecked-byte (unsigned-bit-shift-right
                                                  (aget words (unsigned-bit-shift-right-int j 3))
                                                  (unchecked-multiply-int 8 (bit-and j 7)))))
                  (recur (unchecked-inc-int j))))
              bytes))))))
