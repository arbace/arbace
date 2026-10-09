/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package sun.util.calendar;

import java.util.Date;
import java.util.TimeZone;

/**
 * jrt's own sun.util.calendar.ZoneInfo (doc/go/JRT-NOTES.md, phase 2B "Dates"): the zone class
 * TimeZone.getTimeZone answers, as on the JVM, here always a fixed offset without daylight
 * saving time (the JDK's reads the time zone database, tzdb.dat, which the Go build does not
 * have). Its region IDs are the zero-offset ones and Etc/GMT+h, Etc/GMT-h (whose sign is the
 * POSIX one: Etc/GMT+5 is five hours behind GMT).
 */
public class ZoneInfo extends TimeZone {
    private int rawOffset;

    private static final String[] ZERO_IDS = {
        "Etc/GMT", "Etc/GMT+0", "Etc/GMT-0", "Etc/GMT0", "Etc/Greenwich", "Etc/UCT", "Etc/UTC",
        "Etc/Universal", "Etc/Zulu", "GMT", "GMT0", "Greenwich", "UCT", "UTC", "Universal", "Zulu"
    };

    public ZoneInfo() {
    }

    public ZoneInfo(String ID, int rawOffset) {
        setID(ID);
        this.rawOffset = rawOffset;
    }

    public int getOffset(long date) {
        return rawOffset;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (era != java.util.GregorianCalendar.AD && era != java.util.GregorianCalendar.BC) {
            throw new IllegalArgumentException("Illegal era " + era);
        }
        if (month < 0 || month > 11) {
            throw new IllegalArgumentException("Illegal month " + month);
        }
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
        }
        if (milliseconds < 0 || milliseconds >= 86400000) {
            throw new IllegalArgumentException("Illegal millis " + milliseconds);
        }
        return rawOffset;
    }

    public synchronized void setRawOffset(int offsetMillis) {
        rawOffset = offsetMillis;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public boolean useDaylightTime() {
        return false;
    }

    public boolean observesDaylightTime() {
        return false;
    }

    public boolean inDaylightTime(Date date) {
        if (date == null) {
            throw new NullPointerException();
        }
        return false;
    }

    public int getDSTSavings() {
        return 0;
    }

    /** The zone of a region ID this class knows, or null. */
    public static TimeZone getTimeZone(String ID) {
        for (String z : ZERO_IDS) {
            if (z.equals(ID)) {
                return new ZoneInfo(ID, 0);
            }
        }
        if (ID.startsWith("Etc/GMT") && ID.length() > 8 && ID.length() <= 10) {
            char sign = ID.charAt(7);
            int h = 0;
            for (int i = 8; i < ID.length(); i++) {
                char c = ID.charAt(i);
                if (c < '0' || c > '9') {
                    return null;
                }
                h = h * 10 + (c - '0');
            }
            if (ID.charAt(8) == '0') {
                return null;
            }
            if (sign == '+' && h <= 12) {
                return new ZoneInfo(ID, -h * 3600000);
            }
            if (sign == '-' && h <= 14) {
                return new ZoneInfo(ID, h * 3600000);
            }
        }
        return null;
    }

    public static String[] getAvailableIDs() {
        String[] ids = new String[ZERO_IDS.length + 26];
        int n = 0;
        for (String z : ZERO_IDS) {
            ids[n++] = z;
        }
        for (int h = 1; h <= 12; h++) {
            ids[n++] = "Etc/GMT+" + h;
        }
        for (int h = 1; h <= 14; h++) {
            ids[n++] = "Etc/GMT-" + h;
        }
        java.util.Arrays.sort(ids);
        return ids;
    }

    public static String[] getAvailableIDs(int rawOffset) {
        String[] all = getAvailableIDs();
        int n = 0;
        for (String id : all) {
            if (getTimeZone(id).getRawOffset() == rawOffset) {
                n++;
            }
        }
        String[] ids = new String[n];
        n = 0;
        for (String id : all) {
            if (getTimeZone(id).getRawOffset() == rawOffset) {
                ids[n++] = id;
            }
        }
        return ids;
    }

    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return rawOffset == other.getRawOffset() && !other.useDaylightTime();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo that = (ZoneInfo) obj;
        return getID().equals(that.getID()) && rawOffset == that.rawOffset;
    }

    public int hashCode() {
        return rawOffset;
    }

    public String toString() {
        return getClass().getName() + "[id=\"" + getID() + "\"" + ",offset=" + rawOffset
            + ",dstSavings=0,useDaylight=false,transitions=0,lastRule=null]";
    }
}
