/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F24 (write-ahead)"): System.Logger
 * for the classes of the Go build that log through it, while java.lang.System is jrt's hand-written Go, which
 * has no System.Logger, System.Logger.Level or System.LoggerFinder (JCS155 (b), as F3's SSLLogger and F12's
 * PlatformLogger; System.Logger in jrt itself is JCS155 (a), JCS250). jdk.httpserver's classes (F24) are derived
 * from jdk26u's with this Logger in place of System.Logger (test/jrt/derive.ae), so they keep jdk26u's code.
 *
 * Logger is java.lang.System.Logger's interface as jdk26u declares it (src/java.base/share/classes/java/lang/
 * System.java at https://github.com/openjdk/jdk26u baf63fb): the same Level (the same constants, severities and
 * names), the same abstract methods and the same default methods. getLogger(name) is System.getLogger(name) as
 * the JDK answers it for a system module when java.logging is present: a logger whose java.util.logging logger
 * LogManager demands for the module when it is first used (jdk.internal.logger.LazyLoggers' lazy logger over
 * java.logging's LoggerFinder, sun.util.logging.internal.LoggingProviderImpl), here through F5's
 * LoggingProviderImpl access as PlatformLogger's JulBridge demands it; its methods are LoggingProviderImpl's
 * JULWrapper's System.Logger half (src/java.logging/share/classes/sun/util/logging/internal/
 * LoggingProviderImpl.java at baf63fb), the levels mapped by its spi2JulLevelMapping. The interface's and the
 * wrapper's code are jdk26u's: Copyright (c) 2015, 2025, Oracle and/or its affiliates, under the GNU General
 * Public License version 2 with the Classpath Exception (LICENSE.md); the rest Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import sun.util.logging.internal.LoggingProviderImpl;

/** System.Logger for the Go build's own classes: jdk26u's interface, java.util.logging's loggers. */
public final class SystemLoggers {

    private SystemLoggers() {
    }

    /** java.lang.System.Logger, as jdk26u declares it. */
    public interface Logger {

        /** java.lang.System.Logger.Level. */
        public enum Level {
            // for convenience, we're reusing java.util.logging.Level int values
            // the mapping logic in sun.util.logging.PlatformLogger depends
            // on this.
            ALL(Integer.MIN_VALUE),  // typically mapped to/from j.u.l.Level.ALL
            TRACE(400),   // typically mapped to/from j.u.l.Level.FINER
            DEBUG(500),   // typically mapped to/from j.u.l.Level.FINEST/FINE/CONFIG
            INFO(800),    // typically mapped to/from j.u.l.Level.INFO
            WARNING(900), // typically mapped to/from j.u.l.Level.WARNING
            ERROR(1000),  // typically mapped to/from j.u.l.Level.SEVERE
            OFF(Integer.MAX_VALUE);  // typically mapped to/from j.u.l.Level.OFF

            private final int severity;

            private Level(int severity) {
                this.severity = severity;
            }

            public final String getName() {
                return name();
            }

            public final int getSeverity() {
                return severity;
            }
        }

        public String getName();

        public boolean isLoggable(Level level);

        public default void log(Level level, String msg) {
            log(level, (ResourceBundle) null, msg, (Object[]) null);
        }

        public default void log(Level level, Supplier<String> msgSupplier) {
            Objects.requireNonNull(msgSupplier);
            if (isLoggable(Objects.requireNonNull(level))) {
                log(level, (ResourceBundle) null, msgSupplier.get(), (Object[]) null);
            }
        }

        public default void log(Level level, Object obj) {
            Objects.requireNonNull(obj);
            if (isLoggable(Objects.requireNonNull(level))) {
                this.log(level, (ResourceBundle) null, obj.toString(), (Object[]) null);
            }
        }

        public default void log(Level level, String msg, Throwable thrown) {
            this.log(level, null, msg, thrown);
        }

        public default void log(Level level, Supplier<String> msgSupplier,
                Throwable thrown) {
            Objects.requireNonNull(msgSupplier);
            if (isLoggable(Objects.requireNonNull(level))) {
                this.log(level, null, msgSupplier.get(), thrown);
            }
        }

        public default void log(Level level, String format, Object... params) {
            this.log(level, null, format, params);
        }

        public void log(Level level, ResourceBundle bundle, String msg,
                Throwable thrown);

        public void log(Level level, ResourceBundle bundle, String format,
                Object... params);
    }

    /**
     * System.getLogger(name) for a class of a system module: a logger over the java.util.logging logger
     * LogManager demands for name, demanded when first used.
     */
    public static Logger getLogger(String name) {
        Objects.requireNonNull(name);
        return new JulLogger(name);
    }

