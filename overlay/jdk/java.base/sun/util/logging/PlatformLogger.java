/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F12, "As built: F12 (write-ahead)"): jrt's
 * sun.util.logging.PlatformLogger, jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/sun/util/logging/PlatformLogger.java) transcribed with what the Go build
 * lacks taken out: java.lang.System is jrt's hand-written class, which has no System.Logger,
 * System.Logger.Level or System.LoggerFinder (JCS138, JCS155 (b): System.Logger is its own later fix),
 * so there are no lazy loggers (jdk.internal.logger.LazyLoggers) to bridge to: Level keeps its
 * constants, severities, intValue and valueOf without systemLevel(), Bridge and ConfigurableBridge
 * are jdk26u's but for Bridge.convert(System.Logger), toPlatformLevel(System.Logger.Level) is gone,
 * and getLogger's bridge is the one the JDK ends with when java.logging is present (its LoggerFinder,
 * java.logging's LoggingProviderImpl, gives a system logger of java.base a JULWrapper over the
 * java.util.logging logger LogManager demands for the module): JulBridge below, jdk26u's JULWrapper's
 * PlatformLogger half transcribed (src/java.logging/share/classes/sun/util/logging/internal/
 * LoggingProviderImpl.java), its logger demanded through F5's LoggingProviderImpl access when first
 * used, as the JDK's lazy logger demands it. Copyright (c) Oracle and/or its affiliates, under the GNU
 * General Public License version 2 with the Classpath Exception, as the notice above says (LICENSE.md).
 */

package sun.util.logging;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import sun.util.logging.internal.LoggingProviderImpl;

/**
 * Platform logger provides an API for the JRE components to log
 * messages.  This enables the runtime components to eliminate the
 * static dependency of the logging facility and also defers the
 * java.util.logging initialization until it is enabled.
 * In addition, the PlatformLogger API can be used if the logging
 * module does not exist.
 *
 * If the logging facility is not enabled, the platform loggers
 * will output log messages per the default logging configuration
 * (see below). In this implementation, it does not log
 * the stack frame information issuing the log message.
 *
 * When the logging facility is enabled (at startup or runtime),
 * the backend logger will be created for each platform
 * logger and all log messages will be forwarded to the Logger
 * to handle.
 *
 * The PlatformLogger uses an underlying PlatformLogger.Bridge instance
 * obtained by calling {@link PlatformLogger.Bridge#convert PlatformLogger.Bridge.convert(}
 * {@link jdk.internal.logger.LazyLoggers#getLazyLogger(java.lang.String, java.lang.Class)
 * jdk.internal.logger.LazyLoggers#getLazyLogger(name, PlatformLogger.class))}.
 *
 * Logging facility is "enabled" when one of the following
 * conditions is met:
 * 1) ServiceLoader.load({@link java.lang.System.LoggerFinder LoggerFinder.class},
 *    ClassLoader.getSystemClassLoader()).iterator().hasNext().
 * 2) ServiceLoader.loadInstalled({@link jdk.internal.logger.DefaultLoggerFinder}).iterator().hasNext(),
 *    and 2.1) a system property "java.util.logging.config.class" or
 *             "java.util.logging.config.file" is set
 *     or  2.2) java.util.logging.LogManager or java.util.logging.Logger
 *              is referenced that will trigger the logging initialization.
 *
 * Default logging configuration:
 *
 *   No LoggerFinder service implementation declared
 *   global logging level = INFO
 *   handlers = java.util.logging.ConsoleHandler
 *   java.util.logging.ConsoleHandler.level = INFO
 *   java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
 *
 * Limitation:
 * {@code <JAVA_HOME>/conf/logging.properties} is the system-wide logging
 * configuration defined in the specification and read in the
 * default case to configure any java.util.logging.Logger instances.
 * Platform loggers will not detect if {@code <JAVA_HOME>/conf/logging.properties}
 * is modified. In other words, unless the java.util.logging API
 * is used at runtime or the logging system properties is set,
 * the platform loggers will use the default setting described above.
 * The platform loggers are designed for JDK developers use and
 * this limitation can be workaround with setting
 * -Djava.util.logging.config.file system property.
 * <br>
 * Calling PlatformLogger.setLevel will not work when there is a custom
 * LoggerFinder installed - and as a consequence {@link #setLevel setLevel}
 * is now deprecated.
 *
 * @since 1.7
 */
