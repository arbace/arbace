/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Sockets"): jrt's socket table and name service,
 * under HostSocketImpl and the variants of java.net's InetAddress and Inet6AddressImpl
 * (overlay/jdk/variants). A handle is a number naming a listening socket or a connection of the
 * host (jrt's NetHost: Go's net package in B1a); the natives are jrt's Go
 * (go/arbace/jrt/net.clj). A native reports an error as a negative kind, its text in error[0];
 * exception(kind, text) is the exception the JVM throws for it.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** The host's sockets by number, and its name service. */
public final class HostNet {

    /** The end of the stream (read0). */
    public static final int EOF = -1;
    /** Error kinds, negative: a SocketException with the host's text. */
    public static final int ERROR = -2;
    /** BindException (address in use, not available, permission). */
    public static final int BIND = -3;
    /** ConnectException (connection refused). */
    public static final int REFUSED = -4;
    /** NoRouteToHostException. */
    public static final int NO_ROUTE = -5;
    /** A timeout: SocketTimeoutException with the caller's text. */
    public static final int TIMEOUT = -6;
    /** UnknownHostException. */
    public static final int UNKNOWN_HOST = -7;
    /** The socket was closed (by another thread): SocketException("Socket closed"). */
    public static final int CLOSED = -8;
    /** The peer reset the connection: SocketException("Connection reset"). */
    public static final int RESET = -9;
    /** The host has no network (jrt's host lacks NetHost). */
    public static final int UNSUPPORTED = -10;

    /** shutdown0's how. */
    public static final int SHUT_RD = 0;
    public static final int SHUT_WR = 1;

    private HostNet() {
    }

    /** The exception of error kind (< -1) with the host's text; timeout is a timeout's text. */
    public static java.io.IOException exception(int kind, String text, String timeout) {
        switch (kind) {
            case BIND: return new BindException(text);
            case REFUSED: return new ConnectException(text);
            case NO_ROUTE: return new NoRouteToHostException(text);
            case TIMEOUT: return new SocketTimeoutException(timeout);
            case UNKNOWN_HOST: return new UnknownHostException(text);
            case CLOSED: return new SocketException("Socket closed");
            case RESET: return new SocketException("Connection reset");
            default: return new SocketException(text);
        }
    }

    /**
     * Inet6AddressImpl.lookupAllHostAddr (and Inet4AddressImpl's): the addresses of host, each
     * named host, as the JDK's getaddrinfo native makes them.
     */
    public static InetAddress[] lookupAllHostAddr(String host, int characteristics)
            throws UnknownHostException {
        String[] error = new String[1];
        byte[][] addrs = lookup0(host, characteristics, error);
        if (addrs == null)
            throw new UnknownHostException(error[0]);
        InetAddress[] result = new InetAddress[addrs.length];
        for (int i = 0; i < addrs.length; i++)
            result[i] = InetAddress.getByAddress(host, addrs[i]);
        return result;
    }

    /** InetAddressImpl.getHostByAddr: the host name of addr (no message when it has none). */
    public static String getHostByAddr(byte[] addr) throws UnknownHostException {
        String[] error = new String[1];
        String name = reverse0(addr, error);
        if (name == null)
            throw new UnknownHostException();
        return name;
    }

    /** InetAddressImpl.getLocalHostName: the host's name, "localhost" when it has none. */
    public static String getLocalHostName() throws UnknownHostException {
        String[] error = new String[1];
        String name = hostName0(error);
        return name == null || name.isEmpty() ? "localhost" : name;
    }

    /** Listens at host (an IP literal, "" for any address) and port (0: any): a handle. */
    public static native int listen0(String host, int port, String[] error);

    /** Accepts a connection of listener h, waiting up to timeout ms (0: no limit): a handle. */
    public static native int accept0(int h, int timeout, String[] error);

    /**
     * Connects to host (an IP literal) and port, from localHost and localPort when localHost is
     * not null, waiting up to timeout ms (0: no limit): a handle.
     */
    public static native int connect0(String host, int port, String localHost, int localPort,
                                      int timeout, String[] error);

    /** Reads up to len (> 0) bytes of connection h into b at off, waiting up to timeout ms. */
    public static native int read0(int h, byte[] b, int off, int len, int timeout, String[] error);

    /** Writes len bytes of b from off to connection h: 0, or an error kind. */
    public static native int write0(int h, byte[] b, int off, int len, String[] error);

    /** The bytes connection h can give without blocking (0 when the host cannot tell). */
    public static native int available0(int h);

    /** Closes handle h (a listener or a connection); a blocked operation on it ends CLOSED. */
    public static native void close0(int h);

    /** Shuts down connection h for reading (SHUT_RD) or writing (SHUT_WR): 0 or an error kind. */
    public static native int shutdown0(int h, int how, String[] error);

    /** The local (or the remote) address of handle h, 4 or 16 bytes. */
    public static native byte[] address0(int h, boolean local);

    /** The local (or the remote) port of handle h. */
    public static native int port0(int h, boolean local);

    /** Sets option opt (java.net.SocketOptions' number) of handle h: 0 or an error kind. */
    public static native int setOption0(int h, int opt, int value, String[] error);

    /** Option opt of handle h into value[0]: 0 or an error kind. */
    public static native int getOption0(int h, int opt, int[] value, String[] error);

    /**
     * The addresses of host name (4 or 16 bytes each), as LookupPolicy's characteristics
     * select and order them, or null with the reason in error[0].
     */
    public static native byte[][] lookup0(String host, int characteristics, String[] error);

    /** The host name of address addr, or null with the reason in error[0]. */
    public static native String reverse0(byte[] addr, String[] error);

    /** The local host's name, or null with the reason in error[0]. */
    public static native String hostName0(String[] error);

    /** Whether the host has IPv4 (or IPv6) addresses. */
    public static native boolean hasFamily0(boolean ipv6);
}
