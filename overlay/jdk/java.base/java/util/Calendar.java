/*
 * jrt's own java.util.Calendar for the Go build (doc/go/JRT-NOTES.md, phase 2B "Dates" and "Text
 * and locales"). Parts of its code (the field resolution of selectFields, the stamps,
 * adjustStamp, equals, hashCode, toString; the week parameters by locale, setWeekCountData; the
 * display names, getDisplayName, getDisplayNames and their helpers) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/util/Calendar.java, Copyright (c) Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md);
 * the rest is Arbace's own.
 */
package java.util;

import java.time.Instant;

/**
 * java.util.Calendar for the Go build, behaving as the JDK's: the fields, their lazy recomputation (set marks the time
 * unset; the time is computed from the fields in the zone current at that moment, as the JDK
 * computes it), the zone, leniency and the week parameters. Every instance is a
 * GregorianCalendar (no Buddhist or Japanese calendars); the week parameters and display names
 * are the locale's, from the locale providers (CalendarDataUtility, DateFormatSymbols).
 */
public abstract class Calendar implements java.io.Serializable, Cloneable, Comparable<Calendar> {
    public static final int ERA = 0;
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int WEEK_OF_YEAR = 3;
    public static final int WEEK_OF_MONTH = 4;
    public static final int DATE = 5;
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_YEAR = 6;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    public static final int AM_PM = 9;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    public static final int MINUTE = 12;
    public static final int SECOND = 13;
    public static final int MILLISECOND = 14;
    public static final int ZONE_OFFSET = 15;
    public static final int DST_OFFSET = 16;
    public static final int FIELD_COUNT = 17;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;
    public static final int UNDECIMBER = 12;

    public static final int AM = 0;
    public static final int PM = 1;

    public static final int ALL_STYLES = 0;
    static final int STANDALONE_MASK = 0x8000;
    public static final int SHORT = 1;
    public static final int LONG = 2;
    public static final int NARROW_FORMAT = 4;
    public static final int NARROW_STANDALONE = NARROW_FORMAT | STANDALONE_MASK;
    public static final int SHORT_FORMAT = 1;
    public static final int LONG_FORMAT = 2;
    public static final int SHORT_STANDALONE = SHORT | STANDALONE_MASK;
    public static final int LONG_STANDALONE = LONG | STANDALONE_MASK;

    private static final String[] FIELD_NAME = {
        "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH", "DAY_OF_YEAR",
        "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", "HOUR_OF_DAY", "MINUTE", "SECOND",
        "MILLISECOND", "ZONE_OFFSET", "DST_OFFSET"
    };

    // the stamps of the fields: unset, computed, then set by the user in increasing order
    static final int UNSET = 0;
    static final int COMPUTED = 1;
    static final int MINIMUM_USER_STAMP = 2;

    protected int[] fields;
    protected boolean[] isSet;
    protected long time;
    protected boolean isTimeSet;
    protected boolean areFieldsSet;

    transient boolean areAllFieldsSet;
    transient int[] stamp;
    private transient int nextStamp = MINIMUM_USER_STAMP;

    private boolean lenient = true;
    private TimeZone zone;
    private int firstDayOfWeek;
    private int minimalDaysInFirstWeek;

    protected Calendar() {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
    }

    protected Calendar(TimeZone zone, Locale aLocale) {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        stamp = new int[FIELD_COUNT];
        this.zone = zone;
        setWeekCountData(aLocale);
    }

    // The week parameters by locale (jdk26u's cache and setWeekCountData)
    private static final java.util.concurrent.ConcurrentMap<Locale, int[]> cachedLocaleData
        = new java.util.concurrent.ConcurrentHashMap<>(3);

    private void setWeekCountData(Locale desiredLocale)
    {
        /* try to get the Locale data from the cache */
        int[] data = cachedLocaleData.get(desiredLocale);
        if (data == null) {  /* cache miss */
            data = new int[2];
            data[0] = sun.util.locale.provider.CalendarDataUtility.retrieveFirstDayOfWeek(desiredLocale);
            data[1] = sun.util.locale.provider.CalendarDataUtility.retrieveMinimalDaysInFirstWeek(desiredLocale);
            cachedLocaleData.putIfAbsent(desiredLocale, data);
        }
        firstDayOfWeek = data[0];
        minimalDaysInFirstWeek = data[1];
    }

    public static Calendar getInstance() {
        return new GregorianCalendar();
    }

    public static Calendar getInstance(TimeZone zone) {
        return new GregorianCalendar(zone);
    }

    public static Calendar getInstance(Locale aLocale) {
        return new GregorianCalendar(aLocale);
    }

