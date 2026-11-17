/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F8, "As built: F8 (write-ahead)"): the
 * Go build's standard charsets, constructed directly by class name, as jrt's JrtServices constructs
 * the security providers' services: sun.nio.cs.StandardCharsets.lookup (a variant,
 * overlay/jdk/variants/CsStandardCharsets.ae) asks here in place of jdk26u's
 * Class.forName(packagePrefix + className).newInstance() (no class loading by name in the Go build).
 * The cases are the class names of the generated StandardCharsets' Classes map for linux
 * (make/data/charsetmapping/charsets and stdcs-linux, jdk26u baf63fb): 60 classes of sun.nio.cs.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.cs;

import java.nio.charset.Charset;

/** The Go build's standard charsets by class name. */
final class JrtCharsets {
    private JrtCharsets() {
    }

    /** A new instance of sun.nio.cs's charset class className, or null when there is none. */
    static Charset newCharset(String className) {
        switch (className) {
            case "Big5": return new Big5();
            case "Big5_HKSCS": return new Big5_HKSCS();
            case "CESU_8": return new CESU_8();
            case "EUC_CN": return new EUC_CN();
            case "EUC_JP": return new EUC_JP();
            case "EUC_JP_LINUX": return new EUC_JP_LINUX();
            case "EUC_KR": return new EUC_KR();
            case "EUC_TW": return new EUC_TW();
            case "GB18030": return new GB18030();
            case "GBK": return new GBK();
            case "IBM437": return new IBM437();
            case "IBM737": return new IBM737();
            case "IBM775": return new IBM775();
            case "IBM850": return new IBM850();
            case "IBM852": return new IBM852();
            case "IBM855": return new IBM855();
            case "IBM857": return new IBM857();
            case "IBM858": return new IBM858();
            case "IBM862": return new IBM862();
            case "IBM866": return new IBM866();
            case "IBM874": return new IBM874();
            case "ISO_8859_1": return new ISO_8859_1();
            case "ISO_8859_11": return new ISO_8859_11();
            case "ISO_8859_13": return new ISO_8859_13();
            case "ISO_8859_15": return new ISO_8859_15();
            case "ISO_8859_16": return new ISO_8859_16();
            case "ISO_8859_2": return new ISO_8859_2();
            case "ISO_8859_3": return new ISO_8859_3();
            case "ISO_8859_4": return new ISO_8859_4();
            case "ISO_8859_5": return new ISO_8859_5();
            case "ISO_8859_6": return new ISO_8859_6();
            case "ISO_8859_7": return new ISO_8859_7();
            case "ISO_8859_8": return new ISO_8859_8();
            case "ISO_8859_9": return new ISO_8859_9();
            case "JIS_X_0201": return new JIS_X_0201();
            case "JIS_X_0208": return new JIS_X_0208();
            case "JIS_X_0212": return new JIS_X_0212();
            case "Johab": return new Johab();
            case "KOI8_R": return new KOI8_R();
            case "KOI8_U": return new KOI8_U();
            case "MS1250": return new MS1250();
            case "MS1251": return new MS1251();
            case "MS1252": return new MS1252();
            case "MS1253": return new MS1253();
            case "MS1254": return new MS1254();
            case "MS1257": return new MS1257();
            case "MS932": return new MS932();
            case "SJIS": return new SJIS();
            case "TIS_620": return new TIS_620();
            case "US_ASCII": return new US_ASCII();
            case "UTF_16": return new UTF_16();
            case "UTF_16BE": return new UTF_16BE();
            case "UTF_16LE": return new UTF_16LE();
            case "UTF_16LE_BOM": return new UTF_16LE_BOM();
            case "UTF_32": return new UTF_32();
            case "UTF_32BE": return new UTF_32BE();
            case "UTF_32BE_BOM": return new UTF_32BE_BOM();
            case "UTF_32LE": return new UTF_32LE();
            case "UTF_32LE_BOM": return new UTF_32LE_BOM();
            case "UTF_8": return new UTF_8();
            default: return null;
        }
    }
}
