/*
 * jrt's own sun.util.resources.LocaleData for the Go build (doc/go/JRT-NOTES.md, "Text and
 * locales"). Its API is jdk26u's (openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/resources/LocaleData.java, Copyright (c) Oracle and/or
 * its affiliates, under the GNU General Public License version 2 with the Classpath Exception,
 * LICENSE.md); the bundles' lookup is Arbace's own.
 */
package sun.util.resources;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.LocaleProviderAdapter;

/**
 * The locale data bundles of a locale, as the JDK's LocaleData gives them: a locale's bundle of
 * a kind (FormatData, TimeZoneNames, CurrencyNames, LocaleNames, CalendarData of CLDR;
 * CollationData, BreakIteratorInfo, BreakIteratorResources of the JRE) is the chain of the
 * bundles of its candidate locales that have one, down to the root locale's (ResourceBundle.
 * getBundle's bundle and its parents, as one bundle: a key's value is the first bundle's that
 * has the key). The candidate locales are the CLDR adapter's for CLDR's bundles (CLDR's parent
 * locales) and ResourceBundle.Control's for the JRE's. The bundles are the classes the Go build
 * has: java.base's CLDR data (the root and English locales), the JRE's root data, and
 * jdk.localedata's for the locales bin/jrt-convert selected (jdk.internal.jrt.LocaleDataIndex). One
 * instance of each bundle, made at its first use. Deviation: the typed getters return
 * ResourceBundle (the chain), not OpenListResourceBundle or TimeZoneNamesBundle.
 */
public class LocaleData {
    private static final String CLDR_TEXT = "sun.text.resources.cldr";
    private static final String CLDR_UTIL = "sun.util.resources.cldr";
    private static final String JRE_TEXT = "sun.text.resources";

    // one bundle per name ("FormatData_fr"), and one chain per base name and locale
    private static final ConcurrentMap<String, Object> BUNDLES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ResourceBundle> CHAINS = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    public LocaleData(LocaleProviderAdapter.Type type) {
    }

    public ResourceBundle getCalendarData(Locale locale) {
        return getBundle(CLDR_UTIL + ".CalendarData", locale);
    }

    public ResourceBundle getCurrencyNames(Locale locale) {
        return getBundle(CLDR_UTIL + ".CurrencyNames", locale);
    }

    public ResourceBundle getLocaleNames(Locale locale) {
        return getBundle(CLDR_UTIL + ".LocaleNames", locale);
    }

    public ResourceBundle getTimeZoneNames(Locale locale) {
        return getBundle(CLDR_UTIL + ".TimeZoneNames", locale);
    }

    public ResourceBundle getBreakIteratorInfo(Locale locale) {
        return getBundle(JRE_TEXT + ".BreakIteratorInfo", locale);
    }

    public ResourceBundle getBreakIteratorResources(Locale locale) {
        return getBundle(JRE_TEXT + ".BreakIteratorResources", locale);
    }

    public ResourceBundle getCollationData(Locale locale) {
        return getBundle(JRE_TEXT + ".CollationData", locale);
    }

    public ResourceBundle getDateFormatData(Locale locale) {
        return getBundle(CLDR_TEXT + ".FormatData", locale);
    }

    public ResourceBundle getNumberFormatData(Locale locale) {
        return getBundle(CLDR_TEXT + ".FormatData", locale);
    }

    /** The chain of baseName's bundles of locale's candidate locales. */
    public static ResourceBundle getBundle(String baseName, Locale locale) {
        String key = baseName + "-" + locale.toLanguageTag() + "-" + locale;
        ResourceBundle chain = CHAINS.get(key);
        if (chain == null) {
            List<ResourceBundle> bundles = new ArrayList<>();
            for (Locale l : candidates(baseName, locale)) {
                ResourceBundle b = bundle(baseName, l);
                if (b != null) {
                    bundles.add(b);
                }
            }
            if (bundles.isEmpty()) {
                throw new MissingResourceException("Can't find bundle for base name "
                    + baseName + ", locale " + locale, baseName + "_" + locale, "");
            }
            chain = new Chain(bundles.toArray(new ResourceBundle[0]));
            ResourceBundle prev = CHAINS.putIfAbsent(key, chain);
            if (prev != null) {
                chain = prev;
            }
        }
        return chain;
    }

