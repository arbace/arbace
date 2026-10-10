/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package jdk.internal.jrt;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import sun.util.calendar.ZoneInfo;

/**
 * The default time zone as jrt's hand-written java.util.Date needs it (doc/go/JRT-NOTES.md,
 * "Time"): Date's local fields, toString's zone name and its deprecated local constructors are
 * the default zone's, as the JDK's Date computes them (its BaseCalendar over
 * TimeZone.getDefaultRef: a ZoneInfo's offsets by the instant and by the wall time, another
 * zone's raw offset and daylight saving). jrt's Date cannot name the translated TimeZone; c2g sets
 * jrt's DateZone hooks to these methods.
 */
public final class DefaultZone {
    private DefaultZone() {
    }

    /** The default zone's offset in milliseconds at the instant utc. */
    public static int offset(long utc) {
        TimeZone tz = TimeZone.getDefault();
        if (tz instanceof ZoneInfo zi) {
            return zi.getOffsets(utc, null);
        }
        return tz.getOffset(utc);
    }

    /** The default zone's offset in milliseconds at the local time local (the wall time). */
    public static int offsetByWall(long local) {
        TimeZone tz = TimeZone.getDefault();
        if (tz instanceof ZoneInfo zi) {
            int[] offsets = new int[2];
            zi.getOffsetsByWall(local, offsets);
            return offsets[0] + offsets[1];
        }
        int raw = tz.getRawOffset();
        return tz.getOffset(local - raw);
    }

    /** The default zone's short name at the instant utc, in US English (Date.toString's). */
    public static String name(long utc) {
        TimeZone tz = TimeZone.getDefault();
        boolean daylight;
        if (tz instanceof ZoneInfo zi) {
            int[] offsets = new int[2];
            zi.getOffsets(utc, offsets);
            daylight = offsets[1] != 0;
        } else {
            daylight = tz.getOffset(utc) != tz.getRawOffset();
        }
        return tz.getDisplayName(daylight, TimeZone.SHORT, Locale.US);
    }
}
