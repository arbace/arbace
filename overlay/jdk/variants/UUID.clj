;; Go-build variant of java.util.UUID (C2G-SPEC §4.6; doc/go/EVAL-NOTES.md): read by c2g only.
;; randomUUID draws from java.security.SecureRandom, which is not in the closed world (JRT-NOTES,
;; "left to phase 2a"); the variant draws its 16 bytes from the host's random source (jrt's
;; Host.RandomBytes, crypto/rand in B1a), then sets the version and variant bits as jdk26u's
;; randomUUID does. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant UUID
  (method ^:public ^:static randomUUID ^UUID []
    (let [randomBytes (new byte/1 16)]
      (UUID/hostRandomBytes randomBytes)
      (aset randomBytes 6 (unchecked-byte (bit-and-int (aget randomBytes 6) 0x0f)))
      (aset randomBytes 6 (unchecked-byte (bit-or-int (aget randomBytes 6) 0x40)))
      (aset randomBytes 8 (unchecked-byte (bit-and-int (aget randomBytes 8) 0x3f)))
      (aset randomBytes 8 (unchecked-byte (bit-or-int (aget randomBytes 8) (unchecked-byte 0x80))))
      (UUID. randomBytes)))

  (c2g/add
    (method ^:private ^:static ^:native hostRandomBytes ^void [^byte/1 b])))
