;; jrt: sockets and the name service (doc/go/JRT-NOTES.md, "Sockets"): the natives of
;; jdk.internal.jrt.HostNet (overlay/jdk/java.base/jdk/internal/jrt/HostNet.java), the socket
;; table under jrt's HostSocketImpl and the variants of java.net's InetAddress, Inet4AddressImpl
;; and Inet6AddressImpl. The network is a host's: NetHost, an interface a Host may implement
;; besides Host (B1a's OSHost does, over Go's net and syscall); a host without it has no network,
;; and every native fails with HostNet.UNSUPPORTED.
(in-ns 'go.arbace.jrt)

(go/file "net.go"
  :imports [[context "context"] [errors "errors"] [io "io"] [net "net"] [os "os"] [strconv "strconv"]
            [strings "strings"] [sync "sync"] [syscall "syscall"] [time "time"] [unsafe "unsafe"]])

(go/type NetHost
  "NetHost is the network of a host (java.net's sockets and name service). A Host implements it
to give the program a network; jrt asks the current host for it at each use.\n"
  (interface
    (^{:doc "Listen listens for TCP connections at address (host:port; an empty host: every
address of the host, both families).\n"}
      Listen [^string address] :results [net/Listener error])
    (^{:doc "Dial connects to remote (host:port), from local when it is not empty, giving up
after timeout when it is positive.\n"}
      Dial [^string local ^string remote ^{:tag time/Duration} timeout] :results [net/Conn error])
    (^{:doc "LookupIP is the addresses of host.\n"}
      LookupIP [^string host] :results [(slice net/IP) error])
    (^{:doc "LookupAddr is the host names of address ip.\n"}
      LookupAddr [^{:tag net/IP} ip] :results [(slice string) error])
    (^{:doc "Hostname is the host's name.\n"}
      Hostname [] :results [string error])
    (^{:doc "HasFamily tells whether the host has IPv6 (or IPv4) addresses.\n"}
      HasFamily ^bool [^bool ipv6])
    (^{:doc "SetOption sets option opt (java.net.SocketOptions' number) of a listener or a
connection.\n"}
      SetOption ^error [^any s ^int opt ^int value])
    (^{:doc "GetOption is option opt of a listener or a connection.\n"}
      GetOption [^any s ^int opt] :results [int error])
    (^{:doc "Available is the bytes connection c can give without blocking (0 when unknown).\n"}
      Available ^int [^{:tag net/Conn} c])))

;; HostNet's error kinds (HostNet.java)
(go/const
  [^{:tag int32 :val -1} netEOF -1]
  [^{:tag int32 :val -2} netError -2]
  [^{:tag int32 :val -3} netBind -3]
  [^{:tag int32 :val -4} netRefused -4]
  [^{:tag int32 :val -5} netNoRoute -5]
  [^{:tag int32 :val -6} netTimeout -6]
  [^{:tag int32 :val -7} netUnknownHost -7]
  [^{:tag int32 :val -8} netClosed -8]
  [^{:tag int32 :val -9} netReset -9]
  [^{:tag int32 :val -10} netUnsupported -10])

;; java.net.SocketOptions' numbers
(go/const
  [^{:val 1} optTcpNoDelay 1]
  [^{:val 3} optIpTos 3]
  [^{:val 4} optReuseAddr 4]
  [^{:val 8} optKeepAlive 8]
  [^{:val 14} optReusePort 14]
  [^{:val 128} optLinger 128]
  [^{:val 4097} optSndBuf 4097]
  [^{:val 4098} optRcvBuf 4098]
  [^{:val 4099} optOOBInline 4099])

(go/func netHost "netHost is the current host's network, if it has one.\n"
  [] :results [NetHost bool]
  (let [(values n ok) (assert NetHost (CurrentHost))]
    (return n ok)))

;; ---------------------------------------------------------------------------------------
;; Errors: the kind HostSocketImpl throws and the JVM's text (the C library's strerror and
;; gai_strerror; this machine's JVM is built on musl, whose texts these are)

(go/var ^{:tag (map syscall/Errno string)} errnoText
  (lit (map syscall/Errno string)
       [syscall/EADDRINUSE "Address in use"]
       [syscall/EADDRNOTAVAIL "Address not available"]
       [syscall/ECONNREFUSED "Connection refused"]
       [syscall/ECONNRESET "Connection reset by peer"]
       [syscall/ECONNABORTED "Connection aborted"]
       [syscall/EPIPE "Broken pipe"]
       [syscall/EHOSTUNREACH "Host is unreachable"]
       [syscall/ENETUNREACH "Network unreachable"]
       [syscall/ETIMEDOUT "Operation timed out"]
       [syscall/EACCES "Permission denied"]
       [syscall/ENOTCONN "Socket not connected"]
       [syscall/EINVAL "Invalid argument"]
       [syscall/EAFNOSUPPORT "Address family not supported by protocol"]))

(go/func capitalized ^string [^string s]
  (when (== s "")
    (return s))
  (+ (strings/ToUpper (subslice s 0 1)) (subslice s 1)))

(go/func netErrorKind
  "netErrorKind is HostNet's kind of a host error and its text; reading tells a read's error
(a reset connection is its own kind there, as NioSocketImpl's ConnectionResetException).\n"
  [^error err ^bool reading] :results [int32 string]
  (when (errors/Is err net/ErrClosed)
    (return netClosed "Socket closed"))
  (when (errors/Is err os/ErrDeadlineExceeded)
    (return netTimeout "timed out"))
  (let [^syscall/Errno errno 0]
    (when (errors/As err (addr errno))
      (let [(values text ok) (aget errnoText errno)]
        (when (not ok)
          (set! text (capitalized (.Error errno))))
        (switch errno
          (case [syscall/ECONNREFUSED] (return netRefused text))
          (case [syscall/EADDRINUSE syscall/EADDRNOTAVAIL syscall/EACCES] (return netBind text))
          (case [syscall/EHOSTUNREACH] (return netNoRoute text))
          (case [syscall/ECONNRESET]
            (when reading
              (return netReset text))))
        (return netError text))))
  (let [^{:tag (* net/DNSError)} de nil]
    (when (errors/As err (addr de))
      (return netUnknownHost (dnsText (.-Name de) de))))
  (return netError (capitalized (.Error err))))

(go/func dnsText "dnsText is the JVM's UnknownHostException text: host: gai_strerror's.\n"
  ^string [^string host ^{:tag (* net/DNSError)} de]
  (cond (.-IsNotFound de) (return (+ host ": Name does not resolve"))
        (or (.-IsTemporary de) (.-IsTimeout de)) (return (+ host ": Temporary failure in name resolution")))
  (+ host ": Non-recoverable failure in name resolution"))

(go/func setError "setError puts text into error[0] and returns kind.\n"
  ^int32 [^{:tag (* RefArray)} errs ^int32 kind ^string text]
  (when (and (!= errs nil) (> (len (.-A errs)) 0))
    (aset (.-A errs) 0 (Str text)))
  kind)

(go/func failed "failed is setError of a host error.\n"
  ^int32 [^{:tag (* RefArray)} errs ^error err ^bool reading]
  (let [(values kind text) (netErrorKind err reading)]
    (setError errs kind text)))

;; ---------------------------------------------------------------------------------------
;; The socket table

(go/type netSock
  "netSock is an open socket of the table: a listener or a connection.\n"
  (struct ^{:tag net/Listener} l ^{:tag net/Conn} c))

(go/var ^{:tag sync/Mutex} socksMu)
(go/var ^{:tag (map int32 (* netSock))} openSocks (make (map int32 (* netSock))))
(go/var ^int32 nextSock 0)

(go/func addSock "addSock enters s into the table: its handle.\n" ^int32 [^{:tag (* netSock)} s]
  (.Lock socksMu)
  (defer (.Unlock socksMu))
  (let [h nextSock]
    (inc! nextSock)
    (aset openSocks h s)
    h))

(go/func sock "sock is the socket of handle h, or nil when it is closed.\n"
  ^{:tag (* netSock)} [^int32 h]
  (.Lock socksMu)
  (defer (.Unlock socksMu))
  (aget openSocks h))

(go/func joinHostPort ^string [^{:tag (* String)} host ^int32 port]
  (let [hs ""]
    (when (!= host nil)
      (set! hs (.String host)))
    (net/JoinHostPort hs (strconv/Itoa (conv int port)))))

(go/func millis ^{:tag time/Duration} [^int32 ms]
  (* (conv time/Duration ms) time/Millisecond))

(go/func HostNet_Listen0_String_I_String1__I_native
  "HostNet_Listen0_String_I_String1__I_native listens at host (an IP literal, empty for every
address) and port: a handle, or an error kind with its text in error[0].\n"
  ^int32 [^{:tag (* String)} host ^int32 port ^{:tag (* RefArray)} errs]
  (let [(values nh ok) (netHost)]
    (when (not ok)
      (return (setError errs netUnsupported "Network not available")))
    (let [(values l err) (.Listen nh (joinHostPort host port))]
      (when (!= err nil)
        (return (failed errs err false)))
      (addSock (addr (lit netSock :l l))))))

(go/func HostNet_Accept0_I_I_String1__I_native
  "HostNet_Accept0_I_I_String1__I_native accepts a connection of listener h, waiting up to
timeout ms (0: no limit): its handle, or an error kind.\n"
  ^int32 [^int32 h ^int32 timeout ^{:tag (* RefArray)} errs]
  (let [s (sock h)]
    (when (or (== s nil) (== (.-l s) nil))
      (return (setError errs netClosed "Socket closed")))
    (let [(values dl ok) (assert (interface (SetDeadline ^error [^{:tag time/Time} t])) (.-l s))]
      (when ok
        (if (> timeout 0)
          (.SetDeadline dl (.Add (time/Now) (millis timeout)))
          (.SetDeadline dl (lit time/Time)))))
    (let [(values c err) (.Accept (.-l s))]
      (when (!= err nil)
        (return (failed errs err false)))
      (newConn c))))

(go/func newConn
  "newConn enters a new connection into the table with the JVM's defaults (no TCP_NODELAY, no
keep-alive: Go sets both on its connections).\n"
  ^int32 [^{:tag net/Conn} c]
  (let [(values tc ok) (assert (* net/TCPConn) c)]
    (when ok
      (.SetNoDelay tc false)
      (.SetKeepAlive tc false)))
  (addSock (addr (lit netSock :c c))))

(go/func HostNet_Connect0_String_I_String_I_I_String1__I_native
  "HostNet_Connect0_String_I_String_I_I_String1__I_native connects to host and port (from
localHost and localPort when localHost is not null), waiting up to timeout ms (0: no limit): a
handle, or an error kind.\n"
  ^int32 [^{:tag (* String)} host ^int32 port ^{:tag (* String)} localHost ^int32 localPort
          ^int32 timeout ^{:tag (* RefArray)} errs]
  (let [(values nh ok) (netHost)]
    (when (not ok)
      (return (setError errs netUnsupported "Network not available")))
    (let [local ""]
      (when (!= localHost nil)
        (set! local (joinHostPort localHost localPort)))
      (let [(values c err) (.Dial nh local (joinHostPort host port) (millis timeout))]
        (when (!= err nil)
          (return (failed errs err false)))
        (newConn c)))))

(go/func conn "conn is the connection of handle h, or nil when it is closed.\n"
  ^{:tag net/Conn} [^int32 h]
  (let [s (sock h)]
    (when (== s nil)
      (return nil))
    (.-c s)))

(go/func HostNet_Read0_I_B1_I_I_I_String1__I_native
  "HostNet_Read0_I_B1_I_I_I_String1__I_native reads up to len (> 0) bytes of connection h into
b at off, waiting up to timeout ms (0: no limit): the count, -1 at the end, or an error kind.\n"
  ^int32 [^int32 h ^{:tag (* ByteArray)} b ^int32 off ^int32 n ^int32 timeout ^{:tag (* RefArray)} errs]
  (let [c (conn h)]
    (when (== c nil)
      (return (setError errs netClosed "Socket closed")))
    (if (> timeout 0)
      (.SetReadDeadline c (.Add (time/Now) (millis timeout)))
      (.SetReadDeadline c (lit time/Time)))
    (let [p (byteView b off n)]
      (while true
        (let [(values k err) (.Read c p)]
          (when (> k 0)
            (return (conv int32 k)))
          (when (errors/Is err io/EOF)
            (return netEOF))
          (when (!= err nil)
            (return (failed errs err true))))))))

(go/func HostNet_Write0_I_B1_I_I_String1__I_native
  "HostNet_Write0_I_B1_I_I_String1__I_native writes len bytes of b from off to connection h:
0, or an error kind.\n"
  ^int32 [^int32 h ^{:tag (* ByteArray)} b ^int32 off ^int32 n ^{:tag (* RefArray)} errs]
  (let [c (conn h)]
    (when (== c nil)
      (return (setError errs netClosed "Socket closed")))
    (let [(values _ err) (.Write c (byteView b off n))]
      (when (!= err nil)
        (return (failed errs err false)))
      0)))

(go/func HostNet_Available0_I__I_native
  "HostNet_Available0_I__I_native is what connection h gives without blocking.\n"
  ^int32 [^int32 h]
  (let [c (conn h)
        (values nh ok) (netHost)]
    (when (or (== c nil) (not ok))
      (return 0))
    (conv int32 (.Available nh c))))

(go/func HostNet_Close0_I__V_native
  "HostNet_Close0_I__V_native closes handle h; an operation blocked on it ends with
net.ErrClosed (HostNet.CLOSED).\n"
  [^int32 h]
  (.Lock socksMu)
  (let [s (aget openSocks h)]
    (delete openSocks h)
    (.Unlock socksMu)
    (when (== s nil)
      (return))
    (if (!= (.-l s) nil)
      (.Close (.-l s))
      (.Close (.-c s)))))

(go/func HostNet_Shutdown0_I_I_String1__I_native
  "HostNet_Shutdown0_I_I_String1__I_native shuts connection h down for reading (0) or writing
(1): 0, or an error kind.\n"
  ^int32 [^int32 h ^int32 how ^{:tag (* RefArray)} errs]
  (let [c (conn h)]
    (when (== c nil)
      (return (setError errs netClosed "Socket closed")))
    (let [(values tc ok) (assert (interface (CloseRead ^error []) (CloseWrite ^error [])) c)]
      (when (not ok)
        (return 0))
      (let [^error err nil]
        (if (== how 0)
          (set! err (.CloseRead tc))
          (set! err (.CloseWrite tc)))
        (when (!= err nil)
          (return (failed errs err false)))
        0))))

(go/func sockAddr "sockAddr is the local (or remote) address of handle h.\n"
  ^{:tag (* net/TCPAddr)} [^int32 h ^bool local]
  (let [s (sock h)
        ^{:tag net/Addr} a nil]
    (cond (== s nil) (return nil)
          (!= (.-l s) nil) (set! a (.Addr (.-l s)))
          local (set! a (.LocalAddr (.-c s)))
          :else (set! a (.RemoteAddr (.-c s))))
    (let [(values ta ok) (assert (* net/TCPAddr) a)]
      (when (not ok)
        (return nil))
      ta)))

(go/func ipBytes "ipBytes is ip as Java's address bytes: 4 for IPv4, else 16.\n"
  ^{:tag (* ByteArray)} [^{:tag net/IP} ip]
  (let [v4 (.To4 ip)]
    (when (!= v4 nil)
      (set! ip v4))
    (let [b (NewByteArray (conv int32 (len ip)))]
      (range [i x ip]
        (aset (.-A b) i (conv int8 x)))
      b)))

(go/func HostNet_Address0_I_Z__B1_native
  "HostNet_Address0_I_Z__B1_native is the local (or remote) address of handle h (0.0.0.0 when
it is closed).\n"
  ^{:tag (* ByteArray)} [^int32 h ^bool local]
  (let [a (sockAddr h local)]
    (when (or (== a nil) (== (.-IP a) nil))
      (return (NewByteArray 4)))
    (ipBytes (.-IP a))))

