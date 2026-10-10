/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency", JC7): the methods of ExecutorService
 * that jrt's hand-written ForkJoinPool cannot define itself, their types (List, Collection)
 * being translated classes jrt's own build cannot name. c2g gives ForkJoinPool Go methods that
 * call these (c2g_support) once ExecutorService is translated. They follow the JDK's documented
 * behaviour over the pool's public API: invokeAll waits for every task and returns their
 * futures; invokeAny returns the result of one task that completed normally, cancelling the
 * others (as AbstractExecutorService's), else throws ExecutionException; shutdownNow shuts the
 * pool down and returns an empty list (the JDK's pool cancels its queued tasks; jrt's runs
 * every task in a worker of its own at once, so none is queued).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** ExecutorService's collection methods for jrt's ForkJoinPool. */
public final class ForkJoinPools {

    private ForkJoinPools() {
    }

    /** shutdownNow: shut down; no task is queued. */
    public static List<Runnable> shutdownNow(ForkJoinPool pool) {
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
}
