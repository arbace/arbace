;; java.net (doc/go/JRT-NOTES.md, "Sockets"): addresses from literals and bytes (no name
;; service: only what does not depend on the host's names), their printing, equality and kinds;
;; socket addresses; sockets over the loopback address on ports the host chooses (the values a
;; connection carries, its states and the exceptions of misuse); arbace.java.io on sockets. No
;; host names, port numbers, buffer sizes or timings appear in the results.

(import '[java.net InetAddress Inet4Address Inet6Address InetSocketAddress Socket ServerSocket
          SocketException SocketTimeoutException UnknownHostException])
(require '[arbace.java.io :as io])

;; ----- addresses from literals
(InetAddress/getByName "127.0.0.1")
(str (InetAddress/getByName "127.0.0.1"))
(str (InetAddress/getByName "10.1.2.3"))
(str (InetAddress/getByName "::1"))
(str (InetAddress/getByName "[::1]"))
(str (InetAddress/getByName "fe80::1:2"))
(str (InetAddress/getByName "2001:db8::ff00:42:8329"))
(str (InetAddress/getByName "::ffff:10.0.0.1"))
(class (InetAddress/getByName "::ffff:10.0.0.1"))
(class (InetAddress/getByName "::1"))
(.getHostAddress (InetAddress/getByName "2001:0db8:0000:0000:0000:0000:0000:0001"))
(vec (.getAddress (InetAddress/getByName "192.168.0.255")))
(vec (.getAddress (InetAddress/getByName "::2")))
(str (InetAddress/ofLiteral "1.2.3.4"))
(str (InetAddress/ofLiteral "::3"))
(str (Inet4Address/ofLiteral "255.255.255.255"))
(str (Inet6Address/ofLiteral "1::"))
(try (InetAddress/ofLiteral "1.2.3") (catch IllegalArgumentException e (.getMessage e)))
(try (InetAddress/ofLiteral "1.2.3.256") (catch IllegalArgumentException e (.getMessage e)))
(try (Inet4Address/ofLiteral "::1") (catch IllegalArgumentException e (.getMessage e)))
(try (InetAddress/getByName "[1.2.3.4") (catch UnknownHostException e (.getMessage e)))
(try (InetAddress/getByName "[1.2.3.4]") (catch UnknownHostException e (.getMessage e)))
(try (InetAddress/getByName "::1::2") (catch UnknownHostException e (.getMessage e)))
(str (Inet4Address/ofPosixLiteral "0x7f.1"))
(str (Inet4Address/ofPosixLiteral "010.0.0.1"))

;; ----- addresses from bytes
(str (InetAddress/getByAddress (byte-array [10 0 0 1])))
(str (InetAddress/getByAddress "named" (byte-array [10 0 0 1])))
(str (InetAddress/getByAddress (byte-array (concat (repeat 15 0) [1]))))
(str (InetAddress/getByAddress "six" (byte-array (concat [0x20 0x01 0x0d 0xb8] (repeat 11 0) [7]))))
(class (InetAddress/getByAddress (byte-array (concat (repeat 10 0) [-1 -1 10 0 0 1]))))
(try (InetAddress/getByAddress (byte-array [1 2 3])) (catch UnknownHostException e (.getMessage e)))
(.getHostName (InetAddress/getByAddress "given" (byte-array [1 2 3 4])))
(str (Inet6Address/getByAddress "h" (byte-array (concat (repeat 15 0) [1])) 3))
(.getScopeId (Inet6Address/getByAddress "h" (byte-array (concat (repeat 15 0) [1])) 3))

;; ----- equality, hashes and kinds
(= (InetAddress/getByName "10.0.0.1") (InetAddress/getByAddress "x" (byte-array [10 0 0 1])))
(.hashCode (InetAddress/getByName "10.0.0.1"))
(.hashCode (InetAddress/getByName "::1"))
(= (InetAddress/getByName "::1") (InetAddress/getByName "0:0:0:0:0:0:0:1"))
(for [a ["127.0.0.1" "0.0.0.0" "224.0.0.1" "169.254.1.1" "10.0.0.1" "8.8.8.8" "::1" "::" "fe80::1" "fec0::1" "ff02::1" "ff05::1" "ff0e::1"]
      :let [x (InetAddress/getByName a)]]
  [a (.isLoopbackAddress x) (.isAnyLocalAddress x) (.isMulticastAddress x) (.isLinkLocalAddress x)
   (.isSiteLocalAddress x) (.isMCGlobal x) (.isMCLinkLocal x) (.isMCSiteLocal x)])
(str (InetAddress/getLoopbackAddress))
(.isIPv4CompatibleAddress (InetAddress/getByName "::10.0.0.1"))

;; ----- socket addresses
(str (InetSocketAddress. (InetAddress/getByName "10.0.0.1") 80))
(str (InetSocketAddress. (InetAddress/getByName "::1") 8080))
(str (InetSocketAddress/createUnresolved "example.invalid" 1))
(.isUnresolved (InetSocketAddress/createUnresolved "example.invalid" 1))
(.getHostString (InetSocketAddress. (InetAddress/getByName "10.0.0.1") 80))
(str (InetSocketAddress. 7))
(= (InetSocketAddress. (InetAddress/getByName "10.0.0.1") 80) (InetSocketAddress. (InetAddress/getByName "10.0.0.1") 80))
(try (InetSocketAddress. (InetAddress/getByName "10.0.0.1") 70000) (catch IllegalArgumentException e (.getMessage e)))
(try (InetSocketAddress/createUnresolved nil 1) (catch IllegalArgumentException e (.getMessage e)))

;; ----- sockets over the loopback address
(def lo (InetAddress/getByName "127.0.0.1"))
(def ss (ServerSocket. 0 50 lo))
(def port (.getLocalPort ss))
[(.isBound ss) (.isClosed ss) (str (.getInetAddress ss)) (pos? port) (.getSoTimeout ss)]
(def c (Socket. lo port))
(def s (.accept ss))
[(.isConnected c) (.isBound c) (.isClosed c) (= port (.getPort c)) (= (.getLocalPort c) (.getPort s))]
[(str (.getInetAddress c)) (str (.getLocalAddress s)) (.getTcpNoDelay c) (.getKeepAlive c) (.getSoLinger c) (.getSoTimeout c)]
(do (.setTcpNoDelay c true) (.setSoLinger c true 3) (.setKeepAlive c true) [(.getTcpNoDelay c) (.getSoLinger c) (.getKeepAlive c)])
(do (.write (.getOutputStream c) (.getBytes "hello\n" "UTF-8")) (.flush (.getOutputStream c)) nil)
(let [r (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream s) "UTF-8"))] (.readLine r))
(do (.write (.getOutputStream s) (byte-array (range 0 200))) nil)
(let [in (.getInputStream c) b (byte-array 200)] (loop [n 0] (if (< n 200) (recur (+ n (.read in b n (- 200 n)))) (vec (take-last 5 b)))))
(do (.setSoTimeout c 50) (try (.read (.getInputStream c)) (catch SocketTimeoutException e [(class e) (.getMessage e)])))
(do (.setSoTimeout c 0) (.shutdownOutput s) [(.isOutputShutdown s) (.read (.getInputStream c)) (.read (.getInputStream c))])
(try (.write (.getOutputStream s) 1) (catch SocketException e (.getMessage e)))
(with-open [w (io/writer c)] (.write w "via io\n"))
(.isClosed c)
(slurp (io/reader s))
(do (.close s) (.close c) [(.isClosed s) (.isClosed c)])
(try (.getInputStream c) (catch SocketException e (.getMessage e)))
(try (.getOutputStream (Socket.)) (catch SocketException e (.getMessage e)))
(try (.connect (Socket.) (InetSocketAddress/createUnresolved "nowhere.invalid" 1)) (catch UnknownHostException e (.getMessage e)))
(try (.connect c (InetSocketAddress. lo port)) (catch SocketException e (.getMessage e)))
(do (.setSoTimeout ss 30) (try (.accept ss) (catch SocketTimeoutException e [(class e) (.getMessage e)])))
(try (ServerSocket. port 50 lo) (catch java.net.BindException e (.getMessage e)))
(do (.close ss) [(.isClosed ss) (try (.accept ss) (catch SocketException e (.getMessage e)))])
(try (Socket. lo port) (catch java.net.ConnectException e (.getMessage e)))
(str (Socket.))
(let [x (ServerSocket.)] [(str x) (.isBound x) (do (.bind x (InetSocketAddress. lo 0)) (.isBound x)) (do (.close x) (.isClosed x))])
(try (ServerSocket. -1) (catch IllegalArgumentException e (.getMessage e)))
