/*
 * jrt's own sun.util.locale.provider.ResourceBundleBasedAdapter (doc/go/JRT-NOTES.md, "Time"
 * and "Text and locales"): its two methods are jdk26u's (openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/ResourceBundleBasedAdapter.java,
 * Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
 * with the Classpath Exception, LICENSE.md).
 */
package sun.util.locale.provider;

import java.util.List;
import java.util.Locale;
import sun.util.resources.LocaleData;

/**
 * An adapter whose data are resource bundles, which jrt's one adapter (LocaleProviderAdapter)
 * is: its locale data (jrt's LocaleData) and its candidate locales.
 */
public interface ResourceBundleBasedAdapter {
    public LocaleData getLocaleData();

    /**
     * candidate locales customization
     */
    public List<Locale> getCandidateLocales(String baseName, Locale locale);
}
