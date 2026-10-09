;; Go-build variant of java.time.Instant (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, phase 2B "Dates"):
;; read by c2g only. Instant is translated from jdk26u; java.time.format and Clock are outside
;; the closed world, so toString writes DateTimeFormatter.ISO_INSTANT's text through jrt's own
;; jdk.internal.jrt.TimeText, and now() reads System.currentTimeMillis (millisecond precision;
;; the JVM's Clock has the OS clock's). Copyright (c) the Arbace authors; Eclipse Public License
;; 1.0.
(in-ns 'java.time)

(c2g/variant Instant
  (method ^:public toString ^String [this]
    (jdk.internal.jrt.TimeText/instant seconds nanos))

  (method ^:public ^:static now ^Instant []
    (Instant/ofEpochMilli (System/currentTimeMillis))))
