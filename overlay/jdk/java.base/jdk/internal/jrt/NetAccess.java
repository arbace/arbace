/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F3, "As built: F3 (write-ahead)"): the
 * access object of java.net.InetAddress that SharedSecrets holds in the JDK
 * (JavaNetInetAddressAccess: an address's original host name, IPv4's value, IPv6's bytes), kept
 * here as jrt's PkiAccess and ZipAccess keep theirs (jrt has no SharedSecrets). InetAddress's
 * variant (overlay/jdk/variants/InetAddressAccess.ae) sets it in its static initializer;
 * SSLSocketImpl's variant reads it (useImplicitHost: the host name an unconnected socket was
 * connected to, for the server name indication and the endpoint identification).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.net.InetAddress;
import jdk.internal.access.JavaNetInetAddressAccess;

/** InetAddress's access object. */
public final class NetAccess {
    private static JavaNetInetAddressAccess javaNetInetAddressAccess;

    private NetAccess() {
    }

    /** Set by InetAddress's static initializer. */
    public static void setJavaNetInetAddressAccess(JavaNetInetAddressAccess access) {
        javaNetInetAddressAccess = access;
    }

    /** InetAddress's access object, InetAddress initialized first. */
    public static JavaNetInetAddressAccess getJavaNetInetAddressAccess() {
        JavaNetInetAddressAccess access = javaNetInetAddressAccess;
        if (access == null) {
            // InetAddress's static initializer sets it
            InetAddress.getLoopbackAddress();
            access = javaNetInetAddressAccess;
        }
        return access;
    }
}
