/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Channels"; F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md):
 * the host's descriptors under java.nio's selectable channels. jdk26u's sun.nio.ch classes call
 * their natives (Net, IOUtil, SocketDispatcher, UnixDispatcher, PollSelectorImpl) on a
 * FileDescriptor; jrt's overlays of those classes keep their Java and call these natives on the
 * descriptor's number instead (IOUtil.fdVal). The natives are jrt's Go (go/arbace/jrt/channels.ae,
 * channels_linux.ae), ports of jdk26u's src/java.base/unix/native/libnio/ch/{Net,IOUtil,
 * SocketDispatcher,UnixDispatcher,PollSelectorImpl}.c: the same system calls, flags and results.
 *
 * Two departures, both below the natives' interface: every descriptor is non-blocking in the
 * kernel, and a descriptor configured blocking (configureBlocking0) waits in Go's poller, so that
 * an asynchronous close (UnixDispatcher.preClose, the JDK's dup2 and signal) wakes a thread blocked
 * in it (preClose0); and the JDK's IOStatus.THROWN, an exception raised by the native, is THROWN
 * here with the error number in err[0], which exception() turns into the JDK's exception (the
 * natives' JNU_Throw... calls, Net.c's handleSocketError).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;

/** The host's descriptors: sockets, pipes and poll, by number. */
public final class HostChannels {

    /** sun.nio.ch.IOStatus's values, as the natives return them. */
    public static final int EOF = -1;
    public static final int UNAVAILABLE = -2;
    public static final int INTERRUPTED = -3;
    /** An error: its number in err[0]. */
    public static final int THROWN = -5;

    /** The error numbers the exceptions are chosen by (Linux's, the same on amd64 and arm64). */
    public static final int EPIPE = 32;
    public static final int EACCES = 13;
    public static final int EPROTO = 71;
    public static final int EOPNOTSUPP = 95;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ECONNRESET = 104;
    public static final int ENOTCONN = 107;
    public static final int ETIMEDOUT = 110;
    public static final int ECONNREFUSED = 111;
    public static final int EHOSTUNREACH = 113;
    public static final int EINPROGRESS = 115;
    /** jrt's own: the host has no descriptors (a host without channels, the box's). */
    public static final int ENOHOST = -1;

    private HostChannels() {
    }

    /** The host's text of error number errno (the C library's strerror). */
    public static String text(int errno) {
        if (errno == ENOHOST)
            return "Channels not available";
        return strerror0(errno);
    }

    /**
     * Net.c's handleSocketErrorWithMessage: the java.net exception of a socket call's errno,
     * with message (null: the error's text alone, else "message: text").
     */
    public static IOException socketException(int errno, String message) {
        String text = message == null ? text(errno) : message + ": " + text(errno);
        switch (errno) {
            case EPROTO:
                return new ProtocolException(text);
            case ECONNREFUSED:
            case ETIMEDOUT:
            case ENOTCONN:
                return new ConnectException(text);
            case EHOSTUNREACH:
                return new NoRouteToHostException(text);
            case EADDRINUSE:
            case EADDRNOTAVAIL:
            case EACCES:
                return new BindException(text);
            default:
                return new SocketException(text);
        }
    }

    /** JNU_ThrowByNameWithLastError(env, "java/net/SocketException", message). */
    public static SocketException plainSocketException(int errno, String message) {
        return new SocketException(message + ": " + text(errno));
    }

    /** JNU_ThrowIOExceptionWithLastError(env, message): the error's text, else message. */
    public static IOException ioException(int errno, String message) {
        String text = text(errno);
        return new IOException(text == null || text.isEmpty() ? message : text);
    }

    /** IOUtil.c's convertReturnVal for a read or a write that failed (err[0] set). */
    public static IOException readWriteException(int errno, boolean reading) {
        return ioException(errno, reading ? "Read failed" : "Write failed");
    }

    // -- the natives; each answers THROWN with the error number in err[0] when it fails --

    /** strerror(errno). */
    static native String strerror0(int errno);

    /** ipv6_available(): the host has IPv6 (a socket of AF_INET6 can be made). */
    public static native boolean ipv6Available0();

    /** Net.socket0: a socket (AF_INET6 when ipv6, stream or datagram), SO_REUSEADDR when reuse. */
    public static native int socket0(boolean ipv6, boolean stream, boolean reuse, int[] err);

    /**
     * Net.bind0: binds fd to address addr (4 or 16 bytes; on an AF_INET6 socket a 4-byte address
     * is mapped), scope id scope, port: 0 or THROWN.
     */
    public static native int bind0(int fd, byte[] addr, int scope, int port, int[] err);

    /** Net.listen: 0 or THROWN. */
    public static native int listen0(int fd, int backlog, int[] err);

    /** Net.connect0: 1, UNAVAILABLE (in progress), INTERRUPTED or THROWN. */
    public static native int connect0(int fd, byte[] addr, int scope, int port, int[] err);

    /**
     * Net.accept: 1 with the new descriptor in newfd[0] and the peer's address in addr[0] (4 or
     * 16 bytes) and port, scope in port[0], port[1]; UNAVAILABLE, INTERRUPTED or THROWN.
     */
    public static native int accept0(int fd, int[] newfd, byte[][] addr, int[] port, int[] err);

