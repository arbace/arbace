;; jrt's tests: helpers for the differential test data (testdata/*.txt, made by bin/jrt
;; testdata from the JVM; test/jrt/testdata.clj describes the format).
(in-ns 'go.arbace.jrt)

(go/file "testutil_test.go"
  :imports [[fmt "fmt"] [os "os"] [strconv "strconv"] [strings "strings"] [testing "testing"]])

(go/func readCases
  "readCases reads testdata/NAME: one case per line, its tab-separated fields.\n"
  ^{:tag (slice (slice string))} [^{:tag (* testing/T)} t ^string name]
  (let [(values b err) (os/ReadFile (+ "testdata/" name))]
    (when (!= err nil)
      (.Fatal t err))
    (let [^{:tag (slice (slice string))} out nil]
      (range [_ l (strings/Split (conv string b) "\n")]
        (when (!= l "")
          (set! out (append out (strings/Split l "\t")))))
      out)))

(go/func unesc "unesc decodes an escaped field into UTF-16 code units.\n"
  ^{:tag (slice uint16)} [^string s]
  (let [v (make (slice uint16) 0 (len s))]
    (for [i 0] (< i (len s)) (inc! i)
      (let [c (aget s i)]
        (if (!= c \\)
          (set! v (append v (conv uint16 c)))
          (do
            (inc! i)
            (switch (aget s i)
              (case [\\] (set! v (append v \\)))
              (case [\t] (set! v (append v \tab)))
              (case [\n] (set! v (append v \newline)))
              (case [\u]
                (let [(values x _) (strconv/ParseUint (subslice s (+ i 1) (+ i 5)) 16 16)]
                  (set! v (append v (conv uint16 x)))
                  (set! i (+ i 4)))))))))
    v))

(go/func esc "esc encodes code units as the test files do.\n"
  ^string [^{:tag (slice uint16)} v]
  (let [^strings/Builder b (zero strings/Builder)]
    (range [_ c v]
      (cond
        (== c \\) (.WriteString b "\\\\")
        (== c \tab) (.WriteString b "\\t")
        (== c \newline) (.WriteString b "\\n")
        (and (>= c 0x20) (<= c 0x7e)) (.WriteByte b (conv byte c))
        :else (fmt/Fprintf (addr b) "\\u%04X" c)))
    (.String b)))

(go/func jstr "jstr is the String of an escaped field.\n"
  ^{:tag (* String)} [^string s]
  (newString (unesc s)))

(go/func exText "exText is an exception as the test files write it.\n"
  ^string [^Throwable_I e]
  (let [s (+ "!" (.GoName (.GetClass__Class e)))
        m (.GetMessage__String e)]
    (when (!= m nil)
      (set! s (+ s ": " (esc (.-value m)))))
    s))

(go/func res
  "res runs f and returns its result field, or the Java exception it throws as !CLASS:
MESSAGE (Go's run-time errors mapped as jrt.Catch maps them).\n"
  [^{:tag (func [] [string])} f] :results [^string out]
  (let [^Throwable_I exc nil]
    (set! out ((fn [] :results [^string r]
                 (defer (Catch (addr exc)))
                 (set! r (f))
                 (return))))
    (when (!= exc nil)
      (set! out (exText exc)))
    (return)))

(go/func sres "sres is a String result as a field (null for nil).\n"
  ^string [^{:tag (* String)} s]
  (when (== s nil)
    (return "null"))
  (esc (.-value s)))

(go/func ires ^string [^int32 i] (strconv/Itoa (conv int i)))
(go/func bres ^string [^bool b] (strconv/FormatBool b))
(go/func atoi ^int32 [^string s]
  (let [(values n _) (strconv/Atoi s)]
    (conv int32 n)))

(go/var ^{:tag (map string int)} failures (make (map string int)))

(go/func check
  "check compares a result with the expected field and reports a mismatch (the first three of
each operation in full, then a count at the end of the test, failuresReport).\n"
  [^{:tag (* testing/T)} t ^{:tag (slice string)} c ^string got]
  (let [want (aget c (- (len c) 1))]
    (when (!= got want)
      (let [k (+ (.Name t) " " (aget c 0))]
        (aset failures k (+ (aget failures k) 1))
        (when (<= (aget failures k) 3)
          (.Errorf t "%s: got %q, want %q" (strings/Join (subslice c _ (- (len c) 1)) " ") got want))))))

(go/func failuresReport [^{:tag (* testing/T)} t]
  (range [k n failures]
    (when (and (strings/HasPrefix k (+ (.Name t) " ")) (> n 3))
      (.Errorf t "%s: %d mismatches" k n))))
