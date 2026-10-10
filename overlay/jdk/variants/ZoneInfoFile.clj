;; Go-build variant of sun.util.calendar.ZoneInfoFile (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Time"): read by c2g only. As TzdbZoneRulesProvider's variant, it reads the time-zone database
;; from the embedded resource lib/tzdb.dat where the JDK reads $JAVA_HOME/lib/tzdb.dat.
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.util.calendar)

(c2g/variant ZoneInfoFile
  (method ^:private ^:static loadTZDB ^void []
    (try
      (let [in (.getResourceAsStream ZoneInfoFile "/lib/tzdb.dat")]
        (when (nil? in)
          (throw (java.io.FileNotFoundException. "lib/tzdb.dat")))
        (with-resources [dis (DataInputStream. (BufferedInputStream. in))]
          (ZoneInfoFile/load dis)))
      (catch Exception x
        (throw (Error. x))))))
