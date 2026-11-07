/*
 * Arbace (overlay/jdk, F10 of doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a"): jrt's
 * sun.nio.ch.DefaultSelectorProvider. jdk26u's (linux, baf63fb) is EPollSelectorProvider, over
 * epoll's natives (EPoll, EventFD); jrt's is the poll(2) selector every Unix port of jdk26u has
 * (PollSelectorProvider, the default jdk26u's AIX build uses), over jrt's poll native.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.nio.ch;

/**
 * Creates this platform's default SelectorProvider
 */

public class DefaultSelectorProvider {
    private static final SelectorProviderImpl INSTANCE = new PollSelectorProvider();

    /**
     * Prevent instantiation.
     */
    private DefaultSelectorProvider() { }

    /**
     * Returns the default SelectorProvider implementation.
     */
    public static SelectorProviderImpl get() {
        return INSTANCE;
    }
}
