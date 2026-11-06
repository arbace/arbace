/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: F13 (write-ahead)"; JCS161 (a)):
 * what java.lang.management asks of the runtime, the natives HotSpot's management interface (jmm) answers in
 * the JDK through sun.management.VMManagementImpl and ThreadImpl: here jrt's (go/arbace/jrt/management.ae),
 * which the variants of those two classes call (overlay/jdk/variants/VMManagementImpl.ae, ThreadImpl.ae). The
 * threads are jrt's live threads (started and not ended, and the main thread); the counts are the JVM's
 * ThreadService's: live, daemon, peak (reset to the live count), ever started. The settings the beans enable
 * (contention monitoring, CPU time, allocated memory) are kept here.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

/** The runtime's management facts. */
public final class HostManagement {

    private HostManagement() {
    }

    /** The live threads, in start order. */
    public static native Thread[] threads0();

    /** The live threads' count. */
    public static native int liveCount0();

    /** The live daemon threads' count. */
    public static native int daemonCount0();

    /** The most live threads at once since the start or the last reset. */
    public static native int peakCount0();

    /** Makes the peak the current live count. */
    public static native void resetPeakCount0();

    /** The threads ever started. */
    public static native long totalStarted0();

    /** The runtime's start, in milliseconds since the epoch. */
    public static native long startTime0();

    /** The milliseconds since the runtime's start. */
    public static native long uptime0();

    /** The process id. */
    public static native int pid0();

    private static volatile boolean contentionMonitoring;
    private static volatile boolean cpuTime;
    private static volatile boolean allocatedMemory;

    /** Thread contention monitoring as ThreadMXBean set it. */
    public static boolean contentionMonitoring() {
        return contentionMonitoring;
    }

    public static void setContentionMonitoring(boolean on) {
        contentionMonitoring = on;
    }

    /** Thread CPU time measurement as ThreadMXBean set it. */
    public static boolean cpuTime() {
        return cpuTime;
    }

    public static void setCpuTime(boolean on) {
        cpuTime = on;
    }

    /** Thread allocated memory measurement as com.sun.management.ThreadMXBean set it. */
    public static boolean allocatedMemory() {
        return allocatedMemory;
    }

    public static void setAllocatedMemory(boolean on) {
        allocatedMemory = on;
    }
}