(go/func HostNet_Port0_I_Z__I_native
  "HostNet_Port0_I_Z__I_native is the local (or remote) port of handle h (0 when it is closed).\n"
  ^int32 [^int32 h ^bool local]
  (let [a (sockAddr h local)]
    (when (== a nil)
      (return 0))
    (conv int32 (.-Port a))))

(go/func sockOf "sockOf is the listener or the connection of handle h, or nil.\n"
  ^any [^int32 h]
  (let [s (sock h)]
    (cond (== s nil) (return nil)
          (!= (.-l s) nil) (return (.-l s)))
    (.-c s)))

(go/func HostNet_SetOption0_I_I_I_String1__I_native
  "HostNet_SetOption0_I_I_I_String1__I_native sets option opt of handle h: 0, or an error kind.\n"
  ^int32 [^int32 h ^int32 opt ^int32 value ^{:tag (* RefArray)} errs]
  (let [s (sockOf h)
        (values nh ok) (netHost)]
    (when (== s nil)
      (return (setError errs netClosed "Socket closed")))
    (when (not ok)
      (return (setError errs netUnsupported "Network not available")))
    (let [err (.SetOption nh s (conv int opt) (conv int value))]
      (when (!= err nil)
        (return (failed errs err false)))
      0)))

(go/func HostNet_GetOption0_I_I_I1_String1__I_native
  "HostNet_GetOption0_I_I_I1_String1__I_native puts option opt of handle h into value[0]: 0, or
an error kind.\n"
  ^int32 [^int32 h ^int32 opt ^{:tag (* IntArray)} value ^{:tag (* RefArray)} errs]
  (let [s (sockOf h)
        (values nh ok) (netHost)]
    (when (== s nil)
      (return (setError errs netClosed "Socket closed")))
    (when (not ok)
      (return (setError errs netUnsupported "Network not available")))
    (let [(values v err) (.GetOption nh s (conv int opt))]
      (when (!= err nil)
        (return (failed errs err false)))
      (aset (.-A value) 0 (conv int32 v))
      0)))

