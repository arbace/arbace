/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Sockets"): jrt's platform SocketImpl, written for
 * jrt in place of jdk26u's sun.nio.ch.NioSocketImpl (which needs java.nio's channels, the
 * poller and the VM's natives), after its documented behaviour and its messages, over jrt's
 * socket table (HostNet). SocketImpl.createPlatformSocketImpl returns it (a variant). A server
 * socket listens when it is bound (the host has no unbound sockets; ServerSocket.bind always
 * binds then listens); a client socket's bind is recorded and taken by the connect.
 * Its state checks, messages, streams and option handling are transcribed from jdk26u's
 * NioSocketImpl (https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/
 * sun/nio/ch/NioSocketImpl.java): Copyright (c) 2019, 2025, Oracle and/or its affiliates, GNU
 * General Public License version 2 with the Classpath Exception (LICENSE.md); the rest
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import sun.net.PlatformSocketImpl;

/** A TCP socket of the host: a listener (server) or a connection. */
public final class HostSocketImpl extends SocketImpl implements PlatformSocketImpl {

    // The maximum number of bytes to read or write per call to the host
    private static final int MAX_BUFFER_SIZE = 128 * 1024;

    private final boolean server;

    // held when reading, accepting or connecting; when writing
    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock writeLock = new ReentrantLock();

    private final Object stateLock = new Object();
    private static final int ST_NEW = 0;
    private static final int ST_UNCONNECTED = 1;
    private static final int ST_CONNECTING = 2;
    private static final int ST_CONNECTED = 3;
    private static final int ST_CLOSED = 5;
    private volatile int state;

    // the host's socket: a listener once bound (server), a connection once connected
    private volatile int handle = -1;

    // a client socket's local address and port, given by bind before connect
    private InetAddress bindAddress;
    private int bindPort;

    // options set before the host's socket exists, applied when it does
    private final HashMap<Integer, Integer> pending = new HashMap<>();

    // read or accept timeout in millis
    private volatile int timeout;

    private volatile boolean isInputClosed;
    private volatile boolean isOutputClosed;

    // used by read, protected by readLock
    private boolean readEOF;
    private boolean connectionReset;

    /** A SocketImpl for a ServerSocket (server) or a Socket. */
    public HostSocketImpl(boolean server) {
        this.server = server;
    }

    private boolean isOpen() {
        return state < ST_CLOSED;
    }

    private void ensureOpen() throws SocketException {
        int state = this.state;
        if (state == ST_NEW)
            throw new SocketException("Socket not created");
        if (state >= ST_CLOSED)
            throw new SocketException("Socket closed");
    }

    private void ensureOpenAndConnected() throws SocketException {
        int state = this.state;
        if (state < ST_CONNECTED)
            throw new SocketException("Not connected");
        if (state > ST_CONNECTED)
            throw new SocketException("Socket closed");
    }

    /** The exception of a host error; a closed socket's after close is "Socket closed". */
    private IOException failure(int kind, String text, String timeoutText) {
        if (!isOpen())
            return new SocketException("Socket closed");
        return HostNet.exception(kind, text, timeoutText);
    }

    /** An address's literal for the host ("" for none). */
    private static String literal(InetAddress a) {
        return a == null ? "" : a.getHostAddress();
    }

    private static InetAddress inetAddress(byte[] b) throws IOException {
        try {
            return InetAddress.getByAddress(b);
        } catch (UnknownHostException e) {
            throw new SocketException(e.getMessage());
        }
    }

    // -- reading and writing