public class PlatformLogger {

    /**
     * PlatformLogger logging levels.
     */
    public static enum Level {
        // The name and value must match that of {@code java.util.logging.Level}s.
        // Declare in ascending order of the given value for binary search.
        ALL,
        FINEST,
        FINER,
        FINE,
        CONFIG,
        INFO,
        WARNING,
        SEVERE,
        OFF;

        // The integer values must match that of {@code java.util.logging.Level}
        // objects.
        private static final int SEVERITY_OFF     = Integer.MAX_VALUE;
        private static final int SEVERITY_SEVERE  = 1000;
        private static final int SEVERITY_WARNING = 900;
        private static final int SEVERITY_INFO    = 800;
        private static final int SEVERITY_CONFIG  = 700;
        private static final int SEVERITY_FINE    = 500;
        private static final int SEVERITY_FINER   = 400;
        private static final int SEVERITY_FINEST  = 300;
        private static final int SEVERITY_ALL     = Integer.MIN_VALUE;

        // ascending order for binary search matching the list of enum constants
        private static final int[] LEVEL_VALUES = new int[] {
            SEVERITY_ALL, SEVERITY_FINEST, SEVERITY_FINER,
            SEVERITY_FINE, SEVERITY_CONFIG, SEVERITY_INFO,
            SEVERITY_WARNING, SEVERITY_SEVERE, SEVERITY_OFF
        };

        public int intValue() {
            return LEVEL_VALUES[this.ordinal()];
        }

        /**
         * Maps a severity value to an effective logger level.
         * @param level The severity of the messages that should be
         *        logged with a logger set to the returned level.
         * @return The effective logger level, which is the nearest Level value
         *         whose severity is greater or equal to the given level.
         *         For level > SEVERE (OFF excluded), return SEVERE.
         */
        public static Level valueOf(int level) {
            switch (level) {
                // ordering per the highest occurrences in the jdk source
                // finest, fine, finer, info first
                case SEVERITY_FINEST  : return Level.FINEST;
                case SEVERITY_FINE    : return Level.FINE;
                case SEVERITY_FINER   : return Level.FINER;
                case SEVERITY_INFO    : return Level.INFO;
                case SEVERITY_WARNING : return Level.WARNING;
                case SEVERITY_CONFIG  : return Level.CONFIG;
                case SEVERITY_SEVERE  : return Level.SEVERE;
                case SEVERITY_OFF     : return Level.OFF;
                case SEVERITY_ALL     : return Level.ALL;
            }
            // return the nearest Level value >= the given level,
            // for level > SEVERE, return SEVERE and exclude OFF
            int i = Arrays.binarySearch(LEVEL_VALUES, 0, LEVEL_VALUES.length-2, level);
            return values()[i >= 0 ? i : (-i-1)];
        }
    }

    /**
     *
     * The PlatformLogger.Bridge interface is implemented by the System.Logger
     * objects returned by our default JUL provider - so that JRE classes using
     * PlatformLogger see no difference when JUL is the actual backend.
     *
     * PlatformLogger is now only a thin adaptation layer over the same
     * loggers than returned by java.lang.System.getLogger(String name).
     *
     * The recommendation for JRE classes going forward is to use
     * java.lang.System.getLogger(String name), which will
     * use Lazy Loggers when possible and necessary.
     *
     */
    public static interface Bridge {

        /**
         * Gets the name for this platform logger.
         * @return the name of the platform logger.
         */
        public String getName();

        /**
         * Returns true if a message of the given level would actually
         * be logged by this logger.
         * @param level the level
         * @return whether a message of that level would be logged
         */
        public boolean isLoggable(Level level);
        public boolean isEnabled();

