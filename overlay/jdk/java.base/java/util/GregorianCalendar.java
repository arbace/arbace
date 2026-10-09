/*
 * jrt's own java.util.GregorianCalendar for the Go build (doc/go/JRT-NOTES.md, phase 2B "Dates"). Parts of its code
 * (computeTime and its field resolution, add, roll, getWeekYear, the week numbering, getActualMaximum, the field tables) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/util/GregorianCalendar.java, Copyright (c) Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md);
 * the rest is Arbace's own.
 */
package java.util;

/**
 * java.util.GregorianCalendar for the Go build, behaving as the JDK's (checked against it
 * differentially): the Julian calendar before the cutover (by default 1582-10-15, settable), the
 * Gregorian from it, as the JDK's (and jrt's Date) count; the fields computed in the calendar's
 * zone (a fixed offset, TimeZone); the time from the fields as the JDK resolves them, leniently
 * or not; add, roll and the week year as the JDK's. Not here: setWeekDate,
 * getWeeksInWeekYear, toZonedDateTime and from(ZonedDateTime) (java.time's zones are outside
 * the closed world), serialization.
 */
public class GregorianCalendar extends Calendar {
    public static final int BC = 0;
    public static final int AD = 1;

    static final int BCE = 0;
    static final int CE = 1;

    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = 60 * ONE_SECOND;
    private static final int ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;

    static final int[] MIN_VALUES = {
        BCE, 1, JANUARY, 1, 0, 1, 1, SUNDAY, 1, AM, 0, 0, 0, 0, 0, -13 * ONE_HOUR, 0
    };
    static final int[] LEAST_MAX_VALUES = {
        CE, 292269054, DECEMBER, 52, 4, 28, 365, SATURDAY, 4, PM, 11, 23, 59, 59, 999, 14 * ONE_HOUR,
        20 * ONE_MINUTE
    };
    static final int[] MAX_VALUES = {
        CE, 292278994, DECEMBER, 53, 6, 31, 366, SATURDAY, 6, PM, 11, 23, 59, 59, 999, 14 * ONE_HOUR,
        2 * ONE_HOUR
    };

    private static final int[] MONTH_LENGTH = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private static final int[] LEAP_MONTH_LENGTH = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    /** The default cutover, 1582-10-15T00:00:00Z. */
    static final long DEFAULT_GREGORIAN_CUTOVER = -12219292800000L;

    private long gregorianCutover = DEFAULT_GREGORIAN_CUTOVER;
    // the cutover as a day count since 1970-01-01, its Gregorian year, and the Julian year of
    // the day before it
    private transient long gregorianCutoverDay;
    private transient int gregorianCutoverYear;
    private transient int gregorianCutoverYearJulian;

    public GregorianCalendar() {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
    }

