;; jrt: the natives of java.util.TimeZone and java.util.zip.CRC32 (doc/go/JRT-NOTES.md, "Time"),
;; translated from jdk26u with the time-zone database. TimeZone's default zone is the host's,
;; found as jdk26u's src/java.base/unix/native/libjava/TimeZone_md.c finds it on Linux: the
;; environment variable TZ, else the zone /etc/localtime names (a symbolic link's target after
;; "zoneinfo/", else the file of /usr/share/zoneinfo with the same contents); a zone ID Java does
;; not know falls back to the host's offset now, as GMT+hh:mm. The symbolic link is read through
;; HostLinks, an optional interface of the host (Host stays as it is); the rest through Host.
;; CRC32 (ZoneInfo's checksums) is Go's hash/crc32, the same IEEE polynomial as zlib's.
(in-ns 'go.arbace.jrt)

(go/file "timezone.go"
  :imports [[crc32 "hash/crc32"] [io "io"] [os "os"] [path "path"] [strings "strings"]])

(go/type HostLinks
  "HostLinks is the host's symbolic links (TimeZone's /etc/localtime): a Host may implement it.\n"
  (interface
    (^{:doc "Readlink is readlink(2): the target of the symbolic link name; an error when name is
not a symbolic link.\n"}
      Readlink [^string name] :results [string error])))

(go/method Readlink [^OSHost h ^string name] :results [string error] (os/Readlink name))

(go/const
  [^{:tag string :val "/usr/share/zoneinfo" :doc "zoneinfoDir is TimeZone_md.c's ZONEINFO_DIR.\n"}
   zoneinfoDir "/usr/share/zoneinfo"]
  [^{:tag string :val "/etc/localtime" :doc "localtimeFile is TimeZone_md.c's DEFAULT_ZONEINFO_FILE.\n"}
   localtimeFile "/etc/localtime"])

(go/func TimeZone_GetSystemTimeZoneID_String__String_native
  "TimeZone_GetSystemTimeZoneID_String__String_native is TimeZone.getSystemTimeZoneID
(findJavaTZ_md): TZ, else the zone of /etc/localtime, without a leading : or posix/; null when
neither tells.\n"
  ^{:tag (* String)} [^{:tag (* String)} javaHome]
  (let [h (CurrentHost)
        (values tz ok) (.Getenv h "TZ")]
    (when (or (not ok) (== tz ""))
      (set! tz (platformTimeZoneID h))
      (when (== tz "")
        (return nil)))
    (set! tz (strings/TrimPrefix tz ":"))
    (set! tz (strings/TrimPrefix tz "posix/"))
    (Str tz)))

(go/func zoneName
  "zoneName is TimeZone_md.c's getZoneName: the part of a path after zoneinfo/, or \"\".\n"
  ^string [^string p]
  (let [i (strings/Index p "zoneinfo/")]
    (when (< i 0)
      (return ""))
    (subslice p (+ i 9))))

(go/func platformTimeZoneID
  "platformTimeZoneID is TimeZone_md.c's getPlatformTimeZoneID: the zone of /etc/localtime, by
its symbolic link's target (duplicate slashes, . and .. collapsed), else by the file of
/usr/share/zoneinfo with the same contents; \"\" when none.\n"
  ^string [^Host h]
  (let [(values hl ok) (assert HostLinks h)]
    (when ok
      (let [(values link err) (.Readlink hl localtimeFile)]
        (when (== err nil)
          (let [z (zoneName (path/Clean link))]
            (when (!= z "")
              (return z)))))))
  (let [buf (readHostFile h localtimeFile)]
    (when (== buf nil)
      (return ""))
    (findZoneinfoFile h buf zoneinfoDir)))

(go/func readHostFile "readHostFile is a file's contents through the host, or nil.\n"
  ^{:tag (slice byte)} [^Host h ^string name]
  (let [(values f err) (.Open h name os/O_RDONLY 0)]
    (when (!= err nil)
      (return nil))
    (let [(values b rerr) (io/ReadAll f)]
      (.Close f)
      (when (!= rerr nil)
        (return nil))
      (when (== b nil)
        (set! b (lit (slice byte))))
      b)))

(go/func findZoneinfoFile
  "findZoneinfoFile is TimeZone_md.c's findZoneinfoFile: the zone whose file below dir has the
contents buf (UTC and GMT first in the top directory; then the directory's entries, here in
sorted order where the C code takes readdir's, skipping hidden names, ROC, posixrules and
localtime); \"\" when none.\n"
  ^string [^Host h ^{:tag (slice byte)} buf ^string dir]
  (when (== dir zoneinfoDir)
    (range [_ z (lit (slice string) "UTC" "GMT")]
      (let [m (fileIdentical h buf (+ dir "/" z))]
        (when (!= m "")
          (return m)))))
  (let [(values names err) (.ReadDir h dir)]
    (when (!= err nil)
      (return ""))
    (range [_ n names]
      (when (or (strings/HasPrefix n ".") (== n "ROC") (== n "posixrules") (== n "localtime"))
        (continue))
      (let [m (fileIdentical h buf (+ dir "/" n))]
        (when (!= m "")
          (return m))))
    ""))

(go/func fileIdentical
  "fileIdentical is TimeZone_md.c's isFileIdentical: a directory searched, a regular file of
buf's size compared; the zone name of a match, else \"\".\n"
  ^string [^Host h ^{:tag (slice byte)} buf ^string p]
  (let [(values fi err) (.Stat h p)]
    (when (!= err nil)
      (return ""))
    (when (.-IsDir fi)
      (return (findZoneinfoFile h buf p)))
    (when (or (!= (.-Size fi) (conv int64 (len buf))) (!= (bit-and (.-Mode fi) (conv uint32 os/ModeType)) 0))
      (return ""))
    (let [b (readHostFile h p)]
      (when (or (== b nil) (!= (conv string b) (conv string buf)))
        (return ""))
      (zoneName p))))

(go/func TimeZone_GetSystemGMTOffsetID__String_native
  "TimeZone_GetSystemGMTOffsetID__String_native is TimeZone.getSystemGMTOffsetID
(getGMTOffsetID): the host's offset from UTC now (Go's local time, from TZ and /etc/localtime)
as GMT+hh:mm or GMT-hh:mm, GMT when it is zero.\n"
  ^{:tag (* String)} []
  (let [(values _ off) (.Zone (.Now (CurrentHost)))]
    (when (== (/ off 60) 0)
      (return (Str "GMT")))
    (let [sign "+"
          m (/ off 60)]
      (when (< m 0)
        (set! sign "-")
        (set! m (- m)))
      (Str (+ "GMT" sign (twoDigits (/ m 60)) ":" (twoDigits (% m 60)))))))

(go/func twoDigits ^string [^int n]
  (+ (conv string (conv rune (+ 48 (/ n 10)))) (conv string (conv rune (+ 48 (% n 10))))))

;; ---------------------------------------------------------------------------------------
;; java.util.zip.CRC32

(go/func CRC32_Update_I_I__I_native
  "CRC32_Update_I_I__I_native is CRC32's native update(int crc, int b): the CRC-32 of the byte b
after crc.\n"
  ^int32 [^int32 crc ^int32 b]
  (conv int32 (crc32/Update (conv uint32 crc) crc32/IEEETable (lit (slice byte) (conv byte b)))))

(go/func CRC32_UpdateBytes0_I_B1_I_I__I_native
  "CRC32_UpdateBytes0_I_B1_I_I__I_native is CRC32's native updateBytes0: the CRC-32 of b[off,
off+len) after crc (the caller checked the bounds).\n"
  ^int32 [^int32 crc ^{:tag (* ByteArray)} b ^int32 off ^int32 n]
  (let [p (make (slice byte) n)]
    (for [i (conv int32 0)] (< i n) (inc! i)
      (aset p i (conv byte (aget (.-A b) (+ off i)))))
    (conv int32 (crc32/Update (conv uint32 crc) crc32/IEEETable p))))
