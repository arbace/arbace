/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS254 (b)"): jdk26u's
 * jdk.internal.net.http.hpack.HPACK (src/java.net.http/share/classes/jdk/internal/net/http/hpack/HPACK.java
 * at https://github.com/openjdk/jdk26u baf63fb), derived by test/jrt/derive.ae (bin/jrt-convert derive): System.Logger as jdk.internal.jrt.SystemLoggers.Logger (JCS155);
 * nothing else changed. jdk26u's text Copyright (c) Oracle and/or its affiliates, under the GNU General Public
 * License version 2 with the Classpath Exception (LICENSE.md); the derivation Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.hpack.HPACK.Logger.Level;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.EXTRA;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.NONE;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.NORMAL;

/**
 * Internal utilities and stuff.
 */
public final class HPACK {

    private static final RootLogger LOGGER;
    private static final Map<String, Level> logLevels =
            Map.of("NORMAL", NORMAL, "EXTRA", EXTRA);

    static {
        String PROPERTY = "jdk.internal.httpclient.hpack.log.level";

        String value = System.getProperty(PROPERTY);

        if (value == null) {
            LOGGER = new RootLogger(NONE);
        } else {
            String upperCasedValue = value.toUpperCase();
            Level l = logLevels.get(upperCasedValue);
            if (l == null) {
                LOGGER = new RootLogger(NONE);
                LOGGER.log(jdk.internal.jrt.SystemLoggers.Logger.Level.INFO,
                        () -> format("%s value '%s' not recognized (use %s); logging disabled",
                                     PROPERTY, value, logLevels.keySet().stream().collect(joining(", "))));
            } else {
                LOGGER = new RootLogger(l);
                LOGGER.log(jdk.internal.jrt.SystemLoggers.Logger.Level.DEBUG,
                        () -> format("logging level %s", l));
            }
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    private HPACK() { }

    /**
     * The purpose of this logger is to provide means of diagnosing issues _in
     * the HPACK implementation_. It's not a general purpose logger.
     */
    // implements jdk.internal.jrt.SystemLoggers.Logger to make it possible to skip this class
    // when looking for the Caller.
    public static class Logger implements jdk.internal.jrt.SystemLoggers.Logger {

        /**
         * Log detail level.
         */
        public enum Level {

            NONE(0, jdk.internal.jrt.SystemLoggers.Logger.Level.OFF),
            NORMAL(1, jdk.internal.jrt.SystemLoggers.Logger.Level.DEBUG),
            EXTRA(2, jdk.internal.jrt.SystemLoggers.Logger.Level.TRACE);

            private final int level;
            final jdk.internal.jrt.SystemLoggers.Logger.Level systemLevel;

            Level(int i, jdk.internal.jrt.SystemLoggers.Logger.Level system) {
                level = i;
                systemLevel = system;
            }

            public final boolean implies(Level other) {
                return this.level >= other.level;
            }
        }

        private final String name;
        private final Level level;
        private final String path;
        private final jdk.internal.jrt.SystemLoggers.Logger logger;

        private Logger(String path, String name, Level level) {
            this(path, name, level, null);
        }

        private Logger(String p, String name, Level level, jdk.internal.jrt.SystemLoggers.Logger logger) {
            this.path = p;
            this.name = name;
            this.level = level;
            this.logger = Utils.getHpackLogger(path::toString, level.systemLevel);
        }

        public final String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(jdk.internal.jrt.SystemLoggers.Logger.Level level) {
            return logger.isLoggable(level);
        }

        @Override
        public void log(jdk.internal.jrt.SystemLoggers.Logger.Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            logger.log(level, bundle, msg,thrown);
        }

        @Override
        public void log(jdk.internal.jrt.SystemLoggers.Logger.Level level, ResourceBundle bundle, String format, Object... params) {
            logger.log(level, bundle, format, params);
        }

        /*
         * Usual performance trick for logging, reducing performance overhead in
         * the case where logging with the specified level is a NOP.
         */

        public boolean isLoggable(Level level) {
            return this.level.implies(level);
        }

        public void log(Level level, Supplier<String> s) {
            if (this.level.implies(level)) {
                logger.log(level.systemLevel, s);
            }
        }

        public Logger subLogger(String name) {
            return new Logger(path + "/" + name, name, level);
        }

    }

    private static final class RootLogger extends Logger {

        protected RootLogger(Level level) {
            super("hpack", "hpack", level);
        }

    }

    // -- low-level utilities --

    @FunctionalInterface
    interface BufferUpdateConsumer {
        void accept(long data, int len);
    }

    @SuppressWarnings("fallthrough")
    public static int read(ByteBuffer source,
                           long buffer,
                           int bufferLen,
                           BufferUpdateConsumer consumer)
    {
        // read as much as possible (up to 8 bytes)
        int nBytes = Math.min((64 - bufferLen) >> 3, source.remaining());
        switch (nBytes) {
            case 0:
                break;
            case 3:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 2:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 1:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
                consumer.accept(buffer, bufferLen);
                break;
            case 7:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 6:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 5:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 4:
                buffer |= ((source.getInt() & 0x00000000ffffffffL) << (32 - bufferLen));
                bufferLen += 32;
                consumer.accept(buffer, bufferLen);
                break;
            case 8:
                buffer = source.getLong();
                bufferLen = 64;
                consumer.accept(buffer, bufferLen);
                break;
            default:
                throw new InternalError(String.valueOf(nBytes));
        }
        return nBytes;
    }

    // The number of bytes that can be written at once
    // (calculating in bytes, not bits, since
    //  destination.remaining() * 8 might overflow)
    @SuppressWarnings("fallthrough")
    public static int write(long buffer,
                            int bufferLen,
                            BufferUpdateConsumer consumer,
                            ByteBuffer destination)
    {
        int nBytes = Math.min(bufferLen >> 3, destination.remaining());
        switch (nBytes) {
            case 0:
                break;
            case 3:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 2:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 1:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
                consumer.accept(buffer, bufferLen);
                break;
            case 7:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 6:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 5:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 4:
                destination.putInt((int) (buffer >>> 32));
                buffer <<= 32;
                bufferLen -= 32;
                consumer.accept(buffer, bufferLen);
                break;
            case 8:
                destination.putLong(buffer);
                buffer = 0;
                bufferLen = 0;
                consumer.accept(buffer, bufferLen);
                break;
            default:
                throw new InternalError(String.valueOf(nBytes));
        }
        return nBytes;
    }

    /*
     * Returns the number of bytes the given number of bits constitute.
     */
    static int bytesForBits(int n) {
        assert (n / 8 + (n % 8 != 0 ? 1 : 0)) == (n + 7) / 8
                && (n + 7) / 8 == ((n + 7) >> 3) : n;
        return (n + 7) >> 3;
    }
}