    public GregorianCalendar(TimeZone zone) {
        this(zone, Locale.getDefault(Locale.Category.FORMAT));
    }

    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefaultRef(), aLocale);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setCutover(DEFAULT_GREGORIAN_CUTOVER);
        setTimeInMillis(System.currentTimeMillis());
    }

    public GregorianCalendar(int year, int month, int dayOfMonth) {
        this(year, month, dayOfMonth, 0, 0, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this(year, month, dayOfMonth, hourOfDay, minute, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        this(year, month, dayOfMonth, hourOfDay, minute, second, 0);
    }

    GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second, int millis) {
        super(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
        setCutover(DEFAULT_GREGORIAN_CUTOVER);
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DAY_OF_MONTH, dayOfMonth);
        if (hourOfDay >= 12 && hourOfDay <= 23) {
            this.internalSet(AM_PM, PM);
            this.internalSet(HOUR, hourOfDay - 12);
        } else {
            this.internalSet(HOUR, hourOfDay);
        }
        this.set(HOUR_OF_DAY, hourOfDay);
        this.set(MINUTE, minute);
        this.set(SECOND, second);
        this.internalSet(MILLISECOND, millis);
    }

    // ---------------------------------------------------------------------------------------
    // the cutover and the two calendars, over day counts since 1970-01-01

    private void setCutover(long cutover) {
        gregorianCutover = cutover;
        gregorianCutoverDay = Math.floorDiv(cutover, ONE_DAY);
        gregorianCutoverYear = (int) gregorianYmd(gregorianCutoverDay)[0];
        gregorianCutoverYearJulian = (int) julianYmd(gregorianCutoverDay - 1)[0];
    }

    public void setGregorianChange(Date date) {
        long cutoverTime = date.getTime();
        if (cutoverTime == gregorianCutover) {
            return;
        }
        complete();
        setCutover(cutoverTime);
        setTimeInMillis(time);
    }

    public final Date getGregorianChange() {
        return new Date(gregorianCutover);
    }

    /** Days since 1970-01-01 of the proleptic Gregorian y-m-d (m 1..12, d any). */
    static long gregorianDays(long y, long m, long d) {
        if (m <= 2) {
            y--;
        }
        long era = Math.floorDiv(y, 400);
        long yoe = y - era * 400;
        long mp = Math.floorMod(m + 9, 12);
        long doy = (153 * mp + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /** Days since 1970-01-01 of the Julian y-m-d (astronomical years, m 1..12, d any). */
    static long julianDays(long y, long m, long d) {
        long a = Math.floorDiv(14 - m, 12);
        long yy = y + 4800 - a;
        long mm = m + 12 * a - 3;
        return d + Math.floorDiv(153 * mm + 2, 5) + 365 * yy + Math.floorDiv(yy, 4) - 32083 - 2440588;
    }

    static long[] gregorianYmd(long days) {
        long z = days + 719468;
        long era = Math.floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new long[] { yoe + era * 400 + (m <= 2 ? 1 : 0), m, d };
    }

    static long[] julianYmd(long days) {
        long c = days + 2440588 + 32082;
        long dd = Math.floorDiv(4 * c + 3, 1461);
        long e = c - Math.floorDiv(1461 * dd, 4);
        long mm = Math.floorDiv(5 * e + 2, 153);
        long d = e - Math.floorDiv(153 * mm + 2, 5) + 1;
        long m = mm + 3 - 12 * Math.floorDiv(mm, 10);
        long y = dd - 4800 + Math.floorDiv(mm, 10);
        return new long[] { y, m, d };
    }

    /** {astronomical year, month 1..12, day} of a day count in this calendar. */
    private long[] ymd(long days) {
        return days >= gregorianCutoverDay ? gregorianYmd(days) : julianYmd(days);
    }

    /**
     * The day count of astronomical year y, month m (0-based, any: it counts on) and day d (any)
     * as the JDK's computeTime chooses between the calendars around the cutover.
     */
    private long fixedDate(long y, long m0, long d) {
        y += Math.floorDiv(m0, 12);
        long m = Math.floorMod(m0, 12) + 1;
        return choose(y, gregorianDays(y, m, d), julianDays(y, m, d));
    }

    private long choose(long year, long gfd, long jfd) {
        if (year > gregorianCutoverYear && year > gregorianCutoverYearJulian) {
            if (gfd >= gregorianCutoverDay) {
                return gfd;
            }
            return jfd;
        }
        if (year < gregorianCutoverYear && year < gregorianCutoverYearJulian) {
            return jfd;
        }
        if (gfd >= gregorianCutoverDay) {
            return gfd;
        }
        return jfd;
    }

    /** The day count of the first of month m0 (0-based, counting on) of Julian year y. */
    private static long julianFirst(long y, long m0) {
        return julianDays(y + Math.floorDiv(m0, 12), Math.floorMod(m0, 12) + 1, 1);
    }

    private boolean isCutoverYear(long y) {
        return y == gregorianCutoverYear || y == gregorianCutoverYearJulian;
    }

    /** The day count of January 1 of astronomical year y in this calendar. */
    private long jan1(long y) {
        return fixedDate(y, 0, 1);
    }

    public boolean isLeapYear(int year) {
        if ((year & 3) != 0) {
            return false;
        }
        if (year > gregorianCutoverYear) {
            return (year % 100 != 0) || (year % 400 == 0);
        }
        if (year < gregorianCutoverYearJulian) {
            return true;
        }
        boolean gregorian;
        if (gregorianCutoverYear == gregorianCutoverYearJulian) {
            gregorian = gregorianYmd(gregorianCutoverDay)[1] < 3;
        } else {
            gregorian = year == gregorianCutoverYear;
        }
        return gregorian ? (year % 100 != 0) || (year % 400 == 0) : true;
    }

    private int monthLength(int month, int year) {
        return isLeapYear(year) ? LEAP_MONTH_LENGTH[month] : MONTH_LENGTH[month];
    }

    // ---------------------------------------------------------------------------------------
    // fields and time

    private static long dayOfWeekOnOrBefore(long days, int dayOfWeek) {
        return days - Math.floorMod(dayOfWeekOf(days) - dayOfWeek, 7);
    }

    private static int dayOfWeekOf(long days) {
        return (int) Math.floorMod(days + 4, 7) + 1;
    }

    private int weekNumber(long day1, long days) {
        long day1st = dayOfWeekOnOrBefore(day1 + 6, getFirstDayOfWeek());
        int ndays = (int) (day1st - day1);
        if (ndays >= getMinimalDaysInFirstWeek()) {
            day1st -= 7;
        }
        int normalized = (int) (days - day1st);
        if (normalized >= 0) {
            return normalized / 7 + 1;
        }
        return Math.floorDiv(normalized, 7) + 1;
    }

    @Override
    protected void computeFields() {
        TimeZone tz = getTimeZone();
        int zoneOffset = tz.getRawOffset();
        int dstOffset = tz.getOffset(time) - zoneOffset;
        long local = time + zoneOffset + dstOffset;
        long days = Math.floorDiv(local, ONE_DAY);
        int tod = (int) Math.floorMod(local, ONE_DAY);
        long[] ymd = ymd(days);
        long y = ymd[0];
        int month = (int) ymd[1] - 1;
        int dom = (int) ymd[2];
        long jan1 = jan1(y);
        int era = CE;
        long yearOfEra = y;
        if (y <= 0) {
            era = BCE;
            yearOfEra = 1 - y;
        }
        internalSet(ERA, era);
        internalSet(YEAR, (int) yearOfEra);
        internalSet(MONTH, month);
        internalSet(DAY_OF_MONTH, dom);
        internalSet(DAY_OF_WEEK, dayOfWeekOf(days));
        internalSet(DAY_OF_YEAR, (int) (days - jan1) + 1);
        // the first day of the month: in the cutover month, the Julian first (GregorianCalendar's
        // getFixedDateMonth1)
        long month1 = fixedDate(y, month, 1);
        internalSet(DAY_OF_WEEK_IN_MONTH, (int) (days - month1) / 7 + 1);
        internalSet(WEEK_OF_MONTH, weekNumber(month1, days));
        int weekOfYear = weekNumber(jan1, days);
        if (weekOfYear == 0) {
            weekOfYear = weekNumber(jan1(y - 1), jan1 - 1);
        } else if (weekOfYear >= 52 || (y <= gregorianCutoverYear && y >= gregorianCutoverYearJulian - 1)) {
            // near the cutover, the JDK checks every week for the next year's first
            long nextJan1 = jan1(y + 1);
            long nextJan1st = dayOfWeekOnOrBefore(nextJan1 + 6, getFirstDayOfWeek());
            int ndays = (int) (nextJan1st - nextJan1);
            if (ndays >= getMinimalDaysInFirstWeek() && days >= (nextJan1st - 7)) {
                weekOfYear = 1;
            }
        }
        internalSet(WEEK_OF_YEAR, weekOfYear);
        int hour = tod / ONE_HOUR;
        internalSet(HOUR_OF_DAY, hour);
        internalSet(AM_PM, hour / 12);
        internalSet(HOUR, hour % 12);
        internalSet(MINUTE, (tod / ONE_MINUTE) % 60);
        internalSet(SECOND, (tod / ONE_SECOND) % 60);
        internalSet(MILLISECOND, tod % ONE_SECOND);
        internalSet(ZONE_OFFSET, zoneOffset);
        internalSet(DST_OFFSET, dstOffset);
        areFieldsSet = areAllFieldsSet = true;
    }

    @Override
    protected void computeTime() {
        int[] originalFields = null;
        if (!isLenient()) {
            originalFields = new int[FIELD_COUNT];
            for (int field = 0; field < FIELD_COUNT; field++) {
                int value = internalGet(field);
                if (isExternallySet(field)) {
                    if (value < getMinimum(field) || value > getMaximum(field)) {
                        throw new IllegalArgumentException(getFieldName(field));
                    }
                }
                originalFields[field] = value;
            }
        }
        boolean[] userSet = new boolean[FIELD_COUNT];
        for (int field = 0; field < FIELD_COUNT; field++) {
            userSet[field] = isExternallySet(field);
        }
        int fieldMask = selectFields();
        long year = isSet(YEAR) ? internalGet(YEAR) : 1970;
        int era = isSet(ERA) ? internalGet(ERA) : CE;
        if (era == BCE) {
            year = 1 - year;
        } else if (era != CE) {
            throw new IllegalArgumentException("Invalid era");
        }
        long timeOfDay = 0;
        if (isFieldSet(fieldMask, HOUR_OF_DAY)) {
            timeOfDay += (long) internalGet(HOUR_OF_DAY);
        } else {
            timeOfDay += internalGet(HOUR);
            if (isFieldSet(fieldMask, AM_PM)) {
                timeOfDay += 12 * internalGet(AM_PM);
            }
        }
        timeOfDay *= 60;
        timeOfDay += internalGet(MINUTE);
        timeOfDay *= 60;
        timeOfDay += internalGet(SECOND);
        timeOfDay *= 1000;
        timeOfDay += internalGet(MILLISECOND);
        long days = timeOfDay / ONE_DAY;
        timeOfDay %= ONE_DAY;
        while (timeOfDay < 0) {
            timeOfDay += ONE_DAY;
            --days;
        }
        long gfd;
        long jfd;
        calculate: {
            if (year > gregorianCutoverYear && year > gregorianCutoverYearJulian) {
                gfd = days + dateOfFields(true, year, fieldMask);
                if (gfd >= gregorianCutoverDay) {
                    days = gfd;
                    break calculate;
                }
                jfd = days + dateOfFields(false, year, fieldMask);
            } else if (year < gregorianCutoverYear && year < gregorianCutoverYearJulian) {
                jfd = days + dateOfFields(false, year, fieldMask);
                if (jfd < gregorianCutoverDay) {
                    days = jfd;
                    break calculate;
                }
                gfd = jfd;
            } else {
                jfd = days + dateOfFields(false, year, fieldMask);
                gfd = days + dateOfFields(true, year, fieldMask);
            }
            if (isFieldSet(fieldMask, DAY_OF_YEAR) || isFieldSet(fieldMask, WEEK_OF_YEAR)) {
                if (gregorianCutoverYear == gregorianCutoverYearJulian) {
                    days = jfd;
                    break calculate;
                } else if (year == gregorianCutoverYear) {
                    days = gfd;
                    break calculate;
                }
            }
            if (gfd >= gregorianCutoverDay) {
                days = gfd;
            } else if (jfd < gregorianCutoverDay) {
                days = jfd;
            } else {
                if (!isLenient()) {
                    throw new IllegalArgumentException("the specified date doesn't exist");
                }
                days = jfd;
            }
        }
        long local = days * ONE_DAY + timeOfDay;
        TimeZone tz = getTimeZone();
        long t;
        if (isFieldSet(fieldMask, ZONE_OFFSET)) {
            t = local - internalGet(ZONE_OFFSET) - (isFieldSet(fieldMask, DST_OFFSET) ? internalGet(DST_OFFSET) : 0);
        } else {
            t = local - tz.getRawOffset();
            t -= tz.getOffset(t) - tz.getRawOffset();
        }
        time = t;
        computeFields();
        if (originalFields != null) {
            for (int field = 0; field < FIELD_COUNT; field++) {
                if (userSet[field] && originalFields[field] != internalGet(field)) {
                    String s = originalFields[field] + " -> " + internalGet(field);
                    System.arraycopy(originalFields, 0, fields, 0, fields.length);
                    throw new IllegalArgumentException(getFieldName(field) + ": " + s);
                }
            }
        }
    }

    /**
     * The day count of the date the fields select (JDK's resolution: the month's day, week of
     * the month and day of the week, day of the week in the month; the year's day, week of the
     * year and day of the week) in the Gregorian or the Julian calendar.
     */
    private long dateOfFields(boolean gregorian, long year, int fieldMask) {
        long month = JANUARY;
        if (isFieldSet(fieldMask, MONTH)) {
            month = internalGet(MONTH);
            year += Math.floorDiv(month, 12);
            month = Math.floorMod(month, 12);
        }
        long fixedDate = gregorian ? gregorianDays(year, month + 1, 1) : julianDays(year, month + 1, 1);
        if (isFieldSet(fieldMask, MONTH)) {
            if (isFieldSet(fieldMask, DAY_OF_MONTH)) {
                if (isSet(DAY_OF_MONTH)) {
                    fixedDate += internalGet(DAY_OF_MONTH);
                    fixedDate--;
                }
            } else {
                if (isFieldSet(fieldMask, WEEK_OF_MONTH)) {
                    long firstDayOfWeek = dayOfWeekOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                    if ((firstDayOfWeek - fixedDate) >= getMinimalDaysInFirstWeek()) {
                        firstDayOfWeek -= 7;
                    }
                    if (isFieldSet(fieldMask, DAY_OF_WEEK)) {
                        firstDayOfWeek = dayOfWeekOnOrBefore(firstDayOfWeek + 6, internalGet(DAY_OF_WEEK));
                    }
                    fixedDate = firstDayOfWeek + 7L * (internalGet(WEEK_OF_MONTH) - 1);
                } else {
                    int dayOfWeek = isFieldSet(fieldMask, DAY_OF_WEEK) ? internalGet(DAY_OF_WEEK) : getFirstDayOfWeek();
                    int dowim = isFieldSet(fieldMask, DAY_OF_WEEK_IN_MONTH) ? internalGet(DAY_OF_WEEK_IN_MONTH) : 1;
                    if (dowim >= 0) {
                        fixedDate = dayOfWeekOnOrBefore(fixedDate + (7L * dowim) - 1, dayOfWeek);
                    } else {
                        int lastDate = monthLength((int) month, (int) year) + (7 * (dowim + 1));
                        fixedDate = dayOfWeekOnOrBefore(fixedDate + lastDate - 1, dayOfWeek);
                    }
                }
            }
        } else {
            if (year == gregorianCutoverYear && gregorian && fixedDate < gregorianCutoverDay
                && gregorianCutoverYear != gregorianCutoverYearJulian) {
                fixedDate = gregorianCutoverDay;
            }
            if (isFieldSet(fieldMask, DAY_OF_YEAR)) {
                fixedDate += internalGet(DAY_OF_YEAR);
                fixedDate--;
            } else {
                long firstDayOfWeek = dayOfWeekOnOrBefore(fixedDate + 6, getFirstDayOfWeek());
                if ((firstDayOfWeek - fixedDate) >= getMinimalDaysInFirstWeek()) {
                    firstDayOfWeek -= 7;
                }
                if (isFieldSet(fieldMask, DAY_OF_WEEK)) {
                    int dayOfWeek = internalGet(DAY_OF_WEEK);
                    if (dayOfWeek != getFirstDayOfWeek()) {
                        firstDayOfWeek = dayOfWeekOnOrBefore(firstDayOfWeek + 6, dayOfWeek);
                    }
                }
                fixedDate = firstDayOfWeek + 7 * ((long) internalGet(WEEK_OF_YEAR) - 1);
            }
        }
        return fixedDate;
    }

    // ---------------------------------------------------------------------------------------
    // arithmetic, as the JDK's GregorianCalendar adds and rolls

    private int era() {
        return isSet(ERA) ? internalGet(ERA) : CE;
    }

    /** The current year's month length (its era's year). */
    private int monthLength(int month) {
        int year = internalGet(YEAR);
        if (era() == BCE) {
            year = 1 - year;
        }
        return monthLength(month, year);
    }

    private void setYearOfEra(int year, boolean ce) {
        if (year > 0) {
            set(YEAR, year);
        } else {
            set(YEAR, 1 - year);
            set(ERA, ce ? BCE : CE);
        }
    }

    private void pinDayOfMonth() {
        int year = internalGet(YEAR);
        int monthLen;
        if (year > gregorianCutoverYear || year < gregorianCutoverYearJulian) {
            monthLen = monthLength(internalGet(MONTH));
        } else {
            GregorianCalendar gc = (GregorianCalendar) clone();
            gc.setLenient(true);
            monthLen = gc.getActualMaximum(DAY_OF_MONTH);
        }
        int dom = internalGet(DAY_OF_MONTH);
        if (dom > monthLen) {
            set(DAY_OF_MONTH, monthLen);
        }
    }

    @Override
    public void add(int field, int amount) {
        if (amount == 0) {
            return;
        }
        if (field < 0 || field >= ZONE_OFFSET) {
            throw new IllegalArgumentException();
        }
        complete();
        if (field == YEAR) {
            int year = internalGet(YEAR);
            if (era() == CE) {
                setYearOfEra(year + amount, true);
            } else {
                setYearOfEra(year - amount, false);
            }
            pinDayOfMonth();
        } else if (field == MONTH) {
            int month = internalGet(MONTH) + amount;
            int year = internalGet(YEAR);
            int yAmount = month >= 0 ? month / 12 : (month + 1) / 12 - 1;
            if (yAmount != 0) {
                if (era() == CE) {
                    setYearOfEra(year + yAmount, true);
                } else {
                    setYearOfEra(year - yAmount, false);
                }
            }
            if (month >= 0) {
                set(MONTH, month % 12);
            } else {
                month %= 12;
                if (month < 0) {
                    month += 12;
                }
                set(MONTH, JANUARY + month);
            }
            pinDayOfMonth();
        } else if (field == ERA) {
            int era = internalGet(ERA) + amount;
            if (era < 0) {
                era = 0;
            }
            if (era > 1) {
                era = 1;
            }
            set(ERA, era);
        } else {
            long delta = amount;
            long timeOfDay = 0;
            switch (field) {
            case HOUR: case HOUR_OF_DAY:
                delta *= 60 * 60 * 1000;
                break;
            case MINUTE:
                delta *= 60 * 1000;
                break;
            case SECOND:
                delta *= 1000;
                break;
            case WEEK_OF_YEAR: case WEEK_OF_MONTH: case DAY_OF_WEEK_IN_MONTH:
                delta *= 7;
                break;
            case AM_PM:
                delta = amount / 2;
                timeOfDay = 12 * (amount % 2);
                break;
            default:
                break;
            }
            if (field >= HOUR) {
                setTimeInMillis(time + delta);
                return;
            }
            long fd = currentDay();
            timeOfDay += internalGet(HOUR_OF_DAY);
            timeOfDay *= 60;
            timeOfDay += internalGet(MINUTE);
            timeOfDay *= 60;
            timeOfDay += internalGet(SECOND);
            timeOfDay *= 1000;
            timeOfDay += internalGet(MILLISECOND);
            if (timeOfDay >= ONE_DAY) {
                fd++;
                timeOfDay -= ONE_DAY;
            } else if (timeOfDay < 0) {
                fd--;
                timeOfDay += ONE_DAY;
            }
            fd += delta;
            int zoneOffset = internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET);
            setTimeInMillis(fd * ONE_DAY + timeOfDay - zoneOffset);
        }
    }

    /** The day count of the current local date (fields and time in sync). */
    private long currentDay() {
        return Math.floorDiv(time + internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET), ONE_DAY);
    }

    private static int getRolledValue(int value, int amount, int min, int max) {
        assert value >= min && value <= max;
        int range = max - min + 1;
        amount %= range;
        int n = value + amount;
        if (n > max) {
            n -= range;
        } else if (n < min) {
            n += range;
        }
        assert n >= min && n <= max;
        return n;
    }

    /** The first day of the current month (in the cutover month, the Julian first). */
    private long month1(long fd) {
        int dom = internalGet(DAY_OF_MONTH);
        long y = era() == BCE ? 1 - internalGet(YEAR) : internalGet(YEAR);
        if (isCutoverYear(y)) {
            return fixedDate(y, internalGet(MONTH), 1);
        }
        return fd - dom + 1;
    }

    /** The days of the current month, the cutover's gap left out. */
    private int actualMonthLength(long month1) {
        long y = era() == BCE ? 1 - internalGet(YEAR) : internalGet(YEAR);
        return (int) (fixedDate(y, internalGet(MONTH) + 1, 1) - month1);
    }

    @Override
    public void roll(int field, boolean up) {
        roll(field, up ? +1 : -1);
    }

    @Override
    public void roll(int field, int amount) {
        if (amount == 0) {
            return;
        }
        if (field < 0 || field >= ZONE_OFFSET) {
            throw new IllegalArgumentException();
        }
        complete();
        int min = getMinimum(field);
        int max = getMaximum(field);
        long ny = era() == BCE ? 1 - internalGet(YEAR) : internalGet(YEAR);
        switch (field) {
        case AM_PM: case ERA: case YEAR: case MINUTE: case SECOND: case MILLISECOND:
            break;
        case HOUR: case HOUR_OF_DAY: {
            int rolledValue = getRolledValue(internalGet(field), amount, min, max);
            int hourOfDay = rolledValue;
            if (field == HOUR && internalGet(AM_PM) == PM) {
                hourOfDay += 12;
            }
            long fd = currentDay();
            long tod = ((hourOfDay * 60L + internalGet(MINUTE)) * 60 + internalGet(SECOND)) * 1000
                + internalGet(MILLISECOND);
            setTimeInMillis(fd * ONE_DAY + tod - internalGet(ZONE_OFFSET) - internalGet(DST_OFFSET));
            return;
        }
        case MONTH: {
            if (!isCutoverYear(ny)) {
                int mon = (internalGet(MONTH) + amount) % 12;
                if (mon < 0) {
                    mon += 12;
                }
                set(MONTH, mon);
                int monthLen = monthLength(mon);
                if (internalGet(DAY_OF_MONTH) > monthLen) {
                    set(DAY_OF_MONTH, monthLen);
                }
            } else {
                int yearLength = 12;
                int mon = (internalGet(MONTH) + amount) % yearLength;
                if (mon < 0) {
                    mon += yearLength;
                }
                set(MONTH, mon);
                int monthLen = getActualMaximum(DAY_OF_MONTH);
                if (internalGet(DAY_OF_MONTH) > monthLen) {
                    set(DAY_OF_MONTH, monthLen);
                }
            }
            return;
        }
        case WEEK_OF_YEAR: {
            max = getActualMaximum(WEEK_OF_YEAR);
            set(DAY_OF_WEEK, internalGet(DAY_OF_WEEK));
            int woy = internalGet(WEEK_OF_YEAR);
            int value = woy + amount;
            long fd = currentDay();
            if (!isCutoverYear(ny)) {
                long weekYear = getWeekYear();
                if (weekYear == ny) {
                    if (value > min && value < max) {
                        set(WEEK_OF_YEAR, value);
                        return;
                    }
                    long day1 = fd - (7 * (woy - min));
                    if (ymd(day1)[0] != ny) {
                        min++;
                    }
                    fd += 7 * (max - internalGet(WEEK_OF_YEAR));
                    if (ymd(fd)[0] != ny) {
                        max--;
                    }
                } else {
                    if (weekYear > ny) {
                        if (amount < 0) {
                            amount++;
                        }
                        woy = max;
                    } else {
                        if (amount > 0) {
                            amount -= woy - max;
                        }
                        woy = min;
                    }
                }
                int newWeekOfYear = getRolledValue(woy, amount, min, max);
                if (newWeekOfYear == 1 && isInvalidWeek1() && amount > 0) {
                    newWeekOfYear++;
                }
                set(field, newWeekOfYear);
                return;
            }
            long day1 = fd - (7 * (woy - min));
            if (ymd(day1)[0] != ny) {
                min++;
            }
            fd += 7 * (max - woy);
            if (ymd(fd)[0] != ny) {
                max--;
            }
            value = getRolledValue(woy, amount, min, max) - 1;
            long[] d = ymd(day1 + value * 7L);
            set(MONTH, (int) d[1] - 1);
            set(DAY_OF_MONTH, (int) d[2]);
            return;
        }
        case WEEK_OF_MONTH: {
            boolean cutover = isCutoverYear(ny);
            int dow = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
            if (dow < 0) {
                dow += 7;
            }
            long fd = currentDay();
            long month1 = month1(fd);
            int monthLength = actualMonthLength(month1);
            long monthDay1st = dayOfWeekOnOrBefore(month1 + 6, getFirstDayOfWeek());
            if ((int) (monthDay1st - month1) >= getMinimalDaysInFirstWeek()) {
                monthDay1st -= 7;
            }
            max = getActualMaximum(field);
            int value = getRolledValue(internalGet(field), amount, 1, max) - 1;
            long nfd = monthDay1st + value * 7L + dow;
            if (nfd < month1) {
                nfd = month1;
            } else if (nfd >= (month1 + monthLength)) {
                nfd = month1 + monthLength - 1;
            }
            int dayOfMonth = cutover ? (int) ymd(nfd)[2] : (int) (nfd - month1) + 1;
            set(DAY_OF_MONTH, dayOfMonth);
            return;
        }
        case DAY_OF_MONTH: {
            if (!isCutoverYear(ny)) {
                max = monthLength(internalGet(MONTH));
                break;
            }
            long fd = currentDay();
            long month1 = month1(fd);
            int value = getRolledValue((int) (fd - month1), amount, 0, actualMonthLength(month1) - 1);
            set(DAY_OF_MONTH, (int) ymd(month1 + value)[2]);
            return;
        }
        case DAY_OF_YEAR: {
            max = getActualMaximum(field);
            if (!isCutoverYear(ny)) {
                break;
            }
            long fd = currentDay();
            long jan1 = fd - internalGet(DAY_OF_YEAR) + 1;
            int value = getRolledValue((int) (fd - jan1) + 1, amount, min, max);
            long[] d = ymd(jan1 + value - 1);
            set(MONTH, (int) d[1] - 1);
            set(DAY_OF_MONTH, (int) d[2]);
            return;
        }
        case DAY_OF_WEEK: {
            if (!isCutoverYear(ny)) {
                int weekOfYear = internalGet(WEEK_OF_YEAR);
                if (weekOfYear > 1 && weekOfYear < 52) {
                    set(WEEK_OF_YEAR, weekOfYear);
                    max = SATURDAY;
                    break;
                }
            }
            amount %= 7;
            if (amount == 0) {
                return;
            }
            long fd = currentDay();
            long dowFirst = dayOfWeekOnOrBefore(fd, getFirstDayOfWeek());
            fd += amount;
            if (fd < dowFirst) {
                fd += 7;
            } else if (fd >= dowFirst + 7) {
                fd -= 7;
            }
            long[] d = ymd(fd);
            set(ERA, d[0] <= 0 ? BCE : CE);
            set((int) (d[0] <= 0 ? 1 - d[0] : d[0]), (int) d[1] - 1, (int) d[2]);
            return;
        }
        case DAY_OF_WEEK_IN_MONTH: {
            min = 1;
            if (!isCutoverYear(ny)) {
                int dom = internalGet(DAY_OF_MONTH);
                int monthLength = monthLength(internalGet(MONTH));
                int lastDays = monthLength % 7;
                max = monthLength / 7;
                int x = (dom - 1) % 7;
                if (x < lastDays) {
                    max++;
                }
                set(DAY_OF_WEEK, internalGet(DAY_OF_WEEK));
                break;
            }
            long fd = currentDay();
            long month1 = month1(fd);
            int monthLength = actualMonthLength(month1);
            int lastDays = monthLength % 7;
            max = monthLength / 7;
            int x = (int) (fd - month1) % 7;
            if (x < lastDays) {
                max++;
            }
            int value = getRolledValue(internalGet(field), amount, min, max) - 1;
            fd = month1 + value * 7L + x;
            set(DAY_OF_MONTH, (int) ymd(fd)[2]);
            return;
        }
        default:
            break;
        }
        set(field, getRolledValue(internalGet(field), amount, min, max));
    }

    /** JDK's isInvalidWeek1: whether week 1 of the current year is too short to exist. */
    private boolean isInvalidWeek1() {
        long jan1 = gregorianDays(internalGet(YEAR), 1, 1);
        int jan1Dow = dayOfWeekOf(jan1);
        int daysInFirstWeek;
        if (getFirstDayOfWeek() <= jan1Dow) {
            daysInFirstWeek = 7 - jan1Dow + getFirstDayOfWeek();
        } else {
            daysInFirstWeek = getFirstDayOfWeek() - jan1Dow;
        }
        int endDow = getFirstDayOfWeek() - 1 == 0 ? 7 : getFirstDayOfWeek() - 1;
        return daysInFirstWeek >= getMinimalDaysInFirstWeek()
            && !dayInMinWeek(internalGet(DAY_OF_WEEK), jan1Dow, endDow);
    }

    private static boolean dayInMinWeek(int day, int startDay, int endDay) {
        if (endDay >= startDay) {
            return (day >= startDay && day <= endDay);
        }
        return (day >= startDay || day <= endDay);
    }

    @Override
    public boolean isWeekDateSupported() {
        return true;
    }

    /** The week year, as the JDK's GregorianCalendar computes it. */
    @Override
    public int getWeekYear() {
        int year = get(YEAR);
        if (era() == BCE) {
            year = 1 - year;
        }
        if (year > gregorianCutoverYear + 1) {
            int weekOfYear = internalGet(WEEK_OF_YEAR);
            if (internalGet(MONTH) == JANUARY) {
                if (weekOfYear >= 52) {
                    --year;
                }
            } else {
                if (weekOfYear == 1) {
                    ++year;
                }
            }
            return year;
        }
        int dayOfYear = internalGet(DAY_OF_YEAR);
        int maxDayOfYear = getActualMaximum(DAY_OF_YEAR);
        int minimalDays = getMinimalDaysInFirstWeek();
        if (dayOfYear > minimalDays && dayOfYear < (maxDayOfYear - 6)) {
            return year;
        }
        GregorianCalendar cal = (GregorianCalendar) clone();
        cal.setLenient(true);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(DAY_OF_YEAR, 1);
        cal.complete();
        int delta = getFirstDayOfWeek() - cal.get(DAY_OF_WEEK);
        if (delta != 0) {
            if (delta < 0) {
                delta += 7;
            }
            cal.add(DAY_OF_YEAR, delta);
        }
        int minDayOfYear = cal.get(DAY_OF_YEAR);
        if (dayOfYear < minDayOfYear) {
            if (minDayOfYear <= minimalDays) {
                --year;
            }
        } else {
            cal.set(YEAR, year + 1);
            cal.set(DAY_OF_YEAR, 1);
            cal.complete();
            int del = getFirstDayOfWeek() - cal.get(DAY_OF_WEEK);
            if (del != 0) {
                if (del < 0) {
                    del += 7;
                }
                cal.add(DAY_OF_YEAR, del);
            }
            minDayOfYear = cal.get(DAY_OF_YEAR) - 1;
            if (minDayOfYear == 0) {
                minDayOfYear = 7;
            }
            if (minDayOfYear >= minimalDays) {
                int days = maxDayOfYear - dayOfYear + 1;
                if (days <= (7 - minDayOfYear)) {
                    ++year;
                }
            }
        }
        return year;
    }

    @Override
    public int getMinimum(int field) {
        return MIN_VALUES[field];
    }

    @Override
    public int getMaximum(int field) {
        return MAX_VALUES[field];
    }

    @Override
    public int getGreatestMinimum(int field) {
        return MIN_VALUES[field];
    }

    @Override
    public int getLeastMaximum(int field) {
        return LEAST_MAX_VALUES[field];
    }

    @Override
    public int getActualMinimum(int field) {
        return getMinimum(field);
    }

    @Override
    public int getActualMaximum(int field) {
        complete();
        long y = internalGet(ERA) == BCE ? 1 - internalGet(YEAR) : internalGet(YEAR);
        switch (field) {
        case DAY_OF_MONTH:
            return monthLength(internalGet(MONTH), (int) y);
        case DAY_OF_YEAR:
            return (int) (jan1(y + 1) - jan1(y));
        case WEEK_OF_MONTH: {
            if (isCutoverYear(y)) {
                // the JDK's way in the cutover year: count the month's weeks
                GregorianCalendar gc = (GregorianCalendar) clone();
                int yy = gc.internalGet(YEAR);
                int mm = gc.internalGet(MONTH);
                int value;
                do {
                    value = gc.get(WEEK_OF_MONTH);
                    gc.add(WEEK_OF_MONTH, +1);
                } while (gc.get(YEAR) == yy && gc.get(MONTH) == mm);
                return value;
            }
            // as the JDK computes it, a BC year's weeks are those of the Julian AD year of the
            // same number (its CalendarDate is set from the year of the era)
            int month = internalGet(MONTH);
            long first;
            int monthLength;
            if (internalGet(ERA) == BCE) {
                first = julianFirst(internalGet(YEAR), month);
                monthLength = (int) (julianFirst(internalGet(YEAR), month + 1) - first);
            } else {
                first = fixedDate(y, month, 1);
                monthLength = (int) (fixedDate(y, month + 1, 1) - first);
            }
            int dayOfWeek = dayOfWeekOf(first) - getFirstDayOfWeek();
            if (dayOfWeek < 0) {
                dayOfWeek += 7;
            }
            int nDaysFirstWeek = 7 - dayOfWeek;
            int value = 3;
            if (nDaysFirstWeek >= getMinimalDaysInFirstWeek()) {
                value++;
            }
            monthLength -= nDaysFirstWeek + 7 * 3;
            if (monthLength > 0) {
                value++;
                if (monthLength > 7) {
                    value++;
                }
            }
            return value;
        }
        case WEEK_OF_YEAR: {
            if (isCutoverYear(y)) {
                // the JDK's way in the cutover year: the week of the year's last day (or of the
                // day a week before, when that last week belongs to the next year)
                GregorianCalendar gc = (GregorianCalendar) clone();
                int maxDayOfYear = getActualMaximum(DAY_OF_YEAR);
                gc.set(DAY_OF_YEAR, maxDayOfYear);
                int value = gc.get(WEEK_OF_YEAR);
                if (gc.get(MONTH) == DECEMBER && value == 1) {
                    gc.set(DAY_OF_YEAR, maxDayOfYear - 7);
                    value = gc.get(WEEK_OF_YEAR);
                }
                return value;
            }
            long jan1 = internalGet(ERA) == BCE ? julianFirst(internalGet(YEAR), JANUARY) : jan1(y);
            int dayOfWeek = dayOfWeekOf(jan1) - getFirstDayOfWeek();
            if (dayOfWeek < 0) {
                dayOfWeek += 7;
            }
            int value = 52;
            int magic = dayOfWeek + getMinimalDaysInFirstWeek() - 1;
            if ((magic == 6) || (isLeapYear((int) y) && (magic == 5 || magic == 12))) {
                value++;
            }
            return value;
        }
        case DAY_OF_WEEK_IN_MONTH: {
            long first = fixedDate(y, internalGet(MONTH), 1);
            int ndays = (int) (fixedDate(y, internalGet(MONTH) + 1, 1) - first);
            int x = internalGet(DAY_OF_WEEK) - dayOfWeekOf(first);
            if (x < 0) {
                x += 7;
            }
            ndays -= x;
            return (ndays + 6) / 7;
        }
        case YEAR:
            return internalGet(ERA) == BCE ? 292269055 : 292278994;
        default:
            return getMaximum(field);
        }
    }

    @Override
    public String getCalendarType() {
        return "gregory";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GregorianCalendar && super.equals(obj)
            && gregorianCutover == ((GregorianCalendar) obj).gregorianCutover;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (int) (gregorianCutoverDay + 719163);
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public TimeZone getTimeZone() {
        return super.getTimeZone();
    }

    @Override
    public void setTimeZone(TimeZone zone) {
        super.setTimeZone(zone);
    }
}
