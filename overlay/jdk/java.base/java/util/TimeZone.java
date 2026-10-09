/*
 * jrt's own java.util.TimeZone for the Go build (doc/go/JRT-NOTES.md, phase 2B "Dates"). Parts of its code
 * (parseCustomTimeZone, and toCustomID of sun/util/calendar/ZoneInfoFile.java) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/util/TimeZone.java, Copyright (c) Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md);
 * the rest is Arbace's own.
 */
package java.util;

import sun.util.calendar.ZoneInfo;

/**
 * java.util.TimeZone for the Go build, behaving as the JDK's for the zones it has: the zones are fixed offsets (sun.util.calendar.ZoneInfo), the time
 * zone database and the locale providers being outside the closed world. getTimeZone knows the
 * zero-offset IDs (GMT, UTC and their aliases), Etc/GMT+h and Etc/GMT-h, and the custom IDs
 * GMT+h, GMT+hh, GMT+hhmm, GMT+h:mm, GMT+hh:mm (and -), normalized as the JDK normalizes them
 * (GMT+05:00); any other ID is GMT, as the JDK falls back for an unknown ID. The default zone is
 * GMT, as jrt's Date.
 */
public abstract class TimeZone implements java.io.Serializable, Cloneable {
    public static final int SHORT = 0;
    public static final int LONG = 1;

    static final String GMT_ID = "GMT";

    private String ID;

    private static volatile TimeZone defaultTimeZone;

    public TimeZone() {
    }

    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds);

    public int getOffset(long date) {
        if (inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    public abstract void setRawOffset(int offsetMillis);

    public abstract int getRawOffset();

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
    }

    public final String getDisplayName() {
        return getDisplayName(false, LONG, Locale.getDefault(Locale.Category.DISPLAY));
    }

    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }

    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * The zone's name in English for every locale (the root locale's names, the locale data
     * being outside the closed world), as the JDK names the zones jrt has: a custom ID by
     * itself, UTC and GMT and their aliases by their names, other zones as GMT+hh:mm.
     */
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        if (style != SHORT && style != LONG) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
        String id = getID();
        if (id.startsWith("GMT+") || id.startsWith("GMT-")) {
            return id;
        }
        switch (id) {
        case "UTC": case "UCT": case "Universal": case "Zulu":
        case "Etc/UTC": case "Etc/UCT": case "Etc/Universal": case "Etc/Zulu":
            return style == SHORT ? "UTC" : "Coordinated Universal Time";
        case "GMT": case "GMT0": case "Greenwich": case "Etc/GMT": case "Etc/GMT0": case "Etc/GMT+0":
        case "Etc/GMT-0": case "Etc/Greenwich":
            return style == SHORT ? "GMT" : "Greenwich Mean Time";
        default:
            int offset = getRawOffset();
            if (daylight) {
                offset += getDSTSavings();
            }
            return toCustomID(offset);
        }
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    public abstract boolean useDaylightTime();

    public boolean observesDaylightTime() {
        return useDaylightTime();
    }

    public abstract boolean inDaylightTime(Date date);

    public static synchronized TimeZone getTimeZone(String ID) {
        TimeZone tz = ZoneInfo.getTimeZone(ID);
        if (tz == null) {
            tz = parseCustomTimeZone(ID);
            if (tz == null) {
                tz = new ZoneInfo(GMT_ID, 0);
            }
        }
        return tz;
    }

    public static synchronized String[] getAvailableIDs(int rawOffset) {
        return ZoneInfo.getAvailableIDs(rawOffset);
    }

    public static synchronized String[] getAvailableIDs() {
        return ZoneInfo.getAvailableIDs();
    }

    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }

    static TimeZone getDefaultRef() {
        TimeZone defaultZone = defaultTimeZone;
        if (defaultZone == null) {
            defaultZone = new ZoneInfo(GMT_ID, 0);
            defaultTimeZone = defaultZone;
        }
        return defaultZone;
    }

    public static void setDefault(TimeZone zone) {
        defaultTimeZone = zone == null ? null : (TimeZone) zone.clone();
    }

    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset()
            && useDaylightTime() == other.useDaylightTime();
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /** GMT+hh:mm or GMT-hh:mm of an offset in milliseconds (ZoneInfoFile.toCustomID). */
    static String toCustomID(int gmtOffset) {
        char sign;
        int offset = gmtOffset / 60000;
        if (offset >= 0) {
            sign = '+';
        } else {
            sign = '-';
            offset = -offset;
        }
        int hh = offset / 60;
        int mm = offset % 60;
        char[] buf = new char[] { 'G', 'M', 'T', sign, '0', '0', ':', '0', '0' };
        if (hh >= 10) {
            buf[4] += (char) (hh / 10);
        }
        buf[5] += (char) (hh % 10);
        if (mm != 0) {
            buf[7] += (char) (mm / 10);
            buf[8] += (char) (mm % 10);
        }
        return new String(buf);
    }

    /** jdk26u's TimeZone.parseCustomTimeZone: a custom ID's zone, or null. */
    private static TimeZone parseCustomTimeZone(String id) {
        int length;
        if ((length = id.length()) < (GMT_ID.length() + 2) || id.indexOf(GMT_ID) != 0) {
            return null;
        }
        int index = GMT_ID.length();
        boolean negative = false;
        char c = id.charAt(index++);
        if (c == '-') {
            negative = true;
        } else if (c != '+') {
            return null;
        }
        int hours = 0;
        int minutes = 0;
        int num = 0;
        int countDelim = 0;
        int len = 0;
        while (index < length) {
            c = id.charAt(index++);
            if (c == ':') {
                if (countDelim > 1) {
                    return null;
                }
                if (len == 0 || len > 2) {
                    return null;
                }
                if (countDelim == 0) {
                    hours = num;
                } else if (countDelim == 1) {
                    minutes = num;
                }
                countDelim++;
                num = 0;
                len = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
            num = num * 10 + (c - '0');
            len++;
        }
        if (index != length) {
            return null;
        }
        if (countDelim == 0) {
            if (len <= 2) {
                hours = num;
                minutes = 0;
                num = 0;
            } else if (len <= 4) {
                hours = num / 100;
                minutes = num % 100;
                num = 0;
            } else {
                return null;
            }
        } else if (countDelim == 1) {
            if (len == 2) {
                minutes = num;
                num = 0;
            } else {
                return null;
            }
        } else {
            if (len != 2) {
                return null;
            }
        }
        if (hours > 23 || minutes > 59 || num > 59) {
            return null;
        }
        int gmtOffset = (hours * 3_600 + minutes * 60 + num) * 1_000;
        if (gmtOffset == 0) {
            return new ZoneInfo(negative ? "GMT-00:00" : "GMT+00:00", 0);
        }
        int offset = negative ? -gmtOffset : gmtOffset;
        return new ZoneInfo(toCustomID(offset), offset);
    }
}
