/*
 * jrt's own sun.util.locale.provider.LocaleProviderAdapter for the Go build (doc/go/JRT-NOTES.md,
 * "Time" and "Text and locales"). Its Type enum, getLocaleServiceProvider, the providers' lazy
 * creation, isSupportedProviderLocale, toLocaleArray and the CLDR adapter's candidate locales
 * (getCandidateLocales, applyAliases, applyParentLocales, getParentLocale, getEquivalentLoc) are
 * transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/LocaleProviderAdapter.java,
 * JRELocaleProviderAdapter.java and src/java.base/share/classes/sun/util/cldr/
 * CLDRLocaleProviderAdapter.java, Copyright (c) Oracle and/or its affiliates, under the GNU
 * General Public License version 2 with the Classpath Exception (LICENSE.md); the rest is
 * Arbace's own.
 */
package sun.util.locale.provider;

import java.text.spi.BreakIteratorProvider;
import java.text.spi.CollatorProvider;
import java.text.spi.DateFormatProvider;
import java.text.spi.DateFormatSymbolsProvider;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.spi.CurrencyNameProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import jdk.internal.jrt.LocaleDataIndex;
import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.cldr.CLDRParentLocales;
import sun.util.resources.LocaleData;

/**
 * The Go build's one locale provider adapter: the CLDR adapter over java.base's own locale data
 * (the root and English locales) and the locales of jdk.localedata that bin/jrt-convert put in
 * the build (jdk.internal.jrt.LocaleDataIndex, JRT_LOCALES), with the JRE adapter's collation and
 * break iterator data. Every type and provider class answers this adapter. Its providers are
 * jdk26u's (DecimalFormatSymbolsProviderImpl, ...); the time zone name, calendar data and
 * calendar name providers are jrt's TimeZoneNameUtility and CalendarDataUtility, which the
 * JDK's java.util and java.time call directly, so getLocaleServiceProvider has none for them.
 */
public abstract class LocaleProviderAdapter {

    /** The adapter types, as the JDK's. */
    public enum Type {
        JRE("sun.util.locale.provider.JRELocaleProviderAdapter", "sun.util.resources", "sun.text.resources"),
        CLDR("sun.util.cldr.CLDRLocaleProviderAdapter", "sun.util.resources.cldr", "sun.text.resources.cldr"),
        SPI("sun.util.locale.provider.SPILocaleProviderAdapter"),
        HOST("sun.util.locale.provider.HostLocaleProviderAdapter"),
        FALLBACK("sun.util.locale.provider.FallbackLocaleProviderAdapter", "sun.util.resources", "sun.text.resources");

        private final String CLASSNAME;
        private final String UTIL_RESOURCES_PACKAGE;
        private final String TEXT_RESOURCES_PACKAGE;

        Type(String className) {
            this(className, null, null);
        }

        Type(String className, String util, String text) {
            CLASSNAME = className;
            UTIL_RESOURCES_PACKAGE = util;
            TEXT_RESOURCES_PACKAGE = text;
        }

        public String getAdapterClassName() {
            return CLASSNAME;
        }

        public String getUtilResourcesPackage() {
            return UTIL_RESOURCES_PACKAGE;
        }

        public String getTextResourcesPackage() {
            return TEXT_RESOURCES_PACKAGE;
        }
    }

    /** java.base's CLDR locales (CLDRBaseLocaleDataMetaInfo.availableLanguageTags), and the root locale, which the JDK's fallback adapter supports. */
    private static final String BASE_TAGS = "und en en-Latn-US en-US en-US-POSIX";

    private static final LocaleProviderAdapter INSTANCE = new Base();

    private static final List<Type> ADAPTER_PREFERENCE = List.of(Type.CLDR);

    private final ConcurrentMap<Locale, LocaleResources> localeResources = new ConcurrentHashMap<>();

    LocaleProviderAdapter() {
    }

    /** The adapter for a provider class and a locale: the one adapter. */
    public static LocaleProviderAdapter getAdapter(Class<? extends LocaleServiceProvider> providerClass, Locale locale) {
        return INSTANCE;
    }