;; ---------------------------------------------------------------------------------------
;; The name service

(go/func HostNet_Lookup0_String_I_String1__B2_native
  "HostNet_Lookup0_String_I_String1__B2_native is the addresses of host as LookupPolicy's
characteristics select and order them (IPV4 1, IPV6 2, IPV4_FIRST 4, IPV6_FIRST 8), or null with
the JVM's text in error[0].\n"
  ^{:tag (* RefArray)} [^{:tag (* String)} host ^int32 characteristics ^{:tag (* RefArray)} errs]
  (let [name (.String (NN host))
        (values nh ok) (netHost)]
    (when (not ok)
      (setError errs netUnsupported (+ name ": Name does not resolve"))
      (return nil))
    (let [(values ips err) (.LookupIP nh name)]
      (when (!= err nil)
        (let [^{:tag (* net/DNSError)} de nil]
          (if (errors/As err (addr de))
            (setError errs netUnknownHost (dnsText name de))
            (setError errs netUnknownHost (+ name ": Name does not resolve"))))
        (return nil))
      (let [v4 (make (slice net/IP) 0 (len ips))
            v6 (make (slice net/IP) 0 (len ips))
            all (make (slice net/IP) 0 (len ips))]
        (range [_ ip ips]
          (if (!= (.To4 ip) nil)
            (when (!= (bit-and characteristics 1) 0)
              (set! v4 (append v4 ip))
              (set! all (append all ip)))
            (when (!= (bit-and characteristics 2) 0)
              (set! v6 (append v6 ip))
              (set! all (append all ip)))))
        (cond (!= (bit-and characteristics 4) 0) (set! all (append v4 (spread v6)))
              (!= (bit-and characteristics 8) 0) (set! all (append v6 (spread v4))))
        (when (== (len all) 0)
          (setError errs netUnknownHost (+ name ": Name does not resolve"))
          (return nil))
        (let [r (NewRefArray (.ArrayClass Prim_byte) (conv int32 (len all)))]
          (range [i ip all]
            (aset (.-A r) i (ipBytes ip)))
          r)))))

