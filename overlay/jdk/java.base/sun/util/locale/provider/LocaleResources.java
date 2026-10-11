/*
 * jrt's own sun.util.locale.provider.LocaleResources for the Go build (doc/go/JRT-NOTES.md,
 * "Time" and "Text and locales"). Its lookups (getBreakIteratorInfo, getBreakIteratorResources,
 * getCalendarData, getCollationData, getDecimalFormatSymbolsData, getNumberPatterns and
 * getNumberStrings, getCNPatterns, getCurrencyName, getLocaleName, getTimeZoneNames,
 * getZoneIDs, getZoneStrings, getCalendarNames, getJavaTimeNames, getDateTimePattern and
 * getJavaTimeDateTimePattern, getLocalizedPattern with its skeleton matching, getRules,
 * getListPatterns) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/LocaleResources.java, Copyright (c) Oracle
 * and/or its affiliates, under the GNU General Public License version 2 with the Classpath
 * Exception (LICENSE.md); the caching (strong, where the JDK's are soft references) and the
 * MessageFormat subset are Arbace's own.
 */
package sun.util.locale.provider;

import java.text.ListFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import sun.util.resources.LocaleData;

/**
 * A locale's resources, as the JDK's CLDR adapter has them, over the locale data of the Go
 * build (jrt's LocaleData: java.base's CLDR bundles, the JRE's root data, and the bundles of
 * jdk.localedata's locales bin/jrt-convert selected), each the chain of the locale's candidate
 * locales' bundles. Values are cached; they are not to be changed by callers (as the JDK's).
 */
public class LocaleResources {

    private final Locale locale;
    private final LocaleData localeData;
    private final ConcurrentMap<String, Object> cache = new ConcurrentHashMap<>();

    // cache key prefixes
    private static final String BREAK_ITERATOR_INFO = "BII.";
    private static final String CALENDAR_DATA_KEY = "CALD.";
    private static final String COLLATION_DATA = "COLD";
    private static final String DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY = "DFSD";
    private static final String CURRENCY_NAMES = "CN.";
    private static final String LOCALE_NAMES = "LN.";
    private static final String TIME_ZONE_NAMES = "TZN.";
    private static final String ZONE_IDS_CACHEKEY = "ZID";
    private static final String CALENDAR_NAMES = "CALN.";
    private static final String NUMBER_PATTERNS_CACHEKEY = "NP";
    private static final String COMPACT_NUMBER_PATTERNS_CACHEKEY = "CNP";
    private static final String DATE_TIME_PATTERN = "DTP.";
    private static final String RULES_CACHEKEY = "RULE";
    private static final String SKELETON_PATTERN = "SP.";
    private static final String LIST_PATTERN = "LP.";

    // ResourceBundle key names for skeletons
    private static final String SKELETON_INPUT_REGIONS_KEY = "DateFormatItemInputRegions";

    // TimeZoneNamesBundle exemplar city prefix
    private static final String TZNB_EXCITY_PREFIX = "timezone.excity.";

    // null singleton cache value
    private static final Object NULLOBJECT = new Object();

    // RegEx pattern for skeleton validity checking
    private static final Pattern VALID_SKELETON_PATTERN = Pattern.compile(
        "(?<date>" +
        "G{0,5}" +        // Era
        "y*" +            // Year
        "Q{0,5}" +        // Quarter
        "M{0,5}" +        // Month
        "w*" +            // Week of Week Based Year
        "E{0,5}" +        // Day of Week
        "d{0,2})" +       // Day of Month
        "(?<time>" +
        "B{0,5}" +        // Period/AmPm of Day
        "[hHjC]{0,2}" +   // Hour of Day/AmPm
        "m{0,2}" +        // Minute of Hour
        "s{0,2}" +        // Second of Minute
        "[vz]{0,4})");    // Zone

