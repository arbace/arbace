/*
 * jrt's own sun.util.locale.provider.TimeZoneNameUtility for the Go build (doc/go/JRT-NOTES.md,
 * "Time"). Its code is transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/TimeZoneNameUtility.java (with its
 * TimeZoneNameGetter), TimeZoneNameProviderImpl.java, and
 * src/java.base/share/classes/sun/util/cldr/CLDRTimeZoneNameProviderImpl.java and
 * CLDRLocaleProviderAdapter.java (canonicalTZID), Copyright (c) Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md); the
 * lookup over the locale's candidates (LocaleServiceProviderPool's, for the locales java.base's
 * CLDR adapter supports) is Arbace's own.
 */
package sun.util.locale.provider;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import sun.util.calendar.ZoneInfo;
import sun.util.calendar.ZoneInfoFile;
import sun.util.cldr.CLDRBaseLocaleDataMetaInfo;

/**
 * Time zone names, as the JDK's CLDR time zone name provider gives them, over java.base's CLDR
 * TimeZoneNames and FormatData (LocaleResources): the names of the bundles, then the names the
 * JDK derives where CLDR has none (from the parent locales, the canonical zone, the generic or
 * standard name of a zone without daylight saving time, the region format "{0} Time" over the
 * exemplar city, the GMT format). A locale's names come from the first of its candidate locales
 * (en_US, en, the root locale) java.base's CLDR adapter supports: the root locale, en and en_US.
 */
public final class TimeZoneNameUtility {

    private static final ConcurrentHashMap<Locale, SoftReference<String[][]>> cachedZoneData =
        new ConcurrentHashMap<>();

    private static final Map<String, SoftReference<Map<Locale, String[]>>> cachedDisplayNames =
        new ConcurrentHashMap<>();

    public static String[][] getZoneStrings(Locale locale) {
        String[][] zones;
        SoftReference<String[][]> data = cachedZoneData.get(locale);

        if (data == null || ((zones = data.get()) == null)) {
            zones = loadZoneStrings(locale);
            data = new SoftReference<>(zones);
            cachedZoneData.put(locale, data);
        }

        return zones;
    }

    private static String[][] loadZoneStrings(Locale locale) {
        String[][] zoneStrings = cldrZoneStrings(locale);

        if (zoneStrings.length == 0 && locale.equals(Locale.ROOT)) {
            zoneStrings = getZoneStrings(Locale.ENGLISH);
        }

        return zoneStrings;
    }

    public static String[] retrieveDisplayNames(String id, Locale locale) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(locale);

