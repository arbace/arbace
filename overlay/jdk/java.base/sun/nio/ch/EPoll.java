/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.EPoll, the members jdk26u's EPollSelectorImpl (linux, baf63fb) calls. jdk26u's poll
 * array is native memory of struct epoll_event read by Unsafe at an address; jrt has no native
 * memory, so an "address" here is a token naming a pair of int arrays (the events' descriptors and
 * events) in a table, an event's "address" the token plus its index, read by getDescriptor and
 * getEvents as jdk26u's reads its fields: EPollSelectorImpl is jdk26u's unchanged. The natives are
 * EPoll.c's over jdk.internal.jrt.HostChannels.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import jdk.internal.jrt.HostChannels;

/**
 * Provides access to the Linux epoll facility.
 */

class EPoll {
    private EPoll() { }

    // the bits of an event's "address" that index its array (NUM_EPOLLEVENTS <= 1024)
    private static final int INDEX_BITS = 20;
    private static final long INDEX_MASK = (1L << INDEX_BITS) - 1;

    // the poll arrays by token: {descriptors, events}
    private static final ConcurrentHashMap<Long, int[][]> ARRAYS = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT = new AtomicLong();

    // opcodes
    static final int EPOLL_CTL_ADD  = 1;
    static final int EPOLL_CTL_DEL  = 2;
    static final int EPOLL_CTL_MOD  = 3;

    // events
    static final int EPOLLIN   = 0x1;
    static final int EPOLLOUT  = 0x4;

    // flags
    static final int EPOLLONESHOT   = (1 << 30);

    /**
     * Allocates a poll array to handle up to {@code count} events.
     */
    static long allocatePollArray(int count) {
        if (count > INDEX_MASK)
            throw new IllegalArgumentException("count");
        long token = NEXT.incrementAndGet();
        ARRAYS.put(token, new int[][] { new int[count], new int[count] });
        return token << INDEX_BITS;
    }

    /**
     * Free a poll array
     */
    static void freePollArray(long address) {
        ARRAYS.remove(address >>> INDEX_BITS);
    }

    /**
     * Returns event[i];
     */
    static long getEvent(long address, int i) {
        return address + i;
    }

    /**
     * Returns event->data.fd
     */
    static int getDescriptor(long eventAddress) {
        return ARRAYS.get(eventAddress >>> INDEX_BITS)[0][(int) (eventAddress & INDEX_MASK)];
    }

    /**
     * Returns event->events
     */
    static int getEvents(long eventAddress) {
        return ARRAYS.get(eventAddress >>> INDEX_BITS)[1][(int) (eventAddress & INDEX_MASK)];
    }

    // -- Native methods (EPoll.c's, over HostChannels) --

    static int create() throws IOException {
        int[] err = new int[1];
        int epfd = HostChannels.epollCreate0(err);
        if (epfd == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "epoll_create1 failed");
        return epfd;
    }

    static int ctl(int epfd, int opcode, int fd, int events) {
        return HostChannels.epollCtl0(epfd, opcode, fd, events);
    }

    static int wait(int epfd, long pollAddress, int numfds, int timeout)
        throws IOException
    {
        int[][] a = ARRAYS.get(pollAddress >>> INDEX_BITS);
        int[] err = new int[1];
        int n = HostChannels.epollWait0(epfd, a[0], a[1], numfds, timeout, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "epoll_wait failed");
        return n;
    }
}
