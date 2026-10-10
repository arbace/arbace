/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency"): CompletableFuture's delays in the
 * Go build (orTimeout, completeOnTimeout, delayedExecutor). jdk26u's CompletableFuture
 * schedules them in its ForkJoinPool's DelayScheduler (ForkJoinPool.scheduleDelayedTask with a
 * DelayScheduler.ScheduledForkJoinTask), which jrt's hand-written ForkJoinPool has not; here a
 * delayed task is a FutureTask run by a daemon thread (a goroutine) once the delay has passed,
 * unless it was cancelled first, which wakes the thread. The variant of CompletableFuture
 * (overlay/jdk/variants/CompletableFuture.clj) calls schedule.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.LockSupport;

/** Delayed tasks, each on a daemon thread of its own. */
public final class Delays {

    private Delays() {
    }

    /** A delayed task: cancelling (or completing) it wakes its thread. */
    private static final class DelayedTask extends FutureTask<Void> {
        volatile Thread waiter;

        DelayedTask(Runnable task) {
            super(task, null);
        }

        @Override
        protected void done() {
            Thread t = waiter;
            if (t != null)
                LockSupport.unpark(t);
        }
    }

    /**
     * Runs task after nanoDelay nanoseconds (at once if not positive) on a new daemon thread,
     * unless the returned future is cancelled first.
     */
    public static Future<?> schedule(Runnable task, long nanoDelay) {
        DelayedTask f = new DelayedTask(task);
        Thread t = new Thread(() -> {
            long deadline = System.nanoTime() + nanoDelay;
            long rem;
            while (!f.isDone() && (rem = deadline - System.nanoTime()) > 0L)
                LockSupport.parkNanos(f, rem);
            f.run();
        }, "CompletableFutureDelayScheduler");
        t.setDaemon(true);
        f.waiter = t;
        t.start();
        return f;
    }
}