        public void log(Level level, String msg);
        public void log(Level level, String msg, Throwable thrown);
        public void log(Level level, String msg, Object... params);
        public void log(Level level, Supplier<String> msgSupplier);
        public void log(Level level, Throwable thrown, Supplier<String> msgSupplier);
        public void logp(Level level, String sourceClass, String sourceMethod, String msg);
        public void logp(Level level, String sourceClass, String sourceMethod,
                         Supplier<String> msgSupplier);
        public void logp(Level level, String sourceClass, String sourceMethod,
                                                    String msg, Object... params);
        public void logp(Level level, String sourceClass, String sourceMethod,
                         String msg, Throwable thrown);
        public void logp(Level level, String sourceClass, String sourceMethod,
                         Throwable thrown, Supplier<String> msgSupplier);
        public void logrb(Level level, String sourceClass, String sourceMethod,
                          ResourceBundle bundle, String msg, Object... params);
        public void logrb(Level level, String sourceClass, String sourceMethod,
                          ResourceBundle bundle, String msg, Throwable thrown);
        public void logrb(Level level, ResourceBundle bundle, String msg,
                Object... params);
        public void logrb(Level level, ResourceBundle bundle, String msg,
                Throwable thrown);

    }

    /**
     * The {@code PlatformLogger.ConfigurableBridge} interface is used to
     * implement the deprecated {@link PlatformLogger#setLevel} method.
     *
     * PlatformLogger is now only a thin adaptation layer over the same
     * loggers than returned by java.lang.System.getLogger(String name).
     *
     * The recommendation for JRE classes going forward is to use
     * java.lang.System.getLogger(String name), which will
     * use Lazy Loggers when possible and necessary.
     *
     */
    public static interface ConfigurableBridge {

        public abstract class LoggerConfiguration {
            public abstract Level getPlatformLevel();
            public abstract void setPlatformLevel(Level level);
        }

        public default LoggerConfiguration getLoggerConfiguration() {
            return null;
        }

        public static LoggerConfiguration getLoggerConfiguration(PlatformLogger.Bridge logger) {
            if (logger instanceof PlatformLogger.ConfigurableBridge) {
                return ((ConfigurableBridge) logger).getLoggerConfiguration();
            } else {
                return null;
            }
        }
    }

    // Table of known loggers.  Maps names to PlatformLoggers.
    private static final Map<String,WeakReference<PlatformLogger>> loggers =
        new HashMap<>();

    /**
     * Returns a PlatformLogger of a given name.
     * @param name the name of the logger
     * @return a PlatformLogger
     */
    public static synchronized PlatformLogger getLogger(String name) {
        PlatformLogger log = null;
        WeakReference<PlatformLogger> ref = loggers.get(name);
        if (ref != null) {
            log = ref.get();
        }
        if (log == null) {
            // jrt: the bridge java.logging's LoggerFinder gives a system logger (JulBridge, below)
            log = new PlatformLogger(new JulBridge(name));
            loggers.put(name, new WeakReference<>(log));
        }
        return log;
    }

    // The system loggerProxy returned by LazyLoggers
    // This may be a lazy logger - see jdk.internal.logger.LazyLoggers,
    // or may be a Logger instance (or a wrapper thereof).
    //
    private final PlatformLogger.Bridge loggerProxy;
    private PlatformLogger(PlatformLogger.Bridge loggerProxy) {
        this.loggerProxy = loggerProxy;
    }

    /**
     * A convenience method to test if the logger is turned off.
     * (i.e. its level is OFF).
     * @return whether the logger is turned off.
     */
    public boolean isEnabled() {
        return loggerProxy.isEnabled();
    }

    /**
     * Gets the name for this platform logger.
     * @return the name of the platform logger.
     */
    public String getName() {
        return loggerProxy.getName();
    }

    /**
     * Returns true if a message of the given level would actually
     * be logged by this logger.
     * @param level the level
     * @return whether a message of that level would be logged
     */
    public boolean isLoggable(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }

