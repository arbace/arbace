;; The Go build's arbace/java/io.clj (doc/go/EVAL-NOTES.md, "Namespace variants"):
;; - no reflection warnings, which it turns on and which the Go build's world would print at
;;   every start, arbace.core requiring it (the context class loader's URLs, D6, cut);
;; - the escape of "+" written out ("%2B"), where Clojure computes it with URLEncoder (outside
;;   the closed world: java.nio's encoders; JRT-NOTES.md, "Files");
;; - copy between two files through their streams, where Clojure transfers between their
;;   channels (java.nio's channels are outside the closed world; JRT-NOTES.md, "Files").
["(set! *warn-on-reflection* true)" "(set! *warn-on-reflection* false)"

 "(URLEncoder/encode \"+\" \"UTF-8\")" "\"%2B\""

 "(with-open [in (-> input FileInputStream. .getChannel)
              out (-> output FileOutputStream. .getChannel)]
    (let [sz (.size in)]
      (loop [pos 0]
        (let [bytes-xferred (.transferTo in pos (- sz pos) out)
              pos (+ pos bytes-xferred)]
          (when (< pos sz)
            (recur pos))))))"
 "(with-open [in (FileInputStream. input)
              out (FileOutputStream. output)]
    (.transferTo in out)
    nil)"]