    /** The resource bundle based adapter: the one adapter. */
    public static LocaleProviderAdapter getResourceBundleBased() {
        return INSTANCE;
    }

    /** The JRE adapter: the one adapter. */
    public static LocaleProviderAdapter forJRE() {
        return INSTANCE;
    }

    /** The adapter of a type: the one adapter. */
    public static LocaleProviderAdapter forType(Type type) {
        return INSTANCE;
    }

    /** The adapter preference: the CLDR adapter alone (it has the JRE adapter's data too). */
    public static List<Type> getAdapterPreference() {
        return ADAPTER_PREFERENCE;
    }

    /** The type of the adapter: CLDR. */
    public Type getAdapterType() {
        return Type.CLDR;
    }

    // ---------------------------------------------------------------------------------------
    // the language tags (CLDRLocaleProviderAdapter.createLanguageTagSet, over java.base's and
    // the Go build's jdk.localedata tags), in their order

    private static final class Tags {
        static final Set<String> SET = createLanguageTagSet();
    }

    private static Set<String> createLanguageTagSet() {
        Set<String> set = new LinkedHashSet<>();
        for (String t : (BASE_TAGS + " " + LocaleDataIndex.TAGS).trim().split("\\s+")) {
            set.add(t);
        }
        return Collections.unmodifiableSet(set);
    }

    /** The adapter's language tags (every category's). */
    public Set<String> getLanguageTagSet(String category) {
        return Tags.SET;
    }

    private static volatile Locale[] AVAILABLE_LOCALES;

    public Locale[] getAvailableLocales() {
        Locale[] ls = AVAILABLE_LOCALES;
        if (ls == null) {
            ls = Tags.SET.stream().map(Locale::forLanguageTag).toArray(Locale[]::new);
            AVAILABLE_LOCALES = ls;
        }
        return ls;
    }

    /** Whether the adapter's providers support locale (the CLDR adapter's). */
    public boolean isSupportedProviderLocale(Locale locale, Set<String> langtags) {
        return Locale.ROOT.equals(locale)
                || langtags.contains(locale.stripExtensions().toLanguageTag())
                || langtags.contains(getEquivalentLoc(locale).toLanguageTag());
    }

    /** Whether the adapter supports locale. */
    static boolean isSupportedLocale(Locale locale) {
        return INSTANCE.isSupportedProviderLocale(locale, Tags.SET);
    }

    public static Locale[] toLocaleArray(Set<String> tags) {
        return tags.stream()
            .map(t -> switch (t) {
                case "ja-JP-JP" -> JRELocaleConstants.JA_JP_JP;
                case "no-NO-NY" -> JRELocaleConstants.NO_NO_NY;
                case "th-TH-TH" -> JRELocaleConstants.TH_TH_TH;
                default -> Locale.forLanguageTag(t);
            })
            .distinct()
            .toArray(Locale[]::new);
    }

    // ---------------------------------------------------------------------------------------
    // the providers

    @SuppressWarnings("unchecked")
    public <P extends LocaleServiceProvider> P getLocaleServiceProvider(Class<P> c) {
        switch (c.getSimpleName()) {
        case "BreakIteratorProvider":
            return (P) getBreakIteratorProvider();
        case "CollatorProvider":
            return (P) getCollatorProvider();
        case "DateFormatProvider":
            return (P) getDateFormatProvider();
        case "DateFormatSymbolsProvider":
            return (P) getDateFormatSymbolsProvider();
        case "DecimalFormatSymbolsProvider":
            return (P) getDecimalFormatSymbolsProvider();
        case "NumberFormatProvider":
            return (P) getNumberFormatProvider();
        case "CurrencyNameProvider":
            return (P) getCurrencyNameProvider();
        case "LocaleNameProvider":
            return (P) getLocaleNameProvider();
        case "JavaTimeDateTimePatternProvider":
            return (P) getJavaTimeDateTimePatternProvider();
        default:
            // the time zone names and the calendar data are TimeZoneNameUtility's and
            // CalendarDataUtility's
            return null;
        }
    }

