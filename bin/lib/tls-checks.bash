# Sourced by bin/arbace-go and bin/box-smoke: TLS's checks (doc/go/BOX-NOTES.md, "TLS (option 2)").

# tls_server EXE DIR: starts tools/tlsserver (EXE) on the loopback with its files in DIR (its test
# CA, the wrong CA, its ports), waits for them; tls_server_stop ends it. TLS_SERVER_PID is its pid
tls_server() {
  local exe=$1 sdir=$2 i
  rm -rf "$sdir"; mkdir -p "$sdir"
  mkfifo "$sdir/in"
  # started from a subshell, so that it is no job of the caller's (whose wait would wait for it)
  ( "$exe" "$sdir" < "$sdir/in" > "$sdir/log" 2>&1 & echo $! > "$sdir/pid" )
  exec {TLS_SERVER_FD}> "$sdir/in"
  TLS_SERVER_PID=$(cat "$sdir/pid")
  for i in $(seq 1 100); do [ -s "$sdir/https.port" ] && return 0; sleep 0.1; done
  echo "  tls: the test server did not start ($sdir/log)"; return 1
}
tls_server_stop() {
  [ -n "${TLS_SERVER_FD:-}" ] && exec {TLS_SERVER_FD}>&-
  [ -n "${TLS_SERVER_PID:-}" ] && kill "$TLS_SERVER_PID" 2>/dev/null
  TLS_SERVER_FD="" TLS_SERVER_PID=""
}

# tls_build_server OUT [GOARCH]: tools/tlsserver built into OUT with Go's toolchain ($ARBACE_GOROOT,
# default /root/tamago-go), for GOARCH (default the host's)
tls_build_server() {
  local goroot=${ARBACE_GOROOT:-/root/tamago-go}
  [ -x "$goroot/bin/go" ] || return 1
  (cd "$root/tools/tlsserver" && env -u GOOS GOARCH=${2:-$(go_arch_of_host)} GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= \
     GOWORK=off GOPROXY=off CGO_ENABLED=0 "$goroot/bin/go" build -trimpath -o "$1" .)
}
go_arch_of_host() { case $(uname -m) in aarch64) echo arm64 ;; *) echo amd64 ;; esac; }

# tls_checks RUN DIR ROOTS: TLS through arbace.tls and arbace.http (doc/go/BOX-NOTES.md, "TLS
# (option 2)"), for the program the function RUN runs (the executable, or the box's launcher with
# a policy that dials the loopback), against the test server whose files are in DIR as RUN sees
# them (the box: the host's paths are not its; the caller reads them here and passes the PEM in
# the forms), ROOTS the ARBACE_TLS_ROOTS that names the test CA for RUN (a file, or ref:NAME with
# the store RUN has): the handshake and the session, verification against the wrong CA, the
# wrong name and the default roots, the roots given or named by ARBACE_TLS_ROOTS, a request and
# its response, large bodies (Content-Length, chunked, to the connection's end), 8 MiB both ways
# on a raw TLS stream ended by close_notify, an API's refusal (401, JSON). Uses the caller's check
tls_checks() {
  local run=$1 tls_dir=$2 roots=$3 ca wrong hp ep e
  ca=$(sed 's/$/\\n/' "$tls_dir/ca.pem" | tr -d '\n') wrong=$(sed 's/$/\\n/' "$tls_dir/wrong-ca.pem" | tr -d '\n')
  hp=$(cat "$tls_dir/https.port") ep=$(cat "$tls_dir/echo.port")
  e='(do (require (quote [arbace.tls :as tls]) (quote [arbace.http :as http])) (def ca "'"$ca"'") (def wrong "'"$wrong"'") (def hp '"$hp"') (def ep '"$ep"') (def url (str "https://localhost:" hp)) (defn e [f] (try (f) :ok (catch Exception x (or (:arbace.tls/error (ex-data x)) (.getName (class x)))))) (defn pat [n] (let [a (byte-array n)] (dotimes [i n] (aset a i (unchecked-byte (mod i 251)))) a)) nil)'
  check "TLS: a handshake, the session" '[{:protocol "TLSv1.3", :application-protocol "http/1.1", :server-name "localhost"} 2]' \
    "$($run -e "$e" -e '(with-open [s (tls/connect "localhost" hp {:roots ca :alpn ["h2" "http/1.1"]})] (prn [(dissoc (tls/session s) :cipher-suite) (count (tls/peer-certificates s))]))' 2>&1)"
  check "TLS: verification: the wrong CA, the wrong name, the default roots, no roots, an IP address" "[:verify :verify :verify :roots :ok]" \
    "$($run -e "$e" -e '(prn [(e #(tls/connect "localhost" hp {:roots wrong})) (e #(tls/connect "127.0.0.1" hp {:roots ca :server-name "wrong.example"})) (e #(tls/connect "localhost" hp)) (e #(tls/connect "localhost" hp {:roots "no PEM"})) (e #(.close (tls/connect "127.0.0.1" hp {:roots ca})))])' 2>&1)"
  check "TLS: the roots ARBACE_TLS_ROOTS names" '[200 "hello over TLS\n"]' \
    "$(ARBACE_TLS_ROOTS=$roots $run -e "$e" -e '(prn ((juxt :status :body) (http/request {:url (str url "/hello")})))' 2>&1)"
  check "TLS: a request and its response, 1 MB each way" "[200 true]" \
    "$($run -e "$e" -e '(let [b (apply str (repeat 100000 "abcdefghij")) r (http/request {:method :post :url (str url "/echo") :body b :tls {:roots ca}})] (prn [(:status r) (= b (:body r))]))' 2>&1)"
  check "TLS: 3 MB bodies by Content-Length, chunked, to the connection's end" "[true true true]" \
    "$($run -e "$e" -e '(prn (mapv (fn [p] (java.util.Arrays/equals ^bytes (:body (http/request {:url (str url "/" p "?n=3000000") :as :bytes :tls {:roots ca}})) ^bytes (pat 3000000))) ["big" "chunked" "eof"]))' 2>&1)"
  check "TLS: 8 MiB both ways on a raw stream, ended by close_notify, then closed" "[:written true]" \
    "$($run -e "$e" -e '(let [s (tls/connect "localhost" ep {:roots ca}) d (pat (* 8 1024 1024)) w (future (let [o (.getOutputStream s)] (.write o d) (.flush o) (.shutdownOutput s) :written)) in (.getInputStream s) bo (java.io.ByteArrayOutputStream.) buf (byte-array 65536)] (loop [] (let [k (.read in buf)] (when (pos? k) (.write bo buf 0 k) (recur)))) (prn [@w (java.util.Arrays/equals (.toByteArray bo) d)]) (.close s) (shutdown-agents))' 2>&1)"
  check "TLS: an API's refusal, 401 with a JSON body" '[401 "application/json" true]' \
    "$($run -e "$e" -e '(let [r (http/request {:method :post :url (str url "/json") :headers {"content-type" "application/json"} :body "{}" :tls {:roots ca}})] (prn [(:status r) (get-in r [:headers "content-type"]) (boolean (re-find #"authentication_error" (:body r)))]))' 2>&1)"
}
