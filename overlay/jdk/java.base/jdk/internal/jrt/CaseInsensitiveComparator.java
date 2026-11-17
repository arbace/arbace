/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): the value of String.CASE_INSENSITIVE_ORDER
 * in jrt, whose String is hand-written Go. jdk26u's String.CaseInsensitiveComparator compares
 * as String.compareToIgnoreCase does, which jrt's String implements; this class is that
 * comparator. Its serial form is jdk26u's (the class named java.lang.String$CaseInsensitiveComparator,
 * j2g's java-names; readResolve the one instance); INSTANCE is typed Comparator so that the member
 * table names no class by this internal name (n-java-serial, 2026-11-17: jdk/java/lang/String/
 * CaseInsensitiveComparator.java). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.Comparator;

/** String.CASE_INSENSITIVE_ORDER. */
public final class CaseInsensitiveComparator implements Comparator<String>, java.io.Serializable {

    private static final long serialVersionUID = 8575799808933029326L;

    /** The one instance, String.CASE_INSENSITIVE_ORDER. */
    public static final Comparator<String> INSTANCE = new CaseInsensitiveComparator();

    private CaseInsensitiveComparator() {
    }

    @Override
    public int compare(String s1, String s2) {
        return s1.compareToIgnoreCase(s2);
    }

    /** The one instance, as deserialization answers it. */
    private Object readResolve() {
        return INSTANCE;
    }
}