    private int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0)
            return 0;
        readLock.lock();
        try {
            // emulate legacy behavior to return -1, even if socket is closed
            if (readEOF)
                return -1;
            int h;
            synchronized (stateLock) {
                ensureOpenAndConnected();
                h = handle;
            }
            if (connectionReset)
                throw new SocketException("Connection reset");
            if (isInputClosed)
                return -1;
            String[] error = new String[1];
            int n = HostNet.read0(h, b, off, Math.min(len, MAX_BUFFER_SIZE), timeout, error);
            if (n == HostNet.EOF) {
                readEOF = true;
                return -1;
            }
            if (n < 0) {
                if (isInputClosed)
                    return -1;
                if (n == HostNet.RESET)
                    connectionReset = true;
                throw failure(n, error[0], "Read timed out");
            }
            return n;
        } finally {
            readLock.unlock();
        }
    }

    private void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len > 0) {
            writeLock.lock();
            try {
                int h;
                synchronized (stateLock) {
                    ensureOpenAndConnected();
                    h = handle;
                }
                String[] error = new String[1];
                int r = HostNet.write0(h, b, off, len, error);
                if (r < 0)
                    throw failure(r, error[0], "Write timed out");
            } finally {
                writeLock.unlock();
            }
        }
    }

    // -- creating, connecting, binding, accepting

    @Override
    protected void create(boolean stream) throws IOException {
        if (!stream) {
            throw new IOException("Datagram socket creation not supported");
        }
        synchronized (stateLock) {
            if (state != ST_NEW)
                throw new IOException("Already created");
            this.fd = new FileDescriptor();
            this.state = ST_UNCONNECTED;
        }
    }

    @Override
    protected void connect(SocketAddress remote, int millis) throws IOException {
        // SocketImpl connect only specifies IOException
        if (!(remote instanceof InetSocketAddress))
            throw new IOException("Unsupported address type");
        InetSocketAddress isa = (InetSocketAddress) remote;
        if (isa.isUnresolved()) {
            throw new UnknownHostException(isa.getHostName());
        }
        InetAddress address = isa.getAddress();
        if (address.isAnyLocalAddress())
            address = InetAddress.getLocalHost();
        int port = isa.getPort();

        readLock.lock();
        try {
            InetAddress local;
            int localPort;
            synchronized (stateLock) {
                int state = this.state;
                if (state != ST_UNCONNECTED) {
                    if (state == ST_NEW)
                        throw new SocketException("Not created");
                    if (state == ST_CONNECTING)
                        throw new SocketException("Connection in progress");
                    if (state == ST_CONNECTED)
                        throw new SocketException("Already connected");
                    throw new SocketException("Socket closed");
                }
                this.state = ST_CONNECTING;
                this.address = address;
                this.port = port;
                local = bindAddress;
                localPort = bindPort;
            }
            String[] error = new String[1];
            int h = HostNet.connect0(literal(address), port,
                                     local == null && localPort == 0 ? null : literal(local),
                                     localPort, millis, error);
            boolean closedMeanwhile;
            synchronized (stateLock) {
                if (h >= 0 && state == ST_CONNECTING) {
                    handle = h;
                    state = ST_CONNECTED;
                    localport = HostNet.port0(h, true);
                    applyPending(h);
                    return;
                }
                if (h >= 0)
                    HostNet.close0(h);
                closedMeanwhile = state >= ST_CLOSED;
            }
            // the JVM closes a socket whose connection failed
            close();
            if (closedMeanwhile)
                throw new SocketException("Socket closed");
            throw HostNet.exception(h, error[0], "Connect timed out");
        } finally {
            readLock.unlock();
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        connect(new InetSocketAddress(host, port), 0);
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        connect(new InetSocketAddress(address, port), 0);
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (localport != 0 || handle >= 0 || bindAddress != null)
                throw new SocketException("Already bound");
            if (server) {
                String[] error = new String[1];
                int h = HostNet.listen0(host == null || host.isAnyLocalAddress() ? "" : literal(host),
                                        port, error);
                if (h < 0)
                    throw HostNet.exception(h, error[0], "Bind timed out");
                handle = h;
                localport = HostNet.port0(h, true);
                applyPending(h);
            } else {
                bindAddress = host;
                bindPort = port;
                localport = port;
            }
            // the address field is the given host address, as the JVM keeps it
            address = host;
        }
    }

    @Override
    protected void listen(int backlog) throws IOException {
        synchronized (stateLock) {
            ensureOpen();
            if (localport == 0 && handle < 0)
                throw new SocketException("Not bound");
        }
    }

    @Override
    protected void accept(SocketImpl si) throws IOException {
        HostSocketImpl nsi = (HostSocketImpl) si;
        if (nsi.state != ST_NEW)
            throw new SocketException("Not a newly created SocketImpl");
        int h;
        readLock.lock();
        try {
            int lh;
            synchronized (stateLock) {
                ensureOpen();
                if (handle < 0)
                    throw new SocketException("Not bound");
                lh = handle;
            }
            String[] error = new String[1];
            h = HostNet.accept0(lh, timeout, error);
            if (h < 0)
                throw failure(h, error[0], "Accept timed out");
        } finally {
            readLock.unlock();
        }
        synchronized (nsi.stateLock) {
            nsi.fd = new FileDescriptor();
            nsi.handle = h;
            nsi.localport = HostNet.port0(h, true);
            nsi.address = inetAddress(HostNet.address0(h, false));
            nsi.port = HostNet.port0(h, false);
            nsi.state = ST_CONNECTED;
        }
    }

    @Override
    protected InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] a = new byte[1];
                int n = read(a, 0, 1);
                return (n > 0) ? (a[0] & 0xff) : -1;
            }
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return HostSocketImpl.this.read(b, off, len);
            }
            @Override
            public int available() throws IOException {
                return HostSocketImpl.this.available();
            }
            @Override
            public void close() throws IOException {
                HostSocketImpl.this.close();
            }
        };
    }

    @Override
    protected OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                byte[] a = new byte[]{(byte) b};
                write(a, 0, 1);
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                HostSocketImpl.this.write(b, off, len);
            }
            @Override
            public void close() throws IOException {
                HostSocketImpl.this.close();
            }
        };
    }

    @Override
    protected int available() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (isInputClosed) {
                return 0;
            } else {
                return HostNet.available0(handle);
            }
        }
    }

    @Override
    protected void close() throws IOException {
        synchronized (stateLock) {
            int state = this.state;
            if (state >= ST_CLOSED)
                return;
            this.state = ST_CLOSED;
            int h = handle;
            if (h >= 0)
                HostNet.close0(h);
        }
    }

    // -- options

    private static volatile Set<SocketOption<?>> clientSocketOptions;
    private static volatile Set<SocketOption<?>> serverSocketOptions;

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> options = (server) ? serverSocketOptions : clientSocketOptions;
        if (options == null) {
            options = new HashSet<>();
            options.add(StandardSocketOptions.SO_RCVBUF);
            options.add(StandardSocketOptions.SO_REUSEADDR);
            options.add(StandardSocketOptions.IP_TOS);
            if (!server) {
                options.add(StandardSocketOptions.SO_KEEPALIVE);
                options.add(StandardSocketOptions.SO_SNDBUF);
                options.add(StandardSocketOptions.SO_LINGER);
                options.add(StandardSocketOptions.TCP_NODELAY);
            }
            options.add(StandardSocketOptions.SO_REUSEPORT);
            options = Collections.unmodifiableSet(options);
            if (server) {
                serverSocketOptions = options;
            } else {
                clientSocketOptions = options;
            }
        }
        return options;
    }

    /** SocketOptions' number of a standard option. */
    private static int number(SocketOption<?> opt) {
        if (opt == StandardSocketOptions.SO_RCVBUF) return SO_RCVBUF;
        if (opt == StandardSocketOptions.SO_SNDBUF) return SO_SNDBUF;
        if (opt == StandardSocketOptions.SO_REUSEADDR) return SO_REUSEADDR;
        if (opt == StandardSocketOptions.SO_REUSEPORT) return SO_REUSEPORT;
        if (opt == StandardSocketOptions.SO_KEEPALIVE) return SO_KEEPALIVE;
        if (opt == StandardSocketOptions.SO_LINGER) return SO_LINGER;
        if (opt == StandardSocketOptions.TCP_NODELAY) return TCP_NODELAY;
        if (opt == StandardSocketOptions.IP_TOS) return IP_TOS;
        return -1;
    }

    private static boolean isBoolean(int opt) {
        return opt == SO_REUSEADDR || opt == SO_REUSEPORT || opt == SO_KEEPALIVE
            || opt == TCP_NODELAY || opt == SO_OOBINLINE;
    }

    @Override
    protected <T> void setOption(SocketOption<T> opt, T value) throws IOException {
        if (!supportedOptions().contains(opt))
            throw new UnsupportedOperationException("'" + opt + "' not supported");
        if (!opt.type().isInstance(value))
            throw new IllegalArgumentException("Invalid value '" + value + "'");
        int n = number(opt);
        int v = isBoolean(n) ? (((Boolean) value) ? 1 : 0) : ((Integer) value).intValue();
        synchronized (stateLock) {
            ensureOpen();
            hostSet(n, v);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T getOption(SocketOption<T> opt) throws IOException {
        if (!supportedOptions().contains(opt))
            throw new UnsupportedOperationException("'" + opt + "' not supported");
        int n = number(opt);
        synchronized (stateLock) {
            ensureOpen();
            int v = hostGet(n);
            if (isBoolean(n))
                return (T) Boolean.valueOf(v != 0);
            return (T) Integer.valueOf(v);
        }
    }

    /** Sets option n (stateLock held): on the host's socket, else pending. */
    private void hostSet(int n, int v) throws SocketException {
        int h = handle;
        if (h < 0) {
            pending.put(n, v);
            return;
        }
        String[] error = new String[1];
        int r = HostNet.setOption0(h, n, v, error);
        if (r < 0)
            throw new SocketException(error[0]);
    }

    /** Option n (stateLock held): the host socket's, else the pending or default value. */
    private int hostGet(int n) throws SocketException {
        int h = handle;
        if (h < 0) {
            Integer v = pending.get(n);
            if (v != null)
                return v;
            if (n == SO_LINGER)
                return -1;
            if (n == SO_REUSEADDR)
                return server ? 1 : 0;
            if (n == SO_RCVBUF || n == SO_SNDBUF) {
                // the host's default, from a socket made for the question
                return hostDefault(n);
            }
            return 0;
        }
        int[] value = new int[1];
        String[] error = new String[1];
        int r = HostNet.getOption0(h, n, value, error);
        if (r < 0)
            throw new SocketException(error[0]);
        return value[0];
    }

    private static int hostDefault(int n) {
        return n == SO_RCVBUF ? 131072 : 16384;
    }

    private void applyPending(int h) throws SocketException {
        for (var e : pending.entrySet()) {
            String[] error = new String[1];
            HostNet.setOption0(h, e.getKey(), e.getValue(), error);
        }
        pending.clear();
    }

    private boolean booleanValue(Object value, String desc) throws SocketException {
        if (!(value instanceof Boolean))
            throw new SocketException("Bad value for " + desc);
        return (boolean) value;
    }

    private int intValue(Object value, String desc) throws SocketException {
        if (!(value instanceof Integer))
            throw new SocketException("Bad value for " + desc);
        return (int) value;
    }

    @Override
    public void setOption(int opt, Object value) throws SocketException {
        synchronized (stateLock) {
            ensureOpen();
            switch (opt) {
            case SO_LINGER: {
                // the value is "false" to disable, or linger interval to enable
                int i;
                if (value instanceof Boolean && ((boolean) value) == false) {
                    i = -1;
                } else {
                    i = intValue(value, "SO_LINGER");
                }
                hostSet(SO_LINGER, i);
                break;
            }
            case SO_TIMEOUT: {
                int i = intValue(value, "SO_TIMEOUT");
                if (i < 0)
                    throw new IllegalArgumentException("timeout < 0");
                timeout = i;
                break;
            }
            case IP_TOS:
                hostSet(IP_TOS, intValue(value, "IP_TOS"));
                break;
            case TCP_NODELAY:
                hostSet(TCP_NODELAY, booleanValue(value, "TCP_NODELAY") ? 1 : 0);
                break;
            case SO_SNDBUF: {
                int i = intValue(value, "SO_SNDBUF");
                if (i <= 0)
                    throw new SocketException("SO_SNDBUF <= 0");
                hostSet(SO_SNDBUF, i);
                break;
            }
            case SO_RCVBUF: {
                int i = intValue(value, "SO_RCVBUF");
                if (i <= 0)
                    throw new SocketException("SO_RCVBUF <= 0");
                hostSet(SO_RCVBUF, i);
                break;
            }
            case SO_KEEPALIVE:
                hostSet(SO_KEEPALIVE, booleanValue(value, "SO_KEEPALIVE") ? 1 : 0);
                break;
            case SO_OOBINLINE:
                hostSet(SO_OOBINLINE, booleanValue(value, "SO_OOBINLINE") ? 1 : 0);
                break;
            case SO_REUSEADDR:
                hostSet(SO_REUSEADDR, booleanValue(value, "SO_REUSEADDR") ? 1 : 0);
                break;
            case SO_REUSEPORT:
                hostSet(SO_REUSEPORT, booleanValue(value, "SO_REUSEPORT") ? 1 : 0);
                break;
            default:
                throw new SocketException("Unknown option " + opt);
            }
        }
    }

    @Override
    public Object getOption(int opt) throws SocketException {
        synchronized (stateLock) {
            ensureOpen();
            switch (opt) {
            case SO_TIMEOUT:
                return timeout;
            case TCP_NODELAY:
            case SO_OOBINLINE:
            case SO_REUSEADDR:
            case SO_KEEPALIVE:
            case SO_REUSEPORT:
                return hostGet(opt) != 0;
            case SO_LINGER: {
                // return "false" when disabled, linger interval when enabled
                int i = hostGet(SO_LINGER);
                if (i == -1) {
                    return Boolean.FALSE;
                } else {
                    return i;
                }
            }
            case SO_BINDADDR: {
                int h = handle;
                if (h < 0) {
                    try {
                        return bindAddress != null ? bindAddress : InetAddress.getByName("0.0.0.0");
                    } catch (UnknownHostException e) {
                        throw new SocketException(e.getMessage());
                    }
                }
                try {
                    return inetAddress(HostNet.address0(h, true));
                } catch (IOException e) {
                    throw new SocketException(e.getMessage());
                }
            }
            case SO_SNDBUF:
            case SO_RCVBUF:
            case IP_TOS:
                return hostGet(opt);
            default:
                throw new SocketException("Unknown option " + opt);
            }
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (!isInputClosed) {
                String[] error = new String[1];
                int r = HostNet.shutdown0(handle, HostNet.SHUT_RD, error);
                if (r < 0)
                    throw HostNet.exception(r, error[0], "");
                isInputClosed = true;
            }
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        synchronized (stateLock) {
            ensureOpenAndConnected();
            if (!isOutputClosed) {
                String[] error = new String[1];
                int r = HostNet.shutdown0(handle, HostNet.SHUT_WR, error);
                if (r < 0)
                    throw HostNet.exception(r, error[0], "");
                isOutputClosed = true;
            }
        }
    }

    @Override
    protected boolean supportsUrgentData() {
        return false;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        throw new SocketException("Urgent data not supported");
    }
}
