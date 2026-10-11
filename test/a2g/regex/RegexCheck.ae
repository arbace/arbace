;; The oracle's regex corpus (test/oracle/regex, doc/go/ORACLE.md) run by java.util.regex as
;; c2g translates it (bin/c2g-regex, c2g.regex-check; doc/go/C2G-NOTES.md, phase 2C). run does
;; what oracle.driver/regex does for one case and returns the result in the canonical text
;; c2g.regex-check prints the recorded result in: the Go program calls it once per case.

(in-ns 'c2g.regex)

(import '(java.util.regex Pattern Matcher PatternSyntaxException)
        '(java.util ArrayList Map Map$Entry TreeMap))

(do

(defclass ^:public RegexCheck

  ;; ----- the canonical text: strings in double quotes with every character outside printable
  ;; ASCII, and " and \, as \uXXXX (lower-case hex)
  (method ^:static str ^void [^StringBuilder sb ^String s]
    (if (nil? s)
      (.append sb "nil")
      (do
        (.append sb \")
        (loop [^int i 0]
          (when (< i (.length s))
            (let [^char c (.charAt s i)]
              (if (and (>= c 32) (<= c 126) (not (== c 34)) (not (== c 92)))
                (.append sb c)
                (let [h (Integer/toHexString c)]
                  (.append sb "\\u")
                  (loop [^int k (.length h)]
                    (when (< k 4) (.append sb \0) (recur (unchecked-inc-int k))))
                  (.append sb h))))
            (recur (unchecked-inc-int i))))
        (.append sb \"))))

  ;; the inverse of c2g.regex-check's field escape: \uXXXX to the character
  (method ^:static unesc ^String [^String s]
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i (.length s))
          (let [^char c (.charAt s i)]
            (if (and (== c 92) (< (unchecked-add-int i 5) (unchecked-inc-int (.length s))) (== (.charAt s (unchecked-inc-int i)) \u))
              (do (.append sb (unchecked-char (Integer/parseInt (.substring s (unchecked-add-int i 2) (unchecked-add-int i 6)) 16)))
                  (recur (unchecked-add-int i 6)))
              (do (.append sb c)
                  (recur (unchecked-inc-int i)))))))
      (.toString sb)))

  (method ^:static strs ^void [^StringBuilder sb ^String/1 a]
    (.append sb "[")
    (loop [^int i 0]
      (when (< i (alength a))
        (when (> i 0) (.append sb " "))
        (RegexCheck/str sb (aget a i))
        (recur (unchecked-inc-int i))))
    (.append sb "]"))

  (method ^:static spans ^void [^StringBuilder sb ^Matcher m]
    (.append sb "[")
    (loop [^int g 0]
      (when (<= g (.groupCount m))
        (when (> g 0) (.append sb " "))
        (.append sb (java-str "[" (.start m g) " " (.end m g) "]"))
        (recur (unchecked-inc-int g))))
    (.append sb "]"))

  (method ^:static ex ^void [^StringBuilder sb ^Throwable t]
    (.append sb "[")
    (RegexCheck/str sb (.getName (.getClass t)))
    (.append sb " ")
    (RegexCheck/str sb (.getMessage t))
    (.append sb "]"))

  ;; one input: oracle.driver's regex-input
  (method ^:static input ^void [^StringBuilder sb ^Pattern p ^String in ^String/1 reps]
    (let [r (StringBuilder.)]
      (try
        (let [m (.matcher p in)
              matches (.matches m)
              mspans (StringBuilder.)]
          (when matches (RegexCheck/spans mspans m))
          (let [hit (.hitEnd m)]
            (.reset m)
            (let [looking (.lookingAt m)]
              (.reset m)
              (.append r (java-str "{:matches " matches " :hit-end " hit " :looking-at " looking " :find ["))
              (loop [^int n 0]
                (when (and (< n 1000) (.find m))
                  (when (> n 0) (.append r " "))
                  (RegexCheck/spans r m)
                  (recur (unchecked-inc-int n))))
              (.append r "] :split ")
              (RegexCheck/strs r (.split p in))
              (.append r " :split-1 ")
              (RegexCheck/strs r (.split p in -1))
              (when matches
                (.append r " :match-groups ")
                (.append r mspans))
              (when (> (alength reps) 0)
                (.append r " :replace [")
                (loop [^int i 0]
                  (when (< i (alength reps))
                    (when (> i 0) (.append r " "))
                    (try
                      (let [all (.replaceAll (.matcher p in) (aget reps i))
                            first (.replaceFirst (.matcher p in) (aget reps i))]
                        (.append r "[")
                        (RegexCheck/str r all)
                        (.append r " ")
                        (RegexCheck/str r first)
                        (.append r "]"))
                      (catch Throwable t
                        (.append r "[:ex ")
                        (RegexCheck/str r (.getName (.getClass t)))
                        (.append r " ")
                        (RegexCheck/str r (.getMessage t))
                        (.append r "]")))
                    (recur (unchecked-inc-int i))))
                (.append r "]"))
              (.append r "}"))))
        (catch Throwable t
          (.setLength r 0)
          (.append r "{:ex ")
          (RegexCheck/ex r t)
          (.append r "}")))
      (.append sb r)))

  ;; a case: fields separated by tabs, escaped: pattern, flags, the number of inputs, the
  ;; inputs, the replacements
  (method ^:public ^:static run ^String [^String line]
    (let [f (.split line "\t" -1)
          pattern (RegexCheck/unesc (aget f 0))
          flags (Integer/parseInt (aget f 1))
          n (Integer/parseInt (aget f 2))
          inputs (new String/1 n)
          reps (new String/1 (unchecked-subtract-int (unchecked-subtract-int (alength f) 3) n))
          sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i n)
          (aset inputs i (RegexCheck/unesc (aget f (unchecked-add-int 3 i))))
          (recur (unchecked-inc-int i))))
      (loop [^int i 0]
        (when (< i (alength reps))
          (aset reps i (RegexCheck/unesc (aget f (unchecked-add-int (unchecked-add-int 3 n) i))))
          (recur (unchecked-inc-int i))))
      (try
        (let [p (Pattern/compile pattern flags)
              named (TreeMap. (.namedGroups p))]
          (.append sb (java-str "{:groups " (.groupCount (.matcher p "")) " :named {"))
          (let [^:mutable ^boolean firstOne true]
            (for-each [^Map$Entry e (.entrySet named)]
              (when-not firstOne (.append sb " "))
              (set! firstOne false)
              (RegexCheck/str sb (cast String (.getKey e)))
              (.append sb (java-str " " (.getValue e)))))
          (.append sb "} :results [")
          (loop [^int i 0]
            (when (< i n)
              (when (> i 0) (.append sb " "))
              (RegexCheck/input sb p (aget inputs i) reps)
              (recur (unchecked-inc-int i))))
          (.append sb "]}"))
        (catch PatternSyntaxException e
          (.setLength sb 0)
          (.append sb "{:error [")
          (RegexCheck/str sb (.getName (.getClass e)))
          (.append sb " ")
          (RegexCheck/str sb (.getDescription e))
          (.append sb (java-str " " (.getIndex e) " "))
          (RegexCheck/str sb (.getMessage e))
          (.append sb "]}"))
        (catch Throwable t
          (.setLength sb 0)
          (.append sb "{:error ")
          (RegexCheck/ex sb t)
          (.append sb "}")))
      (.toString sb))))

)
