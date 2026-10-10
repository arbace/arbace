;; Go-build variant of java.net.ServerSocket (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Sockets"): read
;; by c2g only. implAccept(Socket) unwraps a SOCKS or HTTP-tunnel SocketImpl
;; (DelegatingSocketImpl), which is outside the closed world: no Socket of the Go build has one,
;; so the variant leaves that step out (c2g has no cast for a pattern-matching instanceof of a cut
;; class). The rest is jdk26u's code (baf63fb; Copyright (c) Oracle and/or its affiliates, GPL 2
;; with the Classpath Exception: LICENSE.md); the rest of the file Copyright (c) the Arbace
;; authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant ServerSocket
  (method ^:protected ^:final implAccept :throws [IOException] ^void [this ^Socket s]
    (let [^:mutable si (.impl s)]
      (if (nil? si)
          (do
            (set! si (.implAccept this))
            (try (.setConnectedImpl s si) (catch SocketException e (.closeQuietly si) (throw e))))
          (do
            (.ensureCompatible this si)
            (if (instance? PlatformSocketImpl impl)
                (let [psi (.platformImplAccept this)]
                  (.copyOptionsTo si psi)
                  (try
                    (.setConnectedImpl s psi)
                    (catch SocketException e (.closeQuietly psi) (throw e))))
                (do
                  (.setImpl s nil)
                  (try (.customImplAccept this si) (finally (.setImpl s si)))
                  (.setConnected s))))))))