    // Input Skeleton map for "preferred" and "allowed"
    // Map<"preferred"/"allowed", Map<"region", "skeleton">>
    private static Map<String, Map<String, String>> inputSkeletons;

    // Skeletons for "j" and "C" input skeleton symbols for this locale
    private String jPattern;
    private String CPattern;

    LocaleResources(Locale locale) {
        this.locale = locale;
        this.localeData = LocaleProviderAdapter.getResourceBundleBased().getLocaleData();
    }

    /** The locale whose resources these are. */
    Locale getLocale() {
        return locale;
    }

    public String getCalendarData(String key) {
        String cacheKey = CALENDAR_DATA_KEY + key;
        Object data = cache.get(cacheKey);
        if (data == null) {
            String caldata = "";
            ResourceBundle rb = localeData.getCalendarData(locale);
            if (rb.containsKey(key)) {
                caldata = rb.getString(key);
            }
            cache.putIfAbsent(cacheKey, caldata);
            data = caldata;
        }
        return (String) data;
    }

    public String[] getNumberPatterns() {
        Object data = cache.get(NUMBER_PATTERNS_CACHEKEY);
        if (data == null) {
            data = getNumberStrings(localeData.getNumberFormatData(locale), "NumberPatterns");
            cache.putIfAbsent(NUMBER_PATTERNS_CACHEKEY, data);
        }
        return (String[]) data;
    }

    private String[] getNumberStrings(ResourceBundle rb, String type) {
        String[] ret = null;
        String key;
        String numSys;

        // Number strings look up. First, try the Unicode extension
        numSys = locale.getUnicodeLocaleType("nu");
        if (numSys != null) {
            key = numSys + "." + type;
            if (rb.containsKey(key)) {
                ret = rb.getStringArray(key);
            }
        }

        // Next, try DefaultNumberingSystem value
        if (ret == null && rb.containsKey("DefaultNumberingSystem")) {
            key = rb.getString("DefaultNumberingSystem") + "." + type;
            if (rb.containsKey(key)) {
                ret = rb.getStringArray(key);
            }
        }

        // Last resort. No need to check the availability.
        // Just let it throw MissingResourceException when needed.
        if (ret == null) {
            ret = rb.getStringArray(type);
        }

        return ret;
    }

    public Object getTimeZoneNames(String key) {
        String cacheKey = TIME_ZONE_NAMES + key;
        Object val = cache.get(cacheKey);
        if (val == null) {
            ResourceBundle tznb = localeData.getTimeZoneNames(locale);
            if (key.startsWith(TZNB_EXCITY_PREFIX)) {
                if (tznb.containsKey(key)) {
                    val = tznb.getString(key);
                }
            } else {
                String[] names = null;
                if (tznb.containsKey(key)) {
                    names = tznb.getStringArray(key);
                } else {
                    var tz = TimeZoneNameUtility.canonicalTZID(key).orElse(key);
                    if (tznb.containsKey(tz)) {
                        names = tznb.getStringArray(tz);
                    }
                }

                if (names != null) {
                    names[0] = key;
                    val = names;
                }
            }
            if (val != null) {
                cache.putIfAbsent(cacheKey, val);
            }
        }

        return val;
    }

    @SuppressWarnings("unchecked")
    Set<String> getZoneIDs() {
        Object data = cache.get(ZONE_IDS_CACHEKEY);
        if (data == null) {
            data = localeData.getTimeZoneNames(locale).keySet();
            cache.putIfAbsent(ZONE_IDS_CACHEKEY, data);
        }
        return (Set<String>) data;
    }

