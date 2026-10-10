/*
 * jrt's own sun.util.locale.provider.CalendarDataUtility for the Go build (doc/go/JRT-NOTES.md,
 * "Time"). Its code is transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/CalendarDataUtility.java,
 * CalendarNameProviderImpl.java (the names' resource keys and lookups) and
 * src/java.base/share/classes/sun/util/cldr/CLDRCalendarDataProviderImpl.java (the week
 * parameters by region), Copyright (c) Oracle and/or its affiliates, under the GNU General
 * Public License version 2 with the Classpath Exception (LICENSE.md); the lookup's order over
 * the locale's candidates (LocaleServiceProviderPool's) is Arbace's own.
 */
package sun.util.locale.provider;

import static java.util.Calendar.*;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The calendar data of the Go build's one locale provider adapter (LocaleProviderAdapter): the
 * week parameters by the locale's region, from java.base's CLDR CalendarData, and the names of
 * eras, months, days of the week and AM/PM from the locale's FormatData (LocaleResources), as
 * the JDK's CLDR providers give them.
 */
public class CalendarDataUtility {
    public static final String FIRST_DAY_OF_WEEK = "firstDayOfWeek";
    public static final String MINIMAL_DAYS_IN_FIRST_WEEK = "minimalDaysInFirstWeek";

    private static final Map<String, Integer> firstDay = new ConcurrentHashMap<>();
    private static final Map<String, Integer> minDays = new ConcurrentHashMap<>();

    // No instantiation
    private CalendarDataUtility() {
    }

    public static int retrieveFirstDayOfWeek(Locale locale) {
        int value = findValue(FIRST_DAY_OF_WEEK, findRegionOverride(locale));
        if (value == 0) {
            value = MONDAY; // default for the world ("001")
        }
        return (value >= SUNDAY && value <= SATURDAY) ? value : SUNDAY;
    }

    public static int retrieveMinimalDaysInFirstWeek(Locale locale) {
        int value = findValue(MINIMAL_DAYS_IN_FIRST_WEEK, findRegionOverride(locale));
        if (value == 0) {
            value = 1; // default for the world ("001")
        }
        return (value >= 1 && value <= 7) ? value : 1;
    }

    // CLDRCalendarDataProviderImpl.findValue
    private static int findValue(String key, Locale locale) {
        Map<String, Integer> map = FIRST_DAY_OF_WEEK.equals(key) ? firstDay : minDays;
        String region = locale.getCountry();

        if (region.isEmpty()) {
            // Use "US" as default
            region = "US";
        }

        Integer val = map.get(region);
        if (val == null) {
            String valStr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(Locale.ROOT)
                   .getCalendarData(key);
            val = retrieveInteger(valStr, region)
                .orElse(retrieveInteger(valStr, "001").orElse(0));
            map.putIfAbsent(region, val);
        }

        return val;
    }

    private static Optional<Integer> retrieveInteger(String src, String region) {
        int regionIndex = src.indexOf(region);
        if (regionIndex >= 0) {
            int start = src.lastIndexOf(';', regionIndex) + 1;
            return Optional.of(Integer.parseInt(src, start, src.indexOf(':', start), 10));
        }
        return Optional.empty();
    }

    public static String retrieveFieldValueName(String id, int field, int value, int style, Locale locale) {
        return getDisplayNameImpl(normalizeCalendarType(id), field, value, style, locale, false);
    }

    public static String retrieveJavaTimeFieldValueName(String id, int field, int value, int style, Locale locale) {
        String type = normalizeCalendarType(id);
        String name = getDisplayNameImpl(type, field, value, style, locale, true);
        if (name == null) {
            name = getDisplayNameImpl(type, field, value, style, locale, false);
        }
        return name;
    }

    public static Map<String, Integer> retrieveFieldValueNames(String id, int field, int style, Locale locale) {
        String type = normalizeCalendarType(id);
        Map<String, Integer> names;
        if (style == ALL_STYLES) {
            names = getDisplayNamesImpl(type, field, SHORT_FORMAT, locale, false);
            for (int st : REST_OF_STYLES) {
                names.putAll(getDisplayNamesImpl(type, field, st, locale, false));
            }
        } else {
            names = getDisplayNamesImpl(type, field, style, locale, false);
        }
        return names.isEmpty() ? null : names;
    }

    public static Map<String, Integer> retrieveJavaTimeFieldValueNames(String id, int field, int style, Locale locale) {
        String type = normalizeCalendarType(id);
        Map<String, Integer> map = getDisplayNamesImpl(type, field, style, locale, true);
        if (map.isEmpty()) {
            map = retrieveFieldValueNames(id, field, style, locale);
        }
        return map == null || map.isEmpty() ? null : map;
    }

    /** The locale itself: jrt's locales have no Unicode extensions, so no region override (rg). */
    public static Locale findRegionOverride(Locale l) {
        return l;
    }

    static String normalizeCalendarType(String requestID) {
        String type;
        if (requestID.equals("gregorian") || requestID.equals("iso8601")) {
            type = "gregory";
        } else if (requestID.startsWith("islamic")) {
            type = "islamic";
        } else {
            type = requestID;
        }
        return type;
    }

    // ---------------------------------------------------------------------------------------
    // CalendarNameProviderImpl's lookups, for the CLDR adapter