    private volatile BreakIteratorProvider breakIteratorProvider;
    private volatile CollatorProvider collatorProvider;
    private volatile DateFormatProvider dateFormatProvider;
    private volatile DateFormatSymbolsProvider dateFormatSymbolsProvider;
    private volatile DecimalFormatSymbolsProvider decimalFormatSymbolsProvider;
    private volatile NumberFormatProvider numberFormatProvider;
    private volatile CurrencyNameProvider currencyNameProvider;
    private volatile LocaleNameProvider localeNameProvider;

    public BreakIteratorProvider getBreakIteratorProvider() {
        if (breakIteratorProvider == null) {
            breakIteratorProvider = new BreakIteratorProviderImpl(getAdapterType(), Tags.SET);
        }
        return breakIteratorProvider;
    }

    public CollatorProvider getCollatorProvider() {
        if (collatorProvider == null) {
            collatorProvider = new CollatorProviderImpl(getAdapterType(), Tags.SET);
        }
        return collatorProvider;
    }

    public DateFormatProvider getDateFormatProvider() {
        if (dateFormatProvider == null) {
            dateFormatProvider = new DateFormatProviderImpl(getAdapterType(), Tags.SET);
        }
        return dateFormatProvider;
    }

    public DateFormatSymbolsProvider getDateFormatSymbolsProvider() {
        if (dateFormatSymbolsProvider == null) {
            dateFormatSymbolsProvider = new DateFormatSymbolsProviderImpl(getAdapterType(), Tags.SET);
        }
        return dateFormatSymbolsProvider;
    }

    public DecimalFormatSymbolsProvider getDecimalFormatSymbolsProvider() {
        if (decimalFormatSymbolsProvider == null) {
            decimalFormatSymbolsProvider = new DecimalFormatSymbolsProviderImpl(getAdapterType(), Tags.SET);
        }
        return decimalFormatSymbolsProvider;
    }

    public NumberFormatProvider getNumberFormatProvider() {
        if (numberFormatProvider == null) {
            numberFormatProvider = new NumberFormatProviderImpl(getAdapterType(), Tags.SET);
        }
        return numberFormatProvider;
    }

    public CurrencyNameProvider getCurrencyNameProvider() {
        if (currencyNameProvider == null) {
            currencyNameProvider = new CurrencyNameProviderImpl(getAdapterType(), Tags.SET);
        }
        return currencyNameProvider;
    }

    public LocaleNameProvider getLocaleNameProvider() {
        if (localeNameProvider == null) {
            localeNameProvider = new LocaleNameProviderImpl(getAdapterType(), Tags.SET);
        }
        return localeNameProvider;
    }

    /** The provider of java.time's localized patterns (JavaTimeDateTimePatternImpl). */
    public JavaTimeDateTimePatternProvider getJavaTimeDateTimePatternProvider() {
        return JavaTimeDateTimePatternImpl.INSTANCE;
    }

    /** The locale's resources, one instance per locale. */
    public LocaleResources getLocaleResources(Locale locale) {
        LocaleResources lr = localeResources.get(locale);
        if (lr == null) {
            lr = new LocaleResources(locale);
            LocaleResources prev = localeResources.putIfAbsent(locale, lr);
            if (prev != null) {
                lr = prev;
            }
        }
        return lr;
    }

    // ---------------------------------------------------------------------------------------
    // the candidate locales

    /**
     * The candidate locales of a bundle: the CLDR adapter's (ResourceBundle.Control's default
     * list of the locale with CLDR's language aliases applied, then its parent locales;
     * sun.util.cldr.CLDRParentLocales). LocaleData takes the JRE's bundles' (the default list)
     * itself, as the JDK's LocaleData asks the JRE adapter for them.
     */
    public List<Locale> getCandidateLocales(String baseName, Locale locale) {
        return CLDRParentLocales.getCandidateLocales(baseName, locale);
    }

    private static final LocaleData LOCALE_DATA = new LocaleData(Type.CLDR);

    /** The adapter's locale data (ResourceBundleBasedAdapter). */
    public LocaleData getLocaleData() {
        return LOCALE_DATA;
    }

    private static Locale getEquivalentLoc(Locale locale) {
        return CLDRParentLocales.getEquivalentLoc(locale);
    }

    private static final class Base extends LocaleProviderAdapter implements ResourceBundleBasedAdapter {
    }
}
