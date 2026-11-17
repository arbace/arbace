/*
 * Arbace (overlay/jdk, doc/go/J2G-SPEC.md §4.1 "Roots from a program's metadata", §10.3 and §12; the
 * Ghidra case study's GH4 (ii), item A4 of its G1 plan, approved by the user on 2026-11-10): jrt's
 * java.util.ServiceLoader for the Go build, a closed-world registry. jdk26u's finds providers through
 * class loaders, module layers and META-INF/services read at run time, and loads their classes; the Go
 * build loads no class. Here the providers are those of the table j2g generates when it builds the
 * program, the resource META-INF/arbace/services (arbace.j2g.meta): the world's module provides
 * (jdk/provides.txt) and every META-INF/services file of the program's resources, in the class path's
 * order, each provider's class rooted by j2g once its service is reached. A provider is found by name in
 * jrt's registry (Class.forName) and made by reflection: a module provider by its public static
 * provider() when it has one, else, as a class path provider, by its public no-argument constructor.
 * The errors are the JDK's ServiceConfigurationErrors ("S: Provider P not found", "not a subtype",
 * "could not be instantiated"). Written from jdk26u's documented API (https://github.com/openjdk/jdk26u
 * at baf63fb), without its code: Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.jrt.ClassPathResources;

/** A facility to load implementations of a service, from the program's generated registry. */
public final class ServiceLoader<S> implements Iterable<S> {

    /** A service provider found by a stream of the loader. */
    public static interface Provider<S> extends Supplier<S> {
        /** The provider's type. */
        Class<? extends S> type();

        /** The provider's instance (a new one each time, unless provider() returns the same). */
        @Override
        S get();
    }

    /** The registry's resource (arbace.j2g.meta/services-resource). */
    private static final String REGISTRY = "META-INF/arbace/services";

    /** One provider of the registry: its class's binary name and whether a module provides it. */
    private static final class Entry {
        final String name;
        final boolean module;

        Entry(String name, boolean module) {
            this.name = name;
            this.module = module;
        }
    }

    /** The registry, read once: service name -> its providers in order. */
    private static Map<String, List<Entry>> registry;

    private static synchronized Map<String, List<Entry>> registry() {
        if (registry == null) {
            Map<String, List<Entry>> m = new HashMap<>();
            byte[] b = ClassPathResources.embeddedBytes(REGISTRY);
            if (b != null) {
                for (String line : new String(b, StandardCharsets.UTF_8).split("\n")) {
                    String[] f = line.trim().split(" ");
                    if (f.length == 3)
                        m.computeIfAbsent(f[1], k -> new ArrayList<>()).add(new Entry(f[2], f[0].equals("module")));
                }
            }
            registry = m;
        }
        return registry;
    }

    private final Class<S> service;
    private final ClassLoader loader;
    /** Only the platform's (module) providers: loadInstalled, the platform loader. */
    private final boolean installedOnly;
    /** The providers instantiated so far by the iterators, in order. */
    private final List<S> instances = new ArrayList<>();
    /** How many of the registry's entries the iterators have gone through. */
    private int next;

    private ServiceLoader(Class<S> service, ClassLoader loader, boolean installedOnly) {
        this.service = Objects.requireNonNull(service, "Service interface cannot be null");
        this.loader = loader;
        this.installedOnly = installedOnly;
    }

    private static ServiceConfigurationError fail(Class<?> service, String msg, Throwable cause) {
        return new ServiceConfigurationError(service.getName() + ": " + msg, cause);
    }

