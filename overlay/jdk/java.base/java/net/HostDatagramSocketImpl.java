/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Datagrams"; F11 of doc/classes/JAVA-DEFERRED-TRIAGE.md):
 * jrt's platform DatagramSocketImpl, written for jrt in place of jdk26u's NIO adaptor
 * (sun.nio.ch.DatagramSocketAdaptor over DatagramChannelImpl, which needs java.nio's channels, the
 * poller and the VM's natives): DatagramSocket's createDelegate (a variant) wraps it in jdk26u's
 * NetMulticastSocket, the legacy delegate a DatagramSocketImplFactory's impl gets, over jrt's
 * socket table (jdk.internal.jrt.HostDatagram, HostNet). It is in java.net, as the JDK's legacy
 * PlainDatagramSocketImpl was, to fill a received DatagramPacket's length without changing its
 * buffer's (DatagramPacket.length, package-private) and to answer dataAvailable. The host has no
 * unbound sockets: the options set before bind are kept and applied by the bind, SO_REUSEADDR and
 * SO_REUSEPORT before the host's bind itself. Messages after jdk26u's DatagramChannelImpl and
 * DatagramSocketAdaptor (https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/
 * sun/nio/ch/: Copyright (c) Oracle and/or its affiliates, GNU General Public License version 2
 * with the Classpath Exception, LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse
 * Public License 1.0.
 */
package java.net;

import java.io.IOException;
import java.util.Set;
import jdk.internal.jrt.HostDatagram;
import jdk.internal.jrt.HostNet;

/** A UDP socket of the host. */
final class HostDatagramSocketImpl extends DatagramSocketImpl {

    private final Object stateLock = new Object();

    // the host's socket once bound; -1 before and after
    private volatile int handle = -1;
    private volatile boolean closed;

    // the options, kept for an unbound socket and for the ones the host does not report
    private boolean reuseAddr, reusePort;
    private int sndBuf, rcvBuf, tos = -1;
    private boolean broadcast;
    private int ttl = 1;
    private boolean loop = true;
    private int timeout;
    private InetAddress multicastIf;
    private NetworkInterface multicastIf2;

    HostDatagramSocketImpl() {
    }

    private int handle() throws SocketException {
        int h = handle;
        if (closed)
            throw new SocketException("Socket closed");
        if (h < 0)
            throw new SocketException("Socket not bound");
        return h;
    }

    private static void check(int kind, String[] error) throws IOException {
        if (kind < 0)
            throw HostDatagram.exception(kind, error[0], "Receive timed out");
    }

    private static void checkSocket(int kind, String[] error) throws SocketException {
        if (kind < 0) {
            IOException e = HostDatagram.exception(kind, error[0], "Receive timed out");
            throw e instanceof SocketException se ? se : new SocketException(e.getMessage());
        }
    }

    @Override
    protected void create() throws SocketException {
        // the host's socket is made by bind
    }

    @Override
    protected void bind(int lport, InetAddress laddr) throws SocketException {
        synchronized (stateLock) {
            if (closed)
                throw new SocketException("Socket closed");
            if (handle >= 0)
                throw new SocketException("Already bound");
            String host = laddr == null || laddr.isAnyLocalAddress() ? "" : laddr.getHostAddress();
            String[] error = new String[1];
            int h = HostDatagram.open0(host, lport, reuseAddr, reusePort, error);
            checkSocket(h, error);
            handle = h;
            localPort = HostNet.port0(h, true);
            // the options set before the bind
            if (sndBuf > 0) setInt(SocketOptions.SO_SNDBUF, sndBuf);
            if (rcvBuf > 0) setInt(SocketOptions.SO_RCVBUF, rcvBuf);
            if (tos >= 0) setInt(SocketOptions.IP_TOS, tos);
            if (broadcast) setInt(SocketOptions.SO_BROADCAST, 1);
            if (ttl != 1) setInt(HostDatagram.OPT_MULTICAST_TTL, ttl);
            if (!loop) setInt(HostDatagram.OPT_MULTICAST_LOOP, 0);
            int index = interfaceIndex();
            if (index > 0) setInt(HostDatagram.OPT_MULTICAST_IF, index);
        }
    }

    // the multicast interface's index (0: the kernel's choice)
    private int interfaceIndex() throws SocketException {
        if (multicastIf2 != null)
            return multicastIf2.getIndex();
        if (multicastIf != null && !multicastIf.isAnyLocalAddress()) {
            HostDatagram.Row r = HostDatagram.byAddress(multicastIf);
            if (r == null)
                throw new SocketException("No network interface with the address " + multicastIf.getHostAddress());
            return r.index;
        }
        return 0;
    }

    // sets option opt of the bound socket
    private void setInt(int opt, int value) throws SocketException {
        String[] error = new String[1];
        checkSocket(HostNet.setOption0(handle, opt, value, error), error);
    }

    private int getInt(int opt) throws SocketException {
        String[] error = new String[1];
        int[] v = new int[1];
        checkSocket(HostNet.getOption0(handle(), opt, v, error), error);
        return v[0];
    }

    @Override
    protected void send(DatagramPacket p) throws IOException {
        int h = handle();
        String host = null;
        int port = 0;
        if (connectedAddress == null) {
            host = p.getAddress().getHostAddress();
            port = p.getPort();
        }
        String[] error = new String[1];
        synchronized (p) {
            check(HostDatagram.send0(h, p.getData(), p.getOffset(), p.getLength(), host, port, error), error);
        }
    }

    private InetAddress connectedAddress;

    @Override
    protected void connect(InetAddress address, int port) throws SocketException {
        String[] error = new String[1];
        checkSocket(HostDatagram.connect0(handle(), address.getHostAddress(), port, error), error);
        connectedAddress = address;
    }

    @Override
    protected void disconnect() {
        if (handle >= 0 && !closed)
            HostDatagram.connect0(handle, null, 0, new String[1]);
        connectedAddress = null;
    }

    @Override
    protected int peek(InetAddress i) throws IOException {
        // the sender's address cannot be put into i (an InetAddress is immutable): its port alone
        DatagramPacket p = new DatagramPacket(new byte[1], 1);
        return peekData(p);
    }

    @Override
    protected int peekData(DatagramPacket p) throws IOException {
        receive(p, true);
        return p.getPort();
    }

    @Override
    protected void receive(DatagramPacket p) throws IOException {
        receive(p, false);
    }

    private void receive(DatagramPacket p, boolean peek) throws IOException {
        int h = handle();
        byte[][] from = new byte[1][];
        int[] port = new int[1];
        String[] error = new String[1];
        synchronized (p) {
            int n = HostDatagram.receive0(h, p.buf, p.offset, p.bufLength, timeout, peek, from, port, error);
            check(n, error);
            p.address = InetAddress.getByAddress(from[0]);
            p.port = port[0];
            p.length = n;
        }
    }

    @Override
    protected void setTimeToLive(int ttl) throws IOException {
        if (ttl < 0 || ttl > 255)
            throw new IllegalArgumentException("ttl out of range");
        synchronized (stateLock) {
            this.ttl = ttl;
            if (handle >= 0)
                setInt(HostDatagram.OPT_MULTICAST_TTL, ttl);
        }
    }

    @Override
    protected int getTimeToLive() throws IOException {
        synchronized (stateLock) {
            return handle >= 0 ? getInt(HostDatagram.OPT_MULTICAST_TTL) : ttl;
        }
    }

    private void join(InetAddress group, NetworkInterface netIf, boolean join) throws IOException {
        int h = handle();
        if (!group.isMulticastAddress())
            throw new SocketException("Not a multicast address");
        int index = netIf != null ? netIf.getIndex() : interfaceIndex();
        String[] error = new String[1];
        check(HostDatagram.join0(h, group.getAddress(), index, null, join, error), error);
    }

    @Override
    protected void join(InetAddress inetaddr) throws IOException {
        join(inetaddr, null, true);
    }

    @Override
    protected void leave(InetAddress inetaddr) throws IOException {
        join(inetaddr, null, false);
    }

    @Override
    protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        if (!(mcastaddr instanceof InetSocketAddress isa))
            throw new IllegalArgumentException("Unsupported address type");
        join(isa.getAddress(), netIf, true);
    }

    @Override
    protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        if (!(mcastaddr instanceof InetSocketAddress isa))
            throw new IllegalArgumentException("Unsupported address type");
        join(isa.getAddress(), netIf, false);
    }

    @Override
    protected void close() {
        synchronized (stateLock) {
            if (closed)
                return;
            closed = true;
            int h = handle;
            handle = -1;
            if (h >= 0)
                HostNet.close0(h);
        }
    }

    @Override
    int dataAvailable() {
        int h = handle;
        if (closed)
            return -1;
        return h < 0 ? 0 : HostNet.available0(h);
    }

    // -- the legacy options (java.net.SocketOptions), as NetMulticastSocket asks them

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        synchronized (stateLock) {
            if (closed)
                throw new SocketException("Socket Closed");
            boolean bound = handle >= 0;
            switch (optID) {
                case SO_TIMEOUT -> timeout = (Integer) value;
                case SO_REUSEADDR -> {
                    reuseAddr = (Boolean) value;
                    if (bound) setInt(SO_REUSEADDR, reuseAddr ? 1 : 0);
                }
                case SO_REUSEPORT -> {
                    reusePort = (Boolean) value;
                    if (bound) setInt(SO_REUSEPORT, reusePort ? 1 : 0);
                }
                case SO_BROADCAST -> {
                    broadcast = (Boolean) value;
                    if (bound) setInt(SO_BROADCAST, broadcast ? 1 : 0);
                }
                case SO_SNDBUF -> {
                    sndBuf = (Integer) value;
                    if (bound) setInt(SO_SNDBUF, sndBuf);
                }
                case SO_RCVBUF -> {
                    rcvBuf = (Integer) value;
                    if (bound) setInt(SO_RCVBUF, rcvBuf);
                }
                case IP_TOS -> {
                    tos = (Integer) value;
                    if (bound) setInt(IP_TOS, tos);
                }
                // the legacy option disables the loopback (MulticastSocket.setLoopbackMode)
                case IP_MULTICAST_LOOP -> {
                    loop = !(Boolean) value;
                    if (bound) setInt(HostDatagram.OPT_MULTICAST_LOOP, loop ? 1 : 0);
                }
                case IP_MULTICAST_IF -> {
                    multicastIf = (InetAddress) value;
                    multicastIf2 = null;
                    if (bound) setInt(HostDatagram.OPT_MULTICAST_IF, interfaceIndex());
                }
                case IP_MULTICAST_IF2 -> {
                    multicastIf2 = (NetworkInterface) value;
                    multicastIf = null;
                    if (bound) setInt(HostDatagram.OPT_MULTICAST_IF, interfaceIndex());
                }
                default -> throw new SocketException("Invalid option: " + optID);
            }
        }
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        synchronized (stateLock) {
            if (closed)
                throw new SocketException("Socket Closed");
            boolean bound = handle >= 0;
            return switch (optID) {
                case SO_TIMEOUT -> timeout;
                case SO_BINDADDR -> bound ? boundAddress() : InetAddress.anyLocalAddress();
                case SO_REUSEADDR -> bound ? getInt(SO_REUSEADDR) != 0 : reuseAddr;
                case SO_REUSEPORT -> bound ? getInt(SO_REUSEPORT) != 0 : reusePort;
                case SO_BROADCAST -> bound ? getInt(SO_BROADCAST) != 0 : broadcast;
                case SO_SNDBUF -> bound ? getInt(SO_SNDBUF) : sndBuf;
                case SO_RCVBUF -> bound ? getInt(SO_RCVBUF) : rcvBuf;
                case IP_TOS -> bound ? getInt(IP_TOS) : Math.max(tos, 0);
                case IP_MULTICAST_LOOP -> !(bound ? getInt(HostDatagram.OPT_MULTICAST_LOOP) != 0 : loop);
                case IP_MULTICAST_IF -> multicastIf != null ? multicastIf : InetAddress.anyLocalAddress();
                case IP_MULTICAST_IF2 -> multicastIf2;
                default -> throw new SocketException("Invalid option: " + optID);
            };
        }
    }

    private InetAddress boundAddress() throws SocketException {
        try {
            return InetAddress.getByAddress(HostNet.address0(handle, true));
        } catch (UnknownHostException e) {
            throw new SocketException(e.getMessage());
        }
    }

    // -- the options of java.net.StandardSocketOptions (DatagramSocket.setOption, getOption)

    private static final Set<SocketOption<?>> OPTIONS = Set.of(
            StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF,
            StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.SO_REUSEPORT,
            StandardSocketOptions.SO_BROADCAST, StandardSocketOptions.IP_TOS,
            StandardSocketOptions.IP_MULTICAST_IF, StandardSocketOptions.IP_MULTICAST_TTL,
            StandardSocketOptions.IP_MULTICAST_LOOP);

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        return OPTIONS;
    }

    @Override
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        if (!OPTIONS.contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");
        if (!name.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");
        if (name == StandardSocketOptions.SO_SNDBUF) setOption(SO_SNDBUF, value);
        else if (name == StandardSocketOptions.SO_RCVBUF) setOption(SO_RCVBUF, value);
        else if (name == StandardSocketOptions.SO_REUSEADDR) setOption(SO_REUSEADDR, value);
        else if (name == StandardSocketOptions.SO_REUSEPORT) setOption(SO_REUSEPORT, value);
        else if (name == StandardSocketOptions.SO_BROADCAST) setOption(SO_BROADCAST, value);
        else if (name == StandardSocketOptions.IP_TOS) setOption(IP_TOS, value);
        else if (name == StandardSocketOptions.IP_MULTICAST_IF) setOption(IP_MULTICAST_IF2, value);
        else if (name == StandardSocketOptions.IP_MULTICAST_TTL) setTimeToLive((Integer) value);
        else setOption(IP_MULTICAST_LOOP, !(Boolean) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        if (!OPTIONS.contains(name))
            throw new UnsupportedOperationException("'" + name + "' not supported");
        Object v;
        if (name == StandardSocketOptions.SO_SNDBUF) v = getOption(SO_SNDBUF);
        else if (name == StandardSocketOptions.SO_RCVBUF) v = getOption(SO_RCVBUF);
        else if (name == StandardSocketOptions.SO_REUSEADDR) v = getOption(SO_REUSEADDR);
        else if (name == StandardSocketOptions.SO_REUSEPORT) v = getOption(SO_REUSEPORT);
        else if (name == StandardSocketOptions.SO_BROADCAST) v = getOption(SO_BROADCAST);
        else if (name == StandardSocketOptions.IP_TOS) v = getOption(IP_TOS);
        else if (name == StandardSocketOptions.IP_MULTICAST_IF) v = getOption(IP_MULTICAST_IF2);
        else if (name == StandardSocketOptions.IP_MULTICAST_TTL) v = getTimeToLive();
        else v = !(Boolean) getOption(IP_MULTICAST_LOOP);
        return (T) v;
    }
}
