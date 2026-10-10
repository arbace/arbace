;; Go-build variant of java.util.zip.CRC32 (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Time"): read by
;; c2g only. Its natives (update, updateBytes0) are jrt's over Go's hash/crc32; the static
;; initializer, which loads the zip library, is cut, and a direct buffer (outside the Go build's
;; world) is read through its get, as the JDK reads a buffer that is neither direct nor backed
;; by an array. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.zip)

(c2g/variant CRC32
  (c2g/cut (static-initializer 0))

  (method ^:public update ^void [this ^ByteBuffer buffer]
    (let [pos (.position buffer)
          limit (.limit buffer)
          rem (unchecked-subtract-int limit pos)]
      (when (<= rem 0)
        (return))
      (if (.hasArray buffer)
          (set! crc (CRC32/updateBytes crc (.array buffer) (unchecked-add-int pos (.arrayOffset buffer)) rem))
          (let [b (new byte/1 (Math/min (.remaining buffer) 4096))]
            (while (.hasRemaining buffer)
              (let [length (Math/min (.remaining buffer) (alength b))]
                (.get buffer b 0 length)
                (.update this b 0 length)))))
      (.position buffer limit))))