(go/func HostNet_Reverse0_B1_String1__String_native
  "HostNet_Reverse0_B1_String1__String_native is the host name of address addr, or null.\n"
  ^{:tag (* String)} [^{:tag (* ByteArray)} addr ^{:tag (* RefArray)} errs]
  (let [(values nh ok) (netHost)]
    (when (not ok)
      (return nil))
    (let [ip (make net/IP (len (.-A (NN addr))))]
      (range [i x (.-A addr)]
        (aset ip i (conv byte x)))
      (let [(values names err) (.LookupAddr nh ip)]
        (when (or (!= err nil) (== (len names) 0))
          (when (!= err nil)
            (setError errs netUnknownHost (.Error err)))
          (return nil))
        (Str (strings/TrimSuffix (aget names 0) "."))))))

(go/func HostNet_HostName0_String1__String_native
  "HostNet_HostName0_String1__String_native is the host's name, or null.\n"
  ^{:tag (* String)} [^{:tag (* RefArray)} errs]
  (let [(values nh ok) (netHost)]
    (when (not ok)
      (return nil))
    (let [(values name err) (.Hostname nh)]
      (when (!= err nil)
        (setError errs netError (.Error err))
        (return nil))
      (Str name))))

(go/func HostNet_HasFamily0_Z__Z_native
  "HostNet_HasFamily0_Z__Z_native tells whether the host has IPv6 (or IPv4) addresses.\n"
  ^bool [^bool ipv6]
  (let [(values nh ok) (netHost)]
    (when (not ok)
      (return (not ipv6)))
    (.HasFamily nh ipv6)))

