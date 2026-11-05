/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F5 (write-ahead)"): jrt's
 * sun.util.logging.internal.LoggingProviderImpl, written for jrt in place of jdk26u's
 * (src/java.logging/share/classes/sun/util/logging/internal/LoggingProviderImpl.java at
 * https://github.com/openjdk/jdk26u baf63fb), which is java.logging's System.LoggerFinder: a
 * DefaultLoggerFinder whose loggers wrap java.util.logging's. System.Logger and its finders are not
 * in the Go build (JCS136), so this one is only the access LogManager registers from its static
 * initializer (LogManager.LoggingProviderAccess): the interface LogManagerAccess and its holder,
 * with jdk26u's names and signatures, from which a later LoggerFinder binds System.Logger to
 * java.util.logging directly (no ServiceLoader). Translated as java.base's, as java.logging's
 * classes are (test/jrt/jrt_sources.ae's logging-sources).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.util.logging.internal;

import java.util.logging.LogManager;

/** java.logging's access for a System.LoggerFinder, without the finder. */
public final class LoggingProviderImpl {

    private LoggingProviderImpl() {
    }

    /** LogManager's way to make the logger of a module (LogManager.LoggingProviderAccess). */
    public static interface LogManagerAccess {
        java.util.logging.Logger demandLoggerFor(LogManager manager, String name, Module module);
    }

    private static volatile LogManagerAccess logManagerAccess;

    /** The access LogManager registered, LogManager initialized first when none is. */
    public static LogManagerAccess getLogManagerAccess() {
        LogManagerAccess access = logManagerAccess;
        if (access == null) LogManager.getLogManager();
        return logManagerAccess;
    }

    /** Called by LogManager's static initializer. */
    public static void setLogManagerAccess(LogManagerAccess accessLoggers) {
        logManagerAccess = accessLoggers;
    }
}
