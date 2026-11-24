/*
 * Arbace (overlay/jdk; java-rows-1124, the user's decision of 2026-11-24): the Go build's extended charsets,
 * the jdk.charsets charsets jdk26u's tests name and no more (ISO-2022-JP and its Microsoft variants,
 * x-eucJP-Open, x-windows-950, x-PCK), in place of jdk26u's generated sun.nio.cs.ext.ExtendedCharsets
 * (make/modules/jdk.charsets/Gensrc.gmk's extcs step over ExtendedCharsets.java.template, which loads every
 * charset of the module by class name through AbstractCharsetProvider). java.nio.charset.Charset's variant
 * (overlay/jdk/variants/Charset.ae) gives it as the one extended provider; the charsets are constructed
 * directly, as jrt's sun.nio.cs.JrtCharsets constructs the standard ones (no class loading by name in the Go
 * build). The canonical names, class names and aliases are jdk26u's (make/data/charsetmapping/charsets,
 * baf63fb); the charsets' own constructors ask aliasesFor, as jdk26u's generated and hand-written ones do.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** The Go build's provider of jdk.charsets' charsets it holds. */
public class ExtendedCharsets extends CharsetProvider {

    /** canonical name, then its aliases, per charset, as jdk26u's charsets file lists them */
    private static final String[][] NAMES = {
        {"ISO-2022-JP", "iso2022jp", "jis", "csISO2022JP", "jis_encoding", "csjisencoding"},
        {"ISO-2022-JP-2", "csISO2022JP2", "iso2022jp2"},
        {"x-windows-50220", "ms50220", "cp50220"},
        {"x-windows-50221", "ms50221", "cp50221"},
        {"x-windows-iso2022jp", "windows-iso2022jp"},
        {"x-eucJP-Open", "EUC_JP_Solaris", "eucJP-open"},
        {"x-windows-950", "ms950", "windows-950"},
        {"x-PCK", "pck"},
    };

    public ExtendedCharsets() {
    }

    /** The aliases of the charset of canonical name charsetName, or null when it is none of these. */
    public static String[] aliasesFor(String charsetName) {
        for (String[] ns : NAMES) {
            if (ns[0].equals(charsetName)) {
                String[] as = new String[ns.length - 1];
                System.arraycopy(ns, 1, as, 0, as.length);
                return as;
            }
        }
        return null;
    }

    private static Charset newCharset(String canonicalName) {
        switch (canonicalName) {
            case "ISO-2022-JP": return new ISO2022_JP();
            case "ISO-2022-JP-2": return new ISO2022_JP_2();
            case "x-windows-50220": return new MS50220();
            case "x-windows-50221": return new MS50221();
            case "x-windows-iso2022jp": return new MSISO2022JP();
            case "x-eucJP-Open": return new EUC_JP_Open();
            case "x-windows-950": return new MS950();
            case "x-PCK": return new PCK();
            default: return null;
        }
    }

    /** The canonical name of charsetName (a canonical name or an alias, its case ignored), or null. */
    private static String canonical(String charsetName) {
        String n = charsetName.toLowerCase(Locale.ROOT);
        for (String[] ns : NAMES) {
            for (String a : ns) {
                if (a.toLowerCase(Locale.ROOT).equals(n)) {
                    return ns[0];
                }
            }
        }
        return null;
    }

    private final java.util.Map<String, Charset> cache = new java.util.HashMap<>();

    @Override
    public synchronized Charset charsetForName(String charsetName) {
        String cn = canonical(charsetName);
        if (cn == null) {
            return null;
        }
        Charset cs = cache.get(cn);
        if (cs == null) {
            cs = newCharset(cn);
            cache.put(cn, cs);
        }
        return cs;
    }

    @Override
    public Iterator<Charset> charsets() {
        List<Charset> l = new ArrayList<>();
        for (String[] ns : NAMES) {
            l.add(charsetForName(ns[0]));
        }
        return l.iterator();
    }
}
