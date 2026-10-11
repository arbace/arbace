/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package jdk.internal.jrt;

import java.util.Objects;

/**
 * The store (doc/go/BOX-NOTES.md, S3): blobs named by their SHA-256 and refs, names pointing
 * at blobs, the store's only mutable state, changed by compare-and-set. In the box (B1b) the
 * monitor keeps it in a directory of the host's (its hypercalls BlobPut, BlobSize, BlobGet,
 * RefGet, RefSet); in the native executable jrt keeps it in the directory ARBACE_BOX_STORE
 * names, in the same format (blobs/HASH, refs/NAME). jrt's host answers (go/arbace/jrt/store.ae,
 * the optional HostStore); the namespace arbace.box is its user, and turns the answers below
 * into its errors.
 *
 * <p>Every method answers a count, a size or OK, or one of the negative answers, the box's
 * ABI's (arbace/box/abi: Store*).
 */
public final class HostStore {

    private HostStore() {
    }

    /** Done. */
    public static final int OK = 0;
    /** No such blob or ref, or the store refused (a host error). */
    public static final int REFUSED = -1;
    /** setRef's comparison failed: the ref is not what the caller said. */
    public static final int CONFLICT = -2;
    /** No store is configured (ARBACE_BOX_STORE, or the launcher's --store). */
    public static final int NONE = -3;
    /** put: the blob is larger than the store takes (ARBACE_BOX_STORE_MAX). */
    public static final int TOO_LARGE = -4;
    /** ref, setRef: a name the store does not take (1 to 64 of [A-Za-z0-9._-], no leading dot). */
    public static final int BAD_NAME = -5;

    /** The length of a hash, a SHA-256. */
    public static final int HASH_BYTES = 32;

    /** OK when the host has a store, else NONE. */
    public static int status() {
        return status0();
    }

    /** Stores data[off, off+len), writing its hash to hash[0, 32): OK, or a negative answer. */
    public static int put(byte[] data, int off, int len, byte[] hash) {
        Objects.checkFromIndexSize(off, len, data.length);
        checkHash(hash);
        return put0(data, off, len, hash);
    }

    /** The size of the blob hash names, or a negative answer (REFUSED: none). */
    public static long size(byte[] hash) {
        checkHash(hash);
        return size0(hash);
    }

    /**
     * Reads the blob hash names from its offset at into buf[off, off+len): the count (0 at or
     * past its end), or a negative answer (REFUSED: no such blob).
     */
    public static int get(byte[] hash, long at, byte[] buf, int off, int len) {
        checkHash(hash);
        Objects.checkFromIndexSize(off, len, buf.length);
        if (at < 0) {
            throw new IllegalArgumentException("a negative offset: " + at);
        }
        return get0(hash, at, buf, off, len);
    }

    /** Writes the hash ref name points at to hash: OK, or a negative answer (REFUSED: no ref). */
    public static int ref(String name, byte[] hash) {
        Objects.requireNonNull(name);
        checkHash(hash);
        return ref0(name, hash);
    }

    /**
     * Points ref name at the blob hash names: unconditionally when old is null, else only if the
     * ref points at old now (all zero: if there is no such ref). OK, or a negative answer
     * (CONFLICT: the comparison failed; REFUSED: no such blob).
     */
    public static int setRef(String name, byte[] hash, byte[] old) {
        Objects.requireNonNull(name);
        checkHash(hash);
        if (old != null) {
            checkHash(old);
        }
        return setRef0(name, hash, old);
    }

    /**
     * The files under dir as a tar (a snapshot: directories and regular files, sorted, their
     * times the epoch), or null with the reason in error[0].
     */
    public static byte[] tar(String dir, String[] error) {
        Objects.requireNonNull(dir);
        return tar0(dir, error);
    }

    /** Writes the tar's directories and regular files under dir: null, or the reason. */
    public static String untar(byte[] tar, String dir) {
        Objects.requireNonNull(tar);
        Objects.requireNonNull(dir);
        return untar0(tar, dir);
    }

    /** The longest blob read whole: the largest array. */
    private static final long MAX_ARRAY = Integer.MAX_VALUE - 8;

    /**
     * The blob hash names, read whole; null when the store has none (or there is no store). A
     * blob longer than an array can be is an IllegalStateException.
     */
    public static byte[] read(byte[] hash) {
        long n = size(hash);
        if (n < 0) {
            return null;
        }
        if (n > MAX_ARRAY) {
            throw new IllegalStateException("blob of " + n + " bytes: longer than an array can be");
        }
        byte[] b = new byte[(int) n];
        for (int off = 0; off < b.length; ) {
            int k = get0(hash, off, b, off, b.length - off);
            if (k <= 0) {
                return null;
            }
            off += k;
        }
        return b;
    }

    /**
     * The entries of a path such as ARBACE_PATH, in order: directories separated by ':', and
     * the store's entries, "store:PREFIX", each from "store:" to the next ':' (so "store:" or
     * "store::" for an empty PREFIX before another entry). Empty entries are left out.
     */
    public static String[] pathEntries(String path) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        int i = 0;
        while (i <= path.length()) {
            int from = path.startsWith("store:", i) ? i + 6 : i;
            int colon = path.indexOf(':', from);
            int end = colon < 0 ? path.length() : colon;
            if (end > i) {
                out.add(path.substring(i, end));
            }
            i = end + 1;
        }
        return out.toArray(new String[0]);
    }

    /**
     * Resource name ("a/b_c.ae") as the path entry "store:PREFIX" finds it: the blob that ref
     * PREFIX + name, its '/'s made '.'s ("PREFIXa.b_c.ae"), points at; null when there is no such
     * ref, the name is not one the store takes, or there is no store.
     */
    public static byte[] resource(String prefix, String name) {
        byte[] hash = new byte[HASH_BYTES];
        if (ref0(prefix + name.replace('/', '.'), hash) != OK) {
            return null;
        }
        return read(hash);
    }

    private static void checkHash(byte[] hash) {
        if (hash.length != HASH_BYTES) {
            throw new IllegalArgumentException("a hash is 32 bytes, not " + hash.length);
        }
    }

    private static native int status0();

    private static native int put0(byte[] data, int off, int len, byte[] hash);

    private static native long size0(byte[] hash);

    private static native int get0(byte[] hash, long at, byte[] buf, int off, int len);

    private static native int ref0(String name, byte[] hash);

    private static native int setRef0(String name, byte[] hash, byte[] old);

    private static native byte[] tar0(String dir, String[] error);

    private static native String untar0(byte[] tar, String dir);
}