        return retrieveDisplayNamesImpl(id, locale);
    }

    public static String retrieveGenericDisplayName(String id, int style, Locale locale) {
        String[] names = retrieveDisplayNamesImpl(id, locale);
        if (Objects.nonNull(names)) {
            return names[6 - style];
        } else {
            return null;
        }
    }

    public static String retrieveDisplayName(String id, boolean daylight, int style, Locale locale) {
        String[] names = retrieveDisplayNamesImpl(id, locale);
        if (Objects.nonNull(names)) {
            return names[(daylight ? 4 : 2) - style];
        } else {
            return null;
        }
    }

    public static Optional<String> convertLDMLShortID(String shortID) {
        return canonicalTZID(shortID);
    }

    public static Optional<String> canonicalTZID(String id) {
        return Optional.ofNullable(CanonicalIDs.MAP.get(id));
    }

    private static final class CanonicalIDs {
        static final Map<String, String> MAP = new CLDRBaseLocaleDataMetaInfo().tzCanonicalIDs();
    }

    private static String[] retrieveDisplayNamesImpl(String id, Locale locale) {
        String[] names;
        Map<Locale, String[]> perLocale = null;

        SoftReference<Map<Locale, String[]>> ref = cachedDisplayNames.get(id);
        if (Objects.nonNull(ref)) {
            perLocale = ref.get();
            if (Objects.nonNull(perLocale)) {
                names = perLocale.get(locale);
                if (Objects.nonNull(names)) {
                    return names;
                }
            }
        }

        names = new String[7];
        names[0] = id;
        for (int i = 1; i <= 6; i ++) {
            names[i] = getLocalizedName(locale,
                    i<5 ? (i<3 ? "std" : "dst") : "generic", i%2, id);
        }

        if (Objects.isNull(perLocale)) {
            perLocale = new ConcurrentHashMap<>();
        }
        perLocale.put(locale, names);
        ref = new SoftReference<>(perLocale);
        cachedDisplayNames.put(id, ref);
        return names;
    }

    // LocaleServiceProviderPool.getLocalizedObject: the name from the first candidate locale
    // the adapter supports that has one
    private static String getLocalizedName(Locale locale, String requestID, int style, String tzid) {
        for (Locale current : LocaleResources.candidateLocales(locale)) {
            if (LocaleResources.isSupportedLocale(current)) {
                String value = getObject(current, requestID, style, tzid);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    // TimeZoneNameGetter.getObject
    private static String getObject(Locale locale, String requestID, int style, String tzid) {
        String value = getName(locale, requestID, style, tzid);
        if (value == null) {
            Map<String, String> aliases = ZoneInfo.getAliasTable();
            if (aliases != null) {
                String canonicalID = aliases.get(tzid);
                if (canonicalID != null) {
                    value = getName(locale, requestID, style, canonicalID);
                }
                if (value == null) {
                    value = examineAliases(locale, requestID,
                                 canonicalID != null ? canonicalID : tzid, style, aliases);
                }
            }
        }

        return value;
    }

    private static String examineAliases(Locale locale, String requestID, String tzid, int style,
                                         Map<String, String> aliases) {
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (entry.getValue().equals(tzid)) {
                String alias = entry.getKey();
                String name = getName(locale, requestID, style, alias);
                if (name != null) {
                    return name;
                }
                name = examineAliases(locale, requestID, alias, style, aliases);
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String getName(Locale locale, String requestID, int style, String tzid) {
        String value = null;
        switch (requestID) {
        case "std":
            value = getDisplayName(tzid, false, style, locale);
            break;
        case "dst":
            value = getDisplayName(tzid, true, style, locale);
            break;
        case "generic":
            value = getGenericDisplayName(tzid, style, locale);
            break;
        }
        return value;
    }

    // ---------------------------------------------------------------------------------------
    // TimeZoneNameProviderImpl and CLDRTimeZoneNameProviderImpl

    private static final String NO_INHERITANCE_MARKER = "∅∅∅";
    private static class AVAILABLE_IDS {
        static final String[] INSTANCE = sortedZoneIds();
    }

    // ZoneInfoFile.zoneIds().sorted().toArray(String[]::new), sorted by Arrays.sort
    private static String[] sortedZoneIds() {
        String[] ids = ZoneInfoFile.zoneIds().toArray(String[]::new);
        Arrays.sort(ids);
        return ids;
    }

    // name indexes
    private static final int INDEX_TZID         = 0;
    private static final int INDEX_STD_LONG     = 1;
    private static final int INDEX_STD_SHORT    = 2;
    private static final int INDEX_DST_LONG     = 3;
    private static final int INDEX_DST_SHORT    = 4;
    private static final int INDEX_GEN_LONG     = 5;
    private static final int INDEX_GEN_SHORT    = 6;

    private static String getDisplayName(String id, boolean daylight, int style, Locale locale) {
        String[] names = getDisplayNameArray(id, locale);
        if (Objects.nonNull(names)) {
            int index = daylight ? 3 : 1;
            if (style == java.util.TimeZone.SHORT) {
                index++;
            }
            return names[index];
        }
        return null;
    }

    private static String getGenericDisplayName(String id, int style, Locale locale) {
        String[] names = getDisplayNameArray(id, locale);
        if (Objects.nonNull(names)) {
            return names[(style == java.util.TimeZone.LONG) ? 5 : 6];
        }
        return null;
    }

    // TimeZoneNameProviderImpl.getDisplayNameArray
    private static String[] bundleDisplayNameArray(String id, Locale locale) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(locale);

        return (String []) LocaleProviderAdapter.getResourceBundleBased()
            .getLocaleResources(locale)
            .getTimeZoneNames(id);
    }

    // CLDRTimeZoneNameProviderImpl.getDisplayNameArray
    private static String[] getDisplayNameArray(String id, Locale locale) {
        String[] namesSuper = bundleDisplayNameArray(id, locale);

        if (namesSuper == null) {
            // try canonical id instead
            namesSuper = bundleDisplayNameArray(
                canonicalTZID(id).orElse(id),
                locale);
        }

        if (namesSuper != null) {
            namesSuper[INDEX_TZID] = id;

            // Check if standard long name exists. If not, try to retrieve the name
            // from language only locale resources. E.g., "Europe/London"
            // for en-GB only contains DST names
            for(int i = INDEX_STD_LONG; i < namesSuper.length; i++) { // index 0 is the 'id' itself
                switch (namesSuper[i]) {
                case "":
                    // Fill in empty elements
                    deriveFallbackName(namesSuper, i, locale,
                                       isFixedOffset(id));
                    break;
                case NO_INHERITANCE_MARKER:
                    // CLDR's "no inheritance marker"
                    namesSuper[i] = toGMTFormat(id,
                                                i == INDEX_DST_LONG || i == INDEX_DST_SHORT,
                                                locale);
                    break;
                default:
                    break;
                }
            }
            return namesSuper;
        } else {
            // Derive the names for this id. Validate the id first
            if (Arrays.binarySearch(AVAILABLE_IDS.INSTANCE, id) >= 0) {
                String[] names = new String[INDEX_GEN_SHORT + 1];
                names[INDEX_TZID] = id;
                deriveFallbackNames(names, locale);
                return names;
            }
        }

        return null;
    }

    // CLDRTimeZoneNameProviderImpl.getZoneStrings
    private static String[][] cldrZoneStrings(Locale locale) {
        String[][] ret = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(locale).getZoneStrings();

        for (int zoneIndex = 0; zoneIndex < ret.length; zoneIndex++) {
            deriveFallbackNames(ret[zoneIndex], locale);
        }
        return ret;
    }

    // Derive fallback time zone name according to LDML's logic
    private static void deriveFallbackNames(String[] names, Locale locale) {
        boolean noDST = isFixedOffset(names[0]);

        for (int i = INDEX_STD_LONG; i <= INDEX_GEN_SHORT; i++) {
            deriveFallbackName(names, i, locale, noDST);
        }
    }

    private static void deriveFallbackName(String[] names, int index, Locale locale, boolean noDST) {
        String id = names[INDEX_TZID];

        if (exists(names, index)) {
            if (names[index].equals(NO_INHERITANCE_MARKER)) {
                // CLDR's "no inheritance marker"
                names[index] = toGMTFormat(id,
                                    index == INDEX_DST_LONG || index == INDEX_DST_SHORT,
                                    locale);
            }
            return;
        }

        // Check parent locales first
        if (!exists(names, index)) {
            var cands = LocaleResources.candidateLocales(locale);
            for (int i = 1; i < cands.size() ; i++) {
                var loc = cands.get(i);
                String[] parentNames = bundleDisplayNameArray(id, loc);
                if (parentNames != null && !parentNames[index].isEmpty()) {
                    // Long names in the root locale are not used.
                    if (!loc.equals(Locale.ROOT) || index % 2 == 0) {
                        names[index] = parentNames[index];
                        return;
                    }
                }
            }
        }

        // Check if COMPAT can substitute the name
        var canonName =
            canonicalTZID(id).map(canonId -> getDisplayNameArray(canonId, locale)[index]);
        if (canonName.isPresent()) {
            names[index] = canonName.get();
            return;
        }

        // Type Fallback
        if (noDST && typeFallback(names, index)) {
            return;
        }

        // Region Fallback
        if (regionFormatFallback(names, index, locale)) {
            return;
        }

        // last resort
        names[index] = toGMTFormat(id,
                                   index == INDEX_DST_LONG || index == INDEX_DST_SHORT,
                                   locale);
        // aliases of "GMT" timezone.
        if ((exists(names, INDEX_STD_LONG)) && (id.startsWith("Etc/")
                || id.startsWith("GMT") || id.startsWith("Greenwich"))) {
            switch (id) {
            case "Etc/GMT":
            case "Etc/GMT-0":
            case "Etc/GMT+0":
            case "Etc/GMT0":
            case "GMT+0":
            case "GMT-0":
            case "GMT0":
            case "Greenwich":
                names[INDEX_DST_LONG] = names[INDEX_GEN_LONG] = names[INDEX_STD_LONG];
                break;
            }
        }
    }

    private static boolean exists(String[] names, int index) {
        return Objects.nonNull(names)
                && Objects.nonNull(names[index])
                && !names[index].isEmpty();
    }

    private static boolean typeFallback(String[] names, int index) {
        // check generic
        int genIndex = INDEX_GEN_SHORT - index % 2;
        if (!exists(names, index) && exists(names, genIndex) && !names[genIndex].startsWith("GMT")) {
            names[index] = names[genIndex];
        } else {
            // check standard
            int stdIndex = INDEX_STD_SHORT - index % 2;
            if (!exists(names, index) && exists(names, stdIndex) && !names[stdIndex].startsWith("GMT")) {
                names[index] = names[stdIndex];
            }
        }

        return exists(names, index);
    }

    private static boolean regionFormatFallback(String[] names, int index, Locale l) {
        if (index % 2 == 0) {
            // ignore short names
            return false;
        }

        String id = names[INDEX_TZID];
        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(l);
        ResourceBundle fd = lr.getJavaTimeFormatData();

        id = canonicalTZID(id).orElse(id);
        String rgn = (String) lr.getTimeZoneNames("timezone.excity." + id);
        if (rgn == null && !id.startsWith("Etc") && !id.startsWith("SystemV")) {
            int slash = id.lastIndexOf('/');
            if (slash > 0) {
                rgn = id.substring(slash + 1).replaceAll("_", " ");
            }
        }

        if (rgn != null) {
            String fmt = "";
            switch (index) {
            case INDEX_STD_LONG:
                fmt = fd.getString("timezone.regionFormat.standard");
                break;
            case INDEX_DST_LONG:
                fmt = fd.getString("timezone.regionFormat.daylight");
                break;
            case INDEX_GEN_LONG:
                fmt = fd.getString("timezone.regionFormat");
                break;
            }
            if (!fmt.isEmpty()) {
                names[index] = LocaleResources.messageFormat(fmt, rgn);
            }
        }

        return exists(names, index);
    }

    private static String toGMTFormat(String id, boolean daylight, Locale l) {
        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(l);
        ResourceBundle fd = lr.getJavaTimeFormatData();
        var zi = ZoneInfoFile.getZoneInfo(id);
        if (zi == null) {
            return fd.getString("timezone.gmtZeroFormat");
        }
        var zr = zi.toZoneId().getRules();
        var now = Instant.now();
        var saving = zr.getTransitions().reversed().stream()
                .dropWhile(zot -> zot.getInstant().isAfter(now))
                .filter(zot -> zr.isDaylightSavings(zot.getInstant()))
                .findFirst()
                .map(zot -> zr.getDaylightSavings(zot.getInstant()))
                .map(Duration::getSeconds)
                .map(Long::intValue)
                .orElse(0);
        int offset = (zr.getStandardOffset(now).getTotalSeconds() +
                (daylight ? saving : 0)) / 60;

        if (offset == 0) {
            return fd.getString("timezone.gmtZeroFormat");
        } else {
            String gmtFormat = fd.getString("timezone.gmtFormat");
            String hourFormat = fd.getString("timezone.hourFormat");

            if (offset > 0) {
                hourFormat = hourFormat.substring(0, hourFormat.indexOf(";"));
            } else {
                hourFormat = hourFormat.substring(hourFormat.indexOf(";") + 1);
                offset = -offset;
            }
            hourFormat = hourFormat
                .replaceFirst("H+", "\\%1\\$02d")
                .replaceFirst("m+", "\\%2\\$02d");
            return LocaleResources.messageFormat(gmtFormat,
                    String.format(l, hourFormat, offset / 60, offset % 60));
        }
    }

    private static boolean isFixedOffset(String id) {
        var zi = ZoneInfo.getTimeZone(id);
        return zi == null || zi.toZoneId().getRules().isFixedOffset();
    }

    private TimeZoneNameUtility() {
    }
}