;; ---------------------------------------------------------------------------------------
;; B1a's network: OSHost over Go's net (the pure Go resolver: /etc/hosts, then DNS) and, for
;; the options, the sockets' descriptors through syscall

(go/method Listen [^OSHost h ^string address] :results [net/Listener error]
  ;; no keep-alive on accepted connections (the JVM's default; Go's is 15 s)
  (.Listen (addr (lit net/ListenConfig :KeepAlive -1)) (context/Background) "tcp" address))

(go/method Dial [^OSHost h ^string local ^string remote ^{:tag time/Duration} timeout]
  :results [net/Conn error]
  (let [d (lit net/Dialer :Timeout timeout :KeepAlive -1)]
    (when (!= local "")
      (let [(values la err) (net/ResolveTCPAddr "tcp" local)]
        (when (!= err nil)
          (return nil err))
        (set! (.-LocalAddr d) la)))
    (.Dial d "tcp" remote)))

(go/method LookupIP [^OSHost h ^string host] :results [(slice net/IP) error]
  (net/LookupIP host))

(go/method LookupAddr [^OSHost h ^{:tag net/IP} ip] :results [(slice string) error]
  (net/LookupAddr (.String ip)))

(go/method Hostname [^OSHost h] :results [string error]
  (os/Hostname))