    public static Calendar getInstance(TimeZone zone, Locale aLocale) {
        return new GregorianCalendar(zone, aLocale);
    }

    public static synchronized Locale[] getAvailableLocales() {
        return java.text.DateFormat.getAvailableLocales();
    }

    public static Set<String> getAvailableCalendarTypes() {
        Set<String> s = new HashSet<>();
        s.add("gregory");
        return Collections.unmodifiableSet(s);
    }

    protected abstract void computeTime();

    protected abstract void computeFields();

    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    public long getTimeInMillis() {
        if (!isTimeSet) {
            updateTime();
        }
        return time;
    }

    public void setTimeInMillis(long millis) {
        if (time == millis && isTimeSet && areFieldsSet && areAllFieldsSet) {
            return;
        }
        time = millis;
        isTimeSet = true;
        areFieldsSet = false;
        computeFields();
        markFieldsComputed();
    }

    public final Instant toInstant() {
        return Instant.ofEpochMilli(getTimeInMillis());
    }

    public int get(int field) {
        complete();
        return internalGet(field);
    }

    protected final int internalGet(int field) {
        return fields[field];
    }

    final void internalSet(int field, int value) {
        fields[field] = value;
    }

    public void set(int field, int value) {
        if (areFieldsSet && !areAllFieldsSet) {
            computeFields();
            markFieldsComputed();
        }
        internalSet(field, value);
        isTimeSet = false;
        areFieldsSet = false;
        isSet[field] = true;
        stamp[field] = nextStamp++;
        if (nextStamp == Integer.MAX_VALUE) {
            adjustStamp();
        }
    }

    public final void set(int year, int month, int date) {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
    }

    public final void set(int year, int month, int date, int hourOfDay, int minute) {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
    }

    public final void set(int year, int month, int date, int hourOfDay, int minute, int second) {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
        set(SECOND, second);
    }

    public final void clear() {
        for (int i = 0; i < fields.length; ) {
            stamp[i] = fields[i] = 0;
            isSet[i++] = false;
        }
        areAllFieldsSet = areFieldsSet = false;
        isTimeSet = false;
    }

    public final void clear(int field) {
        fields[field] = 0;
        stamp[field] = UNSET;
        isSet[field] = false;
        areAllFieldsSet = areFieldsSet = false;
        isTimeSet = false;
    }

    public final boolean isSet(int field) {
        return stamp[field] != UNSET;
    }

    // the fields with display names (jdk26u's masks)
    static final int ERA_MASK = (1 << ERA);
    static final int MONTH_MASK = (1 << MONTH);
    static final int DAY_OF_WEEK_MASK = (1 << DAY_OF_WEEK);
    static final int AM_PM_MASK = (1 << AM_PM);

    // The display names (jdk26u's getDisplayName, getDisplayNames and their helpers): the
    // locale's names from CalendarDataUtility and DateFormatSymbols

    public String getDisplayName(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, SHORT, NARROW_FORMAT, locale,
                            ERA_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }

