/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: F13 (write-ahead)"; JCS161 (a)):
 * what java.lang.management asks of the runtime, the natives HotSpot's management interface (jmm) answers in
 * the JDK through sun.management.VMManagementImpl and ThreadImpl: here jrt's (go/arbace/jrt/management.ae),
 * which the variants of those two classes call (overlay/jdk/variants/VMManagementImpl.ae, ThreadImpl.ae). The
 * threads are jrt's live threads (started and not ended, and the main thread); the counts are the JVM's
 * ThreadService's: live, daemon, peak (reset to the live count), ever started. The settings the beans enable
 * (contention monitoring, CPU time, allocated memory) are kept here. Since slice 2 what a thread waits for: the
 * object, its owner, the blocked and waited counts and times (go/arbace/jrt/management.ae's waitBegin).
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

    private static volatile boolean cpuTime;
    private static volatile boolean allocatedMemory;

    private static volatile boolean contentionMonitoring;

    /** Thread contention monitoring as ThreadMXBean set it (jrt measures the times while it is on). */
    public static boolean contentionMonitoring() {
        return contentionMonitoring;
    }

    public static void setContentionMonitoring(boolean on) {
        contentionMonitoring = on;
        setContentionMonitoring0(on);
    }

    private static native void setContentionMonitoring0(boolean on);

    /** The object t is blocked to enter, or waits on (a monitor's or a lock's, park's blocker), or null. */
    public static native Object waitOn0(Thread t);

    /** The thread holding x's monitor or lock, or null. */
    public static native Thread lockOwner0(Object x);

    /** The threads deadlocked on object monitors (cycles of BLOCKED threads), or null (slice 3). */
    public static native Thread[] findMonitorDeadlocked0();

    /** The threads deadlocked on monitors or ownable synchronizers, or null (slice 3). */
    public static native Thread[] findDeadlocked0();

    /**
     * Starts recording the threads' held monitors and synchronizers (slice 3b), when ThreadMXBean is made: what
     * a thread locked before is not known.
     */
    public static native void trackLocks0();

    /** The monitors t holds, the latest entered first, each once, not the one it waits on (slice 3b). */
    public static native Object[] lockedMonitors0(Thread t);

    /** The ownable synchronizers t holds (jrt's ReentrantLocks; slice 3b). */
    public static native Object[] lockedSynchronizers0(Thread t);

    /** The times t blocked to enter a monitor or a lock. */
    public static native long blockedCount0(Thread t);

    /** The times t waited. */
    public static native long waitedCount0(Thread t);

    /** t's milliseconds blocked while contention monitoring was on; -1 when it is off. */
    public static native long blockedTime0(Thread t);

    /** t's milliseconds waited while contention monitoring was on; -1 when it is off. */
    public static native long waitedTime0(Thread t);

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

    /** ThreadInfo's private constructors, as the JVM reaches them (ThreadInfo's static initializer sets it). */
    public interface ThreadInfoFactory {
        java.lang.management.ThreadInfo newThreadInfo(Thread t, int state, Object lockObj, Thread lockOwner,
                                                      long blockedCount, long blockedTime,
                                                      long waitedCount, long waitedTime,
                                                      StackTraceElement[] stackTrace,
                                                      Object[] monitors, int[] stackDepths,
                                                      Object[] synchronizers);
    }

    private static ThreadInfoFactory threadInfoFactory;

    public static void setThreadInfoFactory(ThreadInfoFactory f) {
        threadInfoFactory = f;
    }

    /** The factory, ThreadInfo initialized first. */
    public static ThreadInfoFactory threadInfoFactory() {
        ThreadInfoFactory f = threadInfoFactory;
        if (f == null) {
            try {
                Class.forName("java.lang.management.ThreadInfo");
            } catch (ClassNotFoundException e) {
                throw new InternalError(e);
            }
            f = threadInfoFactory;
        }
        return f;
    }

    /** A thread's state as jmm reports it (jvmti.h's bits): alive, and runnable, blocked, waiting or timed. */
    public static int jmmState(Thread t) {
        switch (t.getState()) {
            case NEW: return 0;
            case TERMINATED: return 0x0002;
            case BLOCKED: return 0x0001 | 0x0400;
            case WAITING: return 0x0001 | 0x0010;
            case TIMED_WAITING: return 0x0001 | 0x0020;
            default: return 0x0001 | 0x0004;
        }
    }
}
