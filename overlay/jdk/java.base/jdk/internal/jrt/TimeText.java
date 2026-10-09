/*
 * jrt's own jdk.internal.jrt.TimeText for the Go build (doc/go/JRT-NOTES.md, phase 2B "Dates"). Parts of its code
 * (ISO_INSTANT's printing: InstantPrinterParser.format and LocalDate.toString's year) are transcribed from openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/time/format/DateTimeFormatter.java, Copyright (c) Oracle and/or its affiliates,
 * under the GNU General Public License version 2 with the Classpath Exception (LICENSE.md);
 * the rest is Arbace's own.
 */
package jdk.internal.jrt;

/**
 * jrt's own (doc/go/JRT-NOTES.md, phase 2B "Dates"): the texts java.time prints, written out for
 * the Go build, where java.time.format is outside the closed world. {@code Instant.toString}'s
 * variant (overlay/jdk/variants/Instant.clj) calls {@link #instant}, which gives
 * {@code DateTimeFormatter.ISO_INSTANT}'s output: the proleptic ISO (Gregorian) calendar in UTC,
 * as {@code LocalDateTime.toString} prints a date and time, seconds always present, the fraction
 * in groups of three digits, years beyond 9999 with a sign, and years beyond the range of
 * {@code LocalDate} as {@code InstantPrinterParser} splits them into 10,000-year periods.
 */
public final class TimeText {
    private TimeText() {}

    private static final long SECONDS_PER_DAY = 86400L;
    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    /** The text of the instant {@code seconds} after the epoch plus {@code nanos}, as ISO_INSTANT formats it. */
    public static String instant(long seconds, int nanos) {
        StringBuilder buf = new StringBuilder(32);
        if (seconds >= -SECONDS_0000_TO_1970) {
            long zeroSecs = seconds - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
            long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
            long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
            long s = lo - SECONDS_0000_TO_1970;
            if (hi > 0) {
                buf.append('+').append(hi);
            }
            int second = dateTime(buf, s);
            if (second == 0) {
                buf.append(":00");
            }
        } else {
            long zeroSecs = seconds + SECONDS_0000_TO_1970;
            long hi = zeroSecs / SECONDS_PER_10000_YEARS;
            long lo = zeroSecs % SECONDS_PER_10000_YEARS;
            long s = lo - SECONDS_0000_TO_1970;
            int pos = buf.length();
            int second = dateTime(buf, s);
            if (second == 0) {
                buf.append(":00");
            }
            if (hi < 0) {
                if (yearOf(s) == -10_000) {
                    buf.replace(pos, pos + 2, Long.toString(hi - 1));
                } else if (lo == 0) {
                    buf.insert(pos, hi);
                } else {
                    buf.insert(pos + 1, Math.abs(hi));
                }
            }
        }
        if (nanos != 0) {
            buf.append('.');
            if (nanos % 1000_000 == 0) {
                buf.append(Integer.toString((nanos / 1000_000) + 1000).substring(1));
            } else if (nanos % 1000 == 0) {
                buf.append(Integer.toString((nanos / 1000) + 1000_000).substring(1));
            } else {
                buf.append(Integer.toString((nanos) + 1000_000_000).substring(1));
            }
        }
        buf.append('Z');
        return buf.toString();
    }

    /** The proleptic Gregorian year of the epoch second s (UTC). */
    private static long yearOf(long s) {
        return civil(Math.floorDiv(s, SECONDS_PER_DAY))[0];
    }

    /** {y, m, d} of a day count since 1970-01-01 in the proleptic Gregorian calendar. */
    static long[] civil(long days) {
        long z = days + 719468;
        long era = Math.floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        long y = yoe + era * 400 + (m <= 2 ? 1 : 0);
        return new long[] {y, m, d};
    }

    /**
     * Appends LocalDateTime.toString of the UTC date and time of epoch second s (no fraction):
     * the year as LocalDate prints it, -MM-dd, T, HH:mm and :ss when the second is not 0.
     * Returns the second.
     */
    private static int dateTime(StringBuilder buf, long s) {
        long days = Math.floorDiv(s, SECONDS_PER_DAY);
        int secs = (int) Math.floorMod(s, SECONDS_PER_DAY);
        long[] ymd = civil(days);
        int year = (int) ymd[0];
        int month = (int) ymd[1];
        int day = (int) ymd[2];
        int absYear = Math.abs(year);
        if (absYear < 1000) {
            if (year < 0) {
                buf.append(year - 10000).deleteCharAt(buf.length() - 5);
            } else {
                buf.append(year + 10000).deleteCharAt(buf.length() - 5);
            }
        } else {
            if (year > 9999) {
                buf.append('+');
            }
            buf.append(year);
        }
        buf.append(month < 10 ? "-0" : "-").append(month).append(day < 10 ? "-0" : "-").append(day);
        int hour = secs / 3600;
        int minute = (secs / 60) % 60;
        int second = secs % 60;
        buf.append('T').append(hour < 10 ? "0" : "").append(hour).append(minute < 10 ? ":0" : ":").append(minute);
        if (second > 0) {
            buf.append(second < 10 ? ":0" : ":").append(second);
        }
        return second;
    }
}
