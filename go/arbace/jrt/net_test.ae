;; jrt's tests: the socket table and the name service under HostSocketImpl and InetAddress
;; (net.clj; doc/go/JRT-NOTES.md, "Sockets"), through HostNet's natives as the translated
;; classes call them.
(in-ns 'go.arbace.jrt)

(go/file "net_test.go"
  :imports [[strings "strings"] [sync "sync"] [testing "testing"] [time "time"]])

(go/func netErr "netErr is a fresh String[1] for a native's error text.\n" ^{:tag (* RefArray)} []
  (NewRefArray String_class 1))

(go/func errText ^string [^{:tag (* RefArray)} e]
  (let [s (aget (.-A e) 0)]
    (when (== s nil)
      (return ""))
    (.String (assert (* String) s))))

(go/func javaBytes ^{:tag (* ByteArray)} [^string s]
  (let [b (NewByteArray (conv int32 (len s)))]
    (for [i 0] (< i (len s)) (inc! i)
      (aset (.-A b) i (conv int8 (aget s i))))
    b))

(go/func stringOf ^string [^{:tag (* ByteArray)} b ^int32 n]
  (let [p (make (slice byte) n)]
    (for [i 0] (< i (conv int n)) (inc! i)
      (aset p i (conv byte (aget (.-A b) i))))
    (conv string p)))

(go/func listenLocal "listenLocal listens at 127.0.0.1 on a free port: the handle and the port.\n"
  [^{:tag (* testing/T)} t] :results [int32 int32]
  (let [e (netErr)
        l (HostNet_Listen0_String_I_String1__I_native (Str "127.0.0.1") 0 e)]
    (when (< l 0)
      (.Fatalf t "listen: %d %s" l (errText e)))
    (let [port (HostNet_Port0_I_Z__I_native l true)]
      (when (<= port 0)
        (.Fatalf t "port %d" port))
      (return l port))))

(go/func TestSockets
  "A connection through a listener: accept, both directions, the addresses and ports, a read's
timeout, available, shutdown of the output (the peer reads the end), close.\n"
  [^{:tag (* testing/T)} t]
  (let [(values l port) (listenLocal t)
        e (netErr)
        c (HostNet_Connect0_String_I_String_I_I_String1__I_native (Str "127.0.0.1") port nil 0 2000 e)]
    (when (< c 0)
      (.Fatalf t "connect: %d %s" c (errText e)))
    (let [s (HostNet_Accept0_I_I_String1__I_native l 2000 e)]
      (when (< s 0)
        (.Fatalf t "accept: %d %s" s (errText e)))
      (when (or (!= (HostNet_Port0_I_Z__I_native c false) port) (!= (HostNet_Port0_I_Z__I_native s true) port)
                (!= (HostNet_Port0_I_Z__I_native s false) (HostNet_Port0_I_Z__I_native c true)))
        (.Error t "ports"))
      (let [a (HostNet_Address0_I_Z__B1_native s false)]
        (when (or (!= (len (.-A a)) 4) (!= (aget (.-A a) 0) 127) (!= (aget (.-A a) 3) 1))
          (.Errorf t "remote address %v" (.-A a))))
      (when (!= (HostNet_Write0_I_B1_I_I_String1__I_native c (javaBytes "hello") 0 5 e) 0)
        (.Fatalf t "write: %s" (errText e)))
      (let [buf (NewByteArray 16)
            t0 (time/Now)]
        ;; available: what has arrived
        (while (and (< (HostNet_Available0_I__I_native s) 5) (< (time/Since t0) time/Second))
          (time/Sleep time/Millisecond))
        (when (!= (HostNet_Available0_I__I_native s) 5)
          (.Errorf t "available %d" (HostNet_Available0_I__I_native s)))
        (let [n (HostNet_Read0_I_B1_I_I_I_String1__I_native s buf 0 16 0 e)]
          (when (or (!= n 5) (!= (stringOf buf n) "hello"))
            (.Errorf t "read %d" n)))
        ;; nothing more: the read times out
        (let [t1 (time/Now)
              n (HostNet_Read0_I_B1_I_I_I_String1__I_native s buf 0 16 50 e)]
          (when (or (!= n netTimeout) (< (time/Since t1) (* 40 time/Millisecond)))
            (.Errorf t "timed read %d after %v" n (time/Since t1))))
        ;; the other way, then the end
        (HostNet_Write0_I_B1_I_I_String1__I_native s (javaBytes "back") 0 4 e)
        (when (!= (HostNet_Shutdown0_I_I_String1__I_native s 1 e) 0)
          (.Errorf t "shutdown: %s" (errText e)))
        (let [n (HostNet_Read0_I_B1_I_I_I_String1__I_native c buf 0 16 2000 e)]
          (when (or (!= n 4) (!= (stringOf buf n) "back"))
            (.Errorf t "read back %d" n)))
        (when (!= (HostNet_Read0_I_B1_I_I_I_String1__I_native c buf 0 16 2000 e) netEOF)
          (.Error t "the end after shutdown")))
      (HostNet_Close0_I__V_native s)
      (HostNet_Close0_I__V_native c)
      (when (!= (HostNet_Read0_I_B1_I_I_I_String1__I_native c (NewByteArray 1) 0 1 0 e) netClosed)
        (.Error t "a closed handle"))
      (HostNet_Close0_I__V_native l))))

(go/func TestSocketErrors
  "The JVM's exceptions and texts: a refused connection, an address in use, an accept's
timeout, an accept ended by close from another thread.\n"
  [^{:tag (* testing/T)} t]
  (let [(values l port) (listenLocal t)
        e (netErr)]
    (let [r (HostNet_Listen0_String_I_String1__I_native (Str "127.0.0.1") port e)]
      (when (or (!= r netBind) (!= (errText e) "Address in use"))
        (.Errorf t "listen twice: %d %q" r (errText e))))
    (let [r (HostNet_Accept0_I_I_String1__I_native l 30 e)]
      (when (!= r netTimeout)
        (.Errorf t "accept's timeout: %d" r)))
    (let [wg (lit sync/WaitGroup)
          ^int32 r 0]
      (.Add wg 1)
      (go ((fn []
             (set! r (HostNet_Accept0_I_I_String1__I_native l 0 (netErr)))
             (.Done wg))))
      (time/Sleep (* 20 time/Millisecond))
      (HostNet_Close0_I__V_native l)
      (.Wait wg)
      (when (!= r netClosed)
        (.Errorf t "accept after close: %d" r)))
    ;; the port is free now: refused
    (let [r (HostNet_Connect0_String_I_String_I_I_String1__I_native (Str "127.0.0.1") port nil 0 2000 e)]
      (when (or (!= r netRefused) (!= (errText e) "Connection refused"))
        (.Errorf t "connect to a closed port: %d %q" r (errText e))))))

(go/func TestSocketOptions
  "Options as the JVM's defaults are (no TCP_NODELAY, no keep-alive, no linger), set and read
back through the descriptor.\n"
  [^{:tag (* testing/T)} t]
  (let [(values l port) (listenLocal t)
        e (netErr)
        c (HostNet_Connect0_String_I_String_I_I_String1__I_native (Str "127.0.0.1") port nil 0 2000 e)
        v (NewIntArray 1)
        getOpt (fn ^int32 [^int32 opt]
              (when (!= (HostNet_GetOption0_I_I_I1_String1__I_native c opt v e) 0)
                (.Errorf t "get %d: %s" opt (errText e)))
              (aget (.-A v) 0))]
    (when (< c 0)
      (.Fatalf t "connect: %s" (errText e)))
    (when (or (!= (getOpt optTcpNoDelay) 0) (!= (getOpt optKeepAlive) 0) (!= (getOpt optLinger) -1))
      (.Errorf t "defaults: nodelay %d keepalive %d linger %d" (getOpt optTcpNoDelay) (getOpt optKeepAlive) (getOpt optLinger)))
    (HostNet_SetOption0_I_I_I_String1__I_native c optTcpNoDelay 1 e)
    (HostNet_SetOption0_I_I_I_String1__I_native c optLinger 5 e)
    (HostNet_SetOption0_I_I_I_String1__I_native c optRcvBuf 65536 e)
    (when (or (!= (getOpt optTcpNoDelay) 1) (!= (getOpt optLinger) 5) (< (getOpt optRcvBuf) 65536))
      (.Errorf t "set: nodelay %d linger %d rcvbuf %d" (getOpt optTcpNoDelay) (getOpt optLinger) (getOpt optRcvBuf)))
    (when (!= (HostNet_GetOption0_I_I_I1_String1__I_native l optReuseAddr v e) 0)
      (.Errorf t "listener's option: %s" (errText e)))
    (HostNet_Close0_I__V_native c)
    (HostNet_Close0_I__V_native l)))

(go/func TestLookup
  "localhost's addresses, IPv4 first as the JVM's default policy orders them, or IPv4 alone;
a name that does not resolve; reverse lookup of the loopback address; the host's name.\n"
  [^{:tag (* testing/T)} t]
  (let [e (netErr)
        r (HostNet_Lookup0_String_I_String1__B2_native (Str "localhost") 7 e)]
    (when (or (== r nil) (< (len (.-A r)) 1))
      (.Fatalf t "localhost: %s" (errText e)))
    (let [a (assert (* ByteArray) (aget (.-A r) 0))]
      (when (or (!= (len (.-A a)) 4) (!= (aget (.-A a) 0) 127))
        (.Errorf t "localhost's first address %v" (.-A a))))
    (let [r4 (HostNet_Lookup0_String_I_String1__B2_native (Str "localhost") 1 e)]
      (range [_ x (.-A r4)]
        (when (!= (len (.-A (assert (* ByteArray) x))) 4)
          (.Error t "IPv4 only"))))
    (let [bad (HostNet_Lookup0_String_I_String1__B2_native (Str "nonexistent.invalid") 7 e)]
      (when (or (!= bad nil) (not (strings/HasPrefix (errText e) "nonexistent.invalid: ")))
        (.Errorf t "a name that does not resolve: %q" (errText e))))
    (let [name (HostNet_Reverse0_B1_String1__String_native (javaBytes "\u007f\u0000\u0000\u0001") e)]
      (when (== name nil)
        (.Logf t "no name for 127.0.0.1 (%s)" (errText e))))
    (when (== (HostNet_HostName0_String1__String_native e) nil)
      (.Errorf t "host name: %s" (errText e)))
    (when (not (HostNet_HasFamily0_Z__Z_native false))
      (.Error t "no IPv4"))))

(go/type noNetHost
  "noNetHost is a host without a network (B1b's box before it has one).\n"
  (struct Host))

(go/func TestNoNetwork "A host that does not implement NetHost has no sockets.\n" [^{:tag (* testing/T)} t]
  (let [old (CurrentHost)]
    (SetHost (lit noNetHost :Host old))
    (defer (SetHost old))
    (let [e (netErr)]
      (when (!= (HostNet_Listen0_String_I_String1__I_native (Str "127.0.0.1") 0 e) netUnsupported)
        (.Error t "listen without a network"))
      (when (!= (HostNet_Lookup0_String_I_String1__B2_native (Str "localhost") 7 e) nil)
        (.Error t "lookup without a network")))))

(go/func TestToolOptions
  "JAVA_TOOL_OPTIONS split as HotSpot splits it, its -D options as system properties.\n"
  [^{:tag (* testing/T)} t]
  (let [got (strings/Join (splitToolOptions "  -Xmx1g -Darbace.server.repl=\"{:port 5555 :accept arbace.core.server/repl}\" -Da='x y'z -Dflag ") "|")]
    (when (!= got "-Xmx1g|-Darbace.server.repl={:port 5555 :accept arbace.core.server/repl}|-Da=x yz|-Dflag")
      (.Errorf t "split: %q" got)))
  (let [m (make (map string string))]
    (toolOptionProps (lit envHost :Host (CurrentHost) :env "-Dk=v -Dempty -D=x -Xss1m")
                     (fn [^string k ^string v] (aset m k v)))
    (when (or (!= (len m) 2) (!= (aget m "k") "v") (!= (aget m "empty") ""))
      (.Errorf t "properties: %v" m))))

(go/type envHost
  "envHost is a host whose JAVA_TOOL_OPTIONS is env.\n"
  (struct Host ^string env))

(go/method Getenv [^envHost h ^string name] :results [string bool]
  (when (== name "JAVA_TOOL_OPTIONS")
    (return (.-env h) true))
  (.Getenv (.-Host h) name))
