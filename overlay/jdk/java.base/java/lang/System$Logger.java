/*
 * Arbace (overlay/jdk, queue 562, the user's JMS4 of 2026-11-24; doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built:
 * java-misc-1124"): java.lang.System.Logger, whose java.lang.System is jrt's hand-written Go, as a top-level
 * interface named by the nested interface's binary name (System$Logger, legal Java), translated by j2g with its
 * default methods. It is read apart from java.base's other sources (bin/jrt-convert: a top-level System$Logger in
 * the patch-module read hides the platform's member System.Logger from them). The interface is jdk26u's
 * (src/java.base/share/classes/java/lang/System.java at https://github.com/openjdk/jdk26u baf63fb, lines 916-1239):
 * Copyright (c) 1994, 2025, Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception (LICENSE.md).
 */
package java.lang;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public interface System$Logger {

    /**
     * System {@linkplain Logger loggers} levels.
     *
     * A level has a {@linkplain #getName() name} and {@linkplain
     * #getSeverity() severity}.
     * Level values are {@link #ALL}, {@link #TRACE}, {@link #DEBUG},
     * {@link #INFO}, {@link #WARNING}, {@link #ERROR}, {@link #OFF},
     * by order of increasing severity.
     * <br>
     * {@link #ALL} and {@link #OFF}
     * are simple markers with severities mapped respectively to
     * {@link java.lang.Integer#MIN_VALUE Integer.MIN_VALUE} and
     * {@link java.lang.Integer#MAX_VALUE Integer.MAX_VALUE}.
     * <p>
     * <b>Severity values and Mapping to {@code java.util.logging.Level}.</b>
     * <p>
     * {@linkplain System.Logger.Level System logger levels} are mapped to
     * {@linkplain java.logging/java.util.logging.Level  java.util.logging levels}
     * of corresponding severity.
     * <br>The mapping is as follows:
     * <br><br>
     * <table class="striped">
     * <caption>System.Logger Severity Level Mapping</caption>
     * <thead>
     * <tr><th scope="col">System.Logger Levels</th>
     *     <th scope="col">java.util.logging Levels</th>
     * </thead>
     * <tbody>
     * <tr><th scope="row">{@link Logger.Level#ALL ALL}</th>
     *     <td>{@link java.logging/java.util.logging.Level#ALL ALL}</td>
     * <tr><th scope="row">{@link Logger.Level#TRACE TRACE}</th>
     *     <td>{@link java.logging/java.util.logging.Level#FINER FINER}</td>
     * <tr><th scope="row">{@link Logger.Level#DEBUG DEBUG}</th>
     *     <td>{@link java.logging/java.util.logging.Level#FINE FINE}</td>
     * <tr><th scope="row">{@link Logger.Level#INFO INFO}</th>
     *     <td>{@link java.logging/java.util.logging.Level#INFO INFO}</td>
     * <tr><th scope="row">{@link Logger.Level#WARNING WARNING}</th>
     *     <td>{@link java.logging/java.util.logging.Level#WARNING WARNING}</td>
     * <tr><th scope="row">{@link Logger.Level#ERROR ERROR}</th>
     *     <td>{@link java.logging/java.util.logging.Level#SEVERE SEVERE}</td>
     * <tr><th scope="row">{@link Logger.Level#OFF OFF}</th>
     *     <td>{@link java.logging/java.util.logging.Level#OFF OFF}</td>
     * </tbody>
     * </table>
     *
     * @since 9
     *
     * @see java.lang.System.LoggerFinder
     * @see java.lang.System.Logger
     */
    @SuppressWarnings("doclint:reference") // cross-module links
    public enum Level {

        // for convenience, we're reusing java.util.logging.Level int values
        // the mapping logic in sun.util.logging.PlatformLogger depends
        // on this.
        /**
         * A marker to indicate that all levels are enabled.
         * This level {@linkplain #getSeverity() severity} is
         * {@link Integer#MIN_VALUE}.
         */
        ALL(Integer.MIN_VALUE),  // typically mapped to/from j.u.l.Level.ALL
        /**
         * {@code TRACE} level: usually used to log diagnostic information.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 400}.
         */
        TRACE(400),   // typically mapped to/from j.u.l.Level.FINER
        /**
         * {@code DEBUG} level: usually used to log debug information traces.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 500}.
         */
        DEBUG(500),   // typically mapped to/from j.u.l.Level.FINEST/FINE/CONFIG
        /**
         * {@code INFO} level: usually used to log information messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 800}.
         */
        INFO(800),    // typically mapped to/from j.u.l.Level.INFO
        /**
         * {@code WARNING} level: usually used to log warning messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 900}.
         */
        WARNING(900), // typically mapped to/from j.u.l.Level.WARNING
        /**
         * {@code ERROR} level: usually used to log error messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 1000}.
         */
        ERROR(1000),  // typically mapped to/from j.u.l.Level.SEVERE
        /**
         * A marker to indicate that all levels are disabled.
         * This level {@linkplain #getSeverity() severity} is
         * {@link Integer#MAX_VALUE}.
         */
        OFF(Integer.MAX_VALUE);  // typically mapped to/from j.u.l.Level.OFF

        private final int severity;

        private Level(int severity) {
            this.severity = severity;
        }

        /**
         * Returns the name of this level.
         * @return this level {@linkplain #name()}.
         */
        public final String getName() {
            return name();
        }

        /**
         * Returns the severity of this level.
         * A higher severity means a more severe condition.
         * @return this level severity.
         */
        public final int getSeverity() {
            return severity;
        }
    }

    /**
     * Returns the name of this logger.
     *
     * @return the logger name.
     */
    public String getName();

    /**
     * Checks if a message of the given level would be logged by
     * this logger.
     *
     * @param level the log message level.
     * @return {@code true} if the given log message level is currently
     *         being logged.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public boolean isLoggable(Level level);

    /**
     * Logs a message.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msg, (Object[])null);}
     *
     * @param level the log message level.
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public default void log(Level level, String msg) {
        log(level, (ResourceBundle) null, msg, (Object[]) null);
    }

    /**
     * Logs a lazily supplied message.
     *
     * If the logger is currently enabled for the given log message level
     * then a message is logged that is the result produced by the
     * given supplier function.  Otherwise, the supplier is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msgSupplier.get(), (Object[])null);}
     *
     * @param level the log message level.
     * @param msgSupplier a supplier function that produces a message.
     *
     * @throws NullPointerException if {@code level} is {@code null},
     *         or {@code msgSupplier} is {@code null}.
     */
    public default void log(Level level, Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            log(level, (ResourceBundle) null, msgSupplier.get(), (Object[]) null);
        }
    }

    /**
     * Logs a message produced from the given object.
     *
     * If the logger is currently enabled for the given log message level then
     * a message is logged that, by default, is the result produced from
     * calling  toString on the given object.
     * Otherwise, the object is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, obj.toString(), (Object[])null);}
     *
     * @param level the log message level.
     * @param obj the object to log.
     *
     * @throws NullPointerException if {@code level} is {@code null}, or
     *         {@code obj} is {@code null}.
     */
    public default void log(Level level, Object obj) {
        Objects.requireNonNull(obj);
        if (isLoggable(Objects.requireNonNull(level))) {
            this.log(level, (ResourceBundle) null, obj.toString(), (Object[]) null);
        }
    }

    /**
     * Logs a message associated with a given throwable.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msg, thrown);}
     *
     * @param level the log message level.
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param thrown a {@code Throwable} associated with the log message;
     *        can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public default void log(Level level, String msg, Throwable thrown) {
        this.log(level, null, msg, thrown);
    }

    /**
     * Logs a lazily supplied message associated with a given throwable.
     *
     * If the logger is currently enabled for the given log message level
     * then a message is logged that is the result produced by the
     * given supplier function.  Otherwise, the supplier is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msgSupplier.get(), thrown);}
     *
     * @param level one of the log message level identifiers.
     * @param msgSupplier a supplier function that produces a message.
     * @param thrown a {@code Throwable} associated with log message;
     *               can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}, or
     *                               {@code msgSupplier} is {@code null}.
     */
    public default void log(Level level, Supplier<String> msgSupplier,
            Throwable thrown) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            this.log(level, null, msgSupplier.get(), thrown);
        }
    }

    /**
     * Logs a message with an optional list of parameters.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, format, params);}
     *
     * @param level one of the log message level identifiers.
     * @param format the string message format in {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog, if this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public default void log(Level level, String format, Object... params) {
        this.log(level, null, format, params);
    }

    /**
     * Logs a localized message associated with a given throwable.
     *
     * If the given resource bundle is non-{@code null},  the {@code msg}
     * string is localized using the given resource bundle.
     * Otherwise the {@code msg} string is not localized.
     *
     * @param level the log message level.
     * @param bundle a resource bundle to localize {@code msg}; can be
     * {@code null}.
     * @param msg the string message (or a key in the message catalog,
     *            if {@code bundle} is not {@code null}); can be {@code null}.
     * @param thrown a {@code Throwable} associated with the log message;
     *        can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public void log(Level level, ResourceBundle bundle, String msg,
            Throwable thrown);

    /**
     * Logs a message with resource bundle and an optional list of
     * parameters.
     *
     * If the given resource bundle is non-{@code null},  the {@code format}
     * string is localized using the given resource bundle.
     * Otherwise the {@code format} string is not localized.
     *
     * @param level the log message level.
     * @param bundle a resource bundle to localize {@code format}; can be
     * {@code null}.
     * @param format the string message format in {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog if {@code bundle} is not {@code null}); can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    public void log(Level level, ResourceBundle bundle, String format,
            Object... params);
}