(go/method HasFamily ^bool [^OSHost h ^bool ipv6]
  (let [(values addrs err) (net/InterfaceAddrs)]
    (when (!= err nil)
      (return (not ipv6)))
    (range [_ a addrs]
      (let [(values ipn ok) (assert (* net/IPNet) a)]
        (when (and ok (== (== (.To4 (.-IP ipn)) nil) ipv6))
          (return true))))
    false))

(go/func rawConn "rawConn is the descriptor access of a listener or a connection.\n"
  [^any s] :results [syscall/RawConn error]
  (let [(values sc ok) (assert syscall/Conn s)]
    (when (not ok)
      (return nil (errors/New "Socket has no descriptor")))
    (.SyscallConn sc)))

(go/func sockopt "sockopt is the level and the option of SocketOptions' number opt.\n"
  [^int opt] :results [int int bool]
  (switch opt
    (case [optTcpNoDelay] (return syscall/IPPROTO_TCP syscall/TCP_NODELAY true))
    (case [optIpTos] (return syscall/IPPROTO_IP syscall/IP_TOS true))
    (case [optReuseAddr] (return syscall/SOL_SOCKET syscall/SO_REUSEADDR true))
    (case [optKeepAlive] (return syscall/SOL_SOCKET syscall/SO_KEEPALIVE true))
    ;; SO_REUSEPORT (15 on amd64 and arm64; amd64's syscall lacks the name)
    (case [optReusePort] (return syscall/SOL_SOCKET 15 true))
    (case [optSndBuf] (return syscall/SOL_SOCKET syscall/SO_SNDBUF true))
    (case [optRcvBuf] (return syscall/SOL_SOCKET syscall/SO_RCVBUF true))
    (case [optOOBInline] (return syscall/SOL_SOCKET syscall/SO_OOBINLINE true)))
  (return 0 0 false))

(go/method SetOption ^error [^OSHost h ^any s ^int opt ^int value]
  (let [(values rc err) (rawConn s)]
    (when (!= err nil)
      (return err))
    (let [^error serr nil]
      (if (== opt optLinger)
        (let [l (lit syscall/Linger)]
          (when (>= value 0)
            (set! (.-Onoff l) 1)
            (set! (.-Linger l) (conv int32 value)))
          (set! err (.Control rc (fn [^uintptr fd]
                                   (set! serr (syscall/SetsockoptLinger (conv int fd) syscall/SOL_SOCKET
                                                                        syscall/SO_LINGER (addr l)))))))
        (let [(values level name ok) (sockopt opt)]
          (when (not ok)
            (return (errors/New (+ "Unknown option " (strconv/Itoa opt)))))
          (set! err (.Control rc (fn [^uintptr fd]
                                   (set! serr (syscall/SetsockoptInt (conv int fd) level name value)))))))
      (when (!= err nil)
        (return err))
      serr)))

(go/method GetOption [^OSHost h ^any s ^int opt] :results [int error]
  (let [(values rc err) (rawConn s)]
    (when (!= err nil)
      (return 0 err))
    (let [^int v 0
          ^error serr nil]
      (if (== opt optLinger)
        (let [l (lit syscall/Linger)
              ^uint32 size (conv uint32 (unsafe/Sizeof l))]
          (set! err (.Control rc (fn [^uintptr fd]
                                   (let [(values _ _ e) (syscall/Syscall6 syscall/SYS_GETSOCKOPT fd
                                                                           (conv uintptr syscall/SOL_SOCKET)
                                                                           (conv uintptr syscall/SO_LINGER)
                                                                           (conv uintptr (conv unsafe/Pointer (addr l)))
                                                                           (conv uintptr (conv unsafe/Pointer (addr size)))
                                                                           0)]
                                     (when (!= e 0)
                                       (set! serr e))))))
          (if (== (.-Onoff l) 0)
            (set! v -1)
            (set! v (conv int (.-Linger l)))))
        (let [(values level name ok) (sockopt opt)]
          (when (not ok)
            (return 0 (errors/New (+ "Unknown option " (strconv/Itoa opt)))))
          (set! err (.Control rc (fn [^uintptr fd]
                                   (set! (values v serr) (syscall/GetsockoptInt (conv int fd) level name)))))))
      (when (!= err nil)
        (return 0 err))
      (when (and (== serr nil) (!= opt optLinger) (!= opt optSndBuf) (!= opt optRcvBuf) (!= opt optIpTos) (!= v 0))
        ;; a boolean option: 1 when set
        (set! v 1))
      (return v serr))))

(go/method Available ^int [^OSHost h ^{:tag net/Conn} c]
  (let [(values rc err) (rawConn c)]
    (when (!= err nil)
      (return 0))
    (let [^int32 n 0]
      (.Control rc (fn [^uintptr fd]
                     (syscall/Syscall syscall/SYS_IOCTL fd (conv uintptr syscall/TIOCINQ)
                                      (conv uintptr (conv unsafe/Pointer (addr n))))))
      (conv int n))))
