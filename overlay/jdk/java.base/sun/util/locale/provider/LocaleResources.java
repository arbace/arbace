/*
 * jrt's own sun.util.locale.provider.LocaleResources for the Go build (doc/go/JRT-NOTES.md,
 * "Time"). Its lookups (getCalendarData, getNumberPatterns and getNumberStrings,
 * getTimeZoneNames, getZoneIDs, getZoneStrings, getCalendarNames, getJavaTimeNames,
 * getJavaTimeDateTimePattern and getDateTimePattern, getLocalizedPattern with its skeleton
 * matching, getRules) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/locale/provider/LocaleResources.java, Copyright (c) Oracle
 * and/or its affiliates, under the GNU General Public License version 2 with the Classpath
 * Exception (LICENSE.md); the bundles' chaining and the MessageFormat subset are Arbace's own.
 */
package sun.util.locale.provider;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A locale's resources, as the JDK's CLDR adapter has them, over java.base's CLDR bundles:
 * FormatData and TimeZoneNames of the English language (FormatData_en, TimeZoneNames_en) then
 * of the root locale for a locale whose language is English, of the root locale alone for any
 * other (the bundle chain the JDK's candidate locales give, en_US to en to root, there being no
 * en_US bundle in java.base), and the root locale's CalendarData. Values are cached; they are
 * not to be changed by callers (as the JDK's).
 */
public class LocaleResources {

    private static final ResourceBundle ROOT_FORMAT = new sun.text.resources.cldr.FormatData();
    private static final ResourceBundle EN_FORMAT = new sun.text.resources.cldr.FormatData_en();
    private static final ResourceBundle ROOT_TZNAMES = new sun.util.resources.cldr.TimeZoneNames();
    private static final ResourceBundle EN_TZNAMES = new sun.util.resources.cldr.TimeZoneNames_en();
    private static final ResourceBundle CALENDAR_DATA = new sun.util.resources.cldr.CalendarData();

    private final Locale locale;
    private final ResourceBundle formatData;
    private final ResourceBundle timeZoneNames;
    private final ConcurrentMap<String, Object> cache = new ConcurrentHashMap<>();

    // cache key prefixes
    private static final String CALENDAR_DATA_KEY = "CALD.";
    private static final String TIME_ZONE_NAMES = "TZN.";
    private static final String ZONE_IDS_CACHEKEY = "ZID";
    private static final String CALENDAR_NAMES = "CALN.";
    private static final String NUMBER_PATTERNS_CACHEKEY = "NP";
    private static final String DATE_TIME_PATTERN = "DTP.";
    private static final String RULES_CACHEKEY = "RULE";
    private static final String SKELETON_PATTERN = "SP.";

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
        if ("en".equals(locale.getLanguage())) {
            formatData = new Bundles(EN_FORMAT, ROOT_FORMAT);
            timeZoneNames = new Bundles(EN_TZNAMES, ROOT_TZNAMES);
        } else {
            formatData = new Bundles(ROOT_FORMAT);
            timeZoneNames = new Bundles(ROOT_TZNAMES);
        }
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
            if (CALENDAR_DATA.containsKey(key)) {
                caldata = CALENDAR_DATA.getString(key);
            }
            cache.putIfAbsent(cacheKey, caldata);
            data = caldata;
        }
        return (String) data;
    }

    public String[] getNumberPatterns() {
        Object data = cache.get(NUMBER_PATTERNS_CACHEKEY);
        if (data == null) {
            data = getNumberStrings(formatData, "NumberPatterns");
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
            ResourceBundle tznb = timeZoneNames;
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
            data = timeZoneNames.keySet();
            cache.putIfAbsent(ZONE_IDS_CACHEKEY, data);
        }
        return (Set<String>) data;
    }

    // zoneStrings are cached separately in TimeZoneNameUtility.
    String[][] getZoneStrings() {
        ResourceBundle rb = timeZoneNames;
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
            ResourceBundle r = formatData;
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
     * JavaTimeDateTimePatternImpl.getJavaTimeDateTimePattern(String, String, Locale): the
     * pattern of a requested template in the first of the locale's candidate locales that has
     * one, else in the generic calendar's.
     */
    public static String getJavaTimeDateTimePattern(String requestedTemplate, String calType, Locale locale) {
        LocaleProviderAdapter lpa = LocaleProviderAdapter.getResourceBundleBased();
        return candidateLocales(locale).stream()
                .map(lpa::getLocaleResources)
                .map(lr -> lr.getLocalizedPattern(requestedTemplate, calType))
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> calType.equals("generic") ? Optional.empty():
                        Optional.of(getJavaTimeDateTimePattern(requestedTemplate, "generic", locale)))
                .orElseThrow(() -> new DateTimeException("Requested template \"" + requestedTemplate +
                        "\" cannot be resolved in the locale \"" + locale + "\""));
    }

    /**
     * The candidate locales of a locale without script or extensions, as ResourceBundle.Control
     * and the CLDR adapter give them for java.base's locales: language_COUNTRY_variant,
     * language_COUNTRY, language, the root locale.
     */
    static List<Locale> candidateLocales(Locale locale) {
        List<Locale> list = new ArrayList<>(4);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (!variant.isEmpty()) {
            list.add(locale);
        }
        if (!country.isEmpty()) {
            list.add(variant.isEmpty() ? locale : Locale.of(language, country));
        }
        if (!language.isEmpty()) {
            list.add(country.isEmpty() && variant.isEmpty() ? locale : Locale.of(language));
        }
        list.add(Locale.ROOT);
        return list;
    }

    public ResourceBundle getJavaTimeFormatData() {
        return formatData;
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
        ResourceBundle r = formatData;
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
            ResourceBundle r = ROOT_FORMAT;
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
            ResourceBundle rb = formatData;
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

    /**
     * The chain of a locale's bundles as one bundle: a key's value is the first bundle's that
     * has the key (as a bundle's parent chain); the keys are the bundles', in their order.
     */
    private static final class Bundles extends ResourceBundle {
        private final ResourceBundle[] chain;
        private volatile Set<String> keys;

        Bundles(ResourceBundle... chain) {
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
