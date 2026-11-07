/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Sockets", "Datagrams"; F11 of
 * doc/classes/JAVA-DEFERRED-TRIAGE.md): jrt's datagram sockets and the host's network interfaces,
 * under java.net's HostDatagramSocketImpl (overlay/jdk) and the variants of java.net's
 * DatagramSocket, NetworkInterface and InterfaceAddress (overlay/jdk/variants). A datagram socket
 * is a handle of HostNet's table (a UDP socket of the host: jrt's UDPHost, Go's net.UDPConn in
 * B1a), so that HostNet's close0, available0, address0, port0, setOption0 and getOption0 serve it
 * as they serve a connection; the natives here are what UDP adds. A native reports an error as a
 * negative kind (HostNet's, and PORT_UNREACHABLE), its text in error[0]. The natives are jrt's Go
 * (go/arbace/jrt/udp.ae).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;

/** The host's datagram sockets (in HostNet's table) and network interfaces. */
public final class HostDatagram {

    /** The peer's port is unreachable (an ICMP message after a send on a connected socket). */
    public static final int PORT_UNREACHABLE = -14;

    /**
     * jrt's option numbers beside java.net.SocketOptions' (HostNet.setOption0, getOption0): the
     * multicast time to live, multicast loopback (enabled: 1), the multicast interface (by
     * index; 0: the kernel's choice).
     */
    public static final int OPT_MULTICAST_TTL = 0x2001;
    public static final int OPT_MULTICAST_LOOP = 0x2002;
    public static final int OPT_MULTICAST_IF = 0x2003;

    /** An interface's flags (Go's net.Flags). */
    public static final int UP = 1;
    public static final int BROADCAST = 2;
    public static final int LOOPBACK = 4;
    public static final int POINT_TO_POINT = 8;
    public static final int MULTICAST = 16;

    private HostDatagram() {
    }

    /** The exception of error kind (< -1) with the host's text; timeout is a timeout's text. */
    public static java.io.IOException exception(int kind, String text, String timeout) {
        if (kind == PORT_UNREACHABLE)
            return new PortUnreachableException(text);
        return HostNet.exception(kind, text, timeout);
    }

    /**
     * Opens a UDP socket bound to host (an IP literal, "" for every address of both families)
     * and port (0: any), with SO_REUSEADDR (and SO_REUSEPORT) set before the bind as asked: a
     * handle of HostNet's table, or an error kind.
     */
    public static native int open0(String host, int port, boolean reuseAddr, boolean reusePort,
                                   String[] error);

    /**
     * Sends len bytes of b from off on socket h to host (an IP literal) and port, or to the
     * connected peer when host is null: 0, or an error kind.
     */
    public static native int send0(int h, byte[] b, int off, int len, String host, int port,
                                   String[] error);

    /**
     * Receives a datagram on socket h into b at off (at most len bytes; the rest of a longer one
     * is discarded), waiting up to timeout ms (0: no limit), leaving it queued when peek: the
     * datagram's length (at most len), its sender's address in from[0] (4 or 16 bytes) and port in
     * port[0]; or an error kind (TIMEOUT, CLOSED, PORT_UNREACHABLE, ...).
     */
    public static native int receive0(int h, byte[] b, int off, int len, int timeout, boolean peek,
                                      byte[][] from, int[] port, String[] error);

    /**
     * Connects socket h to host (an IP literal) and port (the kernel then filters what it
     * receives and reports an unreachable port), or dissolves the association when host is null:
     * 0, or an error kind.
     */
    public static native int connect0(int h, String host, int port, String[] error);

    /**
     * Joins (or leaves) multicast group (4 or 16 bytes) on socket h at the interface of index
     * ifindex (0: the kernel's choice), from source (null: any source): 0, or an error kind.
     */
    public static native int join0(int h, byte[] group, int ifindex, byte[] source, boolean join,
                                   String[] error);

    /**
     * The host's network interfaces, each an Object[] of: its name (String), int[] {index, flags,
     * mtu}, its hardware address (byte[], null when it has none), its addresses (byte[][], 4 or
     * 16 bytes each), their prefix lengths (int[]) and their broadcast addresses (byte[][], null
     * entries for none); or null with the reason in error[0].
     */
    public static native Object[] interfaces0(String[] error);

    // -- the interfaces as java.net's NetworkInterface variant reads them

    /** One interface of the host. */
    public static final class Row {
        public final String name;
        public final int index;
        public final int flags;
        public final int mtu;
        public final byte[] mac;
        public final InetAddress[] addrs;
        public final short[] prefixes;
        public final Inet4Address[] broadcasts;

        Row(Object[] r) throws UnknownHostException {
            name = (String) r[0];
            int[] n = (int[]) r[1];
            index = n[0];
            flags = n[1];
            mtu = n[2];
            mac = (byte[]) r[2];
            byte[][] a = (byte[][]) r[3];
            int[] p = (int[]) r[4];
            byte[][] b = (byte[][]) r[5];
            addrs = new InetAddress[a.length];
            prefixes = new short[a.length];
            broadcasts = new Inet4Address[a.length];
            for (int i = 0; i < a.length; i++) {
                // an IPv6 address carries the interface's index as its scope (the JDK's: its
                // interface, which Inet6Address's variant does not keep)
                addrs[i] = a[i].length == 16 && (a[i][0] & 0xff) == 0xfe && (a[i][1] & 0xc0) == 0x80
                        ? Inet6Address.getByAddress(null, a[i], index)
                        : InetAddress.getByAddress(a[i]);
                prefixes[i] = (short) p[i];
                if (b != null && b[i] != null)
                    broadcasts[i] = (Inet4Address) InetAddress.getByAddress(b[i]);
            }
        }
    }

    /** The host's interfaces (NetworkInterface.getAll's natives). */
    public static Row[] rows() throws SocketException {
        String[] error = new String[1];
        Object[] all = interfaces0(error);
        if (all == null)
            throw new SocketException(error[0]);
        Row[] rows = new Row[all.length];
        try {
            for (int i = 0; i < all.length; i++)
                rows[i] = new Row((Object[]) all[i]);
        } catch (UnknownHostException e) {
            throw new SocketException(e.getMessage());
        }
        return rows;
    }

    /** The interface named name, or null. */
    public static Row byName(String name) throws SocketException {
        for (Row r : rows())
            if (r.name.equals(name))
                return r;
        return null;
    }

    /** The interface of index index, or null. */
    public static Row byIndex(int index) throws SocketException {
        for (Row r : rows())
            if (r.index == index)
                return r;
        return null;
    }

    /** The interface address addr is bound to, or null (the scope is not compared). */
    public static Row byAddress(InetAddress addr) throws SocketException {
        byte[] want = addr.getAddress();
        for (Row r : rows())
            for (InetAddress a : r.addrs)
                if (java.util.Arrays.equals(a.getAddress(), want))
                    return r;
        return null;
    }

    /** The interface of name name (else of index index), or SocketException as the JDK's natives. */
    public static Row of(String name, int index) throws SocketException {
        Row r = name != null ? byName(name) : byIndex(index);
        if (r == null)
            throw new SocketException("No such device");
        return r;
    }
}
