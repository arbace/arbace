/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13 slice 5, "As built: F13 (write-ahead)"; JCS161 (a)):
 * the platform beans of jdk.management that java.lang.management's ManagementFactory (jrt's, its table) makes, as
 * jdk26u's com.sun.management.internal.PlatformMBeanProviderImpl makes them (its classes are package-private):
 * the operating system's. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package com.sun.management.internal;

import java.lang.management.PlatformManagedObject;
import sun.management.VMManagement;

/** jdk.management's platform beans for jrt's ManagementFactory. */
public final class JrtPlatformBeans {

    private JrtPlatformBeans() {
    }

    /** The operating system's bean (a com.sun.management.UnixOperatingSystemMXBean). */
    public static PlatformManagedObject operatingSystem(VMManagement vm) {
        return new OperatingSystemImpl(vm);
    }
}
