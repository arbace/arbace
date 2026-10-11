/*
 * jrt's own java.util.ResourceBundle for the Go build (doc/go/JRT-NOTES.md, "Time" and "Text
 * and locales"). Its lookups (getObject, containsKey, keySet, handleKeySet and the key set's
 * computation) and its Control's formats, candidate locales (createCandidateList,
 * getDefaultList), fallback locale and bundle names are transcribed from openjdk/jdk26u at
 * baf63fb, src/java.base/share/classes/java/util/ResourceBundle.java,
 * Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
 * with the Classpath Exception (LICENSE.md); the rest is Arbace's own.
 */
package java.util;

/**
 * java.util.ResourceBundle for the Go build: a bundle's lookups through its parent chain, as
 * the JDK's, for the bundles the Go build has as classes (the CLDR locale data of java.base,
 * and of jdk.localedata, which jrt's locale providers chain themselves), and Control's
 * candidate locales, which the providers use. Not here: getBundle and its loading, caching
 * and Control's loading (newBundle, needsReload: there are no class loaders or modules to load
 * bundles from), and clearCache.
 */
public abstract class ResourceBundle {
    /** The parent bundle, consulted by getObject when this bundle has no value for a key. */
    protected ResourceBundle parent = null;

    private volatile Set<String> handleKeys;

    /** Sole constructor. */
    public ResourceBundle() {
    }

