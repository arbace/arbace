/*
 * Arbace (overlay/jdk, queue task 398, java.nio's asynchronous channels): jdk26u's
 * sun.nio.ch.AsynchronousFileChannelImpl (src/java.base/share/classes/sun/nio/ch/AsynchronousFileChannelImpl.java
 * at https://github.com/openjdk/jdk26u baf63fb), derived by test/jrt/derive.ae (bin/jrt-convert derive): no JavaNioAccess (isThreadConfined false: no confined arena in the Go build); the locks in jrt's lock table (jdk.internal.jrt.HostFileChannels, the key lockKey() of the subclass) in place of FileLockTable;
 * nothing else changed. jdk26u's text Copyright (c) Oracle and/or its affiliates, under the GNU General Public
 * License version 2 with the Classpath Exception (LICENSE.md); the derivation Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
/*
 * Copyright (c) 2008, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.*;
import java.io.FileDescriptor;
import java.io.IOException;
import jdk.internal.jrt.HostFileChannels;

/**
 * Base implementation of AsynchronousFileChannel.
 */

abstract class AsynchronousFileChannelImpl
    extends AsynchronousFileChannel
{
    // Arbace: no buffer is thread-confined in the Go build (java.lang.foreign's arenas are outside it)
    private static boolean isThreadConfined(ByteBuffer bb) {
        return false;
    }

    // close support
    protected final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    protected volatile boolean closed;

    // file descriptor
    protected final FileDescriptor fdObj;

    // indicates if open for reading/writing
    protected final boolean reading;
    protected final boolean writing;

    // associated Executor
    protected final ExecutorService executor;

    protected AsynchronousFileChannelImpl(FileDescriptor fdObj,
                                          boolean reading,
                                          boolean writing,
                                          ExecutorService executor)
    {
        this.fdObj = fdObj;
        this.reading = reading;
        this.writing = writing;
        this.executor = executor;
    }

    final ExecutorService executor() {
        return executor;
    }

    @Override
    public final boolean isOpen() {
        return !closed;
    }

    /**
     * Marks the beginning of an I/O operation.
     *
     * @throws  ClosedChannelException  If channel is closed
     */
    protected final void begin() throws IOException {
        closeLock.readLock().lock();
        if (closed)
            throw new ClosedChannelException();
    }

    /**
     * Marks the end of an I/O operation.
     */
    protected final void end() {
        closeLock.readLock().unlock();
    }

    /**
     * Marks end of I/O operation
     */
    protected final void end(boolean completed) throws IOException {
        end();
        if (!completed && !isOpen())
            throw new AsynchronousCloseException();
    }

    // -- file locking --

    abstract <A> Future<FileLock> implLock(long position,
                                           long size,
                                           boolean shared,
                                           A attachment,
                                           CompletionHandler<FileLock,? super A> handler);

    @Override
    public final Future<FileLock> lock(long position,
                                       long size,
                                       boolean shared)

    {
        return implLock(position, size, shared, null, null);
    }

    @Override
    public final <A> void lock(long position,
                               long size,
                               boolean shared,
                               A attachment,
                               CompletionHandler<FileLock,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        implLock(position, size, shared, attachment, handler);
    }

    // Arbace: the key of this channel's file in jrt's lock table (HostFileChannels.lockKey)
    abstract Object lockKey();

    final void ensureFileLockTableInitialized() throws IOException {
        // Arbace: jrt's lock table is the process's, made with it
    }

    final void invalidateAllLocks() throws IOException {
        {
            for (FileLock fl: HostFileChannels.removeLocks(lockKey(), this)) {
                synchronized (fl) {
                    if (fl.isValid()) {
                        FileLockImpl fli = (FileLockImpl)fl;
                        implRelease(fli);
                        fli.invalidate();
                    }
                }
            }
        }
    }

    /**
     * Adds region to lock table
     */
    protected final FileLockImpl addToFileLockTable(long position, long size, boolean shared) {
        final FileLockImpl fli;
        try {
            // like begin() but returns null instead of exception
            closeLock.readLock().lock();
            if (closed)
                return null;

            try {
                ensureFileLockTableInitialized();
            } catch (IOException x) {
                // should not happen
                throw new AssertionError(x);
            }
            fli = new FileLockImpl(this, position, size, shared);
            // may throw OverlappedFileLockException
            HostFileChannels.addLock(lockKey(), fli);
        } finally {
            end();
        }
        return fli;
    }

    protected final void removeFromFileLockTable(FileLockImpl fli) {
        HostFileChannels.removeLock(lockKey(), fli);
    }

    /**
     * Releases the given file lock.
     */
    protected abstract void implRelease(FileLockImpl fli) throws IOException;

    /**
     * Invoked by FileLockImpl to release the given file lock and remove it
     * from the lock table.
     */
    final void release(FileLockImpl fli) throws IOException {
        try {
            begin();
            implRelease(fli);
            removeFromFileLockTable(fli);
        } finally {
            end();
        }
    }


    // -- reading and writing --

    abstract <A> Future<Integer> implRead(ByteBuffer dst,
                                         long position,
                                         A attachment,
                                         CompletionHandler<Integer,? super A> handler);

    @Override
    public final Future<Integer> read(ByteBuffer dst, long position) {
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        if (isThreadConfined(dst))
            throw new IllegalArgumentException("Buffer is thread confined");
        return implRead(dst, position, null, null);
    }

    @Override
    public final <A> void read(ByteBuffer dst,
                               long position,
                               A attachment,
                               CompletionHandler<Integer,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        if (isThreadConfined(dst))
            throw new IllegalArgumentException("Buffer is thread confined");
        implRead(dst, position, attachment, handler);
    }

    abstract <A> Future<Integer> implWrite(ByteBuffer src,
                                           long position,
                                           A attachment,
                                           CompletionHandler<Integer,? super A> handler);


    @Override
    public final Future<Integer> write(ByteBuffer src, long position) {
        if (isThreadConfined(src))
            throw new IllegalArgumentException("Buffer is thread confined");
        return implWrite(src, position, null, null);
    }

    @Override
    public final <A> void write(ByteBuffer src,
                                long position,
                                A attachment,
                                CompletionHandler<Integer,? super A> handler)
    {
        if (handler == null)
            throw new NullPointerException("'handler' is null");
        if (isThreadConfined(src))
            throw new IllegalArgumentException("Buffer is thread confined");
        implWrite(src, position, attachment, handler);
    }
}
