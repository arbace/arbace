;; Go-build variant of sun.net.www.ParseUtil (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"): read
;; by c2g only. ParseUtil is translated from jdk26u; decode, which the file: handler uses on a
;; URL's path, decodes each run of %-escapes as UTF-8 with a CharsetDecoder (java.nio's coders
;; are outside the closed world): the variant collects the run's bytes and decodes them with
;; String's UTF-8, rejecting what is not valid UTF-8 as the decoder's REPORT does (valid UTF-8
;; is exactly what encodes back to the same bytes). Copyright (c) the Arbace authors; Eclipse
;; Public License 1.0.
(in-ns 'sun.net.www)

(c2g/variant ParseUtil
  (method ^:public ^:static decode ^String [^String s]
    (let [n (.length s)]
      (if (or (== n 0) (< (.indexOf s \%) 0))
          s
          (let [sb (StringBuilder. n)
                bb (java.io.ByteArrayOutputStream. n)
                ^:mutable c (.charAt s 0)]
            (let [^:mutable ^int i 0]
              (while (< i n)
                (if (not (== c \%))
                    (do
                      (.append sb c)
                      (when (>= (set! i (unchecked-inc-int i)) n) (break))
                      (set! c (.charAt s i)))
                    (do
                      (.reset bb)
                      (while true
                        (when (< (unchecked-subtract-int n i) 2)
                          (throw (IllegalArgumentException.
                                   (java-str "Malformed escape pair: " s))))
                        (try
                          (.write bb (int (ParseUtil/unescape s i)))
                          (catch [NumberFormatException IndexOutOfBoundsException] e
                            (throw (IllegalArgumentException.
                                     (java-str "Malformed escape pair: " s)))))
                        (set! i (unchecked-add-int i 3))
                        (when (>= i n) (break))
                        (set! c (.charAt s i))
                        (when-not (== c \%) (break)))
                      (let [bytes (.toByteArray bb)
                            text (String. bytes java.nio.charset.StandardCharsets/UTF_8)]
                        (when-not (java.util.Arrays/equals bytes (.getBytes text java.nio.charset.StandardCharsets/UTF_8))
                          (throw (IllegalArgumentException.
                                   "Error decoding percent encoded characters")))
                        (.append sb text))))))
            (.toString sb))))))