        String calendarType = getCalendarType();
        int fieldValue = get(field);
        // the standalone/narrow styles and short era are supported only through
        // CalendarNameProviders.
        if (isStandaloneStyle(style) || isNarrowFormatStyle(style) ||
            field == ERA && (style & SHORT) == SHORT) {
            String val = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                    field, fieldValue,
                                                                    style, locale);
            // Perform fallback here to follow the CLDR rules
            if (val == null) {
                if (isNarrowFormatStyle(style)) {
                    val = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                     field, fieldValue,
                                                                     toStandaloneStyle(style),
                                                                     locale);
                } else if (isStandaloneStyle(style)) {
                    val = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                     field, fieldValue,
                                                                     getBaseStyle(style),
                                                                     locale);
                }
            }
            return val;
        }

        java.text.DateFormatSymbols symbols = java.text.DateFormatSymbols.getInstance(locale);
        String[] strings = getFieldStrings(field, style, symbols);
        if (strings != null) {
            if (fieldValue < strings.length) {
                return strings[fieldValue];
            }
        }
        return null;
    }

    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, ALL_STYLES, NARROW_FORMAT, locale,
                                    ERA_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }

        String calendarType = getCalendarType();
        if (style == ALL_STYLES || isStandaloneStyle(style) || isNarrowFormatStyle(style) ||
            field == ERA && (style & SHORT) == SHORT) {
            Map<String, Integer> map;
            map = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueNames(calendarType, field, style, locale);

            // Perform fallback here to follow the CLDR rules
            if (map == null) {
                if (isNarrowFormatStyle(style)) {
                    map = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueNames(calendarType, field,
                                                                      toStandaloneStyle(style), locale);
                } else if (style != ALL_STYLES) {
                    map = sun.util.locale.provider.CalendarDataUtility.retrieveFieldValueNames(calendarType, field,
                                                                      getBaseStyle(style), locale);
                }
            }
            return map;
        }

        // SHORT or LONG
        return getDisplayNamesImpl(field, style, locale);
    }

    private Map<String,Integer> getDisplayNamesImpl(int field, int style, Locale locale) {
        java.text.DateFormatSymbols symbols = java.text.DateFormatSymbols.getInstance(locale);
        String[] strings = getFieldStrings(field, style, symbols);
        if (strings != null) {
            Map<String,Integer> names = new HashMap<>();
            for (int i = 0; i < strings.length; i++) {
                if (strings[i].isEmpty()) {
                    continue;
                }
                names.put(strings[i], i);
            }
            return names;
        }
        return null;
    }

    boolean checkDisplayNameParams(int field, int style, int minStyle, int maxStyle,
                                   Locale locale, int fieldMask) {
        int baseStyle = getBaseStyle(style); // Ignore the standalone mask
        if (field < 0 || field >= fields.length ||
            baseStyle < minStyle || baseStyle > maxStyle || baseStyle == 3) {
            throw new IllegalArgumentException();
        }
        if (locale == null) {
            throw new NullPointerException();
        }
        return isFieldSet(fieldMask, field);
    }

    private String[] getFieldStrings(int field, int style, java.text.DateFormatSymbols symbols) {
        int baseStyle = getBaseStyle(style); // ignore the standalone mask

        // DateFormatSymbols doesn't support any narrow names.
        if (baseStyle == NARROW_FORMAT) {
            return null;
        }

        return switch (field) {
            case ERA         -> symbols.getEras();
            case MONTH       -> (baseStyle == LONG) ? symbols.getMonths() : symbols.getShortMonths();
            case DAY_OF_WEEK -> (baseStyle == LONG) ? symbols.getWeekdays() : symbols.getShortWeekdays();
            case AM_PM       -> symbols.getAmPmStrings();
            default -> null;
        };
    }

    int getBaseStyle(int style) {
        return style & ~STANDALONE_MASK;
    }

    private int toStandaloneStyle(int style) {
        return style | STANDALONE_MASK;
    }

    private boolean isStandaloneStyle(int style) {
        return (style & STANDALONE_MASK) != 0;
    }

    private boolean isNarrowStyle(int style) {
        return style == NARROW_FORMAT || style == NARROW_STANDALONE;
    }

    private boolean isNarrowFormatStyle(int style) {
        return style == NARROW_FORMAT;
    }

    protected void complete() {
        if (!isTimeSet) {
            updateTime();
        }
        if (!areFieldsSet || !areAllFieldsSet) {
            computeFields();
            markFieldsComputed();
        }
    }

    /** Marks every field computed (after computeFields). */
    private void markFieldsComputed() {
        for (int i = 0; i < fields.length; i++) {
            stamp[i] = COMPUTED;
            isSet[i] = true;
        }
        areFieldsSet = areAllFieldsSet = true;
    }

    /**
     * The fields computeTime resolves from (a mask of 1 << field): the most recently set
     * combination of each group, as the class documentation's "Calendar Fields Resolution"
     * orders them (the JDK's selectFields).
     */
    final int selectFields() {
        int fieldMask = 1 << YEAR;
        if (stamp[ERA] != UNSET) {
            fieldMask |= 1 << ERA;
        }
        int dowStamp = stamp[DAY_OF_WEEK];
        int monthStamp = stamp[MONTH];
        int domStamp = stamp[DAY_OF_MONTH];
        int womStamp = aggregateStamp(stamp[WEEK_OF_MONTH], dowStamp);
        int dowimStamp = aggregateStamp(stamp[DAY_OF_WEEK_IN_MONTH], dowStamp);
        int doyStamp = stamp[DAY_OF_YEAR];
        int woyStamp = aggregateStamp(stamp[WEEK_OF_YEAR], dowStamp);
        int bestStamp = domStamp;
        if (womStamp > bestStamp) {
            bestStamp = womStamp;
        }
        if (dowimStamp > bestStamp) {
            bestStamp = dowimStamp;
        }
        if (doyStamp > bestStamp) {
            bestStamp = doyStamp;
        }
        if (woyStamp > bestStamp) {
            bestStamp = woyStamp;
        }
        if (bestStamp == UNSET) {
            womStamp = stamp[WEEK_OF_MONTH];
            dowimStamp = Math.max(stamp[DAY_OF_WEEK_IN_MONTH], dowStamp);
            woyStamp = stamp[WEEK_OF_YEAR];
            bestStamp = Math.max(Math.max(womStamp, dowimStamp), woyStamp);
            if (bestStamp == UNSET) {
                bestStamp = domStamp = monthStamp;
            }
        }
        if (bestStamp == domStamp
            || (bestStamp == womStamp && stamp[WEEK_OF_MONTH] >= stamp[WEEK_OF_YEAR])
            || (bestStamp == dowimStamp && stamp[DAY_OF_WEEK_IN_MONTH] >= stamp[WEEK_OF_YEAR])) {
            fieldMask |= 1 << MONTH;
            if (bestStamp == domStamp) {
                fieldMask |= 1 << DAY_OF_MONTH;
            } else {
                if (dowStamp != UNSET) {
                    fieldMask |= 1 << DAY_OF_WEEK;
                }
                if (womStamp == dowimStamp) {
                    if (stamp[WEEK_OF_MONTH] >= stamp[DAY_OF_WEEK_IN_MONTH]) {
                        fieldMask |= 1 << WEEK_OF_MONTH;
                    } else {
                        fieldMask |= 1 << DAY_OF_WEEK_IN_MONTH;
                    }
                } else {
                    if (bestStamp == womStamp) {
                        fieldMask |= 1 << WEEK_OF_MONTH;
                    } else if (stamp[DAY_OF_WEEK_IN_MONTH] != UNSET) {
                        fieldMask |= 1 << DAY_OF_WEEK_IN_MONTH;
                    }
                }
            }
        } else {
            if (bestStamp == doyStamp) {
                fieldMask |= 1 << DAY_OF_YEAR;
            } else {
                if (dowStamp != UNSET) {
                    fieldMask |= 1 << DAY_OF_WEEK;
                }
                fieldMask |= 1 << WEEK_OF_YEAR;
            }
        }
        int hourOfDayStamp = stamp[HOUR_OF_DAY];
        int hourStamp = aggregateStamp(stamp[HOUR], stamp[AM_PM]);
        bestStamp = (hourStamp > hourOfDayStamp) ? hourStamp : hourOfDayStamp;
        if (bestStamp == UNSET) {
            bestStamp = Math.max(stamp[HOUR], stamp[AM_PM]);
        }
        if (bestStamp != UNSET) {
            if (bestStamp == hourOfDayStamp) {
                fieldMask |= 1 << HOUR_OF_DAY;
            } else {
                fieldMask |= 1 << HOUR;
                if (stamp[AM_PM] != UNSET) {
                    fieldMask |= 1 << AM_PM;
                }
            }
        }
        if (stamp[MINUTE] != UNSET) {
            fieldMask |= 1 << MINUTE;
        }
        if (stamp[SECOND] != UNSET) {
            fieldMask |= 1 << SECOND;
        }
        if (stamp[MILLISECOND] != UNSET) {
            fieldMask |= 1 << MILLISECOND;
        }
        if (stamp[ZONE_OFFSET] >= MINIMUM_USER_STAMP) {
            fieldMask |= 1 << ZONE_OFFSET;
        }
        if (stamp[DST_OFFSET] >= MINIMUM_USER_STAMP) {
            fieldMask |= 1 << DST_OFFSET;
        }
        return fieldMask;
    }

    private static int aggregateStamp(int stampA, int stampB) {
        if (stampA == UNSET || stampB == UNSET) {
            return UNSET;
        }
        return Math.max(stampA, stampB);
    }

    static boolean isFieldSet(int fieldMask, int field) {
        return (fieldMask & (1 << field)) != 0;
    }

    final boolean isExternallySet(int field) {
        return stamp[field] >= MINIMUM_USER_STAMP;
    }

    static String getFieldName(int field) {
        return FIELD_NAME[field];
    }

    private void updateTime() {
        computeTime();
        isTimeSet = true;
        if (areFieldsSet) {
            markFieldsComputed();
        }
    }

    private void adjustStamp() {
        int max = MINIMUM_USER_STAMP;
        int newStamp = MINIMUM_USER_STAMP;
        for (;;) {
            int min = Integer.MAX_VALUE;
            for (int v : stamp) {
                if (v >= newStamp && min > v) {
                    min = v;
                }
                if (max < v) {
                    max = v;
                }
            }
            if (max != min && min == Integer.MAX_VALUE) {
                break;
            }
            for (int i = 0; i < stamp.length; i++) {
                if (stamp[i] == min) {
                    stamp[i] = newStamp;
                }
            }
            newStamp++;
            if (min == max) {
                break;
            }
        }
        nextStamp = newStamp;
    }

    public String getCalendarType() {
        return "gregory";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            Calendar that = (Calendar) obj;
            return compareTo(getMillisOf(that)) == 0 && lenient == that.lenient
                && firstDayOfWeek == that.firstDayOfWeek
                && minimalDaysInFirstWeek == that.minimalDaysInFirstWeek
                && zone.equals(that.zone);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int otheritems = (lenient ? 1 : 0) | (firstDayOfWeek << 1) | (minimalDaysInFirstWeek << 4)
            | (zone.hashCode() << 7);
        long t = getMillisOf(this);
        return (int) t ^ (int) (t >> 32) ^ otheritems;
    }

    public boolean before(Object when) {
        return when instanceof Calendar && compareTo((Calendar) when) < 0;
    }

    public boolean after(Object when) {
        return when instanceof Calendar && compareTo((Calendar) when) > 0;
    }

    @Override
    public int compareTo(Calendar anotherCalendar) {
        return compareTo(getMillisOf(anotherCalendar));
    }

    private int compareTo(long t) {
        long thisTime = getMillisOf(this);
        return (thisTime > t) ? 1 : (thisTime == t) ? 0 : -1;
    }

    private static long getMillisOf(Calendar calendar) {
        if (calendar.isTimeSet) {
            return calendar.time;
        }
        Calendar cal = (Calendar) calendar.clone();
        cal.setLenient(true);
        return cal.getTimeInMillis();
    }

    public abstract void add(int field, int amount);

    public abstract void roll(int field, boolean up);

    public void roll(int field, int amount) {
        while (amount > 0) {
            roll(field, true);
            amount--;
        }
        while (amount < 0) {
            roll(field, false);
            amount++;
        }
    }

    public void setTimeZone(TimeZone value) {
        zone = value;
        areAllFieldsSet = areFieldsSet = false;
    }

    public TimeZone getTimeZone() {
        return zone;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public boolean isLenient() {
        return lenient;
    }

    public void setFirstDayOfWeek(int value) {
        if (firstDayOfWeek == value) {
            return;
        }
        firstDayOfWeek = value;
        areAllFieldsSet = areFieldsSet = false;
    }

    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public void setMinimalDaysInFirstWeek(int value) {
        if (minimalDaysInFirstWeek == value) {
            return;
        }
        minimalDaysInFirstWeek = value;
        areAllFieldsSet = areFieldsSet = false;
    }

    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    public boolean isWeekDateSupported() {
        return false;
    }


    public int getWeekYear() {
        throw new UnsupportedOperationException();
    }

    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        throw new UnsupportedOperationException();
    }

    public int getWeeksInWeekYear() {
        throw new UnsupportedOperationException();
    }

    public abstract int getMinimum(int field);

    public abstract int getMaximum(int field);

    public abstract int getGreatestMinimum(int field);

    public abstract int getLeastMaximum(int field);

    public int getActualMinimum(int field) {
        return getMinimum(field);
    }

    public int getActualMaximum(int field) {
        return getMaximum(field);
    }

    @Override
    public Object clone() {
        try {
            Calendar other = (Calendar) super.clone();
            other.fields = new int[FIELD_COUNT];
            other.isSet = new boolean[FIELD_COUNT];
            other.stamp = new int[FIELD_COUNT];
            for (int i = 0; i < FIELD_COUNT; i++) {
                other.fields[i] = fields[i];
                other.stamp[i] = stamp[i];
                other.isSet[i] = isSet[i];
            }
            other.zone = (TimeZone) zone.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(800);
        buffer.append(getClass().getName()).append('[');
        appendValue(buffer, "time", isTimeSet, time);
        buffer.append(",areFieldsSet=").append(areFieldsSet);
        buffer.append(",areAllFieldsSet=").append(areAllFieldsSet);
        buffer.append(",lenient=").append(lenient);
        buffer.append(",zone=").append(zone);
        appendValue(buffer, ",firstDayOfWeek", true, (long) firstDayOfWeek);
        appendValue(buffer, ",minimalDaysInFirstWeek", true, (long) minimalDaysInFirstWeek);
        for (int i = 0; i < FIELD_COUNT; ++i) {
            buffer.append(',');
            appendValue(buffer, getFieldName(i), isSet(i), (long) fields[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    private static void appendValue(StringBuilder sb, String item, boolean valid, long value) {
        sb.append(item).append('=');
        if (valid) {
            sb.append(value);
        } else {
            sb.append('?');
        }
    }
}
