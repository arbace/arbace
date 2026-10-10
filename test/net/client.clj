;; The socket server's test client (bin/net-check; doc/go/JRT-NOTES.md, "Sockets"): sessions with
;; a socket REPL (arbace.core.server/repl) and an io-prepl server, as a user's client sends them,
;; printed as transcripts that bin/net-check compares between the JVM's server and the Go
;; executable's. It runs on the JVM Arbace (bin/arbace test/net/client.clj MODE PORT).
;;
;; Modes:
;;   repl PORT        one session of the socket REPL: forms, output, an error, a namespace switch,
;;                    :repl/quit; its transcript
;;   prepl PORT       one session of io-prepl: the same forms; its messages as data, :ms set to 0
;;                    and exceptions' :trace and :at removed (timing and stack frames are the
;;                    builds' own)
;;   concurrent PORT N  N socket REPL sessions at once, each defining and reading back a value
;;                    in its own namespace; prints "N sessions ok" when each saw only its own
(ns net.client
  (:require [arbace.string :as str]
            [arbace.edn :as edn])
  (:import [java.net Socket]
           [java.io BufferedReader InputStreamReader OutputStreamWriter PrintWriter]))

(def forms
  "The session's input: a line per form, as a user types it."
  ["(+ 1 2)"
   "(defn sq [x] (* x x))"
   "(sq 7)"
   "(println \"out\" (sq 3))"
   "(binding [*out* *err*] (println \"to err\"))"
   "(/ 1 0)"
   "(str (:server arbace.core.server/*session*) \" \" (some? (:client arbace.core.server/*session*)))"
   "[*1 *2]"
   "(ns other.ns)"
   "(defn f [] ::kw)"
   "(f)"
   "(str (in-ns 'user))"
   "(def m {:a [1 2.5 \"s\" \\c nil true] :b #{:x}})"
   "m"
   "(map inc (range 5))"
   "(throw (ex-info \"boom\" {:k 1}))"
   "(read-string \"(\")"
   "#?(:clj 1 :default 2)"
   "(count (str (java.net.InetAddress/getLoopbackAddress)))"])

(defn- connect ^Socket [port]
  (doto (Socket. "127.0.0.1" (int port)) (.setSoTimeout 300000)))

(defn- read-all
  "What the server sends until it closes the connection."
  [^Socket s]
  (let [r (BufferedReader. (InputStreamReader. (.getInputStream s) "UTF-8"))
        sb (StringBuilder.)
        buf (char-array 4096)]
    (loop []
      (let [n (.read r buf)]
        (when (pos? n)
          (.append sb buf 0 n)
          (recur))))
    (str sb)))

(defn- send-session
  "Sends the forms and :repl/quit; returns everything the server answered."
  [port lines]
  (with-open [s (connect port)]
    (let [w (PrintWriter. (OutputStreamWriter. (.getOutputStream s) "UTF-8") true)]
      (doseq [l lines] (.println w ^String l))
      (.println w ":repl/quit")
      (read-all s))))

(defn repl-session [port]
  (print (send-session port forms))
  (flush))

(defn- normalize
  "A prepl message without what depends on timing or on the build's frames."
  [m]
  (let [m (cond-> m (:ms m) (assoc :ms 0))]
    (if (and (:exception m) (string? (:val m)))
      (let [v (edn/read-string {:default (fn [_ x] x)} (:val m))]
        (assoc m :val (-> v (dissoc :trace) (update :via (fn [via] (mapv #(dissoc % :at) via))))))
      m)))

(defn prepl-session [port]
  (let [out (send-session port forms)
        msgs (edn/read-string {:default (fn [_ x] x)} (str "[" out "]"))]
    (doseq [m msgs] (prn (normalize m)))))

(defn concurrent [port n]
  (let [results (doall
                  (for [i (range n)]
                    (future
                      ;; each in its own namespace (user is shared by all sessions)
                      (let [t (send-session port [(str "(ns c" i ")")
                                                  (str "(def mine " i ")")
                                                  "(Thread/sleep 200)"
                                                  "mine"
                                                  "(reduce + (range 10000))"])]
                        [i t]))))
        bad (remove (fn [f] (let [[i t] @f]
                              (and (str/includes? t (str "c" i "=> " i "\n"))
                                   (str/includes? t "=> 49995000\n"))))
                    results)]
    (if (empty? bad)
      (println n "sessions ok")
      (doseq [f bad] (println "session" (first @f) "saw:" (pr-str (second @f)))))))

(let [[mode port n] *command-line-args*
      port (Long/parseLong port)]
  (case mode
    "repl" (repl-session port)
    "prepl" (prepl-session port)
    "concurrent" (concurrent port (Long/parseLong n)))
  (shutdown-agents))
