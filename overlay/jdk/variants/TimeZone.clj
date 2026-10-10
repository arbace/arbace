;; Go-build variant of java.util.TimeZone (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Time"): read by
;; c2g only. TimeZone is jdk26u's; the default zone is set as the JDK sets it, with the
;; property user.timezone read and written through System.getProperty and setProperty (jrt's
;; System.getProperties is a copy, which a setProperty on it does not change; the property can
;; be set with JAVA_TOOL_OPTIONS' -Duser.timezone, as on the JVM) and without StaticProperty
;; (java.home is not used by jrt's natives, which detect the host's zone as TimeZone_md.c does
;; on Linux: TZ, /etc/localtime).
;; The package-private getOffsets(long, int[]) is cut: in Go, ZoneInfo's public method of that
;; name (another Java package) would override it (C2G-SPEC §4.4, W4), and nothing in the Go
;; build calls it on a TimeZone (the JDK's calendars of java.util do, which jrt's own
;; GregorianCalendar replaces by getRawOffset and getOffset; SimpleTimeZone keeps its own).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant TimeZone
  (c2g/cut getOffsets ^int [this ^long date ^int/1 offsets])

  (method ^:private ^:static ^:synchronized setDefaultZone ^TimeZone []
    (let [^:mutable zoneID (System/getProperty "user.timezone")]
      (when (or (nil? zoneID) (.isEmpty zoneID))
        (set! zoneID (TimeZone/getSystemTimeZoneID (System/getProperty "java.home")))
        (when (nil? zoneID)
          (set! zoneID GMT_ID)))
      (let [^:mutable tz (TimeZone/getTimeZone zoneID false)]
        (when (nil? tz)
          (let [gmtOffsetID (TimeZone/getSystemGMTOffsetID)]
            (when (some? gmtOffsetID)
              (set! zoneID gmtOffsetID)))
          (set! tz (TimeZone/getTimeZone zoneID true)))
        (System/setProperty "user.timezone" zoneID)
        (set! defaultTimeZone tz)
        tz))))
