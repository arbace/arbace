;; jrt's tests: the host's time zone as TimeZone.getSystemTimeZoneID finds it (TZ, the symbolic
;; link /etc/localtime, a copy of a zoneinfo file), over a host whose files are a temporary
;; directory's; CRC32's natives (doc/go/JRT-NOTES.md, "Time").
(in-ns 'go.arbace.jrt)

(go/file "timezone_test.go"
  :imports [[os "os"] [filepath "path/filepath"] [testing "testing"]])

(go/type zoneHost
  "zoneHost is the OS host with its files below root and its environment env (TZ).\n"
  (struct OSHost ^string root ^{:tag (map string string)} env))

(go/method Open [^{:tag (* zoneHost)} h ^string name ^int flag ^uint32 perm] :results [HostFile error]
  (.Open (.-OSHost h) (+ (.-root h) name) flag perm))
(go/method Stat [^{:tag (* zoneHost)} h ^string name] :results [HostFileInfo error]
  (.Stat (.-OSHost h) (+ (.-root h) name)))
(go/method ReadDir [^{:tag (* zoneHost)} h ^string name] :results [(slice string) error]
  (.ReadDir (.-OSHost h) (+ (.-root h) name)))
(go/method Readlink [^{:tag (* zoneHost)} h ^string name] :results [string error]
  (os/Readlink (+ (.-root h) name)))
(go/method Getenv [^{:tag (* zoneHost)} h ^string name] :results [string bool]
  (let [(values v ok) (aget (.-env h) name)]
    (return v ok)))

(go/func writeZoneFile [^{:tag (* testing/T)} t ^string p ^string content]
  (when (!= (os/MkdirAll (filepath/Dir p) 0755) nil)
    (.Fatal t "mkdir"))
  (when (!= (os/WriteFile p (conv (slice byte) content) 0644) nil)
    (.Fatal t "write")))

(go/func TestSystemTimeZoneID
  "TimeZone.getSystemTimeZoneID: TZ (without : or posix/), else /etc/localtime's link target,
else the zoneinfo file with its contents (UTC and GMT first), else null.\n"
  [^{:tag (* testing/T)} t]
  (let [root (.TempDir t)
        zi (+ root "/usr/share/zoneinfo")
        h (addr (lit zoneHost :root root :env (make (map string string))))
        old (CurrentHost)
        id (fn ^string []
             (let [s (TimeZone_GetSystemTimeZoneID_String__String_native nil)]
               (when (== s nil)
                 (return "<null>"))
               (.String s)))
        check (fn [^string what ^string want]
                (let [got (id)]
                  (when (!= got want)
                    (.Errorf t "%s: %q, want %q" what got want))))]
    (writeZoneFile t (+ zi "/UTC") "TZif-utc")
    (writeZoneFile t (+ zi "/GMT") "TZif-gmt")
    (writeZoneFile t (+ zi "/Europe/Berlin") "TZif-berlin")
    (writeZoneFile t (+ zi "/Asia/Kolkata") "TZif-kolkata")
    (writeZoneFile t (+ zi "/posixrules") "TZif-kolkata")
    (SetHost h)
    (defer (SetHost old))
    (check "no TZ, no /etc/localtime" "<null>")
    (aset (.-env h) "TZ" "Europe/Paris")
    (check "TZ" "Europe/Paris")
    (aset (.-env h) "TZ" ":America/New_York")
    (check "TZ with :" "America/New_York")
    (aset (.-env h) "TZ" "posix/Asia/Tokyo")
    (check "TZ with posix/" "Asia/Tokyo")
    (aset (.-env h) "TZ" "")
    (check "empty TZ, no /etc/localtime" "<null>")
    (when (!= (os/MkdirAll (+ root "/etc") 0755) nil)
      (.Fatal t "mkdir etc"))
    (when (!= (os/Symlink "../usr/share/zoneinfo//Europe/./Berlin" (+ root "/etc/localtime")) nil)
      (.Fatal t "symlink"))
    (check "symbolic link" "Europe/Berlin")
    (os/Remove (+ root "/etc/localtime"))
    (writeZoneFile t (+ root "/etc/localtime") "TZif-kolkata")
    (check "a copy" "Asia/Kolkata")
    (writeZoneFile t (+ root "/etc/localtime") "TZif-utc")
    (check "a copy of UTC" "UTC")
    (writeZoneFile t (+ root "/etc/localtime") "TZif-other")
    (check "a copy of nothing" "<null>")))

(go/func TestCRC32
  "CRC32's natives: the CRC-32 (IEEE) of 123456789 is CBF43926, byte by byte or at once.\n"
  [^{:tag (* testing/T)} t]
  (let [b (NewByteArray 11)
        s "x123456789x"
        c (conv int32 0)]
    (for [i (conv int32 0)] (< i 11) (inc! i)
      (aset (.-A b) i (conv int8 (aget s i))))
    (for [i (conv int32 1)] (< i 10) (inc! i)
      (set! c (CRC32_Update_I_I__I_native c (conv int32 (aget (.-A b) i)))))
    (when (!= (conv uint32 c) 0xCBF43926)
      (.Errorf t "update: %08x" (conv uint32 c)))
    (let [d (CRC32_UpdateBytes0_I_B1_I_I__I_native 0 b 1 9)]
      (when (!= (conv uint32 d) 0xCBF43926)
        (.Errorf t "updateBytes0: %08x" (conv uint32 d))))))
