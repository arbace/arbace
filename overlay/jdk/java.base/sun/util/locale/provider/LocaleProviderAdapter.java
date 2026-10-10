/*
 * jrt's own sun.util.locale.provider.LocaleProviderAdapter for the Go build (doc/go/JRT-NOTES.md,
 * "Time"). Its Type enum is transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/LocaleProviderAdapter.java, Copyright (c)
 * Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md); the rest is Arbace's own.
 */
package sun.util.locale.provider;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Go build's one locale provider adapter: the CLDR adapter over java.base's own locale
 * data, the root and English locales (the bundles bin/jrt-convert generates as the JDK build
 * does). Every provider class and every locale answers this adapter, whose LocaleResources
 * take a locale's data from English for the English language and from the root locale for
 * the others (JRT-NOTES.md, "Time": the JDK's other locales are jdk.localedata's, a module the
 * Go build does not have).
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

    private static final LocaleProviderAdapter INSTANCE = new Base();

    private final ConcurrentMap<Locale, LocaleResources> localeResources = new ConcurrentHashMap<>();

    LocaleProviderAdapter() {
    }

    /** The adapter for a provider class and a locale: the one adapter. */
    public static LocaleProviderAdapter getAdapter(Class<?> providerClass, Locale locale) {
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

    /** The type of the adapter: CLDR. */
    public Type getAdapterType() {
        return Type.CLDR;
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

    private static final class Base extends LocaleProviderAdapter implements ResourceBundleBasedAdapter {
    }
}
