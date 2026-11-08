/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10b"): jrt's
 * sun.nio.ch.NativeSocketAddress. jdk26u's (baf63fb) is a struct sockaddr (the union of
 * sockaddr_in and sockaddr_in6) in native memory, which DatagramChannelImpl's natives receive0 and
 * send0 take by address; jrt has no native memory, so the union is a byte array of the same size
 * and layout (Linux's: the family a native-order short at 0, the port in network order at 2,
 * sin_addr at 4, sin6_flowinfo at 4, sin6_addr at 8, sin6_scope_id at 24; the natives' constants
 * below are the values NativeSocketAddress.c's sizeof and offsetof give on linux-amd64 and
 * linux-arm64, both little-endian), which jrt's natives (jdk.internal.jrt.HostChannels.receive0,
 * send0) give the kernel as recvfrom's and sendto's sockaddr, and which bytes() returns (the
 * Go build's DatagramChannelImpl variant passes it in place of address()). Every other method is
 * jdk26u's, each Unsafe access a read or write of the array at the same offset.
 * jdk26u's code: Copyright (c) 2019, Oracle and/or its affiliates, GPL 2 with the Classpath
 * Exception (LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Arrays;

/**
 * A native socket address that is the union of struct sockaddr, struct sockaddr_in,
 * and struct sockaddr_in6.
 *
 * This class is not thread safe.
 */
class NativeSocketAddress {
    private static final int AF_INET  = AFINET();
    private static final int AF_INET6 = AFINET6();

    private static final int SIZEOF_SOCKADDR4     = sizeofSockAddr4();
    private static final int SIZEOF_SOCKADDR6     = sizeofSockAddr6();
    private static final int SIZEOF_SOCKETADDRESS = Math.max(SIZEOF_SOCKADDR4, SIZEOF_SOCKADDR6);
    private static final int SIZEOF_FAMILY        = sizeofFamily();
    private static final int OFFSET_FAMILY        = offsetFamily();
    private static final int OFFSET_SIN4_PORT     = offsetSin4Port();
    private static final int OFFSET_SIN4_ADDR     = offsetSin4Addr();
    private static final int OFFSET_SIN6_PORT     = offsetSin6Port();
    private static final int OFFSET_SIN6_ADDR     = offsetSin6Addr();
    private static final int OFFSET_SIN6_SCOPE_ID = offsetSin6ScopeId();
    private static final int OFFSET_SIN6_FLOWINFO = offsetSin6FlowInfo();

    // SOCKETADDRESS, as bytes (jdk26u's: its native address)
    private final byte[] memory;

    /**
     * jdk26u's native address of the sockaddr: jrt has none (the Go build's DatagramChannelImpl
     * passes bytes() to its natives).
     */
    long address() {
        throw new UnsupportedOperationException("native memory");
    }

    /** The sockaddr's bytes, given to recvfrom and sendto by jrt's natives (jrt's own). */
    byte[] bytes() {
        return memory;
    }

    NativeSocketAddress() {
        this.memory = new byte[SIZEOF_SOCKETADDRESS];
    }

    static NativeSocketAddress[] allocate(int count) {
        NativeSocketAddress[] array = new NativeSocketAddress[count];
        for (int i = 0; i < count; i++) {
            try {
                array[i] = new NativeSocketAddress();
            } catch (OutOfMemoryError e) {
                freeAll(array);
                throw e;
            }
        }
        return array;
    }

    static void freeAll(NativeSocketAddress[] array) {
        // the arrays are the collector's
    }

    /**
     * Encodes the given InetSocketAddress into this socket address.
     * @param protocolFamily protocol family
     * @param isa the InetSocketAddress to encode
     * @return the size of the socket address (sizeof sockaddr or sockaddr6)
     * @throws UnsupportedAddressTypeException if the address type is not supported
     */
    int encode(ProtocolFamily protocolFamily, InetSocketAddress isa) {
        if (protocolFamily == StandardProtocolFamily.INET) {
            // struct sockaddr
            InetAddress ia = isa.getAddress();
            if (!(ia instanceof Inet4Address))
                throw new UnsupportedAddressTypeException();
            putFamily(AF_INET);
            putAddress(AF_INET, ia);
            putPort(AF_INET, isa.getPort());
            return SIZEOF_SOCKADDR4;
        } else {
            // struct sockaddr6
            putFamily(AF_INET6);
            putAddress(AF_INET6, isa.getAddress());
            putPort(AF_INET6, isa.getPort());
            putInt(OFFSET_SIN6_FLOWINFO, 0);
            return SIZEOF_SOCKADDR6;
        }
    }

    /**
     * Return an InetSocketAddress to represent the socket address in this buffer.
     * @throws SocketException if the socket address is not AF_INET or AF_INET6
     */
    InetSocketAddress decode() throws SocketException {
        int family = family();
        if (family != AF_INET && family != AF_INET6)
            throw new SocketException("Socket family not recognized");
        return new InetSocketAddress(address(family), port(family));
    }

    /**
     * Find a mismatch between this and another socket address
     * @return the byte offset of the first mismatch or -1 if no mismatch
     */
    private int mismatch(NativeSocketAddress other) {
        return Arrays.mismatch(this.memory, other.memory);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NativeSocketAddress) {
            return mismatch((NativeSocketAddress) other) < 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int offset = 0; offset < SIZEOF_SOCKETADDRESS; offset++) {
            h = 31 * h + memory[offset];
        }
        return h;
    }

    @Override
    public String toString() {
        int family = family();
        if (family == AF_INET || family == AF_INET6) {
            return ((family == AF_INET) ? "AF_INET" : "AF_INET6")
                    + ", address=" + address(family) + ", port=" + port(family);
        } else {
            return "<unknown>";
        }
    }

    /**
     * Return the value of the sa_family field.
     */
    private int family() {
        if (SIZEOF_FAMILY == 1) {
            return memory[OFFSET_FAMILY];
        } else if (SIZEOF_FAMILY == 2) {
            return getShort(OFFSET_FAMILY);
        } else {
            throw new InternalError();
        }
    }

    /**
     * Stores the given family in the sa_family field.
     */
    private void putFamily(int family) {
        if (SIZEOF_FAMILY == 1) {
            memory[OFFSET_FAMILY] = (byte) family;
        } else if (SIZEOF_FAMILY == 2) {
            putShort(OFFSET_FAMILY, (short) family);
        } else {
            throw new InternalError();
        }
    }

    /**
     * Return the value of the sin_port or sin6_port field. These fields are
     * stored in network order.
     */
    private int port(int family) {
        byte b1, b2;
        if (family == AF_INET) {
            b1 = memory[OFFSET_SIN4_PORT];
            b2 = memory[OFFSET_SIN4_PORT + 1];
        } else {
            b1 = memory[OFFSET_SIN6_PORT];
            b2 = memory[OFFSET_SIN6_PORT + 1];
        }
        return (Byte.toUnsignedInt(b1) << 8) + Byte.toUnsignedInt(b2);
    }

    /**
     * Stores the given port number in the sin_port or sin6_port field. The
     * port is stored in network order.
     */
    private void putPort(int family, int port) {
        byte b1 = (byte) ((port >> 8) & 0xff);
        byte b2 = (byte) ((port >> 0) & 0xff);
        if (family == AF_INET) {
            memory[OFFSET_SIN4_PORT] = b1;
            memory[OFFSET_SIN4_PORT + 1] = b2;
        } else {
            memory[OFFSET_SIN6_PORT] = b1;
            memory[OFFSET_SIN6_PORT + 1] = b2;
        }
    }

    /**
     * Return an InetAddress to represent the value of the address in the
     * sin4_addr or sin6_addr fields. For IPv6 addresses, the Inet6Address is
     * created with the sin6_scope_id in the sockaddr_in6 structure.
     */
    private InetAddress address(int family) {
        int len;
        int offset;
        int scope_id;
        if (family == AF_INET) {
            len = 4;
            offset = OFFSET_SIN4_ADDR;
            scope_id = 0;
        } else {
            len = 16;
            offset = OFFSET_SIN6_ADDR;
            scope_id = getInt(OFFSET_SIN6_SCOPE_ID);
        }
        byte[] bytes = new byte[len];
        System.arraycopy(memory, offset, bytes, 0, len);
        try {
            if (scope_id == 0) {
                return InetAddress.getByAddress(bytes);
            } else {
                return Inet6Address.getByAddress(null, bytes, scope_id);
            }
        } catch (UnknownHostException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Stores the given InetAddress in the sin_addr or sin6_addr/sin6_scope_id
     * fields. For IPv6 addresses, the sin6_addr will be popluated with an
     * IPv4-mapped IPv6 address when the given InetAddress is an IPv4 address.
     */
    private void putAddress(int family, InetAddress ia) {
        if (family == AF_INET) {
            // IPv4 address
            putAddress(OFFSET_SIN4_ADDR, (Inet4Address) ia);
        } else {
            int scope_id;
            if (ia instanceof Inet4Address) {
                // IPv4-mapped IPv6 address
                Arrays.fill(memory, OFFSET_SIN6_ADDR, OFFSET_SIN6_ADDR + 10, (byte) 0);
                memory[OFFSET_SIN6_ADDR + 10] = (byte) 0xff;
                memory[OFFSET_SIN6_ADDR + 11] = (byte) 0xff;
                putAddress(OFFSET_SIN6_ADDR + 12, (Inet4Address) ia);
                scope_id = 0;
            } else {
                // IPv6 address
                var inet6Address = (Inet6Address) ia;
                putAddress(OFFSET_SIN6_ADDR, inet6Address);
                scope_id = inet6Address.getScopeId();
            }
            putInt(OFFSET_SIN6_SCOPE_ID, scope_id);
        }
    }

    // the address's bytes in network order (jdk26u's JavaNetInetAddressAccess.addressValue, as
    // the public getAddress gives them)
    private void putAddress(int offset, Inet4Address ia) {
        byte[] bytes = ia.getAddress();
        System.arraycopy(bytes, 0, memory, offset, 4);
    }

    private void putAddress(int offset, Inet6Address ia) {
        byte[] bytes = ia.getAddress();
        System.arraycopy(bytes, 0, memory, offset, 16);
    }

    // -- the array in the native byte order (little-endian on linux-amd64 and linux-arm64) --

    private int getShort(int offset) {
        return (short) ((memory[offset] & 0xff) | ((memory[offset + 1] & 0xff) << 8));
    }

    private void putShort(int offset, short value) {
        memory[offset] = (byte) value;
        memory[offset + 1] = (byte) (value >> 8);
    }

    private int getInt(int offset) {
        return (memory[offset] & 0xff) | ((memory[offset + 1] & 0xff) << 8)
                | ((memory[offset + 2] & 0xff) << 16) | ((memory[offset + 3] & 0xff) << 24);
    }

    private void putInt(int offset, int value) {
        memory[offset] = (byte) value;
        memory[offset + 1] = (byte) (value >> 8);
        memory[offset + 2] = (byte) (value >> 16);
        memory[offset + 3] = (byte) (value >> 24);
    }

    // -- the natives' constants: NativeSocketAddress.c's on Linux (sys/socket.h, netinet/in.h) --

    private static int AFINET() { return 2; }
    private static int AFINET6() { return 10; }
    private static int sizeofSockAddr4() { return 16; }
    private static int sizeofSockAddr6() { return 28; }
    private static int sizeofFamily() { return 2; }
    private static int offsetFamily() { return 0; }
    private static int offsetSin4Port() { return 2; }
    private static int offsetSin4Addr() { return 4; }
    private static int offsetSin6Port() { return 2; }
    private static int offsetSin6Addr() { return 8; }
    private static int offsetSin6ScopeId() { return 24; }
    private static int offsetSin6FlowInfo() { return 4; }
}