    // zoneStrings are cached separately in TimeZoneNameUtility.
    String[][] getZoneStrings() {
        ResourceBundle rb = localeData.getTimeZoneNames(locale);
        Set<String> keyset = getZoneIDs();
        // Use a LinkedHashSet to preserve the order
        Set<String[]> value = new LinkedHashSet<>();
        Set<String> tzIds = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
        for (String key : keyset) {
            if (!key.startsWith(TZNB_EXCITY_PREFIX)) {
                value.add(rb.getStringArray(key));
                tzIds.remove(key);
            }
        }

        // Add timezones which are not present in this keyset,
        // so that their fallback names will be generated at runtime.
        tzIds.stream().filter(i -> (!i.startsWith("Etc/GMT")
                && !i.startsWith("GMT")
                && !i.startsWith("SystemV")))
                .forEach(tzid -> {
                    String[] val = new String[7];
                    if (keyset.contains(tzid)) {
                        val = rb.getStringArray(tzid);
                    } else {
                        var canonID = TimeZoneNameUtility.canonicalTZID(tzid)
                                        .orElse(tzid);
                        if (keyset.contains(canonID)) {
                            val = rb.getStringArray(canonID);
                        }
                    }
                    val[0] = tzid;
                    value.add(val);
                });
        return value.toArray(new String[0][]);
    }

    String[] getCalendarNames(String key) {
        return getNames(key);
    }

    String[] getJavaTimeNames(String key) {
        return getNames(key);
    }

    // the CLDR adapter's calendar names and java.time names are the same bundle's
    private String[] getNames(String key) {
        String cacheKey = CALENDAR_NAMES + key;
        Object data = cache.get(cacheKey);
        if (data == null) {
            ResourceBundle formatData = localeData.getDateFormatData(locale);
            if (!formatData.containsKey(key)) {
                return null;
            }
            data = formatData.getStringArray(key);
            cache.putIfAbsent(cacheKey, data);
        }
        return (String[]) data;
    }

