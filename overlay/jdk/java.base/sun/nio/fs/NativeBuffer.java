/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F9 (write-ahead)"): jdk26u's
 * sun.nio.fs.NativeBuffer (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/nio/fs/NativeBuffer.java), derived mechanically (test/jrt/derive.ae, each
 * replacement asserted once): no Cleaner (java.lang.ref.Cleaner is not in the Go build, F17): free() frees the
 * memory itself, once; a buffer dropped without free (a dead thread's cache) is not freed. The changes Copyright
 * (c) the Arbace authors; Eclipse Public License 1.0. jdk26u's notice follows.
 */
/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import jdk.internal.misc.Unsafe;

/**
 * A light-weight buffer in native memory.
 */

class NativeBuffer implements AutoCloseable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private final long address;
    private final int size;
    private boolean freed;

    // optional "owner" to avoid copying
    // (only safe for use by thread-local caches)
    private Object owner;

    private static class Deallocator implements Runnable {
        private final long address;
        Deallocator(long address) {
            this.address = address;
        }
        public void run() {
            unsafe.freeMemory(address);
        }
    }

    NativeBuffer(int size) {
        this.address = unsafe.allocateMemory(size);
        this.size = size;

    }

    @Override
    public void close() {
        release();
    }

    void release() {
        NativeBuffers.releaseNativeBuffer(this);
    }

    long address() {
        return address;
    }

    int size() {
        return size;
    }

    void free() {
        // Arbace: no Cleaner; the Deallocator runs once
        synchronized (this) {
            if (freed)
                return;
            freed = true;
        }
        new Deallocator(address).run();
    }

    // not synchronized; only safe for use by thread-local caches
    void setOwner(Object owner) {
        this.owner = owner;
    }

    // not synchronized; only safe for use by thread-local caches
    Object owner() {
        return owner;
    }
}
