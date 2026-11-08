/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F27 (write-ahead)", JMX-2): the JMX connector
 * providers java.util.ServiceLoader would find, for javax.management.remote's factories (JMXConnectorFactory and
 * JMXConnectorServerFactory, derived by test/jrt/derive.ae to ask this class where jdk26u asks ServiceLoader, which the
 * Go build has not: no module layer, no class path, no META-INF/services). The closed world's providers are the
 * modules' own: java.management.rmi's module-info.java (https://github.com/openjdk/jdk26u baf63fb,
 * src/java.management.rmi/share/classes/module-info.java) provides JMXConnectorProvider with
 * com.sun.jmx.remote.protocol.rmi.ClientProvider and JMXConnectorServerProvider with ServerProvider; no application
 * module or class path provides one. Provider is ServiceLoader.Provider's interface (type and get), so the factories'
 * filters, isSystemProvider among them, read the providers as jdk26u's do.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnectorServerProvider;

/** The service providers of javax.management.remote's two provider interfaces in the closed world. */
public final class JmxProviders {
    private JmxProviders() { }

    /** java.util.ServiceLoader.Provider's interface: a provider's class and its instance. */
    public interface Provider<S> extends Supplier<S> {
        Class<? extends S> type();

        @Override
        S get();
    }

    private static final class Of<S> implements Provider<S> {
        private final Class<? extends S> type;
        private final Supplier<? extends S> make;

        Of(Class<? extends S> type, Supplier<? extends S> make) {
            this.type = type;
            this.make = make;
        }

        public Class<? extends S> type() {
            return type;
        }

        public S get() {
            return make.get();
        }
    }

    /**
     * The providers of service, as ServiceLoader.load(service, loader).stream() would list them: the system modules'
     * (java.management.rmi's), whatever the loader, since no other loader holds a provider configuration.
     */
    @SuppressWarnings("unchecked")
    public static <P> Stream<Provider<P>> providers(Class<P> service, ClassLoader loader) {
        if (service == JMXConnectorProvider.class) {
            Provider<JMXConnectorProvider> p = new Of<>(com.sun.jmx.remote.protocol.rmi.ClientProvider.class,
                                                        com.sun.jmx.remote.protocol.rmi.ClientProvider::new);
            return Stream.of((Provider<P>) (Provider<?>) p);
        }
        if (service == JMXConnectorServerProvider.class) {
            Provider<JMXConnectorServerProvider> p = new Of<>(com.sun.jmx.remote.protocol.rmi.ServerProvider.class,
                                                              com.sun.jmx.remote.protocol.rmi.ServerProvider::new);
            return Stream.of((Provider<P>) (Provider<?>) p);
        }
        return Stream.empty();
    }
}