    private static ServiceConfigurationError fail(Class<?> service, String msg) {
        return new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    /** This loader's entries of the registry. */
    private List<Entry> entries() {
        List<Entry> es = registry().get(service.getName());
        if (es == null)
            return Collections.emptyList();
        if (!installedOnly)
            return es;
        List<Entry> ms = new ArrayList<>();
        for (Entry e : es)
            if (e.module)
                ms.add(e);
        return ms;
    }

    /** The provider of an entry, its class found and checked (not instantiated). */
    private Provider<S> provider(Entry e) {
        Class<?> c;
        try {
            c = Class.forName(e.name, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException | LinkageError x) {
            throw fail(service, "Provider " + e.name + " not found");
        }
        if (e.module) {
            Method pm = null;
            try {
                pm = c.getMethod("provider");
            } catch (NoSuchMethodException x) {
                pm = null;
            }
            if (pm != null && Modifier.isStatic(pm.getModifiers())) {
                if (!service.isAssignableFrom(pm.getReturnType()))
                    throw fail(service, pm + " return type not a subtype");
                return new ProviderImpl<S>(service, c, pm, null);
            }
        }
        if (!service.isAssignableFrom(c))
            throw fail(service, "Provider " + e.name + " not a subtype");
        if (!Modifier.isPublic(c.getModifiers()) || Modifier.isAbstract(c.getModifiers()))
            throw fail(service, "Provider " + e.name + " could not be instantiated",
                       new InstantiationException(e.name + " is not a public concrete class"));
        Constructor<?> ctor;
        try {
            ctor = c.getConstructor();
        } catch (NoSuchMethodException x) {
            throw fail(service, e.name + " Unable to get public no-arg constructor", x);
        }
        return new ProviderImpl<S>(service, c, null, ctor);
    }

    /** A provider: its type, made by its static provider() or its public constructor. */
    private static final class ProviderImpl<S> implements Provider<S> {
        private final Class<S> service;
        private final Class<?> type;
        private final Method factory;
        private final Constructor<?> ctor;

        ProviderImpl(Class<S> service, Class<?> type, Method factory, Constructor<?> ctor) {
            this.service = service;
            this.type = type;
            this.factory = factory;
            this.ctor = ctor;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends S> type() {
            return (Class<? extends S>) type;
        }

        @Override
        public S get() {
            Object p;
            try {
                p = factory != null ? factory.invoke(null) : ctor.newInstance();
            } catch (InvocationTargetException x) {
                Throwable c = x.getCause();
                if (factory != null)
                    throw fail(service, factory + " failed", c);
                throw fail(service, "Provider " + type.getName() + " could not be instantiated", c);
            } catch (ReflectiveOperationException | RuntimeException x) {
                throw fail(service, "Provider " + type.getName() + " could not be instantiated", x);
            }
            if (p == null)
                throw fail(service, factory + " returned null");
            return service.cast(p);
        }

        @Override
        public int hashCode() {
            return Objects.hash(service, type);
        }

        @Override
        public boolean equals(Object ob) {
            return ob instanceof ProviderImpl<?> that && this.service == that.service && this.type == that.type;
        }
    }

    /**
     * An iterator over the providers: those instantiated already, then the registry's next ones,
     * instantiated as they are reached (the JDK's laziness; an error at the provider that causes it).
     */
    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {
            int index;

            @Override
            public boolean hasNext() {
                synchronized (ServiceLoader.this) {
                    return index < instances.size() || next < entries().size();
                }
            }

            @Override
            public S next() {
                synchronized (ServiceLoader.this) {
                    if (index < instances.size())
                        return instances.get(index++);
                    List<Entry> es = entries();
                    if (next >= es.size())
                        throw new NoSuchElementException();
                    Entry e = es.get(next++);
                    S s = provider(e).get();
                    instances.add(s);
                    index++;
                    return s;
                }
            }
        };
    }

    /** A stream of the providers, their classes found as the stream reaches them, not instantiated. */
    public Stream<Provider<S>> stream() {
        List<Entry> es = entries();
        Spliterator<Provider<S>> sp = new Spliterators.AbstractSpliterator<Provider<S>>(es.size(), Spliterator.ORDERED | Spliterator.NONNULL) {
            int i;

            @Override
            public boolean tryAdvance(Consumer<? super Provider<S>> action) {
                if (i >= es.size())
                    return false;
                action.accept(provider(es.get(i++)));
                return true;
            }
        };
        return StreamSupport.stream(sp, false);
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader<>(service, loader, loader == ClassLoader.getPlatformClassLoader());
    }

    public static <S> ServiceLoader<S> load(Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        return new ServiceLoader<>(service, ClassLoader.getPlatformClassLoader(), true);
    }

    public static <S> ServiceLoader<S> load(ModuleLayer layer, Class<S> service) {
        Objects.requireNonNull(layer);
        return new ServiceLoader<>(service, null, true);
    }

    /** The first provider, instantiated; empty when there is none. */
    public Optional<S> findFirst() {
        Iterator<S> it = iterator();
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    /** Clears the cache of instantiated providers. */
    public synchronized void reload() {
        instances.clear();
        next = 0;
    }

    @Override
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
}
