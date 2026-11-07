/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F2, "As built: F2 (write-ahead)"): jrt's
 * jdk.internal.event.EventHelper, written for jrt in place of jdk26u's, which logs the security
 * events (TLS handshakes, security properties, X.509 certificates and validations) to the
 * System.Logger of jdk.event.security when that logger is enabled at the DEBUG level: System.Logger
 * is not in the Go build (JCS138), so the logging is never on and the log methods are never called
 * (their callers test isLoggingSecurity first, as jdk26u's do). The methods' signatures are
 * jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/jdk/internal/event/EventHelper.java).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.event;

import java.time.Instant;

/** The security events' logging, which the Go build does not have. */
public final class EventHelper {

    private EventHelper() {
    }

    /** A TLS handshake's event: nothing is logged. */
    public static void logTLSHandshakeEvent(Instant start, String peerHost, int peerPort,
                                            String cipherSuite, String protocolVersion,
                                            long peerCertId) {
    }

    /** A security property's event: nothing is logged. */
    public static void logSecurityPropertyEvent(String key, String value) {
    }

    /** A certificate path validation's event: nothing is logged. */
    public static void logX509ValidationEvent(long anchorCertId, long[] certIds) {
    }

    /** A certificate's event: nothing is logged. */
    public static void logX509CertificateEvent(String algId, String serialNum, String subject,
                                               String issuer, String keyType, int length,
                                               long certId, long beginDate, long endDate) {
    }

    /** Is the security logger on: never in the Go build. */
    public static boolean isLoggingSecurity() {
        return false;
    }
}
