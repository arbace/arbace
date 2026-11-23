/*
 * Arbace (overlay/jdk, queue 543): the Go build's extended charsets (jdk.charsets), constructed directly by class
 * name, as JrtCharsets constructs the standard ones: sun.nio.cs.ext.AbstractCharsetProvider.lookup (a variant,
 * overlay/jdk/variants/CsExtAbstractCharsetProvider.ae) asks here in place of jdk26u's
 * Class.forName(packagePrefix + className).newInstance() (no class loading by name in the Go build). The cases are
 * the class names of the generated ExtendedCharsets' classMap for linux (make/data/charsetmapping/charsets, extcs
 * and stdcs-linux, jdk26u baf63fb): 113 classes of sun.nio.cs.ext.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.cs.ext;

import java.nio.charset.Charset;

/** The Go build's extended charsets by class name. */
final class JrtExtCharsets {
    private JrtExtCharsets() {
    }

    /** A new instance of sun.nio.cs.ext's charset class className, or null when there is none. */
    static Charset newCharset(String className) {
        switch (className) {
            case "Big5_HKSCS_2001": return new Big5_HKSCS_2001();
            case "Big5_Solaris": return new Big5_Solaris();
            case "EUC_JP_Open": return new EUC_JP_Open();
            case "IBM037": return new IBM037();
            case "IBM1006": return new IBM1006();
            case "IBM1025": return new IBM1025();
            case "IBM1026": return new IBM1026();
            case "IBM1046": return new IBM1046();
            case "IBM1047": return new IBM1047();
            case "IBM1097": return new IBM1097();
            case "IBM1098": return new IBM1098();
            case "IBM1112": return new IBM1112();
            case "IBM1122": return new IBM1122();
            case "IBM1123": return new IBM1123();
            case "IBM1124": return new IBM1124();
            case "IBM1129": return new IBM1129();
            case "IBM1140": return new IBM1140();
            case "IBM1141": return new IBM1141();
            case "IBM1142": return new IBM1142();
            case "IBM1143": return new IBM1143();
            case "IBM1144": return new IBM1144();
            case "IBM1145": return new IBM1145();
            case "IBM1146": return new IBM1146();
            case "IBM1147": return new IBM1147();
            case "IBM1148": return new IBM1148();
            case "IBM1149": return new IBM1149();
            case "IBM1166": return new IBM1166();
            case "IBM1364": return new IBM1364();
            case "IBM1381": return new IBM1381();
            case "IBM1383": return new IBM1383();
            case "IBM273": return new IBM273();
            case "IBM277": return new IBM277();
            case "IBM278": return new IBM278();
            case "IBM280": return new IBM280();
            case "IBM284": return new IBM284();
            case "IBM285": return new IBM285();
            case "IBM290": return new IBM290();
            case "IBM29626C": return new IBM29626C();
            case "IBM297": return new IBM297();
            case "IBM300": return new IBM300();
            case "IBM33722": return new IBM33722();
            case "IBM420": return new IBM420();
            case "IBM424": return new IBM424();
            case "IBM500": return new IBM500();
            case "IBM833": return new IBM833();
            case "IBM834": return new IBM834();
            case "IBM838": return new IBM838();
            case "IBM856": return new IBM856();
            case "IBM860": return new IBM860();
            case "IBM861": return new IBM861();
            case "IBM863": return new IBM863();
            case "IBM864": return new IBM864();
            case "IBM865": return new IBM865();
            case "IBM868": return new IBM868();
            case "IBM869": return new IBM869();
            case "IBM870": return new IBM870();
            case "IBM871": return new IBM871();
            case "IBM875": return new IBM875();
            case "IBM918": return new IBM918();
            case "IBM921": return new IBM921();
            case "IBM922": return new IBM922();
            case "IBM930": return new IBM930();
            case "IBM933": return new IBM933();
            case "IBM935": return new IBM935();
            case "IBM937": return new IBM937();
            case "IBM939": return new IBM939();
            case "IBM942": return new IBM942();
            case "IBM942C": return new IBM942C();
            case "IBM943": return new IBM943();
            case "IBM943C": return new IBM943C();
            case "IBM948": return new IBM948();
            case "IBM949": return new IBM949();
            case "IBM949C": return new IBM949C();
            case "IBM950": return new IBM950();
            case "IBM964": return new IBM964();
            case "IBM970": return new IBM970();
            case "ISCII91": return new ISCII91();
            case "ISO2022_CN": return new ISO2022_CN();
            case "ISO2022_CN_CNS": return new ISO2022_CN_CNS();
            case "ISO2022_CN_GB": return new ISO2022_CN_GB();
            case "ISO2022_JP": return new ISO2022_JP();
            case "ISO2022_JP_2": return new ISO2022_JP_2();
            case "ISO2022_KR": return new ISO2022_KR();
            case "JISAutoDetect": return new JISAutoDetect();
            case "MS1255": return new MS1255();
            case "MS1256": return new MS1256();
            case "MS1258": return new MS1258();
            case "MS50220": return new MS50220();
            case "MS50221": return new MS50221();
            case "MS874": return new MS874();
            case "MS932_0213": return new MS932_0213();
            case "MS936": return new MS936();
            case "MS949": return new MS949();
            case "MS950": return new MS950();
            case "MS950_HKSCS": return new MS950_HKSCS();
            case "MS950_HKSCS_XP": return new MS950_HKSCS_XP();
            case "MSISO2022JP": return new MSISO2022JP();
            case "MacArabic": return new MacArabic();
            case "MacCentralEurope": return new MacCentralEurope();
            case "MacCroatian": return new MacCroatian();
            case "MacCyrillic": return new MacCyrillic();
            case "MacDingbat": return new MacDingbat();
            case "MacGreek": return new MacGreek();
            case "MacHebrew": return new MacHebrew();
            case "MacIceland": return new MacIceland();
            case "MacRoman": return new MacRoman();
            case "MacRomania": return new MacRomania();
            case "MacSymbol": return new MacSymbol();
            case "MacThai": return new MacThai();
            case "MacTurkish": return new MacTurkish();
            case "MacUkraine": return new MacUkraine();
            case "PCK": return new PCK();
            case "SJIS_0213": return new SJIS_0213();
            default: return null;
        }
    }
}
