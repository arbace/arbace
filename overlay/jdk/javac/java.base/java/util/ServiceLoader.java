/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN2"): jrt's
 * java.util.ServiceLoader for the world of javac's tool build, written for jrt in place of
 * jdk26u's, which finds providers through class loaders, modules and META-INF/services: the Go
 * build has none of them, so this ServiceLoader finds no provider. javac then runs as on a JVM
 * whose image holds no plugin, doclint, annotation processor or platform provider: its own
 * NoDocLint, no plugin started, no processor, no --release platform. Written from jdk26u's
 * documented API (https://github.com/openjdk/jdk26u at baf63fb), without its code: Copyright (c)
 * the Arbace authors; Eclipse Public License 1.0.
 */
package java.util;

import java.util.function.Supplier;
import java.util.stream.Stream;

/** A facility to load implementations of a service: none is found. */
public final class ServiceLoader<S> implements Iterable<S> {

    /** A service provider found by a stream of the loader: none is. */
    public static interface Provider<S> extends Supplier<S> {
        Class<? extends S> type();

        @Override
        S get();
    }

    private final Class<S> service;

    private ServiceLoader(Class<S> service) {
        this.service = Objects.requireNonNull(service, "Service interface cannot be null");
    }

    /** An iterator over the providers: empty. */
    @Override
    public Iterator<S> iterator() {
        return Collections.emptyIterator();
    }

    /** A stream of the providers: empty. */
    public Stream<Provider<S>> stream() {
        return Stream.empty();
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<>(service);
    }

    public static <S> ServiceLoader<S> load(Class<S> service) {
        return new ServiceLoader<>(service);
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<>(service);
    }

    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
        Objects.requireNonNull(layer);
        return new ServiceLoader<>(service);
    }

    /** The first provider: none. */
    public Optional<S> findFirst() {
        return Optional.empty();
    }

    /** Clears the cache of providers: there is none. */
    public void reload() {
    }

    @Override
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
}
