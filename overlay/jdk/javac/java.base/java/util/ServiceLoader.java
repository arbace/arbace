/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN2"): jrt's
 * java.util.ServiceLoader for the world of javac's tool build, written for jrt in place of
 * jdk26u's, which finds providers through class loaders, modules and META-INF/services: the Go
 * build has none of them, so this ServiceLoader finds no provider. javac then runs as on a JVM
 * whose image holds no plugin, doclint, annotation processor or platform provider: its own
 * NoDocLint, no plugin started, no processor, no --release platform. The tool build's variant
 * (overlay/jdk/javac/variants/ServiceLoader.ae) binds the few services javac's world provides
 * (JCS159 (a), JCS50 (b), JCS52 (a)) by replacing iterator() and findFirst(); stream() gives
 * iterator()'s providers, each as a Provider (JCS52 (a): com.sun.tools.doclint.DocLint's
 * newDocLint looks doclint up by a stream). Written from jdk26u's documented API
 * (https://github.com/openjdk/jdk26u at baf63fb), without its code: Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
package java.util;

import java.util.function.Supplier;
import java.util.stream.Stream;

/** A facility to load implementations of a service: none is found. */
public final class ServiceLoader<S> implements Iterable<S> {

    /** A service provider found by a stream of the loader. */
    public static interface Provider<S> extends Supplier<S> {
        Class<? extends S> type();

        @Override
        S get();
    }

    /** A provider of the iterator: the instance it gave. */
    private static final class Found<S> implements Provider<S> {
        private final S provider;

        Found(S provider) {
            this.provider = provider;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends S> type() {
            return (Class<? extends S>) provider.getClass();
        }

        @Override
        public S get() {
            return provider;
        }
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

    /** A stream of the providers: iterator()'s, in its order. */
    public Stream<Provider<S>> stream() {
        List<Provider<S>> ps = new ArrayList<>();
        for (S s : this) {
            ps.add(new Found<>(s));
        }
        return ps.stream();
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
