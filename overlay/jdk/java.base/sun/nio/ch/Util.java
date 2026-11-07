/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): the members
 * of jdk26u's sun.nio.ch.Util (baf63fb) that the Go build's channels use: ungrowableSet, jdk26u's;
 * the temporary direct buffers (a per-thread cache of native memory in jdk26u) are heap buffers
 * allocated per use, as jrt has no native memory (IOUtil's overlay reads and writes arrays).
 * jdk26u's code: Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception
 * (LICENSE.md); the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class Util {

    private Util() { }

    /**
     * Returns a temporary buffer of at least the given size (a heap buffer in jrt).
     */
    public static ByteBuffer getTemporaryDirectBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

    /**
     * Releases a temporary buffer (nothing to release in jrt).
     */
    public static void releaseTemporaryDirectBuffer(ByteBuffer buf) {
    }

    /**
     * Releases a temporary buffer to the head of the cache (nothing to release in jrt).
     */
    static void offerFirstTemporaryDirectBuffer(ByteBuffer buf) {
    }

    /**
     * Releases a temporary buffer to the tail of the cache (nothing to release in jrt).
     */
    static void offerLastTemporaryDirectBuffer(ByteBuffer buf) {
    }

    static <E> Set<E> ungrowableSet(final Set<E> s) {
        return new Set<E>() {

                public int size()                 { return s.size(); }
                public boolean isEmpty()          { return s.isEmpty(); }
                public boolean contains(Object o) { return s.contains(o); }
                public Object[] toArray()         { return s.toArray(); }
                public <T> T[] toArray(T[] a)     { return s.toArray(a); }
                public String toString()          { return s.toString(); }
                public Iterator<E> iterator()     { return s.iterator(); }
                public boolean equals(Object o)   { return s.equals(o); }
                public int hashCode()             { return s.hashCode(); }
                public void clear()               { s.clear(); }
                public boolean remove(Object o)   { return s.remove(o); }

                public boolean containsAll(Collection<?> coll) {
                    return s.containsAll(coll);
                }
                public boolean removeAll(Collection<?> coll) {
                    return s.removeAll(coll);
                }
                public boolean retainAll(Collection<?> coll) {
                    return s.retainAll(coll);
                }

                public boolean add(E o){
                    throw new UnsupportedOperationException();
                }
                public boolean addAll(Collection<? extends E> coll) {
                    throw new UnsupportedOperationException();
                }

        };
    }
}
