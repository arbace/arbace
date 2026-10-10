/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency"): the executor of
 * Executors.newThreadPerTaskExecutor and newVirtualThreadPerTaskExecutor in the Go build.
 * jdk26u's java.util.concurrent.ThreadPerTaskExecutor is a thread container (it extends
 * jdk.internal.vm.ThreadContainer, registers itself with ThreadContainers and starts its
 * threads through JavaLangAccess.start(Thread, ThreadContainer)), which jrt has not; this class
 * keeps its observable behaviour: a new thread of the factory per task, RejectedExecutionException
 * when the factory makes no thread or the executor is shut down, shutdownNow interrupting the
 * running threads and returning an empty list, termination once shut down with no thread left,
 * and a future whose cancellation interrupts its thread. invokeAll and invokeAny are
 * AbstractExecutorService's.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** An executor that starts a new thread for each task. */
public final class ThreadPerTaskExecutor extends AbstractExecutorService {
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int TERMINATED = 2;

    private final ThreadFactory factory;
    private final Set<Thread> threads = ConcurrentHashMap.newKeySet();
    private final CountDownLatch terminationSignal = new CountDownLatch(1);
    private final AtomicInteger state = new AtomicInteger(RUNNING);

    private ThreadPerTaskExecutor(ThreadFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    /** A thread-per-task executor whose threads the factory makes. */
    public static ThreadPerTaskExecutor create(ThreadFactory factory) {
        return new ThreadPerTaskExecutor(factory);
    }

    private void ensureNotShutdown() {
        if (state.get() >= SHUTDOWN)
            throw new RejectedExecutionException();
    }

    private void tryTerminate() {
        if (threads.isEmpty() && state.compareAndSet(SHUTDOWN, TERMINATED))
            terminationSignal.countDown();
    }

    private void tryShutdownAndTerminate(boolean interruptThreads) {
        if (state.compareAndSet(RUNNING, SHUTDOWN))
            tryTerminate();
        if (interruptThreads) {
            for (Thread t : threads)
                t.interrupt();
        }
    }

    @Override
    public void shutdown() {
        if (!isShutdown())
            tryShutdownAndTerminate(false);
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (!isTerminated())
            tryShutdownAndTerminate(true);
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return state.get() >= SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return state.get() >= TERMINATED;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit);
        if (isTerminated())
            return true;
        return terminationSignal.await(timeout, unit);
    }

    /** The thread that runs task, not yet started. */
    private Thread newThread(Runnable task) {
        Thread thread = factory.newThread(task);
        if (thread == null)
            throw new RejectedExecutionException();
        return thread;
    }

    /** The current thread's task ended: the executor forgets it, and may terminate. */
    private void taskComplete(Thread thread) {
        threads.remove(thread);
        if (state.get() == SHUTDOWN)
            tryTerminate();
    }

    private void start(Thread thread) {
        threads.add(thread);
        boolean started = false;
        try {
            if (state.get() == RUNNING) {
                thread.start();
                started = true;
            }
        } finally {
            if (!started)
                taskComplete(thread);
        }
        if (!started)
            throw new RejectedExecutionException();
    }

    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task);
        ensureNotShutdown();
        Thread[] self = new Thread[1];
        Thread thread = newThread(() -> {
            try {
                task.run();
            } finally {
                taskComplete(self[0]);
            }
        });
        self[0] = thread;
        start(thread);
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