    public String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType) {
        calType = CalendarDataUtility.normalizeCalendarType(calType);
        String pattern;
        pattern = getDateTimePattern("java.time.", timeStyle, dateStyle, calType);
        if (pattern == null) {
            pattern = getDateTimePattern(null, timeStyle, dateStyle, calType);
        }
        return pattern;
    }

    private String getDateTimePattern(String prefix, int timeStyle, int dateStyle, String calType) {
        String pattern;
        String timePattern = null;
        String datePattern = null;

        if (timeStyle >= 0) {
            if (prefix != null) {
                timePattern = getDateTimePattern(prefix, "TimePatterns", timeStyle, calType);
            }
            if (timePattern == null) {
                timePattern = getDateTimePattern(null, "TimePatterns", timeStyle, calType);
            }
        }
        if (dateStyle >= 0) {
            if (prefix != null) {
                datePattern = getDateTimePattern(prefix, "DatePatterns", dateStyle, calType);
            }
            if (datePattern == null) {
                datePattern = getDateTimePattern(null, "DatePatterns", dateStyle, calType);
            }
        }
        if (timeStyle >= 0) {
            if (dateStyle >= 0) {
                String dateTimePattern = null;
                int dateTimeStyle = Math.max(dateStyle, timeStyle);
                if (prefix != null) {
                    dateTimePattern = getDateTimePattern(prefix, "DateTimePatterns", dateTimeStyle, calType);
                }
                if (dateTimePattern == null) {
                    dateTimePattern = getDateTimePattern(null, "DateTimePatterns", dateTimeStyle, calType);
                }
                pattern = switch (Objects.requireNonNull(dateTimePattern)) {
                    case "{1} {0}" -> datePattern + " " + timePattern;
                    case "{0} {1}" -> timePattern + " " + datePattern;
                    default -> messageFormat(dateTimePattern.replaceAll("'", "''"), timePattern, datePattern);
                };
            } else {
                pattern = timePattern;
            }
        } else if (dateStyle >= 0) {
            pattern = datePattern;
        } else {
            throw new IllegalArgumentException("No date or time style specified");
        }
        return pattern;
    }

    private String getDateTimePattern(String prefix, String key, int styleIndex, String calendarType) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        }
        if (!"gregory".equals(calendarType)) {
            sb.append(calendarType).append('.');
        }
        sb.append(key);
        String resourceKey = sb.toString();
        String cacheKey = sb.insert(0, DATE_TIME_PATTERN).toString();

        Object value = cache.get(cacheKey);
        if (value == null) {
            value = NULLOBJECT;
            ResourceBundle r = localeData.getDateFormatData(locale);
            if (r.containsKey(resourceKey)) {
                value = r.getStringArray(resourceKey);
            } else {
                if (r.containsKey(key)) {
                    value = r.getStringArray(key);
                }
            }
            cache.putIfAbsent(cacheKey, value);
        }
        if (value == NULLOBJECT) {
            return null;
        }

        // for DateTimePatterns. CLDR has multiple styles, while JRE has one.
        String[] styles = (String[]) value;
        return (styles.length > 1 ? styles[styleIndex] : styles[0]);
    }

    /**
     * The lookup locales of LocaleServiceProviderPool (ResourceBundle.Control's default
     * candidate list), whose first supported one with an answer wins.
     */
    static List<Locale> candidateLocales(Locale locale) {
        return ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .getCandidateLocales("", locale);
    }

    /** The CLDR adapter's candidate locales (with CLDR's parent locales). */
    static List<Locale> cldrCandidateLocales(Locale locale) {
        return LocaleProviderAdapter.getResourceBundleBased().getCandidateLocales("", locale);
    }

    /** The lookup locales the adapter supports (LocaleServiceProviderPool's findProviders). */
    static List<Locale> supportedCandidateLocales(Locale locale) {
        List<Locale> list = candidateLocales(locale);
        list.removeIf(l -> !isSupportedLocale(l));
        return list;
    }

    /** Whether the adapter supports the locale. */
    static boolean isSupportedLocale(Locale locale) {
        return LocaleProviderAdapter.isSupportedLocale(locale);
    }

    public ResourceBundle getJavaTimeFormatData() {
        return localeData.getDateFormatData(locale);
    }

    public String getLocalizedPattern(String requestedTemplate, String calType) {
        String cacheKey = SKELETON_PATTERN + calType + "." + requestedTemplate;
        Object data = cache.get(cacheKey);
        String pattern;
        if (data == null) {
            pattern = getLocalizedPatternImpl(requestedTemplate, calType);
            cache.putIfAbsent(cacheKey, pattern != null ? pattern : "");
        } else if ("".equals(data)) {
            // non-existent pattern
            pattern = null;
        } else {
            pattern = (String) data;
        }
        return pattern;
    }

    private String getLocalizedPatternImpl(String requestedTemplate, String calType) {
        initSkeletonIfNeeded();

        // input skeleton substitution
        var skeleton = substituteInputSkeletons(requestedTemplate);

        // validity check
        var matcher = VALID_SKELETON_PATTERN.matcher(skeleton);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Requested template \"%s\" is invalid".formatted(requestedTemplate) +
                    (requestedTemplate.equals(skeleton) ? "." : ", which translated into \"%s\"".formatted(skeleton) +
                            " after the 'j' or 'C' substitution."));
        }

        // try to match entire requested template first
        String matched = matchSkeleton(skeleton, calType);
        if (matched == null) {
            // 2.6.2.2 Missing Skeleton Fields
            var dateMatched = matchSkeleton(matcher.group("date"), calType);
            var timeMatched = matchSkeleton(matcher.group("time"), calType);
            if (dateMatched != null && timeMatched != null) {
                // combine both matches
                var style = switch (requestedTemplate.replaceAll("[^M]+", "").length()) {
                    case 4 -> requestedTemplate.indexOf('E') >= 0 ? 0 : 1;
                    case 3 -> 2;
                    default -> 3;
                };
                var dateTimePattern = getDateTimePattern(null, "DateTimePatterns", style, calType);
                matched = messageFormat(dateTimePattern.replaceAll("'", "''"), timeMatched, dateMatched);
            }
        }

        return matched;
    }

    private String matchSkeleton(String skeleton, String calType) {
        // Expand it with possible inferred skeleton stream based on its priority
        var inferred = possibleInferred(skeleton);

        // Search the closest format pattern string from the resource bundle
        ResourceBundle r = localeData.getDateFormatData(locale);
        return inferred
            .map(s -> ("gregory".equals(calType) ? "" : calType + ".") + "DateFormatItem." + s)
            .map(key -> r.containsKey(key) ? r.getString(key) : null)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private void initSkeletonIfNeeded() {
        // "preferred"/"allowed" input skeleton maps
        if (inputSkeletons == null) {
            Map<String, Map<String, String>> skeletons = new HashMap<>();
            Pattern p = Pattern.compile("([^:]+):([^;]+);");
            ResourceBundle r = localeData.getDateFormatData(Locale.ROOT);
            for (String type : new String[] {"preferred", "allowed"}) {
                var inputRegionsKey = SKELETON_INPUT_REGIONS_KEY + "." + type;
                Map<String, String> typeMap = new HashMap<>();

                if (r.containsKey(inputRegionsKey)) {
                    Matcher m = p.matcher(r.getString(inputRegionsKey));
                    while (m.find()) {
                        for (String region : m.group(2).split(" ")) {
                            typeMap.put(region, m.group(1));
                        }
                    }
                }
                skeletons.put(type, typeMap);
            }
            inputSkeletons = skeletons;
        }

        // j/C patterns for this locale
        if (jPattern == null) {
            jPattern = resolveInputSkeleton("preferred");
            CPattern = resolveInputSkeleton("allowed");
            // hack: "allowed" contains reversed order for hour/period, e.g, "hB" which should be "Bh" as a skeleton
            if (CPattern.length() == 2) {
                var ba = new byte[2];
                ba[0] = (byte)CPattern.charAt(1);
                ba[1] = (byte)CPattern.charAt(0);
                CPattern = new String(ba);
            }
        }
    }

    private String resolveInputSkeleton(String type) {
        var regionToSkeletonMap = inputSkeletons.get(type);
        return regionToSkeletonMap.getOrDefault(locale.getLanguage() + "-" + locale.getCountry(),
            regionToSkeletonMap.getOrDefault(locale.getCountry(),
                regionToSkeletonMap.getOrDefault(locale.getLanguage() + "-001",
                    regionToSkeletonMap.getOrDefault("001", "h"))));
    }

    private String substituteInputSkeletons(String requestedTemplate) {
        var cCount = requestedTemplate.chars().filter(c -> c == 'C').count();
        return requestedTemplate.replaceAll("j", jPattern)
                .replaceFirst("C+", CPattern.replaceAll("([hkHK])", "$1".repeat((int)cCount)));
    }

    private Stream<String> possibleInferred(String skeleton) {
        return priorityList(skeleton, "M", "L").stream()
                .flatMap(s -> priorityList(s, "E", "c").stream())
                .distinct();
    }

    private List<String> priorityList(String skeleton, String pChar, String subChar) {
        int first = skeleton.indexOf(pChar);
        int last = skeleton.lastIndexOf(pChar);

        if (first >= 0) {
            var prefix = skeleton.substring(0, first);
            var suffix = skeleton.substring(last + 1);

            // Priority are based on this chart. First column is the original count of `pChar`,
            // then it is followed by inferred skeletons base on priority.
            //
            // 1->2->3->4 (number form (1-digit) -> number form (2-digit) -> Abbr. form -> Full form)
            // 2->1->3->4
            // 3->4->2->1
            // 4->3->2->1
            var o1 = prefix + pChar + suffix;
            var o2 = prefix + pChar.repeat(2) + suffix;
            var o3 = prefix + pChar.repeat(3) + suffix;
            var o4 = prefix + pChar.repeat(4) + suffix;
            var s1 = prefix + subChar + suffix;
            var s2 = prefix + subChar.repeat(2) + suffix;
            var s3 = prefix + subChar.repeat(3) + suffix;
            var s4 = prefix + subChar.repeat(4) + suffix;
            return switch (last - first) {
                case 1 -> List.of(skeleton, o1, o2, o3, o4, s1, s2, s3, s4);
                case 2 -> List.of(skeleton, o2, o1, o3, o4, s2, s1, s3, s4);
                case 3 -> List.of(skeleton, o3, o4, o2, o1, s3, s4, s2, s1);
                default -> List.of(skeleton, o4, o3, o2, o1, s4, s3, s2, s1);
            };
        } else {
            return List.of(skeleton);
        }
    }

    public String[] getRules() {
        Object data = cache.get(RULES_CACHEKEY);
        if (data == null) {
            ResourceBundle rb = localeData.getDateFormatData(locale);
            String[] rules = new String[2];
            rules[0] = rules[1] = "";
            if (rb.containsKey("PluralRules")) {
                rules[0] = rb.getString("PluralRules");
            }
            if (rb.containsKey("DayPeriodRules")) {
                rules[1] = rb.getString("DayPeriodRules");
            }
            cache.putIfAbsent(RULES_CACHEKEY, rules);
            data = rules;
        }
        return (String[]) data;
    }

    /**
     * java.text.MessageFormat.format for patterns of literal text, quotes and {n} arguments
     * that are strings (what the CLDR patterns hold): '' is a quote, text between single quotes
     * is literal, {n} is the n-th argument.
     */
    static String messageFormat(String pattern, Object... args) {
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        int n = pattern.length();
        for (int i = 0; i < n; i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                if (i + 1 < n && pattern.charAt(i + 1) == '\'') {
                    sb.append('\'');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == '{' && !quoted) {
                int end = pattern.indexOf('}', i);
                int index = Integer.parseInt(pattern.substring(i + 1, end).trim());
                sb.append(index < args.length ? String.valueOf(args[index]) : "{" + index + "}");
                i = end;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------
    // java.text's lookups (jdk26u's)

    Object getBreakIteratorInfo(String key) {
        String cacheKey = BREAK_ITERATOR_INFO + key;
        Object biInfo = cache.get(cacheKey);
        if (biInfo == null) {
            biInfo = localeData.getBreakIteratorInfo(locale).getObject(key);
            cache.putIfAbsent(cacheKey, biInfo);
        }
        return biInfo;
    }

    byte[] getBreakIteratorResources(String key) {
        return (byte[]) localeData.getBreakIteratorResources(locale).getObject(key);
    }

    public String getCollationData() {
        String key = "Rule";
        String cacheKey = COLLATION_DATA;
        String coldata = "";

        try {
            var type = locale.getUnicodeLocaleType("co");
            if (type != null && !type.isEmpty() && !type.equalsIgnoreCase("standard")) {
                key += "." + type;
                cacheKey += type;
            }
        } catch (IllegalArgumentException ignore) {}

        Object data = cache.get(cacheKey);
        if (data == null) {
            ResourceBundle rb = localeData.getCollationData(locale);
            if (rb.containsKey(key)) {
                coldata = rb.getString(key);
            }
            cache.putIfAbsent(cacheKey, coldata);
        } else {
            coldata = (String) data;
        }

        return coldata;
    }

    public Object[] getDecimalFormatSymbolsData() {
        Object data = cache.get(DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY);
        if (data == null) {
            // Note that only dfsdata[0] is prepared here in this method. Other
            // elements are provided by the caller, yet they are cached here.
            ResourceBundle rb = localeData.getNumberFormatData(locale);
            Object[] dfsdata = new Object[3];
            dfsdata[0] = getNumberStrings(rb, "NumberElements");
            Object prev = cache.putIfAbsent(DECIMAL_FORMAT_SYMBOLS_DATA_CACHEKEY, dfsdata);
            data = prev != null ? prev : dfsdata;
        }
        return (Object[]) data;
    }

    public String getCurrencyName(String key) {
        String cacheKey = CURRENCY_NAMES + key;
        Object currencyName = cache.get(cacheKey);
        if (currencyName != null) {
            return currencyName == NULLOBJECT ? null : (String) currencyName;
        }

        ResourceBundle olrb = localeData.getCurrencyNames(locale);
        if (olrb.containsKey(key)) {
            currencyName = olrb.getObject(key);
            cache.putIfAbsent(cacheKey, currencyName);
        }

        return (String) currencyName;
    }

    public String getLocaleName(String key) {
        // Get names for old ISO codes with new ISO code resources
        if (jdk.internal.util.StaticProperty.javaLocaleUseOldISOCodes().equalsIgnoreCase("true")) {
            key = switch (key) {
                case "iw" -> "he";
                case "in" -> "id";
                case "ji" -> "yi";
                default -> key;
            };
        }

        String cacheKey = LOCALE_NAMES + key;
        Object localeName = cache.get(cacheKey);
        if (localeName != null) {
            return localeName == NULLOBJECT ? null : (String) localeName;
        }

        ResourceBundle olrb = localeData.getLocaleNames(locale);
        if (olrb.containsKey(key)) {
            localeName = olrb.getObject(key);
            cache.putIfAbsent(cacheKey, localeName);
        }

        return (String) localeName;
    }

    public String getDateTimePattern(int timeStyle, int dateStyle, Calendar cal) {
        if (cal == null) {
            cal = Calendar.getInstance(locale);
        }
        return getDateTimePattern(null, timeStyle, dateStyle, cal.getCalendarType());
    }

    /**
     * Returns the compact number format patterns.
     * @param formatStyle the style for formatting a number
     * @return an array of compact number patterns
     */
    public String[] getCNPatterns(NumberFormat.Style formatStyle) {
        Objects.requireNonNull(formatStyle);
        String width = (formatStyle == NumberFormat.Style.LONG) ? "long" : "short";
        String cacheKey = width + "." + COMPACT_NUMBER_PATTERNS_CACHEKEY;
        Object data = cache.get(cacheKey);
        if (data == null) {
            ResourceBundle resource = localeData.getNumberFormatData(locale);
            data = (String[]) resource.getObject(width + ".CompactNumberPatterns");
            cache.putIfAbsent(cacheKey, data);
        }
        return (String[]) data;
    }

    /**
     * {@return the list patterns for the locale}
     *
     * @param type a {@link ListFormat.Type}
     * @param style a {@link ListFormat.Style}
     */
    public String[] getListPatterns(ListFormat.Type type, ListFormat.Style style) {
        String typeStr = type.toString().toLowerCase(Locale.ROOT);
        String styleStr = style.toString().toLowerCase(Locale.ROOT);
        String cacheKey = LIST_PATTERN + typeStr + styleStr;
        Object data = cache.get(cacheKey);

        if (data == null) {
            var rbKey = "ListPatterns_" + typeStr + (style == ListFormat.Style.FULL ? "" : "-" + styleStr);
            String[] lpArray = localeData.getDateFormatData(locale).getStringArray(rbKey);

            if (lpArray[0].isEmpty() || lpArray[1].isEmpty() || lpArray[2].isEmpty()) {
                var candList = cldrCandidateLocales(locale);
                if (!candList.isEmpty()) {
                    for (var p : candList.subList(1, candList.size())) {
                        var parentPatterns = localeData.getDateFormatData(p).getStringArray(rbKey);
                        for (int i = 0; i < 3; i++) { // exclude optional ones, ie, "two"/"three"
                            if (lpArray[i].isEmpty()) {
                                lpArray[i] = parentPatterns[i];
                            }
                        }
                    }
                }
            }
            cache.putIfAbsent(cacheKey, lpArray);
            data = lpArray;
        }

        return (String[]) data;
    }
}
