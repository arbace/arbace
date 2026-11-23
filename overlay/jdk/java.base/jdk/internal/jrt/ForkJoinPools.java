/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency", JC7): the methods of ExecutorService
 * that jrt's hand-written ForkJoinPool cannot define itself, their types (List, Collection)
 * being translated classes jrt's own build cannot name. j2g gives ForkJoinPool Go methods that
 * call these (j2g_support) once ExecutorService is translated. They follow the JDK's documented
 * behaviour over the pool's public API: invokeAll waits for every task and returns their
 * futures; invokeAny returns the result of one task that completed normally, cancelling the
 * others (as AbstractExecutorService's), else throws ExecutionException; shutdownNow shuts the
 * pool down and returns an empty list (the JDK's pool cancels its queued tasks; jrt's runs
 * every task in a worker of its own at once, so none is queued).
 * Since queue 543 also ForkJoinPool's delayed tasks (JDK 25's schedule, scheduleAtFixedRate,
 * scheduleWithFixedDelay, cancelDelayedTasksOnShutdown), which return a ScheduledFuture jrt's
 * own build cannot name: a scheduled task is a ForkJoinTask and ScheduledFuture (the user's
 * handle) and a waiter, a task executed in the pool at once (a worker of its own, as every task
 * given to jrt's pool from outside), which waits for the trigger time or the handle's
 * cancellation, then runs the action and completes the handle. As the JDK's: the pool does not
 * terminate while a delayed task waits, a one-shot task still runs after shutdown unless
 * cancelDelayedTasksOnShutdown was called, a periodic one stops at shutdown, a periodic task's
 * exception completes its handle exceptionally, a delay is from the call (fixed rate: from the
 * first trigger; fixed delay: from the end of the previous run).
 * doInvokeAny follows the code of AbstractExecutorService's, from
 * https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/util/concurrent/AbstractExecutorService.java (Oracle's GNU
 * General Public License version 2 with the Classpath Exception header, and Doug Lea's: "Written
 * by Doug Lea with assistance from members of JCP JSR-166 Expert Group and released to the
 * public domain"; LICENSE.md). The rest is Arbace's own: Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** ExecutorService's collection methods for jrt's ForkJoinPool. */
public final class ForkJoinPools {

    private ForkJoinPools() {
    }

    /** shutdownNow: shut down; no task is queued; the delayed tasks cancelled. */
    public static List<Runnable> shutdownNow(ForkJoinPool pool) {
        // its delayed tasks cancelled, as the JDK's shutdownNow cancels them (queue 543)
        if (pool != ForkJoinPool.commonPool())
            CANCEL_ON_SHUTDOWN.put(pool, Boolean.TRUE);
        pool.shutdown();
        return Collections.emptyList();
    }

    /** invokeAll(tasks): every task run, its future done. */
    public static <T> List<Future<T>> invokeAll(ForkJoinPool pool, Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = ForkJoinTask.adapt(t);
                futures.add(f);
                pool.execute(f);
            }
            for (Future<T> f : futures)
                ((ForkJoinTask<?>) f).quietlyJoin();
            return futures;
        } catch (Throwable t) {
            for (Future<T> f : futures)
                f.cancel(false);
            throw t;
        }
    }

    /** invokeAll(tasks, timeout, unit): the tasks not done in time are cancelled. */
    public static <T> List<Future<T>> invokeAll(ForkJoinPool pool, Collection<? extends Callable<T>> tasks,
                                                long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = ForkJoinTask.adapt(t);
                futures.add(f);
                pool.execute(f);
            }
            for (int i = 0; i < futures.size(); i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    try {
                        f.get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                    } catch (CancellationException | ExecutionException ignore) {
                    } catch (TimeoutException timedOut) {
                        for (int j = i; j < futures.size(); j++)
                            futures.get(j).cancel(true);
                        return futures;
                    }
                }
            }
            return futures;
        } catch (Throwable t) {
            for (Future<T> f : futures)
                f.cancel(true);
            throw t;
        }
    }

    /** invokeAny(tasks): a normal result, the others cancelled. */
    public static <T> T invokeAny(ForkJoinPool pool, Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(pool, tasks, false, 0L);
        } catch (TimeoutException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** invokeAny(tasks, timeout, unit). */
    public static <T> T invokeAny(ForkJoinPool pool, Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(pool, tasks, true, unit.toNanos(timeout));
    }

    private static <T> T doInvokeAny(ForkJoinPool pool, Collection<? extends Callable<T>> tasks,
                                     boolean timed, long nanos)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null)
            throw new NullPointerException();
        int n = tasks.size();
        if (n == 0)
            throw new IllegalArgumentException();
        ArrayList<Future<T>> futures = new ArrayList<>(n);
        ExecutorCompletionService<T> ecs = new ExecutorCompletionService<>(pool);
        long deadline = timed ? System.nanoTime() + nanos : 0L;
        try {
            for (Callable<T> t : tasks)
                futures.add(ecs.submit(t));
            ExecutionException ee = null;
            for (int left = n; left > 0; left--) {
                Future<T> f;
                if (timed) {
                    f = ecs.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                    if (f == null)
                        throw new TimeoutException();
                } else {
                    f = ecs.take();
                }
                try {
                    return f.get();
                } catch (ExecutionException eex) {
                    ee = eex;
                } catch (RuntimeException rex) {
                    ee = new ExecutionException(rex);
                }
            }
            if (ee == null)
                ee = new ExecutionException(null);
            throw ee;
        } finally {
            for (Future<T> f : futures)
                f.cancel(true);
        }
    }

    // ---- delayed tasks (queue 543)

    /** The pools whose delayed tasks are cancelled at shutdown (cancelDelayedTasksOnShutdown). */
    private static final Map<ForkJoinPool, Boolean> CANCEL_ON_SHUTDOWN =
        Collections.synchronizedMap(new WeakHashMap<>());

    /** schedule(command, delay, unit): a one-shot delayed task. */
    public static ScheduledFuture<?> schedule(ForkJoinPool pool, Runnable command, long delay, TimeUnit unit) {
        long d = unit.toNanos(delay); // implicit null check of unit, as the JDK's
        if (command == null)
            throw new NullPointerException();
        return start(pool, new ScheduledTask<Void>(pool, command, null, d, 0L));
    }

    /** schedule(callable, delay, unit): a one-shot delayed task with a result. */
    public static <V> ScheduledFuture<V> schedule(ForkJoinPool pool, Callable<V> callable, long delay, TimeUnit unit) {
        long d = unit.toNanos(delay);
        if (callable == null)
            throw new NullPointerException();
        return start(pool, new ScheduledTask<V>(pool, null, callable, d, 0L));
    }

    /** scheduleAtFixedRate(command, initialDelay, period, unit). */
    public static ScheduledFuture<?> scheduleAtFixedRate(ForkJoinPool pool, Runnable command, long initialDelay,
                                                         long period, TimeUnit unit) {
        long d = unit.toNanos(initialDelay), p = unit.toNanos(period);
        if (command == null)
            throw new NullPointerException();
        if (p <= 0L)
            throw new IllegalArgumentException();
        return start(pool, new ScheduledTask<Void>(pool, command, null, d, p));
    }

    /** scheduleWithFixedDelay(command, initialDelay, delay, unit). */
    public static ScheduledFuture<?> scheduleWithFixedDelay(ForkJoinPool pool, Runnable command, long initialDelay,
                                                            long delay, TimeUnit unit) {
        long d = unit.toNanos(initialDelay), p = unit.toNanos(delay);
        if (command == null)
            throw new NullPointerException();
        if (p <= 0L)
            throw new IllegalArgumentException();
        return start(pool, new ScheduledTask<Void>(pool, command, null, d, -p));
    }

    /** cancelDelayedTasksOnShutdown(): the pool's waiting one-shot tasks are cancelled once it is shut down. */
    public static void cancelDelayedTasksOnShutdown(ForkJoinPool pool) {
        if (pool != ForkJoinPool.commonPool())
            CANCEL_ON_SHUTDOWN.put(pool, Boolean.TRUE);
    }

    private static <T> ScheduledTask<T> start(ForkJoinPool pool, ScheduledTask<T> task) {
        if (pool.isShutdown())
            throw new java.util.concurrent.RejectedExecutionException();
        pool.execute(new Waiter(task));
        return task;
    }

    /** The waiter of a scheduled task: waits for its trigger time or its end, then runs it (again, when periodic). */
    private static final class Waiter extends ForkJoinTask<Void> {
        private final ScheduledTask<?> task;

        Waiter(ScheduledTask<?> task) {
            this.task = task;
        }

        public Void getRawResult() {
            return null;
        }

        protected void setRawResult(Void v) {
        }

        protected boolean exec() {
            ScheduledTask<?> t = task;
            for (;;) {
                // wait for the trigger, in slices so that a shutdown is seen
                for (;;) {
                    if (t.isDone())
                        return true;
                    if (t.pool.isShutdown() && (t.period != 0L || CANCEL_ON_SHUTDOWN.containsKey(t.pool))) {
                        t.cancel(false);
                        return true;
                    }
                    long left = t.when - System.nanoTime();
                    if (left <= 0L)
                        break;
                    try {
                        t.get(Math.min(left, 20_000_000L), TimeUnit.NANOSECONDS);
                    } catch (TimeoutException | InterruptedException | ExecutionException
                             | CancellationException ignore) {
                    }
                }
                if (!t.runOnce())
                    return true;
            }
        }
    }

    /**
     * A delayed task as the JDK's ScheduledForkJoinTask: its action, its trigger time (System.nanoTime's), its
     * period (0: once; > 0: fixed rate; < 0: fixed delay).
     */
    static final class ScheduledTask<T> extends ForkJoinTask<T> implements ScheduledFuture<T> {
        final ForkJoinPool pool;
        final Runnable runnable;
        final Callable<? extends T> callable;
        final long period;
        volatile long when;
        T result;

        ScheduledTask(ForkJoinPool pool, Runnable runnable, Callable<? extends T> callable, long delay, long period) {
            this.pool = pool;
            this.runnable = runnable;
            this.callable = callable;
            this.period = period;
            this.when = System.nanoTime() + Math.max(delay, 0L);
        }

        public T getRawResult() {
            return result;
        }

        protected void setRawResult(T v) {
            result = v;
        }

        /** Run by the waiter alone (runOnce); a direct invoke runs the action once. */
        protected boolean exec() {
            return runOnce() || isDone();
        }

        /** Runs the action once: false when the task is done (a one-shot run, an exception, a cancellation). */
        boolean runOnce() {
            if (isDone())
                return false;
            try {
                if (callable != null) {
                    T v = callable.call();
                    if (period == 0L) {
                        complete(v);
                        return false;
                    }
                } else {
                    runnable.run();
                    if (period == 0L) {
                        complete(null);
                        return false;
                    }
                }
            } catch (Throwable ex) {
                completeExceptionally(ex);
                return false;
            }
            if (isDone())
                return false;
            if (period > 0L)
                when += period;
            else
                when = System.nanoTime() - period;
            return true;
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(when - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        public int compareTo(Delayed other) {
            if (other == this)
                return 0;
            long d = (other instanceof ScheduledTask<?> t) ? when - t.when
                : getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (d < 0L) ? -1 : (d > 0L) ? 1 : 0;
        }
    }
}
