/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jdk26u's
 * sun.nio.ch.PollSelectorImpl (unix, baf63fb) with its pollfd array as three int arrays polled by
 * jdk.internal.jrt.HostChannels.poll1 (PollSelectorImpl.c's poll), derived by .tmp/f10/genpoll.py
 * (the triage's derivation). The derivation's changes: Copyright (c) the Arbace authors; Eclipse
 * Public License 1.0.
 */
/*
 * Copyright (c) 2001, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import jdk.internal.jrt.HostChannels;

/**
 * Selector implementation based on poll
 */

class PollSelectorImpl extends SelectorImpl {

    // initial capacity of poll array
    private static final int INITIAL_CAPACITY = 16;

    // poll array, grows as needed
    private int pollArrayCapacity = INITIAL_CAPACITY;
    private int pollArraySize;
    private int[] pollFds;
    private int[] pollEvents;
    private int[] pollRevents;

    // file descriptors used for interrupt
    private final int fd0;
    private final int fd1;

    // keys for file descriptors in poll array, synchronize on selector
    private final List<SelectionKeyImpl> pollKeys = new ArrayList<>();

    // pending updates, queued by putEventOps
    private final Object updateLock = new Object();
    private final Deque<SelectionKeyImpl> updateKeys = new ArrayDeque<>();

    // interrupt triggering and clearing
    private final Object interruptLock = new Object();
    private boolean interruptTriggered;

    PollSelectorImpl(SelectorProvider sp) throws IOException {
        super(sp);

        this.pollFds = new int[pollArrayCapacity];
        this.pollEvents = new int[pollArrayCapacity];
        this.pollRevents = new int[pollArrayCapacity];

        try {
            long fds = IOUtil.makePipe(false);
            this.fd0 = (int) (fds >>> 32);
            this.fd1 = (int) fds;
        } catch (IOException ioe) {
            throw ioe;
        }

        // wakeup support
        synchronized (this) {
            setFirst(fd0, Net.POLLIN);
        }
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    protected int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        assert Thread.holdsLock(this);

        int to = (int) Math.min(timeout, Integer.MAX_VALUE); // max poll timeout
        boolean blocking = (to != 0);
        boolean timedPoll = (to > 0);

        processUpdateQueue();
        processDeregisterQueue();
        try {
            begin(blocking);

            int numPolled;
            do {
                long startTime = timedPoll ? System.nanoTime() : 0;
                numPolled = poll(pollArraySize, to);
                if (numPolled == IOStatus.INTERRUPTED && timedPoll) {
                    // timed poll interrupted so need to adjust timeout
                    long adjust = System.nanoTime() - startTime;
                    to -= (int) TimeUnit.NANOSECONDS.toMillis(adjust);
                    if (to <= 0) {
                        // timeout expired so no retry
                        numPolled = 0;
                    }
                }
            } while (numPolled == IOStatus.INTERRUPTED);
            assert numPolled <= pollArraySize;

        } finally {
            end(blocking);
        }

        processDeregisterQueue();
        return processEvents(action);
    }

    /**
     * Process changes to the interest ops.
     */
    private void processUpdateQueue() {
        assert Thread.holdsLock(this);

        synchronized (updateLock) {
            SelectionKeyImpl ski;
            while ((ski = updateKeys.pollFirst()) != null) {
                int newEvents = ski.translateInterestOps();
                if (ski.isValid()) {
                    int index = ski.getIndex();
                    assert index >= 0 && index < pollArraySize;
                    if (index > 0) {
                        assert pollKeys.get(index) == ski;
                        if (newEvents == 0) {
                            remove(ski);
                        } else {
                            update(ski, newEvents);
                        }
                    } else if (newEvents != 0) {
                        add(ski, newEvents);
                    }
                }
            }
        }
    }

    /**
     * Process the polled events.
     * If the interrupt fd has been selected, drain it and clear the interrupt.
     */
    private int processEvents(Consumer<SelectionKey> action)
        throws IOException
    {
        assert Thread.holdsLock(this);
        assert pollArraySize > 0 && pollArraySize == pollKeys.size();

        int numKeysUpdated = 0;
        for (int i = 1; i < pollArraySize; i++) {
            int rOps = getReventOps(i);
            if (rOps != 0) {
                SelectionKeyImpl ski = pollKeys.get(i);
                assert ski.getFDVal() == getDescriptor(i);
                if (ski.isValid()) {
                    numKeysUpdated += processReadyEvents(rOps, ski, action);
                }
            }
        }

        // check for interrupt
        if (getReventOps(0) != 0) {
            assert getDescriptor(0) == fd0;
            clearInterrupt();
        }

        return numKeysUpdated;
    }

    @Override
    protected void implClose() throws IOException {
        assert !isOpen();
        assert Thread.holdsLock(this);

        // prevent further wakeup
        synchronized (interruptLock) {
            interruptTriggered = true;
        }

        closeIntFD(fd0);
        closeIntFD(fd1);
    }

    @Override
    protected void implRegister(SelectionKeyImpl ski) {
        assert ski.getIndex() == 0;
        ensureOpen();
    }

    @Override
    protected void implDereg(SelectionKeyImpl ski) throws IOException {
        assert !ski.isValid();
        assert Thread.holdsLock(this);

        // remove from poll array
        int index = ski.getIndex();
        if (index > 0) {
            remove(ski);
        }
    }

    @Override
    public void setEventOps(SelectionKeyImpl ski) {
        synchronized (updateLock) {
            updateKeys.addLast(ski);
        }
    }

    @Override
    public Selector wakeup() {
        synchronized (interruptLock) {
            if (!interruptTriggered) {
                try {
                    IOUtil.write1(fd1, (byte)0);
                } catch (IOException ioe) {
                    throw new InternalError(ioe);
                }
                interruptTriggered = true;
            }
        }
        return this;
    }

    private void clearInterrupt() throws IOException {
        synchronized (interruptLock) {
            IOUtil.drain(fd0);
            interruptTriggered = false;
        }
    }

    /**
     * Sets the first pollfd entry in the poll array to the given fd
     */
    private void setFirst(int fd, int ops) {
        assert pollArraySize == 0;
        assert pollKeys.isEmpty();

        putDescriptor(0, fd);
        putEventOps(0, ops);
        pollArraySize = 1;

        pollKeys.add(null);  // dummy element
    }

    /**
     * Adds a pollfd entry to the poll array, expanding the poll array if needed.
     */
    private void add(SelectionKeyImpl ski, int ops) {
        expandIfNeeded();

        int index = pollArraySize;
        assert index > 0;
        putDescriptor(index, ski.getFDVal());
        putEventOps(index, ops);
        putReventOps(index, 0);
        ski.setIndex(index);
        pollArraySize++;

        pollKeys.add(ski);
        assert pollKeys.size() == pollArraySize;
    }

    /**
     * Update the events of pollfd entry.
     */
    private void update(SelectionKeyImpl ski, int ops) {
        int index = ski.getIndex();
        assert index > 0 && index < pollArraySize;
        assert getDescriptor(index) == ski.getFDVal();
        putEventOps(index, ops);
    }

    /**
     * Removes a pollfd entry from the poll array
     */
    private void remove(SelectionKeyImpl ski) {
        int index = ski.getIndex();
        assert index > 0 && index < pollArraySize;
        assert getDescriptor(index) == ski.getFDVal();

        // replace pollfd at index with the last pollfd in array
        int lastIndex = pollArraySize - 1;
        if (lastIndex != index) {
            SelectionKeyImpl lastKey = pollKeys.get(lastIndex);
            assert lastKey.getIndex() == lastIndex;
            int lastFd = getDescriptor(lastIndex);
            int lastOps = getEventOps(lastIndex);
            int lastRevents = getReventOps(lastIndex);
            assert lastKey.getFDVal() == lastFd;
            putDescriptor(index, lastFd);
            putEventOps(index, lastOps);
            putReventOps(index, lastRevents);
            pollKeys.set(index, lastKey);
            lastKey.setIndex(index);
        }
        pollKeys.remove(lastIndex);
        pollArraySize--;
        assert pollKeys.size() == pollArraySize;

        ski.setIndex(0);
    }

    /**
     * Expand poll array if at capacity
     */
    private void expandIfNeeded() {
        if (pollArraySize == pollArrayCapacity) {
            int newCapacity = pollArrayCapacity + INITIAL_CAPACITY;
            pollFds = java.util.Arrays.copyOf(pollFds, newCapacity);
            pollEvents = java.util.Arrays.copyOf(pollEvents, newCapacity);
            pollRevents = java.util.Arrays.copyOf(pollRevents, newCapacity);
            pollArrayCapacity = newCapacity;
        }
    }


    private void putDescriptor(int i, int fd) {
        pollFds[i] = fd;
    }

    private int getDescriptor(int i) {
        return pollFds[i];
    }

    private void putEventOps(int i, int event) {
        pollEvents[i] = (short)event;
    }

    private int getEventOps(int i) {
        return pollEvents[i];
    }

    private void putReventOps(int i, int revent) {
        pollRevents[i] = (short)revent;
    }

    private int getReventOps(int i) {
        return pollRevents[i];
    }

    // PollSelectorImpl.c's poll over the arrays (jrt)
    private int poll(int numfds, int timeout) throws IOException {
        int[] err = new int[1];
        int n = HostChannels.poll1(pollFds, pollEvents, pollRevents, numfds, timeout, err);
        if (n == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "poll failed");
        return n;
    }

    // FileDispatcherImpl.closeIntFD (jrt)
    private static void closeIntFD(int fd) throws IOException {
        int[] err = new int[1];
        if (HostChannels.close0(fd, err) == HostChannels.THROWN)
            throw HostChannels.ioException(err[0], "close failed");
    }

    static {
        IOUtil.load();
    }
}
