;; Go-build variant of jdk.internal.util.ByteArrayLittleEndian (C2G-SPEC §4.6; doc/go/EVAL-NOTES.md): read by
;; c2g only. The class reads and writes little-endian values in byte arrays through VarHandles
;; (java.lang.invoke, which the Go build has not: its static initializer failed, and with it
;; UUID.toString); the variant does it byte by byte, with the same results and the same
;; ArrayIndexOutOfBoundsException for an offset out of range. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'jdk.internal.util)

(c2g/variant ByteArrayLittleEndian
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle SHORT))
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle CHAR))
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle INT))
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle FLOAT))
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle LONG))
  (c2g/cut (field ^:private ^:static ^:final ^VarHandle DOUBLE))
  (c2g/cut ^:private ^:static createLittleEndian ^VarHandle [^{:tag (Class ?)} viewArrayClass])

  (c2g/add
    (method ^:private ^:static load ^long [^byte/1 array ^int offset ^int n]
      ;; the n bytes at offset, little-endian, as the low bits of a long (bounds checked first, as the
      ;; VarHandle checks the whole access)
      (when (or (< offset 0) (> (unchecked-add-int offset n) (alength array)))
        (throw (ArrayIndexOutOfBoundsException. (java-str "Index " offset " out of bounds for length " (alength array)))))
      (loop [^int i 0 ^long v 0]
        (if (< i n)
            (recur (unchecked-inc-int i)
                   (bit-or v (bit-shift-left (bit-and (long (aget array (unchecked-add-int offset i))) 0xff)
                                             (unchecked-multiply-int 8 i))))
            v))))

  (c2g/add
    (method ^:private ^:static store ^void [^byte/1 array ^int offset ^int n ^long v]
      (when (or (< offset 0) (> (unchecked-add-int offset n) (alength array)))
        (throw (ArrayIndexOutOfBoundsException. (java-str "Index " offset " out of bounds for length " (alength array)))))
      (loop [^int i 0]
        (when (< i n)
          (aset array (unchecked-add-int offset i)
                (unchecked-byte (bit-shift-right v (unchecked-multiply-int 8 i))))
          (recur (unchecked-inc-int i))))))


  (method ^:public ^:static getChar ^char [^byte/1 array ^int offset]
    (unchecked-char (ByteArrayLittleEndian/load array offset 2)))

  (method ^:public ^:static getShort ^short [^byte/1 array ^int offset]
    (unchecked-short (ByteArrayLittleEndian/load array offset 2)))

  (method ^:public ^:static getUnsignedShort ^int [^byte/1 array ^int offset]
    (unchecked-int (ByteArrayLittleEndian/load array offset 2)))

  (method ^:public ^:static getInt ^int [^byte/1 array ^int offset]
    (unchecked-int (ByteArrayLittleEndian/load array offset 4)))

  (method ^:public ^:static getFloat ^float [^byte/1 array ^int offset]
    (Float/intBitsToFloat (unchecked-int (ByteArrayLittleEndian/load array offset 4))))

  (method ^:public ^:static getFloatRaw ^float [^byte/1 array ^int offset]
    (Float/intBitsToFloat (unchecked-int (ByteArrayLittleEndian/load array offset 4))))

  (method ^:public ^:static getLong ^long [^byte/1 array ^int offset]
    (ByteArrayLittleEndian/load array offset 8))

  (method ^:public ^:static getDouble ^double [^byte/1 array ^int offset]
    (Double/longBitsToDouble (ByteArrayLittleEndian/load array offset 8)))

  (method ^:public ^:static getDoubleRaw ^double [^byte/1 array ^int offset]
    (Double/longBitsToDouble (ByteArrayLittleEndian/load array offset 8)))

  (method ^:public ^:static setChar ^void [^byte/1 array ^int offset ^char value]
    (ByteArrayLittleEndian/store array offset 2 (long value)))

  (method ^:public ^:static setShort ^void [^byte/1 array ^int offset ^short value]
    (ByteArrayLittleEndian/store array offset 2 (long value)))

  (method ^:public ^:static setUnsignedShort ^void [^byte/1 array ^int offset ^int value]
    (ByteArrayLittleEndian/store array offset 2 (long value)))

  (method ^:public ^:static setInt ^void [^byte/1 array ^int offset ^int value]
    (ByteArrayLittleEndian/store array offset 4 (long value)))

  (method ^:public ^:static setFloat ^void [^byte/1 array ^int offset ^float value]
    (ByteArrayLittleEndian/store array offset 4 (long (Float/floatToIntBits value))))

  (method ^:public ^:static setFloatRaw ^void [^byte/1 array ^int offset ^float value]
    (ByteArrayLittleEndian/store array offset 4 (long (Float/floatToRawIntBits value))))

  (method ^:public ^:static setLong ^void [^byte/1 array ^int offset ^long value]
    (ByteArrayLittleEndian/store array offset 8 value))

  (method ^:public ^:static setDouble ^void [^byte/1 array ^int offset ^double value]
    (ByteArrayLittleEndian/store array offset 8 (Double/doubleToLongBits value)))

  (method ^:public ^:static setDoubleRaw ^void [^byte/1 array ^int offset ^double value]
    (ByteArrayLittleEndian/store array offset 8 (Double/doubleToRawLongBits value)))
)