    /** The base name of the bundle: null, as for a bundle not loaded by getBundle. */
    public String getBaseBundleName() {
        return null;
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    public final Object getObject(String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (parent != null) {
                obj = parent.getObject(key);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle "
                                                   + this.getClass().getName()
                                                   + ", key " + key,
                                                   this.getClass().getName(),
                                                   key);
            }
        }
        return obj;
    }

    /** The locale of the bundle: null, as for a bundle not loaded by getBundle. */
    public Locale getLocale() {
        return null;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    protected abstract Object handleGetObject(String key);

    public abstract Enumeration<String> getKeys();

    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            keys.addAll(rb.handleKeySet());
        }
        return keys;
    }

    protected Set<String> handleKeySet() {
        Set<String> keys = handleKeys;
        if (keys == null) {
            keys = new HashSet<>();
            Enumeration<String> enumKeys = getKeys();
            while (enumKeys.hasMoreElements()) {
                String key = enumKeys.nextElement();
                if (handleGetObject(key) != null) {
                    keys.add(key);
                }
            }
            handleKeys = keys;
        }
        return keys;
    }

    /**
     * ResourceBundle.Control: the formats, the candidate locales, the fallback locale and the
     * bundle names, as the JDK's; no loading (newBundle and needsReload are not here).
     */
    public static class Control {
        public static final List<String> FORMAT_DEFAULT
            = List.of("java.class", "java.properties");

        public static final List<String> FORMAT_CLASS = List.of("java.class");

        public static final List<String> FORMAT_PROPERTIES
            = List.of("java.properties");

        public static final long TTL_DONT_CACHE = -1;

        public static final long TTL_NO_EXPIRATION_CONTROL = -2;

        private static final Control INSTANCE = new Control();

        protected Control() {
        }

        public static final Control getControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return SingleFormatControl.PROPERTIES_ONLY;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return SingleFormatControl.CLASS_ONLY;
            }
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return Control.INSTANCE;
            }
            throw new IllegalArgumentException();
        }

        public static final Control getNoFallbackControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return NoFallbackControl.NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return NoFallbackControl.PROPERTIES_ONLY_NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return NoFallbackControl.CLASS_ONLY_NO_FALLBACK;
            }
            throw new IllegalArgumentException();
        }

        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return FORMAT_DEFAULT;
        }

        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return new ArrayList<>(CANDIDATES_CACHE.computeIfAbsent(locale.getBaseLocale(),
                    Control::createCandidateList));
        }

        private static final Map<sun.util.locale.BaseLocale, List<Locale>> CANDIDATES_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

        private static List<Locale> createCandidateList(sun.util.locale.BaseLocale base) {
            String language = base.getLanguage();
            String script = base.getScript();
            String region = base.getRegion();
            String variant = base.getVariant();

            // Special handling for Norwegian
            boolean isNorwegianBokmal = false;
            boolean isNorwegianNynorsk = false;
            if (language.equals("no")) {
                if (region.equals("NO") && variant.equals("NY")) {
                    variant = "";
                    isNorwegianNynorsk = true;
                } else {
                    isNorwegianBokmal = true;
                }
            }
            if (language.equals("nb") || isNorwegianBokmal) {
                List<Locale> tmpList = getDefaultList("nb", script, region, variant);
                // Insert a locale replacing "nb" with "no" for every list entry with precedence
                List<Locale> bokmalList = new ArrayList<>();
                for (Locale l_nb : tmpList) {
                    var isRoot = l_nb.getLanguage().isEmpty();
                    var l_no = Locale.getInstance(isRoot ? "" : "no",
                            l_nb.getScript(), l_nb.getCountry(), l_nb.getVariant(), null);
                    bokmalList.add(isNorwegianBokmal ? l_no : l_nb);
                    if (isRoot) {
                        break;
                    }
                    bokmalList.add(isNorwegianBokmal ? l_nb : l_no);
                }
                return bokmalList;
            } else if (language.equals("nn") || isNorwegianNynorsk) {
                // Insert no_NO_NY, no_NO, no after nn
                List<Locale> nynorskList = getDefaultList("nn", script, region, variant);
                int idx = nynorskList.size() - 1;
                nynorskList.add(idx++, Locale.getInstance("no", "NO", "NY"));
                nynorskList.add(idx++, Locale.getInstance("no", "NO", ""));
                nynorskList.add(idx++, Locale.getInstance("no", "", ""));
                return nynorskList;
            }
            // Special handling for Chinese
            else if (language.equals("zh")) {
                if (script.isEmpty() && !region.isEmpty()) {
                    // Supply script for users who want to use zh_Hans/zh_Hant
                    // as bundle names (recommended for Java7+)
                    switch (region) {
                        case "TW", "HK", "MO" -> script = "Hant";
                        case "CN", "SG"       -> script = "Hans";
                    }
                }
            }

            return getDefaultList(language, script, region, variant);
        }

        private static List<Locale> getDefaultList(String language, String script, String region, String variant) {
            List<String> variants = null;

            if (!variant.isEmpty()) {
                variants = new ArrayList<>();
                int idx = variant.length();
                while (idx != -1) {
                    variants.add(variant.substring(0, idx));
                    idx = variant.lastIndexOf('_', --idx);
                }
            }

            List<Locale> list = new ArrayList<>();

            if (variants != null) {
                for (String v : variants) {
                    list.add(Locale.getInstance(language, script, region, v, null));
                }
            }
            if (!region.isEmpty()) {
                list.add(Locale.getInstance(language, script, region, "", null));
            }
            if (!script.isEmpty()) {
                list.add(Locale.getInstance(language, script, "", "", null));
                // Special handling for Chinese
                if (language.equals("zh")) {
                    if (region.isEmpty()) {
                        // Supply region(country) for users who still package Chinese
                        // bundles using old convention.
                        switch (script) {
                            case "Hans" -> region = "CN";
                            case "Hant" -> region = "TW";
                        }
                    }
                }

                // With script, after truncating variant, region and script,
                // start over without script.
                if (variants != null) {
                    for (String v : variants) {
                        list.add(Locale.getInstance(language, "", region, v, null));
                    }
                }
                if (!region.isEmpty()) {
                    list.add(Locale.getInstance(language, "", region, "", null));
                }
            }
            if (!language.isEmpty()) {
                list.add(Locale.getInstance(language, "", "", "", null));
            }
            // Add root locale at the end
            list.add(Locale.ROOT);

            return list;
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            Locale defaultLocale = Locale.getDefault();
            return locale.equals(defaultLocale) ? null : defaultLocale;
        }

        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return TTL_NO_EXPIRATION_CONTROL;
        }

        public String toBundleName(String baseName, Locale locale) {
            if (locale == Locale.ROOT) {
                return baseName;
            }

            String language = locale.getLanguage();
            String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();

            if (language == "" && country == "" && variant == "") {
                return baseName;
            }

            StringBuilder sb = new StringBuilder(baseName);
            sb.append('_');
            if (script != "") {
                if (variant != "") {
                    sb.append(language).append('_').append(script).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(script).append('_').append(country);
                } else {
                    sb.append(language).append('_').append(script);
                }
            } else {
                if (variant != "") {
                    sb.append(language).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(country);
                } else {
                    sb.append(language);
                }
            }
            return sb.toString();
        }

        public final String toResourceName(String bundleName, String suffix) {
            StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
            sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
            return sb.toString();
        }
    }

    private static class SingleFormatControl extends Control {
        private static final Control PROPERTIES_ONLY
            = new SingleFormatControl(FORMAT_PROPERTIES);

        private static final Control CLASS_ONLY
            = new SingleFormatControl(FORMAT_CLASS);

        private final List<String> formats;

        protected SingleFormatControl(List<String> formats) {
            this.formats = formats;
        }

        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return formats;
        }
    }

    private static final class NoFallbackControl extends SingleFormatControl {
        private static final Control NO_FALLBACK
            = new NoFallbackControl(FORMAT_DEFAULT);

        private static final Control PROPERTIES_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_PROPERTIES);

        private static final Control CLASS_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_CLASS);

        protected NoFallbackControl(List<String> formats) {
            super(formats);
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }
}