    /** Net.shutdown (how: 0 read, 1 write, 2 both; ENOTCONN ignored): 0 or THROWN. */
    public static native int shutdown0(int fd, int how, int[] err);

    /**
     * Net.localInetAddress and localPort (local) or remoteInetAddress and remotePort: 0 with the
     * address in addr[0] and port, scope in port[0], port[1], or THROWN.
     */
    public static native int address0(int fd, boolean local, byte[][] addr, int[] port, int[] err);

    /**
     * Net.getIntOption0: the option's value into value[0] (IP_MULTICAST_TTL and _LOOP as a byte,
     * SO_LINGER's seconds or -1; with conversion: NET_GetSockOpt's halved buffer sizes): 0 or THROWN.
     */
    public static native int getIntOption0(int fd, boolean mayNeedConversion, int level, int opt,
                                           int[] value, int[] err);

    /** Net.setIntOption0 (with conversion: NET_SetSockOpt's IP_TOS and SO_RCVBUF): 0 or THROWN. */
    public static native int setIntOption0(int fd, boolean mayNeedConversion, int level, int opt,
                                           int arg, boolean isIPv6, int[] err);

    /**
     * Net.poll: the events of fd among events within timeout ms (0: now, -1: no limit), 0 when
     * none (or when fd was pre-closed), or THROWN.
     */
    public static native int poll0(int fd, int events, long timeout, int[] err);

    /** Net.pollConnect: 1 connected, 0 not yet, or THROWN (the connection's error in err[0]). */
    public static native int pollConnect0(int fd, long timeout, int[] err);

    /** Net.available (FIONREAD): the count or THROWN. */
    public static native int available0(int fd, int[] err);

    /** Net.sendOOB: 1, UNAVAILABLE, INTERRUPTED or THROWN. */
    public static native int sendOOB0(int fd, byte b, int[] err);

    /**
     * SocketDispatcher.read0 (and FileDispatcherImpl's for a pipe): up to len bytes of fd into b
     * at off: a count, EOF, UNAVAILABLE, INTERRUPTED or THROWN (ECONNRESET and EPIPE as ECONNRESET
     * when socket).
     */
    public static native int read0(int fd, byte[] b, int off, int len, boolean socket, int[] err);

    /** SocketDispatcher.write0: len bytes of b from off: a count, UNAVAILABLE, INTERRUPTED or THROWN. */
    public static native int write0(int fd, byte[] b, int off, int len, int[] err);

    /** UnixDispatcher.close0: 0 or THROWN. */
    public static native int close0(int fd, int[] err);

    /**
     * UnixDispatcher.preClose0: from now on fd reads its end, writes fail with EPIPE and polls
     * answer at once, as the JDK's dup2 of a closed socket over fd; a thread blocked in fd wakes.
     */
    public static native void preClose0(int fd);

    /** IOUtil.configureBlocking: 0 or THROWN. */
    public static native int configureBlocking0(int fd, boolean blocking, int[] err);

    /** IOUtil.makePipe: 0 with the read and write ends in fds[0], fds[1], or THROWN. */
    public static native int makePipe0(boolean blocking, int[] fds, int[] err);

    /** IOUtil.write1: 1, UNAVAILABLE, INTERRUPTED or THROWN. */
    public static native int writeOne0(int fd, byte b, int[] err);

    /** IOUtil.drain: 1 when something was read, 0 when not, or THROWN. */
    public static native int drain0(int fd, int[] err);

    /** IOUtil.drain1: 1 a byte read, 0 none (EAGAIN), or THROWN. */
    public static native int drainOne0(int fd, int[] err);

    /** IOUtil.fdLimit: RLIMIT_NOFILE's soft limit (Integer.MAX_VALUE when unlimited). */
    public static native int fdLimit0();

    /**
     * PollSelectorImpl.poll: poll(2) of n descriptors fds[i] with events[i] (shorts), their
     * revents into revents[i]: the count, INTERRUPTED or THROWN.
     */
    public static native int poll1(int[] fds, int[] events, int[] revents, int n, int timeout, int[] err);

    /** EPoll.create: epoll_create1(EPOLL_CLOEXEC), or THROWN. */
    public static native int epollCreate0(int[] err);

    /** EPoll.ctl: epoll_ctl of fd with events (the event's data its descriptor): 0 or the error number. */
    public static native int epollCtl0(int epfd, int opcode, int fd, int events);

    /**
     * EPoll.wait: epoll_wait for at most n events within timeout ms (negative: no limit), each event's
     * descriptor and events into fds[i], events[i]: the count, INTERRUPTED or THROWN.
     */
    public static native int epollWait0(int epfd, int[] fds, int[] events, int n, int timeout, int[] err);

    /** EventFD.eventfd0: eventfd(0, 0), non-blocking to the channels as jdk26u's makes it: the descriptor or THROWN. */
    public static native int eventfd0(int[] err);

    /** EventFD.set0: the counter's one added (an 8-byte write), as convertReturnVal. */
    public static native int eventfdSet0(int efd, int[] err);
}
