;; Go-build variant of java.time.zone.TzdbZoneRulesProvider (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Time"): read by c2g only. The JDK reads $JAVA_HOME/lib/tzdb.dat; the Go build embeds the same
;; file (bin/jrt-convert makes it as the JDK build does) as the resource lib/tzdb.dat
;; (amendment RD1's resource data), which the constructor reads through
;; Class.getResourceAsStream. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.time.zone)

(c2g/variant TzdbZoneRulesProvider
  (constructor ^:public [this]
    (try
      (let [in (.getResourceAsStream TzdbZoneRulesProvider "/lib/tzdb.dat")]
        (when (nil? in)
          (throw (java.io.FileNotFoundException. "lib/tzdb.dat")))
        (with-resources [dis (java.io.DataInputStream. (java.io.BufferedInputStream. in))]
          (.load this dis)))
      (catch Exception ex
        (throw (ZoneRulesException. "Unable to load TZDB time-zone rules" ex))))))
