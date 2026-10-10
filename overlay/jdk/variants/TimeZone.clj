;; Go-build variant of java.util.TimeZone (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Time"): read by
;; c2g only. TimeZone is jdk26u's; the default zone is set as the JDK sets it, with the
;; property user.timezone read and written through System.getProperty and setProperty (jrt's
;; System has no Properties object) and without StaticProperty (java.home is not used by jrt's
;; natives, which detect the host's zone as TimeZone_md.c does on Linux: TZ, /etc/localtime).
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util)

(c2g/variant TimeZone
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