        return loggerProxy.isLoggable(level);
    }

    /**
     * Get the log level that has been specified for this PlatformLogger.
     * The result may be null, which means that this logger's
     * effective level will be inherited from its parent.
     *
     * @return  this PlatformLogger's level
     */
    public Level level() {
        final ConfigurableBridge.LoggerConfiguration spi =
                PlatformLogger.ConfigurableBridge.getLoggerConfiguration(loggerProxy);
        return spi == null ? null : spi.getPlatformLevel();
    }

    /**
     * Set the log level specifying which message levels will be
     * logged by this logger.  Message levels lower than this
     * value will be discarded.  The level value {@link Level#OFF}
     * can be used to turn off logging.
     * <p>
     * If the new level is null, it means that this node should
     * inherit its level from its nearest ancestor with a specific
     * (non-null) level value.
     *
     * @param newLevel the new value for the log level (may be null)
     * @deprecated Platform Loggers should not be configured programmatically.
     *             This method will not work if a custom {@link
     *             java.lang.System.LoggerFinder} is installed.
     */
    @Deprecated
    public void setLevel(Level newLevel) {
        final ConfigurableBridge.LoggerConfiguration spi =
                PlatformLogger.ConfigurableBridge.getLoggerConfiguration(loggerProxy);
        if (spi != null) {
            spi.setPlatformLevel(newLevel);
        }
    }

    /**
     * Logs a SEVERE message.
     * @param msg the message
     */
    public void severe(String msg) {
        loggerProxy.log(Level.SEVERE, msg, (Object[])null);
    }

    public void severe(String msg, Throwable t) {
        loggerProxy.log(Level.SEVERE, msg, t);
    }

    public void severe(String msg, Object... params) {
        loggerProxy.log(Level.SEVERE, msg, params);
    }

    /**
     * Logs a WARNING message.
     * @param msg the message
     */
    public void warning(String msg) {
        loggerProxy.log(Level.WARNING, msg, (Object[])null);
    }

    public void warning(String msg, Throwable t) {
        loggerProxy.log(Level.WARNING, msg, t);
    }

    public void warning(String msg, Object... params) {
        loggerProxy.log(Level.WARNING, msg, params);
    }

    /**
     * Logs an INFO message.
     * @param msg the message
     */
    public void info(String msg) {
        loggerProxy.log(Level.INFO, msg, (Object[])null);
    }

    public void info(String msg, Throwable t) {
        loggerProxy.log(Level.INFO, msg, t);
    }

    public void info(String msg, Object... params) {
        loggerProxy.log(Level.INFO, msg, params);
    }

    /**
     * Logs a CONFIG message.
     * @param msg the message
     */
    public void config(String msg) {
        loggerProxy.log(Level.CONFIG, msg, (Object[])null);
    }

    public void config(String msg, Throwable t) {
        loggerProxy.log(Level.CONFIG, msg, t);
    }

    public void config(String msg, Object... params) {
        loggerProxy.log(Level.CONFIG, msg, params);
    }

    /**
     * Logs a FINE message.
     * @param msg the message
     */
    public void fine(String msg) {
        loggerProxy.log(Level.FINE, msg, (Object[])null);
    }

    public void fine(String msg, Throwable t) {
        loggerProxy.log(Level.FINE, msg, t);
    }

    public void fine(String msg, Object... params) {
        loggerProxy.log(Level.FINE, msg, params);
    }

    /**
     * Logs a FINER message.
     * @param msg the message
     */
    public void finer(String msg) {
        loggerProxy.log(Level.FINER, msg, (Object[])null);
    }

    public void finer(String msg, Throwable t) {
        loggerProxy.log(Level.FINER, msg, t);
    }

    public void finer(String msg, Object... params) {
        loggerProxy.log(Level.FINER, msg, params);
    }

    /**
     * Logs a FINEST message.
     * @param msg the message
     */
    public void finest(String msg) {
        loggerProxy.log(Level.FINEST, msg, (Object[])null);
    }

    public void finest(String msg, Throwable t) {
        loggerProxy.log(Level.FINEST, msg, t);
    }

    public void finest(String msg, Object... params) {
        loggerProxy.log(Level.FINEST, msg, params);
    }

    /**
     * jrt: the bridge of a system logger of java.base when java.logging is present, jdk26u's
     * LoggingProviderImpl.JULWrapper as a PlatformLogger.Bridge and ConfigurableBridge: each call
     * on the java.util.logging logger LogManager demands for java.base (LoggingProviderImpl's
     * LogManagerAccess.demandLoggerFor), demanded when first used, the levels mapped by
     * platform2JulLevelMapping.
     */
    private static final class JulBridge extends ConfigurableBridge.LoggerConfiguration
            implements Bridge, ConfigurableBridge {

        private static final java.util.logging.Level[] platform2JulLevelMapping = {
                java.util.logging.Level.ALL,     // mapped from ALL
                java.util.logging.Level.FINEST,  // mapped from FINEST
                java.util.logging.Level.FINER,   // mapped from FINER
                java.util.logging.Level.FINE,    // mapped from FINE
                java.util.logging.Level.CONFIG,  // mapped from CONFIG
                java.util.logging.Level.INFO,    // mapped from INFO
                java.util.logging.Level.WARNING, // mapped from WARNING
                java.util.logging.Level.SEVERE,  // mapped from SEVERE
                java.util.logging.Level.OFF      // mapped from OFF
        };

        private final String name;
        private volatile java.util.logging.Logger julLogger;

        JulBridge(String name) {
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
                                .demandLoggerFor(manager, name, PlatformLogger.class.getModule());
                        julLogger = l;
                    }
                }
            }
            return l;
        }

        static java.util.logging.Level toJUL(Level level) {
            // The caller will throw if null is invalid in its context.
            // There's at least one case where a null level is valid.
            if (level == null) return null;
            assert level.ordinal() < platform2JulLevelMapping.length;
            return platform2JulLevelMapping[level.ordinal()];
        }

        @Override
        public String getName() {
            return julLogger().getName();
        }

        @Override
        public boolean isLoggable(Level level) {
            return julLogger().isLoggable(toJUL(level));
        }

        @Override
        public boolean isEnabled() {
            return julLogger().getLevel() != java.util.logging.Level.OFF;
        }

        @Override
        public void log(Level level, String msg) {
            julLogger().log(toJUL(level), msg);
        }

        @Override
        public void log(Level level, String msg, Throwable thrown) {
            julLogger().log(toJUL(level), msg, thrown);
        }

        @Override
        public void log(Level level, String msg, Object... params) {
            julLogger().log(toJUL(level), msg, params);
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier) {
            julLogger().log(toJUL(level), msgSupplier);
        }

        @Override
        public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
            julLogger().log(toJUL(level), thrown, msgSupplier);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
            julLogger().logp(toJUL(level), sourceClass, sourceMethod, msg);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod,
                         Supplier<String> msgSupplier) {
            julLogger().logp(toJUL(level), sourceClass, sourceMethod, msgSupplier);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod,
                         String msg, Object... params) {
            julLogger().logp(toJUL(level), sourceClass, sourceMethod, msg, params);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod,
                         String msg, Throwable thrown) {
            julLogger().logp(toJUL(level), sourceClass, sourceMethod, msg, thrown);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod,
                         Throwable thrown, Supplier<String> msgSupplier) {
            julLogger().logp(toJUL(level), sourceClass, sourceMethod,
                    thrown, msgSupplier);
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod,
                          ResourceBundle bundle, String key, Object... params) {
            julLogger().logrb(toJUL(level), sourceClass, sourceMethod,
                    bundle, key, params);
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod,
                          ResourceBundle bundle, String key, Throwable thrown) {
            julLogger().logrb(toJUL(level), sourceClass, sourceMethod,
                    bundle, key, thrown);
        }

        @Override
        public void logrb(Level level, ResourceBundle bundle, String key,
                          Object... params) {
            julLogger().logrb(toJUL(level), bundle, key, params);
        }

        @Override
        public void logrb(Level level, ResourceBundle bundle, String key,
                          Throwable thrown) {
            julLogger().logrb(toJUL(level), bundle, key, thrown);
        }

        @Override
        public Level getPlatformLevel() {
            final java.util.logging.Level javaLevel = julLogger().getLevel();
            if (javaLevel == null) return null;
            try {
                return Level.valueOf(javaLevel.getName());
            } catch (IllegalArgumentException e) {
                return Level.valueOf(javaLevel.intValue());
            }
        }

        @Override
        public void setPlatformLevel(Level level) {
            // null is allowed here
            julLogger().setLevel(toJUL(level));
        }

        @Override
        public LoggerConfiguration getLoggerConfiguration() {
            return this;
        }
    }
}
