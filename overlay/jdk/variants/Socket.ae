;; Go-build variant of java.net.Socket (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"): read by
;; c2g only. Socket updates its state bits and installs its streams through VarHandles
;; (java.lang.invoke is not in the Go build); the variant does the same atomic operations through
;; Unsafe (a compare-and-set loop for getAndBitwiseOr, compareAndSetReference for the streams),
;; with the fields' offsets from objectFieldOffset(Class, String). A client socket's SocketImpl is
;; the platform's (jrt's HostSocketImpl) itself, not wrapped in a SocksSocketImpl (no proxies in
;; the Go build), and the streams record no JFR events (jdk.internal.event is not in the Go
;; build). The replacing methods keep jdk26u's code where they keep it (baf63fb; Copyright (c)
;; Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest
;; Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant Socket
  (c2g/cut (field ^:private ^:static ^:final ^java.lang.invoke.VarHandle STATE))
  (c2g/cut (field ^:private ^:static ^:final ^java.lang.invoke.VarHandle IN))
  (c2g/cut (field ^:private ^:static ^:final ^java.lang.invoke.VarHandle OUT))
  (c2g/cut (static-initializer 0))
  (c2g/add (field ^:private ^:static ^:final ^jdk.internal.misc.Unsafe U (jdk.internal.misc.Unsafe/getUnsafe)))
  (c2g/add (field ^:private ^:static ^:final ^long STATE_OFFSET (.objectFieldOffset U Socket "state")))
  (c2g/add (field ^:private ^:static ^:final ^long IN_OFFSET (.objectFieldOffset U Socket "in")))
  (c2g/add (field ^:private ^:static ^:final ^long OUT_OFFSET (.objectFieldOffset U Socket "out")))

  (method ^:private getAndBitwiseOrState ^int [this ^int mask]
    (loop []
      (let [s state]
        (if (.compareAndSetInt U this STATE_OFFSET s (bit-or-int s mask))
            s
            (recur)))))

  (method ^:private ^:static createImpl ^SocketImpl []
    (let [factory Socket/factory]
      (if (some? factory)
          (.createSocketImpl factory)
          (SocketImpl/createPlatformSocketImpl false))))

  (method ^:public getInputStream :throws [java.io.IOException] ^java.io.InputStream [this]
    (let [s state]
      (when (Socket/isClosed s) (throw (SocketException. "Socket is closed")))
      (when-not (Socket/isConnected s) (throw (SocketException. "Socket is not connected")))
      (when (Socket/isInputShutdown s) (throw (SocketException. "Socket input is shutdown")))
      (let [^:mutable in (.-in this)]
        (when (nil? in)
          (set! in (Socket$SocketInputStream. this (.getInputStream impl)))
          (when-not (.compareAndSetReference U this IN_OFFSET nil in)
            (set! in (.-in this))))
        in)))

  (method ^:public getOutputStream :throws [java.io.IOException] ^java.io.OutputStream [this]
    (let [s state]
      (when (Socket/isClosed s) (throw (SocketException. "Socket is closed")))
      (when-not (Socket/isConnected s) (throw (SocketException. "Socket is not connected")))
      (when (Socket/isOutputShutdown s) (throw (SocketException. "Socket output is shutdown")))
      (let [^:mutable out (.-out this)]
        (when (nil? out)
          (set! out (Socket$SocketOutputStream. this (.getOutputStream impl)))
          (when-not (.compareAndSetReference U this OUT_OFFSET nil out)
            (set! out (.-out this))))
        out))))

(c2g/variant Socket$SocketInputStream
  (method ^:public read :throws [java.io.IOException] ^int [this ^byte/1 b ^int off ^int len]
    (.implRead this b off len)))

(c2g/variant Socket$SocketOutputStream
  (method ^:public write :throws [java.io.IOException] ^void [this ^byte/1 b ^int off ^int len]
    (.implWrite this b off len)))
