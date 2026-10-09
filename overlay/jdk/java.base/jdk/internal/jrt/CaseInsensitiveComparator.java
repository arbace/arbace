/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Phase 2C"): the value of String.CASE_INSENSITIVE_ORDER
 * in jrt, whose String is hand-written Go. jdk26u's String.CaseInsensitiveComparator compares
 * as String.compareToIgnoreCase does, which jrt's String implements; this class is that
 * comparator. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.Comparator;

/** String.CASE_INSENSITIVE_ORDER. */
public final class CaseInsensitiveComparator implements Comparator<String>, java.io.Serializable {

    private static final long serialVersionUID = 8575799808933029326L;

    /** The one instance, String.CASE_INSENSITIVE_ORDER. */
    public static final CaseInsensitiveComparator INSTANCE = new CaseInsensitiveComparator();

    private CaseInsensitiveComparator() {
    }

    @Override
    public int compare(String s1, String s2) {
        return s1.compareToIgnoreCase(s2);
    }
}
