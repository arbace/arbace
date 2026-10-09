;; c2g's fixtures (see FxCode.clj): the standard streams' classes (doc/go/JRT-NOTES.md, "Phase
;; 2C"). jrt's own OutputStreamWriter, InputStreamReader, FileOutputStream and FileInputStream
;; (overlay/jdk) against the JDK's: encoding and decoding in the three charsets with malformed
;; and unmappable input, PrintStream and PrintWriter over them, files, System's streams and
;; String.CASE_INSENSITIVE_ORDER.

(in-ns 'c2g.fixtures)

(import '(java.io ByteArrayInputStream ByteArrayOutputStream FileInputStream FileOutputStream
                  FileNotFoundException InputStreamReader OutputStreamWriter PrintStream PrintWriter
                  IOException UnsupportedEncodingException)
        '(java.util Arrays))

(do

(defclass ^:public FxStreams

  (method ^:static hex ^String [^byte/1 bs]
    (let [sb (StringBuilder.)]
      (for-each [^byte b bs] (.append sb (Integer/toHexString (bit-and-int b 255))) (.append sb " "))
      (.toString sb)))

  (method ^:static units ^String [^String s]
    (let [sb (StringBuilder.)]
      (loop [^int i 0]
        (when (< i (.length s))
          (.append sb (Integer/toHexString (.charAt s i))) (.append sb " ")
          (recur (unchecked-inc-int i))))
      (.toString sb)))

  (method ^:static encode ^String [^String cs]
    (let [bo (ByteArrayOutputStream.)
          w (OutputStreamWriter. bo cs)
          enc (.getEncoding w)]
      (.write w "aé€😀|")
      (.write w (unchecked-int 0xd83d))
      (.write w "x\udc00|\ud83d")
      (.write w (new char/1 [(unchecked-char 0xDE01) \y]))
      (.write w (unchecked-int 0xd800))
      (.flush w)
      (let [before (FxStreams/hex (.toByteArray bo))]
        (.close w)
        (java-str enc " " before "/ " (FxStreams/hex (.toByteArray bo)) (.getEncoding w)))))

  (method ^:public ^:static tEncodeUtf8 ^String [] (FxStreams/encode "UTF-8"))
  (method ^:public ^:static tEncodeLatin1 ^String [] (FxStreams/encode "ISO-8859-1"))
  (method ^:public ^:static tEncodeAscii ^String [] (FxStreams/encode "US-ASCII"))

  (method ^:public ^:static tEncodeErrors ^String []
    (let [sb (StringBuilder.)
          w (OutputStreamWriter. (ByteArrayOutputStream.))]
      (try (OutputStreamWriter. (ByteArrayOutputStream.) "x-nope") (catch UnsupportedEncodingException e (.append sb (java-str (.getMessage e) ";"))))
      (try (OutputStreamWriter. (ByteArrayOutputStream.) "bad name!") (catch UnsupportedEncodingException e (.append sb (java-str (.getMessage e) ";"))))
      (try (OutputStreamWriter. (ByteArrayOutputStream.) ^String (FxStreams/nothing)) (catch NullPointerException e (.append sb (java-str (.getMessage e) ";"))))
      (.close w)
      (.close w)
      (try (.write w "x") (catch IOException e (.append sb (java-str (.getMessage e) ";"))))
      (try (.flush w) (catch IOException e (.append sb (java-str (.getMessage e) ";"))))
      (.toString sb)))

  (method ^:static nothing ^Object [] nil)

  (method ^:static decode ^String [^String cs ^byte/1 bs ^int chunk]
    (let [r (InputStreamReader. (ByteArrayInputStream. bs) cs)
          buf (new char/1 chunk)
          sb (StringBuilder.)]
      (let [^:mutable ^int n (.read r buf 0 chunk)]
        (while (>= n 0)
          (.append sb buf 0 n)
          (set! n (.read r buf 0 chunk))))
      (FxStreams/units (.toString sb))))

  (method ^:static decodeAll ^String [^String cs]
    (let [cases (new byte/2 [(new byte/1 [0x61 -61 -87 -30 -126 -84 -16 -97 -104 -128])
                             (new byte/1 [0x61 -61])
                             (new byte/1 [-32 -128 -128 0x62])
                             (new byte/1 [-19 -96 -128 0x63])
                             (new byte/1 [-16 -128 -128 -128])
                             (new byte/1 [-12 -112 -128 -128])
                             (new byte/1 [-64 -81 0x64])
                             (new byte/1 [-31 -128])
                             (new byte/1 [-15 -128 -128])
                             (new byte/1 [-15 -128 0x41 0x42])
                             (new byte/1 [-1 -2 0x7f -128])
                             (new byte/1 [-30 0x28 -95])
                             (new byte/1 [-16 -112 0x28 -68])])
          sb (StringBuilder.)]
      (for-each [^byte/1 c cases]
        (.append sb (java-str (FxStreams/decode cs c 64) "|" (FxStreams/decode cs c 1) "; ")))
      (.toString sb)))

  (method ^:public ^:static tDecodeUtf8 ^String [] (FxStreams/decodeAll "UTF-8"))
  (method ^:public ^:static tDecodeLatin1 ^String [] (FxStreams/decodeAll "ISO-8859-1"))
  (method ^:public ^:static tDecodeAscii ^String [] (FxStreams/decodeAll "US-ASCII"))

  (method ^:public ^:static tReader ^String []
    (let [r (InputStreamReader. (ByteArrayInputStream. (.getBytes "héllo\nw" "UTF-8")))
          sb (StringBuilder.)]
      (.append sb (java-str (.getEncoding r) " " (.ready r) " " (.read r) " " (.read r) " "))
      (.append sb (java-str (.skip r 2) " " (unchecked-char (.read r)) " " (.ready r) " "))
      (let [buf (new char/1 10)]
        (.append sb (java-str (.read r buf 0 10) " " (.read r buf 0 10) " " (.read r buf 0 0))))
      (.close r)
      (.close r)
      (try (.read r) (catch IOException e (.append sb (java-str " " (.getMessage e)))))
      (.append sb (java-str " " (.getEncoding r)))
      (.toString sb)))

  (method ^:public ^:static tPrintStream ^String []
    (let [bo (ByteArrayOutputStream.)
          ps (PrintStream. bo true "UTF-8")]
      (.print ps "xé")
      (.println ps 42)
      (.printf ps "%d-%s%n" (new Object/1 [(Integer/valueOf 1) "a"]))
      (.append ps \c)
      (.write ps 65)
      (.print ps (new char/1 [\o \k]))
      (.println ps 1.5)
      (.print ps ^Object (FxStreams/nothing))
      (.flush ps)
      (java-str (.checkError ps) " " (.name (.charset ps)) " " (.toString bo "UTF-8") "|" (.size bo))))

  (method ^:public ^:static tPrintWriter ^String []
    (let [bo (ByteArrayOutputStream.)
          pw (PrintWriter. (OutputStreamWriter. bo "ISO-8859-1") true)]
      (.print pw "aÿĀ")
      (.println pw "z")
      (.printf pw "%5.2f|%x%n" (new Object/1 [(Double/valueOf 3.14159) (Integer/valueOf 255)]))
      (.close pw)
      (java-str (FxStreams/hex (.toByteArray bo)) (.checkError pw))))

  (method ^:public ^:static tFiles ^String []
    (let [path ".tmp/c2g-fxstreams.txt"
          sb (StringBuilder.)]
      (let [o (FileOutputStream. path)]
        (.write o (.getBytes "hello " "UTF-8"))
        (.write o 0x41)
        (.write o (.getBytes "xyz" "UTF-8") 1 2)
        (.close o)
        (.close o)
        (try (.write o 1) (catch IOException e (.append sb (java-str (.getMessage e) ";")))))
      (let [o (FileOutputStream. path true)]
        (.write o (.getBytes "+more" "UTF-8"))
        (.close o))
      (let [i (FileInputStream. path)
            buf (new byte/1 4)]
        (.append sb (java-str (.available i) " " (.read i) " " (.skip i 2) " " (.read i buf) " "
                              (String. buf 0 4 "UTF-8") " " (.available i) " "))
        (let [^:mutable ^int c (.read i)]
          (while (>= c 0) (.append sb (unchecked-char c)) (set! c (.read i))))
        (.append sb (java-str " " (.read i) " " (.read i buf 0 4) " " (.read i buf 0 0)))
        (.close i)
        (try (.read i) (catch IOException e (.append sb (java-str ";" (.getMessage e))))))
      (try (FileInputStream. ".tmp/no-such-dir/none.txt")
           (catch FileNotFoundException e (.append sb (java-str ";" (.getMessage e)))))
      (try (FileInputStream. ".tmp")
           (catch FileNotFoundException e (.append sb (java-str ";" (.getMessage e)))))
      (try (FileOutputStream. ".tmp/no-such-dir/none.txt")
           (catch FileNotFoundException e (.append sb (java-str ";" (.getMessage e)))))
      (let [r (InputStreamReader. (FileInputStream. path) "UTF-8")
            buf (new char/1 64)
            n (.read r buf)]
        (.close r)
        (.append sb (java-str ";" (String. buf 0 n))))
      (.toString sb)))

  (method ^:public ^:static tSystemStreams ^String []
    (.print System/out "")
    (.flush System/err)
    (java-str (.checkError System/out) " " (.checkError System/err) " " (nil? System/in) " "
              (.getName (.getClass System/out)) " " (.getName (.getClass System/in))))

  (method ^:public ^:static tCaseInsensitiveOrder ^String []
    (let [c String/CASE_INSENSITIVE_ORDER
          a (new Object/1 ["b" "A" "a" "C" "é" "É" "ab" "AB" "Aa"])]
      (Arrays/sort a c)
      (java-str (.compare c "abc" "ABD") " " (.compare c "ß" "SS") " " (.compare c "x" "X") " "
                (Arrays/toString a) " " (identical? c String/CASE_INSENSITIVE_ORDER)))))

)
