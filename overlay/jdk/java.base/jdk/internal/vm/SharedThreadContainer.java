/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency"): the part of jdk26u's
 * jdk.internal.vm.SharedThreadContainer that ThreadPoolExecutor uses, for the Go build. The
 * JDK's thread containers group threads for thread dumps and structured concurrency (the
 * JDK's ThreadContainers registry, JavaLangAccess.start(Thread, ThreadContainer)), which jrt
 * has not: a thread started in this container is started as Thread.start starts it, and the
 * container keeps nothing. The API is the JDK's (create, start, close), so that
 * ThreadPoolExecutor translates unchanged.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.vm;

/** A container of threads that starts them and keeps nothing. */
public class SharedThreadContainer implements AutoCloseable {
    private final String name;

    private SharedThreadContainer(String name) {
        this.name = name;
    }

    /** A new container of the given name. */
    public static SharedThreadContainer create(String name) {
        return new SharedThreadContainer(name);
    }

    /** The container's name. */
    public String name() {
        return name;
    }

    /** Starts the thread (Thread.start). */
    public void start(Thread thread) {
        thread.start();
    }

    /** Closes the container: nothing to release. */
    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return name;
    }
}