    /**
     * The candidate locales of a bundle: the CLDR adapter's (with CLDR's parent locales) for
     * CLDR's bundles, ResourceBundle.Control's default list for the JRE's.
     */
    public static List<Locale> candidates(String baseName, Locale locale) {
        if (baseName.contains(".cldr")) {
            return LocaleProviderAdapter.forType(LocaleProviderAdapter.Type.CLDR)
                .getCandidateLocales(baseName, locale);
        }
        return ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .getCandidateLocales(baseName, locale);
    }

    /** The bundle of baseName of exactly this locale, or null. */
    private static ResourceBundle bundle(String baseName, Locale locale) {
        String simple = baseName.substring(baseName.lastIndexOf('.') + 1);
        String suffix = suffix(locale);
        String name = suffix.isEmpty() ? simple : simple + "_" + suffix;
        String key = baseName.contains(".cldr") ? name : "jre:" + name;
        Object b = BUNDLES.get(key);
        if (b == null) {
            ResourceBundle rb = newBundle(key);
            b = rb == null ? NONE : rb;
            Object prev = BUNDLES.putIfAbsent(key, b);
            if (prev != null) {
                b = prev;
            }
        }
        return b == NONE ? null : (ResourceBundle) b;
    }

    /** ResourceBundle.Control.toBundleName's suffix: language_Script_COUNTRY_variant. */
    private static String suffix(Locale locale) {
        String bundleName = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .toBundleName("", locale);
        return bundleName.isEmpty() ? "" : bundleName.substring(1);
    }

    /** A new bundle of the key: java.base's and the JRE's by name, the others the index's. */
    private static ResourceBundle newBundle(String key) {
        switch (key) {
            case "FormatData": return new sun.text.resources.cldr.FormatData();
            case "FormatData_en": return new sun.text.resources.cldr.FormatData_en();
            case "TimeZoneNames": return new sun.util.resources.cldr.TimeZoneNames();
            case "TimeZoneNames_en": return new sun.util.resources.cldr.TimeZoneNames_en();
            case "CurrencyNames": return new sun.util.resources.cldr.CurrencyNames();
            case "CurrencyNames_en": return new sun.util.resources.cldr.CurrencyNames_en();
            case "LocaleNames": return new sun.util.resources.cldr.LocaleNames();
            case "LocaleNames_en": return new sun.util.resources.cldr.LocaleNames_en();
            case "CalendarData": return new sun.util.resources.cldr.CalendarData();
            case "jre:CollationData": return new sun.text.resources.CollationData();
            case "jre:BreakIteratorInfo": return new sun.text.resources.BreakIteratorInfo();
            case "jre:BreakIteratorResources": return new sun.text.resources.BreakIteratorResources();
            default:
                if (key.startsWith("CalendarData_") || key.startsWith("jre:BreakIterator")) {
                    // CalendarData is the root locale's alone; the break iterators' data are
                    // the root locale's but Thai's (not in the Go build)
                    return null;
                }
                return jdk.internal.jrt.LocaleDataIndex.bundle(key);
        }
    }

    /**
     * A chain of bundles as one bundle: a key's value is the first bundle's that has the key
     * (as a bundle's parent chain); the keys are the bundles', in their order.
     */
    private static final class Chain extends ResourceBundle {
        private final ResourceBundle[] chain;
        private volatile Set<String> keys;

        Chain(ResourceBundle[] chain) {
            this.chain = chain;
        }

        @Override
        protected Object handleGetObject(String key) {
            for (ResourceBundle b : chain) {
                if (b.containsKey(key)) {
                    return b.getObject(key);
                }
            }
            return null;
        }

        @Override
        protected Set<String> handleKeySet() {
            Set<String> ks = keys;
            if (ks == null) {
                ks = new LinkedHashSet<>();
                for (ResourceBundle b : chain) {
                    for (Enumeration<String> e = b.getKeys(); e.hasMoreElements(); ) {
                        ks.add(e.nextElement());
                    }
                }
                keys = ks;
            }
            return ks;
        }

        @Override
        public Enumeration<String> getKeys() {
            Iterator<String> it = handleKeySet().iterator();
            return new Enumeration<String>() {
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                public String nextElement() {
                    return it.next();
                }
            };
        }
    }
}