    /**
     * java.lang.System.getLogger(name) as the Go build answers it (queue 562, the user's JMS4; j2g's System.getLogger
     * calls it): getLogger(name)'s logger as a java.lang.System.Logger (the top-level overlay System$Logger, whose
     * Level has this Level's constants in the same order).
     */
    public static System.Logger getSystemLogger(String name) {
        return new SystemLogger(getLogger(name));
    }

    /** A Logger as a java.lang.System.Logger: the levels mapped by their ordinals, the calls passed on. */
    private static final class SystemLogger implements System.Logger {

        private final Logger logger;

        SystemLogger(Logger logger) {
            this.logger = logger;
        }

        private static Logger.Level level(System.Logger.Level level) {
            return level == null ? null : Logger.Level.values()[level.ordinal()];
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public boolean isLoggable(System.Logger.Level level) {
            return logger.isLoggable(level(level));
        }

        @Override
        public void log(System.Logger.Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            logger.log(level(level), bundle, msg, thrown);
        }

        @Override
        public void log(System.Logger.Level level, ResourceBundle bundle, String format, Object... params) {
            logger.log(level(level), bundle, format, params);
        }
    }

    /**
     * jdk.internal.logger.DefaultLoggerFinder.isSystem(m), as jdk26u answers it: whether the module's loader is a
     * system domain loader (the bootstrap or the platform loader). java.util.logging's Logger and LogManager, derived
     * from jdk26u's (test/jrt/derive.ae, h-java-deferred), ask it here: jdk.internal.logger is not in the Go build.
     */
    public static boolean isSystem(Module m) {
        return jdk.internal.misc.VM.isSystemDomainLoader(m.getClassLoader());
    }

    /**
     * LoggingProviderImpl.JULWrapper's System.Logger half, over the java.util.logging logger demanded
     * lazily (LazyLoggers' JdkLazyLogger wraps the JULWrapper so in the JDK).
     */
    private static final class JulLogger implements Logger {

        private static final java.util.logging.Level[] spi2JulLevelMapping = {
                java.util.logging.Level.ALL,     // mapped from ALL
                java.util.logging.Level.FINER,   // mapped from TRACE
                java.util.logging.Level.FINE,    // mapped from DEBUG
                java.util.logging.Level.INFO,    // mapped from INFO
                java.util.logging.Level.WARNING, // mapped from WARNING
                java.util.logging.Level.SEVERE,  // mapped from ERROR
                java.util.logging.Level.OFF      // mapped from OFF
        };

        private final String name;
        private volatile java.util.logging.Logger julLogger;

        JulLogger(String name) {
            this.name = name;
        }

        private java.util.logging.Logger julLogger() {
            java.util.logging.Logger l = julLogger;
            if (l == null) {
                synchronized (this) {
                    l = julLogger;
                    if (l == null) {
                        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
                        l = LoggingProviderImpl.getLogManagerAccess()
                                .demandLoggerFor(manager, name, SystemLoggers.class.getModule());
                        julLogger = l;
                    }
                }
            }
            return l;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(Level level) {
            return julLogger().isLoggable(toJUL(level));
        }

        @Override
        public void log(Level level, String msg) {
            julLogger().log(toJUL(level), msg);
        }

        @Override
        public void log(Level level,
                        Supplier<String> msgSupplier) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(msgSupplier);
            julLogger().log(toJUL(level), msgSupplier);
        }

        @Override
        public void log(Level level, Object obj) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(obj);
            julLogger().log(toJUL(level), () -> obj.toString());
        }

        @Override
        public void log(Level level,
                        String msg, Throwable thrown) {
            julLogger().log(toJUL(level), msg, thrown);
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier,
                        Throwable thrown) {
            // We need to check for null here to satisfy the contract
            // of System.Logger - because the underlying implementation
            // of julLogger will check for it only if the level is
            // loggable
            Objects.requireNonNull(msgSupplier);
            julLogger().log(toJUL(level), thrown, msgSupplier);
        }

        @Override
        public void log(Level level,
                        String format, Object... params) {
            julLogger().log(toJUL(level), format, params);
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String key, Throwable thrown) {
            julLogger().logrb(toJUL(level), bundle, key, thrown);
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String format, Object... params) {
            julLogger().logrb(toJUL(level), bundle, format, params);
        }

        static java.util.logging.Level toJUL(Level level) {
            if (level == null) return null;
            assert level.ordinal() < spi2JulLevelMapping.length;
            return spi2JulLevelMapping[level.ordinal()];
        }
    }
}