    private static String getDisplayNameImpl(String calendarType, int field, int value, int style, Locale locale, boolean javatime) {
        String name = null;
        String key = getResourceKeyFor(calendarType, field, style, javatime);
        if (key != null) {
            LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(locale);
            String[] strings = javatime ? lr.getJavaTimeNames(key) : lr.getCalendarNames(key);

            // If standalone names are requested and no "standalone." resources are found,
            // try the default ones instead.
            if (strings == null && key.contains("standalone.")) {
                key = key.replaceFirst("standalone.", "");
                strings = javatime ? lr.getJavaTimeNames(key) : lr.getCalendarNames(key);
            }

            if (strings != null && strings.length > 0) {
                if (field == DAY_OF_WEEK || field == YEAR) {
                    --value;
                }
                if (value < 0 || value >= strings.length) {
                    return null;
                }
                name = strings[value];
                // If name is empty in standalone, try its `format' style.
                if (name.isEmpty()
                        && (style == SHORT_STANDALONE || style == LONG_STANDALONE
                            || style == NARROW_STANDALONE)) {
                    name = getDisplayNameImpl(calendarType, field, value,
                                              getBaseStyle(style),
                                              locale, false);
                }
            }
        }
        return name;
    }

    private static final int[] REST_OF_STYLES = {
        SHORT_STANDALONE, LONG_FORMAT, LONG_STANDALONE,
        NARROW_FORMAT, NARROW_STANDALONE
    };

    private static Map<String, Integer> getDisplayNamesImpl(String calendarType, int field,
                                                            int style, Locale locale, boolean javatime) {
        String key = getResourceKeyFor(calendarType, field, style, javatime);
        Map<String, Integer> map = new TreeMap<>(LengthBasedComparator.INSTANCE);
        if (key != null) {
            LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(locale);
            String[] strings = javatime ? lr.getJavaTimeNames(key) : lr.getCalendarNames(key);

            // If standalone names are requested and no "standalone." resources are found,
            // try the default ones instead.
            if (strings == null && key.contains("standalone.")) {
                key = key.replaceFirst("standalone.", "");
                strings = javatime ? lr.getJavaTimeNames(key) : lr.getCalendarNames(key);
            }

            if (strings != null) {
                if (!hasDuplicates(strings) || field == AM_PM) {
                    if (field == YEAR) {
                        if (strings.length > 0) {
                            map.put(strings[0], 1);
                        }
                    } else {
                        int base = (field == DAY_OF_WEEK) ? 1 : 0;
                        for (int i = strings.length - 1; i >= 0; i--) {
                            String name = strings[i];
                            // Ignore any empty string (some standalone month names
                            // are not defined)
                            if (name.isEmpty()) {
                                continue;
                            }
                            if (field == AM_PM && !javatime && i > PM) {
                                // Skip dayPeriods for java.util.Calendar
                                continue;
                            } else {
                                map.put(name, base + i);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private static int getBaseStyle(int style) {
        return style & ~(SHORT_STANDALONE - SHORT_FORMAT);
    }

    /**
     * Comparator implementation for TreeMap which iterates keys from longest
     * to shortest.
     */
    private static class LengthBasedComparator implements Comparator<String> {
        private static final LengthBasedComparator INSTANCE = new LengthBasedComparator();

        private LengthBasedComparator() {
        }

        @Override
        public int compare(String o1, String o2) {
            int n = o2.length() - o1.length();
            return (n == 0) ? o1.compareTo(o2) : n;
        }
    }

    private static boolean hasDuplicates(String[] strings) {
        int len = strings.length;
        for (int i = 0; i < len - 1; i++) {
            String a = strings[i];
            if (a != null && !a.isEmpty()) {
                for (int j = i + 1; j < len; j++) {
                    if (a.equals(strings[j]))  {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String getResourceKeyFor(String type, int field, int style, boolean javatime) {
        int baseStyle = getBaseStyle(style);
        boolean isStandalone = (style != baseStyle);

        if ("gregory".equals(type)) {
            type = null;
        }
        boolean isNarrow = (baseStyle == NARROW_FORMAT);
        StringBuilder key = new StringBuilder();
        // If javatime is true, use prefix "java.time.".
        if (javatime) {
            key.append("java.time.");
        }
        switch (field) {
        case ERA:
            if (type != null) {
                key.append(type).append('.');
            }
            if (isNarrow) {
                key.append("narrow.");
            } else {
                // the CLDR adapter's
                if (baseStyle == LONG) {
                    key.append("long.");
                }
            }
            key.append("Eras");
            break;

        case YEAR:
            if (!isNarrow) {
                key.append(type).append(".FirstYear");
            }
            break;

        case MONTH:
            if ("islamic".equals(type)) {
                key.append(type).append('.');
            }
            if (isStandalone) {
                key.append("standalone.");
            }
            key.append("Month").append(toStyleName(baseStyle));
            break;

        case DAY_OF_WEEK:
            if (isStandalone) {
                key.append("standalone.");
            }
            key.append("Day").append(toStyleName(baseStyle));
            break;

        case AM_PM:
            if (isNarrow) {
                key.append("narrow.");
            }
            key.append("AmPmMarkers");
            break;
        }
        return key.length() > 0 ? key.toString() : null;
    }

    private static String toStyleName(int baseStyle) {
        switch (baseStyle) {
        case SHORT:
            return "Abbreviations";
        case NARROW_FORMAT:
            return "Narrows";
        }
        return "Names";
    }
}
