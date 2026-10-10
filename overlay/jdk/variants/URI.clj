;; Go-build variant of java.net.URI (C2G-SPEC §4.6): read by c2g only. URI's static initializer
;; registers a JavaNetUriAccess with SharedSecrets for the JDK's internals (the module system,
;; jar URLs), none of which is in the Go build; the variant registers nothing. decode (getPath and
;; the other decoded parts) decodes each run of %-escapes as UTF-8 with a CharsetDecoder, outside
;; the closed world: the variant collects the run's bytes and decodes them with String's UTF-8,
;; which replaces malformed input as the decoder's REPLACE does (JRT-NOTES.md, "Files").
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant URI
  (c2g/cut (static-initializer 0))

  (method ^:private ^:static decode ^String [^String s ^boolean ignorePercentInBrackets]
    (if (nil? s)
        s
        (let [n (.length s)]
          (cond
            (== n 0) s
            (< (.indexOf s \%) 0) s
            :else
              (let [sb (StringBuilder. n)
                    bb (java.io.ByteArrayOutputStream. n)
                    ^:mutable c (.charAt s 0)
                    ^:mutable betweenBrackets false]
                (let [^:mutable ^int i 0]
                  (while (< i n)
                    (cond
                      (== c \[) (set! betweenBrackets true)
                      (and betweenBrackets (== c \])) (set! betweenBrackets false))
                    (if (or (not (== c \%)) (and betweenBrackets ignorePercentInBrackets))
                        (do
                          (.append sb c)
                          (when (>= (set! i (unchecked-inc-int i)) n) (break))
                          (set! c (.charAt s i)))
                        (do
                          (.reset bb)
                          (while true
                            (.write bb (int (URI/decode (.charAt s (set! i (unchecked-inc-int i)))
                                                        (.charAt s (set! i (unchecked-inc-int i))))))
                            (when (>= (set! i (unchecked-inc-int i)) n) (break))
                            (set! c (.charAt s i))
                            (when-not (== c \%) (break)))
                          (.append sb (String. (.toByteArray bb) java.nio.charset.StandardCharsets/UTF_8))))))
                (.toString sb)))))))
