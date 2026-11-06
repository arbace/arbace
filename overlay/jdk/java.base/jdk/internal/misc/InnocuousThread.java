/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F12, "As built: F12 (write-ahead)"): jrt's
 * jdk.internal.misc.InnocuousThread, written for jrt after jdk26u's
 * (https://github.com/openjdk/jdk26u at baf63fb, src/java.base/share/classes/jdk/internal/misc/
 * InnocuousThread.java), for the JDK classes that start their system threads through it (since F12
 * sun.net.www.http's KeepAliveCache and KeepAliveStream). jdk26u's thread belongs to a thread group of its
 * own, has no context class loader, erases its thread locals and takes a stack size, all through Unsafe on
 * Thread's fields; jrt's hand-written Thread has no thread groups, no context class loader, its thread
 * locals in Go and goroutine stacks that grow, so this one keeps the factories' signatures and names, the
 * priority, the silently ignored uncaught-exception handler and the run-once rule, and erases nothing.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.misc;

import java.util.concurrent.atomic.AtomicInteger;

/** A system thread: no uncaught-exception handler of its own, run at most once. */
public final class InnocuousThread extends Thread {
    private static final AtomicInteger threadNumber = new AtomicInteger(1);

    private static String newName() {
        return "InnocuousThread-" + threadNumber.getAndIncrement();
    }

    public static Thread newThread(Runnable target) {
        return newThread(newName(), target);
    }

    public static Thread newThread(String name, Runnable target) {
        return newThread(name, target, -1);
    }

    public static Thread newThread(String name, Runnable target, int priority) {
        return createThread(name, target, priority);
    }

    public static Thread newSystemThread(Runnable target) {
        return newSystemThread(newName(), target);
    }

    public static Thread newSystemThread(String name, Runnable target) {
        return newSystemThread(name, target, -1);
    }

    public static Thread newSystemThread(String name, Runnable target, int priority) {
        return createThread(name, target, priority);
    }

    /** The stack size is the JDK's hint; a goroutine's stack grows. */
    public static Thread newSystemThread(String name, Runnable target, long stackSize, int priority) {
        return createThread(name, target, priority);
    }

    private static Thread createThread(String name, Runnable target, int priority) {
        Thread t = new InnocuousThread(target, name);
        if (priority >= 0) {
            t.setPriority(priority);
        }
        return t;
    }

    private InnocuousThread(Runnable target, String name) {
        super(target, name);
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) {
        // silently fail
    }

    /** jdk26u drops the thread locals; jrt's are Go's, kept per goroutine: nothing to drop. */
    public final void eraseThreadLocals() {
    }

    // ensure run method is run only once
    private volatile boolean hasRun;

    @Override
    public void run() {
        if (Thread.currentThread() == this && !hasRun) {
            hasRun = true;
            super.run();
        }
    }
}
