/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F16, "As built: F16 (write-ahead)"): jrt's
 * java.sql.DriverManager, the log writer of jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.sql/share/classes/java/sql/DriverManager.java) alone: java.sql's SQLException and
 * SQLWarning, which the Go build has for javax.sql.rowset.serial's SerialBlob and SerialClob (F16),
 * log to it when one is set. The driver registry (registerDriver, getDriver, getDrivers,
 * getConnection, the login timeout) is not here: the Go build loads no driver (no ServiceLoader, no
 * class loading by name of a jdbc.drivers list), and JDBC's interfaces (Connection, Driver, ...)
 * are not in it. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.sql;

/** The logging part of the JDK's DriverManager. */
public class DriverManager {

    private static volatile java.io.PrintWriter logWriter = null;
    private static final Object logSync = new Object();

    private DriverManager() {
    }

    /** The log writer, or null. */
    public static java.io.PrintWriter getLogWriter() {
        return logWriter;
    }

    /** Sets the log writer; null disables logging. */
    public static void setLogWriter(java.io.PrintWriter out) {
        logWriter = out;
    }

    /** Prints a message to the log writer, if one is set, and flushes it. */
    public static void println(String message) {
        synchronized (logSync) {
            if (logWriter != null) {
                logWriter.println(message);
                logWriter.flush();
            }
        }
    }
}
