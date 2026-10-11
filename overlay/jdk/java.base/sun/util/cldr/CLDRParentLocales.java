/*
 * jrt's own sun.util.cldr.CLDRParentLocales for the Go build (doc/go/JRT-NOTES.md, "Text and
 * locales"): the CLDR adapter's candidate locales. Its code is transcribed from
 * openjdk/jdk26u at baf63fb, src/java.base/share/classes/sun/util/cldr/
 * CLDRLocaleProviderAdapter.java (getCandidateLocales, applyAliases, applyParentLocales,
 * getParentLocale, getEquivalentLoc), Copyright (c) Oracle and/or its affiliates, under the GNU
 * General Public License version 2 with the Classpath Exception (LICENSE.md). It is in the
 * JDK's package for CLDRBaseLocaleDataMetaInfo's package-private members, as the JDK's adapter.
 */
package sun.util.cldr;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The CLDR adapter's candidate locales of a locale: ResourceBundle.Control's default list of
 * the locale with CLDR's language aliases applied, with CLDR's parent locales.
 */
public final class CLDRParentLocales {
    private static final Map<Locale, Locale> parentLocalesMap = new ConcurrentHashMap<>();
    private static final Map<Locale, Locale> langAliasesCache = new ConcurrentHashMap<>();
    private static final CLDRBaseLocaleDataMetaInfo baseMetaInfo = new CLDRBaseLocaleDataMetaInfo();

    static {
        // Assuming these locales do NOT have irregular parent locales.
        parentLocalesMap.put(Locale.ROOT, Locale.ROOT);
        parentLocalesMap.put(Locale.ENGLISH, Locale.ENGLISH);
        parentLocalesMap.put(Locale.US, Locale.US);
    }

    private CLDRParentLocales() {
    }

    public static List<Locale> getCandidateLocales(String baseName, Locale locale) {
        List<Locale> candidates = defaultCandidates(baseName, applyAliases(locale));
        return applyParentLocales(baseName, candidates);
    }

    private static List<Locale> defaultCandidates(String baseName, Locale locale) {
        return ResourceBundle.Control
            .getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .getCandidateLocales(baseName, locale);
    }

    private static Locale applyAliases(Locale loc) {
        return langAliasesCache.computeIfAbsent(loc, l -> {
            var alias = baseMetaInfo.getLanguageAliasMap().get(l.toLanguageTag());
            return alias != null ? Locale.forLanguageTag(alias) : l;
        });
    }

    private static final Locale NB = Locale.forLanguageTag("nb");
    private static final Locale NO = Locale.forLanguageTag("no");
    private static List<Locale> applyParentLocales(String baseName, List<Locale> candidates) {
        // check irregular parents
        for (int i = 0; i < candidates.size(); i++) {
            Locale l = candidates.get(i);
            if (!l.equals(Locale.ROOT)) {
                Locale p = getParentLocale(l);
                if (p != null &&
                    !candidates.get(i+1).equals(p)) {
                    List<Locale> applied = candidates.subList(0, i+1);
                    // Tweak for Norwegian locales, CLDR switched the canonical form of
                    // Norwegian Bokmal language code from "nb" to "no" in CLDR 39
                    // (https://unicode-org.atlassian.net/browse/CLDR-2698)
                    if (p.equals(NB) || p.equals(NO)) {
                        applied.add(NO);
                        applied.add(Locale.ROOT);
                    } else {
                        applied.addAll(applyParentLocales(baseName, defaultCandidates(baseName, p)));
                    }
                    return applied;
                }
            }
        }

        return candidates;
    }

    private static Locale getParentLocale(Locale locale) {
        Locale parent = parentLocalesMap.get(locale);

        if (parent == null) {
            String tag = locale.toLanguageTag();
            for (Map.Entry<Locale, String[]> entry : baseMetaInfo.parentLocales().entrySet()) {
                if (Arrays.binarySearch(entry.getValue(), tag) >= 0) {
                    parent = entry.getKey();
                    break;
                }
            }

            if (parent == null) {
                // check nonlikelyScript locales
                if (CLDRBaseLocaleDataMetaInfo.nonlikelyScript && locale.getCountry().isEmpty()) {
                    var lang = " " + locale.getLanguage() + " ";
                    var script= locale.getScript();
                    if (!script.isEmpty()) {
                        parent = baseMetaInfo.likelyScriptMap().entrySet().stream()
                            .filter(e -> e.getValue().contains(lang))
                            .findAny()
                            .map(Map.Entry::getKey)
                            .map(likely -> likely.equals(script) ? null : Locale.ROOT)
                            .orElse(null);
                    }
                }
            }

            // no parent found
            if (parent == null) {
                parent = locale; // non existent marker
            }
            parentLocalesMap.putIfAbsent(locale, parent);
        }

        if (locale.equals(parent)) {
            // means no irregular parent.
            parent = null;
        }

        return parent;
    }

    public static Locale getEquivalentLoc(Locale locale) {
        return switch (locale.toString()) {
            case "no", "no_NO" -> Locale.forLanguageTag("nb");
            default -> applyAliases(locale);
        };
    }
}
