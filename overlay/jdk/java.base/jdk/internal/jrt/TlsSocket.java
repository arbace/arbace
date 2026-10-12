/*
 * Arbace (overlay/jdk, doc/go/BOX-NOTES.md "TLS (option 2)"): a client socket of jrt's whose
 * connection, once startTls has made the handshake, carries TLS through Go's crypto/tls
 * (go/arbace/jrt/tls.ae), in the box and natively: its streams are the plain text. It is a
 * java.net.Socket over jrt's HostSocketImpl, connected as any socket is (connect, under the
 * box's network policy), then upgraded in place; what javax.net.ssl would offer (SSLSocket,
 * SSLSession) is not in the Go build. Arbace's namespace arbace.tls is its user.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/** A socket whose connection carries TLS once startTls succeeded. */
public final class TlsSocket extends Socket {

    private final HostSocketImpl hostImpl;

    /** An unconnected socket (connect it, then startTls). */
    public TlsSocket() throws SocketException {
        this(new HostSocketImpl(false));
    }

    private TlsSocket(HostSocketImpl impl) throws SocketException {
        super(impl);
        this.hostImpl = impl;
    }

    /**
     * The TLS handshake as a client on the connected socket: serverName sent (SNI) and the
     * peer's certificate verified for it, alpn the application protocols offered (separated by
     * commas; null or "" for none), roots the trusted certificates as PEM (null: jrt's own,
     * Mozilla's), waiting up to timeout ms (0: no limit). 0, or HostNet's error kind with the
     * reason in error[0]: HostNet.TLS_VERIFY, TLS, TLS_ROOTS, or the network's (HostNet.exception).
     */
    public int startTls(String serverName, String alpn, byte[] roots, int timeout, String[] error)
            throws IOException {
        if (!isConnected())
            throw new SocketException("Socket is not connected");
        if (isClosed())
            throw new SocketException("Socket is closed");
        return hostImpl.startTls(serverName, alpn, roots, timeout, error);
    }

    /**
     * The session: the protocol (TLSv1.3), the cipher suite, the application protocol ("" for
     * none) and the server name; null before the handshake.
     */
    public String[] session() throws IOException {
        String[] info = new String[4];
        return hostImpl.tlsInfo(info) == 0 ? info : null;
    }

    /** The peer's certificates, DER, the peer's own first (null before the handshake). */
    public byte[][] peerCertificates() throws IOException {
        return hostImpl.tlsPeer();
    }
}
